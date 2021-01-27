package com.larrydelaney.choliwater;


public class Config {

    // *** Your Purchase Code of CodeCanyon ***
    // 1. Buy a WebViewGold license (https://www.webviewgold.com/download/android) for each app you publish. If your app is going to be free, a "Regular license" is required. If your app will be sold to your users or if you use the In-App Purchases API, an "Extended license" is required. More info: https://codecanyon.net/licenses/standard?ref=onlineappcreator
    // 2. Grab your Purchase Code (this is how to find it quickly: https://help.market.envato.com/hc/en-us/articles/202822600-Where-Is-My-Purchase-Code-)
    // 3. Great! Just enter it here and restart your app:
    // *** Your Purchase Code of CodeCanyon ***
    // 1. Buy a WebViewGold license (https://www.webviewgold.com/download/android) for each app you publish. If your app is going to be free, a "Regular license" is required. If your app will be sold to your users or if you use the In-App Purchases API, an "Extended license" is required. More info: https://codecanyon.net/licenses/standard?ref=onlineappcreator
    // 2. Grab your Purchase Code (this is how to find it quickly: https://help.market.envato.com/hc/en-us/articles/202822600-Where-Is-My-Purchase-Code-)
    // 3. Great! Just enter it here and restart your app:
    public static final String PURCHASECODE = "66829129-c1e1-42cc-865f-67481417c057";
    // 4. Enjoy your app! :)

    /**
     * Main configuration of your WebViewGold app
     */

    // Domain host and subdomain without any https:// or http:// prefixes (e.g. "www.example.org")
    public static final String HOST = "file:///android_asset/index.html";

    // Your URL including https:// or http:// prefix and including www. or any required subdomain (e.g. "https://www.example.org")
    public static final String HOME_URL = "file:///android_asset/index.html";

    // Set a customized UserAgent for WebView URL requests (or leave it empty to use the default Android UserAgent)
    public static final String USER_AGENT = "";

    // Set to "true" to open external links in another browser by default
    public static final boolean OPEN_EXTERNAL_URLS_IN_ANOTHER_BROWSER = false;

    // Set to "true" to clear the WebView cache on each app startup and do not use cached versions of your web app/website
    public static final boolean CLEAR_CACHE_ON_STARTUP = true;

    //Set to "true" to use local "assets/index.html" file instead of URL
    public static final boolean USE_LOCAL_HTML_FOLDER = true;

    //Set to "true" to enable deep linking for App Links (take a look at the documentation for further information)
    public static final boolean IS_DEEP_LINKING_ENABLED = false;

    // Set to "true" to activate the splash screen
    public static final boolean SPLASH_SCREEN_ACTIVATED = false;

    //Set the splash screen timeout time in milliseconds
    public static final int SPLASH_TIMEOUT = 1800;


    /**
     * Dialog options
     */

    // Set the minimum number of days to be passed after the application is installed before the "Rate this app" dialog will be displayed
    public static final int RATE_DAYS_UNTIL_PROMPT = 3;
    // Set the minimum number of application launches before the "Rate this app" dialog will be displayed
    public static final int RATE_LAUNCHES_UNTIL_PROMPT = 3;

    // Set the minimum number of days to be passed after the application is installed before the "Follow on Facebook" dialog will be displayed
    public static final int FACEBOOK_DAYS_UNTIL_PROMPT = 2;
    // Set the minimum number of application launches before the "Rate this app" dialog will be displayed
    public static final int FACEBOOK_LAUNCHES_UNTIL_PROMPT = 4;
    // Set the URL of your Facebook page
    public static final String FACEBOOK_URL = "https://www.facebook.com/PandemoniumProductionsInc";


    /**
     * OneSignal options
     */

    //Set to "true" to activate OneSignal Push (set OneSignal IDs in the build.gradle file)
    public static final boolean PUSH_ENABLED = false;

    //Set to "true" if you want to extend URL request by ?onesignal_push_id=XYZ (set the OneSignal IDs in the build.gradle file)
    public static final boolean PUSH_ENHANCE_WEBVIEW_URL = false;

    //Set to "true" if WebView should be reloaded when the app gets a UserID from OneSignal (set the OneSignal IDs in the build.gradle file)
    public static final boolean PUSH_RELOAD_ON_USERID = false;


    /**
     * AdMob options
     */

    //Set to "true" if you want to display AdMob banner ads (set the AdMob IDs in the strings.xml file)
    public static final boolean SHOW_BANNER_AD = false;

    //Set to "true" if you want to display AdMob fullscreen interstitial ads after X website clicks (set the AdMob IDs in the strings.xml file)
    public static final boolean SHOW_FULL_SCREEN_AD = false;

    //X website clicks for AdMob interstitial ads (set the AdMob IDs in the strings.xml file)
    public static final int SHOW_AD_AFTER_X = 15;

    //Set the Google Play In-App Purchase Key (receive it from Google Play Developer Console)
    public static final String PURCHASE_LICENSE_KEY = "123456789";
}