package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.util.Arrays;
import java.util.Vector;

import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Stats.StatValue.KnownID;
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

	final JSON_Object json_data;
	Error error;
	String errorMessage;
	Stats stats;

	public SaveGameData(JSON_Object json_data) {
		this.json_data = json_data;
		parseStats();
	}
	
	private void parseStats() {
		JSON_Array arrayValue = getArrayValue(json_data,"PlayerStateData","Stats");
		if (arrayValue==null)
			stats = null;
		else
			stats = new Stats(this,arrayValue);
	}

	final static class Stats {
		
		Vector<StatValue> globalStats;
		Vector<PlanetStats> planetStats;
		JSON_Array notParsedStats;
		private final SaveGameData data;

		public Stats(SaveGameData data, JSON_Array statList) {
			this.data = data;
			globalStats = null;
			planetStats = new Vector<>();
			KnownID.setOrderNumbers();
			
			for (Value groupValue : statList) {
				if (!isOK(groupValue,Type.Object)) { notParsedStats.add(groupValue); continue; }
				JSON_Object group = ((ObjectValue)groupValue).value;
				
				String groupID = data.getStringValue(group,"GroupId");
				if (groupID==null) { notParsedStats.add(groupValue); continue; }
				
				JSON_Array groupStats = data.getArrayValue(group,"Stats");
				if (groupStats==null) { notParsedStats.add(groupValue); continue; }
				
				switch(groupID) {
				case "^GLOBAL_STATS":
					if (globalStats!=null) { notParsedStats.add(groupValue); continue; }
					
					globalStats = new Vector<>();
					fillInto(groupStats,globalStats);
					
					break;
					
				case "^PLANET_STATS":
					Value addressValue = group.getValue("Address");
					if (addressValue==null) { notParsedStats.add(groupValue); continue; }
					
					String address = null;
					switch(addressValue.type) {
					case String:
						if (!(addressValue instanceof StringValue)) { notParsedStats.add(groupValue); continue; }
						address = ((StringValue)addressValue).value;
						break;
					case Integer:
						if (!(addressValue instanceof IntegerValue)) { notParsedStats.add(groupValue); continue; }
						Long value = ((IntegerValue)addressValue).value;
						address = String.format("0x%014X (%d)", value, value);
						break;
					default:
						{ notParsedStats.add(groupValue); continue; }
					}
					
					planetStats.add(new PlanetStats(address,groupStats));
					
					break;
					
				default:
					{ notParsedStats.add(groupValue); continue; }
				}
			}
		}

		private void fillInto(JSON_Array stats, Vector<StatValue> statsVector) {
			KnownID[] knownIDs = StatValue.KnownID.values();
			for (Value value : stats) {
				if (!isOK(value,Type.Object)) continue;
				JSON_Object statObject = ((ObjectValue)value).value;
				
				StatValue stat = new StatValue();
				
				stat.ID = data.getStringValue(statObject,"Id");
				if (stat.ID==null) continue;
				for (int i=0; i<knownIDs.length; ++i)
					if (stat.ID.equals("^"+knownIDs[i]))
						stat.knownID = knownIDs[i];
				
				JSON_Object statValue = data.getObjectValue(statObject,"Value");
				if (statValue==null) continue;
				
				Long statIntValue = data.getIntegerValue(statValue,"IntValue");
				if (statIntValue==null) continue;
				stat.IntValue = statIntValue;
				
				Double statFloatValue = data.getFloatValue(statValue,"FloatValue");
				if (statFloatValue==null) continue;
				stat.FloatValue = statFloatValue;
				
				Double statDenominator = data.getFloatValue(statValue,"Denominator");
				if (statDenominator==null) continue;
				stat.Denominator = statDenominator;
				
				statsVector.add(stat);
			}
			
			statsVector.sort(null);
		}

		static class StatValue implements Comparable<StatValue> {
			enum KnownID {
				MONEY("Units"),
				TIME, DEATHS, LONGEST_LIFE, LONGEST_LIFE_EX, TIMES_IN_SPACE,
				GLOBAL_MISSION, ATLAS_PATH, ATLAS_STORY,
				DIST_WALKED("Distance walked"),
				DIST_SWAM  ("Distance swam"),
				DIST_FLY   ("Distance flown"),
				DIST_WARP  ("Number of warps"),
				ALIENS_MET ("Aliens met"),
				WORDS_LEARNT("Words learnt"),
				TRA_STANDING("Gek"    +" standing"), TSEEN_SYSTEMS("Gek"    +" Systems seen"), TWORDS_LEARNT("Gek"    +" words learnt"), TDONE_MISSIONS("Gek"    +" missions done"),
				EXP_STANDING("Korvax" +" standing"), ESEEN_SYSTEMS("Korvax" +" Systems seen"), EWORDS_LEARNT("Korvax" +" words learnt"), EDONE_MISSIONS("Korvax" +" missions done"),
				WAR_STANDING("Vy'keen"+" standing"), WSEEN_SYSTEMS("Vy'keen"+" Systems seen"), WWORDS_LEARNT("Vy'keen"+" words learnt"), WDONE_MISSIONS("Vy'keen"+" missions done"),
				TGUILD_STAND("Traders"  +" guild standing"), TGDONE_MISSIONS("Traders"  +" guild missions done"),
				EGUILD_STAND("Explorers"+" guild standing"), EGDONE_MISSIONS("Explorers"+" guild missions done"),
				WGUILD_STAND("Warriors" +" guild standing"), WGDONE_MISSIONS("Warriors" +" guild missions done"),
				SENTINEL_KILLS("Sentinels killed (all)"), DRONES_KILLED("Sentinel drones killed"), QUADS_KILLED("Sentinel quads killed"), WALKERS_KILLED("Sentinel walkers killed"),
				POLICE_KILLED, PIRATES_KILLED, ENEMIES_KILLED,
				PREDS_KILLED, CREATURES_KILL,
				DISC_ALL_CREATU, DISC_FLORA, DISC_CREATURES, DISC_MINERALS, DISC_PLANETS, DISC_WAYPOINTS,
				TUTORIAL,
				TECH_BOUGHT, SHIPS_BOUGHT,
				DEPOTS_BROKEN,
				FPODS_BROKEN,
				EARLY_WARPS,
				BLACKHOLE_WARPS,
				ITEMS_TELEPRT,
				STATION_VISITED,
				RARE_SCANNED,
				SPACE_BATTLES,
				PHOTO_MODE_USED,
				ARTIFACT_HINTS,
				PLANTS_PLANTED,
				RES_EXTRACTED,
				SALVAGE_LOOTED,
				VISIT_EXT_BASE,
				
				// only in planet stats
				ALL_CREATURES;
				
				public String fullName;
				public int orderNumber;
				
				KnownID() {
					this.fullName = ""; //toString();
					this.orderNumber = 0; //KnownID.values().length;
				}
				KnownID(String name) {
					this.fullName = name;
				}
				
				public static void setOrderNumbers() {
					KnownID[] values = values();
					for (int i=0; i<values.length; ++i)
						values[i].orderNumber = i;
				}
			}
			
			String ID;
			KnownID knownID;
			long IntValue;
			double FloatValue;
			double Denominator;
			
			
			public StatValue() {
				this.ID = null;
				this.knownID = null;
				this.IntValue = 0;
				this.FloatValue = 0;
				this.Denominator = 0;
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
				return this.knownID.orderNumber - other.knownID.orderNumber;
			}
		}
		
		class PlanetStats {
			
			final String address;
			Vector<StatValue> stats;
			
			PlanetStats(String address, JSON_Array groupStats) {
				this.address = address;
				this.stats = new Vector<>();
				fillInto(groupStats,stats);
			}
		}
	
	}
	
	private static boolean isOK(Value value, Type expectedType) {
		if (value==null) return false;
		if (value.type!=expectedType) return false;
		switch(value.type) {
		case Bool   : return (value instanceof BoolValue);
		case Float  : return (value instanceof FloatValue);
		case Integer: return (value instanceof IntegerValue);
		case String : return (value instanceof StringValue);
		case Array  : return (value instanceof ArrayValue);
		case Object : return (value instanceof ObjectValue);
		}
		return false;
	}
	
	enum Error { NoError, UnexpectedType, PathIsNotSolvable, ValueIsNull }

	private Value getValue(JSON_Object data, Object... path) {
		Value value = null;
		try {
			value = JSON_Data.getSubNode(data,path);
			error = Error.NoError;
			errorMessage = "";
			if (value==null) {
				error = Error.ValueIsNull;
				errorMessage = String.format("Value is null. (path: %s)", Arrays.toString(path));
			}
		} catch (PathIsNotSolvableException e) {
			e.printStackTrace();
			error = Error.PathIsNotSolvable;
			errorMessage = "PathIsNotSolvable: "+Arrays.toString(path);
		}
		return value;
	}

	private Boolean getBoolValue(JSON_Object data, Object... path) {
		Value value = getValue(data,path);
		if (value==null) return null;
		if (value instanceof BoolValue) {
			BoolValue realValue = (BoolValue)value;
			error = Error.NoError;
			errorMessage = "";
			return realValue.value;
		} else {
			error = Error.UnexpectedType;
			errorMessage = String.format("Value has not the expected type (%s). %s type found. (path: %s)", Type.Bool, value.type, Arrays.toString(path));
			return null;
		}
	}

	private Long getIntegerValue(JSON_Object data, Object... path) {
		Value value = getValue(data, path);
		if (value==null) return null;
		if (value instanceof IntegerValue) {
			IntegerValue realValue = (IntegerValue)value;
			error = Error.NoError;
			errorMessage = "";
			return realValue.value;
		} else {
			error = Error.UnexpectedType;
			errorMessage = String.format("Value has not the expected type (%s). %s type found. (path: %s)", Type.Integer, value.type, Arrays.toString(path));
			return null;
		}
	}

	private Double getFloatValue(JSON_Object data, Object... path) {
		Value value = getValue(data, path);
		if (value==null) return null;
		if (value instanceof FloatValue) {
			FloatValue realValue = (FloatValue)value;
			error = Error.NoError;
			errorMessage = "";
			return realValue.value;
		} else {
			error = Error.UnexpectedType;
			errorMessage = String.format("Value has not the expected type (%s). %s type found. (path: %s)", Type.Float, value.type, Arrays.toString(path));
			return null;
		}
	}

	private String getStringValue(JSON_Object data, Object... path) {
		Value value = getValue(data, path);
		if (value==null) return null;
		if (value instanceof StringValue) {
			StringValue realValue = (StringValue)value;
			error = Error.NoError;
			errorMessage = "";
			return realValue.value;
		} else {
			error = Error.UnexpectedType;
			errorMessage = String.format("Value has not the expected type (%s). %s type found. (path: %s)", Type.String, value.type, Arrays.toString(path));
			return null;
		}
	}

	private JSON_Array getArrayValue(JSON_Object data, Object... path) {
		Value value = getValue(data, path);
		if (value==null) return null;
		if (value instanceof ArrayValue) {
			ArrayValue realValue = (ArrayValue)value;
			error = Error.NoError;
			errorMessage = "";
			return realValue.value;
		} else {
			error = Error.UnexpectedType;
			errorMessage = String.format("Value has not the expected type (%s). %s type found. (path: %s)", Type.Array, value.type, Arrays.toString(path));
			return null;
		}
	}

	private JSON_Object getObjectValue(JSON_Object data, Object... path) {
		Value value = getValue(data, path);
		if (value==null) return null;
		if (value instanceof ObjectValue) {
			ObjectValue realValue = (ObjectValue)value;
			error = Error.NoError;
			errorMessage = "";
			return realValue.value;
		} else {
			error = Error.UnexpectedType;
			errorMessage = String.format("Value has not the expected type (%s). %s type found. (path: %s)", Type.Object, value.type, Arrays.toString(path));
			return null;
		}
	}

	public Long getUnits          () { return getIntegerValue( json_data, "PlayerStateData","Units"           ); }
	public Long getPlayerHealth   () { return getIntegerValue( json_data, "PlayerStateData","Health"          ); }
	public Long getPlayerShield   () { return getIntegerValue( json_data, "PlayerStateData","Shield"          ); }
	public Long getShipHealth     () { return getIntegerValue( json_data, "PlayerStateData","ShipHealth"      ); }
	public Long getShipShield     () { return getIntegerValue( json_data, "PlayerStateData","ShipShield"      ); }
	public Long getTimeAlive      () { return getIntegerValue( json_data, "PlayerStateData","TimeAlive"       ); }
	public Long getTotalPlayTime  () { return getIntegerValue( json_data, "PlayerStateData","TotalPlayTime"   ); }
	public Long getHazardTimeAlive() { return getIntegerValue( json_data, "PlayerStateData","HazardTimeAlive" ); }
	
	public Boolean     getTestBool   (Object... path) { return getBoolValue   (json_data, path); }
	public Long        getTestInteger(Object... path) { return getIntegerValue(json_data, path); }
	public Double      getTestFloat  (Object... path) { return getFloatValue  (json_data, path); }
	public String      getTestString (Object... path) { return getStringValue (json_data, path); }
	public JSON_Array  getTestArray  (Object... path) { return getArrayValue  (json_data, path); }
	public JSON_Object getTestObject (Object... path) { return getObjectValue (json_data, path); }
}
