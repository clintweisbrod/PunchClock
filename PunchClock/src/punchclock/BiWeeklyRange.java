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

public class BiWeeklyRange extends DateRange
{
	private Date refStartDate;

	public BiWeeklyRange()
	{
		Calendar cal = getZeroTimeCalendar(new Date());
		refStartDate = cal.getTime();

		setRange(refStartDate);
	}

	public BiWeeklyRange(Date dt)
	{
		Calendar cal = getZeroTimeCalendar(dt);
		refStartDate = cal.getTime();

		setRange(new Date());
	}

	public void setRange(Date dt)
	{
		Calendar cal = getZeroTimeCalendar(dt);
		Date dt0 = cal.getTime();
		int dayOfYear;
		Date testDate;

		if(refStartDate.equals(dt0))
		{
			start = dt0;
			dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
			dayOfYear += 14;
			cal.set(Calendar.DAY_OF_YEAR, dayOfYear);
			end = cal.getTime();
			end = new Date(end.getTime() - 1);	// Subtract one millisecond
		}
		else if(refStartDate.before(dt))
		{
			cal.setTime(refStartDate);
			testDate = cal.getTime();
			dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
			while (testDate.before(dt))
			{
				dayOfYear += 14;	// dayOfYear is correctly interpreted beyond 365
				cal.set(Calendar.DAY_OF_YEAR, dayOfYear);
				testDate = cal.getTime();
				dayOfYear = cal.get(Calendar.DAY_OF_YEAR);	// We do this so that our day of year remains <= 366
			}
			end = cal.getTime();
			end = new Date(end.getTime() - 1);	// Subtract one millisecond
			
			// Go back 14 days for start date
			dayOfYear -= 14;
			cal.set(Calendar.DAY_OF_YEAR, dayOfYear);
			start = cal.getTime();
			refStartDate = start;
		}
		else
		{
			cal.setTime(refStartDate);
			testDate = cal.getTime();
			dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
			while (testDate.after(dt))
			{
				dayOfYear -= 14;	// dayOfYear is correctly interpreted before 1
				cal.set(Calendar.DAY_OF_YEAR, dayOfYear);
				testDate = cal.getTime();
				dayOfYear = cal.get(Calendar.DAY_OF_YEAR);	// We do this so that our day of year remains >= 1
			}
			start = cal.getTime();
			refStartDate = start;
			
			// Go forward 14 days for end date
			dayOfYear += 14;
			cal.set(Calendar.DAY_OF_YEAR, dayOfYear);
			end = cal.getTime();
			end = new Date(end.getTime() - 1);	// Subtract one millisecond
		}
	}
}