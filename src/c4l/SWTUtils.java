package c4l;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import c4l.GUI.Callback;

public final class SWTUtils {
	public static MenuItem addMenuItem(final Menu m, final String l, 
		final int a, final Callback c) {
		MenuItem i = new MenuItem(m, SWT.PUSH);
		i.setText(l);
		i.setAccelerator(a);		

		i.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				c.call();
			}
		});
		
		return i;
	}
	
	public static void setFontStyle(final Control c, final int style) {
	    FontData[] data = c.getFont().getFontData();
	    
	    for (int i = 0; i < data.length; i++) {
	      data[i].setStyle(style);
	    }
	    
	    Font newFont = new Font(c.getDisplay(), data);
	    c.setFont(newFont);
	}		

	private SWTUtils() { }
}
