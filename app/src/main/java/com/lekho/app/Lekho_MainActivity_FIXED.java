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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int MIC_REQUEST = 1001;
    private WebView webView;
    private SpeechRecognizer recognizer;
    private boolean pendingMic = false;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new SpeechBridge(), "AndroidSpeech");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void startSpeech() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            pendingMic = true;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, MIC_REQUEST);
            return;
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            sendError("इस फोन में Voice Recognition उपलब्ध नहीं है।");
            return;
        }

        destroyRecognizer();
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle p) { sendStatus("🎤 Mic चालू है… अब बोलिए"); }
            @Override public void onBeginningOfSpeech() { sendStatus("🎤 सुन रहा हूँ…"); }
            @Override public void onRmsChanged(float r) {}
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEndOfSpeech() { sendStatus("आवाज़ process हो रही है…"); }
            @Override public void onPartialResults(Bundle b) {}
            @Override public void onEvent(int t, Bundle b) {}

            @Override public void onResults(Bundle b) {
                ArrayList<String> m = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (m != null && !m.isEmpty() && m.get(0) != null) sendResult(m.get(0));
                else sendError("आवाज़ समझ नहीं आई। फिर से Mic दबाएँ।");
                destroyRecognizer();
            }

            @Override public void onError(int e) {
                sendError("Voice input शुरू नहीं हो पाया। फिर से Mic दबाएँ।");
                destroyRecognizer();
            }
        });

        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN");
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);

        try { recognizer.startListening(i); }
        catch (Exception e) {
            sendError("Microphone अभी शुरू नहीं हो पाया।");
            destroyRecognizer();
        }
    }

    private String safe(String s) {
        return (s == null ? "" : s).replace("\\","\\\\")
                .replace("'","\\'").replace("\n"," ").replace("\r"," ");
    }

    private void sendResult(String s) {
        if (webView == null) return;
        String x = safe(s);
        webView.post(() -> webView.evaluateJavascript(
                "window.__nativeSpeechResult&&window.__nativeSpeechResult('" + x + "')", null));
    }

    private void sendStatus(String s) {
        if (webView == null) return;
        String x = safe(s);
        webView.post(() -> webView.evaluateJavascript(
                "window.showMicStatus&&window.showMicStatus('" + x + "')", null));
    }

    private void sendError(String s) {
        if (webView == null) return;
        String x = safe(s);
        webView.post(() -> webView.evaluateJavascript(
                "window.__nativeSpeechError&&window.__nativeSpeechError('" + x + "')", null));
    }

    private void destroyRecognizer() {
        if (recognizer != null) {
            try { recognizer.stopListening(); } catch (Exception ignored) {}
            try { recognizer.cancel(); } catch (Exception ignored) {}
            try { recognizer.destroy(); } catch (Exception ignored) {}
            recognizer = null;
        }
    }

    private class SpeechBridge {
        @JavascriptInterface public void startSpeech() {
            runOnUiThread(MainActivity.this::startSpeech);
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_REQUEST) {
            boolean ok = grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED;
            boolean start = pendingMic;
            pendingMic = false;
            if (ok && start) startSpeech();
            else if (!ok) sendError("Microphone permission Allow करें।");
        }
    }

    // One back handler: close print preview/modal first, then leave the app.
    @Override public void onBackPressed() {
        if (webView == null) { super.onBackPressed(); return; }

        webView.evaluateJavascript(
            "(function(){" +
            "var p=document.getElementById('lekhoPrintPreview');" +
            "if(p){p.remove();return 'handled'}" +
            "var m=document.querySelector('.productModal');" +
            "if(m){m.remove();return 'handled'}" +
            "if(window.__nativeBack&&window.__nativeBack())return 'handled';" +
            "return 'exit'})()",
            value -> {
                if ("\"exit\"".equals(value)) MainActivity.super.onBackPressed();
            });
    }

    @Override protected void onDestroy() {
        destroyRecognizer();
        if (webView != null) { webView.destroy(); webView = null; }
        super.onDestroy();
    }
}
