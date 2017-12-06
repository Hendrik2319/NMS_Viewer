package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Locale;
import java.util.Vector;
import java.util.function.Function;

import javax.activation.DataHandler;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.DiscoveryData.AvailableData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.DiscoveryData.StoreData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.KnownWords;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.KnownWords.KnownWord;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Stats.StatValue;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.GalacticRegion;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Galaxy;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Planet;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.SolarSystem;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.UniverseAddress;
import net.schwarzbaer.java.games.nomanssky.saveviewer.TreeView.AbstractTreeNode;
import net.schwarzbaer.java.games.nomanssky.saveviewer.TreeView.JsonTreeNode;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.BoolValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.FloatValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.IntegerValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.PathIsNotSolvableException;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.StringValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value.Type;

class SaveGameView extends JPanel {
	private static final long serialVersionUID = -1641171938196309864L;
	
	final File file;
	final SaveGameData data;
	private JTabbedPane tabbedPane;

	public SaveGameView(File file, SaveGameData data) {
		super(new BorderLayout(3, 3));
		setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		
		this.file = file;
		this.data = data;
		
		tabbedPane = new JTabbedPane();
		//tabbedPane.setPreferredSize(new Dimension(620, 500));
		
		tabbedPane.addTab("General",new GeneralDataPanel(data));
		tabbedPane.addTab("Known Universe",new UniversePanel(data));
		if (data.stats     !=null) tabbedPane.addTab("Stats",new StatsPanel(data));
		if (data.knownWords!=null) tabbedPane.addTab("KnownWords",new KnownWordsPanel(data));
		
		tabbedPane.addTab("DiscoveryData (Avail.)",new DiscoveryDataAvailablePanel(data));
		tabbedPane.addTab("DiscoveryData (Store)",new DiscoveryDataStorePanel(data));
		
		tabbedPane.addTab("Raw Data Tree",new RawDataTreePanel(file,data));
		
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
	
	
	@Override
	public String toString() {
		return file.getName();
	}


	private static <T,R> Vector<R> convertVector( Vector<T> vector, Function<? super T,? extends R> convertValue ) {
		Vector<R> result = new Vector<>();
		for (T value : vector)
			result.add(convertValue.apply(value));
		return result;
	}
	
	private static class SaveGameViewTabPanel extends JPanel {
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
		
		protected void setNameForUniverseAddress(UniverseAddress ua) {
			if (ua==null) {
				JOptionPane.showMessageDialog(this, "Current location couldn't be identified.");
				return;
			}
			
			if (ua.isPlanet     ()) setNameForUniverseAddress(ua, data.universe.getOrCreatePlanet(ua));
			if (ua.isSolarSystem()) setNameForUniverseAddress(ua, data.universe.getOrCreateSolarSystem(ua));
			
			updateContent();
		}

		protected void setNameForUniverseAddress(UniverseAddress ua, SolarSystem system) {
			String name = JOptionPane.showInputDialog(this, "New name for solar system "+ua.getExtendedSigBoostCode(), system.getName());
			if (name!=null) {
				system.setName(name);
				SaveViewer.saveNamesOfUniverseObjectsToFile(data.universe);
			}
		}

		protected void setNameForUniverseAddress(UniverseAddress ua, Planet planet) {
			String name = JOptionPane.showInputDialog(this, "New name for planet "+ua.getExtendedSigBoostCode(), planet.getName());
			if (name!=null) {
				planet.setName(name);
				SaveViewer.saveNamesOfUniverseObjectsToFile(data.universe);
			}
			planet.setName(name);
		}
	}

	private static class UniversePanel extends SaveGameViewTabPanel implements TreeSelectionListener {
		private static final long serialVersionUID = -4594889224613582352L;
		
		private JTree tree;
		private DefaultTreeModel treeModel;
		private UniverseTreeNode selectedNode;

		private JTextArea textArea;
		private JLabel portalGlyphs;
		private JButton buttonSetName;

		public UniversePanel(SaveGameData data) {
			super(data);
			
			selectedNode = null;
			
			treeModel = new DefaultTreeModel(new UniverseTreeNode(data.universe));
			tree = new JTree(treeModel);
			JScrollPane treeScrollPane = new JScrollPane(tree);
			//treeScrollPane.setPreferredSize(new Dimension(600, 500));
			tree.addTreeSelectionListener(this);
			
			textArea = new JTextArea();
			textArea.setEditable(false);
			textArea.setPreferredSize(new Dimension(600, 100));
			
			portalGlyphs = new JLabel();
			portalGlyphs.setPreferredSize(new Dimension(50*12, 45*1));
			
			JPanel southPanel = new JPanel(new BorderLayout(3, 3));
//			southPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(""), BorderFactory.createEmptyBorder(3, 3, 3, 3)));
			southPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(3, 3, 3, 3)));
			southPanel.add(textArea,BorderLayout.CENTER);
			southPanel.add(portalGlyphs,BorderLayout.SOUTH);
			
			buttonSetName = createButton("Set name",e -> setNameForSelectedNode());
			
			JPanel buttonPanel = new JPanel();
			buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
			buttonPanel.add(buttonSetName);
			
			add(treeScrollPane,BorderLayout.CENTER);
			add(southPanel,BorderLayout.SOUTH);
			add(buttonPanel,BorderLayout.EAST);
		}
		
