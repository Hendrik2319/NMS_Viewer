package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
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
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.schwarzbaer.gui.IconSource;
import net.schwarzbaer.gui.StandardDialog;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Gui;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Galaxy;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Planet;
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
	
	enum UniverseTreeActionCommand { SetName, SetDistance, ExpandAll, CollapseRemainingTree, FindObject }
	
	enum UniverseTreeIcons { Universe, Galaxy, Region, SolarSystem, Planet, GekSys, KorvaxSys, VykeenSys, Yellow, Red, Green, Blue }
	private static final int TreeIconHeight = 20;
	static IconSource.CachedIcons<UniverseTreeIcons> UniverseTreeIconsIS = null;
	
	public static IconSource.CachedIndexedImages PortalGlyphsIS = null;
	
	private static class UniverseTreeSolarSystemIconsMap {
		
		private Icon[][] values;
		
		UniverseTreeSolarSystemIconsMap() {
			values = null;
		}
		
		void createValues() {
			Icon icon = UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.SolarSystem);
			
			Race[] races = Race.values();
			Icon[] raceIcons = new Icon[races.length];
			for (Race race:races)
				switch(race) {
				case Gek   : raceIcons[race.ordinal()] = UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.GekSys);    break;
				case Korvax: raceIcons[race.ordinal()] = UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.KorvaxSys); break;
				case Vykeen: raceIcons[race.ordinal()] = UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.VykeenSys); break;
				}
			
			StarClass[] starClasses = StarClass.values();
			Icon[] starClassIcons = new Icon[starClasses.length];
			for (StarClass starClass:starClasses)
				switch(starClass) {
				case Yellow: starClassIcons[starClass.ordinal()] = UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.Yellow); break;
				case Red   : starClassIcons[starClass.ordinal()] = UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.Red   ); break;
				case Green : starClassIcons[starClass.ordinal()] = UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.Green ); break;
				case Blue  : starClassIcons[starClass.ordinal()] = UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.Blue  ); break;
				}
			
			values = new Icon[races.length+1][starClasses.length+1];
			values[0][0] = icon;
			Icon icon2;
			for (Race race:races) {
				icon2 = IconSource.combine(icon, raceIcons[race.ordinal()]);
				values[race.ordinal()+1][0] = icon2;
				for (StarClass starClass:starClasses)
					values[race.ordinal()+1][starClass.ordinal()+1] = IconSource.combine(icon2, starClassIcons[starClass.ordinal()]);
			}
			for (StarClass starClass:starClasses)
				values[0][starClass.ordinal()+1] = IconSource.combine(icon, starClassIcons[starClass.ordinal()]);
			
		}
		
