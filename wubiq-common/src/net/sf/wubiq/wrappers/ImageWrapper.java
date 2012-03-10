/**
 * 
 */
package net.sf.wubiq.wrappers;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

import javax.imageio.ImageIO;

/**
 * @author Federico Alcantara
 *
 */
public class ImageWrapper implements Serializable {
	private static final long serialVersionUID = 1L;
	private byte[] imageData;
	
	public ImageWrapper(){
		
	}
	
	public ImageWrapper(Image img){
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			ImageIO.write((BufferedImage)img, "png", output);
			imageData = output.toByteArray();
			output.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Image getImage() {
		Image returnValue = null;
		try {
			returnValue = ImageIO.read(new ByteArrayInputStream(imageData));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return returnValue;
	}
}
