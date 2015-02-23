package com.mobmaxime.vuforia_ar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;

public class SplashActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE); 
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			public void run() {
				Intent i = new Intent(getApplicationContext(),
						MainActivity.class);
				startActivity(i);
				util.fade_in_out(SplashActivity.this);
			}
		}, 3000);
	}
}
