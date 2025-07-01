package org.rti.ttfinder.utils;

import static org.rti.ttfinder.utils.AppUtils.copyDirectoryOneLocationToAnotherLocation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.rti.ttfinder.R;
import org.rti.ttfinder.data.entity.Assessment;
import org.rti.ttfinder.data.helper.DaoHelper;
import org.rti.ttfinder.data.helper.DbLoaderInterface;
import org.rti.ttfinder.data.loader.AssessmentLoader;
import org.rti.ttfinder.data.preference.AppPreference;
import org.rti.ttfinder.data.preference.PrefKey;
import org.rti.ttfinder.models.ClassificationModel;
import org.rti.ttfinder.models.ClassificationQueue;
import org.rti.ttfinder.models.ImageModel;
import org.rti.ttfinder.models.ProcessedImageResult;
import org.rti.ttfinder.wrapper.TTProcessor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ImageClassificationUtils {

    private static ImageClassificationUtils instance;
    private Context mContext;

    // Private constructor to prevent instantiation from other classes
    private ImageClassificationUtils(Context context) {
        mContext = context.getApplicationContext();
    }

    // Public method to provide access to the singleton instance
    public static synchronized ImageClassificationUtils getInstance(Context context) {
        if (instance == null) {
            instance = new ImageClassificationUtils(context);
        }
        return instance;
    }

    public TTProcessor setupImageProcessor(String root, String outputDir) {
        TTProcessor imageProcessor = null;
        try {
            File myDir = new File(root + "/" + mContext.getResources().getString(R.string.folder_name) + "/" + outputDir);
            imageProcessor = new TTProcessor(myDir, mContext, true);
        } catch (IOException e) {
            Log.e("MAIN", "Error while setting up image processor : \n" + e);
        }
        return imageProcessor;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void processAndHandleResults(ClassificationQueue classificationQueue, TTProcessor imageProcessor, File destinationImageFolder, ArrayList<Assessment> results) {
        ImageModel leftImageModel = classificationQueue.getLeftEyeModel().getImage();
        ImageModel rightImageModel = classificationQueue.getRightEyeModel().getImage();

        processImageModel(leftImageModel, imageProcessor, destinationImageFolder, results, classificationQueue.getAssessment(), true);
        processImageModel(rightImageModel, imageProcessor, destinationImageFolder, results, classificationQueue.getAssessment(), false);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void processImageModel(ImageModel imageModel, TTProcessor imageProcessor, File destinationImageFolder, ArrayList<Assessment> results, Assessment assessment, boolean isLeftEye) {
        File imgFile = new File(imageModel.getImagePath());
        Log.v("ImagePath", String.valueOf(imgFile.exists()));
        if (imgFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            imageModel.setBitmap(bitmap);

            ClassificationModel classification = processImage(imageModel, imageProcessor);

            if (classification == null || classification.getClassificationResult() == null || classification.isBlurred()) {
                moveAndDeleteFile(imgFile, destinationImageFolder, classification);
            } else {
                updateAssessmentResults(assessment, classification, isLeftEye);
                results.add(assessment);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void moveAndDeleteFile(File sourceFile, File destinationFolder, ClassificationModel classification) {
        try {
            copyDirectoryOneLocationToAnotherLocation(sourceFile, destinationFolder);
            if(sourceFile.exists()) {
                if (sourceFile.delete()) {
                    System.out.println("file Deleted: " + sourceFile.getPath());
                }
                else {
                    System.out.println("file not Deleted: " + sourceFile.getPath());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getLocalizedMessage());
        }
    }

    public void updateAssessmentResults(Assessment assessment, ClassificationModel classification, boolean isLeftEye) {
        if (isLeftEye) {
            assessment.setLeft_eye_result(classification.getClassificationResult());
            assessment.setLeft_eye_classification_started(classification.getStartedTime());
            assessment.setLeft_eye_classification_ended(classification.getEndedTime());
        } else {
            assessment.setRight_eye_result(classification.getClassificationResult());
            assessment.setRight_eye_classification_started(classification.getStartedTime());
            assessment.setRight_eye_classification_ended(classification.getEndedTime());
        }
        assessment.setAssesment_started(AppUtils.getDate(System.currentTimeMillis()));
        assessment.setAssessment_ended(AppUtils.getDate(System.currentTimeMillis()));
    }

    public ClassificationModel processImage(final ImageModel imageModel, TTProcessor imageProcessor) {
        boolean isDebuggable = AppPreference.getInstance(mContext).getBoolean(PrefKey.IS_DEBUGGABLE, false);
        if(imageProcessor == null) {
            Log.e("MAINProcessImage", "Image processor not initialized, returning");
            return null;
        }

        if(imageModel.getBitmap() == null) {
            Log.e("MAINProcessImage", "Image not initialized, returning");
            return null;
        }

        int mPhotoWidth = imageModel.getBitmap().getWidth();
        int mPhotoHeight = imageModel.getBitmap().getHeight();
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

        Bitmap nBitmap = Bitmap.createBitmap(imageModel.getBitmap(), xcrop, ycrop, targetWidth, targetWidth);
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
            classificationModel.setImage(imageModel);
            classificationModel.setBlurred(isBlurred);
            classificationModel.setClassificationResult("Blurry Image");
            return classificationModel;
        }

        long startTimeForImageProcessing = SystemClock.uptimeMillis();
        long startTime = System.currentTimeMillis();
        try {
            imageProcessor.setCreateDebugImages(isDebuggable);
            ProcessedImageResult processImage = imageProcessor.processImage(finalBitmap, imageModel.getImageName());
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
            return null;
        }
        catch (SecurityException e) {
            Log.e("MAINProcessImage", "**** Security Exception processing the image :: \n" + e);
            return null;
        }
        long endTimeForImageProcessing = SystemClock.uptimeMillis();
        Log.v("MAINProcessImage", "Time cost to process image: " + (endTimeForImageProcessing - startTimeForImageProcessing) + "ms");
        classificationModel.setStartedTime(AppUtils.getDate(startTime));
        classificationModel.setEndedTime(AppUtils.getDate(System.currentTimeMillis()));
        classificationModel.setImageName(imageModel.imageName);
        classificationModel.setImage(imageModel);
        return classificationModel;
    }

    public void saveData(ArrayList<Assessment> assessments){
        AssessmentLoader loader = new AssessmentLoader(mContext);
        loader.setDbLoaderInterface(new DbLoaderInterface() {
            @Override
            public void onFinished(Object object) {
                Log.d("Added","Assessment added");
            }
        });
        loader.execute(DaoHelper.INSERT_ALL, assessments);

    }

    public void pushToQueue(ClassificationQueue classificationQueue){
        ArrayList<ClassificationQueue> updatedQueues = new ArrayList<>();
        String existingData = AppPreference.getInstance(mContext).getString(PrefKey.GRADING_QUEUE);
        if(existingData != null){
            ArrayList<ClassificationQueue> existingQueue = new Gson().fromJson(existingData, new TypeToken<ArrayList<ClassificationQueue>>(){}.getType());
            updatedQueues.addAll(existingQueue);
        }
        updatedQueues.add(classificationQueue);
        String updatedQueueString = new Gson().toJson(updatedQueues);
        AppPreference.getInstance(mContext).setString(PrefKey.GRADING_QUEUE, updatedQueueString);

        //Toast.makeText(mContext, mContext.getResources().getString(R.string.queue_added_msg), Toast.LENGTH_SHORT).show();
    }

    // Optionally, you can provide a method to access the Context
    public Context getContext() {
        return mContext;
    }
}
