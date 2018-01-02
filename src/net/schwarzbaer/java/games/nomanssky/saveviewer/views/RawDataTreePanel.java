package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveViewer;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.SaveGameView.SaveGameViewTabPanel;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TreeView.JsonTreeNode;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value.Type;

class RawDataTreePanel extends SaveGameViewTabPanel implements MouseListener, ActionListener {
	private static final long serialVersionUID = -50409207801775293L;
	
	enum RawDataTreeActionCommand { ShowPath, CopyPath, CopyValue, CopyValueName }

	private JTree tree;
	private JPopupMenu contextMenu;
	private TreePath contextMenuTarget;

	public RawDataTreePanel(File file, SaveGameData data) {
		super(data);
		
		SaveViewer.log("Create tree from file \"%s\" ...",file.getPath());
		tree = new JTree(new DefaultTreeModel(new JsonTreeNode(data.json_data)));
		tree.addMouseListener(this);
		//tree.setCellRenderer(new DefaultTreeCellRenderer());
		JScrollPane treeScrollPane = new JScrollPane(tree);
		treeScrollPane.setPreferredSize(new Dimension(600, 500));
		SaveViewer.log_ln(" done");
		
		contextMenu = new JPopupMenu("Contextmenu");
		contextMenu.add(createMenuItem("Copy Path",RawDataTreeActionCommand.CopyPath));
		contextMenu.add(createMenuItem("Copy Value Name",RawDataTreeActionCommand.CopyValueName));		
		contextMenu.add(createMenuItem("Copy Value",RawDataTreeActionCommand.CopyValue));
		contextMenu.add(createMenuItem("Show Path",RawDataTreeActionCommand.ShowPath));
		
		add(treeScrollPane,BorderLayout.CENTER);
	}

	private JMenuItem createMenuItem(String label, RawDataTreePanel.RawDataTreeActionCommand actionCommand) {
		JMenuItem menuItem = new JMenuItem(label);
		menuItem.addActionListener(this);
		menuItem.setActionCommand(actionCommand.toString());
		return menuItem;
	}

	@Override public void mousePressed(MouseEvent e) {}
	@Override public void mouseReleased(MouseEvent e) {}
	@Override public void mouseEntered(MouseEvent e) {}
	@Override public void mouseExited(MouseEvent e) {}
	@Override public void mouseClicked(MouseEvent e) {
		if (e.getButton()==MouseEvent.BUTTON3) {
			contextMenuTarget = tree.getPathForLocation(e.getX(), e.getY());
			if (contextMenuTarget!=null)
				contextMenu.show(tree, e.getX(), e.getY());
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		RawDataTreePanel.RawDataTreeActionCommand actionCommand = RawDataTreeActionCommand.valueOf(e.getActionCommand());
		Object pathComp = contextMenuTarget.getLastPathComponent();
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
}