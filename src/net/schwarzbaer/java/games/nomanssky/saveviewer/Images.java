package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.MouseInputAdapter;

import net.schwarzbaer.gui.Canvas;
import net.schwarzbaer.gui.GUI;
import net.schwarzbaer.gui.StandardDialog;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView;

public class Images {
	private static final String FILE_COLORS = "NMS_Viewer.Colors.txt";
	
	public NamedColor[] colorValues;
	private HashMap<Integer,NamedColor> colorMap; 
	private final Vector<ColorListListender> colorListListenders;
	
	public String[] imagesNames;
	public final HashMap<String,BufferedImage> images;
	
	public Images() {
		colorValues = null;
		colorMap = null;
		colorListListenders = new Vector<>();
		imagesNames = null;
		images = new HashMap<String,BufferedImage>();
	}
	
	public void init() {
		prepareColors();
		readImages();
	}
	
	private void prepareColors() {
		
		colorMap = new HashMap<>();
		Vector<NamedColor> colorValuesVec = new Vector<>(); 
		
		addColor(colorValuesVec, 0xBB392C, "Isotop" );
		addColor(colorValuesVec, 0xFFC456, "Oxid" );
		addColor(colorValuesVec, 0x0249A1, "Silikat" );
		addColor(colorValuesVec, 0x5DCD93, "Neutrales Element" );
		addColor(colorValuesVec, 0x5A6F36, "Pflanze 1" );
		addColor(colorValuesVec, 0x4B2A57, "Pflanze 2" );
		addColor(colorValuesVec, 0x4D585E, null );
		addColor(colorValuesVec, 0x1C364D, "Nanit-Haufen" );
		addColor(colorValuesVec, 0x10805C, "Völker-Geschenk" );
		addColor(colorValuesVec, 0xF0A92B, "Produkt" );
		
		loadColorsFromFile(colorValuesVec);
		
		colorValues = colorValuesVec.toArray(new NamedColor[0]);
	}

