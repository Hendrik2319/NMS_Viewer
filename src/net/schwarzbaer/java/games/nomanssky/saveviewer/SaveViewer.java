package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
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

import net.schwarzbaer.java.games.nomanssky.recipeanalyser.RecipeAnalyser;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Images.ShowImagesDialog;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Duration;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.FactoryForExtras;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.KnownSteamIDs;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.NVExtra;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.VExtra;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.GlobalDeObfuscatorUsagePanel;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.SaveGameView;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.SimplePanels;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TreeView;
import net.schwarzbaer.java.lib.gui.Disabler;
import net.schwarzbaer.java.lib.gui.HexViewPanel;
import net.schwarzbaer.java.lib.gui.IconSource;
import net.schwarzbaer.java.lib.gui.ProgressDialog;
import net.schwarzbaer.java.lib.gui.StandardMainWindow;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.JSON_Object;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser;
import net.schwarzbaer.java.lib.system.ClipboardTools;

public class SaveViewer implements ActionListener {

	private static final Color COLOR_Creative_SaveGame = Color.BLUE;
	private static final Color COLOR_Expedition_SaveGame = new Color(0x008000);
	private static final Color COLOR_PreNext_SaveGame = Color.RED;
	public static final boolean DEBUG = true;
	private StandardMainWindow mainWindow;

	enum TabHeaderIcons { Close, Close_Inactive, Reload, Reload_Inactive }
	static IconSource<TabHeaderIcons> tabheaderIS;
	public static Config config;
	public static DeObfuscator deObfuscator;
	public static KnownSteamIDs steamIDs;
	
	private JFileChooser inputFileChooser;
	private JFileChooser htmlFileChooser;
	private JFileChooser jsonFileChooser;
	private ContentPane contentPane;
	private ComparePanel compareTab;
	private final Vector<SaveGameView> loadedSaveGames;
	private Color DEFAULT_BUTTON_FOREGROUND_COLOR;
	private final EnumMap<Tool,ToolWindow> openTools;
	
	private enum Tool {
		RecipeAnalyser,
		ProductionOptimiser,
		UpgradeModuleInstallHelper
	}
	
