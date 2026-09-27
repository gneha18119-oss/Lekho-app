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

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int RECORD_AUDIO_REQUEST = 1001;
    private static final int SPEECH_REQUEST = 1002;

    private WebView webView;
    private String activeInputId = "";

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
        webView.setWebChromeClient(new WebChromeClient());

        webView.addJavascriptInterface(new MicBridge(), "LekhoMic");

        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");

        if (android.os.Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    RECORD_AUDIO_REQUEST
            );
        }
    }

    public class MicBridge {

        @JavascriptInterface
        public void startListening(String inputId) {

            activeInputId = inputId;

            if (android.os.Build.VERSION.SDK_INT >= 23 &&
                    checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                            != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        RECORD_AUDIO_REQUEST
                );

                return;
            }

            startSpeechRecognition();
        }
    }

    private void startSpeechRecognition() {

        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

            intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    Locale.getDefault()
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                    Locale.getDefault()
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "बोलिए..."
            );

            intent.putExtra(
                    RecognizerIntent.EXTRA_MAX_RESULTS,
                    1
            );

            startActivityForResult(intent, SPEECH_REQUEST);

        } catch (Exception e) {
            sendMicMessage("Mic उपलब्ध नहीं है");
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != SPEECH_REQUEST) {
            return;
        }

        if (resultCode == RESULT_OK && data != null) {

            ArrayList<String> results =
                    data.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS
                    );

            if (results != null && !results.isEmpty()) {

                String text = results.get(0);

                String safeText = text
                        .replace("\\", "\\\\")
                        .replace("'", "\\'")
                        .replace("\n", " ")
                        .replace("\r", " ");

                String javascript =
                        "setMicText('" +
                        activeInputId +
                        "','" +
                        safeText +
                        "')";

                webView.evaluateJavascript(javascript, null);

            } else {
                sendMicMessage("आवाज़ समझ नहीं आई");
            }

        } else {
            sendMicMessage("Mic बंद किया गया");
        }
    }

    private void sendMicMessage(String message) {

        String safeMessage = message
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", " ")
                .replace("\r", " ");

        String javascript =
                "micError('" + safeMessage + "')";

        webView.evaluateJavascript(javascript, null);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == RECORD_AUDIO_REQUEST) {

            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if (!activeInputId.isEmpty()) {
                    startSpeechRecognition();
                }

            } else {
                sendMicMessage(
                        "Mic permission allow करें"
                );
            }
        }
    }

    @Override
    public void onBackPressed() {

        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
