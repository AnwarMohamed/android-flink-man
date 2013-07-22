package com.example.flinkdownloadmanager;

import java.util.ArrayList;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ItemListBaseAdapter extends BaseAdapter {
	private static ArrayList<ItemDetails> itemDetailsrrayList;
	
	private Integer[] imgid = {
			R.drawable.image,
			R.drawable.text_document
			};
	
	private LayoutInflater l_Inflater;

	public ItemListBaseAdapter(Context context, ArrayList<ItemDetails> results) {
		itemDetailsrrayList = results;
		l_Inflater = LayoutInflater.from(context);
	}

	public int getCount() {
		return itemDetailsrrayList.size();
	}

	public Object getItem(int position) {
		return itemDetailsrrayList.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = l_Inflater.inflate(R.layout.layout_item, null);
			
			holder = new ViewHolder();
			holder.txt_itemFilename = (TextView) convertView.findViewById(R.id.name);
			holder.txt_itemDownloadDetails = (TextView) convertView.findViewById(R.id.host);
			holder.txt_itemHost = (TextView) convertView.findViewById(R.id.itemDescription);
			holder.itemImage = (ImageView) convertView.findViewById(R.id.photo);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		holder.txt_itemFilename.setText(itemDetailsrrayList.get(position).getFilename());
		holder.txt_itemHost.setText(itemDetailsrrayList.get(position).getHost());
		holder.itemImage.setImageResource(imgid[itemDetailsrrayList.get(position).getImageNumber() - 1]);
		
		int progress = itemDetailsrrayList.get(position).getProgress();
		if (progress < 100 && progress > 0)
			holder.txt_itemDownloadDetails.setText(itemDetailsrrayList.get(position).getdataTime() + 
					": In Progress  " + itemDetailsrrayList.get(position).getProgress() + "%  " + 
					(int)(itemDetailsrrayList.get(position).getSize()/1024) + "KB");
		else if (progress == 100)
			holder.txt_itemDownloadDetails.setText(itemDetailsrrayList.get(position).getdataTime() + 
					": Downloaded  " + (int)(itemDetailsrrayList.get(position).getSize()/1024) + "KB");	
		else
			holder.txt_itemDownloadDetails.setText(itemDetailsrrayList.get(position).getdataTime() + 
					": Not Downloading  " + (int)(itemDetailsrrayList.get(position).getSize()/1024) + "KB");

		return convertView;
	}

	static class ViewHolder {
		TextView txt_itemFilename;
		TextView txt_itemHost;
		//TextView txt_itemSize;
		TextView txt_itemDownloadDetails;
		//TextView txt_itemDataTime;
		ImageView itemImage;
	}
}
