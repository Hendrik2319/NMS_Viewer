package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Comparator;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.activation.DataHandler;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import net.schwarzbaer.gui.Disabler;
import net.schwarzbaer.gui.IconSource;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.RawDataTreePanel;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.SaveGameView;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TreeView;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.UniversePanel;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.JSON_Object;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser;

public class SaveViewer implements ActionListener {

	private static final String IMAGES_TOOLBAR_PNG = "/images/Toolbar.png";
	private static final String IMAGES_TAB_HEADER_PNG = "/images/TabHeader.png";
	public static final boolean DEBUG = true;
	private StandardMainWindow mainWindow;

	enum TabHeaderIcons { Close, Close_Inactive, Reload, Reload_Inactive }
	public static IconSource<ToolbarIcons> toolbarIS;
	static IconSource<TabHeaderIcons> tabheaderIS;
	public static Images images;
	
	private JFileChooser inputFileChooser;
	private JFileChooser htmlFileChooser;
	private JFileChooser jsonFileChooser;
	private ContentPane contentPane;
	private ComparePanel compareTab;
	private Vector<SaveGameView> loadedSaveGames;
	
	public static void main(String[] args) {
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		
		images = new Images();
		images.init();
		
		UniversePanel.prepareIconSources();
		RawDataTreePanel.prepareIconSource();
		FileExport.prepareModels();
		
		tabheaderIS = new IconSource<TabHeaderIcons>(10,10){
			@Override protected int getIconIndexInImage(TabHeaderIcons key) { return key.ordinal(); }
		};
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
		
		GameInfos.loadKnownStatIDsFromFile();
		GameInfos.loadProductIDsFromFile();
		GameInfos.loadTechIDsFromFile();
		GameInfos.loadSubstanceIDsFromFile();
		GameInfos.loadUniverseObjectDataFromFile();

		
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
	private static void writeUIDefaults(String title, UIDefaults defaults) {
		System.out.println(title+".keys: [");
		Set<Object> keySet = defaults.keySet();
		TreeSet<Object> sortedSet = new TreeSet<Object>(Comparator.nullsLast((o1, o2) -> o1.toString().compareTo(o2.toString())));
		sortedSet.addAll(keySet);
		for (Object key:sortedSet)
			System.out.println("\t"+key);
		System.out.println("]");
	}
	
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
		
		mainWindow = new StandardMainWindow();
		
		compareTab = null;
		contentPane = new ContentPane();
		
		mainWindow.startGUI(contentPane);
		updateWindowTitle();
	}
	
	private enum ActionCommand { Open, Reload, Close, WriteHTML, WriteJSON, SwitchFolder, Compare, TabSelected, ComputeCoordinates, save_hg, save2_hg, RefreshExtraImages, SelectCoordinates }

	@Override
	public void actionPerformed(ActionEvent e) {
		ActionCommand actionCommand = ActionCommand.valueOf(e.getActionCommand());
		switch (actionCommand) {
		case save_hg:
			openSaveGame(new File(getGameFolder().getPath()+"/st_76561198016584395/save.hg"));
			break;
		case save2_hg:
			openSaveGame(new File(getGameFolder().getPath()+"/st_76561198016584395/save2.hg"));
			break;
		case SwitchFolder: {
			inputFileChooser.setCurrentDirectory(getGameFolder());
			String message = String.format("Current folder changed to \"%s\"", inputFileChooser.getCurrentDirectory().getPath());
			JOptionPane.showMessageDialog(mainWindow, message, "Current folder", JOptionPane.INFORMATION_MESSAGE);
			break;
		}
		case Open:
			if (inputFileChooser.showOpenDialog(mainWindow)==JFileChooser.APPROVE_OPTION)
				openSaveGame(inputFileChooser.getSelectedFile());
			break;
			
		case Close:
			if (contentPane.selectedSaveGameView!=null) closeSaveGameView(contentPane.selectedSaveGameView);
			break;
			
		case Reload:
			if (contentPane.selectedSaveGameView!=null) reloadSaveGameView(contentPane.selectedSaveGameView);
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
			images.reloadImageList();
			break;
		}
	}

	private File getGameFolder() {
		Properties prop = System.getProperties();
//			ArrayList<Object> keys = new ArrayList<>(prop.keySet());
//			keys.sort(null);
//			for (Object key : keys) {
//				System.out.printf("[%s] = %s\r\n",key,prop.get(key));
//			}
		String home = prop.get("user.home").toString();
		String fs = prop.get("file.separator").toString();
		String path = home+fs+"AppData"+fs+"Roaming"+fs+"HelloGames"+fs+"NMS";
		return new File(path);
	}

