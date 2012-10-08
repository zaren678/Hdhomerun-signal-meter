package com.zaren;

import com.zaren.HdhomerunSignalMeterLib.util.HDHomerunLogger;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TextView;

/**
 * About dialog. Builds custom alert dialog.
 */
public class AboutActivity extends Activity
{

   /*
    * (non-Javadoc)
    * 
    * @see android.app.Activity#onCreate(android.os.Bundle)
    */
   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      HDHomerunLogger.d("AboutActivity: onCreate");
      super.onCreate(savedInstanceState);
      setContentView(R.layout.about_dialog);

      String versionName = "unknown";
      PackageInfo pInfo;
      try
      {
         pInfo = this.getPackageManager().getPackageInfo(this.getPackageName(),
               0);
         versionName = pInfo.versionName;
      }
      catch (NameNotFoundException e)
      {
         HDHomerunLogger.e("Unable to retrieve versionName from manifest file");
      }
      
      TextView versionText = (TextView) this.findViewById(R.id.versionText);
      versionText.setText("Version: " + versionName);
      
      TextView modelText = (TextView) this.findViewById(R.id.modelText);
      modelText.setText("Model: " + android.os.Build.MODEL);
   }

   /*
    * (non-Javadoc)
    * 
    * @see android.app.Activity#onDestroy()
    */
   @Override
   protected void onDestroy()
   {
      HDHomerunLogger.d("AboutActivity: onDestroy");
      super.onDestroy();
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