package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Vector;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.DefaultTableCellRenderer;

import net.schwarzbaer.java.games.nomanssky.saveviewer.FileExport;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.IDMap;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Gui;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Images.NamedColor;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.AddressdableObject;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.BuildingObject;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Companions.Companion;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Coordinates;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Frigate;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.FrigateMission;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.FrigateMission.FrigateMissionTask;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Inventories.Inventory;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.PersistentPlayerBase;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.SeedValue;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.TimeStamp;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.UnboundBuildingObject;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.UniverseAddress;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveViewer;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.SaveGameView.SaveGameViewPanelGroupingPanel;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.SaveGameView.SaveGameViewTabGroupingPanel;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.SaveGameView.SaveGameViewTabPanel;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.SimplifiedTable;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.VerySimpleTable;
import net.schwarzbaer.java.lib.gui.IconSource;
import net.schwarzbaer.java.lib.gui.StandardDialog;
import net.schwarzbaer.java.lib.gui.IconSource.CachedIcons;
import net.schwarzbaer.java.lib.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.java.lib.gui.Tables.SimplifiedColumnIDInterface;
import net.schwarzbaer.java.lib.gui.Tables.SimplifiedTableModel;

public class SimplePanels {

	private static final CachedIcons<BuildingObject.BaseObjColor> BaseObjColorIconsIS;
	private static final CachedIcons<BuildingObject.BaseObjAppearance> BaseObjAppearanceIconsIS;
	
	static {
		IconSource<BuildingObject.BaseObjColor> uncachedBaseObjColorIconsIS_ = new IconSource<>(52,52,8);
		uncachedBaseObjColorIconsIS_.readIconsFromResource("/images/FarbCodes.Farben.png");
		BaseObjColorIconsIS = uncachedBaseObjColorIconsIS_.cacheIcons(BuildingObject.BaseObjColor.values());
		
		IconSource<BuildingObject.BaseObjAppearance> uncachedBaseObjAppearanceIconsIS = new IconSource<>(97,97,-1);
		uncachedBaseObjAppearanceIconsIS.readIconsFromResource("/images/FarbCodes.Gestaltung.png");
		BaseObjAppearanceIconsIS = uncachedBaseObjAppearanceIconsIS.cacheIcons(BuildingObject.BaseObjAppearance.values());
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
	static Vector<String> getSorted(Collection<String> collection) {
		Vector<String> vec = new Vector<>(collection);
		vec.sort(Comparator.<String,String>comparing(String::toLowerCase).thenComparing(Comparator.naturalOrder()));
		return vec;
	}
	protected static void markSelectedObjectsInUniverse(JTable table, AddressdableObject[] objs, UniversePanel universePanel) {
		int[] rowsV = table.getSelectedRows();
		for (int rowV:rowsV) {
			int rowM = table.convertRowIndexToModel(rowV);
			if (0<=rowM && rowM<objs.length)
				universePanel.markAddress(objs[rowM]);
		}
	}
	protected static void markSelectedObjectsInUniverse(JTable table, Vector<? extends AddressdableObject> objs, UniversePanel universePanel) {
		int[] rowsV = table.getSelectedRows();
		for (int rowV:rowsV) {
			int rowM = table.convertRowIndexToModel(rowV);
			if (0<=rowM && rowM<objs.size())
				universePanel.markAddress(objs.get(rowM));
		}
	}
	protected static void markSelectedAddressesInUniverse(JTable table, Vector<UniverseAddress> objs, UniversePanel universePanel) {
		int[] rowsV = table.getSelectedRows();
		for (int rowV:rowsV) {
			int rowM = table.convertRowIndexToModel(rowV);
			if (0<=rowM && rowM<objs.size())
				universePanel.markAddress(objs.get(rowM));
		}
	}

	private static JComponent createLabeledScrollPane(String label, Component view) {
		JScrollPane scrollPane = new JScrollPane(view);
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder(label));
		panel.add(scrollPane,BorderLayout.CENTER);
		return panel;
	}

	private static class MarkAddressesAddOn {
		
		private AddressdableObject[] objArr;
		private Vector<? extends AddressdableObject> objVec;
		private Vector<UniverseAddress> uaVec;
		private UniverseAddress clickedUA = null;

		MarkAddressesAddOn() {
			this.objArr = null;
			this.objVec = null;
		}
		private MarkAddressesAddOn setData(AddressdableObject[] objArr) {
			this.uaVec  = null;
			this.objArr = objArr;
			this.objVec = null;
			return this;
		}
		private MarkAddressesAddOn setData(Vector<? extends AddressdableObject> objVec) {
			this.uaVec  = null;
			this.objArr = null;
			this.objVec = objVec;
			return this;
		}
		private MarkAddressesAddOn setUaVec(Vector<UniverseAddress> uaVec) {
			this.uaVec  = uaVec;
			this.objArr = null;
			this.objVec = null;
			return this;
		}
		
		MarkAddressesAddOn addTo(SimplifiedTable<?> table, UniversePanel universePanel, Universe universe) {
			JMenuItem miMarkInUniverse, miMarkSelectedInUniverse;
			JPopupMenu contextMenu = table.getContextMenu();
			contextMenu.addSeparator();
			contextMenu.add(miMarkInUniverse         = Gui.createMenuItem("Mark Address of Clicked Row in \"Known Universe\"",e->universePanel.markAddress(clickedUA)));
			contextMenu.add(miMarkSelectedInUniverse = Gui.createMenuItem("Mark Addresses of Selected Rows in \"Known Universe\"",e->markSelectedAddressesInUniverse(table,universePanel)));
			table.addContextMenuInvokeListener((rowV, columnV) -> {
				
				int[] selectedRows = table.getSelectedRows();
				miMarkSelectedInUniverse.setEnabled(selectedRows.length>0 && haveData());
				miMarkSelectedInUniverse.setText(String.format("Mark Addresses of %sSelected Row%s in \"Known Universe\"", selectedRows.length==0?"":selectedRows.length+" ", selectedRows.length==1?"":"s" ));
				
				int rowM = table.convertRowIndexToModel(rowV);
				if (haveData() && 0<=rowM && rowM<getDataLength()) {
					if (isUaVec()) {
						clickedUA = getUA(rowM);
					} else {
						AddressdableObject clickedRow = getObject(rowM);
						clickedUA = clickedRow!=null ? clickedRow.getUniverseAddress() : null;
					}
					if (clickedUA!=null) {
						String name = clickedUA.getVerboseNameInOneLine(universe, 1);
						miMarkInUniverse.setEnabled(true);
						miMarkInUniverse.setText(String.format("Mark \"%s\" in \"Known Universe\"", name.trim()));
						return;
					}
				}
				clickedUA = null;
				miMarkInUniverse.setEnabled(false);
				miMarkInUniverse.setText("Mark Address of Clicked Row in \"Known Universe\"");
			});
			
			return this;
		}
		
		private boolean haveData() {
			return (objArr!=null || objVec!=null || uaVec!=null);
		}
		private boolean isUaVec() {
			return uaVec!=null;
		}
	
		private UniverseAddress getUA(int rowM) {
			if (uaVec!=null) return uaVec.get(rowM);
			return null;
		}
		private AddressdableObject getObject(int rowM) {
			if (objArr!=null) return objArr[rowM];
			if (objVec!=null) return objVec.get(rowM);
			return null;
		}

		private int getDataLength() {
			if (objArr!=null) return objArr.length;
			if (objVec!=null) return objVec.size();
			if (uaVec !=null) return uaVec.size();
			return 0;
		}

		private void markSelectedAddressesInUniverse(JTable table, UniversePanel universePanel) {
			if (objArr!=null) SimplePanels.markSelectedObjectsInUniverse  (table,objArr,universePanel);;
			if (objVec!=null) SimplePanels.markSelectedObjectsInUniverse  (table,objVec,universePanel);;
			if (uaVec !=null) SimplePanels.markSelectedAddressesInUniverse(table,uaVec ,universePanel);;
		}
	}

	public static class VerySimpleTableTabPanel<DataType> extends SaveGameViewTabPanel {
		private static final long serialVersionUID = -9113552065575773276L;
		protected VerySimpleTable<DataType> table;
	
		public VerySimpleTableTabPanel(SaveGameData data, String tableName, boolean disableAutoResize, boolean installDebugContextMenu, boolean useRowSorter, DataType[] tableData, VerySimpleTable.ColumnID<DataType>[] tableColumns) {
			this(data, tableName, new VerySimpleTable<DataType>(tableName, disableAutoResize, installDebugContextMenu, useRowSorter, tableData, tableColumns));
		}
		public VerySimpleTableTabPanel(SaveGameData data, String tableName, boolean disableAutoResize, boolean installDebugContextMenu, boolean useRowSorter, Vector<DataType> tableData, VerySimpleTable.ColumnID<DataType>[] tableColumns) {
			this(data, tableName, new VerySimpleTable<DataType>(tableName, disableAutoResize, installDebugContextMenu, useRowSorter, tableData, tableColumns));
		}
		public VerySimpleTableTabPanel(SaveGameData data, String tableName, DataType[] tableData, VerySimpleTable.ColumnID<DataType>[] tableColumns) {
			this(data, tableName, new VerySimpleTable<DataType>(tableName, tableData, tableColumns));
		}
		public VerySimpleTableTabPanel(SaveGameData data, String tableName, Vector<DataType> tableData, VerySimpleTable.ColumnID<DataType>[] tableColumns) {
			this(data, tableName, new VerySimpleTable<DataType>(tableName, tableData, tableColumns));
		}
		public VerySimpleTableTabPanel(SaveGameData data, String tableName, boolean disableAutoResize, boolean installDebugContextMenu, boolean useRowSorter) {
			this(data, tableName, new VerySimpleTable<DataType>(tableName, disableAutoResize, installDebugContextMenu, useRowSorter));
		}
		public VerySimpleTableTabPanel(SaveGameData data, String tableName) {
			this(data, tableName, new VerySimpleTable<DataType>(tableName));
		}
		public VerySimpleTableTabPanel(SaveGameData data, String tableName, VerySimpleTable<DataType> table) {
			super(data);
			this.table = table;
			JScrollPane tableScrollPane = new JScrollPane(table);
			add(tableScrollPane,BorderLayout.CENTER);
		}
		public void setData(Vector<DataType> tableData, VerySimpleTable.ColumnID<DataType>[] tableColumns) {
			table.setData(tableData, tableColumns);
		}
	}

	public static class DeObfuscatorUsagePanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = -1827760869937255459L;

		public DeObfuscatorUsagePanel(SaveGameData data) {
			super(data);
			
			JTextArea textArea = new JTextArea();
			textArea.setEditable(false);
			
			LocalTableModel tableModel = new LocalTableModel();
			SimplifiedTable<ColumnID> table = new SimplifiedTable<>("DeObfuscatorUsageTable",tableModel,true,SaveViewer.DEBUG,true);
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION, true);
			table.getSelectionModel().addListSelectionListener(e->{
				textArea.setText("");
				int selectedRowV = table.getSelectedRow();
				if (selectedRowV<0) return;
				
				String originalStr = tableModel.getValue(table.convertRowIndexToModel(selectedRowV));
				HashSet<String> pathSet = this.data.deObfuscatorUsage.get(originalStr);
				if (pathSet!=null) {
					Vector<String> paths = getSorted(pathSet); 
					for (String str:paths)
						textArea.append(str+"\r\n");
				}
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
		
		private static class ColumnID extends VerySimpleTable.ColumnID<SaveGameData.Frigate> {
			public ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, BiFunction<SaveGameData.Frigate, Integer, Object> getValue) {
				super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
			}
			public ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, Function<SaveGameData.Frigate, Object> getValue) {
				super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
			}
		}

		private JTextArea textArea;
		private Frigate selected;
		private int selectedRow;
		
		public FrigatesPanel(SaveGameData data, Window mainWindow) {
			super(data);
			textArea = new JTextArea();
			selected = null;
			selectedRow = -1;
			
			VerySimpleTable<SaveGameData.Frigate> table = new VerySimpleTable<SaveGameData.Frigate>("FrigatesTable",true,SaveViewer.DEBUG,true, data.frigates, new ColumnID[] {
				new ColumnID("#"               ,   Integer.class, 35, -1,  35,  35, (fr,row)->row+1),
				new ColumnID("Name"            ,    String.class, 35, -1, 180, 180, (fr,row)->fr.name==null || fr.name.isEmpty() ? "<Frigate "+(row+1)+">" : fr.name),
				new ColumnID("Ship Type"       ,    String.class, 35, -1,  80,  80, fr->fr.shipType),
				new ColumnID("Crew Race"       ,    String.class, 35, -1,  80,  80, fr->fr.crewRace),
				new ColumnID("Aquired"         , TimeStamp.class, 35, -1, 120, 120, fr->fr.aquired),
				new ColumnID("Fights"          ,      Long.class, 35, -1,  70,  70, fr->fr.successfulFights),
				new ColumnID("Expeditions"     ,      Long.class, 35, -1,  70,  70, fr->fr.expeditions),
				new ColumnID("Damages"         ,      Long.class, 35, -1,  70,  70, fr->fr.damages),
				new ColumnID("Fuel Consumption",    String.class, 35, -1,  70,  70, fr->fr.fuelConsumption==null ? "" : fr.fuelConsumption+" t / 250 ly"),
				new ColumnID("Combat"          ,      Long.class, 35, -1,  50,  50, fr->fr.combatValue),
				new ColumnID("Exploration"     ,      Long.class, 35, -1,  50,  50, fr->fr.explorationValue),
				new ColumnID("Mining"          ,      Long.class, 35, -1,  50,  50, fr->fr.miningValue),
				new ColumnID("Diplomacy"       ,      Long.class, 35, -1,  50,  50, fr->fr.diplomacyValue),
				new ColumnID("[Sum]"           ,      Long.class, 35, -1,  50,  50, fr->SaveGameData.sum(fr.combatValue,fr.explorationValue,fr.miningValue,fr.diplomacyValue)),
				new ColumnID("Modifications"   ,    String.class, 35, -1,  50,  50, fr->showValue(fr.modifications)),
				new ColumnID("Cur.Dam."        ,    String.class, 35, -1,  50,  50, fr->fr.damageValue==null || fr.damageValue==0?"":"damaged ("+fr.damageValue+")"),
				new ColumnID("ModelSeed"       ,    String.class, 35, -1, 130, 130, fr->fr.modelSeed==null ? null : fr.modelSeed.getSeedStr()),
				new ColumnID("HomeSeed"        ,    String.class, 35, -1, 130, 130, fr->fr.homeSeed ==null ? null : fr.homeSeed .getSeedStr()),
				new ColumnID("Unidentified"    ,    String.class, 35, -1,  70,  70, fr->fr.unidentified.getUnidentifiedLongs()),
			});
			
			
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION, true);
			table.addSelectionListener((fr,row)->{
				selected = fr;
				selectedRow = row==null?-1:row;
				showValues();
			});
			
