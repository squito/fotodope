package us.therashids.PictureDB;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import us.therashids.PictureDB.PropertyLoader.Props;
import us.therashids.PictureDB.picture_manipulation.ImageResizer;

import com.therashids.TagDag.BadTagTaggablePair;
import com.therashids.TagDag.BasePostgresTagDB;
import com.therashids.TagDag.StringTag;
import com.therashids.TagDag.Tag;
import com.therashids.TagDag.TagAnnotations;
import com.therashids.TagDag.TagDB;
import com.therashids.TagDag.Taggable;
import com.therashids.TagDag.UnrecognizedAnnotations;
import com.therashids.TagDag.UnsupportedTagType;
import com.therashids.TagDag.UnsupportedTaggable;

/**
 * Controls the connection to the database AND the local scaled image directories.
 * 
 * 
 * @author Imran Rashid
 *
 */
public abstract class DBConnection implements TagDB<PictureInfo> {
	
	//TODO change all pictures-with-tags queries to reflect dag
	
	//TODO need to change autocommit for all operations that involve getting an id.
	
	//TODO change DB to use URLs
	
	private static int MAX_PER_SUBDIR;
	
	private static final int needsReviewTagId = 1;
	
	Connection conn;
	TagDB<PictureInfo> tagdb;
	
	//set a default initialization value
	File scaledDir;	
	File currentScaledSubDir;
	int currentScaledSubDirInt = 1;
	int countInCurrentDir;
		
	private static DBConnection globalInstance = null;
	
	private static Class<? extends DBConnection> defaultDB = H2PictureDB.class;
	
	String[][] tableSequences = new String[][]{
			{"picture_files","picture_id","picture_files_id_seq"},
			{"scaled_pictures","scaled_id","scaled_pictures_id_seq"},
			{"string_tags","id","string_tags_id_seq"},
	};
	
	public static DBConnection getInstance(){
		return getInstance(defaultDB);
	}
	
	public static DBConnection getInstance(Class<? extends DBConnection> dbClass){
		MAX_PER_SUBDIR = Integer.parseInt(PropertyLoader.get(Props.IMAGES_PER_DIRECTORY));
		if(globalInstance == null){
			try{
				globalInstance = dbClass.newInstance();
				globalInstance.initialize();
			} catch (SQLException e){
				e.printStackTrace();
				globalInstance = null;
			} catch(ClassNotFoundException e){
				e.printStackTrace();
				globalInstance = null;
			} catch (InstantiationException e) {
				e.printStackTrace();
				globalInstance = null;
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				globalInstance = null;
			}
		}
		return globalInstance;
	}
	
	public abstract void doConnect() throws ClassNotFoundException, SQLException;
	
	public void disconnect() throws SQLException {
		conn.close();
	}
	
	public void initialize() throws ClassNotFoundException, SQLException {
		//get connection
		doConnect();
		tagdb = new BasePostgresTagDB<PictureInfo>(conn);
		tagdb.addTaggableTable(PictureInfo.class, "picture_tags");
		
		//get global properties
		scaledDir = new File(PropertyLoader.get(Props.SCALED_PICTURE_DIRECTORY));
		String t = getGeneralInfoField("current_scaled_subdir");
		if(t == null || t.equals(""))
			setGeneralInfoField("current_scaled_subdir", Integer.toString(currentScaledSubDirInt));
		else
			currentScaledSubDirInt = Integer.parseInt(t);
		currentScaledSubDir = new File(scaledDir, Integer.toString(currentScaledSubDirInt));
		if(currentScaledSubDir.exists())
			countInCurrentDir = currentScaledSubDir.listFiles().length;
		else{
			currentScaledSubDir.mkdirs();
			countInCurrentDir = 0;
		}
	}
	
	/* *************** UTILS ***************************/
	
	
	private boolean isDuplicateKeyError(SQLException ex){
		if(ex.getMessage().startsWith("ERROR: duplicate key"))
			return true;
		else
			return false;
	}
	
	
	/* ************** GENERAL SETUP **********************/
	
	public void setGeneralInfoField(String field, String value) throws SQLException {
		Statement st = conn.createStatement();
		String cmd = "UPDATE general_info SET " + field + " = $$" + value + "$$";
		st.executeUpdate(cmd);
	}
	
	public void setGeneralInfoField(String field, int value) throws SQLException {
		Statement st = conn.createStatement();
		String cmd = "UPDATE general_info SET " + field + " = $$" + value + "$$";
		st.executeUpdate(cmd);
	}
	
	public String getGeneralInfoField(String field) throws SQLException {
		Statement st = conn.createStatement();
		String cmd = "SELECT " + field + " FROM general_info";
		ResultSet rs = st.executeQuery(cmd);
		if(rs.next())
			return rs.getString(1);
		else
			return null;
	}
	
	
	/**
	 * adds a picture to the database, gets its id, updates *info* with it (and returns the
	 * same info object, which has been updated in place).  Also add the tag "needs review"
	 * to the picture.
	 * 
	 * note this is a seperate method from *addPicture* just so that adding the picture
	 * and tag are one transaction.
	 * @param info
	 * @return
	 * @throws SQLException
	 */
	public PictureInfo addPictureToReview(PictureInfo info) throws SQLException {
		conn.setAutoCommit(false);		
		String cmd = "INSERT INTO picture_files (filename, dir, time_taken, width, height, orientation) VALUES (?, ?, ?, ?, ?, ?)";
		PreparedStatement ins = conn.prepareStatement(cmd);
		ins.setString(1, info.getPictureFile().getAbsolutePath());
		ins.setString(2, info.getPictureFile().getParent());
		Calendar date = info.getTimeTaken();
		if(date != null)
			ins.setTimestamp(3, new Timestamp(date.getTimeInMillis()));
		else
			ins.setNull(3, java.sql.Types.TIMESTAMP);
		ins.setInt(4, info.getWidth());
		ins.setInt(5, info.getHeight());
		if(info.getOrientation() != 0)
			ins.setInt(6, info.getOrientation());
		else
			ins.setNull(6,java.sql.Types.INTEGER);
		ins.executeUpdate();
		cmd = "SELECT currval('picture_files_id_seq')";
		Statement getId = conn.createStatement();
		ResultSet rs = getId.executeQuery(cmd);
		if(rs.next()){
			info.id = rs.getInt(1);
			File thumbFile = makeThumbFileName(info.id);
			cmd = "UPDATE picture_files SET thumbFile = $$" + thumbFile.getAbsolutePath() + "$$ WHERE picture_id = " + info.id;
			getId.executeUpdate(cmd);
			info.setThumbFile(thumbFile);
			cmd = "INSERT INTO picture_tags (picture_id, tag_id) VALUES (?,?)";
			ins = conn.prepareStatement(cmd);
			ins.setInt(1, info.id);
			ins.setInt(2, needsReviewTagId);
			ins.executeUpdate();
			conn.commit();
			conn.setAutoCommit(true);
			return info;
		}
		else
			return null;

	}
	
	
	public PictureInfo addPicture(PictureInfo info) throws SQLException {
		String cmd = "INSERT INTO picture_files (filename, dir, time_taken, width, height, orientation) VALUES (?, ?, ?, ?, ?, ?)";
		PreparedStatement ins = conn.prepareStatement(cmd);
		ins.setString(1, info.getPictureFile().getAbsolutePath());
		ins.setString(2, info.getPictureFile().getParentFile().getAbsolutePath());
		Calendar date = info.getTimeTaken();
		if(date != null)
			ins.setTimestamp(3, new Timestamp(date.getTimeInMillis()));
		else
			ins.setNull(3, java.sql.Types.TIMESTAMP);
		ins.setInt(4, info.getWidth());
		ins.setInt(5, info.getHeight());
		if(info.getOrientation() != 0)
			ins.setInt(6, info.getOrientation());
		else
			ins.setNull(6,java.sql.Types.INTEGER);
		ins.executeUpdate();
		cmd = "SELECT currval('picture_files_id_seq')";
		Statement getId = conn.createStatement();
		ResultSet rs = getId.executeQuery(cmd);
		if(rs.next()){
			info.id = rs.getInt(1);
			File thumbFile = makeThumbFileName(info.id);
			cmd = "UPDATE picture_files SET thumbFile = $$" + thumbFile.getAbsolutePath() + "$$ WHERE picture_id = " + info.id;
			getId.executeUpdate(cmd);
			info.setThumbFile(thumbFile);
			return info;
		}
		else
			return null;
	}
	
