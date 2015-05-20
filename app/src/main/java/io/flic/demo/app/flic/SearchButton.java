package io.flic.demo.app.flic;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;

import io.flic.demo.app.R;
import io.flic.lib.FlicError;

public class SearchButton extends Button {

    final int SEARCH_CLOSE_DIALOG = 0;
    final int SEARCH_NO_FLICS = 1;
    final int SEARCH_PRIVATE_MODE = 2;
    final int SEARCH_NO_INTERNET_CONNECTON = 3;
    final int SEARCH_BLUETOOTH_DISABLED = 4;

    private int searchTime = 10000;

    private OnClickListener onClickListener = null;
    private Activity activity;
    private Handler handler;
    private HandlerThread handlerThread;

    public SearchButton(Context context) {
        super(context);
    }

    public SearchButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SearchButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        this.activity = (Activity) this.getContext();
        this.handlerThread = new HandlerThread(FlicApplication.getApp().getPackageName());
        this.handlerThread.start();
        this.handler = new Handler(this.handlerThread.getLooper());
        this.setText("SEARCH");
        this.setBackgroundColor(FlicApplication.getApp().getResources().getColor(R.color.flic_red2));
        this.setTextColor(FlicApplication.getApp().getResources().getColor(R.color.flic_white));
        super.setOnClickListener(new SearchClickListener());
    }

    @Override
    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    private class SearchClickListener implements OnClickListener {

        @Override
        public void onClick(View view) {
            loadView();
            if (SearchButton.this.onClickListener != null)
                SearchButton.this.onClickListener.onClick(view);
        }
    }
    private boolean animationRunning;
    private boolean firstDiscover;
    private boolean searchRunning;
    private String discoveredDeviceId;
    private RelativeLayout searchView;
    private TextView searchTitle;
    private ImageView searchIcon;
    private ImageView stopSearch;
    private RelativeLayout searchAgain;
    private TextView searchAgainApply;
    private TextView searchAgainClose;
    private ImageView searchMainImage;
    private View searchDivider;
    private TextView searchInfoText;
    private RelativeLayout searchBorder;
    private TextView searchMainText;

    private Animation animation;

    private Runnable endSearchAnimation = new Runnable() {
        @Override
        public void run() {
            SearchButton.this.activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    SearchButton.this.noFlicsFoundFromSearch();
                }
            });
        }
    };

    private Runnable endConnectAnimation = new Runnable() {
        @Override
        public void run() {
            SearchButton.this.activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    SearchButton.this.flicDiscoveredFromSearchConnectTimedOut();
                }
            });
        }
    };

    private void openInfoDialog(CharSequence title, CharSequence text) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this.activity);
        alertDialog.setTitle(title)
                .setMessage(text)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }
    public int getScreenWidth() {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }
    public static float convertDpToPixel(float dp) {
        DisplayMetrics metrics = FlicApplication.getApp().getResources().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return px;
    }
    private void loadView() {
        if (animationRunning) return;

        View.inflate(this.activity, R.layout.flic_search, (FrameLayout)this.activity.findViewById(android.R.id.content));
        this.searchView = (RelativeLayout) this.activity.findViewById(R.id.flic_search);
        this.searchView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {

                    @Override
                    public void onGlobalLayout() {
                        SearchButton.this.searchView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        SearchButton.this.searchView.setVisibility(View.GONE);
                        SearchButton.this.startSearchAnimation();
                    }

                });
    }

    private void startSearchAnimation() {
        this.searchRunning = true;
        this.searchTitle = (TextView) searchView.findViewById(R.id.flic_search_status_title);
        this.searchIcon = (ImageView) searchView.findViewById(R.id.flic_search_status_icon);
        this.stopSearch = (ImageView) searchView.findViewById(R.id.flic_search_stop_search);
        this.searchAgain = (RelativeLayout) searchView.findViewById(R.id.flic_search_try_again);
        this.searchAgainApply = (TextView) searchView.findViewById(R.id.flic_search_try_again_apply);
        this.searchAgainClose = (TextView) searchView.findViewById(R.id.flic_search_try_again_close);
        this.searchMainImage = (ImageView) searchView.findViewById(R.id.flic_search_main_image);
        this.searchDivider = searchView.findViewById(R.id.flic_search_divider);
        this.searchInfoText = (TextView) searchView.findViewById(R.id.flic_search_info_text);
        this.searchBorder = (RelativeLayout) searchView.findViewById(R.id.flic_search_border);
        this.searchMainText = (TextView) searchView.findViewById(R.id.flic_search_main_text);
        Typeface typeFace = Typeface.createFromAsset(this.activity.getAssets(), "fonts/Roboto-Medium.ttf");
        this.searchAgainClose.setTypeface(typeFace);
        this.searchAgainApply.setTypeface(typeFace);
        this.searchAgainApply.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SearchButton.this.startSearchAnimation();
            }
        });
        this.searchAgainClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SearchButton.this.endSearchAnimation();
            }
        });
        RelativeLayout.LayoutParams lp2 = new RelativeLayout.LayoutParams(this.getScreenWidth() / 2, ViewGroup.LayoutParams.MATCH_PARENT);
        lp2.addRule(RelativeLayout.ABOVE, searchView.findViewById(R.id.flic_search_footer).getId());
        lp2.addRule(RelativeLayout.CENTER_HORIZONTAL);
        this.searchMainImage.setLayoutParams(lp2);

        lp2 = new RelativeLayout.LayoutParams((int) (this.getScreenWidth() / 1.2), ViewGroup.LayoutParams.MATCH_PARENT);
        lp2.addRule(RelativeLayout.ABOVE, searchView.findViewById(R.id.flic_search_footer).getId());
        lp2.addRule(RelativeLayout.CENTER_HORIZONTAL);
        this.searchMainText.setLayoutParams(lp2);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (this.getScreenWidth() / 2) + ((int) convertDpToPixel(65)));
        this.searchView.findViewById(R.id.flic_search_new_button_view).setLayoutParams(lp);


        FlicApplication.getApp().addButtonUpdateListener(new FlicButtonUpdateListenerAdapter() {
            @Override
            public void buttonDiscovered(final String deviceId, int rssi, boolean isPrivateMode) {
                SearchButton.this.activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SearchButton.this.flicDiscoveredFromSearch(deviceId);
                    }
                });
            }

            @Override
            public String getHash() {
                return "SearchButton.startSearchAnimation";
            }
        });

        if (FlicApplication.getApp().isBluetoothActive()) {
            FlicApplication.getApp().startScan();
            this.firstDiscover = true;
            this.searchTitle.setText(this.getResources().getString(R.string.flic_search_searching_title));
            this.searchIcon.setVisibility(View.VISIBLE);
            this.searchIcon.setBackground(this.getResources().getDrawable(R.drawable.main_add_flic_searching_icon));
            this.stopSearch.setVisibility(View.VISIBLE);
            this.stopSearch.setBackground(this.getResources().getDrawable(R.drawable.main_add_flic_cancel_icon));
            this.searchAgain.setVisibility(View.GONE);
            this.searchMainImage.setVisibility(View.VISIBLE);
            this.searchMainImage.setBackground(this.getResources().getDrawable(R.drawable.main_click_flic));
            this.searchDivider.setVisibility(View.INVISIBLE);
            this.searchInfoText.setVisibility(View.VISIBLE);
            this.searchInfoText.setText(this.getResources().getString(R.string.flic_search_searching_info_text));
            this.searchInfoText.setTypeface(null);
            this.searchBorder.setBackgroundColor(this.getResources().getColor(R.color.flic_red2));
            this.searchMainText.setVisibility(View.GONE);
            this.searchView.setVisibility(View.VISIBLE);

            this.animation = new TranslateAnimation(
                    TranslateAnimation.ABSOLUTE, 0f,
                    TranslateAnimation.ABSOLUTE, 0f,
                    TranslateAnimation.RELATIVE_TO_PARENT, 0f,
                    TranslateAnimation.RELATIVE_TO_PARENT, 0.2f);
            this.animation.setDuration(600);
            this.animation.setRepeatCount(-1);
            this.animation.setRepeatMode(Animation.REVERSE);
            this.animation.setInterpolator(new LinearInterpolator());
            this.searchIcon.setAnimation(this.animation);

            handler.removeCallbacks(this.endSearchAnimation);
            handler.postDelayed(this.endSearchAnimation, this.searchTime);
            this.setSearchSmallIconAction(this.SEARCH_CLOSE_DIALOG);
        } else {
            this.searchTitle.setText(this.getResources().getString(R.string.flic_search_bluetooth_off_title));
            this.searchIcon.setVisibility(View.VISIBLE);
            this.searchIcon.setBackground(this.getResources().getDrawable(R.drawable.main_add_flic_bluetooth_off_icon));
            YoYo.with(Techniques.Shake)
                    .duration(800)
                    .playOn(this.searchIcon);
            this.stopSearch.setVisibility(View.VISIBLE);
            this.stopSearch.setBackground(this.getResources().getDrawable(R.drawable.main_add_flic_info_icon));
            this.searchAgain.setVisibility(View.VISIBLE);
            this.searchMainImage.setVisibility(View.GONE);
            this.searchDivider.setVisibility(View.VISIBLE);
            this.searchInfoText.setVisibility(View.GONE);
            this.searchBorder.setBackgroundColor(this.getResources().getColor(R.color.flic_error));
            this.searchMainText.setVisibility(View.VISIBLE);
            this.searchMainText.setText(this.getResources().getString(R.string.flic_search_bluetooth_off_main_text));
        }
        Animation animation = new TranslateAnimation(0, 0, this.searchView.getHeight(), 0);
        animation.setDuration(500);
        animation.setFillAfter(false);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                SearchButton.this.animationRunning = true;
                SearchButton.this.searchView.setClickable(true);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        if (!this.animationRunning) {
            this.searchView.startAnimation(animation);
        }
    }

    private void noFlicsFoundFromSearch() {
        this.searchRunning = false;
        FlicApplication.getApp().stopScan();
        this.animation.cancel();
        handler.removeCallbacks(this.endConnectAnimation);
        handler.removeCallbacks(this.endSearchAnimation);
        this.animation.cancel();
        this.searchTitle.setText(this.getResources().getString(R.string.flic_search_no_flics_found_title));
        this.searchIcon.setVisibility(View.GONE);
        this.searchAgain.setVisibility(View.VISIBLE);
        this.stopSearch.setVisibility(View.VISIBLE);
        this.stopSearch.setBackground(this.getResources().getDrawable(R.drawable.main_add_flic_info_icon));
        this.searchInfoText.setVisibility(View.GONE);
        this.searchDivider.setVisibility(View.VISIBLE);
        this.searchMainImage.setVisibility(View.GONE);
        this.searchMainText.setVisibility(View.VISIBLE);
        this.searchMainText.setText(this.getResources().getString(R.string.flic_search_no_flics_found_main_text));
        this.searchBorder.setBackgroundColor(this.getResources().getColor(R.color.flic_red2));

        this.setSearchSmallIconAction(SEARCH_NO_FLICS);
    }

    private void flicDiscoveredFromSearch(String deviceId) {
        if ((FlicApplication.getApp().getButton(deviceId) == null) && this.firstDiscover) {
            FlicApplication.getApp().addButtonEventListener(deviceId, new FlicButtonEventListenerAdapter() {
                @Override
                public void buttonDown(String deviceId) {
                    SearchButton.this.activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            YoYo.with(Techniques.RubberBand)
                                    .duration(300)
                                    .playOn(SearchButton.this.searchMainImage);
                        }
                    });
                }

                @Override
                public void buttonReady(final String deviceId, final String UUID) {
                    if (!searchRunning)
                        return;
                    FlicApplication.getApp().stopScan();
                    SearchButton.this.activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            flicConnectedFromSearch(deviceId, UUID);
                        }
                    });
                }

                @Override
                public void buttonConnectionFailed(String deviceId, final int status) {
                    if (!searchRunning)
                        return;
                    FlicApplication.getApp().stopScan();
                    SearchButton.this.activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            flicDiscoveredFromSearchFailedToConnect(status);
                        }
                    });
                }

                @Override
                public void buttonDisconnected(final String deviceId, final int status) {
                    if (!searchRunning)
                        return;
                    SearchButton.this.activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (status != FlicError.REBONDING) {
                                flicDiscoveredFromSearchFailedToConnect(status);
                            }
                        }
                    });
                }

                @Override
                public String getHash() {
                    return "SearchButton.flicDiscoveredFromSearch";
                }
            });

            this.firstDiscover = false;
            this.discoveredDeviceId = deviceId;
            FlicApplication.getApp().connectButton(deviceId);
            this.searchTitle.setText(this.getResources().getString(R.string.flic_search_flic_discovered_title));
            this.searchIcon.setBackground(this.getResources().getDrawable(R.drawable.main_add_flic_connecting_icon));
            this.searchAgain.setVisibility(View.GONE);
            this.stopSearch.setVisibility(View.GONE);
            this.searchBorder.setBackgroundColor(this.getResources().getColor(R.color.flic_red2));
            this.searchMainImage.setVisibility(View.VISIBLE);
            this.searchMainImage.setBackground(this.getResources().getDrawable(R.drawable.main_add_flic_mint_connecting));
            this.searchInfoText.setVisibility(View.VISIBLE);
            this.searchInfoText.setText(this.getResources().getString(R.string.flic_search_flic_discovered_info_text));
            Typeface typeFace = Typeface.createFromAsset(this.activity.getAssets(), "fonts/Roboto-Light.ttf");
            this.searchInfoText.setTypeface(typeFace);
            handler.removeCallbacks(this.endSearchAnimation);
            handler.postDelayed(this.endConnectAnimation, 13000);
        }
    }

    private void flicDiscoveredFromSearchFailedToConnect(int status) {
        this.searchRunning = false;
        handler.removeCallbacks(this.endConnectAnimation);
        this.animation.cancel();
        FlicApplication.getApp().stopScan();
        if (discoveredDeviceId != null) {
            FlicApplication.getApp().deleteButton(discoveredDeviceId);
            discoveredDeviceId = null;
        }

        switch (status) {
            case FlicError.BUTTON_IS_PRIVATE:
                this.searchTitle.setText(this.getResources().getString(R.string.flic_search_private_mode_title));
                this.searchIcon.setVisibility(View.VISIBLE);
                this.searchIcon.setBackground(this.getResources().getDrawable(R.drawable.main_add_flic_private_mode_icon));
                YoYo.with(Techniques.Shake)
                        .duration(800)
                        .playOn(this.searchIcon);
                this.stopSearch.setVisibility(View.VISIBLE);
                this.stopSearch.setBackground(this.getResources().getDrawable(R.drawable.main_add_flic_info_icon));
                this.searchAgain.setVisibility(View.VISIBLE);
                this.searchMainImage.setVisibility(View.VISIBLE);
                this.searchMainImage.setBackground(this.getResources().getDrawable(R.drawable.main_click_flic));
                this.searchDivider.setVisibility(View.VISIBLE);
                this.searchInfoText.setVisibility(View.GONE);
                this.searchInfoText.setText(this.getResources().getString(R.string.flic_search_private_mode_info_text));
                this.searchInfoText.setTypeface(null);
                this.searchBorder.setBackgroundColor(this.getResources().getColor(R.color.flic_gray1));
                this.searchMainText.setVisibility(View.GONE);
                this.searchView.setVisibility(View.VISIBLE);
                this.setSearchSmallIconAction(SEARCH_PRIVATE_MODE);
                break;
            case FlicError.BACKEND_UNREACHABLE:
                this.searchTitle.setText(this.getResources().getString(R.string.flic_search_backend_unreachable_title));
                this.searchIcon.setVisibility(View.VISIBLE);
                this.searchIcon.setBackground(this.getResources().getDrawable(R.drawable.main_add_flic_no_internet_icon));
                YoYo.with(Techniques.Shake)
                        .duration(800)
                        .playOn(this.searchIcon);
                this.stopSearch.setVisibility(View.VISIBLE);
                this.stopSearch.setBackground(this.getResources().getDrawable(R.drawable.main_add_flic_info_icon));
                this.searchAgain.setVisibility(View.VISIBLE);
                this.searchMainImage.setVisibility(View.GONE);
                this.searchDivider.setVisibility(View.VISIBLE);
                this.searchInfoText.setVisibility(View.GONE);
                this.searchInfoText.setTypeface(null);
                this.searchBorder.setBackgroundColor(this.getResources().getColor(R.color.flic_error));
                this.searchMainText.setVisibility(View.VISIBLE);
                this.searchMainText.setText(this.getResources().getString(R.string.flic_search_backend_unreachable_main_text));
                this.searchView.setVisibility(View.VISIBLE);
                this.setSearchSmallIconAction(SEARCH_NO_INTERNET_CONNECTON);
                break;
            default:
                this.flicDiscoveredFromSearchConnectTimedOut();
                break;
        }
    }

    private void setSearchSmallIconAction(int option) {
        switch (option) {
            case SEARCH_CLOSE_DIALOG:
                this.stopSearch.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        handler.removeCallbacks(SearchButton.this.endSearchAnimation);
                        FlicApplication.getApp().stopScan();
                        endSearchAnimation();
                    }
                });
                break;
            case SEARCH_NO_FLICS:
                this.stopSearch.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openInfoDialog(SearchButton.this.getResources().getText(R.string.flic_search_no_flics_found_popup_title),
                                SearchButton.this.getResources().getText(R.string.flic_search_no_flics_found_popup_text));
                    }
                });
                break;
            case SEARCH_PRIVATE_MODE:
                this.stopSearch.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SearchButton.this.openInfoDialog(SearchButton.this.getResources().getText(R.string.flic_search_private_mode_popup_title),
                                SearchButton.this.getResources().getText(R.string.flic_search_private_mode_popup_text));
                    }
                });
                break;
            case SEARCH_NO_INTERNET_CONNECTON:
                this.stopSearch.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SearchButton.this.openInfoDialog(SearchButton.this.getResources().getText(R.string.flic_search_backend_unreachable_popup_title),
                                SearchButton.this.getResources().getText(R.string.flic_search_backend_unreachable_popup_text));
                    }
                });
                break;
            case SEARCH_BLUETOOTH_DISABLED:
                this.stopSearch.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SearchButton.this.openInfoDialog(SearchButton.this.getResources().getText(R.string.flic_search_bluetooth_off_popup_title),
                                SearchButton.this.getResources().getText(R.string.flic_search_bluetooth_off_popup_text));
                    }
                });
                break;
        }
    }

    private void flicDiscoveredFromSearchConnectTimedOut() {
        this.searchRunning = false;
        FlicApplication.getApp().stopScan();
        if (discoveredDeviceId != null) {
            FlicApplication.getApp().deleteButton(discoveredDeviceId);
            discoveredDeviceId = null;
        }
        this.animation.cancel();
        handler.removeCallbacks(this.endConnectAnimation);
        this.searchTitle.setText(this.getResources().getString(R.string.flic_search_no_connection_title));
        this.searchIcon.setVisibility(View.VISIBLE);
        this.searchIcon.setBackground(this.getResources().getDrawable(R.drawable.main_add_flic_error_icon));
        YoYo.with(Techniques.Shake)
                .duration(800)
                .playOn(this.searchIcon);
        this.searchAgain.setVisibility(View.VISIBLE);
        this.stopSearch.setVisibility(View.GONE);
        this.searchInfoText.setVisibility(View.GONE);
        this.searchDivider.setVisibility(View.VISIBLE);
        this.stopSearch.setBackground(this.getResources().getDrawable(R.drawable.main_add_flic_info_icon));
        this.searchBorder.setBackgroundColor(this.getResources().getColor(R.color.flic_error));
        this.searchMainImage.setVisibility(View.GONE);
        this.searchMainText.setVisibility(View.VISIBLE);
        this.searchMainText.setText(this.getResources().getString(R.string.flic_search_no_connection_main_text));
    }

    private void endSearchAnimation() {
        FlicApplication.getApp().stopScan();
        Animation animation = new TranslateAnimation(0, 0, 0, this.searchView.getHeight());
        animation.setDuration(500);
        animation.setFillAfter(true);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                SearchButton.this.animationRunning = false;
                SearchButton.this.searchView.setClickable(false);
                SearchButton.this.searchView.setVisibility(View.GONE);
                ((ViewGroup)SearchButton.this.activity.findViewById(android.R.id.content)).removeView(searchView);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        this.searchView.startAnimation(animation);
    }

    private void flicConnectedFromSearch(final String deviceId, final String uuid) {
        this.searchRunning = false;
        FlicApplication.getApp().stopScan();
        FlicButton flicButton = new FlicButton(deviceId, FlicButton.FlicColor.FLIC_COLOR_MINT, true);
        handler.removeCallbacks(this.endConnectAnimation);
        this.animation.cancel();

        this.searchTitle.setText(this.getResources().getString(R.string.flic_search_connected_title));
        this.searchIcon.setVisibility(View.VISIBLE);
        this.searchIcon.setBackground(this.getResources().getDrawable(R.drawable.main_add_flic_connected_icon));
        YoYo.with(Techniques.Shake)
                .duration(800)
                .playOn(this.searchIcon);
        this.stopSearch.setVisibility(View.VISIBLE);
        this.stopSearch.setBackground(this.getResources().getDrawable(R.drawable.main_add_flic_cancel_icon));
        this.setSearchSmallIconAction(this.SEARCH_CLOSE_DIALOG);
        this.searchAgain.setVisibility(View.GONE);
        this.searchMainImage.setVisibility(View.VISIBLE);
        this.searchMainImage.setBackground(this.getResources().getDrawable(R.drawable.main_add_flic_mint_connected));
        this.searchDivider.setVisibility(View.INVISIBLE);
        this.searchInfoText.setVisibility(View.VISIBLE);
        this.searchInfoText.setText(this.getResources().getString(R.string.flic_search_connected_info_text));
        Typeface typeFace = Typeface.createFromAsset(this.activity.getAssets(), "fonts/Roboto-Medium.ttf");
        this.searchInfoText.setTypeface(typeFace);
        this.searchBorder.setBackgroundColor(this.getResources().getColor(R.color.flic_teal2));
        this.searchMainText.setVisibility(View.GONE);
    }
}
