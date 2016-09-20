package com.landscape.dragrefresh;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.joanzapata.android.BaseAdapterHelper;
import com.joanzapata.android.QuickAdapter;
import com.landscape.dragrefreshview.DragLoadListener;
import com.landscape.dragrefreshview.DragRefreshLayout;
import com.landscape.dragrefreshview.DragRefreshListener;

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
        adapter = new QuickAdapter<String>(this, R.layout.item_test, mockData()) {
            @Override
            protected void convert(BaseAdapterHelper helper, String item) {
                helper.setText(R.id.tv_name, item);
            }
        };
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(MainActivity.this, "you click test" + (position + 1), Toast.LENGTH_SHORT).show();
            }
        });
        dragRefresh.setRefreshListener(new DragRefreshListener() {
            @Override
            public void onRefresh() {
//                listView.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        dragRefresh.setRefreshing(false);
//                    }
//                }, 3000);
            }

            @Override
            public void refreshCancel() {
                Log.i("testDrag", "refreshCancel");
            }
        });
        dragRefresh.setLoadListener(new DragLoadListener() {
            @Override
            public void onLoad() {
//                listView.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        dragRefresh.setLoading(false);
//                    }
//                }, 3000);
            }

            @Override
            public void loadCancel() {
                Log.i("testDrag", "loadCancel");
            }
        });
    }

    private List<String> mockData() {
        List<String> data = new ArrayList<>();
        for (int i = 0; i < 17; i++) {
            data.add("test" + i);
        }
        return data;
    }
}
