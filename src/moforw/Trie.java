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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;

import org.junit.Test;

public class Trie {
	public static Set<String> newStringSet(final String...items) {
		final Set<String> res = new HashSet<>();
		res.addAll(Arrays.asList(items));		
		return res;
	}
		
	public static final Set<String> SPECIALS = 
		newStringSet("#caption", "#font", "#font-size", "#time-format");
	
	public final Node root = new Node(this, null, "");

	public Trie(final Log l) {
		if (l != null) {
			l.load(this);
		}

		log = l;
	}
	
	public void clear() {
		root.childNodes.clear();
	}

	public void commitFact(final Fact f, final Change.Type ct) {		
		if (log != null) {
			log.commitFact(f,  ct);
		}
	}
	
	public void deleteAll(final Fact t, final Context cx) {		
		final List<Node> ns = new ArrayList<>();
		ns.addAll(t.nodes);
		ns.parallelStream().forEach((n) -> {
			if (n.trie != this) {
				throw new RuntimeException("Delete from wrong trie!");
			}
			
			n.deleteFact(t, cx);
		});
	}

	public Node find(final String key) {
		return root.find(key);
	}
	
	public Set<Fact> findAll(final String key, final int minMatch) {	
		final Set<Node> nss = root.findEndNodes(key, minMatch);
		final Set<Fact> res = new ConcurrentSkipListSet<>();
		
		if (nss.isEmpty()) {
			return res;
		}
		
		nss
			.parallelStream()
			.forEach((n) -> n.updateScore(key, nss));	
		
		nss
			.parallelStream()
			.sorted(Node::compareScoreTo)
			.findFirst()
			.get()
			.getAllFacts(res);
	
		return res;
	}
		
	public Node insert(final String key, final Fact t, final Context cx) {				
		Node n;
		Node pn;
		int i;
		char c;
		
		final StringBuilder nkbuf = new StringBuilder();
		
		for (n = root, pn = n, i = 0; 
			 i < key.length() && n != null;) {
			c = key.charAt(i);
			pn = n;
			n = n.getChild(c);
			if (n != null) {
				nkbuf.append(c);
				i++;
			}
		}
		
		if (n == null) {
			for (n = pn;
		 	 	 i < key.length() && n != null;) {
				c = key.charAt(i);
				nkbuf.append(c);
				pn = n;
				n = new Node(this, pn, nkbuf.toString()); 
				pn.insertChild(c, n);
				i++;
			}			
		}
			
		if (n != null) {
			n.insertFact(t, cx);
		}
		
		return n;
	}		
		
	public Node insertAll(final Fact t, final Context cx) {				
		if (t.key.indexOf("#done") > -1) {
			final String tk = t.key.replace("#done", "#todo");
			final Node n = find(tk);
			
			if (n != null && n.level == tk.length()) {
				final Set<Node> ns = new ConcurrentSkipListSet<>();
				
				n.getAllFacts().parallelStream().forEach((tt) -> {
					deleteAll(tt, cx);
					final String ttk = tt.key
							.replace("#todo", "#done")
							.replace("#flash", "")
							.replace("  ", " ");
					ns.add(basicInsertAll(tt.clone(ttk), cx));	
					
				});
				
				if (ns.size() == 1) {
					return ns.iterator().next();
				}
			}
		}
		
		final String fk = Key.next(t.key, 0);
		
		if (SPECIALS.contains(fk)) {
				Node n = find(fk);
				
				if (n != null) {
					Fact prev = null;
					
					for (final Fact tt: n.getAllFacts()) {						
						if (tt.key.equals(t.key)) {
							return n;
						}
						
						if (prev == null) {
							prev = tt;
						}
						
						deleteAll(tt, cx);
					}
					
					return basicInsertAll((prev == null) ? t : prev.clone(t.key), cx);
				}
		}
		
		return basicInsertAll(t, cx);
	}

	private final Log log;
	
	private Node basicInsertAll(final Fact t, final Context cx) {
		if (!t.createdAt.equals(Albaum.nullTime)) {
			basicInsertAll(String.format("#at %s", cx.format(t.createdAt)), t, cx);
		}

		return basicInsertAll(t.key, t, cx);
	}

	private Node basicInsertAll(final String k, final Fact t, final Context cx) {
		final Set<String> ks = Key.split(k);
		ks.parallelStream().forEach((kk) -> insert(kk, t, cx));
		return insert(k, t, cx);
	}

	public static class Tests {		
		@Test
		public void testAt() {
			Context cx = new Context();
			Trie s = new Trie(null);
			Fact f = new Fact("abc");
			s.insertAll(f, cx);
			cx.commit();
			
			assertTrue(s.find("#at " + cx.format(f.createdAt)).getAllFacts().contains(f));
		}
		
		@Test
		public void testCommitDelete() {
			Context cx = new Context();
			Trie s = new Trie(null);	
			Fact f = new Fact("abc");
			
			s.insertAll(f, cx);
			cx.commit();
			s.deleteAll(f, cx);
			cx.commit();
			cx.rollback();
			
			assertFalse(s.find("abc").facts.contains(f));
			assertTrue(cx.changes.isEmpty());
		}

