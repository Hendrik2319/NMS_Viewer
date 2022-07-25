package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;
import java.util.function.BiConsumer;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;

import net.schwarzbaer.gui.Canvas;
import net.schwarzbaer.gui.FileChooser;
import net.schwarzbaer.gui.ProgressDialog;
import net.schwarzbaer.java.games.nomanssky.saveviewer.FileExport;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Gui;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Point3D;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Galaxy;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Region;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.SolarSystem;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.UniverseAddress;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.SaveGameView.SaveGameViewTabPanel;

class GalaxyMapPanel extends SaveGameViewTabPanel {
	private static final long serialVersionUID = 9055290876621464068L;

	private Window mainWindow;
	private UniversePanel universePanel;
	
	private GalaxyMap galaxyMap;
	private JScrollBar scrollBarHoriz;
	private JScrollBar scrollBarVert;

	private JComboBox<ZoomStep> zoomField;
	private JLabel statusField;
	
	private Galaxy currentGalaxy;
	private RegionData currentRegionData;

	private JComboBox<Galaxy> cmbbxGalaxies;
	
	private static class ZoomStep {
		private double value;
		private ZoomStep(double value) { this.value = value; }
		@Override public String toString() { return String.format(Locale.ENGLISH, "%1.1f%%", value*100); }
		public static ZoomStep[] create(double[] zoomSteps) {
			ZoomStep[] arr = new ZoomStep[zoomSteps.length];
			for (int i=0; i<zoomSteps.length; ++i) arr[i] = new ZoomStep(zoomSteps[i]);
			return arr;
		}
	}
	private static class GlyphNumber {
		private int value;
		private GlyphNumber(int value) { this.value = value; }
		@Override public String toString() {
			switch (value) {
			case 1 : return "Glyph 0";
			case 17: return "Known Glyphs";
			default: return String.format("Glyphs 0..%d", value-1);
			}
		}
		public static GlyphNumber[] create() {
			GlyphNumber[] arr = new GlyphNumber[17];
			for (int i=0; i<arr.length; ++i) arr[i] = new GlyphNumber(i+1);
			return arr;
		}
	}
	
	public GalaxyMapPanel(SaveGameData data, Window mainWindow) {
		super(data);
		this.mainWindow = mainWindow;
		this.universePanel = null;
		
		CombinedListener combiListener = new CombinedListener();
		
		int preselectedGalaxy = 0;
		currentGalaxy = data.universe.galaxies.get(preselectedGalaxy);
		currentRegionData = new RegionData(currentGalaxy);
		//knownGlyphs = 0b110111100L;
		galaxyMap = new GalaxyMap(combiListener,currentRegionData,data.general.currentUniverseAddress,data.general.knownGlyphsMask);
		
		combiListener.setContextMenu(new ContextMenu(galaxyMap));
		
		cmbbxGalaxies = new JComboBox<>(data.universe.galaxies);
		cmbbxGalaxies.setSelectedIndex(preselectedGalaxy);
		cmbbxGalaxies.addActionListener(e->galaxyMap.setGalaxy(currentRegionData = new RegionData(currentGalaxy = (Galaxy)cmbbxGalaxies.getSelectedItem())));
		
		zoomField = new JComboBox<ZoomStep>(ZoomStep.create(new double[]{0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0,1.25,1.5,1.75,2.0,2.5,3.0,3.5,4.0,5.0,6.0,7.0,8.0,10.0}));
		zoomField.addActionListener( e->{
			ZoomStep zoomStep = (ZoomStep)zoomField.getSelectedItem();
			if (zoomStep!=null) { galaxyMap.setZoom(zoomStep.value); showStatus(-1,-1); }
		});
		
		statusField = new JLabel("");
		
		JCheckBox chkbxShowGlyphRegions = new JCheckBox("Show region reachable by known glyphs", false);
		JComboBox<GlyphNumber> cmbbxKnownGlyphs = new JComboBox<>(GlyphNumber.create());
		cmbbxKnownGlyphs.setSelectedItem(null);
		cmbbxKnownGlyphs.setEnabled(false);

		chkbxShowGlyphRegions.addActionListener(e->{
			boolean show = chkbxShowGlyphRegions.isSelected();
			cmbbxKnownGlyphs.setEnabled(show);
			showGlyphOverlay(show?cmbbxKnownGlyphs.getSelectedIndex()+1:0);
		});
		cmbbxKnownGlyphs.addActionListener(e->showGlyphOverlay(cmbbxKnownGlyphs.getSelectedIndex()+1));
		
		JPanel leftStatusPanel = new JPanel();
		leftStatusPanel.setLayout(new BoxLayout(leftStatusPanel, BoxLayout.X_AXIS));
		leftStatusPanel.add(cmbbxGalaxies);
		leftStatusPanel.add(zoomField);
		
		JPanel rightStatusPanel = new JPanel();
		rightStatusPanel.setLayout(new BoxLayout(rightStatusPanel, BoxLayout.X_AXIS));
		rightStatusPanel.add(chkbxShowGlyphRegions);
		rightStatusPanel.add(cmbbxKnownGlyphs);
		
		JPanel statusPanel = new JPanel(new BorderLayout(3,3));
		statusPanel.add(leftStatusPanel,BorderLayout.WEST);
		statusPanel.add(statusField,BorderLayout.CENTER);
		statusPanel.add(rightStatusPanel,BorderLayout.EAST);
		
		galaxyMap.prepareMap();
		galaxyMap.addMouseWheelListener(combiListener);
		galaxyMap.addMouseMotionListener(combiListener);
		galaxyMap.addMouseListener(combiListener);
		
		scrollBarVert = new JScrollBar(JScrollBar.VERTICAL);
		scrollBarHoriz = new JScrollBar(JScrollBar.HORIZONTAL);
		
		scrollBarVert.addAdjustmentListener(combiListener);
		scrollBarHoriz.addAdjustmentListener(combiListener);
		
		JPanel mapview = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		mapview.setLayout(layout);
		
		addComp(mapview,layout,c, galaxyMap     , 1,1,1,GridBagConstraints.BOTH);
		addComp(mapview,layout,c, scrollBarVert , 0,1,GridBagConstraints.REMAINDER,GridBagConstraints.VERTICAL);
		addComp(mapview,layout,c, scrollBarHoriz, 1,0,1,GridBagConstraints.HORIZONTAL);
		addComp(mapview,layout,c, new JLabel()  , 0,0,GridBagConstraints.REMAINDER,GridBagConstraints.NONE);
		
		add(mapview,BorderLayout.CENTER);
		add(statusPanel,BorderLayout.SOUTH);
		
		zoomField.setSelectedItem(null);
	}

