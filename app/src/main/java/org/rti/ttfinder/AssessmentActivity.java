package org.rti.ttfinder;

import static org.rti.ttfinder.data.AppConstants.FORM_ID;
import static org.rti.ttfinder.data.AppConstants.PATIENT_ID;
import static org.rti.ttfinder.data.AppConstants.VALUE;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.rti.ttfinder.data.entity.Assessment;
import org.rti.ttfinder.data.helper.DaoHelper;
import org.rti.ttfinder.data.helper.DbLoaderInterface;
import org.rti.ttfinder.data.loader.AssessmentLoader;
import org.rti.ttfinder.data.preference.AppPreference;
import org.rti.ttfinder.data.preference.PrefKey;
import org.rti.ttfinder.enums.Eye;
import org.rti.ttfinder.enums.TTAnswer;
import org.rti.ttfinder.listeners.TTQuestionDialogActionListener;
import org.rti.ttfinder.models.ClassificationModel;
import org.rti.ttfinder.models.ClassificationQueue;
import org.rti.ttfinder.ui.AdminLoginDialogFragment;
import org.rti.ttfinder.ui.TTQuestionDialogFragment;
import org.rti.ttfinder.utils.AppUtils;
import org.rti.ttfinder.utils.MultiClassificationWorker;
import org.rti.ttfinder.utils.SingleClassificationWorker;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.ArrayList;
import java.util.List;

public class AssessmentActivity extends AppCompatActivity implements TTQuestionDialogActionListener {
    private Button btnLeftEye, btnRightEye, btnScamQRCode, btnSave, btnGps;
    private LinearLayout llScreeningQuestion;
    private TextInputLayout llLEftEyeQuestion, llRightEyeQuestion;
    private RadioGroup rgRightEye, rgLeftEye;
    private TextInputLayout til_ttid;
    private EditText et_tt_id;
    private CheckBox cbConsent;
    private ImageView ivLeftEyeResult, ivRightEyeResult;
    private String classifiedGrade;
    private boolean ttIdValidated = false;
    public static int LEFT_EYE_RESULT = 1;
    public static int RIGHT_EYE_RESULT = 2;
    public static int SCAN_RESULT = 3;
    private ClassificationModel leftEyeModel, rightEyeModel;
    private long startTimeForImageProcessing;

    private String formId;
    private String patientId;
    private String rightEyeTTValue = "";
    private String leftEyeTTValue = "";
    private String confirmedRightEyeTTValue = "";
    private String confirmedLeftEyeTTValue = "";

