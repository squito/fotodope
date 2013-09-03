package us.therashids.PictureDB;


import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.jheader.App1Header;
import net.sourceforge.jheader.JpegFormatException;
import net.sourceforge.jheader.JpegHeaders;
import net.sourceforge.jheader.TagFormatException;
import net.sourceforge.jheader.TagValue;
import net.sourceforge.jheader.App1Header.Tag;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.tiff.TiffField;
import org.apache.sanselan.formats.tiff.TiffImageMetadata;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import us.therashids.PictureDB.PropertyLoader.Props;
import us.therashids.PictureDB.gui.FileBrowserPanel;
import us.therashids.PictureDB.gui.TestFrame;
import us.therashids.PictureDB.gui.Thumbnail;
import us.therashids.PictureDB.picture_manipulation.ImageResizer;
import us.therashids.PictureDB.picture_manipulation.PythonResizer;
import us.therashids.util.FileCopy;

/**
 * does the intial processing for all files.
 * Needs a DB connection, and FlickrConnection must already be authorized!
 * 
 * @author Imran Rashid
 *
 */
public class DirectorySet {
	
	int THUMBSIZE = Integer.parseInt(PropertyLoader.get(Props.THUMBSIZE));
	File DEFAULT_BASE_DIRECTORY = new File(PropertyLoader.get(Props.BASE_DATA_DIR));
	File UNDATED_DIRECTORY = new File(PropertyLoader.get(Props.UNDATED_PICTURE_DIRECTORY));
	private static final int MAX_PER_UNDATED_SUBDIR = Integer.parseInt(PropertyLoader.get(Props.IMAGES_PER_DIRECTORY));
	int undatedSubdir;
	int countInCurrentUndatedSubdir;
	
	static int thumbQuality = 60;
	
	TestFrame driver;
	
	boolean USE_PYTHON = false;
	
	
	static final String[] monthIntToDirName = new String[]{
		"01January",
		"02February",
		"03March",
		"04April",
		"05May",
		"06June",
		"07July",
		"08August",
		"09September",
		"10October",
		"11November",
		"12December"
	};
	
	private ImageResizer resizer;
	boolean flickerizeAll = false;
	
	/** value is true if all subdirectories are also mapped, otherwise false**/
	HashMap<File, Boolean> mappedDirectories;
	
	List<File> alreadyExistingPictureFiles;
	
	DBConnection db;
	
	/**
	 * initializes the directory set -- gets the set of already indexed dirs and the current
	 * thumbsize from the db.
	 * @param c
	 * @param db
	 * @throws SQLException
	 */
	public DirectorySet(TestFrame c, DBConnection db) throws SQLException {
		driver = c;
		resizer = new ImageResizer(c);
		this.db = db;
		mappedDirectories = db.getMappedDirectories();
		undatedSubdir = Integer.parseInt(db.getGeneralInfoField("current_undated_subdir"));
		UNDATED_DIRECTORY.mkdirs();
		File d = new File(UNDATED_DIRECTORY, Integer.toString(undatedSubdir));
		d.mkdir();
		countInCurrentUndatedSubdir = d.list().length;
	}
	
	
	/**
	 * read a directory, and add all image files in into the database.  (does not recurse
	 * into subdirectories.  does not recheck directories already in db.)
	 * @param dir
	 * @throws IOException
	 * @throws SQLException
	 */
	public void addDirectoryToDBOffline(final File dir) throws IOException, SQLException{
		addDirectoryToDBOffline(dir, false, false);
	}
	
	/**
	 * read a directory, and add all image files in into the database.  (does
	 * not recheck directories already in db.)
	 * @param dir
	 * @param recursive if true, recurse into subdirectories
	 * @throws IOException
	 * @throws SQLException
	 */
	public void addDirectoryToDBOffline(final File dir, final boolean recursive) throws IOException, SQLException{
		addDirectoryToDBOffline(dir, recursive, false);
	}

	
	
