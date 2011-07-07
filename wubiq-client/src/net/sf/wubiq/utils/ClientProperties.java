/**
 * 
 */
package net.sf.wubiq.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

import net.sf.wubiq.common.WebKeys;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Collects the value from wubiq-client.properties.
 * This file search is as followed. It is first searched in the folder in the system file where the client is run.
 * If not found then it is look up in the default paths of java class paths.
 * @author Federico Alcantara
 *
 */
public class ClientProperties {
	private static final Log LOG = LogFactory.getLog(ClientProperties.class);
	private static Properties properties;
	
	private ClientProperties() {
	}

	public static String getHost() {
		return get("host", "http://localhost").trim();
	}
	
	public static String getPort() {
		return get("port", "8080").trim();
	}
	
	public static String getApplicationName() {
		return get("application", WebKeys.DEFAULT_APPLICATION_NAME).trim();
	}
	
	public static String getServletName() {
		return get("servlet", WebKeys.DEFAULT_SERVLET_NAME).trim();
	}
	
	public static String getUuid() {
		return get("uuid", UUID.randomUUID().toString()).trim();
	}
	
	private static String get(String key, String defaultValue) {
		String returnValue = getProperties().getProperty(key);
		if (returnValue == null) {
			returnValue = defaultValue;
		}
		return returnValue;
	}

	/**
	 * @return the properties
	 */
	private static Properties getProperties() {
		if (properties == null) {
			try {
				properties = new Properties();
				InputStream stream = null;
				try {
					stream = new FileInputStream("./" + WebKeys.CLIENT_PROPERTIES_FILE_NAME +".properties");
					LOG.info(ClientLabels.get("client.info_client_properties_found_file"));
				} catch (Exception e) {
					LOG.info(ClientLabels.get("client.info_no_client_properties_found_file"));
				}
				if (stream == null) {
					stream = Class.class.getResourceAsStream("/" + WebKeys.CLIENT_PROPERTIES_FILE_NAME + ".properties");
					if (stream == null) {
						throw new IOException("null");
					}
					LOG.info(ClientLabels.get("client.info_client_properties_found"));
				}
				properties.load(stream);
			} catch (Exception e) {
				LOG.info(ClientLabels.get("client.info_no_client_properties_found"));
			}
		}
		return properties;
	}
	
}
