package networkMessages;

import java.io.Serializable;

public class Response implements Serializable{
	private String latLon = new String();
	private byte[] photo;
	private long photoName;
	private String title = new String();
	
	public Response(String latLon, byte[] photo, long photoName, String title){
		this.latLon = latLon;
		this.photo = photo;
		this.photoName = photoName;
		this.title = title;
	}

	public String getLatLon() {
		return latLon;
	}

	public byte[] getPhoto() {
		return photo;
	}

	public long getPhotoName() {
		return photoName;
	}

	public String getTitle() {
		return title;
	}
	
	
	
}
