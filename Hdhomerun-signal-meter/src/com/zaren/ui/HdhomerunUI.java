package com.zaren.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.os.Handler;

import com.zaren.HdhomerunActivity;
import com.zaren.Preferences;
import com.zaren.R;
import com.zaren.HdhomerunSignalMeterLib.data.ChannelScanProgram;
import com.zaren.HdhomerunSignalMeterLib.data.CurrentChannelAndProgram;
import com.zaren.HdhomerunSignalMeterLib.data.DeviceController;
import com.zaren.HdhomerunSignalMeterLib.data.DeviceListInt;
import com.zaren.HdhomerunSignalMeterLib.data.DeviceResponse;
import com.zaren.HdhomerunSignalMeterLib.data.HdhomerunDiscoverDevice;
import com.zaren.HdhomerunSignalMeterLib.data.HdhomerunDiscoverDeviceArray;
import com.zaren.HdhomerunSignalMeterLib.data.OnChannelMapSelectedListener;
import com.zaren.HdhomerunSignalMeterLib.data.OnDeviceSelectedListener;
import com.zaren.HdhomerunSignalMeterLib.data.OnProgramSelectedListener;
import com.zaren.HdhomerunSignalMeterLib.data.ProgramsList;
import com.zaren.HdhomerunSignalMeterLib.data.TunerStatus;
import com.zaren.HdhomerunSignalMeterLib.events.ChannelChangedObserverInt;
import com.zaren.HdhomerunSignalMeterLib.events.ChannelMapListChangedObserverInt;
import com.zaren.HdhomerunSignalMeterLib.events.ChannelMapObserverInt;
import com.zaren.HdhomerunSignalMeterLib.events.ProgramListObserverInt;
import com.zaren.HdhomerunSignalMeterLib.events.ProgramObserverInt;
import com.zaren.HdhomerunSignalMeterLib.events.TunerStatusObserverInt;
import com.zaren.HdhomerunSignalMeterLib.ui.ArraySpinner;
import com.zaren.HdhomerunSignalMeterLib.ui.HdhomerunSignalMeterUiInt;
import com.zaren.HdhomerunSignalMeterLib.ui.IndeterminateProgressBarInt;
import com.zaren.HdhomerunSignalMeterLib.ui.ProgressBarDrawables;
import com.zaren.HdhomerunSignalMeterLib.ui.SigStrengthProgressBar;
import com.zaren.HdhomerunSignalMeterLib.ui.SnrQualityProgressBar;
import com.zaren.HdhomerunSignalMeterLib.ui.SymbolQualityProgressBar;
import com.zaren.HdhomerunSignalMeterLib.util.ErrorHandler;
import com.zaren.HdhomerunSignalMeterLib.util.HDHomerunLogger;
import com.zaren.HdhomerunSignalMeterLib.util.Utils;

public class HdhomerunUI implements HdhomerunSignalMeterUiInt, IndeterminateProgressBarInt, DeviceListInt, TunerStatusObserverInt, ChannelMapObserverInt, ChannelMapListChangedObserverInt, ChannelChangedObserverInt, ProgramObserverInt, ProgramListObserverInt
{
    private Button mTuneButton;
    private TextView mChannelText;
    private TextView mDataRateText;
    private TextView mNetworkDataRateText;
    private EditText mChannelEditText;
    private ArraySpinner mDeviceSpinner;
    private ArraySpinner mChannelMapSpinner;
    private ArraySpinner mProgramSpinner;
    private SigStrengthProgressBar mSigStrBar;
    private SnrQualityProgressBar mSnrQualBar;
    private SymbolQualityProgressBar mSymQualBar;
    private HdhomerunActivity mMainActivity;
    private ProgressBarDrawables mProgressBarDrawables;
    private boolean mEnableDetailsMenu = false;
    private boolean mIsBusy = false;
    private static DeviceController mCntrl;
    private Handler mUiHandler;

