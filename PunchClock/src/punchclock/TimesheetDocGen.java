package punchclock;

import punchclock.db.*;

import java.text.*;
import java.util.*;
import java.io.*;
import java.sql.*;
import javax.swing.*;
import org.apache.log4j.*;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2003
 * Company:
 * @author Clint Weisbrod
 * @version 1.0
 */

public class TimesheetDocGen extends DocGen
{
	private static Category cat = Category.getInstance(TimesheetDocGen.class.getName());

	private final static String SQL_SELECT_COMPANY_TASKS = "SELECT id, name, code FROM tasks WHERE companyID = {0} ORDER BY name";

	private final static String SQL_SELECT_DAILY_TIMELOGS = "SELECT * FROM timelog WHERE companyID = {0} " +
		"AND taskID = {1} AND start >= ''{2}'' AND start < ''{3}'' ORDER BY start";

	private final static String SQL_SELECT_DAILY_TIMELOGS2 = "SELECT * FROM timelog WHERE companyID = {0} " +
		"AND taskID = {1} AND start >= ''{2}'' AND start < ''{3}'' AND comment = ''{4}'' ORDER BY start";

	private final static String SQL_GET_DECIMAL_PRECISION = "SELECT decimalPrecision FROM companies WHERE id =";

	private final static String HTML_TIMESHEET_TITLE =
		"<html>" +
		"<head>" +
		"<title></title>" +
		"<style>" +
		"body         '{' font-family: Verdana; font-size: 10pt '}'" +
		"table        '{' font-family: Verdana; font-size: 10pt '}'" +
		"</style>" +
		"</head>" +
		"<body>" +
		"<table border=''0'' width=''100%''>" +
		"  <tr>" +
		"    <td align=''left'' width=''65%'' style=''font-weight: bold; font-size: 16pt''>{0}</td>" +
		"    <td align=''right'' width=''35%'' style=''font-weight: bold; font-size: 14pt''>Daily Timesheet</td>" +
		"  </tr>" +
		"</table>";

	private final static String HTML_TIMESHEET_HEADER =
		"<table border=1 rules=''all'' cellspacing=''0'' width=''100%''>" +
		"  <tr>" +
		"    <td align=''left'' width=''25%'' style=''font-weight: bold''>Company</td>" +
		"    <td align=''left'' width=''75%''>{0}</td>" +
		"  </tr>" +
		"  <tr>" +
		"    <td align=''left'' width=''25%'' style=''font-weight: bold''>Date From</td>" +
		"    <td align=''left'' width=''75%''>{1,date,long}</td>" +
		"  </tr>" +
		"  <tr>" +
		"    <td align=''left'' width=''25%'' style=''font-weight: bold''>Date To</td>" +
		"    <td align=''left'' width=''75%''>{2,date,long}</td>" +
		"  </tr>" +
		"</table>" +
		"<br>";

	private final static String HTML_TIMESHEET_BODY_HEADER =
		"<table border=1 rules='all' cellspacing='0' width='100%'>" +
		"  <tr style='height: 35px; background-color: #A4CDF4; font-weight: bold'>" +
		"    <td align='left' width='25%'>Date</td>" +
		"    <td align='left' width='15%'>Focus</td>" +
		"    <td align='left' width='50%'>Description</td>" +
		"    <td align='center' width='10%'>Hours</td>" +
		"  </tr>";
		
	private final static String HTML_TIMESHEET_ITEM_DATA1 =
		"  <tr>" +
		"    <td>{0}</td>" +
		"    <td>{1}</td>" +
		"    <td>{2}</td>" +
		"    <td align=''center''>{3}</td>" +
		"  </tr>";

	private final static String HTML_TIMESHEET_ITEM_DATA2 =
		"  <tr>" +
		"    <td>&nbsp;</td>" +
		"    <td>{0}</td>" +
		"    <td>{1}</td>" +
		"    <td align=''center''>{2}</td>" +
		"  </tr>";

	private final static String HTML_TIMESHEET_BLANK_TABLE_ROW =
		"<tr style='height: 5px'><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr>";

	private final static String HTML_TIMESHEET_FOOTER =
		"  <tr style=''height: 35px; font-weight: bold; background-color: #B3F4B0; padding-top: 10px''>" +
		"    <td colspan=''3'' align=''right''>Total</td>" +
		"    <td align=''center''>{0}</td>" +
		"  </tr>" +
		"</table>";

	private final static String HTML_TIMESHEET_SIGNATURES =
		"<br>" +
		"<br>" +
		"<table border=0>" +
		"  <tr>" +
		"    <td align='left' width='50%'>________________________________</td>" +
		"    <td width=30>&nbsp;</td>" +
		"    <td align='right' width='50%'>________________________________</td>" +
		"  </tr>" +
		"  <tr>" +
		"    <td align='left' >Contractor's Signature</td>" +
		"    <td>&nbsp;</td>" +
		"    <td align='right'>Manager's Signature</td>" +
		"  </tr>" +
		"</table>";

	private final static String HTML_TIMESHEET_NOTICE =
		"<p style=''font-size: 8pt''>" +
		"<b>IMPORTANT NOTICE</b>:<br>" +
		"Time intervals displayed for each task on this timesheet reflect rounded (and therefore " +
		"approximate) values. The total time listed on this timesheet is not necessarily the exact sum of the displayed " +
		"itemized times. Doing so would introduce rounding error. {0} maintains exact records of time " +
		"expended for all billable activities listed on this timesheet. All timesheet totals and " +
		"invoices submitted by {0} are based upon these exact time records." +
		"</p>" +
		"</body>" +
		"</html>";

