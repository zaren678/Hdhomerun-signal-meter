package com.zaren;

import com.zaren.HdhomerunSignalMeterLib.util.HDHomerunLogger;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
   public static final String KEY_PREF_VTUNE = "vTune_pref";
   public static final String KEY_PREF_DEBUG_TO_FILE = "debug_to_file_pref";
   public static final String KEY_PREF_LOCK_ORIENTATION = "lock_orientation";
   /*
    * (non-Javadoc)
    * 
    * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
    */
   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      HDHomerunLogger.d("Preferences: onCreate");
      super.onCreate(savedInstanceState);

      addPreferencesFromResource(R.xml.preferences);
      
      SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
      mPreferences.registerOnSharedPreferenceChangeListener(this);
   }

   @Override
   public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
   {
      HDHomerunLogger.i("Preference "+key+" changed");
      if(key.equalsIgnoreCase(KEY_PREF_VTUNE))
      {
         HDHomerunLogger.i("Preference " + key + " is now " + sharedPreferences.getBoolean(key, false));
      }
      else if(key.equalsIgnoreCase(KEY_PREF_DEBUG_TO_FILE))
      {
         HDHomerunLogger.setDebugToFile(sharedPreferences.getBoolean(key, false));
      }
      else if(key.equalsIgnoreCase(KEY_PREF_LOCK_ORIENTATION))
      {
         if(sharedPreferences.getBoolean(key, false) == true)
         {
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
         }
         else
         {
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
         }
      }
      
   }

   /* (non-Javadoc)
    * @see android.preference.PreferenceActivity#onDestroy()
    */
   @Override
   protected void onDestroy()
   {
      HDHomerunLogger.d("Preferences: onDestroy");
      super.onDestroy();
      
      SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
      mPreferences.unregisterOnSharedPreferenceChangeListener(this);
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

}