	/**
	 * read a directory, and add all image files in into the database.
	 * @param dir
	 * @param recursive if true, recurse into subdirectories
	 * @param recheck unless this is a true, if a dir is already in the db then skip
	 * @throws IOException
	 * @throws SQLException
	 */
	public void addDirectoryToDBOffline(final File dir, final boolean recursive, final boolean recheck) throws IOException, SQLException {
		final Calendar now = new GregorianCalendar();
		now.setTimeInMillis(System.currentTimeMillis());
		if(mappedDirectories.containsKey(dir) && !recheck)
			return;
		File[] files;
		files = dir.listFiles();
		BatchJobAdapter<File> bj = new BatchJobAdapter<File>(){
			boolean anySubDirs;
			
			@Override
			public void aboutToProcess() {
			}
			
			@Override
			public void processOneItem(File f) throws IOException, SQLException{
				if(ImageResizer.isGoodPictureFile(f)){
					processOnePictureFile(f);
				}
				if(f.isDirectory()){
					anySubDirs = true;
					if(recursive)
						addDirectoryToDBOffline(f, recursive, recheck);
				}

			}
			
			@Override
			public void allDone() throws IOException, SQLException{
				boolean allSub = !anySubDirs || recursive;
				mappedDirectories.put(dir, allSub);
				db.addDirectoryToDB(dir, allSub, now);
			}
			
		};
		List<File> fileList = new ArrayList<File>();
		for(File f: files)
			fileList.add(f);
		bj.setList(fileList);
		driver.addBatchJob(bj);
//		boolean anySubDirs = false;
//		for(File f: files){
//			if(isGoodPictureFile(f)){
//				processOnePictureFile(f);
//			}
//			if(f.isDirectory()){
//				anySubDirs = true;
//				if(recursive)
//					addDirectoryToDBOffline(f, recursive, recheck);
//			}
//		}
//		boolean allSub = !anySubDirs || recursive;
//		mappedDirectories.put(dir, allSub);
//		db.addDirectoryToDB(dir, allSub, now);
	}
	
	
	/**
	 * Given another directory, will process all the files in that directory,
	 * and copy them into the "default" directory structure.  This is useful
	 * for copying images from flash media, other people's cds, etc.
	 * 
	 * @param importDir
	 * @param recursive
	 */
	public void importPicturesToDefaultDirectories(File importDir, final PictureImportOptions opts, final FileBrowserPanel fb) throws IOException, SQLException{
		alreadyExistingPictureFiles = new ArrayList<File>();
		importPicturesToDefaultDirectoriesHelper(importDir, opts, fb);
	}
	
	void importPicturesToDefaultDirectoriesHelper(final File importDir,
			final PictureImportOptions opts, final FileBrowserPanel fb) throws IOException, SQLException {
		File[] files = importDir.listFiles();
		BatchJobAdapter<File> bj = new BatchJobAdapter<File>() {
			public void aboutToProcess() throws IOException, SQLException {
			}

			public void processOneItem(File f) throws IOException, SQLException {
				if (ImageResizer.isGoodPictureFile(f)) {
					bp.setCurrentStatusText("Importing: " + f);
					try {
						importPictureToDefaultDirectory(f, opts);
					} catch (IOException e) {
						System.out
								.println("exception on file: " + f.toString());
						throw (e);
					} catch (SQLException e) {
						System.out
								.println("exception on file: " + f.toString());
						throw (e);
					} catch (RuntimeException e) {
						System.out
								.println("exception on file: " + f.toString());
						throw (e);
					}
				}
				if (f.isDirectory() && opts.isRecursive()) {
					importPicturesToDefaultDirectoriesHelper(f, opts, fb);
				}
			}

			public void allDone() throws IOException, SQLException {
				fb.updateTree();
				bp.setCurrentStatusText("Done importing pictures");
			}
		};
		List<File> fileList = new ArrayList<File>();
		for (File f : files)
			fileList.add(f);
		bj.setList(fileList);
		driver.addBatchJob(bj);
	}
	
