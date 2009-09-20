package com.bisbe.contacts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.CursorJoiner;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Contacts;
import android.provider.Contacts.GroupMembership;
import android.provider.Contacts.Groups;
import android.provider.Contacts.People;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.TabHost;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TabHost.TabSpec;
public class ContactsEvolved extends TabActivity {
    /** Called when the activity is first created. */

	final String SHOW_GROUPS = "showGroups";
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		

        super.onCreate(savedInstanceState);   
        setContentView(R.layout.main);
    
        setupTabs();
	}

	public void setupTabs()
	{
		tabNames = null;
		removeDialog(SELECT_GROUP_DIALOG); 
        TabHost mTabHost = getTabHost();
        mTabHost.setCurrentTab(0);
        mTabHost.clearAllTabs();
        mTabHost = getTabHost();
//		ContactsListView lv = (ContactsListView) findViewById(R.id.listView1);
        FrameLayout fl = (FrameLayout) findViewById(android.R.id.tabcontent);
        
        Cursor c = managedQuery(People.CONTENT_URI, null, null, null, People._ID + " ASC");
        Cursor groupsCursor = getGroupsCursor();

        generateTabs(groupsCursor, c, fl);
        mTabHost.setCurrentTab(0);
        mTabHost.refreshDrawableState();
		
	}
	
	private int useID = 1234;
	private ArrayList<String> tabNames;
	private void addTab(FrameLayout fl, ContactsListView lv, String tabTitle)
	{
		if(tabNames == null)
			tabNames = new ArrayList<String>();
		Log.d("AddTab", "Entered AddTab");
		fl.addView(lv);
		lv.setId(useID++);
		TabHost mTabHost = getTabHost();
		if (mTabHost == null)
			Log.d("AddTab", "mTabHost is null");
		if (tabTitle == null)
			Log.d("AddTab", "Tab Title is null");
		if (lv == null)
			Log.d("AddTab", "The Listview is null");
		if (lv.getId() == View.NO_ID)
			Log.d("AddTab", "The Listview has no id");
	
		String useName = tabTitle;
		useName = useName.replaceAll("System Group: ", "");
		TabSpec useSpec = mTabHost.newTabSpec(tabTitle).setIndicator(useName).setContent(lv.getId());
		if (useSpec == null)
			Log.d("AddTab", "The Spec is null");

		tabNames.add(tabTitle);
		mTabHost.addTab(useSpec);
	}
	
	private Cursor getGroupMembershipCursor(String groupName)
	{
		//Get cursor for Group Membership.  Filter for specific group from this iteration of the loop.
		String gmWhereClause = Contacts.GroupMembership.NAME + "= " + "'" + groupName + "'";
		Cursor gmCursor = managedQuery(GroupMembership.CONTENT_URI,
										null,
										gmWhereClause,
										null,
										GroupMembership.PERSON_ID + " ASC");
		return gmCursor;
	}

	
	private MatrixCursor genMatrix(CursorJoiner cj, Cursor gmCursor, Cursor contactsCursor)
	{
		MatrixCursor mCursor = new MatrixCursor( new String[] {People._ID,People.NAME, People.NUMBER},10);
//		SortedMap<String, String> mapResults = new TreeMap<String, String>();
		ArrayList<String[]> listResults = new ArrayList<String[]>();
        for (CursorJoiner.Result joinerResult : cj) 
        {
            switch (joinerResult) 
            {
                    case BOTH: // handle case where a row with the same key is in both cursors
                            String id = contactsCursor.getString(contactsCursor.getColumnIndex(People._ID));
                            String name = contactsCursor.getString(contactsCursor.getColumnIndex(People.NAME));
                            String phone = contactsCursor.getString(contactsCursor.getColumnIndex(People.NUMBER));
                            if(phone == null)
                            	phone = "";
                            listResults.add(new String[] {id, name, phone});
                            
                            Log.d("Groups", "From GM: GM Name: " + gmCursor.getString(gmCursor.getColumnIndex(GroupMembership.NAME)) + ", GroupID: " + gmCursor.getString(gmCursor.getColumnIndex(GroupMembership.GROUP_ID)));
                            break;
            }
            
        }

 
        Collections.sort(listResults, new Comparator<String[]>() {
            public int compare(String[] obj1, String[] obj2) {
                return obj1[1].compareTo(obj2[1]);
            }
        });

        for(String[] entry : listResults)
        {
        	mCursor.addRow(entry);
        }
        
        return mCursor;
	}

	
	private void generateTabs(Cursor groupsCursor, Cursor contactsCursor, FrameLayout fl)
	{
		int groupNameIndex = groupsCursor.getColumnIndex(Groups.NAME);
		SharedPreferences settings = this.getSharedPreferences(SHOW_GROUPS, 0);
		
		if (groupsCursor.moveToFirst())
		{
			do 
			{
				
				String groupName = groupsCursor.getString(groupNameIndex);
				if(settings.contains(groupName) && settings.getBoolean(groupName, true) == false)
				{
					continue;
				}
				//Get cursor for Group Membership.  Filter for specific group from this iteration of the loop.
				Cursor gmCursor = getGroupMembershipCursor(groupsCursor.getString(groupNameIndex));	
				String cols = "";
				int numCols = groupsCursor.getColumnCount();
				for (int i = 0; i < numCols; i++)
				{
					cols += groupsCursor.getColumnName(i) + ",";
				}
				
				Log.d("Groups","DEBUG: Column Names: " + cols);
				Log.d("Groups","DEBUG: Name: " + groupsCursor.getString(groupNameIndex) + ", GroupID: " + groupsCursor.getString(groupsCursor.getColumnIndex(Groups._ID)));
				Log.d("Groups","DEBUG: Group Count: " + gmCursor.getCount());
				
				
				//Create a join of Contacts and GroupMembership for the specified group.
				CursorJoiner cj = new CursorJoiner(gmCursor, new String[] {GroupMembership.PERSON_ID}, contactsCursor, new String[] {People._ID});

				//Use the above CursorJoiner to populate a new Cursor with the information we need to populate a listview.
                MatrixCursor mCursor = genMatrix(cj, gmCursor, contactsCursor);
                ContactsListView lv = new ContactsListView(this);
                ListAdapter adapter = new ContactsAdapter(this,R.layout.contact_list_item,mCursor,new String[] {People.NAME,People.NUMBER}, new int[] 
            		    {R.id.firstLine, R.id.secondLine}); 

                lv.setAdapter(adapter);

                addTab(fl, lv, groupName);

			} while (groupsCursor.moveToNext() && !groupsCursor.isAfterLast());
		}
		Log.d("Groups","DEBUG: Done loading groups");

	}
	
	private Cursor getGroupsCursor()
	{
		// Form an array specifying which columns to return. 
//		String[] projection = new String[] { Groups._ID, Groups._COUNT, Groups.NAME};

		// Make the query. 
		Cursor groupsCursor = managedQuery(Groups.CONTENT_URI,
		                         null, // Which columns to return 
		                         null,       // Which rows to return (all rows)
		                         null,       // Selection arguments (none)
		                         // Put the results in ascending order by name
		                         Groups.NAME + " ASC");

		return groupsCursor;
	}

	private Cursor getGroupCursorByName(String  groupName)
	{
		Cursor groupsCursor = managedQuery(Groups.CONTENT_URI,
                null, // Which columns to return 
                Groups.NAME + " == '" + groupName + "'" ,       // Which rows to return (all rows)
                null,       // Selection arguments (none)
                // Put the results in ascending order by name
                Groups.NAME + " ASC");

		
		return groupsCursor;
		
	}
	
	public static final int ADD_GROUP = 0;
	public static final int DELETE_GROUP = 1;
	public static final int SELECT_VISIBLE_GROUPS = 2;
	public boolean onCreateOptionsMenu(Menu menu) 
	{
	    menu.add(0, SELECT_VISIBLE_GROUPS, 0, "Select shown groups");
	    menu.add(0, ADD_GROUP, 0, "Add a group");
	    menu.add(0, DELETE_GROUP, 0, "Delete this group");

	    
	    
	    return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case ADD_GROUP:
	        addNewGroup();
	        return true;
	    case DELETE_GROUP:
	    	deleteCurrentGroup();
	        return true;
	    case SELECT_VISIBLE_GROUPS:
	    	selectGroups();
	    	
	        
	    }
	    return false;
	}
	
	public void deleteCurrentGroup()
	{
		showDialog(DELETE_GROUP_DIALOG);
	}
	
	public void addNewGroup()
	{
		showDialog(ADD_GROUP_DIALOG);
	}
	
	public void selectGroups()
	{
		showDialog(SELECT_GROUP_DIALOG);
	}
	
	
	
	public static final int ADD_GROUP_DIALOG = 0;
	public static final int DELETE_GROUP_DIALOG = 1;
	public static final int SELECT_GROUP_DIALOG = 2;
	public static final int ADD_MEMBERSHIP_DIALOG = 3;
	public static final int REMOVE_MEMBERSHIP_DIALOG = 4;
    @Override
    protected Dialog onCreateDialog(int id) 
    {
    	switch(id)
    	{
	    	case ADD_GROUP_DIALOG:
	        LayoutInflater factory = LayoutInflater.from(this);
	        final View textEntryView = factory.inflate(R.layout.alert_dialog_text_entry, null);
	        return new AlertDialog.Builder(this)
	            .setTitle("Add New Group")
	            .setView(textEntryView)
	            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) 
	                {
	                	ContentValues values = new ContentValues();
	
	                	EditText et = (EditText) textEntryView.findViewById(R.id.groupname_edit);
	                	values.put(Groups.NAME, et.getText().toString());
	
	                	Uri uri = getContentResolver().insert(Groups.CONTENT_URI, values);
	                	setupTabs();
	                    /* User clicked OK so do some stuff */
	                }
	            })
	            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	
	                    /* User clicked cancel so do some stuff */
	                }
	            })
	            .create();
	    	case DELETE_GROUP_DIALOG:
	    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    		builder.setMessage("Are you sure you want to delete this group?")
	    		       .setCancelable(false)
	    		       .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	    		           public void onClick(DialogInterface dialog, int id) 
	    		           {
		    		       		int tabIndex = getTabHost().getCurrentTab();
		    		       		String groupName = tabNames.get(tabIndex);
		    		       		
		    		    		Cursor groupCursor = getGroupCursorByName(groupName);
		    		    		groupCursor.moveToFirst();
		    		    		int groupIdIndex = groupCursor.getColumnIndex(Groups._ID);
		    		    		int systemGroupIndex = groupCursor.getColumnIndex(Contacts.GroupsColumns.SYSTEM_ID);
		    		    		if(!groupCursor.isNull(systemGroupIndex))
		    		    		{
		    		    			Toast.makeText(getApplicationContext(), "Cannot delete System Group.", Toast.LENGTH_SHORT).show();
		    		    			return;
		    		    		}
		    		    		
		    		    		long groupID = groupCursor.getLong(groupIdIndex);
		    		    		getContentResolver().delete(Groups.CONTENT_URI, Groups._ID + "== " + groupID, null);
			                	setupTabs();
	    		               
	    		           }
	    		       })
	    		       .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
	    		           public void onClick(DialogInterface dialog, int id) 
	    		           {
	    		                dialog.cancel();
	    		           }
	    		       });
	    		AlertDialog confirmDelete = builder.create();
	    		return confirmDelete;
	    	case SELECT_GROUP_DIALOG:
	    		
                AlertDialog.Builder sgBuilder = new AlertDialog.Builder(this)
                .setTitle("Select groups to view")
	    		       .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	    		           public void onClick(DialogInterface dialog, int id) 
	    		           {
	    		               setupTabs();
	    		           }
	    		       })
	    		       .setCancelable(true);
                		Cursor c = getGroupsCursor();

                		SharedPreferences settings = this.getSharedPreferences(SHOW_GROUPS, 0);
                		Editor editor = settings.edit();
                		
                		int size = c.getCount();
                		final CharSequence[] groups = new CharSequence[size];
                		final boolean[] checkedArray = new boolean[size];
			            
			    		if (c.moveToFirst())
			    		{
			    			do 
			    			{
			    				String groupName = c.getString(c.getColumnIndex(Groups.NAME));
			    				groups[c.getPosition()] = groupName;

			    				if(!settings.contains(groupName))
			    				{
			    					editor.putBoolean(groupName, true);
			    				}
			    				
			    				checkedArray[c.getPosition()] = settings.getBoolean(groupName, true);
			    				
			    			} while (c.moveToNext() && !c.isAfterLast());
			    		}
			    		editor.commit();
			    		
			    		

			            sgBuilder.setMultiChoiceItems(groups, checkedArray, new DialogInterface.OnMultiChoiceClickListener() {
			                public void onClick(DialogInterface dialog, int item, boolean checked) 
			                {			                	
			                	updateSelectedGroups(groups[item].toString(), checked);
			                }
			            });

			            return sgBuilder.create();

	    	case ADD_MEMBERSHIP_DIALOG:
	    		
                AlertDialog.Builder personGroupBuilder = new AlertDialog.Builder(this)
                .setTitle("Add contact to group:")
	    		       .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	    		           public void onClick(DialogInterface dialog, int id) 
	    		           {
	    		        	   if(membershipGroupName.equals(Groups.GROUP_MY_CONTACTS))
	    		        		   People.addToMyContactsGroup(getContentResolver(), membershipPersonID);
	    		        	   else
	    		        		   Contacts.People.addToGroup(getContentResolver(), membershipPersonID, membershipGroupName);
	    		               membershipGroupName = null;
	    		               membershipPersonID = -1;
	    		               setupTabs();
	    		           }
	    		       })
	    		       .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
	    		           public void onClick(DialogInterface dialog, int id) 
	    		           {
	    		                dialog.cancel();
	    		           }
	    		       })
	    		       .setCancelable(true);
                		Cursor allGroupsCursor = getGroupsCursor();
                		int agcSize = allGroupsCursor.getCount();

                		final CharSequence[] allGroups = new CharSequence[agcSize];
			            
			    		if (allGroupsCursor.moveToFirst())
			    		{
			    			do 
			    			{
			    				String groupName = allGroupsCursor.getString(allGroupsCursor.getColumnIndex(Groups.NAME));
			    				allGroups[allGroupsCursor.getPosition()] = groupName;
			    							    				
			    			} while (allGroupsCursor.moveToNext() && !allGroupsCursor.isAfterLast());
			    		}
			    		
			    		

			    		personGroupBuilder.setSingleChoiceItems(allGroups, -1, new DialogInterface.OnClickListener() {
			    		    public void onClick(DialogInterface dialog, int item) {
			    		      
			    		        membershipGroupName = (String) allGroups[item];
			    		    }
			    		});


			            return personGroupBuilder.create();

