package com.example.androidar;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;
import com.example.androidar.tour_info.TaskCompleted;
import com.example.androidar.tour_info.fetchData;
import com.google.zxing.Result;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.regex.Pattern;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

import static android.Manifest.permission.CAMERA;
// https://www.youtube.com/watch?v=otkz5Cwdw38&t=924s
public class QRActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler, TaskCompleted {

    private final String apiServer = "http://www.garttox.jedovarnik.cz/api/";
    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_LOCATION = 2;
    private ZXingScannerView scannerView;
    private static int camId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private String myResult;
    private String tourName;
    private String result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);
        this.result="null";
        scannerView = new ZXingScannerView(this);
        setContentView(scannerView);
        int currentApiVersion = Build.VERSION.SDK_INT;

        if(currentApiVersion >=  Build.VERSION_CODES.M)
        {
            if(checkPermission())
            {
                Toast.makeText(getApplicationContext(), "Permission already granted!", Toast.LENGTH_LONG).show();
            }
            else
            {
                requestPermission();
            }
        }
    }

    private boolean checkPermission()
    {
        return (ContextCompat.checkSelfPermission(getApplicationContext(), CAMERA) == PackageManager.PERMISSION_GRANTED);
    }

    private void requestPermission()
    {
        ActivityCompat.requestPermissions(this, new String[]{CAMERA}, REQUEST_CAMERA);
    }

    @Override
    public void onResume() {
        super.onResume();

        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= android.os.Build.VERSION_CODES.M) {
            if (checkPermission()) {
                if(scannerView == null) {
                    scannerView = new ZXingScannerView(this);
                    setContentView(scannerView);
                }
                scannerView.setResultHandler(this);
                scannerView.startCamera();
            } else {
                requestPermission();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        scannerView.stopCamera();
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA:
                if (grantResults.length > 0) {

                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted){
                        Toast.makeText(getApplicationContext(), "Permission Granted, Now you can access camera", Toast.LENGTH_LONG).show();
                    }else {
                        Toast.makeText(getApplicationContext(), "Permission Denied, You cannot access and camera", Toast.LENGTH_LONG).show();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                                showMessageOKCancel("You need to allow access to both the permissions",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                                            REQUEST_LOCATION);
                                                }
                                            }
                                        });
                                return;
                            }
                        }
                    }
                }
                break;

            case REQUEST_LOCATION:
                if (grantResults.length > 0) {

                    boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (locationAccepted){
                        Toast.makeText(getApplicationContext(), "Permission Granted, Now you can access maps", Toast.LENGTH_LONG).show();
                        openMaps();
                    }else {
                        Toast.makeText(getApplicationContext(), "Permission Denied, You cannot access maps", Toast.LENGTH_LONG).show();
                        scannerView.resumeCameraPreview(QRActivity.this);
                        }
                    }
                break;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new android.support.v7.app.AlertDialog.Builder(QRActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    /**
     * @param result je výsledek skenování QR kódu
     */
    @Override
    public void handleResult(Result result) {
        myResult = result.getText(); //do proměnné

        if(myResult.contains("|")){  // kontrola zda myResult obsahuje "|"
            final String splited[] = myResult.split(Pattern.quote("|")); // Rozdělení Stringu podle |
            if(splited[0].equals("OpavaTour") && splited[1].matches("[0-9]+")){ // kontrola zda String splňuje pattern OpavaTour|ID
                this.myResult=splited[1];
                new fetchData(QRActivity.this).execute(apiServer+"exist?id="+this.myResult,"exist"); // dotaz na api server, vysledkem je zda ID je platné
            }
            else{
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setPositiveButton("Zpět", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        scannerView.resumeCameraPreview(QRActivity.this);
                    }
                });
                builder.setMessage("QR kód má špatný formát");
                AlertDialog alert1 = builder.create();
                alert1.show();
            }
        }
        else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setPositiveButton("Zpět", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    scannerView.resumeCameraPreview(QRActivity.this);
                }
            });
            builder.setMessage("QR kód neobsahuje stezku");
            AlertDialog alert1 = builder.create();
            alert1.show();
        }

    }

    public void verified(String name, final String myResult){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setPositiveButton("Zpět", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                scannerView.resumeCameraPreview(QRActivity.this);
            }
        });
        builder.setNeutralButton("Ano", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new fetchData(QRActivity.this).execute(apiServer+"points?id="+myResult,"OpenMaps");
            }
        });
        builder.setMessage("Přejete si spustit stezku: "+ name);
        AlertDialog alert1 = builder.create();
        alert1.show();
    }

    // metoda pro spouštění MapsActivity po ověření povolení
    public void openMaps() {
        // kontrola zda
        if(!this.result.equals("null")) {
            // kontrola zda uživatel dal povolení k použití GPS
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // spouštění MapsActivity
                Intent intent = new Intent(this, MapsActivity.class);
                // předávání dat do MapsActivity
                intent.putExtra("QRresult", myResult);
                intent.putExtra("pointsFetch", this.result);
                intent.putExtra("tourName", this.tourName);
                startActivity(intent);
            } else {
                // vyžádání povolení k použití GPS
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            }
        }
    }

    @Override
    public void onTaskComplete(String result, String identifier){
        switch(identifier) {
            case "exist":
                JSONObject JObject = null;
                try {
                    JObject = new JSONObject(result);
                    String status = (String) JObject.get("status");
                    if (status.equals("avaible")) {
                        this.tourName=(String) JObject.get("tour");
                        this.verified((String) JObject.get("tour"), myResult);
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);

                        builder.setPositiveButton("Zpět", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                scannerView.resumeCameraPreview(QRActivity.this);
                            }
                        });
                        builder.setMessage("Tahle stezka buď již nefunguje nebo neexistuje");
                        AlertDialog alert1 = builder.create();
                        alert1.show();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case "OpenMaps":
                this.result=result;
                openMaps();
                break;

        }
    }
}