	public static void main(String[] args) {
		config = Config.readFromFile();
		deObfuscator = DeObfuscator.readFromFile();
		steamIDs = new KnownSteamIDs();
		steamIDs.readFromFile();
		
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
		
		loadTabHeaderIcons();
		Gui.loadToolbarIcons();
		
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

	static void loadTabHeaderIcons() {
		tabheaderIS = new IconSource<>(10,10);
		tabheaderIS.readIconsFromResource(FileExport.RES_IMAGES_TAB_HEADER_PNG);
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

	public static void writeSaveViewerFunctionsInUsage() {
		Gui.log_ln("[Function]");
		Gui.log_ln("      -writeKnownSteamIDsToHTML     writes known SteamIDs to HTML file \"%s\"", FileExport.FILE_KNOWN_STEAM_ID_HTML);
		Gui.log_ln("      -loadGame <G> -base2vrml <B>  load save game G and writes base B to VRML file");
	}
	private static void processCommands(String[] args) {
		int loadSavegame = -1;
		int writeBase2VRML = -1;
		boolean writeKnownSteamIDsToHTML = false;
		for (int i=0; i<args.length; i++) {
			switch (args[i].toLowerCase()) {
			
			case "-writeknownsteamidstohtml":
				writeKnownSteamIDsToHTML = true;
				break;
				
			case "-loadgame":
				if (i+1<args.length) {
					try { loadSavegame = Integer.parseInt(args[i+1]); i++; }
					catch (NumberFormatException e) { loadSavegame = -1; }
				}
				break;
				
			case "-base2vrml":
				if (i+1<args.length) {
					try { writeBase2VRML = Integer.parseInt(args[i+1]); i++; }
					catch (NumberFormatException e) { writeBase2VRML = -1; }
				}
				break;
			}
		}
		
		if (writeKnownSteamIDsToHTML)
			FileExport.writeKnownSteamIDsToHTML();
		
		if (loadSavegame<1)
			return;
		
		SaveViewer saveViewer = new SaveViewer();
		File saveGameFile = new File(saveViewer.getSavegameFolder()+SaveGameButtons.getFilename(loadSavegame-1));
		if (!saveGameFile.isFile())
			return;
		
		SaveGameData data = saveViewer.openSaveGame(saveGameFile, loadSavegame-1, null);
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
		FileExport.writePosToVRML_models(suggestFileName,null,playerbase,null,"Base "+baseIndex,true,null);
	}

	@SuppressWarnings("unused")
	private static void writeUIDefaults(String title, UIDefaults defaults) {
		Gui.log_ln(title+".keys: [");
		Set<Object> keySet = defaults.keySet();
		TreeSet<Object> sortedSet = new TreeSet<Object>(Comparator.nullsLast((o1, o2) -> o1.toString().compareTo(o2.toString())));
		sortedSet.addAll(keySet);
		for (Object key:sortedSet)
			Gui.log_ln("\t"+key);
		Gui.log_ln("]");
	}
	
	public SaveViewer() {
		loadedSaveGames = new Vector<>();
		mainWindow = null;
		openTools = new EnumMap<>(Tool.class);
	}

	private void createGUI() {
		DEFAULT_BUTTON_FOREGROUND_COLOR = new JButton().getForeground();
		
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
		
		@SuppressWarnings("resource")
		ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
		//FrequentlyTask frequentlyUpdater = new FrequentlyTask(5000,true,this::checkSavegameExistence);
		//mainWindow = new StandardMainWindow("",e->frequentlyUpdater.stop(),StandardMainWindow.DefaultCloseOperation.EXIT_ON_CLOSE);
		//mainWindow = new StandardMainWindow("", e->executor.shutdown(), StandardMainWindow.DefaultCloseOperation.EXIT_ON_CLOSE);
		mainWindow = new StandardMainWindow("", e->checkClosing(()->executor.shutdown()));
		
		compareTab = null;
		contentPane = new ContentPane();
		
		mainWindow.startGUI(contentPane);
		mainWindow.setIconImagesFromResource("/logo/", "applogo_16.png", "applogo_24.png", "applogo_32.png", "applogo_48.png", "applogo_64.png", "applogo_96.png", "applogo_128.png", "applogo_256.png", "applogo_320.png");
		
		
		updateWindowTitle();
		
		executor.scheduleAtFixedRate(this::checkSavegameExistence, 0, 5, TimeUnit.SECONDS);
		//frequentlyUpdater.start();
	}

	private void checkClosing(Runnable lastActionBeforeClose) {
		for (Tool tool : Tool.values()) {
			ToolWindow toolWindow = openTools.get(tool);
			if (toolWindow==null)
				continue;
			
			Window window = toolWindow.getWindow();
			if (window!=null && !window.isDisplayable())
				continue;
			
			if (!toolWindow.isClosingAllowed())
				return;
		}
		
		for (Tool tool : Tool.values()) {
			ToolWindow toolWindow = openTools.get(tool);
			if (toolWindow != null) {
				Window window = toolWindow.getWindow();
				if (window != null)
					window.dispose();
			}
		}
		
		if (lastActionBeforeClose!=null) lastActionBeforeClose.run();
		mainWindow.dispose();
		System.exit(0);
	}

	private enum ActionCommand {
		Open, Reload, Close, Compare,
		WriteHTML, WriteJSON,
		SwitchToGameFolder, SwitchToBackupFolder,
		TabSelected, ComputeCoordinates, SelectCoordinates,
		RefreshExtraImages, FindNewExtraImages, ShowExtraImages,
		OpenRecipeAnalyser, OpenProductionOptimiser, OpenUpgradeModuleInstallHelper,
		WriteKnownSteamIDsToHTML, SetPathToVRMLviewer,
		EditItemBackgroundColors,
		ClearOptionalValues, ShowOptionalValues,
		ClearUnknownValues, ShowUnknownValues,
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		ActionCommand actionCommand = ActionCommand.valueOf(e.getActionCommand());
		switch (actionCommand) {
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
			new Gui.CoordinatesDialog(mainWindow,getCurrentUniverse()).showDialog();
			break;
			
		case SelectCoordinates: {
			Gui.CoordinatesDialog dlg = new Gui.CoordinatesDialog(mainWindow,getCurrentUniverse(),true,"Select Coordinates");
			dlg.showDialog();
			if (dlg.hasResult()) {
				dlg.getResult();
			}
		} break;
			
		case RefreshExtraImages:
			Gui.runWithProgressDialog(mainWindow,"Refresh Extra Images", pd->{
				Images.getInstance().extraImages.reload(pd);
			});
			break;
			
		case FindNewExtraImages:
			Gui.runWithProgressDialog(mainWindow,"Find New Extra Images", pd->{
				Images.getInstance().extraImages.findNewImages(pd);
			});
			break;
			
		case ShowExtraImages:
			new ShowImagesDialog(mainWindow,"Extra Images").showDialog();
			break;
			
		case EditItemBackgroundColors:
			new Images.ColorListDialog(mainWindow,"Edit Item Background Colors").showDialog();
			break;
			
		case OpenRecipeAnalyser:
			startTool(Tool.RecipeAnalyser, ()->RecipeAnalyser.start(false));
			break;
			
		case OpenProductionOptimiser:
			startTool(Tool.ProductionOptimiser, ()->ProductionOptimiser.start(false));
			break;
			
		case OpenUpgradeModuleInstallHelper:
			startTool(Tool.UpgradeModuleInstallHelper, ()->UpgradeModuleInstallHelper.start(false));
			break;
			
		case WriteKnownSteamIDsToHTML:
			FileExport.writeKnownSteamIDsToHTML();
			break;
			
		case SetPathToVRMLviewer:
			String path = JOptionPane.showInputDialog(mainWindow, "Set path to VRML viewer:");
			if (path!=null) {
				SaveViewer.config.vrmlViewer = path;
				SaveViewer.config.writeToFile();
			}
			break;
			
		case ClearOptionalValues: SaveGameData.globalOptionalValues.clear(); break;
		case ShowOptionalValues : SaveGameData.globalOptionalValues.show("Optional Values",System.err); break;
		
		case ClearUnknownValues: SaveGameData.globalUnknownValues.clear(); break;
		case ShowUnknownValues : SaveGameData.globalUnknownValues.show(System.err); break;
		}
	}

	private void startTool(Tool tool, Supplier<ToolWindow> startTool) {
		
		ToolWindow toolWindow = openTools.get(tool);
		if (toolWindow == null) {
			createTool(tool, startTool);
			return;
		}
		
		Window window = toolWindow.getWindow();
		if (window == null) {
			createTool(tool, startTool);
			return;
		}
		
		if (!window.isDisplayable()) {
			createTool(tool, startTool);
			return;
		}
		
		if (window.isVisible())
			window.requestFocus();
		else
			window.setVisible(true);
	}

	private void createTool(Tool tool, Supplier<ToolWindow> startTool) {
		ToolWindow toolWindow = startTool.get();
		if (toolWindow!=null)
			openTools.put(tool, toolWindow);
	}

	private Universe getCurrentUniverse() {
		if (contentPane.selectedSaveGameView == null) return null;
		return contentPane.selectedSaveGameView.data.universe;
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
			contentPane.saveGameButtons.forEach((filename, comp) -> {
				File savefile = new File(getSavegameFolder()+filename);
				String lastModified = SaveGameData.TimeStamp.getTimeStr(savefile.lastModified());
				SaveGameData data = !savefile.isFile() ? null : openSaveGameForPreview(savefile);
				if (data==null) {
					comp.setToolTipText(savefile.isFile() ? "Can't parse SaveGame" : "SaveGame not exists");
				} else if (data.isPreNEXT) {
					comp.setToolTipText("PreNext SaveGame (will not be parsed)");
					comp.setForeground(COLOR_PreNext_SaveGame);
				} else if (data.version!=null && data.version>100000) {
					comp.setToolTipText(String.format("Expedition ( %s h, %s )", Duration.toString(data.general.totalPlayTime), lastModified));
					comp.setForeground(COLOR_Expedition_SaveGame);
				} else if (data.version!=null && data.version>5000) {
					comp.setToolTipText(String.format("Creative ( %s h, %s )", Duration.toString(data.general.totalPlayTime), lastModified));
					comp.setForeground(COLOR_Creative_SaveGame);
				} else {
					comp.setToolTipText(String.format("Normal ( %s h, %s )", Duration.toString(data.general.totalPlayTime), lastModified));
					comp.setForeground(DEFAULT_BUTTON_FOREGROUND_COLOR);
				}
				comp.setEnabled(savefile.isFile());
			});
	}

	private void openSaveGame(File saveGameFile, int saveGameIndex) {
		Gui.runWithProgressDialog(mainWindow,"Open SaveGame", pd->{
			openSaveGame(saveGameFile,saveGameIndex,pd);
		});
	}

	private SaveGameData openSaveGameForPreview(File saveGameFile) {
		JSON_Object<NVExtra, VExtra> new_json_data = loadAndParseSaveGameFile(saveGameFile, false);
		
		HashMap<String, HashSet<String>> deObfuscatorUsage = null;
		boolean isPreNEXT;
		if (SaveGameData.hasValue(new_json_data, "Version"))
			isPreNEXT = true;
		else {
			deObfuscatorUsage = deObfuscator.deObfuscate(new_json_data,false);
			isPreNEXT = false;
		}
		SaveGameData saveGameData;
		if (new_json_data==null) {
			saveGameData = null;
			
		} else {
			saveGameData = new SaveGameData(new_json_data,saveGameFile.getName(),-1,isPreNEXT);
			saveGameData.setDeObfuscatorUsage(deObfuscatorUsage);
			saveGameData.parse(true,null);
		}
		
		return saveGameData;
	}

	private SaveGameData openSaveGame(File saveGameFile, int saveGameIndex, ProgressDialog pd) {
		if (pd!=null) Gui.runInEventThreadAndWait(()->{ pd.setTaskTitle("Parse file"); pd.setValue(0, 4); });
		JSON_Object<NVExtra, VExtra> new_json_data = loadAndParseSaveGameFile(saveGameFile, true);
		
		HashMap<String, HashSet<String>> deObfuscatorUsage = null;
		boolean isPreNEXT;
		if (SaveGameData.hasValue(new_json_data, "Version"))
			isPreNEXT = true;
		else {
			if (pd!=null) Gui.runInEventThreadAndWait(()->{ pd.setTaskTitle("DeObfuscate value names"); pd.setValue(1); });
			deObfuscatorUsage = deObfuscator.deObfuscate(new_json_data);
			isPreNEXT = false;
		}
		
		SaveGameData saveGameData;
		if (new_json_data==null) {
			saveGameData = null;
			if (mainWindow!=null)
				JOptionPane.showMessageDialog(mainWindow, "Can't parse selected file. It is not a valid JSON formated No Man's Sky savegame.", "Parse Error", JOptionPane.ERROR_MESSAGE);
			
		} else {
			saveGameData = new SaveGameData(new_json_data,saveGameFile.getName(),saveGameIndex,isPreNEXT);
			saveGameData.setDeObfuscatorUsage(deObfuscatorUsage);
			
			if (pd!=null) Gui.runInEventThreadAndWait(()->{ pd.setTaskTitle("Parse JSON data"); pd.setValue(2); });
			boolean parsedWithoutException = saveGameData.parse_guarded(false,mainWindow);
			
			Gui.runInEventThreadAndWait(()->{
				if (mainWindow!=null) {
					if (pd!=null) { pd.setTaskTitle("Update GUI"); pd.setValue(3); }
					SaveGameView saveGameView = new SaveGameView(mainWindow,saveGameFile,saveGameData,!isPreNEXT,!parsedWithoutException);
					loadedSaveGames.add(saveGameView);
					contentPane.addSaveGameView(saveGameView);
					updateWindowTitle();
				}
			});
		}
		
		Gui.runInEventThreadAndWait(()->{
			if (mainWindow!=null) {
				contentPane.disabler.setEnable(ActionCommand.Close    , contentPane.selectedSaveGameView!=null);
				contentPane.disabler.setEnable(ActionCommand.Reload   , contentPane.selectedSaveGameView!=null);
				contentPane.disabler.setEnable(ActionCommand.Compare  , loadedSaveGames.size()>1 && compareTab==null);
				contentPane.disabler.setEnable(ActionCommand.WriteHTML, contentPane.selectedSaveGameView!=null);
				contentPane.disabler.setEnable(ActionCommand.WriteJSON, contentPane.selectedSaveGameView!=null);
			}
		});
		
		return saveGameData;
	}
	
	private void reloadSaveGameView(SaveGameView view) {
		Gui.runWithProgressDialog(mainWindow,"Reload SaveGame", pd->{
			if (pd!=null) Gui.runInEventThreadAndWait(()->{ pd.setTaskTitle("Parse file"); pd.setValue(0, 5); });
			Gui.log_ln("");
			JSON_Object<NVExtra, VExtra> new_json_data = loadAndParseSaveGameFile(view.file, true);
			
			HashMap<String, HashSet<String>> deObfuscatorUsage = null;
			boolean isPreNEXT;
			if (SaveGameData.hasValue(new_json_data, "Version")) // <--- UnObfuscated String "Version"
				isPreNEXT = true;
			else {
				if (pd!=null) Gui.runInEventThreadAndWait(()->{ pd.setTaskTitle("DeObfuscate value names"); pd.setValue(1); });
				deObfuscatorUsage = deObfuscator.deObfuscate(new_json_data);
				isPreNEXT = false;
			}
			
			if (new_json_data!=null) {
				
				if (pd!=null) Gui.runInEventThreadAndWait(()->{ pd.setTaskTitle("Prepare for new JSON data"); pd.setValue(2); });
				GameInfos.removeUsages(view.data);
				SaveGameData saveGameData = new SaveGameData(new_json_data,view.data.filename,view.data.index,isPreNEXT);
				saveGameData.setDeObfuscatorUsage(deObfuscatorUsage);
				
				if (pd!=null) Gui.runInEventThreadAndWait(()->{ pd.setTaskTitle("Parse JSON data"); pd.setValue(3); });
				boolean parsedWithoutException = saveGameData.parse_guarded(false,mainWindow);
				
				Gui.runInEventThreadAndWait(()->{
					if (pd!=null) { pd.setTaskTitle("Update GUI"); pd.setValue(4); }
					view.replaceData(saveGameData,!isPreNEXT,!parsedWithoutException);
					contentPane.updateIDPanels();
				});
			}
		});
	}
	
	private JSON_Object<NVExtra, VExtra> loadAndParseSaveGameFile(File saveGameFile, boolean withConsoleLog) {
		if (withConsoleLog) Gui.log("Parse file \"%s\" ...",saveGameFile.getPath());
		
		byte[] decodeBytes = SaveGameDecoder.decodeFile(saveGameFile);
		if (decodeBytes==null)
		{
			if (withConsoleLog) Gui.log_ln(" can't read file");
			return null;
		}
		
		String fileContent = new String(decodeBytes, StandardCharsets.UTF_8);
		JSON_Data.Value<NVExtra,VExtra> result = JSON_Parser.parse(fileContent,new FactoryForExtras(),null);
		JSON_Object<NVExtra,VExtra> new_json_data = JSON_Data.getObjectValue(result);
		
		if (new_json_data==null)
		{
			if (decodeBytes!=null)
				showBytes("Decoded Content of %s".formatted(saveGameFile.getName()), decodeBytes);
			throw new IllegalStateException("Parsed JSON tree is not an JSON object.");
		}
		
		JSON_Data.traverseAllValues(new_json_data, false, (path,nv)->nv.extra.setHost(nv), (path,v)->v.extra.setHost(v));
		
		if (withConsoleLog) Gui.log_ln(" done");
		
		return new_json_data;
	}

	void showBytes(String title, byte[] bytes)
	{
		showBytes(mainWindow, title, bytes, ModalityType.APPLICATION_MODAL);
	}
	static void showBytes(Window window, String title, byte[] bytes)
	{
		showBytes(window, title, bytes, ModalityType.APPLICATION_MODAL);
	}
	static void showBytes(Window window, String title, byte[] bytes, ModalityType modalityType)
	{
		HexViewPanel.showAsDialog(
				window, title,
				new HexViewPanel(HexViewPanel.PREFFERED_PAGESIZE, true), bytes,
				dlg -> {
					AppSettings.getInstance().registerExtraWindow(
							dlg,
							AppSettings.ValueKey.ParseErrorHexView_WindowX,
							AppSettings.ValueKey.ParseErrorHexView_WindowY,
							AppSettings.ValueKey.ParseErrorHexView_WindowWidth,
							AppSettings.ValueKey.ParseErrorHexView_WindowHeight,
							HexViewPanel.PREFFERED_WIDTH,
							HexViewPanel.PREFFERED_HEIGHT
					);
				},
				modalityType
		);
	}

	private void closeSaveGameView(SaveGameView view) {
		Gui.runWithProgressDialog(mainWindow,"Close SaveGame", pd->{
			Gui.runInEventThreadAndWait(()->{
				loadedSaveGames.remove(view);
				GameInfos.removeUsages(view.data);
				contentPane.removeSaveGameView(view);
				updateWindowTitle();
				if (loadedSaveGames.size()<2 && compareTab!=null) {
					contentPane.removeTab(compareTab);
					compareTab=null;
				}
			});
		});
	}

	private void updateWindowTitle() {
		if (contentPane.selectedSaveGameView == null)
			mainWindow.setTitle("(New) No Man's Sky - Viewer");
		else
			mainWindow.setTitle("(New) No Man's Sky - Viewer - "+contentPane.selectedSaveGameView.file.getPath());
//		if (DEBUG) System.out.println("Set window title to \""+mainWindow.getTitle()+"\"");
	}
	
	public interface ToolWindow {
		Window getWindow();
		boolean isClosingAllowed();
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

	public static class Config {
		private String savegameSubFolder    = null;
		private String savegameBackupFolder = null;
		
		public boolean openNewlyWrittenVrmlFileInViewer = false;
		public String vrmlViewer = null;
		
		public HashSet<String> highlightedBuildingObjects = new HashSet<>();
		public boolean useSmallLightsAsMeasurePoints = false;
		public boolean addNumberToObjectIDInAxisCross = false;
		
		Config() {
		}
		
		boolean isSavegameSubFolderKnown() { return savegameSubFolder   !=null; }
		boolean isBackupFolderKnown     () { return savegameBackupFolder!=null; }
		public boolean isVrmlViewerConfigured() { return vrmlViewer!=null; }

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
		}

		static Config readFromFile() {
			Config config = new Config();
			
			File file = new File(FileExport.FILE_CFG);
			if (!file.isFile()) return config;
			
			Gui.log_ln("Read Config from file \""+file.getPath()+"\" ...");
			String str;
			try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8))) {
				while ((str=in.readLine())!=null) {
					if (str.startsWith("SavegameSubFolder="         )) config.savegameSubFolder    = str.substring("SavegameSubFolder="   .length());
					if (str.startsWith("SavegameBackupFolder="      )) config.savegameBackupFolder = str.substring("SavegameBackupFolder=".length());
					if (str.startsWith("VrmlViewer="                )) config.vrmlViewer           = str.substring("VrmlViewer=".length());
					if (str.startsWith("HighlightedBuildingObjects=")) splitStringSet(str.substring("HighlightedBuildingObjects=".length()), ",", config.highlightedBuildingObjects);
					if (str.equals("OpenNewlyWrittenVrmlFileInViewer")) config.openNewlyWrittenVrmlFileInViewer = true;
					if (str.equals("UseSmallLightsAsMeasurePoints"   )) config.useSmallLightsAsMeasurePoints    = true;
					if (str.equals("AddNumberToObjectIDInAxisCross"  )) config.addNumberToObjectIDInAxisCross   = true;
				}
			}
			catch (FileNotFoundException e) { e.printStackTrace(); }
			catch (IOException e) { e.printStackTrace(); }
			
			return config;
		}
		
