package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.schwarzbaer.gui.Canvas;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Images;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Inventory;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Inventory.BaseStatValue;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Inventory.Slot;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Inventory.Slot.Type;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveViewer;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.SaveGameView.SaveGameViewTabPanel;

final class InventoriesPanel extends SaveGameViewTabPanel {
	private static final long serialVersionUID = -6965281963499623839L;
	private JTabbedPane tabbedPane;
	private Window mainwindow;

	public InventoriesPanel(SaveGameData data, Window mainwindow) {
		super(data);
		
		this.mainwindow = mainwindow;
		
		tabbedPane = new JTabbedPane();
		addTab(data.inventories.player       );
		addTab(data.inventories.playerTech   );
		addTab(data.inventories.playerCargo  );
		addTab(data.inventories.grave        );
		addTab(data.inventories.multitool    );
		addTab(data.inventories.freighter    );
		addTab(data.inventories.freighterTech);
		addTab(data.inventories.ship_old     );
		tabbedPane.addTab("Ships"           , new InventoryListPanel(mainwindow).addInv(data.inventories.ships,data.inventories.ships_Tech));
		tabbedPane.addTab("Vehicles"        , new InventoryListPanel(mainwindow).addInv(data.inventories.vehicles));
		tabbedPane.addTab("Containers"      , new InventoryListPanel(mainwindow,0,2).addInv(data.inventories.chests));
		tabbedPane.addTab("Magic Chests"    , new InventoryListPanel(mainwindow).addInv(data.inventories.magicChest).addInv(data.inventories.magicChest2));

		tabbedPane.addChangeListener(new ChangeListener() {
			@Override public void stateChanged(ChangeEvent e) {
				updateSelectedTab();
			}
		});

		add(tabbedPane,BorderLayout.CENTER);
	}


	private void addTab(Inventory inventory) {
		if (inventory!=null)
			tabbedPane.addTab(inventory.label, new InventoryPanel(mainwindow, inventory));
	}
	
	
	private void updateSelectedTab() {
		Component comp = tabbedPane.getSelectedComponent();
		if (comp instanceof Updatable)
			((Updatable)comp).updateContent();
	}


	@Override
	public void updateContent() {
		updateSelectedTab();
	}
	
	private static interface Updatable {
		public void updateContent();
	}

	final static class InventoryPanel extends JPanel implements Updatable {
		private static final long serialVersionUID = 8549406812793642121L;
		
		private Inventory inventory;
		private JTextArea textarea;
		private InventoryDisplay inventoryLabel;
		private JPopupMenu contextMenu;

		private JMenuItem miSetLabel;
		private JMenuItem miSetImage;
		private Slot clickedSlot;
		private Updatable updateListener;

		private Window mainwindow;


		public InventoryPanel(Window mainwindow, Inventory inventory) {
			this(mainwindow, inventory, false, true, null);
		}
		
		public InventoryPanel(Window mainwindow, Inventory inventory, boolean withTitledBorder, boolean makeInventoryScrollable, Updatable updateListener) {
			super(new BorderLayout(3, 3));
			if (withTitledBorder) setBorder(BorderFactory.createTitledBorder(inventory==null?"<null>":inventory.label));
			else                  setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			
			this.mainwindow = mainwindow;
			this.updateListener = updateListener;
			this.inventory = inventory;
			this.clickedSlot = null;
			
			textarea = new JTextArea();
			textarea.setEditable(false);
			JScrollPane textareaScrollPane = new JScrollPane(textarea);
			textareaScrollPane.getViewport().setPreferredSize(new Dimension(350, 300));
			add(textareaScrollPane, BorderLayout.EAST);
			showValues();
			
			contextMenu = new JPopupMenu();
			contextMenu.add(miSetLabel=SaveViewer.createMenuItem("Set Label", e->setLabelForResource()));
			contextMenu.add(miSetImage=SaveViewer.createMenuItem("Set Image", e->setImageForResource()));
			
			if (this.inventory!=null && this.inventory.width!=null && this.inventory.height!=null) {
				inventoryLabel = new InventoryDisplay(this,(int)(long)this.inventory.width,(int)(long)this.inventory.height,this.inventory.slots);
				add(makeInventoryScrollable?new JScrollPane(inventoryLabel):inventoryLabel, BorderLayout.CENTER);
			} else
				inventoryLabel = null;
		}

