package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Collection;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.schwarzbaer.java.games.nomanssky.saveviewer.Gui;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.NVExtra;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.VExtra;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveViewer;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TreeView.JsonTreeNode;
import net.schwarzbaer.java.lib.gui.IconSource;
import net.schwarzbaer.java.lib.gui.StandardDialog;
import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.lib.gui.ValueListOutput;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value.Type;

public class RawDataTreePanel extends SaveGameView.SaveGameViewTabPanel implements ActionListener {
	private static final long serialVersionUID = -50409207801775293L;
	
	enum RawDataTreeIcons { Object, Array, String, Number, Bool, Null }
	static IconSource.CachedIcons<RawDataTreeIcons> rawDataTreeIS;
	
	static {
	// prepare IconSource
		IconSource<RawDataTreeIcons> source = new IconSource<>(16,16);
		source.readIconsFromResource("/images/RawTreeIcons.png");
		rawDataTreeIS = source.cacheIcons(RawDataTreeIcons.values());
	}
	
	private final Window mainWindow;
	private final DefaultTreeModel treeModel;
	private final JTextArea infoTextArea;
	private boolean hideProcessedNodes;

	private TreePath contextMenuTarget;
	private final JPopupMenu contextMenu_tree;
	private final JPopupMenu contextMenu_node;
	private final JMenuItem miHideShowProcessedNodes_node;
	private final JMenuItem miHideShowProcessedNodes_tree;
	private final JMenuItem miShowLegendOfTreeColors_tree;
	private final JMenuItem miShowLegendOfTreeColors_node;
	private boolean showDeObfuscation;
	private TreeColorsLegend treeColorsLegend;

	public RawDataTreePanel(Window mainWindow, File file, SaveGameData data, boolean showDeObfuscation) {
		super(data);
		this.mainWindow = mainWindow;
		this.showDeObfuscation = showDeObfuscation;
		hideProcessedNodes = false;
		treeColorsLegend = null;
		
		Gui.log("Create tree from file \"%s\" ...",file.getPath());
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
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(e -> {
			TreePath path = e.getPath();
			if (path==null) return;
			Object pathComp = path.getLastPathComponent();
			if (pathComp instanceof JsonTreeNode)
				showValues(path, (JsonTreeNode) pathComp);
		});
		
		JScrollPane treeScrollPane = new JScrollPane(tree);
		treeScrollPane.setPreferredSize(new Dimension(600, 500));
		Gui.log_ln(" done");
		
		infoTextArea = new JTextArea();
		infoTextArea.setEditable(false);
		JScrollPane infoTextScrollPane = new JScrollPane(infoTextArea);
		infoTextScrollPane.setPreferredSize(new Dimension(600,300));
		
		contextMenu_tree = new JPopupMenu("Contextmenu");
		contextMenu_tree.add(miHideShowProcessedNodes_tree = Gui.createMenuItem("###", this, RawDataTreeActionCommand.HideShowProcessedNodes, null));
		contextMenu_tree.add(miShowLegendOfTreeColors_tree = Gui.createMenuItem("###", this, RawDataTreeActionCommand.ShowLegendOfTreeColors, null));
		
		contextMenu_node = new JPopupMenu("Contextmenu");
		contextMenu_node.add(Gui.createMenuItem("Show Path"                   , this, RawDataTreeActionCommand.ShowPath          , null));
		contextMenu_node.add(Gui.createMenuItem("Show Path (Obfuscated Names)", this, RawDataTreeActionCommand.ShowPathObfuscated, null));
		contextMenu_node.add(Gui.createMenuItem("Copy Path"                   , this, RawDataTreeActionCommand.CopyPath          , Gui.ToolbarIcons.Copy));
		contextMenu_node.add(Gui.createMenuItem("Copy Path (Obfuscated Names)", this, RawDataTreeActionCommand.CopyPathObfuscated, Gui.ToolbarIcons.Copy));
		contextMenu_node.add(Gui.createMenuItem("Copy Value Name", this, RawDataTreeActionCommand.CopyValueName, Gui.ToolbarIcons.Copy));		
		contextMenu_node.add(Gui.createMenuItem("Copy Value", this, RawDataTreeActionCommand.CopyValue, Gui.ToolbarIcons.Copy));
		contextMenu_node.addSeparator();
		contextMenu_node.add(miHideShowProcessedNodes_node = Gui.createMenuItem("###", this, RawDataTreeActionCommand.HideShowProcessedNodes, null));
		contextMenu_node.add(miShowLegendOfTreeColors_node = Gui.createMenuItem("###", this, RawDataTreeActionCommand.ShowLegendOfTreeColors, null));
		
		updateMenuItems();
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScrollPane, infoTextScrollPane);
		splitPane.setResizeWeight(1);
		