		public void writeToFile() {
			Gui.log_ln("Write Config to file \""+FileExport.FILE_CFG+"\" ...");
			long start = System.currentTimeMillis();
			File file = new File(FileExport.FILE_CFG);
			try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8));) {
				
				if ( savegameSubFolder   !=null) out.printf("SavegameSubFolder"   +"=%s%n",savegameSubFolder   );
				if ( savegameBackupFolder!=null) out.printf("SavegameBackupFolder"+"=%s%n",savegameBackupFolder);
				if ( vrmlViewer          !=null) out.printf("VrmlViewer"          +"=%s%n",vrmlViewer);
				if ( addNumberToObjectIDInAxisCross      ) out.printf("AddNumberToObjectIDInAxisCross%n");
				if ( useSmallLightsAsMeasurePoints       ) out.printf("UseSmallLightsAsMeasurePoints%n");
				if ( openNewlyWrittenVrmlFileInViewer    ) out.printf("OpenNewlyWrittenVrmlFileInViewer%n");
				if (!highlightedBuildingObjects.isEmpty()) out.printf("HighlightedBuildingObjects=%s%n", joinStringSet(",",highlightedBuildingObjects));
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			Gui.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
		}

		private static void splitStringSet(String str, String delimiter, Set<String> set) {
			String[] parts = str.split(delimiter);
			for (String objectID : parts)
				set.add(objectID);
		}

		private static String joinStringSet(String delimiter, Set<String> set) {
			Vector<String> vec = new Vector<>(set);
			vec.sort(null);
			return String.join(delimiter, vec);
		}
	}
	
	public static class DeObfuscator {
		
		private HashMap<String, String> replacements;

		DeObfuscator() {
			replacements = new HashMap<>();
		}

		public String getReplacement(String originalStr) {
			return replacements.get(originalStr);
		}

		public HashMap<String, HashSet<String>> deObfuscate(JSON_Object<NVExtra,VExtra> data) {
			return deObfuscate(data, true);
		}
		public HashMap<String, HashSet<String>> deObfuscate(JSON_Object<NVExtra,VExtra> data, boolean verbose) {
			
			HashMap<String, HashSet<String>> usage = new HashMap<>();
			Result res = new Result();
			
			JSON_Data.traverseNamedValues(data, false, (path,nv)->{
				String originalStr = nv.name;
				
				String newStr = getReplacement(originalStr);
				res.all++;
				if (newStr!=null) {
					nv.name = newStr;
					nv.extra.wasDeObfuscated = true;
					nv.extra.originalStr = originalStr;
					res.known++;
				} else
					res.unkown.add(nv.name);
			});
			
			JSON_Data.traverseNamedValues(data, false, (path,nv)->{
				String originalStr = nv.extra.wasDeObfuscated ? nv.extra.originalStr : nv.name;
				HashSet<String> u = usage.get(originalStr);
				if (u==null) usage.put(originalStr, u = new HashSet<>());
				u.add(path);
			});
			
			if (verbose) {
				Gui.log_ln("DeObfuscation done");
				Gui.log_ln("   %d of %d replacements done",res.known,res.all);
				Gui.log_ln("   %d unknown names",res.unkown.size());
				Gui.log_ln("   %d known names",replacements.size());
			}
			
			return usage;
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
			Gui.log_ln("Read DeObfuscator definitions from resource file \""+resourcePath+"\" ...");
			try (BufferedReader in = new BufferedReader(new InputStreamReader(DeObfuscator.class.getResourceAsStream(resourcePath),StandardCharsets.UTF_8))) {
				HashSet<String> valueSet = new HashSet<>();
				String str;
				while ((str=in.readLine())!=null) {
					if (!str.isEmpty() && !str.startsWith("//")) {
						String key = str.substring(0,3);
						String value = str.substring(4);
						String oldValue = deObfuscator.replacements.put(key, value);
						boolean wasUniqueValue = valueSet.add(value);
						if (oldValue!=null)  Gui.log_error_ln("   ReDefinition of key \"%s\" from \"%s\" to \"%s\"", key, oldValue, value);
						if (!wasUniqueValue) Gui.log_error_ln("   Replacement \"%s\" is used twice or more", value);
					}
				}
			}
			catch (FileNotFoundException e) { e.printStackTrace(); }
			catch (IOException e) { e.printStackTrace(); }
			Gui.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
			
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
	
	private class SaveGameButtons
	{
		private static int SAVE_GAME_COUNT = 15;
		private final List<JButton> buttons = new Vector<>(); 
		
		void addButtons(JToolBar toolBar)
		{
			buttons.clear();
			for (int i=0; i<SAVE_GAME_COUNT; i++)
			{
				toolBar.add(createButton(i*2+0));
				toolBar.add(createButton(i*2+1));
				toolBar.addSeparator();
			}
		}
		
		private JButton createButton(int index)
		{
			JButton button = Gui.createButton("%d".formatted(index+1), Gui.ToolbarIcons.Open, e -> {
				String filepath = getSavegameFolder() + getFilename(index);
				openSaveGame(new File(filepath),index);
			});
			buttons.add(button);
			return button;
		}
		
		static String getFilename(int index)
		{
			return index==0 ? "save.hg" : "save%d.hg".formatted(index+1);
		}
		
		void forEach(BiConsumer<String,JButton> action)
		{
			for (int index=0; index<buttons.size(); index++)
				action.accept(getFilename(index), buttons.get(index));
		}
	}

	private class ContentPane extends JPanel {
		private static final long serialVersionUID = -2737846401785644788L;
		
		private final Disabler<ActionCommand> disabler;
		private final JTabbedPane tabbedPane;
		private SaveGameView selectedSaveGameView;

		private final GameInfos.GeneralizedIDPanel techIDsPanel;
		private final GameInfos.GeneralizedIDPanel productIDsPanel;
		private final GameInfos.GeneralizedIDPanel substanceIDsPanel;
		private final GlobalDeObfuscatorUsagePanel globalDeObfuscatorUsagePanel;

		private final SaveGameButtons saveGameButtons;
		
		ContentPane() {
			super( new BorderLayout(3,3) );
			setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
			
			saveGameButtons = new SaveGameButtons();
			
			disabler = new Disabler<>();
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
			tabbedPane.addTab("Global DeObfuscator Usage", globalDeObfuscatorUsagePanel = new GlobalDeObfuscatorUsagePanel(loadedSaveGames));
			
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
			globalDeObfuscatorUsagePanel.updateSaveGameList();
		}

		public void removeTab(JPanel panel) {
			tabbedPane.remove(panel);
		}

		public void removeSaveGameView(SaveGameView saveGameView) {
			tabbedPane.remove(saveGameView);
			techIDsPanel     .removeUsage(saveGameView);
			productIDsPanel  .removeUsage(saveGameView);
			substanceIDsPanel.removeUsage(saveGameView);
			globalDeObfuscatorUsagePanel.updateSaveGameList();
		}

		private void addButtons(JToolBar toolBar) {
			saveGameButtons.addButtons(toolBar);
			toolBar.add(createButton("Compute Coordinates" , Gui.ToolbarIcons.ComputePortalGlyphs, ActionCommand.ComputeCoordinates,true));
//			toolBar.add(createButton("Select Coordinates"  , Gui.ToolbarIcons.ComputePortalGlyphs, ActionCommand.SelectCoordinates,true));
			
			JPopupMenu toolsMenu = new JPopupMenu("Tools");
			toolsMenu.add(createMenuItem("Refresh Extra Images" , Gui.ToolbarIcons.Reload, ActionCommand.RefreshExtraImages,true));
			toolsMenu.add(createMenuItem("Find New Extra Images", Gui.ToolbarIcons.Reload, ActionCommand.FindNewExtraImages,true));
			toolsMenu.add(createMenuItem("Show Extra Images"    , Gui.ToolbarIcons.Open,   ActionCommand.ShowExtraImages   ,true));
			toolsMenu.addSeparator();
			toolsMenu.add(createMenuItem("Edit Item Background Colors", Gui.ToolbarIcons.ColorEdit, ActionCommand.EditItemBackgroundColors,true));
			toolsMenu.addSeparator();
			toolsMenu.add(createMenuItem("Recipe Analyser"     , Gui.ToolbarIcons.Open, ActionCommand.OpenRecipeAnalyser,true));
			toolsMenu.add(createMenuItem("Production Optimiser", Gui.ToolbarIcons.Open, ActionCommand.OpenProductionOptimiser,true));
			toolsMenu.add(createMenuItem("UpgradeModule InstallHelper", Gui.ToolbarIcons.Open, ActionCommand.OpenUpgradeModuleInstallHelper,true));
			
			JPopupMenu extraMenu = new JPopupMenu("Extra");
			extraMenu.add(createMenuItem("Set path to VRML viewer", Gui.ToolbarIcons.Open, ActionCommand.SetPathToVRMLviewer,true));
			extraMenu.addSeparator();
			extraMenu.add(createMenuItem("Write KnownSteamIDs to HTML", Gui.ToolbarIcons.Save, ActionCommand.WriteKnownSteamIDsToHTML,true));
			extraMenu.addSeparator();
			extraMenu.add(createMenuItem("Switch to NMS Savegame Folder", Gui.ToolbarIcons.Open, ActionCommand.SwitchToGameFolder ,true));
			extraMenu.add(createMenuItem("Switch to Backup Folder"      , Gui.ToolbarIcons.Open, ActionCommand.SwitchToBackupFolder,true));
			extraMenu.add(createMenuItem("Open Savegame"    , Gui.ToolbarIcons.Open   , ActionCommand.Open   ,true));
//			extraMenu.add(createMenuItem("Reload"           , Gui.ToolbarIcons.Reload , ActionCommand.Reload ,false));
//			extraMenu.add(createMenuItem("Close"            , Gui.ToolbarIcons.Close  , ActionCommand.Close  ,false));
			extraMenu.add(createMenuItem("Compare Savegames", Gui.ToolbarIcons.EmptyDoc, ActionCommand.Compare,false));
			extraMenu.addSeparator();
			extraMenu.add(createMenuItem("Write as HTML", Gui.ToolbarIcons.SaveAs, ActionCommand.WriteHTML,false));
			extraMenu.add(createMenuItem("Write as JSON", Gui.ToolbarIcons.SaveAs, ActionCommand.WriteJSON,false));
			extraMenu.addSeparator();
			extraMenu.add(createMenuItem("Clear ScanResults of Optional Values", Gui.ToolbarIcons.Delete, ActionCommand.ClearOptionalValues,true));
			extraMenu.add(createMenuItem("Show ScanResults of Optional Values" , Gui.ToolbarIcons.Save  , ActionCommand.ShowOptionalValues ,true));
			extraMenu.addSeparator();
			extraMenu.add(createMenuItem("Clear Unknown Values", Gui.ToolbarIcons.Delete, ActionCommand.ClearUnknownValues,true));
			extraMenu.add(createMenuItem("Show Unknown Values" , Gui.ToolbarIcons.Save  , ActionCommand.ShowUnknownValues ,true));

			toolBar.addSeparator();
			toolBar.add(createButton("Tools", toolsMenu, true));
			toolBar.add(createButton("Extra", extraMenu, true));
		}

		private JButton createButton(String title, JPopupMenu popupMenu, boolean enabled) {
			JButton button = new JButton(title);
			if (popupMenu!=null)
				button.addActionListener(e->popupMenu.show(button,0,button.getHeight()));
			button.setEnabled(enabled);
			return button;
		}

		private JButton createButton(String title, Gui.ToolbarIcons iconKey, ActionCommand actionCommand, boolean enabled) {
			return Gui.createButton(title, SaveViewer.this, disabler, actionCommand, enabled, iconKey);
		}

		private JMenuItem createMenuItem(String title, Gui.ToolbarIcons iconKey, ActionCommand actionCommand, boolean enabled) {
			return Gui.createMenuItem(title, SaveViewer.this, disabler, actionCommand, enabled, iconKey);
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
			
			selector1 = new JComboBox<>(loadedSaveGames);
			selector2 = new JComboBox<>(loadedSaveGames);
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
			//updateTree();
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
			Gui.log("Set tree to files \"%s\" and \"%s\" ...",sg1.file.getPath(),sg2.file.getPath());
			tree.setModel(new DefaultTreeModel(new TreeView.CompareTreeNode(sg1.data.json_data,sg2.data.json_data)));
			tree.setCellRenderer(new TreeView.CompareTreeNode.CellRenderer());
			Gui.log_ln(" done");
		}
	
	}
	
	public static void test() {
		String filepath = "save.hg";
		File sourcefile = new File(filepath);
		
		JSON_Data.Value<NVExtra,VExtra> result = JSON_Parser.parse(sourcefile,StandardCharsets.UTF_8,new FactoryForExtras(),null);
		JSON_Object<NVExtra,VExtra> json_Object = JSON_Data.getObjectValue(result);
		if (json_Object==null) return;
		
		File htmlfile = new File("./"+sourcefile.getName()+".html"); 
		FileExport.writeToHTML(sourcefile.getName(),json_Object,htmlfile);
		
		File copyfile = new File("./"+sourcefile.getName()+".copy.txt"); 
		FileExport.writeToJSON(json_Object,copyfile);
	}

	public static void executeShellCommand(String[] cmdStrs) {
		try { Runtime.getRuntime().exec(cmdStrs); }
		catch (IOException e) { e.printStackTrace(); }
	}

	public static void copyToClipBoard(String str) {
		ClipboardTools.copyToClipBoard(str);
	}

	public static String pasteFromClipBoard() {
		return ClipboardTools.getStringFromClipBoard(true);
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
}
