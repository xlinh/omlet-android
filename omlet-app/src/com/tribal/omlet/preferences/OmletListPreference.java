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

package com.tribal.omlet.preferences;

import com.tribal.omlet.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tribal.mobile.preferences.CustomListPreference;
import com.tribal.mobile.util.ViewHelper;

/**
 * Omlet implementation of {@link CustomListPreference}.
 * 
 * @author Jon Brasted
 */
public class OmletListPreference extends CustomListPreference {

	/* Constructors */

	public OmletListPreference(Context context) {
		super(context);
	}

	public OmletListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/* Methods */

	@Override
	public View getView(View convertView, ViewGroup parent) {
		View view = super.getView(convertView, parent);

		// get title and summary View
		View titleView = view.findViewById(android.R.id.title);

		if (titleView != null) {
			TextView titleTextView = (TextView) titleView;
			titleTextView.setTextAppearance(view.getContext(), R.style.PreferencesTitleTextAppearance);
			
			ViewHelper.setTypeFace(titleTextView);
		}

		View summaryView = view.findViewById(android.R.id.summary);

		if (summaryView != null) {
			TextView summaryTextView = (TextView) summaryView;
			summaryTextView.setTextAppearance(view.getContext(), R.style.PreferencesSummaryTextAppearance);
			
			
			//summaryTextView.setText("Currently Using: " +summaryTextView.getText());
			
			ViewHelper.setTypeFace(summaryTextView);
		}

		return view;
	}
}