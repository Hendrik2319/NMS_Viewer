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
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.MouseInputAdapter;

import net.schwarzbaer.gui.Canvas;
import net.schwarzbaer.gui.StandardDialog;

public class Images {
	private static final String FILE_COLORS = "NMS_Viewer.Colors.txt";
	
	public NamedColor[] colorValues;
	private final HashMap<Integer,NamedColor> colorMap; 
	private final Vector<ColorListListender> colorListListenders;
	
	public String[] imagesNames;
	private final HashMap<String,BufferedImage> images;
	private final Vector<ImageListListender> imageListListenders;

	
	public Images() {
		colorValues = null;
		colorMap = new HashMap<>();
		colorListListenders = new Vector<>();
		imagesNames = null;
		images = new HashMap<String,BufferedImage>();
		imageListListenders = new Vector<>();
	}
	
	public void init() {
		prepareColors();
		readImages();
	}
	
	private void prepareColors() {
		
		colorMap.clear();
		Vector<NamedColor> colorValuesVec = new Vector<>(); 
		
		addColor(colorValuesVec, 0xBB392C, "Isotop" );
		addColor(colorValuesVec, 0xFFC456, "Oxid" );
		addColor(colorValuesVec, 0x0249A1, "Silikat" );
		addColor(colorValuesVec, 0x5DCD93, "Neutral" );
		addColor(colorValuesVec, 0x701781, "Exotisch" );
		addColor(colorValuesVec, 0x5A6F36, "Pflanze Oliv" );
		addColor(colorValuesVec, 0x4B2A57, "Pflanze Violet" );
		addColor(colorValuesVec, 0xC68C1E, "Pflanze Gelb" );
		addColor(colorValuesVec, 0x1E4FD0, "Pflanze Blau" );
		addColor(colorValuesVec, 0x00A64C, "Pflanze Grün" );
		addColor(colorValuesVec, 0xB74418, "Pflanze Rot" );
		addColor(colorValuesVec, 0x78502D, "Pflanze Braun" );
		addColor(colorValuesVec, 0x1C364D, "Nanit-Haufen" );
		addColor(colorValuesVec, 0x10805C, "Völker-Geschenk" );
		addColor(colorValuesVec, 0xF0A92B, "Produkt" );
		addColor(colorValuesVec, 0xC11746, "Energie" );
		addColor(colorValuesVec, 0x085C78, "Tech 1" );
		addColor(colorValuesVec, 0x0063B6, "Tech 2" );
		addColor(colorValuesVec, 0x236D4C, "Waffe Impulswerfer" );
		addColor(colorValuesVec, 0x19BC79, "Waffe Impulswerfer Upgrade" );
		addColor(colorValuesVec, 0x495746, "Waffe Minenlaser" );
		addColor(colorValuesVec, 0x5B9352, "Waffe Minenlaser Upgrade" );
		addColor(colorValuesVec, 0x0D81A8, "Waffe Blitzwerfer Upgrade" );		
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
	
	public static interface ImageListListender {
		public void imageListChanged();
	}
	
	public void addImageListListender(ImageListListender ill) {
		imageListListenders.add(ill);
	}
	
	public void removeImageListListender(ImageListListender ill) {
		imageListListenders.remove(ill);
	}

	public void reloadImageList() {
		readImages();
		for (ImageListListender ill:imageListListenders)
			ill.imageListChanged();
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

		public static BufferedImage createImage(NamedColor value, int width, int height) {
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics g = image.getGraphics();
			g.setColor(value.color);
			g.fillRect(0,0,width,height);
			return image;
		}
	}

	public static class ImageGridDialog extends StandardDialog {
		private static final long serialVersionUID = -3724853350437145460L;
		private static Color COLOR_BACKGRIOUND = null;
		private static Color COLOR_BACKGRIOUND_SELECTED = null;
		private static Color COLOR_BACKGRIOUND_PRESELECTED = null;
		private static Color COLOR_FOREGRIOUND = null;
		private static Color COLOR_FOREGRIOUND_SELECTED = null;
		
		private String selected;
		private int cols;
		private int preselectedIndex;
		private JScrollPane imageScrollPane;
	
		public ImageGridDialog(Window parent, String title, String initialValue) {
			super(parent,title,ModalityType.APPLICATION_MODAL);
			
			selected = null;
			
			ImageLabel.defaultFont = new JLabel().getFont();
			JTextArea dummy = new JTextArea();
			COLOR_BACKGRIOUND = dummy.getBackground();
			COLOR_FOREGRIOUND = dummy.getForeground();
			COLOR_BACKGRIOUND_SELECTED = dummy.getSelectionColor();
			COLOR_FOREGRIOUND_SELECTED = dummy.getSelectedTextColor();
			COLOR_BACKGRIOUND_PRESELECTED = brighter(COLOR_BACKGRIOUND_SELECTED,0.7f);
			
			cols = 6;
			JPanel imagePanel = new JPanel(new GridLayout(0,cols,0,0));
			imagePanel.setBorder(BorderFactory.createEtchedBorder());
			imagePanel.setBackground(COLOR_BACKGRIOUND);
			
			preselectedIndex = -1;
			for (int i=0; i<SaveViewer.images.imagesNames.length; ++i) {
				String name = SaveViewer.images.imagesNames[i];
				BufferedImage image = SaveViewer.images.getImage(name,null,64,64);
				if (image!=null) {
					boolean isPreSelected = name.equals(initialValue);
					imagePanel.add(new ImageLabel(this,name,image,isPreSelected));
					if (isPreSelected) preselectedIndex=i;
				}
			}
			
			imageScrollPane = new JScrollPane(imagePanel);
			imageScrollPane.setPreferredSize(new Dimension(700,600));
			
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
	
		@Override
		public void windowOpened(WindowEvent e) {
			if (preselectedIndex>=0) {
				int row = preselectedIndex/cols;
				int rowCount = Math.round((float)Math.ceil(SaveViewer.images.imagesNames.length*1.0f/cols));
				//System.out.printf("Row %d/%d was preselected\r\n",row,rowCount);
				
				JScrollBar scrollBar = imageScrollPane.getVerticalScrollBar();
				int val = scrollBar.getValue();
				int max = scrollBar.getMaximum();
				int min = scrollBar.getMinimum();
				int ext = scrollBar.getVisibleAmount();
				//System.out.printf("VerticalScrollBar is at %d..%d(%d)..%d \r\n",min,val,ext,max);
				
				int h = (max-min)/rowCount;
				//System.out.printf("h = %d \r\n",h);
				val = row*h - (ext-h)/2 + min;
				//System.out.printf("val = %d \r\n",val);
				val = Math.max(min,val);
				val = Math.min(max-ext,val);
				
				scrollBar.setValue(val);
				//System.out.printf("VerticalScrollBar set to %d..%d(%d)..%d \r\n",min,val,ext,max);
			}
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
