package net.schwarzbaer.java.games.nomanssky.recipeanalyser;

import java.awt.Color;
import java.awt.Component;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.gui.Tables.SimplifiedColumnIDInterface;
import net.schwarzbaer.gui.Tables.SimplifiedTableModel;

class RecipesTableModel<IDType extends Comparable<IDType>> extends SimplifiedTableModel<RecipesTableModel.ColumnID> {

	enum ColumnID implements SimplifiedColumnIDInterface {
		Index    ("#"       , Integer.class, 20,-1, 30, 50),
		OutputAm ("O"       , Integer.class, 20,-1, 30, 50),
		Output   ("Output"  ,  String.class, 20,-1,150,150),
		Input1Am ("I1"      , Integer.class, 20,-1, 30, 50),
		Input1   ("Input 1" ,  String.class, 20,-1,150,150),
		Input2Am ("I2"      , Integer.class, 20,-1, 30, 50),
		Input2   ("Input 2" ,  String.class, 20,-1,150,150),
		Input3Am ("I3"      , Integer.class, 20,-1, 30, 50),
		Input3   ("Input 3" ,  String.class, 20,-1,150,150),
		;
		
		private SimplifiedColumnConfig columnConfig;
		
		ColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
			columnConfig = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
		}
		@Override public SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
	}

	private final DataModel<IDType> dataModel;
	private final Vector<RecipeRow> recipesRows;
	public RecipeRow clickedRecipeRow;
	public DataModel<IDType>.Recipe clickedRecipe;

	protected RecipesTableModel(DataModel<IDType> dataModel) {
		super(ColumnID.values());
		this.dataModel = dataModel;
		clickedRecipeRow = null;
		clickedRecipe = null;
		this.recipesRows = new Vector<>();
		for (int r=0; r<dataModel.recipes.size(); r++) {
			DataModel<IDType>.Recipe recipe = dataModel.recipes.get(r);
			int maxInputs = recipe.getMaxNumberOfInputs();
			for (int i=0; i<maxInputs; i++)
				recipesRows.add(new RecipeRow(r,i,recipe));
		}
	}

	public void setClickedRecipe(int rowIndex) {
		
		if (rowIndex<0 || rowIndex>=recipesRows.size()) clickedRecipeRow = null;
		else clickedRecipeRow = recipesRows.get(rowIndex);
		
		if (clickedRecipeRow == null) clickedRecipe = null;
		else clickedRecipe = clickedRecipeRow.recipe;
	}

	private class RecipeRow {
		private int index;
		private int row;
		private DataModel<IDType>.Recipe recipe;
		public RecipeRow(int index, int row, DataModel<IDType>.Recipe recipe) {
			this.index = index;
			this.row = row;
			this.recipe = recipe;
		}
	}

	@Override public ColumnID getColumnID(int columnIndex) {
		return super.getColumnID(columnIndex);
	}

	@Override public int getRowCount() { return recipesRows.size(); }

	@Override
	public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
		RecipeRow recipeRow = recipesRows.get(rowIndex);
		switch (columnID) {
		case Index   : if (recipeRow.row==0) return recipeRow.index; break;
		case Output  : if (recipeRow.row==0) return dataModel.getIngredientName(recipeRow.recipe.outputValue); break;
		case OutputAm: if (recipeRow.row==0) return getAmount(recipeRow.recipe.outputValue); break;
		case Input1  : return dataModel.getIngredientName(recipeRow.recipe.getInputValue(0, recipeRow.row));
		case Input2  : return dataModel.getIngredientName(recipeRow.recipe.getInputValue(1, recipeRow.row));
		case Input3  : return dataModel.getIngredientName(recipeRow.recipe.getInputValue(2, recipeRow.row));
		case Input1Am: return getAmount(recipeRow.recipe.getInputValue(0, recipeRow.row));
		case Input2Am: return getAmount(recipeRow.recipe.getInputValue(1, recipeRow.row));
		case Input3Am: return getAmount(recipeRow.recipe.getInputValue(2, recipeRow.row));
		}
		return null;
	}

	private Integer getAmount(DataModel<IDType>.RecipeIngredient ingredient) {
		if (ingredient==null) return null;
		return ingredient.amount;
	}

	@Override public void fireTableColumnUpdate(ColumnID columnID) {
		super.fireTableColumnUpdate(columnID);
	}

	TableCellRenderer createTableCellRenderer() {
		return new RecipesTableRenderer();
	}

	class RecipesTableRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = -8561629608671929683L;
	
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (row<recipesRows.size()) {
				
				ColumnID columnID = getColumnID(column);
				RecipeRow recipeRow = recipesRows.get(row);
				
				switch (columnID) {
				case Index:
					setHorizontalAlignment(CENTER); break;
				case Output:
				case Input1:
				case Input2:
				case Input3:
					setHorizontalAlignment(LEFT); break;
				case OutputAm:
				case Input1Am:
				case Input2Am:
				case Input3Am:
					setHorizontalAlignment(RIGHT); break;
				}
				
				boolean isProducible = recipeRow.recipe.isProducible();
				
				Color bgColor, fgColor = table.getForeground();
				
				if (recipeRow.recipe.isWrong) {
					bgColor = RecipeAnalyser.BGCOLOR_RECIPE_IS_WRONG;
					fgColor = RecipeAnalyser.TXTCOLOR_RECIPE_IS_WRONG;
					
				} else {
					if ((recipeRow.index&1)==0) bgColor = isProducible ? RecipeAnalyser.BGCOLOR_RECIPE_EVEN_PRODUCIBLE : RecipeAnalyser.BGCOLOR_RECIPE_EVEN;
					else                        bgColor = isProducible ? RecipeAnalyser.BGCOLOR_RECIPE_ODD_PRODUCIBLE : RecipeAnalyser.BGCOLOR_RECIPE_ODD;
					
					switch (columnID) {
					case Input1Am:
					case Input1:
						if (isProducible(recipeRow,0)) bgColor = darker(bgColor,0.1);
						break;
						
					case Input2Am:
					case Input2:
						if (isProducible(recipeRow,1)) bgColor = darker(bgColor,0.1);
						break;
						
					case Input3Am:
					case Input3:
						if (isProducible(recipeRow,2)) bgColor = darker(bgColor,0.1);
						break;
						
					default: break;
					}
				}
				
				if (!isSelected) {
					setBackground(bgColor);
					setForeground(fgColor);
				}
			}
			return component;
		}
	
		private Color darker(Color color, double d) {
			return new Color(
					(int)Math.floor(color.getRed  ()*(1-d)),
					(int)Math.floor(color.getGreen()*(1-d)),
					(int)Math.floor(color.getBlue ()*(1-d))
			);
		}
	
		private boolean isProducible(RecipeRow recipeRow, int i) {
			DataModel<IDType>.RecipeIngredient recipeIngredient = recipeRow.recipe.getInputValue(i, recipeRow.row);
			if (recipeIngredient==null) return false;
			return dataModel.isProducible(recipeIngredient.id) || dataModel.isInStock(recipeIngredient.id);
		}
		
		
	}

}