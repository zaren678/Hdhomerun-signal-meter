package com.zaren;


import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.zaren.ui.HdhomerunUI;
import com.zaren.HdhomerunSignalMeterLib.data.DeviceListInt;
import com.zaren.HdhomerunSignalMeterLib.data.DiscoverTask;
import com.zaren.HdhomerunSignalMeterLib.ui.HdhomerunSignalMeterUiInt;
import com.zaren.HdhomerunSignalMeterLib.ui.IndeterminateProgressBarInt;
import com.zaren.HdhomerunSignalMeterLib.util.ErrorHandler;
import com.zaren.HdhomerunSignalMeterLib.util.HDHomerunLogger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;

import android.net.wifi.WifiManager;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

public class HdhomerunActivity extends Activity
{
   
   private WifiStateReceiver wifiReceiver;
   private static Context context;
   private static HdhomerunSignalMeterUiInt mUiElements;
   private HdhomerunUI mUi;
   private static IndeterminateProgressBarInt mProgressBar;
   private static DeviceListInt mDeviceList;

   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
      setContentView(R.layout.main);
      
      
      HDHomerunLogger.d("onCreate");
	  ErrorHandler.mainActivity = this;
	  context = this.getApplicationContext();
      
      
      HdhomerunUI mUi = new HdhomerunUI(this,null);
      mUiElements = mUi;
      mUi.buildUIElements();
      mDeviceList = mUi;
      mProgressBar = mUi;
      
      //register to receive wifi state change broadcasts
      wifiReceiver = new WifiStateReceiver(this);
      IntentFilter filter = new IntentFilter();
      filter.addAction(android.net.wifi.WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
      this.registerReceiver(wifiReceiver, filter);
	  
      discoverDevices();
     
      AdRequest request = new AdRequest.Builder().build();
      //request.addTestDevice(AdRequest.TEST_EMULATOR);
     
      AdView adView = (AdView) findViewById(R.id.adView);
      adView.loadAd(request);
   }

   @Override
   protected void onPause() 
   {
	   HDHomerunLogger.d("HdhomerunActivity: onPause");
	   super.onPause();
	   mUiElements.pause();
   }
   
   @Override
   protected void onResume() 
   {
	  HDHomerunLogger.d("HdhomerunActivity: onResume");
	   
	  SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(HdhomerunActivity.getAppContext());
      boolean lockOrientation = mPreferences.getBoolean(Preferences.KEY_PREF_LOCK_ORIENTATION, false);
      
      if(lockOrientation)
      {
         this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
      }
      else
      {
         this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
      }
	   
	   super.onResume();
	   mUiElements.resume();
   }
   
   @Override
	protected void onStart() 
   {
	   HDHomerunLogger.d("HdhomerunActivity: onStart");
		super.onStart();
	}
	
	@Override
	protected void onStop() 
	{
		HDHomerunLogger.d("HdhomerunActivity: onStop");
		super.onStop();
	}
	
	

	@Override
	protected void onDestroy() 
	{
		super.onDestroy();
		
		this.unregisterReceiver(wifiReceiver);
	}

    protected String wifiIpAddress(Context context) {
        // from http://stackoverflow.com/questions/16730711/get-my-wifi-ip-address-android

        WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        // Convert little-endian to big-endianif needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            // Log.e("WIFIIP", "Unable to get host address.");
            ipAddressString = null;
        }

        return ipAddressString;
    }

	public void discoverDevices()
	{
       boolean isWifi = wifiIpAddress(context) != null;

       if(!isWifi)
	   {
	      ErrorHandler.HandleError("Wifi must be connected");
	      return;
	   }
	   
	   new DiscoverTask(mProgressBar, mDeviceList).execute();
	}	

   public void stop()
   {
      mUiElements.stop();
   }
   
   static HdhomerunSignalMeterUiInt getUiElements()
   {
      return mUiElements;
   }

   /* (non-Javadoc)
    * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
    */
   @Override
   public boolean onCreateOptionsMenu(Menu menu)
   {
      HDHomerunLogger.d("onCreateOptionsMenu");
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.menu, menu);
      return super.onCreateOptionsMenu(menu);
   }

   /* (non-Javadoc)
    * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
    */
   @Override
   public boolean onOptionsItemSelected(MenuItem item)
   {
      switch(item.getItemId())
      {
         case R.id.menu_about:
            Intent aboutActivity = new Intent(getBaseContext(),
                  AboutActivity.class);
            startActivity(aboutActivity);
            break;
         case R.id.menu_prefs:
            Intent settingsActivity = new Intent(getBaseContext(),
                  Preferences.class);
            startActivity(settingsActivity);
            break;
         case R.id.menu_details:
            Intent detailsActivity = new Intent(getBaseContext(),
                  DetailsActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable("device", mUiElements.getCntrl().getDevice());
            detailsActivity.putExtras(bundle);
            startActivity(detailsActivity);
            break;
         default:
            return super.onOptionsItemSelected(item);
      }
      return true;
   }
   
   public static Context getAppContext()
   {
      return context;
   }

   /* (non-Javadoc)
    * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
    */
   @Override
   public boolean onPrepareOptionsMenu(Menu menu)
   {
      MenuItem detailsMenuItem = menu.findItem(R.id.menu_details);
      detailsMenuItem.setEnabled(mUiElements.isEnableDetailsMenu());
      return super.onPrepareOptionsMenu(menu);
   }

   /* (non-Javadoc)
    * @see android.app.Activity#onConfigurationChanged(android.content.res.Configuration)
    */
   @Override
   public void onConfigurationChanged(Configuration newConfig)
   {
      mUiElements.pause();
      HDHomerunLogger.d("onConfigurationChanged()" );
      super.onConfigurationChanged(newConfig);
      
      setContentView(R.layout.main);

      AdRequest request = new AdRequest.Builder().build();
      //request.addTestDevice(AdRequest.TEST_EMULATOR);
     
      AdView adView = (AdView) findViewById(R.id.adView);
      adView.loadAd(request);
      
      
      mUi.buildUIElements();
      mUiElements.resume();
   }
}
