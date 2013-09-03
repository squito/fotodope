package us.therashids.PictureDB;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.List;

import javax.xml.bind.JAXBElement;

import us.therashids.PictureDB.picture_manipulation.ImageResizer;

import com.google.code.facebookapi.FacebookException;
import com.google.code.facebookapi.FacebookJaxbRestClient;
import com.google.code.facebookapi.schema.Album;
import com.google.code.facebookapi.schema.Photo;
import com.google.code.facebookapi.schema.PhotosGetAlbumsResponse;

/**
 * class to upload photos to facebook.
 * 
 * Currently NOT thread-safe.
 * 
 * @author im
 * 
 */
public class FacebookUploader {
	public static String API_KEY = "9197bddab1896aee94bd38c322bd262f";
	public static String SECRET = "4630858e05ae5c5070e305207e0f22df";
	static int MAX_DIM = 600; // facebook api says max is 604

	ImageResizer imgResizer;
	DBConnection db;

	boolean authorized = false;
	long userId;
	FacebookJaxbRestClient client;
	String session;

	public FacebookUploader(ImageResizer imgResizer, DBConnection db) {
		this.imgResizer = imgResizer;
		this.db = db;
	}
	
	@SuppressWarnings("unchecked")
	public Album chooseAlbum(FacebookPrompter fa) throws FacebookException {
		// authorize
		ensureAuthorization(fa);
		// get the album
		client.photos_getAlbums(userId);
		List<Album> albums = ((PhotosGetAlbumsResponse) client
				.getResponsePOJO()).getAlbum();
		Album album = fa.chooseAlbum(albums);
		if (album.getAid() == 0) {
			client.photos_createAlbum(album.getName());
			album = ((JAXBElement<Album>) client.getResponsePOJO()).getValue();
		}
		return album;
	}

	@SuppressWarnings("unchecked")
	public void uploadPhoto(PictureInfo p, Album album, FacebookPrompter fa)
			throws FacebookException, SQLException, IOException {

		if (db.isInFacebok(p, userId, album.getAid())) {
			fa.notifyAlreadyUploaded(p);
			return;
		}
		// TODO check if the photo has been uploaded to another album / user,
		// inform the user

		// create a scaled image
		int width = p.getWidth();
		int height = p.getHeight();
		if (width > height) {
			width = MAX_DIM;
			height = (int) (height * (MAX_DIM / (double) p.getWidth()));
		} else {
			height = MAX_DIM;
			width = (int) (width * (MAX_DIM / (double) p.getHeight()));
		}
		File fileToUpload = imgResizer.ensureScaledCache(db, p, width, height);
		client.photos_upload(fileToUpload, album.getAid());
		Photo fbPhoto = ((JAXBElement<Photo>) client.getResponsePOJO())
				.getValue();

		db.addFacebookPhoto(p, fbPhoto.getPid(), userId, fbPhoto.getAid());
	}

	public void ensureAuthorization(FacebookPrompter fa)
			throws FacebookException {
		if (!authorized) {
			client = new FacebookJaxbRestClient(API_KEY, SECRET);
			client.setIsDesktop(true);
			String token = client.auth_createToken();
			String url = "http://www.facebook.com/login.php?api_key=" + API_KEY
					+ "&v=1.0" + "&auth_token=" + token;
			fa.authorize(url);
			session = client.auth_getSession(token);
			userId = client.users_getLoggedInUser();
			authorized = true;
		}
	}

	public interface FacebookPrompter {
		/**
		 * get the user to go to the URL and authorize.
		 * 
		 * @param URL
		 */
		public void authorize(String URL);

		/**
		 * choose which album to upload the photos too. Either return one
		 * element from the list, or return a new Album with the title set.
		 * 
		 * @param albums
		 * @return
		 */
		public Album chooseAlbum(List<Album> albums);

		/**
		 * notify the user that they have already uploaded this picture to the
		 * same album, so it will not be done again.
		 */
		public void notifyAlreadyUploaded(PictureInfo p);

		/**
		 * tell the user that they have already uploaded this picture to a
		 * different album, and let them choose whether they want to re-upload
		 * to a different album. Return true if the user wishes to upload again
		 * anyway.
		 * 
		 * @param f
		 * @param otherAlbum
		 * @return
		 */
		public boolean askUpload(PictureInfo p, String otherAlbum);

		/**
		 * tell the user that this photo has already been uploaded to somebody
		 * else's account, and let them choose if they still want to upload.
		 * Return true if the user wishes to upload anyway
		 * 
		 * @param f
		 * @param otherUser
		 * @param album
		 * @return
		 */
		public boolean askUpload(PictureInfo p, String otherUser, String album);
	}
	
	public static class CmdLineFacebookPrompter implements FacebookPrompter {
		public boolean askUpload(PictureInfo arg0, String arg1) {
			return true;
		}
		public boolean askUpload(PictureInfo arg0, String arg1, String arg2) {
			return true;
		}
		public void authorize(String url) {
			System.out.println(url);
			System.out.println("log in to facebook and press enter.");
			try {
				System.in.read();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		public Album chooseAlbum(List<Album> as) {
			System.out.println("Choose an album:");
			for(int i = 0; i < as.size(); i++){
				System.out.println( (i + 1) + ": " + as.get(i).getName());
			}
			System.out.println((as.size() + 1) + ": <New Album>");
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in)); 
			try {
				String l = in.readLine();
				int choice = Integer.parseInt(l) - 1;
				if(choice < as.size())
					return as.get(choice);
				else{
					System.out.println("Enter name for new album:");
					String name = in.readLine();
					Album album = new Album();
					album.setName(name);
					return album;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// TODO Auto-generated method stub
			return null;
		}
		
		public void notifyAlreadyUploaded(PictureInfo arg0) {
		}
	}

}
