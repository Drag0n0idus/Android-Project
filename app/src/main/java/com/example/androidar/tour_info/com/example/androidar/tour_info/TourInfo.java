package com.example.androidar.tour_info;

import com.example.androidar.MapsActivity;
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

public class TourInfo implements TaskCompleted{
    private MapsActivity mapsContext;
    private final String apiKey = "AIzaSyCF241i93KMq6y-tHtrQoVhGtGweauHSk4";
    private String origin = "";
    private String destination = "";
    private String[] waypoints = null;
    private String author = "";
    public String name = "";
    //From database later, now for testing
    int count = 5;
    private String[] test = {"49.938887, 17.902368", "49.936215, 17.901329", "49.938963, 17.901628", "49.938773, 17.900169", "49.940900, 17.893140"};
    private String originDb = test[0];
    private String destinationDb = test[1];
    //

    private Point[] points;

    public TourInfo(MapsActivity mapsContext) {
        new fetchData(TourInfo.this).execute();
        this.mapsContext=mapsContext;
        this.setWaypoints(count);
        this.setOrigin(originDb);
        this.setDestination(destinationDb);
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
        for(int i = 0; i < (count - 1); i++) {
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

    public void setWaypoints(int count) {
        this.waypoints = new String[count-2];
        for(int i = 2; i < count; i++){
            this.waypoints[i-2] = test[i];
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
                break;

            case "points":
                try{
                    JSONArray JArray = new JSONArray(result);
                    this.points=new Point[JArray.length()];
                    for(int i = 0;i <JArray.length();i++){
                        JSONObject JO = (JSONObject) JArray.get(i);
                        this.points[i]=new Point((String)JO.get("coorinateE"),(String)JO.get("coorinateN"),(String)JO.get("name"),(int)JO.get("order"));
                    }
                }catch (JSONException e) {
                    e.printStackTrace();
                }
                break;

            default:

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
}
