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

package c4l;

public abstract class Change {
	public enum Type {INSERT, DELETE};
	
	public final Node node;
	public final Fact fact;
	public final Type type;
	
	public Change(final Type ct, final Node n, final Fact t) {
		type = ct;
		node = n;
		fact = t;
	}
	
	public void commit() {
		if (node.key.compareToIgnoreCase(Key.strip(fact.key)) == 0) {
			node.trie.commitFact(fact, type);
		}
	}

	public void rollback() {
		onRollback();
	}

	protected abstract void onRollback();

	public static class Insert extends Change {
		public Insert(final Node n, final Fact t) {
			super(Type.INSERT, n, t);
		}
		
		@Override
		protected void onRollback() {
			fact.nodes.remove(node);
			node.facts.remove(fact);			
		}
	}

	public static class Delete extends Change {
		public Delete(final Node n, final Fact t) {
			super(Type.DELETE, n, t);
		}

		@Override
		protected void onRollback() {
			fact.nodes.add(node);
			node.facts.add(fact);			
		}
	}
}