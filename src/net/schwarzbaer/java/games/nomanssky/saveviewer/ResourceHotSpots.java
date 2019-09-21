package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.MouseInputAdapter;
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

	public static void main(String[] args) {
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		SaveViewer.loadToolbarIcons();
		GameInfos.loadUniverseObjectDataFromFile();
		start(null);
	}
	
	public static void start(SaveGameData.Universe.Planet planet) {
		new ResourceHotSpots()
			.readConfig()
			.readData()
			.addNewData(planet)
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
		
		disabler = new Disabler<ActionCommand>();
		disabler.setCareFor(ActionCommand.values());
		
		if (windowConfig == null)
			windowConfig = new WindowConfig();
		
		referencePointsTableModel = new ReferencePointsTableModel();
		referencePointsTable = new TableView.SimplifiedTable("ReferencePoints", referencePointsTableModel, true, true, true);
		configureEachColumn(referencePointsTable,referencePointsTableModel);
		JScrollPane referencePointsTableScrollPane = new JScrollPane(referencePointsTable);
		referencePointsTableScrollPane.setPreferredSize(new Dimension(400, 200));
		referencePointsTableScrollPane.setBorder(createCompoundBorder("Reference Points"));
		
		circlesTableModel = new CirclesTableModel();
		circlesTable = new TableView.SimplifiedTable("Circles", circlesTableModel, true, true, true);
		configureEachColumn(circlesTable,circlesTableModel);
		JScrollPane circlesTableScrollPane = new JScrollPane(circlesTable);
		circlesTableScrollPane.setPreferredSize(new Dimension(400, 200));
		circlesTableScrollPane.setBorder(createCompoundBorder("Circles"));
		
		hotSpotsTableModel = new HotSpotsTableModel();
		hotSpotsTable = new TableView.SimplifiedTable("HotSpots", hotSpotsTableModel, true, true, true);
		configureEachColumn(hotSpotsTable,hotSpotsTableModel);
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
		JPanel hotSpotsViewPanel = new JPanel(new BorderLayout());
		hotSpotsViewPanel.setBorder(createCompoundBorder("HotSpots View"));
		hotSpotsViewPanel.add(hotSpotsView,BorderLayout.CENTER);
		
		windowConfig.centerPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		windowConfig.centerPanel.setResizeWeight(0);
		windowConfig.centerPanel.setLeftComponent(windowConfig.tablePanel);
		windowConfig.centerPanel.setRightComponent(hotSpotsViewPanel);
		
		planetComboBox = new JComboBox<Planet>(planets);
		regionComboBox = new JComboBox<>();
		SaveViewer.setComp(planetComboBox,this,disabler,ActionCommand.SelectPlanet);
		SaveViewer.setComp(regionComboBox,this,disabler,ActionCommand.SelectRegion);
		planetComboBox.setMinimumSize(new Dimension(100,16));
		regionComboBox.setMinimumSize(new Dimension(100,16));
		
		JLabel planetRadiusLabel = new JLabel("    Radius: ");
		disabler.add(ActionCommand.ChangePlanetRadius, planetRadiusLabel);
		planetRadiusField = SaveViewer.createTextField("", this, disabler, ActionCommand.ChangePlanetRadius);
		planetRadiusField.setColumns(10);
		planetRadiusField.setEnabled(false);
		planetRadiusField.setMinimumSize(new Dimension(70,16));
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		
		c.weightx=0;
		JPanel planetPanel = new JPanel(new GridBagLayout());
		planetPanel.setBorder(BorderFactory.createTitledBorder("Planet"));
		planetPanel.add(SaveViewer.createButton("Add Planet" , this, disabler, ActionCommand.AddPlanet),c);
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
		menuData.add(SaveViewer.createMenuItem("Clear data"                , this, disabler, ActionCommand.ClearData     , ToolbarIcons.Delete));
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

	private void updateGuiAfterRegionChange() {
		if (referencePointsTableModel!=null) referencePointsTableModel.setRegion(currentRegion);
		if (circlesTableModel        !=null) circlesTableModel        .setRegion(currentRegion);
		if (hotSpotsTableModel       !=null) hotSpotsTableModel       .setRegion(currentRegion);
		if (hotSpotsView             !=null) hotSpotsView             .setRegion(currentPlanet,currentRegion);
	}

	private void updatePlanetRadiusField() {
		planetRadiusField.setText( currentPlanet==null || currentPlanet.radius==null ? "" : String.format(Locale.ENGLISH, "%1.2f", currentPlanet.radius) );
	}

	private void updateGuiAccess() {
		if (disabler==null) return;
		disabler.setEnable(ac->{
			switch (ac) {
			
			case AddPlanet:
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
				return currentPlanet!=null && currentRegion!=null;
				
			}
			return null;
		});
	}

	enum ActionCommand {
		ClearData,
		SaveDataFile,
		ChangePlanetRadius,
		AddPlanet, AddRegion,
		SelectPlanet, SelectRegion,
		ChangePlanetName, ChangeRegionName, SaveWindowConfig,
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

		case AddPlanet:
			break;
		case AddRegion:
			break;
			
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
			}
		} break;
		}
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

	private <ColumnID extends Enum<ColumnID> & SimplifiedColumnIDInterface> void configureEachColumn(JTable table, LocalTableModel<ColumnID> tableModel) {
		forEachColumn(table, (column,i)->{
			i = table.convertColumnIndexToModel(i);
			ColumnID id = tableModel.getColumnID(i);
			tableModel.configureColumn(column,i,id);
		});
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
	
	private ResourceHotSpots addNewData(SaveGameData.Universe.Planet planet) {
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
						if (planet.universeAddress!=null) {
							String name = GameInfos.getName(planet.universeAddress);
							if (name==null) name = planet.universeAddress.getCoordinates_Planet();
							planet.name = name;
						}
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

		private static class ReferencePoint {
			
			public LatLong location;
			public String name;
			
			ReferencePoint() {
				location = new LatLong();
				name = null;
			}
		}

		private static class Circle {
			
			LatLong center;
			Float radius;
			
			Circle() {
				center = new LatLong();
				radius = null;
			}
		}

		private static class HotSpot {
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
		}
	}
	
	private static abstract class LocalTableModel<ColumnID extends Enum<ColumnID> & SimplifiedColumnIDInterface> extends SimplifiedTableModel<ColumnID> {
		
		protected Planet.Region region;
		
		protected LocalTableModel(ColumnID[] columns) {
			super(columns);
		}

		public void setRegion(Planet.Region region) {
			//SaveViewer.log_ln("%s.setRegion( %s )", getClass().getSimpleName(), region);
			this.region = region;
			fireTableUpdate();
		}
		
		@Override public ColumnID getColumnID(int columnIndex) {
			return super.getColumnID(columnIndex);
		}
		public abstract void configureColumn(TableColumn column, Integer i, ColumnID columnID);
	}

	private enum ReferencePointsTableColumnID implements SimplifiedColumnIDInterface {
		Latitude ("Latitude" ,  String.class, 20,-1, 70,-1),
		Longitude("Longitude",  String.class, 20,-1, 70,-1),
		Name     ("Name"     ,  String.class, 20,-1,150,-1),
		;
		private SimplifiedColumnConfig columnConfig;
		ReferencePointsTableColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
			columnConfig = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
		}
		@Override public SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
	}
	
	private static class ReferencePointsTableModel extends LocalTableModel<ReferencePointsTableColumnID> {
		

		protected ReferencePointsTableModel() {
			super(ReferencePointsTableColumnID.values());
		}

		@Override public int getRowCount() { return region==null ? 0 : region.referencePoints.size()+1; }
		@Override protected boolean isCellEditable(int rowIndex, int columnIndex, ReferencePointsTableColumnID columnID) { return true; }
		@Override public void configureColumn(TableColumn column, Integer i, ReferencePointsTableColumnID columnID) {}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex, ReferencePointsTableColumnID columnID) {
			if (region==null || rowIndex>=region.referencePoints.size())
				return null;
			
			Planet.ReferencePoint referencePoint = region.referencePoints.get(rowIndex);
			switch (columnID) {
			case Latitude : return referencePoint.location.getLatitudeStr();
			case Longitude: return referencePoint.location.getLongitudeStr();
			case Name     : return referencePoint.name;
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
			}
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

	private static class CirclesTableModel extends LocalTableModel<CirclesTableColumnID> {
		
		protected CirclesTableModel() {
			super(CirclesTableColumnID.values());
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
			if (region==null || rowIndex>=region.circles.size())
				return null;
			
			Planet.Circle circle = region.circles.get(rowIndex);
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
	
	private static class HotSpotsTableModel extends LocalTableModel<HotSpotsTableColumnID> {

		protected HotSpotsTableModel() {
			super(HotSpotsTableColumnID.values());
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
			if (region==null || rowIndex>=region.hotSpots.size())
				return null;
			
			Planet.HotSpot hotSpot = region.hotSpots.get(rowIndex);
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
			case Substance: hotSpot.substance = (String)aValue; break;
			case Class    : hotSpot.hotSpotClass = (Planet.HotSpot.Class)aValue; if (hotSpot.rate==null && hotSpot.hotSpotClass!=null) { hotSpot.rate=hotSpot.hotSpotClass.rate; fireTableRowUpdate(rowIndex); } break;
			case Rate     : try { hotSpot.rate = parseInt(aValue); } catch (NumberFormatException e) { fireTableRowUpdate(rowIndex); }
			}
		}
	}
	
	private static class HotSpotsView extends Canvas {
		private static final long serialVersionUID = 3631270386892323918L;
		
		private static final Color COLOR_AXIS = new Color(0x70000000,true);
		//private static final int nMajorTicksPerAxis = 2;
		private static final int minMinorTickUnitLength_px = 7;
		private static final int majorTickLength_px = 10;
		private static final int minorTickLength_px = 4;
		private static final int minScaleLength_px = 50;
		
		private Planet planet = null;
		private Planet.Region region = null;

		private LatLong center = null;
		private float scaleLengthPerAngleLat  = Float.NaN;
		private float scaleLengthPerAngleLong = Float.NaN;
		private float scalePixelPerLength     = Float.NaN;

		private Point panStart = null;
		private Point tempPanOffset = null;

		private Axes verticalAxes   = new Axes(true);
		private Axes horizontalAxes = new Axes(false);
		private Scale mapScale = new Scale();
		
		HotSpotsView() {
			MouseInputAdapter mouse = new MouseInputAdapter() {
				@Override public void mousePressed   (MouseEvent e) { if (e.getButton()==MouseEvent.BUTTON1) startPan  (e.getPoint()); }
				@Override public void mouseDragged   (MouseEvent e) { proceedPan(e.getPoint());  }
				@Override public void mouseReleased  (MouseEvent e) { if (e.getButton()==MouseEvent.BUTTON1) stopPan   (e.getPoint());  }
				@Override public void mouseWheelMoved(MouseWheelEvent e) { zoom(e.getPoint(),e.getPreciseWheelRotation()); }
				
			};
			addMouseListener(mouse);
			addMouseMotionListener(mouse);
			addMouseWheelListener(mouse);
		}

		@Override
		public void setBorder(Border border) {
			throw new UnsupportedOperationException();
		}

		protected void startPan(Point point) {
			panStart = point;
			tempPanOffset = new Point();
			repaint();
		}

		protected void proceedPan(Point point) {
			if (panStart != null)
				tempPanOffset = sub(point,panStart);
			repaint();
		}

		protected void stopPan(Point point) {
			if (panStart!=null && center!=null && !Float.isNaN(scalePixelPerLength) && !Float.isNaN(scaleLengthPerAngleLat) && !Float.isNaN(scaleLengthPerAngleLong)) {
				Point offset = sub(point,panStart);
				float offsetLat  = -offset.y / scalePixelPerLength / scaleLengthPerAngleLat;
				float offsetLong =  offset.x / scalePixelPerLength / scaleLengthPerAngleLong;
				center = new LatLong( center.latitude-offsetLat, center.longitude-offsetLong );
				updateScaleLengthPerAngle();
				updateAxes();
			}
			panStart = null;
			tempPanOffset = null;
			repaint();
		}

		private Point sub(Point p1, Point p2) {
			return new Point(p1.x-p2.x,p1.y-p2.y);
		}

		protected void zoom(Point point, double preciseWheelRotation) {
			if (center==null) return;
			
			LatLong centerOld = new LatLong(center);
			LatLong location = convertScreenToAngle(point);
			
			float f = (float) Math.pow(1.1f, preciseWheelRotation);
			if (scalePixelPerLength*f < minScaleLength_px/6000f) return;
			
			scalePixelPerLength *= f;
			center.latitude  = (centerOld.latitude  - location.latitude ) / f + location.latitude;
			center.longitude = (centerOld.longitude - location.longitude) * (float) (Math.cos(centerOld.latitude/180*Math.PI) / Math.cos(center.latitude/180*Math.PI) ) / f + location.longitude;
			
			updateScaleLengthPerAngle();
			updateAxes();
			mapScale.update();
			repaint();
		}

		private float convertLength_ScreenToAngle_Lat (float length_px) { return length_px / scalePixelPerLength / scaleLengthPerAngleLat ; }
		private float convertLength_ScreenToAngle_Long(float length_px) { return length_px / scalePixelPerLength / scaleLengthPerAngleLong; }

		private Float convertPos_ScreenToAngle_Lat(int y) {
			if (center==null) return null;
			if (tempPanOffset!=null) y -= tempPanOffset.y;
			return center.latitude  - convertLength_ScreenToAngle_Lat(y - height/2f);
		}

		private Float convertPos_ScreenToAngle_Long(int x) {
			if (center==null) return null;
			if (tempPanOffset!=null) x -= tempPanOffset.x;
			return center.longitude + convertLength_ScreenToAngle_Long(x - width /2f);
		}
		private LatLong convertScreenToAngle(Point point) {
			if (center==null) return null;
			return new LatLong(
				convertPos_ScreenToAngle_Lat (point.y),
				convertPos_ScreenToAngle_Long(point.x)
			);
		}

		private float convertLength_AngleToScreen_Lat (float length_a) { return length_a * scaleLengthPerAngleLat  * scalePixelPerLength; }
		private float convertLength_AngleToScreen_Long(float length_a) { return length_a * scaleLengthPerAngleLong * scalePixelPerLength; }

		private Integer convertPos_AngleToScreen_Lat (Float latitude) {
			if (center==null) return null;
			float y = height/2f - convertLength_AngleToScreen_Lat (latitude  - center.latitude );
			if (tempPanOffset!=null) y += tempPanOffset.y;
			return Math.round(y);
		}
		private Integer convertPos_AngleToScreen_Long(Float longitude) {
			if (center==null) return null;
			float x = width /2f + convertLength_AngleToScreen_Long(longitude - center.longitude);
			if (tempPanOffset!=null) x += tempPanOffset.x;
			return Math.round(x);
		}
		private Point convertPos_AngleToScreen(LatLong location) {
			if (center==null) return null;
			return new Point(
				convertPos_AngleToScreen_Long(location.longitude),
				convertPos_AngleToScreen_Lat (location.latitude )
			);
		}

		private Integer convertLength_LengthToScreen(Float length_u) {
			if (length_u==null || Float.isNaN(length_u) || Float.isNaN(scalePixelPerLength)) return null;
			return Math.round( length_u * scalePixelPerLength );
		}

		private void updateScaleLengthPerAngle() {
			scaleLengthPerAngleLat  = (float) (2*Math.PI*planet.radius / 360);
			scaleLengthPerAngleLong = (float) (2*Math.PI*planet.radius / 360 * Math.cos(center.latitude/180*Math.PI));
		}

		private void updateAxes() {
			if (center==null) return;
			  verticalAxes.updateTicks( convertLength_ScreenToAngle_Lat (minMinorTickUnitLength_px) );
			horizontalAxes.updateTicks( convertLength_ScreenToAngle_Long(minMinorTickUnitLength_px) );
		}

		public void setRegion(Planet planet, Planet.Region region) {
			this.planet = planet;
			this.region = region;
			reset();
		}

		public void reset() {
			if (region!=null && planet!=null) {
				LatLong min = new LatLong();
				LatLong max = new LatLong();
				
				region.referencePoints.forEach(item->{ min.setMin(item.location); max.setMax(item.location); });
				region.circles        .forEach(item->{ min.setMin(item.center  ); max.setMax(item.center  ); });
				region.hotSpots       .forEach(item->{ min.setMin(item.location); max.setMax(item.location); });
				
				if (min.latitude==null || min.longitude==null || max.latitude==null || max.longitude==null ) {
					center = null;
					scaleLengthPerAngleLat  = Float.NaN;
					scaleLengthPerAngleLong = Float.NaN;
					scalePixelPerLength     = Float.NaN;
					return;
				}
				
				center = new LatLong( (min.latitude+max.latitude)/2, (min.longitude+max.longitude)/2 );
				
				updateScaleLengthPerAngle();
				float scalePixelPerLengthLat  = (height-30) / ((max.latitude -min.latitude )*scaleLengthPerAngleLat );
				float scalePixelPerLengthLong = (width -30) / ((max.longitude-min.longitude)*scaleLengthPerAngleLong);
				scalePixelPerLength = Math.min(scalePixelPerLengthLat, scalePixelPerLengthLong);
				updateAxes();
				mapScale.update();
			}
			repaint();
		}

		@Override
		protected void paintCanvas(Graphics g, int x, int y, int width, int height) {
			if (!(g instanceof Graphics2D)) return;
			Graphics2D g2 = (Graphics2D) g;
			
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setClip(x, y, width, height);
			
			g2.setColor(Color.WHITE);
			g2.fillRect(x, y, width, height);
			
			if (region==null) {
				g2.setColor(Color.RED);
				g2.drawRect(x+1, y+1, width-3, height-3);
			} else {
				
				region.circles        .forEach(item->draw(g2,item));
				region.referencePoints.forEach(item->draw(g2,item));
				region.hotSpots       .forEach(item->draw(g2,item));
				
				verticalAxes.drawAxis( g2, x+5      , y+20, height-40, true  );
				verticalAxes.drawAxis( g2, x+width-5, y+20, height-40, false );
				horizontalAxes.drawAxis( g2, y+5       , x+20, width-40, true  );
				horizontalAxes.drawAxis( g2, y+height-5, x+20, width-40, false );
				
				mapScale.drawScale( g2, x+width-110, y+height-50, 60,15 );
			}
			
			g2.setClip(null);
		}
		
		private class Scale {

			private float scaleLength_u = 1;
			private int scaleLength_px = minScaleLength_px;

			public void update() {
				if (Float.isNaN(scalePixelPerLength)) return;
				
				scaleLength_u = 1;
				
				if (( convertLength_LengthToScreen(scaleLength_u) < minScaleLength_px) )
					while ( convertLength_LengthToScreen(scaleLength_u) < minScaleLength_px) {
						float base = scaleLength_u;
						if (convertLength_LengthToScreen(scaleLength_u) < minScaleLength_px) scaleLength_u = 1.5f*base;
						if (convertLength_LengthToScreen(scaleLength_u) < minScaleLength_px) scaleLength_u =    2*base;
						if (convertLength_LengthToScreen(scaleLength_u) < minScaleLength_px) scaleLength_u =    3*base;
						if (convertLength_LengthToScreen(scaleLength_u) < minScaleLength_px) scaleLength_u =    4*base;
						if (convertLength_LengthToScreen(scaleLength_u) < minScaleLength_px) scaleLength_u =    5*base;
						if (convertLength_LengthToScreen(scaleLength_u) < minScaleLength_px) scaleLength_u =    6*base;
						if (convertLength_LengthToScreen(scaleLength_u) < minScaleLength_px) scaleLength_u =    8*base;
						if (convertLength_LengthToScreen(scaleLength_u) < minScaleLength_px) scaleLength_u =   10*base;
					}
				else
					while ( convertLength_LengthToScreen(scaleLength_u*0.80f) > minScaleLength_px) {
						float base = scaleLength_u;
						if (convertLength_LengthToScreen(base*0.80f) > minScaleLength_px) scaleLength_u = base*0.80f;
						if (convertLength_LengthToScreen(base*0.60f) > minScaleLength_px) scaleLength_u = base*0.60f;
						if (convertLength_LengthToScreen(base*0.50f) > minScaleLength_px) scaleLength_u = base*0.50f;
						if (convertLength_LengthToScreen(base*0.40f) > minScaleLength_px) scaleLength_u = base*0.40f;
						if (convertLength_LengthToScreen(base*0.30f) > minScaleLength_px) scaleLength_u = base*0.30f;
						if (convertLength_LengthToScreen(base*0.20f) > minScaleLength_px) scaleLength_u = base*0.20f;
						if (convertLength_LengthToScreen(base*0.15f) > minScaleLength_px) scaleLength_u = base*0.15f;
						if (convertLength_LengthToScreen(base*0.10f) > minScaleLength_px) scaleLength_u = base*0.10f;
					}
				scaleLength_px = convertLength_LengthToScreen(scaleLength_u);
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
				Rectangle2D bounds = g2.getFontMetrics().getStringBounds(str, g2);
				
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
				if (center==null) return;
				if (width1<0) return; // display area too small
				
				float minAngle_a,maxAngle_a,angleWidth_a;
				if (isVertical) minAngle_a = convertPos_ScreenToAngle_Lat (c1);
				else            minAngle_a = convertPos_ScreenToAngle_Long(c1);
				if (isVertical) maxAngle_a = convertPos_ScreenToAngle_Lat (c1+width1);
				else            maxAngle_a = convertPos_ScreenToAngle_Long(c1+width1);
				
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
				if (isVertical) c1 = convertPos_AngleToScreen_Lat (angle);
				else            c1 = convertPos_AngleToScreen_Long(angle);
				
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
				if (isVertical) c1 = convertPos_AngleToScreen_Lat (angle);
				else            c1 = convertPos_AngleToScreen_Long(angle);
				
				int tickLeft  = minorTickLength_px/2;
				int tickRight = minorTickLength_px/2;
				if (labelsRightBottom) tickLeft  = 0;
				else                   tickRight = 0;
				if (isVertical) g2.drawLine(c0-tickLeft, c1, c0+tickRight, c1);
				else            g2.drawLine(c1, c0-tickLeft, c1, c0+tickRight);
			}
		}

		private void draw(Graphics2D g2, Planet.ReferencePoint item) {
			Point p = convertPos_AngleToScreen(item.location);
			if (p!=null) {
				g2.setColor(new Color(0x5EB91E));
				g2.fillOval(p.x-3, p.y-3, 6, 6);
			}
		}

		private void draw(Graphics2D g2, Planet.Circle item) {
			Point p = convertPos_AngleToScreen(item.center);
			Integer radius = convertLength_LengthToScreen(item.radius);
			if (p!=null && radius!=null) {
				g2.setColor(Color.LIGHT_GRAY);
				g2.fillOval(p.x-radius, p.y-radius, radius*2, radius*2);
			}
		}

		private void draw(Graphics2D g2, Planet.HotSpot item) {
			Point p = convertPos_AngleToScreen(item.location);
			if (p!=null) {
				if (item.type==null) {
					g2.setColor(Color.BLACK);
				} else
					switch (item.type) {
					case Mineral: g2.setColor(new Color(0x5A84B1));break;
					case Gas    : g2.setColor(new Color(0xFFCC00));break;
					case Energy : g2.setColor(new Color(0xFC4200)); break;
					default:
						break;
					
					}
				g2.fillOval(p.x-3, p.y-3, 6, 6);
			}
			
			// TODO Auto-generated method stub
			
		}
	
	}

}