	public void updatePictureInfo(PictureInfo newInfo, PictureInfo oldInfo) throws SQLException {
		String cmd = "UPDATE picture_files SET (filename, dir, time_taken, width, height, orientation) = (?, ?, ?, ?, ?, ?) WHERE picture_id = ?";
		PreparedStatement upd = conn.prepareStatement(cmd);
		upd.setString(1, newInfo.getPictureFile().getAbsolutePath());
		upd.setString(2, newInfo.getPictureFile().getParent());
		Calendar date = newInfo.getTimeTaken();
		if(date != null)
			upd.setTimestamp(3, new Timestamp(date.getTimeInMillis()));
		else
			upd.setNull(3, java.sql.Types.TIMESTAMP);
		upd.setInt(4, newInfo.getWidth());
		upd.setInt(5, newInfo.getHeight());
		if(newInfo.getOrientation() != 0)
			upd.setInt(6, newInfo.getOrientation());
		else
			upd.setNull(6,java.sql.Types.INTEGER);
		upd.setInt(7, oldInfo.getId());
		upd.executeUpdate();
	}
	
	/**
	 * deletes the record of the picture from the database, without any warnings.
	 * 
	 * @param info
	 * @return list of scaled files
	 * @throws SQLException
	 */
	public List<File> removePictureNoWarnings(PictureInfo info) throws SQLException{
		
		String cmd = "SELECT scaled_filename FROM scaled_pictures WHERE original_id = ?";
		PreparedStatement q = conn.prepareStatement(cmd);
		q.setInt(1, info.getId());
		ResultSet rs = q.executeQuery();
		List<File> scaledFiles = new ArrayList<File>();
		while(rs.next()){
			scaledFiles.add(new File(rs.getString("scaled_filename")));
		}
		
		/* note that flickr_info,picture_tags, and scaled_pictures automatically cascade the delete.
		 * other tables do not (yet) ... not sure if they should or if it should be seperate?
		 * 
		 */
		cmd = "DELETE FROM picture_files WHERE picture_id = ?";
		PreparedStatement st = conn.prepareStatement(cmd);
		st.setInt(1, info.getId());
		st.executeUpdate();
		
		return scaledFiles;
	}
		
	//what if there are too many level 1 subdirectories?  but thats a lot of images ...
	File makeThumbFileName(int id) throws SQLException {
		ensureScaledSubDir();
		countInCurrentDir++;
		return new File(currentScaledSubDir, "t-" + id + ".jpg");
	}
	
	public void addFlickrInfo(PictureInfo info, Calendar sync_time) throws SQLException {
		PreparedStatement st = conn.prepareStatement("INSERT INTO flickr_info (flickr_id, picture_id, last_synced) VALUES (?, ?, ?)");
		st.setLong(1, info.getFlickrID());
		st.setInt(2, info.getId());
		if(sync_time != null)
			st.setTimestamp(3, new Timestamp(sync_time.getTimeInMillis()));
		else
			st.setNull(3, java.sql.Types.TIMESTAMP);
		st.executeUpdate();
	}
	
	
	PreparedStatement addFacebookPhotoStmt; 
	public void addFacebookPhoto(PictureInfo info, long fbPid, long fbUser, long fbAid) throws SQLException{
		if(addFacebookPhotoStmt == null){
			addFacebookPhotoStmt = conn.prepareStatement("INSERT INTO facebook_photos (picture_id, fb_oid, fb_pid, fb_aid) VALUES (?,?,?,?)");
		}
		addFacebookPhotoStmt.setLong(1, info.getId());
		addFacebookPhotoStmt.setLong(2, fbUser);
		addFacebookPhotoStmt.setLong(3, fbPid);
		addFacebookPhotoStmt.setLong(4, fbAid);
		addFacebookPhotoStmt.executeUpdate();
	}
	
	PreparedStatement isInFacebookStmt;
	public boolean isInFacebok(PictureInfo info, long fbUser, long fbAid) throws SQLException {
		if(isInFacebookStmt == null)
			isInFacebookStmt = conn.prepareStatement("SELECT * FROM facebook_photos WHERE picture_id = ? AND fb_oid = ? AND fb_aid = ?");
		isInFacebookStmt.setLong(1, info.getId());
		isInFacebookStmt.setLong(2, fbUser);
		isInFacebookStmt.setLong(3, fbAid);
		
		ResultSet rs = isInFacebookStmt.executeQuery();
		return rs.next();
	}
	
	
	/* *********** GET PICTURES *****************/
	
	private PreparedStatement getPictureByIdStmt;
	
	public PictureInfo getPictureById(int id) throws SQLException {
		if(getPictureByIdStmt == null){
			getPictureByIdStmt = conn.prepareStatement(picture_info_select + " WHERE p.picture_id = ?");
		}
		getPictureByIdStmt.setInt(1, id);
		ResultSet rs = getPictureByIdStmt.executeQuery();
		if(rs.next()){
			return oneResultToPictureInfo(rs);
		}
		return null;
	}
	
	
	private PreparedStatement getPictureBetweenIdStmt;
	
	public List<PictureInfo> getPictureBetweenId(int minId, int maxId) throws SQLException {
		if(getPictureBetweenIdStmt == null){
			getPictureBetweenIdStmt = conn.prepareStatement(picture_info_select + "WHERE p.picture_id > ? AND p.picture_id < ?");
		}
		getPictureBetweenIdStmt.setInt(1, minId);
		getPictureBetweenIdStmt.setInt(2,maxId);
		ResultSet rs = getPictureBetweenIdStmt.executeQuery();
		List<PictureInfo> result = new ArrayList<PictureInfo>();
		while(rs.next()){
			result.add(oneResultToPictureInfo(rs));
		}
		return result;
	}

	
	public List<PictureInfo> getPictureInDirectory(File directory) throws SQLException{
		PreparedStatement st = conn.prepareStatement(picture_info_select + " WHERE dir = ?");
		st.setString(1, directory.getAbsolutePath());
		List<PictureInfo> result = new ArrayList<PictureInfo>();
		ResultSet rs = st.executeQuery();
		while(rs.next()){
			result.add(oneResultToPictureInfo(rs));
		}
		return result;

	}
	
	private PreparedStatement getPicsWithTagStmt;
	
	/**
	 * gets all pictures with a given tag
	 * @param tagId
	 * @return
	 * @throws SQLException
	 */
	public List<PictureInfo> getPicturesWithTag(StringTag tag) throws SQLException{
		if(!tag.inDB()){
			
		}
		if(getPicsWithTagStmt == null){
			getPicsWithTagStmt = conn.prepareStatement(
					"SELECT p.*, f.*" +
					" FROM picture_tags, picture_files p" + 
					" LEFT OUTER JOIN flickr_info f ON (p.picture_id = f.picture_id)" + 
					" WHERE picture_tags.tag_id = ? AND p.picture_id = picture_tags.taggable_id");

		}

		getPicsWithTagStmt.setInt(1, tag.getId());
		List<PictureInfo> result = new ArrayList<PictureInfo>();
		ResultSet rs = getPicsWithTagStmt.executeQuery();
		while(rs.next()){
			result.add(oneResultToPictureInfo(rs));
		}
		return result;
	}
	
