package com.wickham.android;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.wickham.android.recordwav.R;

public class FileSaveDialog extends Dialog {

    private EditText mFilename;
    private Message mResponse;

    public FileSaveDialog(Context context,
                          Resources resources,
                          String originalName,
                          Message response) {
        super(context);

        // Inflate our UI from its XML layout description.
        setContentView(R.layout.file_save);

        setTitle(resources.getString(R.string.file_save_title));

        mFilename = (EditText)findViewById(R.id.filename);

        Button save = (Button)findViewById(R.id.save);
        save.setOnClickListener(saveListener);
        Button cancel = (Button)findViewById(R.id.cancel);
        cancel.setOnClickListener(cancelListener);
        mResponse = response;
    }

    private View.OnClickListener saveListener = new View.OnClickListener() {
            public void onClick(View view) {
                mResponse.obj = mFilename.getText();
                mResponse.sendToTarget();
                dismiss();
            }
        };

    private View.OnClickListener cancelListener = new View.OnClickListener() {
            public void onClick(View view) {
                dismiss();
            }
        };
}
