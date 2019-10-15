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
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.TreeSet;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
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
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.schwarzbaer.gui.Canvas;
import net.schwarzbaer.gui.IconSource;
import net.schwarzbaer.gui.IconSource.CachedIcons;
import net.schwarzbaer.gui.StandardDialog;
import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.gui.Tables.SimplifiedColumnIDInterface;
import net.schwarzbaer.gui.TristateCheckBox;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Gui;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Gui.ListMenu;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Gui.PopupDialog;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Gui.TextFieldWithSuggestions;
import net.schwarzbaer.java.games.nomanssky.saveviewer.ResourceHotSpots;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.PersistentPlayerBase;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.TeleportEndpoints;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.DiscoverableObject;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.DiscoverableObject.ExtraInfo;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Galaxy;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.ObjectWithSource;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Planet;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Planet.Biome;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Planet.BuriedTreasure;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Planet.Resources;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Region;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.SolarSystem;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.SolarSystem.Race;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.SolarSystem.StarClass;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.UniverseAddress;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.UniverseObject;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveViewer;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.SimplifiedTable;

public class UniversePanel extends SaveGameView.SaveGameViewTabPanel implements ActionListener {
	private static final long serialVersionUID = -4594889224613582352L;
	
	private enum UniverseTreeActionCommand { SetName, SetDistance, ExpandAll, CollapseRemainingTree, FindExtraInfo, RemoveHighlights, ShowFullDiscoveredUniverseData, ScrollToCurrentPosition, OpenResourceHotSpots }
	private enum UniverseTreeIcons {
		Universe, Galaxy, Region, SolarSystem, Planet,
		GekSys, KorvaxSys, VykeenSys, Unexplored,
		Yellow, Red, Green, Blue,
		ConflictLevel1, ConflictLevel2, ConflictLevel3,
		EconomyLevel1, EconomyLevel2, EconomyLevel3,
		;
	}
	private enum PlanetTreeIcons {
		BiomeUndef, BiomeLush, BiomeScorched, BiomeBarren, BiomeIrradiated, BiomeToxic, BiomeFrozen, BiomeAirless, BiomeExotic, BiomeExoticMega,
		BiomeAnomMetalFlowers, BiomeAnomShells, BiomeAnomBones, BiomeAnomMushrooms, BiomeAnomScreenCrystals, BiomeAnomFragmColumns, BiomeAnomBubbles, BiomeAnomLimeStars, BiomeAnomHexagons, BiomeAnomBeams, BiomeAnomContour,
		SentinelAggressive,
		;
	}
	private enum AdditionalTreeIcons {
		VehicleSummoner(20), BaseMainRoom(26), BaseMainRoomOther(26), Freighter(44), Teleporter(20), BlackHole(20), Atlas(17), Anomaly(20);

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
			icons[0] = iconNullId == null ? null : iconSource.getCachedIcon(iconNullId);
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
		
		protected Icon[][][][] combineIconArrays(Icon[] icons1, Icon[] icons2, Icon[] icons3, Icon[] icons4, IconID iconBgId) {
			Icon iconBG = iconBgId!=null?iconSource.getCachedIcon(iconBgId):null;
			Icon[][][][] icons = new Icon[icons1.length][icons2.length][icons3.length][icons4.length];
			Icon icon1,icon2,icon3;
			for (int i1=0; i1<icons1.length; i1++) {
				icon1 = IconSource.combine(iconBG, icons1[i1]);
				for (int i2=0; i2<icons2.length; i2++) {
					icon2 = IconSource.combine(icon1, icons2[i2]);
					for (int i3=0; i3<icons3.length; i3++) {
						icon3 = IconSource.combine(icon2, icons3[i3]);
						for (int i4=0; i4<icons4.length; i4++) {
							icons[i1][i2][i3][i4] = IconSource.combine(icon3, icons4[i4]);
						}
					}
				}
			}
			return icons;
		}
		
		protected <KeyType> Icon[] createIconArray(KeyType[] values, int x, int y, int w, int h, Function<KeyType,IconID> getIconID) {
			return createIconArray(values, x,y,w,h, getIconID, null);
		}
		protected <KeyType> Icon[] createIconArray(KeyType[] values, int x, int y, int w, int h, Function<KeyType,IconID> getIconID, Color bgColor) {
			Icon[] icons = new Icon[values.length];
			for (int i=0; i<values.length; i++) {
				IconID iconID = getIconID.apply(values[i]);
				Icon cachedIcon = iconSource.getCachedIcon(iconID);
				icons[i] = IconSource.cutIcon(cachedIcon,x,y,w,h,bgColor);
			}
			return icons;
		}
		
		protected <KeyType extends Enum<KeyType>> EnumMap<KeyType,Icon> createIconEnumMap(Class<KeyType> eClass, KeyType[] values, int x, int y, int w, int h, Function<KeyType,IconID> getIconID) {
			return createIconEnumMap(eClass, values, x,y,w,h, getIconID, null);
		}
		protected <KeyType extends Enum<KeyType>> EnumMap<KeyType,Icon> createIconEnumMap(Class<KeyType> eClass, KeyType[] values, int x, int y, int w, int h, Function<KeyType,IconID> getIconID, Color bgColor) {
			EnumMap<KeyType, Icon> enumMap = new EnumMap<>(eClass);
			for (KeyType value:values) {
				IconID iconID = getIconID.apply(value);
				Icon cachedIcon = iconSource.getCachedIcon(iconID);
				enumMap.put(value, IconSource.cutIcon(cachedIcon,x,y,w,h,bgColor)); 
			}
			return enumMap;
		}
	}
	
	private static class SolarSystemIcons extends IconsMap<UniverseTreeIcons> {
		
		private Icon[][][][] icons;
		private Icon[] unexploredIcons;
		
		EnumMap<Race,Icon> RaceIcons;
		EnumMap<StarClass,Icon> StarClassIcons;
		private Icon[] ConflictLevelIcons;
		private Icon[] EconomyLevelIcons;
		
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
			Function<Integer, UniverseTreeIcons> getEconomyLevelIconID = (Integer economyLevel)->{
				switch(economyLevel) {
				case 1: return UniverseTreeIcons.EconomyLevel1;
				case 2: return UniverseTreeIcons.EconomyLevel2;
				case 3: return UniverseTreeIcons.EconomyLevel3;
				}
				return null;
			};
			
			Icon[] raceIcons = createIconArray(Race.values(), null, getRaceIconID);
			Icon[] starClassIcons = createIconArray(StarClass.values(), null, getStarClassIconID);
			Icon[] conflictLevelIcons = createIconArray(new Integer[]{1,2,3}, null, getConflictLevelIconID);
			Icon[] economyLevelIcons = createIconArray(new Integer[]{1,2,3}, null, getEconomyLevelIconID);
			icons = combineIconArrays(raceIcons, starClassIcons, conflictLevelIcons, economyLevelIcons, UniverseTreeIcons.SolarSystem);
			
			Icon[] unexploredIcon = new Icon[] { null, iconSource.getCachedIcon(UniverseTreeIcons.Unexplored) };
			unexploredIcons = combineIconArrays(unexploredIcon, starClassIcons, UniverseTreeIcons.SolarSystem)[1];
			
			EconomyLevelIcons  = createIconArray(new Integer[]{1,2,3}, 25,3,5,17, getEconomyLevelIconID, Color.GRAY);
			ConflictLevelIcons = createIconArray(new Integer[]{1,2,3}, 0,9,11,11, getConflictLevelIconID);
			RaceIcons = createIconEnumMap(Race.class, Race.values(), 8,0,20,20, getRaceIconID);
			StarClassIcons = new EnumMap<>(StarClass.class);
			for (StarClass starClass:StarClass.values()) {
				Icon cachedIcon = get(null, starClass, -1, -1, false);
				StarClassIcons.put(starClass, IconSource.cutIcon(cachedIcon,0,0,20,20)); 
			}
		}
		
		Icon get(Race race, StarClass starClass, int conflictLevel, int economyLevel, boolean unexplored) {
			int raceIndex      = race     ==null?0:(race     .ordinal()+1);
			int starClassIndex = starClass==null?0:(starClass.ordinal()+1);
			if (conflictLevel<1 || 3<conflictLevel) conflictLevel = 0;
			if (economyLevel<1 || 3<economyLevel) economyLevel = 0;
			if (unexplored) return unexploredIcons[starClassIndex];
			return icons[raceIndex][starClassIndex][conflictLevel][economyLevel];
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
				case AnomMetalFlowers  : return PlanetTreeIcons.BiomeAnomMetalFlowers  ;
				case AnomShells        : return PlanetTreeIcons.BiomeAnomShells        ;
				case AnomBones         : return PlanetTreeIcons.BiomeAnomBones         ;
				case AnomMushrooms     : return PlanetTreeIcons.BiomeAnomMushrooms     ;
				case AnomScreenCrystals: return PlanetTreeIcons.BiomeAnomScreenCrystals;
				case AnomFragmColumns  : return PlanetTreeIcons.BiomeAnomFragmColumns  ;
				case AnomBubbles       : return PlanetTreeIcons.BiomeAnomBubbles       ;
				case AnomLimeStars     : return PlanetTreeIcons.BiomeAnomLimeStars     ;
				case AnomHexagons      : return PlanetTreeIcons.BiomeAnomHexagons      ;
				case AnomBeams         : return PlanetTreeIcons.BiomeAnomBeams         ;
				case AnomContour       : return PlanetTreeIcons.BiomeAnomContour       ;
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
		
		int offsetX = 0;
		int offsetY = TreeIconHeight*2;
		IconSource<AdditionalTreeIcons> UncachedAdditionalIcons = new IconSource<AdditionalTreeIcons>(offsetX,offsetY,TreeIconHeight, id->id.iconWidth, AdditionalTreeIcons.values());
		UncachedAdditionalIcons.readIconsFromResource("/images/UniverseTreeIcons.png");
		AdditionalIcons = UncachedAdditionalIcons.cacheIcons(AdditionalTreeIcons.values());
		
	}

	private JTree tree;
	private DefaultTreeModel treeModel;
	private UniverseNode treeRoot;
	private JScrollPane treeScrollPane;
	
	private GenericTreeNode<?> selectedNode;
	private GenericTreeNode<?> clickedNode;
	private TreePath clickedTreePath;
	private int[] markerList;
	private float selectedMarkerIndex;
	
	private Contextmenu_Other       contextMenu_Other;
	private Contextmenu_Region      contextMenu_Region;
	private Contextmenu_SolarSystem contextMenu_SolarSystem;
	private Contextmenu_Planet      contextMenu_Planet;
	
	private Window mainWindow;
	
	private SearchBar             searchBar;
	private AbstractInfoPanel     currentInfoPanel;
	private InfoPanel_Other       infoPanel_Other;
	private InfoPanel_SolarSystem infoPanel_SolarSystem;
	private InfoPanel_Planet      infoPanel_Planet;
	private GalaxyMapPanel galaxyMapPanel;
	
	public UniversePanel(SaveGameData data, Window mainWindow) {
		super(data);
		this.mainWindow = mainWindow;
		this.galaxyMapPanel = null;
		
		markerList = new int[0];
		selectedMarkerIndex = Float.NaN;
		
		selectedNode = null;
		TreeListener listener = new TreeListener();
		
		treeModel = new DefaultTreeModel(treeRoot = new UniverseNode(data.universe));
		tree = new JTree(treeModel);
		tree.addTreeSelectionListener(listener);
		tree.addMouseListener(listener);
		tree.setCellRenderer(new UniverseTreeCellRenderer(data));
		tree.setRowHeight(TreeIconHeight+1);
		
		treeScrollPane = new JScrollPane(tree);
		
		contextMenu_Other       = new Contextmenu_Other();
		contextMenu_SolarSystem = new Contextmenu_SolarSystem();
		contextMenu_Planet      = new Contextmenu_Planet();
		contextMenu_Region      = new Contextmenu_Region();
		
		infoPanel_Other       = new InfoPanel_Other();
		infoPanel_SolarSystem = new InfoPanel_SolarSystem();
		infoPanel_Planet      = new InfoPanel_Planet();
		
		searchBar = new SearchBar();
		
		JPanel treePanel = new JPanel(new BorderLayout(3,3));
		treePanel.add(treeScrollPane,BorderLayout.CENTER);
		treePanel.add(searchBar,BorderLayout.NORTH);
		
		add(treePanel,BorderLayout.CENTER);
		add(currentInfoPanel = infoPanel_Other,BorderLayout.EAST);
		
		JScrollBar verticalScrollBar = treeScrollPane.getVerticalScrollBar();
		verticalScrollBar.getModel().addChangeListener(e->{
			if (!verticalScrollBar.getValueIsAdjusting())
				updateSelectedMarkerIndex();
		});
		
		expandFullTree();
		scrollToCurrentPosition();
		
		updateMarkerList();
	}
	
