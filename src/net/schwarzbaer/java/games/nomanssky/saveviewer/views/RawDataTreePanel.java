package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import net.schwarzbaer.gui.IconSource;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveViewer;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TreeView.JsonTreeNode;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value.Type;

public class RawDataTreePanel extends SaveGameView.SaveGameViewTabPanel implements ActionListener {
	private static final long serialVersionUID = -50409207801775293L;
	
	enum RawDataTreeIcons { Object, Array, String, Number, Bool }
	static IconSource<RawDataTreeIcons> rawDataTreeIS = null;
	public static void prepareIconSource() {
		rawDataTreeIS = new IconSource<RawDataTreeIcons>(16,16){
			@Override protected int getIconIndexInImage(RawDataTreeIcons key) {
				if (key!=null) return key.ordinal();
			 	throw new IllegalArgumentException("Unknown icon key: "+key);
			}
		};
		rawDataTreeIS.readIconsFromResource("/images/RawTreeIcons.png");
		rawDataTreeIS.cacheIcons(RawDataTreeIcons.values());
	}
	private DefaultTreeModel treeModel;
	private boolean hideProcessedNodes;

	private TreePath contextMenuTarget;
	private JPopupMenu contextMenu_tree;
	private JPopupMenu contextMenu_node;
	private JMenuItem miHideShowProcessedNodes_node;
	private JMenuItem miHideShowProcessedNodes_tree;

	public RawDataTreePanel(File file, SaveGameData data) {
		super(data);
		hideProcessedNodes = false;
		
		SaveViewer.log("Create tree from file \"%s\" ...",file.getPath());
		JTree tree = new JTree(treeModel = new DefaultTreeModel(new JsonTreeNode(this.data.json_data,hideProcessedNodes)));
		tree.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				if (e.getButton()==MouseEvent.BUTTON3) {
					contextMenuTarget = tree.getPathForLocation(e.getX(), e.getY());
					if (contextMenuTarget!=null) contextMenu_node.show(tree, e.getX(), e.getY());
					else                         contextMenu_tree.show(tree, e.getX(), e.getY());
				}
			}
		});
		tree.setCellRenderer(new CellRenderer());
		JScrollPane treeScrollPane = new JScrollPane(tree);
		treeScrollPane.setPreferredSize(new Dimension(600, 500));
		SaveViewer.log_ln(" done");
		
		contextMenu_tree = new JPopupMenu("Contextmenu");
		contextMenu_tree.add(miHideShowProcessedNodes_tree = createMenuItem("Hide Processed Nodes",RawDataTreeActionCommand.HideShowProcessedNodes));
		
		contextMenu_node = new JPopupMenu("Contextmenu");
		contextMenu_node.add(createMenuItem("Show Path",RawDataTreeActionCommand.ShowPath));
		contextMenu_node.add(createMenuItem("Copy Path",RawDataTreeActionCommand.CopyPath));
		contextMenu_node.add(createMenuItem("Copy Value Name",RawDataTreeActionCommand.CopyValueName));		
		contextMenu_node.add(createMenuItem("Copy Value",RawDataTreeActionCommand.CopyValue));
		contextMenu_node.addSeparator();
		contextMenu_node.add(miHideShowProcessedNodes_node = createMenuItem("Hide Processed Nodes",RawDataTreeActionCommand.HideShowProcessedNodes));
		
		updateMenuItems();
		
		add(treeScrollPane,BorderLayout.CENTER);
	}

	private void updateMenuItems() {
		if (hideProcessedNodes) {
			miHideShowProcessedNodes_tree.setText("Show Processed Nodes");
			miHideShowProcessedNodes_node.setText("Show Processed Nodes");
		} else {
			miHideShowProcessedNodes_tree.setText("Hide Processed Nodes");
			miHideShowProcessedNodes_node.setText("Hide Processed Nodes");
		}
	}

	private JMenuItem createMenuItem(String label, RawDataTreePanel.RawDataTreeActionCommand actionCommand) {
		JMenuItem menuItem = new JMenuItem(label);
		menuItem.addActionListener(this);
		menuItem.setActionCommand(actionCommand.toString());
		return menuItem;
	}

	enum RawDataTreeActionCommand { ShowPath, CopyPath, CopyValue, CopyValueName, HideShowProcessedNodes }

	@Override
	public void actionPerformed(ActionEvent e) {
		RawDataTreePanel.RawDataTreeActionCommand actionCommand = RawDataTreeActionCommand.valueOf(e.getActionCommand());
		Object pathComp = null;
		if (contextMenuTarget!=null)
			pathComp = contextMenuTarget.getLastPathComponent();
		switch(actionCommand) {
		case CopyPath:
			SaveViewer.copyToClipBoard(pathToShortString(contextMenuTarget));
			break;
		case ShowPath:
			System.out.println("Path: "+contextMenuTarget);
			System.out.println("    = "+pathToShortString(contextMenuTarget));
			break;
		case CopyValueName:
			if (pathComp instanceof JsonTreeNode)
				SaveViewer.copyToClipBoard(getName((JsonTreeNode)pathComp));
			break;
		case CopyValue:
			if (pathComp instanceof JsonTreeNode)
				SaveViewer.copyToClipBoard(((JsonTreeNode)pathComp).data.toString());
			break;
		case HideShowProcessedNodes:
			hideProcessedNodes = !hideProcessedNodes;
			treeModel.setRoot(new JsonTreeNode(this.data.json_data,hideProcessedNodes));
			updateMenuItems();
			break;
		}
	}

	private String getName(JsonTreeNode node) {
		if (node.parent==null) return "[root]";
		if (node.name==null) {
			if (node.parent.data.type!=Type.Array)
				return "<nameless value inside of non array>";
			return "["+node.parent.getIndex(node)+"]";
		}
		return node.name;
	}

	private String pathToShortString(TreePath treePath) {
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

	private final class CellRenderer extends DefaultTreeCellRenderer {
		private static final long serialVersionUID = 7697237514743853958L;
	
		@Override public Component getTreeCellRendererComponent(JTree tree, Object obj, boolean isSelected, boolean isExpanded, boolean isLeaf, int row, boolean hasFocus) {
			Component component = super.getTreeCellRendererComponent(tree, obj, isSelected, isExpanded, isLeaf, row, hasFocus);
			
			JLabel label = null;
			if (component instanceof JLabel) label = (JLabel)component;
			
			boolean wasProcessed = false;
			RawDataTreeIcons icon = null;
			if (obj instanceof JsonTreeNode) {
				Value value = ((JsonTreeNode)obj).data;
				wasProcessed = value.wasProcessed;
				switch (value.type) {
				case Array  : icon = RawDataTreeIcons.Array ; break;
				case Bool   : icon = RawDataTreeIcons.Bool  ; break;
				case Float  : icon = RawDataTreeIcons.Number; break;
				case Integer: icon = RawDataTreeIcons.Number; break;
				case Object : icon = RawDataTreeIcons.Object; break;
				case String : icon = RawDataTreeIcons.String; break;
				}
			}
			
			if (label != null)
				label.setIcon(icon==null?null:rawDataTreeIS.getCachedIcon(icon));
			
			if (isSelected)
				component.setForeground(getTextSelectionColor());
			else if (wasProcessed)
				component.setForeground(Color.GRAY);
			else
				component.setForeground(getTextNonSelectionColor());
			
			return component;
		}
	}
}