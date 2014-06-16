/**
 * 
 */
package net.sf.wubiq.proxies;

import java.lang.reflect.Method;
import java.util.UUID;

import net.sf.cglib.proxy.MethodProxy;
import net.sf.wubiq.clients.DirectPrintManager;
import net.sf.wubiq.enums.RemoteCommand;

/**
 * This proxy is located on the remote side, and connects to the server
 * to read the information from it.
 * @author Federico Alcantara
 *
 */
public class ProxyClientSlave extends ProxyBase {
	public DirectPrintManager manager;
	
	/**
	 * Creates the proxy handler for a client in its slave role.
	 * @param manager Printing manager.
	 * @param objectUUID Unique id of the object taken from the server.
	 * @param filtered List of methods' names not handled by this proxy.
	 */
	public ProxyClientSlave(DirectPrintManager manager,
			UUID objectUUID,
			String[] filtered) {
		super(objectUUID, filtered);
		this.manager = manager;
	}
	
	/**
	 * @see net.sf.wubiq.proxies.ProxyMasterBase#intercept(java.lang.Object, java.lang.reflect.Method, java.lang.Object[], net.sf.cglib.proxy.MethodProxy)
	 */
	@Override
	public Object intercept(Object object, Method method, Object[] args,
			MethodProxy methodProxy) throws Throwable {
		if ("manager".equals(method.getName())) {
			return manager;
		}
		return super.intercept(object, method, args, methodProxy);
	}
	
	/**
	 * @see net.sf.wubiq.proxies.ProxyBase#interception(java.lang.Object, java.lang.reflect.Method, java.lang.Object[], net.sf.cglib.proxy.MethodProxy)
	 */
	public Object interception(Object object, Method method, Object[] args,
			MethodProxy methodProxy) throws Throwable {
		return manager.readFromRemote(
				new RemoteCommand(objectUUID(), method.getName(), args));
	}
}
