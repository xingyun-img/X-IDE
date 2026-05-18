package com.xingyun.ide;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatEditText;

public class CodeEditor extends AppCompatEditText {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint = new Paint();
    private final Paint dividerPaint = new Paint();

    private static final int PADDING_RIGHT = 10;
    private int lineWidth = 58;

    public CodeEditor(Context context) {
        super(context);
        init();
    }

    public CodeEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CodeEditor(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint.setColor(0xFF888888);
        linePaint.setTextAlign(Paint.Align.RIGHT);
        bgPaint.setColor(0xFF1A1A1A);
        dividerPaint.setColor(0xFF333333);
        dividerPaint.setStrokeWidth(1f);
        setHorizontallyScrolling(false);
        updatePadding();
    }

    private void updatePadding() {
        setPadding(lineWidth + 14, 12, 8, 12);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();

        canvas.drawRect(0, 0, lineWidth, height, bgPaint);
        canvas.drawLine(lineWidth, 0, lineWidth, height, dividerPaint);

        linePaint.setTextSize(getTextSize());
        linePaint.setTypeface(getTypeface());

        Layout layout = getLayout();
        if (layout != null && layout.getLineCount() > 0) {
            int count = layout.getLineCount();
            int topPad = getTotalPaddingTop();
            int lineHeight = getLineHeight();

            Paint.FontMetrics fm = linePaint.getFontMetrics();
            float baselineOffset = (lineHeight - (fm.descent - fm.ascent)) / 2f - fm.ascent;

            for (int i = 0; i < count; i++) {
                float y = topPad + i * lineHeight + baselineOffset;
                canvas.drawText(String.valueOf(i + 1), lineWidth - PADDING_RIGHT, y, linePaint);
            }
        }

        canvas.save();
        canvas.clipRect(lineWidth + 1, 0, width, height);
        super.onDraw(canvas);
        canvas.restore();
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lenBefore, int lenAfter) {
        super.onTextChanged(text, start, lenBefore, lenAfter);
        adjustLineWidth();
    }

    private void adjustLineWidth() {
        int lines = getLineCount();
        int digits = String.valueOf(Math.max(1, lines)).length();
        int newWidth = 40 + digits * 18;

        if (newWidth != lineWidth) {
            lineWidth = newWidth;
            updatePadding();
        }
    }
}