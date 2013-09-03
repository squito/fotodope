package us.therashids.PictureDB.gui;


import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.*;

import info.clearthought.layout.TableLayout;

import us.therashids.PictureDB.PictureImportOptions;
import us.therashids.PictureDB.PictureImportOptions.DateChoice;
import us.therashids.PictureDB.PictureImportOptions.SizeChoice;
import com.therashids.utils.DateChooser;

public class ImportOptionChooser extends JDialog {
	
	JCheckBox photogQ;
	JTextField photogS;
	File chosenFile;
	JTextField file;
	JFileChooser fileChooser;
	JCheckBox recursiveQ;
	
	JRadioButton exifDate;
	JRadioButton manualDate;
	DateChooser manDateField;
	
	JRadioButton exifSize;
	JRadioButton jpegSize;
	JRadioButton imgSize;
	
	
	
	JButton done;

	
	public ImportOptionChooser(Frame owner){
		super(owner, "Picture Import Options", true);
		
		double[][] size = new double[][]{
				{10,100, 20, TableLayout.FILL, 100, 10},
				{10, 20, 2, 20, 10, 20, 2, 20, 10, 20, 10, 
					20, 2, 20, 2, 20,
					10,
					20,2,20,2,20,2,20,
					TableLayout.FILL, 25, 10}
		};
		TableLayout layout = new TableLayout(size);
		setLayout(layout);

		
		JLabel fileL = new JLabel("file:");
		file = new JTextField();
		JButton fileDialogButton = new JButton("Choose ...");
		fileChooser = new JFileChooser();
		final Component c = this;
		fileDialogButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int chosenQ = fileChooser.showOpenDialog(c);
				if(chosenQ != JFileChooser.APPROVE_OPTION)
					return;
		
				chosenFile = fileChooser.getSelectedFile();
				file.setText(chosenFile.getAbsolutePath());
			}
		});
		add(fileL, "1, 1");
		add(file, "1, 3, 3, 3");
		add(fileDialogButton, "4,3");
		
		
		photogQ = new JCheckBox();
		photogS = new JTextField();
		JLabel photoL = new JLabel("photographer");
		
		
		add(photoL, "1,5");
		add(photogQ, "2,5");
		add(photogS, "1,7,4,7");
		
		JLabel recursiveL = new JLabel("include sub-dirs?");
		recursiveQ = new JCheckBox();
		recursiveQ.setSelected(true);
		add(recursiveL, "1,9");
		add(recursiveQ, "2,9");
		
		exifDate = new JRadioButton("Use Exif Date");
		manualDate = new JRadioButton("Manual Date");
		manDateField = new DateChooser();
		ButtonGroup dateGroup = new ButtonGroup();
		dateGroup.add(exifDate);
		dateGroup.add(manualDate);
		exifDate.setSelected(true);
		
		JLabel dateL = new JLabel("Picture Date");
		add(dateL, "1,11");
		add(exifDate, "1,13");
		add(manualDate, "1, 15");
		add(manDateField, "3,15, 4,15");
		
		exifSize = new JRadioButton("Exif Size");
		jpegSize = new JRadioButton("JPEG size");
		imgSize = new JRadioButton("img size");
		ButtonGroup sizeGroup = new ButtonGroup();
		sizeGroup.add(exifSize);
		sizeGroup.add(jpegSize);
		sizeGroup.add(imgSize);
		exifSize.setSelected(true);
		
		
		JLabel sizeL = new JLabel("Picture Size");
		add(sizeL, "1, 17");
		add(exifSize, "1,19");
		add(jpegSize, "1, 21");
		add(imgSize, "1, 23");
		
		
		
		done = new JButton("done");
		done.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		
		add(done, "4,25");
	}
	
	/**
	 * returns the PictureImportOptions from the dialog
	 * 
	 * @return
	 */
	public PictureImportOptions getOptions(){
		PictureImportOptions opts = new PictureImportOptions();
		opts.setImportFile(chosenFile);
		if(photogS.getText().equals(""))
			opts.setPhotographer(null);
		else
			opts.setPhotographer(photogS.getText());
		opts.setRecursive(recursiveQ.isSelected());
		
		if(exifDate.isSelected())
			opts.setDateChoice(DateChoice.ExifDate);
		else if (manualDate.isSelected()){
			opts.setDateChoice(DateChoice.ManualDate);
			opts.setManualDate(manDateField.getDate());
		}
		
		if(exifSize.isSelected())
			opts.setSizeChoice(SizeChoice.ExifSize);
		else if(jpegSize.isSelected())
			opts.setSizeChoice(SizeChoice.JPEGHeaders);
		else if(imgSize.isSelected())
			opts.setSizeChoice(SizeChoice.PicSize);
		
		
		return opts;
	}

}
