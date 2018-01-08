package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;

import net.schwarzbaer.gui.StandardDialog;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Images.NamedColor;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.UniverseAddress;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.UniversePanel;

public class Gui {

	public static class ListBoxDialog<T> extends StandardDialog {
		private static final long serialVersionUID = -317119785847294385L;
		
		private boolean hasResult;
		private T[] options;
		private JList<T> listBox;
	
		public ListBoxDialog(Window mainWindow, String message, String title, T[] options, T initialValue, int width, int height) {
			super(mainWindow,title,ModalityType.APPLICATION_MODAL);
			this.options = options;
			hasResult = false;
			
			listBox = new JList<T>(options);
			listBox.setSelectedValue(initialValue, true);;
			
			JScrollPane scrollPane = new JScrollPane(listBox);
			scrollPane.setPreferredSize(new Dimension(width,height));
			
			JPanel inputPanel = new JPanel(new BorderLayout(3,3));
			inputPanel.add(new JLabel(message),BorderLayout.CENTER);
			inputPanel.add(scrollPane,BorderLayout.SOUTH);
			
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,3,3));
			buttonPanel.add(SaveViewer.createButton("Ok", e->{hasResult=true; closeDialog();}));
			buttonPanel.add(SaveViewer.createButton("Cancel", e->closeDialog()));
			
			JPanel contentPane = new JPanel(new BorderLayout(3,3));
			contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
			contentPane.add(inputPanel,BorderLayout.CENTER);
			contentPane.add(buttonPanel,BorderLayout.SOUTH);
			
