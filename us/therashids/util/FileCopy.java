package us.therashids.util;




import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileCopy {
  public static void main(String[] args) {
    try {
      copy("fromFile.txt", "toFile.txt");
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
  }
  
  
  public static void copy(String fromFileName, String toFileName)
  throws IOException {
File fromFile = new File(fromFileName);
File toFile = new File(toFileName);
  }


    public static void copy(File fromFile, File toFile) throws IOException{
  	  

    if (!fromFile.exists())
      throw new IOException("FileCopy: " + "no such source file: "
          + fromFile.getAbsolutePath());
    if (!fromFile.isFile())
      throw new IOException("FileCopy: " + "can't copy directory: "
          + fromFile.getAbsolutePath());
    if (!fromFile.canRead())
      throw new IOException("FileCopy: " + "source file is unreadable: "
          + fromFile.getAbsolutePath());

    if (toFile.isDirectory())
      toFile = new File(toFile, fromFile.getName());

    if (toFile.exists()) {
      if (!toFile.canWrite())
        throw new IOException("FileCopy: "
            + "destination file is unwriteable: " + toFile.getAbsolutePath());
      System.out.print("Overwrite existing file " + toFile.getName()
          + "? (Y/N): ");
      System.out.flush();
      BufferedReader in = new BufferedReader(new InputStreamReader(
          System.in));
      String response = in.readLine();
      if (!response.equals("Y") && !response.equals("y"))
        throw new IOException("FileCopy: "
            + "existing file was not overwritten.");
    } else {
      String parent = toFile.getParent();
      if (parent == null)
        parent = System.getProperty("user.dir");
      File dir = new File(parent);
      if (!dir.exists())
        throw new IOException("FileCopy: "
            + "destination directory doesn't exist: " + parent);
      if (dir.isFile())
        throw new IOException("FileCopy: "
            + "destination is not a directory: " + parent);
      if (!dir.canWrite())
        throw new IOException("FileCopy: "
            + "destination directory is unwriteable: " + parent);
    }

    FileInputStream from = null;
    FileOutputStream to = null;
    try {
      from = new FileInputStream(fromFile);
      to = new FileOutputStream(toFile);
      byte[] buffer = new byte[4096];
      int bytesRead;

      while ((bytesRead = from.read(buffer)) != -1)
        to.write(buffer, 0, bytesRead); // write
    } finally {
      if (from != null)
        try {
          from.close();
        } catch (IOException e) {
          ;
        }
      if (to != null)
        try {
          to.close();
        } catch (IOException e) {
          ;
        }
    }
  }
}