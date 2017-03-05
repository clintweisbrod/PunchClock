package punchclock;

import java.util.*;
import java.text.*;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2003
 * Company:
 * @author Clint Weisbrod
 * @version 1.0
 */

public class DateTime
{
	public DateTime()
	{
	}

	public static String getCurrentDateString()
	{
		Calendar rightNow = Calendar.getInstance();
		java.util.Date curDate = rightNow.getTime();
		SimpleDateFormat formatter = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");

		return formatter.format(curDate);
	}

	public static String getDateString(java.util.Date dt)
	{
		SimpleDateFormat formatter = new SimpleDateFormat ("yyyy-MM-dd");

		return formatter.format(dt);
	}

	public static String getDateTimeString(java.util.Date dt)
	{
		SimpleDateFormat formatter = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");

		return formatter.format(dt);
	}

	public static java.util.Date getDateFromDateString(String dateString)
	{
		java.util.Date result = null;

		try
		{
			SimpleDateFormat formatter = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");
			result = formatter.parse(dateString);
		}
		catch (ParseException e1)
		{
			try
			{
				SimpleDateFormat formatter = new SimpleDateFormat ("yyyy-MM-dd");
				result = formatter.parse(dateString);
			}
			catch (ParseException e2)
			{
			}
		}

		return result;
	}
	
	public static String get12HourTimeString(java.util.Date dt, String format)
	{
		SimpleDateFormat formatter = new SimpleDateFormat(format);

		return formatter.format(dt);
	}

	public static String getTimeString(long seconds)
	{
		long hour, minute, second;
		String result = null;

		hour = seconds / 3600;
		minute = (seconds - (hour * 3600)) / 60;
		second = seconds - (hour * 3600) - (minute * 60);

		Object[] insertArgs = {new Long(hour),
													 new Long(minute),
													 new Long(second)};
		result = MessageFormat.format("{0,number,00}:{1,number,00}:{2,number,00}", insertArgs);

		return result;
	}

	public static long getDateDiff(String date1, String date2)
	{
		long result = 0;

		java.util.Date dt1 = getDateFromDateString(date1);
		java.util.Date dt2 = getDateFromDateString(date2);
		result = dt2.getTime() - dt1.getTime();

		return result;
	}
	
	public static String stripDecimalPortion(String dateString)
	{
		String result = dateString;
		int index = dateString.lastIndexOf('.');
		if (index != -1)
			result = dateString.substring(0, index);
		
		return result;
	}
}