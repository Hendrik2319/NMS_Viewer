package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Vector;
import java.util.function.BiConsumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import net.schwarzbaer.gui.Canvas;
import net.schwarzbaer.gui.Disabler;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.gui.StandardMainWindow.DefaultCloseOperation;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.gui.Tables.SimplifiedColumnIDInterface;
import net.schwarzbaer.gui.Tables.SimplifiedTableModel;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.UniverseAddress;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveViewer.ToolbarIcons;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView;

public class ResourceHotSpots implements ActionListener {
	
	private static final String RESOURCE_HOTSPOTS_DATA = "NMS_Viewer.ResourceHotSpots.data";
	private static final String RESOURCE_HOTSPOTS_CFG = "NMS_Viewer.ResourceHotSpots.cfg";

	private StandardMainWindow mainwindow = null;
	private TableView.SimplifiedTable referencePointsTable = null;
	private TableView.SimplifiedTable circlesTable = null;
	private TableView.SimplifiedTable hotSpotsTable = null;
	private Disabler<ActionCommand>   disabler = null;

	private ReferencePointsTableModel referencePointsTableModel = null;
	private CirclesTableModel         circlesTableModel = null;
	private HotSpotsTableModel        hotSpotsTableModel = null;
	private HotSpotsView              hotSpotsView = null;
	
	private Vector<Planet> planets = new Vector<>();
	private Planet         currentPlanet = null;
	private Planet.Region  currentRegion = null;

	private JComboBox<Planet> planetComboBox = null;
	private JComboBox<Planet.Region> regionComboBox = null;

	private JTextField planetRadiusField;
	
	private WindowConfig windowConfig = null;
	
	private JComboBox<String> hotSpotSubstanceSelect = null;
	private JComboBox<Planet.HotSpot.Type > hotSpotTypeSelect = null;
	private JComboBox<Planet.HotSpot.Class> hotSpotClassSelect = null;

	public static void main(String[] args) {
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		SaveViewer.loadToolbarIcons();
		start(null);
	}
	
	public static void start(SaveGameData.Universe.Planet planet) {
		new ResourceHotSpots()
			.readConfig()
			.readData()
			.preselectPlanet(planet)
			.createGUI(planet==null);
	}
	
	private class WindowConfig {
		public JSplitPane upperTablePanel = null;
		public JSplitPane tablePanel = null;
		public JSplitPane centerPanel = null;
		
		private Dimension mainwindowSize = null;
		private Point mainwindowLocation = null;
		private Integer centerPanelDividerLocation = null;
		private Integer tablePanelDividerLocation = null;
		private Integer upperTablePanelDividerLocation = null;
		
		public void writeToFile(PrintWriter out) {
			if (mainwindowSize==null) mainwindowSize = new Dimension();
			if (mainwindowLocation==null) mainwindowLocation = new Point();
			mainwindow.getSize(mainwindowSize);
			mainwindow.getLocation(mainwindowLocation);
			out.printf("mainwindow.location.x=%d%n", mainwindowLocation.x);
			out.printf("mainwindow.location.y=%d%n", mainwindowLocation.y);
			out.printf("mainwindow.size.width=%d%n", mainwindowSize.width);
			out.printf("mainwindow.size.height=%d%n", mainwindowSize.height);
			out.printf("centerPanel.dividerLocation=%d%n", centerPanel.getDividerLocation());
			out.printf("tablePanel.dividerLocation=%d%n", tablePanel.getDividerLocation());
			out.printf("upperTablePanel.dividerLocation=%d%n", upperTablePanel.getDividerLocation());
		}
		
		public void parseLineFromFile(String line) {
			if (line.startsWith("mainwindow.size.width=")) {
				String valueStr = line.substring("mainwindow.size.width=".length());
				if (mainwindowSize==null) mainwindowSize = new Dimension();
				try { mainwindowSize.width = Integer.parseInt(valueStr); }
				catch (NumberFormatException e) {}
			}
			if (line.startsWith("mainwindow.size.height=")) {
				String valueStr = line.substring("mainwindow.size.height=".length());
				if (mainwindowSize==null) mainwindowSize = new Dimension();
				try { mainwindowSize.height = Integer.parseInt(valueStr); }
				catch (NumberFormatException e) {}
			}
			if (line.startsWith("mainwindow.location.x=")) {
				String valueStr = line.substring("mainwindow.location.x=".length());
				if (mainwindowLocation==null) mainwindowLocation = new Point();
				try { mainwindowLocation.x = Integer.parseInt(valueStr); }
				catch (NumberFormatException e) {}
			}
			if (line.startsWith("mainwindow.location.y=")) {
				String valueStr = line.substring("mainwindow.location.y=".length());
				if (mainwindowLocation==null) mainwindowLocation = new Point();
				try { mainwindowLocation.y = Integer.parseInt(valueStr); }
				catch (NumberFormatException e) {}
			}
			if (line.startsWith("centerPanel.dividerLocation=")) {
				String valueStr = line.substring("centerPanel.dividerLocation=".length());
				try { centerPanelDividerLocation = Integer.parseInt(valueStr); }
				catch (NumberFormatException e) { centerPanelDividerLocation = null; }
			}
			if (line.startsWith("tablePanel.dividerLocation=")) {
				String valueStr = line.substring("tablePanel.dividerLocation=".length());
				try { tablePanelDividerLocation = Integer.parseInt(valueStr); }
				catch (NumberFormatException e) { tablePanelDividerLocation = null; }
			}
			if (line.startsWith("upperTablePanel.dividerLocation=")) {
				String valueStr = line.substring("upperTablePanel.dividerLocation=".length());
				try { upperTablePanelDividerLocation = Integer.parseInt(valueStr); }
				catch (NumberFormatException e) { upperTablePanelDividerLocation = null; }
			}
		}

		public void configureGUI() {
			if (mainwindowSize!=null)
				mainwindow.setSize(mainwindowSize.width, mainwindowSize.height);
			if (mainwindowLocation!=null)
				mainwindow.setLocation(mainwindowLocation.x, mainwindowLocation.y);
			if (centerPanelDividerLocation    !=null)
				centerPanel    .setDividerLocation(centerPanelDividerLocation    );
			if (upperTablePanelDividerLocation!=null)
				upperTablePanel.setDividerLocation(upperTablePanelDividerLocation);
			if (tablePanelDividerLocation     !=null)
				tablePanel     .setDividerLocation(tablePanelDividerLocation     );
		}
	}

