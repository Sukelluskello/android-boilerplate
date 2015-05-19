package io.flic.demo.app.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;

import java.util.HashMap;

import io.flic.demo.app.FlicApplication;
import io.flic.demo.app.FlicButton;
import io.flic.demo.app.FlicButtonEventListener;
import io.flic.demo.app.FlicButtonEventListenerAdapter;
import io.flic.demo.app.FlicButtonUpdateListenerAdapter;
import io.flic.demo.app.R;

public class MainActivity extends Activity {

    private void setupFlicButton(FlicButton flicButton) {
        FlicApplication.getApp().addButtonEventListener(flicButton.getDeviceId(), new FlicButtonEventListenerAdapter() {
            @Override
            public void buttonClick(String deviceId) {
                Log.i("MainActivity", "buttonClick: " + deviceId);
            }

            @Override
            public void buttonDoubleClick(String deviceId) {
                Log.i("MainActivity", "buttonDoubleClick: " + deviceId);
            }

            @Override
            public void buttonHold(String deviceId) {
                Log.i("MainActivity", "buttonHold: " + deviceId);
            }

            @Override
            public String getHash() {
                return "MainActivity.setupFlicButton";
            }
        });
    }

    private void setupWhitelist() {
        FlicApplication.getApp().whitelistDeviceId("08:d4:2c:01:5b:00");
        FlicApplication.getApp().whitelistDeviceId("08:d4:2c:01:b1:00");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.activity_main);
        this.list = (LinearLayout) this.findViewById(R.id.activity_main_list);

        this.setupWhitelist();

        FlicApplication.getApp().addButtonUpdateListener(new FlicButtonUpdateListenerAdapter() {

            @Override
            public void buttonAdded(final FlicButton flicButton) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        MainActivity.this.updateList();
                        MainActivity.this.setupFlicButton(flicButton);
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

    @Override
    public void onResume() {
        super.onResume();
        this.updateList();
    }

    private LinearLayout list;
    private HashMap<String, View> viewMap;

    private void updateList() {
        this.list.removeAllViewsInLayout();
        this.viewMap = new HashMap<>();
        LayoutInflater inflater = (LayoutInflater) this.getApplicationContext().getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        for (final FlicButton flicButton : FlicApplication.getApp().getButtons()) {
            RelativeLayout flicButtonRow = (RelativeLayout) inflater.inflate(R.layout.flic_button_row, null);
            ((TextView)flicButtonRow.findViewById(R.id.flic_button_row_name)).setText(flicButton.getDeviceId());


            if (flicButton.isConnected()) {
                flicButtonRow.findViewById(R.id.flic_button_row_icon).setBackground(this.getResources().getDrawable(R.drawable.main_add_flic_white_connected));
                flicButtonRow.findViewById(R.id.flic_button_row_connect_toggle_icon).setBackground(this.getResources().getDrawable(R.drawable.main_flic_disconnect));
            } else {
                flicButtonRow.findViewById(R.id.flic_button_row_icon).setBackground(this.getResources().getDrawable(R.drawable.main_flic_disabled_white));
                flicButtonRow.findViewById(R.id.flic_button_row_connect_toggle_icon).setBackground(this.getResources().getDrawable(R.drawable.main_flic_connect));
            }

            this.viewMap.put(flicButton.getDeviceId(), flicButtonRow);
            this.list.addView(flicButtonRow);
            FlicApplication.getApp().addButtonEventListener(flicButton.getDeviceId(), new FlicButtonEventListenerAdapter() {
                @Override
                public void buttonDown(final String deviceId) {

                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            YoYo.with(Techniques.Shake)
                                    .duration(300)
                                    .playOn(viewMap.get(deviceId).findViewById(R.id.flic_button_row_icon));
                        }
                    });
                }

                @Override
                public void buttonReady(String deviceId, String UUID) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.updateList();
                        }
                    });
                }

                @Override
                public void buttonDisconnected(String deviceId, int status) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.updateList();
                        }
                    });
                }

                @Override
                public String getHash() {
                    return "MainActivity.updateList";
                }
            });

            flicButtonRow.findViewById(R.id.flic_button_row_connect_toggle).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (flicButton.isConnected()) {
                        FlicApplication.getApp().getFlicService().disconnectButton(flicButton.getDeviceId());
                    } else {
                        FlicApplication.getApp().getFlicService().connectButton(flicButton.getDeviceId());
                    }
                }
            });
        }
    }
}
