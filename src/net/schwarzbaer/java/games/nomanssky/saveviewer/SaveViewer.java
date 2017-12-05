package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
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
import net.schwarzbaer.gui.IconSource;
import net.schwarzbaer.gui.IconSource.IndexOnlyIconSource;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Stats.StatValue.KnownID;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.JSON_Object;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser;

public class SaveViewer implements ActionListener {
	
	private static final String FILE_KNOWN_STAT_ID = "NMS_Viewer.KnownStatID.txt";

	static final boolean DEBUG = true;
	
	private static StandardMainWindow mainWindow;

	static IndexOnlyIconSource portalGlyphsIS_100_90;
	static IndexOnlyIconSource portalGlyphsIS_50_45;
	static IconSource<ToolbarIcons> toolbarIS;
	
	private JFileChooser inputFileChooser;
	private JFileChooser htmlFileChooser;
	private JFileChooser jsonFileChooser;
	private ContentPane contentPane;
	private ComparePanel compareTab;
	private Vector<SaveGameView> loadedSaveGames;
	
	public static void main(String[] args) {
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		
		portalGlyphsIS_100_90 = new IconSource.IndexOnlyIconSource(100,90,4);
		portalGlyphsIS_100_90.readIconsFromResource("/PortalGlyphs.100.90.png");
		
		portalGlyphsIS_50_45 = new IconSource.IndexOnlyIconSource(50,45,4);
		portalGlyphsIS_50_45.readIconsFromResource("/PortalGlyphs.50.45.png");
		
		toolbarIS = new IconSource<ToolbarIcons>(16,16){
			@Override protected int getIconIndexInImage(ToolbarIcons key) {
				switch(key) {
				case SwitchFolder: return 1;
				case Open        : return 1;
				case Compare     : return 0;
				case SaveAs      : return 3;
				}
			 	throw new IllegalArgumentException("Unknown icon key: "+key);
			}};
		toolbarIS.readIconsFromResource("/Toolbar.png");
		
		loadKnownStatIDsFromFile();
		
//		long address = 4623450600164292L;
//		System.out.printf("0x%015X\r\n",address);
//		UniverseAddress uAddr = new UniverseAddress(address);
//		System.out.println(uAddr);
//		System.out.println(uAddr.getExtSigBoostCode());
		
		
//		writeUIDefaults("UIManagerDefaults",UIManager.getDefaults());
//		writeUIDefaults("LookAndFeelDefaults",UIManager.getLookAndFeelDefaults());
		
		new SaveViewer().createGUI();
	}

//	private static void writeUIDefaults(String title, UIDefaults defaults) {
//		System.out.println(title+".keys: [");
//		Set<Object> keySet = defaults.keySet();
//		TreeSet<Object> sortedSet = new TreeSet<Object>(Comparator.nullsLast((o1, o2) -> o1.toString().compareTo(o2.toString())));
//		sortedSet.addAll(keySet);
//		for (Object key:sortedSet)
//			System.out.println("\t"+key);
//		System.out.println("]");
//	}
	
	public SaveViewer() {
		loadedSaveGames = new Vector<SaveGameView>();
	}

	private void createGUI() {
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
			String message = String.format("Current folder changed to \"%s\"", inputFileChooser.getCurrentDirectory().getPath());
			JOptionPane.showMessageDialog(mainWindow, message, "Current folder", JOptionPane.INFORMATION_MESSAGE);
			break;
		}
		case Open:
			if (inputFileChooser.showOpenDialog(mainWindow)==JFileChooser.APPROVE_OPTION) {
				File selectedFile = inputFileChooser.getSelectedFile();
				log("Parse file \"%s\" ...",selectedFile.getPath());
				JSON_Object new_json_data = new JSON_Parser(selectedFile).parse();
				log_ln(" done");
				if (new_json_data!=null) {
					SaveGameData saveGameData = new SaveGameData(new_json_data).parse();
					SaveGameView saveGameView = new SaveGameView(selectedFile,saveGameData);
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
					FileExport.writeToHTML(contentPane.currentSelected.file.getName(),contentPane.currentSelected.data.json_data,selectedFile);
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
					FileExport.writeToJSON(contentPane.currentSelected.data.json_data,selectedFile);
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
//		if (DEBUG) System.out.println("Set window title to \""+mainWindow.getTitle()+"\"");
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
			tabbedPane.setPreferredSize(new Dimension(620, 500));
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
			toolBar.add(createButton("Switch to NMS Savegame Folder", ToolbarIcons.SwitchFolder, ActionCommand.SwitchFolder,true));
			toolBar.add(createButton("Open Savegame", ToolbarIcons.Open, ActionCommand.Open,true));
			toolBar.add(createButton("Compare Savegames", ToolbarIcons.Compare, ActionCommand.Compare,false));
			toolBar.add(createButton("Write as HTML", ToolbarIcons.SaveAs, ActionCommand.WriteHTML,false));
			toolBar.add(createButton("Write as JSON", ToolbarIcons.SaveAs, ActionCommand.WriteJSON,false));
		}

		private JButton createButton(String title, ToolbarIcons iconKey, ActionCommand actionCommand, boolean enabled) {
			JButton button = new JButton(title);
			if (iconKey!=null) button.setIcon(toolbarIS.getIcon(iconKey));
			button.setActionCommand(actionCommand.toString());
			button.addActionListener(SaveViewer.this);
			button.setEnabled(enabled);
			disabler.add(actionCommand, button);
			return button;
		}
		
	}

	enum ToolbarIcons { SwitchFolder, Open, SaveAs, Compare }

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
			tree.setModel(new DefaultTreeModel(new TreeView.CompareTreeNode(sg1.data.json_data,sg2.data.json_data)));
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

	public static void loadKnownStatIDsFromFile() {
		File file = new File(FILE_KNOWN_STAT_ID);
		if (!file.isFile()) return;
		
		System.out.println();
		String str;
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8))) {
			while ((str=in.readLine())!=null) {
				int pos = str.indexOf('=');
				if (pos<0) continue;
				
				KnownID knownID;
				try { knownID = SaveGameData.Stats.StatValue.KnownID.valueOf(str.substring(0, pos)); }
				catch (Exception e) { knownID = null; }
				if (knownID == null) continue;
				
				String fullName = str.substring(pos+1);
				if (!fullName.equals(knownID.fullName)) {
					System.out.printf("Changed StatValue.KnownID.fullName found: [old]\"%s\" -> [new]\"%s\"\r\n",knownID.fullName,fullName);
					knownID.fullName = fullName;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println();
	}

	public static void saveKnownStatIDsToFile() {
		KnownID[] knownIDs = SaveGameData.Stats.StatValue.KnownID.values();
		
		File file = new File(FILE_KNOWN_STAT_ID);
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8));) {
			for (KnownID id:knownIDs)
				out.printf("%s=%s\r\n",id.toString(),id.fullName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
