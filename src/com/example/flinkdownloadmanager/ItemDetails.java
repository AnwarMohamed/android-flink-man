package com.example.flinkdownloadmanager;

import java.net.MalformedURLException;
import java.net.URL;

public class ItemDetails {

	public String getFilename() {
		return filename;
	}

	public int getProgress() {
		return progress;
	}
	public void setProgress(int progress) {
		this.progress = progress;
	}
	
	public int getDownloaded() {
		return downloadedBytes;
	}
	public void setDownloaded(int downloadedBytes) {
		this.downloadedBytes = downloadedBytes;
	}
	
	public int getImageNumber() {
		return imageNumber;
		//return 1;
	}
	public void setImageNumber(int imageNumber) {
		this.imageNumber = imageNumber;
	}
	
	public String getdataTime() {
		return dataTime;
	}
	public void setdataTime(String dataTime) {
		this.dataTime = dataTime;
	}
	
	public long getSize() {
		return size;
	}
	public void setSize(int size) {
		this.size = size;
	}
	
	public int getID() {
		return id;
	}
	public void setID(int id) {
		this.id = id;
	}
	
	public String getHost() {
		return host;
	}
	
	public String getUrl() {
		return url;
	}
	
	public String getContent() {
		return content;
	}
	
	public void setContent(String content)
	{
		this.content = content;
	}
	
	public boolean setUrl(String url) {
		
		try {
			URL _url = new URL(url);
			this.url = url;
			this.host = _url.getHost();
			this.filename = _url.getFile().split("/")[_url.getFile().split("/").length-1];
			
			return true;
		} catch (MalformedURLException e) {
			return false;
		}

	}
	
	private String filename ;
	private int progress;
	private int downloadedBytes;
	private int imageNumber;
	private String dataTime;
	private int size;
	private String url;
	private String host;
	private int id;
	private String content;
}
