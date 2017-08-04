package com.dev.kallinikos.chatbat;

import android.app.ProgressDialog;
import android.icu.text.DateFormat;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Date;


public class ProfileActivity extends AppCompatActivity {

    private ImageView mProfileImage;
    private TextView mProfileName;
    private TextView mProfileStatus;
    private TextView mProfileFriendsCount;
    private Button mProfileSendRequestBtn;

    private DatabaseReference mUsersDB;
    private DatabaseReference mFriendReqDB;
    private FirebaseUser mCurrentUser;
    private DatabaseReference mFriendDB;

    private ProgressDialog mProgress;

    private String mCurrentState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        final String userId = getIntent().getStringExtra("userid");

        mUsersDB = FirebaseDatabase.getInstance().getReference().child("Users").child(userId);
        mFriendReqDB = FirebaseDatabase.getInstance().getReference().child("Friend_req");
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mFriendDB = FirebaseDatabase.getInstance().getReference().child("Friends");

        mProfileImage = (ImageView)findViewById(R.id.profile_display_image);
        mProfileName = (TextView)findViewById(R.id.profile_display_name);
        mProfileStatus = (TextView)findViewById(R.id.profile_status);
        mProfileFriendsCount = (TextView)findViewById(R.id.profile_total_friends);
        mProfileSendRequestBtn = (Button)findViewById(R.id.profile_request_btn);


        mCurrentState = "not_friends";



        mProgress = new ProgressDialog(ProfileActivity.this);
        mProgress.setTitle("Loading User Data...");
        mProgress.setMessage("Please wait while we load the user data.");
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.show();

        mUsersDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String displayName = dataSnapshot.child("name").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                String image = dataSnapshot.child("image").getValue().toString();


                mProfileName.setText(displayName);
                mProfileStatus.setText(status);

                Picasso.with(ProfileActivity.this).load(image).placeholder(R.drawable.profile).into(mProfileImage);

                // Friends List / Request Feature
                mFriendReqDB.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if(dataSnapshot.hasChild(userId)){
                            String reqType = dataSnapshot.child(userId).child("request_type").getValue().toString();
                            if(reqType.equals("received")){
                                mCurrentState = "req_received";
                                mProfileSendRequestBtn.setText("Accept Friend Request");
                            }else if(reqType.equals("sent")){
                                mCurrentState = "req_sent";
                                mProfileSendRequestBtn.setText("Cancel Friend Request");
                            }
                            
                            mProgress.dismiss();
                        }else{
                            mFriendDB.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.hasChild(userId)){
                                        mCurrentState = "friends";
                                        mProfileSendRequestBtn.setText("Unfriend this Person");

                                    }
                                    mProgress.dismiss();
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    mProgress.dismiss();
                                }
                            });
                        }



                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        mProfileSendRequestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mProfileSendRequestBtn.setEnabled(false);

                // Not Friends State
                if(mCurrentState.equals("not_friends")){

                    mFriendReqDB.child(mCurrentUser.getUid()).child(userId).child("request_type")
                            .setValue("sent").addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                mFriendReqDB.child(userId).child(mCurrentUser.getUid()).child("request_type")
                                        .setValue("received").addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        mProfileSendRequestBtn.setEnabled(true);
                                        mCurrentState = "req_sent";
                                        mProfileSendRequestBtn.setText("Cancel Friend Request");
                                        Toast.makeText(ProfileActivity.this, "Request sent",Toast.LENGTH_SHORT).show();

                                    }
                                });
                            }else{
                                Toast.makeText(ProfileActivity.this, "Failed to send request",Toast.LENGTH_SHORT).show();
                            }
                            mProfileSendRequestBtn.setEnabled(true);

                        }
                    });

                }

                // Cancel Reqquest State
                if(mCurrentState.equals("req_sent")){

                    mFriendReqDB.child(mCurrentUser.getUid()).child(userId).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mFriendReqDB.child(userId).child(mCurrentUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    mProfileSendRequestBtn.setEnabled(true);
                                    mCurrentState = "not_friends";
                                    mProfileSendRequestBtn.setText("Send Friend Request");
                                }
                            });
                        }
                    });
                }


                // Request received state
                if(mCurrentState.equals("req_received")){

                    final String currentDate = java.text.DateFormat.getDateTimeInstance().format(new Date());
                    mFriendDB.child(mCurrentUser.getUid()).child(userId).setValue(currentDate).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                            mFriendDB.child(userId).child(mCurrentUser.getUid()).setValue(currentDate).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    mFriendReqDB.child(mCurrentUser.getUid()).child(userId).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            mFriendReqDB.child(userId).child(mCurrentUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    mProfileSendRequestBtn.setEnabled(true);
                                                    mCurrentState = "friends";
                                                    mProfileSendRequestBtn.setText("Unfriend this Person");
                                                }
                                            });
                                        }
                                    });

                                }
                            });

                        }
                    });

                }
            }
        });

    }
}
