package ru.coyul.painting.simplepainter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Date;
import java.util.Locale;


public class MainActivity extends Activity {

    private PaintingView mPaintingView;
    private Button mBtnSave;
    private Button mBtnClear;

    private static final String[] PERMISSIONS = new String[]{
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPaintingView = (PaintingView) findViewById(R.id.painting);
        mBtnSave = (Button) findViewById(R.id.btn_save);
        mBtnClear = (Button) findViewById(R.id.btn_clear);

        checkAndRequestPermission();

        mBtnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveView();
            }
        });

        mBtnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPaintingView.clear();
            }
        });

    }

    //for versions >= 23 should ask permission to write to storage
    public void checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(PERMISSIONS, 30);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MainActivity.this,
                    getString(R.string.permission_okay),
                    Toast.LENGTH_LONG)
                    .show();

        } else {
            Toast.makeText(MainActivity.this,
                    getString(R.string.permission_denied),
                    Toast.LENGTH_LONG)
                    .show();
        }
    }

    //saving image - get bitmap and save it into file in another thread
    private void saveView() {
        mPaintingView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        mPaintingView.setDrawingCacheEnabled(true);
        mPaintingView.buildDrawingCache();

        Bitmap cache = mPaintingView.getDrawingCache();
        Bitmap bitmap = cache.copy(cache.getConfig(), false);
        mPaintingView.setDrawingCacheEnabled(false);

        new SaveTask().execute(bitmap);
    }


    private class SaveTask extends AsyncTask<Bitmap, Void, File> {

        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage(getString(R.string.process_save));
            progressDialog.show();
        }

        @Override
        protected File doInBackground(Bitmap... bitmaps) {


            String name = getUniqueFileName();
            File resultFile;
            //write on sd card, if it is available - otherwise in internal phone memory
            if (isExternalStorageWritable()) {
                resultFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), name);
            } else {
                resultFile = new File(getDir("images", Context.MODE_PRIVATE), name);
            }


            FileOutputStream outputStream;
            try {
                outputStream = new FileOutputStream(resultFile);
                bitmaps[0].compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(resultFile)));
                outputStream.close();
            } catch (FileNotFoundException e) {
                Log.e(this.getClass().getName(), "FileNotFoundException", e);
                resultFile = null;
            } catch (IOException r) {
                Log.e(this.getClass().getName(), "IOException", r);
            }

            return resultFile;

        }

        @Override
        protected void onPostExecute(File file) {
            progressDialog.dismiss();
            if (file != null) {
                Toast.makeText(MainActivity.this,
                        getString(R.string.file_save, file.getName(), file.getParent()),
                        Toast.LENGTH_LONG)
                        .show();
            } else {
                Toast.makeText(MainActivity.this,
                        getString(R.string.error_save),
                        Toast.LENGTH_LONG)
                        .show();
            }
        }
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private String getUniqueFileName() {
        java.text.SimpleDateFormat simpleDateFormat = new java.text.SimpleDateFormat(getString(R.string.save_file_format), Locale.getDefault());
        Date date = new Date();
        return simpleDateFormat.format(date);
    }

}