			JPopupMenu contextMenu = new JPopupMenu();
			contextMenu.add(Gui.createMenuItem("Edit Modicifations", e->{
				if (selected==null) return;
				
				Vector<Frigate.EditableModification> editableMods = new Vector<>();
				for (Frigate.Modification mod:selected.modifications) {
					if (mod instanceof Frigate.EditableModification)
						editableMods.add((Frigate.EditableModification)mod);
				}
				if (!editableMods.isEmpty()) {
					EditNewMods dlg = new EditNewMods(mainWindow, "Edit Modifications", editableMods, ()->showValues());
					SwingUtilities.invokeLater(() -> {
						dlg.showDialog();
						Frigate.EditableModification.saveKnownEditableModsToFile();
						table.getModel_SimplifiedTableModel().fireTableUpdate();
					});
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
		
		private String showValue(Vector<Frigate.Modification> modifications) {
			int i=0;
			for (Frigate.Modification mod:modifications)
				if (mod instanceof Frigate.EditableModification && ((Frigate.EditableModification)mod).isUndefined()) i++;
			return modifications.size()+(i==0?"":(" ("+i+")"));
		}

		private void showValues() {
			textArea.setText("");
			if (selected==null) return;
			
			appendln();
			appendln("Name      : %s", selected.name     ==null ? "" : selected.name!=null && selected.name.isEmpty() ? "<Frigate "+(selectedRow+1)+">" : selected.name);
			appendln("Ship Type : %s", selected.shipType ==null ? "" : selected.shipType);
			appendln("Crew Race : %s", selected.crewRace ==null ? "" : selected.crewRace);
			appendln("Model Seed: %s", selected.modelSeed==null ? "<null>" : selected.modelSeed.getSeedStr());
			appendln("Home Seed : %s", selected.homeSeed ==null ? "<null>" : selected. homeSeed.getSeedStr());
			if (selected.homeSeed!=null) {
				UniverseAddress ua = selected.homeSeed.getSeedAsUniverseAddress();
				if (ua!=null) {
					Vector<String> verboseName = ua.getVerboseName(data.universe);
					for (String str:verboseName) appendln("            %s", str);
				}
			}
			appendln();
			appendln("When Aquired     : %s", selected.aquired         ==null ? "" : selected.aquired          );
			appendln("Expeditions      : %s", selected.expeditions     ==null ? "" : selected.expeditions      );
			appendln("Successful Fights: %s", selected.successfulFights==null ? "" : selected.successfulFights );
			appendln("Failed Fights    : %s", selected.failedFights_Q  ==null ? "" : selected.failedFights_Q   );
			appendln("Damages          : %s", selected.damages         ==null ? "" : selected.damages          );
			appendln("Fuel Consumption : %s", selected.fuelConsumption ==null ? "" : String.format("%d t / 250 ly", selected.fuelConsumption) );
			appendln("Fuel Capacity    : %s", selected.fuelCapacity_Q  ==null ? "" : selected.fuelCapacity_Q   );
			appendln("Speed            : %s", selected.speed_Q         ==null ? "" : selected.speed_Q          );
			appendln();
			appendln("Combat      : %s ", selected.combatValue     ==null ? "" : selected.combatValue      );
			appendln("Exploration : %s ", selected.explorationValue==null ? "" : selected.explorationValue );
			appendln("Mining      : %s ", selected.miningValue     ==null ? "" : selected.miningValue      );
			appendln("Diplomacy   : %s ", selected.diplomacyValue  ==null ? "" : selected.diplomacyValue   );
			appendln();
			appendln("Modifications :");
			for (Frigate.Modification mod:selected.modifications) {
				appendln("    %s:", mod.getLabel());
				appendln("        %s", mod.getValue() );
			}
			appendln();
			appendln("Unidentified Values :");
			appendln("    %s", selected.unidentified.getUnidentifiedLongs());
		}
		
		private void appendln() {
			appendln("");
		}
		private void appendln(String format, Object...objects) {
			textArea.append(String.format(format+"\r\n", objects));
		}
		
		private static class EditNewMods extends StandardDialog {
			private static final long serialVersionUID = -6893651224062554261L;

			public EditNewMods(Window parent, String title, Vector<Frigate.EditableModification> editableMods, Runnable updateTask) {
				super(parent, title);
				
				JComboBox<Frigate.EditableModification> cmbBxEditableMods = new JComboBox<>(editableMods);
				JTextField txtfldLabel = Gui.createTextField("", (String str)->{int i=cmbBxEditableMods.getSelectedIndex(); if (i>=0) { editableMods.get(i).label = str; if (updateTask!=null) updateTask.run(); } });
				JTextField txtfldValue = Gui.createTextField("", (String str)->{int i=cmbBxEditableMods.getSelectedIndex(); if (i>=0) { editableMods.get(i).value = str; if (updateTask!=null) updateTask.run(); } });
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
				buttonPanel.add(Gui.createButton("Close", e->closeDialog()),BorderLayout.EAST);
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
	
	
	public static class FrigateMissionsPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = -2373140421444054954L;
		
		private JTextArea textArea;
		
		private static class MissionTaskColumnID extends VerySimpleTable.ColumnID<FrigateMissionTask> {
			public MissionTaskColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, BiFunction<FrigateMissionTask, Integer, Object> getValue) {
				super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
			}
			public MissionTaskColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, Function<FrigateMissionTask, Object> getValue) {
				super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
			}
		}
		
		private static class MissionColumnID extends VerySimpleTable.ColumnID<FrigateMission> {
			public MissionColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, BiFunction<FrigateMission, Integer, Object> getValue) {
				super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
			}
			public MissionColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, Function<FrigateMission, Object> getValue) {
				super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
			}
		}

		public FrigateMissionsPanel(SaveGameData data, Window mainWindow) {
			super(data);
			textArea = new JTextArea();
			
			VerySimpleTable<FrigateMissionTask> missionTasksTable = new VerySimpleTable<FrigateMissionTask>("FrigateMissionTasksTable",true,SaveViewer.DEBUG,true);
			VerySimpleTable<FrigateMission>     missionsTable     = new VerySimpleTable<FrigateMission>("FrigateMissionsTable",true,SaveViewer.DEBUG,true,data.frigateMissions,new MissionColumnID[] {
				new MissionColumnID("Name"           ,    String.class, 35, -1, 105, 105, (frm,row)->frm.name!=null && frm.name.isEmpty() ? "<Frigate Mission "+(row+1)+">" : frm.name),
				new MissionColumnID("Type"           ,    String.class, 35, -1,  60,  60, frm->frm.missionType),
				new MissionColumnID("Distance"       ,    String.class, 35, -1,  60,  60, frm->frm.distance),
				new MissionColumnID("Start Time"     , TimeStamp.class, 35, -1, 120, 120, frm->frm.startTime),
				new MissionColumnID("Last Event"     , TimeStamp.class, 35, -1, 120, 120, frm->frm.timeOfLastEvent),
				new MissionColumnID("UniverseAddress",    String.class, 35, -1, 160, 160, frm->frm.universeAddress==null ? "" : frm.universeAddress.getCoordinates()),
				new MissionColumnID("Planet / System",    String.class, 35, -1, 420, 420, frm->getPlanetOrSystem(frm.universeAddress)),
				new MissionColumnID("Seed"           , SeedValue.class, 35, -1, 160, 160, frm->frm.seed),
				new MissionColumnID("Values 1"       ,    String.class, 35, -1,  90,  90, frm->frm.missionValues1),
				new MissionColumnID("Values 2"       ,    String.class, 35, -1,  90,  90, frm->frm.missionValues2),
				new MissionColumnID("Position 1"     ,    String.class, 35, -1, 130, 130, frm->frm.position1),
				new MissionColumnID("Position 2"     ,    String.class, 35, -1, 130, 130, frm->frm.position2),
				new MissionColumnID("Some IDs"       ,    String.class, 35, -1,  90,  90, frm->frm.someIDs),
				new MissionColumnID("Progress"       ,    Double.class, 35, -1,  60,  60, frm->frm.progress),
					
				new MissionColumnID("ID1"   ,  String.class, 35, -1,  35,  35, frm->{ return frm.ID1_3oW;    }),
				new MissionColumnID("Int1"  ,    Long.class, 35, -1,  35,  35, frm->{ return frm.int1__DC;   }),
				new MissionColumnID("Int2"  ,    Long.class, 35, -1,  35,  35, frm->{ return frm.int2_U87;   }),
				new MissionColumnID("Int3"  ,    Long.class, 35, -1,  35,  35, frm->{ return frm.int3_G_H;   }),
				new MissionColumnID("Int4"  ,    Long.class, 35, -1,  35,  35, frm->{ return frm.int4_omN;   }),
				new MissionColumnID("Bool1" , Boolean.class, 35, -1,  50,  50, frm->{ return frm.bool1_b78;  }),
				new MissionColumnID("Array1",  String.class, 35, -1,  50,  50, frm->{ return frm.array1_WZs; }),
				new MissionColumnID("Array2",  String.class, 35, -1,  50,  50, frm->{ return frm.array2_1xe; }),
			});
			
			MissionTaskColumnID[] missionTaskColumnIDs = new MissionTaskColumnID[] {
				new MissionTaskColumnID("#"              ,   Integer.class, 35, -1,  35,  35, (frmt,row)->{ return row+1; }),
				new MissionTaskColumnID("Type"           ,    String.class, 35, -1, 120, 120, frmt->{ return frmt.missionTaskType;                                                             }),
				new MissionTaskColumnID("Other ID"       ,    String.class, 35, -1,  60,  60, frmt->{ return frmt.otherID;                                                                     }),
				new MissionTaskColumnID("Completed"      ,   Boolean.class, 35, -1,  65,  65, frmt->{ return frmt.completed;                                                                   }),
				new MissionTaskColumnID("UniverseAddress",    String.class, 35, -1, 160, 160, frmt->{ if (frmt.universeAddress==null) return ""; return frmt.universeAddress.getCoordinates(); }),
				new MissionTaskColumnID("Planet / System",    String.class, 35, -1, 420, 420, frmt->{ return getPlanetOrSystem(frmt.universeAddress);                                          }),
				new MissionTaskColumnID("Seed"           , SeedValue.class, 35, -1, 160, 160, frmt->{ return frmt.seed;                                                                        }),
				new MissionTaskColumnID("Array1"         ,    String.class, 35, -1,  50,  50, frmt->{ return frmt.array1_iaH; }),
				new MissionTaskColumnID("Array2"         ,    String.class, 35, -1,  50,  50, frmt->{ return frmt.array2_QJG; }),
				new MissionTaskColumnID("Array3"         ,    String.class, 35, -1,  50,  50, frmt->{ return frmt.array3_fe2; }),
				new MissionTaskColumnID("Bool2"          ,   Boolean.class, 35, -1,  50,  50, frmt->{ return frmt.bool2_fvN;  }),
				new MissionTaskColumnID("Bool3"          ,   Boolean.class, 35, -1,  50,  50, frmt->{ return frmt.bool3_8GD;  }),
			};
			
			missionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION, true);
			missionsTable.addSelectionListener((frigateMission,rowIndex)->{
				showValues(frigateMission,rowIndex==null?-1:rowIndex);
				missionTasksTable.setData(frigateMission.missionTasks, missionTaskColumnIDs);
			});
			
			JScrollPane missionsTableScrollPane = new JScrollPane(missionsTable);
			missionsTableScrollPane.setPreferredSize(new Dimension(100,150));
			
			JScrollPane textareaScrollPane = new JScrollPane(textArea);
			textareaScrollPane.setPreferredSize(new Dimension(300,50));
			
			JScrollPane missionsTasksTableScrollPane = new JScrollPane(missionTasksTable);
			missionsTasksTableScrollPane.setPreferredSize(new Dimension(50,50));
			
			add(missionsTableScrollPane,BorderLayout.NORTH);
			add(textareaScrollPane,BorderLayout.WEST);
			add(missionsTasksTableScrollPane,BorderLayout.CENTER);
			
		}

		private void showValues(FrigateMission frm, int rowIndex) {
			textArea.setText("");
			if (frm==null) return;
			
			appendln("Name     : %s", frm.name       ==null?"":(frm.name!=null && frm.name.isEmpty()?("<FrigateMission "+(rowIndex+1)+">"):frm.name));
			appendln("Type     : %s", frm.missionType==null?"":frm.missionType);
			appendln("Distance : %s", frm.distance   ==null?"":frm.distance);
			appendln();
			appendln("Start      : %s", frm.startTime      ==null?"":frm.startTime);
			appendln("Last Event : %s", frm.timeOfLastEvent==null?"":frm.timeOfLastEvent);
			appendln();
			appendln("Universe Address:");
			appendln("    %s", frm.universeAddress==null?"":frm.universeAddress.getCoordinates() );
			appendln("    %s", getPlanetOrSystem(frm.universeAddress) );
			appendln("Seed : %s ", frm.seed==null?"":frm.seed );
			appendln();
			appendln("Mission Values:");
			appendln("    %s", frm.missionValues1==null?"":frm.missionValues1 );
			appendln("    %s", frm.missionValues2==null?"":frm.missionValues2 );
			appendln("Positions:");
			appendln("    %s", frm.position1==null?"":frm.position1 );
			appendln("    %s", frm.position2==null?"":frm.position2 );
			appendln("Some IDs : %s ", frm.someIDs==null?"":frm.someIDs );
			appendln("Progress : %s", frm.progress );
			appendln();
			appendln("Unidentified Values:");
			appendln("    ID1    : %s", frm.ID1_3oW   ==null?"":frm.ID1_3oW    );
			appendln("    Int1   : %s", frm.int1__DC  ==null?"":frm.int1__DC   );
			appendln("    Int2   : %s", frm.int2_U87  ==null?"":frm.int2_U87   );
			appendln("    Int3   : %s", frm.int3_G_H  ==null?"":frm.int3_G_H   );
			appendln("    Int4   : %s", frm.int4_omN  ==null?"":frm.int4_omN   );
			appendln("    Bool1  : %s", frm.bool1_b78 ==null?"":frm.bool1_b78  );
			appendln("    Array1 : %s", frm.array1_WZs==null?"":frm.array1_WZs );
			appendln("    Array2 : %s", frm.array2_1xe==null?"":frm.array2_1xe );
		}
		
		private void appendln() {
			appendln("");
		}
		private void appendln(String format, Object...objects) {
			textArea.append(String.format(format+"\r\n", objects));
		}

		private String getPlanetOrSystem(UniverseAddress universeAddress) {
			if (universeAddress ==null) return "";
			return universeAddress.getVerboseNameInOneLine(data.universe, 2);
		}
	}
	