	private void loadColorsFromFile(Vector<NamedColor> colorValuesVec) {
		File file = new File(FILE_COLORS);
		if (!file.isFile()) return;
		
		long start = System.currentTimeMillis();
		System.out.println("Read background colors from file \""+file.getPath()+"\"...");
		String str;
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8))) {
			while ((str=in.readLine())!=null) {
				int pos = str.indexOf('=');
				if (pos<0) continue;
				
				String valueStr = str.substring(0, pos);
				String nameStr = str.substring(pos+1);
				
				NamedColor newColor;
				try { newColor = new NamedColor(Integer.parseInt(valueStr,16),nameStr); }
				catch (NumberFormatException e) { continue; }
				
				NamedColor existingColor = colorMap.putIfAbsent(newColor.value, newColor);
				if (existingColor!=null) {
					if (!existingColor.name.equals(newColor.name)) {
						System.out.printf("   change name of %s into %s\r\n", existingColor, newColor.name);
						existingColor.name = newColor.name;
					}
				} else {
					System.out.printf("   %s added\r\n", newColor);
					colorValuesVec.add(newColor);
				}
			}
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
		
		System.out.println("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}

	private void saveColorsToFile() {
		long start = System.currentTimeMillis();
		File file = new File(FILE_COLORS);
		System.out.println("Write background colors to file \""+file.getPath()+"\"...");
		
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8));) {
			for (NamedColor color:colorValues)
				if (color!=null)
					out.printf("%06X=%s\r\n",color.value,color.name);
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		
		System.out.println("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}

	public NamedColor getColor(Integer value) {
		if (value == null) return null;
		NamedColor color = colorMap.get(value);
		if (color==null) color = new NamedColor(value,String.format("%06X", value));
		return color;
	}
	
	private void addColor(Vector<NamedColor> colorValuesVec, int value, String name) {
		if (name==null) name=String.format("[%d]%06X", colorValuesVec.size()+1, value);
		NamedColor namedColor = new NamedColor(value,name);
		colorMap.put(value, namedColor);
		colorValuesVec.add(namedColor);
	}
	
	private NamedColor addColor(int value, String name) {
		if (colorMap.containsKey(value)) return null;
		if (name.isEmpty()) name = String.format("%06X", value);
		NamedColor namedColor = new NamedColor(value,name);
		colorMap.put(value, namedColor);
		colorValues = Arrays.copyOf(colorValues, colorValues.length+1);
		colorValues[colorValues.length-1] = namedColor;
		return namedColor;
	}
	
	public static interface ColorListListender {
		public void colorAdded(NamedColor color);
	}
	
	public void addColorListListender(ColorListListender cll) {
		colorListListenders.add(cll);
	}
	
	public void removeColorListListender(ColorListListender cll) {
		colorListListenders.remove(cll);
	}
	
	public void showAddColorDialog(Window parent, String title) {
		AddColorDialog dlg = new AddColorDialog(parent, title);
		dlg.showDialog();
		if (dlg.hasResult()) {
			NamedColor color = dlg.getResult();
			color = addColor(color.value, color.name);
			if (color!=null) {
				for (ColorListListender ccl:colorListListenders)
					ccl.colorAdded(color);
				saveColorsToFile();
			}
		}
	}
	
	public static class AddColorDialog extends StandardDialog {
		private static final long serialVersionUID = 3667827984599580401L;
		private ColorView colorView;
		private JTextField nameField;
		private JTextField rgbField;
		
		private Integer value;
		private NamedColor result;

		public AddColorDialog(Window parent, String title) {
			super(parent, title, ModalityType.APPLICATION_MODAL);
			
			value = null;
			result = null;
			
			nameField = new JTextField(10);
			rgbField = new JTextField(10);
			Color defaultBGColor = rgbField.getBackground();
			rgbField.addFocusListener(new FocusListener() {
				@Override public void focusGained(FocusEvent e) {
					rgbField.setBackground(defaultBGColor);
				}
				@Override public void focusLost(FocusEvent e) {
					try {
						value = Integer.parseInt(rgbField.getText(),16);
						colorView.setColor(new Color(value));
					}
					catch (NumberFormatException e1) {
						rgbField.setBackground(Color.RED);
						colorView.setColor(null);
						value = null;
					}
				}
			});
			
			GridBagConstraints c = new GridBagConstraints();
			GridBagLayout fieldLayout = new GridBagLayout();
			JPanel fieldPanel = new JPanel();
			fieldPanel.setLayout(fieldLayout);
			addComp(fieldPanel,fieldLayout,c, new JLabel("Name : ")  , 0,0,1                           ,GridBagConstraints.BOTH);
			addComp(fieldPanel,fieldLayout,c, nameField              , 1,0,GridBagConstraints.REMAINDER,GridBagConstraints.BOTH);
			addComp(fieldPanel,fieldLayout,c, new JLabel("RRGGBB : "), 0,0,1                           ,GridBagConstraints.BOTH);
			addComp(fieldPanel,fieldLayout,c, rgbField               , 1,0,GridBagConstraints.REMAINDER,GridBagConstraints.BOTH);
			
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
			buttonPanel.add(SaveViewer.createButton("Ok", e->{if (value!=null) result = new NamedColor(value, nameField.getText()); closeDialog(); }));
			buttonPanel.add(SaveViewer.createButton("Cancel", e->{ closeDialog(); }));
			
			JPanel contentPane = new JPanel(new BorderLayout(3,3));
			contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
			contentPane.add(fieldPanel,BorderLayout.CENTER);
			contentPane.add(colorView = new ColorView(100,50),BorderLayout.EAST);
			contentPane.add(buttonPanel,BorderLayout.SOUTH);
			
			createGUI(contentPane);
			setSizeAsMinSize();
		}
		
		private void addComp(JPanel panel, GridBagLayout layout, GridBagConstraints c, Component comp, double weightx, double weighty, int gridwidth, int fill) {
			c.weightx=weightx;
			c.weighty=weighty;
			c.gridwidth=gridwidth;
			c.fill = fill;
			layout.setConstraints(comp, c);
			panel.add(comp);
		}

		public NamedColor getResult() {
			return result;
		}

		public boolean hasResult() {
			return result!=null;
		}

		private static class ColorView extends Canvas {
			private static final long serialVersionUID = -569349695124967737L;
			private Color color;

			public ColorView(int width, int height) {
				color = null;
				setPreferredSize(width,height);
			}
			
			public void setColor(Color color) {
				this.color = color;
				repaint();
			}

			@Override protected void paintCanvas(Graphics g, int width, int height) {
				g.setColor(Color.GRAY);
				g.drawRect(0, 0, width-1, height-1);
				if (color==null) {
					g.drawLine(0, 0, width-1, height-1);
					g.drawLine(width-1, 0, 0, height-1);
				} else {
					g.setColor(color);
					g.fillRect(1, 1, width-2, height-2);
				}
			}
		}
		
	}

	private void readImages() {
		long start = System.currentTimeMillis();
		File folder = new File("extra/resource_icons");
		System.out.println("Read image resources from \""+folder.getPath()+"\" ...");
		if (!folder.isDirectory()) {
			System.out.println("   ... abort reading. Can't open folder.");
			return;
		}
		
		imagesNames = folder.list(new FilenameFilter() {
			@Override public boolean accept(File dir, String name) {
				name = name.toLowerCase();
				if (name.endsWith(".png")) return true;
				if (name.endsWith(".jpg")) return true;
				if (name.endsWith(".jpeg")) return true;
				return false;
			}
		});
		Arrays.sort(imagesNames);
		
		images.clear();
		for (int i=0; i<imagesNames.length; ++i) {
			File file = new File(folder,imagesNames[i]);
			//InputStream stream = images.getClass().getResourceAsStream("/icons/"+imagesNames[i]);
			BufferedImage image = null;
			//if (file!=null)
				try { image = ImageIO.read(file); }
				catch (IOException e) {}
			if (image!=null)
				images.put(imagesNames[i], image);
		}
		
		System.out.println("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}

	public BufferedImage[] getImages(boolean sorted) {
		if (!sorted)
			return images.values().toArray(new BufferedImage[0]);
		
		BufferedImage[] array = new BufferedImage[imagesNames.length];
		for (int i=0; i<imagesNames.length; ++i)
			array[i] = images.get(imagesNames[i]);
		
		return array;
	}

	public BufferedImage getImage(String imageFileName, Integer imageBackground, int width, int height) {
		BufferedImage baseImage = null;
		if (imageFileName!=null)
			baseImage = images.get(imageFileName);
		
		if (imageBackground!=null || (baseImage!=null && (width!=baseImage.getWidth() || height!=baseImage.getHeight()))) {
			if (width <0) width  = (baseImage==null)?256:baseImage.getWidth();
			if (height<0) height = (baseImage==null)?256:baseImage.getHeight();
			BufferedImage combinedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics g = combinedImage.getGraphics();
			if (imageBackground!=null) {
				g.setColor(new Color(imageBackground));
				g.fillRect(0,0,width,height);
			}
			if (baseImage!=null) {
				if (width==baseImage.getWidth() && height==baseImage.getHeight())
					g.drawImage(baseImage,0,0,null);
				else {
					if (g instanceof Graphics2D) {
						Graphics2D g2 = (Graphics2D)g;
						g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
						g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
						g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
						g2.drawImage(baseImage,0,0,width,height,null);
					} else
						g.drawImage(baseImage,0,0,width,height,null);
				}
			}
			return combinedImage;
		} else
			return baseImage;
	}

	public static class NamedColor {
	
		public final int value;
		public String name;
		public final Color color;
	
		public NamedColor(int value, String name) {
			this.value = value;
			this.color = new Color(value);
			this.name = name;
		}

		@Override
		public String toString() {
			return String.format("Color( %06X, \"%s\" )",value,name);
		}
	}

	public static class IdImageDialog extends StandardDialog {
		private static final long serialVersionUID = -4493777651637626630L;
		
		private JLabel imageField;
		private JTextArea textarea;
		
		private GameInfos.GeneralizedID id;
		private boolean hasDataChanged;

		private ColorListListender colorListListender;
	
		public IdImageDialog(Window parent, GameInfos.GeneralizedID id) {
			super(parent, String.format("Set Image and Background for %s",id.getName()), ModalityType.APPLICATION_MODAL, false);
			
			this.id = new GameInfos.GeneralizedID(id);
			this.hasDataChanged = false;
			
			textarea = new JTextArea();
			textarea.setEditable(false);
			JScrollPane textareaScrollPane = new JScrollPane(textarea);
			textareaScrollPane.getViewport().setPreferredSize(new Dimension(400, 100));
			
			Vector<String> images = new Vector<>(Arrays.asList(SaveViewer.images.imagesNames));
			images.insertElementAt("",0);
			JComboBox<String> cmbbxImages = new JComboBox<String>(images);
			cmbbxImages.setSelectedItem(id.getImageFileName());
			cmbbxImages.addActionListener(e->setImageFileName((String)cmbbxImages.getSelectedItem()));
			
			Vector<NamedColor> colors = new Vector<>(Arrays.asList(SaveViewer.images.colorValues));
			colors.insertElementAt(null,0);
			JComboBox<NamedColor> cmbbxColors = new JComboBox<NamedColor>(new DefaultComboBoxModel<NamedColor>(colors));
			cmbbxColors.setRenderer(new TableView.NamedColorRenderer());
			cmbbxColors.setSelectedItem(SaveViewer.images.getColor(id.getImageBG()));
			cmbbxColors.addActionListener(e->setImageBGColor((NamedColor)cmbbxColors.getSelectedItem()));
			
			colorListListender = new ColorListListender() {
				@Override public void colorAdded(NamedColor color) {
					cmbbxColors.addItem(color);
					cmbbxColors.revalidate();
				}
			};
			
			JPanel buttonPanel = new JPanel(new GridLayout(1,0,3,3));
			buttonPanel.add(createButton("Apply" ,e->{closeDialog();}));
			buttonPanel.add(createButton("Cancel",e->{hasDataChanged = false; closeDialog();}));
			
			JPanel cmbbxPanel = new JPanel(new GridLayout(0,1,3,3));
			cmbbxPanel.add(GUI.createRightAlignedPanel(createButton("select ...",e->showImageList(cmbbxImages)), cmbbxImages));
			cmbbxPanel.add(GUI.createRightAlignedPanel(createButton("Add Color",e->SaveViewer.images.showAddColorDialog(IdImageDialog.this,"Add Color")), cmbbxColors));
			
			JPanel inputPanel = new JPanel(new BorderLayout(3,3));
			inputPanel.add(cmbbxPanel, BorderLayout.CENTER);
			inputPanel.add(buttonPanel, BorderLayout.SOUTH);
			
			JPanel centerPanel = new JPanel(new BorderLayout(3,3));
			centerPanel.add(textareaScrollPane, BorderLayout.CENTER);
			centerPanel.add(inputPanel, BorderLayout.SOUTH);
			
			imageField = new JLabel();
			imageField.setBorder(BorderFactory.createEtchedBorder());
			imageField.setPreferredSize(new Dimension(256,256));
			
			JPanel contentPane = new JPanel(new BorderLayout(3,3));
			contentPane.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
			contentPane.add(centerPanel,BorderLayout.WEST);
			contentPane.add(imageField,BorderLayout.CENTER);
			
			showValues();
			this.createGUI(contentPane);
		}
		
		@Override public void windowOpened(WindowEvent e) { SaveViewer.images.addColorListListender   (colorListListender); System.out.println("add"); }
		@Override public void windowClosed(WindowEvent e) { SaveViewer.images.removeColorListListender(colorListListender); System.out.println("remove"); }

		private void showImageList(JComboBox<String> cmbbxImages) {
			ImageGridDialog dlg = new ImageGridDialog(this,id.getImageFileName());
			dlg.showDialog();
			if (dlg.hasChoosen()) {
				String result = dlg.getImageFileName();
				setImageFileName(result);
				cmbbxImages.setSelectedItem(result);
			}
		}
	
		private JButton createButton(String title, ActionListener l) {
			JButton button = new JButton(title);
			button.addActionListener(l);
			return button;
		}
	
		private void setImageBGColor(NamedColor namedColor) {
			id.setImageBG(namedColor==null?null:namedColor.value);
			hasDataChanged = true;
			showValues();
		}
	
		private void setImageFileName(String filename) {
			id.setImageFileName(filename);
			hasDataChanged = true;
			showValues();
		}
	
		private void showValues() {
			textarea.setText("");
			
			textarea.append("ID     : "+id.id+"\r\n");
			if (!id.label.isEmpty()) textarea.append("Label  : "+id.label+"\r\n");
			textarea.append("Image  : "+(id.hasImage  ()?id.getImageFileName():"<none>")+"\r\n");
			textarea.append("ImageBG: "+(id.hasImageBG()?String.format("%06X",id.getImageBG()):"<none>")+"\r\n");
			
			BufferedImage image = id.getImage();
			if (image==null) {
				imageField.setIcon(null);
			} else {
				imageField.setIcon(new ImageIcon(image));
				imageField.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));							
			}
		}
	
		public boolean hasDataChanged() { return hasDataChanged; }
		public Integer getImageBG() { return id.getImageBG(); }
		public String getImageFileName() { return id.getImageFileName(); }
	}

	public static class ImageGridDialog extends StandardDialog {
		private static final long serialVersionUID = -3724853350437145460L;
		private static Color COLOR_BACKGRIOUND = null;
		private static Color COLOR_BACKGRIOUND_SELECTED = null;
		private static Color COLOR_BACKGRIOUND_PRESELECTED = null;
		private static Color COLOR_FOREGRIOUND = null;
		private static Color COLOR_FOREGRIOUND_SELECTED = null;
		
		private String selected;
	
		public ImageGridDialog(Window parent, String initialValue) {
			super(parent,"Select Image",ModalityType.APPLICATION_MODAL);
			
			selected = null;
			
			ImageLabel.defaultFont = new JLabel().getFont();
			JTextArea dummy = new JTextArea();
			COLOR_BACKGRIOUND = dummy.getBackground();
			COLOR_FOREGRIOUND = dummy.getForeground();
			COLOR_BACKGRIOUND_SELECTED = dummy.getSelectionColor();
			COLOR_FOREGRIOUND_SELECTED = dummy.getSelectedTextColor();
			COLOR_BACKGRIOUND_PRESELECTED = brighter(COLOR_BACKGRIOUND_SELECTED,0.7f);
			
			
			JPanel imagePanel = new JPanel(new GridLayout(0,6,0,0));
			imagePanel.setBorder(BorderFactory.createEtchedBorder());
			imagePanel.setBackground(COLOR_BACKGRIOUND);
			
			JScrollPane imageScrollPane = new JScrollPane(imagePanel);
			imageScrollPane.setPreferredSize(new Dimension(700,600));
			
			for (int i=0; i<SaveViewer.images.imagesNames.length; ++i) {
				String name = SaveViewer.images.imagesNames[i];
				BufferedImage image = SaveViewer.images.getImage(name,null,64,64);
				if (image!=null)
					imagePanel.add(new ImageLabel(this,name,image,name.equals(initialValue)));
			}
			
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			buttonPanel.add(SaveViewer.createButton("Choose \"No Image\"",e->setResult("")));
			buttonPanel.add(SaveViewer.createButton("Cancel",e->closeDialog()));
			
			JPanel contentPane = new JPanel(new BorderLayout(3,3));
			contentPane.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
			contentPane.add(imageScrollPane,BorderLayout.CENTER);
			contentPane.add(buttonPanel,BorderLayout.SOUTH);
			
			this.createGUI(contentPane);
		}
	
		private Color brighter(Color color, float fraction) {
			int r = color.getRed();
			int g = color.getGreen();
			int b = color.getBlue();
			r = Math.min(255, Math.round(255-(255-r)*(1-fraction)));
			g = Math.min(255, Math.round(255-(255-g)*(1-fraction)));
			b = Math.min(255, Math.round(255-(255-b)*(1-fraction)));
			return new Color(r,g,b);
		}
	
		private void setResult(String name) {
			selected = name;
			closeDialog();
		}
	
		public String getImageFileName() {
			return selected;
		}
	
		public boolean hasChoosen() {
			return selected != null;
		}
	
		private static final class ImageLabel extends JPanel {
			private static final long serialVersionUID = 4629632101041946456L;
			public static Font defaultFont;
	
			public ImageLabel(ImageGridDialog parent, String name, BufferedImage image, boolean isPreSelected) {
				super(new BorderLayout(3,3));
				setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
				
				JTextArea textArea = new JTextArea(name);
				textArea.setPreferredSize(new Dimension(100,60));
				textArea.setLineWrap(true);
				textArea.setWrapStyleWord(false);
				textArea.setEditable(false);
				textArea.setFont(defaultFont);
				textArea.setBackground(null);
				MouseListener[] mouseListeners = textArea.getMouseListeners();
				MouseMotionListener[] mouseMotionListeners = textArea.getMouseMotionListeners();
				for (MouseListener l:mouseListeners) textArea.removeMouseListener(l);
				for (MouseMotionListener l:mouseMotionListeners) textArea.removeMouseMotionListener(l);
				
				
				add(new JLabel(new ImageIcon(image)),BorderLayout.NORTH);
				add(textArea,BorderLayout.CENTER);
				
				MouseInputAdapter m = new MouseInputAdapter() {
					@Override public void mouseClicked(MouseEvent e) { parent.setResult(name); }
					@Override public void mouseEntered(MouseEvent e) { setBackground(COLOR_BACKGRIOUND_SELECTED); textArea.setForeground(COLOR_FOREGRIOUND_SELECTED); }
					@Override public void mouseExited (MouseEvent e) { setBackground(isPreSelected?COLOR_BACKGRIOUND_PRESELECTED:COLOR_BACKGRIOUND); textArea.setForeground(COLOR_FOREGRIOUND); }
				};
				
				setBackground(isPreSelected?COLOR_BACKGRIOUND_PRESELECTED:COLOR_BACKGRIOUND);
				addMouseListener(m);
				addMouseMotionListener(m);
				textArea.addMouseListener(m);
				textArea.addMouseMotionListener(m);
			}
		
		}
	
	}
}
