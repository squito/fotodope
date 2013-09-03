package us.therashids.PictureDB;

import java.io.*;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.RequestContext;
import com.aetrion.flickr.auth.Auth;
import com.aetrion.flickr.auth.Permission;
import com.aetrion.flickr.photos.PhotoList;
import com.aetrion.flickr.photos.PhotosInterface;
import com.aetrion.flickr.photosets.Photoset;
import com.aetrion.flickr.uploader.UploadMetaData;
import com.aetrion.flickr.uploader.Uploader;
import com.aetrion.flickr.util.AuthStore;
import com.aetrion.flickr.util.FileAuthStore;


public class FlickrConnection {
	
	private static final String key = "419960c9b1d1a6fcce01afabf98e5326";
	private static final String secret = "85152c3ca4a67644";
	private static final String user = "illinoisimran";
	private static final String myNsid = "11628881@N00";
	
	private static final File AUTH_DIR = new File(System.getProperty("user.home") + File.separatorChar + ".flickrAuth");
	
	private String nsid = null;
	private Flickr flickr = null;
	private AuthStore authStore = null;
	private String sharedSecret = null;
	
	boolean authenticated = false;
	
	public FlickrConnection() throws IOException, ParserConfigurationException{
		this(key, myNsid, secret, AUTH_DIR);
	}
	
	public FlickrConnection(String apiKey, String nsid, String sharedSecret, File authsDir) throws IOException, ParserConfigurationException {
		this.flickr = new Flickr(apiKey, sharedSecret, new REST());
		this.sharedSecret = sharedSecret;
		this.nsid = nsid;
		
		if (authsDir != null) {
			this.authStore = new FileAuthStore(authsDir);
		}
	}
	
	private void authorize() throws IOException, SAXException, FlickrException {
		String frob = this.flickr.getAuthInterface().getFrob();
		
		URL authUrl = this.flickr.getAuthInterface().buildAuthenticationUrl(Permission.DELETE, frob);
		System.out.println("Please visit: " + authUrl.toExternalForm() + " then, hit enter.");
				
		System.in.read();
		
		
		Auth token = this.flickr.getAuthInterface().getToken(frob);
		RequestContext.getRequestContext().setAuth(token);
		this.authStore.store(token);
		System.out.println("Thanks.  You probably will not have to do this every time.  Now starting backup.");
	}
	
	public void connect()throws IOException, SAXException, FlickrException{
		RequestContext rc = RequestContext.getRequestContext();
		rc.setSharedSecret(this.sharedSecret);
		
		if (this.authStore != null) {
			Auth auth = this.authStore.retrieve(this.nsid);
			if (auth == null) this.authorize();
			else rc.setAuth(auth);
		}
	}
	
	public void getPhotoList() throws SAXException, IOException, FlickrException{
		connect();
		
		PhotosInterface photoInt = flickr.getPhotosInterface();
		
		PhotoList photos = photoInt.getNotInSet(10, 1);
		System.out.println(photos.size());
	}
	
	/**
	 * gets all photosets on flickr, as albums in my database.
	 * if a photoset does not have a corresponding album, then
	 * create the album.
	 * 
	 * this does *not* do anything with the album picture list.
	 * 
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 * @throws FlickrException
	 */
	public Collection<Album> getFlickrSetsAsAlbums() throws SAXException, IOException, FlickrException{
		if(!authenticated){
			connect();
			authenticated = true;
		}
		Collection<Album> flickrAlbums = new ArrayList<Album>();
		Collection<Photoset> sets = flickr.getPhotosetsInterface().getList(nsid).getPhotosets();
		DBConnection db = DBConnection.getInstance();
		for(Photoset p: sets){
			String flickrId = p.getId();
			Album a;
			try{
				a = db.getAlbumByFlickrId(flickrId);
				if(a == null){
					//wasn't already in the db -- add it
					a = db.addAlbumFromFlickr(flickrId, p.getTitle());
				}
				flickrAlbums.add(a);

			} catch (SQLException ex){
				//TODO
			}
		}
		return flickrAlbums;
	}
	
