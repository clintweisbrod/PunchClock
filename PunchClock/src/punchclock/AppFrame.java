package punchclock;

import punchclock.db.*;

import mseries.ui.*;
import mseries.Calendar.*;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.io.*;
import java.util.*;
import java.util.Date;
import java.text.*;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.*;
import org.apache.log4j.*;
import com.borland.jbcl.layout.*;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Dimension;
import java.awt.Toolkit;

public class AppFrame extends JFrame
{
	// DataSource for connection pooling
	public static DataSourceProxy ds;

	// logging category
	private static Category cat = Category.getInstance(AppFrame.class.getName());

	SafeProperties props = new SafeProperties();

	private java.util.Timer captionUpdater = null;
	private java.util.Timer offsetUpdater = null;
	private SimpleDateFormat captionDateFormatter = null;
	private java.util.Date punchInTime = null;
	private int punchInRate;
	private float hoursPerWeek;
	private long hst;
	private long income;
	private int incomeTaxRate;
	private long secondsToday;	//Number of seconds logged for current day
	private long secondsPeriod;	//Number of seconds logged for current period
	private long penniesToday;
	private long penniesPeriod;

	private final static int MAX_COMMENT_CHARS = 64;

	private final static String CLICK_PUNCH_OUT = "Click \"Punch Out\" when done working on this task.";

	private final static String ENTER_COMMENT = "Please enter a comment for this session.";

	private final static String SQL_SELECT_BILL_RECS = "SELECT start, end FROM timelog WHERE " +
		"companyID = {0,number,0} AND start >= ''{1}'' AND end < ''{2}'' ORDER BY start";

	private final static String SQL_SELECT_BILL_RECS_PREMIDNIGHT = "SELECT end FROM timelog WHERE " +
		"companyID = {0,number,0} AND start < ''{1}'' AND end > ''{1}''";

	private final static String SQL_SELECT_BILL_RECS_POSTMIDNIGHT = "SELECT start FROM timelog WHERE " +
		"companyID = {0,number,0} AND start < ''{1}'' AND end > ''{1}''";

	private final static String SQL_SELECT_EXPENSES = "SELECT * FROM expenses WHERE " +
		"companyID = {0,number,0} AND date >= ''{1}'' AND date < ''{2}'' ORDER BY date";
	
	private final static String SQL_SELECT_TAX_YEARS = "SELECT DISTINCT(YEAR(end)) FROM timelog";
	
	private final static String SQL_SELECT_TASKIDS = "SELECT DISTINCT taskID FROM timelog WHERE start >= ''{0}'' AND end < ''{1}''";
	
	private final static String SQL_SELECT_TIMELOGS_BY_TASKID = "SELECT start, end FROM timelog WHERE taskID = {0,number,0} AND start >= ''{1}'' AND end < ''{2}''";
	
	private boolean ignoreEvents = false;

	DateRange dtRange = null;
	JMenuBar jMenuBar1 = new JMenuBar();
	JMenu jMenuFile = new JMenu();
	JMenuItem jMenuFileExit = new JMenuItem();
	JMenu jMenuHelp = new JMenu();
	JMenuItem jMenuHelpAbout = new JMenuItem();
	CompanyTaskDlg companyTaskDialog = null;

	JEditorPane jEditPane = new JEditorPane();
	JScrollPane jScrollPane = new JScrollPane(jEditPane);

	BorderLayout borderLayout2 = new BorderLayout();
	VerticalFlowLayout verticalFlowLayout1 = new VerticalFlowLayout();  //  @jve:decl-index=0:
	FlowLayout flowLayout1 = new FlowLayout();

	JTabbedPane jTabbedPane1 = new JTabbedPane();

	JPanel contentPane;
	JPanel jPanel1 = new JPanel();
	JPanel jPanel2 = new JPanel();
	JPanel jPanel3 = new JPanel();
	JPanel jPanel4 = new JPanel();
	JPanel jPanel5 = new JPanel();
	JPanel jPanel6 = new JPanel();
	JPanel jPanel7 = new JPanel();

	GridBagLayout gridBagLayout1 = new GridBagLayout();
	GridBagLayout gridBagLayout2 = new GridBagLayout();
	GridBagLayout gridBagLayout3 = new GridBagLayout();
	GridBagLayout gridBagLayout4 = new GridBagLayout();
	GridBagLayout gridBagLayout5 = new GridBagLayout();
	GridBagLayout gridBagLayout6 = new GridBagLayout();
	GridBagLayout gridBagLayout7 = new GridBagLayout();

	JComboBox cmbCompany = new JComboBox();
	JComboBox cmbTask = new JComboBox();
	JComboBox cmbComment = new JComboBox();
	JComboBox cmbDocType = new JComboBox();
	JComboBox cmbExpenseDesc = new JComboBox();
	JComboBox cmbTaxYear = new JComboBox();

	JButton btnPunchIn = new JButton();
	JButton btnPunchOut = new JButton();
	JButton btnChoosePath = new JButton();
	JButton btnGenDoc = new JButton();
	JButton btnAddExpense = new JButton();
	JButton btnEditExpense = new JButton();
	JButton btnDeleteExpense = new JButton();
	JButton btnAddWorkDayAmount = new JButton();

	JLabel elapsedTime = new JLabel();
	JLabel lastWorkTime = new JLabel();
	JLabel periodRatio = new JLabel();
	JLabel accruedEarnings = new JLabel();
	JLabel offsetTime = new JLabel();
	JLabel jLabel1 = new JLabel();
	JLabel jLabel2 = new JLabel();
	JLabel jLabel3 = new JLabel();
	JLabel jLabel4 = new JLabel();
	JLabel jLabel5 = new JLabel();
	JLabel jLabel6 = new JLabel();
	JLabel jLabel7 = new JLabel();
	JLabel jLabel8 = new JLabel();
	JLabel jLabel9 = new JLabel();
	JLabel jLabel10 = new JLabel();
	JLabel jLabel12 = new JLabel();
	JLabel jLabel13 = new JLabel();
	JLabel jLabel14 = new JLabel();
	JLabel jLabel15 = new JLabel();
	JLabel jLabel16 = new JLabel();
	JLabel jLabel17 = new JLabel();
	JLabel jLabel18 = new JLabel();
	JLabel jLabel19 = new JLabel();
	JLabel jLabel20 = new JLabel();
	JLabel jLabel21 = new JLabel();

	MDateEntryField cmbStartDate = new MDateEntryField(10);
	MDateEntryField cmbEndDate = new MDateEntryField(10);
	MDateEntryField cmbExpenseDate = new MDateEntryField(10);
	MDateEntryField cmbWorkDayDate = new MDateEntryField(10);
	MDefaultPullDownConstraints pdcStartDate = new MDefaultPullDownConstraints();
	MDefaultPullDownConstraints pdcEndDate = new MDefaultPullDownConstraints();
	MDefaultPullDownConstraints pdcExpenseDate = new MDefaultPullDownConstraints();
	MDefaultPullDownConstraints pdcWorkDayDate = new MDefaultPullDownConstraints();
	MSpinner spnRange = new MSpinner(0);

//	LimitedStyledDocument lsd = new LimitedStyledDocument(MAX_COMMENT_CHARS);
//	JTextField txtComment = new JTextField(lsd, null, 0);
	JTextField txtDocPath = new JTextField();
	JTextField txtExpenseAmount = new JTextField();
	JTextField txtGST = new JTextField();
	JTextField txtIncome = new JTextField();
	JTextField txtTaxTotal = new JTextField();
	JCheckBox chkHST = new JCheckBox();

	SpinnerNumberModel model1 = new SpinnerNumberModel(0, 0, 600, 1);
	JSpinner spnPunchInOffset = new JSpinner(model1);
	SpinnerNumberModel model2 = new SpinnerNumberModel(0, 0, 600, 1);
	JSpinner spnPunchOutOffset = new JSpinner(model2);

	JLabel jLabelPunchInOffset = new JLabel();
	JLabel jLabelPunchOutOffset = new JLabel();
	JTextField txtPercent = new JTextField();
	SpinnerNumberModel model3 = new SpinnerNumberModel(0, 0, 100, 1);
	JSpinner spnIncomeTaxPercent = new JSpinner(model3);
	JProgressBar periodProgress = new JProgressBar(0, 100);
	JTextField txtWorkDayHours = new JTextField();

	private JButton jCompanyTaskButton = null;
	private JTextField txtCPPContribution = null;
	private JLabel jLabelCPP = null;

	/**Construct the frame*/
	public AppFrame()
	{
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);

		//Configure logging using the properties file
		String currentDir = System.getProperty("user.dir");
//		String currentDir = System.getProperty("BASEDIR");
		currentDir = currentDir.replace('\\', '/');
		currentDir = currentDir.substring(currentDir.indexOf('/'));	// Make sure path begins with '/'
		if(currentDir.charAt(currentDir.length()-1) == '/')
			currentDir = currentDir.substring(0, currentDir.length()-1);

		Properties logProps = new Properties();
		try
		{
			logProps.load(new FileInputStream(currentDir + "/properties/logging.properties"));
			PropertyConfigurator.configure(logProps);
		}
		catch (Exception e)
		{
			BasicConfigurator.configure();
		}

		//Initialize props
		try
		{
			props.load(currentDir + "/properties/punchclock.properties");

			incomeTaxRate = Integer.valueOf(props.getSafeProperty("incomeTaxRate", "35")).intValue();
		}
		catch (Exception e)
		{
			cat.error(e.toString());
		}

		try
		{
			jbInit();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			cat.error(e.toString());
		}

		dbInit();

		ignoreEvents = true;
		DBMethods.populateCompanies(ds, cmbCompany);
		ignoreEvents = false;

		initControls();

