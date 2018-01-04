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
import java.util.HashSet;
import java.util.Locale;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
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
import net.schwarzbaer.gui.IconSource.IndexOnlyIconSource;
import net.schwarzbaer.gui.StandardDialog;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos;
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
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.SaveGameView.EnumCheckBoxMenuItem;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.SaveGameView.SaveGameViewTabPanel;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.SimplifiedColumnConfig;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.SimplifiedTable;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TreeView.AbstractTreeNode;

public class UniversePanel extends SaveGameViewTabPanel implements ActionListener {
		private static final long serialVersionUID = -4594889224613582352L;
		
		enum UniverseTreeActionCommand { SetName, SetDistance, SetRace, SetStarClass, ExpandAll, CollapseRemainingTree, FindObject }
		
		public static final IndexOnlyIconSource PortalGlyphsIS  = new IconSource.IndexOnlyIconSource( 50,45,4);
		
		enum UniverseTreeIcons { Universe, Galaxy, Region, SolarSystem, Planet, GekSys, KorvaxSys, VykeenSys, Yellow, Red, Green, Blue }
		private static final int TreeIconHeight = 20;
		static final IconSource<UniversePanel.UniverseTreeIcons> UniverseTreeIconsIS = new IconSource<UniversePanel.UniverseTreeIcons>(30,TreeIconHeight){
			@Override protected int getIconIndexInImage(UniversePanel.UniverseTreeIcons key) {
				if (key!=null) return key.ordinal();
			 	throw new IllegalArgumentException("Unknown icon key: "+key);
			}
		};
		
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
		
		private static UniversePanel.UniverseTreeSolarSystemIconsMap UniverseTreeSolarSystemIcons = null;

		public static void prepareIconSources() {
			PortalGlyphsIS.readIconsFromResource("/images/PortalGlyphs.50.45.png");
			PortalGlyphsIS.cacheImages(16);
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
			
			UniverseTreeIconsIS.readIconsFromResource("/images/UniverseTreeIcons.png");
			UniverseTreeIconsIS.cacheIcons(UniverseTreeIcons.values());
			
			Icon icon = UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.Galaxy);
			icon = IconSource.cutIcon(icon,5,0,20,20);
			UniverseTreeIconsIS.setCachedIcon(UniverseTreeIcons.Galaxy,icon);
			
			UniverseTreeSolarSystemIcons = new UniverseTreeSolarSystemIconsMap();
			UniverseTreeSolarSystemIcons.createValues();
		}

		private JTree tree;
		private DefaultTreeModel treeModel;
		private UniversePanel.UniverseNode treeRoot;
		
		private UniversePanel.GenericTreeNode<?> selectedNode;
		private UniversePanel.GenericTreeNode<?> clickedNode;
		private TreePath clickedTreePath;
		
		private JLabel portalGlyphs;
		private JTextArea textArea;
