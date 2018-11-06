package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;

import net.schwarzbaer.java.games.nomanssky.saveviewer.FileExport;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Gui;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.BuildingObject;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.DiscoveryData.AvailableData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.DiscoveryData.StoreData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.PersistentPlayerBase;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.TimeStamp;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.UnboundBuildingObject;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.UniverseAddress;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveViewer;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.SaveGameView.SaveGameViewTabPanel;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.SimplifiedTable;

public class SimplePanels {
	
	static class StoredInteractionsPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = 1017824861605442560L;

		public StoredInteractionsPanel(SaveGameData data) {
			super(data);
			
			StoredInteractionsTableModel tableModel = new StoredInteractionsTableModel();
			SimplifiedTable table = new SimplifiedTable("StoredInteractionsTable",tableModel,true,SaveViewer.DEBUG,true);
			JScrollPane tableScrollPane = new JScrollPane(table);
			add(tableScrollPane,BorderLayout.CENTER);
		}
		
		private enum StoredInteractionsTableColumnID implements TableView.SimplifiedColumnIDInterface {
			// [70, 160, 160, 130, 80, 190, 150, 150]
			GroupIndex       ("G"               , String.class,  35,-1, 35, 35),
			InteractionIndex ("I"               , String.class,  35,-1, 35, 35),
			GalacticAddress  ("GalacticAddress" , String.class,  80,-1,160,160),
			Value            ("Value"           , String.class,  65,-1,100,100),
			Position         ("Position"        , String.class, 150,-1,250,250);
			
			private TableView.SimplifiedColumnConfig columnConfig;
			
			StoredInteractionsTableColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				columnConfig = new TableView.SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			@Override public TableView.SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
		}
		
		private class StoredInteractionsTableModel extends TableView.SimplifiedTableModel<StoredInteractionsTableColumnID> {
			
			public StoredInteractionsTableModel() {
				super(StoredInteractionsTableColumnID.values());
			}

			@Override
			public int getRowCount() {
				return data.storedInteractions.size();
			}

