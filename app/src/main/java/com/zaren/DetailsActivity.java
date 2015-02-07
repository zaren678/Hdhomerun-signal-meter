package com.zaren;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zaren.HdhomerunSignalMeterLib.data.HdhomerunCommErrorException;
import com.zaren.HdhomerunSignalMeterLib.data.HdhomerunDevice;
import com.zaren.HdhomerunSignalMeterLib.util.ErrorHandler;
import com.zaren.HdhomerunSignalMeterLib.util.HDHomerunLogger;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

public class DetailsActivity extends Activity
{

   private static final int MIN_RESTART_FIRMWARE_VERSION = 20111025;
   private static final String SELF = "self";
   private static final String CABLECARD = "cablecard";
   private HdhomerunDevice device;

   /*
    * (non-Javadoc)
    * 
    * @see android.app.Activity#onCreate(android.os.Bundle)
    */
   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      HDHomerunLogger.d("DetailsActivity: onCreate");
      Bundle bundle = getIntent().getExtras();

      device = (HdhomerunDevice) bundle.getSerializable("device");
      
      setContentView(R.layout.details_activity);

      refreshItems();
      super.onCreate(savedInstanceState);
   }

   /*
    * (non-Javadoc)
    * 
    * @see android.app.Activity#onDestroy()
    */
   @Override
   protected void onDestroy()
   {
      HDHomerunLogger.d("DetailsActivity: onDestroy");
      super.onDestroy();
   }
   
   /* (non-Javadoc)
    * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
    */
   @Override
   public boolean onCreateOptionsMenu(Menu menu)
   {
      HDHomerunLogger.d("DetailsActivity: onCreateOptionsMenu");
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.details_menu, menu);
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
         case R.id.details_menu_refresh:
            refreshItems();
            break;
         case R.id.details_menu_restart_self:
            restartDevice(SELF);
            break;
         case R.id.details_menu_restart_cablecard:
            restartDevice(CABLECARD);
            break;
         default:
            return super.onOptionsItemSelected(item);
      }
      return true;
   }

   private void restartDevice(String type)
   {
      if (device != null)
      {
         device.setVar("/sys/restart", type);
         HdhomerunActivity.getUiElements().stop();
      }
      else
      {
         ErrorHandler.HandleError("No Device Set");
      }
   }

   private void refreshItems()
   {
      TextView deviceId = (TextView) findViewById(R.id.device_id);
      TextView deviceIp = (TextView) findViewById(R.id.device_ip);
      TextView tuner = (TextView) findViewById(R.id.tuner_num);
      TextView deviceModel = (TextView) findViewById(R.id.device_model);
      TextView firmwareVersion = (TextView) findViewById(R.id.firmware_version);
      TextView lockkeyOwner = (TextView) findViewById(R.id.lockkey_owner);
      TextView targetIp = (TextView) findViewById(R.id.target_ip);

      if (device != null)
      {
         deviceId.setText("Device ID: " + String.format("%X",device.getDeviceId()));
         
         try
         {
            InetAddress theIpAddress = InetAddress.getByAddress(device.getIpAddrArray());
            deviceIp.setText("Device IP: "+theIpAddress.getHostAddress());
         }
         catch (UnknownHostException e1)
         {
            deviceModel.setText("Device IP: Unknown");
         }
         
         
         tuner.setText("Tuner: "+device.getTuner());
         try
         {
            deviceModel.setText("Device Model: "+device.getModel());
         }
         catch (HdhomerunCommErrorException e)
         {
            ErrorHandler.HandleError(e.getError());
            deviceModel.setText("Device Model: NULL");
         }
         firmwareVersion.setText("Firmware: " + device.getFirmwareVersion());
         lockkeyOwner.setText("Lockkey Owner: " + device.getLockkeyOwner());
         targetIp.setText("Target IP: " + device.getTargetIp());
      }
      else
      {
         deviceId.setText("Device ID: None");
         deviceIp.setText("Device IP: ");
         tuner.setText("Tuner: ");;
         deviceModel.setText("Device Model: ");
         firmwareVersion.setText("Firmware: ");
         lockkeyOwner.setText("Lockkey Owner: ");
         targetIp.setText("Target IP: ");
      }
      
   }

   /* (non-Javadoc)
    * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
    */
   @Override
   public boolean onPrepareOptionsMenu(Menu menu)
   {     
      if(device != null)
      {
         MenuItem restartSelfMenuItem = menu.findItem(R.id.details_menu_restart_self);
         MenuItem restartCableCardMenuItem = menu.findItem(R.id.details_menu_restart_cablecard);
         if(getFirmwareVersionIntegerFromString(device.getFirmwareVersion()) >= MIN_RESTART_FIRMWARE_VERSION)
         {   
            restartSelfMenuItem.setEnabled(true);
            
            //Now check to see if we are a cablecard device
            if(device.getDeviceType() == HdhomerunDevice.DEVICE_CABLECARD)
            {
               restartCableCardMenuItem.setEnabled(true);
            }
            else
            {
               restartCableCardMenuItem.setEnabled(false);
            }
         }
         else
         {
            restartSelfMenuItem.setEnabled(false);
            restartCableCardMenuItem.setEnabled(false);
         }
         
      }
      
      
      return super.onPrepareOptionsMenu(menu);
   }

   /* (non-Javadoc)
    * @see android.app.Activity#onResume()
    */
   @Override
   protected void onResume()
   {
      SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(HdhomerunActivity.getAppContext());
      boolean lockOrientation = mPreferences.getBoolean(Preferences.KEY_PREF_LOCK_ORIENTATION, false);
      
      if(lockOrientation == true)
      {
         this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
      }
      else
      {
         this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
      }
      
      super.onResume();
   }
   
   public int getFirmwareVersionIntegerFromString(String firmwareVersionStr )
   {
      //make the default return value the restart firmware, if its too old than restart will just get rejected
      int retVal = MIN_RESTART_FIRMWARE_VERSION;
      
      //parsing a string that looks like this 20120217beta2
      Pattern firmwarePattern = Pattern.compile("^(\\d{8})");
      Matcher m = firmwarePattern.matcher(firmwareVersionStr);
      
      if(m.find() == true)
      {
         if(m.group(0) != null)
         {
            try
            {
               retVal = Integer.parseInt(m.group(0));
            }
            catch (NumberFormatException e)
            {
               HDHomerunLogger.e(m.group(0) + " is not a parsable number");
            }
         }
      }
      else
      {
         HDHomerunLogger.e("Error parsing firmware version from " + firmwareVersionStr);
      }
      
      return retVal;
   }

}
