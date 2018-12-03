package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.TreeSet;
import java.util.Vector;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.schwarzbaer.gui.IconSource;
import net.schwarzbaer.gui.IconSource.CachedIcons;
import net.schwarzbaer.gui.StandardDialog;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Gui;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Gui.ListMenu;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.PersistentPlayerBase;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Galaxy;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Planet;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Planet.Biome;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Region;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.SolarSystem;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.SolarSystem.Race;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.SolarSystem.StarClass;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.UniverseObject;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.UniverseObject.ExtraInfo;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.UniverseAddress;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveViewer;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.SimplifiedColumnConfig;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.SimplifiedTable;

public class UniversePanel extends SaveGameView.SaveGameViewTabPanel implements ActionListener {
	private static final long serialVersionUID = -4594889224613582352L;
	
	private enum UniverseTreeActionCommand { SetName, SetDistance, ExpandAll, CollapseRemainingTree, FindObject, RemoveHighlights, ShowFullDiscoveredUniverseData }
	private enum UniverseTreeIcons {
		Universe, Galaxy, Region, SolarSystem, Planet,
		GekSys, KorvaxSys, VykeenSys, Unexplored,
		Yellow, Red, Green, Blue,
		ConflictLevel1, ConflictLevel2, ConflictLevel3,
		;
	}
	private enum PlanetTreeIcons {
		BiomeUndef, BiomeLush, BiomeScorched, BiomeBarren, BiomeIrradiated, BiomeToxic, BiomeFrozen, BiomeAirless, BiomeExotic, BiomeExoticMega,
		SentinelAggressive;
	}
	private enum AdditionalTreeIcons {
		VehicleSummoner(20), BaseMainRoom(26), Freighter(44), Teleporter(20), BlackHole(20), Atlas(17);

		private int iconWidth;
		private AdditionalTreeIcons(int iconWidth) { this.iconWidth = iconWidth; }
	}
	
	private static final int TreeIconHeight = 20;
	private static IconSource.CachedIcons<UniverseTreeIcons> UniverseTreeIconsIS;
	private static IconSource.CachedIcons<AdditionalTreeIcons> AdditionalIcons;
	public  static IconSource.CachedIndexedImages PortalGlyphsIS;
	private static SolarSystemIcons SolarSystemIcons;
	private static PlanetIcons PlanetIcons;

	private static class IconsMap<IconID extends Enum<IconID>> {
		
		protected CachedIcons<IconID> iconSource;
		protected IconsMap(IconSource.CachedIcons<IconID> iconSource) {
			this.iconSource = iconSource;
		}
		
		protected <KeyType> Icon[] createIconArray(KeyType[] values, IconID iconNullId, Function<KeyType,IconID> getIconID) {
			Icon[] icons = new Icon[values.length+1];
			icons[0] = iconNullId!=null?iconSource.getCachedIcon(iconNullId):null;
			for (int i=0; i<values.length; i++)
				icons[i+1] = iconSource.getCachedIcon(getIconID.apply(values[i]));
			return icons;
		}
		
		protected Icon[][] combineIconArrays(Icon[] icons1, Icon[] icons2, IconID iconBgId) {
			Icon iconBG = iconBgId!=null?iconSource.getCachedIcon(iconBgId):null;
			Icon[][] icons = new Icon[icons1.length][icons2.length];
			Icon icon;
			for (int i=0; i<icons1.length; i++) {
				icon = IconSource.combine(iconBG, icons1[i]);
				for (int j=0; j<icons2.length; j++)
					icons[i][j] = IconSource.combine(icon, icons2[j]);
			}
			return icons;
		}
		
		protected Icon[][][] combineIconArrays(Icon[] icons1, Icon[] icons2, Icon[] icons3, IconID iconBgId) {
			Icon iconBG = iconBgId!=null?iconSource.getCachedIcon(iconBgId):null;
			Icon[][][] icons = new Icon[icons1.length][icons2.length][icons3.length];
			Icon icon1,icon2;
			for (int i=0; i<icons1.length; i++) {
				icon1 = IconSource.combine(iconBG, icons1[i]);
				for (int j=0; j<icons2.length; j++) {
					icon2 = IconSource.combine(icon1, icons2[j]);
					for (int k=0; k<icons3.length; k++) {
						icons[i][j][k] = IconSource.combine(icon2, icons3[k]);
					}
				}
			}
			return icons;
		}
		
		protected <KeyType> Icon[] createIconArray(KeyType[] values, int x, int y, int w, int h, Function<KeyType,IconID> getIconID) {
			Icon[] icons = new Icon[values.length];
			for (int i=0; i<values.length; i++) {
				IconID iconID = getIconID.apply(values[i]);
				Icon cachedIcon = iconSource.getCachedIcon(iconID);
				icons[i] = IconSource.cutIcon(cachedIcon,x,y,w,h);
			}
			return icons;
		}
		
		protected <E extends Enum<E>> EnumMap<E,Icon> createIconEnumMap(Class<E> eClass, E[] values, int x, int y, int w, int h, Function<E,IconID> getIconID) {
			EnumMap<E, Icon> enumMap = new EnumMap<>(eClass);
			for (E value:values) {
				IconID iconID = getIconID.apply(value);
				Icon cachedIcon = iconSource.getCachedIcon(iconID);
				enumMap.put(value, IconSource.cutIcon(cachedIcon,x,y,w,h)); 
			}
			return enumMap;
		}
	}
	
	private static class SolarSystemIcons extends IconsMap<UniverseTreeIcons> {
		
		private Icon[][][] icons;
		private Icon[] unexploredIcons;
		
		EnumMap<Race,Icon> RaceIcons;
		EnumMap<StarClass,Icon> StarClassIcons;
		private Icon[] ConflictLevelIcons;
		
		SolarSystemIcons() {
			super(UniverseTreeIconsIS);
			icons = null;
			unexploredIcons = null;
			RaceIcons = null;
			StarClassIcons = null;
			ConflictLevelIcons = null;
		}

		void createValues() {
			Function<Race, UniverseTreeIcons> getRaceIconID = (Race race)->{
				switch(race) {
				case Gek   : return UniverseTreeIcons.GekSys;
				case Korvax: return UniverseTreeIcons.KorvaxSys;
				case Vykeen: return UniverseTreeIcons.VykeenSys;
				}
				return null;
			};
			Function<StarClass, UniverseTreeIcons> getStarClassIconID = (StarClass starClass)->{
				switch(starClass) {
				case Yellow: return UniverseTreeIcons.Yellow;
				case Red   : return UniverseTreeIcons.Red   ;
				case Green : return UniverseTreeIcons.Green ;
				case Blue  : return UniverseTreeIcons.Blue  ;
				}
				return null;
			};
			Function<Integer, UniverseTreeIcons> getConflictLevelIconID = (Integer conflictLevel)->{
				switch(conflictLevel) {
				case 1: return UniverseTreeIcons.ConflictLevel1;
				case 2: return UniverseTreeIcons.ConflictLevel2;
				case 3: return UniverseTreeIcons.ConflictLevel3;
				}
				return null;
			};
			
			Icon[] raceIcons = createIconArray(Race.values(), null, getRaceIconID);
			Icon[] starClassIcons = createIconArray(StarClass.values(), null, getStarClassIconID);
			Icon[] conflictLevelIcons = createIconArray(new Integer[]{1,2,3}, null, getConflictLevelIconID);
			icons = combineIconArrays(raceIcons, starClassIcons, conflictLevelIcons, UniverseTreeIcons.SolarSystem);
			
			Icon[] unexploredIcon = new Icon[] { null, iconSource.getCachedIcon(UniverseTreeIcons.Unexplored) };
			unexploredIcons = combineIconArrays(unexploredIcon, starClassIcons, UniverseTreeIcons.SolarSystem)[1];
			
			ConflictLevelIcons = createIconArray(new Integer[]{1,2,3}, 0,9,11,11, getConflictLevelIconID);
			RaceIcons = createIconEnumMap(Race.class, Race.values(), 10,0,20,20, getRaceIconID);
			StarClassIcons = new EnumMap<>(StarClass.class);
			for (StarClass starClass:StarClass.values()) {
				Icon cachedIcon = get(null, starClass, -1, false);
				StarClassIcons.put(starClass, IconSource.cutIcon(cachedIcon,0,0,20,20)); 
			}
		}
		
		Icon get(Race race, StarClass starClass, int conflictLevel, boolean unexplored) {
			int raceIndex      = race     ==null?0:(race     .ordinal()+1);
			int starClassIndex = starClass==null?0:(starClass.ordinal()+1);
			if (conflictLevel<1 || 3<conflictLevel) conflictLevel = 0;
			if (unexplored) return unexploredIcons[starClassIndex];
			return icons[raceIndex][starClassIndex][conflictLevel];
		}
	}
	
	private static class PlanetIcons extends IconsMap<PlanetTreeIcons> {

		private Icon[][] icons;
		EnumMap<Biome,Icon> BiomeIcons;
		
		PlanetIcons(IconSource.CachedIcons<PlanetTreeIcons> iconSource) {
			super(iconSource);
			icons = null;
			BiomeIcons = null;
		}

		void createValues() {
			Function<Biome, PlanetTreeIcons> getBiomeIconID = (Biome biome)->{
				switch(biome) {
				case Lush       : return PlanetTreeIcons.BiomeLush;
				case Scorched   : return PlanetTreeIcons.BiomeScorched;
				case Barren     : return PlanetTreeIcons.BiomeBarren;
				case Toxic      : return PlanetTreeIcons.BiomeToxic;
				case Frozen     : return PlanetTreeIcons.BiomeFrozen;
				case Irradiated : return PlanetTreeIcons.BiomeIrradiated;
				case Airless    : return PlanetTreeIcons.BiomeAirless;
				case Exotic     : return PlanetTreeIcons.BiomeExotic;
				case Exotic_Mega: return PlanetTreeIcons.BiomeExoticMega;
				}
				return null;
			};
			
			Icon[] biomeIcons    = createIconArray(Biome.values(), PlanetTreeIcons.BiomeUndef, getBiomeIconID);
			Icon[] sentinelIcons = new Icon[] { null, iconSource.getCachedIcon(PlanetTreeIcons.SentinelAggressive) };
			icons = combineIconArrays(biomeIcons, sentinelIcons, null);
			
			BiomeIcons = createIconEnumMap(Biome.class, Biome.values(), 0,0,20,20, getBiomeIconID);
		}