	/**
	 * get picture with ANY of the tags in the list
	 * @param tags
	 * @return
	 * @throws SQLException
	 */
	public List<PictureInfo> getPicturesWithTags(Iterable<StringTag> tags)
			throws SQLException {
		Statement st = conn.createStatement();
		StringBuffer cmd = new StringBuffer("SELECT DISTINCT p.*, f.*"
				+ " FROM picture_tags, picture_files p"
				+ " LEFT OUTER JOIN flickr_info f ON (p.picture_id = f.picture_id)"
				+ " WHERE picture_tags.tag_id IN (");
		for(StringTag t: tags){
			cmd.append(t.getId());
			cmd.append(",");
		}
		cmd.deleteCharAt(cmd.length() -1);
		cmd.append(") AND p.picture_id = picture_tags.taggable_id");
		cmd.append(" ORDER BY TIME_TAKEN LIMIT ");
		cmd.append(PropertyLoader.get(Props.IMG_LIMIT));
		List<PictureInfo> result = new ArrayList<PictureInfo>();
		ResultSet rs = st.executeQuery(cmd.toString());
		while(rs.next()){
			result.add(oneResultToPictureInfo(rs));
		}
		return result;
	}
	
		
	private static String picture_info_select = 
		"SELECT p.picture_id, p.filename, p.thumbfile, p.time_taken, p.width, p.height, p.orientation,f.flickr_id " +
		"FROM picture_files p LEFT OUTER JOIN flickr_info f ON (p.picture_id = f.picture_id)";
	
	/**
	 * given a result set, that matches
	 * p.picture_id, p.filename, p.thumbfile, p.time_taken, p.width, p.height, p.orientation,f.flickr_id
	 * it returns a pictureInfo
	 * 
	 * 
	 * @param rs
	 * @return
	 */
	PictureInfo oneResultToPictureInfo(ResultSet rs) throws SQLException{
		//TODO should check for null on more columns ...
		int id = rs.getInt(1);		//BUG -- this should be rs.getInt("picture_id"),but that doesn't work!
		String picFile = rs.getString("filename");
		String thumbFile = rs.getString("thumbfile");
		PictureInfo info = new PictureInfo(id, new File(picFile), new File(thumbFile));
		Calendar timeTaken;
		Timestamp t = rs.getTimestamp("time_taken");
		if(t == null){
			timeTaken = null;
		}
		else{
			timeTaken = new GregorianCalendar();
			timeTaken.setTimeInMillis(t.getTime());
		}
		int width = rs.getInt("width");
		int height = rs.getInt("height");
		int orientation = rs.getInt("orientation");
		info.setWidth(width);
		info.setHeight(height);
		info.setOrientation(orientation);
		long flickrId = rs.getLong("flickr_id");
		if(!rs.wasNull())
			info.setFlickrID(flickrId);
		
		info.setTimeTaken(timeTaken);
		return info;
	}
	
	public List<PictureInfo> getPicturesBetweenDates(Calendar start, Calendar end, boolean includeUndated, boolean highlightsOnly) throws SQLException {
		String where = " WHERE (time_taken > ? AND time_taken < ?)" +
				(includeUndated ? " OR time_taken IS NULL" : "") +
				(highlightsOnly ? " AND f.flickr_id IS NOT NULL" : "");
		//TODO facebook is another "highlight"
		PreparedStatement st = conn.prepareStatement(picture_info_select + where);
		st.setTimestamp(1, new Timestamp(start.getTimeInMillis()));
		st.setTimestamp(2, new Timestamp(end.getTimeInMillis()));
		List<PictureInfo> result = new ArrayList<PictureInfo>();
		ResultSet rs = st.executeQuery();
		while(rs.next()){
			result.add(oneResultToPictureInfo(rs));
		}
		return result;
	}
	
	public List<PictureInfo> getAllPictures() throws SQLException{
		PreparedStatement st = conn.prepareStatement(picture_info_select);		
		List<PictureInfo> result = new ArrayList<PictureInfo>();
		ResultSet rs = st.executeQuery();
		while(rs.next()){
			result.add(oneResultToPictureInfo(rs));
		}
		return result;
	}
	
	
	public File getScaledPictureFile(PictureInfo info, int width, int height) throws SQLException {
		String cmd = "SELECT scaled_filename FROM scaled_pictures " + 
			"WHERE original_id = ? AND width = ? AND height = ?";
		PreparedStatement st = conn.prepareStatement(cmd);
		st.setInt(1, info.getId());
		st.setInt(2,width);
		st.setInt(3, height);
		ResultSet rs = st.executeQuery();
		if(rs.next()){
			String f = rs.getString(1);
			if(f == null || rs.wasNull())
				return null;
			else
				return new File(f);
		}
		else
			return null;
	}
	
	/**
	 * for a given pictureinfo, return the file for a scaled version with the given width, height,
	 * and min quality.  If more than one, return the one with the highest quality.
	 * If there are none, return null.
	 * 
	 * @param info
	 * @param width the width in pixels
	 * @param height the height in pixels
	 * @param minQuality an integer from 0 - 100
	 * @return
	 * @throws SQLException
	 */	
	public File getScaledPictureFile(PictureInfo info, int width, int height, int minQuality) throws SQLException {
		String cmd = "SELECT scaled_filename FROM scaled_pictures " + 
			"WHERE original_id = ? AND width = ? AND height = ? AND quality > ? ORDER BY quality DESC";
		PreparedStatement st = conn.prepareStatement(cmd);
		st.setInt(1, info.getId());
		st.setInt(2,width);
		st.setInt(3, height);
		st.setInt(4,minQuality);
		ResultSet rs = st.executeQuery();
		if(rs.next()){
			String f = rs.getString(1);
			if(f == null || rs.wasNull())
				return null;
			else
				return new File(f);
		}
		else
			return null;
	}

	
	
	
	public File getScaledFilename() throws SQLException{
		String cmd = "SELECT currval('scaled_files_id_seq')";
		Statement getId = conn.createStatement();
		ResultSet rs = getId.executeQuery(cmd);
		if(rs.next()){
			//TODO maybe do insert with this, have them commit together ??
			int id = rs.getInt(1);
			File thumbFile = makeScaledFileName(id);
			return thumbFile;
		}
		else
			return null;

	}
	
	File makeScaledFileName(int id) throws SQLException {
		ensureScaledSubDir();
		countInCurrentDir++;
		return new File(currentScaledSubDir, "s-" + id + ".jpg");
	}
	
	/**
	 * get the directory a new scaled picture file would go in
	 * @return
	 * @throws SQLException
	 */
	void ensureScaledSubDir() throws SQLException{
		while(countInCurrentDir >= MAX_PER_SUBDIR){
			currentScaledSubDirInt++;
			currentScaledSubDir = new File(scaledDir, Integer.toString(currentScaledSubDirInt));
			if(!currentScaledSubDir.exists()){
				if(!currentScaledSubDir.mkdir())//TODO error handling
					throw new RuntimeException("could not create directory " + currentScaledSubDir.toString());
				else
					countInCurrentDir = 0;
			}
			else if (currentScaledSubDir.isDirectory()){
				countInCurrentDir = currentScaledSubDir.list().length;
			}
			else{
				//already exists, and its not a directory -- need to choose another file.
				countInCurrentDir = MAX_PER_SUBDIR + 1;
			}
		}
		setGeneralInfoField("current_scaled_subdir", currentScaledSubDirInt);
	}
	
