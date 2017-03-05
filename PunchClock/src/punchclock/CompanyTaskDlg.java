package punchclock;

import javax.swing.JPanel;
import java.awt.Frame;
import java.awt.BorderLayout;
import javax.swing.JDialog;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import java.awt.GridBagConstraints;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.SpinnerNumberModel;

import punchclock.db.DBMethods;
import punchclock.db.DataSourceProxy;

import mseries.Calendar.MDateChanger;
import mseries.Calendar.MDefaultPullDownConstraints;
import mseries.ui.MChangeEvent;
import mseries.ui.MChangeListener;
import mseries.ui.MDateEntryField;
import mseries.ui.MSimpleDateFormat;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Calendar;

public class CompanyTaskDlg extends JDialog
{
	private static final long serialVersionUID = 1L;
	
	private final static String SQL_SELECT_COMPANY = "SELECT * FROM companies WHERE name = ''{0}''";
	
	private final static String SQL_SELECT_TASK = "SELECT * FROM tasks WHERE companyID = {0} AND name = ''{1}''";
	
	private final static String SQL_SELECT_PROVINCES = "SELECT abbr FROM provinces";
	
	private final static String SQL_SELECT_STATES = "SELECT abbr FROM states";
	
	private final static String SQL_UPDATE_COMPANY = "UPDATE companies SET name = ''{0}'', address = ''{1}'', " +
	"city = ''{2}'', state = ''{3}'', zip = ''{4}'', billingContact = ''{5}'', billPeriod = ''{6}'', " +
	"biWeeklyDate = {7}, hoursPerWeek = {8,number,0}, timesheetPath = ''{9}'', invoicePath = ''{10}'', " +
	"defaultHourlyRate = {11,number,0}, defaultApplyHST = {12,number,0}, decimalPrecision = {13,number,0} " +
	"WHERE id = {14,number,0}";
	
	private final static String SQL_INSERT_TASK = "INSERT INTO tasks ( companyID, name, rate, applyHST) VALUES " +
	"({0,number,0}, ''{1}'', {2,number,0}, {3,number,0})";
	
	private final static String SQL_UPDATE_TASK = "UPDATE tasks SET code = {0}, rate = {1,number,0}, applyHST = {2,number,0} " +
	"WHERE companyID = {3,number,0} AND name = ''{4}''";
	
	private int currentCompanyID = 0;
	private AppFrame owner = null;
	private JPanel jContentPane = null;
	private JLabel jLabel1 = null;
	private JComboBox jCompanyCmb = null;
	private JLabel jLabel2 = null;
	private JTextField jAddressText = null;
	private JLabel jLabel3 = null;
	private JTextField jCityText = null;
	private JLabel jLabel4 = null;
	private JTextField jContactText = null;
	private JLabel jLabel5 = null;
	private JTextField jHoursPerWeek = null;
	private JLabel jLabel6 = null;
	private JTextField jTimesheetPath = null;
	private JLabel jLabel7 = null;
	private JTextField jInvoicePath = null;
	private JLabel jLabel8 = null;
	private JComboBox jTasksCmb = null;
	private JLabel jLabel9 = null;
	private JTextField jRateText = null;
	private JButton jTimesheetButton = null;
	private JButton jInvoiceButton = null;
	private JLabel jLabel10 = null;
	private JComboBox jProvStateCmb = null;
	private JLabel jLabel11 = null;
	private JTextField jPostalZip = null;
	private JLabel jLabel12 = null;
	private JLabel jLabel13 = null;
	private JComboBox jBillingPeriodCmb = null;
	private MDateEntryField jBiWeeklyDate = null;
	private JLabel jLabel14 = null;
	private JSpinner jDecimalPrecision = null;
	private JLabel jLabel15 = null;
	private JTextField jCodeText = null;
	private JLabel jLabel16 = null;
	private JTextField jDefaultHourlyRateText = null;
	private JLabel jLabel17 = null;
	private JCheckBox jDefaultApplyHSTCheckBox = null;
	
	private class CompanyInfoFocusLostListener extends FocusAdapter
	{
		public void focusLost(FocusEvent e)	{
			updateCompanyChanges();
		}
	}
	private CompanyInfoFocusLostListener companyInfoFocusLostListener = new CompanyInfoFocusLostListener();
	
	private class TaskInfoFocusLostListener extends FocusAdapter
	{
		public void focusLost(FocusEvent e)	{
			updateTaskChanges();
		}
	}
	private TaskInfoFocusLostListener taskInfoFocusLostListener = new TaskInfoFocusLostListener();

	private JLabel jLabelGST = null;

	private JCheckBox jApplyHSTCheckBox = null;
	

