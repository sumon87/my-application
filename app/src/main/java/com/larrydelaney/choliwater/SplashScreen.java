package com.larrydelaney.choliwater;

import android.content.Intent;
import android.gold.webview.codecanyon.com.webview.R;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import static com.larrydelaney.choliwater.Config.SPLASH_TIMEOUT;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);
        final ImageView splash = findViewById(R.id.splash);

        if (Config.SPLASH_SCREEN_ACTIVATED) {


            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }


            splash.setImageResource(R.mipmap.splash);
            getWindow().setNavigationBarColor(getResources().getColor(R.color.colorWhite));
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorWhite));
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    SplashScreen.this.startActivity(new Intent(SplashScreen.this, MainActivity.class));
                    SplashScreen.this.finish();
                }
            }, SPLASH_TIMEOUT);
        }
        else {
            splash.setVisibility(View.GONE);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    SplashScreen.this.startActivity(new Intent(SplashScreen.this, MainActivity.class));
                    SplashScreen.this.finish();
                }
            }, 0);
        }

    }
}
