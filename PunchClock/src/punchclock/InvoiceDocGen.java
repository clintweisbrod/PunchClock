package punchclock;

import punchclock.db.*;

import java.awt.*;
import java.util.*;
import java.util.Date;
import java.text.*;
import java.io.*;
import java.sql.*;

import javax.swing.*;

import org.apache.log4j.*;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.events.FieldPositioningEvents;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2003
 * Company:
 * @author Clint Weisbrod
 * @version 1.0
 */

public class InvoiceDocGen extends DocGen
{
	class InvoiceItemData
	{
		public String code = null;
		public long milliSeconds;
		public long rate;
		public boolean applyHST;

		public InvoiceItemData(String code, long milliSeconds, long rate, boolean inApplyHST) {
			this.code = code;
			this.milliSeconds = milliSeconds;
			this.rate = rate;
			this.applyHST = inApplyHST;
		}
	}

	private static Category cat = Category.getInstance(InvoiceDocGen.class.getName());

	private final static String SQL_SELECT_COMPANY_TASKS = "SELECT id, name, code FROM tasks WHERE companyID = {0} ORDER BY name";
	
	private final static String SQL_SELECT_DAILY_TIMELOGS = "SELECT * FROM timelog WHERE companyID = {0} " +
	"AND taskID = {1} AND start >= ''{2}'' AND end < ''{3}'' ORDER BY start";

	private final static String SQL_SELECT_DAILY_TIMELOGS2 = "SELECT * FROM timelog WHERE companyID = {0} " +
	"AND taskID = {1} AND start >= ''{2}'' AND start < ''{3}'' AND comment = ''{4}'' ORDER BY start";
	
	private final static String SQL_SELECT_TIMELOGS = "SELECT * FROM timelog WHERE companyID = {0} " +
		"AND start >= ''{1}'' AND end < ''{2}'' ORDER BY start";
	
	private final static String SQL_SELECT_TIMELOG_STRADDLING_BOUNDARY = "SELECT * FROM timelog WHERE companyID = {0} " +
			"AND start < ''{1}'' AND end > ''{2}'' ORDER BY start";

	private final static String SQL_SELECT_TASK_RATE_AND_HST = "SELECT name, code, rate, applyHST FROM tasks WHERE id = {0}";

	private final static String SQL_SELECT_COMPANY_INFO = "SELECT * FROM companies WHERE id = {0}";

//	private final static String SQL_SELECT_INVOICE_NUMBER = "SELECT MAX(id) FROM invoices";
	private final static String SQL_SELECT_INVOICE_NUMBER = "SELECT id FROM invoices ORDER BY id DESC";

	private final static String SQL_SELECT_EXPENSES = "SELECT * FROM expenses WHERE " +
		"companyID = {0} AND date >= ''{1}'' AND date < ''{2}'' ORDER BY date";

	private final static String SQL_INSERT_INVOICE = "INSERT INTO invoices (id, companyID, periodStart, " +
		"periodEnd, date, total, hst) VALUES ({0}, {1}, ''{2}'', ''{3}'', ''{4}'', ''{5}'', ''{6}'')";

	private final static String SQL_GET_DECIMAL_PRECISION = "SELECT decimalPrecision FROM companies WHERE id =";
	
	private final static String INVOICE_NOTICE =
		"Time intervals displayed for each item in this invoice reflect rounded (and therefore " +
		"approximate) values. {0} maintains exact records of time expended for all billable activities listed " +
		"in this invoice. The billable amounts listed for each item are calculated by using these exact times, " +
		"not the displayed, rounded values.";
	
	private final static String TIMESHEET_NOTICE =
		"Time intervals displayed for each item in this timesheet reflect rounded (and therefore " +
		"approximate) values. The total time listed in this timesheet is not necessarily the exact sum of the displayed " +
		"itemized times. Doing so would introduce rounding error. {0} maintains exact records of time " +
		"expended for all billable activities listed in this timesheet. All timesheet totals and " +
		"invoices submitted by {0} are based upon these exact time records.";
	
	private long amountHST;
	private long amountNoHST;
	private long hstTotal;
	private long invoiceTotal;
	private Hashtable<String, InvoiceItemData> invoiceItems = new Hashtable<String, InvoiceItemData>(10);

	public InvoiceDocGen(AppFrame appFrame)
	{
		this.appFrame = appFrame;
	}

	public String getDocExtension()
	{
		return "pdf";
	}

	public String getBaseFilename()
	{
		String result = null;

		try
		{
			result = appFrame.props.getSafeProperty("invoice.baseFilename");
		}
		catch(IOException e)
		{
			cat.error(e.toString());
		}

		return result;
	}

