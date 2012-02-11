package net.sf.wubiq.android;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Toast;

/**
 * Main activity.
 * @author Federico Alcantara
 *
 */
public class WubiqActivity extends Activity {
	public static final String PREFERENCES = "WUBIQ_ANDROID";
	public static final String HOST_KEY="server_host";
	public static final String PORT_KEY="server_port";
	public static final String UUID_KEY="client_uuid";
	public static final String DEVICE_PREFIX = "wubiq-android-bt_";
	
	@SuppressWarnings("unused")
	private PrintManagerService printManagerService;
	private boolean printManagerServiceBound = false;
	private ServiceConnection serviceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder binder) {
			printManagerService = ((PrintManagerService.PrintManagerBinder)binder).getService();
			Toast.makeText(WubiqActivity.this, R.string.service_started, Toast.LENGTH_SHORT);
		}

		public void onServiceDisconnected(ComponentName name) {
			printManagerService = null;
		}
		
	};

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
    
    /**
     * Called when configure server is selected.
     * @param view Calling view object.
     */
    public void configureServer(View view) {
    	Intent intent = new Intent(this, ConfigureServerActivity.class);
    	startActivity(intent);
    }

    /**
     * Invokes bluetooth devices configuration.
     * @param view Calling view object.
     */
    public void configureBluetooth(View view) {
    	Intent intent = new Intent(this, ConfigureBluetoothActivity.class);
    	startActivity(intent);
    }
    
    /**
     * Start print services.
     * @param view Calling view object.
     */
    public void startService(View view) {
		bindService(new Intent(this, PrintManagerService.class), serviceConnection, Context.BIND_AUTO_CREATE);
		printManagerServiceBound = true;
    }
    
    /**
     * Stop print services. 
     * @param view Calling view object.
     */
    public void stopService(View view) {
    	if (printManagerServiceBound) {
    		unbindService(serviceConnection);
    		printManagerServiceBound = false;
    	}
    }
}