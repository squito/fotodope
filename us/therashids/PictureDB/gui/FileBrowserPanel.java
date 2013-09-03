package us.therashids.PictureDB.gui;

import java.sql.SQLException;
import java.util.*;
import java.awt.Dimension;
import java.io.File;
import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;

import us.therashids.PictureDB.DBConnection;
import us.therashids.PictureDB.PictureInfo;

/**
 * A tree that displays ALL directories, and selection events update the db view
 * @author Imran Rashid
 *
 */
public class FileBrowserPanel extends JScrollPane implements TreeWillExpandListener, TreeSelectionListener{
	
	JTree tree;
	FileNode root;
	FileNode allDirs;
	FileNode importedDirs;
	TreePath currentPath;
	ImageListPanel imgList;

	public FileBrowserPanel(ImageListPanel imgList){
		super(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		this.imgList = imgList;
		root = new FileNode("root");
		allDirs = new FileNode("All Directories");
		importedDirs = new FileNode("Imported Directories");
		root.add(importedDirs);
		root.add(allDirs);
		tree = new JTree(root);
		tree.setRootVisible(false);
		tree.addTreeWillExpandListener(this);
		tree.addTreeSelectionListener(this);
		setupAllDirSubtree();
		setupImportedSubtree();
		setViewportView(tree);
		setMinimumSize(new Dimension(100,100));
		setPreferredSize(new Dimension(200,400));
	}
	
	private void setupAllDirSubtree(){
		for(File f: File.listRoots()){
			allDirs.add(new FileNode(f, true));
		}
		allDirs.expanded = true;
	}
	
	/**
	 * get all the catalogued dirs from the database, build a tree out of them
	 *
	 */
	private void setupImportedSubtree(){
		try{
			Set<File> dirs = DBConnection.getInstance().getDirectories();
			HashMap<File, FileNode> nameToNode = new HashMap<File, FileNode>();
			for(File f: dirs){
				File parent = f.getParentFile();
				if(dirs.contains(parent)){
					//has a parent -- add it to the parent, creating all necessary nodes as needed
					FileNode parentNode;
					if(nameToNode.containsKey(parent))
						parentNode = nameToNode.get(parent);
					else{
						parentNode = new FileNode(parent);
						parentNode.expanded = true;
						nameToNode.put(parent, parentNode);
					}
					FileNode thisNode;
					if(nameToNode.containsKey(f))
						thisNode = nameToNode.get(f);
					else{
						thisNode = new FileNode(f);
						thisNode.expanded = true;
						nameToNode.put(f, thisNode);
					}
					parentNode.add(thisNode);
				}
				else{
					//has no parent -- add it to the top level
					FileNode thisNode = nameToNode.get(f);
					if(thisNode == null){
						thisNode = new FileNode(f);
						thisNode.expanded = true;
						nameToNode.put(f,thisNode);
					}
					importedDirs.add(thisNode);
				}
			}
			nameToNode = null;
			dirs = null;
			importedDirs.sortChildren();
		} catch(SQLException e){
		}
		
	}
	
	void deleteEmptyDirs() throws SQLException {
		DBConnection db = DBConnection.getInstance();
		Set<File> dirs = DBConnection.getInstance().getDirectories();
		for(File d: dirs){
			removeEmptyAndParents(d, db);
		}
	}
	
	private void removeEmptyAndParents(File dir, DBConnection db) throws SQLException {
		if(dir == null)
			return;
		if(db.getNumPicsInDirectory(dir) == 0){
			try{
				db.removeDirectory(dir);
			} catch(SQLException ex){
				System.out.println(dir);
			}
			if(dir.list().length == 0)
				dir.delete();
			removeEmptyAndParents(dir.getParentFile(), db);
		}
	}
	
	
	//Dir tree is lazily filled in
	@SuppressWarnings("unchecked")
	public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
		currentPath = event.getPath();
		FileNode node = (FileNode) currentPath.getLastPathComponent();
		Enumeration<FileNode> children = node.children();
		while(children.hasMoreElements()){
			FileNode child = children.nextElement();
			if(child.expanded)
				continue;
			File[] dirContents = child.f.listFiles();
			if(dirContents != null && dirContents.length != 0){
				for(File sf: dirContents){
					if(sf.isDirectory()){
						child.add(new FileNode(sf));
					}
				}
			}
			child.expanded = true;
		}
	}
	
	/**
	 * called when the directory structure has changed (eg., after an import), so the display updates
	 * 
	 * probably should be redesigned
	 */
	public void updateTree(){
		//extremely inefficient -- we should just get notified about where the changes are
		setupImportedSubtree();
		
		TreeModelEvent e = new TreeModelEvent(this, new Object[]{root});
		DefaultTreeModel m = ((DefaultTreeModel)tree.getModel());
		for(TreeModelListener l: m.getTreeModelListeners()){
			l.treeStructureChanged(e);
		}
	}
	
	/*
	 * don't do anything when the tree is collapsed
	 */
	public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
	}
	
	public void valueChanged(TreeSelectionEvent e) {
		currentPath = e.getPath();
		updateThumbpanel();
	}
	
	/**
	 * resets the thumbpanel based on the currently selected directory
	 *
	 */
	public void updateThumbpanel(){
		if(currentPath == null)
			imgList.setPictures(new ArrayList<PictureInfo>());
		else{
			FileNode node = (FileNode)currentPath.getLastPathComponent();
			if(node.inDB){
				try{
					List<PictureInfo> pics = DBConnection.getInstance().getPictureInDirectory(node.f);
					imgList.setPictures(pics);
				}catch (SQLException ex){
					ex.printStackTrace();
				}
			}
		}
	}
	
	class FileNode extends DefaultMutableTreeNode implements Comparable<FileNode>{
		final File f;
		boolean expanded = false;
		boolean inDB = false;
		
		FileNode(String s){
			super(s);
			f = null;
		}
		
		FileNode(File f){
			super(f.getName());
			this.f = f;
			try{
				inDB = DBConnection.getInstance().isDirectoryInDB(f);
			} catch(SQLException e){
				e.printStackTrace();
			}
		}
		
		FileNode(File f, boolean root){
			super(f.toString());
			this.f = f;
		}
		
		void sortChildren(){
			List<FileNode> children = new ArrayList<FileNode>();
			for(int i = 0; i < getChildCount(); i++){
				children.add((FileNode)getChildAt(i));
			}
			Collections.sort(children);
			removeAllChildren();
			for(FileNode n: children){
				add(n);
				n.sortChildren();
			}
		}
		
		public int compareTo(FileNode o) {
			if(f == null && o.f == null){
				return 0;
			}
			else if(f == null)
				return 1;
			else if(o.f == null)
				return -1;
			else{
				//if last component of file name is number, then order numerically
				boolean isNumber1 = true, isNumber2 = true;
				int n1 = 0, n2 = 0;
				try{
				n1 = Integer.parseInt(f.getName());
				}catch(NumberFormatException ex){
					isNumber1 = false;
				}
				try{
				n2 = Integer.parseInt(o.f.getName());
				}catch(NumberFormatException ex){
					isNumber2 = false;
				}
				
				if(isNumber1){
					if(isNumber2)
						return n1 - n2;
					else
						return -1;
				}
				else{
					if(isNumber2)
						return 1;
					else
						return f.getAbsolutePath().compareTo(o.f.getAbsolutePath());
				}
			}
		}
	}
	
}
