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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.MutableComboBoxModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.table.TableCellEditor;

import net.schwarzbaer.gui.StandardDialog;
import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.ComboboxCellEditor;
import net.schwarzbaer.gui.Tables.NonStringRenderer;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID.UpgradeClass;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Images.CachedAlphaImages;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Images.ExtraImageList.ImageListListener;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Images.NamedColor;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.PolarCoordinates;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Stats.StatValue.KnownID;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.DiscoverableObject;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.DiscoverableObject.ExtraInfo;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Galaxy;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Planet;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Planet.Resources;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Region;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.SolarSystem;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.UniverseAddress;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.SaveGameView;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.DebugTableContextMenu;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.SimplifiedTable;

public class GameInfos {
	
	private static HashMap<Long,UniverseObjectData> universeObjectDataArr;
	private static HashMap<Integer,HashSet<String>> conflictLevelLabels;
	private static HashMap<Integer,HashSet<String>> economyLevelLabels;
	
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
		Universe.SolarSystem.SystemState systemState;
		Double distanceToCenter;
		int conflictLevel;
		int economyLevel;
		String conflictLevelLabel;
		String economyLevelLabel;
		Boolean hasAtlasInterface; 
		Boolean hasBlackHole;
		Long blackHoleTarget;
		boolean withRemembranceTerminal;
		Integer numberOfPlanets;
		
