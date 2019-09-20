package net.schwarzbaer.java.games.nomanssky.saveviewer;

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
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Vector;
import java.util.function.Function;
import java.util.function.Supplier;

import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.IDMap;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Inventories.Inventory;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.KnownSteamIDs.AssignmentExistsException;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.ObjectWithSource;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.SolarSystem.Race;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.ArrayValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.BoolValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.FloatValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.IntegerValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.JSON_Array;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.JSON_Object;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.ObjectValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.PathIsNotSolvableException;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.StringValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value.Type;

public class SaveGameData {

	public static Error error;
	public static String errorMessage;
	private static boolean isStackTraceEnabled;
	
	public final String filename;
	public final int index;
	public final JSON_Object json_data;
	public HashMap<String, Vector<String>> deObfuscatorUsage = null;
	
	public final General general;
	public final Universe universe;
	public final DiscoveryData discoveryData;
	public Stats stats = null;
	public KnownWords knownWords = null;
	public KnownWords knownWords2 = null;
	public Inventories inventories = null;
	public KnownBlueprints knownBlueprints = null;
	public UnboundBuildingObject[] baseBuildingObjects = null;
	public Vector<PersistentPlayerBase> persistentPlayerBases = null;
	public Vector<StoredInteraction> storedInteractions = null;
	public Vector<TeleportEndpoints> teleportEndpoints = null;
	public Vector<Frigate> frigates = null;
	public Vector<FrigateMission> frigateMissions = null;
	public Freighter  freighter  = null;
	public SpaceShips spaceShips = null;
	public Exocrafts  exocrafts  = null;
	
	public SaveGameData(JSON_Object json_data, String filename, int index) {
		error = Error.NoError;
		errorMessage = "";
		isStackTraceEnabled = true;
		
		this.filename = filename;
		this.index = index;
		this.json_data = json_data;
		
		this.general = new General(this);
		this.universe = new Universe();
		this.discoveryData = new DiscoveryData(this);
	}

	public void setDeObfuscatorUsage(HashMap<String, Vector<String>> deObfuscatorUsage) {
		this.deObfuscatorUsage = deObfuscatorUsage;
	}
	
	public SaveGameData parse(boolean isNEXT) {
		if (!isNEXT) return this;
		
		general.parse();
		parseStats();
		knownBlueprints = KnownBlueprints.parse(this);
		knownWords  = parseKnownWords("KnownWords","Word","Races");
		knownWords2 = parseKnownWords("[KnownWords2]","[Word]","Races");
		parseDiscoveryData();
		inventories = Inventories.parseInventories(this);
		parseBaseBuildingObjects();
		parsePersistentPlayerBases();
		storedInteractions = StoredInteraction.parse(this);
		teleportEndpoints = TeleportEndpoints.parse(this);
		freighter  = Freighter .parse(this);
		spaceShips = SpaceShips.parse(this);
		exocrafts  = Exocrafts .parse(this);
		parseFrigates();
		parseFrigateMissions();
		universe.sort();
		//universe.writeToConsole();
		
		determineAdditionalInfos();
		
		GameInfos.readUniverseObjectDataFromDataPool(universe,false);
		GameInfos.saveAllIDsToFiles();
		SaveViewer.steamIDs.writeToFile();
		return this;
	}

	private void determineAdditionalInfos() {
		if (baseBuildingObjects!=null) {
			for (UnboundBuildingObject bbo:baseBuildingObjects) {
				if (bbo.galacticAddress==null) continue;
				if (bbo.objectID==null) continue;
				if (bbo.objectID.equals("^SUMMON_GARAGE")) {
					Universe.Planet planet = universe.findPlanet(bbo.galacticAddress);
					if (planet!=null) planet.additionalInfos.hasExocraftSummoningStation = true;
				}
			}
		}
		if (persistentPlayerBases!=null) {
			for (PersistentPlayerBase base:persistentPlayerBases) {
				if (base.galacticAddress==null) continue;
				if (base.baseType==null) continue;
				switch(base.baseType) {
				case FreighterBase: {
					Universe.SolarSystem system = universe.findSolarSystem(base.galacticAddress);
					if (system!=null) system.additionalInfos.hasFreighter = true;
				} break;
				case HomePlanetBase: {
					Universe.Planet planet = universe.findPlanet(base.galacticAddress);
					if (planet!=null) {
						planet.additionalInfos.bases.add(base);
						for (BuildingObject bbo:base.objects) {
							if (bbo.objectID==null) continue;
							if (bbo.objectID.equals("^SUMMON_GARAGE")) {
								planet.additionalInfos.hasExocraftSummoningStation = true;
							}
						}
					}
				} break;
				}
			}
		}
		if (teleportEndpoints!=null) {
			for (TeleportEndpoints tel:teleportEndpoints) {
				if (tel.universeAddress==null) continue;
				if (tel.universeAddress.isPlanet()) {
					Universe.Planet planet = universe.findPlanet(tel.universeAddress);
					if (planet!=null) planet.additionalInfos.hasTeleportEndPoint=true;
				}
				if (tel.universeAddress.isSolarSystem()) {
					Universe.SolarSystem system = universe.findSolarSystem(tel.universeAddress);
					if (system!=null) system.additionalInfos.hasTeleportEndPoint=true;
				}
			}
		}
	}

	private static Long parseHexFormatedNumber(JSON_Object obj, String valueName) {
		return parseHexFormatedNumber(obj.getValue(valueName));
	}

	private static Long parseHexFormatedNumber(Value value) {
		if (value==null) return null;
		switch(value.type) {
		case String:
			if (value instanceof StringValue) {
				String addressStr = ((StringValue)value).value;
				if (addressStr.startsWith("0x")) addressStr = addressStr.substring(2);
				try {
					long l = Long.parseUnsignedLong(addressStr, 16);
					value.wasProcessed=true;
					return l;
				} catch (NumberFormatException e) {}
			}
			break;
			
		case Integer:
			if (value instanceof IntegerValue) {
				value.wasProcessed=true;
				return ((IntegerValue)value).value;
			}
			break;
			
		default:
			break;
		}
		return null;
	}

	private static UniverseAddress parseUniverseAddressField(JSON_Object obj, String valueName) {
		Value addressValue = obj.getValue(valueName);
		if (addressValue==null) return null;
		
		switch(addressValue.type) {
		case String:
			if (addressValue instanceof StringValue) {
				String addressStr = ((StringValue)addressValue).value;
				if (isPlanetAddressOK(addressStr)) {
					addressValue.wasProcessed=true;
					return new UniverseAddress( Long.parseLong(addressStr.substring(2), 16) );
				}
			}
			break;
			
		case Integer:
			if (addressValue instanceof IntegerValue) {
				addressValue.wasProcessed=true;
				return new UniverseAddress( ((IntegerValue)addressValue).value );
			}
			break;
			
		default:
			break;
		}
		return null;
	}

	private static UniverseAddress parseUniverseAddressStructure(JSON_Object data, Object... path) {
		JSON_Object universeAddressObj = getObjectValue(data, path);
		if (universeAddressObj==null) return null;
		
		Long galaxyIndexLong = getIntegerValue(universeAddressObj,"RealityIndex");
		if (galaxyIndexLong==null) return null;
		int galaxyIndex = (int)(long)galaxyIndexLong;
		
		JSON_Object galacticAddressObj = getObjectValue(universeAddressObj,"GalacticAddress");
		if (galacticAddressObj==null) return null;
		
		Long voxelXLong = getIntegerValue(galacticAddressObj,"VoxelX");
		Long voxelYLong = getIntegerValue(galacticAddressObj,"VoxelY");
		Long voxelZLong = getIntegerValue(galacticAddressObj,"VoxelZ");
		if (voxelXLong==null) return null;
		if (voxelYLong==null) return null;
		if (voxelZLong==null) return null;
		int voxelX = (int)(long)voxelXLong;
		int voxelY = (int)(long)voxelYLong;
		int voxelZ = (int)(long)voxelZLong;
		
		Long solarSystemIndexLong = getIntegerValue(galacticAddressObj,"SolarSystemIndex");
		if (solarSystemIndexLong==null) return null;
		int solarSystemIndex = (int)(long)solarSystemIndexLong;
		
		Long planetIndexLong = getIntegerValue(galacticAddressObj,"PlanetIndex");
		if (planetIndexLong==null) return null;
		int planetIndex = (int)(long)planetIndexLong;
		
		return new UniverseAddress(galaxyIndex, voxelX, voxelY, voxelZ, solarSystemIndex, planetIndex);
	}

	public static class Position {
		
		public Coordinates pos;
		public Coordinates at;
		public Coordinates up;
		public PolarCoordinates gps;
		
		public Position() {
			this.pos = null;
			this.at = null;
			this.up = null;
			this.gps = null;
		}
		public Position(Position position) {
			this.pos = position.pos==null?null:new      Coordinates(position.pos);
			this.at  = position.at ==null?null:new      Coordinates(position.at );
			this.up  = position.up ==null?null:new      Coordinates(position.up );
			this.gps = position.gps==null?null:new PolarCoordinates(position.gps);
		}
		private static Position parse(JSON_Object obj, String valueName_Pos, String valueName_At, String valueName_Up) {
			Position position = new Position();
			position.pos = Coordinates.parse(obj, valueName_Pos);
			position.at  = Coordinates.parse(obj, valueName_At);
			position.up  = Coordinates.parse(obj, valueName_Up);
			position.gps = PolarCoordinates.parse(position.pos);
			return position;
		}
	}

	public static class PolarCoordinates {
		
		public final double latitude, longitude, radius;
		
		private PolarCoordinates(double latitude, double longitude, double radius) {
			this.latitude  = latitude;
			this.longitude = longitude;
			this.radius    = radius;
		}

		public PolarCoordinates(PolarCoordinates gps) {
			this(gps.latitude,gps.longitude,gps.radius);
		}

		public static PolarCoordinates parse(Coordinates pos) {
			if (pos==null) return null;
			if (pos.length<3) return null;
			
			//new PolarCoordinates();
			double radius    = Math.sqrt(pos.x*pos.x+pos.y*pos.y+pos.z*pos.z);
			double latitude  = Math.asin(pos.y/radius)/Math.PI*180;
			double longitude = Math.atan2(pos.x,pos.z)/Math.PI*180;
			
			return new PolarCoordinates(latitude, longitude, radius);
		}

		@Override
		public String toString() {
			return toString(true);
		}

		public String toString(boolean withRadius) {
			String latCh =  latitude<0?"S":"N";
			String lonCh = longitude<0?"W":"E";
			if (withRadius)
				return String.format(Locale.ENGLISH, " %s%05.2f  %s%06.2f  (R:%1.2f)", latCh, Math.abs(latitude), lonCh, Math.abs(longitude), radius);
			else
				return String.format(Locale.ENGLISH, " %s%05.2f  %s%06.2f"           , latCh, Math.abs(latitude), lonCh, Math.abs(longitude));
		}
	}

	public static class Coordinates extends Point3D {
	
		private static Coordinates parse(JSON_Object obj, String valueName) {
			JSON_Array arrayValue = getArrayValue(obj, valueName);
			if (arrayValue==null) return null;
			
			Coordinates coords = new Coordinates();
			for (int i=0; i<arrayValue.size(); ++i) {
				Value value = arrayValue.get(i);
				Double d = getFloat(value);
				if (d!=null) {
					value.wasProcessed=true;
					coords.set(i,d);
				}
			}
			
			return coords;
		}

		public double w1;
		private int length;
	
		public Coordinates() {
			super(0,0,0);
			this.w1 = 0;
			this.length = 0; 
		}
		
		public Coordinates(Coordinates p) {
			super(p);
			this.w1 = p.w1;
			this.length = p.length; 
		}
		
		public Coordinates(Point3D p) {
			super(p);
			this.w1 = 0;
			this.length = 0; 
		}

		public void set(int i, double value) {
			switch(i) {
			case 0: x = value; break;
			case 1: y = value; break;
			case 2: z = value; break;
			case 3: w1 = value; break;
			}
			length = Math.max(i+1, length);
		}
	
		@Override
		public String toString() {
			return toString("%f");
		}
	
		public String toString(String valueformat) {
			String vf = valueformat;
			switch(length) {
			case 0 : return String.format(Locale.ENGLISH, "()"                               );
			case 1 : return String.format(Locale.ENGLISH, "("+vf+")"                         , x);
			case 2 : return String.format(Locale.ENGLISH, "("+vf+","+vf+")"                  , x, y);
			case 3 : return String.format(Locale.ENGLISH, "("+vf+","+vf+","+vf+")"           , x, y, z);
			case 4 : return String.format(Locale.ENGLISH, "("+vf+","+vf+","+vf+","+vf+")"    , x, y, z, w1);
			default: return String.format(Locale.ENGLISH, "("+vf+","+vf+","+vf+","+vf+",...)", x, y, z, w1);
			}
		}
	}

	public static class Point3D {
		public double x,y,z;
	
		public Point3D(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	
		public Point3D(Point3D pos) {
			this.x = pos.x;
			this.y = pos.y;
			this.z = pos.z;
		}

		public void set(Point3D pos) {
			this.x = pos.x;
			this.y = pos.y;
			this.z = pos.z;
		}
	
		public void min(Point3D pos) {
			x = Math.min(x, pos.x);
			y = Math.min(y, pos.y);
			z = Math.min(z, pos.z);
		}
	
		public void max(Point3D pos) {
			x = Math.max(x, pos.x);
			y = Math.max(y, pos.y);
			z = Math.max(z, pos.z);
		}
		
		public Point3D add(double x, double y, double z) {
			return new Point3D(x+this.x, y+this.y, z+this.z);
		}

		public Point3D add(Point3D vec) {
			return new Point3D(x+vec.x, y+vec.y, z+vec.z);
		}
		
		public Point3D sub(Point3D vec) {
			return new Point3D(x-vec.x, y-vec.y, z-vec.z);
		}
	
		public Point3D mul(double size) {
			return new Point3D(x*size, y*size, z*size);
		}
		
		public Point3D mul(double x, double y, double z) {
			return new Point3D(x*this.x, y*this.y, z*this.z);
		}
	
		public double distTo(Point3D p) {
			return Math.sqrt((x-p.x)*(x-p.x) + (y-p.y)*(y-p.y) + (z-p.z)*(z-p.z));
		}
		
		public double distTo_onSphere(Point3D other) {
			double thisR = this.length();
			double otherR = other.length();
			double angle = Math.asin( this.crossProd(other).length()/thisR/otherR );
			double distOnSameHeight = angle * (thisR+otherR)/2;
			return Math.sqrt((thisR-otherR)*(thisR-otherR)+distOnSameHeight*distOnSameHeight);
		}
	
		public Point3D crossProd(Point3D p) {
			// this X p
			return new Point3D( y*p.z-z*p.y, z*p.x-x*p.z, x*p.y-y*p.x );
		}

		public double scalarProd(Point3D p) {
			return x*p.x+y*p.y+z*p.z;
		}

		public Point3D normalize() {
			return mul(1/length());
		}
	
		public static Point3D normalizeOrNull(Point3D vec) {
			if (vec==null || vec.isZero()) return null;
			return vec.normalize();
		}

		public double length() {
			return distTo(new Point3D(0,0,0));
		}

		public boolean isZero() {
			return x==0 && y==0 && z==0;
		}
		
		@Override
		public String toString() {
			return "Point3D("+toString("%1.3f",true)+")";
		}

		public String toString(String valueformat, boolean withComma) {
			String vf = valueformat;
			String sp = " ";
			if (withComma) sp = ",";
			return String.format(Locale.ENGLISH, vf+sp+vf+sp+vf, x, y, z);
		}
	}

	public static class TimeStamp implements Comparable<TimeStamp>{
		public final long value_s;
		private TimeStamp(long value_s) {
			this.value_s = value_s;
		}
		public static TimeStamp create(Long value_s) {
			if (value_s==null) return null;
			return new TimeStamp(value_s);
		}
		@Override public String toString() {
			return DateFormat.getDateTimeInstance().format(new Date(value_s*1000));
		}
		@Override public int compareTo(TimeStamp other) {
			return (int) (this.value_s-other.value_s);
		}
	}
	

	public static class Duration implements Comparable<Duration>{
		public final long value_s;
		private Duration(long value_s) {
			this.value_s = value_s;
		}
		public static Duration create(Long value_s) {
			if (value_s==null) return null;
			return new Duration(value_s);
		}
		@Override public String toString() {
			return toString(value_s);
		}
		@Override public int compareTo(Duration other) {
			return (int) (this.value_s-other.value_s);
		}
		public static String toString(Long value_s) {
			if (value_s==null) return "";
			return toString((long)value_s);
		}
		public static String toString(long value_s) {
			long s = value_s%60;
			value_s = (value_s-s)/60;
			long m = value_s%60;
			long h = (value_s-m)/60;
			return String.format("%3d:%02d:%02d", h,m,s);
		}
		public static String toString(double value_s) {
			long value_s_long = (long)value_s;
			int frac = (int)((value_s-value_s_long)*1000);
			long s = value_s_long%60;
			value_s_long = (value_s_long-s)/60;
			long m = value_s_long%60;
			long h = (value_s_long-m)/60;
			return String.format("%3d:%02d:%02d.%03d", h,m,s,frac);
		}
	}

	public static class Owner {
		public String LID;
		public String UID;
		public String USN;
		public TimeStamp TS;

		public static Owner parse(JSON_Object parentObj, String valueName) {
			JSON_Object objectValue = getObjectValue(parentObj, valueName);
			if (objectValue==null) return null;
			
			Owner owner = new Owner();
			owner.LID = getStringValue(objectValue, "LID");
			owner.UID = getStringValue(objectValue, "UID");
			owner.USN = getStringValue(objectValue, "USN");
			owner.TS  = TimeStamp.create(getIntegerValue(objectValue, "TS"));
			
			if (owner.USN!=null && !owner.USN.isEmpty() && owner.UID!=null && !owner.UID.isEmpty())
				try {
					SaveViewer.steamIDs.set(owner.UID, owner.USN);
				} catch (AssignmentExistsException e) {
					e.printConflict();
				}
			
			return owner;
		}
	}

	public static class SeedValue {
		
		private Boolean boolVal;
		private Long longVal;

		public SeedValue(Boolean boolVal, Long longVal) {
			super();
			this.boolVal = boolVal;
			this.longVal = longVal;
		}

		public static SeedValue parse(JSON_Array json_Array) {
			if (json_Array==null) return null;
			return new SeedValue(
				getBoolValue(json_Array, 0),
				parseHexFormatedNumber(getValue(json_Array, 1))
			);
		}

		@Override public String toString() {
			if (longVal==null)
				return String.format("<%s> <null>", boolVal, longVal);
			return     String.format("<%s> 0x%016X", boolVal, longVal);
		}
	}

	public static class ResourceBlock {
	
		public String filename = null;
		public SeedValue seed = null;
		public String altID = null;
		public JSON_Array ProceduralTexture_Samplers = null;
	
		public static ResourceBlock parse(JSON_Object data, Object... path) {
			JSON_Object resourceObj = getObjectValue(data, path);
			if (resourceObj==null) return null;
	
			ResourceBlock resourceBlock = new ResourceBlock();
			resourceBlock.filename = getStringValue(resourceObj, "Filename");
			resourceBlock.seed = SeedValue.parse( getArrayValue(resourceObj, "Seed") );
			resourceBlock.altID = getStringValue(resourceObj, "AltId");
			resourceBlock.ProceduralTexture_Samplers = getArrayValue(resourceObj, "ProceduralTexture", "Samplers");
			
			return resourceBlock;
		}
	}

	public static class TeleportEndpoints {
		
		public enum TeleportHost {
			Base("Base on Planet"),
			Spacestation("Space Station"),
			ExternalBase("Base of another Player"),
			;
			
			public String label;
			TeleportHost(String label) {
				this.label = label;
			}

