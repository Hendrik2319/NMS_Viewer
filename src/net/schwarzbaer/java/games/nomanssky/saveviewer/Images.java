package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.schwarzbaer.gui.Canvas;
import net.schwarzbaer.gui.IconSource;
import net.schwarzbaer.gui.ProgressDialog;
import net.schwarzbaer.gui.StandardDialog;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.IDMap;

public class Images {
	
	public NamedColor[] colorValues;
	private final HashMap<Integer,NamedColor> colorMap; 
	private final Vector<ColorListListender> colorListListenders;
	
	public String[] imagesNames;
	private final HashMap<String,BufferedImage> images;
	private final Vector<ImageListListener> imageListListeners;

	
	public Images() {
		colorValues = null;
		colorMap = new HashMap<>();
		colorListListenders = new Vector<>();
		imagesNames = null;
		images = new HashMap<String,BufferedImage>();
		imageListListeners = new Vector<>();
	}
	
	public void init() {
		prepareColors();
		readImages(null);
		UpgradeCategoryImages.init();
	}
	
	private void prepareColors() {
		
		colorMap.clear();
		Vector<NamedColor> colorValuesVec = new Vector<>(); 
		
		addColor(colorValuesVec, 0xBB392C, "Isotop" );
		addColor(colorValuesVec, 0xFFC456, "Oxid" );
		addColor(colorValuesVec, 0x0249A1, "Silikat" );
		addColor(colorValuesVec, 0x5DCD93, "Neutral" );
		addColor(colorValuesVec, 0x701781, "Exotisch" );
		addColor(colorValuesVec, 0x4B2A57, "Pflanze Violet" );
		addColor(colorValuesVec, 0x542640, "Pflanze Violet1" );
		addColor(colorValuesVec, 0x1E4FD0, "Pflanze Blau" );
		addColor(colorValuesVec, 0x218CAC, "Pflanze Sd" );
		addColor(colorValuesVec, 0x00A64C, "Pflanze Grün" );
		addColor(colorValuesVec, 0x1EB808, "Pflanze Grün1" );
		addColor(colorValuesVec, 0x5A6F36, "Pflanze Oliv" );
		addColor(colorValuesVec, 0x78502D, "Pflanze Braun" );
		addColor(colorValuesVec, 0xC68C1E, "Pflanze Gelb" );
		addColor(colorValuesVec, 0xB74418, "Pflanze Rot" );
		addColor(colorValuesVec, 0x236D4C, "Waffe Impulswerfer" );
		addColor(colorValuesVec, 0x19BC79, "Waffe Impulswerfer Upgrade" );
		addColor(colorValuesVec, 0x495746, "Waffe Minenlaser" );
		addColor(colorValuesVec, 0x0D81A8, "Waffe Blitzwerfer Upgrade" );		
		addColor(colorValuesVec, 0x903031, "Waffe Glutspeer" );
		addColor(colorValuesVec, 0xE84E4C, "Waffe Glutspeer Upgrade" );
		addColor(colorValuesVec, 0x937030, "Waffe Streublaster" );
		addColor(colorValuesVec, 0xFFBF37, "Waffe Streublaster Upgrade" );
		addColor(colorValuesVec, 0x2E999F, "Waffe Plasmawerfer" );
		addColor(colorValuesVec, 0x7D4665, "Waffe Zyklotron-B. Upgrade" );
		addColor(colorValuesVec, 0x7E0100, "Rohstoff Cd" );
		addColor(colorValuesVec, 0xBB3830, "Rohstoff O2" );
		addColor(colorValuesVec, 0xD92600, "Rohstoff P" );
		addColor(colorValuesVec, 0xF36D16, "Rohstoff Na" );
		addColor(colorValuesVec, 0xE57000, "Rohstoff Py" );
		addColor(colorValuesVec, 0xE88F00, "Rohstoff Cu" );
		addColor(colorValuesVec, 0xFFAD00, "Rohstoff U" );
		addColor(colorValuesVec, 0xA96E06, "Rohstoff Au" );
		addColor(colorValuesVec, 0x2D0400, "Rohstoff Ch" );
		addColor(colorValuesVec, 0x4D414F, "Rohstoff Pf" );
		addColor(colorValuesVec, 0x4D3780, "Rohstoff Rn" );
		addColor(colorValuesVec, 0x386118, "Rohstoff Em" );
		addColor(colorValuesVec, 0x265E39, "Rohstoff Sf" );
		addColor(colorValuesVec, 0x1F8A42, "Rohstoff Cl" );
		addColor(colorValuesVec, 0x1F8B40, "Rohstoff NaCl" );
		addColor(colorValuesVec, 0x4F7576, "Rohstoff Pt" );
		addColor(colorValuesVec, 0x365A7E, "Rohstoff H" );
		addColor(colorValuesVec, 0x005C83, "Rohstoff Co" );
		addColor(colorValuesVec, 0x013A7F, "Rohstoff In" );
		addColor(colorValuesVec, 0xDEDCD1, "Rohstoff H3" );
		addColor(colorValuesVec, 0x8B7E75, "Rohstoff Fe" );
		addColor(colorValuesVec, 0xDE921F, "Rohstoff N" );
		addColor(colorValuesVec, 0x1C364D, "Nanit-Haufen" );
		addColor(colorValuesVec, 0x10805C, "Völker-Geschenk (alt)" );
		addColor(colorValuesVec, 0x007951, "Völker-Geschenk" );
		addColor(colorValuesVec, 0x4D2957, "Völker-Geschenk (synth)" );
		addColor(colorValuesVec, 0xF0A92B, "Produkt & Upgrade S" );
		addColor(colorValuesVec, 0xC11746, "Energie" );
		addColor(colorValuesVec, 0x2C7C9F, "Tragbares Objekt" );
		addColor(colorValuesVec, 0x085C78, "Tech 1" );
		addColor(colorValuesVec, 0x0063B6, "Tech 2 & Upgrade B" );
		addColor(colorValuesVec, 0x095C77, "Tech 1 (Synth)" );
		addColor(colorValuesVec, 0x2177C8, "Tech 2 (Synth)" );
		addColor(colorValuesVec, 0x0063B7, "Tech 3 (Synth, old: Tech2)" );
		addColor(colorValuesVec, 0x7C4562, "Upgrade A" );
		addColor(colorValuesVec, 0x5B9352, "Upgrade C" );
		addColor(colorValuesVec, 0x682603, "Schlüssel" );
		addColor(colorValuesVec, 0x262626, "Feuerwerk" );
		addColor(colorValuesVec, 0x507951, "Gas-Produkt 1" );
		addColor(colorValuesVec, 0x207951, "Gas-Produkt 2" );
		addColor(colorValuesVec, 0x153250, "Fundstück (gewöhnlich)" );
		addColor(colorValuesVec, 0x460C34, "Fundstück (ungewöhnlich)" );
		addColor(colorValuesVec, 0xDC9401, "Fundstück (selten)" );
		addColor(colorValuesVec, 0x7B0000, "Schaden" );
		addColor(colorValuesVec, 0xFFFFFF, "Weiß" );
		addColor(colorValuesVec, 0xCCCCCC, "CCCCCC" );
		addColor(colorValuesVec, 0x447519, "Larvenkern" );
		addColor(colorValuesVec, 0x1A2733, "Special Token" );
		
		loadColorsFromFile(colorValuesVec);
		
		colorValues = colorValuesVec.toArray(new NamedColor[0]);
	}

