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

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;

public class Log {
	public final Context context;

	public Log(final Context cx, final Path p) {
		context = cx;
		filePath = p;
		
		if (!p.toFile().isFile()) {
			initFact("#caption Not your mother's todo list");
			initFact("#done ");
			initFact("#flash ");
			initFact("#font DejaVu Sans Mono");
			initFact("#font-size 10");
			initFact("#time-format yyyy-MM-dd HH:mm");
			initFact("#todo ");
		}
	}
	
	public void commitFact(final Fact f, final Change.Type ct, final JsonGenerator json, final boolean recursive) {		
		json
			.write("key", f.key)
			.write("createdAt", context.formatJS(f.createdAt))
			.write("version", f.version);	
		
		switch(ct) {
			case DELETE: {
				json.write("deleted", true);
				json.writeNull("previousVersion");
				break;
			}			
			case INSERT: {
				if (!recursive || f.previousVersion == null) {
					json.writeNull("previousVersion");
				} else {
					json.writeStartObject("previousVersion");
					commitFact(f.previousVersion, ct, json, false);
					json.writeEnd();
				}
				break;
			}
			default:
				throw new RuntimeException("Unknown change type: " + ct);
		}
	}

	public void commitFact(final Fact f, final Change.Type ct) {
		FileWriter fw;
		try {
			fw = new FileWriter(filePath.toString(), true);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		try(JsonGenerator json = Json.createGenerator(fw)) {
			json.writeStartObject();
			commitFact(f, ct, json, true);
			json.writeEnd();
			
			try {
				fw.write('\n');
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public void initFact(final String k) {
		commitFact(new Fact(k, Albaum.nullTime), Change.Type.INSERT);
	}
	
	public void load(final Trie s) {
		try (InputStream in = Files.newInputStream(filePath);
		    BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
		    String line;
		    final Set<Fact> facts = new TreeSet<Fact>();
		    
		    while ((line = reader.readLine()) != null) {
		    	if (line.compareTo(" ") > 0) {
			    	try (JsonReader json = Json.createReader(new StringReader(line))) {
				    	final JsonObject o = json.readObject();
				    	final Fact f = new Fact(o, context);
				    	if (o.get("deleted") == JsonValue.TRUE) {
				    		facts.remove(f);
				    	} else {
				    		facts.add(f);
				    	}				    	
			    	}
		    	}
		    }
		    
		    facts.parallelStream().forEach((f) -> s.insertAll(f, context));
    		context.commit();			    	
		} catch (IOException e) {
		    throw new RuntimeException(e);
		}
	}

	private final Path filePath;
}
