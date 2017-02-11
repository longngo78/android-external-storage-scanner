package longo.com.clover;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity implements FileScanner.Listener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 101;

    private FloatingActionButton mScanButton;
    private FileScanner mFileScanner;
    private TextView mInfoTextView;
    private TextView mListTextView;
    private TextView mResultTextView;
    private TextView mScanningTextView;
    private ProgressBar mProgressBar;

    // progress check
    private long mFileCount;
    private long mTotalSize;
    private long mStartTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // layout
        setContentView(R.layout.activity_main);

        // text views
        mInfoTextView = (TextView) findViewById(R.id.tv_info);

        mResultTextView = (TextView) findViewById(R.id.tv_result);
        mResultTextView.setText(null);
        mListTextView = (TextView) findViewById(R.id.tv_list);
        mListTextView.setText(null);
        mScanningTextView = (TextView) findViewById(R.id.tv_scanning);
        mScanningTextView.setVisibility(View.GONE);

        // progress bar
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.GONE);

        // button
        mScanButton = (FloatingActionButton) findViewById(R.id.fabScan);
        mScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OnClickScan();
            }
        });

        // the scanner
        mFileScanner = new FileScanner(this);

        updateInfo();
    }

    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // we need permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_READ_EXTERNAL_STORAGE);

            /*if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_READ_EXTERNAL_STORAGE);
            }*/

            return false;
        }

        return true;
    }

    private void updateInfo() {
        mInfoTextView.setText("Dir:" + StorageUtils.STORAGE_DIR_PATH
                + "\nReadable: " + StorageUtils.isExternalStorageReadable()
                + "\nWritable: " + StorageUtils.isExternalStorageWritable()
                + (StorageUtils.isExternalStorageEmpty() ? "\nNeeds READ_EXTERNAL_STORAGE permission!" : "")
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay!
                    //Toast.makeText(this, R.string.start_scan_now, Toast.LENGTH_LONG).show();
                    updateInfo();

                    // scan now
                    OnClickScan();
                } else {
                    // permission denied, boo!
                    Toast.makeText(this, R.string.you_must_allow, Toast.LENGTH_LONG).show();
                    //finish();
                }

                return;
            }
        }
    }

    private void OnClickScan() {
        Log.d(TAG, "OnClickScan()");

        // check permission
        if (!checkPermission()) {
            return;
        }

        mFileCount = 0;
        mTotalSize = 0;
        mStartTime = System.currentTimeMillis();

        // lock UI
        mScanButton.setEnabled(false);
        mProgressBar.setVisibility(View.VISIBLE);
        mScanningTextView.setVisibility(View.VISIBLE);
        // clear text
        mResultTextView.setText("");
        mListTextView.setText("");

        // start scanning in background
        mFileScanner.scanInBackground();
    }

    @Override
    public void onScanFile(final StorageUtils.FileEntry fileEntry) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFileCount++;
                if (!fileEntry.dir) mTotalSize += fileEntry.size;

                // append entry to text view for fun
                //mListTextView.setText(String.format("%s\n%s", mListTextView.getText(), fileEntry.toString()));
                mListTextView.setText(fileEntry.toString());

                // update counter and time
                final long elapsedTime = System.currentTimeMillis() - mStartTime;
                mResultTextView.setText(String.format("Files: %d\nSize: %d\nTime: %dms", mFileCount, mTotalSize, elapsedTime));
            }
        });
    }

    private Runnable mUpdateUIRunnable = new Runnable() {
        @Override
        public void run() {
            // unlock UI
            mScanButton.setEnabled(true);
            mProgressBar.setVisibility(View.GONE);
            mScanningTextView.setVisibility(View.GONE);

            // summary
            final List<StorageUtils.FileEntry> files = mFileScanner.getFiles();
            final long elapsedTime = System.currentTimeMillis() - mStartTime;
            mResultTextView.setText(String.format("Files: %d\nSize: %d\nTime: %dms", mFileCount, mTotalSize, elapsedTime));

            // list the files
            mListTextView.setText(mFileScanner.getOutput());
        }
    };

    @Override
    public void OnScanComplete() {
        // update on UI Thread
        runOnUiThread(mUpdateUIRunnable);
    }
}
