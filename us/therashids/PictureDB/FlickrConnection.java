package us.therashids.PictureDB;

import java.io.*;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;

import javax.xml.parsers.ParserConfigurationException;

import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.xml.sax.SAXException;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.RequestContext;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.auth.Permission;
import com.flickr4java.flickr.photos.PhotoList;
import com.flickr4java.flickr.photos.PhotosInterface;
import com.flickr4java.flickr.photosets.Photoset;
import com.flickr4java.flickr.uploader.UploadMetaData;
import com.flickr4java.flickr.uploader.Uploader;
import com.flickr4java.flickr.util.AuthStore;
import com.flickr4java.flickr.util.FileAuthStore;


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
			try {
				this.authStore = new FileAuthStore(authsDir);
			} catch (FlickrException fe) {
				throw new RuntimeException("error initializing Flickr connection", fe);
			}
		}
	}
	
	private void authorize() throws IOException, SAXException, FlickrException {
		Token reqToken = this.flickr.getAuthInterface().getRequestToken();
		
		String authUrl = this.flickr.getAuthInterface().getAuthorizationUrl(reqToken, Permission.DELETE);
		System.out.println("Please visit: " + authUrl + ", paste the given code, and hit enter.");
				
		Scanner scanner = new Scanner(System.in);
		String tokenKey = scanner.nextLine();
		
		Token accessToken = this.flickr.getAuthInterface().getAccessToken(reqToken, new Verifier(tokenKey));
		Auth auth = this.flickr.getAuthInterface().checkToken(accessToken);
		RequestContext.getRequestContext().setAuth(auth);
		this.authStore.store(auth);
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
