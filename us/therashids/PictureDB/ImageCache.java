package us.therashids.PictureDB;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import us.therashids.PictureDB.gui.TestFrame;
import us.therashids.PictureDB.picture_manipulation.ImageResizer;


/**
 * stores images that have been loaded (thumbnails etc.) to avoid reloading many times.
 * 
 * @author im
 *
 */
public class ImageCache {
	
	private static ImageCache inst = null;
	DBConnection db;
	ImageResizer rz;
	
	private ImageCache(Component container){
		//TODO maybe have separate caches for different image sizes, eg. just cache 10 full screen images, more thumbnails
		cache = new LinkedHashMap<ImageView, Image>(100, 0.75f, true){
			@Override
			protected boolean removeEldestEntry(
					java.util.Map.Entry<ImageView, Image> eldest) {
				return size() > 50;
			}
		};
		db = DBConnection.getInstance();
		rz = new ImageResizer(container);
	}
	
	public static void setupInst(Component container){
		inst = new ImageCache(container);
	}
	
	public static ImageCache getInst(){
		return inst;
	}
	
	Map<ImageView, Image> cache;
	
	public Image getImage(PictureInfo pic, Container container,
			boolean squared, int width, int height, int border) throws IOException,
			InterruptedException, SQLException {
		ImageView v = new ImageView(squared, width, height, pic, border, rz.quality);
		Image im = cache.get(v);
		if (im == null) {

			if (pic.getId() == -1) {
				BufferedImage ni = new BufferedImage(width, height,
						BufferedImage.TYPE_INT_RGB);
				Graphics2D g = ni.createGraphics();
				g.setColor(Color.BLACK);
				g.fillRect(0, 0, width, height);
				im = ni;
			} else {
				im = rz.getScaledImageAndMakeAllCaches(db, pic, width, height, true, rz.quality).img;
				if (squared) {
					int h = im.getHeight(null);
					int w = im.getWidth(null);
					int size = Math.max(h, w) + 2 * border;
					BufferedImage ni = new BufferedImage(size, size,
							BufferedImage.TYPE_INT_RGB);
					Graphics2D g = ni.createGraphics();
					g.setColor(Color.BLACK);
					g.fillRect(0, 0, size, size);
					g.drawImage(im, (size - w) / 2, (size - h) / 2, w, h, null);
					im = ni;
				}
			}
			v.img = im;
			cache.put(v, im);
		}
		return im;
	}
	
	public Image getThumbnail(PictureInfo pic, Container container) throws IOException, InterruptedException {
		MediaTracker tr = new MediaTracker(container);
		Image im = Toolkit.getDefaultToolkit().getImage(
				pic.getThumbFile().getCanonicalPath());
		tr.addImage(im, 0);
		tr.waitForID(0);
		return im;
	}

}