		@Override
		public void updateContent() {
			for (int i=0; i<tree.getRowCount(); ++i) {
				TreePath path = tree.getPathForRow(i);
				if (path==null) continue;
				Object comp = path.getLastPathComponent();
				if (comp instanceof TreeNode)
					treeModel.nodeChanged((TreeNode)comp);
			}
		}

		private void disableSetNameButton() {
			changeSetNameButton(false,"Set Name");
		}
		
		private void changeSetNameButton(boolean enabled, String title) {
			buttonSetName.setEnabled(enabled);
			buttonSetName.setText(title);
		}
		
		private void setNameForSelectedNode() {
			switch(selectedNode.type) {
			case Planet     : setNameForUniverseAddress(selectedNode.planet     .getUniverseAddress(),selectedNode.planet     ); break;
			case SolarSystem: setNameForUniverseAddress(selectedNode.solarSystem.getUniverseAddress(),selectedNode.solarSystem); break;
			default:break;
			}
			treeModel.nodeChanged(selectedNode);
		}

		@Override
		public void valueChanged(TreeSelectionEvent e) {
			Object comp = e.getPath().getLastPathComponent();
			if (!(comp instanceof UniverseTreeNode)) {
				selectedNode = null;
				return;
			}
			
			textArea.setText("");
			selectedNode = (UniverseTreeNode)comp;
			UniverseAddress ua;
			int n;
			
			if (selectedNode.type!=NodeType.Planet)
				portalGlyphs.setIcon(null);
			
			switch(selectedNode.type) {
			case Planet:
				changeSetNameButton(true, selectedNode.planet.hasName()?"Change name":"Set name");
				ua = selectedNode.planet.getUniverseAddress();
				long portalGlyphCode = ua.getPortalGlyphCode();
				portalGlyphs.setIcon(createPortalGlyphs(portalGlyphCode));
				
				textArea.append(String.format("Universe Coordinates       : %s\r\n", ua.getCoordinates()));
				textArea.append(String.format("Universe Address           : 0x%014X\r\n", ua.getAddress()));
				textArea.append(String.format("Portal Glyph Code          : %012X\r\n", portalGlyphCode));
				textArea.append(String.format("Extended SignalBoster Code : %s", ua.getExtendedSigBoostCode()));
				break;
				
			case SolarSystem:
				changeSetNameButton(true, selectedNode.solarSystem.hasName()?"Change name":"Set name");
				n = selectedNode.solarSystem.planets.size();
				textArea.append(String.format("%d known planet%s\r\n", n, n>1?"s":""));
				ua = selectedNode.solarSystem.getUniverseAddress();
				textArea.append(String.format("Universe Coordinates : %s\r\n", ua.getSolarSystemCoordinates()));
				textArea.append(String.format("SignalBoster Code    : %s", ua.getSigBoostCode()));
				break;
				
			case GalacticRegion:
				disableSetNameButton();
				n = selectedNode.galacticRegion.solarSystems.size();
				textArea.append(String.format("%d known solar system%s\r\n", n, n>1?"s":""));
				ua = selectedNode.galacticRegion.getUniverseAddress();
				textArea.append(String.format("Universe Coordinates      : %s\r\n", ua.getGalacticRegionCoordinates()));
				textArea.append(String.format("Reduced SignalBoster Code : %s", ua.getReducedSigBoostCode()));
				break;
				
			case Galaxy:
				disableSetNameButton();
				n = selectedNode.galaxy.galacticRegions.size();
				textArea.append(String.format("%d known region%s\r\n", n, n>1?"s":""));
				break;
				
			case Universe:
				disableSetNameButton();
				n = selectedNode.universe.galaxies.size();
				textArea.append(String.format("%d known galax%s\r\n", n, n>1?"ies":"y"));
				break;
				
			case Unknown:
				disableSetNameButton();
				break;
			}
		}
		
