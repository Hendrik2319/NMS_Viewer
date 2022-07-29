package net.schwarzbaer.java.games.nomanssky.recipeanalyser;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import net.schwarzbaer.gui.StandardDialog;
import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.java.games.nomanssky.recipeanalyser.RecipeAnalyser.Lang;
import net.schwarzbaer.java.games.nomanssky.recipeanalyser.RecipeAnalyser.ParseException;
import net.schwarzbaer.java.games.nomanssky.recipeanalyser.RecipeAnalyser.RawdataModel;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Gui;

abstract class DataModel<IDType extends Comparable<IDType>> {
	
	private static class NutrientProcessorDataModel extends DataModel<Integer> {
		
		NutrientProcessorDataModel(RecipeAnalyser gui, boolean wasEdited, int[] tempWrongRecipes) {
			super(gui, Type.NutrientProcessor, wasEdited, tempWrongRecipes);
		}
	
		@Override protected Integer parseID(String str) {
			try { return Integer.parseInt(str); }
			catch (NumberFormatException e) { return null; }
		}
	
		@Override protected RecipeIngredient parseRecipeIngredient(int row, String[] rowData, int i) throws ParseException {
			if (rowData.length!=7) throw new ParseException("Row %d with wrong number (%d) of entries", row, rowData.length);
			int valueCol = i*2, emptyCol = valueCol-1;
			
			if (emptyCol>=0 && !rowData[emptyCol].isEmpty())
				throw new ParseException("Empty cell expected at row %d columns %d. Found \"%s\"", row,emptyCol, rowData[emptyCol]);
			
			if (rowData[valueCol].equals("--"))
				return null;
			
			try {
				int id = Integer.parseInt(rowData[valueCol]);
				return new RecipeIngredient(1,id);
			}
			catch (NumberFormatException e) {
				throw new ParseException("Wrong data in cell (%d,%d): \"%s\"", row,valueCol, rowData[valueCol]);
			}
		}
	
		@Override protected Ingredient parseIngredient(int row, String[] rowData) {
			String type   ="";
			String nameDE ="";
			String nameEN ="";
			String desc   ="";
			
			if (rowData.length>0) type   = rowData[0];
			if (rowData.length>1) nameDE = rowData[1];
			if (rowData.length>2) nameEN = rowData[2];
			if (rowData.length>3) desc   = rowData[3];
			
			if (type.isEmpty() && nameDE.isEmpty() && nameEN.isEmpty() && desc.isEmpty())
				return null;
			
			return new Ingredient(row, row+1,type,nameDE,nameEN,desc);
		}
		
		@Override protected Ingredient castIngredient(Object obj) {
			if (obj instanceof Ingredient) return (Ingredient) obj;
			return null;
		}
	
		private enum IngredientType {
			IM, IP0, IP1, IP2A, IP2B, IR0, IR1, IR2, IRM, IRX, IAF, IAK, BC, BH, EI, EP
		}
		private class Ingredient extends DataModel<Integer>.Ingredient {
		
			private final int rawRowIndex;
			private final int ingredientIndex;
			@SuppressWarnings("unused")
			private final NutrientProcessorDataModel.IngredientType type;
			private final String typeStr;
			private String nameDE;
			private String nameEN;
			private String desc;
		
			public Ingredient(int rawRowIndex, int ingredientIndex, String type, String nameDE, String nameEN, String desc) {
				this.rawRowIndex = rawRowIndex;
				this.ingredientIndex = ingredientIndex;
				this.typeStr = type;
				
				NutrientProcessorDataModel.IngredientType type_;
				try { type_ = IngredientType.valueOf(type); }
				catch (Exception e) { type_ = null; }
				this.type = type_; 
				
				this.nameDE = nameDE;
				this.nameEN = nameEN;
				this.desc = desc;
			}
	
			@Override int getRawRowIndex() { return rawRowIndex; }
	
			@Override void setName(String name, Lang language) {
				switch (language) {
				case De: nameDE = name; break;
				case En: nameEN = name; break;
				}
			}
			
			@Override void setDesc(String desc) {
				this.desc = desc;
			}
	
			@Override int getNameRawColumnIndex(Lang language) {
				switch (language) {
				case De: return 1;
				case En: return 2;
				}
				return -1;
			}
			@Override int getDescRawColumnIndex() {
				return 3;
			}
	
			@Override boolean isNameEditable(Lang language) { return true; }
			@Override boolean isDescEditable() { return true; }
		
			@Override Integer getID   () { return ingredientIndex; }
			@Override String  getType () { return typeStr; }
			@Override String  getDesc () { return desc; }
			@Override Float   getPrice() { return null; }
	
			@Override String getName(Lang language) {
				switch (language) {
				case De: if (!nameDE.isEmpty()) return nameDE; break;
				case En: if (!nameEN.isEmpty()) return nameEN; break;
				}
				return null;
			}
			
			@Override GeneralizedID getGeneralizedID() {
				// TODO [OLD] NutrientProcessorDataModel.Ingredient.getGeneralizedID()
				return null;
			}
		}
	
		@Override protected InputValueCombination createCombi(Integer in0, Integer in1, Integer in2) {
			return new InputValueCombination(in0, in1, in2);
		}
		private class InputValueCombination extends DataModel<Integer>.InputValueCombination {
			protected InputValueCombination(Integer in0, Integer in1, Integer in2) { super(in0, in1, in2); }
			@Override protected InputValueCombination cast(Object obj) {
				if (obj instanceof InputValueCombination) return (InputValueCombination) obj;
				return null;
			}
		}
	}

	private static class RefinerDataModel extends DataModel<String> {
		
		RefinerDataModel(RecipeAnalyser gui, boolean wasEdited, int[] tempWrongRecipes) {
			super(gui, Type.Refiner, wasEdited, tempWrongRecipes);
		}
	
		@Override protected String parseID(String str) {
			return str;
		}
	
		@Override protected RecipeIngredient parseRecipeIngredient(int row, String[] rowData, int i) {
			int amountCol = i*2, idCol = i*2+1;
			if (rowData.length<=idCol)return null;
			
			String id = rowData[idCol];
			if (id.isEmpty()) return null;
			
			Integer amount;
			try { amount = Integer.parseInt(rowData[amountCol]); }
			catch (NumberFormatException e) { amount = null; }
			
			return new RecipeIngredient(amount,id);
		}
	
