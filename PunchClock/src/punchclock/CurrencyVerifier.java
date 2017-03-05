package punchclock;

import javax.swing.*;
import org.apache.log4j.*;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2003
 * Company:      Weisbrod Software Engineering
 * @author Clint Weisbrod
 * @version 1.0
 */

public class CurrencyVerifier extends InputVerifier
{
	// logging category
	private static Category cat = Category.getInstance(AppFrame.class.getName());

	public boolean verify(JComponent input)
	{
		boolean result = true;
		JTextField tf = (JTextField) input;
		String text = tf.getText();
/*
		DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		char decimal = symbols.getMonetaryDecimalSeparator();
		String dollarSign = symbols.getCurrencySymbol();

		//Scan for occurence of monetary decimal separator
		int index = text.lastIndexOf(symbols.getMonetaryDecimalSeparator());
		if(index >= 0)
		{
			//If monetary decimal separator does exist, it better be in the last position or the correct position
			//based on decimal places
		}
*/

		//For now, just make sure text can be parsed to a double value.
		if(text.length() > 0)
		{
			try
			{
				Double.parseDouble(text);
			}
			catch(NumberFormatException e)
			{
				result = false;
				cat.equals(e.toString());
			}
		}

		return result;
	}
}
