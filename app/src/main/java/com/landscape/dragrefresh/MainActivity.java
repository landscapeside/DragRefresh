package com.landscape.dragrefresh;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import com.joanzapata.android.BaseAdapterHelper;
import com.joanzapata.android.QuickAdapter;
import com.landscape.dragrefreshview.DragRefreshLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    QuickAdapter adapter;

    @Bind(R.id.list_view)
    ListView listView;
    @Bind(R.id.drag_refresh)
    DragRefreshLayout dragRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        adapter = new QuickAdapter<String>(this,R.layout.item_test,mockData()) {
            @Override
            protected void convert(BaseAdapterHelper helper, String item) {
                helper.setText(R.id.tv_name, item);
            }
        };
        listView.setAdapter(adapter);

    }

    private List<String> mockData() {
        List<String> data = new ArrayList<>();
        for (int i = 0; i < 17; i++) {
            data.add("test" + i);
        }
        return data;
    }
}
