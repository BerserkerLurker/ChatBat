package com.dev.kallinikos.chatbat;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

/**
 * Created by kallinikos on 04/08/17.
 */

public class ChatBat extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Firebase offline

        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        // Picasso offline

        Picasso.Builder builder = new Picasso.Builder(this);
        builder.downloader(new OkHttpDownloader(this, Integer.MAX_VALUE));
        Picasso built = builder.build();
        built.setIndicatorsEnabled(true);
        built.setLoggingEnabled(true);
        Picasso.setSingletonInstance(built);
    }
}
