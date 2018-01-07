package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
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
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import net.schwarzbaer.gui.StandardDialog;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID.Usage;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Gui.ListMenu;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Gui.ListMenu.ExternFunction;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Gui.NamedColorListMenu;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Images.ImageGridDialog;
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
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.NonStringRenderer;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.SimplifiedTable;

public class GameInfos {
	private static final String FILE_KNOWN_STAT_ID = "NMS_Viewer.KnownStatID.txt";
	private static final String FILE_PRODUCT_ID    = "NMS_Viewer.ProdIDs.txt";
	private static final String FILE_TECH_ID       = "NMS_Viewer.TechIDs.txt";
	private static final String FILE_SUBSTANCE_ID  = "NMS_Viewer.SubstanceIDs.txt";
	private static final String FILE_UNIVERSE_OBJECT_DATA = "NMS_Viewer.UniverseObjects.txt";
	
	private static HashMap<Long,UniverseObjectData> universeObjectDataArr;
	
	private static class UniverseObjectData implements Comparable<UniverseObjectData>{
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
		public int compareTo(UniverseObjectData other) {
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
			UniverseObjectData uoData = null;
			
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
			UniverseObjectData uoData = universeObjectDataArr.get(address);
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
		Vector<UniverseObjectData> values = new Vector<>(universeObjectDataArr.values());
		Collections.sort(values);
		
		System.out.print("Write data pool to file \""+FILE_UNIVERSE_OBJECT_DATA+"\" ...");
		File file = new File(FILE_UNIVERSE_OBJECT_DATA);
		boolean isFirst = true;
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8));) {
			for (UniverseObjectData uoData:values) {
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

	public static final IDMap productIDs   = new IDMap();
	public static final IDMap techIDs      = new IDMap();
	public static final IDMap substanceIDs = new IDMap();

	public static class IDMap {
		private HashMap<String, GeneralizedID> map;
		
		public IDMap() {
			map = new HashMap<>();
		}
	
		public Iterable<GeneralizedID> getValues() {
			return new Iterable<GeneralizedID>() {
				@Override public Iterator<GeneralizedID> iterator() { return map.values().iterator(); }
			}; 
		}
	
		public Vector<GeneralizedID> getSortedValues() {
			Vector<GeneralizedID> vector = new Vector<GeneralizedID>(map.values());
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
	
		public void set(String id, GeneralizedID generalizedID) {
			map.put(id, generalizedID);
		}
	
		public GeneralizedID get(String id) {
			GeneralizedID newID = new GeneralizedID(id);
			GeneralizedID existingID = map.putIfAbsent(id, newID);
			return existingID==null?newID:existingID;
		}
	}

	public static void loadProductIDsFromFile  () { loadIDsFromFile(FILE_PRODUCT_ID  ,productIDs  ,"product"   ); }

	public static void loadTechIDsFromFile     () { loadIDsFromFile(FILE_TECH_ID     ,techIDs     ,"technology"); }

	public static void loadSubstanceIDsFromFile() { loadIDsFromFile(FILE_SUBSTANCE_ID,substanceIDs,"substance" ); }

	private static void loadIDsFromFile(String filePath, IDMap map, String idLabel) {
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
				
				if (idStr.endsWith(".type")) {
					idStr = idStr.substring(0, idStr.length()-".type".length());
					GeneralizedID id = map.get(idStr);
					id.type = GeneralizedID.Type.getType(value);
				}
				else if (idStr.endsWith(".symbol")) {
					idStr = idStr.substring(0, idStr.length()-".symbol".length());
					GeneralizedID id = map.get(idStr);
					id.symbol = value;
				}
				else if (idStr.endsWith(".image")) {
					idStr = idStr.substring(0, idStr.length()-".image".length());
					GeneralizedID id = map.get(idStr);
					id.setImageFileName(value);
				}
				else if (idStr.endsWith(".imageBG")) {
					idStr = idStr.substring(0, idStr.length()-".imageBG".length());
					GeneralizedID id = map.get(idStr);
					try { id.setImageBG(Integer.parseInt(value,16)); }
					catch (NumberFormatException e) {}
				}
				else {
					GeneralizedID id = map.get(idStr);
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

	public static void saveIDsToFile(String filePath, IDMap map, String idLabel) {
		long start = System.currentTimeMillis();
		System.out.println("Write "+idLabel+" IDs to file \""+filePath+"\"...");
		
		File file = new File(filePath);
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8));) {
			for (String idStr:map.getSortedKeys()) {
				GeneralizedID id = map.get(idStr);
				out.printf("%s=%s\r\n",idStr,id.label);
				if (id.type!=null  ) out.printf("%s.type=%s\r\n"     ,idStr,id.type);
				if (id.hasSymbol ()) out.printf("%s.symbol=%s\r\n"   ,idStr,id.symbol);
				if (id.hasImage  ()) out.printf("%s.image=%s\r\n"    ,idStr,id.getImageFileName());
				if (id.hasImageBG()) out.printf("%s.imageBG=%06X\r\n",idStr,id.getImageBG());
			}
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		
		System.out.println("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}

	public static class GeneralizedID {
		
		enum Type {
			ShipWeapon               ("Schiffswaffe"),
			ShipWeaponUpgrade        ("Schiffswaffen-Upgrade"),
			ShipExtension            ("Schiffs-Erweiterung"),        
			ShipExtensionUpgrade     ("Schiffs-Erweiterungs-Upgrade"),
			MultitoolWeapon          ("Multitool-Waffe"),
			MultitoolWeaponUpgrade   ("Multitool-Waffen-Upgrade"),
			MultitoolExtension       ("Multitool-Erweiterung"),
			MultitoolExtensionUpgrade("Multitool-Erweiterungs-Upgrade"),
			ExosuitExtension         ("Exo-Anzug-Erweiterung"),
			ExosuitExtensionUpgrade  ("Exo-Anzug-Erweiterungs-Upgrade"),
			ExocraftWeapon           ("Exo-Fahrzeug-Waffe"),
			ExocraftWeaponUpgrade    ("Exo-Fahrzeug-Waffen-Upgrade"),
			ExocraftExtension        ("Exo-Fahrzeug-Erweiterung"),
			ExocraftExtensionUpgrade ("Exo-Fahrzeug-Erweiterungs-Upgrade"),
			BaseComponent            ("Basis-Komponente"),
			Plant                    ("Pflanze");
			
			private String label;
			Type(String label) { this.label = label; }
			public String getLabel() { return label; }
			public static Type getType(String str) {
				try { return valueOf(str); }
				catch (Exception e) { return null; }
			}
		}
		
		public final String id;
		public String label;
		public String symbol;
		public Type type;
		private String imageFileName;
		private Integer imageBackground;
		final HashMap<SaveGameData,Usage> usage;
		private BufferedImage cachedImage;
		
		private GeneralizedID(String id, String label) {
			this.id = id;
			this.label = label;
			this.symbol = "";
			this.type = null;
			this.usage = new HashMap<>();
			this.imageFileName = null;
			this.imageBackground = null;
			this.cachedImage = null;
		}
		public GeneralizedID(String id) {
			this(id,"");
		}
		public GeneralizedID(GeneralizedID other) {
			this.id = other.id;
			this.label = other.label;
			this.symbol = other.symbol;
			this.type = other.type;
			this.usage = new HashMap<SaveGameData,Usage>(other.usage);
			this.imageFileName = other.imageFileName;
			this.imageBackground = other.imageBackground;
			this.cachedImage = other.cachedImage;
		}
		
		public boolean hasLabel() {
			return label!=null && !label.isEmpty();
		}
		public boolean hasSymbol() {
			return symbol!=null && !symbol.isEmpty();
		}
		public String getName() {
			if (!hasLabel ()) return "["+id+"]";
			if (!hasSymbol()) return label+" ["+id+"]";
			return "("+symbol+") "+label+" ["+id+"]";
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
		private GeneralizedIDTableModel tableModel;
		private GeneralizedID clickedID;
		private Point clickedCell;
	
		private JTextArea textarea;
		private JLabel imageField;

		public GeneralizedIDPanel(Window mainwindow, IDMap idMap, String tableLabel) {
			super(new BorderLayout(3, 3));
			setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			
			this.mainwindow = mainwindow;
			
			clickedID = null;
			clickedCell = null;
			tableModel = new GeneralizedIDTableModel(this,idMap);
			table = new SimplifiedTable(tableLabel,tableModel,true,false,true);
			table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			table.getSelectionModel().addListSelectionListener(e->{
				if (table.getSelectedRowCount()==1)
					showID(tableModel.getValue(table.convertRowIndexToModel(table.getSelectedRow())));
				else
					showID(null);
			});
			prepareTable();
			
			GeneralizedID.Type[] types = addNull(GeneralizedID.Type.values());
			ExternFunction<GeneralizedID.Type> setType_Group = new ExternFunction<GeneralizedID.Type>() {
				@Override public void setResult(GeneralizedID.Type value) {
					int[] rows = table.getSelectedRows();
					for (int row:rows) {
						GeneralizedID id = tableModel.getValue(table.convertRowIndexToModel(row));
						if (id!=null) id.type = value; 
					}
					updateAfterContextMenuAction(true);
				}
				@Override public String toString(GeneralizedID.Type value) { if (value==null) return "<none>"; return value.getLabel(); }
			};
			ExternFunction<GeneralizedID.Type> setType_Single = new ExternFunction<GeneralizedID.Type>() {
				@Override public void setResult(GeneralizedID.Type value) {
					if (clickedID!=null) clickedID.type = value;
					updateAfterContextMenuAction(clickedID!=null);
				}
				@Override public String toString(GeneralizedID.Type value) { if (value==null) return "<none>"; return value.getLabel(); }
			};
			ListMenu<GeneralizedID.Type> typeListMenu_Std     = new ListMenu<GeneralizedID.Type>("Type", types, null, setType_Single);
			ListMenu<GeneralizedID.Type> typeListMenu_Image   = new ListMenu<GeneralizedID.Type>("Type", types, null, setType_Single);
			ListMenu<GeneralizedID.Type> typeListMenu_ImageBG = new ListMenu<GeneralizedID.Type>("Type", types, null, setType_Single);
			ListMenu<GeneralizedID.Type> typeListMenu_Group   = new ListMenu<GeneralizedID.Type>("Type of all", types, null, setType_Group);
			
			
			NamedColor[] colors = addNull(SaveViewer.images.colorValues);
			ExternFunction<NamedColor> setColor_Group = new ExternFunction<NamedColor>() {
				@Override public void setResult(NamedColor value) {
					int[] rows = table.getSelectedRows();
					for (int row:rows) {
						GeneralizedID id = tableModel.getValue(table.convertRowIndexToModel(row));
						if (id!=null) id.setImageBG(value==null?null:value.value); 
					}
					updateAfterContextMenuAction(true);
				}
				@Override public boolean isEqual(NamedColor v1, NamedColor v2) {
					if (v1==null && v2==null) return true;
					if (v1==null || v2==null) return false;
					return v1.value==v2.value;
				}
				@Override public String toString(NamedColor value) { if (value==null) return "<none>"; return value.name; }
			};
			ExternFunction<NamedColor> setColor_Single = new ExternFunction<NamedColor>() {
				@Override public void setResult(NamedColor value) {
					if (clickedID!=null) clickedID.setImageBG(value==null?null:value.value);
					updateAfterContextMenuAction(clickedID!=null);
				}
				@Override public boolean isEqual(NamedColor v1, NamedColor v2) {
					if (v1==null && v2==null) return true;
					if (v1==null || v2==null) return false;
					return v1.value==v2.value;
				}
				@Override public String toString(NamedColor value) { if (value==null) return "<none>"; return value.name; }
			};
			NamedColorListMenu colorListMenu_Std     = new NamedColorListMenu("Background", colors, null, setColor_Single);
			NamedColorListMenu colorListMenu_Image   = new NamedColorListMenu("Background", colors, null, setColor_Single);
			NamedColorListMenu colorListMenu_ImageBG = new NamedColorListMenu("Background", colors, null, setColor_Single);
			NamedColorListMenu colorListMenu_Group   = new NamedColorListMenu("Background of all", colors, null, setColor_Group);
			SaveViewer.images.addColorListListender(new Images.ColorListListender() {
				@Override public void colorAdded(NamedColor color) {
					NamedColor[] colors = addNull(SaveViewer.images.colorValues);
					colorListMenu_Std    .updateValues(colors);
					colorListMenu_Image  .updateValues(colors);
					colorListMenu_ImageBG.updateValues(colors);
					colorListMenu_Group  .updateValues(colors);
				}
			});
			
			DebugTableContextMenu contextMenuStd     = new DebugTableContextMenu(table);
			DebugTableContextMenu contextMenuImage   = new DebugTableContextMenu(table);
			DebugTableContextMenu contextMenuImageBG = new DebugTableContextMenu(table);
			DebugTableContextMenu contextMenuGroup   = new DebugTableContextMenu(table);
			
			contextMenuGroup.addSeparator();
			contextMenuGroup.add(typeListMenu_Group);
			contextMenuGroup.add(colorListMenu_Group);
			contextMenuGroup.add(createMenuItem("ImageFile of all ...",ActionCommand.SelectImage4AllSelected));
			
			contextMenuStd.addSeparator();
			contextMenuStd.add(createMenuItem("Edit ID",ActionCommand.EditID));
			contextMenuStd.add(typeListMenu_Std);
			contextMenuStd.add(colorListMenu_Std);
			contextMenuStd.add(createMenuItem("ImageFile ...",ActionCommand.SelectImage));
			
			contextMenuImage.addSeparator();
			contextMenuImage.add(createMenuItem("Edit ID",ActionCommand.EditID));
			contextMenuImage.add(typeListMenu_Image);
			contextMenuImage.add(colorListMenu_Image);
			contextMenuImage.add(createMenuItem("ImageFile ...",ActionCommand.SelectImage));
			contextMenuImage.addSeparator();
			contextMenuImage.add(createMenuItem("Clear ImageFile",ActionCommand.ClearImage));
			contextMenuImage.add(createMenuItem("Copy ImageFile",ActionCommand.CopyImage));
			contextMenuImage.add(createMenuItem("Paste ImageFile",ActionCommand.PasteImage));
			
			contextMenuImageBG.addSeparator();
			contextMenuImageBG.add(createMenuItem("Edit ID",ActionCommand.EditID));
			contextMenuImageBG.add(typeListMenu_ImageBG);
			contextMenuImageBG.add(colorListMenu_ImageBG);
			contextMenuImageBG.add(createMenuItem("ImageFile ...",ActionCommand.SelectImage));
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
						
						if (table.getSelectedRowCount()>1) {
							typeListMenu_Group.clearSelection();
							contextMenuGroup.show(table, e.getX(), e.getY());
						} else {
							DebugTableContextMenu contextMenu;
							ListMenu<GeneralizedID.Type> typeListMenu;
							NamedColorListMenu colorListMenu;
							switch (tableModel.getColumnID(colM)) {
							case Image:
								typeListMenu = typeListMenu_Image;
								colorListMenu = colorListMenu_Image;
								contextMenu = contextMenuImage;
								break;
							case ImgBG:
								typeListMenu = typeListMenu_ImageBG;
								colorListMenu = colorListMenu_ImageBG;
								contextMenu = contextMenuImageBG;
								break;
							default:
								typeListMenu = typeListMenu_Std;
								colorListMenu = colorListMenu_Std;
								contextMenu = contextMenuStd;
								break;
							}
							if (clickedID!=null) {
								typeListMenu.setValue(clickedID.type);
								colorListMenu.setValue(SaveViewer.images.getColor(clickedID.getImageBG()));
							}
							contextMenu.show(table, e.getX(), e.getY());
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
		
		private JMenuItem createMenuItem(String label, ActionCommand actionCommand) {
			JMenuItem menuItem = new JMenuItem(label);
			menuItem.addActionListener(this);
			menuItem.setActionCommand(actionCommand.toString());
			return menuItem;
		}
	
		enum ActionCommand { EditID, SelectImage, ClearImage, CopyImage, PasteImage, ClearBackground, CopyBackground, PasteBackground, AddBackgroundColor, SelectImage4AllSelected, SetBackground4All }
		
		@Override
		public void actionPerformed(ActionEvent e) {
			ActionCommand actionCommand = ActionCommand.valueOf(e.getActionCommand());
			//if (clickedID!=null) System.out.printf("%s(%s)\r\n",actionCommand,clickedID.id);
			if (clickedID==null) return;
			
			boolean idChanged = false;
			String cbValue;
			switch(actionCommand) {
			case EditID: {
				table.stopCellEditing();
				EditIdDialog dlg = new EditIdDialog(mainwindow,clickedID);
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
				
			case SetBackground4All: {
				NamedColor[] colors = addNull(SaveViewer.images.colorValues);
				Gui.ComboBoxDialog<NamedColor> dlg = new Gui.ComboBoxDialog<>(mainwindow,"Select color for all selected rows", "Select color", colors, null);
				dlg.showDialog();
				if (dlg.hasResult()) {
					NamedColor color = dlg.getResult();
					int[] rows = table.getSelectedRows();
					for (int row:rows) {
						GeneralizedID id = tableModel.getValue(table.convertRowIndexToModel(row));
						id.setImageBG(color==null?null:color.value); 
					}
					idChanged=true;
				}
			} break;
				
			case SelectImage4AllSelected: {
				ImageGridDialog dlg = new Images.ImageGridDialog(mainwindow, null);
				dlg.showDialog();
				if (dlg.hasChoosen()) {
					String image = dlg.getImageFileName();
					int[] rows = table.getSelectedRows();
					for (int row:rows) {
						GeneralizedID id = tableModel.getValue(table.convertRowIndexToModel(row));
						id.setImageFileName(image); 
					}
					idChanged=true;
				}
			} break;
			}
			updateAfterContextMenuAction(idChanged);
		}

		private void updateAfterContextMenuAction(boolean idChanged) {
			if (idChanged) {
				if (clickedCell!=null) tableModel.updateTableCell(clickedCell.x,clickedCell.y);
				tableModel.updateAfterCellChange(clickedID);
				if (clickedCell!=null) {
					int row = table.convertRowIndexToView(clickedCell.y);
					table.setRowSelectionInterval(row,row);
				}
				table.repaint();
			}
			clickedID = null;
		}
		
		private <T> T[] addNull(T[] arr) {
			Vector<T> vec = new Vector<>(Arrays.asList(arr));
			vec.insertElementAt(null,0);
			return vec.toArray(Arrays.copyOf(arr,0));
		}

		private void prepareTable() {
			
			ComboboxCellEditor<GeneralizedID.Type> typeCellEditor =
					new TableView.ComboboxCellEditor<GeneralizedID.Type>(addNull(GeneralizedID.Type.values()));
			NonStringRenderer<GeneralizedID.Type> typeRenderer =
					new TableView.NonStringRenderer<GeneralizedID.Type>(t->{if (t instanceof GeneralizedID.Type) return ((GeneralizedID.Type)t).getLabel(); return null; });
			typeCellEditor.setRenderer(typeRenderer);
			setCellEditor  (GeneralizedIDColumnID.Type, typeCellEditor);
			setCellRenderer(GeneralizedIDColumnID.Type, typeRenderer);
			
			ComboboxCellEditor<String> imageCellEditor =
					new TableView.ComboboxCellEditor<String>(addNull(SaveViewer.images.imagesNames));
			SaveViewer.images.addImageListListender(new Images.ImageListListender() {
				@Override public void imageListChanged() {
					imageCellEditor.setValues(addNull(SaveViewer.images.imagesNames));
				}
			});
			setCellEditor(GeneralizedIDColumnID.Image, imageCellEditor);
			
			ComboboxCellEditor<NamedColor> colorCellEditor =
					new TableView.ComboboxCellEditor<NamedColor>(addNull(SaveViewer.images.colorValues));
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
	
		private void setCellRenderer(GeneralizedIDColumnID columnID, TableCellRenderer cellRenderer) {
			TableColumn column = getColumn(columnID);
			if (column==null) return;
			column.setCellRenderer(cellRenderer);
		}
	
		private void setCellEditor(GeneralizedIDColumnID columnID, TableCellEditor cellEditor) {
			TableColumn column = getColumn(columnID);
			if (column==null) return;
			column.setCellEditor(cellEditor);
		}
	
		private TableColumn getColumn(GeneralizedIDColumnID columnID) {
			TableColumnModel columnModel = table.getColumnModel();
			if (columnModel==null) return null;
			
			int columnIndex = tableModel.getColumn(columnID);
			if (columnIndex<0) return null;
			
			return columnModel.getColumn(columnIndex);
		}
	
		private void showID(GeneralizedID id) {
			textarea.setText("");
			if (id==null) {
				imageField.setIcon(null);
				return;
			}
			
			textarea.append("ID     : "+id.id+"\r\n");
			if (id.type!=null  ) textarea.append("Type   : "+id.type.label+"\r\n");
			if (id.hasLabel  ()) textarea.append("Label  : "+id.label+"\r\n");
			if (id.hasSymbol ()) textarea.append("Symbol : "+id.symbol+"\r\n");
			if (id.hasImage  ()) textarea.append("Image  : "+id.getImageFileName()+"\r\n");
			if (id.hasImageBG()) textarea.append("ImageBG: "+String.format("%06X",id.getImageBG())+"\r\n");
			
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
			ID    ("ID"        ,            String.class,  80,-1,120,120),
			Type  ("Type"      ,GeneralizedID.Type.class, 100,-1,140,140),
			Symbol("Sym."      ,            String.class,  10,-1, 30, 30),
			Label ("Label"     ,            String.class, 150,-1,200,200),
			Image ("Image"     ,            String.class, 150,-1,250,250),
			ImgBG ("Background",        NamedColor.class, 150,-1,200,200),
			Usage (""          ,            String.class,  50,-1, 80, 80);
			
			private TableView.SimplifiedColumnConfig columnConfig;
			
			GeneralizedIDColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				columnConfig = new TableView.SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			@Override public TableView.SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
		}
	
		private static class GeneralizedIDTableModel extends TableView.SimplifiedTableModel<GeneralizedIDColumnID> {
	
			private static final GeneralizedIDColumnID[] COLUMNS = new GeneralizedIDColumnID[]{
					GeneralizedIDColumnID.ID,
					GeneralizedIDColumnID.Type,
					GeneralizedIDColumnID.Symbol,
					GeneralizedIDColumnID.Label,
					GeneralizedIDColumnID.Image,
					GeneralizedIDColumnID.ImgBG
				};

			private static final int EXTRA_ROWS = 1;
			
			private int numberOfLabledIDs;
			private IDMap sourceIdMap;
	
			private Vector<GeneralizedID> IDs;
			private Vector<SaveGameView> usageKeys;
	
			private GeneralizedIDPanel panel;
	
			protected GeneralizedIDTableModel(GeneralizedIDPanel panel, IDMap sourceIdMap) {
				super(COLUMNS);
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
				for (GeneralizedID id:IDs) {
					if (!id.label.isEmpty())
						++numberOfLabledIDs;
				}
			}
	
			@Override
			protected GeneralizedIDColumnID getColumnID(int columnIndex) {
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
			protected boolean isCellEditable(int rowIndex, int columnIndex, GeneralizedIDColumnID columnID) {
				if (rowIndex<EXTRA_ROWS) return false;
				switch(columnID) {
				case ID    : return false;
				case Type  :
				case Symbol:
				case Label :
				case Image :
				case ImgBG : return true;
				case Usage : return false;
				}
				return false;
			}
	
			public GeneralizedID getValue(int rowIndex) {
				if (rowIndex<EXTRA_ROWS) return null;
				return IDs.get(rowIndex-EXTRA_ROWS);
			}
	
			@Override
			public Object getValueAt(int rowIndex, int columnIndex, GeneralizedIDColumnID columnID) {
				if (rowIndex<EXTRA_ROWS) {
					switch(columnID) {
					case ID    : return String.format(Locale.ENGLISH,"total: %d", IDs.size());
					case Type  : return null;
					case Symbol: return "";
					case Label : return String.format(Locale.ENGLISH,"labeled: %d (%1.1f%%)", numberOfLabledIDs, IDs.isEmpty()?0:numberOfLabledIDs*100.0f/IDs.size());
					case Image : return "";
					case ImgBG : return "";
					case Usage : return "";
					}
					return null;
				}
				GeneralizedID id = IDs.get(rowIndex-EXTRA_ROWS);
				if (id==null) return null;
				switch(columnID) {
				case ID    : return id.id;
				case Type  : return id.type;
				case Symbol: return id.symbol;
				case Label : return id.label;
				case Image : return id.imageFileName;
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
			protected void setValueAt(Object aValue, int rowIndex, int columnIndex, GeneralizedIDColumnID columnID) {
				if (rowIndex<EXTRA_ROWS) return;
				
				GeneralizedID id = IDs.get(rowIndex-EXTRA_ROWS);
				if (id==null) return;
				
				switch(columnID) {
				case ID    : return;
				case Type  : id.type   = (aValue instanceof GeneralizedID.Type)?(GeneralizedID.Type)aValue:null; break;
				case Symbol: id.symbol = aValue==null?"":aValue.toString(); break;
				case Label : id.label  = aValue==null?"":aValue.toString(); break;
				case Image : id.setImageFileName(aValue); break;
				case ImgBG : id.setImageBG((aValue instanceof NamedColor)?((NamedColor)aValue).value:null); break;
				case Usage : return;
				}
				updateAfterCellChange(id);
			}
	
			public void updateAfterCellChange(GeneralizedID id) {
				panel.showID(id);
				
				if (sourceIdMap==techIDs     ) saveTechIDsToFile();
				if (sourceIdMap==productIDs  ) saveProductIDsToFile();
				if (sourceIdMap==substanceIDs) saveSubstanceIDsToFile();
			}
		}
	}

	public static class EditIdDialog extends StandardDialog {
		private static final long serialVersionUID = -4493777651637626630L;
		
		private JLabel imageField;
		private JTextArea textarea;
		
		private GeneralizedID id;
		private boolean hasDataChanged;
	
		private Images.ColorListListender colorListListender;
		private Images.ImageListListender imageListListender;
	
		public EditIdDialog(Window parent, GeneralizedID id) {
			super(parent, getDlgTitle(id), ModalityType.APPLICATION_MODAL, false);
			
			this.id = new GeneralizedID(id);
			this.hasDataChanged = false;
			
			textarea = new JTextArea();
			textarea.setEditable(false);
			JScrollPane textareaScrollPane = new JScrollPane(textarea);
			textareaScrollPane.getViewport().setPreferredSize(new Dimension(400, 100));
			
			JTextField labelTextField = new JTextField();
			labelTextField.setText(this.id.label);
			labelTextField.addActionListener(e->setLabel(labelTextField.getText()));
			labelTextField.addFocusListener(new FocusListener() {
				@Override public void focusGained(FocusEvent e) {}
				@Override public void focusLost(FocusEvent e) { setLabel(labelTextField.getText()); }
			});
			
			JTextField symbolTextField = new JTextField();
			symbolTextField.setPreferredSize(new Dimension(50,16));
			symbolTextField.setText(this.id.symbol);
			symbolTextField.addActionListener(e->setSymbol(symbolTextField.getText()));
			symbolTextField.addFocusListener(new FocusListener() {
				@Override public void focusGained(FocusEvent e) {}
				@Override public void focusLost(FocusEvent e) { setSymbol(symbolTextField.getText()); }
			});
			
			Vector<GeneralizedID.Type> types = new Vector<>();
			types.add(null); types.addAll(Arrays.asList(GeneralizedID.Type.values()));
			JComboBox<GeneralizedID.Type> cmbbxTypes = new JComboBox<GeneralizedID.Type>(types);
			cmbbxTypes.setSelectedItem(this.id.type);
			cmbbxTypes.addActionListener(e->setType((GeneralizedID.Type)cmbbxTypes.getSelectedItem()));
			cmbbxTypes.setRenderer(new TableView.NonStringRenderer<GeneralizedID.Type>(t->{if (t instanceof GeneralizedID.Type) return ((GeneralizedID.Type)t).getLabel(); return null; }));
			
			Vector<String> images = new Vector<>();
			images.add(""); images.addAll(Arrays.asList(SaveViewer.images.imagesNames));
			JComboBox<String> cmbbxImages = new JComboBox<String>(images);
			cmbbxImages.setSelectedItem(this.id.getImageFileName());
			cmbbxImages.addActionListener(e->setImageFileName((String)cmbbxImages.getSelectedItem()));
			
			imageListListender = new Images.ImageListListender() {
				@Override public void imageListChanged() {
					Vector<String> images = new Vector<>();
					images.add(""); images.addAll(Arrays.asList(SaveViewer.images.imagesNames));
					cmbbxImages.setModel(new DefaultComboBoxModel<>(images));
					cmbbxImages.setSelectedItem(EditIdDialog.this.id.getImageFileName());
				}
			};
			
			Vector<Images.NamedColor> colors = new Vector<>(Arrays.asList(SaveViewer.images.colorValues));
			colors.insertElementAt(null,0);
			JComboBox<Images.NamedColor> cmbbxColors = new JComboBox<Images.NamedColor>(new DefaultComboBoxModel<Images.NamedColor>(colors));
			cmbbxColors.setRenderer(new TableView.NamedColorRenderer());
			cmbbxColors.setSelectedItem(SaveViewer.images.getColor(this.id.getImageBG()));
			cmbbxColors.addActionListener(e->setImageBGColor((Images.NamedColor)cmbbxColors.getSelectedItem()));
			
			colorListListender = new Images.ColorListListender() {
				@Override public void colorAdded(Images.NamedColor color) {
					cmbbxColors.addItem(color);
					cmbbxColors.revalidate();
				}
			};
			
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
			buttonPanel.add(createButton("Apply" ,e->{closeDialog();}));
			buttonPanel.add(createButton("Cancel",e->{hasDataChanged = false; closeDialog();}));
			
			GridBagConstraints c = new GridBagConstraints();
			GridBagLayout cmbbxPanelLayout = new GridBagLayout();
			JPanel cmbbxPanel = new JPanel();
			cmbbxPanel.setLayout(cmbbxPanelLayout);
			c.insets = new Insets(1, 0, 1, 0);
			
			JButton selectImageButton = createButton("Select Image",e->showImageList(cmbbxImages));
			JButton    addColorButton = createButton("Add Color"   ,e->SaveViewer.images.showAddColorDialog(EditIdDialog.this,"Add Color"));
			
			addComp(cmbbxPanel,cmbbxPanelLayout,c,textareaScrollPane,1,1,GridBagConstraints.REMAINDER,1, GridBagConstraints.BOTH);
			
			addComp(cmbbxPanel,cmbbxPanelLayout,c,new JLabel("Sym./Label : ",JLabel.RIGHT),0,0,1,1, GridBagConstraints.HORIZONTAL);
			addComp(cmbbxPanel,cmbbxPanelLayout,c,symbolTextField,0,0,1,1, GridBagConstraints.BOTH);
			addComp(cmbbxPanel,cmbbxPanelLayout,c,labelTextField ,1,0,GridBagConstraints.REMAINDER,1, GridBagConstraints.BOTH);
			
			addComp(cmbbxPanel,cmbbxPanelLayout,c,new JLabel("Type : ",JLabel.RIGHT),0,0,1,1, GridBagConstraints.HORIZONTAL);
			addComp(cmbbxPanel,cmbbxPanelLayout,c,cmbbxTypes       ,1,0,GridBagConstraints.REMAINDER,1, GridBagConstraints.BOTH);
			
			addComp(cmbbxPanel,cmbbxPanelLayout,c,new JLabel("Image File : ",JLabel.RIGHT),0,0,1,1, GridBagConstraints.HORIZONTAL);
			addComp(cmbbxPanel,cmbbxPanelLayout,c,cmbbxImages      ,1,0,2,1, GridBagConstraints.BOTH);
			addComp(cmbbxPanel,cmbbxPanelLayout,c,selectImageButton,0,0,GridBagConstraints.REMAINDER,1, GridBagConstraints.BOTH);
			
			addComp(cmbbxPanel,cmbbxPanelLayout,c,new JLabel("Background : ",JLabel.RIGHT),0,0,1,1, GridBagConstraints.HORIZONTAL);
			addComp(cmbbxPanel,cmbbxPanelLayout,c,cmbbxColors   ,1,0,2,1, GridBagConstraints.BOTH);
			addComp(cmbbxPanel,cmbbxPanelLayout,c,addColorButton,0,0,GridBagConstraints.REMAINDER,1, GridBagConstraints.BOTH);
			
			imageField = new JLabel();
			imageField.setBorder(BorderFactory.createEtchedBorder());
			imageField.setPreferredSize(new Dimension(256,256));
			
			JPanel contentPane = new JPanel(new BorderLayout(3,3));
			contentPane.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
			contentPane.add(cmbbxPanel,BorderLayout.WEST);
			contentPane.add(imageField,BorderLayout.CENTER);
			contentPane.add(buttonPanel,BorderLayout.SOUTH);
			
			showValues();
			this.createGUI(contentPane);
		}
	
		private static String getDlgTitle(GeneralizedID id) {
			return String.format("Set values of ID \"%s\"",id.getName());
		}
		
		private void addComp(JPanel panel, GridBagLayout layout, GridBagConstraints c, Component comp, double weightx, double weighty, int gridwidth, int gridheight, int fill) {
			c.weightx=weightx;
			c.weighty=weighty;
			c.gridwidth=gridwidth;
			c.gridheight=gridheight;
			c.fill = fill;
			layout.setConstraints(comp, c);
			panel.add(comp);
		}
		
		@Override public void windowOpened(WindowEvent e) {
			SaveViewer.images.addColorListListender(colorListListender);
			SaveViewer.images.addImageListListender(imageListListender);
		}
		@Override public void windowClosed(WindowEvent e) {
			SaveViewer.images.removeColorListListender(colorListListender);
			SaveViewer.images.removeImageListListender(imageListListender);
		}
	
		private void showImageList(JComboBox<String> cmbbxImages) {
			Images.ImageGridDialog dlg = new Images.ImageGridDialog(this,id.getImageFileName());
			dlg.showDialog();
			if (dlg.hasChoosen()) {
				String result = dlg.getImageFileName();
				setImageFileName(result);
				cmbbxImages.setSelectedItem(result);
			}
		}
	
		private JButton createButton(String title, ActionListener l) {
			JButton button = new JButton(title);
			button.addActionListener(l);
			return button;
		}
	
		private void setLabel(String label) {
			id.label = label;
			setTitle(getDlgTitle(id));
			hasDataChanged = true;
			showValues();
		}

		private void setSymbol(String symbol) {
			id.symbol = symbol;
			setTitle(getDlgTitle(id));
			hasDataChanged = true;
			showValues();
		}

		private void setType(GeneralizedID.Type type) {
			id.type = type;
			hasDataChanged = true;
			showValues();
		}

		private void setImageFileName(String filename) {
			id.setImageFileName(filename);
			hasDataChanged = true;
			showValues();
		}

		private void setImageBGColor(Images.NamedColor namedColor) {
			id.setImageBG(namedColor==null?null:namedColor.value);
			hasDataChanged = true;
			showValues();
		}
		
		private void showValues() {
			textarea.setText("");
			
			textarea.append("ID     : "+id.id+"\r\n");
			textarea.append("Label  : "+id.label+"\r\n");
			textarea.append("Symbol : "+id.symbol+"\r\n");
			textarea.append("Type   : "+(id.type==null?"":id.type.label)+"\r\n");
			textarea.append("Image  : "+(id.hasImage  ()?id.getImageFileName():"<none>")+"\r\n");
			textarea.append("ImageBG: "+(id.hasImageBG()?String.format("%06X",id.getImageBG()):"<none>")+"\r\n");
			
			BufferedImage image = id.getImage();
			if (image==null) {
				imageField.setIcon(null);
			} else {
				imageField.setIcon(new ImageIcon(image));
				imageField.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));							
			}
		}
	
		public boolean hasDataChanged() { return hasDataChanged; }
		public Integer getImageBG() { return id.getImageBG(); }
		public String getImageFileName() { return id.getImageFileName(); }
		public String getLabel() { return id.label; }
	}
	
}