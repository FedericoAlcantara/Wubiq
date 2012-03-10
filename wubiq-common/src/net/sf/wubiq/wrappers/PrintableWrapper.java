package net.sf.wubiq.wrappers;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ImageObserver;
import java.awt.image.renderable.RenderableImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Wraps a printable into a serializable class
 * @author Federico Alcantara
 *
 */
public class PrintableWrapper implements Printable, Serializable {
	private static final long serialVersionUID = 1L;
	private static Log LOG = LogFactory.getLog(PrintableWrapper.class);
	private transient Printable printable;
	private Set<GraphicCommand> graphicCommands;
	private int returnValue = 0;
	private int width;
	private int height;
	private AffineTransform scaleTransform; 
	
	public PrintableWrapper() {
	}

	/**
	 * Creates a wrapper by reading printable values. This should be the 
	 * preferred constructor.
	 * @param printable Printable object to be encapsulated.
	 */
	public PrintableWrapper(Printable printable) {
		this.printable = printable;
	}
	
	public int print(Graphics graphics, PageFormat pageFormat, int pageIndex, double xScale, double yScale) 
			throws PrinterException {
		this.width = new Double(pageFormat.getPaper().getImageableWidth() * xScale).intValue();
		this.height = new Double(pageFormat.getPaper().getImageableHeight() * yScale).intValue();
		this.scaleTransform = new AffineTransform();
		scaleTransform.scale(xScale, yScale);
		return print(graphics, pageFormat, pageIndex);
	}

	@Override
	public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
			throws PrinterException {
		Graphics2D graph = (Graphics2D) graphics;
		if (graphicCommands == null) {
			graphicCommands = new TreeSet<GraphicCommand>();
			setupGraphics(graph, pageFormat);
			GraphicsRecorder graphicsRecorder = new GraphicsRecorder(graphicCommands, graph);
			returnValue = printable.print(graphicsRecorder, new PageFormatWrapper(pageFormat), pageIndex);
		} else {
			AffineTransform newScale = new AffineTransform();
			double x = graph.getDeviceConfiguration().getBounds().getWidth() / (pageFormat.getWidth() + 30); // Arbitrary
			double y = graph.getDeviceConfiguration().getBounds().getHeight() / (pageFormat.getHeight());
			newScale.scale(x, y);
			graph.setTransform(newScale);
			executeGraphics(graph, pageFormat, x, y);
		}
		return returnValue;
	}
	
	/**
	 * Sets the initial parameter for the graphics object.
	 * @param graph Graphics2D object.
	 * @param pageFormat Format of the page to be rendered.
	 */
	private void setupGraphics(Graphics2D graph, PageFormat pageFormat) {
		graph.setTransform(scaleTransform);
		graph.setClip(new Rectangle2D.Double(pageFormat.getImageableX(),
				pageFormat.getImageableY(),
				pageFormat.getImageableWidth(), 
				pageFormat.getImageableHeight()));
		graph.setBackground(Color.WHITE);
		graph.clearRect(0, 0, width, height);
	}


	/**
	 * Iterates through the previously serialized command set.
	 * @param graph Graphics2D object receiving the commands.
	 * @param pageFormat Format of the page to be rendered.
	 */
	private void executeGraphics(Graphics2D graph, PageFormat pageFormat, double xScale, double yScale) {
		Iterator<GraphicCommand> it = graphicCommands.iterator();
		while (it.hasNext()) {
			GraphicCommand graphicCommand = it.next();
			executeMethod(graph, graphicCommand, xScale, yScale);
		}
	}
	
