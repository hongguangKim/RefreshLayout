package com.example.refreshlayout;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.widget.RefreshFooter;
import com.example.widget.RefreshHeaderImpl;
import com.example.widget.RefreshLayout;

public class MainActivity extends Activity {

	private ListView listView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		listView = (ListView) findViewById(R.id.listView);

		final RefreshLayout refreshLayout = (RefreshLayout) findViewById(R.id.refreshLayout);
		if (refreshLayout != null) {
			// 刷新状��的回调
			refreshLayout
					.setRefreshListener(new RefreshLayout.OnRefreshListener() {
						@Override
						public void onRefresh(final int type) {
							// 延迟3秒后刷新成功
							refreshLayout.postDelayed(new Runnable() {
								@Override
								public void run() {
									if (type == RefreshLayout.TYPE_HEADER) {
										refreshLayout.refreshComplete();
										if (listView != null) {
											listView.setAdapter(new MainAdapter(50));
										}
									}else if (type == RefreshLayout.TYPE_FOOTER){
										refreshLayout.refreshAddMoreComplete();
										if (listView != null) {
											int currentTopPosition=listView.getFirstVisiblePosition();
											listView.setAdapter(new MainAdapter(60));
											listView.setSelectionFromTop(currentTopPosition+1, 0);
										}
										//refreshLayout.addContentView(new RefreshFooter(getApplicationContext()));
									}
								}
							}, 3000);
						}
					});
		}
		refreshLayout.autoRefresh();

	}

	class MainAdapter extends BaseAdapter {

		int count =50;
		public  MainAdapter(int count ){
			this.count = count ;
		}
		@Override
		public int getCount() {
			return count;
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				TextView textView = new TextView(MainActivity.this);
				textView.setText(String.valueOf(position));
				textView.setTextColor(Color.BLACK);
				textView.setBackgroundColor(0x55ff0000);
				textView.setLayoutParams(new AbsListView.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT, 200));
				textView.setGravity(Gravity.CENTER);
				convertView = textView;
			} else {
				((TextView) convertView).setText(String.valueOf(position));
			}

			return convertView;
		}
	}
}
