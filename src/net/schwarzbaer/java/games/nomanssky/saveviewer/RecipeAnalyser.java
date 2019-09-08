package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import net.schwarzbaer.gui.FileChooser;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.gui.StandardMainWindow.DefaultCloseOperation;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.gui.Tables.SimplifiedColumnIDInterface;
import net.schwarzbaer.gui.Tables.SimplifiedTableModel;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.DebugTableContextMenu;

public class RecipeAnalyser implements ActionListener {
	private static final String RECIPE_ANALYSER_CFG = "NMS_Viewer.RecipeAnalyser.cfg";

	private static final Color COLOR_INGREDIENT_MARKER = new Color(0,213,255);
	private static final Color COLOR_INGREDIENT_PRODUCIBLE = new Color(0,255,102);
	private static final Color COLOR_INGREDIENT_INPUT  = new Color(255,204,153);
	private static final Color COLOR_INGREDIENT_OUTPUT = new Color(204,255,0);

	private static final Color COLOR_RECIPE_ODD  = new Color(1.0f,1.0f,0.8f);
	private static final Color COLOR_RECIPE_EVEN = Color.WHITE;

	public static void main(String[] args) {
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		SaveViewer.loadToolbarIcons();
		start(true);
	}

	static void start(boolean standalone) {
		new RecipeAnalyser()
			.readConfig()
			.writeConfig()
			.createGUI(standalone)
			.openLastDataFile();
	}

	private FileChooser               fileChooser = null;
	private StandardMainWindow        mainwindow = null;
	private TableView.SimplifiedTable ingredientsTable = null;
	private IngredientsTableModel     ingredientsTableModel = null;
	private TableView.SimplifiedTable recipesTable = null;
	private RecipesTableModel         recipesTableModel = null;
	private RecipesTableRenderer      recipesTableRenderer = null;
	private JTable                    rawIngredientsTable = null;
	private JTable                    rawRecipesTable = null;
	
	private JCheckBoxMenuItem miHighlightProducibleInIngredientsTable = null;
	
	private File currentOpenDataFile = null;
	private Vector<String[]> rawIngredientsData   = null;
	private Vector<String[]> rawRecipesData = null;
	private RecipeListConfig recipeListConfig = null;

	private StatusFields statusFields;

	private Ingredient getIngredient(Integer ingredientIndex) {
		if (ingredientIndex==null) return null;
		if (ingredientsTableModel==null) return null;
		return ingredientsTableModel.getIngredient(ingredientIndex);
	}
	private String getIngredientName(Integer ingredientIndex) {
		if (ingredientIndex==null) return "";
		if (ingredientsTableModel==null) return "<"+ingredientIndex+">";
		return ingredientsTableModel.getIngredientName(ingredientIndex);
	}

