package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.swing.AbstractCellEditor;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import net.schwarzbaer.gui.Canvas;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Gui;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Images;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Images.NamedColor;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Images.UpgradeCategory;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.TimeStamp;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveViewer;

public class TableView {

	public static class DebugTableContextMenu extends JPopupMenu implements ActionListener {
		private static final long serialVersionUID = 13123780239483223L;
		
		private JTable table;
		private enum TableContextMenuActionCommand { ShowWidths, CopyTable, CopySelectedRows }
		
		public DebugTableContextMenu(JTable table) {
			super("DebugTableContextMenu");
			this.table = table;
			
			add(createMenuItem("Show Widths",TableContextMenuActionCommand.ShowWidths,null));
			add(createMenuItem("Copy Selected Rows",TableContextMenuActionCommand.CopySelectedRows,SaveViewer.ToolbarIcons.Copy));
			add(createMenuItem("Copy Table",TableContextMenuActionCommand.CopyTable,SaveViewer.ToolbarIcons.Copy));
		}
	
		private JMenuItem createMenuItem(String label, TableContextMenuActionCommand actionCommand, SaveViewer.ToolbarIcons icon) {
			JMenuItem menuItem = new JMenuItem(label);
			menuItem.addActionListener(this);
			menuItem.setActionCommand(actionCommand.toString());
			if (icon!=null) menuItem.setIcon(SaveViewer.toolbarIS.getIcon(icon));
			return menuItem;
		}
	
		@Override
		public void actionPerformed(ActionEvent e) {
			TableContextMenuActionCommand actionCommand = TableContextMenuActionCommand.valueOf(e.getActionCommand());
			switch(actionCommand) {
			case CopyTable: {
				TableModel model = table.getModel();
				StringBuilder sb = new StringBuilder();
				for (int row=0; row<table.getRowCount(); ++row) {
					for (int col=0; col<table.getColumnCount(); ++col) {
						if (col>0) sb.append("\t");
						int rowM = table.convertRowIndexToModel(row);
						int colM = table.convertColumnIndexToModel(col);
						Object value = model.getValueAt(rowM,colM);
						sb.append(value==null?"":value);
					}
					sb.append("\r\n");
				}
				SaveViewer.copyToClipBoard(sb.toString());
			} break;
			case CopySelectedRows: {
				TableModel model = table.getModel();
				StringBuilder sb = new StringBuilder();
				for (int row:table.getSelectedRows()) {
					for (int col=0; col<model.getColumnCount(); ++col) {
						if (col>0) sb.append("\t");
						int rowM = table.convertRowIndexToModel(row);
						int colM = table.convertColumnIndexToModel(col);
						sb.append(model.getValueAt(rowM,colM));
					}
					sb.append("\r\n");
				}
				SaveViewer.copyToClipBoard(sb.toString());
			} break;
			case ShowWidths: {
				TableColumnModel columnModel = table.getColumnModel();
				int[] widths = new int[columnModel.getColumnCount()];
				for (int i=0; i<columnModel.getColumnCount(); ++i)
					widths[i] = columnModel.getColumn(i).getWidth();
				SaveViewer.log_ln(Arrays.toString(widths));
			} break;
			}
		}
	
	}

	public static class SimplifiedTable extends JTable {
		private static final long serialVersionUID = 6963749333892762675L;
		private boolean useRowSorter;
		private String name;
		private DebugTableContextMenu contextMenu;
		private TableCellRenderer overallCellRenderer;
		
		SimplifiedTable(String name, boolean disableAutoResize, boolean installDebugContextMenu, boolean useRowSorter) {
			super();
			this.name = name;
			this.useRowSorter = useRowSorter;
			this.overallCellRenderer = null;
			if (disableAutoResize)
				setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			if (installDebugContextMenu) {
				contextMenu = new DebugTableContextMenu(this);
				new Gui.ContextMenuInvoker(this, contextMenu);
			} else contextMenu=null;
			//setAutoCreateRowSorter(useRowSorter);
		}
		
		public DebugTableContextMenu getDebugTableContextMenu() {
			return contextMenu;
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
			if (overallCellRenderer!=null)
				setCellRendererForAllColumns(overallCellRenderer, false);
		}

		public void setCellRendererForAllColumns(TableCellRenderer renderer, boolean resetAfterModelChange) {
			if (resetAfterModelChange)
				this.overallCellRenderer = renderer;
			TableColumnModel tableColumnModel = getColumnModel();
			for (int i=0; i<tableColumnModel.getColumnCount(); ++i)
				tableColumnModel.getColumn(i).setCellRenderer(renderer);
		}

