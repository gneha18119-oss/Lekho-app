package com.lekho.app;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.webkit.WebViewAssetLoader;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private static final int MIC_REQUEST = 1001;
    private WebView webView;
    private FirebaseAuth auth;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        webView = new WebView(this);
        setContentView(webView);
        WebView.setWebContentsDebuggingEnabled(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowContentAccess(false);

        WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();
        webView.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.RECORD_AUDIO}, MIC_REQUEST);
                    } else {
                        request.grant(request.getResources());
                    }
                });
            }
        });
        webView.addJavascriptInterface(new FirebaseBridge(), "LekhoNative");
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }

    private void sendOtp(String phone, final String callback) {
        String e164 = phone.startsWith("+") ? phone : "+91" + phone;
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(e164)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override public void onVerificationCompleted(PhoneAuthCredential credential) {
                        // Instant verification can happen on some devices; still report success.
                        runJs(callback + "(true,'OTP स्वतः verify हो गया ✓','')");
                    }
                    @Override public void onVerificationFailed(FirebaseException e) {
                        runJs(callback + "(false," + js(e.getMessage() == null ? "OTP भेजना असफल हुआ।" : e.getMessage()) + ",'')");
                    }
                    @Override public void onCodeSent(String id, PhoneAuthProvider.ForceResendingToken token) {
                        verificationId = id;
                        resendToken = token;
                        runJs(callback + "(true,'OTP भेज दिया गया ✓','')");
                    }
                }).build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyOtp(String code, final String callback) {
        if (verificationId == null) {
            runJs(callback + "(false,'पहले OTP भेजें।','')");
            return;
        }
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        auth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful() && auth.getCurrentUser() != null) {
                runJs(callback + "(true,'OTP verify सफल ✓'," + js(auth.getCurrentUser().getUid()) + ")");
            } else {
                String msg = task.getException() == null ? "OTP गलत है या expire हो गया।" : task.getException().getMessage();
                runJs(callback + "(false," + js(msg) + ",'')");
            }
        });
    }

    private void runJs(String js) {
        runOnUiThread(() -> webView.evaluateJavascript("javascript:" + js, null));
    }

    private String js(String s) {
        if (s == null) s = "";
        return "'" + s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ") + "'";
    }

    public class FirebaseBridge {
        @JavascriptInterface public void sendOtp(String phone, String callback) {
            if (phone == null || !phone.matches("\\d{10}")) {
                runJs(callback + "(false,'सही 10 अंकों का मोबाइल नंबर डालें।','')");
                return;
            }
            sendOtp(phone, callback);
        }
        @JavascriptInterface public void verifyOtp(String code, String callback) {
            if (code == null || !code.matches("\\d{6}")) {
                runJs(callback + "(false,'6 अंकों का OTP डालें।','')");
                return;
            }
            verifyOtp(code, callback);
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            webView.reload();
        } else if (requestCode == MIC_REQUEST) {
            Toast.makeText(this, "लेखो ऐप को Microphone permission दें।", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