    /**
     * @param mainActivity
     * @param deviceControl
     */
    public HdhomerunUI( final HdhomerunActivity mainActivity, final DeviceController deviceControl )
    {
        mMainActivity = mainActivity;
        mCntrl = deviceControl;
        mUiHandler = new Handler();
    }

    public void buildUIElements()
    {
        mProgressBarDrawables = new ProgressBarDrawables( mMainActivity );
        mTuneButton = (Button)mMainActivity.findViewById( R.id.tuneButton );
        mTuneButton.setOnClickListener( new View.OnClickListener()
        {
            public void onClick( View v )
            {
                tuneButtonClick();
            }
        } );

        ImageButton refreshDevicesButton = (ImageButton)mMainActivity.findViewById( R.id.refreshDevicesButton );
        refreshDevicesButton.setOnClickListener( new View.OnClickListener()
        {
            public void onClick( View v )
            {
                mMainActivity.discoverDevices();
            }
        } );

        mChannelText = (TextView)mMainActivity.findViewById( R.id.channelTextView );
        mDataRateText = (TextView)mMainActivity.findViewById( R.id.dataRateText );
        mNetworkDataRateText = (TextView)mMainActivity.findViewById( R.id.networkDataRate );

        // if channelEditText is not null that probably means we're in a
        // configuration change
        Editable curChannel = null;
        if( mChannelEditText != null )
        {
            curChannel = mChannelEditText.getText();
        }

        mChannelEditText = (EditText)mMainActivity.findViewById( R.id.channelText );

        if( curChannel != null )
        {
            mChannelEditText.setText( curChannel );
        }

        mChannelEditText.setOnKeyListener( new OnKeyListener()
        {
            public boolean onKey( View v, int keyCode, KeyEvent event )
            {
                // If the event is a key-down event on the "enter" button
                if( ( event.getAction() == KeyEvent.ACTION_DOWN ) && ( keyCode == KeyEvent.KEYCODE_ENTER ) )
                {
                    tuneButtonClick();
                    InputMethodManager imm = (InputMethodManager)mMainActivity.getSystemService( Context.INPUT_METHOD_SERVICE );
                    imm.hideSoftInputFromWindow( mChannelEditText.getWindowToken(), 0 );
                }
                return false;
            }
        } );

        ImageButton scanBackButton = (ImageButton)mMainActivity.findViewById( R.id.scanBackButton );
        scanBackButton.setOnClickListener( new View.OnClickListener()
        {
            public void onClick( View v )
            {
                channelScanBackwardClick();
            }
        } );

        ImageButton scanForwardButton = (ImageButton)mMainActivity.findViewById( R.id.scanForwardButton );
        scanForwardButton.setOnClickListener( new View.OnClickListener()
        {
            public void onClick( View v )
            {
                channelScanForwardClick();
            }
        } );

        // Initializing all the spinners are similar to each other, if it already
        // exists that means we are mid
        // reconfiguration so save off the state and set the new one back to that
        // state
        ArrayAdapter<?> deviceArrayAdapter = null;
        int curDevicePos = 0;
        if( mDeviceSpinner != null )
        {
            deviceArrayAdapter = mDeviceSpinner.getArrayAdapter();
            curDevicePos = mDeviceSpinner.getSelectedItemPosition();
        }

        mDeviceSpinner = (ArraySpinner)mMainActivity.findViewById( R.id.deviceSpinner );

        if( deviceArrayAdapter != null )
        {
            mDeviceSpinner.setArrayAdapter( deviceArrayAdapter );
            mDeviceSpinner.setOnItemSelectedListener( new OnDeviceSelectedListener( this, this ) );
            mDeviceSpinner.setSelectionSilently( curDevicePos );
        }

        ArrayAdapter<?> channelMapArrayAdapter = null;
        int curMapPos = 0;
        if( mChannelMapSpinner != null )
        {
            channelMapArrayAdapter = mChannelMapSpinner.getArrayAdapter();
            curMapPos = mChannelMapSpinner.getSelectedItemPosition();
        }
        mChannelMapSpinner = (ArraySpinner)mMainActivity.findViewById( R.id.channelMapSpinner );
        if( channelMapArrayAdapter != null )
        {
            mChannelMapSpinner.setArrayAdapter( channelMapArrayAdapter );
            mChannelMapSpinner.setOnItemSelectedListener( new OnChannelMapSelectedListener( mCntrl ) );
            mChannelMapSpinner.setSelectionSilently( curMapPos );
        }

        ArrayAdapter<?> progArrayAdapter = null;
        int curProgPos = 0;
        if( mProgramSpinner != null )
        {
            progArrayAdapter = mProgramSpinner.getArrayAdapter();
            curProgPos = mProgramSpinner.getSelectedItemPosition();
        }
        mProgramSpinner = (ArraySpinner)mMainActivity.findViewById( R.id.programSpinner );
        if( progArrayAdapter != null )
        {
            mProgramSpinner.setArrayAdapter( progArrayAdapter );
            mProgramSpinner.setOnItemSelectedListener( new OnProgramSelectedListener( mCntrl ) );
            mProgramSpinner.setSelectionSilently( curProgPos );
        }

        mSigStrBar = (SigStrengthProgressBar)mMainActivity.findViewById( R.id.signalStrengthBar );
        mSnrQualBar = (SnrQualityProgressBar)mMainActivity.findViewById( R.id.snrQualBar );
        mSymQualBar = (SymbolQualityProgressBar)mMainActivity.findViewById( R.id.symQualhBar );

        mSigStrBar.setProgressBarDrawables( mProgressBarDrawables );
        mSigStrBar.setTextSize( mMainActivity.getResources().getDimension( R.dimen.progress_bar_text ) );
        mSnrQualBar.setProgressBarDrawables( mProgressBarDrawables );
        mSnrQualBar.setTextSize( mMainActivity.getResources().getDimension( R.dimen.progress_bar_text ) );
        mSymQualBar.setProgressBarDrawables( mProgressBarDrawables );
        mSymQualBar.setTextSize( mMainActivity.getResources().getDimension( R.dimen.progress_bar_text ) );
    }

