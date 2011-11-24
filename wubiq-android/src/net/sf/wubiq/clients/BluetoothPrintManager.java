/**
 * 
 */
package net.sf.wubiq.clients;


import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import net.sf.wubiq.android.AndroidActivity;
import net.sf.wubiq.android.R;
import net.sf.wubiq.clients.impl.StarDirectPrintImpl;
import net.sf.wubiq.common.CommandKeys;
import net.sf.wubiq.common.ParameterKeys;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

/**
 * Establish the connection to the server and prints the data received.
 * @author Federico Alcantara
 *
 */
public class BluetoothPrintManager extends LocalPrintManager {
	SharedPreferences preferences;
	Context context;
	BluetoothAdapter bAdapter;
	private Map<String, String> compressionMap;

	public BluetoothPrintManager(Context context, SharedPreferences preferences) {
		this.context = context;
		this.preferences = preferences;
		this.bAdapter = BluetoothAdapter.getDefaultAdapter();
		initializeDefault(this);
	}
	
	@Override
	protected void registerPrintServices() throws ConnectException {
		registerComputerName();
		// Gather printServices.
		doLog("Register Print Services");
		for (BluetoothDevice device : bAdapter.getBondedDevices()) {
			String deviceKey = AndroidActivity.DEVICE_PREFIX + device.getAddress();
			String selection = preferences.getString(deviceKey, null);
			if (selection != null) {
				StringBuffer printServiceRegister = new StringBuffer(serializeServiceName(device));
				StringBuffer categories = new StringBuffer(serializePrintServiceCategories(device));
				categories.insert(0, ParameterKeys.PARAMETER_SEPARATOR)
				.insert(0, ParameterKeys.PRINT_SERVICE_CATEGORIES);
				askServer(CommandKeys.REGISTER_PRINT_SERVICE, printServiceRegister.toString(), categories.toString());
			}
		}
	}
	
	@Override
	protected void processPendingJob(String jobId) throws ConnectException {
		StringBuffer parameter = new StringBuffer(ParameterKeys.PRINT_JOB_ID)
		.append(ParameterKeys.PARAMETER_SEPARATOR)
		.append(jobId);
		doLog("Process Pending Job:" + jobId);
		InputStream stream = null;
		try {
			String printServiceName = askServer(CommandKeys.READ_PRINT_SERVICE_NAME, parameter.toString());
			doLog("Job(" + jobId + ") printServiceName:" + printServiceName);
			String attributesData = askServer(CommandKeys.READ_PRINT_ATTRIBUTES, parameter.toString());
			doLog("Job(" + jobId + ") attributesData:" + attributesData);
			stream = (InputStream)pollServer(CommandKeys.READ_PRINT_JOB, parameter.toString());
			doLog("Job(" + jobId + ") stream:" + stream);
			doLog("Job(" + jobId + ") print pdf");
			IMobileDirectPrint directPrint = new StarDirectPrintImpl(context);
			directPrint.printBmpImage(printServiceName, stream);
			doLog("Job(" + jobId + ") printed.");
			askServer(CommandKeys.CLOSE_PRINT_JOB, parameter.toString());
			doLog("Job(" + jobId + ") close print job.");
		} catch (ConnectException e) {
			throw e;
		} finally {
			try {
				if (stream != null) {
					stream.close();
				}
			} catch (IOException e) {
				doLog(e.getMessage());
			}
		}
	}
	
	@Override
	public String getHost() {
		Resources resources = context.getResources();
		return preferences.getString(AndroidActivity.HOST_KEY, resources.getString(R.string.server_host_default));
	}
	
	@Override
	public String getPort() {
		Resources resources = context.getResources();
		return preferences.getString(AndroidActivity.PORT_KEY, resources.getString(R.string.server_port_default));
	}
	
	@Override
	public String getUuid() {
		return preferences.getString(AndroidActivity.UUID_KEY, UUID.randomUUID().toString());
	}
		
	private String serializeServiceName(BluetoothDevice device) {
		StringBuffer printServiceRegister = new StringBuffer(ParameterKeys.PRINT_SERVICE_NAME)
		.append(ParameterKeys.PARAMETER_SEPARATOR)
		.append(device.getAddress());
		return printServiceRegister.toString();

	}
	
