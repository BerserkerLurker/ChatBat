package com.dev.kallinikos.chatbat;

import android.app.ProgressDialog;
import android.icu.text.DateFormat;
import android.icu.text.StringPrepParseException;
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
import java.util.HashMap;
import java.util.Map;


public class ProfileActivity extends AppCompatActivity {

    private ImageView mProfileImage;
    private TextView mProfileName;
    private TextView mProfileStatus;
    private TextView mProfileFriendsCount;
    private Button mProfileSendRequestBtn;
    private Button mDeclineBtn;

    private DatabaseReference mRootRef;
    private DatabaseReference mUsersDB;
    private DatabaseReference mFriendReqDB;
    private FirebaseUser mCurrentUser;
    private DatabaseReference mFriendDB;
    private DatabaseReference mNotificationDB;

    private ProgressDialog mProgress;

    private String mCurrentState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        final String userId = getIntent().getStringExtra("userid");

        mRootRef = FirebaseDatabase.getInstance().getReference();
        mUsersDB = FirebaseDatabase.getInstance().getReference().child("Users").child(userId);
        mFriendReqDB = FirebaseDatabase.getInstance().getReference().child("Friend_req");
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mFriendDB = FirebaseDatabase.getInstance().getReference().child("Friends");
        mNotificationDB = FirebaseDatabase.getInstance().getReference().child("notifications");

        mProfileImage = (ImageView)findViewById(R.id.profile_display_image);
        mProfileName = (TextView)findViewById(R.id.profile_display_name);
        mProfileStatus = (TextView)findViewById(R.id.profile_status);
        mProfileFriendsCount = (TextView)findViewById(R.id.profile_total_friends);
        mProfileSendRequestBtn = (Button)findViewById(R.id.profile_request_btn);
        mDeclineBtn = (Button)findViewById(R.id.profile_decline_request_btn);

        mDeclineBtn.setVisibility(View.INVISIBLE);
        mDeclineBtn.setEnabled(false);


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

                                mDeclineBtn.setVisibility(View.VISIBLE);
                                mDeclineBtn.setEnabled(true);

                            }else if(reqType.equals("sent")){
                                mCurrentState = "req_sent";
                                mProfileSendRequestBtn.setText("Cancel Friend Request");

                                mDeclineBtn.setVisibility(View.INVISIBLE);
                                mDeclineBtn.setEnabled(false);
                            }

                            mProgress.dismiss();
                        }else{
                            mFriendDB.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.hasChild(userId)){
                                        mCurrentState = "friends";
                                        mProfileSendRequestBtn.setText("Unfriend this Person");

                                        mDeclineBtn.setVisibility(View.INVISIBLE);
                                        mDeclineBtn.setEnabled(false);
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

                    DatabaseReference newNotificationRef = mRootRef.child("notifications").child(userId).push();
                    String newNotificationId = newNotificationRef.getKey();


                    HashMap<String, String> notificationData = new HashMap<String, String>();
                    notificationData.put("from", mCurrentUser.getUid());
                    notificationData.put("type", "request");


                    Map requestMap = new HashMap<>();
                    requestMap.put("Friend_req/" + mCurrentUser.getUid() + "/" + userId + "/request_type", "sent");
                    requestMap.put("Friend_req/" + userId + "/" + mCurrentUser.getUid() + "/request_type", "received");
                    requestMap.put("notifications/" + userId + "/" + newNotificationId, notificationData);


                    mRootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                            if(databaseError != null){
                                Toast.makeText(ProfileActivity.this, "Error : sending request", Toast.LENGTH_SHORT).show();
                            }

                            mProfileSendRequestBtn.setEnabled(true);
                            mCurrentState = "req_sent";
                            mProfileSendRequestBtn.setText("Cancel Friend Request");


                        }
                    });

                }

                // Cancel Request State
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

                                    mDeclineBtn.setVisibility(View.INVISIBLE);
                                    mDeclineBtn.setEnabled(false);
                                }
                            });
                        }
                    });
                }


                // Request received state
                if(mCurrentState.equals("req_received")){

                    final String currentDate = java.text.DateFormat.getDateTimeInstance().format(new Date());

                    Map friendMap = new HashMap();
                    friendMap.put("Friends/" + mCurrentUser.getUid() + "/" + userId + "/date", currentDate);
                    friendMap.put("Friends/" + userId + "/" + mCurrentUser.getUid() + "/date", currentDate);

                    friendMap.put("Friend_req/" + mCurrentUser.getUid() + "/" + userId, null);
                    friendMap.put("Friend_req/" + userId + "/" +mCurrentUser.getUid(), null);

                    mRootRef.updateChildren(friendMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if (databaseError == null)   {
                                mProfileSendRequestBtn.setEnabled(true);
                                mCurrentState = "friends";
                                mProfileSendRequestBtn.setText("Unfriend this person");

                                mDeclineBtn.setVisibility(View.INVISIBLE);
                                mDeclineBtn.setEnabled(false);
                            }else {
                                String error = databaseError.getMessage();
                                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });


                }


                // Unfriend
                if(mCurrentState.equals("friends")){

                    Map unfriendMap = new HashMap();
                    unfriendMap.put("Friends/" + mCurrentUser.getUid() + "/" + userId, null);
                    unfriendMap.put("Friends/" + userId + "/" + mCurrentUser.getUid(), null);

                    mRootRef.updateChildren(unfriendMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if (databaseError == null)   {
                                mCurrentState = "not_friends";
                                mProfileSendRequestBtn.setText("Send friend request");

                                mDeclineBtn.setVisibility(View.INVISIBLE);
                                mDeclineBtn.setEnabled(false);
                            }else {
                                String error = databaseError.getMessage();
                                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                            }

                            mProfileSendRequestBtn.setEnabled(true);

                        }
                    });
                }
            }
        });

    }


    // TODO: online not functioning correctly
    @Override
    protected void onStart() {
        super.onStart();

        if(mCurrentUser != null){
            mRootRef.child("Users").child(mCurrentUser.getUid()).child("online").setValue(true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mCurrentUser != null) {
            mRootRef.child("Users").child(mCurrentUser.getUid()).child("online").setValue(false);
        }

    }
}
