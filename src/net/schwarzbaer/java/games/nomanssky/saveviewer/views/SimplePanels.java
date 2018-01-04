package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.table.TableCellEditor;

import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.BuildingObject;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.DiscoveryData.AvailableData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.DiscoveryData.StoreData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.PersistentPlayerBase;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.UnboundBuildingObject;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveViewer;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.SaveGameView.SaveGameViewTabPanel;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.DebugTableContextMenu;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.SimplifiedTable;

class SimplePanels {
	
	static class BaseBuildingObjectsPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = 6246130206148705495L;

		public BaseBuildingObjectsPanel(SaveGameData data) {
			super(data);
			
			BBOTableModel tableModel = new BBOTableModel();
			SimplifiedTable table = new SimplifiedTable("BBOTable",tableModel,true,SaveViewer.DEBUG,true);
			JScrollPane tableScrollPane = new JScrollPane(table);
			
			DebugTableContextMenu contextMenu = table.getDebugTableContextMenu();
			contextMenu.add(SaveViewer.createMenuItem("Update ObjectIDs",e->tableModel.initiateColumnUpdate(BBOColumnID.ObjectID)));
			
			add(tableScrollPane,BorderLayout.CENTER);
		}

		private enum BBOColumnID implements TableView.SimplifiedColumnIDInterface {
			// [70, 160, 160, 130, 80, 190, 150, 150]
			Timestamp       ("Timestamp"       , String.class, 35,-1, 70, 70),
			ObjectID        ("ObjectID"        , String.class, 80,-1,160,160),
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
				UnboundBuildingObject bbo = data.baseBuildingObjects[rowIndex];
				if (bbo==null) return null;
				switch(columnID) {
				case Timestamp      : if (bbo.timestamp      ==null) return ""; else return SaveGameData.timestampToString(bbo.timestamp);
				case ObjectID       : if (bbo.objectID       ==null) return ""; else return bbo.getNameOfObjectID();
				case GalacticAddress: if (bbo.galacticAddress==null) return ""; else return bbo.galacticAddress.getCoordinates();
				case RegionSeed     : if (bbo.regionSeed     ==null) return ""; else return String.format("0x%016X", bbo.regionSeed);
				case UserData       : if (bbo.userData       ==null) return ""; else return String.format("0x%08X" , bbo.userData  );
				case Position       : if (bbo.position==null || bbo.position.pos==null) return ""; else return bbo.position.pos.toString("%1.2f");
				case Up             : if (bbo.position==null || bbo.position.up ==null) return ""; else return bbo.position.up .toString("%1.4f");
				case At             : if (bbo.position==null || bbo.position.at ==null) return ""; else return bbo.position.at .toString("%1.4f");
				
				}
				return null;
			}
		}
	}
	
	static class PersistentPlayerBasesPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = -632703090899520348L;

		public PersistentPlayerBasesPanel(SaveGameData data) {
			super(data);
			
			JTabbedPane tabbedPane = new JTabbedPane();
			int i=0;
			for (PersistentPlayerBase pb:this.data.persistentPlayerBases) {
				String title = String.format("Base %d", ++i);
				if (pb.name!=null && !pb.name.isEmpty()) title = String.format("Base \"%s\"", pb.name);
				tabbedPane.addTab(title, new PlayerBasePanel(pb));
			}
			
			add(tabbedPane,BorderLayout.CENTER);
		}
		
		static class PlayerBasePanel extends JPanel {
			private static final long serialVersionUID = 6070388468452658705L;

			public PlayerBasePanel(PersistentPlayerBase playerbase) {
				super(new BorderLayout(3, 3));
				setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
				
				JTextArea textArea = new JTextArea();
				JScrollPane textAreaScrollPane = new JScrollPane(textArea);
				textAreaScrollPane.setPreferredSize(new Dimension(500, 50));
				showValues(playerbase,textArea);
				
				BaseObjectsTableModel tableModel = new BaseObjectsTableModel(playerbase.objects);
				SimplifiedTable table = new SimplifiedTable("BBOTable",tableModel,true,SaveViewer.DEBUG,true);
				JScrollPane tableScrollPane = new JScrollPane(table);
				
				DebugTableContextMenu contextMenu = table.getDebugTableContextMenu();
				contextMenu.add(SaveViewer.createMenuItem("Update ObjectIDs",e->tableModel.initiateColumnUpdate(BaseObjectsColumnID.ObjectID)));
				
				add(tableScrollPane,BorderLayout.CENTER);
				add(textAreaScrollPane,BorderLayout.WEST);
			}

			private void showValues(PersistentPlayerBase playerbase, JTextArea textArea) {
				textArea.setText("");
				
				if (playerbase.name           !=null) textArea.append("Name        : "+playerbase.name           +"\r\n");
				if (playerbase.baseVersion    !=null) textArea.append("Base Version: "+playerbase.baseVersion    +"\r\n");
				if (playerbase.userData       !=null) textArea.append("User Data   : "+String.format("%08X", playerbase.userData)+"\r\n");
				if (playerbase.rid            !=null) textArea.append("RID         : "+playerbase.rid            +"\r\n");
				
				if (playerbase.owner!=null) {
					textArea.append("\r\nOwner :\r\n");
					textArea.append("   LID: "+(playerbase.owner.LID==null?"":playerbase.owner.LID)+"\r\n");
					textArea.append("   UID: "+(playerbase.owner.UID==null?"":playerbase.owner.UID)+"\r\n");
					textArea.append("   USN: "+(playerbase.owner.USN==null?"":playerbase.owner.USN)+"\r\n");
					textArea.append("   TS : "+(playerbase.owner.TS ==null?"":playerbase.owner.TS )+"\r\n");
				}
				
				if (playerbase.galacticAddress!=null) {
					textArea.append("\r\nGalactic Address :\r\n");
					textArea.append("   "+playerbase.galacticAddress.getCoordinates()+"\r\n");
					textArea.append("   "+playerbase.galacticAddress.getExtendedSigBoostCode()+"\r\n");
					textArea.append("   "+playerbase.galacticAddress.getPortalGlyphCodeStr()+"\r\n");
					textArea.append("   "+String.format(Locale.ENGLISH, "%1.1f", playerbase.galacticAddress.getDistToCenter_inRegionUnits())+" regions to center of galaxy\r\n");
				}
				
				if (playerbase.position!=null || playerbase.position!=null) {
					textArea.append("\r\nPosition :\r\n");
					if (playerbase.position!=null) textArea.append("   ## "+playerbase.position.toString("%1.2f")+"\r\n");
					if (playerbase.forward !=null) textArea.append("   -> "+playerbase.forward .toString("%1.4f")+"\r\n");
				}
			}

			private enum BaseObjectsColumnID implements TableView.SimplifiedColumnIDInterface {
				Timestamp       ("Timestamp"       , String.class, 35,-1, 70, 70),
				ObjectID        ("ObjectID"        , String.class, 65,-1,130,130),
				UserData        ("UserData"        , String.class, 40,-1, 80, 80),
				Position        ("Position"        , String.class, 95,-1,190,190),
				Up              ("Up"              , String.class, 75,-1,150,150),
				At              ("At"              , String.class, 75,-1,150,150);
				
				private TableView.SimplifiedColumnConfig columnConfig;
				
				BaseObjectsColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
					columnConfig = new TableView.SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
				}
				@Override public TableView.SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
			}
			
			private class BaseObjectsTableModel extends TableView.SimplifiedTableModel<BaseObjectsColumnID> {
		
				private BuildingObject[] objects;

				protected BaseObjectsTableModel(BuildingObject[] objects) {
					super(BaseObjectsColumnID.values());
					this.objects = objects;
				}
		
				@Override
				public int getRowCount() {
					return objects.length;
				}
		
				@Override
				public Object getValueAt(int rowIndex, int columnIndex, BaseObjectsColumnID columnID) {
					BuildingObject obj = objects[rowIndex];
					if (obj==null) return null;
					switch(columnID) {
					case Timestamp      : if (obj.timestamp      ==null) return ""; return SaveGameData.timestampToString(obj.timestamp);
					case ObjectID       : if (obj.objectID       ==null) return ""; return obj.getNameOfObjectID();
					case UserData       : if (obj.userData       ==null) return ""; return String.format("0x%08X" , obj.userData  );
					case Position       : if (obj.position==null || obj.position.pos==null) return ""; else return obj.position.pos.toString("%1.2f");
					case Up             : if (obj.position==null || obj.position.up ==null) return ""; else return obj.position.up .toString("%1.4f");
					case At             : if (obj.position==null || obj.position.at ==null) return ""; else return obj.position.at .toString("%1.4f");
					}
					return null;
				}
			}
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
			TSrec("TSrec",   Long.class, 50,-1, 80, 80), //[81, 161, 91, 135, 139]
			DD_UA("DD_UA", String.class, 50,-1,160,160),
			DD_DT("DD_DT", String.class, 50,-1, 90, 90),
			DD_VP("DD_VP", String.class, 50,-1,300,300);
			
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
				case TSrec: if (availableData.TSrec==null) return -1; else return availableData.TSrec;
				case DD_UA: if (availableData.DD.UA==null) return ""; else return availableData.DD.UA.getExtendedSigBoostCode();
				case DD_DT: if (availableData.DD.DT==null) return ""; else return availableData.DD.DT;
				case DD_VP: if (availableData.DD.VP==null) return ""; else return toHexArray(availableData.DD.VP);
				}
				return null;
			}
		}
	}
	
	private static String toHexArray(Long[] arr) {
		String str = "";
		for (Long n:arr) {
			if (!str.isEmpty()) str+=",";
			if (n==null) str+="<null>";
			else str+="0x"+Long.toHexString(n).toUpperCase();
		}
		return "["+str+"]";
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
			DD_UA  ("DD_UA"  , String.class, 50,-1,160,160),
			DD_DT  ("DD_DT"  , String.class, 50,-1, 90, 90),
			DD_VP  ("DD_VP"  , String.class, 50,-1,300,300),
			DM     ("DM"     , String.class, 20,-1, 40, 40),
			DM_CN  ("DM_CN"  , String.class, 50,-1, 80, 80),
			OWS_LID("OWS_LID", String.class, 50,-1,120,120),
			OWS_UID("OWS_UID", String.class, 50,-1,120,120),
			OWS_USN("OWS_USN", String.class, 50,-1, 80, 80),
			OWS_TS ("OWS_TS" ,   Long.class, 50,-1, 80, 80),
			RID    ("RID"    , String.class, 50,-1,350,350),
			PTK    ("PTK"    , String.class, 20,-1, 40, 40);
			
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
				case DD_UA  : if (storeData.DD.UA  ==null) return ""; else return storeData.DD.UA.getExtendedSigBoostCode();
				case DD_DT  : if (storeData.DD.DT  ==null) return ""; else return storeData.DD.DT;
				case DD_VP  : if (storeData.DD.VP  ==null) return ""; else return toHexArray(storeData.DD.VP);
				case DM     : if (storeData.DM     ==null) return ""; else return storeData.DM     ;
				case DM_CN  : if (storeData.DM_CN  ==null) return ""; else return storeData.DM_CN  ;
				case OWS_LID: if (storeData.OWS!=null && storeData.OWS.LID==null) return ""; else return storeData.OWS.LID;
				case OWS_UID: if (storeData.OWS!=null && storeData.OWS.UID==null) return ""; else return storeData.OWS.UID;
				case OWS_USN: if (storeData.OWS!=null && storeData.OWS.USN==null) return ""; else return storeData.OWS.USN;
				case OWS_TS : if (storeData.OWS!=null && storeData.OWS.TS ==null) return -1; else return storeData.OWS.TS ;
				case RID    : if (storeData.RID    ==null) return ""; else return storeData.RID    ;
				case PTK    : if (storeData.PTK    ==null) return ""; else return storeData.PTK    ;
				}
				return null;
			}
		}
	}
}