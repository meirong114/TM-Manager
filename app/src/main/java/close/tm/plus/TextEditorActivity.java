package close.tm.plus;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import android.content.DialogInterface;

public class TextEditorActivity extends AppCompatActivity {

    private EditText editText;
    private File currentFile;
    private boolean isModified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_editor);

        editText = findViewById(R.id.edit_text);

        // 获取文件路径
        String filePath = getIntent().getStringExtra("file_path");
        if (filePath != null) {
            currentFile = new File(filePath);
            loadFileContent();
        }

        // 监听文本变化
        editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    isModified = true;
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
    }

    private void loadFileContent() {
        if (currentFile.exists() && currentFile.canRead()) {
            try {
                StringBuilder content = new StringBuilder();
                BufferedReader reader = new BufferedReader(new FileReader(currentFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                reader.close();
                editText.setText(content.toString());
                setTitle(currentFile.getName());
            } catch (IOException e) {
                Toast.makeText(this, "读取文件失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void saveFile() {
        if (currentFile != null) {
            try {
                FileWriter writer = new FileWriter(currentFile);
                writer.write(editText.getText().toString());
                writer.close();
                isModified = false;
                Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_text_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_save) {
            saveFile();
            return true;
        } else if (id == R.id.menu_exit) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (isModified) {
            // 提示保存
            new android.app.AlertDialog.Builder(this)
                .setTitle("保存更改")
                .setMessage("文件已修改，是否保存？")
                .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveFile();
                        finish();
                    }
                })
                .setNegativeButton("不保存", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNeutralButton("取消", null)
                .show();
        } else {
            super.onBackPressed();
        }
    }
}
