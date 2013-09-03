package us.therashids.PictureDB.picture_manipulation;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import us.therashids.PictureDB.DBConnection;
import us.therashids.PictureDB.ImageView;
import us.therashids.PictureDB.PictureInfo;
import us.therashids.PictureDB.gui.TestFrame;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

public class ImageResizer implements ImageResizoo {

	Component c;
	private static int trackerId = 0;
	static int scaling;

	public int quality = 100;

	private static final boolean USE_PYTHON = false;

	/**
	 * discouraged ... only to be used when there really is no gui
	 */
	public ImageResizer() {
		this(new Container());
	}

	public ImageResizer(Component c) {
		this.c = c;
		scaling = Image.SCALE_AREA_AVERAGING;
	}

	public BufferedImage resizeImageMaxSide(PictureInfo info, int maxSide)
			throws IOException {
		Image im = resizeImageMaxSide(info.getPictureFile(), maxSide);
		if (im != null)
			return rotateImage(im, info);
		else
			return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * us.therashids.PictureDB.picture_manipulation.ImageResizoo#resizeImageMaxSide
	 * (java.io.File, int)
	 */
	public Image resizeImageMaxSide(File f, int maxSide) throws IOException {
		MediaTracker mediaTracker = new MediaTracker(c);
		int id = trackerId++;
		Image im = null;
		try {
			im = Toolkit.getDefaultToolkit().getImage(f.getCanonicalPath());
			mediaTracker.addImage(im, id);
			mediaTracker.waitForID(id);
			if (mediaTracker.isErrorID(id)) {
				throw new IOException("tracker error on load");
			}
			mediaTracker = null;
			return resizeImageMaxSide(im, maxSide);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		mediaTracker = null;
		return null;
	}

	/**
	 * resize an image to the given constraints.
	 * 
	 * Note: the dimensions must be given relative to the way the picture is
	 * displayed. Eg., if the file is saved with the dimensions one way, but the
	 * picture is rotated, then the dimensions need to be given for the rotated
	 * version.
	 * 
	 * @param picInfo
	 * @param width
	 * @param height
	 * @return
	 * @throws IOException
	 */
	public static BufferedImage resizeImageSetProportions(PictureInfo picInfo,
			int width, int height) throws IOException {
		BufferedImage im = loadImage(picInfo.getPictureFile());
		if (picInfo.getOrientation() == 6 || picInfo.getOrientation() == 8) {
			im = resizeImage(im, height, width, scaling);
		} else {
			im = resizeImage(im, width, height, scaling);
		}
		return rotateImage(im, picInfo.getOrientation());
	}

	public BufferedImage loadRotate(PictureInfo picInfo) throws IOException {
		Image im = loadImage(picInfo.getPictureFile());
		return rotateImage(im, picInfo);
	}

	public BufferedImage load(File f) throws IOException {
		Image im = loadImage(f);
		return rotateImage(im, 1);
	}

	// Image loadImage(File f) throws IOException {
	// MediaTracker mediaTracker = new MediaTracker(c);
	// int id = trackerId++;
	// Image im = null;
	// try {
	// im = Toolkit.getDefaultToolkit().getImage(f.getCanonicalPath());
	// mediaTracker.addImage(im, id);
	// mediaTracker.waitForID(id);
	// mediaTracker.removeImage(im, id);
	// if(mediaTracker.isErrorID(id)){
	// throw new IOException("tracker error on load");
	// }
	// mediaTracker = null;
	// return im;
	// } catch (InterruptedException e) {
	// e.printStackTrace();
	// }
	// mediaTracker = null;
	// return null;
	//
	// }

	public static BufferedImage loadImage(File f) throws IOException {
		return ImageIO.read(f);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * us.therashids.PictureDB.picture_manipulation.ImageResizoo#resizeImageMaxSide
	 * (java.awt.Image, int)
	 */
	public Image resizeImageMaxSide(Image im, int maxSide) throws IOException {
		int w = im.getWidth(null);
		int h = im.getHeight(null);
		double imageRatio = (double) w / (double) h;
		if (w > h)
			return resizeImage(im, maxSide, (int) (maxSide / imageRatio));
		else
			return resizeImage(im, (int) (imageRatio * h), maxSide);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * us.therashids.PictureDB.picture_manipulation.ImageResizoo#resizeImage
	 * (java.awt.Image, int, int)
	 */
	public Image resizeImage(Image im, int width, int height)
			throws IOException {
		MediaTracker mediaTracker = new MediaTracker(c);
		try {
			int id = trackerId++;
			Image scaledImg;
			scaledImg = im.getScaledInstance(width, height, scaling);
			mediaTracker.addImage(scaledImg, id);
			mediaTracker.waitForID(id);
			mediaTracker.removeImage(scaledImg, id);
			if (mediaTracker.isErrorID(id)) {
				throw new IOException("tracker error on rescale");
			}
			mediaTracker = null;
			return scaledImg;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		mediaTracker = null;
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * us.therashids.PictureDB.picture_manipulation.ImageResizoo#rotateImage
	 * (java.awt.Image, us.therashids.PictureDB.PictureInfo)
	 */
	public BufferedImage rotateImage(Image img, PictureInfo info) {
		return rotateImage(img, info.getOrientation());
	}

	static BufferedImage rotateImage(Image img, int orientation) {
		int w, h;
		w = img.getWidth(null);
		h = img.getHeight(null);
		double w2, h2;
		w2 = ((double) w) / 2.;
		h2 = ((double) h) / 2.;
		BufferedImage bimg = null;
		switch (orientation) {
		// see http://jpegclub.org/exif_orientation.html
		// only cases 6,8 and 1 are tested

		/*
		 * I have no clue why one rotation centers around (h2,h2) and the other
		 * (w2,w2), but thats the only way it works
		 */
		case 6:
			bimg = new BufferedImage(h, w, BufferedImage.TYPE_INT_RGB);
			bimg.createGraphics().drawImage(img,
					AffineTransform.getRotateInstance(Math.PI / 2, h2, h2),
					null);
			break;
		case 8:
			bimg = new BufferedImage(h, w, BufferedImage.TYPE_INT_RGB);
			bimg.createGraphics().drawImage(img,
					AffineTransform.getRotateInstance(-Math.PI / 2, w2, w2),
					null);
			break;
		case 1:
		case 0:
		default:
			if (img instanceof BufferedImage)
				return (BufferedImage) img;
			bimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			bimg.createGraphics().drawImage(img, 0, 0, null);
			break;
		}
		return bimg;
	}

	/**
	 * 
	 * @param img
	 * @param file
	 * @throws IOException
	 */
	public static void saveImage(BufferedImage bimg, File file, float quality)
			throws IOException {
		// this sure doesn't seem like the most efficient way ... so many
		// created objects
		OutputStream out = new FileOutputStream(file);
		// TODO change to ImageIO library? use ImageIOWriteProgressListener
		JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
		JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(bimg);
		param.setQuality(quality, false);
		encoder.setJPEGEncodeParam(param);
		encoder.encode(bimg);
		out.close();
	}

	public static boolean isGoodPictureFile(File f) {
		if (f.canRead() && f.isFile()) {
			String lower = f.getName().toLowerCase();
			if (lower.endsWith(".jpg") || lower.endsWith(".png")
					|| lower.endsWith(".gif")) {
				return true;
			}
		}
		return false;
	}

	/*
	 * see http://forum.java.sun.com/thread.jspa?forumID=20&threadID=522483
	 */

	public static BufferedImage copy(BufferedImage source,
			BufferedImage target, Object interpolationHint) {
		Graphics2D g = target.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolationHint);
		double scalex = (double) target.getWidth() / source.getWidth();
		double scaley = (double) target.getHeight() / source.getHeight();
		AffineTransform at = AffineTransform.getScaleInstance(scalex, scaley);
		g.drawRenderedImage(source, at);
		g.dispose();
		return target;
	}

	public static Image getScaledInstanceAWT(BufferedImage source,
			double factor, int hint) {
		int w = (int) (source.getWidth() * factor);
		int h = (int) (source.getHeight() * factor);
		return source.getScaledInstance(w, h, hint);
	}

	/**
	 * dangerous, only use when the image comes from a rescaled BufferedImage
	 * 
	 * @param image
	 * @param type
	 * @return
	 */
	static BufferedImage toBufferedImageNoLoad(Image image, int type) {
		int w = image.getWidth(null);
		int h = image.getHeight(null);
		BufferedImage result = new BufferedImage(w, h, type);
		Graphics2D g = result.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return result;
	}

	public static BufferedImage resizeImage(BufferedImage source, int width,
			int height, int hint) {
		return toBufferedImage(source.getScaledInstance(width, height, hint));
		// return toBufferedImageNoLoad(source.getScaledInstance(width, height,
		// hint),BufferedImage.TYPE_INT_RGB);
	}

	/**
	 * checks if the database has a cached version of the image at given
	 * dimensions. (or if the original image is already below the given
	 * dimensions.) if not, it creates it and adds it to the db.
	 * 
	 * @param db
	 * @param info
	 * @param width
	 * @param height
	 * @throws SQLException
	 * @throws IOException
	 */
	public File ensureScaledCache(DBConnection db, PictureInfo info, int width,
			int height) throws SQLException, IOException {
		Dimension scaledSize = getScaledDimensions(info, width, height);
		if (scaledSize.getWidth() >= info.getWidth()
				&& scaledSize.getHeight() >= info.getHeight()) {
			return info.getPictureFile();
		}
		ImageView iv = getScaledImage(db, info, width, height, true);
		return iv.imageFile;
	}

	/**
	 * Get a scaled version of the given picture. The returned {@link ImageView}
	 * will always have the {@link ImageView#img} set. It will have
	 * {@link ImageView#imageFile} set if there is already a saved image with
	 * the requested constraints, or if <code>saveFile == true</code>.
	 * <p>
	 * Note that the returned image always has the same proportions as the
	 * original -- the height and width only specify constraints.
	 * 
	 * @param db
	 * @param info
	 * @param width
	 *            the *max* width for the resulting image
	 * @param height
	 *            the *max* height for the resulting image
	 * @param saveFile
	 *            if true, save the image to a file and store in the db
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 */
	public ImageView getScaledImage(DBConnection db, PictureInfo info,
			int width, int height, boolean saveFile) throws SQLException,
			IOException {
		return getScaledImage(db, info, width, height, saveFile, quality);
	}

	public ImageView getScaledImage(DBConnection db, PictureInfo info,
			int width, int height, boolean saveFile, int quality)
			throws SQLException, IOException {
		Dimension scaledSize = getScaledDimensions(info, width, height);
		// if (scaledSize.getWidth() >= info.getWidth()
		// && scaledSize.getHeight() >= info.getHeight()) {
		// return info.getPictureFile();
		// }
		File scaledFile = db.getScaledPictureFile(info, (int) scaledSize
				.getWidth(), (int) scaledSize.getHeight());
		BufferedImage img = null;
		ImageView result = new ImageView(false, (int)scaledSize.getWidth(), (int)scaledSize.getHeight(), info, 0, quality);
		if (scaledFile == null || !ImageResizer.isGoodPictureFile(scaledFile)) {
			if (USE_PYTHON) {
				scaledFile = db.addScaledPicture(info, (int) scaledSize
						.getWidth(), (int) scaledSize.getHeight(), quality);
				PythonResizer
						.makeConstrainedImage(info, scaledSize, scaledFile);
			} else {
				List<ImageView> scaled = db.getScaledPictures(info);
				Collections.sort(scaled, ImageView.ASC_SORT);
				boolean done = false;
				for (ImageView iv : scaled) {
					if (DefaultResizePolicy.acceptableStart(iv, result)) {
						img = loadImage(iv.imageFile);
						img = resizeImage(img, (int) scaledSize.getWidth(),
								(int) scaledSize.getHeight(), scaling);
						done = true;
						break;
					}
				}

				if (!done){
					img = resizeImageSetProportions(info, (int) scaledSize
							.getWidth(), (int) scaledSize.getHeight());
				}
				if (saveFile) {
					scaledFile = db.addScaledPicture(info, (int) scaledSize
							.getWidth(), (int) scaledSize.getHeight(), quality);
					saveImage(img, scaledFile, ((float) quality) / 100);
				}
			}
		} else {
			img = loadImage(scaledFile);
		}
		result.imageFile = scaledFile;
		result.img = img;
		return result;
	}
	
	public ImageView getScaledImageAndMakeAllCaches(DBConnection db, PictureInfo info,
			int width, int height, boolean saveFile, int quality)
			throws SQLException, IOException {
		Dimension scaledSize = getScaledDimensions(info, width, height);
		// if (scaledSize.getWidth() >= info.getWidth()
		// && scaledSize.getHeight() >= info.getHeight()) {
		// return info.getPictureFile();
		// }
		File scaledFile = db.getScaledPictureFile(info, (int) scaledSize
				.getWidth(), (int) scaledSize.getHeight());
		if(scaledFile == null){
			ensureCachedSizes(db, info, TestFrame.cacheSizes);
			return getScaledImage(db, info, width, height, saveFile, quality);
		}
		else{
			BufferedImage img = null;
			ImageView result = new ImageView(false, (int)scaledSize.getWidth(), (int)scaledSize.getHeight(), info, 0, quality);
			img = loadImage(scaledFile);
			result.imageFile = scaledFile;
			result.img = img;
			return result;
		}

	}

	/**
	 * ensures that there are cached versions of all the required sizes. The
	 * idea is that this can be more efficient than creating the cache for each
	 * one independently, because first make large size cache, then medium size
	 * cache can be made from large cache, than small from medium, etc. etc.
	 * 
	 * @param info
	 * @param sizes
	 *            only the width, height, and quality are used
	 * @return
	 * @throws IOException 
	 * @throws SQLException 
	 */
	public java.util.List<ImageView> ensureCachedSizes(DBConnection db, PictureInfo info,
			java.util.List<ImageView> sizes) throws SQLException, IOException {
		System.out.println("getting scaled imgs for " + info.getPictureFile());
		long start = System.currentTimeMillis();
		Collections.sort(sizes, ImageView.DESC_SORT);
		List<ImageView> result = new ArrayList<ImageView>(sizes.size());
		for(ImageView iv: sizes){
			result.add(getScaledImage(db, info, iv.getWidth(), iv.getHeight(), true, iv.getQuality()));
		}
		long end = System.currentTimeMillis();
		
		System.out.println("total time = " + (end - start) + "ms\n\n");

		return result;
	}
	
	/**
	 * an instance of this class decides if its OK to resize an img starting from an already re-sized image
	 * @author im
	 *
	 */
	public interface ResizeImagePolicy {
		/**
		 * this should return true if the original view is an OK starting point
		 * for making the desired view.
		 * @param original
		 * @param desired
		 * @return
		 */
		public boolean acceptableStart(ImageView original, ImageView desired);
	}
	
	public static class DoubleResizePolicy implements ResizeImagePolicy {
		public boolean acceptableStart(ImageView original, ImageView desired) {
			return (original.getQuality() >= desired.getQuality() && original.getWidth() >= 2 * desired.getQuality() && original.getHeight() >= 2 * desired.getHeight());
		}
	}
	
	static ResizeImagePolicy DefaultResizePolicy = new DoubleResizePolicy();
	
	/**
	 * This method returns a buffered image with the contents of an image
	 */
	public static BufferedImage toBufferedImage(Image image) {
		if (image instanceof BufferedImage) {
			return (BufferedImage) image;
		}

		// This code ensures that all the pixels in the image are loaded
		image = new ImageIcon(image).getImage();

		// Determine if the image has transparent pixels; for this method's
		// implementation, see e661 Determining If an Image Has Transparent
		// Pixels
		boolean hasAlpha = false;

		// Create a buffered image with a format that's compatible with the
		// screen
		BufferedImage bimage = null;
		GraphicsEnvironment ge = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		try {
			// Determine the type of transparency of the new buffered image
			int transparency = Transparency.OPAQUE;
			if (hasAlpha) {
				transparency = Transparency.BITMASK;
			}

			// Create the buffered image
			GraphicsDevice gs = ge.getDefaultScreenDevice();
			GraphicsConfiguration gc = gs.getDefaultConfiguration();
			bimage = gc.createCompatibleImage(image.getWidth(null), image
					.getHeight(null), transparency);
		} catch (HeadlessException e) {
			// The system does not have a screen
		}

		if (bimage == null) {
			// Create a buffered image using the default color model
			int type = BufferedImage.TYPE_INT_RGB;
			if (hasAlpha) {
				type = BufferedImage.TYPE_INT_ARGB;
			}
			bimage = new BufferedImage(image.getWidth(null), image
					.getHeight(null), type);
		}

		// Copy image to buffered image
		Graphics g = bimage.createGraphics();

		// Paint the image onto the buffered image
		g.drawImage(image, 0, 0, null);
		g.dispose();

		return bimage;
	}

	/**
	 * this returns the dimensions that an image *would* be, after it was scaled
	 * down proportionally to fit inside the parameters.
	 * 
	 * @param info
	 * @param width
	 * @param height
	 * @return
	 */
	public static Dimension getScaledDimensions(PictureInfo info, int width,
			int height) {
		int imageWidth = info.getWidth();
		int imageHeight = info.getHeight();
		if (imageWidth < width && imageHeight < height) {
			return new Dimension(imageWidth, imageHeight);
		}
		double widthScale = ((double) imageWidth) / width;
		double heightScale = ((double) imageHeight) / height;
		int scaledWidth, scaledHeight;
		if (widthScale > heightScale) {
			scaledWidth = width;
			scaledHeight = (int) (imageHeight / widthScale);
		} else {
			scaledWidth = (int) (imageWidth / heightScale);
			scaledHeight = height;
		}
		return new Dimension(scaledWidth, scaledHeight);
	}

	public long loadTime(File f) throws IOException {
		long start = System.currentTimeMillis();
		Image im = load(f);
		im = rotateImage(im, 1);
		im = rotateImage(resizeImage(im, 1280, 853), 1);
		long stop = System.currentTimeMillis();
		return stop - start;
	}

	public void imageReadTest(File f) throws IOException {

	}

	public static void main(String[] args) throws IOException {
		File testFile = new File(
				"C:\\Documents and Settings\\Imran Rashid\\My Documents\\My Pictures\\2007\\06June\\14\\IMG_3544.JPG");
		ImageResizer resizer = new ImageResizer();
		for (int i = 0; i < 100; i++) {
			long time = resizer.loadTime(testFile);
			System.out.println("load #" + i + " took " + time + " millis");
		}
	}

}
