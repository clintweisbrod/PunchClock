package punchclock;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2003
 * Company:      Weisbrod Software Engineering
 * @author Clint Weisbrod
 * @version 1.0
 */

public class AboutDlg extends JDialog {
	JPanel panel1 = new JPanel();
	GridBagLayout gridBagLayout1 = new GridBagLayout();
	JLabel jLabel1 = new JLabel();
	JLabel jLabel2 = new JLabel();
	JLabel jLabel3 = new JLabel();
	JButton okButton = new JButton();

	public AboutDlg(Frame frame, String title, boolean modal)
	{
		super(frame, title, modal);

		try {
			jbInit();
			pack();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}

		//Center dialog over parent frame
		Point parentLocation = frame.getLocation();
		Dimension parentSize = frame.getSize();
		Dimension dlgSize = getSize();
		int x = parentLocation.x + (parentSize.width - dlgSize.width) / 2;
		int y = parentLocation.y + (parentSize.height - dlgSize.height) / 2;;
		setLocation(x, y);
	}

	public AboutDlg() {
		this(null, "", false);
	}

	void jbInit() throws Exception
	{
		panel1.setLayout(gridBagLayout1);
		jLabel1.setFont(new java.awt.Font("Dialog", 1, 16));
		jLabel1.setText("Contractor\'s Punch Clock");
		jLabel2.setText("Developed by Clint Weisbrod, 2003");
		jLabel3.setText("Weisbrod Software Engineering");
		okButton.setText("OK");
		okButton.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent e) {
				okButton_actionPerformed(e);
			}
		});

		getContentPane().add(panel1);
		panel1.add(jLabel1,     new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 10, 0, 10), 0, 0));
		panel1.add(jLabel2,    new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 10, 0, 10), 0, 0));
		panel1.add(okButton,      new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 0, 10, 0), 0, 0));
		panel1.add(jLabel3,   new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 10, 0, 10), 0, 0));
	}

	public void okButton_actionPerformed(ActionEvent e) {
		hide();
	}
}