package org.rti.ttfinder.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import org.rti.ttfinder.R;
import org.rti.ttfinder.data.AppConstants;
import org.rti.ttfinder.listeners.DialogActionListener;

import java.util.Locale;

public class AdminLoginDialogFragment extends DialogFragment {

    private DialogActionListener mListener;
    private Button btnEnable;
    private MaterialButton btnCancel;
    private EditText etPassword;
    private TextInputLayout tilPassword;

    public AdminLoginDialogFragment() {
        // Required empty public constructor
    }

    public void setListener(DialogActionListener listener){
        this.mListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Dialog dialog = new Dialog(getActivity(),R.style.Theme_Dialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        return dialog;
    }



    public static AdminLoginDialogFragment newInstance() {
        AdminLoginDialogFragment fragment = new AdminLoginDialogFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_admin_login_dialog, container, false);
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
        tilPassword = view.findViewById(R.id.til_password);
        etPassword = view.findViewById(R.id.etPassword);
        btnEnable = view.findViewById(R.id.btn_enable);
        btnCancel = view.findViewById(R.id.btn_cancel);
    }

    private void initFunctionality() {

    }

    private void initListener() {
        btnEnable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String password = etPassword.getText().toString();
                System.out.println(password);
                if(password.isEmpty() || !password.toLowerCase(Locale.ROOT).equals(AppConstants.ADMIN_PASSWORD.toLowerCase(Locale.ROOT))){
                    tilPassword.setError("Please enter valid password!");
                }
                else{
                    dismiss();
                    mListener.positiveBtn();
                }
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                mListener.negativeBtn();
            }
        });
    }
}