package us.therashids.PictureDB.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import us.therashids.PictureDB.PictureInfo;
import us.therashids.PictureDB.gui.ImageList.ImageListModel;


/**
 * an {@link ImageList}, with a few extra decorations around it.
 * 
 * @author im
 *
 */
public class ImageListPanel extends JPanel {
	
	JLabel numPictures;
	JSlider iconSize;
	ImageList imgList;
	
	public static final Map<Integer, String> sizeChoices = new TreeMap<Integer, String>();
	static{
		sizeChoices.put(50,"tiny");
		sizeChoices.put(100, null);
		sizeChoices.put(200,"med");
		sizeChoices.put(400,null);
		sizeChoices.put(600,"big");
	}
	int[] sliderPosToIconSize;
	
	public ImageListPanel(){
		this(null);
	}
	
	public ImageListPanel(ImageListModel model){
		
		JPanel decorations = new JPanel();
		numPictures = new JLabel("0 pictures");
		decorations.add(numPictures);
		
		Dictionary<Integer, Component> labelTable = new Hashtable<Integer, Component>();
		sliderPosToIconSize = new int[sizeChoices.size()];
		int i = 0;
		for(Entry<Integer, String> e: sizeChoices.entrySet()){
			sliderPosToIconSize[i] = e.getKey();
			if(e.getValue() != null){
				labelTable.put(i, new JLabel(e.getValue()));
			}
			i++;
		}
		iconSize = new JSlider(JSlider.HORIZONTAL, 0, sizeChoices.size() - 1, 1);
		iconSize.setLabelTable(labelTable);
		iconSize.setMinorTickSpacing(1);
		iconSize.setPaintLabels(true);
		iconSize.setPaintTicks(true);
		iconSize.setSnapToTicks(true);
		iconSize.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
			    JSlider source = (JSlider)e.getSource();
			    if (!source.getValueIsAdjusting()) {
			        int idx = (int)source.getValue();
			        imgList.setImgWidth(sliderPosToIconSize[idx]);
			    }
			}
		});
		
		decorations.add(new JLabel("     img size:"));
		decorations.add(iconSize);
		
		setLayout(new BorderLayout());
		add(decorations, BorderLayout.NORTH);
		
		imgList = new ImageList(ImageList.DEFAULT_WIDTH,model);
		imgList.model.addListDataListener(new ListDataListener(){
			public void contentsChanged(ListDataEvent e) {
				updateNumPictures();
			}
			public void intervalAdded(ListDataEvent e) {
				updateNumPictures();
			}
			public void intervalRemoved(ListDataEvent e) {
				updateNumPictures();
			}
		});
		add(new JScrollPane(imgList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
		
	}
	
	public void setPictures(List<PictureInfo> pics){
		imgList.model.setPictures(pics);
		updateNumPictures(pics.size());
	}
	
	void updateNumPictures(){
		updateNumPictures(imgList.model.imgs.size());
	}
	
	void updateNumPictures(int numPics){
		String label = numPics + " picture" + (numPics == 1 ? "" : "s");
		numPictures.setText(label);		
	}
	
	public List<PictureInfo> getPics() {
		return imgList.model.imgs;
	}
}
