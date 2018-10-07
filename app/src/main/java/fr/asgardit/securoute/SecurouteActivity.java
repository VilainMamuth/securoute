package fr.asgardit.securoute;

import java.text.DecimalFormat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
//import android.location.LocationProvider;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

/*
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
//import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
*/

public class SecurouteActivity extends AppCompatActivity implements LocationListener {

    /*TODO :
	 * ajouter paramétrages route sèche, mouillée
	 * ajouter l'équivalent poids de l'énergie cinétique du véhicule
	 * ajouter de temps de freinage et le temps d'arrêt 
	 * ajouter param sur état de vigilence du conducteur et effet sur le temps de réaction
	 * ajouter une activité pour tester le temps de réaction et rendre paramétrable le résultat
	 * ajouter une activité pour mesurer la décelération, possibilité d'enregistrer le résultat
	 * améliorer le calcul de l'acceleration, réduire le bruit
	 * 
	*/
    protected static final String TAG = "MainActivity";
    //protected GoogleApiClient mGoogleApiClient;
    protected Location mCurrentLocation;
    //protected LocationRequest mLocationRequest;
    private LocationManager mLocationManager;
    private boolean permsOk = false;
    private TextView mLon;
    private TextView mLat;
    private TextView mPrecision;
    private TextView mSpeedm;
    private TextView mSpeedk;
    private TextView mAccel;
    private TextView mMaxAccel;
    private TextView mDistSecu;
    private TextView mDistFrein;
    private TextView mDistArret;
    private TextView mDecel;
    private TextView mDecelg;
    private TextView mForce;
    private TextView mPoidsCol;

    private int prefReactionTime;
    private int prefMasseVehicule;
    private int prefMassePilote;

    //private LocationManager mManager;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    //private WindowManager mWindowManager;
    private Display mDisplay;
    //private PowerManager.WakeLock wl;
    private float[] lastAccel = new float[3];
    private float[] filteredAccel = new float[3];
    private float maxAccel = 0;

    private static final int ONE_SECOND = 1000;
    private static final int MY_PERMISSIONS_REQUEST_AFL = 1;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mLon = (TextView) this.findViewById(R.id.lon);
        mLat = (TextView) this.findViewById(R.id.lat);
        mPrecision = (TextView) this.findViewById(R.id.precision);
        mSpeedm = (TextView) this.findViewById(R.id.speedm);
        mSpeedk = (TextView) this.findViewById(R.id.speedk);
        mAccel = (TextView) this.findViewById(R.id.accel);
        //mMaxAccel = (TextView) this.findViewById(R.id.maxAccel);
        mDistSecu = (TextView) this.findViewById(R.id.distsec);
        mDistFrein = (TextView) this.findViewById(R.id.distfrein);
        mDistArret = (TextView) this.findViewById(R.id.distarret);

        mDecel = (TextView) this.findViewById(R.id.decel);
        mDecelg = (TextView) this.findViewById(R.id.decelg);
        mForce = (TextView) this.findViewById(R.id.force);
        mPoidsCol = (TextView) this.findViewById(R.id.poidsCol);

        // Pas d'API Google Play, la vitesse n'est pas accessible
        /* mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        */
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

/*
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
*/

        //

        //PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        //wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "GalileoActivity");

        // Get an instance of the SensorManager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);


        // Instancier le display qui connaît l'orientation de l'appareil
        mDisplay = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

        //LocationProvider provider = mManager.getProvider(LocationManager.GPS_PROVIDER);

        //boolean gpsEnabled = manager.isProviderEnabled(manager.GPS_PROVIDER);

