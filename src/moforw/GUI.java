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

package moforw;

import java.nio.file.FileSystems;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

public class GUI {
	public static interface Callback {
		void call();
	}	
	
	public static interface Update {
		void run();
	}

	public static void run(final Context c) {
		final GUI gui = new GUI(c);
		gui.shell.open();
		
	    gui.updateFont();
	    gui.updateFontSize();
	    gui.setFonts();
		
		while (!gui.shell.isDisposed()) {
			if (gui.isExiting) {
				gui.shell.dispose();
			}
						
			if (!gui.display.readAndDispatch()) {
				gui.display.sleep();
			}
		}
		
		gui.display.dispose();
	}

	public GUI(final Context cx) {
		context = cx;
		
		mainTrie = new Trie(
			new Log(cx, FileSystems.getDefault().getPath("commit.log")));
		
		context.load(mainTrie);
		
		tempTrie = new Trie(null);
				
	    display = new Display();
	    shell = new Shell(display);
	    shell.setMaximized(true);
	    
	    shell.setLayout(new FillLayout());
	    
	    shell.addDisposeListener((e) -> {
            flashTimer.cancel();
            searchTimer.cancel();
            exec.shutdown();
        });	    
	    
	    mainPanel = new Composite(shell, SWT.NONE);
	    
	    final GridLayout layout = new GridLayout();
	    layout.numColumns = 1;
	    mainPanel.setLayout(layout);
	    
	    readerPanel = createReaderPanel();
	    reader = createReader(readerPanel);
	    storeButton = createStoreButton(readerPanel);
	    shell.setDefaultButton(storeButton);
		tree = createTree();		
		flash = createFlash();
		
	    shell.setTabList(new Control[] {mainPanel});
	    mainPanel.setTabList(new Control[] {readerPanel, tree});
	    
	    flashTimer.scheduleAtFixedRate(new TimerTask() {
	    	  @Override
	    	  public void run() {
	    		  display.asyncExec(() -> updateFlash());
	    	  }
	    	}, 0, FLASH_INTERVAL * 1000);
	    
		menuBar = new Menu(shell, SWT.BAR);
	    shell.setMenuBar(menuBar);
	    createAlbaumMenu();
	    createEditMenu();
	    createViewMenu();
	    	    
	    updateCaption();
	}
								
	private static final int FLASH_INTERVAL = 20;
	private static final int READER_FONT_DELTA = 2;
	private static final int READER_ROWS = 3;
	private static final int SEARCH_INTERVAL = 50;
	
	private final Context context;
	private final Display display;
	private final ExecutorService exec = 
		Executors.newFixedThreadPool(
			Runtime.getRuntime().availableProcessors() + 2);
	private final Map<Fact, TreeItem> factItems = new ConcurrentSkipListMap<>();
	private final KeyText flash;
    private final Timer flashTimer = new Timer();
    private String font = "DejaVu Sans Mono";
    private int fontSize = 10;
	private boolean isExiting;
	private final Composite mainPanel;
    private final Trie mainTrie;
    private Menu menuBar;
 	private final Set<String> pinnedKeys = new HashSet<>();
    private String prevInput = "";
    private final Reader reader;
    private final Composite readerPanel;
    private final Timer searchTimer = new Timer();
    private TimerTask searchTask;
    private final Shell shell;
    private final Button storeButton;
    private Callback storeCallback = this::storeInput;
    private final Trie tempTrie;
    private final Tree tree;
	
