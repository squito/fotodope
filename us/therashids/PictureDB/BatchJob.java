package us.therashids.PictureDB;

import us.therashids.PictureDB.gui.BatchProgress;

public interface BatchJob extends Runnable {
	
	/**
	 * set the driver object, so it can register callbacks
	 * @param bp
	 */
	public void setBatchProgress(BatchProgress bp);
	
	
	/**
	 * returns the total amount of work required for this batch job,
	 * from start to finish, in whatever arbitrary units. 
	 * @return
	 */
	public int getTotalWork();
	
	/**
	 * asks the job to pause itself, at next convenient time
	 */
	public void setPaused();
	
	/**
	 * asks the job to start again (after pausing).
	 *
	 */
	public void setPlay();

}