    protected void channelScanForwardClick()
    {
        if( mCntrl == null )
        {
            ErrorHandler.HandleError( "No Device Set" );
            return;
        }

        mCntrl.channelScanForward();
    }

    protected void channelScanBackwardClick()
    {
        if( mCntrl == null )
        {
            ErrorHandler.HandleError( "No Device Set" );
            return;
        }

        mCntrl.channelScanBackward();
    }

    protected void tuneButtonClick()
    {
        if( mCntrl == null )
        {
            ErrorHandler.HandleError( "No Device Set" );
            return;
        }

        int channel;
        try
        {
            channel = Integer.parseInt( mChannelEditText.getText().toString() );
        }
        catch( NumberFormatException e )
        {
            ErrorHandler.HandleError( "Channel must be a number." );
            return;
        }

        SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences( HdhomerunActivity.getAppContext() );
        boolean virtualTune = mPreferences.getBoolean( Preferences.KEY_PREF_VTUNE, false );

        mCntrl.setTunerChannel( channel + "", virtualTune );
    }

    private void setChannelText( String aChannel )
    {
        mChannelText.setText( aChannel );
    }

    private void setDataRateText( String aDataRate )
    {
        mDataRateText.setText( aDataRate );
    }

    private void setNetworkDataRateText( String aNetworkDataRate )
    {
        mNetworkDataRateText.setText( aNetworkDataRate );
    }

    public void setChannelEditText( String aChannel )
    {
        mChannelEditText.setText( aChannel );
    }

    public void setSigStrBar( TunerStatus aDeviceTunerStatus )
    {
        // There seems to be a bug with progressbar where it doesn't update if you
        // set the same
        mSigStrBar.setProgress( 0, aDeviceTunerStatus );

        mSigStrBar.setProgress( (int)aDeviceTunerStatus.signalStrength, aDeviceTunerStatus );
        mSigStrBar.setText( "" + aDeviceTunerStatus.signalStrength );
    }