		@Override
		public void updateContent() {
			if (inventoryLabel != null) inventoryLabel.repaint();
			showValues();
		}

		private void setImageForResource() {
			if (clickedSlot == null) return;
			
			if (clickedSlot.id==null || clickedSlot.type==null) return;
			
			Images.IdImageDialog dlg = new Images.IdImageDialog(mainwindow,clickedSlot.id);
			dlg.showDialog();
			
			if (dlg.hasDataChanged()) {
				clickedSlot.id.setImageBG(dlg.getImageBG());
				clickedSlot.id.setImageFileName(dlg.getImageFileName());
				updateAfterChangedIDdata();
			}
			
			clickedSlot = null;
		}
		
		private void setLabelForResource() {
			if (clickedSlot == null) return;
			
			if (clickedSlot.id==null || clickedSlot.type==null) return;
			
			String name = JOptionPane.showInputDialog(this, String.format("New name for %s ID \"%s\"", clickedSlot.type, clickedSlot.id.id), clickedSlot.id.label);
			if (name!=null) {
				clickedSlot.id.label = name;
				updateAfterChangedIDdata();
			}
			clickedSlot = null;
		}

		private void updateAfterChangedIDdata() {
			switch(clickedSlot.type) {
			case Product   : GameInfos.saveProductIDsToFile();   break;
			case Technology: GameInfos.saveTechIDsToFile();      break;
			case Substance : GameInfos.saveSubstanceIDsToFile(); break;
			}
			if (updateListener!=null) updateListener.updateContent();
			else inventoryLabel.repaint();
		}

		private void showContextMenu(Component invoker, int screenX, int screenY) {
			if (!isValidSlotHovered()) return;
			clickedSlot = inventory.slots[inventoryLabel.hoveredSlot.x][inventoryLabel.hoveredSlot.y];
			miSetLabel.setEnabled(!clickedSlot.isEmpty && clickedSlot.id!=null && clickedSlot.type!=null);
			miSetImage.setEnabled(!clickedSlot.isEmpty && clickedSlot.id!=null && clickedSlot.type!=null);
			contextMenu.show(invoker, screenX, screenY);
		}

		private boolean isValidSlotHovered() {
			return inventory.slots != null &&
					inventoryLabel != null &&
					inventoryLabel.hoveredSlot != null &&
					inventoryLabel.hoveredSlot.x < inventory.slots.length &&
					inventoryLabel.hoveredSlot.y < inventory.slots[inventoryLabel.hoveredSlot.x].length &&
					inventory.slots[inventoryLabel.hoveredSlot.x][inventoryLabel.hoveredSlot.y] != null;
		}

		private void showValues() {
			textarea.setText("");
			if (!isValidSlotHovered()) {
				textarea.append("Width        : "+inventory.width+"\r\n");
				textarea.append("Height       : "+inventory.height+"\r\n");
				textarea.append("Class        : "+inventory.inventoryClass+"\r\n");
				textarea.append("is cool      : "+inventory.isCool+"\r\n");
				textarea.append("Version      : "+inventory.version+"\r\n");
				textarea.append("Special Slots: "+inventory.specialSlots+"\r\n");
				textarea.append("Product MaxStorage Multiplier  : "+inventory.productMaxStorageMultiplier+"\r\n");
				textarea.append("Substance MaxStorage Multiplier: "+inventory.substanceMaxStorageMultiplier+"\r\n");
				if (inventory.baseStatValues==null || inventory.baseStatValues.length==0)
					textarea.append("Base Status Values: none\r\n");
				else {
					textarea.append("Base Status Values:\r\n");
					for (BaseStatValue bsv:inventory.baseStatValues)
						textarea.append(String.format("   %s: %s\r\n",bsv.baseStatID,bsv.value));						
				}
			} else {
				Slot slot = inventory.slots[inventoryLabel.hoveredSlot.x][inventoryLabel.hoveredSlot.y];
				textarea.append("Inventory Slot "+inventoryLabel.hoveredSlot.x+","+inventoryLabel.hoveredSlot.y+"\r\n");
				if (slot==null) {
					textarea.append("   is invalid\r\n");
				} else
				if (slot.isEmpty) {
					textarea.append("   is valid, but empty\r\n");
				} else {
					if (slot.id!=null && !slot.id.label.isEmpty())
					textarea.append("   Label : "+slot.id.label+"\r\n");
					textarea.append("   ID    : "+(slot.id==null?("\""+slot.idStr+"\""):("["+slot.id.id+"]"))+"\r\n");
					textarea.append("   Type  : "+(slot.type==null?slot.typeStr:slot.type)+"\r\n");
					textarea.append("   Amount: "+slot.amount+"/"+slot.maxAmount+"\r\n");
					textarea.append("   Damage: "+slot.damageFactor+"\r\n");
				}
			}
		}

