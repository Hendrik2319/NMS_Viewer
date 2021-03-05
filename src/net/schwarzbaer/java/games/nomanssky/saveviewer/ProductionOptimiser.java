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
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.CompoundBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import net.schwarzbaer.gui.Disabler;
import net.schwarzbaer.gui.FileChooser;
import net.schwarzbaer.gui.ProgressDialog;
import net.schwarzbaer.gui.ProgressDialog.CancelListener;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.gui.StandardMainWindow.DefaultCloseOperation;
import net.schwarzbaer.gui.Tables.CheckBoxRendererComponent;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.gui.Tables.SimplifiedColumnIDInterface;
import net.schwarzbaer.gui.Tables.SimplifiedTableModel;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Gui.ToolbarIcons;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView;

class ProductionOptimiser implements ActionListener {
	
	private static final Color COLOR_INFINITE_PRODUCT = Color.RED;
	private static final Color COLOR_INFINITE_INPUT = new Color(0xe0e0e0);
	private static final Color[] COLOR_PRODUCT_LEVEL = createColors(3,new Color(255,204,153),new Color(204,255,0));

	public static void main(String[] args) {
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		Gui.loadToolbarIcons();
		start(true);
	}
	
	static void start(boolean standalone) {
		new ProductionOptimiser()
			.readConfig()
			.writeConfig()
			.createGUI(standalone)
			.openLastDataFile();
	}

	private File dataFile = null;
	private StandardMainWindow mainwindow = null;
	private FileChooser fileChooser = null;
	private Disabler<ActionCommand> disabler = null;
	
	private TableView.SimplifiedTable<BaseInputsTableColumnID> baseInputsTable = null;
	private TableView.SimplifiedTable<ProductsTableColumnID>   productsTable   = null;
	private BaseInputsTableModel baseInputsTableModel = null;
	private   ProductsTableModel   productsTableModel = null;
	private InputList inputList = null;
	private JTextArea outputTextArea = null;
	private JComboBox<Result> resultList = null;
	
	private Result selectedResult = null;

	ProductionOptimiser() {
		
	}

	enum ActionCommand {
		ClearData,
		OpenDataFile,
		SaveDataFile,
		SaveDataFileAs,
		FindMostValuableProduction,
		;
	}

	private ProductionOptimiser createGUI(boolean standalone) {
		fileChooser = new FileChooser("ProductionOptimiser Data File", "producttree");
		
		disabler = new Disabler<ActionCommand>();
		disabler.setCareFor(ActionCommand.values());
		
		inputList = new InputList();
		
		baseInputsTableModel = new BaseInputsTableModel();
		baseInputsTable = new TableView.SimplifiedTable<>("BaseInputsTable", baseInputsTableModel, true, true, false);
		baseInputsTable.setCellRendererForAllColumns(baseInputsTableModel.createCellRenderer(), true);
		JCheckBox rendererCheckBox = new JCheckBox();
		rendererCheckBox.setHorizontalAlignment(JCheckBox.CENTER);
		baseInputsTable.setDefaultEditor(Boolean.class, new DefaultCellEditor(rendererCheckBox));
		JScrollPane baseInputsTableScrollPane = new JScrollPane(baseInputsTable);
		baseInputsTableScrollPane.setBorder(createCompoundBorder("Base Inputs") );
		baseInputsTableScrollPane.setPreferredSize(new Dimension(getSumofColWidths(BaseInputsTableColumnID.values())+50,200));
		
		productsTableModel = new ProductsTableModel();
		productsTable = new TableView.SimplifiedTable<>("ProductsTable", productsTableModel, true, true, false);
		productsTable.setCellRendererForAllColumns(productsTableModel.createCellRenderer(), true);
		productsTable.setDefaultEditor(Input.class, new DefaultCellEditor(new JComboBox<Input>(inputList)));
		JScrollPane productsTableScrollPane = new JScrollPane(productsTable);
		productsTableScrollPane.setBorder(createCompoundBorder("Products"));
		productsTableScrollPane.setPreferredSize(new Dimension(getSumofColWidths(ProductsTableColumnID.values())+50,300));
		
		resultList = new JComboBox<Result>();
		resultList.addActionListener(e->showResult((Result)resultList.getSelectedItem()));
		resultList.setEnabled(false);
		
		JPanel resultListPanel = new JPanel(new BorderLayout(3,3));
		resultListPanel.setBorder(BorderFactory.createTitledBorder("Results"));
		resultListPanel.add(resultList);
		
		outputTextArea = new JTextArea();
		
		JScrollPane outputScrollPane = new JScrollPane(outputTextArea);
		outputScrollPane.setBorder(createCompoundBorder("Output"));
		outputScrollPane.setPreferredSize(new Dimension(200,200));
		
		JPanel outputPanel = new JPanel(new BorderLayout(3,3));
		outputPanel.add(outputScrollPane,BorderLayout.CENTER);
		outputPanel.add(resultListPanel,BorderLayout.NORTH);
		
		JSplitPane tablePane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		tablePane.setTopComponent(baseInputsTableScrollPane);
		tablePane.setBottomComponent(productsTableScrollPane);
		tablePane.setResizeWeight(0.5);
		
		JSplitPane contentPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		contentPane.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
		contentPane.setLeftComponent(tablePane);
		contentPane.setRightComponent(outputPanel);
		contentPane.setResizeWeight(0.8);
		
		JMenu menuData = new JMenu("Data");
		menuData.add(Gui.createMenuItem("Clear data"                , this, disabler, ActionCommand.ClearData     , ToolbarIcons.Delete));
		menuData.add(Gui.createMenuItem("Read data from file ..."   , this, disabler, ActionCommand.OpenDataFile  , ToolbarIcons.Open));
		menuData.add(Gui.createMenuItem("Write data to file"        , this, disabler, ActionCommand.SaveDataFile  , ToolbarIcons.Save));
		menuData.add(Gui.createMenuItem("Write data to new file ...", this, disabler, ActionCommand.SaveDataFileAs, ToolbarIcons.SaveAs));
		
		JMenu menuAnalyse = new JMenu("Analyse");
		menuAnalyse.add(Gui.createMenuItem("Find production with most valuable products", this, disabler, ActionCommand.FindMostValuableProduction));
		
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(menuData);
		menuBar.add(menuAnalyse);
		
		mainwindow = new StandardMainWindow("Production Optimiser",standalone?DefaultCloseOperation.EXIT_ON_CLOSE:DefaultCloseOperation.HIDE_ON_CLOSE);
		mainwindow.startGUI(contentPane,menuBar);
		
		updateGuiAccess();
		updateWindowTitle();
		return this;
	}

