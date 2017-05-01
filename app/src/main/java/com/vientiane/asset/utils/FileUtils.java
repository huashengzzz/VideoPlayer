package com.vientiane.asset.utils;

import android.content.Context;
import android.os.Environment;

import java.io.File;

public class FileUtils {
   // private static final String SD_PATH = Environment.getExternalStorageDirectory().getPath();
    public static final String NAME = "videorecord";

    /**
     * 设置视频录制的目录路径 android/data/包名/cash/videorecord
     * @param context
     * @return
     */
    public static File getVideoDir(Context context) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return new File(context.getApplicationContext().getExternalCacheDir(),NAME);
        }
        return null;
    }

    /**
     * 获取视频存储的文件目录路径
     * @param context
     * @return
     */
    public static String getVideoDirPath(Context context){
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return context.getApplicationContext().getExternalCacheDir().getPath() + "/" + NAME;
        }
        return "";
    }

    public static void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
            } else {
                String[] filePaths = file.list();
                for (String path : filePaths) {
                    deleteFile(filePath + File.separator + path);
                }
                file.delete();
            }
        }
    }

}
