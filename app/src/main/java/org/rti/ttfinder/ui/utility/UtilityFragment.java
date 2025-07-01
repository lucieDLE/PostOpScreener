package org.rti.ttfinder.ui.utility;

import static org.rti.ttfinder.utils.AppUtils.copyDirectoryOneLocationToAnotherLocation;
import static org.rti.ttfinder.utils.AppUtils.deleteRecursive;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import org.rti.ttfinder.MyApplication;
import org.rti.ttfinder.R;
import org.rti.ttfinder.data.entity.Assessment;
import org.rti.ttfinder.data.helper.DaoHelper;
import org.rti.ttfinder.data.helper.DbLoaderInterface;
import org.rti.ttfinder.data.loader.AssessmentLoader;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.opencsv.CSVWriter;

import org.jetbrains.annotations.NotNull;
import org.rti.ttfinder.data.preference.AppPreference;
import org.rti.ttfinder.data.preference.PrefKey;
import org.rti.ttfinder.models.ClassificationQueue;
import org.rti.ttfinder.utils.AppUtils;
import org.rti.ttfinder.utils.MultiClassificationWorker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class UtilityFragment extends Fragment {

    private Button btnExport;
    private Button btnGradeImage;
    private Switch sAutomaticGrading;
    private Switch sDebug;
    private Switch sScreeningQuestion;
    private Switch sAdjudication;

    private CardView cvGradingLoader;
    private CardView cvExportLoader;
    private TextView tvGradingProgress;
    private LinearLayout llDebugSetting, llAutomaticGradingSetting, llGradeImageSetting, llScreeningQuestion, llAdjudicationSetting;

    private boolean isAdminModeEnable = false;
    private boolean isDebugEnable = false;
    private boolean isScreeningQuestionEnable = false;
    private boolean isAutomaticGradingEnable = false;
    private boolean isAdjudicationEnable = false;
    ArrayList<ClassificationQueue> existingQueue = new ArrayList<>();
    private OneTimeWorkRequest workRequest;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_utility, container, false);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initVariable();
        initView(view);
        initFunctionality();
        initListener();
    }

    private void initVariable() {
        isAdjudicationEnable = AppPreference.getInstance(getActivity()).getBoolean(PrefKey.ADJUDICATION_ENABLE, false);
        isAdminModeEnable = AppPreference.getInstance(getActivity()).getBoolean(PrefKey.IS_ADMIN, false);
        isAutomaticGradingEnable = AppPreference.getInstance(getActivity()).getBoolean(PrefKey.AUTOMATIC_GRADING_ENABLE, true);
        isDebugEnable = AppPreference.getInstance(getActivity()).getBoolean(PrefKey.IS_DEBUGGABLE, false);
        isScreeningQuestionEnable = AppPreference.getInstance(getActivity()).getBoolean(PrefKey.IS_SCREENING_QUESTION_ENABLE, false);
        workRequest = new OneTimeWorkRequest.Builder(MultiClassificationWorker.class)
                .addTag("gradingWorkRequest")
                .build();
    }

    private void initView(View view) {
        btnExport = view.findViewById(R.id.btnExport);
        btnGradeImage = view.findViewById(R.id.btnGradeImage);
        cvGradingLoader = view.findViewById(R.id.cvGradingLoader);
        cvExportLoader = view.findViewById(R.id.cvExportLoader);
        tvGradingProgress = view.findViewById(R.id.tvGradingProgress);
        sAutomaticGrading = view.findViewById(R.id.sAutomaticGrading);
        sDebug = view.findViewById(R.id.sDebug);
        sScreeningQuestion = view.findViewById(R.id.sScreeningQuestion);
        sAdjudication = view.findViewById(R.id.sAdjudication);
        llAutomaticGradingSetting = view.findViewById(R.id.llAutomaticGradingSetting);
        llDebugSetting = view.findViewById(R.id.llDebugSetting);
        llGradeImageSetting = view.findViewById(R.id.llGradeImageSetting);
        llScreeningQuestion = view.findViewById(R.id.llScreeningQuestion);
        llAdjudicationSetting = view.findViewById(R.id.llAdjudicationSetting);
    }

    private void initFunctionality() {
        loadQueues();
        if(isAdminModeEnable){
            llAutomaticGradingSetting.setVisibility(View.VISIBLE);
            llDebugSetting.setVisibility(View.VISIBLE);
            if(isAutomaticGradingEnable){
                llGradeImageSetting.setVisibility(View.GONE);
                llAdjudicationSetting.setVisibility(View.VISIBLE);
            }
            else {
                llGradeImageSetting.setVisibility(View.VISIBLE);
                llAdjudicationSetting.setVisibility(View.GONE);
            }
        }
        else{
            llAutomaticGradingSetting.setVisibility(View.GONE);
            llDebugSetting.setVisibility(View.GONE);
            llGradeImageSetting.setVisibility(View.GONE);
            llAdjudicationSetting.setVisibility(View.GONE);
        }
        sDebug.setChecked(isDebugEnable);
        sAutomaticGrading.setChecked(isAutomaticGradingEnable);
        sScreeningQuestion.setChecked(isScreeningQuestionEnable);
        sAdjudication.setChecked(isAdjudicationEnable);

        if(isWorkRunning()){
            cvGradingLoader.setVisibility(View.VISIBLE);
            btnGradeImage.setVisibility(View.GONE);
        }
        else{
            cvGradingLoader.setVisibility(View.GONE);
            btnGradeImage.setVisibility(View.VISIBLE);
        }
    }

    private void loadQueues(){
        String existingData = AppPreference.getInstance(getActivity()).getString(PrefKey.GRADING_QUEUE);
        if(existingData != null){
            existingQueue = new Gson().fromJson(existingData, new TypeToken<ArrayList<ClassificationQueue>>(){}.getType());
            btnGradeImage.setText(String.format("%s (%d)", getResources().getString(R.string.grade_image), existingQueue.size()));
            tvGradingProgress.setText(String.format("(%d) test left", existingQueue.size()));
        }
    }

    private void initListener() {

        btnExport.setOnClickListener(v -> alertConfirmExport());

        btnGradeImage.setOnClickListener(v -> {
            //Log.d("GRADE_QUEUE", existingData);
            if(existingQueue.size() > 0) {
                alertConfirmGrading();
            }
            else {
                Toast.makeText(getActivity(), "No image found in the queue to be graded!", Toast.LENGTH_SHORT).show();
            }
        });

        sAutomaticGrading.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isAutomaticGradingEnable = isChecked;
            AppPreference.getInstance(getActivity()).setBoolean(PrefKey.AUTOMATIC_GRADING_ENABLE, isChecked);
            if(!isChecked){
                llGradeImageSetting.setVisibility(View.VISIBLE);
                llAdjudicationSetting.setVisibility(View.GONE);
            }
            else{
                llGradeImageSetting.setVisibility(View.GONE);
                llAdjudicationSetting.setVisibility(View.VISIBLE);
            }
        });

        sDebug.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isDebugEnable = isChecked;
            AppPreference.getInstance(getActivity()).setBoolean(PrefKey.IS_DEBUGGABLE, isChecked);
        });

        sScreeningQuestion.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isScreeningQuestionEnable = isChecked;
            AppPreference.getInstance(getActivity()).setBoolean(PrefKey.IS_SCREENING_QUESTION_ENABLE, isChecked);
        });

        sAdjudication.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isAdjudicationEnable = isChecked;
            AppPreference.getInstance(getActivity()).setBoolean(PrefKey.ADJUDICATION_ENABLE, isChecked);
        });

        cvGradingLoader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //alertCancelGrading();
            }
        });
    }

    public void loadDataAndExport() {
        AssessmentLoader loader = new AssessmentLoader(getActivity());
        loader.setDbLoaderInterface(new DbLoaderInterface() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onFinished(Object object) {
                if (object != null) {
                    List<Assessment> dataList = (List<Assessment>) object;
                    if(dataList.isEmpty()){
                        Toast.makeText(getActivity(), "No assessment found to be exported!", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        saveToCsv(dataList);
                    }
                }
                else {
                    //llNoData.setVisibility(View.VISIBLE);
                }
            }
        });
        loader.execute(DaoHelper.FETCH_ALL);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void saveToCsv(List<Assessment> assessments) {
        cvExportLoader.setVisibility(View.VISIBLE);
        btnExport.setVisibility(View.GONE);

        String root;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString();
        }
        else{
            root = Environment.getExternalStorageDirectory().toString();
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd").format(new Date());
        File sourceLogImageFolder = AppUtils.getLogImageOutputDirectory(getActivity());
        File destinationLogImageFolder = AppUtils.getExportLogImageOutputDirectory(getActivity(),timeStamp);
        File sourceImageFolder = AppUtils.getImageOutputDirectory(getActivity());
        File destinationImageFolder = AppUtils.getExportImageOutputDirectory(getActivity(), timeStamp);

        File sourceRejectedImageFolder = AppUtils.getRejectedImageOutputDirectory(getActivity());
        File destinationRejectedImageFolder = AppUtils.getExportRejectedImageOutputDirectory(getActivity(), timeStamp);

        File myDir = new File(root + "/TTScreener/"+timeStamp);
        myDir.mkdirs();
        String fileName1 = "assessment-" + timeStamp + ".csv";
        String fileName2 = "assessment-" + timeStamp + "-binary.csv";
        File file1 = new File(myDir, fileName1);
        File file2 = new File(myDir, fileName2);
        if (file1.exists()) file1.delete();
        if (file2.exists()) file2.delete();
        Set<String> gradedImageNamesSet = new HashSet<>();
        try {
            CSVWriter csvWrite1 = new CSVWriter(new FileWriter(file1));
            CSVWriter csvWrite2 = new CSVWriter(new FileWriter(file2));
            String[] csvHeader1 = {"tt_id","consent", "assessment_date", "right_eye_result","right_eye_image","right_eye_classification_start_time","right_eye_classification_end_time", "left_eye_result","left_eye_image","left_eye_classification_start_time","left_eye_classification_end_time", "have_tt_on_right_eye", "have_tt_on_left_eye", "have_tt_on_right_eye_confirm", "have_tt_on_left_eye_confirm", "gps_coordinate"};
            String[] csvHeader2 = {"tt_id","eye", "photo_name", "algorithm_grade", "field_grade", "agree"};
            List<String[]> csvDataList1 = new ArrayList<>();
            List<String[]> csvDataList2 = new ArrayList<>();
            csvDataList1.add(csvHeader1);
            csvDataList2.add(csvHeader2);
            for(Assessment assessment : assessments){
                String[] csvRecord1 = {
                        assessment.getTt_tracker_id(),
                        assessment.getConsent(),
                        assessment.getAssessment_ended(),
                        assessment.getRight_eye_result(),
                        assessment.getRight_image_name(),
                        assessment.getRight_eye_classification_started(),
                        assessment.getRight_eye_classification_ended(),
                        assessment.getLeft_eye_result(),
                        assessment.getLeft_image_name(),
                        assessment.getLeft_eye_classification_started(),
                        assessment.getLeft_eye_classification_ended(),
                        assessment.getHave_tt_on_right_eye(),
                        assessment.getHave_tt_on_left_eye(),
                        assessment.getHave_tt_on_right_eye_confirm(),
                        assessment.getHave_tt_on_left_eye_confirm(),
                        assessment.getGps_coordinate(),
                };

                // calculate agree of OS
                String osAgree = "1";
                if (assessment.getLeft_eye_result() != null && assessment.getLeft_eye_result().contains("TT")) {
                    if (!assessment.getHave_tt_on_left_eye().toLowerCase().contains("yes")) {
                        osAgree = "0";
                    }
                } else {
                    if (assessment.getHave_tt_on_left_eye().toLowerCase().contains("yes")) {
                        osAgree = "0";
                    }
                }

                String[] csvRecordLeftEye = {
                        assessment.getTt_tracker_id(),
                        "OS",
                        assessment.getLeft_image_name(),
                        assessment.getLeft_eye_result() != null && assessment.getLeft_eye_result().contains("TT") ? "1" : "0",
                        assessment.getHave_tt_on_left_eye().toLowerCase().contains("yes") ? "1" : "0",
                        osAgree
                };

                // calculate agree of OD
                String odAgree = "1";
                if (assessment.getRight_eye_result() != null && assessment.getRight_eye_result().contains("TT")) {
                    if (!assessment.getHave_tt_on_right_eye().toLowerCase().contains("yes")) {
                        odAgree = "0";
                    }
                } else {
                    if (assessment.getHave_tt_on_right_eye().toLowerCase().contains("yes")) {
                        odAgree = "0";
                    }
                }

                String[] csvRecordRightEye = {
                        assessment.getTt_tracker_id(),
                        "OD",
                        assessment.getRight_image_name(),
                        assessment.getRight_eye_result() != null && assessment.getRight_eye_result().contains("TT") ? "1" : "0",
                        assessment.getHave_tt_on_right_eye().toLowerCase().contains("yes") ? "1" : "0",
                        odAgree
                };

                csvDataList1.add(csvRecord1);
                csvDataList2.add(csvRecordLeftEye);
                csvDataList2.add(csvRecordRightEye);

                gradedImageNamesSet.add(assessment.getLeft_image_name());
                gradedImageNamesSet.add(assessment.getRight_image_name());
            }
            csvWrite1.writeAll(csvDataList1);
            csvWrite2.writeAll(csvDataList2);
            csvWrite1.close();
            csvWrite2.close();

            Toast.makeText(getActivity(), "File exported to "+ myDir.getAbsolutePath(), Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
        }

        File[] gradedFiles = sourceImageFolder.listFiles();
        File[] rejectedFiles = sourceRejectedImageFolder.listFiles();

        for (File gradedFile : gradedFiles) {
            if (gradedImageNamesSet.contains(gradedFile.getName())) {
                try {
                    copyDirectoryOneLocationToAnotherLocation(gradedFile,destinationImageFolder);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println(e.getLocalizedMessage());
                }
                deleteRecursive(gradedFile);
            }
        }
        for (File rejectedFile : rejectedFiles) {
            if (gradedImageNamesSet.contains(rejectedFile.getName())) {
                try {
                    copyDirectoryOneLocationToAnotherLocation(rejectedFile,destinationRejectedImageFolder);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println(e.getLocalizedMessage());
                }
                deleteRecursive(rejectedFile);
            }
        }

        try {
            copyDirectoryOneLocationToAnotherLocation(sourceLogImageFolder,destinationLogImageFolder);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getLocalizedMessage());
        }
        deleteRecursive(sourceLogImageFolder);

        cvExportLoader.setVisibility(View.GONE);
        btnExport.setVisibility(View.VISIBLE);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public void alertCancelGrading() {
        AlertDialog.Builder bld = new AlertDialog.Builder(getActivity());
        bld.setTitle("Confirmation to cancel grading images");
        bld.setMessage("Are you sure you want to proceed?");
        bld.setCancelable(false);
        bld.setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss());
        bld.setPositiveButton("Yes", (dialog, which) -> {
            dialog.dismiss();
            WorkManager.getInstance(requireActivity()).cancelAllWork();
            cvGradingLoader.setVisibility(View.GONE);
            btnGradeImage.setVisibility(View.VISIBLE);;
        });
        bld.create().show();
    }

    public void alertConfirmGrading() {
        AlertDialog.Builder bld = new AlertDialog.Builder(getActivity());
        bld.setTitle("Confirmation to start grading images");
        bld.setMessage("There are currently "+existingQueue.size()+" images to be processed. Are you sure you want to proceed?");
        bld.setCancelable(false);
        bld.setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss());
        bld.setPositiveButton("Yes", (dialog, which) -> {
            dialog.dismiss();
            WorkManager.getInstance(requireActivity()).enqueueUniqueWork(
                    "gradingWork",
                    ExistingWorkPolicy.APPEND, // You can choose a policy that suits your needs
                    workRequest
            );
            cvGradingLoader.setVisibility(View.VISIBLE);
            btnGradeImage.setVisibility(View.GONE);
        });
        bld.create().show();
    }
    public void alertConfirmExport() {
        AlertDialog.Builder bld = new AlertDialog.Builder(getActivity());
        bld.setTitle("Confirmation to start exporting");
        bld.setMessage("Are you sure you want to proceed?");
        bld.setCancelable(false);
        bld.setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss());
        bld.setPositiveButton("Yes", (dialog, which) -> {
            dialog.dismiss();
            loadDataAndExport();
        });
        bld.create().show();
    }

    private boolean isWorkRunning() {
         // Get the WorkManager instance
        WorkManager workManager = WorkManager.getInstance(MyApplication.getContext());
        // Get a list of WorkInfo objects for the unique work request
        List<WorkInfo> workInfoList = new ArrayList<>();
        try {
            workInfoList.addAll(workManager.getWorkInfosForUniqueWork("gradingWork").get());
        } catch (ExecutionException e) {
            System.out.println(e.getLocalizedMessage());
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            System.out.println(e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
        // Check if any of the work requests are running
        boolean isRunning = false;
        for (WorkInfo workInfo : workInfoList) {
            if (workInfo.getState() == WorkInfo.State.RUNNING) {
                isRunning = true;
                break;
            }
        }
        return isRunning;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister the progress receiver to avoid memory leaks
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(progressReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register the progress receiver
        IntentFilter filter = new IntentFilter("WORK_PROGRESS_ACTION");
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(progressReceiver, filter);
    }

    private final BroadcastReceiver progressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("WORK_PROGRESS_ACTION".equals(intent.getAction())) {
                int progress = intent.getIntExtra("progress", 0);
                if (progress == 100){
                    cvGradingLoader.setVisibility(View.GONE);
                    btnGradeImage.setVisibility(View.VISIBLE);
                    loadQueues();
                }
                else {
                    cvGradingLoader.setVisibility(View.VISIBLE);
                    btnGradeImage.setVisibility(View.GONE);
                }
            }
        }
    };
}