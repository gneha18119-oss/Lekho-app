package com.lekho.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private static final int MIC_PERMISSION = 101;
    private static final int SPEECH_REQUEST = 102;

    private WebView webView;
    private String inputId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        webView.addJavascriptInterface(
                new MicBridge(),
                "LekhoMic"
        );

        setContentView(webView);

        webView.loadUrl(
                "file:///android_asset/index.html"
        );

        requestMicPermissionIfNeeded();
    }

    private void requestMicPermissionIfNeeded() {

        if (
                android.os.Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(
                        Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.RECORD_AUDIO
                    },
                    MIC_PERMISSION
            );
        }
    }

    private void startListening(String id) {

        inputId = id == null ? "" : id;

        if (
                android.os.Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(
                        Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.RECORD_AUDIO
                    },
                    MIC_PERMISSION
            );
            return;
        }

        try {

            Intent intent =
                    new Intent(
                            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                    );

            intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    "hi-IN"
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_MAX_RESULTS,
                    1
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "बोलिए..."
            );

            startActivityForResult(
                    intent,
                    SPEECH_REQUEST
            );

        } catch (Exception e) {

            sendError(
                    "इस फोन में voice recognition उपलब्ध नहीं है"
            );
        }
    }

    private void sendError(String message) {

        String safe = JSONObject.quote(
                message == null ? "" : message
        );

        webView.evaluateJavascript(
                "micError(" + safe + ")",
                null
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode != SPEECH_REQUEST) {
            return;
        }

        if (
                resultCode == RESULT_OK &&
                data != null
        ) {

            ArrayList<String> results =
                    data.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS
                    );

            if (
                    results != null &&
                    !results.isEmpty()
            ) {

                String text =
                        results.get(0)
                                .replace("\n", " ")
                                .replace("\r", " ")
                                .trim();

                String safeId =
                        JSONObject.quote(inputId);

                String safeText =
                        JSONObject.quote(text);

                webView.evaluateJavascript(
                        "setMicText(" +
                                safeId +
                                "," +
                                safeText +
                                ")",
                        null
                );

            } else {

                sendError(
                        "आवाज़ समझ नहीं आई"
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == MIC_PERMISSION) {

            if (
                    grantResults.length > 0 &&
                    grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED
            ) {

                if (!inputId.isEmpty()) {
                    startListening(inputId);
                }

            } else {

                sendError(
                        "Mic permission allow करें"
                );
            }
        }
    }

    public class MicBridge {

        @JavascriptInterface
        public void startListening(String id) {
            MainActivity.this.startListening(id);
        }
    }
}
