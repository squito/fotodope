package us.therashids.PictureDB;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import us.therashids.PictureDB.gui.BatchProgress;

/**
 * Convenient helper for setting up a thread to process a list of PictureInfos.
 * 
 * @author Imran Rashid
 *
 */
public abstract class BatchJobAdapter<T> implements BatchJob {
	
	List<T> l;
	BatchProgress bp;
	boolean paused = false;
	
	public void setList(List<T> l){
		this.l = l;
	}

	public int getTotalWork() {
		return l.size();
	}

	public void setBatchProgress(BatchProgress bp) {
		this.bp = bp;
	}

	public void setPaused() {
		paused = true;
	}
	
	public void setPlay() {
		paused = false;
	}

	public void run() {
		try{
			aboutToProcess();
		} catch (Exception e){
			//TODO real error handling!
			e.printStackTrace();

		}
		for(T p: l){
			synchronized (this) {
				if (paused) {
					try {
						this.wait(1000); // check once a second for unpause
					} catch (InterruptedException e) {

					}
				}
				else {
					try {
						processOneItem(p);
					} catch (Exception e) {
						// TODO real error handling!
						e.printStackTrace();
					}
					bp.incrementJobValue();
				}
			}
		}
		try{
			allDone();
		} catch (Exception e){
			//TODO real error handling!
			e.printStackTrace();

		}
	}
	
	/**
	 * do whatever processing you want on one picture.
	 * @param pic
	 */
	public abstract void processOneItem(T p) throws IOException, SQLException;
	
	/**
	 * gets called when all the pictures are about to be processed
	 *
	 */
	public abstract void aboutToProcess() throws IOException, SQLException;
	
	/**
	 * gets called once all the pictures have been processed
	 *
	 */
	public abstract void allDone() throws IOException, SQLException;

}
