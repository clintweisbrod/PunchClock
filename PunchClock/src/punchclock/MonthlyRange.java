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

public class MonthlyRange extends DateRange
{
	public MonthlyRange()
	{
		setRange(new Date());
	}

	public void setRange(Date dt)
	{
		Calendar cal = getZeroTimeCalendar(dt);
		int lastDayOfMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		start = cal.getTime();
		cal.set(Calendar.DAY_OF_MONTH, lastDayOfMonth);
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		end = cal.getTime();
	}
}