    public void setSnrQualBar( long snrQual )
    {
        // There seems to be a bug with progressbar where it doesn't update if you
        // set the same
        mSnrQualBar.setProgress( 0 );

        mSnrQualBar.setProgress( (int)snrQual );
        mSnrQualBar.setText( "" + snrQual );
    }

    public void setSymQualBar( long symQual )
    {
        // There seems to be a bug with progressbar where it doesn't update if you
        // set the same
        mSymQualBar.setProgress( 0 );

        mSymQualBar.setProgress( (int)symQual );
        mSymQualBar.setText( "" + symQual );
    }

    public void setCntrl( DeviceController deviceControl )
    {
        mCntrl = deviceControl;

        if( deviceControl != null )
        {
            mEnableDetailsMenu = true;
            mCntrl.events().channelChanged().registerObserver( this );
            mCntrl.events().channelMapChanged().registerObserver( this );
            mCntrl.events().channelMapListChanged().registerObserver( this );
            mCntrl.events().programChanged().registerObserver( this );
            mCntrl.events().programListChanged().registerObserver( this );
            mCntrl.events().tunerStatusChanged().registerObserver( this );
        }
        else
        {
            mEnableDetailsMenu = false;
        }
    }

    /**
     * @return the cntrl
     */
    public DeviceController getCntrl()
    {
        return mCntrl;
    }

    public void pause()
    {
        if( mCntrl != null )
        {
            mCntrl.stopTunerStatusUpdates();
        }
    }

    public void resume()
    {
        if( mCntrl != null )
        {
            mCntrl.startTunerStatusUpdates();
        }
    }

    public void stop()
    {
        HDHomerunLogger.d( "UI: stop" );
        if( mCntrl != null )
        {
            mCntrl.requestStop();
            mCntrl.destroyDevice();
        }

        mCntrl = null;

        mEnableDetailsMenu = false;

        TunerStatus tunerStatus = new TunerStatus( "none", "none", false, false, true, 0, 0, 0, 0, 0, -1 );

        setProgressBarBusy( false );
        setChannelEditText( "" );
        setChannelText( "none" );
        setDataRateText( String.format( "%.3f Mbps", 0.000 ) );
        setNetworkDataRateText( String.format( "%.3f Mbps", 0.000 ) );
        setSnrQualBar( 0 );
        setSymQualBar( 0 );
        setSigStrBar( tunerStatus );
        mProgramSpinner.setAdapter( new ArrayAdapter<String>( mMainActivity,
                                                              android.R.layout.simple_spinner_item,
                                                              new String[ 0 ] ) );
        mChannelMapSpinner.setAdapter( new ArrayAdapter<String>( mMainActivity,
                                                                 android.R.layout.simple_spinner_item,
                                                                 new String[ 0 ] ) );
        mDeviceSpinner.setAdapter( new ArrayAdapter<String>( mMainActivity,
                                                             android.R.layout.simple_spinner_item,
                                                             new String[ 0 ] ) );
    }

    public void setChannelMapSpinnerPos( int setChannelMapIdx )
    {
        if( ( setChannelMapIdx >= 0 ) && ( setChannelMapIdx < mChannelMapSpinner.getCount() ) )
        {
            HDHomerunLogger.d( "setChannelMapSpinnerPos: idx " + setChannelMapIdx );
            mChannelMapSpinner.setSelection( setChannelMapIdx );
        }
    }

    public void setChannelMapSpinnerPosSilently( int setChannelMapIdx )
    {
        if( ( setChannelMapIdx >= 0 ) && ( setChannelMapIdx < mChannelMapSpinner.getCount() ) )
        {
            HDHomerunLogger.d( "setChannelMapSpinnerPosSilently: idx " + setChannelMapIdx );
            mChannelMapSpinner.setSelectionSilently( setChannelMapIdx );
        }
    }

