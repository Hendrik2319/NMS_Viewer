package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
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
import java.util.Comparator;
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
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.JSON_Object;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser;

public class SaveViewer implements ActionListener {
	
	private static final String FILE_KNOWN_STAT_ID = "NMS_Viewer.KnownStatID.txt";
	private static final String FILE_UNIVERSE_OBJECT_DATA = "NMS_Viewer.UniverseObjects.txt";

	static final boolean DEBUG = true;

	
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
		
		SaveGameView.UniversePanel.prepareIconSources();
		
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
		Open, Reload, Close, WriteHTML, WriteJSON, SwitchFolder, Compare, TabSelected, ComputeCoordinates
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
				if (new_json_data==null) {
					JOptionPane.showMessageDialog(mainWindow, "Can't parse selected file. It is not a valid JSON formated No Man's Sky savegame.", "Parse Error", JOptionPane.ERROR_MESSAGE);
				} else {
					SaveGameData saveGameData = new SaveGameData(new_json_data).parse();
					loadUniverseObjectDataFromFile(saveGameData.universe);
					SaveGameView saveGameView = new SaveGameView(mainWindow,selectedFile,saveGameData);
					loadedSaveGames.add(saveGameView);
					contentPane.addSaveGameView(saveGameView);
					updateWindowTitle();
				}
				contentPane.disabler.setEnable(ActionCommand.Close    , contentPane.currentSelected!=null);
				contentPane.disabler.setEnable(ActionCommand.Reload   , contentPane.currentSelected!=null);
				contentPane.disabler.setEnable(ActionCommand.Compare  , loadedSaveGames.size()>1 && compareTab==null);
				contentPane.disabler.setEnable(ActionCommand.WriteHTML, contentPane.currentSelected!=null);
				contentPane.disabler.setEnable(ActionCommand.WriteJSON, contentPane.currentSelected!=null);
			}
			break;
			
		case Close:
			if (contentPane.currentSelected!=null) closeSaveGameView(contentPane.currentSelected);
			break;
			
		case Reload:
			if (contentPane.currentSelected!=null) reloadSaveGameView(contentPane.currentSelected);
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
			contentPane.disabler.setEnable(ActionCommand.Close    , contentPane.currentSelected!=null);
			contentPane.disabler.setEnable(ActionCommand.Reload   , contentPane.currentSelected!=null);
			contentPane.disabler.setEnable(ActionCommand.WriteHTML, contentPane.currentSelected!=null);
			contentPane.disabler.setEnable(ActionCommand.WriteJSON, contentPane.currentSelected!=null);
			if (contentPane.isSelected(compareTab))
				compareTab.updatePanel();
			break;
			
		case ComputeCoordinates:
			new ComputeCoordinatesDialog(mainWindow).showDialog(Position.PARENT_CENTER);
			break;
		}
	}

	private void reloadSaveGameView(SaveGameView view) {
		File file = view.file;
		log_ln("");
		log("Parse file \"%s\" ...",file.getPath());
		JSON_Object new_json_data = new JSON_Parser(file).parse();
		log_ln(" done");
		if (new_json_data!=null) {
			SaveGameData saveGameData = new SaveGameData(new_json_data).parse();
			loadUniverseObjectDataFromFile(saveGameData.universe);
			view.replaceData(saveGameData);
		}
	}

	private void closeSaveGameView(SaveGameView view) {
		loadedSaveGames.remove(view);
		contentPane.removeSaveGameView(view);
		updateWindowTitle();
		
		if (loadedSaveGames.size()<2 && compareTab!=null) {
			contentPane.removeTab(compareTab);
			compareTab=null;
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
			tabbedPane.setPreferredSize(new Dimension(1500, 800));
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
			return panel!=null && tabbedPane.getSelectedComponent()==panel;
		}

		public void addTab(String name, JPanel panel, int index) {
			tabbedPane.insertTab(name, null, panel, null, index);
		}

		public void addSaveGameView(SaveGameView saveGameView) {
			int index = tabbedPane.getTabCount();
			tabbedPane.insertTab(saveGameView.toString(),null, saveGameView, saveGameView.file.getPath(), index);
			tabbedPane.setTabComponentAt(index, new TabHeader(saveGameView));
		}

		public void removeTab(JPanel panel) {
			tabbedPane.remove(panel);
		}

		public void removeSaveGameView(SaveGameView saveGameView) {
			tabbedPane.remove(saveGameView);
		}

		private void addButtons(JToolBar toolBar) {
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
				BufferedImage image = SaveGameView.UniversePanel.PortalGlyphsIS.getCachedImage(nr);
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
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
	
	public static void loadUniverseObjectDataFromFile(Universe universe) {
		System.out.println("Read data of universe objects from file \""+FILE_UNIVERSE_OBJECT_DATA+"\".");
		File file = new File(FILE_UNIVERSE_OBJECT_DATA);
		if (!file.isFile()) return;
		
		System.out.println();
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8))) {
			String str;
			UniverseAddress ua = null;
			SolarSystem system = null;
			Planet planet = null;
			UniverseObject uniObj = null;
			String uniObjName = null;
			
			String lastLabel = null;
			while ((str=in.readLine())!=null) {
				if (str.isEmpty()) continue;
				if ((str.startsWith("[Sys") || str.startsWith("[Pln")) && str.endsWith("]")) {
					system = null;
					planet = null;
					uniObj = null;
					uniObjName = null;
					String addressStr = str.substring("[Sys".length(), str.length()-"]".length());
					long address = Long.parseLong(addressStr, 16);
					ua = new UniverseAddress(address);
					if (str.startsWith("[Sys") && ua.isSolarSystem()) { system = universe.findSolarSystem(ua); System.out.printf("Solar system %s\r\n",ua.getSigBoostCode()); }
					if (str.startsWith("[Pln") && ua.isPlanet     ()) { planet = universe.findPlanet     (ua); System.out.printf("Planet %s\r\n",ua.getExtendedSigBoostCode()); }
					if (system!=null) { uniObj = system; uniObjName = "solar system "+ua.getSigBoostCode(); }
					if (planet!=null) { uniObj = planet; uniObjName = "planet "+ua.getExtendedSigBoostCode(); }
					continue;
				}
				if (str.startsWith("name=")) {
					String name = str.substring("name=".length());
					if (uniObj!=null) {
						uniObj.setOriginalName(name);
						System.out.printf("   Name of %s was defined: \"%s\"\r\n",uniObjName,name);
					}
					continue;
				}
				if (str.startsWith("race=")) {
					if (system==null) continue;
					String race = str.substring("race=".length());
					try { system.race = Universe.SolarSystem.Race.valueOf(race); }
					catch (Exception e) { system.race = null; }
					System.out.printf("   Race of solar system %s was defined: \"%s\" -> [%s]\r\n",ua.getSigBoostCode(),race,system.race);
					continue;
				}
				if (str.startsWith("class=")) {
					if (system==null) continue;
					String starClass = str.substring("class=".length());
					try { system.starClass = Universe.SolarSystem.StarClass.valueOf(starClass); }
					catch (Exception e) { system.starClass = null; }
					System.out.printf("   StarClass of solar system %s was defined: \"%s\" -> [%s]\r\n",ua.getSigBoostCode(),starClass,system.starClass);
					continue;
				}
				if (str.startsWith("distance=")) {
					if (system==null) continue;
					String distance = str.substring("distance=".length());
					try { system.distanceToCenter = Double.parseDouble(distance); }
					catch (NumberFormatException e) { system.distanceToCenter = null; }
					System.out.printf("   Distance to galactic center of solar system %s was defined: %s -> [%s]\r\n",ua.getSigBoostCode(),distance,system.distanceToCenter);
					continue;
				}
				if (str.startsWith("short=")) {
					lastLabel = str.substring("short=".length());
					continue;
				}
				if (str.startsWith("info=")) {
					String info = str.substring("info=".length());
					if (lastLabel!=null) {
						ExtraInfo ei = new ExtraInfo(lastLabel,info);
						if (uniObj!=null) {
							uniObj.extraInfos.add(ei);
							System.out.printf("   Info of %s was defined: ( \"%s\", \"%s\" )\r\n",uniObjName,ei.shortLabel,ei.info);
						}
					}
					lastLabel=null;
					continue;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void saveUniverseObjectDataToFile(Universe universe) {
		System.out.println("Write data of universe objects to file \""+FILE_UNIVERSE_OBJECT_DATA+"\".");
		File file = new File(FILE_UNIVERSE_OBJECT_DATA);
		boolean isFirst = true;
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8));) {
			for (Galaxy g:universe.galaxies) {
				for (Region gr:g.regions) {
					for (SolarSystem sys:gr.solarSystems) {
						if (sys.hasOriginalName() || sys.race!=null || sys.starClass!=null || sys.distanceToCenter!=null) {
							if (!isFirst) out.println();
							isFirst = false;
							out.printf("[Sys%014X]\r\n",sys.getUniverseAddress().getAddress());
							if (sys.hasOriginalName()  ) out.printf("name=%s\r\n",sys.getOriginalName());
							if (sys.race!=null            ) out.printf("race=%s\r\n",sys.race);
							if (sys.starClass!=null       ) out.printf("class=%s\r\n",sys.starClass);
							if (sys.distanceToCenter!=null) out.printf(Locale.ENGLISH,"distance=%f\r\n",sys.distanceToCenter.doubleValue());
							for (ExtraInfo ei:sys.extraInfos)
								if (!ei.shortLabel.isEmpty() || !ei.info.isEmpty()) {
									out.printf("short=%s\r\n",ei.shortLabel);
									out.printf("info=%s\r\n" ,ei.info);
								}
						}
						for (Planet p:sys.planets) {
							if (p.hasOriginalName() || !p.extraInfos.isEmpty()) {
								if (!isFirst) out.println();
								isFirst = false;
								out.printf("[Pln%014X]\r\n",p.getUniverseAddress().getAddress());
								if (p.hasOriginalName()) out.printf("name=%s\r\n",p.getOriginalName());
								for (ExtraInfo ei:p.extraInfos)
									if (!ei.shortLabel.isEmpty() || !ei.info.isEmpty()) {
										out.printf("short=%s\r\n",ei.shortLabel);
										out.printf("info=%s\r\n" ,ei.info);
									}
							}
						}
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
