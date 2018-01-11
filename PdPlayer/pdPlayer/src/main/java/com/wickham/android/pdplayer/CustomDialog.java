package com.wickham.android.pdplayer;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;

public class CustomDialog extends Dialog implements OnClickListener {

	public CustomDialog(Context context) {
		super(context);
		/** 'Window.FEATURE_NO_TITLE' - Used to hide the title */
		requestWindowFeature(Window.FEATURE_NO_TITLE);
	}

//	@Override
	public void onClick(View v) {
	}

}
