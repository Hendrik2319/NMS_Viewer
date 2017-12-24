package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;

import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Stats.StatValue;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveViewer;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.SaveGameView.SaveGameViewTabPanel;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.SimplifiedColumnConfig;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.SimplifiedColumnIDInterface;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.SimplifiedTable;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.SimplifiedTableModel;

class StatsPanel extends SaveGameViewTabPanel {
	private static final long serialVersionUID = -1541256209397699528L;
	
	private SimplifiedTable table;
	
	public StatsPanel(SaveGameData data) {
		super(data);
		
		table = new SimplifiedTable("StatsTable",true,SaveViewer.DEBUG,true);
		JScrollPane tableScrollPane = new JScrollPane(table);
		tableScrollPane.setPreferredSize(new Dimension(600, 500));
		
		Vector<String> statConfigs = new Vector<>();
		statConfigs.add("Global");
		statConfigs.add("All Planets");
		statConfigs.addAll(SaveGameView.convertVector(data.stats.planetStats, ps -> "Planet "+ps.planet));
		
		JComboBox<String> selector = new JComboBox<>(statConfigs);
		selector.addActionListener(e -> changeSelection( selector.getSelectedIndex() ));
			
		add(selector,BorderLayout.NORTH);
		add(tableScrollPane,BorderLayout.CENTER);
		
		changeSelection( selector.getSelectedIndex() );
	}

	private void changeSelection(int index) {
		switch (index) {
		case -1: table.setModel(new DefaultTableModel()); break;
		case  0: table.setModel(new StatsTableModel(data.stats.globalStats)); break;
		case  1: table.setModel(new DefaultTableModel()); break;
		default: table.setModel(new StatsTableModel(data.stats.planetStats.get(index-2).stats)); break;
		}
	}

	private enum StatsTableColumnID implements SimplifiedColumnIDInterface {
		ID         ("ID"         , String.class, 50,-1,120,120),
		Name       ("Name"       , String.class, 50,-1,210,210),
		IntValue   ("Int"        , Long.class  , 20,-1, 70, 70),
		FloatValue ("Float"      , Double.class, 20,-1, 70, 70),
		Denominator("Denominator", Double.class, 20,-1, 40, 40);
		
		private TableView.SimplifiedColumnConfig columnConfig;
		
		StatsTableColumnID() {
			columnConfig = new SimplifiedColumnConfig();
			columnConfig.name = toString();
		}
		StatsTableColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
			columnConfig = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
		}
		@Override public SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
	}

	private static class StatsTableModel extends SimplifiedTableModel<StatsPanel.StatsTableColumnID> {
		
		private Vector<StatValue> statsList;
		
		public StatsTableModel(Vector<StatValue> statsList) {
			super(new StatsPanel.StatsTableColumnID[]{ StatsTableColumnID.ID, StatsTableColumnID.Name, StatsTableColumnID.IntValue, StatsTableColumnID.FloatValue, StatsTableColumnID.Denominator });
			this.statsList = statsList;
		}
	
		@Override public int getRowCount() { return statsList.size(); }
	
		@Override
		public Object getValueAt(int rowIndex, int columnIndex, StatsPanel.StatsTableColumnID columnID) {
			StatValue statValue = statsList.get(rowIndex);
			
			if (statValue==null) return null;
			
			switch(columnID) {
			case ID  : return statValue.ID;
			case Name: if (statValue.knownID!=null) return statValue.knownID.fullName; else return statValue.ID;
			case IntValue: return statValue.IntValue;
			case FloatValue: return statValue.FloatValue;
			case Denominator: return statValue.Denominator;
			}
			return null;
		}
	
		@Override
		protected boolean isCellEditable(int rowIndex, int columnIndex, StatsPanel.StatsTableColumnID columnID) {
			return columnID==StatsTableColumnID.Name;
		}
	
		@Override
		protected void setValueAt(Object aValue, int rowIndex, int columnIndex, StatsPanel.StatsTableColumnID columnID) {
			if (columnID!=StatsTableColumnID.Name) { fireTableCellUpdate(rowIndex, columnIndex); return; }
			
			StatValue statValue = statsList.get(rowIndex);
			
			if (statValue.knownID==null || aValue==null) { fireTableCellUpdate(rowIndex, columnIndex); return; }
			
			statValue.knownID.fullName = aValue.toString();
			SaveViewer.saveKnownStatIDsToFile();
		}
	}
}