		public void stopCellEditing() {
			TableCellEditor tableCellEditor = getCellEditor();
			if (tableCellEditor!=null) tableCellEditor.stopCellEditing();
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
				
				if      (model.getColumnClass(column) == Boolean           .class) comparator = addComparator(comparator,sortOrder,(Integer row)->(Boolean           )model.getValueAt(row,column));
				else if (model.getColumnClass(column) == String            .class) comparator = addComparator(comparator,sortOrder,(Integer row)->(String            )model.getValueAt(row,column));
				else if (model.getColumnClass(column) == Long              .class) comparator = addComparator(comparator,sortOrder,(Integer row)->(Long              )model.getValueAt(row,column));
				else if (model.getColumnClass(column) == Integer           .class) comparator = addComparator(comparator,sortOrder,(Integer row)->(Integer           )model.getValueAt(row,column));
				else if (model.getColumnClass(column) == Double            .class) comparator = addComparator(comparator,sortOrder,(Integer row)->(Double            )model.getValueAt(row,column));
				else if (model.getColumnClass(column) == Float             .class) comparator = addComparator(comparator,sortOrder,(Integer row)->(Float             )model.getValueAt(row,column));
				else if (model.getColumnClass(column) == TimeStamp         .class) comparator = addComparator(comparator,sortOrder,(Integer row)->(TimeStamp         )model.getValueAt(row,column));
				else if (model.getColumnClass(column) == GeneralizedID.Type.class) comparator = addComparator(comparator,sortOrder,(Integer row)->(GeneralizedID.Type)model.getValueAt(row,column));
				else comparator = addComparator(comparator,sortOrder,
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
		
		private <U extends Comparable<? super U>> Comparator<Integer> addComparator(Comparator<Integer> comp, SortOrder sortOrder, Function<? super Integer,? extends U> keyExtractor) {
			if (sortOrder==SortOrder.DESCENDING) {
				if (comp==null) comp = Comparator     .comparing    (keyExtractor,Comparator.nullsFirst(Comparator.naturalOrder()));
				else            comp = comp.reversed().thenComparing(keyExtractor,Comparator.nullsFirst(Comparator.naturalOrder()));
				return comp.reversed();
			} else {
				if (comp==null) comp = Comparator     .comparing    (keyExtractor,Comparator.nullsLast(Comparator.naturalOrder()));
				else            comp = comp           .thenComparing(keyExtractor,Comparator.nullsLast(Comparator.naturalOrder()));
				return comp;
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
		protected void fireTableColumnUpdate(int columnIndex) {
			if (getRowCount()>0)
				fireTableModelEvent(new TableModelEvent(this, 0, getRowCount()-1, columnIndex, TableModelEvent.UPDATE));
		}
		protected void fireTableCellUpdate(int rowIndex, int columnIndex) {
			fireTableModelEvent(new TableModelEvent(this, rowIndex, rowIndex, columnIndex, TableModelEvent.UPDATE));
		}
		protected void fireTableRowAdded(int rowIndex) {
			fireTableModelEvent(new TableModelEvent(this, rowIndex, rowIndex, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
		}
		protected void fireTableRowRemoved(int rowIndex) {
			fireTableModelEvent(new TableModelEvent(this, rowIndex, rowIndex, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE));
		}
		protected void fireTableRowsRemoved(int firstRowIndex, int lastRowIndex) {
			fireTableModelEvent(new TableModelEvent(this, firstRowIndex, lastRowIndex, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE));
		}
		protected void fireTableUpdate() {
			fireTableModelEvent(new TableModelEvent(this));
		}
		protected void fireTableStructureUpdate() {
			fireTableModelEvent(new TableModelEvent(this,TableModelEvent.HEADER_ROW));
		}
		
		public void initiateColumnUpdate(ColumnID columnID) {
			int columnIndex = getColumn( columnID );
			if (columnIndex>=0) fireTableColumnUpdate(columnIndex);
		}

		@Override public abstract int getRowCount();
		public abstract Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID);
		
		public int getUnsortedRowsCount() { return 0; }
		
		protected ColumnID getColumnID(int columnIndex) {
			if (columnIndex<0) return null;
			if (columnIndex<columns.length) return columns[columnIndex];
			return null;
		}
		public int getColumn( ColumnID columnID ) {
			for (int i=0; i<columns.length; ++i)
				if (columns[i]==columnID)
					return i;
			return -1;
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
	
	public static class NonStringRenderer<T> implements ListCellRenderer<T>, TableCellRenderer {
		
		private RendererComponent comp;
		private Function<Object, String> converter;
		
		public NonStringRenderer(Function<Object,String> converter) {
			this.converter = converter;
			this.comp = new RendererComponent();
			comp.setPreferredSize(new Dimension(1,16));
		}
		
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Color bgColor   = isSelected ? table.getSelectionBackground() : table.getBackground();
			Color textColor = isSelected ? table.getSelectionForeground() : table.getForeground();
			comp.set(converter.apply(value),bgColor,textColor);
			return comp;
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends T> list, T value, int index, boolean isSelected, boolean cellHasFocus) {
			Color bgColor   = isSelected ? list.getSelectionBackground() : null; //list.getBackground();
			Color textColor = isSelected ? list.getSelectionForeground() : list.getForeground();
			comp.set(converter.apply(value),bgColor,textColor);
			return comp;
		}

		public static class RendererComponent extends LabelRendererComponent {
			private static final long serialVersionUID = 1870151775725517505L;

			private RendererComponent() {
				setOpaque(true);
			}

			public void set(String value, Color bgColor, Color textColor) {
				setOpaque(bgColor!=null);
				setBackground(bgColor);
				setForeground(textColor);
				setText(value==null?"":value);
			}
		}
	}
	
	public static class UpgradeCategoryRenderer extends IconTextRenderer<Images.UpgradeCategory,Images.UpgradeCategory> {
		
		public UpgradeCategoryRenderer() { super(50,16); }
		
		@Override protected UpgradeCategory cast(Object obj) {
			if (obj instanceof Images.UpgradeCategory) return (Images.UpgradeCategory)obj;
			return null;
		}

		@Override protected Icon createIcon(UpgradeCategory value) {
			return new ImageIcon(Images.UpgradeCategoryImages.createImage(value, 16,16, Color.BLACK));
		}

		@Override protected UpgradeCategory getIconKey(UpgradeCategory value) { return value; }
		@Override protected String          getLabel  (UpgradeCategory value) { return value.getLabel(); }
	}
	
	public static class NamedColorRenderer extends IconTextRenderer<NamedColor,Integer> {
		
		public NamedColorRenderer() { super(50,16); }
		
		@Override protected NamedColor cast(Object obj) {
			if (obj instanceof NamedColor) return (NamedColor)obj;
			return null;
		}

		@Override protected Icon createIcon(NamedColor value) {
			return new ImageIcon(NamedColor.createImage(value,20,13));
		}

		@Override protected Integer getIconKey(NamedColor value) { return value.value; }
		@Override protected String  getLabel  (NamedColor value) { return String.format("[%06X] %s", value.value, value.name);  }
	}
	
	public static abstract class IconTextRenderer<ValueType,IconKey> implements ListCellRenderer<ValueType>, TableCellRenderer {
		
		private RendererComponent comp;
		
		public IconTextRenderer(int prefWidth, int prefHeight) {
			comp = new RendererComponent();
			comp.setPreferredSize(new Dimension(prefWidth,prefHeight));
		}
		
		protected abstract ValueType cast(Object obj);
		protected abstract Icon    createIcon(ValueType value);
		protected abstract IconKey getIconKey(ValueType value);
		protected abstract String  getLabel  (ValueType value);
		
		@Override
		public Component getTableCellRendererComponent(JTable table, Object obj, boolean isSelected, boolean hasFocus, int row, int column) {
			ValueType value = cast(obj);
			if (isSelected) comp.set(value,table.getSelectionBackground(),table.getSelectionForeground());
			else            comp.set(value,table.getBackground(),table.getForeground());
			return comp;
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends ValueType> list, ValueType value, int index, boolean isSelected, boolean cellHasFocus) {
			if (isSelected) comp.set(value,list.getSelectionBackground(),list.getSelectionForeground());
			else            comp.set(value,list.getBackground(),list.getForeground());
			return comp;
		}

		public class RendererComponent extends LabelRendererComponent {
			private static final long serialVersionUID = -6729772313931767140L;
			
			private HashMap<IconKey,Icon> iconCache;
			
			private RendererComponent() {
				iconCache = new HashMap<>();
				setOpaque(true);
			}
			
			public void set(ValueType value, Color bgColor, Color textColor) {
				setBackground(bgColor);
				setForeground(textColor);
				if (value==null) {
					setIcon(null);
					setText("");
				} else {
					Icon icon = iconCache.get(getIconKey(value));
					if (icon==null) iconCache.put(getIconKey(value),icon = createIcon(value));
					setIcon(icon);
					setText(getLabel(value));
				}
			}
		}
	}

	private static class LabelRendererComponent extends JLabel {
		private static final long serialVersionUID = -695854080940136137L;
		
		@Override public void revalidate() {}
		@Override public void invalidate() {}
		@Override public void validate() {}
		@Override public void repaint(long tm, int x, int y, int width, int height) {}
		@Override public void repaint(Rectangle r) {}
		@Override public void repaint() {}
		@Override public void repaint(long tm) {}
		@Override public void repaint(int x, int y, int width, int height) {}

		@Override public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}
		@Override public void firePropertyChange(String propertyName, int oldValue, int newValue) {}
		@Override public void firePropertyChange(String propertyName, char oldValue, char newValue) {}
		@Override protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}
		@Override public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {}
		@Override public void firePropertyChange(String propertyName, short oldValue, short newValue) {}
		@Override public void firePropertyChange(String propertyName, long oldValue, long newValue) {}
		@Override public void firePropertyChange(String propertyName, float oldValue, float newValue) {}
		@Override public void firePropertyChange(String propertyName, double oldValue, double newValue) {}
	}
	
	public static class SimpleColorRenderer implements ListCellRenderer<Integer>, TableCellRenderer {
		
		private RendererComponent comp;
		
		public SimpleColorRenderer() {
			comp = new RendererComponent();
			comp.setPreferredSize(new Dimension(50,16));
		}
		
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Integer intValue = null;
			if (value instanceof Integer) intValue = (Integer)value;
			comp.set(intValue,isSelected?table.getSelectionBackground():table.getBackground());
			return comp;
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends Integer> list, Integer value, int index, boolean isSelected, boolean cellHasFocus) {
			comp.set(value,isSelected?list.getSelectionBackground():list.getBackground());
			return comp;
		}

		public static class RendererComponent extends Canvas {
			private static final long serialVersionUID = -3943051255994570512L;
			
			private Color valueColor;
			private Color bgColor;
			
			public void set(Integer value, Color bgColor) {
				this.bgColor = bgColor;
				if (value==null) valueColor = null;
				else             valueColor = new Color(value);
			}

			@Override
			protected void paintCanvas(Graphics g, int width, int height) {
				if (valueColor==null) {
					g.setColor(bgColor);
					g.fillRect(0, 0, width, height);
				} else {
					g.setColor(bgColor);
					g.drawRect(0, 0, width-1, height-1);
					//g.drawRect(1, 1, width-3, height-3);
					g.setColor(valueColor);
					g.fillRect(1, 1, width-2, height-2);
					//g.fillRect(2, 2, width-4, height-4);
				}
			}
			
			@Override public void revalidate() {}
			@Override public void invalidate() {}
			@Override public void validate() {}
			@Override public void repaint(long tm, int x, int y, int width, int height) {}
			@Override public void repaint(Rectangle r) {}
			@Override public void repaint() {}
			@Override public void repaint(long tm) {}
			@Override public void repaint(int x, int y, int width, int height) {}
	
			@Override public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}
			@Override public void firePropertyChange(String propertyName, int oldValue, int newValue) {}
			@Override public void firePropertyChange(String propertyName, char oldValue, char newValue) {}
			@Override protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}
			@Override public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {}
			@Override public void firePropertyChange(String propertyName, short oldValue, short newValue) {}
			@Override public void firePropertyChange(String propertyName, long oldValue, long newValue) {}
			@Override public void firePropertyChange(String propertyName, float oldValue, float newValue) {}
			@Override public void firePropertyChange(String propertyName, double oldValue, double newValue) {}
		}
		
	}
	
