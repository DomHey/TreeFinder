package com.domhey.treefinder;

public class TreeMarker {
	private String imageUrl = new String();
	private String lat = new String();
	private String lon = new String();
	private String title = new String();
	private String id = new String();
	
	public TreeMarker(String id,String url, String lat, String lon, String t){
		this.imageUrl = url;
		this.lat = lat;
		this.lon = lon;
		this.title = t;
		this.id = id;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public String getLat() {
		return lat;
	}

	public String getLon() {
		return lon;
	}

	public String getTitle() {
		return title;
	}
	
	public String getId(){
		return this.id;
	}
}
