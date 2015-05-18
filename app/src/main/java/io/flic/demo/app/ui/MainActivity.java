package io.flic.demo.app.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
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
    FlicApplication app;
    LinearLayout list;
    HashMap<String, View> viewMap;

    FlicButtonEventListener flucButtonListener = new FlicButtonEventListenerAdapter() {
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
        public void buttonUp(String deviceId) {

        }

        @Override
        public void buttonClick(String deviceId) {

        }

        @Override
        public void buttonDoubleClick(String deviceId) {

        }

        @Override
        public void buttonHold(String deviceId) {

        }

        @Override
        public void buttonConnected(String deviceId, String UUID) {

        }

        @Override
        public void buttonDisconnected(String deviceId, int status) {

        }

        @Override
        public String getHash() {
            return null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.activity_main);
        this.list = (LinearLayout) this.findViewById(R.id.activity_main_list);
        this.app = (FlicApplication) this.getApplication();

        //TODO ALLA KNAPPAR VERKAR INTE KUNNA LÄSAS IN
        //TODO KNAPPARNA LADDAS INTE IN NÄR APPEN STARTAS
        //TODO LISTA MED ACCEPTERADE KNAPPAR

        this.app.addButtonUpdateListener(new FlicButtonUpdateListenerAdapter() {

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
        this.list.removeAllViewsInLayout();
        this.viewMap = new HashMap<>();
        LayoutInflater inflater = (LayoutInflater) this.getApplicationContext().getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        for (FlicButton flicButton : this.app.getButtons()) {
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
            this.app.addButtonEventListener(flicButton.getDeviceId(), this.flucButtonListener);

            flicButtonRow.findViewById(R.id.flic_button_row_connect_toggle).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //TODO DISC/CONN
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        this.updateList();
    }
}
