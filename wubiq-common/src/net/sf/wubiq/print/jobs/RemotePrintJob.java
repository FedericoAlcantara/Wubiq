/**
 * 
 */
package net.sf.wubiq.print.jobs;

import java.awt.print.PageFormat;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.attribute.DocAttributeSet;
import javax.print.attribute.PrintJobAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.event.PrintJobAttributeListener;
import javax.print.event.PrintJobListener;

import net.sf.wubiq.print.managers.IRemotePrintJobManager;
import net.sf.wubiq.print.managers.RemotePrintJobManagerType;
import net.sf.wubiq.print.managers.impl.RemotePrintJobManagerFactory;
import net.sf.wubiq.utils.Is;
import net.sf.wubiq.utils.PageableUtils;
import net.sf.wubiq.utils.PdfUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Federico Alcantara
 *
 */
public class RemotePrintJob implements DocPrintJob {
	private static final Log LOG = LogFactory.getLog(RemotePrintJob.class);
	private PrintJobAttributeSet printJobAttributeSet;
	private PrintRequestAttributeSet printRequestAttributeSet;
	private DocAttributeSet docAttributeSet;
	private DocFlavor docFlavor;
	private PrintService printService;
	private String printServiceName;
	private RemotePrintJobStatus status;
	private Object printData;
	private PageFormat pageFormat;
	
	public RemotePrintJob() {
	}

