package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.Vector;

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

	Error error;
	String errorMessage;
	private boolean isStackTraceEnabled;
	
	final JSON_Object json_data;
	final General general;
	Universe universe;
	Stats stats;
	KnownWords knownWords;
	DiscoveryData discoveryData;
	
	public SaveGameData(JSON_Object json_data) {
		error = Error.NoError;
		errorMessage = "";
		isStackTraceEnabled = true;
		
		this.json_data = json_data;
		this.general = new General(this);
		this.universe = new Universe();
		this.stats = null;
		this.knownWords = null;
		this.discoveryData = new DiscoveryData(this);
	}
	
	public SaveGameData parse() {
		general.parse();
		parseStats();
		parseKnownWords();
		parseDiscoveryData();
		universe.sort();
		//universe.writeToConsole();
		return this;
	}

	private void parseStats() {
		JSON_Array arrayValue = getArrayValue(json_data,"PlayerStateData","Stats");
		if (arrayValue==null)
			stats = null;
		else {
			stats = new Stats(this).parse(arrayValue);
			if (!stats.notParsedStats.isEmpty())
				System.out.println("Found "+stats.notParsedStats.size()+" not parseable Stats.");
		}
	}

	private void parseKnownWords() {
		JSON_Array arrayValue = getArrayValue(json_data,"PlayerStateData","KnownWords");
		if (arrayValue==null)
			knownWords = null;
		else {
			knownWords = new KnownWords(this).parse(arrayValue);
			if (!knownWords.notParsedKnownWords.isEmpty())
				System.out.println("Found "+knownWords.notParsedKnownWords.size()+" not parseable KnownWords.");
		}
	}

	private void parseDiscoveryData() {
		JSON_Array arrayValue_Store = getArrayValue(json_data,"DiscoveryManagerData","DiscoveryData-v1","Store","Record");
		JSON_Array arrayValue_Available = getArrayValue(json_data,"DiscoveryManagerData","DiscoveryData-v1","Available");
		
		discoveryData.parse(arrayValue_Store,arrayValue_Available);
		
		if (!discoveryData.notParsedStoreData.isEmpty())
			System.out.println("Found "+discoveryData.notParsedStoreData.size()+" not parseable DiscoveryStoreData.");
		if (!discoveryData.notParsedAvailableData.isEmpty())
			System.out.println("Found "+discoveryData.notParsedAvailableData.size()+" not parseable DiscoveryAvailableData.");
	}

	final static class DiscoveryData {
		
		private SaveGameData data;
		Vector<StoreData> storeData;
		Vector<AvailableData> availableData;
		JSON_Array notParsedStoreData;
		JSON_Array notParsedAvailableData;
		
		public DiscoveryData(SaveGameData data) {
			this.data = data;
			this.storeData = new Vector<>();
			this.availableData = new Vector<>();
			notParsedStoreData     = new JSON_Array();
			notParsedAvailableData = new JSON_Array();
		}
		
		public void parse(JSON_Array arrStore, JSON_Array arrAvailable) {
			if (arrStore!=null) {
				for (Value objValue:arrStore) {
					if (!isOK(objValue, Type.Object)) { notParsedAvailableData.add(objValue); continue; }
					JSON_Object object = ((ObjectValue)objValue).value;
					
					StoreData stData = new StoreData();
					
					// DD.UA  UniverseAddress
					// DD.DT  String
					// DD.VP[0]  String
					// DD.VP[1]  String | long
					parseDD(data.getObjectValue(object,"DD"), stData.DD);
					
					// DM  empty object
					JSON_Object dmObj = data.getObjectValue(object,"DM");
					if (dmObj!=null && !dmObj.isEmpty()) {
						stData.DM = "{"+dmObj.size()+"}";
						stData.DM_CN = data.getStringValue(dmObj,"CN");
					}
					
					// OWS
					//    LID  String
					//    UID  String
					//    USN  String
					//    TS   Long
					JSON_Object owsObj = data.getObjectValue(object,"OWS");
					if (owsObj!=null) {
						stData.OWS_LID = data.getStringValue(owsObj,"LID");
						stData.OWS_UID = data.getStringValue(owsObj,"UID");
						stData.OWS_USN = data.getStringValue(owsObj,"USN");
						stData.OWS_TS  = data.getIntegerValue(owsObj,"TS");
					}
					
					// RID  String (evtl.)
					stData.RID = data.getStringValue_silent(object,"RID");
					if (stData.RID!=null) {
						try {
							stData.RID_bytes = Base64.getDecoder().decode(stData.RID/*.replace("\\","")*/);
							//stData.RID = Arrays.toString(stData.RID_bytes);
							//stData.RID = new String(stData.RID_bytes);
						}
						catch (IllegalArgumentException e) {}
					}
					
					storeData.add(stData);
				}
			}
			
			if (arrAvailable!=null) {
				for (Value objValue:arrAvailable) {
					if (!isOK(objValue, Type.Object)) { notParsedAvailableData.add(objValue); continue; }
					JSON_Object object = ((ObjectValue)objValue).value;
					
					AvailableData availData = new AvailableData();
					
					// TSrec  long
					availData.TSrec = data.getIntegerValue(object,"TSrec");
					
					// DD.UA  UniverseAddress
					// DD.DT  String
					// DD.VP[0]  String
					// DD.VP[1]  String | long
					parseDD(data.getObjectValue(object,"DD"), availData.DD);
					
					availableData.add(availData);
				}
			}
		}

		private void parseDD(JSON_Object ddObj, DDblock dd) {
			if (ddObj==null) return;
				
			// DD.UA  UniverseAddress
			Value addressValue = ddObj.getValue("UA");
			if (addressValue!=null) {
				switch(addressValue.type) {
				case String:
					if (addressValue instanceof StringValue) {
						String addressStr = ((StringValue)addressValue).value;
						if (isPlanetAddressOK(addressStr))
							dd.UA = new UniverseAddress( Long.parseLong(addressStr.substring(2), 16) );
					}
					break;
					
				case Integer:
					if (addressValue instanceof IntegerValue)
						dd.UA = new UniverseAddress( ((IntegerValue)addressValue).value );
					break;
					
				default:
					break;
				}
			}
			
			// DD.DT  String
			dd.DT = data.getStringValue(ddObj,"DT");
			
			// DD.VP
			JSON_Array vpArr = data.getArrayValue(ddObj,"VP");
			if (vpArr!=null) {
				
				// DD.VP[0]  String
				if (vpArr.size()>0) {
					Value value = vpArr.get(0);
					switch (value.type) {
					case Integer: dd.VP0 = ""+((IntegerValue)value).value; break;
					case String : dd.VP0 = ((StringValue)value).value; break;
					default:
						break;
					}
				}
				
				// DD.VP[1]  String | long
				if (vpArr.size()>1) {
					Value value = vpArr.get(1);
					switch (value.type) {
					case Integer: dd.VP1 = ""+((IntegerValue)value).value; break;
					case String : dd.VP1 = ((StringValue)value).value; break;
					default:
						break;
					}
				}
			}
		}
		
		static class StoreData {
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
			
			DDblock DD;
			String DM;
			String DM_CN;
			String OWS_LID;
			String OWS_UID;
			String OWS_USN;
			Long   OWS_TS;
			String RID;
			byte[] RID_bytes;
			
			public StoreData() {
				DD = new DDblock();
				DM = null;
				DM_CN = null;
				OWS_LID = null;
				OWS_UID = null;
				OWS_USN = null;
				OWS_TS = null;
				RID = null;
				RID_bytes = null;
			}
		}
		
		static class AvailableData {
			// TSrec  long
			// DD.UA  UniverseAddress
			// DD.DT  String
			// DD.VP[0]  String
			// DD.VP[1]  String | long
			
			Long TSrec;
			DDblock DD;
			
			AvailableData() {
				TSrec = null;
				DD = new DDblock();
			}
		}
		
		static class DDblock {
			UniverseAddress UA;
			String DT;
			String VP0;
			String VP1;
			
			DDblock() {
				UA = null;
				DT = null;
				VP0 = null;
				VP1 = null;
			}
		}
	}

	final static class General {
		
		private SaveGameData data;
		private UniverseAddress currentUniverseAddress;
		
		public General(SaveGameData data) {
			this.data = data;
		}
		
		public void parse() {
			currentUniverseAddress =
					parseUniverseAddress(
							data.getObjectValue(data.json_data,"PlayerStateData","UniverseAddress")
							);
		}

		private UniverseAddress parseUniverseAddress(JSON_Object universeAddressObj) {
			if (universeAddressObj==null) return null;
			
			UniverseAddress newUA = new UniverseAddress(0);
			
			Long galaxyIndexLong = data.getIntegerValue(universeAddressObj,"RealityIndex");
			if (galaxyIndexLong==null) return null;
			newUA.galaxyIndex = (int)(long)galaxyIndexLong;
			
			JSON_Object galacticAddressObj = data.getObjectValue(universeAddressObj,"GalacticAddress");
			if (galacticAddressObj==null) return null;
			
			Long voxelXLong = data.getIntegerValue(galacticAddressObj,"VoxelX");
			Long voxelYLong = data.getIntegerValue(galacticAddressObj,"VoxelY");
			Long voxelZLong = data.getIntegerValue(galacticAddressObj,"VoxelZ");
			if (voxelXLong==null) return null;
			if (voxelYLong==null) return null;
			if (voxelZLong==null) return null;
			newUA.voxelX = (int)(long)voxelXLong;
			newUA.voxelY = (int)(long)voxelYLong;
			newUA.voxelZ = (int)(long)voxelZLong;
			
			Long solarSystemIndexLong = data.getIntegerValue(galacticAddressObj,"SolarSystemIndex");
			if (solarSystemIndexLong==null) return null;
			newUA.solarSystemIndex = (int)(long)solarSystemIndexLong;
			
			Long planetIndexLong = data.getIntegerValue(galacticAddressObj,"PlanetIndex");
			if (planetIndexLong==null) return null;
			newUA.planetIndex = (int)(long)planetIndexLong;
			
			return newUA;
		}

		public UniverseAddress getCurrentUniverseAddress() { return currentUniverseAddress; }
		
		public Long getUnits          () { return data.getIntegerValue( data.json_data, "PlayerStateData","Units"           ); }
		public Long getPlayerHealth   () { return data.getIntegerValue( data.json_data, "PlayerStateData","Health"          ); }
		public Long getPlayerShield   () { return data.getIntegerValue( data.json_data, "PlayerStateData","Shield"          ); }
		public Long getShipHealth     () { return data.getIntegerValue( data.json_data, "PlayerStateData","ShipHealth"      ); }
		public Long getShipShield     () { return data.getIntegerValue( data.json_data, "PlayerStateData","ShipShield"      ); }
		public Long getTimeAlive      () { return data.getIntegerValue( data.json_data, "PlayerStateData","TimeAlive"       ); }
		public Long getTotalPlayTime  () { return data.getIntegerValue( data.json_data, "PlayerStateData","TotalPlayTime"   ); }
		public Long getHazardTimeAlive() { return data.getIntegerValue( data.json_data, "PlayerStateData","HazardTimeAlive" ); }
		
		public Boolean     getTestBool   (Object... path) { return data.getBoolValue   (data.json_data, path); }
		public Long        getTestInteger(Object... path) { return data.getIntegerValue(data.json_data, path); }
		public Double      getTestFloat  (Object... path) { return data.getFloatValue  (data.json_data, path); }
		public String      getTestString (Object... path) { return data.getStringValue (data.json_data, path); }
		public JSON_Array  getTestArray  (Object... path) { return data.getArrayValue  (data.json_data, path); }
		public JSON_Object getTestObject (Object... path) { return data.getObjectValue (data.json_data, path); }
	}

	static final class UniverseAddress implements Comparable<UniverseAddress> {

		int galaxyIndex;
		int voxelX,voxelY,voxelZ;
		int solarSystemIndex;
		int planetIndex;
		
		public UniverseAddress(long address) {
			voxelX = (int)( address      & 0xFFF);
			voxelZ = (int)((address>>12) & 0xFFF);
			voxelY = (int)((address>>24) & 0xFF);
			galaxyIndex    = (int)((address>>32) & 0xFF);
			solarSystemIndex = (int)((address>>40) & 0xFFF);
			planetIndex      = (int)((address>>52) & 0xF);
			if (voxelX>2047) voxelX -= 4096;
			if (voxelY> 127) voxelY -=  256;
			if (voxelZ>2047) voxelZ -= 4096;
		}

		public UniverseAddress(int galacticIndex, int voxelX, int voxelY, int voxelZ, int solarSystemIndex, int planetIndex) {
			this.galaxyIndex = galacticIndex;
			this.voxelX = voxelX;
			this.voxelY = voxelY;
			this.voxelZ = voxelZ;
			this.solarSystemIndex = solarSystemIndex;
			this.planetIndex = planetIndex;
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

		public String getCoordinates() {
			return String.format("%d | %d,%d,%d | %d | %d", galaxyIndex, voxelX, voxelY, voxelZ, solarSystemIndex, planetIndex);
		}

		public String getSolarSystemCoordinates() {
			return String.format("%d | %d,%d,%d | %d", galaxyIndex, voxelX, voxelY, voxelZ, solarSystemIndex);
		}

		public String getGalacticRegionCoordinates() {
			return String.format("%d | %d,%d,%d", galaxyIndex, voxelX, voxelY, voxelZ);
		}

		public String getReducedSigBoostCode() {
			return String.format("%04X:%04X:%04X", voxelX+2047, voxelY+127, voxelZ+2047);
		}

		public String getSigBoostCode() {
			return String.format("%s:%04X", getReducedSigBoostCode(), solarSystemIndex);
		}

		public String getExtendedSigBoostCode() {
			return String.format("G%d|%s|P%d", galaxyIndex, getSigBoostCode(), planetIndex);
		}

		public long getAddress() {
			long address = (((long)voxelY&0xFF)<<24) | (((long)voxelZ&0xFFF)<<12) | ((long)voxelX&0xFFF);
			address = address | (((long)galaxyIndex   &0xFF )<<32);
			address = address | (((long)solarSystemIndex&0xFFF)<<40);
			address = address | (((long)planetIndex     &0xFF )<<52);
			return address;
		}

		public long getPortalGlyphCode() {
			long portalGlyphCode = (((long)voxelY&0xFF)<<24) | (((long)voxelZ&0xFFF)<<12) | ((long)voxelX&0xFFF);
			portalGlyphCode |= ((long)solarSystemIndex&0xFFF)<<32;
			portalGlyphCode |= ((long)planetIndex     &0xFF )<<44;
			return portalGlyphCode;
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
	
	static final class Universe {

		final Vector<Galaxy> galaxies;
		
		Universe() {
			galaxies = new Vector<>();
		}
		
		@Override
		public String toString() {
			return "Universe";
		}


		public void sort() {
			galaxies.sort(Comparator.comparing(g -> g.galacticIndex));
			for (Galaxy g:galaxies) {
				g.galacticRegions.sort(Comparator.comparing((GalacticRegion gr) -> gr.voxelY).thenComparing(Comparator.comparing((GalacticRegion gr) -> gr.voxelY)).thenComparing(Comparator.comparing((GalacticRegion gr) -> gr.voxelZ)));
				for (GalacticRegion gr:g.galacticRegions) {
					gr.solarSystems.sort(Comparator.comparing(sys -> sys.solarSystemIndex));
					for (SolarSystem sys:gr.solarSystems) {
						sys.planets.sort(Comparator.comparing(p -> p.planetIndex));
					}
				}
			}
		}


		public void writeToConsole() {
			System.out.println("Universe:");
			for (Galaxy g:galaxies) {
				System.out.println("\t"+g+":");
				for (GalacticRegion gr:g.galacticRegions) {
					System.out.println("\t\t"+gr+":");
					for (SolarSystem sys:gr.solarSystems) {
						System.out.println("\t\t\t"+sys+":");
						for (Planet p:sys.planets)
							System.out.println("\t\t\t\tPlanet "+p);
					}
				}
			}
		}


		public Planet findPlanet(UniverseAddress uAddr) {
			SolarSystem solarSystem = findSolarSystem(uAddr);
			if (solarSystem==null) return null;
			
			return solarSystem.findPlanet(uAddr.planetIndex);
		}

		public SolarSystem findSolarSystem(UniverseAddress uAddr) {
			Galaxy galaxy = findGalaxy(uAddr.galaxyIndex);
			if (galaxy==null) return null;
			
			GalacticRegion galacticRegion = galaxy.findRegion(uAddr.voxelX,uAddr.voxelY,uAddr.voxelZ);
			if (galacticRegion==null) return null;
			
			return galacticRegion.findSolarSystem(uAddr.solarSystemIndex);
		}

		public Planet getOrCreatePlanet(long address) {
			UniverseAddress uAddr = new UniverseAddress(address);
			return getOrCreatePlanet(uAddr);
		}

		public Planet getOrCreatePlanet(UniverseAddress uAddr) {
			SolarSystem solarSystem = getOrCreateSolarSystem(uAddr);
			
			Planet planet = solarSystem.findPlanet(uAddr.planetIndex);
			if (planet==null) solarSystem.addPlanet(planet=new Planet(solarSystem,uAddr.planetIndex));
			
			return planet;
		}

		public SolarSystem getOrCreateSolarSystem(UniverseAddress uAddr) {
			Galaxy galaxy = findGalaxy(uAddr.galaxyIndex);
			if (galaxy==null) galaxies.add(galaxy=new Galaxy(this,uAddr.galaxyIndex));
			
			GalacticRegion galacticRegion = galaxy.findRegion(uAddr.voxelX,uAddr.voxelY,uAddr.voxelZ);
			if (galacticRegion==null) galaxy.addRegion(galacticRegion=new GalacticRegion(galaxy,uAddr.voxelX,uAddr.voxelY,uAddr.voxelZ));
			
			SolarSystem solarSystem = galacticRegion.findSolarSystem(uAddr.solarSystemIndex);
			if (solarSystem==null) galacticRegion.addSolarSystem(solarSystem=new SolarSystem(galacticRegion,uAddr.solarSystemIndex));
			
			return solarSystem;
		}

		private Galaxy findGalaxy(int galacticIndex) {
			for (Galaxy g:galaxies)
				if (g.galacticIndex==galacticIndex)
					return g;
			return null;
		}

		static final class Galaxy {
			
			final Universe universe;
			final int galacticIndex;
			final Vector<GalacticRegion> galacticRegions;
			
			public Galaxy(Universe universe, int galacticIndex) {
				this.universe = universe;
				this.galacticIndex = galacticIndex;
				this.galacticRegions = new Vector<>();
			}

			@Override
			public String toString() {
				return "Galaxy "+galacticIndex;
			}

			public void addRegion(GalacticRegion galacticRegion) {
				galacticRegions.add(galacticRegion);
			}

			public GalacticRegion findRegion(int voxelX, int voxelY, int voxelZ) {
				for (GalacticRegion gr:galacticRegions)
					if (gr.voxelX==voxelX && gr.voxelY==voxelY && gr.voxelZ==voxelZ)
						return gr;
				return null;
			}
		}
		
		static final class GalacticRegion {
			
			final Galaxy galaxy;
			final int voxelX,voxelY,voxelZ;
			final Vector<SolarSystem> solarSystems;
			
			public GalacticRegion(Galaxy galaxy, int x, int y, int z) {
				this.galaxy = galaxy;
				this.voxelX = x;
				this.voxelY = y;
				this.voxelZ = z;
				this.solarSystems = new Vector<>();
			}

			@Override
			public String toString() {
				return "Region "+voxelX+","+voxelY+","+voxelZ;
			}

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
				return new UniverseAddress( galaxy.galacticIndex, voxelX,voxelY,voxelZ, 0,0 );
			}
		}
		
		static final class SolarSystem {
			
			final GalacticRegion galacticRegion;
			final int solarSystemIndex;
			private String name;
			final Vector<Planet> planets;
			
			public SolarSystem(GalacticRegion galacticRegion, int solarSystemIndex) {
				this.galacticRegion = galacticRegion;
				this.solarSystemIndex = solarSystemIndex;
				this.planets = new Vector<>();
				this.name = null; 
			}

			public boolean hasName() { return name!=null; }
			public String getName() { return name; }
			public void setName(String name) { this.name = name; }

			@Override
			public String toString() {
				if (hasName())
					return String.format("SolarSystem %03X (%d) - \"%s\"", solarSystemIndex, solarSystemIndex, name);
				else
					return String.format("SolarSystem %03X (%d)", solarSystemIndex, solarSystemIndex);
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
				if (galacticRegion==null) return null;
				UniverseAddress ua = galacticRegion.getUniverseAddress();
				ua.solarSystemIndex = solarSystemIndex;
				return ua;
			}
		}
		
		static final class Planet {
			
			final SolarSystem solarSystem;
			final int planetIndex;
			private String name;
			private Stats.PlanetStats stats;
			
			public Planet(SolarSystem solarSystem, int planetIndex) {
				this.solarSystem = solarSystem;
				this.planetIndex = planetIndex;
				this.name = null; 
			}

			public boolean hasName() { return name!=null; }
			public String getName() { return name; }
			public void setName(String name) { this.name = name; }

			public void setPlanetStats(Stats.PlanetStats stats) {
				this.stats = stats;
			}

			@Override
			public String toString() {
				UniverseAddress ua = getUniverseAddress();
				if (ua!=null) return ua.getExtendedSigBoostCode() + (hasName()?" - \""+name+"\"":"");
				return "Planet [planetIndex=" + planetIndex + (hasName()?", name=\""+name +"\"":"") + ", solarSystem=" + solarSystem + ", stats=" + stats + "]";
			}

			public UniverseAddress getUniverseAddress() {
				if (solarSystem==null) return null;
				UniverseAddress ua = solarSystem.getUniverseAddress();
				ua.planetIndex = planetIndex;
				return ua;
			}
		}
	}

	static final class KnownWords {

		private final SaveGameData data;
		Vector<KnownWord> wordList;
		JSON_Array notParsedKnownWords;
		int[] wordCounts;

		public KnownWords(SaveGameData data) {
			this.data = data;
			this.wordList = new Vector<>();
			this.wordCounts = null;
			notParsedKnownWords = new JSON_Array();
		}
	
		public KnownWords parse(JSON_Array knownWordsArray) {
			for (Value knownWordValue : knownWordsArray) {
				if (!isOK(knownWordValue, Type.Object)) { notParsedKnownWords.add(knownWordValue); continue; }
				JSON_Object knownWordObj = ((ObjectValue)knownWordValue).value;
				
				KnownWord knownWord = new KnownWord();
				
				knownWord.word = data.getStringValue(knownWordObj,"Word");
				if (knownWord.word==null) { notParsedKnownWords.add(knownWordValue); continue; }
				
				JSON_Array races = data.getArrayValue(knownWordObj,"Races");
				if (races==null) { notParsedKnownWords.add(knownWordValue); continue; }
				knownWord.races = new boolean[races.size()];
				
				boolean errorOccured = false;
				for (int i=0; i<knownWord.races.length; ++i) {
					Value raceBoolValue = races.get(i);
					if (!isOK(raceBoolValue, Type.Bool)) { errorOccured=true; break; }
					knownWord.races[i] = ((BoolValue)raceBoolValue).value;
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
		
		
		static class KnownWord implements Comparable<KnownWord>{
			
			String word;
			boolean[] races;
			
			@Override
			public int compareTo(KnownWord o) {
				return this.word.compareTo(o.word);
			}
		}
	}
	
	final static class Stats {
		
		Vector<StatValue> globalStats;
		Vector<PlanetStats> planetStats;
		JSON_Array notParsedStats;
		private final SaveGameData data;

		public Stats(SaveGameData data) {
			this.data = data;
			globalStats = null;
			planetStats = new Vector<>();
			notParsedStats = new JSON_Array();
			StatValue.KnownID.setOrderNumbers();
		}

		public Stats parse(JSON_Array statList) {
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
					
					long addressLong = -1;
					switch(addressValue.type) {
					case String:
						if (!(addressValue instanceof StringValue)) { notParsedStats.add(groupValue); continue; }
						String addressStr = ((StringValue)addressValue).value;
						if (!isPlanetAddressOK(addressStr)) { notParsedStats.add(groupValue); continue; }
						addressLong = Long.parseLong(addressStr.substring(2), 16);
						break;
					case Integer:
						if (!(addressValue instanceof IntegerValue)) { notParsedStats.add(groupValue); continue; }
						addressLong = ((IntegerValue)addressValue).value;
						break;
					default:
						{ notParsedStats.add(groupValue); continue; }
					}
					
					
					Universe.Planet planet = data.universe.getOrCreatePlanet(addressLong);
					
					PlanetStats ps = new PlanetStats(planet);
					fillInto(groupStats,ps.stats);
					
					planet.setPlanetStats(ps);
					
					planetStats.add(ps);
					
					break;
					
				default:
					{ notParsedStats.add(groupValue); continue; }
				}
			}
			return this;
		}

		private void fillInto(JSON_Array stats, Vector<StatValue> statsVector) {
			StatValue.KnownID[] knownIDs = StatValue.KnownID.values();
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
				TIME, DEATHS, LONGEST_LIFE, LONGEST_LIFE_EX, TIMES_IN_SPACE,
				GLOBAL_MISSION, ATLAS_PATH, ATLAS_STORY,
				DIST_WALKED("Distance walked"),
				DIST_SWAM  ("Distance swam"),
				DIST_FLY   ("Distance flown"),
				ALIENS_MET ("Aliens met"),
				WORDS_LEARNT("Words learnt"),
				TRA_STANDING("Gek"    +" standing"), TSEEN_SYSTEMS("Gek"    +" systems seen"), TWORDS_LEARNT("Gek"    +" words learnt"), TDONE_MISSIONS("Gek"    +" missions done"),
				EXP_STANDING("Korvax" +" standing"), ESEEN_SYSTEMS("Korvax" +" systems seen"), EWORDS_LEARNT("Korvax" +" words learnt"), EDONE_MISSIONS("Korvax" +" missions done"),
				WAR_STANDING("Vy'keen"+" standing"), WSEEN_SYSTEMS("Vy'keen"+" systems seen"), WWORDS_LEARNT("Vy'keen"+" words learnt"), WDONE_MISSIONS("Vy'keen"+" missions done"),
				TGUILD_STAND("Traders"  +" guild standing"), TGDONE_MISSIONS("Traders"  +" guild missions done"), MONEY("Units earned"), PLANTS_PLANTED("Plants planted"),
				EGUILD_STAND("Explorers"+" guild standing"), EGDONE_MISSIONS("Explorers"+" guild missions done"), DIST_WARP  ("Number of warps"), RARE_SCANNED("Rare creatures scanned"),
				WGUILD_STAND("Warriors" +" guild standing"), WGDONE_MISSIONS("Warriors" +" guild missions done"), SENTINEL_KILLS("Sentinels killed (all)"), ENEMIES_KILLED("Enemies killed"),
				DRONES_KILLED("Sentinel drones killed"), QUADS_KILLED("Sentinel quads killed"), WALKERS_KILLED("Sentinel walkers killed"),
				POLICE_KILLED("Sentinel ships killed"),
				PIRATES_KILLED("Pirates killed"),
				PREDS_KILLED, CREATURES_KILL,
				DISC_ALL_CREATU("Planets, where all creatures were found"),
				DISC_FLORA, DISC_CREATURES, DISC_MINERALS, DISC_PLANETS, DISC_WAYPOINTS,
				TUTORIAL,
				TECH_BOUGHT, SHIPS_BOUGHT,
				DEPOTS_BROKEN,
				FPODS_BROKEN,
				EARLY_WARPS,
				BLACKHOLE_WARPS,
				ITEMS_TELEPRT,
				STATION_VISITED,
				SPACE_BATTLES,
				PHOTO_MODE_USED,
				ARTIFACT_HINTS,
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
		
		static class PlanetStats {
			
			final Universe.Planet planet;
			Vector<StatValue> stats;
			
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

	enum Error { NoError, UnexpectedType, PathIsNotSolvable, ValueIsNull }

	private void enableStackTrace(boolean isStackTraceEnabled) {
		this.isStackTraceEnabled = isStackTraceEnabled;
	}

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
			if (isStackTraceEnabled) e.printStackTrace();
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

	private String getStringValue_silent(JSON_Object data, Object... path) {
		enableStackTrace(false);
		String value = getStringValue(data, path);
		enableStackTrace(true);
		return value;
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
}
