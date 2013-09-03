package us.therashids.PictureDB.gui;


import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.JTextField;

import com.therashids.TagDag.Tag;
import com.therashids.utils.AutoCompleteField;
import com.therashids.utils.Trie;

import java.beans.*; //property change stuff
import java.awt.*;
import java.awt.event.*;


/**
 * 
 * based on http://java.sun.com/docs/books/tutorial/uiswing/examples/components/DialogDemoProject/src/components/CustomDialog.java
 * 
 * @author Imran Rashid
 *
 */
public class Tagger extends JDialog
implements ActionListener,
PropertyChangeListener {
	private String typedText = null;
	private AutoCompleteField<Tag> textField;

	private JOptionPane optionPane;

	private String btnString1 = "Enter";
	private String btnString2 = "Cancel";

	/**
	 * Returns null if the typed string was invalid;
	 * otherwise, returns the string as the user entered it.
	 */
	public String getValidatedText() {
		if(typedText == null || typedText.equals(""))
			return null;
		return typedText;
	}

	/** Creates the reusable dialog. */
	public Tagger(Frame aFrame, Trie<Tag> trie) {
		super(aFrame, true);

		setTitle("Add Tags");

		textField = new AutoCompleteField<Tag>(trie);
		Object[] array = {"Enter tags, space separated (or use quotes around tags with spaces)", textField};


		//Create an array specifying the number of dialog buttons
		//and their text.
		Object[] options = {btnString1, btnString2};

		//Create the JOptionPane.
		optionPane = new JOptionPane(array,
				JOptionPane.QUESTION_MESSAGE,
				JOptionPane.YES_NO_OPTION,
				null,
				options,
				options[0]);

		//Make this dialog display it.
		setContentPane(optionPane);

		//Handle window closing correctly.
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				/*
				 * Instead of directly closing the window,
				 * we're going to change the JOptionPane's
				 * value property.
				 */
				optionPane.setValue(new Integer(
						JOptionPane.CLOSED_OPTION));
			}
		});

		//Ensure the text field always gets the first focus.
		addComponentListener(new ComponentAdapter() {
			public void componentShown(ComponentEvent ce) {
				textField.requestFocusInWindow();
			}
		});

		//Register an event handler that puts the text into the option pane.
		textField.addActionListener(this);

		//Register an event handler that reacts to option pane state changes.
		optionPane.addPropertyChangeListener(this);
		pack();
	}

	/** This method handles events for the text field. */
	public void actionPerformed(ActionEvent e) {
		optionPane.setValue(btnString1);
	}

	/** This method reacts to state changes in the option pane. */
	public void propertyChange(PropertyChangeEvent e) {
		String prop = e.getPropertyName();

		if (isVisible()
				&& (e.getSource() == optionPane)
				&& (JOptionPane.VALUE_PROPERTY.equals(prop) ||
						JOptionPane.INPUT_VALUE_PROPERTY.equals(prop))) {
			Object value = optionPane.getValue();

			if (value == JOptionPane.UNINITIALIZED_VALUE) {
				//ignore reset
				return;
			}

			//Reset the JOptionPane's value.
			//If you don't do this, then if the user
			//presses the same button next time, no
			//property change event will be fired.
			optionPane.setValue(
					JOptionPane.UNINITIALIZED_VALUE);

			if (btnString1.equals(value)) {
				typedText = textField.getText();
				//TODO check here if enetered text is legit
				
				//hiding will add the tags
				clearAndHide();
			} else {
				typedText = null;
				clearAndHide();
			}
		}
	}

	/** This method clears the dialog and hides it. */
	public void clearAndHide() {
		textField.setText(null);
		setVisible(false);
	}
}
