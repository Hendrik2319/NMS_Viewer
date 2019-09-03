package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.activation.DataHandler;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import net.schwarzbaer.gui.Disabler;
import net.schwarzbaer.gui.IconSource;
import net.schwarzbaer.gui.ProgressDialog;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.gui.TristateCheckBox;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Images.ShowImagesDialog;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.SaveGameView;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.SimplePanels;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TreeView;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.JSON_Object;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser;

public class SaveViewer implements ActionListener {

	private static final String IMAGES_TOOLBAR_PNG = "/images/Toolbar.png";
	private static final String IMAGES_TAB_HEADER_PNG = "/images/TabHeader.png";
	public static final boolean DEBUG = true;
	private StandardMainWindow mainWindow;

	enum TabHeaderIcons { Close, Close_Inactive, Reload, Reload_Inactive }
	static IconSource<TabHeaderIcons> tabheaderIS;
	public static IconSource<ToolbarIcons> toolbarIS;
	public static Images images;
	public static Config config;
	public static DeObfuscator deObfuscator;
	
	private JFileChooser inputFileChooser;
	private JFileChooser htmlFileChooser;
	private JFileChooser jsonFileChooser;
	private ContentPane contentPane;
	private ComparePanel compareTab;
	private Vector<SaveGameView> loadedSaveGames;
	
	public static void main(String[] args) {
		
		config = Config.readFromFile();
		deObfuscator = DeObfuscator.readFromFile();
		
		GameInfos.loadKnownStatIDsFromFile();
		GameInfos.loadAllIDsFromFiles();
		GameInfos.loadUniverseObjectDataFromFile();
		SaveGameData.Frigate.EditableModification.loadKnownEditableModsFromFile();
		
		if (args.length>0) {
			processCommands(args);
			return;
		}
		
//		selectLookAndFeel();
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		
		images = new Images();
		images.init();
		
		tabheaderIS = new IconSource<TabHeaderIcons>(10,10);
		tabheaderIS.readIconsFromResource(IMAGES_TAB_HEADER_PNG);
		
		toolbarIS = new IconSource<ToolbarIcons>(16,16){
			@Override protected int getIconIndexInImage(ToolbarIcons key) {
				switch(key) {
				case SwitchFolder: return 1;
				case Open        : return 1;
				case Compare     : return 0;
				case SaveAs      : return 3;
				case Close       : return 5;
				case Reload      : return 4;
				case ComputePortalGlyphs: return 6;
				case Cut   : return 7;
				case Copy  : return 8;
				case Paste : return 9;
				case Delete: return 10;
				}
			 	throw new IllegalArgumentException("Unknown icon key: "+key);
			}};
		toolbarIS.readIconsFromResource(IMAGES_TOOLBAR_PNG);
		
//		GameInfos.createFilesWithObsoleteIDs();

//		HashMap<Dimension,Integer> map = new HashMap<>();
//		Integer prev;
//		prev = map.put(new Dimension(12,15), 1); System.out.println("map.put(new Dimension(12,15), 1) -> "+prev);
//		prev = map.put(new Dimension(12,15), 2); System.out.println("map.put(new Dimension(12,15), 2) -> "+prev);
//		prev = map.put(new Dimension(12,15), 3); System.out.println("map.put(new Dimension(12,15), 3) -> "+prev);
		
//		String str;
//		long value;
//		str = "C989299D5EB253EB";
//		value = Long.parseUnsignedLong(str,16);
//		System.out.printf("Hex:%X\r\nStr:%s\r\n",value,str);
//		value = Long.parseLong("C989299D5EB253EB",16);
//		System.out.printf("Hex:%X\r\nStr:%s\r\n",value,str);

		
//		long address = 4623450600164292L;
//		System.out.printf("0x%015X\r\n",address);
//		UniverseAddress uAddr = new UniverseAddress(address);
//		System.out.println(uAddr);
//		System.out.println(uAddr.getExtSigBoostCode());
		
		
//		writeUIDefaults("UIManagerDefaults",UIManager.getDefaults());
//		writeUIDefaults("LookAndFeelDefaults",UIManager.getLookAndFeelDefaults());
//		System.out.println("UIManager(Tree.font):          "+UIManager.getFont("Tree.font"));
//		System.out.println("LookAndFeelDefaults(Tree.font):"+UIManager.getLookAndFeelDefaults().getFont("Tree.font"));
		
		new SaveViewer().createGUI();
	}

