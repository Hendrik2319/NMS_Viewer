package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.BorderLayout;

import javax.swing.JScrollPane;

import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.DiscoveryData.AvailableData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.DiscoveryData.StoreData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveViewer;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.SaveGameView.SaveGameViewTabPanel;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.SimplifiedTable;

class DiscoveryDataPanels {

	static class AvailableDataPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = 2870833302184314416L;
	
		public AvailableDataPanel(SaveGameData data) {
			super(data);
			
			DDATableModel tableModel = new DDATableModel();
			SimplifiedTable table = new SimplifiedTable("DDATable",tableModel,true,SaveViewer.DEBUG,true);
			JScrollPane tableScrollPane = new JScrollPane(table);
			
			add(tableScrollPane,BorderLayout.CENTER);
		}
	
		private enum DDAColumnID implements TableView.SimplifiedColumnIDInterface {
			TSrec ("TSrec" ,   Long.class, 50,-1, 80, 80), //[81, 161, 91, 135, 139]
			DD_UA ("DD_UA" , String.class, 50,-1,160,160),
			DD_DT ("DD_DT" , String.class, 50,-1, 90, 90),
			DD_VP0("DD_VP0", String.class, 50,-1,130,130),
			DD_VP1("DD_VP1", String.class, 50,-1,130,130);
			
			private TableView.SimplifiedColumnConfig columnConfig;
			
			DDAColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				columnConfig = new TableView.SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			@Override public TableView.SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
		}
	
		private class DDATableModel extends TableView.SimplifiedTableModel<AvailableDataPanel.DDAColumnID> {
	
			protected DDATableModel() {
				super(DDAColumnID.values());
			}
	
			@Override
			public int getRowCount() {
				return data.discoveryData.availableData.size();
			}
	
			@Override
			public Object getValueAt(int rowIndex, int columnIndex, AvailableDataPanel.DDAColumnID columnID) {
				AvailableData availableData = data.discoveryData.availableData.get(rowIndex);
				if (availableData==null) return null;
				switch(columnID) {
				case TSrec : if (availableData.TSrec ==null) return -1; else return availableData.TSrec;
				case DD_UA : if (availableData.DD.UA ==null) return ""; else return availableData.DD.UA.getExtendedSigBoostCode();
				case DD_DT : if (availableData.DD.DT ==null) return ""; else return availableData.DD.DT;
				case DD_VP0: if (availableData.DD.VP0==null) return ""; else return availableData.DD.VP0;
				case DD_VP1: if (availableData.DD.VP1==null) return ""; else return availableData.DD.VP1;
				}
				return null;
			}
		}
	}

	static class StoredDataPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = 6619075068331735784L;
	
		public StoredDataPanel(SaveGameData data) {
			super(data);
			
			DDSTableModel tableModel = new DDSTableModel();
			SimplifiedTable table = new SimplifiedTable("DDSTable",tableModel,true,SaveViewer.DEBUG,true);
			JScrollPane tableScrollPane = new JScrollPane(table);
			
			add(tableScrollPane,BorderLayout.CENTER);
		}
	
		private enum DDSColumnID implements TableView.SimplifiedColumnIDInterface {
			DD_UA ("DD_UA" , String.class, 50,-1,160,160),
			DD_DT ("DD_DT" , String.class, 50,-1, 90, 90),
			DD_VP0("DD_VP0", String.class, 50,-1,130,130),
			DD_VP1("DD_VP1", String.class, 50,-1,130,130),
			DM     ("DM"     , String.class, 20,-1, 40, 40),
			DM_CN  ("DM_CN"  , String.class, 50,-1, 80, 80),
			OWS_LID("OWS_LID", String.class, 50,-1,120,120),
			OWS_UID("OWS_UID", String.class, 50,-1,120,120),
			OWS_USN("OWS_USN", String.class, 50,-1, 80, 80),
			OWS_TS ("OWS_TS" ,   Long.class, 50,-1, 80, 80),
			RID    ("RID"    , String.class, 50,-1,350,350);
			
			private TableView.SimplifiedColumnConfig columnConfig;
			
			DDSColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				columnConfig = new TableView.SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			@Override public TableView.SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
		}
	
		private class DDSTableModel extends TableView.SimplifiedTableModel<StoredDataPanel.DDSColumnID> {
	
			protected DDSTableModel() {
				super(DDSColumnID.values());
			}
	
			@Override
			public int getRowCount() {
				return data.discoveryData.storeData.size();
			}
	
			@Override
			public Object getValueAt(int rowIndex, int columnIndex, StoredDataPanel.DDSColumnID columnID) {
				StoreData storeData = data.discoveryData.storeData.get(rowIndex);
				if (storeData==null) return null;
				switch(columnID) {
				case DD_UA : if (storeData.DD.UA ==null) return ""; else return storeData.DD.UA.getExtendedSigBoostCode();
				case DD_DT : if (storeData.DD.DT ==null) return ""; else return storeData.DD.DT;
				case DD_VP0: if (storeData.DD.VP0==null) return ""; else return storeData.DD.VP0;
				case DD_VP1: if (storeData.DD.VP1==null) return ""; else return storeData.DD.VP1;
				case DM     : if (storeData.DM     ==null) return ""; else return storeData.DM     ;
				case DM_CN  : if (storeData.DM_CN  ==null) return ""; else return storeData.DM_CN  ;
				case OWS_LID: if (storeData.OWS_LID==null) return ""; else return storeData.OWS_LID;
				case OWS_UID: if (storeData.OWS_UID==null) return ""; else return storeData.OWS_UID;
				case OWS_USN: if (storeData.OWS_USN==null) return ""; else return storeData.OWS_USN;
				case OWS_TS : if (storeData.OWS_TS ==null) return -1; else return storeData.OWS_TS ;
				case RID    : if (storeData.RID    ==null) return ""; else return storeData.RID    ;
				}
				return null;
			}
		}
	}
	
}