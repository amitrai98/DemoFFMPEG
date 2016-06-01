package com.example.root.demoffmpeg;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Created by tarun on 31/5/16.
 */
public class FfmpegProcess extends AsyncTask<String, Void, String> {

    public static String ROOT= Environment.getExternalStorageDirectory().toString();
    public static String DIRECTORY="/ffmpeg/";
    public static String OUTPUT_FILE="video.mp4";
    private String packageName=null;
    private final String LOGTAG="FFMPEF";
    private ProgressDialog processDialog=null;
    private Activity activity;
    private String inputFilePath;
    private boolean success=false;
    private FfmpegFinishListener trimInterface;
    private String TAG = getClass().getSimpleName();

    public FfmpegProcess(String packageName, Activity act,String path, FfmpegFinishListener trimInterface){
        this.packageName=packageName;
        this.activity=act;
        inputFilePath=path;
        this.trimInterface=trimInterface;
    }

    @Override
    protected void onPreExecute() {
        // TODO Auto-generated method stub
        super.onPreExecute();
        processDialog=new ProgressDialog((Activity)trimInterface );
        processDialog.setCancelable(false);
        processDialog.setMessage("Please wait...");
        processDialog.show();
    }
    @Override
    protected String doInBackground(String... params) {
        try {
            File file=new File(ROOT + DIRECTORY);
            if(!file.exists()){
                file.mkdirs();
            }
            File outputFile=new File(ROOT + DIRECTORY + OUTPUT_FILE);
            if(outputFile.exists()){
                outputFile.delete();
            }
            String[] ffmpegCommand =MainActivity.trimVideo(inputFilePath, ROOT + DIRECTORY + OUTPUT_FILE, packageName);
            for(int i=0;i<ffmpegCommand.length;i++){
                System.out.println("Length :-"+ffmpegCommand[i]);
            }
            Process ffmpegProcess = new ProcessBuilder(ffmpegCommand).redirectErrorStream(true).start();
            logPercentage(ffmpegProcess);
            //Process ffmpegProcess = Runtime.getRuntime().exec(ffmpegCommand);
            System.out.println("ffmpegProcess is---"+ffmpegProcess );
            OutputStream ffmpegOutStream = ffmpegProcess.getOutputStream();
            InputStreamReader inputStreamReader=new InputStreamReader(ffmpegProcess.getInputStream());
            System.out.println("inputStreamReader"+inputStreamReader);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            System.out.println("reader..."+reader);
            String line;

            Log.v(LOGTAG,"***Starting FFMPEG***");
            System.out.println("line"+reader.readLine());
            while ((line = reader.readLine()) != null)
            {
                Log.v(LOGTAG,"***"+line+"***");

            }

            Log.v(LOGTAG,"***Ending FFMPEG***");
            if (ffmpegProcess != null) {
                ffmpegProcess.destroy();
            }
            success=true;
            System.out.println("returning value is ");
            String path = ROOT + DIRECTORY + OUTPUT_FILE;
            return path;

        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Exception"+ex);
           // processDialog.dismiss();
            success=false;
            return null;
        }

    }

    @Override
    protected void onPostExecute(String result) {
        processDialog.dismiss();
        // TODO Auto-generated method stub
        super.onPostExecute(result);
        System.out.println("Result on post  excecute"+result);
        trimInterface.ffmpegResult(success, result);

    }

    private void logPercentage(final Process p){
        new Thread() {
            public void run() {

                try {
                    Scanner sc = new Scanner(p.getErrorStream());

                    // Find duration
                    Pattern durPattern = Pattern.compile("(?<=Duration: )[^,]*");
                    String dur = sc.findWithinHorizon(durPattern, 0);
                    if (dur == null)
                        throw new RuntimeException("Could not parse duration.");
                    String[] hms = dur.split(":");
                    double totalSecs = Integer.parseInt(hms[0]) * 3600
                            + Integer.parseInt(hms[1]) *   60
                            + Double.parseDouble(hms[2]);
                    System.out.println("Total duration: " + totalSecs + " seconds.");

                    // Find time as long as possible.
                    Pattern timePattern = Pattern.compile("(?<=time=)[\\d.]*");
                    String match;
                    while (null != (match = sc.findWithinHorizon(timePattern, 0))) {
                        double progress = Double.parseDouble(match) / totalSecs;
//                    System.out.printf("Progress: %.2f%%%n", progress * 100);
                        Log.e(TAG, ""+"Progress: %.2f%%%n"+ progress * 100);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        }.start();

    }
}
