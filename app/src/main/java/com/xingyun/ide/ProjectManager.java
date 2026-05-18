package com.xingyun.ide;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ProjectManager {
    
    private static final String BASE_PATH = "/storage/emulated/0/X-IDE/";
    
    public ProjectManager() {
        ensureBaseDirectory();
    }
    
    private void ensureBaseDirectory() {
        try {
            File baseDir = new File(BASE_PATH);
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public boolean createProject(String projectName) {
        try {
            File projectDir = new File(BASE_PATH + projectName);
            if (!projectDir.exists()) {
                boolean created = projectDir.mkdirs();
                if (created) {
                    createProjectStructure(projectName);
                    createProjectConfig(projectName);
                    
                    String defaultHtml = "<!DOCTYPE html>\n" +
                                        "<html lang=\"zh-CN\">\n" +
                                        "<head>\n" +
                                        "    <meta charset=\"UTF-8\">\n" +
                                        "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                                        "    <title>" + projectName + "</title>\n" +
                                        "    <link rel=\"stylesheet\" href=\"css/style.css\">\n" +
                                        "</head>\n" +
                                        "<body>\n" +
                                        "    <h1>" + projectName + "</h1>\n" +
                                        "    <p>开始构建你的项目吧！</p>\n" +
                                        "    <script src=\"js/script.js\"></script>\n" +
                                        "</body>\n" +
                                        "</html>";
                    saveFile(projectName, "index.html", defaultHtml);
                    
                    String defaultCss = "/* Main Styles */\n\n" +
                                       "* {\n    margin: 0;\n    padding: 0;\n    box-sizing: border-box;\n}\n\n" +
                                       "body {\n    font-family: Arial, sans-serif;\n    line-height: 1.6;\n" +
                                       "    color: #333;\n    background: #f4f4f4;\n    padding: 20px;\n}\n\n" +
                                       "h1 {\n    color: #2c3e50;\n    margin-bottom: 20px;\n}\n";
                    saveFile(projectName, "css/style.css", defaultCss);
                    
                    String defaultJs = "// Main JavaScript\n\n" +
                                      "document.addEventListener('DOMContentLoaded', function() {\n" +
                                      "    console.log('" + projectName + " loaded!');\n" +
                                      "});\n";
                    saveFile(projectName, "js/script.js", defaultJs);
                }
                return created;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private void createProjectStructure(String projectName) {
        String[] subDirs = {"css", "js", "images", "assets", "fonts"};
        for (String dir : subDirs) {
            File subDir = new File(BASE_PATH + projectName + "/" + dir);
            if (!subDir.exists()) {
                subDir.mkdirs();
            }
        }
    }
    
    private void createProjectConfig(String projectName) {
        String config = "{\n" +
                       "  \"projectName\": \"" + projectName + "\",\n" +
                       "  \"version\": \"1.0.0\",\n" +
                       "  \"created\": \"" + System.currentTimeMillis() + "\"\n" +
                       "}";
        saveFile(projectName, "project.json", config);
    }
    
    public String[] listProjects() {
        List<String> projects = new ArrayList<>();
        try {
            File baseDir = new File(BASE_PATH);
            if (baseDir.exists() && baseDir.isDirectory()) {
                File[] files = baseDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory() && !file.getName().startsWith(".")) {
                            projects.add(file.getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return projects.toArray(new String[0]);
    }
    
    public String[] listRootFilesAndDirs(String projectName) {
        List<String> dirs = new ArrayList<>();
        List<String> files = new ArrayList<>();
        try {
            File projectDir = new File(BASE_PATH + projectName);
            if (projectDir.exists() && projectDir.isDirectory()) {
                File[] entries = projectDir.listFiles();
                if (entries != null) {
                    for (File entry : entries) {
                        if (entry.getName().startsWith(".")) continue;
                        if (entry.getName().equals("project.json")) continue;
                        if (entry.isDirectory()) {
                            dirs.add(entry.getName() + "/");
                        } else {
                            files.add(entry.getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<String> all = new ArrayList<>();
        all.addAll(dirs);
        all.addAll(files);
        return all.toArray(new String[0]);
    }
    
    public String[] listFilesInDir(String projectName, String dirPath) {
        List<String> dirs = new ArrayList<>();
        List<String> files = new ArrayList<>();
        try {
            File dir = new File(BASE_PATH + projectName, dirPath);
            if (dir.exists() && dir.isDirectory()) {
                File[] entries = dir.listFiles();
                if (entries != null) {
                    for (File entry : entries) {
                        if (entry.getName().startsWith(".")) continue;
                        if (entry.getName().equals("project.json")) continue;
                        if (entry.isDirectory()) {
                            dirs.add(entry.getName() + "/");
                        } else {
                            files.add(entry.getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<String> all = new ArrayList<>();
        all.addAll(dirs);
        all.addAll(files);
        return all.toArray(new String[0]);
    }
    
    public String[] listRootFiles(String projectName) {
        List<String> fileList = new ArrayList<>();
        try {
            File projectDir = new File(BASE_PATH + projectName);
            if (projectDir.exists() && projectDir.isDirectory()) {
                File[] files = projectDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && !file.getName().equals("project.json") && !file.getName().startsWith(".")) {
                            fileList.add(file.getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileList.toArray(new String[0]);
    }
    
    public String[] listAllFiles(String projectName) {
        List<String> fileList = new ArrayList<>();
        try {
            File projectDir = new File(BASE_PATH + projectName);
            if (projectDir.exists() && projectDir.isDirectory()) {
                listFilesRecursive(projectDir, projectDir, fileList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileList.toArray(new String[0]);
    }
    
    private void listFilesRecursive(File baseDir, File currentDir, List<String> fileList) {
        File[] files = currentDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && !file.getName().startsWith(".")) {
                    listFilesRecursive(baseDir, file, fileList);
                } else if (file.isFile() && !file.getName().equals("project.json") && !file.getName().startsWith(".")) {
                    String relativePath = file.getAbsolutePath()
                        .substring(baseDir.getAbsolutePath().length() + 1);
                    fileList.add(relativePath);
                }
            }
        }
    }
    
    public boolean saveFile(String projectName, String fileName, String content) {
        FileWriter writer = null;
        try {
            File file = new File(BASE_PATH + projectName, fileName);
            
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            writer = new FileWriter(file);
            writer.write(content);
            writer.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public String loadFile(String projectName, String fileName) {
        BufferedReader reader = null;
        try {
            File file = new File(BASE_PATH + projectName, fileName);
            if (!file.exists()) {
                return null;
            }
            
            StringBuilder content = new StringBuilder();
            reader = new BufferedReader(new FileReader(file));
            String line;
            
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            
            return content.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public boolean deleteFile(String projectName, String fileName) {
        try {
            File file = new File(BASE_PATH + projectName, fileName);
            if (file.exists()) {
                if (file.isDirectory()) {
                    return deleteDirectory(file);
                }
                return file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean renameFile(String projectName, String oldName, String newName) {
        try {
            File oldFile = new File(BASE_PATH + projectName, oldName);
            File newFile = new File(BASE_PATH + projectName, newName);
            
            File parentDir = newFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            return oldFile.exists() && oldFile.renameTo(newFile);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean createDirectory(String projectName, String dirPath) {
        try {
            File dir = new File(BASE_PATH + projectName, dirPath);
            if (!dir.exists()) {
                return dir.mkdirs();
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean deleteProject(String projectName) {
        try {
            File projectDir = new File(BASE_PATH + projectName);
            if (projectDir.exists()) {
                return deleteDirectory(projectDir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    private boolean deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return directory.delete();
    }
    
    public String getProjectInfo(String projectName) {
        StringBuilder info = new StringBuilder();
        info.append("项目名称: ").append(projectName).append("\n\n");
        info.append("存储路径: \n").append(BASE_PATH).append(projectName).append("\n");
        
        File projectDir = new File(BASE_PATH + projectName);
        if (projectDir.exists()) {
            int fileCount = 0;
            int dirCount = 0;
            
            File[] files = projectDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        dirCount++;
                    } else {
                        fileCount++;
                    }
                }
            }
            
            info.append("\n文件数量: ").append(fileCount).append(" 个\n");
            info.append("子文件夹: ").append(dirCount).append(" 个\n");
        }
        
        return info.toString();
    }
}