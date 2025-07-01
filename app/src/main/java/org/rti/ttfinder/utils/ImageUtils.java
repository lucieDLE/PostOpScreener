package org.rti.ttfinder.utils;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.FileOutputStream;
import java.io.IOException;

public class ImageUtils {

    public static boolean checkForBlurryImage(Bitmap image) {
        System.gc();
        try {
            Mat matImage = new Mat();
            Utils.bitmapToMat(image, matImage);
            Mat matImageGrey = new Mat();
            Imgproc.cvtColor(matImage, matImageGrey, Imgproc.COLOR_BGR2GRAY);

            Mat laplacianImage = new Mat();
            Imgproc.Laplacian(matImageGrey, laplacianImage, CvType.CV_8U);

            int rows = laplacianImage.rows();
            int cols = laplacianImage.cols();

            int maxLap = 0;
            byte[] lapData = new byte[rows * cols];
            laplacianImage.get(0, 0, lapData);
            for (byte pixel : lapData) {
                int value = pixel & 0xFF; // Convert byte to unsigned int
                if (value > maxLap) {
                    maxLap = value;
                }
            }

            int soglia = 150; // Adjust this threshold as needed
            if (maxLap <= soglia) {
                System.out.println("**** THIS IMAGE IS BLURRY: " + maxLap);
                return true;
            }
            System.runFinalization();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            System.runFinalization();
            return true;
        }
    }

    public static void saveImage(final Bitmap bitmap, final String filepath, boolean compress){
        try (FileOutputStream out = new FileOutputStream(filepath)) {
            if(compress)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
            else
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
        } catch (IOException e) {
            Log.e("ImageUtils","Error saving image: \n" + e);
        }
    }
}