			public static TeleportHost parseValue(String teleportHostStr) {
				if (teleportHostStr==null) return null;
				try { return valueOf(teleportHostStr); }
				catch (Exception e) { return null; }
			}
		}
		
		
		public String name;
		public String teleportHostStr;
		public TeleportHost teleportHost;
		public Coordinates position;
		public Coordinates lookAt;
		public UniverseAddress universeAddress;
		public PolarCoordinates gpsCoords;
		
		public TeleportEndpoints() {
			this.name = null;
			this.teleportHost = null;
			this.teleportHostStr = null;
			this.position = null;
			this.lookAt = null;
			this.universeAddress = null;
			this.gpsCoords = null;
		}

		private static Vector<TeleportEndpoints> parse(SaveGameData data) {
			JSON_Array arrayValue = getArrayValue(data.json_data,"PlayerStateData","TeleportEndpoints");
			if (arrayValue==null) return null;
			JSON_Array notParsableObjects = new JSON_Array();
			
			Vector<TeleportEndpoints> teleportEndpoints = new Vector<TeleportEndpoints>();
			for (int i=0; i<arrayValue.size(); ++i) {
				Value value = arrayValue.get(i);
				JSON_Object objectValue = getObject(value);
				if (objectValue==null) {
					notParsableObjects.add(value);
					continue;
				}
				
				TeleportEndpoints te = new TeleportEndpoints();
				te.universeAddress  = parseUniverseAddressStructure(objectValue, "UniverseAddress");
				te.position         = Coordinates     .parse(objectValue, "Position");
				te.gpsCoords        = PolarCoordinates.parse(te.position);
				te.lookAt           = Coordinates     .parse(objectValue, "LookAt");
				te.teleportHostStr  = getStringValue(objectValue, "TeleportHost");
				te.teleportHost     = TeleportHost.parseValue(te.teleportHostStr);
				te.name             = getStringValue(objectValue, "Name");
				
				if (te.universeAddress!=null) {
					ObjectWithSource obj = data.universe.getOrCreate(te.universeAddress);
					obj.foundInTeleportEndpoints.add(teleportEndpoints.size());
				}
				
				teleportEndpoints.add(te);
			}
			
			if (!notParsableObjects.isEmpty())
				SaveViewer.log_error_ln("Found "+notParsableObjects.size()+" not parseable TeleportEndpoints.");
			
			return teleportEndpoints;
		}
	}

	public static class StoredInteraction {
		
		public int groupIndex;
		public int interactionIndex;
		public UniverseAddress galacticAddress;
		public Long value;
		public Coordinates position;
		public PolarCoordinates gpsCoords;
		
		public StoredInteraction() {
			this.groupIndex = -1;
			this.interactionIndex = -1;
			this.galacticAddress = null;
			this.value = null;
			this.position = null;
			this.gpsCoords = null;
		}

		private static Vector<StoredInteraction> parse(SaveGameData data) {
			JSON_Array arrayValue = getArrayValue(data.json_data,"PlayerStateData","StoredInteractions");
			if (arrayValue==null) return null;
			JSON_Array notParsableObjects = new JSON_Array();
			
			Vector<StoredInteraction> storedInteractions = new Vector<StoredInteraction>();
			for (int i=0; i<arrayValue.size(); ++i) {
				Value value = arrayValue.get(i);
				JSON_Object objectValue = getObject(value);
				if (objectValue==null) {
					notParsableObjects.add(value);
					continue;
				}
				
				JSON_Array interactions = getArrayValue(objectValue,"Interactions");
				if (interactions==null) continue;
				
				for (int j=0; j<interactions.size(); ++j) {
					Value interactionValue = interactions.get(j);
					JSON_Object interaction = getObject(interactionValue);
					if (interaction==null) {
						notParsableObjects.add(interactionValue);
						continue;
					}
					StoredInteraction si = new StoredInteraction();
					si.groupIndex       = i;
					si.interactionIndex = j;
					si.galacticAddress  = parseUniverseAddressField(interaction, "GalacticAddress");
					si.value            = getIntegerValue(interaction, "Value");
					si.position         = Coordinates.parse(interaction, "Position");
					si.gpsCoords        = PolarCoordinates.parse(si.position);
					
					storedInteractions.add(si);
				}
			}
			
			if (!notParsableObjects.isEmpty())
				SaveViewer.log_error_ln("Found "+notParsableObjects.size()+" not parseable StoredInteractions.");
			
			return storedInteractions;
		}
	}

	private void parsePersistentPlayerBases() {
		JSON_Array arrayValue = getArrayValue(json_data,"PlayerStateData","PersistentPlayerBases");
		if (arrayValue==null) return;
		JSON_Array notParsableObjects = new JSON_Array();
		
//		persistentPlayerBases = new PersistentPlayerBases();
		persistentPlayerBases = new Vector<>();
		for (int baseIndex=0; baseIndex<arrayValue.size(); ++baseIndex) {
			Value value = arrayValue.get(baseIndex);
			JSON_Object objectValue = getObject(value);
			if (objectValue==null) {
				notParsableObjects.add(value);
				continue;
			}
			
			PersistentPlayerBase pb = new PersistentPlayerBase(this,baseIndex);
			pb.baseVersion     = getIntegerValue (objectValue, "BaseVersion");
			pb.galacticAddress = parseUniverseAddressField(objectValue, "GalacticAddress");
			pb.position        = Coordinates     .parse(objectValue, "Position");
			pb.gpsCoords       = PolarCoordinates.parse(pb.position);
			pb.forward         = Coordinates     .parse(objectValue, "Forward");
			pb.userData        = getIntegerValue (objectValue, "UserData");
			pb.lastUpdateTS    = TimeStamp.create(getIntegerValue (objectValue, "LastUpdateTimestamp"));
			pb.rid             = getStringValue  (objectValue, "RID");
			pb.owner           = Owner.parse     (objectValue, "Owner");
			pb.name            = getStringValue  (objectValue, "Name");
			pb.baseTypeStr     = getStringValue  (objectValue, "BaseType", "BaseType_");
			pb.baseType        = PersistentPlayerBase.BaseType.parseValue(pb.baseTypeStr);
			pb.value__wx7      = getIntegerValue_silent(objectValue, "??? [wx7]");
			
			pb.objects = parsePersistentPlayerBasesObjects(objectValue, "Objects", pb.baseTypeStr!=null?pb.baseTypeStr:"Base", baseIndex);
			
			if (pb.galacticAddress!=null) {
				ObjectWithSource obj = universe.getOrCreate(pb.galacticAddress);
				obj.foundInPersistentPlayerBases.add(persistentPlayerBases.size());
			}
			
			persistentPlayerBases.add(pb);
//			persistentPlayerBases.set(i,pb);
		}
		
		if (!notParsableObjects.isEmpty())
			SaveViewer.log_error_ln("Found "+notParsableObjects.size()+" not parseable PersistentPlayerBases.");
	}
	
	private BuildingObject[] parsePersistentPlayerBasesObjects(JSON_Object parentObj, String valueName, String baseType, int baseIndex) {
		JSON_Array arrayValue = getArrayValue(parentObj,valueName);
		if (arrayValue==null) return null;
		JSON_Array notParsableObjects = new JSON_Array();
		
		Vector<BuildingObject> vector = new Vector<BuildingObject>();
		for (int i=0; i<arrayValue.size(); i++) {
			Value value = arrayValue.get(i);
			JSON_Object objectValue = getObject(value);
			if (objectValue==null) {
				notParsableObjects.add(value);
				continue;
			}
			
			BuildingObject bbo = new BuildingObject(this);
			parseBuildingObject(objectValue, bbo, "["+(baseIndex+1)+"]"+baseType, i);
			
			vector.add(bbo);
		}
		
		if (!notParsableObjects.isEmpty())
			SaveViewer.log_error_ln("Found "+notParsableObjects.size()+" not parseable Objects in PersistentPlayerBases["+baseIndex+"].");
		
		return vector.toArray(new BuildingObject[0]);
	}

	private void parseBuildingObject(JSON_Object objectValue, BuildingObject bbo, String label, int index) {
		bbo.timestamp = TimeStamp.create(getIntegerValue (objectValue, "Timestamp"));
		bbo.objectID  = getStringValue  (objectValue, "ObjectID");
		bbo.userData  = getIntegerValue (objectValue, "UserData");
		bbo.position  = Position.parse  (objectValue, "Position", "Up", "At");
		if (bbo.objectID!=null) {
			GeneralizedID id = GameInfos.productIDs.get(bbo.objectID, this, GameInfos.GeneralizedID.Usage.Type.BuildingObject);
			id.getUsage(this).addBBOUsage(label,index);
		}
	}

//	public static class PersistentPlayerBases {
//		public PersistentPlayerBase planetBase;
//		public PersistentPlayerBase freighterBase;
//		public PersistentPlayerBase otherPlayersBase;
//		public Vector<PersistentPlayerBase> additionalBases;
//		private PersistentPlayerBases() {
//			this.planetBase       = null;
//			this.freighterBase    = null;
//			this.otherPlayersBase = null;
//			this.additionalBases = new Vector<>();
//		}
//		public void set(int i, PersistentPlayerBase pb) {
//			switch(i) {
//			case 0: planetBase       = pb; break;
//			case 1: freighterBase    = pb; pb.isFreighterBase=true; break;
//			case 2: otherPlayersBase = pb; break;
//			default: additionalBases.add(pb); break;
//			}
//		}
//	}

	public static class PersistentPlayerBase {

		public enum BaseType {
			FreighterBase("F","Freighter","Freighter Base"), HomePlanetBase("P","Planet","Planet Base");
			
			static BaseType parseValue(String str) {
				try { return valueOf(str); }
				catch (Exception e) { return null; }
			}

			private String shortLabel, midLabel, longLabel;
			private BaseType(String shortLabel, String midLabel, String longLabel) {
				this.shortLabel = shortLabel;
				this.midLabel   = midLabel;
				this.longLabel  = longLabel;
			}
			public String getLongLabel () { return longLabel; }
			public String getMidLabel  () { return midLabel; }
			public String getShortLabel() { return shortLabel; }
		}
		
		public final SaveGameData source;
		public final int baseIndex;
		
		public UniverseAddress galacticAddress;
		public String name;
		public Owner owner;
		public Long baseVersion;
		public String rid;
		public TimeStamp lastUpdateTS;
		public Long userData;
		public Coordinates forward;
		public Coordinates position;
		public PolarCoordinates gpsCoords;
		public BuildingObject[] objects;
		public String baseTypeStr;
		public BaseType baseType;
		public Long value__wx7;
		
		public PersistentPlayerBase(SaveGameData source, int baseIndex) {
			this.source = source;
			this.baseIndex = baseIndex;
			this.galacticAddress = null;
			this.name = null;
			this.owner = null;
			this.baseVersion = null;
			this.rid = null;
			this.userData = null;
			this.forward = null;
			this.position = null;
			this.gpsCoords = null;
			this.objects = null;
			this.baseTypeStr = null;
			this.baseType = null;
			this.value__wx7 = null;
		}
	}

	private void parseBaseBuildingObjects() {
		JSON_Array arrayValue = getArrayValue(json_data,"PlayerStateData","BaseBuildingObjects");
		if (arrayValue==null) return;
		JSON_Array notParsableObjects = new JSON_Array();
		
		Vector<UnboundBuildingObject> vector = new Vector<UnboundBuildingObject>();
		for (int i=0; i<arrayValue.size(); i++) {
			Value value = arrayValue.get(i);
			JSON_Object objectValue = getObject(value);
			if (objectValue==null) {
				notParsableObjects.add(value);
				continue;
			}
			UnboundBuildingObject bbo = new UnboundBuildingObject(this);
			bbo.galacticAddress = parseUniverseAddressField(objectValue, "GalacticAddress");
			bbo.regionSeed      = parseHexFormatedNumber   (objectValue, "RegionSeed");
			parseBuildingObject(objectValue, bbo, "BaseBuildingObjects", i);
			
			if (bbo.galacticAddress!=null) {
				ObjectWithSource obj = universe.getOrCreate(bbo.galacticAddress);
				obj.foundInBaseBuildingObjects.add(vector.size());
			}
			
			vector.add(bbo);
		}
		baseBuildingObjects = vector.toArray(new UnboundBuildingObject[0]);
		
		if (!notParsableObjects.isEmpty())
			SaveViewer.log_error_ln("Found "+notParsableObjects.size()+" not parseable BaseBuildingObjects.");
	}

	public static class UnboundBuildingObject extends BuildingObject {
		public UniverseAddress galacticAddress;
		public Long regionSeed;
		
		UnboundBuildingObject(SaveGameData source) {
			super(source);
			this.galacticAddress = null;
			this.regionSeed = null;
		}
	}

	public static class BuildingObject {

		//public GeneralizedID objectID1;
		public String specialName;
		
		public TimeStamp timestamp;
		public String objectID;
		public Long userData;
		public Position position;
		public final SaveGameData source;
		
		BuildingObject(SaveGameData source) {
			this.source = source;
			this.timestamp = null;
			this.objectID = null;
			this.userData = null;
			this.position = null;
			
			this.specialName = null;
			//this.objectID1 = null;
		}

		public BuildingObject(BuildingObject obj) {
			this.source = obj.source;
			this.timestamp = obj.timestamp;
			this.objectID  = obj.objectID;
			this.userData  = obj.userData;
			this.position  = obj.position==null?null:new Position(obj.position);
			this.specialName = obj.specialName;
			//this.objectID1   = obj.objectID1;
		}

		public String getNameOnly() {
			if (objectID==null) {
				if (specialName!=null) return specialName;
				return "";
			}
			GeneralizedID id = GameInfos.productIDs.get(objectID);
			if (id==null) return "";
			return id.getLabel();
		}

		public String getNameOrObjectID() {
			if (objectID==null) {
				if (specialName!=null) return specialName;
				return "";
			}
			GeneralizedID id = GameInfos.productIDs.get(objectID);
			if (id==null) return objectID;
			return id.getName();
		}

		public static BuildingObject createFromBase(PersistentPlayerBase playerbase) {
			BuildingObject obj = new BuildingObject(playerbase.source);
			obj.position = new Position();
			obj.position.pos = playerbase.position;
			if (playerbase.position!=null && !playerbase.position.isZero())
				obj.position.at = new Coordinates(playerbase.position.normalize());
			obj.position.up = playerbase.forward;
			obj.objectID = null;
			obj.specialName = playerbase.name;
			return obj;
		}

		@Override
		public String toString() {
			return "BuildingObject("+objectID+")";
		}
	}

	public static final class KnownBlueprints {

		public GeneralizedID[] products = null;
		public GeneralizedID[] technologies = null;
		
		private static KnownBlueprints parse(SaveGameData data) {
			KnownBlueprints knownBlueprints = new KnownBlueprints();
			knownBlueprints.technologies = parse(data, getArrayValue(data.json_data,"PlayerStateData","KnownTech"    ), GameInfos.techIDs   );
			knownBlueprints.products     = parse(data, getArrayValue(data.json_data,"PlayerStateData","KnownProducts"), GameInfos.productIDs);
			return knownBlueprints;
		}

		private static GeneralizedID[] parse(SaveGameData data, JSON_Array arrayValue, IDMap map) {
			if (arrayValue==null) return new GeneralizedID[0];
			
			GeneralizedID[] knownBlueprints = new GeneralizedID[arrayValue.size()];
			for (int i=0; i<arrayValue.size(); ++i) {
				Value value = arrayValue.get(i);
				String id = getString(value);
				if (id!=null) {
					knownBlueprints[i] = map.get(id,data,GeneralizedID.Usage.Type.Blueprint);// addGeneralizedID(map, id);
					knownBlueprints[i].getUsage(data).addBlueprintUsage((GameInfos.techIDs==map?"Technology":"Product"),i);
				}
			}
			Arrays.sort(knownBlueprints,Comparator.nullsLast(Comparator.comparing(b->b.id)));
			return knownBlueprints;
		}
	}

	public static class Freighter {
		
		public enum FreighterClass { CapitalFreighter, Freighter }
		
		public String name = null;
		public FreighterClass freighterClass = null; 
		public UniverseAddress ua = null;
		public Position pos = null;
		public Race crewRace = null;
		public Inventory inventory = null;
		public Inventory inventoryTech = null;
		
		private ResourceBlock crewResourceBlock = null;
		private ResourceBlock freighterResourceBlock = null;

		private static Freighter parse(SaveGameData data) {
			Freighter freighter = new Freighter();
			
			freighter.name = getStringValue(data.json_data,"PlayerStateData","PlayerFreighterName");
			freighter.ua   = parseUniverseAddressStructure(data.json_data,"PlayerStateData","FreighterUniverseAddress");
			freighter.pos  = Position.parse(getObjectValue(data.json_data, "PlayerStateData"), "FreighterPosition", "FreighterMatrixLookAt", "FreighterMatrixUp");
			
			freighter.crewResourceBlock      = ResourceBlock.parse(data.json_data,"PlayerStateData","CurrentFreighterNPC");
			freighter.freighterResourceBlock = ResourceBlock.parse(data.json_data,"PlayerStateData","CurrentFreighter");
			
			if (freighter.crewResourceBlock!=null && freighter.crewResourceBlock.filename!=null) {
				switch (freighter.crewResourceBlock.filename) {
				case "MODELS/COMMON/PLAYER/PLAYERCHARACTER/NPCKORVAX.SCENE.MBIN": freighter.crewRace = Race.Korvax; break;
				case "MODELS/COMMON/PLAYER/PLAYERCHARACTER/NPCVYKEEN.SCENE.MBIN": freighter.crewRace = Race.Vykeen; break;
				case "MODELS/COMMON/PLAYER/PLAYERCHARACTER/NPCGEK.SCENE.MBIN"   : freighter.crewRace = Race.Gek; break;
				}
			}
			
			if (freighter.freighterResourceBlock!=null && freighter.freighterResourceBlock.filename!=null) {
				switch (freighter.freighterResourceBlock.filename) {
				case "MODELS/COMMON/SPACECRAFT/INDUSTRIAL/CAPITALFREIGHTER_PROC.SCENE.MBIN": freighter.freighterClass = FreighterClass.CapitalFreighter; break;
				case "MODELS/COMMON/SPACECRAFT/INDUSTRIAL/FREIGHTER_PROC.SCENE.MBIN"       : freighter.freighterClass = FreighterClass.Freighter; break;
				}
			}
			
			String inventoryLabel = "Freighter";
			if (freighter.name!=null) inventoryLabel += String.format(" \"%s\"", freighter.name);
			if (freighter.crewRace!=null || freighter.freighterClass!=null) {
				String values = "";
				if (freighter.crewRace      !=null) values += String.format("%sCrew:%s" , values.isEmpty()?"":", ", freighter.crewRace);
				if (freighter.freighterClass!=null) values += String.format("%sClass:%s", values.isEmpty()?"":", ", freighter.freighterClass);
				inventoryLabel += "  ( "+values+" )";
			}
			
			freighter.inventory     = Inventories.parse(data,getObjectValue(data.json_data, "PlayerStateData", "FreighterInventory"         ), inventoryLabel    , "FreighterInventory");
			freighter.inventoryTech = Inventories.parse(data,getObjectValue(data.json_data, "PlayerStateData", "FreighterInventory_TechOnly"), "Freighter (Tech)", "FreighterInventory_TechOnly");
			
			return freighter;
		}
	}

	public static class SpaceShips extends VehicleGroup {

		private static SpaceShips parse(SaveGameData data) {
			return (SpaceShips)parseArray(
				data,
				getArrayValue(data.json_data, "PlayerStateData", "ShipOwnership"), "ShipOwnership",
				getIntegerValue(data.json_data, "PlayerStateData", "PrimaryShip"),
				SpaceShips::new, SpaceShip::new
			);
		}
		
		public static class SpaceShip extends VehicleGroup.Vehicle {
			public enum VehicleClass { Transporter, Fighter, Shuttle, Exotic }
			public VehicleClass vehicleClass = null;
			