		@Override protected Ingredient parseIngredient(int row, String[] rowData) {
			String type  ="";
			String name  ="";
			String ID    ="";
			String price ="";
			String genID ="";
			
			if (rowData.length>0) type  = rowData[0];
			if (rowData.length>2) ID    = rowData[2];
			if (rowData.length>3) genID = rowData[3];
			if (rowData.length>4) name  = rowData[4]; // -> Ingredient.getNameRawIndex()
			if (rowData.length>5) price = rowData[5];
			
			if (ID.isEmpty() && type.isEmpty() && name.isEmpty() && price.isEmpty() && genID.isEmpty())
				return null;
			
			return new Ingredient(row, ID,type,name,price,genID);
		}
		
		@Override protected Ingredient castIngredient(Object obj) {
			if (obj instanceof Ingredient) return (Ingredient) obj;
			return null;
		}
		
		private class Ingredient extends DataModel<String>.Ingredient {
	
			private final int rawRowIndex;
			private final String id;
			private final String type;
			private String name;
			private final Float price;
			private final GeneralizedID genID;
	
			public Ingredient(int rawRowIndex, String id, String type, String name, String priceStr, String genID) {
				this.rawRowIndex = rawRowIndex;
				this.id = id;
				this.type = type;
				this.name = name==null || name.isEmpty() ? null : name;
				
				Float price_;
				try { price_ = Float.parseFloat(priceStr.replace(",","")); }
				catch (NumberFormatException e) { price_ = null; }
				this.price = price_;
				
				GeneralizedID genID_ = GameInfos.getGeneralizedID(genID);
				if (genID_==null)
					genID_ = new GeneralizedID(genID);
				this.genID = genID_;
			}
	
			@Override int getRawRowIndex() { return rawRowIndex; }
	
			@Override void setName(String name, Lang language) { this.name = name; }
			@Override void setDesc(String desc) { throw new UnsupportedOperationException(); }
	
			@Override int getNameRawColumnIndex(Lang language) { return 4; }
			@Override int getDescRawColumnIndex() { return -1; }
	
			@Override boolean isNameEditable(Lang language) { return language==Lang.De; }
			@Override boolean isDescEditable() { return false; }
	
			@Override String getID   () { return id; }
			@Override String getType () { return type; }
			@Override String getName (Lang language) { return language==Lang.De ? name : null; }
			@Override String getDesc () { return null; }
			@Override Float  getPrice() { return price; }
			@Override GeneralizedID getGeneralizedID() { return genID; }
		}
	
		@Override protected InputValueCombination createCombi(String in0, String in1, String in2) {
			return new InputValueCombination(in0, in1, in2);
		}
		private class InputValueCombination extends DataModel<String>.InputValueCombination {
			protected InputValueCombination(String in0, String in1, String in2) { super(in0, in1, in2); }
			@Override protected InputValueCombination cast(Object obj) {
				if (obj instanceof InputValueCombination) return (InputValueCombination) obj;
				return null;
			}
		}
		
	}

	enum Type {
		NutrientProcessor(AppSettings.ValueKey.IngredientsInStock_NutrientProcessor),
		Refiner          (AppSettings.ValueKey.IngredientsInStock_Refiner          ),
		;
		final AppSettings.ValueKey valuekeyForIngredientsInStock;
		Type(AppSettings.ValueKey valuekeyForIngredientsInStock) {
			this.valuekeyForIngredientsInStock = valuekeyForIngredientsInStock;
		}
	}

	private final RecipeAnalyser gui;

	IngredientsTableModel<IDType> ingredientsTableModel = null;
	RecipesTableModel<IDType>     recipesTableModel = null;
	RawdataModel                  rawdataModel = null;
	
	Vector<String[]> rawIngredientsData = null;
	Vector<String[]> rawRecipesData = null;
	
	Vector<Recipe> recipes = null;
	private HashSet<IDType> producible = new HashSet<>();
	private HashSet<IDType> inStock = new HashSet<>();
	private DataModel.StockListener stockListener = null;
	
	final DataModel.Type type;
	boolean wasEdited;
	boolean hasUnsavedChanges;

	ServiceFunctions serviceFunctions;
	private int[] tempWrongRecipes;

	
	DataModel(RecipeAnalyser gui, DataModel.Type type, boolean wasEdited, int[] tempWrongRecipes) {
		this.gui = gui;
		this.type = type;
		this.wasEdited = wasEdited;
		this.hasUnsavedChanges = false;
		this.tempWrongRecipes = tempWrongRecipes;
		this.serviceFunctions = new ServiceFunctions();
	}

	void updateAfterLanguageChange() {
		recipesTableModel.fireTableColumnUpdate(RecipesTableModel.ColumnID.Input1);
		recipesTableModel.fireTableColumnUpdate(RecipesTableModel.ColumnID.Input2);
		recipesTableModel.fireTableColumnUpdate(RecipesTableModel.ColumnID.Input3);
		recipesTableModel.fireTableColumnUpdate(RecipesTableModel.ColumnID.Output);
	}

	static DataModel<?> create(RecipeAnalyser gui, DataModel.Type type) {
		return create(gui, type, false, null);
	}
	static DataModel<?> create(RecipeAnalyser gui, DataModel.Type type, boolean wasEdited, int[] tempWrongRecipes) {
		if (type != null)
			switch (type) {
			case NutrientProcessor: return new NutrientProcessorDataModel(gui, wasEdited, tempWrongRecipes);
			case Refiner          : return new           RefinerDataModel(gui, wasEdited, tempWrongRecipes);
			}
		return new NutrientProcessorDataModel(gui, wasEdited, tempWrongRecipes); // no config
	}