	private String serializePrintServiceCategories(BluetoothDevice device) {
		StringBuffer categories = new StringBuffer("" +
				"javax.print.attribute.standard.Copies=javax.print.attribute.standard.CopiesSupported:S:1,9999;" +
				"javax.print.attribute.standard.Finishings=javax.print.attribute.standard.Finishings:E:NONE;" +
				"javax.print.attribute.standard.JobSheets=javax.print.attribute.standard.JobSheets:E:NONE/javax.print.attribute.standard.JobSheets:E:STANDARD;" +
				"javax.print.attribute.standard.Media=sun.print.CustomMediaSizeName:E:CUSTOM/" +
					"javax.print.attribute.standard.MediaSizeName:E:NA_LETTER/" +
					"javax.print.attribute.standard.MediaSizeName:E:NA_LEGAL/" +
					"javax.print.attribute.standard.MediaSizeName:E:NA_8X10;" +
				"javax.print.attribute.standard.NumberUp=javax.print.attribute.standard.NumberUp:I:1/" +
					"javax.print.attribute.standard.NumberUp:I:2/" +
					"javax.print.attribute.standard.NumberUp:I:4/" +
					"javax.print.attribute.standard.NumberUp:I:6/" +
					"javax.print.attribute.standard.NumberUp:I:9/" +
					"javax.print.attribute.standard.NumberUp:I:16;" +
				"javax.print.attribute.standard.OrientationRequested=javax.print.attribute.standard.OrientationRequested:E:PORTRAIT/" +
					"javax.print.attribute.standard.OrientationRequested:E:LANDSCAPE/" +
					"javax.print.attribute.standard.OrientationRequested:E:REVERSE_LANDSCAPE/" +
					"javax.print.attribute.standard.OrientationRequested:E:REVERSE_PORTRAIT;" +
				"javax.print.attribute.standard.PageRanges=javax.print.attribute.standard.PageRanges:S:1,2147483647;" +
				"javax.print.attribute.standard.Sides=javax.print.attribute.standard.Sides:E:ONE_SIDED;" +
				"javax.print.attribute.standard.MediaPrintableArea=javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,209.55,279.4/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,209.55,355.6/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,177.8,266.7/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,93.486,146.756/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,109.714,116.064/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,58.561,64.911/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,113.594,119.944/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,69.85,127.0/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,82.55,127.0/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,95.25,152.4/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,15.875,107.95,168.275/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,120.65,177.8/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,120.65,203.2/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,146.05,203.2/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,196.85,254.0/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,133.35,215.9/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,196.85,304.8/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:3.528,11.642,206.375,297.039/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,141.817,209.903/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,98.425,148.167/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,67.733,104.775/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,45.861,74.083/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,30.692,52.211/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,19.403,37.042/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,208.492,304.8/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,169.333,249.767/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,118.533,175.683/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,81.492,124.883/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,55.386,87.842/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,37.394,61.736/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,24.342,43.744/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,176.389,256.469/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,121.356,182.739/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,84.314,127.706/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,57.15,90.664/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,38.453,63.5/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,25.4,44.803/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,155.575,228.953/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,118.533,323.85/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,107.597,161.925/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,155.575,113.947/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,103.364,219.781/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,74.436,161.925/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,155.575,80.786/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,74.436,113.947/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,107.597,80.786/javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,50.447,80.786/javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,74.436,56.797/javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,33.514,56.797/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,50.447,39.864/javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,21.519,39.864/javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,33.514,27.869/javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,209.55,330.2/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,182.386,245.886/javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,194.381,257.881/javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,116.417,185.914/javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,122.414,197.908/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,131.586,215.9/javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,149.578,233.892/javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,104.422,177.8/javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,104.422,180.975/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,93.486,149.931/javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,93.486,148.167/javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,141.817,200.025/javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,113.594,234.95/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,83.608,204.964/javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,198.614,89.958/javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,98.425,241.3/javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,104.775,146.05/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,92.075,190.5/javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,184.15,98.425/javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,95.25,136.525/javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,95.25,177.8/" +
					"javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,82.903,118.886/javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,48.331,86.078/javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,93.486,199.672/javax.print.attribute.standard.MediaPrintableArea:A:6.35,11.642,209.55,329.847;" +
				"javax.print.attribute.standard.Destination=javax.print.attribute.standard.Destination:;" +
				"javax.print.attribute.standard.Chromaticity=javax.print.attribute.standard.Chromaticity:E:MONOCHROME/javax.print.attribute.standard.Chromaticity:E:COLOR");
		return compressAttributes(categories.toString());
	}
	
	public String compressAttributes(String attributeList) {
		String returnValue = attributeList;
		for (Entry<String, String> entry : getCompressionMap().entrySet()) {
			returnValue = returnValue.replaceAll(entry.getKey(), entry.getValue());
		}
		return returnValue;
	}
	
	public String deCompressAttributes(String attributeList) {
		String returnValue = attributeList;
		for (Entry<String, String> entry : getCompressionMap().entrySet()) {
			returnValue = returnValue.replaceAll(entry.getValue(), entry.getKey());
		}
		return returnValue;
	}

	private  Map<String, String>getCompressionMap() {
		if (compressionMap == null) {
			compressionMap = new LinkedHashMap<String, String>();
			compressionMap.put("javax\\.print\\.attribute\\.standard\\.MediaPrintableArea", "xMPAx");
			compressionMap.put("javax\\.print\\.attribute\\.standard\\.MediaSizeName", "xMSNx");
			compressionMap.put("javax\\.print\\.attribute\\.standard\\.NumberUp", "xNUPx");
			compressionMap.put("javax\\.print\\.attribute\\.standard\\.OrientationRequested", "xORQx");
			compressionMap.put("javax\\.print\\.attribute\\.standard\\.Sides", "xSIDx");
			compressionMap.put("javax\\.print\\.attribute\\.standard\\.PageRanges", "xPRAx");
			compressionMap.put("javax\\.print\\.attribute\\.standard\\.JobSheets", "xJSHx");
			compressionMap.put("javax\\.print\\.attribute\\.standard\\.Finishings", "xFINx");
			compressionMap.put("javax\\.print\\.attribute\\.standard\\.CopiesSupported", "xCSUx");
			compressionMap.put("javax.print.attribute.standard.Chromaticity", "xCHRx");
			compressionMap.put("javax.print.attribute.standard.Destination", "xDSTx");
			compressionMap.put("sun.print.CustomMediaSizeName", "xSCMx");
		}
		return compressionMap;
	}

}