/*
 	This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
*/

package albaum;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;

//SWT docs says StyledText is not intented to be subclassed;
//I'll subclass whatever I feel like, thank you very much.

public class KeyText extends StyledText {
	public KeyText(Composite parent, int style) {
		super(parent, SWT.MULTI | style);
		setWordWrap(true);
	}
}
