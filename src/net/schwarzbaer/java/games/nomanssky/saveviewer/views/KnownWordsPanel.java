package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.BorderLayout;
import java.util.Locale;

import javax.swing.JScrollPane;

import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.KnownWords;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.KnownWords.KnownWord;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveViewer;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.SaveGameView.SaveGameViewTabPanel;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.SimplifiedTable;
import net.schwarzbaer.java.lib.gui.Tables;

class KnownWordsPanel extends SaveGameViewTabPanel {
	private static final long serialVersionUID = 7096092479075372171L;
	
	public KnownWordsPanel(SaveGameData data, KnownWords knownWords) {
		super(data);
		
		KnownWordsTableModel tableModel = new KnownWordsTableModel(knownWords);
		SimplifiedTable<KnownWordsTableColumnID> table = new SimplifiedTable<>("KnownWords",tableModel,true,SaveViewer.DEBUG,true);
		JScrollPane tableScrollPane = new JScrollPane(table);
		
		add(tableScrollPane,BorderLayout.CENTER);
	}

	private enum KnownWordsTableColumnID implements Tables.SimplifiedColumnIDInterface {
		WordID        ("ID"  , 50,-1,120,120),
		TranslatedWord("Word", 50,-1,100,100),
		Race          (""    , 20,-1, 70, 70);
	
		private Tables.SimplifiedColumnConfig columnConfig;
		
		KnownWordsTableColumnID() {
			columnConfig = new Tables.SimplifiedColumnConfig();
		}
		KnownWordsTableColumnID(String name, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
			columnConfig = new Tables.SimplifiedColumnConfig(name, String.class, minWidth, maxWidth, prefWidth, currentWidth);
		}
		@Override public Tables.SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
	}

	private static class KnownWordsTableModel extends Tables.SimplifiedTableModel<KnownWordsTableColumnID> {
	
		private static final int ADDITIONAL_ROWS = 2;
		private KnownWords knownWords;
		private int numberOfRaces;
		
		public KnownWordsTableModel(KnownWords knownWords) {
			super(new KnownWordsTableColumnID[]{ KnownWordsTableColumnID.WordID, KnownWordsTableColumnID.TranslatedWord });
			this.knownWords = knownWords;
			numberOfRaces = this.knownWords.wordCounts.length;
		}
		
		@Override
		public KnownWordsTableColumnID getColumnID(int columnIndex) {
			if (columnIndex<columns.length) return super.getColumnID(columnIndex);
			if (columnIndex<columns.length+numberOfRaces) return KnownWordsTableColumnID.Race;
			return null;
		}
		@Override public int getColumnCount() { return columns.length+numberOfRaces; }
		@Override public String getColumnName(int columnIndex) {
			if (columnIndex<columns.length) return super.getColumnName(columnIndex);
			if (columnIndex<columns.length+numberOfRaces) {
				switch(columnIndex-columns.length) {
				case 0: return "Gek";
				case 1: return "Vy'keen";
				case 2: return "Korvax";
				case 4: return "Atlas";
				default:
					return "Race "+(columnIndex-columns.length);
				}
			}
			return null;
		}
	
		@Override
		public int getRowCount() {
			return knownWords.wordList.size()+ADDITIONAL_ROWS;
		}
		
		@Override
		public int getUnsortedRowsCount() {
			return ADDITIONAL_ROWS;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex, KnownWordsPanel.KnownWordsTableColumnID columnID) {
			if (rowIndex<ADDITIONAL_ROWS) {
				switch(columnID) {
				case WordID: return rowIndex>0?"":String.format("%d different words", knownWords.wordList.size());
				case TranslatedWord: break;
				case Race:
					int race = columnIndex-columns.length;
					switch (rowIndex) {
					case 0: return String.format(Locale.ENGLISH,"%d (%1.1f%%)", knownWords.wordCounts[race], knownWords.wordCounts[race]*100.0f/knownWords.wordList.size());
					case 1: return (knownWords.wordList.size()-knownWords.wordCounts[race])+" left";
					}
				}
				return "";
			} else {
				KnownWord knownWord = knownWords.wordList.get(rowIndex-ADDITIONAL_ROWS);
				if (knownWord==null) return null;
				
				switch(columnID) {
				case WordID: return knownWord.word;
				case TranslatedWord: return "";
				case Race:
					int race = columnIndex-columns.length;
					return (race>=knownWord.races.length)?"???":(knownWord.races[race]?"known":"");
				}
			}
			return null;
		}
	}
}