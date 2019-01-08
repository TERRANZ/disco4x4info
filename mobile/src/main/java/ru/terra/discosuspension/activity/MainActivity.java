package ru.terra.discosuspension.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import ru.terra.discosuspension.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_main);
    }

    public void start(View view) {
        startActivity(new Intent(this, FourXFourInfoActivity.class));
    }

    public void config(View view) {
        startActivity(new Intent(this, ConfigActivity.class));
    }
}
