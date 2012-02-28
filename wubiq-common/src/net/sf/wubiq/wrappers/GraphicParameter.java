/**
 * 
 */
package net.sf.wubiq.wrappers;

import java.io.Serializable;

/**
 * @author Federico Alcantara
 *
 */
public class GraphicParameter implements Serializable{
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("rawtypes")
	private Class parameterType;
	private Object parameterValue;
	
	@SuppressWarnings("rawtypes")
	public GraphicParameter(Class parameterType, Object parameterValue) {
		this.parameterType = parameterType;
		this.parameterValue = parameterValue;
	}
	
	/**
	 * @return the parameterType
	 */
	@SuppressWarnings("rawtypes")
	public Class getParameterType() {
		return parameterType;
	}
	/**
	 * @param parameterType the parameterType to set
	 */
	@SuppressWarnings("rawtypes")
	public void setParameterType(Class parameterType) {
		this.parameterType = parameterType;
	}
	/**
	 * @return the parameterValue
	 */
	public Object getParameterValue() {
		return parameterValue;
	}
	/**
	 * @param parameterValue the parameterValue to set
	 */
	public void setParameterValue(Object parameterValue) {
		this.parameterValue = parameterValue;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "GraphicParameter [parameterType=" + parameterType
				+ ", parameterValue=" + parameterValue + "]";
	}
}