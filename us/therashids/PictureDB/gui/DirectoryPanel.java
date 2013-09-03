package us.therashids.PictureDB.gui;

import javax.swing.*;
import javax.swing.tree.*;


public class DirectoryPanel extends JPanel {
	
	private static final String rootName = "All indexed directories";
	
	
	MutableTreeNode directoryRootNode;
	TreeModel directoryTreeModel;
	JTree directoryTree;
	
	public void initContent(){
        directoryRootNode = new DefaultMutableTreeNode(rootName);
        directoryTreeModel = new DefaultTreeModel(directoryRootNode);
        directoryTree = new JTree(directoryTreeModel);
	}

}
