package com.andybotting.tramhunter.activity;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.dao.TramHunterDB;

public class TramHunter extends ListActivity {

	ListView listView;

	
	String[] menuItems = {
			 "Favourite Stops",
			 "Browse for a Stop",
			 "Enter a TramTracker ID",
			 "Nearby Stops" 
	};

	String[] menuDesc = {
			 "Find your favourite stops fast",
			 "Pick your stop from the list",
			 "Use the TramTracker ID from the tram stop sign post",
			 "Use location services to find the nearest stops"
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
					case 2: {
						Intent intent = new Intent(TramHunter.this, EnterTTIDActivity.class);
						startActivityForResult(intent, 1);
						break;
					}
					case 3: {
						Intent intent = new Intent(TramHunter.this, NearStopsActivity.class);
						startActivityForResult(intent, 1);
						break;
					}
				}
  
			}
								
		});

		
		setListAdapter(new MenuListAdapter());
	}
  
	
	private class MenuListAdapter extends BaseAdapter {

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

			LayoutInflater inflater = getLayoutInflater();
			pv = inflater.inflate(R.layout.home_row, parent, false);
					
			wrapper = new ViewWrapper(pv);
			pv.setTag(wrapper);
				
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