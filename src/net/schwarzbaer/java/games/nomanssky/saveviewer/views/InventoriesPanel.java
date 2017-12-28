package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import net.schwarzbaer.gui.Canvas;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveViewer;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Inventory;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Inventory.BaseStatValue;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Inventory.Slot;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.SaveGameView.SaveGameViewTabPanel;

final class InventoriesPanel extends SaveGameViewTabPanel {
	private static final long serialVersionUID = -6965281963499623839L;

	public InventoriesPanel(SaveGameData data) {
		super(data);
		
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Player"        , new InventoryPanel(data.inventories.player));
		tabbedPane.addTab("Player (Tech)" , new InventoryPanel(data.inventories.playerTech));
		tabbedPane.addTab("Player (Cargo)", new InventoryPanel(data.inventories.playerCargo));
		tabbedPane.addTab("MultiTool"     , new InventoryPanel(data.inventories.multitool));
//		tabbedPane.addTab("Player", new InventoryPanel(data.inventories.player));
//		tabbedPane.addTab("Player", new InventoryPanel(data.inventories.player));
//		tabbedPane.addTab("Player", new InventoryPanel(data.inventories.player));
//		tabbedPane.addTab("Player", new InventoryPanel(data.inventories.player));
//		tabbedPane.addTab("Player", new InventoryPanel(data.inventories.player));
//		tabbedPane.addTab("Player", new InventoryPanel(data.inventories.player));
		// TODO
		
		add(tabbedPane,BorderLayout.CENTER);
	}
	
	
	@Override
	public void updateContent() {}


	final static class InventoryPanel extends JPanel {
		private static final long serialVersionUID = 8549406812793642121L;
		
		private Inventory inventory;
		private JTextArea textarea;
//		private JLabel inventoryLabel;
		private InventoryDisplay inventoryLabel;
		private JPopupMenu contextMenu;

		private JMenuItem miSetLabel;
		private Slot clickedSlot;

		public InventoryPanel(Inventory inventory) {
			super(new BorderLayout(3, 3));
			setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			
			this.inventory = inventory;
			clickedSlot = null;
			
			textarea = new JTextArea();
			textarea.setEditable(false);
			JScrollPane textareaScrollPane = new JScrollPane(textarea);
			textareaScrollPane.getViewport().setPreferredSize(new Dimension(500, 300));
			add(textareaScrollPane, BorderLayout.EAST);
			showValues();
			
			contextMenu = new JPopupMenu();
			contextMenu.add(miSetLabel=createMenuItem("Set Label", e->setLabelForResource()));
			
			if (this.inventory!=null && this.inventory.width!=null && this.inventory.height!=null) {
				inventoryLabel = new InventoryDisplay(this,(int)(long)this.inventory.width,(int)(long)this.inventory.height,this.inventory.slots);
				add(new JScrollPane(inventoryLabel), BorderLayout.CENTER);
			} else
				inventoryLabel = null;
		}

		private void setLabelForResource() {
			if (clickedSlot == null) return;
			
			if (clickedSlot.id_==null || clickedSlot.type==null) return;
			
			String name = JOptionPane.showInputDialog(this, String.format("New name for %s ID \"%s\"", clickedSlot.type, clickedSlot.id_.id), clickedSlot.id_.label);
			if (name!=null) {
				clickedSlot.id_.label = name;
				switch(clickedSlot.type) {
				case Product   : SaveViewer.saveProductIDsToFile();   break;
				case Technology: SaveViewer.saveTechIDsToFile();      break;
				case Substance : SaveViewer.saveSubstanceIDsToFile(); break;
				}
				inventoryLabel.repaint();
			}
			clickedSlot = null;
		}

		private void showContextMenu(Component invoker, int screenX, int screenY) {
			if (!isValidSlotHovered()) return;
			clickedSlot = inventory.slots[inventoryLabel.hoveredSlot.x][inventoryLabel.hoveredSlot.y];
			miSetLabel.setEnabled(!clickedSlot.isEmpty && clickedSlot.id_!=null && clickedSlot.type!=null);
			contextMenu.show(invoker, screenX, screenY);
		}

		private boolean isValidSlotHovered() {
			if (inventory.slots == null) { System.out.println(1); return false; }
			if (inventoryLabel == null) { System.out.println(2); return false; }
			if (inventoryLabel.hoveredSlot == null) { System.out.println(3); return false; }
			if (inventoryLabel.hoveredSlot.x >= inventory.slots.length) { System.out.println(4); return false; }
			if (inventoryLabel.hoveredSlot.y >= inventory.slots[inventoryLabel.hoveredSlot.x].length) { System.out.println(5); return false; }
			if (inventory.slots[inventoryLabel.hoveredSlot.x][inventoryLabel.hoveredSlot.y] == null) { System.out.println(6); return false; }
			return  true;
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
					textarea.append("   ID    : "+(slot.id_==null?("\""+slot.idStr+"\""):(slot.id_.label+" ["+slot.id_.id+"]"))+"\r\n");
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
			private static final Color COLOR__SLOT_TEXT = Color.BLUE;
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
								g2.setPaint(COLOR__SLOT_TEXT);
								int offset = 13;
								g2.drawString(slot.type==null?slot.typeStr:slot.type.toString(), xS+3, yS+offset); offset+=10;
								if (slot.id_!=null && !slot.id_.label.isEmpty()) {
									g2.drawString(slot.id_.label, xS+3, yS+offset); offset+=10;
								}
								g2.drawString(slot.id_==null?slot.idStr:slot.id_.id, xS+3, yS+offset); offset+=10;
								g2.drawString(String.format("%d/%d", slot.amount, slot.maxAmount), xS+3, yS+offset); offset+=10;
							}
						}
					}
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
}