    public void setProgramSpinnerPos( int programPosition )
    {
        if( ( programPosition >= 0 ) && ( programPosition < mProgramSpinner.getCount() ) )
        {
            HDHomerunLogger.d( "setProgramSpinnerPos: idx " + programPosition );
            mProgramSpinner.setSelection( programPosition );
        }
    }

    public void setProgramSpinnerPosSilently( int programPosition )
    {
        if( ( programPosition >= 0 ) && ( programPosition < mProgramSpinner.getCount() ) )
        {
            HDHomerunLogger.d( "setProgramSpinnerPosSilently: idx " + programPosition );
            mProgramSpinner.setSelectionSilently( programPosition );
        }
    }

    /**
     * @return the enableDetailsMenu
     */
    public boolean isEnableDetailsMenu()
    {
        return mEnableDetailsMenu;
    }

    @Override
    public void setDeviceList( HdhomerunDiscoverDeviceArray aDeviceList )
    {
        int oldDevicePos = mDeviceSpinner.getSelectedItemPosition();

        ArrayAdapter<HdhomerunDiscoverDevice> deviceArrayAdapter = new ArrayAdapter<HdhomerunDiscoverDevice>(
                mMainActivity,
                android.R.layout.simple_spinner_item,
                aDeviceList.getDiscoverDeviceList() );
        deviceArrayAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
        mDeviceSpinner.setArrayAdapter( deviceArrayAdapter );
        mDeviceSpinner.setOnItemSelectedListener( new OnDeviceSelectedListener( this, this ) );

        if( oldDevicePos != AdapterView.INVALID_POSITION )
        {
            mDeviceSpinner.setSelection( oldDevicePos );
        }
    }

