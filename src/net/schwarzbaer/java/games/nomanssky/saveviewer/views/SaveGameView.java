package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Vector;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe;

public class SaveGameView extends JPanel {

	private static final long serialVersionUID = -1641171938196309864L;
	
	private final Window mainWindow;
	private final JTabbedPane tabbedPane;
	public  final File file;
	public  SaveGameData data;
	private boolean isNEXT;
	private boolean exceptionWhileParsing;

	public SaveGameView(Window mainWindow, File file, SaveGameData data, boolean newFormat, boolean exceptionWhileParsing) {
		super(new BorderLayout(3, 3));
		setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		
		this.mainWindow = mainWindow;
		this.file = file;
		this.data = data;
		this.isNEXT = newFormat;
		this.exceptionWhileParsing = exceptionWhileParsing;
		
		tabbedPane = new JTabbedPane();
		//tabbedPane.setPreferredSize(new Dimension(620, 500));
		
		addAllTabs();
		
		tabbedPane.addChangeListener(new ChangeListener() {
			@Override public void stateChanged(ChangeEvent e) {
				Component comp = tabbedPane.getSelectedComponent();
				if (comp instanceof SaveGameViewTab) {
					SaveGameViewTab tabPanel = (SaveGameViewTab)comp;
					tabPanel.updateContent();
				}
			}
		});
		
		add(tabbedPane,BorderLayout.CENTER);
	}


	private void addAllTabs() {
		if (isNEXT)
		{
			SaveGameViewTabGroupingPanel rawDataPanel = new SaveGameViewTabGroupingPanel(data);
			rawDataPanel.addPanel("DeObfuscator Usage", new SimplePanels.DeObfuscatorUsagePanel(data));
			
			if (!exceptionWhileParsing)
			{
				
				UniversePanel universePanel = new UniversePanel(data,mainWindow);
				GalaxyMapPanel galaxyMapPanel = new GalaxyMapPanel(data,mainWindow);
				galaxyMapPanel.setUniversePanel(universePanel);
				universePanel.setGalaxyMapPanel(galaxyMapPanel);
				
				tabbedPane.addTab("General",new GeneralDataPanel(data));
				tabbedPane.addTab("Known Universe",universePanel);
				tabbedPane.addTab("Galaxy Map",galaxyMapPanel);
				
				if (data.inventories!=null) tabbedPane.addTab("Inventories",new InventoriesPanel(data,mainWindow));
				if (data.companions !=null && !data.companions .isEmpty()) tabbedPane.addTab("Companions" ,new SimplePanels.CompanionsPanel (data));
				if (data.appearances!=null && !data.appearances.isEmpty()) tabbedPane.addTab("Appearances",new SimplePanels.AppearancesPanel(data));
				
				if (data.frigates   !=null) tabbedPane.addTab("Frigates"   ,new SimplePanels.FrigatesPanel(data,mainWindow));
				if (data.frigateMissions!=null) tabbedPane.addTab("Frigate Missions",new SimplePanels.FrigateMissionsPanel(data,mainWindow));
				
				if (data.teleportEndpoints    !=null) tabbedPane.addTab("Teleport Endpoints" , new SimplePanels.TeleportEndpointsPanel(data,universePanel));
				if (data.persistentPlayerBases!=null) tabbedPane.addTab("Player Bases"       , new SimplePanels.PersistentPlayerBasesPanel(data,mainWindow,universePanel));
				if (data.baseBuildingObjects  !=null) tabbedPane.addTab("BaseBuildingObjects", new SimplePanels.BaseBuildingObjectsPanel(data,mainWindow,universePanel));
				
				if (data.stats      !=null) tabbedPane.addTab("Status Values",new StatsPanel(data));
				if (data.knownWords !=null) tabbedPane.addTab("KnownWords"   ,new KnownWordsPanel(data,data.knownWords ));
				if (data.knownWords2!=null) tabbedPane.addTab("KnownWords II",new KnownWordsPanel(data,data.knownWords2));
				
				rawDataPanel.addPanel("DiscoveryData (Available)", new SimplePanels.DiscoveredDataAvailablePanel(data,universePanel));
				rawDataPanel.addPanel("DiscoveryData (Stored)"   , new SimplePanels.DiscoveredDataStoredPanel   (data,universePanel));
				
//				SaveGameViewPanelGroupingPanel blueprintsPanel = new SaveGameViewPanelGroupingPanel(data);
//				blueprintsPanel.addPanel("Known Product Blueprints",new SimplePanels.BlueprintsPanel(data,BlueprintType.KnownProductBlueprints,"KnownProductBlueprintsTable"));
//				blueprintsPanel.addPanel("Known Tech"+" Blueprints",new SimplePanels.BlueprintsPanel(data,BlueprintType.KnownTechBlueprints   ,"KnownTechBlueprintsTable"   ));
//				tabbedPane.addTab("Blueprints",blueprintsPanel);
				
				tabbedPane.addTab("Blueprints",new SimplePanels.AllBlueprintsPanel(data,mainWindow));
				
				SimplePanels.ExperimentalData.addPanels(rawDataPanel,data,mainWindow,universePanel);
				if (data.visitedSystems!=null) rawDataPanel.addPanel("Visited Systems"    ,new SimplePanels.VisitedSystemsPanel(data,universePanel));
				rawDataPanel.addPanel("Atlas Stations",new SimplePanels.AtlasStationAdressDataPanel(data,universePanel));
			}
			
			tabbedPane.addTab("Raw Data",rawDataPanel);
		}
		
//		tabbedPane.addTab("### SortTestPanel ###",new SortTestPanel(data));
		tabbedPane.addTab("Raw Data Tree",new RawDataTreePanel(mainWindow,file,data,isNEXT));
	}
	
	
	public void replaceData(SaveGameData data, boolean isNEXT, boolean exceptionWhileParsing) {
		this.data = data;
		this.isNEXT = isNEXT;
		this.exceptionWhileParsing = exceptionWhileParsing;
		tabbedPane.removeAll();
		addAllTabs();
	}


