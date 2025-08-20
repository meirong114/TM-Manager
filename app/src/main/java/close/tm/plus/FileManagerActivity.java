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
import android.content.pm.ResolveInfo;
import android.content.ComponentName;

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
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
grantRoot();
        // 2. 检查是否初始化成功
        if (drawerLayout == null) {
            throw new IllegalStateException("DrawerLayout 未找到！请检查布局文件 ID");
        }
        if (navigationView == null) {
            throw new IllegalStateException("NavigationView 未找到！请检查布局文件 ID");
            
            
        }
        
        // 3. 设置菜单按钮点击事件
        menuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (drawerLayout != null) {
                        drawerLayout.openDrawer(GravityCompat.START); // 打开侧边栏
                    }
                }
            });

        // 4. 设置侧边栏菜单项点击监听
        navigationView.setNavigationItemSelectedListener(
            new NavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    int id = item.getItemId();
                    if (id == R.id.nav_thanks) {
                        openThanksActivity();
                    } else if (id == R.id.nav_group) {
                        joinQQGroup();
                    }
                    // 关闭侧边栏（确保 drawerLayout 非空）
                    if (drawerLayout != null) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    }
                    return true;
                }
            }
        );
    
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
    // 在 FileManagerActivity 中添加这些方法
// 判断是否为图片文件
    private boolean isImageFile(File file) {
        String fileName = file.getName().toLowerCase();
        String extension = getFileExtension(fileName);

        String[] imageExtensions = {
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "ico", "tiff"
        };

        for (String ext : imageExtensions) {
            if (ext.equals(extension)) {
                return true;
            }
        }
        return false;
    }

// 打开文本文件
    private void openTextFile(File file) {
        Intent intent = new Intent(this, TextEditorActivity.class);
        intent.putExtra("file_path", file.getAbsolutePath());
        startActivity(intent);
    }

// 打开图片文件
    private void openImageFile(File file) {
        Intent intent = new Intent(this, ImageViewerActivity.class);
        intent.putExtra("file_path", file.getAbsolutePath());
        startActivity(intent);
    }

