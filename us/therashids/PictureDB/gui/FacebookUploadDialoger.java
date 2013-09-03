package us.therashids.PictureDB.gui;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import us.therashids.PictureDB.PictureInfo;
import us.therashids.PictureDB.FacebookUploader.FacebookPrompter;

import com.therashids.utils.BareBonesBrowserLaunch;

import com.google.code.facebookapi.schema.Album;

public class FacebookUploadDialoger implements FacebookPrompter {
	
	Frame frame;
	AuthorizeDialog authorizeDialog;
	
	public FacebookUploadDialoger(Frame frame) {
		this.frame = frame;
		authorizeDialog = new AuthorizeDialog(frame);
	}

	public boolean askUpload(PictureInfo p, String otherAlbum) {
		return true;
	}

	public boolean askUpload(PictureInfo p, String otherUser, String album) {
		return true;
	}

	public void authorize(String URL) {
		authorizeDialog.setURL(URL);
		authorizeDialog.setVisible(true);
	}

	public Album chooseAlbum(List<Album> albums) {
		int sz = albums.size();
		if(sz > 0){
			String[] choices = new String[sz + 1];
			for(int i = 0; i < sz;i++){
				choices[i] = albums.get(i).getName();
			}
			choices[sz] = "<Create New Album>";
			String albumName = (String) JOptionPane.showInputDialog(frame,
					"Choose which album to upload into",
					"Choose Album",
					JOptionPane.QUESTION_MESSAGE,
					null,
					choices,
					choices[0]);
			int chosenIndex = -1;
			for(int i = 0; i <= sz; i++){
				if(choices[i].equals(albumName)){
					chosenIndex = i;
					break;
				}						
			}
			if(chosenIndex < sz)
				return albums.get(chosenIndex);
		}
		String name = (String) JOptionPane.showInputDialog(frame, "Enter new album name");
		if(name != null){
			Album a = new Album();
			a.setName(name);
			return a;
		}
		
		
		return null;
	}

	public void notifyAlreadyUploaded(PictureInfo p) {
	}
	
	class AuthorizeDialog extends JDialog {
		String URL;
		JEditorPane edPane;
		
		AuthorizeDialog(Frame frame){
			super(frame, true);
			getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
			JLabel lab = new JLabel("Login to facebook, then click OK.  (If your browser doesn't automatically open, go to the following URL:)");
			add(lab);
			edPane = new JEditorPane();
			edPane.setEditable(false);
			edPane.setContentType("text/html");
			add(edPane);
			JButton ok = new JButton("OK");
			ok.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					setVisible(false);
				}
			});
			add(ok);
		}
		
		void setURL(String url){
			URL = url;
			edPane.setText("<a>" + url + "</a>");
			pack();
		}
		
		@Override
		public void setVisible(boolean b) {
			if(b){
				BareBonesBrowserLaunch.openURL(URL);
			}
			super.setVisible(b);
		}
		
		
		
		
	}

}