//			void put(Race race, StarClass starClass, Icon value) {
//				values[race.ordinal()][starClass.ordinal()] = value;
//			}
		
		Icon get(Race race, StarClass starClass) {
			int raceIndex      = race     ==null?0:(race     .ordinal()+1);
			int starClassIndex = starClass==null?0:(starClass.ordinal()+1);
			return values[raceIndex][starClassIndex];
		}
	}
	
	private static HashMap<Race,Icon> RaceIcons = null;
	private static HashMap<StarClass,Icon> StarClassIcons = null;
	private static UniverseTreeSolarSystemIconsMap UniverseTreeSolarSystemIcons = null;

	public static void prepareIconSources() {
		
		IconSource.IndexOnlyIconSource PortalGlyphsIS_ = new IconSource.IndexOnlyIconSource( 50,45,4);
		PortalGlyphsIS_.readIconsFromResource("/images/PortalGlyphs.50.45.png");
		PortalGlyphsIS = PortalGlyphsIS_.cacheImages(16);
		String[] labels = new String[]{"N","NNE","NE","ENE","E","ESE","SE","SSE","S","SSW","SW","WSW","W","WNW","NW","NNW"};
		for (int i=0; i<16; ++i) {
			BufferedImage cachedImage = PortalGlyphsIS.getCachedImage(i);
			Graphics g = cachedImage.getGraphics();
			if (!(g instanceof Graphics2D)) return;
			Graphics2D g2 = (Graphics2D)g;
			
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setFont(g2.getFont().deriveFont(10.0f).deriveFont(Font.BOLD));
			g2.setPaint(new Color(0xFFAF00));
			g2.drawString(i+" "+labels[i], 2, 10);
		}
		
		IconSource<UniverseTreeIcons> UniverseTreeIcons_ = new IconSource<UniverseTreeIcons>(30,TreeIconHeight);
		UniverseTreeIcons_.readIconsFromResource("/images/UniverseTreeIcons.png");
		UniverseTreeIconsIS = UniverseTreeIcons_.cacheIcons(UniverseTreeIcons.values());
		
		Icon icon = UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.Galaxy);
		icon = IconSource.cutIcon(icon,5,0,20,20);
		UniverseTreeIconsIS.setCachedIcon(UniverseTreeIcons.Galaxy,icon);
		
		UniverseTreeSolarSystemIcons = new UniverseTreeSolarSystemIconsMap();
		UniverseTreeSolarSystemIcons.createValues();
		
		RaceIcons = new HashMap<>();
		Race[] races = Race.values();
		for (Race race:races) {
			Icon cachedIcon = null;
			switch(race) {
			case Gek   : cachedIcon = UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.GekSys   ); break;
			case Korvax: cachedIcon = UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.KorvaxSys); break;
			case Vykeen: cachedIcon = UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.VykeenSys); break;
			}
			RaceIcons.put(race, IconSource.cutIcon(cachedIcon,10,0,20,20)); 
		}
		
		StarClassIcons = new HashMap<>();
		StarClass[] starClasses = StarClass.values();
		for (StarClass starClass:starClasses) {
			Icon cachedIcon = UniverseTreeSolarSystemIcons.get(null, starClass);
			StarClassIcons.put(starClass, IconSource.cutIcon(cachedIcon,0,0,20,20)); 
		}
	}

	private JTree tree;
	private DefaultTreeModel treeModel;
	private UniverseNode treeRoot;
	
	private GenericTreeNode<?> selectedNode;
	private GenericTreeNode<?> clickedNode;
	private TreePath clickedTreePath;
	
	private JLabel portalGlyphs;
	private JTextArea textArea;
