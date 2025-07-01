package org.rti.ttfinder.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import org.rti.ttfinder.AssessmentActivity;
import org.rti.ttfinder.data.entity.Assessment;
import org.rti.ttfinder.data.helper.DaoHelper;
import org.rti.ttfinder.data.helper.DbLoaderInterface;
import org.rti.ttfinder.data.loader.AssessmentLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

import kotlin.jvm.internal.Intrinsics;

public class AppUtils {

    public static Bitmap loadBitmap(Context mContext,int resource) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inSampleSize = 2;
        return BitmapFactory.decodeResource(mContext.getResources(), resource, options);
    }

    public static Bitmap loadBitmap(String path) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inSampleSize = 2;
        return BitmapFactory.decodeFile(path, options);
    }

    public static void alert(Context context,String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(context);
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        bld.create().show();
    }

    public static String getDate(long milliSeconds)
    {
        // Create a DateFormatter object for displaying date in specified format.
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss a");
        //formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }

    public static void showToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    public static File getImageOutputDirectory (Context mCOntext){
        String root;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                    .toString();
        } else {
            root = Environment.getExternalStorageDirectory().toString();
        }
        File myDir = new File(root + "/TTScreener/Images");
        myDir.mkdirs();
        File var3;
        if (myDir.exists()) {
            var3 = myDir;
        } else {
            var3 = mCOntext.getFilesDir();
            Intrinsics.checkNotNullExpressionValue(var3, "filesDir");
        }
        return var3;
    }

    public static File getLogImageOutputDirectory (Context mContext){
        String root;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                    .toString();
        } else {
            root = Environment.getExternalStorageDirectory().toString();
        }
        File myDir = new File(root + "/TTScreener/TTScreenerMLAppLogs");
        myDir.mkdirs();
        File var3;
        if (myDir.exists()) {
            var3 = myDir;
        } else {
            var3 = mContext.getFilesDir();
            Intrinsics.checkNotNullExpressionValue(var3, "filesDir");
        }
        return var3;
    }

    public static File getExportLogImageOutputDirectory (Context mContext, String folder){
        String root;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                    .toString();
        } else {
            root = Environment.getExternalStorageDirectory().toString();
        }
        File myDir = new File(root + "/TTScreener/"+folder+"/TTScreenerMLAppLogs");
        myDir.mkdirs();
        File var3;
        if (myDir.exists()) {
            var3 = myDir;
        } else {
            var3 = mContext.getFilesDir();
            Intrinsics.checkNotNullExpressionValue(var3, "filesDir");
        }
        return var3;
    }

    public static File getExportImageOutputDirectory (Context mContext, String folder){
        String root;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                    .toString();
        } else {
            root = Environment.getExternalStorageDirectory().toString();
        }
        File myDir = new File(root + "/TTScreener/"+folder+"/Images");
        myDir.mkdirs();
        File var3;
        if (myDir.exists()) {
            var3 = myDir;
        } else {
            var3 = mContext.getFilesDir();
            Intrinsics.checkNotNullExpressionValue(var3, "filesDir");
        }
        return var3;
    }
    public static File getExportRejectedImageOutputDirectory (Context mContext, String folder){
        String root;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                    .toString();
        } else {
            root = Environment.getExternalStorageDirectory().toString();
        }
        File myDir = new File(root + "/TTScreener/"+folder+"/RejectedImages");
        myDir.mkdirs();
        File var3;
        if (myDir.exists()) {
            var3 = myDir;
        } else {
            var3 = mContext.getFilesDir();
            Intrinsics.checkNotNullExpressionValue(var3, "filesDir");
        }
        return var3;
    }
    public static File getRejectedImageOutputDirectory (Context mContext){
        String root;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                    .toString();
        } else {
            root = Environment.getExternalStorageDirectory().toString();
        }
        File myDir = new File(root + "/TTScreener/RejectedImages");
        myDir.mkdirs();
        File var3;
        if (myDir.exists()) {
            var3 = myDir;
        } else {
            var3 = mContext.getFilesDir();
            Intrinsics.checkNotNullExpressionValue(var3, "filesDir");
        }
        return var3;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void copyDirectoryOneLocationToAnotherLocation(File sourceLocation, File targetLocation)
            throws IOException {

        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }

            File[] children = sourceLocation.listFiles();
            for (int i = 0; i < sourceLocation.listFiles().length; i++) {
                copyDirectoryOneLocationToAnotherLocation(children[i], targetLocation);
            }
        } else {
            InputStream in = Files.newInputStream(sourceLocation.toPath());
            OutputStream out = null;
            if(!targetLocation.isDirectory()){
                out = Files.newOutputStream(targetLocation.toPath());
            }
            else{
                out = Files.newOutputStream(Paths.get(new File(targetLocation, sourceLocation.getName()).getPath()));
            }
            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }

    public static void deleteRecursive(File fileOrDirectory) {

        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();

    }


}
