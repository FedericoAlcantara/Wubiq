/**
 * 
 */
package net.sf.wubiq.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.sf.wubiq.common.WebKeys;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Collects the value from wubiq-server.properties.
 * This file search is as followed. It is first searched in the folder in the system file where the client is run.
 * If not found then it is look up in the default paths of java class paths.
 * @author Federico Alcantara
 *
 */
public class ServerProperties {
	private static final Log LOG = LogFactory.getLog(ServerProperties.class);
	private static Properties properties;
	private static Map<String, String> users;
	private static String realPath;
	
	private ServerProperties() {
	}

	public static String getHsqldbHost() {
		return get("host", "file:");
	}
	
	public static String getHsqldbPort() {
		return get("hsqldb.port", "");
	}
	
	public static String getHsqldbDatabaseName() {
		return get("hsqldb.database", WebKeys.DEFAULT_HSQLDB_DATABASE_NAME);
	}
	
	public static String getHsqldbDbName() {
		return get("hsqldb.dbname", WebKeys.DEFAULT_HSQLDB_DB_ALIAS);
	}
	
	public static String getPrintJobManager() {
		return get("manager", "net.sf.wubiq.print.managers.impl.HsqldbPrintJobManager");
	}

	public static String getRemotePrintJobManager() {
		return get("remoteManager", "net.sf.wubiq.print.managers.impl.DirectConnectPrintJobManager");
	}
	
	/**
	 * Gets the list of users with their privileges. The list is gather from a comma separated
	 * list of elements in the form userid:password.
	 * @return List of users.
	 */
	public static Map<String, String> getUsers() {
		if (users == null) {
			users = new HashMap<String, String>();
			String usersList = get("users", "");
			for (String userPassword : usersList.split("[,;]")) {
				if (userPassword.contains(":")) {
					String[] data = userPassword.split(":");
					String userId = ServerUtils.INSTANCE.normalizedUserId(data[0]);
					if (!Is.emptyString(userId)) {
						users.put(userId, ServerUtils.INSTANCE.normalizedPassword(data[1]));
					}
				}
			}
		}
		return users;
	}
	
	/**
	 * True if the user / password combination is found in the map of users.
	 * @param userId Id of the user to search for.
	 * @param password Password.
	 * @return
	 */
	public static boolean isValidUser(String userId, String password) {
		return getUsers().containsKey(userId) &&
				getUsers().get(userId).equals(ServerUtils.INSTANCE.normalizedPassword(password));
	}

	
	private static String get(String key, String defaultValue) {
		String returnValue = getProperties().getProperty(key);
		if (returnValue == null) {
			returnValue = defaultValue;
		}
		return returnValue;
	}

	/**
	 * It lookup for wubiq-server.properties file.
	 * First it searches in ../tomcat/webapps/wubiq-server/WEB-INF/classes. Then searches in:<br/>
	 * ../tomcat/webapps/wubiq-server/WEB-INF/, ../tomcat/webapps/wubiq-server/WEB-INF/conf<br/>
	 * ../tomcat/webapps/wubiq-server/, ../tomcat/webapps/wubiq-server/conf<br/>
	 * ../tomcat/webapps/, ../tomcat/webapps/conf<br/>
	 * ../tomcat/, ../tomcat/conf<br/>
	 * ../, ../conf<br/>
	 * 
	 * @return the properties.
	 */
	private static Properties getProperties() {
		if (properties == null) {
			try {
				properties = new Properties();
				
				File propertyFile = new File(getRealPath() + "/WEB-INF/classes/" + WebKeys.SERVER_PROPERTIES_FILE_NAME + ".properties");
				if (!propertyFile.exists()) {
					propertyFile = propertyFile.getParentFile();
					for (int i = 0; i < 5; i++) {
						propertyFile = propertyFile.getParentFile();
						File testFile = new File(propertyFile.getParent() + "/" + WebKeys.SERVER_PROPERTIES_FILE_NAME + ".properties");
						if (testFile.exists()) {
							propertyFile = testFile;
							break;
						} else {
							testFile = new File(propertyFile.getParent() + "/conf/" + WebKeys.SERVER_PROPERTIES_FILE_NAME + ".properties");
							if (testFile.exists()) {
								propertyFile = testFile;
								break;
							}		
						}
					}
				}
				if (propertyFile.exists()) {
					properties.load(new FileInputStream(propertyFile));
					LOG.info(ServerLabels.get("server.info_server_properties_found"));
				} else {
					LOG.info(ServerLabels.get("server.info_no_server_properties_found"));
				}
			} catch (FileNotFoundException e) {
				LOG.info(ServerLabels.get("server.info_no_server_properties_found"));
			} catch (IOException e) {
				LOG.info(ServerLabels.get("server.info_no_server_properties_found"));
			}
		}
		return properties;
	}

	public static String getRealPath() {
		return realPath;
	}

	public static void setRealPath(String realPath) {
		ServerProperties.realPath = realPath;
	}
	
}
