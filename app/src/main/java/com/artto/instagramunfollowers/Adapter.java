package com.artto.instagramunfollowers;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.Vector;

import dev.niekirk.com.instagram4android.requests.payload.InstagramUserSummary;

public class Adapter extends RecyclerView.Adapter<Adapter.UserViewHolder> {

    private Vector<InstagramUserSummary> users = new Vector<>();
    private Context context;

    void setUsers(Vector<InstagramUserSummary> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    long[] getUnfollowAllList() {
        final int size = users.size();
        long[] result = new long[size];
        for (int i = 0; i < size; i++) {
            result[i] = users.get(i).getPk();
        }
        return result;
    }

    void removeItem(final int position) {
        users.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, users.size());
    }

    @Override
    public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        View v = LayoutInflater.from(context).inflate(R.layout.card_person, parent, false);
        return new UserViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final UserViewHolder holder, final int i) {
        holder.username.setText(users.get(i).getUsername());
        Glide.with(context).load(users.get(i).getProfile_pic_url()).into(holder.imageView);

        holder.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final int position = holder.getAdapterPosition();
                context.sendBroadcast(new Intent("com.artto.instagramunfollowers.UNFOLLOW")
                        .putExtra("username", users.get(position).getPk())
                        .putExtra("count", users.size()));
                removeItem(position);
            }
        });

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final int position = holder.getAdapterPosition();
                context.startActivity(newInstagramProfileIntent(users.get(position).getUsername()));
            }
        });
    }

    private Intent newInstagramProfileIntent(String username) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        try {
            if (context.getPackageManager().getPackageInfo("com.instagram.android", 0) != null) {
                intent.setData(Uri.parse("http://instagram.com/_u/" + username));
                intent.setPackage("com.instagram.android");
                return intent;
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        intent.setData(Uri.parse("http://instagram.com/" + username));
        return intent;
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView imageView;
        TextView username;
        Button button;
        UserViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cvUser);
            imageView = itemView.findViewById(R.id.person_photo);
            username = itemView.findViewById(R.id.cvUsername);
            button = itemView.findViewById(R.id.button);
        }
    }
}