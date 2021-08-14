package ru.terra.discosuspension.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import ru.terra.discosuspension.R;
import ru.terra.discosuspension.activity.components.ProtocolSelectionAsyncTask;
import ru.terra.discosuspension.obd.io.bt.BtOBDBackend;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_main);
    }

    public void start(View view) {
        if (checkAndRequestPermissions())
            startActivity(new Intent(this, FourXFourInfoActivity.class));
    }

    public void config(View view) {
        startActivity(new Intent(this, ConfigActivity.class));
    }

    public void selectProtocol(View view) {
        new ProtocolSelectionAsyncTask(this, new BtOBDBackend()).execute();
    }

    private boolean checkAndRequestPermissions() {
        int bt = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH);
        int btAdmin = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN);
        int internet = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
        int filesWrite = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int filesRead = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (bt != PackageManager.PERMISSION_GRANTED
                || btAdmin != PackageManager.PERMISSION_GRANTED
                || internet != PackageManager.PERMISSION_GRANTED
                || filesWrite != PackageManager.PERMISSION_GRANTED
                || filesRead != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.INTERNET,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            return false;
        }
        return true;
    }
}