	/**
	 * @param owner
	 */
	public CompanyTaskDlg(AppFrame inOwner) {
		super(inOwner);
		this.owner = inOwner;
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setSize(510, 450);
		this.setResizable(false);
		this.setTitle("Companies & Tasks");
		this.setModal(true);
		this.setContentPane(getJContentPane());
		
		DBMethods.populateCompanies(AppFrame.ds, jCompanyCmb);
		
		// initialize company and task to values in main window
		jCompanyCmb.setSelectedIndex(owner.getCurrentCompanyIndex());
		jTasksCmb.setSelectedIndex(owner.getCurrentTaskIndex());
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null)
		{
			GridBagConstraints gridBagConstraints17 = new GridBagConstraints();
			gridBagConstraints17.gridx = 1;
			gridBagConstraints17.anchor = GridBagConstraints.WEST;
			gridBagConstraints17.insets = new Insets(0, 0, 0, 0);
			gridBagConstraints17.gridy = 17;
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.gridx = 1;
			gridBagConstraints.anchor = GridBagConstraints.WEST;
			gridBagConstraints.insets = new Insets(5, 5, 0, 0);
			gridBagConstraints.gridy = 16;
			jLabelGST = new JLabel();
			jLabelGST.setText("HST");
			jLabel17 = new JLabel();
			jLabel17.setText("Default HST");
			GridBagConstraints gridBagConstraints27 = new GridBagConstraints();
			gridBagConstraints27.anchor = GridBagConstraints.WEST;
			gridBagConstraints27.insets = new Insets(5, 5, 0, 5);
			gridBagConstraints27.gridx = 3;
			gridBagConstraints27.gridy = 8;
			GridBagConstraints gridBagConstraints28 = new GridBagConstraints();
			gridBagConstraints28.anchor = GridBagConstraints.WEST;
			gridBagConstraints28.insets = new Insets(5, 0, 0, 5);
			gridBagConstraints28.gridx = 3;
			gridBagConstraints28.gridy = 9;
			GridBagConstraints gridBagConstraints26 = new GridBagConstraints();
			gridBagConstraints26.fill = GridBagConstraints.NONE;
			gridBagConstraints26.gridy = 9;
			gridBagConstraints26.weightx = 0.5D;
			gridBagConstraints26.anchor = GridBagConstraints.WEST;
			gridBagConstraints26.insets = new Insets(5, 5, 0, 0);
			gridBagConstraints26.gridx = 2;
			jLabel16 = new JLabel();
			jLabel16.setText("Default Rate ($ / hr)");
			GridBagConstraints gridBagConstraints25 = new GridBagConstraints();
			gridBagConstraints25.fill = GridBagConstraints.NONE;
			gridBagConstraints25.gridy = 8;
			gridBagConstraints25.weightx = 0.5D;
			gridBagConstraints25.anchor = GridBagConstraints.WEST;
			gridBagConstraints25.insets = new Insets(5, 5, 0, 0);
			gridBagConstraints25.gridx = 2;
			GridBagConstraints gridBagConstraints23 = new GridBagConstraints();
			gridBagConstraints23.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints23.gridy = 17;
			gridBagConstraints23.weightx = 0.0D;
			gridBagConstraints23.anchor = GridBagConstraints.WEST;
			gridBagConstraints23.insets = new Insets(0, 5, 0, 0);
			gridBagConstraints23.gridx = 2;
			GridBagConstraints gridBagConstraints22 = new GridBagConstraints();
			gridBagConstraints22.gridx = 2;
			gridBagConstraints22.anchor = GridBagConstraints.WEST;
			gridBagConstraints22.insets = new Insets(5, 5, 0, 0);
			gridBagConstraints22.gridy = 16;
			jLabel15 = new JLabel();
			jLabel15.setText("Code");
			GridBagConstraints gridBagConstraints21 = new GridBagConstraints();
			gridBagConstraints21.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints21.gridy = 9;
			gridBagConstraints21.weightx = 0.0D;
			gridBagConstraints21.anchor = GridBagConstraints.WEST;
			gridBagConstraints21.insets = new Insets(0, 5, 0, 0);
			gridBagConstraints21.gridx = 1;
			GridBagConstraints gridBagConstraints20 = new GridBagConstraints();
			gridBagConstraints20.gridx = 1;
			gridBagConstraints20.anchor = GridBagConstraints.WEST;
			gridBagConstraints20.insets = new Insets(5, 5, 0, 0);
			gridBagConstraints20.gridy = 8;
			jLabel14 = new JLabel();
			jLabel14.setText("Decimal Precision");
			GridBagConstraints gridBagConstraints19 = new GridBagConstraints();
			gridBagConstraints19.fill = GridBagConstraints.NONE;
			gridBagConstraints19.gridy = 7;
			gridBagConstraints19.weightx = 0.5D;
			gridBagConstraints19.anchor = GridBagConstraints.WEST;
			gridBagConstraints19.insets = new Insets(0, 5, 0, 0);
			gridBagConstraints19.gridx = 2;
			GridBagConstraints gridBagConstraints18 = new GridBagConstraints();
			gridBagConstraints18.fill = GridBagConstraints.NONE;
			gridBagConstraints18.gridy = 7;
			gridBagConstraints18.weightx = 0.0D;
			gridBagConstraints18.anchor = GridBagConstraints.WEST;
			gridBagConstraints18.insets = new Insets(0, 5, 0, 0);
			gridBagConstraints18.gridx = 1;
			GridBagConstraints gridBagConstraints16 = new GridBagConstraints();
			gridBagConstraints16.gridx = 2;
			gridBagConstraints16.anchor = GridBagConstraints.WEST;
			gridBagConstraints16.insets = new Insets(5, 5, 0, 0);
			gridBagConstraints16.weightx = 0.5D;
			gridBagConstraints16.gridy = 6;
			jLabel13 = new JLabel();
			jLabel13.setText("Bi-weekly Date");
			GridBagConstraints gridBagConstraints15 = new GridBagConstraints();
			gridBagConstraints15.gridx = 1;
			gridBagConstraints15.anchor = GridBagConstraints.WEST;
			gridBagConstraints15.insets = new Insets(5, 5, 0, 0);
			gridBagConstraints15.gridy = 6;
			jLabel12 = new JLabel();
			jLabel12.setText("Billing Period");
			GridBagConstraints gridBagConstraints14 = new GridBagConstraints();
			gridBagConstraints14.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints14.gridy = 5;
			gridBagConstraints14.weightx = 0.5D;
			gridBagConstraints14.anchor = GridBagConstraints.WEST;
			gridBagConstraints14.insets = new Insets(0, 5, 0, 0);
			gridBagConstraints14.gridx = 2;
			GridBagConstraints gridBagConstraints13 = new GridBagConstraints();
			gridBagConstraints13.gridx = 2;
			gridBagConstraints13.anchor = GridBagConstraints.WEST;
			gridBagConstraints13.insets = new Insets(5, 5, 0, 0);
			gridBagConstraints13.weightx = 0.5D;
			gridBagConstraints13.gridy = 4;
			jLabel11 = new JLabel();
			jLabel11.setText("Postal / Zip");
			GridBagConstraints gridBagConstraints12 = new GridBagConstraints();
			gridBagConstraints12.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints12.gridy = 5;
			gridBagConstraints12.weightx = 0.0D;
			gridBagConstraints12.anchor = GridBagConstraints.WEST;
			gridBagConstraints12.insets = new Insets(0, 5, 0, 0);
			gridBagConstraints12.gridx = 1;
			GridBagConstraints gridBagConstraints111 = new GridBagConstraints();
			gridBagConstraints111.gridx = 1;
			gridBagConstraints111.anchor = GridBagConstraints.WEST;
			gridBagConstraints111.insets = new Insets(5, 5, 0, 0);
			gridBagConstraints111.gridy = 4;
			jLabel10 = new JLabel();
			jLabel10.setText("Prov/State");
			GridBagConstraints gridBagConstraints101 = new GridBagConstraints();
			gridBagConstraints101.gridx = 3;
			gridBagConstraints101.anchor = GridBagConstraints.WEST;
			gridBagConstraints101.insets = new Insets(0, 5, 0, 5);
			gridBagConstraints101.gridy = 13;
			GridBagConstraints gridBagConstraints91 = new GridBagConstraints();
			gridBagConstraints91.gridx = 3;
			gridBagConstraints91.anchor = GridBagConstraints.WEST;
			gridBagConstraints91.insets = new Insets(0, 5, 0, 5);
			gridBagConstraints91.gridy = 11;
			GridBagConstraints gridBagConstraints81 = new GridBagConstraints();
			gridBagConstraints81.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints81.gridy = 17;
			gridBagConstraints81.weightx = 1.0;
			gridBagConstraints81.anchor = GridBagConstraints.WEST;
			gridBagConstraints81.insets = new Insets(0, 5, 0, 0);
			gridBagConstraints81.gridx = 0;
			GridBagConstraints gridBagConstraints61 = new GridBagConstraints();
			gridBagConstraints61.gridx = 0;
			gridBagConstraints61.anchor = GridBagConstraints.WEST;
			gridBagConstraints61.insets = new Insets(5, 5, 0, 0);
			gridBagConstraints61.gridy = 16;
			jLabel9 = new JLabel();
			jLabel9.setText("Rate ($ / hr)");
			GridBagConstraints gridBagConstraints51 = new GridBagConstraints();
			gridBagConstraints51.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints51.gridy = 15;
			gridBagConstraints51.weightx = 1.0;
			gridBagConstraints51.gridwidth = 2;
			gridBagConstraints51.insets = new Insets(0, 5, 0, 0);
			gridBagConstraints51.anchor = GridBagConstraints.WEST;
			gridBagConstraints51.gridx = 0;
			GridBagConstraints gridBagConstraints41 = new GridBagConstraints();
			gridBagConstraints41.gridx = 0;
			gridBagConstraints41.gridwidth = 2;
			gridBagConstraints41.anchor = GridBagConstraints.WEST;
			gridBagConstraints41.insets = new Insets(15, 5, 0, 0);
			gridBagConstraints41.gridy = 14;
			jLabel8 = new JLabel();
			jLabel8.setText("Tasks");
			GridBagConstraints gridBagConstraints31 = new GridBagConstraints();
			gridBagConstraints31.fill = GridBagConstraints.BOTH;
			gridBagConstraints31.gridy = 13;
			gridBagConstraints31.weightx = 1.0;
			gridBagConstraints31.gridwidth = 3;
			gridBagConstraints31.insets = new Insets(0, 5, 0, 0);
			gridBagConstraints31.gridx = 0;
			GridBagConstraints gridBagConstraints24 = new GridBagConstraints();
			gridBagConstraints24.gridx = 0;
			gridBagConstraints24.anchor = GridBagConstraints.WEST;
			gridBagConstraints24.insets = new Insets(5, 5, 0, 0);
			gridBagConstraints24.gridy = 12;
			jLabel7 = new JLabel();
			jLabel7.setText("Invoice Path");
			GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
			gridBagConstraints11.fill = GridBagConstraints.BOTH;
			gridBagConstraints11.gridy = 11;
			gridBagConstraints11.weightx = 1.0;
			gridBagConstraints11.gridwidth = 3;
			gridBagConstraints11.insets = new Insets(0, 5, 0, 0);
			gridBagConstraints11.gridx = 0;
			GridBagConstraints gridBagConstraints10 = new GridBagConstraints();
			gridBagConstraints10.gridx = 0;
			gridBagConstraints10.anchor = GridBagConstraints.WEST;
			gridBagConstraints10.insets = new Insets(5, 5, 0, 0);
			gridBagConstraints10.gridy = 10;
			jLabel6 = new JLabel();
			jLabel6.setText("Timesheet Path");
			GridBagConstraints gridBagConstraints9 = new GridBagConstraints();
			gridBagConstraints9.fill = GridBagConstraints.NONE;
			gridBagConstraints9.gridy = 9;
			gridBagConstraints9.weightx = 1.0;
			gridBagConstraints9.anchor = GridBagConstraints.WEST;
			gridBagConstraints9.insets = new Insets(0, 5, 0, 0);
			gridBagConstraints9.gridx = 0;
			GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
			gridBagConstraints8.gridx = 0;
			gridBagConstraints8.anchor = GridBagConstraints.WEST;
			gridBagConstraints8.insets = new Insets(5, 5, 0, 0);
			gridBagConstraints8.gridy = 8;
			jLabel5 = new JLabel();
			jLabel5.setText("Hours per Week");
			GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
			gridBagConstraints7.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints7.gridy = 7;
			gridBagConstraints7.weightx = 0.5D;
			gridBagConstraints7.gridwidth = 1;
			gridBagConstraints7.insets = new Insets(0, 5, 0, 0);
			gridBagConstraints7.gridx = 0;
			GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
			gridBagConstraints6.gridx = 0;
			gridBagConstraints6.anchor = GridBagConstraints.WEST;
			gridBagConstraints6.gridwidth = 1;
			gridBagConstraints6.insets = new Insets(5, 5, 0, 0);
			gridBagConstraints6.gridy = 6;
			jLabel4 = new JLabel();
			jLabel4.setText("Billing Contact");
			GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
			gridBagConstraints5.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints5.gridy = 5;
			gridBagConstraints5.weightx = 0.5D;
			gridBagConstraints5.insets = new Insets(0, 5, 0, 0);
			gridBagConstraints5.gridx = 0;
			GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
			gridBagConstraints4.gridx = 0;
			gridBagConstraints4.anchor = GridBagConstraints.WEST;
			gridBagConstraints4.insets = new Insets(5, 5, 0, 0);
			gridBagConstraints4.gridy = 4;
			jLabel3 = new JLabel();
			jLabel3.setText("City");
			GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
			gridBagConstraints3.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints3.gridy = 3;
			gridBagConstraints3.weightx = 1.0;
			gridBagConstraints3.gridwidth = 2;
			gridBagConstraints3.insets = new Insets(0, 5, 0, 0);
			gridBagConstraints3.anchor = GridBagConstraints.WEST;
			gridBagConstraints3.gridx = 0;
			GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
			gridBagConstraints2.gridx = 0;
			gridBagConstraints2.anchor = GridBagConstraints.WEST;
			gridBagConstraints2.gridwidth = 2;
			gridBagConstraints2.insets = new Insets(5, 5, 0, 0);
			gridBagConstraints2.gridy = 2;
			jLabel2 = new JLabel();
			jLabel2.setText("Address");
			GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints1.gridy = 1;
			gridBagConstraints1.weightx = 1.0;
			gridBagConstraints1.insets = new Insets(0, 5, 0, 0);
			gridBagConstraints1.gridx = 0;
			gridBagConstraints1.gridwidth = 2;
			GridBagConstraints gridBagConstraints0 = new GridBagConstraints();
			gridBagConstraints0.gridx = 0;
			gridBagConstraints0.anchor = GridBagConstraints.WEST;
			gridBagConstraints0.insets = new Insets(0, 5, 0, 0);
			gridBagConstraints0.gridy = 0;
			jLabel1 = new JLabel();
			jLabel1.setText("Company");
			jContentPane = new JPanel();
			jContentPane.setLayout(new GridBagLayout());
			jContentPane.add(jLabel1, gridBagConstraints0);
			jContentPane.add(getJCompanyCmb(), gridBagConstraints1);
			jContentPane.add(jLabel2, gridBagConstraints2);
			jContentPane.add(getJAddressText(), gridBagConstraints3);
			jContentPane.add(jLabel3, gridBagConstraints4);
			jContentPane.add(getJCityText(), gridBagConstraints5);
			jContentPane.add(jLabel4, gridBagConstraints6);
			jContentPane.add(getJContactText(), gridBagConstraints7);
			jContentPane.add(jLabel5, gridBagConstraints8);
			jContentPane.add(getJHoursPerWeek(), gridBagConstraints9);
			jContentPane.add(jLabel6, gridBagConstraints10);
			jContentPane.add(getJTimesheetPath(), gridBagConstraints11);
			jContentPane.add(jLabel7, gridBagConstraints24);
			jContentPane.add(getJInvoicePath(), gridBagConstraints31);
			jContentPane.add(jLabel8, gridBagConstraints41);
			jContentPane.add(getJTasksCmb(), gridBagConstraints51);
			jContentPane.add(jLabel9, gridBagConstraints61);
			jContentPane.add(getJRateText(), gridBagConstraints81);
			jContentPane.add(getJTimesheetButton(), gridBagConstraints91);
			jContentPane.add(getJInvoiceButton(), gridBagConstraints101);
			jContentPane.add(jLabel10, gridBagConstraints111);
			jContentPane.add(getJProvStateCmb(), gridBagConstraints12);
			jContentPane.add(jLabel11, gridBagConstraints13);
			jContentPane.add(getJPostalZip(), gridBagConstraints14);
			jContentPane.add(jLabel12, gridBagConstraints15);
			jContentPane.add(jLabel13, gridBagConstraints16);
			jContentPane.add(getJBillingPeriodCmb(), gridBagConstraints18);
			jContentPane.add(getJBiWeeklyDate(), gridBagConstraints19);
			jContentPane.add(jLabel14, gridBagConstraints20);
			jContentPane.add(getJDecimalPrecision(), gridBagConstraints21);
			jContentPane.add(jLabel15, gridBagConstraints22);
			jContentPane.add(getJCodeText(), gridBagConstraints23);
			jContentPane.add(jLabel16, gridBagConstraints25);
			jContentPane.add(getDefaultHourlyRateText(), gridBagConstraints26);
			jContentPane.add(jLabel17, gridBagConstraints27);
			jContentPane.add(getDefaultGSTApplicableCheckBox(), gridBagConstraints28);
			jContentPane.add(jLabelGST, gridBagConstraints);
			jContentPane.add(getJApplyHSTCheckBox(), gridBagConstraints17);
		}
		
