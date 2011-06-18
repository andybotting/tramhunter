/*  
 * Copyright 2010 Andy Botting <andy@andybotting.com>  
 *  
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * This file is distributed in the hope that it will be useful, but  
 * WITHOUT ANY WARRANTY; without even the implied warranty of  
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU  
 * General Public License for more details.  
 *  
 * You should have received a copy of the GNU General Public License  
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  
 *  
 * This file incorporates work covered by the following copyright and  
 * permission notice:
 * 
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andybotting.tramhunter.dao;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import java.util.List;

import com.andybotting.tramhunter.objects.Stop;

/**
 * Provides search suggestions for a list of words and their definitions.
 */
public class TramHunterProvider extends ContentProvider {

    public static String AUTHORITY = "com.andybotting.tramhunter";

    private static final int SEARCH_SUGGEST = 0;
    
    private static final UriMatcher sURIMatcher = buildUriMatcher();

    private static final String[] COLUMNS = {
    	BaseColumns._ID,
    	SearchManager.SUGGEST_COLUMN_TEXT_1,
    	SearchManager.SUGGEST_COLUMN_TEXT_2,
    	SearchManager.SUGGEST_COLUMN_INTENT_DATA,
    };

    private static UriMatcher buildUriMatcher() {
    	UriMatcher matcher =  new UriMatcher(UriMatcher.NO_MATCH);
    	matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
    	matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);
    	return matcher;
    }

    @Override
    public boolean onCreate() {
    	return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    	if (!TextUtils.isEmpty(selection)) {
        	throw new IllegalArgumentException("selection not allowed for " + uri);
    	}
    	if (selectionArgs != null && selectionArgs.length != 0) {
    		throw new IllegalArgumentException("selectionArgs not allowed for " + uri);
    	}
    	if (!TextUtils.isEmpty(sortOrder)) {
    		throw new IllegalArgumentException("sortOrder not allowed for " + uri);
    	}
    	switch (sURIMatcher.match(uri)) {
    	case SEARCH_SUGGEST:
            	String query = null;
                if (uri.getPathSegments().size() > 1) {
                    query = uri.getLastPathSegment().toLowerCase();
                }
                return getSuggestions(query, projection);
    	default:
    		throw new IllegalArgumentException("Unknown URL " + uri);
    	}
	}

	private Cursor getSuggestions(String query, String[] projection) {
		String processedQuery = query == null ? "" : query.toLowerCase();
		MatrixCursor cursor = new MatrixCursor(COLUMNS);
          
		// Inefficient use of the DB!
		TramHunterDB mDB = new TramHunterDB(); 
		List<Stop> stops = mDB.getStopsForSearch(processedQuery);
		mDB.close();
        
		long id = 0;
		for (Stop stop: stops) {
			cursor.addRow(columnValuesOfWord(id++, stop));
		}

		return cursor;
    }

    private Object[] columnValuesOfWord(long id, Stop stop) {
    	
		String text1 = stop.getStopName();
		String text2 = "Stop " + stop.getFlagStopNumber() + ": ";
		text2 += stop.getCityDirection();
		text2 += " (" + stop.getTramTrackerID() + ") " + stop.getRoutesString();

		// Return an object suitable for Android search
		return new Object[] {id, text1,	text2, stop.getTramTrackerID(), };
    }

    public String getType(Uri uri) {
        switch (sURIMatcher.match(uri)) {
            case SEARCH_SUGGEST:
                return SearchManager.SUGGEST_MIME_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }

    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}
