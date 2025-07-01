package org.rti.ttfinder.utils;

import static org.rti.ttfinder.utils.AppUtils.copyDirectoryOneLocationToAnotherLocation;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.google.gson.Gson;

import org.rti.ttfinder.R;
import org.rti.ttfinder.data.preference.AppPreference;
import org.rti.ttfinder.data.preference.PrefKey;
import org.rti.ttfinder.models.ClassificationModel;
import org.rti.ttfinder.models.ProcessedImageResult;
import org.rti.ttfinder.wrapper.TTProcessor;

import java.io.File;
import java.io.IOException;

public class ClassificationAsyncTask extends AsyncTask<Void, Void, ClassificationModel> {
    ProgressDialog dialog;
    Activity mActivity;
    private String imageName = "", imagePath = "";
    Bitmap bitmap =null;

    public interface AsyncResponse {
        void processFinish(Boolean output);
    }

    AsyncResponse asyncResponse = null;

    public ClassificationAsyncTask(Activity mActivity, Uri uri, AsyncResponse asyncResponse ){
        super();
        this.mActivity = mActivity;

        Log.e("uri", String.valueOf(uri));
        this.imageName = uri.getLastPathSegment();
        this.imagePath = uri.getPath();
        try {
            this.bitmap = MediaStore.Images.Media.getBitmap(mActivity.getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.asyncResponse = asyncResponse;

    }
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog = ProgressDialog.show(mActivity,"","Processing...",true);
        //showHideLoader(true);
    }

    @Override
    protected ClassificationModel doInBackground(Void... voids) {
        ClassificationModel result;
        String outputDir = "TTScreenerMLAppLogs";
        TTProcessor imageProcessor = null;
        String root;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString();
        }
        else{
            root = Environment.getExternalStorageDirectory().toString();
        }

        try {
            File myDir = new File(root + "/"+ mActivity.getResources().getString(R.string.folder_name)+"/"+outputDir);
            //To do processing without debugging options uncomment the next line
            //imageProcessor = new TTProcessor(this);
            imageProcessor = new TTProcessor(myDir, mActivity, true);
        }
        catch(IOException e) {
            Log.e("MAIN", "Error while setting up image processor : \n" + e);
        }
        String recordStr = imageName.lastIndexOf('.') >= 0 ? imageName.substring(0, imageName.lastIndexOf('.')) : imageName;
        result = processImage(bitmap, recordStr, imageProcessor);
        return result;
    }

