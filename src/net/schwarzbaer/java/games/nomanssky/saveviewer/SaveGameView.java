package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.Arrays;
import java.util.Vector;
import java.util.function.Function;

import javax.activation.DataHandler;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Stats.StatValue;
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
		tabbedPane.setPreferredSize(new Dimension(600, 500));
		
		tabbedPane.addTab("General",new GeneralDataPanel());
		if (data.stats!=null)
			tabbedPane.addTab("Stats",new StatsPanel());
		
		tabbedPane.addTab("Raw Data Tree",new RawDataTreePanel());
		
		add(tabbedPane,BorderLayout.CENTER);
	}
	
	
	@Override
	public String toString() {
		return file.getName();
	}


	private <T,R> Vector<R> convertVector( Vector<T> vector, Function<? super T,? extends R> convertValue ) {
		Vector<R> result = new Vector<>();
		for (T value : vector)
			result.add(convertValue.apply(value));
		return result;
	}


	private static class StatsTableModel implements TableModel {
		
		enum Column { ID, Name, IntValue, FloatValue, Denominator }
		private final static Column[] columns = { Column.ID, Column.Name, Column.IntValue, Column.FloatValue, Column.Denominator };
		
		private Vector<StatValue> statsList;
	
		public StatsTableModel(Vector<StatValue> statsList) {
			this.statsList = statsList;
		}
		
		@Override public void addTableModelListener(TableModelListener l) {}
		@Override public void removeTableModelListener(TableModelListener l) {}
		
		private Column getColumnID(int columnIndex) {
			if (columnIndex<0) return null;
			if (columnIndex>=columns.length) return null;
			return columns[columnIndex];
		}

		@Override public int getRowCount() { return statsList.size(); }
		@Override public int getColumnCount() { return columns.length; }
		@Override public boolean isCellEditable(int rowIndex, int columnIndex) { return false; }
		@Override public void setValueAt(Object aValue, int rowIndex, int columnIndex) {}
		
		@Override
		public String getColumnName(int columnIndex) {
			Column col = getColumnID(columnIndex);
			switch(col) {
			case ID  : return "ID";
			case Name: return "Name";
			case IntValue: return "Int";
			case FloatValue: return "Float";
			case Denominator: return "Denominator";
			}
			return "???["+columnIndex+"]";
		}
	
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			Column col = getColumnID(columnIndex);
			switch(col) {
			case ID  : 
			case Name: return String.class;
			case IntValue: return Long.class;
			case FloatValue: return Double.class;
			case Denominator: return Double.class;
			}
			return Object.class;
		}
	
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (rowIndex<0) return null;
			if (rowIndex>=statsList.size()) return null;
			
			Column col = getColumnID(columnIndex);
			StatValue statValue = statsList.get(rowIndex);
			
			if (col==null) return null;
			if (statValue==null) return null;
			
			switch(col) {
			case ID  : return statValue.ID;
			case Name: if (statValue.knownID!=null) return statValue.knownID.fullName; else return statValue.ID;
			case IntValue: return statValue.IntValue;
			case FloatValue: return statValue.FloatValue;
			case Denominator: return statValue.Denominator;
			}
			return null;
		}

		public void setColumnWidths(JTable table) {
			TableColumnModel columnModel = table.getColumnModel();
			for (int i=0; i<columnModel.getColumnCount(); ++i)
				setColumnWidth(i,columnModel.getColumn(i));
		}

		private void setColumnWidth(int columnIndex, TableColumn column) {
			// [125, 91, 59, 75, 36]
			Column col = getColumnID(columnIndex);
			switch(col) {
			case ID         : setColumnWidth(column,50,-1,120,120); break;
			case Name       : setColumnWidth(column,50,-1,160,160); break;
			case IntValue   : setColumnWidth(column,20,-1, 70, 70); break;
			case FloatValue : setColumnWidth(column,20,-1, 70, 70); break;
			case Denominator: setColumnWidth(column,20,-1, 40, 40); break;
			}
		}

		private void setColumnWidth(TableColumn column, int min, int max, int preferred, int width) {
			if (min>=0) column.setMinWidth(min);
			if (max>=0) column.setMinWidth(max);
			if (preferred>=0) column.setPreferredWidth(preferred);
			if (width    >=0) column.setWidth(width);
		}
	}

	private class StatsPanel extends JPanel {
		private static final long serialVersionUID = -1541256209397699528L;
		
		private JTable table;
		
		public StatsPanel() {
			super(new BorderLayout(3, 3));
			setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			
			table = new JTable();
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			JScrollPane tableScrollPane = new JScrollPane(table);
			tableScrollPane.setPreferredSize(new Dimension(600, 500));
			
			Vector<String> statConfigs = new Vector<>();
			statConfigs.add("Global");
			statConfigs.add("All Planets");
			statConfigs.addAll(convertVector(data.stats.planetStats, t -> "Planet "+t.address ));
			
			JComboBox<String> selector = new JComboBox<>(statConfigs);
			selector.addActionListener(e -> changeSelection( selector.getSelectedIndex() ));
			
			JPanel northPanel = new JPanel();
			northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.X_AXIS));
			northPanel.add(selector);
			
			JButton button;
			button = new JButton("show widths");
			button.addActionListener(e -> showColWidths());
			northPanel.add(button);
			button = new JButton("copy table content");
			button.addActionListener(e -> copyTableContent());
			northPanel.add(button);
			
