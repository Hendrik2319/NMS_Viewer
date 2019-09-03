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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
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
import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.ComboboxCellEditor;
import net.schwarzbaer.gui.Tables.NonStringRenderer;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID.Type;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID.Usage;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Images.NamedColor;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Stats.StatValue.KnownID;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.DiscoverableObject;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.DiscoverableObject.ExtraInfo;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Galaxy;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Planet;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Region;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.SolarSystem;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.UniverseAddress;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.SaveGameView;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.DebugTableContextMenu;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.SimplifiedTable;

public class GameInfos {
	private static final String FILE_KNOWN_STAT_ID = "NMS_Viewer.KnownStatID.txt";
	private static final String FILE_PRODUCT_ID    = "NMS_Viewer.ProdIDs.txt";
	private static final String FILE_TECH_ID       = "NMS_Viewer.TechIDs.txt";
	private static final String FILE_SUBSTANCE_ID  = "NMS_Viewer.SubstanceIDs.txt";
	private static final String FILE_UNIVERSE_OBJECT_DATA = "NMS_Viewer.UniverseObjects.txt";
	
	private static HashMap<Long,UniverseObjectData> universeObjectDataArr;
	private static HashMap<Integer,HashSet<String>> conflictLevelLabels;
	
	private static class UOD_Region extends UniverseObjectData {
		public UOD_Region(UniverseAddress ua) {
			super(ua, Type.Region);
		}
		public UOD_Region(Region region) {
			this(region.getUniverseAddress());
			this.oldname = region.oldname;
			this.name = region.name;
		}
	}
	
	private static class UOD_UniverseObject extends UniverseObjectData {
		Vector<ExtraInfo> extraInfos;
		public UOD_UniverseObject(UniverseAddress ua, Type type) {
			super(ua,type);
			extraInfos = new Vector<>();
		}
		public UOD_UniverseObject(UniverseAddress ua, Type type, DiscoverableObject uo) {
			this(ua,type);
			this.oldname = uo.getOldOriginalName();
			this.   name = uo.   getOriginalName();
			for (ExtraInfo ei:uo.extraInfos)
				extraInfos.add(new ExtraInfo(ei));
		}
	}
	
	private static class UOD_SolarSystem extends UOD_UniverseObject {
		Universe.SolarSystem.Race race;
		Universe.SolarSystem.StarClass starClass;
		Double distanceToCenter;
		int conflictLevel;
		String conflictLevelLabel;
		Boolean isUnexplored;
		Boolean hasAtlasInterface; 
		Boolean hasBlackHole;
		Long blackHoleTarget;
		
		public UOD_SolarSystem(UniverseAddress ua) {
			super(ua, Type.SolarSystem);
			race = null;
			isUnexplored = null;
			starClass = null;
			distanceToCenter = null;
			conflictLevel = -1;
			conflictLevelLabel = null;
			hasAtlasInterface = null;
			hasBlackHole = null;
			blackHoleTarget = null;
		}
		public UOD_SolarSystem(SolarSystem sys) {
			super(sys.getUniverseAddress(), Type.SolarSystem, sys);
			race = sys.race;
			isUnexplored = sys.isUnexplored;
			starClass = sys.starClass;
			distanceToCenter = sys.distanceToCenter;
			conflictLevel = sys.conflictLevel;
			conflictLevelLabel = sys.conflictLevelLabel;
			hasAtlasInterface = sys.hasAtlasInterface;
			hasBlackHole = sys.hasBlackHole;
			blackHoleTarget = (sys.blackHoleTarget==null || !sys.hasBlackHole)?null:sys.blackHoleTarget.getAddress();
		}
	}
	
	private static class UOD_Planet extends UOD_UniverseObject {
		Universe.Planet.Biome biome;
		boolean areSentinelsAggressive;
		boolean withWater;
		boolean withGravitinoBalls;
		Universe.Planet.BuriedTreasure buriedTreasure;
		public boolean hasExtremeBiome;
		
		public UOD_Planet(UniverseAddress ua) {
			super(ua, Type.Planet);
			biome = null;
			hasExtremeBiome = false;
			areSentinelsAggressive = false;
			withWater = false;
			withGravitinoBalls = false;
			buriedTreasure = null;
		}
		public UOD_Planet(Planet planet) {
			super(planet.getUniverseAddress(),Type.Planet,planet);
			biome = planet.biome;
			hasExtremeBiome = planet.hasExtremeBiome;
			areSentinelsAggressive = planet.areSentinelsAggressive;
			withWater = planet.withWater;
			withGravitinoBalls = planet.withGravitinoBalls;
			buriedTreasure = planet.buriedTreasure;
		}
	}
	
	private static class UniverseObjectData implements Comparable<UniverseObjectData>{
		enum Type { Region,SolarSystem,Planet }
		
		final UniverseAddress universeAddress;
		final UniverseObjectData.Type type;
		String oldname;
		String name;
		
		protected UniverseObjectData(UniverseAddress universeAddress, UniverseObjectData.Type type) {
			this.universeAddress = universeAddress;
			this.type = type;
			this.oldname = null;
			this.name = null;
		}
	
		@Override
		public int compareTo(UniverseObjectData other) {
			if (other==null) return -1;
			return universeAddress.compareTo(other.universeAddress);
		}
		
		
	}
	
	public static void updateConflictLevelLabels() {
		conflictLevelLabels = new HashMap<>();
		for (UniverseObjectData uoData:universeObjectDataArr.values()) {
			if (!(uoData instanceof UOD_SolarSystem)) continue;
			UOD_SolarSystem system = (UOD_SolarSystem)uoData;
			if (system.conflictLevelLabel!=null) {
				HashSet<String> labels = conflictLevelLabels.get(system.conflictLevel);
				if (labels==null) conflictLevelLabels.put(system.conflictLevel, labels = new HashSet<>());
				labels.add(system.conflictLevelLabel);
			}
		}
	}
	
	public static String[] getConflictLevelLabels(int conflictLevel) {
		HashSet<String> labels;
		if (conflictLevel<1) {
			labels = new HashSet<>();
			for (HashSet<String> set:conflictLevelLabels.values())
				labels.addAll(set);
			
		} else
			labels = conflictLevelLabels.get(conflictLevel);
		
		if (labels==null || labels.isEmpty()) return new String[0];
		String[] arr = labels.toArray(new String[0]);
		Arrays.sort(arr);
		return arr;
	}