	public void setGalaxyMapPanel(GalaxyMapPanel galaxyMapPanel) {
		this.galaxyMapPanel = galaxyMapPanel;
	}

	private static abstract class AbstractInfoPanel extends JPanel {
		private static final long serialVersionUID = 1055278730261206951L;
		
		private JTextArea textArea;
		protected boolean isSettingContent;
		
		AbstractInfoPanel() {
			super(new BorderLayout(3,3));
			setPreferredSize(new Dimension(650,500));
			isSettingContent = false;
			
			textArea = new JTextArea();
			textArea.setEditable(false);
			
			JScrollPane scrollPane = new JScrollPane(textArea);
			scrollPane.setBorder(BorderFactory.createEtchedBorder());
			
			add(scrollPane,BorderLayout.CENTER);
		}
		public void setContent(GenericTreeNode<?> node) {
			isSettingContent = true;
			setContent_intern(node);
			isSettingContent = false;
		}
		protected abstract void setContent_intern(GenericTreeNode<?> node);
		
		protected void setText(String str) { textArea.setText(str); }
		protected void clearText()         { setText(""); }
		protected void append  (               String format, Object...objects ) { textArea.append(String.format(        format, objects)); }
		protected void append  (Locale locale, String format, Object...objects ) { textArea.append(String.format(locale, format, objects)); }
		protected void appendln(                                               ) { append(               "\r\n"         ); }
		protected void appendln(               String format, Object...objects ) { append(        format+"\r\n", objects); }
		protected void appendln(Locale locale, String format, Object...objects ) { append(locale, format+"\r\n", objects); }

		protected void showObjectWithSource(ObjectWithSource obj) {
			appendln();
			appendln("Source: %s", obj.getLongSourceIDStr());
			
			if (!obj.discoveredItems_Avail.isEmpty() || !obj.discoveredItems_Store.isEmpty()) {
				appendln();
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
		}
	}
	
	private class InfoPanel_Other extends AbstractInfoPanel {
		private static final long serialVersionUID = 4133259332387200850L;

