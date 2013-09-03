package us.therashids.PictureDB;

import java.io.File;
import java.util.Calendar;

/**
 * stores options for importing pictures
 * 
 * 
 * @author Imran Rashid
 *
 */
public class PictureImportOptions {
	
	String photographer;
	File importFile;
	boolean recursive;
	DateChoice dateChoice;
	Calendar manualDate;
	SizeChoice sizeChoice;
	
	public enum DateChoice {
		/**
		 * trust the date in the EXIF data
		 */
		ExifDate,
		/**
		 * use the last time the file was modified
		 */
		FileModDate,
		/**
		 * user manually specifies the date
		 */
		ManualDate}
	
	public enum SizeChoice {
		/**
		 * use the size of the picture specified in the EXIF data
		 */
		ExifSize,
		/**
		 * if the picture is a JPEG, use the JPEG headers
		 */
		JPEGHeaders,
		/**
		 * read in the picture file as an image, and query its size
		 */
		PicSize}
	
	
	public String getPhotographer() {
		return photographer;
	}
	
	public void setPhotographer(String photographer) {
		this.photographer = photographer;
	}
	
	public boolean isRecursive() {
		return recursive;
	}
	
	public void setRecursive(boolean recursive) {
		this.recursive = recursive;
	}
	
	public File getImportFile() {
		return importFile;
	}
	
	public void setImportFile(File importFile) {
		this.importFile = importFile;
	}

	public DateChoice getDateChoice() {
		return dateChoice;
	}

	public void setDateChoice(DateChoice dateChoice) {
		this.dateChoice = dateChoice;
	}

	public Calendar getManualDate() {
		return manualDate;
	}

	public void setManualDate(Calendar manualDate) {
		this.manualDate = manualDate;
	}

	public SizeChoice getSizeChoice() {
		return sizeChoice;
	}

	public void setSizeChoice(SizeChoice sizeChoice) {
		this.sizeChoice = sizeChoice;
	}
	
}