	public static int getConflictLevel(String label) {
		for (Integer level:conflictLevelLabels.keySet()) {
			HashSet<String> labels = conflictLevelLabels.get(level);
			if (labels.contains(label)) return level;
		}
		return -1;
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
			
			UniverseObjectData uoData = null;
			@SuppressWarnings("unused")
			UOD_Region         region = null;
			UOD_SolarSystem    system = null;
			UOD_Planet         planet = null;
			UOD_UniverseObject uniObj = null;
			
			String nextShortLabel = null;
			Boolean showInParent = null;
			
			String str, valueStr;
			while ((str=in.readLine())!=null) {
				if (str.isEmpty()) continue;
				if ((str.startsWith("[Reg") || str.startsWith("[Sys") || str.startsWith("[Pln")) && str.endsWith("]")) {
					uoData = null;
					region = null;
					system = null;
					planet = null;
					uniObj = null;
					nextShortLabel = null;
					showInParent = null;
					
					String addressStr = str.substring("[Sys".length(), str.length()-"]".length());
					long address;
					try { address = Long.parseLong(addressStr, 16); }
					catch (NumberFormatException e) {
						System.err.printf("Can't parse universe address in: \"%s\"\r\n",str);
						continue;
					}
					UniverseAddress ua = new UniverseAddress(address);
					if (str.startsWith("[Reg") && ua.isRegion     ()) uoData =          region = new UOD_Region     (ua);
					if (str.startsWith("[Sys") && ua.isSolarSystem()) uoData = uniObj = system = new UOD_SolarSystem(ua);
					if (str.startsWith("[Pln") && ua.isPlanet     ()) uoData = uniObj = planet = new UOD_Planet     (ua);
					if (uoData != null) universeObjectDataArr.put(address, uoData);
					continue;
				}
				if (uoData!=null) {
					if (str.startsWith("name=")) {
						uoData.name = str.substring("name=".length());
						continue;
					}
					if (str.startsWith("oldname=")) {
						uoData.oldname = str.substring("oldname=".length());
						continue;
					}
				}
				if (system!=null) {
					if (str.startsWith("race=")) {
						valueStr = str.substring("race=".length());
						try { system.race = Universe.SolarSystem.Race.valueOf(valueStr); }
						catch (Exception e) { system.race = null; }
						continue;
					}
					if (str.equals("unexplored")) {
						system.isUnexplored = true;
						continue;
					}
					if (str.startsWith("atlasinterface=")) {
						valueStr = str.substring("atlasinterface=".length());
						system.hasAtlasInterface = valueStr.equalsIgnoreCase("true");
						continue;
					}
					if (str.startsWith("blackhole=")) {
						valueStr = str.substring("blackhole=".length());
						system.hasBlackHole = valueStr.equalsIgnoreCase("true");
						continue;
					}
					if (str.startsWith("blackholetarget=")) {
						valueStr = str.substring("blackholetarget=".length());
						try { system.blackHoleTarget = Long.parseLong(valueStr, 16); }
						catch (NumberFormatException e) { system.blackHoleTarget = null; }
						continue;
					}
					if (str.startsWith("class=")) {
						valueStr = str.substring("class=".length());
						try { system.starClass = Universe.SolarSystem.StarClass.valueOf(valueStr); }
						catch (Exception e) { system.starClass = null; }
						continue;
					}
					if (str.startsWith("distance=")) {
						valueStr = str.substring("distance=".length());
						try { system.distanceToCenter = Double.parseDouble(valueStr); }
						catch (NumberFormatException e) { system.distanceToCenter = null; }
						continue;
					}
					if (str.startsWith("conflict=")) {
						valueStr = str.substring("conflict=".length());
						try { system.conflictLevel = Integer.parseInt(valueStr); }
						catch (NumberFormatException e) { system.conflictLevel = -1; }
						continue;
					}
					if (str.startsWith("conflict_label=")) {
						valueStr = str.substring("conflict_label=".length());
						system.conflictLevelLabel = valueStr;
						continue;
					}
				}
				if (planet!=null) {
					if (str.startsWith("biome=")) {
						valueStr = str.substring("biome=".length());
						try { planet.biome = Universe.Planet.Biome.valueOf(valueStr); }
						catch (Exception e) { planet.biome = null; }
						continue;
					}
					if (str.equals("is extreme")) {
						planet.hasExtremeBiome = true;
						continue;
					}
					if (str.equals("aggrSentinels") || str.startsWith("aggrSentinels=") || str.equals("sentinel=Aggressive")) {
						planet.areSentinelsAggressive = true;
						continue;
					}
					if (str.equals("with water")) {
						planet.withWater = true;
						continue;
					}
					if (str.equals("gravitino balls")) {
						planet.withGravitinoBalls = true;
						continue;
					}
					if (str.startsWith("buriedTreasure=")) {
						valueStr = str.substring("buriedTreasure=".length());
						try { planet.buriedTreasure = Universe.Planet.BuriedTreasure.valueOf(valueStr); }
						catch (Exception e) { planet.buriedTreasure = null; }
						continue;
					}
				}
				if (uniObj!=null) {
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
						valueStr = str.substring("info=".length());
						if (nextShortLabel!=null && showInParent!=null)
							uniObj.extraInfos.add(new ExtraInfo(showInParent,nextShortLabel,valueStr));
						nextShortLabel=null;
						showInParent=null;
						continue;
					}
				}
			}
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
		
		updateConflictLevelLabels();
		
