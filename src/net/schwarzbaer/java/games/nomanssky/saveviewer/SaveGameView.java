package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Locale;
import java.util.TreeSet;
import java.util.Vector;
import java.util.function.Function;

import javax.activation.DataHandler;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.schwarzbaer.gui.Canvas;
import net.schwarzbaer.gui.IconSource;
import net.schwarzbaer.gui.IconSource.IndexOnlyIconSource;
import net.schwarzbaer.gui.ProgressDialog;
import net.schwarzbaer.gui.ProgressDialog.CancelListener;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GUI.EnumCheckBoxMenuItem;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.DiscoveryData.AvailableData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.DiscoveryData.StoreData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.KnownWords;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.KnownWords.KnownWord;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Stats.StatValue;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Galaxy;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Planet;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Region;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.SolarSystem;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.SolarSystem.Race;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.SolarSystem.StarClass;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.UniverseObject.ExtraInfo;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.UniverseAddress;
import net.schwarzbaer.java.games.nomanssky.saveviewer.TableView.SimplifiedColumnConfig;
import net.schwarzbaer.java.games.nomanssky.saveviewer.TableView.SimplifiedColumnIDInterface;
import net.schwarzbaer.java.games.nomanssky.saveviewer.TableView.SimplifiedTable;
import net.schwarzbaer.java.games.nomanssky.saveviewer.TableView.SimplifiedTableModel;
import net.schwarzbaer.java.games.nomanssky.saveviewer.TreeView.AbstractTreeNode;
import net.schwarzbaer.java.games.nomanssky.saveviewer.TreeView.JsonTreeNode;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.BoolValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.FloatValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.IntegerValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.PathIsNotSolvableException;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.StringValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value.Type;
import sun.misc.SharedSecrets;

class SaveGameView extends JPanel {

	private static final long serialVersionUID = -1641171938196309864L;
	
	final File file;
	SaveGameData data;
	private JTabbedPane tabbedPane;

	private Window mainWindow;

	public SaveGameView(Window mainWindow, File file, SaveGameData data) {
		super(new BorderLayout(3, 3));
		setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		
		this.mainWindow = mainWindow;
		this.file = file;
		this.data = data;
		
		tabbedPane = new JTabbedPane();
		//tabbedPane.setPreferredSize(new Dimension(620, 500));
		
		addAllTabs();
		
		tabbedPane.addChangeListener(new ChangeListener() {
			@Override public void stateChanged(ChangeEvent e) {
				Component comp = tabbedPane.getSelectedComponent();
				if (comp instanceof SaveGameViewTabPanel) {
					SaveGameViewTabPanel tabPanel = (SaveGameViewTabPanel)comp;
					tabPanel.updateContent();
				}
			}
		});
		
		add(tabbedPane,BorderLayout.CENTER);
	}


	private void addAllTabs() {
		tabbedPane.addTab("General",new GeneralDataPanel(data));
		tabbedPane.addTab("Known Universe",new UniversePanel(data));
		tabbedPane.addTab("Galaxy Map",new GalaxyMapPanel(data,mainWindow));
		
		if (data.stats     !=null) tabbedPane.addTab("Stats",new StatsPanel(data));
		if (data.knownWords!=null) tabbedPane.addTab("KnownWords",new KnownWordsPanel(data));
		
		tabbedPane.addTab("DiscoveryData (Avail.)",new DiscoveryDataAvailablePanel(data));
		tabbedPane.addTab("DiscoveryData (Store)",new DiscoveryDataStorePanel(data));
		
		tabbedPane.addTab("Raw Data Tree",new RawDataTreePanel(file,data));
	}
	
	
	public void replaceData(SaveGameData data) {
		this.data = data;
		tabbedPane.removeAll();
		addAllTabs();
	}


	@Override
	public String toString() {
		return file.getName();
	}


	private static <T,R> Vector<R> convertVector( Vector<T> vector, Function<? super T,? extends R> convertValue ) {
		Vector<R> result = new Vector<>();
		for (T value : vector)
			result.add(convertValue.apply(value));
		return result;
	}
	
	private static class SaveGameViewTabPanel extends JPanel {
		private static final long serialVersionUID = -5779057150309507685L;
		
		protected SaveGameData data;

		public SaveGameViewTabPanel(SaveGameData data) {
			super(new BorderLayout(3, 3));
			setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			this.data = data;
		}
		
		public void updateContent() {};

		protected static JButton createButton(String title, ActionListener l) {
			JButton button = new JButton(title);
			button.addActionListener(l);
			return button;
		}

		protected void setNameForUniverseAddress(UniverseAddress ua, Universe.UniverseObject object, String objectStr) {
			String name = JOptionPane.showInputDialog(this, "New name for "+objectStr+" "+ua.getExtendedSigBoostCode(), object.getOriginalName());
			if (name!=null) {
				if (name.isEmpty()) name=null;
				object.setOriginalName(name);
				SaveViewer.saveUniverseObjectDataToFile(data.universe);
			}
		}
	}

	static class UniversePanel extends SaveGameViewTabPanel implements ActionListener {
		private static final long serialVersionUID = -4594889224613582352L;
		
		enum UniverseTreeActionCommand { SetName, SetDistance, SetRace, SetStarClass, ExpandAll, CollapseRemainingTree }
		
		static final IndexOnlyIconSource PortalGlyphsIS_100_90 = new IconSource.IndexOnlyIconSource(100,90,4);
		static final IndexOnlyIconSource PortalGlyphsIS_50_45  = new IconSource.IndexOnlyIconSource( 50,45,4);
		
