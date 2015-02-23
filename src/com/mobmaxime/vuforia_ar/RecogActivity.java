package com.mobmaxime.vuforia_ar;

import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.mobmaxime.application.SampleApplicationControl;
import com.mobmaxime.application.SampleApplicationException;
import com.mobmaxime.application.SampleApplicationGLView;
import com.mobmaxime.application.SampleApplicationSession;
import com.mobmaxime.application.Texture;
import com.mobmaxime.common.LoadingDialogHandler;
import com.qualcomm.vuforia.CameraDevice;
import com.qualcomm.vuforia.ImageTracker;
import com.qualcomm.vuforia.State;
import com.qualcomm.vuforia.TargetFinder;
import com.qualcomm.vuforia.TargetSearchResult;
import com.qualcomm.vuforia.Trackable;
import com.qualcomm.vuforia.Tracker;
import com.qualcomm.vuforia.TrackerManager;
import com.qualcomm.vuforia.Vuforia;

public class RecogActivity extends Activity implements SampleApplicationControl {

	private static final String LOGTAG = "Cloud_Recog";
	SampleApplicationSession vuforiaAppSession;

	private int mlastErrorCode = 0;
	private int mInitErrorCode = 0;
	private double mLastErrorTime;
	private boolean mFinishActivityOnError;

	private RelativeLayout mUILayout;
	private boolean mContAutofocus = false;
	// Alert Dialog used to display SDK errors
	private AlertDialog mErrorDialog;
	private GestureDetector mGestureDetector;
	private Vector<Texture> mTextures;
	boolean mIsDroidDevice = false;
	private SampleApplicationGLView mGlView;
	private View mFlashOptionView;
	private boolean mFlash = false;
	boolean mFinderStarted = false;
	private static final String kAccessKey = "2268decb6b9575a15075607c4ae204d0ec8dade5";
	private static final String kSecretKey = "dcbe8c2e70da145cbcb0a3a8e0f320940d87e03f";
	private CloudRecoRenderer mRenderer;

	// These codes match the ones defined in TargetFinder in Vuforia.jar
	static final int INIT_SUCCESS = 2;
	static final int INIT_ERROR_NO_NETWORK_CONNECTION = -1;
	static final int INIT_ERROR_SERVICE_NOT_AVAILABLE = -2;
	static final int UPDATE_ERROR_AUTHORIZATION_FAILED = -1;
	static final int UPDATE_ERROR_PROJECT_SUSPENDED = -2;
	static final int UPDATE_ERROR_NO_NETWORK_CONNECTION = -3;
	static final int UPDATE_ERROR_SERVICE_NOT_AVAILABLE = -4;
	static final int UPDATE_ERROR_BAD_FRAME_QUALITY = -5;
	static final int UPDATE_ERROR_UPDATE_SDK = -6;
	static final int UPDATE_ERROR_TIMESTAMP_OUT_OF_RANGE = -7;
	static final int UPDATE_ERROR_REQUEST_TIMEOUT = -8;
	static final int HIDE_LOADING_DIALOG = 0;
	private boolean mExtendedTracking = false;

	private LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(
			this);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		vuforiaAppSession = new SampleApplicationSession(this);
		startLoadingAnimation();
		vuforiaAppSession
				.initAR(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		// mGestureDetector = new GestureDetector(this, new GestureListener());

		mTextures = new Vector<Texture>();
		loadTextures();

		// getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		// getActionBar().setCustomView(R.layout.actionbar_recog);

		mIsDroidDevice = android.os.Build.MODEL.toLowerCase().startsWith(
				"droid");
	}

