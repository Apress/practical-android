package com.wickham.android.splash;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class MainActivity extends Activity {
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main); 
        
        // Setup the ActionBar and the Spinner in the ActionBar
        getActionBar().setDisplayShowTitleEnabled(true);
        getActionBar().setSubtitle("Practical Android");
        getActionBar().setTitle("Splash");
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	new MenuInflater(this).inflate(R.menu.actions, menu);
    	return(super.onCreateOptionsMenu(menu));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item.getItemId() == R.id.exit) {
    		finish();
    		return(true);
    	}
    	return(super.onOptionsItemSelected(item));
    }      
		   
}