			createGUI(contentPane);
		}
	
		public boolean hasResult() {
			return hasResult;
		}
	
		public T getResult() {
			int index = listBox.getSelectedIndex();
			if (index<0) return null;
			return options[index];
		}
	
		public int getResultIndex() {
			return listBox.getSelectedIndex();
		}
	}
	
	public static class ComboBoxDialog<T> extends StandardDialog {
		private static final long serialVersionUID = 2109107550076395513L;
		
		private boolean hasResult;
		private T[] options;
		private JComboBox<T> comboBox;
	
		public ComboBoxDialog(Window mainWindow, String message, String title, T[] options, T initialValue) {
			super(mainWindow,title,ModalityType.APPLICATION_MODAL);
			this.options = options;
			hasResult = false;
			
			comboBox = new JComboBox<T>(options);
			comboBox.setSelectedItem(initialValue);
			
			JPanel inputPanel = new JPanel(new BorderLayout(3,3));
			inputPanel.add(new JLabel(message),BorderLayout.CENTER);
			inputPanel.add(comboBox,BorderLayout.SOUTH);
			
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,3,3));
			buttonPanel.add(SaveViewer.createButton("Ok", e->{hasResult=true; closeDialog();}));
			buttonPanel.add(SaveViewer.createButton("Cancel", e->closeDialog()));
			
			JPanel contentPane = new JPanel(new BorderLayout(3,3));
			contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
			contentPane.add(inputPanel,BorderLayout.CENTER);
			contentPane.add(buttonPanel,BorderLayout.SOUTH);
			
			createGUI(contentPane);
		}
	
		public boolean hasResult() {
			return hasResult;
		}
	
		public T getResult() {
			int index = comboBox.getSelectedIndex();
			if (index<0) return null;
			return options[index];
		}
	
		public int getResultIndex() {
			return comboBox.getSelectedIndex();
		}
	
	}

	public static class CoordinatesDialog extends StandardDialog {
		private static final long serialVersionUID = -2899608237998750242L;
		
		private JLabel[] glyphLabels;
		private JTextArea outputField;
		
		private boolean usedAsInputDialog;
		private UniverseAddress shownUA;
		private UniverseAddress resultUA;
		private AbstractInputPanel[] inputPanels;
		
		private static abstract class AbstractInputPanel extends JPanel {
			private static final long serialVersionUID = -2301492858089122177L;
			
			protected CoordinatesDialog parent;
		
			AbstractInputPanel(CoordinatesDialog parent) {
				this.parent = parent;
				setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),BorderFactory.createEmptyBorder(3,3,3,3)));
			}
			
			JPanel createButtonPanel(String btnTitle, ActionListener aL) {
				JPanel panel = new JPanel();
				panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),BorderFactory.createEmptyBorder(3,3,3,3)));
				JButton btnCompute = new JButton(btnTitle);
				//ActionListener aL = e->computeUniverseAddress();
				btnCompute.addActionListener(aL);
				panel.add(btnCompute);
				return panel;
			}
		
			abstract UniverseAddress computeUniverseAddress();
			abstract void setAddress(UniverseAddress ua);
		}
		
		public CoordinatesDialog(Window parent) {
			this(parent,false,"Compute Coordinates");
		}
		
		public CoordinatesDialog(Window parent, boolean usedAsInputDialog, String title) {
			super(parent,title,ModalityType.APPLICATION_MODAL);
			this.usedAsInputDialog = usedAsInputDialog;
			this.shownUA = null;
			this.resultUA = null;
			
			inputPanels = new AbstractInputPanel[4];
			GridBagLayout layout = new GridBagLayout();
			JPanel inputPanels = new JPanel(layout);
			addInputPanel(inputPanels, layout, this.inputPanels[0] = new InputAsCoords(this));
			addInputPanel(inputPanels, layout, this.inputPanels[1] = new InputAsSigBoostCode(this));
			addInputPanel(inputPanels, layout, this.inputPanels[2] = new InputAsPortalGlyphCode(this));
			addInputPanel(inputPanels, layout, this.inputPanels[3] = new InputAsAddress(this));
			
			JPanel portalGlyphPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,3,3));
			portalGlyphPanel.setBorder(BorderFactory.createEtchedBorder());
			glyphLabels = new JLabel[12];
			Dimension preferredSize = new Dimension(50,50);
			for (int i=0; i<glyphLabels.length; ++i) {
				glyphLabels[i] = new JLabel();
				glyphLabels[i].setPreferredSize(preferredSize);
				portalGlyphPanel.add(glyphLabels[i]);
			}
			
			outputField = new JTextArea();
			Border outsideBorder;
			if (this.usedAsInputDialog) outsideBorder = BorderFactory.createTitledBorder("Selected Address");
			else                        outsideBorder = BorderFactory.createEtchedBorder();
			outputField.setBorder(BorderFactory.createCompoundBorder(outsideBorder,BorderFactory.createEmptyBorder(3,3,3,3)));
			outputField.setPreferredSize(new Dimension(300,20));
			outputField.setEditable(false);
			
			JPanel contentPane = new JPanel(new BorderLayout(3,3));
			contentPane.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
			contentPane.add(inputPanels,BorderLayout.WEST);
			contentPane.add(outputField,BorderLayout.CENTER);
			contentPane.add(portalGlyphPanel,BorderLayout.SOUTH);

			JPanel dialogPanel = new JPanel(new BorderLayout(3,3));
			dialogPanel.add(contentPane,BorderLayout.CENTER);
			if (this.usedAsInputDialog) {
				JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,3,3));
				buttonPanel.add(SaveViewer.createButton("Ok", e->{resultUA = shownUA; closeDialog();}));
				buttonPanel.add(SaveViewer.createButton("Cancel", e->closeDialog()));
				dialogPanel.add(buttonPanel,BorderLayout.SOUTH);
			}
			
			super.createGUI( dialogPanel );
			super.setSizeAsMinSize();
