package com.wickham.android.lazyloading;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import android.graphics.Bitmap;

public class MemoryCache {
    private Map<String, Bitmap> cache = Collections.synchronizedMap(
            new LinkedHashMap<String, Bitmap>(10, 0.75f, true)); //Last argument true for LRU ordering
    private long size = 0; //current allocated size
    private long limit = 2000000; //max memory in bytes

    public MemoryCache() {
        //use 25% of available heap size
        setLimit(Runtime.getRuntime().maxMemory()/4);
    }
    
    public void setLimit(long new_limit) {
        limit = new_limit;
    }

    public Bitmap get(String id) {
        try {
            if (!cache.containsKey(id)) return null;
            return cache.get(id);
        } catch(NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void put(String id, Bitmap bitmap) {
        try {
            if (cache.containsKey(id)) size -= getSizeInBytes(cache.get(id));
            cache.put(id, bitmap);
            size += getSizeInBytes(bitmap);
            checkSize();
        } catch(Throwable e) {
            e.printStackTrace();
        }
    }
    
    private void checkSize() {
        if(size>limit) {
            Iterator<Entry<String, Bitmap>> iter=cache.entrySet().iterator();	//least recently accessed item will be the first one iterated  
            while(iter.hasNext()) {
                Entry<String, Bitmap> entry=iter.next();
                size-=getSizeInBytes(entry.getValue());
                iter.remove();
                if (size <= limit) break;
            }
        }
    }

    public void clear() {
        try {
            cache.clear();
            size=0;
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }

    long getSizeInBytes(Bitmap bitmap) {
        if (bitmap == null) return 0;
        return bitmap.getRowBytes() * bitmap.getHeight();
    }
}