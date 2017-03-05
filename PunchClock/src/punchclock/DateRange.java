package punchclock;

import java.util.*;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2003
 * Company:
 * @author Clint Weisbrod
 * @version 1.0
 */

public abstract class DateRange
{
	public static final int WEEKLY = 1;
	public static final int BIWEEKLY = 2;
	public static final int BIMONTHLY = 3;
	public static final int MONTHLY = 4;

	protected Date start = null;
	protected Date end = null;

	public abstract void setRange(Date dt);

	public DateRange() {
	}

	public DateRange(Date dt) {
		setRange(dt);
	}

	public Calendar getZeroTimeCalendar(Date dt)
	{
		Calendar result = Calendar.getInstance();
		result.setTime(dt);
		result.set(Calendar.HOUR, 0);
		result.set(Calendar.MINUTE, 0);
		result.set(Calendar.SECOND, 0);
		result.set(Calendar.MILLISECOND, 0);
		result.set(Calendar.AM_PM, Calendar.AM);

		return result;
	}
	
	public Calendar getLastSecondOfDayCalendar(Date dt)
	{
		Calendar result = Calendar.getInstance();
		result.setTime(dt);
		result.set(Calendar.HOUR, 11);
		result.set(Calendar.MINUTE, 59);
		result.set(Calendar.SECOND, 59);
		result.set(Calendar.MILLISECOND, 0);
		result.set(Calendar.AM_PM, Calendar.PM);

		return result;
	}

	public void moveForward() {
		// Get calendar instance representing one day more than end date
		Calendar cal = Calendar.getInstance();
		cal.setTime(end);
		cal.add(Calendar.DAY_OF_YEAR, 1);
		
//		cal.setTime(new Date(end.getTime() + 86400000));
		setRange(cal.getTime());
	}

	public void moveForward(int numPeriods) {
		for(int i=0; i<numPeriods; i++)
			moveForward();
	}

	public void moveBackward() {
		// Get calendar instance representing one day less than start date
		Calendar cal = Calendar.getInstance();
		cal.setTime(start);
		cal.add(Calendar.DAY_OF_YEAR, -1);
		
//		cal.setTime(new Date(start.getTime() - 86400000));
		setRange(cal.getTime());
	}

	public void moveBackward(int numPeriods) {
		for(int i=0; i<numPeriods; i++)
			moveBackward();
	}

	public Date getStart() {
		return start;
	}

	public Date getEnd() {
		return end;
	}

	private int getRangeWeekdays(Date dt)
	{
		int result = 0;
		int day;
		int lastDSTOffset, curDSTOffset, deltaDSTOffset;
		Date x = new Date(start.getTime());
		Calendar cal = Calendar.getInstance();

		cal.setTime(x);
		while(x.getTime() < dt.getTime())
		{
			lastDSTOffset = cal.get(Calendar.DST_OFFSET);
			day = cal.get(Calendar.DAY_OF_WEEK);
			if(day >= Calendar.MONDAY && day <= Calendar.FRIDAY)
				result++;
			x.setTime(x.getTime() + 86400000);
			cal.setTime(x);
			curDSTOffset = cal.get(Calendar.DST_OFFSET);
			deltaDSTOffset = curDSTOffset - lastDSTOffset;
			if (deltaDSTOffset != 0)
			{
				x.setTime(x.getTime() - deltaDSTOffset);
				cal.setTime(x);
			}
		}

		return result;
	}

	public long getRangeWeekdaysInSeconds()
	{
		return ((long)getRangeWeekdays(end) * 86400);
	}

	public long getElapsedRangeWeekdaysInSeconds(Date dt)
	{
		Calendar cal = Calendar.getInstance();
		long result = (long)getRangeWeekdays(dt);

		// Now convert weekday value to seconds
		result = (result - 1) * 86400;

		// Adjust result to include elapsed seconds today
		cal.setTime(dt);
		result += (cal.get(Calendar.HOUR_OF_DAY) * 3600 + cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND));

		return result;
	}

	public int getElapsedRangeWeekdays(Date dt)
	{
		int result = getRangeWeekdays(dt);

		if (result > 0)
			result--;

		return result;
	}

	public double getRangeProgressByWeekday(Date dt)
	{
		long weekdaysSeconds = getRangeWeekdaysInSeconds();
		long curWeekdaySeconds = getElapsedRangeWeekdaysInSeconds(dt);

		return (double)curWeekdaySeconds / (double)weekdaysSeconds;
	}
	
	public double getRangeProgress(Date dt)
	{
		double result = 0;
		
		long rangeMilliseconds = end.getTime() - start.getTime();
		long currentRangeMilliseconds = dt.getTime() - start.getTime();
		result = (double)currentRangeMilliseconds / (double)rangeMilliseconds;
		
		return result;
	}
}