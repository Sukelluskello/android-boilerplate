package io.flic.demo.app.ui;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;

import io.flic.demo.app.FlicApplication;
import io.flic.demo.app.FlicButton;
import io.flic.demo.app.FlicButtonUpdateListenerAdapter;
import io.flic.demo.app.R;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("MainActivity", "onCreate");
        this.setContentView(R.layout.activity_main);

        final LinearLayout list = (LinearLayout) findViewById(R.id.activity_main);

        final HashMap<String, View> viewMap = new HashMap<>();

        FlicApplication app = (FlicApplication) getApplication();
        app.addButtonUpdateListener(new FlicButtonUpdateListenerAdapter() {

            @Override
            public void buttonAdded(FlicButton flicButton) {
                TextView textView = new TextView(MainActivity.this);
                textView.setText(flicButton.getDeviceId());
                viewMap.put(flicButton.getDeviceId(), textView);
                list.addView(textView);
            }

            @Override
            public void buttonDeleted(FlicButton flicButton) {
                if (viewMap.containsKey(flicButton.getDeviceId())) {
                    View view = viewMap.get(flicButton.getDeviceId());
                    list.removeView(view);
                }
            }

            @Override
            public String getHash() {
                return "MainActivity.onCreate";
            }
        });
    }
}
