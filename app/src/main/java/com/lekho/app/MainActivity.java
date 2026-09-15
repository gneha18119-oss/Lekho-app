package com.lekho.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int MIC_REQUEST = 1001;
    private WebView webView;
    private SpeechRecognizer speechRecognizer;
    private boolean waitingForPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new SpeechBridge(), "AndroidSpeech");

        setContentView(webView);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MIC_REQUEST);
        }

        webView.loadUrl("file:///android_asset/index.html");
    }

    private void startNativeSpeech() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            sendError("इस फोन में Hindi Voice Recognition उपलब्ध नहीं है।");
            return;
        }

        if (speechRecognizer != null) {
            try { speechRecognizer.destroy(); } catch (Exception ignored) {}
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { sendStatus("🎤 Mic चालू है… अब बोलिए"); }
            @Override public void onBeginningOfSpeech() { sendStatus("🎤 सुन रहा हूँ…"); }
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}

            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    sendResult(matches.get(0));
                } else {
                    sendError("आवाज़ समझ नहीं आई। फिर से Mic दबाएँ।");
                }
                destroyRecognizer();
            }

            @Override public void onError(int error) {
                String msg;
                switch (error) {
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        msg = "Microphone permission Allow करें।"; break;
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        msg = "आवाज़ समझ नहीं आई। फिर से साफ बोलें।"; break;
                    case SpeechRecognizer.ERROR_NETWORK:
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        msg = "Voice service के लिए Internet चालू रखें।"; break;
                    case SpeechRecognizer.ERROR_AUDIO:
                        msg = "Microphone उपलब्ध नहीं है या किसी दूसरे app ने उसे इस्तेमाल किया है।"; break;
                    default:
                        msg = "Voice input शुरू नहीं हो पाया। फिर से Mic दबाएँ।";
                }
                sendError(msg);
                destroyRecognizer();
            }
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);

        try {
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            sendError("Microphone अभी शुरू नहीं हो पाया। फिर कोशिश करें।");
            destroyRecognizer();
        }
    }

    private void destroyRecognizer() {
        if (speechRecognizer != null) {
            try { speechRecognizer.stopListening(); } catch (Exception ignored) {}
            try { speechRecognizer.destroy(); } catch (Exception ignored) {}
            speechRecognizer = null;
        }
    }

    private void sendResult(String text) {
        if (webView == null) return;
        String safe = text == null ? "" : text.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ").replace("\r", " ");
        webView.post(() -> webView.evaluateJavascript("window.__nativeSpeechResult('" + safe + "')", null));
    }

    private void sendError(String message) {
        if (webView == null) return;
        String safe = message == null ? "Voice input शुरू नहीं हो पाया।" : message.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ").replace("\r", " ");
        webView.post(() -> webView.evaluateJavascript("window.__nativeSpeechError('" + safe + "')", null));
    }

    private void sendStatus(String message) {
        if (webView == null) return;
        String safe = message == null ? "🎤 सुन रहा हूँ…" : message.replace("\\", "\\\\").replace("'", "\\'");
        webView.post(() -> webView.evaluateJavascript("window.showMicStatus && window.showMicStatus('" + safe + "')", null));
    }

    private class SpeechBridge {
        @JavascriptInterface
        public void startSpeech() {
            runOnUiThread(() -> {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    waitingForPermission = true;
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, MIC_REQUEST);
                    return;
                }
                startNativeSpeech();
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (waitingForPermission) startNativeSpeech();
            } else {
                sendError("Microphone permission Allow करें, तभी Voice Typing चलेगी।");
            }
            waitingForPermission = false;
        }
    }

    @Override
    protected void onDestroy() {
        destroyRecognizer();
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
