package com.andybotting.tramhunter;

import android.app.AlertDialog;
import android.app.ListActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class TramHunter extends ListActivity {

	ListView listView;

	
	String[] menuItems = {
			 "Favourite Stops",
			 "Browse for a Stop",
			 "Enter a TramTracker ID" 
	};

	String[] menuDesc = {
			 "Find your favourite stops fast",
			 "Pick your stop from the list",
			 "Use the TramTracker ID from the tram stop sign post" 
	};


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		boolean firstLaunch = checkFirstLaunch();
		if (firstLaunch == false) {
			showAbout();
		} else {
			displayMenu();
		}
	}
	
	
	public boolean checkFirstLaunch() {
		TramHunterDB db = new TramHunterDB(this);
		boolean firstLaunch = db.checkFirstLaunch();
		return firstLaunch;

	}

	
	public void showAbout() {
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(TramHunter.this);
		
		dialogBuilder.setTitle("About");
		dialogBuilder.setMessage(R.string.change_log);
		
		dialogBuilder.setPositiveButton("OK",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						TramHunterDB db = new TramHunterDB(TramHunter.this);
						db.setFirstLaunch();
						displayMenu();
						db.close();
					}
				});

		dialogBuilder.setCancelable(false);
		dialogBuilder.create().show();		
	}
	
	public void displayMenu() {
		
		//ArrayList menuItems = new ArrayList();   
		setContentView(R.layout.home);
			
		listView = (ListView)this.findViewById(android.R.id.list);
		setTitle(getResources().getText(R.string.app_name));

		listView.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View row, int position, long id) {
				
				switch ( (int)id ) {
					case 0: {
						Intent intent = new Intent(TramHunter.this, StopsListActivity.class);
						startActivityForResult(intent, 1);
						break;
					}
					case 1: {
						Intent intent = new Intent(TramHunter.this, RoutesListActivity.class);
						startActivityForResult(intent, 1);
						break;
					}
					case 2:
						Intent intent = new Intent(TramHunter.this, EnterTTIDActivity.class);
						startActivityForResult(intent, 1);
						break;

					}
  
			}
								
		});

		ScrollView sv = new ScrollView(this);
		sv.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		LinearLayout layout = new LinearLayout(this);
		layout.setPadding(10, 10, 10, 0);
		layout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));		
		
		layout.setOrientation(LinearLayout.VERTICAL);	
		
		setListAdapter(new MenuListAdapter(this));
	}
  
	
	private class MenuListAdapter extends BaseAdapter {
		
		private Context mContext;		
		private int usenameHeight;

		public MenuListAdapter(Context context) {
			mContext = context;
		}

		public int getCount() {
			return menuItems.length;
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
				
			View pv = convertView;
			ViewWrapper wrapper = null;
				
			if (pv == null) {
				LayoutInflater inflater = getLayoutInflater();
				pv = inflater.inflate(R.layout.home_row, parent, false);
					
				wrapper = new ViewWrapper(pv);
				if (position == 0) {
					usenameHeight = wrapper.getTextLabel1().getHeight();
				}
					
				pv.setTag(wrapper);
					
				wrapper = new ViewWrapper(pv);
				pv.setTag(wrapper);
			} 
			else {
				wrapper = (ViewWrapper) pv.getTag();
			}
				
			pv.setId(0);
				
			wrapper.getTextLabel1().setText(menuItems[position]);
			wrapper.getTextLabel2().setText(menuDesc[position]);

			return pv;
		}

	}

	
	class ViewWrapper {
		View base;
				
		TextView textLabel1 = null;
		TextView textLabel2 = null;
		

		ViewWrapper(View base) {
			this.base = base;
		}

		TextView getTextLabel1() {
			if (textLabel1 == null) {
				textLabel1 = (TextView) base.findViewById(R.id.textLabel1);
			}
			return (textLabel1);
		}

		TextView getTextLabel2() {
			if (textLabel2 == null) {
				textLabel2 = (TextView) base.findViewById(R.id.textLabel2);
			}
			return (textLabel2);
		}

	}	 
	
	
	// Add settings to menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, 0, 0, "About");
		MenuItem menuItem1 = menu.findItem(0);
		menuItem1.setIcon(R.drawable.ic_menu_info_details);

		return true;
	}

	
	// Menu actions
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			showAbout();
			return true;
		}
		return false;

	}
	  
}