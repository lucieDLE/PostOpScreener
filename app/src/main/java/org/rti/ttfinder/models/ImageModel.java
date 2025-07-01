package org.rti.ttfinder.models;

import android.graphics.Bitmap;
import android.net.Uri;

public class ImageModel {
    public String imageName;
    public String imagePath;
    public Uri imageUri;
    public Bitmap bitmap;
    public boolean isLeft;

    public ImageModel() {
    }

    public ImageModel(String imageName, String imagePath, Uri imageUri, Bitmap bitmap) {
        this.imageName = imageName;
        this.imagePath = imagePath;
        this.imageUri = imageUri;
        this.bitmap = bitmap;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public Uri getImageUri() {
        return imageUri;
    }

    public void setImageUri(Uri imageUri) {
        this.imageUri = imageUri;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public boolean isLeft() {
        return isLeft;
    }

    public void setLeft(boolean left) {
        isLeft = left;
    }
}
