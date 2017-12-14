package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;

class GUI {
	
	static class EnumCheckBoxMenuItem<Key extends Enum<Key>, ActionCommand extends Enum<ActionCommand>> extends JCheckBoxMenuItem {
		private static final long serialVersionUID = -3010890115769695204L;
		
		Key key;
		ActionCommand actionCommand;

		protected EnumCheckBoxMenuItem(String label, ActionCommand actionCommand, Key key, boolean selected, ActionListener actionListener) {
			super(label);
			this.actionCommand = actionCommand;
			this.key = key;
			setSelected(selected);
			addActionListener(actionListener);
			setActionCommand(actionCommand.toString());
		}

		public static <K extends Enum<K>, CBMI extends JCheckBoxMenuItem> void setCheckBoxMenuItems(CBMI[] miArr, K key, K[] keys, ButtonGroup bg) {
			boolean nothingSelected = true;
			for (int i=0; i<keys.length; ++i) {
				boolean isSelected = key==keys[i];
				nothingSelected &= !isSelected;
				miArr[i].setSelected(isSelected);
				//System.out.printf("Set ECBMI[%8s]%s selected (%s)\r\n",keys[i],isSelected?"":" not",miArr[i].isSelected()==isSelected);
			}
			if (nothingSelected) bg.clearSelection();
		}
	}

}
