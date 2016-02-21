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

//TODO move todo's to C4L Albaum

//TODO --- v100 ---

//TODO add tiling interface
///TODO auto adjust nr of cols in global grid layout
///TODO add tile class
///TODO show inactive tiles in low alpha
////TODO convert existing ui into Finder tile
////TODO add shortcut ctrl|f in Show menu
////TODO first shortcut focuses last used Finder if not already in Finder
////TODO second shortcut opens a new Finder
///TODO unified menu bar
////TODO update when view changes
////TODO use grid layout
///TODO max 8 (4 x 2) tiles
///TODO increase cols first, then rows
///TODO keep track of slotCount / tileCount
////TODO add shortcuts Ctrl1-8 to switch tile
////TODO updating font/font-size should affect all tiles

//TODO extract StoreButton from GUI

//TODO add ItemEditor class
///TODO copy code from readerPanel
///TODO add Cancel button

//TODO add Edit tile
///TODO add edit selection short cut (ctrl|e)
///TODO extract ReaderPanel class with buttons from GUI
////TODO use to show common prefix (if any) in input on top
////TODO use to edit tree items
///TODO selected items in list below
///TODO update items in realtime when prefix changes
///TODO add support for ctrl|i / ctrl|d in list

//TODO add undo facility
///TODO add label param to Context.commit()
///TODO add undo view
////TODO show commits in tree
///TODO ctrl|z to move up
///TODO fill stack while moving up, use to move down
////TODO clear on committ
////TODO ctrl|r to move down

//TODO add #show-score setting
//TODO set to false default
//TODO add 'Show Scores' item to View menu
///TODO update tries

//TODO add coloring support
///TODO 5 colors, green, yellow, red, blue & black
///TODO add 5 tiny toggle buttons in colors centered below store button
///TODO add #red, #blue etc to currentInput
///TODO index colored facts automagically under String.format("#%s %s", color, key)
///TODO black = all/none
////TODO prefix search key with #color if not black
///TODO Add color menu / alt|1-5
///TODO Launch new search task when color is modified
///TODO set text color in reader, tree item & flash

///TODO add support for secrets
////TODO add checked ctrl|t shortcut / 'Show Secre&ts' to viewMenu
////TODO rename HasKey to Labeled, key() to label()
////TODO add boolean param showSecrets
////TODO send in item.checked when asking for label
////TODO update all labels in tree when item is toggled
////TODO prefix secet with #secret, filter Key.next() if showSecrets is false
/////TODO 'maestro card pin #secret NNNN'
////TODO otherwise filter '#secret'
///TODO show secrets when editing item
///TODO don't register #secret or the following key in trie

//TODO add stemming of non-# keys
///TODO snowball2? other java implementations?

//TODO compress log file

//TODO add #my specials
///TODO add '-' to Key.SPLIT
///TODO add String Fact.value(), filter #secret
///TODO add '#my-name X'
///TODO add '#my-email X'
///TODO add '#my-email-password X'
///TODO add '#my-smtp-host X'
///TODO add '#my-smtp-port X'
///TODO add '#my-imap-host X'
///TODO add '#my-imap-port X'
///TODO add empty settings for completion in Log.init

//TODO add key specials
///TODO add '#my public-key X' special
///TODO add '#my private-key X' special
///TODO generate and commit on trie.load if not exists

//TODO add Trie.SECRETS
///TODO add '#my-email-password & #private-key"
///TODO automatically insert #secret if not exists

//TODO add system password
///TODO store pbkdf2 hash in tree
///TODO add login dialog
////TODO add Shift|Ctrl|L &Lock shortcut to Albaum menu
///TODO implement AES encryption of log

//TODO add export function
///TODO multi select facts, right click and select export
///TODO separate export dialog
////TODO field for prefix to add to exported facts

//TODO add import function
///TODO separate import dialog
////TODO field for prefix to add to imported facts

//TODO add ability to send selection via email
///TODO useful for shopping lists etc

//TODO add publish/subscribe of key spaces via email
///TODO add prefix option on publish and subscribe
///TODO remove if found in key

public class Main {		
	public static void main(final String[] args) {
		Context cx = new Context();
		GUI.run(cx);    
	}
}