	private void startLoadingAnimation() {
		// Inflates the Overlay Layout to be displayed above the Camera View
		LayoutInflater inflater = LayoutInflater.from(this);
		mUILayout = (RelativeLayout) inflater.inflate(R.layout.camera_overlay,
				null, false);

		mUILayout.setVisibility(View.VISIBLE);
		mUILayout.setBackgroundColor(Color.BLACK);

		// By default
		loadingDialogHandler.mLoadingDialogContainer = mUILayout
				.findViewById(R.id.loading_indicator);
		loadingDialogHandler.mLoadingDialogContainer
				.setVisibility(View.VISIBLE);

		addContentView(mUILayout, new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));
		getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		getActionBar().setCustomView(R.layout.actionbar_recog);
		getActionBar().getCustomView().setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						// Do stuff here.
						onBackPressed();
					}
				});

	}

	private void loadTextures() {
		mTextures.add(Texture.loadTextureFromApk("TextureTeapotRed.png",
				getAssets()));
	}

	// Called when the activity will start interacting with the user.
	@Override
	protected void onResume() {
		Log.d(LOGTAG, "onResume");
		super.onResume();

		// This is needed for some Droid devices to force portrait
		if (mIsDroidDevice) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}

		try {
			vuforiaAppSession.resumeAR();
		} catch (SampleApplicationException e) {
			Log.e(LOGTAG, e.getString());
		}

		// Resume the GL view:
		if (mGlView != null) {
			mGlView.setVisibility(View.VISIBLE);
			mGlView.onResume();
		}

	}

	// Callback for configuration changes the activity handles itself
	@Override
	public void onConfigurationChanged(Configuration config) {
		Log.d(LOGTAG, "onConfigurationChanged");
		super.onConfigurationChanged(config);

		vuforiaAppSession.onConfigurationChanged();
	}

	// Called when the system is about to start resuming a previous activity.
	@Override
	protected void onPause() {
		Log.d(LOGTAG, "onPause");
		super.onPause();

		// Turn off the flash
		if (mFlashOptionView != null && mFlash) {
			// OnCheckedChangeListener is called upon changing the checked state
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				((Switch) mFlashOptionView).setChecked(false);
			} else {
				((CheckBox) mFlashOptionView).setChecked(false);
			}
		}

		try {
			vuforiaAppSession.pauseAR();
		} catch (SampleApplicationException e) {
			Log.e(LOGTAG, e.getString());
		}

		// Pauses the OpenGLView
		if (mGlView != null) {
			mGlView.setVisibility(View.INVISIBLE);
			mGlView.onPause();
		}
	}

	// The final call you receive before your activity is destroyed.
	@Override
	protected void onDestroy() {
		Log.d(LOGTAG, "onDestroy");
		super.onDestroy();

		try {
			vuforiaAppSession.stopAR();
		} catch (SampleApplicationException e) {
			Log.e(LOGTAG, e.getString());
		}

		System.gc();
	}

	// Initializes AR application components.
	private void initApplicationAR() {
		// Create OpenGL ES view:
		int depthSize = 16;
		int stencilSize = 0;
		boolean translucent = Vuforia.requiresAlpha();

		// Initialize the GLView with proper flags
		mGlView = new SampleApplicationGLView(this);
		mGlView.init(translucent, depthSize, stencilSize);

		// Setups the Renderer of the GLView
		mRenderer = new CloudRecoRenderer(vuforiaAppSession, this);
		mRenderer.setTextures(mTextures);
		mGlView.setRenderer(mRenderer);

	}

	// Returns the error message for each error code
	private String getStatusDescString(int code) {
		if (code == UPDATE_ERROR_AUTHORIZATION_FAILED)
			return getString(R.string.UPDATE_ERROR_AUTHORIZATION_FAILED_DESC);
		if (code == UPDATE_ERROR_PROJECT_SUSPENDED)
			return getString(R.string.UPDATE_ERROR_PROJECT_SUSPENDED_DESC);
		if (code == UPDATE_ERROR_NO_NETWORK_CONNECTION)
			return getString(R.string.UPDATE_ERROR_NO_NETWORK_CONNECTION_DESC);
		if (code == UPDATE_ERROR_SERVICE_NOT_AVAILABLE)
			return getString(R.string.UPDATE_ERROR_SERVICE_NOT_AVAILABLE_DESC);
		if (code == UPDATE_ERROR_UPDATE_SDK)
			return getString(R.string.UPDATE_ERROR_UPDATE_SDK_DESC);
		if (code == UPDATE_ERROR_TIMESTAMP_OUT_OF_RANGE)
			return getString(R.string.UPDATE_ERROR_TIMESTAMP_OUT_OF_RANGE_DESC);
		if (code == UPDATE_ERROR_REQUEST_TIMEOUT)
			return getString(R.string.UPDATE_ERROR_REQUEST_TIMEOUT_DESC);
		if (code == UPDATE_ERROR_BAD_FRAME_QUALITY)
			return getString(R.string.UPDATE_ERROR_BAD_FRAME_QUALITY_DESC);
		else {
			return getString(R.string.UPDATE_ERROR_UNKNOWN_DESC);
		}
	}

	// Returns the error message for each error code
	private String getStatusTitleString(int code) {
		if (code == UPDATE_ERROR_AUTHORIZATION_FAILED)
			return getString(R.string.UPDATE_ERROR_AUTHORIZATION_FAILED_TITLE);
		if (code == UPDATE_ERROR_PROJECT_SUSPENDED)
			return getString(R.string.UPDATE_ERROR_PROJECT_SUSPENDED_TITLE);
		if (code == UPDATE_ERROR_NO_NETWORK_CONNECTION)
			return getString(R.string.UPDATE_ERROR_NO_NETWORK_CONNECTION_TITLE);
		if (code == UPDATE_ERROR_SERVICE_NOT_AVAILABLE)
			return getString(R.string.UPDATE_ERROR_SERVICE_NOT_AVAILABLE_TITLE);
		if (code == UPDATE_ERROR_UPDATE_SDK)
			return getString(R.string.UPDATE_ERROR_UPDATE_SDK_TITLE);
		if (code == UPDATE_ERROR_TIMESTAMP_OUT_OF_RANGE)
			return getString(R.string.UPDATE_ERROR_TIMESTAMP_OUT_OF_RANGE_TITLE);
		if (code == UPDATE_ERROR_REQUEST_TIMEOUT)
			return getString(R.string.UPDATE_ERROR_REQUEST_TIMEOUT_TITLE);
		if (code == UPDATE_ERROR_BAD_FRAME_QUALITY)
			return getString(R.string.UPDATE_ERROR_BAD_FRAME_QUALITY_TITLE);
		else {
			return getString(R.string.UPDATE_ERROR_UNKNOWN_TITLE);
		}
	}

	public void startFinderIfStopped() {
		if (!mFinderStarted) {
			mFinderStarted = true;

			// Get the image tracker:
			TrackerManager trackerManager = TrackerManager.getInstance();
			ImageTracker imageTracker = (ImageTracker) trackerManager
					.getTracker(ImageTracker.getClassType());

			// Initialize target finder:
			TargetFinder targetFinder = imageTracker.getTargetFinder();

			targetFinder.clearTrackables();
			targetFinder.startRecognition();
		}
	}

	public void stopFinderIfStarted() {
		if (mFinderStarted) {
			mFinderStarted = false;

			// Get the image tracker:
			TrackerManager trackerManager = TrackerManager.getInstance();
			ImageTracker imageTracker = (ImageTracker) trackerManager
					.getTracker(ImageTracker.getClassType());

			// Initialize target finder:
			TargetFinder targetFinder = imageTracker.getTargetFinder();

			targetFinder.stop();
		}
	}

	// Shows error messages as System dialogs
	public void showErrorMessage(int errorCode, double errorTime,
			boolean finishActivityOnError) {
		if (errorTime < (mLastErrorTime + 5.0) || errorCode == mlastErrorCode)
			return;

		mlastErrorCode = errorCode;
		mFinishActivityOnError = finishActivityOnError;

		runOnUiThread(new Runnable() {
			public void run() {
				if (mErrorDialog != null) {
					mErrorDialog.dismiss();
				}

				// Generates an Alert Dialog to show the error message
				AlertDialog.Builder builder = new AlertDialog.Builder(
						RecogActivity.this);
				builder.setMessage(
						getStatusDescString(RecogActivity.this.mlastErrorCode))
						.setTitle(
								getStatusTitleString(RecogActivity.this.mlastErrorCode))
						.setCancelable(false)
						.setIcon(0)
						.setPositiveButton(getString(R.string.button_OK),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										if (mFinishActivityOnError) {
											finish();
										} else {
											dialog.dismiss();
										}
									}
								});

				mErrorDialog = builder.create();
				mErrorDialog.show();
			}
		});
	}

	@Override
	public boolean doInitTrackers() {
		TrackerManager tManager = TrackerManager.getInstance();
		Tracker tracker;

		// Indicate if the trackers were initialized correctly
		boolean result = true;

		tracker = tManager.initTracker(ImageTracker.getClassType());
		if (tracker == null) {
			Log.e(LOGTAG,
					"Tracker not initialized. Tracker already initialized or the camera is already started");
			result = false;
		} else {
			Log.i(LOGTAG, "Tracker successfully initialized");
		}

		return result;
	}

	@Override
	public boolean doLoadTrackersData() {
		// TODO Auto-generated method stub
		Log.d(LOGTAG, "initCloudReco");

		// Get the image tracker:
		TrackerManager trackerManager = TrackerManager.getInstance();
		ImageTracker imageTracker = (ImageTracker) trackerManager
				.getTracker(ImageTracker.getClassType());

		// Initialize target finder:
		TargetFinder targetFinder = imageTracker.getTargetFinder();

		// Start initialization:
		if (targetFinder.startInit(kAccessKey, kSecretKey)) {
			targetFinder.waitUntilInitFinished();
		}

		int resultCode = targetFinder.getInitState();
		if (resultCode != TargetFinder.INIT_SUCCESS) {
			if (resultCode == TargetFinder.INIT_ERROR_NO_NETWORK_CONNECTION) {
				mInitErrorCode = UPDATE_ERROR_NO_NETWORK_CONNECTION;
			} else {
				mInitErrorCode = UPDATE_ERROR_SERVICE_NOT_AVAILABLE;
			}

			Log.e(LOGTAG, "Failed to initialize target finder.");
			return false;
		}

		// Use the following calls if you would like to customize the color of
		// the UI
		// targetFinder->setUIScanlineColor(1.0, 0.0, 0.0);
		// targetFinder->setUIPointColor(0.0, 0.0, 1.0);

		return true;
	}

	@Override
	public boolean doStartTrackers() {
		// TODO Auto-generated method stub
		// Indicate if the trackers were started correctly
		boolean result = true;

		// Start the tracker:
		TrackerManager trackerManager = TrackerManager.getInstance();
		ImageTracker imageTracker = (ImageTracker) trackerManager
				.getTracker(ImageTracker.getClassType());
		imageTracker.start();

		// Start cloud based recognition if we are in scanning mode:
		TargetFinder targetFinder = imageTracker.getTargetFinder();
		targetFinder.startRecognition();
		mFinderStarted = true;

		return result;
	}

	@Override
	public boolean doStopTrackers() {
		// TODO Auto-generated method stub
		// Indicate if the trackers were stopped correctly
		boolean result = true;

		TrackerManager trackerManager = TrackerManager.getInstance();
		ImageTracker imageTracker = (ImageTracker) trackerManager
				.getTracker(ImageTracker.getClassType());

		if (imageTracker != null) {
			imageTracker.stop();

			// Stop cloud based recognition:
			TargetFinder targetFinder = imageTracker.getTargetFinder();
			targetFinder.stop();
			mFinderStarted = false;

			// Clears the trackables
			targetFinder.clearTrackables();
		} else {
			result = false;
		}

		return result;
	}

	@Override
	public boolean doUnloadTrackersData() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean doDeinitTrackers() {
		// TODO Auto-generated method stub
		// Indicate if the trackers were deinitialized correctly
		boolean result = true;

		TrackerManager tManager = TrackerManager.getInstance();
		tManager.deinitTracker(ImageTracker.getClassType());

		return result;
	}

	@Override
	public void onInitARDone(SampleApplicationException exception) {
		// TODO Auto-generated method stub
		if (exception == null) {
			initApplicationAR();

			// Now add the GL surface view. It is important
			// that the OpenGL ES surface view gets added
			// BEFORE the camera is started and video
			// background is configured.
			addContentView(mGlView, new LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.MATCH_PARENT));

			// Start the camera:
			try {
				vuforiaAppSession.startAR(CameraDevice.CAMERA.CAMERA_DEFAULT);
			} catch (SampleApplicationException e) {
				Log.e(LOGTAG, e.getString());
			}

			boolean result = CameraDevice.getInstance().setFocusMode(
					CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);

			if (result)
				mContAutofocus = true;
			else
				Log.e(LOGTAG, "Unable to enable continuous autofocus");

			mUILayout.bringToFront();

			// Hides the Loading Dialog
			loadingDialogHandler.sendEmptyMessage(HIDE_LOADING_DIALOG);

			mUILayout.setBackgroundColor(Color.TRANSPARENT);

			// mSampleAppMenu = new SampleAppMenu(this, this, "Cloud Reco",
			// mGlView, mUILayout, null);
			// setSampleAppMenuSettings();

		} else {
			Log.e(LOGTAG, exception.getString());
			if (mInitErrorCode != 0) {
				showErrorMessage(mInitErrorCode, 10, true);
			} else {
				finish();
			}
		}
	}

	@Override
	public void onQCARUpdate(State state) {
		// TODO Auto-generated method stub
		// Get the tracker manager:
		TrackerManager trackerManager = TrackerManager.getInstance();

		// Get the image tracker:
		ImageTracker imageTracker = (ImageTracker) trackerManager
				.getTracker(ImageTracker.getClassType());

		// Get the target finder:
		TargetFinder finder = imageTracker.getTargetFinder();

		// Check if there are new results available:
		final int statusCode = finder.updateSearchResults();
		// final int Code_num = finder.ge
		// string uniqueID = targetSearchResult.UniqueTargetId;

		// Show a message if we encountered an error:
		if (statusCode < 0) {

			boolean closeAppAfterError = (statusCode == UPDATE_ERROR_NO_NETWORK_CONNECTION || statusCode == UPDATE_ERROR_SERVICE_NOT_AVAILABLE);

			showErrorMessage(statusCode, state.getFrame().getTimeStamp(),
					closeAppAfterError);

		} else if (statusCode == TargetFinder.UPDATE_RESULTS_AVAILABLE) {
			// Process new search results
			// showToast("Got Something" + Code_num);
			if (finder.getResultCount() > 0) {
				TargetSearchResult result = finder.getResult(0);

				try {
					JSONArray jarray = new JSONArray(result.getMetaData());
					JSONObject jobj = jarray.getJSONObject(0);

					//showToast("Got Something --->" + );
					
					
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(jobj.getString("weburl")));
					startActivity(browserIntent);
					//onBackPressed();

				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log.d("Message", e.getMessage());
				}
				// Check if this target is suitable for tracking:
				if (result.getTrackingRating() > 0) {
					Trackable trackable = finder.enableTracking(result);

					// showToast(trackable.getUserData() + "");
					// Log.d("UserData", trackable.getUserData() + "");
					// Log.d("trackable", trackable.toString()+"");
					// Log.d("Type", trackable.getType() + "");

					if (mExtendedTracking)
						trackable.startExtendedTracking();
				}
			}
		}

	}

	private void showToast(String text) {
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
		util.left_right(this);
		try {
			vuforiaAppSession.stopAR();
		} catch (SampleApplicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finish();

	}
}