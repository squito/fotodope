package us.therashids.PictureDB.gui;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import us.therashids.PictureDB.BatchJob;
import us.therashids.PictureDB.BatchJobAdapter;
import us.therashids.PictureDB.DBConnection;
import us.therashids.PictureDB.DirectorySet;
import us.therashids.PictureDB.FacebookUploader;
import us.therashids.PictureDB.FlickrConnection;
import us.therashids.PictureDB.ImageCache;
import us.therashids.PictureDB.ImageView;
import us.therashids.PictureDB.PictureImportOptions;
import us.therashids.PictureDB.PictureInfo;
import us.therashids.PictureDB.PropertyLoader;
import us.therashids.PictureDB.FacebookUploader.FacebookPrompter;
import us.therashids.PictureDB.gui.ImageList.ImageListModel;
import us.therashids.PictureDB.gui.SlideshowViewer.Mode;
import us.therashids.PictureDB.picture_manipulation.ImageResizer;
import us.therashids.util.FileCopy;

import com.flickr4java.flickr.FlickrException;
import com.google.code.facebookapi.FacebookException;
import com.google.code.facebookapi.schema.Album;
import com.therashids.TagDag.StringTag;
import com.therashids.TagDag.Tag;
import com.therashids.TagDag.TagSet;
import com.therashids.utils.DateRangePanel;

public class TestFrame extends JFrame {
	
	ImageListPanel tagListPanel;
	ImageListPanel dateListPanel;
	ImageListPanel dirListPanel;
	
	ImageListModel savedModel;
	ImageListPanel savedPanel;
	
	JMenuBar menuBar;
	DBConnection db;
	DirectorySet directorySet;
	BatchProgress bp;
	final JFileChooser fileChooser = new JFileChooser();
	FileBrowserPanel fb;
	final ImageTagBrowserPanel tb;
	DateRangePanel dateSelectionPanel;
	final ImageResizer imgResizer;
	final Tagger tggr;
	
	JTabbedPane panes;
	JSplitPane dirView;
	JSplitPane tagView;
	JSplitPane dateView;
	JScrollPane savedView;
	
	SlideshowViewer slideshow;
	
	TagSet<PictureInfo> tagSet;
	
	FlickrConnection flickrConn;
	FacebookUploader fbUploader;
	FacebookPrompter fbPrompter;
	ImportOptionChooser importOptionChooser;
	
	
	
	
	