	public boolean generateDoc(StringBuffer status)
	{
		boolean result = false;
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd");
		Connection conn = null;
		Statement stmt1 = null, stmt2 = null;
		ResultSet rs1 = null, rs2 = null;
		int companyID = DBMethods.getTableInt(AppFrame.ds, "companies", "id", "name='" + appFrame.cmbCompany.getSelectedItem().toString() + "'");
		int roundAmounts = DBMethods.getTableInt(AppFrame.ds, "companies", "roundToNearestHalfHour", "name='" + appFrame.cmbCompany.getSelectedItem().toString() + "'");
		int invoiceNumber = 1;
		int decimalPrecision;
		String sql;
		String billingContact = null, companyName = null, companyAddress = null;
		String companyCity = null, companyState = null, companyZIP = null, companyCurrency = null;
		java.util.Date dt1 = null;
		java.util.Date dt2 = null;

		try
		{
			conn = AppFrame.ds.getConnection();
			stmt1 = conn.createStatement();

			//Obtain the decimal precision to be used for this company
			decimalPrecision = Integer.parseInt(appFrame.props.getSafeProperty("defaultDecimalPrecision", "3"));
			rs1 = stmt1.executeQuery(SQL_GET_DECIMAL_PRECISION + companyID);
			if(rs1.first())
				decimalPrecision = rs1.getInt(1);

			//Retrieve last invoice inserted for invoice #
			rs1 = stmt1.executeQuery(SQL_SELECT_INVOICE_NUMBER);
			if(rs1.first())
				invoiceNumber = rs1.getInt(1) + 1;

			//Get company information from database
			Object[] insertArgs1 = {new Integer(companyID)};
			sql = MessageFormat.format(SQL_SELECT_COMPANY_INFO, insertArgs1);
			rs1 = stmt1.executeQuery(sql);
			if(rs1.first())
			{
				billingContact = rs1.getString("billingContact");
				companyName = rs1.getString("name");
				companyAddress = rs1.getString("address");
				companyCity = rs1.getString("city");
				companyState = rs1.getString("state");
				companyZIP = rs1.getString("zip");
				companyCurrency = rs1.getString("currency");
			}

			//Gather invoice items for work done in specified period
			invoiceItems.clear();
			
			//Construct query to return all timelog entries in selected date range
//			dt1 = appFrame.cmbStartDate.getValue();
//			dt2 = appFrame.cmbEndDate.getValue();
			dt1 = appFrame.dtRange.getStart();
			dt2 = appFrame.dtRange.getEnd();
			
			// Add one day to dt2
			Calendar cal = Calendar.getInstance();
			cal.setTime(dt2);
			int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
			dayOfYear++;
			cal.set(Calendar.DAY_OF_YEAR, dayOfYear);
			dt2 = cal.getTime();
			
			String day1, day2;
			day1 = DateTime.getDateString(dt1);
			day2 = DateTime.getDateString(dt2);
			Object[] insertArgs2 = {new Integer(companyID),
									new String(day1),
									new String(day2)};
			sql = MessageFormat.format(SQL_SELECT_TIMELOGS, insertArgs2);
			rs1 = stmt1.executeQuery(sql);
			if (rs1.first())
			{
				do
				{
					//Now obtain task record for this timelog entry to obtain rate and hst status
					rs2 = getTask(conn, rs1.getInt("taskID"));
					if(rs2.first())
					{
						// Get code for this task. May be NULL
						String code = rs2.getString("code");
						
						//Get rate for this task
						int rate = rs2.getInt("rate");
						boolean applyHST = (rs2.getInt("applyHST") == 1);

						long milliSeconds = DateTime.getDateDiff(rs1.getString("start"), rs1.getString("end"));
						
						addInvoiceItem(code, milliSeconds, rate, applyHST);
					}
				} while(rs1.next());
			}
			
			// The above block handled all the timelog entries entirely within the date range. We must also
			// handle timelog entries that straddle the date range interval boundaries.
			
			// timelog record straddling start of date range
			Object[] insertArgs3 = {new Integer(companyID),
									new String(day1),
									new String(day1)};
			sql = MessageFormat.format(SQL_SELECT_TIMELOG_STRADDLING_BOUNDARY, insertArgs3);
			rs1 = stmt1.executeQuery(sql);
			if (rs1.first())
			{
				rs2 = getTask(conn, rs1.getInt("taskID"));
				if (rs2.first())
				{
					// Get code for this task. May be NULL
					String code = rs2.getString("code");
					
					// Get rate for this task
					int rate = rs2.getInt("rate");
					boolean applyHST = (rs2.getInt("applyHST") == 1);
					
					// The end date will be within the date range
					Date endDate = DateTime.getDateFromDateString(rs1.getString("end"));
					
					// Set dt1 to beginning of dt2's day
					cal.setTime(endDate);
					cal.set(Calendar.HOUR_OF_DAY, 0);
					cal.set(Calendar.MINUTE, 0);
					cal.set(Calendar.SECOND, 0);
					cal.set(Calendar.MILLISECOND, 0);
					Date startDate = cal.getTime();

					long milliSeconds = endDate.getTime() - startDate.getTime();
					addInvoiceItem(code, milliSeconds, rate, applyHST);
				}
			}
			
			// timelog record straddling end of date range
			Object[] insertArgs4 = {new Integer(companyID),
									new String(day2),
									new String(day2)};
			sql = MessageFormat.format(SQL_SELECT_TIMELOG_STRADDLING_BOUNDARY, insertArgs4);
			rs1 = stmt1.executeQuery(sql);
			if (rs1.first())
			{
				rs2 = getTask(conn, rs1.getInt("taskID"));
				if (rs2.first())
				{
					// Get code for this task. May be NULL
					String code = rs2.getString("code");
					
					// Get rate for this task
					int rate = rs2.getInt("rate");
					boolean applyHST = (rs2.getInt("applyHST") == 1);
					
					// The start date will be within the date range
					Date startDate = DateTime.getDateFromDateString(rs1.getString("start"));
					
					// Set dt1 to beginning of dt2's day
					cal.setTime(startDate);
					cal.set(Calendar.HOUR_OF_DAY, 23);
					cal.set(Calendar.MINUTE, 59);
					cal.set(Calendar.SECOND, 59);
					cal.set(Calendar.MILLISECOND, 999);
					Date endDate = cal.getTime();

					long milliSeconds = endDate.getTime() - startDate.getTime();
					addInvoiceItem(code, milliSeconds, rate, applyHST);
				}
			}
			
			com.itextpdf.text.Font helvetica22Bold = FontFactory.getFont(FontFactory.HELVETICA, 22, com.itextpdf.text.Font.BOLD);
			com.itextpdf.text.Font helvetica12Bold = FontFactory.getFont(FontFactory.HELVETICA, 12, com.itextpdf.text.Font.BOLD);
			com.itextpdf.text.Font helvetica10Bold = FontFactory.getFont(FontFactory.HELVETICA, 10, com.itextpdf.text.Font.BOLD);
			com.itextpdf.text.Font helvetica8Normal = FontFactory.getFont(FontFactory.HELVETICA, 8, com.itextpdf.text.Font.NORMAL);

			//Begin PDF document generation
			Document document = new Document();
			generateDocFileName(appFrame.cmbEndDate.getValue());
			PdfWriter pdfWriter = PdfWriter.getInstance(document, new FileOutputStream(getDocFileFullPath()));

			//Add miscellaneous information to document
			String s = appFrame.props.getSafeProperty("company.name") + " - Invoice";
			document.addTitle(s);
			s = "Consulting fees for period: " + dateFormatter.format(appFrame.dtRange.getStart()) +
				" - " + dateFormatter.format(appFrame.dtRange.getEnd()); 
			document.addSubject(s);
			document.addCreator("PunchClock");
			document.addAuthor("Clint Weisbrod");
			document.open();

			PdfPTable table1 = new PdfPTable(2);
			float[] columnWidths1 = {75.0f, 25.0f};
			table1.setTotalWidth(100.0f);
			table1.setWidths(columnWidths1);
			table1.setWidthPercentage(100.0f);
			table1.setSpacingAfter(10.0f);

			PdfPCell cell;
			cell = new PdfPCell(new Phrase(appFrame.props.getSafeProperty("company.name"), helvetica22Bold));
			cell.setBorderWidth(0.0f);
			cell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
			table1.addCell(cell);

			DecimalFormat df2 = new DecimalFormat("00000");
			cell = new PdfPCell(new Phrase("Invoice #: " + df2.format(invoiceNumber), helvetica12Bold));
			cell.setBorderWidth(0.0f);
			cell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
			table1.addCell(cell);
			
			cell = new PdfPCell();
			cell.setBorderWidth(0.0f);
			table1.addCell(cell);

			cell = new PdfPCell(new Phrase("Date: " + dateFormatter.format(new java.util.Date())));
			cell.setBorderWidth(0.0f);
			cell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
			table1.addCell(cell);

			document.add(table1);

			document.add(new Paragraph(10.0f, appFrame.props.getSafeProperty("company.address")));
			document.add(new Paragraph(appFrame.props.getSafeProperty("company.city") + ", " +
																 appFrame.props.getSafeProperty("company.state") + ", " +
																 appFrame.props.getSafeProperty("company.zip")));
			document.add(new Paragraph(appFrame.props.getSafeProperty("company.country")));

			document.add(new Paragraph(30.0f, "GST Registration #: " + appFrame.props.getSafeProperty("company.registration"), helvetica12Bold));

			document.add(new Paragraph(30.0f, billingContact, helvetica12Bold));
			document.add(new Paragraph(companyName));
			document.add(new Paragraph(companyAddress));
			document.add(new Paragraph(companyCity + ", " + companyState + ", " + companyZIP));
			
			outputInvoiceItemsTable(document, companyID, roundAmounts, day1, day2, dateFormatter, decimalPrecision, companyCurrency, status);

			if (companyCurrency.equals("USD"))
				document.add(new Paragraph("Amount payable in USD."));
			else
				document.add(new Paragraph("Amount payable in CDN."));
			document.add(new Paragraph("Payment due upon receipt. Thank-you."));
			
			outputSignatures(document, pdfWriter, status);
			
			// Add invoice notice
//			document.add(new Paragraph("IMPORTANT NOTICE:", helvetica10Bold));
//			Object[] insertArgs8 = {new String(appFrame.props.getSafeProperty("company.name"))};			
//			document.add(new Paragraph(MessageFormat.format(INVOICE_NOTICE, insertArgs8), helvetica8Normal));
			
			document.newPage();
			
			outputTimesheet(document, status);

			document.close();
			result = true;
			
			//Document successfully generated. Ask user if they want to view it.
			int viewDoc = JOptionPane.showConfirmDialog(null,
				"The invoice was successfully generated.\nWould you like to view it?",
				"Document Generation", JOptionPane.YES_NO_OPTION);
			if (viewDoc == JOptionPane.YES_OPTION)
			{
				try {
					File workingDir = new File(getDocFilePath());
					Runtime.getRuntime().exec("cmd /C start " + getDocFileName(), null, workingDir);
				}
				catch(IOException e) {
					cat.error(e.toString());
				}
			}

			//Ask user if this is an "official" invoice. If so, write record to invoice table
			int officialDoc = JOptionPane.showConfirmDialog(null, "Is this an official invoice?", "Invoice Generation",
															JOptionPane.YES_NO_OPTION);
			if(officialDoc == JOptionPane.YES_OPTION)
			{
				//Attempt to mark generated file with read-only attribute
				File newFile = new File(getDocFileFullPath());
				try
				{
					newFile.setReadOnly();
				}
				catch(SecurityException e)
				{
					cat.error(e.toString());
				}

				//Insert new invoice record
				day1 = DateTime.getDateTimeString(dt1);
				dt2.setTime(dt2.getTime() - 1000);
				day2 = DateTime.getDateTimeString(dt2);
				Object[] insertArgs6 = {new Integer(invoiceNumber),
										new Integer(companyID),
										new String(day1),
										new String(day2),
										new String(DateTime.getCurrentDateString()),
										new String(Long.toString(invoiceTotal - hstTotal)),
										new String(Long.toString(hstTotal))};
				sql = MessageFormat.format(SQL_INSERT_INVOICE, insertArgs6);
				stmt1.executeUpdate(sql);
			}
		}
		catch(Exception e)
		{
			cat.error(e.toString());
			status.append("Exception occured. Invoice not created.");
		}
		finally
		{
			try
			{
				if (rs1 != null)
					rs1.close();
				if (rs2 != null)
					rs2.close();
				if (stmt1 != null)
					stmt1.close();
				if (stmt2 != null)
					stmt2.close();
				if (conn != null)
					conn.close();
			}
			catch (Exception e)
			{
			}
		}

		return result;
	}
	
