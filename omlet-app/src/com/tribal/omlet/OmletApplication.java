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

import com.tribal.omlet.util.database.OmletDatabaseHelper;

import android.content.Intent;
import android.os.Environment;

import com.tribal.mobile.base.BaseApplication;
import com.tribal.mobile.base.ClearApplicationDataCompleted;
import com.tribal.mobile.util.DeleteDirectoryCompleted;
import com.tribal.mobile.util.FileHelper;
import com.tribal.mobile.util.database.BaseDatabaseHelper;
import com.tribal.mobile.util.database.DatabaseHelper;

/**
 * Application class.
 * 
 * @author Jon Brasted
 */
public class OmletApplication extends BaseApplication implements DeleteDirectoryCompleted {
	/* Constructor */
	
	public OmletApplication() {
		System.setProperty("http.keepAlive", "false");
	}
	
	/* Methods */
	
	@Override
	protected BaseDatabaseHelper createDatabaseHelper() {
		return new OmletDatabaseHelper(this, DatabaseHelper.DEFAULT_DB_NAME);
	}
	
	@Override
	protected void onClearingApplicationDataOverride(ClearApplicationDataCompleted callback) {
		// clear all the packages as well
		String externalStoragePackagesPath = String.format(getString(R.string.external_storage_packages_path_format_string), ""); 
		String externalStorageCoursesDirectoryPath = Environment.getExternalStorageDirectory() + externalStoragePackagesPath;
		
		// pass the callback into the payload
		Object payload = callback;
		
		// delete directory recursively
		FileHelper.deleteDirRecursiveAsync(new File(externalStorageCoursesDirectoryPath), payload, this);
	}

	@Override
	public void onDeleteDirectoryCompleted(boolean success, Object payload) {
		if (payload instanceof ClearApplicationDataCompleted) {
			ClearApplicationDataCompleted callback = (ClearApplicationDataCompleted)payload;
			callback.onClearApplicationDataCompleted();
		}
	}
	
	@Override
	public void resetApplication() {
		Intent loginActivity = new Intent(this, LoginActivity.class);
		loginActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(loginActivity);
	}
}