		private Icon createPortalGlyphs(long portalGlyphCode) {
			BufferedImage image = new BufferedImage(50*12, 45*1, BufferedImage.TYPE_INT_RGB);
			Graphics graphics = image.getGraphics();
			
			for (int i=11; i>=0; --i) {
				int nr = (int)(portalGlyphCode&0xF);
				portalGlyphCode = portalGlyphCode>>4;
				BufferedImage glyph = SaveViewer.portalGlyphsIS_50_45.getImage(nr);
				graphics.drawImage(glyph, i*50, 0, null);
			}
			return new ImageIcon(image);
		}

		enum NodeType { Universe, Galaxy, GalacticRegion, SolarSystem, Planet, Unknown }
		
		static class UniverseTreeNode extends AbstractTreeNode<UniverseTreeNode> {

			private NodeType type;
			private Universe universe;
			private Galaxy galaxy;
			private GalacticRegion galacticRegion;
			private SolarSystem solarSystem;
			private Planet planet;

			private UniverseTreeNode(UniverseTreeNode parent) {
				super(parent);
				this.type = NodeType.Unknown;
				this.universe = null;
				this.galaxy = null;
				this.galacticRegion = null;
				this.solarSystem = null;
				this.planet = null;
			}
			
			public UniverseTreeNode(Universe universe) {
				this((UniverseTreeNode)null);
				this.universe = universe;
				this.type = NodeType.Universe;
			}

			public UniverseTreeNode(UniverseTreeNode parent, Object obj) {
				this(parent);
				if (obj instanceof Galaxy        ) { this.galaxy         = (Galaxy        )obj; type = NodeType.Galaxy        ; }
				if (obj instanceof GalacticRegion) { this.galacticRegion = (GalacticRegion)obj; type = NodeType.GalacticRegion; }
				if (obj instanceof SolarSystem   ) { this.solarSystem    = (SolarSystem   )obj; type = NodeType.SolarSystem   ; }
				if (obj instanceof Planet        ) { this.planet         = (Planet        )obj; type = NodeType.Planet        ; }
			}

			@Override
			public String toString() {
				if (universe      !=null) { return universe.toString(); }
				if (galaxy        !=null) { return galaxy.toString(); }
				if (galacticRegion!=null) { return galacticRegion.toString(); }
				if (solarSystem   !=null) { return solarSystem.toString(); }
				if (planet        !=null) { return planet.toString(); }
				return "???";
			}

			@Override
			public boolean getAllowsChildren() {
				return (universe!=null || galaxy!=null || galacticRegion!=null || solarSystem!=null);
			}

			@Override
			void createChildren() {
				if (universe      !=null) { createChildren(universe.galaxies          ); return; }
				if (galaxy        !=null) { createChildren(galaxy.galacticRegions     ); return; }
				if (galacticRegion!=null) { createChildren(galacticRegion.solarSystems); return; }
				if (solarSystem   !=null) { createChildren(solarSystem.planets        ); return; }
				if (planet        !=null) { children = new UniverseTreeNode[0]; return; }
			}
			
