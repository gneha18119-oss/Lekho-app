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
    private static final int MIC_PERMISSION=101;
    private static final int SPEECH_REQUEST=102;
    private WebView webView;
    private String inputId="";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView=new WebView(this);
        WebSettings s=webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new MicBridge(),"LekhoMic");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
        requestMicPermissionIfNeeded();
    }

    private void requestMicPermissionIfNeeded() {
        if(android.os.Build.VERSION.SDK_INT>=23 &&
           checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},MIC_PERMISSION);
        }
    }

    private void startListening(String id) {
        inputId=id==null?"":id;
        if(android.os.Build.VERSION.SDK_INT>=23 &&
           checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},MIC_PERMISSION);
            return;
        }
        try {
            Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault());
            i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
            i.putExtra(RecognizerIntent.EXTRA_PROMPT,"बोलिए...");
            startActivityForResult(i,SPEECH_REQUEST);
        } catch(Exception e) {
            sendError("इस फोन में voice recognition उपलब्ध नहीं है");
        }
    }

    private void sendError(String message) {
        String safe=message.replace("\\","\\\\").replace("'","\\'").replace("\n"," ").replace("\r"," ");
        webView.evaluateJavascript("micError('"+safe+"')",null);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=SPEECH_REQUEST) return;
        if(resultCode==RESULT_OK && data!=null) {
            ArrayList<String> r=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if(r!=null && !r.isEmpty()) {
                String text=r.get(0).replace("\\","\\\\").replace("'","\\'").replace("\n"," ").replace("\r"," ");
                String id=inputId.replace("\\","\\\\").replace("'","\\'");
                webView.evaluateJavascript("setMicText('"+id+"','"+text+"')",null);
            } else sendError("आवाज़ समझ नहीं आई");
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==MIC_PERMISSION) {
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                if(!inputId.isEmpty()) startListening(inputId);
            } else sendError("Mic permission allow करें");
        }
    }

    public class MicBridge {
        @JavascriptInterface public void startListening(String id) {
            MainActivity.this.startListening(id);
        }
    }
}
