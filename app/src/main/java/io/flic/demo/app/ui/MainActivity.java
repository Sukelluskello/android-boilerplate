package io.flic.demo.app.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.HashMap;

import io.flic.demo.app.FlicApplication;
import io.flic.demo.app.FlicButton;
import io.flic.demo.app.FlicButtonUpdateListenerAdapter;
import io.flic.demo.app.R;

public class MainActivity extends Activity {
    FlicApplication app;
    LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.activity_main);
        list = (LinearLayout) findViewById(R.id.activity_main_list);
        app = (FlicApplication) getApplication();

        app.addButtonUpdateListener(new FlicButtonUpdateListenerAdapter() {

            @Override
            public void buttonAdded(FlicButton flicButton) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        MainActivity.this.updateList();
                    }
                });
            }

            @Override
            public void buttonDeleted(FlicButton flicButton) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        MainActivity.this.updateList();
                    }
                });
            }

            @Override
            public String getHash() {
                return "MainActivity.onCreate";
            }
        });
    }

    private void updateList() {
        list.removeAllViewsInLayout();
        HashMap<String, View> viewMap = new HashMap<>();
        LayoutInflater inflater = (LayoutInflater)getApplicationContext().getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        for (FlicButton flicButton : app.getButtons()) {
            RelativeLayout flicButtonRow = (RelativeLayout) inflater.inflate(R.layout.flic_button_row, null);
            TextView textView = (TextView) flicButtonRow.findViewById(R.id.flic_button_row_name);
            textView.setText(flicButton.getDeviceId());
            viewMap.put(flicButton.getDeviceId(), flicButtonRow);
            list.addView(flicButtonRow);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        this.updateList();
    }
}
