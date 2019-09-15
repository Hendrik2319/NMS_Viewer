package net.schwarzbaer.java.games.nomanssky.saveviewer;

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
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
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
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.gui.StandardMainWindow.DefaultCloseOperation;
import net.schwarzbaer.gui.Tables.CheckBoxRendererComponent;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.gui.Tables.SimplifiedColumnIDInterface;
import net.schwarzbaer.gui.Tables.SimplifiedTableModel;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveViewer.ToolbarIcons;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView;

class ProductionOptimiser implements ActionListener {

	private static final String PRODUCTION_OPTIMISER_CFG = "NMS_Viewer.ProductionOptimiser.cfg";
	
	private static final Color COLOR_INFINITE_PRODUCT = Color.RED;
	private static final Color COLOR_INFINITE_INPUT = new Color(0xe0e0e0);
	private static final Color[] COLOR_PRODUCT_LEVEL = createColors(3,new Color(255,204,153),new Color(204,255,0));

	public static void main(String[] args) {
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		SaveViewer.loadToolbarIcons();
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
	
	private TableView.SimplifiedTable baseInputsTable = null;
	private TableView.SimplifiedTable   productsTable = null;
	private BaseInputsTableModel baseInputsTableModel = null;
	private   ProductsTableModel   productsTableModel = null;
	private InputList inputList = null;
	private JTextArea outputTextArea = null;

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
		baseInputsTable = new TableView.SimplifiedTable("BaseInputsTable", baseInputsTableModel, true, true, false);
		baseInputsTable.setCellRendererForAllColumns(baseInputsTableModel.createCellRenderer(), true);
		JCheckBox rendererCheckBox = new JCheckBox();
		rendererCheckBox.setHorizontalAlignment(JCheckBox.CENTER);
		baseInputsTable.setDefaultEditor(Boolean.class, new DefaultCellEditor(rendererCheckBox));
		JScrollPane baseInputsTableScrollPane = new JScrollPane(baseInputsTable);
		baseInputsTableScrollPane.setBorder(createCompoundBorder("Base Inputs") );
		baseInputsTableScrollPane.setPreferredSize(new Dimension(900,200));
		
		productsTableModel = new ProductsTableModel();
		productsTable = new TableView.SimplifiedTable("ProductsTable", productsTableModel, true, true, false);
		productsTable.setCellRendererForAllColumns(productsTableModel.createCellRenderer(), true);
		productsTable.setDefaultEditor(Input.class, new DefaultCellEditor(new JComboBox<Input>(inputList)));
		JScrollPane productsTableScrollPane = new JScrollPane(productsTable);
		productsTableScrollPane.setBorder(createCompoundBorder("Products"));
		productsTableScrollPane.setPreferredSize(new Dimension(900,200));
		
		outputTextArea = new JTextArea();
		JScrollPane outputScrollPane = new JScrollPane(outputTextArea);
		outputScrollPane.setBorder(BorderFactory.createTitledBorder("Output"));
		outputScrollPane.setBorder(createCompoundBorder("Output"));
		outputScrollPane.setPreferredSize(new Dimension(200,200));
		
		JSplitPane tablePane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		tablePane.setTopComponent(baseInputsTableScrollPane);
		tablePane.setBottomComponent(productsTableScrollPane);
		tablePane.setResizeWeight(0.5);
		
		JSplitPane contentPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		contentPane.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
		contentPane.setLeftComponent(tablePane);
		contentPane.setRightComponent(outputScrollPane);
		contentPane.setResizeWeight(0.8);
		
		JMenu menuData = new JMenu("Data");
		menuData.add(SaveViewer.createMenuItem("Clear data"                , this, disabler, ActionCommand.ClearData     , ToolbarIcons.Delete));
		menuData.add(SaveViewer.createMenuItem("Read data from file ..."   , this, disabler, ActionCommand.OpenDataFile  , ToolbarIcons.Open));
		menuData.add(SaveViewer.createMenuItem("Write data to file"        , this, disabler, ActionCommand.SaveDataFile  , ToolbarIcons.Save));
		menuData.add(SaveViewer.createMenuItem("Write data to new file ...", this, disabler, ActionCommand.SaveDataFileAs, ToolbarIcons.SaveAs));
		
		JMenu menuAnalyse = new JMenu("Analyse");
		menuAnalyse.add(SaveViewer.createMenuItem("Find production with most valuable products", this, disabler, ActionCommand.FindMostValuableProduction));
		
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(menuData);
		menuBar.add(menuAnalyse);
		
		mainwindow = new StandardMainWindow("Production Optimiser",standalone?DefaultCloseOperation.EXIT_ON_CLOSE:DefaultCloseOperation.HIDE_ON_CLOSE);
		mainwindow.startGUI(contentPane,menuBar);
		
		updateGuiAccess();
		updateWindowTitle();
		return this;
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
			
		case FindMostValuableProduction:
			// TODO
			break;
		}
	}

	private ProductionOptimiser readConfig() {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(PRODUCTION_OPTIMISER_CFG), StandardCharsets.UTF_8))) {
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
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(PRODUCTION_OPTIMISER_CFG), StandardCharsets.UTF_8))) {
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
						try { baseInput.amount = Integer.parseInt(valueStr); }
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
						try { product.amount = Integer.parseInt(valueStr); }
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
				else out.printf("amount=%d%n",baseInput.amount);
				out.println();
			});
			productsTableModel.forEach(product->{
				out.println("[Product]");
				out.printf("name=%s%n",product.name);
				out.printf("price=%d%n",product.price);
				out.printf("amount=%d%n",product.amount);
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
		String message = String.format("Entered name \"%s\" is not unique. It will be changed into \"%s\".", input.name, newName);
		JOptionPane.showMessageDialog(mainwindow, message, "Name is not unique", JOptionPane.WARNING_MESSAGE);
		input.name = newName;
	}

	private class InputList implements ComboBoxModel<Input> {
		
		private Vector<ListDataListener> listDataListeners;
		private Input selectedItem = null;

		InputList() {
			listDataListeners = new Vector<>();
		}

		public void update() {
			ListDataEvent e = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, getSize()-1);
			for (ListDataListener ldl:listDataListeners) {
				ldl.contentsChanged(e);
			}
		}

		@Override public void    addListDataListener(ListDataListener l) { listDataListeners.   add(l); }
		@Override public void removeListDataListener(ListDataListener l) { listDataListeners.remove(l); }

		@Override public int getSize() {
			return baseInputsTableModel.size() + productsTableModel.size();
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
		int amount;
		
		private BaseInput(String name) {
			super(name);
			this.isInfinite = true;
			this.amount = 0;
		}
	}
	private static class Product extends Input{

		int   amount;
		Input input1;
		Input input2;
		Input input3;
		int   n1;
		int   n2;
		int   n3;
		
		Product(String name) {
			super(name);
			this.amount = 1;
			this.input1 = null;
			this.input2 = null;
			this.input3 = null;
			this.n1 = 1;
			this.n2 = 1;
			this.n3 = 1;
		}

		public int getLevel() {
			return 1 + Math.max(Math.max(getLevelOf(input1), getLevelOf(input2)), getLevelOf(input3));
		}

		private int getLevelOf(Input input) {
			if (input instanceof Product)
				return ((Product)input).getLevel();
			return 0;
		}

		public boolean isInfinite(Input input) {
			if (input==null) return true;
			if (input instanceof BaseInput)
				return ((BaseInput) input).isInfinite;
			return false;
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
		Infinite("Inf"   , Boolean.class, 20,-1, 30,-1),
		Amount  ("Amount", Integer.class, 20,-1, 50,-1),
		Name    ("Name"  ,  String.class, 20,-1,150,-1),
		Price   ("Price" , Integer.class, 20,-1, 50,-1),
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
			}
			return false;
		}

		@Override public Object getValueAt(int rowIndex, int columnIndex, BaseInputsTableColumnID columnID) {
			if (rowIndex>=size())
				return null;
			BaseInput baseInput = get(rowIndex);
			switch (columnID) {
			case Infinite: return baseInput.isInfinite;
			case Amount  : return baseInput.isInfinite?null:baseInput.amount;
			case Name    : return baseInput.name;
			case Price   : return baseInput.price;
			}
			return null;
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
			case Infinite: baseInput.isInfinite = (Boolean) aValue; productsTableModel.fireTableUpdate(); fireTableRowUpdate(rowIndex); break; 
			case Amount  : baseInput.amount     = parseInt(aValue); productsTableModel.fireTableUpdate(); break;
			case Name    : baseInput.name       = (String) aValue; checkUniqueName(baseInput); productsTableModel.fireTableUpdate(); break;
			case Price   : baseInput.price      = parseInt(aValue); break;
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
					label.setHorizontalAlignment( columnClass==Integer.class ? JLabel.CENTER : JLabel.LEFT );
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
		Level   ("Lvl"    , Integer.class, 20,-1, 20,-1),
		Amount  ("N"      , Integer.class, 20,-1, 40,-1),
		Name    ("Result" ,  String.class, 20,-1,150,-1),
		Price   ("Price"  , Integer.class, 20,-1, 40,-1),
		N1      ("N1"     , Integer.class, 20,-1, 40,-1),
		Input1  ("Input 1",   Input.class, 20,-1,150,-1),
		N2      ("N2"     , Integer.class, 20,-1, 40,-1),
		Input2  ("Input 2",   Input.class, 20,-1,150,-1),
		N3      ("N3"     , Integer.class, 20,-1, 40,-1),
		Input3  ("Input 3",   Input.class, 20,-1,150,-1),
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
			}
			return false;
		}

		@Override public Object getValueAt(int rowIndex, int columnIndex, ProductsTableColumnID columnID) {
			if (rowIndex>=size())
				return null;
			Product product = get(rowIndex);
			switch (columnID) {
			case Level : return product.getLevel();
			case Amount: return product.amount;
			case Name  : return product.name;
			case Price : return product.price;
			case Input1: return product.input1;
			case Input2: return product.input2;
			case Input3: return product.input3;
			case N1    : return product.input1==null?null:product.n1;
			case N2    : return product.input2==null?null:product.n2;
			case N3    : return product.input3==null?null:product.n3;
			}
			return null;
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
			case Amount: product.amount = parseInt(aValue); break;
			case Name  : product.name = (String)aValue; checkUniqueName(product); break;
			case Price : product.price = parseInt(aValue); break;
			case Input1: product.input1 = (Input)aValue; fireTableRowUpdate(rowIndex); break;
			case Input2: product.input2 = (Input)aValue; fireTableRowUpdate(rowIndex); break;
			case Input3: product.input3 = (Input)aValue; fireTableRowUpdate(rowIndex); break;
			case N1    : product.n1 = parseInt(aValue); break;
			case N2    : product.n2 = parseInt(aValue); break;
			case N3    : product.n3 = parseInt(aValue); break;
			}
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
						label.setHorizontalAlignment( columnClass==Integer.class ? JLabel.CENTER : JLabel.LEFT );
					}
					
					switch (getColumnID(columnIndex)) {
					case Level :
					case Amount:
					case Name  :
					case Price : break;
					case Input1:
					case N1    : if (product.input1!=null) bgColor = getProductLevelColor(product.getLevelOf(product.input1)); break;
					case Input2:
					case N2    : if (product.input2!=null) bgColor = getProductLevelColor(product.getLevelOf(product.input2)); break;
					case Input3:
					case N3    : if (product.input3!=null) bgColor = getProductLevelColor(product.getLevelOf(product.input3)); break;
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