	public void setUniversePanel(UniversePanel universePanel) {
		this.universePanel = universePanel;
	}

	public void updateBlackHoleConnections() {
		currentRegionData.updateBlackHoleConnections();
	}

	public void updateUniverseData() {
		if (currentGalaxy!=null)
			currentGalaxy = data.universe.findGalaxy(currentGalaxy.galaxyIndex);
		cmbbxGalaxies.setModel(new DefaultComboBoxModel<>(data.universe.galaxies));
		cmbbxGalaxies.setSelectedItem(currentGalaxy);
		galaxyMap.setGalaxy(currentRegionData = new RegionData(currentGalaxy));
	}

	private void addComp(JPanel panel, GridBagLayout layout, GridBagConstraints c, Component comp, double weightx, double weighty, int gridwidth, int fill) {
		c.weightx=weightx;
		c.weighty=weighty;
		c.gridwidth=gridwidth;
		c.fill = fill;
		layout.setConstraints(comp, c);
		panel.add(comp);
	}

	private void showGlyphOverlay(int numberOfKnownGlyphs) {
		Worker worker = galaxyMap.showGlyphOverlay(numberOfKnownGlyphs);
		if (worker==null) return;
		
		Gui.runWithProgressDialog(mainWindow,"Create Glyph Overlay", pd->{
			worker.setProgressDialog(pd);
			worker.run();
		});
//			SwingUtilities.invokeLater(new Runnable() {
//				@Override public void run() {
//					ProgressDialog pd = new ProgressDialog(mainWindow,"Create Glyph Overlay");
//					worker.setProgressDialog(pd);
//					pd.addCancelListener(worker);
//					new Thread(worker).start();
//					boolean error;
//					int errorcount = 0;
//					do {
//						error = false;
//						try { pd.showDialog(); }
//						catch (Exception e) { error=true; Gui.log_error_ln("pd.showDialog() -> error["+(++errorcount)+"]"); }
//					} while(error);
//				}
//			});
	}
	
	private static abstract class Worker implements ProgressDialog.CancelListener, Runnable {
		boolean stopNow;
		private ProgressDialog pd;
		
		Worker() { stopNow = false; pd = null; }
		void setProgressDialog(ProgressDialog pd) { this.pd = pd; }
		
		protected void setProgress(int value         ) { if (pd!=null) Gui.runInEventThreadAndWait(()->pd.setValue(value    )); }
		protected void setProgress(int value, int max) { if (pd!=null) Gui.runInEventThreadAndWait(()->pd.setValue(value,max)); }
		protected void setTaskTitle(String title)      { if (pd!=null) Gui.runInEventThreadAndWait(()->pd.setTaskTitle(title)); }
		
		@Override public void cancelTask() { stopNow = true; if (pd!=null) Gui.runInEventThreadAndWait(()->pd.closeDialog()); }
		@Override public void run() { compute(); if (pd!=null) Gui.runInEventThreadAndWait(()->pd.closeDialog()); }
		protected abstract void compute();
	}

	private void showStatus(int mouseX, int mouseY) {
		String str = String.format(Locale.ENGLISH, "Zoom: %1.1f%%", galaxyMap.zoomRatio*100);
		if (mouseX>=0 && mouseY>=0) {
			int voxelX = galaxyMap.computeVoxelX(mouseX);
			int voxelZ = galaxyMap.computeVoxelZ(mouseY);
			UniverseAddress ua = new UniverseAddress(0,voxelX,0,voxelZ,0,0);
			str += String.format(Locale.ENGLISH, ",  Region: (%d,#,%d)", voxelX,voxelZ);
			str += String.format(Locale.ENGLISH, ",  GlyphCode: %s", ua.getPortalGlyphCodeStr());
			str += String.format(Locale.ENGLISH, ",  Distance to Center: %1.1f regions", ua.getDistToCenter_inRegionUnits() /*Math.sqrt(voxelX*voxelX+voxelZ*voxelZ)*/);
		}
		statusField.setText(str);
	}

	private class ContextMenu extends JPopupMenu {
		private static final long serialVersionUID = -5698577950332700493L;
		
		private final Component invoker;
		private RegionData.RegionCoord clickedPos;

		private final JMenuItem miMarkRegions;
		private final JMenuItem miDistCircle;
		private final JMenuItem miWriteMapToVRML;

		private final FileChooser vrmlFileChooser;