	public PictureInfo importPictureToDefaultDirectory(File pictureFile, final PictureImportOptions opts) throws IOException, SQLException {
		return importPictureToDefaultDirectory(pictureFile, null, opts);
	}
	
	/**
	 * import one picture into the default directory.
	 * @param f	the picture file that should be imported
	 * @param size a dimension specifying the size of the picture -- if null, the value
	 * 				will be obtained by reading the file
	 */
	public PictureInfo importPictureToDefaultDirectory(File pictureFile, Dimension size, final PictureImportOptions opts) throws IOException, SQLException {
		PictureInfo info = getPictureInfo(pictureFile, opts);
		if(info == null)
			return null;	//TODO error handling here -- bad jpeg, we should at least log it somehow
		if(size != null){
			info.setWidth((int)size.getWidth());
			info.setHeight((int)size.getHeight());
		}
		//given the date, copy the file over (and change info to reflect it)
		File dir;
		if(info.timeTaken != null)
			dir = getDirectoryFromDate(DEFAULT_BASE_DIRECTORY, info.timeTaken);
		else
			dir = getUndatedDirectory();
		File newPictureFile = new File(dir,pictureFile.getName());
//		if(newPictureFile.exists()){
//			alreadyExistingPictureFiles.add(newPictureFile);
//			return;
//		}
//		FileCopy.copy(pictureFile, newPictureFile);
		if(!newPictureFile.exists()){
			FileCopy.copy(pictureFile, newPictureFile);
		}

		info.pictureFile = newPictureFile;
		//now add it to DB
		db.addPicture(info);
		makeThumbnail(info);
		if(opts.getPhotographer() != null){
			Person photographer = new Person(opts.getPhotographer());
			db.getOrAddPerson(photographer);
			db.addPhotographer(photographer, info);
		}
		return info;
	}
	
	/**
	 * for a picture already in the database, re-run it through the import
	 * routine.  This will re-create the thumbnail, reload the EXIF info,
	 * reset the date (and maybe move the file).  But, it will keep
	 * all of the tags, etc. associated with the picture.
	 * 
	 * @param oldInfo
	 * @param opts
	 * @return
	 */
	public PictureInfo reimportPicture(PictureInfo oldInfo, PictureImportOptions opts) throws IOException, SQLException {
		File oldFile = oldInfo.getPictureFile();
		PictureInfo newInfo = getPictureInfo(oldFile, opts);
		newInfo.setId(oldInfo.getId());
		
		File dir;
		if(newInfo.timeTaken != null)
			dir = getDirectoryFromDate(DEFAULT_BASE_DIRECTORY, newInfo.timeTaken);
		else
			dir = getUndatedDirectory();
		File newPictureFile = new File(dir,oldFile.getName().replaceAll("#", ""));
		if(!newPictureFile.equals(oldFile)){
			//move the picture over
			oldFile.renameTo(newPictureFile);
		}
		newInfo.setThumbFile(oldInfo.getThumbFile());
		newInfo.setPictureFile(newPictureFile);
		
		db.updatePictureInfo(newInfo, oldInfo);
		makeThumbnail(newInfo);
		
		return newInfo;
	}
	
	
		
	File getUndatedDirectory(){
		if(countInCurrentUndatedSubdir >= MAX_PER_UNDATED_SUBDIR){
			undatedSubdir++;
			countInCurrentUndatedSubdir = 1;
			File d = new File(UNDATED_DIRECTORY, Integer.toString(undatedSubdir));
			d.mkdir();
			return d;
		}
		countInCurrentUndatedSubdir++;
		return new File(UNDATED_DIRECTORY, Integer.toString(undatedSubdir));
	}
	
