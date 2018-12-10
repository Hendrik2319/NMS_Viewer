package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;

import net.schwarzbaer.gui.StandardDialog;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.gui.Tables.SimplifiedColumnIDInterface;
import net.schwarzbaer.gui.Tables.SimplifiedTableModel;
import net.schwarzbaer.java.games.nomanssky.saveviewer.FileExport;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Gui;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.BuildingObject;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.DiscoveryData.AvailableData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.DiscoveryData.StoreData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Frigate;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Frigate.Modification;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.PersistentPlayerBase;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.TimeStamp;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.UnboundBuildingObject;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.UniverseAddress;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveViewer;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.SaveGameView.SaveGameViewTabPanel;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.SimplifiedTable;

public class SimplePanels {
	
	public static class DeObfuscatorUsagePanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = -1827760869937255459L;

		public DeObfuscatorUsagePanel(SaveGameData data) {
			super(data);
			
			JTextArea textArea = new JTextArea();
			textArea.setEditable(false);
			
			LocalTableModel tableModel = new LocalTableModel();
			SimplifiedTable table = new SimplifiedTable("StoredInteractionsTable",tableModel,true,SaveViewer.DEBUG,true);
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			table.getSelectionModel().addListSelectionListener(e->{
				textArea.setText("");
				String originalStr = tableModel.getValue(table.convertRowIndexToModel(table.getSelectedRow()));
				Vector<String> paths = data.deObfuscatorUsage.get(originalStr);
				if (paths!=null)
					for (String str:paths)
						textArea.append(str+"\r\n");
			});
			
			JScrollPane tableScrollPane = new JScrollPane(table);
			tableScrollPane.setPreferredSize(new Dimension(270, 100));
			add(tableScrollPane,BorderLayout.WEST);
			
			JScrollPane textAreaScrollPane = new JScrollPane(textArea);
			add(textAreaScrollPane,BorderLayout.CENTER);
			
		}
		
		private enum ColumnID implements SimplifiedColumnIDInterface {
			Original    ("Original"   , String.class,  35,-1,  60,  60),
			Replacement ("Replacement", String.class,  35,-1, 180, 180);
			
			private SimplifiedColumnConfig columnConfig;
			
			ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				columnConfig = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			@Override public SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
		}
		
		private class LocalTableModel extends SimplifiedTableModel<ColumnID> {
			
			private Vector<String> values;

			public LocalTableModel() {
				super(ColumnID.values());
				values = new Vector<>(data.deObfuscatorUsage.keySet());
			}

			@Override
			public int getRowCount() {
				return values.size();
			}
			
			public String getValue(int rowIndex) {
				return values.get(rowIndex);
			}

