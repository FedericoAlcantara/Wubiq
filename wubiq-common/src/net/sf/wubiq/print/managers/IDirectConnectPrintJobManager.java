/**
 * 
 */
package net.sf.wubiq.print.managers;

import javax.print.PrintService;


/**
 * Represents the contract for the Direct connection Management
 * @author Federico Alcantara
 *
 */
public interface IDirectConnectPrintJobManager extends IRemotePrintJobManager {
	/**
	 * Finds the associated direct connector queue handler.
	 * @param queueId Id of the queue to find.
	 * @return DirectConnectorQueue found or newly created. Must never be null.
	 */
	IDirectConnectorQueue directConnector(String queueId);
	
	/**
	 * Gets the queue state.
	 * @return Queue state.
	 */
	boolean isPrinting(PrintService printService);

}