		Icon get(Biome biome, boolean areSentinelsAggressive) {
			int biomeIndex = biome==null?0:(biome.ordinal()+1);
			return icons[biomeIndex][areSentinelsAggressive?1:0];
		}
	}

	//public static void prepareIconSources()
	static {
		
		IconSource.IndexOnlyIconSource PortalGlyphsIS_ = new IconSource.IndexOnlyIconSource( 50,45,4);
		PortalGlyphsIS_.readIconsFromResource("/images/PortalGlyphs.50.45.png");
		PortalGlyphsIS = PortalGlyphsIS_.cacheImages(16);
		String[] labels = new String[]{"N","NNE","NE","ENE","E","ESE","SE","SSE","S","SSW","SW","WSW","W","WNW","NW","NNW"};
		for (int i=0; i<16; ++i) {
			BufferedImage cachedImage = PortalGlyphsIS.getCachedImage(i);
			Graphics g = cachedImage.getGraphics();
			if (g instanceof Graphics2D) {
				Graphics2D g2 = (Graphics2D)g;
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g2.setFont(g2.getFont().deriveFont(10.0f).deriveFont(Font.BOLD));
				g2.setPaint(new Color(0xFFAF00));
				g2.drawString(i+" "+labels[i], 2, 10);
			}
		}
		
		IconSource<UniverseTreeIcons> UncachedUniverseTreeIcons = new IconSource<UniverseTreeIcons>(30,TreeIconHeight);
		UncachedUniverseTreeIcons.readIconsFromResource("/images/UniverseTreeIcons.png");
		UniverseTreeIconsIS = UncachedUniverseTreeIcons.cacheIcons(UniverseTreeIcons.values());
		
		Icon icon = UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.Galaxy);
		icon = IconSource.cutIcon(icon,5,0,20,20);
		UniverseTreeIconsIS.setCachedIcon(UniverseTreeIcons.Galaxy,icon);
		
		SolarSystemIcons = new SolarSystemIcons();
		SolarSystemIcons.createValues();
		
		IconSource<PlanetTreeIcons> UncachedPlanetIcons = new IconSource<PlanetTreeIcons>(0,TreeIconHeight,20,TreeIconHeight);
		UncachedPlanetIcons.readIconsFromResource("/images/UniverseTreeIcons.png");
		PlanetIcons = new PlanetIcons(UncachedPlanetIcons.cacheIcons(PlanetTreeIcons.values()));
		PlanetIcons.createValues();
		
