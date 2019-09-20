package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.CompoundBorder;
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

	private StandardMainWindow mainwindow = null;
	private TableView.SimplifiedTable referencePointsTable = null;
	private TableView.SimplifiedTable circlesTable = null;
	private TableView.SimplifiedTable hotSpotsTable = null;
	private Disabler<ActionCommand>   disabler = null;

	private ReferencePointsTableModel referencePointsTableModel = null;
	private CirclesTableModel         circlesTableModel = null;
	private HotSpotsTableModel        hotSpotsTableModel = null;
	private HotSpotView hotSpotView = null;
	
	private Vector<Planet> planets = new Vector<>();
	private Planet         currentPlanet = null;
	private Planet.Region  currentRegion = null;

	private JComboBox<Planet> planetComboBox = null;
	private JComboBox<Planet.Region> regionComboBox = null;

	private JTextField planetRadiusField;

	public static void main(String[] args) {
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		SaveViewer.loadToolbarIcons();
		GameInfos.loadUniverseObjectDataFromFile();
		start(null);
	}
	
	public static void start(SaveGameData.Universe.Planet planet) {
		new ResourceHotSpots()
//			.readConfig()
//			.writeConfig()
			.readData()
			.addNewData(planet)
			.createGUI(planet==null);
	}

	private ResourceHotSpots createGUI(boolean standalone) {
		
		disabler = new Disabler<ActionCommand>();
		disabler.setCareFor(ActionCommand.values());
		
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
		
		JSplitPane upperTablePanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		upperTablePanel.setResizeWeight(1f/2f);
		upperTablePanel.setTopComponent(referencePointsTableScrollPane);
		upperTablePanel.setBottomComponent(circlesTableScrollPane);
		
		JSplitPane tablePanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		tablePanel.setResizeWeight(2f/3f);
		tablePanel.setTopComponent(upperTablePanel);
		tablePanel.setBottomComponent(hotSpotsTableScrollPane);
		
		hotSpotView = new HotSpotView();
		hotSpotView.setPreferredSize(new Dimension(400,600));
		hotSpotView.setBorder(createCompoundBorder("HotSpots View"));
		
		JSplitPane centerPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		centerPanel.setResizeWeight(0);
		centerPanel.setLeftComponent(tablePanel);
		centerPanel.setRightComponent(hotSpotView);
		
		planetComboBox = new JComboBox<Planet>(planets);
		regionComboBox = new JComboBox<>();
		
		JLabel planetRadiusLabel = new JLabel("    Radius: ");
		disabler.add(ActionCommand.ChangePlanetRadius, planetRadiusLabel);
		planetRadiusField = SaveViewer.createTextField("", this, disabler, ActionCommand.ChangePlanetRadius);
		planetRadiusField.setColumns(10);
		planetRadiusField.setEnabled(false);
		
		planetComboBox.addActionListener(e->{
			currentPlanet = (Planet)planetComboBox.getSelectedItem();
			//SaveViewer.log_ln("ResourceHotSpots.currentPlanet: %s", currentPlanet);
			if (currentPlanet==null)
				regionComboBox.setModel(new DefaultComboBoxModel<Planet.Region>());
			else
				regionComboBox.setModel(new DefaultComboBoxModel<Planet.Region>(currentPlanet.regions));
			updatePlanetRadiusField();
			regionComboBox.setSelectedItem(null);
			updateGuiAccess();
		});
		regionComboBox.addActionListener(e->{
			currentRegion = (Planet.Region)regionComboBox.getSelectedItem();
			//SaveViewer.log_ln("ResourceHotSpots.currentRegion: %s", currentRegion);
			referencePointsTableModel.setRegion(currentRegion);
			circlesTableModel.setRegion(currentRegion);
			hotSpotsTableModel.setRegion(currentRegion);
			updateGuiAccess();
		});
		
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
		planetPanel.add(new JLabel(" "),c);
		
		JPanel contentPane = new JPanel(new BorderLayout(3,3));
		contentPane.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
		contentPane.add(selectPanel,BorderLayout.NORTH);
		contentPane.add(centerPanel,BorderLayout.CENTER);
		
		JMenu menuData = new JMenu("Data");
		menuData.add(SaveViewer.createMenuItem("Clear data"                , this, disabler, ActionCommand.ClearData     , ToolbarIcons.Delete));
		menuData.add(SaveViewer.createMenuItem("Write data to file"        , this, disabler, ActionCommand.SaveDataFile  , ToolbarIcons.Save));
		
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(menuData);
		
		mainwindow = new StandardMainWindow("Resource HotSpots",standalone?DefaultCloseOperation.EXIT_ON_CLOSE:DefaultCloseOperation.HIDE_ON_CLOSE);
		mainwindow.startGUI(contentPane,menuBar);
		
		//SaveViewer.log_ln("ResourceHotSpots.currentPlanet: %s <initial>", currentPlanet);
		//SaveViewer.log_ln("ResourceHotSpots.currentRegion: %s <initial>", currentRegion);
		planetComboBox.setSelectedItem(currentPlanet);
		regionComboBox.setSelectedItem(currentRegion);
		updateGuiAccess();
		return this;
	}

	private void updatePlanetRadiusField() {
		planetRadiusField.setText( currentPlanet==null || currentPlanet.radius==null ? "" : String.format(Locale.ENGLISH, "%1.2f", currentPlanet.radius) );
	}

	private void updateGuiAccess() {
		disabler.setEnable(ac->{
			switch (ac) {
			case ClearData:
			case SaveDataFile:
			case AddPlanet:
				return true;
				
			case AddRegion:
			case ChangePlanetName:
			case ChangePlanetRadius:
				return currentPlanet!=null;
				
			case ChangeRegionName:
				return currentPlanet!=null && currentRegion!=null;
			}
			return null;
		});
	}

	enum ActionCommand {
		ClearData,
		SaveDataFile,
		ChangePlanetName, ChangeRegionName,
		ChangePlanetRadius,
		AddRegion, AddPlanet,
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
			
		case AddPlanet:
			break;
		case AddRegion:
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

	@SuppressWarnings("unused")
	private ResourceHotSpots writeConfig() {
		return this;
	}

	@SuppressWarnings("unused")
	private ResourceHotSpots readConfig() {
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
		if (referencePointsTableModel!=null) referencePointsTableModel.setRegion(null);
		if (circlesTableModel        !=null) circlesTableModel        .setRegion(null);
		if (hotSpotsTableModel       !=null) hotSpotsTableModel       .setRegion(null);
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
		
		public void setLatitudeStr (String aValue) { latitude  = parse(aValue); }
		public void setLongitudeStr(String aValue) { longitude = parse(aValue); }

		public String getLatitudeStr () { return toString(latitude ); }
		public String getLongitudeStr() { return toString(longitude); }

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
		protected LocalTableModel(ColumnID[] columns) {
			super(columns);
		}
		@Override public ColumnID getColumnID(int columnIndex) {
			return super.getColumnID(columnIndex);
		}
		public abstract void configureColumn(TableColumn column, Integer i, ColumnID columnID);
		public abstract void setRegion(Planet.Region region);
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

		private Planet.Region region;
		@Override
		public void setRegion(Planet.Region region) {
			this.region = region;
			fireTableUpdate();
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

		private Planet.Region region;
		@Override
		public void setRegion(Planet.Region region) {
			this.region = region;
			fireTableUpdate();
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

		private Planet.Region region;
		@Override
		public void setRegion(Planet.Region region) {
			this.region = region;
			fireTableUpdate();
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
	
	private static class HotSpotView extends Canvas {
		private static final long serialVersionUID = 3631270386892323918L;

		@Override
		protected void paintCanvas(Graphics g, int x, int y, int width, int height) {
			if (!(g instanceof Graphics2D)) return;
			Graphics2D g2 = (Graphics2D) g;
			
			// TODO Auto-generated method stub
			g2.setColor(Color.WHITE);
			g2.fillRect(x, y, width, height);
		}
	
	}

}
