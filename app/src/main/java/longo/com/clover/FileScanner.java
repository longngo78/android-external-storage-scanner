package longo.com.clover;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by longngo on 2/3/17.
 */

public class FileScanner implements Runnable {
    private static final String TAG = FileScanner.class.getSimpleName();

    private static Comparator<StorageUtils.FileEntry> comparator = new Comparator<StorageUtils.FileEntry>() {
        @Override
        public int compare(final StorageUtils.FileEntry a, final StorageUtils.FileEntry b) {
            return (int) (b.size - a.size);
        }
    };

    // list of files scanned
    private List<StorageUtils.FileEntry> mFiles = new ArrayList<>();
    private boolean mScanning = false;
    private final Listener mListener;

    // Sets the amount of time an idle thread waits before terminating
    private static final int KEEP_ALIVE_TIME = 1;
    private static final int NUMBER_OF_CORES = 1;
    // Sets the Time Unit to seconds
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
    // Creates a thread pool manager
    private final ThreadPoolExecutor mThreadPoolExecutor;

    public FileScanner(Listener listener) {
        mListener = listener;

        mThreadPoolExecutor = new ThreadPoolExecutor(
                NUMBER_OF_CORES,       // Initial pool size
                NUMBER_OF_CORES,       // Max pool size
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                new LinkedBlockingQueue<Runnable>());
    }

    /**
     * Start scanning in a Background Thread
     */
    public void scanInBackground() {
        mThreadPoolExecutor.execute(this);
    }

    @Override
    public void run() {
        // safe check
        if (mScanning) return;

        mScanning = true;

        mFiles.clear();
        StorageUtils.scanDir(StorageUtils.STORAGE_DIR, mFiles);

        // sort descending by size
        Collections.sort(mFiles, comparator);

        // finish
        mScanning = false;

        // callback
        if (mListener != null) {
            mListener.OnScanComplete();
        }
    }

    public List<StorageUtils.FileEntry> getFiles() {
        return mFiles;
    }

    public interface Listener {
        void OnScanComplete();
    }
}
