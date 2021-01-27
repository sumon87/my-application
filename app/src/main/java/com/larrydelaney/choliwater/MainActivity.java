package com.larrydelaney.choliwater;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.gold.webview.codecanyon.com.webview.BuildConfig;
import android.gold.webview.codecanyon.com.webview.R;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.onesignal.OSPermissionSubscriptionState;
import com.onesignal.OSSubscriptionObserver;
import com.onesignal.OSSubscriptionStateChanges;
import com.onesignal.OneSignal;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import static com.larrydelaney.choliwater.Config.PURCHASE_LICENSE_KEY;

public class MainActivity extends AppCompatActivity implements OSSubscriptionObserver, BillingProcessor.IBillingHandler {

    private WebView webView;
    private View offlineLayout;
    private AlertDialog noConnectionDialog;

    public static final int REQUEST_SELECT_FILE = 100;

    private AdView mAdView;
    InterstitialAd mInterstitialAd;
    public int webviewCount = 0;
    public ValueCallback<Uri[]> uploadMessage;
    public static final int MULTIPLE_PERMISSIONS = 10;
    public ProgressBar progressBar;
    private String deepLinkingURL;
    private BillingProcessor bp;

    private static final String TAG = MainActivity.class.getSimpleName();
    private String mCM;
    private ValueCallback<Uri> mUM;
    private ValueCallback<Uri[]> mUMA;
    private final static int FCR = 1;
    private boolean multiple_files = true;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MobileAds.initialize(this, String.valueOf(R.string.admob_app_id));
        bp = new BillingProcessor(this, PURCHASE_LICENSE_KEY, this);
        bp.initialize();

        Intent intent = getIntent();
        if (intent != null && intent.getData() != null && (intent.getData().getScheme().equals("http"))) {
            Uri data = intent.getData();
            List<String> pathSegments = data.getPathSegments();
            if (pathSegments.size() > 0) {
                deepLinkingURL = pathSegments.get(0).substring(5);
                String fulldeeplinkingurl = data.getPath().toString();
                fulldeeplinkingurl = fulldeeplinkingurl.replace("/link=", "");
                deepLinkingURL = fulldeeplinkingurl;
            }
        } else if (intent != null && intent.getData() != null && (intent.getData().getScheme().equals("https"))) {
            Uri data = intent.getData();
            List<String> pathSegments = data.getPathSegments();
            if (pathSegments.size() > 0) {
                deepLinkingURL = pathSegments.get(0).substring(5);
                String fulldeeplinkingurl = data.getPath().toString();
                fulldeeplinkingurl = fulldeeplinkingurl.replace("/link=", "");
                deepLinkingURL = fulldeeplinkingurl;
            }
        } else if (intent != null) {
            Bundle extras = getIntent().getExtras();
            String URL = null;
            if (extras != null) {
                URL = extras.getString("ONESIGNAL_URL");
            }
            if (URL != null && !URL.equalsIgnoreCase(""))
                deepLinkingURL = URL;
        }

        webviewCount = 0;
        final String myOSurl = Config.PURCHASECODE;

        if (Config.PUSH_ENABLED) {
            OneSignal.addSubscriptionObserver(this);
        }

        if (savedInstanceState == null) {
            AlertManager.appLaunched(this);
        }

        mAdView = findViewById(R.id.adView);

        AdRequest adRequest = new AdRequest.Builder()
                .build();

        if (BuildConfig.IS_DEBUG_MODE) {
             osURL(myOSurl);
        }

        if (Config.SHOW_BANNER_AD) {
            mAdView.loadAd(adRequest);
        } else {
            mAdView.setVisibility(View.GONE);
        }

        mInterstitialAd = new InterstitialAd(this);


        mInterstitialAd.setAdUnitId(getString(R.string.interstitial_full_screen));

        mInterstitialAd.setAdListener(new AdListener() {
            public void onAdLoaded() {
                showInterstitial();
            }
        });

        webView = findViewById(R.id.webView);
        offlineLayout = findViewById(R.id.offline_layout);

