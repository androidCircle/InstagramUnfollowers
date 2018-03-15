package com.artto.instagramunfollowers;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class LoginActivity extends AppCompatActivity {

    private EditText etLogin;
    private EditText etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etLogin = findViewById(R.id.etLogin);
        etPassword = findViewById(R.id.etPassword);
        if (savedInstanceState != null) {
            etLogin.setText(savedInstanceState.getString("username"));
            etPassword.setText(savedInstanceState.getString("password"));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("username", etLogin.getText().toString());
        outState.putString("password", etPassword.getText().toString());
        super.onSaveInstanceState(outState);
    }

    public void onClick(View view) {
        setResult(RESULT_OK, new Intent()
                .putExtra("username", etLogin.getText().toString())
                .putExtra("password", etPassword.getText().toString()));
        finish();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
