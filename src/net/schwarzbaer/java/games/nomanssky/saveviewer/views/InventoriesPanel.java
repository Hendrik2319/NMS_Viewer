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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.function.Predicate;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.schwarzbaer.gui.Canvas;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Debug;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID;
import net.schwarzbaer.java.games.nomanssky.saveviewer.GameInfos.GeneralizedID.UpgradeClass;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Images.SelectImageDialog;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Inventories.Inventory;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Inventories.Inventory.SlotType;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.VehicleGroup.Vehicle;
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
		addTab("Player"          , new InventoryListPanel(mainwindow).addInv(data.inventories.player.standard,data.inventories.player.tech,data.inventories.player.cargo));
		addTab(data.inventories.grave);
		addTab(data.inventories.multitool);
		addTab("Freighter"       , new InventoryListPanel(mainwindow).addInv(data.freighter.inventory,data.freighter.inventoryTech));
		addTab(data.inventories.ship_old);
		if (data.spaceShips.vehicles!=null) addTab("SpaceShips", new InventoryListPanel(mainwindow).addInv(data.spaceShips.vehicles));
		if (data.exocrafts .vehicles!=null) addTab("Exocrafts" , new InventoryListPanel(mainwindow).addInv(data.exocrafts .vehicles));
		addTab("Containers"      , new InventoryListPanel(mainwindow,0,2).addInv(data.inventories.chests));
		addTab("Magic Chests"    , new InventoryListPanel(mainwindow).addInv(data.inventories.ingredientStorage,data.inventories.magicChest,data.inventories.magicChest2));

		tabbedPane.addChangeListener(new ChangeListener() {
			@Override public void stateChanged(ChangeEvent e) {
				updateSelectedTab();
			}
		});

		add(tabbedPane,BorderLayout.CENTER);
	}


	private void addTab(String title, InventoryListPanel listPanel) {
		tabbedPane.addTab(title, listPanel);
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

	final static class InventoryPanel extends JPanel implements Updatable, ActionListener {
		private static final long serialVersionUID = 8549406812793642121L;
		
		private Inventory inventory;
		private JTextArea textarea;
		private InventoryDisplay inventoryLabel;
		private JPopupMenu contextMenu;

		private Inventory.Slot clickedSlot;
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
			contextMenu.add(SaveViewer.createMenuItem("Edit ID", this, ActionCommand.EditID ));
			contextMenu.addSeparator();;
			contextMenu.add(SaveViewer.createMenuItem("Select Image File", this, ActionCommand.SelectImageFile ));
			contextMenu.add(SaveViewer.createMenuItem("Copy Image File"  , this, ActionCommand.CopyImageFile  , SaveViewer.ToolbarIcons.Copy  ));
			contextMenu.add(SaveViewer.createMenuItem("Paste Image File" , this, ActionCommand.PasteImageFile , SaveViewer.ToolbarIcons.Paste ));
			contextMenu.add(SaveViewer.createMenuItem("Remove Image File", this, ActionCommand.RemoveImageFile, SaveViewer.ToolbarIcons.Delete));
			contextMenu.addSeparator();;
			contextMenu.add(SaveViewer.createMenuItem("Copy Background Color"  , this, ActionCommand.CopyBGColor  , SaveViewer.ToolbarIcons.Copy  ));
			contextMenu.add(SaveViewer.createMenuItem("Paste Background Color" , this, ActionCommand.PasteBGColor , SaveViewer.ToolbarIcons.Paste ));
			contextMenu.add(SaveViewer.createMenuItem("Remove Background Color", this, ActionCommand.RemoveBGColor, SaveViewer.ToolbarIcons.Delete));
			
			if (this.inventory!=null && this.inventory.width!=null && this.inventory.height!=null) {
				inventoryLabel = new InventoryDisplay(this,(int)(long)this.inventory.width,(int)(long)this.inventory.height,this.inventory.slots);
				if (makeInventoryScrollable) {
					JScrollPane scrollPane = new JScrollPane(inventoryLabel);
					scrollPane.getVerticalScrollBar  ().setUnitIncrement(10);
					scrollPane.getHorizontalScrollBar().setUnitIncrement(10);
					add(scrollPane, BorderLayout.CENTER);
				} else
					add(inventoryLabel, BorderLayout.CENTER);
			} else
				inventoryLabel = null;
		}
		
		private enum ActionCommand { EditID, SelectImageFile, CopyImageFile, PasteImageFile, RemoveImageFile, CopyBGColor, PasteBGColor, RemoveBGColor }
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if (clickedSlot == null) return;
			if (clickedSlot.id==null) return;
			
			String cbValue;
			switch(ActionCommand.valueOf(e.getActionCommand())) {
			case EditID: {
				GameInfos.EditIdDialog dlg = new GameInfos.EditIdDialog(mainwindow,clickedSlot.id);
				dlg.showDialog();
				if (dlg.hasDataChanged()) {
					dlg.transferChangesTo(clickedSlot.id);
					updateAfterChangedIDdata();
				}
			} break;
				
			case SelectImageFile: {
				SelectImageDialog dlg = new SelectImageDialog(mainwindow,"Select image of "+clickedSlot.id.getName(),clickedSlot.id.getImageFileName());
				dlg.showDialog();
				if (dlg.hasChoosen()) {
					String result = dlg.getImageFileName();
					clickedSlot.id.setImageFileName(result);
					updateAfterChangedIDdata();
				}
			} break;
				
			case RemoveImageFile: clickedSlot.id.setImageFileName(""); updateAfterChangedIDdata(); break;
			case RemoveBGColor  : clickedSlot.id.setImageBG(null); updateAfterChangedIDdata(); break;
				
			case CopyImageFile: SaveViewer.copyToClipBoard(clickedSlot.id.getImageFileName()); break;
			case CopyBGColor  : if (clickedSlot.id.hasImageBG()) { SaveViewer.copyToClipBoard(String.format("%06X", clickedSlot.id.getImageBG())); } break;
			
			case PasteImageFile:
				cbValue = SaveViewer.pasteFromClipBoard();
				if (cbValue!=null) {
					clickedSlot.id.setImageFileName(cbValue);
					updateAfterChangedIDdata();
				}
				break;
			case PasteBGColor:
				cbValue = SaveViewer.pasteFromClipBoard();
				if (cbValue!=null)
					try {
						clickedSlot.id.setImageBG(Integer.parseInt(cbValue, 16));
						updateAfterChangedIDdata();
					} catch (NumberFormatException e1) {}
				break;
			}
			
			clickedSlot = null;
		}

		@Override
		public void updateContent() {
			if (inventoryLabel != null) inventoryLabel.repaint();
			showValues();
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
			if (clickedSlot!=null && !clickedSlot.isEmpty && clickedSlot.id!=null)
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
				if (inventory.usedSlots!=null)
					textarea.append("Used Slots   : "+inventory.usedSlots+(inventory.validSlots!=null?("/"+inventory.validSlots):"")+"\r\n");
				textarea.append("Class        : "+inventory.inventoryClass+"\r\n");
				textarea.append("is cool      : "+inventory.isCool+"\r\n");
				textarea.append("Version      : "+inventory.version+"\r\n");
				textarea.append("Product MaxStorage Multiplier  : "+inventory.productMaxStorageMultiplier+"\r\n");
				textarea.append("Substance MaxStorage Multiplier: "+inventory.substanceMaxStorageMultiplier+"\r\n");
				if (inventory.baseStatValues==null || inventory.baseStatValues.length==0)
					textarea.append("Base Status Values: none\r\n");
				else {
					textarea.append("Base Status Values:\r\n");
					for (Inventory.BaseStatValue bsv:inventory.baseStatValues)
						textarea.append(String.format("   %s: %s\r\n",bsv.baseStatID,bsv.value));						
				}
			} else {
				Inventory.Slot slot = inventory.slots[inventoryLabel.hoveredSlot.x][inventoryLabel.hoveredSlot.y];
				textarea.append("Inventory Slot "+inventoryLabel.hoveredSlot.x+","+inventoryLabel.hoveredSlot.y+"\r\n");
				if (slot==null) {
					textarea.append("   is invalid\r\n");
				} else
				if (slot.isEmpty) {
					textarea.append("   is empty\r\n");
				} else {
					textarea.append("   Label : "+((slot.id!=null && slot.id.hasLabel ())?slot.id.label :"") +"\r\n");
					textarea.append("   Symbol: "+((slot.id!=null && slot.id.hasSymbol())?slot.id.symbol:"")+"\r\n");
					textarea.append("   ID    : "+(slot.id==null?("\""+slot.idStr+"\""):("["+slot.id.id+"]"))+"\r\n");
					textarea.append("   Type  : "+(slot.type==null?slot.typeStr:slot.type)+"\r\n");
					if (slot.id!=null && slot.id.type!=null)
						textarea.append("           "+slot.id.type.getLabel()+"\r\n");
					textarea.append("   Amount: "+slot.amount+"/"+slot.maxAmount+"\r\n");
					textarea.append("   Damage: "+slot.damageFactor+"\r\n");
				}
			}
		}

		public static class InventoryDisplay extends Canvas implements MouseListener, MouseMotionListener {
			private static final long serialVersionUID = -1799938226122102016L;
			
			private static final boolean DRAW_HOVERED_SLOT_WITH_OVERLAY  = true;
			private static final Color COLOR__SLOT_HOVERED_OVERLAY       = new Color(0x8089c5ff,true);
			private static final Color COLOR__SLOT_HOVERED_OVERLAY_FIXED = new Color(0x8091ff94,true);
			private static final Color COLOR__SLOT_HOVERED       = new Color(0xFFD800);
			private static final Color COLOR__SLOT_HOVERED_FIXED = Color.GREEN;
			private static final Color COLOR__SLOT_EDGE          = Color.BLACK;
			
			private static final Color COLOR__SLOT_TITLE        = Color.BLACK;
			private static final Color COLOR__SLOT_TITLE_IDONLY = Color.RED;
			@SuppressWarnings("unused")
			private static final Color COLOR__SLOT_TITLE_MARKER = Color.GREEN;

			private static final Color COLOR__SLOT_GAUGE        = Color.WHITE;
			private static final Color COLOR__SLOT_GAUGE_EMPTY  = new Color(255,255,255,128);

			private static final Color COLOR__SLOT_TEXT_AMOUNT  = Color.WHITE;
			private static final Color COLOR__SLOT_TEXT_SYMBOL  = Color.BLACK;
			private static final Color COLOR__SLOT_BG_SYMBOL    = new Color(255,255,255,192);
			
			private static final Color COLOR__SLOT_TEXT_LABEL     = Color.RED;
			
			private static final Color COLOR__SLOT_TEXT           = Color.BLUE;
			private static final Color COLOR__SLOT_TEXT_PRODUCT   = new Color(0x4040FF);
			private static final Color COLOR__SLOT_TEXT_SUBSTANCE = new Color(0xFF6900);
			private static final Color COLOR__SLOT_TEXT_TECH      = new Color(0xAD00AD);

			private static final Color COLOR__SLOT_BG            = Color.WHITE;
			private static final Color COLOR__DAMAGED_SLOT_BG    = Color.RED;

			private static final Color COLOR__UPGRCLS_BG     = new Color(0,0,0,160);
			private static final Color COLOR__UPGRCLS_BORDER = new Color(255,255,255,192);
			private static final Color COLOR__UPGRCLS_TEXT   = Color.WHITE;

			private static final Color COLOR__INVENTORY_BG = new Color(0xE0E0E0);
			
			private static final Stroke STROKE__SLOT_HOVERED = new BasicStroke(6.0f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL);
			private static final Stroke STROKE__STANDARD     = new BasicStroke(1.0f);
			
			public static final int SLOT_BORDER = 3;
			public static final int SLOT_WIDTH  = 90;
			public static final int SLOT_HEIGHT = SLOT_WIDTH+13;
			public static final int SLOT_RASTER_X = SLOT_WIDTH +2*SLOT_BORDER;
			public static final int SLOT_RASTER_Y = SLOT_HEIGHT+2*SLOT_BORDER;
			
			private static final int UPGRCLS_WIDTH  = Math.round(SLOT_WIDTH*0.4f);
			private static final int UPGRCLS_HEIGHT = UPGRCLS_WIDTH;
			private static final int UPGRCLS_BORDER = 4;
			private static final int UPGRCLS_CORNER_RADIUS = 7;
			
			private static final float UPGRCLS_STR_FONT_SCALE = 1.7f;
			private static final int UPGRCLS_STR_OFFSET_Y = 6;
			
			private enum UpgrCls {
				S("S",new Color(0xFFA712), 0),
				A("A",new Color(0xA063D8),-1),
				B("B",new Color(0x3C8BD6), 0),
				C("C",new Color(0x3FA782),-1);
				
				private String letter;
				private Color color;
				private int offsetX;

				private UpgrCls(String letter, Color color, int offsetX) {
					this.letter = letter;
					this.color = color;
					this.offsetX = offsetX;
				}
				static UpgrCls get(GeneralizedID.UpgradeClass upgradeClass) {
					if (upgradeClass!=null)
						switch (upgradeClass) {
						case S: return S;
						case A: return A;
						case B: return B;
						case C: return C;
						}
					return null;
				}
			}
			
			private int inventoryWidth;
			private int inventoryHeight;
			private int imageWidth;
			private int imageHeight;
			private Inventory.Slot[][] slots;
			private Point hoveredSlot;
			private InventoryPanel panel;

			private boolean isHoveredSlotFixed;

			public InventoryDisplay(InventoryPanel panel, int inventoryWidth, int inventoryHeight, Inventory.Slot[][] slots) {
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
			protected void paintCanvas(Graphics g, int x, int y, int width, int height) {
				if (!(g instanceof Graphics2D)) return;
				Graphics2D g2 = (Graphics2D)g;
				Font standardFont = getStandardFont(g2);
				g2.setFont(standardFont);
				
				Rectangle baseClip = g2.getClipBounds();
				
				g2.setPaint(COLOR__INVENTORY_BG);
				g2.fillRect(x, y, imageWidth, imageHeight);
				
				if (!DRAW_HOVERED_SLOT_WITH_OVERLAY)
					drawHoveredSlot_Border(g2, x, y, hoveredSlot, isHoveredSlotFixed);
				
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g2.setStroke(STROKE__STANDARD);
				for (int indexX=0; indexX<inventoryWidth; ++indexX)
					for (int indexY=0; indexY<inventoryHeight; ++indexY) {
						//g2.setClip(baseClip);
						Inventory.Slot slot = slots[indexX][indexY];
						if (slot == null)
							continue;
						
						GeneralizedID slotId = slot.id;
						String slotIdStr = slot.idStr;
						Long amount = slot.amount;
						Long maxAmount = slot.maxAmount;
						SlotType type = slot.type;
						String typeStr = slot.typeStr;
						boolean isEmpty = slot.isEmpty;
						String specialSlotType = slot.specialSlotType;
						Double damageFactor = slot.damageFactor;
						
						int x1=x+indexX*SLOT_RASTER_X+SLOT_BORDER;
						int y1=y+indexY*SLOT_RASTER_Y+SLOT_BORDER;
						
						drawSlot(g2, x1,y1, baseClip, standardFont, slotId, slotIdStr, amount, maxAmount, type, typeStr, isEmpty, specialSlotType, damageFactor );
					}
				
				//g2.setClip(baseClip);
				
				if (DRAW_HOVERED_SLOT_WITH_OVERLAY)
					drawHoveredSlot_Overlay(g2, x, y, hoveredSlot, isHoveredSlotFixed);
			}

			@SuppressWarnings("unused")
			public static void drawSlotGrid(Graphics2D g2, int x, int y, int nColumn, GeneralizedID[] slotIDs, Predicate<Integer> isSelected, Integer hovered) {
				Font standardFont = getStandardFont(g2);
				g2.setFont(standardFont);
				Rectangle baseClip = g2.getClipBounds();
				
				if (!DRAW_HOVERED_SLOT_WITH_OVERLAY)
					drawHoveredSlot_Border(g2, x, y, hovered==null?null:new Point(hovered%nColumn,hovered/nColumn), false);
				
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setStroke(STROKE__STANDARD);
				int row = 0;
				while(row*nColumn < slotIDs.length) {
					for (int col=0; col<nColumn && row*nColumn+col<slotIDs.length; col++) {
						int i = row*nColumn+col;
						int x1=x+col*SLOT_RASTER_X+SLOT_BORDER;
						int y1=y+row*SLOT_RASTER_Y+SLOT_BORDER;
						
						if (!DRAW_HOVERED_SLOT_WITH_OVERLAY && isSelected!=null && isSelected.test(i))
							drawHoveredSlot_Border(g2, x1, y1, true);
						
						drawSlotSimple(g2, x1, y1, baseClip, standardFont, slotIDs[i]);
						//g2.setClip(baseClip);
						
						if (DRAW_HOVERED_SLOT_WITH_OVERLAY && isSelected!=null && isSelected.test(i))
							drawHoveredSlot_Overlay(g2, x1, y1, true);
					}
					row++;
				}
				
				//g2.setClip(baseClip);
				
				if (DRAW_HOVERED_SLOT_WITH_OVERLAY)
					drawHoveredSlot_Overlay(g2, x, y, hovered==null?null:new Point(hovered%nColumn,hovered/nColumn), false);
			}

			private static Font getStandardFont(Graphics2D g2) {
				return g2.getFont().deriveFont(Font.PLAIN, 11);
			}

			private static void drawHoveredSlot_Border(Graphics2D g2, int x, int y, Point hoveredSlot, boolean isHoveredSlotFixed) {
				if (hoveredSlot!=null) {
					int xH = x+hoveredSlot.x*SLOT_RASTER_X+SLOT_BORDER;
					int yH = y+hoveredSlot.y*SLOT_RASTER_Y+SLOT_BORDER;
					drawHoveredSlot_Border(g2, xH, yH, isHoveredSlotFixed);
				}
			}
			private static void drawHoveredSlot_Overlay(Graphics2D g2, int x, int y, Point hoveredSlot, boolean isHoveredSlotFixed) {
				if (hoveredSlot!=null) {
					int xH = x+hoveredSlot.x*SLOT_RASTER_X+SLOT_BORDER;
					int yH = y+hoveredSlot.y*SLOT_RASTER_Y+SLOT_BORDER;
					drawHoveredSlot_Overlay(g2, xH, yH, isHoveredSlotFixed);
				}
			}
			private static void drawHoveredSlot_Border(Graphics2D g2, int x, int y, boolean isHoveredSlotFixed) {
				g2.setStroke(STROKE__SLOT_HOVERED);
				g2.setPaint(isHoveredSlotFixed?COLOR__SLOT_HOVERED_FIXED:COLOR__SLOT_HOVERED);
				g2.drawRect(x, y, SLOT_WIDTH, SLOT_HEIGHT);
			}
			private static void drawHoveredSlot_Overlay(Graphics2D g2, int x, int y, boolean isHoveredSlotFixed) {
				//g2.setStroke(STROKE__SLOT_HOVERED);
				g2.setPaint(isHoveredSlotFixed?COLOR__SLOT_HOVERED_OVERLAY_FIXED:COLOR__SLOT_HOVERED_OVERLAY);
				g2.fillRect(x, y, SLOT_WIDTH, SLOT_HEIGHT);
			}

			private static void drawSlotSimple(Graphics2D g2, int x, int y, Rectangle baseClip, Font standardFont, GeneralizedID slotId) {
				drawSlotSimple(g2, x,y, baseClip, standardFont, slotId, null, null);
			}
			private static void drawSlotSimple(Graphics2D g2, int x, int y, Rectangle baseClip, Font standardFont, GeneralizedID slotId, Long amount, Long maxAmount) {
				Debug.Assert(slotId!=null);
				drawSlot(g2, x,y, baseClip, standardFont, slotId, null, amount, maxAmount, null, "???", false, "", null );
			}
			
			private static void drawSlot(Graphics2D g2, int x, int y, Rectangle baseClip, Font standardFont,
					GeneralizedID slotId, String slotIdStr, Long amount, Long maxAmount, SlotType type, String typeStr, boolean isEmpty, String specialSlotType, Double damageFactor ) {
				
				if (!isEmpty) {
					g2.setPaint((damageFactor==null || damageFactor==0)?COLOR__SLOT_BG:COLOR__DAMAGED_SLOT_BG);
					g2.fillRect(x, y, SLOT_WIDTH, SLOT_HEIGHT);
				}
				g2.setPaint(COLOR__SLOT_EDGE);
				g2.drawRect(x, y, SLOT_WIDTH-1, SLOT_HEIGHT-1);
				
				int innerWidth  = SLOT_WIDTH-2;
				int innerHeight = SLOT_HEIGHT-2;
				int innerOffsetX = 1;
				int innerOffsetY = 1;
				
				g2.setClip(baseClip.createIntersection(new Rectangle(x+innerOffsetX, y+innerOffsetY, innerWidth, innerHeight)));
				int imageBorder = 3;
				int imageSize = innerWidth-2*imageBorder;
				int strOffsetX = innerOffsetX+4;
				int strOffsetY = innerOffsetY+12;
				
				if (isEmpty) {
					if (specialSlotType!=null) {
						g2.setPaint(COLOR__SLOT_TITLE);
						g2.drawString(specialSlotType, x+strOffsetX, y+strOffsetY);
					}
					g2.setClip(baseClip);
					return; 
				}
				
				BufferedImage image = slotId==null?null:slotId.getCachedImage(imageSize,imageSize);
				if (image == null) {
					int incrementY = 13;
					g2.setPaint(getSlotTextColor(type));
					
					g2.drawString(type==null?typeStr:type.toString(), x+strOffsetX, y+strOffsetY); strOffsetY+=incrementY;
					
					if (slotId!=null && slotId.hasLabel()) {
						g2.setPaint(COLOR__SLOT_TEXT_LABEL);
						g2.drawString(slotId.label, x+strOffsetX, y+strOffsetY); strOffsetY+=incrementY;
						g2.setPaint(getSlotTextColor(type));
					}
					
					g2.drawString(slotId==null?slotIdStr:slotId.id, x+strOffsetX, y+strOffsetY); strOffsetY+=incrementY;
				
					g2.drawString(String.format("%s/%s", amount, maxAmount), x+strOffsetX, y+strOffsetY); strOffsetY+=incrementY;
					
					g2.setClip(baseClip);
					return; 
				}
				
				Font stdBoldFont = standardFont.deriveFont(Font.BOLD);
				g2.setFont( stdBoldFont );
				
				int markerWidth = 0;
				//if (slot.id!=null && slot.id.type!=null && slot.id.type.isUpgrade){
				//	if (!getUpgrades(slot).isEmpty()) {
				//		g2.setPaint(COLOR__SLOT_TITLE_MARKER);
				//		String marker = "[U]";
				//		g2.drawString(marker, x+strOffsetX, y+strOffsetY);
				//		markerWidth = 2+g2.getFontMetrics().stringWidth(marker);
				//	}
				//}
				
				if (slotId.hasLabel()) {
					g2.setPaint(COLOR__SLOT_TITLE);
					g2.drawString(slotId.label, x+strOffsetX+markerWidth, y+strOffsetY);
				} else {
					g2.setPaint(COLOR__SLOT_TITLE_IDONLY);
					g2.drawString(slotId.id, x+strOffsetX+markerWidth, y+strOffsetY);
				}
				
				//g2.setPaint(getSlotTextColor(slot.type));
				
				int imageX = x+innerOffsetX+imageBorder;
				int imageY = y+innerOffsetY+innerHeight-imageBorder-imageSize;
				g2.drawImage(image, imageX,imageY, null);
				
				if (slotId.upgradeClass!=null)
					//drawUpgradeClass_old(g2, slot.id.upgradeClass, x1, y1, innerHeight, innerOffsetX, innerOffsetY, imageBorder, stdBoldFont);
					drawUpgradeClass(g2, slotId.upgradeClass, imageX,imageY, stdBoldFont);
				
				if (amount!=null && maxAmount!=null)
					drawAmount(g2, amount.intValue(), maxAmount.intValue(), imageX,imageY, imageSize);
				
				if (slotId.hasSymbol())
					drawSymbol(g2, slotId, imageX,imageY, imageBorder, stdBoldFont);
				
				g2.setFont( standardFont );
				g2.setClip(baseClip);
			}

			private static void drawAmount(Graphics2D g2, int amount, int maxAmount, int imageX, int imageY, int imageSize) {
				//int amount    = slot.amount.intValue();
				//int maxAmount = slot.maxAmount.intValue();
					
				if (amount>=0 && maxAmount>1) {
					int gaugeBorder = 3;
					int gaugeWidth  = imageSize-2*gaugeBorder;
					int gaugeHeight = 5;
					int gaugeX = imageX+gaugeBorder;
					int gaugeY = imageY+imageSize-gaugeBorder-gaugeHeight;
					int full = (amount*gaugeWidth)/maxAmount;
					int empty = gaugeWidth-full;
					
					if (full>0) {
						g2.setPaint(COLOR__SLOT_GAUGE);
						g2.fillRect(gaugeX,gaugeY,full,gaugeHeight);
					}
					if (empty>0) {
						g2.setPaint(COLOR__SLOT_GAUGE_EMPTY);
						g2.fillRect(gaugeX+full,gaugeY,empty,gaugeHeight);
					}
					String str = String.format("%d/%d",amount,maxAmount);
					int textWidth = g2.getFontMetrics().stringWidth(str);
					g2.setPaint(COLOR__SLOT_TEXT_AMOUNT);
					g2.drawString(str, gaugeX+full+empty-textWidth, gaugeY-4);
				}
			}

			private static void drawSymbol(Graphics2D g2, GeneralizedID slotID, int imageX, int imageY, int imageBorder, Font stdBoldFont) {
				int bgRecW = 35;
				int bgRecX = imageX+imageBorder;
				int bgRecY = imageY+imageBorder;
				Rectangle2D bounds = stdBoldFont.getStringBounds(slotID.symbol, g2.getFontRenderContext());
				float strX = bgRecX+bgRecW/2.0f-(float)bounds.getWidth()/2;
				float strY = bgRecY+11;
				g2.setPaint(COLOR__SLOT_BG_SYMBOL);
				g2.fillRoundRect( bgRecX,bgRecY, bgRecW,14, 12,12 );
				g2.setPaint(COLOR__SLOT_TEXT_SYMBOL);
				g2.drawString(slotID.symbol, strX,strY );
			}

			@SuppressWarnings("unused")
			private static void drawUpgradeClass_old(Graphics2D g2, UpgradeClass upgradeClass, int x1, int y1, final int innerHeight,
					final int innerOffsetX, final int innerOffsetY, int imageBorder, Font stdBoldFont) {
				UpgrCls upgrCls = UpgrCls.get(upgradeClass);
				int iconX = x1+innerOffsetX+imageBorder+UPGRCLS_BORDER;
				int iconY = y1+innerOffsetY+innerHeight-imageBorder-UPGRCLS_BORDER-UPGRCLS_HEIGHT;
				g2.setPaint(COLOR__UPGRCLS_BG);
				g2.fillRoundRect(iconX,iconY, UPGRCLS_WIDTH, UPGRCLS_HEIGHT, UPGRCLS_CORNER_RADIUS*2, UPGRCLS_CORNER_RADIUS*2);
				g2.setPaint(COLOR__UPGRCLS_BORDER);
				g2.drawRoundRect(iconX,iconY, UPGRCLS_WIDTH-1, UPGRCLS_HEIGHT-1, UPGRCLS_CORNER_RADIUS*2, UPGRCLS_CORNER_RADIUS*2);
				
				int UPGRCLS_ICON_WIDTH  = 18;
				int UPGRCLS_ICON_HEIGHT = 28;
				int[] xPoints = new int[]{
						iconX+UPGRCLS_WIDTH/2-UPGRCLS_ICON_WIDTH/2,
						iconX+UPGRCLS_WIDTH/2+UPGRCLS_ICON_WIDTH/2,
						iconX+UPGRCLS_WIDTH/2+UPGRCLS_ICON_WIDTH/2,
						iconX+UPGRCLS_WIDTH/2,
						iconX+UPGRCLS_WIDTH/2-UPGRCLS_ICON_WIDTH/2,
				};
				int[] yPoints = new int[]{
						iconY+UPGRCLS_HEIGHT/2-UPGRCLS_ICON_HEIGHT/2,
						iconY+UPGRCLS_HEIGHT/2-UPGRCLS_ICON_HEIGHT/2,
						iconY+UPGRCLS_HEIGHT/2+UPGRCLS_ICON_HEIGHT/4,
						iconY+UPGRCLS_HEIGHT/2+UPGRCLS_ICON_HEIGHT/2,
						iconY+UPGRCLS_HEIGHT/2+UPGRCLS_ICON_HEIGHT/4,
				};
				g2.setPaint(upgrCls.color);
				g2.fillPolygon(xPoints, yPoints, 5);
				//Image img = Images.UpgradeCategoryImages.getCachedImage(slot.id.upgradeCat, UPGRCAT_IMG_SIZE,UPGRCAT_IMG_SIZE);
				//g2.drawImage(img, iconX+UPGRCAT_IMG_OFFSET_X, iconY+UPGRCAT_IMG_OFFSET_Y, null);

				g2.setPaint(COLOR__UPGRCLS_TEXT);
				g2.setFont( stdBoldFont.deriveFont(stdBoldFont.getSize()*UPGRCLS_STR_FONT_SCALE) );
				int strX = iconX+UPGRCLS_WIDTH/2-g2.getFontMetrics().stringWidth(upgrCls.letter)/2+upgrCls.offsetX;
				int strY = iconY+UPGRCLS_HEIGHT/2+UPGRCLS_STR_OFFSET_Y;
				g2.drawString(upgrCls.letter, strX, strY);
				g2.setFont( stdBoldFont );
			}

			private static void drawUpgradeClass(Graphics2D g2, UpgradeClass upgradeClass, int imageX, int imageY, Font stdBoldFont) {
				UpgrCls upgrCls = UpgrCls.get(upgradeClass);
				int iconX = imageX+UPGRCLS_BORDER;
				int iconY = imageY+UPGRCLS_BORDER;
				
				int UPGRCLS_ICON_WIDTH  = 18;
				int UPGRCLS_ICON_HEIGHT = 28;
				int[] xPoints = new int[]{
						iconX,
						iconX+UPGRCLS_ICON_WIDTH,
						iconX+UPGRCLS_ICON_WIDTH,
						iconX+UPGRCLS_ICON_WIDTH/2,
						iconX,
				};
				int[] yPoints = new int[]{
						iconY,
						iconY,
						iconY+UPGRCLS_ICON_HEIGHT*3/4,
						iconY+UPGRCLS_ICON_HEIGHT,
						iconY+UPGRCLS_ICON_HEIGHT*3/4,
				};
				g2.setPaint(upgrCls.color);
				g2.fillPolygon(xPoints, yPoints, 5);
				g2.setPaint(upgrCls.color.darker());
				g2.fillPolygon(Arrays.copyOfRange(xPoints,1,5), Arrays.copyOfRange(yPoints,1,5), 4);
				g2.setPaint(Color.BLACK);
				g2.drawPolygon(xPoints, yPoints, 5);
				g2.drawLine( xPoints[2],yPoints[2]-4, xPoints[3],yPoints[3]-4 );
				g2.drawLine( xPoints[4],yPoints[4]-4, xPoints[3],yPoints[3]-4 );

				g2.setPaint(COLOR__UPGRCLS_TEXT);
				g2.setFont( stdBoldFont.deriveFont(stdBoldFont.getSize()*UPGRCLS_STR_FONT_SCALE) );
				int strX = iconX+UPGRCLS_ICON_WIDTH /2-g2.getFontMetrics().stringWidth(upgrCls.letter)/2+upgrCls.offsetX+1;
				int strY = iconY+UPGRCLS_ICON_HEIGHT/2+UPGRCLS_STR_OFFSET_Y-3;
				g2.drawString(upgrCls.letter, strX, strY);
				g2.setFont( stdBoldFont );
			}

			private static Color getSlotTextColor(Inventory.SlotType type) {
				if (type!=null)
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

		private InventoryListPanel(Window mainwindow, JPanel contentPanel) {
			super();
			this.mainwindow = mainwindow;
			this.contentPanel = contentPanel;
			setViewportView(this.contentPanel);
			getVerticalScrollBar  ().setUnitIncrement(10);
			getHorizontalScrollBar().setUnitIncrement(10);
		}

		public InventoryListPanel(Window mainwindow, int rows, int cols) {
			this(mainwindow,new JPanel(new GridLayout(rows, cols)));
		}
		
		public InventoryListPanel(Window mainwindow) {
			this(mainwindow,new JPanel());
			contentPanel.setLayout(new BoxLayout(contentPanel,BoxLayout.Y_AXIS));
		}

		@Override
		public void updateContent() {
			for (Component comp:contentPanel.getComponents())
				if (comp instanceof Updatable)
					((Updatable)comp).updateContent();
		}

		public InventoryListPanel addInv(Inventory... inventories) {
			for (int i=0; i<inventories.length; ++i)
				if (inventories[i]!=null)
					contentPanel.add(new InventoryPanel(mainwindow,inventories[i],true,false,this));
			return this;
		}

		public InventoryListPanel addInv(Vehicle... vehicles) {
			for (int i=0; i<vehicles.length; ++i)
				if (vehicles[i]!=null) {
				contentPanel.add(new InventoryPanel(mainwindow,vehicles[i].inventory    ,true,false,this));
				contentPanel.add(new InventoryPanel(mainwindow,vehicles[i].inventoryTech,true,false,this));
			}
			return this;
		}
/*
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
*/	
	}
}
