package us.therashids.PictureDB.gui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.TransferHandler;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import us.therashids.PictureDB.ImageCache;
import us.therashids.PictureDB.PictureInfo;

import com.therashids.utils.UniqueList;


/**
 * A JList for displaying images.
 * 
 * Supports drag and drop of images, as well as multiple image sizes.  Commonly used decorations
 * in {@link ImageListPanel}.
 * 
 * @author im
 *
 */
public class ImageList extends JList{
	static final int DEFAULT_WIDTH = 100;
	private static final int IMG_BORDER = 3;
	
	static final PictureInfo genericPic = new PictureInfo(-1, null, null);
	
	public enum Mode {
		UNIQUE,
		ORDERED
	}
		
	ImageListModel model;
	int img_width;
	int imgBwidth;
	
	
	
	public ImageList() {
		this(DEFAULT_WIDTH);
	}
	
	public ImageList(int width) {
		model = new ImageListModel();
		doSetup(model, width);
	}
	
	public ImageList(int width, ImageListModel model){
		if(model == null)
			model = new ImageListModel();
		this.model = model;
		doSetup(model, width);
	}
	
	void doSetup(ImageListModel model, int width){
		setCellRenderer(new ImageCellRenderer());
		setModel(model);
		model.addImageList(this);
		setImgWidth(width);
		setDropMode(DropMode.INSERT);
		setTransferHandler(new ImageTransferHandler(this));
		setDragEnabled(true);
//		DragSource dragSource = DragSource.getDefaultDragSource();
//		dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, new DragSupport());
		setSelectionBackground(Color.RED);
	}
	
	void setImgWidth(int width){
		img_width = width;
		imgBwidth = width + 2 * IMG_BORDER;
		setPrototypeCellValue(null);
		setPrototypeCellValue(genericPic);
		model.fireContentsChanged();
	}
	
	/**
	 * make all the elements of the list selected
	 */
	public void setAllSelected(){
		setSelectionInterval(0, model.imgs.size() - 1);
	}
	
	public void setNoneSelected() {
		setSelectionInterval(-1, -1);
	}
	
	static class ImageListModel extends AbstractListModel {
		
		List<PictureInfo> imgs = new UniqueList<PictureInfo>();
		Mode mode = Mode.UNIQUE;
		List<ImageList> imgLists = new ArrayList<ImageList>();	//TODO would be nice to remove this dependency...
		
		public void addImageList(ImageList il){
			imgLists.add(il);
		}

		public void setPictures(List<PictureInfo> imgs){
			Collections.sort(imgs, PictureInfo.SORT_ASC_DATE);
			if(mode == Mode.UNIQUE && !(imgs instanceof UniqueList)){
					this.imgs.clear();
					this.imgs.addAll(imgs);
			}
			else {
				this.imgs = imgs;
			}
			fireContentsChanged();
		}
		
		public PictureInfo getElementAt(int index) {
			return imgs.get(index);
		}
		
		public int getSize() {
			return imgs.size();
		}
		
		public void remove(PictureInfo pic){
			if(imgs.remove(pic))
				fireContentsChanged();
		}
		
		public void remove(int index){
			imgs.remove(index);
			fireContentsChanged();
		}
		
		public void add(PictureInfo pic){
			imgs.add(pic);
			fireContentsChanged();
		}
		
		public void add(PictureInfo pic, int index) {
			imgs.add(index, pic);
			fireContentsChanged();
		}
		
		void fireContentsChanged(){
			for (ListDataListener l : getListDataListeners()) {
				l.contentsChanged(new ListDataEvent(this,
						ListDataEvent.CONTENTS_CHANGED, 0, imgs.size()));
			}
			
		}
		
	}
	
	class ImageCellRenderer extends JLabel implements ListCellRenderer {
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			PictureInfo pic = (PictureInfo) value;
			ImageIcon ii;
			if(pic.getPictureFile() != null)
				setToolTipText(pic.getPictureFile().toString());
	         if (isSelected) {
	             setBackground(list.getSelectionBackground());
	             setForeground(list.getSelectionForeground());
	         } else {
	             setBackground(list.getBackground());
	             setForeground(list.getForeground());
	         }
			try {
				ii = new ImageIcon(ImageCache.getInst().getImage(pic, this, true, img_width, img_width, IMG_BORDER));
				setIcon(ii);
			} catch (IOException e) {
				System.err.println(pic.getPictureFile());
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			setBorder(BorderFactory.createRaisedBevelBorder());			
			return this;
		}
	}
	
	class ImageTransferHandler extends TransferHandler {
		
		//1)createTransferable
		//2)importData
		//3)exportDone
		
		Component container;
		
		public ImageTransferHandler(Component container) {
			this.container = container;
		}
		
		@Override
		public boolean canImport(TransferSupport ts) {
			for(DataFlavor flv: ts.getDataFlavors()){
				if(flv.equals(PicTransfer.flvr))
					return true;
			}
			return false;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public boolean importData(TransferSupport ts) {
			JList.DropLocation dl = (JList.DropLocation) ts.getDropLocation();
			int toIndex = dl.getIndex();
			Transferable tsf = ts.getTransferable();
			try {
				Object o = tsf.getTransferData(PicTransfer.flvr);
				List<PictureInfo> ps = (List) o;
				for(PictureInfo p: ps)
					model.add(p,toIndex);
				return true;
			} catch (UnsupportedFlavorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return false;
		}
		
		@Override
		public int getSourceActions(JComponent c) {
			return TransferHandler.MOVE;
		}
		
		@Override
		protected Transferable createTransferable(JComponent c) {
			return new PicTransfer(Arrays.asList(((ImageList)c).getSelectedValues()));
		}
		
		@Override
		protected void exportDone(JComponent source, Transferable data,
				int action) {
		}
	}
	
	//TODO switch to pure swing drag and drop, using glass pane to render image, so that we can copy multiple selected images
	class DragSupport implements DragGestureListener, DragSourceListener, ImageObserver {
		public void dragGestureRecognized(DragGestureEvent dge) {
			if (dge != null) {
				Point orig = dge.getDragOrigin();
				int idx = ImageList.this.locationToIndex(orig);
				if(idx < 0) return;
				PictureInfo pic = model.getElementAt(idx);
				Image im = null;
				try {
					im = ImageCache.getInst().getImage(pic, ImageList.this,
							true, img_width, img_width, IMG_BORDER);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(im == null)
					return;
				// Draw the drag image using a BufferedImage
				BufferedImage image;
				if (DragSource.isDragImageSupported()) {
					image = new BufferedImage(img_width, img_width,
							BufferedImage.TYPE_INT_ARGB);
					Graphics2D g = image.createGraphics();
					g.setComposite(AlphaComposite.getInstance(
							AlphaComposite.SRC, 0.75f)); // semitransparent
															// image
					g.drawImage(im, 0, 0, this);
					g.dispose();
				} else {
					image = null;
				}
				// Start drag operation.
				Point where = ImageList.this.indexToLocation(idx);
				where.x = where.x - orig.x;
				where.y = where.y - orig.y;
				dge.startDrag(null, image, where, new PicTransfer(pic), this);
			}
		}
		
		public void dragDropEnd(DragSourceDropEvent dsde) {}
		public void dragEnter(DragSourceDragEvent dsde) {}
		public void dragExit(DragSourceEvent dse) {}
		public void dragOver(DragSourceDragEvent dsde) {}
		public void dropActionChanged(DragSourceDragEvent dsde) {}
		
		
		
		public boolean imageUpdate(Image arg0, int arg1, int arg2, int arg3,
				int arg4, int arg5) {
			return false;
		}
	}


}
