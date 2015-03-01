package com.ry.inject.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.text.TextUtils;
import java.io.*;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created with IntelliJ IDEA
 *
 * @description:操作文件帮助类
 * @author: yangbing3@ucweb.com
 * @date: 2014/10/22 11:44
 */

public class FileUtils {

    public static void copyAssetsFile(Context context, String assetsFile, String destFile) {
        AssetManager assetManager = context.getAssets();
            InputStream in = null;
            OutputStream out = null;
            try {
                in = assetManager.open(assetsFile);
                File outFile = new File(destFile);
                out = new FileOutputStream(outFile);
                copyFile(in, out);
                in.close();
                out.flush();
                out.close();
            } catch(IOException e) {

            }
    }

    public static void copyAssetsFiles(Context context, String assetsDir, String destDir) {
        AssetManager assetManager = context.getAssets();
        String[] files = null;
        try {
            files = assetManager.list(assetsDir);
        } catch (IOException e) {
            return;
        }
        File dest = new File(destDir);
        if (!dest.exists()) {
            dest.mkdir();
        }

        for(String filename : files) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = assetManager.open(assetsDir + "/" + filename);
                File outFile = new File(destDir + "/" + filename);
                out = new FileOutputStream(outFile);
                copyFile(in, out);
                in.close();
                out.flush();
                out.close();
            } catch(IOException e) {
            }
        }
    }

    public static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    public static boolean fileExists(String filePath){
        return new File(filePath).exists();
    }

    public static void createDir(String dirPath,boolean needChmod){

        try {

            File dir = new File(dirPath);
            dir.mkdir();
//            RootShell.execCommand("chmod 777 -R " + dirPath,true);
            if(needChmod) {
                ShellUtils.execCommand("chmod 777 -R " + dirPath, true);
            }
        }catch (Exception e){
//            L.w(e);
        }
    }

    /**
     * 读取lua文件头注释
     * @param filePath
     * */
    public static String readHeadNoteTextFromLuaFile(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }

        File file = new File(filePath);
        BufferedReader reader = null;
        StringBuffer sb = new StringBuffer();
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = reader.readLine()) != null) {
                line=line.trim();
                if("".equals(line)||"--".equals(line)){
                    continue;
                }
                if(line.startsWith("--")){
                    sb.append(line.substring(2)).append("\n");
                }else {
                    break;
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
        return sb.toString();
    }

    public static HashMap<String, String> searchFile(File[] files, String suffix) {
        HashMap<String, String> map = new HashMap<String, String>(0);
        if (files == null) {
            return map;
        }
        for (File file : files) {
            if (file.isDirectory()) { //若为目录则递归查找
//                L.d("File Path %s isDirectory", file.getPath());
                HashMap temp = searchFile(file.listFiles(), suffix);
                map.putAll(temp);
            } else if (file.isFile()) {
                String path = file.getPath();
                if (path.endsWith(suffix)) {//查找指定扩展名的文件
                    //do someth
//                    L.d("File Path %s isFile, put to map", file.getPath());
                    map.put(file.getName(), path);
                }
            }
        }
        return map;
    }

    /**
     * 解压文件
     * @param destDir   解压目录
     * @param fis  解压的文件流
     * **/
    public static boolean unzipFile(InputStream fis, File destDir, boolean closeStream) {
        final byte[] buffer = new byte[4096];
        ZipInputStream zis = null;

        try {
            // make sure the directory is existent
            destDir.mkdirs();
            zis = new ZipInputStream(fis);
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                String fileName = entry.getName();

                if (entry.isDirectory()) {
                    new File(destDir, fileName).mkdirs();

                } else {
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(destDir, fileName)));
                    int lenRead;

                    while ((lenRead = zis.read(buffer)) != -1) {
                        bos.write(buffer, 0, lenRead);
                    }

                    bos.close();
                }
                if (closeStream) {
                    zis.closeEntry();
                }
            }

            return true;

        } catch (IOException e) {
//            L.w(e);
        } finally {
            if (closeStream) {
//                Utils.closeCloseable(zis);
            }
        }

        return false;
    }
}