		private ContextMenu(Component invoker) {
			super("Contextmenu");
			this.invoker = invoker;
			this.clickedPos = null;
			
			miMarkRegions = new JMenuItem("Mark Regions in \"Known Universe\"");
			miMarkRegions.addActionListener(e->{
				if (clickedPos!=null)
					universePanel.highlightRegions(currentGalaxy.galaxyIndex,clickedPos.voxelX,clickedPos.voxelZ);
			});
			
			miDistCircle = new JMenuItem("Show Circle with Distance to Galaxy Center");
			miDistCircle.addActionListener(e->galaxyMap.showDistCircle(clickedPos));
			
			vrmlFileChooser = new FileChooser("VRML-File", "wrl");
			miWriteMapToVRML = new JMenuItem("Write Galaxy Map to VRML file");
			miWriteMapToVRML.addActionListener(e->{
				if (currentGalaxy==null) return;
				vrmlFileChooser.suggestFileName(String.format("S%s_Galaxy_%s", data.index>=0?(""+(data.index+1)):"#", currentGalaxy.getName()));
				if (vrmlFileChooser.showSaveDialog(this.invoker)!=FileChooser.APPROVE_OPTION) return;
				File vrmlFile = vrmlFileChooser.getSelectedFile();
				FileExport.writeGalaxyToVRML(vrmlFile, currentGalaxy, galaxyMap::getRegionColor, GalaxyMap.COLOR_BLACKHOLE_CONNECTION, mainWindow);
				FileExport.openFileInVrmlViewer(vrmlFile);
			});
			
			JCheckBoxMenuItem chkbxUsePreparedBitmap    = new JCheckBoxMenuItem("Use Prepared Bitmap", galaxyMap.usePreparedBitmap);
			JCheckBoxMenuItem chkbxShowMarkers          = new JCheckBoxMenuItem("Show markers", galaxyMap.showMarkers);
			JCheckBoxMenuItem chkbxShowBlackHoleTargets = new JCheckBoxMenuItem("Show Black Hole Targets", galaxyMap.showBlackHoleTargets);
			
			chkbxShowMarkers.setEnabled(!galaxyMap.usePreparedBitmap);
			
			chkbxUsePreparedBitmap.addActionListener( e->{
				boolean usePreparedBitmap = chkbxUsePreparedBitmap.isSelected();
				galaxyMap.usePreparedBitmap(usePreparedBitmap);
				chkbxShowMarkers.setEnabled(usePreparedBitmap);
				if (!usePreparedBitmap) chkbxShowMarkers.setSelected(true);
			});
			chkbxShowMarkers.addActionListener( e->{
				galaxyMap.showMarkers(chkbxShowMarkers.isSelected());
			});
			chkbxShowBlackHoleTargets.addActionListener( e->{
				galaxyMap.showBlackHoleTargets(chkbxShowBlackHoleTargets.isSelected());
			});
			
			add(miMarkRegions);
			add(miDistCircle);
			add(miWriteMapToVRML);
			addSeparator();
			add(chkbxUsePreparedBitmap);
			add(chkbxShowMarkers);
			add(chkbxShowBlackHoleTargets);
		}

		public void show(int screenX, int screenY, RegionData.RegionCoord clickedPos) {
			this.clickedPos = clickedPos;
			miMarkRegions.setText(String.format("Mark Regions (%d,#,%d) in \"Known Universe\"", clickedPos.voxelX, clickedPos.voxelZ));
			miDistCircle .setText(String.format("Show Circle with Distance to Galaxy Center for Region (%d,#,%d)", clickedPos.voxelX, clickedPos.voxelZ));
			miWriteMapToVRML.setEnabled(currentGalaxy!=null);
			if (currentGalaxy!=null) miWriteMapToVRML.setText(String.format("Write Map of %s to VRML File", currentGalaxy.toString()));
			else                     miWriteMapToVRML.setText(              "Write Galaxy Map to VRML file");
			show(invoker, screenX, screenY);
		}
	}
	
	private class CombinedListener implements MouseWheelListener, MouseMotionListener, MouseListener, AdjustmentListener {
		
		private boolean isScrollBarListeningEnabled;
		private ContextMenu contextMenu;
		
		CombinedListener() {
			isScrollBarListeningEnabled = true;
			contextMenu = null;
		}
		
		public void setContextMenu(ContextMenu contextMenu) {
			this.contextMenu = contextMenu;
		}

		@Override
		public void adjustmentValueChanged(AdjustmentEvent e) {
			if (!isScrollBarListeningEnabled) return;
			
			Adjustable adj = e.getAdjustable();
			switch (adj.getOrientation()) {
			case Adjustable.HORIZONTAL:
//					System.out.printf("scrollBarHoriz.adjustmentValueChanged: %d..%d..%d (%d) %s\r\n", adj.getMinimum(),adj.getValue(),adj.getMaximum(),adj.getVisibleAmount(),e.getValueIsAdjusting() );
				galaxyMap.setXOffset(adj.getValue());
				break;
			case Adjustable.VERTICAL:
//					System.out.printf("scrollBarVert .adjustmentValueChanged: %d..%d..%d (%d) %s\r\n", adj.getMinimum(),adj.getValue(),adj.getMaximum(),adj.getVisibleAmount(),e.getValueIsAdjusting() );
				galaxyMap.setYOffset(adj.getValue());
				break;
			}
		}

		public void setHorizScrollBar(int min, int value, int max, int visible) {
			isScrollBarListeningEnabled = false;
			scrollBarHoriz.setValues(value, visible, min, max);
			scrollBarHoriz.setBlockIncrement(Math.min((max-min)/10, visible));
			isScrollBarListeningEnabled = true;
		}
		
		public void setVertScrollBar(int min, int value, int max, int visible) {
			isScrollBarListeningEnabled = false;
			scrollBarVert.setValues(value, visible, min, max);
			scrollBarVert.setBlockIncrement(Math.min((max-min)/10, visible));
			isScrollBarListeningEnabled = true;
		}
		
		@Override public void mouseWheelMoved(MouseWheelEvent e) {
//				System.out.printf(Locale.ENGLISH, "mouseWheelMoved()%s%s %f %d\r\n", e.isShiftDown()?" shift":"", e.isControlDown()?" ctrl":"",e.getPreciseWheelRotation(), e.getUnitsToScroll());
			if (e.isControlDown()) {
				zoomField.setSelectedItem(null);
				galaxyMap.incZoom(e.getX(),e.getY(),-e.getWheelRotation());
			} else {
				int canvasSize = e.isShiftDown()?galaxyMap.getViewWidth():galaxyMap.getViewHeight();
				double scrollAmount = Math.max(canvasSize*0.05,1.0);
				int offsetInc = (int)Math.round(e.getPreciseWheelRotation()*scrollAmount);
				if (e.isShiftDown()) galaxyMap.incXOffset(offsetInc);
				else                 galaxyMap.incYOffset(offsetInc);
			}
			showStatus(e.getX(),e.getY());
		}

		@Override public void mouseEntered (MouseEvent e) { showStatus(e.getX(),e.getY()); galaxyMap.setMousePos(e.getX(),e.getY()); }
		@Override public void mouseMoved   (MouseEvent e) { showStatus(e.getX(),e.getY()); galaxyMap.setMousePos(e.getX(),e.getY()); }
		@Override public void mouseExited  (MouseEvent e) { showStatus(-1,-1); galaxyMap.clearMousePos(); }
		@Override public void mouseClicked (MouseEvent e) {
			if (e.getButton()==MouseEvent.BUTTON3 && contextMenu!=null) {
				int voxelX = galaxyMap.computeVoxelX(e.getX());
				int voxelZ = galaxyMap.computeVoxelZ(e.getY());
				contextMenu.show(e.getX(), e.getY(), new RegionData.RegionCoord(voxelX, voxelZ));
			}
		}
		