	private void addInvoiceItem(String code, long milliSeconds, long rate, boolean applyHST)
	{
		//Attempt to locate invoiceItemData in Hashtable for this rate
		//Construct String representation of rate (and code if it exists) as key
		StringBuffer keyBuf = new StringBuffer(Long.toString(rate));
		if (code != null)
		{
			keyBuf.append(':');
			keyBuf.append(code);
		}
		String key = keyBuf.toString();
		
		InvoiceItemData invoiceItem = (InvoiceItemData)invoiceItems.get(key);
		if(invoiceItem == null)
		{
			//Create new InvoiceItemData object and insert in Hashtable
			invoiceItem = new InvoiceItemData(code, milliSeconds, rate, applyHST);
			invoiceItems.put(key, invoiceItem);
		}
		else
		{
			//Update item for this key
			invoiceItem.milliSeconds += milliSeconds;
			invoiceItems.put(key, invoiceItem);
		}
	}
	
	private ResultSet getTask(Connection conn, int taskID) throws SQLException
	{
		Object[] insertArgs3 = {new Integer(taskID)};
		String sql = MessageFormat.format(SQL_SELECT_TASK_RATE_AND_HST, insertArgs3);
		Statement stmt2 = conn.createStatement();
		return stmt2.executeQuery(sql);
	}
	