	public File addScaledPicture(PictureInfo originalInfo, int width, int height, int quality) throws SQLException{
		conn.setAutoCommit(false);
		String cmd = "INSERT INTO scaled_pictures (original_id, width, height, quality) VALUES (?,?,?,?)";
		PreparedStatement ins = conn.prepareStatement(cmd);
		ins.setInt(1, originalInfo.getId());
		ins.setInt(2,width);
		ins.setInt(3, height);
		ins.setInt(4, quality);
		ins.executeUpdate();
		cmd = "SELECT currval('scaled_pictures_id_seq')";
		Statement getId = conn.createStatement();
		ResultSet rs = getId.executeQuery(cmd);
		if(rs.next()){
			int scaledId = rs.getInt(1);
			File scaledFile = makeScaledFileName(scaledId);
			cmd = "UPDATE scaled_pictures SET scaled_filename = $$" + scaledFile.getAbsolutePath() + "$$ WHERE scaled_id = " + scaledId;
			getId.executeUpdate(cmd);
			conn.commit();
			conn.setAutoCommit(true);
			return scaledFile;
		}
		else
			conn.setAutoCommit(true);
			return null;
	}
	
	PreparedStatement addSclStmt;
	public void addScaledPicture(PictureInfo originalInfo, File scaledFile, int width, int height, int quality) throws SQLException {
		if(addSclStmt == null){
			addSclStmt = conn.prepareStatement("INSERT INTO scaled_pictures (original_id, scaled_filename, width, height, quality) VALUES (?, ?, ?, ?, ?)");
		}
		addSclStmt.setInt(1, originalInfo.getId());
		addSclStmt.setString(2, scaledFile.getAbsolutePath());
		addSclStmt.setInt(3, width);
		addSclStmt.setInt(4, height);
		addSclStmt.setInt(5, quality);
		addSclStmt.executeUpdate();
	}
	
	
	
	private static PreparedStatement getScaledPicStmt;
	public List<ImageView> getScaledPictures(PictureInfo originalInfo) throws SQLException {
		if(getScaledPicStmt == null){
			getScaledPicStmt = conn.prepareStatement("SELECT scaled_filename, width, height, quality FROM scaled_pictures WHERE original_id = ?");
		}
		getScaledPicStmt.setInt(1,originalInfo.getId());
		List<ImageView> result = new ArrayList<ImageView>();
		ResultSet rs = getScaledPicStmt.executeQuery();
		while(rs.next()){
			ImageView iv = new ImageView(false, rs.getInt("width"), rs.getInt("height"), originalInfo, 0, rs.getInt("quality"));
			iv.imageFile = new File(rs.getString("scaled_filename"));
			result.add(iv);
		}
		return result;
	}
	
	
	private static PreparedStatement removeScaledPicStmt;
	
	/**
	 * remove any scaled pictures associated with the given picture.
	 * 
	 * deletes the scaled picture files and removes bindings from db.
	 * 
	 * @param originalInfo
	 * @throws SQLException
	 */
	public void removeScaledPicture(PictureInfo originalInfo) throws SQLException {
		
		for(ImageView iv: getScaledPictures(originalInfo)){
			iv.imageFile.delete();
		}
		
		if(removeScaledPicStmt == null){
			removeScaledPicStmt = conn.prepareStatement("DELETE FROM scaled_pictures WHERE original_id = ?");
		}
		removeScaledPicStmt.setInt(1, originalInfo.getId());
		removeScaledPicStmt.executeUpdate();
	}
	
	/**
	 * get a list of all directories in the database.
	 * @return
	 */
	public HashMap<File, Boolean> getMappedDirectories() throws SQLException {
		Statement st = conn.createStatement();
		String cmd = "SELECT dir, all_sub FROM directories";
		ResultSet rs = st.executeQuery(cmd);
		HashMap<File, Boolean> result = new HashMap<File, Boolean>();
		while(rs.next()){
			String dirname = rs.getString(1);
			boolean all_sub = rs.getBoolean(2);
			result.put(new File(dirname), all_sub);
		}
		return result;
	}
	
	/**
	 * Tell the database that a directory has been indexed.
	 * @param directory
	 * @param allSub true if all subdirectories have been indexed too
	 * @param lastChecked can be null
	 * @throws SQLException
	 */
	public void addDirectoryToDB(File directory, boolean allSub, Calendar lastChecked) throws SQLException {
		String cmd = "INSERT INTO directories (dir, all_sub, last_checked) VALUES (?,?,?)";
		PreparedStatement ins = conn.prepareStatement(cmd);
		ins.setString(1, directory.getAbsolutePath());
		ins.setBoolean(2, allSub);
		if(lastChecked != null)
			ins.setTimestamp(3, new Timestamp(lastChecked.getTimeInMillis()));
		else
			ins.setNull(3, java.sql.Types.TIMESTAMP);
		ins.executeUpdate();
	}
	
	/**
	 * returns true if the directory has already been added to the database
	 * @param directory
	 * @return
	 */
	public boolean isDirectoryInDB(File directory) throws SQLException {
		String cmd = "SELECT * FROM DIRECTORIES WHERE dir = ?";
		PreparedStatement st = conn.prepareStatement(cmd);
		st.setString(1, directory.getAbsolutePath());
		ResultSet rs = st.executeQuery();
		if(rs.next())
			return true;
		else
			return false;
	}
	
	
	//*******Picture Collections **********
	
	public List<PictureInfo> getPicsInAlbum(int albumId) throws SQLException {
		List<PictureInfo> result = new ArrayList<PictureInfo>();
		String cmd = "SELECT p.picture_id, p.filename, p.thumbfile, p.time_taken, p.width, p.height, p.orientation,f.flickr_id " +
		"FROM albums, picture_files p LEFT OUTER JOIN flickr_info f ON (p.picture_id = f.picture_id)" + 
		"WHERE albums.album_id = ? AND albums.picture_id = p.picture_id";
		PreparedStatement st = conn.prepareStatement(cmd);
		st.setInt(1, albumId);
		ResultSet rs = st.executeQuery();
		while(rs.next()){
			result.add(oneResultToPictureInfo(rs));
		}
		return result;
	}
	
	public Album getAlbum(int albumId) throws SQLException {
		String cmd = "SELECT album_id, flickr_id, title FROM album_info WHERE album_id = ?";
		PreparedStatement st = conn.prepareStatement(cmd);
		st.setInt(1, albumId);
		ResultSet rs = st.executeQuery();
		if(rs.next()){
			return oneAlbum(rs);
		}
		else
			return null;
	}
	
	public Album getAlbumByFlickrId(String flickrId) throws SQLException{
		String cmd = "SELECT album_id, flickr_id, title FROM album_info WHERE flickr_id = ?";
		PreparedStatement st = conn.prepareStatement(cmd);
		st.setString(1, flickrId);
		ResultSet rs = st.executeQuery();
		if(rs.next()){
			return oneAlbum(rs);
		}
		else
			return null;
	}
	
	Album oneAlbum(ResultSet rs) throws SQLException {
		int albumId = rs.getInt("album_id");
		String flickrId = rs.getString("flickr_id");
		String title = rs.getString("title");
		if(rs.wasNull())
			title = null;
		return new Album(albumId, flickrId, title);
	}
	
	
	/**
	 * 
	 * @param flickrId
	 * @param title
	 * @return
	 * @throws SQLException
	 */
	public Album addAlbumFromFlickr(String flickrId, String title) throws SQLException{
		String cmd = "INSERT INTO album_info (flickr_id, title) VALUES (?,?)";
		PreparedStatement st = conn.prepareStatement(cmd);
		st.setString(1, flickrId);
		st.setString(2, title);
		st.executeUpdate();
		cmd = "SELECT currval('album_id_seq')";
		Statement getId = conn.createStatement();
		ResultSet rs = getId.executeQuery(cmd);
		if(rs.next()){
			int albumId = rs.getInt(1);
			return new Album(albumId, flickrId, title);
		}
		else
			return null;

	}
	
	
	//************* TAG RELATED ******************
	
	
	
	public List<StringTag> getAllTags() throws SQLException{
		String cmd = "SELECT tag_name, tag_id FROM all_tags";
		Statement st = conn.createStatement();
		ResultSet r = st.executeQuery(cmd);
		List<StringTag> result = new ArrayList<StringTag>();
		while(r.next()){
			StringTag t = new StringTag(r.getString(1));
			t.setId(r.getInt(2));
			result.add(t);
		}
		return result;
	}
	
