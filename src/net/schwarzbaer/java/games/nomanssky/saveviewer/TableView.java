package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.Vector;

import javax.activation.DataHandler;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

class TableView {

	static class DebugTableContextMenu implements MouseListener, ActionListener {
	
		private JTable table;
		private JPopupMenu contextMenu;
		private enum TableContextMenuActionCommand { ShowWidths, CopyTableContent }
		
		public DebugTableContextMenu(JTable table) {
			this.table = table;
			table.addMouseListener(this);
			
			contextMenu = new JPopupMenu("Contextmenu");
			contextMenu.add(createMenuItem("Show Widths",TableContextMenuActionCommand.ShowWidths));
			contextMenu.add(createMenuItem("Copy Table Content",TableContextMenuActionCommand.CopyTableContent));
		}
	
		private JMenuItem createMenuItem(String label, TableContextMenuActionCommand actionCommand) {
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
				contextMenu.show(table, e.getX(), e.getY());
			}
		}
	
		@Override
		public void actionPerformed(ActionEvent e) {
			TableContextMenuActionCommand actionCommand = TableContextMenuActionCommand.valueOf(e.getActionCommand());
			switch(actionCommand) {
			case CopyTableContent: {
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
			} break;
			case ShowWidths: {
				TableColumnModel columnModel = table.getColumnModel();
				int[] widths = new int[columnModel.getColumnCount()];
				for (int i=0; i<columnModel.getColumnCount(); ++i)
					widths[i] = columnModel.getColumn(i).getWidth();
				System.out.println(Arrays.toString(widths));
			} break;
			}
		}
	
	}

	static class SimplifiedTable extends JTable {
		private static final long serialVersionUID = 6963749333892762675L;
//		private boolean useRowSorter;
		
		SimplifiedTable(boolean disableAutoResize, boolean installDebugContextMenu, boolean useRowSorter) {
			super();
//			this.useRowSorter = useRowSorter;
			if (disableAutoResize)
				setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			if (installDebugContextMenu)
				new TableView.DebugTableContextMenu(this);
			setAutoCreateRowSorter(useRowSorter);
		}
		
		SimplifiedTable(SimplifiedTableModel<?> dataModel, boolean disableAutoResize, boolean installDebugContextMenu, boolean useRowSorter) {
			this(disableAutoResize, installDebugContextMenu, useRowSorter);
			setModel(dataModel);
		}

		public void setModel(SimplifiedTableModel<?> dataModel) {
			super.setModel(dataModel);
			dataModel.setColumnWidths(this);
//			if (useRowSorter)
//				setRowSorter(new TableRowSorter<TableModel>(dataModel));
		}
		
	}

	static class SimplifiedColumnConfig {
		public String name;
		public int minWidth;
		public int maxWidth;
		public int prefWidth;
		public int currentWidth;
		public Class<?> columnClass;
		
		SimplifiedColumnConfig() {
			this("",String.class,-1,-1,-1,-1);
		}
		SimplifiedColumnConfig(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
			this.name = name;
			this.columnClass = columnClass;
			this.minWidth = minWidth;
			this.maxWidth = maxWidth;
			this.prefWidth = prefWidth;
			this.currentWidth = currentWidth;
		}
	}

	static interface SimplifiedColumnIDInterface {
		public SimplifiedColumnConfig getColumnConfig();
	}

	static abstract class SimplifiedTableModel<ColumnID extends Enum<ColumnID> & SimplifiedColumnIDInterface> implements TableModel {
		
		protected ColumnID[] columns;
		private Vector<TableModelListener> tableModelListeners;
	
		protected SimplifiedTableModel(ColumnID[] columns) {
			this.columns = columns;
			tableModelListeners = new Vector<>();
		}
	
		@Override public void addTableModelListener(TableModelListener l) { tableModelListeners.add(l); }
		@Override public void removeTableModelListener(TableModelListener l) { tableModelListeners.remove(l); }
		
		protected void fireTableModelEvent(TableModelEvent e) {
			for (TableModelListener tml:tableModelListeners)
				tml.tableChanged(e);
		}
		protected void fireTableCellUpdate(int rowIndex, int columnIndex) {
			fireTableModelEvent(new TableModelEvent(this, rowIndex, rowIndex, columnIndex, TableModelEvent.UPDATE));
		}
		protected void fireTableRowAdded(int rowIndex) {
			fireTableModelEvent(new TableModelEvent(this, rowIndex, rowIndex, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
		}
		protected void fireTableUpdate() {
			fireTableModelEvent(new TableModelEvent(this));
		}

		@Override public abstract int getRowCount();
		public abstract Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID);
		
		
		protected ColumnID getColumnID(int columnIndex) {
			if (columnIndex<0) return null;
			if (columnIndex<columns.length) return columns[columnIndex];
			return null;
		}
		
		@Override public int getColumnCount() { return columns.length; }
		
		@Override
		public String getColumnName(int columnIndex) {
			ColumnID columnID = getColumnID(columnIndex);
			if (columnID==null) return null;
			return columnID.getColumnConfig().name; //getName();
		}
	
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			ColumnID columnID = getColumnID(columnIndex);
			if (columnID==null) return null;
			return columnID.getColumnConfig().columnClass; //getColumnClass();
		}
	
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (rowIndex<0) return null;
			if (rowIndex>=getRowCount()) return null;
			ColumnID columnID = getColumnID(columnIndex);
			if (columnID==null) return null;
			return getValueAt(rowIndex, columnIndex, columnID);
		}
	
		@Override public boolean isCellEditable(int rowIndex, int columnIndex) {
			if (rowIndex<0) return false;
			if (rowIndex>=getRowCount()) return false;
			ColumnID columnID = getColumnID(columnIndex);
			if (columnID==null) return false;
			return isCellEditable(rowIndex, columnIndex, columnID);
		}
		protected boolean isCellEditable(int rowIndex, int columnIndex, ColumnID columnID) { return false; }
	
		@Override public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (rowIndex<0) return;
			if (rowIndex>=getRowCount()) return;
			ColumnID columnID = getColumnID(columnIndex);
			if (columnID==null) return;
			setValueAt(aValue, rowIndex, columnIndex, columnID);
		}
		protected void setValueAt(Object aValue, int rowIndex, int columnIndex, ColumnID columnID) {}
	
		public void setColumnWidths(JTable table) {
			TableColumnModel columnModel = table.getColumnModel();
			for (int i=0; i<columnModel.getColumnCount(); ++i) {
				ColumnID columnID = getColumnID(i);
				if (columnID!=null) {
					SimplifiedColumnConfig config = columnID.getColumnConfig();
					setColumnWidth(columnModel.getColumn(i), config.minWidth, config.maxWidth, config.prefWidth, config.currentWidth);
				}
			}
		}
	
		private void setColumnWidth(TableColumn column, int min, int max, int preferred, int width) {
			if (min>=0) column.setMinWidth(min);
			if (max>=0) column.setMinWidth(max);
			if (preferred>=0) column.setPreferredWidth(preferred);
			if (width    >=0) column.setWidth(width);
		}
	}

}
