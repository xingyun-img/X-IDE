package com.xingyun.ide;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;
import java.io.*;

public class FileManager {
    
    private Context context;
    private static final String FILE_EXTENSION = ".html";
    
    public FileManager(Context context) {
        this.context = context;
    }
    
    public boolean saveToFile(String content, String fileName) {
        try {
            File dir = new File(Environment.getExternalStorageDirectory() + "/HTML_Editor");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            File file = new File(dir, fileName + FILE_EXTENSION);
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.close();
            
            Toast.makeText(context, "Saved to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error saving file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }
    
    public String loadFromFile(String filePath) {
        try {
            File file = new File(filePath);
            StringBuilder content = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
            
            return content.toString();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error loading file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }
}