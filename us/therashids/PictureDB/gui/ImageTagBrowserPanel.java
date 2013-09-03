package us.therashids.PictureDB.gui;


import java.awt.Dimension;
import java.awt.Frame;
import java.sql.SQLException;
import java.util.*;

import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import com.therashids.TagDag.StringTag;
import com.therashids.TagDag.Tag;
import com.therashids.TagDag.TagBrowser;
import com.therashids.TagDag.TagSet;
import com.therashids.TagDag.UnsupportedTagType;

import us.therashids.PictureDB.DBConnection;
import us.therashids.PictureDB.PictureInfo;

public class ImageTagBrowserPanel extends TagBrowser<PictureInfo> implements TreeSelectionListener {
	
	TestFrame controller;
//	ImageThumbsPanel imagePanel;
	ImageListPanel imagePanel;
	
	public ImageTagBrowserPanel(TestFrame owner, TagSet<PictureInfo> theTagSet) {
		super(owner, theTagSet);
		controller = owner;
		addTreeSelectionListener(this);
	}
	
//	public void setPanel(ImageThumbsPanel panel){
//		imagePanel = panel;
//	}
	
	public void setPanel(ImageListPanel panel){
		imagePanel = panel;
	}
	
	public void valueChanged(TreeSelectionEvent tse) {
		try{
			StringTag selected = (StringTag) tse.getPath().getLastPathComponent();
			List<PictureInfo> l = new ArrayList<PictureInfo>();
			for(PictureInfo p: tagSet.getTaggableWithTag(selected)){
				l.add(p);
			}
//			controller.setThumbSet(l, imagePanel);
			imagePanel.setPictures(l);
		} catch(SQLException ex){
			ex.printStackTrace();
		}
	}
	
	
}
