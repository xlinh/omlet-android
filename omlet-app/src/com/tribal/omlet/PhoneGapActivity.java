/*
 * Copyright (c) 2012, TATRC and Tribal
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * * Neither the name of TATRC or TRIBAL nor the
 *   names of its contributors may be used to endorse or promote products
 *   derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL TATRC OR TRIBAL BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.tribal.omlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.tribal.mobile.Framework;
import com.tribal.mobile.activities.SherlockPhoneGapActivity;
import com.tribal.mobile.api.packages.PackageItem;
import com.tribal.mobile.base.BaseApplication;
import com.tribal.mobile.base.BroadcastActions;
import com.tribal.mobile.base.IntentParameterConstants;
import com.tribal.mobile.model.BaseContentItem;
import com.tribal.mobile.model.BookItem;
import com.tribal.mobile.model.HtmlItem;
import com.tribal.mobile.model.MenuItem;
import com.tribal.mobile.model.VideoItem;
import com.tribal.mobile.net.ConnectivityMode;
import com.tribal.mobile.phonegap.MFSettingsKeys;
import com.tribal.mobile.phonegap.MFStoreType;
import com.tribal.mobile.util.ConnectivityUtils;
import com.tribal.mobile.util.ContextUtils;
import com.tribal.mobile.util.DialogHelper;
import com.tribal.mobile.util.NativeFileHelper;
import com.tribal.mobile.util.NativeSettingsHelper;
import com.tribal.mobile.util.ViewHelper;

/**
 * Class that provides all the functionality for hosting web content and interacting with the native layer.
 * 
 * @author Jon Brasted
 */
public class PhoneGapActivity extends SherlockPhoneGapActivity {
	/* Fields */
	
	private TextView headerTextView = null;

	private android.view.MenuItem syncMenuItem = null;
	
	private boolean isSyncing = false;

	/* Properties */
	
	private void setIsSyncing(boolean value) {
		isSyncing = value;
		
		if (syncMenuItem != null) {
			syncMenuItem.setEnabled(!isSyncing);
		}
	}
	
	/* Methods */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Make action bar use up button by default
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	protected void onResume() {
		super.onResume();

		if (!isBroadcastReceiverActionRegistered(BroadcastActions.SyncStarted)) {
			addBroadcastReceiver(BroadcastActions.SyncStarted);
		}

		if (!isBroadcastReceiverActionRegistered(BroadcastActions.SyncProgressUpdate)) {
			addBroadcastReceiver(BroadcastActions.SyncProgressUpdate);
		}

		if (!isBroadcastReceiverActionRegistered(BroadcastActions.SyncCancelled)) {
			addBroadcastReceiver(BroadcastActions.SyncCancelled);
		}

		if (!isBroadcastReceiverActionRegistered(BroadcastActions.SyncCompleted)) {
			addBroadcastReceiver(BroadcastActions.SyncCompleted);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (isBroadcastReceiverActionRegistered(BroadcastActions.SyncStarted)) {
			removeBroadcastReceiver(BroadcastActions.SyncStarted);
		}

		if (isBroadcastReceiverActionRegistered(BroadcastActions.SyncProgressUpdate)) {
			removeBroadcastReceiver(BroadcastActions.SyncProgressUpdate);
		}

		if (isBroadcastReceiverActionRegistered(BroadcastActions.SyncCancelled)) {
			removeBroadcastReceiver(BroadcastActions.SyncCancelled);
		}

		if (isBroadcastReceiverActionRegistered(BroadcastActions.SyncCompleted)) {
			removeBroadcastReceiver(BroadcastActions.SyncCompleted);
		}
	}

	@Override
	protected void onBroadcastReceiveOverride(Intent intent) {
		String action = intent.getAction();

		if (BroadcastActions.SyncStarted.equalsIgnoreCase(action)) {
			// sync started
			onSyncStarted(intent);
		} else if (BroadcastActions.SyncProgressUpdate.equalsIgnoreCase(action)) {
			// sync progress update
			onSyncProgressUpdate();
		} else if (BroadcastActions.SyncCancelled.equalsIgnoreCase(action)) {
			// sync cancelled
			onSyncCancelled(intent);
		} else if (BroadcastActions.SyncCompleted.equalsIgnoreCase(action)) {
			// sync started
			onSyncCompleted(intent);
		} else {
			super.onBroadcastReceiveOverride(intent);
		}
	}

	@Override
	protected void loadView() {
		// remove the app view from root
		this.root.removeAllViews();

		// get a layout inflater
		LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

		// inflate the main phone gap layout into the root
		View mainContentView = layoutInflater.inflate(R.layout.phonegap_main_content, this.root);

		// find the content placeholder
		LinearLayout contentPlaceholder = (LinearLayout) mainContentView
				.findViewById(R.id.content_placeholder);

		// add the app view to the placeholder
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.FILL_PARENT);
		layoutParams.gravity = Gravity.FILL_HORIZONTAL | Gravity.FILL_VERTICAL;

		contentPlaceholder.addView(this.appView, layoutParams);

		// Set Header
		if (!TextUtils.isEmpty(packageName)) {
			getSupportActionBar().setTitle(packageName);
		}

		// enable scroll bars
		this.appView.setVerticalScrollBarEnabled(true);
		this.appView.setHorizontalScrollBarEnabled(true);
		
		// set scroll bar style
		this.appView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
	}

