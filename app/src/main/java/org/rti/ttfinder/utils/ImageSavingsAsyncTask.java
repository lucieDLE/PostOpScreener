package org.rti.ttfinder.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.AsyncTask;

public class ImageSavingsAsyncTask extends AsyncTask<Void, Void, Void> {
    Bitmap bitmap;
    String imagePath;
    float rotationDegrees;

    public ImageSavingsAsyncTask(Bitmap bitmap, String imagePath, float rotationDegrees) {
        this.bitmap = bitmap;
        this.imagePath = imagePath;
        this.rotationDegrees = rotationDegrees;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        ImageUtils.saveImage(bitmap, imagePath,  false);
        return null;
    }
}
