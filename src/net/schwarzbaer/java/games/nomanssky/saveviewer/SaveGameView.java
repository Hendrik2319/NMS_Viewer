package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;

import net.schwarzbaer.java.lib.jsonparser.JSON_Parser.JSON_Object;

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
		tabbedPane.addTab("Raw Data Tree",new DataTreePanel());
		
		add(tabbedPane,BorderLayout.CENTER);
	}
	
	
	@Override
	public String toString() {
		return file.getName();
	}


	private class DataTreePanel extends JPanel {
		private static final long serialVersionUID = -50409207801775293L;
		
		private JTree tree;

		public DataTreePanel() {
			super(new BorderLayout(3, 3));
			setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			
			SaveViewer.log("Create tree from file \"%s\" ...",file.getPath());
			tree = new JTree(new DefaultTreeModel(new TreeView.JsonTreeNode(json_data)));
			//tree.setCellRenderer(new DefaultTreeCellRenderer());
			JScrollPane treeScrollPane = new JScrollPane(tree);
			treeScrollPane.setPreferredSize(new Dimension(600, 500));
			SaveViewer.log_ln(" done");
			
			add(treeScrollPane,BorderLayout.CENTER);
		}
	
	}

}
