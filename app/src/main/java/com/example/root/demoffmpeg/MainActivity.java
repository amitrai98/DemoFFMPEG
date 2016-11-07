package com.example.root.demoffmpeg;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.root.demoffmpeg.util.FileMover;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity implements FfmpegFinishListener{
    private ProgressDialog progressDialog;

    private TextView txt_filepath = null;

    private String[] libraryAssets = {"ffmpeg"};
    private String TAG = getClass().getSimpleName();
    private int REQUEST_TAKE_GALLERY_VIDEO = 101;
    private String selected_path = null;
    private int VIDEO_CAPTURED = 102;

    private Button btn_compress = null, btn_select_video = null, btn_record = null;
    private int REQUEST_CAPTURE_VIDEO = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        moveFFMPEG(getPackageName());
        init();
    }

    private void init(){
        txt_filepath = (TextView) findViewById(R.id.txt_filepath);
        btn_compress = (Button) findViewById(R.id.btn_compress);
        btn_select_video = (Button) findViewById(R.id.btn_select_video);
        btn_record = (Button) findViewById(R.id.btn_record);

        btn_compress.setEnabled(false);
        btn_record.setEnabled(false);
        btn_select_video.setEnabled(false);

        selected_path = null;

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAPTURE_VIDEO_OUTPUT)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAPTURE_VIDEO_OUTPUT)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.CAMERA, Manifest.permission.CAPTURE_VIDEO_OUTPUT},
                        REQUEST_CAPTURE_VIDEO);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CAPTURE_VIDEO);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }

            // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
            // app-defined int constant

            return;
        }else {
            btn_record.setEnabled(true);
            btn_select_video.setEnabled(true);
        }
    }

    public void startCompression(View view) {
        Log.i("FFMPEG", "Inside call");
        try {
            File f = new File(selected_path);
            if(f.exists())
                callTrimAsynctask(selected_path);
            else
                Log.e(TAG, "invalid file");

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void recordVideo(View view){
        Intent captureVideoIntent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
        captureVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 10);
        captureVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
        startActivityForResult(captureVideoIntent, VIDEO_CAPTURED);
    }

    public void selectVideo(View view){
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Video"),REQUEST_TAKE_GALLERY_VIDEO);
    }


    /********
     * ************************************************************************************************
     * ************************************************************************************************
     * *******************implementing FFmpeg library for video ****************************************
     * **************************************************************************************************
     * **************************************************************************************************
     *
     */

    private boolean isFFMPEGCopied(String packageName)
    {
        File ffmpegFile=new File("/data/data/"+packageName+"/ffmpeg" );
        if(ffmpegFile.exists()){
            Log.i("FFMPEG", "FFmpeg lib exists");
            return true;
        }
        else{
            return false;
        }
    }
    private void moveFFMPEG(String packageName){
        progressDialog=new ProgressDialog(MainActivity.this);
        if(!isFFMPEGCopied(packageName)){
            progressDialog.show();
            for (int i = 0; i < libraryAssets.length; i++) {
                try {
                    InputStream ffmpegInputStream = this.getAssets().open(libraryAssets[i]);
                    FileMover fm = new FileMover(ffmpegInputStream,"/data/data/"+packageName+"/" + libraryAssets[i]);
                    fm.moveIt();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        if(progressDialog.isShowing()){
            progressDialog.dismiss();
        }
        setFFMPEFGPermission(packageName);
    }


   private void setFFMPEFGPermission(String packageName){
        String[] args = {"/system/bin/chmod", "755", "/data/data/"+packageName+"/ffmpeg"};
        Process process=null;

        try {
            process = new ProcessBuilder(args).start();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        process.destroy();

    }


    public void callTrimAsynctask(String vPath){

        //Ready to merge two videos
        String packageName=getApplicationContext().getPackageName();
        FfmpegProcess processVideo=new FfmpegProcess(packageName,MainActivity.this,vPath,MainActivity.this);
        processVideo.execute(new String[]{"1"});
    }

    /***function to trim the video**/
    public static  String[] trimVideo(String inputFile, String outputFile, String packageName) {
//        return new String[] {
//                "/data/data/"+packageName+"/ffmpeg",
//                "-y", "-i", inputFile, "-strict", "experimental", "-s", "480x360", "-r", "25", "-vcodec", "mpeg4", "-b", "150k", "-ab", "48000",
//                "-ac", "2", "-ar", "22050", outputFile };

//        return new String[] { "/data/data/"+packageName+"/ffmpeg ",
//                "-y", "-i", inputFile,"-s","480x320","-r","20","-c:v","480x360",
//                        "-present","ultrafast","-c:a","copy","-me_method zero","-tune",
//                        "fastdecode","-tune","zerolatency","-strict","-2","-b:v","1000k",
//                        "-pix_fmt","yuv420p",outputFile};

        return new String[] { "/data/data/"+packageName+"/ffmpeg",
                "-i",
                inputFile, "-s","480x320","-acodec","mp2",
                "-strict","-2","-ac","1","-ar","16000","-r","13","-ab","32000",
                "-aspect","3:2",outputFile};
    }


    @Override
    public void ffmpegResult(boolean success, String msg) {
        if (success) {
            // TODO Auto-generated method stub
            final File path = new File(FfmpegProcess.ROOT + FfmpegProcess.DIRECTORY);
            File outputFile = new File(FfmpegProcess.ROOT + FfmpegProcess.DIRECTORY + FfmpegProcess.OUTPUT_FILE);
            String absolutePath = outputFile.getAbsolutePath();
            Log.e(TAG, "output is at "+absolutePath);
            try {
                File f = new File(selected_path);
                Log.e(TAG , "input file size"+f.length()+"output file size is "+
                        outputFile.length());
            }catch (Exception e){
                e.printStackTrace();
            }
            //Preferences.saveVideoPath(getApplicationContext(), absolutePath);
            /*split(absolutePath);
            setFrameList();*/
        } else {
            Toast.makeText(this, "Failed", Toast.LENGTH_LONG).show();
        }
    }





    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_TAKE_GALLERY_VIDEO) {
                Uri selectedImageUri = data.getData();

                String path = getPath(MainActivity.this, selectedImageUri);

                selected_path = path;
                if(selected_path != null && !selected_path.isEmpty())
                    btn_compress.setEnabled(true);


            }else  if (requestCode == VIDEO_CAPTURED) {
                Uri videoUri = data.getData();
                Log.e(TAG, ""+videoUri);
                try {
//                    String path = videoUri.toString(); // "/mnt/sdcard/FileName.mp3"
//                    File file = new File(new URI(path));
                    String absolute_path = getPath(this, videoUri);
                    Log.e(TAG, ""+absolute_path);
                    selected_path = absolute_path;
                    if(selected_path != null && !selected_path.isEmpty())
                        btn_compress.setEnabled(true);

                } catch (Exception e) {
                    e.printStackTrace();
                }
//                mVideoView.setVideoURI(videoUri);
            }
        }
    }



    public static String getPath(final Context context, final Uri uri)
    {
        final boolean isKitKatOrAbove = Build.VERSION.SDK_INT >=  Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (isKitKatOrAbove && DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }

                    // TODO handle non-primary volumes
                }
                // DownloadsProvider
                else if (isDownloadsDocument(uri)) {

                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                    return getDataColumn(context, contentUri, null, null);
                }
                // MediaProvider
                else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }

                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[] {
                            split[1]
                    };

                    return getDataColumn(context, contentUri, selection, selectionArgs);
                }
            }
            // MediaStore (and general)
            else if ("content".equalsIgnoreCase(uri.getScheme())) {
                return getDataColumn(context, uri, null, null);
            }
            // File
            else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1001: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    btn_record.setEnabled(true);
                    btn_select_video.setEnabled(true);

                } else {
                    Log.e(TAG, "permission denied");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}
