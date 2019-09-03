package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
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
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.swing.BorderFactory;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
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
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.SimplifiedTable;

public class RecipeAnalyser implements ActionListener {
	private static final String RECIPE_ANALYSER_CFG = "NMS_Viewer.RecipeAnalyser.cfg";

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
	private JTable                    rawdataTable = null;
	
	private File currentOpenDataFile = null;
	private Vector<String[]> rawNamesTable   = null;
	private Vector<String[]> rawRecipesTable = null;
	
	private Vector<Recipe> recipes = null;

	private String getNameOfInputValue(Integer nameIndex) {
		if (nameIndex==null) return "";
		if (namesTableModel==null) return "<"+nameIndex+">";
		return namesTableModel.getName(nameIndex);
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

	enum ActionCommand { CopyRecipesFromClipBoard, CopyNamesFromClipBoard, OpenDataFile, SaveDataFile, SaveDataFileAs, FindConflictingRecipes, FindBasicRecipes }

	private RecipeAnalyser createGUI(boolean standalone) {
		fileChooser = new FileChooser("RecipeAnalyser Data File", "recipes");
		
		namesTable = new TableView.SimplifiedTable("RecipesTable", true, true, false);
		namesTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		DebugTableContextMenu contextMenu = namesTable.getDebugTableContextMenu();
		contextMenu.addSeparator();
		contextMenu.add(SaveViewer.createMenuItem("Find all (#) and (#,#) recipes for selected nmaes", this, ActionCommand.FindBasicRecipes));
		
		recipesTableRenderer = new RecipesTableRenderer();
		recipesTable = new TableView.SimplifiedTable("RecipesTable", true, true, false);
		recipesTable.setCellRendererForAllColumns(recipesTableRenderer, true);
		recipesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		rawdataTable = new JTable();
		rawdataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		rawdataTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		JTabbedPane tableTabs = new JTabbedPane();
		tableTabs.addTab("Names", new JScrollPane(namesTable));
		tableTabs.addTab("Recipes", new JScrollPane(recipesTable));
		tableTabs.addTab("Recipes (Raw Data)", new JScrollPane(rawdataTable));
		tableTabs.setPreferredSize(new Dimension(900,800));
		
		JPanel contentPane = new JPanel(new BorderLayout(3,3));
		contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		contentPane.add(tableTabs,BorderLayout.CENTER);
		
		JMenu menuData = new JMenu("Data");
		menuData.add(SaveViewer.createMenuItem("Get recipe data from clipBoard", this, ActionCommand.CopyRecipesFromClipBoard));
		menuData.add(SaveViewer.createMenuItem("Get table of names from clipBoard", this, ActionCommand.CopyNamesFromClipBoard));
		menuData.addSeparator();
		menuData.add(SaveViewer.createMenuItem("Read data from file ..."   , this, ActionCommand.OpenDataFile));
		menuData.add(SaveViewer.createMenuItem("Write data to file"        , this, ActionCommand.SaveDataFile));
		menuData.add(SaveViewer.createMenuItem("Write data to new file ...", this, ActionCommand.SaveDataFileAs));
		
		JMenu menuAnalyse = new JMenu("Analyse");
		menuAnalyse.add(SaveViewer.createMenuItem("Find recipes with same input but different output", this, ActionCommand.FindConflictingRecipes));
		menuAnalyse.add(SaveViewer.createMenuItem("Find all (#) and (#,#) recipes for selected nmaes", this, ActionCommand.FindBasicRecipes));
		
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(menuData);
		menuBar.add(menuAnalyse);
		
		mainwindow = new StandardMainWindow("Recipe Analyser",standalone?DefaultCloseOperation.EXIT_ON_CLOSE:DefaultCloseOperation.HIDE_ON_CLOSE);
		mainwindow.startGUI(contentPane,menuBar);
		return this;
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
			if (recipes!=null) {
				StringBuilder messages = new StringBuilder();
				HashMap<InputValueCombination,Recipe> allCombis = new HashMap<>();
				for (Recipe recipe:recipes) {
					HashSet<InputValueCombination> recipeCombis = recipe.expand();
					for (InputValueCombination combi:recipeCombis) {
						Recipe lastRecipe = allCombis.put(combi, recipe);
						if (lastRecipe!=null)
							messages.append(
									String.format(
											"Found conflicting recipes: Recipe %d (%s) and %d (%s) for inputs (%s)%n",
											recipe    .index, getNameOfInputValue(recipe    .outputValue),
											lastRecipe.index, getNameOfInputValue(lastRecipe.outputValue),
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
				int[] selectedNames = namesTableModel.getSelectedNames(namesTable);
				HashSet<InputValueCombination> allCombis = new HashSet<>();
				for (Recipe recipe:recipes) {
					HashSet<InputValueCombination> recipeCombis = recipe.expand();
					allCombis.addAll(recipeCombis);
				}
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
		}
	}

	private void readDataFromFile(File file) {
		try (ZipFile zipin = new ZipFile(file)) {
			rawNamesTable   = readTabTableFromZIP(zipin, "names"  );
			rawRecipesTable = readTabTableFromZIP(zipin, "recipes");
			parseNamesTable  (rawNamesTable  );
			parseRecipesTable(rawRecipesTable);
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
			writeTabTableToZIP(zipout, out, "names"  , rawNamesTable  );
			writeTabTableToZIP(zipout, out, "recipes", rawRecipesTable);
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
		
		rawNamesTable = rawTabTable;
		namesTableModel = new NamesTableModel(rawTabTable);
		namesTable.setModel(namesTableModel);
		if (recipesTableModel!=null)
			recipesTableModel.fireTableUpdate();
	}

	private void parseRecipesTable(Vector<String[]> rawTabTable) {
		if (rawTabTable==null) return;
		
		rawRecipesTable = rawTabTable;
		rawdataTable.setModel(new RawdataModel(rawTabTable));
		
		try {
			recipes = parseRecipes(rawTabTable);
			recipesTableModel = new RecipesTableModel(recipes);
			recipesTable.setModel(recipesTableModel);
			recipesTableRenderer.setModel(recipesTableModel);
		} catch (ParseException e) {
			e.printStackTrace();
		}
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

		@Override
		public String toString() {
			switch (values.length) {
			case 1: return String.format("%s"        , getNameOfInputValue(values[0]) );
			case 2: return String.format("%s, %s"    , getNameOfInputValue(values[0]), getNameOfInputValue(values[1]));
			case 3: return String.format("%s, %s, %s", getNameOfInputValue(values[0]), getNameOfInputValue(values[1]), getNameOfInputValue(values[2]) );
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

	private class Recipe {

		private int index;
		private int outputValue;
		private Vector<Integer> inputValues1;
		private Vector<Integer> inputValues2;
		private Vector<Integer> inputValues3;

		public Recipe(int index, int outputValue) {
			this.index = index;
			this.outputValue = outputValue;
			inputValues1 = new Vector<>();
			inputValues2 = new Vector<>();
			inputValues3 = new Vector<>();
		}

		public HashSet<InputValueCombination> expand() {
			HashSet<InputValueCombination> combis = new HashSet<>();
			Vector<Vector<Integer>> nonEmptyArrays = new Vector<>();
			if (!inputValues1.isEmpty()) nonEmptyArrays.add(inputValues1);
			if (!inputValues2.isEmpty()) nonEmptyArrays.add(inputValues2);
			if (!inputValues3.isEmpty()) nonEmptyArrays.add(inputValues3);
			if (nonEmptyArrays.size()>0)
				for (int in0:nonEmptyArrays.get(0))
					if (nonEmptyArrays.size()>1) {
						for (int in1:nonEmptyArrays.get(1))
							if (nonEmptyArrays.size()>2) {
								for (int in2:nonEmptyArrays.get(2))
									combis.add(new InputValueCombination(in0,in1,in2));
							} else
								combis.add(new InputValueCombination(in0,in1));
					} else
						combis.add(new InputValueCombination(in0));
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

	private static class ParseException extends Exception {
		private static final long serialVersionUID = -1522718627839188116L;

		ParseException(String format, Object...args) {
			super(String.format(Locale.ENGLISH, format, args));
		}
	}

	private enum NamesTableColumnID implements SimplifiedColumnIDInterface {
		Index  ("#"          , Integer.class, 20,-1, 30, 30),
		Type   ("Type"       ,  String.class, 20,-1,100,100),
		NameDE ("Name (DE)"  ,  String.class, 20,-1,150,150),
		NameEN ("Name (EN)"  ,  String.class, 20,-1,150,150),
		Desc   ("Description",  String.class, 20,-1,450,450),
		;
		
		private SimplifiedColumnConfig columnConfig;
		
		NamesTableColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
			columnConfig = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
		}
		@Override public SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
	}
	
	private static class NamesTableModel extends SimplifiedTableModel<NamesTableColumnID> {

		private Vector<String[]> rawNamesTable;
		private static final int ColType   = 0;  
		private static final int ColNameDE = 1;
		private static final int ColNameEN = 2;
		private static final int ColDesc = 3;  

		protected NamesTableModel(Vector<String[]> rawNamesTable) {
			super(NamesTableColumnID.values());
			this.rawNamesTable = rawNamesTable;
		}

		public int[] getSelectedNames(SimplifiedTable namesTable) {
			int[] selectedRows = namesTable.getSelectedRows();
			int[] selectedNames = new int[selectedRows.length];
			for (int i=0; i<selectedRows.length; i++)
				selectedNames[i] = selectedRows[i]+1;
			return selectedNames;
		}

		@Override public int getRowCount() {
			return rawNamesTable.size();
		}

		@Override public Object getValueAt(int rowIndex, int columnIndex, NamesTableColumnID columnID) {
			String[] row = rawNamesTable.get(rowIndex);
			switch (columnID) {
			case Index : return rowIndex+1;
			case Type  : return row.length>ColType  ?row[ColType  ]:"";
			case NameDE: return row.length>ColNameDE?row[ColNameDE]:"";
			case NameEN: return row.length>ColNameEN?row[ColNameEN]:"";
			case Desc  : return row.length>ColDesc  ?row[ColDesc  ]:"";
			}
			return null;
		}

		public String getName(int nameIndex) {
			int rowIndex = nameIndex-1;
			
			if (rowIndex<0 || rowIndex>=rawNamesTable.size())
				return String.format("<%d> OutOfRange(%d..%d)", nameIndex,0,rawNamesTable.size()-1);
			String[] row = rawNamesTable.get(rowIndex);
			
			if (row.length>ColNameDE && !row[ColNameDE].isEmpty())
				return row[ColNameDE];
			if (row.length>ColNameEN && !row[ColNameEN].isEmpty())
				return row[ColNameEN];
			
			return String.format("<%d>", nameIndex);
		}
	}

	private static class RecipesTableRenderer extends DefaultTableCellRenderer {
		private static final Color Color2 = new Color(1.0f,1.0f,0.8f);
		private static final Color Color1 = Color.WHITE;
		private static final long serialVersionUID = -8561629608671929683L;
		private RecipesTableModel model = null;

		public void setModel(RecipesTableModel model) {
			this.model = model;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (!isSelected && model!=null && row<model.recipesRows.size()) {
				RecipesTableModel.RecipeRow recipeRow = model.recipesRows.get(row);
				if ((recipeRow.index&1)==0)
					setBackground(Color1);
				else
					setBackground(Color2);
			}
			return component;
		}
		
		
	}

	private enum RecipesTableColumnID implements SimplifiedColumnIDInterface {
		Index  ("#"      , Integer.class, 20,-1, 30, 50),
		Output ("Output" ,  String.class, 20,-1,150,150),
		Input1 ("Input 1",  String.class, 20,-1,150,150),
		Input2 ("Input 2",  String.class, 20,-1,150,150),
		Input3 ("Input 3",  String.class, 20,-1,150,150),
		;
		
		private SimplifiedColumnConfig columnConfig;
		
		RecipesTableColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
			columnConfig = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
		}
		@Override public SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
	}

	private class RecipesTableModel extends SimplifiedTableModel<RecipesTableColumnID> {

		private Vector<RecipeRow> recipesRows;

		protected RecipesTableModel(Vector<Recipe> recipes) {
			super(RecipesTableColumnID.values());
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

		@Override public int getRowCount() { return recipesRows.size(); }

		@Override
		public Object getValueAt(int rowIndex, int columnIndex, RecipesTableColumnID columnID) {
			RecipeRow recipeRow = recipesRows.get(rowIndex);
			switch (columnID) {
			case Index : if (recipeRow.row==0) return recipeRow.index; break;
			case Output: if (recipeRow.row==0) return getNameOfInputValue(recipeRow.recipe.outputValue); break;
			case Input1: return getNameOfInputValue(recipeRow.recipe.getInputValue(0, recipeRow.row));
			case Input2: return getNameOfInputValue(recipeRow.recipe.getInputValue(1, recipeRow.row));
			case Input3: return getNameOfInputValue(recipeRow.recipe.getInputValue(2, recipeRow.row));
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
