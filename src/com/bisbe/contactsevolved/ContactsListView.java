package com.bisbe.contactsevolved;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.app.Activity;

public class ContactsListView extends ListView {

	
	private Activity currActivity;

	
	
	public ContactsListView(Context context) {
		super(context);
		currActivity = (Activity) context;
		this.setOnItemClickListener(contactClickListener);
		setItemsCanFocus(true);
		// TODO Auto-generated constructor stub
	}
	
	public ContactsListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		currActivity = (Activity) context;
		this.setOnItemClickListener(contactClickListener);
		setItemsCanFocus(true);
		// TODO Auto-generated constructor stub
	}

	public ContactsListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		currActivity = (Activity) context;
		this.setOnItemClickListener(contactClickListener);
		setItemsCanFocus(true);
		// TODO Auto-generated constructor stub
	}
	
	private OnItemClickListener contactClickListener = new OnItemClickListener()
	{
	    public void onItemClick(AdapterView<?> parent, View v, int position, long dbidentifier) 
	    {
	        // do something when the button is clicked
	    	
	    	Uri viewContactURI = Uri.parse("content://contacts/people/" + dbidentifier);
	    	Intent myIntent = new Intent(Intent.ACTION_VIEW, viewContactURI); 
	    	currActivity.startActivity(myIntent);
	    	
	    }
	};


}
