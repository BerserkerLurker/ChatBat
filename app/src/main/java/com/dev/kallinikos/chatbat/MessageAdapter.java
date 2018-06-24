package com.dev.kallinikos.chatbat;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;



public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder>{

    private List<Messages> mMessageList;
//    private FirebaseAuth mAuth;
    private DatabaseReference mUserDatabase;

    public MessageAdapter(List<Messages> mMessageList){
        this.mMessageList = mMessageList;
    }


    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_single_layout, parent, false);

        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final MessageViewHolder holder, int position) {

//        mAuth = FirebaseAuth.getInstance();


//        String currentUserId = mAuth.getCurrentUser().getUid();

        Messages c = mMessageList.get(position);

        String fromUser = c.getFrom();
        String messageType = c.getType();

        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(fromUser);

        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String name = dataSnapshot.child("name").getValue().toString();
                String image = dataSnapshot.child("thumb_image").getValue().toString();

                holder.displayName.setText(name);

                Picasso.with(holder.profileImage.getContext()).load(image)
                        .placeholder(R.drawable.profile).into(holder.profileImage);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

//        if (fromUser != null) {
//            if (fromUser.equals(currentUserId)) {
//
//                holder.messageText.setBackgroundColor(Color.WHITE);
//                holder.messageText.setTextColor(Color.BLACK);
//
//            } else {
//
//                holder.messageText.setBackgroundResource(R.drawable.message_type_background);
//                holder.messageText.setTextColor(Color.WHITE);
//
//            }
//        }
//        holder.messageText.setText(c.getMessage());


        if(messageType.equals("text")) {

            holder.messageText.setText(c.getMessage());
            holder.messageImage.setVisibility(View.GONE);


        } else {

            holder.messageText.setVisibility(View.INVISIBLE);
            Picasso.with(holder.profileImage.getContext()).load(c.getMessage())
                    .placeholder(R.drawable.profile).into(holder.messageImage);

        }

    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {

        public TextView messageText;
        public CircleImageView profileImage;
        public TextView displayName;
        public ImageView messageImage;


        public MessageViewHolder(View itemView) {
            super(itemView);
            messageText = (TextView)itemView.findViewById(R.id.messageTextLayout);
            profileImage = (CircleImageView)itemView.findViewById(R.id.messageProfileLayout);
            displayName = (TextView) itemView.findViewById(R.id.nameTextLayout);
            messageImage = (ImageView) itemView.findViewById(R.id.messageImageLayout);
        }
    }
}