			<T> void createChildren(Vector<T> vector) {
				children = new UniverseTreeNode[vector.size()];
				for (int i=0; i<children.length; ++i)
					children[i] = new UniverseTreeNode(this,vector.get(i));
			}
		}
	}
	
	

	private static class KnownWordsPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = 7096092479075372171L;
		
		private JTable table;
		
		public KnownWordsPanel(SaveGameData data) {
			super(data);
			
			KnownWordsTableModel tableModel = new KnownWordsTableModel(data.knownWords);
			table = new JTable(tableModel);
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			JScrollPane tableScrollPane = new JScrollPane(table);
			tableScrollPane.setPreferredSize(new Dimension(800, 500));
			tableModel.setColumnWidths(table);
			
			if (SaveViewer.DEBUG)
				new TableView.DebugTableContextMenu(table);
			
			add(tableScrollPane,BorderLayout.CENTER);
		}

		private enum KnownWordsTableColumnID implements TableView.SimplifiedColumnIDInterface {
			WordID        ("ID"  , 50,-1,120,120),
			TranslatedWord("Word", 50,-1,100,100),
			Race          (""    , 20,-1, 70, 70);
		
			private TableView.SimplifiedColumnConfig columnConfig;
			
			KnownWordsTableColumnID() {
				columnConfig = new TableView.SimplifiedColumnConfig();
			}
			KnownWordsTableColumnID(String name, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				columnConfig = new TableView.SimplifiedColumnConfig(name, String.class, minWidth, maxWidth, prefWidth, currentWidth);
			}
			@Override public TableView.SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
		}

		private static class KnownWordsTableModel extends TableView.SimplifiedTableModel<KnownWordsTableColumnID> {
		
			private KnownWords knownWords;
			private int numberOfRaces;
			
			public KnownWordsTableModel(KnownWords knownWords) {
				super(new KnownWordsTableColumnID[]{ KnownWordsTableColumnID.WordID, KnownWordsTableColumnID.TranslatedWord });
				this.knownWords = knownWords;
				numberOfRaces = this.knownWords.wordCounts.length;
			}
			
			@Override
			protected KnownWordsTableColumnID getColumnID(int columnIndex) {
				if (columnIndex<columns.length) return super.getColumnID(columnIndex);
				if (columnIndex<columns.length+numberOfRaces) return KnownWordsTableColumnID.Race;
				return null;
			}
			@Override public int getColumnCount() { return columns.length+numberOfRaces; }
			@Override public String getColumnName(int columnIndex) {
				if (columnIndex<columns.length) return super.getColumnName(columnIndex);
				if (columnIndex<columns.length+numberOfRaces) {
					switch(columnIndex-columns.length) {
					case 0: return "Gek";
					case 1: return "Vy'keen";
					case 2: return "Korvax";
					case 4: return "Atlas";
					default:
						return "Race "+(columnIndex-columns.length);
					}
				}
				return null;
			}
		
			@Override
			public int getRowCount() {
				return knownWords.wordList.size()+1;
			}
			
			@Override
			public Object getValueAt(int rowIndex, int columnIndex, KnownWordsTableColumnID columnID) {
				if (rowIndex==0) {
					switch(columnID) {
					case WordID: return String.format("%d different words", knownWords.wordList.size());
					case TranslatedWord: return "";
					case Race:
						int race = columnIndex-columns.length;
						return String.format(Locale.ENGLISH,"%d (%1.1f%%)", knownWords.wordCounts[race], knownWords.wordCounts[race]*100.0f/knownWords.wordList.size());
					}
				} else {
					KnownWord knownWord = knownWords.wordList.get(rowIndex-1);
					if (knownWord==null) return null;
					
					switch(columnID) {
					case WordID: return knownWord.word;
					case TranslatedWord: return "";
					case Race:
						int race = columnIndex-columns.length;
						return (race>=knownWord.races.length)?"???":(knownWord.races[race]?"known":"");
					}
				}
				return null;
			}
		}
	}
	
	private static class DiscoveryDataAvailablePanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = 2870833302184314416L;
		
		private JTable table;

		public DiscoveryDataAvailablePanel(SaveGameData data) {
			super(data);
			
			DDATableModel tableModel = new DDATableModel();
			table = new JTable(tableModel);
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			JScrollPane tableScrollPane = new JScrollPane(table);
			tableScrollPane.setPreferredSize(new Dimension(600, 500));
			tableModel.setColumnWidths(table);
			
			new TableView.DebugTableContextMenu(table);
			
			add(tableScrollPane,BorderLayout.CENTER);
		}

		private enum DDAColumnID implements TableView.SimplifiedColumnIDInterface {
			TSrec ("TSrec" ,   Long.class, 50,-1, 80, 80), //[81, 161, 91, 135, 139]
			DD_UA ("DD_UA" , String.class, 50,-1,160,160),
			DD_DT ("DD_DT" , String.class, 50,-1, 90, 90),
			DD_VP0("DD_VP0", String.class, 50,-1,130,130),
			DD_VP1("DD_VP1", String.class, 50,-1,130,130);
			
			private TableView.SimplifiedColumnConfig columnConfig;
			
			DDAColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				columnConfig = new TableView.SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			@Override public TableView.SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
		}

		private class DDATableModel extends TableView.SimplifiedTableModel<DDAColumnID> {

			protected DDATableModel() {
				super(DDAColumnID.values());
			}

			@Override
			public int getRowCount() {
				return data.discoveryData.availableData.size();
			}

			@Override
			public Object getValueAt(int rowIndex, int columnIndex, DDAColumnID columnID) {
				AvailableData availableData = data.discoveryData.availableData.get(rowIndex);
				if (availableData==null) return null;
				switch(columnID) {
				case TSrec : if (availableData.TSrec ==null) return -1; else return availableData.TSrec;
				case DD_UA : if (availableData.DD.UA ==null) return ""; else return availableData.DD.UA.getExtendedSigBoostCode();
				case DD_DT : if (availableData.DD.DT ==null) return ""; else return availableData.DD.DT;
				case DD_VP0: if (availableData.DD.VP0==null) return ""; else return availableData.DD.VP0;
				case DD_VP1: if (availableData.DD.VP1==null) return ""; else return availableData.DD.VP1;
				}
				return null;
			}
		}
	}
	
	private static class DiscoveryDataStorePanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = 6619075068331735784L;
		
		private JTable table;

		public DiscoveryDataStorePanel(SaveGameData data) {
			super(data);
			
			DDSTableModel tableModel = new DDSTableModel();
			table = new JTable(tableModel);
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			JScrollPane tableScrollPane = new JScrollPane(table);
			tableScrollPane.setPreferredSize(new Dimension(600, 500));
			tableModel.setColumnWidths(table);
			
			new TableView.DebugTableContextMenu(table);
			
			add(tableScrollPane,BorderLayout.CENTER);
		}

		private enum DDSColumnID implements TableView.SimplifiedColumnIDInterface {
			DD_UA ("DD_UA" , String.class, 50,-1,160,160),
			DD_DT ("DD_DT" , String.class, 50,-1, 90, 90),
			DD_VP0("DD_VP0", String.class, 50,-1,130,130),
			DD_VP1("DD_VP1", String.class, 50,-1,130,130),
			DM     ("DM"     , String.class, 20,-1, 40, 40),
			DM_CN  ("DM_CN"  , String.class, 50,-1, 80, 80),
			OWS_LID("OWS_LID", String.class, 50,-1,120,120),
			OWS_UID("OWS_UID", String.class, 50,-1,120,120),
			OWS_USN("OWS_USN", String.class, 50,-1, 80, 80),
			OWS_TS ("OWS_TS" ,   Long.class, 50,-1, 80, 80),
			RID    ("RID"    , String.class, 50,-1,350,350);
			
			private TableView.SimplifiedColumnConfig columnConfig;
			
			DDSColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				columnConfig = new TableView.SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			@Override public TableView.SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
		}

		private class DDSTableModel extends TableView.SimplifiedTableModel<DDSColumnID> {

			protected DDSTableModel() {
				super(DDSColumnID.values());
			}

			@Override
			public int getRowCount() {
				return data.discoveryData.storeData.size();
			}

			@Override
			public Object getValueAt(int rowIndex, int columnIndex, DDSColumnID columnID) {
				StoreData storeData = data.discoveryData.storeData.get(rowIndex);
				if (storeData==null) return null;
				switch(columnID) {
				case DD_UA : if (storeData.DD.UA ==null) return ""; else return storeData.DD.UA.getExtendedSigBoostCode();
				case DD_DT : if (storeData.DD.DT ==null) return ""; else return storeData.DD.DT;
				case DD_VP0: if (storeData.DD.VP0==null) return ""; else return storeData.DD.VP0;
				case DD_VP1: if (storeData.DD.VP1==null) return ""; else return storeData.DD.VP1;
				case DM     : if (storeData.DM     ==null) return ""; else return storeData.DM     ;
				case DM_CN  : if (storeData.DM_CN  ==null) return ""; else return storeData.DM_CN  ;
				case OWS_LID: if (storeData.OWS_LID==null) return ""; else return storeData.OWS_LID;
				case OWS_UID: if (storeData.OWS_UID==null) return ""; else return storeData.OWS_UID;
				case OWS_USN: if (storeData.OWS_USN==null) return ""; else return storeData.OWS_USN;
				case OWS_TS : if (storeData.OWS_TS ==null) return -1; else return storeData.OWS_TS ;
				case RID    : if (storeData.RID    ==null) return ""; else return storeData.RID    ;
				}
				return null;
			}
		}
	}
	
	private static class StatsPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = -1541256209397699528L;
		
		private JTable table;
		
		public StatsPanel(SaveGameData data) {
			super(data);
			
			table = new JTable();
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			JScrollPane tableScrollPane = new JScrollPane(table);
			tableScrollPane.setPreferredSize(new Dimension(600, 500));
			
			Vector<String> statConfigs = new Vector<>();
			statConfigs.add("Global");
			statConfigs.add("All Planets");
			statConfigs.addAll(convertVector(data.stats.planetStats, ps -> "Planet "+ps.planet));
			
			JComboBox<String> selector = new JComboBox<>(statConfigs);
			selector.addActionListener(e -> changeSelection( selector.getSelectedIndex() ));
			
			if (SaveViewer.DEBUG)
				new TableView.DebugTableContextMenu(table);
				
			add(selector,BorderLayout.NORTH);
			add(tableScrollPane,BorderLayout.CENTER);
			
			changeSelection( selector.getSelectedIndex() );
		}

		private void changeSelection(int index) {
			switch (index) {
			case -1:
				table.setModel(new DefaultTableModel());
				break;
			case 0: {
				StatsTableModel tableModel = new StatsTableModel(data.stats.globalStats);
				table.setModel(tableModel);
				tableModel.setColumnWidths(table);
			} break;
			case 1:
				table.setModel(new DefaultTableModel());
				//tableModel.setColumnWidths(table);
				break;
			default: {
				int planetIndex = index-2;
				StatsTableModel tableModel = new StatsTableModel(data.stats.planetStats.get(planetIndex).stats);
				table.setModel(tableModel);
				tableModel.setColumnWidths(table);
			} break;
			}
		}

		private enum StatsTableColumnID implements TableView.SimplifiedColumnIDInterface {
			ID         ("ID"         , String.class, 50,-1,120,120),
			Name       ("Name"       , String.class, 50,-1,210,210),
			IntValue   ("Int"        , Long.class  , 20,-1, 70, 70),
			FloatValue ("Float"      , Double.class, 20,-1, 70, 70),
			Denominator("Denominator", Double.class, 20,-1, 40, 40);
			
			private TableView.SimplifiedColumnConfig columnConfig;
			
			StatsTableColumnID() {
				columnConfig = new TableView.SimplifiedColumnConfig();
				columnConfig.name = toString();
			}
			StatsTableColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				columnConfig = new TableView.SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			@Override public TableView.SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
		}

		private static class StatsTableModel extends TableView.SimplifiedTableModel<StatsTableColumnID> {
			
			private Vector<StatValue> statsList;
			
			public StatsTableModel(Vector<StatValue> statsList) {
				super(new StatsTableColumnID[]{ StatsTableColumnID.ID, StatsTableColumnID.Name, StatsTableColumnID.IntValue, StatsTableColumnID.FloatValue, StatsTableColumnID.Denominator });
				this.statsList = statsList;
			}
		
			@Override public int getRowCount() { return statsList.size(); }
		
			@Override
			public Object getValueAt(int rowIndex, int columnIndex, StatsTableColumnID columnID) {
				StatValue statValue = statsList.get(rowIndex);
				
				if (statValue==null) return null;
				
				switch(columnID) {
				case ID  : return statValue.ID;
				case Name: if (statValue.knownID!=null) return statValue.knownID.fullName; else return statValue.ID;
				case IntValue: return statValue.IntValue;
				case FloatValue: return statValue.FloatValue;
				case Denominator: return statValue.Denominator;
				}
				return null;
			}
		
			@Override
			protected boolean isCellEditable(int rowIndex, int columnIndex, StatsTableColumnID columnID) {
				return columnID==StatsTableColumnID.Name;
			}
		
			@Override
			protected void setValueAt(Object aValue, int rowIndex, int columnIndex, StatsTableColumnID columnID) {
				if (columnID!=StatsTableColumnID.Name) { fireTableCellUpdate(rowIndex, columnIndex); return; }
				
				StatValue statValue = statsList.get(rowIndex);
				
				if (statValue.knownID==null || aValue==null) { fireTableCellUpdate(rowIndex, columnIndex); return; }
				
				statValue.knownID.fullName = aValue.toString();
				SaveViewer.saveKnownStatIDsToFile();
			}
		}
	}

	private static class GeneralDataPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = -3866983525686776846L;
		
		private JTextArea textArea;
	
		public GeneralDataPanel(SaveGameData data) {
			super(data);
			
			textArea = new JTextArea();
			textArea.setEditable(false);
			
			JScrollPane treeScrollPane = new JScrollPane(textArea);
			treeScrollPane.setPreferredSize(new Dimension(600, 500));
			
			JPanel buttonPanel = new JPanel();
			buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
			buttonPanel.add(createButton("Set name for current position",e -> setNameForUniverseAddress(data.general.getCurrentUniverseAddress())));
			
			add(treeScrollPane,BorderLayout.CENTER);
			add(buttonPanel,BorderLayout.SOUTH);
			
			updateContent();
		}

		@Override
		public void updateContent() {
			textArea.setText("");
			appendValue("Current Units    ", data.general.getUnits() );
			appendValue("Player Health    ", data.general.getPlayerHealth() );
			appendValue("Player Shield    ", data.general.getPlayerShield() );
			appendValue("Ship Health      ", data.general.getShipHealth() );
			appendValue("Ship Shield      ", data.general.getShipShield() );
			appendValue("Time Alive       ", data.general.getTimeAlive() );
			appendValue("Total PlayTime   ", data.general.getTotalPlayTime() );
			appendValue("Hazard Time Alive", data.general.getHazardTimeAlive() );
			
			appendEmptyLine();
			UniverseAddress currentUA = data.general.getCurrentUniverseAddress();
			if (currentUA!=null) {
				appendLine("Current Location in Universe:");
				if (currentUA.isPlanet     ()) {
					Planet planet = data.universe.findPlanet(currentUA);
					if (planet!=null && planet.hasName())
						appendLine("    on planet \""+planet.getName()+"\"");
					else
						appendLine("    on a planet");
				}
				if (currentUA.isSolarSystem()) {
					SolarSystem system = data.universe.findSolarSystem(currentUA);
					if (system!=null && system.hasName())
						appendLine("    in solar system \""+system.getName()+"\"");
					else
						appendLine("    in a solar system");
				}
				appendLine("    "+currentUA.getCoordinates());
				appendLine("    "+currentUA.getExtendedSigBoostCode());
			}
			
			
			
//			if (SaveViewer.DEBUG) {
//				appendEmptyLine();
//				appendValue("Test value 1 (Bool)"   , data.general.getTestBool   ("PlayerStateData","Stats",7,"Stats",4,"Value","Denominator") );
//				appendValue("Test value 2 (Integer)", data.general.getTestInteger("PlayerStateData","Stats",7,"Stats",4,"Value","Denominator") );
//				appendValue("Test value 3 (Float)"  , data.general.getTestFloat  ("PlayerStateData","Stats",7,"Stats",4,"Value","Denominator") );
//				appendValue("Test value 4 (String)" , data.general.getTestString ("PlayerStateData","Stats",7,"Stats",4,"Value","Denominator") );
//				appendValue("Test value 5 (Float)"  , Type.Float  , "PlayerStateData","Stats",7,"Stats",4,"Value","Denominator");
//				appendValue("Test value 6 (Integer)", Type.Integer, "PlayerStateData","Stats",7,"Stats",4,"Value","Denominator");
//			}
		}
		
		private void appendEmptyLine() {
			textArea.append("\r\n");
		}

		private void appendStatement(String label, String statement) {
			String line = label+": "+statement;
			appendLine(line);
		}

		private void appendLine(String line) {
			if (!textArea.getText().isEmpty())
				textArea.append("\r\n");
			textArea.append(line);
		}

		private void showError(String label) {
			switch (data.error) {
			case NoError:
				appendStatement(label,"???");
				System.out.println("Value \""+label+"\" is <null>, but error is unknown: \""+data.errorMessage+"\"");
				break;
			case PathIsNotSolvable:
				appendStatement(label,"Value not found");
				System.out.println("Value \""+label+"\" not found: "+data.errorMessage);
				break;
			case UnexpectedType:
				appendStatement(label,"Value has unexpected type");
				System.out.println("Value \""+label+"\" has unexpected type: "+data.errorMessage);
				break;
			case ValueIsNull:
				appendStatement(label,"<null>");
				System.out.println("Value \""+label+"\" is <null>: "+data.errorMessage);
				break;
			}
		}

		@SuppressWarnings("unused")
		private void appendValue(String label, Boolean value) { if (value==null) showError(label); else appendStatement(label, ""+value); }
		private void appendValue(String label, Long    value) { if (value==null) showError(label); else appendStatement(label, ""+value); }
		@SuppressWarnings("unused")
		private void appendValue(String label, Double  value) { if (value==null) showError(label); else appendStatement(label, ""+value); }
		@SuppressWarnings("unused")
		private void appendValue(String label, String  value) { if (value==null) showError(label); else appendStatement(label,    value); }
		
		@SuppressWarnings("unused")
		private void appendValue(String label, Type expectedType, Object... path) {
			Value value = null;
			try {
				value = JSON_Data.getSubNode(data.json_data,path);
				switch(expectedType) {
				case Bool   : if (appendBoolValue   (label,value)) return; break;
				case Float  : if (appendFloatValue  (label,value)) return; break;
				case Integer: if (appendIntegerValue(label,value)) return; break;
				case String : if (appendStringValue (label,value)) return; break;
				default: break;
				}
			} catch (PathIsNotSolvableException e) {
				appendStatement(label,"Value not found");
				System.out.println("Value \""+label+"\" not found: "+e.getMessage());
				return;
			}
			appendStatement(label,"Value has unexpected type");
			System.out.println("Value \""+label+"\" has unexpected type: "+(value==null?"<null>":value.getClass()));
		}

		private boolean appendBoolValue(String label, Value value) {
			if (value instanceof BoolValue) {
				BoolValue valueB = (BoolValue)value;
				appendStatement(label, ""+valueB.value);
				return true;
			}
			return false;
		}

		private boolean appendIntegerValue(String label, Value value) {
			if (value instanceof IntegerValue) {
				IntegerValue valueI = (IntegerValue)value;
				appendStatement(label, ""+valueI.value);
				return true;
			}
			return false;
		}

		private boolean appendFloatValue(String label, Value value) {
			if (value instanceof FloatValue) {
				FloatValue valueF = (FloatValue)value;
				appendStatement(label, ""+valueF.value);
				return true;
			}
			return false;
		}

		private boolean appendStringValue(String label, Value value) {
			if (value instanceof StringValue) {
				StringValue valueS = (StringValue)value;
				appendStatement(label, valueS.value);
				return true;
			}
			return false;
		}
	}

	private static class RawDataTreePanel extends SaveGameViewTabPanel implements MouseListener, ActionListener {
		private static final long serialVersionUID = -50409207801775293L;
		
		enum RawDataTreeActionCommand { ShowPath, CopyPath, CopyValue }

		private JTree tree;
		private JPopupMenu contextMenu;
		private TreePath contextMenuTarget;

		public RawDataTreePanel(File file, SaveGameData data) {
			super(data);
			
			SaveViewer.log("Create tree from file \"%s\" ...",file.getPath());
			tree = new JTree(new DefaultTreeModel(new JsonTreeNode(data.json_data)));
			tree.addMouseListener(this);
			//tree.setCellRenderer(new DefaultTreeCellRenderer());
			JScrollPane treeScrollPane = new JScrollPane(tree);
			treeScrollPane.setPreferredSize(new Dimension(600, 500));
			SaveViewer.log_ln(" done");
			
			contextMenu = new JPopupMenu("Contextmenu");
			contextMenu.add(createMenuItem("Copy Path",RawDataTreeActionCommand.CopyPath));
			contextMenu.add(createMenuItem("Copy Value",RawDataTreeActionCommand.CopyValue));
			contextMenu.add(createMenuItem("Show Path",RawDataTreeActionCommand.ShowPath));
			
			add(treeScrollPane,BorderLayout.CENTER);
		}

		private JMenuItem createMenuItem(String label, RawDataTreeActionCommand actionCommand) {
			JMenuItem menuItem = new JMenuItem(label);
			menuItem.addActionListener(this);
			menuItem.setActionCommand(actionCommand.toString());
			return menuItem;
		}

		@Override public void mousePressed(MouseEvent e) {}
		@Override public void mouseReleased(MouseEvent e) {}
		@Override public void mouseEntered(MouseEvent e) {}
		@Override public void mouseExited(MouseEvent e) {}
		@Override public void mouseClicked(MouseEvent e) {
			if (e.getButton()==MouseEvent.BUTTON3) {
				contextMenuTarget = tree.getPathForLocation(e.getX(), e.getY());
				contextMenu.show(tree, e.getX(), e.getY());
			}
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			RawDataTreeActionCommand actionCommand = RawDataTreeActionCommand.valueOf(e.getActionCommand());
			switch(actionCommand) {
			case CopyPath: {
				Toolkit toolkit = Toolkit.getDefaultToolkit();
				Clipboard clipboard = toolkit.getSystemClipboard();
				DataHandler content = new DataHandler(pathToShortString(contextMenuTarget),"text/plain");
				try { clipboard.setContents(content,null); }
				catch (IllegalStateException e1) { e1.printStackTrace(); }
			} break;
			case ShowPath:
				System.out.println("Path: "+contextMenuTarget);
				System.out.println("    = "+pathToShortString(contextMenuTarget));
				break;
			case CopyValue:
				Toolkit toolkit = Toolkit.getDefaultToolkit();
				Clipboard clipboard = toolkit.getSystemClipboard();
				Object comp = contextMenuTarget.getLastPathComponent();
				if (!(comp instanceof JsonTreeNode)) return;
				
				Value data = ((JsonTreeNode)comp).data;
				
				DataHandler content = new DataHandler(data.toString(),"text/plain");
				try { clipboard.setContents(content,null); }
				catch (IllegalStateException e1) { e1.printStackTrace(); }
				break;
			}
		}

		private String pathToShortString(TreePath treePath) {
			StringBuilder sb = new StringBuilder();
			for (int i=0; i<treePath.getPathCount(); ++i) {
				Object pathComponent = treePath.getPathComponent(i);
				if (pathComponent instanceof JsonTreeNode) {
					JsonTreeNode node = (JsonTreeNode)pathComponent;
					if (node.parent==null)
						sb.append("[root]");
					else if (node.name==null) {
						if (node.parent.data.type!=Type.Array)
							sb.append("<nameless value inside of non array>");
						else
							sb.append("["+node.parent.getIndex(node)+"]");
					} else
						sb.append("."+node.name);
				} else
					sb.append("<unknown class of \""+pathComponent+"\">");
			}
			return sb.toString();
		}
	}
}