		@Test
		public void testDup() {
			Context cx = new Context();
			Trie s = new Trie(null);
			Fact f = new Fact("abc");
			Node n = s.insertAll(f, cx);
			assertEquals(n, s.insertAll(new Fact("abc"), cx));	
		}

		@Test
		public void testExtend() {
			Context cx = new Context();
			Trie s = new Trie(null);
			s.insertAll(new Fact("abc"), cx);
			Node n2 = s.insertAll(new Fact("abcghi"), cx);
			s.insertAll(new Fact("abd"), cx);
			cx.commit();
			
			assertEquals(n2, s.find("abc").extend());
			assertEquals(s.find("ab"), s.find("ab").extend());
			assertEquals(s.find("abd"), s.find("abd").extend());
		}
		
		@SuppressWarnings("unused")
		private <T> void printSet(Set<T> s) {
			System.out.print("{");
			String sep = "";
			for (T ss: s) {
				System.out.print(sep);
				System.out.print(ss);
				sep = ", ";
			}
			System.out.print("}\n");			
		}
		
		@Test
		public void testFindAll() {
			Context cx = new Context();
			Trie t = new Trie(null);
			Set<Fact> fs = new TreeSet<>();
			
			Fact abc = new Fact("abc");
			fs.add(abc);
			Fact abc_def = new Fact("abc def");
			fs.add(abc_def);
			Fact def_ghi = new Fact("def ghi abc");
			fs.add(def_ghi);
			Fact def_abc = new Fact("def abc ghi");
			fs.add(def_abc);

			for (Fact f: fs) {
				t.insertAll(f, cx);
			}

			cx.commit();
			
			Set<Fact> res = t.findAll("abc",  2);
			
			assertEquals(3, res.size());
			assertTrue(res.contains(abc));
			assertTrue(res.contains(abc_def));
			assertTrue(res.contains(def_abc));
			
			res = t.findAll("abc def", 2);
			assertEquals(1, res.size());
			assertTrue(res.contains(abc_def));

			res = t.findAll("ghi", 2);
			assertEquals(1, res.size());
			assertTrue(res.contains(def_ghi));
		}

		@Test
		public void testGetAll() {
			Context cx = new Context();
			Trie s = new Trie(null);
			Set<Fact> fs = new TreeSet<>();
			Set<Node> ns = new TreeSet<>();
			
			for (String str: new String[] {
				"abc", "abcdef", "abcghi", "abcjklghi"}) {
				fs.add(new Fact(str));
			}
			
			for (Fact f: fs) {
				ns.add(s.insertAll(f, cx));
			}
			
			cx.commit();
			Set<Node> res = s.find("abc").getAll();
			assertTrue(res.containsAll(ns));
			res.removeAll(ns);
			assertEquals(0, res.size());
		}

		@Test
		public void testGetAllFacts() {
			Context cx = new Context();
			Trie s = new Trie(null);
			Set<Fact> fs = new TreeSet<>();
			
			for (String str: new String[] {
				"abc", "abcdef", "abcghi", "abcjklghi"}) {
				fs.add(new Fact(str));
			}
			
			for (Fact f: fs) {
				s.insertAll(f, cx);
			}
			
			cx.commit();
			Set<Fact> res = s.find("abc").getAllFacts();
			assertTrue(res.containsAll(fs));
		}
		
		@Test
		public void testKeys() {
			Context cx = new Context();
			Trie s = new Trie(null);
			Fact f = new Fact("abc def ghi");
			Node n = s.insertAll(f, cx);
			s.insertAll(f, cx);
			assertEquals(n, s.find("abc def ghi"));
			
			cx.commit();
			assertTrue(s.find("def ghi").facts.contains(f));			
			assertTrue(s.find("abc def ghi").facts.contains(f));			

			assertEquals(null, s.find("bc def ghi"));		
		}

		@Test
		public void testInsert() {
			Context cx = new Context();
			Trie s = new Trie(null);
			Fact f = new Fact("abc def ghi.");
			Node n = s.insert(f.key, f, cx);
			assertEquals(f.key, n.key);
		}

		@Test
		public void testInsertAll() {
			Context cx = new Context();
			Trie s = new Trie(null);
			Node n = s.insertAll(new Fact("AbC"), cx);
			
			assertNotNull(n);
			assertEquals("AbC", n.key);
			assertEquals(n, s.find("AbC"));
			assertNotEquals(null, s.find("a"));
			assertNotEquals(null, s.find("ab"));
			assertEquals(null, s.find("bc"));
			assertEquals("A", s.find("a").key);
			assertEquals("Ab", s.find("ab").key);
			assertEquals("AbC", s.find("abc").key);
		}
		
		@Test
		public void testRollbackInsert() {
			Context cx = new Context();
			Trie s = new Trie(null);	
			Fact f = new Fact("abc");
			
			s.insertAll(f, cx);
			cx.rollback();
			
			assertFalse(s.find("abc").facts.contains(f));
			assertTrue(cx.changes.isEmpty());
		}

		@Test
		public void testRollbackDelete() {
			Context cx = new Context();
			Trie s = new Trie(null);	
			Fact f = new Fact("abc");
			
			s.insertAll(f, cx);
			cx.commit();
			s.deleteAll(f, cx);
			cx.rollback();
			
			assertTrue(s.find("abc").facts.contains(f));
			assertTrue(cx.changes.isEmpty());
		}		
	}	
}