			@Override protected String getPredefinedName(int i) { return null; }
			@Override protected String getTypeLabel() { return "SpaceShip"; }
			@Override protected void setVehicleClass(String resourcefilename) {
				switch (resourcefilename) {
				case "MODELS/COMMON/SPACECRAFT/DROPSHIPS/DROPSHIP_PROC.SCENE.MBIN": vehicleClass = VehicleClass.Transporter; break;
				case "MODELS/COMMON/SPACECRAFT/FIGHTERS/FIGHTER_PROC.SCENE.MBIN"  : vehicleClass = VehicleClass.Fighter; break;
				case "MODELS/COMMON/SPACECRAFT/SHUTTLE/SHUTTLE_PROC.SCENE.MBIN"   : vehicleClass = VehicleClass.Shuttle; break;
				case "MODELS/COMMON/SPACECRAFT/S-CLASS/S-CLASS_PROC.SCENE.MBIN"   : vehicleClass = VehicleClass.Exotic; break;
				default:
					SaveViewer.log_warn_ln("Unknown SpaceShip.VehicleClass: \"%s\"", resourcefilename);
				}
			}
			@Override protected String getVehicleClass() {
				if (vehicleClass == null) return null;
				return vehicleClass.toString();
			}
		}
	}
	public static class Exocrafts extends VehicleGroup {

		private static Exocrafts parse(SaveGameData data) {
			return (Exocrafts)parseArray(
				data,
				getArrayValue(data.json_data, "PlayerStateData", "VehicleOwnership"), "VehicleOwnership",
				getIntegerValue(data.json_data, "PlayerStateData", "PrimaryVehicle"),
				Exocrafts::new, Exocraft::new
			);
		}
		
		public static class Exocraft extends VehicleGroup.Vehicle {
			private static final String[] VehicleNames = new String[]{"Roamer", "Nomad", "Colossus", "Pilgrim", null, "Nautilon"};
			@Override protected String getPredefinedName(int i) {
				if (0<=i && i<VehicleNames.length) return VehicleNames[i];
				return null;
			}
			@Override protected String getTypeLabel() { return "Exocraft"; }
			@Override protected void setVehicleClass(String resourcefilename) {}
			@Override protected String getVehicleClass() { return null; }
		}
	}

	public static abstract class VehicleGroup {
		
		public Long primary = null;
		public Vehicle[] vehicles = null;

		protected static VehicleGroup parseArray(SaveGameData data, JSON_Array json_Array, String arraySourcePath, Long primary, Supplier<VehicleGroup> createGroup, Supplier<Vehicle> createVehicle) {
			
			VehicleGroup proto = createGroup.get();
			proto.primary = primary;
			
			if (json_Array!=null) {
				proto.vehicles = new Vehicle[json_Array.size()];
				for (int i=0; i<json_Array.size(); ++i)
					proto.vehicles[i] = Vehicle.parse(data, getObject(json_Array.get(i)), i, arraySourcePath+"["+i+"]", createVehicle, i==proto.primary);
			}	
			
			return proto;
		}

		public static abstract class Vehicle {
			public Inventory inventory = null;
			public Inventory inventoryTech = null;
			private ResourceBlock resourceBlock = null;
			
			protected abstract String getPredefinedName(int i);
			protected abstract String getTypeLabel();
			protected abstract void setVehicleClass(String resourcefilename);
			protected abstract String getVehicleClass();
			
			public static Vehicle parse(SaveGameData data, JSON_Object vehicleData, int i, String dataSourcePath, Supplier<Vehicle> createVehicle, boolean isPrimary) {
				if (vehicleData == null) return null;
				
				Vehicle vehicle = createVehicle.get();
				
				vehicle.resourceBlock = ResourceBlock.parse(vehicleData,"Resource");
				if (vehicle.resourceBlock!=null && vehicle.resourceBlock.filename!=null && !vehicle.resourceBlock.filename.isEmpty())
					vehicle.setVehicleClass(vehicle.resourceBlock.filename);
				
				String baseLabel = vehicle.getTypeLabel()+" "+(i+1);
				
				String predefinedName = vehicle.getPredefinedName(i);
				if (predefinedName!=null && !predefinedName.isEmpty()) baseLabel = "["+(i+1)+"] "+predefinedName;
				
				String inventoryLabel     = baseLabel;
				String inventoryTechLabel = baseLabel+" (Tech)";
				
				String name = getStringValue(vehicleData,"Name");
				String classStr = vehicle.getVehicleClass();
				
				if (name!=null && !name.isEmpty()) inventoryLabel += " \""+name+"\"";
				if (classStr!=null) inventoryLabel += " <"+classStr+">";
				if (isPrimary)      inventoryLabel += "   [Primary]";
				
				vehicle.inventory     = Inventories.parse(data,getObjectValue(vehicleData,"Inventory"         ), inventoryLabel    , dataSourcePath+".Inventory");
				vehicle.inventoryTech = Inventories.parse(data,getObjectValue(vehicleData,"Inventory_TechOnly"), inventoryTechLabel, dataSourcePath+".Inventory_TechOnly");
				
				return vehicle;
			}
		}
	}

	public final static class Inventories {

		private static Inventories parseInventories(SaveGameData data) {
			Inventories inventories = new Inventories();
			inventories.player.standard    = Inventories.parse(data,getObjectValue(data.json_data, "PlayerStateData", "Inventory"                  ), "Player"          , "Inventory"         );
			inventories.player.tech        = Inventories.parse(data,getObjectValue(data.json_data, "PlayerStateData", "Inventory_TechOnly"         ), "Player (Tech)"   , "Inventory_TechOnly");
			inventories.player.cargo       = Inventories.parse(data,getObjectValue(data.json_data, "PlayerStateData", "Inventory_Cargo"            ), "Player (Cargo)"  , "Inventory_Cargo"   );
			inventories.ship_old           = Inventories.parse(data,getObjectValue(data.json_data, "PlayerStateData", "ShipInventory"              ), "Ship (old)"      , "ShipInventory"     );
			inventories.multitool          = Inventories.parse(data,getObjectValue(data.json_data, "PlayerStateData", "WeaponInventory"            ), "MultiTool"       , "WeaponInventory"   );
			inventories.grave              = Inventories.parse(data,getObjectValue(data.json_data, "PlayerStateData", "GraveInventory"             ), "Grave"           , "GraveInventory"    );
			
			inventories.chests = new Inventory[10];
			for (int i=0; i<inventories.chests.length; ++i)
				inventories.chests[i] = Inventories.parse(data,getObjectValue(data.json_data, "PlayerStateData", "Chest"+(i+1)+"Inventory"), "Container "+i, "Chest"+(i+1)+"Inventory");
			inventories.magicChest        = Inventories.parse(data,getObjectValue(data.json_data, "PlayerStateData", "ChestMagicInventory" ), "Magic Chest"  , "ChestMagicInventory" );
			inventories.magicChest2       = Inventories.parse(data,getObjectValue(data.json_data, "PlayerStateData", "ChestMagic2Inventory"), "Magic Chest 2", "ChestMagic2Inventory");
			inventories.ingredientStorage = Inventories.parse(data,getObjectValue(data.json_data, "PlayerStateData", "IngredientStorageInventory"), "Ingredient Storage", "IngredientStorageInventory");
			
			return inventories;
		}
		
		private static Inventory parse(SaveGameData source, JSON_Object inventoryData, String inventoryLabel, String inventorySourcePath) {
			if (inventoryData==null) return null;
			
			Inventory inventory = new Inventory(inventoryLabel);
			inventory.substanceMaxStorageMultiplier = getIntegerValue(inventoryData, "SubstanceMaxStorageMultiplier");
			inventory.productMaxStorageMultiplier   = getIntegerValue(inventoryData, "ProductMaxStorageMultiplier");
			inventory.width   = getIntegerValue(inventoryData, "Width");
			inventory.height  = getIntegerValue(inventoryData, "Height");
			inventory.isCool  = getBoolValue   (inventoryData, "IsCool");
			inventory.version = getIntegerValue(inventoryData, "Version");
			
			inventory.inventoryClass = getStringValue(inventoryData, "Class","InventoryClass");

			if (inventory.inventoryClass==null) {
				JSON_Object classObj = getObjectValue(inventoryData, "Class");
				if (classObj==null) inventory.inventoryClass = "<no \"Class\" value>";
				else {
					Value value = classObj.getValue("InventoryClass");
					if (value==null) inventory.inventoryClass = "<no \"Class.InventoryClass\" value>";
					else             inventory.inventoryClass = "<wrong \"Class.InventoryClass\" value>";
				}
			}
			
			if (inventory.width!=null && inventory.height!=null && inventory.width>0 && inventory.height>0) {
				JSON_Array arrSlots            = getArrayValue(inventoryData,"Slots");
				JSON_Array arrValidSlotIndices = getArrayValue(inventoryData,"ValidSlotIndices");
				JSON_Array arrSpecialSlots     = getArrayValue(inventoryData,"SpecialSlots"    );
				if (arrSlots           !=null) inventory.usedSlots  = arrSlots.size();
				if (arrValidSlotIndices!=null) inventory.validSlots = arrValidSlotIndices.size();
				
				inventory.parseSlots(source, (int)(long)inventory.width, (int)(long)inventory.height, arrSlots, arrValidSlotIndices, arrSpecialSlots, inventoryLabel, inventorySourcePath);
			}
			inventory.parseBaseStatValues(getArrayValue(inventoryData,"BaseStatValues"), inventoryLabel, inventorySourcePath);
			
			return inventory;
		}
		
		public Player player;
		public Inventory multitool = null;
		public Inventory[] chests = null;
		public Inventory magicChest = null;
		public Inventory magicChest2 = null;
		public Inventory ingredientStorage = null;
		public Inventory ship_old = null;
		public Inventory grave = null;
		
		public Inventories() {
			super();
			this.player = new Player();
		}
		public static class Player {
			public Inventory standard;
			public Inventory tech;
			public Inventory cargo;
			public Player() {
				this.standard = null;
				this.tech = null;
				this.cargo = null;
			}
		}
		public final static class Inventory {
		
				private void parseSlots(SaveGameData source, int width, int height, JSON_Array arrSlots, JSON_Array arrValidSlotIndices, JSON_Array arrSpecialSlots, String inventoryLabel, String inventorySourcePath) {
					slots = new Slot[width][height];
					for (Slot[] row:slots)
						Arrays.fill(row, null);
					
					if (arrSlots==null) {
						SaveViewer.log_error_ln(inventorySourcePath+": Inventory has no slots.");
						return;
					}
					
					if (arrValidSlotIndices==null) {
						SaveViewer.log_error_ln(inventorySourcePath+": Inventory has no valid slot indices.");
						return;
					}
					
					int redundantIndices = 0;
					JSON_Array wrongIndices = new JSON_Array();
					for (Value value:arrValidSlotIndices) {
						JSON_Object indexObj = getObject(value);
						if (indexObj==null) continue;
						Long indexX = getIntegerValue(indexObj, "X");
						Long indexY = getIntegerValue(indexObj, "Y");
						if (indexX==null || indexX<0 || indexX>=width ) { wrongIndices.add(value); continue; }
						if (indexY==null || indexY<0 || indexY>=height) { wrongIndices.add(value); continue; }
						if (slots[(int)(long)indexX][(int)(long)indexY]==null) {
							slots[(int)(long)indexX][(int)(long)indexY] = new Slot(true,indexX,indexY);
						} else
							++redundantIndices;
					}
					if (!wrongIndices.isEmpty())
						SaveViewer.log_error_ln(inventorySourcePath+": Found "+wrongIndices.size()+" wrong index(es) in \"ValidSlotIndices\".");
					if (redundantIndices>0)
						SaveViewer.log_error_ln(inventorySourcePath+": Found "+redundantIndices+" redundant index(es) in \"ValidSlotIndices\".");
		
					redundantIndices = 0;
					wrongIndices.clear();
					for (Value value:arrSpecialSlots) {
						JSON_Object indexObj = getObject(value);
						if (indexObj==null) continue;
						Long   indexX = getIntegerValue(indexObj, "Index","X");
						Long   indexY = getIntegerValue(indexObj, "Index","Y");
						String type   = getStringValue (indexObj, "Type","InventorySpecialSlotType");
						if (indexX==null || indexX<0 || indexX>=width ) { wrongIndices.add(value); continue; }
						if (indexY==null || indexY<0 || indexY>=height) { wrongIndices.add(value); continue; }
						if (slots[(int)(long)indexX][(int)(long)indexY]==null) { wrongIndices.add(value); continue; }
						if (slots[(int)(long)indexX][(int)(long)indexY].specialSlotType==null) {
							slots[(int)(long)indexX][(int)(long)indexY].specialSlotType = type;
						} else
							++redundantIndices;
					}
					if (!wrongIndices.isEmpty())
						SaveViewer.log_error_ln(inventorySourcePath+": Found "+wrongIndices.size()+" wrong index(es) in \"SpecialSlots\".");
					if (redundantIndices>0)
						SaveViewer.log_error_ln(inventorySourcePath+": Found "+redundantIndices+" redundant index(es) in \"SpecialSlots\".");
					
					redundantIndices = 0;
					int notValidSlots = 0;
					JSON_Array wrongSlots = new JSON_Array();
					for (Value value:arrSlots) {
						JSON_Object slotObj = getObject(value);
						if (slotObj==null) { wrongSlots.add(value); continue; }
						Slot slot = new Slot(false);
						slot.typeStr      = getStringValue (slotObj, "Type","InventoryType");
						slot.idStr        = getStringValue (slotObj, "Id");
						slot.amount       = getIntegerValue(slotObj, "Amount");
						slot.maxAmount    = getIntegerValue(slotObj, "MaxAmount");
						slot.damageFactor = getFloatValue  (slotObj, "DamageFactor");
						slot.indexX       = getIntegerValue(slotObj, "Index","X");
						slot.indexY       = getIntegerValue(slotObj, "Index","Y");
						
						if (slot.typeStr!=null) {
							switch(slot.typeStr) {
							case "Product"   : slot.type = SlotType.Product; break;
							case "Technology": slot.type = SlotType.Technology; break;
							case "Substance" : slot.type = SlotType.Substance; break;
							}
						}
						if (slot.indexX==null || slot.indexX<0 || slot.indexX>=width ) { wrongSlots.add(value); continue; }
						if (slot.indexY==null || slot.indexY<0 || slot.indexY>=height) { wrongSlots.add(value); continue; }
						int x = (int)(long)slot.indexX;
						int y = (int)(long)slot.indexY;
						if (slots[x][y]==null   ) { wrongSlots.add(value); ++notValidSlots; continue; }
						if (!slots[x][y].isEmpty) { wrongSlots.add(value); ++redundantIndices; continue; }
						
						slots[x][y] = slot;
						
						if (slot.type!=null && slot.idStr!=null) {
							IDMap map = null;
							switch(slot.type) {
							case Product   : map = GameInfos.productIDs;   break;
							case Technology: map = GameInfos.techIDs;      break;
							case Substance : map = GameInfos.substanceIDs; break;
							}
							if (map!=null) {
								slot.id = map.get(slot.idStr,source,GeneralizedID.Usage.Type.InventorySlot); // addGeneralizedID(map, slot.idStr);
								slot.id.getUsage(source).addInventoryUsage(inventoryLabel,x,y);
							}
						}
					}
					if (!wrongSlots.isEmpty()) SaveViewer.log_error_ln(inventorySourcePath+": Found "+wrongSlots.size()+" wrong slots.");
					if (redundantIndices>0   ) SaveViewer.log_error_ln(inventorySourcePath+": Found "+redundantIndices+" redundant slots.");
					if (notValidSlots>0      ) SaveViewer.log_error_ln(inventorySourcePath+": Found "+notValidSlots+" not valid slots.");
				}
		
				private void parseBaseStatValues(JSON_Array valueArray, String inventoryLabel, String inventorySourcePath) {
					baseStatValues = null;
					if (valueArray==null) return;
					
					baseStatValues = new BaseStatValue[valueArray.size()];
					for (int i=0; i<valueArray.size(); ++i) {
						JSON_Object obj = getObject(valueArray.get(i));
						if (obj==null) { baseStatValues[i]=null; continue; }
						baseStatValues[i] = new BaseStatValue(getStringValue(obj,"BaseStatID"),getFloatValue(obj,"Value"));
					}
				}
		
				public final String label;
				public Long width;
				public Long height;
				public Long version;
				public String inventoryClass;
				public Boolean isCool;
				public Long productMaxStorageMultiplier;
				public Long substanceMaxStorageMultiplier;
				public Slot[][] slots;
				public Integer usedSlots;
				public Integer validSlots;
				public BaseStatValue[] baseStatValues;
				
				public Inventory(String label) {
					this.label = label;
					this.width = null;
					this.height = null;
					this.version = null;
					this.inventoryClass = null;
					this.isCool = null;
					this.productMaxStorageMultiplier = null;
					this.substanceMaxStorageMultiplier = null;
					this.slots = null;
					this.baseStatValues = null;
					this.usedSlots = null;
					this.validSlots = null;
				}
		
		//		private SaveGameData getSource2() {
		//			return SaveGameData.this;
		//		}
		
		//		public SaveGameData getSource() {
		//			return getSource2();
		//		}
		
				public enum SlotType { Product, Technology, Substance }
		
				public final static class Slot {
					public Long indexX;
					public Long indexY;
					public String idStr;
					public GeneralizedID id;
					public String typeStr;
					public SlotType type;
					public Long amount;
					public Long maxAmount;
					public Double damageFactor;
					public final boolean isEmpty;
					public String specialSlotType;
					
					
					public Slot(boolean isEmpty) {
						this.indexX = null;
						this.indexY = null;
						this.idStr = null;
						this.id = null;
						this.typeStr = null;
						this.type = null;
						this.amount = null;
						this.maxAmount = null;
						this.damageFactor = null;
						this.isEmpty = isEmpty;
						this.specialSlotType = null;
					}
		
					public Slot(boolean isEmpty, Long indexX, Long indexY) {
						this(isEmpty);
						this.indexX = indexX;
						this.indexY = indexY;
					}
		
		//			public SaveGameData getSource() {
		//				return SaveGameData.this;
		//			}
				
				}
		
				public final static class BaseStatValue {
					public final String baseStatID;
					public final Double value;
					private BaseStatValue(String baseStatID, Double value) {
						this.baseStatID = baseStatID;
						this.value = value;
					}
				}
			}
	
	}
	
