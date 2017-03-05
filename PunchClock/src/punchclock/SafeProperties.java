// Copyright 2002 Octave Communications, inc. All Rights Reserved.
package punchclock;

import java.io.*;
import java.util.Properties;

/**
 * Base class for "safe" properties file
 *
 * @author Clint Weisbrod
 */

public class SafeProperties extends Properties
{
	// Stream name is stored so that it can be included in exception text.
	private String streamName;

	public SafeProperties()
	{
	}

	/**
	 * Most often used load method which uses FileInputStream.
	 */
	public void load(String streamName) throws IOException
	{
		this.streamName = streamName;

		try
		{
			load(new FileInputStream(streamName));
		}
		catch (Exception e)
		{
				throw new IOException("Can't read " + streamName + ". Make sure this file is in the CLASSPATH");
		}
	}

	/**
	 * More general load method which allows for specific input stream.
	 */
	public void load(InputStream is, String streamName) throws IOException
	{
		this.streamName = streamName;

		try
		{
			load(is);
		}
		catch (Exception e)
		{
				throw new IOException("Can't read " + streamName + ". Make sure this file is in the CLASSPATH");
		}
	}

	/**
	 * Method to obtain a property value.
	 */
	public String getSafeProperty(String key) throws IOException
	{
		String result = getProperty(key);
		if(result == null)
			throw new IOException("\"" + key + "\" property not found in " + streamName);

		return result;
	}

	/**
	 * Method to obtain a property value which returns default value if property has no value.
	 */
	public String getSafeProperty(String key, String defaultValue) throws IOException
	{
		String result = getProperty(key);
		if(result == null)
			throw new IOException("\"" + key + "\" property not found in " + streamName);
		else
			result = getProperty(key, defaultValue);

		return result;
	}
}