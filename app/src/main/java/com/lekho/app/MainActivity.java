package com.lekho.app;

import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView webView = new WebView(this);

        webView.setBackgroundColor(Color.WHITE);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        setContentView(webView);

        TextView test = new TextView(this);
        test.setText(
                "लेखो ऐप\n\n" +
                "WebView सफलतापूर्वक चल रहा है।\n\n" +
                "अभी HTML लोड नहीं की गई है।"
        );
        test.setTextSize(22);
        test.setTextColor(Color.BLACK);
        test.setGravity(17);
        test.setBackgroundColor(Color.WHITE);

        setContentView(test);
    }
}