	@Override
	protected void onPackageNameUpdate() {
		if (headerTextView != null && !TextUtils.isEmpty(packageName)) {
			headerTextView.setText(packageName);
		}
	}

	@Override
	protected void onBeforePreLogout() {
		super.onBeforePreLogout();

		try {
			NativeSettingsHelper.getInstance(getApplicationContext()).removePreferenceValue(MFSettingsKeys.LAST_LOGGED_IN_USER);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void redirectToLogin() {
		Intent loginIntent = new Intent(this, LoginActivity.class);
		startActivity(loginIntent);
	}

	/**
	 * Navigate by menu item.
	 * 
	 * @param context			the context
	 * @param menuItem			the menu item
	 * @param parameters		the parameters
	 * @param intentFlags		the intent flags
	 * @param activityClass		the activity class
	 */
	protected void navigateByMenuItem(Context context, MenuItem menuItem, Map<String, Serializable> parameters, int[] intentFlags, Class<?> activityClass) {
		boolean isPhoneGapNavigate = false;

		if (activityClass == null) {

			// determine the activity class
			switch (menuItem.getType()) {
				case menu: {
					activityClass = ListMenuActivity.class;
					break;
				}
				case link: {
					BaseContentItem contentItem = menuItem.getLinkedContentItem();
					Class<? extends BaseContentItem> contentItemClass = contentItem.getClass();

					if (HtmlItem.class.equals(contentItemClass)) {
						activityClass = PhoneGapActivity.class;

						isPhoneGapNavigate = true;
						break;

					} else {
						if (BookItem.class.equals(contentItemClass) || VideoItem.class.equals(contentItemClass)) {
							navigateToNativeFileHandler(contentItem);
							return;
						}

						Log.e("PhoneGapActivity", "BaseContentItem type " + contentItem.getClass() + " is not currently supported in navigateByContentItem().");
						return;
					}
				}
				default: {
					Log.e("NavigationActivity",
							"menu item type "
									+ menuItem.getType()
									+ " is not currently supported in navigateByMenuItem().");
					return;
				}
			}
		}

		// create parameter map
		Map<String, Serializable> parameterMap;

		// check to see if the menu item has been supplied
		if (parameters == null) {
			parameterMap = new HashMap<String, Serializable>();
		} else {
			parameterMap = parameters;
		}

		// add menu item
		if (!parameterMap.containsKey(IntentParameterConstants.MenuItem)) {
			parameterMap.put(IntentParameterConstants.MenuItem, menuItem);
		}

		// add package name
		if (!parameterMap.containsKey(IntentParameterConstants.PackageName)) {
			parameterMap.put(IntentParameterConstants.PackageName, packageName);
		}

		if (isPhoneGapNavigate && !parameterMap.containsKey(IntentParameterConstants.MenuItem)) {
			parameterMap.put("loadUrlTimeoutValue", 60000);
		}

		// navigate
		navigate(context, activityClass, parameterMap, intentFlags);
	}

	/**
	 * Navigate by content item.
	 * 
	 * @param context			the context
	 * @param contentItem		the content item
	 */
	public void navigateByContentItem(Context context, BaseContentItem contentItem) {
		navigateByContentItem(context, contentItem, null);
	}

	/**
	 * Navigate by content item.
	 * 
	 * @param context			the context
	 * @param contentItem		the content item
	 * @param parameterMap		the parameters
	 */
	public void navigateByContentItem(Context context, BaseContentItem contentItem, Map<String, Serializable> parameterMap) {
		// check the content item
		Class<? extends BaseContentItem> contentItemClass = contentItem.getClass();

		if (BookItem.class.equals(contentItemClass) || VideoItem.class.equals(contentItemClass)) {
			// open book or video
			navigateToNativeFileHandler(contentItem);
		} else {
			// use standard navigation logic

			Class<?> activityClass = getContentItemActivityClass(contentItem);

			// check to see if the menu item has been supplied
			if (parameterMap == null) {
				parameterMap = new HashMap<String, Serializable>();
			}

			// add content item
			if (!parameterMap
					.containsKey(IntentParameterConstants.ResourceItem)) {
				parameterMap.put(IntentParameterConstants.ResourceItem,
						contentItem);
			}

			if (!parameterMap.containsKey("loadUrlTimeoutValue")) {
				parameterMap.put("loadUrlTimeoutValue", 60000);
			}
			
			// add package name
			if (!parameterMap.containsKey(IntentParameterConstants.PackageName)) {
				parameterMap.put(IntentParameterConstants.PackageName, packageName);
			}

			// navigate
			navigate(context, activityClass, parameterMap, null);
		}
	}

	private void navigateToNativeFileHandler(BaseContentItem baseContentItem) {
		String externalStorageFilePath = String.format(getString(R.string.external_storage_packages_path_format_string), baseContentItem.getPathWithPackage()); 
		externalStorageFilePath = Environment.getExternalStorageDirectory() + externalStorageFilePath;
		
		// create file
		File file = new File(externalStorageFilePath);

		if (NativeFileHelper.canOpenFile(this, file)) {
			// fire the intent to open the file
			Intent intent = NativeFileHelper.getOpenFileIntent(file);
			startActivity(intent);
		} else {
			// show alert message
			String title = getString(R.string.native_file_open_no_app_message_title);
			String messageFormatString = getString(R.string.native_file_open_no_app_message_message);

			// get the file extension
			String fileExtension = NativeFileHelper
					.getFileExtensionFromPath(file.getAbsolutePath());

			// get file extension without the period
			String fileExtensionWithoutPeriod = fileExtension.substring(1);

			// get formatted message
			String formattedMessage = String.format(messageFormatString, fileExtensionWithoutPeriod);

			DialogHelper.showAlertDialog(this, title, formattedMessage);
		}
	}

	private Class<?> getContentItemActivityClass(BaseContentItem contentItem) {
		// determine the activity class
		Class<?> activityClass = null;

		if (contentItem.getClass().equals(HtmlItem.class)) {
			activityClass = PhoneGapActivity.class;
		} else {
			// Log.e("NavigationActivity", "BaseContentItem type " +
			// contentItem.getClass() +
			// " is not currently supported in navigateByContentItem().");
		}

		return activityClass;
	}

	@Override
	protected void navigateToMenuItem(MenuItem menuItem) {
		navigateByMenuItem(getApplicationContext(), menuItem, null, null, null);
	}

	@Override
	protected void navigateToResourceItem(BaseContentItem resourceItem) {
		navigateByContentItem(getApplicationContext(), resourceItem);
	}

	@Override
	public void navigateToPackageDetails(PackageItem packageItem) {
		Map<String, Serializable> parameterMap = new HashMap<String, Serializable>();
		parameterMap.put(IntentParameterConstants.PackageItem, packageItem);
		navigate(this, PackageDetailActivity.class, parameterMap, null);
	}

	/**
	 * Taken from SystemMenuProviderActivity.
	 * 
	 * @return
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.system_menu, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// ensure that the Framework is still working
		if (Framework.getServer() == null || Framework.getClient() == null) {
			BaseApplication baseApplication = getBaseApplication();
				
			if (baseApplication != null) {
				baseApplication.resetApplication();
				return false;
			}
		}
		
		com.actionbarsherlock.view.MenuItem syncMenuItem = menu.findItem(R.id.menu_item_sync);

		if (Framework.getClient().getUserUsername() == null) {
			com.actionbarsherlock.view.MenuItem logoutMenuItem = menu
					.findItem(R.id.menu_item_logout);

			// disable log out option if the user is not logged in
			if (logoutMenuItem != null) {
				logoutMenuItem.setEnabled(false);
			}

			// disable sync option if the user is not logged in
			if (syncMenuItem != null) {
				syncMenuItem.setEnabled(false);
			}
		} else {
			if (syncMenuItem != null) {
				boolean syncMenuItemEnabled = false;

				if (!isSyncing) {
					// check the connection and whether the user can sync data
					String syncConnectivityModeString = Framework.getClient()
							.getValue(MFStoreType.GLOBAL,
									MFSettingsKeys.DATA_USE);
					
					ConnectivityMode connectivityMode = Enum.valueOf(ConnectivityMode.class, syncConnectivityModeString);

					if (connectivityMode != null) {
						boolean canConnect = ConnectivityUtils.canConnect(this,
								connectivityMode);
						syncMenuItemEnabled = canConnect;
					}
				}

				syncMenuItem.setEnabled(syncMenuItemEnabled);
			}
		}

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(
			com.actionbarsherlock.view.MenuItem item) {
		switch (item.getItemId()) {

			case android.R.id.home:
				// If the up button is pressed then for now just do a back
				// by calling finish, this may change when we have more
				// complicated hierarchies though
				finish();
				break;

			case R.id.menu_item_info: {
				showInfoDialog();
				break;
			}
			case R.id.menu_item_sync: {
				item.setEnabled(false);
				sync();
				break;
			}
			case R.id.menu_item_settings: {
				showSettings();
				break;
			}
			case R.id.menu_item_logout: {
				requestLogout();
				break;
			}
			default: {
				return super.onOptionsItemSelected(item);
			}
		}

		return true;
	}

	private void showInfoDialog() {
		// create view
		View view = LayoutInflater.from(this).inflate(R.layout.info_dialog, null);

		// set typeface on the view
		ViewHelper.setTypeFace(view);

		// create dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(view);
		final AlertDialog dialog = builder.show();

		// populate the version number
		TextView buildVersionTextView = (TextView) dialog.findViewById(R.id.info_dialog_build_version_number);

		if (buildVersionTextView != null) {
			// get the build version
			String versionName = ContextUtils.getVersionName(getApplicationContext());

			String buildVersionLongFormatString = getString(R.string.buildVersionLongFormatString);
			String buildVersionLong = String.format(buildVersionLongFormatString, versionName);

			buildVersionTextView.setText(buildVersionLong);
		}

		// attach on click handler to the close button
		Button closeButton = (Button) dialog
				.findViewById(R.id.info_dialog_close);

		if (closeButton != null) {
			closeButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					dialog.dismiss();
				}
			});
		}
	}

	private void showSettings() {
		// When the menu option is selected, launch an activity through this
		// intent
		Intent launchOptionsIntent = new Intent(this, SettingsActivity.class);

		startActivity(launchOptionsIntent);
	}

	private void sync() {
		Framework.getClient().sync();
	}

	private void onSyncStarted(Intent intent) {
		// set isSyncing
		setIsSyncing(true);

		if (intent.hasExtra(IntentParameterConstants.ShowToast) && 
			intent.getBooleanExtra(IntentParameterConstants.ShowToast, false)) {		
			
			// create a toast
			Toast toast = Toast.makeText(getApplicationContext(), R.string.sync_started_string, Toast.LENGTH_SHORT);
			toast.show();
		}
	}

	private void onSyncProgressUpdate() {
		if (!isSyncing) {
			setIsSyncing(true);
		}
	}

	private void onSyncCancelled(Intent intent) {
		// set isSyncing
		setIsSyncing(false);

		if (intent.hasExtra(IntentParameterConstants.ShowToast) && 
			intent.getBooleanExtra(IntentParameterConstants.ShowToast, false)) {
			
			// create a toast
			Toast toast = Toast.makeText(getApplicationContext(), R.string.sync_cancelled_string, Toast.LENGTH_SHORT);
			toast.show();
		}
				
		// close the notification
		// get notification manager
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		// cancel the notification
		notificationManager.cancel(R.layout.notification_template);
	}

	private void onSyncCompleted(Intent intent) {
		// set isSyncing
		setIsSyncing(false);

		if (intent.hasExtra(IntentParameterConstants.ShowToast) && 
			intent.getBooleanExtra(IntentParameterConstants.ShowToast, false)) {
			
			// create a toast
			Toast toast = Toast.makeText(getApplicationContext(), R.string.sync_complete_string, Toast.LENGTH_SHORT);
			toast.show();
		}
				
		// close the notification
		// get notification manager
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		// cancel the notification
		notificationManager.cancel(R.layout.notification_template);
	}
}