	private void loadColorsFromFile(Vector<NamedColor> colorValuesVec) {
		File file = new File(FileExport.FILE_COLORS);
		if (!file.isFile()) return;
		
		boolean noNewColorAdded = true;
		long start = System.currentTimeMillis();
		Gui.log_ln("Read newly defined background colors from file \"%s\"...", file.getPath());
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
						SaveViewer.log_warn_ln("   changed name of %s into \"%s\"", existingColor, newColor.name);
						existingColor.name = newColor.name;
					}
				} else {
					SaveViewer.log_warn_ln("   added %s", newColor);
					noNewColorAdded = false;
					colorValuesVec.add(newColor);
				}
			}
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
		
		if (noNewColorAdded) SaveViewer.log_warn_ln("   All colors from file are already known. File \"%s\" can be deleted.",file.getPath());
		SaveViewer.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
	}

	private void saveColorsToFile() {
		long start = System.currentTimeMillis();
		File file = new File(FileExport.FILE_COLORS);
		SaveViewer.log_ln("Write background colors to file \""+file.getPath()+"\"...");
		
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8));) {
			for (NamedColor color:colorValues)
				if (color!=null)
					out.printf("%06X=%s\r\n",color.value,color.name);
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		
		SaveViewer.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
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
			buttonPanel.add(Gui.createButton("Ok", e->{if (value!=null) result = new NamedColor(value, nameField.getText()); closeDialog(); }));
			buttonPanel.add(Gui.createButton("Cancel", e->{ closeDialog(); }));
			
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

			@Override protected void paintCanvas(Graphics g, int x, int y, int width, int height) {
				g.setColor(Color.GRAY);
				g.drawRect(x, y, width-1, height-1);
				if (color==null) {
					g.drawLine(x, y, x+width-1, y+height-1);
					g.drawLine(x+width-1, y, x, y+height-1);
				} else {
					g.setColor(color);
					g.fillRect(x+1, y+1, width-2, height-2);
				}
			}
		}
		
	}
	
	private int findImage(String imagesName) {
		for (int i=0; i<imagesNames.length; i++)
			if (imagesNames[i].equalsIgnoreCase(imagesName))
				return i;
		return -1;
	}

	public boolean existImage(String name) {
		return findImage(name) != -1;
	}
	
	public boolean renameImage(String oldName, String newName, ProgressDialog pd, Runnable taskBeforeUpdatingImageListListeners) {
		int index = findImage(oldName);
		if (index == -1) { SaveViewer.log_error_ln("Can't rename image: Image \"%s\" was not found in image list.", oldName); return false; }
		
		int other = findImage(newName);
		if (other!=-1 && other!=index) { SaveViewer.log_error_ln("Can't rename image: Another image with the new name \"%s\" was found in image list.", newName); return false; }
		
		File folder = new File(FileExport.EXTRA_IMAGES_PATH);
		if (!folder.isDirectory()) { SaveViewer.log_error_ln("Can't rename image: Image folder \"%s\" does not exist.", folder); return false; }
		
		File source = new File(folder,oldName);
		File target = new File(folder,newName);
		if (!source.isFile()) { SaveViewer.log_error_ln("Can't rename image: Source image file \"%s\" was not found." , source.getPath()); return false; }
		if ( target.exists()) { SaveViewer.log_error_ln("Can't rename image: Target image file \"%s\" already exists.", target.getPath()); return false; }
		
		if (oldName.equalsIgnoreCase(newName)) {
			File temp = new File(folder,oldName+".temp");
			int i = 0;
			while(temp.exists()) { i++; temp = new File(folder,oldName+".temp"+i); }
			
			try {
				Files.move(source.toPath(), temp.toPath(), StandardCopyOption.ATOMIC_MOVE);
				source = temp;
			} catch (IOException ex) {
				SaveViewer.log_error_ln("Can't rename image: IOException: %s", ex.getMessage());
				return false;
			}
		}
		
		try {
			Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException ex) {
			SaveViewer.log_error_ln("Can't rename image: IOException: %s", ex.getMessage());
			return false;
		}
		
		imagesNames[index] = newName;
		
		BufferedImage image = images.remove(oldName);
		if (image!=null) images.put(newName,image);
		
		if (taskBeforeUpdatingImageListListeners!=null)
			taskBeforeUpdatingImageListListeners.run();
		
		updateImageListListeners(pd);
		return true;
	}

	public boolean deleteImage(String imageFileName, Consumer<Integer> taskBeforeUpdatingImageListListeners) {
		int index = findImage(imageFileName);
		if (index == -1) { SaveViewer.log_error_ln("Can't delete image: Image \"%s\" was not found in image list.", imageFileName); return false; }
		
		File folder = new File(FileExport.EXTRA_IMAGES_PATH);
		if (!folder.isDirectory()) { SaveViewer.log_error_ln("Can't delete image: Image folder \"%s\" does not exist.", folder); return false; }
		
		File source = new File(folder,imageFileName);
		if (!source.isFile()) { SaveViewer.log_error_ln("Can't delete image: Image file \"%s\" was not found.", source.getPath()); return false; }
		
		try {
			Files.delete(source.toPath());
		} catch (IOException ex) {
			SaveViewer.log_error_ln("Can't delete image: IOException: %s", ex.getMessage());
			return false;
		}
		
		Vector<String> vector = new Vector<String>(Arrays.asList(imagesNames));
		vector.remove(index);
		imagesNames = vector.toArray(new String[0]);
		
		images.remove(imageFileName);
		
		if (taskBeforeUpdatingImageListListeners!=null)
			taskBeforeUpdatingImageListListeners.accept(index);
		
		updateImageListListeners(null);
		return true;
	}

	public static interface ImageListListener {
		public void imageListChanged();
	}
	
	public void addImageListListener(ImageListListener ill) {
		imageListListeners.add(ill);
	}
	
	public void removeImageListListener(ImageListListener ill) {
		imageListListeners.remove(ill);
	}

	public void reloadImageList(ProgressDialog pd) {
		readImages(pd);
		updateImageListListeners(pd);
	}

	private void updateImageListListeners(ProgressDialog pd) {
		if (pd!=null) {
			SaveViewer.runInEventThreadAndWait(()->{
				pd.setTaskTitle("Update GUI");
				pd.setValue(0, imageListListeners.size());
			});
		}
		for (int i=0; i<imageListListeners.size(); i++) {
			imageListListeners.get(i).imageListChanged();
			if (pd!=null) {
				int value = i+1;
				SaveViewer.runInEventThreadAndWait(()->pd.setValue(value));
			}
		}
	}

	private void readImages(ProgressDialog pd) {
		if (pd!=null) {
			SaveViewer.runInEventThreadAndWait(()->{
				pd.setTaskTitle("Get Image List");
				pd.setIndeterminate(true);
			});
		}
		long start = System.currentTimeMillis();
		File folder = new File(FileExport.EXTRA_IMAGES_PATH);
		SaveViewer.log_ln("Read image resources from \""+folder.getPath()+"\" ...");
		if (!folder.isDirectory()) {
			SaveViewer.log_error_ln("   ... abort reading. Can't open folder.");
			return;
		}
		
		imagesNames = folder.list(new FilenameFilter() {
			@Override public boolean accept(File dir, String name) {
				if (!new File(dir,name).isFile()) return false;
				name = name.toLowerCase();
				if (name.endsWith(".png" )) return true;
				if (name.endsWith(".jpg" )) return true;
				if (name.endsWith(".jpeg")) return true;
				return false;
			}
		});
		if (pd!=null)
			SaveViewer.runInEventThreadAndWait(()->pd.setTaskTitle("Sort Image List"));
		Arrays.sort(imagesNames,Comparator.comparing(String::toLowerCase));
		
		if (pd!=null)
			SaveViewer.runInEventThreadAndWait(()->{
				pd.setTaskTitle("Read Images");
				pd.setValue(0, imagesNames.length);
			});
		
		int listChunkIndex = 0;
		images.clear();
		for (int i=0; i<imagesNames.length; ++i) {
			File file = new File(folder,imagesNames[i]);
			//InputStream stream = images.getClass().getResourceAsStream("/icons/"+imagesNames[i]);
			
			BufferedImage image = null;
			try { image = ImageIO.read(file); }
			catch (IOException e) {}
			
			if (image!=null)
				images.put(imagesNames[i], image);
			
			int value = i+1;
			if (pd!=null) SaveViewer.runInEventThreadAndWait(()->pd.setValue(value));
			else {
				int n= i*6/imagesNames.length;
				if (listChunkIndex != n) {
					listChunkIndex = n;
					SaveViewer.log(" .. %d",images.size());
				}
			}
		}
		if (pd==null)
			SaveViewer.log_ln(" .. %d",images.size());
		
		SaveViewer.log_ln("   done (in "+((System.currentTimeMillis()-start)/1000.0f)+"s)");
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
		
		if (imageBackground==null) {
			if (baseImage==null) return baseImage;
			if (width==baseImage.getWidth() && height==baseImage.getHeight()) return baseImage;
		}
		
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
	}
	
	public enum UpgradeCategory {
		Generic, Mine, Damage, Speed, Time, Clip, Accuracy, Armour, Protection, Scan, Energy, Jetpack;

		public String getLabel() {
			return toString();
		}

		public static UpgradeCategory getValue(String str) {
			try { return valueOf(str); }
			catch (Exception e) { return null; }
		}
	}
	
	public static class UpgradeCategoryImages {
		private static final String IMAGES_UPGRADECAT_PNG = "/images/Upgradecat.png";
		private static final IconSource<UpgradeCategory> UpgradeCategoryImageIS = new IconSource<UpgradeCategory>(64,64);
		private static final HashMap<ImageKey,Image> images = new HashMap<>();
		private static final HashMap<ImageKey,Icon> icons = new HashMap<>();

		public static void init() {
			UpgradeCategoryImageIS.readIconsFromResource(IMAGES_UPGRADECAT_PNG);
		}
		
		public static Icon getCachedIcon(UpgradeCategory cat, int width, int height) {
			Icon icon = icons.get(new ImageKey(cat,width,height));
			if (icon==null) icons.put(new ImageKey(cat,width,height),icon = new ImageIcon(createImage(cat,width,height)));
			return icon;
		}
		
		public static Image getCachedImage(UpgradeCategory cat, int width, int height) {
			Image image = images.get(new ImageKey(cat,width,height));
			if (image==null) images.put(new ImageKey(cat,width,height),image = createImage(cat,width,height));
			return image;
		}
		
		public static Image createImage(UpgradeCategory cat, int width, int height) {
			BufferedImage image = UpgradeCategoryImageIS.getImage(cat);
			return image.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
		}

		public static Image createImage(UpgradeCategory cat, int width, int height, Color color) {
			BufferedImage image = UpgradeCategoryImageIS.getImage(cat);
			if (color==null)
				return image.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
			
			BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics g = result.getGraphics();
			g.setColor(color);
			g.fillRect(0, 0, width, height);
			g.drawImage(image.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
			return result;
		}

		private static class ImageKey {
			private final UpgradeCategory cat;
			private final int width;
			private final int height;

			public ImageKey(UpgradeCategory cat, int width, int height) {
				this.cat = cat;
				this.width = width;
				this.height = height;
			}

			@Override public int hashCode() { return ((cat.ordinal()&0xF)<<16) | ((width&0xFF)<<8) | (height&0xFF); }

			@Override
			public boolean equals(Object obj) {
				if (!(obj instanceof ImageKey)) return false;
				ImageKey other = (ImageKey)obj;
				if (this.cat   !=other.cat   ) return false;
				if (this.width !=other.width ) return false;
				if (this.height!=other.height) return false;
				return true;
			}

			@Override
			public String toString() {
				return "ImageKey [cat=" + cat + ", width=" + width + ", height=" + height + "]";
			}
		}
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
	
	public static class ImageGridPanel extends net.schwarzbaer.gui.ImageGridPanel implements Iterable<net.schwarzbaer.gui.ImageGridPanel.ImageData> {
		private static final long serialVersionUID = -5485402060151977360L;
		private boolean markUsedImages;
		private int[] sortedIndexes;

		public ImageGridPanel(int cols, String preselectedImageFileName) {
			super(cols, preselectedImageFileName, false, null);
			markUsedImages = false;
			sortedIndexes = null;
			
			setMarkerColors(new Color[] { Color.LIGHT_GRAY, new Color(0xDCB9F2) });
			createImageItems(preselectedImageFileName,this,null);
		}
		
		public int[] removeValue(int index, int[] arr) {
			if (index<0 || index>=arr.length)
				return arr;
			
			if (index==0)
				return Arrays.copyOfRange(arr, 1, arr.length);
			
			if (index==arr.length-1)
				return Arrays.copyOfRange(arr, 0, arr.length-1);
			
			
			return IntStream.concat(
					Arrays.stream(arr,0,index),
					Arrays.stream(arr,index+1,arr.length)
			).toArray();
		}
		
		public void deleteSortedIndex(int index, int nameIndex) {
			sortedIndexes = Arrays
					.stream( removeValue(index, sortedIndexes) )
					.map(i-> i < nameIndex ? i : i-1)
					.toArray();
		}

		public boolean hasStandardOrder() {
			return sortedIndexes==null;
		}

		public void setOrder(int[] sortedIndexes) {
			this.sortedIndexes = sortedIndexes;
		}
		
		public int getSelectedGridIndex() {
			return selectedIndex;
		}
		public void setSelectedGridIndex(int selectedIndex) {
			this.selectedIndex = selectedIndex;
		}

		private int getIndex(int i) {
			if (sortedIndexes!=null) return sortedIndexes[i]; 
			return i;
		}
		private int getLength() {
			if (sortedIndexes!=null) return sortedIndexes.length; 
			return SaveViewer.images.imagesNames.length;
		}
		
		@Override
		public Iterator<ImageData> iterator() {
			return new Iterator<ImageData>() {
				private int index = 0;
				@Override public boolean hasNext() {
					return index < getLength();
				}
				@Override public ImageData next() {
					String imageName = SaveViewer.images.imagesNames[getIndex(index++)];
					BufferedImage image = SaveViewer.images.getImage(imageName,null,64,64);
					return new ImageData(imageName, imageName, image);
				}
			};
		}

		public void resetImages(ProgressDialog pd) {
			if (pd!=null) {
				SaveViewer.runInEventThreadAndWait(()->{
					pd.setTaskTitle("Remove images from grid");
					pd.setIndeterminate(true);
				});
			}
			String selectedImageID = getSelectedImageID();
			removeAll();
			
			if (pd!=null) {
				SaveViewer.runInEventThreadAndWait(()->{
					pd.setTaskTitle("Create new image grid");
					pd.setValue(0, SaveViewer.images.imagesNames.length);
				});
			}
			createImageItems(selectedImageID, this, pd==null ? null : i->SaveViewer.runInEventThreadAndWait(()->pd.setValue(i)));
			if (markUsedImages)
				markUsedImages(markUsedImages);
			
			revalidate();
		}

		public void markUsedImages(boolean markUsedImages) {
			this.markUsedImages = markUsedImages;
			HashSet<String> usedImages = new HashSet<String>();
			HashSet<String> usedImagesObsolete = new HashSet<String>();
			if (markUsedImages) {
				addImageNames(usedImages, usedImagesObsolete, GameInfos.techIDs   );
				addImageNames(usedImages, usedImagesObsolete, GameInfos.productIDs);
				addImageNames(usedImages, usedImagesObsolete, GameInfos.substanceIDs);
			}
			for (ImageGridPanel.ImageItem il:imageItems)
				il.setMarkerIndex( !markUsedImages?0:usedImages.contains(il.ID)?1:usedImagesObsolete.contains(il.ID)?2:0 );
		}

		private void addImageNames(HashSet<String> usedImages, HashSet<String> usedImagesObsolete, IDMap idMap) {
			for (GeneralizedID id:idMap.getValues())
				if (id.hasImageFileName()) {
					if (id.isObsolete) usedImagesObsolete.add(id.getImageFileName());
					else               usedImages        .add(id.getImageFileName());
				}
		}
	}
	
	private static class ComparableImage {
		final WritableRaster raster;
		final int index;
		double similarity;

		ComparableImage(String imageName, int index) {
			BufferedImage image = SaveViewer.images.getImage(imageName,0xFFFFFF,256,256);
			this.raster = image.getRaster();
			this.index = index;
			this.similarity = Double.NaN;
		}

		public void computeSimilarityTo(ComparableImage other) {
			Rectangle bounds      = this .raster.getBounds();
			Rectangle otherBounds = other.raster.getBounds();
			if (!bounds.equals(otherBounds))
				throw new IllegalArgumentException();
			
			double[] p1 = new double[4];
			double[] p2 = new double[4];
			similarity = 0;
			for (int x=bounds.x; x<bounds.width+bounds.x; x++)
				for (int y=bounds.y; y<bounds.height+bounds.y; y++) {
					this .raster.getPixel(x, y, p1);
					other.raster.getPixel(x, y, p2);
					similarity += (float) Math.sqrt( (p1[0]-p2[0])*(p1[0]-p2[0]) + (p1[1]-p2[1])*(p1[1]-p2[1]) + (p1[2]-p2[2])*(p1[2]-p2[2]) );
				}
		}

		@SuppressWarnings("unused")
		public void testRaster() {
			float[] pixel = new float[4];
			Rectangle bounds = raster.getBounds();
			for (int x=bounds.x; x<bounds.width+bounds.x; x++)
				for (int y=bounds.y; y<bounds.height+bounds.y; y++) {
					raster.getPixel(x, y, pixel);
					SaveViewer.log_ln("raster(%d,%d) := [ %1.5f, %1.5f, %1.5f, %1.5f ]", x,y, pixel[0], pixel[1], pixel[2], pixel[3]);
				}
		}
	}

	public static class ShowImagesDialog extends StandardDialog {
		private static final long serialVersionUID = 2440132074027157283L;
		
		private HashMap<String, IdUsage> usage;
		private JScrollPane imageScrollPane;
		private ImageGridPanel imageGridPanel;
		private JTextArea output;
		private JLabel imageField;
		private JPopupMenu contextMenu;
		private JRadioButton rdbtnSortSimilarity;
		
		private String selectedName;
		private String clickedName;
		private int clickedGridIndex;

		
		public ShowImagesDialog(Window parent, String title) {
			super(parent,title,ModalityType.APPLICATION_MODAL);
			boolean markUsedImagesByDefault = true;
			
			imageGridPanel = new ImageGridPanel(8,null);
			imageGridPanel.markUsedImages(markUsedImagesByDefault);
			imageGridPanel.addSelectionListener(this::setSelected);
			imageGridPanel.addRightClickListener((name, index, source, x, y) -> {
				clickedName  = name;
				clickedGridIndex = index;
				contextMenu.show(source, x, y);
			});
			clickedName = null;
			clickedGridIndex = -1;
			selectedName = null;
			
			imageScrollPane = new JScrollPane(imageGridPanel);
			imageScrollPane.setPreferredSize(new Dimension(930,700));
			imageScrollPane.getVerticalScrollBar().setUnitIncrement(10);
			
			imageField = new JLabel();
			imageField.setBorder(BorderFactory.createEtchedBorder());
			imageField.setPreferredSize(new Dimension(256,256));
			
			output = new JTextArea();
			output.setEditable(false);
			JScrollPane outputScrollPane = new JScrollPane(output);
			outputScrollPane.setPreferredSize(new Dimension(400,450));
			
			ButtonGroup bg = new ButtonGroup();
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			JPanel buttonPanel = new JPanel(new GridBagLayout());
			c.weightx = 0;
			buttonPanel.add(Gui.createRadioButton("Sort by name", bg, true, true,e->sortByName()),c);
			buttonPanel.add(rdbtnSortSimilarity = Gui.createRadioButton("Sort by similarity to selected image", bg, false, false,e->sortBySimilarityTo(selectedName)),c);
			buttonPanel.add(Gui.createCheckbox("Mark Used Images", markUsedImagesByDefault, isSelected->imageGridPanel.markUsedImages(isSelected)),c);
			c.weightx = 1;
			buttonPanel.add(new JLabel(),c);
			c.weightx = 0;
			buttonPanel.add(Gui.createButton("Close",e->closeDialog()),c);
			
						
			JPanel rightPanel = new JPanel(new BorderLayout(3,3));
			rightPanel.add(imageField, BorderLayout.NORTH);
			rightPanel.add(outputScrollPane,BorderLayout.CENTER);
			
			JPanel contentPane = new JPanel(new BorderLayout(3,3));
			contentPane.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
			contentPane.add(imageScrollPane,BorderLayout.WEST);
			contentPane.add(rightPanel,BorderLayout.CENTER);
			contentPane.add(buttonPanel,BorderLayout.SOUTH);
			
			contextMenu = new JPopupMenu("ImageContextMenu");
			contextMenu.add("Sort by similarity to this image").addActionListener(e->sortBySimilarityTo(clickedName));
			contextMenu.addSeparator();
			contextMenu.add("Rename").addActionListener(e->renameSelectedImage());
			contextMenu.add("Delete").addActionListener(e->deleteSelectedImage());
			
			getUsage();
			this.createGUI(contentPane);
		}
		
		private void sortByName() {
			SaveViewer.runWithProgressDialog(this, "Sort By Name", pd -> {
				imageGridPanel.setOrder(null);
				imageGridPanel.resetImages(pd);
			});
		}
		
		private void sortBySimilarityTo(String imageName) {
			if (imageName==null) return;
			SaveViewer.runWithProgressDialog(this, "Sort By Similarity", pd -> {
				SaveViewer.runInEventThreadAndWait(()->{
					if (!rdbtnSortSimilarity.isSelected())
						rdbtnSortSimilarity.setSelected(true);
					
					pd.setTaskTitle("Compare images");
					pd.setValue(0, SaveViewer.images.imagesNames.length);
				});
				
				ComparableImage baseImage = new ComparableImage(imageName,-1);
				//selectedImage.testRaster();
				
				ComparableImage[] images = new ComparableImage[SaveViewer.images.imagesNames.length];
				for (int i=0; i<SaveViewer.images.imagesNames.length; i++) {
					ComparableImage image1 = new ComparableImage(SaveViewer.images.imagesNames[i],i);
					image1.computeSimilarityTo(baseImage);
					images[i] = image1;
					int value = i+1;
					SaveViewer.runInEventThreadAndWait(()->pd.setValue(value));
				}
				
				SaveViewer.runInEventThreadAndWait(()->{
					pd.setTaskTitle("Sort images");
					pd.setIndeterminate(true);
				});
				int[] sortedIndexes = Arrays.stream(images)
						.sorted(Comparator.comparing(image2->image2.similarity))
						.mapToInt(image3->image3.index)
						.toArray();
				
				imageGridPanel.setOrder(sortedIndexes);
				imageGridPanel.resetImages(pd);
			});
		}

		private static class IdUsage {
			Vector<GeneralizedID> techIDs     ;
			Vector<GeneralizedID> productIDs  ;
			Vector<GeneralizedID> substanceIDs;
			public IdUsage() {
				this.techIDs = new Vector<>();
				this.productIDs = new Vector<>();
				this.substanceIDs = new Vector<>();
			}
			public int getIdCount() {
				return techIDs.size()+productIDs.size()+substanceIDs.size();
			}
			public void setImageFileName(String finalNewName) {
				for (GeneralizedID id:techIDs     ) id.setImageFileName(finalNewName);
				for (GeneralizedID id:productIDs  ) id.setImageFileName(finalNewName);
				for (GeneralizedID id:substanceIDs) id.setImageFileName(finalNewName);
			}
		}
		
		private void getUsage() {
			usage = new HashMap<>();
			addUsage(GameInfos.techIDs     , iu->iu.techIDs     );
			addUsage(GameInfos.productIDs  , iu->iu.productIDs  );
			addUsage(GameInfos.substanceIDs, iu->iu.substanceIDs);
		}

		private void addUsage(IDMap idMap, Function<IdUsage,Vector<GeneralizedID>> getIdList) {
			for (GeneralizedID id:idMap.getValues())
				if (id.hasImageFileName()) {
					String imageFileName = id.getImageFileName();
					IdUsage idUsage = usage.get(imageFileName);
					if (idUsage==null) usage.put(imageFileName, idUsage = new IdUsage());
					Vector<GeneralizedID> idList = getIdList.apply(idUsage);
					idList.add(id);
				}
		}

		private void deleteSelectedImage() {
			if (clickedName==null && clickedGridIndex<0) return;
			
			if (JOptionPane.YES_OPTION !=
					JOptionPane.showConfirmDialog(
							this,
							"Do you really want to delete image \""+clickedName+"\"?",
							"Are you sure?",
							JOptionPane.YES_NO_CANCEL_OPTION,
							JOptionPane.QUESTION_MESSAGE
					)
				)
				return;
			
			IdUsage idUsage = usage.get(clickedName);
			if (idUsage!=null) {
				int idCount = idUsage.getIdCount();
				if (idCount>0)
					if (JOptionPane.YES_OPTION !=
							JOptionPane.showConfirmDialog(
									this,
									"The image you want to delete is used by "+idCount+" ID"+(idCount>1?"s":"")+". Do you really want to delete this?",
									"Image is in use",
									JOptionPane.YES_NO_CANCEL_OPTION,
									JOptionPane.WARNING_MESSAGE
							)
						)
						return;
			}
			
			boolean wasSuccessful = SaveViewer.images.deleteImage(clickedName, index->{
				IdUsage removedIdUsage = usage.remove(clickedName);
				if (removedIdUsage!=null)
					removedIdUsage.setImageFileName(null);
				if (!imageGridPanel.hasStandardOrder())
					imageGridPanel.deleteSortedIndex(clickedGridIndex,index);
			});
			if (wasSuccessful) {
				if (clickedGridIndex==imageGridPanel.getSelectedGridIndex()) {
					setSelected(null);
					imageGridPanel.setSelectedGridIndex(-1);
				}
				
				SaveViewer.runWithProgressDialog(this, "Reset Images in Window", imageGridPanel::resetImages);
				GameInfos.saveAllIDsToFiles();
			} else
				JOptionPane.showMessageDialog(this, "Can't delete \""+clickedName+"\".", "Error", JOptionPane.ERROR_MESSAGE);
			
			clickedName = null;
			clickedGridIndex = -1;
		}

		private void renameSelectedImage() {
			String oldName = clickedName;
			String newName = JOptionPane.showInputDialog(this, "Enter new name:", oldName);
			while (newName!=null && SaveViewer.images.existImage(newName))
				newName = JOptionPane.showInputDialog(this, "Sorry, image name already exists. Please enter another name:", newName);
			if (newName==null) return;
			
			String finalNewName = newName;
			boolean wasSuccessful = SaveViewer.images.renameImage(oldName, newName, null, ()->{
				IdUsage idUsage = usage.remove(oldName);
				if (idUsage!=null) {
					usage.put(finalNewName, idUsage);
					idUsage.setImageFileName(finalNewName);
				}
			});
			if (wasSuccessful) {
				imageGridPanel.setImageName(clickedGridIndex,newName);
				if (clickedGridIndex==imageGridPanel.getSelectedGridIndex())
					setSelected(newName);
				GameInfos.saveAllIDsToFiles();
			} else
				JOptionPane.showMessageDialog(this, "Can't rename \""+oldName+"\" to \""+newName+"\".", "Error", JOptionPane.ERROR_MESSAGE);
			
			clickedName = null;
			clickedGridIndex = -1;
		}

		private void setSelected(String selectedName) {
			this.selectedName = selectedName;
			rdbtnSortSimilarity.setEnabled(selectedName!=null);
			showValuesOfSelected();
		}
		private void showValuesOfSelected() {
			//SaveViewer.log_ln("Selected Image: %s", selectedName);
			
			output.setText("");
			if (selectedName==null) return;
			
			BufferedImage image = SaveViewer.images.getImage(selectedName,null,-1,-1);
			imageField.setIcon(image!=null?new ImageIcon(image):null);
			
			output.append("Image:\r\n");
			output.append("   "+selectedName+"\r\n");
			if (image!=null) output.append("   "+image.getWidth()+" x "+image.getHeight()+"\r\n");
			output.append("\r\n");
			
			IdUsage idUsage = usage.get(selectedName);
			if (idUsage==null) idUsage = new IdUsage();
			
			output.append("Used by following IDs:\r\n");
			output.append("   technologies:\r\n");
			showIdList(idUsage.techIDs);
			output.append("   products:\r\n");
			showIdList(idUsage.productIDs);
			output.append("   substances:\r\n");
			showIdList(idUsage.substanceIDs);
			output.append("\r\n");
		}

		private void showIdList(Vector<GeneralizedID> idList) {
			if (idList.isEmpty())
				output.append("      none\r\n");
			else
				for (GeneralizedID id:idList)
					output.append("      "+id.getName()+(id.isObsolete?" OBSOLETE":"")+"\r\n");
		}
	}

	public static class SelectImageDialog extends StandardDialog {
		private static final long serialVersionUID = -3724853350437145460L;
		
		private String selected;
		private JScrollPane imageScrollPane;
		private ImageGridPanel imageGridPanel;
	
		public SelectImageDialog(Window parent, String title, String initialValue) {
			super(parent,title,ModalityType.APPLICATION_MODAL);
			selected = null;
			boolean markUsedImagesByDefault = true;
			
			imageGridPanel = new ImageGridPanel(8,initialValue);
			imageGridPanel.addSelectionListener(this::setResult);
			imageGridPanel.markUsedImages(markUsedImagesByDefault);
			imageScrollPane = new JScrollPane(imageGridPanel);
			imageScrollPane.setPreferredSize(new Dimension(930,600));
			imageScrollPane.getVerticalScrollBar().setUnitIncrement(10);
			
			JCheckBox chkbxMarkUsedImages;
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			buttonPanel.add(chkbxMarkUsedImages = Gui.createCheckbox("Mark Used Images", null, markUsedImagesByDefault));
			buttonPanel.add(Gui.createButton("Choose \"No Image\"",e->setResult("")));
			buttonPanel.add(Gui.createButton("Cancel",e->closeDialog()));
			chkbxMarkUsedImages.addActionListener(e->imageGridPanel.markUsedImages(chkbxMarkUsedImages.isSelected()));
			
			JPanel contentPane = new JPanel(new BorderLayout(3,3));
			contentPane.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
			contentPane.add(imageScrollPane,BorderLayout.CENTER);
			contentPane.add(buttonPanel,BorderLayout.SOUTH);
			
			this.createGUI(contentPane);
		}
	
		@Override
		public void windowOpened(WindowEvent e) {
			imageGridPanel.scrollToPreselectedImage(imageScrollPane);
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
	}
}
