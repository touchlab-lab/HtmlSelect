package com.touchlab.htmlselect;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
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

        ImageView touchLab = (ImageView)findViewById(R.id.touchlab);
        touchLab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("http://www.touchlab.co/"));
                startActivity(intent);
            }
        });

        ImageView atavist = (ImageView)findViewById(R.id.atavist);
        atavist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("http://atavist.net/"));
                startActivity(intent);
            }
        });

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
                        Log.e("#############", "reportSelectionCoords");
                        reportSelectionCoords();
                    } else if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        hideClipManager();
                    }
                }

                return gestureScanner.onTouchEvent(motionEvent);
            }
        });

        webView.loadUrl("file:///android_asset/html_select_template.html");

    }
    //the user has clicked on the "Toast" button in the clipboard
    //OSgetSelection() collects the selection string and calls pushSelection(String)
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
            reportSelectionCoords();
        }
    }

    private void flushSelection() {
        setSelecting(false);
        webView.loadUrl("javascript:cancelSelection()");
        hideClipManager();
    }

    //this is called from the initial long press to each subsequent onTouch event.
    //reportSelectionCoords() returns the xy coordinates to the method pushSelectionCoords(x,y) to determine
    //clipboard positioning
    private void reportSelectionCoords() {
        webView.loadUrl("javascript:reportSelectionCoords()");
    }

    //calculates x y coordinates to display clipboard.......this is not ideal and is only a rough guess
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

    //logging from webview JS
    WebChromeClient chromeClient = new WebChromeClient() {
        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            Log.i("########", consoleMessage.sourceId() + ": " + consoleMessage.messageLevel() + " : " + consoleMessage.message() + " : " + consoleMessage.lineNumber());
            return true;
        }
    };


    //////////////////////////////////////////////////////////////OnGestureListener methods
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
    //very first part that is fired when a user does a long press and initiates the selection
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
