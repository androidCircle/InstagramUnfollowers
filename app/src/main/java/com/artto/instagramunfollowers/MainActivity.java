package com.artto.instagramunfollowers;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import dev.niekirk.com.instagram4android.Instagram4Android;
import dev.niekirk.com.instagram4android.requests.InstagramGetUserFollowersRequest;
import dev.niekirk.com.instagram4android.requests.InstagramGetUserFollowingRequest;
import dev.niekirk.com.instagram4android.requests.InstagramUnfollowRequest;
import dev.niekirk.com.instagram4android.requests.payload.InstagramGetUserFollowersResult;
import dev.niekirk.com.instagram4android.requests.payload.InstagramUserSummary;

public class MainActivity extends AppCompatActivity {

    DelayedProgressDialog spinner = new DelayedProgressDialog();
    Toolbar toolbar;
    SearchView searchView;
    TextView tvUsername;
    SwipeRefreshLayout refreshLayout;
    RecyclerView recycler;
    Button bUnfollowAll;

    Instagram4Android instagram;
    Adapter adapter = new Adapter();
    Random random = new Random();

    SharedPreferences.Editor editor;
    SharedPreferences settings;

    AsyncTask undollowingTask;
    boolean isUnfollowingActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initialize();
        if (savedInstanceState != null)
            login(savedInstanceState.getString("username"),
                    savedInstanceState.getString("password"));
        else if (settings.getBoolean("saved", false))
            login(settings.getString("username", ""),
                    settings.getString("password", ""));
        else
            startLoginActivity();
    }

    private void initialize() {
        settings = getSharedPreferences("INSTAGRAM_UNFOLLOWERS", Context.MODE_PRIVATE);
        editor = settings.edit();

        refreshLayout = findViewById(R.id.refreshLayout);
        refreshLayout.setColorSchemeResources(R.color.colorPrimary);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (isUnfollowingActive)
                    undollowingTask.cancel(false);
                loadData();
            }
        });

        toolbar = findViewById(R.id.toolbar);

        recycler = findViewById(R.id.recycler);
        recycler.setHasFixedSize(true);
        recycler.setItemViewCacheSize(30);
        recycler.setDrawingCacheEnabled(true);
        recycler.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        recycler.setLayoutManager(llm);
        recycler.setAdapter(adapter);
        recycler.addOnScrollListener(new OnScrollListener(llm) {
            @Override
            public void onLoadMore() {
                final int count = adapter.loadMore();
                recycler.post(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyItemRangeInserted(adapter.getItemCount() - count, adapter.getItemCount());
                    }
                });
            }
        });

        tvUsername = findViewById(R.id.tvUsername);
        bUnfollowAll = findViewById(R.id.bUnfollowAll);

        searchView = findViewById(R.id.searchView);
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvUsername.setVisibility(View.INVISIBLE);
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                tvUsername.setVisibility(View.VISIBLE);
                return false;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
//                if (newText.length() > 2)
//                    adapter.search(newText);
//                else
//                    adapter.showAll();
                return false;
            }
        });
    }

    private void startLoginActivity() {
        startActivityForResult(new Intent(this, LoginActivity.class), 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK)
            startLoginActivity();
        else
            login(data.getStringExtra("username"),
                    data.getStringExtra("password"));
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
                    tvUsername.setText(username);
                    if (!settings.getBoolean("saved", false)) {
                        editor.putBoolean("saved", true);
                        editor.putString("username", username);
                        editor.putString("password", password);
                        editor.commit();
                    }
                }
            }
        }.execute();
    }

    public void onClickUnfollow(View view) {
        if (isUnfollowingActive)
            undollowingTask.cancel(false);
        else {
            AlertDialog.Builder adb = new AlertDialog.Builder(this)
                    .setTitle(R.string.attention)
                    .setMessage(R.string.dialogUnfollowAll)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            isUnfollowingActive = true;
                            bUnfollowAll.setText(R.string.stop);
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
    }

    public void onClickUsername (final View view) {
        AlertDialog.Builder adb = new AlertDialog.Builder(this)
                .setTitle(R.string.attention)
                .setMessage(R.string.dialogLogOut)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        if (isUnfollowingActive)
                            undollowingTask.cancel(false);
                        startLoginActivity();
                        tvUsername.setText("");
                        adapter.setUsers(new ArrayList<InstagramUserSummary>());
                        bUnfollowAll.setText(R.string.bUnfollowAll);
                        editor.clear();
                        editor.commit();
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
        undollowingTask = new AsyncTask<long[], Void, Void>() {
            @Override
            protected Void doInBackground(long[]... longs) {
                for (long user : longs[0]) {
                    if (isCancelled())
                        return null;

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
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                onStop();
            }

            @Override
            protected void onCancelled() {
                onStop();
            }

            void onStop() {
                bUnfollowAll.setText(getString(R.string.bUnfollowAll));
                isUnfollowingActive = false;
            }
        }.execute(adapter.getUnfollowAllList());
    }

    @SuppressLint("StaticFieldLeak")
    void loadData() {
        new AsyncTask<Void, Void, Void>() {
            ArrayList<InstagramUserSummary> followers = new ArrayList<>();
            ArrayList<InstagramUserSummary> following = new ArrayList<>();

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
                ArrayList<InstagramUserSummary> unfollowers = new ArrayList<>(following);
                for (InstagramUserSummary i : following) {
                    for (InstagramUserSummary j : followers) {
                        if (i.equals(j)) {
                            unfollowers.remove(i);
                            break;
                        }
                    }
                }
                adapter.setUsers(unfollowers);
                spinner.cancel();
                refreshLayout.setRefreshing(false);
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
            }.execute(intent.getLongExtra("username", 0));
        }
    };

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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (instagram != null && instagram.isLoggedIn()) {
            outState.putString("username", instagram.getUsername());
            outState.putString("password", instagram.getPassword());
        }
        super.onSaveInstanceState(outState);
    }
}