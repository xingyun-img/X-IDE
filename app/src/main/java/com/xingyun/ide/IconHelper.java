package com.xingyun.ide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import java.io.InputStream;

public class IconHelper {

    public static Drawable getIcon(Context context, String fileName) {
        String iconName = getIconName(fileName);
        try {
            InputStream is = context.getAssets().open("icons/" + iconName);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            is.close();
            float density = context.getResources().getDisplayMetrics().density;
            int size = (int) (24 * density);
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, size, size, true);
            return new BitmapDrawable(context.getResources(), scaled);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getIconName(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.equals("folder")) return "icon_folder.png";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "icon_html.png";
        if (lower.endsWith(".css")) return "icon_css.png";
        if (lower.endsWith(".js") || lower.endsWith(".mjs") || lower.endsWith(".ts") || lower.endsWith(".jsx") || lower.endsWith(".tsx")) return "icon_js.png";
        if (lower.endsWith(".json")) return "icon_json.png";
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) return "icon_md.png";
        if (lower.endsWith(".py")) return "icon_py.png";
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif") || lower.endsWith(".svg") || lower.endsWith(".webp") || lower.endsWith(".bmp") || lower.endsWith(".ico")) return "icon_img.png";
        return "icon_file.png";
    }
}