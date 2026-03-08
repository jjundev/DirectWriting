package com.directwriting.ime;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.inputmethodservice.InputMethodService;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * S펜 필기 키보드 InputMethodService.
 *
 * 핵심 플로우:
 * Canvas 필기 → [전송] → commitContent() 시도 → 실패 시 클립보드 복사 + 토스트
 */
public class DirectWritingIME extends InputMethodService {

    private static final String AUTHORITY = "com.directwriting.ime.fileprovider";
    private static final String IMAGE_MIME_TYPE = "image/png";
    private static final long CLIPBOARD_PASTE_DELAY_MS = 200L;

    private CanvasView canvasView;
    private boolean targetSupportsImage = false;

    @Override
    public View onCreateInputView() {
        View keyboardView = getLayoutInflater().inflate(R.layout.keyboard_layout, null);

        canvasView = keyboardView.findViewById(R.id.canvas_view);

        // 전송 버튼
        ImageButton btnSend = keyboardView.findViewById(R.id.btn_send);
        btnSend.setOnClickListener(v -> handleSend());

        // 지우기 버튼
        ImageButton btnClear = keyboardView.findViewById(R.id.btn_clear);
        btnClear.setOnClickListener(v -> canvasView.clearCanvas());

        // 되돌리기 버튼
        ImageButton btnUndo = keyboardView.findViewById(R.id.btn_undo);
        btnUndo.setOnClickListener(v -> canvasView.undoLastStroke());

        // 키보드 전환 버튼 (기본 키보드로 전환)
        ImageButton btnKeyboard = keyboardView.findViewById(R.id.btn_keyboard);
        btnKeyboard.setOnClickListener(v -> switchToNextInputMethod());

        return keyboardView;
    }

    @Override
    public void onStartInputView(EditorInfo editorInfo, boolean restarting) {
        super.onStartInputView(editorInfo, restarting);

        // 대상 앱이 이미지 commitContent를 지원하는지 확인
        targetSupportsImage = false;
        if (editorInfo != null) {
            String[] mimeTypes = EditorInfoCompat.getContentMimeTypes(editorInfo);
            for (String mimeType : mimeTypes) {
                if (IMAGE_MIME_TYPE.equals(mimeType)) {
                    targetSupportsImage = true;
                    break;
                }
            }
        }

        // 새 입력 시작 시 캔버스 초기화
        if (canvasView != null && !restarting) {
            canvasView.clearCanvas();
        }
    }

    /**
     * 전송 버튼 눌렀을 때의 핵심 로직.
     * Canvas → Bitmap → PNG 파일 → commitContent 시도 → 실패 시 클립보드 복사
     */
    private void handleSend() {
        if (canvasView == null || canvasView.isEmpty()) {
            Toast.makeText(this, R.string.toast_empty_canvas, Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Canvas → Bitmap
        Bitmap bitmap = canvasView.exportAsBitmap();

        // 2. Bitmap → PNG 파일 저장
        File imageFile = saveBitmapToFile(bitmap);
        if (imageFile == null) {
            Toast.makeText(this, R.string.toast_save_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. FileProvider로 content:// URI 생성
        Uri contentUri = FileProvider.getUriForFile(this, AUTHORITY, imageFile);

        // 4. commitContent 시도
        boolean committed = false;
        if (targetSupportsImage) {
            committed = commitImage(contentUri);
        }

        // 5. 실패 시 클립보드 복사
        if (!committed) {
            copyToClipboard(contentUri);
            scheduleClipboardPaste();
            Toast.makeText(this, R.string.toast_clipboard_copied, Toast.LENGTH_LONG).show();
        }

        // 6. 캔버스 초기화
        canvasView.clearCanvas();
    }

    /**
     * commitContent()로 이미지를 대상 앱에 직접 삽입 시도.
     */
    private boolean commitImage(Uri contentUri) {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection == null) {
            return false;
        }

        EditorInfo editorInfo = getCurrentInputEditorInfo();
        if (editorInfo == null) {
            return false;
        }

        try {
            InputContentInfoCompat contentInfo = new InputContentInfoCompat(
                    contentUri,
                    new android.content.ClipDescription("handwriting", new String[] { IMAGE_MIME_TYPE }),
                    null // linkUri
            );

            return InputConnectionCompat.commitContent(
                    inputConnection,
                    editorInfo,
                    contentInfo,
                    InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,
                    null // opts
            );
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 클립보드에 이미지 URI를 복사합니다.
     */
    private void copyToClipboard(Uri imageUri) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newUri(getContentResolver(), "handwriting", imageUri);
            clipboard.setPrimaryClip(clip);
        }
    }

    /**
     * 클립보드 복사 직후 약간의 딜레이를 두고 붙여넣기를 시도합니다.
     */
    private void scheduleClipboardPaste() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            InputConnection inputConnection = getCurrentInputConnection();
            if (inputConnection != null) {
                try {
                    inputConnection.performContextMenuAction(android.R.id.paste);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, CLIPBOARD_PASTE_DELAY_MS);
    }

    /**
     * Bitmap을 PNG 파일로 저장합니다.
     * 캐시 디렉토리의 shared_images 폴더에 저장하여 FileProvider로 공유 가능하게 합니다.
     */
    private File saveBitmapToFile(Bitmap bitmap) {
        File imageDir = new File(getCacheDir(), "shared_images");
        if (!imageDir.exists()) {
            imageDir.mkdirs();
        }

        // 이전 파일 정리
        cleanOldImages(imageDir);

        String fileName = "handwriting_" + System.currentTimeMillis() + ".png";
        File imageFile = new File(imageDir, fileName);

        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            return imageFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 오래된 이미지 파일을 정리합니다. (최근 10개만 유지)
     */
    private void cleanOldImages(File imageDir) {
        File[] files = imageDir.listFiles();
        if (files != null && files.length > 10) {
            // 가장 오래된 파일부터 삭제
            java.util.Arrays.sort(files, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
            for (int i = 0; i < files.length - 10; i++) {
                files[i].delete();
            }
        }
    }

    /**
     * 다음 입력 방법으로 전환합니다.
     */
    private void switchToNextInputMethod() {
        switchToPreviousInputMethod();
    }
}