	/**
	 * 1) construct the appropriate directory for a picture, given the time it was taken.
	 * 2) make sure those directories exist.
	 * 3) make sure those directories are in the db.
	 * 
	 * assumes it can make the directories, there aren't already files with those names, etc.
	 * @param baseDir
	 * @param time
	 * @return
	 */
	File getDirectoryFromDate(File baseDir, Calendar time) throws SQLException{
		Calendar now = new GregorianCalendar();
		now.setTimeInMillis(System.currentTimeMillis());
		String[] dirParts = new String[]{
				String.valueOf(time.get(Calendar.YEAR)),
				monthIntToDirName[time.get(Calendar.MONTH)],
				String.valueOf(time.get(Calendar.DAY_OF_MONTH))
		};
		File dir = baseDir;
		for(int i = 0; i < dirParts.length; i++){
			dir = new File(dir,dirParts[i]);
			if(!dir.exists()){
				// if it didn't exist before, we can set its last checked time, to now
				dir.mkdir();
				db.addDirectoryToDB(dir, true, now);
			}
			else if(!db.isDirectoryInDB(dir)){
				db.addDirectoryToDB(dir,false,null);
			}
		}
		return dir;
	}
	
	
	/**
	 * adds picture to the database, and creates the thumbnail, sticks the thumbnail in db
	 * @param pictureFile
	 * @return
	 * @throws IOException
	 */
	Thumbnail processOnePictureFile(File pictureFile) throws IOException, SQLException {
//		TODO check if picture is already in DB
		PictureInfo info = getPictureInfo(pictureFile, null);
		db.addPicture(info);
		Thumbnail thumb = getThumbnail(info);
//		ImageResizer.saveImage(thumb.getThumbnail(), info.getThumbFile());
		return thumb;
	}
	
	static DateFormat exifDateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
	
