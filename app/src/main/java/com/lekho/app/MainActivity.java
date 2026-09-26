package com.lekho.app;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private WebView webView;

    private static final int AUDIO_PERMISSION_CODE = 44;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onPermissionRequest(final PermissionRequest request) {

                runOnUiThread(() -> {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                        if (checkSelfPermission(
                                Manifest.permission.RECORD_AUDIO
                        ) != PackageManager.PERMISSION_GRANTED) {

                            requestPermissions(
                                    new String[]{
                                            Manifest.permission.RECORD_AUDIO
                                    },
                                    AUDIO_PERMISSION_CODE
                            );

                            return;
                        }
                    }

                    request.grant(request.getResources());
                });
            }
        });

        setContentView(webView);

        webView.loadUrl("file:///android_asset/index.html");

        requestAudioPermission();
    }

    private void requestAudioPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (checkSelfPermission(
                    Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.RECORD_AUDIO
                        },
