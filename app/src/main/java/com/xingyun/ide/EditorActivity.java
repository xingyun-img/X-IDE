package com.xingyun.ide;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;

public class EditorActivity extends AppCompatActivity {

    private CodeEditor htmlEditor;
    private ListView fileListView;
    private TextView currentFileText, projectNameText, fileCountText;
    private ImageButton btnMenu, btnUndo, btnRedo, btnRun, btnNewFile, btnSave;
    private Button btnClear, btnFormat, btnSearch;
    private View editorPanel, fileListPanel;

    private ProjectManager projectManager;
    private SyntaxHighlighter syntaxHighlighter;
    private AutoCompleteManager autoCompleteManager;
    private String currentProjectName;
    private String currentFileName = "index.html";
    private String currentDir = "";
    private Stack<String> dirStack = new Stack<>();
    private List<String> fileList;
    private ArrayAdapter<String> fileAdapter;

    private Stack<String> undoStack = new Stack<>();
    private Stack<String> redoStack = new Stack<>();
    private boolean isUndoRedo = false;
    private static final int MAX_STACK_SIZE = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }
        setContentView(R.layout.activity_editor);

        try {
            projectManager = new ProjectManager();
            currentProjectName = getIntent().getStringExtra("projectName");
            if (currentProjectName == null || currentProjectName.isEmpty()) {
                currentProjectName = "Untitled";
            }

            initViews();
            initListeners();
            initSyntaxHighlighting();
            initAutoComplete();
            initUndoRedo();
            initSymbolButtons();
            openDefaultFile();
            loadFileList();

            if (getSupportActionBar() != null) getSupportActionBar().hide();
            projectNameText.setText(currentProjectName);
            showEditor();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "初始化失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (syntaxHighlighter != null) syntaxHighlighter.detach();
        if (autoCompleteManager != null) autoCompleteManager.detach();
    }

    @Override
    public void onBackPressed() {
        if (fileListPanel != null && fileListPanel.getVisibility() == View.VISIBLE) {
            if (!currentDir.isEmpty()) {
                goBackDir();
            } else {
                showEditor();
            }
        } else {
            saveCurrentFileSilently();
            if (syntaxHighlighter != null) syntaxHighlighter.detach();
            if (autoCompleteManager != null) autoCompleteManager.detach();
            super.onBackPressed();
        }
    }

    // ==================== 初始化 ====================

    private void initViews() {
        htmlEditor = findViewById(R.id.htmlEditor);
        fileListView = findViewById(R.id.fileListView);
        btnMenu = findViewById(R.id.btnMenu);
        projectNameText = findViewById(R.id.projectNameText);
        btnUndo = findViewById(R.id.btnUndo);
        btnRedo = findViewById(R.id.btnRedo);
        btnRun = findViewById(R.id.btnRun);
        currentFileText = findViewById(R.id.currentFileText);
        btnNewFile = findViewById(R.id.btnNewFile);
        btnSave = findViewById(R.id.btnSave);
        btnClear = findViewById(R.id.btnClear);
        btnFormat = findViewById(R.id.btnFormat);
        btnSearch = findViewById(R.id.btnSearch);
        editorPanel = findViewById(R.id.editorPanel);
        fileListPanel = findViewById(R.id.fileListPanel);
        fileCountText = findViewById(R.id.fileCountText);

        fileList = new ArrayList<>();
        fileAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, fileList) {
            @Override
            public View getView(int pos, View v, android.view.ViewGroup p) {
                View view = super.getView(pos, v, p);
                TextView tv = view.findViewById(android.R.id.text1);
                if (tv != null) {
                    String item = fileList.get(pos);
                    boolean isDir = item.endsWith("/") || item.equals("../");
                    String displayName = isDir ? item.replace("/", "") : item;
                    boolean isCurrent = item.equals(getDisplayFileName());

                    tv.setText(displayName);
                    tv.setTextSize(13);
                    tv.setPadding(28, 18, 28, 18);
                    tv.setTextColor(isCurrent ? 0xFFFFFFFF : (isDir ? 0xFF9CDCFE : 0xFFD4D4D4));
                    tv.setBackgroundColor(isCurrent ? 0xFF3A7BD5 : 0x00000000);

                    if (item.equals("../")) {
                        tv.setCompoundDrawables(null, null, null, null);
                    } else if (isDir) {
                        Drawable folderIcon = IconHelper.getIcon(EditorActivity.this, "folder");
                        tv.setCompoundDrawablesWithIntrinsicBounds(folderIcon, null, null, null);
                    } else {
                        Drawable fileIcon = IconHelper.getIcon(EditorActivity.this, item);
                        tv.setCompoundDrawablesWithIntrinsicBounds(fileIcon, null, null, null);
                    }
                    tv.setCompoundDrawablePadding(12);
                }
                return view;
            }
        };
        fileListView.setAdapter(fileAdapter);
    }

    private void initSyntaxHighlighting() {
        syntaxHighlighter = new SyntaxHighlighter(htmlEditor);
        syntaxHighlighter.attach();
    }

    private void initAutoComplete() {
        autoCompleteManager = new AutoCompleteManager(htmlEditor);
        autoCompleteManager.setLanguage(AutoCompleteManager.getLanguageFromFileName(currentFileName));
        autoCompleteManager.attach();
    }

    private void initUndoRedo() {
        htmlEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!isUndoRedo) {
                    if (undoStack.size() >= MAX_STACK_SIZE) undoStack.remove(0);
                    undoStack.push(s.toString());
                    redoStack.clear();
                }
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void initSymbolButtons() {
        setSymbolButton(R.id.sym_lt, "<");
        setSymbolButton(R.id.sym_gt, ">");
        setSymbolButton(R.id.sym_eq, "=");
        setSymbolButton(R.id.sym_dq, "\"");
        setSymbolButton(R.id.sym_colon, ":");
        setSymbolButton(R.id.sym_comma, ",");
        setSymbolButton(R.id.sym_dot, ".");
        setSymbolButton(R.id.sym_lb, "{");
        setSymbolButton(R.id.sym_rb, "}");
        setSymbolButton(R.id.sym_lp, "(");
        setSymbolButton(R.id.sym_rp, ")");
        setSymbolButton(R.id.sym_slash, "/");
        setSymbolButton(R.id.sym_bslash, "\\");
        setSymbolButton(R.id.sym_qm, "?");
        setSymbolButton(R.id.sym_pipe, "|");
        setSymbolButton(R.id.sym_lbk, "[");
        setSymbolButton(R.id.sym_rbk, "]");
        setSymbolButton(R.id.sym_under, "_");
        setSymbolButton(R.id.sym_dash, "-");
    }

    private void setSymbolButton(int id, String symbol) {
        Button btn = findViewById(id);
        if (btn != null) {
            btn.setText(symbol);
            btn.setOnClickListener(v -> insertSymbol(symbol));
        }
    }

    private void insertSymbol(String symbol) {
        int start = htmlEditor.getSelectionStart();
        int end = htmlEditor.getSelectionEnd();
        Editable editable = htmlEditor.getText();
        if (editable != null) {
            editable.replace(start, end, symbol);
            htmlEditor.setSelection(start + symbol.length());
        }
    }

    private void initListeners() {
        btnMenu.setOnClickListener(v -> toggleFileList());
        btnUndo.setOnClickListener(v -> undo());
        btnRedo.setOnClickListener(v -> redo());
        btnRun.setOnClickListener(v -> runPreview());
        btnNewFile.setOnClickListener(v -> showNewDialog());
        btnSave.setOnClickListener(v -> saveCurrentFile());
        btnClear.setOnClickListener(v -> confirmClear());
        btnFormat.setOnClickListener(v -> formatCode());
        btnSearch.setOnClickListener(v -> showSearchDialog());

        fileListView.setOnItemClickListener((p, v, pos, id) -> {
            if (pos >= 0 && pos < fileList.size()) {
                String item = fileList.get(pos);
                if (item.equals("../")) {
                    goBackDir();
                } else if (item.endsWith("/")) {
                    enterDir(item);
                } else {
                    openFile(item);
                    showEditor();
                }
            }
        });
        fileListView.setOnItemLongClickListener((p, v, pos, id) -> {
            if (pos >= 0 && pos < fileList.size()) {
                String item = fileList.get(pos);
                if (!item.equals("../")) {
                    showItemOptionsDialog(item);
                }
            }
            return true;
        });
    }

    // ==================== 文件操作 ====================

    private void openDefaultFile() {
        String[] entries = projectManager.listRootFilesAndDirs(currentProjectName);
        if (entries != null && entries.length > 0) {
            String htmlFile = null;
            for (String entry : entries) {
                if (!entry.endsWith("/") && (entry.endsWith(".html") || entry.endsWith(".htm"))) {
                    htmlFile = entry;
                    break;
                }
            }
            if (htmlFile != null) {
                currentFileName = htmlFile;
                loadFileContent();
            }
        } else {
            createDefaultFile();
        }
        updateCurrentFileDisplay();
        updateLanguageSettings();
        undoStack.clear();
        redoStack.clear();
    }

    private void loadFileContent() {
        String content = projectManager.loadFile(currentProjectName, currentFileName);
        if (content != null) htmlEditor.setText(content);
    }

    private void createDefaultFile() {
        currentFileName = "index.html";
        String html = "<!DOCTYPE html>\n<html>\n<head>\n    <title>My Page</title>\n</head>\n<body>\n    <h1>Hello World!</h1>\n</body>\n</html>";
        htmlEditor.setText(html);
        projectManager.saveFile(currentProjectName, currentFileName, html);
    }

    private void openFile(String fileName) {
        saveCurrentFileSilently();
        if (!currentDir.isEmpty()) {
            currentFileName = currentDir + "/" + fileName;
        } else {
            currentFileName = fileName;
        }
        String content = projectManager.loadFile(currentProjectName, currentFileName);
        if (content != null) {
            htmlEditor.setText(content);
            undoStack.clear();
            redoStack.clear();
            Toast.makeText(this, "已打开: " + fileName, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "无法打开文件", Toast.LENGTH_SHORT).show();
        }
        updateCurrentFileDisplay();
        updateLanguageSettings();
        loadFileList();
    }

    private void saveCurrentFile() {
        String content = htmlEditor.getText().toString();
        if (projectManager.saveFile(currentProjectName, currentFileName, content)) {
            Toast.makeText(this, "保存成功: " + getDisplayFileName(), Toast.LENGTH_SHORT).show();
            loadFileList();
        } else {
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveCurrentFileSilently() {
        String content = htmlEditor.getText().toString();
        if (content != null && !content.isEmpty()) {
            projectManager.saveFile(currentProjectName, currentFileName, content);
        }
    }

    private void loadFileList() {
        fileList.clear();
        String[] entries;

        if (currentDir.isEmpty()) {
            entries = projectManager.listRootFilesAndDirs(currentProjectName);
        } else {
            entries = projectManager.listFilesInDir(currentProjectName, currentDir);
        }

        // 只要不在根目录，就显示返回上级
        if (!currentDir.isEmpty()) {
            fileList.add("../");
        }

        if (entries != null) {
            for (String entry : entries) {
                fileList.add(entry);
            }
        }
        fileAdapter.notifyDataSetChanged();
        if (fileCountText != null) {
            fileCountText.setText(fileList.size() + " 项");
        }
    }

    private void enterDir(String dirName) {
        saveCurrentFileSilently();
        dirStack.push(currentDir);
        currentDir = currentDir.isEmpty() ? dirName.substring(0, dirName.length() - 1)
                : currentDir + "/" + dirName.substring(0, dirName.length() - 1);
        loadFileList();
    }

    private void goBackDir() {
        if (!dirStack.isEmpty()) {
            currentDir = dirStack.pop();
        } else {
            currentDir = "";
        }
        loadFileList();
    }

    private String getDisplayFileName() {
        int lastSlash = currentFileName.lastIndexOf('/');
        if (lastSlash >= 0) {
            return currentFileName.substring(lastSlash + 1);
        }
        return currentFileName;
    }

    private void updateCurrentFileDisplay() {
        if (currentFileText != null) {
            currentFileText.setText(getDisplayFileName());
        }
    }

    private void updateLanguageSettings() {
        String lang = SyntaxHighlighter.getLanguageFromFileName(currentFileName);
        if (syntaxHighlighter != null) syntaxHighlighter.setLanguage(lang);
        if (autoCompleteManager != null) autoCompleteManager.setLanguage(lang);
    }

    // ==================== 新建对话框 ====================

    private void showNewDialog() {
        new AlertDialog.Builder(this)
            .setTitle("新建")
            .setItems(new String[]{"新建文件", "新建文件夹"}, (d, w) -> {
                switch (w) {
                    case 0: showCreateFileDialog(); break;
                    case 1: showCreateFolderDialog(); break;
                }
            }).show();
    }

    private void showCreateFileDialog() {
        final EditText input = new EditText(this);
        input.setHint("文件名（如：style.css）");
        input.setPadding(32, 16, 32, 16);
        input.setTextColor(0xFF000000);

        new AlertDialog.Builder(this).setTitle("新建文件").setView(input)
            .setPositiveButton("创建", (d, w) -> {
                String name = input.getText().toString().trim();
                if (!name.isEmpty()) createNewFile(name);
                else Toast.makeText(this, "文件名不能为空", Toast.LENGTH_SHORT).show();
            }).setNegativeButton("取消", null).show();
    }

    private void showCreateFolderDialog() {
        final EditText input = new EditText(this);
        input.setHint("文件夹名称");
        input.setPadding(32, 16, 32, 16);
        input.setTextColor(0xFF000000);

        new AlertDialog.Builder(this).setTitle("新建文件夹").setView(input)
            .setPositiveButton("创建", (d, w) -> {
                String name = input.getText().toString().trim();
                if (!name.isEmpty()) createNewFolder(name);
                else Toast.makeText(this, "文件夹名不能为空", Toast.LENGTH_SHORT).show();
            }).setNegativeButton("取消", null).show();
    }

    private void createNewFile(String fileName) {
        saveCurrentFileSilently();
        if (!currentDir.isEmpty()) {
            currentFileName = currentDir + "/" + fileName;
        } else {
            currentFileName = fileName;
        }
        String content = getInitialContent(fileName);
        htmlEditor.setText(content);
        projectManager.saveFile(currentProjectName, currentFileName, content);
        updateCurrentFileDisplay();
        updateLanguageSettings();
        loadFileList();
        undoStack.clear();
        redoStack.clear();
        Toast.makeText(this, "文件 '" + fileName + "' 创建成功", Toast.LENGTH_SHORT).show();
    }

    private void createNewFolder(String folderName) {
        String path = currentDir.isEmpty() ? folderName : currentDir + "/" + folderName;
        boolean success = projectManager.createDirectory(currentProjectName, path);
        if (success) {
            loadFileList();
            Toast.makeText(this, "文件夹 '" + folderName + "' 创建成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "文件夹创建失败，可能已存在", Toast.LENGTH_SHORT).show();
        }
    }

    private String getInitialContent(String fileName) {
        if (fileName.endsWith(".html") || fileName.endsWith(".htm"))
            return "<!DOCTYPE html>\n<html>\n<head>\n    <title>New Page</title>\n</head>\n<body>\n    \n</body>\n</html>";
        if (fileName.endsWith(".css"))
            return "/* " + fileName + " */\n\n* {\n    margin: 0;\n    padding: 0;\n    box-sizing: border-box;\n}\n\nbody {\n    font-family: Arial, sans-serif;\n}\n";
        if (fileName.endsWith(".js"))
            return "// " + fileName + "\n\nconsole.log('Hello World');\n";
        if (fileName.endsWith(".json")) return "{\n    \n}";
        if (fileName.endsWith(".md")) return "# " + fileName + "\n\n";
        if (fileName.endsWith(".py")) return "# " + fileName + "\n\nprint('Hello World')\n";
        return "";
    }

    // ==================== 项目选项对话框 ====================

    private void showItemOptionsDialog(final String itemName) {
        boolean isDir = itemName.endsWith("/");
        String displayName = isDir ? itemName.substring(0, itemName.length() - 1) : itemName;

        String[] options;
        if (isDir) {
            options = new String[]{"打开文件夹", "删除文件夹"};
        } else {
            options = new String[]{"打开文件", "重命名文件", "删除文件"};
        }

        new AlertDialog.Builder(this).setTitle(displayName)
            .setItems(options, (d, w) -> {
                if (isDir) {
                    switch (w) {
                        case 0: enterDir(itemName); break;
                        case 1: showDeleteFolderDialog(itemName); break;
                    }
                } else {
                    switch (w) {
                        case 0: openFile(itemName); showEditor(); break;
                        case 1: showRenameFileDialog(itemName); break;
                        case 2: showDeleteFileDialog(itemName); break;
                    }
                }
            }).show();
    }

    private void showRenameFileDialog(final String oldName) {
        final EditText input = new EditText(this);
        input.setText(oldName);
        input.setPadding(32, 16, 32, 16);
        input.setTextColor(0xFF000000);
        new AlertDialog.Builder(this).setTitle("重命名文件").setView(input)
            .setPositiveButton("重命名", (d, w) -> {
                String newName = input.getText().toString().trim();
                if (!newName.isEmpty() && !newName.equals(oldName)) {
                    String oldPath = currentDir.isEmpty() ? oldName : currentDir + "/" + oldName;
                    String newPath = currentDir.isEmpty() ? newName : currentDir + "/" + newName;
                    if (projectManager.renameFile(currentProjectName, oldPath, newPath)) {
                        if (currentFileName.equals(oldPath)) currentFileName = newPath;
                        updateCurrentFileDisplay();
                        updateLanguageSettings();
                        loadFileList();
                        Toast.makeText(this, "重命名成功", Toast.LENGTH_SHORT).show();
                    } else Toast.makeText(this, "重命名失败", Toast.LENGTH_SHORT).show();
                }
            }).setNegativeButton("取消", null).show();
    }

    private void showDeleteFileDialog(final String fileName) {
        new AlertDialog.Builder(this).setTitle("删除文件")
            .setMessage("确定要删除 '" + fileName + "' 吗？")
            .setPositiveButton("删除", (d, w) -> confirmDeleteFile(fileName))
            .setNegativeButton("取消", null).show();
    }

    private void showDeleteFolderDialog(final String folderName) {
        String displayName = folderName.endsWith("/") ? folderName.substring(0, folderName.length() - 1) : folderName;
        String path = currentDir.isEmpty() ? folderName : currentDir + "/" + folderName;

        new AlertDialog.Builder(this).setTitle("删除文件夹")
            .setMessage("确定要删除文件夹 '" + displayName + "' 及其所有内容吗？\n此操作不可撤销！")
            .setPositiveButton("删除", (d, w) -> {
                if (projectManager.deleteFile(currentProjectName, path)) {
                    loadFileList();
                    Toast.makeText(this, "文件夹已删除", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
                }
            }).setNegativeButton("取消", null).show();
    }

    private void confirmDeleteFile(String fileName) {
        String path = currentDir.isEmpty() ? fileName : currentDir + "/" + fileName;
        if (!projectManager.deleteFile(currentProjectName, path)) {
            Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
            return;
        }
        loadFileList();
        if (currentFileName.equals(path)) {
            if (fileList.size() > 0) {
                String first = fileList.get(0);
                if (!first.endsWith("/") && !first.equals("../")) openFile(first);
                else { createDefaultFile(); loadFileList(); }
            } else { createDefaultFile(); loadFileList(); }
            updateCurrentFileDisplay();
            updateLanguageSettings();
        }
        Toast.makeText(this, "文件已删除", Toast.LENGTH_SHORT).show();
    }

    // ==================== 编辑功能 ====================

    private void undo() {
        if (undoStack.isEmpty()) return;
        isUndoRedo = true;
        redoStack.push(htmlEditor.getText().toString());
        String text = undoStack.pop();
        htmlEditor.setText(text);
        htmlEditor.setSelection(text.length());
        isUndoRedo = false;
    }

    private void redo() {
        if (redoStack.isEmpty()) return;
        isUndoRedo = true;
        undoStack.push(htmlEditor.getText().toString());
        String text = redoStack.pop();
        htmlEditor.setText(text);
        htmlEditor.setSelection(text.length());
        isUndoRedo = false;
    }

    private void formatCode() {
        String code = htmlEditor.getText().toString();
        StringBuilder sb = new StringBuilder();
        int indent = 0;
        boolean inStr = false;
        char strChar = '"';
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (inStr) { sb.append(c); if (c == strChar && code.charAt(i-1) != '\\') inStr = false; continue; }
            if (c == '"' || c == '\'') { inStr = true; strChar = c; sb.append(c); continue; }
            if (c == '<') {
                if (i > 0 && code.charAt(i-1) != '\n') { sb.append('\n'); for (int j = 0; j < indent; j++) sb.append("    "); }
                sb.append(c);
            } else if (c == '>') {
                sb.append(c);
                if (i+1 < code.length() && code.charAt(i+1) != '<' && code.charAt(i+1) != '\n') {
                    sb.append('\n'); for (int j = 0; j < indent+1; j++) sb.append("    ");
                }
            } else if (c == '\n') { sb.append(c); if (i+1 < code.length()) for (int j = 0; j < indent; j++) sb.append("    "); }
            else { sb.append(c); }
        }
        htmlEditor.setText(sb.toString());
        Toast.makeText(this, "已格式化", Toast.LENGTH_SHORT).show();
    }

    private void showSearchDialog() {
        final EditText input = new EditText(this);
        input.setHint("搜索...");
        input.setPadding(32, 16, 32, 16);
        input.setTextColor(0xFF000000);
        new AlertDialog.Builder(this).setTitle("查找").setView(input)
            .setPositiveButton("查找", (d, w) -> {
                String q = input.getText().toString();
                if (!q.isEmpty()) {
                    int idx = htmlEditor.getText().toString().indexOf(q);
                    if (idx >= 0) { htmlEditor.setSelection(idx, idx+q.length()); Toast.makeText(this, "找到", Toast.LENGTH_SHORT).show(); }
                    else Toast.makeText(this, "未找到", Toast.LENGTH_SHORT).show();
                }
            }).setNegativeButton("取消", null).show();
    }

    private void confirmClear() {
        new AlertDialog.Builder(this).setTitle("清空编辑器")
            .setMessage("确定要清空当前内容吗？")
            .setPositiveButton("确定", (d, w) -> { htmlEditor.setText(""); Toast.makeText(this, "已清空", Toast.LENGTH_SHORT).show(); })
            .setNegativeButton("取消", null).show();
    }

    // ==================== 视图切换 ====================

    private void toggleFileList() {
        if (fileListPanel.getVisibility() == View.VISIBLE) showEditor();
        else { currentDir = ""; dirStack.clear(); showFileList(); }
    }

    private void showEditor() {
        if (editorPanel != null) editorPanel.setVisibility(View.VISIBLE);
        if (fileListPanel != null) fileListPanel.setVisibility(View.GONE);
    }

    private void showFileList() {
        loadFileList();
        if (editorPanel != null) editorPanel.setVisibility(View.GONE);
        if (fileListPanel != null) fileListPanel.setVisibility(View.VISIBLE);
    }

    private void runPreview() {
        String htmlContent = htmlEditor.getText().toString();
        if (htmlContent.isEmpty()) { Toast.makeText(this, "没有可预览的内容", Toast.LENGTH_SHORT).show(); return; }
        saveCurrentFileSilently();
        Intent intent = new Intent(EditorActivity.this, PreviewActivity.class);
        intent.putExtra("projectName", currentProjectName);
        intent.putExtra("fileName", currentFileName);
        intent.putExtra("htmlContent", htmlContent);
        startActivity(intent);
    }
}