	@SuppressWarnings("unchecked")
	public PictureInfo getPictureInfo(File pictureFile,
			final PictureImportOptions opts) throws IOException {
		//http://www.screaming-penguin.com/node/7485
		
		PictureInfo info = new PictureInfo(-1, pictureFile, null);
		IImageMetadata metadata = null;
		try {
			metadata = Sanselan.getMetadata(pictureFile);
		} catch (ImageReadException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Map<Integer, TiffField> savedFields = new HashMap<Integer, TiffField>();
		savedFields.put(TiffField.EXIF_TAG_EXIF_IMAGE_WIDTH.tag, null);
		savedFields.put(TiffField.EXIF_TAG_EXIF_IMAGE_LENGTH.tag, null);
		savedFields.put(TiffField.EXIF_TAG_IMAGE_WIDTH.tag, null);
		savedFields.put(TiffField.EXIF_TAG_IMAGE_HEIGHT.tag, null);
		savedFields.put(TiffField.EXIF_TAG_DATE_TIME_ORIGINAL.tag, null);
		savedFields.put(TiffField.EXIF_TAG_ORIENTATION.tag, null);
		List<TiffImageMetadata.Item> meta = metadata.getItems();
		for (TiffImageMetadata.Item i : meta) {
			int t = i.getTiffField().tag;
			if (savedFields.containsKey(t)) {
				savedFields.put(t, i.getTiffField());
			}
		}

		switch (opts.getSizeChoice()) {
		case ExifSize:
			TiffField width = savedFields
					.get(TiffField.EXIF_TAG_EXIF_IMAGE_WIDTH.tag);
			TiffField height = savedFields
					.get(TiffField.EXIF_TAG_EXIF_IMAGE_LENGTH.tag);
			if (width != null && height != null) {
				try {
					info.setWidth(width.getIntValue());
					info.setHeight(height.getIntValue());
					break;
				} catch (ImageReadException e) {
				}
			}
		case JPEGHeaders:
			width = savedFields.get(TiffField.EXIF_TAG_IMAGE_WIDTH.tag);
			height = savedFields.get(TiffField.EXIF_TAG_IMAGE_HEIGHT.tag);
			if (width != null && height != null) {
				try {
					info.setWidth(width.getIntValue());
					info.setHeight(height.getIntValue());
					break;
				} catch (ImageReadException e) {
				}
			}
		case PicSize:
		default:
			throw new NotImplementedException();
		}
		
		switch(opts.getDateChoice()){
		case ExifDate:
			TiffField date = savedFields.get(TiffField.EXIF_TAG_DATE_TIME_ORIGINAL.tag);
			if(date != null){
				try{
					Calendar c = Calendar.getInstance();
					c.setTime(exifDateFormat.parse(date.getStringValue()));
					info.setTimeTaken(c);
				} catch(ImageReadException e){
				} catch (ParseException e) {
				}
			}
			break;
		case ManualDate:
		case FileModDate:
			throw new NotImplementedException();
		}
		TiffField orientation = savedFields.get(TiffField.EXIF_TAG_ORIENTATION.tag);
		if(orientation != null){
			try{
				info.setOrientation(orientation.getIntValue());
			} catch(ImageReadException e){
			}
		}
		setWidthHeightUsingOrientation(info);
		
		return info;
	}
	
	/**
	 * processes header info from pictures
	 * @param pictureFile
	 * @return
	 * @throws IOException
	 */
	public PictureInfo oldGetPictureInfo(File pictureFile,
			final PictureImportOptions opts) throws IOException {
		try {
			PictureInfo info = new PictureInfo(-1, pictureFile, null);
			// get the basic picture info
			/*
			 * note: message "Corrupt JPEG section" comes from
			 * JpegHeaders.readSection() I think it is making a mistake ... java
			 * jpeg reader seems to handle those files just fine. though it does
			 * seem to only occur on files coming from photoshop, not from a
			 * camera, where there is no EXIF info.
			 * 
			 * in either case, it seems really strange to have output on
			 * system.err....
			 */
			JpegHeaders headers = new JpegHeaders(new FileInputStream(
					pictureFile));
			App1Header exifHeader = headers.getApp1Header();

			switch (opts.getSizeChoice()) {

			case ExifSize:
				if (exifHeader != null && getExifSize(info, exifHeader))
					break;
			case JPEGHeaders:
				if (headers != null && getJpegHeaderSize(info, headers)) {
					break;
				}
			case PicSize:
			default:
				BufferedImage img = ImageResizer.loadImage(pictureFile);
				info.setWidth(img.getWidth());
				info.setHeight(img.getHeight());
			}

			switch (opts.getDateChoice()) {
			case ManualDate:
				info.setTimeTaken(opts.getManualDate());
				break;

			case FileModDate:
				throw new NotImplementedException();

			default:
			case ExifDate:
				TagValue date = exifHeader.getValue(Tag.DATETIME);
				Calendar d = null;
				if (date != null) {
					d = date.getAsDateTimeTag().asCalendar();
				}
				info.setTimeTaken(d);
			}

			TagValue orientation = exifHeader.getValue(Tag.ORIENTATION);
			int o = 0;
			if (orientation != null) {
				o = orientation.getAsInteger();
			}
			info.setOrientation(o);
			setWidthHeightUsingOrientation(info);
			return info;
		} catch (JpegFormatException e) {
			e.printStackTrace();
			throw new IOException("jpeg format exception: " + e.getMessage());
		} catch (IOException e) {
			//if(e.getMessage().equals("Error in JPEG - incorrect EXIF header"))
			// return null;
			// else
			throw e;
		}
		// return null;
	}
	
	private boolean getExifSize(PictureInfo info, App1Header exif) throws TagFormatException {
		TagValue h = exif.getValue(Tag.EXIFIMAGEHEIGHT);
		if (h != null)
			info.setHeight(h.getAsLong().intValue());
		TagValue w = exif.getValue(Tag.EXIFIMAGEWIDTH);
		if (w != null)
			info.setWidth(w.getAsLong().intValue());
		return (h!= null && w != null);
	}
	
	private boolean getJpegHeaderSize(PictureInfo info, JpegHeaders headers){
		info.setHeight(headers.getHeight());
		info.setWidth(headers.getWidth());
		return true;
	}
	
	void setWidthHeightUsingOrientation(PictureInfo info){
		// see http://jpegclub.org/exif_orientation.html
		if(info.getOrientation() > 4){
			int temp = info.getHeight();
			info.setHeight(info.getWidth());
			info.setWidth(temp);
		}
	}
	
	Thumbnail getThumbnail(PictureInfo info) throws IOException{
		//create thumbnail
		BufferedImage thumb = resizer.resizeImageMaxSide(info, Integer.parseInt(PropertyLoader.get(Props.THUMBSIZE)));
		return new Thumbnail(thumb, info);
	}
	
	public void makeThumbnail(PictureInfo info) throws IOException, SQLException {
		if(USE_PYTHON){
			PythonResizer.makeThumbnail(info, THUMBSIZE);
		}
		else{
			Thumbnail t = getThumbnail(info);
			ImageResizer.saveImage(t.getThumbnail(), info.getThumbFile(), thumbQuality);
			db.addScaledPicture(info, info.getThumbFile(), t.getThumbnail().getWidth(), t.getThumbnail().getHeight(), thumbQuality);
		}
	}
	
	
	static void fixDirectoryParents(DBConnection db) throws SQLException {
		Set<File> dirs = db.getDirectories();
		Set<File> addedDirs = new HashSet<File>();
		for(File d: dirs){
			File parent = d.getParentFile();
			if(parent.getAbsolutePath().indexOf("My Pictures\\2") >= 0){
				if(!dirs.contains(parent) && !addedDirs.contains(parent)){
					db.addDirectoryToDB(parent, false, null);
					addedDirs.add(parent);
				}
				db.setParentDir(d, parent);
			}
		}
	}
	
//	public static PictureInfo uploadToFlickr(PictureInfo info) throws FlickrException{
//		int flickrID = Integer.parseInt(Photo.create(info.getPictureFile(), info.getPictureFile().getName()).getID());
//		info.setFlickrID(flickrID);
//		return info;
//	}
	
	public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException{
		DBConnection db = DBConnection.getInstance();
		db.initialize();
//		DirectorySet dr = new DirectorySet(new Container(), db);
//		dr.addDirectoryToDBOffline(new File("C:\\Documents and Settings\\Imran Rashid\\My Documents\\My Pictures\\2006\\12December"),true);
//		dr.addDirectoryToDBOffline(new File("C:\\Documents and Settings\\Imran Rashid\\My Documents\\My Pictures\\2006\\12December\\25"));
//		dr.addDirectoryToDBOffline(new File("C:\\Documents and Settings\\Imran Rashid\\My Documents\\My Pictures\\2006\\12December\\19"));
//		dr.addDirectoryToDBOffline(new File("C:\\Documents and Settings\\Imran Rashid\\My Documents\\My Pictures\\2006\\12December\\17"));
//		dr.addDirectoryToDBOffline(new File("C:\\Documents and Settings\\Imran Rashid\\My Documents\\My Pictures\\2006\\10October\\21"));
//		dr.addDirectoryToDBOffline(new File("C:\\Documents and Settings\\Imran Rashid\\My Documents\\My Pictures\\2006\\10October\\23"));
		
//		dr.processOnePictureFile(new File("C:\\Documents and Settings\\Imran Rashid\\My Documents\\My Pictures\\2006\\12December\\17\\IMG_2391.JPG"));
		
//		PictureInfo p = new PictureInfo(-1, new File("C:\\Documents and Settings\\Imran Rashid\\My Documents\\My Pictures\\2006\\12December\\25\\IMG_2438.JPG"), null);
//		uploadToFlickr(p);
		
		fixDirectoryParents(db);
		
		//for some reason, doesn't exit ...
		System.exit(0);
	}

}