	private void showResult(Result result) {
		selectedResult = result;
		baseInputsTableModel.fireTableUpdate();
		productsTableModel.fireTableUpdate();
		if (selectedResult!=null) {
			Gui.log_ln("Avail: %s", selectedResult.avail);
			Gui.log_ln("Amounts of Level 1 products at start: %s", selectedResult.level1Amounts);
			Gui.log_ln("Consumed Base Inputs: %s", selectedResult.consumedBaseInputs);
			Gui.log_ln("Stored Products: %s", selectedResult.storedProducts);
		}
	}

	private <ColumnID extends SimplifiedColumnIDInterface> int getSumofColWidths(ColumnID[] values) {
		int width = 0;
		for (ColumnID colID:values)
			width += colID.getColumnConfig().prefWidth;
		return width;
	}

	private CompoundBorder createCompoundBorder(String title) {
		return BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(title),
				BorderFactory.createLineBorder(Color.LIGHT_GRAY)
		);
	}

	private void updateWindowTitle() {
		String str = "";
		if (dataFile==null) str += "  -  <unsaved>";
		else str += "  -  "+dataFile.getName();
		mainwindow.setTitle("Production Optimiser"+str);
	}

	private void updateGuiAccess() {
		disabler.setEnable(ac->{
			switch (ac) {
			case OpenDataFile:
				return true;
			case ClearData:
			case SaveDataFile:
			case SaveDataFileAs:
				return true;
				//return dataFile!=null;
			case FindMostValuableProduction:
				return true;
			}
			return null;
		});
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		switch(ActionCommand.valueOf(e.getActionCommand())) {
		
		case ClearData:
			dataFile = null;
			baseInputsTableModel.clear(true);
			productsTableModel.clear(true);
			inputList.update();
			
			updateGuiAccess();
			updateWindowTitle();
			break;
		case OpenDataFile:
			if (fileChooser.showOpenDialog(mainwindow)==FileChooser.APPROVE_OPTION) {
				dataFile = fileChooser.getSelectedFile();
				readDataFromFile();
				writeConfig();
				updateGuiAccess();
				updateWindowTitle();
			}
			break;
		case SaveDataFile:
			if (dataFile!=null && dataFile.isFile()) {
				saveDataToFile();
				writeConfig();
				updateWindowTitle();
				break;
			}
			//break;
		case SaveDataFileAs:
			if (fileChooser.showSaveDialog(mainwindow)==FileChooser.APPROVE_OPTION) {
				dataFile = fileChooser.getSelectedFile();
				saveDataToFile();
				writeConfig();
				updateWindowTitle();
			}
			break;
			
		case FindMostValuableProduction: {
			SaveViewer.runWithProgressDialog(mainwindow, "title", pd->{
				Vector<Result> results = new MostValuableProduction(pd).find();
				if (results!=null) {
					resultList.setModel(new DefaultComboBoxModel<Result>(results));
					resultList.setSelectedItem(null);
					resultList.setEnabled(true);
					Gui.log_ln("Most Valuable Productions:");
					for (int i=0; i<results.size(); i++) {
						Result res = results.get(i);
						Gui.log_ln("   [%02d] %d", i+1, res.avail);
					}
				}
			});
		} break;
		}
	}

	private ProductionOptimiser readConfig() {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(FileExport.FILE_CFG_PRODUCTION_OPTIMISER), StandardCharsets.UTF_8))) {
			String line;
			while ( (line=in.readLine())!=null ) {
				if (line.startsWith("OpenProductionTree=")) {
					String valueStr = line.substring("OpenProductionTree=".length());
					dataFile = new File( valueStr );
					if (!dataFile.isFile()) dataFile=null;
				}
//				if (line.equals("SaveInStockIngredients")) {
//					saveInStockIngredients = true;
//				}
//				if (line.startsWith("IngredientsInStock.")) {
//					String valueStr = line.substring("IngredientsInStock.".length());
//					int pos = valueStr.indexOf('=');
//					String typeStr;
//					if (pos<0) {
//						typeStr = valueStr;
//						valueStr = "";
//					} else {
//						typeStr = valueStr.substring(0,pos);
//						valueStr = valueStr.substring(pos+1);
//						try { ingredientsInStock.put(DataModel.Type.valueOf(typeStr),valueStr); }
//						catch (Exception e) {}
//					}
//				}
			}
		} catch (FileNotFoundException e) {
			// Is Ok :)
		} catch (IOException e) {
			e.printStackTrace();
		}
		return this;
	}

	private ProductionOptimiser writeConfig() {
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(FileExport.FILE_CFG_PRODUCTION_OPTIMISER), StandardCharsets.UTF_8))) {
			if (dataFile!=null)
				out.printf("OpenProductionTree=%s%n", dataFile.getAbsolutePath());
//			if (saveInStockIngredients) {
//				out.printf("SaveInStockIngredients%n");
//				for (DataModel.Type type:DataModel.Type.values()) {
//					String str = ingredientsInStock.get(type);
//					if (str!=null) out.printf("IngredientsInStock.%s=%s%n", type, str);
//				}
//			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return this;
	}

	private ProductionOptimiser openLastDataFile() {
		if (dataFile!=null) {
			readDataFromFile();
			updateGuiAccess();
			updateWindowTitle();
		}
		return this;
	}
	
	private enum DataBlock { BaseInput, Product }
	
	private void readDataFromFile() {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), StandardCharsets.UTF_8))) {
			
			baseInputsTableModel.clear(false);
			productsTableModel.clear(false);
			
			String line;
			DataBlock dataBlock = null;
			BaseInput baseInput = null;
			Product   product = null;
			while ( (line=in.readLine())!=null ) {
				
				if (line.equals("[BaseInput]")) {
					dataBlock=DataBlock.BaseInput;
					product = null;
					baseInput = new BaseInput("<BaseInput>");
					baseInput.isInfinite = false;
					baseInputsTableModel.add(baseInput);
				}
				if (dataBlock==DataBlock.BaseInput ) {
					if (baseInput==null) throw new IllegalStateException();
					if (line.startsWith("name=")) {
						String valueStr = line.substring("name=".length());
						baseInput.name = valueStr;
					}
					if (line.startsWith("price=")) {
						String valueStr = line.substring("price=".length());
						try { baseInput.price = Integer.parseInt(valueStr); }
						catch (NumberFormatException e) {}
					}
					if (line.startsWith("amount=")) {
						String valueStr = line.substring("amount=".length());
						try { baseInput.storedAmount = Integer.parseInt(valueStr); }
						catch (NumberFormatException e) {}
					}
					if (line.equals("isInfinite")) {
						baseInput.isInfinite = true;
					}
				}
				
				if (line.equals("[Product]")) {
					dataBlock=DataBlock.Product;
					baseInput = null;
					productsTableModel.add(product = new Product("<Product>"));
				}
				if (dataBlock==DataBlock.Product ) {
					if (product==null) throw new IllegalStateException();
					if (line.startsWith("name=")) {
						String valueStr = line.substring("name=".length());
						product.name = valueStr;
					}
					if (line.startsWith("price=")) {
						String valueStr = line.substring("price=".length());
						try { product.price = Integer.parseInt(valueStr); }
						catch (NumberFormatException e) {}
					}
					if (line.startsWith("amount=")) {
						String valueStr = line.substring("amount=".length());
						try { product.producedAmount = Integer.parseInt(valueStr); }
						catch (NumberFormatException e) {}
					}
					readProductInput(line, 1, product);
					readProductInput(line, 2, product);
					readProductInput(line, 3, product);
				}
				
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		productsTableModel.forEach(product->{
			if (product.input1!=null) product.input1 = getInput(product.input1.name);
			if (product.input2!=null) product.input2 = getInput(product.input2.name);
			if (product.input3!=null) product.input3 = getInput(product.input3.name);
		});
		
		StringBuilder msg = new StringBuilder();
		productsTableModel.forEach(product->{
			if (product.findCircRefOf(product.input1, null)) { msg.append(String.format("input %s of product %s%n", product.input1, product)); product.input1=null; }
			if (product.findCircRefOf(product.input2, null)) { msg.append(String.format("input %s of product %s%n", product.input2, product)); product.input2=null; }
			if (product.findCircRefOf(product.input3, null)) { msg.append(String.format("input %s of product %s%n", product.input3, product)); product.input3=null; }
		});
		String msgStr = msg.toString();
		if (!msgStr.isEmpty()) {
			String message = "Found circular references in read file.\r\n";
			message += "Following inputs will be removed:\r\n";
			message += msgStr;
			showErrorMessage("Found circular references", message);
		}
		
		baseInputsTableModel.fireTableUpdate();
		productsTableModel.fireTableUpdate();
	}

	private void saveDataToFile() {
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(dataFile), StandardCharsets.UTF_8))) {
			baseInputsTableModel.forEach(baseInput->{
				out.println("[BaseInput]");
				out.printf("name=%s%n",baseInput.name);
				out.printf("price=%d%n",baseInput.price);
				if (baseInput.isInfinite) out.printf("isInfinite%n");
				else out.printf("amount=%d%n",baseInput.storedAmount);
				out.println();
			});
			productsTableModel.forEach(product->{
				out.println("[Product]");
				out.printf("name=%s%n",product.name);
				out.printf("price=%d%n",product.price);
				out.printf("amount=%d%n",product.producedAmount);
				writeProductInput(out, 1,product.input1,product.n1);
				writeProductInput(out, 2,product.input2,product.n2);
				writeProductInput(out, 3,product.input3,product.n3);
				out.println();
			});
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void readProductInput(String line, int i, Product product) {
		if (line.startsWith("input"+i+"=")) {
			String valueStr = line.substring(("input"+i+"=").length());
			InputDummy inputDummy = new InputDummy(valueStr);
			switch (i) {
			case 1: product.input1 = inputDummy; break;
			case 2: product.input2 = inputDummy; break;
			case 3: product.input3 = inputDummy; break;
			}
		}
		if (line.startsWith("n"+i+"=")) {
			String valueStr = line.substring(("n"+i+"=").length());
			try {
				int n = Integer.parseInt(valueStr);
				switch (i) {
				case 1: product.n1 = n; break;
				case 2: product.n2 = n; break;
				case 3: product.n3 = n; break;
				}
			}
			catch (NumberFormatException e) {}
		}
	}

	private void writeProductInput(PrintWriter out, int i, Input input, int n) {
		if (input!=null) {
			out.printf("input%d=%s%n", i, input.name);
			out.printf("n%d=%d%n", i, n);
		}
	}

	private static int parseInt(Object aValue) {
		if (aValue!=null)
			try { return Integer.parseInt(aValue.toString()); }
			catch (NumberFormatException e) {}
		return 0;
	}

	private Input getInput(String name) {
		BaseInput baseInput = baseInputsTableModel.get(input->input.name.equals(name));
		if (baseInput!=null) return baseInput;
		Product product = productsTableModel.get(input->input.name.equals(name));
		return product;
	}

	private boolean isUniqueName(Input input, String name) {
		BaseInput baseInput = baseInputsTableModel.get(input_->input_!=input && input_.name.equals(name));
		if (baseInput!=null) return false;
		Product product = productsTableModel.get(input_->input_!=input && input_.name.equals(name));
		if (product!=null) return false;
		return true;
	}

	private void checkUniqueName(Input input) {
		if (input==null) return;
		if (isUniqueName(input,input.name)) return;
		int i=1;
		String newName = String.format("%s_%03d", input.name, i);
		while (!isUniqueName(input,newName))
			newName = String.format("%s_%03d", input.name, ++i);
		showWarningMessage("Name is not unique", String.format("Entered name \"%s\" is not unique. It will be changed into \"%s\".", input.name, newName));
		input.name = newName;
	}

	private void showErrorMessage(String title, String message) {
		JOptionPane.showMessageDialog(mainwindow, message, title, JOptionPane.ERROR_MESSAGE);
	}

	private void showWarningMessage(String title, String message) {
		JOptionPane.showMessageDialog(mainwindow, message, title, JOptionPane.WARNING_MESSAGE);
	}
	
	private class Result {
		public int avail;
		public HashMap<BaseInput,Integer> consumedBaseInputs;
		public HashMap<Product,Integer> storedProducts;
		public HashMap<Product,Integer> level1Amounts;
		
		public Result(HashMap<Product, Integer> currentAmounts) {
			storedProducts = new HashMap<>(currentAmounts);
			level1Amounts = new HashMap<>(currentAmounts);
			consumedBaseInputs = new HashMap<>();
			avail = 0;
		}
		
		@Override public String toString() {
			return "Result [avail=" + avail + "]";
		}
	}

	private class MostValuableProduction implements CancelListener {
		
		private static final int MAX_RESULT_COUNT = 10;
		private Vector<ProductionOptimiser.Result> results;
		private ProgressDialog pd;
		private boolean cancelTask;

		MostValuableProduction(ProgressDialog pd) {
			this.pd = pd;
			results = new Vector<>();
			cancelTask = false;
			pd.addCancelListener(this);
		}

		@Override public void cancelTask() {
			cancelTask = true;
		}

		public Vector<ProductionOptimiser.Result> find() {
			SaveViewer.runInEventThreadAndWait(()->{
				pd.displayProgressString(ProgressDialog.ProgressDisplay.Number);
				
				pd.setTaskTitle("Checking Requirements");
				pd.setIndeterminate(true);
			});
			
			if (!checkRequirements()) return null;
			
			SaveViewer.runInEventThreadAndWait(()->{
				pd.setTaskTitle("Running Loop");
				pd.setIndeterminate(true);
			});
			
			results.clear();
			Loop loop = new Loop();
			loop.start();
			
			if (cancelTask) return null;
			
			return results;
		}

		private class Result extends ProductionOptimiser.Result {
			private boolean canStillProduce;
		
			public Result(HashMap<Product, Integer> currentAmounts) {
				super(currentAmounts);
				currentAmounts.forEach((p,n)->{
					consumeBaseInput(p.input1,p.n1*n);
					consumeBaseInput(p.input2,p.n2*n);
					consumeBaseInput(p.input3,p.n3*n);
				});
			}

			public void computeValues() {
				canStillProduce = true;
				while (canStillProduce) {
					canStillProduce = false;
					productsTableModel.forEach(product->{
						if (isHigherLevelProduct(product)) {
							Integer stored = storedProducts.get(product);
							if (stored==null) stored=0;
							int n1 = getMaxAmount(product.input1, product.n1);
							int n2 = getMaxAmount(product.input2, product.n2);
							int n3 = getMaxAmount(product.input3, product.n3);
							int n = Math.min(Math.min(n1, n2), n3);
							if (n>0) {
								canStillProduce = true;
								consumeProduct(product.input1, product.n1*n);
								consumeProduct(product.input2, product.n2*n);
								consumeProduct(product.input3, product.n3*n);
							}
							storedProducts.put(product,stored+n);
						}
					});
				}
				
				int costs = consumedBaseInputs.entrySet().stream().mapToInt(entry->entry.getKey().price*entry.getValue()).sum();
				int priceOfProducts = storedProducts.entrySet().stream().mapToInt(entry->entry.getKey().price*entry.getValue()).sum();
				avail = priceOfProducts-costs;
			}

			private void consumeBaseInput(Input input, int n) {
				if (input instanceof BaseInput)
					add(consumedBaseInputs, (BaseInput) input, n);
				else if (input!=null)
					throw new IllegalStateException();
			}
		
			private int getMaxAmount(Input input, int n) {
				if (input==null) return Integer.MAX_VALUE;
				if (input instanceof Product) {
					Integer stored = storedProducts.get((Product) input);
					return stored==null ? 0 : stored/n;
				} else
					throw new IllegalStateException();
			}

			private void consumeProduct(Input input, int n) {
				if (input instanceof Product)
					add(storedProducts, (Product) input, -n);
				else if (input!=null)
					throw new IllegalStateException();
			}
			
			private <V> void add(HashMap<V,Integer> map, V key, int n) {
				Integer value = map.get(key);
				if (value==null) value=0;
				map.put(key,value + n);
			}
			
		}

		private class Loop {
			
			private HashMap<Product,Integer> maxAmounts;
			private HashMap<Product,Integer> currentAmounts;
			private Vector<Product> products;
			private HashMap<BaseInput,HashMap<Product,Integer>> neededBaseInputs;
			private int[] indexMultipliers;

			Loop() {
				maxAmounts = new HashMap<>();
				currentAmounts = new HashMap<>();
				products = new Vector<>();
				neededBaseInputs = new HashMap<>();
				indexMultipliers = null;
			}

			public void start() {
				maxAmounts.clear();
				currentAmounts.clear();
				productsTableModel.forEach(p->{
					if ( isLevel1Product(p) ) {
						maxAmounts.put(p,p.computeProducibleAmount());
						currentAmounts.put(p,0);
					}
				});
				
				products.clear();
				products.addAll(maxAmounts.keySet());
				Gui.log_ln("Lvl1 Products: %s", products);
				Gui.log_ln("Max. Amounts: %s", maxAmounts);
				
				indexMultipliers = new int[products.size()];
				int numberOfCases = 1;
				for (int i=products.size()-1; i>=0; i--) {
					Product p = products.get(i);
					indexMultipliers[i] = numberOfCases;
					numberOfCases *= maxAmounts.get(p);
				}
				Gui.log_ln("Number Of Cases: %d", numberOfCases);
				int numberOfCases2 = (int)numberOfCases;
				SaveViewer.runInEventThreadAndWait(()->{ pd.setValue(0, numberOfCases2); });
				
				neededBaseInputs.clear();
				loop(0,0);
			}

			private void loop(int loopLevel, int caseIndex) {
				SaveViewer.runInEventThreadAndWait(()->{
					pd.setValue(caseIndex);
				});
				
				if (loopLevel>=products.size()) { // most inner loop
					Result result = new Result(currentAmounts);
					result.computeValues();
					if (results.isEmpty() || results.firstElement().avail<=result.avail) {
						results.add(result);
						results.sort(Comparator.<ProductionOptimiser.Result,Integer>comparing(res->res.avail));
						while (results.size()>MAX_RESULT_COUNT) {
							if (results.get(0).avail == results.get(MAX_RESULT_COUNT).avail)
								break;
							results.remove(0);
						}
					}
					return;
				}
				
				Product product = products.get(loopLevel);
				int max = maxAmounts.get(product);
				
				for (int n=0; n<=max; n++) {
					if (cancelTask) return;
					setAmount(product, n);
					if (!haveAllResources()) break;
					loop(loopLevel+1, caseIndex + n*indexMultipliers[loopLevel]);
				}
				setAmount(product, 0);
			}

			private void setAmount(Product product, int n) {
				currentAmounts.put(product,n);
				setNeededBaseInputs(product.input1, product, n*product.n1);
				setNeededBaseInputs(product.input2, product, n*product.n2);
				setNeededBaseInputs(product.input3, product, n*product.n3);
			}

			private void setNeededBaseInputs(Input input, Product product, int n) {
				if (input instanceof BaseInput) {
					BaseInput baseInput = (BaseInput) input;
					HashMap<Product, Integer> needs = neededBaseInputs.get(baseInput);
					if (needs==null) neededBaseInputs.put(baseInput, needs = new HashMap<>());
					needs.put(product, n);
				}
			}

			private boolean haveAllResources() {
				for (BaseInput baseInput:neededBaseInputs.keySet()) {
					int consumption = neededBaseInputs.get(baseInput).entrySet().stream().mapToInt(entry->entry.getValue()).sum();
					if (consumption > baseInput.storedAmount)
						return false;
				}
				return true;
			}
		}

		private boolean checkRequirements() {
			StringBuilder msg = new StringBuilder();
			HashSet<Product> usedInputs = new HashSet<>();
			productsTableModel.forEach(p->{
				if ( isLevel1Product(p) )
					return; // this is a level 1 product -> Ok
				
				if (!isHigherLevelProduct(p) )
					msg.append(String.format("\"%s\" used mixed inputs (Base Inputs and Products).%n", p));
				
				if (hasNoInputs(p) )
					msg.append(String.format("\"%s\" has no inputs.%n", p));
				
				if (!hasExclusiveInputs(usedInputs, p) )
					msg.append(String.format("\"%s\": Not all inputs are exclusive.%n", p));
				
				int cost = p.computeCosts();
				if (p.price < cost) {
					msg.append(String.format(Locale.ENGLISH, "\"%s\": Manufacturing costs of %,d U are higher than price of %,d U.%n", p, cost, p.price));
				}
				
			});
			String msgStr = msg.toString();
			if (!msgStr.isEmpty()) {
				showErrorMessage("Not all products meets the requirements", "Not all products meets the requirements:\r\n"+msgStr);
				return false;
			}
			return true;
		}

		private boolean hasNoInputs(Product p) {
			return p.input1==null && p.input2==null && p.input3==null;
		}

		private boolean isLevel1Product(Product p) {
			Input i1 = p.input1;
			Input i2 = p.input2;
			Input i3 = p.input3;
			return (i1==null || i1 instanceof BaseInput) && (i2==null || i2 instanceof BaseInput) && (i3==null || i3 instanceof BaseInput);
		}

		private boolean isHigherLevelProduct(Product p) {
			Input i1 = p.input1;
			Input i2 = p.input2;
			Input i3 = p.input3;
			return (i1==null || i1 instanceof Product) && (i2==null || i2 instanceof Product) && (i3==null || i3 instanceof Product);
		}

		private boolean hasExclusiveInputs(HashSet<Product> usedInputs, Product p) {
			return isExclusiveInput(usedInputs, p.input1) && isExclusiveInput(usedInputs, p.input2) && isExclusiveInput(usedInputs, p.input3);
		}

		private boolean isExclusiveInput(HashSet<Product> usedInputs, Input input) {
			if (input instanceof Product) {
				Product product = (Product) input;
				if (usedInputs.contains(product)) return false;
				usedInputs.add(product);
			}
			return true;
		}
	
	}

	private class InputList implements ComboBoxModel<Input> {
		
		private Vector<ListDataListener> listDataListeners;
		private Input selectedItem = null;

		InputList() {
			listDataListeners = new Vector<>();
		}

		@Override public void    addListDataListener(ListDataListener l) { listDataListeners.   add(l); }
		@Override public void removeListDataListener(ListDataListener l) { listDataListeners.remove(l); }

		public void update() {
			ListDataEvent e = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, getSize()-1);
			for (ListDataListener ldl:listDataListeners) {
				ldl.contentsChanged(e);
			}
		}

		@Override public int getSize() {
			return 1 + baseInputsTableModel.size() + productsTableModel.size();
		}

		@Override public Input getElementAt(int index) {
			int sizeB = baseInputsTableModel.size();
			int sizeP = productsTableModel.size();
			if (index==0) return null;
			if (index<1+sizeB      ) return baseInputsTableModel.get(index-1);
			if (index<1+sizeB+sizeP) return   productsTableModel.get(index-1-sizeB);
			return null;
		}

		@Override public Object getSelectedItem() {
			return selectedItem;
		}

		@Override public void setSelectedItem(Object anItem) {
			selectedItem = anItem instanceof Input ? (Input) anItem : null;
		}
	}
	
	private static class Input {
		String name;
		int price;
		Input(String name) {
			this.name = name;
			this.price = 1;
		}
		@Override public String toString() { return name; }
		
	}
	private static class InputDummy extends Input {

		public InputDummy(String name) {
			super(name);
		}

	}
	private static class BaseInput extends Input{
		
		boolean isInfinite;
		int storedAmount;
		
		private BaseInput(String name) {
			super(name);
			this.isInfinite = true;
			this.storedAmount = 0;
		}
	}
	private static class Product extends Input{

		int   producedAmount;
		Input input1;
		Input input2;
		Input input3;
		int   n1;
		int   n2;
		int   n3;
		
		Product(String name) {
			super(name);
			this.producedAmount = 1;
			this.input1 = null;
			this.input2 = null;
			this.input3 = null;
			this.n1 = 1;
			this.n2 = 1;
			this.n3 = 1;
		}

		public int computeCosts() {
			return 
					(input1==null?0:input1.price*n1) + 
					(input2==null?0:input2.price*n2) + 
					(input3==null?0:input3.price*n3);
		}

		public int getLevel() {
			return getLevel(new HashSet<>());
		}
		private int getLevel(HashSet<Product> parents) {
			if (parents.contains(this))
				throw new IllegalStateException(String.format("Found circular reference: \"%s\" found in [%s]", this, parents));
			parents.add(this);
			return 1 + Math.max(
						Math.max(
							getLevelOf(input1,new HashSet<>(parents)),
							getLevelOf(input2,new HashSet<>(parents))
						),
						getLevelOf(input3,new HashSet<>(parents))
					);
		}

		public int getLevelOf(Input input) {
			HashSet<Product> parents = new HashSet<>(); parents.add(this);
			return getLevelOf(input, parents);
		}
		private int getLevelOf(Input input, HashSet<Product> parents) {
			if (input instanceof Product)
				return ((Product)input).getLevel(parents);
			return 0;
		}

		public boolean isInfinite(Input input) {
			if (input==null) return true;
			if (input instanceof BaseInput)
				return ((BaseInput) input).isInfinite;
			return false;
		}

		public boolean findCircRefOf(Input input, StringBuilder msg) {
			HashSet<Product> parents = new HashSet<>(); parents.add(this);
			return findCircRefOf(input, parents, msg);
		}
		private boolean findCircRefOf(Input input, HashSet<Product> parents, StringBuilder msg) {
			if (input instanceof Product) {
				Product product = (Product) input;
				if (parents.contains(product)) {
					if (msg!=null) msg.append(String.format("\"%s\" found in %s%n", product, parents));
					return true;
				}
				parents.add(product);
				return
					product.findCircRefOf(product.input1, new HashSet<>(parents), msg) ||
					product.findCircRefOf(product.input2, new HashSet<>(parents), msg) ||
					product.findCircRefOf(product.input3, new HashSet<>(parents), msg);
			}
			return false;
		}

		public int computeProducibleAmount() {
			int n1_ = getProducibleAmount(input1,n1);
			int n2_ = getProducibleAmount(input2,n2);
			int n3_ = getProducibleAmount(input3,n3);
			int n = Math.min(Math.min(n1_, n2_), n3_);
			return n * producedAmount;
		}

		private int getProducibleAmount(Input input, int n) {
			if (input instanceof BaseInput) {
				BaseInput baseInput = (BaseInput) input;
				return baseInput.storedAmount/n;
			}
			if (input==null) return Integer.MAX_VALUE;
			return 0;
		}
		
	}
	
	private static Color[] createColors(int n, Color color1, Color color2) {
		Color[] colors = new Color[n];
		float[] comps1 = color1.getComponents(new float[4]);
		float[] comps2   = color2.getComponents(new float[4]);
		for (int i=0; i<colors.length; i++) {
			float f1 = i/(colors.length-1f);
			float f2 = 1-f1;
			colors[i] = new Color(
				comps1[0]*f1+comps2[0]*f2,
				comps1[1]*f1+comps2[1]*f2,
				comps1[2]*f1+comps2[2]*f2
			);
		}
		return colors;
	}
	
	// [30, 56, 148, 51]
	private enum BaseInputsTableColumnID implements SimplifiedColumnIDInterface {
		Infinite ("Inf"      , Boolean.class, 20,-1, 30,-1),
		Amount   ("Amount"   , Integer.class, 20,-1, 50,-1),
		Name     ("Name"     ,  String.class, 20,-1,150,-1),
		Price    ("Price"    , Integer.class, 20,-1, 50,-1),
		Consumed ("Consumed" , Integer.class, 20,-1, 60,-1),
		Cost     ("Cost"     , Integer.class, 20,-1, 50,-1),
		Remaining("Remaining", Integer.class, 20,-1, 60,-1),
		;
		private SimplifiedColumnConfig columnConfig;
		BaseInputsTableColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
			columnConfig = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
		}
		@Override public SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
	}
	private class BaseInputsTableModel extends SimplifiedTableModel<BaseInputsTableColumnID> {

		private Vector<BaseInput> baseInputs;

		protected BaseInputsTableModel() {
			super(BaseInputsTableColumnID.values());
			baseInputs = new Vector<>();
		}

		public void clear(boolean updateTable) {
			baseInputs.clear();
			if (updateTable) fireTableUpdate();
		}

		public int size() { return baseInputs.size(); }
		public void add(BaseInput baseInput) { baseInputs.add(baseInput); }
		public BaseInput get(int index) { return baseInputs.get(index); }

		public BaseInput get(Predicate<BaseInput> predicate) {
			for (BaseInput product:baseInputs)
				if (predicate.test(product))
					return product;
			return null;
		}

		public void forEach(Consumer<BaseInput> action) {
			baseInputs.forEach(action);
		}

		@Override public int getRowCount() {
			return size()+1;
		}

		@Override protected boolean isCellEditable(int rowIndex, int columnIndex, BaseInputsTableColumnID columnID) {
			if (rowIndex>=size())
				return columnID==BaseInputsTableColumnID.Name;
			
			BaseInput baseInput = get(rowIndex);
			switch (columnID) {
			case Amount  : return !baseInput.isInfinite;
			case Infinite:
			case Name    :
			case Price   : return true;
			case Consumed :
			case Cost     :
			case Remaining: return false;
			}
			return false;
		}

		@Override public Object getValueAt(int rowIndex, int columnIndex, BaseInputsTableColumnID columnID) {
			if (rowIndex>=size())
				return null;
			BaseInput baseInput = get(rowIndex);
			switch (columnID) {
			case Infinite: return baseInput.isInfinite;
			case Amount  : return baseInput.isInfinite?null:baseInput.storedAmount;
			case Name    : return baseInput.name;
			case Price   : return baseInput.price;
			case Consumed : if (selectedResult==null) return null; return getConsumedBaseInput(baseInput);
			case Cost     : if (selectedResult==null) return null; return getConsumedBaseInput(baseInput)*baseInput.price;
			case Remaining: if (selectedResult==null || baseInput.isInfinite) return null; return baseInput.storedAmount - getConsumedBaseInput(baseInput);
			}
			return null;
		}

		private int getConsumedBaseInput(BaseInput baseInput) {
			Integer val = selectedResult.consumedBaseInputs.get(baseInput);
			if (val==null) return 0;
			return val;
		}

		@Override protected void setValueAt(Object aValue, int rowIndex, int columnIndex, BaseInputsTableColumnID columnID) {
			if (rowIndex>=size()) {
				if (columnID!=BaseInputsTableColumnID.Name)
					throw new IllegalStateException();
				add(new BaseInput(aValue.toString()));
				inputList.update();
				fireTableRowAdded(size());
			}
			BaseInput baseInput = get(rowIndex);
			switch (columnID) {
			case Infinite: baseInput.isInfinite   = (Boolean) aValue; productsTableModel.fireTableUpdate(); fireTableRowUpdate(rowIndex); break; 
			case Amount  : baseInput.storedAmount = parseInt(aValue); productsTableModel.fireTableUpdate(); break;
			case Name    : baseInput.name         = (String) aValue; checkUniqueName(baseInput); productsTableModel.fireTableUpdate(); break;
			case Price   : baseInput.price        = parseInt(aValue); break;
			case Consumed : break;
			case Cost     : break;
			case Remaining: break;
			}
		}

		public TableCellRenderer createCellRenderer() {
			return new CellRenderer();
		}

		private class CellRenderer extends DefaultTableCellRenderer {
			private static final long serialVersionUID = 6038988920942507227L;
			
			CheckBoxRendererComponent checkBox;
			CellRenderer() {
				checkBox = new CheckBoxRendererComponent();
			}
		
			@Override
			public Component getTableCellRendererComponent(JTable table, Object aValue, boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
				Component component = super.getTableCellRendererComponent(table, aValue, isSelected, hasFocus, rowIndex, columnIndex);
				
				if (Boolean.class.isAssignableFrom(getColumnID(columnIndex).columnConfig.columnClass)) {
					component = checkBox;
					checkBox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
					checkBox.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
					checkBox.setSelected(aValue instanceof Boolean ? (Boolean) aValue : false);
					checkBox.setHorizontalAlignment(CENTER);
				}
				
				Color bgColor = table.getBackground();
				BaseInputsTableColumnID columnID = getColumnID(columnIndex);
				Class<?> columnClass = columnID==null?null:columnID.columnConfig.columnClass;
				
				if (component instanceof JLabel) {
					JLabel label = (JLabel) component;
					int horizontalAlignment;
					
					if (columnID==BaseInputsTableColumnID.Amount || columnID==BaseInputsTableColumnID.Consumed || columnID==BaseInputsTableColumnID.Cost || columnID==BaseInputsTableColumnID.Remaining)
						horizontalAlignment = JLabel.RIGHT;
					else if (columnClass==Integer.class)
						horizontalAlignment = JLabel.CENTER;
					else
						horizontalAlignment = JLabel.LEFT;
					
					label.setHorizontalAlignment( horizontalAlignment );
				}
				
				if (rowIndex<BaseInputsTableModel.this.size()) {
					BaseInput baseInput = get(rowIndex);
					if (baseInput.isInfinite) bgColor = COLOR_INFINITE_INPUT;
				}
				
				if (!isSelected) component.setBackground(bgColor);
				return component;
			}
		}
	}
	
	private enum ProductsTableColumnID implements SimplifiedColumnIDInterface {
		Level     ("Lvl"       , Integer.class, 20,-1, 20,-1),
		Amount    ("N"         , Integer.class, 20,-1, 40,-1),
		Name      ("Result"    ,  String.class, 20,-1,150,-1),
		Price     ("Price"     , Integer.class, 20,-1, 50,-1),
		Cost      ("Costs"     , Integer.class, 20,-1, 50,-1),
		Ratio     ("P/C"       ,  String.class, 20,-1, 50,-1),
		N1        ("N1"        , Integer.class, 20,-1, 40,-1),
		Input1    ("Input 1"   ,   Input.class, 20,-1,150,-1),
		N2        ("N2"        , Integer.class, 20,-1, 40,-1),
		Input2    ("Input 2"   ,   Input.class, 20,-1,150,-1),
		N3        ("N3"        , Integer.class, 20,-1, 40,-1),
		Input3    ("Input 3"   ,   Input.class, 20,-1,150,-1),
		Producible("Producible", Integer.class, 20,-1, 60,-1),
		Produced  ("Produced"  , Integer.class, 20,-1, 60,-1),
		Value     ("Value"     , Integer.class, 20,-1, 60,-1),
		;
		private SimplifiedColumnConfig columnConfig;
		ProductsTableColumnID(String name, Class<?> columnClass, int minWidth, int maxWidth, int prefWidth, int currentWidth) {
			columnConfig = new SimplifiedColumnConfig(name, columnClass, minWidth, maxWidth, prefWidth, currentWidth);
		}
		@Override public SimplifiedColumnConfig getColumnConfig() { return columnConfig; }
	}
	private class ProductsTableModel extends SimplifiedTableModel<ProductsTableColumnID> {
		
		private Vector<Product> products;

		protected ProductsTableModel() {
			super(ProductsTableColumnID.values());
			products = new Vector<>();
		}

		public void clear(boolean updateTable) {
			products.clear();
			if (updateTable) fireTableUpdate();
		}

		public int size() { return products.size(); }
		public void add(Product product) { products.add(product); }
		public Product get(int i) { return products.get(i); }

		public Product get(Predicate<Product> predicate) {
			for (Product product:products)
				if (predicate.test(product))
					return product;
			return null;
		}

		public void forEach(Consumer<Product> action) {
			products.forEach(action);
		}

		@Override public int getRowCount() {
			return size()+1;
		}

		@Override protected boolean isCellEditable(int rowIndex, int columnIndex, ProductsTableColumnID columnID) {
			if (rowIndex>=size())
				return columnID==ProductsTableColumnID.Name;
			Product product = get(rowIndex);
			switch (columnID) {
			
			case Level :
			case Cost  :
			case Ratio :
				return false;
				
			case Amount:
			case Name  :
			case Price :
			case Input1:
			case Input2:
			case Input3:
				return true;
				
			case N1: return product.input1!=null;
			case N2: return product.input2!=null;
			case N3: return product.input3!=null;
			
			case Producible:
			case Produced:
			case Value:
				return false;
			}
			return false;
		}

		@Override public Object getValueAt(int rowIndex, int columnIndex, ProductsTableColumnID columnID) {
			if (rowIndex>=size())
				return null;
			Product product = get(rowIndex);
			switch (columnID) {
			case Level : return product.getLevel();
			case Amount: return product.producedAmount;
			case Name  : return product.name;
			case Price : return product.price;
			case Cost  : return product.computeCosts();
			case Ratio : { int costs = product.computeCosts(); if (costs==0) return ""; return String.format(Locale.ENGLISH, "%1.2f", product.price/(float)costs); }
			case Input1: return product.input1;
			case Input2: return product.input2;
			case Input3: return product.input3;
			case N1    : return product.input1==null?null:product.n1;
			case N2    : return product.input2==null?null:product.n2;
			case N3    : return product.input3==null?null:product.n3;
			case Producible: if (product.getLevel()==1) return product.computeProducibleAmount(); return null;
			case Produced: if (selectedResult==null) return null; return getStoredProducts(product);
			case Value   : if (selectedResult==null) return null; return getStoredProducts(product)*product.price;
			}
			return null;
		}

		private int getStoredProducts(Product product) {
			Integer val = selectedResult.storedProducts.get(product);
			if (val==null) return 0;
			return val;
		}

		@Override protected void setValueAt(Object aValue, int rowIndex, int columnIndex, ProductsTableColumnID columnID) {
			if (rowIndex>=size()) {
				if (columnID!=ProductsTableColumnID.Name)
					throw new IllegalStateException();
				add(new Product(aValue.toString()));
				inputList.update();
				fireTableRowAdded(size());
			}
			Product product = get(rowIndex);
			switch (columnID) {
			case Level : break;
			case Amount: product.producedAmount = parseInt(aValue); break;
			case Name  : product.name = (String)aValue; checkUniqueName(product); break;
			case Price : product.price = parseInt(aValue); break;
			case Cost  : break;
			case Ratio : break;
			case Input1: aValue = checkCircRef(product,(Input)aValue); product.input1 = (Input)aValue; fireTableRowUpdate(rowIndex); break;
			case Input2: aValue = checkCircRef(product,(Input)aValue); product.input2 = (Input)aValue; fireTableRowUpdate(rowIndex); break;
			case Input3: aValue = checkCircRef(product,(Input)aValue); product.input3 = (Input)aValue; fireTableRowUpdate(rowIndex); break;
			case N1    : product.n1 = parseInt(aValue); break;
			case N2    : product.n2 = parseInt(aValue); break;
			case N3    : product.n3 = parseInt(aValue); break;
			case Producible: break;
			case Produced: break;
			case Value   : break;
			}
		}

		private Input checkCircRef(Product product, Input input) {
			StringBuilder msg = new StringBuilder();
			if (product.findCircRefOf(input,msg)) {
				String title = "Found circular reference";
				String message = "Found circular reference:\r\n"+msg;
				message += "This means, that an input of \""+product+"\" or itself needs itself as a direct or indirect input.\r\n";
				message += "Selected input will be removed.";
				showErrorMessage(title, message);
				return null;
			}
			return input;
		}

		public TableCellRenderer createCellRenderer() {
			return new CellRenderer();
		}

		private class CellRenderer extends DefaultTableCellRenderer {
			private static final long serialVersionUID = 5188167151442469126L;
		
			@Override
			public Component getTableCellRendererComponent(JTable table, Object aValue, boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
				Component component = super.getTableCellRendererComponent(table, aValue, isSelected, hasFocus, rowIndex, columnIndex);
				
				Color bgColor = table.getBackground();
				ProductsTableColumnID columnID = getColumnID(columnIndex);
				Class<?> columnClass = columnID==null?null:columnID.columnConfig.columnClass;
				
				if (rowIndex<ProductsTableModel.this.size()) {
					Product product = get(rowIndex);
					
					int level = product.getLevel();
					bgColor = getProductLevelColor(level);
					
					if (component instanceof JLabel) {
						JLabel label = (JLabel) component;
						int horizontalAlignment;
						
						if (columnID==ProductsTableColumnID.Price || columnID==ProductsTableColumnID.Cost || columnID==ProductsTableColumnID.Ratio || columnID==ProductsTableColumnID.Value)
							horizontalAlignment = JLabel.RIGHT;
						else if (columnClass==Integer.class)
							horizontalAlignment = JLabel.CENTER;
						else
							horizontalAlignment = JLabel.LEFT;
						
						label.setHorizontalAlignment( horizontalAlignment );
					}
					
					switch (getColumnID(columnIndex)) {
					case Level :
					case Amount:
					case Name  :
					case Price :
					case Cost  :
					case Ratio : break;
					
					case Input1:
					case N1    : if (product.input1!=null) bgColor = getProductLevelColor(product.getLevelOf(product.input1)); break;
					case Input2:
					case N2    : if (product.input2!=null) bgColor = getProductLevelColor(product.getLevelOf(product.input2)); break;
					case Input3:
					case N3    : if (product.input3!=null) bgColor = getProductLevelColor(product.getLevelOf(product.input3)); break;
					
					case Producible:
					case Produced:
					case Value   : break;
					}
					
					boolean hasInfiniteInput = product.isInfinite(product.input1) && product.isInfinite(product.input2) && product.isInfinite(product.input3);
					if (hasInfiniteInput) bgColor = COLOR_INFINITE_PRODUCT;
				}
				
				if (!isSelected) component.setBackground(bgColor);
				
				return component;
			}

			private Color getProductLevelColor(int level) {
				return COLOR_PRODUCT_LEVEL[ Math.min( level, COLOR_PRODUCT_LEVEL.length-1 ) ];
			}
		
		}
	
	}
}
