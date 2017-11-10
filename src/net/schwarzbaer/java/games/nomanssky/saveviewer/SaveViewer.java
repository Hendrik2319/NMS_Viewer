package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import net.schwarzbaer.gui.Disabler;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser.ArrayValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser.BoolValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser.FloatValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser.IntegerValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser.JSON_Object;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser.NamedValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser.ObjectValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser.StringValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser.Value;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser.Value.Type;

public class SaveViewer implements ActionListener {

	private static StandardMainWindow mainWindow;
	private JSON_Object json_data;
	private JFileChooser inputFileChooser;
	private JFileChooser htmlFileChooser;
	private JFileChooser jsonFileChooser;
	private File currentFile;
	private ContentPane contentPane;

	public static void main(String[] args) {
		new SaveViewer().createGUI();
	}
	
	public SaveViewer() {
		json_data = null;
		currentFile = null;
	}

	private void createGUI() {
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		
		inputFileChooser = new JFileChooser("./");
		inputFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		inputFileChooser.setMultiSelectionEnabled(false);
		inputFileChooser.addChoosableFileFilter(new FileNameExtensionFilter("No Man's Sky SaveGame (*.hg)","hg"));;
		
		htmlFileChooser = new JFileChooser("./");
		htmlFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		htmlFileChooser.setMultiSelectionEnabled(false);
		htmlFileChooser.addChoosableFileFilter(new FileNameExtensionFilter("HTML-File (*.html)","html"));;
		
		jsonFileChooser = new JFileChooser("./");
		jsonFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		jsonFileChooser.setMultiSelectionEnabled(false);
		jsonFileChooser.addChoosableFileFilter(new FileNameExtensionFilter("JSON-File (*.json)","json"));;
		jsonFileChooser.addChoosableFileFilter(new FileNameExtensionFilter("JavaScript-File (*.js)","js"));;
		
		contentPane = new ContentPane();
		
		mainWindow = new StandardMainWindow();
		mainWindow.startGUI(contentPane);
		updateWindowTitle();
	}
	
	private enum ActionCommand {
		Open, WriteHTML, WriteJSON, SwitchFolder
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		ActionCommand actionCommand = ActionCommand.valueOf(e.getActionCommand());
		switch (actionCommand) {
		case SwitchFolder: {
			Properties prop = System.getProperties();
//			ArrayList<Object> keys = new ArrayList<>(prop.keySet());
//			keys.sort(null);
//			for (Object key : keys) {
//				System.out.printf("[%s] = %s\r\n",key,prop.get(key));
//			}
			String home = prop.get("user.home").toString();
			String fs = prop.get("file.separator").toString();
			String path = home+fs+"AppData"+fs+"Roaming"+fs+"HelloGames"+fs+"NMS";
			inputFileChooser.setCurrentDirectory(new File(path));
			break;
		}
		case Open:
			if (inputFileChooser.showOpenDialog(mainWindow)==JFileChooser.APPROVE_OPTION) {
				File selectedFile = inputFileChooser.getSelectedFile();
				JSON_Object new_json_data = new JSON_Parser(selectedFile).parse();
				if (new_json_data!=null) {
					currentFile = selectedFile;
					json_data = new_json_data;
					contentPane.tree.setModel(new DefaultTreeModel(new JsonTreeNode("root",json_data)));
					updateWindowTitle();
				}
				contentPane.disabler.setEnable(ActionCommand.WriteHTML, json_data!=null);
				contentPane.disabler.setEnable(ActionCommand.WriteJSON, json_data!=null);
			}
			break;
		case WriteHTML:
			if (json_data!=null && htmlFileChooser.showSaveDialog(mainWindow)==JFileChooser.APPROVE_OPTION) {
				File selectedFile = htmlFileChooser.getSelectedFile();
				FileExport.writeToHTML(currentFile.getName(),json_data,selectedFile);
			}
			break;
		case WriteJSON:
			if (json_data!=null && jsonFileChooser.showSaveDialog(mainWindow)==JFileChooser.APPROVE_OPTION) {
				File selectedFile = jsonFileChooser.getSelectedFile();
				FileExport.writeToJSON(json_data,selectedFile);
			}
			break;
		}
	}

	private void updateWindowTitle() {
		if (currentFile==null)
			mainWindow.setTitle("No Man's Sky - Viewer");
		else
			mainWindow.setTitle("No Man's Sky - Viewer - "+currentFile.getPath());
		System.out.println("Set window title to \""+mainWindow.getTitle()+"\"");
	}

	private class ContentPane extends JPanel {
		private static final long serialVersionUID = -2737846401785644788L;
		
		private JTree tree;
		private Disabler<ActionCommand> disabler;
		
