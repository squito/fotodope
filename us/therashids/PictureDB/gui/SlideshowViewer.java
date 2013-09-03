package us.therashids.PictureDB.gui;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JFrame;

import us.therashids.PictureDB.DBConnection;
import us.therashids.PictureDB.PictureInfo;
import us.therashids.PictureDB.picture_manipulation.ImageResizer;

import com.therashids.TagDag.StringTag;



public class SlideshowViewer{
	
	List<PictureInfo> pics;
	Image currentImage;
	int currentImageIndex = 0;
	DBConnection db;
	TestFrame driver;
	ImagePanel imagePanel;
	JFrame framePlayer;
	JFrame fullScreenPlayer;
	int width;
	int height;
	ImageResizer resizer;
	Tagger tagger;
	Tagger fullScreenTagger, windowTagger;

	
	enum Mode {FullScreen, Window, OFF}
	
	Mode mode;
	
	public SlideshowViewer(TestFrame driver, DBConnection db, ImageResizer imgResizer) {
		super();
		this.db = db;
		this.driver = driver;
		mode = Mode.OFF;
		resizer = imgResizer;
		imagePanel = new ImagePanel(this);
	}
	
	public void setMode(Mode m){
		if(m != mode){
			if(m == Mode.OFF && mode == Mode.Window){
				framePlayer.setVisible(false);
			}
			else if(m == Mode.OFF && mode == Mode.FullScreen){
				fullScreenPlayer.setVisible(false);
			}
			if(m != Mode.OFF && (pics == null || pics.size() == 0))
				return;
			mode = m;
			currentImageIndex = 0;
	        if(mode == Mode.Window){
	            if(framePlayer == null){
	                framePlayer = new JFrame();
	                framePlayer.addWindowListener(new WindowAdapter(){
	                    public void windowClosing(WindowEvent e){
//	                        playerModel.setExitFlag(true);
	                    	setMode(Mode.OFF);
	                    }
	                });
	                windowTagger = new Tagger(framePlayer, driver.tagSet.getTrie());
	            }
                Dimension size = new Dimension(600, 400);
                framePlayer.setResizable(false);
                framePlayer.setSize(size);
                width = (int)size.getWidth();
                height = (int)size.getHeight();
	            framePlayer.getContentPane().add(imagePanel, BorderLayout.CENTER);
	            updateImage();
	            framePlayer.setVisible(true);
	            tagger = windowTagger;
	        }
	        else if(mode == Mode.FullScreen){
	            if(fullScreenPlayer == null){
	                fullScreenPlayer = new JFrame();
	                fullScreenPlayer.setUndecorated(true);
	                Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
	                fullScreenPlayer.setSize(size);
	                width = (int)size.getWidth();
	                height = (int)size.getHeight();
	                fullScreenTagger = new Tagger(fullScreenPlayer, driver.tagSet.getTrie());
	            }
                Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
                fullScreenPlayer.setSize(size);
                width = (int)size.getWidth();
                height = (int)size.getHeight();
	            fullScreenPlayer.getContentPane().add(imagePanel, BorderLayout.CENTER);
	            updateImage();
	            fullScreenPlayer.setVisible(true);
	            fullScreenPlayer.setFocusable(true);
	            imagePanel.requestFocus();
	            tagger = fullScreenTagger;
	        }

		}
	}
	
	public void requestNextImage(){
		currentImageIndex++;
		if(currentImageIndex >= pics.size()){
			currentImageIndex = 0;
		}
		updateImage();
	}
	
	public void requestPreviousImage(){
		currentImageIndex--;
		if(currentImageIndex < 0){
			currentImageIndex = pics.size() - 1;
		}
		updateImage();
	}
	
	public void requestSave(){
		driver.addToSaveList(pics.get(currentImageIndex));
	}
	
	public void requestUnsave(){
		driver.removeFromSaveList(pics.get(currentImageIndex));
	}
	
	public boolean isInSaveList() {
		return driver.isInSaveList(pics.get(currentImageIndex));
	}
	
	public void requestDelete(){
		driver.deleteOne(pics.get(currentImageIndex), true);
		pics.remove(currentImageIndex);
		//do it this way, instead of just calling updateImage(), so that we take appropriate
		//behavior if its the last image
		currentImageIndex--;
		requestNextImage();
	}
	
	public void setPicList(List<PictureInfo> l){
		pics = l;
	}
	
	void updateImage(){
		try{
			currentImage = getSizedImage(pics.get(currentImageIndex), mode == Mode.FullScreen);
		} catch(IOException ex){
			//TODO
		}
		imagePanel.setImage(currentImage);
	}
	
	
	public Tagger getTagger(){
		return tagger;
	}
	
	public void addTags(Collection<String> tokens) throws SQLException{
		Collection<PictureInfo> p = new ArrayList<PictureInfo>(1);
		p.add(pics.get(currentImageIndex));
		List<StringTag> tags = new ArrayList<StringTag>();
		for(String t: tokens){
			tags.add(new StringTag(t));
		}
		driver.getTagSet().addAllTagsToAllTaggables(tags, p);
	}
			
	BufferedImage getSizedImage(PictureInfo picInfo, boolean makeCachedVersion) throws IOException {
		Dimension scaledSize = ImageResizer.getScaledDimensions(picInfo, width, height);
		if(scaledSize.getWidth() == picInfo.getWidth() && scaledSize.getHeight() == picInfo.getHeight()){
			return resizer.loadRotate(picInfo);
		}
		else{
			//check DB for cached version of resized image
			try{
				resizer.ensureScaledCache(db, picInfo, width, height);
				File scaledFile = db.getScaledPictureFile(picInfo, (int)scaledSize.getWidth(), (int)scaledSize.getHeight());
				BufferedImage img = null;
				if(scaledFile == null){
					//ensureScaledCache should make sure this is never the case
					assert false;
				}
				else{
					img = resizer.load(scaledFile);
				}
				return img;
			} catch(SQLException e){
				e.printStackTrace();
			}
			//failsafe ... if db is down, we can still do this.  is it useful?  hmmm...
			return resizer.resizeImageSetProportions(picInfo, (int)scaledSize.getWidth(), (int)scaledSize.getHeight());
		}
	}

	

}