	public void addParentChildTagRelationship(Tag parent, Tag child)
			throws SQLException, UnsupportedTagType {
		tagdb.addParentChildTagRelationship(parent, child);
	}

	public void addTagToDB(Tag tg) throws SQLException, UnsupportedTagType {
		tagdb.addTagToDB(tg);
	}

	public void addTagToTaggable(Tag tg, PictureInfo tgble, TagAnnotations annot)
			throws SQLException, UnsupportedTagType, UnsupportedTaggable,
			BadTagTaggablePair, UnrecognizedAnnotations {
		tagdb.addTagToTaggable(tg, tgble, annot);
	}

	public TagAnnotations getAnnotationsForTagTaggable(Tag tg, PictureInfo tgble)
			throws SQLException {
		return tagdb.getAnnotationsForTagTaggable(tg, tgble);
	}

	public Iterable<Tag> getChildTags(Tag t) throws SQLException {
		return tagdb.getChildTags(t);
	}

	public boolean getOrAddTag(Tag t) throws SQLException, UnsupportedTagType {
		return tagdb.getOrAddTag(t);
	}

	public <T extends Tag> Iterable<T> getTagsOfType(Class<T> cls)
			throws SQLException, UnsupportedTagType {
		return tagdb.getTagsOfType(cls);
	}