		//Add action listeners
		cmbCompany.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent e) {
				cmbCompany_actionPerformed(e);
			}
		});
		cmbTask.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent e) {
				cmbTask_actionPerformed(e);
			}
		});
		cmbComment.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent e) {
				cmbComment_actionPerformed(e);
			}
		});
		cmbDocType.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent e) {
				cmbDocType_actionPerformed(e);
			}
		});
		cmbExpenseDesc.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent e) {
				cmbExpenseDesc_actionPerformed(e);
			}
		});
		cmbTaxYear.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent e) {
				cmbTaxYear_actionPerformed(e);
			}
		});
		spnPunchInOffset.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				spnPunchInOffset_actionPerformed(e);
			}
		});
		spnPunchOutOffset.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				spnPunchOutOffset_actionPerformed(e);
			}
		});

		double captionUpdateIntervalSeconds = 5;
		try {
			captionUpdateIntervalSeconds = Double.parseDouble(props.getSafeProperty("captionUpdateIntervalSeconds", "5"));
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		captionUpdater = new java.util.Timer();
		captionUpdater.schedule(new TimerTask() {
			public void run() {
				captionUpdate();
			}
		}, new java.util.Date(), (long)(captionUpdateIntervalSeconds * 1000));
		offsetUpdater = new java.util.Timer();
		offsetUpdater.schedule(new TimerTask() {
			public void run() {
				offsetUpdate();
			}
		}, new java.util.Date(), 60000);

		// Select last year in tax year combo box
		if(cmbTaxYear.getItemCount() > 0)
			cmbTaxYear.setSelectedIndex(cmbTaxYear.getItemCount() - 1);
	}
	
	int getCurrentCompanyIndex() {
		return cmbCompany.getSelectedIndex();
	}
	
	int getCurrentTaskIndex() {
		return cmbTask.getSelectedIndex();
	}

	/**Component initialization*/
	private void jbInit() throws Exception
	{
		GridBagConstraints gridBagConstraints112 = new GridBagConstraints();
		gridBagConstraints112.gridx = 0;
		gridBagConstraints112.gridy = 6;
		gridBagConstraints112.gridwidth = 5;
		gridBagConstraints112.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints112.insets = new Insets(10, 5, 0, 5);
		
		GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
		gridBagConstraints4.gridx = 0;
		gridBagConstraints4.gridy = 7;
		gridBagConstraints4.gridwidth = 5;
		gridBagConstraints4.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints4.insets = new Insets(0, 5, 0, 5);
		
		GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
		gridBagConstraints3.gridx = 0;
		gridBagConstraints3.gridy = 8;
		gridBagConstraints3.gridwidth = 5;
		gridBagConstraints3.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints3.insets = new Insets(0, 5, 0, 5);
		
		GridBagConstraints gridBagConstraints110 = new GridBagConstraints();
		gridBagConstraints110.gridx = 0;
		gridBagConstraints110.gridy = 9;
		gridBagConstraints110.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints110.gridwidth = 5;
		gridBagConstraints110.insets = new Insets(10, 5, 0, 5);
		
		GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
		gridBagConstraints5.gridx = 0;
		gridBagConstraints5.gridy = 10;
		gridBagConstraints5.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints5.gridwidth = 5;
		
		accruedEarnings.setHorizontalAlignment(SwingConstants.CENTER);
		GridBagConstraints gridBagConstraints23 = new GridBagConstraints();
		gridBagConstraints23.gridx = 0;
		gridBagConstraints23.gridy = 11;
		gridBagConstraints23.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints23.gridwidth = 5;
		gridBagConstraints23.anchor = GridBagConstraints.NORTH;
		gridBagConstraints23.weighty = 1.0;
		gridBagConstraints23.insets = new Insets(10, 5, 0, 5);

		GridBagConstraints gridBagConstraints22 = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0);
		gridBagConstraints22.anchor = GridBagConstraints.EAST;
		GridBagConstraints gridBagConstraints21 = new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
		gridBagConstraints21.insets = new Insets(0, 20, 0, 0);
		GridBagConstraints gridBagConstraints20 = new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 10, 0), 0, 0);
		gridBagConstraints20.anchor = GridBagConstraints.WEST;
		gridBagConstraints20.insets = new Insets(0, 0, 0, 0);
		GridBagConstraints gridBagConstraints19 = new GridBagConstraints();
		gridBagConstraints19.gridx = 1;
		gridBagConstraints19.gridwidth = 2;
		gridBagConstraints19.insets = new Insets(0, 0, 0, 5);
		gridBagConstraints19.anchor = GridBagConstraints.EAST;
		gridBagConstraints19.gridy = 2;
		jLabelCPP = new JLabel();
		jLabelCPP.setText("CPP Contributions");
		GridBagConstraints gridBagConstraints18 = new GridBagConstraints(3, 4, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
		gridBagConstraints18.gridy = 5;
		gridBagConstraints18.anchor = GridBagConstraints.WEST;
		gridBagConstraints18.insets = new Insets(2, 0, 2, 0);
		GridBagConstraints gridBagConstraints17 = new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
		gridBagConstraints17.gridy = 4;
		gridBagConstraints17.insets = new Insets(2, 0, 2, 0);
		gridBagConstraints17.anchor = GridBagConstraints.WEST;
		GridBagConstraints gridBagConstraints16 = new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
		gridBagConstraints16.gridy = 3;
		gridBagConstraints16.insets = new Insets(2, 0, 2, 0);
		gridBagConstraints16.anchor = GridBagConstraints.WEST;
		GridBagConstraints gridBagConstraints15 = new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0);
		gridBagConstraints15.gridy = 3;
		GridBagConstraints gridBagConstraints14 = new GridBagConstraints(1, 3, 2, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0);
		gridBagConstraints14.gridy = 4;
		GridBagConstraints gridBagConstraints13 = new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
		gridBagConstraints13.gridy = 3;
		GridBagConstraints gridBagConstraints12 = new GridBagConstraints(0, 4, 3, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0);
		gridBagConstraints12.gridy = 5;
		GridBagConstraints gridBagConstraints111 = new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0);
		gridBagConstraints111.gridy = 3;
		GridBagConstraints gridBagConstraints10 = new GridBagConstraints();
		gridBagConstraints10.fill = GridBagConstraints.VERTICAL;
		gridBagConstraints10.gridy = 2;
		gridBagConstraints10.weightx = 1.0;
		gridBagConstraints10.insets = new Insets(2, 0, 2, 0);
		gridBagConstraints10.anchor = GridBagConstraints.WEST;
		gridBagConstraints10.gridx = 3;
		GridBagConstraints gridBagConstraints11 = new GridBagConstraints(0, 1, 1, 1, 0.5, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 0), 0, 0);
		gridBagConstraints11.insets = new Insets(0, 0, 5, 5);
		GridBagConstraints gridBagConstraints2 = new GridBagConstraints(1, 1, 1, 1, 0.5, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 5, 0), 0, 0);
		gridBagConstraints2.gridx = 2;
		gridBagConstraints2.anchor = GridBagConstraints.EAST;
		GridBagConstraints gridBagConstraints1 = new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 5, 0, 0), 0, 0);
		gridBagConstraints1.gridx = 2;
		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.insets = new Insets(0, 0, 5, 0);
		gridBagConstraints.gridy = 1;
		setIconImage(Toolkit.getDefaultToolkit().getImage("resources/punchclock.gif"));

		this.setTitle("Untitled");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setSize(new Dimension(510, 450));

		contentPane = (JPanel) this.getContentPane();
		jLabel7.setText("Comment");
		jLabel8.setText("Document Type");
		jLabel9.setText("Output directory");
		txtDocPath.setPreferredSize(new Dimension(300, 21));
		cmbDocType.setMinimumSize(new Dimension(150, 21));
		cmbDocType.setPreferredSize(new Dimension(150, 21));
		btnChoosePath.setMaximumSize(new Dimension(21, 21));
		btnChoosePath.setMinimumSize(new Dimension(21, 21));
		contentPane.setLayout(gridBagLayout6);

		jTabbedPane1.addMouseListener(new MouseAdapter()  {
			public void mouseClicked(MouseEvent me) {
				tabbedPane_mouseClicked(me);
			}
		});

		jPanel1.setLayout(gridBagLayout1);
		jPanel2.setLayout(gridBagLayout2);
		jPanel3.setLayout(gridBagLayout3);
		jPanel4.setLayout(gridBagLayout4);
		jPanel5.setLayout(gridBagLayout5);
		jPanel6.setLayout(flowLayout1);
		jPanel7.setLayout(gridBagLayout7);

		jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		jScrollPane.getViewport().setBackground(Color.white);
		jEditPane.setCaretPosition(0);
		jEditPane.setFont(new java.awt.Font("Courier", 0, 11));
		jEditPane.setEditable(false);

		jMenuFile.setText("File");
		jMenuFileExit.setText("Exit");
		jMenuFileExit.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent e) {
				jMenuFileExit_actionPerformed(e);
			}
		});
		jMenuHelp.setText("Help");
		jMenuHelpAbout.setText("About");
		jMenuHelpAbout.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent e) {
				jMenuHelpAbout_actionPerformed(e);
			}
		});

		jLabel1.setText("Company");
		jLabel2.setText("Task");
		btnPunchIn.setText("Punch In");
		btnPunchIn.setToolTipText("Click here to start working");
		btnPunchIn.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent e) {
				btnPunchIn_actionPerformed(e);
			}
		});

		btnPunchOut.setToolTipText("Click here to finish working");
		btnPunchOut.setText("Punch Out");
		btnPunchOut.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent e) {
				btnPunchOut_actionPerformed(e);
			}
		});

		btnGenDoc.setText("Generate Document");
		btnGenDoc.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent e) {
				btnGenDoc_actionPerformed(e);
			}
		});

		btnChoosePath.setText("...");
		btnChoosePath.setPreferredSize(new Dimension(21, 21));
		btnChoosePath.setToolTipText("Change output directory");
		btnChoosePath.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent e) {
				btnChoosePath_actionPerformed(e);
			}
		});

		cmbCompany.setPreferredSize(new Dimension(200, 21));
		cmbTask.setPreferredSize(new Dimension(200, 21));
		cmbComment.setEditable(true);
		lastWorkTime.setAlignmentX((float) 0.5);
		lastWorkTime.setHorizontalAlignment(SwingConstants.CENTER);
		lastWorkTime.setVerticalAlignment(SwingConstants.TOP);
		jLabel3.setText("Start Date");
		jLabel6.setText("End Date");

		spnRange.setToolTipText("Adjust date range");
		jLabel4.setText("Date");
		jLabel5.setText("Description");
		jLabel10.setText("Amount");
		txtExpenseAmount.setMinimumSize(new Dimension(100, 21));
		txtExpenseAmount.setPreferredSize(new Dimension(100, 21));
		chkHST.setText("HST Applicable");

		btnAddExpense.setToolTipText("Add a new expense");
		btnAddExpense.setText("New");
		btnAddExpense.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent e) {
				btnAddExpense_actionPerformed(e);
			}
		});

		btnEditExpense.setToolTipText("Edit the displayed expense");
		btnEditExpense.setText("Edit");
		btnEditExpense.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent e) {
				btnEditExpense_actionPerformed(e);
			}
		});

		btnDeleteExpense.setToolTipText("Delete the displayed expense");
		btnDeleteExpense.setText("Delete");
		btnDeleteExpense.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent e) {
				btnDeleteExpense_actionPerformed(e);
			}
		});
		
		btnAddWorkDayAmount.setToolTipText("Add a new work amount");
		btnAddWorkDayAmount.setText("Add Time Worked");
		btnAddWorkDayAmount.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent e) {
				btnAddWorkDayAmount_actionPerformed(e);
			}
		});

		cmbExpenseDesc.setEditable(true);

		jLabelPunchInOffset.setText("Offset");
		spnPunchInOffset.setPreferredSize(new Dimension(50, 25));

		jLabelPunchOutOffset.setText("Offset");
		spnPunchOutOffset.setPreferredSize(new Dimension(50, 25));

		elapsedTime.setMinimumSize(new Dimension(0, 17));
		elapsedTime.setPreferredSize(new Dimension(0, 17));
		elapsedTime.setHorizontalAlignment(SwingConstants.CENTER);
		periodRatio.setMinimumSize(new Dimension(0, 17));
		periodRatio.setPreferredSize(new Dimension(0, 17));
		periodRatio.setHorizontalAlignment(SwingConstants.CENTER);

		jLabel12.setText("GST/HST Collected");
		jLabel13.setText("Income");
		jLabel14.setText("Total Taxes Due");
		jLabel15.setText("Tax Year");
		jLabel16.setText("Income X");
		jLabel17.setText("%");
		txtGST.setMinimumSize(new Dimension(90, 21));
		txtGST.setPreferredSize(new Dimension(90, 21));
		txtGST.setToolTipText("");
		txtGST.setEditable(false);
		txtGST.setHorizontalAlignment(SwingConstants.RIGHT);
		txtIncome.setMinimumSize(new Dimension(90, 21));
		txtIncome.setPreferredSize(new Dimension(90, 21));
		txtIncome.setEditable(false);
		txtIncome.setHorizontalAlignment(SwingConstants.RIGHT);
		txtTaxTotal.setMinimumSize(new Dimension(90, 21));
		txtTaxTotal.setPreferredSize(new Dimension(90, 21));
		txtTaxTotal.setEditable(false);
		txtTaxTotal.setHorizontalAlignment(SwingConstants.RIGHT);
		cmbTaxYear.setMinimumSize(new Dimension(75, 21));
		cmbTaxYear.setPreferredSize(new Dimension(75, 21));
		txtPercent.setMinimumSize(new Dimension(90, 21));
		txtPercent.setPreferredSize(new Dimension(90, 21));
		txtPercent.setEditable(false);
		txtPercent.setHorizontalAlignment(SwingConstants.RIGHT);
		txtWorkDayHours.setMinimumSize(new Dimension(30, 21));
		txtWorkDayHours.setPreferredSize(new Dimension(30, 21));
		txtWorkDayHours.setEditable(true);
		txtWorkDayHours.setHorizontalAlignment(SwingConstants.RIGHT);

		spnIncomeTaxPercent.setPreferredSize(new Dimension(50, 25));
		spnIncomeTaxPercent.getModel().setValue(new Integer(incomeTaxRate));
		spnIncomeTaxPercent.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				spnIncomeTaxPercent_actionPerformed(e);
			}
		});

		jLabel18.setText("Current pay period progress");
		jLabel18.setHorizontalAlignment(SwingConstants.CENTER);
		periodProgress.setStringPainted(true);
		jMenuFile.add(jMenuFileExit);
		jMenuHelp.add(jMenuHelpAbout);
		jMenuBar1.add(jMenuFile);
		jMenuBar1.add(jMenuHelp);
		
		jLabel19.setText("Work Day Date");
		jLabel20.setText("Hours");

		contentPane.add(jPanel4,   new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 35, 0));
		jPanel4.add(jLabel1,              new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
		jPanel4.add(cmbTask, gridBagConstraints2);
		jPanel4.add(jLabel2, gridBagConstraints1);
		jPanel4.add(cmbCompany, gridBagConstraints11);
		jPanel4.add(getJCompanyTaskButton(), gridBagConstraints);
		jTabbedPane1.add(jPanel1, "Work");
		jPanel1.add(jLabelPunchInOffset,	new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.SOUTH, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
		jPanel1.add(jLabelPunchOutOffset,	new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.SOUTH, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
		jPanel1.add(btnPunchIn,						new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
		jPanel1.add(spnPunchInOffset,			new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		jPanel1.add(offsetTime,						new GridBagConstraints(2, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 5), 0, 0));
		jPanel1.add(spnPunchOutOffset,		new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		jPanel1.add(btnPunchOut,					new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
		
		jPanel1.add(jLabel19,							new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
	            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 5, 0, 0), 0, 0));
		jPanel1.add(cmbWorkDayDate,						new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
	            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
		jPanel1.add(jLabel20,							new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
	            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 5, 0, 0), 0, 0));
		jPanel1.add(txtWorkDayHours,					new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
	            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
		jPanel1.add(btnAddWorkDayAmount,				new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0
	            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
		jPanel1.add(jLabel21,							new GridBagConstraints(3, 3, 2, 1, 0.0, 0.0
	            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
				
		jPanel1.add(jLabel7,							new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 5, 0, 0), 0, 0));
		jPanel1.add(cmbComment,						new GridBagConstraints(0, 5, 5, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 5), 0, 0));
		
		jPanel1.add(elapsedTime, gridBagConstraints112);
		jPanel1.add(periodRatio, gridBagConstraints4);
		jPanel1.add(accruedEarnings, gridBagConstraints3);
		jPanel1.add(jLabel18, gridBagConstraints110);
		jPanel1.add(periodProgress, gridBagConstraints5);
		jPanel1.add(lastWorkTime, gridBagConstraints23);
		
		jTabbedPane1.add(jPanel2, "Expenses");
		jPanel2.add(jLabel4,          new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 5, 0, 0), 0, 0));
		jPanel2.add(cmbExpenseDate,           new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
		jPanel2.add(jLabel5,            new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
		jPanel2.add(cmbExpenseDesc,              new GridBagConstraints(0, 3, 3, 1, 1.0, 0.0
						,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 5), 0, 0));
		jPanel2.add(jLabel10,          new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 5, 0, 0), 0, 0));
		jPanel2.add(txtExpenseAmount,          new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
		jPanel2.add(chkHST,       new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
		jPanel2.add(jPanel6,      new GridBagConstraints(0, 4, 3, 1, 1.0, 1.0
						,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 0, 0, 0), 0, 0));
		jPanel6.add(btnAddExpense, null);
		jPanel6.add(btnEditExpense, null);
		jPanel6.add(btnDeleteExpense, null);

		jTabbedPane1.add(jPanel3, "Billing");
		jPanel3.add(jLabel3,                             new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 5, 0, 0), 0, 0));
		jPanel3.add(cmbStartDate,                                      new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
		jPanel3.add(cmbEndDate,                               new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
		jPanel3.add(jLabel6,                      new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 5, 0, 0), 0, 0));
		jPanel3.add(jScrollPane,                               new GridBagConstraints(0, 2, 3, 1, 1.0, 1.0
						,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 0, 5), 0, 0));
		jPanel3.add(spnRange,                     new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0));

		jPanel5.add(jLabel8,            new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
		jPanel5.add(cmbDocType,         new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
		jPanel5.add(btnGenDoc,         new GridBagConstraints(1, 0, 1, 2, 0.0, 0.0
						,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		jPanel5.add(jLabel9,     new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
		jPanel5.add(txtDocPath,       new GridBagConstraints(0, 3, 2, 1, 1.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 5, 5), 0, 0));
		jPanel5.add(btnChoosePath,         new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0));

		jPanel3.add(jPanel5,   new GridBagConstraints(0, 3, 3, 1, 0.0, 0.0
						,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));

		jTabbedPane1.add(jPanel7, "Taxes");
		jPanel7.add(jLabel15, gridBagConstraints22);
		jPanel7.add(cmbTaxYear, gridBagConstraints21);
		jPanel7.add(txtTaxTotal, gridBagConstraints18);
		jPanel7.add(jLabel14, gridBagConstraints12);
		jPanel7.add(txtIncome, gridBagConstraints20);
		jPanel7.add(jLabel13,           new GridBagConstraints(1, 1, 2, 1, 0.0, 0.0
						,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0));
		jPanel7.add(txtGST, gridBagConstraints17);
		jPanel7.add(jLabel12, gridBagConstraints14);
		jPanel7.add(jLabel16, gridBagConstraints111);
		jPanel7.add(txtPercent, gridBagConstraints16);
		jPanel7.add(spnIncomeTaxPercent, gridBagConstraints13);
		jPanel7.add(jLabel17, gridBagConstraints15);
		jPanel7.add(getTxtCPPContribution(), gridBagConstraints10);
		jPanel7.add(jLabelCPP, gridBagConstraints19);
		contentPane.add(jTabbedPane1,  new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
						,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10, 5, 5, 5), 0, 173));

		cmbStartDate.setDateFormatter(new MSimpleDateFormat("MM/dd/yyyy"));
		cmbStartDate.setPreferredSize(new Dimension(100, 21));
		cmbStartDate.setMinimumSize(new Dimension(100, 21));
		pdcStartDate.firstDay = Calendar.SUNDAY;
		pdcStartDate.changerStyle = MDateChanger.BUTTON;
		pdcStartDate.hasShadow = true;
		cmbStartDate.setConstraints(pdcStartDate);
		cmbStartDate.setEditable(false);
		cmbStartDate.addMChangeListener(new MChangeListener() {
			public void valueChanged(MChangeEvent e) {
				cmbStartDate_valueChanged(e);
			}
		});

		cmbEndDate.setDateFormatter(new MSimpleDateFormat("MM/dd/yyyy"));
		cmbEndDate.setPreferredSize(new Dimension(100, 21));
		cmbEndDate.setMinimumSize(new Dimension(100, 21));
		pdcEndDate.firstDay = Calendar.SUNDAY;
		pdcEndDate.changerStyle = MDateChanger.BUTTON;
		pdcEndDate.hasShadow = true;
		cmbEndDate.setConstraints(pdcEndDate);
		cmbEndDate.setEditable(false);
		cmbEndDate.addMChangeListener(new MChangeListener() {
			public void valueChanged(MChangeEvent e)  {
				cmbEndDate_valueChanged(e);
			}
		});

		spnRange.setEditor(new DateEditor("MM/dd/yyyy"));
		spnRange.setPreferredSize(new Dimension(21, 22));
		spnRange.setMinimumSize(new Dimension(21, 22));
		spnRange.addMChangeListener(new MChangeListener() {
			public void valueChanged(MChangeEvent e) {
				spnRange_valueChanged(e);
			}
		});

		cmbExpenseDate.setDateFormatter(new MSimpleDateFormat("MM/dd/yyyy"));
		cmbExpenseDate.setPreferredSize(new Dimension(100, 21));
		cmbExpenseDate.setMinimumSize(new Dimension(100, 21));
		pdcExpenseDate.firstDay = Calendar.SUNDAY;
		pdcExpenseDate.changerStyle = MDateChanger.BUTTON;
		pdcExpenseDate.hasShadow = true;
		cmbExpenseDate.setConstraints(pdcExpenseDate);
		cmbExpenseDate.setEditable(false);
		cmbExpenseDate.addMChangeListener(new MChangeListener() {
			public void valueChanged(MChangeEvent e) {
				cmbExpenseDate_valueChanged(e);
			}
		});
		
		cmbWorkDayDate.setDateFormatter(new MSimpleDateFormat("MM/dd/yyyy"));
		cmbWorkDayDate.setPreferredSize(new Dimension(100, 21));
		cmbWorkDayDate.setMinimumSize(new Dimension(100, 21));
		pdcWorkDayDate.firstDay = Calendar.SUNDAY;
		pdcWorkDayDate.changerStyle = MDateChanger.BUTTON;
		pdcWorkDayDate.hasShadow = true;
		cmbWorkDayDate.setConstraints(pdcWorkDayDate);
		cmbWorkDayDate.setEditable(false);
		cmbWorkDayDate.addMChangeListener(new MChangeListener() {
			public void valueChanged(MChangeEvent e) {
				cmbWorkDayDate_valueChanged(e);
			}
		});

		txtExpenseAmount.setInputVerifier(new CurrencyVerifier());

		cmbDocType.addItem("Invoice");
		cmbDocType.addItem("Timesheet");

		this.setJMenuBar(jMenuBar1);
	}

	private void dbInit()
	{
/*		
		Connection con = null;
		ResultSet rs = null;
		
		try {
			try {
				Driver driver = (Driver)Class.forName("com.mysql.jdbc.Driver").newInstance();
				DriverManager.registerDriver(driver);
				con = DriverManager.getConnection("jdbc:mysql://localhost:3306/Consulting?user=cweisbrod&password=karen27&autoReconnect=true");
			}
			catch(ClassNotFoundException e) {
				System.err.println("ClassNotFoundException: " + e.getMessage());
			}
			catch(InstantiationException e) {
				System.err.println("InstantiationException: " + e.getMessage());
			}
			catch(IllegalAccessException e) {
				System.err.println("IllegalAccessException: " + e.getMessage());
			}
		
			rs = con.createStatement().executeQuery("SELECT VERSION()");
			rs.next();
			System.out.println(rs.getString(1));
		}
		catch(SQLException e) {
			System.err.println("SQLException: " + e.getMessage());
			System.err.println("SQLState: " + e.getSQLState());
			System.err.println("VendorError: " + e.getErrorCode());
		}
		finally {
			try {
				if(con != null)
					con.close();
			}
			catch(SQLException e) {}
		}
*/		
		try
		{
			ds = new DataSourceProxy("punchclock");
		}
		catch(Exception e)
		{
			cat.fatal("Exception: " + e.toString());
			System.exit(1);
		}
	}

	private void initControls()
	{
		//Enable "Punch In" and "Punch Out" buttons according to existence of active timelog record
		int timelogID, companyID, taskID;

		companyID = -1;
		timelogID = getCurrentTimerUpdateInfo();
		btnPunchIn.setEnabled(timelogID == -1);
		spnPunchInOffset.setEnabled(timelogID == -1);
		jLabelPunchInOffset.setEnabled(timelogID == -1);
		cmbCompany.setEnabled(timelogID == -1);
		cmbTask.setEnabled(timelogID == -1);
		cmbComment.setEnabled(timelogID == -1);
		btnPunchOut.setEnabled(timelogID != -1);
		spnPunchOutOffset.setEnabled(timelogID != -1);
		jLabelPunchOutOffset.setEnabled(timelogID != -1);

		if(timelogID != -1)
			offsetTime.setHorizontalAlignment(SwingConstants.RIGHT);
		else
			offsetTime.setHorizontalAlignment(SwingConstants.LEFT);

		//Set controls to appropriate values based on active timelog record (if one exists).
		//Otherwise set controls based on last recorded timelog record
		String company = null, task = null, comment = null;
		if(timelogID != -1)
		{
			punchInTime = DateTime.getDateFromDateString(DBMethods.getTableString(ds, "timelog", "start", "start = end"));
			lastWorkTime.setText(CLICK_PUNCH_OUT);
		}
		else
			timelogID = DBMethods.getTableInt(ds, "timelog", "MAX(id)", "id = id");
		if(timelogID != -1)
		{
			comment = DBMethods.getTableString(ds, "timelog", "comment", "id = " + timelogID);

			companyID = DBMethods.getTableInt(ds, "timelog", "companyID", "id = " + timelogID);
			if(companyID != -1)
				company = DBMethods.getTableString(ds, "companies", "name", "id = " + companyID);

			taskID = DBMethods.getTableInt(ds, "timelog", "taskID", "id = " + timelogID);
			if(taskID != -1)
				task = DBMethods.getTableString(ds, "tasks", "name", "id = " + taskID);
		}

		//Set company combobox selection
		if(company != null)
		{
			cmbCompany.setSelectedItem(company);

			DBMethods.populateTasks(ds, cmbTask, cmbCompany.getSelectedItem().toString());
			DBMethods.populateComments(ds, cmbComment, cmbCompany.getSelectedItem().toString(), task);

			//Set dtRange based on company
			setDtRange(company);

			//Set punch offsets
			setSpinPunchOffsets(companyID);

			//Update hours per week
			hoursPerWeek = DBMethods.getTableFloat(ds, "companies", "hoursPerWeek", "name='" + company + "'");
		}
		else
			cmbCompany.setSelectedIndex(0);

		//Set task combobox selection
		if(task != null)
			cmbTask.setSelectedItem(task);
		else
			cmbTask.setSelectedIndex(0);
		
		// Initialize cmbWorkDayDate to the date of the last timelog record
		Date lastDate = DBMethods.getLastTimelogEndDate(ds);
		if (lastDate != null)
			cmbWorkDayDate.setValue(lastDate);

		//Set comment combobox selection
		if(comment != null)
			cmbComment.setSelectedItem(comment);
		else
			cmbComment.setSelectedIndex(cmbComment.getItemCount()-1);

		//Populate Tax Year combo box
		DBMethods.populateComboBox(ds, cmbTaxYear, SQL_SELECT_TAX_YEARS, true);

		//Update the number of seconds logged for today and for selected period
		captionDateFormatter = new SimpleDateFormat("EEEEE, MMMMM d, yyyy  h:mm:ss a");
		captionUpdate();
	}

	private void setDtRange(String company)
	{
		//Get billing period for specified company
		String billPeriod = DBMethods.getTableString(ds, "companies", "billPeriod", "name = '" + company + "'");
		if(billPeriod == null)
			billPeriod = "BiMonthly";

		try
		{
			//Construct appropriate DateRange object based on billPeriod
			if (billPeriod.equals("Weekly"))
			{
				dtRange = new WeeklyRange(Calendar.SUNDAY + Integer.valueOf(props.getSafeProperty("firstDayOfWeek")).intValue() - 1);
			}
			else if(billPeriod.equals("BiWeekly"))
			{
				//Get BiWeekly date
				String biWeeklyDate = DBMethods.getTableString(ds, "companies", "biWeeklyDate", "name = '" + company + "'");
				if(biWeeklyDate != null)
				{
					java.util.Date bwd = DateTime.getDateFromDateString(biWeeklyDate);
					dtRange = new BiWeeklyRange(bwd);
				}
			}
			else if(billPeriod.equals("BiMonthly"))
			{
				dtRange = new BiMonthlyRange();
			}
			else if(billPeriod.equals("Monthly"))
			{
				dtRange = new MonthlyRange();
			}

			cmbStartDate.setValue(dtRange.getStart());
			cmbEndDate.setValue(dtRange.getEnd());
			spnRange.setModel(new DateRangeSpinnerModel(dtRange));
//			cmbExpenseDate.setValue(dtRange.getStart());	// Set to beginning of billing period
			cmbExpenseDate.setValue(new java.util.Date());	// Set to current date

			//Set the period progress bar to the correct position
			java.util.Date curDt = new java.util.Date();
//			periodProgress.setValue((int)(100 * dtRange.getRangeProgressByWeekday(curDt)));
			periodProgress.setValue((int)(100 * dtRange.getRangeProgress(curDt)));

			//Force initialization of Expenses tab
			cmbExpenseDate_valueChanged(new MChangeEvent(this, dtRange.getStart(), MChangeEvent.PULLDOWN_CLOSED));
		}
		catch (Exception e)
		{
			cat.error(e.toString());
		}
	}

	private void setSpinPunchOffsets(int companyID)
	{
		if(companyID != -1)
		{
			int offset;
			offset = DBMethods.getTableInt(ds, "companies", "punchInOffset", "id = " + companyID);
			spnPunchInOffset.setValue(new Integer(offset));
			offset = DBMethods.getTableInt(ds, "companies", "punchOutOffset", "id = " + companyID);
			spnPunchOutOffset.setValue(new Integer(offset));
		}
	}

	private void outputBillHeader(java.util.Date dt1, java.util.Date dt2, StringBuffer buf)
	{
		buf.append(cmbCompany.getSelectedItem().toString() + " - " + cmbTask.getSelectedItem().toString() + "\n");
		buf.append("Period: " + DateTime.getDateString(dt1) + " - " + DateTime.getDateString(dt2) + "\n");
		buf.append("\n");
		buf.append("Start                End                  Duration\n");
		buf.append("--------------------------------------------------\n");
	}

	private long OutputPreMidnightRecord(Connection conn, Statement stmt, ResultSet rs,
										 int companyID, java.util.Date dt1, java.util.Date dt2,
										 StringBuffer buf, boolean headerOutput) throws SQLException
	{
		//Construct query to retrieve records
		long result = 0;
		String start = DateTime.getDateTimeString(dt1);
		Object[] insertArgs = {	new Integer(companyID),
								new String(start)};
		String sql = MessageFormat.format(SQL_SELECT_BILL_RECS_PREMIDNIGHT, insertArgs);

		//Perform query
		rs = stmt.executeQuery(sql);

		if(rs.first())
		{
			if(!headerOutput)
				outputBillHeader(dt1, dt2, buf);

			//Compute total time and output result
			long duration = DateTime.getDateDiff(start, rs.getString("end")) / 1000;
			result += duration;

			//Output record details to StringBuffer
			String strDuration;
			if(duration == 0)
				strDuration = "active";
			else
				strDuration = DateTime.getTimeString(duration);
			buf.append(start + "  " + rs.getString("end") + "  " + strDuration + "\n");
		}

		return result;
	}

	private long OutputNormalRecords(Connection conn, Statement stmt, ResultSet rs,
									 int companyID, java.util.Date dt1, java.util.Date dt2,
									 StringBuffer buf, boolean headerOutput) throws SQLException
	{
		//Construct query to retrieve records
		long result = 0;
		String start = DateTime.getDateTimeString(dt1);
		String end = DateTime.getDateTimeString(dt2);
		Object[] insertArgs = {	new Integer(companyID),
								new String(start),
								new String(end)};
		String sql = MessageFormat.format(SQL_SELECT_BILL_RECS, insertArgs);

		//Perform query
		rs = stmt.executeQuery(sql);

		if(rs.first())
		{
			if(!headerOutput)
				outputBillHeader(dt1, dt2, buf);

			//Compute total time and output result
			do
			{
				long duration = DateTime.getDateDiff(rs.getString("start"), rs.getString("end")) / 1000;
				result += duration;

				//Output record details to StringBuffer
				String strDuration;
				if(duration == 0)
					strDuration = "active";
				else
					strDuration = DateTime.getTimeString(duration);

				buf.append(DateTime.stripDecimalPortion(rs.getString("start")) + "  " + DateTime.stripDecimalPortion(rs.getString("end")) + "  " + strDuration + "\n");
//				buf.append(rs.getString("start") + "  " + rs.getString("end") + "  " + strDuration + "\n");
			} while(rs.next());
		}

		return result;
	}

	private long OutputPostMidnightRecord(Connection conn, Statement stmt, ResultSet rs,
										  int companyID, java.util.Date dt1, java.util.Date dt2,
										  StringBuffer buf, boolean headerOutput) throws SQLException
	{
		//Construct query to retrieve records
		long result = 0;
		String end = DateTime.getDateTimeString(new java.util.Date(dt2.getTime()-1000));
		Object[] insertArgs = {	new Integer(companyID),
								new String(end)};
		String sql = MessageFormat.format(SQL_SELECT_BILL_RECS_POSTMIDNIGHT, insertArgs);

		//Perform query
		rs = stmt.executeQuery(sql);

		if(rs.first())
		{
			if(!headerOutput)
				outputBillHeader(dt1, dt2, buf);

			//Compute total time and output result
			long duration = DateTime.getDateDiff(rs.getString("start"), end) / 1000;
			result += duration;

			//Output record details to StringBuffer
			String strDuration;
			if(duration == 0)
				strDuration = "active";
			else
				strDuration = DateTime.getTimeString(duration);
			buf.append(rs.getString("start") + "  " + end + "  " + strDuration + "\n");
		}

		return result;
	}

	private void computeBill()
	{
		//Get out of here if we're not looking at 2nd from last (Bill) pane
		if(jTabbedPane1.getSelectedIndex() != jTabbedPane1.getTabCount() - 2)
			return;

		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		long totalTime = 0;
		boolean headerOutput = false;
		StringBuffer buf = new StringBuffer();

		//Clear text area
		jEditPane.setText("");

		//Obtain companyID and taskID from combo boxes
		try
		{
			int companyID = DBMethods.getTableInt(ds, "companies", "id", "name='" + cmbCompany.getSelectedItem().toString() + "'");
			if (companyID != -1)
			{
				//Obtain start and end date strings.
//				java.util.Date dt1 = cmbStartDate.getValue();
//				java.util.Date dt2 = cmbEndDate.getValue();
				java.util.Date dt1 = dtRange.getStart();
				java.util.Date dt2 = dtRange.getEnd();
				
				// Add 1 day to end date because MDateEntryField truncates hours and minutes to zero
//				dt2.setTime(dt2.getTime() + 86400000);

				conn = ds.getConnection();
				stmt = conn.createStatement();

				long tm;

				//Find all records crossing start
				tm = OutputPreMidnightRecord(conn, stmt, rs, companyID, dt1, dt2, buf, headerOutput);
				totalTime += tm;
				if(tm > 0)
					headerOutput = true;

				//Find all records between start and end
				tm = OutputNormalRecords(conn, stmt, rs, companyID, dt1, dt2, buf, headerOutput);
				totalTime += tm;
				if(tm > 0)
					headerOutput = true;

				//Find all records crossing end
				totalTime += OutputPostMidnightRecord(conn, stmt, rs, companyID, dt1, dt2, buf, headerOutput);

				//Output total time
				if(buf.length() > 0)
				{
					buf.append("\n");
					buf.append("Total Time: ");
					buf.append(DateTime.getTimeString(totalTime));
				}

				//Now list any recorded expenses for specified date range
				Object[] insertArgs = {new Integer(companyID),
									   new String(DateTime.getDateTimeString(dt1)),
									   new String(DateTime.getDateTimeString(dt2))};
				String sql = MessageFormat.format(SQL_SELECT_EXPENSES, insertArgs);
				rs = stmt.executeQuery(sql);
				if(rs.first())
				{
					java.util.Date dt;
					String amount;
					NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance();
					buf.append("\n\n");
					buf.append("Expenses\n");
					buf.append("--------\n");
					do
					{
						dt = DateTime.getDateFromDateString(rs.getString("date"));
						amount = currencyFormatter.format((double)rs.getInt("amount")/100.0);
						buf.append(DateTime.getDateString(dt) + " " + rs.getString("description") + ": " + amount + "\n");
					}
					while(rs.next());
				}

				if(buf.length() > 0)
				{
					//Set the JEditorPane text
					jEditPane.setText(buf.toString());

					//Force the viewport to the top of the JEditorPane
					jEditPane.setCaretPosition(0);
				}
			}
		}
		catch(Exception e)
		{
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

	private void resetExpenseControls()
	{
		btnAddExpense.setEnabled(true);
		btnAddExpense.setText("New");
		btnEditExpense.setText("Edit");
		cmbExpenseDesc.setEditable(false);
		txtExpenseAmount.setEditable(false);
		chkHST.setEnabled(false);
		if(cmbExpenseDesc.getItemCount() > 0)
		{
			cmbExpenseDesc.setSelectedIndex(0);
			displayExpenseItem((ExpenseItem)cmbExpenseDesc.getSelectedItem());
			btnEditExpense.setEnabled(true);
			btnDeleteExpense.setEnabled(true);
		}
		else
		{
			txtExpenseAmount.setText("");
			btnEditExpense.setEnabled(false);
			btnDeleteExpense.setEnabled(false);
		}
	}

	private void displayExpenseItem(ExpenseItem item)
	{
		txtExpenseAmount.setText(IntegralCurrency.penniesToString(item.amount));
		chkHST.setSelected(item.hstApplicable != 0);
	}

	private void getDisplayedExpenseItem(ExpenseItem item)
	{
		try
		{
			item.date = cmbExpenseDate.getValue();
			item.amount = IntegralCurrency.stringToPennies(txtExpenseAmount.getText());
			item.hstApplicable = chkHST.isSelected()?1:0;
			item.description = cmbExpenseDesc.getEditor().getItem().toString();
		}
		catch(ParseException e)
		{
			cat.error(e.toString());
		}
	}

	public void captionUpdate()
	{
		java.util.Date currentDate = new java.util.Date();
		this.setTitle("Contractor's Punch Clock - " + captionDateFormatter.format(currentDate));
		
		//Update the number of seconds logged today
		Calendar cal = Calendar.getInstance();
		cal.setTime(new java.util.Date());
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(Calendar.AM_PM, Calendar.AM);
		java.util.Date dtStart = cal.getTime();
		java.util.Date dtEnd = currentDate;
		dtEnd.setTime(dtStart.getTime() + 86400000 - 1);
		
		int companyID = DBMethods.getTableInt(ds, "companies", "id", "name='" + cmbCompany.getSelectedItem().toString() + "'");

		secondsToday = DBMethods.getTimelogSeconds(ds, dtStart, dtEnd, companyID);
		penniesToday = DBMethods.getTimelogPennies(ds, dtStart, dtEnd, companyID);
		secondsPeriod = DBMethods.getTimelogSeconds(ds, dtRange.getStart(), dtRange.getEnd(), companyID);
		penniesPeriod = DBMethods.getTimelogPennies(ds, dtRange.getStart(), dtRange.getEnd(), companyID);

		if (punchInTime != null)
			elapsedTime.setText("Current elapsed time today: " + DateTime.getTimeString(secondsToday) + ", " + IntegralCurrency.penniesToCurrencyString(penniesToday));
		else
			elapsedTime.setText("Total time logged today: " + DateTime.getTimeString(secondsToday));

		UpdatePeriodRatio(secondsPeriod);
		
		accruedEarnings.setText("Total earnings this period: " + IntegralCurrency.penniesToCurrencyString(penniesPeriod));
	}
	
	private void UpdatePeriodRatio(long seconds)
	{
		long standardWorkTime;
		double periodRatioVal;

		// Compute standardWorkTime which based on hoursPerWeek is the number of seconds expected to have been worked
		// up to today's date for the current billing period.
		standardWorkTime = (long)((dtRange.getElapsedRangeWeekdays(new java.util.Date()) + 1) * 86400 * hoursPerWeek / 5 / 24);
		periodRatioVal = 100 * (double)seconds / (double)standardWorkTime;
		Object[] insertArgs = {new String(IntegralCurrency.doubleToString((double)seconds/3600, 2)),
								new String(IntegralCurrency.doubleToString((double)standardWorkTime/3600, 1)),
								new String(IntegralCurrency.doubleToString((double)periodRatioVal, 1))};
		String str = MessageFormat.format("{0} of {1} hours logged this period ({2}%).", insertArgs);
		periodRatio.setText(str);
	}

	public void offsetUpdate()
	{
		if(punchInTime != null)
			UpdatePunchOutOffset();
		else
			UpdatePunchInOffset();
	}

	private void UpdatePunchInOffset()
	{
		Integer v;
		int minutes;
		java.util.Date curTime = new java.util.Date();
		java.util.Date offset = new java.util.Date();

		v = (Integer)spnPunchInOffset.getValue();
		minutes = v.intValue();
		offset.setTime(curTime.getTime() - (long)(minutes * 60000));
		offsetTime.setText(DateTime.get12HourTimeString(offset, "hh:mm a"));
	}

	private void UpdatePunchOutOffset()
	{
		Integer v;
		int minutes;
		java.util.Date curTime = new java.util.Date();
		java.util.Date offset = new java.util.Date();

		v = (Integer)spnPunchOutOffset.getValue();
		minutes = v.intValue();
		offset.setTime(curTime.getTime() + (long)(minutes * 60000));
		offsetTime.setText(DateTime.get12HourTimeString(offset, "hh:mm a"));
	}

	private int getCurrentTimerUpdateInfo()
	{
		int timelogID = DBMethods.getTableInt(ds, "timelog", "id", "start = end");
		if(timelogID != -1)
		{
			punchInTime = DateTime.getDateFromDateString(DBMethods.getTableString(ds, "timelog", "start", "id = " + timelogID));
			int taskID = DBMethods.getTableInt(ds, "timelog", "taskID", "id = " + timelogID);
			if(taskID != -1)
				punchInRate = DBMethods.getTableInt(ds, "tasks", "rate", "id = " + taskID);
		}

		return timelogID;
	}

	private void computeTaxes()
	{
		Connection conn = null;
		Statement stmt1 = null, stmt2 = null;
		ResultSet rs1 = null, rs2 = null;
		Calendar cal = Calendar.getInstance();
		java.util.Date dtStart, dtEnd;
		
		int taxYear = Integer.parseInt(cmbTaxYear.getSelectedItem().toString());
		if (taxYear == 2016)
		{
			// In 2016, I fucked-up and lost some data in the timelog table, so instead of inspecting
			// the timelog records, we just ask for the total from the invoices table.
			try
			{
				cal.set(taxYear, 0, 1, 0, 0, 0);
				dtStart = cal.getTime();
				cal.set(taxYear + 1, 0, 1, 0, 0, 0);
				dtEnd = cal.getTime();
				hst = DBMethods.getTableRangeSum(ds, "invoices", "hst", dtStart, dtEnd);
				income = DBMethods.getTableRangeSum(ds, "invoices", "total", dtStart, dtEnd);
				if(hst != -1 && income != -1)
				{
					txtIncome.setText(IntegralCurrency.penniesToCurrencyString(income));
					txtPercent.setText(IntegralCurrency.penniesToCurrencyString(income * incomeTaxRate / 100));
					txtGST.setText(IntegralCurrency.penniesToCurrencyString(hst));
					txtTaxTotal.setText(IntegralCurrency.penniesToCurrencyString(hst + income * incomeTaxRate / 100));
				}
			}
			catch (NumberFormatException e)
			{
			}
		}
		else
		{
			income = 0;
			hst = 0;
			try
			{
				conn = ds.getConnection();
				stmt1 = conn.createStatement();
				
				cal.set(taxYear, 0, 1, 0, 0, 0);
				dtStart = cal.getTime();
				cal.set(taxYear + 1, 0, 1, 0, 0, 0);
				dtEnd = cal.getTime();
				
				// Obtain all taskID for the tax year
				String yearStart, yearEnd;
				yearStart = DateTime.getDateString(dtStart);
				yearEnd = DateTime.getDateString(dtEnd);
				Object[] insertArgs1 = {new String(yearStart),
										new String(yearEnd)};
				String sql = MessageFormat.format(SQL_SELECT_TASKIDS, insertArgs1);
				rs1 = stmt1.executeQuery(sql);
				if (rs1.first())
				{
					// For each taskID
					do
					{
						int taskID = rs1.getInt("taskID");
						
						// Obtain the rate for the task
						int taskRate = DBMethods.getTableInt(ds, "tasks", "rate", "id = " + taskID);
						
						// Get all timelog entries for given task
						Object[] insertArgs2 = {taskID,
												new String(yearStart),
												new String(yearEnd)};
						sql = MessageFormat.format(SQL_SELECT_TIMELOGS_BY_TASKID, insertArgs2);
						stmt2 = conn.createStatement();
						rs2 = stmt2.executeQuery(sql);
						if (rs2.first())
						{
							do
							{
								long milliseconds = DateTime.getDateDiff(rs2.getString("start"), rs2.getString("end"));
								long pennies = milliseconds * taskRate / 3600000;
								income += pennies;
							} while(rs2.next());
						}
					} while(rs1.next());
				}
				
				txtIncome.setText(IntegralCurrency.penniesToCurrencyString(income));
				txtPercent.setText(IntegralCurrency.penniesToCurrencyString(income * incomeTaxRate / 100));
				txtGST.setText(IntegralCurrency.penniesToCurrencyString(hst));
				txtTaxTotal.setText(IntegralCurrency.penniesToCurrencyString(hst + income * incomeTaxRate / 100));
			}
			catch (NumberFormatException e)
			{
			}
			catch(Exception e)
			{
				cat.error(e.toString());
			}
			finally
			{
				try
				{
					if (rs1 != null)
						rs1.close();
					if (stmt1 != null)
						stmt1.close();
					if (conn != null)
						conn.close();
				}
				catch (Exception e)
				{
				}
			}
		}
	}

	/**Tabbed Pane state change listener*/
	public void tabbedPane_mouseClicked(MouseEvent me)
	{
		java.awt.Rectangle rect = null;

		//Force bill to be computed if "Bill" tab is clicked.
		rect = jTabbedPane1.getBoundsAt(2);
		if(rect != null)
		{
			if(rect.contains(me.getPoint()))
			{
				//Compute bill for initial period
				computeBill();

				//Force txtDocPath to be updated
				cmbDocType.setSelectedIndex(0);
			}
		}

		//Force tax info to be computed if "tax" tab is clicked.
		rect = jTabbedPane1.getBoundsAt(3);
		if(rect != null)
		{
			if(rect.contains(me.getPoint()))
			{
				//Compute bill for initial period
				computeTaxes();
			}
		}
	}

	/**Company combo box action listener*/
	public void cmbCompany_actionPerformed(ActionEvent ae)
	{
		//A different company was selected.

		//Set dtRange based on company
		setDtRange(cmbCompany.getSelectedItem().toString());

		//Update hours per week
		hoursPerWeek = DBMethods.getTableFloat(ds, "companies", "hoursPerWeek", "name='" + cmbCompany.getSelectedItem().toString() + "'");

		captionUpdate();

		//Set punch offsets
		int companyID = DBMethods.getTableInt(ds, "companies", "id", "name='" + cmbCompany.getSelectedItem().toString() + "'");
		if (companyID != -1)
			setSpinPunchOffsets(companyID);

		//Update the tasks combobox.
		ignoreEvents = true;
		DBMethods.populateTasks(ds, cmbTask, cmbCompany.getSelectedItem().toString());
		if(cmbTask.getSelectedIndex() != -1)
			DBMethods.populateComments(ds, cmbComment, cmbCompany.getSelectedItem().toString(), cmbTask.getSelectedItem().toString());
		else
			cmbComment.removeAllItems();
		
		// Disable punch-in button if the comments combobox is empty
		if (cmbComment.getItemCount() > 0)
		{
			btnPunchIn.setEnabled(true);
			spnPunchInOffset.setEnabled(true);
			jLabelPunchInOffset.setEnabled(true);
			offsetTime.setEnabled(true);
		}
		else
		{
			btnPunchIn.setEnabled(false);
			spnPunchInOffset.setEnabled(false);
			jLabelPunchInOffset.setEnabled(false);
			offsetTime.setEnabled(false);
		}

		computeBill();

		//Force txtDocPath to be updated
		cmbDocType.setSelectedIndex(0);

		ignoreEvents = false;
	}

	/**BillTask combo box action listener*/
	public void cmbTask_actionPerformed(ActionEvent ae)
	{
		if(!ignoreEvents)
		{
			DBMethods.populateComments(ds, cmbComment, cmbCompany.getSelectedItem().toString(), cmbTask.getSelectedItem().toString());
	
			// Disable punch-in button if the comments combobox is empty
			if (cmbComment.getItemCount() > 0)
			{
				btnPunchIn.setEnabled(true);
				spnPunchInOffset.setEnabled(true);
				jLabelPunchInOffset.setEnabled(true);
				offsetTime.setEnabled(true);
			}
			else
			{
				btnPunchIn.setEnabled(false);
				spnPunchInOffset.setEnabled(false);
				jLabelPunchInOffset.setEnabled(false);
				offsetTime.setEnabled(false);
			}
	
			//This check is required to avoid multiple computeBill() calls for each task when
			//they are added on a bill company change event.
			//If the task changed, compute the bill.
			computeBill();
		}
	}
	
	/**Comment combo box action listener*/
	public void cmbComment_actionPerformed(ActionEvent ae)
	{
		btnPunchIn.setEnabled(true);
		spnPunchInOffset.setEnabled(true);
		jLabelPunchInOffset.setEnabled(true);
		offsetTime.setEnabled(true);
		
		// Get current comment
		String selectedItem = (String)cmbComment.getSelectedItem();
		if (selectedItem != null)
		{
			// If the comment is not in the list, add it.
			boolean found = false;
			for (int i = 0; i < cmbComment.getItemCount(); i++)
			{
				String itemString = (String)cmbComment.getItemAt(i);
				if (selectedItem.equals(itemString))
				{
					found = true;
					break;
				}
			}
			if (found == false)
			{
				cmbComment.insertItemAt(selectedItem, 0);
				cmbComment.setSelectedIndex(0);
			}
		}
	}

	/**Start date changed action listener*/
	public void cmbStartDate_valueChanged(MChangeEvent ce)
	{
		if(ce.getType() == MChangeEvent.PULLDOWN_CLOSED)
		{
			try
			{
				dtRange.setRange(cmbStartDate.getValue());
			}
			catch(ParseException e)
			{
				cat.error(e.toString());
			}
			computeBill();
		}
	}

	/**End date changed action listener*/
	public void cmbEndDate_valueChanged(MChangeEvent ce)
	{
		if(ce.getType() == MChangeEvent.PULLDOWN_CLOSED)
		{
			try
			{
				dtRange.setRange(cmbEndDate.getValue());
			}
			catch(ParseException e)
			{
				cat.error(e.toString());
			}
			computeBill();
		}
	}

	public void spnRange_valueChanged(MChangeEvent ce)
	{
		cmbStartDate.setValue(dtRange.getStart());
		cmbEndDate.setValue(dtRange.getEnd());
		computeBill();
	}

	/**Expense Description combo box action listener*/
	public void cmbExpenseDesc_actionPerformed(ActionEvent ae)
	{
		if(cmbExpenseDesc.getSelectedIndex() != -1)
		{
			if(!ignoreEvents)
			{
				//A different expense description was selected. Update the amount for this expense.
				displayExpenseItem((ExpenseItem)cmbExpenseDesc.getSelectedItem());
			}
		}
	}

	/**Expense date changed action listener*/
	public void cmbExpenseDate_valueChanged(MChangeEvent ce)
	{
		if(ce.getType() == MChangeEvent.PULLDOWN_CLOSED)
		{
			ignoreEvents = true;
			try
			{
				//Populate Expense description combobox with all expenses for selected date and company
				if(DBMethods.populateExpenseDescriptions(ds,
														 cmbExpenseDesc,
														 DateTime.getDateString(cmbExpenseDate.getValue()),
														 cmbCompany.getSelectedItem().toString()))
				{
					resetExpenseControls();
				}
			}
			catch(ParseException e)
			{
				cat.error(e.toString());
			}
			ignoreEvents = false;
		}
	}
	
	/**Work day date changed action listener*/
	public void cmbWorkDayDate_valueChanged(MChangeEvent ce)
	{
		if (ce.getType() == MChangeEvent.PULLDOWN_CLOSED)
		{
		}
	}

	/**docType combo box action listener*/
	public void cmbDocType_actionPerformed(ActionEvent ae)
	{
		//Update txtDocPath with appropriate path based on selected company and doc type.
		int companyID = DBMethods.getTableInt(ds, "companies", "id", "name='" + cmbCompany.getSelectedItem().toString() + "'");
		String docType = null;

		if(companyID != -1)
		{
			switch(cmbDocType.getSelectedIndex())
			{
			case 0: //Invoice
				docType = "invoice";
				break;
			case 1: //Time sheet
				docType = "timesheet";
				break;
			}
			if(docType != null)
			{
				String path = DBMethods.getCompanyPath(ds, companyID, docType);
				if(path != null)
					txtDocPath.setText(path.replace('/', '\\'));
				else
					txtDocPath.setText("");
			}
		}
	}

	/**Punch In action listener*/
	public void btnPunchIn_actionPerformed(ActionEvent ae)
	{
		if (cmbComment.getSelectedIndex() == -1)
			return;
		
		Integer v = (Integer)spnPunchInOffset.getValue();
		int companyID = DBMethods.getTableInt(ds, "companies", "id", "name='" + cmbCompany.getSelectedItem().toString() + "'");

		if(DBMethods.insertNewTimelogEntry(ds,
											cmbCompany.getSelectedItem().toString(),
											cmbTask.getSelectedItem().toString(),
											cmbComment.getSelectedItem().toString(),
											v.intValue(),
											null,
											null))
		{
			btnPunchIn.setEnabled(false);
			spnPunchInOffset.setEnabled(false);
			jLabelPunchInOffset.setEnabled(false);
			cmbCompany.setEnabled(false);
			cmbTask.setEnabled(false);
			cmbComment.setEnabled(false);
			btnPunchOut.setEnabled(true);
			spnPunchOutOffset.setEnabled(true);
			jLabelPunchOutOffset.setEnabled(true);
			offsetTime.setHorizontalAlignment(SwingConstants.RIGHT);
			lastWorkTime.setText(CLICK_PUNCH_OUT);
			getCurrentTimerUpdateInfo();
		}

		if(companyID != -1)
			DBMethods.setTableInt(ds, "companies", "punchInOffset", v.intValue(), "id = " + companyID);
	}

	/**Punch Out action listener*/
	public void btnPunchOut_actionPerformed(ActionEvent ae)
	{
		//Only allow a punch-out if comment is specified
		if(cmbComment.getSelectedItem().toString().length() > 0)
		{
			Integer v = (Integer)spnPunchOutOffset.getValue();
			int companyID = DBMethods.getTableInt(ds, "companies", "id", "name='" + cmbCompany.getSelectedItem().toString() + "'");
			long duration = DBMethods.updateNewTimelogEntry(ds, cmbComment.getSelectedItem().toString(), v.intValue());
			if(duration > 0)
			{
				punchInTime = null;
				captionUpdate();
				lastWorkTime.setText(DateTime.getTimeString(duration) + " recorded for this task.");

				//Enable correct buttons
				btnPunchIn.setEnabled(true);
				spnPunchInOffset.setEnabled(true);
				jLabelPunchInOffset.setEnabled(true);
				cmbCompany.setEnabled(true);
				cmbTask.setEnabled(true);
				cmbComment.setEnabled(true);
				btnPunchOut.setEnabled(false);
				spnPunchOutOffset.setEnabled(false);
				jLabelPunchOutOffset.setEnabled(false);
				offsetTime.setHorizontalAlignment(SwingConstants.LEFT);
			}

			if(companyID != -1)
				DBMethods.setTableInt(ds, "companies", "punchOutOffset", v.intValue(), "id = " + companyID);
		}
		else
		{
			JOptionPane.showMessageDialog(null, ENTER_COMMENT, "Comment Required", JOptionPane.INFORMATION_MESSAGE);
			cmbComment.grabFocus();
		}
	}

	public void btnCompanyTask_actionPerformed(ActionEvent ae)
	{
		companyTaskDialog = new CompanyTaskDlg(this);

		//Center the window
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension frameSize = companyTaskDialog.getSize();
		if (frameSize.height > screenSize.height) {
			frameSize.height = screenSize.height;
		}
		if (frameSize.width > screenSize.width) {
			frameSize.width = screenSize.width;
		}
		companyTaskDialog.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
		companyTaskDialog.setVisible(true);
	}

	/**Add Expense action listener*/
	public void btnAddExpense_actionPerformed(ActionEvent ae)
	{
		if(btnAddExpense.getText().equals("Add"))
		{
			int companyID = DBMethods.getTableInt(ds, "companies", "id", "name='" + cmbCompany.getSelectedItem().toString() + "'");

			try
			{
				ExpenseItem item = new ExpenseItem(0, companyID, cmbExpenseDate.getValue(),
													 IntegralCurrency.stringToPennies(txtExpenseAmount.getText()),
													 chkHST.isSelected()?1:0,
													 cmbExpenseDesc.getEditor().getItem().toString());
				item.id = DBMethods.insertExpense(ds, item);

				//Add this expense to the combo box
				if(item.id != 0 && item.description.length() > 0)
				{
					cmbExpenseDesc.addItem(item);
					btnEditExpense.setEnabled(true);
					btnDeleteExpense.setEnabled(true);
				}
			}
			catch(Exception e)
			{
				cat.error(e.toString());
			}

			btnAddExpense.setText("New");
		}
		else
		{
			txtExpenseAmount.setText("");
			txtExpenseAmount.setEditable(true);
			chkHST.setSelected(false);
			chkHST.setEnabled(true);
			cmbExpenseDesc.setEditable(true);
			cmbExpenseDesc.getEditor().setItem("");
			btnAddExpense.setText("Add");
			btnEditExpense.setEnabled(false);
			btnDeleteExpense.setEnabled(false);
		}
	}

	/**Edit Expense action listener*/
	public void btnEditExpense_actionPerformed(ActionEvent ae)
	{
		if(btnEditExpense.getText().equals("Edit"))
		{
			btnAddExpense.setEnabled(false);
			btnDeleteExpense.setEnabled(false);
			btnEditExpense.setText("Update");

			//Enable controls
			txtExpenseAmount.setEditable(true);
			chkHST.setEnabled(true);
			cmbExpenseDesc.setEditable(true);
		}
		else
		{
			//Read the item from the controls
			getDisplayedExpenseItem((ExpenseItem)cmbExpenseDesc.getSelectedItem());

			//Update the expense record in the database
			if(DBMethods.updateExpense(ds, (ExpenseItem)cmbExpenseDesc.getSelectedItem()))
			{
				btnEditExpense.setText("Edit");
				btnAddExpense.setEnabled(true);
				btnDeleteExpense.setEnabled(true);

				//Disable controls
				txtExpenseAmount.setEditable(false);
				chkHST.setEnabled(false);
				cmbExpenseDesc.setEditable(false);
			}
		}
	}

	/**Delete Expense action listener*/
	public void btnDeleteExpense_actionPerformed(ActionEvent ae)
	{
		ignoreEvents = true;
		try
		{
			if(DBMethods.deleteExpense(ds, (ExpenseItem)cmbExpenseDesc.getSelectedItem()))
			{
				btnDeleteExpense.setEnabled(false);
				txtExpenseAmount.setText("");
				chkHST.setSelected(false);
				cmbExpenseDesc.removeItemAt(cmbExpenseDesc.getSelectedIndex());

				resetExpenseControls();
			}
		}
		catch(Exception e)
		{
			cat.error(e.toString());
		}

		ignoreEvents = false;
	}
	
	/**Add work amount action listener*/
	public void btnAddWorkDayAmount_actionPerformed(ActionEvent ae)
	{
		ignoreEvents = true;
		
		try
		{
			// Get info for new work day amount
			Date dt = cmbWorkDayDate.getValue();
			String workDayHoursStr = txtWorkDayHours.getText();
			double workDayHours = Double.parseDouble(workDayHoursStr);
			
			// Make sure chosen date (not including time) is greater than or equal to last timelog end date.
			Date lastDate = DBMethods.getLastTimelogEndDate(ds);
			Calendar cal = Calendar.getInstance();
			cal.setTime(lastDate);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			Date lastDate0 = cal.getTime();
			if (dt.getTime() >= lastDate0.getTime())
			{
				// We need to know the start date time for the new record to insert. There may already be 
				// timelog records for the given date. If so, the start date needs to be at least one
				// second greater than the end date of the last record for the given date. If there
				// are no records, we default the start date time to 08:00:00.
				Date startDate = DBMethods.getNextTimelogStartDate(ds, dt);
				
				// Add workDayHours to the startDate to get the endDate.
				long numberOfWorkMillis = (long)(workDayHours * 3600 * 1000);
				Date endDate = new Date(startDate.getTime() + numberOfWorkMillis);
				
				boolean success = false;
				
				// If endDate goes past midnight, we need to add two records.
				Calendar cal1 = Calendar.getInstance();
				cal1.setTime(startDate);
				Calendar cal2 = Calendar.getInstance();
				cal2.setTime(endDate);
				if (cal1.get(Calendar.DAY_OF_YEAR) != cal2.get(Calendar.DAY_OF_YEAR))
				{
					cal1.set(Calendar.HOUR_OF_DAY, 23);
					cal1.set(Calendar.MINUTE, 59);
					cal1.set(Calendar.SECOND, 59);
					cal1.set(Calendar.MILLISECOND, 0);
					Date midnight = cal1.getTime();
					boolean insert1 = DBMethods.insertNewTimelogEntry(ds,
																	  cmbCompany.getSelectedItem().toString(),
																	  cmbTask.getSelectedItem().toString(),
																	  cmbComment.getSelectedItem().toString(),
																	  0,
																	  startDate,
																	  midnight);
					
					// Advance midnight by one second to get 00:00:00 of next day
					midnight.setTime(midnight.getTime() + 1000);
					
					// Now compute remaining work time by subtracting the difference between startDate and midnight
					// from workDayHours
					long remainingMillis = numberOfWorkMillis - (midnight.getTime() - startDate.getTime());
					endDate.setTime(midnight.getTime() + remainingMillis);
					boolean insert2 = DBMethods.insertNewTimelogEntry(ds,
																	  cmbCompany.getSelectedItem().toString(),
																	  cmbTask.getSelectedItem().toString(),
																	  cmbComment.getSelectedItem().toString(),
																	  0,
																	  midnight,
																	  endDate);
					
					success = insert1 && insert2;
				}
				else
				{
					success = DBMethods.insertNewTimelogEntry(ds,
															  cmbCompany.getSelectedItem().toString(),
															  cmbTask.getSelectedItem().toString(),
															  cmbComment.getSelectedItem().toString(),
															  0,
															  startDate,
															  endDate);
				}
				
				if (success)
				{
					// Notify user that record(s) were added
					jLabel21.setText(workDayHoursStr + " hours added.");
					
					// Clear message after 2 seconds
					Timer timer = new Timer(2000, new ActionListener() {
						  @Override
						  public void actionPerformed(ActionEvent arg0) {
							  jLabel21.setText("");
						  }
						});
						timer.setRepeats(false); // Only execute once
						timer.start(); // Go go go!
				}
			}
		}
		catch (ParseException e)
		{
		}
		catch (NumberFormatException e)
		{
		}

		ignoreEvents = false;
	}

	/**Generate Doc action listener*/
	public void btnGenDoc_actionPerformed(ActionEvent ae)
	{
		String docType = null;
		StringBuffer status = new StringBuffer("");

		DocGen docGenerator = null;
		switch(cmbDocType.getSelectedIndex())
		{
		case 0: //Invoice
			docType = "invoice";
			docGenerator = new InvoiceDocGen(this);
			break;
		case 1: //Timesheet
			docType = "timesheet";
			docGenerator = new TimesheetDocGen(this);
			break;
		}

		setCursor(new Cursor(Cursor.WAIT_CURSOR));
		boolean docGenerated = docGenerator.generateDoc(status);
		setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		
		if (!docGenerated)
		{
			//Problem generating document
			String statusStr = status.toString();

			JOptionPane.showMessageDialog(null, statusStr, "Document Generation", JOptionPane.INFORMATION_MESSAGE);
		}

		if(docType != null)
		{
			int companyID = DBMethods.getTableInt(ds, "companies", "id", "name='" + cmbCompany.getSelectedItem().toString() + "'");
			if(companyID != -1)
			{
				//Record path used to write document
//        DBMethods.setCompanyPath(ds, companyID, docType, txtDocPath.getText().replace('\\', '/'));
			}
		}
	}

	/**Choose Path action listener*/
	public void btnChoosePath_actionPerformed(ActionEvent ae)
	{
		JFileChooser chooser = new JFileChooser();
		String docType = null, openPath;

		switch(cmbDocType.getSelectedIndex())
		{
		case 0: //Time sheet
			docType = "timesheet";
			break;
		case 1: //Invoice
			docType = "invoice";
			break;
		}
		if(docType != null)
		{
			int companyID = DBMethods.getTableInt(ds, "companies", "id", "name='" + cmbCompany.getSelectedItem().toString() + "'");
			if(companyID != -1)
			{
				openPath = DBMethods.getCompanyPath(ds, companyID, docType);
				if(openPath != null && openPath.length() > 0)
				{
					int pos = openPath.lastIndexOf('/');
					if(pos != -1)
					{
						String dir = openPath.substring(0, pos);
						chooser.setCurrentDirectory(new File(dir));
						chooser.setSelectedFile(new File(openPath));
					}
				}
			}
		}
		chooser.setDialogTitle("Choose output directory");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int returnVal = chooser.showOpenDialog(this);
		if(returnVal == JFileChooser.APPROVE_OPTION)
			txtDocPath.setText(chooser.getSelectedFile().getAbsolutePath());
	}

	public void spnPunchInOffset_actionPerformed(ChangeEvent e)
	{
		UpdatePunchInOffset();
	}

	public void spnPunchOutOffset_actionPerformed(ChangeEvent e)
	{
		UpdatePunchOutOffset();
	}

	public void cmbTaxYear_actionPerformed(ActionEvent ae)
	{
		//A different tax year was selected.
		computeTaxes();
	}

	public void spnIncomeTaxPercent_actionPerformed(ChangeEvent e)
	{
		Integer val = (Integer)spnIncomeTaxPercent.getValue();
		long taxedIncome;

		incomeTaxRate = val.intValue();
		taxedIncome = income * incomeTaxRate / 100;

		txtPercent.setText(IntegralCurrency.penniesToCurrencyString(taxedIncome));
		txtTaxTotal.setText(IntegralCurrency.penniesToCurrencyString(hst + taxedIncome));
	}

	/**File | Exit action performed*/
	public void jMenuFileExit_actionPerformed(ActionEvent e) {
//    String newline = System.getProperty("line.separator");
		System.exit(0);
	}

	/**Help | About action performed*/
	public void jMenuHelpAbout_actionPerformed(ActionEvent e) {
		AboutDlg dlg = new AboutDlg(this, "About PunchClock", true);
		dlg.setVisible(true);
	}

	/**Overridden so we can exit when window is closed*/
	protected void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			jMenuFileExit_actionPerformed(null);
		}
	}

	/**
	 * This method initializes jCompanyTaskButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJCompanyTaskButton() {
		if (jCompanyTaskButton == null) {
			jCompanyTaskButton = new JButton();
			jCompanyTaskButton.setText("...");
			jCompanyTaskButton.setPreferredSize(new Dimension(21, 21));
			jCompanyTaskButton.setToolTipText("Manage Companies and Tasks");
			jCompanyTaskButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e){
					btnCompanyTask_actionPerformed(e);
				}
			});
		}
		return jCompanyTaskButton;
	}

	/**
	 * This method initializes txtCPPContribution	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getTxtCPPContribution() {
		if (txtCPPContribution == null) {
			txtCPPContribution = new JTextField();
			txtCPPContribution.setPreferredSize(new Dimension(90, 21));
			txtCPPContribution.setEditable(false);
		}
		return txtCPPContribution;
	}
}
