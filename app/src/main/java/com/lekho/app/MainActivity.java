package com.lekho.app;

import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        webView.setBackgroundColor(Color.WHITE);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onReceivedError(
                    WebView view,
                    WebResourceRequest request,
                    WebResourceError error) {

                if (request.isForMainFrame()) {
                    TextView errorText = new TextView(MainActivity.this);
                    errorText.setText(
                            "लेखो ऐप खुल रही है,\n\n" +
                            "लेकिन ऐप की स्क्रीन लोड नहीं हो पाई।\n\n" +
                            "Error: " + error.getDescription()
                    );
                    errorText.setTextSize(18);
                    errorText.setTextColor(Color.BLACK);
                    errorText.setBackgroundColor(Color.WHITE);
                    errorText.setPadding(40, 80, 40, 40);

                    setContentView(errorText);
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient());

        setContentView(webView);

        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