	private Menu createEditMenu() {
		final MenuItem mi = new MenuItem(menuBar, SWT.CASCADE);
		mi.setText("&Edit");
		
		final Menu m = new Menu(shell, SWT.DROP_DOWN);
		mi.setMenu(m);
		
		SWTUtils.addMenuItem(m, "&Store", SWT.CTRL + 'S', () -> {
			storeCallback.call();
		});

		SWTUtils.addMenuItem(m, "Select &All", SWT.CTRL + 'A', () -> {
			tree.selectAll();
		});
		
		SWTUtils.addMenuItem(m, "Select &None", SWT.CTRL + 'N', () -> {
			tree.deselectAll();
		});
		
		SWTUtils.addMenuItem(m, "&Delete Selection", SWT.CTRL + 'D', () -> {
			final Set<TreeItem> ps = new HashSet<>();
			
			for (final TreeItem si: tree.getSelection()) {
				if (si.getParentItem() != null) {
					ps.add(si.getParentItem());
				}
				
				deleteResult(si);
			}			
			
			for (final TreeItem pi: ps) {
				if (pi.getItemCount() == 0) {
					pi.dispose();
				}
			}
		});

		SWTUtils.addMenuItem(m, "&Pin Selection", SWT.CTRL + 'P', () -> {
			final Set<TreeItem> hs = new HashSet<>();

			for (final TreeItem ti: tree.getSelection()) {
				if (ti.getItemCount() == 0) {
					ti.setChecked(true);
					pinnedKeys.add(((Fact)ti.getData()).key);
					
					final TreeItem pi = ti.getParentItem();
					if (pi != null) {
						hs.add(pi);
					}
				}
			}

			pinTreeHeaders(hs);
		});

		SWTUtils.addMenuItem(m, "&Unpin Selection", SWT.CTRL + 'U', () -> {
			final Set<TreeItem> hs = new HashSet<>();

			for (final TreeItem ti: tree.getSelection()) {
				if (ti.getItemCount() == 0) {
					ti.setChecked(false);
					pinnedKeys.remove(((Fact)ti.getData()).key);

					final TreeItem pi = ti.getParentItem();
					if (pi != null) {
						hs.add(pi);
					}
				}
			}

			unpinTreeHeaders(hs);			
		});
		
		return m;
	}

	private KeyText createFlash() {
	    final KeyText f = new KeyText(shell, SWT.READ_ONLY);
	    f.setParent(mainPanel);
	    final Point ss = shell.getSize();		
		final Point rs = f.getSize();
	    f.setSize(ss.x, rs.y);
		f.setBackground(shell.getBackground());
		SWTUtils.setFontStyle(f, SWT.ITALIC);
		f.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		f.setAlignment(SWT.CENTER);
	    return f;
	}
	
	private Menu createAlbaumMenu() {
		final MenuItem mi = new MenuItem(menuBar, SWT.CASCADE);
		mi.setText("&Albaum");
		
		final Menu m = new Menu(shell, SWT.DROP_DOWN);
		mi.setMenu(m);
		
		SWTUtils.addMenuItem(m, "E&xit", SWT.SHIFT + SWT.CTRL + 'X', () -> {
			isExiting = true;
		});
		
		return m;
	}
	
