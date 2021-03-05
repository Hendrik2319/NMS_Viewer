package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.Window;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.IDMap;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Gui.TextAreaOutput;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Appearances.BlockCArray;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Appearances.BlockContainer;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Inventories.Inventory;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.MultiTools.MultiTool;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.IntegerValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.JSON_Array;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.JSON_Object;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.NamedValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.ObjectValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.StringValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.TraverseException;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value;
import net.schwarzbaer.java.lib.jsonparser.JSON_Helper;

public class SaveGameData {
	
	public final String filename;
	public final int index;
	public final JSON_Object<NVExtra,VExtra> json_data;
	public HashMap<String,HashSet<String>> deObfuscatorUsage = null;
	public final boolean isPreNEXT;
	
	public Long version;
	public final General general;
	public final Universe universe;
	public final ExperimentalData experimentalData;
	public DiscoveryData discoveryData = null;
	public Stats stats = null;
	public KnownWords knownWords = null;
	public KnownWords knownWords2 = null;
	public Inventories inventories = null;
	public KnownBlueprints knownBlueprints = null;
	public UnboundBuildingObject[] baseBuildingObjects = null;
	public Vector<PersistentPlayerBase> persistentPlayerBases = null;
	public Vector<TeleportEndpoints> teleportEndpoints = null;
	
	public Vector<Frigate> frigates = null;
	public Vector<FrigateMission> frigateMissions = null;
	
	public Freighter  freighter  = null;
	public VehicleGroup.SpaceShips spaceShips = null;
	public VehicleGroup.Exocrafts  exocrafts  = null;
	
	public Vector<UniverseAddress> AtlasStationAdressData = null;
	public Vector<UniverseAddress> NewAtlasStationAdressData = null;
	public Vector<UniverseAddress> VisitedAtlasStationsData = null;
	public Vector<VisitedSystems.VisitedSystem> visitedSystems = null;
	public Inventory mainMultiTool = null;
	public Vector<MultiTool> altMultiTools = null;
	public Companions companions = null;
	public Appearances appearances = null;
	
	public SaveGameData(JSON_Object<NVExtra,VExtra> json_data, String filename, int index, boolean isPreNEXT) {
		
		this.filename = filename;
		this.index = index;
		this.json_data = json_data;
		this.isPreNEXT = isPreNEXT;
		
		this.general = new General(this);
		this.universe = new Universe();
		this.experimentalData = new ExperimentalData(this);
	}

	public void setDeObfuscatorUsage(HashMap<String, HashSet<String>> deObfuscatorUsage) {
		this.deObfuscatorUsage = deObfuscatorUsage;
	}
	
	public SaveGameData parse(boolean forPreview, Window window) {
		version = getIntegerValue(json_data, "Version");
		if (isPreNEXT) return this;
		
		general.parse();
		if (forPreview) return this;
		
		visitedSystems = VisitedSystems.parse(this);
		parseStats();
		knownBlueprints = KnownBlueprints.parse(this);
		knownWords  = parseKnownWords("KnownWords","Word","Races");
		knownWords2 = parseKnownWords("[KnownWords2]","[Word]","Races");
		discoveryData = DiscoveryData.parse(this);
		inventories = Inventories.parseInventories(this);
		mainMultiTool = MultiTools.parseMain(this);
		altMultiTools = MultiTools.parseAlternatives(this);
		companions = new Companions(this);
		appearances = new Appearances(this);
		
		baseBuildingObjects   = UnboundBuildingObject.parse(this);
		persistentPlayerBases = PersistentPlayerBase.parseBases(this);
		
		AtlasStationAdressData    = parseUniverseAddressStructureArray("AtlasStationAdressData"   , json_data, "PlayerStateData", "AtlasStationAdressData");
		NewAtlasStationAdressData = parseUniverseAddressStructureArray("NewAtlasStationAdressData", json_data, "PlayerStateData", "NewAtlasStationAdressData");
		VisitedAtlasStationsData  = parseUniverseAddressStructureArray("VisitedAtlasStationsData" , json_data, "PlayerStateData", "VisitedAtlasStationsData");
		
		teleportEndpoints = TeleportEndpoints.parse(this);
		freighter  = Freighter .parse(this);
		spaceShips = new VehicleGroup.SpaceShips(this);
		exocrafts  = new VehicleGroup.Exocrafts(this);
		frigates   = Frigate   .parse(this);
		parseFrigateMissions();
		
		experimentalData.parse();
		
		universe.sort();
		//universe.writeToConsole();
		
		determineAdditionalInfos();
		
		GameInfos.readUniverseObjectDataFromDataPool(universe,false);
		GameInfos.saveAllIDsToFiles();
		SaveViewer.steamIDs.writeToFile();
		
		return this;
	}
	
	static final UnknownValues globalUnknownValues = new UnknownValues();
	
	static class UnknownValues extends HashSet<String> {
		private static final long serialVersionUID = 4634388552216036085L;
		
		public void add(String prefix, String format, Object... args) {
			add( prefix +": "+ String.format(format, args) );
		}
		public void show(PrintStream out) {
			out.printf("UnknownValues {%n");
			Vector<String> vector = new Vector<>(this);
			vector.sort(null);
			for (String value:vector) out.printf("   %s%n", value);
			out.printf("}%n");
		}
	}
	
	
	static final OptionalValues globalOptionalValues = new OptionalValues();
	static class OptionalValues extends JSON_Helper.OptionalValues<NVExtra,VExtra> {
		private static final long serialVersionUID = 1738184173309879079L;
		
		void scanStructure(JSON_Object<NVExtra, VExtra> data, Object... path) {
			scan(getSubNode(data,path), toString(path));
		}

		private String toString(Object[] path) {
			Iterator<String> it = Arrays.stream(path).<String>map(obj->{
				if (obj==null) return "<null>";
				if (obj instanceof String ) return (String) obj;
				if (obj instanceof Integer) return String.format("[%d]", obj);
				return String.format("<%s:%s>", obj.getClass().getName(), obj);
			}).iterator();
			return String.join(".", (Iterable<String>)()->it);
		}
	}
	
	private void determineAdditionalInfos() {
		if (baseBuildingObjects!=null) {
			for (UnboundBuildingObject bbo:baseBuildingObjects) {
				if (bbo.galacticAddress==null) continue;
				Universe.Planet planet = universe.findPlanet(bbo.galacticAddress);
				if (planet!=null) {
					planet.additionalInfos.baseBuildingObjects.add(bbo);
					planet.additionalInfos.hasExocraftSummoningStation |= "^SUMMON_GARAGE".equals(bbo.objectID);
				}
			}
		}
		if (persistentPlayerBases!=null) {
			for (PersistentPlayerBase base:persistentPlayerBases) {
				if (base.galacticAddress==null) continue;
				if (base.baseType==null) continue;
				switch(base.baseType) {
				case SpaceBase: break; // TODO [OLD] PersistentPlayerBase.baseType == SpaceBase
				case FreighterBase: {
					Universe.SolarSystem system = universe.findSolarSystem(base.galacticAddress);
					if (system!=null) system.additionalInfos.hasFreighter = true;
				} break;
				case ExternalPlanetBase:
				case HomePlanetBase: {
					Universe.Planet planet = universe.findPlanet(base.galacticAddress);
					if (planet!=null) {
						if (base.baseType==PersistentPlayerBase.BaseType.ExternalPlanetBase)
							planet.additionalInfos.otherPlayerBases.add(base);
						if (base.baseType==PersistentPlayerBase.BaseType.HomePlanetBase) {
							planet.additionalInfos.playerBases.add(base);
							for (BuildingObject bbo:base.objects) {
								if (bbo.objectID==null) continue;
								if (bbo.objectID.equals("^SUMMON_GARAGE")) {
									planet.additionalInfos.hasExocraftSummoningStation = true;
								}
							}
						}
					}
				} break;
				}
			}
		}
		if (general.anomalyUA!=null) {
			if (general.anomalyUA.isSolarSystem()) {
				Universe.SolarSystem system = universe.findSolarSystem(general.anomalyUA);
				if (system!=null) system.additionalInfos.hasAnomaly=true;
			}
		}
		if (teleportEndpoints!=null) {
			for (TeleportEndpoints tel:teleportEndpoints) {
				if (tel.universeAddress==null) continue;
				if (tel.universeAddress.isPlanet()) {
					Universe.Planet planet = universe.findPlanet(tel.universeAddress);
					if (planet!=null) {
						if (tel.teleportHost==TeleportEndpoints.TeleportHost.ExternalBase)
							planet.additionalInfos.teleportEndpointsInOtherPlayerBases.add(tel);
						else
							planet.additionalInfos.teleportEndpoints.add(tel);
					}
				}
				if (tel.universeAddress.isSolarSystem()) {
					Universe.SolarSystem system = universe.findSolarSystem(tel.universeAddress);
					if (system!=null) system.additionalInfos.teleportEndpoints.add(tel);
				}
			}
		}
	}

	private static Long parseHexFormatedNumber(JSON_Object<NVExtra,VExtra> obj, String valueName) {
		return parseHexFormatedNumber(obj.getValue(valueName));
	}

	private static Long parseHexFormatedNumber(Value<NVExtra,VExtra> value) {
		if (value==null) return null;
		switch(value.type) {
		case String:
			StringValue<NVExtra, VExtra> stringValue = value.castToStringValue();
			if (stringValue!=null) {
				String addressStr = stringValue.value;
				if (addressStr!=null) {
					if (addressStr.startsWith("0x"))
						addressStr = addressStr.substring(2);
					try {
						long l = Long.parseUnsignedLong(addressStr, 16);
						value.extra.wasProcessed=true;
						return l;
					} catch (NumberFormatException e) {}
				}
			}
			break;
			
		case Integer:
			IntegerValue<NVExtra, VExtra> integerValue = value.castToIntegerValue();
			if (integerValue!=null) {
				value.extra.wasProcessed=true;
				return integerValue.value;
			}
			break;
			
		default:
			break;
		}
		return null;
	}

	private static UniverseAddress parseUniverseAddressField(JSON_Object<NVExtra,VExtra> obj, String valueName) {
		Value<NVExtra,VExtra> addressValue = obj.getValue(valueName);
		if (addressValue==null) return null;
		
		switch(addressValue.type) {
		case String:
			StringValue<NVExtra, VExtra> stringValue = addressValue.castToStringValue();
			if (stringValue!=null) {
				String addressStr = stringValue.value;
				if (isPlanetAddressOK(addressStr)) {
					stringValue.extra.wasProcessed=true;
					return new UniverseAddress( Long.parseLong(addressStr.substring(2), 16) );
				}
			}
			break;
			
		case Integer:
			IntegerValue<NVExtra, VExtra> integerValue = addressValue.castToIntegerValue();
			if (integerValue!=null) {
				integerValue.extra.wasProcessed=true;
				return new UniverseAddress( integerValue.value );
			}
			break;
			
		default:
			break;
		}
		return null;
	}

