package org.rti.ttfinder.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;

import org.rti.ttfinder.R;
import org.rti.ttfinder.data.AppConstants;
import org.rti.ttfinder.enums.Eye;
import org.rti.ttfinder.listeners.DialogActionListener;
import org.rti.ttfinder.listeners.TTQuestionDialogActionListener;

public class TTQuestionDialogFragment extends DialogFragment {

    private static final String ARG_PARAM = "mismatchEye";
    private static final String ARG_PARAM_FORM_RES = "formResult";
    private static final String ARG_PARAM_ALGO_RES = "algoResult";
    private TTQuestionDialogActionListener mListener;
    private Button btnDone;
    private LinearLayout llScreeningQuestion;
    private TextView tvFormResult, tvAlgorithmicResult;
    private TextInputLayout llLEftEyeQuestion, llRightEyeQuestion;
    private RadioGroup rgRightEye, rgLeftEye;
    private String selectedTTValue = "";
    private String mismatchEye; // LEFT, RIGHT
    private String formRes;
    private String algoRes;

    public TTQuestionDialogFragment() {
        // Required empty public constructor
    }

    public void setListener(TTQuestionDialogActionListener listener){
        this.mListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Dialog dialog = new Dialog(getActivity(),R.style.TTQuestion_Theme_Dialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        return dialog;
    }



    public static TTQuestionDialogFragment newInstance(String mismatchEye, String formResult, String algoResult) {
        TTQuestionDialogFragment fragment = new TTQuestionDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM, mismatchEye);
        args.putString(ARG_PARAM_ALGO_RES, algoResult);
        args.putString(ARG_PARAM_FORM_RES, formResult);
        fragment.setArguments(args);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mismatchEye = getArguments().getString(ARG_PARAM);
            formRes = getArguments().getString(ARG_PARAM_FORM_RES);
            algoRes = getArguments().getString(ARG_PARAM_ALGO_RES);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tt_question_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initVariable();
        initView(view);
        initFunctionality();
        initListener();
    }

    private void initVariable() {
    }

    private void initView(View view) {
        llScreeningQuestion = view.findViewById(R.id.llScreeningQuestion);
        llRightEyeQuestion = view.findViewById(R.id.llRightEyeQuestion);
        llLEftEyeQuestion = view.findViewById(R.id.llLEftEyeQuestion);
        tvAlgorithmicResult = view.findViewById(R.id.tvAlgorithmicResult);
        tvFormResult = view.findViewById(R.id.tvFormResult);
        rgRightEye = view.findViewById(R.id.rgRightEye);
        rgLeftEye = view.findViewById(R.id.rgLeftEye);;
        btnDone = view.findViewById(R.id.btnDone);
    }

    private void initFunctionality() {
        if(mismatchEye.equals(Eye.LEFT.name())){
            llLEftEyeQuestion.setVisibility(View.VISIBLE);
            llRightEyeQuestion.setVisibility(View.GONE);
        }
        else{
            llLEftEyeQuestion.setVisibility(View.GONE);
            llRightEyeQuestion.setVisibility(View.VISIBLE);
        }
        tvFormResult.setText(String.format(": %s", formRes));
        tvAlgorithmicResult.setText(String.format(": %s", algoRes));
    }

    private void initListener() {
        rgRightEye.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton radioButton = (RadioButton)group.findViewById(checkedId);
                selectedTTValue = radioButton.getText().toString();
            }
        });

        rgLeftEye.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton radioButton = (RadioButton)group.findViewById(checkedId);
                selectedTTValue = radioButton.getText().toString();
            }
        });
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mismatchEye.equals(Eye.LEFT.name()) && selectedTTValue.isEmpty()){
                    llLEftEyeQuestion.setError(getResources().getText(R.string.answer_validation_msg));
                }
                else if(mismatchEye.equals(Eye.RIGHT.name()) && selectedTTValue.isEmpty()){
                    llRightEyeQuestion.setError(getResources().getText(R.string.answer_validation_msg));
                }
                else{
                    dismiss();
                    mListener.onPressDone(selectedTTValue, mismatchEye);
                }
            }
        });
    }
}