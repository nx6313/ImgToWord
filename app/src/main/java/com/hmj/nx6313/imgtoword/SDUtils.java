package com.hmj.nx6313.imgtoword;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by nx6313 on 2018/2/2.
 */

public class SDUtils {
    private String file; // 设置文件存放路径
    private String fileName; // 存放文件名称
    private Context context; // 获取到Context 上下文
    private int rawid; // 资源文件ID ，需要COPY 的文件
    private String DATABASE_PATH = "";
    private String DATABASE_NAME = "";

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
        this.DATABASE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + file;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
        this.DATABASE_NAME = fileName;
    }

    public int getRawid() {
        return rawid;
    }

    public void setRawid(int rawid) {
        this.rawid = rawid;
    }

    public SDUtils() {
    }

    /**
     * @param file     文件夹例如： aa/bb
     * @param fileName 文件名
     * @param context  上下文
     * @param rawid    资源ID
     */
    public SDUtils(String file, String fileName, Context context, int rawid) {
        super();
        this.file = file;
        this.fileName = fileName;
        this.context = context;
        this.rawid = rawid;
        this.DATABASE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + file;
        this.DATABASE_NAME = fileName;
    }

    /**
     * 将文件复制到SD卡
     *
     * @return
     * @throws IOException
     */
    public boolean getSQLiteDatabase() throws IOException {
        // 首先判断该目录下的文件夹是否存在
        File dir = new File(DATABASE_PATH);
        String filename1 = DATABASE_PATH + "/" + DATABASE_NAME;
        if (!dir.exists()) {
            // 文件夹不存在 ， 则创建文件夹
            dir.mkdirs();
        }

        // 判断目标文件是否存在
        File file1 = new File(dir, DATABASE_NAME);

        if (!file1.exists()) {
            Log.i("msg", "没有文件，开始创建");
            file1.createNewFile(); // 创建文件
        }

        Log.i("msg", "准备开始进行文件的复制");
        // 开始进行文件的复制
        InputStream input = context.getResources().openRawResource(rawid); // 获取资源文件raw
        // 标号
        try {
            FileOutputStream out = new FileOutputStream(file1); // 文件输出流、用于将文件写到SD卡中
            // -- 从内存出去
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = (input.read(buffer))) != -1) { // 读取文件，-- 进到内存
                out.write(buffer, 0, len); // 写入数据 ，-- 从内存出
            }

            input.close();
            out.close(); // 关闭流
            return true;
        } catch (Exception e) {
            Log.i("msg", "复制异常");
            return false;
        }
    }
}
