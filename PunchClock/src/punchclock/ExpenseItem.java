package punchclock;

import java.util.*;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2003
 * Company:      Weisbrod Software Engineering
 * @author Clint Weisbrod
 * @version 1.0
 */

public class ExpenseItem
{
	public int id;
	public int companyID;
	public Date date;
	public long amount;
	public int hstApplicable;
	public String description;

	public ExpenseItem(int id, int companyID, Date date, long amount, int hstApplicable, String description)
	{
		this.id = id;
		this.companyID = companyID;
		this.date = date;
		this.amount = amount;
		this.hstApplicable = hstApplicable;
		this.description = description;
	}

	public String toString()
	{
		return description;
	}
}