/**
 * 
 */
package net.sf.wubiq.android.devices;

import android.util.Log;

import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;

/**
 * Implements Wubiq Device mechanism.
 * @author Federico Alcantara
 *
 */
public class DeviceStarMicronics extends BaseWubiqDevice {
	private static final String TAG = "DeviceStarMicronics";
	
	/**
	 * Outputs to a star micronics portable printer.
	 */
	protected boolean print() {
		boolean returnValue = false;
		StarIOPort port = null;
		try 
    	{
			port = StarIOPort.getPort("bt:" + getDeviceAddress(), "mini", 10000);
			Thread.sleep(getPrintDelay());
        	int start = 0;
        	int chunk = 4096;
        	while (start < getPrintData().length) {
        		int count = (start + chunk) < getPrintData().length ? chunk : getPrintData().length - start;
        		port.writePort(getPrintData(), start, count);
        		start += count;
				Thread.sleep(getPrintDelay());
        	}
			int sleepTime = (int) (getPrintData().length / 1024.0 * getPrintPause());
			Thread.sleep(sleepTime);
		}
    	catch (Exception e)
    	{
    		Log.e(TAG, e.getMessage());
    		e.printStackTrace();
		}
		finally
		{
			if(port != null)
			{
				try {
					StarIOPort.releasePort(port);
				} catch (StarIOPortException e) {
					Log.e(TAG, e.getMessage());
				}
			}
		}
		return returnValue;
	}

}