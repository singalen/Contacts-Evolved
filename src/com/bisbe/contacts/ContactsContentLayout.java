package com.bisbe.contacts;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.RelativeLayout;
import android.view.ContextMenu.ContextMenuInfo;
public class ContactsContentLayout extends RelativeLayout {

	private ContextMenuInfo mContextMenuInfo;
	
	
	public ContactsContentLayout(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public ContactsContentLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public ContactsContentLayout(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public ContextMenuInfo getContextMenuInfo()
	{
		return mContextMenuInfo;
	}
	
	public void setContextMenuInfo(ContextMenuInfo menuInfo)
	{
		mContextMenuInfo = menuInfo;
	}

}
