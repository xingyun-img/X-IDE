package com.xingyun.ide;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class ProjectListActivity extends AppCompatActivity {
    
    private static final int REQUEST_CODE_STORAGE = 100;
    private static final int REQUEST_CODE_MANAGE_STORAGE = 101;
    
    private ListView projectListView;
    private LinearLayout emptyView;
    private TextView projectCountText;
    private Button btnNewProject;
    private ProjectManager projectManager;
    private ArrayAdapter<String> adapter;
    private List<String> projectList;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_list);
        
        try {
            // 首先检查权限
            checkPermissions();
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "初始化失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+
            if (!Environment.isExternalStorageManager()) {
                requestManageStoragePermission();
            } else {
                initApp();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-10
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    REQUEST_CODE_STORAGE);
            } else {
                initApp();
            }
        } else {
            // Android 5 及以下
            initApp();
        }
    }
    
    private void requestManageStoragePermission() {
        new AlertDialog.Builder(this)
            .setTitle("需要存储权限")
            .setMessage("X IDE 需要访问存储空间来保存您的项目文件。\n\n请点击\"确定\"并在设置中允许\"所有文件访问\"权限。")
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE);
                }
            })
            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(ProjectListActivity.this, 
                        "需要存储权限才能使用", Toast.LENGTH_LONG).show();
                    // 即使没有权限也尝试初始化
                    initApp();
                }
            })
            .setCancelable(false)
            .show();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_MANAGE_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show();
                }
            }
            initApp();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "存储权限被拒绝，部分功能可能无法使用", Toast.LENGTH_LONG).show();
            }
            initApp();
        }
    }
    
    private void initApp() {
        try {
            projectManager = new ProjectManager();
            projectList = new ArrayList<>();
            
            projectListView = findViewById(R.id.projectListView);
            emptyView = findViewById(R.id.emptyView);
            projectCountText = findViewById(R.id.projectCountText);
            btnNewProject = findViewById(R.id.btnNewProject);
            
            adapter = new ArrayAdapter<String>(this, 
                android.R.layout.simple_list_item_1, 
                projectList) {
                @Override
                public View getView(int position, View convertView, android.view.ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    TextView textView = view.findViewById(android.R.id.text1);
                    if (textView != null) {
                        textView.setTextColor(0xFFFFFFFF);
                        textView.setTextSize(16);
                        textView.setPadding(32, 24, 32, 24);
                    }
                    return view;
                }
            };
            
            projectListView.setAdapter(adapter);
            setupListeners();
            loadProjects();
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "初始化失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (projectManager != null) {
            loadProjects();
        }
    }
    
    private void setupListeners() {
        btnNewProject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNewProjectDialog();
            }
        });
        
        projectListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < projectList.size()) {
                    String projectName = projectList.get(position);
                    openProject(projectName);
                }
            }
        });
        
        projectListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < projectList.size()) {
                    String projectName = projectList.get(position);
                    showProjectOptionsDialog(projectName);
                }
                return true;
            }
        });
    }
    
    private void loadProjects() {
        try {
            projectList.clear();
            String[] projects = projectManager.listProjects();
            
            if (projects != null && projects.length > 0) {
                for (String project : projects) {
                    if (project != null && !project.isEmpty()) {
                        projectList.add(project);
                    }
                }
            }
            
            adapter.notifyDataSetChanged();
            
            if (projectList.size() > 0) {
                projectListView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
                projectCountText.setText("项目列表 (" + projectList.size() + ")");
            } else {
                projectListView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
                projectCountText.setText("项目列表 (0)");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void showNewProjectDialog() {
        final EditText input = new EditText(this);
        input.setHint("输入项目名称（如：MyWebsite）");
        input.setPadding(32, 16, 32, 16);
        input.setTextColor(0xFF000000);
        input.setHintTextColor(0xFF888888);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("创建新项目");
        builder.setView(input);
        builder.setPositiveButton("创建", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String projectName = input.getText().toString().trim();
                if (!projectName.isEmpty()) {
                    boolean success = projectManager.createProject(projectName);
                    if (success) {
                        Toast.makeText(ProjectListActivity.this, 
                            "项目 '" + projectName + "' 创建成功", Toast.LENGTH_SHORT).show();
                        loadProjects();
                        openProject(projectName);
                    } else {
                        Toast.makeText(ProjectListActivity.this, 
                            "创建失败，请检查存储权限", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(ProjectListActivity.this, 
                        "项目名称不能为空", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    
    private void showProjectOptionsDialog(final String projectName) {
        final String[] options = {"打开项目", "项目详情", "删除项目"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(projectName);
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        openProject(projectName);
                        break;
                    case 1:
                        showProjectInfo(projectName);
                        break;
                    case 2:
                        showDeleteConfirmDialog(projectName);
                        break;
                }
            }
        });
        builder.show();
    }
    
    private void showProjectInfo(String projectName) {
        String info = projectManager.getProjectInfo(projectName);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("项目信息");
        builder.setMessage(info);
        builder.setPositiveButton("确定", null);
        builder.show();
    }
    
    private void showDeleteConfirmDialog(final String projectName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("删除项目");
        builder.setMessage("确定要删除项目 '" + projectName + "' 吗？\n\n此操作不可撤销！");
        builder.setPositiveButton("删除", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (projectManager.deleteProject(projectName)) {
                    Toast.makeText(ProjectListActivity.this, 
                        "项目已删除", Toast.LENGTH_SHORT).show();
                    loadProjects();
                } else {
                    Toast.makeText(ProjectListActivity.this, 
                        "删除失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    
    private void openProject(String projectName) {
        Intent intent = new Intent(ProjectListActivity.this, EditorActivity.class);
        intent.putExtra("projectName", projectName);
        startActivity(intent);
    }
}