	/**
	 * Validates and perform the graphic command. It unwraps previously wrapped objects
	 * and applies the command to the given Graphics2D object.
	 * @param graph Graphics2D object.
	 * @param graphicCommand Specific graphic command.
	 */
	@SuppressWarnings("rawtypes")
	private void executeMethod(Graphics2D graph, GraphicCommand graphicCommand, double xScale, double yScale) {
		Method method = null;
		Class[] parameterTypes = new Class[]{};
		Object[] parameterValues = new Object[]{};
		if (graphicCommand.getMethodName().equals("setTransform")) {
			AffineTransform tr = (AffineTransform)graphicCommand.getParameters()[0].getParameterValue();
			tr = new AffineTransform(
					tr.getScaleX() * xScale, tr.getShearY() * yScale, 
					tr.getShearX() * xScale, tr.getScaleY() * yScale,  
					tr.getTranslateX() * xScale, tr.getTranslateY() * yScale);
			graphicCommand.getParameters()[0].setParameterValue(tr);
		}
		if (graphicCommand.getParameters().length > 0) {
			parameterTypes = new Class[graphicCommand.getParameters().length];
			parameterValues = new Object[graphicCommand.getParameters().length];
			Object renderingHintValue = null;
			for (int index = 0; index < parameterTypes.length; index++) {
				GraphicParameter parameter = graphicCommand.getParameters()[index];
				parameterValues[index] = parameter.getParameterValue();
				parameterTypes[index] = parameter.getParameterType();
					
				if (parameterTypes[index].equals(ImageWrapper.class)) {
					parameterValues[index] = 
							 ((ImageWrapper)parameter.getParameterValue()).getImage();
					parameterTypes[index] = Image.class;
				} else if (parameterTypes[index].equals(RenderableImageWrapper.class)) {
					parameterValues[index] = 
							((RenderableImageWrapper)parameter.getParameterValue()).getRenderableImage();
					parameterTypes[index] = RenderableImage.class;
				} else if (parameterTypes[index].equals(ImageObserverWrapper.class)) {
					parameterValues[index] = null;
					parameterTypes[index] = ImageObserver.class;
				} else if (parameterTypes[index].equals(ShapeWrapper.class)) {
					parameterTypes[index] = Shape.class;
				} else if (parameterTypes[index].equals(Color.class)) {
					if (graphicCommand.getMethodName().equals("setPaint")) {
						parameterTypes[index] = Paint.class;
					}
				} else if (parameterTypes[index].equals(GlyphVectorWrapper.class)) {
					parameterTypes[index] = GlyphVector.class;
				} else if (parameterTypes[index].equals(RenderingHintWrapper.class)) {
					RenderingHintWrapper hint = (RenderingHintWrapper)parameterValues[index];
					parameterTypes[index] = RenderingHints.Key.class;
					parameterValues[index] = hint.getKey();
					renderingHintValue = hint.getValue();
				} else if (index > 0 && parameterTypes[index - 1].equals(RenderingHints.Key.class)) {
					parameterTypes[index] = Object.class;
					parameterValues[index] = renderingHintValue;
				}
			}
		}
		method = findMethod(graph.getClass(), graphicCommand.getMethodName(), parameterTypes);
		if (method != null) {
			try {
				method.setAccessible(true);
				method.invoke(graph, parameterValues);
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
				throw new RuntimeException(e);
			}
		} else {
			LOG.info("Method not FOUND:" + graphicCommand.getMethodName());
		}
	}
	
	/**
	 * 
	 * @param clazz Class that must contain the given method.
	 * @param name Name of the method.
	 * @param parameterTypes Array of parameter types of the method sought.
	 * @return the method that should be called. Null if the method is not found.
	 */
			
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Method findMethod(Class clazz, String name, Class[] parameterTypes) {
		Method method = null;
		try {
			method = clazz.getDeclaredMethod(name, parameterTypes);
		} catch (SecurityException e) {
			LOG.error(e.getMessage(), e);
		} catch (NoSuchMethodException e) {
			method = null;
		}
		// let's try non declaredMethod
		if (method == null) {
			try {
				method = clazz.getMethod(name, parameterTypes);
			} catch (SecurityException e) {
				LOG.error(e.getMessage(), e);
			} catch (NoSuchMethodException e) {
				method = null;
			}
		}
		
		return method;
	}

}
