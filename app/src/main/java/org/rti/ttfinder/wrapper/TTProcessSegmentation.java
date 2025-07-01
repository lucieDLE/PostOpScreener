package org.rti.ttfinder.wrapper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import org.pytorch.Tensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * This is a helper class to takes in a segmentation (assumed already set),
 * fit a polynomial to eyelid label, and create a stack of NUM_SUBIMAGES images
 * uniformly distributed along this polynomial.
 *
 */
public class TTProcessSegmentation {

    private final String TAG = "TTProcessSegmentation";
    private int subImageSize;
    private int numSubImages;

    private File outputDirectory = null;

    private ArrayList<Bitmap> subImageStack;
    public ArrayList<String> logMessages;

    /**
     * Small class to store point coordinates
     */
    public static class Point {
        public double x; //column
        public double y; //row
        Point(double x, double y){
            this.x = x;
            this.y = y;
        }

        @NonNull
        @Override
        public String toString() {
            return "(" + this.x + "," + this.y +  ")";
        }
    }
    private ArrayList<TTProcessSegmentation.Point> eyelidPolynomialSamples;
    private ArrayList<TTProcessSegmentation.Point> eyelidPixels; //Column indices for pixels with eyelids.
    private Set<String> labels;


    public TTProcessSegmentation(final int sub_image_size,
                                 final int num_subImages
                                )
    {
        this.subImageSize = sub_image_size;
        this.numSubImages = num_subImages;
        this.outputDirectory = null;
        this.eyelidPolynomialSamples = new ArrayList<TTProcessSegmentation.Point>();
        this.eyelidPixels = new ArrayList<TTProcessSegmentation.Point>();
        this.subImageStack = new ArrayList<Bitmap>();
        this.logMessages = new ArrayList<String>();
        this.labels = new HashSet<String>();
    }

    /* Resets the state of the class to start new processing. */
    public void reset(final File outputDirectory)
    {
        this.outputDirectory = outputDirectory;
        this.eyelidPolynomialSamples = new ArrayList<TTProcessSegmentation.Point>();
        this.eyelidPixels = new ArrayList<TTProcessSegmentation.Point>();
        this.subImageStack = new ArrayList<Bitmap>();
        this.logMessages = new ArrayList<String>();
        this.labels = new HashSet<String>();
    }

    public void setDebugOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public final Bitmap getNthSubImage(final int n){
        if(subImageStack != null && n>=0 && n<subImageStack.size()){
            return subImageStack.get(n);
        }
        return null;
    }

    /**
     * Create a list of eyelid pixel points, and find the quality of the image
     *
     * @param inputBitmap: original image, but scaled down for segmentation
     * @param inputBuffer: segmentation model output in the form of a tensorbuffer.
     */
    public Bitmap findEyelidPixels(final Bitmap inputBitmap,
                                   final TensorBuffer inputBuffer)
    {
        if(inputBitmap == null) {
            Log.e(TAG, "Input bitmap image is not set.");
            return null;
        }

        if(inputBuffer == null) {
            Log.e(TAG, "Input bitmap image is not set.");
            return null;
        }

        byte[] bytes = inputBuffer.getBuffer().array();

        int[] colors = new int[bytes.length];

        labels = new HashSet<String>();
        eyelidPixels.clear();
        // Create an initial estimate for the eyelid points.
        eyelidPixels = new ArrayList<Point>(inputBuffer.getFlatSize()/2);
        int imageSizeX = inputBuffer.getShape()[1]; //height
        int imageSizeY = inputBuffer.getShape()[2]; //width
        Log.v(TAG, "Image Size: " + imageSizeX + " " + imageSizeY);
        if (inputBitmap != null && (inputBitmap.getHeight() != imageSizeX || inputBitmap.getWidth() != imageSizeY )) {
            Log.e(TAG, "Dimensions for the input image and input buffer do not agree");
            return null;
        }

        long startGatherEyelidPointsTime = SystemClock.uptimeMillis();
        int r = -1;
        int flatsize = inputBuffer.getFlatSize();
        Log.v(TAG, "Flat size: " + flatsize);
        Log.v(TAG, "Byte suze: " + bytes.length);

        for (int ind = 0; ind < flatsize; ind++) {
            int A = 255;
            int R = 0;
            int G = 0;
            int B = 0;
//            int r = (int) Math.floor(ind / imageSizeX);
            int c = ind % imageSizeY;
            if (c==0) {
                r += 1;
            }
            if (bytes[ind] == 0) { // Assumed to be background
                A = 0;
                labels.add("Background");
            } else if (bytes[ind] == 1) { //Assumed to be cornea
                labels.add("Cornea");
                B = 255;
            } else if (bytes[ind] == 2) { //Assumed to be the pupil
                labels.add("Iris");
                G = 255;
            }
            if (bytes[ind] == 3) { //Assumed to be eyelid region
                labels.add("Eyelid");
                R = 255;
                eyelidPixels.add(new Point(c, r));
            }
            // Create a colored output image if requested
            if (this.outputDirectory != null) {
                colors[ind] = (A & 0xff) << 24 | (B & 0xff) << 16 | (G & 0xff) << 8 | (R & 0xff);
                if (A == 0) {
                    // For background, use RGB from the original image
                    colors[ind] = inputBitmap.getPixel(c, r);
                }
            }
        }

        Log.v(TAG, "Found " + eyelidPixels.size() + " eyelid pixels");
        eyelidPixels.trimToSize();
        Bitmap seg = null;
        if(this.outputDirectory != null) {
            seg = Bitmap.createBitmap(colors, imageSizeX, imageSizeY, Bitmap.Config.ARGB_8888);
        }

        long endGatherEyelidPointsTime = SystemClock.uptimeMillis();
        String msg = "Time cost to findEyelidPoints :: " + (endGatherEyelidPointsTime - startGatherEyelidPointsTime) + "ms";
        Log.v(TAG, msg);
        logMessages.add(msg);
        logMessages.add("Found number of eyelid points :: " + eyelidPixels.size());
        return seg;
    }