	@SuppressWarnings("unused")
	private static void selectLookAndFeel() {
		System.out.println("Installed Look&Feels:");
		String formatStr = "    [%d]  %-15s  %s%n";
		System.out.printf(formatStr, 0, "use default Look&Feel", "");
		System.out.printf(formatStr, 1, "<System>", UIManager.getSystemLookAndFeelClassName());
		System.out.printf(formatStr, 2, "<CrossPlatform>",UIManager.getCrossPlatformLookAndFeelClassName());
		LookAndFeelInfo[] lookAndFeelInfos = UIManager.getInstalledLookAndFeels();
		for (int i=0; i<lookAndFeelInfos.length; i++) {
			UIManager.LookAndFeelInfo lf = lookAndFeelInfos[i];
			System.out.printf(formatStr, i+3, lf.getName(), lf.getClassName());
		}
		
		String text = "Select Look&Feel";
		int value = readInt(text, 0, lookAndFeelInfos.length+2);
		System.out.printf("Value: %d%n", value);
		
		if (value>0)
			try {
				if      (value==1) UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				else if (value==2) UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
				else UIManager.setLookAndFeel(lookAndFeelInfos[value-3].getClassName());
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
	}

	private static int readInt(String text, int min, int max) {
		while (true) {
			System.out.print(text+": ");
			String result = "";
			
			int ch;
			try {
				while ( (ch = System.in.read()) != 0xA ) {
					if (ch!=0xD)
						result += (char)ch;
				}
			} catch (IOException e) {}
			
			try {
				int val = Integer.parseInt(result);
				if (min<=val && val<=max)
					return val;
				else
					System.out.printf("Allowed Range: %d..%d%n", min,max);
			}
			catch (NumberFormatException e) {}
		}
	}

	private static void processCommands(String[] args) {
		int loadSavegame = -1;
		int writeBase2VRML = -1;
		for (int i=0; i<args.length; i++) {
			switch (args[i]) {
			case "-loadGame":
				if (i+1<args.length) {
					try { loadSavegame = Integer.parseInt(args[i+1]); }
					catch (NumberFormatException e) { loadSavegame = -1; }
				}
				break;
				
			case "-base2vrml":
				if (i+1<args.length) {
					try { writeBase2VRML = Integer.parseInt(args[i+1]); }
					catch (NumberFormatException e) { writeBase2VRML = -1; }
				}
				break;
			}
		}
		
		if (loadSavegame<0)
			return;
		
		ActionCommand actionCommand = null;
		for (ActionCommand ac:ActionCommand.values()) {
			if (ac.index==loadSavegame-1)
				actionCommand = ac;
		}
		if (actionCommand==null)
			return;
		
		SaveViewer saveViewer = new SaveViewer();
		File saveGameFile = new File(saveViewer.getSavegameFolder()+actionCommand.filename);
		if (!saveGameFile.isFile())
			return;
		
		SaveGameData data = saveViewer.openSaveGame(saveGameFile, actionCommand.index, null);
		if (data==null)
			return;
		
		if (writeBase2VRML>=0)
			savePlayerBases2VRML(data,writeBase2VRML);
	}

	private static void savePlayerBases2VRML(SaveGameData data, int baseIndex) {
		if (data.persistentPlayerBases==null)
			return;
		if (baseIndex-1<0 || baseIndex-1>=data.persistentPlayerBases.size())
			return;
		
		SaveGameData.PersistentPlayerBase playerbase =
				data.persistentPlayerBases.get(baseIndex-1);
		SimplePanels.PersistentPlayerBasesPanel.PlayerBasePanel.Type type =
				SimplePanels.PersistentPlayerBasesPanel.PlayerBasePanel.Type.Models;
		
		String suggestFileName = SimplePanels.PersistentPlayerBasesPanel.PlayerBasePanel.suggestFileName(type,data,playerbase);
		FileExport.writePosToVRML_models(suggestFileName,null,playerbase,null,"Base "+baseIndex,true);
	}

	@SuppressWarnings("unused")
	private static void writeUIDefaults(String title, UIDefaults defaults) {
		SaveViewer.log_ln(title+".keys: [");
		Set<Object> keySet = defaults.keySet();
		TreeSet<Object> sortedSet = new TreeSet<Object>(Comparator.nullsLast((o1, o2) -> o1.toString().compareTo(o2.toString())));
		sortedSet.addAll(keySet);
		for (Object key:sortedSet)
			SaveViewer.log_ln("\t"+key);
		SaveViewer.log_ln("]");
	}
	
	public SaveViewer() {
		loadedSaveGames = new Vector<SaveGameView>();
		mainWindow = null;
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
		
		ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
		//FrequentlyTask frequentlyUpdater = new FrequentlyTask(5000,true,this::checkSavegameExistence);
		//mainWindow = new StandardMainWindow("",e->frequentlyUpdater.stop(),StandardMainWindow.DefaultCloseOperation.EXIT_ON_CLOSE);
		mainWindow = new StandardMainWindow("",e->executor.shutdown(),StandardMainWindow.DefaultCloseOperation.EXIT_ON_CLOSE);
		
		compareTab = null;
		contentPane = new ContentPane();
		
		mainWindow.startGUI(contentPane);
		updateWindowTitle();
		
		executor.scheduleAtFixedRate(this::checkSavegameExistence, 0, 5, TimeUnit.SECONDS);
		//frequentlyUpdater.start();
	}

	private enum ActionCommand {
		Open, Reload, Close, WriteHTML, WriteJSON, SwitchToGameFolder, SwitchToBackupFolder, Compare, TabSelected, ComputeCoordinates,
		  save_hg( 0,  "save.hg","save.hg"),
		 save2_hg( 1, "save2.hg","..2"    ),
		 save3_hg( 2, "save3.hg","..3"    ),
		 save4_hg( 3, "save4.hg","..4"    ),
		 save5_hg( 4, "save5.hg","..5"    ),
		 save6_hg( 5, "save6.hg","..6"    ),
		 save7_hg( 6, "save7.hg","..7"    ),
		 save8_hg( 7, "save8.hg","..8"    ),
		 save9_hg( 8, "save9.hg","..9"    ),
		save10_hg( 9,"save10.hg","..10"   ),
		RefreshExtraImages, ShowExtraImages, SelectCoordinates;
		
		public static final ActionCommand[] save_commands = {save_hg,save2_hg,save3_hg,save4_hg,save5_hg,save6_hg,save7_hg,save8_hg,save9_hg,save10_hg};
		private String filename;
		private String label;
		private int index;
		
		private ActionCommand() { this(-1,null,null); }
		private ActionCommand(int index, String filename, String label) {
			this.index = index;
			this.filename = filename;
			this.label = label;
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		ActionCommand actionCommand = ActionCommand.valueOf(e.getActionCommand());
		switch (actionCommand) {
		case save_hg: case save2_hg: case save3_hg: case save4_hg: case save5_hg:
		case save6_hg: case save7_hg: case save8_hg: case save9_hg: case save10_hg:
			openSaveGame(new File(getSavegameFolder()+actionCommand.filename),actionCommand.index);
			break;
			
		case SwitchToGameFolder: {
			inputFileChooser.setCurrentDirectory(getGameFolder());
			String message = String.format("Current folder changed to \"%s\"", inputFileChooser.getCurrentDirectory().getPath());
			JOptionPane.showMessageDialog(mainWindow, message, "Current folder", JOptionPane.INFORMATION_MESSAGE);
			break;
		}
		case SwitchToBackupFolder: {
			inputFileChooser.setCurrentDirectory(new File(config.getBackupFolder(mainWindow)));
			String message = String.format("Current folder changed to \"%s\"", inputFileChooser.getCurrentDirectory().getPath());
			JOptionPane.showMessageDialog(mainWindow, message, "Current folder", JOptionPane.INFORMATION_MESSAGE);
			break;
		}
		case Open:
			if (inputFileChooser.showOpenDialog(mainWindow)==JFileChooser.APPROVE_OPTION)
				openSaveGame(inputFileChooser.getSelectedFile(),-1);
			break;
			
		case Close:
			if (contentPane.selectedSaveGameView!=null)
				closeSaveGameView(contentPane.selectedSaveGameView);
			break;
			
		case Reload:
			if (contentPane.selectedSaveGameView!=null)
				reloadSaveGameView(contentPane.selectedSaveGameView);
			break;
			
		case Compare:
			if (compareTab==null) {
				compareTab = new ComparePanel();
				contentPane.addTab("Compare Savegames", compareTab, 0);
				contentPane.disabler.setEnable(ActionCommand.Compare, false);
			}
			break;
			
		case WriteHTML:
			if (contentPane.selectedSaveGameView!=null) {
				htmlFileChooser.setSelectedFile(
						new File(
								htmlFileChooser.getCurrentDirectory(),
								contentPane.selectedSaveGameView.file.getName()+".html"
						)
				);
				if (htmlFileChooser.showSaveDialog(mainWindow)==JFileChooser.APPROVE_OPTION) {
					File selectedFile = htmlFileChooser.getSelectedFile();
					FileExport.writeToHTML(contentPane.selectedSaveGameView.file.getName(),contentPane.selectedSaveGameView.data.json_data,selectedFile);
				}
			}
			break;
			
		case WriteJSON:
			if (contentPane.selectedSaveGameView!=null) {
				jsonFileChooser.setSelectedFile(
						new File(
								jsonFileChooser.getCurrentDirectory(),
								contentPane.selectedSaveGameView.file.getName()+".js"
						)
				);
				if (jsonFileChooser.showSaveDialog(mainWindow)==JFileChooser.APPROVE_OPTION) {
					File selectedFile = jsonFileChooser.getSelectedFile();
					FileExport.writeToJSON(contentPane.selectedSaveGameView.data.json_data,selectedFile);
				}
			}
			break;
			
		case TabSelected:
			updateWindowTitle();
			contentPane.disabler.setEnable(ActionCommand.Close    , contentPane.selectedSaveGameView!=null);
			contentPane.disabler.setEnable(ActionCommand.Reload   , contentPane.selectedSaveGameView!=null);
			contentPane.disabler.setEnable(ActionCommand.WriteHTML, contentPane.selectedSaveGameView!=null);
			contentPane.disabler.setEnable(ActionCommand.WriteJSON, contentPane.selectedSaveGameView!=null);
			if (contentPane.isSelected(compareTab))
				compareTab.updatePanel();
			break;
			
		case ComputeCoordinates:
			new Gui.CoordinatesDialog(mainWindow).showDialog();
			break;
			
		case SelectCoordinates: {
			Gui.CoordinatesDialog dlg = new Gui.CoordinatesDialog(mainWindow,true,"Select Coordinates");
			dlg.showDialog();
			if (dlg.hasResult()) {
				dlg.getResult();
			}
		} break;
			
		case RefreshExtraImages:
			runWithProgressDialog(mainWindow,"Refresh Extra Images", pd->{
				images.reloadImageList(pd);
			});
			break;
			
		case ShowExtraImages:
			new ShowImagesDialog(mainWindow,"Extra Images").showDialog();
			break;
		}
	}

	private boolean isSavegameFolderKnown() {
		return config.isSavegameSubFolderKnown();
	}

	private String getSavegameFolder() {
		Properties prop = System.getProperties();
		String fs = prop.get("file.separator").toString();
		return getGameFolderStr()+fs+config.getSavegameSubFolder(mainWindow)+fs;
	}

	private static File getGameFolder() {
		return new File(getGameFolderStr());
	}

	private static String getGameFolderStr() {
		Properties prop = System.getProperties();
//			ArrayList<Object> keys = new ArrayList<>(prop.keySet());
//			keys.sort(null);
//			for (Object key : keys) {
//				System.out.printf("[%s] = %s\r\n",key,prop.get(key));
//			}
		String home = prop.get("user.home").toString();
		String fs = prop.get("file.separator").toString();
		return home+fs+"AppData"+fs+"Roaming"+fs+"HelloGames"+fs+"NMS";
	}
	
	
	
	private void checkSavegameExistence() {
		if (isSavegameFolderKnown())
			for (ActionCommand ac:ActionCommand.save_commands)
				contentPane.disabler.setEnable(ac, new File(getSavegameFolder()+ac.filename).isFile());
	}

	private void openSaveGame(File saveGameFile, int saveGameIndex) {
		runWithProgressDialog(mainWindow,"Open SaveGame", pd->{
			openSaveGame(saveGameFile,saveGameIndex,pd);
		});
	}

	private SaveGameData openSaveGame(File saveGameFile, int saveGameIndex, ProgressDialog pd) {
		if (pd!=null) { pd.setTaskTitle("Parse file"); pd.setValue(0, 4); }
		log("Parse file \"%s\" ...",saveGameFile.getPath());
		JSON_Object new_json_data = new JSON_Parser(saveGameFile).parse();
		log_ln(" done");
		
		HashMap<String, Vector<String>> deObfuscatorUsage = null;
		boolean isNEXT = false;
		if (!SaveGameData.hasValue(new_json_data, "Version")) {
			if (pd!=null) { pd.setTaskTitle("DeObfuscate value names"); pd.setValue(1); }
			new_json_data = deObfuscator.deObfuscate(new_json_data);
			deObfuscatorUsage = deObfuscator.getUsage();
			isNEXT = true;
		}
		
		SaveGameData saveGameData = null;
		if (new_json_data==null) {
			if (mainWindow!=null)
				JOptionPane.showMessageDialog(mainWindow, "Can't parse selected file. It is not a valid JSON formated No Man's Sky savegame.", "Parse Error", JOptionPane.ERROR_MESSAGE);
			
		} else {
			saveGameData = new SaveGameData(new_json_data,saveGameFile.getName(),saveGameIndex);
			saveGameData.setDeObfuscatorUsage(deObfuscatorUsage);
			
			if (pd!=null) { pd.setTaskTitle("Parse JSON data"); pd.setValue(2); }
			saveGameData.parse(isNEXT);
			
			if (mainWindow!=null) {
				if (pd!=null) { pd.setTaskTitle("Update GUI"); pd.setValue(3); }
				SaveGameView saveGameView = new SaveGameView(mainWindow,saveGameFile,saveGameData,isNEXT);
				loadedSaveGames.add(saveGameView);
				contentPane.addSaveGameView(saveGameView);
				updateWindowTitle();
			}
		}
		
		if (mainWindow!=null) {
			contentPane.disabler.setEnable(ActionCommand.Close    , contentPane.selectedSaveGameView!=null);
			contentPane.disabler.setEnable(ActionCommand.Reload   , contentPane.selectedSaveGameView!=null);
			contentPane.disabler.setEnable(ActionCommand.Compare  , loadedSaveGames.size()>1 && compareTab==null);
			contentPane.disabler.setEnable(ActionCommand.WriteHTML, contentPane.selectedSaveGameView!=null);
			contentPane.disabler.setEnable(ActionCommand.WriteJSON, contentPane.selectedSaveGameView!=null);
		}
		
		return saveGameData;
	}

	private void reloadSaveGameView(SaveGameView view) {
		runWithProgressDialog(mainWindow,"Reload SaveGame", pd->{
			if (pd!=null) { pd.setTaskTitle("Parse file"); pd.setValue(0, 5); }
			log_ln("");
			log("Parse file \"%s\" ...",view.file.getPath());
			JSON_Object new_json_data = new JSON_Parser(view.file).parse();
			log_ln(" done");
			
			HashMap<String, Vector<String>> deObfuscatorUsage = null;
			boolean isNEXT = false;
			if (!SaveGameData.hasValue(new_json_data, "Version")) {
				if (pd!=null) { pd.setTaskTitle("DeObfuscate value names"); pd.setValue(1); }
				new_json_data = deObfuscator.deObfuscate(new_json_data);
				deObfuscatorUsage = deObfuscator.getUsage();
				isNEXT = true;
			}
			
			if (new_json_data!=null) {
				if (pd!=null) { pd.setTaskTitle("Prepare for new JSON data"); pd.setValue(2); }
				GameInfos.removeUsages(view.data);
				SaveGameData saveGameData = new SaveGameData(new_json_data,view.data.filename,view.data.index);
				saveGameData.setDeObfuscatorUsage(deObfuscatorUsage);
				if (pd!=null) { pd.setTaskTitle("Parse JSON data"); pd.setValue(3); }
				saveGameData.parse(isNEXT);
				if (pd!=null) { pd.setTaskTitle("Update GUI"); pd.setValue(4); }
				view.replaceData(saveGameData,isNEXT);
				contentPane.updateIDPanels();
			}
		});
	}

	private void closeSaveGameView(SaveGameView view) {
		runWithProgressDialog(mainWindow,"Close SaveGame", pd->{
			loadedSaveGames.remove(view);
			contentPane.removeSaveGameView(view);
			GameInfos.removeUsages(view.data);
			updateWindowTitle();
			
			if (loadedSaveGames.size()<2 && compareTab!=null) {
				contentPane.removeTab(compareTab);
				compareTab=null;
			}
		});
	}

	private void updateWindowTitle() {
		if (contentPane.selectedSaveGameView == null)
			mainWindow.setTitle("(New) No Man's Sky - Viewer");
		else
			mainWindow.setTitle("(New) No Man's Sky - Viewer - "+contentPane.selectedSaveGameView.file.getPath());
//		if (DEBUG) System.out.println("Set window title to \""+mainWindow.getTitle()+"\"");
	}

	public static void runWithProgressDialog(Window parent, String title, Consumer<ProgressDialog> useProgressDialog) {
		ProgressDialog.runWithProgressDialog(parent, title, 400, useProgressDialog);
	}
	
	public static class FrequentlyTask implements Runnable {
	
		private final Runnable task;
		private int interval_ms;
		private boolean stop;
		private boolean isRunning;
		private boolean skipFirstWait;
	
		public FrequentlyTask(int interval_ms, boolean skipFirstWait, Runnable task) {
			this.interval_ms = interval_ms;
			this.skipFirstWait = skipFirstWait;
			this.task = task;
			this.stop = false;
			this.isRunning = false;
		}
		
		public boolean isRunning() {
			return isRunning;
		}
	
		public boolean start() {
			if (isRunning) return false;
			stop = false;
			new Thread(this).start();
			return true;
		}
	
		public void stop() {
			stop = true;
			synchronized (this) { notifyAll(); }
		}
	
		@Override
		public void run() {
			isRunning = true;
			synchronized (this) {
				
				if (skipFirstWait)
					runTask();
				
				while (!stop) {
					long startTime = System.currentTimeMillis();
					while (!stop && System.currentTimeMillis()-startTime<interval_ms)
						try { wait(interval_ms-(System.currentTimeMillis()-startTime)); }
						catch (InterruptedException e) {}
					
					if (!stop)
						runTask();
				}
			}
			isRunning = false;
		}
	
		private void runTask() {
			task.run();
		}
	
	}

	static class Config {
		private static final String NMS_VIEWER_CFG = "NMS_Viewer.cfg";
		
		private String savegameSubFolder;
		private String savegameBackupFolder;
		
		Config() {
			savegameSubFolder   =null;
			savegameBackupFolder=null;
		}
		
		boolean isSavegameSubFolderKnown() { return savegameSubFolder   !=null; }
		boolean isBackupFolderKnown     () { return savegameBackupFolder!=null; }
		
		String getSavegameSubFolder(JFrame parent) {
			if (savegameSubFolder==null) {
				JFileChooser fileChooser = new JFileChooser(getGameFolder());
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				fileChooser.setMultiSelectionEnabled(false);
				fileChooser.setDialogTitle("Select subfolder that contains save games");
				if (fileChooser.showOpenDialog(parent)==JFileChooser.APPROVE_OPTION) {
					savegameSubFolder = fileChooser.getSelectedFile().getName();
					writeToFile();
				}
			}
			return savegameSubFolder;
		}
		
		String getBackupFolder(JFrame parent) {
			if (savegameBackupFolder==null) {
				JFileChooser fileChooser = new JFileChooser("./");
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				fileChooser.setMultiSelectionEnabled(false);
				fileChooser.setDialogTitle("Select folder that contains backuped save games");
				if (fileChooser.showOpenDialog(parent)==JFileChooser.APPROVE_OPTION) {
					savegameBackupFolder = fileChooser.getSelectedFile().getAbsolutePath();
					writeToFile();
				}
			}
			return savegameBackupFolder;
			//return new File("d:/Games/_game_data/__saves/No Man's Sky - AppData_Roaming_HelloGames_NMS_st_76561198016584395/savegame_PreNEXT");
		}

		static Config readFromFile() {
			Config config = new Config();
			
			File file = new File(NMS_VIEWER_CFG);
			if (!file.isFile()) return config;
			
			log_ln("Read Config from file \""+file.getPath()+"\" ...");
			String str;
			try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8))) {
				while ((str=in.readLine())!=null) {
					if (str.startsWith("SavegameSubFolder="   )) config.savegameSubFolder    = str.substring("SavegameSubFolder="   .length());
					if (str.startsWith("SavegameBackupFolder=")) config.savegameBackupFolder = str.substring("SavegameBackupFolder=".length());
				}
			}
			catch (FileNotFoundException e) { e.printStackTrace(); }
			catch (IOException e) { e.printStackTrace(); }
			
			return config;
		}
		