	public String upload(File pictureFile) throws SAXException, FlickrException, FileNotFoundException, IOException{
		if(!authenticated){
			connect();
			authenticated = true;
		}
		Uploader uploader = new Uploader(flickr.getApiKey(), flickr.getSharedSecret());
		UploadMetaData m = new UploadMetaData();
		m.setTitle(pictureFile.getName());
		m.setPublicFlag(true);
		return uploader.upload(new FileInputStream(pictureFile), m);
	}
	
	public String upload(PictureInfo picInfo) throws SAXException, FlickrException, FileNotFoundException, IOException{
		Uploader uploader = new Uploader(flickr.getApiKey(), flickr.getSharedSecret());
		UploadMetaData m = new UploadMetaData();
		m.setTitle(picInfo.getPictureFile().getName());
		m.setPublicFlag(true);
		m.setTags(picInfo.getTags());
		return uploader.upload(new FileInputStream(picInfo.getPictureFile()), m);
	}

	
	public static void main(String[] args) throws IOException, SAXException, FlickrException, ParserConfigurationException {
		FlickrConnection fc = new FlickrConnection(key, myNsid, secret,AUTH_DIR);
		fc.getPhotoList();
	}

	
	
//	public static User authenticate() throws FlickrException, IOException{
//		Flickr.setApiKey(key);
//		Flickr.setSharedSecret(secret);
//		Permission perm = Permission.DELETE; // access level we want
//        User user = Auth.getDefaultAuthUser(); // Check to see if we've already authenticated
//        //User user = User.findByUsername("netdance"); // or look for a specific user
//        
//        if (user != null && Auth.isAuthenticated(user, perm)) {
//            System.out.println("The Default Authenticated User is \""+user.getUserName()+"\"");
//            System.out.println("Already authenticated to at least "+perm+" level.");
//            System.out.println("We're actually at the "+Auth.getPermLevel(user)+" level.");
//            Auth.setAuthContext(user);
//            System.out.println("We've used "+Auth.getBandwidthUsed()+
//                    " bytes bandwidth out of "+Auth.getBandwidthMax()+
//                    " bytes available.");
//            System.out.println("We have room for "+Auth.getFilesizeMax()+
//                    " bytes of files.");
//            return user;
//        }
//        // Give the URL to enter in the browser
//        System.out.println("Please enter the following URL into a browser, " +
//                "follow the instructions, and then come back");
//        String authURL = Auth.getAuthURL(perm);
//        System.out.println(authURL);
//        // Wait for them to come back
//        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
//        in.readLine();
//        // Alright, now that we're cleared with Flickr, let's try to authenticate
//        System.out.println("Trying to get "+perm+" access.");
//        try {
//            user = Auth.authenticate();
//        } catch (FlickrException ex) {
//            System.out.println("Failed to authenticate.");
//            ex.printStackTrace(System.out);
//            System.exit(1);
//        }
//        if (Auth.isAuthenticated(user,perm)) {
//            Auth.setDefaultAuthUser(user);
//            Auth.setAuthContext(user);
//            System.out.println("We've authenticated user \""+user.getUserName()+
//                    "\" with "+Auth.getPermLevel(user)+" access.");
//            System.out.println("We've used "+Auth.getBandwidthUsed()+
//                    " bytes bandwidth out of "+Auth.getBandwidthMax()+
//                    " bytes available.");
//            System.out.println("We have room for "+Auth.getFilesizeMax()+
//                    " bytes of files.");
//            Auth.setDefaultAuthUser(user);
//        } else {
//            //Shouldn't ever get here - we throw an exception above if we can't authenticate.
//            System.out.println("Oddly unauthenticated");
//        }
//        return user;
//	}
//	
//	public static void getSomePhotos(User user) throws FlickrException{
//		PhotoSearch search = new PhotoSearch();
//		search.setUser(user);
//		search.addExtra("geo");
//		List<Photo> photos = Photo.search(search);
//		for(Photo p: photos){
//			System.out.println(p);
//		}
//		
//	}
//
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) throws FlickrException, IOException {
//		
//		Flickr.setApiKey(key);
//		Flickr.setSharedSecret(secret);
//		User user = authenticate();
//		getSomePhotos(user);
//
//	}

}