	private void parseFrigates() {
		JSON_Array arrayValue = getArrayValue(json_data,"PlayerStateData","[Frigates]");
		if (arrayValue==null) return;
		JSON_Array notParsableObjects = new JSON_Array();
		
		frigates = new Vector<Frigate>();
		for (int i=0; i<arrayValue.size(); ++i) {
			Value value = arrayValue.get(i);
			JSON_Object objectValue = getObject(value);
			if (objectValue==null) {
				notParsableObjects.add(value);
				continue;
			}
			
			Frigate fr = new Frigate();
			fr.name             = getStringValue (objectValue, "[UserDefinedName]");
			fr.shipType         = getStringValue (objectValue, "[ShipType]", "[ShipType]");
			fr.crewRace         = getStringValue (objectValue, "[?CrewRace?]", "[?CrewRaceStr?]");
			fr.aquired          = TimeStamp.create(getIntegerValue(objectValue, "[?aquired?]"));
			fr.successfulFights = getIntegerValue(objectValue, "[Progress]");
			fr.expeditions      = getIntegerValue(objectValue, "[Expeditions]");
			fr.damages          = getIntegerValue(objectValue, "[Damages]");
			
			fr.damageValue      = getIntegerValue(objectValue, "[DamageValue]");
			
			fr.combatValue      = getIntegerValue(objectValue, "Stats",0);
			fr.explorationValue = getIntegerValue(objectValue, "Stats",1);
			fr.miningValue      = getIntegerValue(objectValue, "Stats",2);
			fr.diplomacyValue   = getIntegerValue(objectValue, "Stats",3);
			fr.fuelConsumption  = getIntegerValue(objectValue, "Stats",4);
			
			fr.unidentified.seed1 = SeedValue.parse( getArrayValue(objectValue, "[?Seed1?]") );
			fr.unidentified.seed2 = SeedValue.parse( getArrayValue(objectValue, "[?Seed2/UniverseAddress?]") );
			
			fr.unidentified.statVal5 = getIntegerValue(objectValue, "Stats",5);
			fr.unidentified.statVal6 = getIntegerValue(objectValue, "Stats",6);
			fr.unidentified.statVal7 = getIntegerValue(objectValue, "Stats",7);
			fr.unidentified.statVal8 = getIntegerValue(objectValue, "Stats",8);
			fr.unidentified.statVal9 = getIntegerValue(objectValue, "Stats",9);
			
			fr.unidentified.val1_5VG = getIntegerValue(objectValue, "??? [5VG]");
			fr.unidentified.val2_yJC = getIntegerValue(objectValue, "??? [yJC]");
			
			JSON_Array modArr = getArrayValue(objectValue,"[Modifications]");
			if (modArr!=null) {
				fr.modifications = new Vector<>();
				for (Value modVal:modArr) {
					String modStr = getString(modVal);
					if (modStr==null) { notParsableObjects.add(modVal); continue; }
					if (modStr.equals("^")) continue;
					Frigate.KnownModification mod = Frigate.KnownModification.getMod(modStr);
					fr.modifications.add( mod!=null ? mod : Frigate.EditableModification.getMod(modStr) );
				}
			}
			
			frigates.add(fr);
		}
		
		Frigate.EditableModification.saveKnownEditableModsToFile();
		
		if (!notParsableObjects.isEmpty())
			SaveViewer.log_error_ln("Found "+notParsableObjects.size()+" not parseable Frigates.");
	}
	
	public static class Frigate {

		public interface Modification {
			public String getLabel();
			public String getValue();
		}

		public enum KnownModification implements Modification {
			EXPLORE_PRI   ("Erkundungsspezialist"        ,"Erkundung: +15"),
			EXPLORE_SEC_1 ("Anomalienscanner"            ,"Erkundung: +2"),
			EXPLORE_SEC_2 ("Spektrum für interstellare Signale","Kampf: +4"),
			EXPLORE_SEC_3 ("Kartografiedrohnen"          ,"Erkundung: +6"),
			EXPLORE_SEC_4 ("Echtzeit-Archivierungsgerät" ,"Erkundung: +2"),
			EXPLORE_SEC_5 ("Gravitations-Visualisierer"  ,"Erkundung: +4"),
			EXPLORE_SEC_6 ("Raumzeitanomalienschild"     ,"Erkundung: +6"),
			EXPLORE_TER_1 ("Planetendaten-Schöpfer"      ,"Erkundung: +1"),
			EXPLORE_TER_4 ("Fauna-Analysegerät"          ,"Kampf: +1"),
			EXPLORE_TER_5 ("Holographische Anzeigen"     ,"Erkundung: +2"),
			EXPLORE_BAD_1 ("Wandernder Kompass"          ,"Erkundung: -2"),
			
			MINING_PRI    ("Industriespezialist"          ,"Industrie: +15"),
			MINING_SEC_2  ("Traktorstrahl"                ,"Kampf: +4"),
			MINING_SEC_3  ("Ultraschallschweißer"         ,"Industrie: +6"),
			MINING_SEC_4  ("Erzverarbeitungseinheit"      ,"Industrie: +2"),
			MINING_SEC_5  ("Terraforming-Strahlen"        ,"Industrie: +4"),
			MINING_SEC_6  ("Asteroidenpulverisierer"      ,"Industrie: +6"),
			MINING_TER_1  ("Mineralienextraktoren"        ,"Industrie: +1"),
			MINING_TER_3  ("Asteroidenscanner"            ,"Industrie: +3"),
			MINING_TER_4  ("Erntedrohnen"                 ,"Kampf: +1"),
			MINING_TER_5  ("Metalldetektor"               ,"Industrie: +2"),
			MINING_TER_6  ("Ferngesteuerte Bergbaueinheit","Industrie: +3"),
			MINING_BAD_2  ("Kleine Behälter"              ,"Industrie: -4"),
			MINING_BAD_5  ("Defekte Drohnen"              ,"Industrie: -2"),
			
			COMBAT_PRI    ("Kampfspezialist"               ,"Kampf: +15"),
			COMBAT_SEC_3  ("Tarngerät"                     ,"Kampf: +6"),
			COMBAT_SEC_4  ("Ultraschallwaffe"              ,"Kampf: +2"),
			COMBAT_SEC_5  ("Experimentelle Waffen"         ,"Kampf: +4"),
			COMBAT_TER_1  ("Versteckte Waffen"             ,"Kampf: +1"),
			COMBAT_TER_2  ("Munitionshersteller"           ,"Kampf: +2"),
			COMBAT_TER_4  ("Aggressive Sonden"             ,"Kampf: +1"),
			COMBAT_TER_5  ("Wütender Kapitän"              ,"Kampf: +2"),
			COMBAT_TER_6  ("Nachgerüstete Geschütze"       ,"Kampf: +3"),
			COMBAT_BAD_1  ("Feige Schützen"                ,"Kampf: -2"),
			COMBAT_BAD_2  ("Raketenwerfer aus zweiter Hand","Kampf: -4"),
			COMBAT_BAD_3  ("Defekte Torpedos"              ,"Kampf: -6"),
			
			TRADING_PRI   ("Handelspezialist"            ,"Handel: +15"),
			TRADING_SEC_1 ("Handelsanalysecomputer"      ,"Handel: +2"),
			TRADING_SEC_5 ("Propagandagerät"             ,"Handel: +4"),
			TRADING_SEC_6 ("Teleportationsgerät"         ,"Handel: +6"),
			TRADING_TER_1 ("Wirtschaftsscanner"          ,"Handel: +1"),
			TRADING_TER_3 ("Roboterdiener"               ,"Handel: +3"),
			TRADING_TER_5 ("Verhandlungsmodul"           ,"Handel: +2"),
			TRADING_TER_6 ("Gut gepflegte Crew"          ,"Handel: +3"),
			
			SPEED_TER_1   ("Ortszeit-Dilator"             ,"-1% Expeditionszeit"),
			SPEED_TER_2   ("Masseantrieb"                 ,"-1% Expeditionsdauer"),
			SPEED_TER_3   ("Navigationsexperte"           ,"-2% Expeditionsdauer"),
			SPEED_TER_4   ("Warp-Antrieb"                 ,"-2% Expeditionsdauer"),
			SPEED_TER_5   ("Wurmlochgenerator"            ,"-3% Expeditionsdauer"),
			SPEED_TER_6   ("Experimenteller Impulsantrieb","-3% Expeditionsdauer"),
			SPEED_TER_7   ("Motivierte Crew"              ,"-2% Expeditionsdauer"),
			
			FUEL_PRI      ("Unterstützungsspezialist"    ,"Treibstoffkosten der Expedition: -15"),
			FUEL_TER_1    ("Sauerstoffwiederverwerter"   ,"Treibstoffkosten der Expedition: -2"),
			FUEL_TER_2    ("Abgestimmte Antriebe"        ,"Treibstoffkosten der Expedition: -4"),
			FUEL_TER_3    ("Robotercrew"                 ,"Treibstoffkosten der Expedition: -6"),
			FUEL_TER_4    ("Photonensegel"               ,"Treibstoffkosten der Expedition: -2"),
			FUEL_TER_5    ("Übertakteter Stromverteiler" ,"Treibstoffkosten der Expedition: -4"),
			FUEL_TER_6    ("Solarmodule"                 ,"Treibstoffkosten der Expedition: -6"),
			FUEL_TER_7    ("Effizienter Warpantrieb"     ,"Treibstoffkosten der Expedition: -4"),
			FUEL_BAD_1    ("Durstige Crew"               ,"Kosten pro Warp: +1"),
			FUEL_BAD_3    ("Undichte Treibstoffrohre"    ,"Kosten pro Warp: +4"),
			
			INVULN_TER_1  ("Sich selbst reparierender Rumpf" ,"Schadensreduzierung"),
			INVULN_TER_2  ("Fortgeschrittene Wartungsdrohnen","Schadensreduzierung"),
			INVULN_TER_3  ("Holografische Komponenten"       ,"Schadensreduzierung"),
			
			;
			
			private String label;
			private String value;
		
			KnownModification(String label, String value) {
				this.label = label;
				this.value = value;
			}
		
			@Override public String getLabel() { return label; }
			@Override public String getValue() { return value; }
		
			public static KnownModification getMod(String modStr) {
				for (KnownModification mod:KnownModification.values())
					if (("^"+mod).equals(modStr))
						return mod;
				return null;
			}
		}

		public static class EditableModification implements Modification {
			
			private static HashMap<String,EditableModification> values = new HashMap<>();
			
			public static EditableModification getMod(String modStr) {
				EditableModification mod = values.get(modStr);
				if (mod==null) values.put(modStr, mod = new EditableModification(modStr));
				return mod;
			}

			public static void loadKnownEditableModsFromFile() {
				File file = new File(FileExport.FILE_KNOWN_EDITABLE_MODS);
				if (!file.isFile()) return;
				
				long start = System.currentTimeMillis();
				SaveViewer.log_ln("Read known Editable Frigate Modifications from file \"%s\" ...", file.getPath());
				
				boolean somethingChanged = false;
				EditableModification mod = null;
				String str;
				try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8))) {
					while ((str=in.readLine())!=null) {
						
						if (str.startsWith("[") && str.endsWith("]")) {
							String modStr = str.substring(1, str.length()-1);
							KnownModification knownMod = KnownModification.getMod(modStr);
							if (knownMod!=null) mod = null;
							else { mod = getMod(modStr); somethingChanged = true; }
							
						} else if (str.startsWith("label=")) {
							if (mod!=null) mod.label = str.substring("label=".length());
							
						} else if (str.startsWith("value=")) {
							if (mod!=null) mod.value = str.substring("value=".length());
							
						} else if (str.isEmpty()) {
							if (mod!=null) SaveViewer.log_warn_ln("   %-15s(%-30s,%s)", mod.modStr, '"'+mod.label+'"', '"'+mod.value+'"');
							
						}
					}
				}
				catch (FileNotFoundException e) { e.printStackTrace(); }
				catch (IOException e) { e.printStackTrace(); }
				
				if (!somethingChanged) SaveViewer.log_warn_ln("   All values from file are already known. File \"%s\" can be deleted.", file.getPath());
				SaveViewer.log_ln("   done (in %1.3fs)", (System.currentTimeMillis()-start)/1000.0f);
			}

