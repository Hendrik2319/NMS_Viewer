package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.Component;
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.BuildingObject;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Point3D;
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
	
	private static final String VRML_TEMPLATE_MODELS_WRL = "/vrml/template.models.wrl";
	private static HashMap<String, String> mapObjectID2Model = null;

	static void writeToJSON(JSON_Object json_Object, File copyfile) {
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
		
		writeToJSON_obj(out,json_Object);
		
		out.close();
		
	}

	static void writeToHTML(String title, JSON_Object json_Object, File htmlfile) {
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
		
		writeToHTML_obj(out,json_Object,"obj");
		
		out.println("</body>");
		out.println("</html>");
		
		out.close();
	}

	static void writeToJSON_obj(PrintWriter out, JSON_Object json_Object) {
		if (json_Object==null) {
			out.print("null");
			return;
		}
		
		out.print("{");
		for (int i=0; i<json_Object.size(); ++i) {
			NamedValue namedvalue = json_Object.get(i);
			out.printf("\"%s\":",namedvalue.name);
			writeToJSON_value(out, namedvalue.value);
			if (i+1<json_Object.size()) out.print(",");
		}
		out.print("}");
	}

	static void writeToHTML_obj(PrintWriter out, JSON_Object json_Object, String ID) {
		if (json_Object==null) {
			out.print("null");
			return;
		}
		
		out.println("{ <span class=\"button\" onclick=\"toggle_collapse(this,'"+ID+"');\">[+]</span>");
		out.println("<div id="+ID+" class=\"valuelist hidden\">");
		
		for (int i=0; i<json_Object.size(); ++i) {
			NamedValue namedvalue = json_Object.get(i);
			out.printf("<span class=\"name\">\"%s\"</span> : ",namedvalue.name);
			writeToHTML_value(out, namedvalue.value, ID, i);
			out.println(",<br/>");
		}
		
		out.println("</div>");
		out.println("}");
	}

	static void writeToJSON_arr(PrintWriter out, JSON_Array json_Array) {
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

	static void writeToHTML_arr(PrintWriter out, JSON_Array json_Array, String ID) {
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

	static void writeToJSON_value(PrintWriter out, Value value) {
		if ( value instanceof StringValue  ) out.printf(Locale.ENGLISH,"\"%s\"", ((StringValue )value).value);
		if ( value instanceof FloatValue   ) out.printf(Locale.ENGLISH,"%s"    , ((FloatValue  )value).value);
		if ( value instanceof IntegerValue ) out.printf(Locale.ENGLISH,"%d"    , ((IntegerValue)value).value);
		if ( value instanceof BoolValue    ) out.printf(Locale.ENGLISH,"%s"    , ((BoolValue   )value).value);
		if ( value instanceof ObjectValue  ) writeToJSON_obj(out, ((ObjectValue)value).value);
		if ( value instanceof ArrayValue   ) writeToJSON_arr(out, ((ArrayValue )value).value);
	}

	static void writeToHTML_value(PrintWriter out, Value value, String ID, int i) {
		if ( value instanceof StringValue  ) out.printf(Locale.ENGLISH,"<span class=\"string\">"+"\"%s\""+"</span>", ((StringValue )value).value);
		if ( value instanceof FloatValue   ) out.printf(Locale.ENGLISH,"<span class=\"number\">"+"%s"    +"</span>", ((FloatValue  )value).value);
		if ( value instanceof IntegerValue ) out.printf(Locale.ENGLISH,"<span class=\"number\">"+"%d"    +"</span>", ((IntegerValue)value).value);
		if ( value instanceof BoolValue    ) out.printf(Locale.ENGLISH,"<span class=\"number\">"+"%s"    +"</span>", ((BoolValue   )value).value);
		if ( value instanceof ObjectValue  ) writeToHTML_obj(out, ((ObjectValue)value).value, ID+"_"+i);
		if ( value instanceof ArrayValue   ) writeToHTML_arr(out, ((ArrayValue )value).value, ID+"_"+i);
	}

	public static void writePosToVRML_models(BuildingObject[] objects, SaveGameData.PersistentPlayerBase playerbase, Component parent) {
		if (objects==null && playerbase!=null) objects = playerbase.objects;
		if (objects==null) return;
		System.out.println("Write positions of "+objects.length+" BuildingObjects to VRML file ...");
		
		JFileChooser fc = new JFileChooser("./");
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setMultiSelectionEnabled(false);
		fc.addChoosableFileFilter(new FileNameExtensionFilter("VRML-File (*.wrl)","wrl"));;
		
		if (fc.showSaveDialog(parent)!=JFileChooser.APPROVE_OPTION) return;
		File file = fc.getSelectedFile();
		
		SaveGameData.Point3D min = null;
		SaveGameData.Point3D max = null;
		
		for (BuildingObject obj:objects) {
			if (obj.position==null) continue;
			if (obj.position.pos==null) continue;
			SaveGameData.Point3D pos = new SaveGameData.Point3D(obj.position.pos);
			if (min==null) min = new SaveGameData.Point3D(pos); else min.min(pos);
			if (max==null) max = new SaveGameData.Point3D(pos); else max.max(pos);
		}
		double size = Math.max(Math.max(max.x-min.x,max.y-min.y),max.z-min.z)/200;
		
		StringBuilder templateSB = new StringBuilder();
		InputStream is = templateSB.getClass().getResourceAsStream(VRML_TEMPLATE_MODELS_WRL);
		try (BufferedReader template = new BufferedReader( new InputStreamReader( is, StandardCharsets.UTF_8 ) )) {
			template.lines().forEach(str->templateSB.append(str).append("\r\n"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		try (PrintWriter vrml = new PrintWriter(new BufferedWriter( new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8)))) {
			
			vrml.println(templateSB.toString());
			vrml.println("");
			
			//writeBioroomCoords(vrml);
			//writeMainroomCoords(vrml);
			
			if (playerbase!=null) {
				String name = playerbase.name;
				if (name==null || name.isEmpty()) name = "PlayerBase";
				if (playerbase.position!=null && playerbase.forward!=null) {
					Point3D pos = new Point3D(0,0,0);
					Point3D at = new SaveGameData.Point3D(playerbase.position).normalize();
					Point3D up = playerbase.forward;
					writeModel(vrml, "^MAINROOM", name, pos, at, up, size);
				}
			}
			
			for (BuildingObject obj:objects) {
				if (obj.position==null) continue;
				if (obj.position.pos==null) continue;
				if (obj.position.up==null) continue;
				if (obj.position.at==null) continue;
				
				String objectID = obj.objectID;
				GeneralizedID id = GameInfos.productIDs.get(objectID);
				String label = objectID;
				if (id!=null && !id.label.isEmpty()) label = id.label;
				
				writeModel(vrml, objectID, label, obj.position.pos, obj.position.at, obj.position.up, size);
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		System.out.println("done");
	}
	
	public static void prepareModels() {
		mapObjectID2Model = new HashMap<String,String>();
		addModels("BIOROOM",		"^BIOROOM");
		addModels("MAINROOM",		"^MAINROOM");
		addModels("MAINROOMCUBE",	"^MAINROOMCUBE");
		
		addModels("CUBESTAIRS", 	"^CUBESTAIRS");
		addModels("BUILDDOOR",		"^BUILDDOOR");
		
		addModels("CUBEROOM",		"^CONTAINER0","^CONTAINER1","^CONTAINER2","^CONTAINER3","^CONTAINER4","^CONTAINER5",
									"^CONTAINER6","^CONTAINER7","^CONTAINER8","^CONTAINER9","^CUBEROOM","^CUBEGLASS");
		
		addModels("CORRIDOR",   	"^CORRIDOR","^GLASSCORRIDOR");
		addModels("CORRIDORX",		"^CORRIDORX");
		
		addModels("PLANT",			"^LUSHPLANT","^BARRENPLANT","^CREATUREPLANT","^PEARLPLANT","^SCORCHEDPLANT",
									"^SNOWPLANT","^RADIOPLANT","^TOXICPLANT","^POOPPLANT","^SACVENOMPLANT","^NIPPLANT");
		
		addModels("PLANTER",		"^PLANTER");
		addModels("PLANTERMEGA",	"^PLANTERMEGA");
		addModels("CARBONPLANTER",	"^CARBONPLANTER");
	}
	
	private static void addModels(String modelName, String... objectIDs) {
		for (String objectID:objectIDs)
			mapObjectID2Model.put(objectID, modelName);
	}

	private static void writeModel(PrintWriter vrml, String objectID, String label, Point3D pos, Point3D at, Point3D up, double size) {
		vrml.print("MyOrientation {");
		vrml.printf(Locale.ENGLISH," pos %1.2f %1.2f %1.2f", pos.x, pos.y, pos.z);
		vrml.printf(Locale.ENGLISH," at %1.4f %1.4f %1.4f", at.x, at.y, at.z);
		vrml.printf(Locale.ENGLISH," up %1.4f %1.4f %1.4f", up.x, up.y, up.z);
		
		vrml.print (" children");
		String modelName = mapObjectID2Model.get(objectID);
		
		if (modelName!=null)
			vrml.printf(" %s { string \"%s\" }", modelName, label);
		else
			switch (objectID) {
			default:
				vrml.printf(Locale.ENGLISH," AxisCross { scale %1.3f %1.3f %1.3f string \"%s\" }", size/2, size/2, size/2, objectID);
				break;
			}
		vrml.println(" }");
	}

	@SuppressWarnings("unused")
	private static void writeMainroomCoords(PrintWriter vrml) {
		int segI=16;
		double radius = 1.5*4;
		double height = 4;
		
		vrml.println("# cylinder");
		vrml.print("# coord Coordinate { point [ ");
		for (int i=0; i<segI; ++i) {
			double wI = i*Math.PI*2/segI;
			double y = radius*Math.sin(wI);
			double z = radius*Math.cos(wI);
			vrml.printf(Locale.ENGLISH,"%1.2f %1.2f %1.2f, ", 0.0,y,z);
			vrml.printf(Locale.ENGLISH,"%1.2f %1.2f %1.2f, ", height,y,z);
		}
		vrml.println(" ] }");
		
		vrml.print("# coordIndex [ ");
		String str1 = "", str2 = "";
		for (int i=0; i<segI; ++i) {
			vrml.printf("%d %d -1 ", i*2, i*2+1);
			str1 += (i*2  )+" ";
			str2 += (i*2+1)+" ";
		}
		str1 += "0 -1 ";
		str2 += "1 -1 ";
		vrml.println(str1+str2+"]");
	}

	@SuppressWarnings("unused")
	private static void writeBioroomCoords(PrintWriter vrml) {
		int segI=16, segJ=4;
		double radius = 1.5*4;
		
		vrml.println("# hemisphere");
		vrml.print("# coord Coordinate { point [ ");
		for (int j=0; j<segJ; ++j) {
			double wJ = j*Math.PI/2/segJ;
			double x = radius*Math.sin(wJ);
			double r2 = radius*Math.cos(wJ);
			for (int i=0; i<segI; ++i) {
				double wI = i*Math.PI*2/segI;
				double y = r2*Math.sin(wI);
				double z = r2*Math.cos(wI);
				vrml.printf(Locale.ENGLISH,"%1.2f %1.2f %1.2f, ", x,y,z);
			}
		}
		vrml.println(" ] }");
		
		vrml.print("# coordIndex [ ");
		for (int j=0; j<segJ; ++j) {
			for (int i=0; i<segI; ++i)
				vrml.printf("%d ", j*segI+i);
			vrml.printf("%d -1 ", j*segI);
		}
		for (int i=0; i<segI; ++i) {
			for (int j=0; j<segJ; ++j)
				vrml.printf("%d ", j*segI+i);
			vrml.print("-1 ");
		}
		vrml.println("]");
	}

	public static void writePosToVRML_simple(BuildingObject[] objects, Component parent) {
		System.out.println("Write positions of "+objects.length+" BuildingObjects to VRML file ...");
		
		JFileChooser fc = new JFileChooser("./");
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setMultiSelectionEnabled(false);
		fc.addChoosableFileFilter(new FileNameExtensionFilter("VRML-File (*.wrl)","wrl"));;
		
		if (fc.showSaveDialog(parent)!=JFileChooser.APPROVE_OPTION) return;
		File file = fc.getSelectedFile();
		
		SaveGameData.Point3D min = null;
		SaveGameData.Point3D max = null;
		
		Vector<SaveGameData.Point3D> arrPos = new Vector<>();
		
		for (BuildingObject obj:objects) {
			if (obj.position==null) continue;
			if (obj.position.pos==null) continue;
			SaveGameData.Point3D pos = new SaveGameData.Point3D(obj.position.pos); arrPos.add(pos);
			if (min==null) min = new SaveGameData.Point3D(pos); else min.min(pos);
			if (max==null) max = new SaveGameData.Point3D(pos); else max.max(pos);
		}
		double size = Math.max(Math.max(max.x-min.x,max.y-min.y),max.z-min.z)/200;
		
		Vector<SaveGameData.Point3D> arrUp = new Vector<>();
		Vector<SaveGameData.Point3D> arrAt = new Vector<>();
		for (BuildingObject obj:objects) {
			if (obj.position==null) continue;
			if (obj.position.pos==null) continue;
			SaveGameData.Point3D pos = new SaveGameData.Point3D(obj.position.pos);
			if (obj.position.up!=null) arrUp.add(pos.add(new SaveGameData.Point3D(obj.position.up).normalize().mul(size)));
			if (obj.position.at!=null) arrAt.add(pos.add(new SaveGameData.Point3D(obj.position.at).normalize().mul(size)));
		}
		
		try (PrintWriter vrml = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8))) {
			
			vrml.println("#VRML V2.0 utf8");
			vrml.println("");
			vrml.println("Background { skyColor 0.6 0.7 0.8 }");
			vrml.println("");
			vrml.println("PROTO ColoredSphere [");
			vrml.println("	field SFFloat radius 1000 # in m");
			vrml.println("	field SFVec3f pos 0 0 0");
			vrml.println("	field SFColor color 0.2 0.2 0.2");
			vrml.println("] {");
			vrml.println("	Transform {");
			vrml.println("		translation IS pos");
			vrml.println("		children [");
			vrml.println("			Shape {");
			vrml.println("				appearance Appearance { material Material { diffuseColor IS color } }");
			vrml.println("				geometry Sphere { radius IS radius }");
			vrml.println("			}");
			vrml.println("		]");
			vrml.println("	}");
			vrml.println("}");
			vrml.println("");
			
			SaveGameData.Point3D origin = new SaveGameData.Point3D(0,0,0);
			for (SaveGameData.Point3D p:arrPos) vrml.printf(Locale.ENGLISH,"ColoredSphere { radius %1.2f color 1 1 1 pos %1.2f %1.2f %1.2f } # Pos r:%f\r\n", size/2, p.x, p.y, p.z, p.distTo(origin));
			for (SaveGameData.Point3D p:arrAt ) vrml.printf(Locale.ENGLISH,"ColoredSphere { radius %1.2f color 1 0 0 pos %1.2f %1.2f %1.2f } # At  r:%f\r\n", size/2, p.x, p.y, p.z, p.distTo(origin));
			for (SaveGameData.Point3D p:arrUp ) vrml.printf(Locale.ENGLISH,"ColoredSphere { radius %1.2f color 0 1 0 pos %1.2f %1.2f %1.2f } # Up  r:%f\r\n", size/2, p.x, p.y, p.z, p.distTo(origin));
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		System.out.println("done");
	}
}