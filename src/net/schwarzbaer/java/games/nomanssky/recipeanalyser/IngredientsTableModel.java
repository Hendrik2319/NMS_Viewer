package net.schwarzbaer.java.games.nomanssky.recipeanalyser;

import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Vector;
import java.util.function.Consumer;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import net.schwarzbaer.gui.Tables.CheckBoxRendererComponent;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.gui.Tables.SimplifiedColumnIDInterface;
import net.schwarzbaer.gui.Tables.SimplifiedTableModel;
import net.schwarzbaer.java.games.nomanssky.recipeanalyser.RecipeAnalyser.Lang;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID;

class IngredientsTableModel<IDType extends Comparable<IDType>> extends SimplifiedTableModel<IngredientsTableModel.ColumnID> {

	enum ColumnID implements SimplifiedColumnIDInterface {
		Index      ("#"          ,  String.class, 20,-1, 40, 40),
		Type       ("Type"       ,  String.class, 20,-1,100,100),
		InStock    ("In Stock"   , Boolean.class, 20,-1, 60, 60),
		Producible ("Producible" ,  String.class, 20,-1, 60, 60),
		GenID      ("ID"         ,  String.class, 20,-1,120,120),
		NameDE     ("Name (DE)"  ,  String.class, 20,-1,150,150),
		NameEN     ("Name (EN)"  ,  String.class, 20,-1,150,150),
		Price      ("Price"      ,  String.class, 20,-1,150,150),
		Description("Description",  String.class, 20,-1,450,450),
		;
		
