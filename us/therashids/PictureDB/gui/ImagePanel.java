package us.therashids.PictureDB.gui;

/*
 * $Id: ImagePanel.java,v 1.9 2002/04/04 20:55:48 eerruu Exp $
 * 
 * Copyright (c) 2002 Erich Steiner, Switzerland
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.sql.SQLException;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.therashids.TagDag.TagSet;

import us.therashids.PictureDB.gui.SlideshowViewer.Mode;


class ImagePanel extends JPanel{

    private Image image;

    private JPopupMenu popup;
    private JMenuItem menuItemPrev;
    private JMenuItem menuItemNext;
    private JMenuItem menuItemPlayR;
    private JMenuItem menuItemPlayF;
    private JMenuItem menuItemStop;
    private JMenuItem menuItemClose;
    private JCheckBoxMenuItem menuItemShowPlayerControl;
    private JCheckBoxMenuItem menuItemFullScreen;
    
    final SlideshowViewer slideshow;
    
    ImagePanel(SlideshowViewer sl){
    	super();
    	this.slideshow = sl;
        Action right = new AbstractAction(){
        	public void actionPerformed(ActionEvent e) {
        		slideshow.requestNextImage();
        	}
        };
        Action left = new AbstractAction(){
        	public void actionPerformed(ActionEvent e) {
        		slideshow.requestPreviousImage();
        	}
        };
        Action stop = new AbstractAction(){
        	public void actionPerformed(ActionEvent e) {
        		slideshow.setMode(Mode.OFF);
        	}
        };
        Action delete = new AbstractAction(){
        	public void actionPerformed(ActionEvent e) {
        		int choice = JOptionPane.showConfirmDialog(ImagePanel.this, "Delete this picture?", "Delete Picture", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        		if(choice == JOptionPane.OK_OPTION){
        			slideshow.requestDelete();
        		}
        	}
        };
        Action tag = new AbstractAction(){
        	public void actionPerformed(ActionEvent e) {
        		Tagger t = slideshow.getTagger();
        		t.setVisible(true);
        		String tagList = t.getValidatedText();
        		if (tagList != null) {
					Collection<String> tags = TagSet
							.tokenizeStringListTags(tagList);
					try {
						slideshow.addTags(tags);
					} catch (SQLException ex) {
						// TODO
						ex.printStackTrace();
					}
				}
        	}
        };
        Action save = new AbstractAction(){
        	public void actionPerformed(ActionEvent e) {
        		slideshow.requestSave();
        		repaint();
        	}
        };
        Action unsave = new AbstractAction(){
        	public void actionPerformed(ActionEvent e) {
        		slideshow.requestUnsave();
        		repaint();
        	}
        };
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "right");
		getActionMap().put("right", right);
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "left");
		getActionMap().put("left", left);
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "stop");
		getActionMap().put("stop", stop);
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
		getActionMap().put("delete", delete);
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "tag");
		getActionMap().put("tag", tag);
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), "save");
		getActionMap().put("save", save);
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_U, 0), "unsave");
		getActionMap().put("unsave", unsave);
		
	}

//    ImagePanel(PlayerModel playerModelx){
//        super();
//        this.playerModel = playerModelx;
//
//        setLayout(null);
//
//        playerControlPanel = new PlayerControlPanel(playerModel);
//        add(playerControlPanel);
//        Dimension size = playerControlPanel.getPreferredSize();
//        playerControlPanel.setBounds(5, 5, size.width, size.height);
//
//        initPopupMenus();
//
//        playerModel.addChangeListener(new ChangeListener(){
//            public void stateChanged(ChangeEvent e){
//                boolean playing = playerModel.getImageInc() != 0;
//                menuItemPlayR.setVisible(!playing);
//                menuItemStop.setVisible(playing);
//                menuItemPlayF.setVisible(!playing);
//
//                menuItemFullScreen.setSelected(playerModel.getDisplayMode() == PlayerModel.DISPLAY_FULL_SCREEN);
//                menuItemShowPlayerControl.setSelected(playerModel.getControlVisibleFlag());
//
//            }
//
//
//        });
//
//        addComponentListener(new ComponentAdapter(){
//            public void componentResized(ComponentEvent e){
//                movePlayerControlPanelToVisibleArea();
//            }
//
//
//        });
//
//
//    }


    void setImage(Image image){
        this.image = image;
        repaint();
        revalidate();
    }


    public Dimension getPreferredSize(){
        if(image == null){
            return super.getPreferredSize();
        }
        else{
            return new Dimension(image.getWidth(null), image.getHeight(null));
        }
    }


    public void paintComponent(Graphics g){
        g.setColor(Color.black);
        g.fillRect(0, 0, getWidth(), getHeight());

        if(image != null){
            g.drawImage(image, (getWidth() - image.getWidth(null)) / 2, (getHeight() - image.getHeight(null)) / 2, this);
            if(slideshow.isInSaveList()) {
            	g.setColor(Color.WHITE);
            	g.drawString("Saved", 10, 10);
            }
        }
    }


//    private void initPopupMenus(){
//        popup = new JPopupMenu();
//
//        menuItemPrev = new JMenuItem(Msg.get("menuItem.prevImage"));
//        menuItemPrev.setIcon(Util.getImageIcon("icons/prev.png"));
//        menuItemPrev.addActionListener(new ActionListener(){
//            public void actionPerformed(ActionEvent e){
//                playerModel.setImageIndex(playerModel.getNextImageIndex(-1));
//            }
//
//
//        });
//        popup.add(menuItemPrev);
//
//        menuItemNext = new JMenuItem(Msg.get("menuItem.nextImage"));
//        menuItemNext.setIcon(Util.getImageIcon("icons/next.png"));
//        menuItemNext.addActionListener(new ActionListener(){
//            public void actionPerformed(ActionEvent e){
//                playerModel.setImageIndex(playerModel.getNextImageIndex(1));
//            }
//
//
//        });
//        popup.add(menuItemNext);
//
//        menuItemPlayF = new JMenuItem(Msg.get("menuItem.playForward"));
//        menuItemPlayF.setIcon(Util.getImageIcon("icons/playf.png"));
//        menuItemPlayF.addActionListener(new ActionListener(){
//            public void actionPerformed(ActionEvent e){
//                playerModel.setImageInc(1);
//            }
//
//
//        });
//        popup.add(menuItemPlayF);
//
//        menuItemPlayR = new JMenuItem(Msg.get("menuItem.playReverse"));
//        menuItemPlayR.setIcon(Util.getImageIcon("icons/playr.png"));
//        menuItemPlayR.addActionListener(new ActionListener(){
//            public void actionPerformed(ActionEvent e){
//                playerModel.setImageInc(-1);
//            }
//
//
//        });
//        popup.add(menuItemPlayR);
//
//        menuItemStop = new JMenuItem(Msg.get("menuItem.stop"));
//        menuItemStop.setIcon(Util.getImageIcon("icons/stop.png"));
//        menuItemStop.addActionListener(new ActionListener(){
//            public void actionPerformed(ActionEvent e){
//                playerModel.setImageInc(0);
//            }
//
//
//        });
//        popup.add(menuItemStop);
//
//        popup.addSeparator();
//
//        menuItemShowPlayerControl = new JCheckBoxMenuItem(Msg.get("menuItem.showPlayerControl"));
//        menuItemShowPlayerControl.addActionListener(new ActionListener(){
//            public void actionPerformed(ActionEvent e){
//                playerModel.setControlVisibleFlag(menuItemShowPlayerControl.isSelected());
//            }
//
//
//        });
//        popup.add(menuItemShowPlayerControl);
//
//        menuItemFullScreen = new JCheckBoxMenuItem(Msg.get("menuItem.fullScreen"));
//        menuItemFullScreen.addActionListener(new ActionListener(){
//            public void actionPerformed(ActionEvent e){
//                playerModel.setDisplayMode(menuItemFullScreen.isSelected() ? PlayerModel.DISPLAY_FULL_SCREEN : PlayerModel.DISPLAY_FRAME);
//            }
//
//
//        });
//        popup.add(menuItemFullScreen);
//
//        popup.addSeparator();
//
//        menuItemClose = new JMenuItem(Msg.get("menuItem.close"));
//        menuItemClose.addActionListener(new ActionListener(){
//            public void actionPerformed(ActionEvent e){
//                playerModel.setExitFlag(true);
//            }
//
//
//        });
//        popup.add(menuItemClose);
//
//
//
//        // Add listener to components that can bring up popup menus.
//        MouseListener popupListener = new PopupListener();
//        addMouseListener(popupListener);
//    }


//    private void movePlayerControlPanelToVisibleArea(){
//        Point p = playerControlPanel.getLocation();
//        Dimension size = playerControlPanel.getSize();
//        if(p.x + size.width > getWidth()){
//            p.x = getWidth() - size.width;
//            if(p.x < 0){
//                p.x = 0;
//            }
//        }
//        if(p.y + size.height > getHeight()){
//            p.y = getHeight() - size.height;
//            if(p.y < 0){
//                p.y = 0;
//            }
//        }
//        playerControlPanel.setLocation(p);
//    }


    private class PopupListener extends MouseAdapter{
        public void mousePressed(MouseEvent e){
            if(e.isPopupTrigger()){
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
            else if(e.getClickCount() == 1 && e.getModifiers() == e.BUTTON1_MASK){
                if(popup.isVisible()){
                    // popup hide (done automaticly)
                }
                else{
//                    if(playerModel.getImageInc() == 0){
//                        playerModel.setImageIndex(playerModel.getImageIndex() + 1);
//                    }
                }
            }
        }


        public void mouseReleased(MouseEvent e){
            if(e.isPopupTrigger()){
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }


    }


}

