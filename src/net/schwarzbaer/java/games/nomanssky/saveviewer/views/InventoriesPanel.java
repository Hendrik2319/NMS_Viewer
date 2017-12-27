package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;

import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData;
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
		
		add(tabbedPane,BorderLayout.CENTER);
	}
	
	
	@Override
	public void updateContent() {}


	final static class InventoryPanel extends JPanel implements MouseListener, MouseMotionListener {
		private static final long serialVersionUID = 8549406812793642121L;
		private static final int BORDER = 5;
		private static final int SLOT_WIDTH  = 107;
		private static final int SLOT_HEIGHT = 126;
		private static final int SLOT_RASTER_X = SLOT_WIDTH+3;
		private static final int SLOT_RASTER_Y = SLOT_HEIGHT+4;
		
		private Inventory inventory;
		private JTextArea textarea;
		private Point hoveredSlot;
		private JLabel inventoryLabel;
		private JScrollPane inventoryLabelScrollPane;

		public InventoryPanel(Inventory inventory) {
			super(new BorderLayout(3, 3));
			setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			
			this.inventory = inventory;
			this.hoveredSlot = null;
			
			textarea = new JTextArea();
			textarea.setEditable(false);
			JScrollPane textareaScrollPane = new JScrollPane(textarea);
			textareaScrollPane.getViewport().setPreferredSize(new Dimension(500, 300));
			add(textareaScrollPane, BorderLayout.EAST);
			showValues();
			
			if (this.inventory!=null && this.inventory.width!=null && this.inventory.height!=null) {
				inventoryLabel = new JLabel();
				//inventoryLabel.setBorder(BorderFactory.createEtchedBorder());
				inventoryLabel.setIcon(makeInvImage((int)(long)this.inventory.width,(int)(long)this.inventory.height,this.inventory.slots));
				inventoryLabel.addMouseListener(this);
				inventoryLabel.addMouseMotionListener(this);
				inventoryLabelScrollPane = new JScrollPane(inventoryLabel);
				add(inventoryLabelScrollPane, BorderLayout.CENTER);
			} else {
				inventoryLabel = null;
				inventoryLabelScrollPane = null;
			}
		}

		private Icon makeInvImage(int width, int height, Slot[][] slots) {
			if (width<=0 || height<=0) return null;
			
			int imageWidth  = (width -1)*SLOT_RASTER_X+SLOT_WIDTH +2*BORDER;
			int imageHeight = (height-1)*SLOT_RASTER_Y+SLOT_HEIGHT+2*BORDER;
			BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
			
			Graphics g = image.getGraphics();
			if (!(g instanceof Graphics2D)) return null;
			Graphics2D g2 = (Graphics2D)g;
			
			g2.setPaint(Color.BLACK);
			g2.drawRect(0, 0, imageWidth, imageHeight);
			
			for (int x=0; x<width; ++x)
				for (int y=0; y<height; ++y)
					if (slots[x][y]!=null) {
						int x_=x*SLOT_RASTER_X+BORDER;
						int y_=y*SLOT_RASTER_Y+BORDER;
						if (!slots[x][y].isEmpty) {
							g2.setPaint(Color.WHITE);
							g2.fillRect(x_, y_, SLOT_WIDTH, SLOT_HEIGHT);
						}
						g2.setPaint(Color.BLACK);
						g2.drawRect(x_, y_, SLOT_WIDTH, SLOT_HEIGHT);
					}

			return new ImageIcon(image);
		}

		private void showValues() {
			textarea.setText("");
			if (inventoryLabel!=null) {
				textarea.append("IconTextGap  : "+inventoryLabel.getIconTextGap()+"\r\n");
				textarea.append("X,Y          : "+inventoryLabel.getX()+","+inventoryLabel.getY()+"\r\n");
				textarea.append("Insets       : "+inventoryLabel.getInsets()+"\r\n");
				textarea.append("Bounds       : "+inventoryLabel.getBounds()+"\r\n");
				textarea.append("MousePosition: "+inventoryLabel.getMousePosition()+"\r\n");
				JViewport viewport = inventoryLabelScrollPane.getViewport();
				textarea.append("viewport.ViewPos   : "+viewport.getViewPosition()+"\r\n");
				textarea.append("viewport.ExtentSize: "+viewport.getExtentSize()+"\r\n");
				textarea.append("viewport.MousePos  : "+viewport.getMousePosition()+"\r\n");
				textarea.append("\r\n");
			}
			
			if (hoveredSlot==null || inventory.slots==null || inventory.slots.length<=hoveredSlot.x || inventory.slots[hoveredSlot.x].length<=hoveredSlot.y) {
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
				Slot slot = inventory.slots[hoveredSlot.x][hoveredSlot.y];
				textarea.append("Inventory Slot "+hoveredSlot.x+","+hoveredSlot.y+"\r\n");
				if (slot==null) {
					textarea.append("   is invalid\r\n");
				} else
				if (slot.isEmpty) {
					textarea.append("   is valid, but empty\r\n");
				} else {
					textarea.append("   ID    : "+slot.id+"\r\n");
					textarea.append("   Type  : "+slot.type+"\r\n");
					textarea.append("   Amount: "+slot.amount+"/"+slot.maxAmount+"\r\n");
					textarea.append("   Damage: "+slot.damageFactor+"\r\n");
				}
			}
		}

		private boolean hoveredSlotChanged(int screenX, int screenY) {
			int x = (screenX-BORDER)/SLOT_RASTER_X;
			int y = (screenY-BORDER)/SLOT_RASTER_Y;
			if (screenX-BORDER-x*SLOT_RASTER_X > SLOT_WIDTH ) x = -1;
			if (screenY-BORDER-y*SLOT_RASTER_Y > SLOT_HEIGHT) y = -1;
			Point newP = null;
			if (x>=0 && y>=0) newP = new Point(x, y);
			
			if ((hoveredSlot==null)!=(newP==null)) { hoveredSlot = newP; return true; }
			if ((hoveredSlot==null)&&(newP==null)) { return false; }
			if (hoveredSlot.x==newP.x && hoveredSlot.y==newP.y) { return false; }
			hoveredSlot.setLocation(newP);
			return true;
		}

		@Override public void mouseEntered (MouseEvent e) { if (hoveredSlotChanged(e.getX(),e.getY())) showValues(); }
		@Override public void mouseMoved   (MouseEvent e) { if (hoveredSlotChanged(e.getX(),e.getY())) showValues(); }
		@Override public void mouseExited  (MouseEvent e) { hoveredSlot = null; showValues(); }
		@Override public void mouseClicked (MouseEvent e) {}
		@Override public void mousePressed (MouseEvent e) {}
		@Override public void mouseReleased(MouseEvent e) {}
		@Override public void mouseDragged (MouseEvent e) {}
		
	}
}
