package us.therashids.PictureDB.picture_manipulation;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import us.therashids.PictureDB.PictureInfo;

public interface ImageResizoo {

	public Image resizeImageMaxSide(File f, int maxSide)
			throws IOException;

	/**
	 * resize an image, such that the longest side is equal to the length given.
	 * 
	 * @param im
	 * @param maxSide
	 * @return the resized image
	 * @throws IOException
	 */
	public Image resizeImageMaxSide(Image im, int maxSide)
			throws IOException;

	/**
	 * 
	 * @param im
	 * @param width
	 * @param height
	 * @return
	 * @throws IOException (problem with MediaTracker)
	 */
	public Image resizeImage(Image im, int width, int height)
			throws IOException;

	/**
	 * rotates an image, based on the EXIF rotation stored in PictureInfo.
	 * 
	 * also useful for unrotated images b/c it returns a BufferedImage
	 * 
	 * @param img
	 * @param info
	 * @return
	 */
	public BufferedImage rotateImage(Image img, PictureInfo info);

}