		public UOD_SolarSystem(UniverseAddress ua) {
			super(ua, Type.SolarSystem);
			race = null;
			systemState = Universe.SolarSystem.SystemState.Normal;
			starClass = null;
			distanceToCenter = null;
			conflictLevel = -1;
			conflictLevelLabel = null;
			economyLevel = -1;
			economyLevelLabel = null;
			hasAtlasInterface = null;
			hasBlackHole = null;
			blackHoleTarget = null;
			withRemembranceTerminal = false;
			numberOfPlanets = null;
		}
		public UOD_SolarSystem(SolarSystem sys) {
			super(sys.getUniverseAddress(), Type.SolarSystem, sys);
			race = sys.race;
			systemState = sys.systemState;
			starClass = sys.starClass;
			distanceToCenter = sys.distanceToCenter;
			conflictLevel = sys.conflictLevel;
			conflictLevelLabel = sys.conflictLevelLabel;
			economyLevel = sys.economyLevel;
			economyLevelLabel = sys.economyLevelLabel;
			hasAtlasInterface = sys.hasAtlasInterface;
			hasBlackHole = sys.hasBlackHole;
			blackHoleTarget = (sys.blackHoleTarget==null || !sys.hasBlackHole)?null:sys.blackHoleTarget.getAddress();
			withRemembranceTerminal = sys.withRemembranceTerminal;
			numberOfPlanets = sys.numberOfPlanets;
		}
	}
	
	private static class UOD_Planet extends UOD_UniverseObject {
		Universe.Planet.Biome biome;
		boolean hasExtremeBiome;
		boolean areSentinelsAggressive;
		boolean withWater;
		boolean withBigGeography;
		boolean withGravitinoBalls;
		boolean withRemembranceTerminal;
		Universe.Planet.BuriedTreasure buriedTreasure;
		EnumSet<Universe.Planet.Resources> resources;
		PolarCoordinates portalPos;
		
		public UOD_Planet(UniverseAddress ua) {
			super(ua, Type.Planet);
			biome = null;
			hasExtremeBiome = false;
			areSentinelsAggressive = false;
			withWater = false;
			withBigGeography = false;
			withGravitinoBalls = false;
			withRemembranceTerminal = false;
			buriedTreasure = null;
			resources = EnumSet.noneOf(Universe.Planet.Resources.class);
			portalPos = null;
		}
		public UOD_Planet(Planet planet) {
			super(planet.getUniverseAddress(),Type.Planet,planet);
			biome = planet.biome;
			hasExtremeBiome = planet.hasExtremeBiome;
			areSentinelsAggressive = planet.areSentinelsAggressive;
			withWater = planet.withWater;
			withBigGeography = planet.withBigGeography;
			withGravitinoBalls = planet.withGravitinoBalls;
			withRemembranceTerminal = planet.withRemembranceTerminal;
			buriedTreasure = planet.buriedTreasure;
			resources = EnumSet.copyOf(planet.resources);
			portalPos = planet.portalPos==null ? null : new PolarCoordinates(planet.portalPos);
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
		conflictLevelLabels = updateLevelLabels(system->system.conflictLevelLabel, system->system.conflictLevel);
	}
	public static void updateEconomyLevelLabels() {
		economyLevelLabels = updateLevelLabels(system->system.economyLevelLabel, system->system.economyLevel);
	}
	private static HashMap<Integer,HashSet<String>> updateLevelLabels(Function<UOD_SolarSystem,String> getLabel, Function<UOD_SolarSystem,Integer> getLevel) {
		HashMap<Integer,HashSet<String>> levelLabels = new HashMap<>();
		for (UniverseObjectData uoData:universeObjectDataArr.values()) {
			if (!(uoData instanceof UOD_SolarSystem)) continue;
			UOD_SolarSystem system = (UOD_SolarSystem)uoData;
			String levelLabel = getLabel.apply(system);
			if (levelLabel!=null) {
				int level = getLevel.apply(system);
				HashSet<String> labels = levelLabels.get(level);
				if (labels==null) levelLabels.put(level, labels = new HashSet<>());
				labels.add(levelLabel);
			}
		}
		return levelLabels;
	}
	
	public static String[] getConflictLevelLabels(int conflictLevel) {
		return getLevelLabels(conflictLevel, conflictLevelLabels);
	}
	public static String[] getEconomyLevelLabels(int economyLevel) {
		return getLevelLabels(economyLevel, economyLevelLabels);
	}
	private static String[] getLevelLabels(int level, HashMap<Integer,HashSet<String>> levelLabels) {
		HashSet<String> labels;
		if (level<1) {
			labels = new HashSet<>();
			for (HashSet<String> set:levelLabels.values())
				labels.addAll(set);
			
		} else
			labels = levelLabels.get(level);
		
		if (labels==null || labels.isEmpty()) return new String[0];
		String[] arr = labels.toArray(new String[0]);
		Arrays.sort(arr,Comparator.comparing(String::toLowerCase));
		return arr;
	}

	public static int getConflictLevel(String label) {
		return getLabeledLevel(label, conflictLevelLabels);
	}
	public static int getEconomyLevel(String label) {
		return getLabeledLevel(label, economyLevelLabels);
	}
	private static int getLabeledLevel(String label, HashMap<Integer,HashSet<String>> levelLabels) {
		for (Integer level:levelLabels.keySet()) {
			HashSet<String> labels = levelLabels.get(level);
			if (labels.contains(label)) return level;
		}
		return -1;
	}

	public static void parseExtraInfos() {
		EnumSet<Resources> resources = EnumSet.noneOf(Universe.Planet.Resources.class);
		universeObjectDataArr.forEach((address, uoData)->{
			
			if (uoData instanceof UOD_Planet) {
				UOD_Planet planet = (UOD_Planet) uoData;
				
				for (ExtraInfo exi:planet.extraInfos) {
					if (exi.info.startsWith("<ParsedPlanetaryResources>")) continue;
					if (exi.info.startsWith("<Parsed>")) continue;
					
					String shortLabel = exi.shortLabel.toLowerCase();
					if (shortLabel.equals("portal")) {
						String[] parts = exi.info.split(",");
						if (parts.length==2) {
							String latStr  = parts[0].trim(); if (latStr .startsWith("+")) latStr  = latStr .substring(1);
							String longStr = parts[1].trim(); if (longStr.startsWith("+")) longStr = longStr.substring(1);
							try {
								planet.portalPos = new PolarCoordinates(Double.parseDouble(latStr), Double.parseDouble(longStr), Double.NaN);
								exi.info = "<Parsed> "+exi.info;
								continue;
							}
							catch (NumberFormatException e) {}
						}
					}
					
					resources.clear();
					for (String str:exi.info.split(",")) {
						Resources res = Universe.Planet.Resources.getViaLabel(str.trim());
						if (res!=null) resources.add(res);
						else { resources.clear(); break; }
					}
					if (!resources.isEmpty()) {
						exi.info = "<ParsedPlanetaryResources> "+exi.info;
						planet.resources.addAll(resources);
					}				}
			}
			
			if (uoData instanceof UOD_SolarSystem) {
				UOD_SolarSystem system = (UOD_SolarSystem) uoData;
				
				for (ExtraInfo exi:system.extraInfos) {
					String shortLabel = exi.shortLabel.toLowerCase();
					if (shortLabel.startsWith("<parsed>")) continue;
					if (shortLabel.equals("1 planet")) {
						system.numberOfPlanets = 1;
						exi.shortLabel = "<Parsed> "+exi.shortLabel;
					} else if (shortLabel.endsWith(" planeten")) {
						String valueStr = shortLabel.substring(0, shortLabel.length()-" planeten".length()).trim();
						try {
							system.numberOfPlanets = Integer.parseInt(valueStr);
							exi.shortLabel = "<Parsed> "+exi.shortLabel;
						} catch (NumberFormatException e) {}
					}
				}
			}
		});
	}
	
	public static String getName(UniverseAddress ua) {
		if (ua==null) return null;
		UniverseObjectData uod = universeObjectDataArr.get(ua.getAddress());
		if (uod==null) return null;
		return uod.name;
	}

	private static String readValue(String str, String prefix) {
		return str.startsWith(prefix) ? str.substring(prefix.length()) : null;
	}
	
	private static Integer parseInt(String valueStr, Integer defaultValue) {
		return parseNumber(Integer::parseInt, valueStr, defaultValue);
	}
	
	@SuppressWarnings("unused")
	private static Integer parseInt(String valueStr, int radix, Integer defaultValue) {
		return parseNumber(t->Integer.parseInt(t,radix), valueStr, defaultValue);
	}
	
	@SuppressWarnings("unused")
	private static Long parseLong(String valueStr, Long defaultValue) {
		return parseNumber(Long::parseLong, valueStr, defaultValue);
	}
	
	private static Long parseLong(String valueStr, int radix, Long defaultValue) {
		return parseNumber(t->Long.parseLong(t,radix), valueStr, defaultValue);
	}
	
	private static Double parseDouble(String valueStr, Double defaultValue) {
		return parseNumber(Double::parseDouble, valueStr, defaultValue);
	}
	
	private static <A> A parseNumber(Function<String,A> convert, String valueStr, A defaultValue) {
		try { return convert.apply(valueStr); }
		catch (NumberFormatException e) { return defaultValue; }
	}
	
	private static <A extends Enum<A>> A valueOf(Function<String,A> convert, String valueStr, A defaultValue) {
		try { return convert.apply(valueStr); }
		catch (Exception e) { return defaultValue; }
	}

	public static void loadUniverseObjectDataFromFile() {
		long start = System.currentTimeMillis();
		Gui.log_ln("Read data of universe objects from file \""+FileExport.FILE_UNIVERSE_OBJECT_DATA+"\"...");
		universeObjectDataArr = new HashMap<>();
		
		File file = new File(FileExport.FILE_UNIVERSE_OBJECT_DATA);
		if (!file.isFile()) {
			Gui.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
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
			
			String line, valueStr;
			while ((line=in.readLine())!=null) {
				if (line.isEmpty()) continue;
				if ((line.startsWith("[Reg") || line.startsWith("[Sys") || line.startsWith("[Pln")) && line.endsWith("]")) {
					uoData = null;
					region = null;
					system = null;
					planet = null;
					uniObj = null;
					nextShortLabel = null;
					showInParent = null;
					
					String addressStr = line.substring("[Sys".length(), line.length()-"]".length());
					long address;
					try { address = Long.parseLong(addressStr, 16); }
					catch (NumberFormatException e) {
						System.err.printf("Can't parse universe address in: \"%s\"\r\n",line);
						continue;
					}
					UniverseAddress ua = new UniverseAddress(address);
					if (line.startsWith("[Reg") && ua.isRegion     ()) uoData =          region = new UOD_Region     (ua);
					if (line.startsWith("[Sys") && ua.isSolarSystem()) uoData = uniObj = system = new UOD_SolarSystem(ua);
					if (line.startsWith("[Pln") && ua.isPlanet     ()) uoData = uniObj = planet = new UOD_Planet     (ua);
					if (uoData != null) universeObjectDataArr.put(address, uoData);
					continue;
				}
				if (uoData!=null) {
					if ((valueStr = readValue(line,"name="))!=null) {
						uoData.name = valueStr;
						continue;
					}
					if ((valueStr = readValue(line,"oldname="))!=null) {
						uoData.oldname = valueStr;
						continue;
					}
				}
				if (system!=null) {
					if ((valueStr = readValue(line,"race="))!=null) {
						system.race = valueOf(Universe.SolarSystem.Race::valueOf,valueStr,null);
						continue;
					}
					if (line.equals("unexplored")) {
						system.systemState = Universe.SolarSystem.SystemState.Unexplored;
						continue;
					}
					if (line.equals("abandoned")) {
						system.systemState = Universe.SolarSystem.SystemState.Abandoned;
						continue;
					}
					if ((valueStr = readValue(line,"atlasinterface="))!=null) {
						system.hasAtlasInterface = valueStr.equalsIgnoreCase("true");
						continue;
					}
					if ((valueStr = readValue(line,"blackhole="))!=null) {
						system.hasBlackHole = valueStr.equalsIgnoreCase("true");
						continue;
					}
					if ((valueStr = readValue(line,"blackholetarget="))!=null) {
						system.blackHoleTarget = parseLong(valueStr, 16, null);
						continue;
					}
					if (line.equals("remembrance terminal")) {
						system.withRemembranceTerminal = true;
						continue;
					}
					if ((valueStr = readValue(line,"class="))!=null) {
						system.starClass = valueOf(Universe.SolarSystem.StarClass::valueOf,valueStr,null);
						continue;
					}
					if ((valueStr = readValue(line,"distance="))!=null) {
						system.distanceToCenter = parseDouble(valueStr,null);
						continue;
					}
					if ((valueStr = readValue(line,"planets="))!=null) {
						system.numberOfPlanets = parseInt(valueStr,null);
						continue;
					}
					if ((valueStr = readValue(line,"conflict="))!=null) {
						system.conflictLevel = parseInt(valueStr,-1);
						continue;
					}
					if ((valueStr = readValue(line,"conflict_label="))!=null) {
						system.conflictLevelLabel = valueStr;
						continue;
					}
					if ((valueStr = readValue(line,"economy="))!=null) {
						system.economyLevel = parseInt(valueStr,-1);
						continue;
					}
					if ((valueStr = readValue(line,"economy_label="))!=null) {
						system.economyLevelLabel = valueStr;
						continue;
					}
				}
				if (planet!=null) {
					if ((valueStr = readValue(line,"biome="))!=null) {
						planet.biome = valueOf(Universe.Planet.Biome::valueOf,valueStr,null);
						continue;
					}
					if (line.equals("is extreme")) {
						planet.hasExtremeBiome = true;
						continue;
					}
					if (line.equals("aggrSentinels") || line.startsWith("aggrSentinels=") || line.equals("sentinel=Aggressive")) {
						planet.areSentinelsAggressive = true;
						continue;
					}
					if (line.equals("with water")) {
						planet.withWater = true;
						continue;
					}
					if (line.equals("big geography")) {
						planet.withBigGeography = true;
						continue;
					}
					if (line.equals("gravitino balls")) {
						planet.withGravitinoBalls = true;
						continue;
					}
					if (line.equals("remembrance terminal")) {
						planet.withRemembranceTerminal = true;
						continue;
					}
					if ((valueStr = readValue(line,"buriedTreasure="))!=null) {
						planet.buriedTreasure = valueOf(Universe.Planet.BuriedTreasure::valueOf,valueStr,null);
						continue;
					}
					if ((valueStr = readValue(line,"resources="))!=null) {
						planet.resources.clear();
						for (String resStr:valueStr.split(",")) {
							try { planet.resources.add(Universe.Planet.Resources.valueOf(resStr)); }
							catch (Exception e) {}
						}
						continue;
					}
					if ((valueStr = readValue(line,"portal="))!=null) {
						planet.portalPos = PolarCoordinates.parseValueStr(valueStr);
						continue;
					}
				}
				if (uniObj!=null) {
					if ((valueStr = readValue(line,"short="))!=null) {
						nextShortLabel = valueStr;
						showInParent = false;
						continue;
					}
					if ((valueStr = readValue(line,"short.P="))!=null) {
						nextShortLabel = valueStr;
						showInParent = true;
						continue;
					}
					if ((valueStr = readValue(line,"info="))!=null) {
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
		updateEconomyLevelLabels();
		parseExtraInfos();
		
		Gui.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}

	public static void readUniverseObjectDataFromDataPool(Universe universe, boolean forceCreation) {
		long start = System.currentTimeMillis();
		Gui.log_ln("Read data of universe objects from data pool ... ");
		
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
			case Region     :              uod_region = (UOD_Region     )uoData; if (ua.isRegion     ())          region = forceCreation ? universe.getOrCreateRegion     (ua,obj->{}) : universe.findRegion     (ua); break;
			case SolarSystem: uod_uniObj = uod_system = (UOD_SolarSystem)uoData; if (ua.isSolarSystem()) uniObj = system = forceCreation ? universe.getOrCreateSolarSystem(ua,obj->{}) : universe.findSolarSystem(ua); break;
			case Planet     : uod_uniObj = uod_planet = (UOD_Planet     )uoData; if (ua.isPlanet     ()) uniObj = planet = forceCreation ? universe.getOrCreatePlanet     (ua,obj->{}) : universe.findPlanet     (ua); break;
			}
			if (region!=null) { objName = "region "+ua.getCoordinates_Region();   if (withOutput) Gui.log_ln("Region %s"      ,ua.getCoordinates_Region  ()); }
			if (system!=null) { objName = "solar system "+ua.getSigBoostCode();   if (withOutput) Gui.log_ln("Solar system %s",ua.getSigBoostCode        ()); }
			if (planet!=null) { objName = "planet "+ua.getExtendedSigBoostCode(); if (withOutput) Gui.log_ln("Planet %s"      ,ua.getExtendedSigBoostCode()); }
			
			if (uoData.name!=null) {
				if (uniObj!=null) uniObj.setOriginalName(uoData.name);
				if (region!=null) region.setName(uoData.name);
				if (withOutput && objName!=null) Gui.log_ln("   Name of %s was defined: \"%s\"",objName,uoData.name);
			}
			
			if (uoData.oldname!=null) {
				if (uniObj!=null) uniObj.setOldOriginalName(uoData.oldname);
				if (region!=null) region.setOldName(uoData.oldname);
				if (withOutput && objName!=null) Gui.log_ln("   Old Name of %s was defined: \"%s\"",objName,uoData.oldname);
			}
			
			if (system!=null) {
				if (uod_system.race!=null) {
					system.race = uod_system.race;
					if (withOutput) Gui.log_ln("   Race of %s was defined: %s", objName, system.race);
				}
				if (uod_system.starClass!=null) {
					system.starClass = uod_system.starClass;
					if (withOutput) Gui.log_ln("   Star Class of %s was defined: %s", objName, system.starClass);
				}
				if (uod_system.distanceToCenter!=null) {
					system.distanceToCenter = uod_system.distanceToCenter;
					if (withOutput) Gui.log_ln("   Distance to galactic center of %s was defined: %s", objName, system.distanceToCenter);
				}
				if (uod_system.conflictLevel>=0) {
					system.conflictLevel = uod_system.conflictLevel;
					if (withOutput) Gui.log_ln("   Conflict Level of %s was defined: %d", objName, system.conflictLevel);
				}
				if (uod_system.conflictLevelLabel!=null) {
					system.conflictLevelLabel = uod_system.conflictLevelLabel;
					if (withOutput) Gui.log_ln("   Conflict Level Label of %s was defined: \"%s\"", objName, system.conflictLevelLabel);
				}
				if (uod_system.economyLevel>=0) {
					system.economyLevel = uod_system.economyLevel;
					if (withOutput) Gui.log_ln("   Economy Level of %s was defined: %d", objName, system.economyLevel);
				}
				if (uod_system.economyLevelLabel!=null) {
					system.economyLevelLabel = uod_system.economyLevelLabel;
					if (withOutput) Gui.log_ln("   Economy Level Label of %s was defined: \"%s\"", objName, system.economyLevelLabel);
				}
				if (uod_system.systemState!=null) {
					system.systemState = uod_system.systemState;
					if (withOutput) Gui.log_ln("   %s was defined as unexplored", objName);
				}
				if (uod_system.hasAtlasInterface!=null) {
					system.hasAtlasInterface = uod_system.hasAtlasInterface;
					if (withOutput) Gui.log_ln("   %s has an Atlas Interface: %s", objName, system.hasAtlasInterface);
				}
				if (uod_system.hasBlackHole!=null) {
					system.hasBlackHole = uod_system.hasBlackHole;
					if (withOutput) Gui.log_ln("   %s has a Black Hole: %s", objName, system.hasBlackHole);
				}
				if (uod_system.blackHoleTarget!=null) {
					system.blackHoleTarget = new UniverseAddress(uod_system.blackHoleTarget);
					if (withOutput) Gui.log_ln("   %s has a Black Hole Target: %s", objName, system.blackHoleTarget);
				}
				if (uod_system.withRemembranceTerminal) {
					system.withRemembranceTerminal = uod_system.withRemembranceTerminal;
					if (withOutput) Gui.log_ln("   %s has a Remembrance Terminal", objName);
				}
				if (uod_system.numberOfPlanets!=null) {
					system.numberOfPlanets = uod_system.numberOfPlanets;
					if (withOutput) Gui.log_ln("   %s has %d planets", objName, system.numberOfPlanets);
				}
			}
			
			if (planet!=null) {
				if (uod_planet.biome!=null) {
					planet.biome = uod_planet.biome;
					if (withOutput) Gui.log_ln("   Biome of %s was defined: %s",objName,planet.biome);
				}
				if (uod_planet.hasExtremeBiome) {
					planet.hasExtremeBiome = uod_planet.hasExtremeBiome;
					if (withOutput) Gui.log_ln("   %s has extreme biome",objName);
				}
				if (uod_planet.areSentinelsAggressive) {
					planet.areSentinelsAggressive = uod_planet.areSentinelsAggressive;
					if (withOutput) Gui.log_ln("   Sentinels of %s are aggressive",objName);
				}
				if (uod_planet.withWater) {
					planet.withWater = uod_planet.withWater;
					if (withOutput) Gui.log_ln("   %s has water",objName);
				}
				if (uod_planet.withBigGeography) {
					planet.withBigGeography = uod_planet.withBigGeography;
					if (withOutput) Gui.log_ln("   %s has big geography",objName);
				}
				if (uod_planet.withGravitinoBalls) {
					planet.withGravitinoBalls = uod_planet.withGravitinoBalls;
					if (withOutput) Gui.log_ln("   %s has Gravitino Balls",objName);
				}
				if (uod_planet.withRemembranceTerminal) {
					planet.withRemembranceTerminal = uod_planet.withRemembranceTerminal;
					if (withOutput) Gui.log_ln("   %s has Remembrance Terminal",objName);
				}
				if (uod_planet.buriedTreasure!=null) {
					planet.buriedTreasure = uod_planet.buriedTreasure;
					if (withOutput) Gui.log_ln("   Buried Treasure of %s was defined: %s",objName,planet.buriedTreasure);
				}
				if (uod_planet.portalPos!=null) {
					planet.portalPos = new PolarCoordinates(uod_planet.portalPos);
					if (withOutput) Gui.log_ln("   Portal Position of %s was defined: %s",objName,planet.portalPos.toString());
				}
				if (!uod_planet.resources.isEmpty()) {
					planet.resources.clear();
					planet.resources.addAll(uod_planet.resources);
					if (withOutput) Gui.log_ln("   Resources of %s were defined: %s",objName,planet.resources);
				}
			}
			
			if (uniObj!=null) {
				uniObj.extraInfos.clear();
				for (ExtraInfo ei:uod_uniObj.extraInfos) {
					uniObj.extraInfos.add(ei);
					if (withOutput) Gui.log_ln("   Info of %s was defined: ( \"%s\", \"%s\" )",objName,ei.shortLabel,ei.info);
				}
			}
			
		}
		if (!withOutput) Gui.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}

	public static void saveUniverseObjectDataToFile(Universe universe) {
		long start = System.currentTimeMillis();
		Gui.log("Write data of universe objects to data pool ...");
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
		Gui.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
		
		start = System.currentTimeMillis();
		Vector<UniverseObjectData> values = new Vector<>(universeObjectDataArr.values());
		Collections.sort(values);
		
		Gui.log("Write data pool to file \""+FileExport.FILE_UNIVERSE_OBJECT_DATA+"\" ...");
		File file = new File(FileExport.FILE_UNIVERSE_OBJECT_DATA);
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
					if (uod_system.systemState == Universe.SolarSystem.SystemState.Unexplored)
						out.printf("unexplored\r\n");
					else {
						if (uod_system.race              !=null) out.printf("race=%s\r\n",uod_system.race);
						if (uod_system.economyLevel      >=0   ) out.printf("economy=%d\r\n",uod_system.economyLevel);
						if (uod_system.economyLevelLabel !=null) out.printf("economy_label=%s\r\n",uod_system.economyLevelLabel);
						if (uod_system.systemState == Universe.SolarSystem.SystemState.Abandoned)
							out.printf("abandoned\r\n");
						else {
							if (uod_system.conflictLevel     >=0   ) out.printf("conflict=%d\r\n",uod_system.conflictLevel);
							if (uod_system.conflictLevelLabel!=null) out.printf("conflict_label=%s\r\n",uod_system.conflictLevelLabel);
						}
					}
					if (uod_system.hasAtlasInterface!=null) {
						//if (uod_system.hasAtlasInterface || SolarSystem.shouldHaveAtlasInterface(uod_system.universeAddress.solarSystemIndex))
							out.printf("atlasinterface=%s\r\n",uod_system.hasAtlasInterface);
					}
					if (uod_system.hasBlackHole!=null) {
						//if (uod_system.hasBlackHole || SolarSystem.shouldHaveBlackHole(uod_system.universeAddress.solarSystemIndex))
							out.printf("blackhole=%s\r\n",uod_system.hasBlackHole);
					}
					if (uod_system.blackHoleTarget  !=null) out.printf("blackholetarget=%014X%n",uod_system.blackHoleTarget);
					if (uod_system.withRemembranceTerminal) out.printf("remembrance terminal%n");
					if (uod_system.starClass        !=null) out.printf("class=%s%n",uod_system.starClass);
					if (uod_system.distanceToCenter !=null) out.printf(Locale.ENGLISH,"distance=%f%n",uod_system.distanceToCenter.doubleValue());
					if (uod_system.numberOfPlanets  !=null) out.printf("planets=%d%n",uod_system.numberOfPlanets.intValue());
				}
				
				if (uod_planet!=null) {
					if ( uod_planet.biome            !=null) out.printf("biome=%s%n",uod_planet.biome);
					if ( uod_planet.hasExtremeBiome        ) out.printf("is extreme%n");
					if ( uod_planet.areSentinelsAggressive ) out.printf("aggrSentinels%n");
					if ( uod_planet.withWater              ) out.printf("with water%n");
					if ( uod_planet.withBigGeography       ) out.printf("big geography%n");
					if ( uod_planet.withGravitinoBalls     ) out.printf("gravitino balls%n");
					if ( uod_planet.withRemembranceTerminal) out.printf("remembrance terminal%n");
					if ( uod_planet.buriedTreasure   !=null) out.printf("buriedTreasure=%s%n",uod_planet.buriedTreasure);
					if (!uod_planet.resources.isEmpty()    ) out.printf("resources=%s%n",String.join(",", Universe.Planet.Resources.getStringIterable(uod_planet.resources)));
					if ( uod_planet.portalPos        !=null && uod_planet.portalPos.isValueOk()) out.printf("portal=%s%n",uod_planet.portalPos.toValueStr());
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
		Gui.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}

	public static void loadKnownStatIDsFromFile() {
		File file = new File(FileExport.FILE_KNOWN_STAT_ID);
		if (!file.isFile()) return;
		
		boolean somethingChanged = false;
		long start = System.currentTimeMillis();
		Gui.log_ln("Read known StatIDs from file \"%s\" ...", file.getPath());
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
					Gui.log_warn_ln("   changed fullName of %16s  from \"%s\"  into \"%s\"",knownID,knownID.fullName,fullName);
					knownID.fullName = fullName;
					somethingChanged = true;
				}
			}
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
		
		if (!somethingChanged) Gui.log_warn_ln("   All values from file are already known. File \"%s\" can be deleted.", file.getPath());
		Gui.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}

	public static void saveKnownStatIDsToFile() {
		File file = new File(FileExport.FILE_KNOWN_STAT_ID);
		long start = System.currentTimeMillis();
		Gui.log_ln("Write known StatIDs to file \""+file.getPath()+"\" ...");
		
		KnownID[] knownIDs = KnownID.values();
		
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8));) {
			for (KnownID id:knownIDs)
				out.printf("%s=%s\r\n",id.toString(),id.fullName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		Gui.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}
	
	public static final IDMap productIDs   = new IDMap("ProductIDs");
	public static final IDMap techIDs      = new IDMap("TechIDs");
	public static final IDMap substanceIDs = new IDMap("SubstanceIDs");
	
	public static void removeUsages(SaveGameData oldData) {
		for (GeneralizedID id:productIDs  .getValues()) id.usage.remove(oldData);
		for (GeneralizedID id:techIDs     .getValues()) id.usage.remove(oldData);
		for (GeneralizedID id:substanceIDs.getValues()) id.usage.remove(oldData);
	}
	
	public static GeneralizedID getGeneralizedID(String id) {
		GeneralizedID generalizedID = null;
		if (generalizedID==null) generalizedID = productIDs  .getIfContains(id);
		if (generalizedID==null) generalizedID = techIDs     .getIfContains(id);
		if (generalizedID==null) generalizedID = substanceIDs.getIfContains(id);
		return generalizedID;
	}

	public static class IDMap {
		private final HashMap<String, GeneralizedID> map;
		private final String label;
		private final TemplateList templateList;
		
		public IDMap(String label) {
			this.label = label;
			map = new HashMap<>();
			templateList = new TemplateList();
		}
		
		public TemplateList getTemplateList() {
			return templateList;
		}

		public String getLabel() {
			return label;
		}
		
		public void forEach(Consumer<GeneralizedID> action) {
			map.values().forEach(action);
		}
		
		public Iterable<GeneralizedID> getValues() {
			return () -> map.values().iterator(); 
		}
	
		public Vector<GeneralizedID> getSortedValues() {
			Vector<GeneralizedID> vector = new Vector<GeneralizedID>(map.values());
			vector.sort(Comparator.comparing(id->id.id));
			return vector;
		}
	
		public Iterable<String> getSortedKeys() {
			Vector<String> vector = new Vector<String>(map.keySet());
			vector.sort(null);
			return () -> vector.iterator(); 
		}
	
//		public void set(String id, GeneralizedID generalizedID) {
//			map.put(id, generalizedID);
//		}
		
		public GeneralizedID get(String id, SaveGameData source, GeneralizedID.Usage.Type usageType) {
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
		
		public GeneralizedID getIfContains(String id) {
			return map.get(id);
		}

		public void remove(GeneralizedID id) {
			map.remove(id.id);
		}
	}

	public static void loadAllIDsFromFiles() {
		loadIDsFromFile(FileExport.FILE_PRODUCT_ID  ,productIDs  ,"product"   );
		loadIDsFromFile(FileExport.FILE_TECH_ID     ,techIDs     ,"technology");
		loadIDsFromFile(FileExport.FILE_SUBSTANCE_ID,substanceIDs,"substance" );
	}
	
	private enum IDFileBlock { Templates, IDs }
	private static void loadIDsFromFile(String filePath, IDMap map, String idLabel) {
		File file = new File(filePath);
		if (!file.isFile()) return;
		
		long start = System.currentTimeMillis();
		Gui.log_ln("Read "+idLabel+" IDs from file \""+filePath+"\"...");
		String line;
		IDFileBlock currentBlock = IDFileBlock.IDs;
		GeneralizedIDTemplate template = null;
		
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8))) {
			while ((line=in.readLine())!=null) {
				
				switch (line) {
				case "[Templates]": currentBlock = IDFileBlock.Templates; break;
				case "[IDs]"      : currentBlock = IDFileBlock.IDs; break;
				}
				
				switch (currentBlock) {
				
				case Templates: {
					if (line.isEmpty()) {
						if (template!=null) map.templateList.add(template);
						template = null;
					}
					if (line.startsWith("MinValues=")) {
						if (template!=null) map.templateList.add(template);
						String str = line.substring("MinValues=".length());
						try {
							int minValues = Integer.parseInt(str);
							template = new GeneralizedIDTemplate(minValues);
						} catch (NumberFormatException e) {
							Gui.log_error_ln("Can't parse Templates.MinValues as integer in \"%s\"", str);
							template = null;
						}
					}
					if (line.startsWith("Label=") && template!=null) {
						String str = line.substring("Label=".length());
						template.label = str;
					}
					if (line.startsWith("Symbol=") && template!=null) {
						String str = line.substring("Symbol=".length());
						template.symbol = str;
					}
					if (line.startsWith("Type=") && template!=null) {
						String str = line.substring("Type=".length());
						try { template.type = GeneralizedID.Type.valueOf(str); }
						catch (Exception e) {
							Gui.log_error_ln("Can't parse Templates.Type as GeneralizedID.Type in \"%s\"", str);
							template.type = null;
						}
					}
					if (line.startsWith("UpgradeClass=") && template!=null) {
						String str = line.substring("UpgradeClass=".length());
						try { template.upgradeClass = GeneralizedID.UpgradeClass.valueOf(str); }
						catch (Exception e) {
							Gui.log_error_ln("Can't parse Templates.UpgradeClass as GeneralizedID.UpgradeClass in \"%s\"", str);
							template.upgradeClass = null;
						}
					}
					if (line.startsWith("Image=") && template!=null) {
						String str = line.substring("Image=".length());
						template.imageFileName = str;
					}
					if (line.startsWith("Background=") && template!=null) {
						String str = line.substring("Background=".length());
						try { template.imageBackground = Integer.parseInt(str, 16); }
						catch (NumberFormatException e) {
							Gui.log_error_ln("Can't parse Templates.Background as hex integer in \"%s\"", str);
							template.imageBackground = null;
						}
					}
				} break;
					
				case IDs: {
					int pos = line.indexOf('=');
					if (pos<0) continue;
					
					String idStr = line.substring(0, pos);
					String value = line.substring(pos+1);
					
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
				} break;
				}
			}
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
		
		Gui.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
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
	public static void saveProductIDsToFile  () { saveIDsToFile(FileExport.FILE_PRODUCT_ID  ,productIDs  ,"product"    ); }
	public static void saveTechIDsToFile     () { saveIDsToFile(FileExport.FILE_TECH_ID     ,techIDs     ,"technologie"); }
	public static void saveSubstanceIDsToFile() { saveIDsToFile(FileExport.FILE_SUBSTANCE_ID,substanceIDs,"substance"  ); }

	public static void saveIDsToFile(String filePath, IDMap map, String idLabel) {
		long start = System.currentTimeMillis();
		Gui.log_ln("Write "+idLabel+" IDs to file \""+filePath+"\"...");
		
		File file = new File(filePath);
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8));) {
			if (!map.templateList.isEmpty()) {
				out.println("[Templates]");
				map.templateList.forEachSorted(template->{
					out.printf("MinValues=%d%n", template.minValues);
					if (template.label          !=null) out.printf("Label=%s%n"       , template.label          );
					if (template.symbol         !=null) out.printf("Symbol=%s%n"      , template.symbol         );
					if (template.type           !=null) out.printf("Type=%s%n"        , template.type           );
					if (template.upgradeClass   !=null) out.printf("UpgradeClass=%s%n", template.upgradeClass   );
					if (template.imageFileName  !=null) out.printf("Image=%s%n"       , template.imageFileName  );
					if (template.imageBackground!=null) out.printf("Background=%06X%n", template.imageBackground);
					out.println();
				});
			}
			
			out.println("[IDs]");
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
		
		Gui.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}
	
	private static class CreateTemplateDialog extends StandardDialog {
		private static final long serialVersionUID = -3032594452167487654L;
		
		private int minValues;
		private boolean useLabel;
		private boolean useSymbol;
		private boolean useType;
		private boolean useUpgradeClass;
		private boolean useImageFileName;
		private boolean useImageBackground;
		private JButton btnOk;
		
		private GeneralizedIDTemplate result;

		public CreateTemplateDialog(Window parent, String title, GeneralizedID id, TemplateList list) {
			super(parent, title);
			result = null;
			minValues = 2;
			
			JPanel contentPane = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.gridx = 0;
			c.gridwidth = 2;
			boolean en;
			c.gridy = 0; en = useLabel          =(null!=id.label          ); contentPane.add(Gui.createCheckbox("use Label"       , en, en, b->{ useLabel          =b; check(id,list); }),c);
			c.gridy = 1; en = useSymbol         =(null!=id.symbol         ); contentPane.add(Gui.createCheckbox("use Symbol"      , en, en, b->{ useSymbol         =b; check(id,list); }),c);
			c.gridy = 2; en = useType           =(null!=id.type           ); contentPane.add(Gui.createCheckbox("use Type"        , en, en, b->{ useType           =b; check(id,list); }),c);
			c.gridy = 3; en = useUpgradeClass   =(null!=id.upgradeClass   ); contentPane.add(Gui.createCheckbox("use UpgradeClass", en, en, b->{ useUpgradeClass   =b; check(id,list); }),c);
			c.gridy = 4; en = useImageFileName  =(null!=id.imageFileName  ); contentPane.add(Gui.createCheckbox("use Image"       , en, en, b->{ useImageFileName  =b; check(id,list); }),c);
			c.gridy = 5; en = useImageBackground=(null!=id.imageBackground); contentPane.add(Gui.createCheckbox("use Background"  , en, en, b->{ useImageBackground=b; check(id,list); }),c);
			c.gridwidth = 1;
			c.gridy = 6;
			contentPane.add(new JLabel("Min. Number of Values: "),c);
			c.gridx = 1;
			contentPane.add(Gui.createTextField(Integer.toString(minValues), 5, (String str)->{
				try { minValues = Integer.parseInt(str); }
				catch (NumberFormatException e1) {}
				return Integer.toString(minValues);
			}),c);
			
			createGUI(contentPane,
				btnOk = Gui.createButton("Ok", e->{ result = createTemplate(id); closeDialog(); }),
				Gui.createButton("Cancel", e->{ closeDialog(); })
			);
		}

		public GeneralizedIDTemplate getResult() {
			return result;
		}

		private GeneralizedIDTemplate createTemplate(GeneralizedID id) {
			return new GeneralizedIDTemplate(
				minValues,
				!useLabel           ? null : id.label,
				!useSymbol          ? null : id.symbol,
				!useType            ? null : id.type,
				!useUpgradeClass    ? null : id.upgradeClass,
				!useImageFileName   ? null : id.imageFileName,
				!useImageBackground ? null : id.imageBackground
			);
		}

		private void check(GeneralizedID id, TemplateList list) {
			btnOk.setEnabled(!list.contains(createTemplate(id)));
		}
		
	}
	
	private static class TemplateList {
		private final Vector<GeneralizedIDTemplate> list;
		
		TemplateList() {
			list = new Vector<>();
		}
		
		public boolean isEmpty() {
			return list.isEmpty();
		}
		
		@SuppressWarnings("unused")
		public void forEach(Consumer<? super GeneralizedIDTemplate> action) {
			list.forEach(action);
		}
		public void forEachSorted(Consumer<? super GeneralizedIDTemplate> action) {
			Vector<GeneralizedIDTemplate> vec = new Vector<>(list);
			vec.sort(null);
			vec.forEach(action);
		}

		boolean contains(GeneralizedIDTemplate newTemplate) {
			for (GeneralizedIDTemplate template:list) {
				if (template.equals(newTemplate))
					return true;
			}
			return false;
		}
		
		boolean add(GeneralizedIDTemplate newTemplate) {
			if (contains(newTemplate)) return false;
			list.add(newTemplate);
			return true;
		}
		
		GeneralizedIDTemplate get(GeneralizedID id) {
			GeneralizedIDTemplate result = null;
			for (GeneralizedIDTemplate template:list) {
				if (template.fitsTo(id)) {
					if (result==null) result = template; // 1st hit is ok
					else return null; // 2nd hit is not
				}
			}
			return result;
		}
	}

	private static class GeneralizedIDProto {
		public String label;
		public String symbol;
		public GeneralizedID.Type type;
		public GeneralizedID.UpgradeClass upgradeClass;
		protected String imageFileName;
		protected Integer imageBackground;
		
		protected GeneralizedIDProto(String label, String symbol, GeneralizedID.Type type, UpgradeClass upgradeClass, String imageFileName, Integer imageBackground) {
			this.label = label;
			this.symbol = symbol;
			this.type = type;
			this.upgradeClass = upgradeClass;
			this.imageFileName = imageFileName;
			this.imageBackground = imageBackground;
		}
		protected GeneralizedIDProto(GeneralizedIDProto id) {
			this(id.label, id.symbol, id.type, id.upgradeClass, id.imageFileName, id.imageBackground);
		}
		
		boolean equals(GeneralizedIDProto other) {
			if (!equals(this.label          , other.label          )) return false;
			if (!equals(this.symbol         , other.symbol         )) return false;
			if (!equals(this.type           , other.type           )) return false;
			if (!equals(this.upgradeClass   , other.upgradeClass   )) return false;
			if (!equals(this.imageFileName  , other.imageFileName  )) return false;
			if (!equals(this.imageBackground, other.imageBackground)) return false;
			return true;
		}
		
		<V> boolean equals(V v1, V v2) {
			if (v1==null && v2==null) return true;
			if (v1==null || v2==null) return false;
			return v1.equals(v2);
		}
		protected int compareTo(GeneralizedIDProto other) {
			int val = 0;
			if (val==0 && this.label          !=null) val = label          .compareTo(other.label          );
			if (val==0 && this.symbol         !=null) val = symbol         .compareTo(other.symbol         );
			if (val==0 && this.type           !=null) val = type           .compareTo(other.type           );
			if (val==0 && this.upgradeClass   !=null) val = upgradeClass   .compareTo(other.upgradeClass   );
			if (val==0 && this.imageFileName  !=null) val = imageFileName  .compareTo(other.imageFileName  );
			if (val==0 && this.imageBackground!=null) val = imageBackground.compareTo(other.imageBackground);
			return val;
		}
	}

	private static class GeneralizedIDTemplate extends GeneralizedIDProto implements Comparable<GeneralizedIDTemplate> {
		int minValues;
		
		public GeneralizedIDTemplate(int minValues) {
			this(minValues,null,null,null,null,null,null);
		}
		public GeneralizedIDTemplate(int minValues,
				String label, String symbol, GeneralizedID.Type type,
				GeneralizedID.UpgradeClass upgradeClass, String imageFileName, Integer imageBackground) {
			super(label, symbol, type, upgradeClass, imageFileName, imageBackground);
			this.minValues = minValues;
		}

		public GeneralizedIDTemplate(GeneralizedID id, int minValues) {
			super(id);
			this.minValues = minValues;
		}

		boolean fitsTo(GeneralizedID id) {
			int nValues = 0;
			if (id.label          !=null) { if (id.label          .equals(label          )) ++nValues; else return false; }
			if (id.symbol         !=null) { if (id.symbol         .equals(symbol         )) ++nValues; else return false; }
			if (id.type           !=null) { if (id.type           .equals(type           )) ++nValues; else return false; }
			if (id.upgradeClass   !=null) { if (id.upgradeClass   .equals(upgradeClass   )) ++nValues; else return false; }
			if (id.imageFileName  !=null) { if (id.imageFileName  .equals(imageFileName  )) ++nValues; else return false; }
			if (id.imageBackground!=null) { if (id.imageBackground.equals(imageBackground)) ++nValues; else return false; }
			return nValues>=minValues;
		}
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("min="+minValues);
			if (label          !=null) sb.append(", Lab="  ).append(label          );
			if (symbol         !=null) sb.append(", Sym="  ).append(symbol         );
			if (type           !=null) sb.append(", Type=" ).append(type           );
			if (upgradeClass   !=null) sb.append(", Upgr=" ).append(upgradeClass   );
			if (imageFileName  !=null) sb.append(", Image=").append(imageFileName  );
			if (imageBackground!=null) sb.append(", BG="   ).append(imageBackground);
			return "[ "+sb.toString()+" ]";
		}
		@Override
		public int compareTo(GeneralizedIDTemplate other) {
			int val = super.compareTo(other);
			if (val==0) val = this.minValues-other.minValues;
			return val;
		}
	}

	public static class GeneralizedID extends GeneralizedIDProto {
		
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
			Bait                      ("Köder"),
			Ingredient                ("Zutat"),
			Treasure                  ("Schatz"),
			PlanetTrophy              ("Planeten-Trophäe"),
			;
			
			public final String label;
			public final boolean isUpgradeModule;
			
			Type(String label) { this(label,false); }
			Type(String label, boolean isUpgradeModule) { this.label = label; this.isUpgradeModule = isUpgradeModule; }
			
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
		final HashMap<SaveGameData,Usage> usage;
		private BufferedImage cachedImage;
		
		private GeneralizedID(String id, String label) {
			super(label,null,null,null,null,null);
			this.id = id;
			this.isObsolete = false;
			this.usage = new HashMap<>();
			this.cachedImage = null;
		}
		public GeneralizedID(String id) {
			this(id,"");
		}
		public GeneralizedID(GeneralizedID other) {
			super(other);
			this.id = other.id;
			this.isObsolete = other.isObsolete;
			this.usage = new HashMap<SaveGameData,Usage>(other.usage);
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
		@Override public String toString() { return getName(); }
		
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
		
		public String getImageBGLabel() { return imageBackground==null ? "<no color>" : Images.getInstance().colors.getColorLabel(imageBackground); }
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
			return Images.getInstance().extraImages.getImage(imageFileName,imageBackground,width,height);
		}
		
		public Usage getUsage(SaveGameData source) {
			Usage newUsage = new Usage();
			Usage oldUsage = usage.putIfAbsent(source, newUsage);
			return oldUsage==null?newUsage:oldUsage;
		}
	
		public static class Usage {
			public enum Type {
				BuildingObject("Bo"), Blueprint("Bl"), QuicksilverSpecial("QS"), InventorySlot("In");
				
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
	
		private SimplifiedTable<GeneralizedIDColumnID> table;
		private GeneralizedIDTableModel tableModel;
		private GeneralizedID clickedID;
		private Point clickedCell;
		private GeneralizedID selectedID;
	
		private JTextArea textarea;
		private JLabel imageField;

		public GeneralizedIDPanel(Window mainwindow, IDMap idMap, String tableLabel) {
			super(new BorderLayout(3, 3));
			setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			
			this.mainwindow = mainwindow;
			
			clickedID = null;
			clickedCell = null;
			selectedID = null;
			tableModel = new GeneralizedIDTableModel(this,idMap);
			table = new SimplifiedTable<>(tableLabel,tableModel,true,false,true);
			table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION, true);
			table.getSelectionModel().addListSelectionListener(e->{
				selectedID = null;
				if (table.getSelectedRowCount()==1)
					selectedID = tableModel.getValue(table.convertRowIndexToModel(table.getSelectedRow()));
				showID(selectedID);
			});
			prepareTable();
			
			GeneralizedID.Type[] types = SaveViewer.addNull(GeneralizedID.Type.values());
			Gui.ListMenuItems.ExternFunction<GeneralizedID.Type> setType = new Gui.ListMenuItems.ExternFunction<GeneralizedID.Type>() {
				@Override public void setResult(GeneralizedID.Type value) {
					updateAfterContextMenuAction(setType(value),null);
				}
				@Override public void configureMenuItem(JMenuItem menuItem, GeneralizedID.Type value) {
					menuItem.setText(value==null?"<none>":value.label);
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
			
			NamedColor[] colors = SaveViewer.addNull(Images.getInstance().colors.getArray());
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
			Images.getInstance().colors.addColorListListender(new Images.Colors.ColorListListender() {
				@Override public void colorAdded   (NamedColor color)           { updateColors(); }
				@Override public void colorChanged (NamedColor color)           { updateColors(); }
				@Override public void orderChanged ()                           { updateColors(); }
				@Override public void colorsRemoved(Vector<NamedColor> removed) { updateColors(); }
				private void updateColors() {
					NamedColor[] colors = SaveViewer.addNull(Images.getInstance().colors.getArray());
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
			contextMenuGroup.add(Gui.createMenuItem("Delete selected",this,ActionCommand.DeleteID,Gui.ToolbarIcons.Delete));
			contextMenuGroup.addSeparator();
			contextMenuGroup.add(typeListMenu_Group);
			contextMenuGroup.add(colorListMenu_Group);
			contextMenuGroup.add(Gui.createMenuItem("ImageFile of selected ...",this,ActionCommand.SelectImage4AllSelected));
			contextMenuGroup.add(upgrclsListMenu_Group);
			contextMenuGroup.addSeparator();
			contextMenuGroup.add(Gui.createMenuItem("Clear ImageFile of selected",this,ActionCommand.ClearImage,Gui.ToolbarIcons.Delete));
			contextMenuGroup.add(Gui.createMenuItem("Paste ImageFile of selected",this,ActionCommand.PasteImage,Gui.ToolbarIcons.Paste));
			contextMenuGroup.addSeparator();
			contextMenuGroup.add(Gui.createMenuItem("Clear Background of selected",this,ActionCommand.ClearBackground,Gui.ToolbarIcons.Delete));
			contextMenuGroup.add(Gui.createMenuItem("Paste Background of selected",this,ActionCommand.PasteBackground,Gui.ToolbarIcons.Paste));
			contextMenuGroup.addSeparator();
			contextMenuGroup.add(Gui.createMenuItem("Add Background Color",this,ActionCommand.AddBackgroundColor));
			
			contextMenuStd.addSeparator();
			addStandardItems(contextMenuStd, typeListMenu_Std, colorListMenu_Std, upgrclsListMenu_Std);
			
			contextMenuImage.addSeparator();
			addStandardItems(contextMenuImage, typeListMenu_Image, colorListMenu_Image, upgrclsListMenu_Image);
			contextMenuImage.addSeparator();
			contextMenuImage.add(Gui.createMenuItem("Clear ImageFile",this,ActionCommand.ClearImage,Gui.ToolbarIcons.Delete));
			contextMenuImage.add(Gui.createMenuItem("Copy ImageFile" ,this,ActionCommand.CopyImage ,Gui.ToolbarIcons.Copy));
			contextMenuImage.add(Gui.createMenuItem("Paste ImageFile",this,ActionCommand.PasteImage,Gui.ToolbarIcons.Paste));
			
			contextMenuImageBG.addSeparator();
			addStandardItems(contextMenuImageBG, typeListMenu_ImageBG, colorListMenu_ImageBG, upgrclsListMenu_ImageBG);
			contextMenuImageBG.addSeparator();
			contextMenuImageBG.add(Gui.createMenuItem("Clear Background",this,ActionCommand.ClearBackground,Gui.ToolbarIcons.Delete));
			contextMenuImageBG.add(Gui.createMenuItem("Copy Background" ,this,ActionCommand.CopyBackground ,Gui.ToolbarIcons.Copy));
			contextMenuImageBG.add(Gui.createMenuItem("Paste Background",this,ActionCommand.PasteBackground,Gui.ToolbarIcons.Paste));
			contextMenuImageBG.addSeparator();
			contextMenuImageBG.add(Gui.createMenuItem("Add Background Color",this,ActionCommand.AddBackgroundColor));
			
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
								colorListMenu.setValue(Images.getInstance().colors.getColor(clickedID.getImageBG()));
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
			
			Gui.SimpleContextMenu imagePreviewContextMenu = new Gui.SimpleContextMenu(imageField);
			imagePreviewContextMenu.add(Gui.createMenuItem("Show Alpha View" , e->showImage(selectedID==null ? null : Images.generateAlphaImage(selectedID.imageFileName))));
			
			JPanel eastPanel = new JPanel(new BorderLayout(3, 3));
			eastPanel.add(imageField, BorderLayout.NORTH);
			eastPanel.add(textareaScrollPane, BorderLayout.CENTER);
			
			add(eastPanel, BorderLayout.EAST);
			
		}

		private void addStandardItems(DebugTableContextMenu contextMenu, Gui.ListMenu<GeneralizedID.Type> typeListMenu, Gui.NamedColorListMenu colorListMenu, Gui.ListMenu<GeneralizedID.UpgradeClass> upgrclsListMenu) {
			contextMenu.add(Gui.createMenuItem("Edit ID",this,ActionCommand.EditID));
			contextMenu.add(typeListMenu);
			contextMenu.add(colorListMenu);
			contextMenu.add(Gui.createMenuItem("ImageFile ...",this,ActionCommand.SelectImage));
			contextMenu.add(upgrclsListMenu);
			contextMenu.addSeparator();;
			contextMenu.add(Gui.createMenuItem("Delete",this,ActionCommand.DeleteID,Gui.ToolbarIcons.Delete));
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
				EditIdDialog dlg = new EditIdDialog(mainwindow, clickedID, tableModel.sourceIdMap.templateList);
				dlg.showDialog();
				if (dlg.hasIdDataChanged() || dlg.wasIdTemplateAdded()) {
					if (dlg.hasIdDataChanged()) dlg.transferChangesTo(clickedID);
					idChanged = true;
				}
			} break;
			
			case DeleteID: {
				int[] rows = table.getSelectedRows();
				int rowCount = rows.length>1?rows.length:(clickedCell!=null?1:0);
				if (rowCount>0) {
					String message = "Do you really want to delete "+(rowCount==1?"this ID":("these "+rowCount+" IDs"))+"?";
					if (JOptionPane.YES_OPTION==JOptionPane.showConfirmDialog(mainwindow, message, "Delete IDs", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE)) {
						//Gui.log_ln("Rows to delete: %s", Arrays.toString(rows));
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
						//Gui.log_ln("DeletedRows: %s", deletedRows.toString());
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
				Images.getInstance().showAddColorDialog(mainwindow,"Add Color");
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
			table.setCellEditor  (GeneralizedIDColumnID.UpgrCls, upgradeClassCellEditor);
			table.setCellRenderer(GeneralizedIDColumnID.UpgrCls, upgradeClassRenderer);
			
			ComboboxCellEditor<GeneralizedID.Type> typeCellEditor =
					new ComboboxCellEditor<GeneralizedID.Type>(SaveViewer.addNull(GeneralizedID.Type.values()));
			NonStringRenderer<GeneralizedID.Type> typeRenderer =
					new NonStringRenderer<GeneralizedID.Type>(t->{if (t instanceof GeneralizedID.Type)
						return ((GeneralizedID.Type)t).label; return null; });
			typeCellEditor.setRenderer(typeRenderer);
			table.setCellEditor  (GeneralizedIDColumnID.Type, typeCellEditor);
			table.setCellRenderer(GeneralizedIDColumnID.Type, typeRenderer);
			
			ComboboxCellEditor<String> imageCellEditor =
					new ComboboxCellEditor<String>(SaveViewer.addNull(Images.getInstance().extraImages.names));
			Images.getInstance().extraImages.addImageListListener(new ImageListListener() {
				@Override public void imageListChanged() {
					imageCellEditor.setValues(SaveViewer.addNull(Images.getInstance().extraImages.names));
					tableModel.updateTableColumn(GeneralizedIDColumnID.Image);
				}
			});
			table.setCellEditor(GeneralizedIDColumnID.Image, imageCellEditor);
			
			ComboboxCellEditor<NamedColor> colorCellEditor =
					new ComboboxCellEditor<NamedColor>(SaveViewer.addNull(Images.getInstance().colors.getArray()));
			Images.getInstance().colors.addColorListListender(new Images.Colors.ColorListListender() {
				@Override public void colorAdded   (NamedColor color)           { resetValues(); }
				@Override public void colorChanged (NamedColor color)           { resetValues(); }
				@Override public void orderChanged ()                           { resetValues(); }
				@Override public void colorsRemoved(Vector<NamedColor> removed) { resetValues(); }
				private void resetValues() { colorCellEditor.setValues(SaveViewer.addNull(Images.getInstance().colors.getArray())); }
			});
			
			TableView.NamedColorRenderer colorRenderer = new TableView.NamedColorRenderer();
			colorCellEditor.setRenderer(colorRenderer);
			table.setCellEditor  (GeneralizedIDColumnID.ImgBG, colorCellEditor);
			table.setCellRenderer(GeneralizedIDColumnID.ImgBG, colorRenderer);
		}
	
		private void showID(GeneralizedID id) {
			textarea.setText("");
			if (id==null) {
				imageField.setIcon(null);
				return;
			}
			
			showImage(id.getImage());
			
			textarea.append("ID     : "+id.id+"\r\n");
			if (id.type!=null  ) textarea.append("Type   : "+id.type.label+"\r\n");
			if (id.hasLabel  ()) textarea.append("Label  : "+id.label+"\r\n");
			if (id.hasSymbol ()) textarea.append("Symbol : "+id.symbol+"\r\n");
			if (id.hasImageFileName  ()) textarea.append("Image  : "+id.getImageFileName()+"\r\n");
			if (id.hasImageBG()) textarea.append("ImageBG: "+String.format("%06X",id.getImageBG())+"\r\n");
			
			for (SaveGameData key:id.usage.keySet()) {
				textarea.append("\r\n");
				textarea.append(key.filename+":\r\n");
				GeneralizedID.Usage usages = id.usage.get(key);
				if (usages.isEmpty())
					textarea.append("   none\r\n");
				for (String str:usages.inventoryUsages) textarea.append("   "+str+"\r\n");
				for (String str:usages.blueprintUsages) textarea.append("   "+str+"\r\n");
				for (String str:usages.bboUsages      ) textarea.append("   "+str+"\r\n");
			}
		}

		private void showImage(BufferedImage image) {
			if (image==null) {
				imageField.setIcon(null);
			} else {
				imageField.setIcon(new ImageIcon(image));
				imageField.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));							
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
			Type    ("Type"         ,        GeneralizedID.Type.class, 100,-1,200,200),
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
				
				Images.getInstance().colors.addColorListListender(new Images.Colors.ColorListListender() {
					@Override public void colorChanged(NamedColor color) { updateTableColumn(GeneralizedIDColumnID.ImgBG); }
					@Override public void colorAdded(NamedColor color) {}
					@Override public void orderChanged() {}
					@Override public void colorsRemoved(Vector<NamedColor> removed) {  updateTableColumn(GeneralizedIDColumnID.ImgBG);  }
				});
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
			public GeneralizedIDColumnID getColumnID(int columnIndex) {
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
				//Gui.log_ln("GeneralizedIDTableModel.deleteValue: [%d] -> %s", rowIndex, id==null?"<null>":id.getName());
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
				case ImgBG   : return Images.getInstance().colors.getColor( id.getImageBG() );
				case UpgrCls : return id.upgradeClass;
				case Usage :
					GeneralizedID.Usage usage = id.usage.get(usageKeys.get(columnIndex-columns.length).data);
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
		
		private final JLabel imagePreviewField;
		private final JTextArea valueOutput;
		
		private final TemplateList templateList;
		private final GeneralizedID id;
		private boolean hasIdDataChanged;
		private boolean ignoreIdDataChanges;
		private boolean wasIdTemplateAdded;
		private boolean showAlphaImage;
	
		private final Images.Colors.ColorListListender colorListListender;
		private final ImageListListener imageListListender;

		private final JTextField txtfldLabel;
		private final JTextField txtfldSymbol;

		private final JComboBox<GeneralizedID.Type> cmbbxType;
		private final JComboBox<String> cmbbxBgImage;
		private final JComboBox<NamedColor> cmbbxBgColor;
		private final JComboBox<GeneralizedID.UpgradeClass> cmbbxUpgradeClass;

		private final CachedAlphaImages cachedAlphaImages;
		
		public EditIdDialog(Window parent, GeneralizedID originalID, TemplateList templateList) {
			super(parent, getDlgTitle(originalID), ModalityType.APPLICATION_MODAL, false);
			
			this.templateList = templateList;
			this.id = new GeneralizedID(originalID);
			hasIdDataChanged = false;
			ignoreIdDataChanges = false;
			wasIdTemplateAdded = false;
			showAlphaImage = false;
			
			cachedAlphaImages = new CachedAlphaImages();
			
			valueOutput = new JTextArea();
			valueOutput.setEditable(false);
			JScrollPane textareaScrollPane = new JScrollPane(valueOutput);
			textareaScrollPane.getViewport().setPreferredSize(new Dimension(400, 100));
			
			txtfldLabel = Gui.createTextField(
				id.getLabel(),
				(String str)->{
					id.setLabel(str);
					setTitle(getDlgTitle(id));
					idDataChanged();
				}
			);
			
			txtfldSymbol = Gui.createTextField(
				id.getSymbol(),
				(String str)->{
					id.setSymbol(str);
					setTitle(getDlgTitle(id));
					idDataChanged();
				}
			);
			
			cmbbxType = new JComboBox<GeneralizedID.Type>(SaveViewer.addNull(GeneralizedID.Type.values()));
			cmbbxType.setSelectedItem(id.type);
			cmbbxType.addActionListener(e->{
				id.type = (GeneralizedID.Type)cmbbxType.getSelectedItem();
				idDataChanged();
			});
			cmbbxType.setRenderer(new NonStringRenderer<GeneralizedID.Type>(t->{if (t instanceof GeneralizedID.Type)
				return ((GeneralizedID.Type)t).label; return null; }));
			
			cmbbxBgImage = new JComboBox<String>(SaveViewer.addNull(Images.getInstance().extraImages.names));
			cmbbxBgImage.setSelectedItem(id.getImageFileName());
			cmbbxBgImage.addActionListener(e->{ id.setImageFileName((String)cmbbxBgImage.getSelectedItem()); idDataChanged(); });
			
			imageListListender = new ImageListListener() {
				@Override public void imageListChanged() {
					cmbbxBgImage.setModel(new DefaultComboBoxModel<>(SaveViewer.addNull(Images.getInstance().extraImages.names)));
					cmbbxBgImage.setSelectedItem(id.getImageFileName());
				}
			};
			
			EditIdDialog.BgColorComboBoxModel cmbbxBgColorModel = new EditIdDialog.BgColorComboBoxModel(SaveViewer.addNull(Images.getInstance().colors.getArray()));
			cmbbxBgColor = new JComboBox<>(cmbbxBgColorModel);
			cmbbxBgColor.setRenderer(new TableView.NamedColorRenderer());
			cmbbxBgColor.setSelectedItem(Images.getInstance().colors.getColor(id.getImageBG()));
			cmbbxBgColor.addActionListener(e->{
				NamedColor namedColor = (NamedColor)cmbbxBgColor.getSelectedItem();
				id.setImageBG(namedColor==null?null:namedColor.value);
				idDataChanged();
			});
			
			colorListListender = new Images.Colors.ColorListListender() {
				@Override public void colorAdded   (NamedColor color)           { cmbbxBgColor.addItem(color); cmbbxBgColor.revalidate(); }
				@Override public void colorChanged (NamedColor color)           { cmbbxBgColor.revalidate(); }
				@Override public void orderChanged ()                           { cmbbxBgColorModel.setData(SaveViewer.addNull(Images.getInstance().colors.getArray())); }
				@Override public void colorsRemoved(Vector<NamedColor> removed) { cmbbxBgColorModel.setData(SaveViewer.addNull(Images.getInstance().colors.getArray())); }
			};
			
			cmbbxUpgradeClass = new JComboBox<>(SaveViewer.addNull(GeneralizedID.UpgradeClass.values())	);
			cmbbxUpgradeClass.setRenderer(new NonStringRenderer<GeneralizedID.UpgradeClass>(t->{if (t instanceof GeneralizedID.UpgradeClass) return ((GeneralizedID.UpgradeClass)t).getLabel(); return null; }));
			cmbbxUpgradeClass.setSelectedItem(id.upgradeClass);
			cmbbxUpgradeClass.addActionListener(e->{id.upgradeClass=(GeneralizedID.UpgradeClass)cmbbxUpgradeClass.getSelectedItem(); idDataChanged();});
			
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
			buttonPanel.add(createButton("Apply" ,e->{closeDialog();}));
			buttonPanel.add(createButton("Cancel",e->{hasIdDataChanged = false; closeDialog();}));
			if (this.templateList!=null)
				buttonPanel.add(createButton("Define Current Values as Template",e->defineTemplate()));
			
			GridBagConstraints c = new GridBagConstraints();
			JPanel cmbbxPanel = new JPanel(new GridBagLayout());
			c.insets = new Insets(1, 0, 1, 0);
			
			JButton selectImageButton = createButton("Select Image",e->showImageList(cmbbxBgImage));
			JButton    addColorButton = createButton("Add Color"   ,e->Images.getInstance().showAddColorDialog(this,"Add Color"));
			
			addComp(cmbbxPanel,c,textareaScrollPane,1,1,GridBagConstraints.REMAINDER,1, GridBagConstraints.BOTH);
			
			addComp(cmbbxPanel,c,new JLabel("Label/Sym. : ",JLabel.RIGHT),0,0,1,1, GridBagConstraints.HORIZONTAL);
			addComp(cmbbxPanel,c,txtfldLabel ,1,0,1,1, GridBagConstraints.BOTH);
			addComp(cmbbxPanel,c,txtfldSymbol,0,0,GridBagConstraints.REMAINDER,1, GridBagConstraints.BOTH);
			
			addComp(cmbbxPanel,c,new JLabel("Type : ",JLabel.RIGHT),0,0,1,1, GridBagConstraints.HORIZONTAL);
			addComp(cmbbxPanel,c,cmbbxType,1,0,GridBagConstraints.REMAINDER,1, GridBagConstraints.BOTH);
			
			addComp(cmbbxPanel,c,new JLabel("Image File : ",JLabel.RIGHT),0,0,1,1, GridBagConstraints.HORIZONTAL);
			addComp(cmbbxPanel,c,cmbbxBgImage,1,0,1,1, GridBagConstraints.BOTH);
			addComp(cmbbxPanel,c,selectImageButton,0,0,GridBagConstraints.REMAINDER,1, GridBagConstraints.HORIZONTAL);
			
			addComp(cmbbxPanel,c,new JLabel("Background : ",JLabel.RIGHT),0,0,1,1, GridBagConstraints.HORIZONTAL);
			addComp(cmbbxPanel,c,cmbbxBgColor,1,0,1,1, GridBagConstraints.BOTH);
			addComp(cmbbxPanel,c,addColorButton,0,0,GridBagConstraints.REMAINDER,1, GridBagConstraints.HORIZONTAL);
			
			addComp(cmbbxPanel,c,new JLabel("Upgrade : ",JLabel.RIGHT),0,0,1,1, GridBagConstraints.HORIZONTAL);
			addComp(cmbbxPanel,c,cmbbxUpgradeClass,1,0,GridBagConstraints.REMAINDER,1, GridBagConstraints.BOTH);
			
			imagePreviewField = new JLabel();
			imagePreviewField.setBorder(BorderFactory.createEtchedBorder());
			imagePreviewField.setPreferredSize(new Dimension(256,256));
			imagePreviewField.setMinimumSize(new Dimension(256,256));
			
			JCheckBoxMenuItem miNormalView, miAlphaView;
			Gui.SimpleContextMenu imagePreviewContextMenu = new Gui.SimpleContextMenu(imagePreviewField);
			imagePreviewContextMenu.add(miNormalView = Gui.createCheckBoxMenuItem("Normal View", e->{ showAlphaImage = false; showImage(); }));
			imagePreviewContextMenu.add(miAlphaView  = Gui.createCheckBoxMenuItem("Alpha View" , e->{ showAlphaImage = true ; showImage(); }));
			imagePreviewContextMenu.addContextMenuInvokeListener((x,y)->{
				miNormalView.setSelected(!showAlphaImage);
				miAlphaView .setSelected( showAlphaImage);
			});
			
			
			JPanel contentPane = new JPanel(new BorderLayout(3,3));
			contentPane.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
			contentPane.add(cmbbxPanel,BorderLayout.CENTER);
			contentPane.add(imagePreviewField,BorderLayout.EAST);
			contentPane.add(buttonPanel,BorderLayout.SOUTH);
			
			showValues();
			this.createGUI(contentPane);
		}
	
		private static class BgColorComboBoxModel implements MutableComboBoxModel<NamedColor> {
			
			private final Vector<ListDataListener> listDataListeners;
			private final Vector<NamedColor> data;
			private Object selectedItem;

			public BgColorComboBoxModel(NamedColor[] data) {
				this.data = new Vector<>();
				Collections.addAll(this.data, data);
				listDataListeners = new Vector<>();
				selectedItem = null;
			}
		
			public void setData(NamedColor[] data) {
				int size = Math.max(this.data.size(), data.length);
				this.data.clear();
				Collections.addAll(this.data, data);
				fireContentsChangedEvent(0, size);
			}

			@Override public int getSize() { return data.size(); }
		
			@Override public NamedColor getElementAt(int index) {
				if (index<0 || index>=data.size()) return null;
				return data.get(index);
			}
		
			@Override public void    addListDataListener(ListDataListener l) { listDataListeners.   add(l); }
			@Override public void removeListDataListener(ListDataListener l) { listDataListeners.remove(l); }
		
			@Override public void setSelectedItem(Object selectedItem) { this.selectedItem = selectedItem; }
			@Override public Object getSelectedItem() { return selectedItem; }

			@Override public void addElement(NamedColor item) {
				data.add(item);
				int index = data.size()-1;
				fireIntervalAddedEvent(index, index);
			}

			@Override public void insertElementAt(NamedColor item, int index) {
				data.insertElementAt(item, index);
				fireIntervalAddedEvent(index, index);
			}

			@Override public void removeElementAt(int index) {
				data.remove(index);
				fireIntervalRemovedEvent(index, index);
			}

			@Override public void removeElement(Object obj) {
				int index = data.indexOf(obj);
				if (index<0) return;
				removeElementAt(index);
			}

			private void fireContentsChangedEvent(int index0, int index1) {
				ListDataEvent event = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, index0, index1);
				listDataListeners.forEach(ldl->ldl.contentsChanged(event));
			}
			private void fireIntervalRemovedEvent(int index0, int index1) {
				ListDataEvent event = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, index0, index1);
				listDataListeners.forEach(ldl->ldl.intervalRemoved(event));
			}
			private void fireIntervalAddedEvent(int index0, int index1) {
				ListDataEvent event = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, index0, index1);
				listDataListeners.forEach(ldl->ldl.intervalAdded(event));
			}
		
		}
		private void defineTemplate() {
			if (templateList==null) return;
			CreateTemplateDialog dlg = new CreateTemplateDialog(this, "Create New Template", id, templateList);
			dlg.showDialog();
			GeneralizedIDTemplate template = dlg.getResult();
			if (template!=null) {
				boolean successful = templateList.add(template);
				if (successful) Gui.log_ln("Added a new Template: %s", template);
				wasIdTemplateAdded |= successful;
			}
		}

		private static String getDlgTitle(GeneralizedID id) {
			return String.format("Set values of ID \"%s\"",id.getName());
		}
		
		private void addComp(JPanel panel, GridBagConstraints c, Component comp, double weightx, double weighty, int gridwidth, int gridheight, int fill) {
			c.weightx=weightx;
			c.weighty=weighty;
			c.gridwidth=gridwidth;
			c.gridheight=gridheight;
			c.fill = fill;
			panel.add(comp,c);
		}
		
		@Override public void windowOpened(WindowEvent e) {
			Images.getInstance().colors.addColorListListender(colorListListender);
			Images.getInstance().extraImages.addImageListListener(imageListListender);
		}
		@Override public void windowClosed(WindowEvent e) {
			Images.getInstance().colors.removeColorListListender(colorListListender);
			Images.getInstance().extraImages.removeImageListListener(imageListListender);
		}
	
		private void showImageList(JComboBox<String> cmbbxImages) {
			Images.SelectImageDialog dlg = new Images.SelectImageDialog(this,"Select image of "+id.getName(),id.getImageFileName());
			dlg.showDialog();
			if (dlg.hasChoosen()) {
				String result = dlg.getImageFileName();
				id.setImageFileName(result);
				idDataChanged();
				cmbbxImages.setSelectedItem(result);
			}
		}
	
		private JButton createButton(String title, ActionListener l) {
			JButton button = new JButton(title);
			button.addActionListener(l);
			return button;
		}
	
		private void idDataChanged() {
			if (ignoreIdDataChanges) return;
			hasIdDataChanged = true;
			showValues();
			if (templateList!=null) {
				SwingUtilities.invokeLater(()->{
					GeneralizedIDTemplate template = templateList.get(id);
					if (template!=null && !template.equals(id)) {
						JOptionPane.showMessageDialog(this, "Found a matching template. Values will be set.", "Matching Template Found", JOptionPane.INFORMATION_MESSAGE);
						ignoreIdDataChanges = true;
						if (id.label          ==null) { id.label          = template.label ;           txtfldLabel .setText( id.getLabel ()); }
						if (id.symbol         ==null) { id.symbol         = template.symbol;           txtfldSymbol.setText( id.getSymbol()); }
						if (id.type           ==null) { id.type           = template.type  ;           cmbbxType        .setSelectedItem(id.type); }
						if (id.imageFileName  ==null) { id.setImageFileName(template.imageFileName  ); cmbbxBgImage     .setSelectedItem(id.getImageFileName()); }
						if (id.imageBackground==null) { id.setImageBG      (template.imageBackground); cmbbxBgColor     .setSelectedItem(Images.getInstance().colors.getColor(id.getImageBG())); }
						if (id.upgradeClass   ==null) { id.upgradeClass   = template.upgradeClass;     cmbbxUpgradeClass.setSelectedItem(id.upgradeClass); }
						showValues();
						ignoreIdDataChanges = false;
					}
				});
			}
		}

		private void showValues() {
			valueOutput.setText("");
			
			valueOutput.append("ID     : "+(id.id    ==null?"--------":id.id    )+"\r\n");
			valueOutput.append("Label  : "+(id.label ==null?"--------":id.label )+"\r\n");
			valueOutput.append("Symbol : "+(id.symbol==null?"--------":id.symbol)+"\r\n");
			valueOutput.append("Type   : "+(id.type  ==null?"--------":id.type.label)+"\r\n");
			valueOutput.append("Image  : "+(id.hasImageFileName  ()?id.getImageFileName():"<none>")+"\r\n");
			valueOutput.append("ImageBG: "+(id.hasImageBG()?String.format("%06X",id.getImageBG()):"<none>")+"\r\n");
			
			showImage();
		}

		private void showImage() {
			if (showAlphaImage)
				setImage(cachedAlphaImages.get(id.imageFileName));
			else
				setImage(id.getImage());
		}

		private void setImage(BufferedImage image) {
			if (image==null) {
				imagePreviewField.setIcon(null);
			} else {
				imagePreviewField.setIcon(new ImageIcon(image));
				imagePreviewField.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));							
			}
		}
	
		public boolean wasIdTemplateAdded() { return wasIdTemplateAdded; }
		public boolean hasIdDataChanged() { return hasIdDataChanged; }
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