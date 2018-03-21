package com.artto.instagramunfollowers;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.ArrayList;

import dev.niekirk.com.instagram4android.requests.payload.InstagramUserSummary;

public class Adapter extends RecyclerView.Adapter<Adapter.UserViewHolder> {

    private ArrayList<InstagramUserSummary> fullList = new ArrayList<>();
    private ArrayList<InstagramUserSummary> users = new ArrayList<>();

    void setUsers(ArrayList<InstagramUserSummary> users) {
        this.users.clear();
        fullList = users;
        loadMore();
        notifyDataSetChanged();
    }

    long[] getUnfollowAllList() {
        int count = getItemCount() >= 50 ? 50 : getItemCount();
        if (count == 0)
            return new long[] {0};

        long[] result = new long[count];
        for (int i = 0; i < count; i++)
            result[i] = users.get(i).getPk();
        return result;
    }

    void removeItem(final int position) {
        users.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, users.size(), null);
    }

    int loadMore() {
        if (fullList.isEmpty())
            return 0;

        int count = fullList.size() >= 50 ? 50 : fullList.size();
        for (; count > 0; count--) {
            users.add(fullList.get(0));
            fullList.remove(0);
        }
        return count;
    }

    private Intent instProfileIntent(final String username, Context context) {
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
    public void onBindViewHolder(final UserViewHolder holder, final int i) {
        holder.username.setText(users.get(i).getUsername());
        Glide.with(holder.imageView.getContext())
                .load(users.get(i).getProfile_pic_url())
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.imageView);

        holder.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final int position = holder.getAdapterPosition();
                holder.button.getContext().sendBroadcast(new Intent("com.artto.instagramunfollowers.UNFOLLOW")
                        .putExtra("username", users.get(position).getPk()));
                removeItem(position);
            }
        });

        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = holder.layout.getContext();
                context.startActivity(instProfileIntent(users.get(holder.getAdapterPosition()).getUsername(), context));
            }
        });
    }

    @Override
    public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_person, parent, false);
        return new UserViewHolder(v);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layout;
        ImageView imageView;
        TextView username;
        Button button;
        UserViewHolder(View itemView) {
            super(itemView);
            layout = itemView.findViewById(R.id.layout);
            imageView = itemView.findViewById(R.id.person_photo);
            username = itemView.findViewById(R.id.cvUsername);
            button = itemView.findViewById(R.id.button);
        }
    }
}