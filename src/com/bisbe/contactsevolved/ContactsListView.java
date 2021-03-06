package com.bisbe.contactsevolved;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;
import android.app.Activity;

public class ContactsListView extends ListView  
{
	private Activity currActivity;
	
	public ContactsListView(Context context) 
	{
		super(context);
		currActivity = (Activity) context;
	}
	
	public ContactsListView(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		currActivity = (Activity) context;
	}

	public ContactsListView(Context context, AttributeSet attrs, int defStyle) 
	{
		super(context, attrs, defStyle);
		currActivity = (Activity) context;
	}
}
