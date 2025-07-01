package org.rti.ttfinder.ui.assessment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.rti.ttfinder.AssessmentActivity;
import org.rti.ttfinder.R;
import org.rti.ttfinder.adapters.AssessmentAdapter;
import org.rti.ttfinder.data.entity.Assessment;
import org.rti.ttfinder.data.helper.DaoHelper;
import org.rti.ttfinder.data.helper.DbLoaderInterface;
import org.rti.ttfinder.data.loader.AssessmentLoader;
import org.rti.ttfinder.data.preference.AppPreference;
import org.rti.ttfinder.data.preference.PrefKey;
import org.rti.ttfinder.listeners.RecyclerTouchListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.rti.ttfinder.utils.AppUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AssessmentFragment extends Fragment {

    private FloatingActionButton fbAdd, fabDelete;
    private TextView tvEmpty;
    private ArrayList<Assessment> dataList;
    private RecyclerView rvAssessment;
    private AssessmentAdapter mAdapter;
    private RecyclerTouchListener touchListener;
    private boolean isAdmin = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_assessment, container, false);
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
        dataList = new ArrayList<>();
        isAdmin = AppPreference.getInstance(getActivity()).getBoolean(PrefKey.IS_ADMIN, false);
    }

    private void initView(View view) {
        tvEmpty = view.findViewById(R.id.tvEmpty);
        fbAdd = view.findViewById(R.id.fbAdd);
        fabDelete = view.findViewById(R.id.fbDelete);
        rvAssessment = view.findViewById(R.id.rvAssessment);

        rvAssessment.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rvAssessment.setLayoutManager(linearLayoutManager);

        if(isAdmin){
            fabDelete.setVisibility(View.VISIBLE);
        }
        else{
            fabDelete.setVisibility(View.GONE);
        }

    }

    private void initFunctionality() {
        mAdapter = new AssessmentAdapter(dataList,getActivity());
        rvAssessment.setAdapter(mAdapter);
    }

    private void initListener() {

        fbAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AssessmentActivity.class);
                startActivity(intent);
            }
        });

        if(isAdmin){
            enableRecyclerViewTouchListener();
        }

        fabDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmationDialog();
            }
        });
    }

    private void confirmationDialog(){
        new AlertDialog.Builder(getActivity())
                .setTitle("Delete All Assessment")
                .setMessage("Are you sure you want to delete all assessment?")
                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        for(Assessment assessment : dataList){
                            deleteFromDb(assessment);
                        }
                    }
                })
                .setCancelable(false)
                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void enableRecyclerViewTouchListener(){
        touchListener = new RecyclerTouchListener(getActivity(),rvAssessment);

        touchListener
                .setClickable(new RecyclerTouchListener.OnRowClickListener() {
                    @Override
                    public void onRowClicked(int position) {
                        //Toast.makeText(getActivity(),dataList.get(position).getName(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onIndependentViewClicked(int independentViewID, int position) {

                    }
                })
                .setSwipeOptionViews(R.id.delete_task)
                .setSwipeable(R.id.rowFG, R.id.rowBG, new RecyclerTouchListener.OnSwipeOptionsClickListener() {
                    @Override
                    public void onSwipeOptionClicked(int viewID, int position) {
                        switch (viewID){
                            case R.id.delete_task:
                                //dataList.remove(position);
                                Assessment assessment = dataList.get(position);
                                deleteFromDb(assessment);
                                //Toast.makeText(getActivity(),"Delete Not Available",Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                });

        rvAssessment.addOnItemTouchListener(touchListener);
    }

    private void deleteFromDb(Assessment assessment){
        System.out.println(new Gson().toJson(assessment));
        File sourceLogImageFolder = AppUtils.getLogImageOutputDirectory(getActivity());
        File sourceImageFolder = AppUtils.getImageOutputDirectory(getActivity());

        try {
            deleteLogRecursive(sourceLogImageFolder, assessment);
            deleteImageRecursive(sourceImageFolder, assessment);
        }catch (Exception e){
            //System.out.println(e.printStackTrace());
            System.out.println(e.getLocalizedMessage());
        }


        AssessmentLoader loader = new AssessmentLoader(getActivity());
        loader.setDbLoaderInterface(new DbLoaderInterface() {
            @Override
            public void onFinished(Object object) {
                loadData();
            }
        });
        loader.execute(DaoHelper.DELETE,assessment);
    }

    public static void deleteLogRecursive(File fileOrDirectory, Assessment assessment) {

        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                if(child.getName().equals(assessment.getLeft_image_log_file_name()) || child.getName().equals(assessment.getRight_image_log_file_name()))
                    deleteLogRecursive(child, assessment);
            }
        }

        if(fileOrDirectory.getName().equals(assessment.getLeft_image_log_file_name()) || fileOrDirectory.getName().equals(assessment.getRight_image_log_file_name())) {
            fileOrDirectory.delete();
        }
    }

    public static void deleteImageRecursive(File fileOrDirectory, Assessment assessment) {

        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                if(child.getName().equals(assessment.getLeft_image_name()) || child.getName().equals(assessment.getRight_image_name()))
                    deleteImageRecursive(child, assessment);
            }
        }
        //System.out.println(fileOrDirectory.getName() + " <=> "+assessment.getLeft_image_name());
        if(fileOrDirectory.getName().equals(assessment.getLeft_image_name()) || fileOrDirectory.getName().equals(assessment.getRight_image_name())) {
            fileOrDirectory.delete();
        }
    }

    private void loadData() {

        AssessmentLoader loader = new AssessmentLoader(getActivity());
        loader.setDbLoaderInterface(new DbLoaderInterface() {
            @Override
            public void onFinished(Object object) {
                if (object != null) {
                    List<Assessment> assessments = (List<Assessment>) object;
                    dataList.clear();
                    dataList.addAll(assessments);
                    mAdapter.notifyDataSetChanged();
                    if(dataList.isEmpty()){
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                    else {
                        tvEmpty.setVisibility(View.GONE);
                    }
                    Log.d("AssessmentList",new Gson().toJson(dataList));
                }
                else {
                    tvEmpty.setVisibility(View.VISIBLE);
                }
            }
        });
        loader.execute(DaoHelper.FETCH_ALL);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister the progress receiver to avoid memory leaks
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(progressReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
        if(isAdmin) {
            rvAssessment.addOnItemTouchListener(touchListener);
        }

        // Register the progress receiver
        IntentFilter filter = new IntentFilter("WORK_PROGRESS_ACTION");
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(progressReceiver, filter);
    }

    private final BroadcastReceiver progressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("WORK_PROGRESS_ACTION".equals(intent.getAction())) {
                int progress = intent.getIntExtra("progress", 0);
                System.out.println("WORK_PROGRESS_ACTION => "+progress);
                loadData();
            }
        }
    };

    private void pushTestData(){
        String data = "[{\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 0,\n\t\t\"left_image_name\": \"sev_1155_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1155_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1155\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1155_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1155_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1155_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1155_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1155_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1155_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 1,\n\t\t\"left_image_name\": \"sev_1156_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1156_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1156\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1156_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1156_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1156_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1156_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1156_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1156_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 2,\n\t\t\"left_image_name\": \"sev_1157_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1157_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1157\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1157_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1157_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1157_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1157_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1157_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1157_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 3,\n\t\t\"left_image_name\": \"sev_1158_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1158_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1158\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1158_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1158_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1158_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1158_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1158_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1158_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 4,\n\t\t\"left_image_name\": \"sev_1159_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1159_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1159\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1159_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1159_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1159_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1159_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1159_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1159_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 5,\n\t\t\"left_image_name\": \"sev_1160_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1160_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1160\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1160_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1160_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1160_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1160_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1160_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1160_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 6,\n\t\t\"left_image_name\": \"sev_1161_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1161_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1161\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1161_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1161_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1161_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1161_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1161_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1161_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 7,\n\t\t\"left_image_name\": \"sev_1162_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1162_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1162\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1162_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1162_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1162_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1162_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1162_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1162_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 8,\n\t\t\"left_image_name\": \"sev_1163_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1163_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1163\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1163_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1163_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1163_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1163_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1163_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1163_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 9,\n\t\t\"left_image_name\": \"sev_1164_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1164_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1164\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1164_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1164_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1164_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1164_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1164_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1164_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 10,\n\t\t\"left_image_name\": \"sev_1165_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1165_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1165\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1165_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1165_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1165_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1165_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1165_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1165_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 11,\n\t\t\"left_image_name\": \"sev_1166_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1166_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1166\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1166_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1166_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1166_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1166_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1166_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1166_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 12,\n\t\t\"left_image_name\": \"sev_1167_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1167_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1167\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1167_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1167_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1167_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1167_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1167_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1167_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 13,\n\t\t\"left_image_name\": \"sev_1168_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1168_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1168\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1168_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1168_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1168_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1168_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1168_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1168_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 14,\n\t\t\"left_image_name\": \"sev_1169_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1169_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1169\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1169_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1169_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1169_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1169_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1169_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1169_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 15,\n\t\t\"left_image_name\": \"sev_1170_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1170_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1170\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1170_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1170_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1170_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1170_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1170_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1170_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 16,\n\t\t\"left_image_name\": \"sev_1171_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1171_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1171\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1171_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1171_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1171_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1171_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1171_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1171_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 17,\n\t\t\"left_image_name\": \"sev_1172_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1172_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1172\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1172_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1172_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1172_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1172_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1172_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1172_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 18,\n\t\t\"left_image_name\": \"sev_1173_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1173_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1173\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1173_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1173_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1173_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1173_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1173_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1173_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 19,\n\t\t\"left_image_name\": \"sev_1174_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1174_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1174\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1174_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1174_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1174_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1174_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1174_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1174_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 20,\n\t\t\"left_image_name\": \"sev_1175_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1175_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1175\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1175_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1175_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1175_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1175_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1175_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1175_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 21,\n\t\t\"left_image_name\": \"sev_1176_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1176_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1176\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1176_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1176_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1176_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1176_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1176_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1176_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 22,\n\t\t\"left_image_name\": \"sev_1177_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1177_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1177\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1177_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1177_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1177_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1177_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1177_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1177_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 23,\n\t\t\"left_image_name\": \"sev_1178_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1178_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1178\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1178_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1178_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1178_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1178_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1178_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1178_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 24,\n\t\t\"left_image_name\": \"sev_1179_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1179_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1179\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1179_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1179_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1179_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1179_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1179_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1179_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 25,\n\t\t\"left_image_name\": \"sev_1180_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1180_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1180\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1180_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1180_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1180_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1180_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1180_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1180_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 26,\n\t\t\"left_image_name\": \"sev_1181_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1181_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1181\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1181_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1181_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1181_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1181_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1181_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1181_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 27,\n\t\t\"left_image_name\": \"sev_1182_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1182_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1182\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1182_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1182_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1182_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1182_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1182_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1182_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 28,\n\t\t\"left_image_name\": \"sev_1183_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1183_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1183\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1183_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1183_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1183_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1183_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1183_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1183_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}, {\n\t\"assessment\": {\n\t\t\"assesment_started\": \"2023/09/05 00:45:28 AM\",\n\t\t\"assessment_ended\": \"2023/09/05 00:45:53 AM\",\n\t\t\"consent\": \"Confirmed\",\n\t\t\"gps_coordinate\": \"23.8028577,90.3706753\",\n\t\t\"have_tt_on_left_eye\": \"\",\n\t\t\"have_tt_on_left_eye_confirm\": \"\",\n\t\t\"have_tt_on_right_eye\": \"\",\n\t\t\"have_tt_on_right_eye_confirm\": \"\",\n\t\t\"id\": 29,\n\t\t\"left_image_name\": \"sev_1184_Left_20230721115850.jpg\",\n\t\t\"right_image_name\": \"sev_1184_Right_20230721115817.jpg\",\n\t\t\"tt_tracker_id\": \"1184\"\n\t},\n\t\"leftEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1184_Left_20230721115850.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1184_Left_20230721115850.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1184_Left_20230721115850.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t},\n\t\"rightEyeModel\": {\n\t\t\"image\": {\n\t\t\t\"imageName\": \"sev_1184_Right_20230721115817.jpg\",\n\t\t\t\"imagePath\": \"/storage/emulated/0/Documents/TTScreener/Images/sev_1184_Right_20230721115817.jpg\",\n\t\t\t\"isLeft\": false\n\t\t},\n\t\t\"imageName\": \"sev_1184_Right_20230721115817.jpg\",\n\t\t\"isBlurred\": false,\n\t\t\"isSuccess\": false\n\t}\n}]";
        AppPreference.getInstance(getActivity()).setString(PrefKey.GRADING_QUEUE, data);
    }
}