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

	private static final Color COLOR_NAME_MARKER = new Color(0,213,255);
	private static final Color COLOR_NAME_PRODUCIBLE = new Color(0,255,102);
	private static final Color COLOR_NAME_INPUT  = new Color(255,204,153);
	private static final Color COLOR_NAME_OUTPUT = new Color(204,255,0);

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
	private TableView.SimplifiedTable namesTable = null;
	private NamesTableModel           namesTableModel = null;
	private TableView.SimplifiedTable recipesTable = null;
	private RecipesTableModel         recipesTableModel = null;
	private RecipesTableRenderer      recipesTableRenderer = null;
	private JTable                    rawNamesTable = null;
	private JTable                    rawRecipesTable = null;
	
	private JCheckBoxMenuItem miHighlightProducibleInNamesTable = null;
	
	private File currentOpenDataFile = null;
	private Vector<String[]> rawNamesData   = null;
	private Vector<String[]> rawRecipesData = null;

	private StatusFields statusFields;

	private Name getName(Integer nameIndex) {
		if (nameIndex==null) return null;
		if (namesTableModel==null) return null;
		return namesTableModel.getName(nameIndex);
	}
	private String getNameStr(Integer nameIndex) {
		if (nameIndex==null) return "";
		if (namesTableModel==null) return "<"+nameIndex+">";
		return namesTableModel.getNameStr(nameIndex);
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
		CopyNamesFromClipBoard,
		OpenDataFile,
		SaveDataFile,
		SaveDataFileAs,
		FindConflictingRecipes,
		FindBasicRecipes,
		FindCombinableInputs,
		ClearMarkersInNamesTable,
		HighlightProducibleInNamesTable,
		FindRecipes, SetInStock, UnsetInStock,
		;
	}

	private RecipeAnalyser createGUI(boolean standalone) {
		fileChooser = new FileChooser("RecipeAnalyser Data File", "recipes");
		
		NamesTableRenderer namesTableRenderer = new NamesTableRenderer();
		namesTable = new TableView.SimplifiedTable("RecipesTable", true, true, false);
		namesTable.setCellRendererForAllColumns(namesTableRenderer, true);
		namesTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		//namesTable.setDefaultEditor(Boolean.class, new DefaultCellEditor(new JCheckBox()));
		//namesTable.setDefaultEditor(Boolean.class, new DefaultCellEditor(new JComboBox<>(new Boolean[] {true, false})));
		namesTable.setDefaultEditor(Boolean.class, new DefaultCellEditor(new JComboBox<>(new String[] {"In Stock","---"})));
		
		JCheckBoxMenuItem miHighlightProducible = SaveViewer.createCheckBoxMenuItem("Highlight producible", this, ActionCommand.HighlightProducibleInNamesTable);
		JMenuItem miFindCombinableInputs = SaveViewer.createMenuItem("Mark all inputs, that are combinable (#,#) with ####", this, ActionCommand.FindCombinableInputs);
		JMenuItem miFindRecipes = SaveViewer.createMenuItem("Find recipe chain for ####", this, ActionCommand.FindRecipes);
		
		namesTable.addContextMenuInvokeListener((rowV, columnV)->{
			if (namesTableModel==null) return;
			
			int rowM = namesTable.convertRowIndexToModel(rowV);
			Name name = namesTableModel.getNameAtRow(rowM);
			String nameStr = name==null?null:name.getName();
			boolean hasName = nameStr != null;
			String nameStr2 = hasName ? ("\""+nameStr+"\"") : "<???>";
			namesTableModel.clickedName = hasName ? name : null;

			miFindCombinableInputs.setText(String.format("Mark all inputs, that are combinable (#,#) with %s", nameStr2));
			miFindCombinableInputs.setEnabled(hasName);
			
			miHighlightProducible.setSelected(namesTableModel.highlightProducible);
			
			boolean isProducible = hasName && name.isOutputValue && namesTableModel.producible.contains(name.nameIndex);
			miFindRecipes.setText(String.format("Find recipe chain for %s", nameStr2));
			miFindRecipes.setEnabled(isProducible);
		});
		
		DebugTableContextMenu contextMenu = namesTable.getDebugTableContextMenu();
		contextMenu.addSeparator();
		contextMenu.add(SaveViewer.createMenuItem("Find all (#) and (#,#) recipes for selected inputs", this, ActionCommand.FindBasicRecipes));
		contextMenu.addSeparator();
		contextMenu.add(miFindCombinableInputs);
		contextMenu.add(SaveViewer.createMenuItem("Clear markers", this, ActionCommand.ClearMarkersInNamesTable));
		contextMenu.addSeparator();
		contextMenu.add(SaveViewer.createMenuItem("Set InStock for selected inputs", this, ActionCommand.SetInStock));
		contextMenu.add(SaveViewer.createMenuItem("Unset InStock for selected inputs", this, ActionCommand.UnsetInStock));
		contextMenu.add(miHighlightProducible);
		contextMenu.add(miFindRecipes);
		
		recipesTableRenderer = new RecipesTableRenderer();
		recipesTable = new TableView.SimplifiedTable("RecipesTable", true, true, false);
		recipesTable.setCellRendererForAllColumns(recipesTableRenderer, true);
		recipesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		rawNamesTable = new JTable();
		rawNamesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		rawNamesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		rawRecipesTable = new JTable();
		rawRecipesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		rawRecipesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		JTabbedPane tableTabs = new JTabbedPane();
		tableTabs.addTab("Names", new JScrollPane(namesTable));
		tableTabs.addTab("Recipes", new JScrollPane(recipesTable));
		tableTabs.addTab("Names (Raw Data)", new JScrollPane(rawNamesTable));
		tableTabs.addTab("Recipes (Raw Data)", new JScrollPane(rawRecipesTable));
		tableTabs.setPreferredSize(new Dimension(1100,800));
		
		JPanel contentPane = new JPanel(new BorderLayout(3,3));
		contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		contentPane.add(tableTabs,BorderLayout.CENTER);
		contentPane.add(statusFields = new StatusFields(),BorderLayout.SOUTH);
		
		JMenu menuData = new JMenu("Data");
		menuData.add(SaveViewer.createMenuItem("Get recipe data from clipboard", this, ActionCommand.CopyRecipesFromClipBoard));
		menuData.add(SaveViewer.createMenuItem("Get table of names from clipboard", this, ActionCommand.CopyNamesFromClipBoard));
		menuData.addSeparator();
		menuData.add(SaveViewer.createMenuItem("Read data from file ..."   , this, ActionCommand.OpenDataFile));
		menuData.add(SaveViewer.createMenuItem("Write data to file"        , this, ActionCommand.SaveDataFile));
		menuData.add(SaveViewer.createMenuItem("Write data to new file ...", this, ActionCommand.SaveDataFileAs));
		
		miHighlightProducibleInNamesTable = SaveViewer.createCheckBoxMenuItem("Highlight producible in names table", this, ActionCommand.HighlightProducibleInNamesTable);
		JMenu menuAnalyse = new JMenu("Analyse");
		menuAnalyse.add(SaveViewer.createMenuItem("Clear markers in names table", this, ActionCommand.ClearMarkersInNamesTable));
		menuAnalyse.add(miHighlightProducibleInNamesTable);
		menuAnalyse.addSeparator();
		menuAnalyse.add(SaveViewer.createMenuItem("Find recipes with same input but different output", this, ActionCommand.FindConflictingRecipes));
		menuAnalyse.add(SaveViewer.createMenuItem("Find all (#) and (#,#) recipes for selected names", this, ActionCommand.FindBasicRecipes));
		
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
		
		case CopyNamesFromClipBoard  : parseNamesTable  (parseTabTable(SaveViewer.pasteFromClipBoard())); break;
		case CopyRecipesFromClipBoard: parseRecipesTable(parseTabTable(SaveViewer.pasteFromClipBoard())); break;
		
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
											recipe    .index, getNameStr(recipe    .outputValue),
											lastRecipe.index, getNameStr(lastRecipe.outputValue),
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
			if (namesTableModel!=null) {
				StringBuilder messages = new StringBuilder();
				Integer[] selectedNames = namesTableModel.getSelectedNames();
				HashSet<InputValueCombination> allCombis = getSetOfAllCombis();
				for (int value:selectedNames) {
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
			if (namesTableModel!=null && namesTableModel.clickedName!=null && recipesTableModel!=null) {
				HashMap<Integer,HashSet<InputValueCombination>> allProdRecipe = new HashMap<>();
				for (Integer n:namesTableModel.producible)
					allProdRecipe.put(n, new HashSet<InputValueCombination>());
				for (Recipe recipe:recipesTableModel.recipes) {
					if (namesTableModel.producible.contains(recipe.outputValue)) {
						HashSet<InputValueCombination> allowedCombis = recipe.expand(namesTableModel.producible::contains);
						allProdRecipe.get(recipe.outputValue).addAll(allowedCombis);
					}
				}
				//for (Integer output:allProdRecipe.keySet())
				//	for (InputValueCombination combi:allProdRecipe.get(output))
				//		System.out.printf("%s <-- %s%n", getNameStr(output), combi.toString());
				
				//HashSet<InputValueCombination> allowedCombis = allProdRecipe.get( namesTableModel.clickedName.nameIndex );
				RecipeChainFinder recipeChainFinder = new RecipeChainFinder(namesTableModel.clickedName.nameIndex,allProdRecipe,this::getNameStr);
				recipeChainFinder.printTree(System.out);
				//recipeChainFinder.getShortestRecipeChain();
				
				
			}
			break;
			
		case FindCombinableInputs:
			if (namesTableModel!=null && namesTableModel.clickedName!=null) {
				HashSet<InputValueCombination> allCombis = getSetOfAllCombis();
				namesTableModel.highlighted.clear();
				namesTableModel.forEachName(name -> {
					InputValueCombination combi = new InputValueCombination(namesTableModel.clickedName.nameIndex,name.nameIndex);
					if (allCombis.contains(combi))
						namesTableModel.highlighted.add(name.nameIndex);
				});
				namesTableModel.fireTableUpdate();
			}
			break;
			
		case ClearMarkersInNamesTable:
			if (namesTableModel!=null) {
				namesTableModel.highlighted.clear();
				namesTableModel.fireTableUpdate();
			}
			break;
			
		case HighlightProducibleInNamesTable:
			if (namesTableModel!=null) {
				namesTableModel.highlightProducible = !namesTableModel.highlightProducible;
				miHighlightProducibleInNamesTable.setSelected(namesTableModel.highlightProducible);
				namesTableModel.fireTableUpdate();
			}
			break;
			
		case SetInStock:
			if (namesTableModel!=null) {
				namesTableModel.forEachSelectedName(name->name.isInStock = true);
				namesTableModel.updateProducibility();
				namesTableModel.fireTableUpdate();
			}
			break;
			
		case UnsetInStock:
			if (namesTableModel!=null) {
				namesTableModel.forEachSelectedName(name->name.isInStock = false);
				namesTableModel.updateProducibility();
				namesTableModel.fireTableUpdate();
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

	private void readDataFromFile(File file) {
		try (ZipFile zipin = new ZipFile(file)) {
			rawNamesData   = readTabTableFromZIP(zipin, "names"  );
			rawRecipesData = readTabTableFromZIP(zipin, "recipes");
			parseNamesTable  (rawNamesData  );
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
			writeTabTableToZIP(zipout, out, "names"  , rawNamesData  );
			writeTabTableToZIP(zipout, out, "recipes", rawRecipesData);
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

	private void parseNamesTable(Vector<String[]> rawTabTable) {
		if (rawTabTable==null) return;
		
		rawNamesData = rawTabTable;
		rawNamesTable.setModel(new RawdataModel(rawTabTable));
		
		Name[] nameIndexes = new Name[rawTabTable.size()];
		Arrays.fill(nameIndexes, null);
		try {
			Vector<Name> names = parseNames(rawTabTable, nameIndexes);
			namesTableModel = new NamesTableModel(names, nameIndexes);
			namesTable.setModel(namesTableModel);
			if (recipesTableModel!=null) recipesTableModel.fireTableUpdate();
			checkInputOutput();
			namesTableModel.updateProducibility();
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
			if (namesTableModel!=null)
				namesTableModel.updateProducibility();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	private void checkInputOutput() {
		if (recipesTableModel==null) return;
		if (namesTableModel==null) return;
		
		for (Name name:namesTableModel.names) {
			if (name!=null) {
				name.isOutputValue = false;
				name.isInputValue = false;
			}
		}
		
		Name name;
		for (Recipe recipe:recipesTableModel.recipes) {
			
			name = getName(recipe.outputValue);
			if (name!=null) name.isOutputValue = true;
			
			for (Vector<Integer> inputValues:recipe.inputValues) {
				for (Integer inputValue:inputValues) {
					name = getName(inputValue);
					if (name!=null) name.isInputValue = true;
				}
			}
		}
	}

	private Vector<Name> parseNames(Vector<String[]> rawTabTable, Name[] nameIndexes) throws ParseException {
		Vector<Name> names = new Vector<>();
		Name lastName = null;
		for (int row=0; row<rawTabTable.size(); row++) {
			String[] rowData = rawTabTable.get(row);
			
			String type   = rowData.length>0?rowData[0]:"";
			String nameDE = rowData.length>1?rowData[1]:"";
			String nameEN = rowData.length>2?rowData[2]:"";
			String desc   = rowData.length>3?rowData[3]:"";
			
			Name name = null;
			if (!type.isEmpty() || !nameDE.isEmpty() || !nameEN.isEmpty() || !desc.isEmpty())
				name = new Name(row+1,type,nameDE,nameEN,desc);
			
			nameIndexes[row] = name;
			if (name!=null || lastName!=null)
				names.add(lastName = name);
		}
		return names;
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

	private static class Name {

		private enum NameType {
			IM, IP0, IP1, IP2A, IP2B, IR0, IR1, IR2, IRM, IRX, IAF, IAK, BC, BH, EI, EP
		}

		private int nameIndex;
		private NameType type;
		private String typeStr;
		private String nameDE;
		private String nameEN;
		private String desc;
		private boolean isInputValue = false;
		private boolean isOutputValue = false;
		private boolean isInStock = false;

		public Name(int nameIndex, String type, String nameDE, String nameEN, String desc) {
			this.nameIndex = nameIndex;
			this.typeStr = type;
			try { this.type = NameType.valueOf(type); }
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
			String nameStr = getName();
			return String.format("Name [%d] <%s> \"%s\" %s%s%s", nameIndex, typeStr, nameStr==null?"":nameStr, isInputValue?"I":"-", isOutputValue?"O":"-", isInStock?"S":"-");
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
			return "Recipe " + index + " (" + getNameStr(outputValue) + ")";
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
			case 1: return String.format("%s"        , getNameStr(values[0]) );
			case 2: return String.format("%s, %s"    , getNameStr(values[0]), getNameStr(values[1]));
			case 3: return String.format("%s, %s, %s", getNameStr(values[0]), getNameStr(values[1]), getNameStr(values[2]) );
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

	private class NamesTableRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = -5822408016974497527L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			
			if (namesTableModel!=null) {
				switch (namesTableModel.getColumnID(column)) {
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
				
				Name name = namesTableModel.getNameAtRow(row);
				if (!isSelected) {
					if (name!=null && name.getName()!=null) {
						if      (namesTableModel.highlighted.contains(name.nameIndex)) setBackground(COLOR_NAME_MARKER);
						else if (namesTableModel.highlightProducible && namesTableModel.producible .contains(name.nameIndex)) setBackground(COLOR_NAME_PRODUCIBLE);
						else if (name.isOutputValue) setBackground(COLOR_NAME_OUTPUT);
						else if (name.isInputValue ) setBackground(COLOR_NAME_INPUT);
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

	private enum NamesTableColumnID implements SimplifiedColumnIDInterface {
		Index      ("#"          , Integer.class, 20,-1, 30, 30),
		Type       ("Type"       ,  String.class, 20,-1,100,100),
		InStock    ("In Stock"   , Boolean.class, 20,-1, 60, 60),
		Producible ("Producible" ,  String.class, 20,-1, 60, 60),
		NameDE     ("Name (DE)"  ,  String.class, 20,-1,150,150),
		NameEN     ("Name (EN)"  ,  String.class, 20,-1,150,150),
		Description("Description",  String.class, 20,-1,450,450),
		;
		
		private SimplifiedColumnConfig columnConfig;
		
		NamesTableColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
			columnConfig = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
		}
		@Override public SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
	}
	
	private class NamesTableModel extends SimplifiedTableModel<NamesTableColumnID> {

		private Vector<Name> names;
		private Name[] nameIndexes;
		private Name clickedName = null;
		private HashSet<Integer> highlighted = new HashSet<>();
		private HashSet<Integer> producible = new HashSet<>();
		private boolean highlightProducible = false;

		protected NamesTableModel(Vector<Name> names, Name[] nameIndexes) {
			super(NamesTableColumnID.values());
			this.names = names;
			this.nameIndexes = nameIndexes;
		}

		private void updateProducibility() {
			producible.clear();
			forEachName(name->{
				if (name.isInStock) producible.add(name.nameIndex);
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

		public void forEachName(Consumer<Name> consumer) {
			for (Name name:names)
			 if (name!=null && name.getName()!=null)
				consumer.accept(name);
		}

		public void forEachSelectedName(Consumer<Name> consumer) {
			int[] selectedRows = namesTable.getSelectedRows();
			for (int i=0; i<selectedRows.length; i++) {
				Name name = names.get(selectedRows[i]);
				if (name!=null && name.getName()!=null)
					consumer.accept(name);
			}
		}

		public Integer[] getSelectedNames() {
			Vector<Integer> selectedNames = new Vector<>();
			forEachSelectedName((Name name)->selectedNames.add(name.nameIndex));
			return selectedNames.toArray(new Integer[0]);
		}

		@Override public NamesTableColumnID getColumnID(int columnIndex) {
			return super.getColumnID(columnIndex);
		}

		@Override public int getRowCount() {
			return names.size();
		}

		public Name getName(int nameIndex) {
			int listIndex = nameIndex-1;
			if (listIndex<0 || listIndex>=nameIndexes.length) return null;
			return nameIndexes[listIndex];
		}

		public String getNameStr(int nameIndex) {
			int listIndex = nameIndex-1;
			
			if (listIndex<0 || listIndex>=nameIndexes.length)
				return String.format("<%d> OutOfRange(%d..%d)", nameIndex,1,nameIndexes.length);
			
			Name name = nameIndexes[listIndex];
			if (name!=null) {
				String str = name.getName();
				if (str!=null) return str;
			}
			
			return String.format("<%d>", nameIndex);
		}

		public Name getNameAtRow(int rowIndex) {
			if (0<=rowIndex && rowIndex<names.size())
				return names.get(rowIndex);
			return null;
		}

		@Override public Object getValueAt(int rowIndex, int columnIndex, NamesTableColumnID columnID) {
			Name name = getNameAtRow(rowIndex);
			boolean isInput = name!=null && name.getName()!=null;
			switch (columnID) {
			case Index      : return isInput ? name.nameIndex : "";
			case Type       : return name==null?"":name.type==null?name.typeStr:name.type;
			case InStock    : return isInput ? (name.isInStock?"In Stock":"---") : null;
			case Producible : return !isInput ? "" : producible.contains(name.nameIndex)?"producible":"----";
			case NameDE     : return name==null?"":name.nameDE;
			case NameEN     : return name==null?"":name.nameEN;
			case Description: return name==null?"":name.desc;
			}
			return null;
		}

		@Override protected void setValueAt(Object aValue, int rowIndex, int columnIndex, NamesTableColumnID columnID) {
			Name name = getNameAtRow(rowIndex);
			if (!(name!=null && name.getName()!=null)) return;
			
			switch (columnID) {
			case InStock:
				if (aValue instanceof Boolean) name.isInStock = (Boolean) aValue;
				if (aValue instanceof String ) name.isInStock = "In Stock".equals((String) aValue);
				updateProducibility();
				fireTableUpdate();
				break;
			default: break;
			}
		}

		@Override protected boolean isCellEditable(int rowIndex, int columnIndex, NamesTableColumnID columnID) {
			Name name = getNameAtRow(rowIndex);
			boolean isInput = name!=null && name.getName()!=null;
			if (columnID==NamesTableColumnID.InStock) return isInput;
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
			case Output: if (recipeRow.row==0) return getNameStr(recipeRow.recipe.outputValue); break;
			case Input1: return getNameStr(recipeRow.recipe.getInputValue(0, recipeRow.row));
			case Input2: return getNameStr(recipeRow.recipe.getInputValue(1, recipeRow.row));
			case Input3: return getNameStr(recipeRow.recipe.getInputValue(2, recipeRow.row));
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
