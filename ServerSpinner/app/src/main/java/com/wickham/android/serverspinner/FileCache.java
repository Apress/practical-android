package com.wickham.android.serverspinner;

import java.io.File;
import android.content.Context;
import android.os.Environment;

public class FileCache {
    private File cacheDir;
    public FileCache(Context context) {
        cacheDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
    }
    public File getFile(String url){
        String filename=String.valueOf(url.hashCode());
        File f = new File(cacheDir, filename);
        return f;
    }
    public void clear(){
        File[] files=cacheDir.listFiles();
        if(files==null)
            return;
        for(File f:files)
            f.delete();
    }
}