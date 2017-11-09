package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import net.schwarzbaer.java.lib.jsonparser.JSON_Object;
import net.schwarzbaer.java.lib.jsonparser.JSON_Object.FloatValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Object.IntegerValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Object.ObjectValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Object.StringValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Object.Value;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser;

public class SaveViewer {

	public static void main(String[] args) {
//		String filepath = "c:/Users/Hendrik 2/AppData/Roaming/HelloGames/NMS/st_76561198016584395/save.hg";
		String filepath = "save.hg";
		File sourcefile = new File(filepath);
		
		JSON_Parser json_Parser;
		try {
			json_Parser = new JSON_Parser(sourcefile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		JSON_Object json_Object = json_Parser.parse();
		
		File htmlfile = new File("./"+sourcefile.getName()+".html"); 
		writeToHTML(sourcefile.getName(),json_Object,htmlfile);
	}

	private static void writeToHTML(String title, JSON_Object json_Object, File htmlfile) {
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
		
		writeToHTML_obj("obj",json_Object,out);
		
		out.println("</body>");
		out.println("</html>");
		
		out.close();
	}

	private static void writeToHTML_obj(String ID, JSON_Object json_Object, PrintWriter out) {
		// TODO Auto-generated method stub
		if (json_Object==null)
			out.print("null");
		
		out.println("{ <span class=\"button\" onclick=\"toggle_collapse(this,'"+ID+"');\">[-]</span>");
		out.println("<div id="+ID+" class=\"valuelist\">");
		
		for (int i=0; i<json_Object.values.size(); ++i) {
			Value value = json_Object.values.get(i);
			out.printf("<span class=\"name\">\"%s\"</span> : ",value.name);
			if ( value instanceof StringValue  ) out.printf(Locale.ENGLISH,"<span class=\"string\">"+"\"%s\""+"</span>", ((StringValue )value).value);
			if ( value instanceof FloatValue   ) out.printf(Locale.ENGLISH,"<span class=\"number\">"+"%s"    +"</span>", ((FloatValue  )value).value);
			if ( value instanceof IntegerValue ) out.printf(Locale.ENGLISH,"<span class=\"number\">"+"%d"    +"</span>", ((IntegerValue)value).value);
			if ( value instanceof ObjectValue  ) writeToHTML_obj(ID+"_"+i, ((ObjectValue)value).value, out);
			out.println(",<br/>");
			
		}
		
		out.println("</div>");
		out.println("}");
	}
}