    /**
     * Create a list of eyelid pixel points, and find the quality of the image
     *
     * @param inputBitmap: original image, but scaled down for segmentation
     * @param inputBuffer: segmentation model output in the form of a Tensor for Pytorch.
     */
    public Bitmap findEyelidPixels(final Bitmap inputBitmap,
                                   final Tensor inputBuffer)
    {
        if(inputBitmap == null) {
            Log.e(TAG, "Input bitmap image is not set.");
            return null;
        }

        if(inputBuffer == null) {
            Log.e(TAG, "Input bitmap image is not set.");
            return null;
        }

        byte[] bytes = inputBuffer.getDataAsUnsignedByteArray();
        int[] colors = new int[bytes.length];

        labels = new HashSet<String>();
        eyelidPixels.clear();
        // Create an initial estimate for the eyelid points.
        eyelidPixels = new ArrayList<Point>(bytes.length /2);
        int imageSizeX = (int)inputBuffer.shape()[2]; //height
        int imageSizeY = (int)inputBuffer.shape()[3]; //width
        Log.v(TAG, "Image Size: " + imageSizeX + " " + imageSizeY);
        if (inputBitmap != null && (inputBitmap.getHeight() != imageSizeX || inputBitmap.getWidth() != imageSizeY )) {
            Log.e(TAG, "Dimensions for the input image and input buffer do not agree");
            return null;
        }

        long startGatherEyelidPointsTime = SystemClock.uptimeMillis();
        int r = -1;
        for (int ind = 0; ind < bytes.length; ind++) {
            int A = 255;
            int R = 0;
            int G = 0;
            int B = 0;
            //int r = (int) Math.floor(ind / imageSizeX);
            int c = ind % imageSizeY;
            if (c==0) {
                r +=1;
            }
            if (bytes[ind] == 0) { // Assumed to be background
                A = 0;
                labels.add("Background");
            } else if (bytes[ind] == 1) { //Assumed to be cornea
                labels.add("Cornea");
                B = 255;
            } else if (bytes[ind] == 2) { //Assumed to be the pupil
                labels.add("Iris");
                G = 255;
            }
            if (bytes[ind] == 3) { //Assumed to be eyelid region
                labels.add("Eyelid");
                R = 255;
                eyelidPixels.add(new Point(c, r));
            }
            // Create a colored output image if requested
            if (this.outputDirectory != null) {
                colors[ind] = (A & 0xff) << 24 | (B & 0xff) << 16 | (G & 0xff) << 8 | (R & 0xff);
                if (A == 0) {
                    // For background, use RGB from the original image
                    colors[ind] = inputBitmap.getPixel(c, r);
                }
            }
        }
        long iterateenddtime = SystemClock.uptimeMillis();
        String msg_1 = "Time cost to iterate :: " + (iterateenddtime - startGatherEyelidPointsTime) + "ms";
        Log.v(TAG, msg_1);

        Log.v(TAG, "Found " + eyelidPixels.size() + " eyelid pixels");
        eyelidPixels.trimToSize();

        long iterateenddtime1 = SystemClock.uptimeMillis();
        String msg_2 = "Time cost to trim :: " + (iterateenddtime1 - iterateenddtime) + "ms";
        Log.v(TAG, msg_2);


        Bitmap seg = null;
        if(this.outputDirectory != null) {
            seg = Bitmap.createBitmap(colors, imageSizeX, imageSizeY, Bitmap.Config.ARGB_8888);
        }

        long endGatherEyelidPointsTime = SystemClock.uptimeMillis();
        String msg = "Time cost to findEyelidPoints :: " + (endGatherEyelidPointsTime - startGatherEyelidPointsTime) + "ms";
        Log.v(TAG, msg);
        logMessages.add(msg);
        logMessages.add("Found number of eyelid points :: " + eyelidPixels.size());
        return seg;
    }

