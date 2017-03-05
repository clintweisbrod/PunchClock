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

public class WeeklyRange extends DateRange
{
	private int weekDayStart = Calendar.SUNDAY;

	public WeeklyRange()
	{
		setRange(new Date());
	}

	public WeeklyRange(int weekDayStart)
	{
		this.weekDayStart = weekDayStart;
		setRange(new Date());
	}

	public void setRange(Date inStartDate)
	{
		Calendar cal = getZeroTimeCalendar(inStartDate);
		int curDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
		int offset = curDayOfWeek - weekDayStart;
		if(offset < 0)
			offset = 7 + offset;
		cal.add(Calendar.DAY_OF_YEAR, -offset);
//		cal.setTime(new Date(curDate.getTime() - (long)offset*86400000));
		start = cal.getTime();
		cal.add(Calendar.DAY_OF_YEAR, 7);
		cal.add(Calendar.MILLISECOND, -1);
//		cal.setTime(new Date(start.getTime() + 7*86400000 - 1));
		end = cal.getTime();
	}
}