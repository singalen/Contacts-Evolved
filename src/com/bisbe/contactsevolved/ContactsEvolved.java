package com.bisbe.contactsevolved;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Contacts;
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
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TabHost.TabSpec;

public class ContactsEvolved extends TabActivity {

	final String SHOW_GROUPS = "showGroups";
	
	@Override
    public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
    	Log.d("Starting Program", "After Oncreate");
        setContentView(R.layout.main);
    	Log.d("Starting Program", "After setContentView");
        setupTabs();
    	Log.d("Starting Program", "After setupTabs");
	}

	public void setupTabs()
	{
		tabNames = null;
		removeDialog(SELECT_GROUP_DIALOG); 
		removeDialog(REMOVE_MEMBERSHIP_DIALOG);
		removeDialog(ADD_MEMBERSHIP_DIALOG);

		getTabHost().setVisibility(View.GONE);
        //http://code.google.com/p/android/issues/detail?id=2772#c8
        damnClearTabs();
        //mTabHost = getTabHost(); // TODO: WTF?..

        FrameLayout fl = (FrameLayout) findViewById(android.R.id.tabcontent);
        Cursor groupsCursor = getGroupsCursor();
        try
        {
            generateTabs(groupsCursor, fl);
        }
        finally 
        {
            groupsCursor.close();
        }
        getTabHost().setCurrentTab(0);
        getTabHost().setVisibility(View.VISIBLE);
        getTabHost().refreshDrawableState();
	}

    private void damnClearTabs()
    {
        if(getTabHost().getChildCount() > 0)
        {
            getTabHost().setCurrentTab(-1);
            getTabHost().clearAllTabs();
        }
    }
	
	private int useID = 1234;
	private ArrayList<String> tabNames;
	
	private ArrayList<String> getTabNames() {
        if(tabNames == null) tabNames = new ArrayList<String>();
        return tabNames;
	}
	
	private void addTab(FrameLayout fl, String groupName)
	{
        ContactsListView lv = new ContactsListView(this);
        lv.setOnItemClickListener(contactClickListener);
        this.registerForContextMenu(lv);
        
        lv.setAdapter(new ContactsAdapter(this, R.layout.contact_list_item, groupName));
        //lv.setAdapter(createGroupContactsAdapter(groupName));
		
		fl.addView(lv);
		lv.setId(useID++);
	
		// TODO: Add icons on tops of at least system groups.
		String useName = groupName.length() == 0 ? "All contacts" : groupName;
		useName = useName.replaceAll("System Group: ", "");
		TabSpec useSpec = getTabHost().newTabSpec(groupName).setIndicator(useName).setContent(lv.getId());
		
		getTabNames().add(groupName);
		getTabHost().addTab(useSpec);
	}
	
    private final AdapterView.OnItemClickListener contactClickListener = new AdapterView.OnItemClickListener() 
    {
        public void onItemClick(AdapterView<?> parent, View v, int position, long dbidentifier)
        {
            // Seems to only fire when keyboard selected, not on touch event?
            Uri viewContactURI = Uri.parse("content://contacts/people/" + dbidentifier);
            Log.d("Stuff", String.valueOf(dbidentifier));
            Intent myIntent = new Intent(Intent.ACTION_VIEW, viewContactURI);
            startActivity(myIntent);
        }
    };
	
	private void generateTabs(Cursor groupsCursor, FrameLayout fl)
	{
		int groupNameIndex = groupsCursor.getColumnIndex(Groups.NAME);
		SharedPreferences settings = this.getSharedPreferences(SHOW_GROUPS, 0);
		
		if (groupsCursor.moveToFirst())
		{
			int shown = 0;
			do 
			{
				String groupName = groupsCursor.getString(groupNameIndex);
				boolean onlyGroup = groupsCursor.isLast() && shown == 0;
				
				if (settings.getBoolean(groupName, true) == false && !onlyGroup)
				{
					continue;
				}
				
				Log.d("generateTabs", "Tab to be generated: " + groupName);
				shown++;
				
                addTab(fl, groupName);

			} while (groupsCursor.moveToNext() && !groupsCursor.isAfterLast());
		}
		else 
		{
            addTab(fl, "");
		}
		Log.d("Groups", "DEBUG: Done loading groups");
	}
	
	private Cursor getGroupsCursor()
	{
		return managedQuery(Groups.CONTENT_URI,
		                         null, // All columns
		                         null, // All rows
		                         null, // Put the results in ascending order by name
		                         Groups.NAME + " ASC");
	}

	private Cursor getGroupCursorByName(String groupName)
	{
		Cursor groupsCursor = managedQuery(Groups.CONTENT_URI,
                null, // Which columns to return 
                Groups.NAME + " == '" + groupName + "'" , // Which rows to return (all rows)
                null, // Selection arguments (none)
                Groups.NAME + " ASC");

		
		return groupsCursor;
	}
	
	public static final int ADD_GROUP = 0;
	public static final int DELETE_GROUP = 1;
	public static final int SELECT_VISIBLE_GROUPS = 2;
	public static final int ADD_CONTACT = 3;
	public boolean onCreateOptionsMenu(Menu menu) 
	{
	    menu.add(0, DELETE_GROUP, 0, "Delete This Group").setIcon(R.drawable.minus);
	    menu.add(0, ADD_GROUP, 0, "Add a Group").setIcon(R.drawable.plus);
	    menu.add(0, SELECT_VISIBLE_GROUPS, 0, "Select Shown Groups").setIcon(R.drawable.people);
	    menu.add(0,ADD_CONTACT,0, "Create New Contact").setIcon(R.drawable.person);
	    
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
	    	return true;
	    case ADD_CONTACT:
	    	addContact();
	    	return true;
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
	public static final int DELETE_CONTACT_DIALOG = 5;

	
	@Override
    protected Dialog onCreateDialog(int id) 
    {
    	switch(id)
    	{
    		//Create a new group.  WILL SYNC.
	    	case ADD_GROUP_DIALOG:
	    	    return createAddGroupDialog();
	        //Delete a contact. WILL SYNC.
	    	case DELETE_CONTACT_DIALOG:
	    	    return createConfirmDeleteDialog();
	    		
	    	//Delete a group from database.  WILL SYNC.
	    	case DELETE_GROUP_DIALOG:
                return createConfirmDeleteGroupDialog();
	    	//Select which groups are currently visible.
	    	case SELECT_GROUP_DIALOG:
	    	    return createSelectGroupsDialog();

			//Add an existing contact to a group. WILL SYNC.
	    	case ADD_MEMBERSHIP_DIALOG:
                return createAddMemebershipDialog();

			//Remove a contact from a group. WILL SYNC.
	    	case REMOVE_MEMBERSHIP_DIALOG:
                return createRemoveMemebershipDialog();
    	}
    	return null;
    }

    private Dialog createRemoveMemebershipDialog()
    {
        AlertDialog.Builder personGroupBuilder;
        personGroupBuilder = new AlertDialog.Builder(this)
        .setTitle("Remove contact from group:")
               .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) 
                   {

                	   if(membershipPersonID < 0 || membershipGroupName == null)
                	   {
                		   return;
                	   }
                	   long membershipID = getGroupMembershipID(membershipPersonID, membershipGroupName);
                	   removeGroupMembershipByID(membershipID);
                	   membershipGroupName = null;
                       membershipPersonID = -1;
                       setupTabs();
                   }
               })
               .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) 
                   {
                        dialog.cancel();
                		removeDialog(REMOVE_MEMBERSHIP_DIALOG);

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

    private Dialog createAddMemebershipDialog()
    {
        AlertDialog.Builder personGroupBuilder = new AlertDialog.Builder(this)
        .setTitle("Add contact to group:")
               .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) 
                   {
                	   Log.d("add membership dialog", membershipPersonID + " adding to " + membershipGroupName);
                	   if(membershipGroupName.equals(Groups.GROUP_MY_CONTACTS))
                	   {
                		   People.addToMyContactsGroup(getContentResolver(), membershipPersonID);
                		   Toast.makeText(getApplicationContext(), "Added to My Contacts", Toast.LENGTH_SHORT);
        	        	   Log.d("add membership dialog", "if called");

                	   }
                	   else
                	   {
                		   Contacts.People.addToGroup(getContentResolver(), membershipPersonID, membershipGroupName);
                		   Toast.makeText(getApplicationContext(), "Added to " + membershipGroupName, Toast.LENGTH_SHORT);
        	        	   Log.d("add membership dialog", "else called");

                	   }
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
    }

    private Dialog createSelectGroupsDialog()
    {
        Log.d("OnCreateDialog", "SELECT_GROUP_DIALOG case active");
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
    }

    private Dialog createConfirmDeleteGroupDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to delete this group?")
               .setCancelable(false)
               .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) 
                   {
        	       		long groupID = getCurrentGroupID();
        	       		if(groupID == -1)
        	       			return;
        	    		getContentResolver().delete(Groups.CONTENT_URI, Groups._ID + " == " + groupID, null);
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
    }

    private Dialog createConfirmDeleteDialog()
    {
        AlertDialog.Builder dcBuilder = new AlertDialog.Builder(this);
        dcBuilder.setMessage("Are you sure you want to delete this contact?")
               .setCancelable(false)
               .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) 
                   {

           		 	Uri personUri = ContentUris.withAppendedId(People.CONTENT_URI, deletePersonID);
        		 	Log.d("delete contact", "uri: " + personUri.toString());

                    getApplicationContext().getContentResolver().delete(
                            ContentUris.withAppendedId(
                                    Contacts.People.CONTENT_URI,
                                    deletePersonID), null, null);
                    deletePersonID = -1;
                    setupTabs();
                       
                   }
               })
               .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) 
                   {
                        dialog.cancel();
                   }
               });
        AlertDialog confirmContactDelete = dcBuilder.create();
        return confirmContactDelete;
    }

    private Dialog createAddGroupDialog()
    {
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

                	getContentResolver().insert(Groups.CONTENT_URI, values);
                	setupTabs();
                }
            })
            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    // Placeholder.  Currently nothing to do when user hits cancel.
                }
            })
            .create();
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
		 	menu.add(0, DELETE_CONTACT, 0,  "Delete Contact");
		 	menu.add(0, ADD_GROUP_MEMBERSHIP, 0, "Add to Group");
		 	menu.add(0, REMOVE_GROUP_MEMBERSHIP, 0, "Remove from Group");
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
		 removeDialog(REMOVE_MEMBERSHIP_DIALOG);
		 showDialog(REMOVE_MEMBERSHIP_DIALOG);
	 }
	 
	 private void editContact(View v, long personId)
	 {
		 	Uri personUri = ContentUris.withAppendedId(People.CONTENT_URI, personId);
		 	Log.d("edit contact", "uri: " + personUri.toString());
		 	Intent myIntent = new Intent(Intent.ACTION_EDIT, personUri); 
	    	this.startActivityForResult(myIntent, EDIT_CONTACT_ACODE);
	 }
	 
	 
	 private long deletePersonID = -1;
	 private void deleteContact(View v,long personID)
	 {
		 	deletePersonID = personID;
		 	showDialog(DELETE_CONTACT_DIALOG);
	 }

	 private void addContact()
	 {
	 	Intent myIntent = new Intent(Contacts.Intents.Insert.ACTION, People.CONTENT_URI);
    	this.startActivityForResult(myIntent, GET_NEW_CONTACT_ACODE);
	 }
	 
	 final static int GET_NEW_CONTACT_ACODE = 1;
	 final static int EDIT_CONTACT_ACODE = 2;
	 @Override
	 public void onActivityResult(int reqCode, int resultCode, Intent data) 
	 {
	   super.onActivityResult(reqCode, resultCode, data);
	
	   switch (reqCode) 
	   {
	     case (GET_NEW_CONTACT_ACODE) :
		     try
		     {
		       if (resultCode == Activity.RESULT_OK) 
		       {
		    	 if(data == null)
		    	 {
		    		 break;
		    	 }
		         Uri contactData = data.getData();
		         Cursor c =  managedQuery(contactData, null, null, null, null);
		         if (c.moveToFirst()) 
		         {
		           long personID = c.getLong(c.getColumnIndexOrThrow(People._ID));
		           long groupID = getCurrentGroupID();
		           People.addToGroup(getContentResolver(), personID, groupID);
		         }
		       }
		     }
		     catch(Exception e)
		     {
		    	 String errMsg = e.getMessage();
		    	 if(errMsg == null)
		    	 {
		    		 errMsg = "";
		    	 }
		    	 Log.d("OnActivitResult", errMsg);
		     }
		     break;
	     case (EDIT_CONTACT_ACODE) :
	    	 break;
		 //nothing really needs doing here-  just update the tabs.
	   }
	   setupTabs(); 
	}
 
 
	public long getGroupMembershipID(long contactID, String groupName) 
	{
	    Cursor cur = null;
	    Context context = getApplicationContext();
	    long returnID = 0;
	    try {
	        cur = context.getContentResolver().query(
	                Contacts.GroupMembership.CONTENT_URI,
	                new String[] { Contacts.GroupMembership._ID },
	                new StringBuilder().append(Contacts.GroupMembership.NAME).append("=?")
	                .append(" AND ").append(Contacts.GroupMembership.PERSON_ID).append("=?")
	                .toString(),
	                new String[] { groupName, String.valueOf(contactID) },
	                Contacts.GroupMembership.DEFAULT_SORT_ORDER);
	        if (cur.moveToFirst())
	            returnID = cur.getLong(0);
	    } finally {
	        cur.close();
	    }
	    return returnID;
	}
	
	public void removeGroupMembershipByID(long groupMembershipID)
	{
		
        final int cnt = getApplicationContext().getContentResolver().delete(
                ContentUris.withAppendedId(
                        Contacts.GroupMembership.CONTENT_URI,
                        groupMembershipID), null, null);
        if (cnt != 0)
            Toast.makeText(getApplicationContext(), "Removed.", Toast.LENGTH_SHORT).show();
	}

	private long getCurrentGroupID() 
	{
		int tabIndex = getTabHost().getCurrentTab();
		String groupName = tabNames.get(tabIndex);
		
		Cursor groupCursor = getGroupCursorByName(groupName);
		groupCursor.moveToFirst();
		int groupIdIndex = groupCursor.getColumnIndex(Groups._ID);
		int systemGroupIndex = groupCursor.getColumnIndex(Contacts.GroupsColumns.SYSTEM_ID);
		if(!groupCursor.isNull(systemGroupIndex))
		{
			return -1;
		}
		
		long groupID = groupCursor.getLong(groupIdIndex);
		groupCursor.close();
		return groupID;
	}
    
}