package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.activation.DataHandler;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

public class TableView {

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

	public static class SimplifiedTable extends JTable {
		private static final long serialVersionUID = 6963749333892762675L;
		private boolean useRowSorter;
		private String name;
		
		SimplifiedTable(String name, boolean disableAutoResize, boolean installDebugContextMenu, boolean useRowSorter) {
			super();
			this.name = name;
			this.useRowSorter = useRowSorter;
			if (disableAutoResize)
				setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			if (installDebugContextMenu)
				new TableView.DebugTableContextMenu(this);
			//setAutoCreateRowSorter(useRowSorter);
		}
		
		public SimplifiedTable(String name, SimplifiedTableModel<?> dataModel, boolean disableAutoResize, boolean installDebugContextMenu, boolean useRowSorter) {
			this(name, disableAutoResize, installDebugContextMenu, useRowSorter);
			setModel(dataModel);
		}

		public void setModel(SimplifiedTableModel<?> dataModel) {
			super.setModel(dataModel);
			dataModel.setColumnWidths(this);
			if (useRowSorter)
				setRowSorter(new SimplifiedRowSorter(name,dataModel));
		}
		
	}
	
	static class SimplifiedRowSorter extends RowSorter<SimplifiedTableModel<?>> {

		private SimplifiedTableModel<?> model;
		private LinkedList<RowSorter.SortKey> keys;
		@SuppressWarnings("unused")
		private String name;
		private Integer[] modelRowIndexes;
		private int[] viewRowIndexes;

		SimplifiedRowSorter(String name, SimplifiedTableModel<?> model) {
			this.name = name;
			this.model = model;
			this.keys = new LinkedList<RowSorter.SortKey>();
			this.modelRowIndexes = null;
			this.viewRowIndexes = null;
		}
		
		public void setModel(SimplifiedTableModel<?> model) {
			this.model = model;
			this.keys = new LinkedList<RowSorter.SortKey>();
			sort();
		}

		@Override public SimplifiedTableModel<?> getModel() { return model; }

		private void log(String format, Object... values) {
			//System.out.printf(String.format("[%08X:%s] ", this.hashCode(), name)+format+"\r\n",values);
		}

		private static String toString(List<? extends RowSorter.SortKey> keys) {
			if (keys==null) return "<null>";
			String str = "";
			for (RowSorter.SortKey key:keys) {
				if (!str.isEmpty()) str+=", ";
				str+=key.getColumn()+":"+key.getSortOrder();
			}
			if (!str.isEmpty()) str = "[ "+str+" ]";
			return str;
		}
		
		private void sort() {
			if (model==null) {
				this.modelRowIndexes = null;
				this.viewRowIndexes = null;
				return;
			}
			
			log("sort() -> %s",toString(keys));
			
			int rowCount = getModelRowCount();
			if (modelRowIndexes==null || modelRowIndexes.length!=rowCount)
				modelRowIndexes = new Integer[rowCount];
			
			for (int i=0; i<modelRowIndexes.length; ++i)
				modelRowIndexes[i] = i;
			
			Comparator<Integer> comparator = null;
			
			int unsortedRows = model.getUnsortedRowsCount();
			if (0<unsortedRows)
				comparator = Comparator.comparingInt((Integer row)->(row<unsortedRows?row:unsortedRows));
			
			for (SortKey key:keys) {
				SortOrder sortOrder = key.getSortOrder();
				if (sortOrder==SortOrder.UNSORTED) continue;
				int column = key.getColumn();
				
				if      (model.getColumnClass(column) == Boolean.class) comparator = setComparator(comparator,sortOrder,(Integer row)->(Boolean)model.getValueAt(row,column));
				else if (model.getColumnClass(column) == String .class) comparator = setComparator(comparator,sortOrder,(Integer row)->(String )model.getValueAt(row,column));
				else if (model.getColumnClass(column) == Long   .class) comparator = setComparator(comparator,sortOrder,(Integer row)->(Long   )model.getValueAt(row,column));
				else if (model.getColumnClass(column) == Integer.class) comparator = setComparator(comparator,sortOrder,(Integer row)->(Integer)model.getValueAt(row,column));
				else if (model.getColumnClass(column) == Double .class) comparator = setComparator(comparator,sortOrder,(Integer row)->(Double )model.getValueAt(row,column));
				else if (model.getColumnClass(column) == Float  .class) comparator = setComparator(comparator,sortOrder,(Integer row)->(Float  )model.getValueAt(row,column));
				else comparator = setComparator(comparator,sortOrder,
							(Integer row)->{
								Object object = model.getValueAt(row,column);
								if (object==null) return null;
								return object.toString();
							});
			}
			
			if (comparator!=null)
				Arrays.sort(modelRowIndexes, comparator);
			
			if (viewRowIndexes==null || viewRowIndexes.length!=rowCount)
				viewRowIndexes = new int[rowCount];
			for (int i=0; i<viewRowIndexes.length; ++i) viewRowIndexes[i] = -1;
			for (int i=0; i<modelRowIndexes    .length; ++i) viewRowIndexes[modelRowIndexes[i]] = i;
			
			fireSortOrderChanged();
		}
		
