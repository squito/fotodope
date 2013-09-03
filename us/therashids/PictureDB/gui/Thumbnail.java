package us.therashids.PictureDB.gui;

import java.awt.Image;
import java.awt.image.BufferedImage;

import us.therashids.PictureDB.ImageView;
import us.therashids.PictureDB.PictureInfo;

/**
 * TODO should be replaced with {@link ImageView}
 * @author im
 *
 */
public class Thumbnail {
	
	BufferedImage thumbnail;
	PictureInfo info;
	
	public Thumbnail(BufferedImage thumbnail, PictureInfo info) {
		super();
		this.thumbnail = thumbnail;
		this.info = info;
	}
	
	
	public PictureInfo getInfo() {
		return info;
	}
	public void setInfo(PictureInfo info) {
		this.info = info;
	}
	public BufferedImage getThumbnail() {
		return thumbnail;
	}
	public void setThumbnail(BufferedImage thumbnail) {
		this.thumbnail = thumbnail;
	}
	
	
}
