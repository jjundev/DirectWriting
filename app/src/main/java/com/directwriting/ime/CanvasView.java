package com.directwriting.ime;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * S펜/손가락 필기를 위한 Canvas 뷰.
 */
public class CanvasView extends View {
    private static final int MAX_UNDO_HISTORY = 30;
    private static final float MIN_ERASER_SIZE_PX = 8f;
    private static final float MAX_ERASER_SIZE_PX = 72f;
    private static final float DEFAULT_ERASER_SIZE_PX = 24f;
    private static final float PRESSURE_MIN_MULTIPLIER = 0.5f;
    private static final float PRESSURE_MAX_BONUS = 2.0f;
    private static final float SINGLE_POINT_SEGMENT_EPSILON_PX = 0.1f;
    private static final int ERASER_CURSOR_FILL_COLOR = 0x33888888;
    private static final int ERASER_CURSOR_OUTLINE_COLOR = 0xCC333333;
    private static final float ERASER_CURSOR_OUTLINE_WIDTH_PX = 2f;

    public enum ToolType {
        PEN,
        ERASER
    }

    public enum EraserMode {
        STROKE,
        AREA
    }

    private static final class Stroke {
        final Path path;
        final Paint paint;
        final ArrayList<PointF> samplePoints;

        Stroke(Path path, Paint paint, ArrayList<PointF> samplePoints) {
            this.path = path;
            this.paint = paint;
            this.samplePoints = samplePoints;
        }
    }

    public interface OnHistoryStateChangedListener {
        void onHistoryStateChanged(boolean canUndo, boolean canRedo);
    }

    private final ArrayList<Stroke> strokes = new ArrayList<>();
    private final ArrayList<ArrayList<Stroke>> undoHistory = new ArrayList<>();
    private final ArrayList<ArrayList<Stroke>> redoHistory = new ArrayList<>();

    private Path currentPath;
    private Paint currentPaint;
    private ArrayList<PointF> currentSamplePoints;

    private int strokeColor = ImePreferences.DEFAULT_PEN_COLOR;
    private float baseStrokeWidth = ImePreferences.DEFAULT_PEN_THICKNESS;
    private boolean pressureSensitive = ImePreferences.DEFAULT_PRESSURE_SENSITIVITY;
    private ToolType toolType = ToolType.PEN;
    private EraserMode eraserMode = EraserMode.STROKE;
    private float eraserSizePx = DEFAULT_ERASER_SIZE_PX;

    private boolean strokeEraserConsumed = false;
    private ArrayList<Stroke> pendingEraserUndoSnapshot;
    private boolean eraserGestureChanged = false;
    private float lastX;
    private float lastY;
    private boolean hasLastEraserPoint = false;
    private float lastEraserX;
    private float lastEraserY;
    private final Paint eraserCursorFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint eraserCursorOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean eraserCursorVisible = false;
    private boolean eraserHoverActive = false;
    private boolean eraserTouchActive = false;
    private float eraserCursorX = 0f;
    private float eraserCursorY = 0f;
    private OnHistoryStateChangedListener onHistoryStateChangedListener;

    public CanvasView(Context context) {
        super(context);
        init();
    }

    public CanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CanvasView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setBackgroundColor(Color.WHITE);
        eraserCursorFillPaint.setStyle(Paint.Style.FILL);
        eraserCursorFillPaint.setColor(ERASER_CURSOR_FILL_COLOR);

