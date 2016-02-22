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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Context {
	public final Queue<Change> changes = new ConcurrentLinkedQueue<>();
		
	public void addChange(final Change c) {
		changes.add(c);
	}
	
	public void commit() {
		for (final Change c: changes) {
			c.commit();
		}
		changes.clear();
	}
	
	public String format(final Instant i) {
		return i.equals(Albaum.nullTime) ? "" : timeFormat.format(i);
	}

	public String formatJS(final Instant i) {
		return i.equals(Albaum.nullTime) ? "" : timeFormatJS.format(i);
	}

	public void load(final Trie t) {
		final Fact f = t.root.findFirstFact("#time-format ");
		
		final String p = (f == null) 
			? "yyyy-MM-dd HH:mm" 
			: f.key.replace("#time-format ", "");
		
		timeFormat = DateTimeFormatter
			.ofPattern(p)
			.withZone(ZoneId.systemDefault());
	}
	
	public Instant parseTime(final String s) {		
		return s.equals("") ? Albaum.nullTime : Instant.from(timeFormat.parse(s));
	}

	public Instant parseTimeJS(final String s) {		
		return s.equals("") ? Albaum.nullTime : Instant.from(timeFormatJS.parse(s));
	}

	public void rollback() {
		for (final Change c: changes) {
			c.rollback();
		}
		changes.clear();
	}
	
	private final DateTimeFormatter timeFormatJS = timeFormat = DateTimeFormatter
			.ofPattern("yyyy-MM-dd HH:mm")
			.withZone(ZoneId.systemDefault());
	private DateTimeFormatter timeFormat;
}
