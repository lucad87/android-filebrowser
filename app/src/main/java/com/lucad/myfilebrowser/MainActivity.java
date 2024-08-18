package com.lucad.myfilebrowser;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    WebView myFileBrowser;

    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILECHOOSER_RESULTCODE = 1;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myFileBrowser = findViewById(R.id.myFileBrowser);
        myFileBrowser.getSettings().setJavaScriptEnabled(true);
        myFileBrowser.getSettings().setDomStorageEnabled(true); // Enable DOM storage
        //myFileBrowser.getSettings().setDatabaseEnabled(true); // Enable database storage
        myFileBrowser.getSettings().setAllowFileAccess(true);
        myFileBrowser.getSettings().setAllowFileAccess(true);
        myFileBrowser.getSettings().setAllowFileAccessFromFileURLs(true);
        myFileBrowser.getSettings().setAllowUniversalAccessFromFileURLs(true);

        myFileBrowser.setWebViewClient(new WebViewClient());

        // Set a DownloadListener to handle file downloads
        myFileBrowser.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                // Handle the download here
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            }
        });

        // Set a WebChromeClient to handle file uploads
        myFileBrowser.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                MainActivity.this.filePathCallback = filePathCallback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*"); // Allow all file types
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Allow multiple selection
                startActivityForResult(Intent.createChooser(intent, "Choose Files"), FILECHOOSER_RESULTCODE);
                return true;
            }
        });

        // Load the URL
        myFileBrowser.loadUrl("https://browser.lucad.cloud/");

        // Inject JavaScript to observe changes in the parent element
        myFileBrowser.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // JavaScript to observe changes in the parent element
                String js = "var targetNode = document.body;" + // Observe the body or a specific parent element
                        "var config = { childList: true, subtree: true };" +
                        "var callback = function(mutationsList, observer) {" +
                        "    for(var mutation of mutationsList) {" +
                        "        if (mutation.type === 'childList') {" +
                        "            var actions = document.querySelectorAll('.card-action .action .title');" +
                        "            for (var i = 0; i < actions.length; i++) {" +
                        "                if (actions[i].textContent.trim() === 'Folder') {" +
                        "                    actions[i].parentElement.style.display = 'none';" +
                        "                }" +
                        "            }" +
                        "        }" +
                        "    }" +
                        "};" +
                        "var observer = new MutationObserver(callback);" +
                        "observer.observe(targetNode, config);";
                view.evaluateJavascript(js, null);
            }
        });

        OnBackPressedDispatcher onBackPressedDispatcher = getOnBackPressedDispatcher();
        onBackPressedDispatcher.addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (myFileBrowser.canGoBack()) {
                    myFileBrowser.goBack();
                } else {
                    setEnabled(false);
                    onBackPressedDispatcher.onBackPressed();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILECHOOSER_RESULTCODE) {
            if (filePathCallback != null) {
                Uri[] results = null;
                if (data != null && resultCode == RESULT_OK) {
                    // Handle multiple file selection
                    if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        results = new Uri[count];
                        for (int i = 0; i < count; i++) {
                            results[i] = data.getClipData().getItemAt(i).getUri();
                        }
                    } else if (data.getData() != null) {
                        // Single file selected
                        results = new Uri[]{data.getData()};
                    }
                }
                filePathCallback.onReceiveValue(results);
                filePathCallback = null;
            }
        }
    }

}