		@Override public void mousePressed (MouseEvent e) {}
		@Override public void mouseReleased(MouseEvent e) {}
		@Override public void mouseDragged (MouseEvent e) {}
	}
	
	private static class RegionData {
		
		private HashMap<RegionCoord, RegionState> regions;
		private Vector<BlackHoleConn> blackHoleConnections;
		private Galaxy galaxy;
		
		private RegionData(Galaxy galaxy) {
			this.galaxy = galaxy;
			regions = new HashMap<>();
			blackHoleConnections = new Vector<>();
			for (Region region:this.galaxy.regions) {
				RegionCoord rc = new RegionCoord(region.voxelX,region.voxelZ);
				RegionState regionState = regions.get(rc);
				if (regionState==null)
					regions.put( rc, new RegionState(region) );
				else
					regionState.update(region);
			}
			updateBlackHoleConnections();
		}

		private void updateBlackHoleConnections() {
			blackHoleConnections.clear();
			for (Region region:galaxy.regions)
				for (SolarSystem sys:region.solarSystems)
					if (sys.hasBlackHole && sys.blackHoleTarget!=null)
						blackHoleConnections.add(new BlackHoleConn(sys.getUniverseAddress(), sys.blackHoleTarget));
		}
		
		public void forEachRegion(BiConsumer<RegionCoord, RegionState> action) {
			regions.forEach(action);
		}
		
		public static class RegionState {
			boolean isReachableByTeleport;
			
			RegionState(Region region) {
				isReachableByTeleport = false;
				update(region);
			}

			private void update(Region region) {
				isReachableByTeleport = isReachableByTeleport || region.isReachableByTeleport();
			}
		}
		
		private static class RegionCoord {
			private int voxelX;
			private int voxelZ;
			public RegionCoord(int voxelX, int voxelZ) {
				this.voxelX = voxelX;
				this.voxelZ = voxelZ;
			}
			public RegionCoord(UniverseAddress ua) {
				this(ua.voxelX,ua.voxelZ);
			}
			@Override
			public int hashCode() {
				return (voxelX&0xFFFF)<<16 | (voxelZ&0xFFFF);
			}
			@Override
			public boolean equals(Object obj) {
				if (this == obj) return true;
				if (obj == null) return false;
				if (!(obj instanceof RegionCoord)) return false;
				RegionCoord other = (RegionCoord) obj;
				return (voxelX!=other.voxelX) || (voxelZ!=other.voxelZ);
			}
		}
		
		private static class BlackHoleConn {
			static int CIRCLE_SEGMENTS = 200;
			
			private final RegionCoord source;
			private final RegionCoord target;
			final SaveGameData.Point3D[] polygon;
			
			public BlackHoleConn(RegionCoord source, RegionCoord target) {
				this.source = source;
				this.target = target;
				this.polygon = generatePolygon();
			}
			public BlackHoleConn(UniverseAddress sourceUA, UniverseAddress targetUA) {
				this(new RegionCoord(sourceUA), new RegionCoord(targetUA));
			}
			
			private SaveGameData.Point3D[] generatePolygon() {
				double startRadius = Math.sqrt( source.voxelX*source.voxelX + source.voxelZ*source.voxelZ );
				double endRadius   = Math.sqrt( target.voxelX*target.voxelX + target.voxelZ*target.voxelZ );
				double startAngle_deg = Math.atan2(source.voxelZ, source.voxelX)/Math.PI*180;
				double endAngle_deg   = Math.atan2(target.voxelZ, target.voxelX)/Math.PI*180;
				boolean flipped = (endAngle_deg>startAngle_deg &&  endAngle_deg-startAngle_deg>180) || (startAngle_deg>endAngle_deg &&  startAngle_deg-endAngle_deg<180);
				
				while (startAngle_deg    >endAngle_deg) endAngle_deg += 360;
				while (startAngle_deg+360<endAngle_deg) endAngle_deg -= 360;
				
				double angleDelta_deg = endAngle_deg-startAngle_deg;
				if (flipped) angleDelta_deg -= 360;
				int nSeg = (int)Math.ceil(Math.abs(angleDelta_deg)/360*CIRCLE_SEGMENTS);
				double segAngle_deg = angleDelta_deg/nSeg;
				
				SaveGameData.Point3D[] polygons = new SaveGameData.Point3D[nSeg+1];
				for (int i=0; i<=nSeg; i++) {
					double radius = (startRadius*(nSeg-i) + endRadius*i)/nSeg;
					double x = radius * Math.cos( (i*segAngle_deg+startAngle_deg)/180*Math.PI );
					double z = radius * Math.sin( (i*segAngle_deg+startAngle_deg)/180*Math.PI );
					polygons[i] = new SaveGameData.Point3D(x,0,z);
				}
				return polygons;
			}
		}
	}
	
	private static class GalaxyMap extends Canvas {

		private static final long serialVersionUID = -6290765806544803046L;

		private static final Color COLOR_BACKGROUND = Color.BLACK;
		private static final Color COLOR_GRID  = new Color(0x202020);
		private static final Color COLOR_AXIS = new Color(0x000090);
		private static final Color COLOR_KNOWN_REGION_WITH_TELEPORT = Color.CYAN;
		private static final Color COLOR_KNOWN_REGION = Color.YELLOW;
		private static final Color COLOR_CURRENT_POS  = Color.MAGENTA;
		private static final Color COLOR_GALAXY_CENTER = Color.RED;
		private static final Color COLOR_BLACKHOLE_CONNECTION = Color.PINK;

		private static final int MAP_WIDTH  = 4096;
		private static final int MAP_HEIGHT = 4096;
		private static final int MAP_CENTER_X = 2047;
		private static final int MAP_CENTER_Y = 2047;
		private static final int MAP_GRID = 16;
		
		private static final int GRID_MIN = 5;
		private static final int GRID_STEP = 2;
		private static final float GRID_DIM_RANGE = 1.0f;
		
		private static final double ZOOM_INC = 1.1;
		
