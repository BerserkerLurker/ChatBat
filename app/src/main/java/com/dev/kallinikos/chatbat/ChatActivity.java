package com.dev.kallinikos.chatbat;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextClock;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private String mChatUser;
    private String mUserName;

    private Toolbar mChatToolbar;

    private DatabaseReference mRootRef;

    private TextView mTitleView;
    private TextView mLastSeenView;
    private CircleImageView mProfileImage;

    private FirebaseAuth mAuth;
    private String mCurrentUserId;

    private ImageButton mChatAddBtn;
    private ImageButton mChatSendBtn;
    private EditText mChatMessageView;

    private RecyclerView mMessagesList;
    private SwipeRefreshLayout mRefreshLayout;

    private final List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager mLinearLayout;
    private MessageAdapter mAdapter;

    private static final int TOTAL_ITEMS_TO_LOAD = 10;
    private int mCurrentPage = 1;


    private int itemPos = 0;
    private String mLastKey = "";
    private String mPrevKey = "";

    private static final int GALLERY_PICK = 1;
    private StorageReference mImageStorage;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mChatToolbar = (Toolbar)findViewById(R.id.chat_app_bar);
        setSupportActionBar(mChatToolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);



        mRootRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        mCurrentUserId = mAuth.getCurrentUser().getUid();
        mImageStorage = FirebaseStorage.getInstance().getReference();


        mChatUser = getIntent().getStringExtra("userid");
        mUserName = getIntent().getStringExtra("username");

        // getSupportActionBar().setTitle(mUserName);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View action_bar_view = inflater.inflate(R.layout.chat_custom_bar,null);

        actionBar.setCustomView(action_bar_view);

        // --- Custom ActionBar Items ---

        mTitleView = (TextView) findViewById(R.id.custom_bar_title);
        mLastSeenView = (TextView) findViewById(R.id.custom_bar_seen);
        mProfileImage = (CircleImageView) findViewById(R.id.custom_bar_image);

        mChatAddBtn = (ImageButton) findViewById(R.id.chatAddBtn);
        mChatSendBtn = (ImageButton) findViewById(R.id.chatSendBtn);
        mChatMessageView = (EditText) findViewById(R.id.chatMsgView);


        mAdapter = new MessageAdapter(messagesList);

        mMessagesList = (RecyclerView) findViewById(R.id.messagesList);
        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.messageSwipeLayout);

        mLinearLayout = new LinearLayoutManager(this);

        mMessagesList.setHasFixedSize(true);
        mMessagesList.setLayoutManager(mLinearLayout);

        mMessagesList.setAdapter(mAdapter);

        loadMessages();
        
        mTitleView.setText(mUserName);

        mRootRef.child("Users").child(mChatUser).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                String online = dataSnapshot.child("online").getValue().toString();
                String image = dataSnapshot.child("image").getValue().toString();

                if (online.equals("true")){
                    mLastSeenView.setText("Online");
                }else {

                    GetTimeAgo getTimeAgo = new GetTimeAgo();

                    long lastTime = Long.parseLong(online);

                    String lastSeenTime = getTimeAgo.getTimeAgo(lastTime, getApplicationContext());

                    mLastSeenView.setText(lastSeenTime);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mRootRef.child("Chat").child(mCurrentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (!dataSnapshot.hasChild(mChatUser)){

                    Map chatAddMap = new HashMap();
                    chatAddMap.put("seen", false);
                    chatAddMap.put("timestamp", ServerValue.TIMESTAMP);

                    Map chatUserMap = new HashMap();
                    chatUserMap.put("Chat/" + mCurrentUserId + "/" + mChatUser, chatAddMap);
                    chatUserMap.put("Chat/" + mChatUser + "/" + mCurrentUserId, chatAddMap);

                    mRootRef.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                            if (databaseError != null){
                                Log.d("Chat Log", databaseError.getMessage());
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mChatSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });


        mChatAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(galleryIntent, "Select Image"), GALLERY_PICK);
            }
        });

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                mCurrentPage++;
                itemPos = 0;

                //messagesList.clear();

                loadMoreMessages();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == GALLERY_PICK && resultCode == RESULT_OK){

            Uri imageUri = data.getData();

            final String currentUserRef = "messages/" + mCurrentUserId + "/" + mChatUser;
            final String chatUserRef = "messages/" + mChatUser + "/" + mCurrentUserId;

            DatabaseReference userMessagePush = mRootRef.child("messages").child(mCurrentUserId).child(mChatUser).push();

            final String pushId = userMessagePush.getKey();

            StorageReference filePath = mImageStorage.child("message_images").child(pushId + ".jpg");

            filePath.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if (task.isSuccessful()){

                        String downloadUrl = task.getResult().getDownloadUrl().toString();

                        Map messageMap = new HashMap();
                        messageMap.put("message", downloadUrl);
                        messageMap.put("seen", false);
                        messageMap.put("type", "image");
                        messageMap.put("time", ServerValue.TIMESTAMP);
                        messageMap.put("from", mCurrentUserId);

                        Map messageUserMap = new HashMap();
                        messageUserMap.put(currentUserRef+ "/" + pushId, messageMap);
                        messageUserMap.put(chatUserRef+ "/" + pushId, messageMap);

                        mChatMessageView.setText("");

                        mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if (databaseError != null){
                                    Log.d("Chat Log", databaseError.getMessage());
                                }
                            }
                        });
                    }


                }
            });
        }
    }

    private void loadMoreMessages() {

        DatabaseReference messageRef = mRootRef.child("messages").child(mCurrentUserId).child(mChatUser);

        Query messageQuery = messageRef.orderByKey().endAt(mLastKey).limitToLast(10);

        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                Messages message = dataSnapshot.getValue(Messages.class);
                String messageKey = dataSnapshot.getKey();

                if (!mPrevKey.equals(messageKey)){

                    messagesList.add(itemPos++, message);

                }else{

                    mPrevKey = mLastKey;

                }

                if(itemPos == 1){

                    mLastKey = messageKey;
                }




                mAdapter.notifyDataSetChanged();

                mRefreshLayout.setRefreshing(false);

                mLinearLayout.scrollToPositionWithOffset(10, 0);

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void loadMessages() {


        DatabaseReference messageRef = mRootRef.child("messages").child(mCurrentUserId).child(mChatUser);

        Query messageQuery = messageRef.limitToLast(mCurrentPage * TOTAL_ITEMS_TO_LOAD);

        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                Messages message = dataSnapshot.getValue(Messages.class);

                itemPos++;

                if(itemPos == 1){
                    mLastKey = dataSnapshot.getKey();
                    mPrevKey = mLastKey;
                }

                messagesList.add(message);
                mAdapter.notifyDataSetChanged();

                mMessagesList.scrollToPosition(messagesList.size() - 1);

                mRefreshLayout.setRefreshing(false);

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void sendMessage() {

        String message = mChatMessageView.getText().toString();

        String currentUserRef = "messages/" + mCurrentUserId + "/" + mChatUser;
        String chatUserRef = "messages/" + mChatUser + "/" + mCurrentUserId;

        DatabaseReference userMessagePush = mRootRef.child("messages").child(mCurrentUserId).child(mChatUser).push();

        String pushId = userMessagePush.getKey();

        if(!TextUtils.isEmpty(message)){
            Map messageMap = new HashMap();
            messageMap.put("message", message);
            messageMap.put("seen", false);
            messageMap.put("type", "text");
            messageMap.put("time", ServerValue.TIMESTAMP);
            messageMap.put("from", mCurrentUserId);

            Map messageUserMap = new HashMap();
            messageUserMap.put(currentUserRef+ "/" + pushId, messageMap);
            messageUserMap.put(chatUserRef+ "/" + pushId, messageMap);

            mChatMessageView.setText("");

            mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if (databaseError != null){
                        Log.d("Chat Log", databaseError.getMessage());
                    }
                }
            });
        }
    }
}
