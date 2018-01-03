package com.dev.kallinikos.chatbat;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class FriendsFragment extends Fragment {

    private RecyclerView mFriendsList;

    private DatabaseReference mFriendsDB;
    private DatabaseReference mUsersDB;

    private FirebaseAuth mAuth;

    private String mCurrentUserId;

    private View mMainView;


    public FriendsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mMainView = inflater.inflate(R.layout.fragment_friends, container, false);

        mFriendsList = (RecyclerView) mMainView.findViewById(R.id.friends_list);
        mAuth = FirebaseAuth.getInstance();

        mCurrentUserId = mAuth.getCurrentUser().getUid();

        mFriendsDB = FirebaseDatabase.getInstance().getReference().child("Friends").child(mCurrentUserId);
        mFriendsDB.keepSynced(true);

        mUsersDB = FirebaseDatabase.getInstance().getReference().child("Users");
        mUsersDB.keepSynced(true);

        mFriendsList.setHasFixedSize(true);
        mFriendsList.setLayoutManager(new LinearLayoutManager(getContext()));

        return mMainView;
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerAdapter<Friends, FriendsViewHolder> friendsRecyclerViewAdapter = new FirebaseRecyclerAdapter<Friends, FriendsViewHolder>(

                Friends.class,
                R.layout.users_single_layout,
                FriendsViewHolder.class,
                mFriendsDB

        ) {
            @Override
            protected void populateViewHolder(final FriendsViewHolder friendsViewHolder, final Friends friends, int position) {
                friendsViewHolder.setDate(friends.getDate());

                final String list_user_id = getRef(position).getKey();

                mUsersDB.child(list_user_id).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        final String userName = dataSnapshot.child("name").getValue().toString();
                        String userThumb = dataSnapshot.child("thumb_image").getValue().toString();

                        if(dataSnapshot.hasChild("online")) {
                            Boolean userOnline = (Boolean)dataSnapshot.child("online").getValue();
                            friendsViewHolder.setUserOnline(userOnline);
                        }


                        friendsViewHolder.setName(userName);
                        friendsViewHolder.setUserImage(userThumb, getContext());

                        friendsViewHolder.mView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                                CharSequence options[] = new CharSequence[]{"Open profile", "Send message"};

                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                                builder.setTitle("Select options");
                                builder.setItems(options, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        // Click events for options
                                        if (i == 0){
                                            Intent profileIntent = new Intent(getContext(), ProfileActivity.class);
                                            profileIntent.putExtra("userid", list_user_id);
                                            startActivity(profileIntent);

                                        }

                                        if (i == 1){
                                            Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                            chatIntent.putExtra("userid", list_user_id);
                                            chatIntent.putExtra("username", userName);
                                            startActivity(chatIntent);
                                        }

                                    }
                                });

                                builder.show();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        };

        mFriendsList.setAdapter(friendsRecyclerViewAdapter);
    }

    public static class FriendsViewHolder extends RecyclerView.ViewHolder{
        View  mView;


        public FriendsViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
        }

        public void setDate(String date){
            TextView userNameView = (TextView) mView.findViewById(R.id.users_single_status);
            userNameView.setText(date);
        }

        public void setName(String name){

            TextView userNameView = (TextView)mView.findViewById(R.id.users_single_name);
            userNameView.setText(name);
        }

        public void setUserImage(String thumbImage, Context ctx){
            CircleImageView userImageView = (CircleImageView)mView.findViewById(R.id.users_single_img);
            Picasso.with(ctx).load(thumbImage).placeholder(R.drawable.profile).into(userImageView);

        }

        public void setUserOnline(boolean onlineStatus){

            ImageView userOnlineView =(ImageView)mView.findViewById(R.id.users_single_online_icon);

            if(onlineStatus){
                userOnlineView.setVisibility(View.VISIBLE);
            }else {
                userOnlineView.setVisibility(View.INVISIBLE);
            }

        }
    }
}