//			super.setResizable(false);
		}

		private void addInputPanel(JPanel inputPanels, GridBagLayout layout, AbstractInputPanel inputPanel) {
			GridBagConstraints c = new GridBagConstraints();
			
			c.weightx=1;
			c.gridwidth=1;
			c.fill = GridBagConstraints.BOTH;
			layout.setConstraints(inputPanel, c);
			inputPanels.add(inputPanel);
			
			String btnTitle = this.usedAsInputDialog ? "Select" : "Compute";
			ActionListener aL = e->{
				UniverseAddress ua = inputPanel.computeUniverseAddress();
				if (ua!=null) {
					showAddress(ua);
					if (usedAsInputDialog)
						for (AbstractInputPanel panel:this.inputPanels)
							panel.setAddress(ua);
				}
			};
			
			JPanel buttonPanel = inputPanel.createButtonPanel(btnTitle,aL);
			c.weightx=0;
			c.gridwidth=GridBagConstraints.REMAINDER;
			c.fill = GridBagConstraints.BOTH;
			layout.setConstraints(buttonPanel, c);
			inputPanels.add(buttonPanel);
		}

		private void showAddress(UniverseAddress ua) {
			shownUA = ua;
			
			long portalGlyphCode = ua.getPortalGlyphCode();
			
			for (int i=11; i>=0; --i) {
				int nr = (int) (portalGlyphCode&0xF);
				portalGlyphCode = portalGlyphCode>>4;
				BufferedImage image = UniversePanel.PortalGlyphsIS.getCachedImage(nr);
				ImageIcon icon = image==null?null:new ImageIcon( image );
				glyphLabels[i].setIcon(icon);
			}
			
			outputField.setText(ua.getCoordinates());
			outputField.append("\r\n"+ua.getExtendedSigBoostCode());
			outputField.append("\r\n"+ua.getPortalGlyphCodeStr());
			outputField.append("\r\n"+ua.getAddressStr());
			outputField.append("\r\n    = "+ua.getAddress());
		}

		private void showError(String message) {
			outputField.setText(message);
		}
		
		public boolean hasResult() {
			if (!usedAsInputDialog) throw new UnsupportedOperationException("This is not an input dialog. Calling \"hasResult()\" is not allowed.");
			return resultUA!=null;
		}

		public long getResult() {
			if (!usedAsInputDialog) throw new UnsupportedOperationException("This is not an input dialog. Calling \"getResult()\" is not allowed.");
			return resultUA.getAddress();
		}

		private static class InputField extends JTextField {
			private static final long serialVersionUID = -4256186100798813519L;
			
			final static private Color bgcolor = UIManager.getLookAndFeelDefaults().getColor("TextField.background");
			final static private FocusListener bgResetter = new FocusListener() {
				@Override public void focusLost(FocusEvent e) {}
				@Override public void focusGained(FocusEvent e) {
					Component comp = e.getComponent();
					if (comp!=null) comp.setBackground(bgcolor);
				}
			};
			
			InputField(int prefWidth, int prefHeight) {
				addFocusListener(bgResetter);
				setPreferredSize(new Dimension(prefWidth,prefHeight));
			}

			private void getValue(TextFieldValue result, boolean forceHex) {
				String str = getText();
				if (forceHex) {
					try { result.parseHex(str); result.valueWasHex = true; }
					catch (NumberFormatException e) { result.parseError = true; }
				} else
				if (str.startsWith("0x")) {
					try { result.parseHex(str.substring("0x".length())); result.valueWasHex = true; }
					catch (NumberFormatException e) { result.parseError = true; }
				} else {
					try { result.parseDec(str); result.valueWasHex = false; }
					catch (NumberFormatException e) { result.parseError = true; }
				}
			}

			TextFieldValueLong getLongValue() { TextFieldValueLong result = new TextFieldValueLong(); getValue(result,false); return result; }
			TextFieldValueInt  getIntValue () { TextFieldValueInt  result = new TextFieldValueInt (); getValue(result,false); return result; }
			TextFieldValueLong getLongHexValue() { TextFieldValueLong result = new TextFieldValueLong(); getValue(result,true); return result; }
			@SuppressWarnings("unused")
			TextFieldValueInt  getIntHexValue () { TextFieldValueInt  result = new TextFieldValueInt (); getValue(result,true); return result; }

			void setError() {
				setBackground(Color.RED);
			}
		}

		private static abstract class TextFieldValue {
			boolean valueWasHex;
			boolean parseError;
			TextFieldValue() {
				valueWasHex = false;
				parseError = false;
			}
			abstract void parseHex(String str) throws NumberFormatException;
			abstract void parseDec(String str) throws NumberFormatException;
		}

		private static class TextFieldValueLong extends TextFieldValue {
			long value;
			TextFieldValueLong() { value = 0L; }
			@Override void parseHex(String str) throws NumberFormatException { value = Long.parseLong(str,16); }
			@Override void parseDec(String str) throws NumberFormatException { value = Long.parseLong(str); }
		}

		private static class TextFieldValueInt extends TextFieldValue {
			int value;
			TextFieldValueInt() { value = 0; }
			@Override void parseHex(String str) throws NumberFormatException { value = Integer.parseInt(str,16); }
			@Override void parseDec(String str) throws NumberFormatException { value = Integer.parseInt(str); }
		}

		private static class InputAsPortalGlyphCode extends AbstractInputPanel {
			private static final long serialVersionUID = -5005085383637770875L;
			private InputField galaxyIndex;
			private InputField portalGlyphCode;

			InputAsPortalGlyphCode(CoordinatesDialog parent) {
				super(parent);
				add(new JLabel("Galaxy:"));
				add(galaxyIndex = new InputField(35,20));
				add(new JLabel("PortalGlyphCode:"));
				add(portalGlyphCode = new InputField(200,20));
			}

			@Override void setAddress(UniverseAddress ua) {
				galaxyIndex.setText(""+ua.galaxyIndex);
				portalGlyphCode.setText(""+ua.getPortalGlyphCodeStr());
			}

			@Override UniverseAddress computeUniverseAddress() {
				TextFieldValueInt  galaxyIndexValue     = galaxyIndex    .getIntValue();
				TextFieldValueLong portalGlyphCodeValue = portalGlyphCode.getLongHexValue();
				boolean error = false;
				if (galaxyIndexValue    .parseError) { galaxyIndex    .setError(); error = true; }
				if (portalGlyphCodeValue.parseError) { portalGlyphCode.setError(); error = true; }
				if (error) {
					parent.showError("Wrong Input");
					return null;
				}
				long region = portalGlyphCodeValue.value & 0xFFFFFFFFL;
				long plnsys = (portalGlyphCodeValue.value>>32) & 0xFFFF;
				return new UniverseAddress( (plnsys<<40) | (((long)galaxyIndexValue.value&0xFF)<<32) | region);
			}
		}

		private static class InputAsAddress extends AbstractInputPanel {
			private static final long serialVersionUID = 5365096410288633609L;
			private InputField universeAddress;

			InputAsAddress(CoordinatesDialog parent) {
				super(parent);
				add(new JLabel("Universe Address:"));
				add(universeAddress = new InputField(200,20));
			}

			@Override void setAddress(UniverseAddress ua) {
				universeAddress.setText(ua.getAddressStr());
			}

			@Override UniverseAddress computeUniverseAddress() {
				TextFieldValueLong universeAddressValue = universeAddress.getLongValue();
				if (universeAddressValue.parseError) { universeAddress.setError(); parent.showError("Wrong Input"); return null; }
				return new UniverseAddress(universeAddressValue.value);
			}
		}

		private static class InputAsSigBoostCode extends AbstractInputPanel {
			private static final long serialVersionUID = -108501123032087816L;
			private InputField galaxyIndex;
			private InputField sigBoostCode;
			private InputField planetIndex;

			InputAsSigBoostCode(CoordinatesDialog parent) {
				super(parent);
				//setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
				add(new JLabel("Galaxy:"));
				add(galaxyIndex = new InputField(35,20));
				add(new JLabel("  Signal Booster Code:"));
				add(sigBoostCode = new InputField(150,20));
				add(new JLabel("  Planet:"));
				add(planetIndex = new InputField(35,20));
			}

			@Override void setAddress(UniverseAddress ua) {
				galaxyIndex.setText(""+ua.galaxyIndex);
				sigBoostCode.setText(ua.getSigBoostCode());
				planetIndex.setText(""+ua.planetIndex);
			}

			@Override UniverseAddress computeUniverseAddress() {
				TextFieldValueInt galaxyIndexValue = galaxyIndex .getIntValue();
				String            sigBoostCodeStr  = sigBoostCode.getText();
				TextFieldValueInt planetIndexValue = planetIndex .getIntValue();
				
				boolean error = false;
				if (planetIndexValue.parseError) { planetIndex.setError(); error = true; }
				if (galaxyIndexValue.parseError) { galaxyIndex.setError(); error = true; }
				
				boolean sigBoostCodeError = false;
				if (sigBoostCodeStr.length()<19) { sigBoostCode.setError(); sigBoostCodeError = true; }
				
				String[] strings = null;
				if (!sigBoostCodeError) {
					strings = sigBoostCodeStr.split(":");
					if (strings.length!=4) { sigBoostCode.setError(); sigBoostCodeError = true; }
				}
				
				long[] values = null;
				if (!sigBoostCodeError) {
					values = new long[strings.length];
					for (int i=0; i<strings.length; ++i)
						try { values[i] =Long.parseLong( strings[i], 16 ); }
						catch (NumberFormatException e) {
							sigBoostCode.setError(); sigBoostCodeError = true;
						}
				}
				
				if (error || sigBoostCodeError) {
					parent.showError("Wrong Input");
					return null;
				}
				
				int voxelX = (int) (values[0]-2047);
				int voxelY = (int) (values[1]-127);
				int voxelZ = (int) (values[2]-2047);
				int solarSystemIndex = (int) values[3];
				
				return new UniverseAddress(galaxyIndexValue.value, voxelX, voxelY, voxelZ, solarSystemIndex, planetIndexValue.value);
			}
		}

		private static class InputAsCoords extends AbstractInputPanel {
			private static final long serialVersionUID = 5940223603403241578L;
			
			private InputField galaxyIndex;
			private InputField regionVoxelX;
			private InputField regionVoxelY;
			private InputField regionVoxelZ;
			private InputField solarSystemIndex;
			private InputField planetIndex;
			
			InputAsCoords(CoordinatesDialog parent) {
				super(parent);
				//setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
				add(new JLabel("Galaxy:"));
				add(galaxyIndex = new InputField(35,20));
				add(new JLabel("  Region:"));
				add(regionVoxelX = new InputField(50,20));
				add(regionVoxelY = new InputField(40,20));
				add(regionVoxelZ = new InputField(50,20));
				add(new JLabel("  Solar System:"));
				add(solarSystemIndex = new InputField(50,20));
				add(new JLabel("  Planet:"));
				add(planetIndex = new InputField(35,20));
			}
		
			@Override void setAddress(UniverseAddress ua) {
				galaxyIndex.setText(""+ua.galaxyIndex);
				regionVoxelX.setText(""+ua.voxelX);
				regionVoxelY.setText(""+ua.voxelY);
				regionVoxelZ.setText(""+ua.voxelZ);
				solarSystemIndex.setText(""+ua.solarSystemIndex);
				planetIndex.setText(""+ua.planetIndex);
			}

			@Override UniverseAddress computeUniverseAddress() {
				TextFieldValueInt planetIndexValue      = planetIndex     .getIntValue();
				TextFieldValueInt solarSystemIndexValue = solarSystemIndex.getIntValue();
				TextFieldValueInt regionVoxelXValue     = regionVoxelX    .getIntValue();
				TextFieldValueInt regionVoxelYValue     = regionVoxelY    .getIntValue();
				TextFieldValueInt regionVoxelZValue     = regionVoxelZ    .getIntValue();
				TextFieldValueInt galaxyIndexValue      = galaxyIndex     .getIntValue();
				
				boolean error = false;
				if (planetIndexValue     .parseError) { planetIndex     .setError(); error = true; }
				if (solarSystemIndexValue.parseError) { solarSystemIndex.setError(); error = true; }
				if (regionVoxelXValue    .parseError) { regionVoxelX    .setError(); error = true; }
				if (regionVoxelYValue    .parseError) { regionVoxelY    .setError(); error = true; }
				if (regionVoxelZValue    .parseError) { regionVoxelZ    .setError(); error = true; }
				if (galaxyIndexValue     .parseError) { galaxyIndex     .setError(); error = true; }
				
				if (error) {
					parent.showError("Wrong Input");
					return null;
				}
				
				int voxelX = regionVoxelXValue.value;
				int voxelY = regionVoxelYValue.value;
				int voxelZ = regionVoxelZValue.value;
				if (regionVoxelXValue.valueWasHex) { if (voxelX>0x7FF) voxelX |= 0xFFFFF000; }
				if (regionVoxelYValue.valueWasHex) { if (voxelY> 0x7F) voxelY |= 0xFFFFFF00; }
				if (regionVoxelZValue.valueWasHex) { if (voxelZ>0x7FF) voxelZ |= 0xFFFFF000; }
				
				return new UniverseAddress(galaxyIndexValue.value, voxelX, voxelY, voxelZ, solarSystemIndexValue.value, planetIndexValue.value);
			}
		}
	}

	public static class NamedColorListMenu extends ListMenu<Images.NamedColor> {
		private static final long serialVersionUID = -7848156677924768189L;

		public NamedColorListMenu(String title, NamedColor[] values, NamedColor initialValue, ExternFunction<NamedColor> externFunctionality) {
			super(title, values, initialValue, externFunctionality);
		}

		@Override
		protected void configureMenuItem(JCheckBoxMenuItem menuItem, NamedColor value) {
			if (value!=null)
				menuItem.setIcon(new ImageIcon(NamedColor.createImage(value,20,13)));
		}
	}

	public static class ListMenu<ValueType> extends JMenu {
		private static final long serialVersionUID = 1243718139539544213L;
		private ExternFunction<ValueType> externFunctionality;
		private ButtonGroup buttonGroup;
		private ValueType selectedValue;

		public ListMenu(String title, ValueType[] values, ValueType initialValue, ExternFunction<ValueType> externFunctionality) {
			super(title);
			this.selectedValue = null;
			this.externFunctionality = externFunctionality;
			createMenuItems(values,initialValue);
		}

		private void createMenuItems(ValueType[] values, ValueType initialValue) {
			buttonGroup = new ButtonGroup();
			for (ValueType value:values) {
				
				boolean isChecked = externFunctionality.isEqual(initialValue,value);
				if (isChecked) selectedValue = value;
				
				String str = externFunctionality.toString(value);
				JCheckBoxMenuItem menuItem = new ValueMenuItem<ValueType>(value,str,isChecked);
				configureMenuItem(menuItem,value);
				menuItem.addActionListener(e->{
					selectedValue = value;
					externFunctionality.setResult(value);
				});
				
				buttonGroup.add(menuItem);
				add(menuItem);
			}
		}

		protected void configureMenuItem(JCheckBoxMenuItem menuItem, ValueType value) {}

		public void updateValues(ValueType[] values) {
			removeAll();
			createMenuItems(values,selectedValue);
		}
		
		public void clearSelection() {
			buttonGroup.clearSelection();
		}
		
		public void setValue(ValueType value) {
			Enumeration<AbstractButton> menuItems = buttonGroup.getElements();
			while (menuItems.hasMoreElements()) {
				AbstractButton abstractButton = menuItems.nextElement();
				if (!(abstractButton instanceof ValueMenuItem<?>)) continue;
				@SuppressWarnings("unchecked")
				ValueMenuItem<ValueType> menuItem = (ValueMenuItem<ValueType>)abstractButton;
				if (externFunctionality.isEqual(value,menuItem.value)) {
					menuItem.setSelected(true);
					return;
				}
			}
			buttonGroup.clearSelection();
		}
		
		private static class ValueMenuItem<T> extends JCheckBoxMenuItem {
			private static final long serialVersionUID = 6905578523280731223L;
			private T value;

			public ValueMenuItem(T value, String str, boolean isChecked) {
				super(str, isChecked);
				this.value = value;
			}
			
		}

		public static abstract class ExternFunction<T> {
			public abstract void setResult(T value);
			public String toString(T value) { return value==null?"":value.toString(); }
			public boolean isEqual(T v1, T v2) { return v1==v2; }
		}
		
	}

	public static class ContextMenuInvoker implements MouseListener {
		
		private Component invoker;
		private JPopupMenu contextMenu;
		
		public ContextMenuInvoker(Component invoker, JPopupMenu contextMenu) {
			this.invoker = invoker;
			this.contextMenu = contextMenu;
			invoker.addMouseListener(this);
		}
		
		@Override public void mousePressed(MouseEvent e) {}
		@Override public void mouseReleased(MouseEvent e) {}
		@Override public void mouseEntered(MouseEvent e) {}
		@Override public void mouseExited(MouseEvent e) {}
		@Override public void mouseClicked(MouseEvent e) {
			if (e.getButton()==MouseEvent.BUTTON3) {
				contextMenu.show(invoker, e.getX(), e.getY());
			}
		}
	}
}
