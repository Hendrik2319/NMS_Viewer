package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Stack;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.BuildingObject;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.NVExtra;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Point3D;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Position;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.VExtra;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Galaxy;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Region;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.SolarSystem;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.UniverseAddress;
import net.schwarzbaer.java.lib.gui.ProgressDialog;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.ArrayValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.BoolValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.FloatValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.IntegerValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.JSON_Array;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.JSON_Object;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.NamedValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.ObjectValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.StringValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value;

public class FileExport {
	static final String RES_IMAGES_TAB_HEADER_PNG = "/images/TabHeader.png";
	static final String RES_IMAGES_TOOLBAR_PNG    = "/images/Toolbar.png";
	static final String RES_KNOWN_STEAM_ID_TemplateHTML = "/html/KnownSteamIDs.template.html";
	
	static final String EXTRA_IMAGES_PATH = "extra/resource_icons";
	
	static final String FILE_CFG                  = "NMS_Viewer.cfg";
	static final String FILE_COLORS               = "NMS_Viewer.Colors.txt";
	static final String FILE_KNOWN_STAT_ID        = "NMS_Viewer.KnownStatID.txt";
	static final String FILE_PRODUCT_ID           = "NMS_Viewer.ProdIDs.txt";
	static final String FILE_TECH_ID              = "NMS_Viewer.TechIDs.txt";
	static final String FILE_SUBSTANCE_ID         = "NMS_Viewer.SubstanceIDs.txt";
	static final String FILE_UNIVERSE_OBJECT_DATA = "NMS_Viewer.UniverseObjects.txt";
	static final String FILE_KNOWN_EDITABLE_MODS  = "NMS_Viewer.KnownEditableMods.txt";;
	static final String FILE_KNOWN_STEAM_ID       = "NMS_Viewer.KnownSteamIDs.txt";
	static final String FILE_KNOWN_STEAM_ID_HTML  = "NMS_Viewer.KnownSteamIDs.html";
	static final String FILE_CFG_PRODUCTION_OPTIMISER = "NMS_Viewer.ProductionOptimiser.cfg";
	static final String FILE_CFG_RESOURCE_HOTSPOTS    = "NMS_Viewer.ResourceHotSpots.cfg";
	static final String FILE_DATA_RESOURCE_HOTSPOTS   = "NMS_Viewer.ResourceHotSpots.data";
	static final String FILE_CFG_UPGRADE_MODULE_INSTALL_HELPER = "NMS_Viewer.UpgradeModuleInstallHelper.cfg";
	
	private static final Color COLOR_WINDOW = new Color(0.3f, 0.5f, 1.0f);
	
	static {
		VRMLoutput.vrmlFileChooser = new JFileChooser("./");
		VRMLoutput.vrmlFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		VRMLoutput.vrmlFileChooser.setMultiSelectionEnabled(false);
		VRMLoutput.vrmlFileChooser.setFileFilter(VRMLoutput.vrmlFileFilter = new FileNameExtensionFilter("VRML-File (*.wrl)","wrl"));

		VRMLoutput.createModelMap();
	}
	
	static void writeKnownSteamIDsToHTML() {
		Gui.log_ln("Write KnownSteamIDs to HTML file \"%s\" ...", FILE_KNOWN_STEAM_ID_HTML);
		long start = System.currentTimeMillis();
		
		try (
				BufferedReader in = new BufferedReader(new InputStreamReader("".getClass().getResourceAsStream(RES_KNOWN_STEAM_ID_TemplateHTML), StandardCharsets.UTF_8));
				PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(FILE_KNOWN_STEAM_ID_HTML), StandardCharsets.UTF_8))
		) {
			String line;
			while ( (line=in.readLine())!=null ) {
				if (line.equals("// write here: new SteamID( id, name )"))
					SaveViewer.steamIDs.forEachSorted((id,name)->out.printf("\t\tnew SteamID( \"%s\", \"%s\" ),%n", id, name.replace("\"", "\\\"")));
				else
					out.println(line);
			}
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
		
		Gui.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}

	static void writeToJSON(JSON_Object<NVExtra,VExtra> json_Object, File copyfile) {
		PrintWriter out;
		try {
			out = new PrintWriter(copyfile,StandardCharsets.UTF_8.name());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return;
		}
		
		HtmlJsonOutput.writeToJSON_obj(out,json_Object);
		
		out.close();
		
	}

	static void writeToHTML(String title, JSON_Object<NVExtra,VExtra> json_Object, File htmlfile) {
		PrintWriter out;
		try {
			out = new PrintWriter(htmlfile,StandardCharsets.UTF_8.name());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return;
		}
		
		out.println("<!DOCTYPE html>");
		out.println("<html>");
		out.println("	<head>");
		out.println("		<meta charset=\"UTF-8\">");
		out.println("		<title>"+title+"</title>");
		out.println("		<style type=\"text/css\" media=\"screen\">");
		out.println("			body { font-family:sans-serif; }");
		out.println("			.valuelist { padding-left:40px; }");
		out.println("			.hidden { display:none; }");
		out.println("			.button { font-weight:bold; color:#AAA; background:#EEE; }");
		out.println("			.number { color:red; }");
		out.println("			.string { color:#007F7F; }");
		out.println("			.name   { color:#0000FF; }");
		out.println("		</style>");
		out.println("		<script type=\"text/javascript\" charset=\"utf-8\">");
		out.println("			function toggle_collapse( btn_obj, div_ID ) {");
		out.println("				var div_obj = document.getElementById(div_ID);");
		out.println("				if (div_obj) {");
		out.println("					var old_className = div_obj.className;");
		out.println("					if (old_className == 'valuelist') {");
		out.println("						if (btn_obj) btn_obj.innerHTML = '[+]';");
		out.println("						div_obj.className = 'valuelist hidden';");
		out.println("					} else {");
		out.println("						if (btn_obj) btn_obj.innerHTML = '[-]';");
		out.println("						div_obj.className = 'valuelist';");
		out.println("					}");
		out.println("				}");
		out.println("			}");
		out.println("		</script>");
		out.println("	</head>");
		out.println("<body>");
		
		HtmlJsonOutput.writeToHTML_obj(out,json_Object,"obj");
		
		out.println("</body>");
		out.println("</html>");
		
		out.close();
	}
	
	private static class HtmlJsonOutput {

		static void writeToJSON_obj(PrintWriter out, JSON_Object<NVExtra,VExtra> json_Object) {
			if (json_Object==null) {
				out.print("null");
				return;
			}
			
			out.print("{");
			for (int i=0; i<json_Object.size(); ++i) {
				NamedValue<NVExtra,VExtra> namedvalue = json_Object.get(i);
				out.printf("\"%s\":",namedvalue.name);
				writeToJSON_value(out, namedvalue.value);
				if (i+1<json_Object.size()) out.print(",");
			}
			out.print("}");
		}

		static void writeToHTML_obj(PrintWriter out, JSON_Object<NVExtra,VExtra> json_Object, String ID) {
			if (json_Object==null) {
				out.print("null");
				return;
			}
			
			out.println("{ <span class=\"button\" onclick=\"toggle_collapse(this,'"+ID+"');\">[+]</span>");
			out.println("<div id="+ID+" class=\"valuelist hidden\">");
			
			for (int i=0; i<json_Object.size(); ++i) {
				NamedValue<NVExtra,VExtra> namedvalue = json_Object.get(i);
				out.printf("<span class=\"name\">\"%s\"</span> : ",namedvalue.name);
				writeToHTML_value(out, namedvalue.value, ID, i);
				out.println(",<br/>");
			}
			
			out.println("</div>");
			out.println("}");
		}

		static void writeToJSON_arr(PrintWriter out, JSON_Array<NVExtra,VExtra> json_Array) {
			if (json_Array==null) {
				out.print("null");
				return;
			}
			
			out.print("[");
			
			for (int i=0; i<json_Array.size(); ++i) {
				//if (json_Array.hasMixedContent()) out.print(" ");
				writeToJSON_value(out, json_Array.get(i));
				if (i+1<json_Array.size()) out.print(",");
			}
			//if (json_Array.hasMixedContent()) out.print(" ");
			
			out.print("]");
		}

		static void writeToHTML_arr(PrintWriter out, JSON_Array<NVExtra,VExtra> json_Array, String ID) {
			if (json_Array==null) {
				out.print("null");
				return;
			}
			
			out.println("[ <span class=\"button\" onclick=\"toggle_collapse(this,'"+ID+"');\">[+]</span>");
			out.println("<div id="+ID+" class=\"valuelist hidden\">");
			
			for (int i=0; i<json_Array.size(); ++i) {
				writeToHTML_value(out, json_Array.get(i), ID, i);
				out.println(",<br/>");
			}
			
			out.println("</div>");
			out.println("]");
		}

		static void writeToJSON_value(PrintWriter out, Value<NVExtra,VExtra> value) {
			if ( value instanceof StringValue  ) out.printf(Locale.ENGLISH,"\"%s\"", value.castToStringValue ().value);
			if ( value instanceof FloatValue   ) out.printf(Locale.ENGLISH,"%s"    , value.castToFloatValue  ().value);
			if ( value instanceof IntegerValue ) out.printf(Locale.ENGLISH,"%d"    , value.castToIntegerValue().value);
			if ( value instanceof BoolValue    ) out.printf(Locale.ENGLISH,"%s"    , value.castToBoolValue   ().value);
			if ( value instanceof ObjectValue  ) writeToJSON_obj(out, value.castToObjectValue().value);
			if ( value instanceof ArrayValue   ) writeToJSON_arr(out, value.castToArrayValue ().value);
		}

		static void writeToHTML_value(PrintWriter out, Value<NVExtra,VExtra> value, String ID, int i) {
			if ( value instanceof StringValue  ) out.printf(Locale.ENGLISH,"<span class=\"string\">"+"\"%s\""+"</span>", value.castToStringValue ().value);
			if ( value instanceof FloatValue   ) out.printf(Locale.ENGLISH,"<span class=\"number\">"+"%s"    +"</span>", value.castToFloatValue  ().value);
			if ( value instanceof IntegerValue ) out.printf(Locale.ENGLISH,"<span class=\"number\">"+"%d"    +"</span>", value.castToIntegerValue().value);
			if ( value instanceof BoolValue    ) out.printf(Locale.ENGLISH,"<span class=\"number\">"+"%s"    +"</span>", value.castToBoolValue   ().value);
			if ( value instanceof ObjectValue  ) writeToHTML_obj(out, value.castToObjectValue().value, ID+"_"+i);
			if ( value instanceof ArrayValue   ) writeToHTML_arr(out, value.castToArrayValue ().value, ID+"_"+i);
		}
		
	}
	
	private static long startTask(ProgressDialog pd, String indent, String taskTitle) {
		return startTask(pd, indent, taskTitle, -1);
	}

	private static long startTask(ProgressDialog pd, String indent, String taskTitle, int max) {
		if (pd!=null) Gui.runInEventThreadAndWait(()->{
			pd.setTaskTitle(taskTitle);
			if (max>=0) pd.setValue(0, max);
			else pd.setIndeterminate(true);
		});
		long startTime = System.currentTimeMillis();
		Gui.log("%s%s ... ",indent,taskTitle);
		return startTime;
	}

	private static void endTask(long startTime) {
		endTask(startTime,"");
	}

	private static void endTask(long startTime, String additionalInfo) {
		Gui.log_ln("done (in %1.2fs)%s", (System.currentTimeMillis()-startTime)/1000.0, additionalInfo);
	}

	public static void openFileInVrmlViewer(File file) {
		if (!SaveViewer.config.isVrmlViewerConfigured() || file==null) return;
		SaveViewer.executeShellCommand(new String[] {SaveViewer.config.vrmlViewer,file.getAbsolutePath()});
	}

	private static boolean isObjectAboveMAINROOM(BuildingObject obj, BuildingObject[] bObjs, Vector<String> objectIDs) {
		if (bObjs==null) return false;
		if (obj==null) return false;
		if (obj.position==null) return false;
		if (obj.position.pos==null) return false;
		if (obj.position.up ==null) return false;
		if (obj.position.at ==null) return false;
		
		Point3D targetPos = obj.position.pos.add(obj.position.up.normalize().mul(4));
		for (BuildingObject other:bObjs) {
			if (other==null) continue;
			if (other.objectID==null) continue;
			if (other.position==null) continue;
			if (other.position.pos==null) continue;
			if (other.position.up ==null) continue;
			if (other.position.at ==null) continue;
			
			if (!objectIDs.contains(other.objectID)) continue;
			if (targetPos.distTo(other.position.pos)>0.1) continue;
			
			if (other.position.up.normalize().crossProd(obj.position.up.normalize()).length()<0.01)
				return true;
		}
		return false;
	}

	public static void writePosToVRML_models(String suggestedFileName, BuildingObject[] objects, SaveGameData.PersistentPlayerBase playerbase, Window parent, String label, boolean dontAsk, Consumer<File> openFileInViewer) {
		Consumer<ProgressDialog> task = (ProgressDialog pd)->{
			BuildingObject[] bObjs = objects;
			if (bObjs==null && playerbase!=null) bObjs = playerbase.objects;
			if (bObjs==null) return;
			
			long startTime, startTimeTotal = System.currentTimeMillis();
			Gui.log_ln("Write positions of "+bObjs.length+" BuildingObjects to VRML file ...");
			
			File file;
			if (dontAsk)
				file = VRMLoutput.createBaseVrmlFile(suggestedFileName);
			else {
				if (pd!=null) Gui.runInEventThreadAndWait(()->{
					pd.setIndeterminate(true);
					pd.setTaskTitle("Ask for filename");
				});
				file = VRMLoutput.selectVrmlFile2Write(parent,suggestedFileName);
			}
			if (file==null) return;
			
			if (pd!=null) Gui.runInEventThreadAndWait(()->{
				pd.setIndeterminate(true);
				pd.setTaskTitle("Determine max. dimensions");
			});
			Point3D min = null;
			Point3D max = null;
			
			for (BuildingObject obj:bObjs) {
				if (obj.position==null) continue;
				if (obj.position.pos==null) continue;
				Point3D pos = new Point3D(obj.position.pos);
				if (min==null) min = new Point3D(pos); else min.min(pos);
				if (max==null) max = new Point3D(pos); else max.max(pos);
			}

			Vector<String> antiRoofObjects = new Vector<>();
			Collections.addAll(antiRoofObjects, "^MAINROOM", "^MAINROOM_WATER", "^MAINROOMCUBE", "^MAINROOMCUBE_W", "^BIOROOM", "^MAINROOMFRAME");
			Collections.addAll(antiRoofObjects, "^CORRIDORV_WATER", "^CUBEROOM", "^CUBEGLASS", "^CUBEROOMCURVED", "^CURVEDCUBEROOF", "^CUBESOLID", "^CUBEFRAME");

			
			HashSet<String> usedModels = new HashSet<>();
			boolean[] isMAINROOMwithRoof = new boolean[bObjs.length];
			for (int i=0; i<bObjs.length; i++) {
				BuildingObject obj = bObjs[i];
				
				String model = VRMLoutput.mapObjectID2Model.get(obj.objectID);
				if (model!=null)
					usedModels.add(model);
				
				if (model!=null && (model.equals("MAINROOM") || model.equals("MAINROOMCUBE")))
					isMAINROOMwithRoof[i] = !isObjectAboveMAINROOM(obj,bObjs,antiRoofObjects);
				else
					isMAINROOMwithRoof[i] = false;
			}

			try (PrintWriter vrml = new PrintWriter(new BufferedWriter( new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8)))) {
				
				startTime = startTask(pd, "   ", "Write templates");
				VRMLoutput.writeTemplateToFile(vrml,usedModels);
				endTask(startTime);
				
				CubeCombine cubeCombine = /*new CubeCombine_Dummy();*/ new CubeCombine();
				FreighterRoomCombine freightCombine = new FreighterRoomCombine();
				
				if (playerbase!=null) {
					Point3D up = Point3D.normalizeOrNull(playerbase.position);
					Point3D at = Point3D.normalizeOrNull(playerbase.forward);
					if (at!=null && up!=null) {
						cubeCombine.setBaseOrientation(at,up);
						//freightCombine.setBaseOrientation(at,up);
					}
				}
				
				HashMap<String, Integer> objectIDNumbers = null;
				if (SaveViewer.config.addNumberToObjectIDInAxisCross)
					objectIDNumbers = new HashMap<>();
				
				startTime = startTask(pd, "   ", "Write standard objects to file", bObjs.length);
				for (int i=0; i<bObjs.length; i++) {
					BuildingObject obj = bObjs[i];
					if (!cubeCombine.add(obj) && !freightCombine.add(obj)) {
						VRMLoutput.writeModel(vrml, obj, isMAINROOMwithRoof[i], objectIDNumbers);
					}
					int value = i+1;
					if (pd!=null) Gui.runInEventThreadAndWait(()->{ pd.setValue(value); });
				}
				endTask(startTime);
				
				if (!cubeCombine.isEmpty()) {
					startTime = startTask(pd, "   ", "Write result of CubeCombine to file");
					vrml.println("# CubeCombine: result mesh");
					cubeCombine.writeModel(vrml);
					endTask(startTime);
				}
				
				if (!freightCombine.isEmpty()) {
					startTime = startTask(pd, "   ", "Write result of FreighterRoomCombine to file");
					Gui.log_ln("");
					vrml.println("# FreighterRoomCombine: result mesh");
					freightCombine.writeModel(vrml,pd);
					Gui.log("   ");
					endTask(startTime);
				}
				
				
				BuildingObject[] remainingObjects;
				remainingObjects = cubeCombine.getRemainingObjects();
				if (remainingObjects.length>0) {
					startTime = startTask(pd, "   ", "Write unprocessed objects of CubeCombine to file");
					vrml.println("# CubeCombine: remaining objects");
					for (BuildingObject obj:remainingObjects)
						VRMLoutput.writeModel(vrml, obj);
					endTask(startTime);
				}
				
				remainingObjects = freightCombine.getRemainingObjects();
				if (remainingObjects.length>0) {
					startTime = startTask(pd, "   ", "Write unprocessed objects of FreighterRoomCombine to file");
					vrml.println("# FreighterRoomCombine: remaining objects");
					for (BuildingObject obj:remainingObjects)
						VRMLoutput.writeModel(vrml, obj);
					endTask(startTime);
				}
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			Gui.log_ln("done (in %1.2fs)",(System.currentTimeMillis()-startTimeTotal)/1000.0);
			
			if (openFileInViewer!=null)
				openFileInViewer.accept(file);
		};
		
		runProgressDialogTask(parent, label, task);
	}

	public static void writePosToVRML_simple(String suggestedFileName, BuildingObject[] objects, Double planetRadius, Window parent, String label, Consumer<File> openFileInViewer) {
		Consumer<ProgressDialog> task = (ProgressDialog pd)->{
			
			long startTime, startTimeTotal = System.currentTimeMillis();
			Gui.log_ln("Write positions of "+objects.length+" BuildingObjects to VRML file ...");
			Double pRad = planetRadius;
			if (pRad!=null && pRad<=0) pRad=null;
			
			if (pd!=null) Gui.runInEventThreadAndWait(()->{
				pd.setIndeterminate(true);
				pd.setTaskTitle("Ask for filename");
			});
			File file = VRMLoutput.selectVrmlFile2Write(parent,suggestedFileName);
			if (file==null) return;
			
			if (pd!=null) Gui.runInEventThreadAndWait(()->{
				pd.setIndeterminate(true);
				pd.setTaskTitle("Determine max. dimensions");
			});
			Point3D min = null;
			Point3D max = null;
			
			if (pRad!=null) {
				min = new Point3D(-pRad,-pRad,-pRad);
				max = new Point3D( pRad, pRad, pRad);
			}
			
			for (BuildingObject obj:objects) {
				if (obj.position==null) continue;
				if (obj.position.pos==null) continue;
				if (min==null) min = new Point3D(obj.position.pos); else min.min(obj.position.pos);
				if (max==null) max = new Point3D(obj.position.pos); else max.max(obj.position.pos);
			}
			double size = 0;;
			if (max!=null && min!=null)
				size = Math.max(Math.max(max.x-min.x,max.y-min.y),max.z-min.z)/200;
			
			try (PrintWriter vrml = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8))) {
				
				startTime = startTask(pd, "   ", "Write templates");
				vrml.println("#VRML V2.0 utf8");
				vrml.println("");
				vrml.println("Background { skyColor 0.6 0.7 0.8 }");
				vrml.println("");
				vrml.println("PROTO Axis [");
				vrml.println("	field SFVec3f scale 1 1 1");
				vrml.println("	field SFVec3f pos 0 0 0");
				vrml.println("	field SFVec3f up  1 0 0");
				vrml.println("	field SFVec3f at  0 1 0");
				vrml.println("	field MFString string []");
				vrml.println("] {");
				vrml.println("	Transform { translation IS pos");
				vrml.println("		children [");
				vrml.println("			Transform { scale IS scale children [");
				vrml.println("				Shape { appearance Appearance { material Material { diffuseColor 1 1 1 } } geometry DEF sphere Sphere { radius 0.5 } }");
				vrml.println("				Billboard { axisOfRotation 0 0 0");
				vrml.println("					children [");
				vrml.println("						Transform { translation 0 1 0 scale 1 1 1 children [");
				vrml.println("							Shape {");
				vrml.println("								appearance Appearance { material Material { diffuseColor 1 1 0 } }");
				vrml.println("								geometry Text { string IS string fontStyle FontStyle { justify [ \"MIDDLE\" \"END\" ] family \"SANSSERIF\"} }");
				vrml.println("							}");
				vrml.println("						]}");
				vrml.println("					]");
				vrml.println("				}");
				vrml.println("			] }");
				vrml.println("			Transform { translation IS up scale IS scale children [");
				vrml.println("				Shape { appearance Appearance { material Material { diffuseColor 1 0 0 } } geometry USE sphere }");
				vrml.println("			] }");
				vrml.println("			Transform { translation IS at scale IS scale children [");
				vrml.println("				Shape { appearance Appearance { material Material { diffuseColor 0 1 0 } } geometry USE sphere }");
				vrml.println("			] }");
				vrml.println("		]");
				vrml.println("	}");
				vrml.println("}");
				vrml.println("");
				endTask(startTime);
				
				if (pRad!=null) {
					startTime = startTask(pd, "   ", "Write planet");
					VRMLoutput.writeSphere(vrml,pRad, new Point3D(0,0,0), java.awt.Color.GRAY);
					endTask(startTime);
				}
				
				startTime = startTask(pd, "   ", "Write objects", objects.length);
				for (int i=0; i<objects.length; i++) {
					int value = i;
					if (pd!=null) Gui.runInEventThreadAndWait(()->{ pd.setValue(value); });
					BuildingObject obj = objects[i];
					if (obj.position==null) continue;
					if (obj.position.pos==null) continue;
					Position p = obj.position;
					vrml.printf(               "Axis {");
					if (size>0) vrml.printf(Locale.ENGLISH," scale %1.2f %1.2f %1.2f", size, size, size);
					vrml.printf(Locale.ENGLISH," pos %1.2f %1.2f %1.2f", p.pos.x, p.pos.y, p.pos.z);
					if (p.up!=null && !p.up.isZero()) vrml.printf(" up %s", p.up.normalize().mul(size).toString("%1.3f",false));
					if (p.at!=null && !p.at.isZero()) vrml.printf(" at %s", p.at.normalize().mul(size).toString("%1.3f",false));
					vrml.printf(               " string \"%s\"", obj.getNameOrObjectID().replace('\"','_'));
					vrml.printf(Locale.ENGLISH," } # Pos r:%f\r\n", p.pos.length());
				}
				if (pd!=null) Gui.runInEventThreadAndWait(()->{ pd.setValue(objects.length); });
				endTask(startTime);
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			Gui.log_ln("done (in %1.2fs)",(System.currentTimeMillis()-startTimeTotal)/1000.0);
			
			if (openFileInViewer!=null)
				openFileInViewer.accept(file);
		};
		runProgressDialogTask(parent, label, task);
	}

	public static void writeGalaxyToVRML(File file, Galaxy galaxy, Function<Region,Color> getcolor, Color colorBlackholeConnection, Window parent) {
		if (file  ==null) return;
		if (galaxy==null) return;
		
		Consumer<ProgressDialog> task = (ProgressDialog pd)->{
			long startTime, startTime_total;
			
			startTime_total = startTask(pd, "", "Write Galaxy Map to VRML");
			try (PrintWriter vrml = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
				
				startTime = startTask(pd, "   ", "Write Header");
				vrml.println("#VRML V2.0 utf8");
				vrml.println("");
				vrml.println("Background { skyColor 0 0 0 }");
				vrml.println("");
				endTask(startTime);
				
				startTime = startTask(pd, "   ", "Create Outline");
				LineGeometry.OptimizeNode outlineAndZeroAxes = new LineGeometry.OptimizeNode(
//						new LineGeometry.PolyLine().add(-2047.5, 0.5, 0.5).add(+2047.5, 0.5, 0.5),
//						new LineGeometry.PolyLine().add(-2047.5,-0.5, 0.5).add(+2047.5,-0.5, 0.5),
//						new LineGeometry.PolyLine().add(-2047.5,-0.5,-0.5).add(+2047.5,-0.5,-0.5),
//						new LineGeometry.PolyLine().add(-2047.5, 0.5,-0.5).add(+2047.5, 0.5,-0.5),
//						
//						new LineGeometry.PolyLine().add( 0.5,-127.5, 0.5).add( 0.5,+127.5, 0.5),
//						new LineGeometry.PolyLine().add(-0.5,-127.5, 0.5).add(-0.5,+127.5, 0.5),
//						new LineGeometry.PolyLine().add(-0.5,-127.5,-0.5).add(-0.5,+127.5,-0.5),
//						new LineGeometry.PolyLine().add( 0.5,-127.5,-0.5).add( 0.5,+127.5,-0.5),
//						
//						new LineGeometry.PolyLine().add( 0.5, 0.5,-2047.5).add( 0.5, 0.5,+2047.5),
//						new LineGeometry.PolyLine().add(-0.5, 0.5,-2047.5).add(-0.5, 0.5,+2047.5),
//						new LineGeometry.PolyLine().add(-0.5,-0.5,-2047.5).add(-0.5,-0.5,+2047.5),
//						new LineGeometry.PolyLine().add( 0.5,-0.5,-2047.5).add( 0.5,-0.5,+2047.5),
						
						new LineGeometry.PolyLine()
							.add(-2047.5,-127.5,-2047.5)
							.add( 2047.5,-127.5,-2047.5)
							.add( 2047.5,-127.5, 2047.5)
							.add(-2047.5,-127.5, 2047.5)
							.close(),
						
						new LineGeometry.PolyLine()
							.add(-2047.5, 127.5,-2047.5)
							.add( 2047.5, 127.5,-2047.5)
							.add( 2047.5, 127.5, 2047.5)
							.add(-2047.5, 127.5, 2047.5)
							.close(),
						
						new LineGeometry.PolyLine().add(-2047.5,-127.5,-2047.5).add(-2047.5, 127.5,-2047.5),
						new LineGeometry.PolyLine().add( 2047.5,-127.5,-2047.5).add( 2047.5, 127.5,-2047.5),
						new LineGeometry.PolyLine().add( 2047.5,-127.5, 2047.5).add( 2047.5, 127.5, 2047.5),
						new LineGeometry.PolyLine().add(-2047.5,-127.5, 2047.5).add(-2047.5, 127.5, 2047.5)
				);
				endTask(startTime);
				
				LineGeometry.DirectWriteGroupingNode zeroAxes = new LineGeometry.DirectWriteGroupingNode();
				LineGeometry.DirectWriteGroupingNode grid1 = new LineGeometry.DirectWriteGroupingNode();
				LineGeometry.DirectWriteGroupingNode grid2 = new LineGeometry.DirectWriteGroupingNode();
				Predicate<Integer> isGrid2 = i->(i & 0xFF)==0;
				int inc = 64;
				
				startTime = startTask(pd, "   ", "Create Horizontal Grid Lines", 127);
				LineGeometry.PolyLine lineXPYP, lineXNYP, lineZPYP, lineZNYP, lineXPYN, lineXNYN, lineZPYN, lineZNYN;
				for (int y=0; y<128; y+=inc) {
					final int finalY = y;
					float y_ = y+0.5f;
					for (int xz=0; xz<2048; xz+=inc) {
						float xz_ = xz+0.5f;
						lineXPYP = new LineGeometry.PolyLine().add( xz_,y_,-2047.5).add( xz_,y_, 2047.5);
						lineXNYP = new LineGeometry.PolyLine().add(-xz_,y_,-2047.5).add(-xz_,y_, 2047.5);
						lineZPYP = new LineGeometry.PolyLine().add(-2047.5,y_, xz_).add( 2047.5,y_, xz_);
						lineZNYP = new LineGeometry.PolyLine().add(-2047.5,y_,-xz_).add( 2047.5,y_,-xz_);
						lineXPYN = new LineGeometry.PolyLine().add( xz_,-y_,-2047.5).add( xz_,-y_, 2047.5);
						lineXNYN = new LineGeometry.PolyLine().add(-xz_,-y_,-2047.5).add(-xz_,-y_, 2047.5);
						lineZPYN = new LineGeometry.PolyLine().add(-2047.5,-y_, xz_).add( 2047.5,-y_, xz_);
						lineZNYN = new LineGeometry.PolyLine().add(-2047.5,-y_,-xz_).add( 2047.5,-y_,-xz_);
						LineGeometry.DirectWriteGroupingNode grid = y==0 && xz==0 ? zeroAxes : isGrid2.test(y) && isGrid2.test(xz) ? grid2 : grid1;
						grid.add(lineXPYP, lineXNYP, lineZPYP, lineZNYP, lineXPYN, lineXNYN, lineZPYN, lineZNYN);
					}
					Gui.runInEventThreadAndWait(()->{ pd.setValue(finalY); });
				}
				endTask(startTime);
				
				startTime = startTask(pd, "   ", "Create Vertical Grid Lines", 2047);
				LineGeometry.PolyLine lineXPZP, lineXNZP, lineXPZN, lineXNZN;
				for (int x=0; x<2048; x+=inc) {
					final int finalX = x;
					float x_ = x+0.5f;
					for (int z=0; z<2048; z+=inc) {
						float z_ = z+0.5f;
						lineXPZP = new LineGeometry.PolyLine().add( x_,-127.5, z_).add( x_, 127.5, z_);
						lineXNZP = new LineGeometry.PolyLine().add( x_,-127.5,-z_).add( x_, 127.5,-z_);
						lineXPZN = new LineGeometry.PolyLine().add(-x_,-127.5,-z_).add(-x_, 127.5,-z_);
						lineXNZN = new LineGeometry.PolyLine().add(-x_,-127.5, z_).add(-x_, 127.5, z_);
						LineGeometry.DirectWriteGroupingNode grid = x==0 && z==0 ? zeroAxes : isGrid2.test(x) && isGrid2.test(z) ? grid2 : grid1;
						grid.add(lineXPZP, lineXNZP, lineXPZN, lineXNZN);
					}
					Gui.runInEventThreadAndWait(()->{ pd.setValue(finalX); });
				}
				endTask(startTime);
				
				startTime = startTask(pd, "   ", "Write Grid & Outline");
				
				vrml.println();
				vrml.printf("# Outline%n");
				outlineAndZeroAxes.write(vrml, "", 2, new Color(0x80FFFFFF,true));
				vrml.println();
				
				vrml.printf("# ZeroAxes%n");
				zeroAxes.write(vrml, "", 2, new Color(0x80FFFFFF,true));
				vrml.println();
				
				vrml.printf("# Grid 2%n");
				grid2.write(vrml, "", 2, new Color(0x80A0A0A0,true));
				vrml.println();
				
				vrml.printf("# Grid 1%n");
				grid1.write(vrml, "", 2, new Color(0x80808080,true));
				vrml.println();
				
				endTask(startTime);
				
				LineGeometry.DirectWriteGroupingNode blackHoleJumps = new LineGeometry.DirectWriteGroupingNode();
				startTime = startTask(pd, "   ", "Create BlackHole Jumps");
				for (Region region: galaxy.regions) {
					for (SolarSystem sys:region.solarSystems)
						if (sys.hasBlackHole && sys.blackHoleTarget!=null)
							blackHoleJumps.add(createBlackHoleJump(sys.getUniverseAddress(),sys.blackHoleTarget));
				}
				endTask(startTime);
				
				startTime = startTask(pd, "   ", "Write BlackHole Jumps");
				vrml.printf("# BlackHole Jumps%n");
				blackHoleJumps.write(vrml, "", 2, colorBlackholeConnection);
				vrml.println();
				endTask(startTime);
				
				startTime = startTask(pd, "   ", "Write Regions");
				for (Region region: galaxy.regions) {
					Color color = getcolor==null ? null : getcolor.apply(region);
					if (color==null) color = Color.YELLOW;
					writeCube(vrml,region.voxelX,region.voxelY,region.voxelZ,"%1.1f",1,"%1.1f",color, 0xA0);
				}
				endTask(startTime);
			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			endTask(startTime_total);
			
		};
		runProgressDialogTask(parent, String.format("Map of %s", galaxy.toString()), task);
	}

	private static LineGeometry.PolyLine createBlackHoleJump(UniverseAddress p1, UniverseAddress p2) {
		int p1X = p1.voxelX; int p2X = p2.voxelX;
		int p1Y = p1.voxelY; int p2Y = p2.voxelY;
		int p1Z = p1.voxelZ; int p2Z = p2.voxelZ;
		
		double p1Radius = Math.sqrt(p1X*p1X+p1Z*p1Z);
		double p2Radius = Math.sqrt(p2X*p2X+p2Z*p2Z);
		double p1Angle = Math.atan2(p1Z,p1X);
		double p2Angle = Math.atan2(p2Z,p2X);
		if (p2Angle<p1Angle) p2Angle += 2*Math.PI;
		
		double deltaAngle = p2Angle-p1Angle;
		if (deltaAngle>Math.PI) deltaAngle -= 2*Math.PI;
		long nSeg = Math.round(Math.ceil( Math.abs(deltaAngle) / (Math.PI/50) ));
		
		double segAngle  = deltaAngle/nSeg;
		double segY      = (p2Y-p1Y)/(double)nSeg;
		double segRadius = (p2Radius-p1Radius)/nSeg;
		
		LineGeometry.PolyLine line = new LineGeometry.PolyLine();
		line.add(p1X,p1Y,p1Z);
		for (int i=1; i<nSeg; i++) {
			double y = p1Y      + i*segY;
			double r = p1Radius + i*segRadius;
			double a = p1Angle  + i*segAngle;
			double x = r*Math.cos(a);
			double z = r*Math.sin(a);
			line.add(x,y,z);
		}
		line.add(p2X,p2Y,p2Z);
		
		return line;
	}

	@SuppressWarnings("unused")
	private static void writeCube(PrintWriter vrml, float x, float y, float z, String coordFormat, float size, String sizeFormat, Color color) {
		writeCube(vrml, x, y, z, coordFormat, size, sizeFormat, color, color.getAlpha());
	}

	private static void writeCube(PrintWriter vrml, float x, float y, float z, String coordFormat, float size, String sizeFormat, Color color, int alpha) {
		vrml.printf(Locale.ENGLISH, "Transform { translation "+coordFormat+" "+coordFormat+" "+coordFormat+" children ", x,y,z);
		vrml.printf("Shape {");
		vrml.printf(" appearance Appearance { material Material {");
		vrml.printf(Locale.ENGLISH, " diffuseColor %1.5f %1.5f %1.5f", color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f);
		if (alpha<255) vrml.printf(Locale.ENGLISH, " transparency %1.5f", (255-alpha)/255f);
		vrml.printf(" } }");
		vrml.printf(Locale.ENGLISH, " geometry Box { size "+sizeFormat+" "+sizeFormat+" "+sizeFormat+" }", size, size, size);
		vrml.printf(" } }%n");
	}

	private static void runProgressDialogTask(Window parent, String label, Consumer<ProgressDialog> task) {
		if (parent==null)
			task.accept(null);
		else
			Gui.runWithProgressDialog(parent, "Write "+label+" to VRML", task);
	}

	private static class FreighterRoomCombine {
		private static final String OBJECT_ID_FREIGHTER_CORE = "^FREIGHTER_CORE";

		private enum ObjectID {
			BRIDGECONNECTOR("^BRIDGECONNECTOR"),
			AIRLCKCONNECTOR("^AIRLCKCONNECTOR"),
			CUBEROOM_SPACE (OBJECT_ID_FREIGHTER_CORE, "^CUBEROOM_SPACE", "^CUBEROOMB_SPACE", "^CUBEROOMC_SPACE"),
			S_CONTAINER    ("^S_CONTAINER0", "^S_CONTAINER1", "^S_CONTAINER2", "^S_CONTAINER3", "^S_CONTAINER4",
			                "^S_CONTAINER5", "^S_CONTAINER6", "^S_CONTAINER7", "^S_CONTAINER8", "^S_CONTAINER9", "^GARAGE_FREIGHT"),
			CORRIDOR_SPACE ("^CORRIDOR_SPACE"),
			CORRIDORL_SPACE("^CORRIDORL_SPACE"),
			CORRIDORT_SPACE("^CORRIDORT_SPACE"),
			CORRIDORX_SPACE("^CORRIDORX_SPACE"),
			CORSTAIRS_SPACE("^CORSTAIRS_SPACE"),
			NPCFRIGTERM    ("^NPCFRIGTERM"),
			;
			
			private String[] ids;
			ObjectID(String...ids) {
				this.ids = ids;
			}
			public boolean is(String id) {
				for (String id1:ids)
					if (id1.equals(id))
						return true;
				return false;
			}
			public static ObjectID get(String id) {
				for (ObjectID objectID:ObjectID.values())
					if (objectID.is(id))
						return objectID;
				return null;
			}
		}
		
		private static class PlacingObj {
			
			private static final Point3D VecZpos = new Point3D(0,0,1);
			private static final Point3D VecXpos = new Point3D(1,0,0);

			enum LocalDirection { Xpos, Xneg, Zpos, Zneg;

				public LocalDirection prev() {
					switch (this) {
					case Xneg: return Zneg;
					case Zneg: return Xpos;
					case Xpos: return Zpos;
					case Zpos: return Xneg;
					}
					return null;
				}
	
				public LocalDirection next() {
					switch (this) {
					case Xneg: return Zpos;
					case Zpos: return Xpos;
					case Xpos: return Zneg;
					case Zneg: return Xneg;
					}
					return null;
				}

				public LocalDirection opp() {
					switch (this) {
					case Xneg: return Xpos;
					case Zpos: return Zneg;
					case Xpos: return Xneg;
					case Zneg: return Zpos;
					}
					return null;
				}
			}
			
			private BuildingObject obj;
			private int x;
			private int y;
			private int z;
			private LocalDirection locDir;
			private ObjectID objectID;

			public PlacingObj(BuildingObject obj) {
				this.obj = obj;
				locDir = null;
				setPos(Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE);
				objectID=ObjectID.get(obj.objectID);
			}

			public void setPos(int x, int y, int z) {
				this.x = x;
				this.y = y;
				this.z = z;
			}

			private static boolean isSameDirection(Point3D vec1, Point3D vec2) {
				if (vec1.crossProd(vec2).length() > 0.0001)
					// both "at"s are not parallel
					return false;
				
				if (vec1.normalize().add(vec2.normalize()).length()<1)
					// both "at"s have not same direction
					return false;
				
				return true;
			}

			private static LocalDirection getLocalDirection(Point3D vec) {
				if (vec.crossProd(VecXpos).length() < 0.0001) {
					double x = VecXpos.add(vec.normalize()).length();
					if (x>1.5) return LocalDirection.Xpos;
					if (x<0.5) return LocalDirection.Xneg;
					
				} else if (vec.crossProd(VecZpos).length() < 0.0001) {
					double z = VecZpos.add(vec.normalize()).length();
					if (z>1.5) return LocalDirection.Zpos;
					if (z<0.5) return LocalDirection.Zneg;
				}
				return null;
			}

			private static Point3D getFixedPos(BuildingObject obj) {
				if (ObjectID.BRIDGECONNECTOR.is(obj.objectID))
					return obj.position.pos.add(obj.position.at.mul(4));
				
				//if (ObjectID.NPCFRIGTERM    .is(obj.objectID))
				//	return obj.position.pos.add(obj.position.at.mul(-2)).add(obj.position.up.mul(0.05));
				
				if (ObjectID.AIRLCKCONNECTOR.is(obj.objectID))
					return obj.position.pos.add(obj.position.at.mul(4));
				
				if (ObjectID.CORRIDORL_SPACE.is(obj.objectID))
					return obj.position.pos.add(obj.position.up.crossProd(obj.position.at).mul(-2));
				
				if (ObjectID.CORRIDORT_SPACE.is(obj.objectID))
					return obj.position.pos.add(obj.position.at.mul(4));
				
				return new Point3D(obj.position.pos);
			}

			public boolean isObjPosAsExpected() {
				if (this.obj==null || this.obj.position==null || this.obj.position.pos==null || this.obj.position.at==null || this.obj.position.up==null)
					return false;
				
				if (!isSameDirection(this.obj.position.up,new Point3D(0,1,0)))
					return false;
				
				locDir = getLocalDirection(this.obj.position.at);
				if (locDir==null)
					return false;
				
				return true;
			}

			public PlacingObj createOthogonalObj(BuildingObject obj) {
				PlacingObj placingObj = new PlacingObj(obj);
				
				if (!placingObj.isObjPosAsExpected())
					return null;
				
				Point3D pos = getFixedPos(obj);
				Point3D indexPos = pos.sub(this.obj.position.pos).mul(0.125,0.25,0.125);
				if (Math.abs(indexPos.x-Math.round(indexPos.x))>0.001) return null;
				if (Math.abs(indexPos.y-Math.round(indexPos.y))>0.001) return null;
				if (Math.abs(indexPos.z-Math.round(indexPos.z))>0.001) return null;
				
				// special: AIRLCKCONNECTOR
				if (placingObj.objectID==ObjectID.AIRLCKCONNECTOR) {
					//placingObj.objectID=ObjectID.CORSTAIRS_SPACE;
					placingObj.locDir = placingObj.locDir.prev();
					indexPos.y -= 1;
				}
				
				placingObj.setPos(
					(int)Math.round(indexPos.x),
					(int)Math.round(indexPos.y),
					(int)Math.round(indexPos.z)
				);
				
				return placingObj;
			}
			
		}

		private static class Raster {
			
			private PlacingObj[][][] objects;
			public int sizeX;
			public int sizeY;
			public int sizeZ;
			public int offsetX;
			public int offsetY;
			public int offsetZ;
			
			public Raster() {
				objects = null;
				offsetX = 0; sizeX = 1;
				offsetY = 0; sizeY = 1;
				offsetZ = 0; sizeZ = 1;
			}
			
			public void setRange(PlacingObj obj) {
				int maxX = offsetX+sizeX-1;
				int maxY = offsetY+sizeY-1;
				int maxZ = offsetZ+sizeZ-1;
				offsetX = Math.min( obj.x, offsetX );
				offsetY = Math.min( obj.y, offsetY );
				offsetZ = Math.min( obj.z, offsetZ );
				sizeX   = Math.max( obj.x, maxX )-offsetX+1;
				sizeY   = Math.max( obj.y, maxY )-offsetY+1;
				sizeZ   = Math.max( obj.z, maxZ )-offsetZ+1;
			}

			public void createEmptyRaster() {
				objects = new PlacingObj[sizeX][sizeY][sizeZ];
				for (int x=0; x<sizeX; x++)
					for (int y=0; y<sizeY; y++)
						Arrays.fill(objects[x][y],null);
			}
			
			public void set(PlacingObj obj) {
				objects[obj.x-offsetX][obj.y-offsetY][obj.z-offsetZ] = obj;
			}
			
//			public boolean is(int x, int y, int z, int dx, int dy, int dz, ObjectID objectID) {
//				PlacingObj rasterObj = get(x+dx,y+dy,z+dz);
//				if (rasterObj==null) return false;
//				return (rasterObj.objectID==objectID);
//			}

			public PlacingObj get(int x, int y, int z) {
				if (x<0 || x>=sizeX) return null;
				if (y<0 || y>=sizeY) return null;
				if (z<0 || z>=sizeZ) return null;
				return objects[x][y][z];
			}
		}

		final Vector<BuildingObject> remainingObjects;
		@SuppressWarnings("unused")
		private PrintWriter vrml;
		
		FreighterRoomCombine() {
			remainingObjects = new Vector<>();
			vrml = null;
		}

		public boolean isEmpty() {
			return remainingObjects.isEmpty();
		}

		public BuildingObject[] getRemainingObjects() {
			return remainingObjects.toArray(new BuildingObject[0]);
		}

		public boolean add(BuildingObject obj) {
			if (obj==null) return false;
			for (ObjectID id:ObjectID.values())
				if (id.is(obj.objectID)) {
					remainingObjects.add(obj);
					return true;
				}
			return false;
		}
		
		public void writeModel(PrintWriter vrml, ProgressDialog pd) {
			if (remainingObjects.isEmpty())
				return;
			
			this.vrml = vrml;
			long startTime;
			
			startTime = startTask(pd, "      ", "Create Raster");
			Raster raster = createRaster();
			endTask(startTime);
			
			if (raster==null) {
				this.vrml = null;
				return;
			}
			
			Vector<SingleText> extraTexts = new Vector<>();
			startTime = startTask(pd, "      ", "Create Geometry");
			LineGeometry.IndexedLineSet geometry = createGeometry(raster,extraTexts);
			endTask(startTime);
			
			startTime = startTask(pd, "      ", "Write Geometry to File");
			Gui.log_ln("");
			LineGeometry.writeIndexedLineSet_verbose(vrml, geometry, "", Color.BLACK, pd, "         ");
			Gui.log("      ");
			endTask(startTime);
			
			if (!extraTexts.isEmpty()) {
				startTime = startTask(pd, "      ", "Write Texts to File");
				for (SingleText txt:extraTexts)
					VRMLoutput.writeSingleTextNode(vrml, txt.text, txt.pos, txt.at, txt.up);
				endTask(startTime);
			}
			this.vrml = null;
		}
		
		private static class SingleText {

			public String text;
			public Point3D pos;
			public Point3D at;
			public Point3D up;
			
			public SingleText(BuildingObject obj) {
				text = VRMLoutput.getLabel(obj.objectID);
				pos = obj.position.pos;
				at  = obj.position.at;
				up  = obj.position.up;
			}
		}

		private Raster createRaster() {
			PlacingObj anchor = null;
			Vector<PlacingObj> placingObjects = new Vector<>();
			
			for (BuildingObject obj : remainingObjects)
				if (OBJECT_ID_FREIGHTER_CORE.equals(obj.objectID)) {
					anchor = new PlacingObj(obj);
					break;
				}
			if (anchor==null)
				return null;
			
			anchor.setPos(0,0,0);
			placingObjects.add(anchor);
			
			if (!anchor.isObjPosAsExpected())
				return null;
			
			remainingObjects.remove(anchor.obj);
			
			Raster raster = new Raster();
			for (int i=0; i<remainingObjects.size();) {
				BuildingObject obj = remainingObjects.get(i);
				PlacingObj placingObj;
				if ((placingObj = anchor.createOthogonalObj(obj))!=null) {
					remainingObjects.remove(i);
					raster.setRange(placingObj);
					placingObjects.add(placingObj);
				} else
					i++;
			}
			
			raster.createEmptyRaster();
			
			for (PlacingObj obj:placingObjects)
				raster.set(obj);
			
			return raster;
		}

		private LineGeometry.IndexedLineSet createGeometry(Raster raster, Vector<SingleText> extraTexts) {
			
			LineGeometry.GroupingNode baseGroup = new LineGeometry.GroupingNode();
			for (int x=0; x<raster.sizeX; x++)
				for (int y=0; y<raster.sizeY; y++)
					for (int z=0; z<raster.sizeZ; z++) {
						PlacingObj obj = raster.get(x,y,z);
						if (obj==null) continue;
						
						LineGeometry.IndexedLineSet objGeometry = null;
						switch (obj.objectID) {
						case CUBEROOM_SPACE : objGeometry = new CUBEROOM(raster,x,y,z).createGeometry(); break;
						case AIRLCKCONNECTOR: extraTexts.add(new SingleText(obj.obj)); 
						case CORSTAIRS_SPACE: objGeometry = new CORSTAIRS  (obj).createGeometry(); break;
						case CORRIDOR_SPACE : objGeometry = new CORRIDOR   (obj).createGeometry(); break;
						case CORRIDORL_SPACE: objGeometry = new CORRIDOR_L (obj).createGeometry(); break;
						case CORRIDORT_SPACE: objGeometry = new CORRIDOR_T (obj).createGeometry(); break;
						case CORRIDORX_SPACE: objGeometry = new CORRIDOR_X ()   .createGeometry(); break;
						case S_CONTAINER    : objGeometry = new S_CONTAINER()   .createGeometry(); extraTexts.add(new SingleText(obj.obj)); break;
						case NPCFRIGTERM    : objGeometry = NPCFRIGTERM.create(obj).createGeometry(); extraTexts.add(new SingleText(obj.obj)); break;
						case BRIDGECONNECTOR: extraTexts.add(new SingleText(obj.obj)); break;
						}
						if (objGeometry!=null)
							baseGroup.add(
								new LineGeometry.Transform(objGeometry)
								.addTranslation(new Point3D(x*8,y*4,z*8))
							);
					}
			
			PlacingObj anchor = raster.get(-raster.offsetX,-raster.offsetY,-raster.offsetZ);
			
			return new LineGeometry.Transform(baseGroup)
				.addTranslation(new Point3D(raster.offsetX*8,raster.offsetY*4,raster.offsetZ*8))
				.addTranslation(anchor.obj.position.pos);
		}

		private static boolean needDoorToNeighbor(PlacingObj neighbor, PlacingObj neighbor_sub, PlacingObj.LocalDirection dirToNeighbor) {
			return
				is(neighbor    ,ObjectID.NPCFRIGTERM    , dirToNeighbor.opp ()) ||
				
				is(neighbor    ,ObjectID.CORRIDOR_SPACE , dirToNeighbor.next()) ||
				is(neighbor    ,ObjectID.CORRIDOR_SPACE , dirToNeighbor.prev()) ||
				
				is(neighbor    ,ObjectID.CORRIDORL_SPACE, dirToNeighbor.next()) ||
				is(neighbor    ,ObjectID.CORRIDORL_SPACE, dirToNeighbor       ) ||
				
				is(neighbor    ,ObjectID.CORRIDORT_SPACE, dirToNeighbor       ) ||
				is(neighbor    ,ObjectID.CORRIDORT_SPACE, dirToNeighbor.next()) ||
				is(neighbor    ,ObjectID.CORRIDORT_SPACE, dirToNeighbor.prev()) ||
				
				is(neighbor    ,ObjectID.AIRLCKCONNECTOR, dirToNeighbor.next()) ||
				is(neighbor_sub,ObjectID.AIRLCKCONNECTOR, dirToNeighbor.prev()) ||
				is(neighbor    ,ObjectID.CORSTAIRS_SPACE, dirToNeighbor.next()) ||
				is(neighbor_sub,ObjectID.CORSTAIRS_SPACE, dirToNeighbor.prev()) ||
				
				is(neighbor    ,ObjectID.CORRIDORX_SPACE) ||
				
				is(neighbor    ,ObjectID.S_CONTAINER);
		}

		private static boolean is(PlacingObj neighbor, ObjectID objectID) {
			return is(neighbor, objectID, null);
		}

		private static boolean is(PlacingObj neighbor, ObjectID objectID, PlacingObj.LocalDirection neighborLocDir) {
			return neighbor!=null && (objectID==null || neighbor.objectID==objectID) && (neighborLocDir==null || neighbor.locDir==neighborLocDir);
		}

		private static class CORRIDOR {
		
			private PlacingObj obj;

			public CORRIDOR(PlacingObj obj) {
				this.obj = obj;
			}
		
			public LineGeometry.IndexedLineSet createGeometry() {
				LineGeometry.PolyLine fullProfile = new LineGeometry.PolyLine()
					.addArc(LineGeometry.Axis.Z, new Point3D(1.4,0.6,0), 0.6, 270,360, false)
					.addArc(LineGeometry.Axis.Z, new Point3D(1.4,2.7,0), 0.6,   0, 90, false)
					.add(new Point3D( 1.0,3.3,0))
					.add(new Point3D( 0.5,3.8,0))
					.add(new Point3D(-0.5,3.8,0))
					.add(new Point3D(-1.0,3.3,0))
					.addArc(LineGeometry.Axis.Z, new Point3D(-1.4,2.7,0), 0.6,  90,180, false)
					.addArc(LineGeometry.Axis.Z, new Point3D(-1.4,0.6,0), 0.6, 180,270, false)
					.close();
				
				LineGeometry.OptimizeNode objWestEast = new LineGeometry.OptimizeNode(
					new LineGeometry.Transform(fullProfile).addTranslation(new Point3D(0,0,-4)),
					fullProfile,
					new LineGeometry.Transform(fullProfile).addTranslation(new Point3D(0,0,4)),
					
					new LineGeometry.PolyLine( new Point3D(-0.5,3.8,-4), new Point3D(-0.5,3.8,4) ),
					new LineGeometry.PolyLine( new Point3D(-1.0,3.3,-4), new Point3D(-1.0,3.3,4) ),
					new LineGeometry.PolyLine( new Point3D(-1.4,3.3,-4), new Point3D(-1.4,3.3,4) ),
					new LineGeometry.PolyLine( new Point3D(-2.0,2.7,-4), new Point3D(-2.0,2.7,4) ),
					new LineGeometry.PolyLine( new Point3D(-2.0,0.6,-4), new Point3D(-2.0,0.6,4) ),
					new LineGeometry.PolyLine( new Point3D(-1.4,0.0,-4), new Point3D(-1.4,0.0,4) ),
					new LineGeometry.PolyLine( new Point3D(-1.0,0.0,-4), new Point3D(-1.0,0.0,4) ),
					new LineGeometry.PolyLine( new Point3D( 0.0,0.0,-4), new Point3D( 0.0,0.0,4) ),
					new LineGeometry.PolyLine( new Point3D( 1.0,0.0,-4), new Point3D( 1.0,0.0,4) ),
					new LineGeometry.PolyLine( new Point3D( 1.4,0.0,-4), new Point3D( 1.4,0.0,4) ),
					new LineGeometry.PolyLine( new Point3D( 2.0,0.6,-4), new Point3D( 2.0,0.6,4) ),
					new LineGeometry.PolyLine( new Point3D( 2.0,2.7,-4), new Point3D( 2.0,2.7,4) ),
					new LineGeometry.PolyLine( new Point3D( 1.4,3.3,-4), new Point3D( 1.4,3.3,4) ),
					new LineGeometry.PolyLine( new Point3D( 1.0,3.3,-4), new Point3D( 1.0,3.3,4) ),
					new LineGeometry.PolyLine( new Point3D( 0.5,3.8,-4), new Point3D( 0.5,3.8,4) ),
					
					new LineGeometry.PolyLine( new Point3D(-1.4,0,-2), new Point3D(1.4,0,-2) ),
					new LineGeometry.PolyLine( new Point3D(-1.4,0, 2), new Point3D(1.4,0, 2) ),
					new LineGeometry.PolyLine( new Point3D(-1.0,0,-1), new Point3D(1.0,0,-1) ),
					new LineGeometry.PolyLine( new Point3D(-1.0,0, 1), new Point3D(1.0,0, 1) ),
					new LineGeometry.PolyLine( new Point3D(-1.0,0,-3), new Point3D(1.0,0,-3) ),
					new LineGeometry.PolyLine( new Point3D(-1.0,0, 3), new Point3D(1.0,0, 3) )
				);
				
				switch (obj.locDir) {
				case Xpos:
				case Xneg: return objWestEast;
				case Zneg:
				case Zpos: return new LineGeometry.Transform(objWestEast).addRotation(LineGeometry.Axis.Y, 90);
				}
				return null;
			}
		}

		private static class CORRIDOR_T {
		
			private PlacingObj obj;

			public CORRIDOR_T(PlacingObj obj) {
				this.obj = obj;
			}
		
			public LineGeometry.IndexedLineSet createGeometry() {
				LineGeometry.PolyLine profile = new LineGeometry.PolyLine()
					.addArc(LineGeometry.Axis.Z, new Point3D(1.4,0.6,4), 0.6, 270,360, false)
					.addArc(LineGeometry.Axis.Z, new Point3D(1.4,2.7,4), 0.6,   0, 90, false)
					.add(new Point3D( 1.0,3.3,4))
					.add(new Point3D( 0.5,3.8,4))
					.add(new Point3D(-0.5,3.8,4))
					.add(new Point3D(-1.0,3.3,4))
					.addArc(LineGeometry.Axis.Z, new Point3D(-1.4,2.7,4), 0.6,  90,180, false)
					.addArc(LineGeometry.Axis.Z, new Point3D(-1.4,0.6,4), 0.6, 180,270, false)
					.close();
				
				LineGeometry.GroupingNode cornerNE = new LineGeometry.GroupingNode(
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(4,3.8,4), 4.0-0.5, 180,270, false, 8),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(4,3.3,4), 4.0-1.0, 180,270, false, 8),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(4,3.3,4), 4.0-1.4, 180,270, false, 8),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(4,2.7,4), 4.0-2.0, 180,270, false, 8),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(4,0.6,4), 4.0-2.0, 180,270, false, 8),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(4,0.0,4), 4.0-1.4, 180,270, false, 8),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(4,0.0,4), 4.0-1.0, 180,270, false, 8),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(4,0.0,4), 4.0+0.0, 180,270, false, 8),
					
					new LineGeometry.Transform( new LineGeometry.PolyLine( new Point3D(0,0,-4), new Point3D(0,0,-4+1.0) ) ).addRotation(LineGeometry.Axis.Y,22.5).addTranslation(new Point3D(4,0,4)),
					new LineGeometry.Transform( new LineGeometry.PolyLine( new Point3D(0,0,-4), new Point3D(0,0,-4+1.4) ) ).addRotation(LineGeometry.Axis.Y,45  ).addTranslation(new Point3D(4,0,4)),
					new LineGeometry.Transform( new LineGeometry.PolyLine( new Point3D(0,0,-4), new Point3D(0,0,-4+1.0) ) ).addRotation(LineGeometry.Axis.Y,67.5).addTranslation(new Point3D(4,0,4))
					
				);
				
				LineGeometry.OptimizeNode tcrossWSE = new LineGeometry.OptimizeNode(
					new LineGeometry.Transform(cornerNE).addRotation(LineGeometry.Axis.Y, -90),
					new LineGeometry.Transform(cornerNE).addRotation(LineGeometry.Axis.Y,-180),
					profile,
					new LineGeometry.Transform(profile).addRotation(LineGeometry.Axis.Y,-90),
					new LineGeometry.Transform(profile).addRotation(LineGeometry.Axis.Y,-180),
						
					new LineGeometry.PolyLine( new Point3D( 0.0,0.0,-4), new Point3D( 0.0,0.0,4) ),
					new LineGeometry.PolyLine( new Point3D( 1.0,0.0,-4), new Point3D( 1.0,0.0,4) ),
					new LineGeometry.PolyLine( new Point3D( 1.4,0.0,-4), new Point3D( 1.4,0.0,4) ),
					new LineGeometry.PolyLine( new Point3D( 2.0,0.6,-4), new Point3D( 2.0,0.6,4) ),
					new LineGeometry.PolyLine( new Point3D( 2.0,2.7,-4), new Point3D( 2.0,2.7,4) ),
					new LineGeometry.PolyLine( new Point3D( 1.4,3.3,-4), new Point3D( 1.4,3.3,4) ),
					new LineGeometry.PolyLine( new Point3D( 1.0,3.3,-4), new Point3D( 1.0,3.3,4) ),
					new LineGeometry.PolyLine( new Point3D( 0.5,3.8,-4), new Point3D( 0.5,3.8,4) ),
					
					new LineGeometry.PolyLine( new Point3D(0,0,-3), new Point3D(1.0,0,-3) ),
					new LineGeometry.PolyLine( new Point3D(0,0,-2), new Point3D(1.4,0,-2) ),
					new LineGeometry.PolyLine( new Point3D(0,0,-1), new Point3D(1.0,0,-1) ),
					
					new LineGeometry.PolyLine()
						.add(new Point3D(-4,0,0))
						.addArc(LineGeometry.Axis.Z, new Point3D(1.4,0.6,0), 0.6, 270,360, false)
						.addArc(LineGeometry.Axis.Z, new Point3D(1.4,2.7,0), 0.6,   0, 90, false)
						.add(new Point3D( 1.0,3.3,0))
						.add(new Point3D( 0.5,3.8,0)),
					
					new LineGeometry.PolyLine( new Point3D(0,0, 1), new Point3D(1.0,0, 1) ),
					new LineGeometry.PolyLine( new Point3D(0,0, 2), new Point3D(1.4,0, 2) ),
					new LineGeometry.PolyLine( new Point3D(0,0, 3), new Point3D(1.0,0, 3) )
				);
				
				switch (obj.locDir) {
				case Xpos: return tcrossWSE;
				case Zneg: return new LineGeometry.Transform(tcrossWSE).addRotation(LineGeometry.Axis.Y, 90);
				case Xneg: return new LineGeometry.Transform(tcrossWSE).addRotation(LineGeometry.Axis.Y,180);
				case Zpos: return new LineGeometry.Transform(tcrossWSE).addRotation(LineGeometry.Axis.Y,270);
				}
				return null;
			}
		}

		private static class CORRIDOR_L {
		
			private PlacingObj obj;

			public CORRIDOR_L(PlacingObj obj) {
				this.obj = obj;
			}
		
			public LineGeometry.IndexedLineSet createGeometry() {
				
				LineGeometry.PolyLine fullProfile = new LineGeometry.PolyLine()
					.addArc(LineGeometry.Axis.Z, new Point3D(1.4,0.6,0), 0.6, 270,360, false)
					.addArc(LineGeometry.Axis.Z, new Point3D(1.4,2.7,0), 0.6,   0, 90, false)
					.add(new Point3D( 1.0,3.3,0))
					.add(new Point3D( 0.5,3.8,0))
					.add(new Point3D(-0.5,3.8,0))
					.add(new Point3D(-1.0,3.3,0))
					.addArc(LineGeometry.Axis.Z, new Point3D(-1.4,2.7,0), 0.6,  90,180, false)
					.addArc(LineGeometry.Axis.Z, new Point3D(-1.4,0.6,0), 0.6, 180,270, false)
					.close();
				
				LineGeometry.Transform objWestEast = new LineGeometry.Transform(
					new LineGeometry.OptimizeNode(
						new LineGeometry.Transform(fullProfile).addTranslation(new Point3D(4,0,0)),
						new LineGeometry.Transform(fullProfile).addTranslation(new Point3D(4,0,0)).addRotation(LineGeometry.Axis.Y, -45),
						new LineGeometry.Transform(fullProfile).addTranslation(new Point3D(4,0,0)).addRotation(LineGeometry.Axis.Y, -90),
						
						new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,3.8,0), 4.0-0.5, 0,90, false, 8),
						new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,3.3,0), 4.0-1.0, 0,90, false, 8),
						new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,3.3,0), 4.0-1.4, 0,90, false, 8),
						new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,2.7,0), 4.0-2.0, 0,90, false, 8),
						new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,0.6,0), 4.0-2.0, 0,90, false, 8),
						new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,0.0,0), 4.0-1.4, 0,90, false, 8),
						new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,0.0,0), 4.0-1.0, 0,90, false, 8),
						new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,0.0,0), 4.0+0.0, 0,90, false, 8),
						new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,0.0,0), 4.0+1.0, 0,90, false, 8),
						new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,0.0,0), 4.0+1.4, 0,90, false, 8),
						new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,0.6,0), 4.0+2.0, 0,90, false, 8),
						new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,2.7,0), 4.0+2.0, 0,90, false, 8),
						new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,3.3,0), 4.0+1.4, 0,90, false, 8),
						new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,3.3,0), 4.0+1.0, 0,90, false, 8),
						new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,3.8,0), 4.0+0.5, 0,90, false, 8),
						
						new LineGeometry.Transform( new LineGeometry.PolyLine( new Point3D(4.0+0.0,0,0), new Point3D(4.0+1.0,0,0) ) ).addRotation(LineGeometry.Axis.Y, -11.25),
						new LineGeometry.Transform( new LineGeometry.PolyLine( new Point3D(4.0-1.4,0,0), new Point3D(4.0+1.4,0,0) ) ).addRotation(LineGeometry.Axis.Y, -22.5 ),
						new LineGeometry.Transform( new LineGeometry.PolyLine( new Point3D(4.0+0.0,0,0), new Point3D(4.0+1.0,0,0) ) ).addRotation(LineGeometry.Axis.Y, -33.75),
						new LineGeometry.Transform( new LineGeometry.PolyLine( new Point3D(4.0+0.0,0,0), new Point3D(4.0+1.0,0,0) ) ).addRotation(LineGeometry.Axis.Y, -56.25),
						new LineGeometry.Transform( new LineGeometry.PolyLine( new Point3D(4.0-1.4,0,0), new Point3D(4.0+1.4,0,0) ) ).addRotation(LineGeometry.Axis.Y, -67.5 ),
						new LineGeometry.Transform( new LineGeometry.PolyLine( new Point3D(4.0+0.0,0,0), new Point3D(4.0+1.0,0,0) ) ).addRotation(LineGeometry.Axis.Y, -78.75)
					)
				).addTranslation(new Point3D(-4,0,-4));
				
				switch (obj.locDir) {
				case Xpos: return objWestEast;
				case Zneg: return objWestEast.addRotation(LineGeometry.Axis.Y, 90);
				case Xneg: return objWestEast.addRotation(LineGeometry.Axis.Y,180);
				case Zpos: return objWestEast.addRotation(LineGeometry.Axis.Y,270);
				}
				return null;
			}
		}

		private static class CORRIDOR_X {
		
			public CORRIDOR_X() {}
		
			public LineGeometry.IndexedLineSet createGeometry() {
				
				LineGeometry.GroupingNode cornerNE = new LineGeometry.GroupingNode(
					new LineGeometry.PolyLine()
						.addArc(LineGeometry.Axis.Z, new Point3D(1.4,0.6,4), 0.6, 270,360, false)
						.addArc(LineGeometry.Axis.Z, new Point3D(1.4,2.7,4), 0.6,   0, 90, false)
						.add(new Point3D( 1.0,3.3,4))
						.add(new Point3D( 0.5,3.8,4))
						.add(new Point3D(-0.5,3.8,4))
						.add(new Point3D(-1.0,3.3,4))
						.addArc(LineGeometry.Axis.Z, new Point3D(-1.4,2.7,4), 0.6,  90,180, false)
						.addArc(LineGeometry.Axis.Z, new Point3D(-1.4,0.6,4), 0.6, 180,270, false)
						.close(),
						
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(4,3.8,4), 4.0-0.5, 180,270, false, 8),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(4,3.3,4), 4.0-1.0, 180,270, false, 8),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(4,3.3,4), 4.0-1.4, 180,270, false, 8),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(4,2.7,4), 4.0-2.0, 180,270, false, 8),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(4,0.6,4), 4.0-2.0, 180,270, false, 8),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(4,0.0,4), 4.0-1.4, 180,270, false, 8),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(4,0.0,4), 4.0-1.0, 180,270, false, 8),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(4,0.0,4), 4.0+0.0, 180,270, false, 8),
					
					new LineGeometry.Transform( new LineGeometry.PolyLine( new Point3D(0,0,-4), new Point3D(0,0,-4+1.0) ) ).addRotation(LineGeometry.Axis.Y,22.5).addTranslation(new Point3D(4,0,4)),
					new LineGeometry.Transform( new LineGeometry.PolyLine( new Point3D(0,0,-4), new Point3D(0,0,-4+1.4) ) ).addRotation(LineGeometry.Axis.Y,45  ).addTranslation(new Point3D(4,0,4)),
					new LineGeometry.Transform( new LineGeometry.PolyLine( new Point3D(0,0,-4), new Point3D(0,0,-4+1.0) ) ).addRotation(LineGeometry.Axis.Y,67.5).addTranslation(new Point3D(4,0,4)),
					
					new LineGeometry.PolyLine( new Point3D(0,0,0), new Point3D(0,0,4) )
				);
				
				return new LineGeometry.OptimizeNode(
					cornerNE,
					new LineGeometry.Transform(cornerNE).addRotation(LineGeometry.Axis.Y, 90),
					new LineGeometry.Transform(cornerNE).addRotation(LineGeometry.Axis.Y,180),
					new LineGeometry.Transform(cornerNE).addRotation(LineGeometry.Axis.Y,270)
				);
			}
		}

		private static class CORSTAIRS {
		
			private PlacingObj obj;

			public CORSTAIRS(PlacingObj obj) {
				this.obj = obj;
			}

			public LineGeometry.IndexedLineSet createGeometry() {
				LineGeometry.PolyLine fullProfile = new LineGeometry.PolyLine()
					.addArc(LineGeometry.Axis.Z, new Point3D(1.4,0.6,0), 0.6, 270,360, false)
					.addArc(LineGeometry.Axis.Z, new Point3D(1.4,2.7,0), 0.6,   0, 90, false)
					.add(new Point3D( 1.0,3.3,0))
					.add(new Point3D( 0.5,3.8,0))
					.add(new Point3D(-0.5,3.8,0))
					.add(new Point3D(-1.0,3.3,0))
					.addArc(LineGeometry.Axis.Z, new Point3D(-1.4,2.7,0), 0.6,  90,180, false)
					.addArc(LineGeometry.Axis.Z, new Point3D(-1.4,0.6,0), 0.6, 180,270, false)
					.close();
				
				LineGeometry.GroupingNode objWestEast = new LineGeometry.OptimizeNode(
					new LineGeometry.Transform(fullProfile).addTranslation(new Point3D(0,0,-4)),
					new LineGeometry.Transform(fullProfile).addTranslation(new Point3D(0,0,-3)),
					new LineGeometry.Transform(fullProfile).addTranslation(new Point3D(0,2, 0)),
					new LineGeometry.Transform(fullProfile).addTranslation(new Point3D(0,4, 3)),
					new LineGeometry.Transform(fullProfile).addTranslation(new Point3D(0,4, 4)),
					
					new LineGeometry.PolyLine( new Point3D(-0.5,3.8,-4), new Point3D(-0.5,3.8,-3), new Point3D(-0.5,3.8+4,3), new Point3D(-0.5,3.8+4,4) ),
					new LineGeometry.PolyLine( new Point3D(-1.0,3.3,-4), new Point3D(-1.0,3.3,-3), new Point3D(-1.0,3.3+4,3), new Point3D(-1.0,3.3+4,4) ),
					new LineGeometry.PolyLine( new Point3D(-1.4,3.3,-4), new Point3D(-1.4,3.3,-3), new Point3D(-1.4,3.3+4,3), new Point3D(-1.4,3.3+4,4) ),
					new LineGeometry.PolyLine( new Point3D(-2.0,2.7,-4), new Point3D(-2.0,2.7,-3), new Point3D(-2.0,2.7+4,3), new Point3D(-2.0,2.7+4,4) ),
					new LineGeometry.PolyLine( new Point3D(-2.0,0.6,-4), new Point3D(-2.0,0.6,-3), new Point3D(-2.0,0.6+4,3), new Point3D(-2.0,0.6+4,4) ),
					new LineGeometry.PolyLine( new Point3D(-1.4,0.0,-4), new Point3D(-1.4,0.0,-3), new Point3D(-1.4,0.0+4,3), new Point3D(-1.4,0.0+4,4) ),
					new LineGeometry.PolyLine( new Point3D(-1.0,0.0,-4), new Point3D(-1.0,0.0,-3), new Point3D(-1.0,0.0+4,3), new Point3D(-1.0,0.0+4,4) ),
					new LineGeometry.PolyLine( new Point3D( 0.0,0.0,-4), new Point3D( 0.0,0.0,-3), new Point3D( 0.0,0.0+4,3), new Point3D( 0.0,0.0+4,4) ),
					new LineGeometry.PolyLine( new Point3D( 1.0,0.0,-4), new Point3D( 1.0,0.0,-3), new Point3D( 1.0,0.0+4,3), new Point3D( 1.0,0.0+4,4) ),
					new LineGeometry.PolyLine( new Point3D( 1.4,0.0,-4), new Point3D( 1.4,0.0,-3), new Point3D( 1.4,0.0+4,3), new Point3D( 1.4,0.0+4,4) ),
					new LineGeometry.PolyLine( new Point3D( 2.0,0.6,-4), new Point3D( 2.0,0.6,-3), new Point3D( 2.0,0.6+4,3), new Point3D( 2.0,0.6+4,4) ),
					new LineGeometry.PolyLine( new Point3D( 2.0,2.7,-4), new Point3D( 2.0,2.7,-3), new Point3D( 2.0,2.7+4,3), new Point3D( 2.0,2.7+4,4) ),
					new LineGeometry.PolyLine( new Point3D( 1.4,3.3,-4), new Point3D( 1.4,3.3,-3), new Point3D( 1.4,3.3+4,3), new Point3D( 1.4,3.3+4,4) ),
					new LineGeometry.PolyLine( new Point3D( 1.0,3.3,-4), new Point3D( 1.0,3.3,-3), new Point3D( 1.0,3.3+4,3), new Point3D( 1.0,3.3+4,4) ),
					new LineGeometry.PolyLine( new Point3D( 0.5,3.8,-4), new Point3D( 0.5,3.8,-3), new Point3D( 0.5,3.8+4,3), new Point3D( 0.5,3.8+4,4) ),
					
					new LineGeometry.PolyLine( new Point3D(-1.0,0.5,-2.25), new Point3D(1.0,0.5,-2.25) ),
					new LineGeometry.PolyLine( new Point3D(-1.4,1.0,-1.5 ), new Point3D(1.4,1.0,-1.5 ) ),
					new LineGeometry.PolyLine( new Point3D(-1.0,1.5,-0.75), new Point3D(1.0,1.5,-0.75) ),
					new LineGeometry.PolyLine( new Point3D(-1.0,2.5, 0.75), new Point3D(1.0,2.5, 0.75) ),
					new LineGeometry.PolyLine( new Point3D(-1.4,3.0, 1.5 ), new Point3D(1.4,3.0, 1.5 ) ),
					new LineGeometry.PolyLine( new Point3D(-1.0,3.5, 2.25), new Point3D(1.0,3.5, 2.25) )
				);
				
				switch (obj.locDir) {
				case Xpos: return objWestEast;
				case Zneg: return new LineGeometry.Transform(objWestEast).addRotation(LineGeometry.Axis.Y, 90);
				case Xneg: return new LineGeometry.Transform(objWestEast).addRotation(LineGeometry.Axis.Y,180);
				case Zpos: return new LineGeometry.Transform(objWestEast).addRotation(LineGeometry.Axis.Y,270);
				}
				return null;
			}
		}

		@SuppressWarnings("unused")
		private static class NPCFRIGTERM_old {
		
			private PlacingObj obj;
		
			public NPCFRIGTERM_old(PlacingObj obj) {
				this.obj = obj;
			}
		
			public LineGeometry.IndexedLineSet createGeometry() {
				LineGeometry.IndexedLineSet tempObj;
				LineGeometry.Transform roomObjS = new LineGeometry.Transform(
					new LineGeometry.OptimizeNode(
						new LineGeometry.Transform(tempObj = CUBEROOM.createInsideCornerNE()).addTranslation(new Point3D(3,0,3)),
						new LineGeometry.Transform(tempObj).addRotation(LineGeometry.Axis.Y, 90).addTranslation(new Point3D( 3,0,-3)),
						new LineGeometry.Transform(tempObj).addRotation(LineGeometry.Axis.Y,180).addTranslation(new Point3D(-3,0,-3)),
						new LineGeometry.Transform(tempObj).addRotation(LineGeometry.Axis.Y,270).addTranslation(new Point3D(-3,0, 3)),
						
						new LineGeometry.Transform(tempObj = CUBEROOM.createWallN()).addTranslation(new Point3D(3,0,0)),
						new LineGeometry.Transform(tempObj).addRotation(LineGeometry.Axis.Y, 90).addTranslation(new Point3D(0,0,-3)),
						new LineGeometry.Transform(tempObj).addRotation(LineGeometry.Axis.Y,270).addTranslation(new Point3D(0,0, 3)),
						
						new LineGeometry.Transform(createWallDoorN_terminal()).addRotation(LineGeometry.Axis.Y,180).addTranslation(new Point3D(-3,0,0)),
						
						CUBEROOM.createCeilingLight(),
						CUBEROOM.createFloor()
					)
				).addTranslation(new Point3D(-1,0,0));
				
				switch (obj.locDir) {
				case Xneg: return roomObjS;
				case Zpos: return roomObjS.addRotation(LineGeometry.Axis.Y, 90);
				case Xpos: return roomObjS.addRotation(LineGeometry.Axis.Y,180);
				case Zneg: return roomObjS.addRotation(LineGeometry.Axis.Y,270);
				}
				return null;
			}
		
			private static LineGeometry.IndexedLineSet createWallDoorN_terminal() {
				return new LineGeometry.GroupingNode(
					new LineGeometry.PolyLine()
						.add(new Point3D(-1.0,0.0,2))
						.addArc(LineGeometry.Axis.Z, new Point3D(-0.6,0.6,2), 0.6, 270, 360, false)
						.addArc(LineGeometry.Axis.Z, new Point3D(-0.6,2.7,2), 0.6,   0,  90, false)
						.add(new Point3D(-1.0,3.3,2)),
					
					new LineGeometry.PolyLine()
						.addArc(LineGeometry.Axis.X, new Point3D(0,0.6, 1.4), 0.6,  90,180, false)
						.addArc(LineGeometry.Axis.X, new Point3D(0,0.6,-1.4), 0.6, 180,270, false),
					
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,0,-2), 0.6, 270,360, false),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,0,-2), 1.0, 270,360, false),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,0, 2), 1.0, 180,270, false),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,0, 2), 0.6, 180,270, false),
					
					new LineGeometry.PolyLine( new Point3D( 0,0, 1), new Point3D(-1,0, 1) ),
					new LineGeometry.PolyLine( new Point3D( 0,0, 0), new Point3D(-1,0, 0) ),
					new LineGeometry.PolyLine( new Point3D( 0,0,-1), new Point3D(-1,0,-1) ),
					
					new LineGeometry.PolyLine( new Point3D( 0.0,2.7,-2), new Point3D( 0.0,2.7,2) ),
					new LineGeometry.PolyLine( new Point3D(-0.6,3.3,-2), new Point3D(-0.6,3.3,2) ),
					new LineGeometry.PolyLine( new Point3D(-1.0,3.3,-2), new Point3D(-1.0,3.3,2) ),
					new LineGeometry.PolyLine( new Point3D(-1.5,3.8,-1.5), new Point3D(-1.5,3.8,1.5) )
				);
			}
		}

		private static class NPCFRIGTERM extends CUBEROOM {
			
			private PlacingObj.LocalDirection dirOfExit;

			private NPCFRIGTERM(PlacingObj.LocalDirection dirOfExit, Raster raster, int x, int y, int z) {
				super(raster, x, y, z);
				this.dirOfExit = dirOfExit;
			}

			public static NPCFRIGTERM create(PlacingObj obj) {
				Raster raster = createDummyRaster_ExitOnXneg();
				return new NPCFRIGTERM(obj.locDir, raster, -raster.offsetX, -raster.offsetY, -raster.offsetZ);
			}

			private static Raster createDummyRaster_ExitOnXneg() {
				Raster raster = new Raster();
				SaveGameData.Coordinates posAN = new SaveGameData.Coordinates(new Point3D(0,0,0));
				SaveGameData.Coordinates atAN  = new SaveGameData.Coordinates(new Point3D(0,0,1));
				SaveGameData.Coordinates upAN  = new SaveGameData.Coordinates(new Point3D(0,1,0));
				PlacingObj anchorDummy = new PlacingObj(BuildingObject.createDummy("^CUBEROOM_SPACE", posAN, upAN, atAN));
				
				anchorDummy.setPos(0,0,0);
				if (!anchorDummy.isObjPosAsExpected()) throw new IllegalStateException();
				
				SaveGameData.Coordinates posEX = new SaveGameData.Coordinates(new Point3D(-8,0,0));
				SaveGameData.Coordinates atEX  = new SaveGameData.Coordinates(new Point3D(0,0,1));
				SaveGameData.Coordinates upEX  = new SaveGameData.Coordinates(new Point3D(0,1,0));
				PlacingObj exitDummy = anchorDummy.createOthogonalObj(BuildingObject.createDummy("^CORRIDOR_SPACE", posEX, upEX, atEX));
				
				raster.setRange(exitDummy);
				raster.createEmptyRaster();
				raster.set(anchorDummy);
				raster.set(exitDummy);
				return raster;
			}

			@Override public LineGeometry.IndexedLineSet createGeometry() {
				LineGeometry.Transform desk = new LineGeometry.Transform(VRMLoutput.SoftwareBuildModels.create_BUILDSIMPLEDESK())
					.addRotation(LineGeometry.Axis.Z, 90)
					.addRotation(LineGeometry.Axis.Y,-90)
					.addTranslation(LineGeometry.Axis.Z, 2.09);
				
				LineGeometry.Transform chair = new LineGeometry.Transform(VRMLoutput.SoftwareBuildModels.create_BUILDCHAIR())
					.addRotation(LineGeometry.Axis.Z, 90)
					.addRotation(LineGeometry.Axis.Y,-90)
					.addTranslation(0.5,0,1.25);
				
				LineGeometry.Transform bed = new LineGeometry.Transform(VRMLoutput.SoftwareBuildModels.create_BUILDBED())
					.addRotation(LineGeometry.Axis.Z, 90)
					.addRotation(LineGeometry.Axis.Y,-180)
					.addTranslation(1.0,0,-1.9);
				
				double screenHeight1  = 1.4;
				double screenHeight2  = 2.5;
				double screenXBackPos = 2.9;
				double screenWidth    = (screenHeight2-screenHeight1)/232.0*405.0; // 405(1.1) : 232
				double screenAngle    = 45;
				double screenRadius   = screenWidth/2/Math.sin(screenAngle/2/180*Math.PI); // r.sin(a/2) = w/2;
				LineGeometry.PolyLine frame;
				LineGeometry.GroupingNode screen = new LineGeometry.GroupingNode()
						.add(frame=new LineGeometry.PolyLine()
								.addArc(LineGeometry.Axis.Y, new Point3D(screenXBackPos-screenRadius, screenHeight1, 0), screenRadius, 90+screenAngle/2, 90-screenAngle/2,  true, 6)
								.addArc(LineGeometry.Axis.Y, new Point3D(screenXBackPos-screenRadius, screenHeight2, 0), screenRadius, 90-screenAngle/2, 90+screenAngle/2, false, 6)
								.close())
						.add(new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(screenXBackPos-screenRadius, screenHeight1*2.0/3.0+screenHeight2/3, 0), screenRadius, 90+screenAngle/2, 90-screenAngle/2,  true, 6))
						.add(new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(screenXBackPos-screenRadius, screenHeight2*2.0/3.0+screenHeight1/3, 0), screenRadius, 90+screenAngle/2, 90-screenAngle/2,  true, 6))
						.add(new LineGeometry.PolyLine().add(frame.get(1)).add(frame.get(12)))
						.add(new LineGeometry.PolyLine().add(frame.get(2)).add(frame.get(11)))
						.add(new LineGeometry.PolyLine().add(frame.get(3)).add(frame.get(10)))
						.add(new LineGeometry.PolyLine().add(frame.get(4)).add(frame.get( 9)))
						.add(new LineGeometry.PolyLine().add(frame.get(5)).add(frame.get( 8)))
						;
				
				LineGeometry.Prism console = new LineGeometry.Prism(LineGeometry.Axis.Z, screenWidth*0.8,
						new Point3D(screenXBackPos    , screenHeight1     , 0),
						new Point3D(screenXBackPos-0.5, screenHeight1-0.50, 0),
						new Point3D(screenXBackPos-0.8, screenHeight1-0.50, 0),
						new Point3D(screenXBackPos-0.8, screenHeight1-0.55, 0),
						new Point3D(screenXBackPos-0.3, screenHeight1-1.00, 0),
						new Point3D(screenXBackPos    , screenHeight1-1.00, 0)
				);
				
				
				LineGeometry.GroupingNode objExitOnXneg = new LineGeometry.GroupingNode()
					.add(super.createGeometry())
					.add(new LineGeometry.PolyLine().add(-4, 0,-1.4).add(-4, 0,+1.4))
					.add(desk).add(chair).add(bed).add(screen).add(console)
					;
				
				switch (dirOfExit) {
				case Xneg: return objExitOnXneg;
				case Zpos: return new LineGeometry.Transform(objExitOnXneg).addRotation(LineGeometry.Axis.Y, 90);
				case Xpos: return new LineGeometry.Transform(objExitOnXneg).addRotation(LineGeometry.Axis.Y,180);
				case Zneg: return new LineGeometry.Transform(objExitOnXneg).addRotation(LineGeometry.Axis.Y,270);
				}
				return null;
			}
		}

		private static class CUBEROOM {
		
			private Raster raster;
			private int x;
			private int y;
			private int z;

			public CUBEROOM(Raster raster, int x, int y, int z) {
				this.raster = raster;
				this.x = x;
				this.y = y;
				this.z = z;
			}
			
			public LineGeometry.IndexedLineSet createGeometry() {
				PlacingObj neighbor_NW = raster.get(x+1, y  , z-1);
				PlacingObj neighbor_NE = raster.get(x+1, y  , z+1);
				PlacingObj neighbor_SW = raster.get(x-1, y  , z-1);
				PlacingObj neighbor_SE = raster.get(x-1, y  , z+1);
				PlacingObj neighbor_N  = raster.get(x+1, y  , z  );
				PlacingObj neighbor_S  = raster.get(x-1, y  , z  );
				PlacingObj neighbor_W  = raster.get(x  , y  , z-1);
				PlacingObj neighbor_E  = raster.get(x  , y  , z+1);
				PlacingObj neighbor_N0 = raster.get(x+1, y-1, z  );
				PlacingObj neighbor_S0 = raster.get(x-1, y-1, z  );
				PlacingObj neighbor_W0 = raster.get(x  , y-1, z-1);
				PlacingObj neighbor_E0 = raster.get(x  , y-1, z+1);
				
				return new LineGeometry.OptimizeNode(
					createCornerNE( neighbor_N, neighbor_NE, neighbor_E ),
					createCornerNE( neighbor_W, neighbor_NW, neighbor_N ).addRotation(LineGeometry.Axis.Y, 90),
					createCornerNE( neighbor_S, neighbor_SW, neighbor_W ).addRotation(LineGeometry.Axis.Y,180),
					createCornerNE( neighbor_E, neighbor_SE, neighbor_S ).addRotation(LineGeometry.Axis.Y,270),
					
					createWallN( neighbor_N, neighbor_N0, PlacingObj.LocalDirection.Xpos ),
					createWallN( neighbor_W, neighbor_W0, PlacingObj.LocalDirection.Zneg ).addRotation(LineGeometry.Axis.Y, 90),
					createWallN( neighbor_S, neighbor_S0, PlacingObj.LocalDirection.Xneg ).addRotation(LineGeometry.Axis.Y,180),
					createWallN( neighbor_E, neighbor_E0, PlacingObj.LocalDirection.Zpos ).addRotation(LineGeometry.Axis.Y,270),
					
					createCeilingLight(),
					createFloor()
				);
			}

			private static LineGeometry.IndexedLineSet createCeilingLight() {
				return new LineGeometry.GroupingNode(
					new LineGeometry.PolyLine()
						.addArc(LineGeometry.Axis.Y, new Point3D( 1.2,3.8, 1.2), 0.2,  0, 90, false)
						.addArc(LineGeometry.Axis.Y, new Point3D( 1.2,3.8,-1.2), 0.2, 90,180, false)
						.addArc(LineGeometry.Axis.Y, new Point3D(-1.2,3.8,-1.2), 0.2,180,270, false)
						.addArc(LineGeometry.Axis.Y, new Point3D(-1.2,3.8, 1.2), 0.2,270,360, false)
						.close(),
					new LineGeometry.PolyLine()
						.addArc(LineGeometry.Axis.Y, new Point3D( 0.9,3.8, 0.9), 0.2,  0, 90, false)
						.addArc(LineGeometry.Axis.Y, new Point3D( 0.9,3.8,-0.9), 0.2, 90,180, false)
						.addArc(LineGeometry.Axis.Y, new Point3D(-0.9,3.8,-0.9), 0.2,180,270, false)
						.addArc(LineGeometry.Axis.Y, new Point3D(-0.9,3.8, 0.9), 0.2,270,360, false)
						.close(),
					new LineGeometry.PolyLine()
						.addArc(LineGeometry.Axis.Y, new Point3D( 0.9,3.8, 0.9), 0.1,  0, 90, false)
						.addArc(LineGeometry.Axis.Y, new Point3D( 0.9,3.8,-0.9), 0.1, 90,180, false)
						.addArc(LineGeometry.Axis.Y, new Point3D(-0.9,3.8,-0.9), 0.1,180,270, false)
						.addArc(LineGeometry.Axis.Y, new Point3D(-0.9,3.8, 0.9), 0.1,270,360, false)
						.close()
				);
			}

			private static LineGeometry.IndexedLineSet createFloor() {
				return new LineGeometry.GroupingNode(
					new LineGeometry.PolyLine( new Point3D( 2,0, 2), new Point3D( 2,0,-2) ),
					new LineGeometry.PolyLine( new Point3D( 1,0, 2), new Point3D( 1,0,-2) ),
					new LineGeometry.PolyLine( new Point3D( 0,0, 2), new Point3D( 0,0,-2) ),
					new LineGeometry.PolyLine( new Point3D(-1,0, 2), new Point3D(-1,0,-2) ),
					new LineGeometry.PolyLine( new Point3D(-2,0, 2), new Point3D(-2,0,-2) ),
					new LineGeometry.PolyLine( new Point3D( 2,0, 2), new Point3D(-2,0, 2) ),
					new LineGeometry.PolyLine( new Point3D( 2,0, 1), new Point3D(-2,0, 1) ),
					new LineGeometry.PolyLine( new Point3D( 2,0, 0), new Point3D(-2,0, 0) ),
					new LineGeometry.PolyLine( new Point3D( 2,0,-1), new Point3D(-2,0,-1) ),
					new LineGeometry.PolyLine( new Point3D( 2,0,-2), new Point3D(-2,0,-2) )
				);
			}

			private static LineGeometry.Transform createWallN(PlacingObj neighbor_N, PlacingObj neighbor_N0, PlacingObj.LocalDirection locDir) {
				if (is(neighbor_N,ObjectID.CUBEROOM_SPACE))
					return new LineGeometry.Transform(createNoWallN()).addTranslation(new Point3D(3,0,0));
				else {
					if (needDoorToNeighbor(neighbor_N, neighbor_N0, locDir))
						return new LineGeometry.Transform(createWallDoorN()).addTranslation(new Point3D(3,0,0));
					else
						return new LineGeometry.Transform(createWallN()).addTranslation(new Point3D(3,0,0));
				}
			}

			private static LineGeometry.IndexedLineSet createNoWallN() {
				return new LineGeometry.GroupingNode(
						
					new LineGeometry.PolyLine( new Point3D( 1,0, 2), new Point3D( 1,0, 0) ),
					new LineGeometry.PolyLine( new Point3D( 0,0, 2), new Point3D( 0,0,-2) ),
					
					new LineGeometry.PolyLine( new Point3D( 1,0, 2), new Point3D(-1,0, 2) ),
					new LineGeometry.PolyLine( new Point3D( 1,0, 1), new Point3D(-1,0, 1) ),
					new LineGeometry.PolyLine( new Point3D( 1,0, 0), new Point3D(-1,0, 0) ),
					new LineGeometry.PolyLine( new Point3D( 1,0,-1), new Point3D(-1,0,-1) ),
					new LineGeometry.PolyLine( new Point3D( 1,0,-2), new Point3D(-1,0,-2) ),
					new LineGeometry.PolyLine( new Point3D(-1,3.8,-1.5), new Point3D(-1,3.8,1.5) ),
					new LineGeometry.PolyLine( new Point3D( 1,3.8, 0.0), new Point3D( 1,3.8,1.5) )
				);
			}

			private static LineGeometry.IndexedLineSet createWallN() {
				return new LineGeometry.GroupingNode(
					new LineGeometry.PolyLine()
						.add(new Point3D(-1.0,0.0,2))
						.addArc(LineGeometry.Axis.Z, new Point3D(-0.6,0.6,2), 0.6, 270, 360, false)
						.addArc(LineGeometry.Axis.Z, new Point3D(-0.6,2.7,2), 0.6,   0,  90, false)
						.add(new Point3D(-1.0,3.3,2)),
					new LineGeometry.PolyLine( new Point3D(-1.0,0  , 0  ), new Point3D(-0.6,0  ,0  ) ),
					new LineGeometry.PolyLine( new Point3D(-0.6,0  ,-2  ), new Point3D(-0.6,0  ,2  ) ),
					new LineGeometry.PolyLine( new Point3D( 0.0,0.6,-2  ), new Point3D( 0.0,0.6,2  ) ),
					new LineGeometry.PolyLine( new Point3D( 0.0,2.7,-2  ), new Point3D( 0.0,2.7,2  ) ),
					new LineGeometry.PolyLine( new Point3D(-0.6,3.3,-2  ), new Point3D(-0.6,3.3,2  ) ),
					new LineGeometry.PolyLine( new Point3D(-1.0,3.3,-2  ), new Point3D(-1.0,3.3,2  ) ),
					new LineGeometry.PolyLine( new Point3D(-1.5,3.8,-1.5), new Point3D(-1.5,3.8,1.5) )
				);
			}

			private static LineGeometry.IndexedLineSet createWallDoorN() {
				LineGeometry.PolyLine profilTuerZarge = new LineGeometry.PolyLine()
					.addArc(LineGeometry.Axis.Y, new Point3D( 0.3,0,0), 0.2,  90, 180, false)
					.addArc(LineGeometry.Axis.Y, new Point3D(-0.3,0,0), 0.2, 180, 270, false);

				return new LineGeometry.GroupingNode(
					new LineGeometry.PolyLine()
						.add(new Point3D(-1.0,0.0,2))
						.addArc(LineGeometry.Axis.Z, new Point3D(-0.6,0.6,2), 0.6, 270, 360, false)
						.addArc(LineGeometry.Axis.Z, new Point3D(-0.6,2.7,2), 0.6,   0,  90, false)
						.add(new Point3D(-1.0,3.3,2)),
					
					new LineGeometry.PolyLine()
						.addArc(LineGeometry.Axis.X, new Point3D(0,0.6, 1.4), 0.6,  90,180, false)
						.addArc(LineGeometry.Axis.X, new Point3D(0,0.6,-1.4), 0.6, 180,270, false),
					
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,0,-2), 0.6, 270,360, false),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,0,-2), 1.0, 270,360, false),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,0, 2), 1.0, 180,270, false),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,0, 2), 0.6, 180,270, false),
					
					new LineGeometry.PolyLine( new Point3D( 1,0, 1), new Point3D(-1,0, 1) ),
					new LineGeometry.PolyLine( new Point3D( 1,0, 0), new Point3D(-1,0, 0) ),
					new LineGeometry.PolyLine( new Point3D( 1,0,-1), new Point3D(-1,0,-1) ),
					
					new LineGeometry.PolyLine( new Point3D( 0.0,2.7,-2), new Point3D( 0.0,2.7,2) ),
					new LineGeometry.PolyLine( new Point3D(-0.6,3.3,-2), new Point3D(-0.6,3.3,2) ),
					new LineGeometry.PolyLine( new Point3D(-1.0,3.3,-2), new Point3D(-1.0,3.3,2) ),
					new LineGeometry.PolyLine( new Point3D(-1.5,3.8,-1.5), new Point3D(-1.5,3.8,1.5) ),
					
					new LineGeometry.Transform(profilTuerZarge)                                      .addTranslation(new Point3D(0.5,0  , 1.4)),
					new LineGeometry.Transform(profilTuerZarge)                                      .addTranslation(new Point3D(0.5,2.3, 1.4)),
					new LineGeometry.Transform(profilTuerZarge).addRotation(LineGeometry.Axis.X, -90).addTranslation(new Point3D(0.5,2.7, 1.0)),
					new LineGeometry.Transform(profilTuerZarge).addRotation(LineGeometry.Axis.X, -90).addTranslation(new Point3D(0.5,2.7,-1.0)),
					new LineGeometry.Transform(profilTuerZarge).addRotation(LineGeometry.Axis.X,-180).addTranslation(new Point3D(0.5,2.3,-1.4)),
					new LineGeometry.Transform(profilTuerZarge).addRotation(LineGeometry.Axis.X,-180).addTranslation(new Point3D(0.5,0  ,-1.4)),
					new LineGeometry.PolyLine()
						.add(new Point3D(0.0,0,-1.4))
						.addArc(LineGeometry.Axis.X, new Point3D(0.0,2.3,-1.0), 0.4, 270, 360, false),
					new LineGeometry.PolyLine()
						.addArc(LineGeometry.Axis.X, new Point3D(0.0,2.3, 1.0), 0.4,   0,  90, false)
						.add(new Point3D(0.0,0, 1.4)),
					new LineGeometry.PolyLine()
						.add(new Point3D(0.2,0,-1.2))
						.addArc(LineGeometry.Axis.X, new Point3D(0.2,2.3,-1.0), 0.2, 270, 360, false)
						.addArc(LineGeometry.Axis.X, new Point3D(0.2,2.3, 1.0), 0.2,   0,  90, false)
						.add(new Point3D(0.2,0, 1.2)),
					new LineGeometry.PolyLine()
						.add(new Point3D(0.8,0,-1.2))
						.addArc(LineGeometry.Axis.X, new Point3D(0.8,2.3,-1.0), 0.2, 270, 360, false)
						.addArc(LineGeometry.Axis.X, new Point3D(0.8,2.3, 1.0), 0.2,   0,  90, false)
						.add(new Point3D(0.8,0, 1.2)),
					new LineGeometry.PolyLine()
						.add(new Point3D(1.0,0,-1.4))
						.addArc(LineGeometry.Axis.X, new Point3D(1.0,2.3,-1.0), 0.4, 270, 360, false)
						.addArc(LineGeometry.Axis.X, new Point3D(1.0,2.3, 1.0), 0.4,   0,  90, false)
						.add(new Point3D(1.0,0, 1.4))
				);
			}

			private static LineGeometry.Transform createCornerNE(PlacingObj neighbor_N, PlacingObj neighbor_NE, PlacingObj neighbor_E) {
				if (is(neighbor_N,ObjectID.CUBEROOM_SPACE)) {
					if (is(neighbor_E,ObjectID.CUBEROOM_SPACE)) {
						if (is(neighbor_NE,ObjectID.CUBEROOM_SPACE)) return new LineGeometry.Transform(createEmptyCorner()).addTranslation(new Point3D(3,0,3));
						else return new LineGeometry.Transform(createOutsideCornerNE()).addTranslation(new Point3D(3,0,3));
					} else
						return new LineGeometry.Transform(createCornerNEWallNS()).addTranslation(new Point3D(3,0,3));
				} else {
					if (is(neighbor_E,ObjectID.CUBEROOM_SPACE))
						return new LineGeometry.Transform(createCornerNEWallWE()).addTranslation(new Point3D(3,0,3));
					else
						return new LineGeometry.Transform(createInsideCornerNE()).addTranslation(new Point3D(3,0,3));
				}
			}

			private static LineGeometry.IndexedLineSet createEmptyCorner() {
				return new LineGeometry.GroupingNode(
					new LineGeometry.PolyLine( new Point3D( 1, 0 , 1), new Point3D( 1, 0 ,-1) ),
					new LineGeometry.PolyLine( new Point3D( 0, 0 , 1), new Point3D( 0, 0 ,-1) ),
					new LineGeometry.PolyLine( new Point3D( 1, 0 , 1), new Point3D(-1, 0 , 1) ),
					new LineGeometry.PolyLine( new Point3D( 1, 0 , 0), new Point3D(-1, 0 , 0) ),
					new LineGeometry.PolyLine( new Point3D( 1,3.8, 1), new Point3D(-1.5,3.8, 1.0) ),
					new LineGeometry.PolyLine( new Point3D(-1,3.8, 1), new Point3D(-1.0,3.8,-1.5) ),
					new LineGeometry.PolyLine( new Point3D( 1,3.8,-1), new Point3D(-1.5,3.8,-1.0) )
				);
			}

			private static LineGeometry.IndexedLineSet createInsideCornerNE() {
				return new LineGeometry.GroupingNode(
					new LineGeometry.PolyLine()
						.add(new Point3D(-1.5,3.8,-1.5))
						.add(new Point3D(-1.0,3.3,-1.0))
						.addArc(LineGeometry.Axis.X, new Point3D(-1,2.7,-0.6), 0.6,  0, 90, false)
						.addArc(LineGeometry.Axis.X, new Point3D(-1,0.6,-0.6), 0.6, 90,180, false)
						.add(new Point3D(-1.0,0.0,-1.0)),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(-1,0.0,-1), 0.4, 0, 90, false),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(-1,0.6,-1), 1.0, 0, 90, false),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(-1,2.7,-1), 1.0, 0, 90, false),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(-1,3.3,-1), 0.4, 0, 90, false)
				);
			}

			private static LineGeometry.IndexedLineSet createOutsideCornerNE() {
				return new LineGeometry.GroupingNode(
					new LineGeometry.PolyLine()
						.add(new Point3D(-1.0,0.0,1))
						.addArc(LineGeometry.Axis.Z, new Point3D(-0.6,0.6,1), 0.6, 270, 360, false)
						.addArc(LineGeometry.Axis.Z, new Point3D(-0.6,2.7,1), 0.6,   0,  90, false)
						.add(new Point3D(-1.0,3.3,1))
						.add(new Point3D(-1.5,3.8,1)),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(1,0.0,1), 2.0, 180,270, false),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(1,0.0,1), 1.6, 180,270, false),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(1,0.6,1), 1.0, 180,270, false),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(1,2.7,1), 1.0, 180,270, false),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(1,3.3,1), 1.6, 180,270, false),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(1,3.3,1), 2.0, 180,270, false),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(1,3.8,1), 2.5, 180,270, false),
					new LineGeometry.PolyLine( new Point3D(-1,0,0), new Point3D(0,0,-1) ),
					new LineGeometry.PolyLine( new Point3D(-1.5,3.8,1), new Point3D(-1.5,3.8,-1), new Point3D(-1,3.8,-1.5), new Point3D(1,3.8,-1.5) )
				);
			}

			private static LineGeometry.IndexedLineSet createCornerNEWallNS() {
				return new LineGeometry.GroupingNode(
					new LineGeometry.PolyLine()
						.add(new Point3D(-1,3.8,-1.5))
						.add(new Point3D(-1,3.3,-1.0))
						.addArc(LineGeometry.Axis.X, new Point3D(-1,2.7,-0.6), 0.6,  0, 90, false)
						.addArc(LineGeometry.Axis.X, new Point3D(-1,0.6,-0.6), 0.6, 90,180, false)
						.add(new Point3D(-1,0.0,-1.0)),
					new LineGeometry.PolyLine( new Point3D(-1.0,0  ,-0.6), new Point3D(1,0  ,-0.6) ),
					new LineGeometry.PolyLine( new Point3D(-1.0,0.6, 0.0), new Point3D(1,0.6, 0.0) ),
					new LineGeometry.PolyLine( new Point3D(-1.0,2.7, 0.0), new Point3D(1,2.7, 0.0) ),
					new LineGeometry.PolyLine( new Point3D(-1.0,3.3,-0.6), new Point3D(1,3.3,-0.6) ),
					new LineGeometry.PolyLine( new Point3D(-1.0,3.3,-1.0), new Point3D(1,3.3,-1.0) ),
					new LineGeometry.PolyLine( new Point3D(-1.5,3.8,-1.5), new Point3D(1,3.8,-1.5) )
				);
			}

			private static LineGeometry.IndexedLineSet createCornerNEWallWE() {
				return new LineGeometry.GroupingNode(
					new LineGeometry.PolyLine()
						.add(new Point3D(-1.0,0.0,1))
						.addArc(LineGeometry.Axis.Z, new Point3D(-0.6,0.6,1), 0.6, 270, 360, false)
						.addArc(LineGeometry.Axis.Z, new Point3D(-0.6,2.7,1), 0.6,   0,  90, false)
						.add(new Point3D(-1.0,3.3,1))
						.add(new Point3D(-1.5,3.8,1)),
					new LineGeometry.PolyLine( new Point3D(-0.6,0  ,-1.0), new Point3D(-0.6,0  ,1) ),
					new LineGeometry.PolyLine( new Point3D( 0.0,0.6,-1.0), new Point3D( 0.0,0.6,1) ),
					new LineGeometry.PolyLine( new Point3D( 0.0,2.7,-1.0), new Point3D( 0.0,2.7,1) ),
					new LineGeometry.PolyLine( new Point3D(-0.6,3.3,-1.0), new Point3D(-0.6,3.3,1) ),
					new LineGeometry.PolyLine( new Point3D(-1.0,3.3,-1.0), new Point3D(-1.0,3.3,1) ),
					new LineGeometry.PolyLine( new Point3D(-1.5,3.8,-1.5), new Point3D(-1.5,3.8,1) ),
					new LineGeometry.PolyLine( new Point3D(-1.5,3.8,-1.0), new Point3D(-1.0,3.3,-1) )
				);
			}
		
		}

		private static class S_CONTAINER {
		
			public S_CONTAINER() {}
		
			public LineGeometry.IndexedLineSet createGeometry() {
				LineGeometry.IndexedLineSet tempObj;
				return new LineGeometry.OptimizeNode(
					new LineGeometry.Transform(tempObj = createInsideCornerNE()).addTranslation(new Point3D(4,0,4)),
					new LineGeometry.Transform(tempObj).addRotation(LineGeometry.Axis.Y, 90).addTranslation(new Point3D( 4,0,-4)),
					new LineGeometry.Transform(tempObj).addRotation(LineGeometry.Axis.Y,180).addTranslation(new Point3D(-4,0,-4)),
					new LineGeometry.Transform(tempObj).addRotation(LineGeometry.Axis.Y,270).addTranslation(new Point3D(-4,0, 4)),
					
					new LineGeometry.Transform(tempObj = createWallDoorN()).addTranslation(new Point3D(4,0,0)),
					new LineGeometry.Transform(tempObj).addRotation(LineGeometry.Axis.Y, 90).addTranslation(new Point3D( 0,0,-4)),
					new LineGeometry.Transform(tempObj).addRotation(LineGeometry.Axis.Y,180).addTranslation(new Point3D(-4,0, 0)),
					new LineGeometry.Transform(tempObj).addRotation(LineGeometry.Axis.Y,270).addTranslation(new Point3D( 0,0, 4)),
					
					CUBEROOM.createCeilingLight(),
					createFloor(),
					createMonitoringDesk()
				);
			}

			private static LineGeometry.IndexedLineSet createInsideCornerNE() {
				return new LineGeometry.GroupingNode(
					new LineGeometry.PolyLine( new Point3D(-1.0,3.3,-1.0), new Point3D(-1.5,3.8,-1.5) ),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(-1,0.0,-1), 0.4, 0, 90, false),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(-1,0.5,-1), 0.9, 0, 90, false),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(-1,2.8,-1), 0.9, 0, 90, false),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(-1,3.3,-1), 0.4, 0, 90, false)
				);
			}

			private static LineGeometry.IndexedLineSet createWallDoorN() {
				LineGeometry.PolyLine vertWallLine = new LineGeometry.PolyLine()
					.add(new Point3D(-1.0,0.0,0))
					.addArc(LineGeometry.Axis.Z, new Point3D(-0.6,0.5,0), 0.5, 270, 360, false)
					.addArc(LineGeometry.Axis.Z, new Point3D(-0.6,2.8,0), 0.5,   0,  90, false)
					.add(new Point3D(-1.0,3.3,0));
				
				return new LineGeometry.GroupingNode(
					new LineGeometry.Transform(vertWallLine).addTranslation(new Point3D(0,0,-3)),
					new LineGeometry.Transform(vertWallLine).addTranslation(new Point3D(0,0,-2)),
					new LineGeometry.Transform(vertWallLine).addTranslation(new Point3D(0,0, 2)),
					new LineGeometry.Transform(vertWallLine).addTranslation(new Point3D(0,0, 3)),
					new LineGeometry.PolyLine( new Point3D(-1.0,3.3,-2), new Point3D(-1.5,3.8,-2) ),
					new LineGeometry.PolyLine( new Point3D(-1.0,3.3, 2), new Point3D(-1.5,3.8, 2) ),
					
					new LineGeometry.PolyLine()
						.addArc(LineGeometry.Axis.X, new Point3D(0,2.2, 1.4), 0.5,   0, 90, false)
						.addArc(LineGeometry.Axis.X, new Point3D(0,0.5, 1.4), 0.5,  90,180, false)
						.addArc(LineGeometry.Axis.X, new Point3D(0,0.5,-1.4), 0.5, 180,270, false)
						.addArc(LineGeometry.Axis.X, new Point3D(0,2.2,-1.4), 0.5, 270,360, false)
						.close(),
						
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(-0.1,2.2, 1.4), 0.6,   0, 90, false),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(-0.1,2.2,-1.4), 0.6, 270,360, false),
						
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Z, new Point3D(0,2.8, 1.4), 0.1, 180,270, false),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Z, new Point3D(0,2.8,-1.4), 0.1, 180,270, false),
					
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,2.2,-2), 0.1, 360,270, true ),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,0.5,-2), 0.1, 360,270, true ).add(new Point3D(-0.1,0.5,-3)),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,0.0,-2), 0.6, 360,270, true ).add(new Point3D(-0.6,0.0,-3)),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,0.0,-2), 1.0, 360,270, true ),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,0.0, 2), 1.0, 180,270, false),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,0.0, 2), 0.6, 180,270, false).add(new Point3D(-0.6,0.0,3)),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,0.5, 2), 0.1, 180,270, false).add(new Point3D(-0.1,0.5,3)),
					new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,2.2, 2), 0.1, 180,270, false),
					
					new LineGeometry.PolyLine( new Point3D( 0,0, 1), new Point3D(-2,0, 1) ),
					new LineGeometry.PolyLine( new Point3D( 0,0, 0), new Point3D(-2,0, 0) ),
					new LineGeometry.PolyLine( new Point3D( 0,0,-1), new Point3D(-2,0,-1) ),
					
					new LineGeometry.PolyLine( new Point3D(-0.1,2.8,-3), new Point3D(-0.1,2.8,3) ),
					new LineGeometry.PolyLine( new Point3D(-0.6,3.3,-3), new Point3D(-0.6,3.3,3) ),
					new LineGeometry.PolyLine( new Point3D(-1.0,3.3,-3), new Point3D(-1.0,3.3,3) ),
					new LineGeometry.PolyLine( new Point3D(-1.5,3.8,-2.5), new Point3D(-1.5,3.8,2.5) )
				);
			}
		
			
			private static LineGeometry.IndexedLineSet createMonitoringDesk() {
				
				LineGeometry.PolyLine line1 = new LineGeometry.PolyLine();
				LineGeometry.PolyLine line2 = new LineGeometry.PolyLine();
				LineGeometry.PolyLine line3 = new LineGeometry.PolyLine();
				LineGeometry.PolyLine line4 = new LineGeometry.PolyLine();
				LineGeometry.loopArc(2.1, 0, 360, false, 16, (i, nSeg, x, y) -> {
					if ((i&1)==1) {
						line1.add(new Point3D(y     ,0.0,x     ));
						line2.add(new Point3D(y*0.9 ,0.0,x*0.9 ));
						line3.add(new Point3D(y*0.5 ,0.0,x*0.5 ));
						line4.add(new Point3D(y*0.45,0.0,x*0.45));
					}
				});
				line1.close(); line2.close(); line3.close(); line4.close();
				
				double h_under = 0.7;
				double h_edge  = 1.0;
				double h_top = 1.3;
				double r_top = 0.9;
				
				double w_edge = 1.4; // half of width :)
				double wc_edge = 0.8; // edge corner
				double w_under = 0.6; // half of width :)
				
				double x = 0.4;
				double y = Math.sqrt(r_top*r_top-x*x);
				
				LineGeometry.PolyLine edge = new LineGeometry.PolyLine()
					.addArc(LineGeometry.Axis.X, new Point3D(0,0.1, 0.5), 0.1, 180,270, false)
					.add(new Point3D(0,h_under,0.35));
				
				return new LineGeometry.GroupingNode(
					line1,line2,line3,line4,
					
					new LineGeometry.Circle(LineGeometry.Axis.Y, new Point3D(0,0.0,0), 0.5),
					new LineGeometry.Circle(LineGeometry.Axis.Y, new Point3D(0,0.1,0), 0.4),
					new LineGeometry.Circle(LineGeometry.Axis.Y, new Point3D(0,h_under,0), 0.35),
					
					edge,
					new LineGeometry.Transform(edge).addRotation(LineGeometry.Axis.Y, 90),
					new LineGeometry.Transform(edge).addRotation(LineGeometry.Axis.Y,180),
					new LineGeometry.Transform(edge).addRotation(LineGeometry.Axis.Y,270),
					
					new LineGeometry.PolyLine(
						new Point3D(-w_under,h_under,-w_under),
						new Point3D( w_under,h_under,-w_under),
						new Point3D( w_under,h_under, w_under),
						new Point3D(-w_under,h_under, w_under)
					).close(),
					
					new LineGeometry.PolyLine(
						new Point3D(- w_edge,h_edge,-wc_edge),
						new Point3D(-wc_edge,h_edge,- w_edge),
						new Point3D( wc_edge,h_edge,- w_edge),
						new Point3D(  w_edge,h_edge,-wc_edge),
						new Point3D(  w_edge,h_edge, wc_edge),
						new Point3D( wc_edge,h_edge,  w_edge),
						new Point3D(-wc_edge,h_edge,  w_edge),
						new Point3D(- w_edge,h_edge, wc_edge)
					).close(),
					
					new LineGeometry.PolyLine(
						new Point3D(-y,h_top,-x),
						new Point3D(-w_edge ,h_edge ,-wc_edge),
						new Point3D(-w_under,h_under,-w_under),
						new Point3D(-wc_edge,h_edge ,-w_edge ),
						new Point3D(-x,h_top,-y)
					),
			
					new LineGeometry.PolyLine(
						new Point3D( x,h_top,-y),
						new Point3D( wc_edge,h_edge ,-w_edge ),
						new Point3D( w_under,h_under,-w_under),
						new Point3D( w_edge ,h_edge ,-wc_edge),
						new Point3D( y,h_top,-x)
					),
			
					new LineGeometry.PolyLine(
						new Point3D( y,h_top,x),
						new Point3D( w_edge ,h_edge , wc_edge),
						new Point3D( w_under,h_under, w_under),
						new Point3D( wc_edge,h_edge , w_edge ),
						new Point3D( x,h_top,y)
					),
			
					new LineGeometry.PolyLine(
						new Point3D(-x,h_top,y),
						new Point3D(-wc_edge,h_edge , w_edge ),
						new Point3D(-w_under,h_under, w_under),
						new Point3D(-w_edge ,h_edge , wc_edge),
						new Point3D(-y,h_top,x)
					),
					
					new LineGeometry.Circle(LineGeometry.Axis.Y, new Point3D(0,h_top,0), r_top)
				);
			}
		
			private static LineGeometry.IndexedLineSet createFloor() {
				return new LineGeometry.GroupingNode(
					new LineGeometry.PolyLine( new Point3D( 3,0, 3), new Point3D( 3,0,-3) ),
					new LineGeometry.PolyLine( new Point3D( 2,0, 3), new Point3D( 2,0,-3) ),
					new LineGeometry.PolyLine( new Point3D(-2,0, 3), new Point3D(-2,0,-3) ),
					new LineGeometry.PolyLine( new Point3D(-3,0, 3), new Point3D(-3,0,-3) ),
					new LineGeometry.PolyLine( new Point3D( 3,0, 3), new Point3D(-3,0, 3) ),
					new LineGeometry.PolyLine( new Point3D( 3,0, 2), new Point3D(-3,0, 2) ),
					new LineGeometry.PolyLine( new Point3D( 3,0,-2), new Point3D(-3,0,-2) ),
					new LineGeometry.PolyLine( new Point3D( 3,0,-3), new Point3D(-3,0,-3) )
				);
			}
		}
	}

	@SuppressWarnings("unused")
	private static class CubeCombine_Dummy extends CubeCombine {
		CubeCombine_Dummy() { Gui.log_ln("\r\n\r\n##############################\r\nCubeCombine_Dummy\r\n##############################\r\n\r\n\r\n"); }
		@Override public boolean add(BuildingObject obj) { return false; }
		@Override public BuildingObject[] getRemainingObjects() { return new BuildingObject[0]; }
		@Override public void writeModel(PrintWriter vrml) {}
	}

	private static class CubeCombine {
		
		private static final double CUBESIZE = 4.0;
		Stack<BuildingObject> freeObj;
		Stack<BuildingObject> remainingObj;
		private Point3D baseAt;
		private Point3D baseUp;
		
		public CubeCombine() {
			freeObj = new Stack<>();
			remainingObj = new Stack<>();
			this.baseAt = null;
			this.baseUp = null;
			
			/*
			Vector<Line> lines = new Vector<>();
			lines.add(new Line(new Index3D(4,5,6),new Index3D(1,2,3)));
			boolean wasFound = lines.remove(new Line(new Index3D(4,5,6),new Index3D(1,2,3)));
			System.out.println("#########\r\nVector.remove "+(wasFound?"":"DOESN'T ")+"found specified line.\r\n#########");
			
			HashMap<Index3D, Integer> blocks = new HashMap<>();
			Integer prev;
			prev = blocks.put(new Index3D(0,1,2), 3); System.out.println("blocks.put(new Index3D(0,1,2), 3) -> "+prev);
			prev = blocks.put(new Index3D(0,1,2), 4); System.out.println("blocks.put(new Index3D(0,1,2), 4) -> "+prev);
			prev = blocks.get(new Index3D(0,1,2));    System.out.println("blocks.get(new Index3D(0,1,2))    -> "+prev);
			
			System.out.println("new Index3D(0,1,2) == new Index3D(0,1,2)       -> "+( new Index3D(0,1,2) == new Index3D(0,1,2) ));
			System.out.println("new Index3D(0,1,2).equals(new Index3D(0,1,2))  -> "+( new Index3D(0,1,2).equals(new Index3D(0,1,2)) ));
			
			HashMap<Line, Integer> blocks = new HashMap<>();
			Integer prev;
			prev = blocks.put(new Line(new Index3D(4,5,6),new Index3D(1,2,3)), 3); System.out.println("blocks.put(new Line(new Index3D(4,5,6),new Index3D(1,2,3)), 3) -> "+prev);
			prev = blocks.put(new Line(new Index3D(4,5,6),new Index3D(1,2,3)), 4); System.out.println("blocks.put(new Line(new Index3D(4,5,6),new Index3D(1,2,3)), 4) -> "+prev);
			prev = blocks.put(new Line(new Index3D(1,2,3),new Index3D(4,5,6)), 5); System.out.println("blocks.put(new Line(new Index3D(1,2,3),new Index3D(4,5,6)), 5) -> "+prev);
			prev = blocks.get(new Line(new Index3D(4,5,6),new Index3D(1,2,3)));    System.out.println("blocks.get(new Line(new Index3D(4,5,6),new Index3D(1,2,3)))    -> "+prev);
			*/
		}
		
		public void setBaseOrientation(Point3D at, Point3D up) {
			this.baseAt = at;
			this.baseUp = up;
		}

		public boolean add(BuildingObject obj) {
			if (obj==null) return false;
			switch (obj.objectID) {
			case "^CUBEROOM": case "^CUBEGLASS": case "^CUBEROOMCURVED": case "^CURVEDCUBEROOF":
				obj = fixUpsideDown(obj);
				freeObj.add(obj);
				return true;
			}
			return false;
		}

		public boolean isEmpty() {
			return freeObj.isEmpty();
		}

		private BuildingObject fixUpsideDown(BuildingObject obj) {
			if (baseAt==null || baseUp==null) return obj;
			if (obj==null) return obj;
			if (obj.position==null) return obj;
			if (obj.position.pos==null) return obj;
			
			Point3D at = Point3D.normalizeOrNull(obj.position.at);
			Point3D up = Point3D.normalizeOrNull(obj.position.up);
			if (at==null || up==null) return obj;
			
			double val = baseUp.scalarProd(up);
			if (Math.abs(val+1)>Neighborhood.ANGLE_TOLERANCE) return obj;
			
			BuildingObject newObj = new BuildingObject(obj);
			newObj.position.pos.set(newObj.position.pos.add(up.mul(CUBESIZE)));
			newObj.position.up .set(up.mul(-1));
			
			if (newObj.objectID.equals("^CUBEROOMCURVED"))
				newObj.position.at.set(up.mul(-1).crossProd(at));
			
			return newObj;
		}

		private static boolean isCube(Neighbor nb) {
			if (nb==null) return false;
			if (nb.type==Neighbor.Type.Cube) return true;
			if (nb.type==Neighbor.Type.GlassCube) return true;
			return false;
		}

		private static boolean isCurvedCube(Neighbor nb, Orientation... allowedOrientations) {
			if (nb==null) return false;
			if (nb.type!=Neighbor.Type.CurvedCube) return false;
			for (Orientation o:allowedOrientations)
				if (nb.o==o) return true;
			return false;
		}

		private static boolean isCurvedRoof(Neighbor nb, Orientation... allowedOrientations) {
			if (nb==null) return false;
			if (nb.type!=Neighbor.Type.CurvedRoof) return false;
			for (Orientation o:allowedOrientations)
				if (nb.o==o) return true;
			return false;
		}

		private static Neighbor get(Neighbor[][][] mat, Index3D i) {
			return get(mat, i.iX, i.iY, i.iZ);
		}

		private static Neighbor get(Neighbor[][][] mat, int x, int y, int z) {
			if (x<0) return null;
			if (y<0) return null;
			if (z<0) return null;
			if (x>=mat.length) return null;
			if (y>=mat[x].length) return null;
			if (z>=mat[x][y].length) return null;
			return mat[x][y][z];
		}

		public BuildingObject[] getRemainingObjects() {
			remainingObj.addAll(freeObj);
			freeObj.clear();
			return remainingObj.toArray(new BuildingObject[0]);
		}

		public void writeModel(PrintWriter vrml) {
			int i=0;
			while (!freeObj.isEmpty()) {
				BuildingObject obj = freeObj.pop();
				Neighborhood neighborhood = new Neighborhood(obj,++i);
				neighborhood.addNeighbors(freeObj);
				//if (neighborhood.hasOnlyStartObj())
				//	remainingObj.push(obj);
				//else
					neighborhood.writeModel(vrml);
			}
		}
		
		private static class Neighbor {
			enum Type { Cube, GlassCube, CurvedCube, CurvedRoof }
			final Index3D i;
			final Orientation o;
			final BuildingObject obj;
			final Type type;
		
			public Neighbor(Index3D i, Orientation o, BuildingObject obj) {
				this.i = i;
				this.o = o;
				this.obj = obj;
				if (obj==null) type=null;
				else
					switch(obj.objectID) {
					case "^CUBEROOM"      : type=Type.Cube; break;
					case "^CUBEGLASS"     : type=Type.GlassCube; break;
					case "^CUBEROOMCURVED": type=Type.CurvedCube; break;
					case "^CURVEDCUBEROOF": type=Type.CurvedRoof; break;
					default: type=null;
					}
			}

			@Override
			public String toString() {
				return "Neighbor("+i+","+o+","+type+","+obj+")";
			}
		}

		private static class Neighborhood {
			private static final double ANGLE_TOLERANCE = 0.0001;
			private static final double POS_TOLERANCE = 0.01;
			
			private HashMap<Index3D, Neighbor> blocks;
			private Point3D anchorPos;
			private Point3D anchorXup;
			private Point3D anchorYat;
			private Point3D anchorZ;
			private int neighborhoodIndex;
			
			public Neighborhood(BuildingObject firstObj, int neighborhoodIndex) {
				this.neighborhoodIndex = neighborhoodIndex;
				anchorPos = null;
				anchorXup = null;
				anchorYat = null;
				anchorZ   = null;
				if (firstObj.position!=null) {
					anchorPos = firstObj.position.pos;
					anchorXup = Point3D.normalizeOrNull(firstObj.position.up);
					anchorYat = Point3D.normalizeOrNull(firstObj.position.at);
					if (anchorXup!=null && anchorYat!=null) anchorZ = anchorXup.crossProd(anchorYat).normalize();
				}
				Index3D origin = new Index3D(0,0,0);
				blocks = new HashMap<>();
				blocks.put(origin, new Neighbor(origin, Orientation.PosY, firstObj));
			}
			
			private Neighbor getNeighborRelation(Position position, BuildingObject obj) {
				if (position==null) return null;
				if (position.pos==null) return null;
				if (position.at==null || position.at.isZero()) return null;
				if (position.up==null || position.up.isZero()) return null;
				
				double valUp = anchorXup.scalarProd(position.up.normalize());
				if (Math.abs(valUp-1.0)>ANGLE_TOLERANCE) return null;
				
				Orientation orientation = getOrientation(position);
				if (orientation==null) return null;
				
				Point3D indexVec = position.pos.add(anchorPos.mul(-1));
				double iX_d=anchorXup.scalarProd(indexVec)/CUBESIZE; int iX=(int)Math.round(iX_d); double deltaX=Math.abs(iX_d-iX);
				double iY_d=anchorYat.scalarProd(indexVec)/CUBESIZE; int iY=(int)Math.round(iY_d); double deltaY=Math.abs(iY_d-iY);
				double iZ_d=anchorZ  .scalarProd(indexVec)/CUBESIZE; int iZ=(int)Math.round(iZ_d); double deltaZ=Math.abs(iZ_d-iZ);
				
				//System.out.printf(Locale.ENGLISH,"Neighborhood %d:   At:%1.8f   Up:%11.8f   iX:%1.8f   iY:%1.8f   iZ:%1.8f\r\n", neighborhoodIndex, valAt, valUp, deltaX, deltaY, deltaZ);
				
				if (deltaX>POS_TOLERANCE) return null;
				if (deltaY>POS_TOLERANCE) return null;
				if (deltaZ>POS_TOLERANCE) return null;
				
				return new Neighbor(new Index3D(iX,iY,iZ), orientation, obj);
			}

			private Orientation getOrientation(Position position) {
				Point3D at = position.at.normalize();
				double valAt = anchorYat.scalarProd(at);
				if (Math.abs(valAt-1)<ANGLE_TOLERANCE) return Orientation.PosY;
				if (Math.abs(valAt+1)<ANGLE_TOLERANCE) return Orientation.NegY;
				
				if (Math.abs(valAt)>ANGLE_TOLERANCE) return null;
				
				if (anchorXup.scalarProd(anchorYat.crossProd(at))>0) return Orientation.PosZ;
				return Orientation.NegZ;
			}

			public void addNeighbors(Stack<BuildingObject> freeObj) {
				if (anchorPos==null) return;
				if (anchorXup==null) return;
				if (anchorYat==null) return;
				if (anchorZ  ==null) return;
				
				for (BuildingObject obj:new Vector<>(freeObj)) {
					Neighbor neighbor = getNeighborRelation(obj.position,obj);
					if (neighbor==null) continue;
					blocks.put(neighbor.i,neighbor);
					freeObj.remove(obj);
				}
			}
			
			public Neighbor[][][] toMatrix(Index3D minCorner, Index3D size) {
				Index3D min=null;
				Index3D max=null;
				for (Index3D index:blocks.keySet()) {
					if (min==null) min=new Index3D(index); else min.min(index);
					if (max==null) max=new Index3D(index); else max.max(index);
				}
				
				minCorner.set(min);
				size.iX = max.iX-min.iX+1;
				size.iY = max.iY-min.iY+1;
				size.iZ = max.iZ-min.iZ+1;
				
				Neighbor[][][] mat = new Neighbor[size.iX][size.iY][size.iZ];
				for (int x=0; x<size.iX; ++x)
					for (int y=0; y<size.iY; ++y)
						Arrays.fill(mat[x][y],null);
				
				for (Index3D index:blocks.keySet())
					mat[index.iX-min.iX][index.iY-min.iY][index.iZ-min.iZ] = blocks.get(index);
				
				return mat;
			}

			@SuppressWarnings("unused")
			public void writeModel_simple(PrintWriter vrml) {
				Point3D color = new Point3D(1,0,0);
				int i=0;
				for (Neighbor n:blocks.values())
					VRMLoutput.writeModel(vrml, n.obj.objectID, String.format("N%d Obj%d", neighborhoodIndex, ++i), null, n.obj.position.pos, n.obj.position.at, n.obj.position.up, color);
			}

			public void writeModel(PrintWriter vrml) {
				Index3D minCorner = new Index3D(0,0,0);
				Index3D size = new Index3D(0,0,0);
				Neighbor[][][] mat = toMatrix(minCorner,size);
				new Geometry(this,minCorner,size,mat).writeModel(vrml);
			}

			private Point3D computePoint(double iX, double iY, double iZ) {
				return anchorPos
						.add(anchorXup.mul((iX    )*CUBESIZE))
						.add(anchorYat.mul((iY-0.5)*CUBESIZE))
						.add(anchorZ  .mul((iZ-0.5)*CUBESIZE));
			}
			
		}

		private static class Geometry {
			
			private static final Orientation[] OArrAll = Orientation.values();
			
			private static final Index3D ind111 = new Index3D(1,1,1);
			private static final Index3D ind110 = new Index3D(1,1,0);
			private static final Index3D ind101 = new Index3D(1,0,1);
			private static final Index3D ind100 = new Index3D(1,0,0);
			private static final Index3D ind011 = new Index3D(0,1,1);
			private static final Index3D ind010 = new Index3D(0,1,0);
			private static final Index3D ind001 = new Index3D(0,0,1);
			private static final Index3D ind000 = new Index3D(0,0,0);
			
			private Neighborhood neighborhood;
			private Index3D minCorner;
			private Index3D size;
			private Neighbor[][][] mat;
			
			private HashSet<Line> lines;
			private Vector<PolyLine> extraLines;
		
			private CubeGeometry cubeGeometry;
			private GlassCubeGeometry glassCubeGeometry;
			private CurvedCubeGeometry curvedCubeGeometry;
			private CurvedRoofGeometry curvedRoofGeometry;
		
			public Geometry(Neighborhood neighborhood, Index3D minCorner, Index3D size, Neighbor[][][] mat) {
				this.neighborhood = neighborhood;
				this.minCorner = minCorner;
				this.size = size;
				this.mat = mat;
				this.lines = new HashSet<>();
				this.extraLines = new Vector<>();
				
				cubeGeometry = new CubeGeometry();
				glassCubeGeometry = new GlassCubeGeometry();
				curvedCubeGeometry = new CurvedCubeGeometry();
				curvedRoofGeometry = new CurvedRoofGeometry();
			}
		
			public void writeModel(PrintWriter vrml) {
				createLines();
				optimizeLines();
				
				StringBuilder pointsStrBase, indexesStrBase;
				createBaseModel(pointsStrBase = new StringBuilder(), 0, indexesStrBase = new StringBuilder());
				if (pointsStrBase.length()>0 || indexesStrBase.length()>0)
					VRMLoutput.writeIndexedLineSet(vrml, "", pointsStrBase, indexesStrBase, Color.BLACK);
				
				StringBuilder pointsStrW, indexesStrW;
				createWindows(pointsStrW = new StringBuilder(), 0, indexesStrW = new StringBuilder());
				if (pointsStrW.length()>0 || indexesStrW.length()>0)
					VRMLoutput.writeIndexedLineSet(vrml, "", pointsStrW, indexesStrW, COLOR_WINDOW);
			}
		
			private void setLine(boolean set, Line line) {
				if (line!=null) {
					if (set) lines.add(line);
					else     lines.remove(line);
				}
			}
		
			private Line popLine() {
				Line line = getLine();
				lines.remove(line);
				return line;
			}
		
			private Line getLine() {
				Iterator<Line> it = lines.iterator();
				if (it.hasNext()) return it.next();
				return null;
			}
		
			private void createLines() {
				cubeGeometry      .createLines();
				curvedCubeGeometry.modifyLines();
				curvedRoofGeometry.modifyLines();
			}
		
			private void optimizeLines() {
				Vector<Line> optimizedLines = new Vector<>();
				
				while (!lines.isEmpty()) {
					Line line = popLine();
					Index3D vec12 = line.p2.sub(line.p1);
					while (lines.remove(new Line(line.p2,line.p2.add(vec12)))) line.p2 = line.p2.add(vec12);
					while (lines.remove(new Line(line.p1,line.p1.sub(vec12)))) line.p1 = line.p1.sub(vec12);
					optimizedLines.add(line);
				}
				
				lines.addAll(optimizedLines);
			}
		
			private Point3D computePoint(Index3D i) { return neighborhood.computePoint(i.iX,i.iY,i.iZ); }
			private Point3D computePoint(Point3D i) { return neighborhood.computePoint(i.x,i.y,i.z); }
		
			private int addPoint(StringBuilder pointsStr, int counter, int[][][] indexes, Index3D i) {
				if (indexes[i.iX][i.iY][i.iZ]==-1) {
					indexes[i.iX][i.iY][i.iZ] = counter++;
					Point3D p = computePoint(minCorner.add(i));
					addPoint(pointsStr, p);
				}
				return counter;
			}
		
			private void addPoint(StringBuilder pointsStr, Point3D p) {
				pointsStr.append((pointsStr.length()==0?"":", ")+p.toString("%1.2f",false));
			}
		
			private int createBaseModel(StringBuilder pointsStr, int pointCounter, StringBuilder indexesStr) {
				int[][][] indexes = new int[size.iX+1][size.iY+1][size.iZ+1];
				for (int x=0; x<=size.iX; ++x)
					for (int y=0; y<=size.iY; ++y)
						Arrays.fill(indexes[x][y],-1);
				
				for (Line line:lines) {
					pointCounter = addPoint(pointsStr, pointCounter, indexes, line.p1);
					pointCounter = addPoint(pointsStr, pointCounter, indexes, line.p2);
					int ip1 = indexes[line.p1.iX][line.p1.iY][line.p1.iZ];
					int ip2 = indexes[line.p2.iX][line.p2.iY][line.p2.iZ];
					indexesStr.append(ip1+" "+ip2+" -1 ");
				}
				
				for (PolyLine pline:extraLines) {
					pointCounter = addPoint(pointsStr, pointCounter, indexes, pline.pStart);
					pointCounter = addPoint(pointsStr, pointCounter, indexes, pline.pEnd);
					int ipStart = indexes[pline.pStart.iX][pline.pStart.iY][pline.pStart.iZ];
					int ipEnd   = indexes[pline.pEnd.iX][pline.pEnd.iY][pline.pEnd.iZ];
					
					indexesStr.append(ipStart+" ");
					for (Point3D p:pline.points) {
						addPoint(pointsStr, computePoint(p.add(minCorner.toPoint3D())));
						indexesStr.append((pointCounter++)+" ");
					}
					indexesStr.append(ipEnd+" -1 ");
				}
				
				return pointCounter;
			}
		
			private void createWindows(StringBuilder pointsStrW, int pointCounterW, StringBuilder indexesStrW) {
				pointCounterW = glassCubeGeometry .createWindows(pointsStrW, pointCounterW, indexesStrW);
				pointCounterW = curvedRoofGeometry.createWindows(pointsStrW, pointCounterW, indexesStrW);
			}
		
			private class CubeGeometry {
		
				private void createLines() {
					Index3D i = new Index3D(0,0,0);
					for (int x=0; x<size.iX; ++x)
						for (int y=0; y<size.iY; ++y)
							for (int z=0; z<size.iZ; ++z)
								if (isCube(get(mat,x,y,z))) {
									i.set(x,y,z);
									if (!isCube(get(mat,x+1,y,z))) lines.add(new Line(i.add(ind111), i.add(ind110)));
									if (!isCube(get(mat,x,y+1,z))) lines.add(new Line(i.add(ind011), i.add(ind010)));
									if (!isCube(get(mat,x-1,y,z))) lines.add(new Line(i.add(ind001), i.add(ind000)));
									if (!isCube(get(mat,x,y-1,z))) lines.add(new Line(i.add(ind101), i.add(ind100)));
									
									if (!isCube(get(mat,x+1,y,z))) lines.add(new Line(i.add(ind110), i.add(ind100)));
									if (!isCube(get(mat,x,y,z+1))) lines.add(new Line(i.add(ind111), i.add(ind101)));
									if (!isCube(get(mat,x-1,y,z))) lines.add(new Line(i.add(ind011), i.add(ind001)));
									if (!isCube(get(mat,x,y,z-1))) lines.add(new Line(i.add(ind010), i.add(ind000)));
									
									if (!isCube(get(mat,x,y+1,z))) lines.add(new Line(i.add(ind111), i.add(ind011)));
									if (!isCube(get(mat,x,y,z+1))) lines.add(new Line(i.add(ind101), i.add(ind001)));
									if (!isCube(get(mat,x,y-1,z))) lines.add(new Line(i.add(ind100), i.add(ind000)));
									if (!isCube(get(mat,x,y,z-1))) lines.add(new Line(i.add(ind110), i.add(ind010)));
									
									Neighbor nb = get(mat,x-1,y,z);
									if (!isCube(nb) && !isCurvedCube(nb,OArrAll) && !isCurvedRoof(nb)) {
										lines.add(new Line(i.add(ind001), i.add(ind010)));
										lines.add(new Line(i.add(ind000), i.add(ind011)));
									}
								}
				}
				
			}
			
			private class GlassCubeGeometry {
			
				private int createWindows(StringBuilder pointsStr, int pointCounter, StringBuilder indexesStr) {
					Index3D i = new Index3D(0,0,0);
					for (int x=0; x<size.iX; ++x)
						for (int y=0; y<size.iY; ++y)
							for (int z=0; z<size.iZ; ++z) {
								Neighbor nb = get(mat,x,y,z);
								if (nb==null) continue;
								if (nb.type!=Neighbor.Type.GlassCube) continue;
								i.set(x,y,z);
								i.set(minCorner.add(i));
								Neighbor nb1;
								nb1 = get(mat,x+1,y,z); if (!isCube(nb1) && !isCurvedCube(nb1, OArrAll                          ) && !isCurvedRoof(nb1, OArrAll                                             ) ) pointCounter = addWindow(pointsStr, pointCounter, indexesStr, i, ind111, ind110, ind100, ind101);
								nb1 = get(mat,x,y+1,z); if (!isCube(nb1) && !isCurvedCube(nb1, Orientation.NegY,Orientation.NegZ) && !isCurvedRoof(nb1, Orientation.PosY, Orientation.PosZ, Orientation.NegZ) ) pointCounter = addWindow(pointsStr, pointCounter, indexesStr, i, ind111, ind110, ind010, ind011);
								nb1 = get(mat,x,y,z+1); if (!isCube(nb1) && !isCurvedCube(nb1, Orientation.NegZ,Orientation.PosY) && !isCurvedRoof(nb1, Orientation.PosY, Orientation.PosZ, Orientation.NegY) ) pointCounter = addWindow(pointsStr, pointCounter, indexesStr, i, ind111, ind101, ind001, ind011);
								nb1 = get(mat,x,y-1,z); if (!isCube(nb1) && !isCurvedCube(nb1, Orientation.PosY,Orientation.PosZ) && !isCurvedRoof(nb1, Orientation.PosZ, Orientation.NegY, Orientation.NegZ) ) pointCounter = addWindow(pointsStr, pointCounter, indexesStr, i, ind101, ind100, ind000, ind001);
								nb1 = get(mat,x,y,z-1); if (!isCube(nb1) && !isCurvedCube(nb1, Orientation.PosZ,Orientation.NegY) && !isCurvedRoof(nb1, Orientation.PosY, Orientation.NegY, Orientation.NegZ) ) pointCounter = addWindow(pointsStr, pointCounter, indexesStr, i, ind110, ind100, ind000, ind010);
							}
					return pointCounter;
				}
			
				private int addWindow(StringBuilder pointsStr, int counter, StringBuilder indexesStr, Index3D iXYZ, Index3D i1, Index3D i2, Index3D i3, Index3D i4) {
					Point3D i1_p = iXYZ.toPoint3D().add(i1.toPoint3D());
					Point3D i2_p = iXYZ.toPoint3D().add(i2.toPoint3D());
					Point3D i3_p = iXYZ.toPoint3D().add(i3.toPoint3D());
					Point3D i4_p = iXYZ.toPoint3D().add(i4.toPoint3D());
					
					addPoint(pointsStr, computePoint( i1_p.mul(0.96).add(i3_p.mul(0.04)) )); int iw1=counter++;
					addPoint(pointsStr, computePoint( i2_p.mul(0.96).add(i4_p.mul(0.04)) )); int iw2=counter++;
					addPoint(pointsStr, computePoint( i3_p.mul(0.96).add(i1_p.mul(0.04)) )); int iw3=counter++;
					addPoint(pointsStr, computePoint( i4_p.mul(0.96).add(i2_p.mul(0.04)) )); int iw4=counter++;
					
					indexesStr.append(iw1+" "+iw2+" "+iw3+" "+iw4+" "+iw1+" -1 ");
					
					return counter;
				}
				
			}
		
			private class CurvedRoofGeometry {
				private void modifyLines() {
					Index3D i = new Index3D(0,0,0);
					for (int x=0; x<size.iX; ++x)
						for (int y=0; y<size.iY; ++y)
							for (int z=0; z<size.iZ; ++z) {
								Neighbor nb = get(mat,x,y,z);
								if (nb==null) continue;
								if (nb.type!=Neighbor.Type.CurvedRoof) continue;
								i.set(x,y,z);
								setVertSideLines(nb.o,i);
								setBottomLines  (nb.o,i);
								setTopLine      (nb.o,i);
								
								extraLines.add(createRightCurvedRoofEdge(i,nb.o));
								Index3D iLeft = nb.o.pos().addTo(i);
								Neighbor nbLeft = get(mat,iLeft);
								if (nbLeft==null || nbLeft.type!=Neighbor.Type.CurvedRoof || nbLeft.o!=nb.o)
									extraLines.add(createRightCurvedRoofEdge(iLeft,nb.o));
							}
				}
		
				private void setTopLine(Orientation baseO, Index3D i) {
					switch(baseO) {
					case PosY: lines.add(new Line(i.add(ind101), i.add(ind100))); break;
					case PosZ: lines.add(new Line(i.add(ind100), i.add(ind110))); break;
					case NegY: lines.add(new Line(i.add(ind110), i.add(ind111))); break;
					case NegZ: lines.add(new Line(i.add(ind111), i.add(ind101))); break;
					}
				}
		
				private void setBottomLines(Orientation baseO, Index3D i) {
					boolean base = true, neg = false, pos = false, opp = false, bottom = false;
					Neighbor nb;
					Index3D iBot = i.add(-1, 0, 0);
					nb = get(mat, iBot);
					if (!isCube(nb) && !isCurvedRoof(nb)) {
						neg=!isCurvedCube(nb,baseO      ,baseO.neg());
						opp=!isCurvedCube(nb,baseO.neg(),baseO.opp());
						pos=!isCurvedCube(nb,baseO.opp(),baseO.pos());
						bottom=!isCurvedCube(nb, OArrAll);
					}
					nb = get(mat, iBot.add(baseO.neg())); if (!neg && !isCube(nb) && !isCurvedCube(nb, baseO.pos(), baseO.opp()) && !isCurvedRoof(nb)) neg=true;
					nb = get(mat, i   .add(baseO.neg())); if (!neg && !isCube(nb) && !isCurvedCube(nb, baseO.pos(), baseO.opp()) && !isCurvedRoof(nb, baseO, baseO.neg(), baseO.opp())) neg=true;
					nb = get(mat, iBot.add(baseO.opp())); if (!opp && !isCube(nb) && !isCurvedCube(nb, baseO      , baseO.pos()) && !isCurvedRoof(nb)) opp=true;
					nb = get(mat, i   .add(baseO.opp())); if (!opp && !isCube(nb) && !isCurvedCube(nb, baseO      , baseO.pos()) && !isCurvedRoof(nb, baseO.neg(), baseO.opp(), baseO.pos())) opp=true;
					nb = get(mat, iBot.add(baseO.pos())); if (!pos && !isCube(nb) && !isCurvedCube(nb, baseO.neg(), baseO      ) && !isCurvedRoof(nb)) pos=true;
					nb = get(mat, i   .add(baseO.pos())); if (!pos && !isCube(nb) && !isCurvedCube(nb, baseO.neg(), baseO      ) && !isCurvedRoof(nb, baseO.opp(), baseO.pos(), baseO)) pos=true;
					
					Line baseLine = null;
					Line posLine = null;
					Line oppLine = null;
					Line negLine = null;
					switch(baseO) {
					case PosY: baseLine = new Line(i.add(ind010), i.add(ind011)); posLine = new Line(i.add(ind011), i.add(ind001)); break;
					case PosZ: baseLine = new Line(i.add(ind011), i.add(ind001)); posLine = new Line(i.add(ind001), i.add(ind000)); break;
					case NegY: baseLine = new Line(i.add(ind001), i.add(ind000)); posLine = new Line(i.add(ind000), i.add(ind010)); break;
					case NegZ: baseLine = new Line(i.add(ind000), i.add(ind010)); posLine = new Line(i.add(ind010), i.add(ind011)); break;
					}
					switch(baseO) {
					case PosY: oppLine = new Line(i.add(ind001), i.add(ind000)); negLine = new Line(i.add(ind000), i.add(ind010)); break;
					case PosZ: oppLine = new Line(i.add(ind000), i.add(ind010)); negLine = new Line(i.add(ind010), i.add(ind011)); break;
					case NegY: oppLine = new Line(i.add(ind010), i.add(ind011)); negLine = new Line(i.add(ind011), i.add(ind001)); break;
					case NegZ: oppLine = new Line(i.add(ind011), i.add(ind001)); negLine = new Line(i.add(ind001), i.add(ind000)); break;
					}
					
					if (bottom) {
						lines.add(new Line(i.add(ind011), i.add(ind000)));
						lines.add(new Line(i.add(ind001), i.add(ind010)));
					}
					setLine(base, baseLine);
					setLine(pos, posLine);
					setLine(opp, oppLine);
					setLine(neg, negLine);
				}
		
				private void setVertSideLines(Orientation baseO, Index3D i) {
					boolean oppNeg = false, oppPos = false;
					Neighbor nb;
					Index3D iOpp = i.add(baseO.opp());
					nb = get(mat, iOpp);
					if (!isCube(nb) && !isCurvedCube(nb, baseO, baseO.pos())) {
						oppNeg=!isCurvedRoof(nb, baseO.pos(), baseO.opp());
						oppPos=!isCurvedRoof(nb, baseO.neg(), baseO.opp());
					}
					nb = get(mat, iOpp.add(baseO.neg())); if (!oppNeg && !isCube(nb) && !isCurvedCube(nb, baseO.pos()) && !isCurvedRoof(nb, baseO.opp(), baseO.neg())) oppNeg=true;
					nb = get(mat, i   .add(baseO.neg())); if (!oppNeg && !isCube(nb) && !isCurvedCube(nb, baseO.opp()) && !isCurvedRoof(nb, baseO.neg(), baseO      )) oppNeg=true;
					nb = get(mat, iOpp.add(baseO.pos())); if (!oppPos && !isCube(nb) && !isCurvedCube(nb, baseO      ) && !isCurvedRoof(nb, baseO.opp(), baseO.pos())) oppPos=true;
					nb = get(mat, i   .add(baseO.pos())); if (!oppPos && !isCube(nb) && !isCurvedCube(nb, baseO.neg()) && !isCurvedRoof(nb, baseO.pos(), baseO      )) oppPos=true;
				
					Line oppNegLine = null;
					Line oppPosLine = null;
					switch(baseO) {
					case PosY: oppNegLine = new Line(i.add(ind100), i.add(ind000)); oppPosLine = new Line(i.add(ind101), i.add(ind001)); break;
					case PosZ: oppNegLine = new Line(i.add(ind110), i.add(ind010)); oppPosLine = new Line(i.add(ind100), i.add(ind000)); break;
					case NegY: oppNegLine = new Line(i.add(ind111), i.add(ind011)); oppPosLine = new Line(i.add(ind110), i.add(ind010)); break;
					case NegZ: oppNegLine = new Line(i.add(ind101), i.add(ind001)); oppPosLine = new Line(i.add(ind111), i.add(ind011)); break;
					}
					
					setLine(oppNeg, oppNegLine);
					setLine(oppPos, oppPosLine);
				}
		
				private PolyLine createRightCurvedRoofEdge(Index3D i, Orientation o) {
					PolyLine pline = null;
					switch (o) {
					case PosY: pline = new PolyLine(i.add(ind100), i.add(ind010)); break;
					case PosZ: pline = new PolyLine(i.add(ind110), i.add(ind011)); break;
					case NegY: pline = new PolyLine(i.add(ind111), i.add(ind001)); break;
					case NegZ: pline = new PolyLine(i.add(ind101), i.add(ind000)); break;
					}
					
					int seg = 4;
					for (int wi=1; wi<seg; ++wi) {
						double w = wi*Math.PI/2/seg;
						double x=0,y=0,z=0;
						switch (o) {
						case PosY: z=0; x=Math.cos(w); y=  Math.sin(w); break;
						case PosZ: y=1; x=Math.cos(w); z=  Math.sin(w); break;
						case NegY: z=1; x=Math.cos(w); y=1-Math.sin(w); break;
						case NegZ: y=0; x=Math.cos(w); z=1-Math.sin(w); break;
						}
						pline.points.add(new Point3D(i.iX+x,i.iY+y,i.iZ+z));
					}
					
					return pline;
				}
		
				private int createWindows(StringBuilder pointsStr, int pointCounter, StringBuilder indexesStr) {
					Index3D i = new Index3D(0,0,0);
					for (int x=0; x<size.iX; ++x)
						for (int y=0; y<size.iY; ++y)
							for (int z=0; z<size.iZ; ++z) {
								Neighbor nb = get(mat,x,y,z);
								if (nb==null) continue;
								if (nb.type!=Neighbor.Type.CurvedRoof) continue;
								i.set(x,y,z);
								i.set(minCorner.add(i));
								pointCounter = addWindow(pointsStr, pointCounter, indexesStr, i, nb.o);
							}
					return pointCounter;
				}
				
				private int addWindow(StringBuilder pointsStr, int counter, StringBuilder indexesStr, Index3D i, Orientation o) {
					double wStart = Math.atan(0.1);
					double wEnd = Math.PI/2-wStart;
					String strLeft = "";
					String strRight = "";
					int seg = 4;
					for (int wi=0; wi<=seg; ++wi) {
						double w = wi*(wEnd-wStart)/seg + wStart;
						double x=0,y=0,z=0,dy=0,dz=0;
						switch (o) {
						case PosY: z=0.5; dz= 0.4; x=Math.cos(w); y=  Math.sin(w); break;
						case PosZ: y=0.5; dy= 0.4; x=Math.cos(w); z=  Math.sin(w); break;
						case NegY: z=0.5; dz=-0.4; x=Math.cos(w); y=1-Math.sin(w); break;
						case NegZ: y=0.5; dy=-0.4; x=Math.cos(w); z=1-Math.sin(w); break;
						}
						addPoint(pointsStr, computePoint( new Point3D(i.iX+x,i.iY+y-dy,i.iZ+z-dz) ));
						addPoint(pointsStr, computePoint( new Point3D(i.iX+x,i.iY+y+dy,i.iZ+z+dz) ));
						strRight = strRight+(counter+2*wi)+" ";
						strLeft  = (counter+2*wi+1)+" "+strLeft;
					}
					indexesStr.append(strRight+" "+strLeft+" "+counter+" -1 ");
					
					counter += 2*seg+2;
					return counter;
				}
			}
		
			private class CurvedCubeGeometry {
		
				private void modifyLines() {
					Index3D i = new Index3D(0,0,0);
					for (int x=0; x<size.iX; ++x)
						for (int y=0; y<size.iY; ++y)
							for (int z=0; z<size.iZ; ++z) {
								Neighbor nb = get(mat,x,y,z);
								if (nb==null) continue;
								if (nb.type!=Neighbor.Type.CurvedCube) continue;
								i.set(x,y,z);
								setVertCenterLine(nb.o      ,i);
								setVertSideLines (nb.o      ,i);
								setHorizLines    (nb.o      ,i);
								setHorizLines    (nb.o.neg(),i);
								
								extraLines.add(createCurvedCubeEdge(i,nb.o));
								Neighbor nbBelow = get(mat,x-1,y,z);
								if (nbBelow==null || nbBelow.type!=Neighbor.Type.CurvedCube || nbBelow.o!=nb.o)
									extraLines.add(createCurvedCubeEdge(i.add(-1,0,0),nb.o));
								
								if (!isCube(nbBelow) && !isCurvedCube(nbBelow,OArrAll) && !isCurvedRoof(nbBelow)) {
									setBottom(nb.o, i);
								}
							}
				}
		
				private void setBottom(Orientation baseO, Index3D i) {
					switch(baseO) {
					case NegY: 
					case PosY: lines.add(new Line(i.add(ind011), i.add(ind000))); break;
					case NegZ:
					case PosZ: lines.add(new Line(i.add(ind001), i.add(ind010))); break;
					}
				}
		
				private PolyLine createCurvedCubeEdge(Index3D i, Orientation o) {
					PolyLine pline = null;
					switch (o) {
					case PosY: pline = new PolyLine(i.add(ind111), i.add(ind100)); break;
					case PosZ: pline = new PolyLine(i.add(ind101), i.add(ind110)); break;
					case NegY: pline = new PolyLine(i.add(ind100), i.add(ind111)); break;
					case NegZ: pline = new PolyLine(i.add(ind110), i.add(ind101)); break;
					}
					
					int seg = 4;
					for (int wi=1; wi<seg; ++wi) {
						double w = wi*Math.PI/2/seg;
						double x=0,y=0,z=0;
						switch (o) {
						case PosY: x=1; y=1-Math.sin(w); z=  Math.cos(w); break;
						case PosZ: x=1; y=1-Math.cos(w); z=1-Math.sin(w); break;
						case NegY: x=1; y=  Math.sin(w); z=1-Math.cos(w); break;
						case NegZ: x=1; y=  Math.cos(w); z=  Math.sin(w); break;
						}
						pline.points.add(new Point3D(i.iX+x,i.iY+y,i.iZ+z));
					}
					
					return pline;
				}
		
				private void setHorizLines(Orientation baseO, Index3D i) {
					Index3D iNb = i.add(baseO);
					boolean upper = false, lower = false;
					Neighbor nb;
					nb = get(mat, iNb);
					if (!isCube(nb) && !isCurvedCube(nb,baseO.opp(),baseO.neg())) {
						lower = !isCurvedRoof(nb,baseO,baseO.pos(),baseO.neg());
						upper = true;
					}
					nb = get(mat, iNb.add( 1,0,0)); if (!upper && !isCube(nb) && !isCurvedCube(nb,baseO.opp(),baseO.neg()) && !isCurvedRoof(nb,baseO,baseO.pos(),baseO.neg()      )) upper = true;
					nb = get(mat, iNb.add(-1,0,0)); if (!lower && !isCube(nb) && !isCurvedCube(nb,baseO.opp(),baseO.neg()) && !isCurvedRoof(nb                                    )) lower = true;
					nb = get(mat, i  .add( 1,0,0)); if (!upper && !isCube(nb) && !isCurvedCube(nb,baseO.pos(),baseO      ) && !isCurvedRoof(nb,baseO.opp(),baseO.pos(),baseO.neg())) upper = true;
					nb = get(mat, i  .add(-1,0,0)); if (!lower && !isCube(nb) && !isCurvedCube(nb,baseO.pos(),baseO      ) && !isCurvedRoof(nb,OArrAll                            )) lower = true;
					
					Line lowerLine=null;
					Line upperLine=null;
					switch(baseO) {
					case PosY: lowerLine=new Line(i.add(ind011), i.add(ind010)); upperLine=new Line(i.add(ind111), i.add(ind110)); break;
					case PosZ: lowerLine=new Line(i.add(ind001), i.add(ind011)); upperLine=new Line(i.add(ind101), i.add(ind111)); break;
					case NegY: lowerLine=new Line(i.add(ind000), i.add(ind001)); upperLine=new Line(i.add(ind100), i.add(ind101)); break;
					case NegZ: lowerLine=new Line(i.add(ind010), i.add(ind000)); upperLine=new Line(i.add(ind110), i.add(ind100)); break;
					}
					
					setLine(lower, lowerLine);
					setLine(upper, upperLine);
				}
		
				private void setVertSideLines(Orientation baseO, Index3D i) {
					switch(baseO) {
					case NegY:
					case PosY:
						lines.add(new Line(i.add(ind111), i.add(ind011)));
						lines.add(new Line(i.add(ind100), i.add(ind000)));
						break;
					case NegZ:
					case PosZ:
						lines.add(new Line(i.add(ind110), i.add(ind010)));
						lines.add(new Line(i.add(ind101), i.add(ind001)));
						break;
					}
				}
		
				private void setVertCenterLine(Orientation baseO, Index3D i) {
					Index3D i1;
					Orientation nextO;
					
					boolean setCenterLine = false;
					for (i1=i.add(baseO), nextO=baseO.neg(); nextO!=baseO; i1=i1.add(nextO), nextO=nextO.neg()) {
						Neighbor nb1 = get(mat,i1);
						if (!isCube(nb1) && !isCurvedCube(nb1, nextO) && !isCurvedRoof(nb1, nextO.pos(),nextO.opp()) ) {
							setCenterLine = true;
							break;
						}
					}
					
					Line centerLine=null;
					switch(baseO) {
					case PosY: centerLine=new Line(i.add(ind110), i.add(ind010)); break;
					case PosZ: centerLine=new Line(i.add(ind111), i.add(ind011)); break;
					case NegY: centerLine=new Line(i.add(ind101), i.add(ind001)); break;
					case NegZ: centerLine=new Line(i.add(ind100), i.add(ind000)); break;
					}
					setLine(setCenterLine, centerLine);
				}
				
			}
		}

		private static class PolyLine {
			Index3D pStart,pEnd;
			Vector<Point3D> points;
			PolyLine(Index3D pStart,Index3D pEnd) {
				this.pStart = pStart;
				this.pEnd = pEnd;
				points = new Vector<>();
			}
			@Override
			public String toString() {
				return "PolyLine("+pStart+","+points+"," +pEnd+ ")";
			}
		}
		
		private static class Line {
			Index3D p1,p2;
			Line(Index3D p1,Index3D p2) {
				this.p1 = p1;
				this.p2 = p2;
			}
			@Override
			public boolean equals(Object obj) {
				if (!(obj instanceof Line)) return false;
				Line line = (Line)obj;
				if (line.p1.equals(p1) && line.p2.equals(p2)) return true;
				if (line.p2.equals(p1) && line.p1.equals(p2)) return true;
				return false;
			}
			@Override
			public int hashCode() {
				return p1.hashCode() ^ p2.hashCode();
			}
			@Override
			public String toString() {
				return "Line("+p1+","+p2+")";
			}
		}
		
		private enum Orientation {
			PosY, PosZ, NegY, NegZ;

			private static Orientation get(int i) {
				switch(i) {
				case 0: return Orientation.PosY;
				case 1: return Orientation.PosZ;
				case 2: return Orientation.NegY;
				case 3: return Orientation.NegZ;
				}
				return null;
			}

			public Index3D addTo(Index3D i) {
				switch(this) {
				case NegY: return i.add(0,-1,0);
				case NegZ: return i.add(0,0,-1);
				case PosY: return i.add(0, 1,0);
				case PosZ: return i.add(0,0, 1);
				}
				return null;
			}

			public Orientation pos() {
				return get((ordinal()+1)%4);
			}

			public Orientation opp() {
				return get((ordinal()+2)%4);
			}

			public Orientation neg() {
				return get((ordinal()+3)%4);
			}
		}
		
		private static class Index3D {
			int iX,iY,iZ;
			
			public Index3D (int iX, int iY, int iZ) { this.iX = iX; this.iY = iY; this.iZ = iZ; }
			public void set(int iX, int iY, int iZ) { this.iX = iX; this.iY = iY; this.iZ = iZ; }

			public Index3D (Index3D i) { this(i.iX,i.iY,i.iZ); }
			public void set(Index3D i) { set (i.iX,i.iY,i.iZ); }
			
			public Point3D toPoint3D() { return new Point3D(iX,iY,iZ); }

			public Index3D add(Index3D i) { return new Index3D(iX+i.iX, iY+i.iY, iZ+i.iZ); }
			public Index3D add(int iX, int iY, int iZ) { return new Index3D(iX+this.iX, iY+this.iY, iZ+this.iZ); }
			public Index3D sub(Index3D i) { return new Index3D(iX-i.iX, iY-i.iY, iZ-i.iZ); }
			public Index3D add(Orientation o) {
				switch(o) {
				case PosY: return add(0, 1, 0);
				case PosZ: return add(0, 0, 1);
				case NegY: return add(0,-1, 0);
				case NegZ: return add(0, 0,-1);
				}
				return null;
			}

			public void min(Index3D index) {
				iX = Math.min(iX, index.iX);
				iY = Math.min(iY, index.iY);
				iZ = Math.min(iZ, index.iZ);
			}

			public void max(Index3D index) {
				iX = Math.max(iX, index.iX);
				iY = Math.max(iY, index.iY);
				iZ = Math.max(iZ, index.iZ);
			}

			@Override
			public int hashCode() {
				return (iX&0xFFF)<<(5*4) | (iY&0xFF)<<(3*4) | (iZ&0xFFF)<<(0*4);
			}

			@Override
			public boolean equals(Object obj) {
				if (!(obj instanceof Index3D)) return false;
				Index3D other = (Index3D)obj;
				return iX==other.iX && iY==other.iY && iZ==other.iZ;
			}
			@Override
			public String toString() {
				return "Index3D("+iX+","+iY+","+iZ+")";
			}
		}
	}

	private static class VRMLoutput {

		private static final String VRML_TEMPLATE_MODELS_WRL = "/vrml/template.models.wrl";
		private static final HashMap<String, String> mapObjectID2Model = new HashMap<>();
		private static final HashMap<String, Proto> mapModel2Proto = new HashMap<>();
		private static JFileChooser vrmlFileChooser;
		private static FileNameExtensionFilter vrmlFileFilter;
		
		private static void createModelMap() {
			mapObjectID2Model.clear();
			mapModel2Proto.clear();
			
			addModels("__SIMPLE_LINE",  "^U_PIPELINE","^U_PORTALLINE","^U_POWERLINE","^U_BYTEBEATLINE");
			
			SoftwareBuildModels.addModelsToModelMap();
			
		}
		private static void writeProtos(PrintWriter vrml, HashSet<String> usedModels) {
			
			writeSimpleLineProtoToFile(vrml);
			
			mapModel2Proto.forEach((model,proto)->{
				if (usedModels.contains(model)) proto.writeProtoToFile(vrml);
				if (proto.requiredProto!=null && usedModels.contains(proto.requiredProto)) proto.writeProtoToFile(vrml);
			});
			
			if (usedModels.contains("GENERAL_DECAL")) SoftwareBuildModels.write_IMAGE_DECAL_Proto(vrml);
		}
		
		private static String getRoofModelName(String modelName) {
			return SoftwareBuildModels.MAINROOMmodels.getRoofModelName(modelName);
		}

		private static String[] makeVariations(String format, String... strs) {
			String[] variations = new String[strs.length];
			for (int i = 0; i < strs.length; i++)
				variations[i] = String.format(format, strs[i]);
			return variations;
		}

		private static String[] makeVariations(String format, Integer... numbers) {
			String[] variations = new String[numbers.length];
			for (int i = 0; i < numbers.length; i++)
				variations[i] = String.format(format, numbers[i]);
			return variations;
		}
		
		private static void addModels(String modelName, String... objectIDs) {
			for (String id:objectIDs)
				mapObjectID2Model.put(id, modelName);
		}
		
		private static void addProto(Proto proto) {
			if (mapModel2Proto.containsKey(proto.protoName))
				throw new IllegalStateException(String.format("Can't add proto \"%s\" to list. A proto with the same name already exists.", proto.protoName));
			
			mapModel2Proto.put(proto.protoName, proto); 
		}
		
		private static class SoftwareBuildModels {
			
			private static final Color PLANT_COLOR = new Color(0.3f,0.6f,0.3f);

			static void addModelsToModelMap() {
				
				//addModels("CUBEROOM_SPACE", "^CUBEROOM_SPACE","^CUBEROOMB_SPACE","^CUBEROOMC_SPACE","^FREIGHTER_CORE");
				addModels("SMALLLIGHT",     "^SMALLLIGHT");
				addModels("WALLLIGHT",      makeVariations("^WALLLIGHT%s", "BLUE","GREEN","PINK","RED","WHITE","YELLOW"));
				
				addModels("BUILDLANDINGPAD","^BUILDLANDINGPAD");
				
				addModels("BUILDSIMPLEDESK","^BUILDSIMPLEDESK");
				addModels("BUILDCHAIR",     "^BUILDCHAIR");
				addModels("BUILDBED",       "^BUILDBED");
				
				addModels("BASE_FLAG",      "^BASE_FLAG");
				addModels("PARAGON",        "^U_PARAGON");
				addModels("BUILDSAVE",      "^BUILDSAVE");
				addModels("MINIPORTAL",     "^U_MINIPORTAL");
				
				addModels("NPCTERMINAL",    "^NPCVEHICLETERM","^NPCWEAPONTERM","^NPCSCIENCETERM","^NPCFARMTERM","^NPCBUILDERTERM");
				
				addModels("PLANT",          "^BARRENPLANT","^CREATUREPLANT","^GRAVPLANT","^LUSHPLANT","^NIPPLANT","^PEARLPLANT","^POOPPLANT",
				                            "^RADIOPLANT","^SACVENOMPLANT","^SCORCHEDPLANT","^SNOWPLANT","^TOXICPLANT");
				
				addModels("GENERAL_DECAL",  makeVariations("^SPEC_DECAL%02d", 1,2,3,4,5,6,7,8));
				addModels("GENERAL_DECAL",  makeVariations("^BUILDDECALVIS%d", 1,2,3,4,5));
				addModels("GENERAL_DECAL",  makeVariations("^BUILDDECALSIMP%d", 1,2,3,4));
				addModels("GENERAL_DECAL",  makeVariations("^BUILDDECALNUM%d", 1,2,3,4,5,6,7,8,9,0));
				addModels("GENERAL_DECAL",  "^DECAL_HAZARD", "^DECAL_HORROR", "^DECAL_JELLY", "^DECAL_SKULL");
				addModels("GENERAL_DECAL",  "^BUILDDECAL", "^BUILDDECAL2", "^BUILDDECALHELLO", "^BUILDDECALNMS");
				
				addProto( new Proto("SMALLLIGHT"     , 0.20,   0,     create_SMALLLIGHT     (), 4) );
				addProto( new Proto("WALLLIGHT"      , 0.20,  90, 12, create_WALLLIGHT      ()) );
				addProto( new Proto("BUILDLANDINGPAD", 3.00,   0,     create_BUILDLANDINGPAD()) );
				addProto( new Proto("BUILDSIMPLEDESK", 0.50,   0,     create_BUILDSIMPLEDESK()) );
				addProto( new Proto("BUILDCHAIR"     , 0.50,   0,     create_BUILDCHAIR     ()) );
				addProto( new Proto("BUILDBED"       , 0.50,   0,     create_BUILDBED       ()) );
				addProto( new Proto("BASE_FLAG"      , 0.50, 135,     create_BASE_FLAG      ()) );
				addProto( new Proto("PARAGON"        , 0.50, 135, new Point3D(1.4,0,0), create_PARAGON()));
				addProto( new Proto("BUILDSAVE"      , 0.50,   0,     create_BUILDSAVE      ()) );
				addProto( new Proto("MINIPORTAL"     , 0.50,   0,     create_MINIPORTAL     ()) );
				addProto( new Proto("NPCTERMINAL"    , 0.50, 180, new Point3D(1.2,1.6,0), create_NPCTERMINAL()));
				addProto( new Proto("PLANT"          , 0.50, PLANT_COLOR, PLANT_COLOR, create_PLANT()));
				addProto( new Proto("GENERAL_DECAL"  , 0.50, 180, 12, create_GENERAL_DECAL  ()) );
				
				Garages.addModelsToModelMap();
				MAINROOMmodels.addModelsToModelMap();
				CubeRoomObjects.addModelsToModelMap();
				SolitaryWallsAndFloors.addModelsToModelMap();
				Corridors.addModelsToModelMap();
				PoweredDevices.addModelsToModelMap();
			}

			private static LineGeometry.IndexedLineSet create_PLANT() {
				String coords  = "0 0 0, 0.2 0.05 0, 0.4 0.2 0, 0.3 -0.1 0, 0.6 -0.25 0, 0.4 0 0.04, 0.7 0 0.1";
				String indexes = "0 1 2 -1 0 3 4 -1 0 5 6";
				LineGeometry.IndexedLineSet lineSet = LineGeometry.IndexedLineSet.parse(coords,indexes, "PLANT");
				if (lineSet==null) lineSet = new LineGeometry.PolyLine();
				return lineSet;
			}

			private static LineGeometry.IndexedLineSet create_NPCTERMINAL() {
				StringBuilder coords = new StringBuilder();
				StringBuilder indexes = new StringBuilder();
				
				coords.append("0    0.8  1.0,");
				coords.append("0    1.8  1.0,");
				coords.append("1.0  1.9  1.2,");
				coords.append("1.2  2.0  1.4,");
				coords.append("1.2  0.8  1.4,");
				coords.append("1.2  1.2  1.0,");
				coords.append("1.2  1.2 -1.0,");
				coords.append("1.2  0.8 -1.4,");
				coords.append("1.2  2.0 -1.4,");
				coords.append("1.0  1.9 -1.2,");
				coords.append("0    1.8 -1.0,");
				coords.append("0    0.8 -1.0,");
				
				coords.append("0.8  0.30  0.25,");
				coords.append("0.8  0.45  0.40,");
				coords.append("0.8  0.65  0.40,");
				coords.append("0.8  0.80  0.25,");
				coords.append("0.8  0.80 -0.25,");
				coords.append("0.8  0.65 -0.40,");
				coords.append("0.8  0.45 -0.40,");
				coords.append("0.8  0.30 -0.25,");
				
				coords.append("1.1  0.25  0.1,");
				coords.append("0.8  0.3   0.1,");
				coords.append("0.0  0.8   0.1,");
				coords.append("0.0  0.8   0.3,");
				coords.append("0.0  0.3   0.3,");
				coords.append("0.0  0.3  -0.3,");
				coords.append("0.0  0.8  -0.3,");
				coords.append("0.0  0.8  -0.1,");
				coords.append("0.8  0.3  -0.1,");
				coords.append("1.1  0.25 -0.1,");
				
				coords.append("1.1  0.25  0.3,");
				coords.append("1.3  0.25  0.3,");
				coords.append("1.3  0.25 -0.3,");
				coords.append("1.1  0.25 -0.3,");
				
				// beine
				coords.append("0.0   1.2   0.4,");
				coords.append("0.65  1.45  0.4,");
				coords.append("0.85  0.75  0.0,");
				coords.append("0.65  1.45 -0.4,");
				coords.append("0.0   1.2  -0.4,");
				
				// arme
				coords.append("1.2   1.5   0.3,");
				coords.append("1.25  1.1   0.4,");
				coords.append("1.5   0.8   0.0,");
				coords.append("1.6   1.1  -0.4,");
				coords.append("1.9   1.4  -0.4,");
				
				coords.append("1.55  0.8   0.00,");
				coords.append("1.61  0.8   0.14,");
				coords.append("1.75  0.8   0.20,");
				coords.append("1.89  0.8   0.14,");
				coords.append("1.95  0.8   0.00,");
				coords.append("1.89  0.8  -0.14,");
				coords.append("1.75  0.8  -0.20,");
				coords.append("1.61  0.8  -0.14 ");
				
				indexes.append(" 0 1 2 3 4 5 6 7 8 9 10 11 -1");
				indexes.append(" 3 8 -1 1 10 -1 4 2 9 7 -1");
				indexes.append(" 12 13 14 15 16 17 18 19 12 -1");
				indexes.append(" 20 21 22 23 24 25 26 27 28 29 -1");
				indexes.append(" 30 31 32 33 30 -1");
				indexes.append(" 34 35 36 37 38 -1");
				indexes.append(" 39 40 41 42 43 -1");
				indexes.append(" 36 41 44 45 46 47 48 49 50 51 44 -1");
			
				LineGeometry.IndexedLineSet lineSet = LineGeometry.IndexedLineSet.parse(coords.toString(),indexes.toString(), "NPCTERMINAL");
				if (lineSet==null) lineSet = new LineGeometry.PolyLine();
				return lineSet;
			}

			private static LineGeometry.GroupingNode create_MINIPORTAL() {
				double width = 2.5; // "small width" of hexagon
				double radius = width/2/Math.cos(Math.PI/6); // r*cos(30�) = width/2
				double radius2 = 0.6;

				double plugPDist = 1.61102; // Y|Z
				double plugPHeight = 0.6; // X
				double plugPPosY = Math.sqrt(plugPDist*plugPDist-plugPHeight*plugPHeight);
				double plugTDist = 1.82683; // Y|Z
				double plugTHeight = 1.6; // X
				double plugTPosY = Math.sqrt(plugTDist*plugTDist-plugTHeight*plugTHeight);
				
				
				LineGeometry.PolyLine p1,p2,p3,p4,p5,p6;
				LineGeometry.GroupingNode group = new LineGeometry.GroupingNode()
						.add(p1 = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D( 0  ,0,0), radius , 0, 360, false, 6))
						.add(p2 = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(0.35,0,0), radius2, 0, 360, false, 12))
						.add(new LineGeometry.PolyLine()
								.add(p2.get(11)).add(p1.get(5))
								.add(p2.get( 9)).add(p1.get(4))
								.add(p2.get( 7)).add(p1.get(3))
								.add(p2.get( 5)).add(p1.get(2))
								.add(p2.get( 3)).add(p1.get(1))
								.add(p2.get( 1)).add(p1.get(0))
								.close())
						
						
						.add(p3 = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(4.00,0,0), 0.50, 0, 360, false, 24))
						.add(p4 = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(3.80,0,0), 0.95, 0, 360, false, 24))
						.add(p5 = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(3.70,0,0), 0.95, 0, 360, false, 24))
						.add(p6 = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(3.65,0,0), 1.00, 0, 360, false, 24))
						.add(new LineGeometry.PolyLine().add(p3.get( 0)).add(p4.get( 0)))
						.add(new LineGeometry.PolyLine().add(p3.get( 3)).add(p4.get( 3)))
						.add(new LineGeometry.PolyLine().add(p3.get( 6)).add(p4.get( 6)))
						.add(new LineGeometry.PolyLine().add(p3.get( 9)).add(p4.get( 9)))
						.add(new LineGeometry.PolyLine().add(p3.get(12)).add(p4.get(12)))
						.add(new LineGeometry.PolyLine().add(p3.get(15)).add(p4.get(15)))
						.add(new LineGeometry.PolyLine().add(p3.get(18)).add(p4.get(18)))
						.add(new LineGeometry.PolyLine().add(p3.get(21)).add(p4.get(21)))
						.add(new LineGeometry.PolyLine().add(p5.get( 0)).add(p6.get( 0)))
						.add(new LineGeometry.PolyLine().add(p5.get( 3)).add(p6.get( 3)))
						.add(new LineGeometry.PolyLine().add(p5.get( 6)).add(p6.get( 6)))
						.add(new LineGeometry.PolyLine().add(p5.get( 9)).add(p6.get( 9)))
						.add(new LineGeometry.PolyLine().add(p5.get(12)).add(p6.get(12)))
						.add(new LineGeometry.PolyLine().add(p5.get(15)).add(p6.get(15)))
						.add(new LineGeometry.PolyLine().add(p5.get(18)).add(p6.get(18)))
						.add(new LineGeometry.PolyLine().add(p5.get(21)).add(p6.get(21)))
						
						.add(new LineGeometry.PolyLine().add(0,-radius,0).add(plugPHeight,-plugPPosY,0).add(plugTHeight,-plugTPosY,0))
						;
				return group;
			}

			private static void write_IMAGE_DECAL_Proto(PrintWriter vrml) {
				vrml.println("PROTO IMAGE_DECAL [");
				vrml.println("	field MFString url []");
				vrml.println("] {");
				vrml.println("	Shape {");
				vrml.println("		appearance Appearance {");
				vrml.println("			material Material { diffuseColor 1 1 1 }");
				vrml.println("			texture ImageTexture { url IS url }");
				vrml.println("		}");
				vrml.println("		geometry IndexedFaceSet {");
				vrml.println("			solid FALSE");
				vrml.println("			coord Coordinate { point [ 0 1 -1, 0 1 1, 0 -1 1, 0 -1 -1 ] }");
				vrml.println("			texCoord TextureCoordinate { point [ 0 0, 1 0, 1 1, 0 1 ] }");
				vrml.println("			coordIndex [ 0 1 2 3 ]");
				vrml.println("		}");
				vrml.println("	}");
				vrml.println("}");
				vrml.println();
			}
			
			private static LineGeometry.PolyLine create_GENERAL_DECAL() {
				// |Up| -> 3.00x   H:4.94  W:4.0     Ratio: 1,235   
				// |Up| -> 1.00x   H:1.64  W:1.32914 Ratio: 1,234
				// |Up| -> 0.25x   H:0.41  W:0.34015 Ratio: 1,2053505806261943260326326620609
				LineGeometry.PolyLine group = new LineGeometry.PolyLine()
						.addArc(LineGeometry.Axis.X, new Point3D(0,0,0), 0.75, 0, 360, false, 16)
						;
				return group;
			}
			
			private static LineGeometry.GroupingNode create_BUILDSAVE() {
				// radius: Abstand zum Messpunkt: 0.85
				// 185(0,85) : 137  0,62945945945945945945945945945946 -> Max.Radius
				// 185(0,85) : 114  0,52378378378378378378378378378378 -> TopRadius1==BottomRadius
				// 185(0,85) : 101  0,46405405405405405405405405405405 -> TopRadius2
				// 185(0,85) : 172  0,79027027027027027027027027027027 -> height4Top
				// 185(0,85) : 163  0,74891891891891891891891891891892 -> height3Top
				// 185(0,85) :  51  0,23432432432432432432432432432432 -> height2Max
				double radius1Bottom = 0.52;
				double radius2Max    = 0.63, height2Max = 0.23;
				double radius3Top    = 0.52, height3Top = 0.75;
				double radius4Top    = 0.46, height4Top = 0.79;
				
				LineGeometry.PolyLine c1,c2,c3,c4;
				LineGeometry.GroupingNode group = new LineGeometry.GroupingNode()
						.add(c1=new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(    0     , 0, 0), radius1Bottom, 0, 360, false, 32))
						.add(c2=new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(height2Max, 0, 0), radius2Max   , 0, 360, false, 32))
						.add(c3=new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(height3Top, 0, 0), radius3Top   , 0, 360, false, 32))
						.add(c4=new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(height4Top, 0, 0), radius4Top   , 0, 360, false, 32))
						.add(new LineGeometry.PolyLine().add(c1.get( 1)).add(c2.get( 1)).add(c3.get( 1)).add(c4.get( 1)).add(c4.get(15)).add(c3.get(15)).add(c2.get(15)).add(c1.get(15)))
						.add(new LineGeometry.PolyLine().add(c1.get(31)).add(c2.get(31)).add(c3.get(31)).add(c4.get(31)).add(c4.get(17)).add(c3.get(17)).add(c2.get(17)).add(c1.get(17)))
						.add(new LineGeometry.PolyLine().add(c1.get( 7)).add(c2.get( 7)).add(c3.get( 7)).add(c4.get( 7)).add(c4.get(25)).add(c3.get(25)).add(c2.get(25)).add(c1.get(25)))
						.add(new LineGeometry.PolyLine().add(c1.get( 9)).add(c2.get( 9)).add(c3.get( 9)).add(c4.get( 9)).add(c4.get(23)).add(c3.get(23)).add(c2.get(23)).add(c1.get(23)))
						;
				return group;
			}
			
			private static LineGeometry.PolyLine create_PARAGON() {
				double top    = 2.5;
				double bottom = 1.5;
				double width  = 0.5;
				double midSpace = 0.2;
				double offset = 0.1;
				
				LineGeometry.PolyLine sign = new LineGeometry.PolyLine()
						.add(bottom,0,-offset)
						.add(top/2+bottom/2+midSpace/2,0, width/2)
						.add(top/2+bottom/2+midSpace/2,0, 0)
						.add(top,0,offset)
						.add(top/2+bottom/2-midSpace/2,0,-width/2)
						.add(top/2+bottom/2-midSpace/2,0, 0)
						.close()
						;
				return sign;
			}
			
			private static LineGeometry.IndexedLineSet create_BASE_FLAG() {
				// display: -Z,+Y
				double footDist = 0.7; // along Z|Y
				double height = 1.2; // 370(1.6):280
				double bodyTop    = 0.70; // 370(1.6):160
				double bodyBottom = 0.15;
				double bodyWidth  = 0.50;
				
				LineGeometry.PolyLine foot;
				LineGeometry.GroupingNode group = new LineGeometry.GroupingNode()
						.add(new LineGeometry.Box(bodyTop-bodyBottom, bodyWidth, bodyWidth).addTranslation((bodyTop+bodyBottom)/2,0,0))
						.add(foot = new LineGeometry.PolyLine().add(0,footDist,0).add((bodyTop+bodyBottom)/2,footDist/3+bodyWidth/3,0).add(bodyBottom, bodyWidth/2, 0))
						.add(new LineGeometry.Transform(foot).addRotation(LineGeometry.Axis.X, 90))
						.add(new LineGeometry.Transform(foot).addRotation(LineGeometry.Axis.X,180))
						.add(new LineGeometry.Transform(foot).addRotation(LineGeometry.Axis.X,270))
						.add(new LineGeometry.Transform(new LineGeometry.Prism(LineGeometry.Axis.Z,bodyWidth,
								new Point3D(bodyTop    ,  bodyWidth/2    , 0),
								new Point3D(bodyTop+0.2,  bodyWidth/2    , 0),
								new Point3D(height     ,  bodyWidth/2-0.2, 0),
								new Point3D(height     , -bodyWidth/2    , 0),
								new Point3D(bodyTop    , -bodyWidth/2    , 0)
						)).addRotation(LineGeometry.Axis.X,-45)
						)
						;
				return group;
			}
			
			private static LineGeometry.IndexedLineSet create_BUILDBED() {
				double height_bed  = 0.65; // X
				double height_matt = 0.10; // X
				double height_head = 1.20; // X
				double width  = 1.35; // Z
				double head_width = 0.2; // Y
				double length = 3.10; // Y : Foot -Y / Head +Y
				
				Point3D pF;
				Point3D pH;
				LineGeometry.GroupingNode group = new LineGeometry.GroupingNode()
					.add(new LineGeometry.Prism(LineGeometry.Axis.Z, width,
						pF=	new Point3D(height_bed     , -length/2     , 0),
							new Point3D(        0.4    , -length/2     , 0),
							new Point3D(        0.2    , -length/2+0.20, 0),
							new Point3D(          0    , -length/2+0.20, 0),
							new Point3D(          0    ,  length/2     , 0),
							new Point3D(height_head    ,  length/2     , 0),
							new Point3D(height_head    ,  length/2-head_width    , 0),
							new Point3D(height_head-0.4,  length/2-head_width-0.4, 0),
						pH=	new Point3D(height_bed     ,  length/2-head_width-0.4, 0)
					));
				
				Point3D pFR = pF.add(0,0,-width/2); Point3D pFRM = pFR.add(height_matt,+0.05,+0.05);
				Point3D pFL = pF.add(0,0,+width/2); Point3D pFLM = pFL.add(height_matt,+0.05,-0.05);
				Point3D pHR = pH.add(0,0,-width/2); Point3D pHRM = pHR.add(height_matt,-0.05,+0.05);
				Point3D pHL = pH.add(0,0,+width/2); Point3D pHLM = pHL.add(height_matt,-0.05,-0.05);
				group.add(
					new LineGeometry.PolyLine(pFRM,pHRM,pHLM,pFLM).close(),
					new LineGeometry.PolyLine(pFR,pFRM),
					new LineGeometry.PolyLine(pFL,pFLM),
					new LineGeometry.PolyLine(pHR,pHRM),
					new LineGeometry.PolyLine(pHL,pHLM)
				);
				
				return group;
			}

			private static LineGeometry.IndexedLineSet create_BUILDCHAIR() {
				double height_seat = 0.5; // X
				double height_back = 1.1; // X
				double diam_foot   = 0.8; // YZ
				double width_seat  = 0.6; // Z 
				double width_back  = 0.5; // Z 
				
				LineGeometry.GroupingNode group = new LineGeometry.GroupingNode(
						new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(0,0,0), diam_foot/2, 0, 315, false, 7).close(),
						new LineGeometry.PolyLine().add(0,diam_foot/2,0).add(0,-diam_foot/2,0),
						new LineGeometry.PolyLine().add(0,0,diam_foot/2).add(0,0,-diam_foot/2),
						new LineGeometry.PolyLine().add(0,0,0).add(height_seat,0,0),
						new LineGeometry.PolyLine()
							.add(height_seat, width_seat/2, width_seat/2)
							.add(height_seat,-width_seat/2, width_seat/2)
							.add(height_seat,-width_seat/2,-width_seat/2)
							.add(height_seat, width_seat/2,-width_seat/2).close(),
						new LineGeometry.PolyLine()
							.add(height_seat, width_seat/2    , width_seat/2)
							.add(height_back, width_seat/2+0.1, width_back/2)
							.add(height_back, width_seat/2+0.1,-width_back/2)
							.add(height_seat, width_seat/2    ,-width_seat/2)
				);
				return group;
			}

			private static LineGeometry.IndexedLineSet create_BUILDSIMPLEDESK() {
				double height = 0.80; //  X
				double depth  = 1.25; // -Y
				double width  = 3.00; //  Z
				double footPosZ = 0.33; // from left or right edge
				double footPosY1 = 0.16; // from rear edge
				double footPosY2 = 0.53; // from rear edge
				
				LineGeometry.GroupingNode group = new LineGeometry.GroupingNode(
						new LineGeometry.PolyLine()
								.add(height, depth/2, width/2)
								.add(height,-depth/2, width/2)
								.add(height,-depth/2,-width/2)
								.add(height, depth/2,-width/2)
								.close(),
						new LineGeometry.PolyLine()
								.add(height,-depth/2+footPosY1, width/2-footPosZ)
								.add(     0,-depth/2+footPosY1, width/2-footPosZ),
						new LineGeometry.PolyLine()
								.add(height,-depth/2+footPosY2, width/2-footPosZ)
								.add(     0,-depth/2+footPosY2, width/2-footPosZ),
						new LineGeometry.PolyLine()
								.add(0, depth/2, width/2-footPosZ)
								.add(0,-depth/2, width/2-footPosZ),
						new LineGeometry.PolyLine()
								.add(height,-depth/2+footPosY1,-width/2+footPosZ)
								.add(     0,-depth/2+footPosY1,-width/2+footPosZ),
						new LineGeometry.PolyLine()
								.add(height,-depth/2+footPosY2,-width/2+footPosZ)
								.add(     0,-depth/2+footPosY2,-width/2+footPosZ),
						new LineGeometry.PolyLine()
								.add(0, depth/2,-width/2+footPosZ)
								.add(0,-depth/2,-width/2+footPosZ)
				);
				
				return group;
			}

			private static LineGeometry.GroupingNode create_SMALLLIGHT() {
				LineGeometry.PolyLine polyLine;
				LineGeometry.GroupingNode group;
				
				group = new LineGeometry.GroupingNode();
				
				double d = 0.25;
				double r = d/2;
				double rt = r*0.7;
				double h = 0.7;
				double rh = (r-rt)/4+rt;
				double hh = 7*h/8;
				
				polyLine = new LineGeometry.PolyLine();
				polyLine.add(new Point3D( 0 , 0,  r ));
				polyLine.add(new Point3D(h/2, 0,  r ));
				polyLine.add(new Point3D( h , 0,  rt));
				polyLine.add(new Point3D( h , 0, -rt));
				polyLine.add(new Point3D(h/2, 0, -r ));
				polyLine.add(new Point3D( 0 , 0, -r ));
				polyLine.close();
				group.add( polyLine, new LineGeometry.Transform(polyLine).addRotation(LineGeometry.Axis.X,90) );
				
				polyLine = new LineGeometry.PolyLine();
				polyLine.add(new Point3D(hh  , 0,  rh));
				polyLine.add(new Point3D(h+rt, 0,  rh));
				polyLine.add(new Point3D(h+rt, 0, -rh));
				polyLine.add(new Point3D(hh  , 0, -rh));
				//polyLine.close();
				group.add( polyLine );
				
				group.add(
					new LineGeometry.Circle(LineGeometry.Axis.X, new Point3D( 0 , 0, 0), r ),
					new LineGeometry.Circle(LineGeometry.Axis.X, new Point3D(h/2, 0, 0), r ),
					new LineGeometry.Circle(LineGeometry.Axis.X, new Point3D( h , 0, 0), rt)
				);
				
				return group;
			}

			private static LineGeometry.GroupingNode create_WALLLIGHT() {
				LineGeometry.PolyLine polyLine;
				LineGeometry.GroupingNode group;
				
				group = new LineGeometry.GroupingNode();
				
				double width  = 0.3;
				double length = 0.57;
				double radius = width/2;
				
				polyLine = new LineGeometry.PolyLine();
				polyLine.addArc(LineGeometry.Axis.X, new Point3D(0,   length/2 - radius , 0), radius, -90,  90, false);
				polyLine.addArc(LineGeometry.Axis.X, new Point3D(0, -(length/2 - radius), 0), radius,  90, 270, false);
				polyLine.close();
				group.add(polyLine);
				
				polyLine = new LineGeometry.PolyLine(); polyLine.addArc(LineGeometry.Axis.Y, new Point3D(0,   length/2 - radius , 0), radius, 0, 180, false); group.add(polyLine);
				polyLine = new LineGeometry.PolyLine(); polyLine.addArc(LineGeometry.Axis.Y, new Point3D(0, -(length/2 - radius), 0), radius, 0, 180, false); group.add(polyLine);
				polyLine = new LineGeometry.PolyLine(); polyLine.addArc(LineGeometry.Axis.Y, new Point3D(0,            0        , 0), radius, 0, 180, false); group.add(polyLine);
				
				polyLine = new LineGeometry.PolyLine();
				polyLine.addArc(LineGeometry.Axis.Z, new Point3D(0, -(length/2 - radius), 0), radius, -90,   0, false);
				polyLine.addArc(LineGeometry.Axis.Z, new Point3D(0,   length/2 - radius , 0), radius,   0,  90, false);
				//polyLine.close();
				group.add(polyLine);
				
				return group;
			}

			private static LineGeometry.GroupingNode create_BUILDLANDINGPAD() {
				LineGeometry.PolyLine polyLine;
				LineGeometry.GroupingNode group;
				
				double width = 24;
				double width_side = 16;
				double width_square1 = 19.6;
				double width_square2 = 14.6;
				double width_circ = 13;
				double height1 = 0.95;
				double height2 = 1.20;
				double ext_side = 0.8;
				double ext_square1 = 0.4;
				
				group = new LineGeometry.GroupingNode();
				group.add(
					new LineGeometry.Transform(
						new LineGeometry.Box( height1/2, width, width )
					).addTranslation(new Point3D(height1/4,0,0))
				);
				
				polyLine = new LineGeometry.PolyLine();
				polyLine.add(new Point3D(   0     , width/2 + ext_side, -width_side/2));
				polyLine.add(new Point3D(   0     , width/2           , -width_side/2));
				polyLine.add(new Point3D(height1/2, width/2           , -width_side/2));
				polyLine.add(new Point3D(   0     , width/2 + ext_side, -width_side/2));
				polyLine.add(new Point3D(   0     , width/2 + ext_side,  width_side/2));
				polyLine.add(new Point3D(height1/2, width/2           ,  width_side/2));
				polyLine.add(new Point3D(   0     , width/2           ,  width_side/2));
				polyLine.add(new Point3D(   0     , width/2 + ext_side,  width_side/2));
				group.add(
					polyLine,
					new LineGeometry.Transform(polyLine).addRotation(LineGeometry.Axis.X,90),
					new LineGeometry.Transform(polyLine).addRotation(LineGeometry.Axis.X,180),
					new LineGeometry.Transform(polyLine).addRotation(LineGeometry.Axis.X,270)
				);
				
				polyLine = new LineGeometry.PolyLine();
				polyLine.add(new Point3D( height1/2, width/2         ,  width/2         ));
				polyLine.add(new Point3D( height1  , width/2-ext_side,  width/2-ext_side));
				polyLine.add(new Point3D( height1  , width/2-ext_side, -width/2+ext_side));
				group.add(
					polyLine,
					new LineGeometry.Transform(polyLine).addRotation(LineGeometry.Axis.X,90),
					new LineGeometry.Transform(polyLine).addRotation(LineGeometry.Axis.X,180),
					new LineGeometry.Transform(polyLine).addRotation(LineGeometry.Axis.X,270)
				);
				
				polyLine = new LineGeometry.PolyLine();
				polyLine.add(new Point3D( height1, width_square1/2+ext_square1,  width_square1/2+ext_square1));
				polyLine.add(new Point3D( height1, width_square1/2+ext_square1, -width_square1/2-ext_square1));
				polyLine.add(new Point3D( height2, width_square1/2            , -width_square1/2            ));
				polyLine.add(new Point3D( height2, width_square1/2            ,  width_square1/2            ));
				group.add(
					polyLine,
					new LineGeometry.Transform(polyLine).addRotation(LineGeometry.Axis.X,90),
					new LineGeometry.Transform(polyLine).addRotation(LineGeometry.Axis.X,180),
					new LineGeometry.Transform(polyLine).addRotation(LineGeometry.Axis.X,270)
				);
				
				polyLine = new LineGeometry.PolyLine();
				polyLine.add(new Point3D( height2,  width_square2/2,  width_square2/2));
				polyLine.add(new Point3D( height2,  width_square2/2, -width_square2/2));
				polyLine.add(new Point3D( height2, -width_square2/2, -width_square2/2));
				polyLine.add(new Point3D( height2, -width_square2/2,  width_square2/2));
				polyLine.close();
				group.add(
					polyLine,
					new LineGeometry.Circle(LineGeometry.Axis.X, new Point3D(height2, 0, 0), width_circ/2)
				);
				
				polyLine = new LineGeometry.PolyLine();
				polyLine.add(new Point3D( height2    ,  width_square1/2-1, -width_square2/2+0.5));
				polyLine.add(new Point3D( height2+1.5,  width_square1/2-1, -width_square2/2+0.5));
				group.add( polyLine );
				
				polyLine = new LineGeometry.PolyLine();
				polyLine.add(new Point3D( height2+1.5+0.3,  width_square1/2-1-0.7, -width_square2/2+0.5+1));
				polyLine.add(new Point3D( height2+1.5-0.3,  width_square1/2-1+0.7, -width_square2/2+0.5+1));
				polyLine.add(new Point3D( height2+1.5-0.3,  width_square1/2-1+0.7, -width_square2/2+0.5-1));
				polyLine.add(new Point3D( height2+1.5+0.3,  width_square1/2-1-0.7, -width_square2/2+0.5-1));
				polyLine.close();
				group.add(polyLine);
				
				return group;
			}

			@SuppressWarnings("unused")
			private static LineGeometry.GroupingNode create_CUBEROOM_SPACE() {
				//LineGeometry.Box box = new LineGeometry.Box(3.8,8,8);
				//box.addTranslation(new Point3D(1.9,0,0));
				//box.write(vrml, "	", null);
				
				LineGeometry.PolyLine polyLine;
				LineGeometry.GroupingNode group;
				
				polyLine = new LineGeometry.PolyLine();
				polyLine.addArc(LineGeometry.Axis.X, new Point3D(0, 3, 3), 1,  0, 90, false);
				polyLine.addArc(LineGeometry.Axis.X, new Point3D(0,-3, 3), 1, 90,180, false);
				polyLine.addArc(LineGeometry.Axis.X, new Point3D(0,-3,-3), 1,180,270, false);
				polyLine.addArc(LineGeometry.Axis.X, new Point3D(0, 3,-3), 1,270,360, false);
				polyLine.close();
				
				group = new LineGeometry.GroupingNode();
				group.add(
					polyLine,
					new LineGeometry.Transform(polyLine).addTranslation(new Point3D(3,0,0))
				);
				//group.write(vrml, "	", null);
				
				double w = 6;
				double h = 3.8;
				
				double cr  = 0.6;
				double at1 = 45;
				
				double ht1 = 0.5;
				double wt1 = ht1/Math.tan(at1/180*Math.PI);;
				double wt2 = 1.0;
				double wb2 = 1.0;
				
				double h1 = h-ht1;
				double w1 = w-2*wb2;
				
				group = new LineGeometry.GroupingNode();
				
				polyLine = new LineGeometry.PolyLine();
				polyLine.addArc(LineGeometry.Axis.Y, new Point3D(   cr  ,0,  w/2-cr    ),   cr,270,360, false);
				polyLine.addArc(LineGeometry.Axis.Y, new Point3D(h1-cr  ,0,  w/2-cr    ),   cr,  0, 90, false);
				polyLine.addArc(LineGeometry.Axis.Y, new Point3D(h1-cr  ,0,-(w/2-cr   )),   cr, 90,180, false);
				polyLine.addArc(LineGeometry.Axis.Y, new Point3D(   cr  ,0,-(w/2-cr   )),   cr,180,270, false);
				polyLine.close();
				group.add(
					new LineGeometry.Transform(polyLine).addTranslation(new Point3D(0,-2,0)),
					new LineGeometry.Transform(polyLine).addTranslation(new Point3D(0, 2,0)),
					new LineGeometry.Transform(polyLine).addRotation(LineGeometry.Axis.X,90).addTranslation(new Point3D(0,0,-2)),
					new LineGeometry.Transform(polyLine).addRotation(LineGeometry.Axis.X,90).addTranslation(new Point3D(0,0, 2))
				);
				
				polyLine = new LineGeometry.PolyLine();
				polyLine.addArc(LineGeometry.Axis.X, new Point3D(0, w1/2, w1/2), wb2,  0, 90, false);
				polyLine.addArc(LineGeometry.Axis.X, new Point3D(0,-w1/2, w1/2), wb2, 90,180, false);
				polyLine.addArc(LineGeometry.Axis.X, new Point3D(0,-w1/2,-w1/2), wb2,180,270, false);
				polyLine.addArc(LineGeometry.Axis.X, new Point3D(0, w1/2,-w1/2), wb2,270,360, false);
				polyLine.close();
				group.add(
					new LineGeometry.Transform(polyLine).addTranslation(new Point3D(cr,0,0)),
					new LineGeometry.Transform(polyLine).addTranslation(new Point3D(h1-cr,0,0))
				);
				
				polyLine = new LineGeometry.PolyLine();
				polyLine.addArc(LineGeometry.Axis.X, new Point3D(0, w1/2, w1/2), wb2-cr,  0, 90, false);
				polyLine.addArc(LineGeometry.Axis.X, new Point3D(0,-w1/2, w1/2), wb2-cr, 90,180, false);
				polyLine.addArc(LineGeometry.Axis.X, new Point3D(0,-w1/2,-w1/2), wb2-cr,180,270, false);
				polyLine.addArc(LineGeometry.Axis.X, new Point3D(0, w1/2,-w1/2), wb2-cr,270,360, false);
				polyLine.close();
				group.add(
					polyLine,
					new LineGeometry.Transform(polyLine).addTranslation(new Point3D(h1,0,0))
				);
				
				polyLine = new LineGeometry.PolyLine();
				polyLine.addArc(LineGeometry.Axis.Y, new Point3D(   cr  ,0,  w/2-cr    ),   cr,270,360, false);
				polyLine.addArc(LineGeometry.Axis.Y, new Point3D(h1-cr  ,0,  w/2-cr    ),   cr,  0, 90, false);
				polyLine.add(new Point3D(h1    , 0,  w/2-wt2     ));
				polyLine.add(new Point3D(h1+ht1, 0,  w/2-wt2-wt1 ));
				polyLine.add(new Point3D(h1+ht1, 0,-(w/2-wt2-wt1)));
				polyLine.add(new Point3D(h1    , 0,-(w/2-wt2    )));
				polyLine.addArc(LineGeometry.Axis.Y, new Point3D(h1-cr  ,0,-(w/2-cr   )),   cr, 90,180, false);
				polyLine.addArc(LineGeometry.Axis.Y, new Point3D(   cr  ,0,-(w/2-cr   )),   cr,180,270, false);
				polyLine.close();
				group.add(
					polyLine,
					new LineGeometry.Transform(polyLine).addRotation(LineGeometry.Axis.X,90)
				);
				
				polyLine = new LineGeometry.PolyLine();
				polyLine.add(new Point3D(h,  w/2-wt2-wt1 ,  w/2-wt2-wt1 ));
				polyLine.add(new Point3D(h,-(w/2-wt2-wt1),  w/2-wt2-wt1 ));
				polyLine.add(new Point3D(h,-(w/2-wt2-wt1),-(w/2-wt2-wt1)));
				polyLine.add(new Point3D(h,  w/2-wt2-wt1 ,-(w/2-wt2-wt1)));
				polyLine.close();
				group.add(
					polyLine,
					new LineGeometry.PolyLine(new Point3D(h,  w/2-wt2-wt1 ,  w/2-wt2-wt1 ),new Point3D(h1,  w/2-wt2 ,  w/2-wt2 )),
					new LineGeometry.PolyLine(new Point3D(h,-(w/2-wt2-wt1),  w/2-wt2-wt1 ),new Point3D(h1,-(w/2-wt2),  w/2-wt2 )),
					new LineGeometry.PolyLine(new Point3D(h,-(w/2-wt2-wt1),-(w/2-wt2-wt1)),new Point3D(h1,-(w/2-wt2),-(w/2-wt2))),
					new LineGeometry.PolyLine(new Point3D(h,  w/2-wt2-wt1 ,-(w/2-wt2-wt1)),new Point3D(h1,  w/2-wt2 ,-(w/2-wt2)))
				);
				
				return group;
			}

			private static LineGeometry.Transform createXFloorBasedBox(double sizeX, double sizeY, double sizeZ) {
				return new LineGeometry.Box(sizeX,sizeY,sizeZ)
						.addTranslation(new Point3D(sizeX/2,0,0));
			}

			private static class PoweredDevices {
				
				static void addModelsToModelMap() {
					addModels("SOLARPANEL"   , "^U_SOLAR_S"     );
					addModels("BATTERY"      , "^U_BATTERY_S"   );
					addModels("BIO_GENERATOR", "^U_BIOGENERATOR");
					addModels("EM_GENERATOR" , "^U_GENERATOR_S" );
					
					addModels("PLANTER"      , "^PLANTER"      );
					addModels("PLANTERMEGA"  , "^PLANTERMEGA"  );
					addModels("CARBONPLANTER", "^CARBONPLANTER");
					
					addProto( new Proto("SOLARPANEL"   , 0.5,   0, create_SOLARPANEL   ()) );
					addProto( new Proto("BATTERY"      , 0.5,   0, create_BATTERY      ()) );
					addProto( new Proto("BIO_GENERATOR", 0.5,   0, create_BIO_GENERATOR()) );
					addProto( new Proto("EM_GENERATOR" , 0.5,   0, create_EM_GENERATOR ()) );
					
					addProto( new Proto("PLANTER"      , 0.5,  45,     create_PLANTER    ()) );
					addProto( new Proto("PLANTERMEGA"  , 0.5,  45, 25, create_PLANTERMEGA()) );
					addProto( new Proto("CARBONPLANTER", 0.5, 180, new Point3D(0,-0.73,0), create_CARBONPLANTER()) );
				}
				
				private static LineGeometry.GroupingNode create_PLANTER() {
					double raster = 2;
					double spacing = 0.05;
					double width = raster-spacing;
					double height = 2.5; // X
					double height1 = 0.6; // X
					double cornerR = 0.1;
					
					double border = 0.1;
					double borderR = 0.05;
					
					double plugDist = 2.53; // Y|Z
					double plugHeight = 0.32; // X
					
					// Backside: -Y
					
					LineGeometry.PolyLine p0,p1;
					LineGeometry.GroupingNode body = new LineGeometry.GroupingNode()
							.add(p0 = new LineGeometry.PolyLine()
									.addArc(LineGeometry.Axis.X, new Point3D(0, width/2-cornerR, width/2-cornerR), cornerR,   0,  90, false, 4)
									.addArc(LineGeometry.Axis.X, new Point3D(0,-width/2+cornerR, width/2-cornerR), cornerR,  90, 180, false, 4)
									.addArc(LineGeometry.Axis.X, new Point3D(0,-width/2+cornerR,-width/2+cornerR), cornerR, 180, 270, false, 4)
									.addArc(LineGeometry.Axis.X, new Point3D(0, width/2-cornerR,-width/2+cornerR), cornerR, 270, 360, false, 4)
									.close()
							)
							.add(p1 = p0.getCopy(height1, 0, 0))
							
							.add(new LineGeometry.PolyLine()
									.addArc(LineGeometry.Axis.X, new Point3D(height1, width/2-border-borderR, width/2-border-borderR), borderR,   0,  90, false, 4)
									.addArc(LineGeometry.Axis.X, new Point3D(height1,-width/2+border+borderR, width/2-border-borderR), borderR,  90, 180, false, 4)
									.addArc(LineGeometry.Axis.X, new Point3D(height1,-width/2+border+borderR,-width/2+border+borderR), borderR, 180, 270, false, 4)
									.addArc(LineGeometry.Axis.X, new Point3D(height1, width/2-border-borderR,-width/2+border+borderR), borderR, 270, 360, false, 4)
									.close()
							)
							.add(new LineGeometry.PolyLine()
									.add(height,-width/2+0.5   ,-0.1)
									.add(height,-width/2+border,-0.1)
									.addArc(LineGeometry.Axis.X, new Point3D(height,-width/2+border+borderR,-width/2+border+borderR), borderR, 180, 270, false, 4)
									.addArc(LineGeometry.Axis.X, new Point3D(height, width/2-border-borderR,-width/2+border+borderR), borderR, 270, 360, false, 4)
									.addArc(LineGeometry.Axis.X, new Point3D(height, width/2-border-borderR, width/2-border-borderR), borderR,   0,  90, false, 4)
									.addArc(LineGeometry.Axis.X, new Point3D(height,-width/2+border+borderR, width/2-border-borderR), borderR,  90, 180, false, 4)
									.add(height,-width/2+border, 0.1)
									.add(height,-width/2+0.5   , 0.1)
									.close()
							)
							
							.add(new LineGeometry.PolyLine()
									.add(height1    ,-width/2    ,0)
									.add(height1+0.3,-width/2+0.1,0)
									.add(height -0.2,-width/2+0.1,0)
									.add(height     ,-width/2+0.3,0)
									.add(height     ,-width/2+0.5,0)
							)
							
							.add(new LineGeometry.PolyLine().add(p0.get( 0)).add(p1.get( 0)))
							.add(new LineGeometry.PolyLine().add(p0.get( 4)).add(p1.get( 4)))
							.add(new LineGeometry.PolyLine().add(p0.get( 5)).add(p1.get( 5)))
							.add(new LineGeometry.PolyLine().add(p0.get( 9)).add(p1.get( 9)))
							.add(new LineGeometry.PolyLine().add(p0.get(10)).add(p1.get(10)))
							.add(new LineGeometry.PolyLine().add(p0.get(14)).add(p1.get(14)))
							.add(new LineGeometry.PolyLine().add(p0.get(15)).add(p1.get(15)))
							.add(new LineGeometry.PolyLine().add(p0.get(19)).add(p1.get(19)))
							
							.add(new LineGeometry.PolyLine().add(0,0, width/2).add(plugHeight,0, plugDist/2))
							.add(new LineGeometry.PolyLine().add(0,0,-width/2).add(plugHeight,0,-plugDist/2))
							.add(new LineGeometry.PolyLine().add(0, width/2,0).add(plugHeight, plugDist/2,0))
							.add(new LineGeometry.PolyLine().add(0,-width/2,0).add(plugHeight,-plugDist/2,0))
							;
					return body;
				}
				
				private static LineGeometry.GroupingNode create_PLANTERMEGA() {
					double raster = 4;
					double spacing = 0.05;
					double width = raster-spacing;
					double height = 2.5; // X
					double height1 = 0.6; // X
					double cornerR = 0.1;
					
					double border = 0.1;
					double borderR = 0.05;
					
					double plugDist = 4.54; // Y|Z
					double plugHeight = 0.32; // X
					
					LineGeometry.PolyLine p3 = new LineGeometry.PolyLine()
							.add(height1    ,border    ,0)
							.add(height1+0.3,border+0.1,0)
							.add(height -0.2,border+0.1,0)
							.add(height     ,border+0.3,0)
							.add(height     ,border+0.5,0)
							;
					double p3Ydiag = (border+0.5)/Math.sqrt(2);
					
					LineGeometry.PolyLine p0,p1,p2;
					LineGeometry.GroupingNode body = new LineGeometry.GroupingNode()
							.add(p0 = new LineGeometry.PolyLine()
									.addArc(LineGeometry.Axis.X, new Point3D(0, width/2-cornerR, width/2-cornerR), cornerR,  0, 90, false, 4)
									.addArc(LineGeometry.Axis.X, new Point3D(0,-width/2+cornerR, width/2-cornerR), cornerR, 90,180, false, 4)
									.addArc(LineGeometry.Axis.X, new Point3D(0,-width/2+cornerR,-width/2+cornerR), cornerR,180,270, false, 4)
									.addArc(LineGeometry.Axis.X, new Point3D(0, width/2-cornerR,-width/2+cornerR), cornerR,270,360, false, 4)
									.close()
							)
							.add(p1 = p0.getCopy(height1, 0, 0))
							
							.add(p2 = new LineGeometry.PolyLine()
									.addArc(LineGeometry.Axis.X, new Point3D(height1, width/2-border-borderR, width/2-border-borderR), borderR,   0,  90, false, 4)
									.addArc(LineGeometry.Axis.X, new Point3D(height1,         border+borderR, width/2-border-borderR), borderR,  90, 180, false, 4)
									.addArc(LineGeometry.Axis.X, new Point3D(height1,         border+borderR,         border+borderR), borderR, 180, 270, false, 4)
									.addArc(LineGeometry.Axis.X, new Point3D(height1, width/2-border-borderR,         border+borderR), borderR, 270, 360, false, 4)
									.close()
							)
							.add(p2.getCopy(0, -width/2,     0   ))
							.add(p2.getCopy(0, -width/2, -width/2))
							.add(p2.getCopy(0,     0   , -width/2))
							
							.add(new LineGeometry.PolyLine()
									.addArc(LineGeometry.Axis.X, new Point3D(height1, 0, 0), border, 0, 360, false, 16)
							)
							.add(new LineGeometry.Transform(p3).addRotation(LineGeometry.Axis.X,  45))
							.add(new LineGeometry.Transform(p3).addRotation(LineGeometry.Axis.X, 135))
							.add(new LineGeometry.Transform(p3).addRotation(LineGeometry.Axis.X, 225))
							.add(new LineGeometry.Transform(p3).addRotation(LineGeometry.Axis.X, 315))
							
							.add(new LineGeometry.PolyLine()
									.addArc(LineGeometry.Axis.X, new Point3D(height, width/2-border-borderR, width/2-border-borderR), borderR,   0,  90, false, 4)
									.addArc(LineGeometry.Axis.X, new Point3D(height,-width/2+border+borderR, width/2-border-borderR), borderR,  90, 180, false, 4)
									.addArc(LineGeometry.Axis.X, new Point3D(height,-width/2+border+borderR,-width/2+border+borderR), borderR, 180, 270, false, 4)
									.addArc(LineGeometry.Axis.X, new Point3D(height, width/2-border-borderR,-width/2+border+borderR), borderR, 270, 360, false, 4)
									.close()
							)
							.add(new LineGeometry.PolyLine()
									.add(height, p3Ydiag, p3Ydiag)
									.add(height,-p3Ydiag, p3Ydiag)
									.add(height,-p3Ydiag,-p3Ydiag)
									.add(height, p3Ydiag,-p3Ydiag)
									.close()
							)
							.add(new LineGeometry.PolyLine().add(height, width/2-border, 0).add(height, -width/2+border, 0))
							.add(new LineGeometry.PolyLine().add(height, 0, width/2-border).add(height, 0, -width/2+border))
							
							.add(new LineGeometry.PolyLine().add(p0.get( 0)).add(p1.get( 0)))
							.add(new LineGeometry.PolyLine().add(p0.get( 4)).add(p1.get( 4)))
							.add(new LineGeometry.PolyLine().add(p0.get( 5)).add(p1.get( 5)))
							.add(new LineGeometry.PolyLine().add(p0.get( 9)).add(p1.get( 9)))
							.add(new LineGeometry.PolyLine().add(p0.get(10)).add(p1.get(10)))
							.add(new LineGeometry.PolyLine().add(p0.get(14)).add(p1.get(14)))
							.add(new LineGeometry.PolyLine().add(p0.get(15)).add(p1.get(15)))
							.add(new LineGeometry.PolyLine().add(p0.get(19)).add(p1.get(19)))
							
							.add(new LineGeometry.PolyLine().add(height1,0, width/2).add(0,0, width/2).add(plugHeight,0, plugDist/2))
							.add(new LineGeometry.PolyLine().add(height1,0,-width/2).add(0,0,-width/2).add(plugHeight,0,-plugDist/2))
							.add(new LineGeometry.PolyLine().add(height1, width/2,0).add(0, width/2,0).add(plugHeight, plugDist/2,0))
							.add(new LineGeometry.PolyLine().add(height1,-width/2,0).add(0,-width/2,0).add(plugHeight,-plugDist/2,0))
							;
					return body;
				}
				
				private static LineGeometry.IndexedLineSet create_CARBONPLANTER() {
					// backside: -Y
					// height: X
					double height = 2.6; // old: 2.8;
					double width = 1.8; // Z
					double backplanePosY = -1;
					double frontPosY  = backplanePosY + height/717.0*233.0;
					double frontPosX  = height        - height/717.0*634.0;
					double bottomPosY = backplanePosY + height/717.0*173.0;
					double topPosY    = backplanePosY + height/717.0* 58.0;
					double frontPosY1  = backplanePosY + height/717.0*205.0;
					double frontPosX1  = frontPosX;
					double bottomPosY1 = backplanePosY + height/717.0*161.0;
					double topPosY1    = backplanePosY + height/717.0* 39.0;
					double height1     =                 height/717.0*673.0;
					double offsetX1    = height/2-height1/2;
					double borderZ     = width/400.0*12;
					
					double plugDistXY = 0.2274; // sqrt(y�+x�)
					double plugHeight = 0.226-0.01; // X
					double plugPosY = Math.sqrt(plugDistXY*plugDistXY - plugHeight*plugHeight); // Y
					
					LineGeometry.PolyLine pN,pP,pN1,pP1;
					LineGeometry.GroupingNode body = new LineGeometry.GroupingNode()
							.add(pN = new LineGeometry.PolyLine()
									.add(     0   ,backplanePosY,-width/2+borderZ)
									.add(   height,backplanePosY,-width/2+borderZ)
									.add(   height,      topPosY,-width/2+borderZ)
									.add(frontPosX,    frontPosY,-width/2+borderZ)
									.add(     0   ,   bottomPosY,-width/2+borderZ)
									.close()
							)
							.add(pP = pN.getCopy(0, 0, width-2*borderZ))
							
							.add(pN1 = new LineGeometry.PolyLine()
									.add(offsetX1        ,backplanePosY ,-width/2)
									.add(offsetX1+height1,backplanePosY ,-width/2)
									.add(offsetX1+height1,      topPosY1,-width/2)
									.add(frontPosX1      ,    frontPosY1,-width/2)
									.add(offsetX1        ,   bottomPosY1,-width/2)
									.close()
							)
							.add(pP1 = pN1.getCopy(0, 0, width))
							
							.add(new LineGeometry.PolyLine().add(pN1.get(0)).add(pN.get(0)).add(pP.get(0)).add(pP1.get(0)))
							.add(new LineGeometry.PolyLine().add(pN1.get(1)).add(pN.get(1)).add(pP.get(1)).add(pP1.get(1)))
							.add(new LineGeometry.PolyLine().add(pN1.get(2)).add(pN.get(2)).add(pP.get(2)).add(pP1.get(2)))
							.add(new LineGeometry.PolyLine().add(pN1.get(3)).add(pN.get(3)).add(pP.get(3)).add(pP1.get(3)))
							.add(new LineGeometry.PolyLine().add(pN1.get(4)).add(pN.get(4)).add(pP.get(4)).add(pP1.get(4)))
							
							.add(new LineGeometry.PolyLine().add(frontPosX,frontPosY,0).add(plugHeight,plugPosY,0).add(0,bottomPosY,0))
							;
					return body;
					
					//StringBuilder coords = new StringBuilder();
					//coords.append("0   -0.2  0.9,");
					//coords.append("0   -0.2 -0.9,");
					//coords.append("0   -1.0 -0.9,");
					//coords.append("0   -1.0  0.9,");
					//coords.append("2.8 -0.8  0.9,");
					//coords.append("2.8 -0.8 -0.9,");
					//coords.append("2.8 -1.0 -0.9,");
					//coords.append("2.8 -1.0  0.9");
					//
					//String indexes = "0 1 2 3 0 -1 0 4 7 3 -1 1 5 6 2 -1 4 5 -1 6 7";
					//
					//LineGeometry.IndexedLineSet lineSet = LineGeometry.IndexedLineSet.parse(coords.toString(),indexes, "CARBONPLANTER");
					//if (lineSet==null) lineSet = new LineGeometry.PolyLine();
					//return lineSet;
				}
			
				private static LineGeometry.GroupingNode create_EM_GENERATOR() {
					double height = 7; // X
					double plugDist = 2.35; // Y|Z
					double plugHeight = 0.32; // X
					double feetDist   = 2.6;
					double bodyBottom = 0.15;
					double ring0Radius = 1.00; double ring0H = bodyBottom;
					double ring1Radius = 0.95; double ring1H = plugHeight*2.5;
					double plugHeightA = plugHeight-0.05;
					double plugHeightB = plugHeight+0.05;
					double plugRadiusA = (plugHeightA-ring0H)/(ring1H-ring0H)*(ring1Radius-ring0Radius)+ring0Radius;
					double plugRadiusB = (plugHeightB-ring0H)/(ring1H-ring0H)*(ring1Radius-ring0Radius)+ring0Radius;
					
					LineGeometry.PolyLine poly = new LineGeometry.PolyLine()
							.add(ring1H     ,ring1Radius     ,0)
							.add(ring1H+0.80,ring1Radius+0.80,0)
							.add(ring1H+0.80,ring1Radius+0.60,0)
							.add(ring1H+0.95,ring1Radius+0.60,0)
							.add(ring1H+1.08,ring1Radius+0.53,0)
							.add(ring1H+1.15,ring1Radius+0.40,0)
							.add(ring1H+1.15,ring1Radius+0.10,0)
							.add(2.80,0.60,0)
							.add(3.50,0.60,0)
							.add(3.70,0.80,0)
							.add(4.00,0.50,0)
							.add(height-0.15,0.50,0)
							.add(height     ,0.20,0)
							.add(2.60,0.20,0)
							.add(2.00,0.50,0)
							.add(1.30,0.50,0)
							.add(1.10,0.30,0)
							.add(ring0H,0.30,0)
							;
					
					LineGeometry.PolyLine c0,c1;
					LineGeometry.GroupingNode body = new LineGeometry.GroupingNode()
							.add(c0 = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(ring0H,0,0), ring0Radius, 0, 360, false, 16))
							.add(c1 = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(ring1H,0,0), ring1Radius, 0, 360, false, 16))
							.add(new LineGeometry.PolyLine().add(c0.get( 0)).add(plugHeightA, plugRadiusA,0).add(plugHeight, plugDist/2,0).add(plugHeightB, plugRadiusB,0).add(c1.get( 0)))
							.add(new LineGeometry.PolyLine().add(c0.get( 4)).add(plugHeightA,0, plugRadiusA).add(plugHeight,0, plugDist/2).add(plugHeightB,0, plugRadiusB).add(c1.get( 4)))
							.add(new LineGeometry.PolyLine().add(c0.get( 8)).add(plugHeightA,-plugRadiusA,0).add(plugHeight,-plugDist/2,0).add(plugHeightB,-plugRadiusB,0).add(c1.get( 8)))
							.add(new LineGeometry.PolyLine().add(c0.get(12)).add(plugHeightA,0,-plugRadiusA).add(plugHeight,0,-plugDist/2).add(plugHeightB,0,-plugRadiusB).add(c1.get(12)))
							.add(new LineGeometry.PolyLine().add(c0.get( 2)).add(0, feetDist/2, feetDist/2).add(c1.get( 2)).close())
							.add(new LineGeometry.PolyLine().add(c0.get( 6)).add(0,-feetDist/2, feetDist/2).add(c1.get( 6)).close())
							.add(new LineGeometry.PolyLine().add(c0.get(10)).add(0,-feetDist/2,-feetDist/2).add(c1.get(10)).close())
							.add(new LineGeometry.PolyLine().add(c0.get(14)).add(0, feetDist/2,-feetDist/2).add(c1.get(14)).close())
							.add(new LineGeometry.Transform(poly).addRotation(LineGeometry.Axis.X, 45))
							.add(new LineGeometry.Transform(poly).addRotation(LineGeometry.Axis.X,135))
							.add(new LineGeometry.Transform(poly).addRotation(LineGeometry.Axis.X,225))
							.add(new LineGeometry.Transform(poly).addRotation(LineGeometry.Axis.X,315))
							;
					
					return body;
				}
			
				private static LineGeometry.GroupingNode create_BIO_GENERATOR() {
					double height = 1.3; // X
					double plugDist = 1.58; // Y|Z
					double plugHeight = 0.33; // X
					double bodyBottom = 0.15;
					//double barrelAxisLength = 1.18;
					double barrelDiam = 1;
					double barrelCentH = height-barrelDiam/2;
					double barrelRad0 = barrelDiam/2   ; double barrelPos0 = 0; 
					double barrelRad1 = barrelRad0-0.04; double barrelPos1 = 0.18;
					double barrelRad2 = barrelRad0-0.14; double barrelPos2 = 0.30;
					double barrelRad3 = barrelRad0-0.16; double barrelPos3 = 0.36;
					double barrelRad4 = barrelRad0-0.27; double barrelPos4 = 0.38;
					double barrelRad5 = barrelRad0-0.29; double barrelPos5 = 0.44;
					double barrelRad6 = barrelRad0-0.40; double barrelPos6 = 0.46;
					double barrelRad7 = barrelRad0-0.40; double barrelPos7 = 0.52;
					
					// plugs: +Y/-Y
					// barrel along Z
					
					LineGeometry.PolyLine c0;
					LineGeometry.PolyLine c1P,c2P,c3P,c4P,c5P,c6P,c7P;
					LineGeometry.PolyLine c1N,c2N,c3N,c4N,c5N,c6N,c7N;
					LineGeometry.GroupingNode body = new LineGeometry.GroupingNode()
							.add(new LineGeometry.PolyLine() // Plugs A
									.add(plugHeight,-plugDist  /2,0)
									.add(plugHeight,-barrelDiam/2,0)
							)
							.add(new LineGeometry.PolyLine() // Plugs B
									.add(plugHeight,+plugDist  /2,0)
									.add(plugHeight,+barrelDiam/2,0)
							)
							.add(c7P = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Z, new Point3D(barrelCentH, 0,  barrelPos7), barrelRad7, -90, 270, false, 24))
							.add(c6P = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Z, new Point3D(barrelCentH, 0,  barrelPos6), barrelRad6, -90, 270, false, 24))
							.add(c5P = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Z, new Point3D(barrelCentH, 0,  barrelPos5), barrelRad5, -90, 270, false, 24))
							.add(c4P = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Z, new Point3D(barrelCentH, 0,  barrelPos4), barrelRad4, -90, 270, false, 24))
							.add(c3P = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Z, new Point3D(barrelCentH, 0,  barrelPos3), barrelRad3, -90, 270, false, 24))
							.add(c2P = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Z, new Point3D(barrelCentH, 0,  barrelPos2), barrelRad2, -90, 270, false, 24))
							.add(c1P = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Z, new Point3D(barrelCentH, 0,  barrelPos1), barrelRad1, -90,  90, false, 12))
							.add(c0  = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Z, new Point3D(barrelCentH, 0,  barrelPos0), barrelRad0, -90,  90, false, 12))
							.add(c1N = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Z, new Point3D(barrelCentH, 0, -barrelPos1), barrelRad1, -90,  90, false, 12))
							.add(c2N = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Z, new Point3D(barrelCentH, 0, -barrelPos2), barrelRad2, -90, 270, false, 24))
							.add(c3N = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Z, new Point3D(barrelCentH, 0, -barrelPos3), barrelRad3, -90, 270, false, 24))
							.add(c4N = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Z, new Point3D(barrelCentH, 0, -barrelPos4), barrelRad4, -90, 270, false, 24))
							.add(c5N = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Z, new Point3D(barrelCentH, 0, -barrelPos5), barrelRad5, -90, 270, false, 24))
							.add(c6N = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Z, new Point3D(barrelCentH, 0, -barrelPos6), barrelRad6, -90, 270, false, 24))
							.add(c7N = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Z, new Point3D(barrelCentH, 0, -barrelPos7), barrelRad7, -90, 270, false, 24))
							.add(new LineGeometry.PolyLine().add(bodyBottom, barrelRad2, barrelPos2).add(barrelCentH, barrelRad2, barrelPos2))
							.add(new LineGeometry.PolyLine().add(bodyBottom, barrelRad1, barrelPos1).add(barrelCentH, barrelRad1, barrelPos1))
							.add(new LineGeometry.PolyLine().add(bodyBottom, barrelRad0, barrelPos0).add(barrelCentH, barrelRad0, barrelPos0))
							.add(new LineGeometry.PolyLine().add(bodyBottom, barrelRad1,-barrelPos1).add(barrelCentH, barrelRad1,-barrelPos1))
							.add(new LineGeometry.PolyLine().add(bodyBottom, barrelRad2,-barrelPos2).add(barrelCentH, barrelRad2,-barrelPos2))
							.add(new LineGeometry.PolyLine().add(bodyBottom,-barrelRad2, barrelPos2).add(barrelCentH,-barrelRad2, barrelPos2))
							.add(new LineGeometry.PolyLine().add(bodyBottom,-barrelRad1, barrelPos1).add(barrelCentH,-barrelRad1, barrelPos1))
							.add(new LineGeometry.PolyLine().add(bodyBottom,-barrelRad0, barrelPos0).add(barrelCentH,-barrelRad0, barrelPos0))
							.add(new LineGeometry.PolyLine().add(bodyBottom,-barrelRad1,-barrelPos1).add(barrelCentH,-barrelRad1,-barrelPos1))
							.add(new LineGeometry.PolyLine().add(bodyBottom,-barrelRad2,-barrelPos2).add(barrelCentH,-barrelRad2,-barrelPos2))
							
							.add(new LineGeometry.PolyLine()
									.add(bodyBottom, barrelRad2, barrelPos2)
									.add(bodyBottom, barrelRad1, barrelPos1)
									.add(bodyBottom, barrelRad0, barrelPos0)
									.add(bodyBottom, barrelRad1,-barrelPos1)
									.add(bodyBottom, barrelRad2,-barrelPos2)
									.add(bodyBottom,-barrelRad2,-barrelPos2)
									.add(bodyBottom,-barrelRad1,-barrelPos1)
									.add(bodyBottom,-barrelRad0, barrelPos0)
									.add(bodyBottom,-barrelRad1, barrelPos1)
									.add(bodyBottom,-barrelRad2, barrelPos2)
									.close()
							)
							
							.add(new LineGeometry.PolyLine().add(0,-barrelDiam/2,-barrelDiam/2).add(bodyBottom,-barrelRad2,-barrelPos2)) // Foot NN
							.add(new LineGeometry.PolyLine().add(0,+barrelDiam/2,+barrelDiam/2).add(bodyBottom, barrelRad2, barrelPos2)) // Foot PP
							.add(new LineGeometry.PolyLine().add(0,+barrelDiam/2,-barrelDiam/2).add(bodyBottom, barrelRad2,-barrelPos2)) // Foot PN
							.add(new LineGeometry.PolyLine().add(0,-barrelDiam/2,+barrelDiam/2).add(bodyBottom,-barrelRad2, barrelPos2)) // Foot NP
							;
					for (int i=0; i<7; i++)
						body.add(
							new LineGeometry.PolyLine(
								c7P.get(i*2),
								c6P.get(i*2),
								c5P.get(i*2),
								c4P.get(i*2),
								c3P.get(i*2),
								c2P.get(i*2),
								c1P.get(i*2),
								c0 .get(i*2),
								c1N.get(i*2),
								c2N.get(i*2),
								c3N.get(i*2),
								c4N.get(i*2),
								c5N.get(i*2),
								c6N.get(i*2),
								c7N.get(i*2)
							)
						);
					for (int i=7; i<12; i++) {
						body.add(
							new LineGeometry.PolyLine(
								c7P.get(i*2),
								c6P.get(i*2),
								c5P.get(i*2),
								c4P.get(i*2),
								c3P.get(i*2),
								c2P.get(i*2)
							)
						);
						body.add(
							new LineGeometry.PolyLine(
								c2N.get(i*2),
								c3N.get(i*2),
								c4N.get(i*2),
								c5N.get(i*2),
								c6N.get(i*2),
								c7N.get(i*2)
							)
						);
					}
					
					return body;
				}
			
				private static LineGeometry.GroupingNode create_BATTERY() {
					double height = 1.75; // X
					double plugDist = 1.55; // Y|Z
					double plugHeight = 0.25; // X
					double barrelDiam = 1.3;
					double barrelHoleDiam = 0.8;
					//double barrelCoreDiam = 0.4;
					double barrelCoreRadius = 0.2;
					double barrelMinH = height-barrelDiam;
					double barrelThick= barrelDiam/2;
					double barrelCentH= barrelDiam/2+barrelMinH;
					
					// battery barrel along Y axis
					
					LineGeometry.PolyLine c0,c1,c2,c3,c4,c5;
					LineGeometry.GroupingNode body = new LineGeometry.GroupingNode()
							.add(new LineGeometry.PolyLine()
									.add(barrelMinH,  barrelThick/2-0.1, 0)
									.add(barrelMinH, -barrelThick/2+0.1, 0)
							)
							.add(new LineGeometry.PolyLine() // Plugs A & C
									.add(plugHeight,0,-plugDist/2)
									.add(plugHeight,0,-plugDist/4)
									.add(barrelMinH,0,     0     )
									.add(plugHeight,0,+plugDist/4)
									.add(plugHeight,0,+plugDist/2)
							)
							.add(new LineGeometry.PolyLine() // Plugs B & D
									.add(plugHeight,-plugDist/2,0)
									.add(plugHeight,-plugDist/4,0)
									.add(barrelMinH,     0     ,0)
									.add(plugHeight,+plugDist/4,0)
									.add(plugHeight,+plugDist/2,0)
							)
							.add(new LineGeometry.PolyLine() // Feet NN & PP
									.add(    0     ,-plugDist/2,-plugDist/2)
									.add(barrelMinH,     0     ,     0     )
									.add(    0     ,+plugDist/2,+plugDist/2)
							)
							.add(new LineGeometry.PolyLine() // Feet PN & NP
									.add(    0     ,+plugDist/2,-plugDist/2)
									.add(barrelMinH,     0     ,     0     )
									.add(    0     ,-plugDist/2,+plugDist/2)
							)
							.add(c0 = new LineGeometry.PolyLine()
									.addArc(LineGeometry.Axis.Y, new Point3D(barrelCentH,  barrelThick/2-0.05, 0), barrelHoleDiam/2, 0, 360, false, 16)
							)
							.add(c1 = new LineGeometry.PolyLine()
									.addArc(LineGeometry.Axis.Y, new Point3D(barrelCentH,  barrelThick/2, 0), barrelDiam/2-0.03, 0, 360, false, 16)
							)
							.add(c2 = new LineGeometry.PolyLine()
									.addArc(LineGeometry.Axis.Y, new Point3D(barrelCentH,  barrelThick/2-0.1, 0), barrelDiam/2, 0, 360, false, 16)
							)
							.add(c3 = new LineGeometry.PolyLine()
									.addArc(LineGeometry.Axis.Y, new Point3D(barrelCentH, -barrelThick/2+0.1, 0), barrelDiam/2, 0, 360, false, 16)
							)
							.add(c4 = new LineGeometry.PolyLine()
									.addArc(LineGeometry.Axis.Y, new Point3D(barrelCentH, -barrelThick/2, 0), barrelDiam/2-0.03, 0, 360, false, 16)
							)
							.add(c5 = new LineGeometry.PolyLine()
									.addArc(LineGeometry.Axis.Y, new Point3D(barrelCentH, -barrelThick/2+0.05, 0), barrelHoleDiam/2, 0, 360, false, 16)
							)
							.add(new LineGeometry.PolyLine()
									.addArc(LineGeometry.Axis.Y, new Point3D(barrelCentH,  barrelThick/2, 0), barrelCoreRadius, 0, 360, false, 16)
							)
							.add(new LineGeometry.PolyLine()
									.addArc(LineGeometry.Axis.Y, new Point3D(barrelCentH, -barrelThick/2, 0), barrelCoreRadius, 0, 360, false, 16)
							)
							.add(new LineGeometry.PolyLine()
									.add(barrelCentH + barrelCoreRadius,  barrelThick/2, 0)
									.add(barrelCentH + barrelCoreRadius, -barrelThick/2, 0)
							)
							.add(new LineGeometry.PolyLine()
									.add(barrelCentH - barrelCoreRadius,  barrelThick/2, 0)
									.add(barrelCentH - barrelCoreRadius, -barrelThick/2, 0)
							)
							.add(new LineGeometry.PolyLine()
									.add(barrelCentH,  barrelThick/2, +barrelCoreRadius)
									.add(barrelCentH, -barrelThick/2, +barrelCoreRadius)
							)
							.add(new LineGeometry.PolyLine()
									.add(barrelCentH,  barrelThick/2, -barrelCoreRadius)
									.add(barrelCentH, -barrelThick/2, -barrelCoreRadius)
							)
							;
					for (int i=0; i<8; i++)
						body.add(new LineGeometry.PolyLine(c0.get(i*2+1), c1.get(i*2+1), c2.get(i*2+1), c3.get(i*2+1), c4.get(i*2+1), c5.get(i*2+1)).close());
					
					
					return body;
				}
			
				private static LineGeometry.MultipleIndexedLineSets create_SOLARPANEL() {
					double height = 1.8; // X
					double plugDist = 1.35; // Y|Z
					double plugHeight = 0.1455; // X
					double panelMinHeight = height/3;
					double panelMaxHeight = height;
					double panelMidHeight = panelMaxHeight/2+panelMinHeight/2;
					double panelWidth     = height;  // real (diagonal) panel width:  sqrt( panelWidth^2 + panelWidth^2 )
					// Panel:
					// Corners: +Y,+Z  &  -Y,-Z
					
					
					LineGeometry.GroupingNode body = new LineGeometry.GroupingNode()
							.add(new LineGeometry.PolyLine() // Center Stick
									.add(   0.4,    0,     0     )
									.add( panelMidHeight, 0, 0 )
							)
							.add(new LineGeometry.PolyLine() // Plugs A & C
									.add(plugHeight,0,-plugDist/2)
									.add(plugHeight,0,-plugDist/4)
									.add(   0.4    ,0,     0     )
									.add(plugHeight,0,+plugDist/4)
									.add(plugHeight,0,+plugDist/2)
							)
							.add(new LineGeometry.PolyLine() // Plugs B & D
									.add(plugHeight,-plugDist/2,0)
									.add(plugHeight,-plugDist/4,0)
									.add(   0.4    ,     0     ,0)
									.add(plugHeight,+plugDist/4,0)
									.add(plugHeight,+plugDist/2,0)
							)
							.add(new LineGeometry.PolyLine() // Feet NN & PP
									.add( 0 ,-plugDist/2,-plugDist/2)
									.add(0.7,     0     ,     0     )
									.add( 0 ,+plugDist/2,+plugDist/2)
							)
							.add(new LineGeometry.PolyLine() // Feet PN & NP
									.add( 0 ,+plugDist/2,-plugDist/2)
									.add(0.7,     0     ,     0     )
									.add( 0 ,-plugDist/2,+plugDist/2)
							)
							.add(new LineGeometry.PolyLine() // Panel
									.add( panelMidHeight, +panelWidth/2, +panelWidth/2)
									.add( panelMinHeight,       0      , +panelWidth/2)
									.add( panelMinHeight, -panelWidth/2,       0      )
									.add( panelMidHeight, -panelWidth/2, -panelWidth/2)
									.add( panelMaxHeight,       0      , -panelWidth/2)
									.add( panelMaxHeight, +panelWidth/2,       0      )
									.close()
									)
							.add(new LineGeometry.PolyLine()
									.add( panelMidHeight, +panelWidth/2, +panelWidth/2)
									.add( panelMidHeight, -panelWidth/2, -panelWidth/2)
							)
							.add(new LineGeometry.PolyLine()
									.add( panelMaxHeight, +panelWidth/2,       0      )
									.add( panelMinHeight, -panelWidth/2,       0      )
							)
							.add(new LineGeometry.PolyLine()
									.add( panelMinHeight,       0      , +panelWidth/2)
									.add( panelMaxHeight,       0      , -panelWidth/2)
							)
							;
					
					LineGeometry.MultipleIndexedLineSets multi = new LineGeometry.MultipleIndexedLineSets()
							.add(body)
							;
					return multi;
				}
			}

			private static class Garages {
				
				static void addModelsToModelMap() {
					addModels("GARAGE_L", "^GARAGE_L");
					addModels("GARAGE_M", "^GARAGE_M", "^GARAGE_B", "^GARAGE_MECH");
					addModels("GARAGE_S", "^GARAGE_S", "^GARAGE_SUB");
					addModels("SUMMON_GARAGE", "^SUMMON_GARAGE");
					
					addProto( new Proto("GARAGE_L"     , create_GARAGE_L     ()) );
					addProto( new Proto("GARAGE_M"     , create_GARAGE_M     ()) );
					addProto( new Proto("GARAGE_S"     , create_GARAGE_S     ()) );
					addProto( new Proto("SUMMON_GARAGE", create_SUMMON_GARAGE()) );
				}
				
				private final static double height = 1;
				private final static double ring1Width = 1.75;
				private final static double ring2Width = 0.75;
				
				private interface AddOn {
					void add(LineGeometry.GroupingNode group, LineGeometry.PolyLine c1, LineGeometry.PolyLine c2, LineGeometry.PolyLine c3, LineGeometry.PolyLine c4);
				}
				
				private static LineGeometry.GroupingNode createGarage(double radius, AddOn addOn) {
					double radius2 = radius -ring1Width;
					double radius3 = radius2-ring2Width;
					double radius4 = radius3*2.0/5.0;
					LineGeometry.PolyLine c1,c2,c3,c4;
					LineGeometry.GroupingNode group = new LineGeometry.GroupingNode()
							.add(c1=new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(  0   ,0,0), radius , 0, 360, false, 24))
							.add(c2=new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(height,0,0), radius2, 0, 360, false, 24))
							.add(c3=new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(height,0,0), radius3, 0, 360, false, 18))
							.add(c4=new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(height,0,0), radius4, 0, 360, false, 12))
							.add(new LineGeometry.PolyLine().add(c1.get( 2)).add(c2.get( 2)))
							.add(new LineGeometry.PolyLine().add(c1.get( 6)).add(c2.get( 6)))
							.add(new LineGeometry.PolyLine().add(c1.get(10)).add(c2.get(10)))
							.add(new LineGeometry.PolyLine().add(c1.get(14)).add(c2.get(14)))
							.add(new LineGeometry.PolyLine().add(c1.get(18)).add(c2.get(18)))
							.add(new LineGeometry.PolyLine().add(c1.get(22)).add(c2.get(22)))
							.add(new LineGeometry.PolyLine().add(c3.get( 0)).add(c4.get( 0)))
							.add(new LineGeometry.PolyLine().add(c3.get( 3)).add(c4.get( 2)))
							.add(new LineGeometry.PolyLine().add(c3.get( 6)).add(c4.get( 4)))
							.add(new LineGeometry.PolyLine().add(c3.get( 9)).add(c4.get( 6)))
							.add(new LineGeometry.PolyLine().add(c3.get(12)).add(c4.get( 8)))
							.add(new LineGeometry.PolyLine().add(c3.get(15)).add(c4.get(10)))
							;
					if (addOn!=null)
						addOn.add(group,c1,c2,c3,c4);
					return group;
				}
				
				private static LineGeometry.IndexedLineSet create_GARAGE_L() {
					return createGarage(7.0,null); // 0.00 0.00 7.00, 1.00 0.00 5.00
				}
				private static LineGeometry.IndexedLineSet create_GARAGE_M() {
					return createGarage(5.25,null);// 0.00 0.00 5.25, 1.00 0.00 3.50
				}
				private static LineGeometry.IndexedLineSet create_GARAGE_S() {
					return createGarage(4.75,null);// 0.00 0.00 4.75, 1.00 0.00 3.00
				}
				private static LineGeometry.IndexedLineSet create_SUMMON_GARAGE() {
					double midHeight = 2;
					double radarHeight = 2.5;
					double radarRadius = 6;
					double radarAngle  = 45;
					
					return createGarage(5.25,(g,c12,c2,c3,c4)->{
						LineGeometry.PolyLine p;
						g
						.add(new LineGeometry.PolyLine().add(c4.get(0)).add(midHeight,0,0).add(c4.get(6)))
						.add(new LineGeometry.PolyLine().add(c4.get(3)).add(midHeight,0,0).add(c4.get(9)))
						.add(
								new LineGeometry.Transform(
										new LineGeometry.GroupingNode()
												.add(p = new LineGeometry.PolyLine()
														.addArc(LineGeometry.Axis.X, new Point3D(0, -radarRadius, 0), radarRadius, -radarAngle/2, radarAngle/2, false, 4)
														.addArc(LineGeometry.Axis.X, new Point3D(radarHeight, -radarRadius, 0), radarRadius, radarAngle/2, -radarAngle/2, true, 4)
														.close()
												)
												.add(new LineGeometry.PolyLine().add(p.get(1)).add(p.get(8)))
												.add(new LineGeometry.PolyLine().add(p.get(2)).add(p.get(7)))
												.add(new LineGeometry.PolyLine().add(p.get(3)).add(p.get(6)))
								)
								.addRotation(LineGeometry.Axis.Z, 20)
								.addTranslation(LineGeometry.Axis.X, midHeight)
								.addRotation(LineGeometry.Axis.X, 100)
						)
						;
					});
				}
			}

			private static class MAINROOMmodels {

				static void addModelsToModelMap() {
					addModels("BIOROOM",      "^BIOROOM");
					addModels("MAINROOM",     "^MAINROOM", "^MAINROOM_WATER");
					addModels("MAINROOMCUBE", "^MAINROOMCUBE", "^MAINROOMCUBE_W");
					
					addProto( new Proto("BIOROOM"          , create_BIOROOM          ()) );
					addProto( new Proto("MAINROOM"         , create_MAINROOM         ()) );
					addProto( new Proto("MAINROOM_ROOF"    , create_MAINROOM_ROOF    (), "MAINROOM") );
					addProto( new Proto("MAINROOMCUBE"     , create_MAINROOMCUBE     ()) );
					addProto( new Proto("MAINROOMCUBE_ROOF", create_MAINROOMCUBE_ROOF(), "MAINROOMCUBE") );
				}

				static String getRoofModelName(String modelName) {
					switch (modelName) {
					case "MAINROOM"    : return "MAINROOM_ROOF"    ;
					case "MAINROOMCUBE": return "MAINROOMCUBE_ROOF";
					}
					return null;
				}

				private final static double radius = 6; // Y|Z
				private final static double roomHeight = 4-0.05; // X
				private final static double cubesize = 4;
				
				private final static double roofBaseHeight = 4; // X
				private final static double roofPeakHeight = 6.28;
				private final static double roofRing1H = 5.63, roofRing1R = 4.5;
				private final static double roofRing3H = 5.96, roofRing3R = 2.40;
				private final static double roofRing2H = roofRing1H*0.4+roofRing3H*0.6, roofRing2R = (roofRing1R+roofRing3R)/2;
				private final static double roofPlattformR = 2.2;
				
				private final static double cornerR = 2;
				private final static double roofRing1CornerR = 1.0;
				private final static double roofRing2CornerR = 0.5;
				private final static double roofRing3CornerR = 0.2;
				private final static double roofPlattformCornerR = 0.1;
				
				private static LineGeometry.GroupingNode create_BIOROOM() {
					LineGeometry.PolyLine polyLine = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.Y, new Point3D(0,0,0), 6, 0, 70, false);
					LineGeometry.GroupingNode group1 = new LineGeometry.GroupingNode();
					for (int i=0; i<16; i++) {
						LineGeometry.Transform transform = new LineGeometry.Transform(polyLine);
						transform.addRotation(LineGeometry.Axis.X, 360.0/16*i);
						group1.add(transform);
					}
					LineGeometry.loopArc(6, 0, 70, false, 4, (i, nSeg, x, y) -> group1.add(new LineGeometry.Circle(LineGeometry.Axis.X, new Point3D(y,0,0), x)));
					
					return group1;
				}

				private static LineGeometry.IndexedLineSet create_MAINROOM() {
					LineGeometry.PolyLine c0,c1;
					LineGeometry.GroupingNode group = new LineGeometry.GroupingNode()
							.add(c0 = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(  0   ,0,0), radius, 0, 360, false, 32))
							.add(c1 = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(roomHeight,0,0), radius, 0, 360, false, 32))
							.add(new LineGeometry.PolyLine().add(c0.get( 2)).add(c1.get( 2)))
							.add(new LineGeometry.PolyLine().add(c0.get( 6)).add(c1.get( 6)))
							.add(new LineGeometry.PolyLine().add(c0.get(10)).add(c1.get(10)))
							.add(new LineGeometry.PolyLine().add(c0.get(14)).add(c1.get(14)))
							.add(new LineGeometry.PolyLine().add(c0.get(18)).add(c1.get(18)))
							.add(new LineGeometry.PolyLine().add(c0.get(22)).add(c1.get(22)))
							.add(new LineGeometry.PolyLine().add(c0.get(26)).add(c1.get(26)))
							.add(new LineGeometry.PolyLine().add(c0.get(30)).add(c1.get(30)))
							;
					return group;
				}

				private static LineGeometry.IndexedLineSet create_MAINROOM_ROOF() {
					LineGeometry.PolyLine c0,c1,c2,c3,c4;
					LineGeometry.GroupingNode group = new LineGeometry.GroupingNode()
							.add(c0 = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(roofBaseHeight,0,0),     radius, 0, 360, false, 32))
							.add(c1 = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(roofRing1H    ,0,0),     roofRing1R, 0, 360, false, 32))
							.add(c2 = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(roofRing2H    ,0,0),     roofRing2R, 0, 360, false, 16))
							.add(c3 = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(roofRing3H    ,0,0),     roofRing3R, 0, 360, false, 16))
							.add(c4 = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(roofPeakHeight,0,0), roofPlattformR, 0, 360, false,  8))
							.add(new LineGeometry.PolyLine().add(c0.get( 2)).add(c1.get( 2)).add(c2.get( 1)).add(c3.get( 1)))
							.add(new LineGeometry.PolyLine().add(c0.get( 6)).add(c1.get( 6)).add(c2.get( 3)).add(c3.get( 3)))
							.add(new LineGeometry.PolyLine().add(c0.get(10)).add(c1.get(10)).add(c2.get( 5)).add(c3.get( 5)))
							.add(new LineGeometry.PolyLine().add(c0.get(14)).add(c1.get(14)).add(c2.get( 7)).add(c3.get( 7)))
							.add(new LineGeometry.PolyLine().add(c0.get(18)).add(c1.get(18)).add(c2.get( 9)).add(c3.get( 9)))
							.add(new LineGeometry.PolyLine().add(c0.get(22)).add(c1.get(22)).add(c2.get(11)).add(c3.get(11)))
							.add(new LineGeometry.PolyLine().add(c0.get(26)).add(c1.get(26)).add(c2.get(13)).add(c3.get(13)))
							.add(new LineGeometry.PolyLine().add(c0.get(30)).add(c1.get(30)).add(c2.get(15)).add(c3.get(15)))
							.add(new LineGeometry.PolyLine().add(c1.get( 0)).add(c2.get( 0)).add(c3.get( 0)))
							.add(new LineGeometry.PolyLine().add(c1.get( 4)).add(c2.get( 2)).add(c3.get( 2)))
							.add(new LineGeometry.PolyLine().add(c1.get( 8)).add(c2.get( 4)).add(c3.get( 4)))
							.add(new LineGeometry.PolyLine().add(c1.get(12)).add(c2.get( 6)).add(c3.get( 6)))
							.add(new LineGeometry.PolyLine().add(c1.get(16)).add(c2.get( 8)).add(c3.get( 8)))
							.add(new LineGeometry.PolyLine().add(c1.get(20)).add(c2.get(10)).add(c3.get(10)))
							.add(new LineGeometry.PolyLine().add(c1.get(24)).add(c2.get(12)).add(c3.get(12)))
							.add(new LineGeometry.PolyLine().add(c1.get(28)).add(c2.get(14)).add(c3.get(14)))
							.add(new LineGeometry.PolyLine().add(c4.get(0)).add(c4.get(4)))
							.add(new LineGeometry.PolyLine().add(c4.get(2)).add(c4.get(6)))
							;
					return group;
				}

				private static LineGeometry.IndexedLineSet create_MAINROOMCUBE() {
					LineGeometry.GroupingNode group = new LineGeometry.GroupingNode()
							.add(new LineGeometry.PolyLine()
									.addArc(LineGeometry.Axis.X, new Point3D(0, radius-cornerR, radius-cornerR), cornerR,   0,  90, false, 6)
									.addArc(LineGeometry.Axis.X, new Point3D(0,-radius+cornerR, radius-cornerR), cornerR,  90, 180, false, 6)
									.addArc(LineGeometry.Axis.X, new Point3D(0,-radius+cornerR,-radius+cornerR), cornerR, 180, 270, false, 6)
									.addArc(LineGeometry.Axis.X, new Point3D(0, radius-cornerR,-radius+cornerR), cornerR, 270, 360, false, 6)
									.close()
							)
							.add(new LineGeometry.PolyLine()
									.addArc(LineGeometry.Axis.X, new Point3D(roomHeight, radius-cornerR, radius-cornerR), cornerR,   0,  90, false, 6)
									.addArc(LineGeometry.Axis.X, new Point3D(roomHeight,-radius+cornerR, radius-cornerR), cornerR,  90, 180, false, 6)
									.addArc(LineGeometry.Axis.X, new Point3D(roomHeight,-radius+cornerR,-radius+cornerR), cornerR, 180, 270, false, 6)
									.addArc(LineGeometry.Axis.X, new Point3D(roomHeight, radius-cornerR,-radius+cornerR), cornerR, 270, 360, false, 6)
									.close()
							)
							.add(new LineGeometry.PolyLine().add(0, cubesize/2, radius).add(roomHeight, cubesize/2, radius))
							.add(new LineGeometry.PolyLine().add(0,-cubesize/2, radius).add(roomHeight,-cubesize/2, radius))
							.add(new LineGeometry.PolyLine().add(0, cubesize/2,-radius).add(roomHeight, cubesize/2,-radius))
							.add(new LineGeometry.PolyLine().add(0,-cubesize/2,-radius).add(roomHeight,-cubesize/2,-radius))
							.add(new LineGeometry.PolyLine().add(0, radius, cubesize/2).add(roomHeight, radius, cubesize/2))
							.add(new LineGeometry.PolyLine().add(0, radius,-cubesize/2).add(roomHeight, radius,-cubesize/2))
							.add(new LineGeometry.PolyLine().add(0,-radius, cubesize/2).add(roomHeight,-radius, cubesize/2))
							.add(new LineGeometry.PolyLine().add(0,-radius,-cubesize/2).add(roomHeight,-radius,-cubesize/2))
							;
					return group;
				}

				private static LineGeometry.IndexedLineSet create_MAINROOMCUBE_ROOF() {
					LineGeometry.GroupingNode group = new LineGeometry.GroupingNode()
							.add(new LineGeometry.PolyLine()
									.addArc(LineGeometry.Axis.X, new Point3D(roofBaseHeight, radius-cornerR, radius-cornerR), cornerR,   0,  90, false, 6)
									.addArc(LineGeometry.Axis.X, new Point3D(roofBaseHeight,-radius+cornerR, radius-cornerR), cornerR,  90, 180, false, 6)
									.addArc(LineGeometry.Axis.X, new Point3D(roofBaseHeight,-radius+cornerR,-radius+cornerR), cornerR, 180, 270, false, 6)
									.addArc(LineGeometry.Axis.X, new Point3D(roofBaseHeight, radius-cornerR,-radius+cornerR), cornerR, 270, 360, false, 6)
									.close()
							)
							.add(new LineGeometry.PolyLine()
									.addArc(LineGeometry.Axis.X, new Point3D(roofRing1H, roofRing1R-roofRing1CornerR, roofRing1R-roofRing1CornerR), roofRing1CornerR,   0,  90, false, 6)
									.addArc(LineGeometry.Axis.X, new Point3D(roofRing1H,-roofRing1R+roofRing1CornerR, roofRing1R-roofRing1CornerR), roofRing1CornerR,  90, 180, false, 6)
									.addArc(LineGeometry.Axis.X, new Point3D(roofRing1H,-roofRing1R+roofRing1CornerR,-roofRing1R+roofRing1CornerR), roofRing1CornerR, 180, 270, false, 6)
									.addArc(LineGeometry.Axis.X, new Point3D(roofRing1H, roofRing1R-roofRing1CornerR,-roofRing1R+roofRing1CornerR), roofRing1CornerR, 270, 360, false, 6)
									.close()
							)
							.add(new LineGeometry.PolyLine()
									.addArc(LineGeometry.Axis.X, new Point3D(roofRing2H, roofRing2R-roofRing2CornerR, roofRing2R-roofRing2CornerR), roofRing2CornerR,   0,  90, false, 4)
									.addArc(LineGeometry.Axis.X, new Point3D(roofRing2H,-roofRing2R+roofRing2CornerR, roofRing2R-roofRing2CornerR), roofRing2CornerR,  90, 180, false, 4)
									.addArc(LineGeometry.Axis.X, new Point3D(roofRing2H,-roofRing2R+roofRing2CornerR,-roofRing2R+roofRing2CornerR), roofRing2CornerR, 180, 270, false, 4)
									.addArc(LineGeometry.Axis.X, new Point3D(roofRing2H, roofRing2R-roofRing2CornerR,-roofRing2R+roofRing2CornerR), roofRing2CornerR, 270, 360, false, 4)
									.close()
							)
							.add(new LineGeometry.PolyLine()
									.addArc(LineGeometry.Axis.X, new Point3D(roofRing3H, roofRing3R-roofRing3CornerR, roofRing3R-roofRing3CornerR), roofRing3CornerR,   0,  90, false, 3)
									.addArc(LineGeometry.Axis.X, new Point3D(roofRing3H,-roofRing3R+roofRing3CornerR, roofRing3R-roofRing3CornerR), roofRing3CornerR,  90, 180, false, 3)
									.addArc(LineGeometry.Axis.X, new Point3D(roofRing3H,-roofRing3R+roofRing3CornerR,-roofRing3R+roofRing3CornerR), roofRing3CornerR, 180, 270, false, 3)
									.addArc(LineGeometry.Axis.X, new Point3D(roofRing3H, roofRing3R-roofRing3CornerR,-roofRing3R+roofRing3CornerR), roofRing3CornerR, 270, 360, false, 3)
									.close()
							)
							.add(new LineGeometry.PolyLine()
									.addArc(LineGeometry.Axis.X, new Point3D(roofPeakHeight, roofPlattformR-roofPlattformCornerR, roofPlattformR-roofPlattformCornerR), roofPlattformCornerR,   0,  90, false, 3)
									.addArc(LineGeometry.Axis.X, new Point3D(roofPeakHeight,-roofPlattformR+roofPlattformCornerR, roofPlattformR-roofPlattformCornerR), roofPlattformCornerR,  90, 180, false, 3)
									.addArc(LineGeometry.Axis.X, new Point3D(roofPeakHeight,-roofPlattformR+roofPlattformCornerR,-roofPlattformR+roofPlattformCornerR), roofPlattformCornerR, 180, 270, false, 3)
									.addArc(LineGeometry.Axis.X, new Point3D(roofPeakHeight, roofPlattformR-roofPlattformCornerR,-roofPlattformR+roofPlattformCornerR), roofPlattformCornerR, 270, 360, false, 3)
									.close()
							)
							
							.add(new LineGeometry.PolyLine().add(roofPeakHeight, 0, roofPlattformR).add(roofPeakHeight, 0, -roofPlattformR))
							.add(new LineGeometry.PolyLine().add(roofPeakHeight, roofPlattformR, 0).add(roofPeakHeight, -roofPlattformR, 0))
							
							.add(new LineGeometry.PolyLine().add(roofRing1H, 0, roofRing1R).add(roofRing2H, 0, roofRing2R).add(roofRing3H, 0, roofRing3R))
							.add(new LineGeometry.PolyLine().add(roofRing1H, 0,-roofRing1R).add(roofRing2H, 0,-roofRing2R).add(roofRing3H, 0,-roofRing3R))
							.add(new LineGeometry.PolyLine().add(roofRing1H, roofRing1R, 0).add(roofRing2H, roofRing2R, 0).add(roofRing3H, roofRing3R, 0))
							.add(new LineGeometry.PolyLine().add(roofRing1H,-roofRing1R, 0).add(roofRing2H,-roofRing2R, 0).add(roofRing3H,-roofRing3R, 0))
							
							.add(new LineGeometry.PolyLine().add(roofBaseHeight, cubesize/2, radius).add(roofRing1H, cubesize/2, roofRing1R).add(roofRing2H, cubesize/2, roofRing2R).add(roofRing3H, cubesize/2, roofRing3R))
							.add(new LineGeometry.PolyLine().add(roofBaseHeight,-cubesize/2, radius).add(roofRing1H,-cubesize/2, roofRing1R).add(roofRing2H,-cubesize/2, roofRing2R).add(roofRing3H,-cubesize/2, roofRing3R))
							.add(new LineGeometry.PolyLine().add(roofBaseHeight, cubesize/2,-radius).add(roofRing1H, cubesize/2,-roofRing1R).add(roofRing2H, cubesize/2,-roofRing2R).add(roofRing3H, cubesize/2,-roofRing3R))
							.add(new LineGeometry.PolyLine().add(roofBaseHeight,-cubesize/2,-radius).add(roofRing1H,-cubesize/2,-roofRing1R).add(roofRing2H,-cubesize/2,-roofRing2R).add(roofRing3H,-cubesize/2,-roofRing3R))
							.add(new LineGeometry.PolyLine().add(roofBaseHeight, radius, cubesize/2).add(roofRing1H, roofRing1R, cubesize/2).add(roofRing2H, roofRing2R, cubesize/2).add(roofRing3H, roofRing3R, cubesize/2))
							.add(new LineGeometry.PolyLine().add(roofBaseHeight, radius,-cubesize/2).add(roofRing1H, roofRing1R,-cubesize/2).add(roofRing2H, roofRing2R,-cubesize/2).add(roofRing3H, roofRing3R,-cubesize/2))
							.add(new LineGeometry.PolyLine().add(roofBaseHeight,-radius, cubesize/2).add(roofRing1H,-roofRing1R, cubesize/2).add(roofRing2H,-roofRing2R, cubesize/2).add(roofRing3H,-roofRing3R, cubesize/2))
							.add(new LineGeometry.PolyLine().add(roofBaseHeight,-radius,-cubesize/2).add(roofRing1H,-roofRing1R,-cubesize/2).add(roofRing2H,-roofRing2R,-cubesize/2).add(roofRing3H,-roofRing3R,-cubesize/2))
							;
					return group;
				}
			}

			private static class CubeRoomObjects {
				static void addModelsToModelMap() {
					addModels("WALLFLOORLADDER", "^WALLFLOORLADDER");
					addModels("CUBESTAIRS",      "^CUBESTAIRS"     );
					addModels("CUBEFLOOR",       "^CUBEFLOOR"      );
					addModels("CUBEWALL",        "^CUBEWALL"       );
					addModels("CUBEINNERDOOR",   "^CUBEINNERDOOR"  );
					addModels("CUBEWINDOW",      "^CUBEWINDOW"     );
					addModels("CUBEWINDOWOVAL",  "^CUBEWINDOWOVAL" );
					addModels("CUBEWINDOWSMALL", "^CUBEWINDOWSMALL");
					addModels("CUBEFRAME",       "^CUBEFRAME"      );
					
					addModels("CONTAINER",  makeVariations("^CONTAINER%d", 0,1,2,3,4,5,6,7,8,9));
					
					addProto( new Proto("WALLFLOORLADDER", 0.6, 0, 15, create_WALLFLOORLADDER()) );
					addProto( new Proto("CUBEFLOOR"      ,         15, create_CUBEFLOOR      ()) );
					addProto( new Proto("CUBEWALL"       , 0.6, 0, 25, create_CUBEWALL       ()) );
					addProto( new Proto("CUBEINNERDOOR"  , 0.6, 0, 25, create_CUBEINNERDOOR  ()) );
					addProto( new Proto("CUBEWINDOW"     , 0.6, 0, 25, create_CUBEWINDOW     ()) );
					addProto( new Proto("CUBEWINDOWOVAL" , 0.6, 0, 25, create_CUBEWINDOWOVAL ()) );
					addProto( new Proto("CUBEWINDOWSMALL", 0.6, 0, 25, create_CUBEWINDOWSMALL()) );
					addProto( new Proto("CUBESTAIRS"     ,         15, create_CUBESTAIRS     ()) );
					addProto( new Proto("CUBEFRAME"      ,         15, create_CUBEFRAME      ()) );
					addProto( new Proto("CONTAINER"      ,         15, create_CONTAINER      ()) );
				}
			
				private static final double cubesize = 4;
				private static final double wallspacing = 0.05;
				private static final double wallthickness = 0.1;
				
				private static LineGeometry.Box create_CUBEFLOOR() {
					return new LineGeometry.Box( wallthickness, cubesize-wallspacing, cubesize-wallspacing );
				}
				
				private static LineGeometry.Transform create_CUBEWALL() {
					return createXFloorBasedBox( cubesize-wallspacing, wallthickness, cubesize-wallspacing );
				}
				
				private static LineGeometry.GroupingNode create_WALLFLOORLADDER() {
					// 3.28562
					double posY = -1.28;
					double widthZ = cubesize/6;
					double distToWall = widthZ/2;
					int nSteps = 8;
					
					LineGeometry.GroupingNode group = new LineGeometry.GroupingNode()
							.add(new LineGeometry.PolyLine().add(0,posY+distToWall, widthZ/2).add(cubesize-wallspacing,posY+distToWall, widthZ/2))
							.add(new LineGeometry.PolyLine().add(0,posY+distToWall,-widthZ/2).add(cubesize-wallspacing,posY+distToWall,-widthZ/2))
							;
					
					for (int i=0; i<nSteps; i++) {
						double h = (i+0.5)*cubesize/nSteps;
						LineGeometry.PolyLine step = new LineGeometry.PolyLine();
						
						if      (i==0       ) step.add(   0    ,posY,-widthZ/2);
						else if (i==nSteps-1) step.add(cubesize,posY,-widthZ/2);
						
						step.add(h,posY+distToWall,-widthZ/2).add(h,posY+distToWall,widthZ/2);
						
						if      (i==0       ) step.add(   0    ,posY, widthZ/2);
						else if (i==nSteps-1) step.add(cubesize,posY, widthZ/2);
						
						group.add(step);
					}
					
					return group;
				}
				
				private static LineGeometry.GroupingNode create_CUBEFRAME() {
					double h = cubesize-wallspacing;
					double v = (cubesize-wallspacing)/2;
					Point3D p000 = new Point3D( 0, -v, -v ); Point3D p100 = new Point3D( h, -v, -v );
					Point3D p001 = new Point3D( 0, -v,  v ); Point3D p101 = new Point3D( h, -v,  v );
					Point3D p011 = new Point3D( 0,  v,  v ); Point3D p111 = new Point3D( h,  v,  v );
					Point3D p010 = new Point3D( 0,  v, -v ); Point3D p110 = new Point3D( h,  v, -v );
					LineGeometry.GroupingNode group = new LineGeometry.GroupingNode()
							.add(new LineGeometry.PolyLine().add(p000).add(p100).add(p111).add(p011).close())
							.add(new LineGeometry.PolyLine().add(p001).add(p101).add(p110).add(p010).close())
							
							.add(new LineGeometry.PolyLine().add(p001).add(p011).add(p110).add(p100).close())
							.add(new LineGeometry.PolyLine().add(p010).add(p000).add(p101).add(p111).close())
							
							.add(new LineGeometry.PolyLine().add(p000).add(p001).add(p111).add(p110).close())
							.add(new LineGeometry.PolyLine().add(p011).add(p010).add(p100).add(p101).close())
							;
					return group;
				}
				
				private static LineGeometry.GroupingNode create_CUBESTAIRS() {
					// X: Height
					// Y: Depth
					// Z: Width==Height
					double width = 3.2;
					double stairThickH = 0.8;
				
					LineGeometry.GroupingNode group = new LineGeometry.GroupingNode()
							.add(new LineGeometry.Prism(LineGeometry.Axis.Z, width,
									new Point3D(cubesize-wallspacing            , -(cubesize-wallspacing)/2            , 0),
									new Point3D(cubesize-wallspacing-stairThickH, -(cubesize-wallspacing)/2            , 0),
									new Point3D(              0                 ,  (cubesize-wallspacing)/2-stairThickH, 0),
									new Point3D(              0                 ,  (cubesize-wallspacing)/2            , 0)
									));
					
					int stepCount = 11;
					double stepDepth = 0.5; // Y
					double stepWidth = 3.0; // Z
					
					for (int i=0; i<stepCount-1; i++) {
						double x = (i+1)*(cubesize-wallspacing)/stepCount;
						double y = (cubesize-wallspacing)/2 - x;
						group.add(new LineGeometry.PolyLine()
								.add(x, y+stepDepth/2,  stepWidth/2)
								.add(x, y+stepDepth/2, -stepWidth/2)
								.add(x, y-stepDepth/2, -stepWidth/2)
								.add(x, y-stepDepth/2,  stepWidth/2)
								.close()
						);
					}
					
					return group;
				}
				
				private static LineGeometry.GroupingNode create_CUBEINNERDOOR() {
					// X: Height
					// Y: Thickness
					// Z: Width==Height
					
					// r*sin(a/2) = sp/2
					// tan(a/2) = sp/2 / h
					
					double doorRadius = cubesize*0.8/2;
					double doorFloorSpacing = cubesize/4;
					
					double doorFloorAngle_deg = Math.asin( doorFloorSpacing/2/doorRadius )*2 * 180/Math.PI;
					double doorCenterHeight = doorFloorSpacing/2 / Math.tan(doorFloorAngle_deg/2 *Math.PI/180);
					
					LineGeometry.GroupingNode group = new LineGeometry.GroupingNode()
						.add(new LineGeometry.PolyLine()
								.add( cubesize-wallspacing,  wallthickness/2,  (cubesize-wallspacing)/2 )
								.add(         0           ,  wallthickness/2,  (cubesize-wallspacing)/2 )
								.addArc(LineGeometry.Axis.Y, new Point3D(doorCenterHeight,  wallthickness/2, 0), doorRadius, -90+doorFloorAngle_deg/2, 270-doorFloorAngle_deg/2, false)
								.add(         0           ,  wallthickness/2, -(cubesize-wallspacing)/2 )
								.add( cubesize-wallspacing,  wallthickness/2, -(cubesize-wallspacing)/2 )
								.close()
						)
						.add(new LineGeometry.PolyLine()
								.add( cubesize-wallspacing, -wallthickness/2,  (cubesize-wallspacing)/2 )
								.add(         0           , -wallthickness/2,  (cubesize-wallspacing)/2 )
								.addArc(LineGeometry.Axis.Y, new Point3D(doorCenterHeight, -wallthickness/2, 0), doorRadius, -90+doorFloorAngle_deg/2, 270-doorFloorAngle_deg/2, false)
								.add(         0           , -wallthickness/2, -(cubesize-wallspacing)/2 )
								.add( cubesize-wallspacing, -wallthickness/2, -(cubesize-wallspacing)/2 )
								.close()
						)
						.add(new LineGeometry.PolyLine()
								.add( cubesize-wallspacing,  wallthickness/2,  (cubesize-wallspacing)/2 )
								.add( cubesize-wallspacing, -wallthickness/2,  (cubesize-wallspacing)/2 )
						)
						.add(new LineGeometry.PolyLine()
								.add(         0           ,  wallthickness/2,  (cubesize-wallspacing)/2 )
								.add(         0           , -wallthickness/2,  (cubesize-wallspacing)/2 )
						)
						.add(new LineGeometry.PolyLine()
								.add(         0           ,  wallthickness/2,  (doorFloorSpacing)/2 )
								.add(         0           , -wallthickness/2,  (doorFloorSpacing)/2 )
						)
						.add(new LineGeometry.PolyLine()
								.add(         0           ,  wallthickness/2, -(doorFloorSpacing)/2 )
								.add(         0           , -wallthickness/2, -(doorFloorSpacing)/2 )
						)
						.add(new LineGeometry.PolyLine()
								.add(         0           ,  wallthickness/2, -(cubesize-wallspacing)/2 )
								.add(         0           , -wallthickness/2, -(cubesize-wallspacing)/2 )
						)
						.add(new LineGeometry.PolyLine()
								.add( cubesize-wallspacing,  wallthickness/2, -(cubesize-wallspacing)/2 )
								.add( cubesize-wallspacing, -wallthickness/2, -(cubesize-wallspacing)/2 )
						)
						;
					
					return group;
				}
			
				private static LineGeometry.MultipleIndexedLineSets create_CUBEWINDOW() {
					LineGeometry.MultipleIndexedLineSets multi = new LineGeometry.MultipleIndexedLineSets()
							.add(COLOR_WINDOW, new LineGeometry.PolyLine()
									.add(cubesize*0.1, 0, cubesize*0.4)
									.add(cubesize*0.1, 0,-cubesize*0.4)
									.add(cubesize*0.9, 0,-cubesize*0.4)
									.add(cubesize*0.9, 0, cubesize*0.4)
									.close()
							);
					return multi;
				}
			
				private static LineGeometry.MultipleIndexedLineSets create_CUBEWINDOWOVAL() {
					double radius = 1;
					LineGeometry.MultipleIndexedLineSets multi = new LineGeometry.MultipleIndexedLineSets()
							.add(COLOR_WINDOW, new LineGeometry.PolyLine()
									.addArc(LineGeometry.Axis.Y, new Point3D(cubesize/2,0,0), radius, 0, 360, false)
							);
					return multi;
				}
			
				private static LineGeometry.MultipleIndexedLineSets create_CUBEWINDOWSMALL() {
					// height: 170 : 40 -> 0.95
					// width : 155 : 56 -> 1.45
					double heightW = 0.95;
					double widthW  = 1.45;
					LineGeometry.MultipleIndexedLineSets multi = new LineGeometry.MultipleIndexedLineSets()
							.add(COLOR_WINDOW, new LineGeometry.PolyLine()
									.add(cubesize/2+heightW/2, 0, widthW/2)
									.add(cubesize/2+heightW/2, 0,-widthW/2)
									.add(cubesize/2-heightW/2, 0,-widthW/2)
									.add(cubesize/2-heightW/2, 0, widthW/2)
									.close()
							);
					return multi;
				}

				private static LineGeometry.IndexedLineSet create_CONTAINER() {
					// TO-DO: create_CONTAINER
					String coords  = "0.050 -1.950 -1.950, 3.950 -1.950 -1.950, 3.950 -1.950 1.950, 0.050 -1.950 1.950, 0.050 1.950 -1.950, 3.950 1.950 -1.950, 3.950 1.950 1.950, 0.050 1.950 1.950, 3.000 1.950 0.000, 2.951 1.950 0.309, 2.809 1.950 0.588, 2.588 1.950 0.809, 2.309 1.950 0.951, 2.000 1.950 1.000, 1.691 1.950 0.951, 1.412 1.950 0.809, 1.191 1.950 0.588, 1.049 1.950 0.309, 1.000 1.950 0.000, 1.049 1.950 -0.309, 1.191 1.950 -0.588, 1.412 1.950 -0.809, 1.691 1.950 -0.951, 2.000 1.950 -1.000, 2.309 1.950 -0.951, 2.588 1.950 -0.809, 2.809 1.950 -0.588, 2.951 1.950 -0.309, 2.800 1.462 0.000, 2.761 1.462 0.247, 2.647 1.462 0.470, 2.470 1.462 0.647, 2.247 1.462 0.761, 2.000 1.462 0.800, 1.753 1.462 0.761, 1.530 1.462 0.647, 1.353 1.462 0.470, 1.239 1.462 0.247, 1.200 1.462 0.000, 1.239 1.462 -0.247, 1.353 1.462 -0.470, 1.530 1.462 -0.647, 1.753 1.462 -0.761, 2.000 1.462 -0.800, 2.247 1.462 -0.761, 2.470 1.462 -0.647, 2.647 1.462 -0.470, 2.761 1.462 -0.247, 2.551 1.761 0.000, 2.509 1.761 0.211, 2.390 1.761 0.390, 2.211 1.761 0.509, 2.000 1.761 0.551, 1.789 1.761 0.509, 1.610 1.761 0.390, 1.491 1.761 0.211, 1.449 1.761 0.000, 1.491 1.761 -0.211, 1.610 1.761 -0.390, 1.789 1.761 -0.509, 2.000 1.761 -0.551, 2.211 1.761 -0.509, 2.390 1.761 -0.390, 2.509 1.761 -0.211, 2.200 1.927 0.000, 2.173 1.927 0.100, 2.100 1.927 0.173, 2.000 1.927 0.200, 1.900 1.927 0.173, 1.827 1.927 0.100, 1.800 1.927 0.000, 1.827 1.927 -0.100, 1.900 1.927 -0.173, 2.000 1.927 -0.200, 2.100 1.927 -0.173, 2.173 1.927 -0.100, 2.180 1.900 0.000, 2.156 1.900 0.090, 2.090 1.900 0.156, 2.000 1.900 0.180, 1.910 1.900 0.156, 1.844 1.900 0.090, 1.820 1.900 0.000, 1.844 1.900 -0.090, 1.910 1.900 -0.156, 2.000 1.900 -0.180, 2.090 1.900 -0.156, 2.156 1.900 -0.090, 2.000 1.462 0.800, 2.000 1.595 0.716, 2.000 1.711 0.611, 2.000 1.807 0.488, 2.000 1.880 0.349, 2.000 1.927 0.200, 2.000 1.462 -0.800, 2.000 1.595 -0.716, 2.000 1.711 -0.611, 2.000 1.807 -0.488, 2.000 1.880 -0.349, 2.000 1.927 -0.200, 2.800 1.462 0.000, 2.716 1.595 0.000, 2.611 1.711 0.000, 2.488 1.807 0.000, 2.349 1.880 0.000, 2.200 1.927 0.000, 1.200 1.462 0.000, 1.284 1.595 0.000, 1.389 1.711 0.000, 1.512 1.807 0.000, 1.651 1.880 0.000, 1.800 1.927 0.000";
					String indexes = "0 1 2 3 0 4 5 6 7 4 -1 1 5 -1 2 6 -1 3 7 -1 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 8 -1 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 28 -1 48 49 50 51 52 53 54 55 56 57 58 59 60 61 62 63 48 -1 64 65 66 67 68 69 70 71 72 73 74 75 64 -1 76 77 78 79 80 81 82 83 84 85 86 87 76 -1 88 89 90 91 92 93 -1 94 95 96 97 98 99 -1 100 101 102 103 104 105 -1 106 107 108 109 110 111 -1";
					LineGeometry.IndexedLineSet lineSet = LineGeometry.IndexedLineSet.parse(coords,indexes, "CONTAINER");
					if (lineSet==null) lineSet = new LineGeometry.PolyLine();
					return lineSet;
				}
				
			}

			private static class Corridors {
				
				static void addModelsToModelMap() {
					//addModels("CORRIDOR",       "^CORRIDOR","^GLASSCORRIDOR"); 
					addModels("CORRIDOR",       "^CORRIDOR");
					addModels("GLASSCORRIDOR",  "^GLASSCORRIDOR");
					
					addModels("CORRIDORL",      "^CORRIDORL");
					addModels("CORRIDORX",      "^CORRIDORX");
					addModels("CORRIDORT",      "^CORRIDORT");
					addModels("CORRIDORC",      "^CORRIDORC");
					
					addModels("DOOR2",          "^DOOR2");  // Holo-T�r
					addModels("BUILDDOOR",      "^BUILDDOOR");
					addModels("BUILDRAMP",      "^BUILDRAMP");
					
					addModels("CORRIDOR_WATER",  "^CORRIDOR_WATER");
					addModels("CORRIDORL_WATER", "^CORRIDORL_WATER");
					addModels("CORRIDORX_WATER", "^CORRIDORX_WATER");
					addModels("CORRIDORT_WATER", "^CORRIDORT_WATER");
					addModels("CORRIDORV_WATER", "^CORRIDORV_WATER");
					addModels("BUILDDOOR_WATER", "^BUILDDOOR_WATER");
					
					addProto( new Proto("CORRIDOR_WATER" ,          15, create_CORRIDOR_WATER ()) );
					addProto( new Proto("CORRIDORL_WATER",          15, create_CORRIDORL_WATER()) );
					addProto( new Proto("CORRIDORX_WATER",          15, create_CORRIDORX_WATER()) );
					addProto( new Proto("CORRIDORT_WATER", 1.0, 90, 15, create_CORRIDORT_WATER()) );
					addProto( new Proto("CORRIDORV_WATER",          15, create_CORRIDORV_WATER()) );
					addProto( new Proto("BUILDDOOR_WATER",          15, create_BUILDDOOR_WATER()) );
					
					addProto( new Proto("CORRIDOR"       ,          15, create_CORRIDOR       ()) );
					addProto( new Proto("CORRIDORL"      ,          15, create_CORRIDORL      ()) );
					addProto( new Proto("CORRIDORX"      ,          15, create_CORRIDORX      ()) );
					addProto( new Proto("CORRIDORT"      ,          15, create_CORRIDORT      ()) );
					
					addProto( new Proto("CORRIDORC"      , 1.0, 45,     create_CORRIDORC      ()) );
					addProto( new Proto("GLASSCORRIDOR"  ,          15, create_GLASSCORRIDOR  ()) );
					
					addProto( new Proto("DOOR2"          ,          15, create_DOOR2          ()) );
					addProto( new Proto("BUILDDOOR"      ,          15, create_BUILDDOOR      ()) );
					addProto( new Proto("BUILDRAMP"      , 0.5, 90, 15, create_BUILDRAMP      ()) );
				}

				private static final double raster = 4.0; // YZ
				private static final double height = 3.3; // X
				private static final double width  = 3.05; // Y
				private static final double width2 = 1.95; // Y
				private static final double heightW = (height-(width-width2))/3 + (width-width2)/2; // Unterkante der Fensterscheibe
				private static final double spacing = 0.05;
				
				private static final double radius = width/2+0.1;
				private static final double bottomAngle = Math.asin(width2/2/radius)*2*180/Math.PI; // r*sin(a/2) = w2/2
				private static final double flange = 0.3;
				
				private static LineGeometry.IndexedLineSet create_CORRIDORV_WATER() {
					LineGeometry.PolyLine pZN,pZNF,pZPF,pZP;
					LineGeometry.GroupingNode group = new LineGeometry.GroupingNode()
							.add(pZN  = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(0,0,0), radius, 0, 360, false, 24))
							.add(pZNF = pZN.getCopy(               flange,0,0))
							.add(pZPF = pZN.getCopy(raster-spacing-flange,0,0))
							.add(pZP  = pZN.getCopy(raster-spacing       ,0,0))
							.add(new LineGeometry.PolyLine().add(pZN.get( 0)).add(pZNF.get( 0)).add(pZPF.get( 0)).add(pZP.get( 0)))
							.add(new LineGeometry.PolyLine().add(pZN.get( 6)).add(pZNF.get( 6)).add(pZPF.get( 6)).add(pZP.get( 6)))
							.add(new LineGeometry.PolyLine().add(pZN.get(12)).add(pZNF.get(12)).add(pZPF.get(12)).add(pZP.get(12)))
							.add(new LineGeometry.PolyLine().add(pZN.get(18)).add(pZNF.get(18)).add(pZPF.get(18)).add(pZP.get(18)))
							;
					return group;
				}
				
				private static LineGeometry.IndexedLineSet create_BUILDDOOR_WATER() {
					LineGeometry.PolyLine pY,pYF;
					LineGeometry.GroupingNode group = new LineGeometry.GroupingNode()
							.add(pY = new LineGeometry.PolyLine()
									.add(0, spacing/2, -width2/2)
									.addArc(LineGeometry.Axis.Y, new Point3D(height-radius, spacing/2, 0), radius, 270-bottomAngle/2, 270+bottomAngle/2, true, 20)
									.add(0, spacing/2, +width2/2)
									.close()
							)
							.add(pYF = pY.getCopy(0,flange,0))
							.add(new LineGeometry.PolyLine().add(pY.get( 0)).add(pYF.get( 0)))
							.add(new LineGeometry.PolyLine().add(pY.get( 1)).add(pYF.get( 1)))
							.add(new LineGeometry.PolyLine().add(pY.get( 5)).add(pYF.get( 5)))
							.add(new LineGeometry.PolyLine().add(pY.get(11)).add(pYF.get(11)))
							.add(new LineGeometry.PolyLine().add(pY.get(17)).add(pYF.get(17)))
							.add(new LineGeometry.PolyLine().add(pY.get(21)).add(pYF.get(21)))
							.add(new LineGeometry.PolyLine().add(pY.get(22)).add(pYF.get(22)))
							;
					
					LineGeometry.PolyLine pY1,pY2,pY3,pY4;
					pY1 = pYF.getCopy(0,0.3,0);
					pY2 = pY1.getCopy(0,0.3,0);
					pY3 = pY2.getCopy(0,0.3,0);
					pY4 = pY3.getCopy(0,0.3,0);
					group
						.add(new LineGeometry.PolyLine().add(pYF.get( 1)).add(pY1.get( 1)))
						.add(new LineGeometry.PolyLine().add(pYF.get( 4)).add(pY4.get( 4)))
						.add(new LineGeometry.PolyLine().add(pYF.get( 6)).add(pY2.get( 6)))
						.add(new LineGeometry.PolyLine().add(pYF.get(10)).add(pY3.get(10)))
						.add(new LineGeometry.PolyLine().add(pYF.get(13)).add(pY4.get(13)))
						.add(new LineGeometry.PolyLine().add(pYF.get(15)).add(pY2.get(15)))
						.add(new LineGeometry.PolyLine().add(pYF.get(19)).add(pY1.get(19)))
						.add(new LineGeometry.PolyLine().add(pYF.get(21)).add(pY3.get(21)))
						;
					return group;
				}
				
				private static LineGeometry.IndexedLineSet create_CORRIDOR_WATER() {
					LineGeometry.PolyLine pZN,pZNF,pZPF,pZP;
					LineGeometry.GroupingNode group = new LineGeometry.GroupingNode()
							.add(pZN = new LineGeometry.PolyLine()
									.add(0, -width2/2, -(raster-spacing)/2)
									.addArc(LineGeometry.Axis.Z, new Point3D(height-radius, 0, -(raster-spacing)/2       ), radius, 180+bottomAngle/2, 180-bottomAngle/2, false, 20)
									.add(0, +width2/2, -(raster-spacing)/2)
									.close()
							)
							.add(pZNF = pZN.getCopy(0, 0, flange))
							.add(pZP  = pZN.getCopy(0, 0, raster-spacing))
							.add(pZPF = pZP.getCopy(0, 0,-flange))
							.add(new LineGeometry.PolyLine().add(pZN.get( 0)).add(pZNF.get( 0)).add(pZPF.get( 0)).add(pZP.get( 0)))
							.add(new LineGeometry.PolyLine().add(pZN.get( 1)).add(pZNF.get( 1)).add(pZPF.get( 1)).add(pZP.get( 1)))
							.add(new LineGeometry.PolyLine().add(pZN.get( 5)).add(pZNF.get( 5)).add(pZPF.get( 5)).add(pZP.get( 5)))
							.add(new LineGeometry.PolyLine().add(pZN.get(11)).add(pZNF.get(11)).add(pZPF.get(11)).add(pZP.get(11)))
							.add(new LineGeometry.PolyLine().add(pZN.get(17)).add(pZNF.get(17)).add(pZPF.get(17)).add(pZP.get(17)))
							.add(new LineGeometry.PolyLine().add(pZN.get(21)).add(pZNF.get(21)).add(pZPF.get(21)).add(pZP.get(21)))
							.add(new LineGeometry.PolyLine().add(pZN.get(22)).add(pZNF.get(22)).add(pZPF.get(22)).add(pZP.get(22)))
							;
					return group;
				}

				private static LineGeometry.IndexedLineSet create_CORRIDORL_WATER() {
					LineGeometry.PolyLine pZN,pZNF,pD,pYNF,pYN;
					LineGeometry.GroupingNode group = new LineGeometry.GroupingNode()
							.add(pZN = new LineGeometry.PolyLine()
									.add(0, -width2/2, -(raster-spacing)/2)
									.addArc(LineGeometry.Axis.Z, new Point3D(height-radius, 0, -(raster-spacing)/2), radius, 180+bottomAngle/2, 180-bottomAngle/2, false, 20)
									.add(0, +width2/2, -(raster-spacing)/2)
									.close()
							)
							.add(pYN = new LineGeometry.PolyLine()
									.add(0, -(raster-spacing)/2, -width2/2)
									.addArc(LineGeometry.Axis.Y, new Point3D(height-radius, -(raster-spacing)/2, 0), radius, 270-bottomAngle/2, 270+bottomAngle/2, true, 20)
									.add(0, -(raster-spacing)/2, +width2/2)
									.close()
							)
							.add(pZNF = pZN.getCopy(0,0,flange))
							.add(pYNF = pYN.getCopy(0,flange,0))
							;
					
					pD = new LineGeometry.PolyLine();
					pZNF.forEachPointXYZ((x,y,z)->pD.add(x,y,y));
					group.add(pD);
					
					group
							.add(new LineGeometry.PolyLine().add(pZN.get( 0)).add(pZNF.get( 0)).add(pD.get( 0)).add(pYNF.get( 0)).add(pYN.get( 0)))
							.add(new LineGeometry.PolyLine().add(pZN.get( 1)).add(pZNF.get( 1)).add(pD.get( 1)).add(pYNF.get( 1)).add(pYN.get( 1)))
							.add(new LineGeometry.PolyLine().add(pZN.get( 5)).add(pZNF.get( 5)).add(pD.get( 5)).add(pYNF.get( 5)).add(pYN.get( 5)))
							.add(new LineGeometry.PolyLine().add(pZN.get(11)).add(pZNF.get(11)).add(pD.get(11)).add(pYNF.get(11)).add(pYN.get(11)))
							.add(new LineGeometry.PolyLine().add(pZN.get(17)).add(pZNF.get(17)).add(pD.get(17)).add(pYNF.get(17)).add(pYN.get(17)))
							.add(new LineGeometry.PolyLine().add(pZN.get(21)).add(pZNF.get(21)).add(pD.get(21)).add(pYNF.get(21)).add(pYN.get(21)))
							.add(new LineGeometry.PolyLine().add(pZN.get(22)).add(pZNF.get(22)).add(pD.get(22)).add(pYNF.get(22)).add(pYN.get(22)))
							;
					return group;
				}

				private static LineGeometry.IndexedLineSet create_CORRIDORX_WATER() {
					LineGeometry.PolyLine pZN,pZNF,pZP,pZPF,pYN,pYNF,pYP,pYPF;
					LineGeometry.GroupingNode group = new LineGeometry.GroupingNode()
							.add(pZN = new LineGeometry.PolyLine()
									.add(0, -width2/2, -(raster-spacing)/2)
									.addArc(LineGeometry.Axis.Z, new Point3D(height-radius, 0, -(raster-spacing)/2), radius, 180+bottomAngle/2, 180-bottomAngle/2, false, 20)
									.add(0, +width2/2, -(raster-spacing)/2)
									.close()
							)
							.add(pYN = new LineGeometry.PolyLine()
									.add(0, -(raster-spacing)/2, -width2/2)
									.addArc(LineGeometry.Axis.Y, new Point3D(height-radius, -(raster-spacing)/2, 0), radius, 270-bottomAngle/2, 270+bottomAngle/2, true, 20)
									.add(0, -(raster-spacing)/2, +width2/2)
									.close()
							)
							.add(pZP  = pZN.getCopy(0, 0, raster-spacing))
							.add(pYP  = pYN.getCopy(0, raster-spacing, 0))
							.add(pZNF = pZN.getCopy(0,0, flange))
							.add(pZPF = pZP.getCopy(0,0,-flange))
							.add(pYNF = pYN.getCopy(0, flange,0))
							.add(pYPF = pYP.getCopy(0,-flange,0))
							;
					
					LineGeometry.PolyLine pD1,pD2;
					pD1 = new LineGeometry.PolyLine();
					pD2 = new LineGeometry.PolyLine();
					pZNF.forEachPointXYZ((x,y,z)->pD1.add(x,y, y));
					pZNF.forEachPointXYZ((x,y,z)->pD2.add(x,y,-y));
					group.add(pD1);
					group.add(pD2);
					
					group
							.add(new LineGeometry.PolyLine().add(pZN.get(11)).add(pZNF.get(11)).add(pD1.get(11)).add(pZPF.get(11)).add(pZP.get(11)))
							.add(new LineGeometry.PolyLine().add(pYN.get(11)).add(pYNF.get(11)).add(pD2.get(11)).add(pYPF.get(11)).add(pYP.get(11)))
							
							.add(new LineGeometry.PolyLine().add(pZN.get( 0)).add(pZNF.get( 0)).add(pD1.get( 0)).add(pYNF.get( 0)).add(pYN.get( 0)))
							.add(new LineGeometry.PolyLine().add(pZN.get( 1)).add(pZNF.get( 1)).add(pD1.get( 1)).add(pYNF.get( 1)).add(pYN.get( 1)))
							.add(new LineGeometry.PolyLine().add(pZN.get( 5)).add(pZNF.get( 5)).add(pD1.get( 5)).add(pYNF.get( 5)).add(pYN.get( 5)))
							
							.add(new LineGeometry.PolyLine().add(pZP.get(17)).add(pZPF.get(17)).add(pD1.get(17)).add(pYPF.get(17)).add(pYP.get(17)))
							.add(new LineGeometry.PolyLine().add(pZP.get(21)).add(pZPF.get(21)).add(pD1.get(21)).add(pYPF.get(21)).add(pYP.get(21)))
							.add(new LineGeometry.PolyLine().add(pZP.get(22)).add(pZPF.get(22)).add(pD1.get(22)).add(pYPF.get(22)).add(pYP.get(22)))
							
							.add(new LineGeometry.PolyLine().add(pZN.get(17)).add(pZNF.get(17)).add(pD2.get(17)).add(pYPF.get( 5)).add(pYP.get( 5)))
							.add(new LineGeometry.PolyLine().add(pZN.get(21)).add(pZNF.get(21)).add(pD2.get(21)).add(pYPF.get( 1)).add(pYP.get( 1)))
							.add(new LineGeometry.PolyLine().add(pZN.get(22)).add(pZNF.get(22)).add(pD2.get(22)).add(pYPF.get( 0)).add(pYP.get( 0)))
							
							.add(new LineGeometry.PolyLine().add(pZP.get( 5)).add(pZPF.get( 5)).add(pD2.get( 5)).add(pYNF.get(17)).add(pYN.get(17)))
							.add(new LineGeometry.PolyLine().add(pZP.get( 1)).add(pZPF.get( 1)).add(pD2.get( 1)).add(pYNF.get(21)).add(pYN.get(21)))
							.add(new LineGeometry.PolyLine().add(pZP.get( 0)).add(pZPF.get( 0)).add(pD2.get( 0)).add(pYNF.get(22)).add(pYN.get(22)))
							;
					return group;
				}
				
				private static LineGeometry.IndexedLineSet create_CORRIDORT_WATER() {
					LineGeometry.PolyLine pZN,pZNF,pD,pYN,pYNF,pYP,pYPF;
					LineGeometry.GroupingNode group = new LineGeometry.GroupingNode()
							.add(pZN = new LineGeometry.PolyLine()
									.add(0, -width2/2, -(raster-spacing)/2)
									.addArc(LineGeometry.Axis.Z, new Point3D(height-radius, 0, -(raster-spacing)/2), radius, 180+bottomAngle/2, 180-bottomAngle/2, false, 20)
									.add(0, +width2/2, -(raster-spacing)/2)
									.close()
							)
							.add(pYN = new LineGeometry.PolyLine()
									.add(0, -(raster-spacing)/2, -width2/2)
									.addArc(LineGeometry.Axis.Y, new Point3D(height-radius, -(raster-spacing)/2, 0), radius, 270-bottomAngle/2, 270+bottomAngle/2, true, 20)
									.add(0, -(raster-spacing)/2, +width2/2)
									.close()
							)
							.add(pYP  = pYN.getCopy(0,raster-spacing,0))
							.add(pZNF = pZN.getCopy(0,0,flange))
							.add(pYNF = pYN.getCopy(0, flange,0))
							.add(pYPF = pYP.getCopy(0,-flange,0))
							;
					
					pD = new LineGeometry.PolyLine();
					pZNF.forEachPointXYZ((x,y,z)->pD.add(x,y,-Math.abs(y)));
					group.add(pD);
					
					group
							.add(new LineGeometry.PolyLine().add(pZN.get( 0)).add(pZNF.get( 0)).add(pD.get( 0)).add(pYNF.get( 0)).add(pYN.get( 0)))
							.add(new LineGeometry.PolyLine().add(pZN.get( 1)).add(pZNF.get( 1)).add(pD.get( 1)).add(pYNF.get( 1)).add(pYN.get( 1)))
							.add(new LineGeometry.PolyLine().add(pZN.get( 5)).add(pZNF.get( 5)).add(pD.get( 5)).add(pYNF.get( 5)).add(pYN.get( 5)))
							
					//		.add(new LineGeometry.PolyLine().add(pZN.get(11)).add(pZNF.get(11)).add(pD.get(11)).add(pYNF.get(11)).add(pYN.get(11)))
							.add(new LineGeometry.PolyLine().add(pZN.get(17)).add(pZNF.get(17)).add(pD.get(17)).add(pYPF.get( 5)).add(pYP.get( 5)))
							.add(new LineGeometry.PolyLine().add(pZN.get(21)).add(pZNF.get(21)).add(pD.get(21)).add(pYPF.get( 1)).add(pYP.get( 1)))
							.add(new LineGeometry.PolyLine().add(pZN.get(22)).add(pZNF.get(22)).add(pD.get(22)).add(pYPF.get( 0)).add(pYP.get( 0)))
							
							.add(new LineGeometry.PolyLine().add(pYN.get(11)).add(pYNF.get(11)).add(pYPF.get(11)).add(pYP.get(11)))
							.add(new LineGeometry.PolyLine().add(pYN.get(17)).add(pYNF.get(17)).add(pYPF.get(17)).add(pYP.get(17)))
							.add(new LineGeometry.PolyLine().add(pYN.get(21)).add(pYNF.get(21)).add(pYPF.get(21)).add(pYP.get(21)))
							.add(new LineGeometry.PolyLine().add(pYN.get(22)).add(pYNF.get(22)).add(pYPF.get(22)).add(pYP.get(22)))
							
							.add(new LineGeometry.PolyLine().add(pZN.get(11)).add(pZNF.get(11)).add(pD.get(11)))
							;
					return group;
				}

				private static LineGeometry.IndexedLineSet create_CORRIDOR() {
					return new LineGeometry.Prism(LineGeometry.Axis.Z, raster-spacing,
							new Point3D(                      0,  width2/2, 0),
							new Point3D(       (width-width2)/2,  width /2, 0),
							new Point3D(height-(width-width2)/2,  width /2, 0),
							new Point3D(height                 ,  width2/2, 0),
							new Point3D(height                 , -width2/2, 0),
							new Point3D(height-(width-width2)/2, -width /2, 0),
							new Point3D(       (width-width2)/2, -width /2, 0),
							new Point3D(                      0, -width2/2, 0)
					);
				}

				private static LineGeometry.IndexedLineSet create_CORRIDORL() {
					
					Point3D[] profile1 = new Point3D[8];
					Point3D[] profile2 = new Point3D[8];
					Point3D[] profile3 = new Point3D[8];
					LineGeometry.GroupingNode group = new LineGeometry.GroupingNode()
							.add(new LineGeometry.PolyLine(
									profile1[0] = new Point3D(                      0,  width2/2, -(raster-spacing)/2),
									profile1[1] = new Point3D(       (width-width2)/2,  width /2, -(raster-spacing)/2),
									profile1[2] = new Point3D(height-(width-width2)/2,  width /2, -(raster-spacing)/2),
									profile1[3] = new Point3D(height                 ,  width2/2, -(raster-spacing)/2),
									profile1[4] = new Point3D(height                 , -width2/2, -(raster-spacing)/2),
									profile1[5] = new Point3D(height-(width-width2)/2, -width /2, -(raster-spacing)/2),
									profile1[6] = new Point3D(       (width-width2)/2, -width /2, -(raster-spacing)/2),
									profile1[7] = new Point3D(                      0, -width2/2, -(raster-spacing)/2)
							).close())
							.add(new LineGeometry.PolyLine(
									profile2[0] = new Point3D(                      0,  width2/2,  width2/2),
									profile2[1] = new Point3D(       (width-width2)/2,  width /2,  width /2),
									profile2[2] = new Point3D(height-(width-width2)/2,  width /2,  width /2),
									profile2[3] = new Point3D(height                 ,  width2/2,  width2/2)
							))
							.add(new LineGeometry.PolyLine(
									profile2[4] = new Point3D(height                 , -width2/2, -width2/2),
									profile2[5] = new Point3D(height-(width-width2)/2, -width /2, -width /2),
									profile2[6] = new Point3D(       (width-width2)/2, -width /2, -width /2),
									profile2[7] = new Point3D(                      0, -width2/2, -width2/2)
							))
							.add(new LineGeometry.PolyLine(
									profile3[0] = new Point3D(                      0, -(raster-spacing)/2,  width2/2),
									profile3[1] = new Point3D(       (width-width2)/2, -(raster-spacing)/2,  width /2),
									profile3[2] = new Point3D(height-(width-width2)/2, -(raster-spacing)/2,  width /2),
									profile3[3] = new Point3D(height                 , -(raster-spacing)/2,  width2/2),
									profile3[4] = new Point3D(height                 , -(raster-spacing)/2, -width2/2),
									profile3[5] = new Point3D(height-(width-width2)/2, -(raster-spacing)/2, -width /2),
									profile3[6] = new Point3D(       (width-width2)/2, -(raster-spacing)/2, -width /2),
									profile3[7] = new Point3D(                      0, -(raster-spacing)/2, -width2/2)
							).close());
					for (int i=0; i<8; i++)
						group.add(new LineGeometry.PolyLine(profile1[i],profile2[i],profile3[i]));
					
					return group;
				}

				private static LineGeometry.IndexedLineSet create_CORRIDORX() {
					Point3D[] profileYNZN = new Point3D[4];
					Point3D[] profileYNZP = new Point3D[4];
					Point3D[] profileYPZN = new Point3D[4];
					Point3D[] profileYPZP = new Point3D[4];
					Point3D[] profileZNYN = new Point3D[4];
					Point3D[] profileZNYP = new Point3D[4];
					Point3D[] profileZPYN = new Point3D[4];
					Point3D[] profileZPYP = new Point3D[4];
					Point3D[] profileNN = new Point3D[4];
					Point3D[] profileNP = new Point3D[4];
					Point3D[] profilePN = new Point3D[4];
					Point3D[] profilePP = new Point3D[4];
					LineGeometry.GroupingNode group = new LineGeometry.GroupingNode()
							.add(new LineGeometry.PolyLine(
									profileZNYP[0] = new Point3D(                      0,  width2/2, -(raster-spacing)/2),
									profileZNYP[1] = new Point3D(       (width-width2)/2,  width /2, -(raster-spacing)/2),
									profileZNYP[2] = new Point3D(height-(width-width2)/2,  width /2, -(raster-spacing)/2),
									profileZNYP[3] = new Point3D(height                 ,  width2/2, -(raster-spacing)/2),
									profileZNYN[3] = new Point3D(height                 , -width2/2, -(raster-spacing)/2),
									profileZNYN[2] = new Point3D(height-(width-width2)/2, -width /2, -(raster-spacing)/2),
									profileZNYN[1] = new Point3D(       (width-width2)/2, -width /2, -(raster-spacing)/2),
									profileZNYN[0] = new Point3D(                      0, -width2/2, -(raster-spacing)/2)
							).close())
							.add(new LineGeometry.PolyLine(
									profileYNZP[0] = new Point3D(                      0, -(raster-spacing)/2,  width2/2),
									profileYNZP[1] = new Point3D(       (width-width2)/2, -(raster-spacing)/2,  width /2),
									profileYNZP[2] = new Point3D(height-(width-width2)/2, -(raster-spacing)/2,  width /2),
									profileYNZP[3] = new Point3D(height                 , -(raster-spacing)/2,  width2/2),
									profileYNZN[3] = new Point3D(height                 , -(raster-spacing)/2, -width2/2),
									profileYNZN[2] = new Point3D(height-(width-width2)/2, -(raster-spacing)/2, -width /2),
									profileYNZN[1] = new Point3D(       (width-width2)/2, -(raster-spacing)/2, -width /2),
									profileYNZN[0] = new Point3D(                      0, -(raster-spacing)/2, -width2/2)
							).close())
							.add(new LineGeometry.PolyLine(
									profileZPYP[0] = new Point3D(                      0,  width2/2,  (raster-spacing)/2),
									profileZPYP[1] = new Point3D(       (width-width2)/2,  width /2,  (raster-spacing)/2),
									profileZPYP[2] = new Point3D(height-(width-width2)/2,  width /2,  (raster-spacing)/2),
									profileZPYP[3] = new Point3D(height                 ,  width2/2,  (raster-spacing)/2),
									profileZPYN[3] = new Point3D(height                 , -width2/2,  (raster-spacing)/2),
									profileZPYN[2] = new Point3D(height-(width-width2)/2, -width /2,  (raster-spacing)/2),
									profileZPYN[1] = new Point3D(       (width-width2)/2, -width /2,  (raster-spacing)/2),
									profileZPYN[0] = new Point3D(                      0, -width2/2,  (raster-spacing)/2)
							).close())
							.add(new LineGeometry.PolyLine(
									profileYPZP[0] = new Point3D(                      0,  (raster-spacing)/2,  width2/2),
									profileYPZP[1] = new Point3D(       (width-width2)/2,  (raster-spacing)/2,  width /2),
									profileYPZP[2] = new Point3D(height-(width-width2)/2,  (raster-spacing)/2,  width /2),
									profileYPZP[3] = new Point3D(height                 ,  (raster-spacing)/2,  width2/2),
									profileYPZN[3] = new Point3D(height                 ,  (raster-spacing)/2, -width2/2),
									profileYPZN[2] = new Point3D(height-(width-width2)/2,  (raster-spacing)/2, -width /2),
									profileYPZN[1] = new Point3D(       (width-width2)/2,  (raster-spacing)/2, -width /2),
									profileYPZN[0] = new Point3D(                      0,  (raster-spacing)/2, -width2/2)
							).close())
							.add(new LineGeometry.PolyLine(
									profilePP[0] = new Point3D(                      0,  width2/2,  width2/2),
									profilePP[1] = new Point3D(       (width-width2)/2,  width /2,  width /2),
									profilePP[2] = new Point3D(height-(width-width2)/2,  width /2,  width /2),
									profilePP[3] = new Point3D(height                 ,  width2/2,  width2/2)
							))
							.add(new LineGeometry.PolyLine(
									profilePN[0] = new Point3D(                      0,  width2/2, -width2/2),
									profilePN[1] = new Point3D(       (width-width2)/2,  width /2, -width /2),
									profilePN[2] = new Point3D(height-(width-width2)/2,  width /2, -width /2),
									profilePN[3] = new Point3D(height                 ,  width2/2, -width2/2)
							))
							.add(new LineGeometry.PolyLine(
									profileNP[0] = new Point3D(                      0, -width2/2,  width2/2),
									profileNP[1] = new Point3D(       (width-width2)/2, -width /2,  width /2),
									profileNP[2] = new Point3D(height-(width-width2)/2, -width /2,  width /2),
									profileNP[3] = new Point3D(height                 , -width2/2,  width2/2)
							))
							.add(new LineGeometry.PolyLine(
									profileNN[0] = new Point3D(                      0, -width2/2, -width2/2),
									profileNN[1] = new Point3D(       (width-width2)/2, -width /2, -width /2),
									profileNN[2] = new Point3D(height-(width-width2)/2, -width /2, -width /2),
									profileNN[3] = new Point3D(height                 , -width2/2, -width2/2)
							));
					for (int i=0; i<4; i++) {
						group.add(new LineGeometry.PolyLine(profileYNZN[i],profileNN[i],profileZNYN[i]));
						group.add(new LineGeometry.PolyLine(profileYPZN[i],profilePN[i],profileZNYP[i]));
						group.add(new LineGeometry.PolyLine(profileYNZP[i],profileNP[i],profileZPYN[i]));
						group.add(new LineGeometry.PolyLine(profileYPZP[i],profilePP[i],profileZPYP[i]));
					}
					
					return group;
				}

				private static LineGeometry.IndexedLineSet create_CORRIDORT() {
					Point3D[] profileYNZN = new Point3D[4];
					Point3D[] profileYNZP = new Point3D[4];
					Point3D[] profileYPZN = new Point3D[4];
					Point3D[] profileYPZP = new Point3D[4];
					Point3D[] profileZNYN = new Point3D[4];
					Point3D[] profileZNYP = new Point3D[4];
					Point3D[] profileNN = new Point3D[4];
					Point3D[] profilePN = new Point3D[4];
					LineGeometry.GroupingNode group = new LineGeometry.GroupingNode()
							.add(new LineGeometry.PolyLine(
									profileZNYP[0] = new Point3D(                      0,  width2/2, -(raster-spacing)/2),
									profileZNYP[1] = new Point3D(       (width-width2)/2,  width /2, -(raster-spacing)/2),
									profileZNYP[2] = new Point3D(height-(width-width2)/2,  width /2, -(raster-spacing)/2),
									profileZNYP[3] = new Point3D(height                 ,  width2/2, -(raster-spacing)/2),
									profileZNYN[3] = new Point3D(height                 , -width2/2, -(raster-spacing)/2),
									profileZNYN[2] = new Point3D(height-(width-width2)/2, -width /2, -(raster-spacing)/2),
									profileZNYN[1] = new Point3D(       (width-width2)/2, -width /2, -(raster-spacing)/2),
									profileZNYN[0] = new Point3D(                      0, -width2/2, -(raster-spacing)/2)
							).close())
							.add(new LineGeometry.PolyLine(
									profileYNZP[0] = new Point3D(                      0, -(raster-spacing)/2,  width2/2),
									profileYNZP[1] = new Point3D(       (width-width2)/2, -(raster-spacing)/2,  width /2),
									profileYNZP[2] = new Point3D(height-(width-width2)/2, -(raster-spacing)/2,  width /2),
									profileYNZP[3] = new Point3D(height                 , -(raster-spacing)/2,  width2/2),
									profileYNZN[3] = new Point3D(height                 , -(raster-spacing)/2, -width2/2),
									profileYNZN[2] = new Point3D(height-(width-width2)/2, -(raster-spacing)/2, -width /2),
									profileYNZN[1] = new Point3D(       (width-width2)/2, -(raster-spacing)/2, -width /2),
									profileYNZN[0] = new Point3D(                      0, -(raster-spacing)/2, -width2/2)
							).close())
							.add(new LineGeometry.PolyLine(
									profileYPZP[0] = new Point3D(                      0,  (raster-spacing)/2,  width2/2),
									profileYPZP[1] = new Point3D(       (width-width2)/2,  (raster-spacing)/2,  width /2),
									profileYPZP[2] = new Point3D(height-(width-width2)/2,  (raster-spacing)/2,  width /2),
									profileYPZP[3] = new Point3D(height                 ,  (raster-spacing)/2,  width2/2),
									profileYPZN[3] = new Point3D(height                 ,  (raster-spacing)/2, -width2/2),
									profileYPZN[2] = new Point3D(height-(width-width2)/2,  (raster-spacing)/2, -width /2),
									profileYPZN[1] = new Point3D(       (width-width2)/2,  (raster-spacing)/2, -width /2),
									profileYPZN[0] = new Point3D(                      0,  (raster-spacing)/2, -width2/2)
							).close())
							.add(new LineGeometry.PolyLine(
									profilePN[0] = new Point3D(                      0,  width2/2, -width2/2),
									profilePN[1] = new Point3D(       (width-width2)/2,  width /2, -width /2),
									profilePN[2] = new Point3D(height-(width-width2)/2,  width /2, -width /2),
									profilePN[3] = new Point3D(height                 ,  width2/2, -width2/2)
							))
							.add(new LineGeometry.PolyLine(
									profileNN[0] = new Point3D(                      0, -width2/2, -width2/2),
									profileNN[1] = new Point3D(       (width-width2)/2, -width /2, -width /2),
									profileNN[2] = new Point3D(height-(width-width2)/2, -width /2, -width /2),
									profileNN[3] = new Point3D(height                 , -width2/2, -width2/2)
							));
					for (int i=0; i<4; i++) {
						group.add(new LineGeometry.PolyLine(profileYNZN[i],profileNN[i],profileZNYN[i]));
						group.add(new LineGeometry.PolyLine(profileYPZN[i],profilePN[i],profileZNYP[i]));
						group.add(new LineGeometry.PolyLine(profileYNZP[i],profileYPZP[i]));
					}
					
					return group;
				}

				private static LineGeometry.MultipleIndexedLineSets create_CORRIDORC() {
					LineGeometry.GroupingNode body = new LineGeometry.GroupingNode()
							.add(new LineGeometry.PolyLine(
									new Point3D(                      0, raster-spacing/2, +width2/2-raster/2),
									new Point3D(       (width-width2)/2, raster-spacing/2, +width /2-raster/2),
									new Point3D(height-(width-width2)/2, raster-spacing/2, +width /2-raster/2),
									new Point3D(height                 , raster-spacing/2, +width2/2-raster/2),
									new Point3D(height                 , raster-spacing/2, -width2/2-raster/2),
									new Point3D(height-(width-width2)/2, raster-spacing/2, -width /2-raster/2),
									new Point3D(       (width-width2)/2, raster-spacing/2, -width /2-raster/2),
									new Point3D(                      0, raster-spacing/2, -width2/2-raster/2)
							).close())
							.add(new LineGeometry.PolyLine(
									new Point3D(                      0, +width2/2-raster/2, raster-spacing/2),
									new Point3D(       (width-width2)/2, +width /2-raster/2, raster-spacing/2),
									new Point3D(height-(width-width2)/2, +width /2-raster/2, raster-spacing/2),
									new Point3D(height                 , +width2/2-raster/2, raster-spacing/2),
									new Point3D(height                 , -width2/2-raster/2, raster-spacing/2),
									new Point3D(height-(width-width2)/2, -width /2-raster/2, raster-spacing/2),
									new Point3D(       (width-width2)/2, -width /2-raster/2, raster-spacing/2),
									new Point3D(                      0, -width2/2-raster/2, raster-spacing/2)
							).close())
							.add(new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(                      0, raster-spacing/2, raster-spacing/2), +width2/2 + raster*1.5-spacing/2, 180, 270, false, 18))
							.add(new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(       (width-width2)/2, raster-spacing/2, raster-spacing/2), +width /2 + raster*1.5-spacing/2, 180, 270, false, 18))
					//		.add(new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(height-(width-width2)/2, raster-spacing/2, raster-spacing/2), +width /2 + raster*1.5-spacing/2, 180, 270, false, 18))
					//		.add(new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(height                 , raster-spacing/2, raster-spacing/2), +width2/2 + raster*1.5-spacing/2, 180, 270, false, 18))
							.add(new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(height                 , raster-spacing/2, raster-spacing/2), -width2/2 + raster*1.5-spacing/2, 180, 270, false, 18))
							.add(new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(height-(width-width2)/2, raster-spacing/2, raster-spacing/2), -width /2 + raster*1.5-spacing/2, 180, 270, false, 18))
							.add(new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(       (width-width2)/2, raster-spacing/2, raster-spacing/2), -width /2 + raster*1.5-spacing/2, 180, 270, false, 18))
							.add(new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(                      0, raster-spacing/2, raster-spacing/2), -width2/2 + raster*1.5-spacing/2, 180, 270, false, 18))
							;
					int nSeg = 6;
					double segAngle_deg = 90/nSeg;
					double startAngle_deg = 180 + 0.5;
					double endAngle_deg   = 180 + segAngle_deg-0.5;
					double wW = width2/6;
					
					LineGeometry.PolyLine pl1,pl2,pl3,pl4;
					LineGeometry.GroupingNode window = new LineGeometry.GroupingNode()
							.add(pl1 = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(heightW                , 0, 0),   +width /2 + raster*1.5-spacing/2, startAngle_deg, endAngle_deg, false, 3))
							.add(pl2 = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(height-(width-width2)/2, 0, 0),   +width /2 + raster*1.5-spacing/2, startAngle_deg, endAngle_deg, false, 3))
							.add(pl3 = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(height                 , 0, 0),   +width2/2 + raster*1.5-spacing/2, startAngle_deg, endAngle_deg, false, 3))
							.add(pl4 = new LineGeometry.PolyLine().addArc(LineGeometry.Axis.X, new Point3D(height                 , 0, 0), wW-width2/2 + raster*1.5-spacing/2, startAngle_deg, endAngle_deg, false, 3))
							.add(new LineGeometry.PolyLine( pl1.getFirst(), pl2.getFirst(), pl3.getFirst(), pl4.getFirst() ))
							.add(new LineGeometry.PolyLine( pl1.getLast (), pl2.getLast (), pl3.getLast (), pl4.getLast () ))
							;
					
					LineGeometry.MultipleIndexedLineSets multi = new LineGeometry.MultipleIndexedLineSets()
							.add(body)
							.add(COLOR_WINDOW, new LineGeometry.Transform(window)                                                 .addTranslation(0, raster-spacing/2, raster-spacing/2))
							.add(COLOR_WINDOW, new LineGeometry.Transform(window).addRotation(LineGeometry.Axis.X, segAngle_deg*1).addTranslation(0, raster-spacing/2, raster-spacing/2))
							.add(COLOR_WINDOW, new LineGeometry.Transform(window).addRotation(LineGeometry.Axis.X, segAngle_deg*2).addTranslation(0, raster-spacing/2, raster-spacing/2))
							.add(COLOR_WINDOW, new LineGeometry.Transform(window).addRotation(LineGeometry.Axis.X, segAngle_deg*3).addTranslation(0, raster-spacing/2, raster-spacing/2))
							.add(COLOR_WINDOW, new LineGeometry.Transform(window).addRotation(LineGeometry.Axis.X, segAngle_deg*4).addTranslation(0, raster-spacing/2, raster-spacing/2))
							.add(COLOR_WINDOW, new LineGeometry.Transform(window).addRotation(LineGeometry.Axis.X, segAngle_deg*5).addTranslation(0, raster-spacing/2, raster-spacing/2))
							;
					
					return multi;
				}

				private static LineGeometry.MultipleIndexedLineSets create_GLASSCORRIDOR() {
					Point3D[] profile1 = new Point3D[8];
					Point3D[] profile2 = new Point3D[8];
					LineGeometry.GroupingNode body = new LineGeometry.GroupingNode()
							.add(new LineGeometry.PolyLine(
									profile1[0] = new Point3D(                      0,  width2/2, -(raster-spacing)/2),
									profile1[1] = new Point3D(       (width-width2)/2,  width /2, -(raster-spacing)/2),
									profile1[2] = new Point3D(height-(width-width2)/2,  width /2, -(raster-spacing)/2),
									profile1[3] = new Point3D(height                 ,  width2/2, -(raster-spacing)/2),
									profile1[4] = new Point3D(height                 , -width2/2, -(raster-spacing)/2),
									profile1[5] = new Point3D(height-(width-width2)/2, -width /2, -(raster-spacing)/2),
									profile1[6] = new Point3D(       (width-width2)/2, -width /2, -(raster-spacing)/2),
									profile1[7] = new Point3D(                      0, -width2/2, -(raster-spacing)/2)
							).close())
							.add(new LineGeometry.PolyLine(
									profile2[0] = new Point3D(                      0,  width2/2,  (raster-spacing)/2),
									profile2[1] = new Point3D(       (width-width2)/2,  width /2,  (raster-spacing)/2),
									profile2[2] = new Point3D(height-(width-width2)/2,  width /2,  (raster-spacing)/2),
									profile2[3] = new Point3D(height                 ,  width2/2,  (raster-spacing)/2),
									profile2[4] = new Point3D(height                 , -width2/2,  (raster-spacing)/2),
									profile2[5] = new Point3D(height-(width-width2)/2, -width /2,  (raster-spacing)/2),
									profile2[6] = new Point3D(       (width-width2)/2, -width /2,  (raster-spacing)/2),
									profile2[7] = new Point3D(                      0, -width2/2,  (raster-spacing)/2)
							).close())
							.add(new LineGeometry.PolyLine(profile1[0],profile2[0]))
							.add(new LineGeometry.PolyLine(profile1[1],profile2[1]))
					//		.add(new LineGeometry.PolyLine(profile1[2],profile2[2]))
					//		.add(new LineGeometry.PolyLine(profile1[3],profile2[3]))
					//		.add(new LineGeometry.PolyLine(profile1[4],profile2[4]))
					//		.add(new LineGeometry.PolyLine(profile1[5],profile2[5]))
							.add(new LineGeometry.PolyLine(profile1[6],profile2[6]))
							.add(new LineGeometry.PolyLine(profile1[7],profile2[7]))
							;
					
					double spacingW = 0.05;
					double zPos1 = spacingW;
					double zPos2 = (raster-spacing)/2-spacingW;
					
					LineGeometry.GroupingNode window = new LineGeometry.GroupingNode()
							.add(new LineGeometry.PolyLine(
									profile1[1] = new Point3D(heightW                ,  width /2, zPos1),
									profile1[2] = new Point3D(height-(width-width2)/2,  width /2, zPos1),
									profile1[3] = new Point3D(height                 ,  width2/2, zPos1),
									profile1[4] = new Point3D(height                 , -width2/2, zPos1),
									profile1[5] = new Point3D(height-(width-width2)/2, -width /2, zPos1),
									profile1[6] = new Point3D(heightW                , -width /2, zPos1)
							))
							.add(new LineGeometry.PolyLine(
									profile2[1] = new Point3D(heightW                ,  width /2, zPos2),
									profile2[2] = new Point3D(height-(width-width2)/2,  width /2, zPos2),
									profile2[3] = new Point3D(height                 ,  width2/2, zPos2),
									profile2[4] = new Point3D(height                 , -width2/2, zPos2),
									profile2[5] = new Point3D(height-(width-width2)/2, -width /2, zPos2),
									profile2[6] = new Point3D(heightW                , -width /2, zPos2)
							))
					//		.add(new LineGeometry.PolyLine(profile1[0],profile2[0]))
							.add(new LineGeometry.PolyLine(profile1[1],profile2[1]))
							.add(new LineGeometry.PolyLine(profile1[2],profile2[2]))
							.add(new LineGeometry.PolyLine(profile1[3],profile2[3]))
							.add(new LineGeometry.PolyLine(profile1[4],profile2[4]))
							.add(new LineGeometry.PolyLine(profile1[5],profile2[5]))
							.add(new LineGeometry.PolyLine(profile1[6],profile2[6]))
					//		.add(new LineGeometry.PolyLine(profile1[7],profile2[7]))
							;
					
					LineGeometry.MultipleIndexedLineSets multi = new LineGeometry.MultipleIndexedLineSets()
							.add(body)
							.add(COLOR_WINDOW, window)
							.add(COLOR_WINDOW, new LineGeometry.Transform(window).addTranslation(0,0,-(raster-spacing)/2))
							;
					return multi;
				}

				private static LineGeometry.IndexedLineSet create_BUILDDOOR() {
					double plateThickness = 0.3;
					LineGeometry.Prism bigPlate = new LineGeometry.Prism(LineGeometry.Axis.Y, plateThickness,
							new Point3D(                      0, 0,  width2/2),
							new Point3D(       (width-width2)/2, 0,  width /2),
							new Point3D(height-(width-width2)/2, 0,  width /2),
							new Point3D(height                 , 0,  width2/2),
							new Point3D(height                 , 0, -width2/2),
							new Point3D(height-(width-width2)/2, 0, -width /2),
							new Point3D(       (width-width2)/2, 0, -width /2),
							new Point3D(                      0, 0, -width2/2)
					);
					double widthD2 = 0.75;
					double widthD  = 1.30;
					double heightD = 2.25;
					LineGeometry.Prism door = new LineGeometry.Prism(LineGeometry.Axis.Y, plateThickness,
							new Point3D(                         0 + 0.25, 0,  widthD2/2),
							new Point3D(        (widthD-widthD2)/2 + 0.25, 0,  widthD /2),
							new Point3D(heightD-(widthD-widthD2)/2 + 0.25, 0,  widthD /2),
							new Point3D(heightD                    + 0.25, 0,  widthD2/2),
							new Point3D(heightD                    + 0.25, 0, -widthD2/2),
							new Point3D(heightD-(widthD-widthD2)/2 + 0.25, 0, -widthD /2),
							new Point3D(        (widthD-widthD2)/2 + 0.25, 0, -widthD /2),
							new Point3D(                         0 + 0.25, 0, -widthD2/2)
					);
					return new LineGeometry.Transform(new LineGeometry.GroupingNode(bigPlate,door)).addTranslation(0,plateThickness/2,0);
				}

				private static LineGeometry.IndexedLineSet create_DOOR2() {
					double plateThickness = 0.3;
					double widthD2 = width2-0.35;
					double widthD  = width -0.7;
					double heightD = height-0.7;
					double depthD  = 0.6;
					double xOffsetD = height/2 - heightD/2;
					Point3D[] profile1 = new Point3D[8];
					Point3D[] profile2 = new Point3D[8];
					Point3D[] profile3 = new Point3D[8];
					LineGeometry.GroupingNode group = new LineGeometry.GroupingNode()
							.add(new LineGeometry.PolyLine(
									profile1[0] = new Point3D(                      0, 0,  width2/2),
									profile1[1] = new Point3D(       (width-width2)/2, 0,  width /2),
									profile1[2] = new Point3D(height-(width-width2)/2, 0,  width /2),
									profile1[3] = new Point3D(height                 , 0,  width2/2),
									profile1[4] = new Point3D(height                 , 0, -width2/2),
									profile1[5] = new Point3D(height-(width-width2)/2, 0, -width /2),
									profile1[6] = new Point3D(       (width-width2)/2, 0, -width /2),
									profile1[7] = new Point3D(                      0, 0, -width2/2)
							).close())
							.add(new LineGeometry.PolyLine(
									profile2[0] = new Point3D(                      0, plateThickness,  width2/2),
									profile2[1] = new Point3D(       (width-width2)/2, plateThickness,  width /2),
									profile2[2] = new Point3D(height-(width-width2)/2, plateThickness,  width /2),
									profile2[3] = new Point3D(height                 , plateThickness,  width2/2),
									profile2[4] = new Point3D(height                 , plateThickness, -width2/2),
									profile2[5] = new Point3D(height-(width-width2)/2, plateThickness, -width /2),
									profile2[6] = new Point3D(       (width-width2)/2, plateThickness, -width /2),
									profile2[7] = new Point3D(                      0, plateThickness, -width2/2)
							).close())
							.add(new LineGeometry.PolyLine(
									profile3[0] = new Point3D(                         0 + xOffsetD, plateThickness + depthD,  widthD2/2),
									profile3[1] = new Point3D(        (widthD-widthD2)/2 + xOffsetD, plateThickness + depthD,  widthD /2),
									profile3[2] = new Point3D(heightD-(widthD-widthD2)/2 + xOffsetD, plateThickness + depthD,  widthD /2),
									profile3[3] = new Point3D(heightD                    + xOffsetD, plateThickness + depthD,  widthD2/2),
									profile3[4] = new Point3D(heightD                    + xOffsetD, plateThickness + depthD, -widthD2/2),
									profile3[5] = new Point3D(heightD-(widthD-widthD2)/2 + xOffsetD, plateThickness + depthD, -widthD /2),
									profile3[6] = new Point3D(        (widthD-widthD2)/2 + xOffsetD, plateThickness + depthD, -widthD /2),
									profile3[7] = new Point3D(                         0 + xOffsetD, plateThickness + depthD, -widthD2/2)
							).close());
					for (int i=0; i<8; i++)
						group.add(new LineGeometry.PolyLine(profile1[i],profile2[i],profile3[i]));
					
					return group;
				}

				private static LineGeometry.IndexedLineSet create_BUILDRAMP() {
					double length = 2; // Y   400
					double width = 1.3; // Z  | Door Width
					// railing
					// total length : 400 == 2
					// length       : 381 -> 1.9
					// height       : 196 -> 0.95
					// round corner : 30  -> 0.15
					// max X        : 165 -> 0.8
					double railLength =  1.9; // Y   400
					double railMaxX   =  0.8; // X
					double railMinX   = -0.15; // X
					double railCRad   =  0.15;
					double railZPos   = width/2-0.05;
					
					LineGeometry.GroupingNode group = new LineGeometry.GroupingNode()
							.add(new LineGeometry.PolyLine()
									.add(0, length/2, width/2)
									.add(0,-length/2, width/2)
									.add(0,-length/2,-width/2)
									.add(0, length/2,-width/2)
									.close())
							.add(new LineGeometry.PolyLine()
									.addArc(LineGeometry.Axis.Z, new Point3D( railMinX+railCRad,  railLength/2-railCRad, railZPos), railCRad,  90, 180, false)
									.addArc(LineGeometry.Axis.Z, new Point3D( railMinX+railCRad, -railLength/2+railCRad, railZPos), railCRad, 180, 270, false)
									.addArc(LineGeometry.Axis.Z, new Point3D( railMaxX-railCRad, -railLength/2+railCRad, railZPos), railCRad, 270, 360, false)
									.addArc(LineGeometry.Axis.Z, new Point3D( railMaxX-railCRad,  railLength/2-railCRad, railZPos), railCRad,   0,  90, false)
									.close())
							.add(new LineGeometry.PolyLine()
									.addArc(LineGeometry.Axis.Z, new Point3D( railMinX+railCRad,  railLength/2-railCRad, -railZPos), railCRad,  90, 180, false)
									.addArc(LineGeometry.Axis.Z, new Point3D( railMinX+railCRad, -railLength/2+railCRad, -railZPos), railCRad, 180, 270, false)
									.addArc(LineGeometry.Axis.Z, new Point3D( railMaxX-railCRad, -railLength/2+railCRad, -railZPos), railCRad, 270, 360, false)
									.addArc(LineGeometry.Axis.Z, new Point3D( railMaxX-railCRad,  railLength/2-railCRad, -railZPos), railCRad,   0,  90, false)
									.close())
							;
					
					return group;
				}
			}
			
			private static class SolitaryWallsAndFloors {
				
				static void addModelsToModelMap() {
					addModels("__SOLITARY_DOOR",         makeVariations("^%s_DOOR"        , "W","C","M") ); 
					addModels("__SOLITARY_DOOR_HALF",    makeVariations("^%s_DOOR_H"      , "W","C","M") ); 
					addModels("__SOLITARY_WALL",         makeVariations("^%s_WALL"        , "W","C","M") );
					addModels("__SOLITARY_WALL_H",       makeVariations("^%s_WALL_H"      , "W","C","M") );
					addModels("__SOLITARY_WALL_Q",       makeVariations("^%s_WALL_Q"      , "W","C","M") );
					addModels("__SOLITARY_WALL_Q_H",     makeVariations("^%s_WALL_Q_H"    , "W","C","M") );
					addModels("__SOLITARY_WALLDIAGONAL", makeVariations("^%s_WALLDIAGONAL", "W","C","M") );
					addModels("__SOLITARY_ROOF_M",       makeVariations("^%s_ROOF_M"      , "W","C","M") );
					addModels("__SOLITARY_ROOF_C",       makeVariations("^%s_ROOF_C"      , "W","C","M") );
					addModels("__SOLITARY_ROOF_IC",      makeVariations("^%s_ROOF_IC"     , "W","C","M") );
					addModels("__SOLITARY_ROOF",         makeVariations("^%s_ROOF"        , "W","C","M") );
					addModels("__SOLITARY_FLOOR",        makeVariations("^%s_FLOOR"       , "W","C","M") );
					addModels("__SOLITARY_FLOOR_Q",      makeVariations("^%s_FLOOR_Q"     , "W","C","M") );
					addModels("__SOLITARY_DOORWINDOW",   makeVariations("^%s_DOORWINDOW"  , "W","C","M") );
					addModels("__SOLITARY_WALL_WINDOW",  makeVariations("^%s_WALL_WINDOW" , "W","C","M") );
					addModels("__SOLITARY_GLASSFLOOR",   makeVariations("^%s_GFLOOR"      , "W","C","M") );
					addModels("__SOLITARY_RAMP",         makeVariations("^%s_RAMP"        , "W","C","M") );
					addModels("__SOLITARY_RAMP_H",       makeVariations("^%s_RAMP_H"      , "W","C","M") );
					addModels("__SOLITARY_TRIFLOOR",     makeVariations("^%s_TRIFLOOR"    , "W","C","M") );
					addModels("__SOLITARY_TRIFLOOR_Q",   makeVariations("^%s_TRIFLOOR_Q"  , "W","C","M") );
					addModels("__SOLITARY_ARCH",         makeVariations("^%s_ARCH"        , "W","C","M") );
					addModels("__SOLITARY_ARCH_H",       makeVariations("^%s_ARCH_H"      , "W","C","M") );
					addModels("__SOLITARY_GDOOR",        makeVariations("^%s_GDOOR"       , "W","C","M") ); 
					
					//addModels("__SOLITARY_GDOOR_D",      makeVariations("^%s_GDOOR_D"     , "W","C","M") ); // unknown object
					//addModels("__SOLITARY_SDOOR",        makeVariations("^%s_SDOOR"       , "W","C","M") ); // can't be build in game
					
					addProto( new Proto("__SOLITARY_ARCH"        ,     create_ARCH        ()) );
					addProto( new Proto("__SOLITARY_ARCH_H"      , 12, create_ARCH_H      ()) );
					addProto( new Proto("__SOLITARY_ROOF"        ,     create_ROOF        ()) );
					addProto( new Proto("__SOLITARY_ROOF_M"      ,     create_ROOF_M      ()) );
					addProto( new Proto("__SOLITARY_ROOF_C"      ,     create_ROOF_C      ()) );
					addProto( new Proto("__SOLITARY_ROOF_IC"     ,     create_ROOF_IC     ()) );
					addProto( new Proto("__SOLITARY_WALLDIAGONAL",     create_WALLDIAGONAL()) );
					addProto( new Proto("__SOLITARY_DOOR"        ,     create_DOOR        ()) );
					addProto( new Proto("__SOLITARY_DOOR_HALF"   , 12, create_DOOR_HALF   ()) );
					addProto( new Proto("__SOLITARY_DOORWINDOW"  ,     create_DOORWINDOW  ()) );
					addProto( new Proto("__SOLITARY_GDOOR"       ,     create_GDOOR       ()) );
					addProto( new Proto("__SOLITARY_RAMP"        ,     create_RAMP        ()) );
					addProto( new Proto("__SOLITARY_RAMP_H"      , 12, create_RAMP_H      ()) );
					addProto( new Proto("__SOLITARY_WALL_WINDOW" ,     create_WALL_WINDOW ()) );
					addProto( new Proto("__SOLITARY_GLASSFLOOR"  ,     create_GLASSFLOOR  ()) );
					
					addProto( new Proto("__SOLITARY_FLOOR"       ,     new LineGeometry.Box(wall_thickness,wall_length  ,wall_length  )) );
					addProto( new Proto("__SOLITARY_FLOOR_Q"     , 12, new LineGeometry.Box(wall_thickness,wall_length/2,wall_length/2)) );
					addProto( new Proto("__SOLITARY_TRIFLOOR"    , 15, new LineGeometry.RegularPrism(LineGeometry.Axis.X,wall_thickness,3,wall_length  ,-90)) );
					addProto( new Proto("__SOLITARY_TRIFLOOR_Q"  , 0.5, 0, 17, new LineGeometry.RegularPrism(LineGeometry.Axis.X,wall_thickness,3,wall_length/2,-90)) );
					
					addProto( new Proto("__SOLITARY_WALL"        ,     createXFloorBasedBox(wall_height  ,wall_thickness,wall_length  )) );
					addProto( new Proto("__SOLITARY_WALL_H"      , 12, createXFloorBasedBox(wall_height  ,wall_thickness,wall_length/2)) );
					addProto( new Proto("__SOLITARY_WALL_Q"      ,     createXFloorBasedBox(wall_height/4,wall_thickness,wall_length  )) );
					addProto( new Proto("__SOLITARY_WALL_Q_H"    , 12, createXFloorBasedBox(wall_height/4,wall_thickness,wall_length/2)) );
				}

				private static final double wall_thickness = 0.3;
				private static final double wall_length    = 16.0/3.0;
				private static final double wall_height    = 10.0/3.0;
				private static final double door_width     = wall_length*0.3;
				private static final double door_height    = wall_height*3.0/4;
				private static final double border_h       = 0.75;
				private static final double border_v       = 0.65;
				private static final double spacing        = 0.30;

				private static LineGeometry.MultipleIndexedLineSets create_GLASSFLOOR() {
					double window_width = wall_length - 2*border_h;
					LineGeometry.MultipleIndexedLineSets lineSets = new LineGeometry.MultipleIndexedLineSets()
						.add(              new LineGeometry.Box(wall_thickness, wall_length, wall_length))
						.add(COLOR_WINDOW, new LineGeometry.Box(wall_thickness,window_width,window_width));
					return lineSets;
				}

				private static LineGeometry.MultipleIndexedLineSets create_WALL_WINDOW() {
					double window_width   = wall_length - 2*border_h;
					double window_height  = wall_height - 2*border_v;
					
					LineGeometry.MultipleIndexedLineSets lineSets = new LineGeometry.MultipleIndexedLineSets()
						.add(new LineGeometry.Transform( new LineGeometry.Box( wall_height, wall_thickness, wall_length ) )
								.addTranslation(new Point3D( wall_height/2,0,0 )))
						.add(COLOR_WINDOW, new LineGeometry.Transform( new LineGeometry.Box( window_height, wall_thickness, window_width ) )
								.addTranslation(new Point3D( window_height/2 + border_v, 0, 0)))
						;
					return lineSets;
				}

				private static LineGeometry.MultipleIndexedLineSets create_DOORWINDOW() {
					double window_width   = wall_length - border_h - spacing - door_width - border_h;
					double window_height  = wall_height - 2*border_v;
					double window_zOffset = wall_length/2 - border_h - window_width/2;
					double door_zOffset   = wall_length/2 - border_h - door_width/2;
					
					LineGeometry.GroupingNode group;
					group = new LineGeometry.GroupingNode();
					group.add(
						new LineGeometry.Transform( new LineGeometry.Box( wall_height, wall_thickness, wall_length ))
							.addTranslation(new Point3D( wall_height/2,0,0 )),
						
						new LineGeometry.Transform( new LineGeometry.Box( door_height, wall_thickness, door_width ) )
							.addTranslation(new Point3D( door_height/2 + wall_thickness/2, 0, -door_zOffset))
					);
					
					LineGeometry.MultipleIndexedLineSets lineSets = new LineGeometry.MultipleIndexedLineSets()
						.add(group)
						.add(COLOR_WINDOW, new LineGeometry.Transform( new LineGeometry.Box( window_height, wall_thickness, window_width ) )
								.addTranslation(new Point3D( window_height/2 + border_v, 0, window_zOffset)));
					return lineSets;
				}

				private static LineGeometry.Prism create_ARCH() {
					Vector<Point3D> points = new Vector<>();
					points.add( new Point3D( wall_height*5/5.0, 0, +wall_length*16/32.0 ) );
					points.add( new Point3D( 0                , 0, +wall_length*16/32.0 ) );
					points.add( new Point3D( 0                , 0, +wall_length*13/32.0 ) );
					points.add( new Point3D( wall_height*4/5.0, 0, +wall_length*11/32.0 ) );
					points.add( new Point3D( wall_height*4/5.0, 0, -wall_length*11/32.0 ) );
					points.add( new Point3D( 0                , 0, -wall_length*13/32.0 ) );
					points.add( new Point3D( 0                , 0, -wall_length*16/32.0 ) );
					points.add( new Point3D( wall_height*5/5.0, 0, -wall_length*16/32.0 ) );
					Point3D[] pointArr = points.toArray(new Point3D[points.size()]);
					
					return new LineGeometry.Prism(LineGeometry.Axis.Y, wall_thickness, pointArr);
				}

				private static LineGeometry.Prism create_ARCH_H() {
					Vector<Point3D> points = new Vector<>();
					points.add( new Point3D( wall_height*4/5.0, 0,  wall_length*8/32.0 ) );
					points.add( new Point3D( wall_height*4/5.0, 0, -wall_length*3/32.0 ) );
					points.add( new Point3D( 0                , 0, -wall_length*5/32.0 ) );
					points.add( new Point3D( 0                , 0, -wall_length*8/32.0 ) );
					points.add( new Point3D( wall_height*5/5.0, 0, -wall_length*8/32.0 ) );
					points.add( new Point3D( wall_height*5/5.0, 0,  wall_length*8/32.0 ) );
					Point3D[] pointArr = points.toArray(new Point3D[points.size()]);
					
					return new LineGeometry.Prism(LineGeometry.Axis.Y, wall_thickness, pointArr);
				}

				private static LineGeometry.GroupingNode create_ROOF() {
					LineGeometry.PolyLine polyLine;
					LineGeometry.GroupingNode group;
					
					group = new LineGeometry.GroupingNode();
					
					polyLine = new LineGeometry.PolyLine();
					polyLine.add(new Point3D(wall_thickness/2, wall_length/2   ,  wall_length/2   ));
					polyLine.add(new Point3D(wall_height*0.6 , wall_length*0.15,  wall_length*0.15));
					polyLine.add(new Point3D(wall_height*0.6 , wall_length*0.15, -wall_length*0.15));
					group.add(
						new LineGeometry.Box(wall_thickness,wall_length,wall_length),
						polyLine,
						new LineGeometry.Transform(polyLine).addRotation(LineGeometry.Axis.X,90),
						new LineGeometry.Transform(polyLine).addRotation(LineGeometry.Axis.X,180),
						new LineGeometry.Transform(polyLine).addRotation(LineGeometry.Axis.X,270)
					);
					
					return group;
				}

				private static LineGeometry.GroupingNode create_ROOF_M() {
					LineGeometry.PolyLine polyLine;
					LineGeometry.GroupingNode group;
					
					group = new LineGeometry.GroupingNode();
					
					polyLine = new LineGeometry.PolyLine();
					polyLine.add(new Point3D( -wall_thickness/2              , -wall_length/2, wall_length/2));
					polyLine.add(new Point3D(  wall_thickness/2              , -wall_length/2, wall_length/2));
					polyLine.add(new Point3D(  wall_thickness/2 + wall_height,  wall_length/2, wall_length/2));
					polyLine.add(new Point3D( -wall_thickness/2 + wall_height,  wall_length/2, wall_length/2));
					polyLine.close();
					group.add(
						polyLine,
						new LineGeometry.Transform(polyLine).addTranslation(new Point3D(0, 0, -wall_length)),
						new LineGeometry.PolyLine( new Point3D( -wall_thickness/2              , -wall_length/2, wall_length/2), new Point3D( -wall_thickness/2              , -wall_length/2, -wall_length/2) ),
						new LineGeometry.PolyLine( new Point3D(  wall_thickness/2              , -wall_length/2, wall_length/2), new Point3D(  wall_thickness/2              , -wall_length/2, -wall_length/2) ),
						new LineGeometry.PolyLine( new Point3D(  wall_thickness/2 + wall_height,  wall_length/2, wall_length/2), new Point3D(  wall_thickness/2 + wall_height,  wall_length/2, -wall_length/2) ),
						new LineGeometry.PolyLine( new Point3D( -wall_thickness/2 + wall_height,  wall_length/2, wall_length/2), new Point3D( -wall_thickness/2 + wall_height,  wall_length/2, -wall_length/2) )
					);
					
					return group;
				}

				private static LineGeometry.GroupingNode create_ROOF_C() {
					LineGeometry.PolyLine polyLine;
					LineGeometry.GroupingNode group;
					
					group = new LineGeometry.GroupingNode();
					
					polyLine = new LineGeometry.PolyLine();
					polyLine.add(new Point3D( -wall_thickness/2 + wall_height,  wall_length/2, -wall_length/2));
					polyLine.add(new Point3D( -wall_thickness/2              , -wall_length/2, -wall_length/2));
					polyLine.add(new Point3D(  wall_thickness/2              , -wall_length/2, -wall_length/2));
					polyLine.add(new Point3D(  wall_thickness/2 + wall_height,  wall_length/2, -wall_length/2));
					polyLine.close();
					group.add(polyLine);
					
					polyLine = new LineGeometry.PolyLine();
					polyLine.add(new Point3D( -wall_thickness/2 + wall_height,  wall_length/2, -wall_length/2));
					polyLine.add(new Point3D( -wall_thickness/2              , -wall_length/2,  wall_length/2));
					polyLine.add(new Point3D(  wall_thickness/2              , -wall_length/2,  wall_length/2));
					polyLine.add(new Point3D(  wall_thickness/2 + wall_height,  wall_length/2, -wall_length/2));
					group.add(polyLine);
					
					polyLine = new LineGeometry.PolyLine();
					polyLine.add(new Point3D( -wall_thickness/2 + wall_height,  wall_length/2, -wall_length/2));
					polyLine.add(new Point3D( -wall_thickness/2              ,  wall_length/2,  wall_length/2));
					polyLine.add(new Point3D(  wall_thickness/2              ,  wall_length/2,  wall_length/2));
					polyLine.add(new Point3D(  wall_thickness/2 + wall_height,  wall_length/2, -wall_length/2));
					group.add(polyLine);
					
					polyLine = new LineGeometry.PolyLine();
					polyLine.add(new Point3D( -wall_thickness/2, -wall_length/2, -wall_length/2));
					polyLine.add(new Point3D( -wall_thickness/2, -wall_length/2,  wall_length/2));
					polyLine.add(new Point3D( -wall_thickness/2,  wall_length/2,  wall_length/2));
					group.add(polyLine);
					
					polyLine = new LineGeometry.PolyLine();
					polyLine.add(new Point3D(  wall_thickness/2, -wall_length/2, -wall_length/2));
					polyLine.add(new Point3D(  wall_thickness/2, -wall_length/2,  wall_length/2));
					polyLine.add(new Point3D(  wall_thickness/2,  wall_length/2,  wall_length/2));
					group.add(polyLine);
					
					return group;
				}

				private static LineGeometry.GroupingNode create_ROOF_IC() {
					LineGeometry.PolyLine polyLine;
					LineGeometry.GroupingNode group;
					
					group = new LineGeometry.GroupingNode();
					
					polyLine = new LineGeometry.PolyLine();
					polyLine.add(new Point3D(  wall_thickness/2              ,  wall_length/2, -wall_length/2));
					polyLine.add(new Point3D(  wall_thickness/2 + wall_height, -wall_length/2, -wall_length/2));
					polyLine.add(new Point3D( -wall_thickness/2 + wall_height, -wall_length/2, -wall_length/2));
					polyLine.add(new Point3D( -wall_thickness/2              ,  wall_length/2, -wall_length/2));
					polyLine.close();
					group.add(polyLine);
					
					polyLine = new LineGeometry.PolyLine();
					polyLine.add(new Point3D(  wall_thickness/2              ,  wall_length/2, -wall_length/2));
					polyLine.add(new Point3D(  wall_thickness/2 + wall_height, -wall_length/2,  wall_length/2));
					polyLine.add(new Point3D( -wall_thickness/2 + wall_height, -wall_length/2,  wall_length/2));
					polyLine.add(new Point3D( -wall_thickness/2              ,  wall_length/2, -wall_length/2));
					group.add(polyLine);
					
					polyLine = new LineGeometry.PolyLine();
					polyLine.add(new Point3D(  wall_thickness/2              ,  wall_length/2, -wall_length/2));
					polyLine.add(new Point3D(  wall_thickness/2 + wall_height,  wall_length/2,  wall_length/2));
					polyLine.add(new Point3D( -wall_thickness/2 + wall_height,  wall_length/2,  wall_length/2));
					polyLine.add(new Point3D( -wall_thickness/2              ,  wall_length/2, -wall_length/2));
					group.add(polyLine);
					
					polyLine = new LineGeometry.PolyLine();
					polyLine.add(new Point3D( -wall_thickness/2 + wall_height, -wall_length/2, -wall_length/2));
					polyLine.add(new Point3D( -wall_thickness/2 + wall_height, -wall_length/2,  wall_length/2));
					polyLine.add(new Point3D( -wall_thickness/2 + wall_height,  wall_length/2,  wall_length/2));
					group.add(polyLine);
					
					polyLine = new LineGeometry.PolyLine();
					polyLine.add(new Point3D(  wall_thickness/2 + wall_height, -wall_length/2, -wall_length/2));
					polyLine.add(new Point3D(  wall_thickness/2 + wall_height, -wall_length/2,  wall_length/2));
					polyLine.add(new Point3D(  wall_thickness/2 + wall_height,  wall_length/2,  wall_length/2));
					group.add(polyLine);
					
					return group;
				}

				private static LineGeometry.GroupingNode create_WALLDIAGONAL() {
					LineGeometry.PolyLine polyLine;
					LineGeometry.GroupingNode group;
					
					group = new LineGeometry.GroupingNode();
					
					polyLine = new LineGeometry.PolyLine();
					polyLine.add(new Point3D( 0          , wall_thickness/2, -wall_length/2));
					polyLine.add(new Point3D( 0          , wall_thickness/2,  wall_length/2));
					polyLine.add(new Point3D( wall_height, wall_thickness/2,  wall_length/2));
					polyLine.close();
					group.add(
						polyLine,
						new LineGeometry.Transform(polyLine).addTranslation(new Point3D(0, -wall_thickness, 0)),
						new LineGeometry.PolyLine( new Point3D( 0          , wall_thickness/2, -wall_length/2), new Point3D( 0          , -wall_thickness/2, -wall_length/2) ),
						new LineGeometry.PolyLine( new Point3D( 0          , wall_thickness/2,  wall_length/2), new Point3D( 0          , -wall_thickness/2,  wall_length/2) ),
						new LineGeometry.PolyLine( new Point3D( wall_height, wall_thickness/2,  wall_length/2), new Point3D( wall_height, -wall_thickness/2,  wall_length/2) )
					);
					
					return group;
				}

				private static LineGeometry.GroupingNode create_DOOR() {
					LineGeometry.GroupingNode group;
					group = new LineGeometry.GroupingNode();
					group.add(
							
						new LineGeometry.Transform(
							new LineGeometry.Box( wall_height, wall_thickness, wall_length )
						).addTranslation(new Point3D( wall_height/2,0,0 )),
						
						new LineGeometry.Transform(
							new LineGeometry.Box( door_height, wall_thickness, door_width )
						).addTranslation(new Point3D( door_height/2 + wall_thickness/2,0,0))
					);
					
					return group;
				}

				private static LineGeometry.GroupingNode create_DOOR_HALF() {
					LineGeometry.GroupingNode group;
					group = new LineGeometry.GroupingNode();
					group.add(
							
						new LineGeometry.Transform(
							new LineGeometry.Box( wall_height, wall_thickness, wall_length/2 )
						).addTranslation(new Point3D( wall_height/2,0,0 )),
						
						new LineGeometry.Transform(
							new LineGeometry.Box( door_height, wall_thickness, door_width )
						).addTranslation(new Point3D( door_height/2 + wall_thickness/2,0,0))
					);
					
					return group;
				}

				private static LineGeometry.GroupingNode create_GDOOR() {
					Vector<Point3D> points = new Vector<>();
					points.add( new Point3D( wall_height*10/10.0, 0, +wall_length*8/16.0 ) );
					points.add( new Point3D( 0                  , 0, +wall_length*8/16.0 ) );
					points.add( new Point3D( 0                  , 0, +wall_length*7/16.0 ) );
					points.add( new Point3D( wall_height* 9/10.0, 0, +wall_length*7/16.0 ) );
					points.add( new Point3D( wall_height* 9/10.0, 0, -wall_length*7/16.0 ) );
					points.add( new Point3D( 0                  , 0, -wall_length*7/16.0 ) );
					points.add( new Point3D( 0                  , 0, -wall_length*8/16.0 ) );
					points.add( new Point3D( wall_height*10/10.0, 0, -wall_length*8/16.0 ) );
					
					Point3D[] pointArr = points.toArray(new Point3D[points.size()]);
					LineGeometry.Prism frame = new LineGeometry.Prism(LineGeometry.Axis.Y, wall_thickness, pointArr);
					
					LineGeometry.Transform door = new LineGeometry.Transform( new LineGeometry.Box( wall_height*9/10.0, wall_thickness/3, wall_length*14/16.0 ) )
							.addTranslation(new Point3D(-wall_height*1.5/10,0,0))
							.addRotation(LineGeometry.Axis.Z, -25)
							.addTranslation(new Point3D(+wall_height*6.0/10,0,0))
							;
					
					return new LineGeometry.GroupingNode(frame,door);
				}

				private static LineGeometry.GroupingNode create_RAMP() {
					LineGeometry.GroupingNode group;
					group = new LineGeometry.GroupingNode();
					
					int n_steps = 11;
					double step_thickness = wall_thickness*0.4;
					double step_width = wall_length/n_steps;
					
					LineGeometry.Box step = new LineGeometry.Box( step_thickness, step_width, wall_length );
					
					double offsetX0 = wall_thickness/2 - step_thickness/2;
					double offsetY0 = -wall_length/2 - step_width/2;
					double offsetZ0 = 0;
					double offsetX1 = offsetX0 + wall_height;
					double offsetY1 = offsetY0 + wall_length;
					double offsetZ1 = 0;
					
					Point3D offset = new Point3D(0,0,0);
					for (int i=1; i<=n_steps; i++) {
						offset.x = (offsetX1-offsetX0)/n_steps*i + offsetX0;
						offset.y = (offsetY1-offsetY0)/n_steps*i + offsetY0;
						offset.z = (offsetZ1-offsetZ0)/n_steps*i + offsetZ0;
						group.add(
							new LineGeometry.Transform(step).addTranslation(offset)
						);
					}
					
					return group;
				}

				private static LineGeometry.GroupingNode create_RAMP_H() {
					LineGeometry.GroupingNode group;
					group = new LineGeometry.GroupingNode();
					
					int n_steps = 11;
					double step_thickness = wall_thickness*0.4;
					double step_width = wall_length/n_steps;
					
					LineGeometry.Box step = new LineGeometry.Box( step_thickness, step_width, wall_length/2 );
					
					double offsetX0 = wall_thickness/2 - step_thickness/2;
					double offsetY0 = -wall_length/2 - step_width/2;
					double offsetZ0 = 0;
					double offsetX1 = offsetX0 + wall_height;
					double offsetY1 = offsetY0 + wall_length;
					double offsetZ1 = 0;
					
					Point3D offset = new Point3D(0,0,0);
					for (int i=1; i<=n_steps; i++) {
						offset.x = (offsetX1-offsetX0)/n_steps*i + offsetX0;
						offset.y = (offsetY1-offsetY0)/n_steps*i + offsetY0;
						offset.z = (offsetZ1-offsetZ0)/n_steps*i + offsetZ0;
						group.add(
							new LineGeometry.Transform(step).addTranslation(offset)
						);
					}
					
					return group;
				}
				
			}
			
		}

		private static void writeTemplateToFile(PrintWriter vrml, HashSet<String> usedModels) {
			
			StringBuilder templateSB = new StringBuilder();
			InputStream is = templateSB.getClass().getResourceAsStream(VRMLoutput.VRML_TEMPLATE_MODELS_WRL);
			try (BufferedReader template = new BufferedReader( new InputStreamReader( is, StandardCharsets.UTF_8 ) )) {
				template.lines().forEach(str->templateSB.append(str).append("\r\n"));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			vrml.println(templateSB.toString());
			vrml.println("");
			
			writeProtos(vrml, usedModels);
		}

		private static File createBaseVrmlFile(String suggestedFileName) {
			File folder = new File("Bases.VRML");
			if (folder.isDirectory()) return new File(folder,suggestedFileName);
			return new File(suggestedFileName);
		}

		private static File selectVrmlFile2Write(Component parent, String suggestedFileName) {
			//Gui.log_ln("selectVrmlFile2Write: suggested: \"%s\"", suggestedFileName);
			File folder = new File("Bases.VRML");
			if (folder.isDirectory()) {
				//Gui.log_ln("selectVrmlFile2Write: \"%s\" exists", folder);
				File projectBase = new File("./").getAbsoluteFile();
				try { projectBase = projectBase.getCanonicalFile(); }
				catch (IOException e) {}
				//Gui.log_ln("selectVrmlFile2Write: ProjectBaseFolder: \"%s\"", projectBase);
				//Gui.log_ln("selectVrmlFile2Write: VrmlFileChooser.CurrentDirectory: \"%s\"", vrmlFileChooser.getCurrentDirectory());
				if (vrmlFileChooser.getCurrentDirectory().equals(projectBase)) {
					//Gui.log_ln("   equals to ProjectBaseFolder \"%s\"", projectBase);
					vrmlFileChooser.setCurrentDirectory(folder);
					//Gui.log_ln("selectVrmlFile2Write: VrmlFileChooser.CurrentDirectory: \"%s\"", vrmlFileChooser.getCurrentDirectory());
				}
			}
			
			vrmlFileChooser.setSelectedFile(new File(vrmlFileChooser.getCurrentDirectory(),suggestedFileName));
			//Gui.log_ln("selectVrmlFile2Write: VrmlFileChooser.SelectedFile: \"%s\"", vrmlFileChooser.getSelectedFile());
			
			while (true) {
				if (vrmlFileChooser.showSaveDialog(parent)!=JFileChooser.APPROVE_OPTION)
					return null;
				
				boolean fileExtChanged = false;
				File file = vrmlFileChooser.getSelectedFile();
				if (vrmlFileChooser.getFileFilter()==vrmlFileFilter) {
					String name = file.getName();
					if (!name.toLowerCase().endsWith(".wrl")) {
						file = new File(file.getParentFile(), name+".wrl");
						fileExtChanged = true;
					}
				}
				
				if (file.isDirectory()) {
					String message = "Selected file \""+file.getPath()+"\" is a directory. Please select an unused file name.";
					if (fileExtChanged)
						message = "File extension \".wrl\" was added. Resulting file \""+file.getPath()+"\" is a directory. Please select an unused file name.";
					if (JOptionPane.OK_OPTION!=JOptionPane.showConfirmDialog(parent, message, "Selected file is a directory", JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE))
						return null;
				} else
				if (file.isFile()) {
					String message = "Selected file \""+file.getPath()+"\" already exists. Do you want to overwrite this file?";
					int result = JOptionPane.showConfirmDialog(parent, message, "Selected file is a directory", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
					switch (result) {
					case JOptionPane.YES_OPTION: return file;
					case JOptionPane.NO_OPTION: break;
					default: return null; 
					}
				} else
					return file;
			}
		}

		private static void writeModel(PrintWriter vrml, BuildingObject obj) {
			writeModel(vrml, obj, false, null);
		}
		private static void writeModel(PrintWriter vrml, BuildingObject obj, boolean isMAINROOMwithRoof, HashMap<String,Integer> objectIDNumbers) {
			if (obj.position==null) return;
			if (obj.position.pos==null) return;
			if (obj.position.up==null) return;
			if (obj.position.at==null) return;
			
			GeneralizedID id = obj.getId();
			String objectID = obj.objectID;
			String label = getLabel(objectID);
			String extraLine = null; // obj.userData==null ? null : String.format("0x%08X", obj.userData);
			
			writeModel(vrml, id, objectID, isMAINROOMwithRoof, objectIDNumbers, label, extraLine, obj.position.pos, obj.position.at, obj.position.up, null);
		}
		
		private static String getLabel(String objectID) {
			GameInfos.GeneralizedID id = GameInfos.productIDs.get(objectID);
			String label = objectID;
			if (id!=null && id.hasLabel()) label = id.label;
			return label;
		}

		private static void writeSingleTextNode(PrintWriter vrml, String text, Point3D pos, Point3D at, Point3D up) {
			writeMyOrientation(vrml, pos, at, up, ()->{
				vrml.printf(" SingleText { string %s }", createLabelStrs(text, 20));
			});
		}

		private static void writeModel(PrintWriter vrml, String objectID, String label, String extraLine, Point3D pos, Point3D at, Point3D up, Point3D lineColor) {
			writeModel(vrml, null, objectID, false, null, label, extraLine, pos, at, up, lineColor);
		}
		private static void writeModel(PrintWriter vrml, GeneralizedID id, String objectID, boolean isMAINROOMwithRoof, HashMap<String,Integer> objectIDNumbers, String label, String extraLine, Point3D pos, Point3D at, Point3D up, Point3D lineColor) {
			String modelName = mapObjectID2Model.get(objectID);
			
			if ("__SIMPLE_LINE".equals(modelName)) {
				writeSimpleLine(vrml, pos, at, up, objectID);
				
			} else if ("^SMALLLIGHT".equals(objectID) && SaveViewer.config.useSmallLightsAsMeasurePoints) {
				writeMeasurePoint(vrml, pos);
				
			} else {
				writeMyOrientation(vrml, pos, at, up, ()->{
					if ("GENERAL_DECAL".equals(modelName) && id!=null && id.hasImageFileName()) {
						File imageFile = Images.ExtraImageList.getImageFile(id.getImageFileName());
						if (imageFile.isFile()) {
							vrml.printf(" IMAGE_DECAL { url [ \"%s\" ] }", imageFile.getAbsolutePath().replace('\\', '/'));
							return;
						}
					}
					if (modelName!=null) {
						Proto proto = mapModel2Proto.get(modelName);
						int maxTextLength = proto==null || proto.maxTextLength==null ? 20 : proto.maxTextLength.intValue();
						
						String lineColorStr = lineColor==null?"":(" lineColor "+lineColor.toString("%1.2f",false));
						String labelStr = createLabelStrs(extraLine==null ? label : label+" "+extraLine, maxTextLength);
						String normalModel = String.format(" %s { string %s%s }", modelName, labelStr, lineColorStr);
						
						if (isMAINROOMwithRoof) {
							String roofModelName = getRoofModelName(modelName);
							if (roofModelName!=null)
								vrml.printf(" [ %s %s{} ]", normalModel, roofModelName);
							else
								vrml.print(normalModel);
						} else
							vrml.print(normalModel);
						
					} else {
						String str;
						Vector<String> lines = new Vector<>();
						if (label!=null && !label.equals(objectID)) lines.add(label.replace("\\", "\\\\").replace("\"", "\\\""));
						if (objectIDNumbers!=null) {
							Integer n = objectIDNumbers.get(objectID);
							if (n==null) n=0;
							objectIDNumbers.put(objectID,n+1);
							lines.add(String.format("%s [%02d]", objectID, n));
						} else
							lines.add(objectID);
						if (extraLine!=null) lines.add(extraLine.replace("\\", "\\\\").replace("\"", "\\\""));
						if (lines.size()==1) str = String.format("\"%s\"", lines.firstElement());
						else                 str = String.format("[ \"%s\" ]", String.join("\", \"", lines));
						vrml.printf(Locale.ENGLISH," AxisCross { string %s }", str);
					}
				});
			}
			
		}
		private static String createLabelStrs(String label, int maxChunkLength) {
			String labelStr;
			if (label.length()>maxChunkLength) {
				Vector<String> strParts = splitIntoChunks(label,maxChunkLength);
				labelStr = "["+join(strParts, str->" \""+str+"\"")+" ]";;
			} else
				labelStr = "\""+label+"\"";
			return labelStr;
		}

		private static String join(Vector<String> parts, Function<String,String> convertPart) {
			String str = "";
			for (String part:parts) str += convertPart.apply(part);
			return str;
		}
		
		private static Vector<String> splitIntoChunks(String label, int maxChunkLength) {
			Vector<String> strParts = new Vector<>();
			
			label = label.trim();
			while ( !label.isEmpty() ) {
				
				int splitPos = -1;
				if (label.length()>maxChunkLength)
					while (true) {
						int posSpace = label.indexOf(' ', splitPos+1);
						int posLine  = label.indexOf('-', splitPos+1);
						int newSplitPos;
						if (posSpace >= 0) {
							if (posLine < 0) newSplitPos = posSpace;
							else newSplitPos = Math.min(posSpace, posLine);
							
						} else if (posLine >= 0) newSplitPos = posLine;
						else newSplitPos = -1;
						
						if (newSplitPos < 0) // no more split positions from here
							break;
						
						else if (newSplitPos < maxChunkLength) // found a new split position
							splitPos = newSplitPos;
						
						else {
							if (splitPos < 0) // it's the first run
								splitPos = newSplitPos; // tradeoff: chunk exceeds maxChunkLength, but there is not earlier split pos
							break;
						}
					}
				
				String part;
				if (splitPos < 0) {
					part = label;
					label = "";
				} else {
					part = label.substring(0,splitPos+1).trim(); // incl. space or line char
					label = label.substring(splitPos+1).trim();
				}
				
				strParts.add(part);
			}
			return strParts;
		}
		
		static class Proto {
			final String protoName;
			final String requiredProto;
			final double labelScale;
			final Double labelXRotation_deg;
			final Point3D labelTranslation;
			final Integer maxTextLength;
			final Color defaultTextColor;
			final Color defaultLineColor;
			final ProtoGeometry geometry;
			final int precision;
			
			Proto(String protoName, ProtoGeometry geometry) {
				this(protoName, null, null, null, null, null, null, geometry, null, null);
			}
			Proto(String protoName, ProtoGeometry geometry, String requiredProto) {
				this(protoName, null, null, null, null, null, null, geometry, null, requiredProto);
			}
			Proto(String protoName, int maxTextLength, ProtoGeometry geometry) {
				this(protoName, null, null, null, maxTextLength, null, null, geometry, null, null);
			}
			Proto(String protoName, Double labelScale, double labelXRotation_deg, ProtoGeometry geometry) {
				this(protoName, labelScale, labelXRotation_deg, null, null, null, null, geometry, null, null);
			}
			Proto(String protoName, Double labelScale, double labelXRotation_deg, ProtoGeometry geometry, int precision) {
				this(protoName, labelScale, labelXRotation_deg, null, null, null, null, geometry, precision, null);
			}
			Proto(String protoName, Double labelScale, double labelXRotation_deg, int maxTextLength, ProtoGeometry geometry) {
				this(protoName, labelScale, labelXRotation_deg, null, maxTextLength, null, null, geometry, null, null);
			}
			Proto(String protoName, Double labelScale, double labelXRotation_deg, Point3D labelTranslation, ProtoGeometry geometry) {
				this(protoName, labelScale, labelXRotation_deg, labelTranslation, null, null, null, geometry, null, null);
			}
			Proto(String protoName, Double labelScale, Color defaultTextColor, Color defaultLineColor, ProtoGeometry geometry) {
				this(protoName, labelScale, null, null, null, defaultTextColor, defaultLineColor, geometry, null, null);
			}
			Proto(String protoName, Double labelScale, Double labelXRotation_deg, Point3D labelTranslation, Integer maxTextLength, Color defaultTextColor, Color defaultLineColor, ProtoGeometry geometry, Integer precision, String requiredProto) {
				if (protoName==null || protoName.isEmpty()) throw new IllegalArgumentException();
				this.protoName = protoName;
				this.requiredProto = requiredProto;
				this.labelScale = labelScale==null ? 1.0 : labelScale;
				this.labelXRotation_deg = labelXRotation_deg;
				this.labelTranslation = labelTranslation;
				this.maxTextLength = maxTextLength;
				this.defaultTextColor = defaultTextColor==null ? new Color(0,0,0.5f) : defaultTextColor;
				this.defaultLineColor = defaultLineColor==null ? new Color(0,0,0)    : defaultLineColor;
				this.geometry = geometry;
				this.precision = precision==null ? 2 : precision;
			}
			
			void writeProtoToFile(PrintWriter vrml) {
				
				vrml.println("PROTO "+protoName+" [");
				vrml.println("	field MFString string []");
				vrml.printf ("	field SFColor textColor %s%n", toString(defaultTextColor,"%1.2f"));
				vrml.printf ("	field SFColor lineColor %s%n", toString(defaultLineColor,"%1.2f"));
				vrml.println("] {");
				Point3D scale = new Point3D(1,1,1).mul(labelScale/2);
				if (labelXRotation_deg!=null || labelTranslation!=null) {
					vrml.print  ("	Transform {");
					if (labelXRotation_deg!=null) vrml.printf (Locale.ENGLISH," rotation 1 0 0 %1.6f", labelXRotation_deg/180*Math.PI);
					if (labelTranslation  !=null) vrml.printf (Locale.ENGLISH," translation %1.6f %1.6f %1.6f", labelTranslation.x, labelTranslation.y, labelTranslation.z);
					vrml.println(" children");
				}
				vrml.printf(Locale.ENGLISH,"	Transform { scale %1.3f %1.3f %1.3f rotation 0 1 0 1.5707963%n", scale.x, scale.y, scale.z);
				vrml.println("		children Shape {");
				vrml.println("			appearance Appearance { material Material { diffuseColor IS textColor } }");
				vrml.println("			geometry Text { string IS string fontStyle FontStyle { justify [ \"MIDDLE\" \"END\" ] family \"SANSSERIF\"} }");
				vrml.println("		}");
				vrml.println("	}");
				if (labelXRotation_deg!=null || labelTranslation!=null)
					vrml.println("	}");
				geometry.write(vrml,"\t", precision);
				vrml.println("}");
				vrml.println();
			}
			
			private static String toString(Color color, String format) {
				return String.format(Locale.ENGLISH, format+" "+format+" "+format, color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f);
			}

			interface ProtoGeometry {
				void write(PrintWriter vrml, String vrmlIndent, int precision);
			}
		}

		private static void writeMeasurePoint(PrintWriter vrml, Point3D pos) {
			vrml.printf(Locale.ENGLISH, "MessPunkt { pos %1.4f %1.4f %1.4f addPosToStrings FALSE string \"%1$1.3f | %2$1.3f | %3$1.3f\" }%n", pos.x, pos.y, pos.z);
		}

		private static void writeSimpleLineProtoToFile(PrintWriter vrml) {
			vrml.println("PROTO SIMPLE_LINE [");
			vrml.println("	field MFVec3f point []");
			vrml.println("	field SFColor lineColor 0 0 0");
			vrml.println("] {");
			vrml.println("	Shape {");
			vrml.println("		appearance Appearance { material Material { emissiveColor IS lineColor } }");
			vrml.println("		geometry IndexedLineSet {");
			vrml.println("			coord Coordinate { point IS point }");
			vrml.println("			coordIndex [ 0 1 ]");
			vrml.println("		}");
			vrml.println("	}");
			vrml.println("}");
			vrml.println();
		}
		
		private static void writeSimpleLine(PrintWriter vrml, Point3D pos, Point3D at, Point3D up, String objectID) {
			Color color = Color.BLACK;
			switch (objectID) {
			case "^U_PIPELINE"    : color = Color.RED; break;
			case "^U_PORTALLINE"  : color = Color.CYAN; break;
			case "^U_POWERLINE"   : color = Color.BLUE; break;
			case "^U_BYTEBEATLINE": color = Color.MAGENTA; break;
			default: Debug.Assert(false); break;
			}
			vrml.print  ("SIMPLE_LINE {");
			vrml.printf (Locale.ENGLISH," point [ %1.4f %1.4f %1.4f, %1.4f %1.4f %1.4f ]", pos.x,pos.y,pos.z, pos.x+at.x,pos.y+at.y,pos.z+at.z);
			vrml.printf (Locale.ENGLISH," lineColor %1.3f %1.3f %1.3f", color.getRed()/255.0f, color.getGreen()/255.0f, color.getBlue()/255.0f);
			vrml.println(" }");
		}

		private static void writeMyOrientation(PrintWriter vrml, Point3D pos, Point3D at, Point3D up, Runnable writeChildren) {
			double scale = 1;
			if (up!=null) {
				scale = up.length();
				if (scale!=0) up = up.normalize();
				else scale = 1;
			}
			if (at!=null && !at.isZero())
				at = at.normalize();
			
			vrml.print("MyOrientation {");
			vrml.printf(Locale.ENGLISH," pos %1.2f %1.2f %1.2f", pos.x, pos.y, pos.z);
			if (at!=null) vrml.printf(Locale.ENGLISH," at %1.4f %1.4f %1.4f", at.x, at.y, at.z);
			if (up!=null) vrml.printf(Locale.ENGLISH," up %1.4f %1.4f %1.4f", up.x, up.y, up.z);
			if (scale!=1) vrml.printf(Locale.ENGLISH," scale %1$1.4f %1$1.4f %1$1.4f", scale);
			
			vrml.print (" children");
			writeChildren.run();
			
			vrml.println(" }");
		}
		
		private static void writeIndexedLineSet(PrintWriter vrml, String indent, StringBuilder pointsStr, StringBuilder indexesStr, Color color) {
			vrml.println(indent+"Shape {");
			vrml.print  (indent+"	appearance Appearance { material Material { ");
			if (color==null) // for PROTO
				vrml.printf ("emissiveColor IS lineColor");
			else {
				vrml.printf (Locale.ENGLISH, "emissiveColor %1.3f %1.3f %1.3f", color.getRed()/255.0f, color.getGreen()/255.0f, color.getBlue()/255.0f);
				int alpha = color.getAlpha();
				if (alpha<255) vrml.printf(Locale.ENGLISH, " transparency %1.3f", (255-alpha)/255f);
			}
			vrml.println(indent+" } }");
			vrml.println(indent+"	geometry IndexedLineSet {");
			vrml.printf (indent+"		coord Coordinate { point [ %s ] }\r\n",pointsStr.toString());
			vrml.printf (indent+"		coordIndex [ %s ]\r\n",indexesStr.toString());
			vrml.println(indent+"	}");
			vrml.println(indent+"}");
		}

		private static void writeSphere(PrintWriter vrml, double radius, Point3D center, java.awt.Color color) {
			
			int segLon = 32;
			int segLat = 16;
			if (color==null)
				color = java.awt.Color.BLACK;
			
			StringBuilder coordIndexes = new StringBuilder();
			Vector<Point3D> coordPoints = new Vector<>();
			Vector<StringBuilder> coordIndexesLon = new Vector<>();
			
			for (int iLon=0; iLon<segLon; ++iLon)
				coordIndexesLon.add(new StringBuilder());
			
			for (int iLat=1; iLat<segLat; ++iLat) {
				double wLat = iLat*Math.PI/segLat-Math.PI/2;
				double y = radius*Math.sin(wLat)+center.y;
				double radLat = radius*Math.cos(wLat);
				
				StringBuilder coordIndexesLat = new StringBuilder();
				String firstIndex = coordPoints.size()+" ";
				
				for (int iLon=0; iLon<segLon; ++iLon) {
					double wLon = iLon*2*Math.PI/segLon;
					double x = radLat*Math.sin(wLon)+center.x;
					double z = radLat*Math.cos(wLon)+center.z;
					
					String coordsIndex = coordPoints.size()+" ";
					coordIndexesLat.append(coordsIndex);
					coordIndexesLon.get(iLon).append(coordsIndex);
					coordPoints.add(new Point3D(x,y,z));
				}
				coordIndexes.append(coordIndexesLat.append(firstIndex+"-1 ").toString());
			}
			
			for (int iLon=0; iLon<segLon; ++iLon)
				coordIndexes.append(coordIndexesLon.get(iLon).append("-1 ").toString());
			
			StringBuilder coordPointsStr = new StringBuilder();
			for (Point3D p:coordPoints)
				coordPointsStr.append((coordPointsStr.length()>0?", ":"")+String.format(Locale.ENGLISH, "%1.2f %1.2f %1.2f", p.x, p.y, p.z));
			
			writeIndexedLineSet(vrml, "", coordPointsStr, coordIndexes, color);
		}
		
	}
	
	private static class LineGeometry {
		static int CIRCLE_SEGMENTS = 20; // = 32;
		
		enum Axis { X, Y, Z }
		
		static class Segment {
			Vector<Integer> indexes;
			Segment() {
				indexes = new Vector<>();
			}
			public Segment(Segment seg) {
				this();
				indexes.addAll(seg.indexes);
			}
			public boolean isEmpty() {
				return indexes.isEmpty();
			}
			public Segment addIndexOffset(int indexOffset) {
				for (int i=0; i<indexes.size(); i++)
					indexes.set(i,indexes.get(i)+indexOffset);
				return this;
			}
			public Segment add(int... indexes) {
				for (int i:indexes) this.indexes.add(i);
				return this;
			}
			public boolean equals(Segment other) {
				if (other==null) return false;
				if (this.indexes.size()!=other.indexes.size()) return false;
				int size = indexes.size();
				boolean isEqual = true;
				boolean isReversed = true;
				for (int i=0; i<size; i++) {
					isEqual    &= (int)this.indexes.get(i)==(int)other.indexes.get(i);
					isReversed &= (int)this.indexes.get(i)==(int)other.indexes.get(size-i-1);
					if (!isEqual && !isReversed)
						return false;
				}
				return true;
			}
			public boolean isConnectedTo(Segment other) {
				if (other==null) return false;
				int first1 = this.indexes.firstElement();
				int first2 = other.indexes.firstElement();
				int last1 = this.indexes.lastElement();
				int last2 = other.indexes.lastElement();
				return (first1==first2) || (last1==last2) || (first1==last2) || (last1==first2);
			}
			@Override
			public String toString() {
				return "Segment [indexes=" + indexes.toString() + "]";
			}
		}

		static void writeIndexedLineSet_verbose(PrintWriter vrml, IndexedLineSet indexedLineSet, String vrmlIndent, Color color, ProgressDialog pd, String logIndent) {
			writeIndexedLineSet_verbose(vrml, indexedLineSet, vrmlIndent, 2, color, pd, logIndent);
		}

		static void writeIndexedLineSet_verbose(PrintWriter vrml, IndexedLineSet indexedLineSet, String vrmlIndent, int precision, Color color, ProgressDialog pd, String logIndent) {
			long startTime = 0;
			boolean verbose = logIndent!=null;
			
			StringBuilder pointsStr = new StringBuilder();
			StringBuilder indexesStr = new StringBuilder();
			
			if (verbose) startTime = startTask(pd, logIndent, "Prepare Geometry for output");
			indexedLineSet.prepareForOutput();
			if (verbose) endTask(startTime);
			
			/*			
			int n0, n1;
			
			if (verbose) startTime = startTask(pd, logIndent, "Optimize points");
			n0 = indexedLineSet.points.size();
			indexedLineSet.optimizePoints();
			n1 = indexedLineSet.points.size();
			if (verbose) endTask(startTime, String.format(Locale.ENGLISH, "  -> %d of %d points removed (%1.1f%%)", n0-n1, n0, (n0-n1)*100.0/n0) );
			
			if (verbose) startTime = startTask(pd, logIndent, "Optimize segments");
			n0 = indexedLineSet.segments.size();
			indexedLineSet.optimizeSegments();
			n1 = indexedLineSet.segments.size();
			if (verbose) endTask(startTime, String.format(Locale.ENGLISH, "  -> %d of %d segments removed (%1.1f%%)", n0-n1, n0, (n0-n1)*100.0/n0) );
			 */
			
			Vector<Point3D> points = indexedLineSet.points;
			if (verbose) startTime = startTask(pd, logIndent, "Build point array", points.size());
			for (int i=0; i<points.size(); i++) {
				Point3D p = points.get(i);
				if (pointsStr.length()>0) pointsStr.append(", ");
				pointsStr.append(String.format(Locale.ENGLISH,"%1."+precision+"f %1."+precision+"f %1."+precision+"f", p.x, p.y, p.z));
				int value = i+1;
				if (verbose && pd!=null) Gui.runInEventThreadAndWait(()->{ pd.setValue(value); });
			}
			if (verbose) endTask(startTime);
			
			Vector<Segment> segments = indexedLineSet.segments;
			if (verbose) startTime = startTask(pd, logIndent, "Build index array", segments.size());
			for (int s=0; s<segments.size(); s++) {
				Segment seg = segments.get(s);
				if (indexesStr.length()>0) indexesStr.append(" -1");
				for (int i:seg.indexes) indexesStr.append(" "+i);
				int value = s+1;
				if (verbose && pd!=null) Gui.runInEventThreadAndWait(()->{ pd.setValue(value); }); 
			}
			if (verbose) endTask(startTime);
			
			if (verbose) startTime = startTask(pd, logIndent, "Write VRML structure");
			VRMLoutput.writeIndexedLineSet(vrml, vrmlIndent, pointsStr, indexesStr, color);
			if (verbose) endTask(startTime);
		}

		static void writeIndexedLineSet(PrintWriter vrml, IndexedLineSet indexedLineSet, String vrmlIndent, int precision, Color color) {
			writeIndexedLineSet_verbose(vrml, indexedLineSet, vrmlIndent, precision, color, null, null);
		}
		
		private static void setPointValue(Point3D p, Axis axis, double value) {
			switch (axis) {
			case X: p.x=value; break;
			case Y: p.y=value; break;
			case Z: p.z=value; break;
			}
		}
		
		public static class MultipleIndexedLineSets implements VRMLoutput.Proto.ProtoGeometry {
			private final Vector<StoredIndexedLineSet> indexedLineSets;

			MultipleIndexedLineSets() {
				indexedLineSets = new Vector<>();
			}

			@Override public void write(PrintWriter vrml, String vrmlIndent, int precision) {
				for (StoredIndexedLineSet storedILS:indexedLineSets)
					storedILS.indexedLineSet.write(vrml, vrmlIndent, precision, storedILS.color);
			}
			
			@SuppressWarnings("unused")
			MultipleIndexedLineSets add(MultipleIndexedLineSets other) {
				if (other!=null)
					indexedLineSets.addAll(other.indexedLineSets);
				return this;
			}
			MultipleIndexedLineSets add(IndexedLineSet indexedLineSet) {
				return add(null,indexedLineSet);
			}
			MultipleIndexedLineSets add(Color color, IndexedLineSet indexedLineSet) {
				if (indexedLineSet!=null)
					indexedLineSets.add(new StoredIndexedLineSet(color,indexedLineSet));
				return this;
			}
			
			static class StoredIndexedLineSet {
				private final Color color;
				private final IndexedLineSet indexedLineSet;
				StoredIndexedLineSet(Color color, IndexedLineSet indexedLineSet) {
					this.color = color;
					this.indexedLineSet = indexedLineSet;
				}
			}
		}

		public static abstract class IndexedLineSet implements VRMLoutput.Proto.ProtoGeometry {
			Vector<Point3D> points;
			Vector<Segment> segments;
			IndexedLineSet() {
				points = new Vector<>();
				segments = new Vector<>();
			}
			
			public static IndexedLineSet parse(String coords, String indexes, String debugOutputLabel) {
				IndexedLineSet lines = new IndexedLineSet() { @Override protected void prepareForOutput() {} };
				
				coords = cleanStr(coords);
				indexes = cleanStr(indexes);
				
				String[] points = coords.split(",");
				for (int i=0; i<points.length; i++) {
					String pointStr = points[i].trim();
					String[] valueStrs = pointStr.split(" ");
					if (valueStrs.length!=3) {
						Gui.log_error_ln("[%s] Can't parse coords. Point %d has %s than 3 values: \"%s\"", debugOutputLabel, i, valueStrs.length<3 ? "less" : "more", pointStr);
						return null;
					}
					String str = "?";
					try {
						str="X"; double x = Double.parseDouble(valueStrs[0]);
						str="Y"; double y = Double.parseDouble(valueStrs[1]);
						str="Z"; double z = Double.parseDouble(valueStrs[2]);
						lines.points.add(new Point3D(x, y, z));
					} catch (NumberFormatException e) {
						Gui.log_error_ln("[%s] Can't parse coords. %s value in point %d isn't a number: \"%s\"", debugOutputLabel, str, i, pointStr);
						return null;
					}
				}
				
				Segment segment = new Segment();
				String[] indexStrs = indexes.split(" ");
				for (int i=0; i<indexStrs.length; i++) {
					String indexStr = indexStrs[i];
					int index;
					try {
						index = Integer.parseInt(indexStr);
					} catch (NumberFormatException e) {
						Gui.log_error_ln("[%s] Can't parse indexes. Value %d of index list isn't an integer number: \"%s\"", debugOutputLabel, i, indexStr);
						return null;
					}
					if (index<0) {
						if (!segment.isEmpty())
							lines.segments.add(segment);
						segment = new Segment();
					} else {
						segment.add(index);
					}
				}
				if (!segment.isEmpty())
					lines.segments.add(segment);
				
				return lines;
			}

			private static String cleanStr(String str) {
				str = str.replace("\r"," ").replace("\n"," ").replace("\t"," ");
				
				int length = str.length();
				str = str.replace("  "," ");
				while(length != str.length()) {
					length = str.length();
					str = str.replace("  "," ");
				}
				
				str = str.trim();
				
				return str;
			}

			protected abstract void prepareForOutput();

			void optimizePoints() {
				for (int p1Index=0; p1Index<points.size(); p1Index++) {
					Point3D p1 = points.get(p1Index);
					
					for (int p2Index=p1Index+1; p2Index<points.size();) {
						Point3D p2 = points.get(p2Index);
						
						if (p1.distTo(p2)<0.000001) {
							points.remove(p2Index);
							for (Segment seg:segments)
								for (int i=0; i<seg.indexes.size(); i++) {
									int index = seg.indexes.get(i);
									if (index==p2Index)
										seg.indexes.set(i,p1Index);
									else if (index>p2Index)
										seg.indexes.set(i,index-1);
								}
						} else
							p2Index++;
					}
				}
			}
			
			private static class Pair {
				@SuppressWarnings("unused")
				Segment seg1,seg2;
				Pair(Segment seg1, Segment seg2) {
					this.seg1 = seg1;
					this.seg2 = seg2;
				} 
			}
			
			void optimizeSegments() {
				Vector<Pair> connectedSegments = new Vector<>();
				for (int i1=0; i1<segments.size(); i1++) {
					Segment seg1 = segments.get(i1);
					for (int i2=i1+1; i2<segments.size();) {
						Segment seg2 = segments.get(i2);
						if (seg1.equals(seg2))
							segments.remove(i2);
						else {
							if (seg1.isConnectedTo(seg2))
								connectedSegments.add(new Pair(seg1,seg2));
							i2++;
						}
					}
				}
				// TODO [OLD] LineGeometry.IndexedLineSet.optimizeSegments()
			}

			@Override public void write(PrintWriter vrml, String vrmlIndent, int precision             ) { writeIndexedLineSet(vrml, this, vrmlIndent, precision,  null); }
			void                  write(PrintWriter vrml, String vrmlIndent, int precision, Color color) { writeIndexedLineSet(vrml, this, vrmlIndent, precision, color); }
		}
		
		static class OptimizeNode extends GroupingNode {
			OptimizeNode(IndexedLineSet... objs) {
				super.add(objs);
				super.prepareForOutput();
				super.optimizePoints();
				super.optimizeSegments();
			}
			@Override
			OptimizeNode add(IndexedLineSet... objs) {
				throw new UnsupportedOperationException("Can't add object to an OptimizeNode after construction.");
			}
			@Override
			protected void prepareForOutput() {
			}
		}
		
		static class GroupingNode extends IndexedLineSet {
			Vector<IndexedLineSet> subNodes;
			GroupingNode() {
				subNodes = new Vector<>();
			}
			GroupingNode(IndexedLineSet... objs) {
				this();
				add(objs);
			}
			GroupingNode add(IndexedLineSet... objs) {
				for (IndexedLineSet obj:objs) if (obj!=null) subNodes.add(obj);
				return this;
			}
			@Override
			protected void prepareForOutput() {
				points.clear();
				segments.clear();
				for (IndexedLineSet subNode:subNodes) {
					subNode.prepareForOutput();
					for (Segment seg:subNode.segments)
						segments.add(new Segment(seg).addIndexOffset(points.size()));
					points.addAll(subNode.points);
				}
			}
		}
		
		static class DirectWriteGroupingNode extends IndexedLineSet {
			
			DirectWriteGroupingNode(IndexedLineSet... objs) {
				add(objs);
			}
			DirectWriteGroupingNode add(IndexedLineSet... objs) {
				for (IndexedLineSet subNode:objs) {
					subNode.prepareForOutput();
					for (Segment seg:subNode.segments)
						segments.add(new Segment(seg).addIndexOffset(points.size()));
					points.addAll(subNode.points);
				}
				return this;
			}
			@Override
			protected void prepareForOutput() {}
			
		}
		
		static class Transform extends IndexedLineSet {
			
			private static class TransformMatrix {
				double[][] m; // m[row][col];
				
				TransformMatrix() {
					m = new double[4][];
					m[0] = new double[] { 1,0,0,0 };
					m[1] = new double[] { 0,1,0,0 };
					m[2] = new double[] { 0,0,1,0 };
					m[3] = new double[] { 0,0,0,1 };
				}
				
				Point3D transform( Point3D p ) {
					return new Point3D(
						m[0][0]*p.x + m[0][1]*p.y + m[0][2]*p.z + m[0][3],
						m[1][0]*p.x + m[1][1]*p.y + m[1][2]*p.z + m[1][3],
						m[2][0]*p.x + m[2][1]*p.y + m[2][2]*p.z + m[2][3]
					);
				}
				
				TransformMatrix mul( TransformMatrix other ) {  //  result = this * other 
					TransformMatrix newM = new TransformMatrix();
					for (int row=0; row<4; row++)
						for (int col=0; col<4; col++) {
							newM.m[row][col] =
									this.m[row][0]*other.m[0][col] + 
									this.m[row][1]*other.m[1][col] + 
									this.m[row][2]*other.m[2][col] + 
									this.m[row][3]*other.m[3][col]; 
						}
					return newM;
				}
				
				TransformMatrix setTranslation( Point3D p ) {
					m[0] = new double[] { 1,0,0, p.x };
					m[1] = new double[] { 0,1,0, p.y };
					m[2] = new double[] { 0,0,1, p.z };
					m[3] = new double[] { 0,0,0,  1  };
					return this;
				}
				
				TransformMatrix setRotation( Axis rotationAxis, double rotationAngle_deg ) {
					double sin = Math.sin(rotationAngle_deg/180*Math.PI);
					double cos = Math.cos(rotationAngle_deg/180*Math.PI);
					switch (rotationAxis) {
					case X:
						m[0] = new double[] { 1,  0 ,  0 , 0 };
						m[1] = new double[] { 0, cos,-sin, 0 };
						m[2] = new double[] { 0, sin, cos, 0 };
						m[3] = new double[] { 0,  0 ,  0 , 1 };
						break;
					case Y:
						m[0] = new double[] { cos, 0, sin, 0 };
						m[1] = new double[] {  0 , 1,  0 , 0 };
						m[2] = new double[] {-sin, 0, cos, 0 };
						m[3] = new double[] {  0 , 0,  0 , 1 };
						break;
					case Z:
						m[0] = new double[] { cos,-sin, 0, 0 };
						m[1] = new double[] { sin, cos, 0, 0 };
						m[2] = new double[] {  0 ,  0 , 1, 0 };
						m[3] = new double[] {  0 ,  0 , 0, 1 };
						break;
					}
					return this;
				}
				
				TransformMatrix setScale( Point3D p ) {
					m[0] = new double[] { p.x,  0 ,  0 , 0 };
					m[1] = new double[] {  0 , p.y,  0 , 0 };
					m[2] = new double[] {  0 ,  0 , p.z, 0 };
					m[3] = new double[] {  0 ,  0 ,  0 , 1 };
					return this;
				}
			}
			
			private IndexedLineSet child;
			private TransformMatrix transformMatrix;

			Transform(IndexedLineSet child) {
				this.child = child;
				this.transformMatrix = new TransformMatrix();
			}

			Transform addTranslation(Axis axis, double dist) {
				switch (axis) {
				case X: return addTranslation( new Point3D(dist,0,0) );
				case Y: return addTranslation( new Point3D(0,dist,0) );
				case Z: return addTranslation( new Point3D(0,0,dist) );
				}
				return this;
			}
			
			Transform addTranslation(double x, double y, double z) {
				return addTranslation( new Point3D(x,y,z) );
			}
			
			Transform addTranslation( Point3D translation ) {
				transformMatrix = new TransformMatrix().setTranslation(translation).mul(transformMatrix);
				return this;
			}
			Transform addRotation( Axis rotationAxis, double rotationAngle_deg ) {
				transformMatrix = new TransformMatrix().setRotation(rotationAxis, rotationAngle_deg).mul(transformMatrix);
				return this;
			}
			Transform addScale( Point3D scale ) {
				transformMatrix = new TransformMatrix().setScale(scale).mul(transformMatrix);
				return this;
			}

			@Override
			protected void prepareForOutput() {
				if (child==null)
					return;
				
				child.prepareForOutput();
				
				segments.clear();
				for (Segment seg:child.segments)
					segments.add(new Segment(seg));
				
				points.clear();
				for (Point3D p:child.points)
					points.add(transformMatrix.transform(p));
			}
		}
		
		static class PolyLine extends IndexedLineSet {
			
			private Segment segment;
			private boolean isClosed;
			
			PolyLine() {
				isClosed = false;
				segment = new Segment();
				segments.add(segment);
			}
			PolyLine(Point3D... points) {
				this();
				addAll(points);
			}
			
			public PolyLine getCopy(double dx, double dy, double dz) {
				PolyLine p = new PolyLine();
				forEachPointXYZ((x,y,z) -> p.add(x+dx,y+dy,z+dz));
				if (isClosed) p.close();
				return p;
			}
			
			@Override
			protected void prepareForOutput() {}
			
			public Point3D getLast() {
				return points.lastElement();
			}
			public Point3D getFirst() {
				return points.firstElement();
			}
			public Point3D get(int i) {
				return points.get(i);
			}
			public void forEachPointXYZ(Point3DAction action) {
				forEachPoint(p->action.doWithPoint(p.x,p.y,p.z));
			}
			public void forEachPoint(Consumer<Point3D> action) {
				points.forEach(action);
			}
			public interface Point3DAction {
				void doWithPoint(double x, double y, double z);
			}
			
			
			public PolyLine addAll(Point3D... points) {
				for (Point3D p:points) add(p);
				return this;
			}
			public PolyLine add(Point3D p) {
				segment.add(points.size());
				points.add(p);
				return this;
			}
			public PolyLine add(double x, double y, double z) {
				segment.add(points.size());
				points.add(new Point3D(x,y,z));
				return this;
			}
			
			public PolyLine close() {
				isClosed = true;
				if (!segment.indexes.isEmpty())
					segment.add(segment.indexes.firstElement());
				return this;
			}
			
			public PolyLine addArc(Axis axis, Point3D center, double radius, double startAngle_deg, double endAngle_deg, boolean flipped) {
				return addArc(axis, center, radius, startAngle_deg, endAngle_deg, flipped, -1);
			}
			
			public PolyLine addArc(Axis axis, Point3D center, double radius, double startAngle_deg, double endAngle_deg, boolean flipped, int nSeg) {
				loopArc(radius, startAngle_deg, endAngle_deg, flipped, nSeg, (i, nSeg_, x, y) -> {
					switch (axis) {
					case X: add( center.add(0, x, y) ); break;
					case Y: add( center.add(y, 0, x) ); break;
					case Z: add( center.add(x, y, 0) ); break;
					}
				});
				return this;
			}
		}
		
		static class Circle extends PolyLine {
			Circle(Axis axis, Point3D center, double radius) {
				addArc(axis, center, radius, 0, 360, false);
			}
		}
		
		static class RegularPrism extends Prism {
			RegularPrism(Axis heightAxis, double height, int nSeg, double edgeLength, double startAngle_deg) {
				super(heightAxis, height, generatePoints(heightAxis, nSeg, edgeLength, startAngle_deg));
			}
			private static Point3D[] generatePoints(Axis axis, int nSeg, double edgeLength, double startAngle_deg) {
				Point3D[] points = new Point3D[nSeg];
				double radius = edgeLength/2/Math.sin(Math.PI/nSeg);
				loopArc(radius, startAngle_deg, startAngle_deg+360, false, nSeg, (i, nSeg_, x, y) -> {
					if (i<nSeg)
						switch (axis) {
						case X: points[i] = new Point3D(0, x, y); break;
						case Y: points[i] = new Point3D(y, 0, x); break;
						case Z: points[i] = new Point3D(x, y, 0); break;
						}
				});
				return points;
			}
		}
		
		static class Prism extends GroupingNode {
			Prism(Axis heightAxis, double height, Point3D... points) {
				super(
					new Transform(new PolyLine(points).close()).addTranslation(heightAxis,+height/2),
					new Transform(new PolyLine(points).close()).addTranslation(heightAxis,-height/2),
					new GroupingNode(generateHeightLines(heightAxis, height, points))
				);
			}
			private static PolyLine[] generateHeightLines(Axis heightAxis, double height, Point3D[] points) {
				PolyLine[] lines = new PolyLine[points.length];
				for (int i=0; i<points.length; i++) {
					Point3D p = points[i];
					Point3D p1 = new Point3D(p); setPointValue(p1, heightAxis, +height/2);
					Point3D p2 = new Point3D(p); setPointValue(p2, heightAxis, -height/2);
					lines[i] = new PolyLine(p1,p2);
				}
				return lines;
			}
		}
		
		static class Box extends Transform {
			Box(double sizeX, double sizeY, double sizeZ) {
				super(new GroupingNode(
					new PolyLine(new Point3D(0,0,0),new Point3D(1,0,0)),
					new PolyLine(new Point3D(0,0,0),new Point3D(0,1,0)),
					new PolyLine(new Point3D(0,0,0),new Point3D(0,0,1)),
					new PolyLine(new Point3D(1,1,1),new Point3D(0,1,1)),
					new PolyLine(new Point3D(1,1,1),new Point3D(1,0,1)),
					new PolyLine(new Point3D(1,1,1),new Point3D(1,1,0)),
					new PolyLine(new Point3D(1,0,0),new Point3D(1,1,0)),
					new PolyLine(new Point3D(1,0,0),new Point3D(1,0,1)),
					new PolyLine(new Point3D(0,1,0),new Point3D(1,1,0)),
					new PolyLine(new Point3D(0,1,0),new Point3D(0,1,1)),
					new PolyLine(new Point3D(0,0,1),new Point3D(0,1,1)),
					new PolyLine(new Point3D(0,0,1),new Point3D(1,0,1))
				));
				addScale(new Point3D(sizeX, sizeY, sizeZ));
				addTranslation(new Point3D(-sizeX/2, -sizeY/2, -sizeZ/2));
			}
		}
		
		static interface LoopArcReceiver {
			public void setValue( int i, int nSeg, double x, double y );
		}
		
		@SuppressWarnings("unused")
		static void loopArc(double radius, double startAngle_deg, double endAngle_deg, boolean flipped, LoopArcReceiver receiver) {
			loopArc(radius, startAngle_deg, endAngle_deg, flipped, -1, receiver);
		}
		
		static void loopArc(double radius, double startAngle_deg, double endAngle_deg, boolean flipped, int nSeg, LoopArcReceiver receiver) {
			while (startAngle_deg    >endAngle_deg) endAngle_deg += 360;
			while (startAngle_deg+360<endAngle_deg) endAngle_deg -= 360;
			
			double angleDelta_deg = endAngle_deg-startAngle_deg;
			if (flipped) angleDelta_deg -= 360;
			if (nSeg<0) nSeg = (int)Math.ceil(Math.abs(angleDelta_deg)/360*CIRCLE_SEGMENTS);
			double segAngle_deg = angleDelta_deg/nSeg;
			
			for (int i=0; i<=nSeg; i++) {
				double x = radius * Math.cos( (i*segAngle_deg+startAngle_deg)/180*Math.PI );
				double y = radius * Math.sin( (i*segAngle_deg+startAngle_deg)/180*Math.PI );
				receiver.setValue(i,nSeg,x,y);
			}
		}
		
	}
}