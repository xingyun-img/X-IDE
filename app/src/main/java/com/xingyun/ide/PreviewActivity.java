package com.xingyun.ide;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PreviewActivity extends AppCompatActivity {
    
    private WebView previewWebView;
    private ImageButton btnDebug, btnRefresh, btnBack;
    private TextView titleText, debugStatusText;
    private LinearLayout debugPanel;
    
    // 调试标签
    private Button btnLogTab, btnNetworkTab, btnConsoleTab;
    private Button btnLogAll, btnLogInfo, btnLogWarn, btnLogError;
    private Button btnClearDebug, btnExecute;
    
    // 日志和网络列表
    private ListView logListView, networkListView;
    private LinearLayout logFilterBar, networkHeader;
    private LinearLayout consolePanel;
    private ListView consoleOutputList;
    private EditText consoleInput;
    
    // 数据
    private List<LogEntry> allLogs = new ArrayList<>();
    private List<LogEntry> filteredLogs = new ArrayList<>();
    private List<NetworkEntry> networkLogs = new ArrayList<>();
    private List<String> consoleOutputs = new ArrayList<>();
    
    private ArrayAdapter<String> logAdapter;
    private ArrayAdapter<String> networkAdapter;
    private ArrayAdapter<String> consoleAdapter;
    private List<String> logDisplayList = new ArrayList<>();
    private List<String> networkDisplayList = new ArrayList<>();
    
    private boolean isDebugVisible = false;
    private String currentLogFilter = "all";
    private String currentTab = "log"; // log, network, console
    
    private ProjectManager projectManager;
    private String projectName;
    private String fileName;
    private String htmlContent;
    
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        
        try {
            projectManager = new ProjectManager();
            projectName = getIntent().getStringExtra("projectName");
            fileName = getIntent().getStringExtra("fileName");
            htmlContent = getIntent().getStringExtra("htmlContent");
            
            if (projectName == null) projectName = "";
            if (fileName == null) fileName = "";
            
            initializeViews();
            setupAdapters();
            setupListeners();
            setupWebView();
            loadContent();
            
            addLog("info", "预览页面已启动");
            addLog("info", "项目: " + projectName + ", 文件: " + fileName);
            addConsoleOutput("系统", "控制台已就绪，可输入 JavaScript 代码执行");
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "预览初始化失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    // ==================== 初始化 ====================
    
    private void initializeViews() {
        previewWebView = findViewById(R.id.previewWebView);
        btnDebug = findViewById(R.id.btnDebug);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnBack = findViewById(R.id.btnBack);
        titleText = findViewById(R.id.titleText);
        debugPanel = findViewById(R.id.debugPanel);
        debugStatusText = findViewById(R.id.debugStatusText);
        
        btnLogTab = findViewById(R.id.btnLogTab);
        btnNetworkTab = findViewById(R.id.btnNetworkTab);
        btnConsoleTab = findViewById(R.id.btnConsoleTab);
        btnLogAll = findViewById(R.id.btnLogAll);
        btnLogInfo = findViewById(R.id.btnLogInfo);
        btnLogWarn = findViewById(R.id.btnLogWarn);
        btnLogError = findViewById(R.id.btnLogError);
        btnClearDebug = findViewById(R.id.btnClearDebug);
        btnExecute = findViewById(R.id.btnExecute);
        
        logListView = findViewById(R.id.logListView);
        networkListView = findViewById(R.id.networkListView);
        logFilterBar = findViewById(R.id.logFilterBar);
        networkHeader = findViewById(R.id.networkHeader);
        consolePanel = findViewById(R.id.consolePanel);
        consoleOutputList = findViewById(R.id.consoleOutputList);
        consoleInput = findViewById(R.id.consoleInput);
        
        if (titleText != null) {
            String title = projectName;
            if (!fileName.isEmpty()) {
                title += " / " + fileName;
            }
            titleText.setText(title);
        }
    }
    
    private void setupAdapters() {
        // 日志适配器
        logAdapter = new ArrayAdapter<String>(this, 
            android.R.layout.simple_list_item_1, logDisplayList) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                if (textView != null) {
                    textView.setTextSize(11);
                    textView.setTypeface(android.graphics.Typeface.MONOSPACE);
                    textView.setPadding(12, 6, 12, 6);
                    
                    String text = logDisplayList.get(position);
                    if (text.contains("[信息]")) {
                        textView.setTextColor(0xFF2196F3);
                    } else if (text.contains("[警告]")) {
                        textView.setTextColor(0xFFFF9800);
                    } else if (text.contains("[错误]")) {
                        textView.setTextColor(0xFFF44336);
                    } else {
                        textView.setTextColor(0xFFD4D4D4);
                    }
                }
                return view;
            }
        };
        logListView.setAdapter(logAdapter);
        
        // 网络适配器
        networkAdapter = new ArrayAdapter<String>(this, 
            android.R.layout.simple_list_item_1, networkDisplayList) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                if (textView != null) {
                    textView.setTextSize(10);
                    textView.setTypeface(android.graphics.Typeface.MONOSPACE);
                    textView.setPadding(8, 4, 8, 4);
                    textView.setTextColor(0xFFD4D4D4);
                }
                return view;
            }
        };
        networkListView.setAdapter(networkAdapter);
        
        // 控制台适配器
        consoleAdapter = new ArrayAdapter<String>(this,
            android.R.layout.simple_list_item_1, consoleOutputs) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                if (textView != null) {
                    textView.setTextSize(11);
                    textView.setTypeface(android.graphics.Typeface.MONOSPACE);
                    textView.setPadding(12, 6, 12, 6);
                    
                    String text = consoleOutputs.get(position);
                    if (text.startsWith("> ")) {
                        textView.setTextColor(0xFF4CAF50);
                    } else if (text.startsWith("< ")) {
                        textView.setTextColor(0xFFDCDCAA);
                    } else if (text.startsWith("[系统]")) {
                        textView.setTextColor(0xFF888888);
                    } else if (text.startsWith("[错误]")) {
                        textView.setTextColor(0xFFF44336);
                    } else {
                        textView.setTextColor(0xFFD4D4D4);
                    }
                }
                return view;
            }
        };
        consoleOutputList.setAdapter(consoleAdapter);
    }
    
    // ==================== WebView 设置 ====================
    
    private void setupWebView() {
        if (previewWebView == null) return;
        
        previewWebView.getSettings().setJavaScriptEnabled(true);
        previewWebView.getSettings().setAllowFileAccess(true);
        previewWebView.getSettings().setDomStorageEnabled(true);
        previewWebView.getSettings().setLoadWithOverviewMode(true);
        previewWebView.getSettings().setUseWideViewPort(true);
        
        // 添加 JavaScript 接口
        previewWebView.addJavascriptInterface(new JSInterface(), "AndroidConsole");
        
        previewWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                addLog("info", "页面标题: " + title);
            }
            
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                String level;
                switch (consoleMessage.messageLevel()) {
                    case ERROR:
                        level = "error";
                        break;
                    case WARNING:
                        level = "warn";
                        break;
                    default:
                        level = "info";
                        break;
                }
                addLog(level, "Console: " + consoleMessage.message());
                return true;
            }
        });
        
        previewWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                addLog("info", "页面加载完成");
                if (debugStatusText != null) {
                    debugStatusText.setText("加载完成");
                }
                // 注入控制台捕获代码
                injectConsoleCapture();
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                addLog("error", "加载错误: " + description);
                addNetworkEntry(failingUrl, "GET", errorCode, "error", 0, 0);
            }
            
            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
                addNetworkEntry(url, "GET", 200, getResourceType(url), 0, 0);
            }
        });
    }
    
    private void injectConsoleCapture() {
        String js = "(function() {" +
            "if (!window.__consoleCaptured) {" +
            "    window.__consoleCaptured = true;" +
            "    var methods = ['log', 'info', 'warn', 'error'];" +
            "    methods.forEach(function(m) {" +
            "        var old = console[m];" +
            "        console[m] = function() {" +
            "            var args = Array.prototype.slice.call(arguments);" +
            "            var msg = args.map(function(a) {" +
            "                try { return typeof a === 'object' ? JSON.stringify(a) : String(a); }" +
            "                catch(e) { return String(a); }" +
            "            }).join(' ');" +
            "            if (window.AndroidConsole) {" +
            "                window.AndroidConsole.onMessage(m, msg);" +
            "            }" +
            "            old.apply(console, arguments);" +
            "        };" +
            "    });" +
            "}})();";
        previewWebView.evaluateJavascript(js, null);
    }
    
    private void loadContent() {
        if (previewWebView == null) return;
        
        if (htmlContent != null && !htmlContent.isEmpty()) {
            addLog("info", "正在加载 HTML 内容 (" + htmlContent.length() + " 字符)");
            previewWebView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
        } else if (projectName != null && !projectName.isEmpty() && fileName != null && !fileName.isEmpty()) {
            String content = projectManager.loadFile(projectName, fileName);
            if (content != null) {
                addLog("info", "正在加载文件: " + fileName);
                previewWebView.loadDataWithBaseURL(null, content, "text/html", "UTF-8", null);
            } else {
                addLog("error", "无法加载文件: " + fileName);
                previewWebView.loadDataWithBaseURL(null, 
                    "<html><body style='padding:20px;'><h2>无法加载文件</h2></body></html>", 
                    "text/html", "UTF-8", null);
            }
        }
    }
    
    // ==================== JavaScript 接口 ====================
    
    private class JSInterface {
        @JavascriptInterface
        public void onMessage(String level, String message) {
            runOnUiThread(() -> {
                addConsoleOutput(level, message);
            });
        }
    }
    
    // ==================== 监听器 ====================
    
    private void setupListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
        
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> {
                addLog("info", "刷新页面");
                consoleOutputs.clear();
                consoleAdapter.notifyDataSetChanged();
                loadContent();
                Toast.makeText(this, "已刷新", Toast.LENGTH_SHORT).show();
            });
        }
        
        if (btnDebug != null) {
            btnDebug.setOnClickListener(v -> toggleDebugPanel());
        }
        
        // 调试标签切换
        if (btnLogTab != null) {
            btnLogTab.setOnClickListener(v -> switchTab("log"));
        }
        
        if (btnNetworkTab != null) {
            btnNetworkTab.setOnClickListener(v -> switchTab("network"));
        }
        
        if (btnConsoleTab != null) {
            btnConsoleTab.setOnClickListener(v -> switchTab("console"));
        }
        
        // 日志过滤
        if (btnLogAll != null) {
            btnLogAll.setOnClickListener(v -> filterLogs("all"));
        }
        
        if (btnLogInfo != null) {
            btnLogInfo.setOnClickListener(v -> filterLogs("info"));
        }
        
        if (btnLogWarn != null) {
            btnLogWarn.setOnClickListener(v -> filterLogs("warn"));
        }
        
        if (btnLogError != null) {
            btnLogError.setOnClickListener(v -> filterLogs("error"));
        }
        
        // 清除
        if (btnClearDebug != null) {
            btnClearDebug.setOnClickListener(v -> clearAllDebug());
        }
        
        // 执行 JS
        if (btnExecute != null) {
            btnExecute.setOnClickListener(v -> executeConsoleInput());
        }
        
        // 控制台输入键盘事件
        if (consoleInput != null) {
            consoleInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_GO || 
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    executeConsoleInput();
                    return true;
                }
                return false;
            });
        }
    }
    
    // ==================== 调试面板 ====================
    
    private void toggleDebugPanel() {
        if (debugPanel == null) return;
        
        if (isDebugVisible) {
            debugPanel.setVisibility(View.GONE);
            previewWebView.setVisibility(View.VISIBLE);
            isDebugVisible = false;
        } else {
            debugPanel.setVisibility(View.VISIBLE);
            previewWebView.setVisibility(View.GONE);
            isDebugVisible = true;
            switchTab(currentTab);
        }
    }
    
    private void switchTab(String tab) {
        currentTab = tab;
        
        // 重置标签按钮
        btnLogTab.setBackgroundColor(tab.equals("log") ? 0xFF4CAF50 : 0xFF333333);
        btnLogTab.setTextColor(tab.equals("log") ? 0xFFFFFFFF : 0xFF999999);
        btnNetworkTab.setBackgroundColor(tab.equals("network") ? 0xFF4CAF50 : 0xFF333333);
        btnNetworkTab.setTextColor(tab.equals("network") ? 0xFFFFFFFF : 0xFF999999);
        btnConsoleTab.setBackgroundColor(tab.equals("console") ? 0xFF4CAF50 : 0xFF333333);
        btnConsoleTab.setTextColor(tab.equals("console") ? 0xFFFFFFFF : 0xFF999999);
        
        // 切换面板
        logFilterBar.setVisibility(tab.equals("log") ? View.VISIBLE : View.GONE);
        logListView.setVisibility(tab.equals("log") ? View.VISIBLE : View.GONE);
        networkHeader.setVisibility(tab.equals("network") ? View.VISIBLE : View.GONE);
        networkListView.setVisibility(tab.equals("network") ? View.VISIBLE : View.GONE);
        consolePanel.setVisibility(tab.equals("console") ? View.VISIBLE : View.GONE);
        
        if (tab.equals("log")) {
            filterLogs(currentLogFilter);
        } else if (tab.equals("network")) {
            updateNetworkDisplay();
            if (debugStatusText != null) {
                debugStatusText.setText("网络请求: " + networkLogs.size() + " 条");
            }
        } else if (tab.equals("console")) {
            if (debugStatusText != null) {
                debugStatusText.setText("控制台 | 输入 JS 代码并执行");
            }
        }
    }
    
    private void filterLogs(String filter) {
        currentLogFilter = filter;
        
        btnLogAll.setBackgroundColor(filter.equals("all") ? 0xFF4CAF50 : 0xFF333333);
        btnLogInfo.setBackgroundColor(filter.equals("info") ? 0xFF2196F3 : 0xFF333333);
        btnLogWarn.setBackgroundColor(filter.equals("warn") ? 0xFFFF9800 : 0xFF333333);
        btnLogError.setBackgroundColor(filter.equals("error") ? 0xFFF44336 : 0xFF333333);
        
        logDisplayList.clear();
        for (LogEntry log : allLogs) {
            if (filter.equals("all") || log.level.equals(filter)) {
                logDisplayList.add(log.toString());
            }
        }
        logAdapter.notifyDataSetChanged();
        
        if (debugStatusText != null) {
            debugStatusText.setText("日志: " + logDisplayList.size() + " 条");
        }
    }
    
    private void updateNetworkDisplay() {
        networkDisplayList.clear();
        for (NetworkEntry entry : networkLogs) {
            networkDisplayList.add(entry.toDisplayString());
        }
        networkAdapter.notifyDataSetChanged();
    }
    
    private void clearAllDebug() {
        allLogs.clear();
        filteredLogs.clear();
        logDisplayList.clear();
        networkLogs.clear();
        networkDisplayList.clear();
        consoleOutputs.clear();
        logAdapter.notifyDataSetChanged();
        networkAdapter.notifyDataSetChanged();
        consoleAdapter.notifyDataSetChanged();
        if (debugStatusText != null) {
            debugStatusText.setText("已清除");
        }
    }
    
    // ==================== 控制台执行 ====================
    
    private void executeConsoleInput() {
        if (consoleInput == null || previewWebView == null) return;
        
        String script = consoleInput.getText().toString().trim();
        if (script.isEmpty()) return;
        
        // 显示输入的命令
        addConsoleOutput("input", script);
        
        // 清空输入框
        consoleInput.setText("");
        
        // 执行 JavaScript
        previewWebView.evaluateJavascript(script, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                runOnUiThread(() -> {
                    if (value != null && !value.equals("null") && !value.isEmpty()) {
                        addConsoleOutput("output", value);
                    }
                });
            }
        });
        
        if (debugStatusText != null) {
            debugStatusText.setText("已执行脚本");
        }
    }
    
    // ==================== 日志记录 ====================
    
    private void addLog(String level, String message) {
        LogEntry entry = new LogEntry(level, message);
        allLogs.add(entry);
        
        if (isDebugVisible && currentTab.equals("log") && 
            (currentLogFilter.equals("all") || currentLogFilter.equals(level))) {
            logDisplayList.add(entry.toString());
            logAdapter.notifyDataSetChanged();
        }
    }
    
    private void addNetworkEntry(String url, String method, int status, String type, long size, long time) {
        NetworkEntry entry = new NetworkEntry(url, method, status, type, size, time);
        networkLogs.add(entry);
        
        if (isDebugVisible && currentTab.equals("network")) {
            networkDisplayList.add(entry.toDisplayString());
            networkAdapter.notifyDataSetChanged();
        }
    }
    
    private void addConsoleOutput(String type, String message) {
        String prefix;
        switch (type) {
            case "input":
                prefix = "> ";
                break;
            case "output":
                prefix = "< ";
                break;
            case "error":
                prefix = "[错误] ";
                break;
            case "warn":
                prefix = "[警告] ";
                break;
            case "info":
            case "log":
                prefix = "[信息] ";
                break;
            default:
                prefix = "[系统] ";
                break;
        }
        consoleOutputs.add(prefix + message);
        
        // 限制输出行数
        while (consoleOutputs.size() > 500) {
            consoleOutputs.remove(0);
        }
        
        consoleAdapter.notifyDataSetChanged();
        if (consoleOutputList != null) {
            consoleOutputList.setSelection(consoleOutputs.size() - 1);
        }
    }
    
    private String getResourceType(String url) {
        if (url == null) return "unknown";
        String lower = url.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "html";
        if (lower.endsWith(".css")) return "css";
        if (lower.endsWith(".js")) return "js";
        if (lower.endsWith(".png")) return "png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "jpg";
        if (lower.endsWith(".svg")) return "svg";
        if (lower.endsWith(".json")) return "json";
        if (lower.endsWith(".woff") || lower.endsWith(".woff2")) return "font";
        if (lower.contains("api") || lower.contains("fetch")) return "xhr";
        return "other";
    }
    
    @Override
    public void onBackPressed() {
        if (isDebugVisible) {
            toggleDebugPanel();
        } else {
            super.onBackPressed();
        }
    }
    
    // ==================== 数据类 ====================
    
    private class LogEntry {
        String level;
        String message;
        String time;
        
        LogEntry(String level, String message) {
            this.level = level;
            this.message = message;
            this.time = timeFormat.format(new Date());
        }
        
        @Override
        public String toString() {
            String prefix;
            switch (level) {
                case "info": prefix = "[信息]"; break;
                case "warn": prefix = "[警告]"; break;
                case "error": prefix = "[错误]"; break;
                default: prefix = "[" + level + "]"; break;
            }
            return time + " " + prefix + " " + message;
        }
    }
    
    private class NetworkEntry {
        String url;
        String method;
        int status;
        String type;
        long size;
        long time;
        
        NetworkEntry(String url, String method, int status, String type, long size, long time) {
            this.url = url;
            this.method = method;
            this.status = status;
            this.type = type;
            this.size = size;
            this.time = time;
        }
        
        String toDisplayString() {
            String shortUrl = url;
            if (url != null && url.length() > 35) {
                shortUrl = "..." + url.substring(url.length() - 32);
            }
            String sizeStr = size > 0 ? (size < 1024 ? size + "B" : (size / 1024) + "KB") : "-";
            String timeStr = time > 0 ? time + "ms" : "-";
            String statusStr = status > 0 ? String.valueOf(status) : "-";
            
            return String.format("%-30s %-4s %-4s %-5s %-5s %s",
                shortUrl != null ? shortUrl : "-",
                method,
                statusStr,
                type,
                sizeStr,
                timeStr);
        }
    }
}