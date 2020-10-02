package punchclock.db;

import punchclock.*;

import java.text.*;
import java.util.Calendar;
import java.util.Date;
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

public class DBMethods
{
	private static Category cat = Category.getInstance(DBMethods.class.getName());
	
	private final static String SQL_SELECT_COMPANY_PATH = "SELECT {0} FROM companies WHERE id = {1,number,0}";

	private final static String SQL_SELECT_COMMENTS = "SELECT DISTINCT comment FROM timelog WHERE companyID = {0,number,0} " +
		"AND taskID = {1,number,0} ORDER BY comment";

	private final static String SQL_SELECT_EXPENSES = "SELECT * FROM expenses WHERE " +
		"date = ''{0}'' AND companyID = {1,number,0} ORDER BY description";

	private final static String SQL_SELECT_EXPENSE_AMOUNT = "SELECT amount, hstApplicable FROM expenses WHERE " +
		"date = ''{0}'' AND companyID = {1,number,0} AND description = ''{2}''";

	private final static String SQL_SELECT_TOTAL = "SELECT SUM({0}) FROM {1} WHERE " +
		"periodStart >= ''{2}'' AND periodEnd < ''{3}''";
	
	private final static String SQL_SELECT_TIMELOG_SECONDS = "SELECT taskID, start, end FROM timelog WHERE " +
		"start >= ''{0}'' AND end < ''{1}'' AND companyID = {2,number,0}";

	private final static String SQL_INSERT_TIMELOG = "INSERT INTO timelog (companyID, taskID, start, end, comment) " +
		"VALUES ({0,number,0}, {1,number,0}, ''{2}'', ''{3}'', \"{4}\")";

	private final static String SQL_INSERT_EXPENSE = "INSERT INTO expenses (companyID, date, amount, hstApplicable, description) " +
		"VALUES ({0,number,0}, ''{1}'', {2}, {3}, \"{4}\")";

	private final static String SQL_UPDATE_TIMELOG = "UPDATE timelog SET end = ''{0}'', " +
		"comment = \"{1}\" WHERE id = {2,number,0}";

	private final static String SQL_UPDATE_EXPENSE = "UPDATE expenses SET date = ''{0}'', amount = {1}, " +
		"hstApplicable = {2}, description = \"{3}\" WHERE id = {4,number,0}";

	private final static String SQL_UPDATE_COMPANY_PATH = "UPDATE companies SET {0} = ''{1}'' WHERE id = {2,number,0}";

	private final static String SQL_DELETE_EXPENSE = "DELETE FROM expenses WHERE " +
		"companyID = {0,number,0} AND date = ''{1}'' AND description = \"{2}\"";
	
	private final static String SQL_SELECT_LAST_END_DATE = "SELECT MAX(end) FROM timelog";
	
	private final static String SQL_SELECT_GREATEST_END_DATE_FOR_DATE = "SELECT MAX(end) from timelog WHERE end > ''{0}''";

	public DBMethods()
	{
	}

