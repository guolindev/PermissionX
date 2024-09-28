package com.permissionx.app;

import android.Manifest;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import com.permissionx.app.databinding.ActivityMainJavaBinding;
import com.permissionx.guolindev.PermissionMediator;
import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.patch.PermissionDelegateHolder;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class MainJavaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainJavaBinding binding = ActivityMainJavaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.makeRequestBtn.setOnClickListener(view -> {
            requestCameraPermission(MainJavaActivity.this);
        });

        binding.makeRequestBtn2.setOnClickListener(view -> {
            PermissionX.init(this, permissionMediator -> {
                permissionMediator
                        .permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .explainReasonBeforeRequest()
                        .request((allGranted, grantedList, deniedList) -> {
                            if (allGranted) {
                                Toast.makeText(MainJavaActivity.this, "All permissions are granted", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainJavaActivity.this, "The following permissions are denied：" + deniedList, Toast.LENGTH_SHORT).show();
                            }
                        });
                return null;
            });

            // or directly use PermissionDelegateHolder
//            PermissionDelegateHolder.delegate(this, fragmentActivity -> {
//                requestCameraPermission(fragmentActivity);// use the activity argument, do not use MainJavaActivity.this
//                return null;
//            });
        });
    }

    private void requestCameraPermission(FragmentActivity activity) {
        PermissionX.init(activity)
                .permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .explainReasonBeforeRequest()
                .onExplainRequestReason((scope, deniedList, beforeRequest) -> {
//                    CustomDialog customDialog = new CustomDialog(activity, "PermissionX needs following permissions to continue", deniedList);
//                    scope.showRequestReasonDialog(customDialog);
                    scope.showRequestReasonDialog(deniedList, "PermissionX needs following permissions to continue", "Allow");
                })
                .onForwardToSettings((scope, deniedList) -> {
                    scope.showForwardToSettingsDialog(deniedList, "Please allow following permissions in settings", "Allow");
                })
                .request((allGranted, grantedList, deniedList) -> {
                    if (allGranted) {
                        Toast.makeText(MainJavaActivity.this, "All permissions are granted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainJavaActivity.this, "The following permissions are denied：" + deniedList, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}