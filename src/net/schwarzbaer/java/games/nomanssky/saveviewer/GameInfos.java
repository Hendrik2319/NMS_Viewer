package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID.Usage;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Images.NamedColor;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Stats.StatValue.KnownID;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Galaxy;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Planet;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Region;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.SolarSystem;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.UniverseObject;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.UniverseObject.ExtraInfo;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.UniverseAddress;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.SaveGameView;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.ComboboxCellEditor;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.DebugTableContextMenu;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.SimplifiedTable;

public class GameInfos {
	private static final String FILE_KNOWN_STAT_ID = "NMS_Viewer.KnownStatID.txt";
	private static final String FILE_PRODUCT_ID    = "NMS_Viewer.ProdIDs.txt";
	private static final String FILE_TECH_ID       = "NMS_Viewer.TechIDs.txt";
	private static final String FILE_SUBSTANCE_ID  = "NMS_Viewer.SubstanceIDs.txt";
	private static final String FILE_UNIVERSE_OBJECT_DATA = "NMS_Viewer.UniverseObjects.txt";
	
	private static HashMap<Long,GameInfos.UniverseObjectData> universeObjectDataArr;
	
	private static class UniverseObjectData implements Comparable<GameInfos.UniverseObjectData>{
		enum Type { SolarSystem,Planet }
		
		final UniverseAddress universeAddress;
		final UniverseObjectData.Type type;
		String name;
		Universe.SolarSystem.Race race;
		Universe.SolarSystem.StarClass starClass; 
		Double distanceToCenter;
		Vector<ExtraInfo> extraInfos;
		
		public UniverseObjectData(UniverseAddress universeAddress, UniverseObjectData.Type type) {
			this.universeAddress = universeAddress;
			this.type = type;
			this.name = null;
			this.race = null;
			this.starClass = null;
			this.distanceToCenter = null;
			this.extraInfos = new Vector<>();
		}
	
		public boolean hasData() {
			return name!=null || race!=null || starClass!=null || distanceToCenter!=null || !extraInfos.isEmpty();
		}
	
		public UniverseObjectData(UniverseAddress universeAddress, SolarSystem sys) {
			this(universeAddress,Type.SolarSystem,sys);
			race = sys.race;
			starClass = sys.starClass;
			distanceToCenter = sys.distanceToCenter;
		}
		
		public UniverseObjectData(UniverseAddress universeAddress, Planet planet) {
			this(universeAddress,Type.Planet,planet);
		}
		
		public UniverseObjectData(UniverseAddress universeAddress, UniverseObjectData.Type type, UniverseObject uo) {
			this(universeAddress,type);
			name = uo.getOriginalName();
			for (ExtraInfo ei:uo.extraInfos)
				extraInfos.add(new ExtraInfo(ei));
		}
	
		@Override
		public int compareTo(GameInfos.UniverseObjectData other) {
			if (other==null) return -1;
			return universeAddress.compareTo(other.universeAddress);
		}
		
		
	}

