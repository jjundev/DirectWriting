package com.directwriting.ime;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DirectWritingIME extends InputMethodService {
    private static final String AUTHORITY = "com.directwriting.ime.fileprovider";
    private static final String IMAGE_MIME_TYPE = "image/png";
    private static final long CLIPBOARD_PASTE_DELAY_MS = 200L;
    private static final long BACKSPACE_INITIAL_REPEAT_DELAY_MS = 350L;
    private static final long BACKSPACE_REPEAT_INTERVAL_MS = 60L;
    private static final int CLIPBOARD_PREVIEW_MAX = 20;
    private static final float HISTORY_BUTTON_ENABLED_ALPHA = 1f;
    private static final float HISTORY_BUTTON_DISABLED_ALPHA = 0.45f;
    private static final int[] PEN_COLOR_PALETTE = new int[]{
            0xFF111111,
            0xFFF44336,
            0xFFFF9800,
            0xFFFDD835,
            0xFF26A69A,
            0xFF1E88E5,
            0xFF3949AB,
            0xFF8E24AA
    };
    private static final int[] ALPHA_KEY_IDS = new int[]{R.id.key_q, R.id.key_w, R.id.key_e, R.id.key_r, R.id.key_t, R.id.key_y, R.id.key_u, R.id.key_i, R.id.key_o, R.id.key_p, R.id.key_a, R.id.key_s, R.id.key_d, R.id.key_f, R.id.key_g, R.id.key_h, R.id.key_j, R.id.key_k, R.id.key_l, R.id.key_z, R.id.key_x, R.id.key_c, R.id.key_v, R.id.key_b, R.id.key_n, R.id.key_m};
    private static final String[] ALPHA_KEY_CODES = new String[]{"q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "a", "s", "d", "f", "g", "h", "j", "k", "l", "z", "x", "c", "v", "b", "n", "m"};
    private static final int[] NUMBER_KEY_IDS = new int[]{R.id.key_num_1, R.id.key_num_2, R.id.key_num_3, R.id.key_num_4, R.id.key_num_5, R.id.key_num_6, R.id.key_num_7, R.id.key_num_8, R.id.key_num_9, R.id.key_num_0};
    private static final Map<String, String> KOREAN_BASE = DirectWritingIME.mapOf("q", "\u3142", "w", "\u3148", "e", "\u3137", "r", "\u3131", "t", "\u3145", "y", "\u315b", "u", "\u3155", "i", "\u3151", "o", "\u3150", "p", "\u3154", "a", "\u3141", "s", "\u3134", "d", "\u3147", "f", "\u3139", "g", "\u314e", "h", "\u3157", "j", "\u3153", "k", "\u314f", "l", "\u3163", "z", "\u314b", "x", "\u314c", "c", "\u314a", "v", "\u314d", "b", "\u3160", "n", "\u315c", "m", "\u3161");
    private static final Map<String, String> KOREAN_SHIFTED = DirectWritingIME.mapOf("q", "\u3143", "w", "\u3149", "e", "\u3138", "r", "\u3132", "t", "\u3146", "y", "\u315b", "u", "\u3155", "i", "\u3151", "o", "\u3152", "p", "\u3156", "a", "\u3141", "s", "\u3134", "d", "\u3147", "f", "\u3139", "g", "\u314e", "h", "\u3157", "j", "\u3153", "k", "\u314f", "l", "\u3163", "z", "\u314b", "x", "\u314c", "c", "\u314a", "v", "\u314d", "b", "\u3160", "n", "\u315c", "m", "\u3161");
    private static final Map<String, String> SYMBOL_PRIMARY = DirectWritingIME.mapOf("q", "1", "w", "2", "e", "3", "r", "4", "t", "5", "y", "6", "u", "7", "i", "8", "o", "9", "p", "0", "a", "@", "s", "#", "d", "$", "f", "%", "g", "&", "h", "-", "j", "+", "k", "(", "l", ")", "z", "*", "x", "\"", "c", "'", "v", ":", "b", ";", "n", "!", "m", "?");
    private static final Map<String, String> SYMBOL_SECONDARY = DirectWritingIME.mapOf("q", "[", "w", "]", "e", "{", "r", "}", "t", "<", "y", ">", "u", "/", "i", "\\", "o", "=", "p", "_", "a", "~", "s", "`", "d", "|", "f", "^", "g", ":", "h", ",", "j", ".", "k", "?", "l", "!", "z", ";", "x", "\"", "c", "'", "v", "`", "b", ",", "n", ".", "m", "/");
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final KeyboardStateMachine stateMachine = new KeyboardStateMachine();
    private final HangulComposer hangulComposer = new HangulComposerImpl();
    private final Map<String, Button> alphaKeyButtons = new HashMap<String, Button>();
    private final View[] colorChipViews = new View[PEN_COLOR_PALETTE.length];
    private final Runnable backspaceRepeatRunnable = new Runnable() {

        @Override
        public void run() {
            DirectWritingIME.this.backspaceRepeated = true;
            DirectWritingIME.this.handleBackspace();
            DirectWritingIME.this.mainHandler.postDelayed(this, BACKSPACE_REPEAT_INTERVAL_MS);
        }
    };
    private View keyboardPanel;
    private View handwritingPanel;
    private View imeRoot;
    private View keyboardQuickBarRow;
    private View keyboardNumberRow;
    private View keyboardRow1;
    private View keyboardRow2;
    private View keyboardRow3;
    private View keyboardRow4;
    private View keyboardNumberRowSplitGap;
    private View keyboardRow1SplitGap;
    private View keyboardRow2SplitGap;
    private View keyboardRow3SplitGap;
    private View keyboardRow4SplitGap;
    private Button btnModeToggle;
    private Button btnModeToggleHandwriting;
    private Button btnPenTool;
    private Button btnEraserTool;
    private View btnUndo;
    private View btnRedo;
    private Button keyClipboardChip;
    private Button keyShift;
    private Button keyLayoutToggle;
    private Button keyLanguageToggle;
    private Button keySpace;
    private Button keySpaceRight;
    private Button keyEnter;
    private View handwritingToolSettingsPanel;
    private View penSettingsPanel;
    private View eraserSettingsPanel;
    private SeekBar seekPenThickness;
    private TextView tvPenThicknessValue;
    private Switch switchPenPressure;
    private RadioGroup groupEraserMode;
    private RadioButton radioEraserStroke;
    private RadioButton radioEraserArea;
    private SeekBar seekEraserSize;
    private TextView tvEraserSizeValue;
    private CanvasView canvasView;
    private int baseQuickBarHeightPx;
    private int baseNumberRowHeightPx;
    private int baseRowHeightPx;
    private int baseLastRowHeightPx;
    private int keyboardFixedExtraHeightPx;
    private int keyboardMinHeightPx;
    private int keyboardMaxHeightPx;
    private float keyboardHeightRatioPortrait;
    private float keyboardHeightRatioLandscape;
    private KeyboardHeightCalculator.BaseHeights keyboardBaseHeights;
    private boolean targetSupportsImage = false;
    private boolean backspaceRepeated = false;
    private boolean suppressHandwritingControlCallbacks = false;
    private boolean handwritingSettingsExpanded = false;
    private String enterKeyLabel = "";
    private CanvasView.ToolType selectedHandwritingTool = CanvasView.ToolType.PEN;

    private enum HandwritingSettingsPanelType {
        PEN,
        ERASER
    }

    private HandwritingSettingsPanelType activeHandwritingSettingsPanel = HandwritingSettingsPanelType.PEN;

    public View onCreateInputView() {
        View root = this.getLayoutInflater().inflate(R.layout.keyboard_layout, null);
        this.imeRoot = root.findViewById(R.id.ime_root);
        this.keyboardQuickBarRow = root.findViewById(R.id.keyboard_quickbar_row);
        this.keyboardNumberRow = root.findViewById(R.id.keyboard_number_row);
        this.keyboardRow1 = root.findViewById(R.id.keyboard_row_1);
        this.keyboardRow2 = root.findViewById(R.id.keyboard_row_2);
        this.keyboardRow3 = root.findViewById(R.id.keyboard_row_3);
        this.keyboardRow4 = root.findViewById(R.id.keyboard_row_4);
        this.keyboardNumberRowSplitGap = root.findViewById(R.id.keyboard_number_row_split_gap);
        this.keyboardRow1SplitGap = root.findViewById(R.id.keyboard_row_1_split_gap);
        this.keyboardRow2SplitGap = root.findViewById(R.id.keyboard_row_2_split_gap);
        this.keyboardRow3SplitGap = root.findViewById(R.id.keyboard_row_3_split_gap);
        this.keyboardRow4SplitGap = root.findViewById(R.id.keyboard_row_4_split_gap);
        this.keyboardPanel = root.findViewById(R.id.panel_keyboard);
        this.handwritingPanel = root.findViewById(R.id.panel_handwriting);
        this.btnModeToggle = (Button) root.findViewById(R.id.btn_mode_toggle);
        this.btnModeToggleHandwriting = (Button) root.findViewById(R.id.btn_mode_toggle_handwriting);
        this.btnPenTool = (Button) root.findViewById(R.id.btn_pen_tool);
        this.btnEraserTool = (Button) root.findViewById(R.id.btn_eraser_tool);
        this.btnUndo = root.findViewById(R.id.btn_undo);
        this.btnRedo = root.findViewById(R.id.btn_redo);
        this.keyClipboardChip = (Button) root.findViewById(R.id.key_clipboard_chip);
        this.keyShift = (Button) root.findViewById(R.id.key_shift);
        this.keyLayoutToggle = (Button) root.findViewById(R.id.key_layout_toggle);
        this.keyLanguageToggle = (Button) root.findViewById(R.id.key_language_toggle);
        this.keySpace = (Button) root.findViewById(R.id.key_space);
        this.keySpaceRight = (Button) root.findViewById(R.id.key_space_right);
        this.keyEnter = (Button) root.findViewById(R.id.key_enter);
        this.canvasView = (CanvasView) root.findViewById(R.id.canvas_view);
        this.handwritingToolSettingsPanel = root.findViewById(R.id.handwriting_tool_settings_panel);
        this.penSettingsPanel = root.findViewById(R.id.panel_pen_settings);
        this.eraserSettingsPanel = root.findViewById(R.id.panel_eraser_settings);
        this.seekPenThickness = (SeekBar) root.findViewById(R.id.seek_pen_thickness);
        this.tvPenThicknessValue = (TextView) root.findViewById(R.id.tv_pen_thickness_value);
        this.switchPenPressure = (Switch) root.findViewById(R.id.switch_pen_pressure);
        this.groupEraserMode = (RadioGroup) root.findViewById(R.id.group_eraser_mode);
        this.radioEraserStroke = (RadioButton) root.findViewById(R.id.radio_eraser_stroke);
        this.radioEraserArea = (RadioButton) root.findViewById(R.id.radio_eraser_area);
        this.seekEraserSize = (SeekBar) root.findViewById(R.id.seek_eraser_size);
        this.tvEraserSizeValue = (TextView) root.findViewById(R.id.tv_eraser_size_value);
        if (this.canvasView != null) {
            this.canvasView.setOnHistoryStateChangedListener(this::renderHandwritingHistoryButtons);
        }
        this.enforceTabletExtractUiPolicy();
        this.initKeyboardHeightConfig();
        this.bindAlphaKeys(root);
        this.bindActionKeys(root);
        this.bindHandwritingSettingsControls(root);
        this.applyCanvasPreferences();
        this.updateEnterKeyLabel(this.getCurrentInputEditorInfo());
        this.renderUi();
        return root;
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        if (this.shouldDisableExtractUi()) {
            return false;
        }
        return super.onEvaluateFullscreenMode();
    }

    public void onStartInputView(EditorInfo editorInfo, boolean restarting) {
        super.onStartInputView(editorInfo, restarting);
        this.enforceTabletExtractUiPolicy();
        this.updateTargetImageSupport(editorInfo);
        this.stateMachine.resetForNewInput();
        this.hangulComposer.flushComposing();
        this.selectedHandwritingTool = CanvasView.ToolType.PEN;
        this.activeHandwritingSettingsPanel = HandwritingSettingsPanelType.PEN;
        this.handwritingSettingsExpanded = false;
        if (this.canvasView != null && !restarting) {
            this.canvasView.clearCanvas();
        }
        this.applyCanvasPreferences();
        this.updateEnterKeyLabel(editorInfo);
        this.updateClipboardChipLabel();
        this.renderUi();
    }

    public void onFinishInput() {
        super.onFinishInput();
        this.stopBackspaceRepeat();
        this.hangulComposer.flushComposing();
        InputConnection inputConnection = this.getCurrentInputConnection();
        if (inputConnection != null) {
            inputConnection.finishComposingText();
        }
    }

    private void bindAlphaKeys(View root) {
        this.alphaKeyButtons.clear();
        for (int i = 0; i < ALPHA_KEY_IDS.length; ++i) {
            Button key = (Button)root.findViewById(ALPHA_KEY_IDS[i]);
            String keyCode = ALPHA_KEY_CODES[i];
            key.setTag((Object)keyCode);
            key.setOnClickListener(this::onKeyClicked);
            this.alphaKeyButtons.put(keyCode, key);
        }
    }

    private void bindActionKeys(View root) {
        int[] clickKeys = new int[]{
                R.id.key_shift,
                R.id.key_layout_toggle,
                R.id.key_language_toggle,
                R.id.key_space,
                R.id.key_space_right,
                R.id.key_enter,
                R.id.key_comma,
                R.id.key_period,
                R.id.btn_mode_toggle,
                R.id.btn_mode_toggle_handwriting,
                R.id.key_clipboard_chip,
                R.id.key_number_row_toggle,
                R.id.key_quickbar_close,
                R.id.btn_send,
                R.id.btn_clear,
                R.id.btn_undo,
                R.id.btn_redo,
                R.id.btn_pen_tool,
                R.id.btn_eraser_tool
        };

        for (int id : clickKeys) {
            View view = root.findViewById(id);
            if (view != null) {
                view.setOnClickListener(this::onKeyClicked);
            }
        }

        for (int id : NUMBER_KEY_IDS) {
            View view = root.findViewById(id);
            if (view != null) {
                view.setOnClickListener(this::onKeyClicked);
            }
        }

        View backspace = root.findViewById(R.id.key_backspace);
        if (backspace != null) {
            backspace.setOnTouchListener(this::onBackspaceTouch);
        }
    }

    private void bindHandwritingSettingsControls(View root) {
        int[] colorChipIds = new int[]{
                R.id.color_chip_0,
                R.id.color_chip_1,
                R.id.color_chip_2,
                R.id.color_chip_3,
                R.id.color_chip_4,
                R.id.color_chip_5,
                R.id.color_chip_6,
                R.id.color_chip_7
        };

        for (int i = 0; i < colorChipIds.length; ++i) {
            View chip = root.findViewById(colorChipIds[i]);
            this.colorChipViews[i] = chip;
            if (chip == null) {
                continue;
            }
            final int index = i;
            chip.setClickable(true);
            chip.setOnClickListener(v -> {
                int color = PEN_COLOR_PALETTE[index];
                ImePreferences.setPenColor((Context)this, color);
                if (this.canvasView != null) {
                    this.canvasView.setStrokeColor(color);
                }
                this.updateColorChipSelection(color);
                this.selectedHandwritingTool = CanvasView.ToolType.PEN;
                this.renderHandwritingToolsUi();
            });
        }

        if (this.seekPenThickness != null) {
            this.seekPenThickness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (DirectWritingIME.this.suppressHandwritingControlCallbacks) {
                        return;
                    }
                    int value = ImePreferences.clampPenThickness(progress + ImePreferences.MIN_PEN_THICKNESS);
                    int normalizedProgress = value - ImePreferences.MIN_PEN_THICKNESS;
                    if (progress != normalizedProgress) {
                        seekBar.setProgress(normalizedProgress);
                        return;
                    }
                    if (DirectWritingIME.this.tvPenThicknessValue != null) {
                        DirectWritingIME.this.tvPenThicknessValue.setText(String.valueOf(value));
                    }
                    ImePreferences.setPenThickness((Context)DirectWritingIME.this, value);
                    if (DirectWritingIME.this.canvasView != null) {
                        DirectWritingIME.this.canvasView.setBaseStrokeWidth(value);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
        }

        if (this.switchPenPressure != null) {
            this.switchPenPressure.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (this.suppressHandwritingControlCallbacks) {
                    return;
                }
                ImePreferences.setPressureSensitivityEnabled((Context)this, isChecked);
                if (this.canvasView != null) {
                    this.canvasView.setPressureSensitive(isChecked);
                }
            });
        }

        if (this.groupEraserMode != null) {
            this.groupEraserMode.setOnCheckedChangeListener((group, checkedId) -> {
                if (this.suppressHandwritingControlCallbacks) {
                    return;
                }
                if (checkedId != R.id.radio_eraser_stroke && checkedId != R.id.radio_eraser_area) {
                    return;
                }
                int mode = checkedId == R.id.radio_eraser_area
                        ? ImePreferences.ERASER_MODE_AREA
                        : ImePreferences.ERASER_MODE_STROKE;
                ImePreferences.setEraserMode((Context)this, mode);
                if (this.canvasView != null) {
                    this.canvasView.setEraserMode(this.toCanvasEraserMode(mode));
                }
            });
        }

        if (this.seekEraserSize != null) {
            this.seekEraserSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (DirectWritingIME.this.suppressHandwritingControlCallbacks) {
                        return;
                    }
                    int value = ImePreferences.clampEraserSize(progress + ImePreferences.MIN_ERASER_SIZE);
                    int normalizedProgress = value - ImePreferences.MIN_ERASER_SIZE;
                    if (progress != normalizedProgress) {
                        seekBar.setProgress(normalizedProgress);
                        return;
                    }
                    if (DirectWritingIME.this.tvEraserSizeValue != null) {
                        DirectWritingIME.this.tvEraserSizeValue.setText(
                                DirectWritingIME.this.getString(R.string.handwriting_px_value, value)
                        );
                    }
                    ImePreferences.setEraserSize((Context)DirectWritingIME.this, value);
                    if (DirectWritingIME.this.canvasView != null) {
                        DirectWritingIME.this.canvasView.setEraserSizePx(value);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
        }
    }

    private void renderHandwritingToolsUi() {
        if (this.canvasView != null) {
            this.canvasView.setToolType(this.selectedHandwritingTool);
        }

        int inactiveTextColor = this.getColor(R.color.key_text);
        int activeTextColor = Color.WHITE;
        if (this.btnPenTool != null) {
            boolean selected = this.selectedHandwritingTool == CanvasView.ToolType.PEN;
            this.btnPenTool.setSelected(selected);
            this.btnPenTool.setTextColor(selected ? activeTextColor : inactiveTextColor);
        }
        if (this.btnEraserTool != null) {
            boolean selected = this.selectedHandwritingTool == CanvasView.ToolType.ERASER;
            this.btnEraserTool.setSelected(selected);
            this.btnEraserTool.setTextColor(selected ? activeTextColor : inactiveTextColor);
        }
        if (this.handwritingToolSettingsPanel != null) {
            this.handwritingToolSettingsPanel.setVisibility(this.handwritingSettingsExpanded ? View.VISIBLE : View.GONE);
        }
        if (this.penSettingsPanel != null) {
            boolean visible = this.handwritingSettingsExpanded
                    && this.activeHandwritingSettingsPanel == HandwritingSettingsPanelType.PEN;
            this.penSettingsPanel.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        if (this.eraserSettingsPanel != null) {
            boolean visible = this.handwritingSettingsExpanded
                    && this.activeHandwritingSettingsPanel == HandwritingSettingsPanelType.ERASER;
            this.eraserSettingsPanel.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        if (this.canvasView != null) {
            this.renderHandwritingHistoryButtons(this.canvasView.canUndo(), this.canvasView.canRedo());
        } else {
            this.renderHandwritingHistoryButtons(false, false);
        }
    }

    private void renderHandwritingHistoryButtons(boolean canUndo, boolean canRedo) {
        this.updateHandwritingHistoryButtonState(this.btnUndo, canUndo);
        this.updateHandwritingHistoryButtonState(this.btnRedo, canRedo);
    }

    private void updateHandwritingHistoryButtonState(View button, boolean enabled) {
        if (button == null) {
            return;
        }
        button.setEnabled(enabled);
        button.setAlpha(enabled ? HISTORY_BUTTON_ENABLED_ALPHA : HISTORY_BUTTON_DISABLED_ALPHA);
    }

    private void updateColorChipSelection(int selectedColor) {
        for (int i = 0; i < PEN_COLOR_PALETTE.length; ++i) {
            View chip = this.colorChipViews[i];
            if (chip == null) {
                continue;
            }
            chip.setBackground(this.buildColorChipDrawable(PEN_COLOR_PALETTE[i], selectedColor == PEN_COLOR_PALETTE[i]));
        }
    }

    private GradientDrawable buildColorChipDrawable(int color, boolean selected) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        int strokeColor = selected ? this.getColor(R.color.key_enter_background) : this.getColor(R.color.key_border);
        int strokeWidthPx = selected ? 4 : 2;
        drawable.setStroke(strokeWidthPx, strokeColor);
        return drawable;
    }

    private int normalizePaletteColor(int color) {
        for (int paletteColor : PEN_COLOR_PALETTE) {
            if (paletteColor == color) {
                return color;
            }
        }
        return PEN_COLOR_PALETTE[0];
    }

    private CanvasView.EraserMode toCanvasEraserMode(int preferenceMode) {
        return preferenceMode == ImePreferences.ERASER_MODE_AREA
                ? CanvasView.EraserMode.AREA
                : CanvasView.EraserMode.STROKE;
    }

    private void onKeyClicked(View view) {
        KeyAction action = KeyActionMapper.map(view);
        switch (action.getType()) {
            case CHARACTER: {
                this.handleCharacterInput(action.getValue());
                break;
            }
            case SHIFT: {
                this.handleShift();
                break;
            }
            case BACKSPACE: {
                this.handleBackspace();
                break;
            }
            case SPACE: {
                this.handleSpace();
                break;
            }
            case COMMA: {
                this.handleComma();
                break;
            }
            case PERIOD: {
                this.handlePeriod();
                break;
            }
            case ENTER: {
                this.handleEnter();
                break;
            }
            case LAYOUT_TOGGLE: {
                this.handleLayoutToggle();
                break;
            }
            case LANGUAGE_TOGGLE: {
                this.handleLanguageToggle();
                break;
            }
            case MODE_TOGGLE: {
                this.handleModeToggle();
                break;
            }
            case QUICKBAR_CLOSE: {
                this.handleQuickBarClose();
                break;
            }
            case NUMBER_ROW_TOGGLE: {
                this.handleNumberRowToggle();
                break;
            }
            case CLIPBOARD_PASTE: {
                this.handleClipboardPaste();
                break;
            }
            case SEND_HANDWRITING: {
                this.handleSend();
                break;
            }
            case CLEAR_HANDWRITING: {
                if (this.canvasView == null) break;
                this.canvasView.clearCanvasUndoable();
                break;
            }
            case UNDO_HANDWRITING: {
                if (this.canvasView == null) break;
                this.canvasView.undoLastStroke();
                break;
            }
            case REDO_HANDWRITING: {
                if (this.canvasView == null) break;
                this.canvasView.redoLastStroke();
                break;
            }
            case PEN_TOOL: {
                this.handlePenToolButton();
                break;
            }
            case ERASER_TOOL: {
                this.handleEraserToolButton();
                break;
            }
        }
    }

    private boolean onBackspaceTouch(View view, MotionEvent event) {
        if (this.stateMachine.getMode() != KeyboardState.Mode.KEYBOARD) {
            return false;
        }
        int action = event.getActionMasked();
        if (action == 0) {
            this.stopBackspaceRepeat();
            this.backspaceRepeated = false;
            this.mainHandler.postDelayed(this.backspaceRepeatRunnable, BACKSPACE_INITIAL_REPEAT_DELAY_MS);
            return true;
        }
        if (action == 1 || action == 3) {
            boolean repeated = this.backspaceRepeated;
            this.stopBackspaceRepeat();
            if (!repeated) {
                this.handleBackspace();
            }
            return true;
        }
        return false;
    }

    private void stopBackspaceRepeat() {
        this.mainHandler.removeCallbacks(this.backspaceRepeatRunnable);
        this.backspaceRepeated = false;
    }

    private void handleCharacterInput(String keyCode) {
        if (this.stateMachine.getMode() != KeyboardState.Mode.KEYBOARD || keyCode == null) {
            return;
        }
        if (this.stateMachine.getLayout() == KeyboardState.Layout.SYMBOL) {
            String symbol = this.resolveSymbol(keyCode);
            this.commitWithPendingComposition(symbol.isEmpty() ? keyCode : symbol);
            return;
        }

        if (this.stateMachine.getLanguage() == KeyboardState.Language.EN) {
            this.commitWithPendingComposition(this.resolveEnglish(keyCode));
            this.stateMachine.consumeSingleShift();
            this.renderUi();
            return;
        }

        String jamo = this.resolveKorean(keyCode);
        if (!jamo.isEmpty()) {
            this.applyHangulResult(this.hangulComposer.inputJamo(jamo.charAt(0)));
            this.stateMachine.consumeSingleShift();
            this.renderUi();
            return;
        }

        this.commitWithPendingComposition(keyCode);
        this.stateMachine.consumeSingleShift();
        this.renderUi();
    }

    private void handleShift() {
        if (this.stateMachine.getMode() != KeyboardState.Mode.KEYBOARD) {
            return;
        }
        this.stateMachine.onShiftPressed(System.currentTimeMillis());
        this.renderUi();
    }

    private void handleBackspace() {
        if (this.stateMachine.getMode() != KeyboardState.Mode.KEYBOARD) {
            return;
        }
        InputConnection inputConnection = this.getCurrentInputConnection();
        if (inputConnection == null) {
            return;
        }
        String composing = this.hangulComposer.getComposingText();
        if (!composing.isEmpty()) {
            this.applyHangulResult(this.hangulComposer.backspace());
            return;
        }
        inputConnection.deleteSurroundingText(1, 0);
    }

    private void handleSpace() {
        if (this.stateMachine.getMode() != KeyboardState.Mode.KEYBOARD) {
            return;
        }
        this.commitWithPendingComposition(" ");
        this.stateMachine.consumeSingleShift();
        this.renderUi();
    }

    private void handleComma() {
        if (this.stateMachine.getMode() != KeyboardState.Mode.KEYBOARD) {
            return;
        }
        this.commitWithPendingComposition(",");
        this.stateMachine.consumeSingleShift();
        this.renderUi();
    }

    private void handlePeriod() {
        if (this.stateMachine.getMode() != KeyboardState.Mode.KEYBOARD) {
            return;
        }
        this.commitWithPendingComposition(".");
        this.stateMachine.consumeSingleShift();
        this.renderUi();
    }

    private void handleEnter() {
        if (this.stateMachine.getMode() != KeyboardState.Mode.KEYBOARD) {
            return;
        }
        InputConnection inputConnection = this.getCurrentInputConnection();
        if (inputConnection == null) {
            return;
        }
        this.commitPendingComposition(inputConnection);
        EditorInfo editorInfo = this.getCurrentInputEditorInfo();
        int imeOptions = editorInfo == null ? 0 : editorInfo.imeOptions;
        int action = imeOptions & 0xFF;
        boolean noEnterAction = (imeOptions & 0x40000000) != 0;
        boolean performed = false;
        if (!noEnterAction && action != 1 && action != 0) {
            performed = inputConnection.performEditorAction(action);
        }
        if (!performed) {
            inputConnection.commitText((CharSequence)"\n", 1);
        }
    }

    private void handleLayoutToggle() {
        if (this.stateMachine.getMode() != KeyboardState.Mode.KEYBOARD) {
            return;
        }
        this.commitPendingComposition(this.getCurrentInputConnection());
        this.stateMachine.toggleLayout();
        this.renderUi();
    }

    private void handleLanguageToggle() {
        if (this.stateMachine.getMode() != KeyboardState.Mode.KEYBOARD) {
            return;
        }
        this.commitPendingComposition(this.getCurrentInputConnection());
        this.stateMachine.toggleLanguage();
        this.renderUi();
    }

    private void handleModeToggle() {
        if (this.stateMachine.getMode() == KeyboardState.Mode.KEYBOARD) {
            this.commitPendingComposition(this.getCurrentInputConnection());
        }
        this.stateMachine.toggleMode();
        this.renderUi();
    }

    private void handlePenToolButton() {
        if (this.stateMachine.getMode() != KeyboardState.Mode.HANDWRITING) {
            return;
        }
        this.selectedHandwritingTool = CanvasView.ToolType.PEN;
        this.toggleHandwritingSettingsPanel(HandwritingSettingsPanelType.PEN);
        this.renderUi();
    }

    private void handleEraserToolButton() {
        if (this.stateMachine.getMode() != KeyboardState.Mode.HANDWRITING) {
            return;
        }
        this.selectedHandwritingTool = CanvasView.ToolType.ERASER;
        if (this.canvasView != null) {
            int mode = ImePreferences.getEraserMode((Context)this);
            this.canvasView.setEraserMode(this.toCanvasEraserMode(mode));
        }
        this.toggleHandwritingSettingsPanel(HandwritingSettingsPanelType.ERASER);
        this.renderUi();
    }

    private void toggleHandwritingSettingsPanel(HandwritingSettingsPanelType panelType) {
        if (this.handwritingSettingsExpanded && this.activeHandwritingSettingsPanel == panelType) {
            this.handwritingSettingsExpanded = false;
            return;
        }
        this.handwritingSettingsExpanded = true;
        this.activeHandwritingSettingsPanel = panelType;
    }

    private void handleQuickBarClose() {
        if (this.stateMachine.getMode() != KeyboardState.Mode.KEYBOARD) {
            return;
        }
        this.stateMachine.closeQuickBar();
        this.renderUi();
    }

    private void handleNumberRowToggle() {
        if (this.stateMachine.getMode() != KeyboardState.Mode.KEYBOARD) {
            return;
        }
        this.stateMachine.toggleNumberRow();
        this.renderUi();
    }

    private void handleClipboardPaste() {
        if (this.stateMachine.getMode() != KeyboardState.Mode.KEYBOARD) {
            return;
        }

        ClipboardManager clipboard = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
        InputConnection inputConnection = this.getCurrentInputConnection();
        if (clipboard == null || inputConnection == null) {
            Toast.makeText(this, R.string.toast_clipboard_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        CharSequence clipboardText = this.getClipboardText(clipboard);
        if (clipboardText != null && clipboardText.length() > 0) {
            this.commitWithPendingComposition(clipboardText.toString());
            this.updateClipboardChipLabel();
            return;
        }

        boolean pasted = false;
        try {
            pasted = inputConnection.performContextMenuAction(android.R.id.paste);
        } catch (Exception ignored) {
            pasted = false;
        }

        if (!pasted) {
            Toast.makeText(this, R.string.toast_clipboard_empty, Toast.LENGTH_SHORT).show();
        }
        this.updateClipboardChipLabel();
    }

    private void commitWithPendingComposition(String text) {
        InputConnection inputConnection = this.getCurrentInputConnection();
        if (inputConnection == null || text == null || text.isEmpty()) {
            return;
        }
        this.commitPendingComposition(inputConnection);
        inputConnection.commitText((CharSequence)text, 1);
    }

    private void commitPendingComposition(InputConnection inputConnection) {
        if (inputConnection == null) {
            return;
        }
        String pending = this.hangulComposer.flushComposing();
        if (!pending.isEmpty()) {
            inputConnection.commitText((CharSequence)pending, 1);
        }
        inputConnection.finishComposingText();
    }

    private void applyHangulResult(HangulComposer.Result result) {
        String composingText;
        InputConnection inputConnection = this.getCurrentInputConnection();
        if (inputConnection == null || result == null) {
            return;
        }
        if (!result.getCommittedText().isEmpty()) {
            inputConnection.commitText((CharSequence)result.getCommittedText(), 1);
        }
        if ((composingText = result.getComposingText()).isEmpty()) {
            inputConnection.finishComposingText();
        } else {
            inputConnection.setComposingText((CharSequence)composingText, 1);
        }
    }

    private void renderUi() {
        if (this.keyboardPanel == null || this.handwritingPanel == null || this.btnModeToggle == null) {
            return;
        }

        boolean keyboardMode = this.stateMachine.getMode() == KeyboardState.Mode.KEYBOARD;
        this.keyboardPanel.setVisibility(keyboardMode ? View.VISIBLE : View.GONE);
        this.handwritingPanel.setVisibility(keyboardMode ? View.GONE : View.VISIBLE);
        this.btnModeToggle.setText(this.getString(R.string.mode_handwriting));
        if (this.btnModeToggleHandwriting != null) {
            this.btnModeToggleHandwriting.setText(this.getString(R.string.mode_keyboard));
        }

        if (keyboardMode) {
            if (this.keyboardQuickBarRow != null) {
                this.keyboardQuickBarRow.setVisibility(this.stateMachine.isQuickBarVisible() ? View.VISIBLE : View.GONE);
            }
            if (this.keyboardNumberRow != null) {
                this.keyboardNumberRow.setVisibility(this.stateMachine.isNumberRowVisible() ? View.VISIBLE : View.GONE);
            }
            this.handwritingSettingsExpanded = false;
            if (this.handwritingToolSettingsPanel != null) {
                this.handwritingToolSettingsPanel.setVisibility(View.GONE);
            }
            this.applyScaledKeyboardHeight();
            this.applyTabletSplitLayout();
            this.renderKeyboardLabels();
            this.updateClipboardChipLabel();
        } else {
            this.applyBaseKeyboardHeight();
            this.applyTabletSplitLayout();
            this.renderHandwritingToolsUi();
        }
    }

    private void initKeyboardHeightConfig() {
        this.baseQuickBarHeightPx = this.getResources().getDimensionPixelSize(R.dimen.ime_keyboard_quickbar_base_height);
        this.baseNumberRowHeightPx = this.getResources().getDimensionPixelSize(R.dimen.ime_keyboard_number_row_base_height);
        this.baseRowHeightPx = this.getResources().getDimensionPixelSize(R.dimen.ime_keyboard_row_base_height);
        this.baseLastRowHeightPx = this.getResources().getDimensionPixelSize(R.dimen.ime_keyboard_last_row_base_height);
        this.keyboardFixedExtraHeightPx = this.getResources().getDimensionPixelSize(R.dimen.ime_keyboard_fixed_extra_height);
        this.keyboardMinHeightPx = this.getResources().getDimensionPixelSize(R.dimen.ime_keyboard_min_height);
        this.keyboardMaxHeightPx = this.getResources().getDimensionPixelSize(R.dimen.ime_keyboard_max_height);
        this.keyboardHeightRatioPortrait = this.getResources().getFraction(R.fraction.ime_keyboard_height_ratio_portrait, 1, 1);
        this.keyboardHeightRatioLandscape = this.getResources().getFraction(R.fraction.ime_keyboard_height_ratio_landscape, 1, 1);
        this.keyboardBaseHeights = new KeyboardHeightCalculator.BaseHeights(
                this.baseQuickBarHeightPx,
                this.baseNumberRowHeightPx,
                this.baseRowHeightPx,
                this.baseRowHeightPx,
                this.baseRowHeightPx,
                this.baseLastRowHeightPx
        );
    }

    private void applyScaledKeyboardHeight() {
        if (this.keyboardBaseHeights == null) {
            return;
        }
        int screenHeightPx = this.getResources().getDisplayMetrics().heightPixels;
        boolean isPortrait =
                this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE;
        KeyboardHeightCalculator.Result result = KeyboardHeightCalculator.calculate(
                screenHeightPx,
                isPortrait,
                this.keyboardHeightRatioPortrait,
                this.keyboardHeightRatioLandscape,
                this.keyboardMinHeightPx,
                this.keyboardMaxHeightPx,
                this.keyboardFixedExtraHeightPx,
                this.keyboardBaseHeights,
                this.stateMachine.isQuickBarVisible(),
                this.stateMachine.isNumberRowVisible()
        );
        this.setViewHeight(this.keyboardQuickBarRow, result.getQuickBarHeightPx());
        this.setViewHeight(this.keyboardNumberRow, result.getNumberRowHeightPx());
        this.setViewHeight(this.keyboardRow1, result.getRow1HeightPx());
        this.setViewHeight(this.keyboardRow2, result.getRow2HeightPx());
        this.setViewHeight(this.keyboardRow3, result.getRow3HeightPx());
        this.setViewHeight(this.keyboardRow4, result.getRow4HeightPx());
        if (this.imeRoot != null) {
            this.imeRoot.setMinimumHeight(result.getTotalHeightPx());
        }
    }

    private void applyBaseKeyboardHeight() {
        this.setViewHeight(this.keyboardQuickBarRow, this.baseQuickBarHeightPx);
        this.setViewHeight(this.keyboardNumberRow, this.baseNumberRowHeightPx);
        this.setViewHeight(this.keyboardRow1, this.baseRowHeightPx);
        this.setViewHeight(this.keyboardRow2, this.baseRowHeightPx);
        this.setViewHeight(this.keyboardRow3, this.baseRowHeightPx);
        this.setViewHeight(this.keyboardRow4, this.baseLastRowHeightPx);
        if (this.imeRoot != null) {
            this.imeRoot.setMinimumHeight(0);
        }
    }

    private boolean shouldDisableExtractUi() {
        Configuration configuration = this.getResources().getConfiguration();
        return ImeWindowModeResolver.shouldDisableExtractUi(configuration.smallestScreenWidthDp);
    }

    private boolean shouldUseTabletSplitLayout() {
        Configuration configuration = this.getResources().getConfiguration();
        return ImeWindowModeResolver.shouldUseSplitLayout(
                configuration.smallestScreenWidthDp,
                configuration.orientation
        );
    }

    private void enforceTabletExtractUiPolicy() {
        if (this.shouldDisableExtractUi()) {
            this.setExtractViewShown(false);
        }
    }

    private void applyTabletSplitLayout() {
        boolean splitEnabled = this.shouldUseTabletSplitLayout();
        this.applySplitSpacer(this.keyboardNumberRowSplitGap, this.keyboardNumberRow, splitEnabled);
        this.applySplitSpacer(this.keyboardRow1SplitGap, this.keyboardRow1, splitEnabled);
        this.applySplitSpacer(this.keyboardRow2SplitGap, this.keyboardRow2, splitEnabled);
        this.applySplitSpacer(this.keyboardRow3SplitGap, this.keyboardRow3, splitEnabled);
        this.applySplitSpacer(this.keyboardRow4SplitGap, this.keyboardRow4, splitEnabled);
        this.updateSplitSpaceKeys(splitEnabled);

        if (splitEnabled && this.keyboardPanel != null && this.keyboardPanel.getWidth() == 0) {
            this.keyboardPanel.post(this::applyTabletSplitLayout);
        }
    }

    private void applySplitSpacer(View spacer, View row, boolean splitEnabled) {
        if (spacer == null) {
            return;
        }
        if (!splitEnabled) {
            spacer.setVisibility(View.GONE);
            this.setViewWidth(spacer, 0);
            return;
        }
        int rowContentWidthPx = this.resolveRowContentWidth(row);
        int gapWidthPx = ImeWindowModeResolver.resolveSplitGapWidthPx(rowContentWidthPx);
        this.setViewWidth(spacer, gapWidthPx);
        spacer.setVisibility(View.VISIBLE);
    }

    private int resolveRowContentWidth(View row) {
        int rowWidth = row != null ? row.getWidth() : 0;
        if (rowWidth <= 0 && this.keyboardPanel != null) {
            rowWidth = this.keyboardPanel.getWidth();
        }
        if (rowWidth <= 0) {
            rowWidth = this.getResources().getDisplayMetrics().widthPixels;
        }
        int horizontalPadding = row != null ? row.getPaddingLeft() + row.getPaddingRight() : 0;
        return Math.max(0, rowWidth - horizontalPadding);
    }

    private void updateSplitSpaceKeys(boolean splitEnabled) {
        if (this.keySpace == null) {
            return;
        }
        this.setHorizontalWeight(this.keySpace, 1f);
        if (this.keySpaceRight != null) {
            this.setHorizontalWeight(this.keySpaceRight, splitEnabled ? 1f : 0f);
            this.keySpaceRight.setVisibility(splitEnabled ? View.VISIBLE : View.GONE);
        }
    }

    private void setHorizontalWeight(View view, float weight) {
        if (view == null) {
            return;
        }
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (!(layoutParams instanceof LinearLayout.LayoutParams)) {
            return;
        }
        LinearLayout.LayoutParams linearLayoutParams = (LinearLayout.LayoutParams) layoutParams;
        if (linearLayoutParams.width != 0 || linearLayoutParams.weight != weight) {
            linearLayoutParams.width = 0;
            linearLayoutParams.weight = weight;
            view.setLayoutParams(linearLayoutParams);
        }
    }

    private void setViewHeight(View view, int heightPx) {
        if (view == null) {
            return;
        }
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams == null) {
            return;
        }
        int safeHeight = Math.max(0, heightPx);
        if (layoutParams.height != safeHeight) {
            layoutParams.height = safeHeight;
            view.setLayoutParams(layoutParams);
        }
    }

    private void setViewWidth(View view, int widthPx) {
        if (view == null) {
            return;
        }
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams == null) {
            return;
        }
        int safeWidth = Math.max(0, widthPx);
        if (layoutParams.width != safeWidth) {
            layoutParams.width = safeWidth;
            view.setLayoutParams(layoutParams);
        }
    }

    private void renderKeyboardLabels() {
        for (String keyCode : ALPHA_KEY_CODES) {
            Button keyButton = this.alphaKeyButtons.get(keyCode);
            if (keyButton == null) continue;
            keyButton.setText(this.resolveLabelForKey(keyCode));
        }
        this.keyLayoutToggle.setText(this.getString(this.stateMachine.getLayout() == KeyboardState.Layout.ALPHA ? R.string.key_toggle_symbols : R.string.key_toggle_alpha));
        this.keyLanguageToggle.setText(R.string.key_language_toggle);
        this.keyLanguageToggle.setEnabled(this.stateMachine.getLayout() == KeyboardState.Layout.ALPHA);
        if (this.stateMachine.getShiftState() == KeyboardState.ShiftState.CAPS_LOCK) {
            this.keyShift.setText(R.string.key_shift_caps_lock);
        } else if (this.stateMachine.getShiftState() == KeyboardState.ShiftState.ON) {
            this.keyShift.setText(R.string.key_shift_on);
        } else {
            this.keyShift.setText(R.string.key_shift_off);
        }
        this.keyEnter.setText(this.enterKeyLabel);
        if (this.keyClipboardChip != null) {
            this.keyClipboardChip.setText(this.resolveClipboardPreviewText());
        }
    }

    private String resolveLabelForKey(String keyCode) {
        if (this.stateMachine.getLayout() == KeyboardState.Layout.SYMBOL) {
            return this.resolveSymbol(keyCode);
        }
        if (this.stateMachine.getLanguage() == KeyboardState.Language.EN) {
            return this.resolveEnglish(keyCode);
        }
        return this.resolveKorean(keyCode);
    }

    private String resolveEnglish(String keyCode) {
        if (keyCode.length() == 1 && Character.isLetter(keyCode.charAt(0)) && this.stateMachine.isShifted()) {
            return keyCode.toUpperCase(Locale.US);
        }
        return keyCode;
    }

    private String resolveKorean(String keyCode) {
        Map<String, String> map = this.stateMachine.isShifted() ? KOREAN_SHIFTED : KOREAN_BASE;
        String value = map.get(keyCode);
        return value == null ? "" : value;
    }

    private String resolveSymbol(String keyCode) {
        Map<String, String> map = this.stateMachine.isShifted() ? SYMBOL_SECONDARY : SYMBOL_PRIMARY;
        String value = map.get(keyCode);
        return value == null ? "" : value;
    }

    private void updateClipboardChipLabel() {
        if (this.keyClipboardChip == null) {
            return;
        }
        this.keyClipboardChip.setText(this.resolveClipboardPreviewText());
    }

    private String resolveClipboardPreviewText() {
        ClipboardManager clipboard = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null || !clipboard.hasPrimaryClip()) {
            return this.getString(R.string.key_clipboard_default);
        }

        CharSequence text = this.getClipboardText(clipboard);
        if (text == null) {
            return this.getString(R.string.key_clipboard_default);
        }

        String normalized = text.toString().replace('\n', ' ').trim();
        if (normalized.isEmpty()) {
            return this.getString(R.string.key_clipboard_default);
        }
        if (normalized.length() > CLIPBOARD_PREVIEW_MAX) {
            return normalized.substring(0, CLIPBOARD_PREVIEW_MAX) + "…";
        }
        return normalized;
    }

    private CharSequence getClipboardText(ClipboardManager clipboard) {
        if (clipboard == null || !clipboard.hasPrimaryClip()) {
            return null;
        }

        ClipDescription description = clipboard.getPrimaryClipDescription();
        if (description == null
                || (!description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                && !description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)
                && !description.hasMimeType("text/*"))) {
            return null;
        }

        ClipData clipData = clipboard.getPrimaryClip();
        if (clipData == null || clipData.getItemCount() == 0) {
            return null;
        }

        CharSequence text = clipData.getItemAt(0).coerceToText(this);
        if (text == null || text.length() == 0) {
            return null;
        }
        return text;
    }

    private void applyCanvasPreferences() {
        if (this.canvasView == null) {
            return;
        }
        int penThickness = ImePreferences.getPenThickness((Context)this);
        boolean pressureSensitive = ImePreferences.isPressureSensitivityEnabled((Context)this);
        int penColor = this.normalizePaletteColor(ImePreferences.getPenColor((Context)this));
        int eraserMode = ImePreferences.getEraserMode((Context)this);
        int eraserSize = ImePreferences.getEraserSize((Context)this);

        ImePreferences.setPenColor((Context)this, penColor);
        ImePreferences.setEraserMode((Context)this, eraserMode);
        ImePreferences.setEraserSize((Context)this, eraserSize);

        this.canvasView.setStrokeColor(penColor);
        this.canvasView.setBaseStrokeWidth(penThickness);
        this.canvasView.setPressureSensitive(pressureSensitive);
        this.canvasView.setEraserMode(this.toCanvasEraserMode(eraserMode));
        this.canvasView.setEraserSizePx(eraserSize);
        this.canvasView.setToolType(this.selectedHandwritingTool);

        this.suppressHandwritingControlCallbacks = true;
        try {
            if (this.seekPenThickness != null) {
                this.seekPenThickness.setProgress(penThickness - ImePreferences.MIN_PEN_THICKNESS);
            }
            if (this.tvPenThicknessValue != null) {
                this.tvPenThicknessValue.setText(String.valueOf(penThickness));
            }
            if (this.switchPenPressure != null) {
                this.switchPenPressure.setChecked(pressureSensitive);
            }
            if (this.groupEraserMode != null) {
                this.groupEraserMode.check(
                        eraserMode == ImePreferences.ERASER_MODE_AREA
                                ? R.id.radio_eraser_area
                                : R.id.radio_eraser_stroke
                );
            }
            if (this.seekEraserSize != null) {
                this.seekEraserSize.setProgress(eraserSize - ImePreferences.MIN_ERASER_SIZE);
            }
            if (this.tvEraserSizeValue != null) {
                this.tvEraserSizeValue.setText(this.getString(R.string.handwriting_px_value, eraserSize));
            }
        } finally {
            this.suppressHandwritingControlCallbacks = false;
        }
        this.updateColorChipSelection(penColor);
        this.renderHandwritingToolsUi();
    }

    private void updateEnterKeyLabel(EditorInfo editorInfo) {
        int imeOptions = editorInfo == null ? 0 : editorInfo.imeOptions;
        int action = imeOptions & 0xFF;
        boolean noEnterAction = (imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0;
        if (noEnterAction) {
            this.enterKeyLabel = this.getString(R.string.key_enter_default);
            return;
        }
        switch (action) {
            case 2: {
                this.enterKeyLabel = this.getString(R.string.key_enter_go);
                break;
            }
            case 3: {
                this.enterKeyLabel = this.getString(R.string.key_enter_search);
                break;
            }
            case 4: {
                this.enterKeyLabel = this.getString(R.string.key_enter_send);
                break;
            }
            case 5: {
                this.enterKeyLabel = this.getString(R.string.key_enter_next);
                break;
            }
            case 6: {
                this.enterKeyLabel = this.getString(R.string.key_enter_done);
                break;
            }
            default: {
                this.enterKeyLabel = this.getString(R.string.key_enter_default);
            }
        }
    }

    private void updateTargetImageSupport(EditorInfo editorInfo) {
        String[] mimeTypes;
        this.targetSupportsImage = false;
        if (editorInfo == null) {
            return;
        }
        for (String mimeType : mimeTypes = EditorInfoCompat.getContentMimeTypes((EditorInfo)editorInfo)) {
            if (!IMAGE_MIME_TYPE.equals(mimeType)) continue;
            this.targetSupportsImage = true;
            break;
        }
    }

    private void handleSend() {
        if (this.canvasView == null || this.canvasView.isEmpty()) {
            Toast.makeText(this, R.string.toast_empty_canvas, Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap bitmap = this.canvasView.exportAsBitmap();
        File imageFile = this.saveBitmapToFile(bitmap);
        if (imageFile == null) {
            Toast.makeText(this, R.string.toast_save_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        Uri contentUri = FileProvider.getUriForFile(this, AUTHORITY, imageFile);
        boolean committed = false;
        if (this.targetSupportsImage) {
            committed = this.commitImage(contentUri);
        }

        if (!committed) {
            this.copyToClipboard(contentUri);
            this.scheduleClipboardPaste();
            Toast.makeText(this, R.string.toast_clipboard_copied, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.toast_image_sent, Toast.LENGTH_SHORT).show();
        }
        this.canvasView.clearCanvas();
    }

    private boolean commitImage(Uri contentUri) {
        InputConnection inputConnection = this.getCurrentInputConnection();
        EditorInfo editorInfo = this.getCurrentInputEditorInfo();
        if (inputConnection == null || editorInfo == null) {
            return false;
        }
        try {
            InputContentInfoCompat contentInfo = new InputContentInfoCompat(contentUri, new ClipDescription((CharSequence)"handwriting", new String[]{IMAGE_MIME_TYPE}), null);
            return InputConnectionCompat.commitContent(
                    inputConnection,
                    editorInfo,
                    contentInfo,
                    InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,
                    null
            );
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void copyToClipboard(Uri imageUri) {
        ClipboardManager clipboard = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newUri((ContentResolver) this.getContentResolver(), "handwriting", imageUri);
            clipboard.setPrimaryClip(clip);
        }
    }

    private void scheduleClipboardPaste() {
        this.mainHandler.postDelayed(() -> {
            InputConnection inputConnection = this.getCurrentInputConnection();
            if (inputConnection != null) {
                try {
                    inputConnection.performContextMenuAction(android.R.id.paste);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, CLIPBOARD_PASTE_DELAY_MS);
    }

    private File saveBitmapToFile(Bitmap bitmap) {
        File imageDir = new File(this.getCacheDir(), "shared_images");
        if (!imageDir.exists() && !imageDir.mkdirs()) {
            return null;
        }

        this.cleanOldImages(imageDir);
        String fileName = "handwriting_" + System.currentTimeMillis() + ".png";
        File imageFile = new File(imageDir, fileName);
        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, (OutputStream) fos);
            fos.flush();
            return imageFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void cleanOldImages(File imageDir) {
        File[] files = imageDir.listFiles();
        if (files != null && files.length > 10) {
            Arrays.sort(files, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
            for (int i = 0; i < files.length - 10; ++i) {
                files[i].delete();
            }
        }
    }

    private static Map<String, String> mapOf(String ... keyValues) {
        HashMap<String, String> map = new HashMap<String, String>();
        int i = 0;
        while (i + 1 < keyValues.length) {
            map.put(keyValues[i], keyValues[i + 1]);
            i += 2;
        }
        return map;
    }
}
