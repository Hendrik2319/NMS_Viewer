package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultTreeCellRenderer;
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
	private File currentFile2;
	private JSON_Object json_data2;

	public static void main(String[] args) {
		new SaveViewer().createGUI();
	}
	
	public SaveViewer() {
		currentFile = null;
		currentFile2 = null;
		json_data = null;
		json_data2 = null;
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
		Open, WriteHTML, WriteJSON, SwitchFolder, Compare
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
				log("Parse file \"%s\" ...",selectedFile.getPath());
				JSON_Object new_json_data = new JSON_Parser(selectedFile).parse();
				log_ln(" done");
				if (new_json_data!=null) {
					currentFile = selectedFile;
					json_data = new_json_data;
					currentFile2 = null;
					json_data2 = null;
					log("Set tree to file \"%s\" ...",currentFile.getPath());
					contentPane.tree.setModel(new DefaultTreeModel(new JsonTreeNode(json_data)));
					contentPane.tree.setCellRenderer(new DefaultTreeCellRenderer());
					log_ln(" done");
					updateWindowTitle();
				}
				if (json_data!=null) {
					contentPane.disabler.setEnable(ActionCommand.WriteHTML, json_data!=null);
					contentPane.disabler.setEnable(ActionCommand.WriteJSON, json_data!=null);
					contentPane.disabler.setEnable(ActionCommand.Compare, json_data!=null);
				}
			}
			break;
		case Compare:
			if (inputFileChooser.showOpenDialog(mainWindow)==JFileChooser.APPROVE_OPTION) {
				File selectedFile = inputFileChooser.getSelectedFile();
				log("Parse file \"%s\" ...",selectedFile.getPath());
				JSON_Object new_json_data = new JSON_Parser(selectedFile).parse();
				log_ln(" done");
				if (new_json_data!=null) {
					currentFile2 = selectedFile;
					json_data2 = new_json_data;
					log("Set tree to files \"%s\" and \"%s\" ...",currentFile.getPath(),currentFile2.getPath());
					contentPane.tree.setModel(new DefaultTreeModel(new CompareTreeNode(json_data,json_data2)));
					contentPane.tree.setCellRenderer(new CompareTreeNode.CellRenderer());
					log_ln(" done");
					updateWindowTitle();
				}
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
	
	private static void log_ln( String format, Object... values ) {
		System.out.printf(format+"\r\n",values);
	}
	
	private static void log( String format, Object... values ) {
		System.out.printf(format,values);
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
			setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
			
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
			toolBar.add(createButton("Compare to Savegame", ActionCommand.Compare,false));
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

	private static class CompareTreeNode extends AbstractTreeNode<CompareTreeNode> {
		
		enum Source { First, Second, Both }
		enum Equal{ Equal, UnEqual, Unknown }
		
		private final Value data2;
		private final Source source;
		private Equal allChildrenAreEqual;

		public CompareTreeNode(JSON_Object json_Object1, JSON_Object json_Object2) {
			this(null,null,new ObjectValue(json_Object1),new ObjectValue(json_Object2));
		}

		private CompareTreeNode(CompareTreeNode parent, String name, Value value, Source source) {
			super(parent,name,value);
			this.data2 = null;
			this.source = source;
			this.allChildrenAreEqual = (source==Source.Both)?Equal.Equal:Equal.UnEqual;
		}

		private CompareTreeNode(CompareTreeNode parent, String name, Value value1, Value value2) {
			super(parent,name,value1);
			this.data2 = value2;
			this.source = Source.Both;
			this.allChildrenAreEqual = Equal.Unknown;
			if (value1.type!=Type.Array && value1.type!=Type.Object) throw new IllegalArgumentException("Construction of double value CompareTreeNode with value type \""+value1.type+"\" is not allowed.");
			if (value2.type!=Type.Array && value2.type!=Type.Object) throw new IllegalArgumentException("Construction of double value CompareTreeNode with value type \""+value2.type+"\" is not allowed.");
		}

		private boolean allChildrenAreEqual() {
			if (children == null) createChildren();
			if (allChildrenAreEqual == Equal.Unknown) {
				for (CompareTreeNode child:children)
					if (!child.allChildrenAreEqual()) {
						allChildrenAreEqual = Equal.UnEqual;
						break;
					}
				if (allChildrenAreEqual == Equal.Unknown)
					allChildrenAreEqual = Equal.Equal;
			}
			return allChildrenAreEqual == Equal.Equal;
		}

		@Override
		void createChildren() {
			switch (data.type) {
			case Object: {
				ObjectValue object1 = (ObjectValue)data;
				ObjectValue object2 = (ObjectValue)data2;
				Vector<CompareTreeNode> childrenVec = new Vector<CompareTreeNode>();
				
				if (object2==null) {
					for (NamedValue namedvalue : object1.value)
						childrenVec.add(new CompareTreeNode(this,namedvalue.name,namedvalue.value,source));
				} else {
					HashMap<String, Value> object2Values = new HashMap<>();
					for (NamedValue namedvalue : object2.value)
						object2Values.put(namedvalue.name, namedvalue.value);
					
					for (NamedValue namedvalue : object1.value) {
						String valueName = namedvalue.name;
						
						if (!object2Values.containsKey(valueName)) {
							childrenVec.add(new CompareTreeNode(this,valueName,namedvalue.value,Source.First));
						} else {
							Value value1 = namedvalue.value;
							Value value2 = object2Values.get(valueName);
							object2Values.remove(valueName);
							if (areEqual(value1,value2)) {
								if (value1.type == Type.Object || value1.type == Type.Array)
									childrenVec.add(new CompareTreeNode(this,valueName,value1,value2));
								else
									childrenVec.add(new CompareTreeNode(this,valueName,value1,Source.Both));
							} else {
								childrenVec.add(new CompareTreeNode(this,valueName,value1,Source.First));
								childrenVec.add(new CompareTreeNode(this,valueName,value2,Source.Second));
							}
						}
					}
					for (String valueName : object2Values.keySet()) {
						Value value2 = object2Values.get(valueName);
						childrenVec.add(new CompareTreeNode(this,valueName,value2,Source.Second));
					}
				}
				
				children = childrenVec.toArray(new CompareTreeNode[0]);
			} break;
			case Array: {
				ArrayValue array1 = (ArrayValue)data;
				ArrayValue array2 = (ArrayValue)data2;
				Vector<CompareTreeNode> childrenVec = new Vector<CompareTreeNode>();
				
				if (array2==null) {
					for (Value value1 : array1.value)
						childrenVec.add(new CompareTreeNode(this,null,value1,source));
				} else {
					int size1 = array1.value.size();
					int size2 = array2.value.size();
					int size = Math.max(size1, size2);
					for (int i=0; i<size; ++i)
						if      (i>=size1) childrenVec.add(new CompareTreeNode(this,null,array2.value.get(i),Source.Second));
						else if (i>=size2) childrenVec.add(new CompareTreeNode(this,null,array1.value.get(i),Source.First ));
						else {
							Value value1 = array1.value.get(i);
							Value value2 = array2.value.get(i);
							if (areEqual(value1,value2)) { 
								if (value1.type == Type.Object || value1.type == Type.Array)
									childrenVec.add(new CompareTreeNode(this,null,value1,value2));
								else
									childrenVec.add(new CompareTreeNode(this,null,value1,Source.Both));
							} else {
								childrenVec.add(new CompareTreeNode(this,null,value1,Source.First));
								childrenVec.add(new CompareTreeNode(this,null,value2,Source.Second));
							}
						}
				}
				
				children = childrenVec.toArray(new CompareTreeNode[0]);
			} break;
			default:
				children = new CompareTreeNode[0];
				break;
			
			}
		}

		private static boolean areEqual(Value value1, Value value2) {
			if (value1.type != value2.type) return false;
			switch(value1.type) {
			case String : return ((StringValue)value1).value.compareTo(((StringValue)value2).value)==0;
//			{
//				String str1 = ((StringValue)value1).value;
//				String str2 = ((StringValue)value2).value;
//				if (str1==null) return str2==null;
//				return str1.compareTo(str2)==0;
//			}
			case Bool   : return ((BoolValue   )value1).value.booleanValue() == ((BoolValue   )value2).value.booleanValue();
			case Float  : return ((FloatValue  )value1).value.doubleValue()  == ((FloatValue  )value2).value.doubleValue();
			case Integer: return ((IntegerValue)value1).value.longValue()    == ((IntegerValue)value2).value.longValue();
			case Array  : return true;
			case Object : return true;
			}
			return false;
		}
		
		
		
//		@Override
//		public String toString() {
//			switch(source) {
//			case First : return "1| "+super.toString();
//			case Second: return "2| "+super.toString();
//			default    :
//				if (allChildrenAreEqual()) return super.toString();
//				else return "## "+super.toString();
//			}
//		}

		@Override
		protected String dataToString() {
			if (data2==null)
				return super.dataToString();
			
			switch(data.type) {
			case Array  : {
				int size  = ((ArrayValue)data ).value.size();
				int size2 = ((ArrayValue)data2).value.size();
				if (size == size2) return String.format("[%d]", size);
				return String.format("[%d] | [%d]", size, size2);
			}
			case Object : {
				int size  = ((ObjectValue)data ).value.size();
				int size2 = ((ObjectValue)data2).value.size();
				if (size == size2) return String.format("{%d}", size);
				return String.format("{%d} | {%d}", size, size2);
			}
			default:
				return super.dataToString();
			}
		}

		private static class CellRenderer extends DefaultTreeCellRenderer {
			private static final long serialVersionUID = -7036052917753626469L;
		
			private static final Color FIRST   = new Color(0xFF0000);
			private static final Color SECOND  = new Color(0x009F00);
			private static final Color UNEQUAL = new Color(0x9F009F);
			
			@Override
			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
				Component component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
				if (value instanceof CompareTreeNode) {
					CompareTreeNode node = (CompareTreeNode)value;
					switch (node.source) {
					case First : component.setForeground(FIRST); break;
					case Second: component.setForeground(SECOND); break;
					case Both:
						if (!node.allChildrenAreEqual()) component.setForeground(UNEQUAL);
						break;
					}
				}
				return component;
			}
		}
		
		
	}

	private static class JsonTreeNode extends AbstractTreeNode<JsonTreeNode> {

		public JsonTreeNode(JSON_Object json_Object) {
			this(null,null,new ObjectValue(json_Object));
		}

		private JsonTreeNode(JsonTreeNode parent, String name, Value value) {
			super(parent,name,value);
		}

		@Override
		void createChildren() {
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
	}

	private abstract static class AbstractTreeNode<TreeNodeType extends TreeNode> implements TreeNode {
		
		protected final TreeNodeType parent;
		protected final String name;
		protected final Value data;
		protected TreeNodeType[] children;
		
		public AbstractTreeNode(TreeNodeType parent, String name, Value data) {
			this.parent = parent;
			this.name = name;
			this.data = data;
			this.children = null;
		}

		abstract void createChildren();

		@Override
		public String toString() {
			if (name==null) return dataToString();
			return name +" : "+ dataToString();
		}

		protected String dataToString() {
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
			if (children == null) createChildren();
			return Arrays.binarySearch(children, node);
		}

		@Override
		public TreeNode getChildAt(int childIndex) {
			if (children == null) createChildren();
			return children[childIndex];
		}
	
		@SuppressWarnings("rawtypes")
		@Override
		public Enumeration children() {
			if (children == null) createChildren();
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