//		private ExtraInfoTableModel extraInfoTableModel;
		private SimplifiedTable extraInfoTable;
		
		private JPopupMenu contextMenu_Other;
		private JPopupMenu contextMenu_SolarSystem;
		private JPopupMenu contextMenu_Planet;
		private JMenuItem miSetName_SolarSystem;
		private JMenuItem miSetName_Planet;
		private UniversePanel.EnumCheckBoxMenuItem_StarClass[] miSetStarClassArr;
		private UniversePanel.EnumCheckBoxMenuItem_Race[] miSetRaceArr;
		private ButtonGroup bgSetStarClass;
		private ButtonGroup bgSetRace;
		
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
			bgSetRace = new ButtonGroup();
			Race[] races = Universe.SolarSystem.Race.values();
			miSetRaceArr = new UniversePanel.EnumCheckBoxMenuItem_Race[races.length];
			for (int i=0; i<races.length; ++i) {
				miSetRaceArr[i] = new EnumCheckBoxMenuItem_Race(races[i].fullName,UniverseTreeActionCommand.SetRace,races[i],false,this);
				contextMenu_SolarSystem.add(miSetRaceArr[i]);
				bgSetRace.add(miSetRaceArr[i]);
			}
			
			contextMenu_SolarSystem.addSeparator();
			bgSetStarClass = new ButtonGroup();
			StarClass[] starClasses = Universe.SolarSystem.StarClass.values();
			miSetStarClassArr = new UniversePanel.EnumCheckBoxMenuItem_StarClass[starClasses.length];
			for (int i=0; i<starClasses.length; ++i) {
				miSetStarClassArr[i] = new EnumCheckBoxMenuItem_StarClass(starClasses[i].toString()+" Class",UniverseTreeActionCommand.SetStarClass,starClasses[i],false,this);
				contextMenu_SolarSystem.add(miSetStarClassArr[i]);
				bgSetStarClass.add(miSetStarClassArr[i]);
			}
			
			contextMenu_SolarSystem.addSeparator();
			contextMenu_SolarSystem.add(createMenuItem("Find universe object",UniverseTreeActionCommand.FindObject));
			contextMenu_SolarSystem.addSeparator();
			contextMenu_SolarSystem.add(createMenuItem("Expand complete tree",UniverseTreeActionCommand.ExpandAll));
			contextMenu_SolarSystem.add(createMenuItem("Collapse remaining tree",UniverseTreeActionCommand.CollapseRemainingTree));
			
			contextMenu_Planet = new JPopupMenu("Planet");
			contextMenu_Planet.add(miSetName_Planet = createMenuItem("Change Name",UniverseTreeActionCommand.SetName));
			contextMenu_Planet.addSeparator();
			contextMenu_Planet.add(createMenuItem("Find universe object",UniverseTreeActionCommand.FindObject));
			contextMenu_Planet.addSeparator();
			contextMenu_Planet.add(createMenuItem("Expand complete tree",UniverseTreeActionCommand.ExpandAll));
			contextMenu_Planet.add(createMenuItem("Collapse remaining tree",UniverseTreeActionCommand.CollapseRemainingTree));
			
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

		private JMenuItem createMenuItem(String label, UniversePanel.UniverseTreeActionCommand actionCommand) {
			JMenuItem menuItem = new JMenuItem(label);
			menuItem.addActionListener(this);
			menuItem.setActionCommand(actionCommand.toString());
			return menuItem;
		}
		
		private static class EnumCheckBoxMenuItem_Race extends EnumCheckBoxMenuItem<Race,UniversePanel.UniverseTreeActionCommand> {
			private static final long serialVersionUID = 8764557520719990435L;
			public EnumCheckBoxMenuItem_Race(String label, UniversePanel.UniverseTreeActionCommand actionCommand, Race key, boolean selected, ActionListener actionListener) {
				super(label, actionCommand, key, selected, actionListener);
			}
		}
		
		private static class EnumCheckBoxMenuItem_StarClass extends EnumCheckBoxMenuItem<StarClass,UniversePanel.UniverseTreeActionCommand> {
			private static final long serialVersionUID = -3616686445304535043L;
			public EnumCheckBoxMenuItem_StarClass(String label, UniversePanel.UniverseTreeActionCommand actionCommand, StarClass key, boolean selected, ActionListener actionListener) {
				super(label, actionCommand, key, selected, actionListener);
			}
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
		
		@Override
		public void actionPerformed(ActionEvent e) {
			UniversePanel.UniverseTreeActionCommand actionCommand = UniverseTreeActionCommand.valueOf(e.getActionCommand());
			Object source = e.getSource();
			switch(actionCommand) {
			case SetName:
				if (clickedNode!=null) {
					Planet planet; SolarSystem system;
					switch(clickedNode.type) {
					case Planet     : planet = ((     UniversePanel.PlanetNode)clickedNode).value; setNameForUniverseAddress(planet.getUniverseAddress(),planet, "planet"      ); break;
					case SolarSystem: system = ((UniversePanel.SolarSystemNode)clickedNode).value; setNameForUniverseAddress(system.getUniverseAddress(),system, "solar system"); break;
					default:break;
					}
					treeModel.nodeChanged(clickedNode);
					if (selectedNode==clickedNode) selectionChanged();
				}
				break;
				
			case SetDistance:
				if (clickedNode instanceof UniversePanel.SolarSystemNode) {
					SolarSystem system=((UniversePanel.SolarSystemNode)clickedNode).value;
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
				
			case SetRace:
				if (source instanceof UniversePanel.EnumCheckBoxMenuItem_Race && clickedNode instanceof UniversePanel.SolarSystemNode) {
					((UniversePanel.SolarSystemNode)clickedNode).value.race = ((UniversePanel.EnumCheckBoxMenuItem_Race)source).key;
					treeModel.nodeChanged(clickedNode);
					if (selectedNode==clickedNode) selectionChanged();
					GameInfos.saveUniverseObjectDataToFile(data.universe);
				}
				break;
			case SetStarClass:
				if (source instanceof UniversePanel.EnumCheckBoxMenuItem_StarClass && clickedNode instanceof UniversePanel.SolarSystemNode) {
					((UniversePanel.SolarSystemNode)clickedNode).value.starClass = ((UniversePanel.EnumCheckBoxMenuItem_StarClass)source).key;
					treeModel.nodeChanged(clickedNode);
					if (selectedNode==clickedNode) selectionChanged();
					GameInfos.saveUniverseObjectDataToFile(data.universe);
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
				UniversePanel.FindObjectDialog dialog = new FindObjectDialog(mainWindow,data.universe);
				dialog.showDialog();
				updateChangedObjects(treeRoot,dialog.getChangedObjects());
				//tree.repaint();
				break;
			}
			clickedNode = null;
			clickedTreePath = null;
		}

		private void updateChangedObjects(UniversePanel.LocalTreeNode node, UniverseObject[] changedObjects) {
			for (UniversePanel.LocalTreeNode child:node.getChildren()) {
				if (child instanceof UniversePanel.GenericTreeNode<?>) {
					UniversePanel.GenericTreeNode<?> genericNode = (UniversePanel.GenericTreeNode<?>)child;
					for (UniverseObject obj:changedObjects)
						if (genericNode.value.equals(obj)) {
							treeModel.nodeChanged(genericNode);
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
			@Override public void mousePressed(MouseEvent e) {}
			@Override public void mouseReleased(MouseEvent e) {}
			@Override public void mouseEntered(MouseEvent e) {}
			@Override public void mouseExited(MouseEvent e) {}
			@Override public void mouseClicked(MouseEvent e) {
				if (e.getButton()==MouseEvent.BUTTON3) {
					//System.out.println("mouseClicked( BUTTON3, "+e.getX()+", "+e.getY()+" )");
					clickedTreePath = tree.getPathForLocation(e.getX(), e.getY());
					clickedNode = null;
					if (clickedTreePath!=null) {
						Object object = clickedTreePath.getLastPathComponent();
						if (object instanceof UniversePanel.GenericTreeNode<?>)
							clickedNode = (UniversePanel.GenericTreeNode<?>)object;
					}
					JPopupMenu contextMenu = UniversePanel.this.mouseClicked();
					contextMenu.show(tree, e.getX(), e.getY());
				}
			}

			@Override
			public void valueChanged(TreeSelectionEvent e) {
				Object comp = e.getPath().getLastPathComponent();
				if (!(comp instanceof UniversePanel.GenericTreeNode<?>))
					selectedNode = null;
				else
					selectedNode = (UniversePanel.GenericTreeNode<?>)comp;
				selectionChanged();
			}
		}
		
		private JPopupMenu mouseClicked() {
			if (clickedNode==null)
				return contextMenu_Other;
			
			switch(clickedNode.type) {
			case SolarSystem:
				SolarSystem system = ((UniversePanel.SolarSystemNode)clickedNode).value;
				miSetName_SolarSystem.setText(system.hasOriginalName()?"Change name":"Set name");
				EnumCheckBoxMenuItem.setCheckBoxMenuItems(miSetRaceArr     , system.race     , Race     .values(), bgSetRace     );
				EnumCheckBoxMenuItem.setCheckBoxMenuItems(miSetStarClassArr, system.starClass, StarClass.values(), bgSetStarClass);
				return contextMenu_SolarSystem;
				
			case Planet:
				miSetName_Planet.setText(((UniversePanel.PlanetNode)clickedNode).value.hasOriginalName()?"Change name":"Set name");
				return contextMenu_Planet;
				
			default:
				return contextMenu_Other;
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
				planet = ((UniversePanel.PlanetNode)selectedNode).value;
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
				system = ((UniversePanel.SolarSystemNode)selectedNode).value;
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
				ua = ((UniversePanel.RegionNode)selectedNode).value.getUniverseAddress();
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
				BufferedImage glyph = PortalGlyphsIS.getImage(nr);
				graphics.drawImage(glyph, i*50, 0, null);
			}
			return new ImageIcon(image);
		}
		
		private static class FindObjectDialog extends StandardDialog {
			private static final long serialVersionUID = -356863578675221086L;
			
			private Universe universe;
			private SimplifiedTable table;
			private FindObjectDialog.FoundExtraInfoTableModel tableModel;

			public FindObjectDialog(Window parent, Universe universe) {
				super(parent, "Find Universe Object", ModalityType.APPLICATION_MODAL);
				this.universe = universe;
				
				table = new SimplifiedTable("FindObjectDialog",false,SaveViewer.DEBUG,true);
				table.setPreferredScrollableViewportSize(new Dimension(500, 600));;
				tableModel = null;
				
				JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
				JCheckBox chkbx;
				buttonPanel.add(chkbx = SaveViewer.createCheckbox("Allow Editing", null, false));
				buttonPanel.add(SaveViewer.createButton("Show Selected in Tree", e->{ if (tableModel!=null) tableModel.setSelected(); closeDialog(); }));
				buttonPanel.add(SaveViewer.createButton("Cancel", e->closeDialog()));
				
				chkbx.addActionListener(e->{ if (tableModel!=null) { tableModel.allowEditing(chkbx.isSelected()); table.setModel(tableModel); } });
				
				JPanel contentPane = new JPanel(new BorderLayout(3,3));
				contentPane.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
				contentPane.add(new JScrollPane(table),BorderLayout.CENTER);
				contentPane.add(buttonPanel,BorderLayout.SOUTH);
				
				createGUI( contentPane );
				setSizeAsMinSize();
			}

			public UniverseObject[] getChangedObjects() {
				if (tableModel!=null) return tableModel.changedObj.toArray(new UniverseObject[0]);
				return new UniverseObject[0];
			}

			@Override
			public void showDialog() {
				HashSet<FindObjectDialog.FoundExtraInfo> infos = new HashSet<>();
				
				for (Galaxy g:universe.galaxies)
					for (Region r:g.regions)
						for (SolarSystem s:r.solarSystems) {
							addInfos(infos,s,false);
							for (Planet p:s.planets)
								addInfos(infos,p,true);
						}
				
				tableModel = new FoundExtraInfoTableModel(universe, infos.toArray(new FindObjectDialog.FoundExtraInfo[0]));
				table.setModel(tableModel);
				
				super.showDialog();
			}

			private void addInfos(HashSet<FindObjectDialog.FoundExtraInfo> infos, UniverseObject obj, boolean isPanet) {
				for (ExtraInfo ei:obj.extraInfos) {
					FindObjectDialog.FoundExtraInfo existingFEI = null;
					for (FindObjectDialog.FoundExtraInfo fei:infos) {
						if (fei.fromPlanet != isPanet) continue;
						if (!fei.label.equals(ei.shortLabel)) continue;
						if (!fei.info.equals(ei.info)) continue;
						existingFEI = fei;
						break;
					}
					if (existingFEI!=null) existingFEI.add(obj,ei);
					else infos.add(new FoundExtraInfo(isPanet,obj,ei));
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
			
			private static class FoundExtraInfoTableModel extends TableView.SimplifiedTableModel<FindObjectDialog.FoundExtraInfoColumnID> {

				private FindObjectDialog.FoundExtraInfo[] data;
				private HashSet<UniverseObject> changedObj;
				private boolean allowEditing;
				private Universe universe;

				protected FoundExtraInfoTableModel(Universe universe, FindObjectDialog.FoundExtraInfo[] data) {
					super(FoundExtraInfoColumnID.values());
					this.universe = universe;
					this.data = data;
					this.changedObj = new HashSet<>();
					this.allowEditing = false;
				}

				public void allowEditing(boolean allowEditing) {
					this.allowEditing = allowEditing;
				}

				public void setSelected() {
					//changedObj.clear();
					for (FindObjectDialog.FoundExtraInfo fei:data)
						for (UniverseObject obj:fei.sourceObjs) {
							if (obj.isSelected != fei.isSelected) changedObj.add(obj);
							obj.isSelected = fei.isSelected;
						}
				}

				@Override public int getRowCount() { return data.length; }

				@Override
				public Object getValueAt(int rowIndex, int columnIndex, FindObjectDialog.FoundExtraInfoColumnID columnID) {
					switch(columnID) {
					case Selected: return data[rowIndex].isSelected;
					case Source  : int n = data[rowIndex].sourceObjs.size(); return String.format("%d %s%s", n, data[rowIndex].fromPlanet?"Planet":"Solar System", n==1?"":"s");
					case Label   : return data[rowIndex].label;
					case Info    : return data[rowIndex].info;
					}
					return null;
				}

				@Override
				protected boolean isCellEditable(int rowIndex, int columnIndex, FindObjectDialog.FoundExtraInfoColumnID columnID) {
					switch(columnID) {
					case Selected: return true;
					case Source  : return false;
					case Label   : return allowEditing;
					case Info    : return allowEditing;
					}
					return false;
				}

				@Override
				protected void setValueAt(Object aValue, int rowIndex, int columnIndex, FindObjectDialog.FoundExtraInfoColumnID columnID) {
					switch(columnID) {
					case Selected: data[rowIndex].isSelected = (Boolean)aValue; break;
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
		private class ExtraInfoTableModel extends TableView.SimplifiedTableModel<UniversePanel.ExtraInfoColumnID> {

			private Vector<ExtraInfo> tableData;
//			private JTable table;
			private boolean isPlanet;

			protected ExtraInfoTableModel(boolean isPlanet, Vector<ExtraInfo> tableData) {
				super(isPlanet?
						new UniversePanel.ExtraInfoColumnID[]{ExtraInfoColumnID.ShowInParent,ExtraInfoColumnID.Label,ExtraInfoColumnID.Info}:
						new UniversePanel.ExtraInfoColumnID[]{ExtraInfoColumnID.Label,ExtraInfoColumnID.Info}
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
			public Object getValueAt(int rowIndex, int columnIndex, UniversePanel.ExtraInfoColumnID columnID) {
				if (tableData==null) return null;
				switch(columnID) {
				case ShowInParent: if (rowIndex==tableData.size()) return false; return tableData.get(rowIndex).showInParent;
				case Label       : if (rowIndex==tableData.size()) return "";    return tableData.get(rowIndex).shortLabel;
				case Info        : if (rowIndex==tableData.size()) return "";    return tableData.get(rowIndex).info;
				}
				return null;
			}

			@Override
			protected boolean isCellEditable(int rowIndex, int columnIndex, UniversePanel.ExtraInfoColumnID columnID) {
				return true;
			}

			@Override
			protected void setValueAt(Object aValue, int rowIndex, int columnIndex, UniversePanel.ExtraInfoColumnID columnID) {
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
				if (value instanceof UniversePanel.GenericTreeNode<?>) {
					UniversePanel.GenericTreeNode<?> node = (UniversePanel.GenericTreeNode<?>)value;
					switch (node.type) {
					case Universe   : setIcon(UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.Universe   )); break;
					case Galaxy     : setIcon(UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.Galaxy     )); break;
					case Region     : setIcon(UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.Region     )); break;
					case SolarSystem:
						if (node instanceof UniversePanel.SolarSystemNode) {
							SolarSystem system = ((UniversePanel.SolarSystemNode)node).value;
							setIcon(UniverseTreeSolarSystemIcons.get(system.race, system.starClass));
						}
						break;
					case Planet     : setIcon(UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.Planet     )); break;
					}
					setFont(standardFont);
					Universe.UniverseObject obj = null;
					if (node instanceof UniversePanel.SolarSystemNode) obj = ((UniversePanel.SolarSystemNode)node).value;
					if (node instanceof      UniversePanel.PlanetNode) obj = ((     UniversePanel.PlanetNode)node).value;
					if (obj != null) {
						if (!selected && !obj.hasOriginalName() && !obj.hasUploadedName()) setForeground(TEXTCOLOR__WITHOUT_NAME);
						if (!selected && obj.isNotUploaded) setForeground(TEXTCOLOR__NOT_UPLOADED);
						if (obj.isCurrPos) {
							if (!selected) setForeground(TEXTCOLOR__CURRENT_POS);
							setFont(boldfont);
						}
						if (obj.isSelected) {
							if (!selected) setForeground(TEXTCOLOR__SELECTED);
							setFont(boldfont);
						}
					}
				}
				return component;
			}
		}
		
		enum NodeType { Universe, Galaxy, Region, SolarSystem, Planet }
		
		static abstract class LocalTreeNode extends AbstractTreeNode<UniversePanel.LocalTreeNode> {
			
			protected LocalTreeNode(UniversePanel.LocalTreeNode parent) {
				super(parent);
			}

			@Override public String toString() { return getLabel(); }
			
			protected abstract String getLabel();
			protected abstract int getDataChildrenCount();
			protected abstract UniversePanel.LocalTreeNode createTreeChild(int i);

			@Override
			void createChildren() {
				children = new UniversePanel.LocalTreeNode[getDataChildrenCount()];
				for (int i=0; i<children.length; ++i)
					children[i] = createTreeChild(i);
			}
			
		}
		
		static abstract class GenericTreeNode<V> extends UniversePanel.LocalTreeNode {
			
			UniversePanel.NodeType type;
			V value;

			protected GenericTreeNode(UniversePanel.LocalTreeNode parent, UniversePanel.NodeType type, V value) {
				super(parent);
				this.value = value;
				this.type = type;
			}

			@Override public boolean getAllowsChildren() { return true; /*except Planet*/ }
			@Override protected String getLabel() { return value.toString(); }
		}
		
		static class UniverseNode extends UniversePanel.GenericTreeNode<Universe> {
			private UniverseNode(Universe value) { super(null, NodeType.Universe, value); }
			@Override protected int getDataChildrenCount() { return value.galaxies.size(); }
			@Override protected UniversePanel.LocalTreeNode createTreeChild(int i) { return new GalaxyNode(this,value.galaxies.get(i)); }
		}
		static class GalaxyNode extends UniversePanel.GenericTreeNode<Galaxy> {
			private GalaxyNode(UniversePanel.UniverseNode parent, Galaxy value) { super(parent, NodeType.Galaxy, value); }
			@Override protected int getDataChildrenCount() { return value.regions.size(); }
			@Override protected UniversePanel.LocalTreeNode createTreeChild(int i) { return new RegionNode(this,value.regions.get(i)); }
		}
		static class RegionNode extends UniversePanel.GenericTreeNode<Region> {
			private RegionNode(UniversePanel.GalaxyNode parent, Region value) { super(parent, NodeType.Region, value); }
			@Override protected int getDataChildrenCount() { return value.solarSystems.size(); }
			@Override protected UniversePanel.LocalTreeNode createTreeChild(int i) { return new SolarSystemNode(this,value.solarSystems.get(i)); }
		}
		static class SolarSystemNode extends UniversePanel.GenericTreeNode<SolarSystem> {
			private SolarSystemNode(UniversePanel.RegionNode parent, SolarSystem value) { super(parent, NodeType.SolarSystem, value); }
			@Override protected int getDataChildrenCount() { return value.planets.size(); }
			@Override protected UniversePanel.LocalTreeNode createTreeChild(int i) { return new PlanetNode(this,value.planets.get(i)); }
			//@Override protected String getLabel() { return value.hasName()?value.getName():super.getLabel(); }
		}
		static class PlanetNode extends UniversePanel.GenericTreeNode<Planet> {
			private PlanetNode(UniversePanel.SolarSystemNode parent, Planet value) { super(parent, NodeType.Planet, value); }
			@Override protected int getDataChildrenCount() { return 0; }
			@Override protected UniversePanel.LocalTreeNode createTreeChild(int i) { throw new UnsupportedOperationException("Can't create a TreeChild from a PlanetNode."); }
			//@Override protected String getLabel() { return value.hasName()?value.getName():super.getLabel(); }
			@Override public boolean getAllowsChildren() { return false; }
		}
	}