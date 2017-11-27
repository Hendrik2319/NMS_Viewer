package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import net.schwarzbaer.java.games.nomanssky.saveviewer.TreeView.JsonTreeNode;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.BoolValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.FloatValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.IntegerValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.JSON_Object;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.PathIsNotSolvableException;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.StringValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value.Type;

class SaveGameView extends JPanel {
	private static final long serialVersionUID = -1641171938196309864L;
	
	
	final File file;
	final JSON_Object json_data;
	private JTabbedPane tabbedPane;

	public SaveGameView(File file, JSON_Object json_data) {
		super(new BorderLayout(3, 3));
		setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		
		this.file = file;
		this.json_data = json_data;
		
		tabbedPane = new JTabbedPane();
		tabbedPane.setPreferredSize(new Dimension(600, 500));
		
		tabbedPane.addTab("General",new GeneralDataPanel());
		
		
		tabbedPane.addTab("Raw Data Tree",new RawDataTreePanel());
		
		add(tabbedPane,BorderLayout.CENTER);
	}
	
	
	@Override
	public String toString() {
		return file.getName();
	}


	private class GeneralDataPanel extends JPanel {
		private static final long serialVersionUID = -3866983525686776846L;
		private JTextArea textArea;
	
		public GeneralDataPanel() {
			super(new BorderLayout(3, 3));
			setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			
			textArea = new JTextArea();
			fillData();
			
			JScrollPane treeScrollPane = new JScrollPane(textArea);
			treeScrollPane.setPreferredSize(new Dimension(600, 500));
			
			add(treeScrollPane,BorderLayout.CENTER);
		}

		private void fillData() {
			appendValue("Current Units", Type.Integer, "PlayerStateData","Units");
			appendValue("Test value (Float)"  , Type.Float  , "PlayerStateData","Stats",7,"Stats",4,"Value","Denominator");
			appendValue("Test value (Integer)", Type.Integer, "PlayerStateData","Stats",7,"Stats",4,"Value","Denominator");
		}
		
		private void appendValue(String label, Type expectedType, Object... path) {
			Value value = null;
			try {
				value = JSON_Data.getSubNode(json_data,path);
				switch(expectedType) {
				case Bool   : if (appendBoolValue   (label,value)) return; break;
				case Float  : if (appendFloatValue  (label,value)) return; break;
				case Integer: if (appendIntegerValue(label,value)) return; break;
				case String : if (appendStringValue (label,value)) return; break;
				default: break;
				}
			} catch (PathIsNotSolvableException e) {
				appendStatement(label,"Value not found");
				System.out.println("Value \""+label+"\" not found: "+e.getMessage());
				return;
			}
			appendStatement(label,"Value has unexpected type");
			System.out.println("Value \""+label+"\" has unexpected type: "+(value==null?"<null>":value.getClass()));
		}
		
		private void appendStatement(String label, String statement) {
			if (!textArea.getText().isEmpty())
				textArea.append("\r\n");
			textArea.append(label+": "+statement);
		}

		private boolean appendBoolValue(String label, Value value) {
			if (value instanceof BoolValue) {
				BoolValue valueB = (BoolValue)value;
				appendStatement(label, ""+valueB.value);
				return true;
			}
			return false;
		}

		private boolean appendIntegerValue(String label, Value value) {
			if (value instanceof IntegerValue) {
				IntegerValue valueI = (IntegerValue)value;
				appendStatement(label, ""+valueI.value);
				return true;
			}
			return false;
		}

		private boolean appendFloatValue(String label, Value value) {
			if (value instanceof FloatValue) {
				FloatValue valueF = (FloatValue)value;
				appendStatement(label, ""+valueF.value);
				return true;
			}
			return false;
		}

		private boolean appendStringValue(String label, Value value) {
			if (value instanceof StringValue) {
				StringValue valueS = (StringValue)value;
				appendStatement(label, valueS.value);
				return true;
			}
			return false;
		}
	}


	private class RawDataTreePanel extends JPanel implements MouseListener, ActionListener {
		private static final long serialVersionUID = -50409207801775293L;
		
		private JTree tree;
		private JPopupMenu contextMenu;
		private TreePath contextMenuTarget;

		public RawDataTreePanel() {
			super(new BorderLayout(3, 3));
			setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			
			SaveViewer.log("Create tree from file \"%s\" ...",file.getPath());
			tree = new JTree(new DefaultTreeModel(new JsonTreeNode(json_data)));
			tree.addMouseListener(this);
			//tree.setCellRenderer(new DefaultTreeCellRenderer());
			JScrollPane treeScrollPane = new JScrollPane(tree);
			treeScrollPane.setPreferredSize(new Dimension(600, 500));
			SaveViewer.log_ln(" done");
			
			contextMenu = new JPopupMenu("Contextmenu");
			contextMenu.add(createMenuItem("Show Path"));
			
			add(treeScrollPane,BorderLayout.CENTER);
		}

		private JMenuItem createMenuItem(String label) {
			JMenuItem menuItem = new JMenuItem(label);
			menuItem.addActionListener(this);
			menuItem.setActionCommand("Show Path");
			return menuItem;
		}

		@Override public void mousePressed(MouseEvent e) {}
		@Override public void mouseReleased(MouseEvent e) {}
		@Override public void mouseEntered(MouseEvent e) {}
		@Override public void mouseExited(MouseEvent e) {}
		@Override public void mouseClicked(MouseEvent e) {
			if (e.getButton()==MouseEvent.BUTTON3) {
				contextMenuTarget = tree.getPathForLocation(e.getX(), e.getY());
				contextMenu.show(tree, e.getX(), e.getY());
			}
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			switch(e.getActionCommand()) {
			case "Show Path":
				System.out.println("Path: "+contextMenuTarget);
				System.out.println("    = "+toString(contextMenuTarget));
				break;
			}
		}

		private String toString(TreePath treePath) {
			StringBuilder sb = new StringBuilder();
			for (int i=0; i<treePath.getPathCount(); ++i) {
				Object pathComponent = treePath.getPathComponent(i);
				if (pathComponent instanceof JsonTreeNode) {
					JsonTreeNode node = (JsonTreeNode)pathComponent;
					if (node.parent==null)
						sb.append("[root]");
					else if (node.name==null) {
						if (node.parent.data.type!=Type.Array)
							sb.append("<nameless value inside of non array>");
						else
							sb.append("["+node.parent.getIndex(node)+"]");
					} else
						sb.append("."+node.name);
				} else
					sb.append("<unknown class of \""+pathComponent+"\">");
			}
			return sb.toString();
		}
	}

}
