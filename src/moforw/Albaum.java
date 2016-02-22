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

import java.time.Instant;

public final class Albaum {
	public static final Instant nullTime = Instant.ofEpochMilli(0);

	public static final int VERSION[] = {1, 0, 1};
	
	public static int version() {
	    return VERSION[0] * 100 + VERSION[1] * 10 + VERSION[2];
	}
	
	private Albaum() { }
}
