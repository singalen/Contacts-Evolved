package com.bisbe.contacts;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.Contacts;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ContactsAdapter extends SimpleCursorAdapter {

	Activity context;
	public ContactsAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to) {
		super(context, layout, c, from, to);
		this.context = (Activity)context;
		// TODO Auto-generated constructor stub
	}

	
	 public View getView(int position, View convertView, ViewGroup parent) 
	 {
		 Cursor c = getCursor();
		 c.moveToPosition(position);
		 View row=View.inflate(context, R.layout.contact_list_item, null);

		 int personIdIndex = c.getColumnIndex(Contacts.People._ID);
		 int personId = c.getInt(personIdIndex);
		 ImageView contactPhotoView = (ImageView) (row.findViewById(R.id.contactImage));
 
		 Uri personUri = ContentUris.withAppendedId(
                 Contacts.People.CONTENT_URI, personId);
 
	     Bitmap photo = Contacts.People.loadContactPhoto(context, personUri, 0, null);
	     contactPhotoView.setImageBitmap(photo);
		 
		 TextView nameLabel=(TextView)(row.findViewById(R.id.firstLine));
		 TextView phoneLabel=(TextView)(row.findViewById(R.id.secondLine));
		 
         int nameIndex = c.getColumnIndex(Contacts.People.NAME);
         String name = c.getString(nameIndex);
         nameLabel.setText(name); 

		 
		 int phoneIndex = c.getColumnIndex(Contacts.People.NUMBER);
         String phone = c.getString(phoneIndex);
         ImageView callButton = (ImageView) (row.findViewById(R.id.callButton));

         if(phone != null || phone.length() > 0)
         {
        	 phoneLabel.setText(phone); 
         }
         else
         {
        	 phoneLabel.setText("");
        	 callButton.setVisibility(View.INVISIBLE);
        	 parent.removeView(callButton);
        	 
         }

         
         
  
		 ContactsContentLayout contentArea = (ContactsContentLayout) (row.findViewById(R.id.row_content));

		 contentArea.setClickable(true);
         contentArea.setFocusable(true);
         contentArea.setBackgroundResource(android.R.drawable.menuitem_background); 
         
         
         String personID = c.getString(c.getColumnIndex(Contacts.People._ID));

         AdapterView.AdapterContextMenuInfo menuInfo = new AdapterView.AdapterContextMenuInfo(contentArea, position, personId);
         contentArea.setContextMenuInfo(menuInfo);

         
         contentArea.setTag(personID);
         context.registerForContextMenu(contentArea);
     	 OnClickListener contactClickListener = new OnClickListener()
    	 {
    	    public void onClick(View v) 
    	    {
    	        // do something when the button is clicked
    	    	String pid = (String)(v.getTag());
    	    	Uri viewContactURI = Uri.parse("content://contacts/people/" + pid);
    	    	Intent myIntent = new Intent(Intent.ACTION_VIEW, viewContactURI); 

    	    	v.getContext().startActivity(myIntent);
    	    	
    	    }
    	};
    	
    	contentArea.setOnClickListener(contactClickListener);
         
    	
    	if(phone != null && phone.length() > 0)
    	{
    		
            callButton.setClickable(true);
            callButton.setFocusable(true);
            callButton.setImageResource(R.drawable.badge_action_call);
            callButton.setBackgroundResource(android.R.drawable.menuitem_background);
    		callButton.setTag(phone);
	    	OnClickListener callContactListener = new OnClickListener()
	    	{
	    	    public void onClick(View v) 
	    	    {
	    	        // do something when the button is clicked

	    	    	Uri callContactURI = Uri.parse("tel:" + (String)v.getTag());
	    	    	Intent myIntent = new Intent(Intent.ACTION_CALL, callContactURI); 
	
	    	    	v.getContext().startActivity(myIntent);
	    	    	
	    	    }
	    	};
	    	callButton.setOnClickListener(callContactListener);
    	}
    	
        return(row);
	 }
	
	
}
