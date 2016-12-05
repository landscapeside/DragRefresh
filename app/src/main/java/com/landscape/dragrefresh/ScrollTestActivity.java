package com.landscape.dragrefresh;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.landscape.dragrefreshview.DragLoadListener;
import com.landscape.dragrefreshview.DragRefreshLayout;
import com.landscape.dragrefreshview.DragRefreshListener;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ScrollTestActivity extends AppCompatActivity {

    @Bind(R.id.drag_refresh)
    DragRefreshLayout dragRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scroll_test);
        ButterKnife.bind(this);

        dragRefresh.setRefreshListener(new DragRefreshListener() {
            @Override
            public void onRefresh() {
            }

            @Override
            public void refreshCancel() {
                Log.i("testDrag", "refreshCancel");
            }
        });
        dragRefresh.setLoadListener(new DragLoadListener() {
            @Override
            public void onLoad() {
            }

            @Override
            public void loadCancel() {
                Log.i("testDrag", "loadCancel");
            }
        });
    }
}