		private static class InventoryDisplay extends Canvas implements MouseListener, MouseMotionListener {
			private static final long serialVersionUID = -1799938226122102016L;
			
			private static final Color COLOR__SLOT_HOVERED       = new Color(0xFFD800);
			private static final Color COLOR__SLOT_HOVERED_FIXED = Color.GREEN;
			private static final Color COLOR__SLOT_EDGE = Color.BLACK;
			
			private static final Paint COLOR__SLOT_TEXT_LABEL = Color.RED;
			private static final Color COLOR__SLOT_TEXT = Color.BLUE;
			private static final Color COLOR__SLOT_TEXT_PRODUCT = new Color(0x4040FF);
			private static final Color COLOR__SLOT_TEXT_SUBSTANCE = new Color(0xFF6900);
			private static final Color COLOR__SLOT_TEXT_TECH = new Color(0xAD00AD);

			private static final Color COLOR__SLOT_BG = Color.WHITE;
			private static final Color COLOR__INVENTORY_BG = new Color(0xE0E0E0);
			
			private static final Stroke STROKE__SLOT_HOVERED = new BasicStroke(6.0f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL);
			private static final Stroke STROKE__STANDARD     = new BasicStroke(1.0f);
			
			private static final int SLOT_BORDER = 3;
			private static final int SLOT_WIDTH  = 90;
			private static final int SLOT_HEIGHT = SLOT_WIDTH+13;
			private static final int SLOT_RASTER_X = SLOT_WIDTH +2*SLOT_BORDER;
			private static final int SLOT_RASTER_Y = SLOT_HEIGHT+2*SLOT_BORDER;

			private int inventoryWidth;
			private int inventoryHeight;
			private int imageWidth;
			private int imageHeight;
			private Slot[][] slots;
			private Point hoveredSlot;
			private InventoryPanel panel;

			private boolean isHoveredSlotFixed;

			public InventoryDisplay(InventoryPanel panel, int inventoryWidth, int inventoryHeight, Slot[][] slots) {
				this.panel = panel;
				this.inventoryWidth = inventoryWidth;
				this.inventoryHeight = inventoryHeight;
				this.slots = slots;
				this.hoveredSlot = null;
				this.isHoveredSlotFixed = false;
				this.imageWidth  = inventoryWidth *SLOT_RASTER_X;
				this.imageHeight = inventoryHeight*SLOT_RASTER_Y;
				setPreferredSize(imageWidth, imageHeight);
				addMouseListener(this);
				addMouseMotionListener(this);
			}

