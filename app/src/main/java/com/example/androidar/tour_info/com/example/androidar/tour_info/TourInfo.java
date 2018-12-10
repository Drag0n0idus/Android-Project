package com.example.androidar.tour_info;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import com.example.androidar.MapsActivity;
import com.example.androidar.QRActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.GeoApiContext;
import com.google.maps.android.PolyUtil;
import com.google.maps.model.DirectionsResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class TourInfo implements TaskCompleted, Parcelable {
    private MapsActivity mapsContext;
    private QRActivity qrContext;
    private final String apiKey = "AIzaSyCF241i93KMq6y-tHtrQoVhGtGweauHSk4";
    private final String apiServer = "http://www.garttox.jedovarnik.cz/api/";
    private String origin = "";
    private String destination = "";
    private String[] waypoints = null;
    private String author = "";
    public String name = "";
    private String id;
    private Point[] points;

    public TourInfo(QRActivity qrContext, String id) {
        this.qrContext=qrContext;
        this.id=id;
        new fetchData(TourInfo.this).execute(apiServer+"tour?id="+id,"tour");
    }

    protected TourInfo(Parcel in) {
        origin = in.readString();
        destination = in.readString();
        waypoints = in.createStringArray();
        author = in.readString();
        name = in.readString();
        id = in.readString();
    }

    public static final Creator<TourInfo> CREATOR = new Creator<TourInfo>() {
        @Override
        public TourInfo createFromParcel(Parcel in) {
            return new TourInfo(in);
        }

        @Override
        public TourInfo[] newArray(int size) {
            return new TourInfo[size];
        }
    };

    public void setMapsContext(MapsActivity mapsContext){
        this.mapsContext=mapsContext;
    }

    public GeoApiContext getGeoContext() {
        GeoApiContext geoApiContext = new GeoApiContext();
        return geoApiContext.setQueryRateLimit(3).setApiKey(apiKey)
                .setConnectTimeout(1, TimeUnit.SECONDS).setReadTimeout(1, TimeUnit.SECONDS)
                .setWriteTimeout(1, TimeUnit.SECONDS);
    }

    public void addMarkersToMap(DirectionsResult results, GoogleMap mMap) {
        mMap.addMarker(new MarkerOptions().position(new LatLng(results.routes[0].legs[0].startLocation.lat,
                results.routes[0].legs[0].startLocation.lng)).title(results.routes[0].legs[0].startAddress));
        for(int i = 0; i < (points.length - 1); i++) {
            mMap.addMarker(new MarkerOptions().position(new LatLng(results.routes[0].legs[i].endLocation.lat,
                    results.routes[0].legs[i].endLocation.lng)).title(results.routes[0].legs[i].endAddress));
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(results.routes[0].legs[0].startLocation.lat,
                results.routes[0].legs[0].startLocation.lng)));
        mMap.setMinZoomPreference(15.0f);
        mMap.setMaxZoomPreference(18.0f);
    }

    public void addPolyline(DirectionsResult results, GoogleMap mMap) {
        List<LatLng> decodedPath = PolyUtil.decode(results.routes[0].overviewPolyline.getEncodedPath());
        mMap.addPolyline(new PolylineOptions().addAll(decodedPath));
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String[] getWaypoints() {
        return waypoints;
    }

    public void setWaypoints() {
        this.waypoints = new String[this.points.length-2];
        for(int i = 2; i < this.points.length; i++){
            this.waypoints[i-2] = this.points[i].getLongitude() + ", " + this.points[i].getLatitude();
        }
    }

    @Override
    public void onTaskComplete(String result, String identifier){
        switch(identifier){
            case "tour":
                try {
                    JSONObject JObject=new JSONObject(result);
                    this.name=(String)JObject.get("name");
                    this.author=(String)JObject.get("author");
                    this.mapsContext.TourName(this.name);
                }catch (JSONException e) {
                    e.printStackTrace();
                }
                new fetchData(TourInfo.this).execute(apiServer+"points?id="+this.id,"points");
                break;

            case "points":
                try{
                    JSONArray JArray = new JSONArray(result);
                    this.points=new Point[JArray.length()];
                    for(int i = 0;i <JArray.length();i++){
                        JSONObject JO = (JSONObject) JArray.get(i);
                        this.points[i]=new Point((String)JO.get("longitude"),(String)JO.get("latitude"),(String)JO.get("name"),(int)JO.get("order"));
                    }
                    if(points.length > 2) {
                        this.setWaypoints();
                    }
                    this.setOrigin(points[0].getLongitude() + ", " + points[0].getLatitude());
                    this.setDestination(points[1].getLongitude() + ", " + points[1].getLatitude());
                }catch (JSONException e) {
                    e.printStackTrace();
                }
                //this.mapsContext.startMap();
                //this.qrContext.openMaps();
                break;
            case "pointDetail":
                try{
                    JSONObject JObject=new JSONObject(result);
                    int order=Integer.parseInt((String)JObject.get("order"));
                    this.points[order-1].setText((String)JObject.get("text"));

                }catch (JSONException e) {
                    e.printStackTrace();
                }

                break;
        }
    /*            JSONArray JA = new JSONArray(data);
            for(int i = 0;i <JA.length();i++){
                JSONObject JO = (JSONObject) JA.get(i);
                singleParsed =  "Name:" + JO.get("name") + "\n"+
                        "Password:" + JO.get("password") + "\n"+
                        "Contact:" + JO.get("contact") + "\n"+
                        "Country:" + JO.get("country") + "\n";

                dataParsed = dataParsed + singleParsed + "\n";
            }*/

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(apiKey);
        dest.writeString(apiServer);
        dest.writeString(origin);
        dest.writeString(destination);
        dest.writeStringArray(waypoints);
        dest.writeString(author);
        dest.writeString(name);
        dest.writeString(id);
    }
}