		SaveViewer.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}

	public static void readUniverseObjectDataFromDataPool(Universe universe, boolean forceCreation) {
		long start = System.currentTimeMillis();
		SaveViewer.log_ln("Read data of universe objects from data pool ... ");
		
		boolean withOutput = false; // DEBUG;
		
		Region         region;
		SolarSystem    system;
		Planet         planet;
		DiscoverableObject uniObj;
		
		@SuppressWarnings("unused")
		UOD_Region         uod_region;
		UOD_SolarSystem    uod_system;
		UOD_Planet         uod_planet;
		UOD_UniverseObject uod_uniObj;
		
		String objName;
		for (Long address:universeObjectDataArr.keySet()) {
			UniverseObjectData uoData = universeObjectDataArr.get(address);
			UniverseAddress ua = new UniverseAddress(address);
			
			objName = null;
			region = null;
			system = null;
			planet = null;
			uniObj = null;
			uod_region = null;
			uod_system = null;
			uod_planet = null;
			uod_uniObj = null;
			
			switch(uoData.type) {
			case Region     :              uod_region = (UOD_Region     )uoData; if (ua.isRegion     ())          region = forceCreation ? universe.getOrCreateRegion     (ua) : universe.findRegion     (ua); break;
			case SolarSystem: uod_uniObj = uod_system = (UOD_SolarSystem)uoData; if (ua.isSolarSystem()) uniObj = system = forceCreation ? universe.getOrCreateSolarSystem(ua) : universe.findSolarSystem(ua); break;
			case Planet     : uod_uniObj = uod_planet = (UOD_Planet     )uoData; if (ua.isPlanet     ()) uniObj = planet = forceCreation ? universe.getOrCreatePlanet     (ua) : universe.findPlanet     (ua); break;
			}
			if (region!=null) { objName = "region "+ua.getCoordinates_Region();   if (withOutput) SaveViewer.log_ln("Region %s"      ,ua.getCoordinates_Region  ()); }
			if (system!=null) { objName = "solar system "+ua.getSigBoostCode();   if (withOutput) SaveViewer.log_ln("Solar system %s",ua.getSigBoostCode        ()); }
			if (planet!=null) { objName = "planet "+ua.getExtendedSigBoostCode(); if (withOutput) SaveViewer.log_ln("Planet %s"      ,ua.getExtendedSigBoostCode()); }
			
			if (uoData.name!=null) {
				if (uniObj!=null) uniObj.setOriginalName(uoData.name);
				if (region!=null) region.setName(uoData.name);
				if (withOutput && objName!=null) SaveViewer.log_ln("   Name of %s was defined: \"%s\"",objName,uoData.name);
			}
			
			if (uoData.oldname!=null) {
				if (uniObj!=null) uniObj.setOldOriginalName(uoData.oldname);
				if (region!=null) region.setOldName(uoData.oldname);
				if (withOutput && objName!=null) SaveViewer.log_ln("   Old Name of %s was defined: \"%s\"",objName,uoData.oldname);
			}
			
			if (system!=null) {
				if (uod_system.race!=null) {
					system.race = uod_system.race;
					if (withOutput) SaveViewer.log_ln("   Race of %s was defined: %s", objName, system.race);
				}
				if (uod_system.starClass!=null) {
					system.starClass = uod_system.starClass;
					if (withOutput) SaveViewer.log_ln("   Star Class of %s was defined: %s", objName, system.starClass);
				}
				if (uod_system.distanceToCenter!=null) {
					system.distanceToCenter = uod_system.distanceToCenter;
					if (withOutput) SaveViewer.log_ln("   Distance to galactic center of %s was defined: %s", objName, system.distanceToCenter);
				}
				if (uod_system.conflictLevel>=0) {
					system.conflictLevel = uod_system.conflictLevel;
					if (withOutput) SaveViewer.log_ln("   Conflict Level of %s was defined: %d", objName, system.conflictLevel);
				}
				if (uod_system.conflictLevelLabel!=null) {
					system.conflictLevelLabel = uod_system.conflictLevelLabel;
					if (withOutput) SaveViewer.log_ln("   Conflict Level Label of %s was defined: \"%s\"", objName, system.conflictLevelLabel);
				}
				if (uod_system.isUnexplored!=null) {
					system.isUnexplored = uod_system.isUnexplored;
					if (withOutput) SaveViewer.log_ln("   %s was defined as unexplored: %s", objName, system.isUnexplored);
				}
				if (uod_system.hasAtlasInterface!=null) {
					system.hasAtlasInterface = uod_system.hasAtlasInterface;
					if (withOutput) SaveViewer.log_ln("   %s has an Atlas Interface: %s", objName, system.hasAtlasInterface);
				}
				if (uod_system.hasBlackHole!=null) {
					system.hasBlackHole = uod_system.hasBlackHole;
					if (withOutput) SaveViewer.log_ln("   %s has a Black Hole: %s", objName, system.hasBlackHole);
				}
				if (uod_system.blackHoleTarget!=null) {
					system.blackHoleTarget = new UniverseAddress(uod_system.blackHoleTarget);
					if (withOutput) SaveViewer.log_ln("   %s has a Black Hole Target: %s", objName, system.blackHoleTarget);
				}
			}
			
			if (planet!=null) {
				if (uod_planet.biome!=null) {
					planet.biome = uod_planet.biome;
					if (withOutput) SaveViewer.log_ln("   Biome of %s was defined: %s",objName,planet.biome);
				}
				if (uod_planet.hasExtremeBiome) {
					planet.hasExtremeBiome = uod_planet.hasExtremeBiome;
					if (withOutput) SaveViewer.log_ln("   %s has extreme biome",objName);
				}
				if (uod_planet.areSentinelsAggressive) {
					planet.areSentinelsAggressive = uod_planet.areSentinelsAggressive;
					if (withOutput) SaveViewer.log_ln("   Sentinels of %s are aggressive",objName);
				}
				if (uod_planet.withWater) {
					planet.withWater = uod_planet.withWater;
					if (withOutput) SaveViewer.log_ln("   %s has water",objName);
				}
				if (uod_planet.withGravitinoBalls) {
					planet.withGravitinoBalls = uod_planet.withGravitinoBalls;
					if (withOutput) SaveViewer.log_ln("   %s has Gravitino Balls",objName);
				}
				if (uod_planet.buriedTreasure!=null) {
					planet.buriedTreasure = uod_planet.buriedTreasure;
					if (withOutput) SaveViewer.log_ln("   Buried Treasure of %s was defined: %s",objName,planet.buriedTreasure);
				}
			}
			
			if (uniObj!=null) {
				uniObj.extraInfos.clear();
				for (ExtraInfo ei:uod_uniObj.extraInfos) {
					uniObj.extraInfos.add(ei);
					if (withOutput) SaveViewer.log_ln("   Info of %s was defined: ( \"%s\", \"%s\" )",objName,ei.shortLabel,ei.info);
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
				universeObjectDataArr.put(ua.getAddress(),new UOD_Region(r));
				for (SolarSystem sys:r.solarSystems) {
					ua = sys.getUniverseAddress();
					universeObjectDataArr.put(ua.getAddress(),new UOD_SolarSystem(sys));
					for (Planet p:sys.planets) {
						ua = p.getUniverseAddress();
						universeObjectDataArr.put(ua.getAddress(),new UOD_Planet(p));
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
		@SuppressWarnings("unused")
		UOD_Region         uod_region = null;
		UOD_SolarSystem    uod_system = null;
		UOD_Planet         uod_planet = null;
		UOD_UniverseObject uod_uniObj = null;
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8));) {
			for (UniverseObjectData uoData:values) {
				if (!isFirst) out.println();
				isFirst = false;
				
				uod_region = null;
				uod_system = null;
				uod_planet = null;
				uod_uniObj = null;
				switch (uoData.type) {
				case Region     :              uod_region = (UOD_Region     )uoData; out.printf("[Reg%014X]\r\n",uoData.universeAddress.getAddress()); break;
				case SolarSystem: uod_uniObj = uod_system = (UOD_SolarSystem)uoData; out.printf("[Sys%014X]\r\n",uoData.universeAddress.getAddress()); break;
				case Planet     : uod_uniObj = uod_planet = (UOD_Planet     )uoData; out.printf("[Pln%014X]\r\n",uoData.universeAddress.getAddress()); break;
				}
				
				if (uoData.   name!=null) out.printf(   "name=%s\r\n",uoData.   name);
				if (uoData.oldname!=null) out.printf("oldname=%s\r\n",uoData.oldname);
				
				if (uod_system!=null) {
					if (uod_system.isUnexplored!=null && uod_system.isUnexplored)
						out.printf("unexplored\r\n");
					else {
						if (uod_system.race              !=null) out.printf("race=%s\r\n",uod_system.race);
						if (uod_system.conflictLevel     >=0   ) out.printf("conflict=%d\r\n",uod_system.conflictLevel);
						if (uod_system.conflictLevelLabel!=null) out.printf("conflict_label=%s\r\n",uod_system.conflictLevelLabel);
					}
					if (uod_system.hasAtlasInterface!=null) {
						if (uod_system.hasAtlasInterface || SolarSystem.shouldHaveAtlasInterface(uod_system.universeAddress.solarSystemIndex))
							out.printf("atlasinterface=%s\r\n",uod_system.hasAtlasInterface);
					}
					if (uod_system.hasBlackHole!=null) {
						if (uod_system.hasBlackHole || SolarSystem.shouldHaveBlackHole(uod_system.universeAddress.solarSystemIndex))
							out.printf("blackhole=%s\r\n",uod_system.hasBlackHole);
					}
					if (uod_system.blackHoleTarget !=null) out.printf("blackholetarget=%014X\r\n",uod_system.blackHoleTarget);
					if (uod_system.starClass       !=null) out.printf("class=%s\r\n",uod_system.starClass);
					if (uod_system.distanceToCenter!=null) out.printf(Locale.ENGLISH,"distance=%f\r\n",uod_system.distanceToCenter.doubleValue());
				}
				
				if (uod_planet!=null) {
					if (uod_planet.biome           !=null) out.printf("biome=%s\r\n",uod_planet.biome);
					if (uod_planet.hasExtremeBiome       ) out.printf("is extreme\r\n");
					if (uod_planet.areSentinelsAggressive) out.printf("aggrSentinels\r\n");
					if (uod_planet.withWater             ) out.printf("with water\r\n");
					if (uod_planet.withGravitinoBalls    ) out.printf("gravitino balls\r\n");
					if (uod_planet.buriedTreasure  !=null) out.printf("buriedTreasure=%s\r\n",uod_planet.buriedTreasure);
				}
				
				if (uod_uniObj!=null) {
					for (ExtraInfo ei:uod_uniObj.extraInfos)
						if (!ei.shortLabel.isEmpty() || !ei.info.isEmpty()) {
							String showInParentStr="";
							if (uoData.type==UniverseObjectData.Type.Planet && ei.showInParent) showInParentStr = ".P";
							out.printf("short%s=%s\r\n", showInParentStr, ei.shortLabel);
							out.printf("info=%s\r\n" , ei.info);
						}
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
		
		boolean somethingChanged = false;
		long start = System.currentTimeMillis();
		SaveViewer.log_ln("Read known StatIDs from file \"%s\" ...", file.getPath());
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
					SaveViewer.log_warn_ln("   changed fullName of %16s  from \"%s\"  into \"%s\"",knownID,knownID.fullName,fullName);
					knownID.fullName = fullName;
					somethingChanged = true;
				}
			}
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
		
		if (!somethingChanged) SaveViewer.log_warn_ln("   All values from file are already known. File \"%s\" can be deleted.", file.getPath());
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
	
	public static void removeUsages(SaveGameData oldData) {
		for (GeneralizedID id:productIDs  .getValues()) id.usage.remove(oldData);
		for (GeneralizedID id:techIDs     .getValues()) id.usage.remove(oldData);
		for (GeneralizedID id:substanceIDs.getValues()) id.usage.remove(oldData);
	}

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
			generalizedID.getUsage(source).addGeneralUsage(usageType);
			generalizedID.isObsolete = false;
			return generalizedID;
		}
	
		public GeneralizedID get(String id) {
			GeneralizedID newID = new GeneralizedID(id);
			GeneralizedID existingID = map.putIfAbsent(id, newID);
			return existingID==null ? newID : existingID;
		}

		public void remove(GeneralizedID id) {
			map.remove(id.id);
		}
	}

	public static void loadAllIDsFromFiles() {
		loadIDsFromFile(FILE_PRODUCT_ID  ,productIDs  ,"product"   );
		loadIDsFromFile(FILE_TECH_ID     ,techIDs     ,"technology");
		loadIDsFromFile(FILE_SUBSTANCE_ID,substanceIDs,"substance" );
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
					idStr = idStr.substring(0, idStr.length()-".obsolete".length());
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
				else if (idStr.endsWith(".upgradeClass")) {
					idStr = idStr.substring(0, idStr.length()-".upgradeClass".length());
					GeneralizedID id = map.get(idStr);
					id.upgradeClass = GeneralizedID.UpgradeClass.parseValue(value);
				}
				else if (idStr.indexOf('.')<0) {
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
	

	public static void saveAllIDsToFiles() {
		saveProductIDsToFile  ();
		saveTechIDsToFile     ();
		saveSubstanceIDsToFile();
	}
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
				if (id.isObsolete        ) out.printf("%s.obsolete=\r\n"      ,idStr);
				if (id.type!=null        ) out.printf("%s.type=%s\r\n"        ,idStr,id.type);
				if (id.hasSymbol       ()) out.printf("%s.symbol=%s\r\n"      ,idStr,id.symbol);
				if (id.hasImageFileName()) out.printf("%s.image=%s\r\n"       ,idStr,id.getImageFileName());
				if (id.hasImageBG      ()) out.printf("%s.imageBG=%06X\r\n"   ,idStr,id.getImageBG());
				if (id.upgradeClass!=null) out.printf("%s.upgradeClass=%s\r\n",idStr,id.upgradeClass);
			}
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		
		SaveViewer.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}

	public static class GeneralizedID {
		
		public enum Type {
			MultitoolWeapon           ("Multitool-Waffe"),
			MultitoolWeaponUpgrade    ("Multitool-Waffen-Upgrade",true),
			MultitoolWeaponUpgradeM   ("Multitool-Waffen-Upgr.-Modul",true),
			MultitoolExtension        ("Multitool-Erweiterung"),
			MultitoolExtensionUpgrade ("Multitool-Erw.-Upgrade",true),
			MultitoolExtensionUpgradeM("Multitool-Erw.-Upgr.-Modul",true),
			ExosuitExtension          ("Exo-Anzug-Erweiterung"),
			ExosuitExtensionUpgrade   ("Exo-Anzug-Erw.-Upgrade",true),
			ExosuitExtensionUpgradeM  ("Exo-Anzug-Erw.-Upgr.-Modul",true),
			ExocraftWeapon            ("Exo-Fahrzeug-Waffe"),
			ExocraftWeaponUpgrade     ("Exo-Fahrzeug-Waffen-Upgrade",true),
			ExocraftWeaponUpgradeM    ("Exo-Fahrzeug-Waffen-Upgr.-Modul",true),
			ExocraftExtension         ("Exo-Fahrzeug-Erweiterung"),
			ExocraftExtensionUpgrade  ("Exo-Fahrzeug-Erw.-Upgrade",true),
			ExocraftExtensionUpgradeM ("Exo-Fahrzeug-Erw.-Upgr.-Modul",true),
			ShipWeapon                ("Schiffswaffe"),
			ShipWeaponUpgrade         ("Schiffswaffen-Upgrade",true),
			ShipWeaponUpgradeM        ("Schiffswaffen-Upgr.-Modul",true),
			ShipExtension             ("Schiffs-Erweiterung"),
			ShipExtensionUpgrade      ("Schiffs-Erw.-Upgrade",true),
			ShipExtensionUpgradeM     ("Schiffs-Erw.-Upgr.-Modul",true),
			FreighterExtension        ("Frachter-Erweiterung"),
			FreighterExtensionUpgrade ("Frachter-Erw.-Upgrade",true),
			FreighterExtensionUpgradeM("Frachter-Erw.-Upgr.-Modul",true),
			BaseComponent             ("Basis-Komponente"),
			BaseComponent_FertigTeil  ("Basis-Komponente (Fertigteil)"),
			BaseDekoration            ("Basis-Dekoration"),
			BaseExternal              ("Basis-Außenanlage"),
			BaseComponentFreighter    ("Basis-Komponente (Frachter)"),
			BaseDekorationFreighter   ("Basis-Dekoration (Frachter)"),
			BaseComponentUnderWater   ("Basis-Komponente (Unterwasser)"),
			Resource                  ("Rohstoff"),
			ResourceCrystal           ("Rohstoff-Kristall"),
			Alloy                     ("Legierung"),
			AtlasSeed                 ("Atlas-Samen"),
			Product                   ("Produkt"),
			ProductExpensive          ("Teures Produkt"),
			TradeProduct              ("Reines Handels-Produkt"),
			Energy                    ("Energie-Produkt"),
			Ammunition                ("Munition"),
			Plant                     ("Pflanze"),
			PlantProduct              ("Frucht"),
			RaceGift                  ("Völker-Geschenk"),
			Special                   ("Speziell"),
			Treasure                  ("Schatz"),
			PlanetTrophy              ("Planeten-Trophäe"),
			;
			
			private String label;
			//public boolean isUpgrade;
			
			Type(String label) { this(label,false); }
			Type(String label, boolean isUpgrade) { this.label = label; /*this.isUpgrade = isUpgrade;*/ }
			
			public String getLabel() { return label; }
			public static Type getType(String str) {
				try { return valueOf(str); }
				catch (Exception e) { return null; }
			}
		}
		
		public enum UpgradeClass {
			S,A,B,C;
			public static UpgradeClass parseValue(String str) {
				try { return valueOf(str); }
				catch (Exception e) { return null; }
			}
			public String getLabel() {
				return this.toString()+"-Class";
			}
		}
		
		public final String id;
		public boolean isObsolete;
		public String label;
		public String symbol;
		public Type type;
		public UpgradeClass upgradeClass;
		private String imageFileName;
		private Integer imageBackground;
		final HashMap<SaveGameData,Usage> usage;
		private BufferedImage cachedImage;
		
		private GeneralizedID(String id, String label) {
			this.id = id;
			this.isObsolete = false;
			this.label = label;
			this.symbol = null;
			this.type = null;
			this.upgradeClass = null;
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
			this.upgradeClass = other.upgradeClass;
			this.usage = new HashMap<SaveGameData,Usage>(other.usage);
			this.imageFileName = other.imageFileName;
			this.imageBackground = other.imageBackground;
			this.cachedImage = other.cachedImage;
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
			public final Vector<String> bboUsages;
			
			public Usage() {
				generalUsages   = new HashSet<>();
				inventoryUsages = new Vector<>();
				blueprintUsages = new Vector<>();
				bboUsages       = new Vector<>();
			}
			
			public void addGeneralUsage(Type type) {
				generalUsages.add(type);
			}
			
			public void addInventoryUsage(String label, int x, int y) {
				inventoryUsages.add(String.format("%s @ (%d,%d)", label, x, y));
			}
	
			public void addBlueprintUsage(String label, int i) {
				blueprintUsages.add(label+" Blueprint "+i);
			}
			
			public void addBBOUsage(String label, int i) {
				bboUsages.add(label+": Object "+i);
			}
	
			public boolean isEmpty() {
				return inventoryUsages.isEmpty() && blueprintUsages.isEmpty() && bboUsages.isEmpty() && generalUsages.isEmpty();
			}

			public String generalUsagesToString() {
				String str = "";
				for (Type type:generalUsages) str += (str.isEmpty()?"":",")+type.keyChar;
				return "{"+str+"}";
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
			table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION, true);
			table.getSelectionModel().addListSelectionListener(e->{
				if (table.getSelectedRowCount()==1)
					showID(tableModel.getValue(table.convertRowIndexToModel(table.getSelectedRow())));
				else
					showID(null);
			});
			prepareTable();
			
			GeneralizedID.Type[] types = SaveViewer.addNull(GeneralizedID.Type.values());
			Gui.ListMenuItems.ExternFunction<GeneralizedID.Type> setType = new Gui.ListMenuItems.ExternFunction<GeneralizedID.Type>() {
				@Override public void setResult(GeneralizedID.Type value) {
					updateAfterContextMenuAction(setType(value),null);
				}
				@Override public void configureMenuItem(JMenuItem menuItem, Type value) {
					menuItem.setText(value==null?"<none>":value.getLabel());
				}
			};
			Gui.ListMenu<GeneralizedID.Type> typeListMenu_Std     = new Gui.ListMenu<GeneralizedID.Type>("Type", types, null, setType);
			Gui.ListMenu<GeneralizedID.Type> typeListMenu_Image   = new Gui.ListMenu<GeneralizedID.Type>("Type", types, null, setType);
			Gui.ListMenu<GeneralizedID.Type> typeListMenu_ImageBG = new Gui.ListMenu<GeneralizedID.Type>("Type", types, null, setType);
			Gui.ListMenu<GeneralizedID.Type> typeListMenu_Group   = new Gui.ListMenu<GeneralizedID.Type>("Type of selected", types, null, setType);
			
			GeneralizedID.UpgradeClass[] upgradeClasses = SaveViewer.addNull(GeneralizedID.UpgradeClass.values());
			Gui.ListMenuItems.ExternFunction<GeneralizedID.UpgradeClass> setUpgradeCat = new Gui.ListMenuItems.ExternFunction<GeneralizedID.UpgradeClass>() {
				@Override public void setResult(GeneralizedID.UpgradeClass value) {
					updateAfterContextMenuAction(setUpgradeClass(value),null);
				}
				@Override public void configureMenuItem(JMenuItem menuItem, GeneralizedID.UpgradeClass value) {
					menuItem.setText(value==null?"<none>":value.getLabel());
				}
			};
			Gui.ListMenu<GeneralizedID.UpgradeClass> upgrclsListMenu_Std     = new Gui.ListMenu<GeneralizedID.UpgradeClass>("Upgrade Class", upgradeClasses, null, setUpgradeCat);
			Gui.ListMenu<GeneralizedID.UpgradeClass> upgrclsListMenu_Image   = new Gui.ListMenu<GeneralizedID.UpgradeClass>("Upgrade Class", upgradeClasses, null, setUpgradeCat);
			Gui.ListMenu<GeneralizedID.UpgradeClass> upgrclsListMenu_ImageBG = new Gui.ListMenu<GeneralizedID.UpgradeClass>("Upgrade Class", upgradeClasses, null, setUpgradeCat);
			Gui.ListMenu<GeneralizedID.UpgradeClass> upgrclsListMenu_Group   = new Gui.ListMenu<GeneralizedID.UpgradeClass>("Upgrade Class of selected", upgradeClasses, null, setUpgradeCat);
			
			NamedColor[] colors = SaveViewer.addNull(SaveViewer.images.colorValues);
			Gui.NamedColorListMenu.ExternFunction setImageBG = new Gui.NamedColorListMenu.ExternFunction() {
				@Override public void setResult(NamedColor value) {
					updateAfterContextMenuAction(setImageBG(value==null?null:value.value),null);
				}
			};
			
			Gui.NamedColorListMenu colorListMenu_Std         = new Gui.NamedColorListMenu("Background", colors, null, setImageBG);
			Gui.NamedColorListMenu colorListMenu_Image       = new Gui.NamedColorListMenu("Background", colors, null, setImageBG);
			Gui.NamedColorListMenu colorListMenu_ImageBG     = new Gui.NamedColorListMenu("Background", colors, null, setImageBG);
			Gui.NamedColorListMenu colorListMenu_UpgradeIcon = new Gui.NamedColorListMenu("Background", colors, null, setImageBG);
			Gui.NamedColorListMenu colorListMenu_Group       = new Gui.NamedColorListMenu("Background of selected", colors, null, setImageBG);
			SaveViewer.images.addColorListListender(new Images.ColorListListender() {
				@Override public void colorAdded(NamedColor color) {
					NamedColor[] colors = SaveViewer.addNull(SaveViewer.images.colorValues);
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
			contextMenuGroup.add(createMenuItem("Delete selected",ActionCommand.DeleteID,SaveViewer.ToolbarIcons.Delete));
			contextMenuGroup.addSeparator();
			contextMenuGroup.add(typeListMenu_Group);
			contextMenuGroup.add(colorListMenu_Group);
			contextMenuGroup.add(createMenuItem("ImageFile of selected ...",ActionCommand.SelectImage4AllSelected));
			contextMenuGroup.add(upgrclsListMenu_Group);
			contextMenuGroup.addSeparator();
			contextMenuGroup.add(createMenuItem("Clear ImageFile of selected",ActionCommand.ClearImage,SaveViewer.ToolbarIcons.Delete));
			contextMenuGroup.add(createMenuItem("Paste ImageFile of selected",ActionCommand.PasteImage,SaveViewer.ToolbarIcons.Paste));
			contextMenuGroup.addSeparator();
			contextMenuGroup.add(createMenuItem("Clear Background of selected",ActionCommand.ClearBackground,SaveViewer.ToolbarIcons.Delete));
			contextMenuGroup.add(createMenuItem("Paste Background of selected",ActionCommand.PasteBackground,SaveViewer.ToolbarIcons.Paste));
			contextMenuGroup.addSeparator();
			contextMenuGroup.add(createMenuItem("Add Background Color",ActionCommand.AddBackgroundColor));
			
			contextMenuStd.addSeparator();
			addStandardItems(contextMenuStd, typeListMenu_Std, colorListMenu_Std, upgrclsListMenu_Std);
			
			contextMenuImage.addSeparator();
			addStandardItems(contextMenuImage, typeListMenu_Image, colorListMenu_Image, upgrclsListMenu_Image);
			contextMenuImage.addSeparator();
			contextMenuImage.add(createMenuItem("Clear ImageFile",ActionCommand.ClearImage,SaveViewer.ToolbarIcons.Delete));
			contextMenuImage.add(createMenuItem("Copy ImageFile" ,ActionCommand.CopyImage ,SaveViewer.ToolbarIcons.Copy  ));
			contextMenuImage.add(createMenuItem("Paste ImageFile",ActionCommand.PasteImage,SaveViewer.ToolbarIcons.Paste ));
			
			contextMenuImageBG.addSeparator();
			addStandardItems(contextMenuImageBG, typeListMenu_ImageBG, colorListMenu_ImageBG, upgrclsListMenu_ImageBG);
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
							upgrclsListMenu_Group.clearSelection();
							contextMenuGroup.show(table, e.getX(), e.getY());
						} else {
							table.clearSelection();
							DebugTableContextMenu contextMenu;
							Gui.ListMenu<GeneralizedID.Type> typeListMenu;
							Gui.NamedColorListMenu colorListMenu;
							Gui.ListMenu<GeneralizedID.UpgradeClass> upgrclsListMenu;
							switch (tableModel.getColumnID(colM)) {
							case Image:
								typeListMenu = typeListMenu_Image;
								colorListMenu = colorListMenu_Image;
								upgrclsListMenu = upgrclsListMenu_Image;
								contextMenu = contextMenuImage;
								break;
							case ImgBG:
								typeListMenu = typeListMenu_ImageBG;
								colorListMenu = colorListMenu_ImageBG;
								upgrclsListMenu = upgrclsListMenu_ImageBG;
								contextMenu = contextMenuImageBG;
								break;
							default:
								typeListMenu = typeListMenu_Std;
								colorListMenu = colorListMenu_Std;
								upgrclsListMenu = upgrclsListMenu_Std;
								contextMenu = contextMenuStd;
								break;
							}
							if (clickedID!=null) {
								typeListMenu.setValue(clickedID.type);
								colorListMenu.setValue(SaveViewer.images.getColor(clickedID.getImageBG()));
								upgrclsListMenu.setValue(clickedID.upgradeClass);
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

		private void addStandardItems(DebugTableContextMenu contextMenu, Gui.ListMenu<GeneralizedID.Type> typeListMenu, Gui.NamedColorListMenu colorListMenu, Gui.ListMenu<GeneralizedID.UpgradeClass> upgrclsListMenu) {
			contextMenu.add(createMenuItem("Edit ID",ActionCommand.EditID));
			contextMenu.add(typeListMenu);
			contextMenu.add(colorListMenu);
			contextMenu.add(createMenuItem("ImageFile ...",ActionCommand.SelectImage));
			contextMenu.add(upgrclsListMenu);
			contextMenu.addSeparator();;
			contextMenu.add(createMenuItem("Delete",ActionCommand.DeleteID,SaveViewer.ToolbarIcons.Delete));
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
	
		enum ActionCommand { DeleteID, EditID, SelectImage, ClearImage, CopyImage, PasteImage, ClearBackground, CopyBackground, PasteBackground, AddBackgroundColor, SelectImage4AllSelected }
		
		@Override
		public void actionPerformed(ActionEvent e) {
			ActionCommand actionCommand = ActionCommand.valueOf(e.getActionCommand());
			//if (clickedID!=null) System.out.printf("%s(%s)\r\n",actionCommand,clickedID.id);
			if (clickedID==null) return;
			
			boolean idChanged = false;
			Vector<Integer> deletedRows = new Vector<>();
			
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
			
			case DeleteID: {
				int[] rows = table.getSelectedRows();
				int rowCount = rows.length>1?rows.length:(clickedCell!=null?1:0);
				if (rowCount>0) {
					String message = "Do you really want to delete "+(rowCount==1?"this ID":("these "+rowCount+" IDs"))+"?";
					if (JOptionPane.YES_OPTION==JOptionPane.showConfirmDialog(mainwindow, message, "Delete IDs", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE)) {
						//SaveViewer.log_ln("Rows to delete: %s", Arrays.toString(rows));
						if (rows.length>1) {
							table.stopCellEditing();
							Vector<Integer> rows2Delete = new Vector<>();
							for (int rowV:rows) rows2Delete.add(table.convertRowIndexToModel(rowV));
							rows2Delete.sort(null);
							for (int i=rows2Delete.size()-1; i>=0; i--)
								if (tableModel.deleteValue(rows2Delete.get(i)))
									deletedRows.add(rows2Delete.get(i));
						} else if (clickedCell!=null) {
							table.stopCellEditing();
							if (tableModel.deleteValue(clickedCell.y))
								deletedRows.add(clickedCell.y);
						}
						//SaveViewer.log_ln("DeletedRows: %s", deletedRows.toString());
					}
				}
			} break;
			
			case SelectImage: {
				Images.SelectImageDialog dlg = new Images.SelectImageDialog(mainwindow,"Select image of "+clickedID.getName(),clickedID.getImageFileName());
				dlg.showDialog();
				if (dlg.hasChoosen()) {
					table.stopCellEditing();
					String result = dlg.getImageFileName();
					clickedID.setImageFileName(result);
					idChanged = true;
				}
			} break;
			case SelectImage4AllSelected: {
				Images.SelectImageDialog dlg = new Images.SelectImageDialog(mainwindow, "Select image of seleted IDs", null);
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
			updateAfterContextMenuAction(idChanged,deletedRows);
		}

		private boolean setImageBG(Integer bgColor) {
			return changeValue(id->id.setImageBG(bgColor));
		}

		private boolean setImageFileName(String imageFileName) {
			return changeValue(id->id.setImageFileName(imageFileName));
		}

		private boolean setUpgradeClass(GeneralizedID.UpgradeClass value) {
			return changeValue(id->id.upgradeClass = value);
		}

		private boolean setType(GeneralizedID.Type value) {
			return changeValue(id->id.type = value);
		}

		private boolean changeValue(Consumer<GeneralizedID> setValue) {
			int[] rows = table.getSelectedRows();
			if (rows.length>1) {
				table.stopCellEditing();
				for (int row:rows) {
					GeneralizedID id = tableModel.getValue(table.convertRowIndexToModel(row));
					if (id!=null) setValue.accept(id);
				}
				return true;
			}
			if (clickedID!=null) {
				table.stopCellEditing();
				setValue.accept(clickedID);
				return true;
			}
			return false;
		}

		private void updateAfterContextMenuAction(boolean idChanged, Vector<Integer> deletedRows) {
			if (idChanged && (deletedRows==null || deletedRows.isEmpty())) {
				if (clickedCell!=null) tableModel.updateTableCell(clickedCell.x,clickedCell.y);
				tableModel.updateAfterCellChange(clickedID);
				if (clickedCell!=null) {
					int row = table.convertRowIndexToView(clickedCell.y);
					table.setRowSelectionInterval(row,row);
				}
				table.repaint();
			} else if (deletedRows!=null && !deletedRows.isEmpty()) {
				tableModel.updateTableRowsRemoved(deletedRows);
				tableModel.updateAfterCellChange(null);
				table.repaint();
			}
			clickedID = null;
			clickedCell = null;
		}
		
		private void prepareTable() {
			ComboboxCellEditor<GeneralizedID.UpgradeClass> upgradeClassCellEditor =
					new ComboboxCellEditor<GeneralizedID.UpgradeClass>(SaveViewer.addNull(GeneralizedID.UpgradeClass.values()));
			NonStringRenderer<GeneralizedID.UpgradeClass> upgradeClassRenderer =
					new NonStringRenderer<GeneralizedID.UpgradeClass>(t->{if (t instanceof GeneralizedID.UpgradeClass) return ((GeneralizedID.UpgradeClass)t).getLabel(); return null; });
			upgradeClassCellEditor.setRenderer(upgradeClassRenderer);
			setCellEditor  (GeneralizedIDColumnID.UpgrCls, upgradeClassCellEditor);
			setCellRenderer(GeneralizedIDColumnID.UpgrCls, upgradeClassRenderer);
			
			ComboboxCellEditor<GeneralizedID.Type> typeCellEditor =
					new ComboboxCellEditor<GeneralizedID.Type>(SaveViewer.addNull(GeneralizedID.Type.values()));
			NonStringRenderer<GeneralizedID.Type> typeRenderer =
					new NonStringRenderer<GeneralizedID.Type>(t->{if (t instanceof GeneralizedID.Type) return ((GeneralizedID.Type)t).getLabel(); return null; });
			typeCellEditor.setRenderer(typeRenderer);
			setCellEditor  (GeneralizedIDColumnID.Type, typeCellEditor);
			setCellRenderer(GeneralizedIDColumnID.Type, typeRenderer);
			
			ComboboxCellEditor<String> imageCellEditor =
					new ComboboxCellEditor<String>(SaveViewer.addNull(SaveViewer.images.imagesNames));
			SaveViewer.images.addImageListListener(new Images.ImageListListener() {
				@Override public void imageListChanged() {
					imageCellEditor.setValues(SaveViewer.addNull(SaveViewer.images.imagesNames));
					tableModel.updateTableColumn(GeneralizedIDColumnID.Image);
				}
			});
			setCellEditor(GeneralizedIDColumnID.Image, imageCellEditor);
			
			ComboboxCellEditor<NamedColor> colorCellEditor =
					new ComboboxCellEditor<NamedColor>(SaveViewer.addNull(SaveViewer.images.colorValues));
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
			
			for (SaveGameData key:id.usage.keySet()) {
				textarea.append("\r\n");
				textarea.append(key.filename+":\r\n");
				Usage usages = id.usage.get(key);
				if (usages.isEmpty())
					textarea.append("   none\r\n");
				for (String str:usages.inventoryUsages) textarea.append("   "+str+"\r\n");
				for (String str:usages.blueprintUsages) textarea.append("   "+str+"\r\n");
				for (String str:usages.bboUsages      ) textarea.append("   "+str+"\r\n");
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
	
		private enum GeneralizedIDColumnID implements Tables.SimplifiedColumnIDInterface {
			Obsolete("Obs"          ,                    String.class,  10,-1, 30, 30),
			ID      ("ID"           ,                    String.class,  80,-1,120,120),
			Type    ("Type"         ,        GeneralizedID.Type.class, 100,-1,160,160),
			Symbol  ("Sym."         ,                    String.class,  10,-1, 30, 30),
			Label   ("Label"        ,                    String.class, 150,-1,200,200),
			Image   ("Image"        ,                    String.class, 150,-1,250,250),
			ImgBG   ("Background"   ,                NamedColor.class, 150,-1,200,200),
			UpgrCls ("Upgrade Class",GeneralizedID.UpgradeClass.class,  80,-1,100,100),
			Usage   (""             ,                    String.class,  50,-1, 80, 80);
			
			private Tables.SimplifiedColumnConfig columnConfig;
			
			GeneralizedIDColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				columnConfig = new Tables.SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			@Override public Tables.SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
		}
	
		private static class GeneralizedIDTableModel extends Tables.SimplifiedTableModel<GeneralizedIDColumnID> {
	
			private static final GeneralizedIDColumnID[] COLUMNS = new GeneralizedIDColumnID[]{
					GeneralizedIDColumnID.Obsolete,
					GeneralizedIDColumnID.ID,
					GeneralizedIDColumnID.Type,
					GeneralizedIDColumnID.Symbol,
					GeneralizedIDColumnID.Label,
					GeneralizedIDColumnID.Image,
					GeneralizedIDColumnID.ImgBG,
					GeneralizedIDColumnID.UpgrCls,
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

			public void updateTableRowsRemoved(Vector<Integer> deletedRows) {
				deletedRows.sort(null);
				int rangeStart = -1;
				for (int i=0; i<deletedRows.size(); i++) {
					int rowM = deletedRows.get(i);
					if (rangeStart==-1) rangeStart = rowM;
					if (i+1>=deletedRows.size() || deletedRows.get(i)>rowM+1) {
						fireTableRowsRemoved(rangeStart,rowM);
						rangeStart = -1;
					}
				}
			}

			public void updateTableCell(int col, int row) {
				fireTableCellUpdate(row, col);
			}
			
			public void updateTableColumn(GeneralizedIDColumnID columnID) {
				fireTableColumnUpdate(getColumn(columnID));
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
				case Obsolete:
				case ID    : return false;
				case Type  :
				case Symbol:
				case Label :
				case Image :
				case ImgBG :
				case UpgrCls: return true;
				case Usage : return false;
				}
				return false;
			}
	
			public GeneralizedID getValue(int rowIndex) {
				if (rowIndex<EXTRA_ROWS) return null;
				return IDs.get(rowIndex-EXTRA_ROWS);
			}

			public boolean deleteValue(int rowIndex) {
				if (rowIndex<EXTRA_ROWS) return false;
				GeneralizedID id = IDs.remove(rowIndex-EXTRA_ROWS);
				sourceIdMap.remove(id);
				//SaveViewer.log_ln("GeneralizedIDTableModel.deleteValue: [%d] -> %s", rowIndex, id==null?"<null>":id.getName());
				return true;
			}

			@Override
			public Object getValueAt(int rowIndex, int columnIndex, GeneralizedIDColumnID columnID) {
				if (rowIndex<EXTRA_ROWS) {
					switch(columnID) {
					case Obsolete: return "";
					case ID    : return String.format(Locale.ENGLISH,"total: %d", IDs.size());
					case Type  : return null;
					case Symbol: return "";
					case Label : return String.format(Locale.ENGLISH,"labeled: %d (%1.1f%%)", numberOfLabledIDs, IDs.isEmpty()?0:numberOfLabledIDs*100.0f/IDs.size());
					case Image :
					case ImgBG :
					case UpgrCls:
					case Usage : return "";
					}
					return null;
				}
				GeneralizedID id = IDs.get(rowIndex-EXTRA_ROWS);
				if (id==null) return null;
				switch(columnID) {
				case Obsolete: return id.isObsolete?"##":"";
				case ID      : return id.id;
				case Type    : return id.type;
				case Symbol  : return id.symbol;
				case Label   : return id.label;
				case Image   : return id.imageFileName;
				case ImgBG   : return SaveViewer.images.getColor( id.getImageBG() );
				case UpgrCls : return id.upgradeClass;
				case Usage :
					Usage usage = id.usage.get(usageKeys.get(columnIndex-columns.length).data);
					if (usage==null) return "";
					if (usage.isEmpty()) return "";
					String str = "";;
					if (!usage.inventoryUsages.isEmpty()) { if (!str.isEmpty()) str+=" "; str += usage.inventoryUsages.size()+"xI"; }
					if (!usage.blueprintUsages.isEmpty()) { if (!str.isEmpty()) str+=" "; str += usage.blueprintUsages.size()+"xB"; }
					if (!usage.bboUsages      .isEmpty()) { if (!str.isEmpty()) str+=" "; str += usage.bboUsages      .size()+"xO"; }
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
				case Obsolete:
				case ID      : return;
				case Type    : id.type   = (aValue instanceof GeneralizedID.Type)?(GeneralizedID.Type)aValue:null; break;
				case Symbol  : id.setSymbol(aValue==null?"":aValue.toString()); break;
				case Label   : id.setLabel (aValue==null?"":aValue.toString()); break;
				case Image   : id.setImageFileName(aValue); break;
				case ImgBG   : id.setImageBG((aValue instanceof NamedColor)?((NamedColor)aValue).value:null); break;
				case UpgrCls : id.upgradeClass = (aValue instanceof GeneralizedID.UpgradeClass)?(GeneralizedID.UpgradeClass)aValue:null; break;
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
		private Images.ImageListListener imageListListender;
	
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
			
			JComboBox<GeneralizedID.Type> cmbbxTypes = new JComboBox<GeneralizedID.Type>(SaveViewer.addNull(GeneralizedID.Type.values()));
			cmbbxTypes.setSelectedItem(id.type);
			cmbbxTypes.addActionListener(e->{
				id.type = (GeneralizedID.Type)cmbbxTypes.getSelectedItem();
				dataChanged();
			});
			cmbbxTypes.setRenderer(new NonStringRenderer<GeneralizedID.Type>(t->{if (t instanceof GeneralizedID.Type) return ((GeneralizedID.Type)t).getLabel(); return null; }));
			
			JComboBox<String> cmbbxImages = new JComboBox<String>(SaveViewer.addNull(SaveViewer.images.imagesNames));
			cmbbxImages.setSelectedItem(id.getImageFileName());
			cmbbxImages.addActionListener(e->{ id.setImageFileName((String)cmbbxImages.getSelectedItem()); dataChanged(); });
			
			imageListListender = new Images.ImageListListener() {
				@Override public void imageListChanged() {
					cmbbxImages.setModel(new DefaultComboBoxModel<>(SaveViewer.addNull(SaveViewer.images.imagesNames)));
					cmbbxImages.setSelectedItem(id.getImageFileName());
				}
			};
			
			JComboBox<Images.NamedColor> cmbbxColors = new JComboBox<Images.NamedColor>(new DefaultComboBoxModel<Images.NamedColor>(SaveViewer.addNull(SaveViewer.images.colorValues)));
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
			
			JComboBox<GeneralizedID.UpgradeClass> cmbbxUpgradeIcon = new JComboBox<>(SaveViewer.addNull(GeneralizedID.UpgradeClass.values())	);
			cmbbxUpgradeIcon.setRenderer(new NonStringRenderer<GeneralizedID.UpgradeClass>(t->{if (t instanceof GeneralizedID.UpgradeClass) return ((GeneralizedID.UpgradeClass)t).getLabel(); return null; }));
			cmbbxUpgradeIcon.setSelectedItem(id.upgradeClass);
			cmbbxUpgradeIcon.addActionListener(e->{id.upgradeClass=(GeneralizedID.UpgradeClass)cmbbxUpgradeIcon.getSelectedItem(); dataChanged();});
			
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
			addComp(cmbbxPanel,cmbbxPanelLayout,c,cmbbxUpgradeIcon,1,0,GridBagConstraints.REMAINDER,1, GridBagConstraints.BOTH);
			
			imageField = new JLabel();
			imageField.setBorder(BorderFactory.createEtchedBorder());
			imageField.setPreferredSize(new Dimension(256,256));
			imageField.setMinimumSize(new Dimension(256,256));
			
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
			SaveViewer.images.addImageListListener(imageListListender);
		}
		@Override public void windowClosed(WindowEvent e) {
			SaveViewer.images.removeColorListListender(colorListListender);
			SaveViewer.images.removeImageListListener(imageListListender);
		}
	
		private void showImageList(JComboBox<String> cmbbxImages) {
			Images.SelectImageDialog dlg = new Images.SelectImageDialog(this,"Select image of "+id.getName(),id.getImageFileName());
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
			
			textarea.append("ID     : "+(id.id    ==null?"--------":id.id    )+"\r\n");
			textarea.append("Label  : "+(id.label ==null?"--------":id.label )+"\r\n");
			textarea.append("Symbol : "+(id.symbol==null?"--------":id.symbol)+"\r\n");
			textarea.append("Type   : "+(id.type  ==null?"--------":id.type.label)+"\r\n");
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
			id.upgradeClass = this.id.upgradeClass;
		}

//		public Integer getImageBG() { return id.getImageBG(); }
//		public String getImageFileName() { return id.getImageFileName(); }
//		public String getLabel() { return id.label; }
	}
	
}