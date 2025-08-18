package close.tm.plus;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.navigation.NavigationView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;
import android.text.InputType;
import androidx.appcompat.widget.Toolbar;
import android.widget.TextView;
import android.view.View;
import android.os.StatFs;
import android.view.MenuItem;
import android.content.Context;
import android.content.ActivityNotFoundException;
import java.util.Collections;
import java.util.Comparator;
import android.widget.Button;

public class FileManagerActivity extends AppCompatActivity implements FileAdapter.FileClickListener,NavigationView.OnNavigationItemSelectedListener {
    private static final int REQUEST_PERMISSION_CODE = 1;
    private RecyclerView recyclerView;
    private FileAdapter fileAdapter;
    private List<File> fileList = new ArrayList<>();
    private File currentDirectory;
    private WebView webView;
    private Toolbar toolbar;
    private TextView titleText;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_manager);
        Button menuButton = findViewById(R.id.menu_button);

        // 设置点击事件
        menuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        recyclerView = findViewById(R.id.recyclerView);
        webView = findViewById(R.id.webView);
        NavigationView navigationView = findViewById(R.id.nav_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        fileAdapter = new FileAdapter(fileList, this);
        recyclerView.setAdapter(fileAdapter);
        toolbar = findViewById(R.id.toolbar);
        titleText = findViewById(R.id.title_text);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        navigationView.setNavigationItemSelectedListener(this);
        if (navigationView == null) {
            throw new IllegalStateException("NavigationView not found in layout");
        }
        // 更新存储信息
        updateStorageInfo();
        if (navigationView == null) {
            throw new IllegalStateException("NavigationView not found in layout");
        }
        // 检查权限
        if (checkPermission()) {
            initFileManager();
        } else {
            requestPermission();
        }
    }
    // 文件排序方法 (添加到FileManagerActivity类中)
    private void sortFilesAZ() {
        Collections.sort(fileList, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    return f1.getName().compareToIgnoreCase(f2.getName());
                }
            });
        fileAdapter.notifyDataSetChanged();
    }
    private void updateStorageInfo() {
        if (navigationView == null) return;

        // 检查是否有header view
        if (navigationView.getHeaderCount() == 0) return;

        View headerView = navigationView.getHeaderView(0);
        StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long totalBytes = statFs.getBlockCountLong() * statFs.getBlockSizeLong();
        long freeBytes = statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong();
        long usedBytes = totalBytes - freeBytes;

        String totalSize = formatSize(totalBytes);
        String usedSize = formatSize(usedBytes);
        int usedPercent = (int) ((usedBytes * 100) / totalBytes);

        TextView storageInfo = headerView.findViewById(R.id.storage_info);
        storageInfo.setText(String.format("内部存储\n已用 %s / %s (%d%%)", 
                                          usedSize, totalSize, usedPercent));
    }

    private String formatSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // 处理菜单项点击
        int id = item.getItemId();

        if (id == R.id.nav_thanks) {
            openThanksActivity();
        } else if (id == R.id.nav_group) {
            joinQQGroup();
        } else if (id == R.id.nav_settings) {
            openAppSettings();
        }

        // 关闭抽屉
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void openThanksActivity() {
        Intent intent = new Intent(this, ThanksActivity.class);
        startActivity(intent);
    }

    private void joinQQGroup() {
        try {
            // 替换为您的QQ群key
            openQQGroupDetail(this, "750492566");
        } catch (Exception e) {
            Toast.makeText(this, "未安装QQ或版本不支持", Toast.LENGTH_SHORT).show();
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent();
        intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
    }

    // 更新当前目录时同时更新标题
    private void refreshFileList() {
        fileList.clear();
        File[] files = currentDirectory.listFiles();
        if (files != null) {
            Collections.addAll(fileList, files);
            sortFilesAZ();
            for (File file : files) {
                fileList.add(file);
            }
        }
        fileAdapter.notifyDataSetChanged();
        updateTitle();
    }

    private void updateTitle() {
        String path = currentDirectory.getAbsolutePath();
        if (path.equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
            titleText.setText("内部存储");
        } else {
            titleText.setText(currentDirectory.getName());
        }
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                                          new String[]{
                                              Manifest.permission.READ_EXTERNAL_STORAGE,
                                              Manifest.permission.WRITE_EXTERNAL_STORAGE
                                          }, REQUEST_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initFileManager();
            } else {
                Toast.makeText(this, "需要存储权限才能使用文件管理器", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initFileManager() {
        currentDirectory = Environment.getExternalStorageDirectory();
        refreshFileList();
    }

    

    private void showCreateDialog(final boolean isFolder) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isFolder ? "创建文件夹" : "创建文件");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("创建", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        if (isFolder) {
                            createFolder(name);
                        } else {
                            createFile(name);
                        }
                    } else {
                        Toast.makeText(FileManagerActivity.this, 
                                       "名称不能为空", Toast.LENGTH_SHORT).show();
                    }
                }
            });

        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

        builder.show();
    }
    private void createFile(String fileName) {
        try {
            File newFile = new File(currentDirectory, fileName);
            if (newFile.createNewFile()) {
                Toast.makeText(this, "文件创建成功", Toast.LENGTH_SHORT).show();
                refreshFileList();
            } else {
                Toast.makeText(this, "文件已存在或创建失败", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "文件创建失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void createFolder(String folderName) {
        File newFolder = new File(currentDirectory, folderName);
        if (newFolder.mkdir()) {
            Toast.makeText(this, "文件夹创建成功", Toast.LENGTH_SHORT).show();
            refreshFileList();
        } else {
            Toast.makeText(this, "文件夹已存在或创建失败", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onFileClick(File file) {
        if (file.isDirectory()) {
            currentDirectory = file;
            refreshFileList();
        } else {
            openFile(file);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.getVisibility() == android.view.View.VISIBLE) {
            webView.setVisibility(android.view.View.GONE);
            recyclerView.setVisibility(android.view.View.VISIBLE);
        } else if (currentDirectory.getParent() != null) {
            currentDirectory = currentDirectory.getParentFile();
            refreshFileList();
        } else {
            super.onBackPressed();
        }
    }
    public static void openQQGroupDetail(Context context, String groupNumber) {
    try {
        // 构造跳转到QQ群详情页面的URL
        String url = "mqqapi://card/show_pslcard?src_type=internal&version=1&uin=" + groupNumber + "&card_type=group&source=qrcode";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        context.startActivity(intent);
    } catch (ActivityNotFoundException e) {
        // 如果设备未安装QQ应用，可以提示用户
        Toast.makeText(context, "未检测到QQ应用，请先安装", Toast.LENGTH_SHORT).show();
    }
}


    private void openFile(File file) {
        String mimeType = getMimeType(file.getPath());

        if (mimeType != null) {
            if (mimeType.startsWith("image/") || mimeType.startsWith("video/") || 
                mimeType.startsWith("audio/") || mimeType.equals("text/html") || 
                mimeType.equals("application/pdf")) {

                // 使用WebView打开媒体文件和HTML/PDF
                recyclerView.setVisibility(android.view.View.GONE);
                webView.setVisibility(android.view.View.VISIBLE);

                webView.setWebViewClient(new WebViewClient());
                webView.getSettings().setJavaScriptEnabled(true);
                webView.getSettings().setAllowFileAccess(true);
                webView.getSettings().setAllowContentAccess(true);

                if (mimeType.equals("application/pdf")) {
                    webView.loadUrl("https://docs.google.com/gview?embedded=true&url=" + Uri.fromFile(file));
                } else {
                    webView.loadUrl(Uri.fromFile(file).toString());
                }
            } else {
                // 其他文件类型使用系统默认应用打开
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(file), mimeType);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            }
        } else {
            Toast.makeText(this, "无法打开此文件类型", Toast.LENGTH_SHORT).show();
        }
    }

    private String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        }
        return type;
    }

    @Override
    public void onCreateFileClick() {
        // 实现创建文件逻辑
        // 这里可以弹出一个对话框让用户输入文件名
        // 然后调用 new File(currentDirectory, fileName).createNewFile();
        showCreateDialog(false);
        refreshFileList();
        // 最后 refreshFileList();
    }

    @Override
    public void onCreateFolderClick() {
        // 实现创建文件夹逻辑
        // 这里可以弹出一个对话框让用户输入文件夹名
        // 然后调用 new File(currentDirectory, folderName).mkdir();
        // 最后 refreshFileList();
        showCreateDialog(false);
        refreshFileList();
    }

    @Override
    public void onZipClick(File file) {
        // 实现压缩功能
        try {
            if (file.isDirectory()) {
                zipFolder(file.getPath(), file.getPath() + ".zip");
            } else {
                zipFile(file.getPath(), file.getPath() + ".zip");
            }
            Toast.makeText(this, "压缩成功", Toast.LENGTH_SHORT).show();
            refreshFileList();
        } catch (IOException e) {
            Toast.makeText(this, "压缩失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onUnzipClick(File file) {
        // 实现解压功能
        try {
            unzip(file.getPath(), currentDirectory.getPath());
            Toast.makeText(this, "解压成功", Toast.LENGTH_SHORT).show();
            refreshFileList();
        } catch (IOException e) {
            Toast.makeText(this, "解压失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void zipFile(String sourcePath, String zipPath) throws IOException {
        FileOutputStream fos = new FileOutputStream(zipPath);
        ZipOutputStream zos = new ZipOutputStream(fos);

        File file = new File(sourcePath);
        FileInputStream fis = new FileInputStream(file);

        ZipEntry zipEntry = new ZipEntry(file.getName());
        zos.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }

        zos.closeEntry();
        fis.close();
        zos.close();
        fos.close();
    }

    private void zipFolder(String sourcePath, String zipPath) throws IOException {
        FileOutputStream fos = new FileOutputStream(zipPath);
        ZipOutputStream zos = new ZipOutputStream(fos);

        File sourceFile = new File(sourcePath);
        addFolderToZip(sourceFile, sourceFile.getName(), zos);

        zos.close();
        fos.close();
    }

    private void addFolderToZip(File folder, String parentFolder, ZipOutputStream zos) throws IOException {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                addFolderToZip(file, parentFolder + "/" + file.getName(), zos);
                continue;
            }

            FileInputStream fis = new FileInputStream(file);
            ZipEntry zipEntry = new ZipEntry(parentFolder + "/" + file.getName());
            zos.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }

            zos.closeEntry();
            fis.close();
        }
    }

    private void unzip(String zipPath, String destPath) throws IOException {
        File destDir = new File(destPath);
        if (!destDir.exists()) {
            destDir.mkdir();
        }

        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath));
        ZipEntry zipEntry = zis.getNextEntry();

        while (zipEntry != null) {
            File newFile = new File(destDir, zipEntry.getName());
            if (zipEntry.isDirectory()) {
                newFile.mkdirs();
            } else {
                // 创建父目录
                new File(newFile.getParent()).mkdirs();

                FileOutputStream fos = new FileOutputStream(newFile);
                byte[] bytes = new byte[1024];
                int length;
                while ((length = zis.read(bytes)) > 0) {
                    fos.write(bytes, 0, length);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }

        zis.closeEntry();
        zis.close();
    }
}
