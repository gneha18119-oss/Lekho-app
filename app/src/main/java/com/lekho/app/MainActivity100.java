package com.lekho.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebViewAssetLoader;

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
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMediaPlaybackRequiresUserGesture(false);

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(
                    WebView view, android.webkit.WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }
        });

        webView.addJavascriptInterface(new MicBridge(), "LekhoMic");
        setContentView(webView);
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }

    private void startListening(String id) {
        inputId = id == null ? "" : id.trim();
        if (inputId.isEmpty()) {
            sendError("Voice input field नहीं मिला।");
            return;
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION);
            return;
        }
        chooseSpeechLanguage();
    }

    private void chooseSpeechLanguage() {
        runOnUiThread(() -> new AlertDialog.Builder(MainActivity.this)
                .setTitle("बोलने की भाषा चुनें / Choose language")
                .setItems(new String[]{"हिंदी", "English"}, (dialog, which) -> {
                    String language = which == 0 ? "hi-IN" : "en-IN";
                    launchSpeechRecognition(language);
                })
                .setOnCancelListener(dialog -> sendError("Voice input रद्द किया गया।"))
                .show());
    }

    private void launchSpeechRecognition(String language) {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language);
            intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                    "बोलिए… / Speak now…");

            if (intent.resolveActivity(getPackageManager()) == null) {
                sendError("इस फोन में Speech Recognition उपलब्ध नहीं है।");
                return;
            }
            startActivityForResult(intent, SPEECH_REQUEST);
        } catch (Exception e) {
            sendError("Voice Recognition शुरू नहीं हो पाया। Google app/Speech Services जाँचें।");
        }
    }

    private void sendError(String message) {
        if (webView == null) return;
        String safe = JSONObject.quote(message == null ? "" : message);
        runOnUiThread(() -> webView.evaluateJavascript(
                "if(window.micError){window.micError(" + safe + ");}", null));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != SPEECH_REQUEST) return;

        if (resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String text = results.get(0).replace("\n", " ")
                        .replace("\r", " ").trim();
                if (text.isEmpty()) {
                    sendError("आवाज़ समझ नहीं आई। फिर से बोलें।");
                    return;
                }
                String safeId = JSONObject.quote(inputId);
                String safeText = JSONObject.quote(text);
                runOnUiThread(() -> webView.evaluateJavascript(
                        "if(window.setMicText){window.setMicText(" +
                                safeId + "," + safeText + ");}", null));
            } else {
                sendError("आवाज़ समझ नहीं आई। फिर से बोलें।");
            }
        } else {
            sendError("Voice input पूरा नहीं हुआ। Mic दबाकर फिर कोशिश करें।");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != MIC_PERMISSION) return;

        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            chooseSpeechLanguage();
        } else {
            sendError("Mic permission allow करें, तभी आवाज़ से entry हो सकेगी।");
        }
    }

    public class MicBridge {
        @JavascriptInterface
        public void startListening(String id) {
            runOnUiThread(() -> MainActivity.this.startListening(id));
        }
    }
}
