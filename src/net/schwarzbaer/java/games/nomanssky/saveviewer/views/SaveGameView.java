package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Vector;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.UniverseAddress;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveViewer;

public class SaveGameView extends JPanel {

	private static final long serialVersionUID = -1641171938196309864L;
	
	public final File file;
	public SaveGameData data;
	private JTabbedPane tabbedPane;

	private Window mainWindow;

	public SaveGameView(Window mainWindow, File file, SaveGameData data) {
		super(new BorderLayout(3, 3));
		setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		
		this.mainWindow = mainWindow;
		this.file = file;
		this.data = data;
		
		tabbedPane = new JTabbedPane();
		//tabbedPane.setPreferredSize(new Dimension(620, 500));
		
		addAllTabs();
		
		tabbedPane.addChangeListener(new ChangeListener() {
			@Override public void stateChanged(ChangeEvent e) {
				Component comp = tabbedPane.getSelectedComponent();
				if (comp instanceof SaveGameViewTabPanel) {
					SaveGameViewTabPanel tabPanel = (SaveGameViewTabPanel)comp;
					tabPanel.updateContent();
				}
			}
		});
		
		add(tabbedPane,BorderLayout.CENTER);
	}


	private void addAllTabs() {
		tabbedPane.addTab("General",new GeneralDataPanel(data));
		tabbedPane.addTab("Known Universe",new UniversePanel(data,mainWindow));
		tabbedPane.addTab("Galaxy Map",new GalaxyMapPanel(data,mainWindow));
		
		if (data.stats     !=null) tabbedPane.addTab("Stats",new StatsPanel(data));
		if (data.knownWords!=null) tabbedPane.addTab("KnownWords",new KnownWordsPanel(data));
		
		tabbedPane.addTab("DiscoveryData (Avail.)",new DiscoveryDataPanels.AvailableDataPanel(data));
		tabbedPane.addTab("DiscoveryData (Store)",new DiscoveryDataPanels.StoredDataPanel(data));
		
//		tabbedPane.addTab("### SortTestPanel ###",new SortTestPanel(data));
		
		tabbedPane.addTab("Raw Data Tree",new RawDataTreePanel(file,data));
	}
	
	
	public void replaceData(SaveGameData data) {
		this.data = data;
		tabbedPane.removeAll();
		addAllTabs();
	}


	@Override
	public String toString() {
		return file.getName();
	}


	static <T,R> Vector<R> convertVector( Vector<T> vector, Function<? super T,? extends R> convertValue ) {
		Vector<R> result = new Vector<>();
		for (T value : vector)
			result.add(convertValue.apply(value));
		return result;
	}
	
	static class SaveGameViewTabPanel extends JPanel {
		private static final long serialVersionUID = -5779057150309507685L;
		
		protected SaveGameData data;

		public SaveGameViewTabPanel(SaveGameData data) {
			super(new BorderLayout(3, 3));
			setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			this.data = data;
		}
		
		public void updateContent() {};

		protected static JButton createButton(String title, ActionListener l) {
			JButton button = new JButton(title);
			button.addActionListener(l);
			return button;
		}

		protected static JCheckBox createCheckbox(String title, ActionListener l, boolean isSelected) {
			JCheckBox button = new JCheckBox(title,isSelected);
			if (l!=null) button.addActionListener(l);
			return button;
		}

		protected void setNameForUniverseAddress(UniverseAddress ua, Universe.UniverseObject object, String objectStr) {
			String name = JOptionPane.showInputDialog(this, "New name for "+objectStr+" "+ua.getExtendedSigBoostCode(), object.getOriginalName());
			if (name!=null) {
				if (name.isEmpty()) name=null;
				object.setOriginalName(name);
				SaveViewer.saveUniverseObjectDataToFile(data.universe);
			}
		}
	}

	public static class EnumCheckBoxMenuItem<Key extends Enum<Key>, ActionCommand extends Enum<ActionCommand>> extends JCheckBoxMenuItem {
		private static final long serialVersionUID = -3010890115769695204L;
		
		Key key;
		ActionCommand actionCommand;

		protected EnumCheckBoxMenuItem(String label, ActionCommand actionCommand, Key key, boolean selected, ActionListener actionListener) {
			super(label);
			this.actionCommand = actionCommand;
			this.key = key;
			setSelected(selected);
			addActionListener(actionListener);
			setActionCommand(actionCommand.toString());
		}

		public static <K extends Enum<K>, CBMI extends JCheckBoxMenuItem> void setCheckBoxMenuItems(CBMI[] miArr, K key, K[] keys, ButtonGroup bg) {
			boolean nothingSelected = true;
			for (int i=0; i<keys.length; ++i) {
				boolean isSelected = key==keys[i];
				nothingSelected &= !isSelected;
				miArr[i].setSelected(isSelected);
				//System.out.printf("Set ECBMI[%8s]%s selected (%s)\r\n",keys[i],isSelected?"":" not",miArr[i].isSelected()==isSelected);
			}
			if (nothingSelected) bg.clearSelection();
		}
	}

/*
 	private static class SortTestPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = -8896141429868126519L;

		public SortTestPanel(SaveGameData data) {
			super(data);
			
			SortTestTableModel tableModel = new SortTestTableModel();
			SimplifiedTable table = new SimplifiedTable("SortTest",tableModel,true,SaveViewer.DEBUG,true);
			JScrollPane tableScrollPane = new JScrollPane(table);
			
			add(tableScrollPane,BorderLayout.CENTER);
		}

		private enum SortTestTableColumnID implements TableView.SimplifiedColumnIDInterface {
			Val1,Val2,Val3,Val4;
			SimplifiedColumnConfig colConf;
			SortTestTableColumnID() { colConf = new SimplifiedColumnConfig(toString(),String.class,10,-1,60,60); }
			@Override public SimplifiedColumnConfig getColumnConfig() { return colConf; }
		}
		
		private static class SortTestTableModel extends SimplifiedTableModel<SortTestTableColumnID> {
			
			private static final int UNSORTEDT_ROWS = 3;

			protected SortTestTableModel() {
				super(SortTestTableColumnID.values());
			}

			@Override public int getRowCount() { return 200; }
			@Override public int getUnsortedRowsCount() { return UNSORTEDT_ROWS; }

			@Override
			public Object getValueAt(int rowIndex, int columnIndex, SortTestTableColumnID columnID) {
				if (rowIndex<UNSORTEDT_ROWS)
					return String.format("%s.%d", columnID,rowIndex);
				switch(columnID) {
				case Val1: return ""+(((rowIndex-UNSORTEDT_ROWS)>>0) & 7);
				case Val2: return ""+(((rowIndex-UNSORTEDT_ROWS)>>3) & 7);
				case Val3: return ""+(((rowIndex-UNSORTEDT_ROWS)>>6) & 7);
				case Val4: return ""+(((rowIndex-UNSORTEDT_ROWS)>>9) & 7);
				}
				return null;
			}
		}
	}
*/

}