    @Override
    protected void onPostExecute(ClassificationModel result) {
        super.onPostExecute(result);
        System.out.println(new Gson().toJson(result));
        if (result == null || result.getClassificationResult() == null) {
            dialog.dismiss();
            File destinationImageFolder = AppUtils.getRejectedImageOutputDirectory(mActivity);
            File file = new File(imagePath);
            if (file.exists()) {
                try {
                    copyDirectoryOneLocationToAnotherLocation(file,destinationImageFolder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (file.delete()) {
                    System.out.println("file Deleted :" + imagePath);
                } else {
                    System.out.println("file not Deleted :" + imagePath);
                }
                Intent returnIntent = new Intent();
                returnIntent.putExtra("result", new Gson().toJson(result));
                mActivity.setResult(Activity.RESULT_OK, returnIntent);
                mActivity.finish();
            }
        }
        else{
            dialog.dismiss();
            if(result.isBlurred()){

                String blurryMessage = mActivity.getString(R.string.blurry_msg);
                alert(blurryMessage, new Gson().toJson(result));
            }
            else {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("result", new Gson().toJson(result));
                mActivity.setResult(Activity.RESULT_OK, returnIntent);
                mActivity.finish();
            }
        }
    }

    public void alert(String message, String result) {
        AlertDialog.Builder bld = new AlertDialog.Builder(mActivity);
        bld.setMessage(message);
        bld.setCancelable(false);
        bld.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                asyncResponse.processFinish(true);
            }
        });
        bld.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                File destinationImageFolder = AppUtils.getRejectedImageOutputDirectory(mActivity);
                File file = new File(imagePath);
                if (file.exists()) {
                    try {
                        copyDirectoryOneLocationToAnotherLocation(file,destinationImageFolder);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (file.delete()) {
                        System.out.println("file Deleted :" + imagePath);
                    } else {
                        System.out.println("file not Deleted :" + imagePath);
                    }
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("result", result);
                    mActivity.setResult(Activity.RESULT_OK, returnIntent);
                    mActivity.finish();
                }
            }
        });
        bld.create().show();
    }

    private ClassificationModel processImage(final Bitmap mBitmapIn, final String recordIdentifier, TTProcessor imageProcessor) {
        boolean isDebuggable = AppPreference.getInstance(mActivity).getBoolean(PrefKey.IS_DEBUGGABLE, false);
        if(imageProcessor == null) {
            Log.e("MAINProcessImage", "Image processor not initialized, returning");
            return null;
        }

        if(mBitmapIn == null) {
            Log.e("MAINProcessImage", "Image not initialized, returning");
            return null;
        }

        int mPhotoWidth = mBitmapIn.getWidth();
        int mPhotoHeight = mBitmapIn.getHeight();
        Log.v("MAINProcessImage", "Original image dimensions : " + mPhotoWidth + "," + mPhotoHeight);

        int ycrop = 0, xcrop=0, targetWidth = 0;
        if(mPhotoHeight >= mPhotoWidth) {
            ycrop = (mPhotoHeight - mPhotoWidth) / 2;
            targetWidth = mPhotoWidth;
        }
        else {
            xcrop = (mPhotoWidth - mPhotoHeight)/2;
            targetWidth = mPhotoHeight;
        }

        Log.v("MAINProcessImage", " image dimensions : width " + xcrop + ", height " + ycrop);

        Bitmap nBitmap = Bitmap.createBitmap(mBitmapIn, xcrop, ycrop, targetWidth, targetWidth);
        // Rotate the image by 90 degrees. This is a hack to get correct orientation of the downloaded image.
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap finalBitmap = Bitmap.createBitmap(nBitmap , 0, 0,
                nBitmap.getWidth(), nBitmap.getHeight(),
                matrix, true);

        mPhotoWidth = finalBitmap.getWidth();
        mPhotoHeight = finalBitmap.getHeight();
        Log.v("MAINProcessImage", "After crop: " + mPhotoWidth + "," + mPhotoHeight);

        ClassificationModel classificationModel = new ClassificationModel();

        long startTimeForReference = SystemClock.uptimeMillis();
        boolean isBlurred = ImageUtils.checkForBlurryImage(finalBitmap);
        long endTimeForReference = SystemClock.uptimeMillis();
        Log.v("MAINProcessImage", "Took " + (endTimeForReference-startTimeForReference) + "ns to process BLURRINESS");
        Log.v("MAINProcessImage", "Blurry Image => "+isBlurred);
        if(isBlurred){
            classificationModel.setBlurred(isBlurred);
            classificationModel.setClassificationResult("Blurry Image");
            classificationModel.setImageName(imageName);
            classificationModel.setStartedTime(AppUtils.getDate(startTimeForReference));
            classificationModel.setEndedTime(AppUtils.getDate(System.currentTimeMillis()));
            return classificationModel;
        }


        long startTimeForImageProcessing = SystemClock.uptimeMillis();
        long startTime = System.currentTimeMillis();
        try {
            imageProcessor.setCreateDebugImages(isDebuggable);
            ProcessedImageResult processImage = imageProcessor.processImage(finalBitmap, recordIdentifier);
            if(processImage.isSuccess()) {
                // Get the recognition result, and output
                String result = imageProcessor.getLastClassificationResult();
                // Get the pipeline time cost
                long timeCostMs = imageProcessor.getLastPipelineTimeCost();

                classificationModel.setClassificationResult(result);
                classificationModel.setLogFileName(processImage.getLogFileName());

                Log.v("MAINProcessImage", "Classification result (label) : " + result);
                Log.v("MAINProcessImage", "Pipeline time cost : " + timeCostMs + "ms");
            }

        }
        catch (IOException e) {
            Log.e("MAINProcessImage", "**** IOException processing hte image : ERROR :: \n" + e);
        }
        catch (SecurityException e) {
            Log.e("MAINProcessImage", "**** Security Exception processing the image :: \n" + e);
        }
        long endTimeForImageProcessing = SystemClock.uptimeMillis();
        Log.v("MAINProcessImage", "Time cost to process image: " + (endTimeForImageProcessing - startTimeForImageProcessing) + "ms");
        classificationModel.setStartedTime(AppUtils.getDate(startTime));
        classificationModel.setEndedTime(AppUtils.getDate(System.currentTimeMillis()));
        classificationModel.setImageName(imageName);
        classificationModel.setSuccess(true);

        return classificationModel;
    }

}