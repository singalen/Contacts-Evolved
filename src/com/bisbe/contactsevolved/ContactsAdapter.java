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
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ContactsAdapter extends SimpleCursorAdapter implements
        View.OnClickListener
{

    Activity context;
    public static HashMap<Integer, Bitmap> personPhotos;

    public ContactsAdapter(Context context, int layout, Cursor c,
            String[] from, int[] to)
    {
        super(context, layout, c, from, to);
        this.context = (Activity) context;
        if (personPhotos == null)
        {
            personPhotos = new HashMap<Integer, Bitmap>();
        }
    }

    public void loadPhotos(Cursor useCursor)
    {
        final Cursor c = useCursor;
        Thread thread = new Thread() {
            @Override
            public void run()
            {
                // TODO : set imageView to a "pending" image
                if (c.moveToFirst())
                {
                    do
                    {
                        int personIdIndex = c.getColumnIndex(Contacts.People._ID);
                        int personId = c.getInt(personIdIndex);

                        Uri personUri = ContentUris.withAppendedId(
                                Contacts.People.CONTENT_URI, personId);

                        if (!personPhotos.containsKey(personId))
                        {
                            Bitmap photo = Contacts.People.loadContactPhoto(context, personUri, 0, null);
                            personPhotos.put(personId, photo);
                        }

                    } while (c.moveToNext());
                }
                c.close();
            }
        };
        thread.start();
    }

    public void onClick(View v)
    {
        ContactViewInfo contact = (ContactViewInfo) v.getTag();
        Intent myIntent = new Intent(Intent.ACTION_VIEW, contact.getContactViewUri());
        v.getContext().startActivity(myIntent);
    }

    private View assignConvertViewContact(View convertView)
    {
        ContactViewInfo contact;
        if (convertView == null)
        {
            convertView = View.inflate(context, R.layout.contact_list_item, null);
            contact = new ContactViewInfo(this);
            contact.contactPhotoView = (ImageView) (convertView.findViewById(R.id.contactImage));
            contact.nameLabel = (TextView) (convertView.findViewById(R.id.firstLine));
            contact.phoneLabel = (TextView) (convertView.findViewById(R.id.secondLine));
            contact.callButton = (ImageView) (convertView.findViewById(R.id.callButton));
            convertView.setTag(contact);
        }
        else
        {
            contact = (ContactViewInfo) convertView.getTag();
        }

        return convertView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        Cursor contactsCursor = getCursor();
        contactsCursor.moveToPosition(position);

        convertView = assignConvertViewContact(convertView);
        ContactViewInfo contact = (ContactViewInfo) convertView.getTag();

        int personIdIndex = contactsCursor.getColumnIndex(Contacts.People._ID);
        int personId = contactsCursor.getInt(personIdIndex);
        contact.setPersonID(personId);

        int nameIndex = contactsCursor.getColumnIndex(Contacts.People.NAME);
        String name = contactsCursor.getString(nameIndex);
        contact.setName(name);

        int phoneIndex = contactsCursor.getColumnIndex(Contacts.People.NUMBER);
        String phone = contactsCursor.getString(phoneIndex);
        contact.setPhone(phone);

        convertView.setBackgroundResource(android.R.drawable.menuitem_background);

        AdapterView.AdapterContextMenuInfo menuInfo = 
            (AdapterView.AdapterContextMenuInfo) ((ContactsContentLayout) convertView).getContextMenuInfo();

        if (menuInfo == null)
        {
            menuInfo = new AdapterView.AdapterContextMenuInfo(convertView, position, personId);
        }
        else
        {
            menuInfo.targetView = convertView;
            menuInfo.position = position;
            menuInfo.id = personId;
        }
        // TODO: If we already have contact saved in convertView, we probably don't have to save menu to it.
        // Just recreate it there.
        ((ContactsContentLayout) convertView).setContextMenuInfo(menuInfo);

        return convertView;
    }
}
