package ru.terra.discosuspension;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import ru.terra.discosuspension.activity.ConfigActivity;
import ru.terra.discosuspension.activity.DiscoAcitivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_main);
    }

    public void start(View view) {
        startActivity(new Intent(this, DiscoAcitivity.class));
    }

    public void config(View view) {
        startActivity(new Intent(this, ConfigActivity.class));
    }
}
