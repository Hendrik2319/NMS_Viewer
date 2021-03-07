package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.SortOrder;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.LabelRendererComponent;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.gui.Tables.SimplifiedRowSorter.RowSorterListener;
import net.schwarzbaer.gui.Tables.SimplifiedTableModel;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Gui;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Images;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Images.NamedColor;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Images.UpgradeCategory;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.BuildingObject;
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
			add(createMenuItem("Copy Selected Rows",TableContextMenuActionCommand.CopySelectedRows,Gui.ToolbarIcons.Copy));
			add(createMenuItem("Copy Table",TableContextMenuActionCommand.CopyTable,Gui.ToolbarIcons.Copy));
		}
	
		private JMenuItem createMenuItem(String label, TableContextMenuActionCommand actionCommand, Gui.ToolbarIcons icon) {
			return Gui.createMenuItem(label, this, actionCommand, icon);
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
				for (int i=0; i<columnModel.getColumnCount(); ++i) {
					TableColumn column = columnModel.getColumn(i);
					widths[column.getModelIndex()] = column.getWidth();
				}
				Gui.log_ln(Arrays.toString(widths));
			} break;
			}
		}
	
	}
	
	public static class SimplifiedTable<ColumnID extends Tables.SimplifiedColumnIDInterface> extends JTable implements Gui.ContextMenuInvoker.ContextMenuInvokeListener {
		private static final long serialVersionUID = 6963749333892762675L;
		private boolean useRowSorter;
		@SuppressWarnings("unused")
		private String name;
		private JPopupMenu contextMenu;
		private TableCellRenderer overallCellRenderer;
		private NewRowSorter rowSorter;
		private RowSorterListener rowSorterListener;
		private int[] selectedRowsM;
		private Vector<ContextMenuInvokeListener> cmiListeners;
		private SimplifiedTableModel<ColumnID> simplifiedTableModel;
		
		public SimplifiedTable(String name, boolean disableAutoResize, boolean installDebugContextMenu, boolean useRowSorter) {
			super();
			this.name = name;
			this.useRowSorter = useRowSorter;
			this.rowSorter = null;
			this.selectedRowsM = null;
			this.rowSorterListener = null;
			this.overallCellRenderer = null;
			this.simplifiedTableModel = null;
			
			if (disableAutoResize)
				setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			
			if (installDebugContextMenu) contextMenu = new DebugTableContextMenu(this);
			else contextMenu = new JPopupMenu();
			Gui.ContextMenuInvoker menuInvoker = new Gui.ContextMenuInvoker(this, contextMenu);
			menuInvoker.addContextMenuInvokeListener(this);
			
			//setAutoCreateRowSorter(useRowSorter);
			cmiListeners = new Vector<>();
		}
		
		public SimplifiedTable(String name, SimplifiedTableModel<ColumnID> dataModel, boolean disableAutoResize, boolean installDebugContextMenu, boolean useRowSorter) {
			this(name, disableAutoResize, installDebugContextMenu, useRowSorter);
			dataModel.setTable(this);
			setModel(dataModel);
		}
		
		public TableColumn getColumn(ColumnID columnID) {
			TableColumnModel columnModel = getColumnModel();
			if (columnModel==null) return null;
			
			if (simplifiedTableModel==null) return null;
			int columnIndex = simplifiedTableModel.getColumn(columnID);
			if (columnIndex<0) return null;
			
			return columnModel.getColumn(convertColumnIndexToView(columnIndex));
		}
		
		public void setCellRenderer(ColumnID columnID, TableCellRenderer cellRenderer) {
			TableColumn column = getColumn(columnID);
			if (column==null) return;
			column.setCellRenderer(cellRenderer);
		}
	
		public void setCellEditor(ColumnID columnID, TableCellEditor cellEditor) {
			TableColumn column = getColumn(columnID);
			if (column==null) return;
			column.setCellEditor(cellEditor);
		}
		
		public JPopupMenu getContextMenu() {
			return contextMenu;
		}

		@Override public void contextMenuWillBeInvoked(int x, int y) {
			Point point = new Point(x,y);
			int row = rowAtPoint(point);
			int column = columnAtPoint(point);
			for (ContextMenuInvokeListener cmiListener:cmiListeners)
				cmiListener.contextMenuWillBeInvoked(row, column);
		}
		
		public interface ContextMenuInvokeListener {
			public void contextMenuWillBeInvoked(int rowV, int columnV);
		}
		
		public void    addContextMenuInvokeListener( ContextMenuInvokeListener listener ) { cmiListeners.   add(listener); } 
		public void removeContextMenuInvokeListener( ContextMenuInvokeListener listener ) { cmiListeners.remove(listener); } 

		public void setSelectionMode(int selectionMode, boolean keepSelectionWhileRowSort) {
			setSelectionMode(selectionMode);
			if (keepSelectionWhileRowSort) {
				getSelectionModel().addListSelectionListener(e->{
					int[] rowsV = getSelectedRows();
					selectedRowsM = new int[rowsV.length];
					for (int i = 0; i < rowsV.length; i++)
						selectedRowsM[i] = convertRowIndexToModel(rowsV[i]);
				});
				setRowSorterListener(()->{
					if (selectedRowsM!=null && selectedRowsM.length>0) {
						Gui.log_ln("Selected Rows (Model): %s", Arrays.toString(selectedRowsM));
						
						int[] rowsV = new int[selectedRowsM.length];
						for (int i = 0; i < rowsV.length; i++)
							rowsV[i] = convertRowIndexToView(selectedRowsM[i]);
						Gui.log_ln("Selected Rows (View) : %s", Arrays.toString(rowsV));
						Arrays.sort(rowsV);
						Gui.log_ln("Selected Rows (View) : %s (sorted)", Arrays.toString(rowsV));
						
						clearSelection();
						int firstRow, lastRow;
						for (int i=0; i<rowsV.length; i++) {
							firstRow = lastRow = rowsV[i];
							boolean endReached = true;
							for (int j=i+1; j<rowsV.length; j++) {
								if (lastRow+1 == rowsV[j]) {
									lastRow = rowsV[j];
								} else {
									i = j-1; // --> lastRow == rowsV[i]
									addRowSelectionInterval(firstRow, rowsV[i]);
									Gui.log_ln("addRowSelectionInterval: %d..%d", firstRow, rowsV[i]);
									endReached = false;
									break;
								}
							}
							if (endReached) {
								addRowSelectionInterval(firstRow, rowsV[rowsV.length-1]);
								Gui.log_ln("addRowSelectionInterval: %d..%d (e)", firstRow, rowsV[rowsV.length-1]);
								break;
							}
						}
					}
				});
			}
		}

		private void setRowSorterListener(RowSorterListener rowSorterListener) {
			this.rowSorterListener = rowSorterListener;
			if (rowSorter!=null && rowSorterListener!=null) rowSorter.addListener(rowSorterListener);
		}
		
		public SimplifiedTableModel<?> getModel_SimplifiedTableModel() {
			return simplifiedTableModel;
		}

		@Override
		public void setModel(TableModel dataModel) {
			this.simplifiedTableModel = null;
			super.setModel(dataModel);
		}

		public void setModel(SimplifiedTableModel<ColumnID> dataModel) {
			super.setModel(this.simplifiedTableModel = dataModel);
			dataModel.setColumnWidths(this);
			if (useRowSorter) {
				setRowSorter(rowSorter = new NewRowSorter(dataModel));
				if (rowSorterListener!=null) rowSorter.addListener(rowSorterListener);
			}
			if (overallCellRenderer!=null)
				setCellRendererForAllColumns(overallCellRenderer, true);
		}

		public void setCellRendererForAllColumns(TableCellRenderer renderer, boolean resetAfterModelChange) {
			this.overallCellRenderer = resetAfterModelChange?renderer:null;
			TableColumnModel tableColumnModel = getColumnModel();
			for (int i=0; i<tableColumnModel.getColumnCount(); ++i)
				tableColumnModel.getColumn(i).setCellRenderer(renderer);
		}

		public void setCellRendererByIndex(TableCellRenderer renderer, Predicate<Integer> forThisColumn) {
			this.overallCellRenderer = null;
			TableColumnModel tableColumnModel = getColumnModel();
			for (int i=0; i<tableColumnModel.getColumnCount(); ++i)
				if (forThisColumn.test(convertColumnIndexToModel(i)))
					tableColumnModel.getColumn(i).setCellRenderer(renderer);
		}

		public void setCellRendererByColumnID(TableCellRenderer renderer, Predicate<ColumnID> forThisColumn) {
			if (simplifiedTableModel==null) return;
			this.overallCellRenderer = null;
			TableColumnModel tableColumnModel = getColumnModel();
			for (int i=0; i<tableColumnModel.getColumnCount(); ++i) {
				ColumnID columnID = simplifiedTableModel.getColumnID(convertColumnIndexToModel(i));
				if (columnID!=null && forThisColumn.test(columnID))
					tableColumnModel.getColumn(i).setCellRenderer(renderer);
			}
		}

		public void stopCellEditing() {
			TableCellEditor tableCellEditor = getCellEditor();
			if (tableCellEditor!=null) tableCellEditor.stopCellEditing();
		}
	}
	
	static class NewRowSorter extends Tables.SimplifiedRowSorter {

		public NewRowSorter(SimplifiedTableModel<?> model) {
			super(model);
		}

		@Override
		protected boolean isNewClass(Class<?> columnClass) {
			return
				(columnClass == TimeStamp                        .class) ||
				(columnClass == GeneralizedID.Type               .class) ||
				(columnClass == Images.NamedColor                .class) ||
				(columnClass == Images.ColorListDialog.ColorUsage.class) ||
				(columnClass == BuildingObject.BaseObjAppearance .class);
		}

		@Override
		protected Comparator<Integer> addComparatorForNewClass(Comparator<Integer> comparator, SortOrder sortOrder, int column) {
			if      (model.getColumnClass(column) == TimeStamp                        .class) comparator = addComparator(comparator,sortOrder,(Integer row)->(TimeStamp                        )model.getValueAt(row,column));
			else if (model.getColumnClass(column) == GeneralizedID.Type               .class) comparator = addComparator(comparator,sortOrder,(Integer row)->(GeneralizedID.Type               )model.getValueAt(row,column));
			else if (model.getColumnClass(column) == Images.NamedColor                .class) comparator = addComparator(comparator,sortOrder,(Integer row)->(Images.NamedColor                )model.getValueAt(row,column));
			else if (model.getColumnClass(column) == Images.ColorListDialog.ColorUsage.class) comparator = addComparator(comparator,sortOrder,(Integer row)->(Images.ColorListDialog.ColorUsage)model.getValueAt(row,column));
			else if (model.getColumnClass(column) == BuildingObject.BaseObjAppearance .class) comparator = addComparator(comparator,sortOrder,(Integer row)->(BuildingObject.BaseObjAppearance )model.getValueAt(row,column));
			return comparator;
		}
	}
	
	public static class VerySimpleTable<DataType> extends SimplifiedTable<VerySimpleTable.ColumnID<DataType>> {
		private static final long serialVersionUID = -5521097765178758216L;
		private VerySimpleTableModel<DataType> tableModel;

		public VerySimpleTable(String name, boolean disableAutoResize, boolean installDebugContextMenu, boolean useRowSorter) {
			super(name, disableAutoResize, installDebugContextMenu, useRowSorter);
			tableModel = null;
		}
		public VerySimpleTable(String name, boolean disableAutoResize, boolean installDebugContextMenu, boolean useRowSorter, DataType[] data, ColumnID<DataType>[] columns) {
			this(name, disableAutoResize, installDebugContextMenu, useRowSorter);
			setData(data, columns);
		}
		public VerySimpleTable(String name, boolean disableAutoResize, boolean installDebugContextMenu, boolean useRowSorter, Vector<DataType> data, ColumnID<DataType>[] columns) {
			this(name, disableAutoResize, installDebugContextMenu, useRowSorter);
			setData(data, columns);
		}
		public VerySimpleTable(String name) {
			this(name, true, true, true);
		}
		public VerySimpleTable(String name, DataType[] data, ColumnID<DataType>[] columns) {
			this(name, true, true, true, data, columns);
		}
		public VerySimpleTable(String name, Vector<DataType> data, ColumnID<DataType>[] columns) {
			this(name, true, true, true, data, columns);
		}
		public void setData(DataType[] data, ColumnID<DataType>[] columns) {
			setModel(tableModel = new VerySimpleTableModel<DataType>(data, columns));
		}
		public void setData(Vector<DataType> data, ColumnID<DataType>[] columns) {
			setModel(tableModel = new VerySimpleTableModel<DataType>(data, columns));
		}
		public VerySimpleTableModel<DataType> getModel_VerySimpleTableModel() {
			return tableModel;
		}
		
		public VerySimpleTable<DataType> computePreferredScrollableViewportSize(int maxWidth, int maxHeight) {
			//SimplifiedTableModel<?> model = this.getModel_SimplifiedTableModel();
			//int width = model==null ? 200 : model.getSumOfPrefColumnWidths()+30;
			//setPreferredScrollableViewportSize(new Dimension(width, height));
			Dimension size = new Dimension(getPreferredSize());
			if (maxWidth >0 && size.width >maxWidth ) size.width  = maxWidth ;
			if (maxHeight>0 && size.height>maxHeight) size.height = maxHeight;
			setPreferredScrollableViewportSize(size);
			return this;
		}
		
		public ColumnID<DataType> getColumn(int columnIndexM) {
			if (tableModel == null) return null;
			return tableModel.columns[columnIndexM];
		}
		
		public void setSelectedValue(DataType selected, boolean shouldScroll) {
			int rowM = tableModel.indexOf(selected);
			int rowV = rowM<0 ? -1 : convertRowIndexToView(rowM);
			if (rowV<0) clearSelection();
			else setRowSelectionInterval(rowV,rowV);
			if (shouldScroll)
				scrollRectToVisible(getCellRect(rowV, 0, true));
		}
		public DataType getValueAt(int rowIndexM) {
			if (rowIndexM<0 || rowIndexM>=getRowCount()) return null;
			return tableModel.getValue(rowIndexM);
		}
		
		public Vector<DataType> getValuesAt(int[] rowIndexesM) {
			Vector<DataType> vec = new Vector<>();
			for (int rowIndexM:rowIndexesM)
				vec.add(getValueAt(rowIndexM));
			return vec;
		}
		public void addSelectionListener(BiConsumer<DataType,Integer> selectionChanged) {
			getSelectionModel().addListSelectionListener(e->{
				if (tableModel==null) return;
				
				int rowIndexV = getSelectedRow();
				if (rowIndexV<0) { selectionChanged.accept(null,null); return; }
				
				int rowIndexM = convertRowIndexToModel(rowIndexV);
				selectionChanged.accept(tableModel.getValue(rowIndexM),rowIndexM);
			});
			
		}
		
		public static class VerySimpleTableModel<DataType> extends Tables.SimplifiedTableModel<ColumnID<DataType>> {

			private final ColumnID<DataType>[] columns;
			private final Vector<DataType> dataVector;
			private final DataType[] dataArray;

			public VerySimpleTableModel(DataType[] data, ColumnID<DataType>[] columns) {
				this(columns,null,data);
			}
			public VerySimpleTableModel(Vector<DataType> data, ColumnID<DataType>[] columns) {
				this(columns,data,null);
			}
			private VerySimpleTableModel(ColumnID<DataType>[] columns, Vector<DataType> dataVector, DataType[] dataArray) {
				super(columns);
				this.columns = columns;
				this.dataVector = dataVector;
				this.dataArray = dataArray;
			}
			
			@Override public int getRowCount() {
				return dataVector!=null ? dataVector.size() : dataArray!=null ? dataArray.length : 0;
			}
			private int indexOf(DataType value) {
				if (dataVector!=null) return dataVector.indexOf(value);
				if (dataArray !=null)
					for (int i=0; i<dataArray.length; i++)
						if (dataArray[i].equals(value))
							return i;
				return -1;
			}

			private DataType getValue(int rowIndex) {
				return dataVector!=null ? dataVector.get(rowIndex) : dataArray!=null ? dataArray[rowIndex] : null;
			}
			@Override public Object getValueAt(int rowIndex, int columnIndex, ColumnID<DataType> columnID) {
				if (rowIndex<0 || rowIndex>=getRowCount()) return null;
				DataType value = getValue(rowIndex);
				return value==null || columnID.getValue==null ? null : columnID.getValue.apply(value,rowIndex);
			}
			@Override protected boolean isCellEditable(int rowIndex, int columnIndex, ColumnID<DataType> columnID) {
				return columnID.setValue!=null;
			}
			@Override protected void setValueAt(Object newValue, int rowIndex, int columnIndex, ColumnID<DataType> columnID) {
				if (rowIndex<0 || rowIndex>=getRowCount()) return;
				DataType value = getValue(rowIndex);
				if (value!=null && columnID.setValue!=null) 
					columnID.setValue.setValue(value, rowIndex, newValue);
			}
			public void fireTableColumnUpdate(ColumnID<DataType> columnID) {
				super.fireTableColumnUpdate(getColumn(columnID));
			}
			@Override public void fireTableRowUpdate(int rowIndex) {
				super.fireTableRowUpdate(rowIndex);
			}
			
		}
		
		public static class ColumnID<DataType> extends Tables.SimplifiedColumnConfig implements Tables.SimplifiedColumnIDInterface {
			
			private final BiFunction<DataType, Integer, Object> getValue;
			private final SetValue<DataType> setValue;

			public <OtherDataType> ColumnID(ColumnID<OtherDataType> other, Function<DataType,OtherDataType> get) {
				super(other);
				getValue = other.getValue==null ? null : (d,i) -> {
					OtherDataType o = d==null ? null : get.apply(d);
					return o==null ? null : other.getValue.apply(o,i);
				};
				setValue = other.setValue==null ? null : (d,i,v)->{
					OtherDataType o = d==null ? null : get.apply(d);
					if (o!=null) other.setValue.setValue(o,i,v);
				};
			}
			public ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, Function<DataType,Object> getValue) {
				this(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue==null ? null : (d,i)->getValue.apply(d));
			}
			public ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, Function<DataType,Object> getValue, BiConsumer<DataType, Object> setValue) {
				this(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue==null ? null : (d,i)->getValue.apply(d), setValue);
			}
			public ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, Function<DataType,Object> getValue, SetValue<DataType> setValue) {
				this(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue==null ? null : (d,i)->getValue.apply(d), setValue);
			}
			public ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, BiFunction<DataType, Integer, Object> getValue) {
				this(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue, (SetValue<DataType>)null);
			}
			public ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, BiFunction<DataType, Integer, Object> getValue, BiConsumer<DataType, Object> setValue) {
				this(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth, getValue, setValue==null ? null :  (row,i,v)->setValue.accept(row, v));
			}
			public ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth, BiFunction<DataType, Integer, Object> getValue, SetValue<DataType> setValue) {
				super(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
				this.getValue = getValue;
				this.setValue = setValue;
			}
			
			public interface SetValue<DataType> {
				void setValue(DataType row, int index, Object newValue);
			}

			@Override public SimplifiedColumnConfig getColumnConfig() {
				return this;
			}
		}
	}
	
	public static class UpgradeCategoryRenderer extends IconTextRenderer<UpgradeCategory,UpgradeCategory> {
		
		public UpgradeCategoryRenderer() { super(50,16); }
		
		@Override protected UpgradeCategory cast(Object obj) {
			if (obj instanceof UpgradeCategory) return (UpgradeCategory)obj;
			return null;
		}

		@Override protected Icon createIcon(UpgradeCategory value) {
			return new ImageIcon(Images.UpgradeCategoryImages.createImage(value, 16,16, Color.BLACK));
		}

		@Override protected UpgradeCategory getIconKey(UpgradeCategory value) { return value; }
		@Override protected String          getLabel  (UpgradeCategory value) { return value==null ? null : value.getLabel(); }
	}
	
	public static class NamedColorRenderer extends IconTextRenderer<NamedColor,Integer> {
		
		private final boolean withName;
		public NamedColorRenderer() { this(true); }
		public NamedColorRenderer(boolean withName) { super(50,16); this.withName = withName; }
		
		@Override protected NamedColor cast(Object obj) {
			if (obj instanceof NamedColor) return (NamedColor)obj;
			return null;
		}

		@Override protected Icon createIcon(NamedColor color) {
			return new ImageIcon(Images.createImage(20, 13, color.color));
		}

		@Override protected Integer getIconKey(NamedColor value) { return value.value; }
		@Override protected String  getLabel  (NamedColor value) { return value==null ? null : withName ? value.getLabel() : value.getValueStr(); }
	}
	
	public static abstract class IconTextRenderer<ValueType,IconKey> implements ListCellRenderer<ValueType>, TableCellRenderer {
		
		private HashMap<IconKey,Icon> iconCache;
		private LabelRendererComponent comp;
		private TableColorizerWrapper getCustomForeground;
		private TableColorizerWrapper getCustomBackground;
		
		public IconTextRenderer(int prefWidth, int prefHeight) {
			getCustomForeground = null;
			getCustomBackground = null;
			iconCache = new HashMap<>();
			comp = new LabelRendererComponent();
			if (prefWidth>0 && prefHeight>0)
				comp.setPreferredSize(new Dimension(prefWidth,prefHeight));
		}
		
		public void setTableColorizers(TableColorizer tableForegroundColorizer, TableColorizer tableBackgroundColorizer) {
			getCustomForeground = tableForegroundColorizer==null ? null : new TableColorizerWrapper(tableForegroundColorizer);
			getCustomBackground = tableBackgroundColorizer==null ? null : new TableColorizerWrapper(tableBackgroundColorizer);
		}
		
		protected abstract ValueType cast(Object obj);
		protected abstract Icon    createIcon(ValueType value);
		protected abstract IconKey getIconKey(ValueType value);
		protected abstract String  getLabel  (ValueType value);
		
		private Icon getIcon(ValueType value) {
			if (value==null) return null;
			
			IconKey iconKey = getIconKey(value);
			if (iconKey==null) return null;
			
			Icon icon = iconCache.get(iconKey);
			if (icon==null) iconCache.put(iconKey,icon = createIcon(value));
			return icon;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object obj, boolean isSelected, boolean hasFocus, int row, int column) {
			ValueType value = cast(obj);
			int rowM = table.convertRowIndexToModel(row);
			int columnM = table.convertColumnIndexToModel(column);
			if (getCustomForeground!=null) getCustomForeground.setCell(rowM, columnM);
			if (getCustomBackground!=null) getCustomBackground.setCell(rowM, columnM);
			comp.configureAsTableCellRendererComponent(table, getIcon(value), getLabel(value), isSelected, hasFocus, getCustomBackground, getCustomForeground);
			return comp;
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends ValueType> list, ValueType value, int index, boolean isSelected, boolean hasFocus) {
			comp.configureAsListCellRendererComponent(list, getIcon(value), getLabel(value), index, isSelected, hasFocus);
			return comp;
		}
		
		private static class TableColorizerWrapper implements Supplier<Color> {
			
			private final TableColorizer tableColorizer;
			private int rowM;
			private int columnM;

			TableColorizerWrapper(TableColorizer tableColorizer) {
				this.tableColorizer = tableColorizer;
			}
			void setCell(int rowM, int columnM) {
				this.rowM = rowM;
				this.columnM = columnM;
			}
			@Override public Color get() {
				return tableColorizer.getColor(rowM, columnM);
			}
		}
	}
	
	public interface TableColorizer {
		Color getColor(int rowM, int columnM);
	}
}
