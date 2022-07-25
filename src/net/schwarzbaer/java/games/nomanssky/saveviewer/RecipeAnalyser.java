package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
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
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import net.schwarzbaer.gui.Disabler;
import net.schwarzbaer.gui.FileChooser;
import net.schwarzbaer.gui.StandardDialog;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.gui.StandardMainWindow.DefaultCloseOperation;
import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.CheckBoxRendererComponent;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.gui.Tables.SimplifiedColumnIDInterface;
import net.schwarzbaer.gui.Tables.SimplifiedTableModel;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Gui.TextAreaDialog;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView;

class RecipeAnalyser implements ActionListener {

	private static final Color COLOR_INGREDIENT_MARKER = new Color(0,213,255);
	private static final Color COLOR_INGREDIENT_PRODUCIBLE = new Color(0,255,102);
	private static final Color COLOR_INGREDIENT_INPUT  = new Color(255,204,153);
	private static final Color COLOR_INGREDIENT_OUTPUT = new Color(204,255,0);

	private static final Color BGCOLOR_RECIPE_ODD  = new Color(1.0f,1.0f,0.8f);
	private static final Color BGCOLOR_RECIPE_EVEN = Color.WHITE;
	private static final Color BGCOLOR_RECIPE_ODD_PRODUCIBLE  = new Color(0xCBE7A0);
	private static final Color BGCOLOR_RECIPE_EVEN_PRODUCIBLE = new Color(0xBBD795);
	@SuppressWarnings("unused")
	private static final Color BGCOLOR_RECIPE_INGREDIENT_PRODUCIBLE = new Color(0x8CD744);
	private static final Color BGCOLOR_RECIPE_IS_WRONG  = new Color(0xF0F0F0);
	private static final Color TXTCOLOR_RECIPE_IS_WRONG = Color.GRAY;
	
	private enum Lang {
		En,De;
		@Override public String toString() {
			switch (this) {
			case De: return "German";
			case En: return "English";
			}
			return "????";
		}
	}
	
	public static void main(String[] args) {
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		Gui.loadToolbarIcons();
		start(true);
	}

	static void start(boolean standalone) {
		new RecipeAnalyser()
			.readConfig()
			.writeConfig()
			.createGUI(standalone)
			.openLastDataFile();
	}

	private Disabler<ActionCommand>   disabler = null;
	private StandardMainWindow        mainwindow = null;
	
	private FileChooser               fileChooser = null;
	private StatusFields              statusFields = null;
	private ResultDialogs             resultDialogs = null;
	
	private TableView.SimplifiedTable<DataModel.IngredientsTableColumnID> ingredientsTable = null;
	private TableView.SimplifiedTable<DataModel.RecipesTableColumnID>     recipesTable     = null;
	
	private JTable                    rawIngredientsTable = null;
	private JTable                    rawRecipesTable = null;
	
	private JCheckBoxMenuItem miHighlightProducibleInIngredientsTable = null;
	
	private File         dataFile  = null;
	private DataModel<?> dataModel = null;
	private Lang         selectedLang = AppSettings.getInstance().getEnum(AppSettings.ValueKey.RecipeAnalyserLang, Lang.En, Lang.class);

	private boolean saveInStockIngredients = false;
	private EnumMap<DataModel.Type,String> ingredientsInStock = new EnumMap<>(DataModel.Type.class);

	private JCheckBoxMenuItem miSaveInStockIngredients;

