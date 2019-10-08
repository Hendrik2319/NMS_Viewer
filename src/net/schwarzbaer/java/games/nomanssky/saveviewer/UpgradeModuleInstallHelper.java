package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
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
import java.util.HashMap;
import java.util.Vector;
import java.util.function.Function;

import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import net.schwarzbaer.gui.Disabler;
import net.schwarzbaer.gui.FileChooser;
import net.schwarzbaer.gui.StandardDialog;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.gui.StandardMainWindow.DefaultCloseOperation;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID;

final class UpgradeModuleInstallHelper implements ActionListener {
	
	private static final String CFG = "NMS_Viewer.UpgradeModuleInstallHelper.cfg";
	private static HashMap<String,GeneralizedID> knownUpgradeModules;

	public static void main(String[] args) {
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		SaveViewer.loadToolbarIcons();
		GameInfos.loadAllIDsFromFiles();
		start(true);
	}

	static void start(boolean standalone) {
		knownUpgradeModules = new HashMap<>();
		GameInfos.productIDs.forEach(id->{
			if (id.type!=null && id.type.isUpgradeModule)
				knownUpgradeModules.put(id.id,id);
		});
		//knownUpgradeModules.sort(Comparator.<GeneralizedID,GeneralizedID.Type>comparing(id->id.type).thenComparing(id->id.label));
		
		new UpgradeModuleInstallHelper()
			.readConfig()
			.writeConfig()
			.createGUI(standalone)
			.openLastDataFile();
	}

	private Disabler<ActionCommand> disabler = null;
	private StandardMainWindow mainwindow = null;
	private JPanel contentPane = null;
	private FileChooser fileChooser = null;
	
	private Config config = new Config();
	private Session currentSession = null;

	private UpgradeModuleInstallHelper readConfig() {
		config.readFromFile(new File(CFG));
		return this;
	}

	private UpgradeModuleInstallHelper writeConfig() {
		config.writeToFile(new File(CFG));
		return this;
	}

	private UpgradeModuleInstallHelper openLastDataFile() {
		return this;
	}

	enum ActionCommand {
		NewSession, OpenSession, SaveSession, SaveSessionAs, EditSession,
		;
	}
	
	private UpgradeModuleInstallHelper createGUI(boolean standalone) {
		fileChooser = new FileChooser("Session", "umih");
		fileChooser.setMultiSelectionEnabled(false);
		
		disabler = new Disabler<ActionCommand>();
		disabler.setCareFor(ActionCommand.values());
		
		contentPane = new JPanel(new GridLayout(0,1,3,3));
		
		
		JMenu menuData = new JMenu("Data");
		menuData.add(SaveViewer.createMenuItem("New Session"            , this, disabler, ActionCommand.NewSession));
		menuData.add(SaveViewer.createMenuItem("Open Session"           , this, disabler, ActionCommand.OpenSession));
		menuData.add(SaveViewer.createMenuItem("Save Session"           , this, disabler, ActionCommand.SaveSession));
		menuData.add(SaveViewer.createMenuItem("Save Session as ..."    , this, disabler, ActionCommand.SaveSessionAs));
		menuData.addSeparator();
		menuData.add(SaveViewer.createMenuItem("Edit Session Parameters", this, disabler, ActionCommand.EditSession));
		
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(menuData);
		
		mainwindow = new StandardMainWindow("UpgradeModule InstallHelper", standalone ? DefaultCloseOperation.EXIT_ON_CLOSE : DefaultCloseOperation.DISPOSE_ON_CLOSE);
		mainwindow.startGUI(contentPane, menuBar);
		
		updateWindowTitle();
		updateGUIaccess();
		
		return this;
	}
	
	private void updateGUIaccess() {
		disabler.setEnable(ac->{
			switch (ac) {
			case NewSession:
			case OpenSession:
				return true;
				
			case EditSession:
			case SaveSessionAs:
				return currentSession!=null;
				
			case SaveSession:
				return currentSession!=null && config.currentSessionFile!=null;
			}
			return null;
		});
	}

