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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

public abstract class Key {
	public static String next(final String key, final int i) {
		int j, k;
		for (j = i; j < key.length() && SPLIT.contains(key.charAt(j)); j++);
		for (k = j; k < key.length() && !SPLIT.contains(key.charAt(k)); k++);
		return key.substring(j, k);
	}
	
	public static int score(final String k1, final String k2) {
		return
		(int)split(k1, true)
			.parallelStream()
			.map((kk) -> strip(kk))
			.filter((kk) -> k2.indexOf(kk) > -1)
			.count() +
		(int)split(k2, true)
			.parallelStream()
			.map((kk) -> strip(kk))
			.filter((kk) -> k1.indexOf(kk) > -1)
			.count();
	}
	
	public static Set<String> split(final String key, boolean includeFull) {
		final ArrayList<String> ks = new ArrayList<String>();
		boolean isQuoted = false;
		final StringBuilder buf = new StringBuilder();
				
		for (int i = 0; i < key.length(); i++) {
			if (!isQuoted && SPLIT.contains(key.charAt(i))) {
				while(i < key.length() && SPLIT.contains(key.charAt(i))) {
					buf.append(key.charAt(i));
					i++;
				}
												
				if (buf.length() > 0) {
					ks.add(buf.toString());
					buf.setLength(0);
				}				
				
				if (i < key.length()) {
					i--;
				}
			} else if (key.charAt(i) == '"' && (i == key.length() - 1 || key.charAt(i + 1) != '"')) {
				isQuoted = !isQuoted;
			} else if (key.charAt(i) == '"') {
				buf.append('"');
				i++;
			} else {
				buf.append(key.charAt(i));
			}
			
			if (i == key.length() - 1 && buf.length() > 0) {
				ks.add(buf.toString());
				buf.setLength(0);
			}			
		}
				
		final Set<String> res = new TreeSet<String>();
				
		for (int i = 0; i < ks.size(); i++) {
			final StringBuilder kbuf = new StringBuilder();
			
			for (int j = i; j < ks.size(); j++) {
				kbuf.append(ks.get(j));
				
				if (i == 0 && (includeFull || j < ks.size() - 1)) {
					res.add(kbuf.toString());
				}
			}
			
			if (i > 0 && i < ks.size() - 1) {
				res.add(kbuf.toString());
			}
		}
		
		for (int i = 0; i < ks.size(); i++) {			
			for (int j = i + 2; j < ks.size(); j++) {
				final StringBuilder kbuf = new StringBuilder();
				kbuf.append(ks.get(i));
				for (int k = j; k < ks.size(); k++) {
					kbuf.append(ks.get(k));
				}
				res.add(kbuf.toString());
			}
		}
		
		return res;
	}

	public static Set<String> split(final String key) {
		return split(key, false);
	}
	
	public static String strip(final String k) {
		int i;
		for (i = 0; i < k.length() && SPLIT.contains(k.charAt(i)); i++);
		
		int j;
		for (j = Math.max(0, k.length() - 1); j >= 0 && SPLIT.contains(k.charAt(j)); j--);
		
		return (j >= i) ? k.substring(i, j + 1) : "";
	}
	
	public static Set<Character> newCharSet(final String cs) {
		final Set<Character> res = new HashSet<Character>();
		
		for (final char c: cs.toCharArray()) {
			res.add(c);
		}
		
		return res;
	}
	
	public static final Set<Character> SPLIT = 
		newCharSet(",;' .!");
	
	public static class Tests {		
		@Test
		public void testNext() throws InterruptedException {
			assertEquals("abc", next("abc", 0));
			assertEquals("abc", next("abc def ghi", 0));
			assertEquals("def", next("abc def ghi", 3));
			assertEquals("ghi", next("abc def ghi", 7));
		}
		
		@Test
		public void testSplitSingle() {
			Set<String> ks = Key.split("abc ", true);
			assertEquals(1, ks.size());						
			assertTrue(ks.contains("abc "));
		}		

		@Test
		public void testSplit() {
			Set<String> ks = Key.split("abc def,ghi;jkl.mno!");

			assertEquals(13, ks.size());
						
			assertTrue(ks.contains("abc "));
			assertTrue(ks.contains("abc def,"));
			assertTrue(ks.contains("abc def,ghi;"));
			assertTrue(ks.contains("abc def,ghi;jkl."));
			assertTrue(ks.contains("abc ghi;jkl.mno!"));
			assertTrue(ks.contains("abc jkl.mno!"));
			assertTrue(ks.contains("abc mno!"));
			assertTrue(ks.contains("def,ghi;jkl.mno!"));
			assertTrue(ks.contains("def,jkl.mno!"));
			assertTrue(ks.contains("def,mno!"));
			assertTrue(ks.contains("ghi;jkl.mno!"));
			assertTrue(ks.contains("ghi;mno!"));
			assertTrue(ks.contains("jkl.mno!"));
		}		

		@Test
		public void testQuote() {
			Set<String> ks = Key.split("\"abc def,ghi;jkl\".mno!pqr");
						
			assertEquals(4, ks.size());
			assertTrue(ks.contains("abc def,ghi;jkl."));
			assertTrue(ks.contains("abc def,ghi;jkl.mno!"));
			assertTrue(ks.contains("mno!pqr"));
			assertTrue(ks.contains("abc def,ghi;jkl.pqr"));
		}		
	}
}