        eraserCursorOutlinePaint.setStyle(Paint.Style.STROKE);
        eraserCursorOutlinePaint.setColor(ERASER_CURSOR_OUTLINE_COLOR);
        eraserCursorOutlinePaint.setStrokeWidth(ERASER_CURSOR_OUTLINE_WIDTH_PX);
    }

    private Paint createPenPaint(float pressure) {
        Paint paint = new Paint();
        paint.setColor(strokeColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(resolvePenWidth(pressure));
        return paint;
    }

    private float resolvePenWidth(float pressure) {
        if (pressureSensitive && pressure > 0f) {
            return Math.max(1f, baseStrokeWidth * (PRESSURE_MIN_MULTIPLIER + pressure * PRESSURE_MAX_BONUS));
        }
        return Math.max(1f, baseStrokeWidth);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        float pressure = event.getPressure();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = x;
                lastY = y;
                strokeEraserConsumed = false;
                if (isEraserActive(event)) {
                    beginEraserGesture();
                    hasLastEraserPoint = true;
                    lastEraserX = x;
                    lastEraserY = y;
                    updateEraserTouchState(true, x, y);
                    if (eraseAtPoint(x, y)) {
                        markEraserGestureChanged();
                    }
                } else {
                    discardPendingEraserGesture();
                    hasLastEraserPoint = false;
                    updateEraserTouchState(false, x, y);
                    startPenStroke(x, y, pressure);
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isEraserActive(event)) {
                    boolean changed;
                    updateEraserTouchState(true, x, y);
                    if (eraserMode == EraserMode.AREA && hasLastEraserPoint) {
                        changed = eraseAreaAlongSegment(lastEraserX, lastEraserY, x, y);
                    } else {
                        changed = eraseAtPoint(x, y);
                    }
                    if (changed) {
                        markEraserGestureChanged();
                    }
                    hasLastEraserPoint = true;
                    lastEraserX = x;
                    lastEraserY = y;
                    invalidate();
                    return true;
                }
                updateEraserTouchState(false, x, y);
                continuePenStroke(x, y, pressure);
                return true;

            case MotionEvent.ACTION_UP:
                if (isEraserActive(event) || hasLastEraserPoint) {
                    updateEraserTouchState(false, x, y);
                    if (eraserMode == EraserMode.AREA && hasLastEraserPoint) {
                        if (eraseAreaAlongSegment(lastEraserX, lastEraserY, x, y)) {
                            markEraserGestureChanged();
                        }
                    }
                    commitEraserGestureIfNeeded();
                    strokeEraserConsumed = false;
                    hasLastEraserPoint = false;
                    invalidate();
                } else {
                    finishPenStroke(x, y);
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                cancelCurrentStroke();
                commitEraserGestureIfNeeded();
                strokeEraserConsumed = false;
                hasLastEraserPoint = false;
                updateEraserTouchState(false, x, y);
                invalidate();
                return true;

            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        int action = event.getActionMasked();
        boolean isStylus = event.getPointerCount() > 0
                && event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS;
        if (toolType != ToolType.ERASER || !isStylus) {
            if (action == MotionEvent.ACTION_HOVER_EXIT || eraserHoverActive) {
                updateEraserHoverState(false, event.getX(), event.getY());
            }
            return super.onHoverEvent(event);
        }

        switch (action) {
            case MotionEvent.ACTION_HOVER_ENTER:
            case MotionEvent.ACTION_HOVER_MOVE:
                updateEraserHoverState(true, event.getX(), event.getY());
                return true;

            case MotionEvent.ACTION_HOVER_EXIT:
                updateEraserHoverState(false, event.getX(), event.getY());
                return true;

            default:
                return super.onHoverEvent(event);
        }
    }

    private void startPenStroke(float x, float y, float pressure) {
        currentPath = new Path();
        currentPaint = createPenPaint(pressure);
        currentSamplePoints = new ArrayList<>();
        currentPath.moveTo(x, y);
        addSamplePoint(currentSamplePoints, x, y);
    }

    private void continuePenStroke(float x, float y, float pressure) {
        if (currentPath == null || currentPaint == null || currentSamplePoints == null) {
            return;
        }
        float midX = (lastX + x) * 0.5f;
        float midY = (lastY + y) * 0.5f;
        currentPath.quadTo(lastX, lastY, midX, midY);
        addSamplePoint(currentSamplePoints, midX, midY);

        if (pressureSensitive && pressure > 0f) {
            currentPaint.setStrokeWidth(resolvePenWidth(pressure));
        }

        lastX = x;
        lastY = y;
        invalidate();
    }

    private void finishPenStroke(float x, float y) {
        if (currentPath == null || currentPaint == null || currentSamplePoints == null) {
            return;
        }
        currentPath.lineTo(x, y);
        addSamplePoint(currentSamplePoints, x, y);
        recordUndoSnapshot(createStrokeSnapshot(strokes));
        strokes.add(new Stroke(currentPath, currentPaint, currentSamplePoints));
        currentPath = null;
        currentPaint = null;
        currentSamplePoints = null;
        invalidate();
    }

    private void cancelCurrentStroke() {
        currentPath = null;
        currentPaint = null;
        currentSamplePoints = null;
    }

    private boolean eraseAtPoint(float x, float y) {
        if (eraserMode == EraserMode.STROKE) {
            if (strokeEraserConsumed) {
                return false;
            }
            int targetIndex = findTopMostStrokeIndex(x, y);
            if (targetIndex >= 0) {
                strokes.remove(targetIndex);
                strokeEraserConsumed = true;
                invalidate();
                return true;
            }
            return false;
        }

        return eraseAreaAlongSegment(x, y, x, y);
    }

    private boolean eraseAreaAlongSegment(float startX, float startY, float endX, float endY) {
        boolean changed = false;
        float eraserRadius = eraserSizePx * 0.5f;
        for (int i = 0; i < strokes.size(); ++i) {
            Stroke stroke = strokes.get(i);
            ArrayList<Stroke> fragments = splitStrokeByEraserSegment(
                    stroke,
                    startX,
                    startY,
                    endX,
                    endY,
                    eraserRadius
            );
            if (fragments == null) {
                continue;
            }

            strokes.remove(i);
            if (!fragments.isEmpty()) {
                strokes.addAll(i, fragments);
                i += fragments.size() - 1;
            } else {
                i -= 1;
            }
            changed = true;
        }

        if (changed) {
            invalidate();
        }
        return changed;
    }

    private void beginEraserGesture() {
        pendingEraserUndoSnapshot = createStrokeSnapshot(strokes);
        eraserGestureChanged = false;
    }

    private void markEraserGestureChanged() {
        eraserGestureChanged = true;
    }

    private void commitEraserGestureIfNeeded() {
        if (eraserGestureChanged && pendingEraserUndoSnapshot != null) {
            recordUndoSnapshot(pendingEraserUndoSnapshot);
        }
        discardPendingEraserGesture();
    }

    private void discardPendingEraserGesture() {
        pendingEraserUndoSnapshot = null;
        eraserGestureChanged = false;
    }

    private ArrayList<Stroke> splitStrokeByEraserSegment(
            Stroke stroke,
            float segmentStartX,
            float segmentStartY,
            float segmentEndX,
            float segmentEndY,
            float eraserRadius
    ) {
        if (stroke == null || stroke.samplePoints == null || stroke.samplePoints.isEmpty()) {
            return null;
        }

        ArrayList<PointF> samplePoints = stroke.samplePoints;
        boolean[] erasedMask = new boolean[samplePoints.size()];
        boolean anyErased = false;
        float hitRadius = eraserRadius + Math.max(0f, stroke.paint.getStrokeWidth() * 0.5f);
        for (int i = 0; i < samplePoints.size(); ++i) {
            PointF point = samplePoints.get(i);
            boolean erased = StrokeHitTestUtils.isPointNearSegment(
                    point.x,
                    point.y,
                    segmentStartX,
                    segmentStartY,
                    segmentEndX,
                    segmentEndY,
                    hitRadius
            );
            erasedMask[i] = erased;
            anyErased |= erased;
        }

        if (!anyErased) {
            return null;
        }

        ArrayList<Stroke> fragments = new ArrayList<>();
        ArrayList<PointF> currentFragmentPoints = new ArrayList<>();
        for (int i = 0; i < samplePoints.size(); ++i) {
            if (erasedMask[i]) {
                addStrokeFragmentIfNeeded(fragments, stroke, currentFragmentPoints);
                currentFragmentPoints.clear();
                continue;
            }
            currentFragmentPoints.add(samplePoints.get(i));
        }
        addStrokeFragmentIfNeeded(fragments, stroke, currentFragmentPoints);
        return fragments;
    }

    private void addStrokeFragmentIfNeeded(
            ArrayList<Stroke> fragments,
            Stroke sourceStroke,
            ArrayList<PointF> fragmentPoints
    ) {
        if (fragmentPoints == null || fragmentPoints.isEmpty()) {
            return;
        }
        ArrayList<PointF> pointsCopy = new ArrayList<>(fragmentPoints.size());
        for (PointF point : fragmentPoints) {
            pointsCopy.add(new PointF(point.x, point.y));
        }
        Path fragmentPath = buildPathFromSamplePoints(pointsCopy);
        Paint fragmentPaint = new Paint(sourceStroke.paint);
        fragments.add(new Stroke(fragmentPath, fragmentPaint, pointsCopy));
    }

    private Path buildPathFromSamplePoints(ArrayList<PointF> samplePoints) {
        Path path = new Path();
        if (samplePoints == null || samplePoints.isEmpty()) {
            return path;
        }

        PointF first = samplePoints.get(0);
        path.moveTo(first.x, first.y);
        if (samplePoints.size() == 1) {
            path.lineTo(
                    first.x + SINGLE_POINT_SEGMENT_EPSILON_PX,
                    first.y + SINGLE_POINT_SEGMENT_EPSILON_PX
            );
            return path;
        }

        PointF previous = first;
        for (int i = 1; i < samplePoints.size(); ++i) {
            PointF current = samplePoints.get(i);
            float midX = (previous.x + current.x) * 0.5f;
            float midY = (previous.y + current.y) * 0.5f;
            path.quadTo(previous.x, previous.y, midX, midY);
            previous = current;
        }
        PointF last = samplePoints.get(samplePoints.size() - 1);
        path.lineTo(last.x, last.y);
        return path;
    }

    private ArrayList<Stroke> createStrokeSnapshot(ArrayList<Stroke> source) {
        ArrayList<Stroke> snapshot = new ArrayList<>(source.size());
        for (Stroke stroke : source) {
            snapshot.add(copyStroke(stroke));
        }
        return snapshot;
    }

    private Stroke copyStroke(Stroke stroke) {
        Path copiedPath = new Path(stroke.path);
        Paint copiedPaint = new Paint(stroke.paint);
        ArrayList<PointF> copiedSamplePoints = copySamplePoints(stroke.samplePoints);
        return new Stroke(copiedPath, copiedPaint, copiedSamplePoints);
    }

    private ArrayList<PointF> copySamplePoints(ArrayList<PointF> sourcePoints) {
        ArrayList<PointF> copiedPoints = new ArrayList<>(sourcePoints.size());
        for (PointF point : sourcePoints) {
            copiedPoints.add(new PointF(point.x, point.y));
        }
        return copiedPoints;
    }

    private void pushHistorySnapshot(
            ArrayList<ArrayList<Stroke>> history,
            ArrayList<Stroke> snapshot
    ) {
        if (history.size() >= MAX_UNDO_HISTORY) {
            history.remove(0);
        }
        history.add(snapshot);
    }

    private void recordUndoSnapshot(ArrayList<Stroke> snapshot) {
        pushHistorySnapshot(undoHistory, snapshot);
        redoHistory.clear();
        notifyHistoryStateChanged();
    }

    private void restoreStrokesFromSnapshot(ArrayList<Stroke> snapshot) {
        strokes.clear();
        if (snapshot == null || snapshot.isEmpty()) {
            return;
        }
        strokes.addAll(createStrokeSnapshot(snapshot));
    }

    private int findTopMostStrokeIndex(float x, float y) {
        for (int i = strokes.size() - 1; i >= 0; --i) {
            if (isPointNearStroke(strokes.get(i), x, y)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isPointNearStroke(Stroke stroke, float x, float y) {
        if (stroke == null || stroke.samplePoints == null || stroke.samplePoints.isEmpty()) {
            return false;
        }
        float radius = eraserSizePx * 0.5f;
        float radiusSquared = radius * radius;
        for (PointF point : stroke.samplePoints) {
            float dx = point.x - x;
            float dy = point.y - y;
            if (dx * dx + dy * dy <= radiusSquared) {
                return true;
            }
        }
        return false;
    }

    private void addSamplePoint(ArrayList<PointF> points, float x, float y) {
        if (points == null) {
            return;
        }
        if (!points.isEmpty()) {
            PointF previous = points.get(points.size() - 1);
            float dx = previous.x - x;
            float dy = previous.y - y;
            if (dx * dx + dy * dy < 1f) {
                return;
            }
        }
        points.add(new PointF(x, y));
    }

    private void updateEraserHoverState(boolean active, float x, float y) {
        if (active) {
            eraserCursorX = x;
            eraserCursorY = y;
        }
        eraserHoverActive = active;
        refreshEraserCursorVisibility();
    }

    private void updateEraserTouchState(boolean active, float x, float y) {
        if (active) {
            eraserCursorX = x;
            eraserCursorY = y;
        }
        eraserTouchActive = active;
        refreshEraserCursorVisibility();
    }

    private void refreshEraserCursorVisibility() {
        boolean shouldShow = toolType == ToolType.ERASER && (eraserHoverActive || eraserTouchActive);
        if (eraserCursorVisible != shouldShow) {
            eraserCursorVisible = shouldShow;
            invalidate();
            return;
        }
        if (shouldShow) {
            invalidate();
        }
    }

    private boolean shouldUseStylusButtonEraser(MotionEvent event) {
        if (event.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS) {
            return false;
        }
        int buttonState = event.getButtonState();
        return (buttonState & MotionEvent.BUTTON_STYLUS_PRIMARY) != 0
                || (buttonState & MotionEvent.BUTTON_STYLUS_SECONDARY) != 0;
    }

    private boolean isEraserActive(MotionEvent event) {
        return toolType == ToolType.ERASER || shouldUseStylusButtonEraser(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (Stroke stroke : strokes) {
            canvas.drawPath(stroke.path, stroke.paint);
        }

        if (currentPath != null && currentPaint != null) {
            canvas.drawPath(currentPath, currentPaint);
        }

        if (eraserCursorVisible) {
            float radius = eraserSizePx * 0.5f;
            canvas.drawCircle(eraserCursorX, eraserCursorY, radius, eraserCursorFillPaint);
            canvas.drawCircle(eraserCursorX, eraserCursorY, radius, eraserCursorOutlinePaint);
        }
    }

    public Bitmap exportAsBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        for (Stroke stroke : strokes) {
            canvas.drawPath(stroke.path, stroke.paint);
        }
        if (currentPath != null && currentPaint != null) {
            canvas.drawPath(currentPath, currentPaint);
        }
        return bitmap;
    }

    public boolean isEmpty() {
        return strokes.isEmpty() && currentPath == null;
    }

    public void clearCanvas() {
        undoHistory.clear();
        redoHistory.clear();
        clearCanvasInternal();
        notifyHistoryStateChanged();
    }

    public void clearCanvasUndoable() {
        cancelCurrentStroke();
        if (strokes.isEmpty()) {
            strokeEraserConsumed = false;
            hasLastEraserPoint = false;
            discardPendingEraserGesture();
            invalidate();
            notifyHistoryStateChanged();
            return;
        }
        recordUndoSnapshot(createStrokeSnapshot(strokes));
        clearCanvasInternal();
    }

    private void clearCanvasInternal() {
        strokes.clear();
        cancelCurrentStroke();
        strokeEraserConsumed = false;
        hasLastEraserPoint = false;
        discardPendingEraserGesture();
        invalidate();
    }

    public void undoLastStroke() {
        if (currentPath != null) {
            cancelCurrentStroke();
            invalidate();
            return;
        }
        if (undoHistory.isEmpty()) {
            return;
        }
        pushHistorySnapshot(redoHistory, createStrokeSnapshot(strokes));
        ArrayList<Stroke> snapshot = undoHistory.remove(undoHistory.size() - 1);
        restoreStrokesFromSnapshot(snapshot);
        strokeEraserConsumed = false;
        hasLastEraserPoint = false;
        discardPendingEraserGesture();
        notifyHistoryStateChanged();
        invalidate();
    }

    public void redoLastStroke() {
        if (redoHistory.isEmpty()) {
            return;
        }
        pushHistorySnapshot(undoHistory, createStrokeSnapshot(strokes));
        ArrayList<Stroke> snapshot = redoHistory.remove(redoHistory.size() - 1);
        restoreStrokesFromSnapshot(snapshot);
        strokeEraserConsumed = false;
        hasLastEraserPoint = false;
        discardPendingEraserGesture();
        notifyHistoryStateChanged();
        invalidate();
    }

    public boolean canUndo() {
        return !undoHistory.isEmpty();
    }

    public boolean canRedo() {
        return !redoHistory.isEmpty();
    }

    public void setOnHistoryStateChangedListener(OnHistoryStateChangedListener listener) {
        this.onHistoryStateChangedListener = listener;
        notifyHistoryStateChanged();
    }

    private void notifyHistoryStateChanged() {
        if (onHistoryStateChangedListener != null) {
            onHistoryStateChangedListener.onHistoryStateChanged(canUndo(), canRedo());
        }
    }

    public void setStrokeColor(int color) {
        this.strokeColor = color;
    }

    public void setBaseStrokeWidth(float width) {
        this.baseStrokeWidth = Math.max(1f, width);
    }

    public void setPressureSensitive(boolean sensitive) {
        this.pressureSensitive = sensitive;
    }

    public void setToolType(ToolType toolType) {
        if (toolType == null) {
            return;
        }
        this.toolType = toolType;
        if (toolType != ToolType.ERASER) {
            eraserHoverActive = false;
            eraserTouchActive = false;
            hasLastEraserPoint = false;
            strokeEraserConsumed = false;
            discardPendingEraserGesture();
        }
        refreshEraserCursorVisibility();
    }

    public ToolType getToolType() {
        return toolType;
    }

    public void setEraserMode(EraserMode eraserMode) {
        if (eraserMode != null) {
            this.eraserMode = eraserMode;
        }
    }

    public EraserMode getEraserMode() {
        return eraserMode;
    }

    public void setEraserSizePx(float sizePx) {
        this.eraserSizePx = Math.max(MIN_ERASER_SIZE_PX, Math.min(sizePx, MAX_ERASER_SIZE_PX));
        if (eraserCursorVisible) {
            invalidate();
        }
    }

    public float getEraserSizePx() {
        return eraserSizePx;
    }
}
