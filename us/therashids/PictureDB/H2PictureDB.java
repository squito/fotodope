package us.therashids.PictureDB;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import org.h2.tools.RunScript;
import org.h2.tools.Server;

import com.therashids.utils.FileUtils;

import us.therashids.PictureDB.PropertyLoader.Props;

/**
 * H2 implementation of picture db connection
 * 
 * 
 * 
 *  java -jar h2-1.1.103.jar -url jdbc:h2:tcp://localhost//Users/imran/Desktop/old projects/workspace/PhotoDB/data/test/testdb/db/picturedb
 *  
 *  
 * 
 * @author Imran Rashid
 *
 */
public class H2PictureDB extends DBConnection {
	
	private static final String SCHEMA_FILE = "data/H2_DB_DEF.sql";
	private static int latest_version = 1;
	
	private static final String protocol = "jdbc:h2:tcp://localhost/";
	
	Server server;
	
	@Override
	public void doConnect() throws ClassNotFoundException, SQLException {
		doConnect( "sa", "");
	}

	public void doConnect(String user, String pass)
			throws ClassNotFoundException, SQLException {
		Class.forName("org.h2.Driver");
		
		server = Server.createTcpServer(new String[0]).start();
		
		conn = DriverManager.getConnection(getDefaultConnURL(), user, pass);
	}
	
	@Override
	public void disconnect() throws SQLException {
		super.disconnect();
		server.stop();
	}
	
	public static void main(String... args) throws Exception {
		PropertyLoader.setPropFileFromArgs(args);
		if(args[0].equals("createNewDB")){
			createNewDB();
			PropertyLoader.set(Props.DB_VERSION, Integer.toString(latest_version));
			PropertyLoader.save();
		}
		else if(args[0].equals("restore")){
			H2PictureDB db = new H2PictureDB();
			db.doConnect();
			db.restoreFromBackupCSV(args[1]);
			db.disconnect();
		}
		else if(args[0].equals("backup")) {
			H2PictureDB db = new H2PictureDB();
			db.doConnect();
			db.backupToCSV();
			db.disconnect();
		}
		else if(args[0].equals("delete")) {
			int minId = Integer.parseInt(args[1]);
			int maxId = Integer.parseInt(args[2]);
			H2PictureDB db = new H2PictureDB();
			db.doConnect();
			for(int i = minId; i <= maxId; i++) {
				PictureInfo pic = db.getPictureById(i);
				if(pic == null)
					continue;
				List<File> scaledFiles = db.removePictureNoWarnings(pic);
				for(File f: scaledFiles){
					f.delete();
				}
				pic.getThumbFile().delete();
				pic.getPictureFile().delete();
			}
			db.disconnect();
		}
	}
	
	public static void createNewDB() throws Exception{
		H2PictureDB db = null;
		try {
			db = new H2PictureDB();
			File f = new File(PropertyLoader.get(Props.BASE_DATA_DIR));
			System.out.println("Base data dir:" + f);
			f.mkdirs();
			f = new File(f,"db");
			if (f.exists())
				FileUtils.deleteDir(f);
			f.mkdirs();
			f = new File(PropertyLoader.get(Props.UNDATED_PICTURE_DIRECTORY));
			if(!f.exists())
				f.mkdirs();
			f = new File(f, "1");
			f.mkdirs();
			System.out.println("Database URL: " + getDefaultConnURL());
			db.doConnect();
			RunScript.main(new String[] { "-url", getDefaultConnURL(), "-user",
					"sa", "-script", getSchema(), "-showResults" });
		} finally {
			if (db != null)
				db.disconnect();
		}
	}
	
	private static String getSchema() throws FileNotFoundException {
		return SCHEMA_FILE;
	}
	
	private static String getDefaultConnURL() {
		return protocol + PropertyLoader.get(Props.BASE_DATA_DIR) + "/db/picturedb";
	}
	
	
	private void restoreFromBackupCSV(String CSVdir) throws SQLException, IOException{
		Statement st = conn.createStatement();
		String[] tables = new String[]{//"directories",
				"picture_files",
				"scaled_pictures","string_tags", "string_tag_dag","picture_tags",
				"flickr_info"
				};
		for(int i = 0; i < tables.length; i++) {
			String t = tables[i];
			String file = CSVdir + t + ".csv";
			System.out.println("Restoring from file: " + file);
			BufferedReader in = new BufferedReader(new FileReader(file));
			String header = in.readLine();
			in.close();
			String cmd = "INSERT INTO " + t + "(" + header + ") SELECT * FROM CSVREAD('" + file + "')";
			System.out.println(cmd);
			int count = st.executeUpdate(cmd);
			System.out.println("Added " + count + " rows");
		}
		updateSequences();
	}
	
	private void backupToCSV() throws SQLException {
		DatabaseMetaData md = conn.getMetaData();
		ResultSet rs = md.getTables(null, null, null, null);
		Statement st = conn.createStatement();
		while(rs.next()){
			String catalog = rs.getString(1);
			String schema = rs.getString(2);
			String tableName = rs.getString(3);
			System.out.println(catalog + "\t" + schema + "\t" + tableName);
			if(catalog.equals("PICTUREDB") && schema.equals("PUBLIC"))
				st.executeUpdate("CALL CSVWRITE('" + tableName + ".csv','SELECT * FROM " + tableName + "')");
		}
	}
	

}
