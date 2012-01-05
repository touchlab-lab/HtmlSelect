package com.touchlab.htmlselect;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class HtmlSelect extends Activity implements GestureDetector.OnGestureListener {

    private GestureDetector gestureScanner;
    private boolean selecting;
    private WebView webView;
    private Handler uiHandler;
    public static final int Y_POPUP_OFFSET = 80;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main);
        initControls();
    }

    private void initControls() {

        gestureScanner = new GestureDetector(this);
        uiHandler = new Handler();

        WebView webView = (WebView) findViewById(R.id.browser);
        this.webView = webView;
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setPluginsEnabled(true);
        webView.setWebChromeClient(chromeClient);
        webView.addJavascriptInterface(new javaScriptInterface(), "htmlSelectActivity");
        webView.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                return true;
            }
        });

        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (isSelecting()) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        Log.e("#############", "showClipManager");
                        showClipManager(motionEvent);
                    } else if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        hideClipManager();
                    }
                }

                return gestureScanner.onTouchEvent(motionEvent);
            }
        });

        webView.loadUrl("file:///android_asset/html_select_template.html");

    }

    private void grabSelection() {

        String js = "javascript:OSgetSelection()";
        webView.loadUrl(js);

    }

    private class javaScriptInterface {

        public void pushSelection(String selection) {
            Log.e("@#@#@#@#@#@#@", "SELECTION:" + selection);
            Toast.makeText(HtmlSelect.this, selection, Toast.LENGTH_LONG).show();
        }

        public void pushSelectionCoords(final int x, final int y) {
            {
                if (isSelecting()) {
                    TextView showToast = (TextView) findViewById(R.id.showToast);
                    showToast.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            grabSelection();
                            flushSelection();
                        }
                    });

                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (isSelecting()) {
                                showClipManager(y);
                            }
                        }
                    });

                }
            }
        }

        public void pushAutoSelect() {
            showClipManager(null);
        }
    }

    private void flushSelection() {
        setSelecting(false);
        webView.loadUrl("javascript:cancelSelection()");
        hideClipManager();
    }

    private void showClipManager(MotionEvent motionEvent) {
        webView.loadUrl("javascript:reportSelectionCoords()");
    }

    private void showClipManager(int y) {
        Display display = getWindowManager().getDefaultDisplay();
        float yOffset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, Y_POPUP_OFFSET, getResources().getDisplayMetrics());

        int yNormalized = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, y, getResources().getDisplayMetrics()) - (int) yOffset;

        if (yNormalized < 0)
            yNormalized = 0;

        final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT);
        FrameLayout clipManager = (FrameLayout) findViewById(R.id.clipManage);
        int xMargin = (display.getWidth() - clipManager.getWidth()) / 2;
        params.setMargins(xMargin, yNormalized, 0, 0);
        clipManager.setVisibility(View.VISIBLE);
        clipManager.setLayoutParams(params);
    }

    private void hideClipManager() {
        FrameLayout clipManager = (FrameLayout) findViewById(R.id.clipManage);
        clipManager.setVisibility(View.GONE);
    }

    public boolean isSelecting() {
        return selecting;
    }

    public void setSelecting(boolean selecting) {
        this.selecting = selecting;
    }

    WebChromeClient chromeClient = new WebChromeClient() {
        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            Log.e("########", consoleMessage.sourceId() + ": " + consoleMessage.messageLevel() + " : " + consoleMessage.message() + " : " + consoleMessage.lineNumber());
            return true;
        }
    };

    public boolean onDown(MotionEvent motionEvent) {
        return false;
    }

    public void onShowPress(MotionEvent motionEvent) {

    }

    public boolean onSingleTapUp(MotionEvent motionEvent) {
        if (isSelecting()) {
            flushSelection();
        }
        return false;
    }

    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    public void onLongPress(MotionEvent motionEvent) {

        if (!isSelecting()) {
            setSelecting(true);
            webView.loadUrl("javascript:startSelection()");
            Log.e("#############", "onLongPress....start selecting");
        }
    }

    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent me) {
        return gestureScanner.onTouchEvent(me);
    }
}
