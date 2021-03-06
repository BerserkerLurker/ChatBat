package com.dev.kallinikos.chatbat;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class StartActivity extends AppCompatActivity {

    private Button mRegBtn;
    private Button mSignInBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        mRegBtn = (Button) findViewById(R.id.start_reg_btn);
        mSignInBtn = (Button) findViewById(R.id.start_signin_btn);

        mRegBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent reg_intent = new Intent(StartActivity.this, RegisterActivity.class);
                startActivity(reg_intent);
            }
        });

        mSignInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sign_in_intent = new Intent(StartActivity.this, LoginActivity.class);
                startActivity(sign_in_intent);
            }
        });
    }
}
