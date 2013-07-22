package com.example.flinkdownloadmanager;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import com.example.flinkdownloadmanager.DownloadService.LocalBinder;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class MainActivity extends Activity {

	private NotificationManager mNotificationManager;
	private static int MAIN_NOTIFICATION_ID = 999999;
	private int nDownloadIDs = 1000, nIDList = 0;
	private static int DOWNLOAD_ID_INDEX = 1000;
	
	Notification noti;
	
	private PendingIntent pIntent;
	boolean mIsBound = false;
	
	private ListView lv;
	private ItemListBaseAdapter DownloadAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		lv = (ListView) findViewById(R.id.dList);
		
		if (android.os.Build.VERSION.SDK_INT > 9) {
		    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		    StrictMode.setThreadPolicy(policy);
		}
		
		Intent mainIntent = new Intent(this, MainActivity.class);
		pIntent = PendingIntent.getActivity(this, 0, mainIntent, 0);

		noti = new Notification.Builder(getApplicationContext())
		        .setContentTitle("Flink Man")
		        .setContentText("No Active Downloads")
		        .setSmallIcon(R.drawable.ic_launcher)
		        .setContentIntent(pIntent)
	        	.build();
		    
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		noti.flags |=  Notification.FLAG_ONGOING_EVENT;
		mNotificationManager.notify(MAIN_NOTIFICATION_ID, noti); 
		
        Intent bind_intent = new Intent(this, DownloadService.class);
        //startService(bind_intent);
        bindService(bind_intent, myConnection, Context.BIND_AUTO_CREATE);
        
		DownloadAdapter = new ItemListBaseAdapter(getApplicationContext(), DownloadItems);
		lv.setAdapter(DownloadAdapter);
        
        registerForContextMenu(lv);
        lv.setOnItemClickListener(new OnItemClickListener() {
        	@Override
        	public void onItemClick(AdapterView<?> a, View v, int position, long id) { 
        		
        		Object o = lv.getItemAtPosition(position);
            	ItemDetails obj_itemDetails = (ItemDetails)o;
            	
            	if (obj_itemDetails.getProgress() == 100)
            	{
	            	Intent iintent = null;
	                if (obj_itemDetails.getContent().startsWith("image"))
	                {
	                	iintent = new Intent(Intent.ACTION_VIEW); 
	                	iintent.setDataAndType(Uri.parse("file:///sdcard/Download/" + obj_itemDetails.getFilename()), "image/*");
	                }
	                else if (obj_itemDetails.getContent().startsWith("text"))
	                {
	                	iintent = new Intent(Intent.ACTION_EDIT); 
	                	iintent.setDataAndType(Uri.parse("file:///sdcard/Download/" + obj_itemDetails.getFilename()), "text/*");
	                }
	                
	                startActivity(iintent);
            	}
        	}  
        });
        
        lv.setLongClickable(true);
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            public boolean onItemLongClick(AdapterView<?> arg0, View view,
                    int pos, long id) {
            	
            	//startActionMode(modeCallBack);
            	view.setSelected(true);
                return false;
            }
        });
        
        
	}
	
	@Override 
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		ItemDetails obj = (ItemDetails) lv.getItemAtPosition(info.position);
        
		menu.setHeaderTitle("Select The Action");
		
		if (obj.getProgress() == 0)
		{
			menu.add(0, v.getId(), 0, "Restart Download");
		}
		else if (obj.getProgress() > 0 && obj.getProgress() < 100)
		{
			menu.add(0, v.getId(), 0, "Stop Download");
		}
		else if (obj.getProgress() == 100)
		{
			menu.add(0, v.getId(), 0, "Open in Viewer");
		}
		
      
        menu.add(0, v.getId(), 0, "File Details");
        menu.add(0, v.getId(), 0, "Clear from List"); 

    } 
		
	@Override
	public boolean onContextItemSelected(MenuItem item) {
	    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	    ItemDetails obj = (ItemDetails) lv.getItemAtPosition(info.position);
	 
	    if (item.getTitle().toString().equals("Open in Viewer"))
	    {
        	Intent iintent = null;
            if (obj.getContent().startsWith("image"))
            {
            	iintent = new Intent(Intent.ACTION_VIEW); 
            	iintent.setDataAndType(Uri.parse("file:///sdcard/Download/" + obj.getFilename()), "image/*");
            }
            else if (obj.getContent().startsWith("text"))
            {
            	iintent = new Intent(Intent.ACTION_EDIT); 
            	iintent.setDataAndType(Uri.parse("file:///sdcard/Download/" + obj.getFilename()), "text/*");
            }
            startActivity(iintent);
            return true;
	    }
	    else if (item.getTitle().toString().equals("Clear from List"))
	    {
	    	DownloadItems.remove(info.position);
	    	if (DownloadAdapter != null)
	    		DownloadAdapter.notifyDataSetChanged();
		    //lv.scrollTo(0, 0);
		    nIDList--;
		    
			if (nIDList == 0)
			{
				TextView t = (TextView)findViewById(R.id.dListHeader);
				t.setVisibility(View.INVISIBLE);
				
				t = (TextView)findViewById(R.id.textView2);
				t.setVisibility(View.VISIBLE);
				
				ListView l = (ListView)findViewById(R.id.dList);
				l.setVisibility(View.INVISIBLE);
			}
		    
		    return true;
	    }
	    else if (item.getTitle().toString().equals("File Details"))
	    {
	    	String msgbox_string = 	"Filename: " + obj.getFilename() + "\n" +
	    							"Size: " + (int)(obj.getSize()/1024) + " KB\n" +
	    							"Origin: " +  obj.getHost() + "\n";
	    	
		    AlertDialog dlg = new AlertDialog.Builder(this).create();
	        dlg.setTitle("File Details");
	        dlg.setMessage(msgbox_string);
	        dlg.setCancelable(true);
	        dlg.setButton(DialogInterface.BUTTON_POSITIVE, "Ok", (DialogInterface.OnClickListener) null);
	        dlg.show();
	    }
	    return false;
	}	
	
	private ArrayList<ItemDetails> DownloadItems = new ArrayList<ItemDetails>();

	private int isItemInList(int id)
	{
		for (int i=0; i<DownloadItems.size(); i++)
			if (DownloadItems.get(i).getID() == id)
	            return i;
		return -1;
	}
		
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	protected void onDestroy() {
		
		unbindService(myConnection);
	    mNotificationManager.cancelAll();
	    super.onDestroy();
	}
	
	private void updateMainNotification(int i)
	{
		String content;
		
		if (i > 0)
			content = i + " Active Downloads";
		else
			content = "No Active Downloads";
		
		noti = new Notification.Builder(getApplicationContext())
        .setContentTitle("Flink Man")
        .setContentText(content)
        .setSmallIcon(R.drawable.ic_launcher)
        .setContentIntent(pIntent)
    	.build();
		noti.flags |=  Notification.FLAG_ONGOING_EVENT;
		mNotificationManager.notify(MAIN_NOTIFICATION_ID, noti);
	}
	
	@Override
	public void onBackPressed() {
		MainActivity.this.moveTaskToBack(true);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

	    switch (item.getItemId()) {
	    case R.id.action_exit:

	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    	builder
	    	.setTitle("Exit")
	    	.setMessage("Are you sure?")
	    	.setIcon(android.R.drawable.ic_menu_close_clear_cancel)
	    	.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	    	    public void onClick(DialogInterface dialog, int which) {			      	
	    	    	finish();
	    	    }
	    	})
	    	.setNegativeButton("No", null)
	    	.show();
	    	
	        return true;

	    case R.id.action_clear_history:
	    	ClearHistory();
	    	return true;
	    	
	    case R.id.action_about:
	    	String msgbox_string = 	"Flink Man\n" + 
	    							"Smart Download Manager\n" +
    								"Version 1.0\n\n" + 
	    							"Programmed by:\n" + 
    								"Anwar Mohamed\n" + 
	    							"anwarelmakrahy@gmail.com\n\n" + 
    								"Copyrights 2013 to Anwar Mohamed\n";
	    	
		    AlertDialog dlg = new AlertDialog.Builder(this).create();
	        dlg.setTitle("About Flink Man");
	        dlg.setMessage(msgbox_string);
	        dlg.setCancelable(true);
	        dlg.setButton(DialogInterface.BUTTON_POSITIVE, "Ok", (DialogInterface.OnClickListener) null);
	        dlg.show();
	        
	    	return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	
	public void DownloadItem(View v)
	{
		ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
	    NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
	    NetworkInfo mMobile = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

	    if (!mWifi.isConnected() && !mMobile.isConnected()) {
	    	Toast.makeText(getApplicationContext(), "No Network connection, Please turn on Wifi or 3G.", Toast.LENGTH_SHORT).show();
	    	return;
	    }
	    
		TextView t = (TextView)findViewById(R.id.txtUrl);	
		if (t.getText().length() < 1)
			Toast.makeText(getApplicationContext(), "Please enter valid URL", Toast.LENGTH_SHORT).show();
		else
		{
			if (!t.getText().toString().toLowerCase().startsWith("http"))
				t.setText("http://" + t.getText());
			
			Intent intent = new Intent();
			intent.setAction("broadcast.download");
			intent.putExtra("url", t.getText().toString());
			intent.putExtra("id", Integer.toString(nDownloadIDs++));
			sendBroadcast(intent);
		}
	}
	
	private ServiceConnection myConnection = new ServiceConnection() {

	    public void onServiceConnected(ComponentName className, IBinder service) {
	    	LocalBinder binder = (LocalBinder)service;
	    	binder.getService();
	        mIsBound = true;
	        triggerDownloader();
	    }
	    
	    public void onServiceDisconnected(ComponentName arg0) {
	        mIsBound = false;
	    }
	};

	
	private BroadcastReceiver Receiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context c, Intent i) {
			String action = i.getAction();
			if ("broadcast.count".equals(action))
			{
				String count = i.getStringExtra("count");	

				int number = 0;
				try
				{number = Integer.parseInt(count); }
				catch ( Exception e )
				{ number = 0; }
				
				updateMainNotification(number);
			}
			else if ("broadcast.progress".equals(action))
			{
				
				//Toast.makeText(getApplicationContext(), "Progress Broadcast Received", Toast.LENGTH_SHORT).show();
				
				int id = Integer.parseInt(i.getStringExtra("id"));
				int result = isItemInList(id);
				
				if (nIDList == 0)
				{
					TextView t = (TextView)findViewById(R.id.dListHeader);
					t.setVisibility(View.VISIBLE);
					
					t = (TextView)findViewById(R.id.textView2);
					t.setVisibility(View.INVISIBLE);
					
					ListView l = (ListView)findViewById(R.id.dList);
					l.setVisibility(View.VISIBLE);
				}
				
				if (result < 0)
				{
				   	ItemDetails item_details = new ItemDetails();
			    	
			    	DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
			    	Date date = new Date();
			    	
			    	item_details.setdataTime(dateFormat.format(date));
			    	item_details.setUrl(i.getStringExtra("url"));
			    	item_details.setDownloaded(Integer.parseInt(i.getStringExtra("cb")));
			    	item_details.setProgress(Integer.parseInt(i.getStringExtra("progress")));
			    	item_details.setSize(Integer.parseInt(i.getStringExtra("tb")));
			    	item_details.setID(Integer.parseInt(i.getStringExtra("id"))); 
			    	item_details.setContent(i.getStringExtra("content"));
			    	
			    	if (i.getStringExtra("content").startsWith("text"))
			    		item_details.setImageNumber(2);
			    	else
			    		item_details.setImageNumber(1);
			    	
			    	DownloadItems.add(0, item_details);
			    	nIDList++;
			    	
	
			    		if (DownloadAdapter != null)
			    			DownloadAdapter.notifyDataSetChanged();
			    	
			    	
				}
				else
				{
					DownloadItems.get(result).setDownloaded(Integer.parseInt(i.getStringExtra("cb")));
					DownloadItems.get(result).setProgress(Integer.parseInt(i.getStringExtra("progress")));
					if (DownloadAdapter != null)
						DownloadAdapter.notifyDataSetChanged();
					//lv.scrollTo(0, 0);
					//lv.setAdapter(new ItemListBaseAdapter(getApplicationContext(), DownloadItems));
				}
			}
			//else if (Intent.ACTION_VIEW.equals(action))
			//	Toast.makeText(getApplicationContext(), "Download Request", Toast.LENGTH_LONG).show();
		}
	};

	private IntentFilter filter = new IntentFilter();
	
	@Override
	protected void onNewIntent(Intent intent) {
	    super.onNewIntent(intent);
	    setIntent(intent);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		
		filter.addAction("broadcast.count");
		filter.addAction("broadcast.progress");
		this.registerReceiver(Receiver, filter);

		super.onResume();
		
		if (!triggerDownloader())
		{
			
			
			Intent intent = new Intent();
			intent.setAction("broadcast.getcount");
			sendBroadcast(intent);
			
	    	if (nDownloadIDs - DOWNLOAD_ID_INDEX > 1 && DownloadAdapter != null)
	    		DownloadAdapter.notifyDataSetChanged();
			
			String clipText = "";
			int sdk = android.os.Build.VERSION.SDK_INT;
			if(sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
				android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			    clipText = clipboard.getText().toString();
			} else {
			    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE); 
			    android.content.ClipData clip = clipboard.getPrimaryClip();
			
			    if (clip != null)
			    	if (clip.getItemCount() > 0)
			    		clipText = clip.getItemAt(0).coerceToText(this).toString();
			}
			
			try {
				  URL url = new URL(clipText);
				  TextView t = (TextView)findViewById(R.id.txtUrl);
				  t.setText(url.toExternalForm());
				  
				} catch (MalformedURLException e) {
				  // it wasn't a URL
				}
		}
	}
	
	@Override
	protected void onPause() {
		this.unregisterReceiver(Receiver);
		super.onPause();
	}
	
	public void ClearHistory()
	{
		DownloadItems.clear();
		if (DownloadAdapter != null)
			DownloadAdapter.notifyDataSetChanged();
		//lv.scrollTo(0, 0);
		
		nIDList = 0;

		TextView t = (TextView)findViewById(R.id.dListHeader);
		t.setVisibility(View.INVISIBLE);
		
		t = (TextView)findViewById(R.id.textView2);
		t.setVisibility(View.VISIBLE);
		
		ListView l = (ListView)findViewById(R.id.dList);
		l.setVisibility(View.INVISIBLE);

	}
	
	public boolean triggerDownloader()
	{
		Intent intent = getIntent();
	    if (intent != null)
    	{
	    	String action = intent.getAction();
		    if (action != null && action.equals(Intent.ACTION_VIEW)) {
		    	Uri data = intent.getData();
		    	TextView t = (TextView)findViewById(R.id.txtUrl);
		    	t.setText(data.toString());
		    	
		    	DownloadItem(findViewById(R.id.btnDownload));
		    	return true;
		    }
		    return false;
    	}
	    return false;
	    
	}
}