			@Override
			protected void paintCanvas(Graphics g, int width, int height) {
				if (!(g instanceof Graphics2D)) return;
				Graphics2D g2 = (Graphics2D)g;
				Font standardFont = g2.getFont().deriveFont(Font.PLAIN, 11);
				g2.setFont(standardFont);
				
				Rectangle baseClip = g2.getClipBounds();
				
				g2.setPaint(COLOR__INVENTORY_BG);
				g2.fillRect(0, 0, imageWidth, imageHeight);
				
				if (hoveredSlot!=null) {
					g2.setStroke(STROKE__SLOT_HOVERED);
					g2.setPaint(isHoveredSlotFixed?COLOR__SLOT_HOVERED_FIXED:COLOR__SLOT_HOVERED);
					int xS=hoveredSlot.x*SLOT_RASTER_X+SLOT_BORDER;
					int yS=hoveredSlot.y*SLOT_RASTER_Y+SLOT_BORDER;
					g2.drawRect(xS, yS, SLOT_WIDTH, SLOT_HEIGHT);
				}
				
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g2.setStroke(STROKE__STANDARD);
				for (int indexX=0; indexX<inventoryWidth; ++indexX)
					for (int indexY=0; indexY<inventoryHeight; ++indexY) {
						Slot slot = slots[indexX][indexY];
						if (slot!=null) {
							int x=indexX*SLOT_RASTER_X+SLOT_BORDER;
							int y=indexY*SLOT_RASTER_Y+SLOT_BORDER;
							if (!slot.isEmpty) {
								g2.setPaint(COLOR__SLOT_BG);
								g2.fillRect(x, y, SLOT_WIDTH, SLOT_HEIGHT);
							}
							g2.setPaint(COLOR__SLOT_EDGE);
							g2.drawRect(x, y, SLOT_WIDTH-1, SLOT_HEIGHT-1);
							
							if (!slot.isEmpty) {
								final int innerWidth  = SLOT_WIDTH-2;
								final int innerHeight = SLOT_HEIGHT-2;
								final int innerOffsetX = 1;
								final int innerOffsetY = 1;
								g2.setClip(baseClip.createIntersection(new Rectangle(x+innerOffsetX, y+innerOffsetY, innerWidth, innerHeight)));
								
								int imageBorder = 3;
								int imageSize = innerWidth-2*imageBorder;
								BufferedImage image = slot.id==null?null:slot.id.getImage(imageSize,imageSize);
								
								int strOffsetX = innerOffsetX+4;
								int strOffsetY = innerOffsetY+12;
								int incrementY = 13;
								
								if (image!=null) {
									g2.setFont( standardFont.deriveFont(Font.BOLD) );
									
									String title = "";
//									switch (slot.type) {
//									case Product   : title += "P:"; break;
//									case Substance : title += "S:"; break;
//									case Technology: title += "T:"; break;
//									}
									if (slot.id.label.isEmpty()) title += slot.id.id;
									else title += slot.id.label;
									
									//g2.setPaint(getSlotTextColor(slot.type));
									g2.setPaint(Color.BLACK);
									g2.drawString(title, x+strOffsetX, y+strOffsetY); strOffsetY+=incrementY;
									
									g2.drawImage(image, x+innerOffsetX+imageBorder,y+innerOffsetY+innerHeight-imageBorder-imageSize, null);
									
									if (slot.amount!=null && slot.maxAmount!=null) {
										int amount    = (int)(long)slot.amount;
										int maxAmount = (int)(long)slot.maxAmount;
										
										if (amount>=0 && maxAmount>1) {
											int gaugeBorder = 3;
											int gaugeWidth  = imageSize-2*gaugeBorder;
											int gaugeHeight = 8;
											int gaugeXOffset = innerOffsetX+imageBorder+gaugeBorder;
											int gaugeYOffset = innerOffsetY+innerHeight-imageBorder-gaugeBorder-gaugeHeight;
											int full = (amount*gaugeWidth)/maxAmount;
											int empty = gaugeWidth-full;
											
											if (full>0) {
												g2.setPaint(Color.WHITE);
												g2.fillRect(x+gaugeXOffset,y+gaugeYOffset,full,gaugeHeight);
											}
											if (empty>0) {
												g2.setPaint(new Color(255,255,255,128));
												g2.fillRect(x+gaugeXOffset+full,y+gaugeYOffset,empty,gaugeHeight);
											}
											g2.setPaint(Color.WHITE);
											g2.drawString(String.format("%d/%d",amount,maxAmount), x+gaugeXOffset, y+gaugeYOffset-4);
										}
									}
									
									g2.setFont( standardFont );
								} else {
									g2.setPaint(getSlotTextColor(slot.type));
									
									g2.drawString(slot.type==null?slot.typeStr:slot.type.toString(), x+strOffsetX, y+strOffsetY); strOffsetY+=incrementY;
									
									if (slot.id!=null && !slot.id.label.isEmpty()) {
										g2.setPaint(COLOR__SLOT_TEXT_LABEL);
										g2.drawString(slot.id.label, x+strOffsetX, y+strOffsetY); strOffsetY+=incrementY;
										g2.setPaint(getSlotTextColor(slot.type));
									}
									
									g2.drawString(slot.id==null?slot.idStr:slot.id.id, x+strOffsetX, y+strOffsetY); strOffsetY+=incrementY;

									g2.drawString(String.format("%s/%s", slot.amount, slot.maxAmount), x+strOffsetX, y+strOffsetY); strOffsetY+=incrementY;
								}
								
								g2.setClip(baseClip);
							}
						}
					}
			}

