/**
 * 
 */
package net.sf.wubiq.android;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;
import com.zebra.android.comm.BluetoothPrinterConnection;
import com.zebra.android.comm.ZebraPrinterConnection;
import com.zebra.android.comm.ZebraPrinterConnectionException;
import com.zebra.android.printer.PrinterLanguage;
import com.zebra.android.printer.ZebraPrinter;
import com.zebra.android.printer.ZebraPrinterFactory;

/**
 * Handles the necessary steps for printing on the client.
 * @author Federico Alcantara
 *
 */
public enum PrintClientUtils {
	INSTANCE;
	private static final String TAG = "PrintClientUtils";
	int printDelay = 500;
	int printPause = 100; // Per each 1024 bytes
	
	/**
	 * Prints the given input to the device, performing all required conversion steps.
	 * @param context Android context.
	 * @param printServiceName Complete device name.
	 * @param input Input data as a stream.
	 * @param resources Application resources.
	 * @param preferences Shared preferences of the application.
	 * @param printServicesName Available bluetooth devices.
	 */
	public void print(Context context, String printServiceName, InputStream input, Resources resources, 
			SharedPreferences preferences, Map<String, BluetoothDevice> printServicesName) {
		printDelay = preferences.getInt(WubiqActivity.PRINT_DELAY_KEY, resources.getInteger(R.integer.print_delay_default));
		printPause = preferences.getInt(WubiqActivity.PRINT_PAUSE_KEY, resources.getInteger(R.integer.print_pause_default));
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		BluetoothDevice device = printServicesName.get(printServiceName);
		String deviceKey = WubiqActivity.DEVICE_PREFIX + device.getAddress();
		String selection = preferences.getString(deviceKey, null);
		MobileDeviceInfo deviceInfo = MobileDevices.INSTANCE.getDevices().get(selection);
		String deviceAddress = device.getAddress();
		try {
			byte[] b = new byte[16 * 1024];  
			int read;  
			while ((read = input.read(b)) != -1) {  
				output.write(b, 0, read);  
			}  
			byte[] printData = output.toByteArray();
			for (MobileClientConversionStep step : deviceInfo.getClientSteps()) {
				if (step.equals(MobileClientConversionStep.OUTPUT_BYTES)) {
					printBytes(deviceInfo, deviceAddress, printData);
				} else if (step.equals(MobileClientConversionStep.OUTPUT_SM_BYTES)) {
					printStarMicronicsByteArray(deviceInfo, deviceAddress, printData); // Does not print all the data
				} else if (step.equals(MobileClientConversionStep.OUTPUT_ZEBRA_IMAGE)) {
					printZebraImage(deviceInfo, deviceAddress, printData);
				} else if (step.equals(MobileClientConversionStep.OUTPUT_ZEBRA_BYTES)) {
					printZebraBytes(deviceInfo, deviceAddress, printData);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Outputs to a star micronics portable printer.
	 * @param deviceInfo Device information
	 * @param deviceAddress Device address (mac address)
	 * @param printData Data to print
	 * @return true if everything is okey.
	 */
	private boolean printStarMicronicsByteArray(MobileDeviceInfo deviceInfo, String deviceAddress, byte[] printData) {
		StarIOPort port = null;
		try 
    	{
			port = StarIOPort.getPort("bt:" + deviceAddress, "mini", 10000);
			
			try
			{
				Thread.sleep(printDelay);
			}
			catch(InterruptedException e) {
				e.printStackTrace();
			}
        	int start = 0;
        	int chunk = 4096;
        	while (start < printData.length) {
        		int count = (start + chunk) < printData.length ? chunk : printData.length - start;
        		port.writePort(printData, start, count);
        		start += count;
    			try
    			{
    				Thread.sleep(printDelay);
    			}
    			catch(InterruptedException e) {
    				e.printStackTrace();
    			}
        	}
			
			try
			{
				int sleepTime = (int) (printData.length / 1024.0 * printPause);
				Thread.sleep(sleepTime);
				return true;
			}
			catch(InterruptedException e) {
				e.printStackTrace();
			}			
		}
    	catch (Exception e)
    	{
    		e.printStackTrace();
		}
		finally
		{
			if(port != null)
			{
				try {
					StarIOPort.releasePort(port);
				} catch (StarIOPortException e) {}
			}
		}
		
		return false;
	}
	
	/**
	 * Print bytes creating a basic bluetooth connection
	 * @param deviceInfo Device to be connected to.
	 * @param deviceAddress Address of the device
	 * @param printData Data to be printed.
	 * @return true if printing was okey.
	 */
	private boolean printBytes(MobileDeviceInfo deviceInfo, String deviceAddress, byte[] printData) {
		boolean returnValue = false;
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		for (BluetoothDevice device : adapter.getBondedDevices()) {
			if (device.getAddress().equals(deviceAddress)) {
				Thread connectThread = new ConnectThread(device, UUID.randomUUID(), printData);
				connectThread.start();
				returnValue = true;
				break;
			}
		}
		return returnValue;
	}
	
	/**
	 * Private class for handling connection.
	 * @author Federico Alcantara
	 *
	 */
	private class ConnectThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private byte[] printData;
	 
	    public ConnectThread(BluetoothDevice device, UUID uuid, byte[] printData) {
	        // Use a temporary object that is later assigned to mmSocket,
	        // because mmSocket is final
	        BluetoothSocket tmp = null;
	        this.printData = printData;
	        
	        // Get a BluetoothSocket to connect with the given BluetoothDevice
	        try {
	            // MY_UUID is the app's UUID string, also used by the server code
	        	Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
	            tmp = (BluetoothSocket) m.invoke(device, 1);
	        } catch (Exception e) {
	        	Log.e(TAG, e.getMessage());
	        	e.printStackTrace();
	        	if (tmp != null) {
	        		try {
	        			tmp.close();
	        		} catch(IOException ex) {
	        			Log.d(TAG, ex.getMessage());
	        		}
	        	}
	        	tmp = null;
			}
	        mmSocket = tmp;
	    }
	 
	    public void run() {
	    	if (mmSocket != null) {
		    	try {
		            // Connect the device through the socket. This will block
		            // until it succeeds or throws an exception
		            mmSocket.connect();
		        } catch (IOException connectException) {
		            // Unable to connect; close the socket and get out
		            try {
		                mmSocket.close();
		            } catch (IOException closeException) { 
		            	Log.d(TAG, closeException.getMessage());
		            }
		            return;
		        }
		 
		        // Do work to manage the connection (in a separate thread)
		        ConnectedThread connectedThread = new ConnectedThread(mmSocket, printData);
		        connectedThread.start();
	    	}
	    }
	 
	}

	private class ConnectedThread extends Thread {
		private BluetoothSocket socket;
	    private final OutputStream mmOutStream;
	    private byte[] printData;
	    
	    public ConnectedThread(BluetoothSocket socket, byte[] printData) {
	        this.socket = socket;
	    	this.printData = printData;
	        OutputStream tmpOut = null;
	 
	        // Get the input and output streams, using temp objects because
	        // member streams are final
	        try {
	            tmpOut = socket.getOutputStream();
	        } catch (IOException e) {
	        	Log.e(TAG, e.getMessage());
	        }
	 
	        mmOutStream = tmpOut;
	    }
	 
	    public void run() {
	        try {
	        	int start = 0;
	        	int chunk = 4096;
	        	while (start < printData.length) {
	        		int count = (start + chunk) < printData.length ? chunk : printData.length - start;
	        		mmOutStream.write(printData, start, count);
	        		start += count;
	    			try
	    			{
	    				Thread.sleep(printDelay);
	    			}
	    			catch(InterruptedException e) {
	    				e.printStackTrace();
	    			}
	        	}
	        } catch (IOException e) {
	        	Log.e(TAG, e.getMessage());
	        	e.printStackTrace();
	        } finally {
        		try {
    				int sleepTime = (int) (printData.length / 1024.0 * printPause);
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					Log.e(TAG, e.getMessage());
				}
	        	try {
					mmOutStream.close();
				} catch (IOException e) {
					Log.d(TAG, e.getMessage());
				}
	        	try {
					socket.close();
				} catch (IOException e) {
					Log.d(TAG, e.getMessage());
				}
	        }
	    }
	}
	
	/**
	 * Prints directly to zebra.
	 * @param deviceInfo Device to be connected to.
	 * @param deviceAddress Address of the device
	 * @param printData Data to be printed.
	 * @return true if printing was okey. Always return true, 
	 * so printing with this method will be considered okey regardless of any printing error.
	 */
	private boolean printZebraBytes(MobileDeviceInfo sentDeviceInfo, 
			String sentDeviceAddress, byte[] sentPrintData) {
		final String deviceAddress = sentDeviceAddress;
		final byte[] printData = sentPrintData;
		new Thread(new Runnable() {
				public void run(){
					ZebraPrinterConnection connection = null;
					try {
						connection = new BluetoothPrinterConnection(deviceAddress);
						connection.open();
						ByteArrayOutputStream lineOut = new ByteArrayOutputStream();
						int index = 0;
						for (index = 0; index < printData.length; index++) {
							byte byteVal = printData[index];
							if (byteVal == 0x0D && (index + 1) < printData.length  
									&& printData[index + 1] == 0x0A) { // Line feed
								index++;
								lineOut.write(new byte[]{0x0D, 0x0A});
								connection.write(lineOut.toByteArray());
								lineOut = new ByteArrayOutputStream();
								Thread.sleep(printDelay);
							} else {
								lineOut.write(byteVal);
							}
						}
						if (lineOut.size() > 0) {
							connection.write(lineOut.toByteArray());
							Thread.sleep(printDelay);
						}
						Thread.sleep(printPause * printData.length / 1024); 
					} catch (ZebraPrinterConnectionException e) {
						Log.e(TAG, e.getMessage(), e);
					} catch (IOException e) {
						Log.e(TAG, e.getMessage(), e);
					} catch (InterruptedException e) {
						Log.e(TAG, e.getMessage());
					} catch (Exception e) {
						Log.e(TAG, e.getMessage(), e);
					} finally {
						try {
							if (connection != null) {
								connection.close();
							}
						} catch (ZebraPrinterConnectionException e) {
							Log.e(TAG, e.getMessage());
						}
					}
				}
			}
		).start();
		return true;
	}
	
	/**
	 * Prints an image to a zebra printer.
	 * @param deviceInfo Device to be connected to.
	 * @param deviceAddress Address of the device
	 * @param printData Data to be printed.
	 * @return true if printing was okey.
	 */
	private boolean printZebraImage(MobileDeviceInfo sentDeviceInfo, String sentDeviceAddress, byte[] sentPrintData) {
		final String deviceAddress = sentDeviceAddress;
		final byte[] printData = sentPrintData;
		new Thread(new Runnable() {
			public void run() {
				Bitmap bitmap = BitmapFactory.decodeByteArray(printData, 0, printData.length);
				int connectionWaitTime = 5;
				ZebraPrinterConnection connection = null;
				ZebraPrinter printer = null;
				try {
					connection = new BluetoothPrinterConnection(deviceAddress);
					connection.open();
					do {
						Thread.sleep(1000); // To wait until the connection is really established.
					} while (connectionWaitTime-- > 0 || !connection.isConnected());
		            printer = ZebraPrinterFactory.getInstance(PrinterLanguage.CPCL, connection);
		            printer.getGraphicsUtil().printImage(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), false);
				} catch (ZebraPrinterConnectionException e) {
					Log.e(TAG, e.getMessage());
				} catch (InterruptedException e) {
					Log.e(TAG, e.getMessage());
				} finally {
					try {
						if (connection != null) {
							connection.close();
						}
					} catch (ZebraPrinterConnectionException e) {
						Log.e(TAG, e.getMessage());
					}
				}
			}
		}
		).start();
		return true;
		
	}
}