	public static class ComboboxCellEditor<T> extends AbstractCellEditor implements TableCellEditor {
		private static final long serialVersionUID = -346693108757882917L;
		
		private Object currentValue;
		private T[] values;
		private ListCellRenderer<? super T> renderer;
		
		public ComboboxCellEditor(T[] values) {
			this.values = values;
			this.currentValue = null;
			this.renderer = null;
		}
		
		public void addValue(T newValue) {
			stopCellEditing();
			values = Arrays.copyOf(values, values.length+1);
			values[values.length-1] = newValue;
		}

		public void setValues(T[] newValues) {
			stopCellEditing();
			values = newValues;
		}

		public void setRenderer(ListCellRenderer<? super T> renderer) {
			this.renderer = renderer;
		}
		
		@Override
		public Object getCellEditorValue() {
			return currentValue;
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			this.currentValue = value;
			
			JComboBox<T> cmbbx = new JComboBox<T>(values);
			if (renderer!=null) cmbbx.setRenderer(renderer);
			cmbbx.setSelectedItem(currentValue);
			cmbbx.setBackground(isSelected?table.getSelectionBackground():table.getBackground());
			cmbbx.addActionListener(e->{
				currentValue = cmbbx.getSelectedItem();
				fireEditingStopped();
			});
			
			return cmbbx;
		}
		
	}

}