		enum UniverseTreeIcons { Universe, Galaxy, Region, SolarSystem, Planet, GekSys, KorvaxSys, VykeenSys, Yellow, Red, Green, Blue }
		private static final int TreeIconHeight = 20;
		static final IconSource<UniverseTreeIcons> UniverseTreeIconsIS = new IconSource<UniverseTreeIcons>(30,TreeIconHeight){
			@Override protected int getIconIndexInImage(UniverseTreeIcons key) {
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
		
		private static UniverseTreeSolarSystemIconsMap UniverseTreeSolarSystemIcons = null;

		public static void prepareIconSources() {
			PortalGlyphsIS_100_90.readIconsFromResource("/PortalGlyphs.100.90.png");
			PortalGlyphsIS_50_45.readIconsFromResource("/PortalGlyphs.50.45.png");
			PortalGlyphsIS_50_45.cacheImages(16);
			String[] labels = new String[]{"N","NNE","NE","ENE","E","ESE","SE","SSE","S","SSW","SW","WSW","W","WNW","NW","NNW"};
			for (int i=0; i<16; ++i) {
				BufferedImage cachedImage = PortalGlyphsIS_50_45.getCachedImage(i);
				Graphics g = cachedImage.getGraphics();
				if (!(g instanceof Graphics2D)) return;
				Graphics2D g2 = (Graphics2D)g;
				
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g2.setFont(g2.getFont().deriveFont(10.0f).deriveFont(Font.BOLD));
				g2.setPaint(new Color(0xFFAF00));
				g2.drawString(i+" "+labels[i], 2, 10);
			}
			
			UniverseTreeIconsIS.readIconsFromResource("/UniverseTreeIcons.png");
			UniverseTreeIconsIS.cacheIcons(UniverseTreeIcons.values());
			
			Icon icon = UniverseTreeIconsIS.getCachedIcon(UniverseTreeIcons.Galaxy);
			icon = IconSource.cutIcon(icon,5,0,20,20);
			UniverseTreeIconsIS.setCachedIcon(UniverseTreeIcons.Galaxy,icon);
			
			UniverseTreeSolarSystemIcons = new UniverseTreeSolarSystemIconsMap();
			UniverseTreeSolarSystemIcons.createValues();
		}

		private JTree tree;
		private DefaultTreeModel treeModel;
		private GenericTreeNode<?> selectedNode;

		private JLabel portalGlyphs;
		private JTextArea textArea;
		private ExtraInfoTableModel extraInfoTableModel;
		
		private GenericTreeNode<?> clickedNode;
		private TreePath clickedTreePath;
		private JPopupMenu contextMenu_Other;
		private JPopupMenu contextMenu_SolarSystem;
		private JPopupMenu contextMenu_Planet;
		private JMenuItem miSetName_SolarSystem;
		private JMenuItem miSetName_Planet;
		private EnumCheckBoxMenuItem_StarClass[] miSetStarClassArr;
		private EnumCheckBoxMenuItem_Race[] miSetRaceArr;
		private ButtonGroup bgSetStarClass;
		private ButtonGroup bgSetRace;
		public UniversePanel(SaveGameData data) {
			super(data);
			
			selectedNode = null;
			TreeListener listener = new TreeListener();
			
			treeModel = new DefaultTreeModel(new UniverseNode(data.universe));
			tree = new JTree(treeModel);
			JScrollPane treeScrollPane = new JScrollPane(tree);
			tree.addTreeSelectionListener(listener);
			tree.addMouseListener(listener);
			tree.setCellRenderer(new UniverseTreeCellRenderer());
			tree.setRowHeight(TreeIconHeight+1);
			expandFullTree();
			
			contextMenu_Other = new JPopupMenu("Contextmenu");
			contextMenu_Other.add(createMenuItem("Expand complete tree",UniverseTreeActionCommand.ExpandAll));
			contextMenu_Other.add(createMenuItem("Collapse remaining tree",UniverseTreeActionCommand.CollapseRemainingTree));
			
			contextMenu_SolarSystem = new JPopupMenu("SolarSystem");
			contextMenu_SolarSystem.add(miSetName_SolarSystem = createMenuItem("Change Name",UniverseTreeActionCommand.SetName));
			contextMenu_SolarSystem.add(createMenuItem("Set measured distance to center of galaxy ",UniverseTreeActionCommand.SetDistance));
			
			contextMenu_SolarSystem.addSeparator();
			bgSetRace = new ButtonGroup();
			Race[] races = Universe.SolarSystem.Race.values();
			miSetRaceArr = new EnumCheckBoxMenuItem_Race[races.length];
			for (int i=0; i<races.length; ++i) {
				miSetRaceArr[i] = new EnumCheckBoxMenuItem_Race(races[i].fullName,UniverseTreeActionCommand.SetRace,races[i],false,this);
				contextMenu_SolarSystem.add(miSetRaceArr[i]);
				bgSetRace.add(miSetRaceArr[i]);
			}
			
			contextMenu_SolarSystem.addSeparator();
			bgSetStarClass = new ButtonGroup();
			StarClass[] starClasses = Universe.SolarSystem.StarClass.values();
			miSetStarClassArr = new EnumCheckBoxMenuItem_StarClass[starClasses.length];
			for (int i=0; i<starClasses.length; ++i) {
				miSetStarClassArr[i] = new EnumCheckBoxMenuItem_StarClass(starClasses[i].toString()+" Class",UniverseTreeActionCommand.SetStarClass,starClasses[i],false,this);
				contextMenu_SolarSystem.add(miSetStarClassArr[i]);
				bgSetStarClass.add(miSetStarClassArr[i]);
			}
			
			contextMenu_SolarSystem.addSeparator();
			contextMenu_SolarSystem.add(createMenuItem("Expand complete tree",UniverseTreeActionCommand.ExpandAll));
			contextMenu_SolarSystem.add(createMenuItem("Collapse remaining tree",UniverseTreeActionCommand.CollapseRemainingTree));
			
			contextMenu_Planet = new JPopupMenu("Planet");
			contextMenu_Planet.add(miSetName_Planet = createMenuItem("Change Name",UniverseTreeActionCommand.SetName));
			contextMenu_Planet.addSeparator();
			contextMenu_Planet.add(createMenuItem("Expand complete tree",UniverseTreeActionCommand.ExpandAll));
			contextMenu_Planet.add(createMenuItem("Collapse remaining tree",UniverseTreeActionCommand.CollapseRemainingTree));
			
			textArea = new JTextArea();
			textArea.setEditable(false);
			textArea.setBorder(BorderFactory.createEtchedBorder());
			
			extraInfoTableModel = new ExtraInfoTableModel();
			SimplifiedTable extraInfoTable = new SimplifiedTable(extraInfoTableModel,true,SaveViewer.DEBUG,true);
			extraInfoTable.setPreferredScrollableViewportSize(new Dimension(610, 120));;
			extraInfoTableModel.setTable(extraInfoTable);
			
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
		
		private static class EnumCheckBoxMenuItem_Race extends EnumCheckBoxMenuItem<Race,UniverseTreeActionCommand> {
			private static final long serialVersionUID = 8764557520719990435L;
			public EnumCheckBoxMenuItem_Race(String label, UniverseTreeActionCommand actionCommand, Race key, boolean selected, ActionListener actionListener) {
				super(label, actionCommand, key, selected, actionListener);
			}
		}
		
		private static class EnumCheckBoxMenuItem_StarClass extends EnumCheckBoxMenuItem<StarClass,UniverseTreeActionCommand> {
			private static final long serialVersionUID = -3616686445304535043L;
			public EnumCheckBoxMenuItem_StarClass(String label, UniverseTreeActionCommand actionCommand, StarClass key, boolean selected, ActionListener actionListener) {
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
			UniverseTreeActionCommand actionCommand = UniverseTreeActionCommand.valueOf(e.getActionCommand());
			Object source = e.getSource();
			switch(actionCommand) {
			case SetName:
				if (clickedNode!=null) {
					Planet planet; SolarSystem system;
					switch(clickedNode.type) {
					case Planet     : planet = ((     PlanetNode)clickedNode).value; setNameForUniverseAddress(planet.getUniverseAddress(),planet, "planet"      ); break;
					case SolarSystem: system = ((SolarSystemNode)clickedNode).value; setNameForUniverseAddress(system.getUniverseAddress(),system, "solar system"); break;
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
						SaveViewer.saveUniverseObjectDataToFile(data.universe);
					}
				}
				break;
				
			case SetRace:
				if (source instanceof EnumCheckBoxMenuItem_Race && clickedNode instanceof SolarSystemNode) {
					((SolarSystemNode)clickedNode).value.race = ((EnumCheckBoxMenuItem_Race)source).key;
					treeModel.nodeChanged(clickedNode);
					if (selectedNode==clickedNode) selectionChanged();
					SaveViewer.saveUniverseObjectDataToFile(data.universe);
				}
				break;
			case SetStarClass:
				if (source instanceof EnumCheckBoxMenuItem_StarClass && clickedNode instanceof SolarSystemNode) {
					((SolarSystemNode)clickedNode).value.starClass = ((EnumCheckBoxMenuItem_StarClass)source).key;
					treeModel.nodeChanged(clickedNode);
					if (selectedNode==clickedNode) selectionChanged();
					SaveViewer.saveUniverseObjectDataToFile(data.universe);
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
			}
			clickedNode = null;
			clickedTreePath = null;
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
						if (object instanceof GenericTreeNode<?>)
							clickedNode = (GenericTreeNode<?>)object;
					}
					JPopupMenu contextMenu = UniversePanel.this.mouseClicked();
					contextMenu.show(tree, e.getX(), e.getY());
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
		
		private JPopupMenu mouseClicked() {
			if (clickedNode==null)
				return contextMenu_Other;
			
			switch(clickedNode.type) {
			case SolarSystem:
				SolarSystem system = ((SolarSystemNode)clickedNode).value;
				miSetName_SolarSystem.setText(system.hasOriginalName()?"Change name":"Set name");
				EnumCheckBoxMenuItem.setCheckBoxMenuItems(miSetRaceArr     , system.race     , Race     .values(), bgSetRace     );
				EnumCheckBoxMenuItem.setCheckBoxMenuItems(miSetStarClassArr, system.starClass, StarClass.values(), bgSetStarClass);
				return contextMenu_SolarSystem;
				
			case Planet:
				miSetName_Planet.setText(((PlanetNode)clickedNode).value.hasOriginalName()?"Change name":"Set name");
				return contextMenu_Planet;
				
			default:
				return contextMenu_Other;
			}
		}

		private void selectionChanged() {
			textArea.setText("");
			
			if (selectedNode==null) {
				extraInfoTableModel.clearData();
				return;
			}
			
			if (selectedNode.type!=NodeType.Planet) {
				portalGlyphs.setIcon(null);
				extraInfoTableModel.clearData();
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
				
				distance_reg = ua.getDistToOther_inRegionUnits(data.general.getCurrentUniverseAddress());
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
			extraInfoTableModel.setData(obj.extraInfos);
		}

		private Icon createPortalGlyphs(long portalGlyphCode) {
			BufferedImage image = new BufferedImage(50*12, 45*1, BufferedImage.TYPE_INT_RGB);
			Graphics graphics = image.getGraphics();
			
			for (int i=11; i>=0; --i) {
				int nr = (int)(portalGlyphCode&0xF);
				portalGlyphCode = portalGlyphCode>>4;
				BufferedImage glyph = PortalGlyphsIS_50_45.getImage(nr);
				graphics.drawImage(glyph, i*50, 0, null);
			}
			return new ImageIcon(image);
		}
		
		private enum ExtraInfoColumnID implements TableView.SimplifiedColumnIDInterface {
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
			private JTable table;

			protected ExtraInfoTableModel() {
				super(ExtraInfoColumnID.values());
				this.tableData = null;
				this.table = null;
			}

			public void setTable(JTable table) {
				this.table = table;
			}

			public void clearData() { setData(null); }
			public void setData(Vector<ExtraInfo> data) {
				if (table!=null && table.isEditing()) {
					TableCellEditor cellEditor = table.getCellEditor();
					if (cellEditor!=null) cellEditor.cancelCellEditing();
				}
				this.tableData = data;
				fireTableUpdate();
			}

			@Override
			public int getRowCount() {
				if (tableData==null) return 0;
				return tableData.size()+1;
			}

			@Override
			public Object getValueAt(int rowIndex, int columnIndex, ExtraInfoColumnID columnID) {
				if (tableData==null) return null;
				if (rowIndex==tableData.size()) return "";
				switch(columnID) {
				case Label: return tableData.get(rowIndex).shortLabel;
				case Info : return tableData.get(rowIndex).info;
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
					case Label: tableData.add(new ExtraInfo(aValue.toString(),"")); break;
					case Info : tableData.add(new ExtraInfo("",aValue.toString())); break;
					}
					fireTableRowAdded(rowIndex+1);
				} else
					switch(columnID) {
					case Label: tableData.get(rowIndex).shortLabel = aValue.toString(); break;
					case Info : tableData.get(rowIndex).info       = aValue.toString(); break;
					}
				treeModel.nodeChanged(selectedNode);
				treeModel.nodeChanged(selectedNode.parent);
				SaveViewer.saveUniverseObjectDataToFile(data.universe);
			}
		}
		
		static class UniverseTreeCellRenderer extends DefaultTreeCellRenderer {

			private static final Color TEXTCOLOR__CURRENT_POS  = new Color(0x2EA000);
			private static final Color TEXTCOLOR__WITHOUT_NAME = new Color(0x808080);
			private static final Color TEXTCOLOR__NO_UPLOADED  = new Color(0x0000FF); // or 0x1D67AE

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
					if (node instanceof SolarSystemNode) obj = ((SolarSystemNode)node).value;
					if (node instanceof      PlanetNode) obj = ((     PlanetNode)node).value;
					if (obj != null) {
						if (!selected && !obj.hasOriginalName() && !obj.hasUploadedName()) setForeground(TEXTCOLOR__WITHOUT_NAME);
						if (!selected && obj.isNotUploaded) setForeground(TEXTCOLOR__NO_UPLOADED);
						if (obj.isCurrPos) {
							if (!selected) setForeground(TEXTCOLOR__CURRENT_POS);
							setFont(boldfont);
						}
					}
				}
				return component;
			}
		}
		
		enum NodeType { Universe, Galaxy, Region, SolarSystem, Planet }
		
		static abstract class LocalTreeNode extends AbstractTreeNode<LocalTreeNode> {
			
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
	
	

	private static class KnownWordsPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = 7096092479075372171L;
		
		public KnownWordsPanel(SaveGameData data) {
			super(data);
			
			KnownWordsTableModel tableModel = new KnownWordsTableModel(data.knownWords);
			SimplifiedTable table = new SimplifiedTable(tableModel,true,SaveViewer.DEBUG,true);
			JScrollPane tableScrollPane = new JScrollPane(table);
			
			add(tableScrollPane,BorderLayout.CENTER);
		}

		private enum KnownWordsTableColumnID implements TableView.SimplifiedColumnIDInterface {
			WordID        ("ID"  , 50,-1,120,120),
			TranslatedWord("Word", 50,-1,100,100),
			Race          (""    , 20,-1, 70, 70);
		
			private TableView.SimplifiedColumnConfig columnConfig;
			
			KnownWordsTableColumnID() {
				columnConfig = new TableView.SimplifiedColumnConfig();
			}
			KnownWordsTableColumnID(String name, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				columnConfig = new TableView.SimplifiedColumnConfig(name, String.class, minWidth, maxWidth, prefWidth, currentWidth);
			}
			@Override public TableView.SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
		}

		private static class KnownWordsTableModel extends TableView.SimplifiedTableModel<KnownWordsTableColumnID> {
		
			private KnownWords knownWords;
			private int numberOfRaces;
			
			public KnownWordsTableModel(KnownWords knownWords) {
				super(new KnownWordsTableColumnID[]{ KnownWordsTableColumnID.WordID, KnownWordsTableColumnID.TranslatedWord });
				this.knownWords = knownWords;
				numberOfRaces = this.knownWords.wordCounts.length;
			}
			
			@Override
			protected KnownWordsTableColumnID getColumnID(int columnIndex) {
				if (columnIndex<columns.length) return super.getColumnID(columnIndex);
				if (columnIndex<columns.length+numberOfRaces) return KnownWordsTableColumnID.Race;
				return null;
			}
			@Override public int getColumnCount() { return columns.length+numberOfRaces; }
			@Override public String getColumnName(int columnIndex) {
				if (columnIndex<columns.length) return super.getColumnName(columnIndex);
				if (columnIndex<columns.length+numberOfRaces) {
					switch(columnIndex-columns.length) {
					case 0: return "Gek";
					case 1: return "Vy'keen";
					case 2: return "Korvax";
					case 4: return "Atlas";
					default:
						return "Race "+(columnIndex-columns.length);
					}
				}
				return null;
			}
		
			@Override
			public int getRowCount() {
				return knownWords.wordList.size()+1;
			}
			
			@Override
			public Object getValueAt(int rowIndex, int columnIndex, KnownWordsTableColumnID columnID) {
				if (rowIndex==0) {
					switch(columnID) {
					case WordID: return String.format("%d different words", knownWords.wordList.size());
					case TranslatedWord: return "";
					case Race:
						int race = columnIndex-columns.length;
						return String.format(Locale.ENGLISH,"%d (%1.1f%%)", knownWords.wordCounts[race], knownWords.wordCounts[race]*100.0f/knownWords.wordList.size());
					}
				} else {
					KnownWord knownWord = knownWords.wordList.get(rowIndex-1);
					if (knownWord==null) return null;
					
					switch(columnID) {
					case WordID: return knownWord.word;
					case TranslatedWord: return "";
					case Race:
						int race = columnIndex-columns.length;
						return (race>=knownWord.races.length)?"???":(knownWord.races[race]?"known":"");
					}
				}
				return null;
			}
		}
	}
	
	private static class DiscoveryDataAvailablePanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = 2870833302184314416L;

		public DiscoveryDataAvailablePanel(SaveGameData data) {
			super(data);
			
			DDATableModel tableModel = new DDATableModel();
			SimplifiedTable table = new SimplifiedTable(tableModel,true,true,true);
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
	
	private static class DiscoveryDataStorePanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = 6619075068331735784L;

		public DiscoveryDataStorePanel(SaveGameData data) {
			super(data);
			
			DDSTableModel tableModel = new DDSTableModel();
			SimplifiedTable table = new SimplifiedTable(tableModel,true,true,true);
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
	
	private static class StatsPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = -1541256209397699528L;
		
		private SimplifiedTable table;
		
		public StatsPanel(SaveGameData data) {
			super(data);
			
			table = new SimplifiedTable(true,SaveViewer.DEBUG,true);
			JScrollPane tableScrollPane = new JScrollPane(table);
			tableScrollPane.setPreferredSize(new Dimension(600, 500));
			
			Vector<String> statConfigs = new Vector<>();
			statConfigs.add("Global");
			statConfigs.add("All Planets");
			statConfigs.addAll(convertVector(data.stats.planetStats, ps -> "Planet "+ps.planet));
			
			JComboBox<String> selector = new JComboBox<>(statConfigs);
			selector.addActionListener(e -> changeSelection( selector.getSelectedIndex() ));
				
			add(selector,BorderLayout.NORTH);
			add(tableScrollPane,BorderLayout.CENTER);
			
			changeSelection( selector.getSelectedIndex() );
		}

		private void changeSelection(int index) {
			switch (index) {
			case -1: table.setModel(new DefaultTableModel()); break;
			case  0: table.setModel(new StatsTableModel(data.stats.globalStats)); break;
			case  1: table.setModel(new DefaultTableModel()); break;
			default: table.setModel(new StatsTableModel(data.stats.planetStats.get(index-2).stats)); break;
			}
		}

		private enum StatsTableColumnID implements SimplifiedColumnIDInterface {
			ID         ("ID"         , String.class, 50,-1,120,120),
			Name       ("Name"       , String.class, 50,-1,210,210),
			IntValue   ("Int"        , Long.class  , 20,-1, 70, 70),
			FloatValue ("Float"      , Double.class, 20,-1, 70, 70),
			Denominator("Denominator", Double.class, 20,-1, 40, 40);
			
			private TableView.SimplifiedColumnConfig columnConfig;
			
			StatsTableColumnID() {
				columnConfig = new SimplifiedColumnConfig();
				columnConfig.name = toString();
			}
			StatsTableColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				columnConfig = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			@Override public SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
		}

		private static class StatsTableModel extends SimplifiedTableModel<StatsTableColumnID> {
			
			private Vector<StatValue> statsList;
			
			public StatsTableModel(Vector<StatValue> statsList) {
				super(new StatsTableColumnID[]{ StatsTableColumnID.ID, StatsTableColumnID.Name, StatsTableColumnID.IntValue, StatsTableColumnID.FloatValue, StatsTableColumnID.Denominator });
				this.statsList = statsList;
			}
		
			@Override public int getRowCount() { return statsList.size(); }
		
			@Override
			public Object getValueAt(int rowIndex, int columnIndex, StatsTableColumnID columnID) {
				StatValue statValue = statsList.get(rowIndex);
				
				if (statValue==null) return null;
				
				switch(columnID) {
				case ID  : return statValue.ID;
				case Name: if (statValue.knownID!=null) return statValue.knownID.fullName; else return statValue.ID;
				case IntValue: return statValue.IntValue;
				case FloatValue: return statValue.FloatValue;
				case Denominator: return statValue.Denominator;
				}
				return null;
			}
		
			@Override
			protected boolean isCellEditable(int rowIndex, int columnIndex, StatsTableColumnID columnID) {
				return columnID==StatsTableColumnID.Name;
			}
		
			@Override
			protected void setValueAt(Object aValue, int rowIndex, int columnIndex, StatsTableColumnID columnID) {
				if (columnID!=StatsTableColumnID.Name) { fireTableCellUpdate(rowIndex, columnIndex); return; }
				
				StatValue statValue = statsList.get(rowIndex);
				
				if (statValue.knownID==null || aValue==null) { fireTableCellUpdate(rowIndex, columnIndex); return; }
				
				statValue.knownID.fullName = aValue.toString();
				SaveViewer.saveKnownStatIDsToFile();
			}
		}
	}

	private static class GeneralDataPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = -3866983525686776846L;
		
		private JTextArea textArea;
	
		public GeneralDataPanel(SaveGameData data) {
			super(data);
			
			textArea = new JTextArea();
			textArea.setEditable(false);
			
			JScrollPane treeScrollPane = new JScrollPane(textArea);
			treeScrollPane.setPreferredSize(new Dimension(600, 500));
			
			JPanel buttonPanel = new JPanel();
			buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
			buttonPanel.add(createButton("Set name for current position",e -> setNameForUniverseAddress(data.general.getCurrentUniverseAddress())));
			
			add(treeScrollPane,BorderLayout.CENTER);
			add(buttonPanel,BorderLayout.SOUTH);
			
			updateContent();
		}
		
		private void setNameForUniverseAddress(UniverseAddress ua) {
			if (ua==null) {
				JOptionPane.showMessageDialog(this, "Current location couldn't be identified.");
				return;
			}
			
			if (ua.isPlanet     ()) setNameForUniverseAddress(ua, data.universe.getOrCreatePlanet     (ua), "planet"      );
			if (ua.isSolarSystem()) setNameForUniverseAddress(ua, data.universe.getOrCreateSolarSystem(ua), "solar system");
			
			updateContent();
		}

		@Override
		public void updateContent() {
			textArea.setText("");
			appendValue("Current Units    ", data.general.getUnits() );
			appendValue("Player Health    ", data.general.getPlayerHealth() );
			appendValue("Player Shield    ", data.general.getPlayerShield() );
			appendValue("Ship Health      ", data.general.getShipHealth() );
			appendValue("Ship Shield      ", data.general.getShipShield() );
			appendValue("Time Alive       ", data.general.getTimeAlive() );
			appendValue("Total PlayTime   ", data.general.getTotalPlayTime() );
			appendValue("Hazard Time Alive", data.general.getHazardTimeAlive() );
			
			appendEmptyLine();
			UniverseAddress currentUA = data.general.getCurrentUniverseAddress();
			if (currentUA!=null) {
				appendLine("Current Location in Universe:");
				if (currentUA.isPlanet     ()) {
					Planet planet = data.universe.findPlanet(currentUA);
					if (planet!=null)
						appendLine("    on planet \""+planet+"\"");
					else
						appendLine("    on a planet");
				}
				if (currentUA.isSolarSystem()) {
					SolarSystem system = data.universe.findSolarSystem(currentUA);
					if (system!=null)
						appendLine("    in solar system \""+system+"\"");
					else
						appendLine("    in a solar system");
				}
				appendLine("    "+currentUA.getCoordinates());
				appendLine("    "+currentUA.getExtendedSigBoostCode());
				appendLine(String.format(Locale.ENGLISH, "    Distance to Center of Galaxy: %1.1f regions", currentUA.getDistToCenter_inRegionUnits()));
			}
			
			Long knownGlyphs = data.general.getKnownGlyphsMaks();
			if (knownGlyphs!=null) {
				appendEmptyLine();
				appendLine("Known portal glyphs:");
				String str = "";
				int n = (int)(long)knownGlyphs;
				for (int i=0; i<16; ++i) {
					if ((n&1) > 0) {
						if (!str.isEmpty()) str+=", ";
						str+=i;
					}
					n = n>>1;
				}
				appendLine("   "+str);
			}
			
			appendEmptyLine();
			appendLine("Discovered Items:");
			appendValue("   available"          , (long)data.discoveryData.availableData.size() );
			appendValue("      on planets"      , (long)data.discoveryData.availDiscoveredItemOnPlanets );
			appendValue("      in solar systems", (long)data.discoveryData.availDiscoveredItemInSolarSystms );
			appendValue("   stored   ", (long)data.discoveryData.storeData.size() );
			appendValue("      on planets"      , (long)data.discoveryData.storedDiscoveredItemOnPlanets );
			appendValue("      in solar systems", (long)data.discoveryData.storedDiscoveredItemInSolarSystms );
			
			
			
//			if (SaveViewer.DEBUG) {
//				appendEmptyLine();
//				appendValue("Test value 1 (Bool)"   , data.general.getTestBool   ("PlayerStateData","Stats",7,"Stats",4,"Value","Denominator") );
//				appendValue("Test value 2 (Integer)", data.general.getTestInteger("PlayerStateData","Stats",7,"Stats",4,"Value","Denominator") );
//				appendValue("Test value 3 (Float)"  , data.general.getTestFloat  ("PlayerStateData","Stats",7,"Stats",4,"Value","Denominator") );
//				appendValue("Test value 4 (String)" , data.general.getTestString ("PlayerStateData","Stats",7,"Stats",4,"Value","Denominator") );
//				appendValue("Test value 5 (Float)"  , Type.Float  , "PlayerStateData","Stats",7,"Stats",4,"Value","Denominator");
//				appendValue("Test value 6 (Integer)", Type.Integer, "PlayerStateData","Stats",7,"Stats",4,"Value","Denominator");
//			}
		}
		
		private void appendEmptyLine() {
			textArea.append("\r\n");
		}

		private void appendStatement(String label, String statement) {
			String line = label+": "+statement;
			appendLine(line);
		}

		private void appendLine(String line) {
			if (!textArea.getText().isEmpty())
				textArea.append("\r\n");
			textArea.append(line);
		}

		private void showError(String label) {
			switch (data.error) {
			case NoError:
				appendStatement(label,"???");
				System.out.println("Value \""+label+"\" is <null>, but error is unknown: \""+data.errorMessage+"\"");
				break;
			case PathIsNotSolvable:
				appendStatement(label,"Value not found");
				System.out.println("Value \""+label+"\" not found: "+data.errorMessage);
				break;
			case UnexpectedType:
				appendStatement(label,"Value has unexpected type");
				System.out.println("Value \""+label+"\" has unexpected type: "+data.errorMessage);
				break;
			case ValueIsNull:
				appendStatement(label,"<null>");
				System.out.println("Value \""+label+"\" is <null>: "+data.errorMessage);
				break;
			}
		}

		@SuppressWarnings("unused")
		private void appendValue(String label, Boolean value) { if (value==null) showError(label); else appendStatement(label, ""+value); }
		private void appendValue(String label, Long    value) { if (value==null) showError(label); else appendStatement(label, ""+value); }
		@SuppressWarnings("unused")
		private void appendValue(String label, Double  value) { if (value==null) showError(label); else appendStatement(label, ""+value); }
		@SuppressWarnings("unused")
		private void appendValue(String label, String  value) { if (value==null) showError(label); else appendStatement(label,    value); }
		
		@SuppressWarnings("unused")
		private void appendValue(String label, Type expectedType, Object... path) {
			Value value = null;
			try {
				value = JSON_Data.getSubNode(data.json_data,path);
				switch(expectedType) {
				case Bool   : if (appendBoolValue   (label,value)) return; break;
				case Float  : if (appendFloatValue  (label,value)) return; break;
				case Integer: if (appendIntegerValue(label,value)) return; break;
				case String : if (appendStringValue (label,value)) return; break;
				default: break;
				}
			} catch (PathIsNotSolvableException e) {
				appendStatement(label,"Value not found");
				System.out.println("Value \""+label+"\" not found: "+e.getMessage());
				return;
			}
			appendStatement(label,"Value has unexpected type");
			System.out.println("Value \""+label+"\" has unexpected type: "+(value==null?"<null>":value.getClass()));
		}

		private boolean appendBoolValue(String label, Value value) {
			if (value instanceof BoolValue) {
				BoolValue valueB = (BoolValue)value;
				appendStatement(label, ""+valueB.value);
				return true;
			}
			return false;
		}

		private boolean appendIntegerValue(String label, Value value) {
			if (value instanceof IntegerValue) {
				IntegerValue valueI = (IntegerValue)value;
				appendStatement(label, ""+valueI.value);
				return true;
			}
			return false;
		}

		private boolean appendFloatValue(String label, Value value) {
			if (value instanceof FloatValue) {
				FloatValue valueF = (FloatValue)value;
				appendStatement(label, ""+valueF.value);
				return true;
			}
			return false;
		}

		private boolean appendStringValue(String label, Value value) {
			if (value instanceof StringValue) {
				StringValue valueS = (StringValue)value;
				appendStatement(label, valueS.value);
				return true;
			}
			return false;
		}
	}

	private static class RawDataTreePanel extends SaveGameViewTabPanel implements MouseListener, ActionListener {
		private static final long serialVersionUID = -50409207801775293L;
		
		enum RawDataTreeActionCommand { ShowPath, CopyPath, CopyValue }

		private JTree tree;
		private JPopupMenu contextMenu;
		private TreePath contextMenuTarget;

		public RawDataTreePanel(File file, SaveGameData data) {
			super(data);
			
			SaveViewer.log("Create tree from file \"%s\" ...",file.getPath());
			tree = new JTree(new DefaultTreeModel(new JsonTreeNode(data.json_data)));
			tree.addMouseListener(this);
			//tree.setCellRenderer(new DefaultTreeCellRenderer());
			JScrollPane treeScrollPane = new JScrollPane(tree);
			treeScrollPane.setPreferredSize(new Dimension(600, 500));
			SaveViewer.log_ln(" done");
			
			contextMenu = new JPopupMenu("Contextmenu");
			contextMenu.add(createMenuItem("Copy Path",RawDataTreeActionCommand.CopyPath));
			contextMenu.add(createMenuItem("Copy Value",RawDataTreeActionCommand.CopyValue));
			contextMenu.add(createMenuItem("Show Path",RawDataTreeActionCommand.ShowPath));
			
			add(treeScrollPane,BorderLayout.CENTER);
		}

		private JMenuItem createMenuItem(String label, RawDataTreeActionCommand actionCommand) {
			JMenuItem menuItem = new JMenuItem(label);
			menuItem.addActionListener(this);
			menuItem.setActionCommand(actionCommand.toString());
			return menuItem;
		}

		@Override public void mousePressed(MouseEvent e) {}
		@Override public void mouseReleased(MouseEvent e) {}
		@Override public void mouseEntered(MouseEvent e) {}
		@Override public void mouseExited(MouseEvent e) {}
		@Override public void mouseClicked(MouseEvent e) {
			if (e.getButton()==MouseEvent.BUTTON3) {
				contextMenuTarget = tree.getPathForLocation(e.getX(), e.getY());
				contextMenu.show(tree, e.getX(), e.getY());
			}
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			RawDataTreeActionCommand actionCommand = RawDataTreeActionCommand.valueOf(e.getActionCommand());
			switch(actionCommand) {
			case CopyPath: {
				Toolkit toolkit = Toolkit.getDefaultToolkit();
				Clipboard clipboard = toolkit.getSystemClipboard();
				DataHandler content = new DataHandler(pathToShortString(contextMenuTarget),"text/plain");
				try { clipboard.setContents(content,null); }
				catch (IllegalStateException e1) { e1.printStackTrace(); }
			} break;
			case ShowPath:
				System.out.println("Path: "+contextMenuTarget);
				System.out.println("    = "+pathToShortString(contextMenuTarget));
				break;
			case CopyValue:
				Toolkit toolkit = Toolkit.getDefaultToolkit();
				Clipboard clipboard = toolkit.getSystemClipboard();
				Object comp = contextMenuTarget.getLastPathComponent();
				if (!(comp instanceof JsonTreeNode)) return;
				
				Value data = ((JsonTreeNode)comp).data;
				
				DataHandler content = new DataHandler(data.toString(),"text/plain");
				try { clipboard.setContents(content,null); }
				catch (IllegalStateException e1) { e1.printStackTrace(); }
				break;
			}
		}

		private String pathToShortString(TreePath treePath) {
			StringBuilder sb = new StringBuilder();
			for (int i=0; i<treePath.getPathCount(); ++i) {
				Object pathComponent = treePath.getPathComponent(i);
				if (pathComponent instanceof JsonTreeNode) {
					JsonTreeNode node = (JsonTreeNode)pathComponent;
					if (node.parent==null)
						sb.append("[root]");
					else if (node.name==null) {
						if (node.parent.data.type!=Type.Array)
							sb.append("<nameless value inside of non array>");
						else
							sb.append("["+node.parent.getIndex(node)+"]");
					} else
						sb.append("."+node.name);
				} else
					sb.append("<unknown class of \""+pathComponent+"\">");
			}
			return sb.toString();
		}
	}

	private static class GalaxyMapPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = 9055290876621464068L;

		private Window mainWindow;
		
		private GalaxyMap galaxyMap;
		private JScrollBar scrollBarHoriz;
		private JScrollBar scrollBarVert;

		private JComboBox<ZoomStep> zoomField;
		private JLabel statusField;
		
		private static class ZoomStep {
			private double value;
			private ZoomStep(double value) { this.value = value; }
			@Override public String toString() { return String.format(Locale.ENGLISH, "%1.1f%%", value*100); }
			public static ZoomStep[] create(double[] zoomSteps) {
				ZoomStep[] arr = new ZoomStep[zoomSteps.length];
				for (int i=0; i<zoomSteps.length; ++i) arr[i] = new ZoomStep(zoomSteps[i]);
				return arr;
			}
		}
		private static class GlyphNumber {
			private int value;
			private GlyphNumber(int value) { this.value = value; }
			@Override public String toString() {
				switch (value) {
				case 1 : return "Glyph 0";
				case 17: return "Known Glyphs";
				default: return String.format("Glyphs 0..%d", value-1);
				}
			}
			public static GlyphNumber[] create() {
				GlyphNumber[] arr = new GlyphNumber[17];
				for (int i=0; i<arr.length; ++i) arr[i] = new GlyphNumber(i+1);
				return arr;
			}
		}
		
		public GalaxyMapPanel(SaveGameData data, Window mainWindow) {
			super(data);
			this.mainWindow = mainWindow;
			
			CombinedListener combiListener = new CombinedListener();
			
			int preselectedGalaxy = 0;
			JComboBox<Galaxy> cmbbxGalaxies = new JComboBox<>(data.universe.galaxies);
			cmbbxGalaxies.setSelectedIndex(preselectedGalaxy);
			cmbbxGalaxies.addActionListener(e->galaxyMap.setGalaxy((Galaxy)cmbbxGalaxies.getSelectedItem()));
			
			zoomField = new JComboBox<ZoomStep>(ZoomStep.create(new double[]{0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0,1.25,1.5,1.75,2.0,2.5,3.0,3.5,4.0,5.0,6.0,7.0,8.0,10.0}));
			zoomField.addActionListener(
					e->{
						ZoomStep zoomStep = (ZoomStep)zoomField.getSelectedItem();
						if (zoomStep!=null) { galaxyMap.setZoom(zoomStep.value); showStatus(-1,-1); }
						}
					);
			
			statusField = new JLabel("");
			
			JCheckBox chkbxShowMarkers = new JCheckBox("Show markers", false);
			chkbxShowMarkers.addActionListener(e->galaxyMap.showMarkers(chkbxShowMarkers.isSelected()));
			
			JCheckBox chkbxShowGlyphRegions = new JCheckBox("Show region reachable by known glyphs", false);
			JComboBox<GlyphNumber> cmbbxKnownGlyphs = new JComboBox<>(GlyphNumber.create());
			cmbbxKnownGlyphs.setSelectedItem(null);
			cmbbxKnownGlyphs.setEnabled(false);

			chkbxShowGlyphRegions.addActionListener(e->{
				boolean show = chkbxShowGlyphRegions.isSelected();
				cmbbxKnownGlyphs.setEnabled(show);
				showGlyphOverlay(show?cmbbxKnownGlyphs.getSelectedIndex()+1:0);
			});
			cmbbxKnownGlyphs.addActionListener(e->showGlyphOverlay(cmbbxKnownGlyphs.getSelectedIndex()+1));
			
			JPanel leftStatusPanel = new JPanel();
			leftStatusPanel.setLayout(new BoxLayout(leftStatusPanel, BoxLayout.X_AXIS));
			leftStatusPanel.add(cmbbxGalaxies);
			leftStatusPanel.add(zoomField);
			
			JPanel rightStatusPanel = new JPanel();
			rightStatusPanel.setLayout(new BoxLayout(rightStatusPanel, BoxLayout.X_AXIS));
			rightStatusPanel.add(chkbxShowMarkers);
			rightStatusPanel.add(chkbxShowGlyphRegions);
			rightStatusPanel.add(cmbbxKnownGlyphs);
			
			JPanel statusPanel = new JPanel(new BorderLayout(3,3));
			statusPanel.add(leftStatusPanel,BorderLayout.WEST);
			statusPanel.add(statusField,BorderLayout.CENTER);
			statusPanel.add(rightStatusPanel,BorderLayout.EAST);
			
			Long knownGlyphs = data.general.getKnownGlyphsMaks();
			//knownGlyphs = 0b110111100L;
			galaxyMap = new GalaxyMap(combiListener,data.universe.galaxies.get(preselectedGalaxy),data.general.getCurrentUniverseAddress(),knownGlyphs);
			galaxyMap.prepareMap();
			galaxyMap.addMouseWheelListener(combiListener);
			galaxyMap.addMouseMotionListener(combiListener);
			galaxyMap.addMouseListener(combiListener);
			
			scrollBarVert = new JScrollBar(JScrollBar.VERTICAL);
			scrollBarHoriz = new JScrollBar(JScrollBar.HORIZONTAL);
			
			scrollBarVert.addAdjustmentListener(combiListener);
			scrollBarHoriz.addAdjustmentListener(combiListener);
			
			JPanel mapview = new JPanel();
			GridBagLayout layout = new GridBagLayout();
			GridBagConstraints c = new GridBagConstraints();
			mapview.setLayout(layout);
			
			addComp(mapview,layout,c, galaxyMap     , 1,1,1,GridBagConstraints.BOTH);
			addComp(mapview,layout,c, scrollBarVert , 0,1,GridBagConstraints.REMAINDER,GridBagConstraints.VERTICAL);
			addComp(mapview,layout,c, scrollBarHoriz, 1,0,1,GridBagConstraints.HORIZONTAL);
			addComp(mapview,layout,c, new JLabel()  , 0,0,GridBagConstraints.REMAINDER,GridBagConstraints.NONE);
			
			add(mapview,BorderLayout.CENTER);
			add(statusPanel,BorderLayout.SOUTH);
			
			zoomField.setSelectedItem(null);
		}

		private void addComp(JPanel panel, GridBagLayout layout, GridBagConstraints c, Component comp, double weightx, double weighty, int gridwidth, int fill) {
			c.weightx=weightx;
			c.weighty=weighty;
			c.gridwidth=gridwidth;
			c.fill = fill;
			layout.setConstraints(comp, c);
			panel.add(comp);
		}

		private void showGlyphOverlay(int numberOfKnownGlyphs) {
			Worker worker = galaxyMap.showGlyphOverlay(numberOfKnownGlyphs);
			if (worker==null) return;
			
			ProgressDialog pd = new ProgressDialog(mainWindow,"Create Glyph Overlay");
			ProgressDialogWorker pdWorker = new ProgressDialogWorker(pd,worker);
			
			pd.addCancelListener(pdWorker);
			
			SwingUtilities.invokeLater(new Runnable() {
				@Override public void run() {
					new Thread(pdWorker).start();
					pd.showDialog();
				}
			});
		}
		
		private static abstract class Worker implements Runnable {
			boolean stopNow;
			private ProgressDialog pd;
			Worker() { stopNow = false; pd = null; }
			
			protected void setProgress(int value         ) { if (pd!=null) pd.setValue(value    ); }
			protected void setProgress(int value, int max) { if (pd!=null) pd.setValue(value,max); }
			protected void setTaskTitle(String title) { if (pd!=null) pd.setTaskTitle(title); }
		}
		
		private static class ProgressDialogWorker implements CancelListener, Runnable {
			private Worker worker;
			public ProgressDialogWorker(ProgressDialog pd, Worker worker) {
				this.worker = worker;
				this.worker.pd = pd;
			}

			@Override public void cancelTask() { worker.stopNow = true; worker.pd.closeDialog(); }
			@Override public void run() { worker.run(); worker.pd.closeDialog(); }
		}

		private void showStatus(int x, int y) {
			String str = String.format(Locale.ENGLISH, "Zoom: %1.1f%%", galaxyMap.zoomRatio*100);
			if (x>=0 && y>=0) {
				int voxelX = galaxyMap.computeVoxelX(x);
				int voxelZ = galaxyMap.computeVoxelZ(y);
				UniverseAddress ua = new UniverseAddress(0,voxelX,0,voxelZ,0,0);
				str += String.format(Locale.ENGLISH, ",  Region: (%d,0,%d)", voxelX,voxelZ);
				str += String.format(Locale.ENGLISH, ",  GlyphCode: %s", ua.getPortalGlyphCodeStr());
				str += String.format(Locale.ENGLISH, ",  Distance to Center: %1.1f regions", ua.getDistToCenter_inRegionUnits() /*Math.sqrt(voxelX*voxelX+voxelZ*voxelZ)*/);
			}
			statusField.setText(str);
		}
		
		private class CombinedListener implements MouseWheelListener, MouseMotionListener, MouseListener, AdjustmentListener {
			
			private boolean isScrollBarListeningEnabled;
			CombinedListener() {
				isScrollBarListeningEnabled = true;
			}
			
			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				if (!isScrollBarListeningEnabled) return;
				
				Adjustable adj = e.getAdjustable();
				switch (adj.getOrientation()) {
				case Adjustable.HORIZONTAL:
//					System.out.printf("scrollBarHoriz.adjustmentValueChanged: %d..%d..%d (%d) %s\r\n", adj.getMinimum(),adj.getValue(),adj.getMaximum(),adj.getVisibleAmount(),e.getValueIsAdjusting() );
					galaxyMap.setXOffset(adj.getValue());
					break;
				case Adjustable.VERTICAL:
//					System.out.printf("scrollBarVert .adjustmentValueChanged: %d..%d..%d (%d) %s\r\n", adj.getMinimum(),adj.getValue(),adj.getMaximum(),adj.getVisibleAmount(),e.getValueIsAdjusting() );
					galaxyMap.setYOffset(adj.getValue());
					break;
				}
			}

			public void setHorizScrollBar(int min, int value, int max, int visible) {
				isScrollBarListeningEnabled = false;
				scrollBarHoriz.setValues(value, visible, min, max);
				scrollBarHoriz.setBlockIncrement(Math.min((max-min)/10, visible));
				isScrollBarListeningEnabled = true;
			}
			
			public void setVertScrollBar(int min, int value, int max, int visible) {
				isScrollBarListeningEnabled = false;
				scrollBarVert.setValues(value, visible, min, max);
				scrollBarVert.setBlockIncrement(Math.min((max-min)/10, visible));
				isScrollBarListeningEnabled = true;
			}
			
			@Override public void mouseWheelMoved(MouseWheelEvent e) {
//				System.out.printf(Locale.ENGLISH, "mouseWheelMoved()%s%s %f %d\r\n", e.isShiftDown()?" shift":"", e.isControlDown()?" ctrl":"",e.getPreciseWheelRotation(), e.getUnitsToScroll());
				if (e.isControlDown()) {
					zoomField.setSelectedItem(null);
					galaxyMap.incZoom(e.getX(),e.getY(),-e.getWheelRotation());
				} else {
					int canvasSize = e.isShiftDown()?galaxyMap.getViewWidth():galaxyMap.getViewHeight();
					double scrollAmount = Math.max(canvasSize*0.05,1.0);
					int offsetInc = (int)Math.round(e.getPreciseWheelRotation()*scrollAmount);
					if (e.isShiftDown()) galaxyMap.incXOffset(offsetInc);
					else                 galaxyMap.incYOffset(offsetInc);
				}
				showStatus(e.getX(),e.getY());
			}

			@Override public void mouseEntered (MouseEvent e) { showStatus(e.getX(),e.getY()); }
			@Override public void mouseMoved   (MouseEvent e) { showStatus(e.getX(),e.getY()); }
			@Override public void mouseExited  (MouseEvent e) { showStatus(-1,-1); }

			@Override public void mouseClicked (MouseEvent e) {}
			@Override public void mousePressed (MouseEvent e) {}
			@Override public void mouseReleased(MouseEvent e) {}
			@Override public void mouseDragged (MouseEvent e) {}
		}
		
		private static class GalaxyMap extends Canvas {
			private static final long serialVersionUID = -6290765806544803046L;

			private static final Color COLOR_BACKGROUND = Color.BLACK;
			private static final Color COLOR_GRID = new Color(0x202020);
			private static final Color COLOR_AXIS = new Color(0x000090);
			private static final Color COLOR_KNOWN_REGION = Color.YELLOW;
			private static final Color COLOR_CURRENT_POS  = Color.MAGENTA;
			private static final Color COLOR_GALAXY_CENTER = Color.RED;
			
			private static final int MAP_WIDTH  = 4096;
			private static final int MAP_HEIGHT = 4096;
			private static final int MAP_CENTER_X = 2047;
			private static final int MAP_CENTER_Y = 2047;
			private static final int MAP_GRID = 16;
			
			private static final double ZOOM_INC = 1.1;
			
			private Galaxy galaxy;
			private BufferedImage mapBaseImage;
			private BufferedImage mapImage;

			private int offsetX;
			private int offsetY;
			private int scaledMapWidth;
			private int scaledMapHeight;
			private double zoomRatio;

			private CombinedListener combiListener;
			private UniverseAddress currentPos;
			private Long knownGlyphs;
			private boolean showMarkers;
			
			GalaxyMap(CombinedListener combiListener, Galaxy galaxy, UniverseAddress currentPos, Long knownGlyphs) {
				this.combiListener = combiListener;
				//withDebugOutput = true;
				this.galaxy = galaxy;
				this.currentPos = currentPos;
				this.knownGlyphs = knownGlyphs;
				this.mapBaseImage = null;
				this.mapImage = null;
				this.offsetX = 0;
				this.offsetY = 0;
				this.scaledMapWidth  = MAP_WIDTH;
				this.scaledMapHeight = MAP_HEIGHT;
				this.zoomRatio = 1.0;
				this.showMarkers = false;
			}

			public void showMarkers(boolean showMarkers) {
				this.showMarkers = showMarkers;
				repaint();
			}

			public void setGalaxy(Galaxy galaxy) {
				this.galaxy = galaxy;
				prepareMap();
				repaint();
			}

			public int computeVoxelX(int screenX) { return (int) Math.floor((screenX+offsetX)/zoomRatio)-MAP_CENTER_X; }
			public int computeVoxelZ(int screenY) { return (int) Math.floor((screenY+offsetY)/zoomRatio)-MAP_CENTER_Y; }

			public int computeScreenX(int voxelX) { return (int) Math.round((voxelX+0.5+MAP_CENTER_X)*zoomRatio-offsetX); }
			public int computeScreenY(int voxelZ) { return (int) Math.round((voxelZ+0.5+MAP_CENTER_Y)*zoomRatio-offsetY); }

			public int getViewWidth () { return width; }
			public int getViewHeight() { return height; }

			public Worker showGlyphOverlay(int numberOfKnownGlyphs) {
				if (numberOfKnownGlyphs==0 || (numberOfKnownGlyphs==17 && knownGlyphs==null)) {
					mapImage = mapBaseImage;
					repaint();
//					System.out.printf(Locale.ENGLISH, "showGlyphOverlay( %d ) -> disable overlay\r\n", numberOfKnownGlyphs);
					return null;
				}
				
				return new Worker() {
					@Override public void run() {
						setTaskTitle("Create portal glyph overlay");
						
						BufferedImage newMapImage = createEmptyMap();
						Graphics graphics = newMapImage.getGraphics();
						graphics.drawImage(mapBaseImage,0,0,null);
						
						if (numberOfKnownGlyphs<17) {
							// N consecutive glyphs
							int N = numberOfKnownGlyphs;
							setProgress(0,N*N);
							graphics.setColor(new Color(0x00,0xFF,0x00,0x7F));
							for (int x1=0; (x1<N) && !stopNow; ++x1)
								for (int y1=0; (y1<N) && !stopNow; ++y1) {
									setProgress(x1*N+y1);
									for (int x2=0; (x2<N) && !stopNow; ++x2)
										for (int y2=0; (y2<N) && !stopNow; ++y2)
											drawRect(graphics, x1*16+x2*256+MAP_CENTER_X, y1*16+y2*256+MAP_CENTER_Y, N, N);
								}
							setProgress(N*N);
						} else {
							int bitmask = (int)(long)knownGlyphs;
							// bit mask of known glyphs
							setProgress(0,MAP_WIDTH);
							graphics.setColor(new Color(0x00,0xFF,0x00,0x7F));
							for (int x=0; (x<MAP_WIDTH) && !stopNow; ++x) {
								setProgress(x);
								int cX = (x-MAP_CENTER_X)&0xFFF;
								if (((bitmask>>((cX>>8)&0xF))&1)==0) continue;
								if (((bitmask>>((cX>>4)&0xF))&1)==0) continue;
								if (((bitmask>>((cX>>0)&0xF))&1)==0) continue;
								for (int y=0; (y<MAP_HEIGHT) && !stopNow; ++y) {
									int cY = (y-MAP_CENTER_Y)&0xFFF;
									if (((bitmask>>((cY>>8)&0xF))&1)==0) continue;
									if (((bitmask>>((cY>>4)&0xF))&1)==0) continue;
									if (((bitmask>>((cY>>0)&0xF))&1)==0) continue;
									graphics.fillRect(x,y,1,1);
								}
							}
							setProgress(MAP_WIDTH);
						}
						
						if (!stopNow) {
							setMapImage(newMapImage);
							repaint();
//							System.out.printf(Locale.ENGLISH, "showGlyphOverlay( %d ) -> enable overlay\r\n", numberOfKnownGlyphs);
						}
					}

					private void drawRect(Graphics graphics, int x, int y, int width, int height) {
						while (x >= MAP_WIDTH ) x -= MAP_WIDTH;
						while (y >= MAP_HEIGHT) y -= MAP_HEIGHT;
						if (x+width > MAP_WIDTH) {
							drawRect(graphics, x, y, MAP_WIDTH-x, height);
							drawRect(graphics, 0, y, x+width-MAP_WIDTH, height);
						}
						if (y+height > MAP_HEIGHT) {
							drawRect(graphics, x, y, width, MAP_HEIGHT-y);
							drawRect(graphics, x, 0, width, y+height-MAP_HEIGHT);
						}
						graphics.fillRect(x,y,width,height);
					}
				};
			}

			public void prepareMap() {
				if (galaxy==null) {
					mapBaseImage = null;
					setMapImage(null);
					return;
				}
				prepareBaseMap();
				copyToMapImage(mapBaseImage);
			}
			
			private synchronized void setMapImage(BufferedImage newMapImage) {
				mapImage = newMapImage;
			}
			
			private synchronized void copyToMapImage(Image newMapImage) {
				mapImage = createEmptyMap();
				Graphics graphics = mapImage.getGraphics();
				graphics.drawImage(newMapImage,0,0,null);
			}

			private synchronized boolean isMapImageNull() {
				return mapImage==null;
			}

			private void prepareBaseMap() {
				mapBaseImage = createEmptyMap();
				Graphics graphics = mapBaseImage.getGraphics();
				
				graphics.setColor(COLOR_BACKGROUND);
				graphics.fillRect(0, 0, MAP_WIDTH, MAP_HEIGHT);
				
				graphics.setColor(COLOR_GRID);
				for (int x=MAP_CENTER_X%MAP_GRID; x<MAP_WIDTH ; x+=MAP_GRID) graphics.drawLine(x, 0, x, MAP_HEIGHT);
				for (int y=MAP_CENTER_Y%MAP_GRID; y<MAP_HEIGHT; y+=MAP_GRID) graphics.drawLine(0, y, MAP_WIDTH, y);
				
				graphics.setColor(COLOR_AXIS);
				graphics.drawLine(MAP_CENTER_X, 0, MAP_CENTER_X, MAP_HEIGHT);
				graphics.drawLine(0, MAP_CENTER_Y, MAP_WIDTH, MAP_CENTER_Y);
				
				graphics.setColor(COLOR_KNOWN_REGION);
				for (Region region:galaxy.regions) {
					graphics.fillRect(region.voxelX+MAP_CENTER_X, region.voxelZ+MAP_CENTER_Y, 1, 1);
				}
				graphics.setColor(COLOR_CURRENT_POS);
				graphics.fillRect(currentPos.voxelX+MAP_CENTER_X, currentPos.voxelZ+MAP_CENTER_Y, 1, 1);
				
				graphics.setColor(COLOR_GALAXY_CENTER);
				graphics.fillRect(MAP_CENTER_X, MAP_CENTER_Y, 1, 1);
			}

			private BufferedImage createEmptyMap() {
				return new BufferedImage(MAP_WIDTH, MAP_HEIGHT, BufferedImage.TYPE_INT_ARGB);
			}

			public void setZoom(double value) {
				incZoom(width/2,height/2, value/zoomRatio);
//				System.out.printf(Locale.ENGLISH, "setZoom( %f ) -> offset:%d,%d zoom:%f\r\n", value, offsetX,offsetY,zoomRatio);
			}

			public void incZoom(int zoomCenterX, int zoomCenterY, int zoomInc) {
				incZoom(zoomCenterX, zoomCenterY, Math.pow(ZOOM_INC, zoomInc));
//				System.out.printf(Locale.ENGLISH, "incZoom( %d,%d,%d, ) -> offset:%d,%d zoom:%f\r\n", zoomCenterX,zoomCenterY,zoomInc, offsetX,offsetY,zoomRatio);
			}

			private void incZoom(int zoomCenterX, int zoomCenterY, double zoomRatioInc) {
				if (this.zoomRatio<0.1 && zoomRatioInc<1) return;
				
				this.zoomRatio *= zoomRatioInc;
				if ((1/ZOOM_INC+1)/2<zoomRatio && zoomRatio<(ZOOM_INC+1)/2) zoomRatio=1.0; // normalize zoomRatio to 1 if it's near 1
				
				offsetX = (int) (Math.round((offsetX+zoomCenterX)*zoomRatioInc)-zoomCenterX);
				offsetY = (int) (Math.round((offsetY+zoomCenterY)*zoomRatioInc)-zoomCenterY);
				scaledMapWidth  = (int)Math.round(MAP_WIDTH*zoomRatio);
				scaledMapHeight = (int)Math.round(MAP_HEIGHT*zoomRatio);
				
				offsetsChanged();
				combiListener.setHorizScrollBar(0,offsetX,scaledMapWidth ,width );
				combiListener.setVertScrollBar (0,offsetY,scaledMapHeight,height);
			}

			public void incYOffset(int offsetYInc) {
				setYOffset(offsetY+offsetYInc);
			}
			
			public void setYOffset(int offsetY) {
				this.offsetY = offsetY;
				offsetsChanged();
				combiListener.setVertScrollBar (0,this.offsetY,scaledMapHeight,height);
//				System.out.printf(Locale.ENGLISH, "incYOffset( %d ) -> offsetY:%d\r\n", offsetYInc, offsetY);
			}

			public void incXOffset(int offsetXInc) {
				setXOffset(offsetX+offsetXInc);
			}

			public void setXOffset(int offsetX) {
				this.offsetX = offsetX;
				offsetsChanged();
				combiListener.setHorizScrollBar(0,this.offsetX,scaledMapWidth ,width );
//				System.out.printf(Locale.ENGLISH, "incXOffset( %d ) -> offsetX:%d\r\n", offsetXInc, offsetX);
			}
			
			private void offsetsChanged() {
				offsetX = Math.min(offsetX,scaledMapWidth-width);
				offsetX = Math.max(offsetX,0);
				offsetY = Math.min(offsetY,scaledMapHeight-height);
				offsetY = Math.max(offsetY,0);
				repaint();
			}
			
			@Override
			protected void paintCanvas(Graphics g, int width, int height) {
				if (isMapImageNull()) return;
				if (!(g instanceof Graphics2D)) return;
				Graphics2D g2 = (Graphics2D)g;
				
				g2.setClip(new Rectangle(0, 0, width, height));
				
				AffineTransform transform = new AffineTransform();
//				transform.setToScale(zoomRatio, zoomRatio);
//				transform.translate(-offsetX,-offsetY);
				transform.setToTranslation(-offsetX,-offsetY);
				transform.scale(zoomRatio, zoomRatio);
				
				int type = zoomRatio<1.0?AffineTransformOp.TYPE_BICUBIC:AffineTransformOp.TYPE_NEAREST_NEIGHBOR;
				AffineTransformOp op = new AffineTransformOp(transform, type);
				synchronized(this) {
					g2.drawImage(mapImage, op, 0, 0);
				}
				
				if (showMarkers) {
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.setColor(COLOR_KNOWN_REGION);
					int markerSize = 5;
					for (Region region:galaxy.regions)
						if (region.voxelX!=currentPos.voxelX && region.voxelY!=currentPos.voxelY)
							drawMarker(g2, region.voxelX, region.voxelZ, markerSize);
					
					g2.setColor(COLOR_CURRENT_POS);
					drawMarker(g2, currentPos.voxelX, currentPos.voxelZ, markerSize);
					
					g2.setColor(COLOR_GALAXY_CENTER);
					drawMarker(g2, 0,0, markerSize);
					
				}
			}

			private void drawMarker(Graphics2D g2, int voxelX, int voxelZ, int size) {
				int x = computeScreenX(voxelX);
				int y = computeScreenY(voxelZ);
				if (0<=x && x<this.width && 0<=y && y<this.height) {
					g2.drawLine(x-size,y-size,x+size,y+size);
					g2.drawLine(x+size,y-size,x-size,y+size);
				}
			}

			@Override
			protected void sizeChanged(int width, int height) {
				offsetsChanged();
				combiListener.setHorizScrollBar(0,offsetX,scaledMapWidth ,width );
				combiListener.setVertScrollBar (0,offsetY,scaledMapHeight,height);
			}
		}
	}
	
	static class EnumMap2K<K1 extends Enum<K1>, K2 extends Enum<K2>, V> {
		
		private K1[] keys1;
		private K2[] keys2;
		private Object[][] values;

		EnumMap2K(Class<K1> keyType1, Class<K2> keyType2) {
			keys1 = getKeyUniverse(keyType1);
			keys2 = getKeyUniverse(keyType2);
			values = new Object[keys1.length][keys2.length];
			for (K1 k1:keys1)
				for (K2 k2:keys2)
					put(k1,k2,null);
		}
		
		void put(K1 key1, K2 key2, V value) {
			values[key1.ordinal()][key2.ordinal()] = value;
		}
		
		@SuppressWarnings("unchecked")
		V get(K1 key1, K2 key2) {
			return (V) values[key1.ordinal()][key2.ordinal()];
		}
		
	    private static <K extends Enum<K>> K[] getKeyUniverse(Class<K> keyType) {
	        return SharedSecrets.getJavaLangAccess().getEnumConstantsShared(keyType);
	    }
	}
}
