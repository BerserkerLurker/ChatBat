package com.dev.kallinikos.chatbat;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class ProfileActivity extends AppCompatActivity {

    private TextView mDisplayId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        String userId = getIntent().getStringExtra("userid");

        mDisplayId = (TextView)findViewById(R.id.profile_display_name);
        mDisplayId.setText(userId);
    }
}