			private Color getSlotTextColor(Type type) {
				switch(type) {
				case Product: return COLOR__SLOT_TEXT_PRODUCT;
				case Substance: return COLOR__SLOT_TEXT_SUBSTANCE;
				case Technology: return COLOR__SLOT_TEXT_TECH;
				}
				return COLOR__SLOT_TEXT;
			}

			private boolean hoveredSlotChanged(int screenX, int screenY) {
				if (isHoveredSlotFixed) return false;
				int x = screenX/SLOT_RASTER_X;
				int y = screenY/SLOT_RASTER_Y;
				Point newP = null;
				if (0<=x && x<inventoryWidth && 0<=y && y<inventoryHeight) newP = new Point(x, y);
				
				if ((hoveredSlot==null)!=(newP==null)) { hoveredSlot = newP; return true; }
				if ((hoveredSlot==null)&&(newP==null)) { return false; }
				if (hoveredSlot.x==newP.x && hoveredSlot.y==newP.y) { return false; }
				hoveredSlot.setLocation(newP);
				return true;
			}

			private boolean hoveredSlotFixed(int screenX, int screenY) {
				boolean wasHoveredSlotFixed = isHoveredSlotFixed;
				isHoveredSlotFixed = false;
				boolean changed = hoveredSlotChanged(screenX, screenY);
				isHoveredSlotFixed = !wasHoveredSlotFixed;
				return changed;
			}

			@Override public void mouseEntered (MouseEvent e) { if (hoveredSlotChanged(e.getX(),e.getY())) { panel.showValues(); repaint(); } }
			@Override public void mouseMoved   (MouseEvent e) { if (hoveredSlotChanged(e.getX(),e.getY())) { panel.showValues(); repaint(); } }
			@Override public void mouseExited  (MouseEvent e) { if (!isHoveredSlotFixed) { hoveredSlot = null; panel.showValues(); repaint(); } }
			@Override public void mousePressed (MouseEvent e) {}
			@Override public void mouseReleased(MouseEvent e) {}
			@Override public void mouseDragged (MouseEvent e) {}
			@Override public void mouseClicked (MouseEvent e) {
				switch(e.getButton()) {
				case MouseEvent.BUTTON1: if (hoveredSlotFixed(e.getX(),e.getY())) { panel.showValues(); } repaint(); break;
				case MouseEvent.BUTTON3: panel.showContextMenu(this,e.getX(),e.getY()); break;
				}
			}
		}
		
	}

	private static final class InventoryListPanel extends JScrollPane implements Updatable {
		private static final long serialVersionUID = -662233636434233389L;

		private Window mainwindow;
		private JPanel contentPanel;

		public InventoryListPanel(Window mainwindow, int rows, int cols) {
			super();
			this.mainwindow = mainwindow;
			contentPanel = new JPanel(new GridLayout(rows, cols));
			setViewportView(contentPanel);
		}
		
		public InventoryListPanel(Window mainwindow) {
			super();
			this.mainwindow = mainwindow;
			contentPanel = new JPanel();
			contentPanel.setLayout(new BoxLayout(contentPanel,BoxLayout.Y_AXIS));
			setViewportView(contentPanel);
		}

		@Override
		public void updateContent() {
			for (Component comp:contentPanel.getComponents())
				if (comp instanceof Updatable)
					((Updatable)comp).updateContent();
		}
		
		protected InventoryListPanel addInv(Inventory inventory) {
			contentPanel.add(new InventoryPanel(mainwindow,inventory,true,false,this));
			return this;
		}

		public InventoryListPanel addInv(Inventory[] inventories) {
			for (int i=0; i<inventories.length; ++i)
				contentPanel.add(new InventoryPanel(mainwindow,inventories[i],true,false,this));
			return this;
		}

		public InventoryListPanel addInv(Inventory[] inventories1, Inventory[] inventories2) {
			for (int i=0; i<inventories1.length; ++i) {
				contentPanel.add(new InventoryPanel(mainwindow,inventories1[i],true,false,this));
				if (inventories2!=null && i<inventories2.length)
					contentPanel.add(new InventoryPanel(mainwindow,inventories2[i],true,false,this));
			}
			if (inventories2!=null)
				for (int i=inventories1.length; i<inventories2.length; ++i)
					contentPanel.add(new InventoryPanel(mainwindow,inventories2[i],true,false,this));
		
			return this;
		}
	
	}
}