	public static class DiscoveredDataAvailablePanel extends VerySimpleTableTabPanel<SaveGameData.DiscoveryData.AvailableData> {
		private static final long serialVersionUID = 2870833302184314416L;

		public DiscoveredDataAvailablePanel(SaveGameData data, UniversePanel universePanel) {
			super(data,"DDATable",true,SaveViewer.DEBUG,true,data.discoveryData.availableData,
				new ColumnID[] {
					new ColumnID("Timestamp"       , TimeStamp.class, 50,-1,140,140, availData->availData.TSrec),
					new ColumnID("Universe Address",    String.class, 50,-1,160,160, availData->availData.DD.UA==null ? "" : availData.DD.UA.getAddressStr()),
					new ColumnID("Universe Address",    String.class, 50,-1,160,160, availData->availData.DD.UA==null ? "" : availData.DD.UA.getCoordinates()),
					new ColumnID("Data Type"       ,    String.class, 50,-1, 90, 90, availData->availData.DD.DT==null ? "" : availData.DD.DT),
					new ColumnID("DD_VP"           ,    String.class, 50,-1,300,300, availData->availData.DD.VP==null ? "" : toHexArray(availData.DD.VP)),
				}
			);
			new MarkAddressesAddOn().setData(this.data.discoveryData.availableData).addTo(table, universePanel, this.data.universe);
		}
		
