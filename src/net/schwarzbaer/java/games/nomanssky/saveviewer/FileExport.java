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
import java.util.Iterator;
import java.util.Locale;
import java.util.Stack;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID;
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
		
		public CubeCombine() {
			freeObj = new Stack<>();
			remainingObj = new Stack<>();
			
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
		}
		
		public boolean add(BuildingObject obj) {
			if (obj==null) return false;
			switch (obj.objectID) {
			case "^CUBEROOM": case "^CUBEGLASS":
				freeObj.add(obj);
				return true;
			default:
				return false;
			}
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
				if (neighborhood.hasOnlyStartObj())
					remainingObj.push(obj);
				else
					neighborhood.writeModel(vrml);
			}
		}
		
		private static class Neighborhood {
			private static final double ANGLE_TOLERANCE = 0.0001;
			private static final double CUBESIZE = 4.0;
			private static final double POS_TOLERANCE = 0.01;
			
			private static final Index3D ind111 = new Index3D(1,1,1);
			private static final Index3D ind110 = new Index3D(1,1,0);
			private static final Index3D ind101 = new Index3D(1,0,1);
			private static final Index3D ind100 = new Index3D(1,0,0);
			private static final Index3D ind011 = new Index3D(0,1,1);
			private static final Index3D ind010 = new Index3D(0,1,0);
			private static final Index3D ind001 = new Index3D(0,0,1);
			private static final Index3D ind000 = new Index3D(0,0,0);
			
			private Block block;
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
				block = new Block(new Index3D(0,0,0),firstObj);
			}

			public boolean hasOnlyStartObj() {
				return block.getNumberUsedBlocks()==1;
			}
			
			private Index3D getNeighborRelation(Position position) {
				if (position==null) return null;
				if (position.pos==null) return null;
				if (position.at==null || position.at.isZero()) return null;
				if (position.up==null || position.up.isZero()) return null;
				Point3D at = position.at.normalize();
				Point3D up = position.up.normalize();
				
				double valAt = anchorXat.scalarProd(at);
				if (Math.abs(valAt-1.0)>ANGLE_TOLERANCE) return null;
				
				double valUp = anchorYup.scalarProd(up);
				if (Math.abs(valUp)>ANGLE_TOLERANCE && Math.abs(valUp-1)>ANGLE_TOLERANCE && Math.abs(valUp+1)>ANGLE_TOLERANCE) return null;
				
				Point3D indexVec = position.pos.add(anchorPos.mul(-1));
				double iX_d=anchorXat.scalarProd(indexVec)/CUBESIZE; int iX=(int)Math.round(iX_d); double deltaX=Math.abs(iX_d-iX);
				double iY_d=anchorYup.scalarProd(indexVec)/CUBESIZE; int iY=(int)Math.round(iY_d); double deltaY=Math.abs(iY_d-iY);
				double iZ_d=anchorZ  .scalarProd(indexVec)/CUBESIZE; int iZ=(int)Math.round(iZ_d); double deltaZ=Math.abs(iZ_d-iZ);
				
				//System.out.printf(Locale.ENGLISH,"Neighborhood %d:   At:%1.8f   Up:%11.8f   iX:%1.8f   iY:%1.8f   iZ:%1.8f\r\n", neighborhoodIndex, valAt, valUp, deltaX, deltaY, deltaZ);
				
				if (deltaX>POS_TOLERANCE) return null;
				if (deltaY>POS_TOLERANCE) return null;
				if (deltaZ>POS_TOLERANCE) return null;
				
				return new Index3D(iX,iY,iZ);
			}

			public void addNeighbors(Stack<BuildingObject> freeObj) {
				if (anchorPos==null) return;
				if (anchorXat==null) return;
				if (anchorYup==null) return;
				if (anchorZ  ==null) return;
				
				for (BuildingObject obj:new Vector<BuildingObject>(freeObj)) {
					Index3D index = getNeighborRelation(obj.position);
					if (index==null) continue;
					block.set(index,obj);
					freeObj.remove(obj);
				}
			}

			@SuppressWarnings("unused")
			public void writeModel_simple(PrintWriter vrml) {
				Point3D color = new Point3D(1,0,0);
				int i=0;
				for (BuildingObject obj:block)
					FileExport.writeModel(vrml, obj.objectID, String.format("N%d Obj%d", neighborhoodIndex, ++i), obj.position.pos, obj.position.at, obj.position.up, 0.5, color);
			}

			public void writeModel(PrintWriter vrml) {
				Index3D minCorner = new Index3D(0,0,0);
				Index3D size = new Index3D(0,0,0);
				BuildingObject[][][] mat = block.toMatrix(minCorner,size);
				
				Vector<Line> lines = createLines(size, mat);
				optimize(lines);
				
				writeBaseModel(vrml, minCorner, size, lines);
				addWindows(vrml, minCorner, size, mat);
			}

			private Vector<Line> createLines(Index3D size, BuildingObject[][][] mat) {
				Vector<Line> lines = new Vector<>();
				Index3D i1 = new Index3D(0,0,0);
				for (int x=0; x<size.iX; ++x)
					for (int y=0; y<size.iY; ++y)
						for (int z=0; z<size.iZ; ++z)
							if (get(mat,x,y,z)!=null) {
								i1.set(x,y,z);
								if (get(mat,x+1,y,z)==null) lines.add(new Line(i1.add(ind111), i1.add(ind110)));
								if (get(mat,x,y+1,z)==null) lines.add(new Line(i1.add(ind011), i1.add(ind010)));
								if (get(mat,x-1,y,z)==null) lines.add(new Line(i1.add(ind001), i1.add(ind000)));
								if (get(mat,x,y-1,z)==null) lines.add(new Line(i1.add(ind101), i1.add(ind100)));
								
								if (get(mat,x+1,y,z)==null) lines.add(new Line(i1.add(ind110), i1.add(ind100)));
								if (get(mat,x,y,z+1)==null) lines.add(new Line(i1.add(ind111), i1.add(ind101)));
								if (get(mat,x-1,y,z)==null) lines.add(new Line(i1.add(ind011), i1.add(ind001)));
								if (get(mat,x,y,z-1)==null) lines.add(new Line(i1.add(ind010), i1.add(ind000)));
								
								if (get(mat,x,y+1,z)==null) lines.add(new Line(i1.add(ind111), i1.add(ind011)));
								if (get(mat,x,y,z+1)==null) lines.add(new Line(i1.add(ind101), i1.add(ind001)));
								if (get(mat,x,y-1,z)==null) lines.add(new Line(i1.add(ind100), i1.add(ind000)));
								if (get(mat,x,y,z-1)==null) lines.add(new Line(i1.add(ind110), i1.add(ind010)));
							}
				return lines;
			}

			private BuildingObject get(BuildingObject[][][] mat, int x, int y, int z) {
				if (x<0) return null;
				if (y<0) return null;
				if (z<0) return null;
				if (x>=mat.length) return null;
				if (y>=mat[x].length) return null;
				if (z>=mat[x][y].length) return null;
				return mat[x][y][z];
			}

			private void optimize(Vector<Line> lines) {
				Vector<Line> optimizedLines = new Vector<>();
				
				while (!lines.isEmpty()) {
					Line line = lines.remove(lines.size()-1);
					Index3D vec12 = line.p2.sub(line.p1);
					while (lines.remove(new Line(line.p2,line.p2.add(vec12)))) line.p2 = line.p2.add(vec12);
					while (lines.remove(new Line(line.p1,line.p1.sub(vec12)))) line.p1 = line.p1.sub(vec12);
					optimizedLines.add(line);
				}
				
				lines.addAll(optimizedLines);
			}

			private void writeIndexedLineSet(PrintWriter vrml, StringBuilder coordPointsStr, StringBuilder coordIndexesStr, Color color) {
				vrml.println("Shape {");
				vrml.print  ("	appearance Appearance { material Material { ");
				vrml.printf (Locale.ENGLISH, "emissiveColor %1.3f %1.3f %1.3f", color.getRed()/255.0f, color.getGreen()/255.0f, color.getBlue()/255.0f);
				vrml.println(" } }");
				vrml.println("	geometry IndexedLineSet {");
				vrml.printf ("		coord Coordinate { point [ %s ] }\r\n",coordPointsStr.toString());
				vrml.printf ("		coordIndex [ %s ]\r\n",coordIndexesStr.toString());
				vrml.println("	}");
				vrml.println("}");
			}

			private void writeBaseModel(PrintWriter vrml, Index3D minCorner, Index3D size, Vector<Line> lines) {
				int[][][] coordIndexes = new int[size.iX+1][size.iY+1][size.iZ+1];
				for (int x=0; x<=size.iX; ++x)
					for (int y=0; y<=size.iY; ++y)
						Arrays.fill(coordIndexes[x][y],-1);
				
				// convert lines to points and indexes
				StringBuilder coordPointsStr = new StringBuilder();
				StringBuilder coordIndexesStr = new StringBuilder();
				int coordPointsCounter = 0;
				for (Line line:lines) {
					coordPointsCounter = addPoint(minCorner, coordIndexes, coordPointsStr, coordPointsCounter, line.p1);
					coordPointsCounter = addPoint(minCorner, coordIndexes, coordPointsStr, coordPointsCounter, line.p2);
					int ip1 = coordIndexes[line.p1.iX][line.p1.iY][line.p1.iZ];
					int ip2 = coordIndexes[line.p2.iX][line.p2.iY][line.p2.iZ];
					coordIndexesStr.append(ip1+" "+ip2+" -1 ");
				}
				if (coordPointsStr.length()>0 || coordIndexesStr.length()>0)
					writeIndexedLineSet(vrml, coordPointsStr, coordIndexesStr, Color.BLACK);
			}

			private void addWindows(PrintWriter vrml, Index3D minCorner, Index3D size, BuildingObject[][][] mat) {
				StringBuilder coordPointsStr2 = new StringBuilder();
				StringBuilder coordIndexesStr2 = new StringBuilder();
				int coordPointsCounter2 = 0;
				Index3D i2 = new Index3D(0,0,0);
				for (int x=0; x<size.iX; ++x)
					for (int y=0; y<size.iY; ++y)
						for (int z=0; z<size.iZ; ++z) {
							BuildingObject obj = get(mat,x,y,z);
							if (obj==null) continue;
							if (!"^CUBEGLASS".equals(obj.objectID)) continue;
							i2.set(x,y,z);
							i2.set(minCorner.add(i2));
							if (get(mat,x+1,y,z)==null) coordPointsCounter2 = addWindow(coordPointsStr2, coordPointsCounter2, coordIndexesStr2, i2, ind111, ind110, ind100, ind101);
							if (get(mat,x,y+1,z)==null) coordPointsCounter2 = addWindow(coordPointsStr2, coordPointsCounter2, coordIndexesStr2, i2, ind111, ind110, ind010, ind011);
							if (get(mat,x,y,z+1)==null) coordPointsCounter2 = addWindow(coordPointsStr2, coordPointsCounter2, coordIndexesStr2, i2, ind111, ind101, ind001, ind011);
							// (get(mat,x-1,y,z)==null) coordPointsCounter2 = addWindow(coordPointsStr2, coordPointsCounter2, coordIndexesStr2, i2, ind011, ind010, ind000, ind001);
							if (get(mat,x,y-1,z)==null) coordPointsCounter2 = addWindow(coordPointsStr2, coordPointsCounter2, coordIndexesStr2, i2, ind101, ind100, ind000, ind001);
							if (get(mat,x,y,z-1)==null) coordPointsCounter2 = addWindow(coordPointsStr2, coordPointsCounter2, coordIndexesStr2, i2, ind110, ind100, ind000, ind010);
						}
				
				if (coordPointsStr2.length()>0 || coordIndexesStr2.length()>0)
					writeIndexedLineSet(vrml, coordPointsStr2, coordIndexesStr2, Color.BLUE);
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

			private int addPoint(Index3D minCorner, int[][][] indexes, StringBuilder pointsStr, int counter, Index3D i) {
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

			private Point3D computePoint(Index3D i) {
				return computePoint(i.iX,i.iY,i.iZ);
			}

			private Point3D computePoint(Point3D i) {
				return computePoint(i.x,i.y,i.z);
			}

			private Point3D computePoint(double x, double y, double z) {
				return anchorPos
						.add(anchorXat.mul((x    )*CUBESIZE))
						.add(anchorYup.mul((y-0.5)*CUBESIZE))
						.add(anchorZ  .mul((z-0.5)*CUBESIZE));
			}
			
		}
		
//		private enum NeighborIndex {
//			NegX(-1,0,0), NegY(0,-1,0), NegZ(0,0,-1),
//			PosX( 1,0,0), PosY(0, 1,0), PosZ(0,0, 1);
//			
//			int incX,incY,incZ;
//			NeighborIndex(int incX, int incY, int incZ) {
//				this.incX = incX; this.incY = incY; this.incZ = incZ;
//			}
//		}
		
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
			
		}
		
		private static class Index3D {
			int iX,iY,iZ;
			
			public Index3D(int iX, int iY, int iZ) {
				this.iX = iX; this.iY = iY; this.iZ = iZ;
			}

			public Index3D(Index3D i) {
				this(i.iX,i.iY,i.iZ);
			}

			public Point3D toPoint3D() {
				return new Point3D(iX,iY,iZ);
			}

			public void set(int iX, int iY, int iZ) {
				this.iX = iX; this.iY = iY; this.iZ = iZ;
			}

			public void set(Index3D index) {
				this.iX = index.iX;
				this.iY = index.iY;
				this.iZ = index.iZ;
			}

			public Index3D add(Index3D i) {
				return new Index3D(iX+i.iX, iY+i.iY, iZ+i.iZ);
			}

			public Index3D sub(Index3D i) {
				return new Index3D(iX-i.iX, iY-i.iY, iZ-i.iZ);
			}

//			@SuppressWarnings("unused")
//			public Index3D add(NeighborIndex ni) {
//				return new Index3D(iX+ni.incX, iY+ni.incY, iZ+ni.incZ);
//			}

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
		}
		
		private static class Block implements Iterable<BuildingObject>{

			private HashMap<Index3D, BuildingObject> blocks;

			public Block(Index3D index, BuildingObject firstObj) {
				blocks = new HashMap<Index3D,BuildingObject>();
				blocks.put(index, firstObj);
			}

			public BuildingObject[][][] toMatrix(Index3D minCorner, Index3D size) {
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
				
				BuildingObject[][][] mat = new BuildingObject[size.iX][size.iY][size.iZ];
				for (int x=0; x<size.iX; ++x)
					for (int y=0; y<size.iY; ++y)
						Arrays.fill(mat[x][y],null);
				
				for (Index3D index:blocks.keySet())
					mat[index.iX-min.iX][index.iY-min.iY][index.iZ-min.iZ] = blocks.get(index);
				
				return mat;
			}

			public void set(Index3D index, BuildingObject obj) {
				blocks.put(index, obj);
			}

			@SuppressWarnings("unused")
			public boolean isFree(Index3D index) {
				return blocks.containsKey(index);
			}

			public int getNumberUsedBlocks() {
				return blocks.size();
			}

			@Override
			public Iterator<BuildingObject> iterator() {
				return blocks.values().iterator();
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
		GeneralizedID id = GameInfos.productIDs.get(objectID);
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