	private ResourceHotSpots createGUI(boolean standalone) {
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		
		disabler = new Disabler<ActionCommand>();
		disabler.setCareFor(ActionCommand.values());
		
		if (windowConfig == null)
			windowConfig = new WindowConfig();
		
		referencePointsTableModel = new ReferencePointsTableModel();
		referencePointsTable = new TableView.SimplifiedTable("ReferencePoints", referencePointsTableModel, true, true, true);
		referencePointsTableModel.configureTable(referencePointsTable);
		JScrollPane referencePointsTableScrollPane = new JScrollPane(referencePointsTable);
		referencePointsTableScrollPane.setPreferredSize(new Dimension(400, 200));
		referencePointsTableScrollPane.setBorder(createCompoundBorder("Reference Points"));
		
		circlesTableModel = new CirclesTableModel();
		circlesTable = new TableView.SimplifiedTable("Circles", circlesTableModel, true, true, true);
		circlesTableModel.configureTable(circlesTable);
		JScrollPane circlesTableScrollPane = new JScrollPane(circlesTable);
		circlesTableScrollPane.setPreferredSize(new Dimension(400, 200));
		circlesTableScrollPane.setBorder(createCompoundBorder("Circles"));
		
		hotSpotsTableModel = new HotSpotsTableModel(this::updateHotSpotSubstanceSelect);
		hotSpotsTable = new TableView.SimplifiedTable("HotSpots", hotSpotsTableModel, true, true, true);
		hotSpotsTableModel.configureTable(hotSpotsTable);
		JScrollPane hotSpotsTableScrollPane = new JScrollPane(hotSpotsTable);
		hotSpotsTableScrollPane.setPreferredSize(new Dimension(400, 300));
		hotSpotsTableScrollPane.setBorder(createCompoundBorder("HotSpots"));
		
		windowConfig.upperTablePanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		windowConfig.upperTablePanel.setResizeWeight(0);
		windowConfig.upperTablePanel.setTopComponent(referencePointsTableScrollPane);
		windowConfig.upperTablePanel.setBottomComponent(circlesTableScrollPane);
		
		windowConfig.tablePanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		windowConfig.tablePanel.setResizeWeight(0);
		windowConfig.tablePanel.setTopComponent(windowConfig.upperTablePanel);
		windowConfig.tablePanel.setBottomComponent(hotSpotsTableScrollPane);
		
		hotSpotsView = new HotSpotsView();
		hotSpotsView.setPreferredSize(new Dimension(400,600));
		
		hotSpotTypeSelect      = new JComboBox<Planet.HotSpot.Type >(SaveViewer.addNull(Planet.HotSpot.Type .values()));
		hotSpotClassSelect     = new JComboBox<Planet.HotSpot.Class>(SaveViewer.addNull(Planet.HotSpot.Class.values()));
		hotSpotSubstanceSelect = new JComboBox<String>();
		SaveViewer.setComp(hotSpotTypeSelect     , disabler, ActionCommand.SelectHotSpotType     , true, hotSpotsView::setSelectCriteria);
		SaveViewer.setComp(hotSpotClassSelect    , disabler, ActionCommand.SelectHotSpotClass    , true, hotSpotsView::setSelectCriteria);
		SaveViewer.setComp(hotSpotSubstanceSelect, disabler, ActionCommand.SelectHotSpotSubstance, true, hotSpotsView::setSelectCriteria);
		
		JPanel hotSpotsSelectPanel = new JPanel(new GridBagLayout());
		hotSpotsSelectPanel.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
		c.weightx=0;
		hotSpotsSelectPanel.add(new JLabel("Type: "),c);
		hotSpotsSelectPanel.add(hotSpotTypeSelect,c);
		hotSpotsSelectPanel.add(new JLabel("   Substance: "),c);
		hotSpotsSelectPanel.add(hotSpotSubstanceSelect,c);
		hotSpotsSelectPanel.add(new JLabel("   Class: "),c);
		hotSpotsSelectPanel.add(hotSpotClassSelect,c);
		c.weightx=1;
		hotSpotsSelectPanel.add(new JLabel(),c);
		
		JPanel hotSpotsViewPanel = new JPanel(new BorderLayout());
		hotSpotsViewPanel.setBorder(createCompoundBorder("HotSpots View"));
		hotSpotsViewPanel.add(hotSpotsSelectPanel,BorderLayout.NORTH);
		hotSpotsViewPanel.add(hotSpotsView,BorderLayout.CENTER);
		referencePointsTableModel.setHotSpotsView(hotSpotsView);
		circlesTableModel        .setHotSpotsView(hotSpotsView);
		hotSpotsTableModel       .setHotSpotsView(hotSpotsView);
		
		windowConfig.centerPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		windowConfig.centerPanel.setResizeWeight(0);
		windowConfig.centerPanel.setLeftComponent(windowConfig.tablePanel);
		windowConfig.centerPanel.setRightComponent(hotSpotsViewPanel);
		
		planetComboBox = new JComboBox<Planet>(planets);
		regionComboBox = new JComboBox<>();
		SaveViewer.setComp(planetComboBox,this,disabler,ActionCommand.SelectPlanet, true);
		SaveViewer.setComp(regionComboBox,this,disabler,ActionCommand.SelectRegion, true);
		planetComboBox.setMinimumSize(new Dimension(100,16));
		regionComboBox.setMinimumSize(new Dimension(100,16));
		
		JLabel planetRadiusLabel = new JLabel("    Radius: ");
		disabler.add(ActionCommand.ChangePlanetRadius, planetRadiusLabel);
		planetRadiusField = SaveViewer.createTextField("", this, disabler, ActionCommand.ChangePlanetRadius);
		planetRadiusField.setColumns(10);
		planetRadiusField.setEnabled(false);
		planetRadiusField.setMinimumSize(new Dimension(70,16));
		
		c.weightx=0;
		JPanel planetPanel = new JPanel(new GridBagLayout());
		planetPanel.setBorder(BorderFactory.createTitledBorder("Planet"));
		planetPanel.add(planetComboBox,c);
		planetPanel.add(SaveViewer.createButton("Change Name", this, disabler, ActionCommand.ChangePlanetName),c);
		planetPanel.add(planetRadiusLabel,c);
		planetPanel.add(planetRadiusField,c);
		
		JPanel regionPanel = new JPanel(new GridBagLayout());
		regionPanel.setBorder(BorderFactory.createTitledBorder("Region"));
		regionPanel.add(SaveViewer.createButton("Add Region" , this, disabler, ActionCommand.AddRegion),c);
		regionPanel.add(regionComboBox,c);
		regionPanel.add(SaveViewer.createButton("Change Name", this, disabler, ActionCommand.ChangeRegionName),c);
		
		c.weightx=0;
		JPanel selectPanel = new JPanel(new GridBagLayout());
		selectPanel.add(planetPanel,c);
		selectPanel.add(regionPanel,c);
		c.weightx=1;
		selectPanel.add(new JLabel(" "),c);
		
		JPanel contentPane = new JPanel(new BorderLayout(3,3));
		contentPane.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
		contentPane.add(selectPanel,BorderLayout.NORTH);
		contentPane.add(windowConfig.centerPanel,BorderLayout.CENTER);
		
		JMenu menuData = new JMenu("Data");
		//menuData.add(SaveViewer.createMenuItem("Clear data"                , this, disabler, ActionCommand.ClearData     , ToolbarIcons.Delete));
		menuData.add(SaveViewer.createMenuItem("Write data to file"        , this, disabler, ActionCommand.SaveDataFile  , ToolbarIcons.Save));
		
		JMenu menuView = new JMenu("View");
		menuView.add(SaveViewer.createMenuItem("Save current configuration of this window", this, disabler, ActionCommand.SaveWindowConfig, ToolbarIcons.Save));
		
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(menuData);
		menuBar.add(menuView);
		
		mainwindow = new StandardMainWindow("Resource HotSpots",standalone?DefaultCloseOperation.EXIT_ON_CLOSE:DefaultCloseOperation.HIDE_ON_CLOSE);
		mainwindow.startGUI(contentPane,menuBar);
		
		//SaveViewer.log_ln("ResourceHotSpots.currentPlanet: %s <initial>", currentPlanet);
		//SaveViewer.log_ln("ResourceHotSpots.currentRegion: %s <initial>", currentRegion);
		planetComboBox.setSelectedItem(currentPlanet);
		regionComboBox.setSelectedItem(currentRegion);
		updateGuiAccess();
		
		SwingUtilities.invokeLater(() -> {
			//mainwindow.setSize(1500, 900);
			windowConfig.configureGUI();
		});
		return this;
	}

	private void updateHotSpotSubstanceSelect() {
		if (currentRegion==null)
			hotSpotSubstanceSelect.setModel(new DefaultComboBoxModel<>());
		else {
			HashSet<String> substances = new HashSet<>();
			currentRegion.hotSpots.forEach(hotSpot->{
				if (hotSpot.substance!=null && !hotSpot.substance.isEmpty())
					substances.add(hotSpot.substance);
			});
			Vector<String> strs = new Vector<>(substances);
			strs.sort(null);
			strs.insertElementAt(null,0);
			hotSpotSubstanceSelect.setModel(new DefaultComboBoxModel<>(strs));
		}
	}

	private void updateGuiAfterRegionChange() {
		if (referencePointsTableModel!=null) referencePointsTableModel.setRegion(currentRegion);
		if (circlesTableModel        !=null) circlesTableModel        .setRegion(currentRegion);
		if (hotSpotsTableModel       !=null) hotSpotsTableModel       .setRegion(currentRegion);
		if (hotSpotsView             !=null) hotSpotsView             .setRegion(currentPlanet,currentRegion);
		if (hotSpotSubstanceSelect   !=null) updateHotSpotSubstanceSelect();
		if (hotSpotTypeSelect        !=null) hotSpotTypeSelect.setSelectedItem(null);
		if (hotSpotClassSelect       !=null) hotSpotClassSelect.setSelectedItem(null);
	}

	private void updatePlanetRadiusField() {
		planetRadiusField.setText( currentPlanet==null || currentPlanet.radius==null ? "" : String.format(Locale.ENGLISH, "%1.2f", currentPlanet.radius) );
	}

	private void updateGuiAccess() {
		if (disabler==null) return;
		disabler.setEnable(ac->{
			switch (ac) {
			
			case SaveWindowConfig:
				return true;
				
			case ClearData:
			case SaveDataFile:
			case SelectPlanet:
				return !planets.isEmpty();
				
			case ChangePlanetName:
			case ChangePlanetRadius:
			case AddRegion:
				return currentPlanet!=null;
				
			case SelectRegion:
				return currentPlanet!=null && !currentPlanet.regions.isEmpty();
				
			case ChangeRegionName:
			case SelectHotSpotType:
			case SelectHotSpotSubstance:
			case SelectHotSpotClass:
				return currentPlanet!=null && currentRegion!=null;
				
			}
			return null;
		});
	}

	enum ActionCommand {
		ClearData,
		SaveDataFile,
		ChangePlanetRadius,
		AddRegion,
		SelectPlanet, SelectRegion,
		ChangePlanetName, ChangeRegionName,
		SaveWindowConfig,
		SelectHotSpotType, SelectHotSpotClass, SelectHotSpotSubstance,
		;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		switch(ActionCommand.valueOf(e.getActionCommand())) {
		
		case ClearData:
			break;
			
		case SaveDataFile:
			writeData();
			break;
		case SaveWindowConfig:
			writeConfig();
			break;
			
		case AddRegion:
			if (currentPlanet!=null) {
				currentPlanet.regions.add( currentRegion = new Planet.Region("<New Region>") );
				regionComboBox.setModel(new DefaultComboBoxModel<Planet.Region>(currentPlanet.regions));
				regionComboBox.setSelectedItem(currentRegion);
			}
			updateGuiAccess();
			break;
			
		case SelectHotSpotType: break;
		case SelectHotSpotSubstance: break;
		case SelectHotSpotClass: break;
			
		case SelectPlanet:
			currentPlanet = (Planet)planetComboBox.getSelectedItem();
			//SaveViewer.log_ln("ResourceHotSpots.currentPlanet: %s", currentPlanet);
			if (currentPlanet==null) {
				regionComboBox.setModel(new DefaultComboBoxModel<Planet.Region>());
				regionComboBox.setSelectedItem(null);
			} else {
				regionComboBox.setModel(new DefaultComboBoxModel<Planet.Region>(currentPlanet.regions));
				regionComboBox.setSelectedItem(currentPlanet.regions.isEmpty()?null:currentPlanet.regions.firstElement());
			}
			updatePlanetRadiusField();
			updateGuiAccess();
			break;
			
		case SelectRegion:
			currentRegion = (Planet.Region)regionComboBox.getSelectedItem();
			//SaveViewer.log_ln("ResourceHotSpots.currentRegion: %s", currentRegion);
			updateGuiAfterRegionChange();
			updateGuiAccess();
			break;
			
		case ChangePlanetName:
			if (currentPlanet!=null) {
				String name = JOptionPane.showInputDialog(mainwindow, "New name for planet", currentPlanet.name);
				if (name!=null) {
					currentPlanet.name = name;
					planetComboBox.repaint();
				}
			}
			break;
		case ChangeRegionName:
			if (currentRegion!=null) {
				String name = JOptionPane.showInputDialog(mainwindow, "New name for region", currentRegion.name);
				if (name!=null) {
					currentRegion.name = name;
					regionComboBox.repaint();
				}
			}
			break;
			
		case ChangePlanetRadius: {
			if (currentPlanet!=null) {
				String str = planetRadiusField.getText();
				str = str.replace(',','.');
				try { currentPlanet.radius = Float.parseFloat( str ); }
				catch (NumberFormatException e1) {}
				updatePlanetRadiusField();
				hotSpotsView.update();
			}
		} break;
		}
	}