		private RegionData regionData;
		private BufferedImage mapBaseImage;
//			private BufferedImage mapImage;
		private BufferedImage overlayImage;

		private int offsetX;
		private int offsetY;
		private int scaledMapWidth;
		private int scaledMapHeight;
		private double zoomRatio;

		private CombinedListener combiListener;
		private UniverseAddress currentPos;
		private Long knownGlyphs;
		private boolean showMarkers;
		private boolean usePreparedBitmap;
		private boolean showBlackHoleTargets;

		private int mouseVoxelX;
		private int mouseVoxelZ;
		private boolean ignoreMousePos;

		private Double distCircle;

		
		GalaxyMap(CombinedListener combiListener, RegionData regionData, UniverseAddress currentPos, Long knownGlyphs) {
			this.combiListener = combiListener;
			this.regionData = regionData;
			//withDebugOutput = true;
			this.currentPos = currentPos;
			this.knownGlyphs = knownGlyphs;
			
			this.mapBaseImage = null;
			this.overlayImage = null;
//				this.mapImage = null;
			
			this.offsetX = 0;
			this.offsetY = 0;
			this.scaledMapWidth  = MAP_WIDTH;
			this.scaledMapHeight = MAP_HEIGHT;
			this.zoomRatio = 1.0;
			this.showMarkers = true;
			this.usePreparedBitmap = false;
			
			this.mouseVoxelX = 0;
			this.mouseVoxelZ = 0;
			this.ignoreMousePos = true;
			
			this.distCircle = null;
		}

		public Color getRegionColor(Region region) {
			if (region.getUniverseAddress().isSameRegion(currentPos)) return COLOR_CURRENT_POS;
			if (region.isReachableByTeleport()) return COLOR_KNOWN_REGION_WITH_TELEPORT;
			return COLOR_KNOWN_REGION;
		}

		public void showDistCircle( RegionData.RegionCoord coords) {
			if (coords!=null)
				distCircle = new SaveGameData.Point3D(coords.voxelX,0,coords.voxelZ).length();
			else
				distCircle = null;
			repaint();
		}

		public void setMousePos(int mouseX, int mouseY) {
			int newMouseVoxelX = computeVoxelX(mouseX);
			int newMouseVoxelZ = computeVoxelZ(mouseY);
			if (ignoreMousePos || mouseVoxelX!=newMouseVoxelX || mouseVoxelZ!=newMouseVoxelZ) {
				mouseVoxelX = newMouseVoxelX;
				mouseVoxelZ = newMouseVoxelZ;
				repaint();
			}
			ignoreMousePos = false;
		}

		public void clearMousePos() {
			ignoreMousePos = true;
			repaint();
		}

		public void usePreparedBitmap(boolean usePreparedBitmap) {
			this.usePreparedBitmap = usePreparedBitmap;
			repaint();
		}

		public void showMarkers(boolean showMarkers) {
			this.showMarkers = showMarkers;
			repaint();
		}

		public void showBlackHoleTargets(boolean showBlackHoleTargets) {
			this.showBlackHoleTargets = showBlackHoleTargets;
			repaint();
		}

		public void setGalaxy(RegionData regionData) {
			this.regionData = regionData;
			prepareMap();
			repaint();
		}

		public int computeVoxelX(int screenX) { return (int) Math.floor((screenX+offsetX)/zoomRatio)-MAP_CENTER_X; }
		public int computeVoxelZ(int screenY) { return (int) Math.floor((screenY+offsetY)/zoomRatio)-MAP_CENTER_Y; }

		public double computeScreenX_d(double voxelX) { return (voxelX+0.5+MAP_CENTER_X)*zoomRatio-offsetX; }
		public double computeScreenY_d(double voxelZ) { return (voxelZ+0.5+MAP_CENTER_Y)*zoomRatio-offsetY; }
		public int computeScreenX(double voxelX) { return (int) Math.round(computeScreenX_d(voxelX)); }
		public int computeScreenY(double voxelZ) { return (int) Math.round(computeScreenY_d(voxelZ)); }

		public int getViewWidth () { return width; }
		public int getViewHeight() { return height; }

		public Worker showGlyphOverlay(int numberOfKnownGlyphs) {
			if (numberOfKnownGlyphs==0 || (numberOfKnownGlyphs==17 && knownGlyphs==null)) {
				setOverlayImage(null);
				//mapImage = mapBaseImage;
				repaint();
//					System.out.printf(Locale.ENGLISH, "showGlyphOverlay( %d ) -> disable overlay\r\n", numberOfKnownGlyphs);
				return null;
			}
			
			return new Worker() {
				@Override protected void compute() {
					setTaskTitle("Create portal glyph overlay");
					
					BufferedImage newOverlayImage = createEmptyMap();
					Graphics graphics = newOverlayImage.getGraphics();
					//graphics.drawImage(mapBaseImage,0,0,null);
					
					if (numberOfKnownGlyphs<17) {
						// N consecutive glyphs
						int N = numberOfKnownGlyphs;
						setProgress(0,N*N);
						graphics.setColor(new Color(0x00,0xFF,0x00,0x7F));
						for (int x1=0; (x1<N) && !stopNow; ++x1)
							for (int y1=0; (y1<N) && !stopNow; ++y1) {
								setProgress(x1*N+y1);
								for (int x2=0; (x2<N) && !stopNow; ++x2)
									for (int y2=0; (y2<N) && !stopNow; ++y2)
										drawRect(graphics, x1*16+x2*256+MAP_CENTER_X, y1*16+y2*256+MAP_CENTER_Y, N, N);
							}
						setProgress(N*N);
					} else {
						int bitmask = (int)(long)knownGlyphs;
						// bit mask of known glyphs
						setProgress(0,MAP_WIDTH);
						graphics.setColor(new Color(0x00,0xFF,0x00,0x7F));
						for (int x=0; (x<MAP_WIDTH) && !stopNow; ++x) {
							setProgress(x);
							int cX = (x-MAP_CENTER_X)&0xFFF;
							if (((bitmask>>((cX>>8)&0xF))&1)==0) continue;
							if (((bitmask>>((cX>>4)&0xF))&1)==0) continue;
							if (((bitmask>>((cX>>0)&0xF))&1)==0) continue;
							for (int y=0; (y<MAP_HEIGHT) && !stopNow; ++y) {
								int cY = (y-MAP_CENTER_Y)&0xFFF;
								if (((bitmask>>((cY>>8)&0xF))&1)==0) continue;
								if (((bitmask>>((cY>>4)&0xF))&1)==0) continue;
								if (((bitmask>>((cY>>0)&0xF))&1)==0) continue;
								graphics.fillRect(x,y,1,1);
							}
						}
						setProgress(MAP_WIDTH);
					}
					
					if (!stopNow) {
						setOverlayImage(newOverlayImage);
						repaint();
//							System.out.printf(Locale.ENGLISH, "showGlyphOverlay( %d ) -> enable overlay\r\n", numberOfKnownGlyphs);
					}
				}

				private void drawRect(Graphics graphics, int x, int y, int width, int height) {
					while (x >= MAP_WIDTH ) x -= MAP_WIDTH;
					while (y >= MAP_HEIGHT) y -= MAP_HEIGHT;
					if (x+width > MAP_WIDTH) {
						drawRect(graphics, x, y, MAP_WIDTH-x, height);
						drawRect(graphics, 0, y, x+width-MAP_WIDTH, height);
					}
					if (y+height > MAP_HEIGHT) {
						drawRect(graphics, x, y, width, MAP_HEIGHT-y);
						drawRect(graphics, x, 0, width, y+height-MAP_HEIGHT);
					}
					graphics.fillRect(x,y,width,height);
				}
			};
		}