		void writeToFile() {
			File file = new File(NMS_VIEWER_CFG);
			try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8));) {
				
				if (savegameSubFolder   !=null) out.printf("SavegameSubFolder=%s\r\n"   ,savegameSubFolder   );
				if (savegameBackupFolder!=null) out.printf("SavegameBackupFolder=%s\r\n",savegameBackupFolder);
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static class DeObfuscator {
		
		private HashMap<String, String> replacements;
		private HashMap<String, Vector<String>> usage;

		DeObfuscator() {
			replacements = new HashMap<>();
			usage = null;
		}
		
		public HashMap<String,Vector<String>> getUsage() {
			return usage;
		}

		public String getReplacement(String originalStr) {
			return replacements.get(originalStr);
		}

		public JSON_Object deObfuscate(JSON_Object data) {
			
			usage = new HashMap<>();
			Result res = new Result();
			
			JSON_Data.traverseNamedValues(data, (path,nv)->{
				String originalStr = nv.name;
				
				Vector<String> u = usage.get(originalStr);
				if (u==null) usage.put(originalStr, u = new Vector<>());
				u.add(path);
				
				String newStr = getReplacement(originalStr);
				res.all++;
				if (newStr!=null) {
					nv.name = newStr;
					nv.wasDeObfuscated = true;
					res.known++;
				} else
					res.unkown.add(nv.name);
			});
			
			SaveViewer.log_ln("DeObfuscation done");
			SaveViewer.log_ln("   %d of %d replacements done",res.known,res.all);
			SaveViewer.log_ln("   %d unknown names",res.unkown.size());
			SaveViewer.log_ln("   %d known names",replacements.size());
			
			return data;
		}

		private static class Result {
			int known;
			int all;
			HashSet<String> unkown;

			Result() {
				known = 0;
				all = 0;
				unkown = new HashSet<>();
			}
		}

		public static DeObfuscator readFromFile() {
			DeObfuscator deObfuscator = new DeObfuscator();
			
			String resourcePath = "/NMS_Viewer.DeObfuscator.txt";
			long start = System.currentTimeMillis();
			log_ln("Read DeObfuscator definitions from resource file \""+resourcePath+"\" ...");
			try (BufferedReader in = new BufferedReader(new InputStreamReader(DeObfuscator.class.getResourceAsStream(resourcePath),StandardCharsets.UTF_8))) {
				HashSet<String> valueSet = new HashSet<>();
				String str;
				while ((str=in.readLine())!=null) {
					if (!str.isEmpty() && !str.startsWith("//")) {
						String key = str.substring(0,3);
						String value = str.substring(4);
						String oldValue = deObfuscator.replacements.put(key, value);
						boolean wasUniqueValue = valueSet.add(value);
						if (oldValue!=null)  log_error_ln("   ReDefinition of key \"%s\" from \"%s\" to \"%s\"", key, oldValue, value);
						if (!wasUniqueValue) log_error_ln("   Replacement \"%s\" is used twice or more", value);
					}
				}
			}
			catch (FileNotFoundException e) { e.printStackTrace(); }
			catch (IOException e) { e.printStackTrace(); }
			log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
			
			return deObfuscator;
		}
		
	}

	class TabHeader extends JPanel {
		private static final long serialVersionUID = 2135969080088517737L;

		public TabHeader(SaveGameView saveGameView) {
			super();
			setOpaque(false);
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			add(new JLabel(saveGameView.toString()+" "));
			add(createButton(TabHeaderIcons.Reload, TabHeaderIcons.Reload_Inactive, e->reloadSaveGameView(saveGameView)));
			add(createButton(TabHeaderIcons.Close , TabHeaderIcons.Close_Inactive , e-> closeSaveGameView(saveGameView)));
		}

		private JButton createButton(TabHeaderIcons activeIcon, TabHeaderIcons inactiveIcon, ActionListener l) {
			JButton button = new JButton(tabheaderIS.getIcon(inactiveIcon));
			button.setFocusable(false);
			button.setMargin(new Insets(0,0,0,0));
			button.setRolloverIcon(tabheaderIS.getIcon(activeIcon));
			button.setContentAreaFilled(false);
			button.addActionListener(l);
			button.setPreferredSize(new Dimension(12,10));
			return button;
		}
	}

	private class ContentPane extends JPanel {
		private static final long serialVersionUID = -2737846401785644788L;
		
		private Disabler<ActionCommand> disabler;
		private JTabbedPane tabbedPane;
		private SaveGameView selectedSaveGameView;

		private GameInfos.GeneralizedIDPanel techIDsPanel;
		private GameInfos.GeneralizedIDPanel productIDsPanel;
		private GameInfos.GeneralizedIDPanel substanceIDsPanel;
		
		ContentPane() {
			super( new BorderLayout(3,3) );
			setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
			
			disabler = new Disabler<ActionCommand>();
			disabler.setCareFor(ActionCommand.values());
			
			JToolBar toolBar = new JToolBar("Standard");
			addButtons(toolBar);
			toolBar.setFloatable(false);
			toolBar.setRollover(true);
			
			selectedSaveGameView = null;
			tabbedPane = new JTabbedPane();
			tabbedPane.setPreferredSize(new Dimension(1600, 800));
			tabbedPane.addTab("Technology IDs", techIDsPanel      = new GameInfos.GeneralizedIDPanel(mainWindow, GameInfos.techIDs     , "TechnologyIDsTable"));
			tabbedPane.addTab("Product IDs"   , productIDsPanel   = new GameInfos.GeneralizedIDPanel(mainWindow, GameInfos.productIDs  , "ProductIDsTable"));
			tabbedPane.addTab("Substance IDs" , substanceIDsPanel = new GameInfos.GeneralizedIDPanel(mainWindow, GameInfos.substanceIDs, "SubstanceIDsTable"));
			
			tabbedPane.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					Component comp = tabbedPane.getSelectedComponent();
					if (comp instanceof SaveGameView) selectedSaveGameView = (SaveGameView)comp;
					else selectedSaveGameView = null;
					if (comp instanceof GameInfos.GeneralizedIDPanel) ((GameInfos.GeneralizedIDPanel)comp).updatePanel();
					actionPerformed(new ActionEvent(tabbedPane, ActionEvent.ACTION_FIRST, ActionCommand.TabSelected.toString()));
				}
			});
			
			add(toolBar,BorderLayout.PAGE_START);
			add(tabbedPane,BorderLayout.CENTER);
			
		}

		public void updateIDPanels() {
			techIDsPanel     .updatePanel();
			productIDsPanel  .updatePanel();
			substanceIDsPanel.updatePanel();
		}

		public boolean isSelected(JPanel panel) {
			return panel!=null && tabbedPane.getSelectedComponent()==panel;
		}

		public void addTab(String name, JPanel panel, int index) {
			tabbedPane.insertTab(name, null, panel, null, index);
		}

		public void addSaveGameView(SaveGameView saveGameView) {
			int index = tabbedPane.getTabCount();
			tabbedPane.insertTab(saveGameView.toString(),null, saveGameView, saveGameView.file.getPath(), index);
			tabbedPane.setTabComponentAt(index, new TabHeader(saveGameView));
			techIDsPanel     .addUsage(saveGameView);
			productIDsPanel  .addUsage(saveGameView);
			substanceIDsPanel.addUsage(saveGameView);
			tabbedPane.setSelectedIndex(index);
		}

		public void removeTab(JPanel panel) {
			tabbedPane.remove(panel);
		}

		public void removeSaveGameView(SaveGameView saveGameView) {
			tabbedPane.remove(saveGameView);
			techIDsPanel     .removeUsage(saveGameView);
			productIDsPanel  .removeUsage(saveGameView);
			substanceIDsPanel.removeUsage(saveGameView);
		}

		private void addButtons(JToolBar toolBar) {
			toolBar.add(createButton(ToolbarIcons.Open, ActionCommand.save_hg  ,true));
			toolBar.add(createButton(ToolbarIcons.Open, ActionCommand.save2_hg ,true));
			toolBar.addSeparator();
			toolBar.add(createButton(ToolbarIcons.Open, ActionCommand.save3_hg ,true));
			toolBar.add(createButton(ToolbarIcons.Open, ActionCommand.save4_hg ,true));
			toolBar.addSeparator();
			toolBar.add(createButton(ToolbarIcons.Open, ActionCommand.save5_hg ,true));
			toolBar.add(createButton(ToolbarIcons.Open, ActionCommand.save6_hg ,true));
			toolBar.addSeparator();
			toolBar.add(createButton(ToolbarIcons.Open, ActionCommand.save7_hg ,true));
			toolBar.add(createButton(ToolbarIcons.Open, ActionCommand.save8_hg ,true));
			toolBar.addSeparator();
			toolBar.add(createButton(ToolbarIcons.Open, ActionCommand.save9_hg ,true));
			toolBar.add(createButton(ToolbarIcons.Open, ActionCommand.save10_hg,true));
			toolBar.addSeparator();
			toolBar.add(createButton("Compute Coordinates" , ToolbarIcons.ComputePortalGlyphs, ActionCommand.ComputeCoordinates,true));
//			toolBar.add(createButton("Select Coordinates"  , ToolbarIcons.ComputePortalGlyphs, ActionCommand.SelectCoordinates,true));
			toolBar.add(createButton("Refresh Extra Images", ToolbarIcons.Reload, ActionCommand.RefreshExtraImages,true));
			toolBar.add(createButton("Show Extra Images"   , ToolbarIcons.Open,   ActionCommand.ShowExtraImages   ,true));
			
			JPopupMenu extraMenu = new JPopupMenu("Extra");
			extraMenu.add(createMenuItem("Switch to NMS Savegame Folder", ToolbarIcons.SwitchFolder, ActionCommand.SwitchToGameFolder ,true));
			extraMenu.add(createMenuItem("Switch to Backup Folder"      , ToolbarIcons.SwitchFolder, ActionCommand.SwitchToBackupFolder,true));
			extraMenu.add(createMenuItem("Open Savegame", ToolbarIcons.Open  , ActionCommand.Open  ,true));
//			extraMenu.add(createMenuItem("Reload"       , ToolbarIcons.Reload, ActionCommand.Reload,false));
//			extraMenu.add(createMenuItem("Close"        , ToolbarIcons.Close , ActionCommand.Close ,false));
			extraMenu.add(createMenuItem("Compare Savegames", ToolbarIcons.Compare, ActionCommand.Compare,false));
			extraMenu.addSeparator();
			extraMenu.add(createMenuItem("Write as HTML", ToolbarIcons.SaveAs, ActionCommand.WriteHTML,false));
			extraMenu.add(createMenuItem("Write as JSON", ToolbarIcons.SaveAs, ActionCommand.WriteJSON,false));

			JButton extrabutton; 
			toolBar.addSeparator();
			toolBar.add(extrabutton = createButton("Extra", null, true));
			extrabutton.addActionListener(e->{
				//extrabutton.getX
				extraMenu.show(extrabutton,0,extrabutton.getHeight());
			});
			
			
		}

		private JButton createButton(String title, ToolbarIcons iconKey, boolean enabled) {
			JButton button = new JButton(title);
			if (iconKey!=null) button.setIcon(toolbarIS.getIcon(iconKey));
			button.setEnabled(enabled);
			return button;
		}

		private JButton createButton(ToolbarIcons iconKey, ActionCommand actionCommand, boolean enabled) {
			return createButton(actionCommand.label, iconKey, actionCommand, enabled);
		}

		private JButton createButton(String title, ToolbarIcons iconKey, ActionCommand actionCommand, boolean enabled) {
			JButton button = createButton(title, iconKey, enabled);
			button.setActionCommand(actionCommand.toString());
			button.addActionListener(SaveViewer.this);
			disabler.add(actionCommand, button);
			return button;
		}

		private JMenuItem createMenuItem(String title, ToolbarIcons iconKey, ActionCommand actionCommand, boolean enabled) {
			JMenuItem button = new JMenuItem(title);
			if (iconKey!=null) button.setIcon(toolbarIS.getIcon(iconKey));
			button.setEnabled(enabled);
			button.setActionCommand(actionCommand.toString());
			button.addActionListener(SaveViewer.this);
			disabler.add(actionCommand, button);
			return button;
			
		}
		
	}

	public enum ToolbarIcons { SwitchFolder, Open, SaveAs, Close, Reload, Compare, ComputePortalGlyphs, Cut, Copy, Paste, Delete }

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

	public static void copyToClipBoard(String str) {
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Clipboard clipboard = toolkit.getSystemClipboard();
		DataHandler content = new DataHandler(str,"text/plain");
		try { clipboard.setContents(content,null); }
		catch (IllegalStateException e1) { e1.printStackTrace(); }
	}

	public static String pasteFromClipBoard() {
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Clipboard clipboard = toolkit.getSystemClipboard();
		Transferable transferable = clipboard.getContents(null);
		if (transferable==null) return null;
		
		DataFlavor textFlavor = new DataFlavor(String.class, "text/plain; class=<java.lang.String>");
		
		if (!transferable.isDataFlavorSupported(textFlavor)) {
			DataFlavor[] transferDataFlavors = transferable.getTransferDataFlavors();
			if (transferDataFlavors==null || transferDataFlavors.length==0) return null;
			
			log_ln("transferDataFlavors: "+toString(transferDataFlavors));
			textFlavor = DataFlavor.selectBestTextFlavor(transferDataFlavors);
		}
		
		if (textFlavor==null) return null;
		
		Reader reader;
		try { reader = textFlavor.getReaderForText(transferable); }
		catch (UnsupportedFlavorException | IOException e) { return null; }
		StringWriter sw = new StringWriter();
		
		int n; char[] cbuf = new char[100000];
		try { while ((n=reader.read(cbuf))>=0) if (n>0) sw.write(cbuf, 0, n); }
		catch (IOException e) {}
		
		try { reader.close(); } catch (IOException e) {}
		return sw.toString();
	}

	private static String toString(DataFlavor[] dataFlavors) {
		if (dataFlavors==null) return "<null>";
		String str = "";
		for (DataFlavor df:dataFlavors) {
			if (!str.isEmpty()) str+=",\r\n";
			str+=""+df;
		}
		return "[\r\n"+str+"\r\n]";
	}

	public static <T> T[] addNull(T[] arr) {
		Vector<T> vec = new Vector<>(Arrays.asList(arr));
		vec.insertElementAt(null,0);
		return vec.toArray(Arrays.copyOf(arr,0));
	}

	public static <T> Vector<T> addNull(Vector<T> arr) {
		Vector<T> vec = new Vector<>(arr);
		vec.insertElementAt(null,0);
		return vec;
	}

	public static void log_ln      ( String format, Object... values ) { System.out.printf(Locale.ENGLISH,format+"\r\n",values); }
	public static void log         ( String format, Object... values ) { System.out.printf(Locale.ENGLISH,format       ,values); }
	public static void log_error_ln( String format, Object... values ) { System.err.printf(Locale.ENGLISH,format+"\r\n",values); }
	public static void log_error   ( String format, Object... values ) { System.err.printf(Locale.ENGLISH,format       ,values); }
	public static void log_warn_ln ( String format, Object... values ) { System.err.printf(Locale.ENGLISH,format+"\r\n",values); }
	public static void log_warn    ( String format, Object... values ) { System.err.printf(Locale.ENGLISH,format       ,values); }

	public static JButton createButton(String title, ActionListener l) {
		JButton button = new JButton(title);
		if (l!=null) button.addActionListener(l);
		return button;
	}

	public static <AC extends Enum<AC>> JButton createButton(String title, ActionListener l, AC actionCommand) {
		JButton button = createButton(title,l);
		button.setActionCommand(actionCommand.toString());
		return button;
	}

	public static JMenuItem createMenuItem(String title, ActionListener l, boolean enabled) {
		return createMenuItem(title, l, null, enabled, null);
	}

	public static JMenuItem createMenuItem(String title, ActionListener l) {
		return createMenuItem(title, l, null);
	}

	public static <AC extends Enum<AC>> JMenuItem createMenuItem(String title, ActionListener l, AC actionCommand) {
		return createMenuItem(title, l, actionCommand, null);
	}

	public static <AC extends Enum<AC>> JMenuItem createMenuItem(String title, ActionListener l, AC actionCommand, ToolbarIcons icon) {
		return createMenuItem(title, l, actionCommand, true, icon);
	}

	public static <AC extends Enum<AC>> JMenuItem createMenuItem(String title, ActionListener l, AC actionCommand, boolean enabled, ToolbarIcons icon) {
		JMenuItem menuItem = new JMenuItem(title);
		menuItem.setEnabled(enabled);
		if (l!=null) menuItem.addActionListener(l);
		if (actionCommand!=null) menuItem.setActionCommand(actionCommand.toString());
		if (icon!=null) menuItem.setIcon(toolbarIS.getIcon(icon));
		return menuItem;
	}

	public static JCheckBox createCheckbox(String title, ActionListener l, boolean isSelected) {
		JCheckBox button = new JCheckBox(title,isSelected);
		if (l!=null) button.addActionListener(l);
		return button;
	}
	
	public static TristateCheckBox createTristateCheckBox(String title, ActionListener l, TristateCheckBox.State state) {
		TristateCheckBox button = new TristateCheckBox(title,state);
		if (l!=null) button.addActionListener(l);
		return button;
	}

	public static JTextField createTextField(String txt, Consumer<String> setInput) {
		JTextField obj = new JTextField(txt);
		if (setInput!=null) {
			obj.addActionListener(e->setInput.accept(obj.getText()));
			obj.addFocusListener(new FocusListener() {
				@Override public void focusGained(FocusEvent e) {}
				@Override public void focusLost(FocusEvent e) { setInput.accept(obj.getText()); }
			});
		}
		return obj;
	}

	public static JTextField createTextField(String txt, ActionListener l) {
		JTextField obj = new JTextField(txt);
		if (l!=null) {
			obj.addActionListener(l);
			obj.addFocusListener(new FocusListener() {
				@Override public void focusGained(FocusEvent e) {}
				@Override public void focusLost(FocusEvent e) { l.actionPerformed(new ActionEvent(obj, ActionEvent.ACTION_FIRST, null)); }
			});
		}
		return obj;
	}
}
