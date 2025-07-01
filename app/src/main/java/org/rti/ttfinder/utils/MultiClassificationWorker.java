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
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.rti.ttfinder.data.entity.Assessment;
import org.rti.ttfinder.data.preference.AppPreference;
import org.rti.ttfinder.data.preference.PrefKey;
import org.rti.ttfinder.models.ClassificationQueue;
import org.rti.ttfinder.wrapper.TTProcessor;

import java.io.File;
import java.util.ArrayList;

public class MultiClassificationWorker extends Worker {

    private Context mContext;

    public MultiClassificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.mContext = context;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @NonNull
    @Override
    public Result doWork() {
            ArrayList<Assessment> results = new ArrayList<>();
            Context applicationContext = getApplicationContext();

            ArrayList<ClassificationQueue> classificationQueues = new ArrayList<>();
            String existingData = AppPreference.getInstance(applicationContext).getString(PrefKey.GRADING_QUEUE);
            if (existingData != null) {
                classificationQueues.addAll(new Gson().fromJson(existingData, new TypeToken<ArrayList<ClassificationQueue>>() {
                }.getType()));
            }
            String root = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                    ? Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString()
                    : Environment.getExternalStorageDirectory().toString();

            TTProcessor imageProcessor = ImageClassificationUtils.getInstance(applicationContext).setupImageProcessor(root, LOG_OUTPUT_DIRECTORY);

            File destinationImageFolder = AppUtils.getRejectedImageOutputDirectory(applicationContext);

            int totalCount = classificationQueues.size();
            int maxProgress = 100;
            int currentProgress = 0;
            broadcastWorkProgress(currentProgress);
            for (int i = 0; i < classificationQueues.size(); i++) {
                results.clear();
                ClassificationQueue classificationQueue = classificationQueues.get(i);
                Log.v("GradingResult", new Gson().toJson(classificationQueue));
                ImageClassificationUtils.getInstance(applicationContext).processAndHandleResults(classificationQueue, imageProcessor, destinationImageFolder, results);
                currentProgress = (i + 1) * maxProgress / totalCount;
                // Save assessment data
                ImageClassificationUtils.getInstance(applicationContext).saveData(results);
                // clear task from queue that are completed
                AppPreference.getInstance(applicationContext).removeGradingFromQueue(classificationQueue.getAssessment().getTt_tracker_id());
                // Update progress using the BroadcastReceiver
                broadcastWorkProgress(currentProgress);
            }
            return Result.success();
    }
    private void broadcastWorkProgress(int progress){
        Intent intent = new Intent("WORK_PROGRESS_ACTION");
        intent.putExtra("progress", progress);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        NotificationUtils.updateNotification(progress, "Batch");
    }
}
