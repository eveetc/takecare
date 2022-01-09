package com.mad.takecare.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mad.takecare.MainActivity;
import com.mad.takecare.R;
import com.parse.ParseUser;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

//        Toolbar myToolbar = (Toolbar) findViewById(R.id.AufgabenToolbar);
//        setSupportActionBar(myToolbar);

        FloatingActionButton backButton = findViewById(R.id.BackButtonSettings);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent1 = new Intent(SettingsActivity.this, MainActivity.class);
//                intent1.putExtra(getString(R.string.usernameParameter), ParseUser.getCurrentUser().getUsername());
//                intent1.putExtra(getString(R.string.viewedUserParameter), ParseUser.getCurrentUser().getUsername());
//                startActivity(intent1);
                finish();
            }
        });

    }
}