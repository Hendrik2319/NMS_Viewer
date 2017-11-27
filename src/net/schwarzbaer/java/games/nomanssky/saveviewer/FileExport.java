package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

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

}