    public boolean areNumberOfPointsSufficient(final int flatsize){
        return ((double) eyelidPixels.size() > (flatsize * 0.01));
    }

    public boolean areNeededSegmentsPresent(){
        return (labels.contains("Cornea") && labels.contains("Eyelid"));
    }

    /**
     * Fits a polynomial to the pixels in segmentation that are labeled as eyelid
     * TODO: Should have a way to make sure only one connected component is present with eyelid label
     *
     * @param scaleX : Scale along the width
     * @param scaleY : Scale along the height
     */
    public boolean fitPolynomial(double scaleX, double scaleY)
    {
        eyelidPolynomialSamples.clear();
        //Assuming that the polynomial points have already been found
        int N = eyelidPixels.size();
        if(N == 0){
            Log.v(TAG, "Don't have a list of eyelid pixels. returning");
            return false;
        }

        //Scale all the points, and create an array of x an y:
        double [] x = new double[N];
        double [] y = new double[N];
        int minCol = 9999, maxCol = 0, maxRow = 0;

        for(int ind=0; ind < eyelidPixels.size(); ind++){
            x[ind] = eyelidPixels.get(ind).x*scaleX;
            y[ind] = eyelidPixels.get(ind).y*scaleY;
            if(x[ind] > maxCol){
                maxCol = (int)x[ind];
            }
            if(x[ind] < minCol){
                minCol = (int)x[ind];
            }
            if(y[ind] > maxRow){
                maxRow = (int)y[ind];
            }
        }

        logMessages.add("Polynomial minCol :: " + minCol);
        logMessages.add("Polynomial maxCol :: " + maxCol);

        maxCol = maxCol - (int)(subImageSize/2);
        maxRow = maxRow - (int)(subImageSize/2);
        minCol = minCol + (int)(subImageSize/2);

        /* FIT POLYNOMIAL TO THE POINTS */
        long startfitPolynomial = SystemClock.uptimeMillis();
        // Fit a polynomial to the eyelid points
        // This is Based on: https://www.bragitoff.com/2017/04/polynomial-fitting-java-codeprogram-works-android-well/
        int n = 3;
        double X[] = new double[2 * n + 1];
        for (int i = 0; i < 2 * n + 1; i++) {
            X[i] = 0;
            for (int j = 0; j < N; j++)
                X[i] = X[i] + Math.pow(x[j], i);        //consecutive positions of the array will store N,sigma(xi),sigma(xi^2),sigma(xi^3)....sigma(xi^2n)
        }
        double B[][] = new double[n + 1][n + 2], a[] = new double[n + 1];            //B is the Normal matrix(augmented) that will store the equations, 'a' is for value of the final coefficients
        for (int i = 0; i <= n; i++)
            for (int j = 0; j <= n; j++)
                B[i][j] = X[i + j];            //Build the Normal matrix by storing the corresponding coefficients at the right positions except the last column of the matrix
        double Y[] = new double[n + 1];                    //Array to store the values of sigma(yi),sigma(xi*yi),sigma(xi^2*yi)...sigma(xi^n*yi)
        for (int i = 0; i < n + 1; i++) {
            Y[i] = 0;
            for (int j = 0; j < N; j++)
                Y[i] = Y[i] + Math.pow(x[j], i) * y[j];        //consecutive positions will store sigma(yi),sigma(xi*yi),sigma(xi^2*yi)...sigma(xi^n*yi)
        }
        for (int i = 0; i <= n; i++)
            B[i][n + 1] = Y[i];                //load the values of Y as the last column of B(Normal Matrix but augmented)
        n = n + 1;
        for (int i = 0; i < n; i++)                    //From now Gaussian Elimination starts(can be ignored) to solve the set of linear equations (Pivotisation)
            for (int k = i + 1; k < n; k++)
                if (B[i][i] < B[k][i])
                    for (int j = 0; j <= n; j++) {
                        double temp = B[i][j];
                        B[i][j] = B[k][j];
                        B[k][j] = temp;
                    }

        for (int i = 0; i < n - 1; i++)            //loop to perform the gauss elimination
            for (int k = i + 1; k < n; k++) {
                double t = B[k][i] / B[i][i];
                for (int j = 0; j <= n; j++)
                    B[k][j] = B[k][j] - t * B[i][j];    //make the elements below the pivot elements equal to zero or elimnate the variables
            }
        for (int i = n - 1; i >= 0; i--)                //back-substitution
        {                        //x is an array whose values correspond to the values of x,y,z..
            a[i] = B[i][n];                //make the variable to be calculated equal to the rhs of the last equation
            for (int j = 0; j < n; j++)
                if (j != i)            //then subtract all the lhs values except the coefficient of the variable whose value                                   is being calculated
                    a[i] = a[i] - B[i][j] * a[j];
            a[i] = a[i] / B[i][i];            //now finally divide the rhs by the coefficient of the variable to be calculated
        }
        StringBuilder coefficients = new StringBuilder();
        for (double v : a) {
            coefficients.append(v);
            coefficients.append(", ");
        }
        logMessages.add("Polynomial Coefficients :: " + coefficients);

        /* Sample the polynomial curve for subimages */
        int dx = (maxCol - minCol) / numSubImages;
        double neighborhood = (double)subImageSize / 2;

        for (int i = 0; i < numSubImages; i++) {
            int col_sample = minCol + i * dx;
            double y_sample = 0;
            for (int d = 0; d < a.length; d++) {
                y_sample += a[d] * Math.pow((double) col_sample, d);
            }
//                Log.v("FitPolynomial", "Sample x: " + col_sample + " y: " + y_sample);
            double left = Math.min(Math.max(col_sample - neighborhood, 0), maxCol - neighborhood);
            double top = Math.min(Math.max(y_sample - neighborhood, 0), maxRow - neighborhood);
            eyelidPolynomialSamples.add(new TTProcessSegmentation.Point(left, top));
        }
        long endfitPolynomial = SystemClock.uptimeMillis();
        String msg = "Time cost to fitPolynomialToEyelid :: " + (endfitPolynomial - startfitPolynomial) + "ms";
        Log.v(TAG, msg);
        logMessages.add(msg);
        return true;
    }

