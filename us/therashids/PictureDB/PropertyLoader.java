package us.therashids.PictureDB;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PropertyLoader {
	
	public static final String PROP_FILE_NAME = "picturedb.props";
	
	public enum Props {
		BASE_DATA_DIR,
		THUMBSIZE,
		UNDATED_PICTURE_DIRECTORY,
		SCALED_PICTURE_DIRECTORY,
		/** number of images per subdirectory in the scaled / undated directories */
		IMAGES_PER_DIRECTORY,
		IMG_DIR,
		IMG_LIMIT,
		DB_VERSION
	}
	
	static Properties props;
	
	static File propFile;
	
	/**
	 * if this is called *before* the *first* call to getProperties,
	 * it will set the prop file to a specific file.  Generally, this should be called
	 * by main() methods
	 * @param f
	 */
	public static void setPropFile(File f){
		if(props != null)
			throw new RuntimeException("cannot call setPropFile() after first call to getProperties!");
		propFile = f;
	}
	
	public static void setPropFileFromArgs(String... args){
		for(String s: args){
			if(s.startsWith("--properties=")){
				PropertyLoader.setPropFile(new File(s.substring(s.indexOf('=') + 1)));
			}
		}
	}
	
	public static List<File> findPropertiesFiles(){
		List<File> result = new ArrayList<File>();
		File currDir = new File(System.getProperty("user.dir"));
		for(String f: currDir.list()){
			if(f.endsWith(PROP_FILE_NAME))
				result.add(new File(currDir, f));
		}
		return result;
	}
	
	public static Properties getProperties(){
		if(props == null)
			try {
				loadProps();
			} catch (IOException e) {
				// TODO should we just bail, or should we go with default properties?
				e.printStackTrace();
			}
		return props;
	}
	
	private static void loadProps() throws IOException{
		if(propFile != null){
			props = new Properties(defaultProps());
			try {
				Reader reader = new BufferedReader(new FileReader(propFile));
				props.load(reader);
				reader.close();

			} catch (FileNotFoundException e) {
				e.printStackTrace(); // properties dont' exist yet, no big deal
			}
			return;
		}
		List<File> fs = findPropertiesFiles();
		if(fs.size() == 0){
			props = new Properties(defaultProps());
			propFile = new File(System.getProperty("user.dir"), PROP_FILE_NAME);
		}
		else if(fs.size() == 1){
			propFile = fs.get(0);
			props = new Properties(defaultProps());
			try {
				BufferedInputStream reader = new BufferedInputStream(new FileInputStream(propFile));
				props.load(reader);
				reader.close();

			} catch (FileNotFoundException e) {
				e.printStackTrace(); // shouldn't ever happen
			}
		}
		else{
			throw new RuntimeException("Sorry, support for multiple properties file not yet implemented");
		}
	}
	
	public static void save() throws IOException {
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(propFile));
		props.store(out, "For PictureDB application");
		out.close();
	}
	
	private static Properties defaultProps(){
		Properties result = new Properties();
		set(result, Props.BASE_DATA_DIR, System.getProperty("user.dir"));
		set(result, Props.THUMBSIZE, "100");
		set(result, Props.IMAGES_PER_DIRECTORY, "1000");
		set(result, Props.IMG_LIMIT, "1000");
		set(result, Props.DB_VERSION, "0");
		return result;
	}
	
	public static void set(Props p, String value){
		Properties props = getProperties();
		set(props, p, value);
	}
	
	private static void set(Properties ps, Props p, String value){
		ps.setProperty(p.name(), value);
	}
	
	public static String get(Props p){
		Properties  props = getProperties();
		if(props.containsKey(p.name())){
			return props.getProperty(p.name());
		}
		else if (p == Props.IMG_DIR)
			return get(Props.BASE_DATA_DIR) + "/imgs";
		else if(p == Props.UNDATED_PICTURE_DIRECTORY)
			return get(Props.IMG_DIR) + "/Undated";
		else if(p == Props.SCALED_PICTURE_DIRECTORY)
			return props.getProperty(Props.BASE_DATA_DIR.name()) + "/Scaled";
		else
			return props.getProperty(p.name());
	}
	

}