        //Met en place les valeurs par défaut la toute première fois, si le 3e argument est true,
        // ce sera fait à chaque démarrage
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);

    }

    protected void onStart() {
        //appelée après oncreate ou en cas de retour sur l'appli,
        // c'est là qu'on doit checker si le GPS est activé ou pas
        // au cas où il aurait été désactivé après le début de l'appli
        //TODO
        Log.i(TAG, "onstart");
        super.onStart();
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
/*
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
*/
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            permsOk = true;
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_AFL);
        }

        prefReactionTime = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("tempsReaction", "2"));
        prefMasseVehicule = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("masseVehicule", "1200"));
        prefMasseVehicule = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("massePilote", "80"));

        mSensorManager.registerListener(mSensListener, mSensor, SensorManager.SENSOR_DELAY_UI);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // Handle item selection
        switch (id) {
            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);

                return true;
            //case R.id.help:
            //    showHelp();
            //    return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void update_info() {
        DecimalFormat frm = new DecimalFormat("0");
        double m = prefMasseVehicule;
        double a = -6; //déceleration du véhicule en m/s-2
        double v = mCurrentLocation.getSpeed();
        double vk = v * 3.6;
        //double tp = prefReactionTime; // temps de perception-reaction moyen admis: 2s
        double dp = prefReactionTime * v;
        double df = -(v * v) / (2 * a);
        double dclcol = v / 0.1;      // 0.1 = durée du choc
        double dclg = dclcol / 9.81;
        double frccol = m * dclcol;
        double poidsCol = frccol / 9.81;
        mLon.setText(String.valueOf(mCurrentLocation.getLongitude()));
        mLat.setText(String.valueOf(mCurrentLocation.getLatitude()));
        mPrecision.setText(String.valueOf(mCurrentLocation.getAccuracy()));

        mSpeedm.setText(frm.format(v));
        mSpeedk.setText(frm.format(vk));
        mDistSecu.setText(frm.format(dp));
        mDistFrein.setText(frm.format(df));
        mDistArret.setText(frm.format(dp + df));

        mDecel.setText(frm.format(dclcol));
        mDecelg.setText(frm.format(dclg));
        mForce.setText(frm.format(frccol));
        mPoidsCol.setText(frm.format(poidsCol));


    }

    private void update_accel(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            //on filtre
            // high pass filter
            float updateFreq = 30; // match this to your update speed
            float cutOffFreq = 0.9f;
            float RC = 1.0f / cutOffFreq;
            float dt = 1.0f / updateFreq;
            float filterConstant = RC / (dt + RC);
            float alpha = filterConstant;
            //float kAccelerometerMinStep = 0.033f;
            //float kAccelerometerNoiseAttenuation = 3.0f;

            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // A commenter/decommenter si besoin
            //float d = clamp(Math.abs(norm(filteredAccel[0], filteredAccel[1], filteredAccel[2]) - norm(x, y, z)) / kAccelerometerMinStep - 1.0f, 0.0f, 1.0f);
            //alpha = d * filterConstant / kAccelerometerNoiseAttenuation + (1.0f - d) * filterConstant;


            filteredAccel[0] = (float) (alpha * (filteredAccel[0] + x - lastAccel[0]));
            filteredAccel[1] = (float) (alpha * (filteredAccel[1] + y - lastAccel[1]));
            filteredAccel[2] = (float) (alpha * (filteredAccel[2] + z - lastAccel[2]));

            lastAccel[0] = x;
            lastAccel[1] = y;
            lastAccel[2] = z;

            maxAccel = (filteredAccel[2] > maxAccel) ? filteredAccel[2] : maxAccel;


            //onFilteredAccelerometerChanged(accelFilter[0], accelFilter[1], accelFilter[2]);

            DecimalFormat frm = new DecimalFormat("0");
            mAccel.setText(frm.format(Math.abs(filteredAccel[2])));
            //mMaxAccel.setText(frm.format(maxAccel));
        }

        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            //mAccel.setText(frm.format(z));
        }
    }


    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();

        mSensorManager.unregisterListener(mSensListener);
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
/*        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }*/
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mLocationManager.removeUpdates(this);


    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();
/*
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
*/

    }

/********* Location **********/




    /**
     * Requests location updates from the FusedLocationApi.
     */
/*
    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        Log.i(TAG, "startLocationUpdate");
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_AFL);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else permsOk = true;

        if (permsOk) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }

    }
*/


	private final SensorEventListener mSensListener = new SensorEventListener() {
		
		@Override
		public void onSensorChanged(SensorEvent event) {
			// TODO Auto-generated method stub
			update_accel(event);
		}
		
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
			
		}
	};

    /************ Google API ************/
    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
/*
    @Override
    public void onConnected(Bundle connectionHint) {
        // Provides a simple way of getting a device's location and is well suited for
        // applications that do not require a fine-grained location and that do not need location
        // updates. Gets the best and most recent location currently available, which may be null
        // in rare cases when a location is not available.
        Log.i(TAG, "Connected");
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mCurrentLocation != null) {
            update_info();
        } else {
            //Toast.makeText(this, R.string.no_location_detected, Toast.LENGTH_LONG).show();
            Toast.makeText(this, "position non detectée", Toast.LENGTH_LONG).show();
        }
        startLocationUpdates();
    }
*/

    /**
     * Callback that fires when the location changes.
     */
    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        //mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        update_info();
        //Toast.makeText(this, getResources().getString(R.string.location_updated_message),Toast.LENGTH_SHORT).show();
    }

/*
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection ratée: ConnectionResult.getErrorCode() = " + result.getErrorCode());
        GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this, 1);
    }


    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }
*/



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_AFL: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    permsOk = true;

                } else {


                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    public void onStatusChanged(String provider, int status, Bundle extras) {}

    public void onProviderEnabled(String provider) {}

    public void onProviderDisabled(String provider) {}




    /************ Clicks **************/

    private void popup(String titre, String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setTitle(titre);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void onDistFreinClick(View v){
        popup("Distance de freinage","Distance parcourue pendant l'action du frein jusqu'à l'arrêt complet.");
    }
    public void onDistSecuClick(View v){
        popup("Distance de sécurité","Distance parcourue pendant le temps de réaction. Le temps de réaction commence lorsque le conducteur voit un danger et le moment où il déclenche le frein.");
    }
    public void onDistArretClick(View v){
        popup("Distance d'arrêt","Distance totale parcourue pour s'arrêter. Tout obstacle fixe situé plus près sera percuté.");
    }



}