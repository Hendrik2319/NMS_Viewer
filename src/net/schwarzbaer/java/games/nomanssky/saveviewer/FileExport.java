package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.Color;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Stack;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.BuildingObject;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Point3D;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Position;
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
		
		Point3D min = null;
		Point3D max = null;
		
		for (BuildingObject obj:objects) {
			if (obj.position==null) continue;
			if (obj.position.pos==null) continue;
			Point3D pos = new Point3D(obj.position.pos);
			if (min==null) min = new Point3D(pos); else min.min(pos);
			if (max==null) max = new Point3D(pos); else max.max(pos);
		}
		double size = 0;
		if (max!=null && min!=null)
			size = Math.max(Math.max(max.x-min.x,max.y-min.y),max.z-min.z)/200;
		size = Math.max(size, 0.25);
		
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
			
			//vrml.println("# race initiator"); writeExocraftPodCoords(vrml, 8,6,16,4, 4,1,16,4, Math.PI/4);
			//vrml.println("# Koloss"); writeExocraftPodCoords(vrml, 7.00,5.0,18,3, 3.5,1.5,18,3, Math.PI/2);
			//vrml.println("# Roamer"); writeExocraftPodCoords(vrml, 5.25,3.5,18,3, 2.5,1.0,18,3, Math.PI/2);
			//vrml.println("# Nomad" ); writeExocraftPodCoords(vrml, 4.75,3.0,18,3, 2.0,1.0,18,3, Math.PI/2);
			
			if (playerbase!=null && !playerbase.isFreighterBase) {
				String name = playerbase.name;
				if (name==null || name.isEmpty()) name = "PlayerBase";
				if (playerbase.position!=null && playerbase.forward!=null) {
					Point3D pos = new Point3D(0,0,0);
					Point3D at = playerbase.position.isZero()?null:playerbase.position.normalize();
					Point3D up = playerbase.forward .isZero()?null:playerbase.forward .normalize();
					writeModel(vrml, "^MAINROOM", name, pos, at, up, size, null);
				}
			}
			
			CubeCombine cubeCombine = /*new CubeCombine_Dummy();*/ new CubeCombine();
			if (playerbase!=null) {
				Point3D at = Point3D.normalizeOrNull(playerbase.position);
				Point3D up = Point3D.normalizeOrNull(playerbase.forward);
				if (at!=null && up!=null)
					cubeCombine.setBaseOrientation(at,up);
			}
			
			for (BuildingObject obj:objects)
				if (!cubeCombine.add(obj))
					writeModel(vrml, obj, size);
			
			cubeCombine.writeModel(vrml);
			
			BuildingObject[] remainingObjects = cubeCombine.getRemainingObjects();
			for (BuildingObject obj:remainingObjects)
				writeModel(vrml, obj, size);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		System.out.println("done");
	}

	@SuppressWarnings("unused")
	private static class CubeCombine_Dummy extends CubeCombine {
		CubeCombine_Dummy() { System.out.println("\r\n\r\n##############################\r\nCubeCombine_Dummy\r\n##############################\r\n\r\n\r\n"); }
		@Override public boolean add(BuildingObject obj) { return false; }
		@Override public BuildingObject[] getRemainingObjects() { return new BuildingObject[0]; }
		@Override public void writeModel(PrintWriter vrml) {}
	}

	private static class CubeCombine {
		
		Stack<BuildingObject> freeObj;
		Stack<BuildingObject> remainingObj;
		private Point3D baseAt;
		private Point3D baseUp;
		
		public CubeCombine() {
			freeObj = new Stack<>();
			remainingObj = new Stack<>();
			this.baseAt = null;
			this.baseUp = null;
			
//			Vector<Line> lines = new Vector<>();
//			lines.add(new Line(new Index3D(4,5,6),new Index3D(1,2,3)));
//			boolean wasFound = lines.remove(new Line(new Index3D(4,5,6),new Index3D(1,2,3)));
//			System.out.println("#########\r\nVector.remove "+(wasFound?"":"DOESN'T ")+"found specified line.\r\n#########");
			
//			HashMap<Index3D, Integer> blocks = new HashMap<>();
//			Integer prev;
//			prev = blocks.put(new Index3D(0,1,2), 3); System.out.println("blocks.put(new Index3D(0,1,2), 3) -> "+prev);
//			prev = blocks.put(new Index3D(0,1,2), 4); System.out.println("blocks.put(new Index3D(0,1,2), 4) -> "+prev);
//			prev = blocks.get(new Index3D(0,1,2));    System.out.println("blocks.get(new Index3D(0,1,2))    -> "+prev);
//			
//			System.out.println("new Index3D(0,1,2) == new Index3D(0,1,2)       -> "+( new Index3D(0,1,2) == new Index3D(0,1,2) ));
//			System.out.println("new Index3D(0,1,2).equals(new Index3D(0,1,2))  -> "+( new Index3D(0,1,2).equals(new Index3D(0,1,2)) ));
			
//			HashMap<Line, Integer> blocks = new HashMap<>();
//			Integer prev;
//			prev = blocks.put(new Line(new Index3D(4,5,6),new Index3D(1,2,3)), 3); System.out.println("blocks.put(new Line(new Index3D(4,5,6),new Index3D(1,2,3)), 3) -> "+prev);
//			prev = blocks.put(new Line(new Index3D(4,5,6),new Index3D(1,2,3)), 4); System.out.println("blocks.put(new Line(new Index3D(4,5,6),new Index3D(1,2,3)), 4) -> "+prev);
//			prev = blocks.put(new Line(new Index3D(1,2,3),new Index3D(4,5,6)), 5); System.out.println("blocks.put(new Line(new Index3D(1,2,3),new Index3D(4,5,6)), 5) -> "+prev);
//			prev = blocks.get(new Line(new Index3D(4,5,6),new Index3D(1,2,3)));    System.out.println("blocks.get(new Line(new Index3D(4,5,6),new Index3D(1,2,3)))    -> "+prev);
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
			default:
				return false;
			}
		}

		private BuildingObject fixUpsideDown(BuildingObject obj) {
			if (baseAt==null || baseUp==null) return obj;
			if (obj==null) return obj;
			if (obj.position==null) return obj;
			if (obj.position.pos==null) return obj;
			
			Point3D at = Point3D.normalizeOrNull(obj.position.at);
			Point3D up = Point3D.normalizeOrNull(obj.position.up);
			if (at==null || up==null) return obj;
			
			double val = baseAt.scalarProd(at);
			if (Math.abs(val+1)>Neighborhood.ANGLE_TOLERANCE) return obj;
			
			BuildingObject newObj = new BuildingObject(obj);
			newObj.position.pos.set(newObj.position.pos.add(at.mul(Neighborhood.CUBESIZE)));
			newObj.position.at .set(at.mul(-1));
			
			if (newObj.objectID.equals("^CUBEROOMCURVED"))
				newObj.position.up.set(at.mul(-1).crossProd(up));
			
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
//				if (neighborhood.hasOnlyStartObj())
//					remainingObj.push(obj);
//				else
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
			private static final double CUBESIZE = 4.0;
			private static final double POS_TOLERANCE = 0.01;
			
			private HashMap<Index3D, Neighbor> blocks;
			private Point3D anchorPos;
			private Point3D anchorXat;
			private Point3D anchorYup;
			private Point3D anchorZ;
			private int neighborhoodIndex;
			
			public Neighborhood(BuildingObject firstObj, int neighborhoodIndex) {
				this.neighborhoodIndex = neighborhoodIndex;
				anchorPos = null;
				anchorXat = null;
				anchorYup = null;
				anchorZ   = null;
				if (firstObj.position!=null) {
					anchorPos = firstObj.position.pos;
					anchorXat = Point3D.normalizeOrNull(firstObj.position.at);
					anchorYup = Point3D.normalizeOrNull(firstObj.position.up);
					if (anchorXat!=null && anchorYup!=null) anchorZ = anchorXat.crossProd(anchorYup).normalize();
				}
				Index3D origin = new Index3D(0,0,0);
				blocks = new HashMap<Index3D,Neighbor>();
				blocks.put(origin, new Neighbor(origin, Orientation.PosY, firstObj));
			}
			
			private Neighbor getNeighborRelation(Position position, BuildingObject obj) {
				if (position==null) return null;
				if (position.pos==null) return null;
				if (position.at==null || position.at.isZero()) return null;
				if (position.up==null || position.up.isZero()) return null;
				
				double valAt = anchorXat.scalarProd(position.at.normalize());
				if (Math.abs(valAt-1.0)>ANGLE_TOLERANCE) return null;
				
				Orientation orientation = getOrientation(position);
				if (orientation==null) return null;
				
				Point3D indexVec = position.pos.add(anchorPos.mul(-1));
				double iX_d=anchorXat.scalarProd(indexVec)/CUBESIZE; int iX=(int)Math.round(iX_d); double deltaX=Math.abs(iX_d-iX);
				double iY_d=anchorYup.scalarProd(indexVec)/CUBESIZE; int iY=(int)Math.round(iY_d); double deltaY=Math.abs(iY_d-iY);
				double iZ_d=anchorZ  .scalarProd(indexVec)/CUBESIZE; int iZ=(int)Math.round(iZ_d); double deltaZ=Math.abs(iZ_d-iZ);
				
				//System.out.printf(Locale.ENGLISH,"Neighborhood %d:   At:%1.8f   Up:%11.8f   iX:%1.8f   iY:%1.8f   iZ:%1.8f\r\n", neighborhoodIndex, valAt, valUp, deltaX, deltaY, deltaZ);
				
				if (deltaX>POS_TOLERANCE) return null;
				if (deltaY>POS_TOLERANCE) return null;
				if (deltaZ>POS_TOLERANCE) return null;
				
				return new Neighbor(new Index3D(iX,iY,iZ), orientation, obj);
			}

			private Orientation getOrientation(Position position) {
				Point3D up = position.up.normalize();
				double valUp = anchorYup.scalarProd(up);
				if (Math.abs(valUp-1)<ANGLE_TOLERANCE) return Orientation.PosY;
				if (Math.abs(valUp+1)<ANGLE_TOLERANCE) return Orientation.NegY;
				
				if (Math.abs(valUp)>ANGLE_TOLERANCE) return null;
				
				if (anchorXat.scalarProd(anchorYup.crossProd(up))>0) return Orientation.PosZ;
				return Orientation.NegZ;
			}

			public void addNeighbors(Stack<BuildingObject> freeObj) {
				if (anchorPos==null) return;
				if (anchorXat==null) return;
				if (anchorYup==null) return;
				if (anchorZ  ==null) return;
				
				for (BuildingObject obj:new Vector<BuildingObject>(freeObj)) {
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
					FileExport.writeModel(vrml, n.obj.objectID, String.format("N%d Obj%d", neighborhoodIndex, ++i), n.obj.position.pos, n.obj.position.at, n.obj.position.up, 0.5, color);
			}

			public void writeModel(PrintWriter vrml) {
				Index3D minCorner = new Index3D(0,0,0);
				Index3D size = new Index3D(0,0,0);
				Neighbor[][][] mat = toMatrix(minCorner,size);
				new Geometry(this,minCorner,size,mat).writeModel(vrml);
			}

			private Point3D computePoint(double iX, double iY, double iZ) {
				return anchorPos
						.add(anchorXat.mul((iX    )*CUBESIZE))
						.add(anchorYup.mul((iY-0.5)*CUBESIZE))
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
					writeIndexedLineSet(vrml, pointsStrBase, indexesStrBase, Color.BLACK);
				
				StringBuilder pointsStrW, indexesStrW;
				createWindows(pointsStrW = new StringBuilder(), 0, indexesStrW = new StringBuilder());
				if (pointsStrW.length()>0 || indexesStrW.length()>0)
					writeIndexedLineSet(vrml, pointsStrW, indexesStrW, new Color(0.3f, 0.5f, 1.0f));
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
		
			private void writeIndexedLineSet(PrintWriter vrml, StringBuilder pointsStr, StringBuilder indexesStr, Color color) {
				vrml.println("Shape {");
				vrml.print  ("	appearance Appearance { material Material { ");
				vrml.printf (Locale.ENGLISH, "emissiveColor %1.3f %1.3f %1.3f", color.getRed()/255.0f, color.getGreen()/255.0f, color.getBlue()/255.0f);
				vrml.println(" } }");
				vrml.println("	geometry IndexedLineSet {");
				vrml.printf ("		coord Coordinate { point [ %s ] }\r\n",pointsStr.toString());
				vrml.printf ("		coordIndex [ %s ]\r\n",indexesStr.toString());
				vrml.println("	}");
				vrml.println("}");
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
					
					addPoint(pointsStr, computePoint( i1_p.mul(0.9).add(i3_p.mul(0.1)) )); int iw1=counter++;
					addPoint(pointsStr, computePoint( i2_p.mul(0.9).add(i4_p.mul(0.1)) )); int iw2=counter++;
					addPoint(pointsStr, computePoint( i3_p.mul(0.9).add(i1_p.mul(0.1)) )); int iw3=counter++;
					addPoint(pointsStr, computePoint( i4_p.mul(0.9).add(i2_p.mul(0.1)) )); int iw4=counter++;
					
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
		
		addModels("RACE_START",	    "^RACE_START");
		addModels("GARAGE_L",	    "^GARAGE_L");
		addModels("GARAGE_M",	    "^GARAGE_M");
		addModels("GARAGE_S",	    "^GARAGE_S");
	}
	
	private static void addModels(String modelName, String... objectIDs) {
		for (String objectID:objectIDs)
			mapObjectID2Model.put(objectID, modelName);
	}

	private static void writeModel(PrintWriter vrml, BuildingObject obj, double size) {
		if (obj.position==null) return;
		if (obj.position.pos==null) return;
		if (obj.position.up==null) return;
		if (obj.position.at==null) return;
		
		String objectID = obj.objectID;
		GameInfos.GeneralizedID id = GameInfos.productIDs.get(objectID,obj.source,GameInfos.GeneralizedID.Usage.Type.BuildingObject);
		String label = objectID;
		if (id!=null && !id.label.isEmpty()) label = id.label;
		
		writeModel(vrml, objectID, label, obj.position.pos, obj.position.at, obj.position.up, size, null);
	}

	private static void writeModel(PrintWriter vrml, String objectID, String label, Point3D pos, Point3D at, Point3D up, double size, Point3D color) {
		vrml.print("MyOrientation {");
		vrml.printf(Locale.ENGLISH," pos %1.2f %1.2f %1.2f", pos.x, pos.y, pos.z);
		if (at!=null) vrml.printf(Locale.ENGLISH," at %1.4f %1.4f %1.4f", at.x, at.y, at.z);
		if (up!=null) vrml.printf(Locale.ENGLISH," up %1.4f %1.4f %1.4f", up.x, up.y, up.z);
		
		vrml.print (" children");
		String modelName = mapObjectID2Model.get(objectID);
		
		if (modelName!=null)
			vrml.printf(" %s { string \"%s\"%s }", modelName, label, (color==null?"":(" lineColor "+color.toString("%1.2f",false))));
		else
			switch (objectID) {
			default:
				vrml.printf(Locale.ENGLISH," AxisCross { scale %1.3f %1.3f %1.3f string \"%s\" }", size/2, size/2, size/2, objectID);
				break;
			}
		vrml.println(" }");
	}

	@SuppressWarnings("unused")
	private static void writeExocraftPodCoords(PrintWriter vrml, double baseRadius, double topRadius, int segI, int modI, double outerRadius, double innerRadius, int segD, int modD, double wDelta) {
		double height = 1;
		
		vrml.print("# coord Coordinate { point [ ");
		
		for (int i=0; i<segI; ++i) {
			double wI = i*Math.PI*2/segI;
			double xBase = 0;
			double yBase = baseRadius*Math.sin(wI);
			double zBase = baseRadius*Math.cos(wI);
			double xTop = height;
			double yTop = topRadius*Math.sin(wI);
			double zTop = topRadius*Math.cos(wI);
			vrml.printf(Locale.ENGLISH,"%1.2f %1.2f %1.2f, ", xBase,yBase,zBase);
			vrml.printf(Locale.ENGLISH,"%1.2f %1.2f %1.2f, ", xTop,yTop,zTop);
		}
		
		for (int i=0; i<segD; ++i) {
			double wI = i*Math.PI*2/segD + wDelta;
			double xOuter = height;
			double yOuter = outerRadius*Math.sin(wI);
			double zOuter = outerRadius*Math.cos(wI);
			double xInner = height;
			double yInner = innerRadius*Math.sin(wI);
			double zInner = innerRadius*Math.cos(wI);
			vrml.printf(Locale.ENGLISH,"%1.2f %1.2f %1.2f, ", xOuter,yOuter,zOuter);
			vrml.printf(Locale.ENGLISH,"%1.2f %1.2f %1.2f, ", xInner,yInner,zInner);
		}
		
		vrml.println(" ] }");
		
		String strRing1, strRing2;
		vrml.print("# coordIndex [ ");
		
		strRing1 = ""; strRing2 = "";
		for (int i=0; i<segI; ++i) {
			if ((i%modI)==0) vrml.printf("%d %d -1 ", i*2, i*2+1);
			strRing1 += (i*2  )+" ";
			strRing2 += (i*2+1)+" ";
		}
		strRing1 += "0 -1 ";
		strRing2 += "1 -1 ";
		vrml.print(strRing1+strRing2);
		
		strRing1 = ""; strRing2 = "";
		for (int i=0; i<segD; ++i) {
			if ((i%modD)==0) vrml.printf("%d %d -1 ", i*2+2*segI, i*2+1+2*segI);
			strRing1 += (i*2  +2*segI)+" ";
			strRing2 += (i*2+1+2*segI)+" ";
		}
		strRing1 += (0+2*segI)+" -1 ";
		strRing2 += (1+2*segI)+" -1 ";
		vrml.print(strRing1+strRing2);
		
		vrml.println("]\r\n");
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

	public static void writePosToVRML_simple(BuildingObject[] objects, Double radius, Component parent) {
		System.out.println("Write positions of "+objects.length+" BuildingObjects to VRML file ...");
		
		if (radius!=null && radius<=0) radius=null;
		
		JFileChooser fc = new JFileChooser("./");
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setMultiSelectionEnabled(false);
		fc.addChoosableFileFilter(new FileNameExtensionFilter("VRML-File (*.wrl)","wrl"));;
		
		if (fc.showSaveDialog(parent)!=JFileChooser.APPROVE_OPTION) return;
		File file = fc.getSelectedFile();
		
		Point3D min = null;
		Point3D max = null;
		
		if (radius!=null) {
			min = new Point3D(-radius,-radius,-radius);
			max = new Point3D( radius, radius, radius);
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
			
			vrml.println("#VRML V2.0 utf8");
			vrml.println("");
			vrml.println("Background { skyColor 0.6 0.7 0.8 }");
			vrml.println("");
			vrml.println("PROTO Axis [");
			vrml.println("	field SFVec3f scale 1 1 1");
			vrml.println("	field SFVec3f pos 0 0 0");
			vrml.println("	field SFVec3f at  1 0 0");
			vrml.println("	field SFVec3f up  0 1 0");
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
			vrml.println("			Transform { translation IS at scale IS scale children [");
			vrml.println("				Shape { appearance Appearance { material Material { diffuseColor 1 0 0 } } geometry USE sphere }");
			vrml.println("			] }");
			vrml.println("			Transform { translation IS up scale IS scale children [");
			vrml.println("				Shape { appearance Appearance { material Material { diffuseColor 0 1 0 } } geometry USE sphere }");
			vrml.println("			] }");
			vrml.println("		]");
			vrml.println("	}");
			vrml.println("}");
			vrml.println("");
			
			if (radius!=null)
				writeSphere(vrml,radius, new Point3D(0,0,0), java.awt.Color.GRAY);
			
			for (BuildingObject obj:objects) {
				if (obj.position==null) continue;
				if (obj.position.pos==null) continue;
				Position p = obj.position;
				vrml.printf(               "Axis {");
				if (size>0) vrml.printf(Locale.ENGLISH," scale %1.2f %1.2f %1.2f", size, size, size);
				vrml.printf(Locale.ENGLISH," pos %1.2f %1.2f %1.2f", p.pos.x, p.pos.y, p.pos.z);
				if (p.up!=null && !p.up.isZero()) vrml.printf(" up %s", p.up.normalize().mul(size).toString("%1.3f",false));
				if (p.at!=null && !p.at.isZero()) vrml.printf(" at %s", p.at.normalize().mul(size).toString("%1.3f",false));
				vrml.printf(               " string \"%s\"", obj.getNameOfObjectID().replace('\"','_'));
				vrml.printf(Locale.ENGLISH," } # Pos r:%f\r\n", p.pos.length());
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		System.out.println("done");
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
		
		vrml.println("Shape {");
		vrml.print  ("	appearance Appearance { material Material { ");
		vrml.printf (Locale.ENGLISH, "emissiveColor %1.3f %1.3f %1.3f", color.getRed()/255.0f, color.getGreen()/255.0f, color.getBlue()/255.0f);
		vrml.println(" } }");
		vrml.println("	geometry IndexedLineSet {");
		vrml.printf ("		coord Coordinate { point [ %s ] }\r\n",coordPointsStr.toString());
		vrml.printf ("		coordIndex [ %s ]\r\n",coordIndexes.toString());
		vrml.println("	}");
		vrml.println("}");
	}
}