	public TimesheetDocGen(AppFrame appFrame)
	{
		this.appFrame = appFrame;
	}

	public String getDocExtension()
	{
		return "html";
	}

	public String getBaseFilename()
	{
		String result = null;

		try
		{
			result = appFrame.props.getSafeProperty("timesheet.baseFilename");
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
		Connection conn = null;
		Statement stmt, taskStmt = null, sessionStmt = null, itemStmt = null;
		ResultSet rs, taskRS = null, sessionRS = null, itemRS = null;
		int companyID = DBMethods.getTableInt(AppFrame.ds, "companies", "id", "name='" + appFrame.cmbCompany.getSelectedItem().toString() + "'");
		int decimalPrecision;
		long totalTime = 0;
		StringBuffer bodyBuffer = new StringBuffer();
		java.util.Date dt1 = null;
		java.util.Date dt2 = null;
		double itemHours;

		try
		{
			conn = AppFrame.ds.getConnection();

			//Obtain the decimal precision to be used for this company
			decimalPrecision = Integer.parseInt(appFrame.props.getSafeProperty("defaultDecimalPrecision", "3"));
			stmt = conn.createStatement();
			rs = stmt.executeQuery(SQL_GET_DECIMAL_PRECISION + companyID);
			if(rs.first())
				decimalPrecision = rs.getInt(1);

			//Obtain resultset for all selected company tasks
			taskStmt = conn.createStatement();
			Object[] insertArgs1 = {new Integer(companyID)};
			String taskSQL = MessageFormat.format(SQL_SELECT_COMPANY_TASKS, insertArgs1);
			taskRS = taskStmt.executeQuery(taskSQL);

			//Perform queries for each day in specified period
			Calendar cal = Calendar.getInstance();
			dt1 = appFrame.cmbStartDate.getValue();
			dt2 = appFrame.cmbEndDate.getValue();
			String day1, day2;
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
										Object[] insertArgs4 = {new String(dtBuf.toString()),
																new String(taskRS.getString("name")),
																new String(comment),
																new String(IntegralCurrency.doubleToString(itemHours, decimalPrecision))};
										bodyBuffer.append(MessageFormat.format(HTML_TIMESHEET_ITEM_DATA1, insertArgs4));
									}
									else
									{
										Object[] insertArgs4 = {new String(taskRS.getString("name")),
																new String(comment),
																new String(IntegralCurrency.doubleToString(itemHours, decimalPrecision))};
										bodyBuffer.append(MessageFormat.format(HTML_TIMESHEET_ITEM_DATA2, insertArgs4));
									}
									firstRow = false;
								}
							} while (sessionRS.next());
						}
					} while (taskRS.next());
				}

				//Append blank row if we wrote data for this day
				if(!firstRow)
					bodyBuffer.append(HTML_TIMESHEET_BLANK_TABLE_ROW);

				// Increment dt1 by one day
				cal.setTime(dt1);
				dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
				dayOfYear++;
				cal.set(Calendar.DAY_OF_YEAR, dayOfYear);
				dt1 = cal.getTime();

			} while (!dt1.after(dt2));

			//Strip final table row from bodyBuffer
			if(bodyBuffer.length() > HTML_TIMESHEET_BLANK_TABLE_ROW.length())
			{
				bodyBuffer.setLength(bodyBuffer.length() - HTML_TIMESHEET_BLANK_TABLE_ROW.length());

				//Write HTML to specified file
				generateDocFileName(dt2);
				BufferedWriter out = new BufferedWriter(new FileWriter(getDocFileFullPath()));

				String html;
				Object[] insertArgs5 = {new String(appFrame.props.getSafeProperty("company.name"))};
				html = MessageFormat.format(HTML_TIMESHEET_TITLE, insertArgs5);
				out.write(html);
				out.newLine();

				Object[] insertArgs6 = {new String(appFrame.cmbCompany.getSelectedItem().toString()),
										new java.util.Date(appFrame.cmbStartDate.getValue().getTime()),
										new java.util.Date(appFrame.cmbEndDate.getValue().getTime())};
				html = MessageFormat.format(HTML_TIMESHEET_HEADER, insertArgs6);
				out.write(html);
				out.newLine();

				out.write(HTML_TIMESHEET_BODY_HEADER);
				out.newLine();
				out.write(bodyBuffer.toString());
				out.newLine();

				Object[] insertArgs7 = {new String(IntegralCurrency.doubleToString(millisecondsToRoundedHours(totalTime, decimalPrecision), decimalPrecision))};
				html = MessageFormat.format(HTML_TIMESHEET_FOOTER, insertArgs7);
				out.write(html);
				out.newLine();

//				out.write(HTML_TIMESHEET_SIGNATURES);
//				out.newLine();

				Object[] insertArgs8 = {new String(appFrame.props.getSafeProperty("company.name"))};
				html = MessageFormat.format(HTML_TIMESHEET_NOTICE, insertArgs8);
				out.write(html);
				out.newLine();

				out.close();

				//Ask user if this is an "official" timesheet. If so, mark file read-only
				int officialDoc = JOptionPane.showConfirmDialog(null, "Is this an official timesheet?", "Timesheet Generation",
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
				}

				result = true;
			}
			else
			{
				status.append("Specified date range has no activity. Timesheet not created.");
			}
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

		return result;
	}
}