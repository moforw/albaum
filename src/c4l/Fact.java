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

import static org.junit.Assert.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.Test;

public class Fact implements Comparable<Fact>, HasKey {	
	public static Instant now() {
		return Instant.now().truncatedTo(ChronoUnit.MINUTES);
	}	
	
	public final Instant createdAt;
	public final String key;
	public final Set<Node> nodes = new ConcurrentSkipListSet<>();
	public final Fact previousVersion;
	public final Fact prototype;
	public final int version;
	
	public Fact(final String k, final Instant ca) {
		key = k;
		createdAt = ca;
		prototype = null;
		previousVersion = null;
		version = 1;
	}

	public Fact(final String k) {
		this(k, now());
	}
	
	public Fact(final Fact p, final String k) {
		createdAt = now();
		prototype = p;
		key = k;
		previousVersion = p;
		version = p.version + 1;
	}
	
	
	public Fact(final Fact p) {
		createdAt = p.createdAt;
		prototype = p;
		key = p.key;
		previousVersion = p.previousVersion;
		version = p.version;
	}
	
	public Fact(final JsonObject o, final Context cx) {
		createdAt = cx.parseTimeJS(o.getString("createdAt"));
		key = o.getString("key");
		prototype = null;
		
		final JsonValue pv = o.get("previousVersion");
		
		previousVersion = (pv == JsonValue.NULL) 
			? null 
			: new Fact(o.getJsonObject("previousVersion"), cx);
		
		version = o.getInt("version");
	}

	public Fact clone(final String k) {
		return new Fact(this, k);
	}

	public Fact clone() {
		return new Fact(this);
	}
	
	@Override
	public int compareTo(Fact other) {	
		int res = key.compareToIgnoreCase(other.key);
		
		if (res != 0) {
			return res;
		}
				
		return other.createdAt.compareTo(createdAt);
	}
			
	@Override
	public String key() {
		return key;
	}
		
	@Override
	public String toString() {
		return String.format("'%s':%d", key, version);
	}
		
	public static class Tests {		
		@Test
		public void testClone() {
			Fact f = new Fact("abc def ghi");
			assertEquals(0, f.compareTo(f.clone()));
		}
	}
}
