package com.mobmaxime.vuforia_ar;

import org.json.JSONArray;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		getActionBar().setCustomView(R.layout.actionbar_activity);
		setContentView(R.layout.activity_main);
	}

	public void scan_click(View v) {
		Intent intent = new Intent(this, RecogActivity.class);
		intent.putExtra("ABOUT_TEXT", "CloudReco/CR_about.html");
		startActivity(intent);
		util.right_left(this);
	}

	public void exit_click(View v) {
		finish();
	}

}