	private RecipeAnalyser readConfig() {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(FileExport.FILE_CFG_RECIPE_ANALYSER), StandardCharsets.UTF_8))) {
			String line;
			while ( (line=in.readLine())!=null ) {
				if (line.startsWith("OpenDataFile=")) {
					String valueStr = line.substring("OpenDataFile=".length());
					dataFile = new File( valueStr );
					if (!dataFile.isFile())
						dataFile = null;
				}
				if (line.equals("SaveInStockIngredients")) {
					saveInStockIngredients = true;
				}
				if (line.startsWith("IngredientsInStock.")) {
					String valueStr = line.substring("IngredientsInStock.".length());
					int pos = valueStr.indexOf('=');
					String typeStr;
					if (pos<0) {
						typeStr = valueStr;
						valueStr = "";
					} else {
						typeStr = valueStr.substring(0,pos);
						valueStr = valueStr.substring(pos+1);
						try { ingredientsInStock.put(DataModel.Type.valueOf(typeStr),valueStr); }
						catch (Exception e) {}
					}
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
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(FileExport.FILE_CFG_RECIPE_ANALYSER), StandardCharsets.UTF_8))) {
			if (dataFile!=null)
				out.printf("OpenDataFile=%s%n", dataFile.getAbsolutePath());
			if (saveInStockIngredients) {
				out.printf("SaveInStockIngredients%n");
				for (DataModel.Type type:DataModel.Type.values()) {
					String str = ingredientsInStock.get(type);
					if (str!=null) out.printf("IngredientsInStock.%s=%s%n", type, str);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return this;
	}
	
	private RecipeAnalyser openLastDataFile() {
		if (dataFile!=null)
			readDataFromFile(dataFile);
		return this;
	}

	enum ActionCommand {
		ClearData,
		OpenDataFile,
		SaveDataFile,
		SaveDataFileAs,
		
		FindRecipes__unfinished,
		FindBasicRecipes,
		FindConflictingRecipes,
		MarkCombinableIngredients,
		FindGrowingCycles,
		
		ClearMarkersInIngredientsTable,
		HighlightProducibleInIngredientsTable,
		FindRecipeChain,
		CopyRefinerRecipesFromClipBoard,
		CopyRefinerIngredientsFromClipBoard,
		CopyNutrientProcessorRecipesFromClipBoard,
		CopyNutrientProcessorIngredientsFromClipBoard,
		SaveInStockIngredients,
		SetInStock, UnsetInStock,
		MarkRecipeAsWrong, FindRecipesWith, ScrollTo,
		;
	}

	private RecipeAnalyser createGUI(boolean standalone) {
		mainwindow = new StandardMainWindow("Recipe Analyser",standalone?DefaultCloseOperation.EXIT_ON_CLOSE:DefaultCloseOperation.HIDE_ON_CLOSE);
		
		fileChooser = new FileChooser("RecipeAnalyser Data File", "recipes");
		
		resultDialogs = new ResultDialogs(mainwindow);
		
		disabler = new Disabler<ActionCommand>();
		disabler.setCareFor(ActionCommand.values());
		
		ingredientsTable = new TableView.SimplifiedTable<>("IngredientsTable", true, true, false);
		ingredientsTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		//namesTable.setDefaultEditor(Boolean.class, new DefaultCellEditor(new JCheckBox()));
		//namesTable.setDefaultEditor(Boolean.class, new DefaultCellEditor(new JComboBox<>(new Boolean[] {true, false})));
		//ingredientsTable.setDefaultEditor(Boolean.class, new DefaultCellEditor(new JComboBox<>(new String[] {"In Stock","---"})));
		
		JMenuItem miFindRecipeChain, miFindRecipesWith, miFindCombinableIngredients;
		JCheckBoxMenuItem miHighlightProducible;
		
		JPopupMenu contextMenu;
		contextMenu = ingredientsTable.getContextMenu();
		contextMenu.addSeparator();
		contextMenu.add(Gui.createMenuItem("Scroll to ...", this, disabler, ActionCommand.ScrollTo));
		contextMenu.addSeparator();
		contextMenu.add(Gui.createMenuItem("Find all (#) and (#,#) recipes for selected ingredients", this, disabler, ActionCommand.FindBasicRecipes));
		contextMenu.add(miFindRecipeChain  = Gui.createMenuItem("Find recipe chain for ####"    , this, disabler, ActionCommand.FindRecipeChain ));
		contextMenu.add(miFindRecipesWith  = Gui.createMenuItem("Find recipes with #### as input", this, disabler, ActionCommand.FindRecipesWith));
		contextMenu.addSeparator();
		contextMenu.add(miFindCombinableIngredients = Gui.createMenuItem("Mark all ingredients, that are combinable (#,#) with ####", this, disabler, ActionCommand.MarkCombinableIngredients));
		contextMenu.add(Gui.createMenuItem("Clear markers", this, disabler, ActionCommand.ClearMarkersInIngredientsTable));
		contextMenu.addSeparator();
		contextMenu.add(Gui.createMenuItem("Set InStock for selected ingredients", this, disabler, ActionCommand.SetInStock));
		contextMenu.add(Gui.createMenuItem("Unset InStock for selected ingredients", this, disabler, ActionCommand.UnsetInStock));
		contextMenu.add(miHighlightProducible = Gui.createCheckBoxMenuItem("Highlight producible", this, disabler, ActionCommand.HighlightProducibleInIngredientsTable));
		
		ingredientsTable.addContextMenuInvokeListener((rowV, columnV)->{
			if (dataModel==null || dataModel.ingredientsTableModel==null) return;
			
			int rowM = ingredientsTable.convertRowIndexToModel(rowV);
			dataModel.ingredientsTableModel.setClickedIngredient(rowM);
			DataModel<?>.Ingredient ingredient = dataModel.ingredientsTableModel.clickedIngredient;
			String name = ingredient==null ? null : ingredient.getName();
			boolean hasName = name != null;
			name = hasName ? ("\""+name+"\"") : "<???>";

			miFindCombinableIngredients.setText(String.format("Mark all ingredients, that are combinable (#,#) with %s", name));
			miFindRecipesWith          .setText(String.format("Find recipes with %s as input", name));
			miFindCombinableIngredients.setEnabled(hasName);
			miFindRecipesWith          .setEnabled(hasName);
			
			miHighlightProducible.setSelected(dataModel.ingredientsTableModel.highlightProducible);
			
			boolean isProducible = hasName && ingredient.isOutputValue && ingredient.isProducible();
			miFindRecipeChain.setText(String.format("Find recipe chain for %s", name));
			miFindRecipeChain.setEnabled(isProducible);
			updateGuiAccess();
		});
		
		recipesTable = new TableView.SimplifiedTable<>("RecipesTable", true, true, false);
		recipesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		JMenuItem miMarkRecipeAsWrong = Gui.createCheckBoxMenuItem("Recipe is wrong", this, disabler, ActionCommand.MarkRecipeAsWrong);
		recipesTable.addContextMenuInvokeListener((rowV, columnV)->{
			if (dataModel==null || dataModel.recipesTableModel==null) return;
			
			int rowM = recipesTable.convertRowIndexToModel(rowV);
			dataModel.recipesTableModel.setClickedRecipe(rowM);
			//DataModel<?>.RecipesTableModel.RecipeRow recipeRow = dataModel.recipesTableModel.clickedRecipeRow;
			DataModel<?>.Recipe clickedRecipe = dataModel.recipesTableModel.clickedRecipe;
			miMarkRecipeAsWrong.setText( String.format("%s is wrong", clickedRecipe==null ? "Recipe" : clickedRecipe.toString()) );
			miMarkRecipeAsWrong.setEnabled(clickedRecipe!=null);
			miMarkRecipeAsWrong.setSelected(clickedRecipe!=null && clickedRecipe.isWrong);
			updateGuiAccess();
		});
		
		contextMenu = recipesTable.getContextMenu();
		contextMenu.addSeparator();
		contextMenu.add(miMarkRecipeAsWrong);
		
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
		
		JMenuBar menuBar = new JMenuBar();
		
		JMenu menuData = menuBar.add(new JMenu("Data"));
		menuData.add(Gui.createMenuItem("Get Refiner recipe from clipboard", this, disabler, ActionCommand.CopyRefinerRecipesFromClipBoard));
		menuData.add(Gui.createMenuItem("Get Refiner ingredients from clipboard", this, disabler, ActionCommand.CopyRefinerIngredientsFromClipBoard));
		menuData.addSeparator();
		menuData.add(Gui.createMenuItem("Get Nutrient Processor recipe from clipboard", this, disabler, ActionCommand.CopyNutrientProcessorRecipesFromClipBoard));
		menuData.add(Gui.createMenuItem("Get Nutrient Processor ingredients from clipboard", this, disabler, ActionCommand.CopyNutrientProcessorIngredientsFromClipBoard));
		menuData.addSeparator();
		menuData.add(Gui.createMenuItem("Clear data"                , this, disabler, ActionCommand.ClearData));
		menuData.add(Gui.createMenuItem("Read data from file ..."   , this, disabler, ActionCommand.OpenDataFile));
		menuData.add(Gui.createMenuItem("Write data to file"        , this, disabler, ActionCommand.SaveDataFile));
		menuData.add(Gui.createMenuItem("Write data to new file ...", this, disabler, ActionCommand.SaveDataFileAs));
		
		miHighlightProducibleInIngredientsTable = Gui.createCheckBoxMenuItem("Highlight producible in ingredients table", this, disabler, ActionCommand.HighlightProducibleInIngredientsTable);
		miSaveInStockIngredients = Gui.createCheckBoxMenuItem("Save InStock ingredients", this, disabler, ActionCommand.SaveInStockIngredients);
		miSaveInStockIngredients.setSelected(saveInStockIngredients);
		JMenu menuAnalyse = menuBar.add(new JMenu("Analyse"));
		menuAnalyse.add(miSaveInStockIngredients);
		menuAnalyse.addSeparator();
		menuAnalyse.add(Gui.createMenuItem("Clear markers in ingredients table", this, disabler, ActionCommand.ClearMarkersInIngredientsTable));
		menuAnalyse.add(miHighlightProducibleInIngredientsTable);
		menuAnalyse.addSeparator();
		menuAnalyse.add(Gui.createMenuItem("Find recipes with same ingredients but different result", this, disabler, ActionCommand.FindConflictingRecipes));
		menuAnalyse.add(Gui.createMenuItem("Find all basic recipes { (#) and (#,#) } for selected ingredients", this, disabler, ActionCommand.FindBasicRecipes));
		menuAnalyse.add(Gui.createMenuItem("Find recipes cycles where amount of at least one ingredient is growing", this, disabler, ActionCommand.FindGrowingCycles));
		menuAnalyse.add(Gui.createMenuItem("[Find recipes with specific ingredients or output]", this, disabler, ActionCommand.FindRecipes__unfinished));
		
		menuBar.add(resultDialogs.createMenu("Result Windows"));
		
		JMenu menuLanguage = menuBar.add(new JMenu("Language"));
		ButtonGroup bg = new ButtonGroup();
		for (Lang lang : Lang.values()) {
			menuLanguage.add(Gui.createCheckBoxMenuItem(lang.toString(), bg, selectedLang==lang, (Boolean b)->{
				selectedLang = lang;
				System.out.printf("selectedLang = %s%n", selectedLang);
				AppSettings.getInstance().putEnum(AppSettings.ValueKey.RecipeAnalyserLang, selectedLang);
				if (dataModel!=null) dataModel.updateAfterLanguageChange();
			}));
		}
		
		mainwindow.startGUI(contentPane,menuBar);
		
		AppSettings.getInstance().registerExtraWindow(mainwindow,
			AppSettings.ValueKey.RecipeAnalyserWindowX,
			AppSettings.ValueKey.RecipeAnalyserWindowY,
			AppSettings.ValueKey.RecipeAnalyserWindowWidth,
			AppSettings.ValueKey.RecipeAnalyserWindowHeight
		);
		
		updateGuiAccess();
		updateWindowTitle();
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
			add(fieldInStock    = new JTextField("0",3), c);
			add(new JLabel(" Producible:"), c);
			add(fieldProducible = new JTextField("0",3), c);
			fieldInStock   .setEditable(false);
			fieldProducible.setEditable(false);
			
			c.weightx = 1;
			add(new JLabel(), c);
		}

		public void clear() {
			fieldInStock.setText("");
			fieldProducible.setText("");
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
		
		case CopyRefinerRecipesFromClipBoard              : setDataModelType(DataModel.Type.Refiner          );  dataModel.parseRecipesTable    (parseTabTable(SaveViewer.pasteFromClipBoard())); break;
		case CopyRefinerIngredientsFromClipBoard          : setDataModelType(DataModel.Type.Refiner          );  dataModel.parseIngredientsTable(parseTabTable(SaveViewer.pasteFromClipBoard())); break;
		case CopyNutrientProcessorRecipesFromClipBoard    : setDataModelType(DataModel.Type.NutrientProcessor);  dataModel.parseRecipesTable    (parseTabTable(SaveViewer.pasteFromClipBoard())); break;
		case CopyNutrientProcessorIngredientsFromClipBoard: setDataModelType(DataModel.Type.NutrientProcessor);  dataModel.parseIngredientsTable(parseTabTable(SaveViewer.pasteFromClipBoard())); break;
		
		case ClearData:
			dataModel = null;
			dataFile = null;
			
			recipesTable.setModel(new DefaultTableModel());
			ingredientsTable.setModel(new DefaultTableModel());
			rawRecipesTable.setModel(new DefaultTableModel());
			rawIngredientsTable.setModel(new DefaultTableModel());
			
			statusFields.clear();
			resultDialogs.closeAllDialogs();
			
			updateGuiAccess();
			updateWindowTitle();
			break;
			
		case OpenDataFile:
			if (fileChooser.showOpenDialog(mainwindow)==FileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				if (file!=null) {
					dataFile = file;
					readDataFromFile(dataFile);
					writeConfig();
				}
			}
			break;
			
		case SaveDataFile:
			if (dataFile!=null && dataFile.isFile()) {
				saveDataToFile(dataFile);
				writeConfig();
				break;
			}
			//break;
			
		case SaveDataFileAs:
			if (fileChooser.showSaveDialog(mainwindow)==FileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				if (file!=null) {
					dataFile = file;
					saveDataToFile(dataFile);
					writeConfig();
				}
			}
			break;
			
		case FindConflictingRecipes:
			if (dataModel!=null) {
				String msgStr = dataModel.serviceFunctions.findConflictingRecipes();
				if (msgStr!=null) {
					if (!msgStr.isEmpty())
						JOptionPane.showMessageDialog(mainwindow, msgStr, "Results", JOptionPane.WARNING_MESSAGE);
					else
						JOptionPane.showMessageDialog(mainwindow, "Everthing is fine. :)", "Results", JOptionPane.INFORMATION_MESSAGE);
				}
			}
			break;
			
		case FindBasicRecipes:
			if (dataModel!=null) {
				String msgStr = dataModel.serviceFunctions.findBasicRecipes();
				if (msgStr!=null) {
					if (!msgStr.isEmpty())
						JOptionPane.showMessageDialog(mainwindow, msgStr, "Results", JOptionPane.WARNING_MESSAGE);
					else
						JOptionPane.showMessageDialog(mainwindow, "Found all (#) and (#,#) recipes. :)", "Results", JOptionPane.INFORMATION_MESSAGE);
				}
			}
			break;
			
		case FindRecipes__unfinished:
			if (dataModel!=null) dataModel.serviceFunctions.findRecipes__unfinished();
			break;
			
		case FindRecipesWith:
			if (dataModel!=null) dataModel.serviceFunctions.findRecipesWith();
			break;
			
		case FindRecipeChain:
			if (dataModel!=null) dataModel.serviceFunctions.findRecipeChains();
			break;
			
		case FindGrowingCycles:
			if (dataModel!=null) dataModel.serviceFunctions.findGrowingCycles();
			break;
			
		case MarkCombinableIngredients:
			if (dataModel!=null) dataModel.serviceFunctions.markCombinableIngredients();
			break;
			
		case ClearMarkersInIngredientsTable:
			if (dataModel!=null && dataModel.ingredientsTableModel!=null) {
				dataModel.ingredientsTableModel.highlighted.clear();
				dataModel.ingredientsTableModel.fireTableUpdate();
			}
			break;
			
		case HighlightProducibleInIngredientsTable:
			if (dataModel!=null && dataModel.ingredientsTableModel!=null) {
				dataModel.ingredientsTableModel.highlightProducible = !dataModel.ingredientsTableModel.highlightProducible;
				miHighlightProducibleInIngredientsTable.setSelected(dataModel.ingredientsTableModel.highlightProducible);
				dataModel.ingredientsTableModel.fireTableUpdate();
			}
			break;
			
		case MarkRecipeAsWrong:
			if (dataModel!=null)
				dataModel.toggleClickedRecipeIsWrong();
			break;
			
		case SetInStock  : setInStock(true ); break;
		case UnsetInStock: setInStock(false); break;
		case SaveInStockIngredients:
			saveInStockIngredients = !saveInStockIngredients;
			miSaveInStockIngredients.setSelected(saveInStockIngredients);
			if (saveInStockIngredients)
				ingredientsInStock.put( dataModel.type, dataModel.getInStockIngredients() );
			writeConfig();
			break;
			
		case ScrollTo:
			if (dataModel!=null && dataModel.ingredientsTableModel!=null) {
				String msg = "Enter Name of Ingredient: ";
				String title = "Scroll to Ingredient";
				String name = JOptionPane.showInputDialog(mainwindow, msg, title, JOptionPane.QUESTION_MESSAGE);
				if (name==null) return;
				
				int rowM = dataModel.ingredientsTableModel.findIngredientRowIndexByName(name);
				if (rowM<0) {
					msg = String.format("Can't find an ingredient with name \"%s\".", name);
					JOptionPane.showMessageDialog(mainwindow, msg, "Ingredient not found", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				
				int rowV = ingredientsTable.convertRowIndexToView(rowM);
				if (rowV<0) return;
				
				ingredientsTable.setRowSelectionInterval(rowV, rowV);
				Rectangle cellRect = ingredientsTable.getCellRect(rowV, 0, true);
				ingredientsTable.scrollRectToVisible(cellRect);
			}
			break;
		}
	}

	private void setInStock(boolean isInStock) {
		if (dataModel!=null) {
			dataModel.serviceFunctions.setSelectedIngredientsInStock(isInStock);
			if (saveInStockIngredients) {
				ingredientsInStock.put( dataModel.type, dataModel.getInStockIngredients() );
				writeConfig();
			}
		}
	}

	private void updateWindowTitle() {
		String str = "";
		if (dataModel == null)
			str += " - <No Data>";
		else {
			str += "  -  " + (dataFile==null ? "" : dataFile.getName());
			if (dataFile==null || dataModel.hasUnsavedChanges)
				str += " <unsaved>";
			switch (dataModel.type) {
			case NutrientProcessor: str += "  [NutrientProcessor Recipes]"; break;
			case Refiner          : str += "  [Refiner Recipes]"; break;
			}
		}
		mainwindow.setTitle("Recipe Analyser"+str);
	}

	private void setDataModelType(DataModel.Type type) {
		if (dataModel == null) {
			resultDialogs.closeAllDialogs();
			dataModel = DataModel.create(type);
			dataModel.setGui(this);
			dataModel.setStockListener(this::ingredientsStockHasChanged);
			if (saveInStockIngredients)
				dataModel.setInStockIngredients(ingredientsInStock.get(dataModel.type));
			else
				ingredientsInStock.clear();
		} else {
			if (dataModel.type != type)
				throw new IllegalStateException(String.format("Can't set type of RecipeListConfig to \"%s\". It is currently set to \"%s\".", type, dataModel.type));
		}
		updateGuiAccess();
		updateWindowTitle();
	}

	private void ingredientsStockHasChanged(String str) {
		ingredientsInStock.put(dataModel.type,str);
		if (saveInStockIngredients)
			writeConfig();
	}

	private void updateGuiAccess() {
		disabler.setEnable(ac->{
			switch (ac) {
			case CopyNutrientProcessorIngredientsFromClipBoard:
			case CopyNutrientProcessorRecipesFromClipBoard:
				return dataModel==null || (dataModel.type==DataModel.Type.NutrientProcessor && !dataModel.wasEdited);
			case CopyRefinerIngredientsFromClipBoard:
			case CopyRefinerRecipesFromClipBoard:
				return dataModel==null || (dataModel.type==DataModel.Type.Refiner && !dataModel.wasEdited);
				
			case OpenDataFile:
				return true;
			case ClearData:
			case SaveDataFile:
			case SaveDataFileAs:
				return dataModel!=null;
				
			case FindRecipes__unfinished:
				return false;
				
			case FindConflictingRecipes:
			case FindGrowingCycles:
				return dataModel!=null && dataModel.recipes!=null;
				
			case FindBasicRecipes:
				return dataModel!=null && dataModel.recipes!=null && ingredientsTable.getSelectedRowCount()>0;
				
			case ClearMarkersInIngredientsTable:
			case HighlightProducibleInIngredientsTable:
			case SaveInStockIngredients:
				return dataModel!=null && dataModel.ingredientsTableModel!=null;
				
			case SetInStock:
			case UnsetInStock:
				return dataModel!=null && dataModel.ingredientsTableModel!=null && ingredientsTable.getSelectedRowCount()>0;
				
			case MarkRecipeAsWrong:
				return dataModel!=null && dataModel.recipesTableModel!=null && dataModel.recipesTableModel.clickedRecipe!=null;
				
			case FindRecipeChain:
			case MarkCombinableIngredients:
			case FindRecipesWith:
				return null;
				
			case ScrollTo:
				return dataModel!=null && dataModel.ingredientsTableModel!=null;
			}
			return null;
		});
	}

	private void readDataFromFile(File file) {
		try (ZipFile zipin = new ZipFile(file)) {
			resultDialogs.closeAllDialogs();
			dataModel = DataModel.readDataCfgFromZIP(zipin, "RecipeListConfig");
			dataModel.setGui(this);
			dataModel.setStockListener(this::ingredientsStockHasChanged);
			if (saveInStockIngredients)
				dataModel.setInStockIngredients(ingredientsInStock.get(dataModel.type));
			else
				ingredientsInStock.clear();
			dataModel.rawIngredientsData = readTabTableFromZIP(zipin, "ingredients");
			dataModel.rawRecipesData     = readTabTableFromZIP(zipin, "recipes");
			dataModel.parseIngredientsTable();
			dataModel.parseRecipesTable();
		} catch (ZipException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		updateGuiAccess();
		updateWindowTitle();
	}

	private void saveDataToFile(File file) {
		try (
				ZipOutputStream zipout = new ZipOutputStream(new FileOutputStream(file));
				PrintWriter out = new PrintWriter(new OutputStreamWriter(zipout, StandardCharsets.UTF_8));
		) {
			dataModel.writeDataCfgToZIP(zipout, out, "RecipeListConfig");
			writeTabTableToZIP(zipout, out, "ingredients", dataModel.rawIngredientsData);
			writeTabTableToZIP(zipout, out, "recipes"    , dataModel.rawRecipesData);
			dataModel.hasUnsavedChanges = false;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		updateWindowTitle();
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
	
	@SuppressWarnings("unused")
	private static class ResultDialogs {
		
		private final Window mainwindow;
		private final Vector<TextAreaDialog> openDialogs;
		private JMenu menu;
		private boolean ignoreCloseEvents;
		
		ResultDialogs(Window mainwindow) {
			this.mainwindow = mainwindow;
			menu = null;
			openDialogs = new Vector<>();
			ignoreCloseEvents = false;
		}

		JMenu createMenu(String title) {
			menu = new JMenu(title);
			fillMenu();
			return menu;
		}

		private void fillMenu() {
			if (menu==null) return;
			menu.removeAll();
			menu.add(Gui.createMenuItem("Close All", e->closeAllDialogs(), !openDialogs.isEmpty()));
			if (!openDialogs.isEmpty()) {
				menu.addSeparator();
				for (TextAreaDialog dlg : openDialogs)
					menu.add(Gui.createMenuItem(dlg.getTitle(), e->dlg.requestFocus()));
			}
		}

		void closeAllDialogs() {
			ignoreCloseEvents = true;
			for (TextAreaDialog dlg : openDialogs)
				dlg.closeDialog();
			ignoreCloseEvents = false;
			openDialogs.clear();
			fillMenu();
		}

		private void dialogClosed(TextAreaDialog dlg) {
			if (ignoreCloseEvents) return;
			openDialogs.remove(dlg);
			fillMenu();
		}

		void showStream(String title, Consumer<PrintStream> print) {
			TextAreaDialog resultDialog = new TextAreaDialog(mainwindow, title, this::dialogClosed);
			resultDialog.setText_Stream(print);
			showDialog(resultDialog);
		}

		void showWriter(String title, Consumer<PrintWriter> print) {
			TextAreaDialog resultDialog = new TextAreaDialog(mainwindow, title, this::dialogClosed);
			resultDialog.setText_Writer(print);
			showDialog(resultDialog);
		}

		void showStreams(String title, Vector<Consumer<PrintStream>> prints) {
			TextAreaDialog resultDialog = TextAreaDialog.createForStreamVariants(mainwindow, title, this::dialogClosed, prints);
			showDialog(resultDialog);
		}

		void showWriters(String title, Vector<Consumer<PrintWriter>> prints) {
			TextAreaDialog resultDialog = TextAreaDialog.createForWriterVariants(mainwindow, title, this::dialogClosed, prints);
			showDialog(resultDialog);
		}

		<IDType extends Comparable<IDType>> void showStream(DataModel<IDType> dataModel, String title, Consumer<PrintStream> print) {
			TextAreaDialog resultDialog = new ResultDialog<>(dataModel, mainwindow, title, this::dialogClosed);
			resultDialog.setText_Stream(print);
			showDialog(resultDialog);
		}

		<IDType extends Comparable<IDType>> void showWriter(DataModel<IDType> dataModel, String title, Consumer<PrintWriter> print) {
			TextAreaDialog resultDialog = new ResultDialog<>(dataModel, mainwindow, title, this::dialogClosed);
			resultDialog.setText_Writer(print);
			showDialog(resultDialog);
		}

		<IDType extends Comparable<IDType>> void showStreams(DataModel<IDType> dataModel, String title, Vector<Consumer<PrintStream>> prints) {
			TextAreaDialog resultDialog = ResultDialog.createResultDialogForStreamVariants(dataModel, mainwindow, title, this::dialogClosed, prints);
			showDialog(resultDialog);
		}

		<IDType extends Comparable<IDType>> void showWriters(DataModel<IDType> dataModel, String title, Vector<Consumer<PrintWriter>> prints) {
			TextAreaDialog resultDialog = ResultDialog.createResultDialogForWriterVariants(dataModel, mainwindow, title, this::dialogClosed, prints);
			showDialog(resultDialog);
		}

		private void showDialog(TextAreaDialog resultDialog) {
			resultDialog.showDialog();
			openDialogs.add(resultDialog);
			openDialogs.sort(Comparator.nullsLast(Comparator.comparing(TextAreaDialog::getTitle)));
			fillMenu();
		}
		
		@SuppressWarnings("unused")
		private static class ResultDialog<IDType extends Comparable<IDType>> extends Gui.TextAreaDialog {
			private static final long serialVersionUID = 5466523258727339825L;
			public static <IDType extends Comparable<IDType>> ResultDialog<IDType> createResultDialogForWriterVariants(
					DataModel<IDType> dataModel,
					Window parent, String title,
					Consumer<TextAreaDialog> closeListener,
					Vector<Consumer<PrintWriter>> variants
			) {
				Constructor<PrintWriter, ResultDialog<IDType>> constructor =
					(parent1, title1, closeListener1, variants1, setText) ->
						new ResultDialog<IDType>(dataModel, parent1, title1, closeListener1, variants1, setText);
				return createForWriterVariants(constructor, parent, title, closeListener, variants);
			}

			public static <IDType extends Comparable<IDType>> ResultDialog<IDType> createResultDialogForStreamVariants(
					DataModel<IDType> dataModel,
					Window parent, String title,
					Consumer<TextAreaDialog> closeListener,
					Vector<Consumer<PrintStream>> variants
			) {
				Constructor<PrintStream, ResultDialog<IDType>> constructor =
					(parent1, title1, closeListener1, variants1, setText) -> 
						new ResultDialog<IDType>(dataModel, parent1, title1, closeListener1, variants1, setText);
				return createForStreamVariants(constructor, parent, title, closeListener, variants);
			}

			private DataModel<IDType>.Ingredient selectedIngredient;

			private ResultDialog(DataModel<IDType> dataModel, Window parent, String title, Consumer<TextAreaDialog> closeListener) {
				this(dataModel, parent, title, closeListener, null, null);
			}

			private <Output> ResultDialog(
					DataModel<IDType> dataModel,
					Window parent, String title,
					Consumer<TextAreaDialog> closeListener,
					Vector<Consumer<Output>> variants,
					BiConsumer<TextAreaDialog, Consumer<Output>> setText
			) {
				super(parent, title, closeListener, variants, setText);
				if (dataModel==null) throw new IllegalArgumentException();
				
				selectedIngredient = null;
				
				JMenuItem miFindRecipeChain, miFindRecipesWith;
				JPopupMenu contextMenu = new JPopupMenu();
				contextMenu.add(miFindRecipeChain = Gui.createMenuItem("Find recipe chain for ####"     , e->{
					SwingUtilities.invokeLater(()->{
						if (selectedIngredient!=null)
							dataModel.serviceFunctions.findRecipeChains(selectedIngredient);
					});
				}));
				contextMenu.add(miFindRecipesWith = Gui.createMenuItem("Find recipes with #### as input", e->{
					SwingUtilities.invokeLater(()->{
						if (selectedIngredient!=null)
							dataModel.serviceFunctions.findRecipesWith(selectedIngredient);
					});
				}));
				
				Gui.ContextMenuInvoker menuInvoker = new Gui.ContextMenuInvoker(outputTextArea, contextMenu);
				menuInvoker.addContextMenuInvokeListener((x, y) -> {
					String text = outputTextArea.getSelectedText();
					selectedIngredient = dataModel.findIngredientByName(text);
					
					miFindRecipeChain.setEnabled(selectedIngredient!=null);
					miFindRecipesWith.setEnabled(selectedIngredient!=null);
					miFindRecipeChain.setText(selectedIngredient==null
							? "Find recipe chain"
							: String.format("Find recipe chain for \"%s\"", selectedIngredient.getName()));
					miFindRecipesWith.setText(selectedIngredient==null
							? "Find recipes with specified input"
							: String.format("Find recipes with \"%s\" as input", selectedIngredient.getName()));
				});
			}
		}
	}
	
	private static class RefinerDataModel extends DataModel<String> {
		
		RefinerDataModel(boolean wasEdited, int[] tempWrongRecipes) {
			super(Type.Refiner, wasEdited, tempWrongRecipes);
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
	
	private static class NutrientProcessorDataModel extends DataModel<Integer> {
		
		NutrientProcessorDataModel(boolean wasEdited, int[] tempWrongRecipes) {
			super(Type.NutrientProcessor, wasEdited, tempWrongRecipes);
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
			private final IngredientType type;
			private final String typeStr;
			private String nameDE;
			private String nameEN;
			private String desc;
		
			public Ingredient(int rawRowIndex, int ingredientIndex, String type, String nameDE, String nameEN, String desc) {
				this.rawRowIndex = rawRowIndex;
				this.ingredientIndex = ingredientIndex;
				this.typeStr = type;
				
				IngredientType type_;
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
	
	private static abstract class DataModel<IDType extends Comparable<IDType>> {
		
		private RecipeAnalyser gui = null;

		private IngredientsTableModel     ingredientsTableModel = null;
		private RecipesTableModel         recipesTableModel = null;
		private RawdataModel              rawdataModel = null;
		
		private Vector<String[]> rawIngredientsData = null;
		private Vector<String[]> rawRecipesData = null;
		
		private Vector<Recipe> recipes = null;
		private HashSet<IDType> producible = new HashSet<>();
		private HashSet<IDType> inStock = new HashSet<>();
		private StockListener stockListener = null;
		
		enum Type { NutrientProcessor, Refiner }
		private Type type;
		private boolean wasEdited;
		private boolean hasUnsavedChanges;

		private ServiceFunctions serviceFunctions;
		private int[] tempWrongRecipes;

		
		DataModel(Type type, boolean wasEdited, int[] tempWrongRecipes) {
			this.type = type;
			this.wasEdited = wasEdited;
			this.hasUnsavedChanges = false;
			this.tempWrongRecipes = tempWrongRecipes;
			this.serviceFunctions = new ServiceFunctions();
		}

		public void updateAfterLanguageChange() {
			recipesTableModel.fireTableColumnUpdate(RecipesTableColumnID.Input1);
			recipesTableModel.fireTableColumnUpdate(RecipesTableColumnID.Input2);
			recipesTableModel.fireTableColumnUpdate(RecipesTableColumnID.Input3);
			recipesTableModel.fireTableColumnUpdate(RecipesTableColumnID.Output);
		}

		public void setGui(RecipeAnalyser gui) {
			this.gui = gui;
		}

		public static DataModel<?> create(Type type) {
			return create(type, false, null);
		}
		public static DataModel<?> create(Type type, boolean wasEdited, int[] tempWrongRecipes) {
			if (type != null)
				switch (type) {
				case NutrientProcessor: return new NutrientProcessorDataModel(wasEdited, tempWrongRecipes);
				case Refiner          : return new           RefinerDataModel(wasEdited, tempWrongRecipes);
				}
			return new NutrientProcessorDataModel(wasEdited, tempWrongRecipes); // no config
		}

		public static DataModel<?> readDataCfgFromZIP(ZipFile zipin, String entryName) throws IOException {
			ZipEntry entry = zipin.getEntry(entryName);
			
			boolean wasEdited = false;
			Type type = null;
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
			
			return create(type, wasEdited, wrongRecipes);
		}
		
		private static String getValueStr(String line, String prefix) {
			if (line.startsWith(prefix))
				return line.substring(prefix.length());
			return null;
		}
		
		public void writeDataCfgToZIP(ZipOutputStream zipout, PrintWriter out, String entryName) throws IOException {
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

		public void toggleClickedRecipeIsWrong() {
			if (recipesTableModel==null) return;
			if (recipesTableModel.clickedRecipe==null) return;
			recipesTableModel.clickedRecipe.isWrong = !recipesTableModel.clickedRecipe.isWrong;
			checkInputOutput();
			updateProducibility();
			gui.recipesTable.repaint();
			gui.ingredientsTable.repaint();
		}

		public String getInStockIngredients() {
			Iterable<String> iterable = () -> inStock.stream().sorted().map(id->id.toString()).iterator();
			return String.join(",", iterable);
		}

		public void setInStockIngredients(String str) {
			inStock.clear();
			if (str==null) return;
			String[] parts = str.split(",");
			for (String p:parts) {
				IDType id = parseID(p);
				if (id!=null) inStock.add(id);
			}
		}
		
		protected abstract IDType parseID(String str);

		public void forEachRecipe(Consumer<Recipe> consumer) {
			for (Recipe recipe:recipes) {
				if (!recipe.isWrong)
					consumer.accept(recipe);
			}
		}

		public boolean forEachRecipe(BiFunction<Boolean,Boolean,Boolean> merge, boolean initialResult, Predicate<Recipe> consumer) {
			boolean result = initialResult;
			for (Recipe recipe:recipes) {
				if (!recipe.isWrong)
					result = merge.apply(result, consumer.test(recipe)) ;
			}
			return result;
		}
		
		public interface StockListener {
			void stockHasChanged(String inStockIngredients);
		}
		
		public void setStockListener(StockListener stockListener) {
			this.stockListener = stockListener;
		}
		
		private void setInStock(boolean isInStock, IDType id) {
			if (isInStock) inStock.add(id);
			else inStock.remove(id);
			stockListener.stockHasChanged(getInStockIngredients());
		}

		private boolean isInStock(IDType id) {
			return inStock.contains(id);
		}

		private boolean isProducible(IDType id) {
			return producible.contains(id);
		}

		private void updateProducibility() {
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

		public void parseIngredientsTable(Vector<String[]> rawTabTable) {
			rawIngredientsData = rawTabTable;
			parseIngredientsTable();
		}

		private void parseIngredientsTable() {
			if (rawIngredientsData==null) return;
			
			rawdataModel = new RawdataModel(rawIngredientsData);
			gui.rawIngredientsTable.setModel(rawdataModel);
			
			try {
				Vector<Ingredient> ingredients = parseIngredients();
				ingredientsTableModel = new IngredientsTableModel(ingredients);
				gui.ingredientsTable.setCellRendererForAllColumns(new IngredientsTableRenderer(), true);
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

		public void parseRecipesTable(Vector<String[]> rawTabTable) {
			rawRecipesData = rawTabTable;
			parseRecipesTable();
		}

		private void parseRecipesTable() {
			if (rawRecipesData==null) return;
			
			gui.rawRecipesTable.setModel(new RawdataModel(rawRecipesData));
			
			try {
				recipes = parseRecipes();
				recipesTableModel = new RecipesTableModel();
				gui.recipesTable.setCellRendererForAllColumns(new RecipesTableRenderer(), true);
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
		
		private class ServiceFunctions {

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

			private void findRecipesWith(Ingredient ingredient) {
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

			private void findRecipeChains(Ingredient ingredient) {
				if (ingredient==null) return;
				if (recipes==null) return;
				
				Vector<Consumer<PrintStream>> variants = new Vector<>(Arrays.asList(
					out->{
						RecipeChainFinder recipeChainFinder = new RecipeChainFinder(ingredient,getAllProducibleRecipes());
						recipeChainFinder.search();
						recipeChainFinder.printTree(out);
					},
					out->{
						RecipeChainFinder2 recipeChainFinder = new RecipeChainFinder2(ingredient,getAllProducibleRecipes());
						recipeChainFinder.search();
						recipeChainFinder.printTree(out);
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

		private class RecipeChainFinder2 {
			
			private DataModel<IDType>.Ingredient finalOutput;
			private HashMap<IDType, HashSet<InputValueCombination>> allProdRecipe;
			private HashMap<IDType, HashSet<InputValueCombination>> neededRecipes;
			private HashMap<IDType, Integer> ingredientOrder;

			public RecipeChainFinder2(Ingredient finalOutput, HashMap<IDType, HashSet<InputValueCombination>> allProdRecipe) {
				this.finalOutput = finalOutput;
				this.allProdRecipe = allProdRecipe;
				this.neededRecipes = new HashMap<>();
				this.ingredientOrder = new HashMap<>();
			}
			
			public void search() {
				neededRecipes.clear();
				ingredientOrder.clear();
				addAllRecipesFor(finalOutput);
				setOrder(finalOutput,0);
			}
			
			private void setOrder(Ingredient ingredient, int newOrderIndex) {
				if (ingredient==null) return;
				IDType id = ingredient.getID();
				Integer oldOrderIndex = ingredientOrder.get(id);
				if (oldOrderIndex==null || oldOrderIndex>newOrderIndex) {
					ingredientOrder.put(id,newOrderIndex);
					HashSet<InputValueCombination> recipes = neededRecipes.get(id);
					if (recipes!=null)
						for (InputValueCombination r:recipes)
							r.forEach(ingID->setOrder(getIngredient(ingID),newOrderIndex+1));
				}
			}

			private void addAllRecipesFor(Ingredient ingredient) {
				if (ingredient==null) return;
				IDType id = ingredient.getID();
				if (neededRecipes.containsKey(id)) return;
				if (isInStock(id)) return;
				
				HashSet<InputValueCombination> recipes = allProdRecipe.get(id);
				if (recipes!=null) {
					neededRecipes.put(id, recipes);
					for (InputValueCombination r:recipes)
						r.forEach(ingID->addAllRecipesFor(getIngredient(ingID)));
				}
			}

			public void printTree(PrintStream out) {
				out.printf("Possible Recipe Chains for \"%s\"%n", getIngredientName(finalOutput.getID()));
				
				Vector<IDType> sortedIDs = new Vector<>(neededRecipes.keySet());
				sortedIDs.sort(Comparator.<IDType,Integer>comparing(ingredientOrder::get,Comparator.nullsLast(Comparator.naturalOrder())));
				
				String indent;
				HashSet<InputValueCombination> recipes;
				for (IDType id:sortedIDs) {
					
					Integer orderIndex = ingredientOrder.get(id);
					recipes = neededRecipes.get(id);
					
					out.printf("    [%s] %s", orderIndex, getIngredientName(id));
					if (recipes.size()==1) { indent = " "; }
					else { out.println(); indent = "          "; }
					
					Vector<InputValueCombination> sortedRecipes = new Vector<>(recipes);
					sortedRecipes.sort(null);
					for (InputValueCombination r:sortedRecipes)
						out.printf("%s<-- %s%n", indent, r.toString());
				}
				out.printf("<end>%n");
			}
		}

		private class RecipeChainFinder {
			
			private Ingredient finalOutput;
			private HashMap<IDType, HashSet<InputValueCombination>> allProdRecipe;
			private RecipeOutput baseRecipeOutput;
		
			public RecipeChainFinder(Ingredient finalOutput, HashMap<IDType, HashSet<InputValueCombination>> allProdRecipe) {
				this.finalOutput = finalOutput;
				this.allProdRecipe = allProdRecipe;
			}

			public void search() {
				baseRecipeOutput = new RecipeOutput(null,this.finalOutput);
			}
			
			public void printTree(PrintStream out) {
				out.printf("Possible Recipe Chains for \"%s\"%n", getIngredientName(finalOutput.getID()));
				baseRecipeOutput.printTree(out,"      ","      ");
				out.printf("<end>%n");
			}
		
			private class RecipeOutput {
		
				private final AllowedRecipe parentRecipe;
				private final Ingredient output;
				private final IDType outputID;
				private final Vector<AllowedRecipe> allowedRecipes;
				private final boolean isBaseInput;
		
				public RecipeOutput(AllowedRecipe parentRecipe, Ingredient output) {
					this.parentRecipe = parentRecipe;
					this.output = output;
					this.outputID = output.getID();
					this.allowedRecipes = new Vector<>();
					HashSet<InputValueCombination> allowed = allProdRecipe.get( outputID );
					this.isBaseInput = isInStock(outputID);
					if (!isBaseInput && !allowed.isEmpty()) {
						for (InputValueCombination recipe:allowed) {
							if (!recipeContainsParent(recipe)) {
								AllowedRecipe allowedRecipe = new AllowedRecipe(this,recipe);
								if (allowedRecipe.isExecutable)
									allowedRecipes.add(allowedRecipe);
							}
						}
					}
				}
		
				public RecipeOutput(AllowedRecipe parentRecipe, IDType unknownID) {
					this.parentRecipe = parentRecipe;
					this.output = null;
					this.outputID = unknownID;
					this.allowedRecipes = null;
					this.isBaseInput = false;
				}

				public void printTree(PrintStream out, String firstIndent, String nextIndent) {
					if (output == null) {
						out.printf("%s%s%s is unknown", firstIndent, getIngredientName(outputID));
						return;
					}
					out.printf("%s%s%s", firstIndent, getIngredientName(outputID), isBaseInput?"  [BaseInput]":"");
					if (allowedRecipes.size() == 1) {
						allowedRecipes.get(0).printTree(out, " ", nextIndent);
					} else {
						out.println();
						for (AllowedRecipe recipe:allowedRecipes )
							recipe.printTree(out, nextIndent+"      ", nextIndent+"      ");
					}
				}
		
				private boolean recipeContainsParent(InputValueCombination recipe) {
					if (recipe.contains(output.getID())) return true;
					if (parentRecipe==null) return false;
					return parentRecipe.parentRecipeOutput.recipeContainsParent(recipe);
				}
				
			}
			
			private class AllowedRecipe {
		
				private final RecipeOutput parentRecipeOutput;
				private final InputValueCombination recipe;
				private final Vector<RecipeOutput> inputs;
				private boolean isExecutable;
		
				public AllowedRecipe(RecipeOutput parentRecipeOutput, InputValueCombination recipe) {
					this.parentRecipeOutput = parentRecipeOutput;
					this.recipe = recipe;
					this.isExecutable = true;
					this.inputs = new Vector<>();// new RecipeOutput[this.recipe.values.size()];
					this.recipe.forEach(val->{
						RecipeOutput recipeOutput;
						Ingredient input = getIngredient(val);
						if (input==null) {
							inputs.add(new RecipeOutput(this,val));
						} else {
							inputs.add(recipeOutput = new RecipeOutput(this,input));
							if (recipeOutput.allowedRecipes.isEmpty() && !recipeOutput.isBaseInput)
								isExecutable = false;
						}
					});
				}
		
				public void printTree(PrintStream out, String firstIndent, String nextIndent) {
					if (inputs.size() == 1) {
						out.printf("%s<--", firstIndent);
						inputs.firstElement().printTree(out, " ", nextIndent);
					} else {
						out.printf("%s<-- %s%n", firstIndent, recipe.toString());
						for (RecipeOutput input:inputs)
							input.printTree(out, nextIndent+"      ", nextIndent+"      ");
					}
				}
			}
		
		}
		
		private abstract class Ingredient {
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

		private class Recipe {
		
			public boolean isWrong;
			private int index;
			private RecipeIngredient outputValue;
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

		private enum RecipesTableColumnID implements SimplifiedColumnIDInterface {
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
			
			RecipesTableColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				columnConfig = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			@Override public SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
		}

		private class RecipesTableModel extends SimplifiedTableModel<RecipesTableColumnID> {
		
			private Vector<RecipeRow> recipesRows;
			public RecipeRow clickedRecipeRow = null;
			public Recipe clickedRecipe = null;
		
			protected RecipesTableModel() {
				super(RecipesTableColumnID.values());
				this.recipesRows = new Vector<>();
				for (int r=0; r<recipes.size(); r++) {
					Recipe recipe = recipes.get(r);
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
				case Index   : if (recipeRow.row==0) return recipeRow.index; break;
				case Output  : if (recipeRow.row==0) return getIngredientName(recipeRow.recipe.outputValue); break;
				case OutputAm: if (recipeRow.row==0) return getAmount(recipeRow.recipe.outputValue); break;
				case Input1  : return getIngredientName(recipeRow.recipe.getInputValue(0, recipeRow.row));
				case Input2  : return getIngredientName(recipeRow.recipe.getInputValue(1, recipeRow.row));
				case Input3  : return getIngredientName(recipeRow.recipe.getInputValue(2, recipeRow.row));
				case Input1Am: return getAmount(recipeRow.recipe.getInputValue(0, recipeRow.row));
				case Input2Am: return getAmount(recipeRow.recipe.getInputValue(1, recipeRow.row));
				case Input3Am: return getAmount(recipeRow.recipe.getInputValue(2, recipeRow.row));
				}
				return null;
			}

			private Integer getAmount(RecipeIngredient ingredient) {
				if (ingredient==null) return null;
				return ingredient.amount;
			}

			@Override public void fireTableColumnUpdate(RecipesTableColumnID columnID) {
				super.fireTableColumnUpdate(columnID);
			}
		
		}

		private enum IngredientsTableColumnID implements SimplifiedColumnIDInterface {
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
			
			IngredientsTableColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
				columnConfig = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
			}
			@Override public SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
		}

		private class IngredientsTableModel extends SimplifiedTableModel<IngredientsTableColumnID> {
		
			private Vector<Ingredient> ingredients;
			private HashMap<IDType,Ingredient> ingredientsMap;
			
			private Ingredient clickedIngredient = null;
			private HashSet<IDType> highlighted = new HashSet<>();
			private boolean highlightProducible = false;
		
			protected IngredientsTableModel(Vector<Ingredient> ingredients) {
				super(IngredientsTableColumnID.values());
				this.ingredients = ingredients;
				this.ingredientsMap = new HashMap<>();
				for (Ingredient ingredient:this.ingredients)
					if (ingredient!=null)
						ingredientsMap.put(ingredient.getID(), ingredient);
			}
		
			public void setClickedIngredient(int rowM) {
				Ingredient ingredient = getIngredientAtRow(rowM);
				clickedIngredient = ingredient==null || ingredient.getName()==null ? null : ingredient;
			}

			public void forEach(Consumer<Ingredient> consumer) {
				for (Ingredient ingredient:ingredients)
					if (ingredient!=null && ingredient.getName()!=null)
						consumer.accept(ingredient);
			}

			public void forEachSelected(Consumer<Ingredient> consumer) {
				int[] selectedRows = gui.ingredientsTable.getSelectedRows();
				for (int i=0; i<selectedRows.length; i++) {
					Ingredient ingredient = ingredients.get(selectedRows[i]);
					if (ingredient!=null && ingredient.getName()!=null)
						consumer.accept(ingredient);
				}
			}

			public void forEachProducible(Consumer<Ingredient> consumer) {
				forEach(ingredient->{
					if (isProducible(ingredient.getID()))
						consumer.accept(ingredient);
				});
			}
		
			public Vector<IDType> getSelected() {
				Vector<IDType> selected = new Vector<>();
				forEachSelected(ingredient->selected.add(ingredient.getID()));
				return selected;
			}
		
			@Override public IngredientsTableColumnID getColumnID(int columnIndex) {
				return super.getColumnID(columnIndex);
			}
		
			@Override public int getRowCount() {
				return ingredients.size();
			}
		
			public int findIngredientRowIndexByName(String name) {
				for (int i=0; i<ingredients.size(); i++) {
					Ingredient ingredient = ingredients.get(i);
					if (ingredient!=null && ingredient.nameEquals(name, true))
						return i;
				}
				return -1;
			}
			
			public Ingredient findIngredientByName(String name) {
				for (int i=0; i<ingredients.size(); i++) {
					Ingredient ingredient = ingredients.get(i);
					if (ingredient!=null && ingredient.nameEquals(name, true))
						return ingredient;
				}
				return null;
			}

			public Ingredient getIngredient(IDType id) {
				return ingredientsMap.get(id);
			}
		
			public String getIngredientName(IDType id) {
				Ingredient ingredient = getIngredient(id);
				
				if (ingredient!=null) {
					String str = ingredient.getName();
					if (str!=null) return str;
				}
				
				return String.format("<%s>", id);
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
				case Index      : return !isInput ? null : ingredient.getID();
				case InStock    : return !isInput ? null : isInStock(ingredient.getID()); // ? "In Stock" : "---";
				case Producible : return !isInput ? null : isInStock(ingredient.getID()) ? "in stock" : isProducible(ingredient.getID()) ? "producible" : "----";
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
		
			@Override protected void setValueAt(Object aValue, int rowIndex, int columnIndex, IngredientsTableColumnID columnID) {
				Ingredient ingredient = getIngredientAtRow(rowIndex);
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
						setInStock(isInStock, id);
						updateProducibility();
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
					
					boolean success = rawdataModel.setCell(str, rawRowIndex, rawColumnIndex);
					if (success) {
						setValue.accept(str);
						wasEdited = true;
						hasUnsavedChanges = true; 
						gui.updateGuiAccess();
						gui.updateWindowTitle();
					}
					
					SwingUtilities.invokeLater(()->{
						fireTableCellUpdate(rowIndex, columnIndex);
					});
				}
			}

			@Override protected boolean isCellEditable(int rowIndex, int columnIndex, IngredientsTableColumnID columnID) {
				Ingredient ingredient = getIngredientAtRow(rowIndex);
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
		}

		private class RecipesTableRenderer extends DefaultTableCellRenderer {
			private static final long serialVersionUID = -8561629608671929683L;
		
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				if (recipesTableModel!=null && row<recipesTableModel.recipesRows.size()) {
					
					RecipesTableColumnID columnID = recipesTableModel.getColumnID(column);
					RecipesTableModel.RecipeRow recipeRow = recipesTableModel.recipesRows.get(row);
					
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
						bgColor = BGCOLOR_RECIPE_IS_WRONG;
						fgColor = TXTCOLOR_RECIPE_IS_WRONG;
						
					} else {
						if ((recipeRow.index&1)==0) bgColor = isProducible ? BGCOLOR_RECIPE_EVEN_PRODUCIBLE : BGCOLOR_RECIPE_EVEN;
						else                        bgColor = isProducible ? BGCOLOR_RECIPE_ODD_PRODUCIBLE : BGCOLOR_RECIPE_ODD;
						
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

			private boolean isProducible(RecipesTableModel.RecipeRow recipeRow, int i) {
				RecipeIngredient recipeIngredient = recipeRow.recipe.getInputValue(i, recipeRow.row);
				if (recipeIngredient==null) return false;
				return DataModel.this.isProducible(recipeIngredient.id) || DataModel.this.isInStock(recipeIngredient.id);
			}
			
			
		}

		private class IngredientsTableRenderer extends DefaultTableCellRenderer {
			private static final long serialVersionUID = -5822408016974497527L;
			
			CheckBoxRendererComponent checkBox;
			IngredientsTableRenderer() {
				checkBox = new CheckBoxRendererComponent();
			}
			
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				
				if (ingredientsTableModel!=null) {
					if (Boolean.class.isAssignableFrom(ingredientsTableModel.getColumnID(column).columnConfig.columnClass)) {
						component = checkBox;
						checkBox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
						checkBox.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
						checkBox.setSelected(value instanceof Boolean ? (Boolean) value : false);
						checkBox.setHorizontalAlignment(CENTER);
					}
					
					switch (ingredientsTableModel.getColumnID(column)) {
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
					
					Ingredient ingredient = ingredientsTableModel.getIngredientAtRow(row);
					if (!isSelected) {
						Color background = table.getBackground();
						if (ingredient != null && ingredient.getName() != null) {
							if (ingredientsTableModel.highlighted.contains(ingredient.getID())) background = COLOR_INGREDIENT_MARKER;
							else if (ingredientsTableModel.highlightProducible && isProducible(ingredient.getID())) background = COLOR_INGREDIENT_PRODUCIBLE;
							else if (ingredient.isOutputValue) background = COLOR_INGREDIENT_OUTPUT;
							else if (ingredient.isInputValue ) background = COLOR_INGREDIENT_INPUT;
						}
						component.setBackground(background);
					}
				}
				
				return component;
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
	
	private static class ParseException extends Exception {
		private static final long serialVersionUID = -1522718627839188116L;

		ParseException(String format, Object...args) {
			super(String.format(Locale.ENGLISH, format, args));
		}
	}

	private static class RawdataModel implements TableModel {
		
		private final Vector<TableModelListener> tableModelListeners;
		private final Vector<String[]> data;
		private final int colCount;

		RawdataModel(Vector<String[]> data) {
			this.data = data;
			int colCount_ = 0;
			for (String[] row:data)
				colCount_ = Math.max(colCount_, row.length);
			colCount = colCount_;
			tableModelListeners = new Vector<>();
		}
		
		boolean setCell(String value, int rowIndex, int columnIndex) {
			if (rowIndex<0 || data.size()<=rowIndex) return false;
			String[] row = data.get(rowIndex);
			
			if (columnIndex<0 || row.length<=columnIndex) return false;
			row[columnIndex] = value;
			
			fireCellUpdate(rowIndex, columnIndex);
			return true;
		}

		@Override public void    addTableModelListener(TableModelListener l) { tableModelListeners.   add(l); }
		@Override public void removeTableModelListener(TableModelListener l) { tableModelListeners.remove(l); }
		
		private void fireCellUpdate(int row, int column) {
			fireTableModelEvent(new TableModelEvent(this,row,row,column,TableModelEvent.UPDATE));
		}
		private void fireTableModelEvent(TableModelEvent e) {
			for (TableModelListener tml : tableModelListeners)
				tml.tableChanged(e);
		}

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
