package fr.asgardit.securoute;

//import java.text.DecimalFormat;

import java.text.DecimalFormat;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
//import android.location.LocationProvider;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.Display;
import android.view.WindowManager;
import android.widget.TextView;

public class SecurouteActivity extends Activity {
	
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
	
	private TextView mLon;
	private TextView mLat;
	private TextView mSpeedm;
	private TextView mSpeedk;
	private TextView mAccel;
	private TextView mMaxAccel;
	private TextView mDistSecu;
	private TextView mDistFrein;
	private TextView mDistArret;
	private LocationManager mManager;
	private SensorManager mSensorManager;
	private Sensor mSensor;
	//private WindowManager mWindowManager;
	private Display mDisplay;
	//private PowerManager.WakeLock wl;
	private float[] lastAccel = new float[3];
	private float[] filteredAccel = new float[3];
	private float maxAccel = 0;
	
	private static final int ONE_SECOND = 1000;
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mLon = (TextView) this.findViewById(R.id.lon);
        mLat = (TextView) this.findViewById(R.id.lat);
        mSpeedm = (TextView) this.findViewById(R.id.speedm);
        mSpeedk = (TextView) this.findViewById(R.id.speedk);
        mAccel = (TextView) this.findViewById(R.id.accel);
        //mMaxAccel = (TextView) this.findViewById(R.id.maxAccel);
        mDistSecu = (TextView) this.findViewById(R.id.distsec);
        mDistFrein = (TextView) this.findViewById(R.id.distfrein);
        mDistArret = (TextView) this.findViewById(R.id.distarret);
        
        mManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        
        //PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        //wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "GalileoActivity");

        // Get an instance of the SensorManager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        
        
        // Instancier le display qui connaît l'orientation de l'appareil
        mDisplay = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        
        //LocationProvider provider = mManager.getProvider(LocationManager.GPS_PROVIDER);
        
        //boolean gpsEnabled = manager.isProviderEnabled(manager.GPS_PROVIDER);
        
        
    }
    
    protected void onStart(){
    	//appelée après oncreate ou en cas de retour sur l'appli,
    	// c'est là qu'on doit checker si le GPS est activé ou pas
    	// au cas où il aurait été désactivé après le début de l'appli
    	//TODO
    	super.onStart();
    	
    	this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    	    
    }

    protected void onResume() {
        super.onResume();
     
    	mSensorManager.registerListener(mSensListener, mSensor,SensorManager.SENSOR_DELAY_UI);

    	mManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, ONE_SECOND, 0, mListener);
        //wl.acquire();
    }

 
    private void update_info(Location loc){
    	DecimalFormat frm = new DecimalFormat("0");
    	double a = -6; //déceleration du véhicule en m/s-2
    	double v = loc.getSpeed();
    	double vk = v*3.6;
    	double tp = 2; // temps de perception-reaction moyen admis: 2s
    	double dp = tp *v ;
    	double df = -(v*v)/(2*a);
    	mLon.setText(String.valueOf(loc.getLongitude()));
    	mLat.setText(String.valueOf(loc.getLatitude()));
    	mSpeedm.setText(frm.format(v));
    	mSpeedk.setText(frm.format(vk));
    	mDistSecu.setText(frm.format(dp));
    	mDistFrein.setText(frm.format(df));
    	mDistArret.setText(frm.format(dp+df));
    }
    
    private void update_accel(SensorEvent event){
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
            
            maxAccel =(filteredAccel[2] > maxAccel) ? filteredAccel[2] : maxAccel; 
			
            
            //onFilteredAccelerometerChanged(accelFilter[0], accelFilter[1], accelFilter[2]);

            DecimalFormat frm = new DecimalFormat("0");
            mAccel.setText(frm.format(Math.abs(filteredAccel[2])));
    		//mMaxAccel.setText(frm.format(maxAccel));
    	}
    	
    	/*if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
        	float x = event.values[0]; 
        	float y = event.values[1]; 
        	float z = event.values[2]; 
        	
        	mAccel.setText(frm.format(z));
		}*/
    }
    
    
    
    protected void onPause(){
    	super.onPause();
    	//wl.release();
    	
    	mSensorManager.unregisterListener(mSensListener);
    	
    }

/*******************/
	private final LocationListener mListener = new LocationListener() {
		
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
		}
		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
		}
		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
		}
		@Override
		public void onLocationChanged(Location location) {
			// TODO Auto-generated method stub
			update_info(location);
		}
	};
	
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



}