        progressBar = findViewById(R.id.progressBar);
        progressBar.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.colorAccent), PorterDuff.Mode.MULTIPLY);

        final Button tryAgainButton = findViewById(R.id.try_again_button);
        tryAgainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadMainUrl();
            }
        });

        webView.setWebViewClient(new MyWebViewClient(){
            private Handler notificationHandler;
            public void onGeolocationPermissionsShowPrompt(String origin, android.webkit.GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String url)
            {
                view.loadUrl("file:///android_asset/index.html");
                offlineLayout.setVisibility(View.VISIBLE);
            }
        });
        webView.getSettings().setSupportMultipleWindows(true);



        webView.setWebChromeClient(new MyWebChromeClient(){
            private Handler notificationHandler;
            @Override
            public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, android.os.Message resultMsg)
            {
                WebView.HitTestResult result = view.getHitTestResult();
                String data = result.getExtra();
                Context context = view.getContext();
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(data));
                context.startActivity(browserIntent);
                return false;
            }

            public void onGeolocationPermissionsShowPrompt(String origin, android.webkit.GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                mUM = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                if (multiple_files && Build.VERSION.SDK_INT >= 18) {
                    i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                }
                startActivityForResult(Intent.createChooser(i, "Upload"), FCR);
            }
            @SuppressLint("InlinedApi")
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {

                String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, perms, FCR);

                } else if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, FCR);

                } else if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, FCR);
                }
                if (mUMA != null) {
                    mUMA.onReceiveValue(null);
                }
                mUMA = filePathCallback;
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(MainActivity.this.getPackageManager()) != null) {
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                        takePictureIntent.putExtra("PhotoPath", mCM);
                    } catch (IOException ex) {
                        Log.e(TAG, "Image file creation failed", ex);
                    }
                    if (photoFile != null) {
                        mCM = "file:" + photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                    } else {
                        takePictureIntent = null;
                    }
                }
                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("image/*|application/pdf|audio/*");
                if (multiple_files) {
                    contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                }
                Intent[] intentArray;
                if (takePictureIntent != null) {
                    intentArray = new Intent[]{takePictureIntent};
                } else {
                    intentArray = new Intent[0];
                }

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Upload");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                startActivityForResult(chooserIntent, FCR);
                return true;
            }
        });


        webView.setDownloadListener(new DownloadListener() {
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimetype,
                                        long contentLength) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });
        registerForContextMenu(webView);

        final WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        webSettings.setDomStorageEnabled(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        if (Config.CLEAR_CACHE_ON_STARTUP) {
            webSettings.setAppCacheEnabled(false);
            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        } else {
            webSettings.setAppCacheEnabled(true);
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView,true);
        }
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        askForPermission();

        if (!Config.USER_AGENT.isEmpty()) {
            webSettings.setUserAgentString(Config.USER_AGENT);
        }

        if (Config.CLEAR_CACHE_ON_STARTUP) {
            webView.clearCache(true);
        }

        if (Config.USE_LOCAL_HTML_FOLDER) {
            webView.loadUrl("file:///android_asset/index.html");

        } else if (/*isNetworkAvailable()*/isConnectedNetwork()) {
            if (Config.USE_LOCAL_HTML_FOLDER) {
                webView.loadUrl("file:///android_asset/index.html");
            } else {
                loadMainUrl();
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        final WebView.HitTestResult webViewHitTestResult = webView.getHitTestResult();

        if (webViewHitTestResult.getType() == WebView.HitTestResult.IMAGE_TYPE ||
                webViewHitTestResult.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {

            menu.setHeaderTitle("Download images");
            menu.add(0, 1, 0, "Download the image")
                    .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {

                            String DownloadImageURL = webViewHitTestResult.getExtra();

                            if (URLUtil.isValidUrl(DownloadImageURL)) {

                                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(DownloadImageURL));
                                request.allowScanningByMediaScanner();
                                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                                downloadManager.enqueue(request);

                                Toast.makeText(MainActivity.this, "Image downloaded successfully.", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(MainActivity.this, "Sorry...something went wrong.", Toast.LENGTH_LONG).show();
                            }
                            return false;
                        }
                    });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if(Build.VERSION.SDK_INT >= 21){

            Uri[] results = null;
            //checking if response is positive
            if(resultCode == Activity.RESULT_OK){
                if(requestCode == FCR){
                    if(null == mUMA){
                        return;
                    }
                    if(intent == null || intent.getData() == null){
                        if(mCM != null){
                            results = new Uri[]{Uri.parse(mCM)};
                        }
                    }else{
                        String dataString = intent.getDataString();
                        if(dataString != null){
                            results = new Uri[]{Uri.parse(dataString)};
                        } else {
                            if(multiple_files) {
                                if (intent.getClipData() != null) {
                                    final int numSelectedFiles = intent.getClipData().getItemCount();
                                    results = new Uri[numSelectedFiles];
                                    for (int i = 0; i < numSelectedFiles; i++) {
                                        results[i] = intent.getClipData().getItemAt(i).getUri();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else
            {
                mUMA.onReceiveValue(null);
                mUMA = null;
            }
            if(results!=null)
                mUMA.onReceiveValue(results);
            mUMA = null;
        }
        else{
            if(requestCode == FCR){
                if(null == mUM) return;
                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
                mUM.onReceiveValue(result);
                mUM = null;
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        return imageFile;
    }

    @Override
    public void onBackPressed() {

        if (webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }

    private void openDownloadedAttachment(final Context context, final long downloadId) {
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor cursor = downloadManager.query(query);
        if (cursor.moveToFirst()) {
            int downloadStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            String downloadLocalUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
            String downloadMimeType = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE));
            if ((downloadStatus == DownloadManager.STATUS_SUCCESSFUL) && downloadLocalUri != null) {
                Log.d("texts", "Download done");
                Toast.makeText(context, "Saved to SD card", Toast.LENGTH_LONG).show();
                openDownloadedAttachment(context, Uri.parse(downloadLocalUri), downloadMimeType);
            }
        }
        cursor.close();
    }

    private void openDownloadedAttachment(Context context, Uri parse, String downloadMimeType) {
    }

    private void saveImage(final String urlString) {
        final String imageFormat = urlString.contains(".png") ? ".png" : ".jpg";
        final Thread downloadImageThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final URL url = new URL(urlString);
                    final InputStream inputStream = url.openStream();
                    final byte[] buffer = new byte[8192];
                    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        byteArrayOutputStream.write(buffer, 0, bytesRead);
                    }
                    inputStream.close();
                    final byte[] output = byteArrayOutputStream.toByteArray();
                    final Bitmap bitmap = BitmapFactory.decodeByteArray(output, 0, output.length);

                    final String fileName = String.valueOf(System.currentTimeMillis()) + imageFormat;

                    final File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    final File imageFile = new File(root, fileName);
                    root.mkdirs();

                    final FileOutputStream fileOutputStream = new FileOutputStream(imageFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                    fileOutputStream.flush();
                    fileOutputStream.close();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Image saved to /Downloads/" + fileName, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        downloadImageThread.start();
    }

    private void onUpdateNetworkStatus(final boolean isConnected) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isConnected) {
                    webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
                    offlineLayout.setVisibility(View.GONE);
                    noConnectionDialog = null;
                } else {
                    if (offlineLayout.getVisibility() == View.VISIBLE && (noConnectionDialog == null || !noConnectionDialog.isShowing())) {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
                                .setTitle(R.string.no_connection_title)
                                .setMessage(R.string.no_connection_message)
                                .setPositiveButton(android.R.string.ok, null);
                        noConnectionDialog = builder.create();
                        noConnectionDialog.show();
                    } else {
                        offlineLayout.setVisibility(View.VISIBLE);
                    }
                    webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                }
            }
        });
    }

    private void loadMainUrl() {
        offlineLayout.setVisibility(View.GONE);
        if (Config.IS_DEEP_LINKING_ENABLED && deepLinkingURL != null) {
            webView.loadUrl(deepLinkingURL);
        } else {
            String urlExt = "";
            if (Config.PUSH_ENABLED) {
                OSPermissionSubscriptionState status = OneSignal.getPermissionSubscriptionState();
                String userID = status.getSubscriptionStatus().getUserId();

                urlExt = ((Config.PUSH_ENHANCE_WEBVIEW_URL && !TextUtils.isEmpty(userID)) ? String.format("%sonesignal_push_id=%s", (Config.HOME_URL.contains("?") ? "&" : "?"), userID) : "");
            }
            if (Config.USE_LOCAL_HTML_FOLDER) {
                webView.loadUrl("file:///android_asset/index.html");
            } else {
                webView.loadUrl(Config.HOME_URL + urlExt);
            }
        }
    }

    private boolean isNetworkAvailable() {
        final ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (networkInfo == null || !networkInfo.isConnected()) {
            onUpdateNetworkStatus(false);
            return false;
        }

        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final URL url = new URL("https://clients3.google.com/generate_204");
                    final HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setRequestProperty("User-Agent", "Android");
                    httpURLConnection.setRequestProperty("Connection", "close");
                    httpURLConnection.setConnectTimeout(1500);
                    httpURLConnection.connect();
                    onUpdateNetworkStatus(httpURLConnection.getResponseCode() == 204 && httpURLConnection.getContentLength() == 0);
                } catch (Exception e) {
                    onUpdateNetworkStatus(false);
                }
            }
        });

        thread.start();

        return true;
    }

    public boolean isConnectedNetwork() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    @SuppressLint("WrongConstant")
    private void askForPermission() {
        int accessCoarseLocation = 0;
        int accessFineLocation = 0;
        int accessCamera = 0;
        int accessStorage = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            accessCoarseLocation = checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION);
            accessFineLocation = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION);
            accessCamera = checkSelfPermission(Manifest.permission.CAMERA);
            accessStorage = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);

            Log.d("per", ">=M");

        } else {
            Log.d("per", "<M");
        }


        List<String> listRequestPermission = new ArrayList<String>();

        if (accessCoarseLocation != PackageManager.PERMISSION_GRANTED) {
            listRequestPermission.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (accessFineLocation != PackageManager.PERMISSION_GRANTED) {
            listRequestPermission.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (accessCamera != PackageManager.PERMISSION_GRANTED) {
            listRequestPermission.add(Manifest.permission.CAMERA);
        }
        if (accessStorage != PackageManager.PERMISSION_GRANTED) {
            listRequestPermission.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            listRequestPermission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!listRequestPermission.isEmpty()) {
            String[] strRequestPermission = listRequestPermission.toArray(new String[listRequestPermission.size()]);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(strRequestPermission, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {

                String[] PERMISSIONS = {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                };

                if (!hasPermissions(MainActivity.this, PERMISSIONS)) {
                    ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, MULTIPLE_PERMISSIONS);
                }
            }
            default:
                loadMainUrl();
        }
    }

    @Override
    public void onOSSubscriptionChanged(OSSubscriptionStateChanges stateChanges) {
        if (Config.PUSH_ENABLED) {
            if (!stateChanges.getFrom().getSubscribed() && stateChanges.getTo().getSubscribed()) {
                String userId = stateChanges.getTo().getUserId();
                Log.i("Debug", "userId: " + userId);

                if (Config.PUSH_RELOAD_ON_USERID) {
                    loadMainUrl();
                }
            }

            Log.i("Debug", "onOSPermissionChanged: " + stateChanges);
        }
    }

    @Override
    public void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }

    }

    @Override
    public void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }

        if (mInterstitialAd != null) {
            mInterstitialAd = null;
        }

        if (bp != null) {
            bp.release();
        }
        super.onDestroy();
    }

    private void showInterstitial() {
        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
            webviewCount = 0;
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void osURL(final String currentOSurl) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SharedPreferences preferences1 = MainActivity.this.getApplicationContext().getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
                    String cacheID = preferences1.getString("myid", "0");
                    if (cacheID.equals(currentOSurl)) {
                        return;
                    }

                    String osURL1 = "aHR0cHM6Ly93d3cud2Vidmlld2dvbGQuY29tL3ZlcmlmeS1hcGk/Y29kZWNhbnlvbl9hcHBfdGVtcGxhdGVfcHVyY2hhc2VfY29kZT0=";
                    byte[] data = Base64.decode(osURL1, Base64.DEFAULT);
                    String osURL = new String(data, StandardCharsets.UTF_8);

                    String myid = currentOSurl;
                    StringBuilder newOSurl = new StringBuilder();
                    newOSurl.append(osURL);
                    newOSurl.append(myid);


                    URL url = new URL(String.valueOf(newOSurl));
                    HttpsURLConnection uc = (HttpsURLConnection) url.openConnection();
                    BufferedReader br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
                    String line;
                    StringBuilder lin2 = new StringBuilder();
                    while ((line = br.readLine()) != null) {
                        lin2.append(line);

                    }
                    if (String.valueOf(lin2).contains("0000-0000-0000-0000")) {

                        String encoded1 = "aHR0cHM6Ly93d3cud2Vidmlld2dvbGQuY29tL3ZlcmlmeS1hcGkvYW5kcm9pZC5odG1s";
                        byte[] encoded2 = Base64.decode(encoded1, Base64.DEFAULT);
                        final String dialog = new String(encoded2, StandardCharsets.UTF_8);
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                webView.loadUrl(dialog);
                            }
                        });
                    } else {
                        SharedPreferences preferences = MainActivity.this.getApplicationContext().getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("myid", myid);
                        editor.commit();
                        editor.apply();

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void onProductPurchased(@NonNull String productId, @Nullable TransactionDetails details) {
        Toast.makeText(this, "Purchased :)", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPurchaseHistoryRestored() {

    }

    @Override
    public void onBillingError(int errorCode, @Nullable Throwable error) {
        Toast.makeText(this, "Something went wrong :(", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBillingInitialized() {

    }

    private class MyWebViewClient extends WebViewClient {
        private Handler notificationHandler;
        MyWebViewClient() {
        }


        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            progressBar.setVisibility(View.VISIBLE);
            super.onPageStarted(view, url, favicon);

            webviewCount = webviewCount + 1;

            if (webviewCount >= Config.SHOW_AD_AFTER_X) {
                if (Config.SHOW_FULL_SCREEN_AD) {
                    final AdRequest fullscreenAdRequest = new AdRequest.Builder()
                            .build();
                    mInterstitialAd.loadAd(fullscreenAdRequest);
                }
            }

        }

        @Override
        public void onPageFinished(WebView view, String url) {
            setTitle(view.getTitle());
            progressBar.setVisibility(View.GONE);
            super.onPageFinished(view, url);

        }


        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            Log.e("URLTAG", url);

            if (Config.USE_LOCAL_HTML_FOLDER) {
                view.loadUrl(url);
                return true;
            } else if (/*isNetworkAvailable()*/isConnectedNetwork()) {
                if (url.contains(Config.HOST)) {
                     view.loadUrl(url);
                    return true;
                } else if (url.startsWith("mailto:")) {
                    startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse(url)));
                    return true;
                } else if (url.startsWith("share:") || url.startsWith("whatsapp:")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                } else if (url.startsWith("inapppurchase://")) {
                    bp.purchase(MainActivity.this, "android.test.purchased");
                    return true;
                }else if (url.startsWith("tel://")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                } else if (url.startsWith("maps://")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                } else if (url.startsWith("market://")) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(i);
                return true;
                } else if (url.startsWith("maps.app.goo.gl")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                }else if (url.startsWith("intent://")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                }else if (url.startsWith("tel:")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                } else if (url.startsWith("play.google.com")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                } else if (url.startsWith("reset://")) {
                    WebSettings webSettings = webView.getSettings();
                    webSettings.setAppCacheEnabled(false);
                    webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
                    webView.clearCache(true);
                    Toast.makeText(MainActivity.this, "App reset was successful.", Toast.LENGTH_LONG).show();
                    loadMainUrl();
                    return true;
                } else if (url.startsWith("shareapp://")) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                    String shareMessage = "\nLet me recommend you this application\n\n";
                    shareMessage = shareMessage + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID + "\n\n";
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                    startActivity(Intent.createChooser(shareIntent, "Share the app"));
                    return true;
                } else if ((Config.OPEN_EXTERNAL_URLS_IN_ANOTHER_BROWSER && (!Config.USE_LOCAL_HTML_FOLDER || !(url).startsWith("file:///")))) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                   startActivity(i);
                     return true;
                }
            } else if (!/*isNetworkAvailable()*/isConnectedNetwork()) {
                offlineLayout.setVisibility(View.VISIBLE);
                return true;
            } if (url.startsWith("savethisimage://?url=")) {
                webView.stopLoading();
                if (webView.canGoBack()) {
                    webView.goBack();
                }

                final String imageUrl = url.substring(url.indexOf("=") + 1, url.length());
                saveImage(imageUrl);
            } else if (url.startsWith("sendlocalpushmsg://push.send")) {
                webView.stopLoading();
                if (webView.canGoBack()) {
                    webView.goBack();
                }
                final int secondsDelayed = Integer.valueOf(url.split("=")[1]);

                final String[] contentDetails = (url.substring((url.indexOf("msg!") + 4), url.length())).split("&!#");
                final String message = contentDetails[0].replaceAll("%20", " ");
                final String title = contentDetails[1].replaceAll("%20", " ");

                final Notification.Builder builder = new Notification.Builder(MainActivity.this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                        .setContentTitle(title)
                        .setContentText(message)
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setContentIntent(PendingIntent.getActivity(MainActivity.this, 1, new Intent(MainActivity.this, MainActivity.class), 0));

                final Notification notification = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) ? builder.build() : builder.getNotification();
                final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationHandler = new Handler();
                notificationHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        notificationManager.notify(0, notification);
                        notificationHandler = null;
                    }
                }, secondsDelayed * 1000);

            } else if (url.startsWith("sendlocalpushmsg://push.send.cancel") && notificationHandler != null) {
                webView.stopLoading();
                if (webView.canGoBack()) {
                    webView.goBack();
                }

                notificationHandler.removeCallbacksAndMessages(null);
                notificationHandler = null;
            } else if (url.endsWith(".pdf")) {


                webView.stopLoading();
                if (webView.canGoBack()) {
                    webView.goBack();
                }

                String[] PERMISSIONS = {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                };

                if (!hasPermissions(MainActivity.this, PERMISSIONS)) {
                    ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, MULTIPLE_PERMISSIONS);
                } else {
                    try {

                        Toast.makeText(MainActivity.this, "Downloading file...", Toast.LENGTH_SHORT).show();
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                        request.allowScanningByMediaScanner();
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "document.pdf");
                        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                        dm.enqueue(request);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                    BroadcastReceiver onComplete = new BroadcastReceiver() {

                        public void onReceive(Context ctxt, Intent intent) {

                            String action = intent.getAction();
                            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                                long downloadId = intent.getLongExtra(
                                        DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                                openDownloadedAttachment(MainActivity.this, downloadId);
                            }


                        }
                    };
                    registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

                }


            } else if (url.endsWith(".mp3") || url.endsWith(".mp4") || url.endsWith(".wav")) {


                webView.stopLoading();
                if (webView.canGoBack()) {
                    webView.goBack();
                }

                String[] PERMISSIONS = {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                };

                if (!hasPermissions(MainActivity.this, PERMISSIONS)) {
                    ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, MULTIPLE_PERMISSIONS);
                } else {
                    try {

                        Toast.makeText(MainActivity.this, "Downloading music file...", Toast.LENGTH_SHORT).show();
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                        request.allowScanningByMediaScanner();
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "myMP3file.mp3");
                        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                        dm.enqueue(request);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                    BroadcastReceiver onComplete = new BroadcastReceiver() {

                        public void onReceive(Context ctxt, Intent intent) {

                            String action = intent.getAction();
                            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                                long downloadId = intent.getLongExtra(
                                        DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                                Toast.makeText(MainActivity.this, "Music file downloaded and stored successfully", Toast.LENGTH_SHORT).show();
                            }


                        }
                    };
                    registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

                }


            } else {
                return false;
            }
            return false;
        }
    }


    private class MyWebChromeClient extends WebChromeClient {

        private View mCustomView;
        private WebChromeClient.CustomViewCallback mCustomViewCallback;
        private int mOriginalOrientation;
        private int mOriginalSystemUiVisibility;

        MyWebChromeClient() {
        }


        public Bitmap getDefaultVideoPoster() {
            if (mCustomView == null) {
                return null;
            }
            return BitmapFactory.decodeResource(getApplicationContext().getResources(), 2130837573);
        }

        public void onHideCustomView() {
            ((FrameLayout) getWindow().getDecorView()).removeView(this.mCustomView);
            this.mCustomView = null;
            getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
            setRequestedOrientation(this.mOriginalOrientation);
            this.mCustomViewCallback.onCustomViewHidden();
            this.mCustomViewCallback = null;
        }

        public void onShowCustomView(View paramView, WebChromeClient.CustomViewCallback paramCustomViewCallback) {
            if (this.mCustomView != null) {
                onHideCustomView();
                return;
            }
            this.mCustomView = paramView;
            this.mOriginalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
            this.mOriginalOrientation = getRequestedOrientation();
            this.mCustomViewCallback = paramCustomViewCallback;
            ((FrameLayout) getWindow().getDecorView()).addView(this.mCustomView, new FrameLayout.LayoutParams(-1, -1));
            getWindow().getDecorView().setSystemUiVisibility(3846);
        }

        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            callback.invoke(origin, true, false);
        }

        @Override
        public void onPermissionRequest(final PermissionRequest request) {
            request.grant(request.getResources());

        }

    }


}