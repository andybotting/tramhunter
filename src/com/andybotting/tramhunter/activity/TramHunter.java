package com.andybotting.tramhunter.activity;

import java.util.List;
import java.util.Random;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
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

import com.andybotting.tramhunter.PreferenceHelper;
import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.Stop;
import com.andybotting.tramhunter.dao.TramHunterDB;

public class TramHunter extends ListActivity {	
	private ListView m_listView;
	private PreferenceHelper preferenceHelper;
	private TramHunterDB db;
	
	private String[] m_menuItems = {"Favourite Stops",
								    "Browse for a Stop",
								    "Enter a TramTracker ID",
								    "Nearby Stops",
								    "Settings"};

	private String[] m_menuDesc = {"Get the details for your favourite stops, fast",
								   "Browse for your stop by route and stop lists",
								   "Get the details for your stop by TramTracker ID",
								   "Use your location to find stops nearest to you",
								   "Set your Tram Hunter preferences"};

	private static String[] m_welcomeMessages = {"Dude, wheres my tram?",
												 "My milkshakes bring all the trams to the stop",
												 "All your tram are belong to us",
												 "Oh Trameo, oh Trameo where art thou?",
												 "Frankly my dear, I dont give a tram",
												 "May the tram be with you",
												 "I love the smell of tram in the morning!",
												 "E.T. tram home",
												 "There's no tram like home",
												 "Say 'hello' to my little tram",
												 "Open the tram bay doors, HAL",
												 "My tram, my precious",
												 "We'll always have trams!",
												 "If you tram it, they will come",
												 "There is no tram",
												 "Here's looking at you, tram",
												 "Snakes on a tram!",
												 "I'm on a tram",
												 "I see your tram is as late as mine",
												 "Tram-a-lama-ding-dong",
												 "I see trams, they're everywhere...",
												 "I can haz my tramz time",
												 "Time stops for no tram",
												 "One small step for trams",
												 "One small step for man, one giant leap for tram times",
												 "You can't handle the tram",
												 "Show me the trolley",
												 "If a tram arrives in a forest and there's no one to board it, does it make a 'ding'?",
												 "Swanston Street is like a box of trams, you never know which one you're gonna get...",
												 "Houston, we have a tram",
												 "Tram on, tram off",
												 "Who let the trams out?",
												 "These are not the trams you are looking for",
												 "If you catch my tram, all of your wildest dreams will come true",
												 "An den? ding. annn den? ding. an den? ding ding",
												 "The first rule of tram club, dont talk about tram club",
												 "Stay away from her you tram!",
												 "Asta la vista Tramy",
												 "If it dings, we can catch it",
												 "He said 'Let there be light rail', and there were trams...",
												 "Catch me if you can...",
												 "A tram for all seasons",
												 "We're going to need a bigger tram",
												 "Do or do not, there is no tram",
												 "Crikey it's a tram, I'll catch it if I can",
												 "Are the trams still screaming",
												 "Silence of the trams",
												 "Wham bam thank you tram",
												 "It ate my tram ticket, doh!",
												 "I'm sorry inspector, I left my tram ticket at home",
												 "Shaken, not tramed",
												 "Tram, James Tram",
												 "You dingin' at me? Are you dingin' at me?",
												 "I know what you're thinking, has he missed 6 trams or only 5?",
												 "Toto I've got a feeling were not on a tram anymore",
												 "What we have here, is a failure to catch a tram",
												 "Play it again tram",
												 "Please tram, I'd like some more",
												 "I like dem french fried trams, mmmmHHhhhmmmm",
												 "You call that a tram? Thats not a tram. This is tram!",
												 "It's going straight to the tram room",
												 "Will you take the red tram or the blue tram",
												 "Is that a tram I see before me?",
												 "I know tram fu",
												 "Never send a man to do a trams job",
												 "Do you feel lucky, tram?",
												 "A good tram is hard to find",
												 "Can you do it on a tram, would you have green eggs and ham?",
												 "To the world you may be one tram, to one tram you may be the world",
												 "Give me liberty or give me trams!",
												 "I am the tram king, I can do anything...",
												 "Absence makes the tram grow fonder",
												 "Float like a butterfly, ding like a tram",
												 "A tram, a tram, my kingdom for a tram",
												 "Hey boy, you're looking mighty cute in that tram",
												 "I pity the tram",
												 "All right, Mr. DeMille, I'm ready for my tram",
												 "Luke, I am your tram",
												 "You'll never know the dark side of the tram",
												 "I ding, therefore I tram",
												 "I had a tram, where all men were created equal",
												 "I really shouldn't have written all of those tram programs",
												 "I'm just a sweet tramsvestite from tramsilvania",
												 "Your daughter came into my yard and kicked my tram",
												 "I always relied on the kindness of passengers",
												 "Trams like turtles",
												 "Loltram",
												 "Because we tram, tram, tram...",
												 "Leave Tramney alone!",
												 "Tramomenom... do doo do do do",
												 "If you cut me, do I not ding?",
												 "Everybody knows, those Trams don't work on water",
												 "I'd ride that for a dollar",
												 "Step away from the tram. You have 20 seconds to comply",
												 "Of all the trams in all the world, you had to walk onto mine",
												 "Yes we tram",
												 "Shot through the heart, and you're to blame, you give trams a bad name",
												 "He's not the messiah, he's a very naughty tram",
												 "Stop, Trammer time",
												 "Trams, no here chopper",
												 "To tram or not to tram, that is the question"};
                                    	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.home);
		
		// Get shared prefs
		preferenceHelper = new PreferenceHelper(this);	
		
		// Create db instance
		db = new TramHunterDB(this);
		
		
		boolean firstLaunch = checkFirstLaunch();
		if (firstLaunch == false) {
			showAbout();
		} 
		else {
			if (preferenceHelper.isFavouriteOnLaunchEnabled()) {
				// If go to fav on launch is set in prefs, and we have some favs set
				List<Stop> stops = db.getFavouriteStops();
				if (stops.size() > 0) {
					// Go to favourite stops
					Intent intent = new Intent(TramHunter.this, StopsListActivity.class);
					startActivityForResult(intent, 1);
				}
				
			}
			displayMenu();
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		// Comment out here to only show messages each time the app is opened.
		setRandomWelcomeMessage();
	}
	
	public boolean checkFirstLaunch() {
		TramHunterDB db = new TramHunterDB(this);
		boolean firstLaunch = db.checkFirstLaunch();
		return firstLaunch;

	}

	private static String getRandomWelcomeMessage(){
		Random r = new Random(System.currentTimeMillis());
		return m_welcomeMessages[r.nextInt(m_welcomeMessages.length - 1)];
	}
	
	public void showAbout() {
		// Get the package name
		String heading = getResources().getText(R.string.app_name) + "\n";
		
        // Get the package version
        PackageManager pm = getPackageManager();
        try {
			PackageInfo pi = pm.getPackageInfo("com.andybotting.tramhunter", 0);
			heading += "v" + pi.versionName + "\n\n";
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
		// Build alert dialog
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		dialogBuilder.setTitle(heading);
		dialogBuilder.setMessage(getResources().getText(R.string.about_msg));
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
		dialogBuilder.setIcon(R.drawable.icon);
		dialogBuilder.show();
	}

	private void setRandomWelcomeMessage() {
		
		TextView welcomeMessageTextView = (TextView) findViewById(R.id.welcomeMessage);
		String welcomeText = "";

		if (preferenceHelper.isWelcomeQuoteEnabled())
			welcomeText = "\"" + getRandomWelcomeMessage()+ "\"";
		
        welcomeMessageTextView.setText(welcomeText);
        
	}
	
	
	public void displayMenu() { 		
		setRandomWelcomeMessage();
		
		m_listView = (ListView)this.findViewById(android.R.id.list);
		setTitle(getResources().getText(R.string.app_name));

		m_listView.setOnItemClickListener(new OnItemClickListener() {

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
					case 4: {
						Intent intent = new Intent(TramHunter.this, SettingsActivity.class);
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
			return m_menuItems.length;
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
				
			wrapper.getTextLabel1().setText(m_menuItems[position]);
			wrapper.getTextLabel2().setText(m_menuDesc[position]);

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
		
		menu.add(0, 1, 0, "Settings");
		MenuItem menuItem2 = menu.findItem(1);
		menuItem2.setIcon(android.R.drawable.ic_menu_preferences);

		return true;
	}

	
	// Menu actions
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			showAbout();
			return true;
		case 1:
			Intent intent = new Intent(TramHunter.this, SettingsActivity.class);
			startActivityForResult(intent, 1);
			return true;
		}
		return false;

	}
	  
}