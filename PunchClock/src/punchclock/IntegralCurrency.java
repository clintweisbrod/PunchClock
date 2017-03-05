package punchclock;

import java.text.*;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2003
 * Company:      Weisbrod Software Engineering
 * @author Clint Weisbrod
 * @version 1.0
 */

public class IntegralCurrency {

	public IntegralCurrency()
	{
	}

	public static double roundToDecimal(double value, int precision)
	{
		double result;
		double base = Math.pow(10.0, (double)precision);

		//Round amount to specified precision
		value += (5.0 / (base * 10.0));
		result = Math.floor(value * base) / base;

		return result;
	}

	public static String stripToDigits(String str)
	{
		StringBuffer buf = new StringBuffer();
		for(int i=0; i<str.length(); i++)
		{
			if(Character.isDigit(str.charAt(i)))
				buf.append(str.charAt(i));
		}

		return buf.toString();
	}

	public static long stringToPennies(String value) throws NumberFormatException
	{
		String stripped = stripToDigits(value);

		//Convert buf containing only digits to integer
		return Long.parseLong(stripped);
	}

	public static long doubleToPennies(double amount) throws NumberFormatException
	{
		//Round amount to nearest penny and truncate to 2 decimal places
		amount += 0.005;
		return stringToPennies(Double.toString(Math.floor(amount * 100.0) / 100.0));
	}

	public static String penniesToString(long pennies)
	{
		String result = Long.toString(pennies);
		boolean negative;

		if(pennies < 0)
		{
			pennies = -pennies;
			negative = true;
		}
		else
			negative = false;

		int len = result.length();
		switch(len)
		{
			case 1:
				result = "0.0" + result;
				break;
			case 2:
				result = "0." + result;
				break;
			default:
				result = result.substring (0,len - 2) + "." + result.substring(len - 2, len);
				break;
		}
		if(negative)
			result = "-" + result;

		return result;
	}

	public static String penniesToCurrencyString(long pennies)
	{
		DecimalFormatSymbols dfs = new DecimalFormatSymbols();
		return dfs.getCurrencySymbol() + penniesToString(pennies);
	}

	public static String doubleToString(double val, int decimalPrecision)
	{
		String result;
		int i;
		StringBuffer zeros = new StringBuffer();
		Object[] insertArgs = {new Double(val)};

		for(i=0; i<decimalPrecision; i++)
			zeros.append('0');
		result = MessageFormat.format("{0,number,0." + zeros.toString() + "}", insertArgs);

		return result;
	}
}