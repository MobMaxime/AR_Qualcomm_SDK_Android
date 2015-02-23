package com.mobmaxime.vuforia_ar;


import android.app.Activity;

public class util {

	public static void right_left(Activity context) {
		context.overridePendingTransition(R.anim.slide_in_right,
				R.anim.slide_out_left);
	}

	public static void fade_in_out(Activity context) {
		context.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
	}

	public static void left_right(Activity context) {
		context.overridePendingTransition(R.anim.slide_in_left,
				R.anim.slide_out_right);
	}

}