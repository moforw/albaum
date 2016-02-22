package albaum;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

public class Reader extends KeyText {
	public static final int MIN_INPUT_LENGTH = 2;

	public Reader(final Trie t, final Composite p, int s) {
		super(p, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | s);
		trie = t;
		
	   	addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				if (getSelectionCount() == 0) {
					inputEnd = getText().length();
				} else {
    				final int selStart = getSelection().x;
    		    	if (selStart > inputEnd) {
    		    		inputEnd = selStart;
    		    	}
    			}
			}
	   	});
	   	
	    addListener(SWT.KeyDown, new Listener() {
	        public void handleEvent(Event e) {
	        	isBackspacing = e.keyCode == 8;

	        	if (e.character > 0 && inputEnd < getText().length()) {
	        		inputEnd++;
	        	} else if (isBackspacing && inputEnd > getText().length()) {
	        		inputEnd--;
	        	}

	    		synchronized (this) {
	    			currentInput = getText();
	    		}
	    		
	    		checkPerfectMatch(currentInput());
	        }	        
	      });
	}
	
	public void checkPerfectMatch(final String key) {
		final Node n = trie.find(key);
		
		perfectMatch = null;

		if (n != null) {
			for (final Fact t: n.facts) {
				if (t.key.compareToIgnoreCase(key) == 0) {
					perfectMatch = t;
					break;
				}
			}
			
			if (perfectMatch == null && n.facts.size() == 1) {
				final Fact t = n.facts.iterator().next();
				if (t.key.indexOf(key) > -1) {
					perfectMatch = t;
				}
			}
			
			if (!isBackspacing && key.length() >= MIN_INPUT_LENGTH) {
				final Node en = n.extend();
				
				if (en != n) {
					inputEnd = key.length();
					setText(en.key);
					setSelection(inputEnd, en.key.length());
				}
			}
		}
		
		SWTUtils.setFontStyle(this, (perfectMatch == null) ? SWT.NONE : SWT.BOLD);
	}
	
	public String currentInput() {
		synchronized (this) {
			return currentInput;
		}
	}
	
	@Override
	public void setText(final String t) {
		super.setText(t);
		
		synchronized (this) {
			currentInput = t;
		}
		
		checkPerfectMatch(t);
	}

	private String currentInput;
	private int inputEnd;
	private boolean isBackspacing;
	private Fact perfectMatch;
	private final Trie trie;
}