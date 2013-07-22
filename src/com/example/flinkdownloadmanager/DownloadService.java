package com.example.flinkdownloadmanager;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.io.InputStream;
import java.io.OutputStream;

import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

public class DownloadService extends Service{
	
	
	private final IBinder mBinder = new LocalBinder();
	NotificationManager mNotifyManager;
	final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
	private int NotiID = 0;
	private int tFinished = 0;

	public int getDownloadCount() {	return NotiID;}
	public int getCurDownloadCount() {	return NotiID - tFinished;}
	
	public class LocalBinder extends Binder {
		DownloadService getService() {
			return DownloadService.this;
        }
    }
	
	private PendingIntent pIntent;
	private Intent mainIntent;
	
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	@Override
	public void onCreate ()
	{
		IntentFilter filter = new IntentFilter();
		filter.addAction("broadcast.download");
		filter.addAction("broadcast.getcount");
		registerReceiver(Receiver, filter);
		
		mainIntent = new Intent(this, MainActivity.class);
		pIntent = PendingIntent.getActivity(this, 0, mainIntent, 0);
		
		Toast.makeText(this, "Service Started", Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	@Override
    public void onDestroy() {
		unregisterReceiver(Receiver);
        Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show();
    }
	
	public void sendCurCounter()
	{
		Intent intent = new Intent();
		intent.setAction("broadcast.count");
		intent.putExtra("count", Integer.toString(NotiID - tFinished));
		sendBroadcast(intent);
	}
	
	public void downloadFile(String uri, int id)
	{
		int count;
		int tContent = 0;
		int curID = NotiID++;
		boolean NotiSet = false;
        try {
        	
            URL url = new URL(uri );
          
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setInstanceFollowRedirects(true);
            if (connection.getContentType().startsWith("image"))
            	tContent = 1;
            else if (connection.getContentType().startsWith("text"))
            	tContent = 2;
            else
            {
            	Message msg = handler.obtainMessage();
            	msg.arg1 = 3;
            	handler.sendMessage(msg);
            	return;
            }
            
            connection.connect();

            int length = connection.getContentLength();

            InputStream input = new BufferedInputStream(url.openStream(), 8192);

            String filename = url.getFile().split("/")[url.getFile().split("/").length-1];
            OutputStream output = new FileOutputStream("/sdcard/Download/" + filename);

            byte data[] = new byte[1024];
            long total = 0;

            
            mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            
            mBuilder.setContentTitle(filename)
                .setContentText("Download in progress")
                .setContentIntent(pIntent)
                .setContentInfo((int)(length/1024) + "KB")
                .setTicker("Downloading " + filename)
                .setSmallIcon(android.R.drawable.ic_menu_save);
            

            mNotifyManager.notify(curID, mBuilder.build());
            
           
            sendCurCounter();
            updateProgress(uri, id, 0, 0, length, 0, connection.getContentType() );
            
            long startTime = System.nanoTime(), endTime = 0;
            while ((count = input.read(data)) != -1) {
            	endTime = System.nanoTime();
            	
                total += count;
 
                if (total <= length)
                {
	                mBuilder.setContentText("Download in progress " + total + "/" + length + "  " + (int)((total*100)/length) + "%" + "    " + (int)((count * 1000000)/(endTime - startTime))/1024 + " Kb/s")
	                		.setProgress(100, (int)((total*100)/length), false)
	                		.setTicker(null);
	                mNotifyManager.notify(curID, mBuilder.build());
                }
                
                NotiSet = true;
                updateProgress(uri, id, (int)((total*100)/length), (int)((count * 1000000)/(endTime - startTime))/1024, length, total, connection.getContentType());
                output.write(data, 0, count);
                startTime = System.nanoTime();
            }

            mNotifyManager.cancel(curID);
            
            mBuilder.setContentText("Download complete")
            		.setAutoCancel(true)
                    .setProgress(0,0,false);
            
            Intent iintent = null;
            PendingIntent ppIntent = null;
            if (tContent == 1)
            {
            	iintent = new Intent(Intent.ACTION_VIEW); 
            	iintent.setDataAndType(Uri.parse("file:///sdcard/Download/" + filename), "image/*");
            }
            else if (tContent == 2)
            {
            	iintent = new Intent(Intent.ACTION_EDIT); 
            	iintent.setDataAndType(Uri.parse("file:///sdcard/Download/" + filename), "text/*");
            }
            
            ppIntent = PendingIntent.getActivity(this, 0, iintent, 0);
            mBuilder.setContentIntent(ppIntent)
            		.setTicker("Downloaded " + filename);      
            mNotifyManager.notify(curID, mBuilder.build());
            tFinished++;
            sendCurCounter();
            
            updateProgress(uri, id, 100, (int)((count * 1000000)/(endTime - startTime))/1024, length, total, connection.getContentType());
            
            output.flush();
            output.close();
            input.close();
            
            //this.wait(1000);
            //updateProgress(uri, id, 100, (int)((count * 1000000)/(endTime - startTime))/1024, length, total, connection.getContentType());
            
        } catch (SocketTimeoutException e2) {
            tFinished++;
            sendCurCounter();
            
            Message msg = handler.obtainMessage();
            msg.arg1 = 1;
            handler.sendMessage(msg);
            
        } catch (IOException e1) {
        	
        	Message msg = handler.obtainMessage();
            msg.arg1 = 2;
            handler.sendMessage(msg);
        	
        	if (NotiSet)
        	{
	            mNotifyManager.cancel(curID);       
	            mBuilder.setContentText("Download Failed")
	                    .setProgress(0,0,false)
	                    .setAutoCancel(true)
	            		.setContentIntent(pIntent);
	                
	            mNotifyManager.notify(curID, mBuilder.build());
	            tFinished++;
	            sendCurCounter();
        	}
        } catch (Exception e) {
        	tFinished++;
            sendCurCounter();
            
            Message msg = handler.obtainMessage();
            msg.arg1 = 1;
            handler.sendMessage(msg);
            
        	if (NotiSet)
        	{
	            mNotifyManager.cancel(curID);       
	            mBuilder.setContentText("Download Failed")
	                    .setProgress(0,0,false)
	                    .setAutoCancel(true)
	            		.setContentIntent(pIntent);
	                
	            mNotifyManager.notify(curID, mBuilder.build());
        	}
        	
        }
	}
	
	private final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
              if(msg.arg1 == 1)
                    Toast.makeText(getApplicationContext(),"Unable to connect to download host, Please check your connection settings.", Toast.LENGTH_LONG).show();
              else if (msg.arg1 == 2)
            	  Toast.makeText(getApplicationContext(), "Failed downloading file", Toast.LENGTH_SHORT).show();
              else if (msg.arg1 == 3)
            	  Toast.makeText(getApplicationContext(), "Sorry right now we accept only Text & Image files.", Toast.LENGTH_SHORT).show();
        }
    };
	
	private BroadcastReceiver Receiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context c, Intent i) {
			String action = i.getAction();

			if ("broadcast.getcount".equals(action))
			{
				sendCurCounter();
			}
			else if ("broadcast.download".equals(action))
			{
				final String url = i.getStringExtra("url");
				final int dID = Integer.parseInt(i.getStringExtra("id"));
				Runnable runnable = new Runnable() {
		            @Override
		            public void run() {
		            	downloadFile(url, dID);
		            }
				};
				Thread t = new Thread(runnable);
				t.start();
			}

		}
	};

	private void updateProgress(String url, int id, int p, int bw, int tb, long cb, String content)
	{
		Intent intent = new Intent();
		intent.setAction("broadcast.progress");
		intent.putExtra("id", Integer.toString(id));
		intent.putExtra("progress", Integer.toString(p));
		intent.putExtra("bw", Integer.toString(bw));
		intent.putExtra("cb", Long.toString(cb));
		intent.putExtra("tb", Integer.toString(tb));
		intent.putExtra("url", url);
		intent.putExtra("content", content);
		sendBroadcast(intent);
	}

}
