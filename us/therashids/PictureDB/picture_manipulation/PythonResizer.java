package us.therashids.PictureDB.picture_manipulation;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.*;

import us.therashids.PictureDB.PictureInfo;

public class PythonResizer {
	
	private static final String python_image_file = "C:\\Documents and Settings\\Imran Rashid\\workspace\\PhotoDB\\us\\therashids\\PictureDB\\picture_manipulation\\PythonImager.py";
	private static final String resizeConstrainedCommand = "resizeImageConstrainedSize";
	private static final String resizeMaxSideCommand = "resizeImageMaxSide";
	
	public static void makeThumbnail(PictureInfo info, int maxSide)
			throws IOException {
		List<String> cmd = new ArrayList<String>();
		cmd.add("python");
		cmd.add("\"" + python_image_file + "\"");
		cmd.add(resizeMaxSideCommand);
		cmd.add("\"" + info.getPictureFile().getAbsolutePath() + "\"");
		cmd.add("\"" + info.getThumbFile().getAbsolutePath() + "\"");
		cmd.add(Integer.toString(maxSide));
		cmd.add(Integer.toString(info.getOrientation()));
		Process p = new ProcessBuilder(cmd).start();

//		String cmd = "python \"" + python_image_file + "\" " + resizeMaxSideCommand
//				+ " \"" + info.getPictureFile().getAbsolutePath() + "\" \""
//				+ info.getThumbFile().getAbsolutePath() + "\" "
//				+ Integer.toString(maxSide) + " "
//				+ Integer.toString(info.getOrientation());
//		Process p = Runtime.getRuntime().exec(cmd, null, null);
		try {
			p.waitFor();
		} catch (InterruptedException ex) {

		}
	}
	
	public static void makeConstrainedImage(PictureInfo info, Dimension constraints, File outFile) throws IOException{
		List<String> cmd = new ArrayList<String>();
		cmd.add("python");
		cmd.add("\"" + python_image_file + "\"");
		cmd.add(resizeConstrainedCommand);
		cmd.add("\"" + info.getPictureFile().getAbsolutePath() + "\"");
		cmd.add("\"" + outFile.getAbsolutePath() + "\"");
		cmd.add(Double.toString(constraints.getWidth()));
		cmd.add(Double.toString(constraints.getHeight()));
		cmd.add(Integer.toString(info.getOrientation()));
		Process p = new ProcessBuilder(cmd).start();
		try {
			p.waitFor();
		} catch (InterruptedException ex) {

		}
	}
	
	
	

}