		private SimplifiedColumnConfig columnConfig;
		
		ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
			columnConfig = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
		}
		@Override public SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
	}

	private final RecipeAnalyser gui;
	private final DataModel<IDType> dataModel;
	final Vector<DataModel<IDType>.Ingredient> ingredients;
	private final HashMap<IDType,DataModel<IDType>.Ingredient> ingredientsMap;
	
	DataModel<IDType>.Ingredient clickedIngredient;
	final HashSet<IDType> highlighted;
	boolean highlightProducible;

	protected IngredientsTableModel(RecipeAnalyser gui, DataModel<IDType> dataModel, Vector<DataModel<IDType>.Ingredient> ingredients) {
		super(ColumnID.values());
		this.gui = gui;
		this.dataModel = dataModel;
		this.ingredients = ingredients;
		this.ingredientsMap = new HashMap<>();
		for (DataModel<IDType>.Ingredient ingredient : this.ingredients)
			if (ingredient!=null)
				ingredientsMap.put(ingredient.getID(), ingredient);
		clickedIngredient = null;
		highlighted = new HashSet<>();
		highlightProducible = false;
	}

	public void setClickedIngredient(int rowM) {
		DataModel<IDType>.Ingredient ingredient = getIngredientAtRow(rowM);
		clickedIngredient = ingredient==null || ingredient.getName()==null ? null : ingredient;
	}

	public void forEach(Consumer<DataModel<IDType>.Ingredient> consumer) {
		for (DataModel<IDType>.Ingredient ingredient:ingredients)
			if (ingredient!=null && ingredient.getName()!=null)
				consumer.accept(ingredient);
	}

	public void forEachSelected(Consumer<DataModel<IDType>.Ingredient> consumer) {
		int[] selectedRows = gui.ingredientsTable.getSelectedRows();
		for (int i=0; i<selectedRows.length; i++) {
			DataModel<IDType>.Ingredient ingredient = ingredients.get(selectedRows[i]);
			if (ingredient!=null && ingredient.getName()!=null)
				consumer.accept(ingredient);
		}
	}

	public void forEachProducible(Consumer<DataModel<IDType>.Ingredient> consumer) {
		forEach(ingredient->{
			if (dataModel.isProducible(ingredient.getID()))
				consumer.accept(ingredient);
		});
	}

	public Vector<IDType> getSelected() {
		Vector<IDType> selected = new Vector<>();
		forEachSelected(ingredient->selected.add(ingredient.getID()));
		return selected;
	}

	@Override public ColumnID getColumnID(int columnIndex) {
		return super.getColumnID(columnIndex);
	}

	@Override public int getRowCount() {
		return ingredients.size();
	}

	public int findIngredientRowIndexByName(String name) {
		for (int i=0; i<ingredients.size(); i++) {
			DataModel<IDType>.Ingredient ingredient = ingredients.get(i);
			if (ingredient!=null && ingredient.nameEquals(name, true))
				return i;
		}
		return -1;
	}
	
	public DataModel<IDType>.Ingredient findIngredientByName(String name) {
		for (int i=0; i<ingredients.size(); i++) {
			DataModel<IDType>.Ingredient ingredient = ingredients.get(i);
			if (ingredient!=null && ingredient.nameEquals(name, true))
				return ingredient;
		}
		return null;
	}

	public DataModel<IDType>.Ingredient getIngredient(IDType id) {
		return ingredientsMap.get(id);
	}

	public String getIngredientName(IDType id) {
		DataModel<IDType>.Ingredient ingredient = getIngredient(id);
		
		if (ingredient!=null) {
			String str = ingredient.getName();
			if (str!=null) return str;
		}
		
		return String.format("<%s>", id);
	}

	public DataModel<IDType>.Ingredient getIngredientAtRow(int rowIndex) {
		if (0<=rowIndex && rowIndex<ingredients.size())
			return ingredients.get(rowIndex);
		return null;
	}

	@Override public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
		DataModel<IDType>.Ingredient ingredient = getIngredientAtRow(rowIndex);
		boolean isInput = ingredient!=null && ingredient.getName()!=null;
		switch (columnID) {
		case Index      : return !isInput ? null : ingredient.getID();
		case InStock    : return !isInput ? null : dataModel.isInStock(ingredient.getID()); // ? "In Stock" : "---";
		case Producible : return !isInput ? null : dataModel.isInStock(ingredient.getID()) ? "in stock" : dataModel.isProducible(ingredient.getID()) ? "producible" : "----";
		case Type       : return ingredient==null ? null : ingredient.getType();
		case NameDE     : return ingredient==null ? null : ingredient.getName(Lang.De);
		case NameEN     : return ingredient==null ? null : ingredient.getName(Lang.En);
		case Description: return ingredient==null ? null : ingredient.getDesc();
		case Price      : return ingredient==null || ingredient.getPrice()==null ? null : String.format(Locale.ENGLISH, "%,1.1f", ingredient.getPrice());
		case GenID      : {
			if (ingredient==null) return null; 
			GeneralizedID id = ingredient.getGeneralizedID();
			if (id==null) return null;
			return id.id;
		}
		}
		return null;
	}

	@Override protected void setValueAt(Object aValue, int rowIndex, int columnIndex, ColumnID columnID) {
		DataModel<IDType>.Ingredient ingredient = getIngredientAtRow(rowIndex);
		if (ingredient==null) return;
		if (ingredient.getName()==null) return;
		
		Boolean isInStock = null;
		int rawRowIndex = ingredient.getRawRowIndex();
		switch (columnID) {
		case InStock:
			if (aValue instanceof Boolean) isInStock = (Boolean) aValue;
			if (aValue instanceof String ) isInStock = "In Stock".equals((String) aValue);
			if (isInStock!=null) {
				IDType id = ingredient.getID();
				dataModel.setInStock(isInStock, id);
				dataModel.updateProducibility();
				fireTableUpdate(); 
			}
			break;
		case NameDE     : setValue(aValue, rowIndex, columnIndex, rawRowIndex, ingredient.getNameRawColumnIndex(Lang.De), str->ingredient.setName(str, Lang.De)); break;
		case NameEN     : setValue(aValue, rowIndex, columnIndex, rawRowIndex, ingredient.getNameRawColumnIndex(Lang.En), str->ingredient.setName(str, Lang.En)); break;
		case Description: setValue(aValue, rowIndex, columnIndex, rawRowIndex, ingredient.getDescRawColumnIndex(       ), str->ingredient.setDesc(str         )); break;
		default: break;
		}
	}

	private void setValue(Object aValue, int rowIndex, int columnIndex, int rawRowIndex, int rawColumnIndex, Consumer<String> setValue) {
		if (aValue instanceof String) {
			String str = (String) aValue;
			
			boolean success = dataModel.rawdataModel.setCell(str, rawRowIndex, rawColumnIndex);
			if (success) {
				setValue.accept(str);
				dataModel.wasEdited = true;
				dataModel.hasUnsavedChanges = true; 
				gui.updateGuiAccess();
				gui.updateWindowTitle();
			}
			
			SwingUtilities.invokeLater(()->{
				fireTableCellUpdate(rowIndex, columnIndex);
			});
		}
	}

	@Override protected boolean isCellEditable(int rowIndex, int columnIndex, ColumnID columnID) {
		DataModel<IDType>.Ingredient ingredient = getIngredientAtRow(rowIndex);
		if (ingredient == null) return false;
		if (ingredient.getName() == null) return false;
		switch (columnID) {
		case InStock: return true;
		case NameDE     : return ingredient.isNameEditable(Lang.De);
		case NameEN     : return ingredient.isNameEditable(Lang.En);
		case Description: return ingredient.isDescEditable();
		default: return false;
		}
	}

	TableCellRenderer createTableCellRenderer() {
		return new IngredientsTableRenderer();
	}

	private class IngredientsTableRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = -5822408016974497527L;
		
		private final CheckBoxRendererComponent checkBox;
		
		IngredientsTableRenderer() {
			checkBox = new CheckBoxRendererComponent();
		}
		
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			
			if (Boolean.class.isAssignableFrom(getColumnID(column).columnConfig.columnClass)) {
				component = checkBox;
				checkBox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
				checkBox.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
				checkBox.setSelected(value instanceof Boolean ? (Boolean) value : false);
				checkBox.setHorizontalAlignment(CENTER);
			}
			
			switch (getColumnID(column)) {
			case Index:
			case Type:
			case InStock:
			case Producible:
				setHorizontalAlignment(CENTER); break;
			case GenID:
			case NameDE:
			case NameEN:
			case Description:
				setHorizontalAlignment(LEFT); break;
			case Price:
				setHorizontalAlignment(RIGHT); break;
			}
			
			DataModel<IDType>.Ingredient ingredient = getIngredientAtRow(row);
			if (!isSelected) {
				Color background = table.getBackground();
				if (ingredient != null && ingredient.getName() != null) {
					if (highlighted.contains(ingredient.getID())) background = RecipeAnalyser.COLOR_INGREDIENT_MARKER;
					else if (highlightProducible && dataModel.isProducible(ingredient.getID())) background = RecipeAnalyser.COLOR_INGREDIENT_PRODUCIBLE;
					else if (ingredient.isOutputValue) background = RecipeAnalyser.COLOR_INGREDIENT_OUTPUT;
					else if (ingredient.isInputValue ) background = RecipeAnalyser.COLOR_INGREDIENT_INPUT;
				}
				component.setBackground(background);
			}
			
			return component;
		}
	
	}
}