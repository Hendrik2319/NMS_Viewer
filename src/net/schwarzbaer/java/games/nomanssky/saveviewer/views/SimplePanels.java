package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.BorderLayout;

import javax.swing.JScrollPane;
import javax.swing.table.TableCellEditor;

import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.BaseBuildingObject;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.DiscoveryData.AvailableData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.DiscoveryData.StoreData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveViewer;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.SaveGameView.SaveGameViewTabPanel;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.SimplifiedTable;

class SimplePanels {
	
	static class BaseBuildingObjectsPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = 6246130206148705495L;

		public BaseBuildingObjectsPanel(SaveGameData data) {
			super(data);
			
			if (data.baseBuildingObjects!=null) {
				BBOTableModel tableModel = new BBOTableModel();
				SimplifiedTable table = new SimplifiedTable("BBOTable",tableModel,true,SaveViewer.DEBUG,true);
				JScrollPane tableScrollPane = new JScrollPane(table);
				
				add(tableScrollPane,BorderLayout.CENTER);
			}
		}

		private enum BBOColumnID implements TableView.SimplifiedColumnIDInterface {
			// [62, 130, 160, 130, 80, 190, 150, 150]
			Timestamp       ("Timestamp"       , String.class, 35,-1, 70, 70),
			ObjectID        ("ObjectID"        , String.class, 65,-1,130,130),
			GalacticAddress ("GalacticAddress" , String.class, 80,-1,160,160),
			RegionSeed      ("RegionSeed"      , String.class, 65,-1,130,130),
			UserData        ("UserData"        , String.class, 40,-1, 80, 80),
			Position        ("Position"        , String.class, 95,-1,190,190),
			Up              ("Up"              , String.class, 75,-1,150,150),
			At              ("At"              , String.class, 75,-1,150,150);
			
			private TableView.SimplifiedColumnConfig columnConfig;
			
			BBOColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				columnConfig = new TableView.SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			@Override public TableView.SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
		}
		
		private class BBOTableModel extends TableView.SimplifiedTableModel<BBOColumnID> {
	
			protected BBOTableModel() {
				super(BBOColumnID.values());
			}
	
			@Override
			public int getRowCount() {
				return data.baseBuildingObjects.length;
			}
	
			@Override
			public Object getValueAt(int rowIndex, int columnIndex, BBOColumnID columnID) {
				BaseBuildingObject bbo = data.baseBuildingObjects[rowIndex];
				if (bbo==null) return null;
				switch(columnID) {
				case Timestamp      : if (bbo.timestamp      ==null) return ""; else return SaveGameData.timestampToString(bbo.timestamp);
				case ObjectID       : if (bbo.objectID       ==null) return ""; else return bbo.objectID;
				case GalacticAddress: if (bbo.galacticAddress==null) return ""; else return bbo.galacticAddress.getCoordinates();
				case RegionSeed     : if (bbo.regionSeed     ==null) return ""; else return String.format("0x%016X", bbo.regionSeed);
				case UserData       : if (bbo.userData       ==null) return ""; else return String.format("0x%08X" , bbo.userData  );
				case Position       : if (bbo.position       ==null) return ""; else return bbo.position.toString("%1.2f");
				case Up             : if (bbo.up             ==null) return ""; else return bbo.up      .toString("%1.4f");
				case At             : if (bbo.at             ==null) return ""; else return bbo.at      .toString("%1.4f");
				
				}
				return null;
			}
		}
	}
	
	static class PersistentPlayerBasesPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = -632703090899520348L;

		public PersistentPlayerBasesPanel(SaveGameData data) {
			super(data);
			// TODO Auto-generated constructor stub
		}

	}
	
	static class BlueprintsPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = -3032632553321731912L;
		enum BlueprintType { KnownProductBlueprints, KnownTechBlueprints }
		
		private final SimplifiedTable table;
		private final BlueprintsTableModel tableModel;
		private final BlueprintType type;
		
		public BlueprintsPanel(SaveGameData data, BlueprintType type, String tableDebugLabel) {
			super(data);
			this.type = type;
			
			GeneralizedID[] blueprints = null;
			switch(type) {
			case KnownProductBlueprints: blueprints=data.knownBlueprints.products; break;
			case KnownTechBlueprints   : blueprints=data.knownBlueprints.technologies; break;
			}
			if (blueprints == null) {
				tableModel = null;
				table = null;
				return;
			}
			
			tableModel = new BlueprintsTableModel(blueprints);
			table = new SimplifiedTable(tableDebugLabel,tableModel,true,SaveViewer.DEBUG,true);
			JScrollPane tableScrollPane = new JScrollPane(table);
			
			add(tableScrollPane,BorderLayout.CENTER);
		}
		
		@Override
		public void updateContent() {
			TableCellEditor cellEditor = table.getCellEditor();
			if (cellEditor!=null) cellEditor.stopCellEditing();
			tableModel.fireTableUpdate();
		}

		private enum BlueprintsColumnID implements TableView.SimplifiedColumnIDInterface {
			ID    ("ID"    , String.class, 100,-1,120,120),
			Label ("Label" , String.class, 200,-1,220,220);
			
			private TableView.SimplifiedColumnConfig columnConfig;
			
			BlueprintsColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				columnConfig = new TableView.SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			@Override public TableView.SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
		}
	
		private class BlueprintsTableModel extends TableView.SimplifiedTableModel<BlueprintsColumnID> {

			private GeneralizedID[] blueprints;

			protected BlueprintsTableModel(GeneralizedID[] blueprints) {
				super(BlueprintsColumnID.values());
				this.blueprints = blueprints;
			}

			@Override public int getRowCount() { return blueprints.length; }
			@Override public Object getValueAt(int rowIndex, int columnIndex, BlueprintsColumnID columnID) {
				switch(columnID) {
				case ID   : return blueprints[rowIndex].id;
				case Label: return blueprints[rowIndex].label;
				}
				return null;
			}

			@Override protected boolean isCellEditable(int rowIndex, int columnIndex, BlueprintsColumnID columnID) { return columnID == BlueprintsColumnID.Label; }
			@Override protected void setValueAt(Object aValue, int rowIndex, int columnIndex, BlueprintsColumnID columnID) {
				if (columnID == BlueprintsColumnID.Label) {
					blueprints[rowIndex].label = aValue==null?"":aValue.toString();
					switch(type) {
					case KnownProductBlueprints: GameInfos.saveProductIDsToFile(); break;
					case KnownTechBlueprints   : GameInfos.saveTechIDsToFile(); break;
					}
				}
			}
		}
	}

	static class DiscoveredDataAvailablePanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = 2870833302184314416L;
	
		public DiscoveredDataAvailablePanel(SaveGameData data) {
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
	
		private class DDATableModel extends TableView.SimplifiedTableModel<DDAColumnID> {
	
			protected DDATableModel() {
				super(DDAColumnID.values());
			}
	
			@Override
			public int getRowCount() {
				return data.discoveryData.availableData.size();
			}
	
			@Override
			public Object getValueAt(int rowIndex, int columnIndex, DDAColumnID columnID) {
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

	static class DiscoveredDataStoredPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = 6619075068331735784L;
	
		public DiscoveredDataStoredPanel(SaveGameData data) {
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
	
		private class DDSTableModel extends TableView.SimplifiedTableModel<DDSColumnID> {
	
			protected DDSTableModel() {
				super(DDSColumnID.values());
			}
	
			@Override
			public int getRowCount() {
				return data.discoveryData.storeData.size();
			}
	
			@Override
			public Object getValueAt(int rowIndex, int columnIndex, DDSColumnID columnID) {
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