	public static void loadUniverseObjectDataFromFile() {
		long start = System.currentTimeMillis();
		System.out.println("Read data of universe objects from file \""+FILE_UNIVERSE_OBJECT_DATA+"\"...");
		universeObjectDataArr = new HashMap<>();
		
		File file = new File(FILE_UNIVERSE_OBJECT_DATA);
		if (!file.isFile()) {
			System.out.println("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
			return;
		}
		
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8))) {
			String str;
			GameInfos.UniverseObjectData uoData = null;
			
			String nextShortLabel = null;
			boolean showInParent = false;
			while ((str=in.readLine())!=null) {
				if (str.isEmpty()) continue;
				if ((str.startsWith("[Sys") || str.startsWith("[Pln")) && str.endsWith("]")) {
					uoData = null;
					String addressStr = str.substring("[Sys".length(), str.length()-"]".length());
					long address;
					try { address = Long.parseLong(addressStr, 16); }
					catch (NumberFormatException e) {
						System.err.printf("Can't parse universe address in: \"%s\"\r\n",str);
						continue;
					}
					UniverseAddress ua = new UniverseAddress(address);
					if (str.startsWith("[Sys") && ua.isSolarSystem()) uoData = new UniverseObjectData(ua,UniverseObjectData.Type.SolarSystem);
					if (str.startsWith("[Pln") && ua.isPlanet     ()) uoData = new UniverseObjectData(ua,UniverseObjectData.Type.Planet);
					if (uoData != null) universeObjectDataArr.put(address, uoData);
					continue;
				}
				if (str.startsWith("name=")) {
					if (uoData!=null) uoData.name = str.substring("name=".length());
					continue;
				}
				if (str.startsWith("race=")) {
					if (uoData==null) continue;
					String race = str.substring("race=".length());
					try { uoData.race = Universe.SolarSystem.Race.valueOf(race); }
					catch (Exception e) { uoData.race = null; }
					continue;
				}
				if (str.startsWith("class=")) {
					if (uoData==null) continue;
					String starClass = str.substring("class=".length());
					try { uoData.starClass = Universe.SolarSystem.StarClass.valueOf(starClass); }
					catch (Exception e) { uoData.starClass = null; }
					continue;
				}
				if (str.startsWith("distance=")) {
					if (uoData==null) continue;
					String distance = str.substring("distance=".length());
					try { uoData.distanceToCenter = Double.parseDouble(distance); }
					catch (NumberFormatException e) { uoData.distanceToCenter = null; }
					continue;
				}
				if (str.startsWith("short=")) {
					nextShortLabel = str.substring("short=".length());
					showInParent = false;
					continue;
				}
				if (str.startsWith("short.P=")) {
					nextShortLabel = str.substring("short.P=".length());
					showInParent = true;
					continue;
				}
				if (str.startsWith("info=")) {
					String info = str.substring("info=".length());
					if (nextShortLabel!=null && uoData!=null)
						uoData.extraInfos.add(new ExtraInfo(showInParent,nextShortLabel,info));
					nextShortLabel=null;
					continue;
				}
			}
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
		System.out.println("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}

	public static void readUniverseObjectDataFromDataPool(Universe universe) {
		long start = System.currentTimeMillis();
		System.out.println("Read data of universe objects from data pool ... ");
		
		boolean withOutput = false; // DEBUG;
		
		SolarSystem system = null;
		Planet planet = null;
		UniverseObject uniObj = null;
		String uniObjName = null;
		for (Long address:universeObjectDataArr.keySet()) {
			GameInfos.UniverseObjectData uoData = universeObjectDataArr.get(address);
			UniverseAddress ua = new UniverseAddress(address);
			
			system = null;
			planet = null;
			
			if (uoData.type==UniverseObjectData.Type.SolarSystem && ua.isSolarSystem()) system = universe.findSolarSystem(ua);
			if (uoData.type==UniverseObjectData.Type.Planet      && ua.isPlanet     ()) planet = universe.findPlanet     (ua);
			if (system!=null) { uniObj = system; uniObjName = "solar system "+ua.getSigBoostCode();   if (withOutput) System.out.printf("Solar system %s\r\n",ua.getSigBoostCode());   }
			if (planet!=null) { uniObj = planet; uniObjName = "planet "+ua.getExtendedSigBoostCode(); if (withOutput) System.out.printf("Planet %s\r\n",ua.getExtendedSigBoostCode()); }
			
			if (uoData.name!=null && uniObj!=null) {
				uniObj.setOriginalName(uoData.name);
				if (withOutput) System.out.printf("   Name of %s was defined: \"%s\"\r\n",uniObjName,uoData.name);
			}
			
			if (uoData.race!=null && system!=null) {
				system.race = uoData.race;
				if (withOutput) System.out.printf("   Race of %s was defined: %s\r\n",uniObjName,system.race);
			}
			
			if (uoData.starClass!=null && system!=null) {
				system.starClass = uoData.starClass;
				if (withOutput) System.out.printf("   StarClass of %s was defined: %s\r\n",uniObjName,system.starClass);
			}
			
			if (uoData.distanceToCenter!=null && system!=null) {
				system.distanceToCenter = uoData.distanceToCenter;
				if (withOutput) System.out.printf("   Distance to galactic center of %s was defined: %s\r\n",uniObjName,system.distanceToCenter);
			}
			
			for (ExtraInfo ei:uoData.extraInfos) {
				if (uniObj!=null) {
					uniObj.extraInfos.add(ei);
					if (withOutput) System.out.printf("   Info of %s was defined: ( \"%s\", \"%s\" )\r\n",uniObjName,ei.shortLabel,ei.info);
				}
			}
			
		}
		if (!withOutput) System.out.println("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}

	public static void saveUniverseObjectDataToFile(Universe universe) {
		long start = System.currentTimeMillis();
		System.out.print("Write data of universe objects to data pool ...");
		for (Galaxy g:universe.galaxies)
			for (Region gr:g.regions)
				for (SolarSystem sys:gr.solarSystems) {
					UniverseAddress ua = sys.getUniverseAddress();
					universeObjectDataArr.put(ua.getAddress(),new UniverseObjectData(ua,sys));
					for (Planet p:sys.planets) {
						ua = p.getUniverseAddress();
						universeObjectDataArr.put(ua.getAddress(),new UniverseObjectData(ua,p));
					}
				}
		System.out.println("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
		
		start = System.currentTimeMillis();
		Vector<GameInfos.UniverseObjectData> values = new Vector<>(universeObjectDataArr.values());
		Collections.sort(values);
		
		System.out.print("Write data pool to file \""+FILE_UNIVERSE_OBJECT_DATA+"\" ...");
		File file = new File(FILE_UNIVERSE_OBJECT_DATA);
		boolean isFirst = true;
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8));) {
			for (GameInfos.UniverseObjectData uoData:values) {
				if (!uoData.hasData()) continue;
				
				if (!isFirst) out.println();
				isFirst = false;
				
				switch (uoData.type) {
				case SolarSystem: out.printf("[Sys%014X]\r\n",uoData.universeAddress.getAddress()); break;
				case Planet     : out.printf("[Pln%014X]\r\n",uoData.universeAddress.getAddress()); break;
				}
				
				if (uoData.name!=null) out.printf("name=%s\r\n",uoData.name);
				if (uoData.type==UniverseObjectData.Type.SolarSystem) {
					if (uoData.race!=null            ) out.printf("race=%s\r\n",uoData.race);
					if (uoData.starClass!=null       ) out.printf("class=%s\r\n",uoData.starClass);
					if (uoData.distanceToCenter!=null) out.printf(Locale.ENGLISH,"distance=%f\r\n",uoData.distanceToCenter.doubleValue());
				}
				
				for (ExtraInfo ei:uoData.extraInfos)
					if (!ei.shortLabel.isEmpty() || !ei.info.isEmpty()) {
						String showInParentStr="";
						if (uoData.type==UniverseObjectData.Type.Planet && ei.showInParent) showInParentStr = ".P";
						out.printf("short%s=%s\r\n", showInParentStr, ei.shortLabel);
						out.printf("info=%s\r\n" , ei.info);
					}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		System.out.println("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}

	public static void loadKnownStatIDsFromFile() {
		File file = new File(FILE_KNOWN_STAT_ID);
		if (!file.isFile()) return;
		
		long start = System.currentTimeMillis();
		System.out.println("Read known StatIDs from file \""+file.getPath()+"\" ...");
		String str;
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8))) {
			while ((str=in.readLine())!=null) {
				int pos = str.indexOf('=');
				if (pos<0) continue;
				
				KnownID knownID;
				try { knownID = KnownID.valueOf(str.substring(0, pos)); }
				catch (Exception e) { knownID = null; }
				if (knownID == null) continue;
				
				String fullName = str.substring(pos+1);
				if (!fullName.equals(knownID.fullName)) {
					System.out.printf("Changed StatValue.KnownID.fullName found: %16s [old]\"%s\" -> [new]\"%s\"\r\n",knownID,knownID.fullName,fullName);
					knownID.fullName = fullName;
				}
			}
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
		System.out.println("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}

	public static void saveKnownStatIDsToFile() {
		File file = new File(FILE_KNOWN_STAT_ID);
		long start = System.currentTimeMillis();
		System.out.println("Write known StatIDs to file \""+file.getPath()+"\" ...");
		
		KnownID[] knownIDs = KnownID.values();
		
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8));) {
			for (KnownID id:knownIDs)
				out.printf("%s=%s\r\n",id.toString(),id.fullName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		System.out.println("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}

	public static final GameInfos.IDMap productIDs   = new IDMap();
	public static final GameInfos.IDMap techIDs      = new IDMap();
	public static final GameInfos.IDMap substanceIDs = new IDMap();
	public static class IDMap {
		private HashMap<String, GameInfos.GeneralizedID> map;
		
		public IDMap() {
			map = new HashMap<>();
		}
	
		public Iterable<GameInfos.GeneralizedID> getValues() {
			return new Iterable<GameInfos.GeneralizedID>() {
				@Override public Iterator<GameInfos.GeneralizedID> iterator() { return map.values().iterator(); }
			}; 
		}
	
		public Vector<GameInfos.GeneralizedID> getSortedValues() {
			Vector<GameInfos.GeneralizedID> vector = new Vector<GameInfos.GeneralizedID>(map.values());
			vector.sort(Comparator.comparing(id->id.id));
			return vector;
		}
	
		public Iterable<String> getSortedKeys() {
			Vector<String> vector = new Vector<String>(map.keySet());
			vector.sort(null);
			return new Iterable<String>() {
				@Override public Iterator<String> iterator() { return vector.iterator(); }
			}; 
		}
	
		public void set(String id, GameInfos.GeneralizedID generalizedID) {
			map.put(id, generalizedID);
		}
	
		public GameInfos.GeneralizedID get(String id) {
			GameInfos.GeneralizedID newID = new GeneralizedID(id);
			GameInfos.GeneralizedID existingID = map.putIfAbsent(id, newID);
			return existingID==null?newID:existingID;
		}
	}

	public static void loadProductIDsFromFile  () { loadIDsFromFile(FILE_PRODUCT_ID  ,productIDs  ,"product"   ); }

	public static void loadTechIDsFromFile     () { loadIDsFromFile(FILE_TECH_ID     ,techIDs     ,"technology"); }

	public static void loadSubstanceIDsFromFile() { loadIDsFromFile(FILE_SUBSTANCE_ID,substanceIDs,"substance" ); }

	private static void loadIDsFromFile(String filePath, GameInfos.IDMap map, String idLabel) {
		File file = new File(filePath);
		if (!file.isFile()) return;
		
		long start = System.currentTimeMillis();
		System.out.println("Read "+idLabel+" IDs from file \""+filePath+"\"...");
		String str;
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8))) {
			while ((str=in.readLine())!=null) {
				int pos = str.indexOf('=');
				if (pos<0) continue;
				
				String idStr = str.substring(0, pos);
				String value = str.substring(pos+1);
				
				if (idStr.endsWith(".symbol")) {
					idStr = idStr.substring(0, idStr.length()-".symbol".length());
					GameInfos.GeneralizedID id = map.get(idStr);
					id.symbol = value;
					
				} else if (idStr.endsWith(".image")) {
					idStr = idStr.substring(0, idStr.length()-".image".length());
					GameInfos.GeneralizedID id = map.get(idStr);
					id.setImageFileName(value);
					
				} else if (idStr.endsWith(".imageBG")) {
					idStr = idStr.substring(0, idStr.length()-".imageBG".length());
					GameInfos.GeneralizedID id = map.get(idStr);
					try { id.setImageBG(Integer.parseInt(value,16)); }
					catch (NumberFormatException e) {}
					
				} else {
					GameInfos.GeneralizedID id = map.get(idStr);
					id.label = value;
				}
			}
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
		
		System.out.println("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}

	public static void saveProductIDsToFile  () { saveIDsToFile(FILE_PRODUCT_ID  ,productIDs  ,"product"    ); }

	public static void saveTechIDsToFile     () { saveIDsToFile(FILE_TECH_ID     ,techIDs     ,"technologie"); }

	public static void saveSubstanceIDsToFile() { saveIDsToFile(FILE_SUBSTANCE_ID,substanceIDs,"substance"  ); }

	public static void saveIDsToFile(String filePath, GameInfos.IDMap map, String idLabel) {
		long start = System.currentTimeMillis();
		System.out.println("Write "+idLabel+" IDs to file \""+filePath+"\"...");
		
		File file = new File(filePath);
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8));) {
			for (String idStr:map.getSortedKeys()) {
				GameInfos.GeneralizedID id = map.get(idStr);
				out.printf("%s=%s\r\n",idStr,id.label);
				if (id.hasSymbol ()) out.printf("%s.symbol=%s\r\n"   ,idStr,id.symbol);
				if (id.hasImage  ()) out.printf("%s.image=%s\r\n"    ,idStr,id.getImageFileName());
				if (id.hasImageBG()) out.printf("%s.imageBG=%06X\r\n",idStr,id.getImageBG());
			}
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		
		System.out.println("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}

	public static class GeneralizedID {
		
		public final String id;
		public String label;
		public String symbol;
		private String imageFileName;
		private Integer imageBackground;
		final HashMap<SaveGameData,Usage> usage;
		private BufferedImage cachedImage;
		
		private GeneralizedID(String id, String label) {
			this.id = id;
			this.label = label;
			this.symbol = "";
			this.usage = new HashMap<>();
			this.imageFileName = null;
			this.imageBackground = null;
			this.cachedImage = null;
		}
		public boolean hasSymbol() {
			return symbol!=null && !symbol.isEmpty();
		}
		public GeneralizedID(String id) {
			this(id,"");
		}
		public GeneralizedID(GameInfos.GeneralizedID other) {
			this.id = other.id;
			this.label = other.label;
			this.symbol = other.symbol;
			this.usage = new HashMap<SaveGameData,Usage>(other.usage);
			this.imageFileName = other.imageFileName;
			this.imageBackground = other.imageBackground;
			this.cachedImage = other.cachedImage;
		}
		
		public String getName() {
			if (label.isEmpty()) return "["+id+"]";
			return label+" ["+id+"]";
		}
		
		public boolean hasImage() { return imageFileName!=null; }
		public String getImageFileName() { return imageFileName==null?"":imageFileName; }
		public void setImageFileName(String fileName) {
			if (fileName!=null && fileName.isEmpty()) fileName = null;
			this.imageFileName = fileName;
			cachedImage = null;
		}
		public void setImageFileName(Object aValue) {
			setImageFileName(aValue==null?null:aValue.toString());
		}
		
		public boolean hasImageBG() { return imageBackground!=null; }
		public Integer getImageBG() { return imageBackground; }
		public void setImageBG(Integer color) {
			this.imageBackground = color;
			cachedImage = null;
		}
		
		public BufferedImage getCachedImage() {
			if (cachedImage == null) cachedImage = getImage();
			return cachedImage;
		}
		
		public BufferedImage getImage() {
			return getImage(-1,-1);
		}
		
		public BufferedImage getCachedImage(int width, int height) {
			if (cachedImage==null || cachedImage.getWidth()!=width || cachedImage.getHeight()!=height) cachedImage = getImage(width, height);
			return cachedImage;
		}
		
		public BufferedImage getImage(int width, int height) {
			return SaveViewer.images.getImage(imageFileName,imageBackground,width,height);
		}
		
		public Usage getUsage(SaveGameData base) {
			Usage newUsage = new Usage();
			Usage oldUsage = usage.putIfAbsent(base, newUsage);
			return oldUsage==null?newUsage:oldUsage;
		}
	
		public class Usage {
			
			public final Vector<String> inventoryUsages;
			public final Vector<String> blueprintUsages;
			
			public Usage() {
				inventoryUsages = new Vector<>();
				blueprintUsages = new Vector<>();
			}
			
			public void addInventoryUsage(String label, int x, int y) {
				inventoryUsages.add(String.format("%s @ (%d,%d)", label, x, y));
			}
	
			public void addBlueprintUsage(String label, int i) {
				blueprintUsages.add(label+" Blueprint "+i);
			}
	
			public boolean isEmpty() {
				return inventoryUsages.isEmpty() && blueprintUsages.isEmpty();
			}
		}
	}

	static class GeneralizedIDPanel extends JPanel implements ActionListener {
		private static final long serialVersionUID = -4946966056212175920L;
	
		private Window mainwindow;
	
		private SimplifiedTable table;
		private GeneralizedIDPanel.GeneralizedIDTableModel tableModel;
		private GameInfos.GeneralizedID clickedID;
		private Point clickedCell;
	
		private JTextArea textarea;
		private JLabel imageField;
	
		public GeneralizedIDPanel(Window mainwindow, GameInfos.IDMap idMap, String tableLabel) {
			super(new BorderLayout(3, 3));
			setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			
			this.mainwindow = mainwindow;
			
			clickedID = null;
			clickedCell = null;
			tableModel = new GeneralizedIDTableModel(this,idMap);
			table = new SimplifiedTable(tableLabel,tableModel,true,false,true);
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			table.getSelectionModel().addListSelectionListener(e->showID(tableModel.getValue(table.getRowSorter().convertRowIndexToModel(table.getSelectedRow()))));
			prepareTable();
			
			DebugTableContextMenu contextMenuStd     = new DebugTableContextMenu(table);
			DebugTableContextMenu contextMenuImage   = new DebugTableContextMenu(table);
			DebugTableContextMenu contextMenuImageBG = new DebugTableContextMenu(table);
			
			contextMenuStd.addSeparator();
			contextMenuStd.add(createMenuItem("Edit ID",ActionCommand.EditID));
			
			contextMenuImage.addSeparator();
			contextMenuImage.add(createMenuItem("Edit ID",ActionCommand.EditID));
			contextMenuImage.addSeparator();
			contextMenuImage.add(createMenuItem("Select ImageFile",ActionCommand.SelectImage));
			contextMenuImage.add(createMenuItem("Clear ImageFile",ActionCommand.ClearImage));
			contextMenuImage.add(createMenuItem("Copy ImageFile",ActionCommand.CopyImage));
			contextMenuImage.add(createMenuItem("Paste ImageFile",ActionCommand.PasteImage));
			
			contextMenuImageBG.addSeparator();
			contextMenuImageBG.add(createMenuItem("Edit ID",ActionCommand.EditID));
			contextMenuImageBG.addSeparator();
			contextMenuImageBG.add(createMenuItem("Clear Background",ActionCommand.ClearBackground));
			contextMenuImageBG.add(createMenuItem("Copy Background",ActionCommand.CopyBackground));
			contextMenuImageBG.add(createMenuItem("Paste Background",ActionCommand.PasteBackground));
			contextMenuImageBG.addSeparator();
			contextMenuImageBG.add(createMenuItem("Add Background Color",ActionCommand.AddBackgroundColor));
			
			
			table.addMouseListener(new MouseAdapter() {
				@Override public void mouseClicked(MouseEvent e) {
					clickedID = null;
					if (e.getButton()==MouseEvent.BUTTON3) {
						int rowM = table.convertRowIndexToModel(table.rowAtPoint(e.getPoint()));
						int colM = table.convertColumnIndexToModel(table.columnAtPoint(e.getPoint()));
						clickedCell = new Point(colM,rowM);
						clickedID = tableModel.getValue(rowM);
						
						switch (tableModel.getColumnID(colM)) {
						case Image: contextMenuImage  .show(table, e.getX(), e.getY()); break;
						case ImgBG: contextMenuImageBG.show(table, e.getX(), e.getY()); break;
						default   : contextMenuStd    .show(table, e.getX(), e.getY()); break;
						}
					}
				}
			});
			
			
			add(new JScrollPane(table),BorderLayout.CENTER);
			
			textarea = new JTextArea();
			textarea.setEditable(false);
			JScrollPane textareaScrollPane = new JScrollPane(textarea);
			textareaScrollPane.getViewport().setPreferredSize(new Dimension(400, 150));
			
			imageField = new JLabel();
			imageField.setBorder(BorderFactory.createEtchedBorder());
			imageField.setPreferredSize(new Dimension(100,100));
			
			JPanel eastPanel = new JPanel(new BorderLayout(3, 3));
			eastPanel.add(imageField, BorderLayout.NORTH);
			eastPanel.add(textareaScrollPane, BorderLayout.CENTER);
			
			
			add(eastPanel, BorderLayout.EAST);
			
		}
		
		private JMenuItem createMenuItem(String label, GeneralizedIDPanel.ActionCommand actionCommand) {
			JMenuItem menuItem = new JMenuItem(label);
			menuItem.addActionListener(this);
			menuItem.setActionCommand(actionCommand.toString());
			return menuItem;
		}
	
		enum ActionCommand { EditID, SelectImage, ClearImage, CopyImage, PasteImage, ClearBackground, CopyBackground, PasteBackground, AddBackgroundColor }
		
		@Override
		public void actionPerformed(ActionEvent e) {
			GeneralizedIDPanel.ActionCommand actionCommand = ActionCommand.valueOf(e.getActionCommand());
			//if (clickedID!=null) System.out.printf("%s(%s)\r\n",actionCommand,clickedID.id);
			if (clickedID==null) return;
			
			boolean idChanged = false;
			String cbValue;
			switch(actionCommand) {
			case EditID: {
				Images.EditIdDialog dlg = new Images.EditIdDialog(mainwindow,clickedID);
				dlg.showDialog();
				if (dlg.hasDataChanged()) {
					clickedID.label = dlg.getLabel();
					clickedID.setImageBG(dlg.getImageBG());
					clickedID.setImageFileName(dlg.getImageFileName());
					idChanged = true;
				}
			} break;
			
			case SelectImage: {
				Images.ImageGridDialog dlg = new Images.ImageGridDialog(mainwindow,clickedID.getImageFileName());
				dlg.showDialog();
				if (dlg.hasChoosen()) {
					String result = dlg.getImageFileName();
					clickedID.setImageFileName(result);
					idChanged = true;
				}
			} break;
				
			case ClearImage     : clickedID.setImageFileName(""); break;
			case ClearBackground: clickedID.setImageBG(null); break;
				
			case CopyImage      : SaveViewer.copyToClipBoard(clickedID.getImageFileName()); break;
			case CopyBackground : if (clickedID.hasImageBG()) { SaveViewer.copyToClipBoard(String.format("%06X", clickedID.getImageBG())); } break;
			
			case PasteImage:
				cbValue = SaveViewer.pasteFromClipBoard();
				if (cbValue!=null) {
					clickedID.setImageFileName(cbValue);
					idChanged = true;
				}
				break;
			case PasteBackground:
				cbValue = SaveViewer.pasteFromClipBoard();
				if (cbValue!=null)
					try { clickedID.setImageBG(Integer.parseInt(cbValue, 16)); idChanged = true; }
					catch (NumberFormatException e1) {}
				break;
				
			case AddBackgroundColor:
				SaveViewer.images.showAddColorDialog(mainwindow,"Add Color");
				break;
			}
			if (idChanged) {
				if (clickedCell!=null) tableModel.updateTableCell(clickedCell.x,clickedCell.y);
				tableModel.updateAfterCellChange(clickedID);
				if (clickedCell!=null) {
					int row = table.convertRowIndexToView(clickedCell.y);
					table.setRowSelectionInterval(row,row);
				}
			}
			clickedID = null;
		}
	
		private void prepareTable() {
			Vector<String> images = new Vector<>(Arrays.asList(SaveViewer.images.imagesNames));
			images.insertElementAt("",0);
			setCellEditor(GeneralizedIDColumnID.Image, new TableView.ComboboxCellEditor<String>(images.toArray(new String[0])));
			
			Vector<NamedColor> colors = new Vector<>(Arrays.asList(SaveViewer.images.colorValues));
			colors.insertElementAt(null,0);
			ComboboxCellEditor<NamedColor> colorCellEditor = new TableView.ComboboxCellEditor<NamedColor>(colors.toArray(new NamedColor[0]));
			SaveViewer.images.addColorListListender(new Images.ColorListListender() {
				@Override public void colorAdded(NamedColor color) {
					colorCellEditor.addValue(color);
				}
			});
			
			TableView.NamedColorRenderer colorRenderer = new TableView.NamedColorRenderer();
			colorCellEditor.setRenderer(colorRenderer);
			setCellEditor  (GeneralizedIDColumnID.ImgBG, colorCellEditor);
			setCellRenderer(GeneralizedIDColumnID.ImgBG, colorRenderer);
		}
	
		private void setCellRenderer(GeneralizedIDPanel.GeneralizedIDColumnID columnID, TableCellRenderer cellRenderer) {
			TableColumn column = getColumn(columnID);
			if (column==null) return;
			column.setCellRenderer(cellRenderer);
		}
	
		private void setCellEditor(GeneralizedIDPanel.GeneralizedIDColumnID columnID, TableCellEditor cellEditor) {
			TableColumn column = getColumn(columnID);
			if (column==null) return;
			column.setCellEditor(cellEditor);
		}
	
		private TableColumn getColumn(GeneralizedIDPanel.GeneralizedIDColumnID columnID) {
			TableColumnModel columnModel = table.getColumnModel();
			if (columnModel==null) return null;
			
			int columnIndex = tableModel.getColumn(columnID);
			if (columnIndex<0) return null;
			
			return columnModel.getColumn(columnIndex);
		}
	
		private void showID(GameInfos.GeneralizedID id) {
			textarea.setText("");
			if (id==null) {
				imageField.setIcon(null);
				return;
			}
			
			textarea.append("ID     : "+id.id+"\r\n");
			if (!id.label.isEmpty()) textarea.append("Label  : "+id.label+"\r\n");
			if (id.hasImage  ()    ) textarea.append("Image  : "+id.getImageFileName()+"\r\n");
			if (id.hasImageBG()    ) textarea.append("ImageBG: "+String.format("%06X",id.getImageBG())+"\r\n");
			
			BufferedImage image = id.getImage();
			if (image==null) {
				imageField.setIcon(null);
			} else {
				imageField.setIcon(new ImageIcon(image));
				imageField.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));							
			}
			
			for (SaveGameData key:id.usage.keySet()) {
				textarea.append("\r\n");
				textarea.append(key.filename+":\r\n");
				Usage usages = id.usage.get(key);
				if (usages.isEmpty())
					textarea.append("   none\r\n");
				for (String str:usages.inventoryUsages) textarea.append("   "+str+"\r\n");
				for (String str:usages.blueprintUsages) textarea.append("   "+str+"\r\n");
			}
		}
	
		public void addUsage(SaveGameView view) {
			stopCellEditing();
			tableModel.addUsage(view);
			tableModel.setColumnWidths(table);
			prepareTable();
		}
	
		public void updatePanel() {
			stopCellEditing();
			tableModel.updateData();
		}
	
		public void removeUsage(SaveGameView view) {
			stopCellEditing();
			tableModel.removeUsage(view);
			tableModel.setColumnWidths(table);
			prepareTable();
		}
	
		private void stopCellEditing() {
			TableCellEditor cellEditor = table.getCellEditor();
			if (cellEditor!=null) cellEditor.stopCellEditing();
		}
	
		private enum GeneralizedIDColumnID implements TableView.SimplifiedColumnIDInterface {
			ID    ("ID"        ,     String.class,  80,-1,120,120),
			Symbol("##"        ,     String.class,  10,-1, 30, 30),
			Label ("Label"     ,     String.class, 150,-1,200,200),
			Image ("Image"     ,     String.class, 150,-1,250,250),
			ImgBG ("Background", NamedColor.class, 150,-1,200,200),
			Usage (""          ,     String.class,  50,-1, 80, 80);
			
			private TableView.SimplifiedColumnConfig columnConfig;
			
			GeneralizedIDColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				columnConfig = new TableView.SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			@Override public TableView.SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
		}
	
		private static class GeneralizedIDTableModel extends TableView.SimplifiedTableModel<GeneralizedIDPanel.GeneralizedIDColumnID> {
	
			private static final int EXTRA_ROWS = 1;
			
			private int numberOfLabledIDs;
			private GameInfos.IDMap sourceIdMap;
	
			private Vector<GameInfos.GeneralizedID> IDs;
			private Vector<SaveGameView> usageKeys;
	
			private GameInfos.GeneralizedIDPanel panel;
	
			protected GeneralizedIDTableModel(GameInfos.GeneralizedIDPanel panel, GameInfos.IDMap sourceIdMap) {
				super(new GeneralizedIDPanel.GeneralizedIDColumnID[]{ GeneralizedIDColumnID.ID, GeneralizedIDColumnID.Symbol, GeneralizedIDColumnID.Label, GeneralizedIDColumnID.Image, GeneralizedIDColumnID.ImgBG });
				this.panel = panel;
				
				this.usageKeys = new Vector<>();
				this.sourceIdMap = sourceIdMap;
				updateIdList();
				updateNumberOfLabledIDs();
			}
	
			public void updateTableCell(int col, int row) {
				fireTableCellUpdate(row, col);
			}
	
			public void addUsage(SaveGameView view) {
				usageKeys.add(view);
				updateIdList();
				updateNumberOfLabledIDs();
				fireTableStructureUpdate();
			}
	
			public void updateData() {
				updateIdList();
				updateNumberOfLabledIDs();
				fireTableUpdate();
			}
	
			public void removeUsage(SaveGameView view) {
				usageKeys.remove(view);
				fireTableStructureUpdate();
			}
	
			private void updateIdList() {
				IDs = sourceIdMap.getSortedValues();
			}
	
			private void updateNumberOfLabledIDs() {
				numberOfLabledIDs = 0;
				for (GameInfos.GeneralizedID id:IDs) {
					if (!id.label.isEmpty())
						++numberOfLabledIDs;
				}
			}
	
			@Override
			protected GeneralizedIDPanel.GeneralizedIDColumnID getColumnID(int columnIndex) {
				if (columnIndex>=columns.length) return GeneralizedIDColumnID.Usage;
				return super.getColumnID(columnIndex);
			}
	
			@Override public int getColumnCount() {
				return columns.length+usageKeys.size();
			}
	
			@Override
			public String getColumnName(int columnIndex) {
				if (columnIndex>=columns.length) return usageKeys.get(columnIndex-columns.length).file.getName();
				return super.getColumnName(columnIndex);
			}
	
			@Override public int getRowCount() { return IDs.size()+EXTRA_ROWS; }
			@Override public int getUnsortedRowsCount() { return EXTRA_ROWS; }
			
			@Override
			protected boolean isCellEditable(int rowIndex, int columnIndex, GeneralizedIDPanel.GeneralizedIDColumnID columnID) {
				if (rowIndex<EXTRA_ROWS) return false;
				switch(columnID) {
				case ID   : return false;
				case Symbol:
				case Label:
				case Image: return true;
				case ImgBG: return true;
				case Usage: return false;
				}
				return false;
			}
	
			public GameInfos.GeneralizedID getValue(int rowIndex) {
				if (rowIndex<EXTRA_ROWS) return null;
				return IDs.get(rowIndex-EXTRA_ROWS);
			}
	
			@Override
			public Object getValueAt(int rowIndex, int columnIndex, GeneralizedIDPanel.GeneralizedIDColumnID columnID) {
				if (rowIndex<EXTRA_ROWS) {
					switch(columnID) {
					case ID    : return String.format(Locale.ENGLISH,"total: %d", IDs.size());
					case Symbol: return "";
					case Label : return String.format(Locale.ENGLISH,"labeled: %d (%1.1f%%)", numberOfLabledIDs, IDs.isEmpty()?0:numberOfLabledIDs*100.0f/IDs.size());
					case Image : return "";
					case ImgBG : return "";
					case Usage : return "";
					}
					return null;
				}
				GameInfos.GeneralizedID id = IDs.get(rowIndex-EXTRA_ROWS);
				if (id==null) return null;
				switch(columnID) {
				case ID    : return id.id;
				case Symbol: return id.symbol;
				case Label : return id.label;
				case Image : return id.getImageFileName();
				case ImgBG : return SaveViewer.images.getColor( id.getImageBG() );
				case Usage :
					Usage usage = id.usage.get(usageKeys.get(columnIndex-columns.length).data);
					if (usage==null) return "";
					if (usage.isEmpty()) return "";
					String str = "";;
					if (!usage.inventoryUsages.isEmpty()) str += usage.inventoryUsages.size()+"xI";
					if (!usage.blueprintUsages.isEmpty()) { if (!str.isEmpty()) str+=" "; str += usage.blueprintUsages.size()+"xB"; }
					return str;
				}
				return null;
			}
	
			@Override
			protected void setValueAt(Object aValue, int rowIndex, int columnIndex, GeneralizedIDPanel.GeneralizedIDColumnID columnID) {
				if (rowIndex<EXTRA_ROWS) return;
				
				GameInfos.GeneralizedID id = IDs.get(rowIndex-EXTRA_ROWS);
				if (id==null) return;
				
				switch(columnID) {
				case ID    : return;
				case Symbol: id.symbol = aValue==null?"":aValue.toString(); break;
				case Label : id.label  = aValue==null?"":aValue.toString(); break;
				case Image : id.setImageFileName(aValue); break;
				case ImgBG : id.setImageBG((aValue instanceof NamedColor)?((NamedColor)aValue).value:null); break;
				case Usage : return;
				}
				updateAfterCellChange(id);
			}
	
			public void updateAfterCellChange(GameInfos.GeneralizedID id) {
				panel.showID(id);
				
				if (sourceIdMap==techIDs     ) saveTechIDsToFile();
				if (sourceIdMap==productIDs  ) saveProductIDsToFile();
				if (sourceIdMap==substanceIDs) saveSubstanceIDsToFile();
			}
		}
	}
	
}