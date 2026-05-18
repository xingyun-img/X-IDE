package com.xingyun.ide;

import android.content.Context;
import android.content.res.Resources;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListPopupWindow;
import android.widget.TextView;
import android.graphics.Color;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutoCompleteManager implements TextWatcher {

    private final EditText editText;
    private final ListPopupWindow popupWindow;
    private final SuggestionAdapter adapter;
    private final List<SuggestionItem> suggestions = new ArrayList<>();
    private boolean isEnabled = true;
    private String currentLanguage = "html";

    private final Map<String, SuggestionItem> htmlTags = new HashMap<>();
    private final Map<String, SuggestionItem> htmlAttrs = new HashMap<>();
    private final Map<String, SuggestionItem> cssProps = new HashMap<>();
    private final Map<String, SuggestionItem> jsKeywords = new HashMap<>();

    private static class SuggestionItem {
        String name;
        String description;
        String example;

        SuggestionItem(String name, String description, String example) {
            this.name = name;
            this.description = description;
            this.example = example;
        }
    }

    public AutoCompleteManager(EditText editText) {
        this.editText = editText;
        Context context = editText.getContext();

        loadWordBank(context, R.raw.html_tags, htmlTags);
        loadWordBank(context, R.raw.html_attrs, htmlAttrs);
        loadWordBank(context, R.raw.css_props, cssProps);
        loadWordBank(context, R.raw.js_keywords, jsKeywords);

        adapter = new SuggestionAdapter(context, suggestions);

        popupWindow = new ListPopupWindow(context);
        popupWindow.setAdapter(adapter);
        popupWindow.setAnchorView(editText);
        popupWindow.setWidth(600);
        popupWindow.setHeight(400);
        popupWindow.setBackgroundDrawable(null);
    }

    private void loadWordBank(Context context, int resId, Map<String, SuggestionItem> map) {
        try {
            Resources resources = context.getResources();
            InputStream is = resources.openRawResource(resId);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\|", 3);
                if (parts.length >= 3) {
                    map.put(parts[0].toLowerCase(), new SuggestionItem(parts[0], parts[1], parts[2]));
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        if (!enabled && popupWindow.isShowing()) popupWindow.dismiss();
    }

    public void setLanguage(String language) {
        this.currentLanguage = language;
    }

    public void attach() { editText.addTextChangedListener(this); }
    public void detach() {
        editText.removeTextChangedListener(this);
        if (popupWindow.isShowing()) popupWindow.dismiss();
    }

    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
        if (!isEnabled || s.length() == 0) { popupWindow.dismiss(); return; }
        int cursorPos = editText.getSelectionStart();
        if (cursorPos < 0) return;

        String text = s.toString();
        String word = getCurrentWord(text, cursorPos);
        if (word == null || word.length() < 1) { popupWindow.dismiss(); return; }

        suggestions.clear();
        String lowerWord = word.toLowerCase();

        Map<String, SuggestionItem> source = getSuggestionSource(text, cursorPos);
        if (source != null) {
            for (Map.Entry<String, SuggestionItem> entry : source.entrySet()) {
                if (entry.getKey().startsWith(lowerWord)) {
                    suggestions.add(entry.getValue());
                }
            }
        }

        if (suggestions.isEmpty()) {
            popupWindow.dismiss();
        } else {
            final int wordStart = cursorPos - word.length();
            popupWindow.setOnItemClickListener((parent, view, pos, id) -> {
                SuggestionItem item = suggestions.get(pos);
                int realStart = wordStart;
                // 支持带点补全（如 console.log）
                int dotPos = word.lastIndexOf('.');
                if (dotPos >= 0) {
                    realStart = wordStart + dotPos + 1;
                }
                s.replace(realStart, cursorPos, item.name);
                editText.setSelection(realStart + item.name.length());
                popupWindow.dismiss();
            });

            try { if (!popupWindow.isShowing()) popupWindow.show(); }
            catch (Exception ignored) {}
        }
    }

    private String getCurrentWord(String text, int cursorPos) {
        if (cursorPos <= 0) return "";
        int start = cursorPos - 1;
        while (start >= 0 && isWordChar(text.charAt(start))) start--;
        start++;
        return text.substring(start, cursorPos);
    }

    private boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '$' || c == '#';
    }

    private Map<String, SuggestionItem> getSuggestionSource(String text, int cursorPos) {
        if (currentLanguage.equals("css")) return cssProps;
        if (currentLanguage.equals("javascript")) return jsKeywords;
        if (isInsideTag(text, cursorPos) && isInsideTagAttribute(text, cursorPos)) return htmlAttrs;
        return htmlTags;
    }

    private boolean isInsideTag(String text, int cursorPos) {
        int lastLt = text.lastIndexOf('<', cursorPos);
        int lastGt = text.lastIndexOf('>', cursorPos);
        return lastLt > lastGt;
    }

    private boolean isInsideTagAttribute(String text, int cursorPos) {
        int lastLt = text.lastIndexOf('<', cursorPos);
        if (lastLt < 0) return false;
        String tagContent = text.substring(lastLt + 1, cursorPos);
        return tagContent.contains(" ") || tagContent.contains("\t") || tagContent.contains("\n");
    }

    public static String getLanguageFromFileName(String fileName) {
        if (fileName == null) return "html";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".css")) return "css";
        if (lower.endsWith(".js")) return "javascript";
        return "html";
    }

    // 自定义适配器，显示名称和描述
    private class SuggestionAdapter extends ArrayAdapter<SuggestionItem> {
        SuggestionAdapter(Context context, List<SuggestionItem> items) {
            super(context, android.R.layout.simple_list_item_2, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(getContext(), android.R.layout.simple_list_item_2, null);
            }
            TextView text1 = convertView.findViewById(android.R.id.text1);
            TextView text2 = convertView.findViewById(android.R.id.text2);

            SuggestionItem item = getItem(position);
            if (item != null) {
                text1.setText(item.name + "  " + item.example);
                text2.setText(item.description);
                text1.setTextColor(Color.WHITE);
                text1.setTextSize(12);
                text2.setTextColor(0xFF888888);
                text2.setTextSize(11);
            }
            convertView.setBackgroundColor(0xFF2D2D2D);
            convertView.setPadding(12, 8, 12, 8);
            return convertView;
        }
    }
}