		int offsetX = PlanetTreeIcons.values().length*20;
		IconSource<AdditionalTreeIcons> UncachedAdditionalIcons = new IconSource<AdditionalTreeIcons>(offsetX,TreeIconHeight,TreeIconHeight, id->id.iconWidth, AdditionalTreeIcons.values());
		UncachedAdditionalIcons.readIconsFromResource("/images/UniverseTreeIcons.png");
		AdditionalIcons = UncachedAdditionalIcons.cacheIcons(AdditionalTreeIcons.values());
		
	}

	private JTree tree;
	private DefaultTreeModel treeModel;
	private UniverseNode treeRoot;
	
	private GenericTreeNode<?> selectedNode;
	private GenericTreeNode<?> clickedNode;
	private TreePath clickedTreePath;
	
	private Contextmenu_Other       contextMenu_Other;
	private Contextmenu_Region      contextMenu_Region;
	private Contextmenu_SolarSystem contextMenu_SolarSystem;
	private Contextmenu_Planet      contextMenu_Planet;
	
	private Window mainWindow;
	
	private AbstractInfoPanel     currentInfoPanel;
	private InfoPanel_Other       infoPanel_Other;
	private InfoPanel_SolarSystem infoPanel_SolarSystem;
	private InfoPanel_Planet      infoPanel_Planet;
	private GalaxyMapPanel galaxyMapPanel;
	
	public UniversePanel(SaveGameData data, Window mainWindow) {
		super(data);
		this.mainWindow = mainWindow;
		this.galaxyMapPanel = null;
		
		selectedNode = null;
		TreeListener listener = new TreeListener();
		
		treeRoot = new UniverseNode(data.universe);
		treeModel = new DefaultTreeModel(treeRoot);
		tree = new JTree(treeModel);
		JScrollPane treeScrollPane = new JScrollPane(tree);
		tree.addTreeSelectionListener(listener);
		tree.addMouseListener(listener);
		tree.setCellRenderer(new UniverseTreeCellRenderer());
		tree.setRowHeight(TreeIconHeight+1);
		expandFullTree();
		
		contextMenu_Other       = new Contextmenu_Other();
		contextMenu_SolarSystem = new Contextmenu_SolarSystem();
		contextMenu_Planet      = new Contextmenu_Planet();
		contextMenu_Region      = new Contextmenu_Region();
		
		infoPanel_Other       = new InfoPanel_Other();
		infoPanel_SolarSystem = new InfoPanel_SolarSystem();
		infoPanel_Planet      = new InfoPanel_Planet();
		
		JPanel treePanel = new JPanel(new BorderLayout(3,3));
		treePanel.add(treeScrollPane,BorderLayout.CENTER);
		treePanel.add(new SearchBar(),BorderLayout.NORTH);
		
		add(treePanel,BorderLayout.CENTER);
		add(currentInfoPanel = infoPanel_Other,BorderLayout.EAST);
	}
	
	public void setGalaxyMapPanel(GalaxyMapPanel galaxyMapPanel) {
		this.galaxyMapPanel = galaxyMapPanel;
	}

	private static abstract class AbstractInfoPanel extends JPanel {
		private static final long serialVersionUID = 1055278730261206951L;
		
		private JTextArea textArea;
		
		AbstractInfoPanel() {
			super(new BorderLayout(3,3));
			setPreferredSize(new Dimension(650,500));
			
			textArea = new JTextArea();
			textArea.setEditable(false);
			
			JScrollPane scrollPane = new JScrollPane(textArea);
			scrollPane.setBorder(BorderFactory.createEtchedBorder());
			
			add(scrollPane,BorderLayout.CENTER);
		}
		public abstract void setContent(GenericTreeNode<?> node);
		
		protected void setText(String str) { textArea.setText(str); }
		protected void clearText()         { setText(""); }
		protected void append  (               String format, Object...objects ) { textArea.append(String.format(        format, objects)); }
		protected void append  (Locale locale, String format, Object...objects ) { textArea.append(String.format(locale, format, objects)); }
		protected void appendln(                                               ) { append(               "\r\n"         ); }
		protected void appendln(               String format, Object...objects ) { append(        format+"\r\n", objects); }
		protected void appendln(Locale locale, String format, Object...objects ) { append(locale, format+"\r\n", objects); }
	}
	
	private static class InfoPanel_Other extends AbstractInfoPanel {
		private static final long serialVersionUID = 4133259332387200850L;

		@Override
		public void setContent(GenericTreeNode<?> selectedNode) {
			clearText();
			
			if (selectedNode==null)
				return;
			
			int n;
			UniverseAddress ua;
			double distance_reg;
			
			switch(selectedNode.type) {
			
			case Region:
				n = selectedNode.getDataChildrenCount();
				appendln("%d known solar system%s", n, n>1?"s":"");
				ua = ((RegionNode)selectedNode).value.getUniverseAddress();
				appendln("Universe Coordinates      : %s", ua.getCoordinates_Region());
				appendln("Reduced SignalBoster Code : %s", ua.getReducedSigBoostCode());
				
				distance_reg = ua.getDistToCenter_inRegionUnits();
				appendln();
				appendln(               "Distance to Galaxy Center :");
				appendln(Locale.ENGLISH,"    computed: %1.2f Regions = %1.1f ly", distance_reg, distance_reg*400);
				break;
				
			case Galaxy:
				n = selectedNode.getDataChildrenCount();
				appendln("%d known region%s\r\n", n, n>1?"s":"");
				break;
				
			case Universe:
				n = selectedNode.getDataChildrenCount();
				appendln("%d known galax%s\r\n", n, n>1?"ies":"y");
				break;
			
			default:
				break;
			}
		}
	}
	
	private static abstract class InfoPanel_UniverseObject extends AbstractInfoPanel {
		private static final long serialVersionUID = -8235731718380188431L;
		
		private SimplifiedTable extraInfoTable;
		private UniversePanel universePanel;
		private JPanel valuePanel;
		private GridBagLayout valuePanelLayout;
		private GridBagConstraints c;
		
		InfoPanel_UniverseObject(UniversePanel universePanel, boolean useValuePanel) {
			this.universePanel = universePanel;
			extraInfoTable = new SimplifiedTable("ExtraInfoTable",true,SaveViewer.DEBUG,true);
			extraInfoTable.setPreferredScrollableViewportSize(new Dimension(610, 120));;
			
			if (useValuePanel) {
				valuePanel = new JPanel(valuePanelLayout = new GridBagLayout());
				valuePanel.setBorder(BorderFactory.createTitledBorder("Values"));
				c = new GridBagConstraints();
				
				JPanel southPanel = new JPanel(new BorderLayout(3,3));
				southPanel.add(valuePanel, BorderLayout.WEST);
				southPanel.add(new JScrollPane(extraInfoTable), BorderLayout.CENTER);
				
				add(southPanel,BorderLayout.SOUTH);
			} else
				add(new JScrollPane(extraInfoTable),BorderLayout.SOUTH);
		}
		
		protected void addCompToValuePanel(Component comp, double weightx, double weighty, int gridwidth, int gridheight, int fill) {
			c.weightx=weightx;
			c.weighty=weighty;
			c.gridwidth=gridwidth;
			c.gridheight=gridheight;
			c.fill = fill;
			valuePanelLayout.setConstraints(comp, c);
			valuePanel.add(comp);
		}

		protected void showDiscNameObj(GenericTreeNode<?> node, Universe.UniverseObject obj) {
			appendln();
			
			//textArea.append(String.format("selected : %s\r\n\r\n", obj.isSelected));
			
			String oldNameLabel = "Old Original Name";
			if (obj.hasOriginalName   ()) { appendln("Original Name : %s", obj.getOriginalName()); oldNameLabel = "  -\"-   (old)"; }
			if (obj.hasOldOriginalName())   appendln(           "%s : %s", oldNameLabel, obj.getOldOriginalName());
			if (obj.hasUploadedName   ())   appendln("Uploaded Name : %s", obj.getUploadedName());
			if (obj.hasDiscoverer     ())   appendln("Discovered by : %s", obj.getDiscoverer());
			
			appendln("Source: %s", obj.getLongSourceIDStr());
			
			if (!obj.discoveredItems_Avail.isEmpty() || !obj.discoveredItems_Store.isEmpty()) {
				appendln("Discovered Items:");
				if (!obj.discoveredItems_Avail.isEmpty()) {
					appendln("   available:");
					for (String item:new TreeSet<>(obj.discoveredItems_Avail.keySet()))
						appendln("      %s: %d", item, obj.discoveredItems_Avail.get(item));
				}
				if (!obj.discoveredItems_Store.isEmpty()) {
					appendln("   stored:");
					for (String item:new TreeSet<>(obj.discoveredItems_Store.keySet()))
						appendln("      %s: %d", item, obj.discoveredItems_Store.get(item));
				}
			}
			extraInfoTable.setModel(new ExtraInfoTableModel(node, obj instanceof Planet, obj.extraInfos));
	//			extraInfoTableModel.setData(obj.extraInfos);
		}

		private enum ExtraInfoColumnID implements TableView.SimplifiedColumnIDInterface {
			ShowInParent(""     , Boolean.class, 10,-1, 20, 20),
			Label       ("Label",  String.class, 20,-1,100,100),
			Info        ("Info" ,  String.class, 50,-1,500,500);
			
			private SimplifiedColumnConfig config;
		
			private ExtraInfoColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				config = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			
			@Override public SimplifiedColumnConfig getColumnConfig() { return config; }
		}

		private class ExtraInfoTableModel extends TableView.SimplifiedTableModel<ExtraInfoColumnID> {
		
			private Vector<ExtraInfo> tableData;
	//			private JTable table;
			private boolean isPlanet;
			private GenericTreeNode<?> node;
	
			protected ExtraInfoTableModel(GenericTreeNode<?> node, boolean isPlanet, Vector<ExtraInfo> tableData) {
				super(isPlanet?
						new ExtraInfoColumnID[]{ExtraInfoColumnID.ShowInParent,ExtraInfoColumnID.Label,ExtraInfoColumnID.Info}:
						new ExtraInfoColumnID[]{ExtraInfoColumnID.Label,ExtraInfoColumnID.Info}
					);
				this.node = node;
				this.isPlanet = isPlanet;
				this.tableData = tableData;
	//				this.table = null;
			}

//			public void setTable(JTable table) {
//				this.table = table;
//			}
//
//			public void clearData() { setData(null); }
//			public void setData(Vector<ExtraInfo> data) {
//				if (table!=null && table.isEditing()) {
//					TableCellEditor cellEditor = table.getCellEditor();
//					if (cellEditor!=null) cellEditor.cancelCellEditing();
//				}
//				this.tableData = data;
//				fireTableUpdate();
//			}
	
			@Override
			public int getRowCount() {
				if (tableData==null) return 0;
				return tableData.size()+1;
			}
	
			@Override
			public Object getValueAt(int rowIndex, int columnIndex, ExtraInfoColumnID columnID) {
				if (tableData==null) return null;
				switch(columnID) {
				case ShowInParent: if (rowIndex==tableData.size()) return false; return tableData.get(rowIndex).showInParent;
				case Label       : if (rowIndex==tableData.size()) return "";    return tableData.get(rowIndex).shortLabel;
				case Info        : if (rowIndex==tableData.size()) return "";    return tableData.get(rowIndex).info;
				}
				return null;
			}
	
			@Override
			protected boolean isCellEditable(int rowIndex, int columnIndex, ExtraInfoColumnID columnID) {
				return true;
			}
	
			@Override
			protected void setValueAt(Object aValue, int rowIndex, int columnIndex, ExtraInfoColumnID columnID) {
				if (tableData==null) return;
				if (rowIndex==tableData.size()) {
					switch(columnID) {
					case ShowInParent: tableData.add(new ExtraInfo((Boolean)aValue,"","")); break;
					case Label       : tableData.add(new ExtraInfo(aValue.toString(),"")); break;
					case Info        : tableData.add(new ExtraInfo("",aValue.toString())); break;
					}
					fireTableRowAdded(rowIndex+1);
				} else
					switch(columnID) {
					case ShowInParent: tableData.get(rowIndex).showInParent = (aValue instanceof Boolean)?(Boolean)aValue:false; break;
					case Label       : tableData.get(rowIndex).shortLabel   = aValue.toString(); break;
					case Info        : tableData.get(rowIndex).info         = aValue.toString(); break;
					}
				universePanel.updateTreeNode(node, isPlanet);
			}
		}
	}
	
	private class InfoPanel_SolarSystem extends InfoPanel_UniverseObject {
		private static final long serialVersionUID = 1050112094455682248L;
		
		private SolarSystemNode node;

		private JCheckBox chkbxAtlasInterface;
		private JCheckBox chkbxBlackHole;
		private JPanel blackHoleTargetPanel;
		private JComboBox<Region> cmbbxBlackHoleTargetRegion;
		private JComboBox<SolarSystem> cmbbxBlackHoleTargetSolarSystem;

		InfoPanel_SolarSystem() {
			super(UniversePanel.this,true);
			this.node = null;
			
			chkbxAtlasInterface = SaveViewer.createCheckbox("has atlas interface", e->{ node.value.hasAtlasInterface = chkbxAtlasInterface.isSelected(); updateTreeNode(node, false); }, false);
			chkbxBlackHole      = SaveViewer.createCheckbox("has black hole"     , e->{ setBlackHole(); updateTreeNode(node, false); galaxyMapPanel.updateBlackHoleConnections(); }, false);
			
			cmbbxBlackHoleTargetRegion      = new JComboBox<Region>();
			cmbbxBlackHoleTargetSolarSystem = new JComboBox<SolarSystem>();
			if (data.general.currentUniverseAddress!=null) {
				final Galaxy galaxy = data.universe.findGalaxy(data.general.currentUniverseAddress);
				if (galaxy!=null) {
					cmbbxBlackHoleTargetRegion.setModel(new DefaultComboBoxModel<>(galaxy.regions));
					//cmbbxBlackHoleTargetRegion.setSelectedItem(null);
				}
			}
			cmbbxBlackHoleTargetRegion.addActionListener(e->{
				Region region = (Region)cmbbxBlackHoleTargetRegion.getSelectedItem();
				if (region==null) cmbbxBlackHoleTargetSolarSystem.setModel(new DefaultComboBoxModel<>());
				else              cmbbxBlackHoleTargetSolarSystem.setModel(new DefaultComboBoxModel<>(SaveViewer.addNull(region.solarSystems)));
			});
			cmbbxBlackHoleTargetSolarSystem.addActionListener(e->{
				SolarSystem system = (SolarSystem)cmbbxBlackHoleTargetSolarSystem.getSelectedItem();
				node.value.blackHoleTarget = system==null?null:system.getUniverseAddress();
				updateTreeNode(node, false);
				galaxyMapPanel.updateBlackHoleConnections();
			});
			cmbbxBlackHoleTargetSolarSystem.setRenderer(new TableView.NonStringRenderer<SolarSystem>((Object obj)->{
				if (!(obj instanceof SolarSystem)) return "";
				SolarSystem system = (SolarSystem)obj;
				return system.toString(true, false, false, true);
			}));
			
			blackHoleTargetPanel = new JPanel(new GridLayout(0,1,3,3));
			blackHoleTargetPanel.setBorder(BorderFactory.createTitledBorder("Black Hole Target"));
			blackHoleTargetPanel.add(cmbbxBlackHoleTargetRegion     );
			blackHoleTargetPanel.add(cmbbxBlackHoleTargetSolarSystem);
			
			addCompToValuePanel(chkbxAtlasInterface , 1,0, GridBagConstraints.REMAINDER,1, GridBagConstraints.BOTH);
			addCompToValuePanel(chkbxBlackHole      , 1,0, GridBagConstraints.REMAINDER,1, GridBagConstraints.BOTH);
			addCompToValuePanel(blackHoleTargetPanel, 1,0, GridBagConstraints.REMAINDER,1, GridBagConstraints.BOTH);
		}

		private void setBlackHole() {
			node.value.hasBlackHole = chkbxBlackHole.isSelected();
			updateBlackHoleTargetPanel();
		}

		private void updateBlackHoleTargetPanel() {
			SolarSystem system = node.value;
			blackHoleTargetPanel.setEnabled(system.hasBlackHole);
			cmbbxBlackHoleTargetRegion.setEnabled(system.hasBlackHole);
			cmbbxBlackHoleTargetSolarSystem.setEnabled(system.hasBlackHole);
			if (system.blackHoleTarget==null) {
				cmbbxBlackHoleTargetRegion.setSelectedItem(null);
				cmbbxBlackHoleTargetSolarSystem.setSelectedItem(null);
			} else {
				cmbbxBlackHoleTargetRegion.setSelectedItem(data.universe.findRegion(system.blackHoleTarget));
				cmbbxBlackHoleTargetSolarSystem.setSelectedItem(data.universe.findSolarSystem(system.blackHoleTarget));
			}
		}
		
		@Override
		public void setContent(GenericTreeNode<?> node) {
			this.node = (SolarSystemNode)node;
			double distance_reg;
			
			SolarSystem system = this.node.value;
			chkbxAtlasInterface.setSelected(system.hasAtlasInterface);
			chkbxBlackHole     .setSelected(system.hasBlackHole     );
			updateBlackHoleTargetPanel();
			
			int n = system.planets.size();
			UniverseAddress ua = system.getUniverseAddress();
			
			clearText();
			appendln("%d known planet%s", n, n>1?"s":"");
			appendln("Universe Coordinates : %s", ua.getCoordinates_SolarSystem());
			appendln("SignalBoster Code    : %s", ua.getSigBoostCode());
			if (system.race     !=null) appendln("Dominant Race        : %s", system.race.fullName);
			if (system.starClass!=null) appendln("Star Class           : %s", system.starClass);
			
			if (data.general.currentUniverseAddress!=null) {
				distance_reg = ua.getDistToOther_inRegionUnits( data.general.currentUniverseAddress );
				appendln();
				appendln(               "Distance to current position:");
				appendln(Locale.ENGLISH,"    computed: %1.2f Regions = %1.1f ly", distance_reg, distance_reg*400);
			}
			
			distance_reg = ua.getDistToCenter_inRegionUnits();
			appendln();
			appendln(                   "Distance to Galaxy Center :");
			appendln(    Locale.ENGLISH,"    computed: %1.2f Regions = %1.1f ly", distance_reg, distance_reg*400);
			if (system.distanceToCenter!=null) {
				double distance_LY = system.distanceToCenter.doubleValue();
				appendln(Locale.ENGLISH,"    measured: %1.1f ly", distance_LY);
				appendln(Locale.ENGLISH,"    -> Region size: %1.2f ly", distance_LY/distance_reg);
			}
			
			showDiscNameObj(node,system);
			
			if (!system.additionalInfos.isEmpty()) {
				appendln();
				appendln("Additional Infos:");
				if (system.additionalInfos.hasFreighter)
					appendln("    Freighter in System");
			}
		}
	}
	
	private class InfoPanel_Planet extends InfoPanel_UniverseObject {
		private static final long serialVersionUID = -5303591976120968332L;
		
		private JLabel portalGlyphs;
		private PlanetNode node;

		private JCheckBox chkbxAggrSent;
		private JCheckBox chkbxWater;
		private JCheckBox chkbxGrav;

		private Gui.IconComboBox<Biome> cmbbxBiome;
		
		InfoPanel_Planet() {
			super(UniversePanel.this, true);
			this.node = null;
			
			portalGlyphs = new JLabel();
			portalGlyphs.setPreferredSize(new Dimension(50*12+10, 45*1+10));
			portalGlyphs.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(3, 3, 3, 3)));
			
			add(portalGlyphs,BorderLayout.NORTH);
			
			cmbbxBiome = new Gui.IconComboBox<Biome>(Biome.values(), new Gui.IconComboBox.ExternalFunctionality<Biome>() {
				@Override public Biome cast(Object obj) {
					if (!(obj instanceof Biome)) return null;
					return (Biome)obj;
				}
				@Override public Icon createIcon(Biome value) {
					return PlanetIcons.BiomeIcons.get(value);
				}
				@Override public String getLabel(Biome value) {
					if (value==null) return "";
					return value.name_EN;
				}
			});
			cmbbxBiome.addActionListener(e->{ node.value.biome = cmbbxBiome.getSelected(); updateTreeNode(node, false);  });
			
			chkbxAggrSent = SaveViewer.createCheckbox("Aggressive Sentinels", e->{ node.value.areSentinelsAggressive = chkbxAggrSent.isSelected(); updateTreeNode(node, false); }, false);
			chkbxWater    = SaveViewer.createCheckbox("with water"          , e->{ node.value.withWater              = chkbxWater   .isSelected(); updateTreeNode(node, true ); }, false);
			chkbxGrav     = SaveViewer.createCheckbox("with Gravitino Balls", e->{ node.value.withGravitinoBalls     = chkbxGrav    .isSelected(); updateTreeNode(node, false); }, false);
			
			addCompToValuePanel(cmbbxBiome   , 1, 0, GridBagConstraints.REMAINDER, 1, GridBagConstraints.BOTH);
			addCompToValuePanel(chkbxAggrSent, 1, 0, GridBagConstraints.REMAINDER, 1, GridBagConstraints.BOTH);
			addCompToValuePanel(chkbxWater   , 1, 0, GridBagConstraints.REMAINDER, 1, GridBagConstraints.BOTH);
			addCompToValuePanel(chkbxGrav    , 1, 0, GridBagConstraints.REMAINDER, 1, GridBagConstraints.BOTH);
		}
		
		@Override
		public void setContent(GenericTreeNode<?> node) {
			this.node = (PlanetNode)node;
			
			Planet planet = this.node.value;
			UniverseAddress ua = planet.getUniverseAddress();
			long portalGlyphCode = ua.getPortalGlyphCode();
			
			cmbbxBiome   .setSelectedItem(planet.biome);
			chkbxAggrSent.setSelected(planet.areSentinelsAggressive);
			chkbxWater   .setSelected(planet.withWater);
			chkbxGrav    .setSelected(planet.withGravitinoBalls);
			portalGlyphs.setIcon(createPortalGlyphs(portalGlyphCode));
			
			clearText();
			appendln("Universe Coordinates       : %s"     , ua.getCoordinates());
			appendln("Universe Address           : 0x%014X", ua.getAddress());
			appendln("Portal Glyph Code          : %012X"  , portalGlyphCode);
			appendln("Extended SignalBoster Code : %s"     , ua.getExtendedSigBoostCode());
			
			showDiscNameObj(node,planet);
			
			if (!planet.additionalInfos.isEmpty()) {
				appendln();
				appendln("Additional Infos:");
				if (planet.additionalInfos.hasExocraftSummoningStation)
					appendln("    Exocraft Summoning Station on Planet");
				for (PersistentPlayerBase base:planet.additionalInfos.bases)
					appendln("    Base on Planet: \"%s\"", base.name);
			}
		}

		private Icon createPortalGlyphs(long portalGlyphCode) {
			BufferedImage image = new BufferedImage(50*12, 45*1, BufferedImage.TYPE_INT_RGB);
			Graphics graphics = image.getGraphics();
			
			for (int i=11; i>=0; --i) {
				int nr = (int)(portalGlyphCode&0xF);
				portalGlyphCode = portalGlyphCode>>4;
				BufferedImage glyph = PortalGlyphsIS.getCachedImage(nr);
				graphics.drawImage(glyph, i*50, 0, null);
			}
			return new ImageIcon(image);
		}
	}
	
	private abstract class AbstractContextmenu extends JPopupMenu {
		private static final long serialVersionUID = -2141303182163706658L;
		
		AbstractContextmenu(String name) {
			super(name);
		}
		protected void addDefaultItems() {
			add(createMenuItem("Remove all Highlights",UniverseTreeActionCommand.RemoveHighlights));
			add(createMenuItem("Find Solar System or Planet",UniverseTreeActionCommand.FindObject));
			addSeparator();
			add(createMenuItem("Expand complete tree",UniverseTreeActionCommand.ExpandAll));
			add(createMenuItem("Collapse remaining tree",UniverseTreeActionCommand.CollapseRemainingTree));
			add(createMenuItem("Show Full Discovered Universe Data",UniverseTreeActionCommand.ShowFullDiscoveredUniverseData));
		}
	}
	
	private class Contextmenu_Other extends AbstractContextmenu {
		private static final long serialVersionUID = 46825168641238400L;
		Contextmenu_Other() {
			super("Other");
			addDefaultItems();
		}
	}
	
	private class Contextmenu_SolarSystem extends AbstractContextmenu {
		private static final long serialVersionUID = -735489295003911054L;
		
		private JMenuItem miSetName;
		private ListMenu<Race> miSetRace;
		private ListMenu<StarClass> miSetStarClass;
		private ListMenu<Integer> miSetConflictLevel;
		private JCheckBoxMenuItem miUnexplored;
		
		Contextmenu_SolarSystem() {
			super("SolarSystem");
			
			add(miSetName = createMenuItem("Change Name",UniverseTreeActionCommand.SetName));
			add(createMenuItem("Set measured distance to center of galaxy ",UniverseTreeActionCommand.SetDistance));
			
			addSeparator();
			miSetRace = new Gui.ListMenu<Universe.SolarSystem.Race>("Set Dominant Race",Universe.SolarSystem.Race.values(),null,
				new Gui.ListMenuItems.ExternFunction<Universe.SolarSystem.Race>() {
					@Override public void setResult(Race value) {
						if (clickedNode instanceof SolarSystemNode) {
							((SolarSystemNode)clickedNode).value.race = value;
							updateTreeNodeAndInfoPanel(clickedNode, false);
						}
					}
					@Override public void configureMenuItem(JMenuItem menuItem, Race value) {
						if (value==null) {
							menuItem.setIcon(null);
							menuItem.setText("<none>");
						} else {
							menuItem.setIcon(SolarSystemIcons.RaceIcons.get(value));
							menuItem.setText(value.fullName);
						}
					}
				}
			);
			miSetRace.setShowSelectedValue(true);
			add(miSetRace);
			
			miSetStarClass = new Gui.ListMenu<Universe.SolarSystem.StarClass>("Set Star Class",Universe.SolarSystem.StarClass.values(),null,
				new Gui.ListMenuItems.ExternFunction<Universe.SolarSystem.StarClass>() {
					@Override public void setResult(StarClass value) {
						if (clickedNode instanceof SolarSystemNode) {
							((SolarSystemNode)clickedNode).value.starClass = value;
							updateTreeNodeAndInfoPanel(clickedNode, false);
						}
					}
					@Override public void configureMenuItem(JMenuItem menuItem, StarClass value) {
						if (value==null) {
							menuItem.setIcon(null);
							menuItem.setText("<none>");
						} else {
							menuItem.setIcon(SolarSystemIcons.StarClassIcons.get(value));
							menuItem.setText(value.toString()+" Class");
						}
					}
				}
			);
			miSetStarClass.setShowSelectedValue(true);
			add(miSetStarClass);
			
			miSetConflictLevel = new Gui.ListMenu<Integer>("Set Conflict Level",new Integer[]{1,2,3},null,
				new Gui.ListMenuItems.ExternFunction<Integer>() {
					@Override public void setResult(Integer value) {
						if (clickedNode instanceof SolarSystemNode) {
							((SolarSystemNode)clickedNode).value.conflictLevel = value==null?-1:value;
							updateTreeNodeAndInfoPanel(clickedNode, false);
						}
					}
					@Override public void configureMenuItem(JMenuItem menuItem, Integer value) {
						if (value==null) {
							menuItem.setIcon(null);
							menuItem.setText("<none>");
						} else {
							menuItem.setIcon(SolarSystemIcons.ConflictLevelIcons[value-1]);
							menuItem.setText("Conflict Level "+value);
						}
					}
				}
			);
			miSetConflictLevel.setShowSelectedValue(true);
			add(miSetConflictLevel);
			
			add(miUnexplored = new JCheckBoxMenuItem("Unexplored", false));
			miUnexplored.addActionListener(e->{
				if (clickedNode instanceof SolarSystemNode) {
					((SolarSystemNode)clickedNode).value.isUnexplored = miUnexplored.isSelected();
					miSetRace.setEnabled(!miUnexplored.isSelected());
					miSetConflictLevel.setEnabled(!miUnexplored.isSelected());
					updateTreeNodeAndInfoPanel(clickedNode, false);
				}
			});
			
			addSeparator();
			addDefaultItems();
		}

		public void setSolarSystem(SolarSystem system) {
			miSetName.setText(system.hasOriginalName()?"Change name":"Set name");
			miSetRace.setValue(system.race);
			miSetStarClass.setValue(system.starClass);
			miSetConflictLevel.setValue(1<=system.conflictLevel&&system.conflictLevel<=3?system.conflictLevel:null);
			miUnexplored.setSelected(system.isUnexplored);
			miSetRace.setEnabled(!system.isUnexplored);
			miSetConflictLevel.setEnabled(!system.isUnexplored);
		}
	}
	
	private class Contextmenu_Planet extends AbstractContextmenu {
		private static final long serialVersionUID = 170749132550138734L;
		
		private JMenuItem miSetName;
		private ListMenu<Biome> miSetBiome;
		private JCheckBoxMenuItem miAggressiveSentinels;
//		private ListMenuItems<SentinelLevel> miSetSentinelLevel;
		
		Contextmenu_Planet() {
			super("Planet");
			
			add(miSetName = createMenuItem("Change Name",UniverseTreeActionCommand.SetName));
			
			addSeparator();
			miSetBiome = new Gui.ListMenu<Biome>("Set Biome",Biome.values(),null,
				new Gui.ListMenuItems.ExternFunction<Biome>() {
					@Override public void setResult(Biome value) {
						if (clickedNode instanceof PlanetNode) {
							((PlanetNode)clickedNode).value.biome = value;
							updateTreeNodeAndInfoPanel(clickedNode,false);
						}
					}
					@Override public void configureMenuItem(JMenuItem menuItem, Biome value) {
						if (value==null) {
							menuItem.setIcon(null);
							menuItem.setText("<none>");
						} else {
							menuItem.setIcon(PlanetIcons.BiomeIcons.get(value));
							menuItem.setText(value.name_EN);
						}
					}
				}
			);
			miSetBiome.setShowSelectedValue(true);
			add(miSetBiome);
			
			add(miAggressiveSentinels = new JCheckBoxMenuItem("Aggressive Sentinels", false));
			miAggressiveSentinels.addActionListener(e->{
				if (clickedNode instanceof PlanetNode) {
					((PlanetNode)clickedNode).value.areSentinelsAggressive = miAggressiveSentinels.isSelected();
					updateTreeNodeAndInfoPanel(clickedNode,false);
				}
			});
/*			
			addSeparator();
			miSetSentinelLevel = new Gui.ListMenuItems<Universe.Planet.SentinelLevel>(Universe.Planet.SentinelLevel.values(),null,
				new Gui.ListMenuItems.ExternFunction<Universe.Planet.SentinelLevel>() {
					@Override public void setResult(Universe.Planet.SentinelLevel value) {
						if (clickedNode instanceof PlanetNode) {
							((PlanetNode)clickedNode).value.sentinelLevel = value;
							treeModel.nodeChanged(clickedNode);
							if (selectedNode==clickedNode) selectionChanged();
							GameInfos.saveUniverseObjectDataToFile(data.universe);
						}
					}
					@Override public void configureMenuItem(JMenuItem menuItem, Universe.Planet.SentinelLevel value) {
						if (value==null) {
							menuItem.setIcon(null);
							menuItem.setText("<none>");
						} else {
							menuItem.setIcon(PlanetIcons.SentinelLevelIcons.get(value));
							menuItem.setText(value.name);
						}
					}
				}
			);
			miSetSentinelLevel.addTo(this);
*/			
			addSeparator();
			addDefaultItems();
		}
		
		public void setPlanet(Planet planet) {
			miSetName.setText(planet.hasOriginalName()?"Change name":"Set name");
			miSetBiome.setValue(planet.biome);
			miAggressiveSentinels.setSelected(planet.areSentinelsAggressive);
		}
	}
	
	private class Contextmenu_Region extends AbstractContextmenu {
		private static final long serialVersionUID = 6065947291015643167L;
		
		private JMenuItem miSetName;
		
		Contextmenu_Region() {
			super("Region");
			add(miSetName = createMenuItem("Change Name",UniverseTreeActionCommand.SetName));
			
			addSeparator();
			addDefaultItems();
		}

		public void setRegion(Region region) {
			miSetName.setText(region.hasName()?"Change name":"Set name");
		}
	}

	private JMenuItem createMenuItem(String label, UniverseTreeActionCommand actionCommand) {
		JMenuItem menuItem = new JMenuItem(label);
		menuItem.addActionListener(this);
		menuItem.setActionCommand(actionCommand.toString());
		return menuItem;
	}
	
	@Override
	public void updateContent() {
		for (int i=0; i<tree.getRowCount(); ++i) {
			TreePath path = tree.getPathForRow(i);
			if (path==null) continue;
			Object comp = path.getLastPathComponent();
			if (comp instanceof TreeNode)
				treeModel.nodeChanged((TreeNode)comp);
		}
	}
	
	private void executeAction(Object source, UniverseTreeActionCommand command) {
		actionPerformed(new ActionEvent(source, ActionEvent.ACTION_FIRST, command.toString()));
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		UniverseTreeActionCommand actionCommand = UniverseTreeActionCommand.valueOf(e.getActionCommand());
		switch(actionCommand) {
		case SetName:
			if (clickedNode!=null) {
				Planet planet;
				SolarSystem system;
				Region region;
				switch(clickedNode.type) {
				case Planet     : planet = ((     PlanetNode)clickedNode).value; setNameForUniverseAddress(planet, "planet"      +" "+planet.getUniverseAddress().getExtendedSigBoostCode_Planet()     ); break;
				case SolarSystem: system = ((SolarSystemNode)clickedNode).value; setNameForUniverseAddress(system, "solar system"+" "+system.getUniverseAddress().getExtendedSigBoostCode_SolarSystem()); break;
				case Region     : region = ((     RegionNode)clickedNode).value; setNameForUniverseAddress(region, "region"      +" "+region.getUniverseAddress().getExtendedSigBoostCode_Region()     ); break;
				default:break;
				}
				updateTreeNodeAndInfoPanel(clickedNode, false, false);
			}
			break;
			
		case SetDistance:
			if (clickedNode instanceof SolarSystemNode) {
				SolarSystem system=((SolarSystemNode)clickedNode).value;
				UniverseAddress ua = system.getUniverseAddress();
				String initialValue;
				if (system.distanceToCenter==null) initialValue="";
				else initialValue=String.format(Locale.ENGLISH,"%f",system.distanceToCenter.doubleValue());
				String valueStr = JOptionPane.showInputDialog(this, "Distance to center of galaxy for solar system \""+ua.getSigBoostCode()+"\"",initialValue);
				if (valueStr!=null) {
					if (valueStr.isEmpty())
						system.distanceToCenter=null;
					else {
						try { system.distanceToCenter = Double.parseDouble(valueStr); }
						catch (NumberFormatException e1) {}
					}
					GameInfos.saveUniverseObjectDataToFile(data.universe);
				}
			}
			break;
			
		case ExpandAll:
			expandFullTree();
			break;
			
		case CollapseRemainingTree:
			for (int row=tree.getRowCount()-1; row>=0; --row)
				tree.collapseRow(row);
			if (clickedTreePath!=null)
				expandPath(clickedTreePath);
			break;
			
		case FindObject: {
			FindObjectDialog dialog = new FindObjectDialog(mainWindow,data.universe);
			dialog.showDialog();
			GenericTreeNode<?>[] changedObjects = dialog.getChangedObjects();
			show(changedObjects);
			updateChangedObjects(treeRoot,changedObjects);
			//tree.repaint();
			} break;
			
		case RemoveHighlights: {
			Vector<GenericTreeNode<?>> changedNodes = new Vector<>(); 
			UniverseNode uNode = (UniverseNode)data.universe.guiComp;
			if (uNode.isHighlighted) {
				uNode.isHighlighted = false;
				changedNodes.addElement(uNode);
			}
			for (Galaxy g:data.universe.galaxies) {
				GalaxyNode gNode = (GalaxyNode)g.guiComp;
				if (gNode.isHighlighted) {
					gNode.isHighlighted = false;
					changedNodes.addElement(gNode);
				}
				for (Region r:g.regions) {
					RegionNode rNode = (RegionNode)r.guiComp;
					if (rNode.isHighlighted) {
						rNode.isHighlighted = false;
						changedNodes.addElement(rNode);
					}
					for (SolarSystem s:r.solarSystems) {
						SolarSystemNode sNode = (SolarSystemNode)s.guiComp;
						if (sNode.isHighlighted) {
							sNode.isHighlighted = false;
							changedNodes.addElement(sNode);
						}
						for (Planet p:s.planets) {
							PlanetNode pNode = (PlanetNode)p.guiComp;
							if (pNode.isHighlighted) {
								pNode.isHighlighted = false;
								changedNodes.addElement(pNode);
							}
						}
					}
				}
			}
			updateChangedTreeNodes(changedNodes.toArray(new GenericTreeNode<?>[0]));
			} break;
			
		case ShowFullDiscoveredUniverseData:
			GameInfos.readUniverseObjectDataFromDataPool(data.universe, true);
			data.universe.sort();
			treeRoot = new UniverseNode(data.universe);
			treeModel.setRoot(treeRoot);
			expandFullTree();
			if (galaxyMapPanel!=null) galaxyMapPanel.updateUniverseData();
			break;
		}
		clickedNode = null;
		clickedTreePath = null;
	}

	public void highlightRegions(int galaxyIndex, int voxelX, int voxelZ) {
		Vector<GenericTreeNode<?>> changedObjects = new Vector<>(); 
		
		Galaxy g = data.universe.findGalaxy(galaxyIndex);
		if (g==null) return;
		
		for (Region r:g.regions) {
			if (r.voxelX==voxelX && r.voxelZ==voxelZ) {
				RegionNode rNode = (RegionNode)r.guiComp;
				if (!rNode.isHighlighted) {
					rNode.isHighlighted = true;
					changedObjects.addElement(rNode);
				}
			}
		}
		updateChangedTreeNodes(changedObjects.toArray(new GenericTreeNode<?>[0]));
	}

	private void show(GenericTreeNode<?>[] changedObjects) {
		SaveViewer.log_ln("Changed Objects:");
		for (GenericTreeNode<?> obj:changedObjects) {
			SaveViewer.log_ln("   "+obj.toString());
		}
	}

	private void updateInfoPanel() {
		AbstractInfoPanel prevInfoPanel = currentInfoPanel;
		
		if (selectedNode==null) {
			currentInfoPanel=infoPanel_Other;
			currentInfoPanel.setContent(null);
		} else {
			switch(selectedNode.type) {
			case Planet     : currentInfoPanel = infoPanel_Planet;      break;
			case SolarSystem: currentInfoPanel = infoPanel_SolarSystem; break;
			default         : currentInfoPanel = infoPanel_Other;       break;
			}
			currentInfoPanel.setContent(selectedNode);
		}
		
		remove(prevInfoPanel); 
		add(currentInfoPanel,BorderLayout.EAST);
		repaint();
		revalidate();
	}

	private void updateTreeNode(GenericTreeNode<?> node, boolean updateParent) {
		treeModel.nodeChanged(node);
		if (updateParent) treeModel.nodeChanged(node.parent);
		GameInfos.saveUniverseObjectDataToFile(data.universe);
	}

	private void updateTreeNodeAndInfoPanel(GenericTreeNode<?> node, boolean updateParent) {
		updateTreeNodeAndInfoPanel(node, updateParent, true);
	}

	private void updateTreeNodeAndInfoPanel(GenericTreeNode<?> node, boolean updateParent, boolean saveDataToFile) {
		treeModel.nodeChanged(node);
		if (updateParent) treeModel.nodeChanged(node.parent);
		if (saveDataToFile) GameInfos.saveUniverseObjectDataToFile(data.universe);
		if (selectedNode==node) updateInfoPanel();
	}

	private void updateChangedTreeNodes(GenericTreeNode<?>[] changedTreeNode) {
		for (GenericTreeNode<?> node:changedTreeNode)
			treeModel.nodeChanged(node);
	}

	private void updateChangedObjects(LocalTreeNode node, Object[] changedObjects) {
		for (LocalTreeNode child:node.getChildren()) {
			if (child instanceof GenericTreeNode<?>) {
				Object value = ((GenericTreeNode<?>)child).value;
				for (Object obj:changedObjects)
					if (value.equals(obj)) {
						treeModel.nodeChanged(child);
						break;
					}
			}
			updateChangedObjects(child, changedObjects);
		}
	}

	private void expandFullTree() {
		for (int row=0; row<tree.getRowCount(); ++row)
			if (!tree.isExpanded(row))
				tree.expandRow(row);
	}

	private void expandPath(TreePath path) {
		TreePath parentPath = path.getParentPath();
		if (parentPath!=null) expandPath(parentPath);
		tree.expandPath(path);
	}
	
	private class TreeListener implements TreeSelectionListener, MouseListener {
		private long clicktime;
		private GenericTreeNode<?> lastDblclickedNode;
		TreeListener() {
			clicktime = 0;
			lastDblclickedNode = null;
		}

		@Override public void mousePressed(MouseEvent e) {}
		@Override public void mouseReleased(MouseEvent e) {}
		@Override public void mouseEntered(MouseEvent e) {}
		@Override public void mouseExited(MouseEvent e) {}
		@Override public void mouseClicked(MouseEvent e) {
			switch (e.getButton()) {

			case MouseEvent.BUTTON1:
				TreePath dblclickedTreePath = tree.getPathForLocation(e.getX(), e.getY());
				GenericTreeNode<?> dblclickedNode = null;
				if (dblclickedTreePath!=null) {
					Object object = dblclickedTreePath.getLastPathComponent();
					if (object instanceof GenericTreeNode<?>)
						dblclickedNode = (GenericTreeNode<?>)object;
				}
				if (dblclickedNode!=null) {
					if (lastDblclickedNode==dblclickedNode && System.currentTimeMillis()-clicktime<700) {
						clickedTreePath = dblclickedTreePath;
						clickedNode = dblclickedNode;
						UniversePanel.this.mouseDblLeftClicked();
						lastDblclickedNode = null;
						clicktime = 0;
					} else {
						lastDblclickedNode = dblclickedNode;
						clicktime = System.currentTimeMillis();
					}
				}
				break;
				
			case MouseEvent.BUTTON2:
				clickedTreePath = tree.getPathForLocation(e.getX(), e.getY());
				clickedNode = null;
				if (clickedTreePath!=null) {
					Object object = clickedTreePath.getLastPathComponent();
					if (object instanceof GenericTreeNode<?>)
						clickedNode = (GenericTreeNode<?>)object;
				}
				UniversePanel.this.mouseMidClicked();
				break;
				
			case MouseEvent.BUTTON3:
				//System.out.println("mouseClicked( BUTTON3, "+e.getX()+", "+e.getY()+" )");
				clickedTreePath = tree.getPathForLocation(e.getX(), e.getY());
				clickedNode = null;
				if (clickedTreePath!=null) {
					Object object = clickedTreePath.getLastPathComponent();
					if (object instanceof GenericTreeNode<?>)
						clickedNode = (GenericTreeNode<?>)object;
				}
				JPopupMenu contextMenu = UniversePanel.this.mouseRightClicked();
				contextMenu.show(tree, e.getX(), e.getY());
				break;
			}
		}

		@Override
		public void valueChanged(TreeSelectionEvent e) {
			Object comp = e.getPath().getLastPathComponent();
			if (!(comp instanceof GenericTreeNode<?>))
				selectedNode = null;
			else
				selectedNode = (GenericTreeNode<?>)comp;
			updateInfoPanel();
		}
	}
	
	private JPopupMenu mouseRightClicked() {
		if (clickedNode==null)
			return contextMenu_Other;
		
		switch(clickedNode.type) {
		case SolarSystem:
			contextMenu_SolarSystem.setSolarSystem(((SolarSystemNode)clickedNode).value);
			return contextMenu_SolarSystem;
			
		case Region:
			contextMenu_Region.setRegion(((RegionNode)clickedNode).value);
			return contextMenu_Region;
			
		case Planet:
			contextMenu_Planet.setPlanet(((PlanetNode)clickedNode).value);
			return contextMenu_Planet;
			
		default:
			return contextMenu_Other;
		}
	}

	public void mouseMidClicked() {
		if (clickedNode==null) return;
		
		switch(clickedNode.type) {
		case Region:
		case SolarSystem:
		case Planet:
			executeAction(tree, UniverseTreeActionCommand.SetName);
			break;
		default:
			break;
		}
	}

	public void mouseDblLeftClicked() {
		if (clickedNode==null) return;
		
		switch(clickedNode.type) {
		case Planet:
			executeAction(tree, UniverseTreeActionCommand.SetName);
			break;
		default:
			break;
		}
	}

	private class SearchBar extends JPanel {
		private static final long serialVersionUID = -322241964276963216L;
		
		private GridBagLayout layout;
		private GridBagConstraints gbc;

		private Gui.IconComboBox<Biome> cmbbxBiome;

		private boolean disableUpdates;
		
		SearchBar() {
			super();
			setLayout(layout = new GridBagLayout());
			gbc = new GridBagConstraints();
			setBorder(BorderFactory.createTitledBorder("Search"));
			disableUpdates = false;
			
			cmbbxBiome = new Gui.IconComboBox<Biome>( SaveViewer.addNull(Biome.values()), 100,16, new Gui.IconComboBox.ExternalFunctionality<Biome>() {
				@Override public Biome cast(Object obj) {
					if (!(obj instanceof Biome)) return null;
					return (Biome)obj;
				}
				@Override public Icon createIcon(Biome value) {
					return PlanetIcons.BiomeIcons.get(value);
				}
				@Override public String getLabel(Biome value) {
					if (value==null) return "<none>";
					return value.name_EN;
				}
			});
			cmbbxBiome.addActionListener(e->updateMarkers());
			
			//addComp( SaveViewer.createButton("X", e->{}), 0, GridBagConstraints.BOTH);
			addComp( SaveViewer.createButton("Clear Markers", e->clearMarkers()), 0, GridBagConstraints.BOTH);
			addComp( cmbbxBiome, 0, GridBagConstraints.BOTH);
			
			addComp( new JLabel(""), 1, GridBagConstraints.BOTH);
		}
		
		
		private void clearMarkers() {
			disableUpdates = true;
			cmbbxBiome.setSelectedItem(null);
			disableUpdates = false;
			updateMarkers();
		}


		private void updateMarkers() {
			if (disableUpdates) return;
			GenericTreeNode<?> node;
			int markedPlanets = 0;
			Biome biome = cmbbxBiome.getSelected();
			for (Galaxy g:data.universe.galaxies) {
				for (Region r:g.regions) {
					for (SolarSystem s:r.solarSystems) {
						for (Planet p:s.planets) {
							node = (GenericTreeNode<?>)p.guiComp;
							boolean isBiome = biome!=null && p.biome==biome;
							boolean isMarked = isBiome;
							setMarker( node, isMarked );
							if (isMarked) ++markedPlanets;
						}
					}
				}
			}
			SaveViewer.log_ln("marked: %d Planets", markedPlanets);
		}


		private void setMarker(GenericTreeNode<?> node, boolean b) {
			if (node.isHighlighted != b) {
				node.isHighlighted = b;
				treeModel.nodeChanged(node);
			}
		}


		private void addComp(Component comp, double weightx, int fill) {
			gbc.weightx=weightx;
			gbc.weighty=0;
			gbc.gridwidth=1;
			gbc.gridheight=1;
			gbc.fill = fill;
			layout.setConstraints(comp, gbc);
			add(comp);
		}
	}

	private static class FindObjectDialog extends StandardDialog {
		private static final long serialVersionUID = -356863578675221086L;
		
		private Universe universe;
		private SimplifiedTable table;
		private FoundExtraInfoTableModel tableModel;

		private JButton btnOK;

		public FindObjectDialog(Window parent, Universe universe) {
			super(parent, "Find Universe Object", ModalityType.APPLICATION_MODAL);
			this.universe = universe;
			
			table = new SimplifiedTable("FindObjectDialog",false,SaveViewer.DEBUG,true);
			table.setPreferredScrollableViewportSize(new Dimension(500, 600));;
			tableModel = null;
			
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			JCheckBox chkbx;
			buttonPanel.add(chkbx = SaveViewer.createCheckbox("Allow Editing", null, false));
			buttonPanel.add(btnOK = SaveViewer.createButton("Show Selected in Tree", e->{ if (tableModel!=null) tableModel.setSelected(); closeDialog(); }));
			buttonPanel.add(SaveViewer.createButton("Close", e->closeDialog()));
			
			chkbx.addActionListener(e->{ if (tableModel!=null) { tableModel.allowEditing(chkbx.isSelected()); table.setModel(tableModel); } });
			
			JPanel contentPane = new JPanel(new BorderLayout(3,3));
			contentPane.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
			contentPane.add(new JScrollPane(table),BorderLayout.CENTER);
			contentPane.add(buttonPanel,BorderLayout.SOUTH);
			
			updateOKbutton();
			createGUI( contentPane );
			setSizeAsMinSize();
		}

		private void updateOKbutton() {
			if (tableModel!=null && tableModel.isSomethingSelected())
				btnOK.setText("Show selected in tree view");
			else
				btnOK.setText("Remove markers in tree view");
		}

		public GenericTreeNode<?>[] getChangedObjects() {
			if (tableModel!=null) return tableModel.changedObj.toArray(new GenericTreeNode<?>[0]);
			return new GenericTreeNode<?>[0];
		}

		@Override
		public void showDialog() {
			Vector<FoundExtraInfo> infos = new Vector<>();
			
			for (Galaxy g:universe.galaxies)
				for (Region r:g.regions)
					for (SolarSystem s:r.solarSystems) {
						addInfos(infos,s,false);
						for (Planet p:s.planets)
							addInfos(infos,p,true);
					}
			
			tableModel = new FoundExtraInfoTableModel(this, universe, infos.toArray(new FoundExtraInfo[0]));
			table.setModel(tableModel);
			
			super.showDialog();
		}

		private void addInfos(Vector<FoundExtraInfo> infos, UniverseObject obj, boolean isPlanet) {
			for (ExtraInfo ei:obj.extraInfos) {
				FoundExtraInfo existingFEI = null;
				for (FoundExtraInfo fei:infos) {
					if (fei.fromPlanet != isPlanet) continue;
					if (!fei.label.equals(ei.shortLabel)) continue;
					if (!fei.info.equals(ei.info)) continue;
					existingFEI = fei;
					break;
				}
				if (existingFEI!=null) existingFEI.add(obj,ei);
				else infos.add(new FoundExtraInfo(isPlanet,obj,ei));
			}
		}

		private static class FoundExtraInfo {
			boolean isSelected;
			boolean fromPlanet;
			String label;
			String info;
			Vector<ExtraInfo> sourceEIs;
			Vector<UniverseObject> sourceObjs;
			
			public FoundExtraInfo(boolean fromPlanet, UniverseObject obj, ExtraInfo ei) {
				this.fromPlanet = fromPlanet;
				this.label = ei.shortLabel;
				this.info = ei.info;
				this.sourceEIs = new Vector<>();
				this.sourceObjs = new Vector<>();
				this.isSelected = false;
				add(obj,ei);
			}

			public void add(UniverseObject obj, ExtraInfo ei) {
				this.sourceEIs.add(ei);
				this.sourceObjs.add(obj);
			}
		}

		private enum FoundExtraInfoColumnID implements TableView.SimplifiedColumnIDInterface {
			Selected(""      , Boolean.class, 10,-1, 30, 30),
			Source  ("Source", String .class, 20,-1, 90, 90),
			Label   ("Label" , String .class, 20,-1, 70, 70),
			Info    ("Info"  , String .class, 50,-1,300,300);
			
			private SimplifiedColumnConfig config;

			private FoundExtraInfoColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				config = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			
			@Override public SimplifiedColumnConfig getColumnConfig() { return config; }
		}
		
		private static class FoundExtraInfoTableModel extends TableView.SimplifiedTableModel<FoundExtraInfoColumnID> {

			private FindObjectDialog dialog;
			private FoundExtraInfo[] data;
			private HashSet<GenericTreeNode<?>> changedObj;
			private boolean allowEditing;
			private Universe universe;

			protected FoundExtraInfoTableModel(FindObjectDialog dialog, Universe universe, FoundExtraInfo[] data) {
				super(FoundExtraInfoColumnID.values());
				this.dialog = dialog;
				this.universe = universe;
				this.data = data;
				this.changedObj = new HashSet<>();
				this.allowEditing = false;
			}

			public boolean isSomethingSelected() {
				for (FoundExtraInfo fei:data)
					if (fei.isSelected)
						return true;
				return false;
			}

			public void allowEditing(boolean allowEditing) {
				this.allowEditing = allowEditing;
			}

			public void setSelected() {
				//changedObj.clear();
				HashSet<UniverseObject> notSelectedObjs = new HashSet<>();
				HashSet<UniverseObject> selectedObjs = new HashSet<>();
				for (FoundExtraInfo fei:data)
					if (fei.isSelected)
						selectedObjs.addAll(fei.sourceObjs);
					else
						notSelectedObjs.addAll(fei.sourceObjs);
				notSelectedObjs.removeAll(selectedObjs);
				
				for (UniverseObject obj:   selectedObjs) setHighlighted(obj, true );
				for (UniverseObject obj:notSelectedObjs) setHighlighted(obj, false);
			}

			private void setHighlighted(UniverseObject obj, boolean highlighted) {
				GenericTreeNode<?> node = (GenericTreeNode<?>)obj.guiComp;
				if (node.isHighlighted!=highlighted) {
					changedObj.add(node);
					node.isHighlighted = highlighted;
				}
			}

			@Override public int getRowCount() { return data.length; }

			@Override
			public Object getValueAt(int rowIndex, int columnIndex, FoundExtraInfoColumnID columnID) {
				switch(columnID) {
				case Selected: return data[rowIndex].isSelected;
				case Source  : int n = data[rowIndex].sourceObjs.size(); return String.format("%d %s%s", n, data[rowIndex].fromPlanet?"Planet":"Solar System", n==1?"":"s");
				case Label   : return data[rowIndex].label;
				case Info    : return data[rowIndex].info;
				}
				return null;
			}

			@Override
			protected boolean isCellEditable(int rowIndex, int columnIndex, FoundExtraInfoColumnID columnID) {
				switch(columnID) {
				case Selected: return true;
				case Source  : return false;
				case Label   : return allowEditing;
				case Info    : return allowEditing;
				}
				return false;
			}

			@Override
			protected void setValueAt(Object aValue, int rowIndex, int columnIndex, FoundExtraInfoColumnID columnID) {
				switch(columnID) {
				case Selected:
					data[rowIndex].isSelected = (Boolean)aValue;
					dialog.updateOKbutton();
					break;
					
				case Source  : break;
				case Label   :
					data[rowIndex].label = (String)aValue;
					for (     ExtraInfo  ei:data[rowIndex].sourceEIs ) ei.shortLabel = (String)aValue;
					for (UniverseObject obj:data[rowIndex].sourceObjs) changedObj.add((GenericTreeNode<?>)obj.guiComp);
					GameInfos.saveUniverseObjectDataToFile(universe);
					break;
					
				case Info    :
					data[rowIndex].info = (String)aValue;
					for (     ExtraInfo  ei:data[rowIndex].sourceEIs ) ei.info = (String)aValue;
					for (UniverseObject obj:data[rowIndex].sourceObjs) changedObj.add((GenericTreeNode<?>)obj.guiComp);
					GameInfos.saveUniverseObjectDataToFile(universe);
					break;
				}
			}
			
		}
		
	}
	
	static class UniverseTreeCellRenderer extends DefaultTreeCellRenderer {

		private static final Color TEXTCOLOR__HIGHLIGHTED  = Color.RED;
		private static final Color TEXTCOLOR__CURRENT_POS  = new Color(0x2EA000);
		private static final Color TEXTCOLOR__WITHOUT_NAME = new Color(0x808080);
		private static final Color TEXTCOLOR__NOT_UPLOADED = new Color(0x0000FF); // or 0x1D67AE
		private static final Color TEXTCOLOR__ONLY_IN_DB   = Color.MAGENTA;

		private static final long serialVersionUID = 4733567681038484432L;
		
		private Font boldfont;
		private Font standardFont;
		
		UniverseTreeCellRenderer() {
			standardFont = UIManager.getFont("Tree.font");
			boldfont = standardFont.deriveFont(Font.BOLD);
		}

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			Component component = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
			if (component instanceof JLabel && value instanceof GenericTreeNode<?>)
				setValues((JLabel)component, selected, (GenericTreeNode<?>)value);
			return component;
		}

		private void setValues(JLabel component, boolean selected, GenericTreeNode<?> node) {
			switch (node.type) {
			case Universe   : component.setIcon(UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.Universe   )); break;
			case Galaxy     : component.setIcon(UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.Galaxy     )); break;
			case Region     : component.setIcon(UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.Region     )); break;
			case SolarSystem:
				if (node instanceof SolarSystemNode) {
					SolarSystemNode solarSystemNode = (SolarSystemNode)node;
					SolarSystem system = solarSystemNode.value;
					Icon icon;
					if (!system.additionalInfos.isEmpty() || system.hasAtlasInterface || system.hasBlackHole) {
						if (solarSystemNode.cachedCustomIcon!=null && solarSystemNode.cachedCustomIcon.is(system.race,system.starClass,system.conflictLevel,system.isUnexplored,system.hasAtlasInterface,system.hasBlackHole))
							icon = solarSystemNode.cachedCustomIcon.get();
						else {
							icon = SolarSystemIcons.get(system.race,system.starClass,system.conflictLevel, system.isUnexplored);
							if (system.hasAtlasInterface)
								icon = IconSource.setSideBySide(icon,AdditionalIcons.getCachedIcon(AdditionalTreeIcons.Atlas));
							if (system.hasBlackHole)
								icon = IconSource.setSideBySide(icon,AdditionalIcons.getCachedIcon(AdditionalTreeIcons.BlackHole));
							if (system.additionalInfos.hasTeleportEndPoint)
								icon = IconSource.setSideBySide(icon,AdditionalIcons.getCachedIcon(AdditionalTreeIcons.Teleporter));
							if (system.additionalInfos.hasFreighter)
								icon = IconSource.setSideBySide(icon,AdditionalIcons.getCachedIcon(AdditionalTreeIcons.Freighter));
							solarSystemNode.cachedCustomIcon = new SolarSystemNode.CachedCustomIcon(icon,system.race,system.starClass,system.conflictLevel,system.isUnexplored,system.hasAtlasInterface,system.hasBlackHole);
						}
					} else
						icon = SolarSystemIcons.get(system.race,system.starClass,system.conflictLevel, system.isUnexplored);
					component.setIcon(icon);
				}
				break;
			case Planet:
				if (node instanceof PlanetNode) {
					PlanetNode planetNode = (PlanetNode)node;
					Planet planet = planetNode.value;
					Icon icon;
					if (!planet.additionalInfos.isEmpty()) {
						if (planetNode.cachedCustomIcon!=null && planetNode.cachedCustomIcon.is(planet.biome, planet.areSentinelsAggressive))
							icon = planetNode.cachedCustomIcon.get();
						else {
							icon = PlanetIcons.get(planet.biome, planet.areSentinelsAggressive);
							if (planet.additionalInfos.hasTeleportEndPoint)
								icon = IconSource.setSideBySide(icon,AdditionalIcons.getCachedIcon(AdditionalTreeIcons.Teleporter));
							if (!planet.additionalInfos.bases.isEmpty())
								icon = IconSource.setSideBySide(icon,AdditionalIcons.getCachedIcon(AdditionalTreeIcons.BaseMainRoom));
							if (planet.additionalInfos.hasExocraftSummoningStation)
								icon = IconSource.setSideBySide(icon,AdditionalIcons.getCachedIcon(AdditionalTreeIcons.VehicleSummoner));
							planetNode.cachedCustomIcon = new PlanetNode.CachedCustomIcon(icon,planet.biome, planet.areSentinelsAggressive);
						}
					} else
						icon = PlanetIcons.get(planet.biome, planet.areSentinelsAggressive);
					component.setIcon(icon);
				}
				//setIcon(UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.Planet     ));
				break;
			}
			component.setFont(standardFont);
			Universe.UniverseObject uniobj = null;
			Region region = null;
			if (node instanceof      RegionNode) region = ((     RegionNode)node).value;
			if (node instanceof SolarSystemNode) uniobj = ((SolarSystemNode)node).value;
			if (node instanceof      PlanetNode) uniobj = ((     PlanetNode)node).value;
			if (uniobj != null) {
				if (!uniobj.hasOriginalName()) {
					if (!selected) component.setForeground(TEXTCOLOR__WITHOUT_NAME);
				} else
				if (uniobj.isNotUploaded) {
					if (!selected) component.setForeground(TEXTCOLOR__NOT_UPLOADED);
				}
				if (!uniobj.hasSourceID()) {
					if (!selected) component.setForeground(TEXTCOLOR__ONLY_IN_DB);
				} 
				if (uniobj.isCurrPos) {
					if (!selected) component.setForeground(TEXTCOLOR__CURRENT_POS);
					component.setFont(boldfont);
				} 
			}
			if (region!=null) {
				if (!region.hasName()) {
					if (!selected) component.setForeground(TEXTCOLOR__WITHOUT_NAME);
				}
			}
			if (node.isHighlighted) {
				if (!selected) component.setForeground(TEXTCOLOR__HIGHLIGHTED);
				component.setFont(boldfont);
			}
		}
	}
	
	enum NodeType { Universe, Galaxy, Region, SolarSystem, Planet }
	
	static abstract class LocalTreeNode extends TreeView.AbstractTreeNode<LocalTreeNode> {
		
		protected LocalTreeNode(LocalTreeNode parent) {
			super(parent);
		}

		@Override public String toString() { return getLabel(); }
		
		protected abstract String getLabel();
		protected abstract int getDataChildrenCount();
		protected abstract LocalTreeNode createTreeChild(int i);
		protected abstract Comparator<Integer> getSorter();

		@Override
		void createChildren() {
			children = new LocalTreeNode[getDataChildrenCount()];
			Comparator<Integer> sorter = getSorter();
			if (sorter!=null) {
				Integer[] order = new Integer[children.length];
				for (int i=0; i<children.length; ++i) order[i] = i;
				Arrays.sort(order,sorter);
				for (int i=0; i<order.length; ++i)
					children[i] = createTreeChild(order[i]);
			} else
				for (int i=0; i<children.length; ++i)
					children[i] = createTreeChild(i);
		}
		
	}
	
	static abstract class GenericTreeNode<V> extends LocalTreeNode {
		
		NodeType type;
		V value;
		boolean isHighlighted;

		protected GenericTreeNode(LocalTreeNode parent, NodeType type, V value) {
			super(parent);
			this.value = value;
			this.type = type;
			isHighlighted = false;
		}

		@Override public boolean getAllowsChildren() { return true; /*except Planet*/ }
		@Override protected String getLabel() { return value.toString(); }
		@Override protected Comparator<Integer> getSorter() { return null; }
		
	}
	
	static class UniverseNode extends GenericTreeNode<Universe> {
		private UniverseNode(Universe value) { super(null, NodeType.Universe, value); value.guiComp=this; }
		@Override protected int getDataChildrenCount() { return value.galaxies.size(); }
		@Override protected LocalTreeNode createTreeChild(int i) { return new GalaxyNode(this,value.galaxies.get(i)); }
	}
	static class GalaxyNode extends GenericTreeNode<Galaxy> {
		private GalaxyNode(UniverseNode parent, Galaxy value) { super(parent, NodeType.Galaxy, value); value.guiComp=this; }
		@Override protected int getDataChildrenCount() { return value.regions.size(); }
		@Override protected LocalTreeNode createTreeChild(int i) { return new RegionNode(this,value.regions.get(i)); }
//		@Override protected Comparator<Integer> getSorter() { return new Comparator<Integer>() {
//			@Override
//			public int compare(Integer i1, Integer i2) {
//				double d1 = value.regions.get(i1).distToCenter;
//				double d2 = value.regions.get(i2).distToCenter;
//				if (d1>d2) return -1;
//				if (d1<d2) return +1;
//				return 0;
//			}
//		}; }
	}
	static class RegionNode extends GenericTreeNode<Region> {
		private RegionNode(GalaxyNode parent, Region value) { super(parent, NodeType.Region, value); value.guiComp=this; }
		@Override protected int getDataChildrenCount() { return value.solarSystems.size(); }
		@Override protected LocalTreeNode createTreeChild(int i) { return new SolarSystemNode(this,value.solarSystems.get(i)); }
		@Override protected String getLabel() { return String.format(Locale.ENGLISH, "%s [%1.1f ly]", value.toString(), value.getUniverseAddress().getDistToCenter_inRegionUnits()*400); }
	}
	static class SolarSystemNode extends GenericTreeNode<SolarSystem> {
		CachedCustomIcon cachedCustomIcon;
		private SolarSystemNode(RegionNode parent, SolarSystem value) { super(parent, NodeType.SolarSystem, value); value.guiComp=this; cachedCustomIcon = null; }
		@Override protected int getDataChildrenCount() { return value.planets.size(); }
		@Override protected LocalTreeNode createTreeChild(int i) { return new PlanetNode(this,value.planets.get(i)); }
		//@Override protected String getLabel() { return value.hasName()?value.getName():super.getLabel(); }
		
		static class CachedCustomIcon {
			private final Icon icon;
			private final Race race;
			private final StarClass starClass;
			private final int conflictLevel;
			private final boolean isUnexplored;
			protected boolean hasAtlasInterface;
			private boolean hasBlackHole;

			public CachedCustomIcon(Icon icon, Race race, StarClass starClass, int conflictLevel, boolean isUnexplored, boolean hasAtlasInterface, boolean hasBlackHole) {
				this.icon = icon;
				this.race = race;
				this.starClass = starClass;
				this.conflictLevel = conflictLevel;
				this.isUnexplored = isUnexplored;
				this.hasAtlasInterface = hasAtlasInterface;
				this.hasBlackHole = hasBlackHole;
			}
			public boolean is(Race race, StarClass starClass, int conflictLevel, boolean isUnexplored, boolean hasAtlasInterface, boolean hasBlackHole) {
				return this.race==race && this.starClass==starClass && this.conflictLevel==conflictLevel && this.isUnexplored==isUnexplored && this.hasAtlasInterface==hasAtlasInterface && this.hasBlackHole==hasBlackHole;
			}
			public Icon get() {
				return icon;
			}
		}
	}
	static class PlanetNode extends GenericTreeNode<Planet> {
		CachedCustomIcon cachedCustomIcon;
		private PlanetNode(SolarSystemNode parent, Planet value) { super(parent, NodeType.Planet, value); value.guiComp=this; cachedCustomIcon = null; }
		@Override protected int getDataChildrenCount() { return 0; }
		@Override protected LocalTreeNode createTreeChild(int i) { throw new UnsupportedOperationException("Can't create a TreeChild from a PlanetNode."); }
		//@Override protected String getLabel() { return value.hasName()?value.getName():super.getLabel(); }
		@Override public boolean getAllowsChildren() { return false; }
		
		static class CachedCustomIcon {
			private final Icon icon;
			private final Biome biome;
			private final boolean areSentinelsAggressive;

			public CachedCustomIcon(Icon icon, Biome biome, boolean areSentinelsAggressive) {
				this.icon = icon;
				this.biome = biome;
				this.areSentinelsAggressive = areSentinelsAggressive;
			}
			public boolean is(Biome biome, boolean areSentinelsAggressive) {
				return this.biome==biome && this.areSentinelsAggressive==areSentinelsAggressive;
			}
			public Icon get() {
				return icon;
			}
		}
	}
}
