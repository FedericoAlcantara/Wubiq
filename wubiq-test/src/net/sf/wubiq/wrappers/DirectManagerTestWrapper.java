/**
 * 
 */
package net.sf.wubiq.wrappers;

import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.io.Serializable;

import javax.print.PrintService;
import javax.print.attribute.DocAttributeSet;
import javax.print.attribute.PrintJobAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;

import net.sf.wubiq.clients.DirectPrintManager;
import net.sf.wubiq.clients.remotes.PageableRemote;

/**
 * Wraps a Direct Manager for testing purposes.
 * @author Federico Alcantara
 *
 */
public class DirectManagerTestWrapper extends DirectPrintManager implements Serializable {
	private static final long serialVersionUID = 1L;

	private LocalManagerTestWrapper testManager;
	
	protected DirectManagerTestWrapper(LocalManagerTestWrapper testManager,
			String jobIdString,
			PrintService printService,
			PrintRequestAttributeSet printRequestAttributeSet,
			PrintJobAttributeSet printJobAttributeSet,
			DocAttributeSet docAttributeSet, boolean debugMode, int debugLevel) {
		super(jobIdString, printService, printRequestAttributeSet,
				printJobAttributeSet, docAttributeSet, debugMode, debugLevel);
		this.testManager = testManager;
		this.testManager.getTestData().setDirectManagerCalled(true);
	}
	
	@Override
	protected void printPrintable(String jobId, PrintService printService,
			PrintRequestAttributeSet printRequestAttributeSet,
			PrintJobAttributeSet printJobAttributeSet,
			DocAttributeSet docAttributeSet, Printable printable) {
		super.printPrintable(jobId, printService, printRequestAttributeSet,
				printJobAttributeSet, docAttributeSet, printable);
		testManager.getTestData().setDirectPrintPrintable(true);
	}

	@Override
	protected Class<? extends PageableRemote> getPageableRemoteClass() {
		return PageableRemoteTestWrapper.class;
	}
	
	@Override
	protected String[] getPageableFilteredMethods() {
		return PageableRemoteTestWrapper.FILTERED_METHODS();
	}
	
	@Override
	protected void printPageable(String jobId, PrintService printService,
			PrintRequestAttributeSet printRequestAttributeSet,
			PrintJobAttributeSet printJobAttributeSet,
			DocAttributeSet docAttributeSet, Pageable pageable) {
		((PageableRemoteTestWrapper)pageable).setTestManager(testManager);
		super.printPageable(jobId, printService, printRequestAttributeSet,
				printJobAttributeSet, docAttributeSet, pageable);
		testManager.getTestData().setDirectPrintPageable(true);
	}
		
	
}