	public RemotePrintJob(PrintService printService) {
		this.printService = printService;
		status = RemotePrintJobStatus.NOT_PRINTED;
		Method getRemoteName;
		try {
			getRemoteName = printService.getClass().getDeclaredMethod("getRemoteName", new Class[]{});
			printServiceName = (String)getRemoteName.invoke(printService, new Object[]{});
		} catch (SecurityException e) {
			LOG.error(e.getMessage(), e);
		} catch (NoSuchMethodException e) {
			LOG.error(e.getMessage(), e);
		} catch (IllegalArgumentException e) {
			LOG.error(e.getMessage(), e);
		} catch (IllegalAccessException e) {
			LOG.error(e.getMessage(), e);
		} catch (InvocationTargetException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	/**
	 * @see javax.print.DocPrintJob#addPrintJobAttributeListener(javax.print.event.PrintJobAttributeListener, javax.print.attribute.PrintJobAttributeSet)
	 */
	@Override
	public void addPrintJobAttributeListener(PrintJobAttributeListener arg0,
			PrintJobAttributeSet arg1) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see javax.print.DocPrintJob#addPrintJobListener(javax.print.event.PrintJobListener)
	 */
	@Override
	public void addPrintJobListener(PrintJobListener arg0) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see javax.print.DocPrintJob#getAttributes()
	 */
	@Override
	public PrintJobAttributeSet getAttributes() {
		return printJobAttributeSet;
	}

	/**
	 * @see javax.print.DocPrintJob#getPrintService()
	 */
	@Override
	public PrintService getPrintService() {
		return printService;
	}

	/**
	 * @see javax.print.DocPrintJob#print(javax.print.Doc, javax.print.attribute.PrintRequestAttributeSet)
	 */
	@Override
	public synchronized void print(Doc doc, PrintRequestAttributeSet printRequestAttributeSet)
			throws PrintException {
		boolean isDirectCommunicationEnabled = false;
		try {
			Method isDirectCommunicationEnabledMethod = printService.getClass().getDeclaredMethod("isDirectCommunicationEnabled", new Class[]{});
			isDirectCommunicationEnabled = (Boolean)isDirectCommunicationEnabledMethod.invoke(printService, new Object[]{});
		} catch (Exception e) {
			isDirectCommunicationEnabled = false;
		}
		
		String uuid = null;
		try {
			Method getUuid = printService.getClass().getDeclaredMethod("getUuid", new Class[]{});
			uuid = (String)getUuid.invoke(printService, new Object[]{});
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		if (!Is.emptyString(uuid)) {
			if (isDirectCommunicationEnabled) {
				printRemote(uuid, doc, printRequestAttributeSet);
			} else {
				printSerialized(uuid, doc, printRequestAttributeSet);
			}
		}
	}

	private void printRemote(String uuid, Doc doc, PrintRequestAttributeSet printRequestAttributeSet) 
			throws PrintException {
		try {
			IRemotePrintJobManager manager = null;
			this.printRequestAttributeSet = printRequestAttributeSet;
			this.docAttributeSet = doc.getAttributes();
			this.docFlavor = doc.getDocFlavor();
			printData = doc.getPrintData();
			if (DocFlavor.INPUT_STREAM.PDF.equals(docFlavor)) {
				docFlavor = DocFlavor.SERVICE_FORMATTED.PAGEABLE;
				printData = PdfUtils.INSTANCE.pdfToPageable((InputStream)doc.getPrintData(), printService, printRequestAttributeSet);
			}
			manager = RemotePrintJobManagerFactory.getRemotePrintJobManager(uuid, RemotePrintJobManagerType.DIRECT_CONNECT);
			manager.addRemotePrintJob(uuid, this);
		} catch (IOException e) {
			throw new PrintException(e);
		} finally {
			
		}
	}
	
	
	/**
	 * Print serialized. This is for compatibility with OLD clients.
	 * @param doc
	 * @param printRequestAttributeSet
	 * @throws PrintException
	 */
	private void printSerialized(String uuid, Doc doc, PrintRequestAttributeSet printRequestAttributeSet) 
			throws PrintException {
		LOG.warn("You are connecting with an OLD client, please update the client to ensure optimal performance");
		try {			
			IRemotePrintJobManager manager = null;
			this.printRequestAttributeSet = printRequestAttributeSet;
			this.docAttributeSet = doc.getAttributes();
			this.docFlavor = doc.getDocFlavor();
			printData = doc.getPrintData();
			update(doc, printRequestAttributeSet);
			manager = RemotePrintJobManagerFactory.getRemotePrintJobManager(uuid, RemotePrintJobManagerType.SERIALIZED);
			manager.addRemotePrintJob(uuid, this);
		} catch (SecurityException e) {
			LOG.error(e.getMessage(), e);
		} catch (IllegalArgumentException e) {
			LOG.error(e.getMessage(), e);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}
	
	/**
	 * Updates this element with the doc information.
	 * @param doc Document to be printed
	 * @param printRequestAttributeSet Request attribute.
	 * @throws PrintException
	 */
	public void update(Doc doc, PrintRequestAttributeSet printRequestAttributeSet)
			throws PrintException {
		this.printRequestAttributeSet = printRequestAttributeSet;
		this.docAttributeSet = doc.getAttributes();
		this.docFlavor = doc.getDocFlavor();
		try {
			printData = doc.getPrintData();
			InputStream transformed = PageableUtils.INSTANCE.getStreamForBytes(printData, getPageFormat(), printRequestAttributeSet);
			printData = transformed;
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	/**
	 * @see javax.print.DocPrintJob#removePrintJobAttributeListener(javax.print.event.PrintJobAttributeListener)
	 */
	@Override
	public void removePrintJobAttributeListener(PrintJobAttributeListener arg0) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see javax.print.DocPrintJob#removePrintJobListener(javax.print.event.PrintJobListener)
	 */
	@Override
	public void removePrintJobListener(PrintJobListener arg0) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Sets a new PrintJob attributeSet.
	 * @param printJobAttributeSet
	 */
	public void setAttributes(PrintJobAttributeSet printJobAttributeSet) {
		this.printJobAttributeSet = printJobAttributeSet;
	}
	
	/**
	 * 
	 * @return A Reader for the input data.
	 * @throws IOException
	 */
	public InputStream getPrintData() throws IOException {
		return (InputStream)printData;
	}
	
	/**
	 * Just the previously saved object.
	 * @return Print data object.
	 */
	public Object getPrintDataObject() {
		return printData;
	}
	

	/**
	 * Sets the document flavor.
	 * @param docFlavor New document flavor to set.
	 */
	public void setDocFlavor(DocFlavor docFlavor){
		this.docFlavor = docFlavor;
	}
	
	/**
	 * 
	 * @return The original doc flavor for the print data.
	 */
	public DocFlavor getDocFlavor() {
		return docFlavor;
	}
	
	/**
	 * 
	 * @return The original attribute set for the document.
	 */
	public DocAttributeSet getDocAttributeSet() {
		return docAttributeSet;
	}
	
	/**
	 * @return The original print request attribute set for the print action.
	 */
	public PrintRequestAttributeSet getPrintRequestAttributeSet() {
		return printRequestAttributeSet;
	}
	
	/**
	 * 
	 * @return The underlying print service name.
	 */
	public String getPrintServiceName() {
		return printServiceName;
	}

	/**
	 * @return Current print job status
	 */
	public RemotePrintJobStatus getStatus() {
		return status;
	}

	/**
	 * Sets a new status for the print job
	 * @param status the status to set.
	 */
	public void setStatus(RemotePrintJobStatus status) {
		this.status = status;
	}

	/**
	 * @return the pageFormat
	 */
	public PageFormat getPageFormat() {
		if (pageFormat == null) {
			pageFormat = PageableUtils.INSTANCE.getPageFormat(printRequestAttributeSet);
			/*
			MediaPrintableArea printableArea = 
					(MediaPrintableArea)PrintServiceUtils.findCategoryAttribute(printRequestAttributeSet, MediaPrintableArea.class);
			Paper paper = new Paper();
			if (printableArea != null) {
				paper.setImageableArea(
						printableArea.getX(MediaPrintableArea.INCH) * 72,
						printableArea.getY(MediaPrintableArea.INCH) * 72,
						printableArea.getWidth(MediaPrintableArea.INCH) * 72,
						printableArea.getHeight(MediaPrintableArea.INCH) * 72);
			} else {
				paper.setSize(612, 828);
				paper.setImageableArea(0, 0, 612, 828);
			}
			pageFormat = new PageFormat();
			pageFormat.setOrientation(PageFormat.PORTRAIT);
			pageFormat.setPaper(paper);
			*/
		}
		return pageFormat;
	}

	/**
	 * @param pageFormat the pageFormat to set
	 */
	public void setPageFormat(PageFormat pageFormat) {
		this.pageFormat = pageFormat;
	}
}