		public void prepareMap() {
			if (regionData==null) setMapBaseImage(null);
			else setMapBaseImage( prepareBaseMap() );
		}
		
		private synchronized void setOverlayImage(BufferedImage newOverlayImage) {
			overlayImage = newOverlayImage;
		}
		
		private synchronized void setMapBaseImage(BufferedImage newMapBaseImage) {
			mapBaseImage = newMapBaseImage;
		}

		private BufferedImage prepareBaseMap() {
			BufferedImage newMapBaseImage = createEmptyMap();
			Graphics graphics = newMapBaseImage.getGraphics();
			
			graphics.setColor(COLOR_BACKGROUND);
			graphics.fillRect(0, 0, MAP_WIDTH, MAP_HEIGHT);
			
			graphics.setColor(COLOR_GRID);
			for (int x=MAP_CENTER_X%MAP_GRID; x<MAP_WIDTH ; x+=MAP_GRID) graphics.drawLine(x, 0, x, MAP_HEIGHT);
			for (int y=MAP_CENTER_Y%MAP_GRID; y<MAP_HEIGHT; y+=MAP_GRID) graphics.drawLine(0, y, MAP_WIDTH, y);
			
			graphics.setColor(COLOR_AXIS);
			graphics.drawLine(MAP_CENTER_X, 0, MAP_CENTER_X, MAP_HEIGHT);
			graphics.drawLine(0, MAP_CENTER_Y, MAP_WIDTH, MAP_CENTER_Y);
			
			regionData.forEachRegion((rc,rs)->{
				graphics.setColor(rs.isReachableByTeleport?COLOR_KNOWN_REGION_WITH_TELEPORT:COLOR_KNOWN_REGION);
				graphics.fillRect(rc.voxelX+MAP_CENTER_X, rc.voxelZ+MAP_CENTER_Y, 1, 1);
			});
			if (regionData.galaxy.galaxyIndex == currentPos.galaxyIndex) {
				graphics.setColor(COLOR_CURRENT_POS);
				graphics.fillRect(currentPos.voxelX+MAP_CENTER_X, currentPos.voxelZ+MAP_CENTER_Y, 1, 1);
			}
			
			graphics.setColor(COLOR_GALAXY_CENTER);
			graphics.fillRect(MAP_CENTER_X, MAP_CENTER_Y, 1, 1);
			
			return newMapBaseImage;
		}