	public static int getTableInt(DataSourceProxy ds, String table, String field, String where)
	{
		Connection conn = null;
		ResultSet rs = null;
		Statement stmt = null;
		int result = -1;

		try
		{
			conn = ds.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT " + field + " FROM " + table + " WHERE " + where);
			if(rs.first())
				result = rs.getInt(1);
		}
		catch (Exception e)
		{
			cat.error(e.toString());
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

		return result;
	}

	public static void setTableInt(DataSourceProxy ds, String table, String field, int value, String where)
	{
		Connection conn = null;
		Statement stmt = null;

		try
		{
			conn = ds.getConnection();
			stmt = conn.createStatement();
			stmt.executeUpdate("UPDATE " + table + " SET " + field + " = " + value + " WHERE " + where);
		}
		catch (Exception e)
		{
			cat.error(e.toString());
		}
		finally
		{
			try
			{
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
	
	public static float getTableFloat(DataSourceProxy ds, String table, String field, String where)
	{
		Connection conn = null;
		ResultSet rs = null;
		Statement stmt = null;
		float result = 0;

		try
		{
			conn = ds.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT " + field + " FROM " + table + " WHERE " + where);
			if(rs.first())
				result = rs.getFloat(1);
		}
		catch (Exception e)
		{
			cat.error(e.toString());
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

		return result;
	}

	public static void setTableFloat(DataSourceProxy ds, String table, String field, float value, String where)
	{
		Connection conn = null;
		Statement stmt = null;

		try
		{
			conn = ds.getConnection();
			stmt = conn.createStatement();
			stmt.executeUpdate("UPDATE " + table + " SET " + field + " = " + value + " WHERE " + where);
		}
		catch (Exception e)
		{
			cat.error(e.toString());
		}
		finally
		{
			try
			{
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

	public static String getTableString(DataSourceProxy ds, String table, String field, String where)
	{
		Connection conn = null;
		ResultSet rs = null;
		Statement stmt = null;
		String result = null;

		try
		{
			conn = ds.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT " + field + " FROM " + table + " WHERE " + where);
			if(rs.first())
				result = rs.getString(1);
		}
		catch (Exception e)
		{
			cat.error(e.toString());
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

		return result;
	}

	public static String getLastComment(DataSourceProxy ds)
	{
		String result = null;
		int timelogID = getTableInt(ds, "timelog", "MAX(id)", "id = id");

		if(timelogID != -1)
			result = getTableString(ds, "timelog", "comment", "id = " + timelogID);

		return result;
	}

	public static String getCompanyPath(DataSourceProxy ds, int companyID, String docType)
	{
		String result = null;
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;

		try
		{
			conn = ds.getConnection();
			stmt = conn.createStatement();

			//Obtain resultset for specified company and docType
			Object[] insertArgs1 = {new String(docType + "Path"),
															new Integer(companyID)};
			String sql = MessageFormat.format(SQL_SELECT_COMPANY_PATH, insertArgs1);
			rs = stmt.executeQuery(sql);
			if(rs.first())
				result = rs.getString(1);
		}
		catch(Exception e)
		{
			cat.error(e.toString());
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

		return result;
	}

	public static void setCompanyPath(DataSourceProxy ds, int companyID, String docType, String path)
	{
		Connection conn = null;
		Statement stmt = null;

		try
		{
			conn = ds.getConnection();
			stmt = conn.createStatement();

			//Construct SQL to update specified path
			Object[] insertArgs1 = {new String(docType + "Path"),
															new String(path),
															new Integer(companyID)};
			String sql = MessageFormat.format(SQL_UPDATE_COMPANY_PATH, insertArgs1);
			stmt.executeUpdate(sql);
		}
		catch(Exception e)
		{
			cat.error(e.toString());
		}
		finally
		{
			try
			{
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

	public static boolean insertNewTimelogEntry(DataSourceProxy ds, String company, String task, String comment,
												int offset, java.util.Date inStart, java.util.Date inEnd)
	{
		boolean result = false;

		//Obtain companyID and taskID from combo boxes
		int companyID = getTableInt(ds, "companies", "id", "name='" + company + "'");
		int taskID = getTableInt(ds, "tasks", "id", "name='" + task + "'");
		if(companyID != -1 && taskID != -1)
		{
			String start, end;
			if (inStart == null)
			{
				//Create a new record in the timelog table using adjusted current date/time
				java.util.Date curTime = new java.util.Date();
				curTime.setTime(curTime.getTime() - (offset * 60000)); //Subtract offset minutes
				start = DateTime.getDateTimeString(curTime);
			}
			else
				start = DateTime.getDateTimeString(inStart);
			if (inEnd == null)
				end = start;
			else
				end = DateTime.getDateTimeString(inEnd);
			
			Object[] insertArgs = { new Integer(companyID),
									new Integer(taskID),
									new String(start),
									new String(end),
									new String(comment)};
			String sql = MessageFormat.format(SQL_INSERT_TIMELOG, insertArgs);
			
			//Create a new record in the timelog table
			Connection conn = null;
			Statement stmt = null;
			try
			{
				conn = ds.getConnection();
				stmt = conn.createStatement();
				stmt.executeUpdate(sql);
				result = true;
			}
			catch (Exception e)
			{
				cat.error(e.toString());
			}
			finally
			{
				try
				{
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

		return result;
	}

	public static long updateNewTimelogEntry(DataSourceProxy ds, String comment, int offset)
	{
		long result = 0;

		//Search timelog table for single record with start == end. Update end with current time.
		int timelogID = getTableInt(ds, "timelog", "id", "start = end");

		//Update timelog entry with current adjusted date/time
		java.util.Date curTime = new java.util.Date();
		curTime.setTime(curTime.getTime() + (offset * 60000)); //Add offset minutes
		String end = DateTime.getDateTimeString(curTime);
		Object[] insertArgs = {new String(end),
													 new String(comment),
													 new Integer(timelogID)};
		String sql = MessageFormat.format(SQL_UPDATE_TIMELOG, insertArgs);

		Connection conn = null;
		ResultSet rs = null;
		Statement stmt = null;
		try
		{
			conn = ds.getConnection();
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);

			//Now get start and end times for this record and compute time interval for display
			sql = "SELECT start, end FROM timelog WHERE id = " + timelogID;
			rs = stmt.executeQuery(sql);
			if(rs.first())
			{
				result = DateTime.getDateDiff(rs.getString("start"), rs.getString("end")) / 1000;
			}
		}
		catch (Exception e)
		{
			cat.error(e.toString());
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

		return result;
	}

	public static void populateCompanies(DataSourceProxy ds, JComboBox comboBox)
	{
		Connection conn = null;
		ResultSet rs = null;
		Statement stmt = null;

		comboBox.removeAllItems();
		try
		{
			conn = ds.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT name FROM companies");
			if(rs.first())
			{
				do
				{
					comboBox.addItem(rs.getString(1));
				} while(rs.next());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			cat.error(e.toString());
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

	public static void populateTasks(DataSourceProxy ds, JComboBox comboBox, String company)
	{
		Connection conn = null;
		ResultSet rs = null;
		Statement stmt = null;

		comboBox.removeAllItems();
		try
		{
			conn = ds.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT id FROM companies WHERE name='" + company + "'");
			if(rs.first())
			{
				int companyID = rs.getInt("id");

				//Now get tasks based on companyID
				rs = stmt.executeQuery("SELECT * FROM tasks WHERE companyID=" + companyID);
				if(rs.first())
				{
					do
					{
						String task = rs.getString("name");
						comboBox.addItem(task);
					} while(rs.next());
				}
			}
		}
		catch (Exception e)
		{
			cat.error(e.toString());
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

	public static void populateComments(DataSourceProxy ds, JComboBox comboBox, String company, String task)
	{
		Connection conn = null;
		ResultSet rs = null;
		Statement stmt = null;
		int companyID = getTableInt(ds, "companies", "id", "name='" + company + "'");
		int taskID = getTableInt(ds, "tasks", "id", "name='" + task + "'");

		if(companyID != -1 && taskID != -1)
		{
			comboBox.removeAllItems();

			try
			{
				conn = ds.getConnection();
				stmt = conn.createStatement();

				Object[] insertArgs = {new Integer(companyID), new Integer(taskID)};
				String sql = MessageFormat.format(SQL_SELECT_COMMENTS, insertArgs);
				rs = stmt.executeQuery(sql);
				if(rs.first())
				{
					do
					{
						comboBox.addItem(rs.getString(1));
					} while(rs.next());
				}
			}
			catch (Exception e)
			{
				cat.error(e.toString());
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
	}

	public static boolean populateExpenseDescriptions(DataSourceProxy ds, JComboBox comboBox, String date, String company)
	{
		boolean result = false;
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		int companyID = getTableInt(ds, "companies", "id", "name='" + company + "'");

		if(companyID != -1)
		{
			comboBox.removeAllItems();

			try
			{
				conn = ds.getConnection();
				stmt = conn.createStatement();

				Object[] insertArgs = {new String(date),
															 new Integer(companyID)};
				String sql = MessageFormat.format(SQL_SELECT_EXPENSES, insertArgs);
				rs = stmt.executeQuery(sql);
				if(rs.first())
				{
					do
					{
						ExpenseItem item = new ExpenseItem(rs.getInt("id"), companyID,
																					DateTime.getDateFromDateString(date),
																					rs.getLong("amount"), rs.getInt("hstApplicable"),
																					rs.getString("description"));

						comboBox.addItem(item);
					} while(rs.next());
				}

				result = true;
			}
			catch (Exception e)
			{
				cat.error(e.toString());
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

		return result;
	}

	public static int populateComboBox(DataSourceProxy ds, JComboBox comboBox, String sql, boolean clearExistingItems)
	{
		Connection conn = null;
		ResultSet rs = null;
		Statement stmt = null;
		int result = 0;

		if (clearExistingItems)
			comboBox.removeAllItems();

		try
		{
			conn = ds.getConnection();
			stmt = conn.createStatement();

			rs = stmt.executeQuery(sql);
			if(rs.first())
			{
				do
				{
					comboBox.addItem(rs.getString(1));
				} while(rs.next());

				result = comboBox.getItemCount();
			}
		}
		catch (Exception e)
		{
			cat.error(e.toString());
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

		return result;
	}

	public static int insertExpense(DataSourceProxy ds, ExpenseItem item)
	{
		int result = 0;
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			conn = ds.getConnection();
			stmt = conn.createStatement();

			Object[] insertArgs = {new Integer(item.companyID),
														 new String(DateTime.getDateString(item.date)),
														 new String(Long.toString(item.amount)),
														 new Integer(item.hstApplicable),
														 new String(item.description)};
			String sql = MessageFormat.format(SQL_INSERT_EXPENSE, insertArgs);
			stmt.executeUpdate(sql);
			rs = stmt.executeQuery("SELECT MAX(id) FROM expenses");
			if(rs.first())
				result = rs.getInt(1);
		}
		catch (Exception e)
		{
			cat.error(e.toString());
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

		return result;
	}

	public static boolean updateExpense(DataSourceProxy ds, ExpenseItem item)
	{
		boolean result = false;
		Connection conn = null;
		Statement stmt = null;

		try
		{
			conn = ds.getConnection();
			stmt = conn.createStatement();

			Object[] insertArgs = {new String(DateTime.getDateString(item.date)),
														 new String(Long.toString(item.amount)),
														 new Integer(item.hstApplicable),
														 new String(item.description),
														 new Integer(item.id)};
			String sql = MessageFormat.format(SQL_UPDATE_EXPENSE, insertArgs);
			stmt.executeUpdate(sql);
			result = true;
		}
		catch (Exception e)
		{
			cat.error(e.toString());
		}
		finally
		{
			try
			{
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			}
			catch (Exception e)
			{
			}
		}

		return result;
	}

	public static boolean deleteExpense(DataSourceProxy ds, ExpenseItem item)
	{
		boolean result = false;
		Connection conn = null;
		Statement stmt = null;

		try
		{
			conn = ds.getConnection();
			stmt = conn.createStatement();

			Object[] insertArgs = {new Integer(item.companyID),
														 new String(DateTime.getDateString(item.date)),
														 new String(item.description)};
			String sql = MessageFormat.format(SQL_DELETE_EXPENSE, insertArgs);
			stmt.executeUpdate(sql);
			result = true;
		}
		catch (Exception e)
		{
			cat.error(e.toString());
		}
		finally
		{
			try
			{
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			}
			catch (Exception e)
			{
			}
		}

		return result;
	}

	public static long getTableRangeSum(DataSourceProxy ds, String table, String field,
										java.util.Date start, java.util.Date end)
	{
		Connection conn = null;
		ResultSet rs = null;
		Statement stmt = null;
		long result = -1;

		try
		{
			conn = ds.getConnection();
			stmt = conn.createStatement();
			Object[] insertArgs = {new String(field),
														 new String(table),
														 new String(DateTime.getDateString(start)),
														 new String(DateTime.getDateString(end))};
			String sql = MessageFormat.format(SQL_SELECT_TOTAL, insertArgs);
			rs = stmt.executeQuery(sql);
			if(rs.first())
				result = rs.getLong(1);
		}
		catch (Exception e)
		{
			cat.error(e.toString());
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

		return result;
	}
	
	public static java.util.Date getLastTimelogEndDate(DataSourceProxy ds)
	{
		Connection conn = null;
		ResultSet rs = null;
		Statement stmt = null;
		java.util.Date result = null;

		try
		{
			conn = ds.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery(SQL_SELECT_LAST_END_DATE);
			String endTime = null;
			if (rs.first())
				endTime = rs.getString(1);
			if (endTime != null)
				result = DateTime.getDateFromDateString(endTime);				
		}
		catch (Exception e)
		{
			cat.error(e.toString());
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

		return result;
	}
	
	public static java.util.Date getNextTimelogStartDate(DataSourceProxy ds, java.util.Date inDate)
	{
		Connection conn = null;
		ResultSet rs = null;
		Statement stmt = null;
		java.util.Date result = null;

		try
		{
			conn = ds.getConnection();
			stmt = conn.createStatement();
			Object[] insertArgs = {new String(DateTime.getDateString(inDate))};
			String sql = MessageFormat.format(SQL_SELECT_GREATEST_END_DATE_FOR_DATE, insertArgs);
			rs = stmt.executeQuery(sql);
			String endTime = null;
			if (rs.first())
				endTime = rs.getString(1);
			if (endTime != null)
			{
				result = DateTime.getDateFromDateString(endTime);
				
				// Add one second to result
				result.setTime(result.getTime() + 1000);
			}
			else
			{
				// No records found for given date so return 08:00:00 on the given date
				Calendar cal = Calendar.getInstance();
				cal.setTime(inDate);
				cal.set(Calendar.HOUR_OF_DAY, 8);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND, 0);
				result = cal.getTime();
			}
		}
		catch (Exception e)
		{
			cat.error(e.toString());
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

		return result;
	}
	
	public static long getTimelogSeconds(DataSourceProxy ds, java.util.Date start, java.util.Date end, int companyID)
	{
		Connection conn = null;
		ResultSet rs = null;
		Statement stmt = null;
		long result = 0;
		
		String dateTimeNow = DateTime.getDateTimeString(new java.util.Date());
		try
		{
			conn = ds.getConnection();
			stmt = conn.createStatement();
			Object[] insertArgs = {new String(DateTime.getDateTimeString(start)),
								   new String(DateTime.getDateTimeString(end)),
								   new Integer(companyID)};
			String sql = MessageFormat.format(SQL_SELECT_TIMELOG_SECONDS, insertArgs);
			rs = stmt.executeQuery(sql);
			if(rs.first())
			{
				do
				{
					String startTime = rs.getString("start");
					String endTime = rs.getString("end");
					if (startTime.equals(endTime))
						endTime = dateTimeNow;
					long duration = DateTime.getDateDiff(startTime, endTime) / 1000;
					result += duration;
				} while(rs.next());
			}
		}
		catch (Exception e)
		{
			cat.error(e.toString());
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

		return result;
	}
	
	public static long getTimelogPennies(DataSourceProxy ds, java.util.Date start, java.util.Date end, int companyID)
	{
		Connection conn = null;
		ResultSet rs = null;
		Statement stmt = null;
		long result = 0;

		String dateTimeNow = DateTime.getDateTimeString(new java.util.Date());
		try
		{
			conn = ds.getConnection();
			stmt = conn.createStatement();
			Object[] insertArgs = {new String(DateTime.getDateTimeString(start)),
								   new String(DateTime.getDateTimeString(end)),
								   new Integer(companyID)};
			String sql = MessageFormat.format(SQL_SELECT_TIMELOG_SECONDS, insertArgs);
			rs = stmt.executeQuery(sql);
			if(rs.first())
			{
				do
				{
					String startTime = rs.getString("start");
					String endTime = rs.getString("end");
					if (startTime.equals(endTime))
						endTime = dateTimeNow;
					long duration = DateTime.getDateDiff(startTime, endTime) / 1000;
					
					// Now get the rate for this company and task
					Object[] insertArgs2 = {new Integer(rs.getInt("taskID")),
											new Integer(companyID)};
					String where = MessageFormat.format("id = {0,number,0} AND companyID = {1,number,0}", insertArgs2);
					int rate = getTableInt(ds, "tasks", "rate", where);
					result += (duration * rate / 3600);
				} while(rs.next());
			}
		}
		catch (Exception e)
		{
			cat.error(e.toString());
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

		return result;
	}
}