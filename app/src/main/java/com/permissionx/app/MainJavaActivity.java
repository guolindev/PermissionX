package com.permissionx.app;

import android.Manifest;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.ExplainReasonCallbackWithBeforeParam;
import com.permissionx.guolindev.callback.ForwardToSettingsCallback;
import com.permissionx.guolindev.callback.RequestCallback;
import com.permissionx.guolindev.request.ExplainScope;
import com.permissionx.guolindev.request.ForwardScope;

import java.util.List;

public class MainJavaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_java);
        Button makeCallBtn = findViewById(R.id.makeCallBtn);
        makeCallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PermissionX.init(MainJavaActivity.this)
                        .permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .onExplainRequestReason(new ExplainReasonCallbackWithBeforeParam() {
                            @Override
                            public void onExplainReason(ExplainScope scope, List<String> deniedList, boolean beforeRequest) {
                                scope.showRequestReasonDialog(deniedList, "PermissionX needs following permissions to continue", "Allow");
                            }
                        })
                        .onForwardToSettings(new ForwardToSettingsCallback() {
                            @Override
                            public void onForwardToSettings(ForwardScope scope, List<String> deniedList) {
                                scope.showForwardToSettingsDialog(deniedList, "Please allow following permissions in settings", "Allow");
                            }
                        })
                        .request(new RequestCallback() {
                            @Override
                            public void onResult(boolean allGranted, List<String> grantedList, List<String> deniedList) {
                                if (allGranted) {
                                    Toast.makeText(MainJavaActivity.this, "All permissions are granted", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(MainJavaActivity.this, "The following permissions are denied：" + deniedList, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }
}