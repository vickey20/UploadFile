package com.example.uploadfile;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
  
public class UploadFileActivity extends Activity implements View.OnClickListener{

/*	// load the library - name matches jni/Android.mk
	  static {
	    System.loadLibrary("ndktest");
	  }
	  
	// declare the native code function - must match ndktest.c
	  private native String invokeNativeFunction();
	*/
	
	static final String TAG = "UpLoadFileActivity";
	
    TextView mTextViewMessage, mTextViewFilePath, mTextViewServiceInfo;
    Button mButtonUpload, mButtonSelectFile, mButtonStart, mButtonStop;
    EditText mEditTextServerAddress;

    Handler mTimerHandler;
    
    int mServerResponseCode = 0;
    ProgressDialog mProgressDialog = null;
    
    static StatService mStatService = null;
    Intent mIntent = null;    
    
    long t1, t2, t3;
    
    static String mUploadServerUri = null;
    static String mUploadFileUri = null;
    

    //Create Service connection
    private ServiceConnection mConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mStatService = null;
			Toast.makeText(UploadFileActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mStatService = ((StatService.MyBinder) service).getService();
			Toast.makeText(UploadFileActivity.this, "Connected", Toast.LENGTH_SHORT).show();
		}
	};
    
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.start:
        	
        	mUploadServerUri = mEditTextServerAddress.getText().toString();
        	
            mIntent = new Intent(this,StatService.class);
            startService(mIntent);
            
            Intent bindIntent = new Intent(this, StatService.class);
            
            if(false == bindService(bindIntent, mConnection, 0))
            {	
            	Log.d(TAG, "Fail to find with service");
            }
            else{
            	mTextViewServiceInfo.setText("Service is running");
            }
            
            break;
        case R.id.stop:
            
            mIntent = new Intent(this,StatService.class);
            stopService(mIntent);
            mTextViewServiceInfo.setText("Service has been stopped");
            break;
        }
    }

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
         
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
          
     // this is where we call the native code
        //String hello = invokeNativeFunction();
         
        //new AlertDialog.Builder(this).setMessage(hello).show();
        
        mTimerHandler = new Handler();
        
        mButtonUpload = (Button) findViewById(R.id.buttonUpload);
        mButtonSelectFile = (Button) findViewById(R.id.selectFile);
        
        mButtonStart = (Button) findViewById(R.id.start);
        mButtonStop = (Button) findViewById(R.id.stop);
        
        mTextViewMessage  = (TextView) findViewById(R.id.messageText);
        mTextViewFilePath  = (TextView) findViewById(R.id.textViewFile);
        mTextViewServiceInfo  = (TextView) findViewById(R.id.textViewService);
        
        mEditTextServerAddress = (EditText) findViewById(R.id.editTextServerAddress);
        mEditTextServerAddress.setText("http://10.109.55.246:80/example/test.php");
        
        mButtonStart.setOnClickListener(this);
        mButtonStop.setOnClickListener(this);
        
        mButtonSelectFile.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent fileintent = new Intent(Intent.ACTION_GET_CONTENT);
                fileintent.setType("gagt/sdf");
                try {
                    startActivityForResult(fileintent, 100);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "No activity can handle picking a file.");
                    e.printStackTrace();
                }
			}
		});
        
        mButtonUpload.setOnClickListener(new OnClickListener() {           
            @Override
            public void onClick(View v) {
            	
            	mUploadServerUri = mEditTextServerAddress.getText().toString();
            	//uploadServerUri = uploadServerUri + "/example/test.php";
            	
            	if(mUploadFileUri != null && mUploadServerUri != null){
            		//Toast.makeText(UploadNew.this, "Upload will start in 10 seconds", Toast.LENGTH_LONG).show();
            		mTimerHandler.postDelayed(timerRunnable, 0);
            	}
            	else{
            		Toast.makeText(UploadFileActivity.this, "Select file and enter server address", 8000).show();
            	}
            }    
        });
        
    }
    
    
	Runnable timerRunnable = new Runnable() {

		@Override
		public void run() {
			
			if(mTimerHandler != null){
				new UploadTask().execute(mUploadFileUri);
			}
		}
	};

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Fix no activity available
    	Log.d(TAG, "request code: " + requestCode + "resultcode: "+ resultCode);
        if (data == null)
            return;
        switch (requestCode) {
        case 100:
            if (resultCode == RESULT_OK) {
                mUploadFileUri = data.getData().getPath();
                mTextViewFilePath.setText("Filepath: " + mUploadFileUri);
            }
        }
    }     
    
    class UploadTask extends AsyncTask<String, Void, Integer>{

    	@Override
    	protected void onPreExecute() {
    		super.onPreExecute();
    		
    		mProgressDialog = ProgressDialog.show(UploadFileActivity.this, "", "Uploading file...", true);
    	}
    	
		@Override
		protected Integer doInBackground(String... params) {
			
			t1 = System.currentTimeMillis();

			return uploadFile(params[0]);
			
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			
			if(result == 0){
				mTextViewMessage.setText("Source file does not exist : " + mUploadFileUri);
			}
			else if(result == 1){
				t2 = System.currentTimeMillis();
	            t3 = t2-t1;
	            
				String msg = "File Upload Completed. \n Time: " + t3;
                
                mTextViewMessage.setText(msg);
                Toast.makeText(UploadFileActivity.this, "File Upload Complete.",
                             Toast.LENGTH_SHORT).show();
			}
			else if(result == 2){
				mTextViewMessage.setText("MalformedURLException Exception : check script url.");
                Toast.makeText(UploadFileActivity.this, "MalformedURLException",
                                                    Toast.LENGTH_SHORT).show();
			}

			if(mProgressDialog != null){
				mProgressDialog.dismiss();
			}
		}
    	
    }
    
    
    public int uploadFile(String sourceFileUri) {
           
    	t1 = System.currentTimeMillis();
    	
    	String filePath[] = sourceFileUri.split("/");
          String fileName = filePath[filePath.length - 1];
          Log.d(TAG, "Filename: " + fileName);
  
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
               
               Log.e(TAG, "Source file does not exist : " + mUploadFileUri);
                
               return 0;
            
          }
          else
          {
               try {
                    
                     // open a URL connection to the Servlet
                   FileInputStream fileInputStream = new FileInputStream(sourceFile);
                   URL url = new URL(mUploadServerUri);
                    
                   // Open a HTTP  connection to  the URL
                   conn = (HttpURLConnection) url.openConnection();
                   conn.setDoInput(true); // Allow Inputs
                   conn.setDoOutput(true); // Allow Outputs
                   conn.setUseCaches(false); // Don't use a Cached Copy
                   conn.setRequestMethod("POST");
                   conn.setRequestProperty("Connection", "Keep-Alive");
                   conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                   conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                   conn.setRequestProperty("uploaded_file", fileName);
                   conn.setConnectTimeout(5*6*10*1000);
                    
                   dos = new DataOutputStream(conn.getOutputStream());
          
                   dos.writeBytes(twoHyphens + boundary + lineEnd);
                   dos.writeBytes("Content-Disposition: form-data; name=uploaded_file;filename=" + fileName + "" + lineEnd);
                    
                   dos.writeBytes(lineEnd);
          
                   // create a buffer of  maximum size
                   bytesAvailable = fileInputStream.available();
          
                   bufferSize = Math.min(bytesAvailable, maxBufferSize);
                   buffer = new byte[bufferSize];
          
                   // read file and write it into form...
                   bytesRead = fileInputStream.read(buffer, 0, bufferSize); 
                      
                   while (bytesRead > 0) {
                        
                     dos.write(buffer, 0, bufferSize);
                     bytesAvailable = fileInputStream.available();
                     bufferSize = Math.min(bytesAvailable, maxBufferSize);
                     bytesRead = fileInputStream.read(buffer, 0, bufferSize);  
                      
                    }
          
                   // send multipart form data necesssary after file data...
                   dos.writeBytes(lineEnd);
                   dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
          
                   // Responses from the server (code and message)
                   mServerResponseCode = conn.getResponseCode();
                   String serverResponseMessage = conn.getResponseMessage();
                     
                   Log.i(TAG, "HTTP Response is : "
                           + serverResponseMessage + ": " + mServerResponseCode);
                    
                   //close the streams //
                   fileInputStream.close();
                   dos.flush();
                   dos.close();
                   
              } catch (Exception ex) {
                   
                  ex.printStackTrace();
                  Log.e(TAG, "error: " + ex.getMessage(), ex);
                  
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