    @Override
    public void tunerStatusChanged( DeviceResponse aResponse, final DeviceController aDeviceController, TunerStatus aTunerStatus, CurrentChannelAndProgram aCurrentChannel )
    {
        if( aResponse.getStatus() == DeviceResponse.SUCCESS )
        {
            setChannelText( Utils.getChannelStringFromTunerStatusChannel( aDeviceController.getDevice(),
                                                                          aTunerStatus.channel,
                                                                          aTunerStatus.lockStr ) );
            setSigStrBar( aTunerStatus );
            setSnrQualBar( aTunerStatus.snrQuality );
            setSymQualBar( aTunerStatus.symbolErrorQuality );
            setDataRateText( aTunerStatus.dataRateString() );
            setNetworkDataRateText( aTunerStatus.networkDataRateString() );

            try
            {
                String theChannelEditString = mChannelEditText.getText().toString();
                int theChannelEditNum = -1;

                if( theChannelEditString != null && theChannelEditString.length() != 0 )
                {
                    theChannelEditNum = Integer.parseInt( mChannelEditText.getText().toString() );
                }

                final int theStatusNum = Utils.getChannelNumberFromTunerStatusChannel( aDeviceController.getDevice(),
                                                                                       aTunerStatus.channel );

                if( theChannelEditNum != theStatusNum )
                {
                    if( !mChannelEditText.hasFocus() )
                    {
                        mChannelEditText.setText( theStatusNum + "" );
                    }

                    mUiHandler.postDelayed( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            final ProgramsList thePrograms = new ProgramsList();
                            aDeviceController.getDevice().getTunerStreamInfo( thePrograms );

                            programListChanged( aDeviceController, thePrograms, theStatusNum );
                        }
                    }, 500 );

                }
            }
            catch( NumberFormatException e )
            {
                // don't do anything, just catch it
            }
        }
        else
        {
            HandleFailureResponse( aResponse );
        }
    }

    @Override
    public void programListChanged( DeviceController aDeviceController, ProgramsList aPrograms, int aChannel )
    {
        ArrayAdapter<ChannelScanProgram> adapter = new ArrayAdapter<ChannelScanProgram>( mMainActivity,
                                                                                         android.R.layout.simple_spinner_item,
                                                                                         aPrograms.toList() );
        adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
        mProgramSpinner.setArrayAdapter( adapter );
        mProgramSpinner.setOnItemSelectedListener( new OnProgramSelectedListener( mCntrl ) );
    }

    @Override
    public void channelMapChanged( DeviceResponse aResponse, DeviceController aDeviceController, String aNewChannelMap )
    {
        @SuppressWarnings( "unchecked" ) ArrayAdapter<String> theAdapter = (ArrayAdapter<String>)mChannelMapSpinner.getAdapter();
        int theMapIndex = theAdapter.getPosition( aNewChannelMap );
        setChannelMapSpinnerPosSilently( theMapIndex );

        if( aResponse.getStatus() != DeviceResponse.SUCCESS )
        {
            HandleFailureResponse( aResponse );
        }
    }

    @Override
    public void programChanged( DeviceResponse aResponse, DeviceController aDeviceController, ChannelScanProgram aChannelScanProgram )
    {

        @SuppressWarnings( "unchecked" ) ArrayAdapter<ChannelScanProgram> theAdapter = (ArrayAdapter<ChannelScanProgram>)mProgramSpinner.getAdapter();
        int theCurrentIndex = mProgramSpinner.getSelectedItemPosition();

        if( theCurrentIndex != AdapterView.INVALID_POSITION )
        {
            int theProgramIndex = theAdapter.getPosition( aChannelScanProgram );

            if( theProgramIndex != theCurrentIndex )
            {
                setProgramSpinnerPosSilently( theProgramIndex );
            }
        }

        if( aResponse.getStatus() != DeviceResponse.SUCCESS )
        {
            HandleFailureResponse( aResponse );
        }

    }

    @Override
    public void channelMapListChanged( DeviceController aDeviceController, String[] aChannelMapList )
    {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>( mMainActivity,
                                                                 android.R.layout.simple_spinner_item,
                                                                 aChannelMapList );
        adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
        mChannelMapSpinner.setArrayAdapter( adapter );
        mChannelMapSpinner.setOnItemSelectedListener( new OnChannelMapSelectedListener( aDeviceController ) );
    }

    @Override
    public void channelChanged( DeviceResponse aResponse, DeviceController aDeviceController, int aCurrentChannel )
    {
        if( aResponse.getStatus() == DeviceResponse.SUCCESS )
        {
            if( aCurrentChannel > -1 )
            {
                mChannelEditText.setText( aCurrentChannel + "" );
            }
            else
            {
                mChannelEditText.setText( "" );
            }
        }
    }

    private void HandleFailureResponse( DeviceResponse aResponse )
    {
        StringBuilder theString = new StringBuilder();

        switch( aResponse.getStatus() )
        {
            case DeviceResponse.COMMUNICATION_ERROR:
                theString.append( "Commnication error " );
                break;
            case DeviceResponse.FAILURE:
                if( aResponse.getBoolean( "Locked", false ) )
                {
                    theString.append( "Failure " );
                }
                else
                {
                    theString.append( "Command rejected " );
                }
                break;
            default:
                theString.append( "Unknown error " );
                break;
        }

        String theAction = aResponse.getString( DeviceResponse.KEY_ACTION );
        if( theAction != null )
        {
            theString.append( theAction + " " );
        }

        String theError = aResponse.getString( DeviceResponse.KEY_ERROR );
        if( theAction != null )
        {
            theString.append( theError );
        }

        ErrorHandler.HandleError( theString.toString() );
    }

    @Override
    public void setProgressBarBusy( boolean aIsBusy )
    {
        mMainActivity.setProgressBarIndeterminateVisibility( aIsBusy );
        mIsBusy = aIsBusy;
    }

    @Override
    public boolean getProgressBarBusy()
    {
        return mIsBusy;
    }

    @Override
    public Context getContext()
    {
        // TODO Auto-generated method stub
        return null;
    }

}