	static DataModel<?> readDataCfgFromZIP(RecipeAnalyser gui, ZipFile zipin, String entryName) throws IOException {
		ZipEntry entry = zipin.getEntry(entryName);
		
		boolean wasEdited = false;
		DataModel.Type type = null;
		int[] wrongRecipes = null;
		
		if (entry != null) {
			BufferedReader in = new BufferedReader( new InputStreamReader(zipin.getInputStream(entry), StandardCharsets.UTF_8));
			String line, valueStr;
			while ( (line=in.readLine())!=null ) {
				if ( line.equals("wasEdited") ) {
					wasEdited = true;
				}
				if ( (valueStr=getValueStr(line,"type="))!=null ) {
					try { type = Type.valueOf( valueStr ); }
					catch (Exception e) {}
				}
				if ( (valueStr=getValueStr(line,"wrongRecipes="))!=null ) {
					String[] parts = valueStr.split(",");
					wrongRecipes = new int[parts.length];
					for (int i=0; i<wrongRecipes.length; i++) {
						try { wrongRecipes[i] = Integer.parseInt(parts[i]); }
						catch (NumberFormatException e) { wrongRecipes[i] = -1; }
					}
					
				}
			}
		}
		
		return create(gui, type, wasEdited, wrongRecipes);
	}
	
	private static String getValueStr(String line, String prefix) {
		if (line.startsWith(prefix))
			return line.substring(prefix.length());
		return null;
	}
	
	void writeDataCfgToZIP(ZipOutputStream zipout, PrintWriter out, String entryName) throws IOException {
		zipout.putNextEntry(new ZipEntry(entryName));
		
		out.printf("type=%s%n",type);
		if (wasEdited)
			out.printf("wasEdited%n");
		
		StringBuilder wrongRecipes = new StringBuilder();
		for (int i=0; i<recipes.size(); i++) {
			Recipe recipe = recipes.get(i);
			if (recipe.isWrong) wrongRecipes.append( (i>0?",":"") + i);
		}
		if (wrongRecipes.length()>0)
			out.printf("wrongRecipes=%s%n", wrongRecipes.toString());
		
		out.flush();
		zipout.closeEntry();
	}

	void toggleClickedRecipeIsWrong() {
		if (recipesTableModel==null) return;
		if (recipesTableModel.clickedRecipe==null) return;
		recipesTableModel.clickedRecipe.isWrong = !recipesTableModel.clickedRecipe.isWrong;
		checkInputOutput();
		updateProducibility();
		gui.recipesTable.repaint();
		gui.ingredientsTable.repaint();
	}

	String getInStockIngredients() {
		Iterable<String> iterable = () -> inStock.stream().sorted().map(id->id.toString()).iterator();
		return String.join(",", iterable);
	}

	void setInStockIngredients(String str) {
		inStock.clear();
		if (str==null) return;
		String[] parts = str.split(",");
		for (String p:parts) {
			IDType id = parseID(p);
			if (id!=null) inStock.add(id);
		}
	}
	
	protected abstract IDType parseID(String str);

	void forEachRecipe(Consumer<Recipe> consumer) {
		for (Recipe recipe:recipes) {
			if (!recipe.isWrong)
				consumer.accept(recipe);
		}
	}

	boolean forEachRecipe(BiFunction<Boolean,Boolean,Boolean> merge, boolean initialResult, Predicate<Recipe> consumer) {
		boolean result = initialResult;
		for (Recipe recipe:recipes) {
			if (!recipe.isWrong)
				result = merge.apply(result, consumer.test(recipe)) ;
		}
		return result;
	}
	
	interface StockListener {
		void stockHasChanged(String inStockIngredients);
	}
	
	void setStockListener(DataModel.StockListener stockListener) {
		this.stockListener = stockListener;
	}
	
	void setInStock(boolean isInStock, IDType id) {
		if (isInStock) inStock.add(id);
		else inStock.remove(id);
		stockListener.stockHasChanged(getInStockIngredients());
	}

	boolean isInStock(IDType id) {
		return inStock.contains(id);
	}

	boolean isProducible(IDType id) {
		return producible.contains(id);
	}

	void updateProducibility() {
		producible.clear();
		producible.addAll(inStock);
		gui.statusFields.setFieldInStock(producible.size());
		if (recipes!=null && !producible.isEmpty()) {
			boolean foundNew = true;
			while (foundNew) {
				foundNew = forEachRecipe( (a,b)->a||b, false,
						recipe->{
					
					if (isProducible(recipe.outputValue.id))
						return false;
					
					Ingredient ingredient = getIngredient(recipe.outputValue.id);
					if (ingredient==null || ingredient.getName()==null)
						return false;
					
					boolean recipeIsProducible = true;
					for (Vector<RecipeIngredient> inputValues:recipe.inputValues)
						if (!inputValues.isEmpty()) {
							boolean found = false;
							for (RecipeIngredient inputValue:inputValues)
								if (isProducible(inputValue.id)) {
									found = true;
									break;
								}
							if (!found) {
								recipeIsProducible = false;
								break;
							}
						}
					
					if (recipeIsProducible) {
						producible.add(recipe.outputValue.id);
						return true;
					}
					return false;
				});
			}
		}
		gui.statusFields.setFieldProducible(producible.size());
	}

	Ingredient getIngredient(IDType id) {
		if (id==null) return null;
		if (ingredientsTableModel==null) return null;
		return ingredientsTableModel.getIngredient(id);
	}
	
	String getIngredientName(RecipeIngredient recipeIngredient) {
		if (recipeIngredient==null) return null;
		return getIngredientName(recipeIngredient.id);
	}
	String getIngredientName(IDType id) {
		if (id==null) return "";
		if (ingredientsTableModel==null) return "<"+id+">";
		return ingredientsTableModel.getIngredientName(id);
	}
	Ingredient findIngredientByName(String name) {
		if (name==null) return null;
		if (ingredientsTableModel==null) return null;
		return ingredientsTableModel.findIngredientByName(name);
	}

	void parseIngredientsTable(Vector<String[]> rawTabTable) {
		rawIngredientsData = rawTabTable;
		parseIngredientsTable();
	}

