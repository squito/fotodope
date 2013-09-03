package us.therashids.PictureDB;

import java.io.File;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Set;

import com.therashids.TagDag.Taggable;

/**
 * holds the meta data for a picture
 * @author Imran Rashid
 *
 */
public class PictureInfo implements Taggable, Serializable {
	
	int id;
	long flickrID;
	int orientation;	//TODO change to enum? right now is EXIF orientation flag
	int width;
	int height;
	
	File pictureFile;
	File thumbFile;
	Calendar timeTaken;
	Set<String> tags;
	
	public static final Comparator<PictureInfo> SORT_ASC_DATE = new Comparator<PictureInfo>(){
		@Override
		public int compare(PictureInfo o1, PictureInfo o2) {
			return o1.getTimeTaken().compareTo(o2.getTimeTaken());
		}
	};
	
	public PictureInfo(int id, File pictureFile, File thumbFile) {
		super();
		this.id = id;
		this.pictureFile = pictureFile;
		this.thumbFile = thumbFile;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public File getPictureFile() {
		return pictureFile;
	}

	public void setPictureFile(File pictureFile) {
		this.pictureFile = pictureFile;
	}

	public File getThumbFile() {
		return thumbFile;
	}

	public void setThumbFile(File thumbFile) {
		this.thumbFile = thumbFile;
	}
	
	public long getFlickrID(){
		return flickrID;
	}
	
	public void setFlickrID(long flickrID){
		this.flickrID = flickrID;
	}
	
	public int getOrientation(){
		return orientation;
	}
	
	public void setOrientation(int orientation){
		this.orientation = orientation;
	}
	
	/**
	 * get the width as the image is intended for display.
	 * note this may be the height of the actual image on disk, if it
	 * has a rotated orientation
	 * @return
	 */
	public int getWidth() {
		return width;
	}
	
	public void setWidth(int width) {
		this.width = width;
	}
	
	/**
	 * get the height as the image is intended for display.
	 * note this may be the width of the actual image on disk, if it
	 * has a rotated orientation
	 * @return
	 */
	public int getHeight() {
		return height;
	}
	
	public void setHeight(int height) {
		this.height = height;
	}
	
	
	public Calendar getTimeTaken(){
		return timeTaken;
	}
	
	public void setTimeTaken(Calendar date){
		this.timeTaken = date;
	}
	
	public Set<String> getTags(){
		if(tags == null){
			try{
				DBConnection.getInstance().getTagsForPicture(this);
			} catch(SQLException e){
				e.printStackTrace();
			}
		}
		return tags;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof PictureInfo){
			PictureInfo p = (PictureInfo)obj;
			return p.getId() == getId();
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return getId();
	}
	
	/**
	 * add tag to this picture.  return false if failed to commit to DB
	 * @param tag
	 * @return
	 */
	public boolean addTag(String tag){
		getTags();
		if(!tags.contains(tag)){
			tags.add(tag);
//			try{
//				DBConnection.getInstance().addTagToPicture(tag, this);
//			}catch(SQLException e){
//				return false;
//			}
		}
		return true;
	}
	
	
//	public boolean removeTag(String tag){
//		getTags();
//		if(tags.contains(tag)){
//			try{
//				DBConnection.getInstance().removeTagFromPicture(tag, this);
//				tags.remove(tag);
//			} catch(SQLException e){
//				return false;
//			}
//		}
//		return true;
//	}
}