	@Override
	public String toString() {
		return file.getName();
	}


	static <T,R> Vector<R> convertVector( Vector<T> vector, Function<? super T,? extends R> convertValue ) {
		Vector<R> result = new Vector<>();
		for (T value : vector)
			result.add(convertValue.apply(value));
		return result;
	}
	
	static interface SaveGameViewTab {
		public void updateContent();
	}
	
	static class SaveGameViewTabGroupingPanel extends SaveGameViewGroupingPanel {
		private static final long serialVersionUID = -2509156613648169308L;
		private JTabbedPane tabbedPane_;

		public SaveGameViewTabGroupingPanel(SaveGameData data) {
			super(data);
			tabbedPane_ = new JTabbedPane();
			add(tabbedPane_,BorderLayout.CENTER);
		}
		
		public <ValueType> SaveGameViewTabGroupingPanel(
				SaveGameData data, Vector<ValueType> array,
				PanelConstructor<ValueType> panelConstructor,
				BiFunction<ValueType,Integer,String> getTitle
		) {
			this(data);
			for (int i=0; i<array.size(); i++) {
				ValueType value = array.get(i);
				addPanel(getTitle.apply(value,i), panelConstructor.construct(data,value,i));
			}
		}
		
		public interface PanelConstructor<ValueType> {
			SaveGameViewTabPanel construct(SaveGameData data, ValueType value, Integer index);
		}
		
		@Override protected void addPanelToGUI(String title, SaveGameViewTabPanel panel) {
			tabbedPane_.addTab(title, panel);
		}
	}
	
	public static class SaveGameViewPanelGroupingPanel extends SaveGameViewGroupingPanel {
		private static final long serialVersionUID = 3371660682332165241L;
		private final JPanel gridPanel;
		private final GridBagConstraints c;
		private final boolean horizontal;

		public SaveGameViewPanelGroupingPanel(SaveGameData data, boolean horizontal, boolean scrollable) {
			super(data);
			this.horizontal = horizontal;
			gridPanel = new JPanel(new GridBagLayout());
			c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 1;
			c.weighty = 1;
			c.gridwidth = 1;
			c.gridheight = 1;
			c.gridx = 0;
			c.gridy = 0;
			if (scrollable) {
				JScrollPane scrollPane = new JScrollPane(gridPanel);
				scrollPane.getVerticalScrollBar  ().setUnitIncrement(10);
				scrollPane.getHorizontalScrollBar().setUnitIncrement(10);
				add(scrollPane,BorderLayout.CENTER);
			} else add(gridPanel,BorderLayout.CENTER);
		}
		
		public <ValueType> SaveGameViewPanelGroupingPanel(
				SaveGameData data, boolean horizontal, boolean scrollable, Vector<ValueType> array,
				PanelConstructor<ValueType> panelConstructor,
				BiFunction<ValueType,Integer,String> getTitle
		) {
			this(data,horizontal,scrollable);
			for (int i=0; i<array.size(); i++) {
				ValueType value = array.get(i);
				addPanel(getTitle.apply(value,i), panelConstructor.construct(data,value,i));
			}
		}
		
