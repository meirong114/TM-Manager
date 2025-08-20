package close.tm.plus;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class ImageViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        ImageView imageView = findViewById(R.id.image_view);

        String filePath = getIntent().getStringExtra("file_path");
        if (filePath != null) {
            File imageFile = new File(filePath);
            if (imageFile.exists()) {
                imageView.setImageURI(android.net.Uri.fromFile(imageFile));
            } else {
                Toast.makeText(this, "图片文件不存在", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