			public static void saveKnownEditableModsToFile() {
				File file = new File(FileExport.FILE_KNOWN_EDITABLE_MODS);
				long start = System.currentTimeMillis();
				SaveViewer.log_ln("Write known Editable Frigate Modifications to file \""+file.getPath()+"\" ...");
				
				Vector<String> modStrs = new Vector<>(values.keySet());
				modStrs.sort(null);
				
				try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8));) {
					for (String modStr:modStrs) {
						EditableModification mod = values.get(modStr);
						out.printf("[%s]%n", mod.modStr);
						out.printf("label=%s%n", mod.label);
						out.printf("value=%s%n", mod.value);
						out.println();
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				
				SaveViewer.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
			}
			
			private final String modStr;
			public String label;
			public String value;
			
			private EditableModification(String modStr) {
				this.modStr = modStr;
				label = "\""+modStr+"\"";
				value = "???";
			}
			@Override public String getLabel() { return label; }
			@Override public String getValue() { return value; }
			@Override public String toString() { return modStr; }

			public boolean isUndefined() { return label.startsWith("\"^"); }
		}
		
		public static class Unidentified {
			
			public SeedValue seed1;
			public SeedValue seed2;
			
			public Long val1_5VG;
			
			public Long statVal5;
			public Long statVal6;
			public Long statVal7;
			public Long statVal8;
			public Long statVal9;
			
			public Long val2_yJC;
			//public Long val3_7hK; // !=0 bei Schaden an Fregatte
			
			Unidentified() {
				this.seed1 = null;
				this.seed2 = null;
				this.val1_5VG = null;
				this.statVal5 = null;
				this.statVal6 = null;
				this.statVal7 = null;
				this.statVal8 = null;
				this.statVal9 = null;
				this.val2_yJC = null;
				//this.val3_7hK = null;
			}

			public String getUnidentifiedLongs() {
				return String.format("%s | %s, %s, %s, %s, %s | %s", val1_5VG, statVal5, statVal6, statVal7, statVal8, statVal9, val2_yJC/*, val3_7hK*/);
			}
		}

		public String name;
		public String shipType;
		public String crewRace;
		public TimeStamp aquired;
		
		public Long successfulFights;
		public Long expeditions;
		public Long damages;
		public Long fuelConsumption;
		
		public Long combatValue;
		public Long explorationValue;
		public Long miningValue;
		public Long diplomacyValue;
		
		public Vector<Modification> modifications;
		public Long damageValue;
		
		public Unidentified unidentified;
		
		public Frigate() {
			this.name = null;
			this.shipType = null;
			this.crewRace = null;
			this.aquired = null;
			this.successfulFights = null;
			this.expeditions = null;
			this.damages = null;
			this.fuelConsumption = null;
			this.combatValue = null;
			this.explorationValue = null;
			this.miningValue = null;
			this.diplomacyValue = null;
			this.modifications = new Vector<>();
			this.unidentified = new Unidentified();
		}
		
	}
	
	private void parseFrigateMissions() {
		JSON_Array arrayValue = getArrayValue(json_data,"PlayerStateData","[RunningFrigateMissions]");
		if (arrayValue==null) return;
		JSON_Array notParsableObjects = new JSON_Array();
		
		JSON_Array array;
		frigateMissions = new Vector<>();
		for (int i=0; i<arrayValue.size(); ++i) {
			Value value = arrayValue.get(i);
			JSON_Object objectValue = getObject(value);
			if (objectValue==null) {
				notParsableObjects.add(value);
				continue;
			}
			
			FrigateMission frm = new FrigateMission();
			frigateMissions.add(frm);
			
			frm.seed             = SeedValue.parse( getArrayValue(objectValue, "Seed") );
			frm.universeAddress  = parseUniverseAddressField(objectValue, "UA");
			frm.name             = getStringValue (objectValue, "[UserDefinedName]");
			frm.startTime        = TimeStamp.create(getIntegerValue(objectValue, "[MissionStartTime]"));
			frm.timeOfLastEvent  = TimeStamp.create(getIntegerValue(objectValue, "[MissionTimeOfLastEvent]"));
			frm.missionType      = getStringValue (objectValue, "[MissionType]", "[MissionType]");
			frm.distance         = getStringValue (objectValue, "[MissionDistance]", "[MissionDistance]");
			
			array = getArrayValue(objectValue,"[??? sbg MissionValues1]"); frm.missionValues1 = (String)(array==null?"<NotFound>":array.toString(Value.Type.Integer));
			array = getArrayValue(objectValue,"[??? lD@ MissionValues2]"); frm.missionValues2 = (String)(array==null?"<NotFound>":array.toString(Value.Type.Integer));
			
			array = getArrayValue(objectValue,"[??? o@4 Position1]"); frm.position1 = (String)(array==null?"<NotFound>":array.toString(Value.Type.Float,"%1.3f"));
			array = getArrayValue(objectValue,"[??? 4j2 Position2]"); frm.position2 = (String)(array==null?"<NotFound>":array.toString(Value.Type.Float,"%1.3f"));
			
			array = getArrayValue(objectValue,"[??? hea Some IDs]"); frm.someIDs = (String)(array==null?"<NotFound>":array.toString(Value.Type.String));
			frm.progress         = getFloatValue(objectValue, "[??? b>d Progress in %]");
			
			frm.ID1_3oW          = getStringValue (objectValue, "[??? 3oW An ID]");
			frm.int1__DC         = getIntegerValue(objectValue, "[??? ?DC Int:0]");
			frm.int2_U87         = getIntegerValue(objectValue, "[??? U87 Int:14]");
			frm.int3_G_H         = getIntegerValue(objectValue, "[??? G;H Int:0]");
			frm.int4_omN         = getIntegerValue(objectValue, "[??? omN Int:14]");
			frm.bool1_b78        = getBoolValue   (objectValue, "[??? b78 Bool:false]");
			
			array = getArrayValue(objectValue,"[??? WZs Arr:[]]"); frm.array1_WZs = (String)(array==null?"<NotFound>":array.toString(null));
			array = getArrayValue(objectValue,"[??? 1xe Arr:[]]"); frm.array2_1xe = (String)(array==null?"<NotFound>":array.toString(null));
			
			JSON_Array missionTasks = getArrayValue(objectValue,"[MissionTasks]");
			if (missionTasks!=null) {
				frm.missionTasks.clear();
				for (int m=0; m<missionTasks.size(); ++m) {
					Value mtValue = missionTasks.get(m);
					JSON_Object mtObject = getObject(mtValue);
					if (mtObject==null) {
						notParsableObjects.add(mtValue);
						continue;
					}
					
					FrigateMission.FrigateMissionTask frmt = new FrigateMission.FrigateMissionTask();
					frm.missionTasks.add(frmt);
					
					frmt.seed            = SeedValue.parse( getArrayValue(mtObject, "Seed") );
					frmt.universeAddress = parseUniverseAddressField(mtObject, "UA");
					frmt.missionTaskType = getStringValue(mtObject, "[MissionTaskTypeID]");
					frmt.otherID         = getStringValue(mtObject, "[??? 7Q; An ID]");
					frmt.completed = getBoolValue(mtObject, "[MissionTaskCompleted]");
					
					array = getArrayValue(mtObject,"[??? iaH Arr:[]]"); frmt.array1_iaH = array==null?"<NotFound>":array.toString(null);
					array = getArrayValue(mtObject,"[??? QJG Arr:[]]"); frmt.array2_QJG = array==null?"<NotFound>":array.toString(null);
					array = getArrayValue(mtObject,"[??? fe2 Arr:[]]"); frmt.array3_fe2 = array==null?"<NotFound>":array.toString(null);
					
					frmt.bool2_fvN = getBoolValue(mtObject, "[??? fvN Bool:false]");
					frmt.bool3_8GD = getBoolValue(mtObject, "[??? 8GD Bool:false]");
				}
			}
		}
		
		Frigate.EditableModification.saveKnownEditableModsToFile();
		
		if (!notParsableObjects.isEmpty())
			SaveViewer.log_error_ln("Found "+notParsableObjects.size()+" not parseable RunningFrigateMissions.");
	}

	public static class FrigateMission {

		public SeedValue seed = null;
		public UniverseAddress universeAddress = null;
		public String name = null;
		public String missionType = null;
		public String distance = null;
		public Vector<FrigateMissionTask> missionTasks = new Vector<>();
		
		public TimeStamp startTime = null;
		public TimeStamp timeOfLastEvent = null;
		public String missionValues1 = null;
		public String missionValues2 = null;
		public String position1 = null;
		public String position2 = null;
		public String someIDs = null;
		public Double progress = null;
		
		public String ID1_3oW = null;
		public Long int1__DC = null;
		public Long int2_U87 = null;
		public Long int3_G_H = null;
		public Long int4_omN = null;
		public Boolean bool1_b78 = null;
		public String array1_WZs = null;
		public String array2_1xe = null;

		public static class FrigateMissionTask {
			public SeedValue seed = null;
			public UniverseAddress universeAddress = null;
			public String missionTaskType = null;
			public String otherID = null;
			public Boolean completed = null;
			
			public String array1_iaH = null;
			public String array2_QJG = null;
			public String array3_fe2 = null;
			public Boolean bool2_fvN = null;
			public Boolean bool3_8GD = null;
		}
		
	}
	
	private void parseDiscoveryData() {
		JSON_Array arrayValue_Store     = getArrayValue(json_data,"DiscoveryManagerData","DiscoveryData-v1","Store","Record");
		JSON_Array arrayValue_Available = getArrayValue(json_data,"DiscoveryManagerData","DiscoveryData-v1","Available");
		
		discoveryData.parseJsonArrays(arrayValue_Store,arrayValue_Available);
		discoveryData.findPlanetsAndSolarSystems();
		discoveryData.findAdditionalPlanetsAndSolarSystems();
		
		if (!discoveryData.notParsedStoreData.isEmpty())
			SaveViewer.log_error_ln("Found "+discoveryData.notParsedStoreData.size()+" not parseable DiscoveryStoreData.");
		if (!discoveryData.notParsedAvailableData.isEmpty())
			SaveViewer.log_error_ln("Found "+discoveryData.notParsedAvailableData.size()+" not parseable DiscoveryAvailableData.");
	}

	public final static class KnownSteamIDs {
		private HashMap<String,String> data;
		
		KnownSteamIDs() {
			data = new HashMap<>();
		}
		
		public String get(String steamID) {
			return data.get(steamID);
		}
		public void set(String steamID, String steamName) throws AssignmentExistsException {
			String prevValue = data.putIfAbsent(steamID, steamName);
			if (prevValue!=null && !prevValue.equals(steamName))
				throw new AssignmentExistsException(steamID, steamName, prevValue);
		}
		
		public static class AssignmentExistsException extends Exception {
			private static final long serialVersionUID = -9040442552016222917L;
			final String steamID,newName,oldName;
			public AssignmentExistsException(String steamID, String newName, String oldName) {
				this.steamID = steamID;
				this.newName = newName;
				this.oldName = oldName;
			}
			public void printConflict() {
				SaveViewer.log_error_ln("KnownSteamIDs:  [ID]%s  [Old]\"%s\" -> [New]\"%s\"", steamID,oldName,newName);
			}
		}
		
		void writeToFile() {
			long start = System.currentTimeMillis();
			SaveViewer.log_ln("Write KnownSteamIDs to file \""+FileExport.FILE_KNOWN_STEAM_ID+"\"...");
			try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(FileExport.FILE_KNOWN_STEAM_ID),StandardCharsets.UTF_8))) {
				Vector<String> ids = new Vector<String>(data.keySet());
				ids.sort(null);
				for (String steamID:ids) {
					String steamName = data.get(steamID);
					out.printf("%s=%s%n", steamID,steamName);
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			SaveViewer.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
		}
		void readFromFile() {
			long start = System.currentTimeMillis();
			try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(FileExport.FILE_KNOWN_STEAM_ID),StandardCharsets.UTF_8))) {
				SaveViewer.log_ln("Read KnownSteamIDs from file \""+FileExport.FILE_KNOWN_STEAM_ID+"\"...");
				String line;
				while ( (line=in.readLine())!=null ) {
					int pos = line.indexOf('=');
					if (pos<0) continue;
					String steamID   = line.substring(0,pos);
					String steamName = line.substring(pos+1);
					data.put(steamID, steamName);
				}
				SaveViewer.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public String getNameReplacement(String str) {
			String steamName = get(str);
			if (steamName==null) return str;
			return "[SteamID of \""+steamName+"\"]";
		}
	}

	public final static class DiscoveryData {
		
		enum SourceArray { AvailableData, StoreData }
		
		private SaveGameData data;
		public Vector<StoreData> storeData;
		public Vector<AvailableData> availableData;
		JSON_Array notParsedStoreData;
		JSON_Array notParsedAvailableData;
		public int storedDiscoveredItemOnPlanets;
		public int storedDiscoveredItemInSolarSystms;
		public int availDiscoveredItemOnPlanets;
		public int availDiscoveredItemInSolarSystms;
		
		public DiscoveryData(SaveGameData data) {
			this.data = data;
			this.storeData = new Vector<>();
			this.availableData = new Vector<>();
			notParsedStoreData     = new JSON_Array();
			notParsedAvailableData = new JSON_Array();
			storedDiscoveredItemOnPlanets = 0;
			storedDiscoveredItemInSolarSystms = 0;
			availDiscoveredItemOnPlanets = 0;
			availDiscoveredItemInSolarSystms = 0;
		}
		
		public void parseJsonArrays(JSON_Array arrStore, JSON_Array arrAvailable) {
			if (arrStore!=null) {
				for (Value objValue:arrStore) {
					JSON_Object object = getObject(objValue);
					if (object==null) { notParsedAvailableData.add(objValue); continue; }
					
					StoreData stData = new StoreData();
					
					// DD.UA  UniverseAddress
					// DD.DT  String
					// DD.VP  array of (hex formated Long or direct Long)
					parseDD(getObjectValue(object,"DD"), stData.DD);
					
					// DM  empty object
					JSON_Object dmObj = getObjectValue(object,"DM");
					if (dmObj!=null && !dmObj.isEmpty()) {
						stData.DM = "{"+dmObj.size()+"}";
						stData.DM_CN = getStringValue(dmObj,"CN");
					}
					
					// OWS
					//    LID  String
					//    UID  String
					//    USN  String
					//    TS   Long
					stData.OWS = Owner.parse(object, "OWS");
					
					// RID  String (evtl.)
					stData.RID = getStringValue_silent(object,"RID");
					if (stData.RID!=null) {
						try {
							stData.RID_bytes = Base64.getDecoder().decode(stData.RID/*.replace("\\","")*/);
							//stData.RID = Arrays.toString(stData.RID_bytes);
							//stData.RID = new String(stData.RID_bytes);
						}
						catch (IllegalArgumentException e) {}
					}
					
					// PTK  String (evtl.)
					stData.PTK = getStringValue_silent(object,"PTK");
					
					storeData.add(stData);
				}
			}
			
			if (arrAvailable!=null) {
				for (Value objValue:arrAvailable) {
					JSON_Object object = getObject(objValue);
					if (object==null) { notParsedAvailableData.add(objValue); continue; }
					
					AvailableData availData = new AvailableData();
					
					// TSrec  long --> TimeStamp
					availData.TSrec = TimeStamp.create( getIntegerValue(object,"TSrec") );
					
					// DD.UA  UniverseAddress
					// DD.DT  String
					// DD.VP  array of (hex formated Long or direct Long)
					parseDD(getObjectValue(object,"DD"), availData.DD);
					
					availableData.add(availData);
				}
			}
		}

		private void parseDD(JSON_Object ddObj, DDblock dd) {
			if (ddObj==null) return;
				
			// DD.UA  UniverseAddress
			dd.UA = parseUniverseAddressField(ddObj, "UA");
			
			// DD.DT  String
			dd.DT = getStringValue(ddObj,"DT");
			
			// DD.VP
			JSON_Array vpArr = getArrayValue(ddObj,"VP");
			if (vpArr!=null) {
				dd.VP = new Long[vpArr.size()];
				for (int i=0; i<vpArr.size(); ++i)
					dd.VP[i] = parseHexFormatedNumber(vpArr.get(i));
			}
		}

		public void findAdditionalPlanetsAndSolarSystems() {
			HashSet<UniverseAddress> unknownAdresses = new HashSet<>();
			HashSet<String> knownTypes = new HashSet<>();
			
			storedDiscoveredItemOnPlanets = 0;
			storedDiscoveredItemInSolarSystms = 0;
			
			for (StoreData stData:storeData) {
				UniverseObject.Type universeObject = processDDblock(stData.DD, SourceArray.StoreData, unknownAdresses, knownTypes);
				if (universeObject!=null)
					switch(universeObject) {
					case Planet     : ++storedDiscoveredItemOnPlanets; break;
					case SolarSystem: ++storedDiscoveredItemInSolarSystms; break;
					default: break;
					}
			}
			
			availDiscoveredItemOnPlanets = 0;
			availDiscoveredItemInSolarSystms = 0;
			
			for (AvailableData avData:availableData) {
				UniverseObject.Type universeObject = processDDblock(avData.DD, SourceArray.AvailableData, unknownAdresses, knownTypes);
				if (universeObject!=null)
					switch(universeObject) {
					case Planet     : ++availDiscoveredItemOnPlanets; break;
					case SolarSystem: ++availDiscoveredItemInSolarSystms; break;
					default: break;
					}
			}
			
//			System.out.println("Known Types ["+knownTypes.size()+"]");
//			for (String type:knownTypes)
//				System.out.println("   "+type);
			
			if (!unknownAdresses.isEmpty()) {
				SaveViewer.log_error_ln("Found undiscovered addresses ["+unknownAdresses.size()+"]");
				for (UniverseAddress ua:unknownAdresses)
					SaveViewer.log_error_ln("   "+ua.getCoordinates());
			}
		}
		
		private UniverseObject.Type processDDblock(DDblock DD, SourceArray sourceArray, HashSet<UniverseAddress> unknownAdresses, HashSet<String> knownTypes) {
			if (DD==null) return null;
			if (DD.DT!=null) {
				if (DD.DT.equals("Planet"     )) return null;
				if (DD.DT.equals("SolarSystem")) return null;
				knownTypes.add(DD.DT);
			}
			
			if (DD.UA==null) return null;
			
			if (DD.UA.isPlanet()) {
				Universe.Planet planet = data.universe.findPlanet(DD.UA);
				if (planet==null)
					unknownAdresses.add(DD.UA);
				else
					planet.addDiscoveredItem(DD.DT,sourceArray);
				return UniverseObject.Type.Planet;
			}
			
			if (DD.UA.isSolarSystem()) {
				Universe.SolarSystem solarSystem = data.universe.findSolarSystem(DD.UA);
				if (solarSystem==null)
					unknownAdresses.add(DD.UA);
				else
					solarSystem.addDiscoveredItem(DD.DT,sourceArray);
				return UniverseObject.Type.SolarSystem;
			}
			return null;
		}

		public void findPlanetsAndSolarSystems() {
			Universe.DiscoverableObject obj;
			
			for (int i=0; i<storeData.size(); i++) {
				StoreData data = storeData.get(i);
				if ((obj = getDiscNameObj(data.DD))!=null) {
					obj.foundInDiscStore.add(i);
					
					if (data.OWS.USN!=null) {
						if (obj.hasDiscoverer())
							obj.setDiscoverer(obj.getDiscoverer()+" | "+data.OWS.USN);
						else
							obj.setDiscoverer(data.OWS.USN);
					}
					if (data.DM_CN!=null) {
						if (obj.hasUploadedName())
							obj.setUploadedName(obj.getUploadedName()+" | "+data.DM_CN);
						else
							obj.setUploadedName(data.DM_CN);
					}
				}
			}
			
			for (int i=0; i<availableData.size(); i++) {
				AvailableData data = availableData.get(i);
				if ((obj = getDiscNameObj(data.DD))!=null)
					obj.foundInDiscAvail.add(i);
			}
		}

		private Universe.DiscoverableObject getDiscNameObj(DDblock dd) {
			if (dd.DT==null || dd.UA==null) return null;
				
			Universe.DiscoverableObject discnameObj = null;
			
			if (dd.DT.equals("Planet") && dd.UA.isPlanet())
				discnameObj = data.universe.getOrCreatePlanet(dd.UA);
			
			if (dd.DT.equals("SolarSystem") && dd.UA.isSolarSystem())
				discnameObj = data.universe.getOrCreateSolarSystem(dd.UA);
				
			return discnameObj;
		}

		public static class StoreData {
			// DD.UA  UniverseAddress
			// DD.DT  String
			// DD.VP[0]  String
			// DD.VP[1]  String | long
			// DM  mostly empty object
			//    CN (evtl.) String
			// OWS
			//    LID  String
			//    UID  String
			//    USN  String
			//    TS   Long
			// RID  String (evtl.)
			
			public DDblock DD;
			public String DM;
			public String DM_CN;
			public Owner OWS;
			public String RID;
			byte[] RID_bytes;
			public String PTK;
			
			public StoreData() {
				DD = new DDblock();
				DM = null;
				DM_CN = null;
				OWS = null;
				RID = null;
				RID_bytes = null;
				PTK = null;
			}
		}
		
		public static class AvailableData {
			// TSrec  long
			// DD.UA  UniverseAddress
			// DD.DT  String
			// DD.VP[0]  String
			// DD.VP[1]  String | long
			
			public TimeStamp TSrec;
			public DDblock DD;
			
			AvailableData() {
				TSrec = null;
				DD = new DDblock();
			}
		}
		
		public static class DDblock {
			public UniverseAddress UA;
			public String DT;
			public Long[] VP;
			
			DDblock() {
				UA = null;
				DT = null;
				VP = null;
			}
		}
	}

	public final static class General {
		
		private SaveGameData data;
		public UniverseAddress currentUniverseAddress = null;
		public UniverseAddress anomalyUA = null;
		public UniverseAddress graveUA = null;
		public Position anomalyPos = null;
		public Position gravePos   = null;
		public Long units        = null;
		public Long nanites      = null;
		public Long quicksilver  = null;
		public Long playerHealth = null;
		public Long playerShield = null;
		public Long energy     = null;
		public Long shipHealth = null;
		public Long shipShield = null;
		public Long timeAlive  = null;
		public Long totalPlayTime   = null;
		public Long hazardTimeAlive = null;
		public Long knownGlyphsMask = null;
		
		public General(SaveGameData data) {
			this.data = data;
		}
		
		public void parse() {
			
			currentUniverseAddress = parseUniverseAddressStructure(data.json_data,"PlayerStateData","UniverseAddress");
			if (currentUniverseAddress!=null) {
				if(currentUniverseAddress.isPlanet     ()) data.universe.getOrCreatePlanet     (currentUniverseAddress).isCurrPos = true;
				if(currentUniverseAddress.isSolarSystem()) data.universe.getOrCreateSolarSystem(currentUniverseAddress).isCurrPos = true;
			}
			anomalyUA   = parseUniverseAddressStructure(data.json_data,"PlayerStateData","AnomalyUniverseAddress");
			graveUA     = parseUniverseAddressStructure(data.json_data,"PlayerStateData","GraveUniverseAddress");
			anomalyPos   = Position.parse(getObjectValue(data.json_data, "PlayerStateData"), "AnomalyPosition", "AnomalyMatrixLookAt", "AnomalyMatrixUp"); 
			gravePos     = Position.parse(getObjectValue(data.json_data, "PlayerStateData"), "GravePosition", "GraveMatrixLookAt", "GraveMatrixUp");
			
			
			units           = getIntegerValue( data.json_data, "PlayerStateData","Units"           );
			nanites         = getIntegerValue( data.json_data, "PlayerStateData","Nanites"         );
			quicksilver     = getIntegerValue( data.json_data, "PlayerStateData","[Quicksilver]"   );
			
			playerHealth    = getIntegerValue( data.json_data, "PlayerStateData","Health"          );
			playerShield    = getIntegerValue( data.json_data, "PlayerStateData","Shield"          );
			energy          = getIntegerValue( data.json_data, "PlayerStateData","Energy"          );
			shipHealth      = getIntegerValue( data.json_data, "PlayerStateData","ShipHealth"      );
			shipShield      = getIntegerValue( data.json_data, "PlayerStateData","ShipShield"      );
			
			timeAlive       = getIntegerValue( data.json_data, "PlayerStateData","TimeAlive"       );
			totalPlayTime   = getIntegerValue( data.json_data, "PlayerStateData","TotalPlayTime"   );
			hazardTimeAlive = getIntegerValue( data.json_data, "PlayerStateData","HazardTimeAlive" );
			
			knownGlyphsMask = getIntegerValue( data.json_data, "PlayerStateData","KnownPortalRunes");
		}
		
//		public Long getUnits          () { return data.getIntegerValue( data.json_data, "PlayerStateData","Units"           ); }
//		public Long getNanites        () { return data.getIntegerValue( data.json_data, "PlayerStateData","Nanites"         ); }
//		public Long getPlayerHealth   () { return data.getIntegerValue( data.json_data, "PlayerStateData","Health"          ); }
//		public Long getPlayerShield   () { return data.getIntegerValue( data.json_data, "PlayerStateData","Shield"          ); }
//		public Long getEnergy         () { return data.getIntegerValue( data.json_data, "PlayerStateData","Energy"          ); }
//		public Long getShipHealth     () { return data.getIntegerValue( data.json_data, "PlayerStateData","ShipHealth"      ); }
//		public Long getShipShield     () { return data.getIntegerValue( data.json_data, "PlayerStateData","ShipShield"      ); }
//		public Long getTimeAlive      () { return data.getIntegerValue( data.json_data, "PlayerStateData","TimeAlive"       ); }
//		public Long getTotalPlayTime  () { return data.getIntegerValue( data.json_data, "PlayerStateData","TotalPlayTime"   ); }
//		public Long getHazardTimeAlive() { return data.getIntegerValue( data.json_data, "PlayerStateData","HazardTimeAlive" ); }
//		public Long getKnownGlyphsMaks() { return data.getIntegerValue( data.json_data, "PlayerStateData","KnownPortalRunes"); }
//		
//		public String getTimeAlive_TStr      () { Long v = getTimeAlive      (); if (v==null) return ""; return Duration.toString(v); }
//		public String getTotalPlayTime_TStr  () { Long v = getTotalPlayTime  (); if (v==null) return ""; return Duration.toString(v); }
//		public String getHazardTimeAlive_TStr() { Long v = getHazardTimeAlive(); if (v==null) return ""; return Duration.toString(v); }
//		
//		public Boolean     getTestBool   (Object... path) { return data.getBoolValue   (data.json_data, path); }
//		public Long        getTestInteger(Object... path) { return data.getIntegerValue(data.json_data, path); }
//		public Double      getTestFloat  (Object... path) { return data.getFloatValue  (data.json_data, path); }
//		public String      getTestString (Object... path) { return data.getStringValue (data.json_data, path); }
//		public JSON_Array  getTestArray  (Object... path) { return data.getArrayValue  (data.json_data, path); }
//		public JSON_Object getTestObject (Object... path) { return data.getObjectValue (data.json_data, path); }
	}

	public static final class UniverseAddress implements Comparable<UniverseAddress> {

		public final int galaxyIndex;
		public final int voxelX;
		final int voxelY;
		public final int voxelZ;
		final int solarSystemIndex;
		final int planetIndex;
		private final long address;
		
		public UniverseAddress(long address) {
			int voxelX_1 = (int)( address      & 0xFFF);
			int voxelZ_1 = (int)((address>>12) & 0xFFF);
			int voxelY_1 = (int)((address>>24) & 0xFF);
			if (voxelX_1>2047) voxelX = voxelX_1-4096; else voxelX = voxelX_1;
			if (voxelY_1> 127) voxelY = voxelY_1- 256; else voxelY = voxelY_1;
			if (voxelZ_1>2047) voxelZ = voxelZ_1-4096; else voxelZ = voxelZ_1;
			galaxyIndex    = (int)((address>>32) & 0xFF);
			solarSystemIndex = (int)((address>>40) & 0xFFF);
			planetIndex      = (int)((address>>52) & 0xF);
			this.address = address;
		}

		public UniverseAddress(int galaxyIndex, int voxelX, int voxelY, int voxelZ, int solarSystemIndex, int planetIndex) {
			this.galaxyIndex = galaxyIndex;
			this.voxelX = voxelX;
			this.voxelY = voxelY;
			this.voxelZ = voxelZ;
			this.solarSystemIndex = solarSystemIndex;
			this.planetIndex = planetIndex;
			long address_ = (((long)this.voxelY&0xFF)<<24) | (((long)this.voxelZ&0xFFF)<<12) | ((long)this.voxelX&0xFFF);
			address_ = address_ | (((long)this.galaxyIndex     &0xFF )<<32);
			address_ = address_ | (((long)this.solarSystemIndex&0xFFF)<<40);
			address_ = address_ | (((long)this.planetIndex     &0xFF )<<52);
			this.address = address_;
		}

		public long getPortalGlyphCode() {
			long portalGlyphCode = (((long)voxelY&0xFF)<<24) | (((long)voxelZ&0xFFF)<<12) | ((long)voxelX&0xFFF);
			portalGlyphCode |= ((long)solarSystemIndex&0xFFF)<<32;
			portalGlyphCode |= ((long)planetIndex     &0xFF )<<44;
			return portalGlyphCode;
		}

		public String getPortalGlyphCodeStr() {
			return String.format("%012X",getPortalGlyphCode());
		}

		public UniverseAddress(UniverseAddress ua, int solarSystemIndex, int planetIndex) {
			this(ua.galaxyIndex,ua.voxelX,ua.voxelY,ua.voxelZ,solarSystemIndex,planetIndex);
		}

		public UniverseAddress(UniverseAddress ua, int planetIndex) {
			this(ua.galaxyIndex,ua.voxelX,ua.voxelY,ua.voxelZ,ua.solarSystemIndex,planetIndex);
		}

		public long getAddress() {
			return address;
		}

		public String getAddressStr() {
			return String.format("0x%014X", address);
		}
		
		public static UniverseAddress parseAddressStr(String str) {
			if (str!=null && str.startsWith("0x"))
				try { return new UniverseAddress( Long.parseLong(str.substring(2), 16) ); }
				catch (NumberFormatException e) {}
			return null;
		}

		@Override
		public int hashCode() {
			return (int)((address>>32)&0xFFFFFFFF);
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof UniverseAddress)) return false;
			UniverseAddress other = (UniverseAddress)obj;
			return this.address == other.address;
		}

		@Override
		public int compareTo(UniverseAddress other) {
			if (other==null) return -1;
			if (this.galaxyIndex!=other.galaxyIndex) return this.galaxyIndex-other.galaxyIndex;
			if (this.voxelX!=other.voxelX) return this.voxelX-other.voxelX;
			if (this.voxelZ!=other.voxelZ) return this.voxelZ-other.voxelZ;
			if (this.voxelY!=other.voxelY) return this.voxelY-other.voxelY;
			if (this.solarSystemIndex!=other.solarSystemIndex) return this.solarSystemIndex-other.solarSystemIndex;
			if (this.planetIndex!=other.planetIndex) return this.planetIndex-other.planetIndex;
			return 0;
		}

		public boolean isPlanet() {
			return planetIndex>0;
		}

		public boolean isSolarSystem() {
			return planetIndex==0 && solarSystemIndex>0;
		}

		public boolean isRegion() {
			return planetIndex==0 && solarSystemIndex==0;
		}

		public Vector<String> getVerboseName(Universe universe) {
			Vector<String> output = new Vector<>();
			
			Universe.Galaxy galaxy = universe.findGalaxy(galaxyIndex);
			if (galaxy==null)
				galaxy = new Universe.Galaxy(universe, galaxyIndex);
			
			Universe.Region region = galaxy.findRegion(voxelX, voxelY, voxelZ);
			if (region==null)
				region = new Universe.Region(galaxy, voxelX, voxelY, voxelZ);
			
			if (isRegion())
				output.add(region.toString());
			else {
				Universe.SolarSystem sys = region.findSolarSystem(solarSystemIndex);
				if (sys==null)
					sys = new Universe.SolarSystem(region, solarSystemIndex);
				
				if (isSolarSystem())
					output.add(sys.toString());
				else {
					Universe.Planet planet = sys.findPlanet(planetIndex);
					if (planet==null)
						planet = new Universe.Planet(sys, planetIndex);
					
					if (isPlanet())
						output.add(planet.toString());
					else {
						output.add(getCoordinates());
						output.add("on "+planet.toString());
					}
					output.add("in "+sys.toString());
				}
				output.add("in "+region.toString());
			}
			output.add("in "+galaxy.toString());
			
			return output;
		}

		public String getVerboseNameInOneLine(Universe universe) {
			
			if (isRegion())
				return "Region \""+getCoordinates_Region()+"\"";
			
			if (isSolarSystem()) {
				Universe.SolarSystem sys = universe.findSolarSystem(this);
				return "SolarSystem \""+getCoordinates_Region()+" | "+sys.toString()+"\"";
			}
			
			if (isPlanet()) {
				Universe.SolarSystem sys = universe.findSolarSystem(this);
				Universe.Planet pln = universe.findPlanet(this);
				return "Planet \""+getCoordinates_Region()+" | "+sys.toString(true,true,false,true)+" | "+pln.toString()+"\"";
			}
			
			return getCoordinates();
		}

		public String getCoordinates() { return getCoordinates_Planet(); }
		public String getCoordinates_Planet     () { return String.format("%d | %d,%d,%d | %d | %d", galaxyIndex, voxelX, voxelY, voxelZ, solarSystemIndex, planetIndex); }
		public String getCoordinates_SolarSystem() { return String.format("%d | %d,%d,%d | %d"     , galaxyIndex, voxelX, voxelY, voxelZ, solarSystemIndex); }
		public String getCoordinates_Region     () { return String.format("%d | %d,%d,%d"          , galaxyIndex, voxelX, voxelY, voxelZ); }

		public String getReducedSigBoostCode() { return String.format("%04X:%04X:%04X", (voxelX+2047)&0xFFFF, (voxelY+127)&0xFFFF, (voxelZ+2047)&0xFFFF); }
		public String getSigBoostCode       () { return String.format(            "%s:%04X", getReducedSigBoostCode(), solarSystemIndex&0xFFFF); }

		public String getExtendedSigBoostCode() { return getExtendedSigBoostCode_Planet(); }
		public String getExtendedSigBoostCode_Planet     () { return String.format("G%d|%s|P%d", galaxyIndex, getSigBoostCode(), planetIndex); }
		public String getExtendedSigBoostCode_SolarSystem() { return String.format("G%d|%s", galaxyIndex, getSigBoostCode()); }
		public String getExtendedSigBoostCode_Region     () { return String.format("G%d|%s", galaxyIndex, getReducedSigBoostCode()); }

		public double getDistToCenter_inRegionUnits() {
			return Math.sqrt(voxelX*voxelX+voxelY*voxelY+voxelZ*voxelZ);
		}

		public double getDistToOther_inRegionUnits(UniverseAddress other) {
			return Math.sqrt(
					(voxelX-other.voxelX)*(voxelX-other.voxelX)+
					(voxelY-other.voxelY)*(voxelY-other.voxelY)+
					(voxelZ-other.voxelZ)*(voxelZ-other.voxelZ) );
		}

		@Override
		public String toString() {
			return "UniverseAddress [\r\n"+
					"\tgalacticIndex=" + galaxyIndex + ",\r\n"+
					"\tvoxel=("+voxelX+", "+voxelY+", "+voxelZ+"),\r\n"+
					"\tsolarSystemIndex=" + solarSystemIndex + ",\r\n"+
					"\tplanetIndex=" + planetIndex + "\r\n"+
					"]";
		}
		
		
	}
	
	public static class UniverseObject {
		public enum Type { Universe, Galaxy, Region, SolarSystem, Planet }
		
		public final Type type;
		public Object guiComp;
		
		protected UniverseObject(Type type) {
			this.type = type;
			this.guiComp = null;
		}
	}
	
	public static final class Universe extends UniverseObject {

		public final Vector<Galaxy> galaxies;
		
		Universe() {
			super(Type.Universe);
			galaxies = new Vector<>();
		}
		
		@Override
		public String toString() {
			return "Universe";
		}

		public void sort() {
			galaxies.sort(Comparator.comparing(g -> g.galaxyIndex));
			for (Galaxy g:galaxies) {
				g.regions.sort(
						Comparator
						.comparing((Region r) -> -r.distToCenter_RU)
						.thenComparing((Region r) -> r.voxelX)
						.thenComparing((Region r) -> r.voxelY)
						.thenComparing((Region r) -> r.voxelZ)
					);
				for (Region r:g.regions) {
					r.solarSystems.sort(Comparator.comparing(s -> s.solarSystemIndex));
					for (SolarSystem s:r.solarSystems) {
						s.planets.sort(Comparator.comparing(p -> p.planetIndex));
					}
				}
			}
		}

		public void writeToConsole() {
			SaveViewer.log_ln("Universe:");
			for (Galaxy g:galaxies) {
				SaveViewer.log_ln("\t"+g+":");
				for (Region r:g.regions) {
					SaveViewer.log_ln("\t\t"+r+":");
					for (SolarSystem s:r.solarSystems) {
						SaveViewer.log_ln("\t\t\t"+s+":");
						for (Planet p:s.planets)
							SaveViewer.log_ln("\t\t\t\tPlanet "+p);
					}
				}
			}
		}

		public Planet findPlanet(UniverseAddress ua) {
			SolarSystem solarSystem = findSolarSystem(ua);
			if (solarSystem==null) return null;
			
			return solarSystem.findPlanet(ua.planetIndex);
		}

		public SolarSystem findSolarSystem(UniverseAddress ua) {
			Region region = findRegion(ua);
			if (region==null) return null;
			
			return region.findSolarSystem(ua.solarSystemIndex);
		}

		public Region findRegion(UniverseAddress ua) {
			Galaxy galaxy = findGalaxy(ua.galaxyIndex);
			if (galaxy==null) return null;
			
			return galaxy.findRegion(ua.voxelX,ua.voxelY,ua.voxelZ);
		}

		public Galaxy findGalaxy(UniverseAddress ua) {
			return findGalaxy(ua.galaxyIndex);
		}

		public Galaxy findGalaxy(int galaxyIndex) {
			for (Galaxy g:galaxies)
				if (g.galaxyIndex==galaxyIndex)
					return g;
			return null;
		}

		public ObjectWithSource getOrCreate(UniverseAddress ua) {
			if (ua.isPlanet     ()) return getOrCreatePlanet     (ua);
			if (ua.isSolarSystem()) return getOrCreateSolarSystem(ua);
			if (ua.isRegion     ()) return getOrCreateRegion     (ua);
			return getOrCreateGalaxy(ua);
		}

		public Planet getOrCreatePlanet(long address) {
			return getOrCreatePlanet(new UniverseAddress(address));
		}

		public Planet getOrCreatePlanet(UniverseAddress ua) {
			SolarSystem solarSystem = getOrCreateSolarSystem(ua);
			
			Planet planet = solarSystem.findPlanet(ua.planetIndex);
			if (planet==null) solarSystem.addPlanet(planet=new Planet(solarSystem,ua.planetIndex));
			
			return planet;
		}

		public SolarSystem getOrCreateSolarSystem(UniverseAddress ua) {
			Region region = getOrCreateRegion(ua);
			
			SolarSystem solarSystem = region.findSolarSystem(ua.solarSystemIndex);
			if (solarSystem==null) region.addSolarSystem(solarSystem=new SolarSystem(region,ua.solarSystemIndex));
			
			return solarSystem;
		}

		public Region getOrCreateRegion(UniverseAddress ua) {
			Galaxy galaxy = getOrCreateGalaxy(ua);
			
			Region region = galaxy.findRegion(ua.voxelX,ua.voxelY,ua.voxelZ);
			if (region==null) galaxy.addRegion(region=new Region(galaxy,ua.voxelX,ua.voxelY,ua.voxelZ));
			
			return region;
		}

		private Galaxy getOrCreateGalaxy(UniverseAddress ua) {
			Galaxy galaxy = findGalaxy(ua.galaxyIndex);
			if (galaxy==null) galaxies.add(galaxy=new Galaxy(this,ua.galaxyIndex));
			
			return galaxy;
		}

		public static class ObjectWithSource extends UniverseObject {
			
			public boolean isCurrPos;
			public boolean foundInStats;
			public HashSet<Integer> foundInDiscStore;
			public HashSet<Integer> foundInDiscAvail;
			public HashSet<Integer> foundInPersistentPlayerBases;
			public HashSet<Integer> foundInBaseBuildingObjects;
			public HashSet<Integer> foundInTeleportEndpoints;
			
			protected ObjectWithSource(Type type) {
				super(type);
				isCurrPos        = false;
				foundInStats     = false;
				foundInDiscStore = new HashSet<>();
				foundInDiscAvail = new HashSet<>();
				foundInPersistentPlayerBases = new HashSet<>();
				foundInBaseBuildingObjects = new HashSet<>();
				foundInTeleportEndpoints = new HashSet<>();
			}
			
			public boolean isNotUploaded() {
				return !foundInDiscAvail.isEmpty();
			}
			
			public boolean hasSourceID() {
				return isCurrPos ||
						foundInStats ||
						!foundInDiscAvail            .isEmpty() ||
						!foundInDiscStore            .isEmpty() ||
						!foundInPersistentPlayerBases.isEmpty() ||
						!foundInBaseBuildingObjects  .isEmpty() ||
						!foundInTeleportEndpoints    .isEmpty()    
						;
			}
			
			public String getSourceIDStr() {
				StringBuilder sb = new StringBuilder();
				if (isCurrPos                              ) {                                    sb.append("CP"); }
				if (foundInStats                           ) { if (sb.length()>0) sb.append('|'); sb.append("St"); }
				if (!foundInDiscStore            .isEmpty()) { if (sb.length()>0) sb.append('|'); sb.append("DS"); }
				if (!foundInDiscAvail            .isEmpty()) { if (sb.length()>0) sb.append('|'); sb.append("DA"); }
				if (!foundInPersistentPlayerBases.isEmpty()) { if (sb.length()>0) sb.append('|'); sb.append("PB"); }
				if (!foundInBaseBuildingObjects  .isEmpty()) { if (sb.length()>0) sb.append('|'); sb.append("BBO"); }
				if (!foundInTeleportEndpoints    .isEmpty()) { if (sb.length()>0) sb.append('|'); sb.append("TE"); }
				return "<"+sb.toString()+">";
			}
			
			public String getLongSourceIDStr() {
				StringBuilder sb = new StringBuilder();
				if (isCurrPos                              ) {                                     sb.append("Current Position"); }
				if (foundInStats                           ) { if (sb.length()>0) sb.append(", "); sb.append("Status Values"); }
				if (!foundInDiscStore            .isEmpty()) { if (sb.length()>0) sb.append(", "); sb.append("Discov.-Store"+toString(foundInDiscStore)); }
				if (!foundInDiscAvail            .isEmpty()) { if (sb.length()>0) sb.append(", "); sb.append("Discov.-Avail."+toString(foundInDiscAvail)); }
				if (!foundInPersistentPlayerBases.isEmpty()) { if (sb.length()>0) sb.append(", "); sb.append("PlayerBase"+toString(foundInPersistentPlayerBases)); }
				if (!foundInBaseBuildingObjects  .isEmpty()) { if (sb.length()>0) sb.append(", "); sb.append("BaseBuildingObject("+toString(foundInBaseBuildingObjects)); }
				if (!foundInTeleportEndpoints    .isEmpty()) { if (sb.length()>0) sb.append(", "); sb.append("TeleportEndpoint"+toString(foundInTeleportEndpoints)); }
				return "<"+sb.toString()+">";
			}

			private String toString(HashSet<Integer> intSet) {
				String str="";
				for (int i: intSet) {
					if (!str.isEmpty()) str+=",";
					str+=(i+1);
				}
				return "("+str+")";
			}
		}

		public static final class Galaxy extends ObjectWithSource {
			private final static String[] PREDEFINED_NAMES_EN = {"Euclid","Hilbert Dimension","Calypso","Hesperius Dimension","Hyades","Ickjamatew","Budullangr","Kikolgallr","Eltiensleen","Eissentam","Elkupalos","Aptarkaba","Ontiniangp","Odiwagiri","Ogtialabi","Muhacksonto","Hitonskyer","Rerasmutul","Isdoraijung","Doctinawyra","Loychazinq","Zukasizawa","Ekwathore","Yeberhahne","Twerbetek","Sivarates","Eajerandal","Aldukesci","Wotyarogii","Sudzerbal","Maupenzhay","Sugueziume","Brogoweldian","Ehbogdenbu","Ijsenufryos","Nipikulha","Autsurabin","Lusontrygiamh","Rewmanawa","Ethiophodhe","Urastrykle","Xobeurindj","Oniijialdu","Wucetosucc","Ebyeloofdud","Odyavanta","Milekistri","Waferganh","Agnusopwit","Teyaypilny"}; 
			@SuppressWarnings("unused")
			private final static String[] PREDEFINED_NAMES_DE = {"Euklid","Hilbert Dimension","Calypso","Hesperius Dimension","Hyades","Ickjamatew","Budullangr","Kikolgallr","Eltiensleen","Eissentam","Elkupalos","Aptarkaba","Ontiniangp","Odiwagiri","Ogtialabi","Muhacksonto","Hitonskyer","Rerasmutul","Isdoraijung","Doctinawyra","Loychazinq","Zukasizawa","Ekwathore","Yeberhahne","Twerbetek","Sivarates","Eajerandal","Aldukesci","Wotyarogii","Sudzerbal","Maupenzhay","Sugueziume","Brogoweldian","Ehbogdenbu","Ijsenufryos","Nipikulha","Autsurabin","Lusontrygiamh","Rewmanawa","Ethiophodhe","Urastrykle","Xobeurindj","Oniijialdu","Wucetosucc","Ebyeloofdud","Odyavanta","Milekistri","Waferganh","Agnusopwit","Teyaypilny"}; 
			
			final Universe universe;
			public final int galaxyIndex;
			public final Vector<Region> regions;

			public Galaxy(Universe universe, int galacticIndex) {
				super(Type.Galaxy);
				this.universe = universe;
				this.galaxyIndex = galacticIndex;
				this.regions = new Vector<>();
			}

			@Override
			public String toString() {
				if (galaxyIndex<PREDEFINED_NAMES_EN.length)
					return "Galaxy \""+PREDEFINED_NAMES_EN[galaxyIndex]+"\"";
				return "Galaxy "+galaxyIndex;
			}

			public void addRegion(Region galacticRegion) {
				regions.add(galacticRegion);
			}

			public Region findRegion(int voxelX, int voxelY, int voxelZ) {
				for (Region r:regions)
					if (r.voxelX==voxelX && r.voxelY==voxelY && r.voxelZ==voxelZ)
						return r;
				return null;
			}
		}
		
		public static final class Region extends ObjectWithSource {
			
			final Galaxy galaxy;
			public final int voxelX;
			private final int voxelY;
			public final int voxelZ;
			public final Vector<SolarSystem> solarSystems;
			public String oldname;
			public String name;
			public double distToCenter_RU;
			
			public Region(Galaxy galaxy, int x, int y, int z) {
				super(Type.Region);
				this.galaxy = galaxy;
				this.voxelX = x;
				this.voxelY = y;
				this.voxelZ = z;
				this.solarSystems = new Vector<>();
				this.setName(null);
				this.distToCenter_RU = getUniverseAddress().getDistToCenter_inRegionUnits();
			}

			@Override
			public String toString() {
				if (name==null && oldname==null) return "Region "+voxelX+","+voxelY+","+voxelZ;
				return "Region "+(name==null?("["+oldname+"]"):("\""+name+"\""))+"  ("+voxelX+","+voxelY+","+voxelZ+")";
			}

			public void    setOldName(String name) { this.oldname = (name==null||name.isEmpty())?null:name; }
			public void    setName   (String name) { this.   name = (name==null||name.isEmpty())?null:name; }
			public String  getOldName() { return oldname==null?"":oldname; }
			public String  getName   () { return    name==null?"":   name; }
			public boolean hasOldName() { return oldname!=null; }
			public boolean hasName   () { return    name!=null; }

			public void addSolarSystem(SolarSystem solarSystem) {
				solarSystems.add(solarSystem);
			}

			public SolarSystem findSolarSystem(int solarSystemIndex) {
				for (SolarSystem sys:solarSystems)
					if (sys.solarSystemIndex==solarSystemIndex)
						return sys;
				return null;
			}

			public UniverseAddress getUniverseAddress() {
				if (galaxy==null) return null;
				return new UniverseAddress( galaxy.galaxyIndex, voxelX,voxelY,voxelZ, 0,0 );
			}

			public boolean isReachableByTeleport() {
				for (SolarSystem sys:solarSystems) {
					if (sys.additionalInfos.hasTeleportEndPoint)
						return true;
					for (Planet planet:sys.planets)
						if (planet.additionalInfos.hasTeleportEndPoint)
							return true;
				}
				return false;
			}
		}
		
		public static class DiscoverableObject extends ObjectWithSource {
			
			private String oldOriginalName; // defined by universe generator before NEXT update
			private String originalName; // defined by universe generator
			private String uploadedName; // defined by uploading player  
			
			private String discoverer;
			
			public final Vector<ExtraInfo> extraInfos;
			
			public final HashMap<String,Integer> discoveredItems_Avail;
			public final HashMap<String,Integer> discoveredItems_Store;
			
			protected DiscoverableObject(Type type) {
				super(type);
				
				oldOriginalName = null;
				originalName = null;
				uploadedName = null;
				
				discoverer = null;
				
				this.extraInfos = new Vector<>();
				
				discoveredItems_Avail = new HashMap<>();
				discoveredItems_Store = new HashMap<>();
			}

			protected String getCombinedExtraInfoLabels(String...extraStrs) {
				StringBuilder sb = new StringBuilder();
				for (ExtraInfo ei:extraInfos)
					if (!ei.shortLabel.isEmpty()) {
						if (sb.length()>0) sb.append(", ");
						sb.append(ei.shortLabel);
					}
				for (String str:extraStrs)
					if (str!=null && !str.isEmpty()) {
						if (sb.length()>0) sb.append(", ");
						sb.append(str);
					}
				return sb.toString();
			}

			public void addDiscoveredItem(String itemLabel, DiscoveryData.SourceArray sourceArray) {
				HashMap<String, Integer> map = null;
				switch (sourceArray) {
				case AvailableData: map = discoveredItems_Avail; break;
				case StoreData    : map = discoveredItems_Store; break;
				}
				Integer value = map.get(itemLabel);
				if (value==null) map.put(itemLabel, 1);
				else             map.put(itemLabel, value+1);
			}

			
			public boolean hasDiscoverer() { return discoverer!=null; }
			public String getDiscoverer() { return discoverer; }
			public void setDiscoverer(String name) { this.discoverer = name; }

			public boolean hasOldOriginalName() { return oldOriginalName!=null; }
			public boolean hasOriginalName   () { return    originalName!=null; }
			public boolean hasUploadedName   () { return    uploadedName!=null; }
			public void setOldOriginalName(String name) { this.oldOriginalName = name; }
			public void setOriginalName   (String name) { this.   originalName = name; }
			public void setUploadedName   (String name) { this.   uploadedName = name; }
			public String getOldOriginalName() { return oldOriginalName; }
			public String getOriginalName   () { return    originalName; }
			public String getUploadedName   () { return    uploadedName; }
			
			public static final class ExtraInfo {
				public boolean showInParent;
				public String shortLabel;
				public String info;
				public ExtraInfo(String shortLabel, String info) {
					this(false,shortLabel,info);
				}
				public ExtraInfo(boolean showInParent, String shortLabel, String info) {
					this.showInParent = showInParent;
					this.shortLabel = shortLabel;
					this.info = info;
				}
				public ExtraInfo(ExtraInfo ei) {
					this(ei.showInParent,ei.shortLabel,ei.info);
				}
			}
		}
		
		public static final class SolarSystem extends DiscoverableObject {
			
			public enum StarClass {
				Yellow("G","F"), Red("K"), Green, Blue("B");
				
				private String[] letters;
				StarClass(String... letters) {
					this.letters = letters;
				}
				public String getLabel() {
					return toString()+" Class"+(letters.length==0?"":" "+Arrays.toString(letters));
				}
			}
			
			public enum Race {
				Gek("Gek"), Korvax("Korvax"), Vykeen("Vy'keen");

				public final String fullName;
				private Race(String fullName) { this.fullName = fullName; }
			}
			
			public static class AdditionalInfos {
				public boolean hasFreighter;
				public boolean hasTeleportEndPoint;
				public AdditionalInfos() {
					this.hasFreighter = false;
					this.hasTeleportEndPoint = false;
				}
				public boolean isEmpty() {
					return !hasFreighter && !hasTeleportEndPoint;
				}
			}
			
			final Region region;
			final int solarSystemIndex;
			public final Vector<Planet> planets;
			
			public Race race;
			public StarClass starClass;
			public int conflictLevel;
			public String conflictLevelLabel;
			public boolean isUnexplored; 
			public Double distanceToCenter;
			public boolean hasAtlasInterface; 
			public boolean hasBlackHole; 
			public UniverseAddress blackHoleTarget;
			
			public AdditionalInfos additionalInfos;
			
			public SolarSystem(Region region, int solarSystemIndex) {
				super(Type.SolarSystem);
				this.region = region;
				this.solarSystemIndex = solarSystemIndex;
				this.planets = new Vector<>();
				this.race = null;
				this.starClass = null;
				this.conflictLevel = -1;
				this.isUnexplored = false;
				this.distanceToCenter = null;
				this.hasAtlasInterface = shouldHaveAtlasInterface(); 
				this.hasBlackHole = shouldHaveBlackHole();
				this.blackHoleTarget = null;
				
				this.additionalInfos = new AdditionalInfos();
			}

			public boolean shouldHaveBlackHole     () { return shouldHaveBlackHole     (solarSystemIndex); }
			public boolean shouldHaveAtlasInterface() { return shouldHaveAtlasInterface(solarSystemIndex); }
			public static boolean shouldHaveBlackHole     (int solarSystemIndex) { return solarSystemIndex == 0x79; }
			public static boolean shouldHaveAtlasInterface(int solarSystemIndex) { return solarSystemIndex == 0x7A; }

			@Override
			public String toString() {
				return toString(true,true,true,true);
			}

			public String toString(boolean withName, boolean withExtraInfo, boolean withDataName, boolean withRace) {
				String strName;
				if      (   hasOriginalName()) strName = String.format("Sys%03X %s", solarSystemIndex, getOriginalName());
				else if (hasOldOriginalName()) strName = String.format("Sys%03X [%s]", solarSystemIndex, getOldOriginalName());
				else                           strName = String.format("SolarSystem %03X (%d)", solarSystemIndex, solarSystemIndex);
				
				String strDataName = (!hasUploadedName()?"":(" | "+getUploadedName()));
				
				String strRace = (race==null)?"":(" ["+race.fullName+"]");
				if (isUnexplored) strRace = " <Unexplored>";
				
				HashSet<String> foundLabels = new HashSet<>();
				for (Planet p:planets) {
					for (ExtraInfo ei:p.extraInfos)
						if (ei.showInParent && !ei.shortLabel.isEmpty())
							foundLabels.add(ei.shortLabel);
					if (p.withWater)
						foundLabels.add("<Water>");
				}
				
				String strExtraInfo = getCombinedExtraInfoLabels(
					(hasAtlasInterface?"<Atlas>":null),
					(hasBlackHole?"<BlackHole>":null)
				);
				for (String str:foundLabels) {
					if (!strExtraInfo.isEmpty()) strExtraInfo+=", ";
					strExtraInfo+=str;
				}
				if (!strExtraInfo.isEmpty()) strExtraInfo=" ("+strExtraInfo+")";
				
				return (withName?strName:"")+(withExtraInfo?strExtraInfo:"")+(withDataName?strDataName:"")+(withRace?strRace:"");
			}

			public void addPlanet(Planet planet) {
				planets.add(planet);
			}

			public Planet findPlanet(int planetIndex) {
				for (Planet p:planets)
					if (p.planetIndex==planetIndex)
						return p;
				return null;
			}

			public UniverseAddress getUniverseAddress() {
				if (region==null) return null;
				UniverseAddress ua = region.getUniverseAddress();
				if (ua==null) return null;
				return new UniverseAddress(ua,solarSystemIndex,0);
			}
		}
		
		public static final class Planet extends DiscoverableObject {
			
			public enum Biome {
				Lush       ("Lush"         ,"Erdähnlich"),
				Barren     ("Barren"       ,"Trocken"),
				Scorched   ("Scorched"     ,"heiß"),
				Frozen     ("Frozen"       ,"Gefroren"),
				Toxic      ("Toxic"        ,"Giftig"),
				Irradiated ("Irradiated"   ,"Verstrahlt"),
				Airless    ("Airless"      ,"Trostlos, ohne Atmosphäre"),
				Exotic     ("Exotic"       ,"Exotisch"),
				Exotic_Mega("Exotic (Mega)","Exotisch, mit Riesenpflanzen"),
				
				AnomMetalFlowers  ("Anomalous (Metal Flowers)"     ,"Anomal (Metall-Blumen)"),
				AnomShells        ("Anomalous (Shells)"            ,"Anomal (Muscheln)"),
				AnomBones         ("Anomalous (Bones)"             ,"Anomal (Knochen)"),
				AnomMushrooms     ("Anomalous (Mushrooms)"         ,"Anomal (Pilze)"),
				AnomScreenCrystals("Anomalous (Screen Crystals)"   ,"Anomal (Bildschirm-Kristalle)"),
				AnomFragmColumns  ("Anomalous (Fragmented Columns)","Anomal (Zersplitterte Säulen)"),
				AnomBubbles       ("Anomalous (Bubbles)"           ,"Anomal (Seifenblasen)"),
				AnomLimeStars     ("Anomalous (Lime Stars)"        ,"Anomal (Kalk-Sterne)"),
				AnomHexagons      ("Anomalous (Hexagons)"          ,"Anomal (Sechsecke)"),
				AnomBeams         ("Anomalous (Beams)"             ,"Anomal (Beams)"),
				AnomContour       ("Anomalous (Contour)"           ,"Anomal (Kontur)"),
				;

				public final String name_EN;
				public final String name_DE;
				private Biome(String name_EN, String name_DE) {
					this.name_EN = name_EN;
					this.name_DE = name_DE;
				}
			}
			
			public enum BuriedTreasure {
				AncientBones    ("Ancient Bones"    ,"Uralte Knochen"),
				SalvageableScrap("Salvageable Scrap","Wiederverwertbarer Schrott"),
				;

				public final String name_EN;
				public final String name_DE;
				private BuriedTreasure(String name_EN, String name_DE) {
					this.name_EN = name_EN;
					this.name_DE = name_DE;
				}
			}
			
			public enum Resources {
				Cu_    ("#Cu", GameInfos.substanceIDs, "^EX_YELLOW"),
				Cd_    ("#Cd", GameInfos.substanceIDs, "^EX_RED"),
				Em_    ("#Em", GameInfos.substanceIDs, "^EX_GREEN"),
				In_    ("#In", GameInfos.substanceIDs, "^EX_BLUE"),
				Cu     ("Cu" , GameInfos.substanceIDs, "^YELLOW2"),
				Cd     ("Cd" , GameInfos.substanceIDs, "^RED2"),
				Em     ("Em" , GameInfos.substanceIDs, "^GREEN2"),
				In     ("In" , GameInfos.substanceIDs, "^BLUE2"),
				
				NH3    ("NH3", GameInfos.substanceIDs, "^TOXIC1"),
				U      ("U"  , GameInfos.substanceIDs, "^RADIO1"),
				P      ("P"  , GameInfos.substanceIDs, "^HOT1"),
				Pf     ("Pf" , GameInfos.substanceIDs, "^LUSH1"),
				Py     ("Py" , GameInfos.substanceIDs, "^DUSTY1"),
				CO2    ("CO2", GameInfos.substanceIDs, "^COLD1"),
				
				Ag     ("Ag"     , GameInfos.substanceIDs, "^ASTEROID1"),
				Au     ("Au"     , GameInfos.substanceIDs, "^ASTEROID2"),
				Co     ("Co"     , GameInfos.substanceIDs, "^CAVE1"),
				Fe_    ("Fe#"    , GameInfos.substanceIDs, "^LAND2"),
				Fe__   ("Fe##"   , GameInfos.substanceIDs, "^LAND3"),
				Na     ("Na"     , GameInfos.substanceIDs, "^CATALYST1"),
				NaCl   ("NaCl"   , GameInfos.substanceIDs, "^WATER1"),
				Sr_Rost("Sr_Rost", GameInfos.substanceIDs, "^SPACEGUNK3"),
				Eiweißperle("Eiweißperle", GameInfos.productIDs, "^ALBUMENPEARL"),
				;
				public final String label;
				public final String ID;
				private IDMap idMap;
				
				Resources(String label, IDMap idMap, String ID) {
					this.label = label;
					this.idMap = idMap;
					this.ID = ID;
				}
				public static Iterable<String> getStringIterable(Collection<Resources> resources) {
					return getStringIterable(resources, Resources::toString);
				}
				public static Iterable<String> getStringIterable(Collection<Resources> resources, Function<Resources,String> convert) {
					return () -> resources.stream().sorted().map(convert).iterator();
				}
				public static Resources getViaLabel(String label) {
					for (Resources res:values())
						if (res.label.equals(label))
							return res;
					return null;
				}
				public GeneralizedID getGeneralizedID() {
//					GeneralizedID id = null;
//					if (id==null) id = GameInfos.productIDs  .getIfContains(ID);
//					if (id==null) id = GameInfos.substanceIDs.getIfContains(ID);
//					return id;
					return idMap.getIfContains(ID);
				}
				public String getShortLabel() {
					return label;
				}
				public String getLongLabel() {
					GeneralizedID id = getGeneralizedID();
					if (id!=null) {
						if (id.hasLabel ()) return id.getLabel ();
					}
					return label;
				}
			}
			
			public static class AdditionalInfos {
				public Vector<PersistentPlayerBase> bases;
				public boolean hasExocraftSummoningStation;
				public boolean hasTeleportEndPoint;
				public AdditionalInfos() {
					this.bases = new Vector<>();
					this.hasExocraftSummoningStation = false;
					this.hasTeleportEndPoint = false;
				}
				public boolean isEmpty() {
					return bases.isEmpty() && !hasExocraftSummoningStation && !hasTeleportEndPoint;
				}
			}
			
			final SolarSystem solarSystem;
			final int planetIndex;
			private Stats.PlanetStats stats;
			public Biome biome;
			public boolean hasExtremeBiome;
			public boolean areSentinelsAggressive;
			public boolean withWater;
			public boolean withGravitinoBalls;
			public BuriedTreasure buriedTreasure;
			public AdditionalInfos additionalInfos;
			public EnumSet<Resources> resources;
			
			public Planet(SolarSystem solarSystem, int planetIndex) {
				super(Type.Planet);
				this.solarSystem = solarSystem;
				this.planetIndex = planetIndex;
				this.biome = null;
				this.hasExtremeBiome = false;
				this.areSentinelsAggressive = false;
				this.withWater = false;
				this.withGravitinoBalls = false;
				this.buriedTreasure = null;
				this.additionalInfos = new AdditionalInfos();
				this.resources = EnumSet.noneOf(Resources.class);
			}
			public void setPlanetStats(Stats.PlanetStats stats) {
				this.stats = stats;
			}

			@Override
			public String toString() {
				String strName;
				if (hasOriginalName())
					strName = String.format("P%1X %s", planetIndex, getOriginalName());
				else if (hasOldOriginalName())
					strName = String.format("P%1X [%s]", planetIndex, getOldOriginalName());
				else {
					UniverseAddress ua = getUniverseAddress();
					if (ua!=null)
						strName = ua.getExtendedSigBoostCode();
					else
						return "Planet [planetIndex=" + planetIndex + ", solarSystem=" + solarSystem + ", stats=" + stats + "]";
				}
				
				String strDataName = (!hasUploadedName()?"":(" | "+getUploadedName()));
				
				String strExtraInfo="";
				if (!extraInfos.isEmpty()) {
					strExtraInfo = getCombinedExtraInfoLabels(
						(hasExtremeBiome?"<Extr>":null),
						(withWater?"<Water>":null),
						(withGravitinoBalls?"<Grav>":null),
						(buriedTreasure==null?null:"<"+buriedTreasure.name_EN+">")
					);
					if (!strExtraInfo.isEmpty()) strExtraInfo = " ("+strExtraInfo+")";
				}
				
				return strName+strExtraInfo+strDataName;
			}

			public UniverseAddress getUniverseAddress() {
				if (solarSystem==null) return null;
				UniverseAddress ua = solarSystem.getUniverseAddress();
				if (ua==null) return null;
				return new UniverseAddress(ua,planetIndex);
			}
		}
	}

	private KnownWords parseKnownWords(String arrLabel, String wordLabel, String racesLabel) {
		JSON_Array arrayValue = getArrayValue(json_data,"PlayerStateData",arrLabel);
		KnownWords array;
		if (arrayValue==null)
			array = null;
		else {
			array = new KnownWords().parse(arrayValue, wordLabel, racesLabel);
			if (!array.notParsedKnownWords.isEmpty())
				SaveViewer.log_error_ln("Found "+array.notParsedKnownWords.size()+" not parseable KnownWords.");
		}
		return array; 
	}

	public static final class KnownWords {

		public Vector<KnownWord> wordList;
		JSON_Array notParsedKnownWords;
		public int[] wordCounts;

		public KnownWords() {
			this.wordList = new Vector<>();
			this.wordCounts = null;
			notParsedKnownWords = new JSON_Array();
		}
	
		public KnownWords parse(JSON_Array knownWordsArray, String wordLabel, String racesLabel) {
			for (Value knownWordValue : knownWordsArray) {
				JSON_Object knownWordObj = getObject(knownWordValue);
				if (knownWordObj==null) { notParsedKnownWords.add(knownWordValue); continue; }
				
				KnownWord knownWord = new KnownWord();
				
				knownWord.word = getStringValue(knownWordObj,wordLabel);
				if (knownWord.word==null) { notParsedKnownWords.add(knownWordValue); continue; }
				
				JSON_Array races = getArrayValue(knownWordObj,racesLabel);
				if (races==null) { notParsedKnownWords.add(knownWordValue); continue; }
				knownWord.races = new boolean[races.size()];
				
				boolean errorOccured = false;
				for (int i=0; i<knownWord.races.length; ++i) {
					Boolean race = getBool(races.get(i));
					if (race==null) { errorOccured=true; break; }
					knownWord.races[i] = race;
				}
				if (errorOccured) { notParsedKnownWords.add(knownWordValue); continue; }
				
				wordList.add(knownWord);
			}
			wordList.sort(null);
			
			int numberOfRaces = 0;
			for (KnownWord word:wordList)
				numberOfRaces = Math.max(numberOfRaces, word.races.length);
			
			wordCounts = new int[numberOfRaces];
			for (int i=0; i<numberOfRaces; ++i) wordCounts[i] = 0;
			
			for (KnownWord word:wordList)
				for (int i=0; i<word.races.length; ++i)
					if (word.races[i]) ++wordCounts[i];
			
			return this;
		}
		
		
		public static class KnownWord implements Comparable<KnownWord>{
			
			public String word;
			public boolean[] races;
			
			@Override
			public int compareTo(KnownWord o) {
				return this.word.compareTo(o.word);
			}
		}
	}
	
	private void parseStats() {
		JSON_Array arrayValue = getArrayValue(json_data,"PlayerStateData","Stats");
		if (arrayValue==null) { stats = null; return; }
		stats = new Stats(this);
		stats.parse(arrayValue);
		if (!stats.notParsedStats.isEmpty())
			SaveViewer.log_error_ln("Found "+stats.notParsedStats.size()+" not parseable Stats.");
	}

	public final static class Stats {
		
		public Vector<StatValue> globalStats;
		public Vector<PlanetStats> planetStats;
		public Vector<OtherStats> otherStats;
		JSON_Array notParsedStats;
		private final SaveGameData data;

		public Stats(SaveGameData data) {
			this.data = data;
			globalStats = null;
			planetStats = new Vector<>();
			otherStats = new Vector<>();
			notParsedStats = new JSON_Array();
		}

		public void parse(JSON_Array statList) {
			for (Value groupValue : statList) {
				JSON_Object group = getObject(groupValue);
				if (group==null) { notParsedStats.add(groupValue); continue; }
				
				String groupID = getStringValue(group,"GroupId");
				if (groupID==null) { notParsedStats.add(groupValue); continue; }
				
				JSON_Array groupStats = getArrayValue(group,"Stats");
				if (groupStats==null) { notParsedStats.add(groupValue); continue; }
				
				switch(groupID) {
				case "^GLOBAL_STATS":
					if (globalStats!=null) { notParsedStats.add(groupValue); continue; }
					
					getIntegerValue(group,"Address"); // -> wasProcessed
					globalStats = new Vector<>();
					fillInto(groupStats,globalStats);
					
					break;
					
				case "^PLANET_STATS": {
					Value addressValue = group.getValue("Address");
					if (addressValue==null) { notParsedStats.add(groupValue); continue; }
					
					long addressLong = -1;
					switch(addressValue.type) {
					case String:
						if (!(addressValue instanceof StringValue)) { notParsedStats.add(groupValue); continue; }
						String addressStr = ((StringValue)addressValue).value;
						if (!isPlanetAddressOK(addressStr)) { notParsedStats.add(groupValue); continue; }
						addressValue.wasProcessed=true;
						addressLong = Long.parseLong(addressStr.substring(2), 16);
						break;
					case Integer:
						if (!(addressValue instanceof IntegerValue)) { notParsedStats.add(groupValue); continue; }
						addressValue.wasProcessed=true;
						addressLong = ((IntegerValue)addressValue).value;
						break;
					default:
						{ notParsedStats.add(groupValue); continue; }
					}
					
					
					Universe.Planet planet = data.universe.getOrCreatePlanet(addressLong);
					planet.foundInStats = true;
					
					PlanetStats ps = new PlanetStats(planet);
					fillInto(groupStats,ps.stats);
					
					planet.setPlanetStats(ps);
					
					planetStats.add(ps);
					
				} break;
					
				default: { 
					Value addressValue = group.getValue("Address");
					if (addressValue==null) { notParsedStats.add(groupValue); continue; }
					
					Long addressLong = parseHexFormatedNumber(addressValue);
					if (addressLong==null) { notParsedStats.add(groupValue); continue; }
					
					OtherStats stats = new OtherStats(groupID,addressLong);
					fillInto(groupStats,stats.stats);
					otherStats.add(stats);
				} break;
					//{ notParsedStats.add(groupValue); continue; }
				
				}
			}
		}

		private static void fillInto(JSON_Array stats, Vector<StatValue> statsVector) {
//			StatValue.KnownID[] knownIDs = StatValue.KnownID.values();
			for (Value value : stats) {
				JSON_Object statObject = getObject(value);
				if (statObject==null) continue;
				
				StatValue stat = new StatValue();
				
				stat.ID = getStringValue(statObject,"Id");
				if (stat.ID==null) continue;
				stat.knownID = StatValue.KnownID.findID(stat.ID);
//				for (int i=0; i<knownIDs.length; ++i)
//					if (stat.ID.equals("^"+knownIDs[i]))
//						stat.knownID = knownIDs[i];
				
				JSON_Object statValue = getObjectValue(statObject,"Value");
				if (statValue!=null) {
					stat.IntValue    = getIntegerValue_silent(statValue,"IntValue");
					stat.FloatValue  = getFloatValue_silent  (statValue,"FloatValue");
					stat.Denominator = getFloatValue_silent  (statValue,"Denominator");
				}
				
				stat.interpretValues();
				statsVector.add(stat);
			}
			
			statsVector.sort(null);
		}

		public static class StatValue implements Comparable<StatValue> {
			public enum KnownID {
				TIME,
				DEATHS("Number of deaths"),
				LONGEST_LIFE("Longest life"),
				LONGEST_LIFE_EX("Longest life in extrem environment"),
				TIMES_IN_SPACE("Number of times in space"),
				GLOBAL_MISSION, ATLAS_PATH, ATLAS_STORY,
				DIST_WALKED("Distance walked"),
				DIST_SWAM  ("Distance swam"),
				DIST_FLY   ("Distance flown"),
				DIST_SUB   ("Distance swam with a submarine"),
				ALIENS_MET ("Aliens met"),
				WORDS_LEARNT("Words learnt"),
				TRA_STANDING("Gek"    +" standing"), TSEEN_SYSTEMS("Gek"    +" systems seen"), TWORDS_LEARNT("Gek"    +" words learnt"), TDONE_MISSIONS("Gek"    +" missions done"), TRA_MET("Gek"    +" met"),
				EXP_STANDING("Korvax" +" standing"), ESEEN_SYSTEMS("Korvax" +" systems seen"), EWORDS_LEARNT("Korvax" +" words learnt"), EDONE_MISSIONS("Korvax" +" missions done"), EXP_MET("Korvax" +" met"),
				WAR_STANDING("Vy'keen"+" standing"), WSEEN_SYSTEMS("Vy'keen"+" systems seen"), WWORDS_LEARNT("Vy'keen"+" words learnt"), WDONE_MISSIONS("Vy'keen"+" missions done"), WAR_MET("Vy'keen"+" met"),
				TGUILD_STAND("Traders"  +" guild standing"), TGDONE_MISSIONS("Traders"  +" guild missions done"), MONEY("Units max. earned"), PLANTS_PLANTED("Plants planted"),
				EGUILD_STAND("Explorers"+" guild standing"), EGDONE_MISSIONS("Explorers"+" guild missions done"), DIST_WARP("Number of warps"), RARE_SCANNED("Rare creatures scanned"),
				WGUILD_STAND("Warriors" +" guild standing"), WGDONE_MISSIONS("Warriors" +" guild missions done"), SENTINEL_KILLS("Sentinels killed (all)"), ENEMIES_KILLED("Enemies killed"),
				DRONES_KILLED("Sentinel drones killed"), QUADS_KILLED("Sentinel quads killed"), WALKERS_KILLED("Sentinel walkers killed"),
				POLICE_KILLED("Sentinel ships killed"),
				PIRATES_KILLED("Pirates killed"),
				PREDS_KILLED("Predatory animals killed"),
				FLORA_KILLED,
				TRADERS_KILLED,
				FIENDS_KILLED,
				FIEND_EGG,
				CREATURES_KILL,
				DISC_ALL_CREATU("Planets, where all creatures were found"),
				DISC_FLORA    ("Discovered vegetables"),
				DISC_CREATURES("Discovered creatures"),
				DISC_MINERALS ("Discovered minerals"),
				DISC_PLANETS  ("Discovered planets"),
				DISC_WAYPOINTS("Discovered waypoints"),
				TUTORIAL,
				TECH_BOUGHT, SHIPS_BOUGHT,
				DEPOTS_BROKEN,
				FPODS_BROKEN,
				EARLY_WARPS,
				BLACKHOLE_WARPS("Warps through a blackhole"),
				ITEMS_TELEPRT,
				STATION_VISITED,
				SPACE_BATTLES,
				PHOTO_MODE_USED,
				ARTIFACT_HINTS,
				RES_EXTRACTED,
				SALVAGE_LOOTED,
				VISIT_EXT_BASE,
				APP_SESSIONS,
				COMM_04_FOUND,
				COMM_04_HANDED,
				COMM_05_FOUND,
				COMM_05_HANDED,
				COMM_06_CHOICE1,
				COMM_06_CHOICE2,
				COMM_06_CHOICE3,
				COMM_06_CHOICE4,
				COMM_06_FOUND,
				COMM_06_HANDED,
				COMM_06_STORY,
				COMM_07_FOUND,
				COMM_07_HANDED,
				COMM_CREA_FED,
				COMM_GLITCHLVL,
				COMM_GLITCHSTAG,
				COMM_INIT_FED,
				COMM_INIT_SENTK,
				COMM_SENT_KILL,
				CREATURES_FED,
				DIS_CREA_BANK,
				DIS_FLORA_BANK,
				DIS_MIN_BANK,
				DIS_PLANET_BANK,
				EMOTES,
				EXPEDITIONS,
				JM,
				JM_BANKED,
				MP_FULL_COUNT,
				MP_FULL_TIME,
				MP_ORB_COUNT,
				MP_ORB_TIME,
				MP_SESSIONS,
				NADA_PROGRESS,
				POLO_PROGRESS,
				PARTS_PLACED,
				PLAY_SESSIONS,
				REWARD_SEED,
				TREASURE_FOUND,
				
				
				// only in planet stats
				ALL_CREATURES;
				
				public String fullName;
				
				KnownID() {
					this(""); //toString();
				}
				KnownID(String name) {
					this.fullName = name;
				}
				public static KnownID findID(String id) {
					if (id.startsWith("^")) id = id.substring(1);
					try { return valueOf(id); }
					catch (Exception e) { return null; }
				}
			}
			
			public String ID;
			public KnownID knownID;
			public Long IntValue;
			public Double FloatValue;
			public Double Denominator;
			public String interpretedValue;
			
			
			public StatValue() {
				this.ID = null;
				this.knownID = null;
				this.IntValue = null;
				this.FloatValue = null;
				this.Denominator = null;
				this.interpretedValue = null;
			}


			public void interpretValues() {
				if (knownID==null) return;
				
				switch (knownID) {
				case TIME:
				case LONGEST_LIFE:
				case LONGEST_LIFE_EX:
					interpretedValue = Duration.toString(FloatValue==null?0:FloatValue);
					break;
					
				case DIST_FLY:
				case DIST_SUB:
				case DIST_SWAM:
				case DIST_WALKED:
					interpretedValue = String.format(Locale.ENGLISH, "%1.2f u", FloatValue==null?0:FloatValue);
					break;
					
				case EXP_STANDING:
				case TRA_STANDING:
				case WAR_STANDING:
					interpretedValue = String.format(Locale.ENGLISH, "%d of 100", IntValue==null?0:IntValue);
					break;
					
				
				default: break;
				}
				
			}


			@Override
			public int compareTo(StatValue other) {
				if (this.knownID==null) {
					if (other.knownID==null)
						return this.ID.compareTo(other.ID);
					return +1;
				}
				if (other.knownID==null)
					return -1;
				return this.knownID.ordinal() - other.knownID.ordinal();
			}
		}
		
		public static class OtherStats {
			
			public final String id;
			public final long address;
			public Vector<StatValue> stats;
			
			OtherStats(String id, long address) {
				this.id = id;
				this.address = address;
				this.stats = new Vector<>();
			}

			@Override
			public String toString() {
				return "GeneralStats [id=" + id + ", address=" + address + ", stats=...]";
			}
		}
		
		public static class PlanetStats {
			
			public final Universe.Planet planet;
			public Vector<StatValue> stats;
			
			PlanetStats(Universe.Planet planet) {
				this.planet = planet;
				this.stats = new Vector<>();
			}

			@Override
			public String toString() {
				return "PlanetStats [planet=" + planet + ", stats=...]";
			}
		}
	
	}
	
	public static boolean isPlanetAddressOK(String addressStr) {
		if (!addressStr.startsWith("0x")) return false;
		if (addressStr.length()>2+1+3+2+2+3+3) return false;
		for (int i=2; i<addressStr.length(); ++i) {
			char ch = addressStr.charAt(i);
			if ('0'<=ch && ch<='9') continue;
			if ('a'<=ch && ch<='f') continue;
			if ('A'<=ch && ch<='F') continue;
			return false;
		}
		return true;
	}

	public enum Error { NoError, UnexpectedType, PathIsNotSolvable, ValueIsNull, ArrayIndexOutOfBounds }

