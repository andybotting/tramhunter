package com.andybotting.tramhunter.activity;

import java.util.Vector;

import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.dao.TramHunterDB;
import com.andybotting.tramhunter.Route;
import com.andybotting.tramhunter.Destination;

import android.app.ExpandableListActivity;
import android.content.Context;

import android.content.Context;
import android.content.Intent;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import android.widget.ExpandableListView.OnChildClickListener;

public class RoutesListActivity extends ExpandableListActivity {

	private List<Route> routes;
	private ExpandableListAdapter mAdapter;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);	  
		
		String title = "Routes";
		setTitle(title);
		
		getExpandableListView().setOnChildClickListener(new OnChildClickListener() {
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				
				// TODO: Fix this dirty hack to get the destinationId. It makes the wild
				// assumption that each route has exactly 2 destinations ordered sequentially
				// which we shouldn't depend on.
				int destinationId = groupPosition * 2 + childPosition;
				
				Bundle bundle = new Bundle();
				bundle.putLong("destinationId", destinationId);
				Intent stopsListIntent = new Intent(RoutesListActivity.this, StopsListActivity.class);
				stopsListIntent.putExtras(bundle);
				startActivityForResult(stopsListIntent, 1);
				return true;
			}
		});
	 
		displayRoutes();
	}
	
	
		
	public void displayRoutes() {
		TramHunterDB db = new TramHunterDB(this);
		routes = db.getRoutes();
		mAdapter = new MyExpandableListAdapter();
        setListAdapter(mAdapter);
	}
  
	
    public class MyExpandableListAdapter extends BaseExpandableListAdapter {
		
		private Context mContext;		

        public Object getChild(int groupPosition, int childPosition) {
        	// Decide if it's routeUp or routeDown
        	if (childPosition == 0)
        		return routes.get(groupPosition).getDestinationUp();
            return routes.get(groupPosition).getDestinationDown();
        }

        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        public int getChildrenCount(int groupPosition) {
        	// Always two routes for each routeGroup
            return 2;
        }

        
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

            AbsListView.LayoutParams lp = new AbsListView.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 50);

            TextView textView = new TextView(RoutesListActivity.this);
            textView.setLayoutParams(lp);
            
            textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            
            textView.setPadding(40, 0, 0, 0);
            textView.setTextSize(20);
            
			Route route = routes.get(groupPosition);
			Destination dest;
			if (childPosition == 0) {
				dest = route.getDestinationUp();
			}
			else {
				dest = route.getDestinationDown();
			}

			textView.setText("To " + dest.getDestination());
			
			return textView;
        }

        public Object getGroup(int groupPosition) {
            return routes.get(groupPosition);
        }

        public int getGroupCount() {
            return routes.size();
        }

        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View pv = convertView;
			ViewWrapper wrapper = null;
            
            if ( pv == null ) {
                    LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    pv = inflater.inflate (R.layout.routes_list_row, null );
            }

			wrapper = new ViewWrapper(pv);
			pv.setTag(wrapper);

			Route route = routes.get(groupPosition);
				
			wrapper.getTextLine1().setText("Route " + route.getNumber());
			wrapper.getTextLine2().setText(route.getDestinationString());
			
			wrapper.getTextLine1().setPadding(32, 0, 0, 0);
			wrapper.getTextLine2().setPadding(32, 0, 0, 0);
			
			return pv;
        }

        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        public boolean hasStableIds() {
            return true;
        }

    }
	

	class ViewWrapper {
		View base;
			
		TextView textLine1 = null;
		TextView textLine2 = null;
			

		ViewWrapper(View base) {
			this.base = base;
		}

		TextView getTextLine1() {
			if (textLine1 == null) {
				textLine1 = (TextView) base.findViewById(R.id.textLine1);
			}
			return (textLine1);
		}

		TextView getTextLine2() {
			if (textLine2 == null) {
				textLine2 = (TextView) base.findViewById(R.id.textLine2);
			}
			return (textLine2);
		}

	}
	  
}