// 显示完整的打开方式选择对话框
    private void showCompleteOpenWithDialog(final File file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择打开方式: " + file.getName());

        // 获取所有可以处理该文件的应用
        Intent baseIntent = new Intent(Intent.ACTION_VIEW);
        String mimeType = getMimeType(file);
        baseIntent.setDataAndType(Uri.fromFile(file), mimeType);

        final List<ResolveInfo> resolveInfos = getPackageManager()
            .queryIntentActivities(baseIntent, PackageManager.MATCH_ALL);

        // 创建选项列表
        List<CharSequence> appNames = new ArrayList<>();
        final List<Intent> intents = new ArrayList<>();

        // 添加内置选项
        if (isTextFile(file)) {
            appNames.add("内置文本编辑器");
            intents.add(new Intent(this, TextEditorActivity.class)
                        .putExtra("file_path", file.getAbsolutePath()));
        }

        if (isImageFile(file)) {
            appNames.add("内置图片查看器");
            intents.add(new Intent(this, ImageViewerActivity.class)
                        .putExtra("file_path", file.getAbsolutePath()));
        }

        // 添加系统应用选项
        for (ResolveInfo info : resolveInfos) {
            String appName = info.loadLabel(getPackageManager()).toString();
            appNames.add(appName + " (系统应用)");

            Intent appIntent = new Intent(Intent.ACTION_VIEW);
            appIntent.setDataAndType(Uri.fromFile(file), mimeType);
            appIntent.setPackage(info.activityInfo.packageName);
            appIntent.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
            intents.add(appIntent);
        }

        if (appNames.isEmpty()) {
            builder.setMessage("没有找到可以打开此文件的应用");
            builder.setPositiveButton("确定", null);
            grantRoot();
        } else {
            builder.setItems(appNames.toArray(new CharSequence[0]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Intent intent = intents.get(which);
                            if (intent.getComponent() != null) {
                                // 系统应用
                                grantRoot();
                                startActivity(intent);
                            } else {
                                // 内置应用
                                startActivity(intent);
                            }
                        } catch (Exception e) {
                            Toast.makeText(FileManagerActivity.this, 
                                           "无法打开文件: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        }

        builder.setNegativeButton("取消", null);
        builder.show();
    }
    private void grantRoot() {
        
        try {
            Runtime.getRuntime().exec("su");
        } catch (IOException e) {}

    }

// 修改 openFile 方法
    private void openFile(File file) {
        String mimeType = getMimeType(file);
grantRoot();
        if (mimeType != null) {
            if (mimeType.startsWith("video/") || mimeType.startsWith("audio/") || 
                mimeType.equals("text/html") || mimeType.equals("application/pdf")) {

                // 使用WebView打开视频、音频、HTML、PDF
                recyclerView.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);

                webView.setWebViewClient(new WebViewClient());
                webView.getSettings().setJavaScriptEnabled(true);
                webView.getSettings().setAllowFileAccess(true);
                webView.getSettings().setAllowContentAccess(true);

                if (mimeType.equals("application/pdf")) {
                    webView.loadUrl("https://docs.google.com/gview?embedded=true&url=" + Uri.fromFile(file));
                } else {
                    webView.loadUrl(Uri.fromFile(file).toString());
                }
            } else if (isTextFile(file)) {
                // 文本文件显示打开方式选择（包含内置编辑器）
                showCompleteOpenWithDialog(file);
            } else if (isImageFile(file)) {
                // 图片文件显示打开方式选择（包含内置查看器）
                showCompleteOpenWithDialog(file);
            } else {
                // 其他文件类型显示完整的打开方式选择
                showCompleteOpenWithDialog(file);
            }
        } else {
            // 未知文件类型显示完整的打开方式选择
            showCompleteOpenWithDialog(file);
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
    // 在 FileManagerActivity 类中添加这些方法
    private void openWithTextEditor(File file) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), "text/plain");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // 检查是否有应用可以处理文本文件
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "未找到文本编辑器应用", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "无法打开文件: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

// 显示打开方式选择对话框
    private void showOpenWithDialog(final File file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择打开方式")
            .setMessage("请选择如何打开文件: " + file.getName());

        // 获取所有可以处理该文件的应用
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String mimeType = getMimeType(file);
        intent.setDataAndType(Uri.fromFile(file), mimeType);

        final List<ResolveInfo> resolveInfos = getPackageManager().queryIntentActivities(intent, 0);

        if (resolveInfos.isEmpty()) {
            // 没有应用可以打开，提供文本编辑器选项
            builder.setMessage("没有找到可以打开此文件的应用\n是否尝试用文本编辑器打开？")
                .setPositiveButton("文本编辑器", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openWithTextEditor(file);
                    }
                })
                .setNegativeButton("取消", null);
        } else {
            // 创建应用选择列表
            final CharSequence[] appNames = new CharSequence[resolveInfos.size() + 1];
            final List<Intent> intents = new ArrayList<>();

            // 添加文本编辑器选项
            appNames[0] = "文本编辑器";
            intents.add(createTextEditorIntent(file));

            // 添加其他应用选项
            for (int i = 0; i < resolveInfos.size(); i++) {
                ResolveInfo info = resolveInfos.get(i);
                appNames[i + 1] = info.loadLabel(getPackageManager());

                Intent appIntent = new Intent(Intent.ACTION_VIEW);
                appIntent.setDataAndType(Uri.fromFile(file), mimeType);
                appIntent.setPackage(info.activityInfo.packageName);
                intents.add(appIntent);
            }

            builder.setItems(appNames, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            startActivity(intents.get(which));
                        } catch (Exception e) {
                            Toast.makeText(FileManagerActivity.this, 
                                           "无法打开文件: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        }

        builder.setNegativeButton("取消", null);
        builder.show();
    }

// 创建文本编辑器Intent
    private Intent createTextEditorIntent(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), "text/plain");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return intent;
    }
// 判断是否为文本文件
    private boolean isTextFile(File file) {
        String fileName = file.getName().toLowerCase();
        String extension = getFileExtension(fileName);

        // 文本文件扩展名列表
        String[] textExtensions = {
            "txt", "log", "class", "smali", "java", "py", "pyw", "cfg", "cnf", "htm",
            "cmd", "bat", "c", "cpp", "h", "hpp", "xml", "html", "php", "yaml", "shtml",
            "css", "js", "json", "md", "ini", "cfg", "conf", "properties", "sh", "nsi"
        };

        for (String ext : textExtensions) {
            if (ext.equals(extension)) {
                return true;
            }
        }
        return false;
    }

// 获取文件扩展名
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }

// 获取文件MIME类型
    private String getMimeType(File file) {
        String fileName = file.getName();
        String extension = getFileExtension(fileName.toLowerCase());

        // 自定义MIME类型映射
        switch (extension) {
            case "txt": return "text/plain";
            case "log": return "text/plain";
            case "java": return "text/x-java-source";
            case "py": return "text/x-python";
            case "pyw": return "text/x-python";
            case "c": return "text/x-c";
            case "cpp": return "text/x-c++";
            case "h": return "text/x-c-header";
            case "xml": return "text/xml";
            case "html": return "text/html";
            case "css": return "text/css";
            case "js": return "text/javascript";
            case "json": return "application/json";
            case "md": return "text/markdown";
            case "bat": return "application/bat";
            case "cmd": return "application/cmd";
            default: return "*/*"; // 未知类型
        }
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
        grantRoot();
        fileList.clear();
        File[] files = currentDirectory.listFiles();
        if (files != null) {
            Collections.addAll(fileList, files);
            sortFilesAZ();
            for (File file : files) {
                fileList.add(file);
                grantRoot();
            }
        }
        grantRoot();
        fileAdapter.notifyDataSetChanged();
        updateTitle();
        grantRoot();
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
            finishAffinity();
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

    /*
    private void openFile(File file) {
        if (file.length() > 10 * 1024 * 1024) { // 大于10MB的图片
            Toast.makeText(this, "图片过大，无法预览", Toast.LENGTH_SHORT).show();
            return;
        }
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
*/
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
