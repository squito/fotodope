package us.therashids.PictureDB.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.*;

import us.therashids.PictureDB.BatchJob;

public class BatchProgress extends JFrame{
	
	BlockingQueue<BatchJob> jobs;
	BatchJob currentJob;
	int currentJobTotalWork;
	int currentJobWorkDone;
	BatchWorker t;
	boolean paused = false;
	
	JProgressBar progress;
	JToggleButton pause, play;
	JLabel queueLength;
	
	JTextArea currentStatus;
	
	private class BatchWorker extends Thread{
		BatchProgress bp;
		
		public BatchWorker(String name, BatchProgress bp) {
			super(name);
			this.bp = bp;
			start();
		}
		
		public void run() {
			try {
				synchronized (this) {
					while (!isInterrupted()) {
						if (paused) {
							this.wait(1000);
						}
						else {
							//BUG somehow, pausing messes up the counter -- it doesn't reach 100%
							currentJob = jobs.take();
							currentJob.setBatchProgress(bp);
							currentJobTotalWork = currentJob.getTotalWork();
							bp.newRunningJob(currentJob);
							currentJob.run();
							currentJob = null;
							queueLength.setText(Integer.toString(jobs.size()));
						}
					}
				}
			}	catch(InterruptedException ex) {}
		}
	}
	
	public BatchProgress() {
		super("Batch Progress");
		setSize(300, 200);
		jobs = new LinkedBlockingQueue<BatchJob>();
		t = new BatchWorker("batch_thread", this);
		
		progress = new JProgressBar();
		progress.setStringPainted(true);
		pause = new JToggleButton("pause");
		pause.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				paused = true;
				pause.setSelected(true);
				play.setSelected(false);
				currentJob.setPaused();
			}
		});
		play = new JToggleButton("play");
		play.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				paused = false;
				pause.setSelected(false);
				play.setSelected(true);
				currentJob.setPlay();
			}
		});
		play.setSelected(true);
		
		JPanel queuePanel = new JPanel();
		queuePanel.setLayout(new FlowLayout());
		queuePanel.add(new JLabel("Number of Jobs in Queue:"));
		queueLength = new JLabel("0");
		queuePanel.add(queueLength);
		
		getContentPane().add(queuePanel, BorderLayout.NORTH);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());
		buttonPanel.add(pause);
		buttonPanel.add(play);
		
		currentStatus = new JTextArea();
		currentStatus.setLineWrap(true);
		buttonPanel.add(currentStatus);
		
		
		
		getContentPane().add(buttonPanel, BorderLayout.CENTER);
		
		getContentPane().add(progress, BorderLayout.SOUTH);
	}
	
	public void addJob(BatchJob r){
		try{
			jobs.put(r);
		} catch(InterruptedException e){
			//TODO
		}
		int numJobs = jobs.size();
		if(currentJob != null)
			numJobs++;
		queueLength.setText(Integer.toString(numJobs));
		queueLength.repaint();
		setVisible(true);
	}
	
	void newRunningJob(BatchJob j){
		progress.setMaximum(j.getTotalWork());
		currentJobWorkDone = 0;
		updateProgressBar();
	}
	
	public void setJobValue(int v){
		currentJobWorkDone = v;
		updateProgressBar();
	}
	
	public void incrementJobValue(){
		currentJobWorkDone++;
		updateProgressBar();
	}
	
	public void incrementJobValue(int incr){
		currentJobWorkDone += incr;
		updateProgressBar();
	}
	
	/**
	 * set a message to be displayed about what is currently being done.
	 * Note that this is NOT automatically cleared, so a job must clear it itself
	 * when done processing.
	 * @param status
	 */
	public void setCurrentStatusText(String status){
		currentStatus.setText(status);
	}
	
	void updateProgressBar(){
		progress.setValue(currentJobWorkDone);
	}

}
