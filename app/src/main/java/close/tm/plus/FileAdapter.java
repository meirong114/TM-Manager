package close.tm.plus;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private List<File> fileList;
    private FileClickListener listener;

    public interface FileClickListener {
        void onFileClick(File file);
        void onCreateFileClick();
        void onCreateFolderClick();
        void onZipClick(File file);
        void onUnzipClick(File file);
    }

    public FileAdapter(List<File> fileList, FileClickListener listener) {
        this.fileList = fileList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        File file = fileList.get(position);
        holder.bind(file, listener);
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        private TextView fileName;
        private ImageView fileIcon;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.fileName);
            fileIcon = itemView.findViewById(R.id.fileIcon);
        }

        public void bind(final File file, final FileClickListener listener) {
            fileName.setText(file.getName());

            if (file.isDirectory()) {
                fileIcon.setImageResource(R.drawable.ic_folder);
            } else {
                fileIcon.setImageResource(R.drawable.ic_file);
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onFileClick(file);
                    }
                });

            // 这里可以添加长按菜单来处理创建、压缩/解压等操作
        }
    }
}