		private static class ColumnID extends VerySimpleTable.ColumnID<SaveGameData.DiscoveryData.AvailableData> {
			public ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, Function<SaveGameData.DiscoveryData.AvailableData, Object> getValue) {
				super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
			}
		}
	}

	public static class DiscoveredDataStoredPanel extends VerySimpleTableTabPanel<SaveGameData.DiscoveryData.StoreData> {
		private static final long serialVersionUID = 6619075068331735784L;

		public DiscoveredDataStoredPanel(SaveGameData data, UniversePanel universePanel) {
			super(data,"DDSTable",true,SaveViewer.DEBUG,true,data.discoveryData.storeData,
				new ColumnID[] {
					new ColumnID("DD_UA"  ,    String.class, 50,-1,160,160, storeData->storeData.DD.UA  ==null ? "" : storeData.DD.UA.getAddressStr()),
					new ColumnID("DD_UA"  ,    String.class, 50,-1,160,160, storeData->storeData.DD.UA  ==null ? "" : storeData.DD.UA.getCoordinates()),
					new ColumnID("DD_DT"  ,    String.class, 50,-1, 90, 90, storeData->storeData.DD.DT  ==null ? "" : storeData.DD.DT),
					new ColumnID("DD_VP"  ,    String.class, 50,-1,300,300, storeData->storeData.DD.VP  ==null ? "" : toHexArray(storeData.DD.VP)),
					new ColumnID("DM"     ,    String.class, 20,-1, 40, 40, storeData->storeData.DM     ==null ? "" : storeData.DM),
					new ColumnID("DM_CN"  ,    String.class, 50,-1, 80, 80, storeData->storeData.DM_CN  ==null ? "" : storeData.DM_CN),
					new ColumnID("OWS_LID",    String.class, 50,-1,120,120, storeData->storeData.OWS!=null && storeData.OWS.LID     ==null ? "" : storeData.OWS.LID),
					new ColumnID("OWS_UID",    String.class, 50,-1,120,120, storeData->storeData.OWS!=null && storeData.OWS.userID  ==null ? "" : storeData.OWS.userID),
					new ColumnID("OWS_USN",    String.class, 50,-1, 80, 80, storeData->storeData.OWS!=null && storeData.OWS.userName==null ? "" : storeData.OWS.userName),
					new ColumnID("OWS_TS" , TimeStamp.class, 50,-1,140,140, storeData->storeData.OWS==null ? null : storeData.OWS.timeStamp),
					new ColumnID("RID"    ,    String.class, 50,-1,350,350, storeData->storeData.RID    ==null ? "" : storeData.RID),
					new ColumnID("PTK"    ,    String.class, 20,-1, 40, 40, storeData->storeData.PTK    ==null ? "" : storeData.PTK),
				}
			);
			new MarkAddressesAddOn().setData(this.data.discoveryData.storeData).addTo(table, universePanel, this.data.universe);
		}
		
		private static class ColumnID extends VerySimpleTable.ColumnID<SaveGameData.DiscoveryData.StoreData> {
			public ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, Function<SaveGameData.DiscoveryData.StoreData, Object> getValue) {
				super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
			}
		}
	}

	public static class TeleportEndpointsPanel extends VerySimpleTableTabPanel<SaveGameData.TeleportEndpoints> {
		private static final long serialVersionUID = 3670607708610340039L;

		public TeleportEndpointsPanel(SaveGameData data, UniversePanel universePanel) {
			super(data,"TeleportEndpointsTable",true,SaveViewer.DEBUG,true,data.teleportEndpoints,
				new ColumnID[] {
					new ColumnID("Name"            , String.class, 40,-1,250,250, te->te.name),
					new ColumnID("Teleport Host"   , String.class, 40,-1,130,130, te->te.teleportHost   !=null ? te.teleportHost.label : te.teleportHostStr==null ? "" : "["+te.teleportHostStr+"]"),
					new ColumnID("Universe Address", String.class, 40,-1,160,160, te->te.universeAddress==null ? "" : te.universeAddress.getCoordinates()),
					new ColumnID("Planet / System" , String.class, 40,-1,420,420, te->te.universeAddress==null ? "" : te.universeAddress.getVerboseNameInOneLine(data.universe,2)),
					new ColumnID("GPS Coords (??)" , String.class, 40,-1,180,180, te->te.gpsCoords      ==null ? "" : te.gpsCoords.toString()),
					new ColumnID("Look At"         , String.class, 40,-1,170,170, te->te.lookAt         ==null ? "" : te.lookAt   .toString(" %1.4f ")),
					new ColumnID("Position"        , String.class, 40,-1,220,220, te->te.position       ==null ? "" : te.position .toString(" %1.2f ")),
					new ColumnID("Unknown"         , String.class, 40,-1,140,140, te->te.getUnknownValues()),
				}
			);
			new MarkAddressesAddOn().setData(this.data.teleportEndpoints).addTo(table, universePanel, this.data.universe);
		}
		
		private static class ColumnID extends VerySimpleTable.ColumnID<SaveGameData.TeleportEndpoints> {
			public ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, Function<SaveGameData.TeleportEndpoints, Object> getValue) {
				super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
			}
		}
	}
	
	public static class ExperimentalData {

		public static void addPanels(SaveGameViewTabGroupingPanel rawDataPanel, SaveGameData data, Window mainWindow, UniversePanel universePanel) {
			if (data.experimentalData.storedInteractions     !=null) rawDataPanel.addPanel("Stored Interactions",new StoredInteractionsPanel(data, universePanel));
			if (data.experimentalData.markerStack            !=null) rawDataPanel.addPanel("Marker Stack"       ,new MarkerStackPanel(data, universePanel));
			if (data.experimentalData.missionProgress        !=null) rawDataPanel.addPanel("Mission Progress"   ,new MissionProgressPanel(data, universePanel));
			if (data.experimentalData.data_Wu_.data          !=null) rawDataPanel.addPanel(data.experimentalData.data_Wu_.getTabTitel(),new DATA_Wu__Panel(data));
			if (data.experimentalData.data_EQt.data          !=null) rawDataPanel.addPanel(data.experimentalData.data_EQt.getTabTitel(),new DATA_EQt_Panel(data));
			if (data.experimentalData.data_m4I.data          !=null) rawDataPanel.addPanel(data.experimentalData.data_m4I.getTabTitel(),new DATA_m4I_Panel(data));
			if (data.experimentalData.maintenanceInteractions!=null) rawDataPanel.addPanel("MaintenanceInteractions"              , new MaintenanceInteractionsTablePanel(data,mainWindow,data.experimentalData.maintenanceInteractions));
			if (data.experimentalData.array_VhC              !=null) rawDataPanel.addPanel("MaintenanceInteractions \"[??? VhC]\"", new MaintenanceInteractionsTablePanel(data,mainWindow,data.experimentalData.array_VhC));
			if (data.experimentalData.array_wyZ              !=null) rawDataPanel.addPanel("PlayerStateData -> [??? wyZ]"         , new Data_wyZ_TablePanel(data,mainWindow,data.experimentalData.array_wyZ));
		}

		private static class Data_wyZ_TablePanel extends TableAndInventoryPanel<SaveGameData.ExperimentalData.Data_wyZ> {
			private static final long serialVersionUID = 3911814142801819114L;

			private static class ColumnID extends VerySimpleTable.ColumnID<SaveGameData.ExperimentalData.Data_wyZ> {
				public ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, Function<SaveGameData.ExperimentalData.Data_wyZ, Object> getValue) {
					super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
				}
				public ColumnID(VerySimpleTable.ColumnID<SaveGameData.ExperimentalData.MaintenanceInteraction> columnID) { super(columnID, item->item.data_YC); }
			}
			
			static final ColumnID[] columns = new ColumnID[] {
				new ColumnID("#"   , Integer.class,  20,-1,35,35, item->item.index),
				new ColumnID("Type",    Long.class,  20,-1,35,35, item->item.inventoryType),
				new ColumnID("X"   ,    Long.class,  20,-1,20,20, item->item.x_WX8),
				new ColumnID("Y"   ,    Long.class,  20,-1,20,20, item->item.y_WX8),
			};
			
			Data_wyZ_TablePanel(SaveGameData data, Window mainwindow, Vector<SaveGameData.ExperimentalData.Data_wyZ> dataVec) {
				super(data, mainwindow, "Data_wyZ_Table", "Data_wyZ", "No InventoryContainer", 0.5, dataVec, merge(columns,MaintenanceInteractionsTablePanel.columns));
			}

			private static ColumnID[] merge( ColumnID[] columns, MaintenanceInteractionsTablePanel.ColumnID[] maintIntColumns) {
				ColumnID[] newColumns = Arrays.copyOf(columns, columns.length+maintIntColumns.length);
				for (int i=0; i<maintIntColumns.length; i++) newColumns[columns.length+i] = new ColumnID(maintIntColumns[i]);
				return newColumns;
			}

			@Override protected Inventory getInventory(SaveGameData.ExperimentalData.Data_wyZ item) {
				return item==null ? null : item.data_YC==null ? null : item.data_YC.inventoryContainer;
			}
		}

		private static class MaintenanceInteractionsTablePanel extends TableAndInventoryPanel<SaveGameData.ExperimentalData.MaintenanceInteraction> {
			private static final long serialVersionUID = -3316149223413000850L;

			private static class ColumnID extends VerySimpleTable.ColumnID<SaveGameData.ExperimentalData.MaintenanceInteraction> {
				public ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, Function<SaveGameData.ExperimentalData.MaintenanceInteraction, Object> getValue) {
					super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
				}
			}

			private static String getInvStr(SaveGameData.ExperimentalData.MaintenanceInteraction item) {
				if (item==null) return null;
				if (item.inventoryContainer==null) return null;
				SaveGameData.Inventories.Inventory inv = item.inventoryContainer;
				if ( (inv.usedSlots==null || inv.usedSlots.intValue()==0) && (inv.validSlots==null || inv.validSlots.intValue()==0)) return null; 
				return String.format("%s/%s", inv.usedSlots==null?"??":inv.usedSlots, inv.validSlots==null?"??":inv.validSlots);
			}
			
			static final ColumnID[] columns = new ColumnID[] {
				new ColumnID("#"         ,   Integer.class,  20,-1, 35, 35, item->item.index),
				new ColumnID("Inventory" ,    String.class,  20,-1, 60, 60, item->getInvStr(item)),
				new ColumnID("lastUpdate", TimeStamp.class,  20,-1,250,250, item->item.lastUpdateTimestamp),
				new ColumnID("eyv"       , TimeStamp.class,  20,-1,250,250, item->item.timestamp_eyv),
				new ColumnID("FML"       , TimeStamp.class,  20,-1,250,250, item->item.timestamp_FML),
				new ColumnID("Flags"     ,    String.class,  20,-1, 60, 60, item->item.flags==null ? "" : String.format("0x%04X", item.flags)),
				new ColumnID("Damage Timers", String.class,  20,-1, 90, 90, item->item.damageTimers==null ? "" : item.damageTimers.toString()),
			};
			
			MaintenanceInteractionsTablePanel(SaveGameData data, Window mainwindow, Vector<SaveGameData.ExperimentalData.MaintenanceInteraction> dataVec) {
				super(data, mainwindow, "MaintenanceInteractionsTable", "MaintenanceInteractions", "No InventoryContainer", 0.5, dataVec, columns);
			}

			@Override protected Inventory getInventory(SaveGameData.ExperimentalData.MaintenanceInteraction item) {
				return item==null ? null : item.inventoryContainer;
			}
		}

		private static abstract class TableAndInventoryPanel<DataType> extends SaveGameViewTabPanel {
			private static final long serialVersionUID = 3484599291713688832L;
			private Component lastInventoryPanel;

			public TableAndInventoryPanel(SaveGameData data, Window mainwindow, String tableDebugLabel, String tableTitle, String noInvStr, double dividerLocation, Vector<DataType> dataVec, VerySimpleTable.ColumnID<DataType>[] columns) {
				super(data);
				
				VerySimpleTable<DataType> table = new VerySimpleTable<>(tableDebugLabel, true, SaveViewer.DEBUG, true, dataVec, columns);
				table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION, true);
				JScrollPane tableScrollPane = new JScrollPane(table);
				tableScrollPane.setBorder(BorderFactory.createTitledBorder(tableTitle));
				
				lastInventoryPanel = new JLabel(noInvStr);
				JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScrollPane, lastInventoryPanel);
				splitPane.setResizeWeight(1);
				splitPane.setDividerLocation(dividerLocation);
				
				table.addSelectionListener((item,index)->{
					int pos = splitPane.getDividerLocation();
					splitPane.remove(lastInventoryPanel);
					SaveGameData.Inventories.Inventory inv = item==null ? null : getInventory(item);
					if (inv!=null)
						splitPane.setRightComponent(lastInventoryPanel = new InventoriesPanel.InventoryPanel(mainwindow, inv, true, true, null));
					else
						splitPane.setRightComponent(lastInventoryPanel = new JLabel(noInvStr));
					splitPane.setDividerLocation(pos);
				});
				
				add(splitPane,BorderLayout.CENTER);
			}

			protected abstract SaveGameData.Inventories.Inventory getInventory(DataType item);
		}

		private static class MissionProgressPanel extends SaveGameViewTabPanel {
			private static final long serialVersionUID = -5481267548249025769L;
			
			private static class MissionColumnID extends VerySimpleTable.ColumnID<SaveGameData.ExperimentalData.MissionProgress.Mission> {
				public MissionColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, BiFunction<SaveGameData.ExperimentalData.MissionProgress.Mission, Integer, Object> getValue) {
					super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
				}
				public MissionColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, Function<SaveGameData.ExperimentalData.MissionProgress.Mission, Object> getValue) {
					super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
				}
			}
			
			private static class ParticipantColumnID extends VerySimpleTable.ColumnID<SaveGameData.ExperimentalData.MissionProgress.Participant> {
				public ParticipantColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, BiFunction<SaveGameData.ExperimentalData.MissionProgress.Participant, Integer, Object> getValue) {
					super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
				}
				public ParticipantColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, Function<SaveGameData.ExperimentalData.MissionProgress.Participant, Object> getValue) {
					super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
				}
			}
			public MissionProgressPanel(SaveGameData data, UniversePanel universePanel) {
				super(data);
				
				MissionColumnID[] missionColumns = new MissionColumnID[] {
						// [50, 129, 61, 120, 50, 96]
					new MissionColumnID("#"           , Integer.class, 20,-1, 30, 30, (mission,i)->i+1),
					new MissionColumnID("Mission"     ,  String.class, 20,-1,120,120, mission->mission.Mission),
					new MissionColumnID("Progress"    ,    Long.class, 20,-1, 60, 60, mission->mission.Progress),
					new MissionColumnID("Seed"        ,  String.class, 20,-1,120,120, mission->mission.Seed==null ? null : String.format("0x%016X", mission.Seed)),
					new MissionColumnID("Data"        ,    Long.class, 20,-1, 40, 40, mission->mission.Data),
					new MissionColumnID("Participants",  String.class, 20,-1,100,100, mission->mission.Participants==null ? "" : String.format("%d Participant(s)", mission.Participants.size())),
				};
				ParticipantColumnID[] participantColumns = new ParticipantColumnID[] {
					new ParticipantColumnID("UA"              , String.class,  20,-1,160,160, part->part.UA==null ? "" : part.UA.getCoordinates()),
					new ParticipantColumnID("Planet / System" , String.class,  20,-1,420,420, part->part.UA==null ? "" : part.UA.getVerboseNameInOneLine(data.universe, 2)),
					new ParticipantColumnID("BuildingSeed"    , String.class,  20,-1,130,130, part->part.BuildingSeed    ==null ? null : part.BuildingSeed.getSeedStr()),
					new ParticipantColumnID("BuildingLocation", String.class,  20,-1,220,220, part->part.BuildingLocation==null ? null : part.BuildingLocation.toString(" %1.2f ")),
					new ParticipantColumnID("ParticipantType" , String.class,  20,-1, 90, 90, part->part.ParticipantType),
				};
				
				VerySimpleTable<SaveGameData.ExperimentalData.MissionProgress.Mission> missionsTable =
						new VerySimpleTable<>("MissionProgress.MissionTable",true,SaveViewer.DEBUG,true,data.experimentalData.missionProgress,missionColumns);
				JScrollPane missionsTableScrollPane = new JScrollPane(missionsTable);
				missionsTableScrollPane.setBorder(BorderFactory.createTitledBorder("Missions"));
				
				VerySimpleTable<SaveGameData.ExperimentalData.MissionProgress.Participant> participantsTable =
						new VerySimpleTable<>("MissionProgress.Mission.ParticipantTable",true,SaveViewer.DEBUG,true);
				JScrollPane participantsTableScrollPane = new JScrollPane(participantsTable);
				TitledBorder participantsTableTitledBorder = BorderFactory.createTitledBorder("Mission[#].Participants");
				participantsTableScrollPane.setBorder(participantsTableTitledBorder);
				MarkAddressesAddOn participantsTableAddOn = new MarkAddressesAddOn().addTo(participantsTable, universePanel, this.data.universe);
				
				missionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION, true);
				missionsTable.addSelectionListener((mission,rowIndex)->{
					participantsTableTitledBorder.setTitle(String.format("Mission[%s].Participants", rowIndex==null || rowIndex<0 ? "#" : rowIndex+1));
					participantsTableScrollPane.repaint();
					Vector<SaveGameData.ExperimentalData.MissionProgress.Participant> participantsTableData = mission==null ? null : mission.Participants;
					participantsTable.setData(participantsTableData, participantColumns);
					participantsTableAddOn.setData(participantsTableData);
				});
				
				
				JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
				splitPane.setLeftComponent(missionsTableScrollPane);
				splitPane.setRightComponent(participantsTableScrollPane);
				
				add(splitPane,BorderLayout.CENTER);
			}
			
		}

		private static class DATA_Wu__Panel extends VerySimpleTableTabPanel<SaveGameData.ExperimentalData.DATA_Wu_.Data> {
			private static final long serialVersionUID = -7995410864996763679L;

			public DATA_Wu__Panel(SaveGameData data) {
				super(data,"DATA_Wu__Table",true,SaveViewer.DEBUG,true,data.experimentalData.data_Wu_.data,
					new ColumnID[] {
						new ColumnID("[2Fk]", String.class,  20,-1,250,250, item->item.value_2Fk),
						new ColumnID("[E=X]", String.class,  20,-1,160,160, item->item.value_E_X),
					}
				);
			}
			
			private static class ColumnID extends VerySimpleTable.ColumnID<SaveGameData.ExperimentalData.DATA_Wu_.Data> {
				public ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, Function<SaveGameData.ExperimentalData.DATA_Wu_.Data, Object> getValue) {
					super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
				}
			}
		}

		private static class DATA_EQt_Panel extends VerySimpleTableTabPanel<SaveGameData.ExperimentalData.DATA_EQt.Data> {
			private static final long serialVersionUID = 1083996963904847727L;

			public DATA_EQt_Panel(SaveGameData data) {
				super(data,"DATA_EQt_Table",true,SaveViewer.DEBUG,true,data.experimentalData.data_EQt.data,
					new ColumnID[] {
						new ColumnID("MissionID", String.class,  20,-1,250,250, item->item.MissionID),
						new ColumnID("[oF@]"    ,   Long.class,  20,-1,160,160, item->item.value_oF_),
					}
				);
			}
			
			private static class ColumnID extends VerySimpleTable.ColumnID<SaveGameData.ExperimentalData.DATA_EQt.Data> {
				public ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, Function<SaveGameData.ExperimentalData.DATA_EQt.Data, Object> getValue) {
					super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
				}
			}
		}

		private static class DATA_m4I_Panel extends VerySimpleTableTabPanel<SaveGameData.ExperimentalData.DATA_m4I.Data> {
			private static final long serialVersionUID = -2231796446086538543L;

			public DATA_m4I_Panel(SaveGameData data) {
				super(data,"DATA_m4I_Table",true,SaveViewer.DEBUG,true,data.experimentalData.data_m4I.data,
					new ColumnID[] {
						new ColumnID("Id"   , String.class,  20,-1,130,130, item->item.Id),
						new ColumnID("Type" , String.class,  20,-1,130,130, item->item.Type),
						new ColumnID("Value",   Long.class,  20,-1, 80, 80, item->item.Value),
					}
				);
			}
			
			private static class ColumnID extends VerySimpleTable.ColumnID<SaveGameData.ExperimentalData.DATA_m4I.Data> {
				public ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, Function<SaveGameData.ExperimentalData.DATA_m4I.Data, Object> getValue) {
					super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
				}
			}
		}

		private static class StoredInteractionsPanel extends SaveGameViewTabPanel {
			private static final long serialVersionUID = 1017824861605442560L;
		
			public StoredInteractionsPanel(SaveGameData data, UniversePanel universePanel) {
				super(data);
				
				VerySimpleTable<SaveGameData.ExperimentalData.StoredInteraction> storedIntTable;
				VerySimpleTable<SaveGameData.ExperimentalData.StoredInteraction.Interaction> intTable;
				
				InteractionColumnID[] intTableColumnIDs = new InteractionColumnID[] {
					new InteractionColumnID("#"               , Integer.class,  35,-1, 35, 35, i->i.index),
					new InteractionColumnID("GalacticAddress" ,  String.class,  80,-1,400,400, i->i.galacticAddress==null ? "" : i.galacticAddress.getVerboseNameInOneLine(data.universe,2)),
					new InteractionColumnID("Value"           ,  String.class,  65,-1,100,100, i->i.value          ==null ? "" : String.format("[0x%04X] %d", i.value, i.value) ),
					new InteractionColumnID("GPS Coords"      ,  String.class, 150,-1,250,250, i->i.gpsCoords      ==null ? "" : i.gpsCoords.toString()),
					new InteractionColumnID("Position"        ,  String.class, 150,-1,250,250, i->i.position       ==null ? "" : i.position.toString(" %1.2f ")),
				};
				
				intTable = new VerySimpleTable<SaveGameData.ExperimentalData.StoredInteraction.Interaction>(
					"Table[StoredInteraction.Interactions]",true,SaveViewer.DEBUG,true
				);
				
				storedIntTable = new VerySimpleTable<SaveGameData.ExperimentalData.StoredInteraction>(
					"Table[StoredInteractions]",true,SaveViewer.DEBUG,true,
					data.experimentalData.storedInteractions,
					new StoredInteractionColumnID[] {
						new StoredInteractionColumnID("#"           , Integer.class,  20,-1, 35, 35, si->si.index),
						new StoredInteractionColumnID("(int)Xf4"    , Integer.class,  20,-1, 50, 50, si->si.intXf4),
						new StoredInteractionColumnID("interactions", Integer.class,  20,-1, 70, 70, si->si.interactions==null ? 0 : si.interactions.size()),
					}
				);
				storedIntTable.computePreferredScrollableViewportSize(200,800);
				storedIntTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				
				MarkAddressesAddOn markAddressesAddOn = new MarkAddressesAddOn().addTo(intTable, universePanel, this.data.universe);
				storedIntTable.addSelectionListener((si,index)->{
					Vector<SaveGameData.ExperimentalData.StoredInteraction.Interaction> interactions = si==null ? null : si.interactions;
					intTable.setData(interactions, intTableColumnIDs);
					markAddressesAddOn.setData(interactions);
				});
				
				add(createLabeledScrollPane("Interactions"      , intTable      ), BorderLayout.CENTER);
				add(createLabeledScrollPane("StoredInteractions", storedIntTable), BorderLayout.WEST);
			}
			
			private static class StoredInteractionColumnID extends VerySimpleTable.ColumnID<SaveGameData.ExperimentalData.StoredInteraction> {
				public StoredInteractionColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, Function<SaveGameData.ExperimentalData.StoredInteraction, Object> getValue) {
					super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
				}
			}
			
			private static class InteractionColumnID extends VerySimpleTable.ColumnID<SaveGameData.ExperimentalData.StoredInteraction.Interaction> {
				public InteractionColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, Function<SaveGameData.ExperimentalData.StoredInteraction.Interaction, Object> getValue) {
					super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
				}
			}
		}

		private static class MarkerStackPanel extends VerySimpleTableTabPanel<SaveGameData.ExperimentalData.MarkerStack.Marker> {
			private static final long serialVersionUID = -2754433276487371566L;
			
			private static class ColumnID extends VerySimpleTable.ColumnID<SaveGameData.ExperimentalData.MarkerStack.Marker> {
				public ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, Function<SaveGameData.ExperimentalData.MarkerStack.Marker, Object> getValue) {
					super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
				}
			}
			public MarkerStackPanel(SaveGameData data, UniversePanel universePanel) {
				super(data,"MarkerStackTable",true,SaveViewer.DEBUG,true,data.experimentalData.markerStack, new ColumnID[] {
						// [50, 157, 124, 393, 134, 204, 84, 50, 116, 78, 94]
						new ColumnID("Table"           ,   Long.class,  20,-1, 40, 40, marker->marker.Table),
						new ColumnID("Event"           , String.class,  20,-1,160,160, marker->marker.Event),
						new ColumnID("MissionID"       , String.class,  20,-1,120,120, marker->marker.MissionID),
						new ColumnID("MissionSeed"     , String.class,  20,-1,130,130, marker->marker.MissionSeed==null ? null : String.format("0x%016X", marker.MissionSeed)),
						new ColumnID("ParticipantType" , String.class,  20,-1, 90, 90, marker->marker.ParticipantType),
						new ColumnID("Time"            , Double.class,  20,-1, 40, 40, marker->marker.Time),
						new ColumnID("GalacticAddress" , String.class,  20,-1,120,120, marker->marker.galacticAddress ==null ? null : marker.galacticAddress.getAddressStr()),
						new ColumnID("GalacticAddress" , String.class,  20,-1,400,400, marker->marker.galacticAddress ==null ? null : marker.galacticAddress.getVerboseNameInOneLine(data.universe, 2)),
						new ColumnID("BuildingSeed"    , String.class,  20,-1,130,130, marker->marker.BuildingSeed    ==null ? null : marker.BuildingSeed.getSeedStr()),
						new ColumnID("BuildingLocation", String.class,  20,-1,220,220, marker->marker.BuildingLocation==null ? null : marker.BuildingLocation.toString(" %1.2f ")),
						new ColumnID("BuildingClass"   , String.class,  20,-1,100,100, marker->marker.BuildingClass),
					});
				new MarkAddressesAddOn().setData(this.data.experimentalData.markerStack).addTo(table, universePanel, this.data.universe);
			}
		}
		
	}

	public static class AppearanceBlockContainerArrayPanel extends SaveGameViewPanelGroupingPanel {
		private static final long serialVersionUID = -695460646232684753L;
	
		AppearanceBlockContainerArrayPanel(SaveGameData data, SaveGameData.Appearances.BlockCArray value, int index) {
			this(data, value, index, AppearanceBlockContainerPanel::generateTitle);
		}
		AppearanceBlockContainerArrayPanel(SaveGameData data, SaveGameData.Appearances.BlockCArray value, int index, BiFunction<SaveGameData.Appearances.BlockContainer,Integer,String> getTitle) {
			super(data, false, true, value==null ? null : value.data, AppearanceBlockContainerPanel::new, getTitle);
		}
		
		public static String generateTitle(SaveGameData.Appearances.BlockCArray data, int index) {
			return String.format("%d", index+1);
		}
	}

	public static class AppearanceBlockContainerPanel extends AppearanceBlockPanel {
		private static final long serialVersionUID = 8497162844923811394L;
	
		AppearanceBlockContainerPanel(SaveGameData data, SaveGameData.Appearances.BlockContainer appearanceBlockContainer, int index) {
			super(data, appearanceBlockContainer==null ? null : appearanceBlockContainer.block, index);
		}
		
		public static String generateTitle(SaveGameData.Appearances.BlockContainer data, int index) {
			return ""+(index+1)+": "+generateTitle(data);
		}
		public static String generateTitle(SaveGameData.Appearances.BlockContainer data) {
			StringBuilder sb = new StringBuilder();
			sb.append("Appearance");
			if (data==null) sb.append(" <null>");
			else {
				if (data.id_VFd!=null) sb.append("\"").append(data.id_VFd).append("\"");
				addValuesToTitle(data.block, sb);
			}
			return sb.toString();
		}
		
	}

	public static class AppearanceBlockPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = -7367567415965481666L;
	
		private static class Array_SMP_ColumnID extends VerySimpleTable.ColumnID<String> {
			public Array_SMP_ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, BiFunction<String, Integer, Object> getValue) {
				super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
			}
		}
		private static class Array_Aak_ColumnID extends VerySimpleTable.ColumnID<SaveGameData.Appearances.Block.Object_Aak> {
			public Array_Aak_ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, BiFunction<SaveGameData.Appearances.Block.Object_Aak, Integer, Object> getValue) {
				super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
			}
		}
		private static class Array_T1_ColumnID extends VerySimpleTable.ColumnID<SaveGameData.Appearances.Block.Object_T1> {
			public Array_T1_ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, BiFunction<SaveGameData.Appearances.Block.Object_T1, Integer, Object> getValue) {
				super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
			}
		}
		private static class Array_gsg_ColumnID extends VerySimpleTable.ColumnID<SaveGameData.Appearances.Block.Object_gsg> {
			public Array_gsg_ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, BiFunction<SaveGameData.Appearances.Block.Object_gsg, Integer, Object> getValue) {
				super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
			}
		}
		
		public static String generateTitle(SaveGameData.Appearances.Block block, int index) {
			return ""+(index+1)+": "+generateTitle(block);
		}
		public static String generateTitle(SaveGameData.Appearances.Block block) {
			StringBuilder sb = new StringBuilder();
			sb.append("Appearance");
			addValuesToTitle(block, sb);
			return sb.toString();
		}
		public static void addValuesToTitle(SaveGameData.Appearances.Block block, StringBuilder sb) {
			if (block==null) sb.append(" <null>");
			else {
				if (block.index >=0   ) sb.append(String.format("[%d]", block.index+1));
				if (block.height!=null) sb.append(String.format(Locale.ENGLISH, "   height:%1.2f", block.height));
			}
		}
		
		private final SaveGameData.Appearances.Block block;
		private VerySimpleTable<String> array_SMP_Table;
		private VerySimpleTable<SaveGameData.Appearances.Block.Object_Aak> array_Aak_Table;
		private VerySimpleTable<SaveGameData.Appearances.Block.Object_T1>  array_T1_Table;
		private VerySimpleTable<SaveGameData.Appearances.Block.Object_gsg> array_gsg_Table;
	
		AppearanceBlockPanel(SaveGameData data, SaveGameData.Appearances.Block block, int index) {
			super(data);
			setLayout(new GridBagLayout());
			this.block = block;
			
			//setBorder(BorderFactory.createTitledBorder(generateTitle(appearanceBlock)));
			
			Vector<String>     array_SMP  = this.block==null ? null : this.block.array_SMP;
			Vector<SaveGameData.Appearances.Block.Object_Aak> colors_Aak = this.block==null ? null : this.block.colors_Aak;
			Vector<SaveGameData.Appearances.Block.Object_T1>  styles_T1  = this.block==null ? null : this.block.styles_T1;
			Vector<SaveGameData.Appearances.Block.Object_gsg> array_gsg  = this.block==null ? null : this.block.array_gsg;
			
			array_SMP_Table = new VerySimpleTable<>("AppearanceBlockPanel.array_SMP ", array_SMP , new Array_SMP_ColumnID[] {
					new Array_SMP_ColumnID("#"           , Integer.class, 20,-1, 30, 30, (str,i)->i+1),
					new Array_SMP_ColumnID("Component ID",  String.class, 20,-1,250,250, (str,i)->str)
			});
			array_Aak_Table = new VerySimpleTable<>("AppearanceBlockPanel.colors_Aak", colors_Aak, new Array_Aak_ColumnID[] {
					new Array_Aak_ColumnID("#1"   ,    Integer.class, 20,-1, 30, 30, (v,i)->i+1),
					new Array_Aak_ColumnID("#2"   ,    Integer.class, 20,-1, 30, 30, (v,i)->v==null ? null : v.index+1),
					new Array_Aak_ColumnID("Label",     String.class, 20,-1,140,140, (v,i)->v==null ? null : v.label_RVl),
					new Array_Aak_ColumnID("Type" ,     String.class, 20,-1, 80, 80, (v,i)->v==null ? null : v.type_Ty),
					new Array_Aak_ColumnID("Color",     String.class, 20,-1,140,140, (v,i)->v==null ? null : v.color_xEg==null ? "<null>" : v.color_xEg.toString(" %1.2f ")),
					new Array_Aak_ColumnID("Color", NamedColor.class, 20,-1,100,100, (v,i)->v==null ? null : v.itemColor),
			});
			array_T1_Table  = new VerySimpleTable<>("AppearanceBlockPanel.styles_T1" , styles_T1 , new Array_T1_ColumnID [] {
					new Array_T1_ColumnID("#1"   , Integer.class, 20,-1, 30, 30, (v,i)->i+1),
					new Array_T1_ColumnID("#2"   , Integer.class, 20,-1, 30, 30, (v,i)->v==null ? null : v.index+1),
					new Array_T1_ColumnID("Item" ,  String.class, 20,-1,100,100, (v,i)->v==null ? null : v.item_6c),
					new Array_T1_ColumnID("Style",  String.class, 20,-1, 90, 90, (v,i)->v==null ? null : v.style_Cv),
			});
			array_gsg_Table = new VerySimpleTable<>("AppearanceBlockPanel.colors_Aak", array_gsg , new Array_gsg_ColumnID[] {
					new Array_gsg_ColumnID("#1"    , Integer.class, 20,-1, 30, 30, (v,i)->i+1),
					new Array_gsg_ColumnID("#2"    , Integer.class, 20,-1, 30, 30, (v,i)->v==null ? null : v.index+1),
					new Array_gsg_ColumnID("ID"    ,  String.class, 20,-1, 90, 90, (v,i)->v==null ? null : v.id_tIm),
					new Array_gsg_ColumnID("Height",  String.class, 20,-1, 60, 60, (v,i)->v==null ? null : String.format(Locale.ENGLISH, "%1.5f", v.height)),
			});
			array_SMP_Table.computePreferredScrollableViewportSize(-1, computeHeight(array_SMP ));
			array_Aak_Table.computePreferredScrollableViewportSize(-1, computeHeight(colors_Aak));
			array_T1_Table .computePreferredScrollableViewportSize(-1, computeHeight(styles_T1 ));
			array_gsg_Table.computePreferredScrollableViewportSize(-1, computeHeight(array_gsg ));
			array_Aak_Table.setDefaultRenderer(NamedColor.class, new TableView.NamedColorRenderer(false));
			
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 1;
			c.weighty = 1;
			add(createLabeledScrollPane("Components" + (isEmpty(array_SMP ) ? " (empty)" : ""), array_SMP_Table),c);
			add(createLabeledScrollPane("Colors"     + (isEmpty(colors_Aak) ? " (empty)" : ""), array_Aak_Table),c);
			add(createLabeledScrollPane("Styles"     + (isEmpty(styles_T1 ) ? " (empty)" : ""), array_T1_Table ),c);
			add(createLabeledScrollPane("Sizes"      + (isEmpty(array_gsg ) ? " (empty)" : ""), array_gsg_Table),c);
		}
	
		private int computeHeight(Vector<?> array) {
			if (isEmpty(array)) return 50;
			return 120;
		}
		private boolean isEmpty(Vector<?> vec) {
			return vec==null || vec.isEmpty();
		}
		
		@Override
		public void updateContent() {
			array_SMP_Table.getModel_SimplifiedTableModel().fireTableUpdate();
			array_Aak_Table.getModel_SimplifiedTableModel().fireTableUpdate();
			array_T1_Table .getModel_SimplifiedTableModel().fireTableUpdate();
			array_gsg_Table.getModel_SimplifiedTableModel().fireTableUpdate();
		}
	}

	public static class VisitedSystemsPanel extends VerySimpleTableTabPanel<SaveGameData.VisitedSystems.VisitedSystem> {
		private static final long serialVersionUID = -3829006848316518198L;

		private static class ColumnID extends VerySimpleTable.ColumnID<SaveGameData.VisitedSystems.VisitedSystem> {
			public ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, Function<SaveGameData.VisitedSystems.VisitedSystem, Object> getValue) {
				super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
			}
		}

		public VisitedSystemsPanel(SaveGameData data, UniversePanel universePanel) {
			super(data, "VisitedSystemsTable", true,SaveViewer.DEBUG,true, data.visitedSystems, new ColumnID[] {
				new ColumnID("Address"        , String.class,  20,-1,140,140, sys->String.format("0x%016X", sys.addr)),
				new ColumnID("Extra"          , String.class,  20,-1, 70, 70, sys->String.format("0x%06X", sys.extra)),
				new ColumnID("UniverseAddress", String.class,  20,-1,140,140, sys->sys.ua==null ? null : sys.ua.getAddressStr()),
				new ColumnID("Coordinates"    , String.class,  20,-1,200,200, sys->sys.ua==null ? null : sys.ua.getCoordinates()),
				new ColumnID("Name"           , String.class,  20,-1,700,700, sys->sys.ua==null ? null : sys.ua.getVerboseNameInOneLine(data.universe, 2)),
			});
			new MarkAddressesAddOn().setData(this.data.visitedSystems).addTo(table, universePanel, this.data.universe);
		}
	}

	public static class CompanionsPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = 1816950969983315262L;

		private static class CompanionColumnID extends VerySimpleTable.ColumnID<SaveGameData.Companions.Companion> {
			public CompanionColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, BiFunction<SaveGameData.Companions.Companion, Integer, Object> getValue) {
				super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
			}
			public CompanionColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, Function<SaveGameData.Companions.Companion, Object> getValue) {
				super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
			}
		}
		private final VerySimpleTable<Companion> companionsTable;
		private final VerySimpleTable<Companion> eggsTable;
		private final SaveGameViewTabGroupingPanel equipmentPanel;

		public CompanionsPanel(SaveGameData data) {
			super(data);
			
			// [20, 110, 120, 400, 120, 110, 360, 75, 75, 46, 56, 132, 104, 123, 60, 60, 60, 120, 120, 120, 120, 115, 115, 115, 115, 125, 125, 120]
			CompanionColumnID[] blueprintColumns = new CompanionColumnID[] {
				new CompanionColumnID("#"              ,     Integer.class, 20,-1, 20, 20, (c,i)->i+1),
				new CompanionColumnID("Name"           ,      String.class, 20,-1,110,110, c->c.name),
				new CompanionColumnID("Type"           ,      String.class, 20,-1,120,120, c->c.type),
				new CompanionColumnID("Body Parts"     ,      String.class, 20,-1,400,400, c->c.bodyParts==null ? null : c.bodyParts.toString()),
				new CompanionColumnID("UniverseAddress",      String.class, 20,-1,120,120, c->c.universeAddress==null ? null : c.universeAddress.getAddressStr()),
				new CompanionColumnID("Coordinates"    ,      String.class, 20,-1,110,110, c->c.universeAddress==null ? null : c.universeAddress.getCoordinates()),
				new CompanionColumnID("Planet/System"  ,      String.class, 20,-1,360,360, c->c.universeAddress==null ? null : c.universeAddress.getVerboseNameInOneLine(data.universe, 2)),
				new CompanionColumnID("Origin Biome"   ,      String.class, 20,-1, 75, 75, c->c.originBiome),
				new CompanionColumnID("Animal Role"    ,      String.class, 20,-1, 75, 75, c->c.animalRole),
				new CompanionColumnID("Gr��e"          ,      String.class, 20,-1, 45, 45, c->c.height_m==null ? "<null>" : String.format(Locale.ENGLISH, "%1.2fm", c.height_m)),
				new CompanionColumnID("Hoffnung"       ,      String.class, 20,-1, 60, 60, c->c.hope    ==null ? "<null>" : String.format(Locale.ENGLISH, "%1.1f%%", c.hope*100)),
				new CompanionColumnID("Character 1"    ,      String.class, 20,-1,130,130, c->c.character==null ? "<null>" : SaveGameData.Companions.Companion.toCharacterStr(c.character.x, 0)),
				new CompanionColumnID("Character 2"    ,      String.class, 20,-1,105,105, c->c.character==null ? "<null>" : SaveGameData.Companions.Companion.toCharacterStr(c.character.y, 1)),
				new CompanionColumnID("Character 3"    ,      String.class, 20,-1,125,125, c->c.character==null ? "<null>" : SaveGameData.Companions.Companion.toCharacterStr(c.character.z, 2)),
				new CompanionColumnID("Bool(Q6I)"      ,      String.class, 20,-1, 60, 60, c->c.unknownBool_Q6I==null ? "<null>" : c.unknownBool_Q6I.booleanValue() ? "true" : "false"),
				new CompanionColumnID("Bool(eK9)"      ,      String.class, 20,-1, 60, 60, c->c.unknownBool_eK9==null ? "<null>" : c.unknownBool_eK9.booleanValue() ? "true" : "false"),
				new CompanionColumnID("Bool(WQX)"      ,      String.class, 20,-1, 60, 60, c->c.unknownBool_WQX==null ? "<null>" : c.unknownBool_WQX.booleanValue() ? "true" : "false"),
				new CompanionColumnID("Seed 1"         ,   SeedValue.class, 20,-1,120,120, c->c.seed1==null ? null : c.seed1.getSeedStr()),
				new CompanionColumnID("Seed 2"         ,   SeedValue.class, 20,-1,120,120, c->c.seed2==null ? null : c.seed2.getSeedStr()),
				new CompanionColumnID("Seed 3"         ,   SeedValue.class, 20,-1,120,120, c->c.seed3==null ? null : c.seed3.getSeedStr()),
				new CompanionColumnID("Seed 4"         ,   SeedValue.class, 20,-1,120,120, c->c.seed4==null ? null : c.seed4.getSeedStr()),
				new CompanionColumnID("Timestamp 1"    ,   TimeStamp.class, 50,-1,115,115, c->c.timestamp1==null ? null : c.timestamp1.setOutputType(TimeStamp.OutputType.Short_MonNumber)),
				new CompanionColumnID("Timestamp 2"    ,   TimeStamp.class, 50,-1,115,115, c->c.timestamp2==null ? null : c.timestamp2.setOutputType(TimeStamp.OutputType.Short_MonNumber)),
				new CompanionColumnID("Timestamp 3"    ,   TimeStamp.class, 50,-1,115,115, c->c.timestamp3==null ? null : c.timestamp3.setOutputType(TimeStamp.OutputType.Short_MonNumber)),
				new CompanionColumnID("Timestamp 4"    ,   TimeStamp.class, 50,-1,115,115, c->c.timestamp4==null ? null : c.timestamp4.setOutputType(TimeStamp.OutputType.Short_MonNumber)),
				new CompanionColumnID("Long(m9o)"      ,      String.class, 20,-1,125,125, c->c.unknownLong_m9o==null ? "<null>" : String.format("0x%016X", c.unknownLong_m9o)),
				new CompanionColumnID("Long(JrL)"      ,      String.class, 20,-1,125,125, c->c.unknownLong_JrL==null ? "<null>" : String.format("0x%016X", c.unknownLong_JrL)),
				new CompanionColumnID("Coords(IEo)"    , Coordinates.class, 20,-1,120,120, c->c.unknownCoords_IEo),
			};
			
			JPanel tablePanel = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 1; c.weighty = 1; c.gridwidth = 1; c.gridheight = 1; c.gridx = 0; c.gridy = 0;
			if (data.companions!=null) {
				companionsTable = addTable(tablePanel, c, "Companions", data.companions.companions, blueprintColumns);
				eggsTable       = addTable(tablePanel, c, "Eggs"      , data.companions.eggs      , blueprintColumns);
				if (data.companions.equipment!=null) {
					equipmentPanel = new SaveGameViewTabGroupingPanel(
							data, data.companions.equipment,
							(d,bca,i) -> new AppearanceBlockContainerArrayPanel(d,bca,i, SaveGameData.Companions::generateLabelForEquipmentBlock),
							SaveGameData.Companions::generateLabelForEquipment
					);
					equipmentPanel.setBorder(BorderFactory.createTitledBorder("Companion Equipment"));
					add(equipmentPanel,BorderLayout.CENTER);
				} else
					equipmentPanel = null;
			} else {
				companionsTable = null;
				eggsTable       = null;
				equipmentPanel  = null;
			}
			add(tablePanel,BorderLayout.NORTH);
		}
		
		@Override
		public void updateContent() {
			if (companionsTable!=null) companionsTable.getModel_SimplifiedTableModel().fireTableUpdate();
			if (eggsTable      !=null) eggsTable      .getModel_SimplifiedTableModel().fireTableUpdate();
			if (equipmentPanel !=null) equipmentPanel.updateContent();
		}
		
		private VerySimpleTable<SaveGameData.Companions.Companion> addTable(JPanel tablePanel, GridBagConstraints c, String label, SaveGameData.Companions.Companion[] data, CompanionColumnID[] columns) {
			VerySimpleTable<SaveGameData.Companions.Companion> table = null;
			if (data==null)
				tablePanel.add(createLabeledScrollPane(label+" <null>", new JLabel()),c);
			else {
				table = new VerySimpleTable<>(label+" Table",true,SaveViewer.DEBUG,true,data,columns);
				table.computePreferredScrollableViewportSize(800,130);
				tablePanel.add(createLabeledScrollPane(label, table),c);
			}
			c.gridy++;
			return table;
		}
	}

	public static class AppearancesPanel extends SaveGameViewTabGroupingPanel {
		private static final long serialVersionUID = 1495283607483088597L;
		public AppearancesPanel(SaveGameData data) {
			super(data);
			if (data.appearances.playerPresets!=null)
				addPanel("Player Presets",
					new SaveGameViewPanelGroupingPanel(
							data, false, true,
							data.appearances.playerPresets,
							AppearanceBlockPanel         ::new,
							SaveGameData.Appearances::generateLabelForPreset
					)
				);
			if (data.appearances.currentSets!=null)
				addPanel("Current Sets (Player/Vehicles)",
					new SaveGameViewPanelGroupingPanel(
							data, false, true,
							data.appearances.currentSets,
							AppearanceBlockContainerPanel::new,
							SaveGameData.Appearances::generateLabelForCurrentSet
					)
				);
		}
	}

	public static class AllBlueprintsPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = 3973411381549824254L;

		private static class BlueprintColumnID extends VerySimpleTable.ColumnID<GeneralizedID> {
			public BlueprintColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, Function<GeneralizedID, Object> getValue) {
				super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
			}
		}
		private static class RecipeColumnID extends VerySimpleTable.ColumnID<String> {
			public RecipeColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, Function<String, Object> getValue) {
				super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
			}
		}
		
		private final VerySimpleTable<GeneralizedID> techsTable;
		private final VerySimpleTable<GeneralizedID> productsTable;
		private final VerySimpleTable<GeneralizedID> quicksilversTable;
		private final VerySimpleTable<String> recipesTable;
		
		public AllBlueprintsPanel(SaveGameData data, Window mainWindow) {
			super(data);
			
			BlueprintColumnID[] blueprintColumns = new BlueprintColumnID[] {
				new BlueprintColumnID("ID"    , String.class, 20,-1,120,120, id->id.id),
				new BlueprintColumnID("Label" , String.class, 20,-1,220,220, id->id.label),
			};
			RecipeColumnID[] recipeColumns = new RecipeColumnID[] {
				new RecipeColumnID("ID"    , String.class, 20,-1,200,200, id->id),
			};
			
			JPanel tablePanel = new JPanel(new GridLayout(1,0));
			techsTable        = addTable(mainWindow, tablePanel, "Known Technologies"        , data.knownBlueprints.technologies, SaveGameData.KnownBlueprints.BlueprintList.Technologies, blueprintColumns);
			productsTable     = addTable(mainWindow, tablePanel, "Known Products"            , data.knownBlueprints.products    , SaveGameData.KnownBlueprints.BlueprintList.Products    , blueprintColumns);
			quicksilversTable = addTable(mainWindow, tablePanel, "Known Quicksilver Specials", data.knownBlueprints.quicksilvers, SaveGameData.KnownBlueprints.BlueprintList.Quicksilvers, blueprintColumns);
			recipesTable      = addTable(            tablePanel, "Known Recipes"             , data.knownBlueprints.recipes     , recipeColumns);
			add(tablePanel,BorderLayout.CENTER);
		}
		
		private VerySimpleTable<String> addTable(JPanel tablePanel, String label, Vector<String> data, RecipeColumnID[] columns) {
			VerySimpleTable<String> table = new VerySimpleTable<>(label+" Table",true,SaveViewer.DEBUG,true,data,columns);
			addTable(tablePanel, label, table);
			return table;
		}
		private VerySimpleTable<GeneralizedID> addTable(Window mainWindow, JPanel tablePanel, String label, Vector<GeneralizedID> data, SaveGameData.KnownBlueprints.BlueprintList listType, BlueprintColumnID[] columns) {
			IDbasedBlueprintsPanel table = new IDbasedBlueprintsPanel(mainWindow, label+" Table", data, listType, columns);
			addTable(tablePanel, label, table);
			return table;
		}
		private void addTable(JPanel tablePanel, String label, JTable table) {
			JScrollPane scrollPane = new JScrollPane(table);
			scrollPane.setBorder(BorderFactory.createTitledBorder(label));
			tablePanel.add(scrollPane);
		}
		
		private static class IDbasedBlueprintsPanel extends VerySimpleTable<GeneralizedID> {
			private static final long serialVersionUID = 3243669593802056512L;
			
			private GeneralizedID clickedID;
			private int clickedRow;
		
			IDbasedBlueprintsPanel(Window mainWindow, String label, Vector<GeneralizedID> data, SaveGameData.KnownBlueprints.BlueprintList listType, BlueprintColumnID[] columns) {
				super(label,true,SaveViewer.DEBUG,true,data,columns);
				clickedID = null;
				clickedRow = -1;
				
				JMenuItem miEditID;
				JPopupMenu contextMenu = getContextMenu();
				contextMenu.addSeparator();
				contextMenu.add(miEditID = Gui.createMenuItem("Edit ID", e->{
					if (clickedID==null || clickedRow<0) return;
					boolean wasChanged = GameInfos.EditIdDialog.showDialog(mainWindow, clickedID, listType.idMap==null ? null : listType.idMap.getTemplateList());
					if (wasChanged) {
						listType.idMap.saveIDsToFile();
						VerySimpleTableModel<GeneralizedID> model = getModel_VerySimpleTableModel();
						model.fireTableRowUpdate(clickedRow);
					}
				}));
				
				addContextMenuInvokeListener((rowV, columnV) -> {
					clickedRow = rowV<0 ? -1 : convertRowIndexToModel(rowV);
					clickedID = clickedRow<0 ? null : data.get(clickedRow);
					miEditID.setEnabled(clickedID!=null);
					if (clickedID==null) miEditID.setText("Edit ID");
					else miEditID.setText(String.format("Edit \"%s\"", clickedID.getName()));
				});
			}
		}

		@Override
		public void updateContent() {
			techsTable       .getModel_SimplifiedTableModel().fireTableUpdate();
			productsTable    .getModel_SimplifiedTableModel().fireTableUpdate();
			quicksilversTable.getModel_SimplifiedTableModel().fireTableUpdate();
			recipesTable     .getModel_SimplifiedTableModel().fireTableUpdate();
		}
	}

	public static class AtlasStationAdressDataPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = -3518416131249623285L;
		
		private static class ColumnID extends VerySimpleTable.ColumnID<SaveGameData.UniverseAddress> {
			public ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, Function<SaveGameData.UniverseAddress, Object> getValue) {
				super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
			}
		}
		public AtlasStationAdressDataPanel(SaveGameData data, UniversePanel universePanel) {
			super(data);
			
			ColumnID[] columns = new ColumnID[] {
				new ColumnID("Address"    , String.class,  50,-1,120,120, ua->ua==null ? null : ua.getAddressStr()),
				new ColumnID("Coordinates", String.class,  50,-1,200,200, ua->ua==null ? null : ua.getCoordinates()),
				new ColumnID("Name"       , String.class,  50,-1,700,700, ua->ua==null ? null : ua.getVerboseNameInOneLine(data.universe, 2)),
			};
			JPanel tablePanel = new JPanel(new GridLayout(0,1));
			addTable(tablePanel, data.       AtlasStationAdressData,        "AtlasStationAdressData", columns, universePanel);
			addTable(tablePanel, data.    NewAtlasStationAdressData,     "NewAtlasStationAdressData", columns, universePanel);
			addTable(tablePanel, data.VisitedAtlasStationsData     , "VisitedAtlasStationsData"     , columns, universePanel);
			add(tablePanel,BorderLayout.CENTER);
		}
		
		private void addTable(JPanel tablePanel, Vector<SaveGameData.UniverseAddress> data, String label, ColumnID[] columns, UniversePanel universePanel) {
			VerySimpleTable<SaveGameData.UniverseAddress> table = new VerySimpleTable<>(label+"Table",true,SaveViewer.DEBUG,true,data,columns);
			JScrollPane scrollPane = new JScrollPane(table);
			scrollPane.setBorder(BorderFactory.createTitledBorder(label));
			tablePanel.add(scrollPane);
			new MarkAddressesAddOn().setUaVec(data).addTo(table, universePanel, this.data.universe);
		}
	}
	
	private static class ImagePanel extends JPanel {
		private static final long serialVersionUID = -8139869439172225802L;
		
		private JLabel text;
		private JLabel image;

		ImagePanel(String title) {
			super(new BorderLayout(3,3));
			setBorder(BorderFactory.createTitledBorder(title));
			add(text  = new JLabel(""),BorderLayout.NORTH);
			add(image = new JLabel(),BorderLayout.CENTER);
			image.setVerticalAlignment(JLabel.TOP);
		}

		public void clear() {
			setIcon(null);
			setText(null);
		}

		public void setIcon(Icon icon) {
			this.image.setIcon(icon);
		}

		public void setText(String text) {
			this.text.setText(text);
		}

		public void set(String objectID, IDMap idMap, int iconWidth, int iconHeight) {
			GeneralizedID id = objectID==null ? null : idMap.get(objectID);
			BufferedImage image = id==null ? null : id.getImage(iconWidth,iconHeight);
			setIcon(image==null ? null : new ImageIcon(image));
			setText(id!=null ? id.getName() : objectID!=null ? objectID : "" );
		}

		public void set(BuildingObject.BaseObjColor color) {
			setIcon(color==null ? null : BaseObjColorIconsIS.getCachedIcon(color));
			setText(color==null ? "" : color.label);
		}

		public void set(BuildingObject.BaseObjAppearance appearance) {
			setIcon(appearance==null ? null : BaseObjAppearanceIconsIS.getCachedIcon(appearance));
			setText(appearance==null ? "" : appearance.toString());
		}
	}
	
	public static class BaseBuildingObjectsPanel extends VerySimpleTableTabPanel<UnboundBuildingObject> {
		private static final long serialVersionUID = 6246130206148705495L;
		private static final Color COLOR_HIGHLIGHT = new Color(0xFFFF7F);
		private Window mainWindow;
		private Long addressToHighlight;
		
		private static class ColumnID extends VerySimpleTable.ColumnID<UnboundBuildingObject> {
			public ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, Function<UnboundBuildingObject, Object> getValue) {
				super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue);
			}
		}

		public BaseBuildingObjectsPanel(SaveGameData data, Window mainWindow, UniversePanel universePanel) {
			super(data,"BBOTable",true,SaveViewer.DEBUG,true,data.baseBuildingObjects, new ColumnID[] {
				new ColumnID("Timestamp"       ,                        TimeStamp.class,  50,-1,140,140, bbo->bbo.timestamp),
				new ColumnID("Name"            ,                           String.class,  50,-1,140,140, bbo->bbo.getNameOnly()),
				new ColumnID("ObjectID"        ,                           String.class,  50,-1,120,120, bbo->bbo.objectID),
				new ColumnID("GalacticAddress" ,                           String.class,  50,-1,160,160, bbo->bbo.galacticAddress==null ? "" : bbo.galacticAddress.getCoordinates()),
				new ColumnID("Planet / System" ,                           String.class, 200,-1,420,420, bbo->bbo.galacticAddress==null ? "" : bbo.galacticAddress.getVerboseNameInOneLine(data.universe, 2)),
				new ColumnID("RegionSeed"      ,                           String.class,  65,-1,130,130, bbo->bbo.regionSeed     ==null ? "" : String.format("0x%016X", bbo.regionSeed)),
				new ColumnID("UserData"        ,                           String.class,  40,-1, 80, 80, bbo->bbo.userData       ==null ? "" : String.format("0x%08X" , bbo.userData  )),
				new ColumnID("Color"           ,                           String.class,  40,-1, 80, 80, bbo->bbo.color          ==null ? "" : bbo.color.label     ),
				new ColumnID("Appearance"      , BuildingObject.BaseObjAppearance.class,  40,-1, 80, 80, bbo->bbo.appearance),
				new ColumnID("GPS Coords"      ,                           String.class,  80,-1,170,170, bbo->bbo.position==null || bbo.position.gps==null ? "" : bbo.position.gps.toString()),
				new ColumnID("Position"        ,                           String.class,  95,-1,250,250, bbo->bbo.position==null || bbo.position.pos==null ? "" : bbo.position.pos.toString("%1.2f")+String.format(Locale.ENGLISH," [R:%1.1f]",bbo.position.pos.length())),
				new ColumnID("Up"              ,                           String.class,  75,-1,150,150, bbo->bbo.position==null || bbo.position.up ==null ? "" : bbo.position.up .toString("%1.4f")),
				new ColumnID("At"              ,                           String.class,  75,-1,150,150, bbo->bbo.position==null || bbo.position.at ==null ? "" : bbo.position.at .toString("%1.4f")),
				new ColumnID("Message"         ,                           String.class,  75,-1,150,150, bbo->bbo.message),
			});
			this.mainWindow = mainWindow;
			this.addressToHighlight = null;
			
			//BBOTableModel tableModel = new BBOTableModel();
			//table = new SimplifiedTable("BBOTable",tableModel,true,SaveViewer.DEBUG,true);
			table.setCellRendererForAllColumns(new DefaultTableCellRenderer(){
				private static final long serialVersionUID = -8578657095051025534L;
				@Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
					Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
					if (!isSelected) {
						Color background = table.getBackground();
						if (addressToHighlight!=null) {
							UnboundBuildingObject ubo = BaseBuildingObjectsPanel.this.data.baseBuildingObjects[table.convertRowIndexToModel(row)];
							if (ubo.galacticAddress!=null && ubo.galacticAddress.getAddress()==addressToHighlight)
								background = COLOR_HIGHLIGHT;
						}
						component.setBackground(background);
					}
					return component;
				}
				
			},true);
			//JScrollPane tableScrollPane = new JScrollPane(table);
			
			new MarkAddressesAddOn().setData(this.data.baseBuildingObjects).addTo(table, universePanel, this.data.universe);
			
			JPopupMenu contextMenu = table.getContextMenu();
			contextMenu.add(Gui.createMenuItem("Highlight Rows with Address ...",e->highlightRowsWithAddress()));
			contextMenu.add(Gui.createMenuItem("Update ObjectIDs",e->table.getModel_VerySimpleTableModel().initiateColumnUpdate(table.getColumn(2))));
			contextMenu.add(Gui.createMenuItem("Write Positions to VRML",e->writePosToVRML(),Gui.ToolbarIcons.SaveAs));
			
			
			ImagePanel objectIdImage,objColorImage,objAppearImage;
			JPanel imagePanel = new JPanel(new GridLayout(0,1));
			imagePanel.setPreferredSize(new Dimension(150,200));
			imagePanel.add(objectIdImage  = new ImagePanel("ObjectID Image"));
			imagePanel.add(objColorImage  = new ImagePanel("Object Color"  ));
			imagePanel.add(objAppearImage = new ImagePanel("Appearance"    ));
			add(imagePanel,BorderLayout.WEST);
			table.getSelectionModel().addListSelectionListener(e -> {
				int[] rowVs = table.getSelectedRows();
				if (rowVs.length==1 && rowVs[0]>=0) {
					int rowM = table.convertRowIndexToModel(rowVs[0]);
					BuildingObject obj = this.data.baseBuildingObjects[rowM];
					
					objectIdImage .set(obj.objectID,GameInfos.productIDs,135,135);
					objColorImage .set(obj.color);
					objAppearImage.set(obj.appearance);
					return;
				}
				objectIdImage .clear();
				objColorImage .clear();
				objAppearImage.clear();
			});

			//add(tableScrollPane,BorderLayout.CENTER);
		}

		private void highlightRowsWithAddress() {
			Gui.CoordinatesDialog dlg = new Gui.CoordinatesDialog(mainWindow,data.universe,true,"Select Coordinates");
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

			public static String getVerboseNameInOneLine(long addr, Universe universe) {
				UniverseAddress ua = new UniverseAddress(addr);
				
				if (ua.isRegion())
					return "Region \""+ua.getCoordinates_Region()+"\"";
				
				if (ua.isSolarSystem()) {
					Universe.SolarSystem sys = universe.findSolarSystem(ua);
					return "SolarSystem \""+ua.getCoordinates_Region()+" | "+sys.toString()+"\"";
				}
				
				if (ua.isPlanet()) {
					Universe.SolarSystem sys = universe.findSolarSystem(ua);
					Universe.Planet pln = universe.findPlanet(ua);
					return "Planet \""+ua.getCoordinates_Region()+" | "+sys.toString(true,true,false,true)+" | "+pln.toString()+"\"";
				}
				
				return ua.getCoordinates();
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
				String str = NamedAddress.getVerboseNameInOneLine(addr,data.universe);
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
			
			String suggestedFileName = String.format("BBO_%s_%016X.wrl", data.index>=0?(""+(data.index+1)):"#", selectedAddress);
			FileExport.writePosToVRML_simple(suggestedFileName,objects.toArray(new BuildingObject[0]), radius, mainWindow,"BuildingObjects",null);
		}
	}

	public static class PersistentPlayerBasesPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = -632703090899520348L;
		private final Window mainWindow;
		private final UniversePanel universePanel;

		public PersistentPlayerBasesPanel(SaveGameData data, Window mainWindow, UniversePanel universePanel) {
			super(data);
			this.mainWindow = mainWindow;
			this.universePanel = universePanel;
			
			JTabbedPane tabbedPane = new JTabbedPane();
			Vector<PersistentPlayerBase> bases = this.data.persistentPlayerBases;
			Vector<PersistentPlayerBase> emptyBases = new Vector<>();
			Vector<String> emptyBaseTitles = new Vector<>();
			for (int i=0; i<bases.size(); i++) {
				PersistentPlayerBase pb = bases.get(i);
				String title;
				//if (bases.size()<15)
					title = String.format("Base %d: %s", i+1, pb.baseType!=null?pb.baseType.getMidLabel():("["+pb.baseTypeStr+"]"));
				//else if (bases.size()<30)
				//	title = String.format("B%d:%s", j+1, pb.baseType==null?"????":pb.baseType.getMidLabel());
				//else
				//	title = String.format("B%d:%s", j+1, pb.baseType==null?"??":pb.baseType.getShortLabel());

				if (pb.objects==null || pb.objects.length==0) {
					emptyBases.add(pb);
					emptyBaseTitles.add(title);
					continue;
				}
				addBaseTab(tabbedPane, pb, title);
			}
			
			if (!emptyBases.isEmpty()) {
				tabbedPane.addTab("Empty Bases", new EmptyBasesPanel(emptyBases,emptyBaseTitles,this));
			}
			
			add(tabbedPane,BorderLayout.CENTER);
		}

		public void markGalacticAddressInUniverse(PersistentPlayerBase playerbase) {
			if (playerbase==null) return;
			if (playerbase.galacticAddress==null) return;
			universePanel.markAddress(playerbase.galacticAddress);
		}

		private static void showBaseValues(JTextArea textArea, PersistentPlayerBase playerbase, Universe universe) {
			textArea.setText("");
			
			textArea.append("Name         : "+(playerbase.name        ==null?"":playerbase.name        )+"\r\n");
			textArea.append("Base Version : "+(playerbase.baseVersion ==null?"":playerbase.baseVersion )+"\r\n");
			textArea.append("User Data    : "+(playerbase.userData    ==null?"":String.format("%08X", playerbase.userData))+"\r\n");
			textArea.append("RID          : "+(playerbase.rid         ==null?"":playerbase.rid         )+"\r\n");
			textArea.append("Type         : "+(playerbase.baseType    !=null?playerbase.baseType.getLongLabel():(playerbase.baseTypeStr==""?"":("["+playerbase.baseTypeStr+"]")))+"\r\n");
			textArea.append("Last Update  : "+(playerbase.lastUpdateTS==null?"":playerbase.lastUpdateTS.toString())+"\r\n");
			
			if (playerbase.owner!=null) {
				textArea.append("\r\nOwner :\r\n");
				textArea.append("   LID : "            +(playerbase.owner.LID==null?"":SaveViewer.steamIDs.getNameReplacement(playerbase.owner.LID))+"\r\n");
				textArea.append("   UID (User ID  ) : "+(playerbase.owner.userID==null?"":SaveViewer.steamIDs.getNameReplacement(playerbase.owner.userID))+"\r\n");
				textArea.append("   USN (User Name) : "+(playerbase.owner.userName==null?"":playerbase.owner.userName)+"\r\n");
				textArea.append("   TS  (Timestamp) : "+(playerbase.owner.timeStamp ==null?"":playerbase.owner.timeStamp.toString())+"\r\n");
			}
			
			if (playerbase.galacticAddress!=null) {
				textArea.append("\r\nGalactic Address :\r\n");
				for (String str:playerbase.galacticAddress.getVerboseName(universe)) textArea.append("   "+str+"\r\n");
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

		private void addBaseTab(JTabbedPane tabbedPane, PersistentPlayerBase pb, String title) {
			int index = tabbedPane.getTabCount();
			tabbedPane.insertTab(title,null, new PlayerBasePanel(pb,this), null, index);
			tabbedPane.setTabComponentAt(index, new BaseTabHeader(title,pb));
		}
		
		private static final class BaseTabHeader extends JPanel {
			private static final long serialVersionUID = -4150419571200547225L;
			private static final Color FG_Freighter = new Color(0x4483FF);
			private static final Color FG_Planet    = new Color(0x007F00);
			private static final Color FG_Other     = Color.MAGENTA.darker();

			public BaseTabHeader(String title, PersistentPlayerBase pb) {
				setOpaque(false);
				setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
				JLabel label = new JLabel(title);
				if (pb.baseType!=null)
					switch (pb.baseType) {
					case FreighterBase     : label.setForeground(FG_Freighter); break;
					case HomePlanetBase    : label.setForeground(FG_Planet); break;
					case ExternalPlanetBase: label.setForeground(FG_Other); break;
					case SpaceBase: break;
					}
				add(label);
			}
		}
		
		public static class EmptyBasesPanel extends JPanel {
			private static final long serialVersionUID = -1066602187570695115L;

			private PersistentPlayerBase selectedBase = null;

			public EmptyBasesPanel(Vector<PersistentPlayerBase> emptyBases, Vector<String> emptyBaseTitles, PersistentPlayerBasesPanel mainPanel) {
				super(new BorderLayout(3, 3));
				
				JList<String> lstBases = new JList<>(emptyBaseTitles);
				lstBases.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				JScrollPane listScrollPane = new JScrollPane(lstBases);
				listScrollPane.setPreferredSize(new Dimension(300, 50));
				
				JTextArea textArea = new JTextArea();
				JScrollPane textAreaScrollPane = new JScrollPane(textArea);
				textAreaScrollPane.setPreferredSize(new Dimension(500, 50));
				
				JMenuItem miMarkInUniverse;
				Gui.SimpleContextMenu textAreaContextMenu = new Gui.SimpleContextMenu(textArea);
				textAreaContextMenu.add(miMarkInUniverse = Gui.createMenuItem("Mark Galactic Address in \"Known Universe\"",e->mainPanel.markGalacticAddressInUniverse(selectedBase)));
				textAreaContextMenu.addContextMenuInvokeListener((x, y) -> miMarkInUniverse.setEnabled(selectedBase!=null));
				
				add(listScrollPane,BorderLayout.WEST);
				add(textAreaScrollPane,BorderLayout.CENTER);
				
				lstBases.addListSelectionListener(e -> {
					int index = lstBases.getSelectedIndex();
					if (0<=index && index<emptyBases.size())
						showBaseValues(textArea, selectedBase = emptyBases.get(index), mainPanel.data.universe);
					else {
						selectedBase = null;
						textArea.setText("");
					}
				});
			}
			
		}

		public static class PlayerBasePanel extends JPanel {
			private static final long serialVersionUID = 6070388468452658705L;
			
			private final SaveGameData data;
			private final PersistentPlayerBase playerbase;
			
			private final Window mainWindow;
			private final PersistentPlayerBasesPanel mainPanel;
			private final JTextArea textArea;
			
			private int clickedRow;
			private BuildingObject clickedBuildingObject;


			public PlayerBasePanel(PersistentPlayerBase playerbase, PersistentPlayerBasesPanel mainPanel) {
				super(new BorderLayout(3, 3));
				this.playerbase = playerbase;
				this.mainPanel = mainPanel;
				this.data = mainPanel.data;
				this.mainWindow = mainPanel.mainWindow;
				clickedRow = -1;
				clickedBuildingObject = null;
				setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
				
				textArea = new JTextArea();
				JScrollPane textAreaScrollPane = new JScrollPane(textArea);
				textAreaScrollPane.setPreferredSize(new Dimension(500, 50));
				
				BaseObjectsTableModel tableModel = new BaseObjectsTableModel(this.playerbase.objects);
				SimplifiedTable<BaseObjectsColumnID> table = new SimplifiedTable<>("BaseObjectsTable",tableModel,true,SaveViewer.DEBUG,true);
				table.setCellRendererForAllColumns(new HighlightCR(this.playerbase.objects), true);
				table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION, true);
				JScrollPane tableScrollPane = new JScrollPane(table);
				
				JCheckBoxMenuItem miHighlightObj;
				JMenuItem miEditID;
				JPopupMenu contextMenu = table.getContextMenu();
				contextMenu.addSeparator();
				contextMenu.add(Gui.createMenuItem("Update ObjectIDs",e->tableModel.initiateColumnUpdate(BaseObjectsColumnID.ObjectID)));
				contextMenu.add(miHighlightObj = Gui.createCheckBoxMenuItem("[Highlight BuildingObject]",e->{
					if (clickedBuildingObject==null) return;
					String objectID = clickedBuildingObject.objectID;
					HashSet<String> set = SaveViewer.config.highlightedBuildingObjects;
					if (set.contains(objectID)) set.remove(objectID);
					else                        set.add   (objectID);
					table.repaint();
					SaveViewer.config.writeToFile();
				}));
				contextMenu.add(miEditID = Gui.createMenuItem("[Edit ID]", e->{
					if (clickedBuildingObject==null || clickedRow<0) return;
					GeneralizedID id = clickedBuildingObject.getId();
					IDMap map = BuildingObject.getIDMap();
					if (id==null || map==null) return;
					boolean wasChanged = GameInfos.EditIdDialog.showDialog(mainWindow, id, map.getTemplateList());
					if (wasChanged) {
						map.saveIDsToFile();
						tableModel.fireTableColumnUpdate(BaseObjectsColumnID.IdType);
						tableModel.fireTableColumnUpdate(BaseObjectsColumnID.Name);
					}
				}));
				contextMenu.addSeparator();
				
				Runnable updateVRMLtasks1 = addVRMLtasks(contextMenu,mainWindow);
				table.addContextMenuInvokeListener((rowV, columnV) -> {
					clickedRow = table.convertRowIndexToModel(rowV);
					clickedBuildingObject = tableModel.getBuildingObject(clickedRow);
					miHighlightObj.setText(String.format("Highlight all \"%s\" in bases", clickedBuildingObject==null ? "???" : clickedBuildingObject.objectID ));
					miHighlightObj.setEnabled(clickedBuildingObject!=null);
					miHighlightObj.setSelected(clickedBuildingObject!=null && SaveViewer.config.highlightedBuildingObjects.contains(clickedBuildingObject.objectID));
					miEditID.setEnabled(clickedBuildingObject!=null);
					miEditID.setText(String.format("Edit ID \"%s\"", clickedBuildingObject==null ? "???" : clickedBuildingObject.objectID ));
					
					updateVRMLtasks1.run();
				});
				
				Gui.SimpleContextMenu textAreaContextMenu = new Gui.SimpleContextMenu(textArea);
				textAreaContextMenu.add(Gui.createMenuItem("Mark Galactic Address in \"Known Universe\"",e->this.mainPanel.markGalacticAddressInUniverse(this.playerbase)));
				Runnable updateVRMLtasks2 = addVRMLtasks(textAreaContextMenu,mainWindow);
				textAreaContextMenu.addContextMenuInvokeListener((x, y) -> updateVRMLtasks2.run());
				
				ImagePanel objectIdImage,objColorImage,objAppearImage;
				JPanel imagePanel = new JPanel(new GridLayout(1,0));
				imagePanel.setPreferredSize(new Dimension(500, 150));
				imagePanel.add(objectIdImage  = new ImagePanel("ObjectID Image"));
				imagePanel.add(objColorImage  = new ImagePanel("Object Color"  ));
				imagePanel.add(objAppearImage = new ImagePanel("Appearance"    ));
				table.getSelectionModel().addListSelectionListener(e -> {
					int rowV = table.getSelectedRow();
					if (rowV>=0) {
						int rowM = table.convertRowIndexToModel(rowV);
						BuildingObject obj = this.playerbase.objects[rowM];
						
						objectIdImage .set(obj.objectID,GameInfos.productIDs,110,110);
						objColorImage .set(obj.color);
						objAppearImage.set(obj.appearance);
						return;
					}
					objectIdImage .clear();
					objColorImage .clear();
					objAppearImage.clear();
				});
				
				JPanel westPanel = new JPanel(new BorderLayout(3,3));
				westPanel.add(textAreaScrollPane,BorderLayout.CENTER);
				westPanel.add(imagePanel,BorderLayout.SOUTH);
				
				add(tableScrollPane,BorderLayout.CENTER);
				add(westPanel,BorderLayout.WEST);
				
				showBaseValues(textArea, this.playerbase, this.data.universe);
				showOtherObjectsOnThisPlanet();
			}

			public enum Type { Simple, Models, Planet }
			private String suggestFileName(Type type) {
				return suggestFileName(type, data, playerbase);
			}
			public static String suggestFileName(Type type, SaveGameData data, PersistentPlayerBase playerbase) {
				String prefix = String.format("Base_S%s_B%d", data.index>=0?(""+(data.index+1)):"#", playerbase.baseIndex+1);
				switch (type) {
				case Simple: return prefix+"_simple.wrl";
				case Models: return prefix+".wrl";
				case Planet: return prefix+"_planet.wrl";
				}
				return "";
			}
			
			private Runnable addVRMLtasks(JPopupMenu contextMenu, Window window) {
				contextMenu.add(Gui.createMenuItem("Write Base to VRML (simple)",e->{
					FileExport.writePosToVRML_simple(suggestFileName(Type.Simple),playerbase.objects,null,mainWindow,"Base",FileExport::openFileInVrmlViewer);
				}, Gui.ToolbarIcons.SaveAs));
				
				contextMenu.add(Gui.createMenuItem("Write Base to VRML (Models)",e->{
					FileExport.writePosToVRML_models(suggestFileName(Type.Models),null,playerbase,mainWindow,"Base", false,FileExport::openFileInVrmlViewer);
				}, Gui.ToolbarIcons.SaveAs));
				
				contextMenu.add(Gui.createMenuItem("Write Whole Planet to VRML (simple)",e->{
					Vector<BuildingObject> nearObj = getNearObjects();
					nearObj.add(BuildingObject.createFromBase(playerbase));
					
					Double radius = null;
					for (BuildingObject obj:nearObj)
						if (obj.position!=null && obj.position.pos!=null) {
							if (radius==null) radius = obj.position.pos.length();
							else radius = Math.min(radius, obj.position.pos.length());
						}
					
					FileExport.writePosToVRML_simple(suggestFileName(Type.Planet),nearObj.toArray(new BuildingObject[0]),radius,mainWindow,"Whole Planet",FileExport::openFileInVrmlViewer);
				}, Gui.ToolbarIcons.SaveAs));
				
				contextMenu.addSeparator();
				
				JCheckBoxMenuItem miOpenNewFileChckBx;
				contextMenu.add(miOpenNewFileChckBx = Gui.createCheckBoxMenuItem("Open newly written file in viewer", SaveViewer.config.openNewlyWrittenVrmlFileInViewer, (Consumer<Boolean>)isSelected->{
					SaveViewer.config.openNewlyWrittenVrmlFileInViewer = isSelected;
					if (SaveViewer.config.openNewlyWrittenVrmlFileInViewer) {
						if (!SaveViewer.config.isVrmlViewerConfigured()) {
							String path = JOptionPane.showInputDialog(window, "Set path to VRML viewer:");
							if (path!=null) SaveViewer.config.vrmlViewer = path;
							if (!SaveViewer.config.isVrmlViewerConfigured())
								SaveViewer.config.openNewlyWrittenVrmlFileInViewer = false;
						}
					}
					SaveViewer.config.writeToFile();
				}));
				
				JCheckBoxMenuItem miUseSmallLightsAsMeasurePoints;
				contextMenu.add(miUseSmallLightsAsMeasurePoints = Gui.createCheckBoxMenuItem("Use SMALL LIGHTs as Measure Points", SaveViewer.config.useSmallLightsAsMeasurePoints, (Consumer<Boolean>)b->{
					SaveViewer.config.useSmallLightsAsMeasurePoints = b;
					SaveViewer.config.writeToFile();
				}));
				
				JCheckBoxMenuItem miAddNumberToObjectIDInAxisCross;
				contextMenu.add(miAddNumberToObjectIDInAxisCross = Gui.createCheckBoxMenuItem("Add Number to ObjectID in AxisCross", SaveViewer.config.addNumberToObjectIDInAxisCross, (Consumer<Boolean>)b->{
					SaveViewer.config.addNumberToObjectIDInAxisCross = b;
					SaveViewer.config.writeToFile();
				}));
				
				return ()->{
					miOpenNewFileChckBx.setSelected(SaveViewer.config.openNewlyWrittenVrmlFileInViewer);
					miUseSmallLightsAsMeasurePoints.setSelected(SaveViewer.config.useSmallLightsAsMeasurePoints);
					miAddNumberToObjectIDInAxisCross.setSelected(SaveViewer.config.addNumberToObjectIDInAxisCross);
				};
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
			
			private static class HighlightCR extends DefaultTableCellRenderer {
				private static final long serialVersionUID = 5514633635145216604L;
				private static final Color HIGHLIGHT_COLOR = new Color(0xffb400);
				private BuildingObject[] objects;

				public HighlightCR(BuildingObject[] objects) {
					this.objects = objects;
				}

				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
					Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
					int rowM = table.convertRowIndexToModel(row);
					if (!isSelected && 0<=rowM && rowM<objects.length) {
						BuildingObject bo = objects[rowM];
						if (SaveViewer.config.highlightedBuildingObjects.contains(bo.objectID)) {
							//setForeground(table.getForeground());
							setBackground(HIGHLIGHT_COLOR);
						} else {
							//setForeground(table.getForeground());
							setBackground(table.getBackground());
						}
					}
					return component;
				}
				
				
				
			}

			private enum BaseObjectsColumnID implements SimplifiedColumnIDInterface {
				// [140, 176, 128, 80, 150, 170, 170]
				Timestamp  ("Timestamp"  ,                        TimeStamp.class, 20,-1,140,140),
				ObjectID   ("ObjectID"   ,                           String.class, 20,-1,130,130),
				Name       ("Name"       ,                           String.class, 20,-1,180,180),
				IdType     ("Type"       ,               GeneralizedID.Type.class, 20,-1,200,200),
				QuickSilver("QuickSilver",                           String.class, 20,-1, 50, 50),
				UserData   ("UserData"   ,                           String.class, 20,-1,100,100),
				Color      ("Color"      ,                           String.class, 20,-1,110,110),
				Appearance ("Appearance" , BuildingObject.BaseObjAppearance.class, 20,-1, 70, 70),
				Position   ("Position"   ,                           String.class, 20,-1,150,150),
				Up         ("Up"         ,                           String.class, 20,-1,170,170),
				At         ("At"         ,                           String.class, 20,-1,170,170),
				Up_Length  ("[Up]"       ,                           Double.class, 20,-1, 50, 50),
				At_Length  ("[At]"       ,                           Double.class, 20,-1, 50, 50),
				Message    ("Message"    ,                           String.class, 20,-1,170,170),
				;
				
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
		
				@Override public void fireTableColumnUpdate(BaseObjectsColumnID columnID) {
					super.fireTableColumnUpdate(columnID);
				}

				@Override public void fireTableRowUpdate(int rowM) {
					super.fireTableRowUpdate(rowM);
				}

				@Override
				public int getRowCount() {
					return objects.length;
				}
		
				public BuildingObject getBuildingObject(int rowIndex) {
					if (rowIndex<0 || rowIndex>=objects.length) return null;
					return objects[rowIndex];
				}
				@Override
				public Object getValueAt(int rowIndex, int columnIndex, BaseObjectsColumnID columnID) {
					BuildingObject obj = getBuildingObject(rowIndex);
					if (obj==null) return null;     
					switch(columnID) {
					case Timestamp  : return obj.timestamp;
					case Name       : return obj.getNameOnly();
					case IdType     : return obj.getIdType();
					case ObjectID   : return obj.objectID;
					case QuickSilver: return getQuickSilverStr(obj);
					case UserData   : if (obj.userData  ==null) return ""; return String.format("0x%08X" , obj.userData  );
					case Color      : if (obj.color     ==null) return ""; return obj.color.label     ;
					case Appearance : return obj.appearance;
					case Position   : if (obj.position  ==null || obj.position.pos==null) return ""; else return obj.position.pos.toString(" %1.2f ");
					case Up         : if (obj.position  ==null || obj.position.up ==null) return ""; else return obj.position.up .toString(" %1.4f ");
					case At         : if (obj.position  ==null || obj.position.at ==null) return ""; else return obj.position.at .toString(" %1.4f ");
					case Up_Length  : if (obj.position  ==null || obj.position.up ==null) return ""; else return String.format(Locale.ENGLISH, "%1.4f", obj.position.up.length());
					case At_Length  : if (obj.position  ==null || obj.position.at ==null) return ""; else return String.format(Locale.ENGLISH, "%1.4f", obj.position.at.length());
					case Message    : return obj.message;
					}
					return null;
				}

				private String getQuickSilverStr(BuildingObject obj) {
					GeneralizedID id = obj.getId();
					if (id==null) return null;
					return id.getUsage(data).generalUsages.contains(GeneralizedID.Usage.Type.QuicksilverSpecial) ? "QS" : "";
				}
			}
		}
	}
}