		add(splitPane,BorderLayout.CENTER);
	}

	private void showValues(TreePath path, JsonTreeNode treeNode) {
		ValueListOutput out = new ValueListOutput();
		
		out.add(0, "Path", "%s", path==null ? "<null>" : pathToShortString(path));
		if (treeNode!=null) {
			out.add(0, "Name", "%s", getName(treeNode));
			if (treeNode.originalName!=null)
				out.add(0, "Original Name", "%s", treeNode.originalName);
			out.add(0, "Value", "%s", treeNode.data.toString());
			
			String originalStr = treeNode.originalName!=null ? treeNode.originalName : treeNode.name;
			if (originalStr!=null) {
				Collection<String> paths = data.fieldNameUsage.getPaths(originalStr);
				if (paths!=null && !paths.isEmpty()) {
					Vector<String> sorted = SimplePanels.getSorted(paths);
					out.add(0, "Usage","Original Name \"%s\" used in", originalStr);
					for (String str:sorted)
						out.add(1, null, "%s", str);
				}
			}
		}
		infoTextArea.setText(out.generateOutput());
	}

	private void updateMenuItems() {
		String text_miHideShowProcessedNodes = (hideProcessedNodes     ? "Show" : "Hide")+" Processed Nodes";
		String text_miShowLegendOfTreeColors = (treeColorsLegend==null ? "Show" : "Hide")+" Legend of Tree Colors";
		miHideShowProcessedNodes_tree.setText(text_miHideShowProcessedNodes);
		miHideShowProcessedNodes_node.setText(text_miHideShowProcessedNodes);
		miShowLegendOfTreeColors_tree.setText(text_miShowLegendOfTreeColors);
		miShowLegendOfTreeColors_node.setText(text_miShowLegendOfTreeColors);
	}

	enum RawDataTreeActionCommand { ShowPath, CopyPath, CopyValue, CopyValueName, HideShowProcessedNodes, ShowLegendOfTreeColors, CopyPathObfuscated, ShowPathObfuscated }

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
		case CopyPathObfuscated:
			SaveViewer.copyToClipBoard(pathToShortString(contextMenuTarget, true));
			break;
		case ShowPath:
			Gui.log_ln("Path: "+contextMenuTarget);
			Gui.log_ln("    = "+pathToShortString(contextMenuTarget));
			break;
		case ShowPathObfuscated:
			Gui.log_ln("Path: "+contextMenuTarget);
			Gui.log_ln("    = "+pathToShortString(contextMenuTarget,true));
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
		case ShowLegendOfTreeColors:
			if (treeColorsLegend==null)
			{
				treeColorsLegend = new TreeColorsLegend(mainWindow, "Legend of Tree Colors");
				treeColorsLegend.showDialog();
			}
			else
			{
				treeColorsLegend.closeDialog();
				treeColorsLegend = null;
			}
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

	private static String pathToShortString(TreePath treePath) {
		return pathToShortString(treePath, false);
	}

	private static String pathToShortString(TreePath treePath, boolean useOriginalNames) {
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
					sb.append("."+(useOriginalNames && node.originalName!=null ? node.originalName : node.name));
			} else
				sb.append("<unknown class of \""+pathComponent+"\">");
		}
		return sb.toString();
	}
	
	private final class CellRenderer extends DefaultTreeCellRenderer
	{
		private static final long serialVersionUID = 7697237514743853958L;

		private static final Color COLOR_HAS_OBFUSCATED_CHILDREN = new Color(0x7F00FF);
		private static final Color COLOR_WAS_NOT_DEOBFUSCATED    = new Color(0xFF00FF);
		private static final Color COLOR_WAS_PROCESSED           = new Color(0x808080);
		private static final Color COLOR_WAS_FULLY_PROCESSED     = new Color(0x00C000);
	
		@Override public Component getTreeCellRendererComponent(JTree tree, Object obj, boolean isSelected, boolean isExpanded, boolean isLeaf, int row, boolean hasFocus) {
			Component component = super.getTreeCellRendererComponent(tree, obj, isSelected, isExpanded, isLeaf, row, hasFocus);
			
			boolean wasProcessed = false;
			boolean hasUnprocessedChildren = false;
			boolean wasDeObfuscated = true;
			boolean hasObfuscatedChildren = true;
			RawDataTreeIcons icon = null;
			if (obj instanceof JsonTreeNode) {
				JsonTreeNode jsonTreeNode = (JsonTreeNode)obj;
				Value<NVExtra,VExtra> value = jsonTreeNode.data;
				wasProcessed = value.extra.wasProcessed;
				hasUnprocessedChildren = value.extra.hasUnprocessedChildren();
				wasDeObfuscated = ((JsonTreeNode)obj).wasDeObfuscated;
				hasObfuscatedChildren = value.extra.hasObfuscatedChildren();
				
				switch (value.type) {
				case Array  : icon = RawDataTreeIcons.Array ; break;
				case Bool   : icon = RawDataTreeIcons.Bool  ; break;
				case Float  : icon = RawDataTreeIcons.Number; break;
				case Integer: icon = RawDataTreeIcons.Number; break;
				case Object : icon = RawDataTreeIcons.Object; break;
				case String : icon = RawDataTreeIcons.String; setText( jsonTreeNode.toString(SaveViewer.steamIDs.getNameReplacement(value.castToStringValue().value)) ); break;
				case Null   : icon = RawDataTreeIcons.Null  ; break;
				}
			}
			
			setIcon(icon==null?null:rawDataTreeIS.getCachedIcon(icon));
			
			if (isSelected)                                      setForeground(getTextSelectionColor());
			else if (showDeObfuscation && hasObfuscatedChildren) setForeground(COLOR_HAS_OBFUSCATED_CHILDREN);
			else if (showDeObfuscation && !wasDeObfuscated)      setForeground(COLOR_WAS_NOT_DEOBFUSCATED);
			else if (wasProcessed)                               setForeground(hasUnprocessedChildren ? COLOR_WAS_PROCESSED : COLOR_WAS_FULLY_PROCESSED);
			else                                                 setForeground(getTextNonSelectionColor());
			
			return component;
		}
	}
	
	private class TreeColorsLegend extends StandardDialog
	{
		private static final long serialVersionUID = 8371032162496534208L;

		TreeColorsLegend(Window parent, String title)
		{
			super(parent, title, ModalityType.MODELESS);
			
			JPanel contentPane = new JPanel(new GridLayout(0, 1));
			contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
			addRow(contentPane, CellRenderer.COLOR_WAS_NOT_DEOBFUSCATED   , "Node is obfuscated");
			addRow(contentPane, CellRenderer.COLOR_HAS_OBFUSCATED_CHILDREN, "Node has obfuscated children");
			addRow(contentPane, CellRenderer.COLOR_WAS_PROCESSED          , "Node was processed");
			addRow(contentPane, CellRenderer.COLOR_WAS_FULLY_PROCESSED    , "Node and its children were fully processed");
			
			KeyAdapter keyAdapter = new KeyAdapter() {
				@Override public void keyPressed(KeyEvent e) {
					if (e.getKeyCode()==KeyEvent.VK_ESCAPE)
					{
						closeDialog();
						treeColorsLegend = null;
						updateMenuItems();
					}
				}
			};
			contentPane.addKeyListener(keyAdapter);
			addKeyListener(keyAdapter);
			
			addWindowListener(new WindowAdapter() {
				@Override public void windowClosing(WindowEvent e)
				{
					treeColorsLegend = null;
					updateMenuItems();
				}
			});
			
			createGUI(contentPane);
		}

		private static void addRow(JPanel contentPane, Color color, String label)
		{
			contentPane.add(
					new JLabel(
							label,
							new Tables.ColorRendererComponent.ColorIcon(30,20, color),
							SwingConstants.LEFT
					)
			);
		}
		
	}
}