    private Location mLocation;
    private static final int REQUEST_LOCATION = 1;
    private LocationManager locationManager;
    private boolean isAdminModeEnabled = true;
    private boolean isAutomaticGradingEnabled = true;
    private boolean isScreeningQuestionEnable = false;
    private boolean isAdjudicationEnable = false;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assessment);

        initVariable();
        initView();
        initFunctionality();
        initListener();

    }

    private void initVariable() {

        startTimeForImageProcessing = System.currentTimeMillis();

        if(getIntent() != null) {
            formId = getIntent().getStringExtra(FORM_ID);
            patientId = getIntent().getStringExtra(PATIENT_ID);
        }
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        isAutomaticGradingEnabled = AppPreference.getInstance(getApplicationContext()).getBoolean(PrefKey.AUTOMATIC_GRADING_ENABLE, true);
        isScreeningQuestionEnable = AppPreference.getInstance(getApplicationContext()).getBoolean(PrefKey.IS_SCREENING_QUESTION_ENABLE, false);
        isAdjudicationEnable = AppPreference.getInstance(getApplicationContext()).getBoolean(PrefKey.ADJUDICATION_ENABLE, false);
        isAdminModeEnabled = AppPreference.getInstance(getApplicationContext()).getBoolean(PrefKey.IS_ADMIN, false);

    }

    private void initView(){
        llScreeningQuestion = findViewById(R.id.llScreeningQuestion);
        llRightEyeQuestion = findViewById(R.id.llRightEyeQuestion);
        llLEftEyeQuestion = findViewById(R.id.llLEftEyeQuestion);
        rgRightEye = findViewById(R.id.rgRightEye);
        rgLeftEye = findViewById(R.id.rgLeftEye);
        til_ttid = findViewById(R.id.til_ttid);
        et_tt_id = findViewById(R.id.et_tt_id);
        cbConsent = findViewById(R.id.cbConsent);
        ivLeftEyeResult = findViewById(R.id.ivLeftEyeResult);
        ivRightEyeResult = findViewById(R.id.ivRightEyeResult);
        btnLeftEye = findViewById(R.id.btnLeftEye);
        btnRightEye = findViewById(R.id.btnRightEye);
        btnScamQRCode = findViewById(R.id.btnScanQr);
        btnGps = findViewById(R.id.btnGps);
        btnSave = findViewById(R.id.btn_save);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void initFunctionality() {

        Log.d("FromODKCollect", "patientId===> "+patientId);
        Log.d("FromODKCollect", "formId===> "+formId);

        if(getIntent() != null){
            et_tt_id.setText(patientId);
            isExistInDataBase(patientId);
        }

        if(isScreeningQuestionEnable){
            llScreeningQuestion.setVisibility(View.VISIBLE);
            llRightEyeQuestion.setVisibility(View.VISIBLE);
            llLEftEyeQuestion.setVisibility(View.VISIBLE);
        }
        else {
            llScreeningQuestion.setVisibility(View.GONE);
            llRightEyeQuestion.setVisibility(View.GONE);
            llLEftEyeQuestion.setVisibility(View.GONE);
        }
        checkLocation(); //check whether location service is enable or not in your  phone
    }

    private void initListener(){

        cbConsent.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    cbConsent.setText(R.string.confirmed);
                }
                else{
                    cbConsent.setText(R.string.not_confirmed);
                }
            }
        });

        et_tt_id.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void afterTextChanged(Editable s) {
                isExistInDataBase(s.toString());
            }
        });

        rgRightEye.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton radioButton = (RadioButton)group.findViewById(checkedId);
                rightEyeTTValue = radioButton.getText().toString();
            }
        });

        rgLeftEye.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton radioButton = (RadioButton)group.findViewById(checkedId);
                leftEyeTTValue = radioButton.getText().toString();
            }
        });

        btnRightEye.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermission(false);
            }
        });

        btnLeftEye.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermission(true);
            }
        });

        btnScamQRCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AssessmentActivity.this, ScanActivity.class);
                startActivityForResult(intent,SCAN_RESULT);
            }
        });

        btnGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isLocationEnabled()) {
                    showAlert();
                } else {
                    getLocation();
                }
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ttId = et_tt_id.getText().toString();

                if(TextUtils.isEmpty(ttId)){
                    til_ttid.setError(getResources().getString(R.string.tt_id_empty_error_msg));
                }
                else if(!ttIdValidated){
                    til_ttid.setError(getResources().getString(R.string.tt_id_exist_error_msg));
                }
                else if(!cbConsent.isChecked()){
                    cbConsent.setError(getResources().getString(R.string.consent_error_msg));
                }
                else if(rightEyeModel == null || (isAutomaticGradingEnabled && rightEyeModel.getClassificationResult() == null)){
                    btnRightEye.setBackgroundColor(getResources().getColor(R.color.colorRed));
                }
                else if(leftEyeModel == null || (isAutomaticGradingEnabled && leftEyeModel.getClassificationResult() == null)){
                    btnLeftEye.setBackgroundColor(getResources().getColor(R.color.colorRed));
                }
                else if(isAutomaticGradingEnabled && isScreeningQuestionEnable && rightEyeTTValue.isEmpty()){
                    llRightEyeQuestion.setError(getResources().getText(R.string.answer_validation_msg));
                }
                else if(isAutomaticGradingEnabled && isScreeningQuestionEnable && leftEyeTTValue.isEmpty()){
                    llLEftEyeQuestion.setError(getResources().getText(R.string.answer_validation_msg));
                }
                else{
                    Assessment assessment = new Assessment(
                        cbConsent.isChecked() ? getResources().getString(R.string.confirmed):getResources().getString(R.string.not_confirmed),
                        AppUtils.getDate(startTimeForImageProcessing),
                        AppUtils.getDate(System.currentTimeMillis()),
                        leftEyeModel.getStartedTime(),
                        leftEyeModel.getEndedTime(),
                        rightEyeModel.getStartedTime(),
                        rightEyeModel.getEndedTime(),
                        ttId,
                        rightEyeModel.getClassificationResult(),
                        leftEyeModel.getClassificationResult(),
                        rightEyeModel.getImageName(),
                        leftEyeModel.getImageName(),
                        leftEyeModel.getLogFileName(),
                        rightEyeModel.getLogFileName(),
                        rightEyeTTValue,
                        leftEyeTTValue,
                        confirmedRightEyeTTValue,
                        confirmedLeftEyeTTValue,
                        mLocation != null ? String.valueOf(mLocation.getLatitude()) + ','+ mLocation.getLongitude() : "0.0, 0.0"
                    );

                    if(isAutomaticGradingEnabled){
                        saveData(assessment);
                    }
                    else{
                        //pushToQueue(assessment);
                        startProcessingInstant(assessment);
                    }
                    if(getIntent() != null){
                        returnValue();
                    }
                }
            }
        });
    }

    private void startProcessingInstant(Assessment assessment){
        ClassificationQueue classificationQueue = new ClassificationQueue(assessment,leftEyeModel, rightEyeModel);
        Data inputData = new Data.Builder()
                .putString("classificationQueueData", new Gson().toJson(classificationQueue))
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SingleClassificationWorker.class)
                .setInputData(inputData)
                .addTag("gradingWorkRequest")
                .build();

        WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork(
                "gradingWork",
                ExistingWorkPolicy.APPEND, // You can choose a policy that suits your needs
                workRequest
        );
        finish();
    }

    private void resetAssessment(){
        et_tt_id.getText().clear();
        cbConsent.setChecked(false);

    }
    private void saveData(Assessment assessment){
        ArrayList<Assessment> dataList = new ArrayList<>();
        dataList.add(assessment); // adding single data here
        AssessmentLoader loader = new AssessmentLoader(AssessmentActivity.this);
        loader.setDbLoaderInterface(new DbLoaderInterface() {
            @Override
            public void onFinished(Object object) {
                finish();
                Log.d("Added","Assessment added");
            }
        });
        loader.execute(DaoHelper.INSERT_ALL, dataList);

    }

    public void returnValue() {
        //String result1 =  "Left Eye :: "+leftEyeModel.getClassificationResult();
        //String result2 =  "Right Eye :: "+rightEyeModel.getClassificationResult();
        String result1 =  "Left Eye :: Complete";
        String result2 =  "Right Eye :: Complete";
        Intent intent = new Intent();
        intent.putExtra(VALUE, result1 + result2);
        setResult(RESULT_OK, intent);
        finish();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void isExistInDataBase(String patientId){
        if(isAutomaticGradingEnabled){
            AssessmentLoader loader = new AssessmentLoader(AssessmentActivity.this);
            loader.setDbLoaderInterface(new DbLoaderInterface() {
                @Override
                public void onFinished(Object object) {
                    if (object != null) {
                        int isExist = (int) object;
                        if(isExist > 0){
                            ttIdValidated = false;
                            til_ttid.setError(getResources().getString(R.string.tt_id_exist_error_msg));
                        }
                        else{
                            ttIdValidated = true;
                            til_ttid.setError(null);
                        }
                    }
                }
            });
            loader.execute(DaoHelper.FETCH_SINGLE, patientId);
        }
        else{
            boolean isExistOnSHaredPref = AppPreference.getInstance(getApplicationContext()).isExistTTID(patientId);
            ttIdValidated = !isExistOnSHaredPref;
            til_ttid.setError(isExistOnSHaredPref ? getResources().getString(R.string.tt_id_exist_error_msg) : null);
        }
    }

    private void showTTQuestionDialog(String eyeType, String formResult, String algoResult){
        TTQuestionDialogFragment fragment = TTQuestionDialogFragment.newInstance(eyeType, formResult, algoResult);
        fragment.setListener(AssessmentActivity.this);
        fragment.setCancelable(false);
        fragment.show(getSupportFragmentManager(),"TTQuestionDialog");
    }
    public void checkPermission(boolean isLeft){
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // check if all permissions are granted
                        if (report.areAllPermissionsGranted()) {
                            // do you work now
                            openCameraActivity(isLeft);
                        }

                        // check for permanent denial of any permission
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            // permission is denied permenantly, navigate user to app settings
                        }
                    }
                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                })
                .onSameThread()
                .check();
    }

    private void openCameraActivity(boolean isLeft){
        String ttId = et_tt_id.getText().toString();
        if(TextUtils.isEmpty(ttId)){
            til_ttid.setError(getResources().getString(R.string.tt_id_empty_error_msg));
        }
        else if(!ttIdValidated){
            til_ttid.setError(getResources().getString(R.string.tt_id_exist_error_msg));
        }
        else {
            Intent intent = new Intent(AssessmentActivity.this, org.rti.ttfinder.CameraActivity.class);
            intent.putExtra("eyeType", isLeft ? "Left" : "Right");
            intent.putExtra("ttId", ttId);
            startActivityForResult(intent, isLeft ? LEFT_EYE_RESULT : RIGHT_EYE_RESULT);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if(requestCode == LEFT_EYE_RESULT){
                String intentData = data.getStringExtra("result");
                leftEyeModel = new Gson().fromJson(intentData, ClassificationModel.class);
                Log.d("ResultImage", intentData);
                btnLeftEye.setText(leftEyeModel.getImageName());
                if(isAutomaticGradingEnabled){
                    if(leftEyeModel.isSuccess()){
                        if(isScreeningQuestionEnable && isAdminModeEnabled){
                            boolean hasTTByAlgorithm = leftEyeModel.getClassificationResult().contains("TT");
                            boolean hasTTByUser = leftEyeTTValue.equals(TTAnswer.YES.label);
                            boolean isNotMismatched = ((!hasTTByAlgorithm || hasTTByUser) && (hasTTByAlgorithm || !hasTTByUser));
                            if(!isNotMismatched){
                                ivLeftEyeResult.setImageResource(R.drawable.grading_failed);
                                btnLeftEye.setBackgroundColor(getResources().getColor(R.color.colorRed));
                                if(isAdjudicationEnable){
                                    showTTQuestionDialog(Eye.LEFT.name(), leftEyeTTValue, hasTTByAlgorithm ? "YES" : "NO");
                                }
                            }
                            else{
                                ivLeftEyeResult.setImageResource(R.drawable.grading_success);
                                btnLeftEye.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                            }
                        }
                        else{
                            ivLeftEyeResult.setImageResource(R.drawable.grading_success);
                            btnLeftEye.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                        }
                    }
                    else{
                        ivLeftEyeResult.setImageResource(R.drawable.grading_failed);
                        btnLeftEye.setBackgroundColor(getResources().getColor(R.color.colorRed));
                        if(isScreeningQuestionEnable && isAdjudicationEnable && isAdminModeEnabled){
                            showTTQuestionDialog(Eye.LEFT.name(), leftEyeTTValue, "REJECTED");
                        }
                    }
                }
            }
            else if(requestCode == RIGHT_EYE_RESULT){
                String intentData = data.getStringExtra("result");
                Log.d("ResultImage", intentData);
                rightEyeModel = new Gson().fromJson(intentData, ClassificationModel.class);
                btnRightEye.setText(rightEyeModel.getImageName());
                if(isAutomaticGradingEnabled){
                    if(rightEyeModel.isSuccess()){
                        if(isScreeningQuestionEnable && isAdminModeEnabled){
                            boolean hasTTByAlgorithm = rightEyeModel.getClassificationResult().contains("TT");
                            boolean hasTTByUser = rightEyeTTValue.equals(TTAnswer.YES.label);
                            boolean isNotMismatched = ((!hasTTByAlgorithm || hasTTByUser) && (hasTTByAlgorithm || !hasTTByUser));
                            if(!isNotMismatched){
                                ivRightEyeResult.setImageResource(R.drawable.grading_failed);
                                btnRightEye.setBackgroundColor(getResources().getColor(R.color.colorRed));
                                if(isAdjudicationEnable){
                                    showTTQuestionDialog(Eye.RIGHT.name(), rightEyeTTValue, hasTTByAlgorithm ? "YES" : "NO");
                                }
                            }
                            else{
                                ivRightEyeResult.setImageResource(R.drawable.grading_success);
                                btnRightEye.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                            }
                        }
                        else{
                            ivRightEyeResult.setImageResource(R.drawable.grading_success);
                            btnRightEye.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                        }
                    }
                    else{
                        ivRightEyeResult.setImageResource(R.drawable.grading_failed);
                        btnRightEye.setBackgroundColor(getResources().getColor(R.color.colorRed));
                        if(isScreeningQuestionEnable && isAdjudicationEnable && isAdminModeEnabled){
                            showTTQuestionDialog(Eye.RIGHT.name(), rightEyeTTValue, "REJECTED");
                        }
                    }
                }
            }
            else if(requestCode == SCAN_RESULT){
                String intentData = data.getStringExtra("scan_result");
                et_tt_id.setText(intentData);
            }
        }
    }
    private void checkLocation() {
        if(!isLocationEnabled())
            showAlert();
    }
    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Enable Location")
                .setMessage("Your Locations Settings is set to 'Off'.\nPlease Enable Location to " +
                        "use this app")
                .setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    }
                });
        dialog.show();
    }
    private boolean isLocationEnabled() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(AssessmentActivity.this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(AssessmentActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            Location locationGPS = getLastKnownLocation();
            if (locationGPS != null) {
                double lat = locationGPS.getLatitude();
                double lon = locationGPS.getLongitude();
                mLocation = locationGPS;
                btnGps.setText(mLocation.getLatitude()+ " , "+ mLocation.getLongitude());
                btnGps.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            } else {
                Toast.makeText(this, "Unable to find location.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Location getLastKnownLocation() {
        locationManager = (LocationManager)getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            @SuppressLint("MissingPermission") Location l = locationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }
    @Override
    public void onPressDone(String answer, String eye) {
        if(eye.equals(Eye.LEFT.name())){
            confirmedLeftEyeTTValue = answer;
        }
        else{
            confirmedRightEyeTTValue = answer;
        }
    }
}