//			add(selector,BorderLayout.NORTH);
			add(northPanel,BorderLayout.NORTH);
			add(tableScrollPane,BorderLayout.CENTER);
			
			changeSelection( selector.getSelectedIndex() );
		}

		private void copyTableContent() {
			TableModel model = table.getModel();
			StringBuilder sb = new StringBuilder();
			for (int row=0; row<model.getRowCount(); ++row) {
				for (int col=0; col<model.getColumnCount(); ++col) {
					if (col>0) sb.append("\t");
					sb.append(model.getValueAt(row, col));
				}
				sb.append("\r\n");
			}
			
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			Clipboard clipboard = toolkit.getSystemClipboard();
			DataHandler content = new DataHandler(sb.toString(),"text/plain");
			try { clipboard.setContents(content,null); }
			catch (IllegalStateException e1) { e1.printStackTrace(); }
		}

		private void showColWidths() {
			TableColumnModel columnModel = table.getColumnModel();
			int[] widths = new int[columnModel.getColumnCount()];
			for (int i=0; i<columnModel.getColumnCount(); ++i)
				widths[i] = columnModel.getColumn(i).getWidth();
			System.out.println(Arrays.toString(widths));
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
	}
	
	private class GeneralDataPanel extends JPanel {
		private static final long serialVersionUID = -3866983525686776846L;
		private JTextArea textArea;
	
		public GeneralDataPanel() {
			super(new BorderLayout(3, 3));
			setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			
			textArea = new JTextArea();
			fillData();
			
			JScrollPane treeScrollPane = new JScrollPane(textArea);
			treeScrollPane.setPreferredSize(new Dimension(600, 500));
			
			add(treeScrollPane,BorderLayout.CENTER);
		}

		private void fillData() {
			appendValue("Current Units", data.getUnits() );
			appendValue("Player Health", data.getPlayerHealth() );
			appendValue("Player Shield", data.getPlayerShield() );
			appendValue("Ship Health", data.getShipHealth() );
			appendValue("Ship Shield", data.getShipShield() );
			appendValue("Time Alive", data.getTimeAlive() );
			appendValue("Total PlayTime", data.getTotalPlayTime() );
			appendValue("Hazard Time Alive", data.getHazardTimeAlive() );
			
			appendEmptyLine();
			appendValue("Test value 1 (Bool)"   , data.getTestBool   ("PlayerStateData","Stats",7,"Stats",4,"Value","Denominator") );
			appendValue("Test value 2 (Integer)", data.getTestInteger("PlayerStateData","Stats",7,"Stats",4,"Value","Denominator") );
			appendValue("Test value 3 (Float)"  , data.getTestFloat  ("PlayerStateData","Stats",7,"Stats",4,"Value","Denominator") );
			appendValue("Test value 4 (String)" , data.getTestString ("PlayerStateData","Stats",7,"Stats",4,"Value","Denominator") );
			appendValue("Test value 5 (Float)"  , Type.Float  , "PlayerStateData","Stats",7,"Stats",4,"Value","Denominator");
			appendValue("Test value 6 (Integer)", Type.Integer, "PlayerStateData","Stats",7,"Stats",4,"Value","Denominator");
		}
		
		private void appendEmptyLine() {
			textArea.append("\r\n");
		}

		private void appendStatement(String label, String statement) {
			if (!textArea.getText().isEmpty())
				textArea.append("\r\n");
			textArea.append(label+": "+statement);
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

		private void appendValue(String label, Boolean value) { if (value==null) showError(label); else appendStatement(label, ""+value); }
		private void appendValue(String label, Long    value) { if (value==null) showError(label); else appendStatement(label, ""+value); }
		private void appendValue(String label, Double  value) { if (value==null) showError(label); else appendStatement(label, ""+value); }
		private void appendValue(String label, String  value) { if (value==null) showError(label); else appendStatement(label,    value); }
		
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

	enum RawDataTreeActionCommand { ShowPath, CopyPath, CopyValue }
	
	private class RawDataTreePanel extends JPanel implements MouseListener, ActionListener {
		private static final long serialVersionUID = -50409207801775293L;
		
		
		private JTree tree;
		private JPopupMenu contextMenu;
		private TreePath contextMenuTarget;

		public RawDataTreePanel() {
			super(new BorderLayout(3, 3));
			setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			
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