			@Override
			public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
				String originalStr = values.get(rowIndex);
				switch(columnID) {
				case Original   : return originalStr;
				case Replacement: return SaveViewer.deObfuscator.getReplacement(originalStr);
				}
				return null;
			}
		}
	}
	
	public static class FrigatesPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = 1017824861605442560L;
		
		private JTextArea textArea;

		public FrigatesPanel(SaveGameData data, Window mainWindow) {
			super(data);
			textArea = new JTextArea();
			
			LocalTableModel tableModel = new LocalTableModel();
			SimplifiedTable table = new SimplifiedTable("FrigatesTable",tableModel,true,SaveViewer.DEBUG,true);
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			table.getSelectionModel().addListSelectionListener(e->{
				int rowIndex = table.convertRowIndexToModel(table.getSelectedRow());
				showValues(data.frigates.get(rowIndex),rowIndex);
			});
			
			JPopupMenu contextMenu = new JPopupMenu();
			contextMenu.add(SaveViewer.createMenuItem("Edit Modicifations", e->{
				Vector<Frigate.EditableModification> editableMods = new Vector<>();
				
				int rowIndex = table.convertRowIndexToModel(table.getSelectedRow());
				Frigate fr = data.frigates.get(rowIndex);
				for (Frigate.Modification mod:fr.modifications) {
					if (mod instanceof Frigate.EditableModification)
						editableMods.add((Frigate.EditableModification)mod);
				}
				if (!editableMods.isEmpty()) {
					EditNewMods dlg = new EditNewMods(mainWindow, "Edit Modifications", editableMods, ()->showValues(fr,rowIndex));
					new Thread(new Runnable() {
						@Override public void run() {
							dlg.showDialog();
							Frigate.EditableModification.saveKnownEditableModsToFile();
						}
					}).start();
				} else {
					JOptionPane.showMessageDialog(mainWindow, "No editable modifications in current frigate.", "No Editable Modifications", JOptionPane.INFORMATION_MESSAGE);
				}
			}));
			textArea.addMouseListener(new MouseInputAdapter() {
				@Override public void mouseClicked(MouseEvent e) {
					if (e.getButton()==MouseEvent.BUTTON3)
						contextMenu.show(textArea, e.getX(), e.getY());
				}
			});
			
			JScrollPane tableScrollPane = new JScrollPane(table);
			add(tableScrollPane,BorderLayout.CENTER);
			
			JScrollPane textareaScrollPane = new JScrollPane(textArea);
			textareaScrollPane.setPreferredSize(new Dimension(400,50));
			add(textareaScrollPane,BorderLayout.EAST);
			
		}
		
		private void showValues(Frigate fr, int rowIndex) {
			textArea.setText("");
			if (fr==null) return;
			
			appendln();
			appendln("Name      : %s", fr.name    ==null?"":(fr.name!=null && fr.name.isEmpty()?("<Frigate "+(rowIndex+1)+">"):fr.name));
			appendln("Ship Type : %s", fr.shipType==null?"":fr.shipType);
			appendln("Crew Race : %s", fr.crewRace==null?"":fr.crewRace);
			appendln();
			appendln("When Aquired     : %s", fr.aquired         ==null?"":fr.aquired          );
			appendln("Expeditions      : %s", fr.expeditions     ==null?"":fr.expeditions      );
			appendln("Successful Fights: %s", fr.successfulFights==null?"":fr.successfulFights );
			appendln("Damages          : %s", fr.damages         ==null?"":fr.damages          );
			appendln("Fuel Consumption : %s", fr.fuelConsumption ==null?"":String.format("%d t / 250 ly", fr.fuelConsumption) );
			appendln();
			appendln("Combat      : %s ", fr.combatValue     ==null?"":fr.combatValue      );
			appendln("Exploration : %s ", fr.explorationValue==null?"":fr.explorationValue );
			appendln("Mining      : %s ", fr.miningValue     ==null?"":fr.miningValue      );
			appendln("Diplomacy   : %s ", fr.diplomacyValue  ==null?"":fr.diplomacyValue   );
			appendln();
			appendln("Modifications :");
			for (Modification mod:fr.modifications) {
				appendln("    %s:", mod.getLabel());
				appendln("        %s", mod.getValue() );
			}
			appendln();
			appendln("Unidentified Values : %s | %s, %s, %s, %s, %s | %s, %s", fr.unidentifiedVal1_5VG, fr.unidentifiedStatVal5, fr.unidentifiedStatVal6, fr.unidentifiedStatVal7, fr.unidentifiedStatVal8, fr.unidentifiedStatVal9, fr.unidentifiedVal2_yJC, fr.unidentifiedVal3_7hK);
		}
		
		private void appendln() {
			appendln("");
		}
		private void appendln(String format, Object...objects) {
			textArea.append(String.format(format+"\r\n", objects));
		}

		private enum ColumnID implements SimplifiedColumnIDInterface {
			// [80, 80, 80, 120, 70, 70, 70, 93, 40, 40, 40, 40, 90, 150]
			Name          ("Name"               ,    String.class, 35, -1,  80,  80),
			ShipType      ("Ship Type"          ,    String.class, 35, -1,  80,  80),
			CrewRace      ("Crew Race"          ,    String.class, 35, -1,  80,  80),
			Aquired       ("Aquired"            , TimeStamp.class, 35, -1, 120, 120),
			Fights        ("Fights"             ,      Long.class, 35, -1,  70,  70),
			Expeditions   ("Expeditions"        ,      Long.class, 35, -1,  70,  70),
			Damages       ("Damages"            ,      Long.class, 35, -1,  70,  70),
			FuelCons      ("Fuel Consumption"   ,    String.class, 35, -1,  80,  80),
			Combat        ("Combat"             ,      Long.class, 35, -1,  50,  50),
			Exploration   ("Exploration"        ,      Long.class, 35, -1,  50,  50),
			Mining        ("Mining"             ,      Long.class, 35, -1,  50,  50),
			Diplomacy     ("Diplomacy"          ,      Long.class, 35, -1,  50,  50),
			Modifications ("Modifications"      ,      Long.class, 35, -1,  60,  60),
			UnidentValues ("Unidentified Values",    String.class, 35, -1, 150, 150);
			
			private SimplifiedColumnConfig columnConfig;
			
			ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				columnConfig = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			@Override public SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
		}
		
		private class LocalTableModel extends SimplifiedTableModel<ColumnID> {
			
			public LocalTableModel() {
				super(ColumnID.values());
			}

			@Override
			public int getRowCount() {
				return data.frigates.size();
			}

			@Override
			public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
				SaveGameData.Frigate fr = data.frigates.get(rowIndex);
				if (fr==null) return null;
				switch(columnID) {
				case Name    : if (fr.name!=null && fr.name.isEmpty()) return "<Frigate "+(rowIndex+1)+">"; return fr.name;
				case ShipType: return fr.shipType;
				case CrewRace: return fr.crewRace;
					
				case FuelCons   : if (fr.fuelConsumption==null) return ""; return fr.fuelConsumption+" t / 250 ly";
				case Aquired    : return fr.aquired;
				case Expeditions: return fr.expeditions;
				case Fights     : return fr.successfulFights;
				case Damages    : return fr.damages;
					
				case Combat     : return fr.combatValue;
				case Exploration: return fr.explorationValue;
				case Mining     : return fr.miningValue;
				case Diplomacy  : return fr.diplomacyValue;
					
				case Modifications: return fr.modifications.size();
				case UnidentValues: return String.format("%s | %s, %s, %s, %s, %s | %s, %s", fr.unidentifiedVal1_5VG, fr.unidentifiedStatVal5, fr.unidentifiedStatVal6, fr.unidentifiedStatVal7, fr.unidentifiedStatVal8, fr.unidentifiedStatVal9, fr.unidentifiedVal2_yJC, fr.unidentifiedVal3_7hK);
				}
				return null;
			}
		}
		
		private static class EditNewMods extends StandardDialog {
			private static final long serialVersionUID = -6893651224062554261L;

			public EditNewMods(Window parent, String title, Vector<Frigate.EditableModification> editableMods, Runnable updateTask) {
				super(parent, title);
				
				JComboBox<Frigate.EditableModification> cmbBxEditableMods = new JComboBox<>(editableMods);
				JTextField txtfldLabel = SaveViewer.createTextField("", (String str)->{int i=cmbBxEditableMods.getSelectedIndex(); if (i>=0) { editableMods.get(i).label = str; if (updateTask!=null) updateTask.run(); } });
				JTextField txtfldValue = SaveViewer.createTextField("", (String str)->{int i=cmbBxEditableMods.getSelectedIndex(); if (i>=0) { editableMods.get(i).value = str; if (updateTask!=null) updateTask.run(); } });
				txtfldLabel.setColumns(40);
				txtfldValue.setColumns(40); //setPreferredSize(new Dimension(200,16));
				
				cmbBxEditableMods.addActionListener(e->{
					int i=cmbBxEditableMods.getSelectedIndex();
					txtfldLabel.setEnabled(i>=0);
					txtfldValue.setEnabled(i>=0);
					if (i>=0) {
						Frigate.EditableModification mod = editableMods.get(i);
						txtfldLabel.setText(mod.label);
						txtfldValue.setText(mod.value);
					} else {
						txtfldLabel.setText("");
						txtfldValue.setText("");
					}
				});
				cmbBxEditableMods.setSelectedItem(editableMods.get(0));
				
				GridBagLayout layout = new GridBagLayout();
				JPanel inputPanel = new JPanel(layout);
				GridBagConstraints gbc = new GridBagConstraints();
				
				addComp(inputPanel,layout,gbc, new JLabel("Label :"), 1, 0, GridBagConstraints.BOTH);
				addComp(inputPanel,layout,gbc, txtfldLabel, GridBagConstraints.REMAINDER, 1, GridBagConstraints.BOTH);
				addComp(inputPanel,layout,gbc, new JLabel("Value :"), 1, 0, GridBagConstraints.BOTH);
				addComp(inputPanel,layout,gbc, txtfldValue, GridBagConstraints.REMAINDER, 1, GridBagConstraints.BOTH);
				
				JPanel buttonPanel = new JPanel(new BorderLayout(10,10));
				buttonPanel.add(SaveViewer.createButton("Close", e->closeDialog()),BorderLayout.EAST);
				buttonPanel.add(new JLabel(""),BorderLayout.CENTER);
				
				JPanel contentPane = new JPanel(new BorderLayout(10,10));
				contentPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
				
				contentPane.add(cmbBxEditableMods,BorderLayout.NORTH);
				contentPane.add(inputPanel,BorderLayout.CENTER);
				contentPane.add(buttonPanel,BorderLayout.SOUTH);
				
				createGUI(contentPane);
				setSizeAsMinSize();
			}
			
			
			protected void addComp(JPanel panel, GridBagLayout layout, GridBagConstraints gbc, Component comp, int gridwidth, double weightx, int fill) {
				gbc.weightx=weightx;
				gbc.weighty=0;
				gbc.gridwidth=gridwidth;
				gbc.gridheight=1;
				gbc.fill = fill;
				layout.setConstraints(comp, gbc);
				panel.add(comp);
			}
		}
	}
	
	public static class StoredInteractionsPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = 1017824861605442560L;

		public StoredInteractionsPanel(SaveGameData data) {
			super(data);
			
			LocalTableModel tableModel = new LocalTableModel();
			SimplifiedTable table = new SimplifiedTable("StoredInteractionsTable",tableModel,true,SaveViewer.DEBUG,true);
			JScrollPane tableScrollPane = new JScrollPane(table);
			add(tableScrollPane,BorderLayout.CENTER);
		}
		
		private enum ColumnID implements SimplifiedColumnIDInterface {
			// [70, 160, 160, 130, 80, 190, 150, 150]
			GroupIndex       ("G"               , String.class,  35,-1, 35, 35),
			InteractionIndex ("I"               , String.class,  35,-1, 35, 35),
			GalacticAddress  ("GalacticAddress" , String.class,  80,-1,160,160),
			Value            ("Value"           , String.class,  65,-1,100,100),
			GpsCoords        ("GPS Coords"      , String.class, 150,-1,250,250),
			Position         ("Position"        , String.class, 150,-1,250,250);
			
			private SimplifiedColumnConfig columnConfig;
			
			ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				columnConfig = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			@Override public SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
		}
		
		private class LocalTableModel extends SimplifiedTableModel<ColumnID> {
			
			public LocalTableModel() {
				super(ColumnID.values());
			}

			@Override
			public int getRowCount() {
				return data.storedInteractions.size();
			}

			@Override
			public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
				SaveGameData.StoredInteraction si = data.storedInteractions.get(rowIndex);
				if (si==null) return null;
				switch(columnID) {
				case GroupIndex      : return si.groupIndex;
				case InteractionIndex: return si.interactionIndex;
				case GalacticAddress : if (si.galacticAddress==null) return ""; else return si.galacticAddress.getCoordinates();
				case Value           : if (si.value    ==null) return ""; else return String.format("[0x%04X] %d", si.value, si.value);
				case Position        : if (si.position ==null) return ""; else return si.position.toString(" %1.2f ");
				case GpsCoords       : if (si.gpsCoords==null) return ""; else return si.gpsCoords.toString();
				}
				return null;
			}
			
		}
	}
	
	public static class TeleportEndpointsPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = 3670607708610340039L;

		public TeleportEndpointsPanel(SaveGameData data) {
			super(data);
			
			LocalTableModel tableModel = new LocalTableModel();
			SimplifiedTable table = new SimplifiedTable("TeleportEndpointsTable",tableModel,true,SaveViewer.DEBUG,true);
			JScrollPane tableScrollPane = new JScrollPane(table);
			add(tableScrollPane,BorderLayout.CENTER);
		}
		
		private enum ColumnID implements SimplifiedColumnIDInterface {
			// [250, 100, 160, 421, 220, 170]
			// [250, 100, 160, 420, 181, 220, 170]
			Name           ("Name"            , String.class, 150,-1,250,250),
			TeleportHost   ("Teleport Host"   , String.class,  50,-1,100,100),
			UniverseAddress("Universe Address", String.class,  80,-1,160,160),
			PlanetOrSystem ("Planet / System" , String.class, 200,-1,420,420),
			GpsCoords      ("GPS Coords"      , String.class,  90,-1,180,180),
			Position       ("Position"        , String.class, 100,-1,220,220),
			LookAt         ("Look At"         , String.class,  90,-1,170,170);
			
			private SimplifiedColumnConfig columnConfig;
			
			ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				columnConfig = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			@Override public SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
		}
		
		private class LocalTableModel extends SimplifiedTableModel<ColumnID> {
			
			public LocalTableModel() {
				super(ColumnID.values());
			}
	
			@Override
			public int getRowCount() {
				return data.teleportEndpoints.size();
			}
	
			@Override
			public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
				SaveGameData.TeleportEndpoints te = data.teleportEndpoints.get(rowIndex);
				if (te==null) return null;
				switch(columnID) {
				case Name           : return te.name;
				case TeleportHost   : if (te.teleportHost   !=null) return te.teleportHost.label; if (te.teleportHostStr==null) return ""; return "["+te.teleportHostStr+"]"; 
				case UniverseAddress: if (te.universeAddress==null) return ""; else return te.universeAddress.getCoordinates();
				case LookAt         : if (te.lookAt         ==null) return ""; else return te.lookAt   .toString(" %1.4f ");
				case Position       : if (te.position       ==null) return ""; else return te.position .toString(" %1.2f ");
				case GpsCoords      : if (te.gpsCoords      ==null) return ""; else return te.gpsCoords.toString();
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

	public static class BaseBuildingObjectsPanel extends SaveGameViewTabPanel {
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
			contextMenu.add(SaveViewer.createMenuItem("Update ObjectIDs",e->tableModel.initiateColumnUpdate(ColumnID.ObjectID)));
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
			FileExport.writePosToVRML_simple(suggestedFileName,objects.toArray(new BuildingObject[0]), radius, mainWindow,"BuildingObjects");
		}

		private enum ColumnID implements SimplifiedColumnIDInterface {
			// [70, 160, 160, 130, 80, 190, 150, 150]
			// [140, 160, 160, 420, 130, 80, 172, 250, 150, 150]
			// [140, 139, 120, 160, 420, 130, 80, 170, 250, 150, 150]
			Timestamp       ("Timestamp"       , TimeStamp.class,  50,-1,140,140),
			Name            ("Name"            ,    String.class,  50,-1,140,140),
			ObjectID        ("ObjectID"        ,    String.class,  50,-1,120,120),
			GalacticAddress ("GalacticAddress" ,    String.class,  50,-1,160,160),
			PlanetOrSystem  ("Planet / System" ,    String.class, 200,-1,420,420),
			RegionSeed      ("RegionSeed"      ,    String.class,  65,-1,130,130),
			UserData        ("UserData"        ,    String.class,  40,-1, 80, 80),
			GpsCoords       ("GPS Coords"      ,    String.class,  80,-1,170,170),
			Position        ("Position"        ,    String.class,  95,-1,250,250),
			Up              ("Up"              ,    String.class,  75,-1,150,150),
			At              ("At"              ,    String.class,  75,-1,150,150);
			
			private SimplifiedColumnConfig columnConfig;
			
			ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				columnConfig = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			@Override public SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
		}
		
		private class BBOTableModel extends SimplifiedTableModel<ColumnID> {
	
			protected BBOTableModel() {
				super(ColumnID.values());
			}
	
			@Override
			public int getRowCount() {
				return data.baseBuildingObjects.length;
			}
	
			@Override
			public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
				UnboundBuildingObject bbo = data.baseBuildingObjects[rowIndex];
				if (bbo==null) return null;
				switch(columnID) {
				case Timestamp      : return bbo.timestamp;
				case Name           : return bbo.getNameOnly();
				case ObjectID       : if (bbo.objectID       ==null) return ""; return bbo.objectID;
				case GalacticAddress: if (bbo.galacticAddress==null) return ""; return bbo.galacticAddress.getCoordinates();
				case RegionSeed     : if (bbo.regionSeed     ==null) return ""; return String.format("0x%016X", bbo.regionSeed);
				case UserData       : if (bbo.userData       ==null) return ""; return String.format("0x%08X" , bbo.userData  );
				case Position       : if (bbo.position==null || bbo.position.pos==null) return ""; else return bbo.position.pos.toString("%1.2f")+String.format(Locale.ENGLISH," [R:%1.1f]",bbo.position.pos.length());
				case Up             : if (bbo.position==null || bbo.position.up ==null) return ""; else return bbo.position.up .toString("%1.4f");
				case At             : if (bbo.position==null || bbo.position.at ==null) return ""; else return bbo.position.at .toString("%1.4f");
				case GpsCoords      : if (bbo.position==null || bbo.position.gps==null) return ""; else return bbo.position.gps.toString();
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

	public static class PersistentPlayerBasesPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = -632703090899520348L;
		private Window mainWindow;

		public PersistentPlayerBasesPanel(SaveGameData data, Window mainWindow) {
			super(data);
			this.mainWindow = mainWindow;
			
			JTabbedPane tabbedPane = new JTabbedPane();
			Vector<PersistentPlayerBase> bases = this.data.persistentPlayerBases;
			for (int i=0; i<bases.size(); i++) {
				PersistentPlayerBase pb = bases.get(i);
				String title;
				//if (bases.size()<15)
					title = String.format("Base %d: %s", i+1, pb.baseType!=null?pb.baseType.getLongLabel():("["+pb.baseTypeStr+"]"));
				//else if (bases.size()<30)
				//	title = String.format("B%d:%s", j+1, pb.baseType==null?"????":pb.baseType.getMidLabel());
				//else
				//	title = String.format("B%d:%s", j+1, pb.baseType==null?"??":pb.baseType.getShortLabel());
				addBaseTab(tabbedPane, pb, title);
			}
			
			add(tabbedPane,BorderLayout.CENTER);
		}

		private void addBaseTab(JTabbedPane tabbedPane, PersistentPlayerBase pb, String title) {
			int index = tabbedPane.getTabCount();
			tabbedPane.insertTab(title,null, new PlayerBasePanel(this.data,pb,mainWindow), null, index);
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

		public static class PlayerBasePanel extends JPanel {
			private static final long serialVersionUID = 6070388468452658705L;
			
			private SaveGameData data;
			private PersistentPlayerBase playerbase;
			private JTextArea textArea;

			private Window mainWindow;

			public PlayerBasePanel(SaveGameData data, PersistentPlayerBase playerbase, Window mainWindow) {
				super(new BorderLayout(3, 3));
				this.mainWindow = mainWindow;
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
			
			public enum Type { Simple, Models, Planet }
			private String suggestFileName(Type type) {
				return suggestFileName(type, data, playerbase);
			}
			public static String suggestFileName(Type type, SaveGameData data, PersistentPlayerBase playerbase) {
				String prefix = String.format("Base_%s.%d", data.index>=0?(""+(data.index+1)):"#", playerbase.baseIndex+1);
				switch (type) {
				case Simple: return prefix+"_simple.wrl";
				case Models: return prefix+".wrl";
				case Planet: return prefix+"_planet.wrl";
				}
				return "";
			}
			
			private void addVRMLtasks(JPopupMenu contextMenu) {
				contextMenu.add(SaveViewer.createMenuItem("Write Base to VRML (simple)",e->FileExport.writePosToVRML_simple(suggestFileName(Type.Simple),playerbase.objects,null,mainWindow,"Base"), null, SaveViewer.ToolbarIcons.SaveAs));
				contextMenu.add(SaveViewer.createMenuItem("Write Base to VRML (Models)",e->FileExport.writePosToVRML_models(suggestFileName(Type.Models),null,playerbase,mainWindow,"Base", false), null, SaveViewer.ToolbarIcons.SaveAs));
				contextMenu.add(SaveViewer.createMenuItem("Write Whole Planet to VRML (simple)",e->{
					Vector<BuildingObject> nearObj = getNearObjects();
					nearObj.add(BuildingObject.createFromBase(playerbase));
					
					Double radius = null;
					for (BuildingObject obj:nearObj)
						if (obj.position!=null && obj.position.pos!=null) {
							if (radius==null) radius = obj.position.pos.length();
							else radius = Math.min(radius, obj.position.pos.length());
						}
					
					FileExport.writePosToVRML_simple(suggestFileName(Type.Planet),nearObj.toArray(new BuildingObject[0]),radius,mainWindow,"Whole Planet");
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
						if (obj.position!=null && obj.position.pos!=null && obj.position.gps!=null) {
							textArea.append(obj.position.gps.toString(false));
							if (playerbase.position!=null)
								textArea.append(String.format(Locale.ENGLISH," (-> %9.2f u)", playerbase.position.distTo_onSphere(obj.position.pos)));
							textArea.append("   ");
						}
						textArea.append(obj.getNameOrObjectID()+"\r\n");
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
				
				if (playerbase.position!=null || playerbase.forward!=null || playerbase.gpsCoords!=null) {
					textArea.append("\r\nPosition :\r\n");
					if (playerbase.position !=null) textArea.append("   position  : "+playerbase.position .toString("%1.2f")+"\r\n");
					if (playerbase.forward  !=null) textArea.append("   forward   : "+playerbase.forward  .toString("%1.4f")+"\r\n");
					if (playerbase.gpsCoords!=null) textArea.append("   gps coords: "+playerbase.gpsCoords.toString()+"\r\n");
				}
			}

			private enum BaseObjectsColumnID implements SimplifiedColumnIDInterface {
				// [140, 176, 128, 80, 150, 170, 170]
				Timestamp("Timestamp", TimeStamp.class, 70,-1,140,140),
				Name     ("Name"     ,    String.class, 50,-1,180,180),
				ObjectID ("ObjectID" ,    String.class, 50,-1,130,130),
				UserData ("UserData" ,    String.class, 40,-1, 80, 80),
				Position ("Position" ,    String.class, 75,-1,150,150),
				Up       ("Up"       ,    String.class, 85,-1,170,170),
				At       ("At"       ,    String.class, 85,-1,170,170);
				
				private SimplifiedColumnConfig columnConfig;
				
				BaseObjectsColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
					columnConfig = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
				}
				@Override public SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
			}
			
			private class BaseObjectsTableModel extends SimplifiedTableModel<BaseObjectsColumnID> {
		
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
					case Name     : return obj.getNameOnly();
					case ObjectID : if (obj.objectID==null) return ""; return obj.objectID;
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
	
	public static class BlueprintsPanel extends SaveGameViewTabPanel {
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

		private enum ColumnID implements SimplifiedColumnIDInterface {
			ID    ("ID"    , String.class, 100,-1,120,120),
			Label ("Label" , String.class, 200,-1,220,220);
			
			private SimplifiedColumnConfig columnConfig;
			
			ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				columnConfig = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			@Override public SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
		}
	
		private class BlueprintsTableModel extends SimplifiedTableModel<ColumnID> {

			private GeneralizedID[] blueprints;

			protected BlueprintsTableModel(GeneralizedID[] blueprints) {
				super(ColumnID.values());
				this.blueprints = blueprints;
			}

			@Override public int getRowCount() { return blueprints.length; }
			@Override public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
				switch(columnID) {
				case ID   : return blueprints[rowIndex].id;
				case Label: return blueprints[rowIndex].label;
				}
				return null;
			}

			@Override protected boolean isCellEditable(int rowIndex, int columnIndex, ColumnID columnID) { return columnID == ColumnID.Label; }
			@Override protected void setValueAt(Object aValue, int rowIndex, int columnIndex, ColumnID columnID) {
				if (columnID == ColumnID.Label) {
					blueprints[rowIndex].setLabel(aValue==null?"":aValue.toString());
					switch(type) {
					case KnownProductBlueprints: GameInfos.saveProductIDsToFile(); break;
					case KnownTechBlueprints   : GameInfos.saveTechIDsToFile(); break;
					}
				}
			}
		}
	}
	
	public static class DiscoveredDataAvailablePanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = 2870833302184314416L;
	
		public DiscoveredDataAvailablePanel(SaveGameData data) {
			super(data);
			
			DDATableModel tableModel = new DDATableModel();
			SimplifiedTable table = new SimplifiedTable("DDATable",tableModel,true,SaveViewer.DEBUG,true);
			JScrollPane tableScrollPane = new JScrollPane(table);
			
			add(tableScrollPane,BorderLayout.CENTER);
		}
	
		private enum ColumnID implements SimplifiedColumnIDInterface {
			TSrec("Timestamp"       , SaveGameData.TimeStamp.class, 50,-1,140,140), //[81, 161, 91, 135, 139]
			DD_UA("Universe Address", String.class, 50,-1,160,160),
			DD_DT("Data Type"       , String.class, 50,-1, 90, 90),
			DD_VP("DD_VP"           , String.class, 50,-1,300,300);
			
			private SimplifiedColumnConfig columnConfig;
			
			ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				columnConfig = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			@Override public SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
		}
	
		private class DDATableModel extends SimplifiedTableModel<ColumnID> {
	
			protected DDATableModel() {
				super(ColumnID.values());
			}
	
			@Override
			public int getRowCount() {
				return data.discoveryData.availableData.size();
			}
	
			@Override
			public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
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

	public static class DiscoveredDataStoredPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = 6619075068331735784L;
	
		public DiscoveredDataStoredPanel(SaveGameData data) {
			super(data);
			
			DDSTableModel tableModel = new DDSTableModel();
			SimplifiedTable table = new SimplifiedTable("DDSTable",tableModel,true,SaveViewer.DEBUG,true);
			JScrollPane tableScrollPane = new JScrollPane(table);
			
			add(tableScrollPane,BorderLayout.CENTER);
		}
	
		private enum ColumnID implements SimplifiedColumnIDInterface {
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
			
			private SimplifiedColumnConfig columnConfig;
			
			ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				columnConfig = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			@Override public SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
		}
	
		private class DDSTableModel extends SimplifiedTableModel<ColumnID> {
	
			protected DDSTableModel() {
				super(ColumnID.values());
			}
	
			@Override
			public int getRowCount() {
				return data.discoveryData.storeData.size();
			}
	
			@Override
			public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
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