	private RecipeAnalyser readConfig() {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(RECIPE_ANALYSER_CFG), StandardCharsets.UTF_8))) {
			String line;
			while ( (line=in.readLine())!=null ) {
				if (line.startsWith("OpenDataFile=")) {
					String valueStr = line.substring("OpenDataFile=".length());
					currentOpenDataFile = new File( valueStr );
				}
			}
		} catch (FileNotFoundException e) {
			// Is Ok :)
		} catch (IOException e) {
			e.printStackTrace();
		}
		return this;
	}

	private RecipeAnalyser writeConfig() {
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(RECIPE_ANALYSER_CFG), StandardCharsets.UTF_8))) {
			if (currentOpenDataFile!=null)
				out.printf("OpenDataFile=%s%n", currentOpenDataFile.getAbsolutePath());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return this;
	}
	
	private RecipeAnalyser openLastDataFile() {
		if (currentOpenDataFile!=null)
			readDataFromFile(currentOpenDataFile);
		return this;
	}

	enum ActionCommand {
		CopyRecipesFromClipBoard,
		CopyIngredientsFromClipBoard,
		OpenDataFile,
		SaveDataFile,
		SaveDataFileAs,
		FindConflictingRecipes,
		FindBasicRecipes,
		FindCombinableIngredients,
		ClearMarkersInIngredientsTable,
		HighlightProducibleInIngredientsTable,
		FindRecipes, SetInStock, UnsetInStock,
		;
	}

	private RecipeAnalyser createGUI(boolean standalone) {
		fileChooser = new FileChooser("RecipeAnalyser Data File", "recipes");
		
		IngredientsTableRenderer ingredientsTableRenderer = new IngredientsTableRenderer();
		ingredientsTable = new TableView.SimplifiedTable("IngredientsTable", true, true, false);
		ingredientsTable.setCellRendererForAllColumns(ingredientsTableRenderer, true);
		ingredientsTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		//namesTable.setDefaultEditor(Boolean.class, new DefaultCellEditor(new JCheckBox()));
		//namesTable.setDefaultEditor(Boolean.class, new DefaultCellEditor(new JComboBox<>(new Boolean[] {true, false})));
		ingredientsTable.setDefaultEditor(Boolean.class, new DefaultCellEditor(new JComboBox<>(new String[] {"In Stock","---"})));
		
		JCheckBoxMenuItem miHighlightProducible = SaveViewer.createCheckBoxMenuItem("Highlight producible", this, ActionCommand.HighlightProducibleInIngredientsTable);
		JMenuItem miFindCombinableIngredients = SaveViewer.createMenuItem("Mark all ingredients, that are combinable (#,#) with ####", this, ActionCommand.FindCombinableIngredients);
		JMenuItem miFindRecipes = SaveViewer.createMenuItem("Find recipe chain for ####", this, ActionCommand.FindRecipes);
		
		ingredientsTable.addContextMenuInvokeListener((rowV, columnV)->{
			if (ingredientsTableModel==null) return;
			
			int rowM = ingredientsTable.convertRowIndexToModel(rowV);
			Ingredient ingredient = ingredientsTableModel.getIngredientAtRow(rowM);
			String name = ingredient==null?null:ingredient.getName();
			boolean hasName = name != null;
			String name2 = hasName ? ("\""+name+"\"") : "<???>";
			ingredientsTableModel.clickedIngredient = hasName ? ingredient : null;

			miFindCombinableIngredients.setText(String.format("Mark all ingredients, that are combinable (#,#) with %s", name2));
			miFindCombinableIngredients.setEnabled(hasName);
			
			miHighlightProducible.setSelected(ingredientsTableModel.highlightProducible);
			
			boolean isProducible = hasName && ingredient.isOutputValue && ingredientsTableModel.producible.contains(ingredient.ingredientIndex);
			miFindRecipes.setText(String.format("Find recipe chain for %s", name2));
			miFindRecipes.setEnabled(isProducible);
		});
		
		DebugTableContextMenu contextMenu = ingredientsTable.getDebugTableContextMenu();
		contextMenu.addSeparator();
		contextMenu.add(SaveViewer.createMenuItem("Find all (#) and (#,#) recipes for selected ingredients", this, ActionCommand.FindBasicRecipes));
		contextMenu.addSeparator();
		contextMenu.add(miFindCombinableIngredients);
		contextMenu.add(SaveViewer.createMenuItem("Clear markers", this, ActionCommand.ClearMarkersInIngredientsTable));
		contextMenu.addSeparator();
		contextMenu.add(SaveViewer.createMenuItem("Set InStock for selected ingredients", this, ActionCommand.SetInStock));
		contextMenu.add(SaveViewer.createMenuItem("Unset InStock for selected ingredients", this, ActionCommand.UnsetInStock));
		contextMenu.add(miHighlightProducible);
		contextMenu.add(miFindRecipes);
		
		recipesTableRenderer = new RecipesTableRenderer();
		recipesTable = new TableView.SimplifiedTable("RecipesTable", true, true, false);
		recipesTable.setCellRendererForAllColumns(recipesTableRenderer, true);
		recipesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		rawIngredientsTable = new JTable();
		rawIngredientsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		rawIngredientsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		rawRecipesTable = new JTable();
		rawRecipesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		rawRecipesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		JTabbedPane tableTabs = new JTabbedPane();
		tableTabs.addTab("Ingredients", new JScrollPane(ingredientsTable));
		tableTabs.addTab("Recipes", new JScrollPane(recipesTable));
		tableTabs.addTab("Ingredients (Raw Data)", new JScrollPane(rawIngredientsTable));
		tableTabs.addTab("Recipes (Raw Data)", new JScrollPane(rawRecipesTable));
		tableTabs.setPreferredSize(new Dimension(1100,800));
		
		JPanel contentPane = new JPanel(new BorderLayout(3,3));
		contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		contentPane.add(tableTabs,BorderLayout.CENTER);
		contentPane.add(statusFields = new StatusFields(),BorderLayout.SOUTH);
		
		JMenu menuData = new JMenu("Data");
		menuData.add(SaveViewer.createMenuItem("Get recipe data from clipboard", this, ActionCommand.CopyRecipesFromClipBoard));
		menuData.add(SaveViewer.createMenuItem("Get ingredients data from clipboard", this, ActionCommand.CopyIngredientsFromClipBoard));
		menuData.addSeparator();
		menuData.add(SaveViewer.createMenuItem("Read data from file ..."   , this, ActionCommand.OpenDataFile));
		menuData.add(SaveViewer.createMenuItem("Write data to file"        , this, ActionCommand.SaveDataFile));
		menuData.add(SaveViewer.createMenuItem("Write data to new file ...", this, ActionCommand.SaveDataFileAs));
		
		miHighlightProducibleInIngredientsTable = SaveViewer.createCheckBoxMenuItem("Highlight producible in ingredients table", this, ActionCommand.HighlightProducibleInIngredientsTable);
		JMenu menuAnalyse = new JMenu("Analyse");
		menuAnalyse.add(SaveViewer.createMenuItem("Clear markers in ingredients table", this, ActionCommand.ClearMarkersInIngredientsTable));
		menuAnalyse.add(miHighlightProducibleInIngredientsTable);
		menuAnalyse.addSeparator();
		menuAnalyse.add(SaveViewer.createMenuItem("Find recipes with same ingredient but different result", this, ActionCommand.FindConflictingRecipes));
		menuAnalyse.add(SaveViewer.createMenuItem("Find all (#) and (#,#) recipes for selected ingredients", this, ActionCommand.FindBasicRecipes));
		
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(menuData);
		menuBar.add(menuAnalyse);
		
		mainwindow = new StandardMainWindow("Recipe Analyser",standalone?DefaultCloseOperation.EXIT_ON_CLOSE:DefaultCloseOperation.HIDE_ON_CLOSE);
		mainwindow.startGUI(contentPane,menuBar);
		return this;
	}

	private class StatusFields extends JPanel {
		private static final long serialVersionUID = 7389915416376379411L;
		private JTextField fieldInStock;
		private JTextField fieldProducible;

		StatusFields() {
			super(new GridBagLayout());
			//setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.insets = new Insets(0,5,0,0);
			
			add(new JLabel(" In Stock:"), c);
			add(fieldInStock    = new JTextField("0",10), c);
			add(new JLabel(" Producible:"), c);
			add(fieldProducible = new JTextField("0",10), c);
			fieldInStock   .setEditable(false);
			fieldProducible.setEditable(false);
			
			c.weightx = 1;
			add(new JLabel(), c);
		}

		public void setFieldInStock(int n) {
			fieldInStock.setText(""+n);
		}

		public void setFieldProducible(int n) {
			fieldProducible.setText(""+n);
		}
	
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		switch(ActionCommand.valueOf(e.getActionCommand())) {
		
		case CopyIngredientsFromClipBoard: parseIngredientsTable(parseTabTable(SaveViewer.pasteFromClipBoard())); break;
		case CopyRecipesFromClipBoard    : parseRecipesTable    (parseTabTable(SaveViewer.pasteFromClipBoard())); break;
		
		case OpenDataFile:
			if (fileChooser.showOpenDialog(mainwindow)==FileChooser.APPROVE_OPTION) {
				currentOpenDataFile = fileChooser.getSelectedFile();
				readDataFromFile(currentOpenDataFile);
				writeConfig();
			}
			break;
		case SaveDataFile:
			if (currentOpenDataFile!=null && currentOpenDataFile.isFile()) {
				saveDataToFile(currentOpenDataFile);
				writeConfig();
				break;
			}
			//break;
		case SaveDataFileAs:
			if (fileChooser.showSaveDialog(mainwindow)==FileChooser.APPROVE_OPTION) {
				currentOpenDataFile = fileChooser.getSelectedFile();
				saveDataToFile(currentOpenDataFile);
				writeConfig();
			}
			break;
			
		case FindConflictingRecipes:
			if (recipesTableModel!=null) {
				StringBuilder messages = new StringBuilder();
				HashMap<InputValueCombination,Recipe> allCombis = new HashMap<>();
				for (Recipe recipe:recipesTableModel.recipes) {
					HashSet<InputValueCombination> recipeCombis = recipe.expand();
					for (InputValueCombination combi:recipeCombis) {
						Recipe lastRecipe = allCombis.put(combi, recipe);
						if (lastRecipe!=null)
							messages.append(
									String.format(
											"Found conflicting recipes: Recipe %d (%s) and %d (%s) for inputs (%s)%n",
											recipe    .index, getIngredientName(recipe    .outputValue),
											lastRecipe.index, getIngredientName(lastRecipe.outputValue),
											combi.toString()
									)
							);
					}
				}
				String msgStr = messages.toString();
				if (!msgStr.isEmpty())
					JOptionPane.showMessageDialog(mainwindow, msgStr, "Results", JOptionPane.WARNING_MESSAGE);
				else
					JOptionPane.showMessageDialog(mainwindow, "Everthing is fine. :)", "Results", JOptionPane.INFORMATION_MESSAGE);
			}
			break;
			
		case FindBasicRecipes:
			if (ingredientsTableModel!=null) {
				StringBuilder messages = new StringBuilder();
				Integer[] selectedIngredients = ingredientsTableModel.getSelected();
				HashSet<InputValueCombination> allCombis = getSetOfAllCombis();
				for (int value:selectedIngredients) {
					InputValueCombination singleCombi = new InputValueCombination(value);
					InputValueCombination doubleCombi = new InputValueCombination(value,value);
					if (!allCombis.contains(singleCombi)) messages.append( String.format( "Can't find a recipes for (%s)%n", singleCombi.toString() ) );
					if (!allCombis.contains(doubleCombi)) messages.append( String.format( "Can't find a recipes for (%s)%n", doubleCombi.toString() ) );
				}
				String msgStr = messages.toString();
				if (!msgStr.isEmpty())
					JOptionPane.showMessageDialog(mainwindow, msgStr, "Results", JOptionPane.WARNING_MESSAGE);
				else
					JOptionPane.showMessageDialog(mainwindow, "Found all (#) and (#,#) recipes. :)", "Results", JOptionPane.INFORMATION_MESSAGE);
			}
			break;
			
		case FindRecipes:
			if (ingredientsTableModel!=null && ingredientsTableModel.clickedIngredient!=null && recipesTableModel!=null) {
				HashMap<Integer,HashSet<InputValueCombination>> allProdRecipe = new HashMap<>();
				for (Integer n:ingredientsTableModel.producible)
					allProdRecipe.put(n, new HashSet<InputValueCombination>());
				for (Recipe recipe:recipesTableModel.recipes) {
					if (ingredientsTableModel.producible.contains(recipe.outputValue)) {
						HashSet<InputValueCombination> allowedCombis = recipe.expand(ingredientsTableModel.producible::contains);
						allProdRecipe.get(recipe.outputValue).addAll(allowedCombis);
					}
				}
				//for (Integer output:allProdRecipe.keySet())
				//	for (InputValueCombination combi:allProdRecipe.get(output))
				//		System.out.printf("%s <-- %s%n", getNameStr(output), combi.toString());
				
				//HashSet<InputValueCombination> allowedCombis = allProdRecipe.get( namesTableModel.clickedName.nameIndex );
				RecipeChainFinder recipeChainFinder = new RecipeChainFinder(ingredientsTableModel.clickedIngredient.ingredientIndex,allProdRecipe,this::getIngredientName);
				recipeChainFinder.printTree(System.out);
				//recipeChainFinder.getShortestRecipeChain();
				
				
			}
			break;
			
		case FindCombinableIngredients:
			if (ingredientsTableModel!=null && ingredientsTableModel.clickedIngredient!=null) {
				HashSet<InputValueCombination> allCombis = getSetOfAllCombis();
				ingredientsTableModel.highlighted.clear();
				ingredientsTableModel.forEach(ingredient -> {
					InputValueCombination combi = new InputValueCombination(ingredientsTableModel.clickedIngredient.ingredientIndex,ingredient.ingredientIndex);
					if (allCombis.contains(combi))
						ingredientsTableModel.highlighted.add(ingredient.ingredientIndex);
				});
				ingredientsTableModel.fireTableUpdate();
			}
			break;
			
		case ClearMarkersInIngredientsTable:
			if (ingredientsTableModel!=null) {
				ingredientsTableModel.highlighted.clear();
				ingredientsTableModel.fireTableUpdate();
			}
			break;
			
		case HighlightProducibleInIngredientsTable:
			if (ingredientsTableModel!=null) {
				ingredientsTableModel.highlightProducible = !ingredientsTableModel.highlightProducible;
				miHighlightProducibleInIngredientsTable.setSelected(ingredientsTableModel.highlightProducible);
				ingredientsTableModel.fireTableUpdate();
			}
			break;
			
		case SetInStock:
			if (ingredientsTableModel!=null) {
				ingredientsTableModel.forEachSelected(ingredient->ingredient.isInStock = true);
				ingredientsTableModel.updateProducibility();
				ingredientsTableModel.fireTableUpdate();
			}
			break;
			
		case UnsetInStock:
			if (ingredientsTableModel!=null) {
				ingredientsTableModel.forEachSelected(ingredient->ingredient.isInStock = false);
				ingredientsTableModel.updateProducibility();
				ingredientsTableModel.fireTableUpdate();
			}
			break;
			
		}
	}

	private HashSet<InputValueCombination> getSetOfAllCombis() {
		HashSet<InputValueCombination> allCombis = new HashSet<>();
		if (recipesTableModel!=null)
			for (Recipe recipe:recipesTableModel.recipes) {
				HashSet<InputValueCombination> recipeCombis = recipe.expand();
				allCombis.addAll(recipeCombis);
			}
		return allCombis;
	}
	
	private static class RecipeListConfig {
		enum Type { NutrientProcessor, Refiner }
		Type type;
		
		private RecipeListConfig() {
			this.type = Type.NutrientProcessor;
		}
		
		public static RecipeListConfig readFromZIP(ZipFile zipin, String entryName) throws IOException {
			RecipeListConfig cfg = new RecipeListConfig();
			
			ZipEntry entry = zipin.getEntry(entryName);
			if (entry == null) return cfg; // no config
			
			BufferedReader in = new BufferedReader( new InputStreamReader(zipin.getInputStream(entry), StandardCharsets.UTF_8));
			String line;
			while ( (line=in.readLine())!=null ) {
				if (line.startsWith("type=")) {
					try { cfg.type = Type.valueOf( line.substring("type=".length()) ); }
					catch (Exception e) {}
				}
			}
			return cfg;
		}
		public void writeToZIP(ZipOutputStream zipout, PrintWriter out, String entryName) throws IOException {
			zipout.putNextEntry(new ZipEntry(entryName));
			out.printf("type=%s%n",type);
			out.flush();
			zipout.closeEntry();
		}
	}

	private void readDataFromFile(File file) {
		try (ZipFile zipin = new ZipFile(file)) {
			recipeListConfig = RecipeListConfig.readFromZIP(zipin, "RecipeListConfig");
			rawIngredientsData = readTabTableFromZIP(zipin, "ingredients");
			if (rawIngredientsData==null) rawIngredientsData = readTabTableFromZIP(zipin, "names"  ); // TODO
			rawRecipesData     = readTabTableFromZIP(zipin, "recipes");
			parseIngredientsTable  (rawIngredientsData  );
			parseRecipesTable(rawRecipesData);
		} catch (ZipException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void saveDataToFile(File file) {
		try (
				ZipOutputStream zipout = new ZipOutputStream(new FileOutputStream(file));
				PrintWriter out = new PrintWriter(new OutputStreamWriter(zipout, StandardCharsets.UTF_8));
		) {
			recipeListConfig.writeToZIP(zipout, out, "RecipeListConfig");
			writeTabTableToZIP(zipout, out, "ingredients", rawIngredientsData  );
			writeTabTableToZIP(zipout, out, "recipes"    , rawRecipesData);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeTabTableToZIP(ZipOutputStream zipout, PrintWriter out, String entryName, Vector<String[]> rawTabTable) throws IOException {
		if (rawTabTable!=null) {
			zipout.putNextEntry(new ZipEntry(entryName));
			for (String[] row:rawTabTable) out.println(String.join("\t", row));
			out.flush();
			zipout.closeEntry();
		}
	}

	private Vector<String[]> readTabTableFromZIP(ZipFile zipin, String entryName) throws IOException {
		ZipEntry entry = zipin.getEntry(entryName);
		if (entry == null) return null;
		
		BufferedReader in = new BufferedReader( new InputStreamReader(zipin.getInputStream(entry), StandardCharsets.UTF_8));
		return readTabTable(in);
	}

	private Vector<String[]> parseTabTable(String rawStringData) {
		if (rawStringData==null) return null;
		
		try (BufferedReader in = new BufferedReader(new StringReader(rawStringData))) {
			return readTabTable(in);
		}
		catch (IOException e) { e.printStackTrace(); }
		return null;
	}

	private Vector<String[]> readTabTable(BufferedReader in) throws IOException {
		Vector<String[]> rawTabTable = new Vector<>(); 
		String line;
		while ( (line=in.readLine())!=null ) {
			String[] parts = line.split("\t");
			rawTabTable.add(parts);
		}
		return rawTabTable;
	}

	private void parseIngredientsTable(Vector<String[]> rawTabTable) {
		if (rawTabTable==null) return;
		
		rawIngredientsData = rawTabTable;
		rawIngredientsTable.setModel(new RawdataModel(rawTabTable));
		
		Ingredient[] indexedIngredients = new Ingredient[rawTabTable.size()];
		Arrays.fill(indexedIngredients, null);
		try {
			Vector<Ingredient> ingredients = parseIngredients(rawTabTable, indexedIngredients);
			ingredientsTableModel = new IngredientsTableModel(ingredients, indexedIngredients);
			ingredientsTable.setModel(ingredientsTableModel);
			if (recipesTableModel!=null) recipesTableModel.fireTableUpdate();
			checkInputOutput();
			ingredientsTableModel.updateProducibility();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	private void parseRecipesTable(Vector<String[]> rawTabTable) {
		if (rawTabTable==null) return;
		
		rawRecipesData = rawTabTable;
		rawRecipesTable.setModel(new RawdataModel(rawTabTable));
		
		try {
			Vector<Recipe> recipes = parseRecipes(rawTabTable);
			recipesTableModel = new RecipesTableModel(recipes);
			recipesTable.setModel(recipesTableModel);
			checkInputOutput();
			if (ingredientsTableModel!=null)
				ingredientsTableModel.updateProducibility();
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
		
		Ingredient ingredient;
		for (Recipe recipe:recipesTableModel.recipes) {
			
			ingredient = getIngredient(recipe.outputValue);
			if (ingredient!=null) ingredient.isOutputValue = true;
			
			for (Vector<Integer> inputValues:recipe.inputValues) {
				for (Integer inputValue:inputValues) {
					ingredient = getIngredient(inputValue);
					if (ingredient!=null) ingredient.isInputValue = true;
				}
			}
		}
	}

	private Vector<Ingredient> parseIngredients(Vector<String[]> rawTabTable, Ingredient[] indexedIngredients) throws ParseException {
		Vector<Ingredient> ingredients = new Vector<>();
		Ingredient last = null;
		for (int row=0; row<rawTabTable.size(); row++) {
			String[] rowData = rawTabTable.get(row);
			
			String type   = rowData.length>0?rowData[0]:"";
			String nameDE = rowData.length>1?rowData[1]:"";
			String nameEN = rowData.length>2?rowData[2]:"";
			String desc   = rowData.length>3?rowData[3]:"";
			
			Ingredient ingredient = null;
			if (!type.isEmpty() || !nameDE.isEmpty() || !nameEN.isEmpty() || !desc.isEmpty())
				ingredient = new Ingredient(row+1,type,nameDE,nameEN,desc);
			
			indexedIngredients[row] = ingredient;
			if (ingredient!=null || last!=null)
				ingredients.add(last = ingredient);
		}
		return ingredients;
	}

	private Vector<Recipe> parseRecipes(Vector<String[]> rawTabTable) throws ParseException {
		Vector<Recipe> recipes = new Vector<>();
		Recipe recipe = null;
		for (int row=0; row<rawTabTable.size(); row++) {
			String[] rowData = rawTabTable.get(row);
			if (rowData.length!=7) throw new ParseException("Row %d with wrong number (%d) of entries", row, rowData.length);
			checkEmptyCell(rowData, row, 1);
			checkEmptyCell(rowData, row, 3);
			checkEmptyCell(rowData, row, 5);
			Integer outputValue = parseCell(rowData, row, 0);
			Integer inputValue1 = parseCell(rowData, row, 2);
			Integer inputValue2 = parseCell(rowData, row, 4);
			Integer inputValue3 = parseCell(rowData, row, 6);
			if (outputValue!=null) recipes.add(recipe = new Recipe(recipes.size(),outputValue));
			if (inputValue1!=null) recipe.addInputValue(0,inputValue1);
			if (inputValue2!=null) recipe.addInputValue(1,inputValue2);
			if (inputValue3!=null) recipe.addInputValue(2,inputValue3);
		}
		return recipes;
	}

	private Integer parseCell(String[] rowData, int row, int col) throws ParseException {
		if (rowData[col].equals("--"))
			return null;
		try {
			return Integer.parseInt(rowData[col]);
		} catch (NumberFormatException e) {
			throw new ParseException("Wrong data in cell (%d,%d): \"%s\"", row,col, rowData[col]);
		}
	}
	private void checkEmptyCell(String[] rowData, int row, int col) throws ParseException {
		if (!rowData[col].isEmpty()) throw new ParseException("Empty cell expected at row %d columns %d. Found \"%s\"", row,col, rowData[col]);
	}
	
	private static class RecipeChainFinder {
		
		private int finalOutput;
		private HashMap<Integer, HashSet<InputValueCombination>> allProdRecipe;
		private RecipeOutput baseRecipeOutput;
		private Function<Integer, String> nameSource;

		public RecipeChainFinder(int finalOutput, HashMap<Integer, HashSet<InputValueCombination>> allProdRecipe, Function<Integer,String> nameSource) {
			this.finalOutput = finalOutput;
			this.allProdRecipe = allProdRecipe;
			this.nameSource = nameSource;
			baseRecipeOutput = new RecipeOutput(null,this.finalOutput);
		}
		
		public void printTree(PrintStream out) {
			out.printf("Possible Recipe Chains for \"%s\"%n", nameSource.apply(finalOutput));
			baseRecipeOutput.printTree(out,"      ","      ");
		}

		private class RecipeOutput {

			private final AllowedRecipe parentRecipe;
			private final int output;
			private final Vector<AllowedRecipe> allowedRecipes;
			private final boolean isBaseInput;

			public RecipeOutput(AllowedRecipe parentRecipe, int output) {
				this.parentRecipe = parentRecipe;
				this.output = output;
				this.allowedRecipes = new Vector<>();
				HashSet<InputValueCombination> allowed = allProdRecipe.get( this.output );
				this.isBaseInput = allowed.isEmpty();
				if (!isBaseInput) {
					for (InputValueCombination recipe:allowed) {
						if (!recipeContainsParent(recipe)) {
							AllowedRecipe allowedRecipe = new AllowedRecipe(this,recipe);
							if (allowedRecipe.isExecutable)
								allowedRecipes.add(allowedRecipe);
						}
					}
				}
			}

			public void printTree(PrintStream out, String firstIndent, String nextIndent) {
				out.printf("%s%s%s", firstIndent, nameSource.apply(output), isBaseInput?"  [BaseInput]":"");
				if (allowedRecipes.size() == 1) {
					allowedRecipes.get(0).printTree(out, " ", nextIndent);
				} else {
					out.println();
					for (AllowedRecipe recipe:allowedRecipes )
						recipe.printTree(out, nextIndent+"      ", nextIndent+"      ");
				}
			}

			private boolean recipeContainsParent(InputValueCombination recipe) {
				if (recipe.contains(output)) return true;
				if (parentRecipe==null) return false;
				return parentRecipe.parentRecipeOutput.recipeContainsParent(recipe);
			}
			
		}
		
		private class AllowedRecipe {

			private final RecipeOutput parentRecipeOutput;
			private final InputValueCombination recipe;
			private final RecipeOutput[] inputs;
			private boolean isExecutable;

			public AllowedRecipe(RecipeOutput parentRecipeOutput, InputValueCombination recipe) {
				this.parentRecipeOutput = parentRecipeOutput;
				this.recipe = recipe;
				this.isExecutable = true;
				this.inputs = new RecipeOutput[this.recipe.values.length];
				for (int i = 0; i < this.recipe.values.length; i++) {
					inputs[i] = new RecipeOutput(this,this.recipe.values[i]);
					if (inputs[i].allowedRecipes.isEmpty() && !inputs[i].isBaseInput)
						isExecutable = false;
				}
			}

			public void printTree(PrintStream out, String firstIndent, String nextIndent) {
				if (inputs.length == 1) {
					out.printf("%s<--", firstIndent);
					inputs[0].printTree(out, " ", nextIndent);
				} else {
					out.printf("%s<-- %s%n", firstIndent, recipe.toString());
					for (RecipeOutput input:inputs)
						input.printTree(out, nextIndent+"      ", nextIndent+"      ");
				}
			}
		}
	
	}

	private static class Ingredient {

		private enum Type {
			IM, IP0, IP1, IP2A, IP2B, IR0, IR1, IR2, IRM, IRX, IAF, IAK, BC, BH, EI, EP
		}

		private int ingredientIndex;
		private Type type;
		private String typeStr;
		private String nameDE;
		private String nameEN;
		private String desc;
		private boolean isInputValue = false;
		private boolean isOutputValue = false;
		private boolean isInStock = false;

		public Ingredient(int ingredientIndex, String type, String nameDE, String nameEN, String desc) {
			this.ingredientIndex = ingredientIndex;
			this.typeStr = type;
			try { this.type = Type.valueOf(type); }
			catch (Exception e) { this.type = null; }
			this.nameDE = nameDE;
			this.nameEN = nameEN;
			this.desc = desc;
		}

		public String getName() {
			if (!nameDE.isEmpty()) return nameDE;
			if (!nameEN.isEmpty()) return nameEN;
			return null;
		}

		@Override
		public String toString() {
			String name = getName();
			return String.format("Name [%d] <%s> \"%s\" %s%s%s", ingredientIndex, typeStr, name==null?"":name, isInputValue?"I":"-", isOutputValue?"O":"-", isInStock?"S":"-");
		}
		
	}

	private class Recipe {

		private int index;
		private int outputValue;
		private Vector<Integer> inputValues1;
		private Vector<Integer> inputValues2;
		private Vector<Integer> inputValues3;
		private Vector<Vector<Integer>> inputValues;

		public Recipe(int index, int outputValue) {
			this.index = index;
			this.outputValue = outputValue;
			inputValues1 = new Vector<>();
			inputValues2 = new Vector<>();
			inputValues3 = new Vector<>();
			inputValues = new Vector<Vector<Integer>>();
			inputValues.add(inputValues1);
			inputValues.add(inputValues2);
			inputValues.add(inputValues3);
		}

		@Override
		public String toString() {
			return "Recipe " + index + " (" + getIngredientName(outputValue) + ")";
		}

		public HashSet<InputValueCombination> expand() {
			return expand(i->true);
		}
		public HashSet<InputValueCombination> expand(Predicate<Integer> isInputAllowed) {
			HashSet<InputValueCombination> combis = new HashSet<>();
			Vector<Vector<Integer>> nonEmptyArrays = new Vector<>();
			if (!inputValues1.isEmpty()) nonEmptyArrays.add(inputValues1);
			if (!inputValues2.isEmpty()) nonEmptyArrays.add(inputValues2);
			if (!inputValues3.isEmpty()) nonEmptyArrays.add(inputValues3);
			if (nonEmptyArrays.size()>0) {
				for (int in0:nonEmptyArrays.get(0)) {
					if (isInputAllowed.test(in0)) {
						if (nonEmptyArrays.size()>1) {
							for (int in1:nonEmptyArrays.get(1)) {
								if (isInputAllowed.test(in1)) {
									if (nonEmptyArrays.size()>2) {
										for (int in2:nonEmptyArrays.get(2)) {
											if (isInputAllowed.test(in2)) {
												combis.add(new InputValueCombination(in0,in1,in2));
											}
										}
									} else
										combis.add(new InputValueCombination(in0,in1));
								}
							}
						} else
							combis.add(new InputValueCombination(in0));
					}
				}
			}
			return combis;
		}

		public void addInputValue(int i, int val) {
			switch (i) {
			case 0: inputValues1.add(val); break;
			case 1: inputValues2.add(val); break;
			case 2: inputValues3.add(val); break;
			}
		}

		public int getMaxNumberOfInputs() {
			return Math.max(Math.max(inputValues1.size(), inputValues2.size()), inputValues3.size());
		}

		public Integer getInputValue(int i, int row) {
			switch (i) {
			case 0: if (row<inputValues1.size()) return inputValues1.get(row); break;
			case 1: if (row<inputValues2.size()) return inputValues2.get(row); break;
			case 2: if (row<inputValues3.size()) return inputValues3.get(row); break;
			}
			return null;
		}
	}

	private class InputValueCombination {
	
		private final int[] values;
	
		public InputValueCombination(int in0, int in1, int in2) {
			values = new int[] { in0,in1,in2 };
			Arrays.sort(values);
		}
		public InputValueCombination(int in0, int in1) {
			values = new int[] { Math.min(in0,in1), Math.max(in0,in1) }; 
		}
		public InputValueCombination(int in0) {
			values = new int[] { in0 }; 
		}
	
		public boolean contains(int val) {
			for (int v:values)
				if (v==val) return true;
			return false;
		}
		
		@Override
		public String toString() {
			switch (values.length) {
			case 1: return String.format("%s"        , getIngredientName(values[0]) );
			case 2: return String.format("%s, %s"    , getIngredientName(values[0]), getIngredientName(values[1]));
			case 3: return String.format("%s, %s, %s", getIngredientName(values[0]), getIngredientName(values[1]), getIngredientName(values[2]) );
			}
			return Arrays.toString(values);
		}
	
		@Override
		public int hashCode() {
			switch (values.length) {
			case 1: return values[0]%500;
			case 2: return values[0]%500 + (values[1]%500)*500;
			case 3: return values[0]%500 + (values[1]%500)*500 + (values[2]%500)*500*500;
			}
			return super.hashCode();
		}
	
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof InputValueCombination)) return false;
			InputValueCombination other = (InputValueCombination) obj;
			
			if (this.values.length!=other.values.length) return false;
			if (this.values.length>0 && this.values[0]!=other.values[0]) return false;
			if (this.values.length>1 && this.values[1]!=other.values[1]) return false;
			if (this.values.length>2 && this.values[2]!=other.values[2]) return false;
			
			return true;
		}
	
	}

	private static class ParseException extends Exception {
		private static final long serialVersionUID = -1522718627839188116L;

		ParseException(String format, Object...args) {
			super(String.format(Locale.ENGLISH, format, args));
		}
	}

	private class IngredientsTableRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = -5822408016974497527L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			
			if (ingredientsTableModel!=null) {
				switch (ingredientsTableModel.getColumnID(column)) {
				case Index:
				case Type:
				case InStock:
				case Producible:
					setHorizontalAlignment(CENTER); break;
				case NameDE:
				case NameEN:
				case Description:
					setHorizontalAlignment(LEFT); break;
				}
				
				Ingredient ingredient = ingredientsTableModel.getIngredientAtRow(row);
				if (!isSelected) {
					if (ingredient!=null && ingredient.getName()!=null) {
						if      (ingredientsTableModel.highlighted.contains(ingredient.ingredientIndex)) setBackground(COLOR_INGREDIENT_MARKER);
						else if (ingredientsTableModel.highlightProducible && ingredientsTableModel.producible .contains(ingredient.ingredientIndex)) setBackground(COLOR_INGREDIENT_PRODUCIBLE);
						else if (ingredient.isOutputValue) setBackground(COLOR_INGREDIENT_OUTPUT);
						else if (ingredient.isInputValue ) setBackground(COLOR_INGREDIENT_INPUT);
						else setBackground(table.getBackground());
					} else setBackground(table.getBackground());
				}
			}
			
			return component;
		}
	
	}

	private class RecipesTableRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = -8561629608671929683L;
	
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (recipesTableModel!=null && row<recipesTableModel.recipesRows.size()) {
				switch (recipesTableModel.getColumnID(column)) {
				case Index: setHorizontalAlignment(CENTER); break;
				case Output:
				case Input1:
				case Input2:
				case Input3: setHorizontalAlignment(LEFT); break;
				}
				RecipesTableModel.RecipeRow recipeRow = recipesTableModel.recipesRows.get(row);
				if (!isSelected) {
					if ((recipeRow.index&1)==0)
						setBackground(COLOR_RECIPE_EVEN);
					else
						setBackground(COLOR_RECIPE_ODD);
				}
			}
			return component;
		}
		
		
	}

	private enum IngredientsTableColumnID implements SimplifiedColumnIDInterface {
		Index      ("#"          , Integer.class, 20,-1, 30, 30),
		Type       ("Type"       ,  String.class, 20,-1,100,100),
		InStock    ("In Stock"   , Boolean.class, 20,-1, 60, 60),
		Producible ("Producible" ,  String.class, 20,-1, 60, 60),
		NameDE     ("Name (DE)"  ,  String.class, 20,-1,150,150),
		NameEN     ("Name (EN)"  ,  String.class, 20,-1,150,150),
		Description("Description",  String.class, 20,-1,450,450),
		;
		
		private SimplifiedColumnConfig columnConfig;
		
		IngredientsTableColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
			columnConfig = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
		}
		@Override public SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
	}
	
	private class IngredientsTableModel extends SimplifiedTableModel<IngredientsTableColumnID> {

		private Vector<Ingredient> ingredients;
		private Ingredient[] indexeIngredients;
		private Ingredient clickedIngredient = null;
		private HashSet<Integer> highlighted = new HashSet<>();
		private HashSet<Integer> producible = new HashSet<>();
		private boolean highlightProducible = false;

		protected IngredientsTableModel(Vector<Ingredient> ingredients, Ingredient[] indexedIngredients) {
			super(IngredientsTableColumnID.values());
			this.ingredients = ingredients;
			this.indexeIngredients = indexedIngredients;
		}

		private void updateProducibility() {
			producible.clear();
			forEach(ingredient->{
				if (ingredient.isInStock) producible.add(ingredient.ingredientIndex);
			});
			statusFields.setFieldInStock(producible.size());
			if (recipesTableModel!=null && !producible.isEmpty()) {
				boolean foundNew = true;
				while (foundNew) {
					foundNew = false;
					for (Recipe recipe:recipesTableModel.recipes) {
						
						if (producible.contains(recipe.outputValue))
							continue;
						
						boolean recipeIsProducible = true;
						for (Vector<Integer> inputValues:recipe.inputValues)
							if (!inputValues.isEmpty()) {
								boolean found = false;
								for (Integer inputValue:inputValues)
									if (producible.contains(inputValue)) {
										found = true;
										break;
									}
								if (!found) {
									recipeIsProducible = false;
									break;
								}
							}
						
						if (recipeIsProducible) {
							producible.add(recipe.outputValue);
							foundNew = true;
						}
					}
				}
			}
			statusFields.setFieldProducible(producible.size());
		}

		public void forEach(Consumer<Ingredient> consumer) {
			for (Ingredient ingredient:ingredients)
			 if (ingredient!=null && ingredient.getName()!=null)
				consumer.accept(ingredient);
		}

		public void forEachSelected(Consumer<Ingredient> consumer) {
			int[] selectedRows = ingredientsTable.getSelectedRows();
			for (int i=0; i<selectedRows.length; i++) {
				Ingredient ingredient = ingredients.get(selectedRows[i]);
				if (ingredient!=null && ingredient.getName()!=null)
					consumer.accept(ingredient);
			}
		}

		public Integer[] getSelected() {
			Vector<Integer> selected = new Vector<>();
			forEachSelected(ingredient->selected.add(ingredient.ingredientIndex));
			return selected.toArray(new Integer[0]);
		}

		@Override public IngredientsTableColumnID getColumnID(int columnIndex) {
			return super.getColumnID(columnIndex);
		}

		@Override public int getRowCount() {
			return ingredients.size();
		}

		public Ingredient getIngredient(int ingredientIndex) {
			int listIndex = ingredientIndex-1;
			if (listIndex<0 || listIndex>=indexeIngredients.length) return null;
			return indexeIngredients[listIndex];
		}

		public String getIngredientName(int ingredientIndex) {
			int listIndex = ingredientIndex-1;
			
			if (listIndex<0 || listIndex>=indexeIngredients.length)
				return String.format("<%d> OutOfRange(%d..%d)", ingredientIndex,1,indexeIngredients.length);
			
			Ingredient ingredient = indexeIngredients[listIndex];
			if (ingredient!=null) {
				String str = ingredient.getName();
				if (str!=null) return str;
			}
			
			return String.format("<%d>", ingredientIndex);
		}

		public Ingredient getIngredientAtRow(int rowIndex) {
			if (0<=rowIndex && rowIndex<ingredients.size())
				return ingredients.get(rowIndex);
			return null;
		}

		@Override public Object getValueAt(int rowIndex, int columnIndex, IngredientsTableColumnID columnID) {
			Ingredient ingredient = getIngredientAtRow(rowIndex);
			boolean isInput = ingredient!=null && ingredient.getName()!=null;
			switch (columnID) {
			case Index      : return !isInput ? null : ingredient.ingredientIndex;
			case InStock    : return !isInput ? null : ingredient.isInStock ? "In Stock" : "---";
			case Producible : return !isInput ? null : producible.contains(ingredient.ingredientIndex) ? "producible" : "----";
			case Type       : return ingredient==null ? "" : ingredient.type==null ? ingredient.typeStr : ingredient.type;
			case NameDE     : return ingredient==null ? "" : ingredient.nameDE;
			case NameEN     : return ingredient==null ? "" : ingredient.nameEN;
			case Description: return ingredient==null ? "" : ingredient.desc;
			}
			return null;
		}

		@Override protected void setValueAt(Object aValue, int rowIndex, int columnIndex, IngredientsTableColumnID columnID) {
			Ingredient ingredient = getIngredientAtRow(rowIndex);
			if (!(ingredient!=null && ingredient.getName()!=null)) return;
			
			switch (columnID) {
			case InStock:
				if (aValue instanceof Boolean) ingredient.isInStock = (Boolean) aValue;
				if (aValue instanceof String ) ingredient.isInStock = "In Stock".equals((String) aValue);
				updateProducibility();
				fireTableUpdate();
				break;
			default: break;
			}
		}

		@Override protected boolean isCellEditable(int rowIndex, int columnIndex, IngredientsTableColumnID columnID) {
			Ingredient ingredient = getIngredientAtRow(rowIndex);
			boolean isInput = ingredient!=null && ingredient.getName()!=null;
			if (columnID==IngredientsTableColumnID.InStock) return isInput;
			return false;
		}
	}

	private enum RecipesTableColumnID implements SimplifiedColumnIDInterface {
		Index    ("#"       , Integer.class, 20,-1, 30, 50),
		Output   ("Output"  ,  String.class, 20,-1,150,150),
		Input1   ("Input 1" ,  String.class, 20,-1,150,150),
		Input2   ("Input 2" ,  String.class, 20,-1,150,150),
		Input3   ("Input 3" ,  String.class, 20,-1,150,150),
		;
		
		private SimplifiedColumnConfig columnConfig;
		
		RecipesTableColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
			columnConfig = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
		}
		@Override public SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
	}

	private class RecipesTableModel extends SimplifiedTableModel<RecipesTableColumnID> {

		private Vector<Recipe> recipes;
		private Vector<RecipeRow> recipesRows;

		protected RecipesTableModel(Vector<Recipe> recipes) {
			super(RecipesTableColumnID.values());
			this.recipes = recipes;
			this.recipesRows = new Vector<>();
			for (int r=0; r<recipes.size(); r++) {
				Recipe recipe = recipes.get(r);
				int maxInputs = recipe.getMaxNumberOfInputs();
				for (int i=0; i<maxInputs; i++)
					recipesRows.add(new RecipeRow(r,i,recipe));
			}
		}

		private class RecipeRow {
			private int index;
			private int row;
			private Recipe recipe;
			public RecipeRow(int index, int row, Recipe recipe) {
				this.index = index;
				this.row = row;
				this.recipe = recipe;
			}
		}

		@Override public RecipesTableColumnID getColumnID(int columnIndex) {
			return super.getColumnID(columnIndex);
		}

		@Override public int getRowCount() { return recipesRows.size(); }

		@Override
		public Object getValueAt(int rowIndex, int columnIndex, RecipesTableColumnID columnID) {
			RecipeRow recipeRow = recipesRows.get(rowIndex);
			switch (columnID) {
			case Index : if (recipeRow.row==0) return recipeRow.index; break;
			case Output: if (recipeRow.row==0) return getIngredientName(recipeRow.recipe.outputValue); break;
			case Input1: return getIngredientName(recipeRow.recipe.getInputValue(0, recipeRow.row));
			case Input2: return getIngredientName(recipeRow.recipe.getInputValue(1, recipeRow.row));
			case Input3: return getIngredientName(recipeRow.recipe.getInputValue(2, recipeRow.row));
			}
			return null;
		}
	
	}

	private static class RawdataModel implements TableModel {
	
		private Vector<String[]> data;
		private int colCount;

		public RawdataModel(Vector<String[]> data) {
			this.data = data;
			colCount = 0;
			for (String[] row:data)
				colCount = Math.max(colCount, row.length);
		}
		
		@Override public void    addTableModelListener(TableModelListener l) {}
		@Override public void removeTableModelListener(TableModelListener l) {}

		@Override public int getRowCount() { return data.size(); }
		@Override public int getColumnCount() { return colCount+1; }
		@Override public String getColumnName(int columnIndex) {
			if (columnIndex==0) return null;
			columnIndex--;
			if (columnIndex<26) return ""+(char)('A'+columnIndex);
			return ""+columnIndex;
		}
		@Override public Class<?> getColumnClass(int columnIndex) { return String.class; }
		@Override public boolean isCellEditable(int rowIndex, int columnIndex) { return false; }
		
		@Override public void setValueAt(Object aValue, int rowIndex, int columnIndex) {}

		@Override public Object getValueAt(int rowIndex, int columnIndex) {
			String[] row = data.get(rowIndex);
			if (columnIndex==0) return String.format("[%03d]", rowIndex);
			columnIndex--;
			if (columnIndex<row.length)
				return row[columnIndex];
			return null;
		}
	
	}

}
