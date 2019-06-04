package ru.terra.discosuspension.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import de.nitri.gauge.Gauge;
import ru.terra.discosuspension.R;
import ru.terra.discosuspension.activity.components.ObdResult;
import ru.terra.discosuspension.service.OBDWorkerService;

public class FourXFourInfoActivity extends AppCompatActivity {
    private static final String TAG = FourXFourInfoActivity.class.getName();

    private TextView tv_gb_temp, tv_tb_temp, tv_rd_temp, tv_gear, tv_curr_gear, tv_tc_rot, tv_tc_sol_len, tv_range, tv_gb_shit_pos;
    private TextView tv_w_fl, tv_w_rl, tv_w_rr, tv_w_fr;
    private ProgressBar pb_front_left, pb_front_right, pb_rear_left, pb_rear_right;
    private ImageView iv_rear_diff_lock, iv_central_diff_lock;
    private Gauge gauge_steering_wheel_pos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_4x4_info);

        tv_gb_temp = findViewById(R.id.tv_gb_temp);
        tv_tb_temp = findViewById(R.id.tv_tb_temp);
        tv_rd_temp = findViewById(R.id.tv_rd_temp);
        tv_gear = findViewById(R.id.tv_gear);
        tv_curr_gear = findViewById(R.id.tv_curr_gear);
        tv_tc_rot = findViewById(R.id.tv_tc_rot);
        tv_tc_sol_len = findViewById(R.id.tv_tc_sol_pos);
        tv_range = findViewById(R.id.tv_range);
        tv_gb_shit_pos = findViewById(R.id.tv_gb_shit_pos);

        pb_front_left = findViewById(R.id.pb_front_left);
        pb_front_right = findViewById(R.id.pb_front_right);
        pb_rear_left = findViewById(R.id.pb_rear_left);
        pb_rear_right = findViewById(R.id.pb_rear_right);

        tv_w_fl = findViewById(R.id.tv_w_fl);
        tv_w_fr = findViewById(R.id.tv_w_fr);
        tv_w_rl = findViewById(R.id.tv_w_rl);
        tv_w_rr = findViewById(R.id.tv_w_rr);

        iv_rear_diff_lock = findViewById(R.id.iv_rear_diff_lock);
        iv_central_diff_lock = findViewById(R.id.iv_central_diff_lock);

        gauge_steering_wheel_pos = findViewById(R.id.gauge_steering_wheel_pos);

        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ObdResult result = (ObdResult) intent.getSerializableExtra("result");
                tv_gb_temp.setText(result.gbTemp);
                tv_tb_temp.setText(result.tcTemp);
                tv_rd_temp.setText(result.rdTemp);

                if (result.rdBlock) {
                    iv_rear_diff_lock.setImageResource(R.drawable.locked);
                } else {
                    iv_rear_diff_lock.setImageResource(R.drawable.unlocked);
                }

                if (result.tcBlock) {
                    iv_central_diff_lock.setImageResource(R.drawable.locked);
                } else {
                    iv_central_diff_lock.setImageResource(R.drawable.unlocked);
                }

                tv_tc_sol_len.setText(result.tcSolLen);
                tv_range.setText(result.tcSolPos);
                tv_curr_gear.setText(result.currentGear);
                tv_tc_rot.setText(result.tcRotation);

                pb_front_left.setProgress(result.suspFLVal);
                tv_w_fl.setText(result.suspFLText);
                pb_front_right.setProgress(result.suspFRVal);
                tv_w_fr.setText(result.suspFRText);
                pb_rear_left.setProgress(result.suspRLVal);
                tv_w_rl.setText(result.suspRLText);
                pb_rear_right.setProgress(result.suspRRVal);
                tv_w_rr.setText(result.suspRRText);

                tv_gear.setText(result.driveShiftPos);
                gauge_steering_wheel_pos.setValue(result.wheelPos);
            }
        }, new IntentFilter("update"));
    }

    @Override
    protected void onStart() {
        super.onStart();
        final Intent serviceIntent = new Intent(getApplicationContext(), OBDWorkerService.class);
        startService(serviceIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    private void doUnbindService() {
        if (isFinishing()) {
            stopService(new Intent(getApplicationContext(), OBDWorkerService.class));
        }
    }
}