		public interface PanelConstructor<ValueType> {
			SaveGameViewTabPanel construct(SaveGameData data, ValueType value, Integer index);
		}
		
		@Override
		protected void addPanelToGUI(String title, SaveGameViewTabPanel panel) {
			if (horizontal) c.gridx++;
			else c.gridy++;
			panel.setBorder(BorderFactory.createTitledBorder(title));
			gridPanel.add(panel,c);
		}
	}
	
	static abstract class SaveGameViewGroupingPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = 997641800196495231L;
		private Vector<SaveGameViewTabPanel> panels;

		public SaveGameViewGroupingPanel(SaveGameData data) {
			super(data);
			panels = new Vector<>();
		}
		
		public void addPanel(String title, SaveGameViewTabPanel panel) {
			panels.add(panel);
			addPanelToGUI(title,panel);
		}

		protected abstract void addPanelToGUI(String title, SaveGameViewTabPanel panel);

		@Override
		public void updateContent() {
			for (SaveGameViewTabPanel p:panels) p.updateContent();
		}
	}
	
	static class SaveGameViewTabPanel extends JPanel implements SaveGameViewTab {
		private static final long serialVersionUID = -5779057150309507685L;
		
		protected SaveGameData data;

		public SaveGameViewTabPanel(SaveGameData data) {
			super(new BorderLayout(3, 3));
			setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			this.data = data;
		}
		
		@Override
		public void updateContent() {}

		protected void setNameForUniverseAddress(Universe.DiscoverableObject object, String objectStr) {
			String initialValue = object.hasOriginalName()?object.getOriginalName():object.getOldOriginalName();
			String name = JOptionPane.showInputDialog(this, "New name for "+objectStr, initialValue);
			if (name!=null) {
				if (name.isEmpty()) name=null;
				object.setOriginalName(name);
				GameInfos.saveUniverseObjectDataToFile(data.universe);
			}
		}

		protected void setNameForUniverseAddress(Universe.Region region, String objectStr) {
			String initialValue = region.hasName()?region.getName():region.getOldName();
			String name = JOptionPane.showInputDialog(this, "New name for "+objectStr, initialValue);
			if (name!=null) {
				if (name.isEmpty()) name=null;
				region.setName(name);
				GameInfos.saveUniverseObjectDataToFile(data.universe);
			}
		}
	}

	public static class EnumCheckBoxMenuItem<Key extends Enum<Key>, ActionCommand extends Enum<ActionCommand>> extends JCheckBoxMenuItem {
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

/*
 	private static class SortTestPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = -8896141429868126519L;

		public SortTestPanel(SaveGameData data) {
			super(data);
			
			SortTestTableModel tableModel = new SortTestTableModel();
			SimplifiedTable table = new SimplifiedTable("SortTest",tableModel,true,SaveViewer.DEBUG,true);
			JScrollPane tableScrollPane = new JScrollPane(table);
			
			add(tableScrollPane,BorderLayout.CENTER);
		}

		private enum SortTestTableColumnID implements TableView.SimplifiedColumnIDInterface {
			Val1,Val2,Val3,Val4;
			SimplifiedColumnConfig colConf;
			SortTestTableColumnID() { colConf = new SimplifiedColumnConfig(toString(),String.class,10,-1,60,60); }
			@Override public SimplifiedColumnConfig getColumnConfig() { return colConf; }
		}
		
		private static class SortTestTableModel extends SimplifiedTableModel<SortTestTableColumnID> {
			
			private static final int UNSORTEDT_ROWS = 3;

			protected SortTestTableModel() {
				super(SortTestTableColumnID.values());
			}

			@Override public int getRowCount() { return 200; }
			@Override public int getUnsortedRowsCount() { return UNSORTEDT_ROWS; }

			@Override
			public Object getValueAt(int rowIndex, int columnIndex, SortTestTableColumnID columnID) {
				if (rowIndex<UNSORTEDT_ROWS)
					return String.format("%s.%d", columnID,rowIndex);
				switch(columnID) {
				case Val1: return ""+(((rowIndex-UNSORTEDT_ROWS)>>0) & 7);
				case Val2: return ""+(((rowIndex-UNSORTEDT_ROWS)>>3) & 7);
				case Val3: return ""+(((rowIndex-UNSORTEDT_ROWS)>>6) & 7);
				case Val4: return ""+(((rowIndex-UNSORTEDT_ROWS)>>9) & 7);
				}
				return null;
			}
		}
	}
*/

}