	private Reader createReader(final Composite p) {
	    final Reader r = new Reader(tempTrie, shell, SWT.None);
	    r.setParent(p);
	    
	    r.addListener(SWT.KeyDown, (e) -> {
    	    storeButton.setEnabled(reader.currentInput().length() >= 
    	    	Reader.MIN_INPUT_LENGTH);

        	if (searchTask != null) {
        		searchTask.cancel();
        	}
        		        	
        	searchTask = new TimerTask() {
        		@Override
        		public void run() {
        			final String i = reader.currentInput();
        			if (i.compareToIgnoreCase(prevInput) != 0) {
        				search(i);
        				final Stream<Node> ns = sortResults();
        				display.asyncExec(() -> {
        					updateResults(ns);
        					reader.checkPerfectMatch(i);
        				});
        			}
        		}
	    	};
	    	
		    searchTimer.schedule(searchTask, SEARCH_INTERVAL);
	    });

	    final Point ss = shell.getSize();				
		final Point rs = r.getSize();
	    r.setSize(ss.x, rs.y);
		r.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));	    		 
	    p.setTabList(new Control[] {r});
	    return r;
	}
	
	private Composite createReaderPanel() {
		final Composite p = new Composite(shell, SWT.NONE);
		p.setParent(mainPanel);
		p.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
	    final GridLayout layout = new GridLayout();
	    layout.numColumns = 2;
	    p.setLayout(layout);
	    
	    return p;
	}
	
	private Button createStoreButton(final Composite p) {
	    final Button b = new Button(shell, SWT.CENTER);
	    b.setParent(p);
	    b.setText("Store");
	    b.setImage(new Image(display, getClass().getResourceAsStream("/images/store.png")));	    
	    final GridData gd = new GridData(GridData.END | GridData.FILL_VERTICAL);	    
		b.setLayoutData(gd);
		b.setEnabled(false);

		b.addListener(SWT.Resize, (e) -> {
			final Point s = b.getSize();
			gd.widthHint = s.x + 10;			
		});
		
		b.addListener(SWT.Selection, (e) -> {
			if (e.type == SWT.Selection) {
				storeInput();
			}
		});
		
		return b;
	}		
	
	private Tree createTree() {
		final Tree t = new Tree(shell, 
			SWT.BORDER | SWT.CHECK | SWT.MULTI | SWT.V_SCROLL);
		
		t.setParent(mainPanel);
		t.setHeaderVisible(true);
		t.setLinesVisible(false);
		
		final Point ss = shell.getSize();
		t.setSize(ss.x / 2, ss.y);
		
		t.setLayoutData(new GridData(GridData.FILL_BOTH));

		TreeColumn col = new TreeColumn(t, SWT.NONE);
		col.setText("Created At");
	    col.setWidth(240);

		col = new TreeColumn(t, SWT.RIGHT);
		col.setText("Score");
	    col.setWidth(80);
	    col.setWidth(0);

		col = new TreeColumn(t, SWT.RIGHT);
		col.setText("Rev");
	    col.setWidth(60);

	    col = new TreeColumn(t, SWT.NONE);
		col.setText("Text");
	    col.setWidth(SWT.MAX);

	    t.addListener(SWT.Selection, (e) -> {
	    	if (e.detail == SWT.CHECK) {
	    		final TreeItem i = (TreeItem)e.item;
	    		final String k = ((HasKey)i.getData()).key();
	
	    		final Set<TreeItem> pinnedHeaders = new HashSet<>();
	    		final Set<TreeItem> unpinnedHeaders = new HashSet<>();
	
	    		final TreeItem pi = i.getParentItem();
	    	  
	    		if (i.getChecked()) {
	    			pinnedKeys.add(k);
	    			if (pi != null) {
	    				pinnedHeaders.add(pi);
	    			}
	    		} else {
	    			pinnedKeys.remove(k);
	    			if (pi != null) {
	    				unpinnedHeaders.add(pi);
	    			}
	    		}
	    	  
	    		for (final TreeItem ii: i.getItems()) {
	    			final String kk = ((HasKey)ii.getData()).key();
	    			final TreeItem pii = ii.getParentItem();
	
	    			if (i.getChecked()) {
	    				ii.setChecked(true);
	    				pinnedKeys.add(kk);
	
	    				if (pii != null) {
	    					pinnedHeaders.add(pii);
	    				}
	    			} else {
	    				ii.setChecked(false);
	    				pinnedKeys.remove(kk);
	    				if (pii != null) {
	    					unpinnedHeaders.add(pii);
	    				}
	    			}
	    		}
	    	  
	    		if (!pinnedHeaders.isEmpty()) {
	    			pinTreeHeaders(pinnedHeaders);
	    		}
	
	    		if (!unpinnedHeaders.isEmpty()) {
	    			unpinTreeHeaders(unpinnedHeaders);
	    		}
	    	}
	    });
	    
	    final TreeEditor editor = new TreeEditor(t);
	    editor.setColumn(3);
	    editor.grabHorizontal = true;
	    
	    t.addListener(SWT.KeyDown, (e) -> {
	    	if (e.keyCode == 13) {
	    		editResult(editor);
	    	}
	    });

	    t.addListener(SWT.MouseDoubleClick, (e) -> {
	    	editResult(editor);
	    });

		return t;
	}

	private Menu createViewMenu() {
		final MenuItem mi = new MenuItem(menuBar, SWT.CASCADE);
		mi.setText("&View");
		
		final Menu m = new Menu(shell, SWT.DROP_DOWN);
		mi.setMenu(m);
		
		SWTUtils.addMenuItem(m, "Increase Font Size", SWT.CTRL + '+', () -> {
			setFontSize(fontSize + 1);
		});
		
		SWTUtils.addMenuItem(m, "Decrease Font Size", SWT.CTRL + '-', () -> {
			setFontSize(fontSize - 1);
		});
		
		return m;
	}
	
	private void deleteResult(final TreeItem ti) {
		if (ti.getItemCount() == 0) {
			final Fact f = (Fact)ti.getData();
			ti.dispose();

			exec.submit(() -> {
				mainTrie.deleteAll(f.prototype, context);
				tempTrie.deleteAll(f, context);
				context.commit();
			});
		}
	}

	private void editResult(final TreeEditor te) {
		if (tree.getSelectionCount() == 1) {
			final TreeItem i = tree.getSelection()[0];
			
			if (i.getItemCount() == 0) {
				final Reader r = new Reader(mainTrie, shell, SWT.None);
				setFont(r, READER_FONT_DELTA);
				te.minimumHeight =  getReaderSize(r);				
				r.setParent(tree);
				final Fact t = (Fact)i.getData();
				r.setText(t.key);
				r.selectAll();
				r.setFocus();
				te.setEditor(r, i);
								
				storeCallback = () -> {
					final String k = r.currentInput();

	        		if (k != t.key) {
						updateTries(t, k);
					}
					
					r.dispose();
					storeCallback = GUI.this::storeInput;
				};
				
				r.addListener(SWT.KeyDown, (e) -> {	
					if (e.keyCode == 27) {
						r.dispose();						
						storeCallback = GUI.this::storeInput;
					}
				});
			}			
		}
	}

	private String getCaption() {
		final Fact f = mainTrie.root.findFirstFact("#caption ");
		if (f != null) {
			return f.key.replace("#caption ", "");
		}
		return null;
	}

	private int getReaderSize(final KeyText r) {
		final GC gc = new GC(r);
		try
		{
		    gc.setFont(r.getFont());
		    final FontMetrics fm = gc.getFontMetrics();
		    return READER_ROWS * fm.getHeight();
		}
		finally {
		    gc.dispose();
		}
	}

	private void initItem(final TreeItem i, final Node n) {
		int tc = 0;
		Fact f = null;
		
		for (Fact tt: n.facts) {
			if (!factItems.containsKey(tt)) {
				f = tt;
				tc++;
			}
		}

		if (tc == 1) {
			initItem(i, f);
		} else {
			i.setText(new String[]{context.format(n.insertedAt), 
				String.valueOf(n.score()), "", n.key});
			i.setData(n);
			if (pinnedKeys.contains(n.key)) {
				i.setChecked(true);
			}
				
			for (final Fact ff: n.facts) {
				if (!factItems.containsKey(ff)) {
					newItem(i, ff);
				}
			}
		}
	}

	private void initItem(final TreeItem i, final Fact f) {
		i.setText(new String[]{context.format(f.createdAt), 
			"", String.valueOf(f.version), f.key});
		i.setData(f);
		
		if (pinnedKeys.contains(f.key)) {
			i.setChecked(true);
		}
		
		factItems.put(f, i);
	}

	private TreeItem newItem(final Tree p, final Node n) {
		final TreeItem i = new TreeItem(p, SWT.NONE);
		initItem(i, n);
		return i;
	}
	
	private TreeItem newItem(final TreeItem p, final Fact t) {
		final TreeItem i = new TreeItem(p, SWT.NONE);
		initItem(i, t);
		p.setExpanded(true);					
		return i;
	}

	private void pinTreeHeaders(final Collection<TreeItem> is) {
		for (final TreeItem ti: is) {
			if (ti.getItemCount() > 0) {
				boolean allPinned = true;
				for (final TreeItem ci: ti.getItems()) {
					if (!ci.getChecked()) {
						allPinned = false;
						break;
					}
				}
				
				if (allPinned) {
					ti.setChecked(true);
					pinnedKeys.add(((HasKey)ti.getData()).key());
				}
			}
		}
	}
	
	private void search(final String key) {		
		Set<Fact> newFacts;
		
		if (key.length() >= Reader.MIN_INPUT_LENGTH) {
			newFacts = mainTrie.findAll(key, Reader.MIN_INPUT_LENGTH);
		} else {
			newFacts = new HashSet<>();
		}
		
		factItems.keySet().parallelStream().forEach((f) -> {
			if (!newFacts.contains(f) && !pinnedKeys.contains(f.key)) {
				tempTrie.deleteAll(f, context);
			}
		});
		
		newFacts.parallelStream().forEach((f) -> {
			tempTrie.insertAll(f.clone(), context);
		});
		
		context.commit();
		
		prevInput = key;
	}

	private void setFont(final Control c, final int sizeDelta) {
	    FontData[] data = c.getFont().getFontData();
	    
	    for (int i = 0; i < data.length; i++) {
	      data[i].setHeight(fontSize + sizeDelta);
	      data[i].setName(font);
	    }
	    
	    Font newFont = new Font(c.getDisplay(), data);
	    c.setFont(newFont);
	    c.pack();
	}
	
	private void setFontSize(int s) {
		exec.submit(() -> {
			mainTrie.insertAll(
					new Fact(String.format("#font-size %d",  s)), 
					context);
			tempTrie.insertAll(
					new Fact(String.format("#font-size %d",  s)), 
					context);
			context.commit();
		});

		fontSize = s;
		setFonts();
	}

	private void setFonts() {
		shell.setRedraw(false);
		try {
			setFont(reader, READER_FONT_DELTA);
		    ((GridData)reader.getLayoutData()).heightHint = 
		    	getReaderSize(reader);
			setFont(storeButton, 2);
			setFont(tree, 0);
			setFont(flash, 2);
			shell.layout(true, true);
		} finally {
			shell.setRedraw(true);
		}
	}
	
	private Stream<Node> sortResults() {
		final Set<Node> ns = tempTrie.root.getAll();
		ns.parallelStream().forEach((n) -> n.updateScore(reader.currentInput()));

		return ns.parallelStream().sorted((l, r) -> l.compareScoreTo(r))
			.sequential();
	}

	private void storeInput() {		
		final Fact f = new Fact(reader.currentInput());
    	exec.submit(() -> {    		
    		mainTrie.insertAll(f, context);			
			tempTrie.insertAll(f.clone(), context);
			context.commit();
	
			final Stream<Node> ns = sortResults();
			
			display.asyncExec(() -> {
	    		updateResults(ns);
				reader.checkPerfectMatch(f.key);
				
				if (factItems.containsKey(f)) {
					tree.select(factItems.get(f));
				}
				
				if (Key.next(f.key, 0).equals("#caption")) {
					updateCaption();
				}
		
				if (f.key.indexOf("#flash") > -1) {
					updateFlash();
				}

				if (Key.next(f.key, 0).equals("#font")) {
					updateFont();
					setFonts();
				}
		
				if (Key.next(f.key, 0).equals("#font-size")) {
					updateFontSize();
					setFonts();
				}	
				
				if (Key.next(f.key, 0).equals("#time-format ")) {
					context.load(mainTrie);
				}
			});
		});	
	}

	private void unpinTreeHeaders(final Collection<TreeItem> is) {
		for (final TreeItem ti: is) {
			if (ti.getItemCount() > 0) {
				boolean anyUnpinned = false;
				for (final TreeItem ci: ti.getItems()) {
					if (!ci.getChecked()) {
						anyUnpinned = true;
						break;
					}
				}
				
				if (anyUnpinned) {
					ti.setChecked(false);
					pinnedKeys.remove(((HasKey)ti.getData()).key());
				}
			}
		}
	}

	private void updateCaption() {
	    final String caption = getCaption();
	    final String title = String.format("Albaum v%d", Albaum.version());
	    
	    shell.setText((caption == null) 
	    	? title 
	    	: String.format("%s | %s", title, caption));		
	}

	private void updateFlash() {
		final Set<Fact> ts = mainTrie.root.findAllFacts("#flash ");

		if (ts.size() == 0) {
			flash.setText("");
		} else {
			final Iterator<Fact> i = ts.iterator();
			
			for (int skips = 
				ThreadLocalRandom.current().nextInt(ts.size()); 
				skips > 0; 
				i.next(), skips--);
		
			final Fact f = (Fact)i.next();
			final String k = f.key.replace("#flash", "").replace("  ", " ");
			if (Key.strip(k).isEmpty() && ts.size() > 1) {
				updateFlash();
			} else {	
				flash.setText(f.key.replace("#flash", "").replace("  ", " "));
			}
		}
	}
	
	private void updateFont() {
		final Fact f = mainTrie.root.findFirstFact("#font ");
		if (f != null) {
			font = f.key.replaceAll("#font ", "");
		}
	}
	
	private void updateFontSize() {
		final Fact f = mainTrie.root.findFirstFact("#font-size ");
		if (f != null) {
			fontSize = Integer.valueOf(f.key.replaceAll("#font-size ", ""));
		}
	}
	
	public void updateResults(final Stream<Node> ns) {
		factItems.clear();		
		tree.setRedraw(false);
		
		try {
			tree.removeAll();
			
			ns.forEach((n) -> {
				for (final Fact t: n.facts) {
					if (!factItems.containsKey(t)) {
						newItem(tree, n);
						break;
					}
				}
			});
		} finally {
			tree.setRedraw(true);
		}
	}

	private void updateTries(final Fact f, final String k) {
		exec.submit(() -> {
			tempTrie.deleteAll(f, context);
			mainTrie.deleteAll(f.prototype, context);			
			tempTrie.insertAll(f.clone(k), context);
			mainTrie.insertAll(f.prototype.clone(k), context);
			context.commit();
		
			final Stream<Node> ns = sortResults();			
			display.asyncExec(() -> {
				updateResults(ns);
				reader.checkPerfectMatch(k);
			});
		});
	}
}
