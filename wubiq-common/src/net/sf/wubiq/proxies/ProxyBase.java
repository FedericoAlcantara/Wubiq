/**
 * 
 */
package net.sf.wubiq.proxies;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 * Defines the minimal implementation for proxied communication for
 * clients, server in their master and slave roles.
 * @author Federico Alcantara
 *
 */
public abstract class ProxyBase implements MethodInterceptor {
	private UUID objectUUID;
	private Set<String> filtered;
	
	public ProxyBase(UUID objectUUID,
			String[] filtered) {
		this.objectUUID = objectUUID;
		this.filtered = new HashSet<String>();
		for (String filter : filtered) {
			this.filtered.add(filter);
		}
	}
	
	/**
	 * @see net.sf.cglib.proxy.MethodInterceptor#intercept(java.lang.Object, java.lang.reflect.Method, java.lang.Object[], net.sf.cglib.proxy.MethodProxy)
	 */
	@Override
	public Object intercept(Object object, Method method, Object[] args,
			MethodProxy methodProxy) throws Throwable {
		if ("objectUUID".equals(method.getName())) {
			return objectUUID;
		} else if (filtered.contains(method.getName())) {
			return methodProxy.invokeSuper(object, args);
		} else {
			return interception(object, method, args, methodProxy);
		}
	}
	
	/**
	 * This is the actual implementation at proxy level. It has
	 * the same meaning as the MethodInterceptor#intercept
	 * @see net.sf.cglib.proxy.MethodInterceptor#intercept(java.lang.Object, java.lang.reflect.Method, java.lang.Object[], net.sf.cglib.proxy.MethodProxy)
	 */
	public abstract Object interception(Object object, Method method, Object[] args,
			MethodProxy methodProxy) throws Throwable;
	
	/**
	 * The unique object identification.
	 * @return The object unique identification.
	 */
	public UUID objectUUID() {
		return objectUUID;
	}
}