		private BufferedImage createEmptyMap() {
			return new BufferedImage(MAP_WIDTH, MAP_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		}

		public void setZoom(double value) {
			incZoom(width/2,height/2, value/zoomRatio);
//				System.out.printf(Locale.ENGLISH, "setZoom( %f ) -> offset:%d,%d zoom:%f\r\n", value, offsetX,offsetY,zoomRatio);
		}

		public void incZoom(int zoomCenterX, int zoomCenterY, int zoomInc) {
			incZoom(zoomCenterX, zoomCenterY, Math.pow(ZOOM_INC, zoomInc));
//				System.out.printf(Locale.ENGLISH, "incZoom( %d,%d,%d, ) -> offset:%d,%d zoom:%f\r\n", zoomCenterX,zoomCenterY,zoomInc, offsetX,offsetY,zoomRatio);
		}

		private void incZoom(int zoomCenterX, int zoomCenterY, double zoomRatioInc) {
			if (this.zoomRatio<0.1 && zoomRatioInc<1) return;
			
			this.zoomRatio *= zoomRatioInc;
			if ((1/ZOOM_INC+1)/2<zoomRatio && zoomRatio<(ZOOM_INC+1)/2) zoomRatio=1.0; // normalize zoomRatio to 1 if it's near 1
			
			offsetX = (int) (Math.round((offsetX+zoomCenterX)*zoomRatioInc)-zoomCenterX);
			offsetY = (int) (Math.round((offsetY+zoomCenterY)*zoomRatioInc)-zoomCenterY);
			scaledMapWidth  = (int)Math.round(MAP_WIDTH*zoomRatio);
			scaledMapHeight = (int)Math.round(MAP_HEIGHT*zoomRatio);
			
			offsetsChanged();
			combiListener.setHorizScrollBar(0,offsetX,scaledMapWidth ,width );
			combiListener.setVertScrollBar (0,offsetY,scaledMapHeight,height);
		}

		public void incYOffset(int offsetYInc) {
			setYOffset(offsetY+offsetYInc);
		}
		
		public void setYOffset(int offsetY) {
			this.offsetY = offsetY;
			offsetsChanged();
			combiListener.setVertScrollBar (0,this.offsetY,scaledMapHeight,height);
//				System.out.printf(Locale.ENGLISH, "incYOffset( %d ) -> offsetY:%d\r\n", offsetYInc, offsetY);
		}

		public void incXOffset(int offsetXInc) {
			setXOffset(offsetX+offsetXInc);
		}

		public void setXOffset(int offsetX) {
			this.offsetX = offsetX;
			offsetsChanged();
			combiListener.setHorizScrollBar(0,this.offsetX,scaledMapWidth ,width );
//				System.out.printf(Locale.ENGLISH, "incXOffset( %d ) -> offsetX:%d\r\n", offsetXInc, offsetX);
		}
		
		private void offsetsChanged() {
			offsetX = Math.min(offsetX,scaledMapWidth-width);
			offsetX = Math.max(offsetX,0);
			offsetY = Math.min(offsetY,scaledMapHeight-height);
			offsetY = Math.max(offsetY,0);
			repaint();
		}
		
		@Override
		protected void paintCanvas(Graphics g, int x__, int y__, int width, int height) {
//				if (isMapImageNull()) return;
			if (!(g instanceof Graphics2D)) return;
			Graphics2D g2 = (Graphics2D)g;
			
			int maxX = Math.min(scaledMapWidth-offsetX,width);
			int maxY = Math.min(scaledMapHeight-offsetY,height);
			
			AffineTransform transform = new AffineTransform();
//				transform.setToScale(zoomRatio, zoomRatio);
//				transform.translate(-offsetX,-offsetY);
			transform.setToTranslation(-offsetX,-offsetY);
			transform.scale(zoomRatio, zoomRatio);
			
			int type = zoomRatio<1.0?AffineTransformOp.TYPE_BICUBIC:AffineTransformOp.TYPE_NEAREST_NEIGHBOR;
			AffineTransformOp op = new AffineTransformOp(transform, type);
			
			if (usePreparedBitmap) {
				g2.setClip(new Rectangle(0, 0, maxX, maxY));
				synchronized(this) {
					if (mapBaseImage!=null)
						g2.drawImage(mapBaseImage, op, 0, 0);
				}
			} else {
				
				g2.setPaint(COLOR_BACKGROUND);
				g2.fillRect(0, 0, maxX, maxY);
				
				double axisX_d = computeScreenX_d(0);
				double axisY_d = computeScreenY_d(0);
				
				double smallGridV = MAP_GRID*zoomRatio;
				while (smallGridV<GRID_MIN) {
					smallGridV *= GRID_STEP;
				}
				
				int minIX = (int)Math.ceil((-axisX_d)/smallGridV);
				int minIY = (int)Math.ceil((-axisY_d)/smallGridV);
				int maxIX = (int)Math.floor((maxX-axisX_d)/smallGridV);
				int maxIY = (int)Math.floor((maxY-axisY_d)/smallGridV);
				
				if (zoomRatio>1) {
					int thickness = (int)Math.round(zoomRatio);
					
					g2.setColor(COLOR_GRID);
					double x = axisX_d+minIX*smallGridV-zoomRatio/2;
					for (int i=minIX; i<=maxIX; ++i, x+=smallGridV)
						if (i!=0) fillRect(g2,maxX,maxY, (int)Math.round(x),0, thickness,maxY);

					double y = axisY_d+minIY*smallGridV-zoomRatio/2;
					for (int i=minIY; i<=maxIY; ++i, y+=smallGridV)
						if (i!=0) fillRect(g2,maxX,maxY, 0,(int)Math.round(y), maxX,thickness);
					
					g2.setColor(COLOR_AXIS);
					int axisX = (int)Math.round(axisX_d-zoomRatio/2);
					int axisY = (int)Math.round(axisY_d-zoomRatio/2);
					fillRect(g2,maxX,maxY, axisX,0, thickness,maxY);
					fillRect(g2,maxX,maxY, 0,axisY, maxX,thickness);
					
					g2.setColor(COLOR_KNOWN_REGION);
					regionData.forEachRegion((rc,rs)->{
						if (regionData.galaxy.galaxyIndex==currentPos.galaxyIndex && rc.voxelX==currentPos.voxelX && rc.voxelZ==currentPos.voxelZ) return;
						if (rs.isReachableByTeleport) return;
						fillBox(g2, maxX, maxY, rc.voxelX, rc.voxelZ);
					});
					
					g2.setColor(COLOR_KNOWN_REGION_WITH_TELEPORT);
					regionData.forEachRegion((rc,rs)->{
						if (regionData.galaxy.galaxyIndex==currentPos.galaxyIndex && rc.voxelX==currentPos.voxelX && rc.voxelZ==currentPos.voxelZ) return;
						if (!rs.isReachableByTeleport) return;
						fillBox(g2, maxX, maxY, rc.voxelX, rc.voxelZ);
					});
					
					if (regionData.galaxy.galaxyIndex==currentPos.galaxyIndex) {
						g2.setColor(COLOR_CURRENT_POS);
						fillBox(g2, maxX, maxY, currentPos.voxelX, currentPos.voxelZ);
					}
					
					g2.setColor(COLOR_GALAXY_CENTER);
					fillBox(g2, maxX,maxY,0,0);
				} else { 
//						System.out.printf(Locale.ENGLISH,"%d..%d, %d..%d",minIX,maxIX,minIY,maxIY);
//						System.out.printf(Locale.ENGLISH,"-> %1.1f..%1.1f, %1.1f..%1.1f\r\n",minIX*smallGridV+axisX_d,maxIX*smallGridV+axisX_d,minIY*smallGridV+axisY_d,maxIY*smallGridV+axisY_d);
					
					float f = Math.min(1.0f,(float)(smallGridV-GRID_MIN)/(GRID_STEP*GRID_MIN-GRID_MIN)/GRID_DIM_RANGE);
					Color smallGridColor = new Color(
							COLOR_GRID.getRed  ()*f/255,
							COLOR_GRID.getGreen()*f/255,
							COLOR_GRID.getBlue ()*f/255);
					double x = axisX_d+minIX*smallGridV;
					for (int i=minIX; i<=maxIX; ++i, x+=smallGridV)
						if (i!=0) {
							if (i%GRID_STEP == 0) g2.setColor(COLOR_GRID);
							else g2.setColor(smallGridColor);
							int x_ = (int)Math.round(x);
							g2.drawLine(x_, 0, x_, maxY-1);
						}

					double y = axisY_d+minIY*smallGridV;
					for (int i=minIY; i<=maxIY; ++i, y+=smallGridV)
						if (i!=0) {
							if (i%GRID_STEP == 0) g2.setColor(COLOR_GRID);
							else g2.setColor(smallGridColor);
							int y_ = (int)Math.round(y);
							g2.drawLine(0, y_, maxX-1, y_);
						}
					
//						for (int x=MAP_CENTER_X%MAP_GRID; x<MAP_WIDTH ; x+=MAP_GRID) graphics.drawLine(x, 0, x, MAP_HEIGHT);
//						for (int y=MAP_CENTER_Y%MAP_GRID; y<MAP_HEIGHT; y+=MAP_GRID) graphics.drawLine(0, y, MAP_WIDTH, y);
					
					g2.setColor(COLOR_AXIS);
					int axisX = (int)Math.round(axisX_d);
					int axisY = (int)Math.round(axisY_d);
					if (0<=axisX && axisX< width) g2.drawLine(axisX, 0, axisX, maxY-1);
					if (0<=axisY && axisY<height) g2.drawLine(0, axisY, maxX-1, axisY);
				}
			}
			
			synchronized(this) {
				if (overlayImage!=null) {
					g2.setClip(new Rectangle(0, 0, maxX, maxY));
					g2.drawImage(overlayImage, op, 0, 0);
				}
			}
			
			if (showMarkers || !usePreparedBitmap) {
				g2.setClip(new Rectangle(0, 0, maxX, maxY));
				
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				int markerSize = 5;
				
				g2.setColor(COLOR_KNOWN_REGION);
				regionData.forEachRegion((rc,rs)->{
					if (regionData.galaxy.galaxyIndex==currentPos.galaxyIndex && rc.voxelX==currentPos.voxelX && rc.voxelZ==currentPos.voxelZ) return;
					if (rs.isReachableByTeleport) return;
					drawMarker(g2, rc.voxelX, rc.voxelZ, markerSize);
				});
				
				g2.setColor(COLOR_KNOWN_REGION_WITH_TELEPORT);
				regionData.forEachRegion((rc,rs)->{
					if (regionData.galaxy.galaxyIndex==currentPos.galaxyIndex && rc.voxelX==currentPos.voxelX && rc.voxelZ==currentPos.voxelZ) return;
					if (!rs.isReachableByTeleport) return;
					drawMarker(g2, rc.voxelX, rc.voxelZ, markerSize);
				});
				
				if (regionData.galaxy.galaxyIndex==currentPos.galaxyIndex) {
					g2.setColor(COLOR_CURRENT_POS);
					drawMarker(g2, currentPos.voxelX, currentPos.voxelZ, markerSize);
				}
				
				g2.setColor(COLOR_GALAXY_CENTER);
				drawMarker(g2, 0,0, markerSize);
				
			}
			
			if (showBlackHoleTargets) {
				g2.setColor(COLOR_BLACKHOLE_CONNECTION);
				int[] xPoints = new int[40];
				int[] yPoints = new int[40];
				for (RegionData.BlackHoleConn bhc:regionData.blackHoleConnections) {
					Point3D[] polygon = bhc.polygon;
					if (xPoints.length<polygon.length) xPoints = new int[polygon.length+10];
					if (yPoints.length<polygon.length) yPoints = new int[polygon.length+10];
					for (int i = 0; i < polygon.length; i++) {
						Point3D p = polygon[i];
						xPoints[i] = computeScreenX(p.x);
						yPoints[i] = computeScreenY(p.z);
					}
					g2.drawPolyline(xPoints, yPoints, polygon.length);
				}
			}
			
			if (distCircle != null) {
				g2.setColor(Color.WHITE);
				int x  = computeScreenX(-distCircle);
				int y  = computeScreenY(-distCircle);
				int x1 = computeScreenX( distCircle);
				int y1 = computeScreenY( distCircle);
				g2.drawArc( x,y, x1-x,y1-y, 0,360);
			}
			
			if (zoomRatio>5 && !ignoreMousePos) {
				int x = (int)Math.round(computeScreenX_d(mouseVoxelX)-zoomRatio/2);
				int y = (int)Math.round(computeScreenY_d(mouseVoxelZ)-zoomRatio/2);
				int thickness = (int)Math.round(zoomRatio);
				if (0<=x && x+thickness<maxX && 0<=y && y+thickness<maxY) {
					g2.setColor(Color.WHITE);
					g2.setXORMode(Color.BLACK);
					g2.drawRect(x, y, thickness, thickness);
					g2.setPaintMode();
				}
			}
		}

		private void fillRect(Graphics2D g2, int maxX, int maxY, int x, int y, int w, int h) {
			int x2 = x+w;
			int y2 = y+h;
			if (maxY<=y || y2<0) return;
			if (maxX<=x || x2<0) return;
			
			x = Math.max(0,x);
			x2 = Math.min(x2,maxX);
			y = Math.max(0,y);
			y2 = Math.min(y2,maxY);
			w = x2-x;
			h = y2-y;
			
			g2.fillRect(x, y, w, h);
		}

		private void fillBox(Graphics2D g2, int maxX, int maxY, int voxelX, int voxelZ) {
			int x = (int)Math.round(computeScreenX_d(voxelX)-zoomRatio/2);
			int y = (int)Math.round(computeScreenY_d(voxelZ)-zoomRatio/2);
			int thickness = (int)Math.round(zoomRatio);
			fillRect(g2,maxX,maxY, x,y,thickness,thickness);
		}

		private void drawMarker(Graphics2D g2, int voxelX, int voxelZ, int size) {
			int x = computeScreenX(voxelX);
			int y = computeScreenY(voxelZ);
			if (0<=x && x<this.width && 0<=y && y<this.height) {
				g2.drawLine(x-size,y-size,x+size,y+size);
				g2.drawLine(x+size,y-size,x-size,y+size);
			}
		}

		@Override
		protected void sizeChanged(int width, int height) {
			offsetsChanged();
			combiListener.setHorizScrollBar(0,offsetX,scaledMapWidth ,width );
			combiListener.setVertScrollBar (0,offsetY,scaledMapHeight,height);
		}
	}
}