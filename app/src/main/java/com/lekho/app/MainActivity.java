package com.lekho.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
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

    private WebView webView;
    private SpeechRecognizer speechRecognizer;
    private static final int MIC_PERMISSION_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        webView.setBackgroundColor(Color.WHITE);
        webView.setWebViewClient(new WebViewClient());

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        webView.addJavascriptInterface(new SpeechBridge(), "AndroidSpeech");

        setContentView(webView);

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MIC_PERMISSION_CODE
            );
        }

        webView.loadUrl("file:///android_asset/index.html");
    }

    private class SpeechBridge {

        @JavascriptInterface
        public void startSpeech() {

            runOnUiThread(() -> {

                if (ContextCompat.checkSelfPermission(
                        MainActivity.this,
                        Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(
                            MainActivity.this,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            MIC_PERMISSION_CODE
                    );

                    return;
                }

                startNativeSpeech();
            });
        }
    }

    private void startNativeSpeech() {

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            sendSpeechError("इस फोन पर Voice Recognition उपलब्ध नहीं है।");
            return;
        }

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
                sendSpeechStatus("सुन रहा हूँ… बोलिए");
            }

            @Override
            public void onBeginningOfSpeech() {
                sendSpeechStatus("सुन रहा हूँ…");
            }

            @Override
            public void onEndOfSpeech() {
                sendSpeechStatus("आवाज़ प्रोसेस हो रही है…");
            }

            @Override
            public void onResults(Bundle results) {

                ArrayList<String> matches =
                        results.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION
                        );

                if (matches != null && !matches.isEmpty()) {
                    sendSpeechResult(matches.get(0));
                } else {
                    sendSpeechError("आवाज़ समझ नहीं आई।");
                }
            }

            @Override
            public void onError(int error) {
                sendSpeechError(getSpeechErrorMessage(error));
            }

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        Intent intent = new Intent(
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
                RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                "hi-IN"
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                3
        );

        speechRecognizer.startListening(intent);
    }

    private void sendSpeechResult(String text) {

        String safeText = text
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", " ");

        runOnUiThread(() -> {

            webView.evaluateJavascript(
                    "window.__nativeSpeechResult('" +
                            safeText +
                            "')",
                    null
            );
        });
    }

    private void sendSpeechError(String message) {

        String safeMessage = message
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", " ");

        runOnUiThread(() -> {

            webView.evaluateJavascript(
                    "window.__nativeSpeechError('" +
                            safeMessage +
                            "')",
                    null
            );
        });
    }

    private void sendSpeechStatus(String message) {

        String safeMessage = message
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", " ");

        runOnUiThread(() -> {

            webView.evaluateJavascript(
                    "window.showMicStatus('" +
                            safeMessage +
                            "')",
                    null
            );
        });
    }

    private String getSpeechErrorMessage(int error) {

        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "माइक्रोफोन में समस्या हुई।";

            case SpeechRecognizer.ERROR_NETWORK:
                return "नेटवर्क की समस्या है।";

            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "नेटवर्क टाइमआउट हुआ।";

            case SpeechRecognizer.ERROR_NO_MATCH:
                return "आवाज़ समझ नहीं आई।";

            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Voice Recognition पहले से चल रहा है।";

            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "माइक्रोफोन की अनुमति नहीं मिली।";

            default:
                return "Voice Recognition में समस्या हुई।";
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == MIC_PERMISSION_CODE) {

            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                sendSpeechStatus("माइक्रोफोन की अनुमति मिल गई।");

            } else {

                sendSpeechError(
                        "माइक्रोफोन की अनुमति दें, तभी Voice चलेगा।"
                );
            }
        }
    }

    @Override
    protected void onDestroy() {

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        if (webView != null) {
            webView.destroy();
        }

        super.onDestroy();
    }
}