	private static UniverseAddress parseUniverseAddressStructure(JSON_Object<NVExtra,VExtra> data, Object... path) {
		JSON_Object<NVExtra,VExtra> universeAddressObj = path.length==0 ? data : getObjectValue(data, path);
		if (universeAddressObj==null) return null;
		
		Long galaxyIndexLong = getIntegerValue(universeAddressObj,"RealityIndex");
		if (galaxyIndexLong==null) return null;
		int galaxyIndex = (int)(long)galaxyIndexLong;
		
		JSON_Object<NVExtra,VExtra> galacticAddressObj = getObjectValue(universeAddressObj,"GalacticAddress");
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
	
	private static Vector<UniverseAddress> parseUniverseAddressStructureArray(String sourceLabel, JSON_Object<NVExtra,VExtra> data, Object... path) {
		return parseObjectArray(v->parseUniverseAddressStructure(v), sourceLabel, data, path);
	}
	@SuppressWarnings("unused")
	private static Vector<UniverseAddress> parseUniverseAddressFieldArray(String sourceLabel, JSON_Object<NVExtra,VExtra> data, Object... path) {
		return parseArray(value->{
			Long addr = getInteger(value);
			if (addr==null) return null;
			return new UniverseAddress(addr);
		}, sourceLabel, data, path);
	}

	private static <ValueType> Vector<ValueType> parseObjectArray(Supplier<ValueType> createNew, BiConsumer<ValueType,JSON_Object<NVExtra,VExtra>> parseValues, String sourceLabel, JSON_Object<NVExtra,VExtra> data, Object... path) {
		return parseObjectArray(objectValue->{
			ValueType dataItem = createNew.get();
			parseValues.accept(dataItem,objectValue);
			return dataItem;
		}, sourceLabel, data, path);
	}
	
	private static <ValueType> Vector<ValueType> parseObjectArray(Function<JSON_Object<NVExtra,VExtra>,ValueType> parseObject, String sourceLabel, JSON_Object<NVExtra,VExtra> data, Object... path) {
		return parseArray(value->{
			JSON_Object<NVExtra,VExtra> objectValue = getObject(value);
			if (objectValue==null) return null;
			return parseObject.apply(objectValue);
		}, sourceLabel, data, path);
	}
	
	private static <ValueType> Vector<ValueType> parseObjectArray(BiFunction<JSON_Object<NVExtra,VExtra>,Integer,ValueType> parseObject, String sourceLabel, JSON_Object<NVExtra,VExtra> data, Object... path) {
		return parseObjectArray(parseObject, sourceLabel, false, data, path);
	}
	private static <ValueType> Vector<ValueType> parseObjectArray(BiFunction<JSON_Object<NVExtra,VExtra>,Integer,ValueType> parseObject, String sourceLabel, boolean isOptional, JSON_Object<NVExtra,VExtra> data, Object... path) {
		return parseArray((value,index)->{
			JSON_Object<NVExtra,VExtra> objectValue = getObject(value);
			if (objectValue==null) return null;
			return parseObject.apply(objectValue,index);
		}, sourceLabel, isOptional, data, path);
	}
	
	@SuppressWarnings("unused")
	private static Vector<Long> parseIntegerArray(String sourceLabel, JSON_Object<NVExtra,VExtra> data, Object... path) {
		return parseArray(SaveGameData::getInteger, sourceLabel, data, path);
	}
	private static Vector<String> parseStringArray(String sourceLabel, JSON_Object<NVExtra,VExtra> data, Object... path) {
		return parseArray(SaveGameData::getString, sourceLabel, data, path);
	}
	
	private static <ValueType> Vector<ValueType> parseArray(Function<Value<NVExtra,VExtra>,ValueType> parseValue, String sourceLabel, JSON_Object<NVExtra,VExtra> data, Object... path) {
		return parseArray((value,i)->parseValue.apply(value), sourceLabel, data, path);
	}
	private static <ValueType> Vector<ValueType> parseArray(BiFunction<Value<NVExtra,VExtra>,Integer,ValueType> parseValue, String sourceLabel, JSON_Object<NVExtra,VExtra> data, Object... path) {
		return parseArray(parseValue, sourceLabel, false, data, path);
	}
	private static <ValueType> Vector<ValueType> parseArray(BiFunction<Value<NVExtra,VExtra>,Integer,ValueType> parseValue, String sourceLabel, boolean isOptional, JSON_Object<NVExtra,VExtra> data, Object... path) {
		JSON_Array<NVExtra,VExtra> arrayValue = isOptional ? getArrayValue_optional(data,path) : getArrayValue(data,path);
		if (arrayValue==null) return null;
		Vector<Value<NVExtra,VExtra>> notParsableObjects = new Vector<>();
		
		Vector<ValueType> dataItems = new Vector<>();
		for (int i=0; i<arrayValue.size(); ++i) {
			Value<NVExtra,VExtra> value = arrayValue.get(i);
			
			ValueType dataItem = parseValue.apply(value,i);
			if (dataItem==null)
				notParsableObjects.add(value);
			else
				dataItems.add(dataItem);
		}
		
		if (!notParsableObjects.isEmpty())
			Gui.log_error_ln("Found %d not parseable items in %s.", notParsableObjects.size(), sourceLabel);
		
		return dataItems;
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
		private static Position parse(JSON_Object<NVExtra,VExtra> obj, String valueName_Pos, String valueName_At, String valueName_Up) {
			Position position = new Position();
			position.pos = Coordinates.parse(obj, valueName_Pos);
			position.at  = Coordinates.parse(obj, valueName_At );
			position.up  = Coordinates.parse(obj, valueName_Up );
			position.gps = PolarCoordinates.parse(position.pos);
			return position;
		}
	}

	public static class PolarCoordinates {
		
		public final double latitude, longitude, radius;
		
		public PolarCoordinates(double latitude, double longitude, double radius) {
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
			
			return radius==0 ? null : new PolarCoordinates(latitude, longitude, radius);
		}

		@Override
		public String toString() {
			return toString(true);
		}

		public String toString(boolean withRadius) {
			String latCh =  latitude<0?"S":"N";
			String lonCh = longitude<0?"W":"E";
			if (withRadius && !Double.isNaN(radius))
				return String.format(Locale.ENGLISH, "%s%05.2f  %s%06.2f  (R:%1.2f)", latCh, Math.abs(latitude), lonCh, Math.abs(longitude), radius);
			else
				return String.format(Locale.ENGLISH, "%s%05.2f  %s%06.2f"           , latCh, Math.abs(latitude), lonCh, Math.abs(longitude));
		}

		public boolean isValueOk() {
			return !Double.isNaN(latitude) && !Double.isNaN(longitude);
		}

		public String toValueStr() {
			if (Double.isNaN(radius))
				return String.format(Locale.ENGLISH, "%1.4f,%1.4f"      , latitude, longitude);
			else
				return String.format(Locale.ENGLISH, "%1.4f,%1.4f,%1.2f", latitude, longitude, radius);
		}

		public static PolarCoordinates parseValueStr(String valueStr) {
			String[] valueParts = valueStr.split(",");
			if (valueParts.length==2 || valueParts.length==3)
				try {
					double latitude  = Double.parseDouble(valueParts[0]);
					double longitude = Double.parseDouble(valueParts[1]);
					double radius    = valueParts.length>2 ? Double.parseDouble(valueParts[2]) : Double.NaN;
					return new PolarCoordinates(latitude, longitude, radius);
				} catch (NumberFormatException e) {}
			
			Gui.log_error_ln("Can't parse PolarCoordinates from \"%s\".", valueStr);
			return null;
		}
	}

	public static class Coordinates extends Point3D {
	
		private static Coordinates parse(JSON_Object<NVExtra,VExtra> obj, String valueName) {
			JSON_Array<NVExtra,VExtra> arrayValue = getArrayValue(obj, valueName);
			return parse(arrayValue);
		}

		private static Coordinates parse(JSON_Array<NVExtra, VExtra> arrayValue) {
			if (arrayValue==null) return null;
			
			Coordinates coords = new Coordinates();
			for (int i=0; i<arrayValue.size(); ++i) {
				Value<NVExtra,VExtra> value = arrayValue.get(i);
				Double d = getFloat(value);
				if (d!=null) coords.set(i,d);
			}
			
			return coords;
		}

		public double w1;
		public double w2;
		public double w3;
		private int length;
	
		public Coordinates() {
			super(0,0,0);
			this.w1 = 0;
			this.w2 = 0;
			this.w3 = 0;
			this.length = 0; 
		}
		
		public Coordinates(Coordinates p) {
			super(p);
			this.w1 = p.w1;
			this.w2 = p.w2;
			this.w3 = p.w3;
			this.length = p.length; 
		}
		
		public Coordinates(Point3D p) {
			super(p);
			this.w1 = 0;
			this.w2 = 0;
			this.w3 = 0;
			this.length = 3; 
		}

		public void set(int i, double value) {
			switch(i) {
			case 0: x = value; break;
			case 1: y = value; break;
			case 2: z = value; break;
			case 3: w1 = value; break;
			case 4: w2 = value; break;
			case 5: w3 = value; break;
			}
			length = Math.max(i+1, length);
		}
	
		@Override
		public String toString() {
			return toString("%f");
		}
	
		public String toString(String valueformat) {
			return toString(valueformat, true);
		}

		@Override
		public String toString(String valueformat, boolean withComma) {
			String vf = valueformat;
			String c = withComma ? "," : " ";
			switch(length) {
			case 0 : return String.format(Locale.ENGLISH, "()"                                  );
			case 1 : return String.format(Locale.ENGLISH, "("+vf+")"                            , x);
			case 2 : return String.format(Locale.ENGLISH, "("+vf+c+vf+")"                       , x, y);
			case 3 : return String.format(Locale.ENGLISH, "("+vf+c+vf+c+vf+")"                  , x, y, z);
			case 4 : return String.format(Locale.ENGLISH, "("+vf+c+vf+c+vf+c+vf+")"             , x, y, z, w1);
			case 5 : return String.format(Locale.ENGLISH, "("+vf+c+vf+c+vf+c+vf+c+vf+")"        , x, y, z, w1, w2);
			case 6 : return String.format(Locale.ENGLISH, "("+vf+c+vf+c+vf+c+vf+c+vf+c+vf+")"   , x, y, z, w1, w2, w3);
			default: return String.format(Locale.ENGLISH, "("+vf+c+vf+c+vf+c+vf+c+vf+c+vf+"...)", x, y, z, w1, w2, w3);
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
		public enum OutputType { Short_MonNumber, Complete_MonDayText }
		
		public final long value_s;
		private OutputType type;
		
		private TimeStamp(long value_s) {
			this.value_s = value_s;
			type = OutputType.Complete_MonDayText;
		}
		public static TimeStamp create(Long value_s) {
			if (value_s==null) return null;
			return new TimeStamp(value_s);
		}
		public TimeStamp setOutputType(OutputType type) {
			this.type = type;
			return this;
		}
		@Override public String toString() {
			//return DateFormat.getDateTimeInstance().format(new Date(value_s*1000));
			return getTimeStr(value_s*1000,type);
		}
		@Override public int compareTo(TimeStamp other) {
			return (int) (this.value_s-other.value_s);
		}
		
		static String getTimeStr(long millis) {
			return getTimeStr(millis, OutputType.Complete_MonDayText);
		}
		static String getTimeStr(long millis, OutputType type) {
			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("CET"), Locale.GERMANY);
			cal.setTimeInMillis(millis);
			if (type!=null)
				switch (type) {
				case Complete_MonDayText: break;
				case Short_MonNumber: return String.format(Locale.ENGLISH, "%1$td.%1$tm.%1$tY %1$tT", cal);
				}
			return String.format(Locale.ENGLISH, "%1$tA, %1$te. %1$tb %1$tY, %1$tT [%1$tZ:%1$tz]", cal);
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
		public String userID;
		public String userName;
		public TimeStamp timeStamp;

		public static Owner parse(JSON_Object<NVExtra,VExtra> parentObj, String valueName) {
			JSON_Object<NVExtra,VExtra> objectValue = getObjectValue(parentObj, valueName);
			if (objectValue==null) return null;
			
			Owner owner = new Owner();
			owner.LID = getStringValue(objectValue, "LID");
			owner.userID = getStringValue(objectValue, "UID");
			owner.userName = getStringValue(objectValue, "USN");
			owner.timeStamp  = TimeStamp.create(getIntegerValue(objectValue, "TS"));
			
			if (owner.userName!=null && !owner.userName.isEmpty() && owner.userID!=null && !owner.userID.isEmpty())
				//try {
					//switch (owner.userID) {
					//case "76561197968127504":
					//case "76561198030447439":
					//	Gui.log_ln("[%s] \"%s\"", owner.userID, owner.userName);
					//	break;
					//}
					SaveViewer.steamIDs.set(owner.userID, owner.userName);
				//} catch (KnownSteamIDs.AssignmentExistsException e) {
				//	e.printConflict();
				//}
			
			return owner;
		}
		
		public String getOwnerName() {
			if (userName!=null && !userName.isEmpty()) return "\""+userName+"\"";
			if (userID!=null) {
				HashSet<String> names = SaveViewer.steamIDs.get(userID);
				if (names==null) return String.format("SteamID %s", userID);
				return KnownSteamIDs.toString(names);
			}
			return null;
		}
	}

	public static class SeedValue {
		
		public final Boolean isValid;
		public final Long seedValue;

		public SeedValue(Boolean isValid, Long seedValue) {
			super();
			this.isValid = isValid;
			this.seedValue = seedValue;
		}

		public static SeedValue parse(JSON_Object<NVExtra,VExtra> object, Object... path) {
			return parse( getArrayValue(object, path) );
		}
		public static SeedValue parse(JSON_Array<NVExtra,VExtra> array) {
			if (array==null || array.size()<2) return null;
			return new SeedValue(
				getBoolValue(array, 0),
				parseHexFormatedNumber(getSubNode(array, 1))
			);
		}

		@Override public String toString() {
			if (seedValue==null)
				return String.format("<%s> <null>" , isValid);
			return     String.format("<%s> 0x%016X", isValid, seedValue);
		}
		
		public String getSeedStr() {
			return getSeedStr(true);
		}
		public String getSeedStr(boolean withLeadingZeros) {
			if (isValid!=null && !isValid.booleanValue()) return "<no valid seed>";
			if (seedValue==null) return "<null>";
			return String.format(withLeadingZeros ? "0x%016X" : "0x%X", seedValue);
		}
		
		public UniverseAddress getSeedAsUniverseAddress() {
			if (seedValue==null) return null;
			return new UniverseAddress(seedValue);
		}
	}

	public static class ResourceBlock {
	
		public final String filename;
		public final SeedValue seed;
		public final String altID;
		public final JSON_Array<NVExtra,VExtra> ProceduralTexture_Samplers;
	
		public ResourceBlock(JSON_Object<NVExtra, VExtra> resourceObj) {
			filename = getStringValue(resourceObj, "Filename");
			seed     = SeedValue.parse( getArrayValue(resourceObj, "Seed") );
			altID    = getStringValue(resourceObj, "AltId");
			ProceduralTexture_Samplers = getArrayValue(resourceObj, "ProceduralTexture", "Samplers");
		}

		public static ResourceBlock parse(JSON_Object<NVExtra,VExtra> data, Object... path) {
			JSON_Object<NVExtra,VExtra> resourceObj = getObjectValue(data, path);
			return resourceObj==null ? null : new ResourceBlock(resourceObj);
		}
	}

	public static class TeleportEndpoints implements AddressdableObject {
		
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
		public Boolean unknown_a__;
		public Boolean unknown_tww;
		
		public TeleportEndpoints() {
			this.name = null;
			this.teleportHost = null;
			this.teleportHostStr = null;
			this.position = null;
			this.lookAt = null;
			this.universeAddress = null;
			this.gpsCoords = null;
		}
		
//		public String getNameAndGPS() {
//			if (gpsCoords==null) return String.format("\"%s\"", name);
//			return String.format("\"%s\" @ %s", name, gpsCoords.toString());
//		}

		@Override public UniverseAddress getUniverseAddress() { return universeAddress; }

		private static Vector<TeleportEndpoints> parse(SaveGameData data) {
			JSON_Array<NVExtra,VExtra> arrayValue = getArrayValue(data.json_data,"PlayerStateData","TeleportEndpoints");
			if (arrayValue==null) return null;
			Vector<Value<NVExtra, VExtra>> notParsableObjects = new Vector<>();
			
			Vector<TeleportEndpoints> teleportEndpoints = new Vector<TeleportEndpoints>();
			for (int i=0; i<arrayValue.size(); ++i) {
				Value<NVExtra,VExtra> value = arrayValue.get(i);
				JSON_Object<NVExtra,VExtra> objectValue = getObject(value);
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
				te.unknown_a__      = getBoolValue_optional(objectValue, "[??? a>; TeleportEndpoints Bool:false]");
				te.unknown_tww      = getBoolValue_optional(objectValue, "[??? tww TeleportEndpoints Bool:false]");
				
				
				if (te.universeAddress!=null) {
					data.universe.getOrCreate(te.universeAddress,obj->obj.foundInTeleportEndpoints.add(teleportEndpoints.size()),obj->obj.containsTeleportEndpoints=true);
				}
				
				teleportEndpoints.add(te);
			}
			
			if (!notParsableObjects.isEmpty())
				Gui.log_error_ln("Found "+notParsableObjects.size()+" not parseable TeleportEndpoints.");
			
			return teleportEndpoints;
		}

		public String getUnknownValues() {
			return String.format("[a>;]=%s [tww]=%s", unknown_a__, unknown_tww);
		}
	}

	public static class PersistentPlayerBase implements AddressdableObject {

		private static Vector<PersistentPlayerBase> parseBases(SaveGameData data) {
				JSON_Array<NVExtra,VExtra> arrayValue = getArrayValue(data.json_data,"PlayerStateData","PersistentPlayerBases");
				if (arrayValue==null) return null;
				Vector<Value<NVExtra, VExtra>> notParsableObjects = new Vector<>();
				
		//		persistentPlayerBases = new PersistentPlayerBases();
				Vector<PersistentPlayerBase> persistentPlayerBases = new Vector<>();
				for (int baseIndex=0; baseIndex<arrayValue.size(); ++baseIndex) {
					Value<NVExtra,VExtra> value = arrayValue.get(baseIndex);
					JSON_Object<NVExtra,VExtra> objectValue = getObject(value);
					if (objectValue==null) {
						notParsableObjects.add(value);
						continue;
					}
					
					PersistentPlayerBase pb = new PersistentPlayerBase(data,baseIndex);
					pb.baseVersion      = getIntegerValue (objectValue, "BaseVersion");
					pb.baseMinorVersion = getIntegerValue_optional(objectValue, "[BaseMinorVersion]");
					pb.galacticAddress  = parseUniverseAddressField(objectValue, "GalacticAddress");
					pb.position         = Coordinates     .parse(objectValue, "Position");
					pb.gpsCoords        = PolarCoordinates.parse(pb.position);
					pb.forward          = Coordinates     .parse(objectValue, "Forward");
					pb.userData         = getIntegerValue (objectValue, "UserData");
					pb.lastUpdateTS     = TimeStamp.create(getIntegerValue (objectValue, "LastUpdateTimestamp"));
					pb.rid              = getStringValue  (objectValue, "RID");
					pb.owner            = Owner.parse     (objectValue, "Owner");
					pb.name             = getStringValue  (objectValue, "Name");
					pb.baseTypeStr      = getStringValue  (objectValue, "BaseType", "BaseType_");
					pb.baseType         = PersistentPlayerBase.BaseType.parseValue(pb.baseTypeStr);
					
					pb.objects = parseBaseObjects(data, getArrayValue(objectValue,"Objects"), pb.baseTypeStr!=null?pb.baseTypeStr:"Base", baseIndex);
					
					if (pb.galacticAddress!=null) {
						data.universe.getOrCreate(pb.galacticAddress,obj->obj.foundInPersistentPlayerBases.add(persistentPlayerBases.size()),obj->obj.containsPersistentPlayerBases=true);
					}
					
					persistentPlayerBases.add(pb);
				}
				
				if (!notParsableObjects.isEmpty())
					Gui.log_error_ln("Found "+notParsableObjects.size()+" not parseable PersistentPlayerBases.");
				
				return persistentPlayerBases;
			}

		private static BuildingObject[] parseBaseObjects(SaveGameData source, JSON_Array<NVExtra,VExtra> arrayValue, String baseType, int baseIndex) {
			if (arrayValue==null) return null;
			Vector<Value<NVExtra, VExtra>> notParsableObjects = new Vector<>();
			
			Vector<BuildingObject> vector = new Vector<BuildingObject>();
			for (int i=0; i<arrayValue.size(); i++) {
				Value<NVExtra,VExtra> value = arrayValue.get(i);
				JSON_Object<NVExtra,VExtra> objectValue = getObject(value);
				if (objectValue==null) {
					notParsableObjects.add(value);
					continue;
				}
				
				BuildingObject bbo = new BuildingObject(source);
				BuildingObject.parseBuildingObject(source, objectValue, bbo, "["+(baseIndex+1)+"]"+baseType, i);
				
				vector.add(bbo);
			}
			
			if (!notParsableObjects.isEmpty())
				Gui.log_error_ln("Found "+notParsableObjects.size()+" not parseable Objects in PersistentPlayerBases["+baseIndex+"].");
			
			return vector.toArray(new BuildingObject[vector.size()]);
		}

		public enum BaseType {
			FreighterBase     ("F","Freighter Base","Players Base on Freighter"),
			HomePlanetBase    ("P","Planet Base"   ,"Players Base on Planet"),
			ExternalPlanetBase("P","Other Base"    ,"Other Players Base on Planet"),
			SpaceBase         ("S","SpaceBase (??)","SpaceBase (??)"),
			;
			
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
		
		public UniverseAddress galacticAddress = null;
		public String name = null;
		public Owner owner = null;
		public Long baseVersion = null;
		public Long baseMinorVersion = null;
		public String rid = null;
		public TimeStamp lastUpdateTS = null;
		public Long userData = null;
		public Coordinates forward = null;
		public Coordinates position = null;
		public PolarCoordinates gpsCoords = null;
		public BuildingObject[] objects = null;
		public String baseTypeStr = null;
		public BaseType baseType = null;
		
		public PersistentPlayerBase(SaveGameData source, int baseIndex) {
			this.source = source;
			this.baseIndex = baseIndex;
		}

		@Override public UniverseAddress getUniverseAddress() { return galacticAddress; }
	}

	public static class UnboundBuildingObject extends BuildingObject implements AddressdableObject {
		public UniverseAddress galacticAddress;
		public Long regionSeed;
		
		UnboundBuildingObject(SaveGameData source) {
			super(source);
			this.galacticAddress = null;
			this.regionSeed = null;
		}
		@Override public UniverseAddress getUniverseAddress() { return galacticAddress; }

		private static UnboundBuildingObject[] parse(SaveGameData data) {
			JSON_Array<NVExtra,VExtra> arrayValue = getArrayValue(data.json_data,"PlayerStateData","BaseBuildingObjects");
			if (arrayValue==null) return null;
			Vector<Value<NVExtra,VExtra>> notParsableObjects = new Vector<>();
			
			Vector<UnboundBuildingObject> vector = new Vector<UnboundBuildingObject>();
			for (int i=0; i<arrayValue.size(); i++) {
				Value<NVExtra,VExtra> value = arrayValue.get(i);
				JSON_Object<NVExtra,VExtra> objectValue = getObject(value);
				if (objectValue==null) {
					notParsableObjects.add(value);
					continue;
				}
				UnboundBuildingObject bbo = new UnboundBuildingObject(data);
				bbo.galacticAddress = parseUniverseAddressField(objectValue, "GalacticAddress");
				bbo.regionSeed      = parseHexFormatedNumber   (objectValue, "RegionSeed");
				BuildingObject.parseBuildingObject(data, objectValue, bbo, "BaseBuildingObjects", i);
				
				if (bbo.galacticAddress!=null) {
					data.universe.getOrCreate(bbo.galacticAddress,obj->obj.foundInBaseBuildingObjects.add(vector.size()),obj->obj.containsBaseBuildingObjects=true);
				}
				
				vector.add(bbo);
			}
			
			if (!notParsableObjects.isEmpty())
				Gui.log_error_ln("Found "+notParsableObjects.size()+" not parseable BaseBuildingObjects.");
			
			return vector.toArray(new UnboundBuildingObject[0]);
		}
	}

	public static class BuildingObject {
		
		public enum BaseObjAppearance {
			New("Neu"), Used("Gebraucht"), HardUsed("Abgenutzt"), Old("Antik");
			public final String label;
			BaseObjAppearance(String label) {
				this.label = label;
			}
		}
		
		public enum BaseObjColor {
			White_Orange("Weiß, Orange"),
			Black_Yellow("Schwarz, Gelb"),
			Yellow_LightBlue("Gelb, Hell-Blau"),
			PastelAqua_Aqua("Pastell-Türkis, Türkis"),
			Blue_White("Blau, Weiß"),
			Blue_Orange("Blau, Orange"),
			Violet_PastelAqua("Violett, Pastell-Türkis"),
			LightViolet_Violet("Hell-Violett, Violett"),
			
			Red_White("Rot, Weiß"),
			Orange_Blue("Orange, Blau"),
			Yellow_White("Gelb, Weiß"),
			Green_DarkGreen("Grün, Dunkel-Grün"),
			Aqua_Red("Türkis, Rot"),
			Violet_Yellow("Violett, Gelb"),
			Gray_Black("Grau, Schwarz"),
			Gray_Red("Grau, Rot"),
			;
			public final String label;
			BaseObjColor(String label) {
				this.label = label;
			}
		}

		private static void parseBuildingObject(SaveGameData source, JSON_Object<NVExtra,VExtra> objectValue, BuildingObject bbo, String label, int index) {
			bbo.timestamp = TimeStamp.create(getIntegerValue (objectValue, "Timestamp"));
			bbo.objectID  = getStringValue  (objectValue, "ObjectID");
			bbo.userData  = getIntegerValue (objectValue, "UserData");
			bbo.message   = getStringValue_optional(objectValue, "[Message]");
			bbo.position  = Position.parse  (objectValue, "Position", "At", "Up"); // bug fixed
			if (bbo.userData!=null) {
				long userData = bbo.userData.longValue();
				// 0x0300000C
				bbo.color = BaseObjColor.values()[(int) (userData & 0x0F)];
				bbo.appearance = BaseObjAppearance.values()[(int) ((userData & 0x03000000) >> 24)];
			}
			
			// revert bug fixing
//			Coordinates temp = bbo.position.at;
//			bbo.position.at = bbo.position.up;
//			bbo.position.up = temp;
			
			if (bbo.objectID!=null) {
				GeneralizedID id = GameInfos.productIDs.get(bbo.objectID, source, GameInfos.GeneralizedID.Usage.Type.BuildingObject);
				id.getUsage(source).addBBOUsage(label,index);
			}
		}

		//public GeneralizedID objectID1;
		public String specialName;
		
		public TimeStamp timestamp;
		public String objectID;
		public Long userData;
		public BaseObjColor color;
		public BaseObjAppearance appearance;
		public String message;

		public Position position;
		public final SaveGameData source;
		
		BuildingObject(SaveGameData source) {
			this.source = source;
			this.timestamp  = null;
			this.objectID   = null;
			this.userData   = null;
			this.color      = null;
			this.appearance = null;
			this.position   = null;
			this.specialName = null;
			//this.objectID1 = null;
		}

		public BuildingObject(BuildingObject obj) {
			this.source = obj.source;
			this.timestamp  = obj.timestamp;
			this.objectID   = obj.objectID;
			this.userData   = obj.userData;
			this.color      = obj.color;
			this.appearance = obj.appearance;
			this.position   = obj.position==null?null:new Position(obj.position);
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
				obj.position.up = new Coordinates(playerbase.position.normalize());
			obj.position.at = playerbase.forward;
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

		public Vector<GeneralizedID> technologies = null;
		public Vector<GeneralizedID> products = null;
		public Vector<GeneralizedID> quicksilvers = null;
		public Vector<String> recipes = null;
		
		private static KnownBlueprints parse(SaveGameData data) {
			KnownBlueprints knownBlueprints = new KnownBlueprints();
			knownBlueprints.technologies   = parseBlueprintArray(data, "Technology" , GameInfos.techIDs   , GeneralizedID.Usage.Type.Blueprint         , "KnownTech"                 , data.json_data, "PlayerStateData","KnownTech"    );
			knownBlueprints.products       = parseBlueprintArray(data, "Product"    , GameInfos.productIDs, GeneralizedID.Usage.Type.Blueprint         , "KnownProducts"             , data.json_data, "PlayerStateData","KnownProducts");
			knownBlueprints.quicksilvers   = parseBlueprintArray(data, "Quicksilver", GameInfos.productIDs, GeneralizedID.Usage.Type.QuicksilverSpecial, "[KnownQuicksilverSpecials]", data.json_data, "PlayerStateData","[KnownQuicksilverSpecials]");
			if (hasValue(data.json_data, "PlayerStateData","[KnownRecipes]"))
				knownBlueprints.recipes = parseStringArray("[KnownRecipes]", data.json_data, "PlayerStateData","[KnownRecipes]");
			return knownBlueprints;
		}
		
		private static Vector<GeneralizedID> parseBlueprintArray(SaveGameData data, String blueprintCategory, IDMap map, GameInfos.GeneralizedID.Usage.Type usageType, String sourceLabel, JSON_Object<NVExtra,VExtra> jsonObject, Object... path) {
			if (!hasValue(jsonObject, path)) return null;
			return parseArray((value,i)->{
				
				String id = getString(value);
				if (id==null) return null;
				
				//GameInfos.GeneralizedID.Usage.Type usageType = GeneralizedID.Usage.Type.Blueprint;
				GeneralizedID generalizedID = map.get(id,data,usageType);
				if (generalizedID==null) {
					Gui.log_error_ln("Can't find ID \"%s\" in %s", id, map.getLabel());
					return null;
				}
				
				generalizedID.getUsage(data).addBlueprintUsage(blueprintCategory,i);
				return generalizedID;
				
			}, sourceLabel, jsonObject, path);
		}
	}

	public static class Freighter implements AddressdableObject {
		
		public enum FreighterClass { CapitalFreighter, Freighter }
		
		public String name = null;
		public FreighterClass freighterClass = null; 
		public UniverseAddress ua = null;
		public Position pos = null;
		public Universe.SolarSystem.Race crewRace = null;
		public Inventory inventory = null;
		public Inventory inventoryTech = null;
		
		private ResourceBlock crewResourceBlock = null;
		private ResourceBlock freighterResourceBlock = null;
		private SeedValue homeseed;
		
		@Override public UniverseAddress getUniverseAddress() { return ua; }

		private static Freighter parse(SaveGameData data) {
			Freighter freighter = new Freighter();
			
			freighter.name = getStringValue(data.json_data,"PlayerStateData","PlayerFreighterName");
			freighter.ua   = parseUniverseAddressStructure(data.json_data,"PlayerStateData","FreighterUniverseAddress");
			freighter.pos  = Position.parse(getObjectValue(data.json_data, "PlayerStateData"), "FreighterPosition", "FreighterMatrixLookAt", "FreighterMatrixUp");
			
			freighter.crewResourceBlock      = ResourceBlock.parse(data.json_data,"PlayerStateData","CurrentFreighterNPC");
			freighter.freighterResourceBlock = ResourceBlock.parse(data.json_data,"PlayerStateData","CurrentFreighter");
			
			if (freighter.crewResourceBlock!=null && freighter.crewResourceBlock.filename!=null) {
				switch (freighter.crewResourceBlock.filename) {
				case "MODELS/COMMON/PLAYER/PLAYERCHARACTER/NPCKORVAX.SCENE.MBIN": freighter.crewRace = Universe.SolarSystem.Race.Korvax; break;
				case "MODELS/COMMON/PLAYER/PLAYERCHARACTER/NPCVYKEEN.SCENE.MBIN": freighter.crewRace = Universe.SolarSystem.Race.Vykeen; break;
				case "MODELS/COMMON/PLAYER/PLAYERCHARACTER/NPCGEK.SCENE.MBIN"   : freighter.crewRace = Universe.SolarSystem.Race.Gek; break;
				case "MODELS\\/COMMON\\/PLAYER\\/PLAYERCHARACTER\\/NPCKORVAX.SCENE.MBIN": freighter.crewRace = Universe.SolarSystem.Race.Korvax; break;
				case "MODELS\\/COMMON\\/PLAYER\\/PLAYERCHARACTER\\/NPCVYKEEN.SCENE.MBIN": freighter.crewRace = Universe.SolarSystem.Race.Vykeen; break;
				case "MODELS\\/COMMON\\/PLAYER\\/PLAYERCHARACTER\\/NPCGEK.SCENE.MBIN"   : freighter.crewRace = Universe.SolarSystem.Race.Gek; break;
				default: Gui.log_warn_ln("Found unknown crew race. ResourceBlock.Filename: \"%s\"", freighter.crewResourceBlock.filename);
				}
			}
			if (freighter.freighterResourceBlock!=null && freighter.freighterResourceBlock.filename!=null) {
				switch (freighter.freighterResourceBlock.filename) {
				case "MODELS/COMMON/SPACECRAFT/INDUSTRIAL/CAPITALFREIGHTER_PROC.SCENE.MBIN": freighter.freighterClass = FreighterClass.CapitalFreighter; break;
				case "MODELS/COMMON/SPACECRAFT/INDUSTRIAL/FREIGHTER_PROC.SCENE.MBIN"       : freighter.freighterClass = FreighterClass.Freighter; break;
				case "MODELS\\/COMMON\\/SPACECRAFT\\/INDUSTRIAL\\/CAPITALFREIGHTER_PROC.SCENE.MBIN": freighter.freighterClass = FreighterClass.CapitalFreighter; break;
				case "MODELS\\/COMMON\\/SPACECRAFT\\/INDUSTRIAL\\/FREIGHTER_PROC.SCENE.MBIN"       : freighter.freighterClass = FreighterClass.Freighter; break;
				case "": break;
				default: Gui.log_warn_ln("Found unknown freighter class. ResourceBlock.Filename: \"%s\"", freighter.freighterResourceBlock.filename);
				}
			}
			
			freighter.homeseed = SeedValue.parse( getArrayValue(data.json_data, "PlayerStateData","[CurrentFreighter HomeSeed]") );
			if (freighter.homeseed!=null) {
				
			}
			
			String inventoryLabel = "Freighter";
			if (freighter.name!=null) inventoryLabel += String.format(" \"%s\"", freighter.name);
			if (freighter.crewRace!=null || freighter.freighterClass!=null) {
				String values = "";
				if (freighter.crewRace      !=null) values += String.format("%sCrew:%s" , values.isEmpty()?"":", ", freighter.crewRace);
				if (freighter.freighterClass!=null) values += String.format("%sClass:%s", values.isEmpty()?"":", ", freighter.freighterClass);
				inventoryLabel += "  ( "+values+" )";
			}
			
			freighter.inventory     = Inventories.parse(
				data,
				getObjectValue(data.json_data, "PlayerStateData", "FreighterInventory"),
				getObjectValue(data.json_data, "PlayerStateData", "FreighterLayout"),
				inventoryLabel,
				"FreighterInventory"
			);
			freighter.inventoryTech = Inventories.parse(
				data,
				getObjectValue(data.json_data, "PlayerStateData", "FreighterInventory_TechOnly"),
				"Freighter (Tech)",
				"FreighterInventory_TechOnly"
			);
			
			if (freighter.inventory!=null) {
				//data.universe;
				freighter.inventory.addExtraInfos(out->{
					out.printf("Freighter Infos:%n");
					if (freighter.name!=null)
						out.printf("   Name: %s%n", freighter.name.isEmpty() ? "<Original Name>" : "\""+freighter.name+"\"");
					if (freighter.inventory.validSlots!=null && freighter.inventory.inventoryClass!=null)
						out.printf("   Type: %s-%d%n", freighter.inventory.inventoryClass, freighter.inventory.validSlots);
					if (freighter.freighterClass!=null)
						out.printf("   Class: %s%n", freighter.freighterClass);
					if (freighter.crewRace      !=null)
						out.printf("   Crew: %s%n" , freighter.crewRace);
					if (freighter.freighterResourceBlock!=null && freighter.freighterResourceBlock.seed!=null)
						out.printf("   Model Seed: %s%n", freighter.freighterResourceBlock.seed.getSeedStr());
					if (freighter.homeseed!=null && freighter.homeseed.seedValue!=null) {
						out.printf("   Home Seed: 0x%016X%n", freighter.homeseed.seedValue);
						UniverseAddress ua = freighter.homeseed.getSeedAsUniverseAddress();
						if (ua!=null) {
							Vector<String> verboseName = ua.getVerboseName(data.universe);
							for (String str:verboseName)
								out.printf("              %s%n", str);
						}
					}
				});
			}
			
			return freighter;
		}
	}

	public static class SpaceShip extends Vehicle {
		public enum ShipClass { Transporter, Fighter, Shuttle, Explorer, Exotic, Alien }
		
		public boolean usesOldColors;
		
		private SpaceShip(SaveGameData data, JSON_Object<NVExtra, VExtra> vehicleData, int i, String dataSourcePath, boolean isPrimary) {
			super(data, vehicleData, i, dataSourcePath, isPrimary,"SpaceShip", null, SpaceShip::getVehicleClass, SpaceShip::getExtraInfosOutput);
		}
		
		private static String getVehicleClass(String resourcefilename) {
			switch (resourcefilename) {
			
			case "MODELS\\/COMMON\\/SPACECRAFT\\/DROPSHIPS\\/DROPSHIP_PROC.SCENE.MBIN"        : return "Transporter";
			case "MODELS\\/COMMON\\/SPACECRAFT\\/FIGHTERS\\/FIGHTER_PROC.SCENE.MBIN"          : return "Fighter";
			case "MODELS\\/COMMON\\/SPACECRAFT\\/SHUTTLE\\/SHUTTLE_PROC.SCENE.MBIN"           : return "Shuttle";
			case "MODELS\\/COMMON\\/SPACECRAFT\\/SCIENTIFIC\\/SCIENTIFIC_PROC.SCENE.MBIN"     : return "Explorer";
			case "MODELS\\/COMMON\\/SPACECRAFT\\/S-CLASS\\/S-CLASS_PROC.SCENE.MBIN"           : return "Exotic";
			case "MODELS\\/COMMON\\/SPACECRAFT\\/S-CLASS\\/BIOPARTS\\/BIOSHIP_PROC.SCENE.MBIN": return "LivingShip";
			
			case "MODELS/COMMON/SPACECRAFT/DROPSHIPS/DROPSHIP_PROC.SCENE.MBIN"      : return "Transporter";
			case "MODELS/COMMON/SPACECRAFT/FIGHTERS/FIGHTER_PROC.SCENE.MBIN"        : return "Fighter";
			case "MODELS/COMMON/SPACECRAFT/SHUTTLE/SHUTTLE_PROC.SCENE.MBIN"         : return "Shuttle";
			case "MODELS/COMMON/SPACECRAFT/SCIENTIFIC/SCIENTIFIC_PROC.SCENE.MBIN"   : return "Explorer";
			case "MODELS/COMMON/SPACECRAFT/S-CLASS/S-CLASS_PROC.SCENE.MBIN"         : return "Exotic";
			case "MODELS/COMMON/SPACECRAFT/S-CLASS/BIOPARTS/BIOSHIP_PROC.SCENE.MBIN": return "LivingShip";
			
			default:
				Gui.log_warn_ln("Unknown SpaceShip.VehicleClass: \"%s\"", resourcefilename);
			}
			return null;
		}
		
		private static void getExtraInfosOutput(Vehicle thisVehicle, TextAreaOutput out) {
			if (thisVehicle instanceof SpaceShip) {
				SpaceShip spaceShip = (SpaceShip) thisVehicle;
				if (spaceShip.usesOldColors)
					out.printf("   Model uses old colors%n");
			}
		}
	}

	public static class Exocraft extends Vehicle {
		public enum Instance {
			Roamer(0,"Roamer"), Nomad(1,"Nomad"), Colossus(2,"Colossus"), Pilgrim(3,"Pilgrim"), Nautilon(5,"Nautilon"), Minotaurus(6,"Minotaurus"),
			;
			public final int index;
			public final String label;
			private Instance(int index, String label) { this.index = index; this.label = label; }
			public static String getName(int i) {
				Instance instance = get(i);
				return instance==null ? null : instance.label;
			}
			private static Instance get(int i) {
				for (Instance inst:values())
					if (inst.index==i)
						return inst;
				return null;
			}
		}
		
		private Exocraft(SaveGameData data, JSON_Object<NVExtra, VExtra> vehicleData, int i, String dataSourcePath, boolean isPrimary) {
			super(data, vehicleData, i, dataSourcePath, isPrimary, "Exocraft", Instance.getName(i), null, null);
		}
	}

	public static abstract class Vehicle {
		public final Inventory inventory;
		public final Inventory inventoryTech;
		public final ResourceBlock resourceBlock;
		public final UniverseAddress location;
		public final Coordinates position;
		public final PolarCoordinates gpsCoords;
		public final Coordinates direction;
		public final String name;
		public final boolean isPrimary;
		public final String vehicleClass;
		
		protected Vehicle(SaveGameData data, JSON_Object<NVExtra,VExtra> vehicleData, int i, String dataSourcePath, boolean isPrimary, String typeLabel, String predefinedName, Function<String,String> getVehicleClass, BiConsumer<Vehicle,TextAreaOutput> extraInfosOutput) {
			
			resourceBlock = ResourceBlock.parse(vehicleData,"Resource");
			if (resourceBlock!=null && resourceBlock.filename!=null && !resourceBlock.filename.isEmpty() && getVehicleClass!=null)
				vehicleClass = getVehicleClass.apply(resourceBlock.filename);
			else
				vehicleClass = null;
			
			String inventoryLabel;
			if (predefinedName == null || predefinedName.isEmpty())
				inventoryLabel = typeLabel+" "+(i+1);
			else
				inventoryLabel = "["+(i+1)+"] "+predefinedName;
			
			inventory     = Inventories.parse(
				data,
				getObjectValue(vehicleData,"Inventory"),
				getObjectValue(vehicleData,"InventoryLayout"),
				inventoryLabel,
				dataSourcePath+".Inventory"
			);
			inventoryTech = Inventories.parse(
				data,
				getObjectValue(vehicleData,"Inventory_TechOnly"),
				inventoryLabel+" (Tech)",
				dataSourcePath+".Inventory_TechOnly"
			);
			
			name = getStringValue(vehicleData,"Name");
			this.isPrimary = isPrimary;
			
			location  = parseUniverseAddressField(vehicleData, "Location");
			position  = Coordinates.parse(vehicleData, "Position");
			gpsCoords = PolarCoordinates.parse(position);
			direction = Coordinates.parse(vehicleData, "Direction");
			
			if (inventory!=null) {
				if (name!=null && !name.isEmpty()) inventory.label += " \""+name+"\"";
				String iClass = inventory.inventoryClass;
				Integer validSlots = inventory.validSlots;
				inventory.label += String.format("<%s%s-%s>", vehicleClass==null?"":vehicleClass+" ", iClass==null?"?":iClass, validSlots==null?"??":validSlots);
				if (this.isPrimary) inventory.label += "   [Primary]";
			}
			
			if (inventory!=null) {
				inventory.addExtraInfos(out->{
					out.printf("%s Infos:%n", typeLabel);
					if (name!=null)
						out.printf("   Name: %s%n", name.isEmpty() ? "<Original Name>" : "\""+name+"\"");
					if (this.isPrimary)
						out.printf("   is Primary %s%n", typeLabel);
					if (inventory.validSlots!=null && inventory.inventoryClass!=null)
						out.printf("   Type: %s-%d%n", inventory.inventoryClass, inventory.validSlots);
					if (vehicleClass!=null)
						out.printf("   Class: %s%n", vehicleClass);
					if (resourceBlock!=null && resourceBlock.seed!=null && (resourceBlock.seed.isValid==null || resourceBlock.seed.isValid.booleanValue()))
						out.printf("   Model Seed: %s%n", resourceBlock.seed.getSeedStr());
					if (location!=null) {
						out.printf("   Location: 0x%016X%n", location.address);
						Vector<String> verboseName = location.getVerboseName(data.universe);
						for (String str:verboseName)
							out.printf("             %s%n", str);
					}
					if (position !=null) out.printf("   Position : %s%n", position .toString("%1.2f"));
					if (direction!=null) out.printf("   Direction: %s%n", direction.toString("%1.2f"));
					if (gpsCoords!=null) out.printf("   GPS      : %s%n", gpsCoords.toString());
				});
				if (extraInfosOutput!=null)
					inventory.addExtraInfos(out->extraInfosOutput.accept(this, out));
			}
		}
	}

	public static class VehicleGroup<VehicleType extends Vehicle> {
		
		public final Long primary;
		public final VehicleType[] vehicles;

		private VehicleGroup(SaveGameData data, JSON_Array<NVExtra,VExtra> json_Array, String arraySourcePath, Long primary, Constructor<VehicleType> constructor, Function<Integer,VehicleType[]> createArray) {
			this.primary = primary;
			
			if (json_Array!=null) {
				vehicles = createArray.apply(json_Array.size());
				for (int i=0; i<json_Array.size(); ++i) {
					JSON_Object<NVExtra, VExtra> object = getObject(json_Array.get(i));
					vehicles[i] = object==null ? null : constructor.construct(data, object, i, arraySourcePath+"["+i+"]", i==this.primary);
				}
			} else
				vehicles = null;
		}
		
		interface Constructor<VehicleType extends Vehicle> {
			VehicleType construct(SaveGameData data, JSON_Object<NVExtra,VExtra> vehicleData, int i, String dataSourcePath, boolean isPrimary);
		}

		public static class Exocrafts extends VehicleGroup<Exocraft> {
			public Exocrafts(SaveGameData data) {
				super(
					data,
					getArrayValue(data.json_data, "PlayerStateData", "VehicleOwnership"), "VehicleOwnership",
					getIntegerValue(data.json_data, "PlayerStateData", "PrimaryVehicle"),
					Exocraft::new, Exocraft[]::new
				);
			}
		}

		public static class SpaceShips extends VehicleGroup<SpaceShip> {
			public SpaceShips(SaveGameData data) {
				super(
					data,
					getArrayValue(data.json_data, "PlayerStateData", "ShipOwnership"), "ShipOwnership",
					getIntegerValue(data.json_data, "PlayerStateData", "PrimaryShip"),
					SpaceShip::new, SpaceShip[]::new
				);
				
				JSON_Array<NVExtra,VExtra> array = getArrayValue_optional(data.json_data, "PlayerStateData", "[ShipUsesOldColors]");
				if (array!=null && vehicles!=null)
					for (int i=0; i<array.size(); i++) {
						Boolean b = getBool(array.get(i));
						if (b!=null && i<vehicles.length) vehicles[i].usesOldColors = b;
					}
			}
		}
	}
	
	public final static class MultiTools {
		
		private static Inventory parseMain(SaveGameData data) {
			return Inventories.parse(
					data,
					getObjectValue(data.json_data, "PlayerStateData", "WeaponInventory"),
					getObjectValue(data.json_data, "PlayerStateData", "WeaponLayout"),
					"Main MultiTool", "WeaponInventory"
				);
		}
		
		private static Vector<MultiTool> parseAlternatives(SaveGameData data) {
			JSON_Array<NVExtra,VExtra> arr = getArrayValue_optional(data.json_data, "PlayerStateData", "[AlternativeWeapons]");
			if (arr==null) return null; 
			
			Vector<MultiTool> multiTools = new Vector<>();
			for (int i=0; i<arr.size(); i++) {
				Value<NVExtra,VExtra> value = arr.get(i);
				JSON_Object<NVExtra,VExtra> obj = getObject(value);
				if (obj==null) continue;
				multiTools.add(MultiTool.parse(data,obj,"Alternative "+(i+1),"AlternativeWeapons["+i+"]"));
			}
			
			return multiTools;
		}
		
		
		public final static class MultiTool {

			public  final Inventory inventory;
			private final String name;
			private final SeedValue seed;
			private final Boolean unknown_OGV_active;
			private final Long unknown_qVG_int1;
			private final Long unknown_jl__int2;

			public static MultiTool parse(SaveGameData data, JSON_Object<NVExtra,VExtra> obj, String label, String sourcePath) {
				if (obj==null) return null;
				return new MultiTool(data, obj, label, sourcePath);
			}

			private MultiTool(SaveGameData data, JSON_Object<NVExtra, VExtra> obj, String label, String sourcePath) {
				name = getStringValue (obj, "Name");
				seed = SeedValue.parse(getArrayValue(obj, "Seed"));
				unknown_OGV_active = getBoolValue (obj, "[??? OGV Active??]");
				unknown_qVG_int1 = getIntegerValue(obj, "[??? qVG AlternativeWeapons Int:3]");
				unknown_jl__int2 = getIntegerValue(obj, "[??? jl; AlternativeWeapons Int:5]");
				
				if (name!=null && !name.isEmpty())
					label = String.format("\"%s\" [%s]", name, label);
				
				Vector<String> unknown_values = new Vector<>(); // unknown_values 
				if (unknown_OGV_active!=null) unknown_values.add(unknown_OGV_active.toString());
				if (unknown_qVG_int1  !=null) unknown_values.add(unknown_qVG_int1  .toString());
				if (unknown_jl__int2  !=null) unknown_values.add(unknown_jl__int2  .toString());
				if (!unknown_values.isEmpty())
					label += " <"+String.join(",", unknown_values)+">";
				
				inventory = Inventories.parse(data, getObjectValue(obj, "Store"), getObjectValue(obj, "[??? CA4]"), label, sourcePath+".Store");
				if (inventory!=null)
					inventory.addExtraInfos(out->{
						out.printf("MultiTool Infos:%n");
						if (name!=null)
							out.printf("   Name: %s%n", name.isEmpty() ? "<Original Name>" : "\""+name+"\"");
						if (inventory.validSlots!=null && inventory.inventoryClass!=null)
							out.printf("   Type: %s-%d%n", inventory.inventoryClass, inventory.validSlots);
						if (seed!=null)
							out.printf("   Model Seed: %s%n", seed.getSeedStr());
						if (!unknown_values.isEmpty())
							out.printf("   Unknown Values: %s%n", String.join(",", unknown_values));
					});
				
			}
			
		}
	}

	public final static class Inventories {

		private static Inventories parseInventories(SaveGameData data) {
			Inventories inventories = new Inventories();
			inventories.player.standard    = Inventories.parse(data, getObjectValue(data.json_data, "PlayerStateData", "Inventory"         ), "Player"          , "Inventory"         );
			inventories.player.tech        = Inventories.parse(data, getObjectValue(data.json_data, "PlayerStateData", "Inventory_TechOnly"), "Player (Tech)"   , "Inventory_TechOnly");
			inventories.player.cargo       = Inventories.parse(data, getObjectValue(data.json_data, "PlayerStateData", "Inventory_Cargo"   ), "Player (Cargo)"  , "Inventory_Cargo"   );
			inventories.grave              = Inventories.parse(data, getObjectValue(data.json_data, "PlayerStateData", "GraveInventory"    ), "Grave"           , "GraveInventory"    );
			inventories.ship_old           = Inventories.parse(data, getObjectValue(data.json_data, "PlayerStateData", "ShipInventory"     ), getObjectValue(data.json_data, "PlayerStateData", "ShipLayout"), "Ship (old)", "ShipInventory");
			
			inventories.chests = new Inventory[10];
			for (int i=0; i<inventories.chests.length; ++i)
				inventories.chests[i] = Inventories.parse(
					data,
					getObjectValue(data.json_data, "PlayerStateData", "Chest"+(i+1)+"Inventory"),
					getObjectValue(data.json_data, "PlayerStateData", "Chest"+(i+1)+"Layout"),
					"Container "+i, "Chest"+(i+1)+"Inventory"
				);
			inventories.baseSalvageCapsule = Inventories.parse(
				data,
				getObjectValue(data.json_data, "PlayerStateData", "ChestMagicInventory" ),
				getObjectValue(data.json_data, "PlayerStateData", "ChestMagicLayout" ),
				"Base Salvage Capsule", "ChestMagicInventory"
			);
			inventories.freighterSalvageCapsule = Inventories.parse(
				data,
				getObjectValue(data.json_data, "PlayerStateData", "ChestMagic2Inventory"),
				getObjectValue(data.json_data, "PlayerStateData", "ChestMagic2Layout"),
				"Freighter Salvage Capsule", "ChestMagic2Inventory"
			);
			if (hasValue(data.json_data, "PlayerStateData", "IngredientStorageInventory"))
				inventories.ingredientStorage = Inventories.parse(
					data,
					getObjectValue(data.json_data, "PlayerStateData", "IngredientStorageInventory"),
					getObjectValue(data.json_data, "PlayerStateData", "IngredientStorageLayout"),
					"Ingredient Storage", "IngredientStorageInventory"
				);
			
			return inventories;
		}
		
		private static Inventory parse(SaveGameData source, JSON_Object<NVExtra,VExtra> inventoryData, String inventoryLabel, String inventorySourcePath) {
			return parse(source, inventoryData, null, inventoryLabel, inventorySourcePath);
		}
		private static Inventory parse(SaveGameData source, JSON_Object<NVExtra,VExtra> inventoryData, JSON_Object<NVExtra,VExtra> inventoryLayoutData, String inventoryLabel, String inventorySourcePath) {
			if (inventoryData==null) return null;
			return new Inventory(source, inventoryData, inventoryLayoutData, inventoryLabel, inventorySourcePath);
		}

		public Player player;
		public Inventory[] chests = null;
		public Inventory baseSalvageCapsule = null;
		public Inventory freighterSalvageCapsule = null;
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
		
			public String label;
			public final String name;
			public final int width;
			public final int height;
			public final Long version;
			public final String inventoryClass;
			public final Boolean isCool;
			public final Long productMaxStorageMultiplier;
			public final Long substanceMaxStorageMultiplier;
			public final Slot[][] slots;
			public final Integer usedSlots;
			public final Integer validSlots;
			public final BaseStatValue[] baseStatValues;
			public final InventoryLayout inventoryLayout;
			public final Vector<Consumer<TextAreaOutput>> extraInfosOutputs;
	
			private Inventory(SaveGameData source, JSON_Object<NVExtra, VExtra> inventoryData, JSON_Object<NVExtra, VExtra> inventoryLayoutData, String inventoryLabel, String inventorySourcePath) {
				this.label = inventoryLabel;
				extraInfosOutputs = new Vector<>();
				
				Long preWidth, preHeight;
				substanceMaxStorageMultiplier = getIntegerValue(inventoryData, "SubstanceMaxStorageMultiplier");
				productMaxStorageMultiplier   = getIntegerValue(inventoryData, "ProductMaxStorageMultiplier");
				isCool         = getBoolValue   (inventoryData, "IsCool");
				name           = getStringValue_optional(inventoryData, "Name");
				version        = getIntegerValue(inventoryData, "Version");
				inventoryClass = getStringValue (inventoryData, "Class","InventoryClass");
				preWidth       = getIntegerValue(inventoryData, "Width");
				preHeight      = getIntegerValue(inventoryData, "Height");
				width  = preWidth ==null ? 0 : preWidth .intValue();
				height = preHeight==null ? 0 : preHeight.intValue();
				
				JSON_Array<NVExtra,VExtra> arrSlots            = getArrayValue(inventoryData,"Slots");
				JSON_Array<NVExtra,VExtra> arrValidSlotIndices = getArrayValue(inventoryData,"ValidSlotIndices");
				JSON_Array<NVExtra,VExtra> arrSpecialSlots     = getArrayValue(inventoryData,"SpecialSlots"    );
				usedSlots  = arrSlots            == null ? null : arrSlots           .size();
				validSlots = arrValidSlotIndices == null ? null : arrValidSlotIndices.size();
				slots = parseSlots(source, arrSlots, arrValidSlotIndices, arrSpecialSlots, inventoryLabel, inventorySourcePath);
				
				baseStatValues = parseBaseStatValues(getArrayValue(inventoryData,"BaseStatValues"));
				
				inventoryLayout = inventoryLayoutData==null ? null : new Inventory.InventoryLayout(inventoryLayoutData);
				
			}

			private Slot[][] parseSlots(SaveGameData source, JSON_Array<NVExtra,VExtra> arrSlots, JSON_Array<NVExtra,VExtra> arrValidSlotIndices, JSON_Array<NVExtra,VExtra> arrSpecialSlots, String inventoryLabel, String inventorySourcePath) {
				Slot[][] slots = new Slot[width][height];
				for (Slot[] row:slots)
					Arrays.fill(row, null);
				
				if (arrSlots==null) {
					Gui.log_error_ln(inventorySourcePath+": Inventory has no slots.");
					return slots;
				}
				
				if (arrValidSlotIndices==null) {
					Gui.log_error_ln(inventorySourcePath+": Inventory has no valid slot indices.");
					return slots;
				}
				
				int redundantIndices = 0;
				Vector<Value<NVExtra,VExtra>> wrongIndices = new Vector<>();
				Vector<Value<NVExtra,VExtra>> wrongSlots = new Vector<>();
				
				for (Value<NVExtra,VExtra> value:arrSlots) {
					JSON_Object<NVExtra,VExtra> slotObj = getObject(value);
					if (slotObj==null) { wrongSlots.add(value); continue; }
					
					Slot slot = new Slot(slotObj,source);
					
					if (slot.indexX==null || slot.indexX<0 || slot.indexX>=width ) { wrongSlots.add(value); continue; }
					if (slot.indexY==null || slot.indexY<0 || slot.indexY>=height) { wrongSlots.add(value); continue; }
					int x = slot.indexX.intValue();
					int y = slot.indexY.intValue();
					if (slots[x][y]!=null) { wrongSlots.add(value); ++redundantIndices; continue; }
					
					slots[x][y] = slot;
					if (slot.id!=null)
						slot.id.getUsage(source).addInventoryUsage(inventoryLabel,x,y);
					
				}
				if (!wrongSlots.isEmpty())
					Gui.log_error_ln(inventorySourcePath+": Found "+wrongSlots.size()+" wrong slots.");
				if (redundantIndices>0   )
					Gui.log_error_ln(inventorySourcePath+": Found "+redundantIndices+" redundant slots.");
				
				redundantIndices = 0;
				wrongIndices.clear();
				for (Value<NVExtra,VExtra> value:arrValidSlotIndices) {
					JSON_Object<NVExtra,VExtra> indexObj = getObject(value);
					if (indexObj==null) continue;
					Long indexX = getIntegerValue(indexObj, "X");
					Long indexY = getIntegerValue(indexObj, "Y");
					if (indexX==null || indexX<0 || indexX>=width ) { wrongIndices.add(value); continue; }
					if (indexY==null || indexY<0 || indexY>=height) { wrongIndices.add(value); continue; }
					int x = indexX.intValue();
					int y = indexY.intValue();
					if (slots[x][y]==null)
						slots[x][y] = new Slot(indexX,indexY);
					else
						slots[x][y].isValid = true;
				}
				if (!wrongIndices.isEmpty())
					Gui.log_error_ln(inventorySourcePath+": Found "+wrongIndices.size()+" wrong index(es) in \"ValidSlotIndices\".");
				if (redundantIndices>0)
					Gui.log_error_ln(inventorySourcePath+": Found "+redundantIndices+" redundant index(es) in \"ValidSlotIndices\".");
				
				redundantIndices = 0;
				wrongIndices.clear();
				for (Value<NVExtra,VExtra> value:arrSpecialSlots) {
					JSON_Object<NVExtra,VExtra> indexObj = getObject(value);
					if (indexObj==null) continue;
					Long   indexX = getIntegerValue(indexObj, "Index","X");
					Long   indexY = getIntegerValue(indexObj, "Index","Y");
					String type   = getStringValue (indexObj, "Type","InventorySpecialSlotType");
					if (indexX==null || indexX<0 || indexX>=width ) { wrongIndices.add(value); continue; }
					if (indexY==null || indexY<0 || indexY>=height) { wrongIndices.add(value); continue; }
					Slot slot = slots[indexX.intValue()][indexY.intValue()];
					if (slot==null) { wrongIndices.add(value); continue; }
					if (slot.specialSlotType != null) { ++redundantIndices; continue; }
					slot.specialSlotType = type;
				}
				if (!wrongIndices.isEmpty())
					Gui.log_error_ln(inventorySourcePath+": Found "+wrongIndices.size()+" wrong index(es) in \"SpecialSlots\".");
				if (redundantIndices>0)
					Gui.log_error_ln(inventorySourcePath+": Found "+redundantIndices+" redundant index(es) in \"SpecialSlots\".");
				
				return slots;
			}

			private BaseStatValue[] parseBaseStatValues(JSON_Array<NVExtra,VExtra> valueArray) {
				if (valueArray==null) return null;
				
				BaseStatValue[] baseStatValues = new BaseStatValue[valueArray.size()];
				for (int i=0; i<valueArray.size(); ++i) {
					JSON_Object<NVExtra,VExtra> obj = getObject(valueArray.get(i));
					if (obj==null) { baseStatValues[i]=null; continue; }
					baseStatValues[i] = new BaseStatValue(getStringValue(obj,"BaseStatID"),getFloatValue(obj,"Value"));
				}
				return baseStatValues;
			}

			public void addExtraInfos(Consumer<TextAreaOutput> extraInfosOutput) {
				if (extraInfosOutput!=null)
					extraInfosOutputs.add(extraInfosOutput);
			}

			public enum SlotType { Product, Technology, Substance }
	
			public final static class Slot {
				
				public boolean isValid;
				public final boolean isEmpty;
				public String specialSlotType;
				
				public final Long indexX;
				public final Long indexY;
				public final String idStr;
				public final GeneralizedID id;
				public final String typeStr;
				public final SlotType type;
				public final Long amount;
				public final Long maxAmount;
				public final Double damageFactor;
				public final Boolean unknownBool;
				
				public Slot(Long indexX, Long indexY) {
					this.isValid = true;
					this.isEmpty = true;
					this.specialSlotType = null;
					
					this.indexX = indexX;
					this.indexY = indexY;
					this.idStr = null;
					this.id = null;
					this.typeStr = null;
					this.type = null;
					this.amount = null;
					this.maxAmount = null;
					this.damageFactor = null;
					this.unknownBool = null;
				}

				public Slot(JSON_Object<NVExtra, VExtra> slotObj, SaveGameData source) {
					isValid = false;
					isEmpty = false;
					specialSlotType = null;
					
					typeStr      = getStringValue (slotObj, "Type","InventoryType");
					idStr        = getStringValue (slotObj, "Id");
					amount       = getIntegerValue(slotObj, "Amount");
					maxAmount    = getIntegerValue(slotObj, "MaxAmount");
					damageFactor = getFloatValue  (slotObj, "DamageFactor");
					indexX       = getIntegerValue(slotObj, "Index","X");
					indexY       = getIntegerValue(slotObj, "Index","Y");
					unknownBool  = getBoolValue_optional(slotObj, "[??? b76 InventorySlot Bool:true]");
					
					if (typeStr!=null) {
						switch(typeStr) {
						case "Product"   : type = SlotType.Product; break;
						case "Technology": type = SlotType.Technology; break;
						case "Substance" : type = SlotType.Substance; break;
						default: type = null;
						}
					} else type = null;
					
					if (type!=null && idStr!=null) {
						IDMap map = getIDMap();
						id = map==null ? null : map.get(idStr,source,GeneralizedID.Usage.Type.InventorySlot);
					} else
						id = null;
				}

				public IDMap getIDMap() {
					if (type!=null)
						switch(type) {
						case Product   : return GameInfos.productIDs;
						case Technology: return GameInfos.techIDs;
						case Substance : return GameInfos.substanceIDs;
						}
					return null;
				}
			
			}
	
			public final static class BaseStatValue {
				public final String baseStatID;
				public final Double value;
				private BaseStatValue(String baseStatID, Double value) {
					this.baseStatID = baseStatID;
					this.value = value;
				}
			}

			public static class InventoryLayout {
			
				public final Long slots;
				public final SeedValue seed;
				public final Long level;

				public InventoryLayout(JSON_Object<NVExtra, VExtra> data) {
					this.slots = getIntegerValue(data, "Slots");
					this.seed  = SeedValue.parse(data, "Seed");
					this.level = getIntegerValue(data, "Level");
				}

				@Override
				public String toString() {
					StringBuilder sb = new StringBuilder();
					if (slots!=null) sb.append(String.format("%d Slots", slots));
					if (level!=null) sb.append(String.format("%sLevel %d", sb.length()>0 ? ", " :"", level));
					if (seed !=null) sb.append(String.format("%sSeed: %s", sb.length()>0 ? ", " :"", seed.getSeedStr(false)));
					return sb.toString();
				}
			}
		}
	
	}
	
	public static class Companions {

		public final Companion[] companions;
		public final Companion[] eggs;
		public final Boolean[] isUnlocked;
		public final Vector<BlockCArray> equipment;
		
		public Companions(SaveGameData data) {
			JSON_Array<NVExtra,VExtra> arrCompanions = getArrayValue_optional(data.json_data, "PlayerStateData", "[Companions]");
			JSON_Array<NVExtra,VExtra> arrCompanionEggs = getArrayValue_optional(data.json_data, "PlayerStateData", "[CompanionEggs]");
			JSON_Array<NVExtra,VExtra> arrUnlockedCompanionSlots = getArrayValue_optional(data.json_data, "PlayerStateData", "[UnlockedCompanionSlots]");
			companions = parseCompanionArray("Companions", arrCompanions);
			eggs       = parseCompanionArray("CompanionEggs",arrCompanionEggs);
			isUnlocked = parseBoolArray("UnlockedCompanionSlots",arrUnlockedCompanionSlots);
			equipment  = parseObjectArray(Appearances.BlockCArray::new, "PlayerStateData.[??? j30]", true, data.json_data, "PlayerStateData","[??? j30]");
		}
		
		public boolean isEmpty() {
			return (companions==null || companions.length==0) &&
					(eggs==null || eggs.length==0) &&
					(isUnlocked==null || isUnlocked.length==0) &&
					(equipment==null || equipment.isEmpty());
		}

		private Boolean[] parseBoolArray(String label, JSON_Array<NVExtra, VExtra> arr) {
			if (arr==null) return null;
			
			if (arr.size()!=6) {
				Gui.log_error_ln("Companion Array \"%s\" has unexpected number of values: %d (!=6)", label, arr.size());
				return null;
			}
			
			Boolean[] result = new Boolean[arr.size()];
			for (int i=0; i<arr.size(); i++)
				result[i] = getBool(arr.get(i));
			
			return result;
		}

		private static Companion[] parseCompanionArray(String label, JSON_Array<NVExtra, VExtra> arr) {
			if (arr==null) return null;
			
			if (arr.size()!=6) {
				Gui.log_error_ln("Companion Array \"%s\" has unexpected number of values: %d (!=6)", label, arr.size());
				return null;
			}
			
			Companion[] result = new Companion[arr.size()];
			Arrays.fill(result, null);
			for (int i=0; i<arr.size(); i++) {
				JSON_Object<NVExtra, VExtra> object = getObject(arr.get(i));
				if (object==null) continue;
				result[i] = new Companion(object);
			}
			return result;
		}

		public static class Companion {

			public final String name;
			public final String type;
			public final Vector<String> bodyParts;
			public final UniverseAddress universeAddress;
			public final String originBiome;
			public final String animalRole;
			public final SeedValue seed1;
			public final SeedValue seed2;
			public final SeedValue seed3;
			public final SeedValue seed4;
			public final TimeStamp timestamp1;
			public final TimeStamp timestamp2;
			public final TimeStamp timestamp3;
			public final TimeStamp timestamp4;
			public final Boolean unknownBool_Q6I;
			public final Boolean unknownBool_eK9;
			public final Boolean unknownBool_WQX;
			public final Double height_m;
			public final Double hope;
			public final Long unknownLong_m9o;
			public final Long unknownLong_JrL;
			public final Coordinates character;
			public final Coordinates unknownCoords_IEo;

			public Companion(JSON_Object<NVExtra, VExtra> objectValue) {
				name = getStringValue(objectValue,"[UserDefinedName]");
				type = getStringValue(objectValue,"[AnimalType]");
				bodyParts = parseStringArray("Companion.[BodyParts]", objectValue, "[BodyParts]");
				universeAddress = parseUniverseAddressField(objectValue, "UA");
				originBiome = getStringValue(objectValue,"[OriginBiome]","[OriginBiome]");
				animalRole  = getStringValue(objectValue,"[AnimalRole]","[AnimalRole]");
				height_m    = getFloatValue(objectValue, "[Height]");
				hope        = getFloatValue(objectValue, "[Hope]");
				character   = Coordinates.parse(objectValue, "[Character]");
				seed1 = SeedValue.parse(getArrayValue(objectValue, "[CompanionSeed1]"));
				seed2 = SeedValue.parse(getArrayValue(objectValue, "[CompanionSeed2]"));
				seed3 = SeedValue.parse(getArrayValue(objectValue, "[CompanionSeed3]"));
				seed4 = SeedValue.parse(getArrayValue(objectValue, "[CompanionSeed4]"));
				timestamp1 = TimeStamp.create(getIntegerValue(objectValue, "[CompanionTimestamp1]"));
				timestamp2 = TimeStamp.create(getIntegerValue(objectValue, "[CompanionTimestamp2]"));
				timestamp3 = TimeStamp.create(getIntegerValue(objectValue, "[CompanionTimestamp3]"));
				timestamp4 = TimeStamp.create(getIntegerValue(objectValue, "[CompanionTimestamp4]"));
				unknownBool_Q6I  = getBoolValue(objectValue, "[??? Q6I Bool]");
				unknownBool_eK9  = getBoolValue(objectValue, "[??? eK9 Bool]");
				unknownBool_WQX  = getBoolValue(objectValue, "[??? WQX Bool]");
				unknownLong_m9o = parseHexFormatedNumber(objectValue, "[??? m9o LongAsString]");
				unknownLong_JrL = parseHexFormatedNumber(objectValue, "[??? JrL LongAsString]");
				unknownCoords_IEo = Coordinates.parse(objectValue, "[??? IEo Coordinate2]");
			}

			public static String toCharacterStr(double value, int index) {
				String characterLabel = "???";
				switch (index) {
				case 0: if (value<0) characterLabel = "Verspieltheit"; else characterLabel = "Hilfsbereitschaft"; break;
				case 1: if (value<0) characterLabel = "Sanftmut";      else characterLabel = "Aggression"; break;
				case 2: if (value<0) characterLabel = "Hingabe";       else characterLabel = "Selbständigkeit"; break;
				}
				return String.format(Locale.ENGLISH, "%1.1f%% %s", Math.abs(value)*100, characterLabel);
			}
			
		}

		public static String generateLabelForEquipment(BlockCArray value, int index) {
			return String.format("Companion %d", index+1);
		}

		public static String generateLabelForEquipmentBlock(BlockContainer value, int index) {
			return Appearances.generateLabel("Equipment Position ", null, value, index);
		}
	}

	public static class Appearances {
		private static final boolean DEBUG_LOG_VALUES = false;
		
		private static void logArray(String prefix, String arrayName, Vector<?> array) {
			if (array==null) globalUnknownValues.add(prefix, "%s = <null>", arrayName);
			else             globalUnknownValues.add(prefix, "%s.size() = %d", arrayName, array.size());
		}
	
		public final Vector<Block> playerPresets;
		public final Vector<BlockContainer> currentSets;
		
		Appearances(SaveGameData data) {
			playerPresets = parseObjectArray(Appearances.Block::new, "PlayerStateData.[??? cf5]", true, data.json_data, "PlayerStateData","[??? cf5]");
			currentSets   = parseObjectArray(Appearances.BlockContainer::new, "PlayerStateData.[??? l:j]", data.json_data, "PlayerStateData","[??? l:j]");
			
			if (DEBUG_LOG_VALUES) logArray("ExperimentalData", "array_cf5", playerPresets);
			if (DEBUG_LOG_VALUES) logArray("ExperimentalData", "array_lj" , currentSets  );
			//if (DEBUG_LOG_VALUES) logArray("ExperimentalData", "array_j30", companionEquipment_j30);
		}
		
		public boolean isEmpty() {
			return (playerPresets==null || playerPresets.isEmpty()) && (currentSets==null || currentSets.isEmpty());
		}
	
		public static String generateLabelForPreset(Block value, int index) {
			StringBuilder sb = new StringBuilder();
			sb.append("Preset");
			if (value==null) {
				sb.append(String.format("[%d]", index+1));
				sb.append(" <null>");
			} else {
				if (value.index<0 || index==value.index) sb.append(String.format("[%d]", index+1));
				else                                     sb.append(String.format("[%d|%d]", index+1, value.index+1));
				if (value.height!=null) sb.append(String.format(Locale.ENGLISH, "   height:%1.2f", value.height));
			}
			return sb.toString();
		}

		public static String generateLabelForCurrentSet(BlockContainer value, int index) {
			return generateLabel("Set", getSetLabel(index), value, index);
		}

		private static String getSetLabel(int index) {
			switch (index) {
			case 0: return "Player";
			case 1: return Exocraft.Instance.Roamer.label;
			case 9: return Exocraft.Instance.Nomad.label;
			case 10: return Exocraft.Instance.Colossus.label;
			case 11: return Exocraft.Instance.Pilgrim.label;
			case 13: return Exocraft.Instance.Nautilon.label;
			case 14: return Exocraft.Instance.Minotaurus.label;
			case 15: return "Freighter";
			}
			return null;
		}

		private static String generateLabel(String title, String containerLabel, BlockContainer value, int index) {
			StringBuilder sb = new StringBuilder();
			sb.append(title);
			if (value==null) {
				sb.append(String.format("[%d]", index+1));
				if (containerLabel!=null) sb.append(" \"").append(containerLabel).append("\"");
				sb.append(" <null>");
			} else {
				if (value.index<0 || index==value.index) sb.append(String.format("[%d]", index+1));
				else                                     sb.append(String.format("[%d|%d]", index+1, value.index+1));
				if (containerLabel!=null) sb.append(" \"").append(containerLabel).append("\"");
				if (value.id_VFd  !=null) sb.append("   ID:\"").append(value.id_VFd).append("\"");
				if (value.block   ==null) sb.append(" <null>");
				else {
					if (value.block.index >=0   ) sb.append(String.format("  [%d]", value.block.index+1));
					if (value.block.height!=null) sb.append(String.format(Locale.ENGLISH, "   height:%1.2f", value.block.height));
				}
			}
			return sb.toString();
		}

		public static class Block {
			
			public final int index;
			public final Vector<String>     array_SMP;
			public final Vector<Object_Aak> colors_Aak;
			public final Vector<Object_T1>  styles_T1;
			public final Vector<Object_gsg> array_gsg;
			public final Double height;
		
			Block(JSON_Object<NVExtra, VExtra> object, int index) {
				this.index = index;
				if (object==null) throw new IllegalArgumentException("new Appearance.Block(<null>) is not allowed");
				array_SMP  = parseStringArray("Appearance.Block.[??? SMP]", object, "[??? SMP]");
				colors_Aak = parseObjectArray(Object_Aak::new, "Appearance.Block.Aak", object, "[??? Aak]");
				styles_T1  = parseObjectArray(Object_T1 ::new, "Appearance.Block.T1" , object, "[??? T>1]");
				array_gsg  = parseObjectArray(Object_gsg::new, "Appearance.Block.gsg", object, "[??? gsg]");
				height = getFloatValue(object, "[Height]");
				if (DEBUG_LOG_VALUES) logArray("Appearance.Block", "array_SMP" , array_SMP );
				if (DEBUG_LOG_VALUES) logArray("Appearance.Block", "colors_Aak", colors_Aak);
				if (DEBUG_LOG_VALUES) logArray("Appearance.Block", "styles_T1" , styles_T1 );
				if (DEBUG_LOG_VALUES) logArray("Appearance.Block", "array_gsg" , array_gsg );
				if (DEBUG_LOG_VALUES) globalUnknownValues.add("Appearance.Block", "height = %s", height);
			}
			
			public static class Object_Aak {
				
				public final int index;
				public final String label_RVl;
				public final String type_Ty;
				public final Coordinates color_xEg;
				public final Images.NamedColor itemColor;
		
				private Object_Aak(JSON_Object<NVExtra, VExtra> object, int index) {
					this.index = index;
					color_xEg = Coordinates.parse(object, "[??? xEg]");
					label_RVl = getStringValue(object, "[??? RVl]","[??? RVl]");
					type_Ty   = getStringValue(object, "[??? RVl]","[??? Ty=]");
					itemColor = parseItemColor();
					if (DEBUG_LOG_VALUES) globalUnknownValues.add("AppearanceBlock.Object_Aak", "color_xEg = %s", color_xEg==null ? "<null>" : color_xEg.toString("%1.2f", true));
					if (DEBUG_LOG_VALUES) globalUnknownValues.add("AppearanceBlock.Object_Aak", "label_RVl = \"%s\"", label_RVl);
					if (DEBUG_LOG_VALUES) globalUnknownValues.add("AppearanceBlock.Object_Aak", "type_Ty   = \"%s\"", type_Ty);
				}
		
				private Images.NamedColor parseItemColor() {
					if (color_xEg==null) return null;
					if (color_xEg.length<3) return null;
					if (color_xEg.length>4) return null;
					int r = ((int) Math.floor(color_xEg.x*255)) & 0xFF;
					int g = ((int) Math.floor(color_xEg.y*255)) & 0xFF;
					int b = ((int) Math.floor(color_xEg.z*255)) & 0xFF;
					int value = r<<16 | g<<8 | b;
					double alpha = color_xEg.length==4 ? color_xEg.w1 : 1;
					
					return new Images.NamedColor(value, alpha, getName());
				}
		
				public String getName() {
					StringBuilder sb = new StringBuilder();
					sb.append(label_RVl!=null ? label_RVl : "<ItemColor>");
					if (type_Ty!=null) sb.append(":  ").append(type_Ty);
					String colorName = sb.toString();
					return colorName;
				}
			}
			
			public static class Object_T1 {
				
				public final int index;
				public final String item_6c;
				public final String style_Cv;
		
				private Object_T1(JSON_Object<NVExtra, VExtra> object, int index) {
					this.index = index;
					item_6c  = getStringValue(object, "[??? @6c]");
					style_Cv = getStringValue(object, "[??? =Cv]");
					if (DEBUG_LOG_VALUES) globalUnknownValues.add("AppearanceBlock.Object_T1", "item_6c  = \"%s\"", item_6c );
					if (DEBUG_LOG_VALUES) globalUnknownValues.add("AppearanceBlock.Object_T1", "style_Cv = \"%s\"", style_Cv);
				}
			}
			
			public static class Object_gsg {
				
				public final int index;
				public final String id_tIm;
				public final Double height;
		
				private Object_gsg(JSON_Object<NVExtra, VExtra> object, int index) {
					this.index = index;
					id_tIm = getStringValue(object, "[??? tIm]");
					height = getFloatValue (object, "[Height]");
					if (DEBUG_LOG_VALUES) globalUnknownValues.add("AppearanceBlock.Object_gsg", "id_tIm = \"%s\"", id_tIm);
					if (DEBUG_LOG_VALUES) globalUnknownValues.add("AppearanceBlock.Object_gsg", "height = \"%s\"", height);
				}
			}
			
		}
	
		public static class BlockContainer {
			
			public final int index;
			public final String id_VFd;
			public final Block block;
		
			BlockContainer(JSON_Object<NVExtra, VExtra> object, int index) {
				this.index = index;
				id_VFd = getStringValue(object, "[??? VFd ID?]");
				JSON_Object<NVExtra, VExtra> blockObj = getObjectValue(object, "[??? wnR]");
				block = blockObj==null ? null : new Block(blockObj,-1);
				if (DEBUG_LOG_VALUES) globalUnknownValues.add("AppearanceBlockContainer", "id_VFd = \"%s\"", id_VFd);
			}
		}
	
		public static class BlockCArray {
			
			public final int index;
			public final Vector<BlockContainer> data;
		
			public BlockCArray(JSON_Object<NVExtra, VExtra> object, int index) {
				this.index = index;
				data = parseObjectArray(BlockContainer::new, "AppearanceBlockContainerArray.Data", object, "Data");
				if (DEBUG_LOG_VALUES) logArray("AppearanceBlockContainerArray", "data", data);
			}
		}
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
			EXPLORE_TER_2 ("Heimlich mitreisender Botaniker","Erkundung: +2"),
			EXPLORE_TER_3 ("Funkteleskope"               ,"Erkundung: +3"),
			EXPLORE_TER_4 ("Fauna-Analysegerät"          ,"Kampf: +1"),
			EXPLORE_TER_5 ("Holographische Anzeigen"     ,"Erkundung: +2"),
			EXPLORE_TER_6 ("Langstreckensensoren"        ,"Erkundung: +3"),
			EXPLORE_BAD_1 ("Wandernder Kompass"          ,"Erkundung: -2"),
			
			MINING_PRI    ("Industriespezialist"          ,"Industrie: +15"),
			MINING_SEC_1  ("Laserbohranordnung"           ,"Industrie: +2"),
			MINING_SEC_2  ("Traktorstrahl"                ,"Kampf: +4"),
			MINING_SEC_3  ("Ultraschallschweißer"         ,"Industrie: +6"),
			MINING_SEC_4  ("Erzverarbeitungseinheit"      ,"Industrie: +2"),
			MINING_SEC_5  ("Terraforming-Strahlen"        ,"Industrie: +4"),
			MINING_SEC_6  ("Asteroidenpulverisierer"      ,"Industrie: +6"),
			MINING_TER_1  ("Mineralienextraktoren"        ,"Industrie: +1"),
			MINING_TER_2  ("Ausfahrbare Bohrer"           ,"Industrie: +2"),
			MINING_TER_3  ("Asteroidenscanner"            ,"Industrie: +3"),
			MINING_TER_4  ("Erntedrohnen"                 ,"Kampf: +1"),
			MINING_TER_5  ("Metalldetektor"               ,"Industrie: +2"),
			MINING_TER_6  ("Ferngesteuerte Bergbaueinheit","Industrie: +3"),
			MINING_BAD_2  ("Kleine Behälter"              ,"Industrie: -4"),
			MINING_BAD_5  ("Defekte Drohnen"              ,"Industrie: -2"),
			
			COMBAT_PRI    ("Kampfspezialist"               ,"Kampf: +15"),
			COMBAT_SEC_1  ("Gewaltige Kanonen"             ,"Kampf: +2"),
			COMBAT_SEC_2  ("Ablative Panzerung"            ,"Kampf: +4"),
			COMBAT_SEC_3  ("Tarngerät"                     ,"Kampf: +6"),
			COMBAT_SEC_4  ("Ultraschallwaffe"              ,"Kampf: +2"),
			COMBAT_SEC_5  ("Experimentelle Waffen"         ,"Kampf: +4"),
			COMBAT_SEC_6  ("Gigantische Kanonen"           ,"Kampf: +6"),
			COMBAT_TER_1  ("Versteckte Waffen"             ,"Kampf: +1"),
			COMBAT_TER_2  ("Munitionshersteller"           ,"Kampf: +2"),
			COMBAT_TER_3  ("Verstärkter Rumpf"             ,"Kampf: +3"),
			COMBAT_TER_4  ("Aggressive Sonden"             ,"Kampf: +1"),
			COMBAT_TER_5  ("Wütender Kapitän"              ,"Kampf: +2"),
			COMBAT_TER_6  ("Nachgerüstete Geschütze"       ,"Kampf: +3"),
			COMBAT_BAD_1  ("Feige Schützen"                ,"Kampf: -2"),
			COMBAT_BAD_2  ("Raketenwerfer aus zweiter Hand","Kampf: -4"),
			COMBAT_BAD_3  ("Defekte Torpedos"              ,"Kampf: -6"),
			
			TRADING_PRI   ("Handelspezialist"            ,"Handel: +15"),
			TRADING_SEC_1 ("Handelsanalysecomputer"      ,"Handel: +2"),
			TRADING_SEC_4 ("Schlafdrohnen"               ,"Handel: +2"),
			TRADING_SEC_5 ("Propagandagerät"             ,"Handel: +4"),
			TRADING_SEC_6 ("Teleportationsgerät"         ,"Handel: +6"),
			TRADING_TER_1 ("Wirtschaftsscanner"          ,"Handel: +1"),
			TRADING_TER_2 ("Ferngesteuerter Marktanalysator","Handel: +2"),
			TRADING_TER_3 ("Roboterdiener"               ,"Handel: +3"),
			TRADING_TER_4 ("Automatischer Übersetzer"    ,"Handel: +1"),
			TRADING_TER_5 ("Verhandlungsmodul"           ,"Handel: +2"),
			TRADING_TER_6 ("Gut gepflegte Crew"          ,"Handel: +3"),
			TRADING_BAD_2 ("Kleiner Frachtraum"          ,"Handel: -4"),
			
			SPEED_TER_1   ("Ortszeit-Dilator"             ,"-1% Expeditionszeit"),
			SPEED_TER_2   ("Masseantrieb"                 ,"-1% Expeditionsdauer"),
			SPEED_TER_3   ("Navigationsexperte"           ,"-2% Expeditionsdauer"),
			SPEED_TER_4   ("Warp-Antrieb"                 ,"-2% Expeditionsdauer"),
			SPEED_TER_5   ("Wurmlochgenerator"            ,"-3% Expeditionsdauer"),
			SPEED_TER_6   ("Experimenteller Impulsantrieb","-3% Expeditionsdauer"),
			SPEED_TER_7   ("Motivierte Crew"              ,"-2% Expeditionsdauer"),
			SPEED_TER_8   ("Dynamischer Ballast"         ,"-2% Expeditionsdauer "),
			
			FUEL_PRI      ("Unterstützungsspezialist"    ,"Treibstoffkosten der Expedition: -15"),
			FUEL_SEC_4    ("Antimateria-Cycler"          ,"Treibstoffkosten der Expedition: -3"),
			FUEL_SEC_5    ("Fortgeschrittener Stromverteiler","Treibstoffkosten der Expedition: -6"),
			FUEL_SEC_6    ("Tragbarer Fusionszünder"     ,"Treibstoffkosten der Expedition: -9"),
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
				Gui.log_ln("Read known Editable Frigate Modifications from file \"%s\" ...", file.getPath());
				
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
							if (mod!=null) Gui.log_warn_ln("   %-15s(%-30s,%s)", mod.modStr, '"'+mod.label+'"', '"'+mod.value+'"');
							
						}
					}
				}
				catch (FileNotFoundException e) { e.printStackTrace(); }
				catch (IOException e) { e.printStackTrace(); }
				
				if (!somethingChanged) Gui.log_warn_ln("   All values from file are already known. File \"%s\" can be deleted.", file.getPath());
				Gui.log_ln("   done (in %1.3fs)", (System.currentTimeMillis()-start)/1000.0f);
			}
	
			public static void saveKnownEditableModsToFile() {
				File file = new File(FileExport.FILE_KNOWN_EDITABLE_MODS);
				long start = System.currentTimeMillis();
				Gui.log_ln("Write known Editable Frigate Modifications to file \""+file.getPath()+"\" ...");
				
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
				
				Gui.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
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
			
			//public SeedValue modelSeed;
			//public SeedValue homeSeed;
			
			//public Long val1_5VG; // failedFights ?
			
			//public Long statVal5; // fuelCapacity_Q 
			//public Long statVal6; // speed_Q
			public Long statVal7;
			public Long statVal8;
			public Long statVal9;
			
			public Long val2_yJC;
			//public Long val3_7hK; // !=0 bei Schaden an Fregatte
			
			Unidentified() {
				//this.modelSeed = null;
				//this.homeSeed = null;
				//this.val1_5VG = null;
				//this.statVal5 = null;
				//this.statVal6 = null;
				this.statVal7 = null;
				this.statVal8 = null;
				this.statVal9 = null;
				this.val2_yJC = null;
				//this.val3_7hK = null;
			}
	
			public String getUnidentifiedLongs() {
				return String.format("%s, %s, %s | %s", /*val1_5VG, statVal5, statVal6,*/ statVal7, statVal8, statVal9, val2_yJC/*, val3_7hK*/);
			}
		}
	
		public String name = null;
		public String shipType = null;
		public String crewRace = null;
		public TimeStamp aquired = null;
		
		public Long successfulFights = null;
		public Long expeditions = null;
		public Long damages = null;
		public Long fuelConsumption = null;
		
		public Long combatValue = null;
		public Long explorationValue = null;
		public Long miningValue = null;
		public Long diplomacyValue = null;
		
		public Vector<Modification> modifications;
		public Long damageValue = null;
		
		public Unidentified unidentified;
		
		public SeedValue modelSeed = null;
		public SeedValue homeSeed = null;
		public Long failedFights_Q = null;
		public Long fuelCapacity_Q = null;
		public Long speed_Q = null;
		
		public Frigate() {
			this.modifications = new Vector<>();
			this.unidentified = new Unidentified();
		}

		private static Vector<Frigate> parse(SaveGameData data) {
			JSON_Array<NVExtra,VExtra> arrayValue = getArrayValue(data.json_data,"PlayerStateData","[Frigates]");
			if (arrayValue==null) return null;
			Vector<Value<NVExtra,VExtra>> notParsableObjects = new Vector<>();
			
			Vector<Frigate> frigates = new Vector<Frigate>();
			for (int i=0; i<arrayValue.size(); ++i) {
				Value<NVExtra,VExtra> value = arrayValue.get(i);
				JSON_Object<NVExtra,VExtra> objectValue = getObject(value);
				if (objectValue==null) {
					notParsableObjects.add(value);
					continue;
				}
				
				Frigate fr = new Frigate();
				fr.name             = getStringValue (objectValue, "[UserDefinedName]");
				fr.shipType         = getStringValue (objectValue, "[ShipType]", "[ShipType]");
				fr.crewRace         = getStringValue (objectValue, "[CrewRace]", "[CrewRaceStr]");
				fr.aquired          = TimeStamp.create(getIntegerValue(objectValue, "[?aquired?]"));
				fr.successfulFights = getIntegerValue(objectValue, "[SuccessfulFights]");
				fr.expeditions      = getIntegerValue(objectValue, "[Expeditions]");
				fr.damages          = getIntegerValue(objectValue, "[Damages]");
				fr.failedFights_Q   = getIntegerValue(objectValue, "[Failed]");
				
				fr.damageValue      = getIntegerValue(objectValue, "[DamageValue]");
				
				fr.combatValue      = getIntegerValue(objectValue, "Stats",0);
				fr.explorationValue = getIntegerValue(objectValue, "Stats",1);
				fr.miningValue      = getIntegerValue(objectValue, "Stats",2);
				fr.diplomacyValue   = getIntegerValue(objectValue, "Stats",3);
				fr.fuelConsumption  = getIntegerValue(objectValue, "Stats",4);
				fr.fuelCapacity_Q   = getIntegerValue(objectValue, "Stats",5);
				fr.speed_Q          = getIntegerValue(objectValue, "Stats",6);
				
				fr.modelSeed = SeedValue.parse( getArrayValue(objectValue, "[ModelSeed]") );
				fr.homeSeed  = SeedValue.parse( getArrayValue(objectValue, "[HomeSeed]") );
				
				fr.unidentified.statVal7 = getIntegerValue(objectValue, "Stats",7);
				fr.unidentified.statVal8 = getIntegerValue(objectValue, "Stats",8);
				fr.unidentified.statVal9 = getIntegerValue(objectValue, "Stats",9);
				
				fr.unidentified.val2_yJC = getIntegerValue(objectValue, "[??? yJC]");
				
				JSON_Array<NVExtra,VExtra> modArr = getArrayValue(objectValue,"[Modifications]");
				if (modArr!=null) {
					fr.modifications = new Vector<>();
					for (Value<NVExtra,VExtra> modVal:modArr) {
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
				Gui.log_error_ln("Found "+notParsableObjects.size()+" not parseable Frigates.");
			
			return frigates;
		}
		
	}

	private void parseFrigateMissions() {
		JSON_Array<NVExtra,VExtra> arrayValue = getArrayValue(json_data,"PlayerStateData","[RunningFrigateMissions]");
		if (arrayValue==null) return;
		Vector<Value<NVExtra,VExtra>> notParsableObjects = new Vector<>();
		
		JSON_Array<NVExtra,VExtra> array;
		frigateMissions = new Vector<>();
		for (int i=0; i<arrayValue.size(); ++i) {
			Value<NVExtra,VExtra> value = arrayValue.get(i);
			JSON_Object<NVExtra,VExtra> objectValue = getObject(value);
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
			
			JSON_Array<NVExtra,VExtra> missionTasks = getArrayValue(objectValue,"[MissionTasks]");
			if (missionTasks!=null) {
				frm.missionTasks.clear();
				for (int m=0; m<missionTasks.size(); ++m) {
					Value<NVExtra,VExtra> mtValue = missionTasks.get(m);
					JSON_Object<NVExtra,VExtra> mtObject = getObject(mtValue);
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
			Gui.log_error_ln("Found "+notParsableObjects.size()+" not parseable RunningFrigateMissions.");
	}

	public static class FrigateMission implements AddressdableObject {

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
		
		@Override public UniverseAddress getUniverseAddress() { return universeAddress; }
	}
	
	public final static class KnownSteamIDs {
		private HashMap<String,HashSet<String>> data;
		
		KnownSteamIDs() {
			data = new HashMap<>();
		}

		public HashSet<String> get(String steamID) {
			return data.get(steamID);
		}
		public void set(String steamID, String steamName) {
			HashSet<String> names = data.get(steamID);
			if (names==null)
				data.put(steamID, names = new HashSet<String>());
			names.add(steamName);
		}
		public void forEachSorted(BiConsumer<String,String> action) {
			Set<Entry<String, HashSet<String>>> entrySet = data.entrySet();
			Vector<Entry<String, HashSet<String>>> entries = new Vector<>(entrySet);
			entries.sort(Comparator.<Entry<String, HashSet<String>>, String>comparing(Entry<String, HashSet<String>>::getKey));
			entries.forEach(entry->{
				String id = entry.getKey();
				Vector<String> names = new Vector<>(entry.getValue());
				names.sort(null);
				names.forEach(name->action.accept(id,name));
			});
		}
		
		static class NameChange {
			final String steamID,newName,oldName;
			
			NameChange(String steamID, String newName, String oldName) {
				this.steamID = steamID;
				this.newName = newName;
				this.oldName = oldName;
			}
			
			@Override
			public int hashCode() {
				int h = 0;
				if (steamID!=null) h ^= steamID.hashCode();
				if (newName!=null) h ^= newName.hashCode();
				if (oldName!=null) h ^= oldName.hashCode();
				return h;
			}
			
			@Override
			public boolean equals(Object obj) {
				if (obj instanceof NameChange) {
					NameChange other = (NameChange) obj;
					return
						strEquals(this.steamID, other.steamID) &&
						strEquals(this.newName, other.newName) &&
						strEquals(this.oldName, other.oldName);
				}
				return false;
			}
			
			private boolean strEquals(String str1, String str2) {
				if (str1==null && str2==null) return true;
				if (str1==null || str2==null) return false;
				return str1.equals(str2);
			}

			@Override
			public String toString() {
				return String.format("NameChange[ ID:%s | Old:\"%s\" -> New:\"%s\" ]", steamID,oldName,newName);
			}
			
		}
		
		public static class AssignmentExistsException extends Exception {
			private static final long serialVersionUID = -9040442552016222917L;
			final NameChange nameChange;
			public AssignmentExistsException(String steamID, String newName, String oldName) {
				nameChange = new NameChange(steamID, newName, oldName);
			}
			public void printConflict() {
				Gui.log_error_ln("KnownSteamIDs:  [ID]%s  [Old]\"%s\" -> [New]\"%s\"", nameChange.steamID,nameChange.oldName,nameChange.newName);
			}
		}
		
		void writeToFile() {
			long start = System.currentTimeMillis();
			Gui.log_ln("Write KnownSteamIDs to file \""+FileExport.FILE_KNOWN_STEAM_ID+"\"...");
			try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(FileExport.FILE_KNOWN_STEAM_ID),StandardCharsets.UTF_8))) {
				Vector<String> ids = new Vector<String>(data.keySet());
				ids.sort(null);
				for (String steamID:ids) {
					Vector<String> names = new Vector<>(data.get(steamID));
					names.sort(null);
					for (String steamName:names)
						out.printf("%s=%s%n", steamID,steamName);
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			Gui.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
		}
		void readFromFile() {
			data.clear();
			long start = System.currentTimeMillis();
			try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(FileExport.FILE_KNOWN_STEAM_ID),StandardCharsets.UTF_8))) {
				Gui.log_ln("Read KnownSteamIDs from file \""+FileExport.FILE_KNOWN_STEAM_ID+"\"...");
				String line;
				while ( (line=in.readLine())!=null ) {
					int pos = line.indexOf('=');
					if (pos<0) continue;
					String steamID   = line.substring(0,pos);
					String steamName = line.substring(pos+1);
					set(steamID, steamName);
				}
				Gui.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public String getNameReplacement(String steamID) {
			HashSet<String> names = get(steamID);
			if (names==null) return steamID;
			return "[SteamID of "+toString(names)+"]";
		}
		
		public static String toString(HashSet<String> names) {
			if (names==null) return null;
			Stream<String> nameStream = names.stream().map(str->"\""+str+"\"");
			return String.join(", ",(Iterable<String>) nameStream::iterator);
		}
	}

	public final static class DiscoveryData {
		
		enum SourceArray { AvailableData, StoreData }
		
		public Vector<StoreData> storeData;
		public Vector<AvailableData> availableData;
		Vector<Value<NVExtra,VExtra>> notParsedStoreData;
		Vector<Value<NVExtra,VExtra>> notParsedAvailableData;
		public int storedDiscoveredItemOnPlanets;
		public int storedDiscoveredItemInSolarSystms;
		public int availDiscoveredItemOnPlanets;
		public int availDiscoveredItemInSolarSystms;
		
		public DiscoveryData() {
			this.storeData     = new Vector<>();
			this.availableData = new Vector<>();
			notParsedStoreData     = new Vector<>();
			notParsedAvailableData = new Vector<>();
			storedDiscoveredItemOnPlanets = 0;
			storedDiscoveredItemInSolarSystms = 0;
			availDiscoveredItemOnPlanets = 0;
			availDiscoveredItemInSolarSystms = 0;
		}

		public static DiscoveryData parse(SaveGameData data) {
			DiscoveryData discoveryData = new DiscoveryData();
			
			discoveryData.parseAvailArray(getArrayValue(data.json_data,"DiscoveryManagerData","DiscoveryData-v1","Available"));
			discoveryData.processAvailableData(data);
			
			discoveryData.parseStoreArray(getArrayValue(data.json_data,"DiscoveryManagerData","DiscoveryData-v1","Store","Record"));
			discoveryData.processStoreData(data);
			
			return discoveryData;
		}

		private void parseAvailArray(JSON_Array<NVExtra,VExtra> arrAvailable) {
			availableData.clear();
			notParsedAvailableData.clear();
			
			if (arrAvailable!=null) {
				for (Value<NVExtra,VExtra> objValue:arrAvailable) {
					JSON_Object<NVExtra,VExtra> object = getObject(objValue);
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
			
			if (!notParsedAvailableData.isEmpty())
				Gui.log_error_ln("Found "+notParsedAvailableData.size()+" not parseable DiscoveryAvailableData.");
		}

		private void parseStoreArray(JSON_Array<NVExtra,VExtra> arrStore) {
			storeData.clear();
			notParsedStoreData.clear();
			
			if (arrStore!=null) {
				for (Value<NVExtra,VExtra> objValue:arrStore) {
					JSON_Object<NVExtra,VExtra> object = getObject(objValue);
					if (object==null) { notParsedStoreData.add(objValue); continue; }
					
					StoreData stData = new StoreData();
					
					// DD.UA  UniverseAddress
					// DD.DT  String
					// DD.VP  array of (hex formated Long or direct Long)
					parseDD(getObjectValue(object,"DD"), stData.DD);
					
					// DM  empty object
					JSON_Object<NVExtra,VExtra> dmObj = getObjectValue(object,"DM");
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
					stData.RID = getStringValue_optional(object,"RID");
					if (stData.RID!=null) {
						try {
							stData.RID_bytes = Base64.getDecoder().decode(stData.RID/*.replace("\\","")*/);
							//stData.RID = Arrays.toString(stData.RID_bytes);
							//stData.RID = new String(stData.RID_bytes);
						}
						catch (IllegalArgumentException e) {}
					}
					
					// PTK  String (evtl.)
					stData.PTK = getStringValue_optional(object,"PTK");
					
					storeData.add(stData);
				}
			}
			
			if (!notParsedStoreData.isEmpty())
				Gui.log_error_ln("Found "+notParsedStoreData.size()+" not parseable DiscoveryStoreData.");
		}

		private void parseDD(JSON_Object<NVExtra,VExtra> ddObj, DDblock dd) {
			if (ddObj==null) return;
				
			// DD.UA  UniverseAddress
			dd.UA = parseUniverseAddressField(ddObj, "UA");
			
			// DD.DT  String
			dd.DT = getStringValue(ddObj,"DT");
			
			// DD.VP
			JSON_Array<NVExtra,VExtra> vpArr = getArrayValue(ddObj,"VP");
			if (vpArr!=null) {
				dd.VP = new Long[vpArr.size()];
				for (int i=0; i<vpArr.size(); ++i)
					dd.VP[i] = parseHexFormatedNumber(vpArr.get(i));
			}
		}

		private void processAvailableData(SaveGameData data) {
			availDiscoveredItemOnPlanets = 0;
			availDiscoveredItemInSolarSystms = 0;
			for (int i=0; i<availableData.size(); i++) {
				int index = i;
				AvailableData avData = availableData.get(i);
				processDD(data, avData.DD, SourceArray.AvailableData,
						(obj)->obj.foundInDiscAvail.add(index), obj->obj.containsDiscAvailObj=true,
						() -> ++availDiscoveredItemOnPlanets, () -> ++availDiscoveredItemInSolarSystms,
						null);
			}
		}

		private void processStoreData(SaveGameData data) {
			storedDiscoveredItemOnPlanets = 0;
			storedDiscoveredItemInSolarSystms = 0;
			for (int i=0; i<storeData.size(); i++) {
				int index = i;
				StoreData stData = storeData.get(i);
				processDD(data, stData.DD, SourceArray.StoreData,
						obj->obj.foundInDiscStore.add(index), obj->obj.containsDiscStoreObj=true,
						() -> ++storedDiscoveredItemOnPlanets, () -> ++storedDiscoveredItemInSolarSystms,
						discObj->processDiscoverableObject(stData,discObj));
			}
		}

		private void processDiscoverableObject(StoreData stData, Universe.DiscoverableObject discObj) {
			if (stData.OWS.userName!=null) {
				if (discObj.hasDiscoverer())
					discObj.setDiscoverer(discObj.getDiscoverer()+" | "+stData.OWS.userName);
				else
					discObj.setDiscoverer(stData.OWS.userName);
			}
			if (stData.DM_CN!=null) {
				if (discObj.hasUploadedName())
					discObj.setUploadedName(discObj.getUploadedName()+" | "+stData.DM_CN);
				else
					discObj.setUploadedName(stData.DM_CN);
			}
		}

		private void processDD(SaveGameData data, DDblock ddBlock, SourceArray sourceArray,
				Consumer<Universe.ObjectWithSource> setSource, Consumer<Universe.ObjectWithSource> setParentsSource,
				Runnable incDiscoveredItemOnPlanets, Runnable incDiscoveredItemInSolarSystms,
				Consumer<Universe.DiscoverableObject> processDiscoverableObject) {
			
			if (ddBlock.UA!=null) {
				
				Universe.ObjectWithSource obj = data.universe.getOrCreate( ddBlock.UA, setSource, setParentsSource );
				if (obj.type==Universe.Type.Planet     ) incDiscoveredItemOnPlanets.run();
				if (obj.type==Universe.Type.SolarSystem) incDiscoveredItemInSolarSystms.run();
				
				if (ddBlock.DT!=null) {
					
					switch (ddBlock.DT) {
					case "SolarSystem":
					case "Planet": 
						if (processDiscoverableObject!=null && objTypeEqualsDT(obj.type,ddBlock.DT) && obj instanceof Universe.DiscoverableObject)
							processDiscoverableObject.accept((Universe.DiscoverableObject) obj);
						obj.addDiscoveredItem(ddBlock.DT, sourceArray);
						break;
					case "Animal":
					case "Mineral":
					case "Sector":
					case "Flora":
						obj.addDiscoveredItem(ddBlock.DT, sourceArray);
						break;
					case "Interactable": // don't add DiscoveredItem
						break;
					default:
						Gui.log_error_ln("Found unknown discovery type in %s: %s", sourceArray, ddBlock.DT);
						break;
					}
				}
			}
		}

		private boolean objTypeEqualsDT(UniverseObject.Type type, String DT) {
			switch (type) {
			case Universe:
			case Galaxy:
			case Region: return false;
			case SolarSystem: return "SolarSystem".equals(DT);
			case Planet     : return "Planet"     .equals(DT);
			}
			return false;
		}

		public static class StoreData implements AddressdableObject {
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
			
			@Override public UniverseAddress getUniverseAddress() { return DD.UA; }
		}
		
		public static class AvailableData implements AddressdableObject {
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

			@Override public UniverseAddress getUniverseAddress() { return DD.UA; }
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

	public static final class ExperimentalData {
		
		private SaveGameData data;
		public Vector<StoredInteraction> storedInteractions = null;
		public Vector<MissionProgress.Mission> missionProgress = null;
		public Vector<MarkerStack.Marker> markerStack = null;
		public DATA_Wu_ data_Wu_ = null;
		public DATA_EQt data_EQt = null;
		public DATA_m4I data_m4I = null;
		public Vector<MaintenanceInteraction> maintenanceInteractions;
		public Vector<MaintenanceInteraction> array_VhC;
		public Vector<Data_wyZ>               array_wyZ;

		public ExperimentalData(SaveGameData data) {
			this.data = data;
		}

		public void parse() {
			storedInteractions = StoredInteraction.parse(data);
			
			missionProgress = MissionProgress.parse(data);
			markerStack = MarkerStack.parse(data);
			
			data_Wu_ = new DATA_Wu_(); data_Wu_.parse(data);
			data_EQt = new DATA_EQt(); data_EQt.parse(data);
			data_m4I = new DATA_m4I(); data_m4I.parse(data);
			
			maintenanceInteractions = parseObjectArray((o,i)->new MaintenanceInteraction(o,i, data, "MaintInt"     , "PlayerStateData.MaintenanceInteractions"), "PlayerStateData.MaintenanceInteractions", data.json_data,"PlayerStateData","MaintenanceInteractions");
			array_VhC               = parseObjectArray((o,i)->new MaintenanceInteraction(o,i, data, "MaintInt(VhC)", "PlayerStateData.[??? VhC]"              ), "PlayerStateData.[??? VhC]"              , data.json_data,"PlayerStateData","[??? VhC]");
			array_wyZ               = parseObjectArray((o,i)->new Data_wyZ(o,i,data), "PlayerStateData.[??? wyZ]", data.json_data,"PlayerStateData","[??? wyZ]");
			
			//globalOptionalValues.scanStructure(data.json_data,"PlayerStateData","MaintenanceInteractions");
			//globalOptionalValues.scanStructure(data.json_data,"PlayerStateData","[??? VhC]");
			//globalOptionalValues.scanStructure(data.json_data,"PlayerStateData","[??? wyZ]");
		}
		
		public static class Data_wyZ {
			
			public final int index;
			public final Long inventoryType;
			public final Long x_WX8;
			public final Long y_WX8;
			public final MaintenanceInteraction data_YC;

			Data_wyZ(JSON_Object<NVExtra,VExtra> object, int index, SaveGameData source) {
				this.index = index;
				JSON_Object<NVExtra, VExtra> data_YC_obj;
				inventoryType = getIntegerValue(object, "InventoryType");
				x_WX8         = getIntegerValue(object, "[??? WX8]", "X");
				y_WX8         = getIntegerValue(object, "[??? WX8]", "Y");
				data_YC_obj   = getObjectValue (object, "[??? ;YC]");
				data_YC = data_YC_obj == null ? null : new MaintenanceInteraction(data_YC_obj, -1, source, "Data_wyZ["+index+"] -> [??? ;YC]", "PlayerStateData.[??? wyZ]["+index+"].[??? ;YC]");
			}
			
		}

		public static class MaintenanceInteraction {
			
			public final int index;
			public final TimeStamp lastUpdateTimestamp;
			public final TimeStamp timestamp_FML;
			public final TimeStamp timestamp_eyv;
			public final Vector<Double> damageTimers;
			public final Long flags;
			public final Inventory inventoryContainer;

			MaintenanceInteraction(JSON_Object<NVExtra,VExtra> object, int index, SaveGameData source, String label, String sourcePath) {
				this.index = index;
				//globalOptionalValues.scan(object,"ExperimentalData.MaintenanceInteraction");
				JSON_Object<NVExtra, VExtra> inventoryContainer_obj;
				lastUpdateTimestamp    = TimeStamp.create(getIntegerValue(object, "LastUpdateTimestamp"));
				timestamp_FML          = TimeStamp.create(getIntegerValue(object, "[??? FML]"));
				timestamp_eyv          = TimeStamp.create(getIntegerValue(object, "[??? eyv]"));
				damageTimers           = parseArray(SaveGameData::getFloat, "ExperimentalData.MaintenanceInteraction.DamageTimers", object, "DamageTimers");
				flags                  = getIntegerValue(object, "Flags");
				inventoryContainer_obj = getObjectValue(object, "InventoryContainer");
				inventoryContainer = inventoryContainer_obj == null ? null : Inventories.parse(source, inventoryContainer_obj, label+"["+index+"] -> InvCont", sourcePath+"["+index+"].InventoryContainer");
			}
		}
		
		public static abstract class RawData<DataType> {
			
			public Vector<DataType> data = null;
			
			public final Object[] path;
			public final String shortLabel;
			private Supplier<DataType> createNew;
			private BiConsumer<DataType, JSON_Object<NVExtra,VExtra>> parseValues;
			
			RawData(String shortLabel, Supplier<DataType> createNew, BiConsumer<DataType, JSON_Object<NVExtra,VExtra>> parseValues, Object... path) {
				this.shortLabel = shortLabel;
				this.createNew = createNew;
				this.parseValues = parseValues;
				this.path = path;
			}
			
			void parse(SaveGameData data) {
				if (!hasValue(data.json_data,path)) return; 
				this.data = parseObjectArray(createNew, parseValues, Arrays.toString(path), data.json_data,path);
			}

			public String getTabTitel() {
				Iterator<String> it = Arrays.stream(path).<String>map(obj->obj==null?"<null>":obj.toString()).iterator();
				return String.join(" -> ",(Iterable<String>) ()->it);
			}
		}

		public static class DATA_Wu_ extends RawData<DATA_Wu_.Data> {
			
			public static class Data {
				public String value_E_X = null;
				public String value_2Fk = null;
			}
			
			DATA_Wu_() {
				super("Wu?", Data::new, (item,objectValue)->{
					item.value_E_X = getStringValue(objectValue, "[??? E=X]");
					item.value_2Fk = getStringValue(objectValue, "[??? 2Fk]");
				}, "PlayerStateData","[??? Wu?]");
			}
		}

		public static class DATA_EQt extends RawData<DATA_EQt.Data> {
			
			public static class Data {
				public String MissionID = null;
				public Long   value_oF_ = null;
			}

			DATA_EQt() {
				super("EQt", Data::new, (item,objectValue)->{
					item.MissionID = getStringValue(objectValue, "MissionID");
					item.value_oF_ = getIntegerValue(objectValue, "[??? oF@]");
				}, "PlayerStateData","[??? EQt]");
			}
		}

		public static class DATA_m4I extends RawData<DATA_m4I.Data> {
			
			public static class Data {
				public String Id;
				public String Type;
				public Long Value;
			}

			DATA_m4I() {
				super("m4I", Data::new, (item,objectValue)->{
					item.Id    =  getStringValue(objectValue, "Id"   );
					item.Type  =  getStringValue(objectValue, "Type" );
					item.Value = getIntegerValue(objectValue, "Value");
				}, "PlayerStateData","[??? m4I]");
			}
		}

		public static class StoredInteraction {
			
			private static Vector<StoredInteraction> parse(SaveGameData data) {
				return parseObjectArray(StoredInteraction::new, "PlayerStateData.StoredInteractions", data.json_data,"PlayerStateData","StoredInteractions");
			}

			public final int index;
			public final Long intXf4;
			public final Vector<Interaction> interactions;
			
			public StoredInteraction(JSON_Object<NVExtra,VExtra> objectValue, int index) {
				this.index = index;
				intXf4 = getIntegerValue_optional(objectValue, "[??? Xf4]");
				interactions = parseObjectArray(Interaction::new, "PlayerStateData.StoredInteractions[].Interactions", objectValue,"Interactions");
			}
			
			public static class Interaction implements AddressdableObject {
				
				public final int index;
				public final UniverseAddress galacticAddress;
				public final Long value;
				public final Coordinates position;
				public final PolarCoordinates gpsCoords;
				
				public Interaction(JSON_Object<NVExtra,VExtra> objectValue, int index) {
					this.index = index;
					galacticAddress = parseUniverseAddressField(objectValue, "GalacticAddress");
					value           = getIntegerValue(objectValue, "Value");
					position        = Coordinates.parse(objectValue, "Position");
					gpsCoords       = PolarCoordinates.parse(position);
				}
				
				@Override public UniverseAddress getUniverseAddress() { return galacticAddress; }
			}
		}

		public final static class MissionProgress {
			
			public final static class Mission {
				public String Mission = null;
				public Long Progress = null;
				public Long Seed = null;
				public Long Data = null;
				public Vector<Participant> Participants = null;
			}
			public final static class Participant implements AddressdableObject {
				public UniverseAddress UA = null;
				public SeedValue BuildingSeed = null;
				public Coordinates BuildingLocation = null;
				public String ParticipantType = null;
				@Override public UniverseAddress getUniverseAddress() { return UA; }
			}
			
			public static Vector<Mission> parse(SaveGameData data) {
				return parseObjectArray(Mission::new, (mission,objectValue)->{
					mission.Mission      = getStringValue (objectValue, "Mission");
					mission.Progress     = getIntegerValue(objectValue, "Progress");
					mission.Seed         = parseHexFormatedNumber(objectValue, "Seed");
					mission.Data         = getIntegerValue(objectValue, "Data");
					mission.Participants = parseObjectArray(Participant::new, (participant,objectValue2)->{
						participant.UA               = parseUniverseAddressField(objectValue2, "UA");
						participant.BuildingSeed     = SeedValue.parse( getArrayValue(objectValue2, "BuildingSeed") );
						if (hasValue(objectValue2, "BuildingLocation"))
							participant.BuildingLocation = Coordinates.parse(objectValue2, "BuildingLocation");
						participant.ParticipantType  = getStringValue (objectValue2, "ParticipantType", "ParticipantType");
					}, "MissionProgress.Participants", objectValue,"Participants");
				}, "MissionProgress", data.json_data,"PlayerStateData","MissionProgress");
			}
			
		}

		public final static class MarkerStack {
			
			public final static class Marker implements AddressdableObject {
		
				public Long Table;
				public String Event;
				public UniverseAddress galacticAddress;
				public SeedValue BuildingSeed;
				public Coordinates BuildingLocation;
				public String BuildingClass;
				public Double Time;
				public String MissionID;
				public Long MissionSeed;
				public String ParticipantType;
				
				@Override public UniverseAddress getUniverseAddress() { return galacticAddress; }
			}
		
			public static Vector<Marker> parse(SaveGameData data) {
				return parseObjectArray(Marker::new, (marker,objectValue)->{
					marker.Table            = getIntegerValue(objectValue, "Table");
					marker.Event            = getStringValue (objectValue, "Event");
					marker.galacticAddress  = parseUniverseAddressField(objectValue, "GalacticAddress");
					marker.BuildingSeed     = SeedValue.parse( getArrayValue(objectValue, "BuildingSeed") );
					marker.BuildingLocation = Coordinates.parse(objectValue, "BuildingLocation");
					marker.BuildingClass    = getStringValue (objectValue, "BuildingClass", "BuildingClass");
					marker.Time             = getFloatValue  (objectValue, "Time");
					marker.MissionID        = getStringValue (objectValue, "MissionID");
					marker.MissionSeed      = getIntegerValue(objectValue, "MissionSeed");
					marker.ParticipantType  = getStringValue (objectValue, "ParticipantType", "ParticipantType");
				}, "MarkerStack", data.json_data,"PlayerStateData","MarkerStack");
			}
		}

	}
	
	public final static class General {
		
		private SaveGameData data;
		public UniverseAddress currentUniverseAddress = null;
		public Long currentBaseGalaxy = null;
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
				if(currentUniverseAddress.isPlanet     ()) data.universe.getOrCreatePlanet     (currentUniverseAddress,obj->obj.isCurrPos=true,obj->obj.containsCurrPos=true);
				if(currentUniverseAddress.isSolarSystem()) data.universe.getOrCreateSolarSystem(currentUniverseAddress,obj->obj.isCurrPos=true,obj->obj.containsCurrPos=true);
			}
			if (hasValue(data.json_data, "PlayerStateData","[CurrentBaseGalaxy]"))
				currentBaseGalaxy = getIntegerValue( data.json_data, "PlayerStateData","[CurrentBaseGalaxy]");
			
			if (hasValue(data.json_data, "PlayerStateData","AnomalyUniverseAddress"))
				anomalyUA = parseUniverseAddressStructure(data.json_data,"PlayerStateData","AnomalyUniverseAddress");
			graveUA       = parseUniverseAddressStructure(data.json_data,"PlayerStateData","GraveUniverseAddress");
			if (    hasValue(data.json_data, "PlayerStateData","AnomalyPosition") &&
					hasValue(data.json_data, "PlayerStateData","AnomalyMatrixLookAt") && 
					hasValue(data.json_data, "PlayerStateData","AnomalyMatrixUp"))
				anomalyPos = Position.parse(getObjectValue(data.json_data, "PlayerStateData"), "AnomalyPosition", "AnomalyMatrixLookAt", "AnomalyMatrixUp"); 
			gravePos       = Position.parse(getObjectValue(data.json_data, "PlayerStateData"), "GravePosition", "GraveMatrixLookAt", "GraveMatrixUp");
			
			
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
	
	public static final class VisitedSystems {
		public static final class VisitedSystem implements AddressdableObject {
			public long addr;
			public UniverseAddress ua;
			public int extra;
			
			@Override public UniverseAddress getUniverseAddress() { return ua; }
		}
		
		public static Vector<VisitedSystem> parse(SaveGameData data) {
			return parseArray(value->{
				Long addr = getInteger(value);
				if (addr==null) return null;
				
				int voxelX = (int)( addr      & 0xFFF);
				int voxelY = (int)((addr>>12) & 0xFF);
				int voxelZ = (int)((addr>>20) & 0xFFF);
				int sysIndex = (int) ((addr>>32)&0xFFF);
				
				VisitedSystem sys = new VisitedSystem();
				sys.addr = addr;
				sys.ua = new UniverseAddress((((long)sysIndex&0xFFF)<<40) | (((long)voxelY&0xFF)<<24) | (((long)voxelZ&0xFFF)<<12) | ((long)voxelX&0xFFF));
				sys.extra = (int) ((addr>>44)&0xFFFFF);
				
				data.universe.getOrCreate(sys.ua,obj->obj.foundInVisitedSystems=true);
				
				return sys;
			}, "VisitedSystems", data.json_data, "PlayerStateData", "VisitedSystems");
		}
	}
	
	public interface AddressdableObject {
		UniverseAddress getUniverseAddress();
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
			return getVerboseNameInOneLine(universe, -1);
		}
		public String getVerboseNameInOneLine(Universe universe, int maxItems) {
			String strOut = "";
			Vector<String> verboseName = getVerboseName(universe);
			for (int i=0; i<verboseName.size() && (maxItems<0 || i<maxItems); i++)
				strOut += " "+verboseName.get(i);
			return strOut;
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
			if (this.galaxyIndex!=other.galaxyIndex) return Double.POSITIVE_INFINITY;
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
			Gui.log_ln("Universe:");
			for (Galaxy g:galaxies) {
				Gui.log_ln("\t"+g+":");
				for (Region r:g.regions) {
					Gui.log_ln("\t\t"+r+":");
					for (SolarSystem s:r.solarSystems) {
						Gui.log_ln("\t\t\t"+s+":");
						for (Planet p:s.planets)
							Gui.log_ln("\t\t\t\tPlanet "+p);
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

		public ObjectWithSource findUniverseObject(UniverseAddress ua) {
			if (ua==null) return null;
			if (ua.isPlanet     ()) return findPlanet     (ua);
			if (ua.isSolarSystem()) return findSolarSystem(ua);
			if (ua.isRegion     ()) return findRegion     (ua);
			return null;
		}
		
		public ObjectWithSource getOrCreate(UniverseAddress ua, Consumer<ObjectWithSource> setSource) {
			return getOrCreate(ua, setSource, setSource);
		}
		public ObjectWithSource getOrCreate(UniverseAddress ua, Consumer<ObjectWithSource> setSource, Consumer<ObjectWithSource> setParentsSource) {
			if (ua.isPlanet     ()) return getOrCreatePlanet     (ua,setSource,setParentsSource);
			if (ua.isSolarSystem()) return getOrCreateSolarSystem(ua,setSource,setParentsSource);
			if (ua.isRegion     ()) return getOrCreateRegion     (ua,setSource,setParentsSource);
			return getOrCreateGalaxy(ua,setSource);
		}

		public Planet getOrCreatePlanet(long address, Consumer<ObjectWithSource> setSource) {
			return getOrCreatePlanet(address, setSource, setSource);
		}
		public Planet getOrCreatePlanet(long address, Consumer<ObjectWithSource> setSource, Consumer<ObjectWithSource> setParentsSource) {
			return getOrCreatePlanet(new UniverseAddress(address),setSource,setParentsSource);
		}

		public Planet getOrCreatePlanet(UniverseAddress ua, Consumer<ObjectWithSource> setSource) {
			return getOrCreatePlanet(ua, setSource, setSource);
		}
		public Planet getOrCreatePlanet(UniverseAddress ua, Consumer<ObjectWithSource> setSource, Consumer<ObjectWithSource> setParentsSource) {
			SolarSystem solarSystem = getOrCreateSolarSystem(ua,setParentsSource);
			
			Planet planet = solarSystem.findPlanet(ua.planetIndex);
			if (planet==null) solarSystem.addPlanet(planet=new Planet(solarSystem,ua.planetIndex));
			
			setSource.accept(planet);
			return planet;
		}

		public SolarSystem getOrCreateSolarSystem(UniverseAddress ua, Consumer<ObjectWithSource> setSource) {
			return getOrCreateSolarSystem(ua, setSource, setSource);
		}
		public SolarSystem getOrCreateSolarSystem(UniverseAddress ua, Consumer<ObjectWithSource> setSource, Consumer<ObjectWithSource> setParentsSource) {
			Region region = getOrCreateRegion(ua,setParentsSource);
			
			SolarSystem solarSystem = region.findSolarSystem(ua.solarSystemIndex);
			if (solarSystem==null) region.addSolarSystem(solarSystem=new SolarSystem(region,ua.solarSystemIndex));
			
			setSource.accept(solarSystem);
			return solarSystem;
		}

		public Region getOrCreateRegion(UniverseAddress ua, Consumer<ObjectWithSource> setSource) {
			return getOrCreateRegion(ua, setSource, setSource);
		}
		public Region getOrCreateRegion(UniverseAddress ua, Consumer<ObjectWithSource> setSource, Consumer<ObjectWithSource> setParentsSource) {
			Galaxy galaxy = getOrCreateGalaxy(ua,setParentsSource);
			
			Region region = galaxy.findRegion(ua.voxelX,ua.voxelY,ua.voxelZ);
			if (region==null) galaxy.addRegion(region=new Region(galaxy,ua.voxelX,ua.voxelY,ua.voxelZ));
			
			setSource.accept(region);
			return region;
		}

		private Galaxy getOrCreateGalaxy(UniverseAddress ua, Consumer<ObjectWithSource> setSource) {
			Galaxy galaxy = findGalaxy(ua.galaxyIndex);
			if (galaxy==null) galaxies.add(galaxy=new Galaxy(this,ua.galaxyIndex));
			
			setSource.accept(galaxy);
			return galaxy;
		}

		public static class ObjectWithSource extends UniverseObject {
			
			public boolean isCurrPos = false;
			public boolean foundInStats = false;
			public boolean foundInVisitedSystems = false;
			public HashSet<Integer> foundInDiscStore = new HashSet<>();
			public HashSet<Integer> foundInDiscAvail = new HashSet<>();
			public HashSet<Integer> foundInPersistentPlayerBases = new HashSet<>();
			public HashSet<Integer> foundInBaseBuildingObjects = new HashSet<>();
			public HashSet<Integer> foundInTeleportEndpoints = new HashSet<>();
			public boolean containsCurrPos = false;
			public boolean containsTeleportEndpoints = false;
			public boolean containsBaseBuildingObjects = false;
			public boolean containsPersistentPlayerBases = false;
			public boolean containsDiscStoreObj = false;
			public boolean containsDiscAvailObj = false;
			
			public final HashMap<String,Integer> discoveredItems_Avail;
			public final HashMap<String,Integer> discoveredItems_Store;
			
			protected ObjectWithSource(Type type) {
				super(type);
				
				discoveredItems_Avail = new HashMap<>();
				discoveredItems_Store = new HashMap<>();
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
			
			public boolean isNotUploaded() {
				return !foundInDiscAvail.isEmpty();
			}
			
			public boolean hasSourceID() {
				return isCurrPos || containsCurrPos || 
						foundInStats ||
						foundInVisitedSystems ||
						!foundInDiscAvail            .isEmpty() ||
						!foundInDiscStore            .isEmpty() ||
						!foundInTeleportEndpoints    .isEmpty() ||
						!foundInBaseBuildingObjects  .isEmpty() ||
						!foundInPersistentPlayerBases.isEmpty() ||
						containsTeleportEndpoints ||
						containsBaseBuildingObjects ||
						containsPersistentPlayerBases ||
						containsDiscStoreObj || containsDiscAvailObj
						;
			}
			
			public String getSourceIDStr() {
				StringBuilder sb = new StringBuilder();
				if (containsCurrPos || isCurrPos           ) {                                    sb.append("CP"); }
				if (foundInStats                           ) { if (sb.length()>0) sb.append('|'); sb.append("St"); }
				if (foundInVisitedSystems                  ) { if (sb.length()>0) sb.append('|'); sb.append("VS"); }
				if (!foundInDiscStore            .isEmpty()) { if (sb.length()>0) sb.append('|'); sb.append("DS"); }
				if (containsDiscStoreObj                   ) { if (sb.length()>0) sb.append('|'); sb.append("DS"); }
				if (!foundInDiscAvail            .isEmpty()) { if (sb.length()>0) sb.append('|'); sb.append("DA"); }
				if (containsDiscAvailObj                   ) { if (sb.length()>0) sb.append('|'); sb.append("DA"); }
				if (!foundInTeleportEndpoints    .isEmpty()) { if (sb.length()>0) sb.append('|'); sb.append("TE"); }
				if (containsTeleportEndpoints              ) { if (sb.length()>0) sb.append('|'); sb.append("TE"); }
				if (!foundInBaseBuildingObjects  .isEmpty()) { if (sb.length()>0) sb.append('|'); sb.append("BBO"); }
				if (containsBaseBuildingObjects            ) { if (sb.length()>0) sb.append('|'); sb.append("BBO"); }
				if (!foundInPersistentPlayerBases.isEmpty()) { if (sb.length()>0) sb.append('|'); sb.append("PB"); }
				if (containsPersistentPlayerBases          ) { if (sb.length()>0) sb.append('|'); sb.append("PB"); }
				return "<"+sb.toString()+">";
			}
			
			public String getLongSourceIDStr() {
				StringBuilder sb = new StringBuilder();
				if (isCurrPos                              ) {                                     sb.append("Current Position"); }
				if (containsCurrPos                        ) { if (sb.length()>0) sb.append(", "); sb.append("Contains Current Position"); }
				if (foundInStats                           ) { if (sb.length()>0) sb.append(", "); sb.append("Status Values"); }
				if (foundInVisitedSystems                  ) { if (sb.length()>0) sb.append(", "); sb.append("Visited Systems"); }
				if (!foundInDiscStore            .isEmpty()) { if (sb.length()>0) sb.append(", "); sb.append("Discov.-Store"     +toString(foundInDiscStore)); }
				if (!foundInDiscAvail            .isEmpty()) { if (sb.length()>0) sb.append(", "); sb.append("Discov.-Avail."    +toString(foundInDiscAvail)); }
				if (!foundInPersistentPlayerBases.isEmpty()) { if (sb.length()>0) sb.append(", "); sb.append("PlayerBase"        +toString(foundInPersistentPlayerBases)); }
				if (!foundInBaseBuildingObjects  .isEmpty()) { if (sb.length()>0) sb.append(", "); sb.append("BaseBuildingObject"+toString(foundInBaseBuildingObjects)); }
				if (!foundInTeleportEndpoints    .isEmpty()) { if (sb.length()>0) sb.append(", "); sb.append("TeleportEndpoint"  +toString(foundInTeleportEndpoints)); }
				if (containsDiscStoreObj                   ) { if (sb.length()>0) sb.append(", "); sb.append("Contains Discov.Store Objects"); }
				if (containsDiscAvailObj                   ) { if (sb.length()>0) sb.append(", "); sb.append("Contains Discov.-Avail. Objects"); }
				if (containsTeleportEndpoints              ) { if (sb.length()>0) sb.append(", "); sb.append("Contains TeleportEndpoints"); }
				if (containsBaseBuildingObjects            ) { if (sb.length()>0) sb.append(", "); sb.append("Contains BaseBuildingObjects"); }
				if (containsPersistentPlayerBases          ) { if (sb.length()>0) sb.append(", "); sb.append("Contains PlayerBases"); }
				return "<"+sb.toString()+">";
			}

			private static String toString(HashSet<Integer> intSet) {
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
		
		public static final class Region extends ObjectWithSource implements AddressdableObject {
			
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

			@Override
			public UniverseAddress getUniverseAddress() {
				if (galaxy==null) return null;
				return new UniverseAddress( galaxy.galaxyIndex, voxelX,voxelY,voxelZ, 0,0 );
			}

			public boolean isReachableByTeleport() {
				for (SolarSystem sys:solarSystems) {
					if (!sys.additionalInfos.teleportEndpoints.isEmpty())
						return true;
					for (Planet planet:sys.planets) {
						if (planet.additionalInfos.isReachableByTeleport())
							return true;
					}
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
			
			protected DiscoverableObject(Type type) {
				super(type);
				
				oldOriginalName = null;
				originalName = null;
				uploadedName = null;
				
				discoverer = null;
				
				this.extraInfos = new Vector<>();
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
		
		public static final class SolarSystem extends DiscoverableObject implements AddressdableObject {
			
			public enum SystemState {
				Normal("Normal"),
				Unexplored("Unerforscht"),
				Abandoned("Aufgegeben"),
				;
				private String label;
				private SystemState(String label) {
					this.label = label;
				}
				public String getLabel() {
					return label;
				}
			}
			public enum StarClass {
				Yellow( Planet.Resources.Cu, Planet.Resources.Cu_, "G","F" ),
				Red   ( Planet.Resources.Cd, Planet.Resources.Cd_, "K","M" ),
				Green ( Planet.Resources.Em, Planet.Resources.Em_, "E" ),
				Blue  ( Planet.Resources.In, Planet.Resources.In_, "B","O" );
				
				public  final Planet.Resources defaultResource;
				public  final Planet.Resources defaultExtremeResource;
				private final String[] letters;
				
				StarClass(Planet.Resources defaultResource, Planet.Resources defaultExtremeResource, String... letters) {
					this.defaultResource = defaultResource;
					this.defaultExtremeResource = defaultExtremeResource;
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
				public boolean hasAnomaly;
				public Vector<TeleportEndpoints> teleportEndpoints; 
				
				public AdditionalInfos() {
					hasFreighter = false;
					hasAnomaly = false;
					teleportEndpoints = new Vector<>();
				}
				public boolean isEmpty() {
					return !hasFreighter && !hasAnomaly && teleportEndpoints.isEmpty();
				}
			}
			
			final Region region;
			final int solarSystemIndex;
			public final Vector<Planet> planets = new Vector<>();
			
			public Race race = null;
			public StarClass starClass = null;
			public SystemState systemState = SystemState.Normal;
			
			public int conflictLevel = -1;
			public String conflictLevelLabel = null;
			public int economyLevel = -1;
			public String economyLevelLabel = null;
			
			public Double distanceToCenter = null;
			public Integer numberOfPlanets = null;
			
			public boolean hasAtlasInterface; 
			public boolean hasBlackHole; 
			public UniverseAddress blackHoleTarget = null;
			public boolean withRemembranceTerminal = false;
			
			public AdditionalInfos additionalInfos = new AdditionalInfos();
			
			public SolarSystem(Region region, int solarSystemIndex) {
				super(Type.SolarSystem);
				this.region = region;
				this.solarSystemIndex = solarSystemIndex;
				this.hasAtlasInterface = shouldHaveAtlasInterface(); 
				this.hasBlackHole = shouldHaveBlackHole();
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
				if (systemState == SystemState.Unexplored) strRace = " <Unexplored>";
				
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
					(hasBlackHole?"<BlackHole>":null),
					(withRemembranceTerminal?"<RememTerm>":null)
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

			@Override
			public UniverseAddress getUniverseAddress() {
				if (region==null) return null;
				UniverseAddress ua = region.getUniverseAddress();
				if (ua==null) return null;
				return new UniverseAddress(ua,solarSystemIndex,0);
			}
		}
		
		public static final class Planet extends DiscoverableObject implements AddressdableObject {
			
			public enum Biome {
				Lush       ("Lush"         ,"Erdähnlich", Resources.Pf ),
				Barren     ("Barren"       ,"Trocken"   , Resources.Py ),
				Scorched   ("Scorched"     ,"heiß"      , Resources.P  ),
				Frozen     ("Frozen"       ,"Gefroren"  , Resources.CO2),
				Toxic      ("Toxic"        ,"Giftig"    , Resources.NH3),
				Irradiated ("Irradiated"   ,"Verstrahlt", Resources.U  ),
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
				public final Resources defaultResource;
				
				private Biome(String name_EN, String name_DE) {
					this(name_EN, name_DE, null);
				}
				private Biome(String name_EN, String name_DE, Resources defaultResource) {
					this.name_EN = name_EN;
					this.name_DE = name_DE;
					this.defaultResource = defaultResource;
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
				
				Pf     ("Pf" , GameInfos.substanceIDs, "^LUSH1"),
				Py     ("Py" , GameInfos.substanceIDs, "^DUSTY1"),
				P      ("P"  , GameInfos.substanceIDs, "^HOT1"),
				CO2    ("CO2", GameInfos.substanceIDs, "^COLD1"),
				NH3    ("NH3", GameInfos.substanceIDs, "^TOXIC1"),
				U      ("U"  , GameInfos.substanceIDs, "^RADIO1"),
				
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
				public Vector<UnboundBuildingObject> baseBuildingObjects;
				public Vector<PersistentPlayerBase> playerBases;
				public Vector<PersistentPlayerBase> otherPlayerBases;
				public boolean hasExocraftSummoningStation;
				public Vector<TeleportEndpoints> teleportEndpoints; 
				public Vector<TeleportEndpoints> teleportEndpointsInOtherPlayerBases; 
				
				public AdditionalInfos() {
					this.baseBuildingObjects = new Vector<>();
					this.playerBases = new Vector<>();
					this.otherPlayerBases = new Vector<>();
					this.hasExocraftSummoningStation = false;
					this.teleportEndpoints = new Vector<>();
					this.teleportEndpointsInOtherPlayerBases = new Vector<>();
				}
				public boolean isEmpty() {
					return
							baseBuildingObjects.isEmpty() &&
							playerBases.isEmpty() &&
							otherPlayerBases.isEmpty() &&
							!hasExocraftSummoningStation &&
							teleportEndpoints.isEmpty() &&
							teleportEndpointsInOtherPlayerBases.isEmpty();
				}
				
				public boolean hasOtherPlayersBase() {
					return !otherPlayerBases.isEmpty() || hasOtherPlayersBaseTeleport();
				}
				public boolean hasOtherPlayersBaseTeleport() {
					return !teleportEndpointsInOtherPlayerBases.isEmpty();
				}
				public boolean hasTeleportEndpoints() {
					return !teleportEndpoints.isEmpty();
				}
				public boolean isReachableByTeleport() {
					return hasOtherPlayersBaseTeleport() || hasTeleportEndpoints();
				}
			}
			
			public final SolarSystem solarSystem;
			public final int planetIndex;
			
			private Stats.PlanetStats stats = null;
			public Biome biome = null;
			public boolean hasExtremeBiome = false;
			public boolean areSentinelsAggressive = false;
			public boolean withWater = false;
			public boolean withBigGeography = false;
			public boolean withGravitinoBalls = false;
			public boolean withRemembranceTerminal = false;
			public BuriedTreasure buriedTreasure = null;
			public AdditionalInfos additionalInfos = new AdditionalInfos();
			public EnumSet<Resources> resources = EnumSet.noneOf(Resources.class);
			public PolarCoordinates portalPos = null;
			
			public Planet(SolarSystem solarSystem, int planetIndex) {
				super(Type.Planet);
				this.solarSystem = solarSystem;
				this.planetIndex = planetIndex;
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
						(withBigGeography?"<BigGeo>":null),
						(withGravitinoBalls?"<Grav>":null),
						(withRemembranceTerminal?"<RememTerm>":null),
						(buriedTreasure==null?null:"<"+buriedTreasure.name_EN+">")
					);
					if (!strExtraInfo.isEmpty()) strExtraInfo = " ("+strExtraInfo+")";
				}
				
				return strName+strExtraInfo+strDataName;
			}

			@Override
			public UniverseAddress getUniverseAddress() {
				if (solarSystem==null) return null;
				UniverseAddress ua = solarSystem.getUniverseAddress();
				if (ua==null) return null;
				return new UniverseAddress(ua,planetIndex);
			}
		}
	}

	private KnownWords parseKnownWords(String arrLabel, String wordLabel, String racesLabel) {
		if (!hasValue(json_data,"PlayerStateData",arrLabel)) return null;
		JSON_Array<NVExtra,VExtra> arrayValue = getArrayValue(json_data,"PlayerStateData",arrLabel);
		KnownWords array;
		if (arrayValue==null)
			array = null;
		else {
			array = new KnownWords().parse(arrayValue, wordLabel, racesLabel);
			if (!array.notParsedKnownWords.isEmpty())
				Gui.log_error_ln("Found "+array.notParsedKnownWords.size()+" not parseable KnownWords.");
		}
		return array; 
	}

	public static final class KnownWords {

		public Vector<KnownWord> wordList;
		Vector<Value<NVExtra,VExtra>> notParsedKnownWords;
		public int[] wordCounts;

		public KnownWords() {
			this.wordList = new Vector<>();
			this.wordCounts = null;
			notParsedKnownWords = new Vector<>();
		}
	
		public KnownWords parse(JSON_Array<NVExtra,VExtra> knownWordsArray, String wordLabel, String racesLabel) {
			for (Value<NVExtra,VExtra> knownWordValue : knownWordsArray) {
				JSON_Object<NVExtra,VExtra> knownWordObj = getObject(knownWordValue);
				if (knownWordObj==null) { notParsedKnownWords.add(knownWordValue); continue; }
				
				KnownWord knownWord = new KnownWord();
				
				knownWord.word = getStringValue(knownWordObj,wordLabel);
				if (knownWord.word==null) { notParsedKnownWords.add(knownWordValue); continue; }
				
				JSON_Array<NVExtra,VExtra> races = getArrayValue(knownWordObj,racesLabel);
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
		JSON_Array<NVExtra,VExtra> arrayValue = getArrayValue(json_data,"PlayerStateData","Stats");
		if (arrayValue==null) { stats = null; return; }
		stats = new Stats(this);
		stats.parse(arrayValue);
		if (!stats.notParsedStats.isEmpty())
			Gui.log_error_ln("Found "+stats.notParsedStats.size()+" not parseable Stats.");
	}

	public final static class Stats {
		
		public Vector<StatValue> globalStats;
		public Vector<PlanetStats> planetStats;
		public Vector<OtherStats> otherStats;
		Vector<Value<NVExtra, VExtra>> notParsedStats;
		private final SaveGameData data;

		public Stats(SaveGameData data) {
			this.data = data;
			globalStats = null;
			planetStats = new Vector<>();
			otherStats = new Vector<>();
			notParsedStats = new Vector<>();
		}

		public void parse(JSON_Array<NVExtra,VExtra> statList) {
			for (Value<NVExtra,VExtra> groupValue : statList) {
				JSON_Object<NVExtra,VExtra> group = getObject(groupValue);
				if (group==null) { notParsedStats.add(groupValue); continue; }
				
				String groupID = getStringValue(group,"GroupId");
				if (groupID==null) { notParsedStats.add(groupValue); continue; }
				
				JSON_Array<NVExtra,VExtra> groupStats = getArrayValue(group,"Stats");
				if (groupStats==null) { notParsedStats.add(groupValue); continue; }
				
				switch(groupID) {
				case "^GLOBAL_STATS":
					if (globalStats!=null) { notParsedStats.add(groupValue); continue; }
					
					getIntegerValue(group,"Address"); // -> wasProcessed
					globalStats = new Vector<>();
					fillInto(groupStats,globalStats);
					
					break;
					
				case "^PLANET_STATS": {
					Value<NVExtra,VExtra> addressValue = group.getValue("Address");
					if (addressValue==null) { notParsedStats.add(groupValue); continue; }
					
					long addressLong = -1;
					switch(addressValue.type) {
					case String:
						StringValue<NVExtra, VExtra> stringValue = addressValue.castToStringValue();
						if (stringValue==null) { notParsedStats.add(groupValue); continue; }
						String addressStr = stringValue.value;
						if (!isPlanetAddressOK(addressStr)) { notParsedStats.add(groupValue); continue; }
						stringValue.extra.wasProcessed=true;
						addressLong = Long.parseLong(addressStr.substring(2), 16);
						break;
					case Integer:
						IntegerValue<NVExtra, VExtra> integerValue = addressValue.castToIntegerValue();
						if (integerValue==null) { notParsedStats.add(groupValue); continue; }
						integerValue.extra.wasProcessed=true;
						addressLong = integerValue.value;
						break;
					default:
						{ notParsedStats.add(groupValue); continue; }
					}
					
					
					Universe.Planet planet = data.universe.getOrCreatePlanet(addressLong,obj->obj.foundInStats = true);
					
					PlanetStats ps = new PlanetStats(planet);
					fillInto(groupStats,ps.stats);
					
					planet.setPlanetStats(ps);
					
					planetStats.add(ps);
					
				} break;
					
				default: { 
					Value<NVExtra,VExtra> addressValue = group.getValue("Address");
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

		private static void fillInto(JSON_Array<NVExtra,VExtra> stats, Vector<StatValue> statsVector) {
//			StatValue.KnownID[] knownIDs = StatValue.KnownID.values();
			for (Value<NVExtra,VExtra> value : stats) {
				JSON_Object<NVExtra,VExtra> statObject = getObject(value);
				if (statObject==null) continue;
				
				StatValue stat = new StatValue();
				
				stat.ID = getStringValue(statObject,"Id");
				if (stat.ID==null) continue;
				stat.knownID = StatValue.KnownID.findID(stat.ID);
//				for (int i=0; i<knownIDs.length; ++i)
//					if (stat.ID.equals("^"+knownIDs[i]))
//						stat.knownID = knownIDs[i];
				
				JSON_Object<NVExtra,VExtra> statValue = getObjectValue(statObject,"Value");
				if (statValue!=null) {
					stat.IntValue    = getIntegerValue_optional(statValue,"IntValue");
					stat.FloatValue  = getFloatValue_optional  (statValue,"FloatValue");
					stat.Denominator = getFloatValue_optional  (statValue,"Denominator");
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
				FIENDS_KILLED("Monsters killed"),
				FIEND_EGG("Monster Eggs collected"),
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
	
	public static Long sum(Long... values) {
		Long sum = null;
		for (Long v:values)
			if (v!=null) {
				 if (sum==null) sum = v;
				 else sum += v;
			 }
		return sum;
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
	
	public static boolean isLongHex(String str) {
		if (!str.startsWith("0x")) return false;
		if (str.length()>18) return false;
		for (int i=2; i<str.length(); ++i) {
			char ch = str.charAt(i);
			if ('0'<=ch && ch<='9') continue;
			if ('a'<=ch && ch<='f') continue;
			if ('A'<=ch && ch<='F') continue;
			return false;
		}
		return true;
	}

	static boolean hasValue(JSON_Object<NVExtra,VExtra> data, Object... path) {
		return JSON_Data.hasSubNode(data, path);
	}

	private static Value<NVExtra, VExtra> getSubNode(JSON_Object<NVExtra, VExtra> data, Object... path) {
		try {
			return JSON_Data.getSubNode(data, path);
		} catch (TraverseException e) {
			Gui.log_error_ln("PathIsNotSolvableException: %s", e.getMessage());
			return null;
		}
	}

	private static Value<NVExtra, VExtra> getSubNode(JSON_Array<NVExtra, VExtra> data, Object... path) {
		try {
			return JSON_Data.getSubNode(data, path);
		} catch (TraverseException e) {
			Gui.log_error_ln("PathIsNotSolvableException: %s", e.getMessage());
			return null;
		}
	}

	@SuppressWarnings("unused")
	private static JSON_Array <NVExtra,VExtra> getArray  (Value<NVExtra,VExtra> val) { return JSON_Data.getArrayValue  (val); }
	private static JSON_Object<NVExtra,VExtra> getObject (Value<NVExtra,VExtra> val) { return JSON_Data.getObjectValue (val); }
	private static String                      getString (Value<NVExtra,VExtra> val) { return JSON_Data.getStringValue (val); }
	private static Boolean                     getBool   (Value<NVExtra,VExtra> val) { return JSON_Data.getBoolValue   (val); }
	private static Long                        getInteger(Value<NVExtra,VExtra> val) { return JSON_Data.getIntegerValue(val); }
	private static Double                      getFloat  (Value<NVExtra,VExtra> val) { return JSON_Data.getFloatValue  (val); }

	private static Boolean                     getBoolValue   (JSON_Object<NVExtra,VExtra> obj, Object... path) { return JSON_Data.getBoolValue   (getSubNode(obj, path)); }
	private static Long                        getIntegerValue(JSON_Object<NVExtra,VExtra> obj, Object... path) { return JSON_Data.getIntegerValue(getSubNode(obj, path)); }
	private static Double                      getFloatValue  (JSON_Object<NVExtra,VExtra> obj, Object... path) { return JSON_Data.getFloatValue  (getSubNode(obj, path)); }
	private static String                      getStringValue (JSON_Object<NVExtra,VExtra> obj, Object... path) { return JSON_Data.getStringValue (getSubNode(obj, path)); }
	private static JSON_Array <NVExtra,VExtra> getArrayValue  (JSON_Object<NVExtra,VExtra> obj, Object... path) { return JSON_Data.getArrayValue  (getSubNode(obj, path)); }
	private static JSON_Object<NVExtra,VExtra> getObjectValue (JSON_Object<NVExtra,VExtra> obj, Object... path) { return JSON_Data.getObjectValue (getSubNode(obj, path)); }

	private static Boolean                     getBoolValue   (JSON_Array <NVExtra,VExtra> arr, Object... path) { return JSON_Data.getBoolValue   (getSubNode(arr, path)); }
	@SuppressWarnings("unused")
	private static Long                        getIntegerValue(JSON_Array <NVExtra,VExtra> arr, Object... path) { return JSON_Data.getIntegerValue(getSubNode(arr, path)); }
	@SuppressWarnings("unused")
	private static Double                      getFloatValue  (JSON_Array <NVExtra,VExtra> arr, Object... path) { return JSON_Data.getFloatValue  (getSubNode(arr, path)); }
	@SuppressWarnings("unused")
	private static String                      getStringValue (JSON_Array <NVExtra,VExtra> arr, Object... path) { return JSON_Data.getStringValue (getSubNode(arr, path)); }
	@SuppressWarnings("unused")
	private static JSON_Array <NVExtra,VExtra> getArrayValue  (JSON_Array <NVExtra,VExtra> arr, Object... path) { return JSON_Data.getArrayValue  (getSubNode(arr, path)); }
	@SuppressWarnings("unused")
	private static JSON_Object<NVExtra,VExtra> getObjectValue (JSON_Array <NVExtra,VExtra> arr, Object... path) { return JSON_Data.getObjectValue (getSubNode(arr, path)); }

//	private static Boolean     getBoolValue   (JSON_Array<NVExtra,VExtra> arr, int index) { return generic_getValue(new GetValueHelper.GVH_Bool   (), arr, index); }
//	@SuppressWarnings("unused") private static Long                        getIntegerValue(JSON_Array<NVExtra,VExtra> arr, int index) { return generic_getValue(new GetValueHelper.GVH_Integer(), arr, index); }
//	@SuppressWarnings("unused") private static Double                      getFloatValue  (JSON_Array<NVExtra,VExtra> arr, int index) { return generic_getValue(new GetValueHelper.GVH_Double (), arr, index); }
//	@SuppressWarnings("unused") private static String                      getStringValue (JSON_Array<NVExtra,VExtra> arr, int index) { return generic_getValue(new GetValueHelper.GVH_String (), arr, index); }
//	@SuppressWarnings("unused") private static JSON_Array <NVExtra,VExtra> getArrayValue  (JSON_Array<NVExtra,VExtra> arr, int index) { return generic_getValue(new GetValueHelper.GVH_Array  (), arr, index); }
//	@SuppressWarnings("unused") private static JSON_Object<NVExtra,VExtra> getObjectValue (JSON_Array<NVExtra,VExtra> arr, int index) { return generic_getValue(new GetValueHelper.GVH_Object (), arr, index); }

	private static Boolean                     getBoolValue_optional   (JSON_Object<NVExtra,VExtra> data, Object... path) { try { return JSON_Data.getBoolValue   (JSON_Data.getSubNode(data, path)); } catch (TraverseException e) { return null; } }
	private static Long                        getIntegerValue_optional(JSON_Object<NVExtra,VExtra> data, Object... path) { try { return JSON_Data.getIntegerValue(JSON_Data.getSubNode(data, path)); } catch (TraverseException e) { return null; } }
	private static Double                      getFloatValue_optional  (JSON_Object<NVExtra,VExtra> data, Object... path) { try { return JSON_Data.getFloatValue  (JSON_Data.getSubNode(data, path)); } catch (TraverseException e) { return null; } }
	private static String                      getStringValue_optional (JSON_Object<NVExtra,VExtra> data, Object... path) { try { return JSON_Data.getStringValue (JSON_Data.getSubNode(data, path)); } catch (TraverseException e) { return null; } }
	private static JSON_Array <NVExtra,VExtra> getArrayValue_optional  (JSON_Object<NVExtra,VExtra> data, Object... path) { try { return JSON_Data.getArrayValue  (JSON_Data.getSubNode(data, path)); } catch (TraverseException e) { return null; } }
	@SuppressWarnings("unused")
	private static JSON_Object<NVExtra,VExtra> getObjectValue_optional (JSON_Object<NVExtra,VExtra> data, Object... path) { try { return JSON_Data.getObjectValue (JSON_Data.getSubNode(data, path)); } catch (TraverseException e) { return null; } }
	
	
	public static ObjectValue<NVExtra, VExtra> createObjectValue(JSON_Object<NVExtra, VExtra> data) {
		VExtra extra = new VExtra(Value.Type.Object);
		ObjectValue<NVExtra, VExtra> host = new ObjectValue<>(data,extra);
		extra.setHost(host);
		return host;
	}
	
	public static class FactoryForExtras implements JSON_Data.FactoryForExtras<NVExtra,VExtra> {
		@Override public NVExtra createNamedValueExtra(Value.Type type) { return new NVExtra(type); }
		@Override public VExtra createValueExtra(Value.Type type) { return new VExtra(type); }
	}

	public static class NVExtra implements JSON_Data.NamedValueExtra {
		public final Value.Type type;
		public NamedValue<NVExtra, VExtra> host;
		public boolean wasDeObfuscated;
		public String originalStr;
	
		public NVExtra(Value.Type type) {
			this.type = type;
			this.host = null; 
			wasDeObfuscated = false;
			originalStr = null;
		}
		void setHost(NamedValue<NVExtra, VExtra> host) {
			this.host = host;
			if (this.host==null) new IllegalArgumentException("Host must not be <null>");
			if (this.host.value.type!=type) new IllegalArgumentException("Host has wrong type: "+this.host.value.type+"!="+type);
		}
	}

	public static class VExtra implements JSON_Data.ValueExtra {
		
		public final Value.Type type;
		public Value<NVExtra, VExtra> host;
		public boolean wasProcessed;
		public Boolean hasUnprocessedChildren;
		public Boolean hasObfuscatedChildren;
		
		public VExtra(Value.Type type) {
			this.type = type;
			this.host = null; 
			wasProcessed = false;
			hasUnprocessedChildren = type!=null && type.isSimple ? false : null;
			hasObfuscatedChildren  = type!=null && type.isSimple ? false : null;
		}
		void setHost(Value<NVExtra, VExtra> host) {
			this.host = host;
			if (this.host==null) new IllegalArgumentException("Host must not be <null>");
			if (this.host.type!=type) new IllegalArgumentException("Host has wrong type: "+this.host.type+"!="+type);
		}
		
		@Override public void markAsProcessed() {
			wasProcessed = true;
		}
		
		public boolean hasUnprocessedChildren() {
			// ArrayValue   @Override public boolean hasUnprocessedChildren() { return JSON_Data.hasUnprocessedChildren(this,this.value, v-> v      ); }
			// ObjectValue  @Override public boolean hasUnprocessedChildren() { return JSON_Data.hasUnprocessedChildren(this,this.value,nv->nv.value); }
			if (type==Value.Type.Array ) {
				if (host==null)
					throw new IllegalStateException();
				if (host.castToArrayValue()==null)
					throw new IllegalStateException();
				return hasUnprocessedChildren(host,host.castToArrayValue ().value, v-> v      );
			}
			if (type==Value.Type.Object) {
				if (host==null)
					throw new IllegalStateException();
				if (host.castToObjectValue()==null)
					throw new IllegalStateException();
				return hasUnprocessedChildren(host,host.castToObjectValue().value,nv->nv.value);
			}
			return false;
		}
		public boolean hasObfuscatedChildren() {
			// ArrayValue   @Override public boolean hasObfuscatedChildren () { return JSON_Data.hasObfuscatedChildren (this,this.value, v-> v      , v->true              ); }
			// ObjectValue  @Override public boolean hasObfuscatedChildren () { return JSON_Data.hasObfuscatedChildren (this,this.value,nv->nv.value,nv->nv.wasDeObfuscated); }
			if (type==Value.Type.Array ) return hasObfuscatedChildren(host,host.castToArrayValue ().value, v-> v      , v->true                    );
			if (type==Value.Type.Object) return hasObfuscatedChildren(host,host.castToObjectValue().value,nv->nv.value,nv->nv.extra.wasDeObfuscated);
			return false;
		}
		
		private static <ChildType> boolean hasUnprocessedChildren(JSON_Data.Value<NVExtra,VExtra> baseValue, Vector<ChildType> children, Function<ChildType,JSON_Data.Value<NVExtra,VExtra>> getValue) {
			if (baseValue.extra.hasUnprocessedChildren!=null) return baseValue.extra.hasUnprocessedChildren;
			baseValue.extra.hasUnprocessedChildren=false;
			for (ChildType child:children) {
				JSON_Data.Value<NVExtra,VExtra> childValue = getValue.apply(child);
				if (!childValue.extra.wasProcessed || childValue.extra.hasUnprocessedChildren()) {
					baseValue.extra.hasUnprocessedChildren=true;
					break;
				}
			}
			return baseValue.extra.hasUnprocessedChildren;
		}
		private static <ChildType> boolean hasObfuscatedChildren(JSON_Data.Value<NVExtra,VExtra> baseValue, Vector<ChildType> children, Function<ChildType,JSON_Data.Value<NVExtra,VExtra>> getValue, Function<ChildType,Boolean> wasDeObfuscated) {
			if (baseValue.extra.hasObfuscatedChildren!=null) return baseValue.extra.hasObfuscatedChildren;
			baseValue.extra.hasObfuscatedChildren=false;
			for (ChildType child:children) {
				JSON_Data.Value<NVExtra,VExtra> childValue = getValue.apply(child);
				if (!wasDeObfuscated.apply(child) || childValue.extra.hasObfuscatedChildren()) {
					baseValue.extra.hasObfuscatedChildren=true;
					break;
				}
			}
			return baseValue.extra.hasObfuscatedChildren;
		}
	}
}
