package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.BorderLayout;
import java.awt.Color;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
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
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID.Type;
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
		enum Type { Region,SolarSystem,Planet }
		
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
		
		public UniverseObjectData(UniverseAddress universeAddress, Region region) {
			this(universeAddress,Type.Region);
			this.name = region.name;
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
		SaveViewer.log_ln("Read data of universe objects from file \""+FILE_UNIVERSE_OBJECT_DATA+"\"...");
		universeObjectDataArr = new HashMap<>();
		
		File file = new File(FILE_UNIVERSE_OBJECT_DATA);
		if (!file.isFile()) {
			SaveViewer.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
			return;
		}
		
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8))) {
			String str;
			UniverseObjectData uoData = null;
			
			String nextShortLabel = null;
			boolean showInParent = false;
			while ((str=in.readLine())!=null) {
				if (str.isEmpty()) continue;
				if ((str.startsWith("[Reg") || str.startsWith("[Sys") || str.startsWith("[Pln")) && str.endsWith("]")) {
					uoData = null;
					String addressStr = str.substring("[Sys".length(), str.length()-"]".length());
					long address;
					try { address = Long.parseLong(addressStr, 16); }
					catch (NumberFormatException e) {
						System.err.printf("Can't parse universe address in: \"%s\"\r\n",str);
						continue;
					}
					UniverseAddress ua = new UniverseAddress(address);
					if (str.startsWith("[Reg") && ua.isRegion     ()) uoData = new UniverseObjectData(ua,UniverseObjectData.Type.Region);
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
		SaveViewer.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}

	public static void readUniverseObjectDataFromDataPool(Universe universe) {
		long start = System.currentTimeMillis();
		SaveViewer.log_ln("Read data of universe objects from data pool ... ");
		
		boolean withOutput = false; // DEBUG;
		
		Region region = null;
		SolarSystem system = null;
		Planet planet = null;
		
		UniverseObject uniObj = null;
		String objName = null;
		for (Long address:universeObjectDataArr.keySet()) {
			UniverseObjectData uoData = universeObjectDataArr.get(address);
			UniverseAddress ua = new UniverseAddress(address);
			
			region = null;
			system = null;
			planet = null;
			uniObj = null;
			objName = null;
			
			switch(uoData.type) {
			case Region     : if (ua.isRegion     ()) region = universe.findRegion     (ua); break;
			case SolarSystem: if (ua.isSolarSystem()) system = universe.findSolarSystem(ua); break;
			case Planet     : if (ua.isPlanet     ()) planet = universe.findPlanet     (ua); break;
			}
			if (region!=null) { uniObj = null;   objName = "region "+ua.getRegionCoordinates();    if (withOutput) SaveViewer.log("Region %s\r\n"      ,ua.getRegionCoordinates   ()); }
			if (system!=null) { uniObj = system; objName = "solar system "+ua.getSigBoostCode();   if (withOutput) SaveViewer.log("Solar system %s\r\n",ua.getSigBoostCode        ()); }
			if (planet!=null) { uniObj = planet; objName = "planet "+ua.getExtendedSigBoostCode(); if (withOutput) SaveViewer.log("Planet %s\r\n"      ,ua.getExtendedSigBoostCode()); }
			
			if (uoData.name!=null) {
				if (uniObj!=null) uniObj.setOriginalName(uoData.name);
				if (region!=null) region.setName(uoData.name);
				if (withOutput && objName!=null) SaveViewer.log("   Name of %s was defined: \"%s\"\r\n",objName,uoData.name);
			}
			
			if (uoData.race!=null && system!=null) {
				system.race = uoData.race;
				if (withOutput) SaveViewer.log("   Race of %s was defined: %s\r\n",objName,system.race);
			}
			
			if (uoData.starClass!=null && system!=null) {
				system.starClass = uoData.starClass;
				if (withOutput) SaveViewer.log("   StarClass of %s was defined: %s\r\n",objName,system.starClass);
			}
			
			if (uoData.distanceToCenter!=null && system!=null) {
				system.distanceToCenter = uoData.distanceToCenter;
				if (withOutput) SaveViewer.log("   Distance to galactic center of %s was defined: %s\r\n",objName,system.distanceToCenter);
			}
			
			for (ExtraInfo ei:uoData.extraInfos) {
				if (uniObj!=null) {
					uniObj.extraInfos.add(ei);
					if (withOutput) SaveViewer.log("   Info of %s was defined: ( \"%s\", \"%s\" )\r\n",objName,ei.shortLabel,ei.info);
				}
			}
			
		}
		if (!withOutput) SaveViewer.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}

	public static void saveUniverseObjectDataToFile(Universe universe) {
		long start = System.currentTimeMillis();
		SaveViewer.log("Write data of universe objects to data pool ...");
		for (Galaxy g:universe.galaxies)
			for (Region r:g.regions) {
				UniverseAddress ua = r.getUniverseAddress();
				universeObjectDataArr.put(ua.getAddress(),new UniverseObjectData(ua,r));
				for (SolarSystem sys:r.solarSystems) {
					ua = sys.getUniverseAddress();
					universeObjectDataArr.put(ua.getAddress(),new UniverseObjectData(ua,sys));
					for (Planet p:sys.planets) {
						ua = p.getUniverseAddress();
						universeObjectDataArr.put(ua.getAddress(),new UniverseObjectData(ua,p));
					}
				}
			}
		SaveViewer.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
		
		start = System.currentTimeMillis();
		Vector<UniverseObjectData> values = new Vector<>(universeObjectDataArr.values());
		Collections.sort(values);
		
		SaveViewer.log("Write data pool to file \""+FILE_UNIVERSE_OBJECT_DATA+"\" ...");
		File file = new File(FILE_UNIVERSE_OBJECT_DATA);
		boolean isFirst = true;
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8));) {
			for (UniverseObjectData uoData:values) {
				if (!uoData.hasData()) continue;
				
				if (!isFirst) out.println();
				isFirst = false;
				
				switch (uoData.type) {
				case Region     : out.printf("[Reg%014X]\r\n",uoData.universeAddress.getAddress()); break;
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
		SaveViewer.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}

	public static void loadKnownStatIDsFromFile() {
		File file = new File(FILE_KNOWN_STAT_ID);
		if (!file.isFile()) return;
		
		long start = System.currentTimeMillis();
		SaveViewer.log_ln("Read known StatIDs from file \""+file.getPath()+"\" ...");
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
					SaveViewer.log("Changed StatValue.KnownID.fullName found: %16s [old]\"%s\" -> [new]\"%s\"\r\n",knownID,knownID.fullName,fullName);
					knownID.fullName = fullName;
				}
			}
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
		SaveViewer.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}

	public static void saveKnownStatIDsToFile() {
		File file = new File(FILE_KNOWN_STAT_ID);
		long start = System.currentTimeMillis();
		SaveViewer.log_ln("Write known StatIDs to file \""+file.getPath()+"\" ...");
		
		KnownID[] knownIDs = KnownID.values();
		
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8));) {
			for (KnownID id:knownIDs)
				out.printf("%s=%s\r\n",id.toString(),id.fullName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		SaveViewer.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
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
	
//		public void set(String id, GeneralizedID generalizedID) {
//			map.put(id, generalizedID);
//		}
		
		public GeneralizedID get(String id, SaveGameData source, Usage.Type usageType) {
			GeneralizedID generalizedID = get(id);
			generalizedID.setUsage(source,usageType);
			return generalizedID;
		}
	
		private GeneralizedID get(String id) {
			GeneralizedID newID = new GeneralizedID(id);
			GeneralizedID existingID = map.putIfAbsent(id, newID);
			return existingID==null ? newID : existingID;
		}

		public HashMap<String, Vector<GeneralizedID>> getIDBaseGroups() {
			HashMap<String, Vector<GeneralizedID>> groups = new HashMap<>();
			for (GeneralizedID id:map.values()) {
				Vector<GeneralizedID> group = groups.get(id.idBase);
				if (group==null) groups.put(id.idBase,group = new Vector<GeneralizedID>());
				group.add(id);
			}
			return groups;
		}

		public Vector<GeneralizedID> getIDBaseGroup(String idBase) {
			Vector<GeneralizedID> group = new Vector<>();
			for (GeneralizedID id:map.values())
				if (id.idBase.equals(idBase))
					group.add(id);
			return group;
		}
	}

	public static void updateUpgrades() {
		updateUpgrades(productIDs  );
		updateUpgrades(techIDs     );
		updateUpgrades(substanceIDs);
	}

	public static void updateUpgrades(IDMap map) {
		HashMap<String,Vector<GeneralizedID>> groups = map.getIDBaseGroups();
		for (String idBase:groups.keySet())
			updateUpgrades(groups.get(idBase));
	}

	public static void updateUpgrades(IDMap map, GeneralizedID id) {
		updateUpgrades(map.getIDBaseGroup(id.idBase));
	}

	public static void updateUpgrades(Vector<GeneralizedID> group) {
		group.sort(Comparator.comparing((GeneralizedID id)->id.idIndex,Comparator.nullsFirst(Comparator.naturalOrder())));
		for (int i=0; i<group.size(); ++i) {
			GeneralizedID id = group.get(i);
			Vector<GeneralizedID> vec = id.betterUpgrades;
			vec.clear();
			//if (id.type!=null && id.type.isUpgrade)
			vec.addAll( group.subList(i+1,group.size()) );
		}
	}

	public static void loadAllIDsFromFiles() {
		loadIDsFromFile(FILE_PRODUCT_ID  ,productIDs  ,"product"   ); updateUpgrades(productIDs  );
		loadIDsFromFile(FILE_TECH_ID     ,techIDs     ,"technology"); updateUpgrades(techIDs     );
		loadIDsFromFile(FILE_SUBSTANCE_ID,substanceIDs,"substance" ); updateUpgrades(substanceIDs);
	}
	private static void loadIDsFromFile(String filePath, IDMap map, String idLabel) {
		File file = new File(filePath);
		if (!file.isFile()) return;
		
		long start = System.currentTimeMillis();
		SaveViewer.log_ln("Read "+idLabel+" IDs from file \""+filePath+"\"...");
		String str;
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8))) {
			while ((str=in.readLine())!=null) {
				int pos = str.indexOf('=');
				if (pos<0) continue;
				
				String idStr = str.substring(0, pos);
				String value = str.substring(pos+1);
				
				if (idStr.endsWith(".obsolete")) {
					idStr = idStr.substring(0, idStr.length()-".type".length());
					GeneralizedID id = map.get(idStr);
					id.isObsolete = true;
				}
				else if (idStr.endsWith(".type")) {
					idStr = idStr.substring(0, idStr.length()-".type".length());
					GeneralizedID id = map.get(idStr);
					id.type = GeneralizedID.Type.getType(value);
				}
				else if (idStr.endsWith(".symbol")) {
					idStr = idStr.substring(0, idStr.length()-".symbol".length());
					GeneralizedID id = map.get(idStr);
					id.setSymbol(value);
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
				else if (idStr.endsWith(".upgradeCat")) {
					idStr = idStr.substring(0, idStr.length()-".upgradeCat".length());
					GeneralizedID id = map.get(idStr);
					id.upgradeCat = Images.UpgradeCategory.getValue(value);
				}
				else if (idStr.endsWith(".upgradeStr")) {
					idStr = idStr.substring(0, idStr.length()-".upgradeStr".length());
					GeneralizedID id = map.get(idStr);
					id.setUpgradeStr(value);
				}
				else {
					GeneralizedID id = map.get(idStr);
					id.setLabel(value);
				}
			}
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
		
		SaveViewer.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}

//	public static void createFilesWithObsoleteIDs() {
//		for (GeneralizedID id:productIDs  .getValues()) id.isObsolete=true;
//		for (GeneralizedID id:techIDs     .getValues()) id.isObsolete=true;
//		for (GeneralizedID id:substanceIDs.getValues()) id.isObsolete=true;
//		
//		saveIDsToFile(FILE_PRODUCT_ID  +".obsolete.txt",productIDs  ,"product"    );
//		saveIDsToFile(FILE_TECH_ID     +".obsolete.txt",techIDs     ,"technologie");
//		saveIDsToFile(FILE_SUBSTANCE_ID+".obsolete.txt",substanceIDs,"substance"  );
//		
//		for (GeneralizedID id:productIDs  .getValues()) id.isObsolete=false;
//		for (GeneralizedID id:techIDs     .getValues()) id.isObsolete=false;
//		for (GeneralizedID id:substanceIDs.getValues()) id.isObsolete=false;
//	}
	
	public static void saveProductIDsToFile  () { saveIDsToFile(FILE_PRODUCT_ID  ,productIDs  ,"product"    ); }
	public static void saveTechIDsToFile     () { saveIDsToFile(FILE_TECH_ID     ,techIDs     ,"technologie"); }
	public static void saveSubstanceIDsToFile() { saveIDsToFile(FILE_SUBSTANCE_ID,substanceIDs,"substance"  ); }

	public static void saveIDsToFile(String filePath, IDMap map, String idLabel) {
		long start = System.currentTimeMillis();
		SaveViewer.log_ln("Write "+idLabel+" IDs to file \""+filePath+"\"...");
		
		File file = new File(filePath);
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8));) {
			for (String idStr:map.getSortedKeys()) {
				GeneralizedID id = map.get(idStr);
				out.printf("%s=%s\r\n",idStr,id.getLabel());
				if (id.isObsolete        ) out.printf("%s.obsolete=\r\n"    ,idStr);
				if (id.type!=null        ) out.printf("%s.type=%s\r\n"      ,idStr,id.type);
				if (id.hasSymbol       ()) out.printf("%s.symbol=%s\r\n"    ,idStr,id.symbol);
				if (id.hasImageFileName()) out.printf("%s.image=%s\r\n"     ,idStr,id.getImageFileName());
				if (id.hasImageBG      ()) out.printf("%s.imageBG=%06X\r\n" ,idStr,id.getImageBG());
				if (id.upgradeCat!=null  ) out.printf("%s.upgradeCat=%s\r\n",idStr,id.upgradeCat);
				if (id.upgradeStr!=null  ) out.printf("%s.upgradeStr=%s\r\n",idStr,id.upgradeStr);
			}
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		
		SaveViewer.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}

	public static class GeneralizedID {
		
		public enum Type {
			MultitoolWeapon          ("Multitool-Waffe"),
			MultitoolWeaponUpgrade   ("Multitool-Waffen-Upgrade",true),
			MultitoolExtension       ("Multitool-Erweiterung"),
			MultitoolExtensionUpgrade("Multitool-Erw.-Upgrade",true),
			ExosuitExtension         ("Exo-Anzug-Erweiterung"),
			ExosuitExtensionUpgrade  ("Exo-Anzug-Erw.-Upgrade",true),
			ExocraftWeapon           ("Exo-Fahrzeug-Waffe"),
			ExocraftWeaponUpgrade    ("Exo-Fahrzeug-Waffen-Upgrade",true),
			ExocraftExtension        ("Exo-Fahrzeug-Erweiterung"),
			ExocraftExtensionUpgrade ("Exo-Fahrzeug-Erw.-Upgrade",true),
			ShipWeapon               ("Schiffswaffe"),
			ShipWeaponUpgrade        ("Schiffswaffen-Upgrade",true),
			ShipExtension            ("Schiffs-Erweiterung"),
			ShipExtensionUpgrade     ("Schiffs-Erw.-Upgrade",true),
			FreighterExtension       ("Frachter-Erweiterung"),
			FreighterExtensionUpgrade("Frachter-Erw.-Upgrade",true),
			BaseComponent            ("Basis-Komponente"),
			BaseDekoration           ("Basis-Dekoration"),
			BaseExternal             ("Basis-Außenanlage"),
			Resource                 ("Rohstoff"),
			Alloy                    ("Legierung"),
			AtlasSeed                ("Atlas-Samen"),
			Product                  ("Allgemeines Produkt"),
			Energy                   ("Energie-Produkt"),
			Plant                    ("Pflanze"),
			PlantProduct             ("Frucht"),
			RaceGift                 ("Völker-Geschenk");
			
			private String label;
			public boolean isUpgrade;
			
			Type(String label) { this(label,false); }
			Type(String label, boolean isUpgrade) { this.label = label; this.isUpgrade = isUpgrade; }
			
			public String getLabel() { return label; }
			public static Type getType(String str) {
				try { return valueOf(str); }
				catch (Exception e) { return null; }
			}
		}
		
		public final String id;
		public boolean isObsolete;
		public String label;
		public String symbol;
		public Type type;
		public Images.UpgradeCategory upgradeCat;
		public String  upgradeStr;
		private String imageFileName;
		private Integer imageBackground;
		final HashMap<SaveGameData,Usage> usage;
		private BufferedImage cachedImage;
		
		private String idBase;
		private Integer idIndex;
		public Vector<GeneralizedID> betterUpgrades;
		
		private GeneralizedID(String id, String label) {
			this.id = id;
			this.isObsolete = false;
			this.label = label;
			this.symbol = null;
			this.type = null;
			this.upgradeCat = null;
			this.upgradeStr = null;
			this.usage = new HashMap<>();
			this.imageFileName = null;
			this.imageBackground = null;
			this.cachedImage = null;
			splitID();
			this.betterUpgrades = new Vector<>();
		}
		public GeneralizedID(String id) {
			this(id,"");
		}
		public GeneralizedID(GeneralizedID other) {
			this.id = other.id;
			this.label = other.label;
			this.symbol = other.symbol;
			this.type = other.type;
			this.upgradeCat = other.upgradeCat;
			this.upgradeStr = other.upgradeStr;
			this.usage = new HashMap<SaveGameData,Usage>(other.usage);
			this.imageFileName = other.imageFileName;
			this.imageBackground = other.imageBackground;
			this.cachedImage = other.cachedImage;
			splitID();
		}
		
		private void splitID() {
			int i;
			for (i = id.length(); i>0; --i) {
				char ch = id.charAt(i-1);
				if (ch<'0'||'9'<ch) break;
			}
			
			idBase = id.substring(0,i);
			
			idIndex = null;
			String indexStr = id.substring(i);
			if (!indexStr.isEmpty()) 
				try { idIndex = Integer.parseInt(indexStr); }
				catch (NumberFormatException e) {}
		}
		
		public boolean hasLabel () { return label !=null && !label .isEmpty(); }
		public boolean hasSymbol() { return symbol!=null && !symbol.isEmpty(); }
		public void setLabel (String label ) { this.label  = (label ==null||label .isEmpty())?null:label ; }
		public void setSymbol(String symbol) { this.symbol = (symbol==null||symbol.isEmpty())?null:symbol; }
		public String getLabel () { return label ==null?"":label ; }
		public String getSymbol() { return symbol==null?"":symbol; }
		
		
		public String getName() {
			if (!hasLabel ()) return "["+id+"]";
			if (!hasSymbol()) return label+" ["+id+"]";
			return "("+symbol+") "+label+" ["+id+"]";
		}
		
		public void setUpgradeStr(String str) { this.upgradeStr = (str==null||str.isEmpty())?null:str; }
		public String getUpgradeStr() { return upgradeStr==null?"":upgradeStr; }
		
		public boolean hasImageFileName() { return imageFileName!=null; }
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
		
		public void setUsage(SaveGameData source, Usage.Type usageType) {
			getUsage(source).generalUsages.add(usageType);
		}
		public Usage getUsage(SaveGameData source) {
			Usage newUsage = new Usage();
			Usage oldUsage = usage.putIfAbsent(source, newUsage);
			return oldUsage==null?newUsage:oldUsage;
		}
	
		public static class Usage {
			public enum Type {
				BuildingObject("Bo"), Blueprint("Bl"), InventorySlot("In");
				
				private final String keyChar;
				Type(String keyChar) { this.keyChar = keyChar; }
			}
			
			public final HashSet<Type>  generalUsages;
			public final Vector<String> inventoryUsages;
			public final Vector<String> blueprintUsages;
			
			public Usage() {
				generalUsages = new HashSet<>();
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
				return inventoryUsages.isEmpty() && blueprintUsages.isEmpty() && generalUsages.isEmpty();
			}

			public String generalUsagesToString() {
				String str = "";
				for (Type type:generalUsages) str += type.keyChar;
				return str;
			}
		}
	}

	private static <T> T[] addNull(T[] arr) {
		Vector<T> vec = new Vector<>(Arrays.asList(arr));
		vec.insertElementAt(null,0);
		return vec.toArray(Arrays.copyOf(arr,0));
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
			Gui.ListMenuItems.ExternFunction<GeneralizedID.Type> setType = new Gui.ListMenuItems.ExternFunction<GeneralizedID.Type>() {
				@Override public void setResult(GeneralizedID.Type value) {
					updateAfterContextMenuAction(setType(value));
				}
				@Override public void configureMenuItem(JCheckBoxMenuItem menuItem, Type value) {
					menuItem.setText(value==null?"<none>":value.getLabel());
				}
			};
			Gui.ListMenu<GeneralizedID.Type> typeListMenu_Std     = new Gui.ListMenu<GeneralizedID.Type>("Type", types, null, setType);
			Gui.ListMenu<GeneralizedID.Type> typeListMenu_Image   = new Gui.ListMenu<GeneralizedID.Type>("Type", types, null, setType);
			Gui.ListMenu<GeneralizedID.Type> typeListMenu_ImageBG = new Gui.ListMenu<GeneralizedID.Type>("Type", types, null, setType);
			Gui.ListMenu<GeneralizedID.Type> typeListMenu_Group   = new Gui.ListMenu<GeneralizedID.Type>("Type of selected", types, null, setType);
			
			Images.UpgradeCategory[] upgrcats = addNull(Images.UpgradeCategory.values());
			Gui.ListMenuItems.ExternFunction<Images.UpgradeCategory> setUpgradeCat = new Gui.ListMenuItems.ExternFunction<Images.UpgradeCategory>() {
				@Override public void setResult(Images.UpgradeCategory value) {
					updateAfterContextMenuAction(setUpgradeCat(value));
				}
				@Override public void configureMenuItem(JCheckBoxMenuItem menuItem, Images.UpgradeCategory value) {
					menuItem.setText(value==null?"<none>":value.getLabel());
					menuItem.setIcon(value==null?null:new ImageIcon(Images.UpgradeCategoryImages.createImage(value,20,20,Color.BLACK)));
				}
			};
			Gui.ListMenu<Images.UpgradeCategory> upgrcatListMenu_Std     = new Gui.ListMenu<Images.UpgradeCategory>("Upgrade Icon", upgrcats, null, setUpgradeCat);
			Gui.ListMenu<Images.UpgradeCategory> upgrcatListMenu_Image   = new Gui.ListMenu<Images.UpgradeCategory>("Upgrade Icon", upgrcats, null, setUpgradeCat);
			Gui.ListMenu<Images.UpgradeCategory> upgrcatListMenu_ImageBG = new Gui.ListMenu<Images.UpgradeCategory>("Upgrade Icon", upgrcats, null, setUpgradeCat);
			Gui.ListMenu<Images.UpgradeCategory> upgrcatListMenu_Group   = new Gui.ListMenu<Images.UpgradeCategory>("Upgrade Icon of selected", upgrcats, null, setUpgradeCat);
			
			NamedColor[] colors = addNull(SaveViewer.images.colorValues);
			Gui.NamedColorListMenu.ExternFunction setImageBG = new Gui.NamedColorListMenu.ExternFunction() {
				@Override public void setResult(NamedColor value) {
					updateAfterContextMenuAction(setImageBG(value==null?null:value.value));
				}
			};
			
			Gui.NamedColorListMenu colorListMenu_Std         = new Gui.NamedColorListMenu("Background", colors, null, setImageBG);
			Gui.NamedColorListMenu colorListMenu_Image       = new Gui.NamedColorListMenu("Background", colors, null, setImageBG);
			Gui.NamedColorListMenu colorListMenu_ImageBG     = new Gui.NamedColorListMenu("Background", colors, null, setImageBG);
			Gui.NamedColorListMenu colorListMenu_UpgradeIcon = new Gui.NamedColorListMenu("Background", colors, null, setImageBG);
			Gui.NamedColorListMenu colorListMenu_Group       = new Gui.NamedColorListMenu("Background of selected", colors, null, setImageBG);
			SaveViewer.images.addColorListListender(new Images.ColorListListender() {
				@Override public void colorAdded(NamedColor color) {
					NamedColor[] colors = addNull(SaveViewer.images.colorValues);
					colorListMenu_Std        .updateValues(colors);
					colorListMenu_Image      .updateValues(colors);
					colorListMenu_ImageBG    .updateValues(colors);
					colorListMenu_UpgradeIcon.updateValues(colors);
					colorListMenu_Group      .updateValues(colors);
				}
			});
			
			DebugTableContextMenu contextMenuStd         = new DebugTableContextMenu(table);
			DebugTableContextMenu contextMenuImage       = new DebugTableContextMenu(table);
			DebugTableContextMenu contextMenuImageBG     = new DebugTableContextMenu(table);
			DebugTableContextMenu contextMenuGroup       = new DebugTableContextMenu(table);
			
			contextMenuGroup.addSeparator();
			contextMenuGroup.add(typeListMenu_Group);
			contextMenuGroup.add(colorListMenu_Group);
			contextMenuGroup.add(createMenuItem("ImageFile of selected ...",ActionCommand.SelectImage4AllSelected));
			contextMenuGroup.add(upgrcatListMenu_Group);
			contextMenuGroup.addSeparator();
			contextMenuGroup.add(createMenuItem("Clear ImageFile of selected",ActionCommand.ClearImage,SaveViewer.ToolbarIcons.Delete));
			contextMenuGroup.add(createMenuItem("Paste ImageFile of selected",ActionCommand.PasteImage,SaveViewer.ToolbarIcons.Paste));
			contextMenuGroup.addSeparator();
			contextMenuGroup.add(createMenuItem("Clear Background of selected",ActionCommand.ClearBackground,SaveViewer.ToolbarIcons.Delete));
			contextMenuGroup.add(createMenuItem("Paste Background of selected",ActionCommand.PasteBackground,SaveViewer.ToolbarIcons.Paste));
			contextMenuGroup.addSeparator();
			contextMenuGroup.add(createMenuItem("Add Background Color",ActionCommand.AddBackgroundColor));
			
			contextMenuStd.addSeparator();
			addStandardItems(contextMenuStd, typeListMenu_Std, colorListMenu_Std, upgrcatListMenu_Std);
			
			contextMenuImage.addSeparator();
			addStandardItems(contextMenuImage, typeListMenu_Image, colorListMenu_Image, upgrcatListMenu_Image);
			contextMenuImage.addSeparator();
			contextMenuImage.add(createMenuItem("Clear ImageFile",ActionCommand.ClearImage,SaveViewer.ToolbarIcons.Delete));
			contextMenuImage.add(createMenuItem("Copy ImageFile" ,ActionCommand.CopyImage ,SaveViewer.ToolbarIcons.Copy  ));
			contextMenuImage.add(createMenuItem("Paste ImageFile",ActionCommand.PasteImage,SaveViewer.ToolbarIcons.Paste ));
			
			contextMenuImageBG.addSeparator();
			addStandardItems(contextMenuImageBG, typeListMenu_ImageBG, colorListMenu_ImageBG, upgrcatListMenu_ImageBG);
			contextMenuImageBG.addSeparator();
			contextMenuImageBG.add(createMenuItem("Clear Background",ActionCommand.ClearBackground,SaveViewer.ToolbarIcons.Delete));
			contextMenuImageBG.add(createMenuItem("Copy Background" ,ActionCommand.CopyBackground ,SaveViewer.ToolbarIcons.Copy  ));
			contextMenuImageBG.add(createMenuItem("Paste Background",ActionCommand.PasteBackground,SaveViewer.ToolbarIcons.Paste ));
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
						
						table.stopCellEditing();
						if (table.getSelectedRowCount()>1) {
							typeListMenu_Group.clearSelection();
							colorListMenu_Group.clearSelection();
							upgrcatListMenu_Group.clearSelection();
							contextMenuGroup.show(table, e.getX(), e.getY());
						} else {
							table.clearSelection();
							DebugTableContextMenu contextMenu;
							Gui.ListMenu<GeneralizedID.Type> typeListMenu;
							Gui.NamedColorListMenu colorListMenu;
							Gui.ListMenu<Images.UpgradeCategory> upgrcatListMenu;
							switch (tableModel.getColumnID(colM)) {
							case Image:
								typeListMenu = typeListMenu_Image;
								colorListMenu = colorListMenu_Image;
								upgrcatListMenu = upgrcatListMenu_Image;
								contextMenu = contextMenuImage;
								break;
							case ImgBG:
								typeListMenu = typeListMenu_ImageBG;
								colorListMenu = colorListMenu_ImageBG;
								upgrcatListMenu = upgrcatListMenu_ImageBG;
								contextMenu = contextMenuImageBG;
								break;
							default:
								typeListMenu = typeListMenu_Std;
								colorListMenu = colorListMenu_Std;
								upgrcatListMenu = upgrcatListMenu_Std;
								contextMenu = contextMenuStd;
								break;
							}
							if (clickedID!=null) {
								typeListMenu.setValue(clickedID.type);
								colorListMenu.setValue(SaveViewer.images.getColor(clickedID.getImageBG()));
								upgrcatListMenu.setValue(clickedID.upgradeCat);
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

		private void addStandardItems(DebugTableContextMenu contextMenu, Gui.ListMenu<GeneralizedID.Type> typeListMenu, Gui.NamedColorListMenu colorListMenu, Gui.ListMenu<Images.UpgradeCategory> upgrcatListMenu) {
			contextMenu.add(createMenuItem("Edit ID",ActionCommand.EditID));
			contextMenu.add(typeListMenu);
			contextMenu.add(colorListMenu);
			contextMenu.add(createMenuItem("ImageFile ...",ActionCommand.SelectImage));
			contextMenu.add(upgrcatListMenu);
		}
		
		private JMenuItem createMenuItem(String label, ActionCommand actionCommand) {
			JMenuItem menuItem = new JMenuItem(label);
			menuItem.addActionListener(this);
			menuItem.setActionCommand(actionCommand.toString());
			return menuItem;
		}
		
		private JMenuItem createMenuItem(String label, ActionCommand actionCommand, SaveViewer.ToolbarIcons icon) {
			JMenuItem menuItem = createMenuItem(label,actionCommand);
			menuItem.setIcon(SaveViewer.toolbarIS.getIcon(icon));
			return menuItem;
		}
	
		enum ActionCommand { EditID, SelectImage, ClearImage, CopyImage, PasteImage, ClearBackground, CopyBackground, PasteBackground, AddBackgroundColor, SelectImage4AllSelected }
		
		@Override
		public void actionPerformed(ActionEvent e) {
			ActionCommand actionCommand = ActionCommand.valueOf(e.getActionCommand());
			//if (clickedID!=null) System.out.printf("%s(%s)\r\n",actionCommand,clickedID.id);
			if (clickedID==null) return;
			
			boolean idChanged = false;
			String clipboardValue;
			switch(actionCommand) {
			case EditID: {
				table.stopCellEditing();
				EditIdDialog dlg = new EditIdDialog(mainwindow,clickedID);
				dlg.showDialog();
				if (dlg.hasDataChanged()) {
					dlg.transferChangesTo(clickedID);
					idChanged = true;
				}
			} break;
			
			case SelectImage: {
				Images.ImageGridDialog dlg = new Images.ImageGridDialog(mainwindow,"Select image of "+clickedID.getName(),clickedID.getImageFileName());
				dlg.showDialog();
				if (dlg.hasChoosen()) {
					table.stopCellEditing();
					String result = dlg.getImageFileName();
					clickedID.setImageFileName(result);
					idChanged = true;
				}
			} break;
			case SelectImage4AllSelected: {
				Images.ImageGridDialog dlg = new Images.ImageGridDialog(mainwindow, "Select image of seleted IDs", null);
				dlg.showDialog();
				if (dlg.hasChoosen()) {
					table.stopCellEditing();
					String image = dlg.getImageFileName();
					int[] rows = table.getSelectedRows();
					for (int row:rows) {
						GeneralizedID id = tableModel.getValue(table.convertRowIndexToModel(row));
						id.setImageFileName(image); 
					}
					idChanged=true;
				}
			} break;
				
			case ClearImage      : idChanged = setImageFileName(null); break;
			case ClearBackground : idChanged = setImageBG(null); break;
				
			case CopyImage      : SaveViewer.copyToClipBoard(clickedID.getImageFileName()); break;
			case CopyBackground : if (clickedID.hasImageBG()) { SaveViewer.copyToClipBoard(String.format("%06X", clickedID.getImageBG())); } break;
			
			case PasteImage:
				clipboardValue = SaveViewer.pasteFromClipBoard();
				if (clipboardValue!=null) idChanged = setImageFileName(clipboardValue);
				break;
			case PasteBackground:
				clipboardValue = SaveViewer.pasteFromClipBoard();
				if (clipboardValue!=null)
					try { idChanged = setImageBG(Integer.parseInt(clipboardValue, 16)); }
					catch (NumberFormatException e1) {}
				break;
				
			case AddBackgroundColor:
				table.stopCellEditing();
				SaveViewer.images.showAddColorDialog(mainwindow,"Add Color");
				break;
			}
			updateAfterContextMenuAction(idChanged);
		}

		private boolean setImageBG(Integer bgColor) {
			return changeValue(id->id.setImageBG(bgColor));
		}

		private boolean setImageFileName(String imageFileName) {
			return changeValue(id->id.setImageFileName(imageFileName));
		}

		private boolean setUpgradeCat(Images.UpgradeCategory value) {
			return changeValue(id->id.upgradeCat = value);
		}

		private boolean setType(GeneralizedID.Type value) {
			return changeValue(id->id.type = value);
		}

		private boolean changeValue(Consumer<GeneralizedID> Consumer) {
			int[] rows = table.getSelectedRows();
			if (rows.length>1) {
				table.stopCellEditing();
				for (int row:rows) {
					GeneralizedID id = tableModel.getValue(table.convertRowIndexToModel(row));
					if (id!=null) Consumer.accept(id); 
				}
				return true;
			}
			if (clickedID!=null) {
				table.stopCellEditing();
				Consumer.accept(clickedID);
				return true;
			}
			return false;
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
		
		private void prepareTable() {
			ComboboxCellEditor<Images.UpgradeCategory> upgrCatCellEditor =
					new TableView.ComboboxCellEditor<Images.UpgradeCategory>(addNull(Images.UpgradeCategory.values()));
			TableView.UpgradeCategoryRenderer upgrCatRenderer = new TableView.UpgradeCategoryRenderer();
			upgrCatCellEditor.setRenderer(upgrCatRenderer);
			setCellEditor  (GeneralizedIDColumnID.UpgrCat, upgrCatCellEditor);
			setCellRenderer(GeneralizedIDColumnID.UpgrCat, upgrCatRenderer);
			
			
			
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
			
			BufferedImage image = id.getImage();
			if (image==null) {
				imageField.setIcon(null);
			} else {
				imageField.setIcon(new ImageIcon(image));
				imageField.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));							
			}
			
			textarea.append("ID     : "+id.id+"\r\n");
			if (id.type!=null  ) textarea.append("Type   : "+id.type.label+"\r\n");
			if (id.hasLabel  ()) textarea.append("Label  : "+id.label+"\r\n");
			if (id.hasSymbol ()) textarea.append("Symbol : "+id.symbol+"\r\n");
			if (id.hasImageFileName  ()) textarea.append("Image  : "+id.getImageFileName()+"\r\n");
			if (id.hasImageBG()) textarea.append("ImageBG: "+String.format("%06X",id.getImageBG())+"\r\n");
			
			if (id.type!=null && id.type.isUpgrade) {
				if (!id.betterUpgrades.isEmpty())
					textarea.append("Upgrades:\r\n");
				for (GeneralizedID upg:id.betterUpgrades)
					textarea.append("   "+(upg.hasLabel()?upg.label:upg.id)+"\r\n");
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
			ID     ("ID"           ,                String.class,  80,-1,120,120),
			Type   ("Type"         ,    GeneralizedID.Type.class, 100,-1,140,140),
			Symbol ("Sym."         ,                String.class,  10,-1, 30, 30),
			Label  ("Label"        ,                String.class, 150,-1,200,200),
			Image  ("Image"        ,                String.class, 150,-1,250,250),
			ImgBG  ("Background"   ,            NamedColor.class, 150,-1,200,200),
			UpgrCat("Upgrade Image",Images.UpgradeCategory.class,  80,-1,100,100),
			UpgrStr("Upgrade Label",                String.class,  50,-1, 80, 80),
			Usage  (""             ,                String.class,  50,-1, 80, 80);
			
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
					GeneralizedIDColumnID.ImgBG,
					GeneralizedIDColumnID.UpgrCat,
					GeneralizedIDColumnID.UpgrStr
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
					if (id.hasLabel())
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
				case ImgBG :
				case UpgrCat:
				case UpgrStr: return true;
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
					case Image :
					case ImgBG :
					case UpgrCat:
					case UpgrStr:
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
				case UpgrCat: return id.upgradeCat;
				case UpgrStr: return id.upgradeStr;
				case Usage :
					Usage usage = id.usage.get(usageKeys.get(columnIndex-columns.length).data);
					if (usage==null) return "";
					if (usage.isEmpty()) return "";
					String str = "";;
					if (!usage.inventoryUsages.isEmpty()) { if (!str.isEmpty()) str+=" "; str += usage.inventoryUsages.size()+"xI"; }
					if (!usage.blueprintUsages.isEmpty()) { if (!str.isEmpty()) str+=" "; str += usage.blueprintUsages.size()+"xB"; }
					if (!usage.generalUsages  .isEmpty()) { if (!str.isEmpty()) str+=" "; str += usage.generalUsagesToString(); }
					
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
				case Symbol: id.setSymbol(aValue==null?"":aValue.toString()); break;
				case Label : id.setLabel (aValue==null?"":aValue.toString()); break;
				case Image : id.setImageFileName(aValue); break;
				case ImgBG : id.setImageBG((aValue instanceof NamedColor)?((NamedColor)aValue).value:null); break;
				case UpgrCat: id.upgradeCat = (aValue instanceof Images.UpgradeCategory)?(Images.UpgradeCategory)aValue:null; break;
				case UpgrStr: id.setUpgradeStr(aValue==null?"":aValue.toString()); break;
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
	
		private abstract static class ModifiedJTextField {
			private JTextField textField;
			ModifiedJTextField() {
				textField = new JTextField();
				textField.setText(getValue());
				textField.addActionListener(e->setValue(textField.getText()));
				textField.addFocusListener(new FocusListener() {
					@Override public void focusGained(FocusEvent e) {}
					@Override public void focusLost(FocusEvent e) { setValue(textField.getText()); }
				});
			}
			public JTextField getTextField() { return textField; }
			protected abstract String getValue();
			protected abstract void setValue(String str);
		}
		
		public EditIdDialog(Window parent, GeneralizedID originalID) {
			super(parent, getDlgTitle(originalID), ModalityType.APPLICATION_MODAL, false);
			
			this.id = new GeneralizedID(originalID);
			this.hasDataChanged = false;
			
			textarea = new JTextArea();
			textarea.setEditable(false);
			JScrollPane textareaScrollPane = new JScrollPane(textarea);
			textareaScrollPane.getViewport().setPreferredSize(new Dimension(400, 100));
			
			JTextField labelTextField = new ModifiedJTextField() {
				@Override protected String getValue() { return id.getLabel(); }
				@Override protected void setValue(String str) { id.setLabel(str); setTitle(getDlgTitle(id)); dataChanged(); }
			}.getTextField();
			
			JTextField symbolTextField = new ModifiedJTextField() {
				@Override protected String getValue() { return id.getSymbol(); }
				@Override protected void setValue(String str) { id.setSymbol(str); setTitle(getDlgTitle(id)); dataChanged(); }
			}.getTextField();
			symbolTextField.setPreferredSize(new Dimension(50,16));
			
			JComboBox<GeneralizedID.Type> cmbbxTypes = new JComboBox<GeneralizedID.Type>(addNull(GeneralizedID.Type.values()));
			cmbbxTypes.setSelectedItem(id.type);
			cmbbxTypes.addActionListener(e->{
				id.type = (GeneralizedID.Type)cmbbxTypes.getSelectedItem();
				dataChanged();
			});
			cmbbxTypes.setRenderer(new TableView.NonStringRenderer<GeneralizedID.Type>(t->{if (t instanceof GeneralizedID.Type) return ((GeneralizedID.Type)t).getLabel(); return null; }));
			
			JComboBox<String> cmbbxImages = new JComboBox<String>(addNull(SaveViewer.images.imagesNames));
			cmbbxImages.setSelectedItem(id.getImageFileName());
			cmbbxImages.addActionListener(e->{ id.setImageFileName((String)cmbbxImages.getSelectedItem()); dataChanged(); });
			
			imageListListender = new Images.ImageListListender() {
				@Override public void imageListChanged() {
					cmbbxImages.setModel(new DefaultComboBoxModel<>(addNull(SaveViewer.images.imagesNames)));
					cmbbxImages.setSelectedItem(id.getImageFileName());
				}
			};
			
			JComboBox<Images.NamedColor> cmbbxColors = new JComboBox<Images.NamedColor>(new DefaultComboBoxModel<Images.NamedColor>(addNull(SaveViewer.images.colorValues)));
			cmbbxColors.setRenderer(new TableView.NamedColorRenderer());
			cmbbxColors.setSelectedItem(SaveViewer.images.getColor(id.getImageBG()));
			cmbbxColors.addActionListener(e->{
				NamedColor namedColor = (Images.NamedColor)cmbbxColors.getSelectedItem();
				id.setImageBG(namedColor==null?null:namedColor.value);
				dataChanged();
			});
			
			colorListListender = new Images.ColorListListender() {
				@Override public void colorAdded(Images.NamedColor color) {
					cmbbxColors.addItem(color);
					cmbbxColors.revalidate();
				}
			};
			
			JComboBox<Images.UpgradeCategory> cmbbxUpgradeIcon = new JComboBox<>(Images.UpgradeCategory.values());
			cmbbxUpgradeIcon.setRenderer(new TableView.UpgradeCategoryRenderer());
			cmbbxUpgradeIcon.setSelectedItem(id.upgradeCat);
			cmbbxUpgradeIcon.addActionListener(e->{id.upgradeCat=(Images.UpgradeCategory)cmbbxUpgradeIcon.getSelectedItem(); dataChanged();});
			
			JTextField upgradeStrTextField = new ModifiedJTextField() {
				@Override protected String getValue() { return id.getUpgradeStr(); }
				@Override protected void setValue(String str) { id.setUpgradeStr(str); dataChanged(); }
			}.getTextField();
			upgradeStrTextField.setPreferredSize(new Dimension(80,16));
			
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
			
			addComp(cmbbxPanel,cmbbxPanelLayout,c,new JLabel("Upgrade : ",JLabel.RIGHT),0,0,1,1, GridBagConstraints.HORIZONTAL);
			addComp(cmbbxPanel,cmbbxPanelLayout,c,cmbbxUpgradeIcon,1,0,2,1, GridBagConstraints.BOTH);
			addComp(cmbbxPanel,cmbbxPanelLayout,c,upgradeStrTextField,0,0,GridBagConstraints.REMAINDER,1, GridBagConstraints.BOTH);
			
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
			Images.ImageGridDialog dlg = new Images.ImageGridDialog(this,"Select image of "+id.getName(),id.getImageFileName());
			dlg.showDialog();
			if (dlg.hasChoosen()) {
				String result = dlg.getImageFileName();
				id.setImageFileName(result);
				dataChanged();
				cmbbxImages.setSelectedItem(result);
			}
		}
	
		private JButton createButton(String title, ActionListener l) {
			JButton button = new JButton(title);
			button.addActionListener(l);
			return button;
		}
	
		private void dataChanged() {
			hasDataChanged = true;
			showValues();
		}

		private void showValues() {
			textarea.setText("");
			
			textarea.append("ID     : "+id.id+"\r\n");
			textarea.append("Label  : "+id.label+"\r\n");
			textarea.append("Symbol : "+id.symbol+"\r\n");
			textarea.append("Type   : "+(id.type==null?"":id.type.label)+"\r\n");
			textarea.append("Image  : "+(id.hasImageFileName  ()?id.getImageFileName():"<none>")+"\r\n");
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
		public void transferChangesTo(GeneralizedID id) {
			id.label  = this.id.label;
			id.symbol = this.id.symbol;
			id.type   = this.id.type;
			id.setImageFileName(this.id.imageFileName);
			id.setImageBG      (this.id.imageBackground);
			id.upgradeCat = this.id.upgradeCat;
			id.upgradeStr = this.id.upgradeStr;
		}

//		public Integer getImageBG() { return id.getImageBG(); }
//		public String getImageFileName() { return id.getImageFileName(); }
//		public String getLabel() { return id.label; }
	}
	
}