    public boolean extractSubImages(final Bitmap originalInputBitmap, Bitmap segmentationBitmap)
    {
        if(eyelidPolynomialSamples.size() != numSubImages || eyelidPolynomialSamples.get(0).y < 0 || eyelidPolynomialSamples.get(0).x < 0){
            Log.e(TAG, "Wasn't able to create exact number of eyelid samples");
            return false;
        }

        long startExtractSubImages = SystemClock.uptimeMillis();

        subImageStack.clear();
        Canvas c = null;
        Paint p = null;
        if(segmentationBitmap != null) {
            c = new Canvas(segmentationBitmap);
            p = new Paint();
            p.setStyle(Paint.Style.STROKE);
            p.setColor(Color.YELLOW);
            p.setStrokeWidth(5);
        }

        for(int i=0; i<numSubImages; i++) {
            TTProcessSegmentation.Point samplePoint = eyelidPolynomialSamples.get(i);
            logMessages.add("Point sample " + i + " :: " + samplePoint);
            double left = samplePoint.x;
            double top = samplePoint.y;
            double right = left + subImageSize;
            double bottom = top + subImageSize;
            logMessages.add("Image  extents (l,t,r,b) " + i + " :: " +
                    "(" + left + "," + top + "," + right + "," + bottom + ")");
            Log.d(TAG, "Top: " + top + " left: " + left);
            Bitmap subImage = Bitmap.createBitmap(originalInputBitmap, (int) left, (int) top, subImageSize, subImageSize);
            subImageStack.add(subImage);
            if (outputDirectory != null && segmentationBitmap != null) {
                c.drawRect((float) left, (float) top, (float) right, (float) bottom, p);
                String filepath = outputDirectory.getAbsolutePath() + File.separator + "patch" + i + ".png";
                try (FileOutputStream out = new FileOutputStream(filepath)) {
                    subImage.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                } catch (IOException e) {
                    Log.e(TAG, "Error saving patch: \n" + e);
                }
            }
        }
        long endExtractSubImages = SystemClock.uptimeMillis();
        String msg = "Time cost to Extract sub images :: " + (endExtractSubImages - startExtractSubImages) + "ms";
        Log.v(TAG, msg);
        logMessages.add(msg);
        return true;
        }

    public int getNumSubImages(){
        return subImageStack.size();
    }

    public void cleanup() {
        if(subImageStack != null){
            for(int k=0; k< subImageStack.size(); k++){
                subImageStack.get(k).recycle();
            }
            subImageStack.clear();
        }
    }
}
