package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

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

	public InventoriesPanel(SaveGameData data) {
		super(data);
		
		tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Player"          , new InventoryPanel(data.inventories.player));
		tabbedPane.addTab("Player (Tech)"   , new InventoryPanel(data.inventories.playerTech));
		tabbedPane.addTab("Player (Cargo)"  , new InventoryPanel(data.inventories.playerCargo));
		tabbedPane.addTab("Grave"           , new InventoryPanel(data.inventories.grave));
		tabbedPane.addTab("MultiTool"       , new InventoryPanel(data.inventories.multitool));
		tabbedPane.addTab("Freighter"       , new InventoryPanel(data.inventories.freighter));
		tabbedPane.addTab("Freighter (Tech)", new InventoryPanel(data.inventories.freighterTech));
		tabbedPane.addTab("Ship (old)"      , new InventoryPanel(data.inventories.ship_old));
		tabbedPane.addTab("Ships"           , new InventoryListPanel().add(data.inventories.ships,"Ship",1,data.inventories.ships_Tech,"Ship Tech",1));
		tabbedPane.addTab("Vehicles"        , new InventoryListPanel().add(data.inventories.vehicles,"Vehicle",1));
		tabbedPane.addTab("Containers"      , new InventoryListPanel().add(data.inventories.chests,"Container",0));
		tabbedPane.addTab("Magic Chests"    , new InventoryListPanel().add(data.inventories.magicChest,"Magic Chest").add(data.inventories.magicChest2,"Magic Chest 2"));

		tabbedPane.addChangeListener(new ChangeListener() {
			@Override public void stateChanged(ChangeEvent e) {
				updateSelectedTab();
			}
		});

		add(tabbedPane,BorderLayout.CENTER);
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

	final static class InventoryPanel 
	extends JPanel implements Updatable {
		private static final long serialVersionUID = 8549406812793642121L;
		
		private Inventory inventory;
		private JTextArea textarea;
//		private JLabel inventoryLabel;
		private InventoryDisplay inventoryLabel;
		private JPopupMenu contextMenu;

		private JMenuItem miSetLabel;
		private Slot clickedSlot;
		private Updatable updateListener;

		public InventoryPanel(Inventory inventory) {
			this(null,inventory, true, null);
		}
		
		public InventoryPanel(String title, Inventory inventory, boolean makeInventoryScrollable, Updatable updateListener) {
			super(new BorderLayout(3, 3));
			if (title==null) setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			else             setBorder(BorderFactory.createTitledBorder(title));
			
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
			contextMenu.add(miSetLabel=createMenuItem("Set Label", e->setLabelForResource()));
			
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

		private void setLabelForResource() {
			if (clickedSlot == null) return;
			
			if (clickedSlot.id==null || clickedSlot.type==null) return;
			
			String name = JOptionPane.showInputDialog(this, String.format("New name for %s ID \"%s\"", clickedSlot.type, clickedSlot.id.id), clickedSlot.id.label);
			if (name!=null) {
				clickedSlot.id.label = name;
				switch(clickedSlot.type) {
				case Product   : SaveViewer.saveProductIDsToFile();   break;
				case Technology: SaveViewer.saveTechIDsToFile();      break;
				case Substance : SaveViewer.saveSubstanceIDsToFile(); break;
				}
				if (updateListener!=null) updateListener.updateContent();
				else inventoryLabel.repaint();
			}
			clickedSlot = null;
		}

		private void showContextMenu(Component invoker, int screenX, int screenY) {
			if (!isValidSlotHovered()) return;
			clickedSlot = inventory.slots[inventoryLabel.hoveredSlot.x][inventoryLabel.hoveredSlot.y];
			miSetLabel.setEnabled(!clickedSlot.isEmpty && clickedSlot.id!=null && clickedSlot.type!=null);
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
					textarea.append("   ID    : "+(slot.id==null?("\""+slot.idStr+"\""):(slot.id.label+" ["+slot.id.id+"]"))+"\r\n");
					textarea.append("   Type  : "+(slot.type==null?slot.typeStr:slot.type)+"\r\n");
					textarea.append("   Amount: "+slot.amount+"/"+slot.maxAmount+"\r\n");
					textarea.append("   Damage: "+slot.damageFactor+"\r\n");
				}
			}
		}

		private static class InventoryDisplay extends Canvas implements MouseListener, MouseMotionListener {
			private static final long serialVersionUID = -1799938226122102016L;
			
			private static final Color COLOR__SLOT_HOVERED = Color.GREEN;
			private static final Color COLOR__SLOT_EDGE = Color.BLACK;
			
			private static final Paint COLOR__SLOT_TEXT_LABEL = Color.RED;
			private static final Color COLOR__SLOT_TEXT = Color.BLUE;
			private static final Color COLOR__SLOT_TEXT_PRODUCT = new Color(0x4040FF);
			private static final Color COLOR__SLOT_TEXT_SUBSTANCE = new Color(0xE07000);
			private static final Color COLOR__SLOT_TEXT_TECH = new Color(0x800080);

			private static final Color COLOR__SLOT_BG = Color.WHITE;
			private static final Color COLOR__INVENTORY_BG = new Color(0xF0F0FF);
			
			private static final Stroke STROKE__SLOT_HOVERED = new BasicStroke(6.0f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL);
			private static final Stroke STROKE__STANDARD     = new BasicStroke(1.0f);
			
			private static final int SLOT_BORDER = 5;
			private static final int SLOT_WIDTH  = 107;
			private static final int SLOT_HEIGHT = 125;
			private static final int SLOT_RASTER_X = SLOT_WIDTH +2*SLOT_BORDER;
			private static final int SLOT_RASTER_Y = SLOT_HEIGHT+2*SLOT_BORDER;

			private int inventoryWidth;
			private int inventoryHeight;
			private int imageWidth;
			private int imageHeight;
			private Slot[][] slots;
			private Point hoveredSlot;
			private InventoryPanel panel;

			public InventoryDisplay(InventoryPanel panel, int inventoryWidth, int inventoryHeight, Slot[][] slots) {
				this.panel = panel;
				this.inventoryWidth = inventoryWidth;
				this.inventoryHeight = inventoryHeight;
				this.slots = slots;
				this.hoveredSlot = null;
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
				
				Rectangle baseClip = g2.getClipBounds();
				
				g2.setPaint(COLOR__INVENTORY_BG);
				g2.fillRect(0, 0, imageWidth, imageHeight);
				
				if (hoveredSlot!=null) {
					g2.setStroke(STROKE__SLOT_HOVERED);
					g2.setPaint(COLOR__SLOT_HOVERED);
					int xS=hoveredSlot.x*SLOT_RASTER_X+SLOT_BORDER;
					int yS=hoveredSlot.y*SLOT_RASTER_Y+SLOT_BORDER;
					g2.drawRect(xS, yS, SLOT_WIDTH, SLOT_HEIGHT);
				}
				
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g2.setStroke(STROKE__STANDARD);
				for (int x=0; x<inventoryWidth; ++x)
					for (int y=0; y<inventoryHeight; ++y) {
						Slot slot = slots[x][y];
						if (slot!=null) {
							int xS=x*SLOT_RASTER_X+SLOT_BORDER;
							int yS=y*SLOT_RASTER_Y+SLOT_BORDER;
							if (!slot.isEmpty) {
								g2.setPaint(COLOR__SLOT_BG);
								g2.fillRect(xS, yS, SLOT_WIDTH, SLOT_HEIGHT);
							}
							g2.setPaint(COLOR__SLOT_EDGE);
							g2.drawRect(xS, yS, SLOT_WIDTH-1, SLOT_HEIGHT-1);
							
							if (!slot.isEmpty) {
//								g2.setClip(xS+1, yS+1, SLOT_WIDTH-2, SLOT_HEIGHT-2);
								g2.setClip(baseClip.createIntersection(new Rectangle(xS+1, yS+1, SLOT_WIDTH-2, SLOT_HEIGHT-2)));
								g2.setPaint(getSlotTextColor(slot.type));
								int offset = 13;
								
								g2.drawString(slot.type==null?slot.typeStr:slot.type.toString(), xS+3, yS+offset); offset+=10;
								
								if (slot.id!=null && !slot.id.label.isEmpty()) {
									g2.setPaint(COLOR__SLOT_TEXT_LABEL);
									g2.drawString(slot.id.label, xS+3, yS+offset); offset+=10;
									g2.setPaint(getSlotTextColor(slot.type));
								}
								
								g2.drawString(slot.id==null?slot.idStr:slot.id.id, xS+3, yS+offset); offset+=10;

								g2.drawString(String.format("%d/%d", slot.amount, slot.maxAmount), xS+3, yS+offset); offset+=10;
								
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

			@Override public void mouseEntered (MouseEvent e) { if (hoveredSlotChanged(e.getX(),e.getY())) { panel.showValues(); repaint(); } }
			@Override public void mouseMoved   (MouseEvent e) { if (hoveredSlotChanged(e.getX(),e.getY())) { panel.showValues(); repaint(); } }
			@Override public void mouseExited  (MouseEvent e) { hoveredSlot = null; panel.showValues(); repaint(); }
			@Override public void mouseClicked (MouseEvent e) { if (e.getButton()==MouseEvent.BUTTON3) panel.showContextMenu(this,e.getX(),e.getY()); }
			@Override public void mousePressed (MouseEvent e) {}
			@Override public void mouseReleased(MouseEvent e) {}
			@Override public void mouseDragged (MouseEvent e) {}
		}
		
	}

	private static final class InventoryListPanel extends JScrollPane implements Updatable {
		private static final long serialVersionUID = -662233636434233389L;

		private JPanel contentPanel;

		public InventoryListPanel() {
			super();
			contentPanel = new JPanel();
//			contentPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 3));
			contentPanel.setLayout(new BoxLayout(contentPanel,BoxLayout.Y_AXIS));
			setViewportView(contentPanel);
		}

		@Override
		public void updateContent() {
			for (Component comp:contentPanel.getComponents())
				if (comp instanceof Updatable)
					((Updatable)comp).updateContent();
		}
		
		public InventoryListPanel add(Inventory inventory, String label) {
			contentPanel.add(new InventoryPanel(label,inventory,false,this));
			return this;
		}

		public InventoryListPanel add(Inventory[] inventories, String label, int firstIndex) {
			for (int i=0; i<inventories.length; ++i)
				contentPanel.add(new InventoryPanel(label+" "+(i+firstIndex),inventories[i],false,this));
			return this;
		}

		public InventoryListPanel add(Inventory[] inventories1, String label1, int firstIndex1, Inventory[] inventories2, String label2, int firstIndex2) {
			for (int i=0; i<inventories1.length; ++i) {
				contentPanel.add(new InventoryPanel(label1+" "+(i+firstIndex1),inventories1[i],false,this));
				if (inventories2!=null && i<inventories2.length)
					contentPanel.add(new InventoryPanel(label2+" "+(i+firstIndex2),inventories2[i],false,this));
			}
			if (inventories2!=null)
				for (int i=inventories1.length; i<inventories2.length; ++i)
					contentPanel.add(new InventoryPanel(label2+" "+(i+firstIndex2),inventories2[i],false,this));
		
			return this;
		}
	
	}
}
