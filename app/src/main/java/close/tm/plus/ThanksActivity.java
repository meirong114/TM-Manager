package close.tm.plus;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ThanksActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thanks);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<ThanksItem> thanksList = new ArrayList<>();
        thanksList.add(new ThanksItem("MT管理器", "安卓搞基不二之选"));
        thanksList.add(new ThanksItem("SB管理器", "学习了一些东西"));
        thanksList.add(new ThanksItem("beloved~", "SB管理器开发者，提供了部分思路"));

        ThanksAdapter adapter = new ThanksAdapter(thanksList);
        recyclerView.setAdapter(adapter);
    }
}
