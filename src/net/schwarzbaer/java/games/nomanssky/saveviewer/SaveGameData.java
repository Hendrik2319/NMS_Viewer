package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Inventory.BaseStatValue;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Inventory.Slot;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Planet;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.SolarSystem;
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

	public Error error;
	public String errorMessage;
	private boolean isStackTraceEnabled;
	
	public final JSON_Object json_data;
	public final General general;
	public Universe universe;
	public Stats stats;
	public KnownWords knownWords;
	public DiscoveryData discoveryData;
	public Inventories inventories;
	
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
		this.inventories = null;
	}
	
	public SaveGameData parse() {
		general.parse();
		parseStats();
		parseKnownWords();
		parseDiscoveryData();
		parseInventories();
		universe.sort();
		//universe.writeToConsole();
		return this;
	}

	private void parseInventories() {
		inventories = null;
		JSON_Object playerStateData = getObjectValue(json_data, "PlayerStateData");
		if (playerStateData==null) return;
		inventories = new Inventories();
		inventories.player        = Inventories.parse(this,getObjectValue(playerStateData, "Inventory"         ), "Inventory"         );
		inventories.playerTech    = Inventories.parse(this,getObjectValue(playerStateData, "Inventory_TechOnly"), "Inventory_TechOnly");
		inventories.playerCargo   = Inventories.parse(this,getObjectValue(playerStateData, "Inventory_Cargo"   ), "Inventory_Cargo"   );
		inventories.ship_old      = Inventories.parse(this,getObjectValue(playerStateData, "ShipInventory"     ), "ShipInventory"     );
		inventories.multitool     = Inventories.parse(this,getObjectValue(playerStateData, "WeaponInventory"   ), "WeaponInventory"   );
		inventories.grave         = Inventories.parse(this,getObjectValue(playerStateData, "GraveInventory"    ), "GraveInventory"    );
		inventories.freighter     = Inventories.parse(this,getObjectValue(playerStateData, "FreighterInventory"), "FreighterInventory");
		inventories.freighterTech = Inventories.parse(this,getObjectValue(playerStateData, "FreighterInventory_TechOnly"), "FreighterInventory_TechOnly");
		
		inventories.chests = new Inventory[10];
		for (int i=0; i<inventories.chests.length; ++i)
			inventories.chests[i] = Inventories.parse(this,getObjectValue(playerStateData, "Chest"+(i+1)+"Inventory"), "Chest"+(i+1)+"Inventory");
		inventories.magicChest  = Inventories.parse(this,getObjectValue(playerStateData, "ChestMagicInventory" ), "ChestMagicInventory" );
		inventories.magicChest2 = Inventories.parse(this,getObjectValue(playerStateData, "ChestMagic2Inventory"), "ChestMagic2Inventory");
		
		inventories.vehicles = null;
		JSON_Array vehicles = getArrayValue(playerStateData,"VehicleOwnership");
		if (vehicles!=null) {
			inventories.vehicles = new Inventory[vehicles.size()];
			for (int i=0; i<vehicles.size(); ++i) {
				JSON_Object vehicleData = Value.getObject(vehicles.get(i));
				inventories.vehicles[i] = null;
				if (vehicleData != null)
					inventories.vehicles[i] = Inventories.parse(this,getObjectValue(vehicleData,"Inventory"),"VehicleOwnership["+i+"].Inventory");
			}
		}
		
		inventories.ships = null;
		inventories.ships_Tech = null;
		JSON_Array ships = getArrayValue(playerStateData,"ShipOwnership");
		if (ships!=null) {
			inventories.ships = new Inventory[ships.size()];
			inventories.ships_Tech = new Inventory[ships.size()];
			for (int i=0; i<ships.size(); ++i) {
				JSON_Object shipData = Value.getObject(ships.get(i));
				inventories.ships     [i] = null;
				inventories.ships_Tech[i] = null;
				if (shipData != null) {
					inventories.ships     [i] = Inventories.parse(this,getObjectValue(shipData,"Inventory"         ),"ShipOwnership["+i+"].Inventory");
					inventories.ships_Tech[i] = Inventories.parse(this,getObjectValue(shipData,"Inventory_TechOnly"),"ShipOwnership["+i+"].Inventory_TechOnly");
				}
			}
		}		
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
		
		discoveryData.parseJsonArrays(arrayValue_Store,arrayValue_Available);
		discoveryData.findPlanetsAndSolarSystems();
		discoveryData.findAdditionalPlanetsAndSolarSystems();
		
		if (!discoveryData.notParsedStoreData.isEmpty())
			System.out.println("Found "+discoveryData.notParsedStoreData.size()+" not parseable DiscoveryStoreData.");
		if (!discoveryData.notParsedAvailableData.isEmpty())
			System.out.println("Found "+discoveryData.notParsedAvailableData.size()+" not parseable DiscoveryAvailableData.");
	}

	public static final class Inventories {

		private static Inventory parse(SaveGameData base, JSON_Object inventoryData, String inventoryLabel) {
			if (inventoryData==null) return null;
			
			Inventory inventory = new Inventory();
			inventory.substanceMaxStorageMultiplier = base.getIntegerValue(inventoryData, "SubstanceMaxStorageMultiplier");
			inventory.productMaxStorageMultiplier   = base.getIntegerValue(inventoryData, "ProductMaxStorageMultiplier");
			inventory.width   = base.getIntegerValue(inventoryData, "Width");
			inventory.height  = base.getIntegerValue(inventoryData, "Height");
			inventory.isCool  = base.getBoolValue   (inventoryData, "IsCool");
			inventory.version = base.getIntegerValue(inventoryData, "Version");
			
			inventory.inventoryClass = base.getStringValue(inventoryData, "Class","InventoryClass");
			if (inventory.inventoryClass==null) {
				JSON_Object classObj = base.getObjectValue(inventoryData, "Class");
				if (classObj==null) inventory.inventoryClass = "<no \"Class\" value>";
				else {
					Value value = classObj.getValue("InventoryClass");
					if (value==null) inventory.inventoryClass = "<no \"Class.InventoryClass\" value>";
					else             inventory.inventoryClass = "<wrong \"Class.InventoryClass\" value>";
				}
			}
			
			if (inventory.width!=null && inventory.height!=null && inventory.width>0 && inventory.height>0) {
				inventory.slots = parseSlots(base, (int)(long)inventory.width, (int)(long)inventory.height, base.getArrayValue(inventoryData,"Slots"),base.getArrayValue(inventoryData,"ValidSlotIndices"), inventoryLabel);
			}
			inventory.baseStatValues = parseBaseStatValues(base, base.getArrayValue(inventoryData,"BaseStatValues"), inventoryLabel);
			inventory.specialSlots   = parseSpecialSlots  (base, base.getArrayValue(inventoryData,"SpecialSlots"  ), inventoryLabel);
			
			return inventory;
		}
		
		private static Slot[][] parseSlots(SaveGameData base, int width, int height, JSON_Array arrSlots, JSON_Array arrValidSlotIndices, String inventoryLabel) {
			Slot[][] slots = new Slot[width][height];
			for (Slot[] row:slots)
				Arrays.fill(row, null);
			
			if (arrSlots==null) {
				System.err.println(inventoryLabel+": Inventory has no slots.");
				return slots;
			}
			
			if (arrValidSlotIndices==null) {
				System.err.println(inventoryLabel+": Inventory has no valid slot indices.");
				return slots;
			}
			
			int redundantSlots = 0;
			JSON_Array wrongValidSlotIndices = new JSON_Array();
			for (Value value:arrValidSlotIndices) {
				JSON_Object indexObj = Value.getObject(value);
				if (indexObj==null) continue;
				Long indexX = base.getIntegerValue(indexObj, "X");
				Long indexY = base.getIntegerValue(indexObj, "Y");
				if (indexX==null || indexX<0 || indexX>=width ) { wrongValidSlotIndices.add(value); continue; }
				if (indexY==null || indexY<0 || indexY>=height) { wrongValidSlotIndices.add(value); continue; }
				if (slots[(int)(long)indexX][(int)(long)indexY]==null) {
					slots[(int)(long)indexX][(int)(long)indexY] = new Slot(true,indexX,indexY);
				} else
					++redundantSlots;
			}
			if (!wrongValidSlotIndices.isEmpty())
				System.err.println(inventoryLabel+": Found "+wrongValidSlotIndices.size()+" wrong \"valid\" slot indices.");
			if (redundantSlots>0)
				System.err.println(inventoryLabel+": Found "+redundantSlots+" redundant \"valid\" slot indices.");
			
			redundantSlots = 0;
			int notValidSlots = 0;
			JSON_Array wrongSlots = new JSON_Array();
			for (Value value:arrSlots) {
				JSON_Object slotObj = Value.getObject(value);
				if (slotObj==null) { wrongSlots.add(value); continue; }
				Slot slot = new Slot(false);
				slot.type         = base.getStringValue (slotObj, "Type","InventoryType");
				slot.id           = base.getStringValue (slotObj, "Id");
				slot.amount       = base.getIntegerValue(slotObj, "Amount");
				slot.maxAmount    = base.getIntegerValue(slotObj, "MaxAmount");
				slot.damageFactor = base.getFloatValue  (slotObj, "DamageFactor");
				slot.indexX       = base.getIntegerValue(slotObj, "Index","X");
				slot.indexY       = base.getIntegerValue(slotObj, "Index","Y");
				if (slot.indexX==null || slot.indexX<0 || slot.indexX>=width ) { wrongSlots.add(value); continue; }
				if (slot.indexY==null || slot.indexY<0 || slot.indexY>=height) { wrongSlots.add(value); continue; }
				int x = (int)(long)slot.indexX;
				int y = (int)(long)slot.indexY;
				if (slots[x][y]==null   ) { wrongSlots.add(value); ++notValidSlots; continue; }
				if (!slots[x][y].isEmpty) { wrongSlots.add(value); ++redundantSlots; continue; }
				slots[x][y] = slot;
			}
			if (!wrongSlots.isEmpty()) System.err.println(inventoryLabel+": Found "+wrongSlots.size()+" wrong slots.");
			if (redundantSlots>0     ) System.err.println(inventoryLabel+": Found "+redundantSlots+" redundant slots.");
			if (notValidSlots>0      ) System.err.println(inventoryLabel+": Found "+notValidSlots+" not valid slots.");

			return slots;
		}

		private static BaseStatValue[] parseBaseStatValues(SaveGameData base, JSON_Array valueArray, String inventoryLabel) {
			if (valueArray==null) return null;
			
			BaseStatValue[] baseStatValues = new BaseStatValue[valueArray.size()];
			for (int i=0; i<valueArray.size(); ++i) {
				JSON_Object obj = Value.getObject(valueArray.get(i));
				if (obj==null) { baseStatValues[i]=null; continue; }
				baseStatValues[i] = new BaseStatValue(base.getStringValue(obj,"BaseStatID"),base.getFloatValue(obj,"Value"));
			}
			return baseStatValues;
		}

		private static String parseSpecialSlots(SaveGameData base, JSON_Array specialSlots, String inventoryLabel) {
			if (specialSlots==null) return null;
			int n = specialSlots.size();
			return (n>0?"no":(n+"")) +" special slot"+ (n==1?"":"s");
		}

		public Inventory player;
		public Inventory playerTech;
		public Inventory playerCargo;
		public Inventory multitool;
		public Inventory[] ships;
		public Inventory[] ships_Tech;
		public Inventory[] vehicles;
		public Inventory[] chests;
		public Inventory magicChest2;
		public Inventory magicChest;
		public Inventory freighter;
		public Inventory freighterTech;
		public Inventory ship_old;
		public Inventory grave;
		public Inventories() {
			super();
			this.ship_old = null;
			this.ships_Tech = null;
			this.ships = null;
			this.vehicles = null;
			this.magicChest2 = null;
			this.magicChest = null;
			this.chests = null;
			this.freighterTech = null;
			this.freighter = null;
			this.grave = null;
			this.multitool = null;
			this.playerCargo = null;
			this.playerTech = null;
			this.player = null;
		}
	
	}

	public static final class Inventory {

		public Long width;
		public Long height;
		public Long version;
		public String inventoryClass;
		public Boolean isCool;
		public Long productMaxStorageMultiplier;
		public Long substanceMaxStorageMultiplier;
		public Slot[][] slots;
		public BaseStatValue[] baseStatValues;
		public String specialSlots;
		
		public Inventory() {
			this.width = null;
			this.height = null;
			this.version = null;
			this.inventoryClass = null;
			this.isCool = null;
			this.productMaxStorageMultiplier = null;
			this.substanceMaxStorageMultiplier = null;
			this.slots = null;
			this.baseStatValues = null;
			this.specialSlots = null;
		}

		public static final class Slot {

			public Long indexX;
			public Long indexY;
			public String id;
			public String type;
			public Long amount;
			public Long maxAmount;
			public Double damageFactor;
			public final boolean isEmpty;
			
			
			public Slot(boolean isEmpty) {
				this.indexX = null;
				this.indexY = null;
				this.id = null;
				this.type = null;
				this.amount = null;
				this.maxAmount = null;
				this.damageFactor = null;
				this.isEmpty = isEmpty;
			}


			public Slot(boolean isEmpty, Long indexX, Long indexY) {
				this(isEmpty);
				this.indexX = indexX;
				this.indexY = indexY;
			}
		
		}

		public static final class BaseStatValue {
			public final String baseStatID;
			public final Double value;
			private BaseStatValue(String baseStatID, Double value) {
				this.baseStatID = baseStatID;
				this.value = value;
			}
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
					JSON_Object object = Value.getObject(objValue);
					if (object==null) { notParsedAvailableData.add(objValue); continue; }
					
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
					JSON_Object object = Value.getObject(objValue);
					if (object==null) { notParsedAvailableData.add(objValue); continue; }
					
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

		public void findAdditionalPlanetsAndSolarSystems() {
			HashSet<UniverseAddress> unknownAdresses = new HashSet<>();
			HashSet<String> knownTypes = new HashSet<>();
			
			storedDiscoveredItemOnPlanets = 0;
			storedDiscoveredItemInSolarSystms = 0;
			
			for (StoreData stData:storeData) {
				int universeObject = processDDblock(stData.DD, SourceArray.StoreData, unknownAdresses, knownTypes);
				switch(universeObject) {
				case 1: ++storedDiscoveredItemOnPlanets; break;
				case 2: ++storedDiscoveredItemInSolarSystms; break;
				}
			}
			
			availDiscoveredItemOnPlanets = 0;
			availDiscoveredItemInSolarSystms = 0;
			
			for (AvailableData avData:availableData) {
				int universeObject = processDDblock(avData.DD, SourceArray.AvailableData, unknownAdresses, knownTypes);
				switch(universeObject) {
				case 1: ++availDiscoveredItemOnPlanets; break;
				case 2: ++availDiscoveredItemInSolarSystms; break;
				}
			}
			
//			System.out.println("Known Types ["+knownTypes.size()+"]");
//			for (String type:knownTypes)
//				System.out.println("   "+type);
			
			if (!unknownAdresses.isEmpty()) {
				System.out.println("Found undiscovered addresses ["+unknownAdresses.size()+"]");
				for (UniverseAddress ua:unknownAdresses)
					System.out.println("   "+ua.getCoordinates());
			}
		}
		
		private int processDDblock(DDblock DD, SourceArray sourceArray, HashSet<UniverseAddress> unknownAdresses, HashSet<String> knownTypes) {
			int universeObject = 0;
			
			if (DD==null) return universeObject;
			if (DD.DT!=null) {
				if (DD.DT.equals("Planet")) return universeObject;
				if (DD.DT.equals("SolarSystem")) return universeObject;
				knownTypes.add(DD.DT);
			}
			
			if (DD.UA==null) return universeObject;
			if (DD.UA.isPlanet()) {
				universeObject = 1;
				Planet planet = data.universe.findPlanet(DD.UA);
				if (planet==null)
					unknownAdresses.add(DD.UA);
				else
					planet.addDiscoveredItem(DD.DT,sourceArray);
			}
			if (DD.UA.isSolarSystem()) {
				universeObject = 2;
				SolarSystem solarSystem = data.universe.findSolarSystem(DD.UA);
				if (solarSystem==null)
					unknownAdresses.add(DD.UA);
				else
					solarSystem.addDiscoveredItem(DD.DT,sourceArray);
			}
			return universeObject;
		}

		public void findPlanetsAndSolarSystems() {
			Universe.UniverseObject obj;
			
			for (StoreData data:storeData)
				if ((obj = getDiscNameObj(data.DD))!=null) {
					obj.foundInDiscStore = true;
					
					if (data.OWS_USN!=null) {
						if (obj.hasDiscoverer())
							obj.setDiscoverer(obj.getDiscoverer()+" | "+data.OWS_USN);
						else
							obj.setDiscoverer(data.OWS_USN);
					}
					if (data.DM_CN!=null) {
						if (obj.hasUploadedName())
							obj.setUploadedName(obj.getUploadedName()+" | "+data.DM_CN);
						else
							obj.setUploadedName(data.DM_CN);
					}
				}
			
			for (AvailableData data:availableData)
				if ((obj = getDiscNameObj(data.DD))!=null)
					obj.isNotUploaded = true;
		}

		private Universe.UniverseObject getDiscNameObj(DDblock dd) {
			if (dd.DT==null || dd.UA==null) return null;
				
			Universe.UniverseObject discnameObj = null;
			
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
			public String OWS_LID;
			public String OWS_UID;
			public String OWS_USN;
			public Long   OWS_TS;
			public String RID;
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
		
		public static class AvailableData {
			// TSrec  long
			// DD.UA  UniverseAddress
			// DD.DT  String
			// DD.VP[0]  String
			// DD.VP[1]  String | long
			
			public Long TSrec;
			public DDblock DD;
			
			AvailableData() {
				TSrec = null;
				DD = new DDblock();
			}
		}
		
		public static class DDblock {
			public UniverseAddress UA;
			public String DT;
			public String VP0;
			public String VP1;
			
			DDblock() {
				UA = null;
				DT = null;
				VP0 = null;
				VP1 = null;
			}
		}
	}

	public final static class General {
		
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
			if(currentUniverseAddress.isPlanet()) {
				Planet planet = data.universe.getOrCreatePlanet(currentUniverseAddress);
				planet.isCurrPos = true;
			}
			if(currentUniverseAddress.isSolarSystem()) {
				SolarSystem system = data.universe.getOrCreateSolarSystem(currentUniverseAddress);
				system.isCurrPos = true;
			}
		}

		private UniverseAddress parseUniverseAddress(JSON_Object universeAddressObj) {
			if (universeAddressObj==null) return null;
			
			Long galaxyIndexLong = data.getIntegerValue(universeAddressObj,"RealityIndex");
			if (galaxyIndexLong==null) return null;
			int galaxyIndex = (int)(long)galaxyIndexLong;
			
			JSON_Object galacticAddressObj = data.getObjectValue(universeAddressObj,"GalacticAddress");
			if (galacticAddressObj==null) return null;
			
			Long voxelXLong = data.getIntegerValue(galacticAddressObj,"VoxelX");
			Long voxelYLong = data.getIntegerValue(galacticAddressObj,"VoxelY");
			Long voxelZLong = data.getIntegerValue(galacticAddressObj,"VoxelZ");
			if (voxelXLong==null) return null;
			if (voxelYLong==null) return null;
			if (voxelZLong==null) return null;
			int voxelX = (int)(long)voxelXLong;
			int voxelY = (int)(long)voxelYLong;
			int voxelZ = (int)(long)voxelZLong;
			
			Long solarSystemIndexLong = data.getIntegerValue(galacticAddressObj,"SolarSystemIndex");
			if (solarSystemIndexLong==null) return null;
			int solarSystemIndex = (int)(long)solarSystemIndexLong;
			
			Long planetIndexLong = data.getIntegerValue(galacticAddressObj,"PlanetIndex");
			if (planetIndexLong==null) return null;
			int planetIndex = (int)(long)planetIndexLong;
			
			return new UniverseAddress(galaxyIndex, voxelX, voxelY, voxelZ, solarSystemIndex, planetIndex);
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
		public Long getKnownGlyphsMaks() { return data.getIntegerValue( data.json_data, "PlayerStateData","KnownPortalRunes"); }
		
		public Boolean     getTestBool   (Object... path) { return data.getBoolValue   (data.json_data, path); }
		public Long        getTestInteger(Object... path) { return data.getIntegerValue(data.json_data, path); }
		public Double      getTestFloat  (Object... path) { return data.getFloatValue  (data.json_data, path); }
		public String      getTestString (Object... path) { return data.getStringValue (data.json_data, path); }
		public JSON_Array  getTestArray  (Object... path) { return data.getArrayValue  (data.json_data, path); }
		public JSON_Object getTestObject (Object... path) { return data.getObjectValue (data.json_data, path); }
	}

	public static final class UniverseAddress implements Comparable<UniverseAddress> {

		final int galaxyIndex;
		public  final int voxelX;
		private final int voxelY;
		public  final int voxelZ;
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

		@Override
		public int hashCode() {
			return (int)((address>>32)&0xFFFFFFFF);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof UniverseAddress) return false;
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

		public String getCoordinates() {
			return String.format("%d | %d,%d,%d | %d | %d", galaxyIndex, voxelX, voxelY, voxelZ, solarSystemIndex, planetIndex);
		}

		public String getSolarSystemCoordinates() {
			return String.format("%d | %d,%d,%d | %d", galaxyIndex, voxelX, voxelY, voxelZ, solarSystemIndex);
		}

		public String getRegionCoordinates() {
			return String.format("%d | %d,%d,%d", galaxyIndex, voxelX, voxelY, voxelZ);
		}

		public String getReducedSigBoostCode() {
			return String.format("%04X:%04X:%04X", (voxelX+2047)&0xFFFF, (voxelY+127)&0xFFFF, (voxelZ+2047)&0xFFFF);
		}

		public String getSigBoostCode() {
			return String.format("%s:%04X", getReducedSigBoostCode(), solarSystemIndex&0xFFFF);
		}

		public String getExtendedSigBoostCode() {
			return String.format("G%d|%s|P%d", galaxyIndex, getSigBoostCode(), planetIndex);
		}

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
	
	public static final class Universe {

		public final Vector<Galaxy> galaxies;
		
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
				g.regions.sort(Comparator.comparing((Region r) -> r.voxelX).thenComparing((Region r) -> r.voxelY).thenComparing((Region r) -> r.voxelZ));
				for (Region r:g.regions) {
					r.solarSystems.sort(Comparator.comparing(s -> s.solarSystemIndex));
					for (SolarSystem s:r.solarSystems) {
						s.planets.sort(Comparator.comparing(p -> p.planetIndex));
					}
				}
			}
		}


		public void writeToConsole() {
			System.out.println("Universe:");
			for (Galaxy g:galaxies) {
				System.out.println("\t"+g+":");
				for (Region r:g.regions) {
					System.out.println("\t\t"+r+":");
					for (SolarSystem s:r.solarSystems) {
						System.out.println("\t\t\t"+s+":");
						for (Planet p:s.planets)
							System.out.println("\t\t\t\tPlanet "+p);
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

		private Galaxy findGalaxy(int galacticIndex) {
			for (Galaxy g:galaxies)
				if (g.galacticIndex==galacticIndex)
					return g;
			return null;
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
			Galaxy galaxy = findGalaxy(ua.galaxyIndex);
			if (galaxy==null) galaxies.add(galaxy=new Galaxy(this,ua.galaxyIndex));
			
			Region region = galaxy.findRegion(ua.voxelX,ua.voxelY,ua.voxelZ);
			if (region==null) galaxy.addRegion(region=new Region(galaxy,ua.voxelX,ua.voxelY,ua.voxelZ));
			
			SolarSystem solarSystem = region.findSolarSystem(ua.solarSystemIndex);
			if (solarSystem==null) region.addSolarSystem(solarSystem=new SolarSystem(region,ua.solarSystemIndex));
			
			return solarSystem;
		}

		public static final class Galaxy {
			private final static String[] PREDEFINED_NAMES_EN = {"Euclid","Hilbert Dimension","Calypso","Hesperius Dimension","Hyades","Ickjamatew","Budullangr","Kikolgallr","Eltiensleen","Eissentam","Elkupalos","Aptarkaba","Ontiniangp","Odiwagiri","Ogtialabi","Muhacksonto","Hitonskyer","Rerasmutul","Isdoraijung","Doctinawyra","Loychazinq","Zukasizawa","Ekwathore","Yeberhahne","Twerbetek","Sivarates","Eajerandal","Aldukesci","Wotyarogii","Sudzerbal","Maupenzhay","Sugueziume","Brogoweldian","Ehbogdenbu","Ijsenufryos","Nipikulha","Autsurabin","Lusontrygiamh","Rewmanawa","Ethiophodhe","Urastrykle","Xobeurindj","Oniijialdu","Wucetosucc","Ebyeloofdud","Odyavanta","Milekistri","Waferganh","Agnusopwit","Teyaypilny"}; 
			@SuppressWarnings("unused")
			private final static String[] PREDEFINED_NAMES_DE = {"Euklid","Hilbert Dimension","Calypso","Hesperius Dimension","Hyades","Ickjamatew","Budullangr","Kikolgallr","Eltiensleen","Eissentam","Elkupalos","Aptarkaba","Ontiniangp","Odiwagiri","Ogtialabi","Muhacksonto","Hitonskyer","Rerasmutul","Isdoraijung","Doctinawyra","Loychazinq","Zukasizawa","Ekwathore","Yeberhahne","Twerbetek","Sivarates","Eajerandal","Aldukesci","Wotyarogii","Sudzerbal","Maupenzhay","Sugueziume","Brogoweldian","Ehbogdenbu","Ijsenufryos","Nipikulha","Autsurabin","Lusontrygiamh","Rewmanawa","Ethiophodhe","Urastrykle","Xobeurindj","Oniijialdu","Wucetosucc","Ebyeloofdud","Odyavanta","Milekistri","Waferganh","Agnusopwit","Teyaypilny"}; 
			final Universe universe;
			final int galacticIndex;
			public final Vector<Region> regions;
			
			public Galaxy(Universe universe, int galacticIndex) {
				this.universe = universe;
				this.galacticIndex = galacticIndex;
				this.regions = new Vector<>();
			}

			@Override
			public String toString() {
				if (galacticIndex<PREDEFINED_NAMES_EN.length)
					return "Galaxy \""+PREDEFINED_NAMES_EN[galacticIndex]+"\"";
				return "Galaxy "+galacticIndex;
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
		
		public static final class Region {
			
			final Galaxy galaxy;
			public final int voxelX;
			private final int voxelY;
			public final int voxelZ;
			public final Vector<SolarSystem> solarSystems;
			
			public Region(Galaxy galaxy, int x, int y, int z) {
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
		
		public static class UniverseObject {
			
			private String discoverer;
			public boolean isCurrPos;
			boolean foundInStats;
			boolean foundInDiscStore;
			public boolean isNotUploaded;
			
			public final Vector<ExtraInfo> extraInfos;
			
			private String originalName;
			private String uploadedName;
			
			public final HashMap<String,Integer> discoveredItems_Avail;
			public final HashMap<String,Integer> discoveredItems_Store;
			
			public boolean isSelected;
			
			protected UniverseObject() {
				discoverer = null;
				isCurrPos        = false;
				foundInStats     = false;
				foundInDiscStore = false;
				isNotUploaded    = false;
				
				this.extraInfos = new Vector<>();
				
				originalName = null;
				uploadedName = null;
				
				discoveredItems_Avail = new HashMap<>();
				discoveredItems_Store = new HashMap<>();
				
				isSelected = false;
			}

			protected String getCombinedExtraInfoLabels() {
				StringBuilder sb = new StringBuilder();
				boolean sbIsEmpty = true;
				for (ExtraInfo ei:extraInfos)
					if (!ei.shortLabel.isEmpty()) {
						if (!sbIsEmpty) sb.append(", ");
						sb.append(ei.shortLabel);
						sbIsEmpty = false;
					}
				String string2 = sb.toString();
				return string2;
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

			
			public boolean hasSourceID() {
				return isCurrPos || foundInStats || isNotUploaded || foundInDiscStore;
			}
			
			public String getSourceIDStr() {
				StringBuilder sb = new StringBuilder();
				if (isCurrPos       ) {                                    sb.append("CP"); }
				if (foundInStats    ) { if (sb.length()>0) sb.append('|'); sb.append("St"); }
				if (foundInDiscStore) { if (sb.length()>0) sb.append('|'); sb.append("DS"); }
				if (isNotUploaded   ) { if (sb.length()>0) sb.append('|'); sb.append("DA"); }
				return "<"+sb.toString()+">";
			}
			
			public boolean hasDiscoverer() { return discoverer!=null; }
			public String getDiscoverer() { return discoverer; }
			public void setDiscoverer(String name) { this.discoverer = name; }

			public boolean hasOriginalName() { return originalName!=null; }
			public boolean hasUploadedName() { return uploadedName!=null; }
			public void setOriginalName(String name) { this.originalName = name; }
			public void setUploadedName(String name) { this.uploadedName = name; }
			public String getOriginalName() { return this.originalName; }
			public String getUploadedName() { return this.uploadedName; }
			
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
		
		public static final class SolarSystem extends UniverseObject {
			
			public enum StarClass {
				Yellow("G"), Red("K"), Green, Blue;
				
				@SuppressWarnings("unused")
				private String[] letters;
				
				StarClass(String... letters) {
					this.letters = letters;
				}
			}
			
			public enum Race {
				Gek("Gek"), Korvax("Korvax"), Vykeen("Vy'keen");

				public final String fullName;
				private Race(String fullName) { this.fullName = fullName; }
			}
			
			final Region region;
			final int solarSystemIndex;
			public final Vector<Planet> planets;
			public Race race;
			public StarClass starClass;
			public Double distanceToCenter;
			
			public SolarSystem(Region region, int solarSystemIndex) {
				this.region = region;
				this.solarSystemIndex = solarSystemIndex;
				this.planets = new Vector<>();
				this.race = null;
				this.starClass = null;
				this.distanceToCenter = null;
			}

			@Override
			public String toString() {
				String strName;
				if (hasOriginalName()) strName = String.format("Sys%03X %s", solarSystemIndex, getOriginalName());
				else                   strName = String.format("SolarSystem %03X (%d)", solarSystemIndex, solarSystemIndex);
				
				String strDataName = (!hasUploadedName()?"":(" | "+getUploadedName()));
				
				String strRace = (race==null)?"":(" ["+race.fullName+"]");
				
				HashSet<String> foundLabels = new HashSet<>();
				for (Planet p:planets)
					for (ExtraInfo ei:p.extraInfos)
						if (ei.showInParent && !ei.shortLabel.isEmpty())
							foundLabels.add(ei.shortLabel);
				String strExtraInfo=getCombinedExtraInfoLabels();
				for (String str:foundLabels) {
					if (!strExtraInfo.isEmpty()) strExtraInfo+=", ";
					strExtraInfo+=str;
				}
				if (!strExtraInfo.isEmpty()) strExtraInfo=" ("+strExtraInfo+")";
				
				return strName+strExtraInfo+strDataName+strRace;
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
		
		public static final class Planet extends UniverseObject {
			
			final SolarSystem solarSystem;
			final int planetIndex;
			private Stats.PlanetStats stats;
			
			public Planet(SolarSystem solarSystem, int planetIndex) {
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
					strExtraInfo = getCombinedExtraInfoLabels();
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

	public static final class KnownWords {

		private final SaveGameData data;
		public Vector<KnownWord> wordList;
		JSON_Array notParsedKnownWords;
		public int[] wordCounts;

		public KnownWords(SaveGameData data) {
			this.data = data;
			this.wordList = new Vector<>();
			this.wordCounts = null;
			notParsedKnownWords = new JSON_Array();
		}
	
		public KnownWords parse(JSON_Array knownWordsArray) {
			for (Value knownWordValue : knownWordsArray) {
				JSON_Object knownWordObj = Value.getObject(knownWordValue);
				if (knownWordObj==null) { notParsedKnownWords.add(knownWordValue); continue; }
				
				KnownWord knownWord = new KnownWord();
				
				knownWord.word = data.getStringValue(knownWordObj,"Word");
				if (knownWord.word==null) { notParsedKnownWords.add(knownWordValue); continue; }
				
				JSON_Array races = data.getArrayValue(knownWordObj,"Races");
				if (races==null) { notParsedKnownWords.add(knownWordValue); continue; }
				knownWord.races = new boolean[races.size()];
				
				boolean errorOccured = false;
				for (int i=0; i<knownWord.races.length; ++i) {
					Boolean race = Value.getBool(races.get(i));
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
	
	public final static class Stats {
		
		public Vector<StatValue> globalStats;
		public Vector<PlanetStats> planetStats;
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
				JSON_Object group = Value.getObject(groupValue);
				if (group==null) { notParsedStats.add(groupValue); continue; }
				
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
					planet.foundInStats = true;
					
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
				JSON_Object statObject = Value.getObject(value);
				if (statObject==null) continue;
				
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

		public static class StatValue implements Comparable<StatValue> {
			public enum KnownID {
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
				TGUILD_STAND("Traders"  +" guild standing"), TGDONE_MISSIONS("Traders"  +" guild missions done"), MONEY("Units max. earned"), PLANTS_PLANTED("Plants planted"),
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
			
			public String ID;
			public KnownID knownID;
			public long IntValue;
			public double FloatValue;
			public double Denominator;
			
			
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

	public enum Error { NoError, UnexpectedType, PathIsNotSolvable, ValueIsNull }

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