	@SuppressWarnings("unchecked")
	public <T extends PictureInfo> Iterable<T> getWithTag(Tag tg,
			Class<T> tgblClass) throws SQLException {
		if(tg instanceof StringTag && PictureInfo.class.isAssignableFrom(tgblClass))
			return (Iterable<T>) getPicturesWithTag((StringTag) tg);
		return tagdb.getWithTag(tg, tgblClass);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends PictureInfo> Iterable<T> getWithTags(Iterable<? extends Tag> tags,
			Class<T> tgblClass) throws SQLException {
		if(PictureInfo.class.isAssignableFrom(tgblClass))
			return (Iterable<T>) getPicturesWithTags((Iterable<StringTag>) tags);
		return tagdb.getWithTags(tags, tgblClass);
	}


	public void removeParentChildRelationship(Tag parent, Tag child)
			throws SQLException {
		tagdb.removeParentChildRelationship(parent, child);
	}

	public void removeTag(Tag tg) throws SQLException {
		tagdb.removeTag(tg);
	}

	public void setTagRootLevel(Tag tag, boolean isRootLevel)
			throws SQLException, UnsupportedTagType {
		tagdb.setTagRootLevel(tag, isRootLevel);
	}
	
	public void removeTagFromPicture(String tag, PictureInfo pic) throws SQLException{
		String cmd = "DELETE FROM picture_tags WHERE picture_id = ? AND tag = ?";
		PreparedStatement del = conn.prepareStatement(cmd);
		del.setInt(1,pic.id);
		del.setString(2, tag);
		del.execute();
	}
	
	/**
	 * update the set of tags of the given picture w/ whats in the db.
	 * 
	 * Currently totally broken
	 * @param pic
	 * @throws SQLException
	 */
	public void getTagsForPicture(PictureInfo pic) throws SQLException {
		String cmd = "SELECT tag_id FROM picture_tags WHERE taggable_id = ?";
		PreparedStatement s = conn.prepareStatement(cmd);
		s.setInt(1,pic.id);
		ResultSet r = s.executeQuery();
		Set<String> result = new HashSet<String>();
		while(r.next()){
			result.add(Integer.toString(r.getInt(1)));
		}
		pic.tags = result;
	}
	
	private PreparedStatement getTagIdStmt;
	
	public void getOrAddTag(StringTag tag) throws SQLException {
		if (tag.getChildTags() != null) {
			for (Tag subTag : tag.getChildTags()) {
				getOrAddTag((StringTag)subTag);
			}
		}
		if(getTagIdStmt == null){
			getTagIdStmt = conn.prepareStatement("SELECT tag_id FROM all_tags WHERE tag_name = ?");
		}
		getTagIdStmt.setString(1,tag.getKey());
		ResultSet rs = getTagIdStmt.executeQuery();
		if(rs.next()){
			tag.setId(rs.getInt("tag_id"));
		}
		else{
			addNewTag(tag);
		}
	}
	
	private static PreparedStatement addChildTagStmt;
	
	/**
	 * add one StringTag as a child of another one
	 * 
	 * TODO should check that there are no cycles -- right now trusting the user!
	 * @param group
	 * @param subgroup
	 * @throws SQLException
	 */
	public void addChildTagStmt(StringTag tag, StringTag child) throws SQLException {
		if(addChildTagStmt == null){
			addChildTagStmt = conn.prepareStatement("INSERT INTO tag_dag (tag_id, child_id) VALUES (?,?)");
		}
		addChildTagStmt.setInt(1,tag.getId());
		addChildTagStmt.setInt(2, child.getId());
		try{
			addChildTagStmt.executeUpdate();
		} catch(SQLException ex){
			if(isDuplicateKeyError(ex))	//this mapping is already in the table
				return;
			else
				throw ex;
		}
	}
	
	private PreparedStatement getTagParentsStmt;
	
	public List<Integer> getStringTagParents(StringTag tag) throws SQLException {
		if(getTagParentsStmt == null){
			getTagParentsStmt = conn.prepareStatement("SELECT tag_id FROM tag_dag WHERE child_id = ?");
		}
		getTagParentsStmt.setInt(1, tag.getId());
		ResultSet rs = getTagParentsStmt.executeQuery();
		List<Integer> result = new ArrayList<Integer>();
		while(rs.next()){
			result.add(rs.getInt("tag_id"));
		}
		return result;
	}
	
	
	private PreparedStatement getTagChildrenStmt;

	public List<Integer> getStringTagChildren(StringTag tag) throws SQLException {
		if(getTagChildrenStmt == null){
			getTagChildrenStmt = conn.prepareStatement("SELECT child_id FROM tag_dag WHERE tag_id = ?");
		}
		getTagChildrenStmt.setInt(1, tag.getId());
		ResultSet rs = getTagChildrenStmt.executeQuery();
		List<Integer> result = new ArrayList<Integer>();
		while(rs.next()){
			result.add(rs.getInt("child_id"));
		}
		return result;
	}
	
	
	
	
	private PreparedStatement getTagFromIdStmt;
	
	public StringTag getStringTagFromId(int id) throws SQLException {
		if(getTagFromIdStmt == null){
			getTagFromIdStmt = conn.prepareStatement("SELECT tag_name FROM all_tags WHERE tag_id = ?");
		}
		getTagFromIdStmt.setInt(1,id);
		ResultSet rs = getTagFromIdStmt.executeQuery();
		if(rs.next()){
			return new StringTag(rs.getString("tag_name"));
		}
		else
			return null;
	}
	
	public int addNewTag(StringTag tag) throws SQLException{
		String cmd = "INSERT INTO all_tags (tag_name) VALUES (?)";
		PreparedStatement ins = conn.prepareStatement(cmd);
		ins.setString(1, tag.getKey());
		ins.execute();
		
		cmd = "SELECT currval('tag_id_seq')";
		Statement getId = conn.createStatement();
		ResultSet rs = getId.executeQuery(cmd);
		if(rs.next()){
			tag.setId(rs.getInt(1));
			return tag.getId();
		}
		else
			return -1;

	}
	
	public Set<File> getDirectories() throws SQLException{
		String cmd = "SELECT dir FROM directories";
		Statement st = conn.createStatement();
		ResultSet rs = st.executeQuery(cmd);
		Set<File> dirs = new HashSet<File>();
		while(rs.next()){
			String fileName = rs.getString(1);
			dirs.add(new File(fileName));
		}
		return dirs;
	}
	
	/**
	 * get the number of pictures in a directory, including all child directories
	 * 
	 * @param dir
	 * @return
	 * @throws SQLException
	 */
	public int getNumPicsInDirectory(File dir) throws SQLException {
		//can't figure out to how to do this with LIKE query -- not sure how to escape the filename
		String cmd = "SELECT COUNT(*) FROM picture_files WHERE dir = ?";
		PreparedStatement st = conn.prepareStatement(cmd);
		st.setString(1, dir.getAbsolutePath());
		ResultSet rs = st.executeQuery();
		int total = 0;
		if(rs.next()){
			total = rs.getInt("count");
		}
		for(File child: getSubDirectories(dir)){
			total += getNumPicsInDirectory(child);
		}
		return total;
	}
	
	/**
	 * removes this directory, and all of its children
	 * 
	 * @param dir
	 * @throws SQLException
	 */
	public void removeDirectory(File dir) throws SQLException {
		Set<File> children = getSubDirectories(dir);
		for(File child: children){
			removeDirectory(child);
		}
		String cmd = "DELETE FROM directories WHERE dir = ?";
		PreparedStatement del = conn.prepareStatement(cmd);
		del.setString(1,dir.getAbsolutePath());
		del.executeUpdate();
	}
	
	Set<File> getSubDirectories(File dir) throws SQLException {
		String cmd = "SELECT dir FROM directories WHERE parent = ?";
		PreparedStatement sel = conn.prepareStatement(cmd);
		sel.setString(1, dir.getAbsolutePath());
		ResultSet rs = sel.executeQuery();
		Set<File> result = new HashSet<File>();
		while(rs.next()){
			result.add(new File(rs.getString("dir")));
		}
		return result;
	}
	
	void setParentDir(File dir, File parentDir) throws SQLException {
		String cmd = "UPDATE directories SET parent = ? WHERE dir = ?";
		PreparedStatement upd = conn.prepareStatement(cmd);
		upd.setString(1, parentDir.getAbsolutePath());
		upd.setString(2, dir.getAbsolutePath());
		upd.executeUpdate();
	}
	
	/* ************* PEOPLE RELATED ********************/
	
	
	private PreparedStatement addPersonInPictureStmt;
	
	public void addPersonInPicture(Person p, PictureInfo info) throws SQLException{
		if(addPersonInPictureStmt == null){
			addPersonInPictureStmt = conn.prepareStatement("INSERT INTO people_in_pictures (picture_id, people_id) VALUES (?,?)");
		}
		addPersonInPictureStmt.setInt(1, info.getId());
		addPersonInPictureStmt.setInt(2, p.getId());
		try{
			addPersonInPictureStmt.executeUpdate();
		} catch(SQLException ex){
			if(isDuplicateKeyError(ex))	//this mapping is already in the table
				return;
			else
				throw ex;
		}
	}
	
	private PreparedStatement addGroupInPictureStmt;
	
	public void addGroupInPicture(PeopleGroup g, PictureInfo info) throws SQLException{
		if(addGroupInPictureStmt == null){
			addGroupInPictureStmt = conn.prepareStatement("INSERT INTO groups_in_pictures (picture_id, group_id) VALUES (?,?)");
		}
		addGroupInPictureStmt.setInt(1, info.getId());
		addGroupInPictureStmt.setInt(2, g.getId());
		try{
			addGroupInPictureStmt.executeUpdate();
		} catch(SQLException ex){
			if(isDuplicateKeyError(ex))	//this mapping is already in the table
				return;
			else
				throw ex;
		}
	}
	
	
	// Q: should I allow multiple photographers?  right now there are no uniqueness constraints
	//		on the table at all ...
	private PreparedStatement addPhotographerStmt;
	
	public void addPhotographer(Person p, PictureInfo info) throws SQLException{
		if(addPhotographerStmt == null){
			addPhotographerStmt = conn.prepareStatement("INSERT INTO photographer (picture_id, people_id) VALUES (?,?)");
		}
		addPhotographerStmt.setInt(1, info.getId());
		addPhotographerStmt.setInt(2, p.getId());
		addPhotographerStmt.executeUpdate();
	}
	

	
	
	private PreparedStatement getPicsWithPeopleStmt;
	
	/**
	 * gets all pictures with a given group of people in it
	 * @param tagId
	 * @return
	 * @throws SQLException
	 */
	public List<PictureInfo> getPicturesWithPeople(Person person) throws SQLException{
		if(!person.inDB()){
			
		}
		if(getPicsWithPeopleStmt == null){
			getPicsWithPeopleStmt = conn.prepareStatement(
					"SELECT p.picture_id, p.filename, p.thumbfile, p.time_taken, p.width, p.height, p.orientation,f.flickr_id" +
					" FROM people_in_pictures, picture_files p " + 
					" LEFT OUTER JOIN flickr_info f ON (p.picture_id = f.picture_id)" + 
					" WHERE people_in_pictures.people_id = ? AND p.picture_id = people_in_pictures.picture_id");

		}
		
		getPicsWithPeopleStmt.setInt(1, person.getId());
		List<PictureInfo> result = new ArrayList<PictureInfo>();
		ResultSet rs = getPicsWithPeopleStmt.executeQuery();
		while(rs.next()){
			result.add(oneResultToPictureInfo(rs));
		}
		return result;
	}

	
	
	private PreparedStatement getPicsWithGroupStmt;
	
	/**
	 * gets all pictures with a given group of people in it
	 * @param tagId
	 * @return
	 * @throws SQLException
	 */
	public List<PictureInfo> getPicturesWithPeopleGroup(PeopleGroup group) throws SQLException{
		if(!group.inDB()){

		}
		if(getPicsWithGroupStmt == null){
			getPicsWithGroupStmt = conn.prepareStatement(
					"SELECT p.picture_id, p.filename, p.thumbfile, p.time_taken, p.width, p.height, p.orientation,f.flickr_id" +
					" FROM groups_in_pictures, picture_files p " + 
					" LEFT OUTER JOIN flickr_info f ON (p.picture_id = f.picture_id)" + 
					" WHERE groups_in_pictures.group_id = ? AND p.picture_id = groups_in_pictures.picture_id");

		}
		
		getPicsWithGroupStmt.setInt(1, group.getId());
		List<PictureInfo> result = new ArrayList<PictureInfo>();
		ResultSet rs = getPicsWithGroupStmt.executeQuery();
		while(rs.next()){
			result.add(oneResultToPictureInfo(rs));
		}
		return result;
	}
	
	
	private static PreparedStatement getPersonByName;
	
	/**
	 * ensures that the given person is in the DB.  updates the id of the given
	 * person to agree with the DB.
	 * 
	 * @param p
	 * @throws SQLException
	 */
	public void getOrAddPerson(Person p) throws SQLException {
		if(getPersonByName == null){
			getPersonByName = conn.prepareStatement("SELECT (people_id) FROM people WHERE first_name = ? AND last_name = ?");
		}
		getPersonByName.setString(1, p.getFirstName());
		getPersonByName.setString(2,p.getLastName());
		ResultSet rs = getPersonByName.executeQuery();
		if(rs.next()){
			p.setId(rs.getInt(1));
		}
		else{
			addPerson(p);
		}
	}
	
	
	private static PreparedStatement addPersonStmt;
	
	public void addPerson(Person p) throws SQLException {
		if(addPersonStmt == null){
			addPersonStmt = conn.prepareStatement("INSERT INTO people (name, first_name, last_name) VALUES (?,?,?)");
		}
		addPersonStmt.setString(1, p.toString());
		addPersonStmt.setString(2, p.getFirstName());
		addPersonStmt.setString(3, p.getLastName());
		addPersonStmt.executeUpdate();
		
		String cmd = "SELECT currval('people_id_seq')";
		Statement getId = conn.createStatement();
		ResultSet rs = getId.executeQuery(cmd);
		if(rs.next()){
			p.setId(rs.getInt(1));
		}
	}
	
	private static PreparedStatement getGroup;
	
	public void getOrAddGroup(PeopleGroup g) throws SQLException {
		for(Person p: g.getPeople()){
			getOrAddPerson(p);
		}
		for(PeopleGroup subgroup: g.getSubgroups()){
			getOrAddGroup(subgroup);
		}
		if(getGroup == null){
			getGroup = conn.prepareStatement("SELECT people_group_id, group_desc FROM people_group WHERE group_name = ?");
		}
		getGroup.setString(1,g.getName());
		
		ResultSet rs = getGroup.executeQuery();
		if(rs.next()){
			g.setId(rs.getInt("people_group_id"));
			g.setDesc(rs.getString("group_desc"));
		}
		else{
			addGroup(g);
		}
		
		//now ensure the subgroup relationship.
		//TODO I am trusting that the user has verified that there are no cycles, I should verify!
		for(PeopleGroup subgroup : g.getSubgroups()){
			addSubgroup(g, subgroup);
		}
		
		//add all the people
		for(Person p: g.getPeople()){
			addPersonInGroup(g, p);
		}
	}
	
	
	private static PreparedStatement addGroup;
	
	public void addGroup(PeopleGroup g) throws SQLException {
		if(addGroup == null){
			addGroup = conn.prepareStatement("INSERT INTO people_group (group_name, group_desc) VALUES (?,?)");
		}
		addGroup.setString(1, g.getName());
		if(g.getDesc() != null)
			addGroup.setString(2,g.getDesc());
		else
			addGroup.setNull(2, Types.CHAR);
		addGroup.executeUpdate();
		
		String cmd = "SELECT currval('people_group_id_seq')";
		Statement getId = conn.createStatement();
		ResultSet rs = getId.executeQuery(cmd);
		if(rs.next()){
			g.setId(rs.getInt(1));
		}

	}
	
	private static PreparedStatement addSubgroupStmt;
	
	/**
	 * add a subgroup relationship
	 * 
	 * TODO should check that there are no cycles -- right now trusting the user!
	 * @param group
	 * @param subgroup
	 * @throws SQLException
	 */
	public void addSubgroup(PeopleGroup group, PeopleGroup subgroup) throws SQLException {
		if(addSubgroupStmt == null){
			addSubgroupStmt = conn.prepareStatement("INSERT INTO group_dag (people_group_id, subgroup) VALUES (?,?)");
		}
		addSubgroupStmt.setInt(1,group.getId());
		addSubgroupStmt.setInt(2, subgroup.getId());
		try{
			addSubgroupStmt.executeUpdate();
		} catch(SQLException ex){
			if(isDuplicateKeyError(ex))	//this mapping is already in the table
				return;
			else
				throw ex;
		}
	}
	
	private static PreparedStatement addPersonInGroupStmt;
	
	public void addPersonInGroup(PeopleGroup group, Person p) throws SQLException {
		if(addPersonInGroupStmt == null){
			addPersonInGroupStmt = conn.prepareStatement("INSERT INTO group_dag (people_group_id, person) VALUES (?,?)");
		}
		addPersonInGroupStmt.setInt(1,group.getId());
		addPersonInGroupStmt.setInt(2, p.getId());
		try{
			addPersonInGroupStmt.executeUpdate();
		} catch(SQLException ex){
			if(isDuplicateKeyError(ex))	//this mapping is already in the table
				return;
			else
				throw ex;
		}
	}
	
	
	private static PreparedStatement getPersonFromIdStmt;

	public Person getPersonFromId(int id) throws SQLException {
		if(getPersonFromIdStmt == null){
			getPersonFromIdStmt = conn.prepareStatement("SELECT name FROM people WHERE people_id = ?");
		}
		getPersonFromIdStmt.setInt(1, id);
		ResultSet rs = getPersonFromIdStmt.executeQuery();
		if(rs.next()){
			return new Person(rs.getString("name"));
		}
		else
			return null;
	}
	
	private static PreparedStatement getPeopleGroupFromId;
	
	public PeopleGroup getPeopleGroupFromId(int id) throws SQLException {
		if(getPeopleGroupFromId == null){
			getPeopleGroupFromId = conn.prepareStatement("SELECT group_name, group_desc FROM people WHERE people_id = ?");
		}
		getPeopleGroupFromId.setInt(1, id);
		ResultSet rs = getPeopleGroupFromId.executeQuery();
		if(rs.next()){
			PeopleGroup result = new PeopleGroup();
			result.setName(rs.getString("group_name"));
			result.setName(rs.getString("group_desc"));
			return result;
		}
		else
			return null;
	}
	
	private PreparedStatement getPeopleInGroupStmt;

	public List<Integer> getPeopleInGroup(PeopleGroup group) throws SQLException {
		if(getPeopleInGroupStmt == null){
			getPeopleInGroupStmt = conn.prepareStatement("SELECT person FROM group_dag WHERE people_group_id = ?");
		}
		getPeopleInGroupStmt.setInt(1, group.getId());
		ResultSet rs = getPeopleInGroupStmt.executeQuery();
		List<Integer> result = new ArrayList<Integer>();
		while(rs.next()){
			result.add(rs.getInt("person"));
		}
		return result;
	}

	
	
	
	
	private PreparedStatement getAllPeopleStmt;
	
	/**
	 * get all the people in the database.
	 * 
	 * does NOT fill in all the group relations
	 * 
	 * @return
	 * @throws SQLException
	 */
	public List<Person> getAllPeople() throws SQLException{
		if(getAllPeopleStmt == null){
			getAllPeopleStmt = conn.prepareStatement("SELECT people_id, name FROM people");
		}
		ResultSet r = getAllPeopleStmt.executeQuery();
		List<Person> result = new ArrayList<Person>();
		while(r.next()){
			int id = r.getInt("people_id");
			if (id != -1) {
				Person p = new Person(r.getString("name"));
				p.setId(id);
				result.add(p);
			}
		}
		return result;
	}
	
	private PreparedStatement getAllGroupsStmt;
	
	/**
	 * get all the people in the database.
	 * 
	 * does NOT fill in all the group relations
	 * 
	 * @return
	 * @throws SQLException
	 */
	public List<PeopleGroup> getAllGroups() throws SQLException{
		if(getAllGroupsStmt == null){
			getAllGroupsStmt = conn.prepareStatement("SELECT people_group_id, group_name, group_desc FROM people_group");
		}
		ResultSet r = getAllGroupsStmt.executeQuery();
		List<PeopleGroup> result = new ArrayList<PeopleGroup>();
		while(r.next()){
			PeopleGroup p = new PeopleGroup();
			int id = r.getInt("people_group_id");
			if(id != -1){
				p.setId(id);
				p.setName(r.getString("group_name"));
				p.setDesc(r.getString("group_desc"));
				result.add(p);
			}
		}
		return result;
	}
	
	public void addTaggableTable(Class<? extends Taggable> tgblClass,
			String tableName) {
		tagdb.addTaggableTable(tgblClass, tableName);
	}

	
	/* ************** static helpers *************************/
	
//	public void addSizeOrientation(DirectorySet ds) throws SQLException, IOException{
//		
//		//get all pictures
//		PreparedStatement st = conn.prepareStatement(
//				"SELECT picture_files.*" +
//				" FROM picture_files WHERE picture_files.height IS NULL");
//		List<PictureInfo> allPics = new ArrayList<PictureInfo>();
//		ResultSet rs = st.executeQuery();
//		while(rs.next()){
//			allPics.add(oneResultToPictureInfo(rs));
//		}
//		
//		//now add their size & orientation
//		String cmd = "UPDATE picture_files SET height = ?, width = ?, orientation = ? WHERE picture_id = ?";
//		st = conn.prepareStatement(cmd);
//		for(PictureInfo pic: allPics){
//			File f = pic.getPictureFile();
//			PictureInfo fullInfo = ds.getPictureInfo(f);
//			//update the height, width, & size
//			st.setInt(1, fullInfo.getHeight());
//			st.setInt(2,fullInfo.getWidth());
//			if(fullInfo.getOrientation() != 0)
//				st.setInt(3, fullInfo.getOrientation());
//			else
//				st.setNull(3, Types.INTEGER);
//			st.setInt(4, pic.getId());
//			st.execute();
//		}
//		
//	}
	
	public void addDirectoryParentReferences() throws SQLException{
		String cmd = "SELECT dir FROM directories";
		Statement st = conn.createStatement();
		ResultSet rs = st.executeQuery(cmd);
		Set<File> dirs = new HashSet<File>();
		while(rs.next()){
			String fileName = rs.getString(1);
			dirs.add(new File(fileName));
		}
		
		cmd = "UPDATE directories SET parent = ? WHERE dir = ?";
		PreparedStatement u = conn.prepareStatement(cmd);
		for(File f: dirs){
			File parent = f.getParentFile();
			if(dirs.contains(parent)){
				u.setString(1, parent.getAbsolutePath());
				u.setString(2,f.getAbsolutePath());
				u.execute();
			}
		}
		
	}
	
//	public void makeScreenSizeCache(String dir) throws SQLException, IOException{
//		ImageResizer resizer = new ImageResizer();
//		//get all pictures
//		PreparedStatement st = conn.prepareStatement(
//				picture_info_select + "WHERE filename LIKE " + dir);
//		List<PictureInfo> allPics = new ArrayList<PictureInfo>();
//		ResultSet rs = st.executeQuery();
//		while(rs.next()){
//			allPics.add(oneResultToPictureInfo(rs));
//		}
//		
//		Dimension s = Toolkit.getDefaultToolkit().getScreenSize();
//		int width = (int) s.getWidth();
//		int height = (int) s.getHeight();
//
//		for (PictureInfo picInfo : allPics) {
//			Dimension scaledSize = ImageResizer.getScaledDimensions(picInfo, width, height);
//			if(scaledSize.getWidth() == picInfo.getWidth() && scaledSize.getHeight() == picInfo.getHeight()){
//				continue;
//			}
//			File scaledFile = getScaledPictureFile(picInfo, (int) scaledSize
//					.getWidth(), (int) scaledSize.getHeight());
//			BufferedImage img;
//			if (scaledFile == null || !ImageResizer.isGoodPictureFile(scaledFile)) {
//				img = resizer.resizeImageSetProportions(picInfo,
//						(int) scaledSize.getWidth(), (int) scaledSize
//								.getHeight());
//				scaledFile = addScaledPicture(picInfo, (int) scaledSize
//						.getWidth(), (int) scaledSize.getHeight());
//				resizer.saveImage(img, scaledFile);
//			}
//		}
//	}
	
	public void pictureLoadTimingTest(String dir) throws SQLException, IOException{
		//get all pictures
		PreparedStatement st = conn.prepareStatement(
				picture_info_select + "WHERE filename LIKE " + dir);
		List<PictureInfo> allPics = new ArrayList<PictureInfo>();
		ResultSet rs = st.executeQuery();
		while(rs.next()){
			allPics.add(oneResultToPictureInfo(rs));
		}
		
		Dimension s = Toolkit.getDefaultToolkit().getScreenSize();
		int width = (int) s.getWidth();
		int height = (int) s.getHeight();
		int i = 0;

		for (PictureInfo picInfo : allPics) {
			i++;
			Dimension scaledSize = ImageResizer.getScaledDimensions(picInfo, width, height);
			if(scaledSize.getWidth() == picInfo.getWidth() && scaledSize.getHeight() == picInfo.getHeight()){
				continue;
			}
			long start = System.currentTimeMillis();
			ImageResizer.resizeImageSetProportions(picInfo,
					(int) scaledSize.getWidth(), (int) scaledSize
							.getHeight());
			long stop = System.currentTimeMillis();
			System.out.println("load #" + i + " took " + (stop - start) + " millis");
		}
	}
	
	public void resetDB() throws SQLException {
		Statement st = conn.createStatement();
		String cmd = "TRUNCATE picture_files CASCADE";
		st.executeUpdate(cmd);
		cmd = "ALTER SEQUENCE picture_files_id_seq RESTART 1";
		st.executeUpdate(cmd);
		cmd = "TRUNCATE directories";
		st.executeUpdate(cmd);
	}
	
	public void uniqueTest() throws SQLException {
		Statement st = conn.createStatement();
		try{
			int r = st.executeUpdate("INSERT INTO group_dag (people_group_id, person) VALUES (9, 107)");
			System.out.println(r);
		} catch( SQLException ex){
			ex.printStackTrace();
//		    System.out.println ("*** SQLException caught ***");
//
//		    while (ex != null) {
//		      System.out.println ("SQLState: " + ex.getSQLState () + "");
//		      System.out.println ("Message: " + ex.getMessage() + "");
//		      System.out.println ("Vendor ErrorCode: " + ex.getErrorCode() + "");
//		      ex = ex.getNextException();
//		      System.out.println("");
//		      }
			
		}
	}
	
	
	public void resetPeopleTables() throws SQLException {
		Statement st = conn.createStatement();
		st.executeUpdate("TRUNCATE people CASCADE");
		st.executeUpdate("TRUNCATE people_group CASCADE");
		st.executeUpdate("INSERT INTO people (people_id, name) VALUES (-1,'dummy')");
		st.executeUpdate("INSERT INTO people_group (people_group_id, group_name) VALUES (-1,'dummy')");
	}
	
	/**
	 * makes sure the sequences are consistent with the values in the tables.  This is useful
	 * if a table is bulk loaded, b/c then the sequence value needs to be updated.
	 * @throws SQLException
	 */
	public void updateSequences() throws SQLException {
		Statement st = conn.createStatement();
		for(String[] s: tableSequences){
			st.execute("ALTER SEQUENCE " + s[2] + " RESTART WITH (SELECT max(" + s[1] + ") + 1 FROM " + s[0] + ")");
		}
	}
	
//	public void runCommand(String[] args) throws SQLException, IOException {
//		if(args[0].equals("resetDB")){
//			resetDB();
//		}
//		else if(args[0].equals("addSizeOrientationInfo")){
//			DirectorySet ds = new DirectorySet(null, this);
//			addSizeOrientation(ds);
//		}
//		else if(args[0].equals("buildDirTree")){
//			addDirectoryParentReferences();
//		}
//		else if(args[0].equals("makeScreenSizeCache")){
//			String[] dirs = new String[]{
//					"'%2007\\\\\\\\06June\\\\\\\\14%'",
//					"'%2007\\\\\\\\06June\\\\\\\\15%'"//,
////					"'%2007\\\\\\\\06June\\\\\\\\16%'",
////					"'%2007\\\\\\\\06June\\\\\\\\17%'"
//					};
//			for(String s: dirs)
//				makeScreenSizeCache(s);
//		}
//		else if(args[0].equals("loadTimingTest")){
//			String[] dirs = new String[]{"'%2007\\\\\\\\06June\\\\\\\\14%'","'%2007\\\\\\\\06June\\\\\\\\15%'"};
//			for(String s: dirs)
//				pictureLoadTimingTest(s);
//		}
//		else if(args[0].equals("uniqueTest")){
//			uniqueTest();
//		}
//		else if(args[0].equals("resetPeople")){
//			resetPeopleTables();
//		}
//		else{
//			System.out.println("invalid command");
//		}
//
//	}
//	
//	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
//		DBConnection db = new DBConnection();
//		db.connect();
//		if(args.length == 0){
//			System.out.println("enter command:");
//			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
//			String line = in.readLine();
//			db.runCommand(line.split(" "));
//		}
//		else{
//			db.runCommand(args);
//		}
//	}
	
}