//	private static JSON_Array getArray(Value val)   { if (val==null || !(val instanceof ArrayValue  ) || val.type!=Type.Array  ) return null; val.wasProcessed=true; return ((ArrayValue  )val).value;}
	private static JSON_Object getObject(Value val) { if (val==null || !(val instanceof ObjectValue ) || val.type!=Type.Object ) return null; val.wasProcessed=true; return ((ObjectValue )val).value;}
	private static String getString(Value val)      { if (val==null || !(val instanceof StringValue ) || val.type!=Type.String ) return null; val.wasProcessed=true; return ((StringValue )val).value;}
	private static Boolean getBool(Value val)       { if (val==null || !(val instanceof BoolValue   ) || val.type!=Type.Bool   ) return null; val.wasProcessed=true; return ((BoolValue   )val).value;}
//	private static Long getInteger(Value val)       { if (val==null || !(val instanceof IntegerValue) || val.type!=Type.Integer) return null; val.wasProcessed=true; return ((IntegerValue)val).value;}
	private static Double getFloat(Value val)       { if (val==null || !(val instanceof FloatValue  ) || val.type!=Type.Float  ) return null; val.wasProcessed=true; return ((FloatValue  )val).value;}

	private static void enableStackTrace(boolean isStackTraceEnabled_) {
		isStackTraceEnabled = isStackTraceEnabled_;
	}

	static boolean hasValue(JSON_Object data, Object... path) {
		try {
			JSON_Data.getSubNode(data,path);
			return true;
		} catch (PathIsNotSolvableException e) {
			return false;
		}
	}

	private static Value getValue(JSON_Object data, Object... path) {
		Value value = null;
		if (path.length==0) throw new IllegalStateException("Calling getValue(JSON_Object data, Object... path) is not allowed with zero length path");
		try {
			value = JSON_Data.getSubNode(data,path);
			error = Error.NoError;
			errorMessage = "";
			if (value==null) {
				error = Error.ValueIsNull;
				errorMessage = String.format("Value is null. (path: %s)", Arrays.toString(path));
			}
		} catch (PathIsNotSolvableException e) {
			if (isStackTraceEnabled) e.printStackTrace();
			error = Error.PathIsNotSolvable;
			errorMessage = "PathIsNotSolvable: "+Arrays.toString(path);
		}
		return value;
	}

	private static Value getValue(JSON_Array arr, int index) {
		Value value = null;
		if (0<=index && index<arr.size()) {
			value = arr.get(index);
			error = Error.NoError;
			errorMessage = "";
			if (value==null) {
				error = Error.ValueIsNull;
				errorMessage = String.format("Value is null. (Array Index: %d)", index);
			}
		} else {
			error = Error.ArrayIndexOutOfBounds;
			errorMessage = String.format("Array Index (%d) is out of bounds [0..%d].", index, arr.size());
		}
		return value;
	}

	private static <V> V generic_convert(GetValueHelper<V> helper, Value value, Supplier<String> source) {
		if (value==null) return null;
		if (helper.isInstance(value)) {
			error = Error.NoError;
			errorMessage = "";
			value.wasProcessed = true;
			return helper.getValue(value);
		} else {
			error = Error.UnexpectedType;
			errorMessage = String.format("Value has not the expected type (%s). %s type found. %s", helper.getType(), value.type, source.get());
			return null;
		}
	}

	private static abstract class GetValueHelper<V> {
		private Value.Type type;
		public GetValueHelper(Value.Type type) { this.type = type;}
		public Value.Type getType() { return type; }
		public abstract boolean isInstance(Value value);
		public abstract V getValue(Value value);
		
		private static final class GVH_Object extends GetValueHelper<JSON_Object> {
			private GVH_Object() { super(Type.Object); }
			@Override public boolean     isInstance(Value value) { return value instanceof ObjectValue; }
			@Override public JSON_Object getValue  (Value value) { return ((ObjectValue)value).value; }
		}
		private static final class GVH_Array extends GetValueHelper<JSON_Array> {
			private GVH_Array() { super(Type.Array); }
			@Override public boolean    isInstance(Value value) { return value instanceof ArrayValue; }
			@Override public JSON_Array getValue  (Value value) { return ((ArrayValue)value).value; }
		}
		private static final class GVH_String extends GetValueHelper<String> {
			private GVH_String() { super(Type.String); }
			@Override public boolean isInstance(Value value) { return value instanceof StringValue; }
			@Override public String  getValue  (Value value) { return ((StringValue)value).value; }
		}
		private static final class GVH_Double extends GetValueHelper<Double> {
			private GVH_Double() { super(Type.Float); }
			@Override public boolean isInstance(Value value) { return value instanceof FloatValue; }
			@Override public Double  getValue  (Value value) { return ((FloatValue)value).value; }
		}
		private static final class GVH_Integer extends GetValueHelper<Long> {
			private GVH_Integer() { super(Type.Integer); }
			@Override public boolean isInstance(Value value) { return value instanceof IntegerValue; }
			@Override public Long    getValue  (Value value) { return ((IntegerValue)value).value; }
		}
		private static final class GVH_Bool extends GetValueHelper<Boolean> {
			private GVH_Bool() { super(Type.Bool); }
			@Override public boolean isInstance(Value value) { return value instanceof BoolValue; }
			@Override public Boolean getValue  (Value value) { return ((BoolValue)value).value; }
		}
	}

	private static <V> V generic_getValue(GetValueHelper<V> helper, JSON_Array arr, int index) {
		return generic_convert(helper, getValue(arr, index), ()->"(Array Index: "+index+")");
	}

	private static <V> V generic_getValue(GetValueHelper<V> helper, JSON_Object data, Object... path) {
		return generic_convert(helper, getValue(data, path), ()->"(path: "+Arrays.toString(path)+")");
	}

	private static Boolean     getBoolValue   (JSON_Object data, Object... path) { return generic_getValue(new GetValueHelper.GVH_Bool   (), data, path); }
	private static Long        getIntegerValue(JSON_Object data, Object... path) { return generic_getValue(new GetValueHelper.GVH_Integer(), data, path); }
	private static Double      getFloatValue  (JSON_Object data, Object... path) { return generic_getValue(new GetValueHelper.GVH_Double (), data, path); }
	private static String      getStringValue (JSON_Object data, Object... path) { return generic_getValue(new GetValueHelper.GVH_String (), data, path); }
	private static JSON_Array  getArrayValue  (JSON_Object data, Object... path) { return generic_getValue(new GetValueHelper.GVH_Array  (), data, path); }
	private static JSON_Object getObjectValue (JSON_Object data, Object... path) { return generic_getValue(new GetValueHelper.GVH_Object (), data, path); }

	private static Boolean     getBoolValue   (JSON_Array arr, int index) { return generic_getValue(new GetValueHelper.GVH_Bool   (), arr, index); }
	@SuppressWarnings("unused") private static Long        getIntegerValue(JSON_Array arr, int index) { return generic_getValue(new GetValueHelper.GVH_Integer(), arr, index); }
	@SuppressWarnings("unused") private static Double      getFloatValue  (JSON_Array arr, int index) { return generic_getValue(new GetValueHelper.GVH_Double (), arr, index); }
	@SuppressWarnings("unused") private static String      getStringValue (JSON_Array arr, int index) { return generic_getValue(new GetValueHelper.GVH_String (), arr, index); }
	@SuppressWarnings("unused") private static JSON_Array  getArrayValue  (JSON_Array arr, int index) { return generic_getValue(new GetValueHelper.GVH_Array  (), arr, index); }
	@SuppressWarnings("unused") private static JSON_Object getObjectValue (JSON_Array arr, int index) { return generic_getValue(new GetValueHelper.GVH_Object (), arr, index); }

	private static Long getIntegerValue_silent(JSON_Object data, Object... path) {
		enableStackTrace(false);
		Long value = getIntegerValue(data, path);
		enableStackTrace(true);
		return value;
	}

	private static Double getFloatValue_silent(JSON_Object data, Object... path) {
		enableStackTrace(false);
		Double value = getFloatValue(data, path);
		enableStackTrace(true);
		return value;
	}

	private static String getStringValue_silent(JSON_Object data, Object... path) {
		enableStackTrace(false);
		String value = getStringValue(data, path);
		enableStackTrace(true);
		return value;
	}
}
