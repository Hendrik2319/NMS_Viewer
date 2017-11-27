package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Properties;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import net.schwarzbaer.gui.Disabler;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.JSON_Object;

public class SaveViewer implements ActionListener {

	private static StandardMainWindow mainWindow;
	private JFileChooser inputFileChooser;
	private JFileChooser htmlFileChooser;
	private JFileChooser jsonFileChooser;
	private ContentPane contentPane;
	private ComparePanel compareTab;
	private Vector<SaveGameView> loadedSaveGames;

	public static void main(String[] args) {
		new SaveViewer().createGUI();
	}
	
	public SaveViewer() {
		loadedSaveGames = new Vector<SaveGameView>();
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
		
		compareTab = null;
		contentPane = new ContentPane();
		
		mainWindow = new StandardMainWindow();
		mainWindow.startGUI(contentPane);
		updateWindowTitle();
	}
	
	private enum ActionCommand {
		Open, WriteHTML, WriteJSON, SwitchFolder, Compare, TabSelected
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
					SaveGameView saveGameView = new SaveGameView(selectedFile,new_json_data);
					loadedSaveGames.add(saveGameView);
					contentPane.addSaveGameView(saveGameView);
					updateWindowTitle();
				}
				contentPane.disabler.setEnable(ActionCommand.Compare, loadedSaveGames.size()>1 && compareTab==null);
				contentPane.disabler.setEnable(ActionCommand.WriteHTML, contentPane.currentSelected!=null);
				contentPane.disabler.setEnable(ActionCommand.WriteJSON, contentPane.currentSelected!=null);
			}
			break;
		case Compare:
			if (compareTab==null) {
				compareTab = new ComparePanel();
				contentPane.addTab("Compare Savegames", compareTab, 0);
				contentPane.disabler.setEnable(ActionCommand.Compare, false);
			}
			break;
		case WriteHTML:
			if (contentPane.currentSelected!=null) {
				htmlFileChooser.setSelectedFile(
						new File(
								htmlFileChooser.getCurrentDirectory(),
								contentPane.currentSelected.file.getName()+".html"
						)
				);
				if (htmlFileChooser.showSaveDialog(mainWindow)==JFileChooser.APPROVE_OPTION) {
					File selectedFile = htmlFileChooser.getSelectedFile();
					FileExport.writeToHTML(contentPane.currentSelected.file.getName(),contentPane.currentSelected.json_data,selectedFile);
				}
			}
			break;
		case WriteJSON:
			if (contentPane.currentSelected!=null) {
				jsonFileChooser.setSelectedFile(
						new File(
								jsonFileChooser.getCurrentDirectory(),
								contentPane.currentSelected.file.getName()+".js"
						)
				);
				if (jsonFileChooser.showSaveDialog(mainWindow)==JFileChooser.APPROVE_OPTION) {
					File selectedFile = jsonFileChooser.getSelectedFile();
					FileExport.writeToJSON(contentPane.currentSelected.json_data,selectedFile);
				}
			}
			break;
		case TabSelected:
			updateWindowTitle();
			contentPane.disabler.setEnable(ActionCommand.WriteHTML, contentPane.currentSelected!=null);
			contentPane.disabler.setEnable(ActionCommand.WriteJSON, contentPane.currentSelected!=null);
			if (contentPane.isSelected(compareTab))
				compareTab.updatePanel();
			break;
		}
	}
	
	static void log_ln( String format, Object... values ) {
		System.out.printf(format+"\r\n",values);
	}
	
	static void log( String format, Object... values ) {
		System.out.printf(format,values);
	}

	private void updateWindowTitle() {
		if (contentPane.currentSelected == null)
			mainWindow.setTitle("No Man's Sky - Viewer");
		else
			mainWindow.setTitle("No Man's Sky - Viewer - "+contentPane.currentSelected.file.getPath());
		System.out.println("Set window title to \""+mainWindow.getTitle()+"\"");
	}

	private class ContentPane extends JPanel {
		private static final long serialVersionUID = -2737846401785644788L;
		
//		private JTree tree;
		private Disabler<ActionCommand> disabler;
		private JTabbedPane tabbedPane;
		private SaveGameView currentSelected;
		
		ContentPane() {
			super( new BorderLayout(3,3) );
			setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
			
			disabler = new Disabler<ActionCommand>();
			disabler.setCareFor(ActionCommand.values());
			
			JToolBar toolBar = new JToolBar("Standard");
			addButtons(toolBar);
			
			currentSelected = null;
			tabbedPane = new JTabbedPane();
			tabbedPane.setPreferredSize(new Dimension(600, 500));
			tabbedPane.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					Component comp = tabbedPane.getSelectedComponent();
					if (comp instanceof SaveGameView) currentSelected = (SaveGameView)comp;
					else currentSelected = null;
					SaveViewer.this.actionPerformed(new ActionEvent(tabbedPane, ActionEvent.ACTION_FIRST, ActionCommand.TabSelected.toString()));
				}
			});
			
			add(toolBar,BorderLayout.PAGE_START);
			add(tabbedPane,BorderLayout.CENTER);
			
		}

		public boolean isSelected(JPanel panel) {
			return tabbedPane.getSelectedComponent() == panel;
		}

		public void addTab(String name, JPanel panel, int index) {
			tabbedPane.insertTab(name, null, panel, null, index);
		}

		public void addSaveGameView(SaveGameView saveGameView) {
			tabbedPane.addTab(saveGameView.toString(), null, saveGameView, saveGameView.file.getPath());
		}

		private void addButtons(JToolBar toolBar) {
			toolBar.add(createButton("Switch to NMS Savegame Folder", ActionCommand.SwitchFolder,true));
			toolBar.add(createButton("Open Savegame", ActionCommand.Open,true));
			toolBar.add(createButton("Compare Savegames", ActionCommand.Compare,false));
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

	private class ComparePanel extends JPanel {
		private static final long serialVersionUID = -876150147630145750L;
		
		private JComboBox<SaveGameView> selector1;
		private JComboBox<SaveGameView> selector2;
		private JTree tree;
		
		public ComparePanel() {
			super( new BorderLayout(3,3) );
			setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
			
			tree = new JTree((TreeModel)null);
			tree.setCellRenderer(new TreeView.CompareTreeNode.CellRenderer());
			JScrollPane treeScrollPane = new JScrollPane(tree);
			treeScrollPane.setPreferredSize(new Dimension(600, 500));
			
			selector1 = new JComboBox<SaveGameView>(loadedSaveGames);
			selector2 = new JComboBox<SaveGameView>(loadedSaveGames);
			JPanel selectorPanel = new JPanel(new GridLayout(1,2,3,3));
			selectorPanel.add(selector1);
			selectorPanel.add(selector2);
			
			selector1.addActionListener(e -> updateTree());
			selector2.addActionListener(e -> updateTree());
			
			add(selectorPanel, BorderLayout.NORTH);
			add(treeScrollPane, BorderLayout.CENTER);
			
			updateTree();
		}

		public void updatePanel() {
			selector1.revalidate();
			selector1.updateUI();
			selector2.revalidate();
			selector2.updateUI();
			updateTree();
		}

		private void updateTree() {
			int index1 = selector1.getSelectedIndex();
			int index2 = selector2.getSelectedIndex();
			if (index1<0 && index2<0) {
				tree.setModel((TreeModel)null);
				return;
			}
			if (index1>=loadedSaveGames.size() || index2>=loadedSaveGames.size()) {
				tree.setModel((TreeModel)null);
				return;
			}
			
			SaveGameView sg1 = loadedSaveGames.get(index1);
			SaveGameView sg2 = loadedSaveGames.get(index2);
			log("Set tree to files \"%s\" and \"%s\" ...",sg1.file.getPath(),sg2.file.getPath());
			tree.setModel(new DefaultTreeModel(new TreeView.CompareTreeNode(sg1.json_data,sg2.json_data)));
			tree.setCellRenderer(new TreeView.CompareTreeNode.CellRenderer());
			log_ln(" done");
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
