package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.image.BufferedImage;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import net.schwarzbaer.gui.Disabler;
import net.schwarzbaer.gui.IconSource;
import net.schwarzbaer.gui.StandardDialog;
import net.schwarzbaer.gui.StandardDialog.Position;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Stats.StatValue.KnownID;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Galaxy;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Planet;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Region;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.SolarSystem;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.UniverseObject;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.UniverseObject.ExtraInfo;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.UniverseAddress;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveViewer.GeneralizedID.Usage;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.SaveGameView;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.ComboboxCellEditor;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.SimplifiedTable;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TreeView;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.UniversePanel;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.JSON_Object;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser;

public class SaveViewer implements ActionListener {

	private static final String FILE_KNOWN_STAT_ID = "NMS_Viewer.KnownStatID.txt";
	private static final String FILE_PRODUCT_ID    = "NMS_Viewer.ProdIDs.txt";
	private static final String FILE_TECH_ID       = "NMS_Viewer.TechIDs.txt";
	private static final String FILE_SUBSTANCE_ID  = "NMS_Viewer.SubstanceIDs.txt";
	private static final String FILE_UNIVERSE_OBJECT_DATA = "NMS_Viewer.UniverseObjects.txt";

	public static final boolean DEBUG = true;
	private static HashMap<Long,UniverseObjectData> universeObjectDataArr;
	
	public static final IDMap productIDs   = new IDMap();
	public static final IDMap techIDs      = new IDMap();
	public static final IDMap substanceIDs = new IDMap();
	
	private StandardMainWindow mainWindow;

	enum TabHeaderIcons { Close, Close_Inactive, Reload, Reload_Inactive }
	static IconSource<ToolbarIcons> toolbarIS;
	static IconSource<TabHeaderIcons> tabheaderIS;
	
	private JFileChooser inputFileChooser;
	private JFileChooser htmlFileChooser;
	private JFileChooser jsonFileChooser;
	private ContentPane contentPane;
	private ComparePanel compareTab;
	private Vector<SaveGameView> loadedSaveGames;
	
	public static void main(String[] args) {
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		
		Images.readImages();
		UniversePanel.prepareIconSources();
		
		tabheaderIS = new IconSource<TabHeaderIcons>(10,10){
			@Override protected int getIconIndexInImage(TabHeaderIcons key) { return key.ordinal(); }
		};
		tabheaderIS.readIconsFromResource("/images/TabHeader.png");
		
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
				}
			 	throw new IllegalArgumentException("Unknown icon key: "+key);
			}};
		toolbarIS.readIconsFromResource("/images/Toolbar.png");
		
		loadKnownStatIDsFromFile();
		loadProductIDsFromFile();
		loadTechIDsFromFile();
		loadSubstanceIDsFromFile();
		loadUniverseObjectDataFromFile();
		
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
	
	private enum ActionCommand {
		Open, Reload, Close, WriteHTML, WriteJSON, SwitchFolder, Compare, TabSelected, ComputeCoordinates, save_hg, save2_hg
	}

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
			new ComputeCoordinatesDialog(mainWindow).showDialog(Position.PARENT_CENTER);
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
			readUniverseObjectDataFromDataPool(saveGameData.universe);
			saveProductIDsToFile();
			saveTechIDsToFile();
			saveSubstanceIDsToFile();
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
			readUniverseObjectDataFromDataPool(saveGameData.universe);
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
		for (GeneralizedID id:productIDs  .getValues()) id.usage.remove(oldData);
		for (GeneralizedID id:techIDs     .getValues()) id.usage.remove(oldData);
		for (GeneralizedID id:substanceIDs.getValues()) id.usage.remove(oldData);
	}

	public static void log_ln( String format, Object... values ) {
		System.out.printf(format+"\r\n",values);
	}
	
	public static void log( String format, Object... values ) {
		System.out.printf(format,values);
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

		private GeneralizedIDPanel techIDsPanel;
		private GeneralizedIDPanel productIDsPanel;
		private GeneralizedIDPanel substanceIDsPanel;
		
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
			tabbedPane.addTab("Technology IDs", techIDsPanel      = new GeneralizedIDPanel(techIDs     , "TechnologyIDsTable"));
			tabbedPane.addTab("Product IDs"   , productIDsPanel   = new GeneralizedIDPanel(productIDs  , "ProductIDsTable"));
			tabbedPane.addTab("Substance IDs" , substanceIDsPanel = new GeneralizedIDPanel(substanceIDs, "SubstanceIDsTable"));
			
			tabbedPane.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					Component comp = tabbedPane.getSelectedComponent();
					if (comp instanceof SaveGameView) selectedSaveGameView = (SaveGameView)comp;
					else selectedSaveGameView = null;
					if (comp instanceof GeneralizedIDPanel) ((GeneralizedIDPanel)comp).updatePanel();
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
			toolBar.add(createButton("Load \"save.hg\"", ToolbarIcons.Open, ActionCommand.save_hg,true));
			toolBar.add(createButton("Load \"save2.hg\"", ToolbarIcons.Open, ActionCommand.save2_hg,true));
			toolBar.add(createButton("Switch to NMS Savegame Folder", ToolbarIcons.SwitchFolder, ActionCommand.SwitchFolder,true));
			toolBar.add(createButton("Open Savegame", ToolbarIcons.Open  , ActionCommand.Open  ,true));
//			toolBar.add(createButton("Reload"       , ToolbarIcons.Reload, ActionCommand.Reload,false));
//			toolBar.add(createButton("Close"        , ToolbarIcons.Close , ActionCommand.Close ,false));
			toolBar.add(createButton("Compare Savegames", ToolbarIcons.Compare, ActionCommand.Compare,false));
			toolBar.add(createButton("Write as HTML", ToolbarIcons.SaveAs, ActionCommand.WriteHTML,false));
			toolBar.add(createButton("Write as JSON", ToolbarIcons.SaveAs, ActionCommand.WriteJSON,false));
			toolBar.add(createButton("Compute Coordinates", ToolbarIcons.ComputePortalGlyphs, ActionCommand.ComputeCoordinates,true));
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

	enum ToolbarIcons { SwitchFolder, Open, SaveAs, Close, Reload, Compare, ComputePortalGlyphs }

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
	
	private static class ComputeCoordinatesDialog extends StandardDialog {
		private static final long serialVersionUID = -2899608237998750242L;
		
		private JLabel[] glyphLabels;
		private JTextArea statusField;
		
		private static abstract class AbstractInputPanel extends JPanel {
			private static final long serialVersionUID = -2301492858089122177L;
			
			protected ComputeCoordinatesDialog parent;
		
			AbstractInputPanel(ComputeCoordinatesDialog parent) {
				this.parent = parent;
				setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),BorderFactory.createEmptyBorder(3,3,3,3)));
			}
			
			JPanel createButtonPanel() {
				JPanel panel = new JPanel();
				panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),BorderFactory.createEmptyBorder(3,3,3,3)));
				JButton btnCompute = new JButton("Compute");
				btnCompute.addActionListener(e->computeUniverseAddress());
				panel.add(btnCompute);
				return panel;
			}
		
			protected abstract void computeUniverseAddress();
		}
		
		ComputeCoordinatesDialog(Window parent) {
			super(parent,"Compute Coordinates",ModalityType.APPLICATION_MODAL);
			
			GridBagLayout layout = new GridBagLayout();
			JPanel inputPanels = new JPanel(layout);
			addInputPanel(inputPanels, layout, new InputAsCoords(this));
			addInputPanel(inputPanels, layout, new InputAsSigBoostCode(this));
			addInputPanel(inputPanels, layout, new InputAsPortalGlyphCode(this));
			addInputPanel(inputPanels, layout, new InputAsAddress(this));
			
			JPanel portalGlyphPanel = new JPanel(new GridLayout(1, 12, 3,3));
			portalGlyphPanel.setBorder(BorderFactory.createEtchedBorder());
			glyphLabels = new JLabel[12];
			Dimension preferredSize = new Dimension(50,50);
			for (int i=0; i<glyphLabels.length; ++i) {
				glyphLabels[i] = new JLabel();
				glyphLabels[i].setPreferredSize(preferredSize);
				portalGlyphPanel.add(glyphLabels[i]);
			}
			
			statusField = new JTextArea();
			statusField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),BorderFactory.createEmptyBorder(3,3,3,3)));
			statusField.setPreferredSize(new Dimension(300,20));
			statusField.setEditable(false);
			
			JPanel contentPane = new JPanel(new BorderLayout(3,3));
			contentPane.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
			contentPane.add(inputPanels,BorderLayout.WEST);
			contentPane.add(statusField,BorderLayout.CENTER);
			contentPane.add(portalGlyphPanel,BorderLayout.SOUTH);
			
			super.createGUI( contentPane );
			super.setSizeAsMinSize();
