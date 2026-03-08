package com.directwriting.ime;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * S펜/손가락 필기를 위한 Canvas 뷰.
 * 필압 감지, 실행 취소, 전체 지우기, 비트맵 추출 기능을 제공합니다.
 */
public class CanvasView extends View {

    // 획 기록
    private final ArrayList<Path> paths = new ArrayList<>();
    private final ArrayList<Paint> paints = new ArrayList<>();

    // 현재 그리고 있는 경로
    private Path currentPath;
    private Paint currentPaint;

    // 기본 설정
    private int strokeColor = Color.BLACK;
    private float baseStrokeWidth = 4f;
    private boolean pressureSensitive = true;

    // 터치 좌표
    private float lastX, lastY;

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
    }

    private Paint createPaint(float pressure) {
        Paint paint = new Paint();
        paint.setColor(strokeColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAntiAlias(true);

        if (pressureSensitive && pressure > 0) {
            // 필압에 따라 선 굵기 조절 (0.5x ~ 2.5x)
            float width = baseStrokeWidth * (0.5f + pressure * 2.0f);
            paint.setStrokeWidth(width);
        } else {
            paint.setStrokeWidth(baseStrokeWidth);
        }

        return paint;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        float pressure = event.getPressure();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentPath = new Path();
                currentPaint = createPaint(pressure);
                currentPath.moveTo(x, y);
                lastX = x;
                lastY = y;
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (currentPath != null) {
                    // 부드러운 곡선을 위해 quadTo 사용
                    float midX = (lastX + x) / 2;
                    float midY = (lastY + y) / 2;
                    currentPath.quadTo(lastX, lastY, midX, midY);
                    lastX = x;
                    lastY = y;

                    // 필압이 변할 때 새로운 세그먼트 시작
                    if (pressureSensitive) {
                        currentPaint = createPaint(pressure);
                    }
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (currentPath != null) {
                    currentPath.lineTo(x, y);
                    paths.add(currentPath);
                    paints.add(currentPaint);
                    currentPath = null;
                    currentPaint = null;
                    invalidate();
                }
                return true;
        }

        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 저장된 모든 획 그리기
        for (int i = 0; i < paths.size(); i++) {
            canvas.drawPath(paths.get(i), paints.get(i));
        }

        // 현재 그리고 있는 획 그리기
        if (currentPath != null && currentPaint != null) {
            canvas.drawPath(currentPath, currentPaint);
        }
    }

    /**
     * Canvas 내용을 Bitmap으로 내보냅니다.
     * 
     * @return 필기 내용이 담긴 Bitmap (흰색 배경)
     */
    public Bitmap exportAsBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        for (int i = 0; i < paths.size(); i++) {
            canvas.drawPath(paths.get(i), paints.get(i));
        }

        return bitmap;
    }

    /**
     * 캔버스 내용이 비어있는지 확인합니다.
     */
    public boolean isEmpty() {
        return paths.isEmpty();
    }

    /**
     * 모든 획을 지우고 캔버스를 초기화합니다.
     */
    public void clearCanvas() {
        paths.clear();
        paints.clear();
        currentPath = null;
        currentPaint = null;
        invalidate();
    }

    /**
     * 마지막 획을 취소합니다.
     */
    public void undoLastStroke() {
        if (!paths.isEmpty()) {
            paths.remove(paths.size() - 1);
            paints.remove(paints.size() - 1);
            invalidate();
        }
    }

    // --- Setters ---

    public void setStrokeColor(int color) {
        this.strokeColor = color;
    }

    public void setBaseStrokeWidth(float width) {
        this.baseStrokeWidth = width;
    }

    public void setPressureSensitive(boolean sensitive) {
        this.pressureSensitive = sensitive;
    }
}
