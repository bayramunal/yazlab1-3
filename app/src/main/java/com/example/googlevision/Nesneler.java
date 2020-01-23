
package com.example.googlevision;

import android.graphics.Bitmap;

public class Nesneler {
    private Bitmap bitmap;
    private String isim;
    private float tahminOrani;

    public Nesneler() {

    }

    public Nesneler(Bitmap bitmap, String name, float score) {
        this.bitmap = bitmap;
        this.isim = name;
        this.tahminOrani = score;
    }

    public String getIsim() {
        return isim;
    }

    public void setIsim(String name) {
        this.isim = name;
    }

    public float getTahminOrani() {
        return tahminOrani;
    }

    public void setTahminOrani(float score) {
        this.tahminOrani = score;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }
}