	void parseIngredientsTable() {
		if (rawIngredientsData==null) return;
		
		rawdataModel = new RawdataModel(rawIngredientsData);
		gui.rawIngredientsTable.setModel(rawdataModel);
		
		try {
			Vector<Ingredient> ingredients = parseIngredients();
			ingredientsTableModel = new IngredientsTableModel<>(gui,this,ingredients);
			gui.ingredientsTable.setCellRendererForAllColumns(ingredientsTableModel.createTableCellRenderer(), true);
			JCheckBox rendererCheckBox = new JCheckBox();
			rendererCheckBox.setHorizontalAlignment(JCheckBox.CENTER);
			gui.ingredientsTable.setDefaultEditor(Boolean.class, new DefaultCellEditor(rendererCheckBox));
			gui.ingredientsTable.setModel(ingredientsTableModel);
			if (recipesTableModel!=null) recipesTableModel.fireTableUpdate();
			checkInputOutput();
			updateProducibility();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	void parseRecipesTable(Vector<String[]> rawTabTable) {
		rawRecipesData = rawTabTable;
		parseRecipesTable();
	}

	void parseRecipesTable() {
		if (rawRecipesData==null) return;
		
		gui.rawRecipesTable.setModel(new RawdataModel(rawRecipesData));
		
		try {
			recipes = parseRecipes();
			recipesTableModel = new RecipesTableModel<>(this);
			gui.recipesTable.setCellRendererForAllColumns(recipesTableModel.createTableCellRenderer(), true);
			gui.recipesTable.setModel(recipesTableModel);
			checkInputOutput();
			updateProducibility();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	private void checkInputOutput() {
		if (recipesTableModel==null) return;
		if (ingredientsTableModel==null) return;
		
		for (Ingredient ingredient:ingredientsTableModel.ingredients) {
			if (ingredient!=null) {
				ingredient.isOutputValue = false;
				ingredient.isInputValue = false;
			}
		}
		
		forEachRecipe(recipe->{
			Ingredient ingredient = getIngredient(recipe.outputValue.id);
			if (ingredient!=null) ingredient.isOutputValue = true;
			
			for (Vector<RecipeIngredient> inputValues:recipe.inputValues) {
				for (RecipeIngredient inputValue:inputValues) {
					ingredient = getIngredient(inputValue.id);
					if (ingredient!=null) ingredient.isInputValue = true;
				}
			}
		});
	}

	private Vector<Ingredient> parseIngredients() throws ParseException {
		Vector<Ingredient> ingredients = new Vector<>();
		Ingredient last = null;
		for (int row=0; row<rawIngredientsData.size(); row++) {
			String[] rowData = rawIngredientsData.get(row);
			Ingredient ingredient = parseIngredient(row, rowData);
			
			if (ingredient!=null || last!=null)
				ingredients.add(last = ingredient);
		}
		return ingredients;
	}
	
	private Vector<Recipe> parseRecipes() throws ParseException {
		
		Vector<Recipe> recipes = new Vector<>();
		
		Recipe recipe = null;
		for (int row=0; row<rawRecipesData.size(); row++) {
			String[] rowData = rawRecipesData.get(row);
			RecipeIngredient outputValue = parseRecipeIngredient(row, rowData, 0);
			RecipeIngredient inputValue1 = parseRecipeIngredient(row, rowData, 1);
			RecipeIngredient inputValue2 = parseRecipeIngredient(row, rowData, 2);
			RecipeIngredient inputValue3 = parseRecipeIngredient(row, rowData, 3);
			if (outputValue!=null) recipes.add(recipe = new Recipe(recipes.size(),outputValue));
			if (inputValue1!=null) recipe.addInputValue(0,inputValue1);
			if (inputValue2!=null) recipe.addInputValue(1,inputValue2);
			if (inputValue3!=null) recipe.addInputValue(2,inputValue3);
		}
		
		if (tempWrongRecipes!=null)
			for (int index:tempWrongRecipes)
				if (0<=index && index<recipes.size())
					recipes.get(index).isWrong = true;
		tempWrongRecipes=null;
		
		return recipes;
	}


	protected abstract Ingredient       castIngredient       (Object obj);
	protected abstract Ingredient       parseIngredient      (int row, String[] rowData) throws ParseException;
	protected abstract RecipeIngredient parseRecipeIngredient(int row, String[] rowData, int i) throws ParseException;
	
	class ServiceFunctions {

		public void findRecipes__unfinished() {
			RecipeDialog dlg = new RecipeDialog(gui.mainwindow, "Define Ingredient Scheme");
			dlg.showDialog();
			if (dlg.hasResult()) {
				IDType output = dlg.output==null ? null : dlg.output.getID();
				IDType input1 = dlg.input1==null ? null : dlg.input1.getID();
				IDType input2 = dlg.input2==null ? null : dlg.input2.getID();
				IDType input3 = dlg.input3==null ? null : dlg.input3.getID();
				InputValueCombination inputs = createCombi(input1,input2,input3);
				
				gui.resultDialogs.showStream("Found Recipes", out->{
					forEachRecipe(recipe->{
						if (recipe.hasInput(inputs) && (output==null || recipe.outputValue.id==output)) {
							Vector<SpecificRecipe> specificRecipes = recipe.getSpecificRecipes(inputs);
							for (SpecificRecipe spRec:specificRecipes)
								out.printf("%s%n", spRec.toString());
						}
					});
				});
			}
		}
		
		public void findRecipesWith() {
			if (ingredientsTableModel!=null)
				findRecipesWith(ingredientsTableModel.clickedIngredient);
		}

		void findRecipesWith(Ingredient ingredient) {
			if (ingredient==null) return;
			if (recipes==null) return;
			
			final boolean producableOnly;
			if (ingredient.isProducible()) {
				String msg = "Show producable recipes only?";
				String title = "Producable recipes only?";
				int result = JOptionPane.showConfirmDialog(gui.mainwindow, msg, title, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
				producableOnly = result == JOptionPane.YES_OPTION;
			} else
				producableOnly = false;
			
			Gui.runWithProgressDialog(gui.mainwindow, "Find Recipes", pd->{
				IDType inputID = ingredient.getID();
				
				Gui.runInEventThreadAndWait(()->{
					pd.setTaskTitle("Find Recipes");
					pd.setValue(0, recipes.size());
				});
				
				HashMap<IDType,Vector<SpecificRecipe>> allRecipes = new HashMap<>();
				forEachRecipe(recipe->{
					Vector<SpecificRecipe> newRecipes = recipe.getSpecificRecipes( inputID, producableOnly );
					if (!newRecipes.isEmpty()) {
						Vector<SpecificRecipe> knownRecipes = allRecipes.get(recipe.outputValue.id);
						if (knownRecipes==null) allRecipes.put(recipe.outputValue.id,knownRecipes=new Vector<>());
						knownRecipes.addAll( newRecipes );
					}
					Gui.runInEventThreadAndWait(()->pd.setValue(recipe.index+1));
				});
				Vector<IDType> sortedIDs = new Vector<>(allRecipes.keySet());
				sortedIDs.sort(null);
				
				Gui.runInEventThreadAndWait(()->{
					pd.setTaskTitle("Output");
					pd.setIndeterminate(true);
				});
				
				String title = "Found Recipes with "+ingredient.getName()+(producableOnly ? " [producable only]" : "");
				gui.resultDialogs.showStream(DataModel.this, title, out->{
					for (IDType id:sortedIDs)
						for (SpecificRecipe recipe:allRecipes.get(id))
							out.println(recipe.toString());
				});
			});
		}
		
		public void markCombinableIngredients() {
			if (ingredientsTableModel!=null && ingredientsTableModel.clickedIngredient!=null) {
				HashSet<InputValueCombination> allCombis = getSetOfAllCombis();
				ingredientsTableModel.highlighted.clear();
				ingredientsTableModel.forEach(ingredient -> {
					InputValueCombination combi = createCombi(ingredientsTableModel.clickedIngredient.getID(),ingredient.getID());
					if (allCombis.contains(combi))
						ingredientsTableModel.highlighted.add(ingredient.getID());
				});
				ingredientsTableModel.fireTableUpdate();
			}
		}

		public void findGrowingCycles() {
			if (recipes!=null) {
				Gui.runWithProgressDialog(gui.mainwindow, "Search for Growing Cycles", pd->{
					Gui.runInEventThreadAndWait(()->{
						pd.setTaskTitle("Search for Growing Cycles");
						pd.setIndeterminate(true);
					});
					
					HashMap<IDType,Vector<Recipe>> allRecipes = new HashMap<>();
					forEachRecipe(recipe->{
						Vector<Recipe> recipes = allRecipes.get(recipe.outputValue.id);
						if (recipes==null) allRecipes.put(recipe.outputValue.id, recipes = new Vector<>());
						recipes.add(recipe);
					});
					
					Vector<GrowingCycle> growingCycles = new Vector<>();
					allRecipes.forEach((id1,recipes1)->{
						
						HashSet<IDType> inputs = new HashSet<>();
						for (Recipe r:recipes1)
							inputs.addAll(r.getSetOfInputs());
						
						for (IDType id2:inputs) {
							if (id1.compareTo(id2)<=0) continue;
							
							Vector<Recipe> recipes2 = allRecipes.get(id2);
							if (recipes2==null) continue;
							
							boolean noCycle = true;
							for (Recipe r:recipes2) if (r.hasInput(id1)) { noCycle = false; break; }
							if (noCycle) continue;
							
							Vector<SpecificRecipe> recipesID1fromID2 = new Vector<>();
							Vector<SpecificRecipe> recipesID2fromID1 = new Vector<>();
							for (Recipe r1:recipes1) recipesID1fromID2.addAll(r1.getSpecificRecipes(id2,false));
							for (Recipe r2:recipes2) recipesID2fromID1.addAll(r2.getSpecificRecipes(id1,false));
							
							float maxRatioID1fromID2 = 0;
							for (SpecificRecipe r1:recipesID1fromID2) {
								float ratio = r1.getRatio(id1,id2);
								if (!Float.isNaN(ratio) && ratio>maxRatioID1fromID2)
									maxRatioID1fromID2 = ratio;
							}
							float maxRatioID2fromID1 = 0;
							for (SpecificRecipe r2:recipesID2fromID1) {
								float ratio = r2.getRatio(id2,id1);
								if (!Float.isNaN(ratio) && ratio>maxRatioID2fromID1)
									maxRatioID2fromID1 = ratio;
							}
							if (maxRatioID1fromID2*maxRatioID2fromID1 > 1)
								growingCycles.add(new GrowingCycle(id1,id2,recipesID1fromID2,recipesID2fromID1));
						}
					});
					
					Gui.runInEventThreadAndWait(()->{
						pd.setTaskTitle("Generate Output");
						pd.setIndeterminate(true);
					});
					
					gui.resultDialogs.showStream("Found Growing Cycles", out->{
						for (GrowingCycle gc:growingCycles) {
							out.printf("( %s , %s )%n", getIngredientName(gc.id1), getIngredientName(gc.id2));
							for (SpecificRecipe r:gc.recipesID1fromID2)
								out.printf("     %s%n", r.toString());
							for (SpecificRecipe r:gc.recipesID2fromID1)
								out.printf("     %s%n", r.toString());
							out.println();
						}
					});
				});
			}
		}

		private class GrowingCycle {

			private IDType id1;
			private IDType id2;
			private Vector<SpecificRecipe> recipesID1fromID2;
			private Vector<SpecificRecipe> recipesID2fromID1;

			public GrowingCycle(IDType id1, IDType id2, Vector<SpecificRecipe> recipesID1fromID2, Vector<SpecificRecipe> recipesID2fromID1) {
				this.id1 = id1;
				this.id2 = id2;
				this.recipesID1fromID2 = recipesID1fromID2;
				this.recipesID2fromID1 = recipesID2fromID1;
			}
			
		}

		public String findConflictingRecipes() {
			if (recipes!=null) {
				StringBuilder messages = new StringBuilder();
				HashMap<InputValueCombination,Recipe> allCombis = new HashMap<>();
				forEachRecipe(recipe->{
					HashSet<InputValueCombination> recipeCombis = recipe.getAllCombinations();
					for (InputValueCombination combi:recipeCombis) {
						Recipe lastRecipe = allCombis.put(combi, recipe);
						if (lastRecipe!=null)
							messages.append(
									String.format(
											"Found conflicting recipes: Recipe %d (%s) and %d (%s) for inputs (%s)%n",
											recipe    .index, getIngredientName(recipe    .outputValue.id),
											lastRecipe.index, getIngredientName(lastRecipe.outputValue.id),
											combi.toString()
									)
							);
					}
				});
				return messages.toString();
			}
			return null;
		}

		public String findBasicRecipes() {
			if (ingredientsTableModel!=null) {
				StringBuilder messages = new StringBuilder();
				Vector<IDType> selectedIngredients = ingredientsTableModel.getSelected();
				HashSet<InputValueCombination> allCombis = getSetOfAllCombis();
				for (IDType value:selectedIngredients) {
					InputValueCombination singleCombi = createCombi(value);
					InputValueCombination doubleCombi = createCombi(value,value);
					if (!allCombis.contains(singleCombi)) messages.append( String.format( "Can't find a recipe for (%s)%n", singleCombi.toString() ) );
					if (!allCombis.contains(doubleCombi)) messages.append( String.format( "Can't find a recipe for (%s)%n", doubleCombi.toString() ) );
				}
				return messages.toString();
			}
			return null;
		}

		public void findRecipeChains() {
			if (ingredientsTableModel!=null)
				findRecipeChains(ingredientsTableModel.clickedIngredient);
		}

		void findRecipeChains(Ingredient ingredient) {
			if (ingredient==null) return;
			if (recipes==null) return;
			
			Vector<Consumer<PrintStream>> variants = new Vector<>(Arrays.asList(
				out -> {
					RecipeChainFinder<IDType> recipeChainFinder = new RecipeChainFinder<>(DataModel.this,ingredient,getAllProducibleRecipes());
					recipeChainFinder.search();
					recipeChainFinder.printTree(out);
				},
				out -> {
					new RecipeChainFinder2<>(DataModel.this,ingredient,getAllProducibleRecipes())
						.search()
						.printTree(out);
				}
			));
			Gui.runWithProgressDialog(gui.mainwindow, "Find Recipe Chains", pd->{
				Gui.runInEventThreadAndWait(()->{
					pd.setTaskTitle("Find Recipe Chains");
					pd.setIndeterminate(true);
				});
				gui.resultDialogs.showStreams(DataModel.this, "Found Recipe Chains for "+ingredient.getName(), variants);
			});
		}

		private HashSet<InputValueCombination> getSetOfAllCombis() {
			HashSet<InputValueCombination> allCombis = new HashSet<>();
			if (recipesTableModel!=null)
				forEachRecipe(recipe->{
					HashSet<InputValueCombination> recipeCombis = recipe.getAllCombinations();
					allCombis.addAll(recipeCombis);
				});
			return allCombis;
		}

		private HashMap<IDType, HashSet<InputValueCombination>> getAllProducibleRecipes() {
			HashMap<IDType,HashSet<InputValueCombination>> allProdRecipe = new HashMap<>();
			ingredientsTableModel.forEachProducible(n->allProdRecipe.put(n.getID(), new HashSet<InputValueCombination>()));
			forEachRecipe(recipe->{
				if (isProducible(recipe.outputValue.id)) {
					HashSet<InputValueCombination> allowedCombis = recipe.getCombinations(DataModel.this::isProducible);
					allProdRecipe.get(recipe.outputValue.id).addAll(allowedCombis);
				}
			});
			return allProdRecipe;
		}

		public void setSelectedIngredientsInStock(boolean isInStock) {
			if (ingredientsTableModel!=null) {
				ingredientsTableModel.forEachSelected(ingredient -> setInStock(isInStock, ingredient.getID()));
				updateProducibility();
				ingredientsTableModel.fireTableUpdate();
			}
		}
		
	}
	
	private boolean contains(Vector<RecipeIngredient> inputValues, IDType id) {
		return getFrom(inputValues, id) != null;
	}

	private RecipeIngredient getFrom(Vector<RecipeIngredient> inputValues, IDType id) {
		for (RecipeIngredient input:inputValues)
			if (input.id.compareTo(id)==0)
				return input;
		return null;
	}

	private InputValueCombination createCombi(IDType in0) { return createCombi(in0,null,null); } 
	private InputValueCombination createCombi(IDType in0, IDType in1) { return createCombi(in0,in1,null); }
	protected abstract InputValueCombination createCombi(IDType in0, IDType in1, IDType in2);
	
	protected abstract class InputValueCombination implements Comparable<InputValueCombination>{
	
		private final Vector<IDType> values;
	
		protected InputValueCombination(IDType in0, IDType in1, IDType in2) {
			values = new Vector<>();
			if (in0!=null) values.add(in0);
			if (in1!=null) values.add(in1);
			if (in2!=null) values.add(in2);
			values.sort(null);
		}
		
		public void forEach(Consumer<IDType> action) {
			values.forEach(action);
		}
		
		public boolean contains(IDType val) {
			for (IDType v:values)
				if (v.compareTo(val)==0) return true;
			return false;
		}
		
		@Override
		public String toString() {
			return String.join(" + ", (Iterable<String>)() -> values.stream().map(DataModel.this::getIngredientName).iterator());
		}
	
		@Override
		public int hashCode() {
			return values.stream().mapToInt(id->id.hashCode()).reduce(0, (i1,i2)->i1^i2);
		}
		
		protected abstract InputValueCombination cast(Object obj);
		
		@Override
		public boolean equals(Object obj) {
			InputValueCombination other = cast(obj);
			if (other==null) return false;
			if (this.values.size()!=other.values.size()) return false;
			for (int i=0; i<values.size(); i++)
				if (this.values.get(i).compareTo(other.values.get(i))!=0)
					return false;
			return true;
		}

		@Override
		public int compareTo(InputValueCombination other) {
			if (other == null) return -1;
			int n = Math.max(values.size(), other.values.size());
			for (int i=0; i<n; i++) {
				if (i>=values.size()) return -1;
				if (i>=other.values.size()) return +1;
				IDType id1 = values.get(i);
				IDType id2 = other.values.get(i);
				int cmp = id1.compareTo(id2);
				if (cmp!=0) return cmp;
			}
			return 0;
		}
	
	}

	abstract class Ingredient {
		protected boolean isInputValue  = false;
		protected boolean isOutputValue = false;
		//protected boolean isInStock     = false;
		
		boolean isProducible() {
			return DataModel.this.isProducible(getID());
		}
		boolean isInStock() {
			return DataModel.this.isInStock(getID());
		}
		
		@Override public String toString() {
			String name = getName();
			return String.format("Ingredient [%s] <%s> \"%s\" %s%s%s", getID(), getType(), name==null?"":name, isInputValue?"I":"-", isOutputValue?"O":"-", isInStock()?"S":"-", isProducible()?"P":"-");
		}
		
		abstract int getRawRowIndex();
		abstract void setName(String name, Lang language);
		abstract void setDesc(String desc);
		abstract int getNameRawColumnIndex(Lang language);
		abstract int getDescRawColumnIndex();
		abstract boolean isNameEditable(Lang language);
		abstract boolean isDescEditable();
		
		abstract IDType getID();
		abstract String getType();
		abstract String getName(Lang language);
		abstract String getDesc();
		abstract Float  getPrice();
		abstract GeneralizedID getGeneralizedID();
		
		String getName() {
			String name = getName(gui.selectedLang);
			if (name!=null) return name;
			
			for (Lang lang : Lang.values()) {
				name = getName(lang);
				if (name!=null) return "["+name+"]";
			}
			
			return null;
		}
		
		boolean nameEquals(String name, boolean ignoreCase) {
			if (name==null) return false;
			for (Lang lang : Lang.values())
				if ( (!ignoreCase && name.equals(getName(lang))) || 
					 ( ignoreCase && name.equalsIgnoreCase(getName(lang))))
					return true;
			return false;
		}
	}

	protected class RecipeIngredient {
		final Integer amount;
		final IDType id;
		RecipeIngredient(Integer amount, IDType id) {
			this.amount = amount;
			this.id = id;
		}
		@Override public String toString() {
			if (amount==null) return getIngredientName(id);
			return amount+" "+getIngredientName(id);
		}
	}
	
	private class SpecificRecipe {
		private RecipeIngredient out;
		private Vector<RecipeIngredient> inputs;
		public SpecificRecipe(RecipeIngredient out, RecipeIngredient in0) { this(out,in0,null,null); }
		public SpecificRecipe(RecipeIngredient out, RecipeIngredient in0, RecipeIngredient in1) { this(out,in0,in1,null); }
		public SpecificRecipe(RecipeIngredient out, RecipeIngredient in0, RecipeIngredient in1, RecipeIngredient in2) {
			this.out = out;
			inputs = new Vector<>();
			if (in0!=null) inputs.add(in0);
			if (in1!=null) inputs.add(in1);
			if (in2!=null) inputs.add(in2);
		}
		public float getRatio(IDType idOut, IDType idIn) {
			if (out.id.compareTo(idOut)!=0)
				throw new IllegalStateException();
			if (out.amount==null || out.amount==0) return Float.NaN;
			for (RecipeIngredient in:inputs)
				if (in.id.compareTo(idIn)==0) {
					if (in.amount==null || in.amount==0) return Float.NaN;
					return out.amount/in.amount;
				}
			return Float.NaN;
		}
		@Override public String toString() {
			return out.toString()+" <-- "+String.join(" + ", (Iterable<String>)() -> inputs.stream().map(RecipeIngredient::toString).iterator());
		}
	}

	class Recipe {
	
		public boolean isWrong;
		private int index;
		RecipeIngredient outputValue;
		private Vector<RecipeIngredient> inputValues1;
		private Vector<RecipeIngredient> inputValues2;
		private Vector<RecipeIngredient> inputValues3;
		private Vector<Vector<RecipeIngredient>> inputValues;
	
		public Recipe(int index, RecipeIngredient outputValue) {
			this.index = index;
			this.outputValue = outputValue;
			inputValues1 = new Vector<>();
			inputValues2 = new Vector<>();
			inputValues3 = new Vector<>();
			inputValues = new Vector<Vector<RecipeIngredient>>();
			inputValues.add(inputValues1);
			inputValues.add(inputValues2);
			inputValues.add(inputValues3);
		}
	
		@Override
		public String toString() {
			return "Recipe " + index + " (" + getIngredientName(outputValue.id) + ")";
		}
		
		public Vector<SpecificRecipe> getSpecificRecipes(IDType inputID, boolean producableOnly) {
			Vector<SpecificRecipe> recipes = new Vector<>();
			Vector<Vector<RecipeIngredient>> nonEmptyArrays = new Vector<>();
			if (!inputValues1.isEmpty()) nonEmptyArrays.add(inputValues1);
			if (!inputValues2.isEmpty()) nonEmptyArrays.add(inputValues2);
			if (!inputValues3.isEmpty()) nonEmptyArrays.add(inputValues3);
			for (int i=0; i<nonEmptyArrays.size(); i++) {
				RecipeIngredient input = getFrom(nonEmptyArrays.get(i),inputID);
				if (input!=null) {
					for (RecipeIngredient in0:nonEmptyArrays.get(0)) {
						if (i==0) in0 = input;
						if (!producableOnly || DataModel.this.isProducible(in0.id)) {
							if (nonEmptyArrays.size() <= 1)
								recipes.add(new SpecificRecipe(outputValue,in0));
							else {
								for (RecipeIngredient in1:nonEmptyArrays.get(1)) {
									if (i==1) in1 = input;
									if (!producableOnly || DataModel.this.isProducible(in1.id)) {
										if (nonEmptyArrays.size() <= 2)
											recipes.add(new SpecificRecipe(outputValue,in0,in1));
										else {
											for (RecipeIngredient in2:nonEmptyArrays.get(2)) {
												if (i==2) in2 = input;
												if (!producableOnly || DataModel.this.isProducible(in2.id))
													recipes.add(new SpecificRecipe(outputValue,in0,in1,in2));
												if (i==2) break;
											}
										}
									}
									if (i==1) break;
								}
							}
						}
						if (i==0) break;
					}
				}
			}
			return recipes;
		}
		
		public Vector<SpecificRecipe> getSpecificRecipes(InputValueCombination inputs) {
			Vector<SpecificRecipe> foundRecipes = new Vector<>();
			// TODO [OLD] DataModel.Recipe.getSpecificRecipes(InputValueCombination)
			return foundRecipes;
		}

		public HashSet<InputValueCombination> getAllCombinations() {
			return getCombinations(i->true);
		}
		public HashSet<InputValueCombination> getCombinations(Predicate<IDType> isInputAllowed) {
			HashSet<InputValueCombination> combis = new HashSet<>();
			Vector<Vector<RecipeIngredient>> nonEmptyArrays = new Vector<>();
			if (!inputValues1.isEmpty()) nonEmptyArrays.add(inputValues1);
			if (!inputValues2.isEmpty()) nonEmptyArrays.add(inputValues2);
			if (!inputValues3.isEmpty()) nonEmptyArrays.add(inputValues3);
			if (nonEmptyArrays.size()>0) {
				for (RecipeIngredient in0:nonEmptyArrays.get(0)) {
					if (isInputAllowed.test(in0.id)) {
						if (nonEmptyArrays.size() <= 1)
							combis.add(createCombi(in0.id));
						else {
							for (RecipeIngredient in1:nonEmptyArrays.get(1)) {
								if (isInputAllowed.test(in1.id)) {
									if (nonEmptyArrays.size() <= 2)
										combis.add(createCombi(in0.id,in1.id));
									else {
										for (RecipeIngredient in2:nonEmptyArrays.get(2)) {
											if (isInputAllowed.test(in2.id)) {
												combis.add(createCombi(in0.id,in1.id,in2.id));
											}
										}
									}
								}
							}
						}
					}
				}
			}
			return combis;
		}
	
		public void addInputValue(int i, RecipeIngredient val) {
			switch (i) {
			case 0: inputValues1.add(val); break;
			case 1: inputValues2.add(val); break;
			case 2: inputValues3.add(val); break;
			}
		}
	
		public int getMaxNumberOfInputs() {
			return Math.max(Math.max(inputValues1.size(), inputValues2.size()), inputValues3.size());
		}
	
		public RecipeIngredient getInputValue(int i, int row) {
			switch (i) {
			case 0: if (row<inputValues1.size()) return inputValues1.get(row); break;
			case 1: if (row<inputValues2.size()) return inputValues2.get(row); break;
			case 2: if (row<inputValues3.size()) return inputValues3.get(row); break;
			}
			return null;
		}

		public HashSet<IDType> getSetOfInputs() {
			HashSet<IDType> inputs = new HashSet<>();
			for (Vector<RecipeIngredient> inputValues:this.inputValues)
				for (RecipeIngredient input:inputValues)
					inputs.add(input.id);
			return inputs;
		}

		public boolean hasInput(InputValueCombination inputs) {
			int[] res = new  int[inputs.values.size()];
			for (int i=0; i<inputs.values.size(); i++) {
				res[i] = findInput(inputs.values.get(i));
				if (res[i]==0) return false;
			}
			if (res.length==0) return true; // no inputs
			if (res.length==1) return true; // 1 input && res[0]!=0
			
			// TODO [OLD] DataModel.Recipe.hasInput(InputValueCombination)
			return false;
		}

		public int findInput(IDType id) {
			int result = 0;
			for (int i=0; i<inputValues.size(); i++) {
				if (contains(inputValues.get(i), id))
					result |= (1<<i);
			}
			return result;
		}

		public boolean hasInput(IDType id) {
			return findInput(id)!=0;
		}

		public boolean isProducible() {
			for (Vector<RecipeIngredient> inputValues:this.inputValues) {
				boolean found = false;
				for (RecipeIngredient input:inputValues) {
					if (DataModel.this.isProducible(input.id)) {
						found = true;
						break;
					}
				}
				if (!inputValues.isEmpty() && !found)
					return false;
			}
			return true;
		}
	}

	private class RecipeDialog extends StandardDialog {
		private static final long serialVersionUID = 5554539104807205300L;
		
		public Ingredient output = null;
		public Ingredient input1 = null;
		public Ingredient input2 = null;
		public Ingredient input3 = null;

		private boolean hasResult;
		private Vector<Ingredient> ingredients;

		private ListCellRenderer<Ingredient> comboBoxRenderer;
		
		public RecipeDialog(Window parent, String title) {
			super(parent, title);
			hasResult = false;
			
			comboBoxRenderer = new Tables.NonStringRenderer<Ingredient>(obj->{
				Ingredient ingredient = castIngredient(obj);
				if (ingredient==null) return "<undefined>";
				String name = ingredient.getName();
				if (name==null) return ""+ingredient.getID();
				return name;
			}) {};
			
			if (ingredientsTableModel==null) ingredients = new Vector<>();
			else ingredients = new Vector<>(ingredientsTableModel.ingredients);
			ingredients.add(0, null);
			
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			
			JPanel inputPanel = new JPanel();
			inputPanel.setBorder(BorderFactory.createLoweredBevelBorder());
			c.weightx = 1; inputPanel.add(createComboBox(ing->output=ing),c);
			c.weightx = 0; inputPanel.add(new JLabel(" <--- "),c);
			c.weightx = 1; inputPanel.add(createComboBox(ing->input1=ing),c);
			c.weightx = 0; inputPanel.add(new JLabel(" + "),c);
			c.weightx = 1; inputPanel.add(createComboBox(ing->input2=ing),c);
			c.weightx = 0; inputPanel.add(new JLabel(" + "),c);
			c.weightx = 1; inputPanel.add(createComboBox(ing->input3=ing),c);
			
			JPanel buttonPanel = new JPanel(new GridBagLayout());
			c.weightx = 1;
			buttonPanel.add(new JLabel(),c);
			c.weightx = 0;
			buttonPanel.add(Gui.createButton("Ok"    , e->{ hasResult=true; closeDialog(); }),c);
			buttonPanel.add(Gui.createButton("Cancel", e->{ closeDialog(); }),c);
			
			JPanel contentPane = new JPanel(new BorderLayout());
			contentPane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			contentPane.add(inputPanel,BorderLayout.CENTER);
			contentPane.add(buttonPanel,BorderLayout.SOUTH);
			
			createGUI(contentPane);
		}

		private JComboBox<DataModel<IDType>.Ingredient> createComboBox(Consumer<Ingredient> selectListener) {
			JComboBox<Ingredient> comp = new JComboBox<Ingredient>(ingredients);
			comp.setRenderer(comboBoxRenderer);
			comp.setSelectedItem(null);
			comp.setPreferredSize(new Dimension(150,20));
			comp.addActionListener(e->selectListener.accept(castIngredient(comp.getSelectedItem())));
			return comp;
		}

		public boolean hasResult() {
			return hasResult;
		}
		
	}
}