	private CompoundBorder createCompoundBorder(String title) {
		return BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(title),
				BorderFactory.createLineBorder(Color.LIGHT_GRAY)
		);
	}

	private ResourceHotSpots writeConfig() {
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(RESOURCE_HOTSPOTS_CFG), StandardCharsets.UTF_8))) {
			
			out.println("[WindowConfig]");
			windowConfig.writeToFile(out);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return this;
	}
	
	private enum ConfigBlock { WindowConfig }
	private ResourceHotSpots readConfig() {
		ConfigBlock configBlock = null;
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(RESOURCE_HOTSPOTS_CFG), StandardCharsets.UTF_8))) {
			String line;
			while ( (line=in.readLine())!=null ) {
				switch (line) {
				case "":
					configBlock = null;
					break;
					
				case "[WindowConfig]":
					configBlock = ConfigBlock.WindowConfig;
					if (windowConfig==null)
						windowConfig = new WindowConfig();
					break;
				}
				
				if (configBlock!=null)
					switch (configBlock) {
					case WindowConfig:
						windowConfig.parseLineFromFile(line);
						break;
					}
			}
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}
		return this;
	}
	
	private enum DataBlock { Planet, Region, ReferencePoint, Circle, HotSpot }
	
	private ResourceHotSpots preselectPlanet(SaveGameData.Universe.Planet planet) {
		if (planet==null) return this;
		
		for (Planet p:planets)
			if (planet.getUniverseAddress().equals(p.universeAddress))
				currentPlanet = p;
		
		if (currentPlanet==null)
			planets.add( currentPlanet = new Planet(planet.getUniverseAddress(),planet.toString()) );
		
		if (currentPlanet.regions.isEmpty())
			currentPlanet.regions.add( currentRegion = new Planet.Region("<New Region>") );
		
		return this;
	}
	private ResourceHotSpots readData() {
		
		DataBlock currentBlock = null;
		Planet planet = null;
		Planet.Region region = null;
		Planet.ReferencePoint referencePoint = null;
		Planet.Circle circle = null;
		Planet.HotSpot hotSpot = null;
		
		planets.clear();
		currentPlanet = null;
		currentRegion = null;
		
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(RESOURCE_HOTSPOTS_DATA), StandardCharsets.UTF_8))) {
			String line;
			while ( (line=in.readLine())!=null ) {
				switch (line) {
				case "[Planet]":
					currentBlock=DataBlock.Planet;
					region = null; referencePoint = null; circle = null; hotSpot = null;
					planets.add( planet = new Planet("<New Planet>") );
					break;
					
				case "[Region]":
					currentBlock=DataBlock.Region;
					referencePoint = null; circle = null; hotSpot = null;
					planet.regions.add( region = new Planet.Region("<New Region>") );
					break;
					
				case "[ReferencePoint]":
				case "[Circle]":
				case "[HotSpot]":
					if (region == null) {
						if (planet == null)
							planets.add( planet = new Planet("<New Planet>") );
						planet.regions.add( region = new Planet.Region("<New Region>") );
					}
					referencePoint = null; circle = null; hotSpot = null;
					switch (line) {
					case "[ReferencePoint]": currentBlock=DataBlock.ReferencePoint; region.referencePoints.add( referencePoint = new Planet.ReferencePoint() ); break;
					case "[Circle]"        : currentBlock=DataBlock.Circle        ; region.circles        .add( circle         = new Planet.Circle        () ); break;
					case "[HotSpot]"       : currentBlock=DataBlock.HotSpot       ; region.hotSpots       .add( hotSpot        = new Planet.HotSpot       () ); break;
					}
					break;
				}
				
				if (currentBlock==null)
					continue;
				
				switch (currentBlock) {
				case Planet:
					if (line.startsWith("UniverseAddress=")) {
						String valueStr = line.substring("UniverseAddress=".length());
						planet.universeAddress = UniverseAddress.parseAddressStr(valueStr);
						if (planet.universeAddress!=null && planet.name==null)
							planet.name = planet.universeAddress.getCoordinates_Planet();
					}
					if (line.startsWith("name=")) {
						String valueStr = line.substring("name=".length());
						planet.name = valueStr;
					}
					if (line.startsWith("radius=")) {
						String valueStr = line.substring("radius=".length());
						try { planet.radius = Float.parseFloat(valueStr); }
						catch (NumberFormatException e) {}
					}
					break;
					
				case Region:
					if (line.startsWith("name=")) {
						String valueStr = line.substring("name=".length());
						region.name = valueStr;
					}
					break;
					
				case ReferencePoint:
					if (line.startsWith("latitude=")) {
						String valueStr = line.substring("latitude=".length());
						referencePoint.location.setLatitudeStr(valueStr);
					}
					if (line.startsWith("longitude=")) {
						String valueStr = line.substring("longitude=".length());
						referencePoint.location.setLongitudeStr(valueStr);
					}
					if (line.startsWith("name=")) {
						String valueStr = line.substring("name=".length());
						referencePoint.name = valueStr;
					}
					if (line.equals("isBase")) {
						referencePoint.isBase = true;
					}
					break;
				
				case Circle:
					if (line.startsWith("latitude=")) {
						String valueStr = line.substring("latitude=".length());
						circle.center.setLatitudeStr(valueStr);
					}
					if (line.startsWith("longitude=")) {
						String valueStr = line.substring("longitude=".length());
						circle.center.setLongitudeStr(valueStr);
					}
					if (line.startsWith("radius=")) {
						String valueStr = line.substring("radius=".length());
						try { circle.radius = Float.parseFloat(valueStr); }
						catch (NumberFormatException e) {}
					}
					break;
				
				case HotSpot:
					if (line.startsWith("latitude=")) {
						String valueStr = line.substring("latitude=".length());
						hotSpot.location.setLatitudeStr(valueStr);
					}
					if (line.startsWith("longitude=")) {
						String valueStr = line.substring("longitude=".length());
						hotSpot.location.setLongitudeStr(valueStr);
					}
					if (line.startsWith("type=")) {
						String valueStr = line.substring("type=".length());
						try { hotSpot.type = Planet.HotSpot.Type.valueOf(valueStr); }
						catch (Exception e) {}
					}
					if (line.startsWith("substance=")) {
						String valueStr = line.substring("substance=".length());
						hotSpot.substance = valueStr;
					}
					if (line.startsWith("class=")) {
						String valueStr = line.substring("class=".length());
						try { hotSpot.hotSpotClass = Planet.HotSpot.Class.valueOf(valueStr); }
						catch (Exception e) {}
					}
					if (line.startsWith("rate=")) {
						String valueStr = line.substring("rate=".length());
						try { hotSpot.rate = Integer.parseInt(valueStr); }
						catch (NumberFormatException e) {}
					}
					break;
				}
				
			}
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}
		updateGuiAfterRegionChange();
		updateGuiAccess();
		
		return this;
	}
	
	private void writeData() {
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(RESOURCE_HOTSPOTS_DATA), StandardCharsets.UTF_8))) {
			planets.forEach(planet->{
				out.println("[Planet]");
				if (planet.universeAddress!=null) out.printf("UniverseAddress=%s%n", planet.universeAddress.getAddressStr());
				if (planet.name           !=null) out.printf("name=%s%n", planet.name);
				if (planet.radius         !=null) out.printf(Locale.ENGLISH, "radius=%1.2f%n", planet.radius);
				out.println();
				
				planet.regions.forEach(region->{
					out.println("[Region]");
					if (region.name!=null) out.printf("name=%s%n", region.name);
					out.println();
					
					region.referencePoints.forEach(referencePoint->{
						out.println("[ReferencePoint]");
						if (referencePoint.location.latitude !=null) out.printf(Locale.ENGLISH, "latitude=%s%n" , referencePoint.location.getLatitudeStr ());
						if (referencePoint.location.longitude!=null) out.printf(Locale.ENGLISH, "longitude=%s%n", referencePoint.location.getLongitudeStr());
						if (referencePoint.name              !=null) out.printf(Locale.ENGLISH, "name=%s%n"     , referencePoint.name);
						if (referencePoint.isBase                  ) out.printf(Locale.ENGLISH, "isBase%n");
						out.println();
					});
					region.circles.forEach(circle->{
						out.println("[Circle]");
						if (circle.center.latitude !=null) out.printf(Locale.ENGLISH, "latitude=%s%n" , circle.center.getLatitudeStr ());
						if (circle.center.longitude!=null) out.printf(Locale.ENGLISH, "longitude=%s%n", circle.center.getLongitudeStr());
						if (circle.radius          !=null) out.printf(Locale.ENGLISH, "radius=%1.2f%n", circle.radius);
						out.println();
					});
					region.hotSpots.forEach(hotSpot->{
						out.println("[HotSpot]");
						if (hotSpot.location.latitude !=null) out.printf(Locale.ENGLISH, "latitude=%s%n" , hotSpot.location.getLatitudeStr ());
						if (hotSpot.location.longitude!=null) out.printf(Locale.ENGLISH, "longitude=%s%n", hotSpot.location.getLongitudeStr());
						if (hotSpot.type              !=null) out.printf(Locale.ENGLISH, "type=%s%n"     , hotSpot.type);
						if (hotSpot.substance         !=null) out.printf(Locale.ENGLISH, "substance=%s%n", hotSpot.substance);
						if (hotSpot.hotSpotClass      !=null) out.printf(Locale.ENGLISH, "class=%s%n"    , hotSpot.hotSpotClass);
						if (hotSpot.rate              !=null) out.printf(Locale.ENGLISH, "rate=%d%n"     , hotSpot.rate);
						out.println();
					});
				});
			});
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private static int parseInt(Object aValue) throws NumberFormatException {
		if (aValue instanceof Integer)
			return (Integer)aValue;
		if (aValue == null)
			throw new NumberFormatException();
		return Integer.parseInt(aValue.toString());
	}

	private static float parseFloat(Object aValue) throws NumberFormatException {
		if (aValue instanceof Float)
			return (Float)aValue;
		if (aValue == null)
			throw new NumberFormatException();
		String str = aValue.toString();
		str = str.replace(',','.');
		return Float.parseFloat(str);
	}

	private static class LatLong {
		
		Float latitude;
		Float longitude;
		
		private LatLong() {
			this.latitude = null;
			this.longitude = null;
		}
		
		private LatLong(float latitude, float longitude) {
			this.latitude = latitude;
			this.longitude = longitude;
		}

		private LatLong(LatLong other) {
			this.latitude = other.latitude;
			this.longitude = other.longitude;
		}

		public void setLatitudeStr (String aValue) { latitude  = parse(aValue); }
		public void setLongitudeStr(String aValue) { longitude = parse(aValue); }

		public String getLatitudeStr () { return toString(latitude ); }
		public String getLongitudeStr() { return toString(longitude); }
		
		@Override
		public String toString() {
			return "(" + toString(latitude) + "," + toString(longitude) + ")";
		}

		private String toString(Float value) {
			if (value==null) return null;
			return String.format(Locale.ENGLISH, "%s%1.2f", value<0?"":"+", value);
		}
		private Float parse(String str) {
			if (str!=null) {
				str = str.trim();
				if (str.startsWith("+")) str=str.substring(1);
				str = str.replace(',', '.');
				try { return Float.parseFloat(str); }
				catch (NumberFormatException e) {}
			}
			return Float.NaN;
		}

		public void setMin(LatLong location) {
			if (location.latitude!=null) {
				if (latitude==null) latitude = location.latitude;
				else latitude = Math.min( latitude, location.latitude );
			}
			if (location.longitude!=null) {
				if (longitude==null) longitude = location.longitude;
				else longitude = Math.min( longitude, location.longitude );
			}
		}

		public void setMax(LatLong location) {
			if (location.latitude!=null) {
				if (latitude==null) latitude = location.latitude;
				else latitude = Math.max( latitude, location.latitude );
			}
			if (location.longitude!=null) {
				if (longitude==null) longitude = location.longitude;
				else longitude = Math.max( longitude, location.longitude );
			}
		}
		
	}
	
	private static class Planet {
		
		UniverseAddress universeAddress;
		String name;
		Float radius;
		
		Vector<Region> regions;

		Planet(UniverseAddress universeAddress, String name) {
			this.universeAddress = universeAddress;
			this.name = name;
			radius = null;
			regions = new Vector<>();
		}

		Planet(String name) {
			this(null,name);
		}

		@Override
		public String toString() {
			return name;
		}

		private static class Region {
			String name;
			Vector<ReferencePoint> referencePoints;
			Vector<Circle> circles;
			Vector<HotSpot> hotSpots;
			Region(String name) {
				this.name = name;
				referencePoints = new Vector<>();
				circles = new Vector<>();
				hotSpots = new Vector<>();
			}
			
			@Override
			public String toString() {
				return name;
			}
		}

		private static class ReferencePoint extends Location {
			
			public LatLong location;
			public String name;
			public boolean isBase;
			
			ReferencePoint() {
				location = new LatLong();
				name = null;
				isBase = false;
			}

			@Override
			public String toString() {
				return "ReferencePoint [location=" + location + ", name=" + name + ", isBase=" + isBase + "]";
			}

			@Override public LatLong getCoords() { return location; }
			@Override public String getTypeStr() { return "Reference Point"; }
			@Override public String getLabel() { return name; }
		}

		private static class Circle extends Location {
			
			LatLong center;
			Float radius;
			
			Circle() {
				center = new LatLong();
				radius = null;
			}

			@Override public LatLong getCoords() { return center; }
			@Override public String getTypeStr() { return "Circle"; }
			@Override public String getLabel() { return "Circle"; }

			@Override
			public String toString() {
				return "Circle [center=" + center + ", radius=" + radius + "]";
			}
		}

		private static class HotSpot extends Location {
			private enum Type { Mineral, Gas, Energy }
			private enum Class {
				A(300),B(200),C(100);
				private int rate;
				Class(int rate) { this.rate = rate; }
			}
			
			LatLong location;
			Type    type;
			String  substance;
			Class   hotSpotClass;
			Integer rate;
			
			private HotSpot() {
				this.location = new LatLong();
				this.type = null;
				this.substance = null;
				this.hotSpotClass = null;
				this.rate = null;
			}

			@Override
			public String toString() {
				return "HotSpot [location=" + location + ", type=" + type + ", substance=" + substance
						+ ", hotSpotClass=" + hotSpotClass + ", rate=" + rate + "]";
			}

			@Override public LatLong getCoords() { return location; }
			@Override public String getTypeStr() { return "HotSpot"; }
			@Override public String getLabel() {
				return String.format("%s %s, Class %s, Rate %s",
						type==null?"???":type,
						substance==null?"":substance,
						hotSpotClass==null?"??":hotSpotClass,
						rate==null?"??":rate
				);
			}
		}
	}
	
	private static abstract class Location {
		protected abstract LatLong getCoords();
		protected abstract String getTypeStr();
		protected abstract String getLabel();
		@Override public abstract String toString();
	}
	
	private static abstract class LocalTableModel<DataType extends Location, ColumnID extends Enum<ColumnID> & SimplifiedColumnIDInterface> extends SimplifiedTableModel<ColumnID> {
		
		protected Planet.Region region;
		private HotSpotsView hotSpotsView = null;
		
		protected LocalTableModel(ColumnID[] columns) {
			super(columns);
		}
		
		public void setHotSpotsView(HotSpotsView hotSpotsView) {
			this.hotSpotsView = hotSpotsView;
		}
		
		protected void updateHotSpotsView() {
			hotSpotsView.update();
		}

		public void setRegion(Planet.Region region) {
			//SaveViewer.log_ln("%s.setRegion( %s )", getClass().getSimpleName(), region);
			if (table.isEditing()) {
				TableCellEditor tce = table.getCellEditor();
				if (tce!=null) tce.stopCellEditing();
			}
			this.region = region;
			fireTableUpdate();
		}

		private void forEachColumn(JTable table, BiConsumer<TableColumn,Integer> action) {
			TableColumnModel columnModelmodel = table.getColumnModel();
			if (columnModelmodel==null) return;
			
			int n = columnModelmodel.getColumnCount();
			for (int i=0; i<n; i++) {
				TableColumn column = columnModelmodel.getColumn(i);
				action.accept(column,i);
			}
		}

		void configureTable(JTable table) {
			forEachColumn(table, (column,i)->{
				i = table.convertColumnIndexToModel(i);
				configureColumn(column,i,getColumnID(i));
			});
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			table.getSelectionModel().addListSelectionListener(e->selectionChanged(table.getSelectedRow()));
			table.addFocusListener(new FocusListener() {
				@Override public void focusGained(FocusEvent e) {}
				@Override public void focusLost(FocusEvent e) {
					table.clearSelection();
					selectionChanged(-1);
				}
			});
		}
		
		private void selectionChanged(int row) {
			if (row<0) hotSpotsView.setDisplayedLocation(null);
			hotSpotsView.setDisplayedLocation(getItemAt(row));
		}
		
		protected abstract DataType getItemAt(int row);
		protected abstract void configureColumn(TableColumn column, Integer i, ColumnID columnID);
	}

	private enum ReferencePointsTableColumnID implements SimplifiedColumnIDInterface {
		Latitude ("Latitude" ,  String.class, 20,-1, 70,-1),
		Longitude("Longitude",  String.class, 20,-1, 70,-1),
		Name     ("Name"     ,  String.class, 20,-1,150,-1),
		IsBase   ("Is Base?" , Boolean.class, 20,-1, 50,-1),
		;
		private SimplifiedColumnConfig columnConfig;
		ReferencePointsTableColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
			columnConfig = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
		}
		@Override public SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
	}
	
	private static class ReferencePointsTableModel extends LocalTableModel<Planet.ReferencePoint,ReferencePointsTableColumnID> {
		

		protected ReferencePointsTableModel() {
			super(ReferencePointsTableColumnID.values());
		}

		@Override public int getRowCount() { return region==null ? 0 : region.referencePoints.size()+1; }
		@Override protected boolean isCellEditable(int rowIndex, int columnIndex, ReferencePointsTableColumnID columnID) { return true; }
		@Override public void configureColumn(TableColumn column, Integer i, ReferencePointsTableColumnID columnID) {}

		@Override
		protected Planet.ReferencePoint getItemAt(int rowIndex) {
			if (region==null || rowIndex<0 || rowIndex>=region.referencePoints.size())
				return null;
			return region.referencePoints.get(rowIndex);
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex, ReferencePointsTableColumnID columnID) {
			Planet.ReferencePoint referencePoint = getItemAt(rowIndex);
			if (referencePoint==null) return null;
				
			switch (columnID) {
			case Latitude : return referencePoint.location.getLatitudeStr();
			case Longitude: return referencePoint.location.getLongitudeStr();
			case Name     : return referencePoint.name;
			case IsBase   : return referencePoint.isBase;
			}
			return null;
		}

		@Override
		protected void setValueAt(Object aValue, int rowIndex, int columnIndex, ReferencePointsTableColumnID columnID) {
			if (region==null)
				return ;
			
			Planet.ReferencePoint referencePoint;
			if (rowIndex>=region.referencePoints.size()) {
				region.referencePoints.add(referencePoint = new Planet.ReferencePoint());
				fireTableRowAdded(region.referencePoints.size());
			} else
				referencePoint = region.referencePoints.get(rowIndex);
			
			switch (columnID) {
			case Latitude : referencePoint.location.setLatitudeStr((String)aValue); break;
			case Longitude: referencePoint.location.setLongitudeStr((String)aValue); break;
			case Name     : referencePoint.name = (String)aValue; break;
			case IsBase   : referencePoint.isBase = (Boolean)aValue; break;
			}
			updateHotSpotsView();
		}
	
	}

	private enum CirclesTableColumnID implements SimplifiedColumnIDInterface {
		Latitude ("Latitude" ,  String.class, 20,-1,70,-1),
		Longitude("Longitude",  String.class, 20,-1,70,-1),
		Radius   ("Radius"   ,  String.class, 20,-1,70,-1),
		;
		private SimplifiedColumnConfig columnConfig;
		CirclesTableColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
			columnConfig = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
		}
		@Override public SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
	}

	private static class CirclesTableModel extends LocalTableModel<Planet.Circle,CirclesTableColumnID> {
		
		protected CirclesTableModel() {
			super(CirclesTableColumnID.values());
		}

		@Override
		protected Planet.Circle getItemAt(int rowIndex) {
			if (region==null || rowIndex<0 || rowIndex>=region.circles.size())
				return null;
			return region.circles.get(rowIndex);
		}
	
		@Override
		public void configureColumn(TableColumn column, Integer i, CirclesTableColumnID columnID) {}
	
		@Override public int getRowCount() { return region==null ? 0 : region.circles.size()+1; }
	
		@Override
		protected boolean isCellEditable(int rowIndex, int columnIndex, CirclesTableColumnID columnID) {
			return true;
		}
	
		@Override
		public Object getValueAt(int rowIndex, int columnIndex, CirclesTableColumnID columnID) {
			Planet.Circle circle = getItemAt(rowIndex);
			if (circle==null) return null;
			
			switch (columnID) {
			case Latitude : return circle.center.getLatitudeStr();
			case Longitude: return circle.center.getLongitudeStr();
			case Radius   : if (circle.radius==null) return null; return String.format(Locale.ENGLISH, "%1.2f", circle.radius);
			}
			return null;
		}
	
		@Override
		protected void setValueAt(Object aValue, int rowIndex, int columnIndex, CirclesTableColumnID columnID) {
			if (region==null)
				return ;
			
			Planet.Circle circle;
			if (rowIndex>=region.circles.size()) {
				region.circles.add(circle = new Planet.Circle());
				fireTableRowAdded(region.circles.size());
			} else
				circle = region.circles.get(rowIndex);
			
			switch (columnID) {
			case Latitude : circle.center.setLatitudeStr((String)aValue); break;
			case Longitude: circle.center.setLongitudeStr((String)aValue); break;
			case Radius   : try { circle.radius = parseFloat(aValue); } catch (NumberFormatException e) { fireTableRowUpdate(rowIndex); } break;
			}
			updateHotSpotsView();
		}
	
	}

	private enum HotSpotsTableColumnID implements SimplifiedColumnIDInterface {
		Latitude ("Latitude" ,  String.class, 20,-1, 70,-1),
		Longitude("Longitude",  String.class, 20,-1, 70,-1),
		Type     ("Type"     ,  Planet.HotSpot.Type.class, 20,-1, 80,-1),
		Substance("Substance",  String.class, 20,-1, 60,-1),
		Class    ("Class"    ,  Planet.HotSpot.Class.class, 20,-1, 40,-1),
		Rate     ("Rate"     , Integer.class, 20,-1, 60,-1),
		;
		private SimplifiedColumnConfig columnConfig;
		HotSpotsTableColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
			columnConfig = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
		}
		@Override public SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
	}
	
	private static class HotSpotsTableModel extends LocalTableModel<Planet.HotSpot,HotSpotsTableColumnID> {

		private SubstanceChangeListener substanceChangeListener;

		protected HotSpotsTableModel(SubstanceChangeListener substanceChangeListener) {
			super(HotSpotsTableColumnID.values());
			this.substanceChangeListener = substanceChangeListener;
		}
		
		private interface SubstanceChangeListener {
			void substanceHasChanged();
		}

		@Override
		protected Planet.HotSpot getItemAt(int rowIndex) {
			if (region==null || rowIndex<0 || rowIndex>=region.hotSpots.size())
				return null;
			return region.hotSpots.get(rowIndex);
		}

		@Override
		public void configureColumn(TableColumn column, Integer i, HotSpotsTableColumnID columnID) {
			switch (columnID) {
			case Latitude: break;
			case Longitude: break;
			case Substance: break;
			case Rate: break;
			case Class: column.setCellEditor(new DefaultCellEditor(new JComboBox<Planet.HotSpot.Class>(Planet.HotSpot.Class.values()))); break;
			case Type : column.setCellEditor(new DefaultCellEditor(new JComboBox<Planet.HotSpot.Type >(Planet.HotSpot.Type .values()))); break;
			}
		}

		@Override public int getRowCount() { return region==null? 0 : region.hotSpots.size()+1; }

		@Override
		protected boolean isCellEditable(int rowIndex, int columnIndex, HotSpotsTableColumnID columnID) {
			return true;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex, HotSpotsTableColumnID columnID) {
			Planet.HotSpot hotSpot = getItemAt(rowIndex);
			if (hotSpot==null) return null;
			
			switch (columnID) {
			case Latitude : return hotSpot.location.getLatitudeStr();
			case Longitude: return hotSpot.location.getLongitudeStr();
			case Type     : return hotSpot.type;
			case Substance: return hotSpot.substance;
			case Class    : return hotSpot.hotSpotClass;
			case Rate     : return hotSpot.rate;
			}
			return null;
		}

		@Override
		protected void setValueAt(Object aValue, int rowIndex, int columnIndex, HotSpotsTableColumnID columnID) {
			if (region==null)
				return;
			
			Planet.HotSpot hotSpot;
			if (rowIndex>=region.hotSpots.size()) {
				region.hotSpots.add(hotSpot = new Planet.HotSpot());
				fireTableRowAdded(region.hotSpots.size());
			} else
				hotSpot = region.hotSpots.get(rowIndex);
			
			switch (columnID) {
			case Latitude : hotSpot.location.setLatitudeStr ((String)aValue); break;
			case Longitude: hotSpot.location.setLongitudeStr((String)aValue); break;
			case Type     : hotSpot.type = (Planet.HotSpot.Type)aValue; break;
			case Substance: hotSpot.substance = (String)aValue; substanceChangeListener.substanceHasChanged(); break;
			case Class    : hotSpot.hotSpotClass = (Planet.HotSpot.Class)aValue; if (hotSpot.rate==null && hotSpot.hotSpotClass!=null) { hotSpot.rate=hotSpot.hotSpotClass.rate; fireTableRowUpdate(rowIndex); } break;
			case Rate     : try { hotSpot.rate = parseInt(aValue); } catch (NumberFormatException e) { fireTableRowUpdate(rowIndex); }
			}
			updateHotSpotsView();
		}
	}
	
	private static class HotSpotsView extends Canvas {

		private static final long serialVersionUID = 3631270386892323918L;

		private static final BasicStroke STROKE_DASHED_LINE = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{2f,3f}, 0f);

		private static final Color COLOR_REFERENCEPOINT         = new Color(0x5EB91E);
		private static final Color COLOR_HOTSPOT_ENERGY         = new Color(0xFC4200);
		private static final Color COLOR_HOTSPOT_GAS            = new Color(0xFFCC00);
		private static final Color COLOR_HOTSPOT_MINERAL        = new Color(0x5A84B1);
		private static final Color COLOR_HOTSPOT_FAILS_CRITERIA = new Color(0xD0D0D0);
		private static final Color COLOR_AXIS                   = new Color(0x70000000,true);
		private static final Color COLOR_BASE_RANGE             = Color.BLACK;
		private static final Color COLOR_CIRCLE_HIGHLIGHTED     = Color.GRAY;
		private static final Color COLOR_CIRCLE_FILL            = new Color(0xEFEFEF);
		private static final Color COLOR_CIRCLE_FILL2           = new Color(0xF7F7F7);
		
		private static final Polygon POLYGON_SMALL_HOUSE = new Polygon(new int[] {0,5,3,3,-3,-3,-5}, new int[] {-4,1,1,3,3,1,1}, 7);
		
		//private static final int nMajorTicksPerAxis = 2;
		private static final int minMinorTickUnitLength_px = 7;
		private static final int majorTickLength_px = 10;
		private static final int minorTickLength_px = 4;
		private static final int minScaleLength_px = 50;
		private static final int MaxNearDistance = 20;

		
		private Planet planet = null;
		private Planet.Region region = null;

		private Point panStart = null;

		private ViewState viewState = new ViewState();
		private Axes verticalAxes   = new Axes(true);
		private Axes horizontalAxes = new Axes(false);
		private Scale mapScale      = new Scale();

		private Location displayedLocation = null;

		private String filterSubstance = null;
		private Planet.HotSpot.Class filterHotSpotClass = null;
		private Planet.HotSpot.Type filterHotSpotType = null;
		
		HotSpotsView() {
			MouseInputAdapter mouse = new MouseInputAdapter() {
				@Override public void mousePressed   (MouseEvent e) { if (e.getButton()==MouseEvent.BUTTON1) startPan  (e.getPoint()); }
				@Override public void mouseDragged   (MouseEvent e) { proceedPan(e.getPoint());  }
				@Override public void mouseReleased  (MouseEvent e) { if (e.getButton()==MouseEvent.BUTTON1) stopPan   (e.getPoint());  }
				@Override public void mouseWheelMoved(MouseWheelEvent e) { zoom(e.getPoint(),e.getPreciseWheelRotation()); }
				
				@Override public void mouseEntered(MouseEvent e) { requestFocusInWindow(); findNearDisplayableLocation(e.getPoint()); }
				@Override public void mouseMoved  (MouseEvent e) { findNearDisplayableLocation(e.getPoint()); }
				@Override public void mouseExited (MouseEvent e) { displayedLocation=null; repaint(); }
				
				@Override
				public void mouseClicked(MouseEvent e) {
					findNearDisplayableLocation(e.getPoint()); 
				}
			};
			addMouseListener(mouse);
			addMouseMotionListener(mouse);
			addMouseWheelListener(mouse);
		}
		
		@Override
		public void setBorder(Border border) {
			throw new UnsupportedOperationException();
		}

		public void setRegion(Planet planet, Planet.Region region) {
			this.planet = planet;
			this.region = region;
			filterSubstance = null;
			filterHotSpotClass = null;
			filterHotSpotType = null;
			reset();
		}

		public void reset() {
			if (viewState.reset()) {
				updateAxes();
				mapScale.update();
			}
			repaint();
		}

		public void update() {
			if (!viewState.isOk())
				reset();
			repaint();
		}

		public void setSelectCriteria(String filterSubstance) {
			this.filterSubstance = filterSubstance;
			repaint();
		}

		public void setSelectCriteria(Planet.HotSpot.Class filterHotSpotClass) {
			this.filterHotSpotClass = filterHotSpotClass;
			repaint();
		}

		public void setSelectCriteria(Planet.HotSpot.Type filterHotSpotType) {
			this.filterHotSpotType = filterHotSpotType;
			repaint();
		}
		
		private boolean meetsCriteria(Planet.HotSpot item) {
			if (filterHotSpotType !=null && !filterHotSpotType .equals(item.type        )) return false;
			if (filterSubstance   !=null && !filterSubstance   .equals(item.substance   )) return false;
			if (filterHotSpotClass!=null && !filterHotSpotClass.equals(item.hotSpotClass)) return false;
			return true;
		}

		private void findNearDisplayableLocation(Point point) {
			if (region==null || !viewState.isOk()) return;
			
			float maxDistance = MaxNearDistance;
			Location loc,nearest = null;
			
			loc = findNearest(point, maxDistance, region.referencePoints);
			if (loc!=null) { maxDistance = getDistance(loc,point); nearest = loc; }
			
			loc = findNearest(point, maxDistance, region.hotSpots);
			if (loc!=null) { maxDistance = getDistance(loc,point); nearest = loc; }
			
			if (displayedLocation!=nearest) {
				displayedLocation=nearest;
				repaint();
			}
		}
		
		private Location findNearest(Point point, float maxDistance, Vector<? extends Location> locations ) {
			Location nearestLocation = null;
			for (Location loc:locations) {
				float dist = getDistance(loc,point);
				if (!Float.isNaN(dist) && dist<maxDistance) {
					nearestLocation = loc;
					maxDistance = dist;
				}
			}
			return nearestLocation;
		}

		private float getDistance(Location loc, Point point) {
			Point locScreen = viewState.convertPos_AngleToScreen(loc.getCoords());
			if (locScreen==null) return Float.NaN;
			return (float) Math.sqrt( (point.x-locScreen.x)*(point.x-locScreen.x) + (point.y-locScreen.y)*(point.y-locScreen.y) );
		}

		public void setDisplayedLocation(Location displayedLocation) {
			this.displayedLocation = displayedLocation;
			repaint();
		}

		protected void startPan(Point point) {
			panStart = point;
			viewState.tempPanOffset = new Point();
			repaint();
		}

		protected void proceedPan(Point point) {
			if (panStart != null)
				viewState.tempPanOffset = sub(point,panStart);
			repaint();
		}

		protected void stopPan(Point point) {
			if (panStart!=null)
				if (viewState.moveCenter(sub(point,panStart))) {
					updateAxes();
				}
			
			panStart = null;
			viewState.tempPanOffset = null;
			repaint();
		}

		protected void zoom(Point point, double preciseWheelRotation) {
			float f = (float) Math.pow(1.1f, preciseWheelRotation);
			if (viewState.zoom(point,f)) {
				updateAxes();
				mapScale.update();
				repaint();
			}
		}

		private Point sub(Point p1, Point p2) {
			return new Point(p1.x-p2.x,p1.y-p2.y);
		}

		private void updateAxes() {
			if (viewState.isOk()) {
				  verticalAxes.updateTicks( viewState.convertLength_ScreenToAngle_Lat (minMinorTickUnitLength_px) );
				horizontalAxes.updateTicks( viewState.convertLength_ScreenToAngle_Long(minMinorTickUnitLength_px) );
			}
		}

		private static Rectangle2D getBounds(Graphics2D g2, Font font, String str) {
			return font.getStringBounds(str==null?"":str, g2.getFontRenderContext());
		}

		private static Rectangle2D getBounds(Graphics2D g2, String str) {
			return g2.getFontMetrics().getStringBounds(str==null?"":str, g2);
		}

		private static Polygon translatePolygonTo(int x, int y, Polygon polygon) {
			Polygon newPolygon = new Polygon();
			newPolygon.npoints = polygon.npoints;
			newPolygon.xpoints = Arrays.copyOf(polygon.xpoints, newPolygon.npoints);
			newPolygon.ypoints = Arrays.copyOf(polygon.ypoints, newPolygon.npoints);
			newPolygon.translate(x, y);
			return newPolygon;
		}

		@Override
		protected void paintCanvas(Graphics g, int x, int y, int width, int height) {
			if (!(g instanceof Graphics2D)) return;
			Graphics2D g2 = (Graphics2D) g;
			
			if (region != null) {
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setClip(x, y, width, height);
				
				g2.setColor(Color.WHITE);
				g2.fillRect(x, y, width, height);
				
				if (viewState.isOk()) {
					
					region.circles        .forEach(item->draw(g2, item, true));
					region.circles        .forEach(item->draw(g2, item, false));
					region.referencePoints.forEach(item->{ if (item!=displayedLocation) draw(g2,item,false); });
					region.hotSpots       .forEach(item->{ if (item!=displayedLocation) draw(g2,item); });
					
					verticalAxes.drawAxis( g2, x+5      , y+20, height-40, true  );
					verticalAxes.drawAxis( g2, x+width-5, y+20, height-40, false );
					horizontalAxes.drawAxis( g2, y+5       , x+20, width-40, true  );
					horizontalAxes.drawAxis( g2, y+height-5, x+20, width-40, false );
					mapScale.drawScale( g2, x+width-110, y+height-50, 60,15 );
					
					if (displayedLocation != null) {
						
						if (displayedLocation instanceof Planet.Circle) {
							Planet.Circle circle = (Planet.Circle) displayedLocation;
							drawHighlighted(g2,circle);
							
						} else {
							if (displayedLocation instanceof Planet.HotSpot       ) draw(g2, (Planet.HotSpot       ) displayedLocation);
							if (displayedLocation instanceof Planet.ReferencePoint) draw(g2, (Planet.ReferencePoint) displayedLocation, true);
							
							new LocationBox(viewState,width,height,displayedLocation)
								.draw(g2,(iconX,iconY)->{
									if (displayedLocation instanceof Planet.HotSpot) {
										Planet.HotSpot hotSpot = (Planet.HotSpot) displayedLocation;
										drawIcon(g2, hotSpot, iconX, iconY);
									}
									if (displayedLocation instanceof Planet.ReferencePoint) {
										Planet.ReferencePoint referencePoint = (Planet.ReferencePoint) displayedLocation;
										drawIcon(g2, referencePoint, iconX, iconY);
									}
								});
						}
					}
				} else {
				}
			} else {
			}
			
			g2.setClip(null);
		}
		
		private static class LocationBox {
			private static final Color COLOR_BOX_BACKGROUND = new Color(0xfff1cf);
			private static final Color COLOR_LINES = Color.GRAY;
			private static final Color COLOR_TEXT_TYPE = Color.GRAY;
			private static final Color COLOR_TEXT_LABEL = Color.BLACK;
			private static final Point boxOffset = new Point(15,20);
			private static final int boxSpacingToBorder = 30;
			private static final int typeXOffset = 4;
			private static final int border = 4;
			private static final int spacing= 2;
			private static final int iconWidth  = 10;
			private static final int iconHeight = 10;

			private Location displayedLocation;
			private ViewState viewState;
			private int width;
			private int height;

			public LocationBox(ViewState viewState, int width, int height, Location displayedLocation) {
				this.viewState = viewState;
				this.width = width;
				this.height = height;
				this.displayedLocation = displayedLocation;
			}

			public void draw(Graphics2D g2, BiConsumer<Integer,Integer> drawIcon) {
				LatLong loc = displayedLocation.getCoords();
				Point p = viewState.convertPos_AngleToScreen(loc);
				
				if (p==null) return;
				if (p.x<0 || p.x>width ) return;
				if (p.y<0 || p.y>height) return;
				
				String type  = displayedLocation.getTypeStr();
				String label = displayedLocation.getLabel();
				
				Font standardFont = g2.getFont();
				Font typeFont = standardFont.deriveFont(standardFont.getSize2D()*0.6f);
				Rectangle2D typeBounds  = getBounds(g2, typeFont, type);
				Rectangle2D labelBounds = getBounds(g2, standardFont, label);
				
				Rectangle box = new Rectangle();
				Point line = new Point();
				
				box.height = (int) Math.round( typeBounds.getHeight() + Math.max( labelBounds.getHeight(), iconHeight ) + 2*border + 1*spacing );
				box.width  = (int) Math.round( Math.max( typeXOffset + typeBounds.getWidth(), labelBounds.getWidth() + spacing + iconWidth ) + 2*border );
				
				if        (p.x+boxOffset.x+box.width < width-boxSpacingToBorder) {
					box.x  = p.x+boxOffset.x;
					line.x = p.x+boxOffset.x;
				} else if (p.x-boxOffset.x-box.width > boxSpacingToBorder) {
					box.x  = p.x-boxOffset.x-box.width;
					line.x = p.x-boxOffset.x;
				} else {
					box.x  = width/2-box.width/2;
					line.x = width/2;
				}
				
				if        (p.y-boxOffset.y-box.height > boxSpacingToBorder) {
					box.y  = p.y-boxOffset.y-box.height;
					line.y = p.y-boxOffset.y;
				} else if (p.y+boxOffset.y+box.height < height-boxSpacingToBorder) {
					box.y  = p.y+boxOffset.y;
					line.y = p.y+boxOffset.y;
				} else {
					box.y  = height/2-box.height/2;
					line.y = height/2;
				}
				
				g2.setColor(COLOR_LINES);
				g2.drawLine(p.x,p.y, line.x, line.y);
				g2.setColor(COLOR_BOX_BACKGROUND);
				g2.fillRect(box.x, box.y, box.width, box.height);
				g2.setColor(COLOR_LINES);
				g2.drawRect(box.x, box.y, box.width-1, box.height-1);
				
				double x,y;
				g2.setColor(COLOR_TEXT_TYPE);
				g2.setFont(typeFont);
				x = box.x + border + typeXOffset - typeBounds.getX();
				y = box.y + border - typeBounds.getY();
				if (type!=null) g2.drawString(type, (int)Math.round(x), (int)Math.round(y));
				
				g2.setColor(COLOR_TEXT_LABEL);
				g2.setFont(standardFont);
				x = box.x + border + iconWidth + spacing - labelBounds.getX();
				y = box.y + border + typeBounds.getHeight()+ spacing + Math.max(labelBounds.getHeight(),iconHeight)/2-labelBounds.getHeight()/2 - labelBounds.getY();
				if (label!=null) g2.drawString(label, (int)Math.round(x), (int)Math.round(y));
				
				x = box.x + border + iconWidth/2;
				y = box.y + border + typeBounds.getHeight()+ spacing + Math.max(labelBounds.getHeight(),iconHeight)/2;
				drawIcon.accept((int)Math.round(x), (int)Math.round(y));
			}
			
		}
		
		private void drawHighlighted(Graphics2D g2, Planet.Circle item) {
			Point p = viewState.convertPos_AngleToScreen(item.center);
			Integer radius  = viewState.convertLength_LengthToScreen(item.radius);
			if (p!=null && radius!=null) {
				g2.setColor(COLOR_CIRCLE_HIGHLIGHTED);
				Stroke currentStroke = g2.getStroke();
				g2.setStroke(STROKE_DASHED_LINE);
				g2.drawOval(p.x-radius, p.y-radius, radius*2-1, radius*2-1);
				g2.setStroke(currentStroke);
				g2.drawLine(p.x-3,p.y, p.x+3, p.y);
				g2.drawLine(p.x,p.y-3, p.x,p.y+3);
			}
		}

		private void draw(Graphics2D g2, Planet.Circle item, boolean drawOuterCircle) {
			Point p = viewState.convertPos_AngleToScreen(item.center);
			Float radius_u = item.radius;
			if (radius_u!=null && !drawOuterCircle) radius_u -= 10;
			Integer radius_px  = viewState.convertLength_LengthToScreen(radius_u);
			if (p!=null && radius_px!=null) {
				g2.setColor(drawOuterCircle?COLOR_CIRCLE_FILL2:COLOR_CIRCLE_FILL);
				g2.fillOval(p.x-radius_px, p.y-radius_px, radius_px*2, radius_px*2);
			}
		}

		private void draw(Graphics2D g2, Planet.ReferencePoint item, boolean asHighlighted) {
			Point p = viewState.convertPos_AngleToScreen(item.location);
			if (p!=null) {
				drawIcon(g2, item, p.x, p.y);
				if (asHighlighted && item.isBase) {
					int radius = viewState.convertLength_LengthToScreen(300f);
					g2.setColor(COLOR_BASE_RANGE);
					Stroke currentStroke = g2.getStroke();
					g2.setStroke(STROKE_DASHED_LINE);
					g2.drawOval(p.x-radius, p.y-radius, radius*2-1, radius*2-1);
					g2.setStroke(currentStroke);
				}
			}
		}

		private void draw(Graphics2D g2, Planet.HotSpot item) {
			Point p = viewState.convertPos_AngleToScreen(item.location);
			if (p!=null) drawIcon(g2, item, p.x, p.y);
		}

		private void drawIcon(Graphics2D g2, Planet.ReferencePoint item, int x, int y) {
			g2.setColor(COLOR_REFERENCEPOINT);
			if (item.isBase)
				g2.fillPolygon(translatePolygonTo(x,y,POLYGON_SMALL_HOUSE));
			else
				g2.fillOval(x-3, y-3, 6, 6);
		}

		private void drawIcon(Graphics2D g2, Planet.HotSpot item, int x, int y) {
			if (item.type==null || !meetsCriteria(item))
				g2.setColor(COLOR_HOTSPOT_FAILS_CRITERIA); 
			else
				switch (item.type) {
				case Mineral: g2.setColor(COLOR_HOTSPOT_MINERAL); break;
				case Gas    : g2.setColor(COLOR_HOTSPOT_GAS    ); break;
				case Energy : g2.setColor(COLOR_HOTSPOT_ENERGY ); break;
				default: break;
				}
			g2.fillOval(x-3, y-3, 6, 6);
		}

		private class Scale {
		
			private float scaleLength_u = 1;
			private int scaleLength_px = minScaleLength_px;
		
			public void update() {
				if (!viewState.haveScalePixelPerLength()) return;
				
				scaleLength_u = 1;
				
				if (( viewState.convertLength_LengthToScreen(scaleLength_u) < minScaleLength_px) )
					while ( viewState.convertLength_LengthToScreen(scaleLength_u) < minScaleLength_px) {
						float base = scaleLength_u;
						if (viewState.convertLength_LengthToScreen(scaleLength_u) < minScaleLength_px) scaleLength_u = 1.5f*base;
						if (viewState.convertLength_LengthToScreen(scaleLength_u) < minScaleLength_px) scaleLength_u =    2*base;
						if (viewState.convertLength_LengthToScreen(scaleLength_u) < minScaleLength_px) scaleLength_u =    3*base;
						if (viewState.convertLength_LengthToScreen(scaleLength_u) < minScaleLength_px) scaleLength_u =    4*base;
						if (viewState.convertLength_LengthToScreen(scaleLength_u) < minScaleLength_px) scaleLength_u =    5*base;
						if (viewState.convertLength_LengthToScreen(scaleLength_u) < minScaleLength_px) scaleLength_u =    6*base;
						if (viewState.convertLength_LengthToScreen(scaleLength_u) < minScaleLength_px) scaleLength_u =    8*base;
						if (viewState.convertLength_LengthToScreen(scaleLength_u) < minScaleLength_px) scaleLength_u =   10*base;
					}
				else
					while ( viewState.convertLength_LengthToScreen(scaleLength_u*0.80f) > minScaleLength_px) {
						float base = scaleLength_u;
						if (viewState.convertLength_LengthToScreen(base*0.80f) > minScaleLength_px) scaleLength_u = base*0.80f;
						if (viewState.convertLength_LengthToScreen(base*0.60f) > minScaleLength_px) scaleLength_u = base*0.60f;
						if (viewState.convertLength_LengthToScreen(base*0.50f) > minScaleLength_px) scaleLength_u = base*0.50f;
						if (viewState.convertLength_LengthToScreen(base*0.40f) > minScaleLength_px) scaleLength_u = base*0.40f;
						if (viewState.convertLength_LengthToScreen(base*0.30f) > minScaleLength_px) scaleLength_u = base*0.30f;
						if (viewState.convertLength_LengthToScreen(base*0.20f) > minScaleLength_px) scaleLength_u = base*0.20f;
						if (viewState.convertLength_LengthToScreen(base*0.15f) > minScaleLength_px) scaleLength_u = base*0.15f;
						if (viewState.convertLength_LengthToScreen(base*0.10f) > minScaleLength_px) scaleLength_u = base*0.10f;
					}
				scaleLength_px = viewState.convertLength_LengthToScreen(scaleLength_u);
			}
			
			private String getScaleLengthStr() {
				float f = scaleLength_u;
				if (f<0.002) return String.format(Locale.ENGLISH, "%1.5fu", f);
				if (f<0.02 ) return String.format(Locale.ENGLISH, "%1.4fu", f);
				if (f<0.2  ) return String.format(Locale.ENGLISH, "%1.3fu", f);
				if (f<2   ) return String.format(Locale.ENGLISH, "%1.2fu", f);
				if (f<1000) return String.format(Locale.ENGLISH, "%1.0fu", f);
				f /= 1000;
				if (f<2   ) return String.format(Locale.ENGLISH, "%1.1fku", f);
				if (f<1000) return String.format(Locale.ENGLISH, "%1.0fku", f);
				f /= 1000;
				if (f<2   ) return String.format(Locale.ENGLISH, "%1.1fMu", f);
				else        return String.format(Locale.ENGLISH, "%1.0fMu", f);
			}
			
			private void drawScale(Graphics2D g2, int x, int y, int w, int h) {
				//g2.setColor(Color.RED);
				//g2.drawRect(x, y, w, h);
				
				g2.setColor(COLOR_AXIS);
				
				g2.drawLine(x+w, y  , x+w, y+h);
				g2.drawLine(x+w, y+h, x+w-scaleLength_px, y+h);
				g2.drawLine(x+w-scaleLength_px, y+h, x+w-scaleLength_px, y);
				
				String str = getScaleLengthStr();
				Rectangle2D bounds = getBounds(g2, str);
				
				g2.drawString( str, (float)(x+w-bounds.getX()-bounds.getWidth()-3), (float)(y+h-bounds.getY()-bounds.getHeight()-3) );
			}
		}

		private class Axes {
			
			public Float majorTickUnit_a = null;
			public Float minorTickUnit_a = null;
			public Integer minorTickCount = null;
			public int precision = 1;
			
			private boolean isVertical;
			
			Axes(boolean isVertical) {
				this.isVertical = isVertical;
			}
			
			public String toString(float angle) {
				return String.format(Locale.ENGLISH, "%1."+precision+"f", angle);
			}
			
			public void updateTicks(float minMinorTickUnitLength_a) {
				majorTickUnit_a = 1f;
				minorTickCount = 5; // minorTickUnit_a = 0.2
				precision = 0;
				while (majorTickUnit_a/10 > minMinorTickUnitLength_a) {
					
					if (majorTickUnit_a/10 > minMinorTickUnitLength_a) {
						majorTickUnit_a /= 2; // majorTickUnit_a = 0.5
						minorTickCount = 5;   // minorTickUnit_a = 0.1
						precision += 1;
					}
					
					if (majorTickUnit_a/10 > minMinorTickUnitLength_a) {
						majorTickUnit_a /= 2.5f; // majorTickUnit_a = 0.2
						minorTickCount = 4;      // minorTickUnit_a = 0.05
					}
					
					if (majorTickUnit_a/10 > minMinorTickUnitLength_a) {
						majorTickUnit_a /= 2; // majorTickUnit_a = 0.1
						minorTickCount = 5;   // minorTickUnit_a = 0.02
					}
				};
				while (majorTickUnit_a/minorTickCount < minMinorTickUnitLength_a) {
					
					if (majorTickUnit_a/minorTickCount < minMinorTickUnitLength_a) {
						majorTickUnit_a *= 2; // majorTickUnit_a = 2
						minorTickCount = 4;   // minorTickUnit_a = 0.5
					}
					
					if (majorTickUnit_a/minorTickCount < minMinorTickUnitLength_a) {
						majorTickUnit_a *= 2.5f; // majorTickUnit_a = 5
						minorTickCount = 5;      // minorTickUnit_a = 1
					}
					
					if (majorTickUnit_a/minorTickCount < minMinorTickUnitLength_a) {
						majorTickUnit_a *= 2; // majorTickUnit_a = 10
						minorTickCount = 5;   // minorTickUnit_a = 2
					}
				};
				minorTickUnit_a = majorTickUnit_a/minorTickCount;
			}
		
			private void drawAxis(Graphics2D g2, int c0, int c1, int width1, boolean labelsRightBottom) {
				//   isVertical:  c0 = x, c1 = y, width1 = height
				// ! isVertical:  c0 = y, c1 = x, width1 = width
				if (width1<0) return; // display area too small
				
				float minAngle_a,maxAngle_a,angleWidth_a;
				if (isVertical) minAngle_a = viewState.convertPos_ScreenToAngle_Lat (c1);
				else            minAngle_a = viewState.convertPos_ScreenToAngle_Long(c1);
				if (isVertical) maxAngle_a = viewState.convertPos_ScreenToAngle_Lat (c1+width1);
				else            maxAngle_a = viewState.convertPos_ScreenToAngle_Long(c1+width1);
				
				if (maxAngle_a<minAngle_a) {
					angleWidth_a = minAngle_a; // angleWidth_a  used as temp. storage
					minAngle_a = maxAngle_a;
					maxAngle_a = angleWidth_a;
				}
				angleWidth_a = maxAngle_a-minAngle_a;
				
				
				float firstMajorTick_a = (float) Math.ceil(minAngle_a / majorTickUnit_a) * majorTickUnit_a;
				
				g2.setPaint(COLOR_AXIS);
				if (isVertical) g2.drawLine(c0, c1, c0, c1+width1);
				else            g2.drawLine(c1, c0, c1+width1, c0);
				
				for (int j=1; minAngle_a < firstMajorTick_a-j*minorTickUnit_a; j++)
					drawMinorTick( g2, c0, firstMajorTick_a - j*minorTickUnit_a, labelsRightBottom );
				
				for (int i=0; firstMajorTick_a+i*majorTickUnit_a < maxAngle_a; i++) {
					float majorTick_a = firstMajorTick_a + i*majorTickUnit_a;
					drawMajorTick( g2, c0, majorTick_a, labelsRightBottom );
					for (int j=1; j<minorTickCount && majorTick_a + j*minorTickUnit_a < maxAngle_a; j++)
						drawMinorTick( g2, c0, majorTick_a + j*minorTickUnit_a, labelsRightBottom );
				}
			}
		
			private void drawMajorTick(Graphics2D g2, int c0, float angle, boolean labelsRightBottom) {
				//   isVertical:  c0 = x, c1 = y, width1 = height
				// ! isVertical:  c0 = y, c1 = x, width1 = width
				int c1;
				if (isVertical) c1 = viewState.convertPos_AngleToScreen_Lat (angle);
				else            c1 = viewState.convertPos_AngleToScreen_Long(angle);
				
				int halfTick = majorTickLength_px/2;
				int tickLeft  = halfTick;
				int tickRight = halfTick;
				if (labelsRightBottom) tickLeft = 0;
				else                   tickRight = 0;
				if (isVertical) g2.drawLine(c0-tickLeft, c1, c0+tickRight, c1);
				else            g2.drawLine(c1, c0-tickLeft, c1, c0+tickRight);
				
				String label = toString(angle);
				Rectangle2D bounds = g2.getFontMetrics().getStringBounds(label, g2);
				
				if (isVertical) {
					if (labelsRightBottom) g2.drawString(label, (float)(c0-bounds.getX()+halfTick+4                  ), (float)(c1-bounds.getY()-bounds.getHeight()/2));
					else                   g2.drawString(label, (float)(c0-bounds.getX()-halfTick-4-bounds.getWidth()), (float)(c1-bounds.getY()-bounds.getHeight()/2));
				} else {
					if (labelsRightBottom) g2.drawString(label, (float)(c1-bounds.getX()-bounds.getWidth()/2), (float)(c0-bounds.getY()+halfTick+4                   ));
					else                   g2.drawString(label, (float)(c1-bounds.getX()-bounds.getWidth()/2), (float)(c0-bounds.getY()-halfTick-4-bounds.getHeight()));
				}
			}
		
			private void drawMinorTick(Graphics2D g2, int c0, float angle, boolean labelsRightBottom) {
				//   isVertical:  c0 = x, c1 = y, width1 = height
				// ! isVertical:  c0 = y, c1 = x, width1 = width
				int c1;
				if (isVertical) c1 = viewState.convertPos_AngleToScreen_Lat (angle);
				else            c1 = viewState.convertPos_AngleToScreen_Long(angle);
				
				int tickLeft  = minorTickLength_px/2;
				int tickRight = minorTickLength_px/2;
				if (labelsRightBottom) tickLeft  = 0;
				else                   tickRight = 0;
				if (isVertical) g2.drawLine(c0-tickLeft, c1, c0+tickRight, c1);
				else            g2.drawLine(c1, c0-tickLeft, c1, c0+tickRight);
			}
		}

		private class ViewState {
		
			private LatLong center = null;
			private float scaleLengthPerAngleLat  = Float.NaN;
			private float scaleLengthPerAngleLong = Float.NaN;
			private float scalePixelPerLength     = Float.NaN;
			
			private Point tempPanOffset = null;
			
			public boolean isOk() {
				return center!=null && !Float.isNaN(scalePixelPerLength) && !Float.isNaN(scaleLengthPerAngleLat) && !Float.isNaN(scaleLengthPerAngleLong);
			}
		
			public boolean haveScalePixelPerLength() {
				return !Float.isNaN(scalePixelPerLength);
			}
		
			public boolean reset() {
				if (region==null || planet==null || planet.radius==null)
					return false;
				
				LatLong min = new LatLong();
				LatLong max = new LatLong();
				
				region.referencePoints.forEach(item->{ min.setMin(item.location); max.setMax(item.location); });
				region.circles        .forEach(item->{ min.setMin(item.center  ); max.setMax(item.center  ); });
				region.hotSpots       .forEach(item->{ min.setMin(item.location); max.setMax(item.location); });
				
				if (min.latitude==null || min.longitude==null || max.latitude==null || max.longitude==null ) {
					clearValues();
					return false;
				}
				
				center = new LatLong( (min.latitude+max.latitude)/2, (min.longitude+max.longitude)/2 );
				
				updateScaleLengthPerAngle();
				float neededheight = (max.latitude -min.latitude )*scaleLengthPerAngleLat;
				float neededWidth  = (max.longitude-min.longitude)*scaleLengthPerAngleLong;
				if (neededheight==0 || neededWidth==0) {
					clearValues();
					return false;
				}
				
				float scalePixelPerLengthLat  = (height-30) / neededheight;
				float scalePixelPerLengthLong = (width -30) / neededWidth;
				scalePixelPerLength = Math.min(scalePixelPerLengthLat, scalePixelPerLengthLong);
				
				return true;
			}

			private void clearValues() {
				center = null;
				scaleLengthPerAngleLat  = Float.NaN;
				scaleLengthPerAngleLong = Float.NaN;
				scalePixelPerLength     = Float.NaN;
			}
			
			public boolean zoom(Point point, float f) {
				if (!isOk()) return false;
				
				LatLong centerOld = new LatLong(center);
				LatLong location = convertScreenToAngle(point);
				
				if (scalePixelPerLength*f < minScaleLength_px/6000f) return false;
				
				scalePixelPerLength *= f;
				center.latitude  = (centerOld.latitude  - location.latitude ) / f + location.latitude;
				center.longitude = (centerOld.longitude - location.longitude) * (float) (Math.cos(centerOld.latitude/180*Math.PI) / Math.cos(center.latitude/180*Math.PI) ) / f + location.longitude;
				
				updateScaleLengthPerAngle();
				
				return true;
			}
		
			public boolean moveCenter(Point offsetOnScreen) {
				if (!isOk()) return false;
				
				float offsetLat  = convertLength_ScreenToAngle_Lat (-offsetOnScreen.y);
				float offsetLong = convertLength_ScreenToAngle_Long( offsetOnScreen.x);
				center = new LatLong( center.latitude-offsetLat, center.longitude-offsetLong );
				updateScaleLengthPerAngle();
				
				return true;
			}
		
			private void updateScaleLengthPerAngle() {
				scaleLengthPerAngleLat  = (float) (2*Math.PI*planet.radius / 360);
				scaleLengthPerAngleLong = (float) (2*Math.PI*planet.radius / 360 * Math.cos(center.latitude/180*Math.PI));
			}
		
			private Integer convertLength_LengthToScreen(Float length_u) {
				if (length_u==null || Float.isNaN(length_u)) return null;
				return Math.round( length_u * scalePixelPerLength );
			}
		
			private Point convertPos_AngleToScreen(LatLong location) {
				if (location.latitude==null || location.longitude==null) return null;
				return new Point(
					convertPos_AngleToScreen_Long(location.longitude),
					convertPos_AngleToScreen_Lat (location.latitude )
				);
			}
			private Integer convertPos_AngleToScreen_Long(float longitude) {
				float x = width /2f + convertLength_AngleToScreen_Long(longitude - center.longitude);
				if (tempPanOffset!=null) x += tempPanOffset.x;
				return Math.round(x);
			}
			private Integer convertPos_AngleToScreen_Lat (float latitude) {
				float y = height/2f - convertLength_AngleToScreen_Lat (latitude  - center.latitude );
				if (tempPanOffset!=null) y += tempPanOffset.y;
				return Math.round(y);
			}
			private float convertLength_AngleToScreen_Long(float length_a) { return length_a * scaleLengthPerAngleLong * scalePixelPerLength; }
			private float convertLength_AngleToScreen_Lat (float length_a) { return length_a * scaleLengthPerAngleLat  * scalePixelPerLength; }
			
			private LatLong convertScreenToAngle(Point point) {
				return new LatLong(
					convertPos_ScreenToAngle_Lat (point.y),
					convertPos_ScreenToAngle_Long(point.x)
				);
			}
			private Float convertPos_ScreenToAngle_Long(int x) {
				if (tempPanOffset!=null) x -= tempPanOffset.x;
				return center.longitude + convertLength_ScreenToAngle_Long(x - width /2f);
			}
			private Float convertPos_ScreenToAngle_Lat(int y) {
				if (tempPanOffset!=null) y -= tempPanOffset.y;
				return center.latitude  - convertLength_ScreenToAngle_Lat(y - height/2f);
			}
			private float convertLength_ScreenToAngle_Long(float length_px) { return length_px / scalePixelPerLength / scaleLengthPerAngleLong; }
			private float convertLength_ScreenToAngle_Lat (float length_px) { return length_px / scalePixelPerLength / scaleLengthPerAngleLat ; }
			
		}
	
	}

}
