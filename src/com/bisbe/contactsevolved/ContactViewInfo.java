/**
 * 
 */
package com.bisbe.contactsevolved;

import android.content.ContentUris;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.Contacts;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

class ContactViewInfo
{
    ImageView contactPhotoView;
    TextView nameLabel;
    TextView phoneLabel;
    ImageView callButton;
    
    ContactsAdapter adapter;

    private int personID;
    private String phone;
    
    public ContactViewInfo(ContactsAdapter adapter) 
    {
        this.adapter = adapter;
    }
    
    public void setPersonID(int personID)
    {
        this.personID = personID;
        if (!adapter.personPhotos.containsKey(personID))
        {
            Uri personUri = ContentUris.withAppendedId(Contacts.People.CONTENT_URI, personID);
            adapter.personPhotos.put(personID, Contacts.People.loadContactPhoto(
                    adapter.context, personUri, 0, null));
        }

        Bitmap photo = adapter.personPhotos.get(personID);
        contactPhotoView.setImageBitmap(photo);        
    }

    public int getPersonID()
    {
        return personID;
    }

    public void setPhone(String phone) {
        this.phone = phone;
        if (phone != null && phone.length() > 0)
        {
            this.phoneLabel.setText(phone);
            callButton.setVisibility(View.VISIBLE);
            
            callButton.setImageResource(R.drawable.badge_action_call);
            
            callButton.setBackgroundResource(android.R.drawable.menuitem_background);
            callButton.setTag(phone);

            OnClickListener callContactListener = new OnClickListener()
            {
                public void onClick(View v) 
                {
                    Uri callContactURI = Uri.parse("tel:" + (String)v.getTag());
                    Intent myIntent = new Intent(Intent.ACTION_CALL, callContactURI);
                    v.getContext().startActivity(myIntent);
                }
            };

            callButton.setOnClickListener(callContactListener);
        }
        else
        {
            this.phoneLabel.setText("");
            this.callButton.setVisibility(View.INVISIBLE);
            this.callButton.setImageDrawable(null);
        }
    }

    public String getPhone() {
        return phone;
    }
    
    public Uri getContactViewUri() 
    {
        //FIXME Isn't it ContentUris.withAppendedId(Contacts.People.CONTENT_URI, personId); ?
        // Contacts.People.CONTENT_URI
        return Uri.parse("content://contacts/people/" + personID);
    }
}