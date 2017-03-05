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

public abstract class DocGen
{
	AppFrame appFrame;
	String filePath;
	String fileName;

	public abstract String getDocExtension();
	public abstract String getBaseFilename();
	public abstract boolean generateDoc(StringBuffer status);

	public String getDocFilePath() {
		return filePath;
	}

	public String getDocFileName() {
		return fileName;
	}

	public String getDocFileFullPath() {
		return filePath + "/" + fileName;
	}

	protected void generateDocFileName(Date dt)
	{
		filePath = appFrame.txtDocPath.getText();
		fileName = getBaseFilename() + DateTime.getDateString(dt) + "." + getDocExtension();
	}

	protected double millisecondsToRoundedHours(long milliseconds, int precision)
	{
		double result;
		double hours;

		hours = ((double)milliseconds / 3600000.0) + (5.0 / Math.pow(10.0, (double)precision + 1));
		result = Math.floor(hours * Math.pow(10.0, (double)precision)) / Math.pow(10.0, (double)precision);

		return result;
	}
}