	public TestFrame(DBConnection db) throws IOException, SQLException, ParserConfigurationException{
		super();
		this.db = db;
		try{
			directorySet = new DirectorySet(this, db);
		} catch(SQLException e){
			e.printStackTrace();
		}
		addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e){
            	try {
					PropertyLoader.save();
					DBConnection.getInstance().disconnect();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (SQLException e2) {
					e2.printStackTrace();
				}
                System.exit(0);
            }
        });
		List<Tag> tagClasses = new ArrayList<Tag>();
		tagClasses.add(new StringTag(""));
		tagSet = new TagSet<PictureInfo>(db, tagClasses, PictureInfo.class);
		tggr = new Tagger(this, tagSet.getTrie());
		
		tb = new ImageTagBrowserPanel(this, tagSet);
		
		//have to do this here b/c its final ...
		flickrConn = new FlickrConnection();
		imgResizer = new ImageResizer(this);
		ImageCache.setupInst(this);
		fbUploader = new FacebookUploader(imgResizer, db);
		fbPrompter = new FacebookUploadDialoger(this);
		slideshow = new SlideshowViewer(this, db, imgResizer);
		importOptionChooser = new ImportOptionChooser(this);
		importOptionChooser.pack();
		initComponents();
		initMenus();
		
		
		tb.setPanel(tagListPanel);
	}
	
	
	JSplitPane getWithSaveSidebar(ImageListPanel ilp) {
		ImageList sideSavePanel = new ImageList(100,savedModel);
		sideSavePanel.setBorder(BorderFactory.createTitledBorder("Saved Images"));
		JScrollPane sideSaveScroll = new JScrollPane(sideSavePanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		ilp.imgList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		ilp.imgList.setVisibleRowCount(-1);
		JSplitPane mainAndSave = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, ilp, sideSaveScroll);
		mainAndSave.setResizeWeight(1);
		
		return mainAndSave;
	}
	
	public void initComponents(){
		savedModel = new ImageListModel();
		panes = new JTabbedPane();
		
		dateListPanel = new ImageListPanel();
		JSplitPane dateAndSave = getWithSaveSidebar(dateListPanel);
		dateSelectionPanel = new DateRangePanel(){
			@Override
			public void datesChosen(Calendar begin, Calendar end, boolean includeUndated, boolean highlightsOnly) {
				try {
					List<PictureInfo> pics = db.getPicturesBetweenDates(begin, end, includeUndated, highlightsOnly);
					dateListPanel.setPictures(pics);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		};
		dateView = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, dateSelectionPanel, dateAndSave);
		panes.add("Date View", dateView);
		
		tagListPanel = new ImageListPanel();
		JSplitPane tagAndSave = getWithSaveSidebar(tagListPanel);
		tagView = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tb, tagAndSave);
		panes.add("Tag View", tagView);
		
		dirListPanel = new ImageListPanel();
		JSplitPane dirAndSave = getWithSaveSidebar(dirListPanel);
		dirAndSave.setResizeWeight(1);

		fb = new FileBrowserPanel(dirListPanel);
		dirView = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,fb,dirAndSave);
		panes.add("Directory View", dirView);
		
		
		savedPanel = new ImageListPanel(savedModel);
		savedPanel.imgList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		savedPanel.imgList.setVisibleRowCount(-1);
		savedView = new JScrollPane(savedPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		panes.add("Saved Pictures", savedView);
		
		
		
		

		
		panes.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				if(panes.getSelectedComponent() == dirView){
					fb.updateThumbpanel();
				}
			}
		});

		getContentPane().add(panes, BorderLayout.CENTER);
		
		
		
	}
	
	public void initMenus(){
		menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        
        JMenu actions = new JMenu("Actions");
        JMenuItem importPics = new JMenuItem("Import Pictures");
        importPics.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		importPics();
        	}
        });
        JMenuItem addDir = new JMenuItem("Add Directory");
        addDir.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		addDir();
        	}
        });
        
        JMenuItem flickerize = new JMenuItem("Flickerize");
        flickerize.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		flickerizeSelection();
        	}
        });
        
        JMenuItem flickrSync = new JMenuItem("Flickr Sync");
        flickrSync.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		flickrSync();
        	}
        });
        
        JMenuItem fbUpload = new JMenuItem("Upload To Facebook");
        fbUpload.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e) {
        		fbUpload();
        	}
        });
        
        JMenuItem remove = new JMenuItem("Remove From DB");
        remove.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		delete(false);
        	}
        });
        
        JMenuItem copy = new JMenuItem("Copy Picture Files");
        copy.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e) {
        		copy();
        	}
        });
        
        JMenuItem delete = new JMenuItem("Delete Pictures");
        delete.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		delete(true);
        	}
        });
        
        Action tagAct = new AbstractAction("Tag Pictures"){
        	public void actionPerformed(ActionEvent e) {
        		tggr.setVisible(true);
        		String tagList = tggr.getValidatedText();
        		if (tagList != null) {
					Collection<String> tokens = TagSet
							.tokenizeStringListTags(tagList);
					List<PictureInfo> pics = getSelectionByMode();
					List<StringTag> tags = new ArrayList<StringTag>();
					for(String t: tokens){
						tags.add(new StringTag(t));
					}
					try {
						getTagSet().addAllTagsToAllTaggables(tags, pics);
					} catch (SQLException ex) {
						// TODO
						ex.printStackTrace();
					}
				}
        	}
        };
        tagAct.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, ActionEvent.CTRL_MASK));
        JMenuItem tag = new JMenuItem(tagAct);
  

         
        actions.add(importPics);
        actions.add(addDir);
        actions.add(flickerize);
        actions.add(flickrSync);
        actions.add(fbUpload);
        actions.add(tag);
        actions.add(remove);
        actions.add(copy);
        actions.add(delete);
        menuBar.add(actions);
        
        
        JMenu selectionMenu = new JMenu("Select");
        Action selectAll = new AbstractAction("Select All"){
        	public void actionPerformed(ActionEvent e) {
        		getActiveList().imgList.setAllSelected();
        	}
        };
        selectAll.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.CTRL_MASK));
        selectionMenu.add(selectAll);
        menuBar.add(selectionMenu);

        
        JMenu slideshowMenu = new JMenu("Slideshow");
        
        Action fullScreenSlideshowAction = new AbstractAction("FullScreen Slideshow"){
        	public void actionPerformed(ActionEvent e) {
        		slideshow.setPicList(getSelectionByMode());
        		slideshow.setMode(Mode.FullScreen);
        	}
        };
        fullScreenSlideshowAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(
                KeyEvent.VK_F, ActionEvent.CTRL_MASK));
        JMenuItem fullScreenSlideshow = new JMenuItem(fullScreenSlideshowAction);

        Action windowSlideshowAction = new AbstractAction("Window Slideshow"){
        	public void actionPerformed(ActionEvent e) {
        		slideshow.setPicList(getSelectionByMode());
        		slideshow.setMode(Mode.Window);
        	}
        };
        windowSlideshowAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(
                KeyEvent.VK_W, ActionEvent.CTRL_MASK));
        JMenuItem windowSlideshow = new JMenuItem(windowSlideshowAction);

        
        slideshowMenu.add(fullScreenSlideshow);
        slideshowMenu.add(windowSlideshow);
        menuBar.add(slideshowMenu);
        
        
        JMenu dbManage = new JMenu("Database Management");
        
        JMenuItem reimport = new JMenuItem("Reimport Pictures");
        reimport.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e) {
        		reimport();
        	}
        });
        
        
        JMenuItem refreshThumb = new JMenuItem("Refresh Thumbnail");
        refreshThumb.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e) {
        		refreshThumbs();
        	}
        });

        Action makeScreenSizeAction = new AbstractAction("Make Cached Images"){
        	public void actionPerformed(ActionEvent e) {
//        		makeScreenSizeCache();
        		makeAllCaches();
        	}
        };
        JMenuItem makeScreenSize = new JMenuItem(makeScreenSizeAction);
                
        Action removeEmptyDirsAction = new AbstractAction("Remove Empty Dirs"){
        	public void actionPerformed(ActionEvent e){
        		try{
        			fb.deleteEmptyDirs();
        		}catch(SQLException ex){
        			ex.printStackTrace();
        		}
        	}
        };
        JMenuItem removeEmptyDirs = new JMenuItem(removeEmptyDirsAction);
        
        Action removeScreenSizeCacheAction = new AbstractAction("Remove Screen Size Cache"){
        	public void actionPerformed(ActionEvent e){
        		deleteScreenSizeCache();
        	}

        };
        JMenuItem removeScreenCache = new JMenuItem(removeScreenSizeCacheAction);
        
        dbManage.add(reimport);
        dbManage.add(refreshThumb);
        dbManage.add(makeScreenSize);
        dbManage.addSeparator();
        dbManage.add(removeEmptyDirs);
        dbManage.add(removeScreenCache);
        menuBar.add(dbManage);
       
	}
	
	void importPics(){
		importOptionChooser.setVisible(true);
		
		PictureImportOptions opts = importOptionChooser.getOptions();
		if(opts.getImportFile() == null)
			return;
		
		try{
			directorySet.importPicturesToDefaultDirectories(opts.getImportFile(), opts, fb);
		} catch(IOException e){
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	void addToSaveList(PictureInfo pic) {
		savedModel.add(pic);
	}
	
	void removeFromSaveList(PictureInfo pic) {
		savedModel.remove(pic);
	}
	
	boolean isInSaveList(PictureInfo pic) {
		return savedModel.imgs.contains(pic);
	}
	
	void addDir(){
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int chosenQ = fileChooser.showOpenDialog(this);
		if(chosenQ != JFileChooser.APPROVE_OPTION)
			return;
		File dirToAdd = fileChooser.getSelectedFile();
		try{
			directorySet.addDirectoryToDBOffline(dirToAdd);
		} catch(IOException e){
			e.printStackTrace();
		} catch(SQLException e){
			e.printStackTrace();
		}
	}
	
	void flickerizeSelection(){
		try{
			List<PictureInfo> pics = getSelectionByMode();
			BatchJobAdapter<PictureInfo> bj = new BatchJobAdapter<PictureInfo>(){
				final FlickrConnection conn = flickrConn;
				public void aboutToProcess() throws IOException, SQLException {
					try{
						conn.connect();
					} catch(SAXException e){
						//TODO
					} catch(FlickrException e){
					}
				}
				public void allDone() throws IOException, SQLException {
				}
				public void processOneItem(PictureInfo p) throws IOException, SQLException {
					if(p.getFlickrID() > 0)
						return;
					long flickrID = 0;
					Calendar now = new GregorianCalendar();
					now.setTimeInMillis(System.currentTimeMillis());
					try{
						flickrID = Long.parseLong(conn.upload(p.getPictureFile()));
					} catch (FlickrException e){
						//TODO
						e.printStackTrace();
					} catch (SAXException e){
						e.printStackTrace();
					}
					p.setFlickrID(flickrID);
					db.addFlickrInfo(p, now);
				}
			};
			bj.setList(pics);
			addBatchJob(bj);
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	void reimport(){
		
		importOptionChooser.setVisible(true);
		final PictureImportOptions opts = importOptionChooser.getOptions();
		
		try{
			List<PictureInfo> pics = getSelectionByMode();
			BatchJobAdapter<PictureInfo> bj = new BatchJobAdapter<PictureInfo>(){
				public void aboutToProcess() throws IOException, SQLException {
				}
				public void allDone() throws IOException, SQLException {
				}
				public void processOneItem(PictureInfo p) throws IOException, SQLException {
					directorySet.reimportPicture(p, opts);
				}
			};
			bj.setList(pics);
			addBatchJob(bj);
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	
	public void flickrSync(){
		try {
			flickrConn.getFlickrSetsAsAlbums();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FlickrException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void fbUpload(){
		try{
			List<PictureInfo> pics = getSelectionByMode();
			final Album uploadAlbum = fbUploader.chooseAlbum(fbPrompter);
			BatchJobAdapter<PictureInfo> bj = new BatchJobAdapter<PictureInfo>(){
				public void aboutToProcess() throws IOException, SQLException {
				}
				public void allDone() throws IOException, SQLException {
				}
				public void processOneItem(PictureInfo p) throws IOException, SQLException {
					try {
						fbUploader.uploadPhoto(p, uploadAlbum, fbPrompter);
					} catch (FacebookException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			};
			bj.setList(pics);
			addBatchJob(bj);

		} catch(Exception e){
			e.printStackTrace();
		}

	}
	
	public static List<ImageView> cacheSizes = new ArrayList<ImageView>();
	static{
		cacheSizes.add(new ImageView(false, 50, 50, null, 0, 70));
		cacheSizes.add(new ImageView(false, 100, 100, null, 0, 70));
		cacheSizes.add(new ImageView(false, 200, 200, null, 0, 80));
		cacheSizes.add(new ImageView(false, 400, 400, null, 0, 80));
		cacheSizes.add(new ImageView(false, 600, 600, null, 0, 100));
		final Dimension s = Toolkit.getDefaultToolkit().getScreenSize();
		final int width = (int) s.getWidth();
		final int height = (int) s.getHeight();
		cacheSizes.add(new ImageView(false, width, height, null, 0, 100));
	}
	
	
	public void makeScreenSizeCache(){
		//TODO should be able to set the sizes as parameters
		final Dimension s = Toolkit.getDefaultToolkit().getScreenSize();
		final int width = (int) s.getWidth();
		final int height = (int) s.getHeight();
		BatchJobAdapter<PictureInfo> bj = new BatchJobAdapter<PictureInfo>(){
			public void aboutToProcess() throws IOException, SQLException {
			}
			public void processOneItem(PictureInfo p) throws IOException, SQLException {
				bp.setCurrentStatusText("Cacheing " + p.getPictureFile());
				imgResizer.ensureScaledCache(db, p, width, height);
			}
			public void allDone() throws IOException, SQLException {
				bp.setCurrentStatusText("Done cacheing");
			}
		};
		bj.setList(getSelectionByMode());
		addBatchJob(bj);
	}
	
	public void makeAllCaches(){
		makeAllCaches(getSelectionByMode());
	}
	
	public void makeAllCaches(List<PictureInfo> pics) {
		BatchJobAdapter<PictureInfo> bj = new BatchJobAdapter<PictureInfo>(){
			public void aboutToProcess() throws IOException, SQLException {
			}
			public void processOneItem(PictureInfo p) throws IOException, SQLException {
				bp.setCurrentStatusText("Cacheing " + p.getPictureFile());
				imgResizer.ensureCachedSizes(db, p, cacheSizes);
			}
			public void allDone() throws IOException, SQLException {
				bp.setCurrentStatusText("Done cacheing");
			}
		};
		bj.setList(pics);
		addBatchJob(bj);		
	}
	
	
	public void copy(){
		List<PictureInfo> pics = getSelectionByMode();
		
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int chosenQ = fileChooser.showDialog(this, "Select Copy Destination");
		if(chosenQ != JFileChooser.APPROVE_OPTION)
			return;

		final File chosenDir = fileChooser.getSelectedFile();
		BatchJobAdapter<PictureInfo> bj = new BatchJobAdapter<PictureInfo>(){
			int count = 0;
			public void aboutToProcess() throws IOException, SQLException {
			}
			public void processOneItem(PictureInfo p) throws IOException, SQLException {
				String newName = p.getPictureFile().getName();
				newName = newName.replaceAll("\\.", "_" + count + "\\.");
				FileCopy.copy(p.getPictureFile(), new File(chosenDir,newName));
				count++;
			}
			public void allDone() throws IOException, SQLException {
			}
		};
		bj.setList(pics);
		addBatchJob(bj);


		
	}
	
	public void delete(boolean deleteOriginal){
		List<PictureInfo> pics = getSelectionByMode();
		int choice = JOptionPane.showConfirmDialog(this, "Delete " + pics.size() + " pictures?", "Delete", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		if(choice == JOptionPane.OK_OPTION){
			for(PictureInfo p: pics){
				deleteOne(p, deleteOriginal);
			}
		}
	}
	
	public void deleteOne(PictureInfo p, boolean deleteOriginal){
		try{
			List<File> scaledFiles = db.removePictureNoWarnings(p);
			for(File f: scaledFiles){
				f.delete();
			}
			p.getThumbFile().delete();
			if(deleteOriginal){
				p.getPictureFile().delete();
			}
			
		} catch(SQLException e){
			e.printStackTrace();
		}
	}
	
	
	public TagSet<PictureInfo> getTagSet(){
		return tagSet;
	}
	
	
	List<PictureInfo> getSelectionByMode() {
		ImageListPanel ilp = getActiveList();
		if(ilp != null)
			return ilp.getPics();
		return null;
	}
	
	ImageListPanel getActiveList() {
		if (panes.getSelectedComponent() == dirView) {
			return dirListPanel;
		} else if (panes.getSelectedComponent() == tagView) {
			return tagListPanel;
		} else if (panes.getSelectedComponent() == dateView) {
			return dateListPanel;
		} else if (panes.getSelectedComponent() == savedView) {
			return savedPanel;
		}
		return null;
	}
	
	
	public void refreshThumbs(){
		BatchJobAdapter<PictureInfo> bj = new BatchJobAdapter<PictureInfo>(){
			public void aboutToProcess() throws IOException, SQLException {
			}
			public void processOneItem(PictureInfo p) throws IOException, SQLException {
				directorySet.makeThumbnail(p);
			}
			public void allDone() throws IOException, SQLException {
			}
		};
		bj.setList(getSelectionByMode());
		addBatchJob(bj);
	}
	
	
	void deleteScreenSizeCache(){
		BatchJobAdapter<PictureInfo> bj = new BatchJobAdapter<PictureInfo>(){
			public void aboutToProcess() throws IOException, SQLException {
			}
			public void processOneItem(PictureInfo p) throws IOException, SQLException {
				db.removeScaledPicture(p);
			}
			public void allDone() throws IOException, SQLException {
			}
		};
		bj.setList(getSelectionByMode());
		addBatchJob(bj);
	}

	
	public void addBatchJob(BatchJob j){
		if(bp == null)
			bp = new BatchProgress();
		bp.addJob(j);
	}
	
	public static void main(String... args) throws IOException, SQLException, ClassNotFoundException, InterruptedException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, ParserConfigurationException{
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		PropertyLoader.setPropFileFromArgs(args);
		normalRun();
	}
	
	public static void normalRun() throws IOException, SQLException, ClassNotFoundException, InterruptedException, ParserConfigurationException{
		PropertyLoader.getProperties();
		TestFrame t = new TestFrame(DBConnection.getInstance());
		t.setSize(new Dimension(600,400));
		t.validate();
		t.setSize(800,600);
		t.setVisible(true);
		t.repaint();
	}

}
