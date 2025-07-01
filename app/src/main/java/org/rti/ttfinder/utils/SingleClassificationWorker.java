package org.rti.ttfinder.utils;

import static org.rti.ttfinder.data.AppConstants.LOG_OUTPUT_DIRECTORY;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;

import org.rti.ttfinder.data.entity.Assessment;
import org.rti.ttfinder.models.ClassificationQueue;
import org.rti.ttfinder.wrapper.TTProcessor;

import java.io.File;
import java.util.ArrayList;

public class SingleClassificationWorker extends Worker {

    private Context mContext;

    public SingleClassificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.mContext = context;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @NonNull
    @Override
    public Result doWork() {
        ArrayList<Assessment> results = new ArrayList<>();
        Context applicationContext = getApplicationContext();
        String root = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                ? Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString()
                : Environment.getExternalStorageDirectory().toString();

        TTProcessor imageProcessor = ImageClassificationUtils.getInstance(applicationContext).setupImageProcessor(root, LOG_OUTPUT_DIRECTORY);
        File destinationImageFolder = AppUtils.getRejectedImageOutputDirectory(applicationContext);

        // Parse input data from work request
        Data inputData = getInputData();
        String inputValue = inputData.getString("classificationQueueData");
        ClassificationQueue classificationQueue = new Gson().fromJson(inputValue, ClassificationQueue.class);
        Log.v("classificationQueue", new Gson().toJson(classificationQueue));

        broadcastWorkProgress(0, "TTID: "+classificationQueue.getAssessment().getTt_tracker_id());
        try{
            ImageClassificationUtils.getInstance(applicationContext).processAndHandleResults(classificationQueue, imageProcessor, destinationImageFolder, results);

            Log.v("GradingResult", new Gson().toJson(results));

            ImageClassificationUtils.getInstance(applicationContext).saveData(results);
            broadcastWorkProgress(100, "TTID: "+classificationQueue.getAssessment().getTt_tracker_id());
            return Result.success();
        }
        catch (Exception e){
            ImageClassificationUtils.getInstance(applicationContext).pushToQueue(classificationQueue);
            return Result.failure();
        }
    }

    private void broadcastWorkProgress(int progress, String prefixMessage){
        Intent intent = new Intent("WORK_PROGRESS_ACTION");
        intent.putExtra("progress", progress);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        NotificationUtils.updateNotification(progress, prefixMessage);
    }
}
