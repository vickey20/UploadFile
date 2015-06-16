package com.example.uploadfile;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;

//import com.example.uploadfile.UploadFileActivity;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class StatService extends Service{

	Runnable mTimerRunnable;
	Handler mTimerHandler;
	long t1, t2, t3;
	
	int mFileCounter = 0;
	
	boolean mStopServiceFlag = false;
	
	CPUStats mCpuStats = null;
	ArrayList<CPUStats> mStatsArray = null;
	
	class CPUStats{
		int user, system, top;
		ArrayList<Integer> appCPU;
		ArrayList<String> app;
		
		public CPUStats() {

			user = 0;
			system = 0;
			top = 0;
			
			appCPU = new ArrayList<Integer>();
			app = new ArrayList<String>();
		}
	}
	
	int mTimeCount = 0;
	int mCount = 0;
	
	StringBuilder mStrBuilder = null, mStatsBuilder = null;
	
	
	boolean mComplete = false; 
	
	int mServerResponseCode = 0;
	
	//final String uploadFileUri = "/mnt/sdcard/music.mp3";
    //final String uploadServerUri = "http://10.109.55.70:80/example/test.php";
    
    static final String mDirectory = "mnt/sdcard/Pictures";
	
	final String TAG = "StatService";
	
	static UploadFileActivity mUploadNewInstance = null;
	
	public static void setUploadInstance(UploadFileActivity up) {
		mUploadNewInstance = up;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "onBind");
		return null;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onUnbind");
		
		return super.onUnbind(intent);
	}
	
	@Override
	public void onCreate() {
		Log.d(TAG, "Service Created");
		mStatsArray = new ArrayList<CPUStats>();
		super.onCreate();
	}
	
	public class MyBinder extends Binder {
	    StatService getService() {
	      return StatService.this;
	    }
	}
	
	@Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.d(TAG, "onStart");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	Log.d(TAG, "onStartCommand");
    	
    	mStrBuilder = new StringBuilder();
    	mStatsBuilder = new StringBuilder();
    	
    	startHandler();
        return Service.START_STICKY;
    }
    
    @Override
    public void onDestroy() {
    	Log.d(TAG, "onDestroy");
    	mStopServiceFlag = true;
		
		mTimerHandler = null;
		Log.d(TAG,"adb top stopped");
		
		mStrBuilder.append("\n\n\nadb top stopped\n\n\n");
		
		if(mFileCounter == 0){
			new BackgroundTask("log", mStrBuilder, mStatsArray).execute();
		}
		UploadFileActivity.mUploadFileUri = null;
    	super.onDestroy();
    }
    
    public void startHandler() {
    	
		mTimerHandler = new Handler();
		mTimerRunnable = new Runnable() {

			@Override
			public void run() {
				
				if(mTimerHandler != null){
					
					mTimeCount++;

					adbDump();
					
					if(UploadFileActivity.mUploadFileUri != null && mTimeCount == 10){
						new UploadTask().execute(UploadFileActivity.mUploadFileUri);
					}
					
					if (mComplete) {
						mCount++;
					}
					
					if(mCount == 10 || mStopServiceFlag == true){
						mTimerHandler = null;
						Log.d(TAG,"adb top stopped");
						
						mStrBuilder.append("\n\n\nadb top stopped\n\n\n");
						if(mFileCounter == 0){
							new BackgroundTask("log", mStrBuilder, mStatsArray).execute();
						}
					}else{
						mTimerHandler.postDelayed(this, 1000);
					}
				}
			}
		};

		mTimerHandler.postDelayed(mTimerRunnable, 0);
    	
	}

    class BackgroundTask extends AsyncTask<String, Void, String>
    {
	 	String file;
	 	StringBuilder content;
	 	ArrayList<CPUStats> statsArray;
	 	
	 	public BackgroundTask( String file, StringBuilder content, ArrayList<CPUStats> statsArray ) {
			this.file = file;
			this.file = file + System.currentTimeMillis();
			this.content = content;
			this.statsArray = statsArray;
		}
	 	
		@Override
		protected String doInBackground(String... params) {
			
			try
			{
				
				File folder = new File(mDirectory);
				if (!folder.exists()) {
					folder.mkdir();
				}
				
				String filename = mDirectory + "/" + file + ".txt";
				String user = mDirectory + "/" + "user.txt";
				String system = mDirectory + "/" + "system.txt";
				String time = mDirectory + "/" + "time.txt";
				String top = mDirectory + "/" + "top.txt";
				
				File myFile = new File( filename );
				File userFile = new File( user );
				File systemFile = new File( system );
				File timeFile = new File( time );
				File topFile = new File( top );
				
				if( !myFile.exists() ){
					myFile.createNewFile();
				}
				if( !userFile.exists() ){
					userFile.createNewFile();
				}
				if( !systemFile.exists() ){
					systemFile.createNewFile();
				}
				if( !timeFile.exists() ){
					timeFile.createNewFile();
				}
				if( !topFile.exists() ){
					topFile.createNewFile();
				}
				
				FileWriter fw = new FileWriter(myFile,true);
				fw.write(content.toString());
				
				if(mStatsBuilder != null){
					mStatsBuilder.append("\n\n");
					fw.write(mStatsBuilder.toString());
				}
				
				fw.close();

				if( statsArray != null && !statsArray.isEmpty() ){
						
					StringBuilder sb1 = new StringBuilder();
					StringBuilder sb2 = new StringBuilder();
					StringBuilder sb3 = new StringBuilder();
					StringBuilder sb4 = new StringBuilder();
					
					int i = 1;
					for(CPUStats stats : statsArray){
						sb1.append(stats.user);
						sb1.append("\n");
						
						sb2.append(stats.system);
						sb2.append("\n");
						
						sb3.append(String.valueOf(i));
						sb3.append("\n");
						
						sb4.append(stats.top);
						sb4.append("\n");
						
						i++;
					}
					
					FileWriter fw1 = new FileWriter(userFile,true);
					fw1.write(sb1.toString());
					fw1.close();
					
					FileWriter fw2 = new FileWriter(systemFile,true);
					fw2.write(sb2.toString());
					fw2.close();
					
					FileWriter fw3 = new FileWriter(timeFile,true);
					fw3.write(sb3.toString());
					fw3.close();
					
					FileWriter fw4 = new FileWriter(topFile,true);
					fw4.write(sb4.toString());
					fw4.close();
				}
				
				Log.d("SaveContents", "Done writing to file.");
				
			}	
			catch(Exception e)
			{
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			mFileCounter++;
		}
    	
    }

    
    
    public void adbDump() {
		try {
			
			mStrBuilder.append("\nIteration: " + mTimeCount + "\n\n");
			
			Log.d(TAG, "Iteration: " + mTimeCount);
			
			// -m 10, how many entries you want, -d 1, delay by how much, -n 1,
			// number of iterations
			Process p = Runtime.getRuntime().exec("top -m 15 -d 1 -n 1");
			//Process p = Runtime.getRuntime().exec("top cat /proc/${pid}/task/*/stat | awk -F \'{print $1, $14}'");

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			int i = 0;
			String line = reader.readLine();
			Log.d(TAG, line);
			mCpuStats = new CPUStats();
			
			while (line != null) {
				Log.e("Output "  + i, line);
				mStrBuilder.append(line + "\n");

				if(i == 3){
					String[] arr = line.split(",[ ]*");
					String[] usr = arr[0].split(" ");
					String[] sys = arr[1].split(" ");
					
					String us = (String) usr[1].subSequence(0, usr[1].length() - 1);
					String sy = (String) sys[1].subSequence(0, sys[1].length() - 1);
							
					mCpuStats.user = Integer.parseInt(us);
					mCpuStats.system = Integer.parseInt(sy);
					
					mStatsBuilder.append("\n\n");
					mStatsBuilder.append("User        System\n");
					mStatsBuilder.append(us + "        " + sy);
				}
				
				if(i >= 7){
					String[] tmp = line.trim().replaceAll(" +", " ").split(" ");
					
					String cpu = (String) tmp[2].subSequence(0, tmp[2].length() - 1);
					int l = tmp.length;
					String app = ((String) tmp[l - 1]).trim();
					
					mStatsBuilder.append("\n");
					mStatsBuilder.append(cpu + "    " + app);
					mStatsBuilder.append("\n");
					
					if(app.equals("top")){
						mCpuStats.top = Integer.parseInt(cpu);
					}
					
					mCpuStats.appCPU.add(Integer.parseInt(cpu));
					mCpuStats.app.add(app);
				}
				
				line = reader.readLine();
				i++  ;
			}

			mStatsArray.add(mCpuStats);
			
			mCpuStats = null;
			
			p.waitFor();
			
			mStrBuilder.append("\n\n\n\n");

		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(getBaseContext(), "Caught", Toast.LENGTH_SHORT)
					.show();
			mTimerHandler = null;
		}

	}
    
    
    class UploadTask extends AsyncTask<String, Void, Integer>{

    	@Override
    	protected void onPreExecute() {
    		super.onPreExecute();
    	}
    	
		@Override
		protected Integer doInBackground(String... params) {
			
			t1 = System.currentTimeMillis();

			return uploadFile(params[0]);
			
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);

			mComplete = true;
			
			if(result == 0){
				Log.d(TAG,"Source File not exist : " + UploadFileActivity.mUploadFileUri);
				
				mStrBuilder.append("\n\n\nSource File not exist : " + UploadFileActivity.mUploadFileUri + "\n\n\n");
			}
			else if(result == 1){
				t2 = System.currentTimeMillis();
	            t3 = t2-t1;
	            
				String msg = "File Upload Completed. \n Time: " + t3;
                
				mStrBuilder.append("\n\n\n" + msg + "\n\n\n");
				
				Log.d(TAG,msg);
                Toast.makeText(StatService.this, "File Upload Complete.",
                             Toast.LENGTH_LONG).show();
                Log.d(TAG,"Stopping in 10 seconds");
                mStrBuilder.append("\n\n\nStopping in 10 seconds\n\n\n");
			}
			else if(result == 2){
				Log.d(TAG,"MalformedURLException Exception : check script url.");
				mStrBuilder.append("\n\n\nMalformedURLException Exception : check script url.\n\n\n");
                Toast.makeText(StatService.this, "MalformedURLException",
                                                    Toast.LENGTH_SHORT).show();
			}

		}
    	
    }
    
    
    public int uploadFile(String sourceFileUri) {
           
    	t1 = System.currentTimeMillis();
    	
    	String filePath[] = sourceFileUri.split("/");
          String fileName = filePath[filePath.length - 1];
          Log.d("uploadFile", "Filename: " + fileName);
          mStrBuilder.append("\n\n\nFilename: " + fileName +"\n\n\n");
  
          fileName = URLEncoder.encode(fileName);
          
          HttpURLConnection conn = null;
          DataOutputStream dos = null; 
          String lineEnd = "\r\n";
          String twoHyphens = "--";
          String boundary = "*****";
          int bytesRead, bytesAvailable, bufferSize;
          byte[] buffer;
          int maxBufferSize = 1 * 1024 * 1024;
          File sourceFile = new File(sourceFileUri);
           
          if (!sourceFile.isFile()) {
               
               Log.e("uploadFile", "Source File not exist : " + UploadFileActivity.mUploadFileUri);
                
               return 0;
            
          }
          else
          {
               try {
            	   
            	   Log.d(TAG,"Upload Started");
            	   
            	   mStrBuilder.append("\n\n\nUpload Started\n\n\n");
                    
                     // open a URL connection to the Servlet
                   FileInputStream fileInputStream = new FileInputStream(sourceFile);
                   URL url = new URL(UploadFileActivity.mUploadServerUri);
                    
                   // Open a HTTP  connection to  the URL
                   conn = (HttpURLConnection) url.openConnection();
                   conn.setDoInput(true); // Allow Inputs
                   conn.setDoOutput(true); // Allow Outputs
                   conn.setUseCaches(false); // Don't use a Cached Copy
                   conn.setRequestMethod("POST");
                   conn.setRequestProperty("Connection", "Keep-Alive");
                   //conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                   conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                   conn.setRequestProperty("uploaded_file", fileName);
                   conn.setConnectTimeout(5*6*10*1000);
                    
                   dos = new DataOutputStream(conn.getOutputStream());
          
                   dos.writeBytes(twoHyphens + boundary + lineEnd);
                   dos.writeBytes("Content-Disposition: form-data; name=uploaded_file;filename=" + fileName + "" + lineEnd);
                   dos.writeBytes(lineEnd);
          
                   buffer = new byte[8192];
                   bytesRead = 0;
                   while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                       dos.write(buffer, 0, bytesRead);
                   }
                   
                   // send multipart form data necesssary after file data...
                   dos.writeBytes(lineEnd);
                   dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
          
                   InputStream is = conn.getInputStream();
                   int ch;
                   StringBuffer sb = new StringBuffer();
                   while ((ch = is.read()) != -1) {
                     sb.append((char) ch);
                   }
                   String response = sb.toString();
                   
                   Log.d(TAG, "response: " + response);
                   
                   // Responses from the server (code and message)
                   mServerResponseCode = conn.getResponseCode();
                   String serverResponseMessage = conn.getResponseMessage();
                     
                   Log.i("uploadFile", "HTTP Response is : "
                           + serverResponseMessage + ": " + mServerResponseCode);
                    
                   //close the streams //
                   fileInputStream.close();
                   dos.flush();
                   dos.close();
                   Log.d(TAG,"Upload finished");
                   mStrBuilder.append("\n\n\nUpload Finished\n\n\n");
                   
              } catch (Exception ex) {
                   
                  ex.printStackTrace();
                  Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
                  mStrBuilder.append("\n\n\nerror: " + ex.getMessage() + "\n\n\n");
                  
              }
               
          }    
          
          if(mServerResponseCode == 200){
              return 1;         
          }   
          else{
        	  return 2;
          }
          
     }
    
}