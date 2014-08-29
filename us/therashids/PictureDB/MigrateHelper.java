package us.therashids.PictureDB;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;

import us.therashids.PictureDB.PropertyLoader.Props;

import com.threrashids.migrate.Migrate;


public class MigrateHelper {
	
	public static void main(String... args) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException{
		PropertyLoader.setPropFileFromArgs(args);
		int currentVersion = Integer.parseInt(PropertyLoader.get(Props.DB_VERSION));
		int toVersion = -1;
		for(int i = 0; i < args.length; i++){
			if(args[i].equals("-to")){
				toVersion = Integer.parseInt(args[i + 1]);
				break;
			}
		}
		if(toVersion == -1){
			System.out.println("Enter migration to version :");
			String line;
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			line = in.readLine();
			toVersion = Integer.parseInt(line);
		}
		
		DBConnection db = DBConnection.getInstance();
		Migrate.migrate("com.therashids.PictureDB.migrations.M", currentVersion, db, toVersion);
		PropertyLoader.set(Props.DB_VERSION, Integer.toString(toVersion));
		PropertyLoader.save();
	}

}
