package com.domhey.treefinder;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import networkMessages.Request;
import networkMessages.Response;
import networkMessages.ServerResponse;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class TreeFinderStartup extends Activity implements OnMapReadyCallback {
	private ImageView popupImage;
	private GoogleMap map;
	private Button addTreeButton;
	private ArrayList<TreeMarker> markerList = new ArrayList<TreeMarker>();
	private Hashtable <String,TreeMarker> markerMapper = new Hashtable<>();
	private File mFile;
	private String MODE = "SELECT";
	private long lastUpdated = 0;
	private String modifiedKey = "com.domhey.treefinder.modifiedKey";
	private SharedPreferences prefs;
	private String ServerIp = "domhey.no-ip.biz";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tree_finder_startup);
		MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
		prefs  = this.getSharedPreferences(
				modifiedKey, Context.MODE_PRIVATE);
		lastUpdated = prefs.getLong(modifiedKey, 0); 
		addTreeButton = (Button) findViewById(R.id.treeButton);
		addTreeButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				createPopup();
				
			}
		});
		
        mapFragment.getMapAsync(this);
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tree_finder_startup, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		case R.id.removeTree:
    			 MODE = "REMOVE";
    			 break;
    		case R.id.syncTree:
    			syncTrees();
    			break;
    		case R.id.uploadTree:
    			MODE = "UPLOAD";
    			break;
    	}
    	return true;
    }

	@Override
	public void onMapReady(GoogleMap googleMap) {
		map = googleMap;
		map.setInfoWindowAdapter(new CustomInfoWindowAdapter());
		map.setOnMarkerClickListener(new OnMarkerClickListener() {
			
			@Override
			public boolean onMarkerClick(Marker marker) {
				if(MODE.equals("SELECT")){
					marker.showInfoWindow();					
				}else if(MODE.equals("REMOVE")){
					removeTree(marker);
				}else if(MODE.equals("UPLOAD")){
					uploadTree(marker);
				}
				MODE = "SELECT";
				return true;
			}
		});
		googleMap.setMyLocationEnabled(true);
		loadMarkerPositions(map);
		//displaySavedMarkers(map);

	}
	
	@Override
	public void onPause(){
		super.onPause();
		saveMarkerPositions();
	}
	
	@Override
	public void onResume(){
		super.onResume();
	}
	
	private void createPopup(){
		final Dialog dialog = new Dialog(this);
		dialog.setContentView(R.layout.popup);
		dialog.setTitle("Neuen Baum speichern");
		
		final EditText title = (EditText) dialog.findViewById(R.id.treeTitle);
		popupImage = (ImageView) dialog.findViewById(R.id.displayImage);
		ImageView camera = (ImageView) dialog.findViewById(R.id.chooseImage);
		Button save = (Button) dialog.findViewById(R.id.saveButton);
		save.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				try{
					addTreeMarker(((BitmapDrawable)popupImage.getDrawable()).getBitmap(), title.getText().toString());
				}catch (Exception e){
					addTreeMarker(null, title.getText().toString());
				}
				dialog.dismiss();
			}
		});
		camera.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mFile = null;
				File dir = new File(Environment.getExternalStorageDirectory() + "/treeFinder");
				try {
					mFile = File.createTempFile("tempImage", ".png", dir);
					Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
					intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mFile));
					startActivityForResult(intent, 0);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});


		dialog.show();
	}
	
	private void addTreeMarker(Bitmap photo, String title){
		Marker marker = map.addMarker(new MarkerOptions()
		.position(new LatLng(map.getMyLocation().getLatitude(), map.getMyLocation().getLongitude()))
		.title(title)
		.icon(BitmapDescriptorFactory.fromResource(R.drawable.treemarker)));
		long unixTime = System.currentTimeMillis() / 1000L;
		if(photo != null){
			saveImage(photo, unixTime);			
		}
		TreeMarker m = new TreeMarker(marker.getId(),String.valueOf(unixTime), String.valueOf(map.getMyLocation().getLatitude()), String.valueOf(map.getMyLocation().getLongitude()), title);
		markerMapper.put(marker.getId(),m);
        
	}
	
	private void addTreeMarker(final Bitmap photo, final String title, final String latLon, final long time){
		
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				Double lat = Double.valueOf(latLon.split(",")[0]);
				Double lon = Double.valueOf(latLon.split(",")[1]);
				Marker marker = map.addMarker(new MarkerOptions()
				.position(new LatLng(lat, lon))
				.title(title)
				.icon(BitmapDescriptorFactory.fromResource(R.drawable.treemarker)));
				if(photo != null){
					saveImage(photo, time);			
				}
				TreeMarker m = new TreeMarker(marker.getId(),String.valueOf(time), latLon.split(",")[0], latLon.split(",")[1], title);
				markerMapper.put(marker.getId(), m);
			}
		});
		
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	  if (resultCode == Activity.RESULT_OK && requestCode == 0) {
		  Bitmap photo = BitmapFactory.decodeFile(mFile.getAbsolutePath()); 
		  photo = resizeImageForImageView(photo);
		  mFile.delete();

		  LayoutParams params = new LayoutParams(photo.getWidth(),photo.getHeight());
		  params.setMargins(10, 30, 10, 0);
		  params.gravity = Gravity.CENTER;
          popupImage.setLayoutParams(params);
          popupImage.setScaleType(ScaleType.FIT_XY);
          popupImage.setImageBitmap(photo);

          
	  }
	}
	
	
	
	 private class CustomInfoWindowAdapter implements InfoWindowAdapter {
		 
	        private View view;
	        
	        public CustomInfoWindowAdapter() {
	            view = getLayoutInflater().inflate(R.layout.custom_windowinfo,
	                    null);
	        }

			@Override
			public View getInfoContents(Marker arg0) {
				return null;
			}

			@Override
			public View getInfoWindow(Marker marker) {
				TextView title = (TextView) view.findViewById(R.id.infoTitle);
				title.setText(marker.getTitle());
				ImageView image = (ImageView) view.findViewById(R.id.infoImage);
				String photoPath = Environment.getExternalStorageDirectory()+"/TreeFinder/Image-"+(markerMapper.get(marker.getId())).getImageUrl()+".jpg";
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inPreferredConfig = Bitmap.Config.ARGB_8888;
				Bitmap bitmap= null;
				try{
					 bitmap = BitmapFactory.decodeFile(photoPath, options);
					}catch (Exception e) {}
				if(bitmap != null){
				 
				  LayoutParams params = new LayoutParams(bitmap.getWidth(),bitmap.getHeight());
				  params.setMargins(20, 20, 20, 20);
				  params.gravity = Gravity.CENTER;
				  image.setLayoutParams(params);
				  image.setScaleType(ScaleType.FIT_XY);
				  image.setImageBitmap(bitmap);
				}else{
					image.setImageBitmap(null);
					LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
					image.setLayoutParams(params);
				}
				return view;
			}
	        
	 }
	

	private void saveImage(Bitmap finalBitmap, long ident) {

	    String root = Environment.getExternalStorageDirectory().toString();
	    File myDir = new File(root + "/TreeFinder");    
	    myDir.mkdirs();
	    String fname = "Image-"+ ident +".jpg";
	    File file = new File (myDir, fname);
	    if (file.exists ()) file.delete (); 
	    try {
	           FileOutputStream out = new FileOutputStream(file);
	           finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
	           out.flush();
	           out.close();

	    } catch (Exception e) {
	           e.printStackTrace();
	    }
	}
	
	
	private void saveMarkerPositions(){
		String root = Environment.getExternalStorageDirectory().toString();
		File myDir = new File(root + "/TreeFinder");
		myDir.mkdirs();
		File file = new File (myDir, "locations.txt");
		if(markerMapper.size() > 0){	    
	    try {
	    	PrintWriter ppw = new PrintWriter(file);
	    	ppw.close();
            FileOutputStream f = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(f);
            
            Iterator it = markerMapper.entrySet().iterator();
            while (it.hasNext()) {
            	Map.Entry pair = (Map.Entry)it.next();
                System.out.println(pair.getKey() + " = " + pair.getValue());
                TreeMarker marker = (TreeMarker) pair.getValue();
                pw.println(marker.getTitle() + "," + marker.getLat()+ "," + marker.getLon()+ "," + marker.getImageUrl());
            }
          	
                pw.flush();
                pw.close();
                f.close();
            
	    }catch (Exception e){
	    	System.out.println(e);
	    }
		}else{
	    	PrintWriter ppw;
			try {
				ppw = new PrintWriter(file);
				ppw.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void loadMarkerPositions(GoogleMap map){
		String root = Environment.getExternalStorageDirectory().toString();
	    File myDir = new File(root + "/TreeFinder");
	    myDir.mkdirs();
	    File file = new File (myDir, "locations.txt");
	    try {
	    	InputStream instream = new FileInputStream(file);
	    	if (instream != null) {
	    	  InputStreamReader inputreader = new InputStreamReader(instream);
	    	  BufferedReader buffreader = new BufferedReader(inputreader);

	    	  String line;
	    	  do {
	    	     line = buffreader.readLine();
	    	     String[] splitArray = line.split(",");
	    	     Marker gmarker = map.addMarker(new MarkerOptions()
	 			.position(new LatLng(Double.valueOf(splitArray[1]), Double.valueOf(splitArray[2])))
	 			.title(splitArray[0])
	 			.icon(BitmapDescriptorFactory.fromResource(R.drawable.treemarker)));
	 			TreeMarker m = new TreeMarker(gmarker.getId(),splitArray[3], splitArray[1], splitArray[2], splitArray[0]);
	 			markerMapper.put(gmarker.getId(), m);
	    	     
	    	  } while (line != null);

	    	}
	    	} catch (Exception ex) {
	    		
	    	}
	    }

	
/*	private void displaySavedMarkers(GoogleMap map){
		for (int i = 0; i < markerList.size(); i++) {
			TreeMarker marker = markerList.get(i);
			Marker gmarker = map.addMarker(new MarkerOptions()
			.position(new LatLng(Double.valueOf(marker.getLat()), Double.valueOf(marker.getLon())))
			.title(marker.getTitle())
			.icon(BitmapDescriptorFactory.fromResource(R.drawable.treemarker)));
			TreeMarker m = new TreeMarker(gmarker.getId(),marker.getImageUrl(), marker.getLat(), marker.getLon(), marker.getTitle());
			markerMapper.put(gmarker.getId(), m);
		}
	}*/
	
	public Bitmap resizeImageForImageView(Bitmap bitmap) {
	    Bitmap resizedBitmap = null;
	    int originalWidth = bitmap.getWidth();
	    int originalHeight = bitmap.getHeight();
	    int newWidth = -1;
	    int newHeight = -1;
	    float multFactor = -1.0F;
	    if(originalHeight > originalWidth) {
	        newHeight = 400;
	        multFactor = (float) originalWidth/(float) originalHeight;
	        newWidth = (int) (newHeight*multFactor);
	    } else if(originalWidth > originalHeight) {
	        newWidth = 300;
	        multFactor = (float) originalHeight/ (float)originalWidth;
	        newHeight = (int) (newWidth*multFactor);
	    } else if(originalHeight == originalWidth) {
	        newHeight = 300;
	        newWidth = 300;
	    }
	    resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false);
	    return resizedBitmap;
	}
	
	private void removeTree(Marker marker){
		marker.remove();
		markerMapper.remove(marker.getId());
	}
	
	private void syncTrees(){
	Thread t = new Thread(new Runnable() {
				
				@Override
				public void run() {
					try {
						Request req = new Request(lastUpdated);
						InetAddress serverAddr = InetAddress.getByName(ServerIp);
						Socket serverSocket = new Socket(serverAddr,4090);
						
						    OutputStream os = serverSocket.getOutputStream();
							ObjectOutputStream oos = new ObjectOutputStream(os);
							InputStream in = serverSocket.getInputStream();
							ObjectInputStream oin = new ObjectInputStream(in);
							oos.writeObject(req);
							oos.flush();
							try {
								Object o = oin.readObject();
								if(o instanceof ServerResponse){
									ArrayList<Response> a = ((ServerResponse)o).getList();
									for (int i = 0; i < a.size(); i++) {
										Response r = a.get(i);
										if(r.getPhoto() != null){
											Bitmap bitmap = BitmapFactory.decodeByteArray(r.getPhoto() , 0, r.getPhoto().length);
											addTreeMarker(bitmap, r.getTitle(), r.getLatLon(), r.getPhotoName());					
										}else{
											addTreeMarker(null, r.getTitle(), r.getLatLon(), r.getPhotoName());
										}
										
									}
									lastUpdated = System.currentTimeMillis() / 1000L;
									prefs.edit().putLong(modifiedKey, lastUpdated).apply();
								}
							} catch (ClassNotFoundException e) {
								// TODO Auto-generated catch block
								serverSocket.close();
								e.printStackTrace();
							}
							serverSocket.close();
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
			
			t.start();
			
			
	}
	
	
	private void uploadTree(final Marker marker){
		String title = marker.getTitle();
		String photoPath = Environment.getExternalStorageDirectory()+"/TreeFinder/Image-"+(markerMapper.get(marker.getId())).getImageUrl()+".jpg";
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		Bitmap bitmap= null;
		byte[] byteArray = null;
		try{
			 bitmap = BitmapFactory.decodeFile(photoPath, options);
			 ByteArrayOutputStream stream = new ByteArrayOutputStream();
			 bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
			 byteArray = stream.toByteArray();
		}catch (Exception e) {}
		
		LatLng pos =  marker.getPosition();
		String latLon = pos.latitude +"," + pos.longitude;
		
		
		final Response res = new Response(latLon, byteArray, Long.valueOf((markerMapper.get(marker.getId())).getImageUrl()), title);
		Thread t = new Thread(new Runnable() {		
			@Override
			public void run() {
				try {
					InetAddress serverAddr = InetAddress.getByName(ServerIp);
					Socket serverSocket = new Socket(serverAddr,4090);
					
					    OutputStream os = serverSocket.getOutputStream();
						ObjectOutputStream oos = new ObjectOutputStream(os);
						oos.writeObject(res);
						oos.flush();
						//oos.close();
						//os.close();
						//serverSocket.close();
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		
		t.start();
		
		
	}

}
