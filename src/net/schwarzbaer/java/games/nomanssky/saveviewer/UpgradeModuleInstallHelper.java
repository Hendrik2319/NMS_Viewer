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
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import net.schwarzbaer.gui.Disabler;
import net.schwarzbaer.gui.FileChooser;
import net.schwarzbaer.gui.StandardDialog;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.gui.StandardMainWindow.DefaultCloseOperation;
import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.CellwiseCellEditor;
import net.schwarzbaer.gui.Tables.CheckBoxRendererComponent;
import net.schwarzbaer.gui.Tables.ComboboxCellEditor;
import net.schwarzbaer.gui.Tables.NonStringRenderer;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.gui.Tables.SimplifiedColumnIDInterface;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Gui.TextFieldWithSuggestions;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView;

final class UpgradeModuleInstallHelper implements ActionListener {
	
	private static final boolean DEBUG_EVENTS = false;
	
	private static final Color COLOR_NOTCURRENTSEQUENCE = Color.LIGHT_GRAY;
	private static final Color COLOR_CURRENTSEQUENCE = Color.WHITE;
	private static final Color COLOR_CURRENTMODULE = new Color(0xFFD000);
	
	private static final Comparator<GeneralizedID> compareGeneralizedIDs =
			Comparator.<GeneralizedID,GeneralizedID.Type>comparing(id->id.type).thenComparing(id->id.id);

	public static void main(String[] args) {
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		Gui.loadToolbarIcons();
		GameInfos.loadAllIDsFromFiles();
		start(true);
	}

	static void start(boolean standalone) {
		new UpgradeModuleInstallHelper()
			.readConfig()
			.writeConfig()
			.createGUI(standalone)
			.openLastDataFile();
	}

	private static Vector<GeneralizedID> sortedID(Collection<GeneralizedID> set) {
		Vector<GeneralizedID> vector = new Vector<>(set);
		vector.sort(compareGeneralizedIDs);
		return vector;
	}

	private static Iterable<KnownModule.ValueDefinition> sortedVD(Collection<KnownModule.ValueDefinition> set) {
		return ()->set.stream().sorted().iterator();
	}

	private static String getLabelOrID(GeneralizedID id) {
		String label = id.label;
		if (label==null || label.isEmpty()) label = id.id;
		return label;
	}
	
	private static void showEvent(String caller, String format, Object... args) {
		String msg = String.format(Locale.ENGLISH, format,args);
		Gui.log_ln("[%1$tH:%1$tM:%1$tS,%1$tL] %2$s: %3$s", new Date(), caller, msg);
	}
	
	private final HashMap<String,GeneralizedID> knownUpgradeModuleIDs;

	private Disabler<ActionCommand> disabler = null;
	private StandardMainWindow mainwindow = null;
	private JSplitPane contentPane = null;
	private FileChooser fileChooser = null;
	
	private Config config = new Config();
	private Session currentSession = null;
	private TablePanel tablePanel = null;
	private ModulePanel modulePanel = null;
	private SequencesTableModel sequencesTableModel = null;
	private TestInstallsIterator testInstallsIterator = new TestInstallsIterator();
	private FinalSequenceTableModel finalSequenceTableModel = null;
	
	private UpgradeModuleInstallHelper() {
		knownUpgradeModuleIDs = new HashMap<>();
		GameInfos.productIDs.forEach(id->{
			if (id.type!=null && id.type.isUpgradeModule)
				knownUpgradeModuleIDs.put(id.id,id);
		});
		//knownUpgradeModules.forEach((str,id)->{
		//	SaveViewer.log_ln("[%s] -> \"%s\"", str, id);
		//});
		//Vector<GeneralizedID> vector = sorted(knownUpgradeModules.values());
	}
	
	private UpgradeModuleInstallHelper readConfig() {
		Gui.log_ln("read config from file \"%s\"", FileExport.FILE_CFG_UPGRADE_MODULE_INSTALL_HELPER);
		config.readFromFile(new File(FileExport.FILE_CFG_UPGRADE_MODULE_INSTALL_HELPER));
		return this;
	}

	private UpgradeModuleInstallHelper writeConfig() {
		Gui.log_ln("write config to file \"%s\"", FileExport.FILE_CFG_UPGRADE_MODULE_INSTALL_HELPER);
		config.writeToFile(new File(FileExport.FILE_CFG_UPGRADE_MODULE_INSTALL_HELPER));
		return this;
	}

	private UpgradeModuleInstallHelper openLastDataFile() {
		openSession(config.currentSessionFile);
		return this;
	}

	enum ActionCommand {
		NewSession, OpenSession, SaveSession, SaveSessionAs, EditSession, StartInstallationTests, StopInstallationTests, InstallNext, DefineFinalInstallation, SetValueColoring, ShowValuePriorities, SetSelectionColoring,
		;
	}
	
	private UpgradeModuleInstallHelper createGUI(boolean standalone) {
		fileChooser = new FileChooser("Session", "umih");
		fileChooser.setMultiSelectionEnabled(false);
		
		disabler = new Disabler<ActionCommand>();
		disabler.setCareFor(ActionCommand.values());
		
		tablePanel = new TablePanel();
		
		modulePanel = new ModulePanel();
		modulePanel.setBorder( BorderFactory.createTitledBorder("Modules"));
		
		sequencesTableModel = new SequencesTableModel();
		JTable sequencesTable = new JTable(sequencesTableModel);
		sequencesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		sequencesTableModel.setTable(sequencesTable);
		
		JScrollPane sequencesTableScrollPane = new JScrollPane(sequencesTable);
		sequencesTableScrollPane.setBorder( Gui.createTitledBorderForScrollPane("Sequences") );
		sequencesTableScrollPane.setMinimumSize(new Dimension(200,150));
		//sequencesPanel.setPreferredSize(new Dimension(200,400));
		
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		
		c.gridx = GridBagConstraints.RELATIVE;
		c.gridy = GridBagConstraints.RELATIVE;
		JPanel installationTestButtonsPanel = new JPanel(new GridBagLayout());
		installationTestButtonsPanel.setBorder( BorderFactory.createTitledBorder("Installation Tests"));
		c.weightx = 0;
		installationTestButtonsPanel.add(Gui.createButton("Start", this, disabler, ActionCommand.StartInstallationTests),c);
		installationTestButtonsPanel.add(Gui.createButton("Next" , this, disabler, ActionCommand.InstallNext),c);
		installationTestButtonsPanel.add(Gui.createButton("Stop" , this, disabler, ActionCommand.StopInstallationTests),c);
		c.weightx = 1;
		installationTestButtonsPanel.add(new JLabel(),c);
		
		finalSequenceTableModel = new FinalSequenceTableModel();
		JTable finalSequenceTable = new JTable(finalSequenceTableModel);
		finalSequenceTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		finalSequenceTableModel.setTable(finalSequenceTable);
		JScrollPane finalSequenceTableScrollPane = new JScrollPane(finalSequenceTable);
		finalSequenceTableScrollPane.setMinimumSize(new Dimension(200,150));
		
		JPanel finalSequencePanel = new JPanel(new GridBagLayout());
		finalSequencePanel.setBorder( BorderFactory.createTitledBorder("Final Installation Sequence"));
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		c.weighty = 0;
		finalSequencePanel.add(finalSequenceTableScrollPane,c);
		c.gridwidth = 1;
		c.weightx = 0;
		c.weighty = 0;
		finalSequencePanel.add(Gui.createCheckbox("Define in Module Tables", disabler, ActionCommand.DefineFinalInstallation, tablePanel.isDefiningFinalInstallation(), true, b -> {
			tablePanel.defineFinalInstallation(b);
			updateGUIaccess();
		}),c);
		ButtonGroup bg1 = new ButtonGroup();
		finalSequencePanel.add(new JLabel(" Coloring: "),c);
		finalSequencePanel.add(Gui.createRadioButton("Selection", bg1, disabler, ActionCommand.SetSelectionColoring, true , true, e->tablePanel.setSelectionColoring(true )),c);
		finalSequencePanel.add(Gui.createRadioButton("Values"   , bg1, disabler, ActionCommand.SetSelectionColoring, false, true, e->tablePanel.setSelectionColoring(false)),c);
		c.weightx = 1;
		finalSequencePanel.add(new JLabel(),c);
		
		
		JPanel optionPanel = new JPanel(new GridBagLayout());
		optionPanel.setBorder( BorderFactory.createTitledBorder("Options"));
		c.gridwidth = 1;
		c.weightx = 0;
		c.weighty = 0;
		optionPanel.add(Gui.createCheckbox   ("Show Value Priorities", disabler, ActionCommand.ShowValuePriorities, false, true, this::showValuePriorities),c);
		ButtonGroup bg2 = new ButtonGroup();
		optionPanel.add(new JLabel(" Coloring: "),c);
		optionPanel.add(Gui.createRadioButton(  "0..Max", bg2, disabler, ActionCommand.SetValueColoring, true , true, e->tablePanel.setValueColoringMinMax(false)),c);
		optionPanel.add(Gui.createRadioButton("Min..Max", bg2, disabler, ActionCommand.SetValueColoring, false, true, e->tablePanel.setValueColoringMinMax(true )),c);
		c.weightx = 1;
		optionPanel.add(new JLabel(),c);
		
		
		c.gridwidth = GridBagConstraints.REMAINDER;
		JPanel sessionPanel = new JPanel(new GridBagLayout());
		c.weighty = 0;
		sessionPanel.add(modulePanel,c);
		sessionPanel.add(sequencesTableScrollPane,c);
		sessionPanel.add(installationTestButtonsPanel,c);
		sessionPanel.add(finalSequencePanel,c);
		sessionPanel.add(optionPanel,c);
		c.weighty = 1;
		sessionPanel.add(new JLabel(),c);
		
		
		contentPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		contentPane.setLeftComponent(sessionPanel);
		contentPane.setRightComponent(tablePanel);
		contentPane.setResizeWeight(0);
		contentPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, e->config.windowSplit = contentPane.getDividerLocation());
		//contentPane = new JPanel(new BorderLayout(3,3));
		//contentPane.add(tablePanel = new TablePanel(),BorderLayout.CENTER);
		
		if (config.windowSplit!=null) {
			contentPane.setDividerLocation(config.windowSplit);
		}
		
		
		JMenu menuData = new JMenu("Data");
		menuData.add(Gui.createMenuItem("New Session"            , this, disabler, ActionCommand.NewSession));
		menuData.add(Gui.createMenuItem("Open Session"           , this, disabler, ActionCommand.OpenSession));
		menuData.add(Gui.createMenuItem("Save Session"           , this, disabler, ActionCommand.SaveSession));
		menuData.add(Gui.createMenuItem("Save Session as ..."    , this, disabler, ActionCommand.SaveSessionAs));
		menuData.addSeparator();
		menuData.add(Gui.createMenuItem("Edit Session Parameters", this, disabler, ActionCommand.EditSession));
		
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(menuData);
		
		mainwindow = new StandardMainWindow("UpgradeModule InstallHelper", standalone ? DefaultCloseOperation.EXIT_ON_CLOSE : DefaultCloseOperation.DISPOSE_ON_CLOSE);
		mainwindow.startGUI(contentPane, menuBar);
		
		updateWindowTitle();
		updateGUIaccess();
		if (config.windowSize!=null) {
			//SaveViewer.log_ln("MainWindow.setSize0: %s", config.windowSize);
			config.windowSize.width  = Math.max(config.windowSize.width , mainwindow.getWidth ());
			config.windowSize.height = Math.max(config.windowSize.height, mainwindow.getHeight());
			//SaveViewer.log_ln("MainWindow.setSize1: %s", config.windowSize);
			mainwindow.setSizeCenteredOnScreen(config.windowSize);
		}
		
		mainwindow.addComponentListener(new ComponentListener() {
			@Override public void componentShown(ComponentEvent e) {}
			@Override public void componentMoved(ComponentEvent e) {}
			@Override public void componentHidden(ComponentEvent e) {}
			
			@Override public void componentResized(ComponentEvent e) {
				config.windowSize=mainwindow.getSize(config.windowSize);
				//SaveViewer.log_ln("MainWindow.Resized: %s", config.windowSize);
			}
		});
		mainwindow.addWindowListener(new WindowAdapter() {
			@Override public void windowClosing(WindowEvent e) {
				//SaveViewer.log_ln("windowClosing");
				writeConfig();
			}
		});
		
