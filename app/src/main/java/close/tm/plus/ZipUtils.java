package close.tm.plus;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.io.FileNotFoundException;

public class ZipUtils {

    private static final String TAG = "ZipUtils";
    private static final int BUFFER_SIZE = 1024 * 1024; // 1MB缓冲区

    /**
     * 压缩文件或文件夹
     */
    public static boolean zipFile(Context context, File sourceFile, String zipFileName) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File tempDir = getTempDir(context, "zip_" + timestamp);

        try {
            // 1. 创建临时目录
            if (!tempDir.exists() && !tempDir.mkdirs()) {
                Log.e(TAG, "无法创建临时目录: " + tempDir.getAbsolutePath());
                return false;
            }

            // 2. 创建临时压缩文件
            File tempZipFile = new File(tempDir, zipFileName);

            // 3. 执行压缩
            boolean compressSuccess = compressToZip(sourceFile, tempZipFile);
            if (!compressSuccess) {
                cleanTempDir(tempDir);
                return false;
            }

            // 4. 移动压缩文件到目标位置
            File targetZipFile = new File(sourceFile.getParent(), zipFileName);
            if (targetZipFile.exists()) {
                // 如果文件已存在，添加数字后缀
                targetZipFile = getUniqueFile(targetZipFile);
            }

            boolean moveSuccess = tempZipFile.renameTo(targetZipFile);
            if (!moveSuccess) {
                // 如果重命名失败，尝试复制
                moveSuccess = copyFile(tempZipFile, targetZipFile);
            }

            // 5. 清理临时文件
            cleanTempDir(tempDir);

            return moveSuccess;

        } catch (Exception e) {
            Log.e(TAG, "压缩文件失败", e);
            cleanTempDir(tempDir);
            return false;
        }
    }

    /**
     * 解压文件
     */
    public static boolean unzipFile(Context context, File zipFile) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File tempDir = getTempDir(context, "unzip_" + timestamp);

        try {
            // 1. 创建临时目录
            if (!tempDir.exists() && !tempDir.mkdirs()) {
                Log.e(TAG, "无法创建临时目录: " + tempDir.getAbsolutePath());
                return false;
            }

            // 2. 解压到临时目录
            boolean unzipSuccess = extractZip(zipFile, tempDir);
            if (!unzipSuccess) {
                cleanTempDir(tempDir);
                return false;
            }

            // 3. 确定目标文件夹名称
            String baseName = getBaseName(zipFile.getName());
            File targetDir = new File(zipFile.getParent(), baseName);

            // 4. 如果目标文件夹已存在，添加数字后缀
            if (targetDir.exists()) {
                targetDir = getUniqueDirectory(targetDir);
            }

            // 5. 创建目标文件夹
            if (!targetDir.mkdirs()) {
                Log.e(TAG, "无法创建目标目录: " + targetDir.getAbsolutePath());
                cleanTempDir(tempDir);
                return false;
            }

            // 6. 移动解压的文件到目标位置
            boolean moveSuccess = moveFiles(tempDir, targetDir);

            // 7. 清理临时文件
            cleanTempDir(tempDir);

            return moveSuccess;

        } catch (Exception e) {
            Log.e(TAG, "解压文件失败", e);
            cleanTempDir(tempDir);
            return false;
        }
    }

    /**
     * 预览压缩包（解压到缓存但不移动）
     */
    public static File previewZip(Context context, File zipFile) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File tempDir = getTempDir(context, "preview_" + timestamp);

        try {
            // 1. 创建临时目录
            if (!tempDir.exists() && !tempDir.mkdirs()) {
                Log.e(TAG, "无法创建预览目录: " + tempDir.getAbsolutePath());
                return null;
            }

            // 2. 解压到临时目录
            boolean unzipSuccess = extractZip(zipFile, tempDir);
            if (!unzipSuccess) {
                cleanTempDir(tempDir);
                return null;
            }

            return tempDir;

        } catch (Exception e) {
            Log.e(TAG, "预览压缩包失败", e);
            cleanTempDir(tempDir);
            return null;
        }
    }

    /**
     * 清理指定临时目录
     */
    public static void cleanPreviewCache(File previewDir) {
        if (previewDir != null && previewDir.exists()) {
            cleanTempDir(previewDir);
        }
    }

    /**
     * 清理所有临时文件
     */
    public static void cleanAllTempFiles(Context context) {
        File baseTempDir = new File(context.getExternalCacheDir(), "temp");
        if (baseTempDir.exists()) {
            deleteRecursive(baseTempDir);
        }
    }

    // ============ 私有方法 ============

    private static File getTempDir(Context context, String subDir) {
        File cacheDir = context.getExternalCacheDir();
        if (cacheDir == null) {
            cacheDir = context.getCacheDir();
        }
        return new File(cacheDir, "temp/" + subDir);
    }

    private static boolean compressToZip(File source, File zipFile) {
        try {
            try (FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos))) {

                if (source.isDirectory()) {
                    zipDirectory(source, source, zos);
                } else {
                    zipFile(source, source.getParentFile(), zos);
                }

                return true;

            }
        } catch (IOException e) {} 
        return false;
    }

    private static void zipDirectory(File rootDir, File sourceDir, ZipOutputStream zos) throws IOException {
        File[] files = sourceDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                zipDirectory(rootDir, file, zos);
            } else {
                zipFile(file, rootDir, zos);
            }
        }
    }

    private static void zipFile(File file, File rootDir, ZipOutputStream zos) throws IOException {
        String entryName = getRelativePath(file, rootDir);
        ZipEntry zipEntry = new ZipEntry(entryName);
        zos.putNextEntry(zipEntry);

        try (FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis, BUFFER_SIZE)) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            while ((length = bis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
        }
        zos.closeEntry();
    }

    private static boolean extractZip(File zipFile, File destDir) {
        try {
            try (FileInputStream fis = new FileInputStream(zipFile);
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis))) {

                ZipEntry entry;
                byte[] buffer = new byte[BUFFER_SIZE];

                try {
                    while ((entry = zis.getNextEntry()) != null) {
                        File entryFile = new File(destDir, entry.getName());

                        if (entry.isDirectory()) {
                            if (!entryFile.exists() && !entryFile.mkdirs()) {
                                Log.w(TAG, "无法创建目录: " + entryFile.getAbsolutePath());
                            }
                        } else {
                            // 确保父目录存在
                            File parentDir = entryFile.getParentFile();
                            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                                Log.w(TAG, "无法创建父目录: " + parentDir.getAbsolutePath());
                                continue;
                            }

                            try {
                                try (FileOutputStream fos = new FileOutputStream(entryFile);
                                BufferedOutputStream bos = new BufferedOutputStream(fos, BUFFER_SIZE)) {

                                    int length;
                                    while ((length = zis.read(buffer)) > 0) {
                                        bos.write(buffer, 0, length);
                                    }
                                }
                            } catch (IOException e) {}
                        }
                        zis.closeEntry();
                    }
                } catch (IOException e) {}

                return true;

            }
        } catch (FileNotFoundException e) {} catch (IOException e) {
            Log.e(TAG, "解压过程出错", e);
            return false;
        }
        return false;
    }

    private static boolean moveFiles(File sourceDir, File targetDir) {
        File[] files = sourceDir.listFiles();
        if (files == null) return true;

        boolean allSuccess = true;
        for (File file : files) {
            File targetFile = new File(targetDir, file.getName());
            if (file.isDirectory()) {
                if (!targetFile.mkdirs()) {
                    Log.w(TAG, "无法创建目标目录: " + targetFile.getAbsolutePath());
                    allSuccess = false;
                    continue;
                }
                allSuccess = moveFiles(file, targetFile) && allSuccess;
                file.delete();
            } else {
                boolean success = file.renameTo(targetFile);
                if (!success) {
                    // 如果重命名失败，尝试复制
                    success = copyFile(file, targetFile);
                    if (success) {
                        file.delete();
                    }
                }
                allSuccess = allSuccess && success;
            }
        }
        return allSuccess;
    }

    private static boolean copyFile(File source, File target) {
        try {
            try (FileInputStream fis = new FileInputStream(source);
            FileOutputStream fos = new FileOutputStream(target)) {

                byte[] buffer = new byte[BUFFER_SIZE];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
                return true;

            }
        } catch (IOException e) {} 
        return false;
    }

    private static File getUniqueFile(File originalFile) {
        String baseName = getBaseName(originalFile.getName());
        String extension = getExtension(originalFile.getName());
        File parent = originalFile.getParentFile();

        for (int i = 1; i < 1000; i++) {
            File newFile = new File(parent, baseName + "_" + i + extension);
            if (!newFile.exists()) {
                return newFile;
            }
        }
        return originalFile; // 作为后备
    }

    private static File getUniqueDirectory(File originalDir) {
        String baseName = originalDir.getName();
        File parent = originalDir.getParentFile();

        for (int i = 1; i < 1000; i++) {
            File newDir = new File(parent, baseName + "_" + i);
            if (!newDir.exists()) {
                return newDir;
            }
        }
        return originalDir; // 作为后备
    }

    private static String getRelativePath(File file, File rootDir) {
        String filePath = file.getAbsolutePath();
        String rootPath = rootDir.getAbsolutePath();

        if (filePath.startsWith(rootPath)) {
            return filePath.substring(rootPath.length() + 1);
        }
        return file.getName();
    }

    private static String getBaseName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(0, dotIndex);
        }
        return fileName;
    }

    private static String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(dotIndex);
        }
        return "";
    }

    private static void cleanTempDir(File dir) {
        if (dir != null && dir.exists()) {
            deleteRecursive(dir);
        }
    }

    private static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] files = fileOrDirectory.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }
}