//Remove Membership Case
	    	case REMOVE_MEMBERSHIP_DIALOG:
	    		
                personGroupBuilder = new AlertDialog.Builder(this)
                .setTitle("Remove contact from group:")
	    		       .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	    		           public void onClick(DialogInterface dialog, int id) 
	    		           {

	    		        	   String personClause = GroupMembership.PERSON_ID + " == " + membershipPersonID;
	    		               String nameClause = Groups.NAME + "== " + membershipGroupName;
	    		               String whereClause = personClause + " AND " + nameClause;
	    		               Uri uri = Uri.parse(GroupMembership.CONTENT_URI.toString() + "/" + membershipGroupName);
	    		               getContentResolver().delete(uri, whereClause, null);
	    		               setupTabs();
	    		        	   
	    		        	   membershipGroupName = null;
	    		               membershipPersonID = -1;
	    		               setupTabs();

	    		           }
	    		       })
	    		       .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
	    		           public void onClick(DialogInterface dialog, int id) 
	    		           {
	    		                dialog.cancel();
	    		           }
	    		       })	    		       
	    		       .setCancelable(true);
                		Cursor currentGroupsCursor = People.queryGroups(getContentResolver(), membershipPersonID);
                		int cgcSize = currentGroupsCursor.getCount();

                		final CharSequence[] currentGroups = new CharSequence[cgcSize];
			            
			    		if (currentGroupsCursor.moveToFirst())
			    		{
			    			do 
			    			{
			    				String groupName = currentGroupsCursor.getString(currentGroupsCursor.getColumnIndex(Groups.NAME));
			    				currentGroups[currentGroupsCursor.getPosition()] = groupName;
			    							    				
			    			} while (currentGroupsCursor.moveToNext() && !currentGroupsCursor.isAfterLast());
			    		}
			    		
			    		

			    		personGroupBuilder.setSingleChoiceItems(currentGroups, -1, new DialogInterface.OnClickListener() {
			    		    public void onClick(DialogInterface dialog, int item) {

			    		        membershipGroupName = (String) currentGroups[item];
			    		    }
			    		});


			            return personGroupBuilder.create();
			            
			            
    	}
    	return null;
    
    }

    public void updateSelectedGroups(String key, boolean value)
    {
    	
	    	SharedPreferences settings = this.getSharedPreferences(SHOW_GROUPS, 0);
			Editor editor = settings.edit();
			editor.putBoolean(key, value);
			editor.commit();
    }
    
     public static final int EDIT_CONTACT_DETAILS = 0;
     public static final int DELETE_CONTACT = 1;
     public static final int ADD_GROUP_MEMBERSHIP = 2;
     public static final int REMOVE_GROUP_MEMBERSHIP = 3;
     
	 public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) 
	 {
		 	super.onCreateContextMenu(menu, v, menuInfo);
		 	menu.add(0, EDIT_CONTACT_DETAILS, 0,  "Edit Contact Details");
		 	menu.add(0, DELETE_CONTACT, 0,  "Delete Contact Details");
		 	menu.add(0, ADD_GROUP_MEMBERSHIP, 0, "Add to Group");

		 	AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

	 }

	 public boolean onContextItemSelected(MenuItem item) 
	 {
		 AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		 if(info == null)
			 Log.d("Context Menu", "info is null");
		 if(info.targetView == null)
			 Log.d("Context Menu", "targetView is null");
		 if(info.targetView.getTag() == null)
			 Log.d("Context Menu", "Tag is null");
		 long personId =  info.id;
		 switch (item.getItemId()) 
		 {
		 	case ADD_GROUP_MEMBERSHIP:
		 		addGroupMembership(personId);
		 		return true;
		 	case REMOVE_GROUP_MEMBERSHIP:
		 		removeGroupMembership(personId);
		 		return true;
		 	case EDIT_CONTACT_DETAILS:
		 		editContact(info.targetView,personId);
		 		return true;
		 	case DELETE_CONTACT:
		 		deleteContact(info.targetView,personId);
		 		return true;
		 	default:
		 		return super.onContextItemSelected(item);
		 }
		 
	 }
	 
	 private long membershipPersonID;
	 private String membershipGroupName;
	 private void addGroupMembership(long personID)
	 {
		 membershipPersonID = personID;
		 showDialog(ADD_MEMBERSHIP_DIALOG);
	 }
	 
	 private void removeGroupMembership(long personID)
	 {
		 membershipPersonID = personID;
		 showDialog(REMOVE_MEMBERSHIP_DIALOG);
	 }
	 
	 private void editContact(View v, long personId)
	 {
		 	Uri personUri = ContentUris.withAppendedId(People.CONTENT_URI, personId);
		 	Log.d("edit contact", "uri: " + personUri.toString());
		 	Intent myIntent = new Intent(Intent.ACTION_EDIT, personUri); 
	    	v.getContext().startActivity(myIntent);
	    	setupTabs();
		 
	 }

	 private void deleteContact(View v,long personId)
	 {
		 	Uri personUri = ContentUris.withAppendedId(People.CONTENT_URI, personId);
		 	Log.d("edit contact", "uri: " + personUri.toString());
		 	Intent myIntent = new Intent(Intent.ACTION_DELETE, personUri); 
	    	v.getContext().startActivity(myIntent);
	    	setupTabs();

	 }
	 
    
}