//		private ExtraInfoTableModel extraInfoTableModel;
	private SimplifiedTable extraInfoTable;
	
	private JPopupMenu contextMenu_Other;
	private JPopupMenu contextMenu_Named;
	private JPopupMenu contextMenu_SolarSystem;
	private JMenuItem miSetName_Named;
	private JMenuItem miSetName_SolarSystem;
	private Gui.ListMenuItems<StarClass> miSetStarClass;
	private Gui.ListMenuItems<Race> miSetRace;
	
	private Window mainWindow;
	
	public UniversePanel(SaveGameData data, Window mainWindow) {
		super(data);
		this.mainWindow = mainWindow;
		
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
		
		contextMenu_Other = new JPopupMenu("Contextmenu");
		contextMenu_Other.add(createMenuItem("Find universe object",UniverseTreeActionCommand.FindObject));
		contextMenu_Other.addSeparator();
		contextMenu_Other.add(createMenuItem("Expand complete tree",UniverseTreeActionCommand.ExpandAll));
		contextMenu_Other.add(createMenuItem("Collapse remaining tree",UniverseTreeActionCommand.CollapseRemainingTree));
		
		contextMenu_SolarSystem = new JPopupMenu("SolarSystem");
		contextMenu_SolarSystem.add(miSetName_SolarSystem = createMenuItem("Change Name",UniverseTreeActionCommand.SetName));
		contextMenu_SolarSystem.add(createMenuItem("Set measured distance to center of galaxy ",UniverseTreeActionCommand.SetDistance));
		
		contextMenu_SolarSystem.addSeparator();
		miSetRace = new Gui.ListMenuItems<Universe.SolarSystem.Race>(Universe.SolarSystem.Race.values(),null,
			new Gui.ListMenuItems.ExternFunction<Universe.SolarSystem.Race>() {
				@Override public void setResult(Race value) {
					if (clickedNode instanceof SolarSystemNode) {
						((SolarSystemNode)clickedNode).value.race = value;
						treeModel.nodeChanged(clickedNode);
						if (selectedNode==clickedNode) selectionChanged();
						GameInfos.saveUniverseObjectDataToFile(data.universe);
					}
				}
				@Override public void configureMenuItem(JCheckBoxMenuItem menuItem, Race value) {
					if (value==null) {
						menuItem.setIcon(null);
						menuItem.setText("<none>");
					} else {
						menuItem.setIcon(RaceIcons.get(value));
						menuItem.setText(value.fullName);
					}
				}
			}
		);
		miSetRace.addTo(contextMenu_SolarSystem);
		
		contextMenu_SolarSystem.addSeparator();
		miSetStarClass = new Gui.ListMenuItems<Universe.SolarSystem.StarClass>(Universe.SolarSystem.StarClass.values(),null,
			new Gui.ListMenuItems.ExternFunction<Universe.SolarSystem.StarClass>() {
				@Override public void setResult(StarClass value) {
					if (clickedNode instanceof SolarSystemNode) {
						((SolarSystemNode)clickedNode).value.starClass = value;
						treeModel.nodeChanged(clickedNode);
						if (selectedNode==clickedNode) selectionChanged();
						GameInfos.saveUniverseObjectDataToFile(data.universe);
					}
				}
				@Override public void configureMenuItem(JCheckBoxMenuItem menuItem, StarClass value) {
					if (value==null) {
						menuItem.setIcon(null);
						menuItem.setText("<none>");
					} else {
						menuItem.setIcon(StarClassIcons.get(value));
						menuItem.setText(value.toString()+" Class");
					}
				}
			}
		);
		miSetStarClass.addTo(contextMenu_SolarSystem);
		
		contextMenu_SolarSystem.addSeparator();
		contextMenu_SolarSystem.add(createMenuItem("Find universe object",UniverseTreeActionCommand.FindObject));
		contextMenu_SolarSystem.addSeparator();
		contextMenu_SolarSystem.add(createMenuItem("Expand complete tree",UniverseTreeActionCommand.ExpandAll));
		contextMenu_SolarSystem.add(createMenuItem("Collapse remaining tree",UniverseTreeActionCommand.CollapseRemainingTree));
		
		contextMenu_Named = new JPopupMenu("Planet");
		contextMenu_Named.add(miSetName_Named = createMenuItem("Change Name",UniverseTreeActionCommand.SetName));
		contextMenu_Named.addSeparator();
		contextMenu_Named.add(createMenuItem("Find universe object",UniverseTreeActionCommand.FindObject));
		contextMenu_Named.addSeparator();
		contextMenu_Named.add(createMenuItem("Expand complete tree",UniverseTreeActionCommand.ExpandAll));
		contextMenu_Named.add(createMenuItem("Collapse remaining tree",UniverseTreeActionCommand.CollapseRemainingTree));
		
		textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setBorder(BorderFactory.createEtchedBorder());
		
//			extraInfoTableModel = new ExtraInfoTableModel();
		extraInfoTable = new SimplifiedTable("ExtraInfoTable",true,SaveViewer.DEBUG,true);
		extraInfoTable.setPreferredScrollableViewportSize(new Dimension(610, 120));;
//			extraInfoTableModel.setTable(extraInfoTable);
		
		portalGlyphs = new JLabel();
		portalGlyphs.setPreferredSize(new Dimension(50*12+10, 45*1+10));
		portalGlyphs.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(3, 3, 3, 3)));
		
		JPanel infoPanel = new JPanel(new BorderLayout(3,3));
		infoPanel.add(new JScrollPane(textArea),BorderLayout.CENTER);
		infoPanel.add(new JScrollPane(extraInfoTable),BorderLayout.SOUTH);
		
		JPanel eastPanel = new JPanel(new BorderLayout(3, 3));
		//eastPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(3, 3, 3, 3)));
		eastPanel.add(portalGlyphs,BorderLayout.NORTH);
		eastPanel.add(infoPanel,BorderLayout.CENTER);
		
		add(treeScrollPane,BorderLayout.CENTER);
		add(eastPanel,BorderLayout.EAST);
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
				case Planet     : planet = ((     PlanetNode)clickedNode).value; setNameForUniverseAddress(planet.getUniverseAddress(),planet, "planet"      ); break;
				case SolarSystem: system = ((SolarSystemNode)clickedNode).value; setNameForUniverseAddress(system.getUniverseAddress(),system, "solar system"); break;
				case Region     : region = ((     RegionNode)clickedNode).value; setNameForUniverseAddress(region.getUniverseAddress(),region, "region"      ); break;
				default:break;
				}
				treeModel.nodeChanged(clickedNode);
				if (selectedNode==clickedNode) selectionChanged();
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
			
		case FindObject:
			FindObjectDialog dialog = new FindObjectDialog(mainWindow,data.universe);
			dialog.showDialog();
			UniverseObject[] changedObjects = dialog.getChangedObjects();
			show(changedObjects);
			updateChangedObjects(treeRoot,changedObjects);
			//tree.repaint();
			break;
		}
		clickedNode = null;
		clickedTreePath = null;
	}

	private void show(UniverseObject[] changedObjects) {
		System.out.println("Changed Objects:");
		for (UniverseObject obj:changedObjects) {
			System.out.println("   "+obj.toString());
		}
	}

	private void updateChangedObjects(LocalTreeNode node, UniverseObject[] changedObjects) {
		for (LocalTreeNode child:node.getChildren()) {
			if (child instanceof GenericTreeNode<?>) {
				Object value = ((GenericTreeNode<?>)child).value;
				for (UniverseObject obj:changedObjects)
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
			selectionChanged();
		}
	}
	
	private JPopupMenu mouseRightClicked() {
		if (clickedNode==null)
			return contextMenu_Other;
		
		switch(clickedNode.type) {
		case SolarSystem:
			SolarSystem system = ((SolarSystemNode)clickedNode).value;
			miSetName_SolarSystem.setText(system.hasOriginalName()?"Change name":"Set name");
			miSetRace.setValue(system.race);
			miSetStarClass.setValue(system.starClass);
			return contextMenu_SolarSystem;
			
		case Region:
			miSetName_Named.setText(((RegionNode)clickedNode).value.hasName()?"Change name":"Set name");
			return contextMenu_Named;
			
		case Planet:
			miSetName_Named.setText(((PlanetNode)clickedNode).value.hasOriginalName()?"Change name":"Set name");
			return contextMenu_Named;
			
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

	private void selectionChanged() {
		textArea.setText("");
		
		if (selectedNode==null) {
			extraInfoTable.setModel(new DefaultTableModel());
//				extraInfoTableModel.clearData();
			return;
		}
		
		if (selectedNode.type!=NodeType.Planet) {
			portalGlyphs.setIcon(null);
			extraInfoTable.setModel(new DefaultTableModel());
//				extraInfoTableModel.clearData();
		}
		
		int n;
		UniverseAddress ua;
		Planet planet;
		SolarSystem system;
		long portalGlyphCode;
		double distance_reg;
		
		switch(selectedNode.type) {
		case Planet:
			planet = ((PlanetNode)selectedNode).value;
			ua = planet.getUniverseAddress();
			portalGlyphCode = ua.getPortalGlyphCode();
			portalGlyphs.setIcon(createPortalGlyphs(portalGlyphCode));
			
			textArea.append(String.format("Universe Coordinates       : %s\r\n", ua.getCoordinates()));
			textArea.append(String.format("Universe Address           : 0x%014X\r\n", ua.getAddress()));
			textArea.append(String.format("Portal Glyph Code          : %012X\r\n", portalGlyphCode));
			textArea.append(String.format("Extended SignalBoster Code : %s\r\n", ua.getExtendedSigBoostCode()));
			
			showDiscNameObj(planet);
		break;
			
		case SolarSystem:
			n = selectedNode.getDataChildrenCount();
			textArea.append(String.format("%d known planet%s\r\n", n, n>1?"s":""));
			system = ((SolarSystemNode)selectedNode).value;
			ua = system.getUniverseAddress();
			textArea.append(String.format("Universe Coordinates : %s\r\n", ua.getSolarSystemCoordinates()));
			textArea.append(String.format("SignalBoster Code    : %s\r\n", ua.getSigBoostCode()));
			if (system.race     !=null) textArea.append(String.format("Dominant Race        : %s\r\n", system.race.fullName));
			if (system.starClass!=null) textArea.append(String.format("Star Class           : %s\r\n", system.starClass));
			
			distance_reg = ua.getDistToOther_inRegionUnits(data.general.currentUniverseAddress);
			textArea.append("\r\n");
			textArea.append(                                 "Distance to current position:\r\n");
			textArea.append(    String.format(Locale.ENGLISH,"    computed: %1.2f Regions = %1.1f ly\r\n", distance_reg, distance_reg*400));
			
			distance_reg = ua.getDistToCenter_inRegionUnits();
			textArea.append("\r\n");
			textArea.append(                                 "Distance to Galaxy Center :\r\n");
			textArea.append(    String.format(Locale.ENGLISH,"    computed: %1.2f Regions = %1.1f ly\r\n", distance_reg, distance_reg*400));
			if (system.distanceToCenter!=null) {
				double distance_LY = system.distanceToCenter.doubleValue();
				textArea.append(String.format(Locale.ENGLISH,"    measured: %1.1f ly\r\n", distance_LY));
				textArea.append(String.format(Locale.ENGLISH,"    -> Region size: %1.2f ly\r\n", distance_LY/distance_reg));
			}
			
			showDiscNameObj(system);
		break;
			
		case Region:
			n = selectedNode.getDataChildrenCount();
			textArea.append(String.format("%d known solar system%s\r\n", n, n>1?"s":""));
			ua = ((RegionNode)selectedNode).value.getUniverseAddress();
			textArea.append(String.format("Universe Coordinates      : %s\r\n", ua.getRegionCoordinates()));
			textArea.append(String.format("Reduced SignalBoster Code : %s\r\n", ua.getReducedSigBoostCode()));
			
			distance_reg = ua.getDistToCenter_inRegionUnits();
			textArea.append("\r\n");
			textArea.append(                             "Distance to Galaxy Center :\r\n");
			textArea.append(String.format(Locale.ENGLISH,"    computed: %1.2f Regions = %1.1f ly\r\n", distance_reg, distance_reg*400));
		break;
			
		case Galaxy:
			n = selectedNode.getDataChildrenCount();
			textArea.append(String.format("%d known region%s\r\n", n, n>1?"s":""));
		break;
			
		case Universe:
			n = selectedNode.getDataChildrenCount();
			textArea.append(String.format("%d known galax%s\r\n", n, n>1?"ies":"y"));
		break;
		}
	}
	
	private void showDiscNameObj(Universe.UniverseObject obj) {
		textArea.append("\r\n");
		
		//textArea.append(String.format("selected : %s\r\n\r\n", obj.isSelected));
		
		if (obj.hasOriginalName()) textArea.append(String.format("Original Name : %s\r\n", obj.getOriginalName()));
		if (obj.hasUploadedName()) textArea.append(String.format("Uploaded Name : %s\r\n", obj.getUploadedName()));
		if (obj.hasDiscoverer  ()) textArea.append(String.format("Discovered by : %s\r\n", obj.getDiscoverer()));
		if (!obj.discoveredItems_Avail.isEmpty() || !obj.discoveredItems_Store.isEmpty()) {
			textArea.append("Discovered Items:\r\n");
			if (!obj.discoveredItems_Avail.isEmpty()) {
				textArea.append("   available:\r\n");
				for (String item:new TreeSet<>(obj.discoveredItems_Avail.keySet()))
					textArea.append(String.format("      %s: %d\r\n", item, obj.discoveredItems_Avail.get(item)));
			}
			if (!obj.discoveredItems_Store.isEmpty()) {
				textArea.append("   stored:\r\n");
				for (String item:new TreeSet<>(obj.discoveredItems_Store.keySet()))
					textArea.append(String.format("      %s: %d\r\n", item, obj.discoveredItems_Store.get(item)));
			}
		}
		extraInfoTable.setModel(new ExtraInfoTableModel(obj instanceof Planet, obj.extraInfos));
//			extraInfoTableModel.setData(obj.extraInfos);
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
			buttonPanel.add(SaveViewer.createButton("Cancel", e->closeDialog()));
			
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

		public UniverseObject[] getChangedObjects() {
			if (tableModel!=null) return tableModel.changedObj.toArray(new UniverseObject[0]);
			return new UniverseObject[0];
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
			private HashSet<UniverseObject> changedObj;
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
				
				for (UniverseObject obj:   selectedObjs) if (!obj.isSelected) { changedObj.add(obj); obj.isSelected = true; }
				for (UniverseObject obj:notSelectedObjs) if ( obj.isSelected) { changedObj.add(obj); obj.isSelected = false; }
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
					for (UniverseObject obj:data[rowIndex].sourceObjs) changedObj.add(obj);
					GameInfos.saveUniverseObjectDataToFile(universe);
					break;
					
				case Info    :
					data[rowIndex].info = (String)aValue;
					for (     ExtraInfo  ei:data[rowIndex].sourceEIs ) ei.info = (String)aValue;
					for (UniverseObject obj:data[rowIndex].sourceObjs) changedObj.add(obj);
					GameInfos.saveUniverseObjectDataToFile(universe);
					break;
				}
			}
			
		}
		
	}
	
	private enum ExtraInfoColumnID implements TableView.SimplifiedColumnIDInterface {
		ShowInParent("", Boolean.class, 10,-1, 20, 20),
		Label("Label", String.class, 20,-1, 50, 50),
		Info ("Info" , String.class, 50,-1,500,500);
		
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

		protected ExtraInfoTableModel(boolean isPlanet, Vector<ExtraInfo> tableData) {
			super(isPlanet?
					new ExtraInfoColumnID[]{ExtraInfoColumnID.ShowInParent,ExtraInfoColumnID.Label,ExtraInfoColumnID.Info}:
					new ExtraInfoColumnID[]{ExtraInfoColumnID.Label,ExtraInfoColumnID.Info}
				);
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
			treeModel.nodeChanged(selectedNode);
			if (isPlanet) treeModel.nodeChanged(selectedNode.parent);
			GameInfos.saveUniverseObjectDataToFile(data.universe);
		}
	}
	
	static class UniverseTreeCellRenderer extends DefaultTreeCellRenderer {

		private static final Color TEXTCOLOR__SELECTED     = Color.RED;
		private static final Color TEXTCOLOR__CURRENT_POS  = new Color(0x2EA000);
		private static final Color TEXTCOLOR__WITHOUT_NAME = new Color(0x808080);
		private static final Color TEXTCOLOR__NOT_UPLOADED  = new Color(0x0000FF); // or 0x1D67AE

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
			if (value instanceof GenericTreeNode<?>) {
				GenericTreeNode<?> node = (GenericTreeNode<?>)value;
				switch (node.type) {
				case Universe   : setIcon(UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.Universe   )); break;
				case Galaxy     : setIcon(UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.Galaxy     )); break;
				case Region     : setIcon(UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.Region     )); break;
				case SolarSystem:
					if (node instanceof SolarSystemNode) {
						SolarSystem system = ((SolarSystemNode)node).value;
						setIcon(UniverseTreeSolarSystemIcons.get(system.race, system.starClass));
					}
					break;
				case Planet     : setIcon(UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.Planet     )); break;
				}
				setFont(standardFont);
				Universe.UniverseObject obj = null;
				Region region = null;
				if (node instanceof      RegionNode) region = ((  RegionNode)node).value;
				if (node instanceof SolarSystemNode) obj = ((SolarSystemNode)node).value;
				if (node instanceof      PlanetNode) obj = ((     PlanetNode)node).value;
				if (obj != null) {
					if (!obj.hasOriginalName() && !obj.hasUploadedName()) {
						if (!selected) setForeground(TEXTCOLOR__WITHOUT_NAME);
					}
					if (obj.isNotUploaded) {
						if (!selected) setForeground(TEXTCOLOR__NOT_UPLOADED);
					}
					if (obj.isCurrPos) {
						if (!selected) setForeground(TEXTCOLOR__CURRENT_POS);
						setFont(boldfont);
					}
					if (obj.isSelected) {
						if (!selected) setForeground(TEXTCOLOR__SELECTED);
						setFont(boldfont);
					}
				}
				if (region!=null) {
					if (!region.hasName()) {
						if (!selected) setForeground(TEXTCOLOR__WITHOUT_NAME);
					}
				}
			}
			return component;
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

		@Override
		void createChildren() {
			children = new LocalTreeNode[getDataChildrenCount()];
			for (int i=0; i<children.length; ++i)
				children[i] = createTreeChild(i);
		}
		
	}
	
	static abstract class GenericTreeNode<V> extends LocalTreeNode {
		
		NodeType type;
		V value;

		protected GenericTreeNode(LocalTreeNode parent, NodeType type, V value) {
			super(parent);
			this.value = value;
			this.type = type;
		}

		@Override public boolean getAllowsChildren() { return true; /*except Planet*/ }
		@Override protected String getLabel() { return value.toString(); }
	}
	
	static class UniverseNode extends GenericTreeNode<Universe> {
		private UniverseNode(Universe value) { super(null, NodeType.Universe, value); }
		@Override protected int getDataChildrenCount() { return value.galaxies.size(); }
		@Override protected LocalTreeNode createTreeChild(int i) { return new GalaxyNode(this,value.galaxies.get(i)); }
	}
	static class GalaxyNode extends GenericTreeNode<Galaxy> {
		private GalaxyNode(UniverseNode parent, Galaxy value) { super(parent, NodeType.Galaxy, value); }
		@Override protected int getDataChildrenCount() { return value.regions.size(); }
		@Override protected LocalTreeNode createTreeChild(int i) { return new RegionNode(this,value.regions.get(i)); }
	}
	static class RegionNode extends GenericTreeNode<Region> {
		private RegionNode(GalaxyNode parent, Region value) { super(parent, NodeType.Region, value); }
		@Override protected int getDataChildrenCount() { return value.solarSystems.size(); }
		@Override protected LocalTreeNode createTreeChild(int i) { return new SolarSystemNode(this,value.solarSystems.get(i)); }
	}
	static class SolarSystemNode extends GenericTreeNode<SolarSystem> {
		private SolarSystemNode(RegionNode parent, SolarSystem value) { super(parent, NodeType.SolarSystem, value); }
		@Override protected int getDataChildrenCount() { return value.planets.size(); }
		@Override protected LocalTreeNode createTreeChild(int i) { return new PlanetNode(this,value.planets.get(i)); }
		//@Override protected String getLabel() { return value.hasName()?value.getName():super.getLabel(); }
	}
	static class PlanetNode extends GenericTreeNode<Planet> {
		private PlanetNode(SolarSystemNode parent, Planet value) { super(parent, NodeType.Planet, value); }
		@Override protected int getDataChildrenCount() { return 0; }
		@Override protected LocalTreeNode createTreeChild(int i) { throw new UnsupportedOperationException("Can't create a TreeChild from a PlanetNode."); }
		//@Override protected String getLabel() { return value.hasName()?value.getName():super.getLabel(); }
		@Override public boolean getAllowsChildren() { return false; }
	}
}
