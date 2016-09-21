# DragRefresh

* 引入
```gradle
dependencies {
    compile 'com.github.landscapeside:DragRefresh:0.2'
}

repositories {
        jcenter()
        maven { url "https://jitpack.io" }
    }
```

* 使用

```xml
<com.landscape.dragrefreshview.DragRefreshLayout
        android:id="@+id/drag_refresh"
        android:layout_width="match_parent"
        android:layout_height="200dp"
        app:refresh_content="@+id/list_view"
        app:refresh_empty="@+id/empty_view">

        <TextView
            android:id="@+id/empty_view"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:text="empty!!"
            android:gravity="center"
            android:visibility="gone"/>

        <ListView
            android:id="@+id/list_view"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:visibility="visible"/>

    </com.landscape.dragrefreshview.DragRefreshLayout>
```

```java
/**
*  下拉刷新事件监听，如果要禁用下拉刷新，给listener传null即可
*/
dragRefresh.setRefreshListener(new DragRefreshListener() {
            @Override
            public void onRefresh() {
                listView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dragRefresh.setRefreshing(false);
                    }
                }, 3000);
            }

            @Override
            public void refreshCancel() {
                Log.i("testDrag", "refreshCancel");
            }
        });
        
/**
*  上拉加载事件监听，如果要禁用上拉加载，给listener传null即可
*/
dragRefresh.setLoadListener(new DragLoadListener() {
        @Override
        public void onLoad() {
            listView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    dragRefresh.setLoading(false);
                }
            }, 3000);
        }

        @Override
        public void loadCancel() {
            Log.i("testDrag", "loadCancel");
        }
    });     
        

```
