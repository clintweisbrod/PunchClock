package punchclock;

import java.util.*;
import mseries.ui.*;
import javax.swing.event.*;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2003
 * Company:
 * @author Clint Weisbrod
 * @version 1.0
 */

public class DateRangeSpinnerModel implements SpinnerModel
{
	EventListenerList listenerList = new EventListenerList();
	DateRange dtRange = null;

	public DateRangeSpinnerModel(DateRange dtRange)
	{
		this.dtRange = dtRange;
	}

	public Object getValue()
	{
		return dtRange.getStart();
	}

	public void setValue(Object v)
	{
		dtRange.setRange((Date)v);
		notifyListeners();
	}

	public void setStep(int step)
	{
	}

	public Object getNextValue()
	{
		//moveBackward() seems counter for getting the next value, but it corresponds to spinning up
		dtRange.moveBackward();
		notifyListeners();
		return dtRange.getStart();
	}

	public Object getPreviousValue()
	{
		//moveForward() seems counter for getting the previous value, but it corresponds to spinning down
		dtRange.moveForward();
		notifyListeners();
		return dtRange.getStart();
	}
	public void addChangeListener(ChangeListener l)
	{
		listenerList.add(ChangeListener.class, l);
	}
	public void removeChangeListener(ChangeListener l)
	{
		listenerList.remove(ChangeListener.class, l);
	}
	protected void notifyListeners()
	{
		ChangeEvent event;
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length-2; i>=0; i-=2)
		{
			if (listeners[i]==ChangeListener.class)
			{
				event = new ChangeEvent(this);
				((ChangeListener)listeners[i+1]).stateChanged(event);
			}
		}
	}
}
