package us.therashids.PictureDB;

import java.sql.SQLException;
import java.util.*;

public class Album {
	
	int id;
	String flickrId;
	List<PictureInfo> pics;
	String title;
	
	Album(int id, String flickrId, String title){
		this.id = id;
		this.flickrId = flickrId;
		this.title = title;
	}
	
	public static Album getFromDB(int id) throws SQLException{
		DBConnection db = DBConnection.getInstance();
		Album a = db.getAlbum(id);
		if(a == null)
			return null;
		a.pics = db.getPicsInAlbum(id);
		return a;
	}
}
