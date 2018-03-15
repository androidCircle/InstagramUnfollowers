package com.artto.instagramunfollowers;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.util.Random;
import java.util.Vector;

import dev.niekirk.com.instagram4android.Instagram4Android;
import dev.niekirk.com.instagram4android.requests.InstagramGetUserFollowersRequest;
import dev.niekirk.com.instagram4android.requests.InstagramGetUserFollowingRequest;
import dev.niekirk.com.instagram4android.requests.InstagramUnfollowRequest;
import dev.niekirk.com.instagram4android.requests.payload.InstagramGetUserFollowersResult;
import dev.niekirk.com.instagram4android.requests.payload.InstagramUserSummary;

public class MainActivity extends AppCompatActivity {

    DelayedProgressDialog spinner = new DelayedProgressDialog();
    RecyclerView recycler;
    Button bUnfollowAll;
    Instagram4Android instagram;
    Adapter adapter = new Adapter();
    Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initialize();
        if (savedInstanceState == null)
            startLoginActivity();
        else
            login(savedInstanceState.getString("username"),
                    savedInstanceState.getString("password"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(unfollowReceiver, new IntentFilter("com.artto.instagramunfollowers.UNFOLLOW"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(unfollowReceiver);
    }

    private void initialize() {
        recycler = findViewById(R.id.recycler);
        recycler.setHasFixedSize(true);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        bUnfollowAll = findViewById(R.id.bUnfollowAll);
    }

    private void startLoginActivity() {
        startActivityForResult(new Intent(this, LoginActivity.class), 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK)
            startLoginActivity();
        else
            login(data.getStringExtra("username"), data.getStringExtra("password"));
    }

    @SuppressLint("StaticFieldLeak")
    private void login(final String username, final String password) {
        spinner.show(getSupportFragmentManager(), "login");
        instagram = Instagram4Android.builder()
                .username(username)
                .password(password)
                .build();
        instagram.setup();

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    instagram.login();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (!instagram.isLoggedIn()) {
                    spinner.cancel();
                    Toast.makeText(getApplicationContext(), R.string.loginError, Toast.LENGTH_LONG).show();
                    startLoginActivity();
                } else {
                    loadData();
                }
            }
        }.execute();
    }

    public void onClickUnfollow(View view) {
        showUnfollowDialog();
    }

    private void showUnfollowDialog() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this)
                .setTitle(R.string.attention)
                .setMessage(R.string.dialogUnfollowAll)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        unfollowAll();
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.cancel();
                    }
                });
        adb.show();
    }

    @SuppressLint("StaticFieldLeak")
    void unfollowAll() {
        new AsyncTask<long[], Void, Void>() {
            @Override
            protected Void doInBackground(long[]... longs) {
                for (long user : longs[0]) {
                    try {
                        Thread.sleep(random.nextInt(5) * 1000);
                        instagram.sendRequest(new InstagramUnfollowRequest(user));
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        publishProgress();
                    }
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Void... values) {
                adapter.removeItem(0);
                bUnfollowAll.setText(getString(R.string.bUnfollowAllCount,
                        adapter.getItemCount()));
            }
        }.execute(adapter.getUnfollowAllList());
    }

    @SuppressLint("StaticFieldLeak")
    void loadData() {
        new AsyncTask<Void, Void, Void>() {
            Vector<InstagramUserSummary> followers = new Vector<>();
            Vector<InstagramUserSummary> following = new Vector<>();

            @Override
            protected Void doInBackground(Void... voids) {
                InstagramGetUserFollowersResult result;
                final long userId = instagram.getUserId();
                try {
                    result = instagram.sendRequest(new InstagramGetUserFollowersRequest(userId));
                    followers.addAll(result.getUsers());
                    while (result.getNext_max_id() != null){
                        result = instagram.sendRequest(new InstagramGetUserFollowersRequest(userId, result.getNext_max_id()));
                        followers.addAll(result.getUsers());
                    }

                    result = instagram.sendRequest(new InstagramGetUserFollowingRequest(userId));
                    following.addAll(result.getUsers());
                    while (result.getNext_max_id() != null){
                        result = instagram.sendRequest(new InstagramGetUserFollowingRequest(userId, result.getNext_max_id()));
                        following.addAll(result.getUsers());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                Vector<InstagramUserSummary> unfollowers = new Vector<>(following);
                for (InstagramUserSummary i : following) {
                    for (InstagramUserSummary j : followers) {
                        if (i.equals(j)) {
                            unfollowers.remove(i);
                            break;
                        }
                    }
                }
                adapter.setUsers(unfollowers);
                bUnfollowAll.setText(getString(R.string.bUnfollowAllCount, unfollowers.size()));
                spinner.cancel();
            }
        }.execute();
    }

    BroadcastReceiver unfollowReceiver = new BroadcastReceiver() {
        @SuppressLint("StaticFieldLeak")
        @Override
        public void onReceive(Context context, Intent intent) {
            new AsyncTask<Long, Integer, Void>() {
                @Override
                protected Void doInBackground(Long... longs) {
                    try {
                        instagram.sendRequest(new InstagramUnfollowRequest(longs[0]));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    bUnfollowAll.setText(getString(R.string.bUnfollowAllCount,
                            adapter.getItemCount()));
                }
            }.execute(intent.getLongExtra("username", 0));
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (instagram != null && instagram.isLoggedIn()) {
            outState.putString("username", instagram.getUsername());
            outState.putString("password", instagram.getPassword());
        }
        super.onSaveInstanceState(outState);
    }
}
