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
import java.util.HashMap;
import java.util.Vector;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import net.schwarzbaer.gui.Disabler;
import net.schwarzbaer.gui.FileChooser;
import net.schwarzbaer.gui.StandardDialog;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.gui.StandardMainWindow.DefaultCloseOperation;
import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.ComboboxCellEditor;
import net.schwarzbaer.gui.Tables.NonStringRenderer;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.gui.Tables.SimplifiedColumnIDInterface;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView;

final class UpgradeModuleInstallHelper implements ActionListener {
	
	private static final String CFG = "NMS_Viewer.UpgradeModuleInstallHelper.cfg";
	private static final Comparator<GeneralizedID> compareGeneralizedIDs =
			Comparator.<GeneralizedID,GeneralizedID.Type>comparing(id->id.type).thenComparing(id->id.id);

	public static void main(String[] args) {
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		SaveViewer.loadToolbarIcons();
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

	private static Vector<GeneralizedID> sorted(Collection<GeneralizedID> set) {
		Vector<GeneralizedID> vector = new Vector<>(set);
		vector.sort(compareGeneralizedIDs);
		return vector;
	}
	
	private final HashMap<String,GeneralizedID> knownUpgradeModuleIDs;

	private Disabler<ActionCommand> disabler = null;
	private StandardMainWindow mainwindow = null;
	private JPanel contentPane = null;
	private FileChooser fileChooser = null;
	
	private Config config = new Config();
	private Session currentSession = null;
	
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
			EditSessionDialog dlg = new EditSessionDialog(mainwindow,"New Session",null,config.knownModules);
			dlg.showDialog();
			Session newSession = dlg.getResult();
			if (newSession!=null) {
				currentSession = newSession;
				config.currentSessionFile = null;
				updateTables();
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
				updateTables();
				updateGUIaccess();
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
					updateGUIaccess();
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
				updateGUIaccess();
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

	private class Config {
		File currentSessionFile;
		HashMap<GeneralizedID,KnownModule> knownModules;
		
		Config() {
			currentSessionFile = null;
			knownModules = new HashMap<>();
		}

		public void readFromFile(File file) {
			currentSessionFile = null;
			knownModules.clear();
			KnownModule knownModule = null;
			KnownModule.KnownModuleValue knownValue = null;
			try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
				String line;
				while ( (line=in.readLine())!=null ) {
					if (line.startsWith("LastSessionFile=")) {
						String str = line.substring("LastSessionFile=".length());
						currentSessionFile = new File(str);
					}
					if (line.startsWith("ModuleID=")) {
						String str = line.substring("ModuleID=".length());
						GeneralizedID moduleID = knownUpgradeModuleIDs.get(str);
						if (moduleID==null) {
							SaveViewer.log_error_ln("Can't find UpgradeModule for ID \"%s\"", str);
							knownModule = null;
						} else
							knownModules.put( moduleID, knownModule = new KnownModule(moduleID) );
					}
					if (line.startsWith("value.label=") && knownModule!=null) {
						String str = line.substring("value.label=".length());
						knownModule.values.add( knownValue = new KnownModule.KnownModuleValue() );
						knownValue.label = str;
					}
					if (line.startsWith("value.format=") && knownValue!=null) {
						String str = line.substring("value.format=".length());
						try { knownValue.format = KnownModule.KnownModuleValue.Format.valueOf(str); }
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
				
				for (GeneralizedID id:sorted(knownModules.keySet())) {
					KnownModule module = knownModules.get(id);
					out.println();                      // decoration only :)
					out.println("[KnownModuleValues]"); // decoration only :)
					Debug.Assert(id==module.moduleID);
					out.printf("ModuleID=%s%n", module.moduleID.id);
					module.values.forEach(v->{
						out.printf("value.label=%s%n", v.label);
						if (v.format!=null) out.printf("value.format=%s%n", v.format);
						if (v.min!=null && !Float.isNaN(v.min)) out.printf("value.min=%1.3f%n", v.min);
						if (v.max!=null && !Float.isNaN(v.max)) out.printf("value.max=%1.3f%n", v.max);
					});
				}
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static class KnownModule {
		
		GeneralizedID moduleID;
		Vector<KnownModuleValue> values;
		
		KnownModule(GeneralizedID moduleID) {
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
	
	private static class InstalledUpgrade {
		
		final KnownModule base;
		String label1;
		String label2;
		HashMap<KnownModule.KnownModuleValue,Float> values;
		
		@SuppressWarnings("unused")
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
			public Integer getInstallPos(int i) {
				if (i<installPos.size()) return installPos.get(i);
				return null;
			}
			public void setInstallPos(int i, Integer newPos) {
				while (i>=installPos.size()) installPos.add(null);
				installPos.set(i, newPos);
			}
		}
	}

	private static final class EditSessionDialog extends StandardDialog implements ActionListener {
		private static final long serialVersionUID = 687525027915443491L;
		private Session session;
		private Session result;
		private Disabler<ActionCommand> disabler;
		private JPanel contentPane;
		private JScrollPane moduleTablePanel;
		private JPanel installOrderPanel;
		private TableView.SimplifiedTable<ColumnID> moduleTable;
		private HashMap<GeneralizedID, KnownModule> knownModules;
		private JTextArea numberOfCyclesOutput;
		
		private int nModules;

		public EditSessionDialog(Window parent, String title, Session session, HashMap<GeneralizedID, KnownModule> knownModules) {
			super(parent, title);
			this.knownModules = knownModules;
			this.session = session==null ? new Session() : new Session(session);
			this.result = null;
			nModules = 0;
			
			disabler = new Disabler<>();
			disabler.setCareFor(ActionCommand.values());
			
			moduleTablePanel = createModuleTablePanel();
			installOrderPanel = null;
			
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.weighty = 1;
			JPanel buttonPanel = new JPanel(new GridBagLayout());
			c.weightx = 1;
			buttonPanel.add(new JLabel(),c);
			c.weightx = 0;
			buttonPanel.add(SaveViewer.createButton("Select Modules"   , this, disabler, ActionCommand.SelectModules),c);
			buttonPanel.add(SaveViewer.createButton("Set Install Order", this, disabler, ActionCommand.SetInstallOrder),c);
			buttonPanel.add(SaveViewer.createButton("Finish", this, disabler, ActionCommand.Finish),c);
			buttonPanel.add(SaveViewer.createButton("Cancel", this, disabler, ActionCommand.Cancel),c);
			
			contentPane = new JPanel(new BorderLayout(3,3));
			contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
			contentPane.add(moduleTablePanel,BorderLayout.CENTER);
			contentPane.add(buttonPanel,BorderLayout.SOUTH);
			
			int prefWidth = Arrays.stream(ColumnID.values()).mapToInt(columnID->columnID.columnConfig.prefWidth).sum();
			contentPane.setPreferredSize(new Dimension(prefWidth+50,500));
			
			updateNumberOfModules();
			updateButtonAccess();
			createGUI(contentPane);
		}

		private void updateButtonAccess() {
			disabler.setEnable(ac->{
				switch (ac) {
				case Cancel: return true;
				case Finish: return nModules>0 && areAllInstallPosDefined();
				
				case SetInstallOrder: return installOrderPanel==null;
				case SelectModules  : return moduleTablePanel ==null;
				}
				return null;
			});
		}

		enum ActionCommand { SelectModules, SetInstallOrder, Finish, Cancel }

		@Override
		public void actionPerformed(ActionEvent e) {
			switch (ActionCommand.valueOf(e.getActionCommand())) {
			
			case Cancel: result = null        ; closeDialog(); break;
			case Finish: result = this.session; closeDialog(); break;
			
			case SelectModules:
				contentPane.remove(installOrderPanel);
				installOrderPanel = null;
				moduleTablePanel = createModuleTablePanel();
				contentPane.add(moduleTablePanel,BorderLayout.CENTER);
				contentPane.revalidate();
				contentPane.repaint();
				updateButtonAccess();
				break;
				
			case SetInstallOrder:
				if (moduleTable.isEditing())
					moduleTable.getCellEditor().stopCellEditing();
				contentPane.remove(moduleTablePanel);
				moduleTablePanel = null;
				installOrderPanel = createInstallOrderPanel();
				contentPane.add(installOrderPanel,BorderLayout.CENTER);
				contentPane.revalidate();
				contentPane.repaint();
				updateButtonAccess();
				break;
			}
		}

		private JScrollPane createModuleTablePanel() {
			SessionTableModel sessionTableModel = new SessionTableModel();
			moduleTable = new TableView.SimplifiedTable<>("", sessionTableModel, true, SaveViewer.DEBUG, true);
			
			ComboboxCellEditor<GeneralizedID> cellEditor =
					new ComboboxCellEditor<GeneralizedID>(SaveViewer.addNull(sorted(knownModules.keySet())));
			
			NonStringRenderer<GeneralizedID> renderer =
					new NonStringRenderer<GeneralizedID>(obj->{
						if (obj instanceof GeneralizedID) {
							GeneralizedID id = (GeneralizedID) obj;
							String label = id.label;
							if (label==null || label.isEmpty()) label = id.id;
							return label;
						}
						return "<none>";
					});
			cellEditor.setRenderer(renderer);
			moduleTable.setCellEditor  (ColumnID.Label, cellEditor);
			moduleTable.setCellRenderer(ColumnID.Label, renderer);
			
			return new JScrollPane(moduleTable);
		}

		private JPanel createInstallOrderPanel() {
			updateNumberOfModules();
			
			JPanel panel = new JPanel(new GridBagLayout());
			panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.insets = new Insets(2, 2, 2, 2);
			
			JRadioButton[][] btns = new JRadioButton[nModules][nModules];
			
			c.weighty = 0;
			c.gridy = 0;
			c.gridx = 1;
			panel.add(createLabel("Unset", true),c);
			for (int i=0; i<nModules; i++) {
				c.gridx = i+2;
				panel.add(createLabel("("+(i+1)+")", true),c);
			}
			int row = 0;
			for (GeneralizedID id:sorted(session.blocks.keySet())) {
				Session.SessionBlock block = session.blocks.get(id);
				Debug.Assert(id==block.module.moduleID);
				for (int i=0; i<block.amount; i++, row++) {
					int index = i;
					c.gridy = row+1;
					c.weightx = 0;
					
					c.gridx = 0;
					panel.add(createLabel("("+(i+1)+") "+block.module.moduleID.label, false),c);
					
					c.gridx = 1;
					ButtonGroup bg = new ButtonGroup();
					Integer pos = block.getInstallPos(i);
					panel.add(SaveViewer.createRadioButton("", bg, pos==null, true, e->setOrder(block,index,null,btns)),c);
					
					for (int col=0; col<nModules; col++) {
						int newPos = col;
						c.gridx = col+2;
						boolean isSelected = pos!=null && pos.intValue()==col;
						panel.add(btns[col][row] = SaveViewer.createRadioButton("", bg, isSelected, true, e->setOrder(block,index,newPos,btns)),c);
					}
					c.weightx = 1;
					c.gridx = nModules+2;
					panel.add(new JLabel(),c);
				}
			}
			c.gridy = nModules+1;
			c.gridx = 0;
			c.weightx = 1;
			c.weighty = 1;
			c.gridwidth = GridBagConstraints.REMAINDER;
			numberOfCyclesOutput = new JTextArea();
			panel.add(new JScrollPane(numberOfCyclesOutput),c);
			
			for (GeneralizedID id:session.blocks.keySet()) {
				Session.SessionBlock block = session.blocks.get(id);
				Debug.Assert(id==block.module.moduleID);
				for (int i=0; i<block.amount; i++) {
					Integer pos = block.getInstallPos(i);
					if (pos!=null)
						setEnabled(btns[pos],false);
				}
			}
			
			computeNumberOfCycles();
			return panel;
		}

		private Component createLabel(String string, boolean centered) {
			return new JLabel(string, centered ? JLabel.CENTER : JLabel.LEFT);
		}

		private void computeNumberOfCycles() {
			
			GeneralizedID[][] sequences = new GeneralizedID[nModules][];
			sequences[0] = createBaseSequence();
			if (sequences[0]==null) {
				numberOfCyclesOutput.setText("Finalize install order to determine number of cycles.");
				return;
			}
			
			numberOfCyclesOutput.setText("");
			
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
			
			int numberOfCycles = 0;
			for (int i=0; i<nModules; ++i) {
				boolean isNull = true;
				for (int j=0; j<nModules; ++j)
					if (sequences[i][j]!=null) { isNull = false; break; }
				if (isNull)
					sequences[i] = null;
				else
					++numberOfCycles;
			}
			SaveViewer.append_ln(numberOfCyclesOutput, "Number of cycles: %d", numberOfCycles);
			
			Vector<GeneralizedID> sortedIDs = sorted(session.blocks.keySet());
			SaveViewer.append_ln(numberOfCyclesOutput, "Modules:");
			for (int i=0; i<sortedIDs.size(); ++i) {
				GeneralizedID id = sortedIDs.get(i);
				int amount = session.blocks.get(id).amount;
				SaveViewer.append_ln(numberOfCyclesOutput, "   %2d  :  %s  (%dx)", i+1, id.toString(), amount);
			}
			SaveViewer.append_ln(numberOfCyclesOutput, "    ##  Dummy Module");
			
			SaveViewer.append_ln(numberOfCyclesOutput, "Sequences:  [%d]", numberOfCycles);
			for (int i=0; i<nModules; ++i) {
				if (sequences[i]!=null) {
					SaveViewer.append(numberOfCyclesOutput, "[%2d] ", i+1);
					String str = "";
					boolean isEnd = true;
					for (int j=nModules-1; j>=0; --j) {
						GeneralizedID id = sequences[i][j];
						if (id==null) {
							if (!isEnd) {
								if (!str.isEmpty()) str = ' '+str;
								str = "##"+str; 
							}
						} else {
							isEnd = false;
							if (!str.isEmpty()) str = ' '+str;
							str = String.format("%2d%s", sortedIDs.indexOf(id)+1, str); 
						}
					}
					SaveViewer.append_ln(numberOfCyclesOutput, str);
				}
			}
		}

		@SuppressWarnings("unused")
		private void showSequences(GeneralizedID[][] sequences, Vector<GeneralizedID> sortedIDs) {
			SaveViewer.log_ln("Sequences:");
			for (int i=0; i<nModules; ++i) {
				SaveViewer.log("[%2d] ", i);
				if (sequences[i] == null)
					SaveViewer.log_ln(" <null>");
				else {
					String str = "";
					for (int j=0; j<nModules; ++j) {
						GeneralizedID id = sequences[i][j];
						if (id==null) {
							if (!str.isEmpty()) str += ',';
							str += "--"; 
						} else {
							if (!str.isEmpty()) str += ',';
							str += String.format("%2d", sortedIDs.indexOf(id)+1); 
						}
					}
					SaveViewer.log_ln(" %s", str);
				}
			}
		}

		private GeneralizedID[] shiftSequences(GeneralizedID[] sequence) {
			GeneralizedID[] newSequences = Arrays.copyOf( Arrays.copyOfRange(sequence, 1, sequence.length), sequence.length);
			newSequences[sequence.length-1] = sequence[0];
			return newSequences;
		}

		private GeneralizedID[] createBaseSequence() {
			GeneralizedID[] baseSequence = new GeneralizedID[nModules];
			
			for (GeneralizedID id:session.blocks.keySet()) {
				Session.SessionBlock block = session.blocks.get(id);
				for (int i=0; i<block.amount; i++) {
					Integer pos = block.getInstallPos(i);
					if (pos==null) return null;
					baseSequence[pos] = id;
				}
			}
			return baseSequence;
		}

		private void updateNumberOfModules() {
			nModules = 0;
			for (GeneralizedID id:session.blocks.keySet()) {
				Session.SessionBlock block = session.blocks.get(id);
				Debug.Assert(id==block.module.moduleID);
				nModules += block.amount;
			}
		}

		private boolean areAllInstallPosDefined() {
			for (GeneralizedID id:session.blocks.keySet()) {
				Session.SessionBlock block = session.blocks.get(id);
				for (int i=0; i<block.amount; i++)
					if (block.getInstallPos(i)==null)
						return false;
			}
			return true;
		}

		private void setOrder(Session.SessionBlock block, int i, Integer newPos, JRadioButton[][] btns) {
			Integer pos = block.getInstallPos(i);
			if (pos!=null)
				setEnabled(btns[pos],true);
			
			block.setInstallPos(i, newPos);
			if (newPos!=null)
				setEnabled(btns[newPos],false);
			
			computeNumberOfCycles();
			updateButtonAccess();
		}

		private void setEnabled(JRadioButton[] btns, boolean enabled) {
			for (JRadioButton btn:btns) btn.setEnabled(enabled);
		}

		public Session getResult() {
			return result;
		}
		
		private enum ColumnID implements SimplifiedColumnIDInterface {
			ID    ("ID"    ,        String.class, 20,-1,120,120),
			Type  ("Type"  ,        String.class, 20,-1,200,200),
			Label ("Name"  , GeneralizedID.class, 20,-1,200,200),
			Amount("Amount",       Integer.class, 20,-1, 50, 50),
			;
			
			private SimplifiedColumnConfig columnConfig;
			
			ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				columnConfig = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			@Override public SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
		}
	
		private class SessionTableModel extends Tables.SimplifiedTableModel<ColumnID> {

			private Vector<GeneralizedID> sortedIDs;

			public SessionTableModel() {
				super(ColumnID.values());
				sortedIDs = sorted(session.blocks.keySet());
			}

			@Override public int getRowCount() { return sortedIDs.size()+1; }

			@Override
			protected boolean isCellEditable(int rowIndex, int columnIndex, ColumnID columnID) {
				switch (columnID) {
				case ID:
				case Type:
					return false;
					
				case Label:
					return true;
					
				case Amount:
					return rowIndex<sortedIDs.size();
				}
				return false;
			}

			@Override
			public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
				if (rowIndex>=sortedIDs.size()) {
					switch (columnID) {
					case Amount: return nModules;
					default: break;
					}
					return null;
				}
				
				GeneralizedID id = sortedIDs.get(rowIndex);
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
			protected void setValueAt(Object aValue, int rowIndex, int columnIndex, ColumnID columnID) {
				if (rowIndex>=sortedIDs.size()) {
					Debug.Assert(columnID==ColumnID.Label);
					if (aValue instanceof GeneralizedID) {
						GeneralizedID id = (GeneralizedID) aValue;
						int row = sortedIDs.indexOf(id);
						if (row>=0)
							JOptionPane.showMessageDialog(table, "You can't select a module twice. (-> Row "+(row+1)+")", "Module already selected", JOptionPane.ERROR_MESSAGE);
						else {
							sortedIDs.add(id);
							KnownModule module = knownModules.get(id);
							Debug.Assert(module!=null);
							Debug.Assert(id==module.moduleID);
							session.blocks.put(id, new Session.SessionBlock(module));
							fireTableRowAdded(sortedIDs.size()-1);
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
						int row = sortedIDs.indexOf(id);
						if (row>=0 && row==rowIndex) row = sortedIDs.indexOf(id,rowIndex+1);
						if (row>=0)
							JOptionPane.showMessageDialog(table, "You can't select a module twice. (-> Row "+(row+1)+")", "Module already selected", JOptionPane.ERROR_MESSAGE);
						else {
							GeneralizedID oldID = sortedIDs.get(rowIndex);
							session.blocks.remove(oldID);
							sortedIDs.set(rowIndex,id);
							KnownModule module = knownModules.get(id);
							session.blocks.put(id, new Session.SessionBlock(module));
							fireTableRowUpdate(rowIndex);
						}
					} else if (aValue==null) {
						GeneralizedID id = sortedIDs.get(rowIndex);
						Debug.Assert(id!=null);
						sortedIDs.remove(rowIndex);
						session.blocks.remove(id);
						fireTableRowRemoved(rowIndex);
					}
					break;
					
				case Amount: {
					GeneralizedID id = sortedIDs.get(rowIndex);
					Debug.Assert(id!=null);
					Session.SessionBlock block = session.blocks.get(id);
					Debug.Assert(block!=null);
					block.amount = aValue==null ? 0 : ((Integer)aValue).intValue();
					updateNumberOfModules();
					fireTableRowUpdate(sortedIDs.size());
				} break;
				}
			}
			
		}
	}

	@SuppressWarnings("unused")
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