		return jContentPane;
	}
	
	private void updateCompanyChanges()
	{
		System.out.println("Updating company data.");
		Connection conn = null;
		ResultSet rs = null;
		Statement stmt = null;
		try
		{
			String companyName = (String)jCompanyCmb.getSelectedItem();
			String address = jAddressText.getText();
			String city = jCityText.getText();
			String state = (String)jProvStateCmb.getSelectedItem();
			String zip = jPostalZip.getText();
			String contact = jContactText.getText();
			String billPeriod = (String)jBillingPeriodCmb.getSelectedItem();
			String biWeeklyDate;
			if (billPeriod.equals("BiWeekly"))
				biWeeklyDate = "'" + new String(DateTime.getDateString(jBiWeeklyDate.getValue())) + "'";
			else
				biWeeklyDate = "null";
			Double hoursPerWeek = Double.parseDouble(jHoursPerWeek.getText());
			String timesheetPath = jTimesheetPath.getText();
			String invoicePath = jInvoicePath.getText();
			Integer defaultRate = new Integer((int)(Double.parseDouble(jDefaultHourlyRateText.getText()) * 100));
			Integer defaultApplyHST = new Integer(jDefaultApplyHSTCheckBox.isSelected()?1:0);
			Integer decimalPrecision = (Integer)jDecimalPrecision.getValue();
			Integer id = new Integer(currentCompanyID);
			
			Object[] insertArgs = {companyName, address, city, state, zip, contact, billPeriod, biWeeklyDate,
								   hoursPerWeek, timesheetPath, invoicePath, defaultRate, defaultApplyHST, decimalPrecision, id};
			String sql = MessageFormat.format(SQL_UPDATE_COMPANY, insertArgs);
			
			conn = AppFrame.ds.getConnection();
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
		}
		catch (Exception e)	{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	private void updateTaskChanges()
	{
		System.out.println("Updating task data.");
		Connection conn = null;
		ResultSet rs = null;
		Statement stmt = null;
		try
		{
			String code = jCodeText.getText();
			if (code.length() == 0)
				code = "null";
			else
				code = "'" + code + "'";
			Integer rate = new Integer((int)(Double.parseDouble(jRateText.getText()) * 100));
			Integer companyID = new Integer(currentCompanyID);
			String name = (String)jTasksCmb.getSelectedItem();
			Integer applyHST = new Integer(jApplyHSTCheckBox.isSelected()? 1:0);
			
			Object[] insertArgs = {code, rate, applyHST, companyID, name};
			String sql = MessageFormat.format(SQL_UPDATE_TASK, insertArgs);
			
			conn = AppFrame.ds.getConnection();
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
		}
		catch (Exception e)	{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	private void companyChanged(ActionEvent event)
	{
		String companyName = (String)jCompanyCmb.getSelectedItem();
		
		currentCompanyID = DBMethods.getTableInt(AppFrame.ds, "companies", "id", "name='" + companyName + "'");
		
		Connection conn = null;
		ResultSet rs = null;
		Statement stmt = null;
		try
		{
			Object[] insertArgs = {companyName};
			String sql = MessageFormat.format(SQL_SELECT_COMPANY, insertArgs);

			conn = AppFrame.ds.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			if (rs.first())
			{
				jAddressText.setText(rs.getString("address"));
				jCityText.setText(rs.getString("city"));
				jPostalZip.setText(rs.getString("zip"));
				jContactText.setText(rs.getString("billingContact"));
				jHoursPerWeek.setText(IntegralCurrency.doubleToString(rs.getDouble("hoursPerWeek"), 2));
				jTimesheetPath.setText(rs.getString("timesheetPath"));
				jInvoicePath.setText(rs.getString("invoicePath"));
				jDecimalPrecision.setValue(rs.getInt("decimalPrecision"));
				
				double defaultRate = (double)rs.getDouble("defaultHourlyRate") / 100;
				jDefaultHourlyRateText.setText(IntegralCurrency.doubleToString(defaultRate, 2));
				jDefaultApplyHSTCheckBox.setSelected((rs.getInt("DefaultApplyHST")) == 1);
				
				String billPeriod = rs.getString("billPeriod");
				boolean isBiWeekly = (billPeriod.equals("BiWeekly"));
				jBiWeeklyDate.setVisible(isBiWeekly);
				jLabel13.setVisible(isBiWeekly);
				jBillingPeriodCmb.setSelectedItem(billPeriod);
				
				jProvStateCmb.setSelectedItem(rs.getString("state"));

				DBMethods.populateTasks(AppFrame.ds, jTasksCmb, companyName);
			}
		}
		catch (Exception e)	{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	private void taskChanged(ActionEvent event)
	{
		// First, add new task if editbox contains item not in list
		String taskName = (String)jTasksCmb.getSelectedItem();
		if (taskName != null)
		{
			// If the comment is not in the list, add it.
			boolean found = false;
			for (int i = 0; i < jTasksCmb.getItemCount(); i++)
			{
				String itemString = (String)jTasksCmb.getItemAt(i);
				if (taskName.equals(itemString))
				{
					found = true;
					break;
				}
			}
			if (found == false)
			{
				jTasksCmb.insertItemAt(taskName, 0);
				jTasksCmb.setSelectedIndex(0);
				
				// TODO: Add new item to tasks table?
				addNewTask();
				
				// Also, clear the code field and set defaults
				jCodeText.setText("");
				jRateText.setText(jDefaultHourlyRateText.getText());
				jApplyHSTCheckBox.setSelected(jDefaultApplyHSTCheckBox.isSelected());
			}
		}
		
		Connection conn = null;
		ResultSet rs = null;
		Statement stmt = null;
		try
		{
			Object[] insertArgs = {new Integer(currentCompanyID), taskName};
			String sql = MessageFormat.format(SQL_SELECT_TASK, insertArgs);
			
			conn = AppFrame.ds.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			if (rs.first())
			{
				double rate = (double)rs.getInt("rate") / 100;
				jRateText.setText(IntegralCurrency.doubleToString(rate, 2));
				
				jApplyHSTCheckBox.setSelected((rs.getInt("applyHST")) == 1);

				String code = rs.getString("code");
				if (code != null)
					jCodeText.setText(code);
				else
					jCodeText.setText("");
			}
		}
		catch (Exception e)	{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	private void billingPeriodChanged(ActionEvent event)
	{
		// If Bi-Weekly pay period is suggested, we show the bi-weekly date selection 
		String billingPeriod = (String)jBillingPeriodCmb.getSelectedItem();
		boolean isVisible = billingPeriod.equals("BiWeekly");
		jLabel13.setVisible(isVisible);
		jBiWeeklyDate.setVisible(isVisible);
	}
	
	private void addNewTask()
	{
		Connection conn = null;
		ResultSet rs = null;
		Statement stmt = null;
		try
		{
			Integer companyID = new Integer(currentCompanyID);
			String name = (String)jTasksCmb.getSelectedItem();
			Integer rate = new Integer((int)(Double.parseDouble(jDefaultHourlyRateText.getText()) * 100));
			Integer applyHST = new Integer(jApplyHSTCheckBox.isSelected()?1:0);
			
			Object[] insertArgs = {companyID, name, rate, applyHST};
			String sql = MessageFormat.format(SQL_INSERT_TASK, insertArgs);
			
			conn = AppFrame.ds.getConnection();
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
		}
		catch (Exception e)	{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	/**
	 * This method initializes jCompanyCmb	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox getJCompanyCmb() {
		if (jCompanyCmb == null) {
			jCompanyCmb = new JComboBox();
			jCompanyCmb.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e){
					companyChanged(e);
				}
			});
			jCompanyCmb.setPreferredSize(new Dimension(31, 21));
		}
		return jCompanyCmb;
	}
	
	

	/**
	 * This method initializes jAddressText	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJAddressText() {
		if (jAddressText == null) {
			jAddressText = new JTextField();
			jAddressText.addFocusListener(companyInfoFocusLostListener);
		}
		return jAddressText;
	}

	/**
	 * This method initializes jCityText	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJCityText() {
		if (jCityText == null) {
			jCityText = new JTextField();
			jCityText.addFocusListener(companyInfoFocusLostListener);
		}
		return jCityText;
	}

	/**
	 * This method initializes jContactText	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJContactText() {
		if (jContactText == null) {
			jContactText = new JTextField();
			jContactText.addFocusListener(companyInfoFocusLostListener);
		}
		return jContactText;
	}

	/**
	 * This method initializes jHoursPerWeek	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJHoursPerWeek() {
		if (jHoursPerWeek == null) {
			jHoursPerWeek = new JTextField();
			jHoursPerWeek.addFocusListener(companyInfoFocusLostListener);
			jHoursPerWeek.setPreferredSize(new Dimension(50, 20));
		}
		return jHoursPerWeek;
	}

	/**
	 * This method initializes jTimesheetPath	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJTimesheetPath() {
		if (jTimesheetPath == null) {
			jTimesheetPath = new JTextField();
			jTimesheetPath.addFocusListener(companyInfoFocusLostListener);
		}
		return jTimesheetPath;
	}

	/**
	 * This method initializes jInvoicePath	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJInvoicePath() {
		if (jInvoicePath == null) {
			jInvoicePath = new JTextField();
			jInvoicePath.addFocusListener(companyInfoFocusLostListener);
		}
		return jInvoicePath;
	}

	/**
	 * This method initializes jTasksCmb	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox getJTasksCmb() {
		if (jTasksCmb == null) {
			jTasksCmb = new JComboBox();
			jTasksCmb.setEditable(true);
			jTasksCmb.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e){
					taskChanged(e);
				}
			});
			jTasksCmb.setPreferredSize(new Dimension(31, 21));
		}
		return jTasksCmb;
	}

	/**
	 * This method initializes jRateText	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJRateText() {
		if (jRateText == null) {
			jRateText = new JTextField();
			jRateText.addFocusListener(taskInfoFocusLostListener);
			jRateText.setPreferredSize(new Dimension(50, 20));
		}
		return jRateText;
	}

	/**
	 * This method initializes jTimesheetButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJTimesheetButton() {
		if (jTimesheetButton == null) {
			jTimesheetButton = new JButton();
			jTimesheetButton.setPreferredSize(new Dimension(21, 21));
			jTimesheetButton.setText("...");
		}
		return jTimesheetButton;
	}

	/**
	 * This method initializes jInvoiceButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJInvoiceButton() {
		if (jInvoiceButton == null) {
			jInvoiceButton = new JButton();
			jInvoiceButton.setText("...");
			jInvoiceButton.setPreferredSize(new Dimension(21, 21));
		}
		return jInvoiceButton;
	}

	/**
	 * This method initializes jProvStateCmb	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox getJProvStateCmb() {
		if (jProvStateCmb == null) {
			jProvStateCmb = new JComboBox();
			DBMethods.populateComboBox(AppFrame.ds, jProvStateCmb, SQL_SELECT_PROVINCES, true);
			jProvStateCmb.addItem("---------");
			DBMethods.populateComboBox(AppFrame.ds, jProvStateCmb, SQL_SELECT_STATES, false);
			jProvStateCmb.addFocusListener(companyInfoFocusLostListener);
			jProvStateCmb.setPreferredSize(new Dimension(140, 21));
		}
		return jProvStateCmb;
	}

	/**
	 * This method initializes jPostalZip	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJPostalZip() {
		if (jPostalZip == null) {
			jPostalZip = new JTextField();
			jPostalZip.addFocusListener(companyInfoFocusLostListener);
			jPostalZip.setPreferredSize(new Dimension(80, 20));
		}
		return jPostalZip;
	}

	/**
	 * This method initializes jBillingPeriodCmb	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox getJBillingPeriodCmb() {
		if (jBillingPeriodCmb == null) {
			jBillingPeriodCmb = new JComboBox();
			jBillingPeriodCmb.addItem("Weekly");
			jBillingPeriodCmb.addItem("BiWeekly");
			jBillingPeriodCmb.addItem("BiMonthly");
			jBillingPeriodCmb.addItem("Monthly");
			jBillingPeriodCmb.addFocusListener(companyInfoFocusLostListener);
			jBillingPeriodCmb.setPreferredSize(new Dimension(140, 21));
			jBillingPeriodCmb.addActionListener(new ActionListener()  {
				public void actionPerformed(ActionEvent e) {
					billingPeriodChanged(e);
				}
			});
		}
		return jBillingPeriodCmb;
	}

	/**
	 * This method initializes jBiWeeklyDate	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private MDateEntryField getJBiWeeklyDate() {
		if (jBiWeeklyDate == null) {
			jBiWeeklyDate = new MDateEntryField(10);
			jBiWeeklyDate.setDateFormatter(new MSimpleDateFormat("MM/dd/yyyy"));
			jBiWeeklyDate.setPreferredSize(new Dimension(90, 21));
			jBiWeeklyDate.setMinimumSize(new Dimension(90, 21));
			MDefaultPullDownConstraints pdcBiWeeklyDate = new MDefaultPullDownConstraints();
			pdcBiWeeklyDate.firstDay = Calendar.SUNDAY;
			pdcBiWeeklyDate.changerStyle = MDateChanger.BUTTON;
			pdcBiWeeklyDate.hasShadow = true;
			jBiWeeklyDate.setConstraints(pdcBiWeeklyDate);
			jBiWeeklyDate.setEditable(false);
			jBiWeeklyDate.addFocusListener(companyInfoFocusLostListener);
		}
		return jBiWeeklyDate;
	}

	/**
	 * This method initializes jDecimalPrecision	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JSpinner getJDecimalPrecision() {
		if (jDecimalPrecision == null) {
			SpinnerNumberModel spinModel = new SpinnerNumberModel(1, 0, 5, 1);
			jDecimalPrecision = new JSpinner(spinModel);
			jDecimalPrecision.addFocusListener(companyInfoFocusLostListener);
			jDecimalPrecision.setPreferredSize(new Dimension(37, 21));
		}
		return jDecimalPrecision;
	}

	/**
	 * This method initializes jCodeText	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJCodeText() {
		if (jCodeText == null) {
			jCodeText = new JTextField();
			jCodeText.addFocusListener(taskInfoFocusLostListener);
			jCodeText.setPreferredSize(new Dimension(80, 20));
		}
		return jCodeText;
	}
	
	/**
	 * This method initializes jDefaultHourlyRateText	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getDefaultHourlyRateText() {
		if (jDefaultHourlyRateText == null) {
			jDefaultHourlyRateText = new JTextField();
			jDefaultHourlyRateText.addFocusListener(companyInfoFocusLostListener);
			jDefaultHourlyRateText.setPreferredSize(new Dimension(50, 20));
		}
		return jDefaultHourlyRateText;
	}
	
	private JCheckBox getDefaultGSTApplicableCheckBox() {
		if (jDefaultApplyHSTCheckBox == null) {
			jDefaultApplyHSTCheckBox = new JCheckBox();
		}
		return jDefaultApplyHSTCheckBox;
	}

	/**
	 * This method initializes jApplyHSTCheckBox	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getJApplyHSTCheckBox() {
		if (jApplyHSTCheckBox == null) {
			jApplyHSTCheckBox = new JCheckBox();
		}
		return jApplyHSTCheckBox;
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"
