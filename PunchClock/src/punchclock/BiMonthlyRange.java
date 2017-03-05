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

public class BiMonthlyRange extends DateRange
{
	public BiMonthlyRange()
	{
		setRange(new Date());
	}

	public void setRange(Date dt)
	{
		Calendar zeroTimeCal = getZeroTimeCalendar(dt);
		Calendar lastSecondCal = getLastSecondOfDayCalendar(dt);
		int currentDay = zeroTimeCal.get(Calendar.DAY_OF_MONTH);
		int lastDayOfMonth = zeroTimeCal.getActualMaximum(Calendar.DAY_OF_MONTH);
		if(currentDay <= 15)
		{
			zeroTimeCal.set(Calendar.DAY_OF_MONTH, 1);
			start = zeroTimeCal.getTime();
			lastSecondCal.set(Calendar.DAY_OF_MONTH, 15);
			end = lastSecondCal.getTime();
		}
		else
		{
			zeroTimeCal.set(Calendar.DAY_OF_MONTH, 16);
			start = zeroTimeCal.getTime();
			lastSecondCal.set(Calendar.DAY_OF_MONTH, lastDayOfMonth);
			end = lastSecondCal.getTime();
		}
	}
}