		ContentPane() {
			super( new BorderLayout(3,3) );
			
			disabler = new Disabler<ActionCommand>();
			disabler.setCareFor(ActionCommand.values());
			
			JToolBar toolBar = new JToolBar("Standard");
			addButtons(toolBar);
			
			tree = new JTree((TreeModel)null);
			JScrollPane treeScrollPane = new JScrollPane(tree);
			treeScrollPane.setPreferredSize(new Dimension(600, 500));
			
			add(toolBar,BorderLayout.PAGE_START);
			add(treeScrollPane,BorderLayout.CENTER);
			
		}

		private void addButtons(JToolBar toolBar) {
			toolBar.add(createButton("Switch to NMS Savegame Folder", ActionCommand.SwitchFolder,true));
			toolBar.add(createButton("Open Savegame", ActionCommand.Open,true));
			toolBar.add(createButton("Write as HTML", ActionCommand.WriteHTML,false));
			toolBar.add(createButton("Write as JSON", ActionCommand.WriteJSON,false));
		}

		private JButton createButton(String title, ActionCommand actionCommand, boolean enabled) {
			JButton button = new JButton(title);
			button.setActionCommand(actionCommand.toString());
			button.addActionListener(SaveViewer.this);
			button.setEnabled(enabled);
			disabler.add(actionCommand, button);
			return button;
		}
		
	}

	private static class JsonTreeNode implements TreeNode {
	
		private JsonTreeNode parent;
		private String name;
		private Value data;
		private JsonTreeNode[] children;

		public JsonTreeNode(String name, JSON_Object json_Object) {
			this(null,name,new ObjectValue(json_Object));
		}
		
		public JsonTreeNode(JsonTreeNode parent, String name, Value data) {
			this.parent = parent;
			this.name = name;
			this.data = data;
			this.children = null;
		}

		@Override
		public String toString() {
			if (name==null) return dataToString();
			return name +" : "+ dataToString();
		}

		private String dataToString() {
			switch(data.type) {
			case String : return String.format("\"%s\"", ((StringValue )data).value);
			case Bool   : return String.format("%s"    , ((BoolValue   )data).value);
			case Float  : return                      ""+((FloatValue  )data).value ;
			case Integer: return String.format("%d"    , ((IntegerValue)data).value);
			case Array  : return String.format("[%d]"  , ((ArrayValue  )data).value.size());
			case Object : return String.format("{%d}"  , ((ObjectValue )data).value.size());
			}
			return data.toString();
		}

		private void createChildren() {
			switch (data.type) {
			case Object: {
				ObjectValue objectValue = (ObjectValue)data;
				children = new JsonTreeNode[objectValue.value.size()];
				int i=0;
				for (NamedValue namedvalue : objectValue.value)
					children[i++] = new JsonTreeNode(this,namedvalue.name,namedvalue.value);
			} break;
			case Array: {
				ArrayValue arrayValue = (ArrayValue)data;
				children = new JsonTreeNode[arrayValue.value.size()];
				int i=0;
				for (Value value : arrayValue.value)
					children[i++] = new JsonTreeNode(this,null,value);
			} break;
			default:
				children = new JsonTreeNode[0];
				break;
			
			}
		}

		@Override
		public TreeNode getParent() {
			return parent;
		}
	
		@Override
		public int getChildCount() {
			if (children == null) createChildren();
			return children.length;
		}
		
		@Override
		public boolean isLeaf() {
			return getChildCount()==0;
		}
		
		@Override
		public boolean getAllowsChildren() {
			return (data.type == Type.Object) || (data.type == Type.Array);
		}
	
		@Override
		public int getIndex(TreeNode node) {
			return Arrays.binarySearch(children, node);
		}

		@Override
		public TreeNode getChildAt(int childIndex) {
			return children[childIndex];
		}
	
		@SuppressWarnings("rawtypes")
		@Override
		public Enumeration children() {
			return new Enumeration<TreeNode>() {
				int index = 0;
				@Override public boolean hasMoreElements() { return children.length>index; }
				@Override public TreeNode nextElement() { return children[index++]; }
			};
		}
		
		
	
	}

	public static void test() {
//		String filepath = "c:/Users/Hendrik 2/AppData/Roaming/HelloGames/NMS/st_76561198016584395/save.hg";
		String filepath = "save.hg";
		File sourcefile = new File(filepath);
		
		JSON_Object json_Object = new JSON_Parser(sourcefile).parse();
		if (json_Object==null) return;
		
		File htmlfile = new File("./"+sourcefile.getName()+".html"); 
		FileExport.writeToHTML(sourcefile.getName(),json_Object,htmlfile);
		
		File copyfile = new File("./"+sourcefile.getName()+".copy.txt"); 
		FileExport.writeToJSON(json_Object,copyfile);
	}
}
