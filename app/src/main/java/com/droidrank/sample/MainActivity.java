package com.droidrank.sample;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements ImageCallback{

    private Button previous, next;

    private static final int PERMISSION_WRITE_EXTERNAL_REQUEST = 100;
    private static final String KEY_COUNT = "count";
    private static final String KEY_URL = "url";
    private ImageView imageView;
    private TextView tv;

    private int count = 0;
    private ImageLoader imageLoader;
    private ArrayList<String> imageUrls = new ArrayList<>();

    //private int total;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        if (savedInstanceState != null) {
            count = savedInstanceState.getInt(KEY_COUNT);
            imageUrls = savedInstanceState.getStringArrayList(KEY_URL);
        }

        imageLoader = new ImageLoader(getApplicationContext(), this);

        handleStoragePermission();

    }

    private void handleStoragePermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_WRITE_EXTERNAL_REQUEST);

        } else {
            if(Utility.isNetworkAvailable(this)) {
                new FetchImagesUrls(String.valueOf(count)).execute();
            } else {
                Utility.showToastMessage(this, getString(R.string.no_net));
            }

        }
    }

    private void fetchAndDisplayImage(int cnt) {
        imageLoader.displayBitmap(imageUrls.get(cnt - 1), cnt);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_COUNT, count);
        outState.putStringArrayList(KEY_URL, imageUrls);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_WRITE_EXTERNAL_REQUEST: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(Utility.isNetworkAvailable(MainActivity.this)) {
                        new FetchImagesUrls(String.valueOf(count)).execute();
                    } else {
                        Utility.showToastMessage(MainActivity.this, getString(R.string.no_net));
                    }
                } else {
                    // permission denied
                }
                return;
            }
        }
    }

    private void initView() {
        previous = (Button) findViewById(R.id.previous);
        //onclick of previous button should navigate the user to previous image
        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (count > 1) {
                    count--;
                    fetchAndDisplayImage(count);
                } else {
                    Utility.showToastMessage(MainActivity.this, getString(R.string.no_prev_data));
                }
            }
        });
        next = (Button) findViewById(R.id.next);
        //onclick of next button should navigate the user to next image
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(count == imageUrls.size()) {
                    count++;

                    if(Utility.isNetworkAvailable(MainActivity.this)) {
                        previous.setEnabled(false);
                        next.setEnabled(false);
                        new FetchImagesUrls(String.valueOf(count)).execute();
                    } else {
                        Utility.showToastMessage(MainActivity.this, getString(R.string.no_net));
                    }
                } else {
                    count++;
                        fetchAndDisplayImage(count);
                    }
                }
        });
        imageView = findViewById(R.id.imageview);
        tv = findViewById(R.id.nodata);

        previous.setEnabled(false);
        next.setEnabled(false);
    }

    @Override
    public void updatePhoto(Bitmap bitmap, int imgCount) {
        if (count == imgCount) {
            if(bitmap == null) {
                Utility.showToastMessage(this, getString(R.string.null_bitmap));
                imageView.setVisibility(View.GONE);
                tv.setVisibility(View.VISIBLE);
            } else {
                imageView.setVisibility(View.VISIBLE);
                tv.setVisibility(View.GONE);
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    private class FetchImagesUrls extends AsyncTask<String, Void, String> {

        private String offset;

        FetchImagesUrls(String ofset) {
            this.offset = ofset;
        }

        @Override
        protected String doInBackground(final String... params) {
            return Utility.getResponse(Constants.API_URL + offset);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            parseJsonAndDisplayImages(result);
        }
    }

    private void parseJsonAndDisplayImages(String result) {
        try {
            JSONObject object = new JSONObject(result);
            JSONArray array = object.getJSONArray("images");
            for(int i = 0; i < array.length(); i++) {
                JSONObject jsonObject = array.getJSONObject(i);
                imageUrls.add(jsonObject.getString("imageUrl"));
            }
            if(count == 0)
            count++;
            fetchAndDisplayImage(count);
            next.setEnabled(true);
            previous.setEnabled(true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}