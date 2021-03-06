package longo.com.clover;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.text.NumberFormat;
import java.util.List;

/**
 * Created by longngo on 2/3/17.
 */

public class StorageUtils {
    private static final String TAG = StorageUtils.class.getSimpleName();

    public static final File STORAGE_DIR = Environment.getExternalStorageDirectory();
    public static final String STORAGE_DIR_PATH = STORAGE_DIR.toString();

    public static class FileEntry {
        private final static String DIR_FORMAT = "%s/: %s";
        private final static String FILE_FORMAT = "%s: %s";

        public String path;
        public boolean dir;
        public long size;

        public FileEntry(String path, boolean dir, long size) {
            this.path = path;
            this.dir = dir;
            this.size = size;
        }

        public String toString() {
            return String.format(dir ? DIR_FORMAT : FILE_FORMAT, path, NumberFormat.getInstance().format(size));
        }
    }

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state));
    }

    public static boolean isExternalStorageEmpty() {
        return STORAGE_DIR.listFiles() == null || STORAGE_DIR.listFiles().length == 0;
    }

    public static long scanDir(File dir, List<FileEntry> outputList, Listener listener) {
        Log.d(TAG, "scanDir(): " + dir);

        long dirSize = 0;

        // list dirs and files
        File[] files = dir.listFiles();
        if (files != null && files.length > 0) {
            for (int i = 0; i < files.length; i++) {
                final File file = files[i];
                if (file.isDirectory()) {
                    // add size
                    dirSize += scanDir(file, outputList, listener);
                } else {
                    // add size
                    long filesize = file.length();
                    dirSize += filesize;

                    // map this file
                    final FileEntry fileEntry = new FileEntry(file.getPath().substring(STORAGE_DIR_PATH.length()), false, filesize);
                    outputList.add(fileEntry);

                    // callback
                    if (listener != null) {
                        listener.onScanFile(fileEntry);
                    }
                }
            }
        }

        // map this dir
        final FileEntry fileEntry = new FileEntry(dir.getPath().substring(STORAGE_DIR_PATH.length()), true, dirSize);
        outputList.add(fileEntry);

        // callback
        if (listener != null) {
            listener.onScanFile(fileEntry);
        }

        return dirSize;
    }

    public static StringBuilder printSizes(List<StorageUtils.FileEntry> files) {

        StringBuilder sb = new StringBuilder();

        final int size = files.size();
        for (int i = 0; i < size; i++) {
            StorageUtils.FileEntry entry = files.get(i);
            sb.append(entry.toString()).append("\n");
        }

        return sb;
    }

    public interface Listener {
        void onScanFile(FileEntry fileEntry);
    }
}