	private void openSaveGame(File saveGameFile) {
		log("Parse file \"%s\" ...",saveGameFile.getPath());
		JSON_Object new_json_data = new JSON_Parser(saveGameFile).parse();
		log_ln(" done");
		if (new_json_data==null) {
			JOptionPane.showMessageDialog(mainWindow, "Can't parse selected file. It is not a valid JSON formated No Man's Sky savegame.", "Parse Error", JOptionPane.ERROR_MESSAGE);
		} else {
			SaveGameData saveGameData = new SaveGameData(new_json_data,saveGameFile.getName()).parse();
			SaveGameView saveGameView = new SaveGameView(mainWindow,saveGameFile,saveGameData);
			loadedSaveGames.add(saveGameView);
			contentPane.addSaveGameView(saveGameView);
			updateWindowTitle();
		}
		contentPane.disabler.setEnable(ActionCommand.Close    , contentPane.selectedSaveGameView!=null);
		contentPane.disabler.setEnable(ActionCommand.Reload   , contentPane.selectedSaveGameView!=null);
		contentPane.disabler.setEnable(ActionCommand.Compare  , loadedSaveGames.size()>1 && compareTab==null);
		contentPane.disabler.setEnable(ActionCommand.WriteHTML, contentPane.selectedSaveGameView!=null);
		contentPane.disabler.setEnable(ActionCommand.WriteJSON, contentPane.selectedSaveGameView!=null);
	}

	private void reloadSaveGameView(SaveGameView view) {
		File file = view.file;
		log_ln("");
		log("Parse file \"%s\" ...",file.getPath());
		JSON_Object new_json_data = new JSON_Parser(file).parse();
		log_ln(" done");
		if (new_json_data!=null) {
			removeUsages(view.data);
			SaveGameData saveGameData = new SaveGameData(new_json_data,file.getName()).parse();
			view.replaceData(saveGameData);
			contentPane.updateIDPanels();
		}
	}

	private void closeSaveGameView(SaveGameView view) {
		loadedSaveGames.remove(view);
		contentPane.removeSaveGameView(view);
		removeUsages(view.data);
		updateWindowTitle();
		
		if (loadedSaveGames.size()<2 && compareTab!=null) {
			contentPane.removeTab(compareTab);
			compareTab=null;
		}
	}
	
	private void removeUsages(SaveGameData oldData) {
		for (GeneralizedID id:GameInfos.productIDs  .getValues()) id.usage.remove(oldData);
		for (GeneralizedID id:GameInfos.techIDs     .getValues()) id.usage.remove(oldData);
		for (GeneralizedID id:GameInfos.substanceIDs.getValues()) id.usage.remove(oldData);
	}

	private void updateWindowTitle() {
		if (contentPane.selectedSaveGameView == null)
			mainWindow.setTitle("No Man's Sky - Viewer");
		else
			mainWindow.setTitle("No Man's Sky - Viewer - "+contentPane.selectedSaveGameView.file.getPath());
//		if (DEBUG) System.out.println("Set window title to \""+mainWindow.getTitle()+"\"");
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
			
			selectedSaveGameView = null;
			tabbedPane = new JTabbedPane();
			tabbedPane.setPreferredSize(new Dimension(1500, 800));
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
			toolBar.add(createButton("\"save.hg\"", ToolbarIcons.Open, ActionCommand.save_hg,true));
			toolBar.add(createButton("\"save2.hg\"", ToolbarIcons.Open, ActionCommand.save2_hg,true));
			toolBar.addSeparator();
			toolBar.add(createButton("Switch to NMS Savegame Folder", ToolbarIcons.SwitchFolder, ActionCommand.SwitchFolder,true));
			toolBar.add(createButton("Open Savegame", ToolbarIcons.Open  , ActionCommand.Open  ,true));
//			toolBar.add(createButton("Reload"       , ToolbarIcons.Reload, ActionCommand.Reload,false));
//			toolBar.add(createButton("Close"        , ToolbarIcons.Close , ActionCommand.Close ,false));
			toolBar.add(createButton("Compare Savegames", ToolbarIcons.Compare, ActionCommand.Compare,false));
			toolBar.addSeparator();
			toolBar.add(createButton("Write as HTML", ToolbarIcons.SaveAs, ActionCommand.WriteHTML,false));
			toolBar.add(createButton("Write as JSON", ToolbarIcons.SaveAs, ActionCommand.WriteJSON,false));
			toolBar.addSeparator();
			toolBar.add(createButton("Compute Coordinates", ToolbarIcons.ComputePortalGlyphs, ActionCommand.ComputeCoordinates,true));
//			toolBar.add(createButton("Select Coordinates", ToolbarIcons.ComputePortalGlyphs, ActionCommand.SelectCoordinates,true));
			toolBar.add(createButton("Refresh Extra Images", ToolbarIcons.Reload, ActionCommand.RefreshExtraImages,true));
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
			
			System.out.println("transferDataFlavors: "+toString(transferDataFlavors));
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

	public static void log_ln( String format, Object... values ) {
		System.out.printf(format+"\r\n",values);
	}
	
	public static void log( String format, Object... values ) {
		System.out.printf(format,values);
	}

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
		JMenuItem menuItem = new JMenuItem(title);
		menuItem.setEnabled(enabled);
		if (l!=null) menuItem.addActionListener(l);
		return menuItem;
	}

	public static JMenuItem createMenuItem(String title, ActionListener l) {
		return createMenuItem(title, l, true);
	}

	public static <AC extends Enum<AC>> JMenuItem createMenuItem(String title, ActionListener l, AC actionCommand) {
		JMenuItem menuItem = createMenuItem(title,l);
		menuItem.setActionCommand(actionCommand.toString());
		return menuItem;
	}

	public static JCheckBox createCheckbox(String title, ActionListener l, boolean isSelected) {
		JCheckBox button = new JCheckBox(title,isSelected);
		if (l!=null) button.addActionListener(l);
		return button;
	}
}
