/**
 * 
 */
package us.therashids.PictureDB;

import java.awt.Image;
import java.io.File;
import java.util.Comparator;


/**
 * this represents a physical "view" of an image.  Eg., if the image is resized, made lower quality, etc.
 * 
 * @author im
 *
 */
public class ImageView {
	final boolean squared;
	final int width;
	final int height;
	final int border;
	final int quality;
	final public PictureInfo pic;
	
	//these next two are NOT included in equals, this is just a convenient place to bundle things up together
	public Image img;
	public File imageFile;
	
	public ImageView(boolean squared, int width, int height, PictureInfo pic, int border, int quality) {
		super();
		this.squared = squared;
		this.width = width;
		this.height = height;
		this.pic = pic;
		this.border = border;
		this.quality = quality;
	}
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ImageView) {
			ImageView nv = (ImageView) obj;
			boolean picsMatch = (nv.pic != null && pic != null && nv.pic.getId() == pic.getId()) || (nv.pic == null && pic == null);
			return (nv.squared == squared && nv.width == width && nv.height == height && picsMatch);
		}
		return false;
	}
	@Override
	public int hashCode() {
		return (squared + "-" + width + "-" + height + "-" + pic.getId()).hashCode();
	}
	
	public boolean isSquared() {
		return squared;
	}
	public int getWidth() {
		return width;
	}
	public int getHeight() {
		return height;
	}
	public int getBorder() {
		return border;
	}
	public int getQuality() {
		return quality;
	}
	public PictureInfo getPic() {
		return pic;
	}
	public Image getImg() {
		return img;
	}
	public File getImageFile() {
		return imageFile;
	}
	
	
	public static Comparator<ImageView> ASC_SORT = new AscendingSort();
	public static Comparator<ImageView> DESC_SORT = new Comparator<ImageView>(){
		public int compare(ImageView o1, ImageView o2) {
			return o2.getWidth() - o1.getWidth();
		};
	};
	/**
	 * sort in increasing order by image width
	 * @author im
	 *
	 */
	public static class AscendingSort implements Comparator<ImageView> {
		public int compare(ImageView o1, ImageView o2) {
			return o1.getWidth() - o2.getWidth();
		}
	}

}