		@Override
		protected void setContent_intern(GenericTreeNode<?> selectedNode) {
			clearText();
			
			if (selectedNode==null)
				return;
			
			int n;
			UniverseAddress ua;
			double distance_center, distance_currPos;
			
			switch(selectedNode.type) {
			
			case Region:
				n = selectedNode.getDataChildrenCount();
				Region region = ((RegionNode)selectedNode).value;
				appendln("%d known solar system%s", n, n>1?"s":"");
				ua = region.getUniverseAddress();
				appendln("Universe Coordinates      : %s", ua.getCoordinates_Region());
				appendln("Reduced SignalBoster Code : %s", ua.getReducedSigBoostCode());
				
				showObjectWithSource(region);
				
				distance_center  = ua.getDistToCenter_inRegionUnits();
				distance_currPos = ua.getDistToOther_inRegionUnits(data.general.currentUniverseAddress);
				appendln();
				appendln(               "Distance to Galaxy Center :");
				appendln(Locale.ENGLISH,"    computed: %1.2f Regions = %1.1f ly", distance_center, distance_center*400);
				appendln();
				appendln(               "Distance to Current Position:");
				if (Double.isInfinite(distance_currPos))
					appendln(Locale.ENGLISH,"    computed: infinite");
				else
					appendln(Locale.ENGLISH,"    computed: %1.2f Regions = %1.1f ly", distance_currPos, distance_currPos*400);
				break;
				
			case Galaxy:
				n = selectedNode.getDataChildrenCount();
				appendln("%d known region%s\r\n", n, n>1?"s":"");
				showObjectWithSource(((GalaxyNode)selectedNode).value);
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
	
	private static abstract class InfoPanel_DiscoverableObject extends AbstractInfoPanel {
		private static final long serialVersionUID = -8235731718380188431L;
		
		private SimplifiedTable<ExtraInfoColumnID> extraInfoTable;
		private UniversePanel universePanel;
		private JPanel leftValuePanel;
		private JPanel rightValuePanel;
		private GridBagConstraints c;
		
		InfoPanel_DiscoverableObject(UniversePanel universePanel, boolean useValuePanel) {
			this.universePanel = universePanel;
			extraInfoTable = new SimplifiedTable<>("ExtraInfoTable",true,SaveViewer.DEBUG,true);
			JScrollPane extraInfoTableScrollPane = new JScrollPane(extraInfoTable);
			extraInfoTableScrollPane.setPreferredSize(new Dimension(500, 60));;
			
			if (useValuePanel) {
				c = new GridBagConstraints();
				leftValuePanel = new JPanel(new GridBagLayout());
				rightValuePanel = new JPanel(new GridBagLayout());
				
				JPanel southPanel = new JPanel(new GridBagLayout());
				southPanel.setBorder(BorderFactory.createTitledBorder("Values"));
				c.insets = new Insets(2,2,2,2);
				addComp(southPanel,  leftValuePanel, 0,0, 1,1, GridBagConstraints.BOTH);
				addComp(southPanel, rightValuePanel, 0,0, 1,1, GridBagConstraints.BOTH);
				addComp(southPanel, new JLabel()   , 1,0, GridBagConstraints.REMAINDER,1, GridBagConstraints.BOTH);
				addComp(southPanel, extraInfoTableScrollPane, 1, 1, GridBagConstraints.REMAINDER , 1, GridBagConstraints.BOTH);
				c.insets = new Insets(0,0,0,0);
				
				add(southPanel,BorderLayout.SOUTH);
			} else
				add(extraInfoTableScrollPane,BorderLayout.SOUTH);
		}
		
		protected void addCompToRight(Component comp, double weightx, double weighty, int gridwidth, int gridheight, int fill) {
			addComp(rightValuePanel, comp, weightx, weighty, gridwidth, gridheight, fill);
		}
		protected void addCompToLeft(Component comp, double weightx, double weighty, int gridwidth, int gridheight, int fill) {
			addComp(leftValuePanel, comp, weightx, weighty, gridwidth, gridheight, fill);
		}
		private void addComp(JPanel panel, Component comp, double weightx, double weighty, int gridwidth, int gridheight, int fill) {
			c.weightx=weightx;
			c.weighty=weighty;
			c.gridwidth=gridwidth;
			c.gridheight=gridheight;
			c.fill = fill;
			panel.add(comp,c);
		}

		protected void showDiscoverableObject(GenericTreeNode<?> node, DiscoverableObject obj) {
			appendln();
			
			//textArea.append(String.format("selected : %s\r\n\r\n", obj.isSelected));
			
			String oldNameLabel = "Old Original Name";
			if (obj.hasOriginalName   ()) { appendln("Original Name : %s", obj.getOriginalName()); oldNameLabel = "  -\"-   (old)"; }
			if (obj.hasOldOriginalName())   appendln(           "%s : %s", oldNameLabel, obj.getOldOriginalName());
			if (obj.hasUploadedName   ())   appendln("Uploaded Name : %s", obj.getUploadedName());
			if (obj.hasDiscoverer     ())   appendln("Discovered by : %s", obj.getDiscoverer());
			
			
			extraInfoTable.setModel(new ExtraInfoTableModel(node, obj instanceof Planet, obj.extraInfos));
	//			extraInfoTableModel.setData(obj.extraInfos);
			
			showObjectWithSource(obj);
		}

		private enum ExtraInfoColumnID implements SimplifiedColumnIDInterface {
			ShowInParent(""     , Boolean.class, 10,-1, 20, 20),
			Label       ("Label",  String.class, 20,-1,100,100),
			Info        ("Info" ,  String.class, 50,-1,390,390);
			
			private SimplifiedColumnConfig config;
		
			private ExtraInfoColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				config = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			
			@Override public SimplifiedColumnConfig getColumnConfig() { return config; }
		}

		private class ExtraInfoTableModel extends Tables.SimplifiedTableModel<ExtraInfoColumnID> {
		
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
	
	private class InfoPanel_SolarSystem extends InfoPanel_DiscoverableObject {
		private static final long serialVersionUID = 1050112094455682248L;
		
		private SolarSystemNode node;

		private JComboBox<Race> cmbbxRace;
		private JComboBox<StarClass> cmbbxStarClass;

		private LabeledLevelsBlock llbConflict;
		private LabeledLevelsBlock llbEconomy;

		private JCheckBox chkbxUnexplored;

		private JCheckBox chkbxAtlasInterface;
		private JCheckBox chkbxBlackHole;
		private JPanel blackHoleTargetPanel;
		private JCheckBox chkbxRememTerm;
		private JComboBox<Region> cmbbxBlackHoleTargetRegion;
		private JComboBox<SolarSystem> cmbbxBlackHoleTargetSolarSystem;

		InfoPanel_SolarSystem() {
			super(UniversePanel.this,true);
			this.node = null;
			
			cmbbxRace = new Gui.IconComboBox<Race>(Race.values()) {
				private static final long serialVersionUID = 5328964374227212373L;
				
				@Override public Race cast(Object obj) {
					if (!(obj instanceof StarClass)) return null;
					return (Race)obj;
				}
				@Override public Icon createIcon(Race value) {
					return SolarSystemIcons.RaceIcons.get(value);
				}
				@Override public String getLabel(Race value) {
					if (value==null) return "";
					return value.fullName;
				}
			};
			cmbbxStarClass = new Gui.IconComboBox<StarClass>(StarClass.values()) {
				private static final long serialVersionUID = 5328964374227212373L;
				
				@Override public StarClass cast(Object obj) {
					if (!(obj instanceof StarClass)) return null;
					return (StarClass)obj;
				}
				@Override public Icon createIcon(StarClass value) {
					return SolarSystemIcons.StarClassIcons.get(value);
				}
				@Override public String getLabel(StarClass value) {
					if (value==null) return "";
					return value.getLabel();
				}
			};
			
			llbConflict = new LabeledLevelsBlock("Conflict Level", SolarSystemIcons.ConflictLevelIcons, 1, 3) {
				@Override void setLevelInNode     (int    level     ) { node.value.conflictLevel      = level; }
				@Override void setLevelLabelInNode(String levelLabel) { node.value.conflictLevelLabel = levelLabel; }
				
				@Override int    getLevelFromNode     () { return node.value.conflictLevel     ; }
				@Override String getLevelLabelFromNode() { return node.value.conflictLevelLabel; }
				
				@Override int      getLevelByLabel(String levelLabel) { return GameInfos.getConflictLevel(levelLabel); }
				@Override String[] getLevelLabels(int level)          { return GameInfos.getConflictLevelLabels(level); }
				@Override void     updateLevelLabelsGameInfos()       { GameInfos.updateConflictLevelLabels(); }
			};
			llbEconomy = new LabeledLevelsBlock( "Economy Level", SolarSystemIcons.EconomyLevelIcons, 1, 3) {
				@Override void setLevelInNode     (int    level     ) { node.value.economyLevel      = level; }
				@Override void setLevelLabelInNode(String levelLabel) { node.value.economyLevelLabel = levelLabel; }
				
				@Override int    getLevelFromNode     () { return node.value.economyLevel     ; }
				@Override String getLevelLabelFromNode() { return node.value.economyLevelLabel; }
				
				@Override int      getLevelByLabel(String levelLabel) { return GameInfos.getEconomyLevel(levelLabel); }
				@Override String[] getLevelLabels(int level)          { return GameInfos.getEconomyLevelLabels(level); }
				@Override void     updateLevelLabelsGameInfos()       { GameInfos.updateEconomyLevelLabels(); }
			};
			
			cmbbxRace         .addActionListener(e->{ if (isSettingContent) return; node.value.race      = (Race     )cmbbxRace     .getSelectedItem(); updateTreeNode(node, false); });
			cmbbxStarClass    .addActionListener(e->{ if (isSettingContent) return; node.value.starClass = (StarClass)cmbbxStarClass.getSelectedItem(); updateTreeNode(node, false); });
			
			chkbxUnexplored = Gui.createCheckbox("is Unexplored", e->{
				if (isSettingContent) return;
				node.value.isUnexplored = chkbxUnexplored.isSelected();
				cmbbxRace               .setEnabled(!node.value.isUnexplored);
				llbConflict.setEnabled(!node.value.isUnexplored);
				llbEconomy .setEnabled(!node.value.isUnexplored);
				updateTreeNode(node, false);
			}, false);
			
			chkbxAtlasInterface = Gui.createCheckbox("has Atlas Interface", e->{
				if (isSettingContent) return;
				node.value.hasAtlasInterface = chkbxAtlasInterface.isSelected();
				updateTreeNode(node, false);
			}, false);
			
			chkbxBlackHole = Gui.createCheckbox("has Black Hole", e->{
				if (isSettingContent) return;
				node.value.hasBlackHole = chkbxBlackHole.isSelected();
				updateBlackHoleTargetPanel();
				updateTreeNode(node, false);
				galaxyMapPanel.updateBlackHoleConnections();
			}, false);
			
			chkbxRememTerm = Gui.createCheckbox("has Remembrance Terminal", e->{
				if (isSettingContent) return;
				node.value.withRemembranceTerminal = chkbxRememTerm.isSelected();
				updateTreeNode(node, false);
			}, false);
			
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
				if (isSettingContent) return;
				SolarSystem system = (SolarSystem)cmbbxBlackHoleTargetSolarSystem.getSelectedItem();
				node.value.blackHoleTarget = system==null?null:system.getUniverseAddress();
				updateTreeNode(node, false);
				galaxyMapPanel.updateBlackHoleConnections();
			});
			cmbbxBlackHoleTargetSolarSystem.setRenderer(new Tables.NonStringRenderer<SolarSystem>((Object obj)->{
				if (!(obj instanceof SolarSystem)) return "";
				SolarSystem system = (SolarSystem)obj;
				return system.toString(true, false, false, true);
			}));
			
			blackHoleTargetPanel = new JPanel(new GridLayout(0,1,3,3));
			blackHoleTargetPanel.setBorder(BorderFactory.createTitledBorder("Black Hole Target"));
			blackHoleTargetPanel.add(cmbbxBlackHoleTargetRegion     );
			blackHoleTargetPanel.add(cmbbxBlackHoleTargetSolarSystem);
			
			addCompToLeft (cmbbxRace           , 1,0, 1                           ,1, GridBagConstraints.BOTH);
			addCompToLeft (cmbbxStarClass      , 0,0, GridBagConstraints.REMAINDER,1, GridBagConstraints.BOTH);
			addCompToLeft (llbEconomy .cmbbxLevel      , 1,0, 1                           ,1, GridBagConstraints.BOTH);
			addCompToLeft (llbEconomy .cmbbxLevelLabels, 1,0, 1                           ,1, GridBagConstraints.BOTH);
			addCompToLeft (llbEconomy .btnAddLevelLabel, 0,0, GridBagConstraints.REMAINDER,1, GridBagConstraints.BOTH);
			addCompToLeft (llbConflict.cmbbxLevel      , 1,0, 1                           ,1, GridBagConstraints.BOTH);
			addCompToLeft (llbConflict.cmbbxLevelLabels, 1,0, 1                           ,1, GridBagConstraints.BOTH);
			addCompToLeft (llbConflict.btnAddLevelLabel, 0,0, GridBagConstraints.REMAINDER,1, GridBagConstraints.BOTH);
			addCompToLeft (chkbxUnexplored     , 1,0, GridBagConstraints.REMAINDER,1, GridBagConstraints.BOTH);
			
			addCompToRight(chkbxAtlasInterface , 1,0, GridBagConstraints.REMAINDER,1, GridBagConstraints.BOTH);
			addCompToRight(chkbxBlackHole      , 1,0, GridBagConstraints.REMAINDER,1, GridBagConstraints.BOTH);
			addCompToRight(blackHoleTargetPanel, 1,0, GridBagConstraints.REMAINDER,1, GridBagConstraints.BOTH);
			addCompToRight(chkbxRememTerm      , 1,0, GridBagConstraints.REMAINDER,1, GridBagConstraints.BOTH);
			
			addCompToLeft (new JLabel(), 1,1, GridBagConstraints.REMAINDER,1, GridBagConstraints.BOTH);
			addCompToRight(new JLabel(), 1,1, GridBagConstraints.REMAINDER,1, GridBagConstraints.BOTH);
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
		protected void setContent_intern(GenericTreeNode<?> node) {
			this.node = (SolarSystemNode)node;
			
			SolarSystem system = this.node.value;
			
			cmbbxRace          .setSelectedItem(system.race         );
			cmbbxStarClass     .setSelectedItem(system.starClass    );
			llbConflict.setLevel(system.conflictLevel);
			llbConflict.updateLevelLabels();
			llbEconomy .setLevel(system.economyLevel);
			llbEconomy .updateLevelLabels();
			
			chkbxUnexplored    .setSelected(system.isUnexplored     );
			chkbxAtlasInterface.setSelected(system.hasAtlasInterface);
			chkbxBlackHole     .setSelected(system.hasBlackHole     );
			chkbxRememTerm     .setSelected(system.withRemembranceTerminal);
			updateBlackHoleTargetPanel();
			
			cmbbxRace  .setEnabled(!system.isUnexplored);
			llbConflict.setEnabled(!system.isUnexplored);
			llbEconomy .setEnabled(!system.isUnexplored);
			
			showInfos();
		}

		private void showInfos() {
			double distance_reg;
			SolarSystem system = this.node.value;
			
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
				if (Double.isInfinite(distance_reg))
					appendln(Locale.ENGLISH,"    computed: infinite");
				else
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
			
			showDiscoverableObject(node,system);
			
			if (!system.additionalInfos.isEmpty()) {
				appendln();
				appendln("Additional Infos:");
				if (system.additionalInfos.hasFreighter)
					appendln("    Freighter in System");
				if (system.additionalInfos.hasAnomaly)
					appendln("    Anomaly in System");
				if (!system.additionalInfos.teleportEndpoints.isEmpty()) {
					appendln("    Teleport Endpoints:");
					for (TeleportEndpoints tel:system.additionalInfos.teleportEndpoints)
						appendln("        \"%s\"", tel.name);
				}
			}
		}
		
		private abstract class LabeledLevelsBlock {
			private Gui.IconComboBox<Integer> cmbbxLevel;
			private JComboBox<String> cmbbxLevelLabels;
			private JButton btnAddLevelLabel;
			private int minLevel;
			private int maxLevel;
			private int undefinedValue;
			
			abstract void setLevelInNode(int level);
			abstract void setLevelLabelInNode(String levelLabel);
			
			private boolean isInRange(int level) {
				return minLevel<=level && level<=maxLevel;
			}
			
			LabeledLevelsBlock(String baseLabel, Icon[] icons, int minLevel, int maxLevel) {
				this.minLevel = minLevel;
				this.maxLevel = maxLevel;
				this.undefinedValue = Math.min(-1, minLevel-1);
				if (maxLevel<minLevel) throw new IllegalArgumentException();
				
				Integer[] values = new Integer[maxLevel-minLevel+1];
				for (int i=0; i<=maxLevel-minLevel; i++)
					values[i] = minLevel+i;
				
				cmbbxLevel = new Gui.IconComboBox<Integer>(values) {
					private static final long serialVersionUID = 5328964374227212373L;
					
					@Override public Integer cast(Object obj) {
						if (!(obj instanceof Integer)) return null;
						return (Integer)obj;
					}
					@Override public Icon createIcon(Integer value) {
						if (value==null || !isInRange(value)) return null;
						return icons[value-minLevel];
					}
					@Override public String getLabel(Integer value) {
						if (value==null || !isInRange(value)) return "";
						return baseLabel+" "+value;
					}
				};
				
				cmbbxLevel.addActionListener(e->{
					if (isSettingContent) return;
					
					Integer val = cmbbxLevel.getSelected();
					setLevelInNode( val==null ? undefinedValue : val );
					updateTreeNode(node, false);
					
					updateLevelLabelsGameInfos();
					updateLevelLabels();
				});
				
				cmbbxLevelLabels = new JComboBox<String>();
				cmbbxLevelLabels.addActionListener(e->{
					if (isSettingContent) return;
					
					int index = cmbbxLevelLabels.getSelectedIndex();
					String levelLabel = index<0?null:cmbbxLevelLabels.getItemAt(index);
					setLevelLabelInNode(levelLabel);
					updateTreeNode(node, false);
					
					if (!isInRange(getLevelFromNode())) {
						int level = getLevelByLabel(levelLabel);
						SwingUtilities.invokeLater(()->setLevel(level));
					}
				});
				btnAddLevelLabel = Gui.createButton("Add",e->{
					String levelLabel = JOptionPane.showInputDialog(InfoPanel_SolarSystem.this, "Define new label:", "Add Label", JOptionPane.PLAIN_MESSAGE);
					if (levelLabel!=null) {
						setLevelLabelInNode(levelLabel);
						updateTreeNode(node, false);
						
						updateLevelLabelsGameInfos();
						updateLevelLabels();
					}
				});
			}

			public void setLevel(int level) {
				cmbbxLevel.setSelectedItem(isInRange(level) ? level : null);
			}

			public void setEnabled(boolean enabled) {
				cmbbxLevel      .setEnabled(enabled);
				cmbbxLevelLabels.setEnabled(enabled);
				btnAddLevelLabel.setEnabled(enabled);
			}
			
			abstract int    getLevelFromNode();
			abstract String getLevelLabelFromNode();
			
			abstract int      getLevelByLabel(String levelLabel);
			abstract String[] getLevelLabels(int level);
			abstract void     updateLevelLabelsGameInfos();
			
			private void updateLevelLabels() {
				int level = getLevelFromNode();
				String label = getLevelLabelFromNode();
				String[] labels = getLevelLabels(level);
				cmbbxLevelLabels.setModel(new DefaultComboBoxModel<>(labels));
				cmbbxLevelLabels.setSelectedItem(label);
				btnAddLevelLabel.setEnabled(level>=1);
			}
			
		}
	}
	
	private class InfoPanel_Planet extends InfoPanel_DiscoverableObject {
		private static final long serialVersionUID = -5303591976120968332L;
		
		private JLabel portalGlyphs;
		private PlanetNode node;

		private JCheckBox chkbxAggrSent;
		private JCheckBox chkbxWater;
		private JCheckBox chkbxGrav;
		private JCheckBox chkbxRememTerm;

		private Gui.IconComboBox<Biome> cmbbxBiome;
		private JComboBox<BuriedTreasure> cmbbxBuriedTreasure;
		private JCheckBox chkbxExtreme;
		
		private JTextField txtfldResources;
		private JButton btnSetResources;
		
		private ResourceSelectDialog resourceSelectDialog;
		
		InfoPanel_Planet() {
			super(UniversePanel.this, true);
			this.node = null;
			
			portalGlyphs = new JLabel();
			portalGlyphs.setPreferredSize(new Dimension(50*12+10, 45*1+10));
			portalGlyphs.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(3, 3, 3, 3)));
			
			add(portalGlyphs,BorderLayout.NORTH);
			
			cmbbxBiome = new Gui.IconComboBox<Biome>(Biome.values()) {
				private static final long serialVersionUID = 5328964374227212373L;
				
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
			};
			cmbbxBiome.addActionListener(e->{ if (isSettingContent) return; node.value.biome = cmbbxBiome.getSelected(); updateTreeNode(node, false);  });
			
			cmbbxBuriedTreasure = new JComboBox<BuriedTreasure>( SaveViewer.addNull(BuriedTreasure.values()));
			cmbbxBuriedTreasure.setRenderer(new Tables.NonStringRenderer<BuriedTreasure>(value->value==null?"":((BuriedTreasure)value).name_EN));
			cmbbxBuriedTreasure.addActionListener(e->{ if (isSettingContent) return; node.value.buriedTreasure = (BuriedTreasure)cmbbxBuriedTreasure.getSelectedItem(); updateTreeNode(node, false);  });
			
			chkbxExtreme   = Gui.createCheckbox("is Extreme"               , e->{ if (isSettingContent) return; node.value.hasExtremeBiome         = chkbxExtreme  .isSelected(); updateTreeNode(node, true ); }, false);
			chkbxAggrSent  = Gui.createCheckbox("Aggressive Sentinels"     , e->{ if (isSettingContent) return; node.value.areSentinelsAggressive  = chkbxAggrSent .isSelected(); updateTreeNode(node, false); }, false);
			chkbxWater     = Gui.createCheckbox("with Water"               , e->{ if (isSettingContent) return; node.value.withWater               = chkbxWater    .isSelected(); updateTreeNode(node, true ); }, false);
			chkbxGrav      = Gui.createCheckbox("with Gravitino Balls"     , e->{ if (isSettingContent) return; node.value.withGravitinoBalls      = chkbxGrav     .isSelected(); updateTreeNode(node, false); }, false);
			chkbxRememTerm = Gui.createCheckbox("with Remembrance Terminal", e->{ if (isSettingContent) return; node.value.withRemembranceTerminal = chkbxRememTerm.isSelected(); updateTreeNode(node, false); }, false);
			
			txtfldResources = new JTextField(20);
			txtfldResources.setEditable(false);
			btnSetResources = Gui.createButton("Change", e->{
				if (resourceSelectDialog==null) resourceSelectDialog = new ResourceSelectDialog(mainWindow, "Select Planetary Resources");
				EnumSet<Resources> result = resourceSelectDialog.showDialog(node.value.resources);
				if (result!=null) {
					node.value.resources.clear();
					node.value.resources.addAll(result);
					updateTxtfldResources();
					updateTreeNode(node, false);
				}
			});
			
			addCompToLeft (cmbbxBiome         , 1, 0, GridBagConstraints.REMAINDER, 1, GridBagConstraints.BOTH);
			addCompToLeft (chkbxExtreme       , 1, 0, GridBagConstraints.REMAINDER, 1, GridBagConstraints.BOTH);
			addCompToLeft (chkbxAggrSent      , 1, 0, GridBagConstraints.REMAINDER, 1, GridBagConstraints.BOTH);
			addCompToLeft (chkbxWater         , 1, 0, GridBagConstraints.REMAINDER, 1, GridBagConstraints.BOTH);
			addCompToLeft (new JLabel()       , 1, 1, GridBagConstraints.REMAINDER, 1, GridBagConstraints.BOTH);
			addCompToRight(txtfldResources    , 1, 0, 1, 1, GridBagConstraints.BOTH);
			addCompToRight(btnSetResources    , 0, 0, GridBagConstraints.REMAINDER, 1, GridBagConstraints.BOTH);
			addCompToRight(cmbbxBuriedTreasure, 1, 0, GridBagConstraints.REMAINDER, 1, GridBagConstraints.BOTH);
			addCompToRight(chkbxGrav          , 1, 0, GridBagConstraints.REMAINDER, 1, GridBagConstraints.BOTH);
			addCompToRight(chkbxRememTerm     , 1, 0, GridBagConstraints.REMAINDER, 1, GridBagConstraints.BOTH);
			addCompToRight(new JLabel()       , 1, 1, GridBagConstraints.REMAINDER, 1, GridBagConstraints.BOTH);
		}
		
		@Override
		protected void setContent_intern(GenericTreeNode<?> node) {
			this.node = (PlanetNode)node;
			
			Planet planet = this.node.value;
			UniverseAddress ua = planet.getUniverseAddress();
			long portalGlyphCode = ua.getPortalGlyphCode();
			
			cmbbxBiome    .setSelectedItem(planet.biome);
			chkbxExtreme  .setSelected(planet.hasExtremeBiome);
			chkbxAggrSent .setSelected(planet.areSentinelsAggressive);
			chkbxWater    .setSelected(planet.withWater);
			chkbxGrav     .setSelected(planet.withGravitinoBalls);
			chkbxRememTerm.setSelected(planet.withRemembranceTerminal);
			cmbbxBuriedTreasure.setSelectedItem(planet.buriedTreasure);
			portalGlyphs.setIcon(createPortalGlyphs(portalGlyphCode));
			
			updateTxtfldResources();
			
			clearText();
			appendln("Universe Coordinates       : %s"     , ua.getCoordinates());
			appendln("Universe Address           : 0x%014X", ua.getAddress());
			appendln("Portal Glyph Code          : %012X"  , portalGlyphCode);
			appendln("Extended SignalBoster Code : %s"     , ua.getExtendedSigBoostCode());
			
			showDiscoverableObject(node,planet);
			
			if (!planet.additionalInfos.isEmpty()) {
				appendln();
				appendln("Additional Infos:");
				if (!planet.additionalInfos.teleportEndpoints.isEmpty()) {
					appendln("    Teleport Endpoints:");
					for (TeleportEndpoints tel:planet.additionalInfos.teleportEndpoints)
						appendln("        %s", tel.getNameAndGPS());
				}
				if (!planet.additionalInfos.teleportEndpointsInOtherPlayerBases.isEmpty()) {
					appendln("    Teleport Endpoints in Base of another Player:");
					for (TeleportEndpoints tel:planet.additionalInfos.teleportEndpointsInOtherPlayerBases)
						appendln("        %s", tel.getNameAndGPS());
				}
				if (planet.additionalInfos.hasExocraftSummoningStation)
					appendln("    Exocraft Summoning Station on Planet");
				for (PersistentPlayerBase base:planet.additionalInfos.playerBases)
					appendln("    Base on Planet: \"%s\"", base.name);
				for (PersistentPlayerBase base:planet.additionalInfos.otherPlayerBases) {
					String ownerName = base.owner!=null ? base.owner.getOwnerName() : null;
					appendln("    Base on Planet: \"%s\" of %s", base.name, ownerName==null ? "another player" : "\""+ownerName+"\"");
				}
			}
		}

		private void updateTxtfldResources() {
			txtfldResources.setText(String.join(", ", Universe.Planet.Resources.getStringIterable(node.value.resources, Resources::getShortLabel)));
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
			add(createMenuItem("Scroll to Current Position",UniverseTreeActionCommand.ScrollToCurrentPosition));
			add(createMenuItem("Remove all Highlights",UniverseTreeActionCommand.RemoveHighlights));
			add(createMenuItem("Find Solar System or Planet via ExtraInfo",UniverseTreeActionCommand.FindExtraInfo));
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
		private ListMenu<Integer> miSetEconomyLevel;
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
							menuItem.setText(value.getLabel());
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
			
			miSetEconomyLevel = new Gui.ListMenu<Integer>("Set Economy Level",new Integer[]{1,2,3},null,
				new Gui.ListMenuItems.ExternFunction<Integer>() {
					@Override public void setResult(Integer value) {
						if (clickedNode instanceof SolarSystemNode) {
							((SolarSystemNode)clickedNode).value.economyLevel = value==null?-1:value;
							updateTreeNodeAndInfoPanel(clickedNode, false);
						}
					}
					@Override public void configureMenuItem(JMenuItem menuItem, Integer value) {
						if (value==null) {
							menuItem.setIcon(null);
							menuItem.setText("<none>");
						} else {
							menuItem.setIcon(SolarSystemIcons.EconomyLevelIcons[value-1]);
							menuItem.setText("Economy Level "+value);
						}
					}
				}
			);
			miSetEconomyLevel.setShowSelectedValue(true);
			add(miSetEconomyLevel);
			
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
			miSetEconomyLevel .setValue(1<=system. economyLevel&&system. economyLevel<=3?system. economyLevel:null);
			miUnexplored.setSelected(system.isUnexplored);
			miSetRace.setEnabled(!system.isUnexplored);
			miSetConflictLevel.setEnabled(!system.isUnexplored);
			miSetEconomyLevel .setEnabled(!system.isUnexplored);
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
			add(createMenuItem("Open Resource HotSpots",UniverseTreeActionCommand.OpenResourceHotSpots));
			
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
			
		case OpenResourceHotSpots:
			if (clickedNode instanceof PlanetNode) {
				Planet planet=((PlanetNode)clickedNode).value;
				ResourceHotSpots.start(planet);
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
			
		case FindExtraInfo: {
			FindExtraInfoDialog dialog = new FindExtraInfoDialog(mainWindow,data.universe);
			dialog.showDialog();
			HashSet<GenericTreeNode<?>> changedObjects = dialog.getChangedObjects();
			showChangedObjects(changedObjects);
			for (GenericTreeNode<?> node:changedObjects) treeModel.nodeChanged(node); 
			updateMarkerList();
			//tree.repaint();
			} break;
			
		case RemoveHighlights: {
			setMarker((UniverseNode)data.universe.guiComp,false);
			for (Galaxy g:data.universe.galaxies) {
				setMarker((GalaxyNode)g.guiComp,false);
				for (Region r:g.regions) {
					setMarker((RegionNode)r.guiComp,false);
					for (SolarSystem s:r.solarSystems) {
						setMarker((SolarSystemNode)s.guiComp,false);
						for (Planet p:s.planets) {
							setMarker((PlanetNode)p.guiComp,false);
						}
					}
				}
			}
			updateMarkerList();
			} break;
			
		case ShowFullDiscoveredUniverseData:
			GameInfos.readUniverseObjectDataFromDataPool(data.universe, true);
			data.universe.sort();
			treeRoot = new UniverseNode(data.universe);
			treeModel.setRoot(treeRoot);
			expandFullTree();
			if (galaxyMapPanel!=null) galaxyMapPanel.updateUniverseData();
			break;
			
		case ScrollToCurrentPosition:
			scrollToCurrentPosition();
			break;
		}
		clickedNode = null;
		clickedTreePath = null;
	}

	private void scrollToCurrentPosition() {
		scrollToPosition(getCurrentPosNode());
	}

	private void scrollToPosition(int row) {
		if (row<0) return;
		JScrollBar scrollBar = treeScrollPane.getVerticalScrollBar();
		int bar = scrollBar.getVisibleAmount();
		int max = scrollBar.getMaximum();
		int min = scrollBar.getMinimum();
		int value = (row*(max-min))/tree.getRowCount() + min - bar/2;
		value = Math.max(value, min);
		value = Math.min(value, max-bar);
		scrollBar.setValue(value);
	}

	private int getScrollBarPosition_Row() {
		JScrollBar scrollBar = treeScrollPane.getVerticalScrollBar();
		int bar = scrollBar.getVisibleAmount();
		int max = scrollBar.getMaximum();
		int min = scrollBar.getMinimum();
		int value = scrollBar.getValue();
		return ( (value + bar/2 - min) * tree.getRowCount() ) / (max-min);
	}

	private int getCurrentPosNode() {
		for (int i=0; i<tree.getRowCount(); ++i) {
			TreePath path = tree.getPathForRow(i);
			if (path != null) {
				Object comp = path.getLastPathComponent();
				if (comp instanceof GenericTreeNode<?>) {
					GenericTreeNode<?> node = (GenericTreeNode<?>)comp;
					if (node.value instanceof Universe.ObjectWithSource)
						if (((Universe.ObjectWithSource)node.value).isCurrPos)
							return i;
				}
			}
		}
		return -1;
	}

	public void highlightRegions(int galaxyIndex, int voxelX, int voxelZ) {
		Galaxy g = data.universe.findGalaxy(galaxyIndex);
		if (g==null) return;
		
		for (Region r:g.regions) {
			if (r.voxelX==voxelX && r.voxelZ==voxelZ) {
				RegionNode rNode = (RegionNode)r.guiComp;
				setMarker(rNode,true);
			}
		}
		updateMarkerList();
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

	private void showChangedObjects(HashSet<GenericTreeNode<?>> changedObjects) {
		Gui.log_ln("Changed Objects:");
		for (GenericTreeNode<?> obj:changedObjects) {
			Gui.log_ln("   "+obj.toString());
		}
	}

	private void setMarker(GenericTreeNode<?> node, boolean b) {
		if (node.isHighlighted != b) {
			node.isHighlighted = b;
			treeModel.nodeChanged(node);
		}
	}

	private void updateMarkerList() {
		markerList = getHighlightedNodes();
		updateSelectedMarkerIndex();
	}

	private void updateSelectedMarkerIndex() {
		selectedMarkerIndex = Float.NaN;
		int centerRow = getScrollBarPosition_Row();
		for (int i=-1; i<markerList.length; i++) {
			int row1 = i>=0 ? markerList[i] : -1;
			int row2 = i+1<markerList.length ? markerList[i+1] : tree.getRowCount();
			
			if (centerRow==row1) {
				selectedMarkerIndex = i;
				break;
			}
			if (centerRow==row2) {
				selectedMarkerIndex = i+1;
				break;
			}
			if (row1<centerRow && centerRow<row2) {
				selectedMarkerIndex = i + (centerRow-row1) / (float)(row2-row1);
				break;
			}
		}
		searchBar.prevMarker.setEnabled(selectedMarkerIndex>0);
		searchBar.nextMarker.setEnabled(selectedMarkerIndex<markerList.length-1);
	}

	private void scrollToMarker(int inc) {
		if (Float.isNaN(selectedMarkerIndex)) return;
		
		if (Math.floor(selectedMarkerIndex) != selectedMarkerIndex) {
			if (inc<0) selectedMarkerIndex = (float) Math.floor(selectedMarkerIndex);
			if (inc>0) selectedMarkerIndex = (float) Math.ceil (selectedMarkerIndex);
		} else selectedMarkerIndex += inc;
		selectedMarkerIndex = Math.max(selectedMarkerIndex, 0 );
		selectedMarkerIndex = Math.min(selectedMarkerIndex, markerList.length-1 );
		
		scrollToPosition( markerList[ (int) selectedMarkerIndex ] );
	}

	private int[] getHighlightedNodes() {
		Vector<Integer> rows = new Vector<>(); 
		for (int i=0; i<tree.getRowCount(); ++i) {
			TreePath path = tree.getPathForRow(i);
			if (path != null) {
				Object comp = path.getLastPathComponent();
				if (comp instanceof GenericTreeNode<?> && ((GenericTreeNode<?>)comp).isHighlighted())
					rows.add(i);
			}
		}
		return rows.stream().mapToInt(i->i).toArray();
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

	private static class ResourceSelectDialog extends StandardDialog {
		private static final long serialVersionUID = 5776076174454193077L;
		
		private EnumSet<Resources> resources = null;
		private boolean ignoreChanges = true;

		public ResourceSelectDialog(Window parent, String title) {
			super(parent, title, ModalityType.APPLICATION_MODAL, true);
			
			JButton okButton     = Gui.createButton("Ok"    , e->{ ignoreChanges=false; closeDialog(); });
			JButton cancelButton = Gui.createButton("Cancel", e->{ closeDialog(); });
			okButton    .setPreferredSize(new Dimension(75,25));
			cancelButton.setPreferredSize(new Dimension(75,25));
			
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			JPanel buttonPanel = new JPanel(new GridBagLayout());
			c.weightx = 1;
			buttonPanel.add(new JLabel(),c);
			c.weightx = 0;
			buttonPanel.add(okButton,c);
			buttonPanel.add(cancelButton,c);
			
			JPanel contentPane = new JPanel(new BorderLayout(3,3));
			contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
			contentPane.add(new ResourceGrid(), BorderLayout.CENTER);
			contentPane.add(buttonPanel, BorderLayout.SOUTH);
			
			createGUI(contentPane);
		}

		public EnumSet<Resources> showDialog(EnumSet<Resources> resources) {
			this.resources = resources.clone();
			showDialog();
			if (ignoreChanges) return null;
			return this.resources;
		}
		
		private class ResourceGrid extends Canvas {
			private static final long serialVersionUID = 8683936443777991288L;
			
			private static final int SLOT_RASTER_X = InventoriesPanel.InventoryPanel.InventoryDisplay.SLOT_RASTER_X;
			private static final int SLOT_RASTER_Y = InventoriesPanel.InventoryPanel.InventoryDisplay.SLOT_RASTER_Y;
			
			private GeneralizedID[][] resIdArrays;
			private GeneralizedID[][] resGrid;
			
			private int nColumn;
			private Point hovered;
			
			ResourceGrid() {
				nColumn = 6;
				hovered = null;
				
				Resources[][] resArrays = new Resources[][] {
					new Resources[] { Resources.Cu_, Resources.Cd_, Resources.Em_, Resources.In_, },
					new Resources[] { Resources.Cu, Resources.Cd, Resources.Em, Resources.In, },
					new Resources[] { Resources.Pf, Resources.Py, Resources.P, Resources.U, Resources.CO2, Resources.NH3 },
					null
				};
				resArrays[3] = getRemaining(Resources.values(), resArrays);
				
				resIdArrays = createFrom(resArrays);
				//for (Resources[] arr:allResources2)
				//	SaveViewer.log_ln("%s", Arrays.toString(arr));
				
				int nRow = computeRowCount(resIdArrays,nColumn);
				resGrid = createResGrid(resIdArrays,nRow,nColumn);
				
				setPreferredSize(new Dimension( nColumn*SLOT_RASTER_X+10, nRow*SLOT_RASTER_Y+10 ));
				
				MouseAdapter mouse = new MouseAdapter() {
					private Point clickStart;
					
					@Override public void mouseEntered(MouseEvent e) { hovered = getIndexes(e); repaint(); }
					@Override public void mouseMoved  (MouseEvent e) { hovered = getIndexes(e); repaint(); }
					@Override public void mouseExited (MouseEvent e) { hovered = null; repaint(); }
					
					
					@Override public void mousePressed (MouseEvent e) { clickStart = getIndexes(e); }
					@Override public void mouseReleased(MouseEvent e) { Point clickEnd = getIndexes(e);
						if (clickEnd==null || !clickEnd.equals(clickStart)) {
							clickStart = null;
							return;
						}
						GeneralizedID id = resGrid[clickEnd.y][clickEnd.x];
						
						Resources resource = getResource(id);
						if (resources.contains(resource)) resources.remove(resource);
						else resources.add(resource);
						
						repaint();
					}
				};
				addMouseListener(mouse);
				addMouseMotionListener(mouse);
			}
			
			private int computeRowCount(GeneralizedID[][] resArrays, int nColumn) {
				int nRows = 0;
				for (GeneralizedID[] arr:resArrays)
					nRows += (int)Math.ceil(arr.length/(float)nColumn);
				return nRows;
			}

			private GeneralizedID[][] createResGrid(GeneralizedID[][] resArrays, int nRow, int nColumn) {
				GeneralizedID[][] resGrid = new GeneralizedID[nRow][nColumn];
				int row = 0;
				for (GeneralizedID[] arr:resArrays) {
					int nRow1 = (int)Math.ceil(arr.length/(float)nColumn);
					for (int r=0; r<nRow1; r++)
						for (int c=0; c<nColumn; c++) {
							int i = r*nColumn+c;
							resGrid[row+r][c] = i>=arr.length?null:arr[i];
						}
					row += nRow1;
				}
				return resGrid;
			}

			private Resources[] getRemaining(Resources[] allRes, Resources[][] resArrays) {
				Vector<Resources> remaining = new Vector<>();
				for (Resources res1:allRes) {
					boolean found = false;
					for (Resources[] arr:resArrays) {
						if (arr!=null)
							for (Resources res2:arr)
								if (res1==res2) {
									found = true;
									break;
								}
						if (found)
							break;
					}
					if (!found)
						remaining.add(res1);
				}
				return remaining.toArray(new Resources[remaining.size()]);
			}
			private GeneralizedID[][] createFrom(Resources[][] resArrays) {
				GeneralizedID[][] idArrays = new GeneralizedID[resArrays.length][];
				for (int i=0; i<resArrays.length; i++) {
					Resources[] arr = resArrays[i];
					idArrays[i] = new GeneralizedID[arr.length];
					for (int j=0; j<arr.length; j++)
						idArrays[i][j] = arr[j].getGeneralizedID();
				}
				return idArrays;
			}

			private Resources getResource(GeneralizedID id) {
				for (Resources res:Resources.values())
					if (res.ID.equals(id.id))
						return res;
				return null;
			}

			private Point getIndexes(MouseEvent e) {
				int gridX = e.getX()/SLOT_RASTER_X;
				int gridY = e.getY()/SLOT_RASTER_Y;
				if (gridY<0 || gridX<0 || gridX>=nColumn || gridY>=resGrid.length) return null;
				if (resGrid[gridY][gridX]==null) return null;
				return new Point(gridX,gridY);
			}
			
			@Override protected void sizeChanged(int width, int height) {
				int nColumnNew = width / SLOT_RASTER_X;
				int nRow = computeRowCount(resIdArrays,nColumnNew);
				if (nRow!=resGrid.length || nColumnNew!=nColumn) {
					nColumn = nColumnNew;
					resGrid = createResGrid(resIdArrays,nRow,nColumn);
				}
			}

			@Override protected void paintCanvas(Graphics g, int x, int y, int width, int height) {
				if (!(g instanceof Graphics2D)) return;
				Graphics2D g2 = (Graphics2D)g;
				//InventoriesPanel.InventoryPanel.InventoryDisplay.drawSlotGrid(g2, x, y, nColumn, resourceIDs, i->resources.contains(allResources[i]), hovered);
				InventoriesPanel.InventoryPanel.InventoryDisplay.drawSlotGrid(g2, x, y, resGrid, id->resources.contains(getResource(id)), hovered);
			}
			
		}
	
	}

	private class SearchBar extends JPanel {
		private static final long serialVersionUID = -322241964276963216L;

		private boolean disableUpdates;
		private PlanetBar planetSearch;
		private SolarSystemBar solarSystemSearch;
		private JButton prevMarker;
		private JButton nextMarker;
		
		SearchBar() {
			super(new GridBagLayout());
			setBorder(BorderFactory.createTitledBorder("Search"));
			disableUpdates = false;
			
			planetSearch = new PlanetBar();
			solarSystemSearch = new SolarSystemBar();
			NameSearch nameSearch = new NameSearch();
			
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.weighty = 0;
			c.weightx = 0;
			
			add( Gui.createButton("Clear Markers"     , e->clearMarkers()), c );
			add( Gui.createButton("Find Planet"       , e->planetSearch     .showPopup(treeScrollPane, 3,3)), c );
			add( Gui.createButton("Find Solar System" , e->solarSystemSearch.showPopup(treeScrollPane, 3,3)), c );
			add( Gui.createButton("Find via Name"     , e->nameSearch       .showPopup(treeScrollPane, 3,3)), c );
			add( Gui.createButton("Find via ExtraInfo", UniversePanel.this, UniverseTreeActionCommand.FindExtraInfo), c );
			add( prevMarker = Gui.createButton("<<", e->scrollToMarker(-1)), c );
			add( nextMarker = Gui.createButton(">>", e->scrollToMarker(+1)), c );
			
			c.weightx = 1;
			add( new JLabel(), c);
		}

		private void clearMarkers() {
			disableUpdates = true;
			planetSearch.clearMarkers();
			solarSystemSearch.clearMarkers();
			disableUpdates = false;
			updateMarkers(str->{});
		}

		private void updateMarkers(Consumer<String> writeResultOuput) {
			if (disableUpdates) return;
			boolean isMarked;
			int markedPlanets = 0;
			int markedSolarSystems = 0;
			setMarker((UniverseNode)data.universe.guiComp,false);
			for (Galaxy g:data.universe.galaxies) {
				setMarker( (GenericTreeNode<?>)g.guiComp, false );
				for (Region r:g.regions) {
					setMarker( (GenericTreeNode<?>)r.guiComp, false );
					for (SolarSystem s:r.solarSystems) {
						isMarked = solarSystemSearch.shouldBeMarked(s);
						setMarker( (GenericTreeNode<?>)s.guiComp, isMarked );
						if (isMarked) ++markedSolarSystems;
						for (Planet p:s.planets) {
							isMarked = planetSearch.shouldBeMarked(p);
							setMarker( (GenericTreeNode<?>)p.guiComp, isMarked );
							if (isMarked) ++markedPlanets;
						}
					}
				}
			}
			updateMarkerList();
			String resultOuput = generateResultOuput(markedPlanets, markedSolarSystems, -1);
			//String resultOuput = String.format("%d Planets, %d Solar Systems", markedPlanets, markedSolarSystems);
			writeResultOuput.accept(resultOuput);
		}

		private String generateResultOuput(int planets, int solarSystems, int regions) {
			String str = "";
			str = addResultOuput(str, planets     , "Planet"      );
			str = addResultOuput(str, solarSystems, "Solar System");
			str = addResultOuput(str, regions     , "Region"      );
			return str;
			//return String.format("%d Planets, %d Solar Systems, %d Regions", planets, solarSystems, regions);
		}

		private String addResultOuput(String str, int n, String label) {
			if (n>=0) {
				String str1;
				if (n==1) str1 = "1 "+label;
				else str1 = String.format("%d %ss", n, label);
				str += (str.isEmpty()?"":", ")+str1;
			}
			return str;
		}
		
		private abstract class AbstractSearchBar extends PopupDialog {
			private static final long serialVersionUID = 3739974784050333833L;
			private JTextField statusOutput;
		
			protected AbstractSearchBar() {
				super(mainWindow);
				JPanel content = new JPanel(new GridBagLayout());
				content.setBorder(
					BorderFactory.createCompoundBorder(
						BorderFactory.createLineBorder(Color.GRAY),
						BorderFactory.createEmptyBorder(2,5,2,5)
					)
				);
				GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.BOTH;
				//c.insets = new Insets(2,5,2,5);
				
				c.weightx = 1;
				c.weighty = 1;
				c.gridwidth = GridBagConstraints.REMAINDER;
				content.add(createContentPanel(),c);
				
				statusOutput = new JTextField(30);
				statusOutput.setEditable(false);
				
				c.weighty = 0;
				c.weightx = 1;
				c.gridwidth = 1;
				content.add(statusOutput,c);
				c.weightx = 0;
				c.gridwidth = GridBagConstraints.REMAINDER;
				content.add( Gui.createButton("Close", e->{ super.hidePopup(); }),c);
				
				super.setGUI(content);
			}
			
			protected void setStatus(String str) {
				statusOutput.setText(str);
			}
		
			@Override
			public void showPopup(Component parent, int x, int y) {
				setStatus("");
				super.showPopup(parent, x, y);
			}

			@Override protected void setGUI(JPanel content) { throw new UnsupportedOperationException(); }
			//@Override protected void hidePopup() { throw new UnsupportedOperationException(); }
		
			protected abstract Component createContentPanel();
		}

		private class NameSearch extends AbstractSearchBar {
			private static final long serialVersionUID = 2446633626490169526L;
			
			private TextFieldWithSuggestions textField = null;
			private boolean searchCaseSensitive = false;
			
			NameSearch() {}

			@Override
			protected Component createContentPanel() {
				JPanel content = new JPanel(new GridBagLayout());
				GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.BOTH;
				c.weighty = 1;
				c.insets = new Insets(2,2,2,2);
				
				textField = new TextFieldWithSuggestions(20, this::search, this::suggestionSelected);
				textField.setMaxSuggestionsListLength(5);
				
				c.weightx = 1;
				content.add(textField,c);
				c.weightx = 0;
				content.add(Gui.createCheckbox("case sensitive", searchCaseSensitive, b->{
					searchCaseSensitive = b;
					textField.updateSuggestions();
				}),c);
				
				return content;
			}

			private void suggestionSelected(String name) {
				search(name);
				hidePopup();
			}

			private HashSet<String> search(String searchStr) {
				HashSet<String> fittingNames = new HashSet<>();
				boolean isMarked;
				int markedPlanets = 0;
				int markedSolarSystems = 0;
				int markedRegions = 0;
				for (Galaxy g:data.universe.galaxies) {
					setMarker( (GenericTreeNode<?>)g.guiComp, false );
					for (Region r:g.regions) {
						isMarked = shouldBeMarked(searchStr,r,fittingNames);
						setMarker( (GenericTreeNode<?>)r.guiComp, isMarked );
						if (isMarked) ++markedRegions;
						
						for (SolarSystem s:r.solarSystems) {
							isMarked = shouldBeMarked(searchStr,s,fittingNames);
							setMarker( (GenericTreeNode<?>)s.guiComp, isMarked );
							if (isMarked) ++markedSolarSystems;
							
							for (Planet p:s.planets) {
								isMarked = shouldBeMarked(searchStr,p,fittingNames);
								setMarker( (GenericTreeNode<?>)p.guiComp, isMarked );
								if (isMarked) ++markedPlanets;
							}
						}
					}
				}
				updateMarkerList();
				String resultOuput = generateResultOuput(markedPlanets, markedSolarSystems, markedRegions);
				setStatus(resultOuput);
				//SaveViewer.log_ln("marked: %d Planets, %d Solar Systems, %d Regions  \"%s\"", markedPlanets, markedSolarSystems, markedRegions, searchStr);
				
				return fittingNames;
			}

			private boolean shouldBeMarked(String searchStr, Region r, HashSet<String> fittingNames) {
				if (searchStr.length()<3) return false;
				
				boolean shouldBeMarked = false;
				shouldBeMarked |= shouldBeMarked(searchStr, r.hasOldName(),r.getOldName(),fittingNames);
				shouldBeMarked |= shouldBeMarked(searchStr, r.hasName(),r.getName(),fittingNames);
				return shouldBeMarked;
			}

			private boolean shouldBeMarked(String searchStr, DiscoverableObject obj, HashSet<String> fittingNames) {
				if (searchStr.length()<3) return false;
				
				boolean shouldBeMarked = false;
				shouldBeMarked |= shouldBeMarked(searchStr, obj.hasOldOriginalName(),obj.getOldOriginalName(),fittingNames);
				shouldBeMarked |= shouldBeMarked(searchStr, obj.hasOriginalName(),obj.getOriginalName(),fittingNames);
				shouldBeMarked |= shouldBeMarked(searchStr, obj.hasUploadedName(),obj.getUploadedName(),fittingNames);
				return shouldBeMarked;
			}
			private boolean shouldBeMarked(String searchStr, boolean hasName, String name, HashSet<String> fittingNames) {
				if (hasName && containsSearchStr(name,searchStr)) {
					fittingNames.add(name);
					return true;
				}
				return false;
			}

			private boolean containsSearchStr(String name, String searchStr) {
				if (searchCaseSensitive)
					return name.contains(searchStr);
				else
					return name.toLowerCase().contains(searchStr.toLowerCase());
			}
		}
		
		private abstract class ObjParameterSearchBar<ObjType> extends AbstractSearchBar {
			private static final long serialVersionUID = 606788005699319541L;

			ObjParameterSearchBar() {}

			@Override protected Component createContentPanel() {
				JPanel content = new JPanel(new GridBagLayout());
//				content.setBorder(
//						BorderFactory.createCompoundBorder(
//								BorderFactory.createLineBorder(Color.GRAY),
//								BorderFactory.createEmptyBorder(2,5,2,5)
//						)
//				);
				
				JPanel left  = new JPanel(new GridBagLayout());
				JPanel right = new JPanel(new GridBagLayout());
				setContent(left, right, new GridBagConstraints());
				
				GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.BOTH;
				
				c.weightx = 1;
				c.weighty = 1;
				c.insets = new Insets(2,5,2,5);
				
				c.gridwidth = 1;
				content.add(left,c);
				c.gridwidth = GridBagConstraints.REMAINDER;
				content.add(right,c);
				
				return content;
			}
			
			protected void updateMarkers() { SearchBar.this.updateMarkers(this::setStatus); }
			protected abstract void setContent(JPanel left, JPanel right, GridBagConstraints c);
			public abstract void clearMarkers();
			public abstract boolean shouldBeMarked(ObjType obj);
		}
		
		private class SolarSystemBar extends ObjParameterSearchBar<SolarSystem> {
			private static final long serialVersionUID = 7635343276037663444L;
			
			private Gui.IconComboBox<Race> cmbbxRace;
			private Gui.IconComboBox<StarClass> cmbbxStarClass;

			private Gui.IconComboBox<Integer> cmbbxConflictLevel;
			private Gui.IconComboBox<Integer> cmbbxEconomyLevel;

			private TristateCheckBox chkbxUnexplored;

			private TristateCheckBox chkbxAtlasInterface;
			private TristateCheckBox chkbxBlackHole;
			private TristateCheckBox chkbxReachableByTeleport;
			private TristateCheckBox chkbxRememTerm;

			SolarSystemBar() {}
			
			@Override
			protected void setContent(JPanel left, JPanel right, GridBagConstraints c) {
				
				cmbbxRace = new Gui.IconComboBox<Race>( SaveViewer.addNull(Race.values())) {
					private static final long serialVersionUID = 5328964374227212373L;
					
					@Override public Race cast(Object obj) {
						if (!(obj instanceof Race)) return null;
						return (Race)obj;
					}
					@Override public Icon createIcon(Race value) {
						return SolarSystemIcons.RaceIcons.get(value);
					}
					@Override public String getLabel(Race value) {
						if (value==null) return "<all values>";
						return value.fullName;
					}
				};
				cmbbxRace.addActionListener(e->updateMarkers());
				
				cmbbxStarClass = new Gui.IconComboBox<StarClass>( SaveViewer.addNull(StarClass.values())) {
					private static final long serialVersionUID = 5328964374227212373L;
					
					@Override public StarClass cast(Object obj) {
						if (!(obj instanceof StarClass)) return null;
						return (StarClass)obj;
					}
					@Override public Icon createIcon(StarClass value) {
						return SolarSystemIcons.StarClassIcons.get(value);
					}
					@Override public String getLabel(StarClass value) {
						if (value==null) return "<all values>";
						return value.getLabel();
					}
				};
				cmbbxStarClass.addActionListener(e->updateMarkers());
				
				cmbbxConflictLevel = new Gui.IconComboBox<Integer>( new Integer[] {null,1,2,3} ) {
					private static final long serialVersionUID = 5328964374227212373L;
					
					@Override public Integer cast(Object obj) {
						if (!(obj instanceof Integer)) return null;
						return (Integer)obj;
					}
					@Override public Icon createIcon(Integer value) {
						if (value==null || value<1 || value>3) return null;
						return SolarSystemIcons.ConflictLevelIcons[value-1];
					}
					@Override public String getLabel(Integer value) {
						if (value==null || value<1 || value>3) return "<all values>";
						return "Conflict Level "+value;
					}
				};
				cmbbxConflictLevel.addActionListener(e->updateMarkers());
				
				cmbbxEconomyLevel = new Gui.IconComboBox<Integer>( new Integer[] {null,1,2,3} ) {
					private static final long serialVersionUID = 5328964374227212373L;
					
					@Override public Integer cast(Object obj) {
						if (!(obj instanceof Integer)) return null;
						return (Integer)obj;
					}
					@Override public Icon createIcon(Integer value) {
						if (value==null || value<1 || value>3) return null;
						return SolarSystemIcons.EconomyLevelIcons[value-1];
					}
					@Override public String getLabel(Integer value) {
						if (value==null || value<1 || value>3) return "<all values>";
						return "Economy Level "+value;
					}
				};
				cmbbxEconomyLevel.addActionListener(e->updateMarkers());
				
				chkbxUnexplored          = Gui.createTristateCheckBox("is unexplored"           , e->updateMarkers(), TristateCheckBox.State.UNDEFINED);
				chkbxAtlasInterface      = Gui.createTristateCheckBox("has atlas interface"     , e->updateMarkers(), TristateCheckBox.State.UNDEFINED);
				chkbxBlackHole           = Gui.createTristateCheckBox("has black hole"          , e->updateMarkers(), TristateCheckBox.State.UNDEFINED);
				chkbxReachableByTeleport = Gui.createTristateCheckBox("is reachable by teleport", e->updateMarkers(), TristateCheckBox.State.UNDEFINED);
				chkbxRememTerm           = Gui.createTristateCheckBox("has remembrance terminal", e->updateMarkers(), TristateCheckBox.State.UNDEFINED);
				
				c.fill = GridBagConstraints.BOTH;
				
				c.weightx = 1;
				c.weighty = 0;
				c.gridwidth = GridBagConstraints.REMAINDER;
				left.add( cmbbxRace         , c );
				left.add( cmbbxStarClass    , c );
				left.add( cmbbxConflictLevel, c );
				left.add( cmbbxEconomyLevel , c );
				right.add( chkbxUnexplored         , c );
				right.add( chkbxAtlasInterface     , c );
				right.add( chkbxBlackHole          , c );
				right.add( chkbxReachableByTeleport, c );
				right.add( chkbxRememTerm          , c );
				
				c.gridwidth = GridBagConstraints.REMAINDER;
				c.weighty = 1;
				left.add(new JLabel(),c);
				right.add(new JLabel(),c);
			}

			@Override
			public void clearMarkers() {
				cmbbxRace         .setSelectedItem(null);
				cmbbxStarClass    .setSelectedItem(null);
				cmbbxConflictLevel.setSelectedItem(null);
				cmbbxEconomyLevel .setSelectedItem(null);
				chkbxUnexplored         .setUndefined();
				chkbxAtlasInterface     .setUndefined();
				chkbxBlackHole          .setUndefined();
				chkbxReachableByTeleport.setUndefined();
				chkbxRememTerm          .setUndefined();
			}

			@Override
			public boolean shouldBeMarked(SolarSystem sys) {
				if (isUnset()) return false;
				
				Race race = cmbbxRace.getSelected();
				if (race!=null && sys.race!=race) return false;
				
				StarClass starClass = cmbbxStarClass.getSelected();
				if (starClass!=null && sys.starClass!=starClass) return false;
				
				Integer conflictLevel = cmbbxConflictLevel.getSelected();
				if (conflictLevel!=null && sys.conflictLevel!=conflictLevel) return false;
				
				Integer economyLevel = cmbbxEconomyLevel.getSelected();
				if (economyLevel!=null && sys.economyLevel!=economyLevel) return false;
				
				if (!chkbxUnexplored         .isUndefined() && sys.isUnexplored           !=chkbxUnexplored    .isSelected()) return false;
				if (!chkbxAtlasInterface     .isUndefined() && sys.hasAtlasInterface      !=chkbxAtlasInterface.isSelected()) return false;
				if (!chkbxBlackHole          .isUndefined() && sys.hasBlackHole           !=chkbxBlackHole     .isSelected()) return false;
				if (!chkbxReachableByTeleport.isUndefined() && sys.additionalInfos.teleportEndpoints.isEmpty()==chkbxReachableByTeleport.isSelected()) return false;
				if (!chkbxRememTerm          .isUndefined() && sys.withRemembranceTerminal!=chkbxRememTerm     .isSelected()) return false;
				
				return true;
			}
			
			private boolean isUnset() {
				return				
					cmbbxRace         .getSelectedItem()==null &&
					cmbbxStarClass    .getSelectedItem()==null &&
					cmbbxConflictLevel.getSelectedItem()==null &&
					cmbbxEconomyLevel .getSelectedItem()==null &&
					chkbxUnexplored         .isUndefined() &&
					chkbxAtlasInterface     .isUndefined() &&
					chkbxBlackHole          .isUndefined() &&
					chkbxReachableByTeleport.isUndefined() &&
					chkbxRememTerm          .isUndefined();
			}
		}
		
		private class PlanetBar extends ObjParameterSearchBar<Planet> {
			private static final long serialVersionUID = -5056089942590204797L;
			
			private Gui.IconComboBox<Biome> cmbbxBiome;
			private JComboBox<BuriedTreasure> cmbbxBuriedTreasure;
			
			private TristateCheckBox chkbxExtreme;
			private TristateCheckBox chkbxAggrSent;
			private TristateCheckBox chkbxWater;
			private TristateCheckBox chkbxGrav;
			private TristateCheckBox chkbxRememTerm;

			private TristateCheckBox chkbxVehicleSummoner;
			private TristateCheckBox chkbxPlayerBase;
			private TristateCheckBox chkbxOtherPlayerBase;
			private TristateCheckBox chkbxTeleporter;

			private ResourceSelectDialog resourceSelectDialog;
			private EnumSet<Resources> resources;
			private boolean findAllRes;

			private JTextField txtfldResources;
			private JRadioButton rdbtnAllRes;
			private JRadioButton rdbtnOneRes;
			
			PlanetBar() {}
			
			@Override
			protected void setContent(JPanel left, JPanel right, GridBagConstraints c) {
				
				cmbbxBiome = new Gui.IconComboBox<Biome>( SaveViewer.addNull(Biome.values()) ) {
					private static final long serialVersionUID = 5328964374227212373L;
					
					@Override public Biome cast(Object obj) {
						if (!(obj instanceof Biome)) return null;
						return (Biome)obj;
					}
					@Override public Icon createIcon(Biome value) {
						return PlanetIcons.BiomeIcons.get(value);
					}
					@Override public String getLabel(Biome value) {
						if (value==null) return "<all values>";
						return value.name_EN;
					}
				};
				cmbbxBiome.addActionListener(e->updateMarkers());
				
				cmbbxBuriedTreasure = new JComboBox<BuriedTreasure>( SaveViewer.addNull(BuriedTreasure.values()));
//				cmbbxBuriedTreasure = new Gui.IconComboBox<BuriedTreasure>( SaveViewer.addNull(BuriedTreasure.values()), 170,20, new Gui.IconComboBox.ExternalFunctionality<BuriedTreasure>() {
//					@Override public BuriedTreasure cast(Object obj) {
//						if (!(obj instanceof BuriedTreasure)) return null;
//						return (BuriedTreasure)obj;
//					}
//					@Override public Icon createIcon(BuriedTreasure value) {
//						return null; // PlanetIcons.BiomeIcons.get(value);
//					}
//					@Override public String getLabel(BuriedTreasure value) {
//						if (value==null) return "<none>";
//						return value.name_EN;
//					}
//				});
				cmbbxBuriedTreasure.addActionListener(e->updateMarkers());
				
				resourceSelectDialog = null;
				resources = EnumSet.noneOf(Resources.class);
				findAllRes = true;
				
				ButtonGroup bgResources = new ButtonGroup();
				rdbtnAllRes = Gui.createRadioButton("all resources", bgResources ,  findAllRes, true, e->{ findAllRes = true ; updateMarkers(); });
				rdbtnOneRes = Gui.createRadioButton("at least one" , bgResources , !findAllRes, true, e->{ findAllRes = false; updateMarkers(); });
				
				txtfldResources = new JTextField(20);
				txtfldResources.setEditable(false);
				
				JButton btnSetResources = Gui.createButton("Set", e->{
					if (resourceSelectDialog==null) resourceSelectDialog = new ResourceSelectDialog(mainWindow, "Select Planetary Resources");
					EnumSet<Resources> result = resourceSelectDialog.showDialog(resources);
					if (result!=null) {
						resources.clear();
						resources.addAll(result);
						updateResourceComps();
						updateMarkers();
					}
				});
				
				chkbxExtreme         = Gui.createTristateCheckBox("is Extreme"                , e->updateMarkers(), TristateCheckBox.State.UNDEFINED);
				chkbxAggrSent        = Gui.createTristateCheckBox("has Aggressive Sentinels"  , e->updateMarkers(), TristateCheckBox.State.UNDEFINED);
				chkbxWater           = Gui.createTristateCheckBox("has Water"                 , e->updateMarkers(), TristateCheckBox.State.UNDEFINED);
				chkbxGrav            = Gui.createTristateCheckBox("has Gravitino Balls"       , e->updateMarkers(), TristateCheckBox.State.UNDEFINED);
				chkbxRememTerm       = Gui.createTristateCheckBox("has RemembranceTerminal"   , e->updateMarkers(), TristateCheckBox.State.UNDEFINED);
				chkbxVehicleSummoner = Gui.createTristateCheckBox("has Vehicle Summoner"      , e->updateMarkers(), TristateCheckBox.State.UNDEFINED);
				chkbxPlayerBase      = Gui.createTristateCheckBox("has Base"                  , e->updateMarkers(), TristateCheckBox.State.UNDEFINED);
				chkbxOtherPlayerBase = Gui.createTristateCheckBox("has Base of another Player", e->updateMarkers(), TristateCheckBox.State.UNDEFINED);
				chkbxTeleporter      = Gui.createTristateCheckBox("is reachable by Teleport"  , e->updateMarkers(), TristateCheckBox.State.UNDEFINED);
				
				c.fill = GridBagConstraints.BOTH;
				
				c.weightx = 1;
				c.weighty = 0;
				c.gridwidth = GridBagConstraints.REMAINDER;
				left.add( cmbbxBiome, c );
				left.add( chkbxExtreme, c );
				left.add( chkbxAggrSent, c );
				left.add( chkbxWater, c );
				
				JPanel resPanel = new JPanel(new BorderLayout());
				resPanel.add(txtfldResources,BorderLayout.CENTER);
				resPanel.add(btnSetResources,BorderLayout.EAST);
				left.add( resPanel, c );
				
				JPanel andOrPanel = new JPanel(new GridLayout(1,0));
				andOrPanel.add(rdbtnAllRes);
				andOrPanel.add(rdbtnOneRes);
				left.add( andOrPanel, c );
				
				right.add( cmbbxBuriedTreasure, c );
				right.add( chkbxGrav, c );
				right.add( chkbxRememTerm, c );
				
				c.gridwidth = 1;
				right.add( new JLabel(AdditionalIcons.getCachedIcon(AdditionalTreeIcons.VehicleSummoner)), c );
				c.gridwidth = GridBagConstraints.REMAINDER;
				right.add( chkbxVehicleSummoner, c );
				
				c.gridwidth = 1;
				right.add( new JLabel(AdditionalIcons.getCachedIcon(AdditionalTreeIcons.BaseMainRoom)), c );
				c.gridwidth = GridBagConstraints.REMAINDER;
				right.add( chkbxPlayerBase, c );
				
				c.gridwidth = 1;
				right.add( new JLabel(AdditionalIcons.getCachedIcon(AdditionalTreeIcons.BaseMainRoomOther)), c );
				c.gridwidth = GridBagConstraints.REMAINDER;
				right.add( chkbxOtherPlayerBase, c );
				
				c.gridwidth = 1;
				right.add( new JLabel(AdditionalIcons.getCachedIcon(AdditionalTreeIcons.Teleporter)), c );
				c.gridwidth = GridBagConstraints.REMAINDER;
				right.add( chkbxTeleporter, c );
				
				c.gridwidth = GridBagConstraints.REMAINDER;
				c.weighty = 1;
				left.add(new JLabel(),c);
				right.add(new JLabel(),c);
				
				updateResourceComps();
			}

			private void updateResourceComps() {
				rdbtnAllRes.setEnabled(!resources.isEmpty());
				rdbtnOneRes.setEnabled(!resources.isEmpty());
				txtfldResources.setText(String.join(", ", Universe.Planet.Resources.getStringIterable(resources, Resources::getShortLabel)));
			}

			@Override
			public boolean shouldBeMarked(Planet p) {
				if (isUnset()) return false;
				
				Biome biome = cmbbxBiome.getSelected();
				if (biome != null && p.biome != biome) return false;
				
				BuriedTreasure buriedTreasure = (BuriedTreasure)cmbbxBuriedTreasure.getSelectedItem();
				if (buriedTreasure != null && p.buriedTreasure != buriedTreasure) return false;
				
				if (!resources.isEmpty()) {
					boolean found = findAllRes;
					for (Resources res:resources)
						if (p.resources.contains(res)) { if (!findAllRes) { found = true ; break; } }
						else                           { if ( findAllRes) { found = false; break; } }
					if (!found) return false;
				}
				
				if (!chkbxExtreme        .isUndefined() && p.hasExtremeBiome         != chkbxExtreme  .isSelected()) return false;
				if (!chkbxAggrSent       .isUndefined() && p.areSentinelsAggressive  != chkbxAggrSent .isSelected()) return false;
				if (!chkbxWater          .isUndefined() && p.withWater               != chkbxWater    .isSelected()) return false;
				if (!chkbxGrav           .isUndefined() && p.withGravitinoBalls      != chkbxGrav     .isSelected()) return false;
				if (!chkbxRememTerm      .isUndefined() && p.withRemembranceTerminal != chkbxRememTerm.isSelected()) return false;
				if (!chkbxVehicleSummoner.isUndefined() && p.additionalInfos.hasExocraftSummoningStation != chkbxVehicleSummoner.isSelected()) return false;
				if (!chkbxPlayerBase     .isUndefined() && p.additionalInfos.playerBases.isEmpty()       == chkbxPlayerBase     .isSelected()) return false;
				if (!chkbxOtherPlayerBase.isUndefined() && p.additionalInfos.hasOtherPlayersBase()       != chkbxOtherPlayerBase.isSelected()) return false;
				if (!chkbxTeleporter     .isUndefined() && p.additionalInfos.teleportEndpoints.isEmpty() == chkbxTeleporter     .isSelected()) return false;
				
				return true;
			}
			
			private boolean isUnset() {
				return
					resources.isEmpty() &&
					cmbbxBiome.getSelectedItem()==null &&
					cmbbxBuriedTreasure.getSelectedItem()==null &&
					chkbxExtreme        .isUndefined() &&
					chkbxAggrSent       .isUndefined() &&
					chkbxWater          .isUndefined() &&
					chkbxGrav           .isUndefined() &&
					chkbxRememTerm      .isUndefined() &&
					chkbxVehicleSummoner.isUndefined() &&
					chkbxPlayerBase     .isUndefined() &&
					chkbxOtherPlayerBase.isUndefined() &&
					chkbxTeleporter     .isUndefined();
			}

			@Override
			public void clearMarkers() {
				resources.clear();
				updateResourceComps();
				cmbbxBiome          .setSelectedItem(null);
				cmbbxBuriedTreasure .setSelectedItem(null);
				chkbxExtreme        .setUndefined();
				chkbxAggrSent       .setUndefined();
				chkbxWater          .setUndefined();
				chkbxGrav           .setUndefined();
				chkbxRememTerm      .setUndefined();
				chkbxVehicleSummoner.setUndefined();
				chkbxPlayerBase     .setUndefined();
				chkbxOtherPlayerBase.setUndefined();
				chkbxTeleporter     .setUndefined();
			}
			
		}
	}

	private static class FindExtraInfoDialog extends StandardDialog {
		private static final long serialVersionUID = -356863578675221086L;
		
		private Universe universe;
		private SimplifiedTable<FoundExtraInfoColumnID> table;
		private FoundExtraInfoTableModel tableModel;

		private JButton btnOK;

		public FindExtraInfoDialog(Window parent, Universe universe) {
			super(parent, "Find Universe Object", ModalityType.APPLICATION_MODAL);
			this.universe = universe;
			
			table = new SimplifiedTable<>("FindObjectDialog",false,SaveViewer.DEBUG,true);
			table.setPreferredScrollableViewportSize(new Dimension(500, 600));;
			tableModel = null;
			
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			JCheckBox chkbx;
			buttonPanel.add(chkbx = Gui.createCheckbox("Allow Editing", null, false));
			buttonPanel.add(btnOK = Gui.createButton("Show Selected in Tree", e->{ if (tableModel!=null) tableModel.setSelected(); closeDialog(); }));
			buttonPanel.add(Gui.createButton("Close", e->closeDialog()));
			
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

		public HashSet<GenericTreeNode<?>> getChangedObjects() {
			if (tableModel!=null) return tableModel.changedObj;
			return new HashSet<>();
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

		private void addInfos(Vector<FoundExtraInfo> infos, DiscoverableObject obj, boolean isPlanet) {
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
			Vector<DiscoverableObject> sourceObjs;
			
			public FoundExtraInfo(boolean fromPlanet, DiscoverableObject obj, ExtraInfo ei) {
				this.fromPlanet = fromPlanet;
				this.label = ei.shortLabel;
				this.info = ei.info;
				this.sourceEIs = new Vector<>();
				this.sourceObjs = new Vector<>();
				this.isSelected = false;
				add(obj,ei);
			}

			public void add(DiscoverableObject obj, ExtraInfo ei) {
				this.sourceEIs.add(ei);
				this.sourceObjs.add(obj);
			}
		}

		private enum FoundExtraInfoColumnID implements SimplifiedColumnIDInterface {
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
		
		private static class FoundExtraInfoTableModel extends Tables.SimplifiedTableModel<FoundExtraInfoColumnID> {

			private FindExtraInfoDialog dialog;
			private FoundExtraInfo[] data;
			private HashSet<GenericTreeNode<?>> changedObj;
			private boolean allowEditing;
			private Universe universe;

			protected FoundExtraInfoTableModel(FindExtraInfoDialog dialog, Universe universe, FoundExtraInfo[] data) {
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
				HashSet<DiscoverableObject> notSelectedObjs = new HashSet<>();
				HashSet<DiscoverableObject> selectedObjs = new HashSet<>();
				for (FoundExtraInfo fei:data)
					if (fei.isSelected)
						selectedObjs.addAll(fei.sourceObjs);
					else
						notSelectedObjs.addAll(fei.sourceObjs);
				notSelectedObjs.removeAll(selectedObjs);
				
				for (DiscoverableObject obj:   selectedObjs) setHighlighted(obj, true );
				for (DiscoverableObject obj:notSelectedObjs) setHighlighted(obj, false);
			}

			private void setHighlighted(DiscoverableObject obj, boolean highlighted) {
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
					for (DiscoverableObject obj:data[rowIndex].sourceObjs) changedObj.add((GenericTreeNode<?>)obj.guiComp);
					GameInfos.saveUniverseObjectDataToFile(universe);
					break;
					
				case Info    :
					data[rowIndex].info = (String)aValue;
					for (     ExtraInfo  ei:data[rowIndex].sourceEIs ) ei.info = (String)aValue;
					for (DiscoverableObject obj:data[rowIndex].sourceObjs) changedObj.add((GenericTreeNode<?>)obj.guiComp);
					GameInfos.saveUniverseObjectDataToFile(universe);
					break;
				}
			}
			
		}
		
	}
	
	static class UniverseTreeCellRenderer extends DefaultTreeCellRenderer {

		private static final Color TEXTCOLOR__HIGHLIGHTED      = Color.RED;
		private static final Color TEXTCOLOR__CURRENT_POS      = new Color(0x2EA000);
		private static final Color TEXTCOLOR__NEAR_CURRENT_POS = new Color(0x2EA000);
		private static final Color TEXTCOLOR__WITHOUT_NAME     = new Color(0x808080);
		private static final Color TEXTCOLOR__NOT_UPLOADED     = new Color(0x0000FF); // or 0x1D67AE
		private static final Color TEXTCOLOR__ONLY_IN_DB       = Color.MAGENTA;

		private static final long serialVersionUID = 4733567681038484432L;
		
		private Font boldfont;
		private Font standardFont;
		private SaveGameData data;
		
		UniverseTreeCellRenderer(SaveGameData data) {
			this.data = data;
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
			component.setIcon(getIcon(node));
			component.setFont(standardFont);
			Universe.DiscoverableObject discObj = null;
			Universe.ObjectWithSource obj = null;
			Region region = null;
			if (node.value instanceof ObjectWithSource) obj = (ObjectWithSource) node.value;
			if (node instanceof      RegionNode) obj = region = ((     RegionNode)node).value;
			if (node instanceof SolarSystemNode) obj = discObj = ((SolarSystemNode)node).value;
			if (node instanceof      PlanetNode) obj = discObj = ((     PlanetNode)node).value;
			if (discObj!=null) {
				if (!discObj.hasOriginalName()) {
					if (!selected) component.setForeground(TEXTCOLOR__WITHOUT_NAME);
				}
			}
			if (obj!=null) {
				if (obj.isNotUploaded()) {
					if (!selected) component.setForeground(TEXTCOLOR__NOT_UPLOADED);
				}
				if (!obj.hasSourceID()) {
					if (!selected) component.setForeground(TEXTCOLOR__ONLY_IN_DB);
				} 
				if (obj.isCurrPos) {
					if (!selected) component.setForeground(TEXTCOLOR__CURRENT_POS);
					component.setFont(boldfont);
				} 
				if (obj.containsCurrPos) {
					if (!selected) component.setForeground(TEXTCOLOR__CURRENT_POS);
				} 
			}
			if (region!=null) {
				if (!region.hasName()) {
					if (!selected) component.setForeground(TEXTCOLOR__WITHOUT_NAME);
				}
				if (data.general.currentUniverseAddress!=null) {
					double dist = data.general.currentUniverseAddress.getDistToOther_inRegionUnits(region.getUniverseAddress());
					if (dist<2 && !selected)
						component.setForeground(TEXTCOLOR__NEAR_CURRENT_POS);
				}
			}
			if (node.isHighlighted()) {
				if (!selected) component.setForeground(TEXTCOLOR__HIGHLIGHTED);
				component.setFont(boldfont);
			}
		}

		private Icon getIcon(GenericTreeNode<?> node) {
			switch (node.type) {
			case Universe   : return UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.Universe);
			case Galaxy     : return UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.Galaxy  );
			case Region     : return UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.Region  );
			case SolarSystem:
				if (node instanceof SolarSystemNode) {
					SolarSystemNode solarSystemNode = (SolarSystemNode)node;
					SolarSystem system = solarSystemNode.value;
					if (!system.additionalInfos.isEmpty() || system.hasAtlasInterface || system.hasBlackHole) {
						if (solarSystemNode.cachedCustomIcon!=null && solarSystemNode.cachedCustomIcon.is(system.race,system.starClass,system.conflictLevel,system.economyLevel,system.isUnexplored,system.hasAtlasInterface,system.hasBlackHole))
							return solarSystemNode.cachedCustomIcon.get();
						else {
							Icon icon = SolarSystemIcons.get(system.race,system.starClass,system.conflictLevel,system.economyLevel,system.isUnexplored);
							if (system.hasAtlasInterface)
								icon = IconSource.setSideBySide(icon,AdditionalIcons.getCachedIcon(AdditionalTreeIcons.Atlas));
							if (system.hasBlackHole)
								icon = IconSource.setSideBySide(icon,AdditionalIcons.getCachedIcon(AdditionalTreeIcons.BlackHole));
							if (!system.additionalInfos.teleportEndpoints.isEmpty())
								icon = IconSource.setSideBySide(icon,AdditionalIcons.getCachedIcon(AdditionalTreeIcons.Teleporter));
							if (system.additionalInfos.hasFreighter)
								icon = IconSource.setSideBySide(icon,AdditionalIcons.getCachedIcon(AdditionalTreeIcons.Freighter));
							if (system.additionalInfos.hasAnomaly)
								icon = IconSource.setSideBySide(icon,AdditionalIcons.getCachedIcon(AdditionalTreeIcons.Anomaly));
							solarSystemNode.cachedCustomIcon = new SolarSystemNode.CachedCustomIcon(icon,system.race,system.starClass,system.conflictLevel,system.economyLevel,system.isUnexplored,system.hasAtlasInterface,system.hasBlackHole);
							return icon;
						}
					} else
						return SolarSystemIcons.get(system.race,system.starClass,system.conflictLevel,system.economyLevel,system.isUnexplored);
				}
				break;
			case Planet:
				if (node instanceof PlanetNode) {
					PlanetNode planetNode = (PlanetNode)node;
					Planet planet = planetNode.value;
					if (!planet.additionalInfos.isEmpty()) {
						if (planetNode.cachedCustomIcon!=null && planetNode.cachedCustomIcon.is(planet.biome, planet.areSentinelsAggressive))
							return planetNode.cachedCustomIcon.get();
						else {
							Icon icon = PlanetIcons.get(planet.biome, planet.areSentinelsAggressive);
							if (planet.additionalInfos.isReachableByTeleport())
								icon = IconSource.setSideBySide(icon,AdditionalIcons.getCachedIcon(AdditionalTreeIcons.Teleporter));
							if (!planet.additionalInfos.playerBases.isEmpty())
								icon = IconSource.setSideBySide(icon,AdditionalIcons.getCachedIcon(AdditionalTreeIcons.BaseMainRoom));
							if (planet.additionalInfos.hasOtherPlayersBase())
								icon = IconSource.setSideBySide(icon,AdditionalIcons.getCachedIcon(AdditionalTreeIcons.BaseMainRoomOther));
							if (planet.additionalInfos.hasExocraftSummoningStation)
								icon = IconSource.setSideBySide(icon,AdditionalIcons.getCachedIcon(AdditionalTreeIcons.VehicleSummoner));
							planetNode.cachedCustomIcon = new PlanetNode.CachedCustomIcon(icon,planet.biome, planet.areSentinelsAggressive);
							return icon;
						}
					} else
						return PlanetIcons.get(planet.biome, planet.areSentinelsAggressive);
				}
				break;
			}
			return null;
		}
	}
	
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
	
	static abstract class GenericTreeNode<V extends UniverseObject> extends LocalTreeNode {
		
		SaveGameData.UniverseObject.Type type;
		V value;
		boolean isHighlighted;

		protected GenericTreeNode(LocalTreeNode parent, SaveGameData.UniverseObject.Type type, V value) {
			super(parent);
			this.value = value;
			this.type = type;
			isHighlighted = false;
			value.guiComp = this;
			createChildren();
		}

		public boolean isHighlighted() { return isHighlighted; }
		@Override public boolean getAllowsChildren() { return type!=SaveGameData.UniverseObject.Type.Planet; }
		@Override protected String getLabel() { return value.toString(); }
		@Override protected Comparator<Integer> getSorter() { return null; }
		
	}
	
	static class UniverseNode extends GenericTreeNode<Universe> {
		private UniverseNode(Universe value) { super(null, SaveGameData.UniverseObject.Type.Universe, value); }
		@Override protected int getDataChildrenCount() { return value.galaxies.size(); }
		@Override protected LocalTreeNode createTreeChild(int i) { return new GalaxyNode(this,value.galaxies.get(i)); }
	}
	static class GalaxyNode extends GenericTreeNode<Galaxy> {
		private GalaxyNode(UniverseNode parent, Galaxy value) { super(parent, SaveGameData.UniverseObject.Type.Galaxy, value); }
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
		private RegionNode(GalaxyNode parent, Region value) { super(parent, SaveGameData.UniverseObject.Type.Region, value); }
		@Override protected int getDataChildrenCount() { return value.solarSystems.size(); }
		@Override protected LocalTreeNode createTreeChild(int i) { return new SolarSystemNode(this,value.solarSystems.get(i)); }
		@Override protected String getLabel() { return String.format(Locale.ENGLISH, "%s [%1.1f ly]", value.toString(), value.getUniverseAddress().getDistToCenter_inRegionUnits()*400); }
	}
	static class SolarSystemNode extends GenericTreeNode<SolarSystem> {
		CachedCustomIcon cachedCustomIcon;
		private SolarSystemNode(RegionNode parent, SolarSystem value) { super(parent, SaveGameData.UniverseObject.Type.SolarSystem, value); cachedCustomIcon = null; }
		@Override protected int getDataChildrenCount() { return value.planets.size(); }
		@Override protected LocalTreeNode createTreeChild(int i) { return new PlanetNode(this,value.planets.get(i)); }
		//@Override protected String getLabel() { return value.hasName()?value.getName():super.getLabel(); }
		
		static class CachedCustomIcon {
			private final Icon icon;
			private final Race race;
			private final StarClass starClass;
			private final int conflictLevel;
			private final int economyLevel;
			private final boolean isUnexplored;
			protected boolean hasAtlasInterface;
			private boolean hasBlackHole;

			public CachedCustomIcon(Icon icon, Race race, StarClass starClass, int conflictLevel, int economyLevel, boolean isUnexplored, boolean hasAtlasInterface, boolean hasBlackHole) {
				this.icon = icon;
				this.race = race;
				this.starClass = starClass;
				this.conflictLevel = conflictLevel;
				this.economyLevel = economyLevel;
				this.isUnexplored = isUnexplored;
				this.hasAtlasInterface = hasAtlasInterface;
				this.hasBlackHole = hasBlackHole;
			}
			public boolean is(Race race, StarClass starClass, int conflictLevel, int economyLevel, boolean isUnexplored, boolean hasAtlasInterface, boolean hasBlackHole) {
				return
						this.race==race &&
						this.starClass==starClass &&
						this.conflictLevel==conflictLevel &&
						this.economyLevel==economyLevel &&
						this.isUnexplored==isUnexplored &&
						this.hasAtlasInterface==hasAtlasInterface &&
						this.hasBlackHole==hasBlackHole;
			}
			public Icon get() {
				return icon;
			}
		}
	}
	static class PlanetNode extends GenericTreeNode<Planet> {
		CachedCustomIcon cachedCustomIcon;
		private PlanetNode(SolarSystemNode parent, Planet value) { super(parent, SaveGameData.UniverseObject.Type.Planet, value); cachedCustomIcon = null; }
		@Override protected int getDataChildrenCount() { return 0; }
		@Override protected LocalTreeNode createTreeChild(int i) { throw new UnsupportedOperationException("Can't create a TreeChild from a PlanetNode."); }
		//@Override protected String getLabel() { return value.hasName()?value.getName():super.getLabel(); }
		
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