//			super.setResizable(false);
		}

		private void addInputPanel(JPanel inputPanels, GridBagLayout layout, AbstractInputPanel inputPanel) {
			GridBagConstraints c = new GridBagConstraints();
			
			c.weightx=1;
			c.gridwidth=1;
			c.fill = GridBagConstraints.BOTH;
			layout.setConstraints(inputPanel, c);
			inputPanels.add(inputPanel);
			
			JPanel buttonPanel = inputPanel.createButtonPanel();
			c.weightx=0;
			c.gridwidth=GridBagConstraints.REMAINDER;
			c.fill = GridBagConstraints.BOTH;
			layout.setConstraints(buttonPanel, c);
			inputPanels.add(buttonPanel);
		}

		private void showUniverseAddress(UniverseAddress ua) {
			long portalGlyphCode = ua.getPortalGlyphCode();
			
			for (int i=11; i>=0; --i) {
				int nr = (int) (portalGlyphCode&0xF);
				portalGlyphCode = portalGlyphCode>>4;
				BufferedImage image = UniversePanel.PortalGlyphsIS.getCachedImage(nr);
				ImageIcon icon = image==null?null:new ImageIcon( image );
				glyphLabels[i].setIcon(icon);
			}
			
			statusField.setText(ua.getCoordinates());
			statusField.append("\r\n"+ua.getExtendedSigBoostCode());
			statusField.append("\r\n"+ua.getPortalGlyphCodeStr());
			statusField.append("\r\n"+ua.getAddressStr());
		}

		private void showError(String message) {
			statusField.setText(message);
		}
		
		private static class InputField extends JTextField {
			private static final long serialVersionUID = -4256186100798813519L;
			
			final static private Color bgcolor = UIManager.getLookAndFeelDefaults().getColor("TextField.background");
			final static private FocusListener bgResetter = new FocusListener() {
				@Override public void focusLost(FocusEvent e) {}
				@Override public void focusGained(FocusEvent e) {
					Component comp = e.getComponent();
					if (comp!=null) comp.setBackground(bgcolor);
				}
			};
			
			InputField(int prefWidth, int prefHeight) {
				addFocusListener(bgResetter);
				setPreferredSize(new Dimension(prefWidth,prefHeight));
			}

			private void getValue(TextFieldValue result, boolean forceHex) {
				String str = getText();
				if (forceHex) {
					try { result.parseHex(str); result.valueWasHex = true; }
					catch (NumberFormatException e) { result.parseError = true; }
				} else
				if (str.startsWith("0x")) {
					try { result.parseHex(str.substring("0x".length())); result.valueWasHex = true; }
					catch (NumberFormatException e) { result.parseError = true; }
				} else {
					try { result.parseDec(str); result.valueWasHex = false; }
					catch (NumberFormatException e) { result.parseError = true; }
				}
			}

			TextFieldValueLong getLongValue() { TextFieldValueLong result = new TextFieldValueLong(); getValue(result,false); return result; }
			TextFieldValueInt  getIntValue () { TextFieldValueInt  result = new TextFieldValueInt (); getValue(result,false); return result; }
			TextFieldValueLong getLongHexValue() { TextFieldValueLong result = new TextFieldValueLong(); getValue(result,true); return result; }
			@SuppressWarnings("unused")
			TextFieldValueInt  getIntHexValue () { TextFieldValueInt  result = new TextFieldValueInt (); getValue(result,true); return result; }

			void setError() {
				setBackground(Color.RED);
			}
		}

		private static abstract class TextFieldValue {
			boolean valueWasHex;
			boolean parseError;
			TextFieldValue() {
				valueWasHex = false;
				parseError = false;
			}
			abstract void parseHex(String str) throws NumberFormatException;
			abstract void parseDec(String str) throws NumberFormatException;
		}

		private static class TextFieldValueLong extends TextFieldValue {
			long value;
			TextFieldValueLong() { value = 0L; }
			@Override void parseHex(String str) throws NumberFormatException { value = Long.parseLong(str,16); }
			@Override void parseDec(String str) throws NumberFormatException { value = Long.parseLong(str); }
		}

		private static class TextFieldValueInt extends TextFieldValue {
			int value;
			TextFieldValueInt() { value = 0; }
			@Override void parseHex(String str) throws NumberFormatException { value = Integer.parseInt(str,16); }
			@Override void parseDec(String str) throws NumberFormatException { value = Integer.parseInt(str); }
		}

		private static class InputAsPortalGlyphCode extends AbstractInputPanel {
			private static final long serialVersionUID = -5005085383637770875L;
			private InputField galaxyIndex;
			private InputField portalGlyphCode;

			InputAsPortalGlyphCode(ComputeCoordinatesDialog parent) {
				super(parent);
				add(new JLabel("Galaxy:"));
				add(galaxyIndex = new InputField(35,20));
				add(new JLabel("PortalGlyphCode:"));
				add(portalGlyphCode = new InputField(200,20));
			}

			@Override
			protected void computeUniverseAddress() {
				TextFieldValueInt  galaxyIndexValue     = galaxyIndex    .getIntValue();
				TextFieldValueLong portalGlyphCodeValue = portalGlyphCode.getLongHexValue();
				boolean error = false;
				if (galaxyIndexValue    .parseError) { galaxyIndex    .setError(); error = true; }
				if (portalGlyphCodeValue.parseError) { portalGlyphCode.setError(); error = true; }
				if (error) {
					parent.showError("Wrong Input");
				} else {
					long region = portalGlyphCodeValue.value & 0xFFFFFFFFL;
					long plnsys = (portalGlyphCodeValue.value>>32) & 0xFFFF;
					UniverseAddress ua = new UniverseAddress( (plnsys<<40) | (((long)galaxyIndexValue.value&0xFF)<<32) | region);
					parent.showUniverseAddress(ua);
				}
			}
		}

		private static class InputAsAddress extends AbstractInputPanel {
			private static final long serialVersionUID = 5365096410288633609L;
			private InputField universeAddress;

			InputAsAddress(ComputeCoordinatesDialog parent) {
				super(parent);
				add(new JLabel("Universe Address:"));
				add(universeAddress = new InputField(200,20));
			}

			@Override
			protected void computeUniverseAddress() {
				TextFieldValueLong universeAddressValue = universeAddress.getLongValue();
				if (universeAddressValue.parseError) { universeAddress.setError(); parent.showError("Wrong Input"); return; }
				UniverseAddress ua = new UniverseAddress(universeAddressValue.value);
				parent.showUniverseAddress(ua);
			}
		}

		private static class InputAsSigBoostCode extends AbstractInputPanel {
			private static final long serialVersionUID = -108501123032087816L;
			private InputField galaxyIndex;
			private InputField sigBoostCode;
			private InputField planetIndex;

			InputAsSigBoostCode(ComputeCoordinatesDialog parent) {
				super(parent);
				//setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
				add(new JLabel("Galaxy:"));
				add(galaxyIndex = new InputField(35,20));
				add(new JLabel("  Signal Booster Code:"));
				add(sigBoostCode = new InputField(150,20));
				add(new JLabel("  Planet:"));
				add(planetIndex = new InputField(35,20));
			}

			@Override
			protected void computeUniverseAddress() {
				TextFieldValueInt galaxyIndexValue = galaxyIndex .getIntValue();
				String            sigBoostCodeStr  = sigBoostCode.getText();
				TextFieldValueInt planetIndexValue = planetIndex .getIntValue();
				
				boolean error = false;
				if (planetIndexValue.parseError) { planetIndex.setError(); error = true; }
				if (galaxyIndexValue.parseError) { galaxyIndex.setError(); error = true; }
				
				boolean sigBoostCodeError = false;
				if (sigBoostCodeStr.length()<19) { sigBoostCode.setError(); sigBoostCodeError = true; }
				
				String[] strings = null;
				if (!sigBoostCodeError) {
					strings = sigBoostCodeStr.split(":");
					if (strings.length!=4) { sigBoostCode.setError(); sigBoostCodeError = true; }
				}
				
				long[] values = null;
				if (!sigBoostCodeError) {
					values = new long[strings.length];
					for (int i=0; i<strings.length; ++i)
						try { values[i] =Long.parseLong( strings[i], 16 ); }
						catch (NumberFormatException e) {
							sigBoostCode.setError(); sigBoostCodeError = true;
						}
				}
				
				if (error || sigBoostCodeError) {
					parent.showError("Wrong Input");
				} else {
					int voxelX = (int) (values[0]-2047);
					int voxelY = (int) (values[1]-127);
					int voxelZ = (int) (values[2]-2047);
					int solarSystemIndex = (int) values[3];
					
					UniverseAddress ua = new UniverseAddress(galaxyIndexValue.value, voxelX, voxelY, voxelZ, solarSystemIndex, planetIndexValue.value);
					parent.showUniverseAddress(ua);
				}
			}
		}

		private static class InputAsCoords extends AbstractInputPanel {
			private static final long serialVersionUID = 5940223603403241578L;
			
			private InputField galaxyIndex;
			private InputField regionVoxelX;
			private InputField regionVoxelY;
			private InputField regionVoxelZ;
			private InputField solarSystemIndex;
			private InputField planetIndex;
			
			InputAsCoords(ComputeCoordinatesDialog parent) {
				super(parent);
				//setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
				add(new JLabel("Galaxy:"));
				add(galaxyIndex = new InputField(35,20));
				add(new JLabel("  Region:"));
				add(regionVoxelX = new InputField(50,20));
				add(regionVoxelY = new InputField(40,20));
				add(regionVoxelZ = new InputField(50,20));
				add(new JLabel("  Solar System:"));
				add(solarSystemIndex = new InputField(50,20));
				add(new JLabel("  Planet:"));
				add(planetIndex = new InputField(35,20));
			}
		
			@Override
			protected void computeUniverseAddress() {
				TextFieldValueInt planetIndexValue      = planetIndex     .getIntValue();
				TextFieldValueInt solarSystemIndexValue = solarSystemIndex.getIntValue();
				TextFieldValueInt regionVoxelXValue     = regionVoxelX    .getIntValue();
				TextFieldValueInt regionVoxelYValue     = regionVoxelY    .getIntValue();
				TextFieldValueInt regionVoxelZValue     = regionVoxelZ    .getIntValue();
				TextFieldValueInt galaxyIndexValue      = galaxyIndex     .getIntValue();
				
				boolean error = false;
				if (planetIndexValue     .parseError) { planetIndex     .setError(); error = true; }
				if (solarSystemIndexValue.parseError) { solarSystemIndex.setError(); error = true; }
				if (regionVoxelXValue    .parseError) { regionVoxelX    .setError(); error = true; }
				if (regionVoxelYValue    .parseError) { regionVoxelY    .setError(); error = true; }
				if (regionVoxelZValue    .parseError) { regionVoxelZ    .setError(); error = true; }
				if (galaxyIndexValue     .parseError) { galaxyIndex     .setError(); error = true; }
				
				if (error) {
					parent.showError("Wrong Input");
				} else {
					int voxelX = regionVoxelXValue.value;
					int voxelY = regionVoxelYValue.value;
					int voxelZ = regionVoxelZValue.value;
					if (regionVoxelXValue.valueWasHex) { if (voxelX>0x7FF) voxelX |= 0xFFFFF000; }
					if (regionVoxelYValue.valueWasHex) { if (voxelY> 0x7F) voxelY |= 0xFFFFFF00; }
					if (regionVoxelZValue.valueWasHex) { if (voxelZ>0x7FF) voxelZ |= 0xFFFFF000; }
					
					UniverseAddress ua = new UniverseAddress(galaxyIndexValue.value, voxelX, voxelY, voxelZ, solarSystemIndexValue.value, planetIndexValue.value);
					parent.showUniverseAddress(ua);
				}
			}
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
				try { knownID = KnownID.valueOf(str.substring(0, pos)); }
				catch (Exception e) { knownID = null; }
				if (knownID == null) continue;
				
				String fullName = str.substring(pos+1);
				if (!fullName.equals(knownID.fullName)) {
					System.out.printf("Changed StatValue.KnownID.fullName found: %16s [old]\"%s\" -> [new]\"%s\"\r\n",knownID,knownID.fullName,fullName);
					knownID.fullName = fullName;
				}
			}
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
		System.out.println();
	}

	public static void saveKnownStatIDsToFile() {
		KnownID[] knownIDs = KnownID.values();
		
		File file = new File(FILE_KNOWN_STAT_ID);
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8));) {
			for (KnownID id:knownIDs)
				out.printf("%s=%s\r\n",id.toString(),id.fullName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void loadProductIDsFromFile  () { loadIDsFromFile(FILE_PRODUCT_ID  ,productIDs  ,"product"   ); }
	public static void loadTechIDsFromFile     () { loadIDsFromFile(FILE_TECH_ID     ,techIDs     ,"technology"); }
	public static void loadSubstanceIDsFromFile() { loadIDsFromFile(FILE_SUBSTANCE_ID,substanceIDs,"substance" ); }

	private static void loadIDsFromFile(String filePath, IDMap map, String idLabel) {
		File file = new File(filePath);
		if (!file.isFile()) return;
		
		long start = System.currentTimeMillis();
		System.out.println("Read "+idLabel+" IDs from file \""+filePath+"\"...");
		String str;
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8))) {
			while ((str=in.readLine())!=null) {
				int pos = str.indexOf('=');
				if (pos<0) continue;
				
				String idStr = str.substring(0, pos);
				String value = str.substring(pos+1);
				
				if (idStr.endsWith(".image")) {
					idStr = idStr.substring(0, idStr.length()-".image".length());
					GeneralizedID id = map.get(idStr);
					id.setImageFileName(value);
				} else if (idStr.endsWith(".imageBG")) {
					idStr = idStr.substring(0, idStr.length()-".imageBG".length());
					GeneralizedID id = map.get(idStr);
					try { id.setImageBG(Integer.parseInt(value,16)); }
					catch (NumberFormatException e) {}
				} else {
					GeneralizedID id = map.get(idStr);
					id.label = value;
				}
			}
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
		
		System.out.println("done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}

	public static void saveProductIDsToFile  () { saveIDsToFile(FILE_PRODUCT_ID  ,productIDs  ,"product"    ); }
	public static void saveTechIDsToFile     () { saveIDsToFile(FILE_TECH_ID     ,techIDs     ,"technologie"); }
	public static void saveSubstanceIDsToFile() { saveIDsToFile(FILE_SUBSTANCE_ID,substanceIDs,"substance"  ); }

	public static void saveIDsToFile(String filePath, IDMap map, String idLabel) {
		long start = System.currentTimeMillis();
		System.out.println("Write "+idLabel+" IDs to file \""+filePath+"\"...");
		
		File file = new File(filePath);
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8));) {
			for (String idStr:map.getSortedKeys()) {
				GeneralizedID id = map.get(idStr);
				out.printf("%s=%s\r\n",idStr,id.label);
				if (id.hasImage  ()) out.printf("%s.image=%s\r\n"    ,idStr,id.getImageFileName());
				if (id.hasImageBG()) out.printf("%s.imageBG=%06X\r\n",idStr,id.getImageBG());
			}
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		
		System.out.println("done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}
	
	public static void loadUniverseObjectDataFromFile() {
		long start = System.currentTimeMillis();
		System.out.println("Read data of universe objects from file \""+FILE_UNIVERSE_OBJECT_DATA+"\"...");
		universeObjectDataArr = new HashMap<>();
		
		File file = new File(FILE_UNIVERSE_OBJECT_DATA);
		if (!file.isFile()) {
			System.out.println("done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
			return;
		}
		
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8))) {
			String str;
			UniverseObjectData uoData = null;
			
			String nextShortLabel = null;
			boolean showInParent = false;
			while ((str=in.readLine())!=null) {
				if (str.isEmpty()) continue;
				if ((str.startsWith("[Sys") || str.startsWith("[Pln")) && str.endsWith("]")) {
					uoData = null;
					String addressStr = str.substring("[Sys".length(), str.length()-"]".length());
					long address;
					try { address = Long.parseLong(addressStr, 16); }
					catch (NumberFormatException e) {
						System.err.printf("Can't parse universe address in: \"%s\"\r\n",str);
						continue;
					}
					UniverseAddress ua = new UniverseAddress(address);
					if (str.startsWith("[Sys") && ua.isSolarSystem()) uoData = new UniverseObjectData(ua,UniverseObjectData.Type.SolarSystem);
					if (str.startsWith("[Pln") && ua.isPlanet     ()) uoData = new UniverseObjectData(ua,UniverseObjectData.Type.Planet);
					if (uoData != null) universeObjectDataArr.put(address, uoData);
					continue;
				}
				if (str.startsWith("name=")) {
					if (uoData!=null) uoData.name = str.substring("name=".length());
					continue;
				}
				if (str.startsWith("race=")) {
					if (uoData==null) continue;
					String race = str.substring("race=".length());
					try { uoData.race = Universe.SolarSystem.Race.valueOf(race); }
					catch (Exception e) { uoData.race = null; }
					continue;
				}
				if (str.startsWith("class=")) {
					if (uoData==null) continue;
					String starClass = str.substring("class=".length());
					try { uoData.starClass = Universe.SolarSystem.StarClass.valueOf(starClass); }
					catch (Exception e) { uoData.starClass = null; }
					continue;
				}
				if (str.startsWith("distance=")) {
					if (uoData==null) continue;
					String distance = str.substring("distance=".length());
					try { uoData.distanceToCenter = Double.parseDouble(distance); }
					catch (NumberFormatException e) { uoData.distanceToCenter = null; }
					continue;
				}
				if (str.startsWith("short=")) {
					nextShortLabel = str.substring("short=".length());
					showInParent = false;
					continue;
				}
				if (str.startsWith("short.P=")) {
					nextShortLabel = str.substring("short.P=".length());
					showInParent = true;
					continue;
				}
				if (str.startsWith("info=")) {
					String info = str.substring("info=".length());
					if (nextShortLabel!=null && uoData!=null)
						uoData.extraInfos.add(new ExtraInfo(showInParent,nextShortLabel,info));
					nextShortLabel=null;
					continue;
				}
			}
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
		System.out.println("done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}
	
	public static void readUniverseObjectDataFromDataPool(Universe universe) {
		long start = System.currentTimeMillis();
		System.out.println("Read data of universe objects from data pool ... ");
		
		boolean withOutput = false; // DEBUG;
		
		SolarSystem system = null;
		Planet planet = null;
		UniverseObject uniObj = null;
		String uniObjName = null;
		for (Long address:universeObjectDataArr.keySet()) {
			UniverseObjectData uoData = universeObjectDataArr.get(address);
			UniverseAddress ua = new UniverseAddress(address);
			
			system = null;
			planet = null;
			
			if (uoData.type==UniverseObjectData.Type.SolarSystem && ua.isSolarSystem()) system = universe.findSolarSystem(ua);
			if (uoData.type==UniverseObjectData.Type.Planet      && ua.isPlanet     ()) planet = universe.findPlanet     (ua);
			if (system!=null) { uniObj = system; uniObjName = "solar system "+ua.getSigBoostCode();   if (withOutput) System.out.printf("Solar system %s\r\n",ua.getSigBoostCode());   }
			if (planet!=null) { uniObj = planet; uniObjName = "planet "+ua.getExtendedSigBoostCode(); if (withOutput) System.out.printf("Planet %s\r\n",ua.getExtendedSigBoostCode()); }
			
			if (uoData.name!=null && uniObj!=null) {
				uniObj.setOriginalName(uoData.name);
				if (withOutput) System.out.printf("   Name of %s was defined: \"%s\"\r\n",uniObjName,uoData.name);
			}
			
			if (uoData.race!=null && system!=null) {
				system.race = uoData.race;
				if (withOutput) System.out.printf("   Race of %s was defined: %s\r\n",uniObjName,system.race);
			}
			
			if (uoData.starClass!=null && system!=null) {
				system.starClass = uoData.starClass;
				if (withOutput) System.out.printf("   StarClass of %s was defined: %s\r\n",uniObjName,system.starClass);
			}
			
			if (uoData.distanceToCenter!=null && system!=null) {
				system.distanceToCenter = uoData.distanceToCenter;
				if (withOutput) System.out.printf("   Distance to galactic center of %s was defined: %s\r\n",uniObjName,system.distanceToCenter);
			}
			
			for (ExtraInfo ei:uoData.extraInfos) {
				if (uniObj!=null) {
					uniObj.extraInfos.add(ei);
					if (withOutput) System.out.printf("   Info of %s was defined: ( \"%s\", \"%s\" )\r\n",uniObjName,ei.shortLabel,ei.info);
				}
			}
			
		}
		if (!withOutput) System.out.println("done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}

	public static void saveUniverseObjectDataToFile(Universe universe) {
		long start = System.currentTimeMillis();
		System.out.print("Write data of universe objects to data pool ...");
		for (Galaxy g:universe.galaxies)
			for (Region gr:g.regions)
				for (SolarSystem sys:gr.solarSystems) {
					UniverseAddress ua = sys.getUniverseAddress();
					universeObjectDataArr.put(ua.getAddress(),new UniverseObjectData(ua,sys));
					for (Planet p:sys.planets) {
						ua = p.getUniverseAddress();
						universeObjectDataArr.put(ua.getAddress(),new UniverseObjectData(ua,p));
					}
				}
		System.out.println("done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
		
		start = System.currentTimeMillis();
		Vector<UniverseObjectData> values = new Vector<>(universeObjectDataArr.values());
		Collections.sort(values);
		
		System.out.print("Write data pool to file \""+FILE_UNIVERSE_OBJECT_DATA+"\" ...");
		File file = new File(FILE_UNIVERSE_OBJECT_DATA);
		boolean isFirst = true;
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8));) {
			for (UniverseObjectData uoData:values) {
				if (!uoData.hasData()) continue;
				
				if (!isFirst) out.println();
				isFirst = false;
				
				switch (uoData.type) {
				case SolarSystem: out.printf("[Sys%014X]\r\n",uoData.universeAddress.getAddress()); break;
				case Planet     : out.printf("[Pln%014X]\r\n",uoData.universeAddress.getAddress()); break;
				}
				
				if (uoData.name!=null) out.printf("name=%s\r\n",uoData.name);
				if (uoData.type==UniverseObjectData.Type.SolarSystem) {
					if (uoData.race!=null            ) out.printf("race=%s\r\n",uoData.race);
					if (uoData.starClass!=null       ) out.printf("class=%s\r\n",uoData.starClass);
					if (uoData.distanceToCenter!=null) out.printf(Locale.ENGLISH,"distance=%f\r\n",uoData.distanceToCenter.doubleValue());
				}
				
				for (ExtraInfo ei:uoData.extraInfos)
					if (!ei.shortLabel.isEmpty() || !ei.info.isEmpty()) {
						String showInParentStr="";
						if (uoData.type==UniverseObjectData.Type.Planet && ei.showInParent) showInParentStr = ".P";
						out.printf("short%s=%s\r\n", showInParentStr, ei.shortLabel);
						out.printf("info=%s\r\n" , ei.info);
					}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		System.out.println("done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}
	
	private static class UniverseObjectData implements Comparable<UniverseObjectData>{
		enum Type { SolarSystem,Planet }
		
		final UniverseAddress universeAddress;
		final Type type;
		String name;
		Universe.SolarSystem.Race race;
		Universe.SolarSystem.StarClass starClass; 
		Double distanceToCenter;
		Vector<ExtraInfo> extraInfos;
		
		public UniverseObjectData(UniverseAddress universeAddress, Type type) {
			this.universeAddress = universeAddress;
			this.type = type;
			this.name = null;
			this.race = null;
			this.starClass = null;
			this.distanceToCenter = null;
			this.extraInfos = new Vector<>();
		}

		public boolean hasData() {
			return name!=null || race!=null || starClass!=null || distanceToCenter!=null || !extraInfos.isEmpty();
		}

		public UniverseObjectData(UniverseAddress universeAddress, SolarSystem sys) {
			this(universeAddress,Type.SolarSystem,sys);
			race = sys.race;
			starClass = sys.starClass;
			distanceToCenter = sys.distanceToCenter;
		}
		
		public UniverseObjectData(UniverseAddress universeAddress, Planet planet) {
			this(universeAddress,Type.Planet,planet);
		}
		
		public UniverseObjectData(UniverseAddress universeAddress, Type type, UniverseObject uo) {
			this(universeAddress,type);
			name = uo.getOriginalName();
			for (ExtraInfo ei:uo.extraInfos)
				extraInfos.add(new ExtraInfo(ei));
		}

		@Override
		public int compareTo(UniverseObjectData other) {
			if (other==null) return -1;
			return universeAddress.compareTo(other.universeAddress);
		}
		
		
	}
	
	public static class GeneralizedID {
		
		public final String id;
		public String label;
		private String imageFileName;
		private Integer imageBackground;
		private final HashMap<SaveGameData,Usage> usage;
		private BufferedImage cachedImage;
		
		private GeneralizedID(String id, String label) {
			this.id = id;
			this.label = label;
			this.usage = new HashMap<>();
			this.imageFileName = null;
			this.imageBackground = null;
			this.cachedImage = null;
		}
		public GeneralizedID(String id) {
			this(id,"");
		}
		public GeneralizedID(GeneralizedID other) {
			this.id = other.id;
			this.label = other.label;
			this.usage = new HashMap<SaveGameData,Usage>(other.usage);
			this.imageFileName = other.imageFileName;
			this.imageBackground = other.imageBackground;
			this.cachedImage = other.cachedImage;
		}
		
		public boolean hasImage() { return imageFileName!=null; }
		public String getImageFileName() { return imageFileName==null?"":imageFileName; }
		public void setImageFileName(String fileName) {
			if (fileName!=null && fileName.isEmpty()) fileName = null;
			this.imageFileName = fileName;
			cachedImage = null;
		}
		public void setImageFileName(Object aValue) {
			setImageFileName(aValue==null?null:aValue.toString());
		}
		
		public boolean hasImageBG() { return imageBackground!=null; }
		public Integer getImageBG() { return imageBackground; }
		public void setImageBG(Integer color) {
			this.imageBackground = color;
			cachedImage = null;
		}
		
		public BufferedImage getCachedImage() {
			if (cachedImage == null) cachedImage = getImage();
			return cachedImage;
		}
		
		public BufferedImage getImage() {
			return getImage(-1,-1);
		}
		
		public BufferedImage getCachedImage(int width, int height) {
			if (cachedImage==null || cachedImage.getWidth()!=width || cachedImage.getHeight()!=height) cachedImage = getImage(width, height);
			return cachedImage;
		}
		
		public BufferedImage getImage(int width, int height) {
			BufferedImage newImage = null;
			
			if (imageFileName!=null)
				newImage = Images.images.get(imageFileName);
			
			if (imageBackground!=null) {
				if (width <0) width  = (newImage==null)?256:newImage.getWidth();
				if (height<0) height = (newImage==null)?256:newImage.getHeight();
				BufferedImage combinedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				Graphics g = combinedImage.getGraphics();
				g.setColor(new Color(imageBackground));
				g.fillRect(0,0,width,height);
				if (newImage!=null) {
					if (width==newImage.getWidth() && height==newImage.getHeight())
						g.drawImage(newImage,0,0,null);
					else {
						if (g instanceof Graphics2D) {
							Graphics2D g2 = (Graphics2D)g;
							g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
							g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
							g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
							g2.drawImage(newImage,0,0,width,height,null);
						} else
							g.drawImage(newImage,0,0,width,height,null);
					}
				}
				newImage = combinedImage;
			}
			
			return newImage;
		}
		
		public Usage getUsage(SaveGameData base) {
			Usage newUsage = new Usage();
			Usage oldUsage = usage.putIfAbsent(base, newUsage);
			return oldUsage==null?newUsage:oldUsage;
		}

		public class Usage {
			
			public final Vector<String> inventoryUsages;
			public final Vector<String> blueprintUsages;
			
			public Usage() {
				inventoryUsages = new Vector<>();
				blueprintUsages = new Vector<>();
			}
			
			public void addInventoryUsage(String label, int x, int y) {
				inventoryUsages.add(String.format("%s @ (%d,%d)", label, x, y));
			}

			public void addBlueprintUsage(String label, int i) {
				blueprintUsages.add(label+" Blueprint "+i);
			}

			public boolean isEmpty() {
				return inventoryUsages.isEmpty() && blueprintUsages.isEmpty();
			}
		}
	}
	
	public static class IDMap {
		private HashMap<String, GeneralizedID> map;
		
		public IDMap() {
			map = new HashMap<>();
		}

		public Iterable<GeneralizedID> getValues() {
			return new Iterable<GeneralizedID>() {
				@Override public Iterator<GeneralizedID> iterator() { return map.values().iterator(); }
			}; 
		}

		public Vector<GeneralizedID> getSortedValues() {
			Vector<GeneralizedID> vector = new Vector<GeneralizedID>(map.values());
			vector.sort(Comparator.comparing(id->id.id));
			return vector;
		}

		public Iterable<String> getSortedKeys() {
			Vector<String> vector = new Vector<String>(map.keySet());
			vector.sort(null);
			return new Iterable<String>() {
				@Override public Iterator<String> iterator() { return vector.iterator(); }
			}; 
		}

		public void set(String id, GeneralizedID generalizedID) {
			map.put(id, generalizedID);
		}

		public GeneralizedID get(String id) {
			GeneralizedID newID = new GeneralizedID(id);
			GeneralizedID existingID = map.putIfAbsent(id, newID);
			return existingID==null?newID:existingID;
		}
	}
	
	private static class GeneralizedIDPanel extends JPanel {
		private static final long serialVersionUID = -4946966056212175920L;

		private SimplifiedTable table;
		private GeneralizedIDTableModel tableModel;

		private JTextArea textarea;
		private JLabel imageField;

		public GeneralizedIDPanel(IDMap idMap, String tableLabel) {
			super(new BorderLayout(3, 3));
			setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			
			tableModel = new GeneralizedIDTableModel(this,idMap);
			table = new SimplifiedTable(tableLabel,tableModel,true,SaveViewer.DEBUG,true);
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			table.getSelectionModel().addListSelectionListener(e->showID(tableModel.getValue(table.getRowSorter().convertRowIndexToModel(table.getSelectedRow()))));
			prepareTable();
			
			add(new JScrollPane(table),BorderLayout.CENTER);
			
			textarea = new JTextArea();
			textarea.setEditable(false);
			JScrollPane textareaScrollPane = new JScrollPane(textarea);
			textareaScrollPane.getViewport().setPreferredSize(new Dimension(400, 150));
			
			imageField = new JLabel();
			imageField.setBorder(BorderFactory.createEtchedBorder());
			imageField.setPreferredSize(new Dimension(100,100));
			
			JPanel eastPanel = new JPanel(new BorderLayout(3, 3));
			eastPanel.add(imageField, BorderLayout.NORTH);
			eastPanel.add(textareaScrollPane, BorderLayout.CENTER);
			
			
			add(eastPanel, BorderLayout.EAST);
			
		}

		private void prepareTable() {
			Vector<String> images = new Vector<>(Arrays.asList(Images.imagesNames));
			images.insertElementAt("",0);
			setCellEditor(GeneralizedIDColumnID.Image, new TableView.ComboboxCellEditor<String>(images.toArray(new String[0])));
			
			Vector<Integer> colors = new Vector<>(Arrays.asList(Images.colors));
			colors.insertElementAt(null,0);
			ComboboxCellEditor<Integer> colorCellEditor = new TableView.ComboboxCellEditor<Integer>(colors.toArray(new Integer[0]));
			
			TableView.ColorRenderer colorRenderer = new TableView.ColorRenderer();
			colorCellEditor.setRenderer(colorRenderer);
			setCellEditor  (GeneralizedIDColumnID.ImgBG, colorCellEditor);
			setCellRenderer(GeneralizedIDColumnID.ImgBG, colorRenderer);
		}

		private void setCellRenderer(GeneralizedIDColumnID columnID, TableCellRenderer cellRenderer) {
			TableColumn column = getColumn(columnID);
			if (column==null) return;
			column.setCellRenderer(cellRenderer);
		}

		private void setCellEditor(GeneralizedIDColumnID columnID, TableCellEditor cellEditor) {
			TableColumn column = getColumn(columnID);
			if (column==null) return;
			column.setCellEditor(cellEditor);
		}

		private TableColumn getColumn(GeneralizedIDColumnID columnID) {
			TableColumnModel columnModel = table.getColumnModel();
			if (columnModel==null) return null;
			
			int columnIndex = tableModel.getColumn(columnID);
			if (columnIndex<0) return null;
			
			return columnModel.getColumn(columnIndex);
		}

		private void showID(GeneralizedID id) {
			textarea.setText("");
			if (id==null) {
				imageField.setIcon(null);
				return;
			}
			
			textarea.append("ID     : "+id.id+"\r\n");
			if (!id.label.isEmpty()) textarea.append("Label  : "+id.label+"\r\n");
			if (id.hasImage  ()    ) textarea.append("Image  : "+id.getImageFileName()+"\r\n");
			if (id.hasImageBG()    ) textarea.append("ImageBG: "+String.format("%06X",id.getImageBG())+"\r\n");
			
			BufferedImage image = id.getImage();
			if (image==null) {
				imageField.setIcon(null);
			} else {
				imageField.setIcon(new ImageIcon(image));
				imageField.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));							
			}
			
			for (SaveGameData key:id.usage.keySet()) {
				textarea.append("\r\n");
				textarea.append(key.filename+":\r\n");
				Usage usages = id.usage.get(key);
				if (usages.isEmpty())
					textarea.append("   none\r\n");
				for (String str:usages.inventoryUsages) textarea.append("   "+str+"\r\n");
				for (String str:usages.blueprintUsages) textarea.append("   "+str+"\r\n");
			}
		}

		public void addUsage(SaveGameView view) {
			stopCellEditing();
			tableModel.addUsage(view);
			tableModel.setColumnWidths(table);
			prepareTable();
		}

		public void updatePanel() {
			stopCellEditing();
			tableModel.updateData();
		}

		public void removeUsage(SaveGameView view) {
			stopCellEditing();
			tableModel.removeUsage(view);
			tableModel.setColumnWidths(table);
			prepareTable();
		}

		private void stopCellEditing() {
			TableCellEditor cellEditor = table.getCellEditor();
			if (cellEditor!=null) cellEditor.stopCellEditing();
		}

		private enum GeneralizedIDColumnID implements TableView.SimplifiedColumnIDInterface {
			ID    ("ID"        ,  String.class,  80,-1,120,120),
			Label ("Label"     ,  String.class, 150,-1,200,200),
			Image ("Image"     ,  String.class, 150,-1,250,250),
			ImgBG ("Background", Integer.class,  50,-1, 70, 70),
			Usage (""          ,  String.class,  80,-1, 80, 80);
			
			private TableView.SimplifiedColumnConfig columnConfig;
			
			GeneralizedIDColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				columnConfig = new TableView.SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			@Override public TableView.SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
		}
	
		private static class GeneralizedIDTableModel extends TableView.SimplifiedTableModel<GeneralizedIDColumnID> {
	
			private static final int EXTRA_ROWS = 1;
			
			private int numberOfLabledIDs;
			private IDMap sourceIdMap;

			private Vector<GeneralizedID> IDs;
			private Vector<SaveGameView> usageKeys;

			private GeneralizedIDPanel panel;

			protected GeneralizedIDTableModel(GeneralizedIDPanel panel, IDMap sourceIdMap) {
				super(new GeneralizedIDColumnID[]{ GeneralizedIDColumnID.ID, GeneralizedIDColumnID.Label, GeneralizedIDColumnID.Image, GeneralizedIDColumnID.ImgBG });
				this.panel = panel;
				
				this.usageKeys = new Vector<>();
				this.sourceIdMap = sourceIdMap;
				updateIdList();
				updateNumberOfLabledIDs();
			}

			public void addUsage(SaveGameView view) {
				usageKeys.add(view);
				updateIdList();
				updateNumberOfLabledIDs();
				fireTableStructureUpdate();
			}

			public void updateData() {
				updateIdList();
				updateNumberOfLabledIDs();
				fireTableUpdate();
			}

			public void removeUsage(SaveGameView view) {
				usageKeys.remove(view);
				fireTableStructureUpdate();
			}

			private void updateIdList() {
				IDs = sourceIdMap.getSortedValues();
			}

			private void updateNumberOfLabledIDs() {
				numberOfLabledIDs = 0;
				for (GeneralizedID id:IDs) {
					if (!id.label.isEmpty())
						++numberOfLabledIDs;
				}
			}

			@Override
			protected GeneralizedIDColumnID getColumnID(int columnIndex) {
				if (columnIndex>=columns.length) return GeneralizedIDColumnID.Usage;
				return super.getColumnID(columnIndex);
			}

			@Override public int getColumnCount() {
				return columns.length+usageKeys.size();
			}

			@Override
			public String getColumnName(int columnIndex) {
				if (columnIndex>=columns.length) return usageKeys.get(columnIndex-columns.length).file.getName();
				return super.getColumnName(columnIndex);
			}

			@Override public int getRowCount() { return IDs.size()+EXTRA_ROWS; }
			@Override public int getUnsortedRowsCount() { return EXTRA_ROWS; }
			
			@Override
			protected boolean isCellEditable(int rowIndex, int columnIndex, GeneralizedIDColumnID columnID) {
				if (rowIndex<EXTRA_ROWS) return false;
				switch(columnID) {
				case ID   : return false;
				case Label:
				case Image: return true;
				case ImgBG: return true;
				case Usage: return false;
				}
				return false;
			}

			public GeneralizedID getValue(int rowIndex) {
				if (rowIndex<EXTRA_ROWS) return null;
				return IDs.get(rowIndex-EXTRA_ROWS);
			}

			@Override
			public Object getValueAt(int rowIndex, int columnIndex, GeneralizedIDColumnID columnID) {
				if (rowIndex<EXTRA_ROWS) {
					switch(columnID) {
					case ID   : return String.format(Locale.ENGLISH,"total: %d", IDs.size());
					case Label: return String.format(Locale.ENGLISH,"labeled: %d (%1.1f%%)", numberOfLabledIDs, IDs.isEmpty()?0:numberOfLabledIDs*100.0f/IDs.size());
					case Image: return "";
					case ImgBG: return "";
					case Usage: return "";
					}
					return null;
				}
				GeneralizedID id = IDs.get(rowIndex-EXTRA_ROWS);
				if (id==null) return null;
				switch(columnID) {
				case ID   : return id.id;
				case Label: return id.label;
				case Image: return id.getImageFileName();
				case ImgBG: return id.getImageBG();
				case Usage:
					Usage usage = id.usage.get(usageKeys.get(columnIndex-columns.length).data);
					if (usage==null) return "";
					if (usage.isEmpty()) return "";
					String str = "";;
					if (!usage.inventoryUsages.isEmpty()) str += usage.inventoryUsages.size()+"xI";
					if (!usage.blueprintUsages.isEmpty()) { if (!str.isEmpty()) str+=" "; str += usage.blueprintUsages.size()+"xB"; }
					return str;
				}
				return null;
			}

			@Override
			protected void setValueAt(Object aValue, int rowIndex, int columnIndex, GeneralizedIDColumnID columnID) {
				if (rowIndex<EXTRA_ROWS) return;
				
				GeneralizedID id = IDs.get(rowIndex-EXTRA_ROWS);
				if (id==null) return;
				
				switch(columnID) {
				case ID   : return;
				case Label: id.label = aValue==null?"":aValue.toString(); break;
				case Image: id.setImageFileName(aValue); break;
				case ImgBG: id.setImageBG((aValue instanceof Integer)?(Integer)aValue:null); break;
				case Usage: return;
				}
				panel.showID(id);
				
				if (sourceIdMap==techIDs     ) saveTechIDsToFile();
				if (sourceIdMap==productIDs  ) saveProductIDsToFile();
				if (sourceIdMap==substanceIDs) saveSubstanceIDsToFile();
			}
		}
	}
}