			@Override
			public Object getValueAt(int rowIndex, int columnIndex, StoredInteractionsTableColumnID columnID) {
				SaveGameData.StoredInteraction si = data.storedInteractions.get(rowIndex);
				if (si==null) return null;
				switch(columnID) {
				case GroupIndex      : return si.groupIndex;
				case InteractionIndex: return si.interactionIndex;
				case GalacticAddress : if (si.galacticAddress==null) return ""; else return si.galacticAddress.getCoordinates();
				case Value           : if (si.value   ==null) return ""; else return String.format("[0x%04X] %d", si.value, si.value);
				case Position        : if (si.position==null) return ""; else return si.position.toString(" %1.2f ");
				}
				return null;
			}
			
		}
	}
	
	static class TeleportEndpointsPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = 3670607708610340039L;

		public TeleportEndpointsPanel(SaveGameData data) {
			super(data);
			
			LocalTableModel tableModel = new LocalTableModel();
			SimplifiedTable table = new SimplifiedTable("TeleportEndpointsTable",tableModel,true,SaveViewer.DEBUG,true);
			JScrollPane tableScrollPane = new JScrollPane(table);
			add(tableScrollPane,BorderLayout.CENTER);
		}
		
		private enum LocalTableColumnID implements TableView.SimplifiedColumnIDInterface {
			// [250, 100, 160, 421, 220, 170]
			Name           ("Name"            , String.class, 150,-1,250,250),
			TeleportHost   ("Teleport Host"   , String.class,  50,-1,100,100),
			UniverseAddress("Universe Address", String.class,  80,-1,160,160),
			PlanetOrSystem ("Planet / System" , String.class, 200,-1,420,420),
			Position       ("Position"        , String.class, 100,-1,220,220),
			LookAt         ("Look At"         , String.class,  90,-1,170,170);
			
			private TableView.SimplifiedColumnConfig columnConfig;
			
			LocalTableColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				columnConfig = new TableView.SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			@Override public TableView.SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
		}
		
		private class LocalTableModel extends TableView.SimplifiedTableModel<LocalTableColumnID> {
			
			public LocalTableModel() {
				super(LocalTableColumnID.values());
			}
	
			@Override
			public int getRowCount() {
				return data.teleportEndpoints.size();
			}
	
			@Override
			public Object getValueAt(int rowIndex, int columnIndex, LocalTableColumnID columnID) {
				SaveGameData.TeleportEndpoints te = data.teleportEndpoints.get(rowIndex);
				if (te==null) return null;
				switch(columnID) {
				case Name           : return te.name;
				case TeleportHost   : return te.teleportHost;
				case UniverseAddress: if (te.universeAddress==null) return ""; else return te.universeAddress.getCoordinates();
				case LookAt         : if (te.lookAt  ==null) return ""; else return te.lookAt  .toString(" %1.4f ");
				case Position       : if (te.position==null) return ""; else return te.position.toString(" %1.2f ");
				case PlanetOrSystem : {
					if (te.universeAddress==null) return "";
					String strOut = "";
					Vector<String> verboseName = te.universeAddress.getVerboseName(data.universe);
					for (int i=0; i<verboseName.size() && i<2; i++)
						strOut += " "+verboseName.get(i);
					return strOut; }
				}
				return null;
			}
		}
	}

	static class BaseBuildingObjectsPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = 6246130206148705495L;
		private static final Color COLOR_HIGHLIGHT = new Color(0xFFFF7F);
		private Window mainWindow;
		private Long addressToHighlight;
		private SimplifiedTable table;

		public BaseBuildingObjectsPanel(SaveGameData data, Window mainWindow) {
			super(data);
			this.mainWindow = mainWindow;
			this.addressToHighlight = null;
			
			BBOTableModel tableModel = new BBOTableModel();
			table = new SimplifiedTable("BBOTable",tableModel,true,SaveViewer.DEBUG,true);
			table.setCellRendererForAllColumns(new DefaultTableCellRenderer(){
				private static final long serialVersionUID = -8578657095051025534L;
				@Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
					Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
					if (!isSelected) {
						Color background = table.getBackground();
						if (addressToHighlight!=null) {
							UnboundBuildingObject ubo = data.baseBuildingObjects[table.convertRowIndexToModel(row)];
							if (ubo.galacticAddress!=null && ubo.galacticAddress.getAddress()==addressToHighlight)
								background = COLOR_HIGHLIGHT;
						}
						component.setBackground(background);
					}
					return component;
				}
				
			},true);
			JScrollPane tableScrollPane = new JScrollPane(table);
			
			TableView.DebugTableContextMenu contextMenu = table.getDebugTableContextMenu();
			contextMenu.addSeparator();
			contextMenu.add(SaveViewer.createMenuItem("Highlight Specific Address",e->highlightSpecificAddress()));
			contextMenu.add(SaveViewer.createMenuItem("Update ObjectIDs",e->tableModel.initiateColumnUpdate(BBOColumnID.ObjectID)));
			contextMenu.add(SaveViewer.createMenuItem("Write Positions to VRML",e->writePosToVRML(),null,SaveViewer.ToolbarIcons.SaveAs));
			
			add(tableScrollPane,BorderLayout.CENTER);
		}
		
		private void highlightSpecificAddress() {
			Gui.CoordinatesDialog dlg = new Gui.CoordinatesDialog(mainWindow,true,"Select Coordinates");
			dlg.showDialog();
			if (dlg.hasResult()) {
				addressToHighlight = dlg.getResult();
			}
			table.repaint();
		}

		private static class NamedAddress {

			private String name;
			private long addr;

			public NamedAddress(String name, long addr) {
				this.name = name;
				this.addr = addr;
			}

			@Override public String toString() { return name; }
		}

		private void writePosToVRML() {
			//System.out.println("data.baseBuildingObjects.length: "+data.baseBuildingObjects.length);
			
			HashMap<Long,Integer> foundAddresses = new HashMap<>();
			for (UnboundBuildingObject ubo:data.baseBuildingObjects) {
				long address = ubo.galacticAddress.getAddress();
				//System.out.printf("  address: %016X\r\n",address);
				Integer amount = foundAddresses.get(address);
				if (amount==null) amount=0;
				foundAddresses.put(address,amount+1);
			}
			//System.out.println("foundAddresses.size(): "+foundAddresses.size());
			
			Vector<NamedAddress> names = new Vector<>();
			for (long addr:foundAddresses.keySet()) {
				Integer amount = foundAddresses.get(addr);
				String str = new UniverseAddress(addr).getVerboseNameInOneLine(data.universe);
				names.add(new NamedAddress(String.format("[%2d] %s",amount,str),addr));
			}
			names.sort(Comparator.comparing((NamedAddress na)->na.name).reversed());
			//System.out.println("names.size(): "+names.size());
			
			Gui.ListBoxDialog<NamedAddress> dlg = new Gui.ListBoxDialog<>(mainWindow,"Select Planet :", "Select Planet", names.toArray(new NamedAddress[0]), names.firstElement(), 600,200);
			dlg.showDialog();
			if (!dlg.hasResult()) return;
			
			int index = dlg.getResultIndex();
			if (index<0) return;
			
			NamedAddress selected = names.get(index);
			long selectedAddress = selected.addr;
			//System.out.printf("selectedAddress: %016X\r\n",selectedAddress);
			//System.out.printf("selectedName   : %s\r\n",selected.name);
			
			Double radius = null;
			Vector<BuildingObject> objects = new Vector<>();
			for (UnboundBuildingObject ubo:data.baseBuildingObjects) {
				long address = ubo.galacticAddress.getAddress();
				//System.out.printf("  address: %016X %s\r\n",address,(address==selectedAddress?"####":""));
				if (address==selectedAddress) {
					objects.add(ubo);
					if (ubo.position!=null && ubo.position.pos!=null) {
						if (radius==null) radius = ubo.position.pos.length();
						else radius = Math.min(radius,ubo.position.pos.length());
					}
				}
			}
			//System.out.println("objects.size(): "+objects.size());
			
			String suggestedFileName = String.format("BBO_%s.wrl", data.index>=0?(""+(data.index+1)):"#");
			FileExport.writePosToVRML_simple(suggestedFileName,objects.toArray(new BuildingObject[0]), radius, this);
		}

		private enum BBOColumnID implements TableView.SimplifiedColumnIDInterface {
			// [70, 160, 160, 130, 80, 190, 150, 150]
			Timestamp       ("Timestamp"       , TimeStamp.class,  35,-1,140,140),
			ObjectID        ("ObjectID"        ,    String.class,  80,-1,160,160),
			GalacticAddress ("GalacticAddress" ,    String.class,  80,-1,160,160),
			PlanetOrSystem  ("Planet / System" ,    String.class, 200,-1,420,420),
			RegionSeed      ("RegionSeed"      ,    String.class,  65,-1,130,130),
			UserData        ("UserData"        ,    String.class,  40,-1, 80, 80),
			Position        ("Position"        ,    String.class,  95,-1,250,250),
			Up              ("Up"              ,    String.class,  75,-1,150,150),
			At              ("At"              ,    String.class,  75,-1,150,150);
			
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
				case Timestamp      : return bbo.timestamp;
				case ObjectID       : if (bbo.objectID       ==null) return ""; else return bbo.getNameOfObjectID();
				case GalacticAddress: if (bbo.galacticAddress==null) return ""; else return bbo.galacticAddress.getCoordinates();
				case RegionSeed     : if (bbo.regionSeed     ==null) return ""; else return String.format("0x%016X", bbo.regionSeed);
				case UserData       : if (bbo.userData       ==null) return ""; else return String.format("0x%08X" , bbo.userData  );
				case Position       : if (bbo.position==null || bbo.position.pos==null) return ""; else return bbo.position.pos.toString("%1.2f")+String.format(Locale.ENGLISH," [R:%1.1f]",bbo.position.pos.length());
				case Up             : if (bbo.position==null || bbo.position.up ==null) return ""; else return bbo.position.up .toString("%1.4f");
				case At             : if (bbo.position==null || bbo.position.at ==null) return ""; else return bbo.position.at .toString("%1.4f");
				case PlanetOrSystem : {
					if (bbo.galacticAddress==null) return "";
					String strOut = "";
					Vector<String> verboseName = bbo.galacticAddress.getVerboseName(data.universe);
					for (int i=0; i<verboseName.size() && i<2; i++)
						strOut += " "+verboseName.get(i);
					return strOut; }
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
			Vector<PersistentPlayerBase> bases = this.data.persistentPlayerBases;
			for (int j = 0; j < bases.size(); j++) {
				PersistentPlayerBase pb = bases.get(j);
				String title;
				//if (bases.size()<15)
					title = String.format("Base %d: %s", j, pb.baseType!=null?pb.baseType.getLongLabel():("["+pb.baseTypeStr+"]"));
				//else if (bases.size()<30)
				//	title = String.format("B%d:%s", j, pb.baseType==null?"????":pb.baseType.getMidLabel());
				//else
				//	title = String.format("B%d:%s", j, pb.baseType==null?"??":pb.baseType.getShortLabel());
				addBaseTab(tabbedPane, pb, title, j);
			}
			
			add(tabbedPane,BorderLayout.CENTER);
		}

		private void addBaseTab(JTabbedPane tabbedPane, PersistentPlayerBase pb, String title, int baseIndex) {
			//String title = String.format("Base %d", ++i);
			//if (pb.name!=null && !pb.name.isEmpty()) title = String.format("Base \"%s\"", pb.name);
			int index = tabbedPane.getTabCount();
			tabbedPane.insertTab(title,null, new PlayerBasePanel(this.data,pb), null, index);
			tabbedPane.setTabComponentAt(index, new BaseTabHeader(title,pb));
		}
		
		private static final class BaseTabHeader extends JPanel {
			private static final long serialVersionUID = -4150419571200547225L;
			private static final Color FG_Freighter = new Color(0x4483FF);
			private static final Color FG_Planet    = new Color(0x007F00);

			public BaseTabHeader(String title, PersistentPlayerBase pb) {
				setOpaque(false);
				setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
				JLabel label = new JLabel(title);
				if (pb.baseType!=null)
					switch (pb.baseType) {
					case FreighterBase : label.setForeground(FG_Freighter); break;
					case HomePlanetBase: label.setForeground(FG_Planet); break;
					}
				add(label);
			}
		}

		static class PlayerBasePanel extends JPanel {
			private static final long serialVersionUID = 6070388468452658705L;
			
			private SaveGameData data;
			private PersistentPlayerBase playerbase;
			private JTextArea textArea;

			public PlayerBasePanel(SaveGameData data, PersistentPlayerBase playerbase) {
				super(new BorderLayout(3, 3));
				setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
				
				this.data = data;
				this.playerbase = playerbase;
				
				textArea = new JTextArea();
				JScrollPane textAreaScrollPane = new JScrollPane(textArea);
				textAreaScrollPane.setPreferredSize(new Dimension(500, 50));
				
				BaseObjectsTableModel tableModel = new BaseObjectsTableModel(this.playerbase.objects);
				SimplifiedTable table = new SimplifiedTable("BaseObjectsTable",tableModel,true,SaveViewer.DEBUG,true);
				JScrollPane tableScrollPane = new JScrollPane(table);
				
				TableView.DebugTableContextMenu contextMenu = table.getDebugTableContextMenu();
				contextMenu.addSeparator();
				contextMenu.add(SaveViewer.createMenuItem("Update ObjectIDs",e->tableModel.initiateColumnUpdate(BaseObjectsColumnID.ObjectID)));
				addVRMLtasks(contextMenu);
				
				JPopupMenu textAreaContextMenu = new JPopupMenu();
				addVRMLtasks(textAreaContextMenu);
				new Gui.ContextMenuInvoker(textArea, textAreaContextMenu);
				
				add(tableScrollPane,BorderLayout.CENTER);
				add(textAreaScrollPane,BorderLayout.WEST);
				
				showValues();
				showOtherObjectsOnThisPlanet();
			}
			
			private enum Type { Simple, Models, Planet }
			private String suggestFileName(Type type) {
				String prefix = String.format("Base_%s.%d", data.index>=0?(""+(data.index+1)):"#", playerbase.baseIndex+1);
				switch (type) {
				case Simple: return prefix+"_simple.wrl";
				case Models: return prefix+".wrl";
				case Planet: return prefix+"_planet.wrl";
				}
				return "";
			}
			
			private void addVRMLtasks(JPopupMenu contextMenu) {
				contextMenu.add(SaveViewer.createMenuItem("Write Base to VRML (simple)",e->FileExport.writePosToVRML_simple(suggestFileName(Type.Simple),playerbase.objects,null, PlayerBasePanel.this), null, SaveViewer.ToolbarIcons.SaveAs));
				contextMenu.add(SaveViewer.createMenuItem("Write Base to VRML (Models)",e->FileExport.writePosToVRML_models(suggestFileName(Type.Models),null,playerbase,PlayerBasePanel.this), null, SaveViewer.ToolbarIcons.SaveAs));
				contextMenu.add(SaveViewer.createMenuItem("Write Whole Planet to VRML (simple)",e->{
					Vector<BuildingObject> nearObj = getNearObjects();
					nearObj.add(BuildingObject.createFromBase(playerbase));
					
					Double radius = null;
					for (BuildingObject obj:nearObj)
						if (obj.position!=null && obj.position.pos!=null) {
							if (radius==null) radius = obj.position.pos.length();
							else radius = Math.min(radius, obj.position.pos.length());
						}
					
					FileExport.writePosToVRML_simple(suggestFileName(Type.Planet),nearObj.toArray(new BuildingObject[0]),radius,this);
				}, null, SaveViewer.ToolbarIcons.SaveAs));
			}

			private void showOtherObjectsOnThisPlanet() {
				Vector<BuildingObject> nearObj = getNearObjects();
				
				if (nearObj.isEmpty()) {
					textArea.append("\r\nNo other objects on same planet.\r\n");
				} else {
					textArea.append("\r\nOther objects on same planet :\r\n");
					for(BuildingObject obj:nearObj) {
						textArea.append("   ");
						if (obj.position!=null && obj.position.pos!=null) {
							textArea.append(obj.position.pos.toString("%8.1f"));
							if (playerbase.position!=null)
								textArea.append(String.format(Locale.ENGLISH," (-> %9.2f u)", playerbase.position.distTo(obj.position.pos)));
							textArea.append("   ");
						}
						textArea.append(obj.getNameOfObjectID()+"\r\n");
					}
				}
			}

			private Vector<BuildingObject> getNearObjects() {
				Vector<BuildingObject> nearObj = new Vector<BuildingObject>();
				
				if (playerbase.galacticAddress==null) return nearObj;
				long pbAddress =  playerbase.galacticAddress.getAddress();
				
				if (data.baseBuildingObjects!=null)
					for(UnboundBuildingObject ubo:data.baseBuildingObjects)
						if (ubo.galacticAddress!=null && ubo.galacticAddress.getAddress()==pbAddress)
							nearObj.add(ubo);
				
				return nearObj;
			}

			private void showValues() {
				textArea.setText("");
				
				textArea.append("Name         : "+(playerbase.name        ==null?"":playerbase.name        )+"\r\n");
				textArea.append("Base Version : "+(playerbase.baseVersion ==null?"":playerbase.baseVersion )+"\r\n");
				textArea.append("User Data    : "+(playerbase.userData    ==null?"":String.format("%08X", playerbase.userData))+"\r\n");
				textArea.append("RID          : "+(playerbase.rid         ==null?"":playerbase.rid         )+"\r\n");
				textArea.append("Type         : "+(playerbase.baseType    !=null?playerbase.baseType.getLongLabel():(playerbase.baseTypeStr==""?"":("["+playerbase.baseTypeStr+"]")))+"\r\n");
				textArea.append("Last Update  : "+(playerbase.lastUpdateTS==null?"":playerbase.lastUpdateTS.toString())+"\r\n");
				textArea.append("Value [wx7]  : "+(playerbase.value__wx7  ==null?"":playerbase.value__wx7  )+"\r\n");
				
				if (playerbase.owner!=null) {
					textArea.append("\r\nOwner :\r\n");
					textArea.append("   LID : "+(playerbase.owner.LID==null?"":playerbase.owner.LID)+"\r\n");
					textArea.append("   UID (User ID  ) : "+(playerbase.owner.UID==null?"":playerbase.owner.UID)+"\r\n");
					textArea.append("   USN (User Name) : "+(playerbase.owner.USN==null?"":playerbase.owner.USN)+"\r\n");
					textArea.append("   TS  (Timestamp) : "+(playerbase.owner.TS ==null?"":playerbase.owner.TS.toString())+"\r\n");
				}
				
				if (playerbase.galacticAddress!=null) {
					textArea.append("\r\nGalactic Address :\r\n");
					for (String str:playerbase.galacticAddress.getVerboseName(data.universe)) textArea.append("   "+str+"\r\n");
					textArea.append("   "+playerbase.galacticAddress.getCoordinates()+"\r\n");
					textArea.append("   "+playerbase.galacticAddress.getExtendedSigBoostCode()+"\r\n");
					textArea.append("   "+playerbase.galacticAddress.getPortalGlyphCodeStr()+"\r\n");
					textArea.append("   "+playerbase.galacticAddress.getAddressStr()+"\r\n");
					textArea.append("   "+String.format(Locale.ENGLISH, "%1.1f", playerbase.galacticAddress.getDistToCenter_inRegionUnits())+" regions to center of galaxy\r\n");
				}
				
				if (playerbase.position!=null || playerbase.forward!=null) {
					textArea.append("\r\nPosition :\r\n");
					if (playerbase.position!=null) textArea.append("   position : "+playerbase.position.toString("%1.2f")+"\r\n");
					if (playerbase.forward !=null) textArea.append("   forward  : "+playerbase.forward .toString("%1.4f")+"\r\n");
				}
			}

			private enum BaseObjectsColumnID implements TableView.SimplifiedColumnIDInterface {
				Timestamp       ("Timestamp"       , TimeStamp.class, 70,-1,140,140),
				ObjectID        ("ObjectID"        ,    String.class,120,-1,240,240),
				UserData        ("UserData"        ,    String.class, 40,-1, 80, 80),
				Position        ("Position"        ,    String.class, 75,-1,150,150),
				Up              ("Up"              ,    String.class, 85,-1,170,170),
				At              ("At"              ,    String.class, 85,-1,170,170);
				
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
					case Timestamp: return obj.timestamp;
					case ObjectID : if (obj.objectID==null) return ""; return obj.getNameOfObjectID();
					case UserData : if (obj.userData==null) return ""; return String.format("0x%08X" , obj.userData  );
					case Position : if (obj.position==null || obj.position.pos==null) return ""; else return obj.position.pos.toString(" %1.2f ");
					case Up       : if (obj.position==null || obj.position.up ==null) return ""; else return obj.position.up .toString(" %1.4f ");
					case At       : if (obj.position==null || obj.position.at ==null) return ""; else return obj.position.at .toString(" %1.4f ");
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
					blueprints[rowIndex].setLabel(aValue==null?"":aValue.toString());
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
			TSrec("Timestamp"       , SaveGameData.TimeStamp.class, 50,-1,140,140), //[81, 161, 91, 135, 139]
			DD_UA("Universe Address", String.class, 50,-1,160,160),
			DD_DT("Data Type"       , String.class, 50,-1, 90, 90),
			DD_VP("DD_VP"           , String.class, 50,-1,300,300);
			
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
				case TSrec: return availableData.TSrec;
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
			DD_UA  ("DD_UA"  ,    String.class, 50,-1,160,160),
			DD_DT  ("DD_DT"  ,    String.class, 50,-1, 90, 90),
			DD_VP  ("DD_VP"  ,    String.class, 50,-1,300,300),
			DM     ("DM"     ,    String.class, 20,-1, 40, 40),
			DM_CN  ("DM_CN"  ,    String.class, 50,-1, 80, 80),
			OWS_LID("OWS_LID",    String.class, 50,-1,120,120),
			OWS_UID("OWS_UID",    String.class, 50,-1,120,120),
			OWS_USN("OWS_USN",    String.class, 50,-1, 80, 80),
			OWS_TS ("OWS_TS" , TimeStamp.class, 50,-1,140,140),
			RID    ("RID"    ,    String.class, 50,-1,350,350),
			PTK    ("PTK"    ,    String.class, 20,-1, 40, 40);
			
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
				case OWS_TS : if (storeData.OWS==null) return null; else return storeData.OWS.TS;
				case RID    : if (storeData.RID    ==null) return ""; else return storeData.RID    ;
				case PTK    : if (storeData.PTK    ==null) return ""; else return storeData.PTK    ;
				}
				return null;
			}
		}
	}
}