package com.zaren;

import com.zaren.HdhomerunActivity;
import com.zaren.HdhomerunSignalMeterLib.util.HDHomerunLogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WifiStateReceiver extends BroadcastReceiver 
{

	private HdhomerunActivity mainActivity;
	
	
	public WifiStateReceiver(HdhomerunActivity _mainActivity) 
	{
		mainActivity = _mainActivity;
	}

	@Override
	public void onReceive(Context context, Intent intent) 
	{
		String action = intent.getAction();
		
		if(action.equals(android.net.wifi.WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION))
		{
			boolean connected = intent.getBooleanExtra(android.net.wifi.WifiManager.EXTRA_SUPPLICANT_CONNECTED, true);
			if(connected == true)
			{
				HDHomerunLogger.d("Wifi Connected");
			}
			else
			{
				HDHomerunLogger.d("Wifi Disconnected");
				mainActivity.stop();
			}
		}
	}

}
