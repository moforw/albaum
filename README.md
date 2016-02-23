# Albaum

## What
Albaum is kind of like a todo list, sort of like a wiki, a bit like Google; but different.

## Why
I had an itch to scratch, the itch of not finding a good place to put my todo lists, diaries, notes, links, passwords and other stuff I want to keep track of.

## How
You currently need to run a 64 bit Linux and have Java 8 installed to run Albaum. More platforms will be added as soon as I manage to find a sane way of doing multi platform Java builds. javax.swing deserves to die, I'd rather poke my eyes out and eat them; so SWT it is. Download the [Linux](https://github.com/moforw/albaum/blob/master/AlbaumLinux.jar?raw=true), or [Macos](https://github.com/moforw/albaum/blob/master/AlbaumMacos.jar?raw=true) version; put it in it's own folder somewhere and launch it. If right/double clicking it doesn't work, launching from a shell with 'java -jar Albaum.jar' will give you an error message describing the reason.

## Interface
The main interface consists of a reader, a tree viewer and a flash viewer; top to bottom. Ctrl|X takes you out of there instantly.

### Reader
The reader updates the tree viewer periodically when the input changes. The input is auto completed when possible, the auto completed part is automatically selected to allow for easy overwriting. New facts are created by pressing Ctrl|S or pushing the Store button. The input is displayed in bold font whenever it exactly matches a fact that's already in the tree. Since tabs are allowed in the input, you need to use Ctrl|Tab to move focus to the tree.

### Tree Viewer
The tree viewer shows the part of the tree that matches the current (uncompleted) input. Facts are scored by nr of links and grouped when appropriate. Facts can be edited by pressing Enter or double clicking in the tree, Ctrl|S saves changes and Escape discards them. The editor is auto completed like the Reader. Ctrl|A selects all facts in tree, Ctrl|N none; Ctrl|D deletes selection.

#### Pinning
Facts can be pinned by checking the checkbox next to the item. Pinned facts stay in the tree until unpinned, regardless of current reader input. Ctrl|P pins selection, Ctrl|U unpins.

### Flash Viewer
The flash viewer is periodically updated to view a random #flash fact.

## Facts
A fact represents a line of text, the time it was created, the current version and all edits made since the fact was first created.

### Unified Namespace
Albaum stores all facts in a single unified namespace, the only way to differentiate two facts is by their keys and/or time of creation. The bad news is that there are no hierarchies, the good news is that you won't miss them much. 

### Quoting
Word indexing can be temporarily disabled by putting part of the fact inside double quotes, the quoted part will only match itself literally. Two consecutive double quotes escapes the quoting mechanism and inserts a regular double quote.

## Tags
Tags are identifiers within facts prefixed with '#' that get special treatment.

### at
All facts are automatically indexed at the key '#at [time]', where [time] represents the time of creation; for easy filtering.

### todo/done
Whenever a fact containg a #done tag is stored, all matching #todo tags are converted to #done. If more than one #todo is converted, an additional #done fact is created for the entire operation.

### flash
Facts containing #flash tags are periodically searched for a random fact to show in the flash viewer. Enter '#flash' in the reader to see what's currently in the loop, store '#flash whatever' to add a message.

## Special Facts

### caption
Displays a message next to the window title, store '#caption whatever' to change.

### font
Sets the font, store '#font Arial' to change the font to 'Arial'.

### font-size
Sets the basic font, all font sizes are calculated from this offset. Store '#font-size 12' to set the font size offset to 12. You can use Ctrl|+ / Ctrl|- to increase/decrease. 

### time-format
Sets the time format used for converting time stamps to/from strings. This affects #at-indexing. Storing "#time-format MM/dd/yyyy hh:mm a" gives you US style time stamps.