package com.bisbe.contacts;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class GroupPreferences extends PreferenceActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.addPreferencesFromResource(R.xml.visible_group_preferences);

	}
}
