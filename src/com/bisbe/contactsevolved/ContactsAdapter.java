package com.bisbe.contactsevolved;

import java.util.HashMap;

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
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ContactsAdapter extends SimpleCursorAdapter 
{

	Activity context;
	public static HashMap<Integer, Bitmap> personPhotos;
	
	public ContactsAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to) 
	{
		super(context, layout, c, from, to);
		this.context = (Activity)context;
		if(personPhotos == null)
		{
			personPhotos = new HashMap<Integer, Bitmap>();
		}
	}

	public void loadPhotos(Cursor useCursor)
	{
		final Cursor c = useCursor;
    	Thread thread = new Thread() {
    		@Override
    		public void run() {
    			//TODO : set imageView to a "pending" image
    			if(c.moveToFirst())
    			{
    				do
    				{
						 int personIdIndex = c.getColumnIndex(Contacts.People._ID);
						 int personId = c.getInt(personIdIndex);
				 
						 Uri personUri = ContentUris.withAppendedId(
				                 Contacts.People.CONTENT_URI, personId);

    					 if(!personPhotos.containsKey(personId))
    					 {
    						 Bitmap photo = Contacts.People.loadContactPhoto(context, personUri, 0, null);
    						 personPhotos.put(personId,photo);
    					 }
    					
    				}
    				while(c.moveToNext());
    				c.close();
    			}
    		}
    	};
    	thread.start();
	}
	
	 class ViewHolder
	 {
		 ImageView contactPhotoView;
		 TextView nameLabel;
		 TextView phoneLabel;		 
		 ContactsContentLayout contentArea;
		 ImageView callButton;
	 }
	
	 @Override
	 public View getView(int position, View convertView, ViewGroup parent) 
	 {
		 Cursor c = getCursor();
		 c.moveToPosition(position);
		 
		 ViewHolder holder;
		 if (convertView == null) 
		 {
			 convertView = View.inflate(context, R.layout.contact_list_item, null);
			 holder = new ViewHolder();
			 holder.contactPhotoView = (ImageView) (convertView.findViewById(R.id.contactImage));
			 holder.nameLabel=(TextView)(convertView.findViewById(R.id.firstLine));
			 holder.phoneLabel=(TextView)(convertView.findViewById(R.id.secondLine));
	         holder.callButton = (ImageView) (convertView.findViewById(R.id.callButton));
	         holder.contentArea = (ContactsContentLayout) (convertView.findViewById(R.id.row_content));
			 convertView.setTag(holder);
		 } 
		 else 
		 {
			 holder = (ViewHolder) convertView.getTag();
		 }
		 

		 int personIdIndex = c.getColumnIndex(Contacts.People._ID);
		 int personId = c.getInt(personIdIndex);
 
		 Uri personUri = ContentUris.withAppendedId(
                 Contacts.People.CONTENT_URI, personId);

		 //Set Photo
		 
		 if(!personPhotos.containsKey(personId))
		 {
			 personPhotos.put(personId,Contacts.People.loadContactPhoto(context, personUri, 0, null));
		 }

	     Bitmap photo = personPhotos.get(personId);
	     holder.contactPhotoView.setImageBitmap(photo);
		 
		 //Set Name Label
         int nameIndex = c.getColumnIndex(Contacts.People.NAME);
         String name = c.getString(nameIndex);
         holder.nameLabel.setText(name); 

		 //Set Phone Number Label
		 int phoneIndex = c.getColumnIndex(Contacts.People.NUMBER);
         String phone = c.getString(phoneIndex);

         if(phone != null && phone.length() > 0)
         {
        	 holder.phoneLabel.setText(phone);
        	 holder.callButton.setVisibility(View.VISIBLE);
         }
         else
         {
        	 holder.phoneLabel.setText("");
        	 holder.callButton.setVisibility(View.INVISIBLE);
//        	 parent.removeView(holder.callButton);
        	 
         }

		 holder.contentArea.setClickable(true);
         holder.contentArea.setFocusable(true);
         holder.contentArea.setBackgroundResource(android.R.drawable.menuitem_background); 
         
         String personID = c.getString(c.getColumnIndex(Contacts.People._ID));
         AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) holder.contentArea.getContextMenuInfo();

         if(holder.contentArea.getContextMenuInfo() == null)
         {
        	 menuInfo = new AdapterView.AdapterContextMenuInfo(holder.contentArea, position, personId);
         }
         else
         {
        	 menuInfo.targetView = holder.contentArea;
        	 menuInfo.position = position;
        	 menuInfo.id = personId;
         }
         holder.contentArea.setContextMenuInfo(menuInfo);

         
         holder.contentArea.setTag(personID);
         context.registerForContextMenu(holder.contentArea);
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
    	
    	holder.contentArea.setOnClickListener(contactClickListener); 
    	
    	if(phone != null && phone.length() > 0)
    	{
    		
            holder.callButton.setImageResource(R.drawable.badge_action_call);
    		holder.callButton.setClickable(true);
            holder.callButton.setFocusable(true);
            
            holder.callButton.setBackgroundResource(android.R.drawable.menuitem_background);
            holder.callButton.setTag(phone);
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
	    	holder.callButton.setOnClickListener(callContactListener);
    	}
    	else
    	{
    			holder.callButton.setImageDrawable(null);
        		holder.callButton.setClickable(false);
                holder.callButton.setFocusable(false);
    	}
    	
        return(convertView);
	 }
	
	
}
