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

import java.io.PrintStream;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

public class Node implements Comparable<Node>, HasKey {
	public final Map<Character, Node> childNodes = 
		new ConcurrentSkipListMap<>();
	public final Instant insertedAt;
	public final String key;
	public final int level;
	public final Node previousNode;
	public final Trie trie;
	public final Set<Fact> facts = new ConcurrentSkipListSet<>();
	
	public Node(final Trie t, final Node p, final String k) {
		trie = t;
		previousNode = p;
		level = (p == null) ? 0 : p.level + 1;
		key = k;
		insertedAt = Instant.now();
	}

	public boolean deleteFact(final Fact f, final Context cx) {
		if (facts.contains(f)) {
			cx.addChange(new Change.Delete(this, f));
			f.nodes.remove(this);
			facts.remove(f);
			return true;
		}
		
		return false;
	}

	public int compareScoreTo(final Node other) {
		int res = Integer.valueOf(other.score.get())
			.compareTo(Integer.valueOf(score.get()));
		
		if (res != 0) {
			return res;
		}
		
		return compareTo(other);
	}

	@Override
	public int compareTo(final Node other) {	
		int res = key.compareToIgnoreCase(other.key);
		
		if (res != 0) {
			return res;
		}
				
		return other.insertedAt.compareTo(insertedAt);
	}
		
	public Node extend() {
		Node n = this;
		
		while (n.childNodes.size() == 1) {
			n = n.childNodes.values().iterator().next();
		}
		
		if (n.getFirstFact() == null) {
			return this;
		}
		
		return n;
	}

	public Node find(final String key) {
		Node n;
		int i;

		for (n = this, i = 0; i < key.length() && n != null; i++) {
			n = n.getChild(key.charAt(i));
		}
		
		return n;
	}

	public Set<Fact> findAllFacts(final String key) {
		final Node n = find(key);
		return (n != null && n.key.compareToIgnoreCase(key) == 0)
			? n.getAllFacts()
			: new ConcurrentSkipListSet<>();
	}

	public Fact findFirstFact(final String key) {
		final Node n = find(key);
		return (n != null && n.key.compareToIgnoreCase(key) == 0)
			? n.getFirstFact()
			: null;
	}
	
	private void findEndNodes(final String key, final int minMatch, final Set<Node> res) {	
		Node n = this;
		int i = 0;
		String k = key;
		
		while (i < k.length()) {
			char c = k.charAt(i);
			if (n != null && n.hasChild(c)) {
				n = n.getChild(c);				
				i++;
			} else {				
				if (n.level >= minMatch) {
					res.add(n);
				}
				
				while (i > 0 && !Key.SPLIT.contains(k.charAt(i))) { 
					i--;					
					n = n.previousNode;
				}

				i++;				
				
				if (n != this) {
					findEndNodes(k.substring(i), minMatch, res);
				}
				
				while (i < k.length() && !Key.SPLIT.contains(k.charAt(i))) { 
					i++; 
				}
				
				while (i < k.length() && Key.SPLIT.contains(k.charAt(i))) { 
					i++; 
				}
				
				if (i < k.length()) {
					k = k.substring(i);
					i = 0;

					findEndNodes(k.substring(i), minMatch, res);						
				}
			}
		}
		
		if (n.level >= minMatch) {
			res.add(n);
		}
	}
	
	public Set<Node> findEndNodes(final String key, final int minMatch) {
		final Set<Node> res = new ConcurrentSkipListSet<>();
		findEndNodes(key, minMatch, res);
		return res;
	}
	
	public Fact findFact(final Fact t) {
		for (final Fact tt: facts) {
			if (tt.compareTo(t) == 0) {
				return tt;
			}
		}
		return null;
	}
	
	@Override
	public String key() {
		return key;
	}

	public void updateScore(final String k) {
		score.set(facts.size() * (1 + Key.score(key,  k)));
	}

	public void updateScore(final String k, final Set<Node> ns) {
		score.set(ns
			.parallelStream()
			.map((n) -> {
				if (n == this) {
					return 0;
				} else {
					final Set<Fact> fs = getAllFacts();
					fs.retainAll(n.getAllFacts());					
					return fs.size();
				}
			})
			.reduce(0, (a, b) -> a + b) * (1 + Key.score(key,  k)));
	}
		
	public Set<Node> getAll() {
		return getAll(new ConcurrentSkipListSet<>());
	}
		
	public Set<Fact> getAllFacts(final Set<Fact> res) {
		res.addAll(facts);
		childNodes.values().parallelStream().forEach((n) -> n.getAllFacts(res));
		return res;
	}
	
	public Set<Fact> getAllFacts() {
		return getAllFacts(new ConcurrentSkipListSet<>());
	}

	public Node getChild(final char c) {
		return childNodes.get(Character.toLowerCase(c));
	}
	
	public Fact getFirstFact() {
		if (!facts.isEmpty()) {
			return facts.iterator().next();
		}
		
		for (final Node n: childNodes.values()) {
			Fact res = n.getFirstFact();
			
			if (res != null) {
				return res;
			}
		}
		
		return null;
	}

	public boolean hasChild(final char c) {
		return childNodes.containsKey(Character.toLowerCase(c));
	}

	public void insertChild(final char c, final Node n) {
		childNodes.put(Character.toLowerCase(c), n);
	}	

	public boolean insertFact(final Fact f, final Context cx) {
		if (!facts.contains(f)) {
			cx.addChange(new Change.Insert(this, f));
			f.nodes.add(this);
			facts.add(f);
			return true;
		}
		
		return false;
	}

	public void print(final PrintStream out, int level) {
		for (Map.Entry<Character, Node> e: childNodes.entrySet()) {
			for (int i = 0; i < level; i++) {
				out.write('-');
			}
			out.write(e.getKey());
			out.printf(" (%d/%d)", 
					e.getValue().childNodes.size(), 
					e.getValue().facts.size());
			out.write('\n');
			e.getValue().print(out, level + 1);
		}
	}
	
	public int score() {
		return score.get();
	}
	
	private final AtomicInteger score = new AtomicInteger(0);
	
	private Set<Node> getAll(final Set<Node> res) {
		if (!facts.isEmpty()) {
			res.add(this);
		}
		
		childNodes.values().parallelStream().forEach((n) -> n.getAll(res));
		return res;
	}
}