		return this;
	}

	private void showValuePriorities(boolean b) {
		tablePanel.showValuePriorities(b);
		finalSequenceTableModel.showValuePriorities(b);
	}

	private void updateGUIaccess() {
		disabler.setEnable(ac->{
			switch (ac) {
			case NewSession:
			case OpenSession:
				return !testInstallsIterator.isRunning();
				
			case EditSession:
			case StartInstallationTests:
			case DefineFinalInstallation:
			case ShowValuePriorities:
				return currentSession!=null && !testInstallsIterator.isRunning();
				
			case StopInstallationTests:
				return currentSession!=null && testInstallsIterator.isRunning();
				
			case InstallNext:
				return currentSession!=null && testInstallsIterator.isRunning() && !testInstallsIterator.hasReachedEnd();
				
			case SaveSessionAs:
				return currentSession!=null;
				
			case SaveSession:
				return currentSession!=null && config.currentSessionFile!=null;
				
			case SetValueColoring:
				return currentSession!=null;
				
			case SetSelectionColoring:
				return currentSession!=null && tablePanel.isDefiningFinalInstallation();
			}
			return null;
		});
	}

	private void updateWindowTitle() {
		mainwindow.setTitle("UpgradeModule InstallHelper - "+( config.currentSessionFile==null ? "<New Session>" : "\""+config.currentSessionFile.getName()+"\"" ));
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		switch (ActionCommand.valueOf(e.getActionCommand())) {
		case NewSession: {
			EditSessionDialog dlg = new EditSessionDialog(mainwindow,"New Session",null,config.knownModules);
			dlg.showDialog();
			Session newSession = dlg.getResult();
			if (newSession!=null) {
				currentSession = newSession;
				config.currentSessionFile = null;
				writeConfig();
				updateDataInGui();
				updateWindowTitle();
				updateGUIaccess();
			}
		} break;
		case EditSession: {
			EditSessionDialog dlg = new EditSessionDialog(mainwindow,"Edit Session",currentSession,config.knownModules);
			dlg.showDialog();
			Session newSession = dlg.getResult();
			if (newSession!=null) {
				currentSession = newSession;
				updateDataInGui();
				updateGUIaccess();
			}
		} break;
		
		case OpenSession:
			if (fileChooser.showOpenDialog(mainwindow)==FileChooser.APPROVE_OPTION)
				openSession(fileChooser.getSelectedFile());
			break;
			
		case SaveSession:
			if (config.currentSessionFile!=null) {
				SaveViewer.runWithProgressDialog(mainwindow, "Save Session", pd->{
					currentSession.saveFile(config.currentSessionFile);
				});
				break;
			}
			
		case SaveSessionAs:
			if (fileChooser.showSaveDialog(mainwindow)==FileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				SaveViewer.runWithProgressDialog(mainwindow, "Save Session", pd->{
					currentSession.saveFile(file);
					config.currentSessionFile = file;
					writeConfig();
					SaveViewer.runInEventThreadAndWait(()->{
						updateWindowTitle();
						updateGUIaccess();
					});
				});
			}
			break;
			
		case StartInstallationTests: testInstallsIterator.start(); updateGUIaccess(); break;
		case StopInstallationTests : testInstallsIterator.stop (); updateGUIaccess(); break;
		case InstallNext           : testInstallsIterator.next (); updateGUIaccess(); break;
		
		case DefineFinalInstallation: break;
		case ShowValuePriorities: break;
		case SetValueColoring: break;
		case SetSelectionColoring: break;
		}
	}

	private void updateDataInGui() {
		tablePanel.updateTables();
		Vector<GeneralizedID> modules = sortedID(currentSession.blocks.keySet());
		GeneralizedID[][] sequences = currentSession.sequences;
		modulePanel.updateModules(modules);
		sequencesTableModel.updateSequences(modules,sequences,currentSession.nModules);
		testInstallsIterator.update();
		finalSequenceTableModel.update(currentSession);
	}

	private void openSession(File file) {
		if (file==null) return;
		SaveViewer.runWithProgressDialog(mainwindow, "Open Session", pd->{
			Session newSession = Session.openFile(file, knownUpgradeModuleIDs::get, config.knownModules::get);
			if (newSession!=null) {
				currentSession = newSession;
				if (config.currentSessionFile != file) {
					config.currentSessionFile = file;
					writeConfig();
				}
				SaveViewer.runInEventThreadAndWait(()->{
					updateDataInGui();
					updateWindowTitle();
					updateGUIaccess();
				});
			}
		});
	}
	
	private static Float computeUpgradePriority(InstalledUpgrade upgrade, Vector<KnownModule.ValueDefinition> values) {
		if (upgrade==null) return null;
		
		float priority = 0;
		float vd_priority = 0;
		for (KnownModule.ValueDefinition vd:values) {
			if (vd.max==null || vd.max==0) continue;
			Float value = upgrade.values.get(vd);
			if (value==null) continue;
			vd_priority += vd.priority;
			priority += vd.priority * (value/vd.max);
		}
		
		if (vd_priority==0)
			return priority;
		return priority/vd_priority;
	}
	
	private static <V> Vector<V> cloneVector( Vector<V> vec, Function<V,V> cloneV ) {
		Vector<V> newVec = new Vector<>();
		vec.forEach(v->{
			if (cloneV!=null) v = cloneV.apply(v);
			newVec.add(v);
		});
		return newVec;
	}

	private static <K,V> HashMap<K,V> cloneHashMap( HashMap<K,V> map, Function<K,K> cloneK, Function<V,V> cloneV ) {
		HashMap<K,V> newMap = new HashMap<>();
		map.forEach((k,v)->{
			if (cloneK!=null) k = cloneK.apply(k);
			if (cloneV!=null) v = cloneV.apply(v);
			newMap.put(k,v);
		});
		return newMap;
	}

	private class Config {
		Dimension windowSize;
		Integer windowSplit;
		File currentSessionFile;
		HashMap<GeneralizedID,KnownModule> knownModules;
		
		Config() {
			currentSessionFile = null;
			knownModules = new HashMap<>();
			windowSize = null;
			windowSplit = null;
		}

		public void readFromFile(File file) {
			currentSessionFile = null;
			knownModules.clear();
			windowSize = null;
			windowSplit = null;
			
			KnownModule.ValueDefinition.uniqueIDs.clearPool();
			KnownModule knownModule = null;
			KnownModule.ValueDefinition knownValue = null;
			try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
				String line;
				while ( (line=in.readLine())!=null ) {
					if (line.startsWith("LastSessionFile=")) {
						String str = line.substring("LastSessionFile=".length());
						currentSessionFile = new File(str);
					}
					if (line.startsWith("WindowWidth=")) {
						String str = line.substring("WindowWidth=".length());
						try {
							int val = Integer.parseInt(str);
							if (windowSize==null) windowSize=new Dimension();
							windowSize.width = val;
						} catch (NumberFormatException e) {
							Gui.log_error_ln("Can't parse WindowWidth as integer in \"%s\"", str);
						}
					}
					if (line.startsWith("WindowHeight=")) {
						String str = line.substring("WindowHeight=".length());
						try {
							int val = Integer.parseInt(str);
							if (windowSize==null) windowSize=new Dimension();
							windowSize.height = val;
						} catch (NumberFormatException e) {
							Gui.log_error_ln("Can't parse WindowHeight as integer in \"%s\"", str);
						}
					}
					if (line.startsWith("WindowSplit=")) {
						String str = line.substring("WindowSplit=".length());
						try {
							windowSplit = Integer.parseInt(str);
						} catch (NumberFormatException e) {
							Gui.log_error_ln("Can't parse WindowSplit as integer in \"%s\"", str);
						}
					}
					
					if (line.startsWith("ModuleID=")) {
						String str = line.substring("ModuleID=".length());
						GeneralizedID moduleID = knownUpgradeModuleIDs.get(str);
						if (moduleID==null) {
							Gui.log_error_ln("Can't find UpgradeModule for ID \"%s\"", str);
							knownModule = null;
						} else
							knownModules.put( moduleID, knownModule = new KnownModule(moduleID) );
					}
					if (line.startsWith("value.uniqueID=") && knownModule!=null) {
						String str = line.substring("value.uniqueID=".length());
						try {
							long uniqueID = Long.parseLong(str,16);
							Debug.Assert(KnownModule.ValueDefinition.uniqueIDs.notExists(uniqueID));
							KnownModule.ValueDefinition.uniqueIDs.add(uniqueID);
							knownModule.values.add( knownValue = new KnownModule.ValueDefinition(knownModule,uniqueID) );
						}
						catch (NumberFormatException e) {
							Gui.log_error_ln("Can't parse <value.uniqueID> as hex long: \"%s\"", str);
							knownValue = null;
						}
					}
					if (line.startsWith("value.label=") && knownModule!=null) {
						String str = line.substring("value.label=".length());
						knownValue.label = str;
					}
					if (line.startsWith("value.format=") && knownValue!=null) {
						String str = line.substring("value.format=".length());
						try { knownValue.format = KnownModule.ValueDefinition.Format.valueOf(str); }
						catch (Exception e) { knownValue.format = null; }
					}
					if (line.startsWith("value.min=") && knownValue!=null) {
						String str = line.substring("value.min=".length());
						try { knownValue.min = Float.parseFloat(str); }
						catch (NumberFormatException e) { knownValue.min = null; }
					}
					if (line.startsWith("value.max=") && knownValue!=null) {
						String str = line.substring("value.max=".length());
						try { knownValue.max = Float.parseFloat(str); }
						catch (NumberFormatException e) { knownValue.max = null; }
					}
					
				}
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
				e.printStackTrace();
			}
			for (GeneralizedID id:knownUpgradeModuleIDs.values()) {
				if (!knownModules.containsKey(id)) {
					knownModules.put( id, new KnownModule(id) );
				}
			}
		}
		
		public void writeToFile(File file) {
			try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
				
				if (currentSessionFile!=null) out.printf("LastSessionFile=%s%n", currentSessionFile.getAbsolutePath());
				if (windowSize!=null) {
					out.printf("WindowWidth=%d%n", windowSize.width);
					out.printf("WindowHeight=%d%n", windowSize.height);
				}
				if (windowSplit!=null) {
					out.printf("WindowSplit=%d%n", windowSplit);
				}
				
				for (GeneralizedID id:sortedID(knownModules.keySet())) {
					KnownModule module = knownModules.get(id);
					if (module.values.isEmpty()) continue;
					out.println();                      // decoration only :)
					out.println("[KnownModuleValues]"); // decoration only :)
					Debug.Assert(id==module.moduleID);
					out.printf("ModuleID=%s%n", module.moduleID.id);
					module.values.forEach(vd->{
						out.printf("value.uniqueID=%016X%n", vd.uniqueID);
						out.printf("value.label=%s%n", vd.label);
						if (vd.format!=null) out.printf("value.format=%s%n", vd.format);
						if (vd.min!=null && !Float.isNaN(vd.min)) out.printf(Locale.ENGLISH, "value.min=%1.3e%n", vd.min);
						if (vd.max!=null && !Float.isNaN(vd.max)) out.printf(Locale.ENGLISH, "value.max=%1.3e%n", vd.max);
					});
				}
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	private final class TestInstallsIterator {

		private int currentSequence;
		private int currentModule;
		private boolean isRunning;

		public void update() {
			currentSequence = 0;
			currentModule = 0;
			isRunning = false;
		}

		public boolean hasReachedEnd() {
			if (currentSession==null) return true;
			if (currentSequence>=currentSession.sequences.length  ) return true;
			if (currentSequence< currentSession.sequences.length-1) return false;
			return currentModule >= currentSession.sequences[currentSequence].length-1;
		}

		public boolean isRunning() {
			return isRunning;
		}

		public void start() {
			currentSequence = 0;
			currentModule = 0;
			isRunning = true;
			Debug.Assert(currentSequence < currentSession.sequences.length);
			Debug.Assert(currentModule < currentSession.sequences[currentSequence].length);
			sequencesTableModel.setCurrentModule(currentSequence,currentModule);
			tablePanel.setCurrentInstallTestModule(currentSession.sequences[currentSequence][currentModule],currentModule);
		}

		public void stop() {
			isRunning = false;
			sequencesTableModel.clearCurrentModule();
			tablePanel.clearCurrentInstallTestModule();
		}

		public void next() {
			Debug.Assert(currentSession!=null);
			Debug.Assert(currentSession.sequences!=null);
			Debug.Assert(currentSequence < currentSession.sequences.length);
			++currentModule;
			if (currentModule>=currentSession.sequences[currentSequence].length) {
				currentModule=0;
				++currentSequence;
			}
			Debug.Assert(currentSequence < currentSession.sequences.length);
			Debug.Assert(currentModule < currentSession.sequences[currentSequence].length);
			
			sequencesTableModel.setCurrentModule(currentSequence,currentModule);
			tablePanel.setCurrentInstallTestModule(currentSession.sequences[currentSequence][currentModule],currentModule);
		}
	}
	
	private static class PoolOfUniqueIDs {
		static PoolOfUniqueIDs instance = null;
		@SuppressWarnings("unused")
		static PoolOfUniqueIDs getInstance() {
			if (instance==null)
				instance = new PoolOfUniqueIDs();
			return instance;
		}

		private HashSet<Long> pool;
		private Random rnd;
		
		PoolOfUniqueIDs() {
			pool = new HashSet<>();
			rnd = new Random();
		}
		
		public void add(long id) { pool.add(id); }
		public boolean exists(long id) { return pool.contains(id); }
		public boolean notExists(long id) { return !exists(id); }
		public void clearPool() { pool.clear(); }

		public long createNewID() {
			long id;
			while ( exists(id=rnd.nextLong()) || id<0 );
			add(id);
			return id;
		}
		
		
		
	}
	
	private static class KnownModule {
		
		GeneralizedID moduleID;
		Vector<ValueDefinition> values;
		
		KnownModule(GeneralizedID moduleID) {
			this.moduleID = moduleID;
			values = new Vector<>();
		}
		
		public ValueDefinition getValueDefinition(long uniqueID) {
			for (ValueDefinition vd:values)
				if (vd.uniqueID==uniqueID)
					return vd;
			return null;
		}

		private static class ValueDefinition implements Comparable<ValueDefinition> {
			enum Format {
				PercentPlus, PercentMinus, Activated, FloatPlus, Lightyears,
				;
				public String getFormatedValue(float value) {
					switch (this) {
					case Activated   : return Math.abs(value)>=1 ? "aktiviert" : "";
					case FloatPlus   : return String.format(Locale.ENGLISH, "+%1.1f"   , Math.abs(value));
					case Lightyears  : return String.format(Locale.ENGLISH, "%1.0f Lj.", Math.abs(value));
					case PercentMinus: return String.format(Locale.ENGLISH, "-%1.0f%%", Math.abs(value));
					case PercentPlus : return String.format(Locale.ENGLISH, "+%1.0f%%", Math.abs(value));
					}
					return null;
				}
			}
			final static PoolOfUniqueIDs uniqueIDs = new PoolOfUniqueIDs();
			
			final KnownModule module;
			final long uniqueID;
			String label;
			Format format;
			Float min,max;
			
			float priority; // value will not be stored
			
			ValueDefinition(KnownModule module) {
				this(module,uniqueIDs.createNewID());
			}
			ValueDefinition(ValueDefinition vd, long uniqueID) {
				this(vd.module,uniqueID);
				setValues(vd);
			}
			ValueDefinition(KnownModule module, long uniqueID) {
				this.module = module;
				this.uniqueID = uniqueID;
				setValues(null);
			}
			public void setValues(ValueDefinition vd) {
				label    = vd==null ? ""   : vd.label   ;
				format   = vd==null ? null : vd.format  ;
				min      = vd==null ? null : vd.min     ;
				max      = vd==null ? null : vd.max     ;
				priority = vd==null ? 1    : vd.priority;
			}
			@Override public int compareTo( ValueDefinition other) {
				return (int) (this.uniqueID - other.uniqueID);
			}
			@Override
			public String toString() {
				StringBuilder sb = new StringBuilder();
				sb.append(String.format("UniqueID=%016X", uniqueID));
				if (module != null) sb.append(", ").append("module=").append(getLabelOrID(module.moduleID));
				if (label  != null) sb.append(", ").append("label=").append(label);
				if (format != null) sb.append(", ").append("format=").append(format);
				if (min    != null) sb.append(", ").append("min=").append(min);
				if (max    != null) sb.append(", ").append("max=").append(max);
				sb.append(", ").append("priority=").append(priority);
				return "VD[ "+sb.toString()+" ]";
			}
			
		}
	}
	
	private static class InstalledUpgrade {
		
		final KnownModule base;
		String label1;
		String label2;
		HashMap<KnownModule.ValueDefinition,Float> values;
		
		InstalledUpgrade(KnownModule base) {
			this.base = base;
			label1 = "";
			label2 = "";
			values = new HashMap<>();
		}
		InstalledUpgrade(InstalledUpgrade module) {
			base = module.base;
			label1 = module.label1;
			label2 = module.label2;
			values = cloneHashMap(module.values, null, null);
		}
		@SuppressWarnings("unused")
		public String getFormatedValue(KnownModule.ValueDefinition vd) {
			if (vd==null) return null;
			Float value = values.get(vd);
			if (value==null) return null;
			return vd.format.getFormatedValue(value);
		}
	}
	
	private static class Session {
		
		HashMap<GeneralizedID,SessionBlock> blocks;
		GeneralizedID[] finalSequence;
		
		int nModules;
		GeneralizedID[][] sequences;
		
		Session() {
			nModules = 0;
			sequences = null;
			finalSequence = null;
			blocks = new HashMap<>();
		}

		Session(Session session) {
			blocks = cloneHashMap(session.blocks,null,SessionBlock::new);
			updateNumberOfModules();
			computeSequences();
		}

		void updateNumberOfModules() {
			nModules = 0;
			for (GeneralizedID id:blocks.keySet()) {
				Session.SessionBlock block = blocks.get(id);
				Debug.Assert(id==block.module.moduleID);
				nModules += block.amount;
			}
			finalSequence = new GeneralizedID[nModules];
			Arrays.fill(finalSequence, null);
		}

		public void computeSequences() {
			sequences = new GeneralizedID[nModules][];
			sequences[0] = createBaseSequence();
			//Vector<GeneralizedID> sortedIDs = sortedID(blocks.keySet());
			
			if (sequences[0] == null) {
				sequences = null;
				return;
			}
			
			for (int i=1; i<nModules; ++i) {
				sequences[i] = shiftSequences(sequences[i-1]);
			}
			
			//showSequences(sequences,sortedIDs);
			
			for (int i0=nModules-1; i0>=0; --i0) {
				GeneralizedID[] sequ0 = sequences[i0];
				for (int i1=i0+1; i1<nModules; ++i1) {
					GeneralizedID[] sequ1 = sequences[i1];
					for (int i=0; i<nModules; ++i) {
						if (sequ0[i]==sequ1[i])
							sequ1[i] = null;
					}
				}
			}
			//showSequences(sequences,sortedIDs);
			
			Vector<GeneralizedID[]> nonNullSequences = new Vector<>();
			for (int i=0; i<nModules; ++i) {
				boolean isNull = true;
				for (int j=nModules-1; j>=0; --j)
					if (sequences[i][j]!=null) {
						if (j<nModules-1)
							sequences[i] = Arrays.copyOf(sequences[i], j+1);
						isNull = false;
						break;
					}
				if (!isNull)
					nonNullSequences.add(sequences[i]);
			}
			sequences = nonNullSequences.toArray(new GeneralizedID[nonNullSequences.size()][]);
			//showSequences(sequences,sortedIDs);
		}

		private GeneralizedID[] createBaseSequence() {
			GeneralizedID[] baseSequence = new GeneralizedID[nModules];
			
			for (GeneralizedID id:blocks.keySet()) {
				Session.SessionBlock block = blocks.get(id);
				for (int i=0; i<block.amount; i++) {
					Integer pos = block.getInstallPos(i,nModules);
					if (pos==null) return null;
					baseSequence[pos] = id;
				}
			}
			return baseSequence;
		}

		private GeneralizedID[] shiftSequences(GeneralizedID[] sequence) {
			GeneralizedID[] newSequences = Arrays.copyOf( Arrays.copyOfRange(sequence, 1, sequence.length), sequence.length);
			newSequences[sequence.length-1] = sequence[0];
			return newSequences;
		}

		@SuppressWarnings("unused")
		private void showSequences(GeneralizedID[][] sequences, Vector<GeneralizedID> sortedIDs) {
			Gui.log_ln("Sequences:");
			for (int i=0; i<sequences.length; ++i) {
				Gui.log("[%2d] ", i);
				if (sequences[i] == null)
					Gui.log_ln(" <null>");
				else {
					String str = "";
					for (int j=0; j<sequences[i].length; ++j) {
						GeneralizedID id = sequences[i][j];
						if (id==null) {
							if (!str.isEmpty()) str += ',';
							str += "--"; 
						} else {
							if (!str.isEmpty()) str += ',';
							str += String.format("%2d", sortedIDs.indexOf(id)+1); 
						}
					}
					Gui.log_ln(" %s", str);
				}
			}
		}
		
		private enum SessionFileChapter { FinalSequence, UpgradeModule, InstalledUpgrade }
		static Session openFile(File file, Function<String,GeneralizedID> getID, Function<GeneralizedID, KnownModule> getKnownModule) {
			Session session = new Session();
			
			SessionFileChapter currentChapter = null;
			SessionBlock block = null;
			Vector<KnownModule.ValueDefinition> announcedValueDefinitions = new Vector<>();
			KnownModule.ValueDefinition announcedValueDefinition = null;
			InstalledUpgrade installedUpgrade = null;
			KnownModule.ValueDefinition usedValueDefinition = null;
			Vector<GeneralizedID> tempFinalSequence = new Vector<>();
			
			try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
				String line;
				while ( (line=in.readLine())!=null ) {
					switch (line) {
					case "[FinalSequence]"   : currentChapter = SessionFileChapter.FinalSequence; break;
					case "[UpgradeModule]"   : currentChapter = SessionFileChapter.UpgradeModule; break;
					case "[InstalledUpgrade]": currentChapter = SessionFileChapter.InstalledUpgrade; break;
					}
					
					switch (currentChapter) {
					case FinalSequence:
						if (line.equals("[FinalSequence]")) {
							tempFinalSequence.clear();
						}
						if (line.startsWith("ModuleID=")) {
							String str = line.substring("ModuleID=".length());
							tempFinalSequence.add(str.isEmpty() ? null : getID.apply(str));
						}
						break;
					
					case UpgradeModule: // SessionBlock
						if (line.startsWith("ModuleID=")) {
							String str = line.substring("ModuleID=".length());
							
							GeneralizedID id = getID.apply(str);
							if (id==null)
								Gui.log_error_ln("Can't find GeneralizedID for \"%s\".", str);
							
							KnownModule module = null;
							if (id!=null) {
								module = getKnownModule.apply(id);
								if (module==null)
									Gui.log_error_ln("Can't find KnownModule for GeneralizedID \"%s\".", id);
							}
							
							block = null;
							announcedValueDefinition = null;
							announcedValueDefinitions.clear();
							installedUpgrade = null;
							usedValueDefinition = null;
							
							if (module!=null)
								session.blocks.put(id, block = new SessionBlock(module));
						}
						if (line.startsWith("Amount=") && block!=null) {
							String str = line.substring("Amount=".length());
							try { block.amount = Integer.parseInt(str); }
							catch (NumberFormatException e) {
								Gui.log_error_ln("Can't parse <block.amount> as integer: \"%s\"", str);
								block.amount = 0;
							}
						}
						if (line.startsWith("InstallPos=") && block!=null) {
							String str = line.substring("InstallPos=".length());
							block.installPos = parseInstallPos(str);
						}
						
						// KnownModule.ValueDefinition  -->  usedValueDefinitions
						if (line.startsWith("valueDef.uniqueID=") && block!=null) {
							String str = line.substring("valueDef.uniqueID=".length());
							try {
								long uniqueID = Long.parseLong(str,16);
								announcedValueDefinition = new KnownModule.ValueDefinition(block.module,uniqueID);
								announcedValueDefinitions.add(announcedValueDefinition);
							}
							catch (NumberFormatException e) {
								Gui.log_error_ln("Can't parse <valueDef.uniqueID> as hex long: \"%s\"", str);
								announcedValueDefinition = null;
							}
						}
						if (line.startsWith("valueDef.label=") && announcedValueDefinition!=null) {
							String str = line.substring("valueDef.label=".length());
							announcedValueDefinition.label = str;
						}
						if (line.startsWith("valueDef.format=") && announcedValueDefinition!=null) {
							String str = line.substring("valueDef.format=".length());
							try { announcedValueDefinition.format = KnownModule.ValueDefinition.Format.valueOf(str); }
							catch (Exception e) {
								Gui.log_error_ln("Can't parse <valueDef.format> as KnownModule.ValueDefinition.Format: \"%s\"", str);
								announcedValueDefinition.format = null;
							}
						}
						break;
						
					case InstalledUpgrade:
						// InstalledUpgrade  -->  block.installedModules
						if (line.equals("[InstalledUpgrade]") && block!=null) {
							if (block.installedModules.isEmpty()) {
								checkValueDefinitions(block.module,announcedValueDefinitions);
								announcedValueDefinition = null;
								announcedValueDefinitions.clear();
								usedValueDefinition = null;
							}
						}
						if (line.startsWith("index=") && block!=null) {
							String str = line.substring("index=".length());
							try {
								int index = Integer.parseInt(str);
								while (index>=block.installedModules.size()) block.installedModules.add(null);
								block.installedModules.set(index, installedUpgrade = new InstalledUpgrade(block.module));
							}
							catch (NumberFormatException e) {
								Gui.log_error_ln("Can't parse <InstalledUpgrade.index> as integer: \"%s\"", str);
								installedUpgrade = null;
							}
						}
						if (line.startsWith("upgrade.label1=") && installedUpgrade!=null) {
							String str = line.substring("upgrade.label1=".length());
							installedUpgrade.label1 = str;
						}
						if (line.startsWith("upgrade.label2=") && installedUpgrade!=null) {
							String str = line.substring("upgrade.label2=".length());
							installedUpgrade.label2 = str;
						}
						
						// uniqueID  -->  usedValueDefinition
						if (line.startsWith("value.uniqueID=") && installedUpgrade!=null) {
							String str = line.substring("value.uniqueID=".length());
							try {
								long uniqueID = Long.parseLong(str,16);
								usedValueDefinition = block.module.getValueDefinition(uniqueID);
								if (usedValueDefinition==null)
									Gui.log_error_ln("Can't find ValueDefinition with UniqueID[%s].", str);
							}
							catch (NumberFormatException e) {
								Gui.log_error_ln("Can't parse <InstalledUpgrade.value.uniqueID> as hex long: \"%s\"", str);
								usedValueDefinition = null;
							}
						}
						// value + usedValueDefinition  -->  InstalledUpgrade.values
						if (line.startsWith("value.value=") && usedValueDefinition!=null) {
							String str = line.substring("value.value=".length());
							Float value;
							try { value = Float.parseFloat(str); }
							catch (NumberFormatException e) {
								Gui.log_error_ln("Can't parse <InstalledUpgrade.value> as Float: \"%s\"", str);
								value = null;
							}
							installedUpgrade.values.put(usedValueDefinition, value);
						}
						break;
					}
					
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			session.updateNumberOfModules();
			session.computeSequences();
			session.finalSequence = tempFinalSequence.toArray(new GeneralizedID[tempFinalSequence.size()]);
			return session;
		}

		private static void checkValueDefinitions(KnownModule module, Vector<KnownModule.ValueDefinition> announcedValueDefinitions) {
			for (KnownModule.ValueDefinition announcedVD:announcedValueDefinitions) {
				KnownModule.ValueDefinition knownVD = module.getValueDefinition(announcedVD.uniqueID);
				if (knownVD==null) {
					// TODO
					Debug.Assert(false);
				} else {
					Debug.Assert(knownVD.module==module);
					Debug.Assert(announcedVD.module==module);
					if (!knownVD.label.equals(announcedVD.label) && !announcedVD.label.isEmpty()) {
						// TODO
						Debug.Assert(false);
					}
					if (knownVD.format!=announcedVD.format && announcedVD.format!=null) {
						// TODO
						Debug.Assert(false);
					}
				}
			}
		}

		void saveFile(File file) {
			try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
				out.println("[FinalSequence]");
				for (GeneralizedID id:finalSequence) {
					out.printf("ModuleID=%s%n", id==null ? "" : id.id);
				}
				out.println();
				
				for (GeneralizedID id:sortedID(blocks.keySet())) {
					SessionBlock block = blocks.get(id);
					Debug.Assert(id==block.module.moduleID);
					out.println("[UpgradeModule]");
					out.printf("ModuleID=%s%n", block.module.moduleID.id);
					out.printf("Amount=%d%n", block.amount);
					out.printf("InstallPos=%s%n", toString(block.installPos));
					HashSet<KnownModule.ValueDefinition> usedValueDefinitions = block.collectKnownValueOfInstalledModules();
					for (KnownModule.ValueDefinition vd:sortedVD(usedValueDefinitions)) {
						Debug.Assert(block.module == vd.module);
						out.printf("valueDef.uniqueID=%016X%n", vd.uniqueID);
						out.printf("valueDef.label=%s%n", vd.label);
						if (vd.format!=null) out.printf("valueDef.format=%s%n", vd.format);
					}
					out.println();
					
					for (int i=0; i<block.installedModules.size(); ++i) {
						InstalledUpgrade upgrade = block.installedModules.get(i);
						if (upgrade!=null) {
							Debug.Assert(block.module == upgrade.base);
							out.println("[InstalledUpgrade]");
							out.printf("index=%d%n", i);
							out.printf("upgrade.label1=%s%n", upgrade.label1);
							out.printf("upgrade.label2=%s%n", upgrade.label2);
							for (KnownModule.ValueDefinition vd:sortedVD(block.module.values)) {
								Float value = upgrade.values.get(vd);
								if (value!=null) {
									out.printf("value.uniqueID=%016X%n", vd.uniqueID);
									out.printf(Locale.ENGLISH,"value.value=%1.7e%n", value);
								}
							}
							out.println();
						}
					}
				}
			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		private static Vector<Integer> parseInstallPos(String str) {
			String[] parts = str.split(",");
			Vector<Integer> vec = new Vector<>(parts.length);
			for (String part:parts) {
				if (part.equals("null"))
					vec.add(null);
				else
					try { vec.add(Integer.parseInt(part)); }
					catch (NumberFormatException e) {
						Gui.log_error_ln("Can't parse <block.installPos> as integer: \"%s\" from \"%s\"", part, str);
						vec.add(null);
					}
			}
			return vec;
		}

		private static String toString(Vector<Integer> installPos) {
			Iterable<String> it = ()->installPos.stream().<String>map(i->i==null?"null":i.toString()).iterator();
			return String.join(",",it);
		}

		private static class SessionBlock {
			
			KnownModule module;
			int amount;
			Vector<Integer> installPos;
			Vector<InstalledUpgrade> installedModules;
			
			public SessionBlock(KnownModule module) {
				this.module = module;
				amount = 0;
				installPos = new Vector<>();
				installedModules = new Vector<>();
			}
			public SessionBlock(SessionBlock block) {
				module = block.module;
				amount = block.amount;
				installPos = cloneVector(block.installPos,null);
				installedModules = cloneVector(block.installedModules,InstalledUpgrade::new);
			}
			public Integer getInstallPos(int i, int nModules) {
				if (i>=installPos.size())
					return null;
				
				Integer pos = installPos.get(i);
				if (pos==null || pos<0 || nModules<=pos)
					return null;
				
				return pos;
			}
			public void setInstallPos(int i, Integer newPos) {
				while (i>=installPos.size()) installPos.add(null);
				installPos.set(i, newPos);
			}
			public HashSet<KnownModule.ValueDefinition> collectKnownValueOfInstalledModules() {
				HashSet<KnownModule.ValueDefinition> knownValues = new HashSet<>();
				installedModules.forEach(upgrade->{
					if (upgrade==null) return;
					module.values.forEach(vd->{
						Float value = upgrade.values.get(vd);
						if (value!=null) knownValues.add(vd);
					});
				}); 
				return knownValues;
			}
		}
	}

	private static final class EditSessionDialog extends StandardDialog implements ActionListener {
		private static final long serialVersionUID = 687525027915443491L;
		
		private Session session;
		private Session result;
		private Disabler<ActionCommand> disabler;
		private JPanel contentPane;
		private ModuleTablePanel moduleTablePanel;
		private InstallOrderPanel installOrderPanel;
		private HashMap<GeneralizedID, KnownModule> knownModules;
		

		public EditSessionDialog(Window parent, String title, Session session, HashMap<GeneralizedID, KnownModule> knownModules) {
			super(parent, title);
			this.knownModules = knownModules;
			this.session = session==null ? new Session() : new Session(session);
			this.result = null;
			
			this.session.updateNumberOfModules();
			
			disabler = new Disabler<>();
			disabler.setCareFor(ActionCommand.values());
			
			moduleTablePanel = new ModuleTablePanel();
			installOrderPanel = null;
			
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.weighty = 1;
			JPanel buttonPanel = new JPanel(new GridBagLayout());
			c.weightx = 1;
			buttonPanel.add(new JLabel(),c);
			c.weightx = 0;
			buttonPanel.add(Gui.createButton("Select Modules"   , this, disabler, ActionCommand.SelectModules),c);
			buttonPanel.add(Gui.createButton("Set Install Order", this, disabler, ActionCommand.SetInstallOrder),c);
			buttonPanel.add(Gui.createButton("Finish", this, disabler, ActionCommand.Finish),c);
			buttonPanel.add(Gui.createButton("Cancel", this, disabler, ActionCommand.Cancel),c);
			
			contentPane = new JPanel(new BorderLayout(3,3));
			contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
			contentPane.add(moduleTablePanel,BorderLayout.CENTER);
			contentPane.add(buttonPanel,BorderLayout.SOUTH);
			
			int prefWidth = Arrays.stream(ModuleTableColumnID.values()).mapToInt(columnID->columnID.columnConfig.prefWidth).sum();
			contentPane.setPreferredSize(new Dimension(prefWidth+50,500));
			
			updateButtonAccess();
			createGUI(contentPane);
		}

		private void updateButtonAccess() {
			disabler.setEnable(ac->{
				switch (ac) {
				case Cancel: return true;
				case Finish: return session.nModules>0 && areAllInstallPosDefined();
				
				case SetInstallOrder: return installOrderPanel==null;
				case SelectModules  : return moduleTablePanel ==null;
				}
				return null;
			});
		}

		private boolean areAllInstallPosDefined() {
			for (GeneralizedID id:session.blocks.keySet()) {
				Session.SessionBlock block = session.blocks.get(id);
				for (int i=0; i<block.amount; i++) {
					Integer pos = block.getInstallPos(i,session.nModules);
					if (pos==null)
						return false;
				}
			}
			return true;
		}

		enum ActionCommand { SelectModules, SetInstallOrder, Finish, Cancel }

		@Override
		public void actionPerformed(ActionEvent e) {
			switch (ActionCommand.valueOf(e.getActionCommand())) {
			
			case Cancel: result = null        ; closeDialog(); break;
			case Finish: result = this.session; closeDialog(); break;
			
			case SelectModules:
				if (installOrderPanel!=null) {
					contentPane.remove(installOrderPanel);
					installOrderPanel = null;
				}
				moduleTablePanel = new ModuleTablePanel();
				contentPane.add(moduleTablePanel,BorderLayout.CENTER);
				contentPane.revalidate();
				contentPane.repaint();
				updateButtonAccess();
				break;
				
			case SetInstallOrder:
				if (moduleTablePanel!=null) {
					if (moduleTablePanel.moduleTable.isEditing())
						moduleTablePanel.moduleTable.getCellEditor().stopCellEditing();
					contentPane.remove(moduleTablePanel);
					moduleTablePanel = null;
				}
				installOrderPanel = new InstallOrderPanel();
				contentPane.add(installOrderPanel,BorderLayout.CENTER);
				contentPane.revalidate();
				contentPane.repaint();
				updateButtonAccess();
				break;
			}
		}
		
		private enum ModuleTableColumnID implements SimplifiedColumnIDInterface {
			ID    ("ID"    ,        String.class, 20,-1,120,120),
			Type  ("Type"  ,        String.class, 20,-1,200,200),
			Label ("Name"  , GeneralizedID.class, 20,-1,200,200),
			Amount("Amount",       Integer.class, 20,-1, 50, 50),
			;
			
			private SimplifiedColumnConfig columnConfig;
			
			ModuleTableColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				columnConfig = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			@Override public SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
		}

		private class ModuleTablePanel extends JScrollPane {
			private static final long serialVersionUID = 4398192133264858213L;

			private TableView.SimplifiedTable<ModuleTableColumnID> moduleTable;
			
			ModuleTablePanel() {
				super();
				
				ModuleTableModel moduleTableModel = new ModuleTableModel();
				moduleTable = new TableView.SimplifiedTable<>("", moduleTableModel, true, SaveViewer.DEBUG, true);
				setViewportView(moduleTable);
				
				ComboboxCellEditor<GeneralizedID> cellEditor =
						new ComboboxCellEditor<GeneralizedID>(SaveViewer.addNull(sortedID(knownModules.keySet())));
				
				NonStringRenderer<GeneralizedID> renderer =
						new NonStringRenderer<GeneralizedID>(obj->{
							if (obj instanceof GeneralizedID)
								return getLabelOrID((GeneralizedID) obj);
							return "<none>";
						});
				cellEditor.setRenderer(renderer);
				moduleTable.setCellEditor  (ModuleTableColumnID.Label, cellEditor);
				moduleTable.setCellRenderer(ModuleTableColumnID.Label, renderer);
			}

			private class ModuleTableModel extends Tables.SimplifiedTableModel<ModuleTableColumnID> {
			
				private Vector<GeneralizedID> orderedIDs;
			
				public ModuleTableModel() {
					super(ModuleTableColumnID.values());
					orderedIDs = sortedID(session.blocks.keySet());
				}
			
				@Override public int getRowCount() { return orderedIDs.size()+1; }
			
				@Override
				protected boolean isCellEditable(int rowIndex, int columnIndex, ModuleTableColumnID columnID) {
					switch (columnID) {
					case ID:
					case Type:
						return false;
						
					case Label:
						return true;
						
					case Amount:
						return rowIndex<orderedIDs.size();
					}
					return false;
				}
			
				@Override
				public Object getValueAt(int rowIndex, int columnIndex, ModuleTableColumnID columnID) {
					if (rowIndex>=orderedIDs.size()) {
						switch (columnID) {
						case Amount: return session.nModules;
						default: break;
						}
						return null;
					}
					
					GeneralizedID id = orderedIDs.get(rowIndex);
					Session.SessionBlock block = session.blocks.get(id);
					Debug.Assert(id==block.module.moduleID);
					
					switch (columnID) {
					case ID    : return id.id;
					case Type  : return id.type==null ? null : id.type.label;
					case Label : return id;
					case Amount: return block.amount;
					}
					return null;
				}
			
				@Override
				protected void setValueAt(Object aValue, int rowIndex, int columnIndex, ModuleTableColumnID columnID) {
					if (rowIndex>=orderedIDs.size()) {
						Debug.Assert(columnID==ModuleTableColumnID.Label);
						if (aValue instanceof GeneralizedID) {
							GeneralizedID id = (GeneralizedID) aValue;
							int row = orderedIDs.indexOf(id);
							if (row>=0)
								JOptionPane.showMessageDialog(table, "You can't select a module twice. (-> Row "+(row+1)+")", "Module already selected", JOptionPane.ERROR_MESSAGE);
							else {
								orderedIDs.add(id);
								KnownModule module = knownModules.get(id);
								Debug.Assert(module!=null);
								Debug.Assert(id==module.moduleID);
								session.blocks.put(id, new Session.SessionBlock(module));
								fireTableRowAdded(orderedIDs.size()-1);
							}
						}
						return;
					}
					
					switch (columnID) {
					
					case ID:
					case Type:
						Debug.Assert(false);
						break;
					
					case Label:
						if (aValue instanceof GeneralizedID) {
							GeneralizedID id = (GeneralizedID) aValue;
							int row = orderedIDs.indexOf(id);
							if (row>=0 && row==rowIndex) row = orderedIDs.indexOf(id,rowIndex+1);
							if (row>=0)
								JOptionPane.showMessageDialog(table, "You can't select a module twice. (-> Row "+(row+1)+")", "Module already selected", JOptionPane.ERROR_MESSAGE);
							else {
								GeneralizedID oldID = orderedIDs.get(rowIndex);
								if (oldID!=id) {
									session.blocks.remove(oldID);
									orderedIDs.set(rowIndex,id);
									KnownModule module = knownModules.get(id);
									session.blocks.put(id, new Session.SessionBlock(module));
									fireTableRowUpdate(rowIndex);
								}
							}
						} else if (aValue==null) {
							GeneralizedID id = orderedIDs.get(rowIndex);
							Debug.Assert(id!=null);
							orderedIDs.remove(rowIndex);
							session.blocks.remove(id);
							fireTableRowRemoved(rowIndex);
						}
						fireTableRowUpdate(orderedIDs.size());
						session.updateNumberOfModules();
						updateButtonAccess();
						break;
						
					case Amount: {
						GeneralizedID id = orderedIDs.get(rowIndex);
						Debug.Assert(id!=null);
						Session.SessionBlock block = session.blocks.get(id);
						Debug.Assert(block!=null);
						block.amount = aValue==null ? 0 : ((Integer)aValue).intValue();
						session.updateNumberOfModules();
						updateButtonAccess();
						fireTableRowUpdate(orderedIDs.size());
					} break;
					}
				}
				
			}
		}
		
		private class InstallOrderPanel extends JPanel {
			private static final long serialVersionUID = 2931295900938287288L;
			
			private JTextArea sequencesOutput;

			InstallOrderPanel() {
				super(new GridBagLayout());
				setBorder(BorderFactory.createLineBorder(Color.GRAY));
				GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.BOTH;
				c.insets = new Insets(2, 2, 2, 2);
				
				session.updateNumberOfModules();
				
				JRadioButton[][] btns = new JRadioButton[session.nModules][session.nModules];
				
				c.weighty = 0;
				c.gridy = 0;
				c.gridx = 1;
				add(createLabel("Unset", true),c);
				for (int i=0; i<session.nModules; i++) {
					c.gridx = i+2;
					add(createLabel("("+(i+1)+")", true),c);
				}
				int row = 0;
				for (GeneralizedID id:sortedID(session.blocks.keySet())) {
					Session.SessionBlock block = session.blocks.get(id);
					Debug.Assert(id==block.module.moduleID);
					for (int i=0; i<block.amount; i++, row++) {
						int index = i;
						c.gridy = row+1;
						c.weightx = 0;
						
						c.gridx = 0;
						add(createLabel("("+(i+1)+") "+block.module.moduleID.label, false),c);
						
						c.gridx = 1;
						ButtonGroup bg = new ButtonGroup();
						Integer pos = block.getInstallPos(i,session.nModules);
						add(Gui.createRadioButton("", bg, pos==null, true, e->setOrder(block,index,null,btns)),c);
						
						for (int col=0; col<session.nModules; col++) {
							int newPos = col;
							c.gridx = col+2;
							boolean isSelected = pos!=null && pos.intValue()==col;
							add(btns[col][row] = Gui.createRadioButton("", bg, isSelected, true, e->setOrder(block,index,newPos,btns)),c);
						}
						c.weightx = 1;
						c.gridx = session.nModules+2;
						add(new JLabel(),c);
					}
				}
				c.gridy = session.nModules+1;
				c.gridx = 0;
				c.weightx = 1;
				c.weighty = 1;
				c.gridwidth = GridBagConstraints.REMAINDER;
				sequencesOutput = new JTextArea();
				add(new JScrollPane(sequencesOutput),c);
				
				for (GeneralizedID id:session.blocks.keySet()) {
					Session.SessionBlock block = session.blocks.get(id);
					Debug.Assert(id==block.module.moduleID);
					for (int i=0; i<block.amount; i++) {
						Integer pos = block.getInstallPos(i,session.nModules);
						if (pos!=null)
							setEnabled(btns[pos],false);
					}
				}
				
				computeSequences();
			}

			private void setEnabled(JRadioButton[] btns, boolean enabled) {
				for (JRadioButton btn:btns) btn.setEnabled(enabled);
			}

			private void setOrder(Session.SessionBlock block, int i, Integer newPos, JRadioButton[][] btns) {
				Integer pos = block.getInstallPos(i,session.nModules);
				if (pos!=null)
					setEnabled(btns[pos],true);
				
				block.setInstallPos(i, newPos);
				if (newPos!=null)
					setEnabled(btns[newPos],false);
				
				computeSequences();
				updateButtonAccess();
			}

			private Component createLabel(String string, boolean centered) {
				return new JLabel(string, centered ? JLabel.CENTER : JLabel.LEFT);
			}

			private void computeSequences() {
				
				session.computeSequences();
				
				if (session.sequences==null) {
					sequencesOutput.setText("Finalize install order to determine number of cycles.");
					return;
				}
				
				sequencesOutput.setText("");
				
				Gui.append_ln(sequencesOutput, "Number of cycles: %d", session.sequences.length);
				
				Vector<GeneralizedID> sortedIDs = sortedID(session.blocks.keySet());
				Gui.append_ln(sequencesOutput, "Modules:");
				for (int i=0; i<sortedIDs.size(); ++i) {
					GeneralizedID id = sortedIDs.get(i);
					Debug.Assert(id!=null);
					int amount = session.blocks.get(id).amount;
					Gui.append_ln(sequencesOutput, "   %2d  :  %s  (%dx)", i+1, id.toString(), amount);
				}
				Gui.append_ln(sequencesOutput, "    ##  Dummy Module");
				
				Gui.append_ln(sequencesOutput, "Sequences:  [%d]", session.sequences.length);
				for (int i=0; i<session.sequences.length; ++i) {
					Gui.append(sequencesOutput, "[%2d] ", i+1);
					String str = "";
					for (int j=0; j<session.sequences[i].length; ++j) {
						GeneralizedID id = session.sequences[i][j];
						if (id==null) {
							if (!str.isEmpty()) str += ' ';
							str += "##"; 
						} else {
							if (!str.isEmpty()) str += ' ';
							int index = sortedIDs.indexOf(id);
							Debug.Assert(index>=0);
							str += String.format("%2d", index+1); 
						}
					}
					Gui.append_ln(sequencesOutput, str);
				}
			}
		}

		public Session getResult() {
			return result;
		}
	}

	private static final class ValueDefDialog extends StandardDialog {
		private static final long serialVersionUID = 6065280492964277262L;
		
		private KnownModule.ValueDefinition tempVD;
		private KnownModule.ValueDefinition result;

		public ValueDefDialog(Window parent, String title, KnownModule.ValueDefinition tempVD) {
			super(parent, title);
			this.tempVD = new KnownModule.ValueDefinition(tempVD,0);
			this.result = null;
			
			JTextField txtfldModule = new JTextField(40);
			txtfldModule.setEditable(false);
			txtfldModule.setText(this.tempVD.module.moduleID.getName());
			
			JTextField txtfldLabel = Gui.createTextField(this.tempVD.label, (String str)->this.tempVD.label = str);
			JTextField txtfldMin   = Gui.createTextField(this.tempVD.min==null?"":String.format(Locale.ENGLISH,"%1.3f",this.tempVD.min), (String str)->this.tempVD.min = parseFloat(str));
			JTextField txtfldMax   = Gui.createTextField(this.tempVD.max==null?"":String.format(Locale.ENGLISH,"%1.3f",this.tempVD.max), (String str)->this.tempVD.max = parseFloat(str));
			
			JComboBox<KnownModule.ValueDefinition.Format> cmbbxFormat = new JComboBox<>(KnownModule.ValueDefinition.Format.values());
			cmbbxFormat.setSelectedItem(this.tempVD.format);
			Gui.setComp(cmbbxFormat, null, null, true, format->this.tempVD.format = format);
			
			JPanel contentPane = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weighty = 0;
			c.weightx = 0; c.gridx=0;
			c.gridy=0; contentPane.add(new JLabel("Module: "),c);
			c.gridy=1; contentPane.add(new JLabel("Label: "),c);
			c.gridy=2; c.gridx=0; contentPane.add(new JLabel("Format: "),c);
			c.gridy=2; c.gridx=2; contentPane.add(new JLabel("  Min: "),c);
			c.gridy=2; c.gridx=4; contentPane.add(new JLabel("  Max: "),c);
			
			c.weightx = 1; c.gridx=1;
			c.gridwidth = 5;
			c.gridy=0; contentPane.add(txtfldModule,c);
			c.gridy=1; contentPane.add(txtfldLabel,c);
			c.gridwidth = 1;
			c.gridy=2; c.gridx=1; contentPane.add(cmbbxFormat,c);
			c.gridy=2; c.gridx=3; contentPane.add(txtfldMin,c);
			c.gridy=2; c.gridx=5; contentPane.add(txtfldMax,c);
			
			createGUI(
				contentPane,
				Gui.createButton("Ok"    , e->{ result=this.tempVD; closeDialog(); }),
				Gui.createButton("Cancel", e->{ result=null       ; closeDialog(); })
			);
		}

		private Float parseFloat(String str) {
			try {
				return Float.parseFloat(str);
			} catch (NumberFormatException e) {
				return null;
			}
		}

		public KnownModule.ValueDefinition getResult() {
			return result;
		}
	
	}

	private final class ModulePanel extends JPanel {
		private static final long serialVersionUID = -1655733521106002871L;

		ModulePanel() {
			super(new GridBagLayout());
		}

		public void updateModules(Vector<GeneralizedID> modules) {
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.weightx = 1;
			c.insets = new Insets(0,5,2,5);
			removeAll();
			for (int i=0; i<modules.size(); i++) {
				GeneralizedID module = modules.get(i);
				String text = String.format("[%d] %s", i+1, module.getName());
				JLabel label = new JLabel(text,JLabel.LEFT);
				add(label,c);
			}
			revalidate();
			repaint();
		}
	}

	private enum TableContextMenuCommands { AddValueDefinition, EditValueDefinition, RemoveValueDefinition, AddAllToMinMax, AddColumnToMinMax  }

	private final class TablePanel extends JScrollPane {
		private static final long serialVersionUID = -1092343150495411023L;
		
		private JPanel tablePanel;
		private TableContextMenu tableContextMenu;
		private HashMap<GeneralizedID, InstalledModulesTableModel> tables;
		private boolean defineFinalInstallation;
		
		TablePanel() {
			tables = new HashMap<>();
			tableContextMenu = new TableContextMenu();
			tablePanel = new JPanel(new GridLayout(0,1,3,3));
			setViewportView(tablePanel);
			setPreferredSize(new Dimension(600,500));
			defineFinalInstallation = false;
		}
	
		public boolean isDefiningFinalInstallation() {
			return defineFinalInstallation;
		}
		public void defineFinalInstallation(boolean b) {
			defineFinalInstallation = b;
			tables.forEach((id,tableModel)->tableModel.defineFinalInstallation(b));
		}
		public void setSelectionColoring(boolean b) {
			tables.forEach((id,tableModel)->tableModel.setSelectionColoring(b));
		}
		public void showValuePriorities(boolean b) {
			tables.forEach((id,tableModel)->tableModel.showValuePriorities(b));
		}
		public void setValueColoringMinMax(boolean b) {
			tables.forEach((id,tableModel)->tableModel.setValueColoringMinMax(b));
		}
		public void clearCurrentInstallTestModule() {
			tables.forEach((id,tableModel)->tableModel.clearCurrentInstallTestModule());
		}
		public void setCurrentInstallTestModule(GeneralizedID currentModule, int currentModuleIndex) {
			tables.forEach((id,tableModel)->tableModel.setCurrentInstallTestModule(id==currentModule ? currentModuleIndex : -1));
		}
		private void updateAfterChangeOnFinalSequence() {
			tables.forEach((id,tableModel)->tableModel.fireTableUpdate());
			finalSequenceTableModel.fireTableUpdate();
		}

		private void updateTables() {
			tablePanel.removeAll();
			tables.clear();
			
			if (currentSession==null) return;
			
			for (GeneralizedID id:sortedID(currentSession.blocks.keySet())) {
				Debug.Assert(id!=null);
				Session.SessionBlock block = currentSession.blocks.get(id);
				
				InstalledModulesTableModel tableModel = new InstalledModulesTableModel(
					block,currentSession.finalSequence,currentSession.nModules,
					new LabelCellEditor(id,upgrade->upgrade==null?null:upgrade.label1),
					new LabelCellEditor(id,upgrade->upgrade==null?null:upgrade.label2),
					new InstalledModulesTableModel.Updater() {
						@Override public void finalSequenceChanged() { updateAfterChangeOnFinalSequence(); }
						@Override public void prioChanged(GeneralizedID id) { finalSequenceTableModel.updatePrioColumn(id); }
					}
				);
				tables.put(id, tableModel);
				
				JTable table = new JTable(tableModel);
				table.addMouseListener(new MouseAdapter() {
					@Override public void mouseClicked(MouseEvent e) {
						if (e.getButton()==MouseEvent.BUTTON3) {
							int col = table.columnAtPoint(e.getPoint());
							int row = table.rowAtPoint(e.getPoint());
							tableContextMenu.setBlock(id,row,col);
							tableContextMenu.show(table, e.getX(), e.getY());
						}
					}
				});
				table.addFocusListener(new FocusListener() {
					@Override public void focusLost  (FocusEvent e) { if (DEBUG_EVENTS) showEvent("JTable("+id.id+")", "focusLost()"); }
					@Override public void focusGained(FocusEvent e) { if (DEBUG_EVENTS) showEvent("JTable("+id.id+")", "focusGained()");
						tables.forEach((id,model)->{ if (tableModel!=model) model.stopEditing(); });
					}
				});
				table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
				table.setPreferredScrollableViewportSize(table.getPreferredSize());
				table.setAutoCreateRowSorter(true);
				
				tableModel.setTable(table);
				
				JScrollPane tableScrollPane = new JScrollPane(table);
				tableScrollPane.setWheelScrollingEnabled(false);
				tableScrollPane.addMouseWheelListener(e->scrollTables(e.getPreciseWheelRotation()));
				tableScrollPane.setBorder( Gui.createTitledBorderForScrollPane(id.getName()) );
				
				tablePanel.add(tableScrollPane);
			}
			tablePanel.revalidate();
			tablePanel.repaint();
		}
	
		private void scrollTables(double wheelRotation) {
			//System.out.println("WheelRotation: "+wheelRotation);
			JScrollBar scrollBar = getVerticalScrollBar();
			if (scrollBar!=null) {
				int minimum = scrollBar.getMinimum();
				int maximum = scrollBar.getMaximum();
				int visibleAmount = scrollBar.getVisibleAmount();
				int value = scrollBar.getValue();
				value += (int)(wheelRotation*20);
				value = Math.min(Math.max(minimum, value), maximum-visibleAmount);
				scrollBar.setValue(value);
			}
		}
	
		private class TableContextMenu extends JPopupMenu implements ActionListener {
			private static final long serialVersionUID = -8902930861450278308L;
			
			private JMenuItem miAddColumnToMinMax;
			
			private JMenuItem miAddValue;
			private JMenuItem miEditValue;
			private JMenuItem miRemoveValue;
			
			private GeneralizedID id = null;
			private Session.SessionBlock block = null;
			private KnownModule.ValueDefinition vd = null;
			private InstalledModulesTableModel tableModel = null;
			@SuppressWarnings("unused")
			private int rowIndex = -1;
			private int columnIndex = -1;

	
			TableContextMenu() {
				super("TableContextMenu");
				add(                      Gui.createMenuItem("Add All Values to respective Value Ranges", this, TableContextMenuCommands.AddAllToMinMax   ));
				add(miAddColumnToMinMax = Gui.createMenuItem("Add Values of this Column to Value Range" , this, TableContextMenuCommands.AddColumnToMinMax));
				addSeparator();;
				add(miAddValue    = Gui.createMenuItem("", this, TableContextMenuCommands.AddValueDefinition   ));
				add(miEditValue   = Gui.createMenuItem("", this, TableContextMenuCommands.EditValueDefinition  ));
				add(miRemoveValue = Gui.createMenuItem("", this, TableContextMenuCommands.RemoveValueDefinition));
			}
		
			public void setBlock(GeneralizedID id, int rowIndex, int columnIndex) {
				this.id = id;
				this.rowIndex = rowIndex;
				this.columnIndex = columnIndex;
				Debug.Assert(id!=null);
				block = currentSession==null ? null : currentSession.blocks.get(id);
				tableModel = tables.get(id);
				Debug.Assert(tableModel!=null);
				vd = tableModel.getVD(columnIndex);
				
				miAddColumnToMinMax.setEnabled(vd!=null);
				miAddValue   .setText(String.format("Add New Value Definition to %s", id.getName()));
				miEditValue  .setText(String.format("Edit [%s] of %s", vd==null?"??":vd.label, id.getName()));
				miEditValue  .setEnabled(vd!=null);
				miRemoveValue.setText(String.format("Remove [%s] of %s", vd==null?"??":vd.label, id.getName()));
				miRemoveValue.setEnabled(vd!=null);
			}
	
			@Override
			public void actionPerformed(ActionEvent e) {
				ValueDefDialog dlg;
				KnownModule.ValueDefinition resultVD;
				switch (TableContextMenuCommands.valueOf(e.getActionCommand())) {
				case AddValueDefinition:
					if (block!=null) {
						dlg = new ValueDefDialog(mainwindow,"Add Value",new KnownModule.ValueDefinition(block.module,0));
						dlg.showDialog();
						resultVD = dlg.getResult();
						if (resultVD!=null) {
							KnownModule.ValueDefinition newVD = new KnownModule.ValueDefinition(block.module);
							newVD.setValues(resultVD);
							block.module.values.add(newVD);
							tableModel.fireTableHeaderAdded();
							writeConfig();
						}
					}
					break;
					
				case EditValueDefinition:
					if (block!=null && vd!=null) {
						dlg = new ValueDefDialog(mainwindow,"Edit Value",new KnownModule.ValueDefinition(vd,0));
						dlg.showDialog();
						resultVD = dlg.getResult();
						if (resultVD!=null) {
							vd.setValues(resultVD);
							tableModel.fireTableHeaderChanged(columnIndex);
							writeConfig();
						}
					}
					break;
					
				case RemoveValueDefinition:
					if (block!=null && vd!=null) {
						String msg = String.format("Do you really want to remove Value %s of %s?", vd==null?"??":vd.label, id.getName());
						if (JOptionPane.showConfirmDialog(mainwindow, msg, "Remove Value", JOptionPane.YES_NO_CANCEL_OPTION)==JOptionPane.YES_OPTION) {
							block.module.values.remove(vd);
							tableModel.fireTableHeaderRemoved(columnIndex);
							writeConfig();
						}
					}
					break;
					
				case AddAllToMinMax:
					if (block!=null) {
						block.module.values.forEach(vd->addToMinMax(block.installedModules,vd));
						tableModel.fireTableUpdate();
						writeConfig();
					}
					break;
					
				case AddColumnToMinMax:
					if (block!=null && vd!=null) {
						addToMinMax(block.installedModules,vd);
						tableModel.fireTableColumnChanged(columnIndex);
						writeConfig();
					}
					break;
				}
			}

			private void addToMinMax(Vector<InstalledUpgrade> modules, KnownModule.ValueDefinition vd) {
				Debug.Assert(vd!=null);
				modules.forEach(upgrade->{
					if (upgrade!=null) {
						Float value = upgrade.values.get(vd);
						if (value!=null) {
							vd.min = vd.min==null ? value : Math.min(vd.min, value);
							vd.max = vd.max==null ? value : Math.max(vd.max, value);
						}
					}
				});
			}
		}

		private class LabelCellEditor implements TableCellEditor {
			
			private TextFieldWithSuggestions editorComp;
			private Vector<CellEditorListener> cellEditorListeners;
			private Object result;
			private Function<InstalledUpgrade, String> getString;

			private GeneralizedID id;

			LabelCellEditor(GeneralizedID id, Function<InstalledUpgrade,String> getString) {
				this.id = id;
				this.getString = getString;
				editorComp = new Gui.TextFieldWithSuggestions(1, this::collectSuggestions, this::selectFinally);
				editorComp.addActionListener(e->selectFinally(editorComp.getText()));
				editorComp.addFocusListener(new FocusListener() {
					@Override public void focusGained(FocusEvent e) { if (DEBUG_EVENTS) showEvent("focusGained()"); }
					@Override public void focusLost  (FocusEvent e) { if (DEBUG_EVENTS) showEvent("focusLost()%s", !editorComp.isShowing()?" -> hideList":"" );
						if (!editorComp.isShowing()) editorComp.hideList();
					}
				});
				cellEditorListeners = new Vector<>();
				result = null;
			}
			
			private void showEvent(String format, Object... args) {
				UpgradeModuleInstallHelper.showEvent("LabelCellEditor("+id.id+")", format, args);
			}
			
			@Override
			public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
				if (DEBUG_EVENTS) showEvent("getTableCellEditorComponent()");
				result = value;
				editorComp.setText( value==null ? null : value.toString(), true );
				return editorComp;
			}

			@Override
			public boolean stopCellEditing() {
				if (DEBUG_EVENTS) showEvent("stopCellEditing()");
				selectFinally(editorComp.getText());
				return true;
			}

			@Override
			public void cancelCellEditing() {
				if (DEBUG_EVENTS) showEvent("cancelCellEditing()");
				editorComp.hideList();
				fireEditingCanceledEvent();
			}

			private void selectFinally(String str) {
				if (DEBUG_EVENTS) showEvent("selectFinally( \"%s\" )", str);
				result = str;
				editorComp.hideList();
				fireEditingStoppedEvent();
			}

			private HashSet<String> collectSuggestions(String str) {
				String lowerCaseStr = str.toLowerCase();
				HashSet<String> suggestions = new HashSet<>();
				if (currentSession!=null) {
					currentSession.blocks.forEach((id,block)->{
						block.installedModules.forEach(upgrade->{
							String upgradeStr = getString.apply(upgrade);
							if (upgradeStr!=null && upgradeStr.toLowerCase().contains(lowerCaseStr))
								suggestions.add(upgradeStr);
						});
					}); 
				}
				return suggestions;
			}

			@Override public Object  getCellEditorValue()                  { if (DEBUG_EVENTS) showEvent("getCellEditorValue()"); return result; }
			@Override public boolean isCellEditable  (EventObject anEvent) { if (DEBUG_EVENTS) showEvent("isCellEditable()"); return true; }
			@Override public boolean shouldSelectCell(EventObject anEvent) { if (DEBUG_EVENTS) showEvent("shouldSelectCell()"); return true; }
		
			@Override public void    addCellEditorListener(CellEditorListener l) { if (DEBUG_EVENTS) showEvent("addCellEditorListener()"); cellEditorListeners.   add(l); }
			@Override public void removeCellEditorListener(CellEditorListener l) { if (DEBUG_EVENTS) showEvent("removeCellEditorListener()"); cellEditorListeners.remove(l); }
			
			private void notifyCellEditorListeners(BiConsumer<CellEditorListener,ChangeEvent> action) {
				ChangeEvent e = new ChangeEvent(this);
				for (int i=0; i<cellEditorListeners.size(); i++)
					action.accept(cellEditorListeners.get(i),e);
			}
			private void fireEditingCanceledEvent() { notifyCellEditorListeners(CellEditorListener::editingCanceled); }
			private void fireEditingStoppedEvent () { notifyCellEditorListeners(CellEditorListener::editingStopped); }
		
		}
	}

	private static class ColorRange {
		
		private Vector<ColorPos> range;
		
		public ColorRange() {
			range = new Vector<>();
		}

		public void add(float f, Color color) {
			Debug.Assert(color!=null);
			Debug.Assert(!Float.isNaN(f));
			range.add(new ColorPos(f,color));
			range.sort(Comparator.<ColorPos>comparingDouble(cp->cp.f));
		}

		public Color computeColor(float f) {
			if (range.isEmpty()) return null;
			if (f >= range. lastElement().f) return range. lastElement().color;
			if (f <= range.firstElement().f) return range.firstElement().color;
			for (int i=1; i<range.size(); ++i) {
				ColorPos prevPos = range.get(i-1);
				ColorPos nextPos = range.get(i);
				if (f==nextPos.f) return nextPos.color;
				if (f>nextPos.f) continue;
				return computeColor(f,prevPos.f,prevPos.color,nextPos.f,nextPos.color);
			}
			return null;
		}
		
		private Color computeColor(float f, float f1, Color c1, float f2, Color c2) {
			float f0 = (f-f1)/(f2-f1);
			f0 = Math.min(Math.max(0, f0), 1);
			float[] rgb1 = c1.getRGBColorComponents(new float[3]);
			float[] rgb2 = c2.getRGBColorComponents(new float[3]);
			rgb1[0] = (rgb2[0]-rgb1[0])*f0 + rgb1[0];
			rgb1[1] = (rgb2[1]-rgb1[1])*f0 + rgb1[1];
			rgb1[2] = (rgb2[2]-rgb1[2])*f0 + rgb1[2];
			return new Color(rgb1[0], rgb1[1], rgb1[2]);
		}

		private static class ColorPos {
			Color color;
			float f;
			public ColorPos(float f, Color color) {
				this.f = f;
				this.color = color;
			}
		}
	}

	private static abstract class AbstractTableModel implements TableModel {
		
		private Vector<TableModelListener> tableModelListeners;
		protected JTable table;
		
		protected AbstractTableModel() {
			tableModelListeners = new Vector<>();
			table = null;
		}
		
		public void setTable(JTable table) {
			this.table = table;
			prepareTable();
		}
		
		protected void forEachColumn(BiConsumer<TableColumn,Integer> action) {
			TableColumnModel columnModel = table.getColumnModel();
			for (int i=0; i<columnModel.getColumnCount(); ++i) {
				TableColumn column = columnModel.getColumn(i);
				action.accept(column, column.getModelIndex());
			}
		}
		
		protected abstract void prepareTable();
		protected abstract int getPrefColumnWidth(int columnIndex);
		
		public void setColumnWidths() {
			forEachColumn((column, columnIndex) -> {
				if (column==null) return;
				int prefWidth = getPrefColumnWidth(columnIndex);
				setColumnWidths(column, 20, prefWidth, prefWidth);
			});
		}

		protected void setColumnWidths(TableColumn column, int minWidth, int prefWidth, int currentWidth) {
			column.setMinWidth(minWidth);
			column.setPreferredWidth(prefWidth);
			column.setWidth(currentWidth);
		}

		@Override public void    addTableModelListener(TableModelListener l) { tableModelListeners.   add(l); }
		@Override public void removeTableModelListener(TableModelListener l) { tableModelListeners.remove(l); }
		
		private void fireTableEvent(TableModelEvent e) {
			tableModelListeners.forEach(l->l.tableChanged(e));
		}
		private TableModelEvent createTableModelEvent() {
			return new TableModelEvent(this);
		}
	
		private TableModelEvent createTableModelEvent(int firstRow, int lastRow, int column, int type) {
			//String typeStr;
			//switch (type) {
			//case TableModelEvent.UPDATE: typeStr = "UPDATE"; break;
			//case TableModelEvent.INSERT: typeStr = "INSERT"; break;
			//case TableModelEvent.DELETE: typeStr = "DELETE"; break;
			//default: typeStr = "["+type+"]"; break;
			//}
			//System.out.printf("new TableModelEvent(this, firstRow:%d, lastRow:%d, column:%d, type:%s)%n", firstRow,lastRow,column,typeStr);
			return new TableModelEvent(this,firstRow,lastRow,column,type);
		}
	
		public void fireTableUpdate() {
			fireTableEvent(createTableModelEvent());
		}
		
		// TableRowEvent
		private void fireTableRowEvent(int rowIndex, int type) {
			fireTableEvent(createTableModelEvent(rowIndex,rowIndex,TableModelEvent.ALL_COLUMNS,type));
		}
		@SuppressWarnings("unused")
		public void fireTableRowChanged(int rowIndex) {
			fireTableRowEvent(rowIndex,TableModelEvent.UPDATE);
		}
		
		// TableCellEvent
		private void fireTableCellEvent(int rowIndex, int columnIndex, int type) {
			fireTableEvent(createTableModelEvent(rowIndex,rowIndex,columnIndex,type));
		}
		@SuppressWarnings("unused")
		public void fireTableCellChanged(int rowIndex, int columnIndex) {
			fireTableCellEvent(rowIndex,columnIndex,TableModelEvent.UPDATE);
		}
		
		// TableColumnEvent
		private void fireTableColumnEvent(int columnIndex, int type) {
			fireTableEvent(createTableModelEvent(0,getRowCount()-1,columnIndex,type));
		}
		@SuppressWarnings("unused")
		public void fireTableColumnRemoved(int columnIndex) {
			fireTableColumnEvent(columnIndex,TableModelEvent.DELETE);
		}
		public void fireTableColumnChanged(int columnIndex) {
			fireTableColumnEvent(columnIndex,TableModelEvent.UPDATE);
		}
		@SuppressWarnings("unused")
		public void fireTableColumnInserted(int columnIndex) {
			fireTableColumnEvent(columnIndex,TableModelEvent.INSERT);
		}
		
		private void fireTableHeaderEvent(int columnIndex, int type) {
			fireTableEvent(createTableModelEvent(TableModelEvent.HEADER_ROW,TableModelEvent.HEADER_ROW,columnIndex,type));
			prepareTable();
		}
		public void fireTableHeaderChanged(int columnIndex) {
			fireTableHeaderEvent(columnIndex, TableModelEvent.UPDATE);
		}
		public void fireTableHeaderRemoved(int columnIndex) {
			fireTableHeaderEvent(columnIndex, TableModelEvent.DELETE);
		}
		public void fireTableHeaderAdded() {
			fireTableHeaderEvent(getColumnCount()-1, TableModelEvent.INSERT);
		}
		public void fireTableHeaderChanged() {
			fireTableHeaderChanged(TableModelEvent.ALL_COLUMNS);
		}
		public void fireTableStructureUpdate() {
			fireTableHeaderChanged();
		}
	}

	private static final class InstalledModulesTableModel extends AbstractTableModel {
	
		private static final String CELLEDITORVALUE_ACTIVATED = "akt.";
		private static final String CELLEDITORVALUE_NOTACTIVATED = "";
		private static final int COLUMN_INDEX     = 0;
		private static final int COLUMN_LABEL1    = COLUMN_INDEX +1;
		private static final int COLUMN_LABEL2    = COLUMN_LABEL1+1;
		private static final int STANDARD_COLUMNS = COLUMN_LABEL2+1;
		
		private Session.SessionBlock block;
		private GeneralizedID[] finalSequence;
		private int nModules;
		
		private CellwiseCellEditor cellwiseCellEditor;
		private TableCellEditor label1CellEditor;
		private TableCellEditor label2CellEditor;
		private TableCellEditor cellEditor_Activated;
		private TableCellEditor cellEditor_TextField;
		
		private MyTableCellRenderer defaultTableCellRenderer;
		
		private int currentInstallTestModule;
		private boolean installTestAreRunning;
		private boolean defineFinalInstallation;
		private DefaultCellEditor cellEditor_Checkbox;
		private Updater updater;
		private boolean isValueColoringMinMax;
		private boolean showValuePriorities;
		private boolean isSelectionColoring;
		
		public interface Updater {
			void prioChanged(GeneralizedID id);
			void finalSequenceChanged();
		}
		
		public InstalledModulesTableModel(Session.SessionBlock block, GeneralizedID[] finalSequence, int nModules, TablePanel.LabelCellEditor label1CellEditor, TablePanel.LabelCellEditor label2CellEditor, Updater updater) {
			this.block = block;
			this.finalSequence = finalSequence;
			this.nModules = nModules;
			this.label1CellEditor = label1CellEditor;
			this.label2CellEditor = label2CellEditor;
			this.updater = updater;
			
			cellEditor_Activated = new DefaultCellEditor(new JComboBox<>( new String[] {CELLEDITORVALUE_ACTIVATED,CELLEDITORVALUE_NOTACTIVATED} ));
			cellEditor_TextField = new DefaultCellEditor(new JTextField());
			JCheckBox checkBox = new JCheckBox(); checkBox.setHorizontalAlignment(JCheckBox.CENTER);
			cellEditor_Checkbox = new DefaultCellEditor(checkBox);
			defaultTableCellRenderer = new MyTableCellRenderer();
			
			cellwiseCellEditor = new Tables.CellwiseCellEditor(this::getCellEditor);
			
			installTestAreRunning = false;
			currentInstallTestModule = -1;
			defineFinalInstallation = false;
			isValueColoringMinMax = false;
			showValuePriorities = false;
			isSelectionColoring = true;
		}
	
		public void defineFinalInstallation(boolean b) {
			this.defineFinalInstallation = b;
			fireTableStructureUpdate();
		}
		public void showValuePriorities(boolean b) {
			this.showValuePriorities = b;
			fireTableStructureUpdate();
		}
		public void setValueColoringMinMax(boolean b) {
			this.isValueColoringMinMax = b;
			table.repaint();
		}
		public void setSelectionColoring(boolean b) {
			this.isSelectionColoring = b;
			table.repaint();
		}

		public void stopEditing() {
			if (table.isEditing()) {
				TableCellEditor cellEditor = table.getCellEditor();
				if (cellEditor!=null)
					cellEditor.stopCellEditing();
			}
		}

		public void clearCurrentInstallTestModule() {
			boolean stoppingInstallTests = installTestAreRunning;
			installTestAreRunning = false;
			currentInstallTestModule = -1;
			if ( (defineFinalInstallation || showValuePriorities) && stoppingInstallTests ) fireTableStructureUpdate();
			else table.repaint();
		}

		public void setCurrentInstallTestModule(int currentModule) {
			boolean startingInstallTests = !installTestAreRunning;
			installTestAreRunning = true;
			currentInstallTestModule = currentModule;
			if ( (defineFinalInstallation || showValuePriorities) && startingInstallTests ) fireTableStructureUpdate();
			else table.repaint();
		}

		@Override
		protected void prepareTable() {
			setColumnWidths();
			setCellEditors();
			setCellRenderers();
		}
	
		private void setCellEditors() {
			forEachColumn((column, columnIndex) -> {
				if (column==null) return;
				column.setCellEditor(cellwiseCellEditor);
			});
		}
	
		private TableCellEditor getCellEditor(int rowIndex, int columnIndex) {
			switch (columnIndex) {
			case  COLUMN_INDEX : break;
			case  COLUMN_LABEL1: if (rowIndex<nModules) return label1CellEditor;
			case  COLUMN_LABEL2: if (rowIndex<nModules) return label2CellEditor;
			default:
				int valueIndex = columnIndex-STANDARD_COLUMNS;
				if (valueIndex<block.module.values.size()) {
					if (rowIndex<nModules) {
						KnownModule.ValueDefinition.Format format = getVD(columnIndex).format;
						switch (format) {
						case Activated: return cellEditor_Activated;
						case FloatPlus:
						case Lightyears:
						case PercentMinus:
						case PercentPlus: break;
						}
					}
				} else {
					if (showValuePriorities) {
						if (valueIndex == block.module.values.size()+1)
							return cellEditor_Checkbox;
					} else
						if (valueIndex == block.module.values.size())
							return cellEditor_Checkbox;
				}
				break;
			}
			return cellEditor_TextField;
		}
	
		private void setCellRenderers() {
			forEachColumn((column, columnIndex) -> {
				if (column==null) return;
				column.setCellRenderer(defaultTableCellRenderer);
			});
		}
		
		@Override
		protected int getPrefColumnWidth(int columnIndex) {
			switch (columnIndex) {
			case  COLUMN_INDEX : return 30;
			case  COLUMN_LABEL1: return 150;
			case  COLUMN_LABEL2: return 150;
			default:
				int valueIndex = columnIndex-STANDARD_COLUMNS;
				if (valueIndex<block.module.values.size())
					return 70;
				if (valueIndex==block.module.values.size())
					return 40;
				if (valueIndex==block.module.values.size()+1 && showValuePriorities)
					return 40;
				return 150;
			}
		}
	
		@Override public int getRowCount   () {
			int c = nModules;
			if (showValuePriorities) ++c;
			return c;
		}
		@Override public int getColumnCount() {
			int c = STANDARD_COLUMNS + block.module.values.size();
			if (showValuePriorities) c += 1;
			if (defineFinalInstallation) c += 2;
			return c;
		}
		
		private KnownModule.ValueDefinition getVD(int columnIndex) {
			int index = columnIndex-STANDARD_COLUMNS;
			if (index<0 || index>=block.module.values.size()) return null;
			return block.module.values.get(index);
		}
	
		@Override
		public String getColumnName(int columnIndex) {
			switch (columnIndex) {
			case COLUMN_INDEX : return "#";
			case COLUMN_LABEL1: return "Label 1";
			case COLUMN_LABEL2: return "Label 2";
			default:
				int valueIndex = columnIndex-STANDARD_COLUMNS;
				if (valueIndex<block.module.values.size())
					return getVD(columnIndex).label;
				if (showValuePriorities) {
					if (valueIndex==block.module.values.size())
						return "Prio";
					if (valueIndex==block.module.values.size()+1)
						return "Install";
				} else {
					if (valueIndex==block.module.values.size())
						return "Install";
				}
				return "Installed";
			}
		}
	
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			switch (columnIndex) {
			case COLUMN_INDEX :
			case COLUMN_LABEL1:
			case COLUMN_LABEL2:
				return String.class;
			default:
				int valueIndex = columnIndex-STANDARD_COLUMNS;
				if (valueIndex<block.module.values.size()) {
					KnownModule.ValueDefinition.Format format = getVD(columnIndex).format;
					switch (format) {
					case Activated: return String.class;
					case FloatPlus:
					case Lightyears:
					case PercentMinus:
					case PercentPlus: break;
					}
					return Float.class;
				}
				if (showValuePriorities) {
					if (valueIndex==block.module.values.size())
						return Float.class;
					if (valueIndex==block.module.values.size()+1)
						return Boolean.class;
				} else {
					if (valueIndex==block.module.values.size())
						return Boolean.class;
				}
				return String.class;
			}
		}
	
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			InstalledUpgrade upgrade = null;
			if (rowIndex<nModules && rowIndex<block.installedModules.size())
				upgrade = block.installedModules.get(rowIndex);
			
			switch (columnIndex) {
			case COLUMN_INDEX : if (rowIndex>=nModules) return "";           return String.format("[%02d]", rowIndex+1);
			case COLUMN_LABEL1: if (rowIndex>=nModules) return "";           return upgrade==null ? null : upgrade.label1;
			case COLUMN_LABEL2: if (rowIndex>=nModules) return "Priority :"; return upgrade==null ? null : upgrade.label2;
			default:
				int valueIndex = columnIndex-STANDARD_COLUMNS;
				if (valueIndex<block.module.values.size()) {
					KnownModule.ValueDefinition vd = getVD(columnIndex);
					if (rowIndex<nModules) {
						if (upgrade==null) return null;
						Float value = upgrade.values.get(vd);
						switch (vd.format) {
						case Activated: return value==null || value<1 ? CELLEDITORVALUE_NOTACTIVATED : CELLEDITORVALUE_ACTIVATED;
						case FloatPlus:
						case Lightyears:
						case PercentMinus:
						case PercentPlus: break;
						}
						return value;
					} else
						return (float)vd.priority;
				}
				if (valueIndex == block.module.values.size() && showValuePriorities)
					return rowIndex<nModules ? computeUpgradePriority(upgrade,block.module.values) : null;
				if (valueIndex == block.module.values.size() + (showValuePriorities?1:0)) {
					return rowIndex<finalSequence.length ? finalSequence[rowIndex]==block.module.moduleID : null;
				}
				return rowIndex<finalSequence.length && finalSequence[rowIndex]!=null ? getLabelOrID(finalSequence[rowIndex]) : null;
			}
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			switch (columnIndex) {
			case COLUMN_INDEX : return false;
			case COLUMN_LABEL1:
			case COLUMN_LABEL2: return rowIndex<nModules;
			default:
				int valueIndex = columnIndex-STANDARD_COLUMNS;
				if (valueIndex<block.module.values.size()) return true;
				if (valueIndex==block.module.values.size() && showValuePriorities) return false;
				if (valueIndex==block.module.values.size() + (showValuePriorities?1:0))
					if (rowIndex<finalSequence.length && defineFinalInstallation && !installTestAreRunning) {
						GeneralizedID finalID = finalSequence[rowIndex];
						return finalID==null || finalID==block.module.moduleID;
					}
				return false;
			}
		}
	
		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			InstalledUpgrade upgrade = null;
			
			if (rowIndex<block.installedModules.size())
				upgrade = block.installedModules.get(rowIndex);
			
			if (upgrade==null && rowIndex<nModules) {
				while(rowIndex>=block.installedModules.size())
					block.installedModules.add(null);
				upgrade = new InstalledUpgrade(block.module);
				block.installedModules.set(rowIndex,upgrade);
			}
			
			switch (columnIndex) {
			case COLUMN_INDEX : Debug.Assert(false); break;
			case COLUMN_LABEL1: Debug.Assert(rowIndex<nModules); upgrade.label1 = (String)aValue; break;
			case COLUMN_LABEL2: Debug.Assert(rowIndex<nModules); upgrade.label2 = (String)aValue; break;
			default:
				int valueIndex = columnIndex-STANDARD_COLUMNS;
				if (valueIndex<block.module.values.size()) {
					KnownModule.ValueDefinition vd = getVD(columnIndex);
					if (rowIndex<nModules) {
						Debug.Assert(upgrade!=null);
						Float value = null;
						switch (vd.format) {
						case Activated: value = CELLEDITORVALUE_ACTIVATED.equals(aValue) ? 1.0f : null; break;
						case FloatPlus:
						case Lightyears:
						case PercentMinus:
						case PercentPlus: value = parseFloat((String)aValue); break;
						}
						upgrade.values.put(vd, value);
					} else {
						Float f = parseFloat((String)aValue);
						if (f!=null) {
							vd.priority = f;
							SwingUtilities.invokeLater(()->{
								fireTableColumnChanged(block.module.values.size());
								table.repaint();
								updater.prioChanged(block.module.moduleID);
							});
						}
					}
				} else {
					if (valueIndex==block.module.values.size() + (showValuePriorities?1:0)) {
						if (aValue instanceof Boolean && rowIndex<finalSequence.length) {
							boolean b = (Boolean) aValue;
							finalSequence[rowIndex] = b ? block.module.moduleID : null;
							updater.finalSequenceChanged();
						}
					}
				}
				break;
			}
		}
	
		private Float parseFloat(String str) {
			try {
				return Float.parseFloat(str.replace(',','.'));
			} catch (NumberFormatException e) {
				return null;
			}
		}

		private class MyTableCellRenderer extends DefaultTableCellRenderer {
			private static final long serialVersionUID = 7128510133641722765L;
			
			private CheckBoxRendererComponent checkBox;
			private ColorRange colorRange;
			
			MyTableCellRenderer() {
				checkBox = new CheckBoxRendererComponent();
				colorRange = new ColorRange();
				colorRange.add(0.00f, Color.WHITE);
				colorRange.add(0.75f, new Color(0xFFD000));
				colorRange.add(1.00f, new Color(0xFF7F00));
			}
		
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
				Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, columnIndex);
				
				rowIndex = table.convertRowIndexToModel(rowIndex);
				columnIndex = table.convertColumnIndexToModel(columnIndex);
				
				int valueIndex = columnIndex-STANDARD_COLUMNS;
				
				if (valueIndex == block.module.values.size() + (showValuePriorities?1:0) && rowIndex<nModules) {
					component = checkBox;
					checkBox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
					checkBox.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
					checkBox.setSelected(value instanceof Boolean ? (Boolean) value : false);
					checkBox.setHorizontalAlignment(CENTER);
					if (rowIndex<finalSequence.length) {
						GeneralizedID finalID = finalSequence[rowIndex];
						checkBox.setEnabled(finalID==block.module.moduleID || finalID==null);
					} else
						checkBox.setEnabled(true);
				}
				
				if (component instanceof JLabel) {
					JLabel label = (JLabel) component;
					int alignment = JLabel.LEFT;
					
					if (rowIndex>=nModules)
						alignment = JLabel.RIGHT;
					else
						switch (columnIndex) {
						case  COLUMN_INDEX : alignment = JLabel.CENTER; break;
						case  COLUMN_LABEL1: alignment = JLabel.RIGHT; break;
						case  COLUMN_LABEL2: alignment = JLabel.LEFT; break;
						default:
							if (valueIndex<block.module.values.size()) {
								KnownModule.ValueDefinition.Format format = getVD(columnIndex).format;
								switch (format) {
								case Activated:
									alignment = JLabel.CENTER;
									break;
								case FloatPlus: 
								case Lightyears:
								case PercentMinus:
								case PercentPlus:
									alignment = JLabel.RIGHT;
									if (value instanceof Float)
										label.setText(format.getFormatedValue((Float) value));
									break;
								}
							} else if (showValuePriorities && valueIndex==block.module.values.size()) {
								alignment = JLabel.RIGHT;
								if (value instanceof Float)
									label.setText(String.format(Locale.ENGLISH,"%1.3f",value));
							}
							break;
						}
					
					label.setHorizontalAlignment(alignment);
				}
				if (!isSelected) {
					Color bg = null;
					if (installTestAreRunning) {
						if (rowIndex == currentInstallTestModule)
							bg = COLOR_CURRENTMODULE;
						else if (currentInstallTestModule>=0)
							bg = COLOR_CURRENTSEQUENCE;
						else
							bg = COLOR_NOTCURRENTSEQUENCE;
						
					} else {
						if (defineFinalInstallation && isSelectionColoring) {
							if (rowIndex<finalSequence.length) {
								GeneralizedID finalID = finalSequence[rowIndex];
								if (finalID==block.module.moduleID)
									bg = COLOR_CURRENTMODULE;
								else if (finalID!=null)
									bg = COLOR_NOTCURRENTSEQUENCE;
							}
						}
					}
					if (bg==null) {
						if (value instanceof Float) {
							float fValue = (Float) value;
							if (rowIndex<nModules) {
								if (0<=valueIndex && valueIndex<block.module.values.size()) {
									KnownModule.ValueDefinition vd = getVD(columnIndex);
									Debug.Assert(vd!=null);
									Float min = isValueColoringMinMax ? vd.min : (Float)0.0f; // else 0..max
									Float max = vd.max;
									if (min!=null && max!=null) {
										float f;
										if (max.floatValue()==min.floatValue())
											f = 1.0f;
										else
											f = (fValue-min)/(max-min);
										f = Math.min(Math.max(0, f), 1);
										bg = colorRange.computeColor(f);
									}
								} else if (valueIndex==block.module.values.size()) {
									bg = colorRange.computeColor(fValue);
								}
							}
						}
					}
					if (bg==null) bg = table.getBackground();
					component.setBackground(bg);
				}
				return component;
			}
		}
	}

	private final static class SequencesTableModel extends AbstractTableModel {
	
		private HashMap<GeneralizedID,Integer> modules;
		private GeneralizedID[][] sequences = null;
		private int currentSequence = -1;
		private int currentModule = -1;
		private TableCellRenderer defaultTableCellRenderer = null;
		private int nModules = 0;

		SequencesTableModel() {
			modules = new HashMap<>();
			defaultTableCellRenderer = new SequencesTableCellRenderer();
		}
		
		private class SequencesTableCellRenderer extends DefaultTableCellRenderer {
			private static final long serialVersionUID = 4603754735459741145L;

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
				Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, columnIndex);
				rowIndex = table.convertRowIndexToModel(rowIndex);
				columnIndex = table.convertColumnIndexToModel(columnIndex);
				
				if (component instanceof JLabel) {
					JLabel label = (JLabel) component;
					
					if (columnIndex==currentSequence) label.setHorizontalAlignment(JLabel.LEFT);
					else  label.setHorizontalAlignment(JLabel.CENTER);
					
					if (!isSelected) {
						Color background = table.getBackground();
						if (currentSequence>=0) {
							if (currentSequence==columnIndex) {
								if (currentModule==rowIndex) background = COLOR_CURRENTMODULE;
								else background = COLOR_CURRENTSEQUENCE;
							} else background = COLOR_NOTCURRENTSEQUENCE;
						}
						label.setBackground(background);
					}
					
				}
				return component;
			}
			
		}
		
		public void clearCurrentModule() {
			setCurrentModule(-1,-1);
		}
		
		public void setCurrentModule(int currentSequence, int currentModule) {
			this.currentSequence = currentSequence;
			this.currentModule = currentModule;
			fireTableStructureUpdate();
		}

		@Override
		protected void prepareTable() {
			setColumnWidths();
			setCellRenderers();
		}

		@Override protected int getPrefColumnWidth(int columnIndex) {
			if (columnIndex==currentSequence) return 170;
			return 35;
		}
		
		private void setCellRenderers() {
			forEachColumn((column, columnIndex) -> {
				if (column==null) return;
				column.setCellRenderer(defaultTableCellRenderer);
			});
		}

		public void updateSequences(Vector<GeneralizedID> modules, GeneralizedID[][] sequences, int nModules) {
			this.nModules = nModules;
			currentSequence = -1;
			currentModule = -1;
			
			this.modules.clear();
			for (int i=0; i<modules.size(); i++) {
				GeneralizedID module = modules.get(i);
				Debug.Assert(module!=null);
				this.modules.put(module,i);
			}
			this.sequences = sequences;
			fireTableHeaderChanged();
			
			table.setPreferredScrollableViewportSize(table.getPreferredSize());
		}

		@Override public int getRowCount   () { return nModules; }
		@Override public int getColumnCount() { return sequences==null ? 0 : sequences.length; }
		@Override public Class<?> getColumnClass(int c) { return String.class; }
		@Override public boolean isCellEditable(int r, int c) { return false; }
		@Override public void setValueAt(Object v, int r, int c) {}
	
		@Override public String getColumnName(int columnIndex) {
			if (columnIndex==currentSequence) return String.format("Sequence %d", columnIndex+1);
			return String.format("Seq%d", columnIndex+1);
		}
	
		@Override public Object getValueAt(int rowIndex, int columnIndex) {
			if (sequences==null) return null;
			
			if (columnIndex<0 || sequences.length<=columnIndex) return null;
			GeneralizedID[] sequence = sequences[columnIndex];
			
			if (rowIndex<0 || sequence.length<=rowIndex) return null;
			GeneralizedID id = sequence[rowIndex];
			if (id==null) {
				if (columnIndex==currentSequence) return "Dummy";
				return "DD";
			}
			
			Integer index = modules.get(id);
			String label = index==null ? "??" : String.format("[%d]", index+1);
			if (columnIndex==currentSequence) label += " "+getLabelOrID(id);
			
			return label;
		}
	
	}

	private final class FinalSequenceTableModel extends AbstractTableModel {
	
		private Session session = null;
		private Vector<GeneralizedID> sortedIDs = null;
		private boolean showValuePriorities = false;

		public void update(Session session) {
			this.session = session;
			sortedIDs = sortedID(this.session.blocks.keySet());
			fireTableStructureUpdate();
		}

		public void showValuePriorities(boolean b) {
			showValuePriorities = b; 
			fireTableStructureUpdate();
		}

		public void updatePrioColumn(GeneralizedID id) {
			int columnIndex = getColumnIndex(id);
			if (columnIndex<0) return;
			fireTableColumnChanged(columnIndex);
		}

		@Override
		protected void prepareTable() {
			setColumnWidths();
			table.setPreferredScrollableViewportSize(table.getPreferredSize());
		}
		
		@Override public int getRowCount() { return session==null ? 0 : session.nModules; }
		@Override public int getColumnCount() { return 1 + (!showValuePriorities||sortedIDs==null?0:sortedIDs.size()); }
		@Override public Class<?> getColumnClass(int columnIndex) { return String.class; }
		@Override public boolean isCellEditable(int rowIndex, int columnIndex) { return false; }
		@Override public void setValueAt(Object aValue, int rowIndex, int columnIndex) {}
		
		@Override public String getColumnName(int columnIndex) {
			if (columnIndex==0) return "Module";
			GeneralizedID id = getID(columnIndex);
			if (id==null) return "["+columnIndex+"]";
			return getLabelOrID(id);
		}

		@Override protected int getPrefColumnWidth(int columnIndex) {
			if (columnIndex==0) return 200;
			return 40;
		}

		@Override public Object getValueAt(int rowIndex, int columnIndex) {
			if (columnIndex==0) {
				if (session==null) return null;
				if (session.finalSequence==null) return null;
				if (session.finalSequence.length<=rowIndex) return null;
				if (session.finalSequence[rowIndex]==null) return null;
				return getLabelOrID(session.finalSequence[rowIndex]);
			} else {
				Session.SessionBlock block = getBlock(columnIndex);
				InstalledUpgrade upgrade = getModule(block, rowIndex);
				if (block==null || upgrade==null) return null;
				Float prio = computeUpgradePriority(upgrade, block.module.values);
				if (prio==null) return null;
				return String.format(Locale.ENGLISH, "%1.3f", prio);
			}
		}

		private Session.SessionBlock getBlock(int columnIndex) {
			GeneralizedID id = getID(columnIndex);
			if (id==null) return null;
			if (session==null) return null;
			return session.blocks.get(id);
		}
		
		private InstalledUpgrade getModule(Session.SessionBlock block, int rowIndex) {
			if (block==null) return null;
			if (rowIndex<0) return null;
			if (block.installedModules.size()<=rowIndex) return null;
			return block.installedModules.get(rowIndex);
		}
		
		private GeneralizedID getID(int columnIndex) {
			if (columnIndex<=0) return null;
			if (sortedIDs==null) return null;
			if (sortedIDs.size()<=columnIndex-1) return null;
			return sortedIDs.get(columnIndex-1);
		}

		private int getColumnIndex(GeneralizedID id) {
			if (sortedIDs!=null) {
				int index = sortedIDs.indexOf(id);
				if (index>=0) return index+1;
			}
			return -1;
		}
	}
}
