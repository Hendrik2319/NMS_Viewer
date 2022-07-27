package net.schwarzbaer.java.games.nomanssky.recipeanalyser;

import java.awt.BorderLayout;
import java.awt.Color;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Locale;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
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
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import net.schwarzbaer.gui.Disabler;
import net.schwarzbaer.gui.FileChooser;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Gui;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Gui.TextAreaDialog;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveViewer;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveViewer.ToolWindow;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView;

public class RecipeAnalyser implements ActionListener, ToolWindow {

	static final Color COLOR_INGREDIENT_MARKER = new Color(0,213,255);
	static final Color COLOR_INGREDIENT_PRODUCIBLE = new Color(0,255,102);
	static final Color COLOR_INGREDIENT_INPUT  = new Color(255,204,153);
	static final Color COLOR_INGREDIENT_OUTPUT = new Color(204,255,0);

	static final Color BGCOLOR_RECIPE_ODD  = new Color(1.0f,1.0f,0.8f);
	static final Color BGCOLOR_RECIPE_EVEN = Color.WHITE;
	static final Color BGCOLOR_RECIPE_ODD_PRODUCIBLE  = new Color(0xCBE7A0);
	static final Color BGCOLOR_RECIPE_EVEN_PRODUCIBLE = new Color(0xBBD795);
	@SuppressWarnings("unused")
	private static final Color BGCOLOR_RECIPE_INGREDIENT_PRODUCIBLE = new Color(0x8CD744);
	static final Color BGCOLOR_RECIPE_IS_WRONG  = new Color(0xF0F0F0);
	static final Color TXTCOLOR_RECIPE_IS_WRONG = Color.GRAY;
	
	enum Lang {
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

	public static ToolWindow start(boolean standalone) {
		return new RecipeAnalyser(standalone).initialize();
	}

	final StandardMainWindow      mainwindow;
	private final Disabler<ActionCommand> disabler;
	
	private final FileChooser         fileChooser;
	final StatusFields        statusFields;
	final ResultDialogs       resultDialogs;
	
	final TableView.SimplifiedTable<DataModel.IngredientsTableColumnID> ingredientsTable;
	final TableView.SimplifiedTable<DataModel.RecipesTableColumnID>     recipesTable;
	
	final JTable                    rawIngredientsTable;
	final JTable                    rawRecipesTable;
	
	private final JCheckBoxMenuItem miHighlightProducibleInIngredientsTable;
	
	private File         dataFile;
	private DataModel<?> dataModel;
	Lang         selectedLang;
	
	RecipeAnalyser(boolean standalone) {
		dataFile = null;
		dataModel = null;
		
		mainwindow = new StandardMainWindow("Recipe Analyser", e->checkClosing(standalone));
		
		disabler = new Disabler<ActionCommand>();
		disabler.setCareFor(ActionCommand.values());
		
		fileChooser = new FileChooser("RecipeAnalyser Data File", "recipes");
		resultDialogs = new ResultDialogs(mainwindow);
		
		selectedLang = AppSettings.getInstance().getEnum(AppSettings.ValueKey.Language, Lang.En, Lang.class);
		
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
		contextMenu = recipesTable.getContextMenu();
		contextMenu.addSeparator();
		contextMenu.add(miMarkRecipeAsWrong);
		
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
		JMenu menuAnalyse = menuBar.add(new JMenu("Analyse"));
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
				AppSettings.getInstance().putEnum(AppSettings.ValueKey.Language, selectedLang);
				if (dataModel!=null) dataModel.updateAfterLanguageChange();
			}));
		}
		
		mainwindow.startGUI(contentPane,menuBar);
		AppSettings.getInstance().registerAppWindow(mainwindow);
		
		updateGuiAccess();
		updateWindowTitle();
	}
	
	@Override public Window getWindow() {
		return mainwindow;
	}

	private void checkClosing(boolean standalone) {
		if (isClosingAllowed()) {
			mainwindow.dispose();
			if (standalone)
				System.exit(0);
		}
	}

	@Override public boolean isClosingAllowed() {
		if (dataModel!=null && (dataFile==null || dataModel.hasUnsavedChanges)) {
			String[] msg = new String[] { "You have unsaved changes.","Do you really want to close this window?" };
			String title = "Are you sure?";
			return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(mainwindow, msg, title, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
		}
		return true;
	}

	private String readIngredientsInStockFromAppSettings(DataModel.Type type) {
		if (type == null) throw new IllegalArgumentException();
		
		AppSettings.ValueKey key = type.valuekeyForIngredientsInStock;
		if (key == null) throw new IllegalStateException();
		
		return AppSettings.getInstance().getString(key, null);
	}

	private void writeIngredientsInStockToAppSettings(DataModel.Type type, String str) {
		if (type == null) throw new IllegalArgumentException();
		
		AppSettings.ValueKey key = type.valuekeyForIngredientsInStock;
		if (key == null) throw new IllegalStateException();
		
		if (str != null) AppSettings.getInstance().putString(key, str);
		else             AppSettings.getInstance().remove(key);
	}
	
	private RecipeAnalyser initialize() {
		dataFile = AppSettings.getInstance().getFile(AppSettings.ValueKey.OpenDataFile, null);
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
		SetInStock, UnsetInStock,
		MarkRecipeAsWrong, FindRecipesWith, ScrollTo,
		;
	}

	class StatusFields extends JPanel {
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
					AppSettings.getInstance().putFile(AppSettings.ValueKey.OpenDataFile, dataFile);
				}
			}
			break;
			
		case SaveDataFile:
			if (dataFile!=null && dataFile.isFile()) {
				saveDataToFile(dataFile);
				AppSettings.getInstance().putFile(AppSettings.ValueKey.OpenDataFile, dataFile);
				break;
			}
			//break;
			
		case SaveDataFileAs:
			if (fileChooser.showSaveDialog(mainwindow)==FileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				if (file!=null) {
					dataFile = file;
					saveDataToFile(dataFile);
					AppSettings.getInstance().putFile(AppSettings.ValueKey.OpenDataFile, dataFile);
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
			writeIngredientsInStockToAppSettings(dataModel.type, dataModel.getInStockIngredients());
		}
	}

	void updateWindowTitle() {
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
			dataModel.setStockListener(str -> writeIngredientsInStockToAppSettings(dataModel.type, str));
			dataModel.setInStockIngredients( readIngredientsInStockFromAppSettings(dataModel.type) );
		} else {
			if (dataModel.type != type)
				throw new IllegalStateException(String.format("Can't change type of DataModel to \"%s\". It is currently set to \"%s\".", type, dataModel.type));
		}
		updateGuiAccess();
		updateWindowTitle();
	}

	void updateGuiAccess() {
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
			dataModel.setStockListener(str -> writeIngredientsInStockToAppSettings(dataModel.type, str));
			dataModel.setInStockIngredients( readIngredientsInStockFromAppSettings(dataModel.type) );
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
	
	static class ResultDialogs {
		
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
	
	static class ParseException extends Exception {
		private static final long serialVersionUID = -1522718627839188116L;

		ParseException(String format, Object...args) {
			super(String.format(Locale.ENGLISH, format, args));
		}
	}

	static class RawdataModel implements TableModel {
		
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
