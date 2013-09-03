package us.therashids.PictureDB;

import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Controls the connection to the database AND the local scaled image directories.
 * 
 * 
 * @author Imran Rashid
 *
 */
public class PSQLPictureDB extends DBConnection {
		
	private static final String jdbcDriver = "org.postgresql.Driver";
	
	//psql.exe -Uphotoapp picturedb
	private static final String dbName = "picturedb";
	private static final String user = "photoapp";
	private static final String password = "photoapp";
	
	
	//backup command:
	//C:\Program Files\PostgreSQL\8.2\bin>pg_dump -d -C -f picturedb_bak.sql -U photoapp picturedb
		
	public void doConnect() throws ClassNotFoundException, SQLException {
		//get connection
		Class.forName(jdbcDriver);
		conn = DriverManager.getConnection("jdbc:postgresql:" + dbName, user, password);
	}	
}