		private <U extends Comparable<? super U>> Comparator<Integer> setComparator(Comparator<Integer> comp, SortOrder sortOrder, Function<? super Integer,? extends U> keyExtractor) {
			if (sortOrder==SortOrder.DESCENDING) {
				if (comp==null) {
					Comparator<Integer> comparator = Comparator.comparing(keyExtractor);
					return comparator.reversed();
				}
				Comparator<Integer> comparator = comp.reversed().thenComparing(keyExtractor);
				return comparator.reversed();
			} else {
				if (comp==null) return Comparator.comparing(keyExtractor);
				return comp.thenComparing(keyExtractor);
			}
		}
		
		
		@Override
		public void toggleSortOrder(int column) {
			RemovePred pred = new RemovePred(column);
			keys.removeIf(pred);
			if (pred.oldSortOrder == SortOrder.ASCENDING)
				keys.addFirst(new SortKey(column, SortOrder.DESCENDING));
			else
				keys.addFirst(new SortKey(column, SortOrder.ASCENDING));
			log("toggleSortOrder( %d )", column);
			sort();
		}

		private static class RemovePred implements Predicate<SortKey> {
			private int column;
			private SortOrder oldSortOrder;
			public RemovePred(int column) {
				this.column = column;
				this.oldSortOrder = SortOrder.UNSORTED;
			}
			@Override public boolean test(SortKey k) {
				if (k.getColumn()==column) {
					oldSortOrder = k.getSortOrder();
					return true;
				}
				return false;
			}
		}

		@Override
		public void setSortKeys(List<? extends RowSorter.SortKey> keys) {
			if (keys==null) this.keys = new LinkedList<RowSorter.SortKey>();
			else            this.keys = new LinkedList<RowSorter.SortKey>(keys);
			log("setSortKeys( %s )",toString(this.keys));
		}

		@Override
		public List<? extends RowSorter.SortKey> getSortKeys() {
			//log("getSortKeys()");
			return keys;
		}

		@Override
		public int convertRowIndexToModel(int index) {
			if (modelRowIndexes==null) return index;
			if (index<0) return -1;
			if (index>=modelRowIndexes.length) return -1;
			return modelRowIndexes[index];
		}

		@Override
		public int convertRowIndexToView(int index) {
			if (viewRowIndexes==null) return index;
			if (index<0) return -1;
			if (index>=viewRowIndexes.length) return -1;
			return viewRowIndexes[index];
		}

		@Override public int getViewRowCount() { return getModelRowCount(); }
		@Override public int getModelRowCount() { if (model==null) return 0; return model.getRowCount(); }

		@Override public void modelStructureChanged() { log("modelStructureChanged()"); sort(); }
		@Override public void allRowsChanged() { log("allRowsChanged()"); sort(); }
		@Override public void rowsInserted(int firstRow, int endRow) { log("rowsInserted( %d, %d )", firstRow, endRow); sort(); }
		@Override public void rowsDeleted(int firstRow, int endRow) { log("rowsDeleted( %d, %d )", firstRow, endRow); sort(); }
		@Override public void rowsUpdated(int firstRow, int endRow) { log("rowsUpdated( %d, %d )", firstRow, endRow); sort(); }
		@Override public void rowsUpdated(int firstRow, int endRow, int column) { log("rowsUpdated( %d, %d, %d )", firstRow, endRow, column); sort();
		}
		
	}

	public static class SimplifiedColumnConfig {
		public String name;
		public int minWidth;
		public int maxWidth;
		public int prefWidth;
		public int currentWidth;
		public Class<?> columnClass;
		
		SimplifiedColumnConfig() {
			this("",String.class,-1,-1,-1,-1);
		}
		public SimplifiedColumnConfig(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
			this.name = name;
			this.columnClass = columnClass;
			this.minWidth = minWidth;
			this.maxWidth = maxWidth;
			this.prefWidth = prefWidth;
			this.currentWidth = currentWidth;
		}
	}

	public static interface SimplifiedColumnIDInterface {
		public SimplifiedColumnConfig getColumnConfig();
	}

	public static abstract class SimplifiedTableModel<ColumnID extends Enum<ColumnID> & SimplifiedColumnIDInterface> implements TableModel {
		
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
		
		public int getUnsortedRowsCount() { return 0; }
		
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
