package com.xingyun.ide;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.widget.EditText;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxHighlighter implements TextWatcher {

    private final EditText editText;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isHighlighting = false;
    private String currentLanguage = "html";
    private Runnable pendingHighlight;

    private static final int COLOR_TAG = 0xFF569CD6;
    private static final int COLOR_ATTR = 0xFF9CDCFE;
    private static final int COLOR_VALUE = 0xFFCE9178;
    private static final int COLOR_COMMENT = 0xFF6A9955;
    private static final int COLOR_CSS_PROP = 0xFF9CDCFE;
    private static final int COLOR_CSS_SEL = 0xFFD7BA7D;
    private static final int COLOR_JS_KEYWORD = 0xFF569CD6;
    private static final int COLOR_JS_FUNC = 0xFFDCDCAA;
    private static final int COLOR_JS_STR = 0xFFCE9178;
    private static final int COLOR_JS_NUM = 0xFFB5CEA8;
    private static final int COLOR_DOCTYPE = 0xFF808080;

    private static final Pattern TAG_PATTERN = Pattern.compile("</?([a-zA-Z0-9]+)[^>]*/?>");
    private static final Pattern ATTR_PATTERN = Pattern.compile("\\s([a-zA-Z-]+)\\s*=");
    private static final Pattern VALUE_PATTERN = Pattern.compile("\"([^\"]*)\"");
    private static final Pattern COMMENT_HTML = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
    private static final Pattern DOCTYPE_PATTERN = Pattern.compile("<!DOCTYPE[^>]*>", Pattern.CASE_INSENSITIVE);

    private static final Pattern CSS_SEL_PATTERN = Pattern.compile("([.#]?[a-zA-Z0-9_-]+)\\s*\\{");
    private static final Pattern CSS_PROP_PATTERN = Pattern.compile("([a-zA-Z-]+)\\s*:");
    private static final Pattern COMMENT_CSS = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    private static final Pattern JS_KEYWORD_PATTERN = Pattern.compile(
            "\\b(var|let|const|function|return|if|else|for|while|do|switch|case|break|continue|" +
            "new|this|typeof|instanceof|class|import|export|from|default|async|await|" +
            "try|catch|finally|throw)\\b");
    private static final Pattern JS_FUNC_PATTERN = Pattern.compile("function\\s+([a-zA-Z_$][a-zA-Z0-9_$]*)");
    private static final Pattern JS_STR_PATTERN = Pattern.compile("('.*?'|\".*?\"|`.*?`)");
    private static final Pattern JS_NUM_PATTERN = Pattern.compile("\\b\\d+\\.?\\d*\\b");
    private static final Pattern COMMENT_JS = Pattern.compile("//.*$|/\\*.*?\\*/", Pattern.DOTALL | Pattern.MULTILINE);

    public SyntaxHighlighter(EditText editText) {
        this.editText = editText;
    }

    public void setLanguage(String language) {
        this.currentLanguage = language;
    }

    public void attach() {
        editText.addTextChangedListener(this);
    }

    public void detach() {
        editText.removeTextChangedListener(this);
        if (pendingHighlight != null) {
            handler.removeCallbacks(pendingHighlight);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
        if (isHighlighting) return;

        // 取消之前的延迟任务
        if (pendingHighlight != null) {
            handler.removeCallbacks(pendingHighlight);
        }

        // 延迟 100ms 执行，减少频繁输入时的卡顿
        pendingHighlight = () -> highlight(s);
        handler.postDelayed(pendingHighlight, 100);
    }

    private void highlight(Editable editable) {
        if (editable.length() == 0) return;
        isHighlighting = true;

        String text = editable.toString();

        // 清除所有 ForegroundColorSpan（这个操作很快）
        ForegroundColorSpan[] spans = editable.getSpans(0, editable.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : spans) {
            editable.removeSpan(span);
        }

        switch (currentLanguage) {
            case "css":
                apply(editable, COMMENT_CSS, text, COLOR_COMMENT);
                apply(editable, CSS_SEL_PATTERN, text, COLOR_CSS_SEL);
                apply(editable, CSS_PROP_PATTERN, text, COLOR_CSS_PROP);
                break;
            case "javascript":
                apply(editable, COMMENT_JS, text, COLOR_COMMENT);
                apply(editable, JS_STR_PATTERN, text, COLOR_JS_STR);
                apply(editable, JS_KEYWORD_PATTERN, text, COLOR_JS_KEYWORD);
                apply(editable, JS_NUM_PATTERN, text, COLOR_JS_NUM);
                apply(editable, JS_FUNC_PATTERN, text, COLOR_JS_FUNC);
                break;
            default:
                apply(editable, COMMENT_HTML, text, COLOR_COMMENT);
                apply(editable, DOCTYPE_PATTERN, text, COLOR_DOCTYPE);

                Matcher tagMatcher = TAG_PATTERN.matcher(text);
                while (tagMatcher.find()) {
                    String tag = tagMatcher.group();
                    int offset = tagMatcher.start();

                    Matcher nameMatcher = Pattern.compile("</?([a-zA-Z0-9]+)").matcher(tag);
                    if (nameMatcher.find()) {
                        setSpanSafe(editable, offset + nameMatcher.start(1), offset + nameMatcher.end(1), COLOR_TAG);
                    }

                    Matcher attrMatcher = ATTR_PATTERN.matcher(tag);
                    while (attrMatcher.find()) {
                        setSpanSafe(editable, offset + attrMatcher.start(1), offset + attrMatcher.end(1), COLOR_ATTR);
                    }

                    Matcher valMatcher = VALUE_PATTERN.matcher(tag);
                    while (valMatcher.find()) {
                        setSpanSafe(editable, offset + valMatcher.start(1), offset + valMatcher.end(1), COLOR_VALUE);
                    }
                }
                break;
        }

        isHighlighting = false;
    }

    private void apply(Editable editable, Pattern pattern, String text, int color) {
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            setSpanSafe(editable, m.start(), m.end(), color);
        }
    }

    private void setSpanSafe(Editable editable, int start, int end, int color) {
        int len = editable.length();
        if (start >= 0 && end <= len && start < end) {
            try {
                editable.setSpan(new ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } catch (IndexOutOfBoundsException ignored) {}
        }
    }

    public static String getLanguageFromFileName(String fileName) {
        if (fileName == null) return "html";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".css")) return "css";
        if (lower.endsWith(".js")) return "javascript";
        return "html";
    }
}