	private void updateWindowTitle() {
		mainwindow.setTitle("UpgradeModule InstallHelper - "+( config.currentSessionFile==null ? "<New Session>" : "\""+config.currentSessionFile.getName()+"\"" ));
	}

	private void updateTables() {
		// TODO Auto-generated method stub
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		switch (ActionCommand.valueOf(e.getActionCommand())) {
		case NewSession: {
			EditSessionDialog dlg = new EditSessionDialog(mainwindow,"New Session",null);
			dlg.showDialog();
			Session newSession = dlg.getResult();
			if (newSession!=null) {
				currentSession = newSession;
				config.currentSessionFile = null;
				updateTables();
				updateWindowTitle();
			}
		} break;
		case EditSession: {
			EditSessionDialog dlg = new EditSessionDialog(mainwindow,"Edit Session",currentSession);
			dlg.showDialog();
			Session newSession = dlg.getResult();
			if (newSession!=null) {
				currentSession = newSession;
				updateTables();
			}
		} break;
		
		case OpenSession:
			if (fileChooser.showOpenDialog(mainwindow)==FileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				Session newSession = Session.openFile(file);
				if (newSession!=null) {
					currentSession = newSession;
					config.currentSessionFile = file;
					updateTables();
					updateWindowTitle();
				}
			}
			break;
			
		case SaveSession:
			if (config.currentSessionFile!=null) {
				currentSession.saveFile(config.currentSessionFile);
				break;
			}
			
		case SaveSessionAs:
			if (fileChooser.showSaveDialog(mainwindow)==FileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				currentSession.saveFile(file);
				config.currentSessionFile = file;
				updateWindowTitle();
			}
			break;
		}
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

	private static class Config {
		File currentSessionFile;
		HashMap<GeneralizedID,KnownModuleValues> knownModuleValues;
		
		Config() {
			currentSessionFile = null;
			knownModuleValues = new HashMap<>();
		}

		public void readFromFile(File file) {
			currentSessionFile = null;
			knownModuleValues.clear();;
			
			KnownModuleValues knownModule = null;
			KnownModuleValues.KnownModuleValue knownValue = null;
			try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
				String line;
				while ( (line=in.readLine())!=null ) {
					if (line.startsWith("LastSessionFile=")) {
						String str = line.substring("LastSessionFile=".length());
						currentSessionFile = new File(str);
					}
					if (line.startsWith("ModuleID=")) {
						String str = line.substring("ModuleID=".length());
						GeneralizedID moduleID = knownUpgradeModules.get(str);
						if (moduleID==null) {
							SaveViewer.log_error_ln("Can't find UpgradeModule for ID \"%s\"", str);
							knownModule = null;
						} else
							knownModuleValues.put( moduleID, knownModule = new KnownModuleValues(moduleID) );
					}
					if (line.startsWith("value.label=") && knownModule!=null) {
						String str = line.substring("value.label=".length());
						knownModule.values.add( knownValue = new KnownModuleValues.KnownModuleValue() );
						knownValue.label = str;
					}
					if (line.startsWith("value.format=") && knownValue!=null) {
						String str = line.substring("value.format=".length());
						try { knownValue.format = KnownModuleValues.KnownModuleValue.Format.valueOf(str); }
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
		}
		
		public void writeToFile(File file) {
			try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
				
				if (currentSessionFile!=null) out.printf("LastSessionFile=%s%n", currentSessionFile.getAbsolutePath());
				
				knownModuleValues.forEach((id,values)->{
					out.println();                      // decoration only :)
					out.println("[KnownModuleValues]"); // decoration only :)
					Debug.Assert(id!=values.moduleID);
					out.printf("ModuleID=%s%n", values.moduleID.id);
					values.values.forEach(v->{
						out.printf("value.label=%s%n", v.label);
						if (v.format!=null) out.printf("value.format=%s%n", v.format);
						if (v.min!=null && !Float.isNaN(v.min)) out.printf("value.min=%1.3f%n", v.min);
						if (v.max!=null && !Float.isNaN(v.max)) out.printf("value.max=%1.3f%n", v.max);
					});
				});
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static class KnownModuleValues {
		
		GeneralizedID moduleID;
		Vector<KnownModuleValue> values;
		
		KnownModuleValues(GeneralizedID moduleID) {
			this.moduleID = moduleID;
			values = new Vector<>();
		}
		
		private static class KnownModuleValue {
			enum Format { PercentPlus, PercentMinus, Activated, FloatPlus, Lightyears }
			String label = null;
			Format format = null;
			Float min = null,max = null;
		}
	}
	
	private static class InstalledModule {
		
		final KnownModuleValues base;
		String label1;
		String label2;
		Vector<InstalledModuleValue> values;
		
		@SuppressWarnings("unused")
		InstalledModule(KnownModuleValues base) {
			this.base = base;
			label1 = "";
			label2 = "";
			values = new Vector<>();
		}
		InstalledModule(InstalledModule module) {
			base = module.base;
			label1 = module.label1;
			label2 = module.label2;
			values = cloneVector(module.values, InstalledModuleValue::new);
		}

		private static class InstalledModuleValue {
			
			final KnownModuleValues.KnownModuleValue base;
			float value;
			
			InstalledModuleValue(KnownModuleValues.KnownModuleValue base, float value) {
				this.base = base;
				this.value = value;
			}
			InstalledModuleValue(InstalledModuleValue modValue) {
				this(modValue.base,modValue.value);
			}
		}
	}
	
	private static class Session {
		
		HashMap<GeneralizedID,SessionBlock> blocks;
		
		Session() {
			blocks = new HashMap<>();
		}

		Session(Session session) {
			blocks = cloneHashMap(session.blocks,null,SessionBlock::new);
		}

		static Session openFile(File file) {
			// TODO Auto-generated method stub
			return null;
		}

		void saveFile(File file) {
			// TODO Auto-generated method stub
			
		}
		
		private static class SessionBlock {
			
			KnownModuleValues module;
			int amount;
			Vector<InstalledModule> installedModules;
			
			@SuppressWarnings("unused")
			public SessionBlock(KnownModuleValues module) {
				this.module = module;
				amount = 0;
				installedModules = new Vector<>();
			}
			public SessionBlock(SessionBlock block) {
				module = block.module;
				amount = block.amount;
				installedModules = cloneVector(block.installedModules,InstalledModule::new);
			}
		}
	}

	private static final class EditSessionDialog extends StandardDialog {
		private static final long serialVersionUID = 687525027915443491L;
		private Session session;
		private Session result;

		public EditSessionDialog(Window parent, String title, Session session) {
			super(parent, title);
			this.session = session==null ? new Session() : new Session(session);
			this.result = null;
			
			JTable table = new JTable(new SessionTableModel(this.session));
			JScrollPane tableScrollPane = new JScrollPane(table);
			
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.weighty = 1;
			JPanel buttonPanel = new JPanel(new GridBagLayout());
			c.weightx = 1;
			buttonPanel.add(new JLabel(),c);
			c.weightx = 0;
			buttonPanel.add(SaveViewer.createButton("Ok"    , e->{ result = this.session; closeDialog(); }),c);
			buttonPanel.add(SaveViewer.createButton("Cancel", e->{ result = null; closeDialog(); }),c);
			
			JPanel contentPane = new JPanel(new BorderLayout(3,3));
			contentPane.add(tableScrollPane,BorderLayout.CENTER);
			contentPane.add(buttonPanel,BorderLayout.SOUTH);
			
			createGUI(contentPane);
		}

		public Session getResult() {
			return result;
		}
	
		private class SessionTableModel implements TableModel {

			private Session session;

			public SessionTableModel(Session session) {
				this.session = session;
			}

			@Override
			public int getRowCount() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public int getColumnCount() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public String getColumnName(int columnIndex) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Class<?> getColumnClass(int columnIndex) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void addTableModelListener(TableModelListener l) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void removeTableModelListener(TableModelListener l) {
				// TODO Auto-generated method stub
				
			}
			
		}
	}

	private static class InstalledModulesTableModel implements TableModel {

		@Override
		public int getRowCount() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getColumnCount() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public String getColumnName(int columnIndex) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void addTableModelListener(TableModelListener l) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void removeTableModelListener(TableModelListener l) {
			// TODO Auto-generated method stub
			
		}
		
	}
}
