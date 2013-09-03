/**
 * 
 */
package us.therashids.PictureDB.gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import us.therashids.PictureDB.PictureInfo;

public class PicTransfer implements Transferable {
	List<PictureInfo> pics;
	static final DataFlavor flvr = new PicTransferFlavor(PicTransfer.class, "Pictures");
	PicTransfer(PictureInfo pic){
		this.pics = Collections.singletonList(pic);
	}
	
	PicTransfer(List pics){
		this.pics = pics;
	}
	
//	PicTransfer(List<PictureInfo> pics){
//		this.pics = pics;
//	}
	
	public Object getTransferData(DataFlavor arg0)
			throws UnsupportedFlavorException, IOException {
		return pics;
	}
	
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[]{PicTransfer.flvr};
	}
	
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		if(flavor.equals(PicTransfer.flvr))
			return true;
		return false;
	}
	
    public static class PicTransferFlavor extends DataFlavor {
    	public PicTransferFlavor(Class<PicTransfer> class1, String string) {
    		super(class1, string);
		}

		@Override
    	public boolean equals(DataFlavor df) {
    		return (df instanceof PicTransferFlavor);
    	}
    }

}