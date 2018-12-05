package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Locale;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Duration;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Position;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Galaxy;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Region;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.UniverseAddress;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.SaveGameView.SaveGameViewTabPanel;

class GeneralDataPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = -3866983525686776846L;
		
		private JTextArea[] textAreas;
		private int currentTextArea;
	
		public GeneralDataPanel(SaveGameData data) {
			super(data);
			
			JPanel textareaPanel = new JPanel(new GridLayout(1,0,3,3));
			textAreas = new JTextArea[2];
			textAreas[0] = createTextarea(textareaPanel,600,500);
			textAreas[1] = createTextarea(textareaPanel,600,500);
			currentTextArea = 0;
			
			add(textareaPanel,BorderLayout.CENTER);
			//add(buttonPanel,BorderLayout.SOUTH);
			
			updateContent();
		}

		private JTextArea createTextarea(JPanel textareaPanel, int width, int height) {
			JTextArea textArea = new JTextArea();
			textArea.setEditable(false);
			JScrollPane scrollPane = new JScrollPane(textArea);
			scrollPane.setPreferredSize(new Dimension(width, height));
			textareaPanel.add(scrollPane);
			return textArea;
		}

		@Override
		public void updateContent() {
			for (JTextArea t:textAreas) t.setText("");
			
			currentTextArea = 0;
			appendValueT("Current Units      ", data.general.units );
			appendValueT("Current Nanites    ", data.general.nanites );
			appendValueT("Current Quicksilver", data.general.quicksilver );
			appendValue ("Player Health      ", data.general.playerHealth );
			appendValue ("Player Shield      ", data.general.playerShield );
			appendValue ("Energy             ", data.general.energy );
			appendValue ("Ship Health        ", data.general.shipHealth );
			appendValue ("Ship Shield        ", data.general.shipShield );
			appendValue ("Time Alive         ", Duration.toString(data.general.timeAlive) );
			appendValue ("Total PlayTime     ", Duration.toString(data.general.totalPlayTime) );
			appendValue ("Hazard Time Alive  ", Duration.toString(data.general.hazardTimeAlive) );
			
			currentTextArea = 1;
			UniverseAddress currentUA = data.general.currentUniverseAddress;
			if (currentUA!=null) {
				if (!textAreaIsEmpty()) appendEmptyLine();
				appendLine("Current Location in Universe:");
				showLocationInUniverse(currentUA,"    ");
				appendLine(String.format(Locale.ENGLISH, "    distance to center of galaxy: %1.1f regions", currentUA.getDistToCenter_inRegionUnits()));
			}
			
			showUAddressAndPosition(data.general.freighterUA, data.general.freighterPos,"Position of Freighter");
			showUAddressAndPosition(data.general.graveUA, data.general.gravePos,"Position of Grave");
			
			currentTextArea = 0;
			Long knownGlyphs = data.general.knownGlyphsMask;
			if (knownGlyphs!=null) {
				if (!textAreaIsEmpty()) appendEmptyLine();
				appendLine("Known Portal Glyphs:");
				String str = "";
				int n = (int)(long)knownGlyphs;
				for (int i=0; i<16; ++i) {
					if ((n&1) > 0) {
						if (!str.isEmpty()) str+=", ";
						str+=i;
					}
					n = n>>1;
				}
				appendLine("   "+str);
			}
			
			if (data.discoveryData!=null) {
				if (!textAreaIsEmpty()) appendEmptyLine();
				appendLine("Discovered Items:");
				appendValue("   available"          , data.discoveryData.availableData.size() );
				appendValue("      on planets"      , data.discoveryData.availDiscoveredItemOnPlanets );
				appendValue("      in solar systems", data.discoveryData.availDiscoveredItemInSolarSystms );
				appendValue("   stored   "          , data.discoveryData.storeData.size() );
				appendValue("      on planets"      , data.discoveryData.storedDiscoveredItemOnPlanets );
				appendValue("      in solar systems", data.discoveryData.storedDiscoveredItemInSolarSystms );
			}
			
			if (currentUA!=null && data.universe!=null) {
				Galaxy galaxy = data.universe.findGalaxy(currentUA);
				if (galaxy!=null) {
					double min = Double.POSITIVE_INFINITY; Vector<Region> minRegions = new Vector<>();
					double max = Double.NEGATIVE_INFINITY; Vector<Region> maxRegions = new Vector<>();
					for (Region r:galaxy.regions) {
						if (!r.isReachableByTeleport()) continue;
						
						if (min>r.distToCenter) {
							minRegions.clear();
							minRegions.add(r);
							min = r.distToCenter;
						} else if (min==r.distToCenter)
							minRegions.add(r);
						
						if (max<r.distToCenter) {
							maxRegions.clear();
							maxRegions.add(r);
							max = r.distToCenter;
						} else if (max==r.distToCenter)
							maxRegions.add(r);
					}
					
					if (!maxRegions.isEmpty() && !minRegions.isEmpty()) {
						if (!textAreaIsEmpty()) appendEmptyLine();
						appendLine("Way to Galactic Center:    ( = Teleport Range )");
						appendLine(String.format(Locale.ENGLISH, "   max: %1.1f ly  %s", max*400, maxRegions.toString()));
						appendLine(String.format(Locale.ENGLISH, "   min: %1.1f ly  %s", min*400, minRegions.toString()));
						appendLine(String.format(Locale.ENGLISH, "        = %1.1f%% of max", min/max*100));
						appendLine(String.format(Locale.ENGLISH, "-> Way to Galactic Center: %1.1f%% passed", (1-min/max)*100));
					}
				}
			}
		}

		private void showUAddressAndPosition(UniverseAddress address, Position position, String title) {
			if (address!=null || position!=null) {
				if (!textAreaIsEmpty()) appendEmptyLine();
				appendLine(title+":");
				if (address!=null) {
					appendLine("    location in universe:");
					showLocationInUniverse(address, "        ");
				}
				if (position!=null) {
					appendLine("    position in system:");
					if (position.pos!=null) appendLine("        pos: "+position.pos.toString("%1.2f"));
					if (position.at !=null) appendLine("        at:  "+position.at .toString("%1.4f"));
					if (position.up !=null) appendLine("        up:  "+position.up .toString("%1.4f"));
					if (position.pos==null && position.at==null && position.up==null)
						appendLine("        <no data>");
				}
			}
		}

		private void showLocationInUniverse(UniverseAddress address, String indent) {
			for (String str:address.getVerboseName(data.universe)) {
				appendLine(indent+str);
			}
			appendLine(indent+address.getCoordinates());
			appendLine(indent+address.getExtendedSigBoostCode());
		}
		
		private void appendLine(String line) {
			if (!textAreaIsEmpty())
				textAreas[currentTextArea].append("\r\n");
			textAreas[currentTextArea].append(line);
		}

		private boolean textAreaIsEmpty() {
			return textAreas[currentTextArea].getText().isEmpty();
		}

		private void appendEmptyLine() {
			textAreas[currentTextArea].append("\r\n");
		}

		private void appendValue (String label, int     value) { appendStatement(label, ""+value); }
		private void appendValueT(String label, Long    value) { appendStatement(label, value==null?"":String.format(Locale.ENGLISH, "%,d", value)); }
		private void appendValue (String label, Long    value) { appendStatement(label, value==null?"":(""+value)); }
		private void appendValue (String label, String  value) { appendStatement(label, value==null?"":(   value)); }

		private void appendStatement(String label, String statement) {
			appendLine(label+": "+statement);
		}
	}