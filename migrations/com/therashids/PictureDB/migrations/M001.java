package com.therashids.PictureDB.migrations;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import us.therashids.PictureDB.DBConnection;

import com.threrashids.migrate.Migration;


public class M001 implements Migration<DBConnection> {
	
	public String description() {
		return "Add tables for pictures uploaded to facebook";
	}
	
	public void migrate(DBConnection db) throws SQLException {
		Connection conn = db.conn;
		Statement st = conn.createStatement();
		st.executeUpdate(
						"CREATE TABLE facebook_photos ("
						+ "picture_id INTEGER NOT NULL REFERENCES picture_files ON DELETE CASCADE ON UPDATE CASCADE,"
						+ "fb_oid BIGINT,"
						+ "fb_pid BIGINT,"
						+ "fb_aid BIGINT,"
						+ "CONSTRAINT facebook_photos_unique UNIQUE (picture_id, fb_oid, fb_pid, fb_aid)"
						+ ");");
		
		st.executeUpdate("CREATE INDEX facebook_photos_pictureid ON facebook_photos (picture_id);");
		st.executeUpdate("CREATE INDEX facebook_photos_pictureid_owner ON facebook_photos (picture_id,fb_oid);");
		st.executeUpdate("CREATE INDEX facebook_photos_pictureid_owner_album ON facebook_photos(picture_id,fb_oid,fb_aid);");
	}
	

}