	private void outputInvoiceItemsTable(Document document, int companyID, int roundAmounts, String day1, String day2,
										SimpleDateFormat dateFormatter, int decimalPrecision, String companyCurrency, StringBuffer status)
	{
		Connection conn = null;
		Statement stmt1 = null, stmt2 = null;
		ResultSet rs1 = null, rs2 = null;
		String sql;
		
		amountHST = amountNoHST = hstTotal = invoiceTotal = 0;
		
		try
		{
			PdfPCell cell;
			PdfPTable table2 = new PdfPTable(4);
			float[] columnWidths2 = {15.0f, 60.0f, 18.0f, 7.0f};
			table2.setTotalWidth(100.0f);
			table2.setWidths(columnWidths2);
			table2.setWidthPercentage(100.0f);
			table2.setSpacingBefore(10.0f);
	
			cell = new PdfPCell(new Phrase("Date"));
			cell.setBorder(com.itextpdf.text.Rectangle.BOTTOM);
			table2.addCell(cell);
			cell = new PdfPCell(new Phrase("Description"));
			cell.setBorder(com.itextpdf.text.Rectangle.BOTTOM);
			table2.addCell(cell);
			cell = new PdfPCell(new Phrase("Amount"));
			cell.setBorder(com.itextpdf.text.Rectangle.BOTTOM);
			cell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
			table2.addCell(cell);
			cell = new PdfPCell(new Phrase("Tax"));
			cell.setBorder(com.itextpdf.text.Rectangle.BOTTOM);
			cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
			table2.addCell(cell);
	
			int row = 2;
	
			//Look for expenses in the date range specified
			conn = AppFrame.ds.getConnection();
			stmt1 = conn.createStatement();
			Object[] insertArgs5 = {new Integer(companyID),
									new String(day1),
									new String(day2)};
			sql = MessageFormat.format(SQL_SELECT_EXPENSES, insertArgs5);
			rs1 = stmt1.executeQuery(sql);
			if(rs1.first())
			{
				do
				{
					//Output expenses to document
					int hstApplicable = rs1.getInt("hstApplicable");
					long expenseAmount = rs1.getLong("amount");
	
					java.util.Date dt = DateTime.getDateFromDateString(rs1.getString("date"));
					cell = new PdfPCell(new Phrase(dateFormatter.format(dt)));
					cell.setBorder(com.itextpdf.text.Rectangle.LEFT | com.itextpdf.text.Rectangle.RIGHT);
					table2.addCell(cell);
	
					cell = new PdfPCell(new Phrase(rs1.getString("description")));
					cell.setBorder(com.itextpdf.text.Rectangle.LEFT | com.itextpdf.text.Rectangle.RIGHT);
					cell.setVerticalAlignment(PdfPCell.ALIGN_CENTER);
					table2.addCell(cell);
	
					cell = new PdfPCell(new Phrase(IntegralCurrency.penniesToCurrencyString(expenseAmount)));
					cell.setBorder(com.itextpdf.text.Rectangle.LEFT | com.itextpdf.text.Rectangle.RIGHT);
					cell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
					cell.setVerticalAlignment(PdfPCell.ALIGN_CENTER);
					table2.addCell(cell);
	
					cell = new PdfPCell(new Phrase((hstApplicable == 1)? "H" : ""));
					cell.setBorder(com.itextpdf.text.Rectangle.LEFT | com.itextpdf.text.Rectangle.RIGHT);
					cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
					cell.setVerticalAlignment(PdfPCell.ALIGN_CENTER);
					table2.addCell(cell);
	
					if(hstApplicable != 0)
						amountHST += expenseAmount;
					else
						amountNoHST += expenseAmount;
	
					row++;
				} while(rs1.next());
			}
	
			//Now output any work amounts to the document
			if(!invoiceItems.isEmpty())
			{
				cell = new PdfPCell(new Phrase(dateFormatter.format(appFrame.cmbEndDate.getValue())));
				cell.setBorder(com.itextpdf.text.Rectangle.LEFT | com.itextpdf.text.Rectangle.RIGHT);
				cell.setVerticalAlignment(PdfPCell.ALIGN_CENTER);
				table2.addCell(cell);
				cell = new PdfPCell(new Phrase("Consulting fees for period: " + dateFormatter.format(appFrame.dtRange.getStart()) +
												" - " + dateFormatter.format(appFrame.dtRange.getEnd())));
				cell.setBorder(com.itextpdf.text.Rectangle.LEFT | com.itextpdf.text.Rectangle.RIGHT);
				cell.setVerticalAlignment(PdfPCell.ALIGN_CENTER);
				table2.addCell(cell);
	
				cell = new PdfPCell();
				cell.setBorder(com.itextpdf.text.Rectangle.LEFT | com.itextpdf.text.Rectangle.RIGHT);
				table2.addCell(cell);
				cell.setBorder(com.itextpdf.text.Rectangle.LEFT | com.itextpdf.text.Rectangle.RIGHT);
				table2.addCell(cell);
	
				//Output itemized list of tasks with different rates
				Enumeration<InvoiceItemData> theEnum = invoiceItems.elements();
				String strData;
				long workAmount;
				InvoiceItemData invoiceItem;
				while(theEnum.hasMoreElements())
				{
					invoiceItem = (InvoiceItemData)theEnum.nextElement();
					
					// Some companies prefer time values rounded to nearest half hour
					double hours = 0;
					if (roundAmounts != 0)
					{
						// Round invoiceItem.milliSeconds to nearest half hour
						hours = (double)getRoundedMilliseconds(invoiceItem.milliSeconds) / 3600000.0;
					}
					else
					{
						// Compute exact work amount
						hours = (double)invoiceItem.milliSeconds / 3600000.0;
					}
					workAmount = (long)(hours * invoiceItem.rate);
					
					//Round total hours to n decimal places for each rate
					hours = IntegralCurrency.roundToDecimal(hours, decimalPrecision);
					//workAmount = (long)(Math.ceil(hours * invoiceItem.rate));
					if (invoiceItem.applyHST)
						amountHST += workAmount;
					else
						amountNoHST += workAmount;
	
					int i;
					Object[] insertArgs4 = {new Double(hours),
											new String(IntegralCurrency.penniesToCurrencyString(invoiceItem.rate))};
					StringBuffer zeros = new StringBuffer();
					for(i=0; i<decimalPrecision; i++)
						zeros.append('0');
					StringBuffer itemDescBuf = new StringBuffer();
					if (invoiceItem.code != null)
					{
						itemDescBuf.append(invoiceItem.code);
						itemDescBuf.append(": ");
					}
					strData = MessageFormat.format("{0,number,0." + zeros.toString() + "} hours @ {1} / hr", insertArgs4);
					itemDescBuf.append(strData);
	
					cell = new PdfPCell();
					cell.setBorder(com.itextpdf.text.Rectangle.LEFT | com.itextpdf.text.Rectangle.RIGHT);
					table2.addCell(cell);
	
					cell = new PdfPCell(new Phrase(itemDescBuf.toString()));
					cell.setBorder(com.itextpdf.text.Rectangle.LEFT | com.itextpdf.text.Rectangle.RIGHT);
					cell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
					cell.setVerticalAlignment(PdfPCell.ALIGN_CENTER);
					table2.addCell(cell);
	
					cell = new PdfPCell(new Phrase(IntegralCurrency.penniesToCurrencyString(workAmount)));
					cell.setBorder(com.itextpdf.text.Rectangle.LEFT | com.itextpdf.text.Rectangle.RIGHT);
					cell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
					cell.setVerticalAlignment(PdfPCell.ALIGN_CENTER);
					table2.addCell(cell);
	
					cell = new PdfPCell(new Phrase(invoiceItem.applyHST?"H":""));
					cell.setBorder(com.itextpdf.text.Rectangle.LEFT | com.itextpdf.text.Rectangle.RIGHT);
					cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
					cell.setVerticalAlignment(PdfPCell.ALIGN_CENTER);
					table2.addCell(cell);
	
					row++;
				}
			}
	
			//Compute HST amount
			hstTotal = Math.round((double)amountHST * Double.parseDouble(appFrame.props.getSafeProperty("hstRate")) / 100);
	
			//Compute invoice total
			invoiceTotal = amountHST + amountNoHST + hstTotal;
			
			if (!companyCurrency.equals("USD"))
			{
				cell = new PdfPCell();
				cell.setBorder(com.itextpdf.text.Rectangle.LEFT | com.itextpdf.text.Rectangle.RIGHT);
				table2.addCell(cell);
				
				cell = new PdfPCell(new Phrase("GST/HST:"));
				cell.setBorder(com.itextpdf.text.Rectangle.LEFT | com.itextpdf.text.Rectangle.RIGHT);
				cell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
				table2.addCell(cell);
				
				cell = new PdfPCell(new Phrase(IntegralCurrency.penniesToCurrencyString(hstTotal)));
				cell.setBorder(com.itextpdf.text.Rectangle.LEFT | com.itextpdf.text.Rectangle.RIGHT);
				cell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
				table2.addCell(cell);
				
				cell = new PdfPCell();
				cell.setBorder(com.itextpdf.text.Rectangle.LEFT | com.itextpdf.text.Rectangle.RIGHT);
				table2.addCell(cell);
			}
			
			cell = new PdfPCell(new Paragraph("Total Due",
					FontFactory.getFont(FontFactory.HELVETICA, 12, com.itextpdf.text.Font.BOLD)));
			cell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
			cell.setColspan(2);
			table2.addCell(cell);
			cell = new PdfPCell(new Paragraph(IntegralCurrency.penniesToCurrencyString(invoiceTotal),
							FontFactory.getFont(FontFactory.HELVETICA, 12, com.itextpdf.text.Font.BOLD)));
			cell.setBorder(com.itextpdf.text.Rectangle.LEFT | com.itextpdf.text.Rectangle.TOP | com.itextpdf.text.Rectangle.BOTTOM);
			cell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
			table2.addCell(cell);
			
			cell = new PdfPCell();
			cell.setBorder(com.itextpdf.text.Rectangle.TOP | com.itextpdf.text.Rectangle.RIGHT | com.itextpdf.text.Rectangle.BOTTOM);
			table2.addCell(cell);
	
			document.add(table2);
		}
		catch(Exception e)
		{
			cat.error(e.toString());
			status.append("Exception occured. Invoice not created.");
		}
		finally
		{
			try
			{
				if (rs1 != null)
					rs1.close();
				if (rs2 != null)
					rs2.close();
				if (stmt1 != null)
					stmt1.close();
				if (stmt2 != null)
					stmt2.close();
				if (conn != null)
					conn.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	public long getRoundedMilliseconds(long inMilliseconds)
	{
		final long kMillisecondsInHalfHour = 30 * 60 * 1000; // 30 minutes * 60 seconds/minute * 1000 milliseconds/second
		final long kMillisecondsInHour = 2 * kMillisecondsInHalfHour;
		
		long wholeHours = inMilliseconds / kMillisecondsInHour;
		long wholeHourMilliseconds = wholeHours * kMillisecondsInHour;
		long remainingMilliseconds = inMilliseconds - wholeHourMilliseconds;
		long roundedMilliseconds = Math.round((double)remainingMilliseconds / (double)kMillisecondsInHalfHour) * kMillisecondsInHalfHour;
		
		return wholeHourMilliseconds + roundedMilliseconds;
	}
	
	private void outputSignatures(Document document, PdfWriter pdfWriter, StringBuffer status)
	{
		try
		{
			PdfPCell cell;
			PdfPTable table4 = new PdfPTable(3);
			float[] columnWidths4 = {35.0f, 30.0f, 35.0f};
			table4.setTotalWidth(100.0f);
			table4.setWidths(columnWidths4);
			table4.setWidthPercentage(100.0f);
			table4.setSpacingBefore(10.0f);
			table4.setSpacingAfter(10.0f);
			
			File imageFile = new File("Signature.png");
			Image image = Image.getInstance(imageFile.toURI().toURL());
			image.scalePercent(25.0f);
			cell = new PdfPCell(image);
			cell.setVerticalAlignment(PdfPCell.ALIGN_BOTTOM);
			cell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
			cell.setPadding(5.0f);
			table4.addCell(cell);
			
			cell = new PdfPCell();
			cell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
			table4.addCell(cell);
			
			cell = new PdfPCell();
			cell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
			PdfFormField sig = PdfFormField.createSignature(pdfWriter);
	//		sig.setWidget(new com.itextpdf.text.Rectangle(100, 100, 200, 200), PdfName.SIG);
			sig.setWidget(new com.itextpdf.text.Rectangle(100, 100, 200, 200), null);
			sig.setFlags(PdfAnnotation.FLAGS_PRINT);
			sig.put(PdfName.DA, new PdfString("/Helv 0 Tf 0 g"));
			sig.setFieldName("Signature1");
			cell.setCellEvent(new FieldPositioningEvents(pdfWriter, sig));
			
			table4.addCell(cell);
			
			cell = new PdfPCell(new Phrase("Contractor's Signature"));
			cell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
			cell.setBorder(com.itextpdf.text.Rectangle.TOP);
			table4.addCell(cell);
			
			cell = new PdfPCell();
			cell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
			table4.addCell(cell);
			
			cell = new PdfPCell(new Phrase("Manager's Signature"));
			cell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
			cell.setBorder(com.itextpdf.text.Rectangle.TOP);
			table4.addCell(cell);
			
			document.add(table4);
		}
		catch(Exception e)
		{
			cat.error(e.toString());
			status.append("Exception occured. Invoice not created.");
		}
	}
	
	private void outputTimesheet(Document document, StringBuffer status)
	{
		Connection conn = null;
		Statement stmt, taskStmt = null, sessionStmt = null, itemStmt = null;
		ResultSet rs, taskRS = null, sessionRS = null, itemRS = null;
		int companyID = DBMethods.getTableInt(AppFrame.ds, "companies", "id", "name='" + appFrame.cmbCompany.getSelectedItem().toString() + "'");
		int decimalPrecision;
		long totalTime = 0;
		java.util.Date dt1 = null;
		java.util.Date dt2 = null;
		double itemHours;

		try
		{
			PdfPCell cell;
			PdfPTable table5 = new PdfPTable(4);
			float[] columnWidths5 = {15.0f, 25.0f, 50.0f, 10.0f};
			table5.setTotalWidth(100.0f);
			table5.setWidths(columnWidths5);
			table5.setWidthPercentage(100.0f);
			table5.setHeaderRows(1);
			table5.setSpacingAfter(10.0f);
			
			com.itextpdf.text.Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA, 12, com.itextpdf.text.Font.BOLD);
			com.itextpdf.text.Font tableFont = FontFactory.getFont(FontFactory.HELVETICA, 8, com.itextpdf.text.Font.NORMAL);
			com.itextpdf.text.Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 10, com.itextpdf.text.Font.BOLD);
			
			cell = new PdfPCell(new Phrase("Date", tableHeaderFont));
			table5.addCell(cell);
			cell = new PdfPCell(new Phrase("Task", tableHeaderFont));
			table5.addCell(cell);
			cell = new PdfPCell(new Phrase("Description", tableHeaderFont));
			table5.addCell(cell);
			cell = new PdfPCell(new Phrase("Hours", tableHeaderFont));
			cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
			table5.addCell(cell);
			
			conn = AppFrame.ds.getConnection();

			//Obtain the decimal precision to be used for this company
			decimalPrecision = Integer.parseInt(appFrame.props.getSafeProperty("defaultDecimalPrecision", "3"));
			stmt = conn.createStatement();
			rs = stmt.executeQuery(SQL_GET_DECIMAL_PRECISION + companyID);
			if(rs.first())
				decimalPrecision = rs.getInt(1);
			
			Calendar cal = Calendar.getInstance();
//			dt1 = appFrame.cmbStartDate.getValue();
//			dt2 = appFrame.cmbEndDate.getValue();
			dt1 = appFrame.dtRange.getStart();
			dt2 = appFrame.dtRange.getEnd();
			String day1, day2;
			
			// Look for a timelog record straddling beginning of date range
			day1 = DateTime.getDateString(dt1);
			Object[] insertArgs4 = {new Integer(companyID),
					new String(day1),
					new String(day1)};
			String sql = MessageFormat.format(SQL_SELECT_TIMELOG_STRADDLING_BOUNDARY, insertArgs4);
			ResultSet rs1 = stmt.executeQuery(sql);
			if (rs1.first())
			{
				ResultSet taskRS1 = getTask(conn, rs1.getInt("taskID"));
				if (taskRS1.first())
				{
					SimpleDateFormat df = new SimpleDateFormat("EEE, MMM d, yyyy");
					StringBuffer dtBuf = new StringBuffer();
					df.format(dt1, dtBuf, new FieldPosition(0));
					
					// Compute milliseconds within the date range
					// The end date will be within the date range
					Date endDate = DateTime.getDateFromDateString(rs1.getString("end"));
					
					// Set dt1 to beginning of dt2's day
					cal.setTime(endDate);
					cal.set(Calendar.HOUR_OF_DAY, 0);
					cal.set(Calendar.MINUTE, 0);
					cal.set(Calendar.SECOND, 0);
					cal.set(Calendar.MILLISECOND, 0);
					Date startDate = cal.getTime();
	
					long milliSeconds = endDate.getTime() - startDate.getTime();
					milliSeconds = getRoundedMilliseconds(milliSeconds);
					totalTime += milliSeconds;
					itemHours = millisecondsToRoundedHours(milliSeconds, decimalPrecision);
					addTimesheetRow(table5, tableFont, dtBuf.toString(), taskRS1.getString("name"), rs1.getString("comment"), IntegralCurrency.doubleToString(itemHours, decimalPrecision));
				}
			}

			//Obtain resultset for all selected company tasks
			taskStmt = conn.createStatement();
			Object[] insertArgs1 = {new Integer(companyID)};
			String taskSQL = MessageFormat.format(SQL_SELECT_COMPANY_TASKS, insertArgs1);
			taskRS = taskStmt.executeQuery(taskSQL);

			//Perform queries for each day in specified period
			do
			{
				day1 = DateTime.getDateString(dt1);
				
				// Generate a Date that is one day ahead of dt1
				cal.setTime(dt1);
				int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
				dayOfYear++;
				cal.set(Calendar.DAY_OF_YEAR, dayOfYear);
				
				day2 = DateTime.getDateString(cal.getTime());

				//Perform query to return all timelog items that occured on day1 for each company task
				boolean firstRow = true;
				if(taskRS.first())
				{
					sessionStmt = conn.createStatement();
					do
					{
						Object[] insertArgs2 = {new Integer(companyID),
												new Integer(taskRS.getInt("id")),
												new String(day1),
												new String(day2)};
						String sessionSQL = MessageFormat.format(SQL_SELECT_DAILY_TIMELOGS, insertArgs2);
						sessionRS = sessionStmt.executeQuery(sessionSQL);

						//Perform refinement query to collect timelog entries with identical comments
						if(sessionRS.first())
						{
							itemStmt = conn.createStatement();
							Vector<String> comments = new Vector<String>();
							do
							{
								//Skip null comments. Should only be on null comment in timelog if punched-in.
								String comment = sessionRS.getString("comment");
								if(comment == null)
									continue;

								//Skip refinement query for comments already processed
								if(comments.indexOf(comment) == -1)
								{
									Object[] insertArgs3 = {new Integer(companyID),
															new Integer(taskRS.getInt("id")),
															new String(day1),
															new String(day2),
															new String(comment)};
									String itemSQL = MessageFormat.format(SQL_SELECT_DAILY_TIMELOGS2, insertArgs3);
									itemRS = itemStmt.executeQuery(itemSQL);

									//Now iterate through itemRS, computing total time for this task/comment
									long itemTime = 0;
									if(itemRS.first())
									{
										do
										{
											itemTime += DateTime.getDateDiff(itemRS.getString("start"), itemRS.getString("end"));
										} while (itemRS.next());
									}
									totalTime += itemTime;

									//Add comment to vector to avoid multiple listing
									comments.add(comment);

									//Output HTML for this activity
									itemHours = millisecondsToRoundedHours(itemTime, decimalPrecision);
									if(firstRow)
									{
										SimpleDateFormat df = new SimpleDateFormat("EEE, MMM d, yyyy");
										StringBuffer dtBuf = new StringBuffer();
										df.format(dt1, dtBuf, new FieldPosition(0));
										addTimesheetRow(table5, tableFont, dtBuf.toString(), taskRS.getString("name"), comment, IntegralCurrency.doubleToString(itemHours, decimalPrecision));
									}
									else
									{
										addTimesheetRow(table5, tableFont, new String(), taskRS.getString("name"), comment, IntegralCurrency.doubleToString(itemHours, decimalPrecision));
									}
									firstRow = false;
								}
							} while (sessionRS.next());
						}
					} while (taskRS.next());
				}

				//Append blank row if we wrote data for this day
				if(!firstRow)
					table5.completeRow();

				// Increment dt1 by one day
				cal.setTime(dt1);
				dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
				dayOfYear++;
				cal.set(Calendar.DAY_OF_YEAR, dayOfYear);
				dt1 = cal.getTime();

			} while (!dt1.after(dt2));
			
			// Look for a timelog record straddling end of date range
			dt1 = appFrame.cmbEndDate.getValue();
			cal.setTime(dt1);
			int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
			dayOfYear++;
			cal.set(Calendar.DAY_OF_YEAR, dayOfYear);
			dt2 = cal.getTime();
			day1 = DateTime.getDateString(dt2);
			Object[] insertArgs5 = {new Integer(companyID),
					new String(day1),
					new String(day1)};
			sql = MessageFormat.format(SQL_SELECT_TIMELOG_STRADDLING_BOUNDARY, insertArgs5);
			rs1 = stmt.executeQuery(sql);
			if (rs1.first())
			{
				ResultSet taskRS1 = getTask(conn, rs1.getInt("taskID"));
				if (taskRS1.first())
				{
					SimpleDateFormat df = new SimpleDateFormat("EEE, MMM d, yyyy");
					StringBuffer dtBuf = new StringBuffer();
					df.format(dt1, dtBuf, new FieldPosition(0));
					
					// Compute milliseconds within the date range
					// The start date will be within the date range
					Date startDate = DateTime.getDateFromDateString(rs1.getString("start"));
					
					// Set dt1 to beginning of dt2's day
					cal.setTime(startDate);
					cal.set(Calendar.HOUR_OF_DAY, 23);
					cal.set(Calendar.MINUTE, 59);
					cal.set(Calendar.SECOND, 59);
					cal.set(Calendar.MILLISECOND, 999);
					Date endDate = cal.getTime();
	
					long milliSeconds = endDate.getTime() - startDate.getTime();
					totalTime += milliSeconds;
					itemHours = millisecondsToRoundedHours(milliSeconds, decimalPrecision);
					addTimesheetRow(table5, tableFont, dtBuf.toString(), taskRS1.getString("name"), rs1.getString("comment"), IntegralCurrency.doubleToString(itemHours, decimalPrecision));
				}
			}
			
			// Add total
			cell = new PdfPCell(new Phrase("Total", footerFont));
			cell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
			cell.setColspan(3);
			table5.addCell(cell);
			cell = new PdfPCell(new Phrase(new String(IntegralCurrency.doubleToString(millisecondsToRoundedHours(totalTime, decimalPrecision), decimalPrecision)),
											footerFont));
			cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
			table5.addCell(cell);
			
			document.add(table5);
			
//			document.add(new Paragraph("IMPORTANT NOTICE:", footerFont));
			
//			Object[] insertArgs8 = {new String(appFrame.props.getSafeProperty("company.name"))};			
//			document.add(new Paragraph(MessageFormat.format(TIMESHEET_NOTICE, insertArgs8), tableFont));
		}
		catch(Exception e)
		{
			cat.error(e.toString());
			status.append("Exception occured. Timesheet not created.");
		}
		finally
		{
			try
			{
				if (taskRS != null)
					taskRS.close();
				if (sessionRS != null)
					sessionRS.close();
				if (itemRS != null)
					itemRS.close();
				if (taskStmt != null)
					taskStmt.close();
				if (sessionStmt != null)
					sessionStmt.close();
				if (itemStmt != null)
					itemStmt.close();
				if (conn != null)
					conn.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	private void addTimesheetRow(PdfPTable table, com.itextpdf.text.Font tableFont, String date, String focus, String desc, String hours)
	{
		PdfPCell cell;
		cell = new PdfPCell(new Phrase(date, tableFont));
		table.addCell(cell);
		cell = new PdfPCell(new Phrase(focus, tableFont));
		table.addCell(cell);
		cell = new PdfPCell(new Phrase(desc, tableFont));
		table.addCell(cell);
		cell = new PdfPCell(new Phrase(hours, tableFont));
		cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
		table.addCell(cell);
	}
}