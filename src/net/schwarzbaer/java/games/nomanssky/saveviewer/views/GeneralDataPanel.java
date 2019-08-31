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
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.SolarSystem;
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
			appendValueO("Current Units      ", data.general.units );
			appendValue ("Current Nanites    ", 10, data.general.nanites );
			appendValue ("Current Quicksilver", 10, data.general.quicksilver );
			appendValue ("Player Health      ", 10, data.general.playerHealth );
			appendValue ("Player Shield      ", 10, data.general.playerShield );
			appendValue ("Energy             ", 10, data.general.energy );
			appendValue ("Ship Health        ", 10, data.general.shipHealth );
			appendValue ("Ship Shield        ", 10, data.general.shipShield );
			appendValue ("Time Alive         ", 10, Duration.toString(data.general.timeAlive) );
			appendValue ("Total PlayTime     ", 10, Duration.toString(data.general.totalPlayTime) );
			appendValue ("Hazard Time Alive  ", 10, Duration.toString(data.general.hazardTimeAlive) );
			
			currentTextArea = 1;
			UniverseAddress currentUA = data.general.currentUniverseAddress;
			if (currentUA!=null) {
				if (!textAreaIsEmpty()) appendEmptyLine();
				appendLine("Current Location in Universe:");
				showLocationInUniverse(currentUA,"    ");
				appendLine(String.format(Locale.ENGLISH, "    distance to center of galaxy: %1.1f regions", currentUA.getDistToCenter_inRegionUnits()));
			}
			
			showUAddressAndPosition(data.general.freighterUA, data.general.freighterPos,"Position of Freighter");
			showUAddressAndPosition(data.general.anomalyUA, data.general.anomalyPos,"Position of Anomaly");
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
					double avgBlackHoleDist2C = 0, blackHoleDist2C;
					int numberOfBlackHoles = 0;
					for (Region r:galaxy.regions) {
						for (SolarSystem sys:r.solarSystems) {
							if (sys.hasBlackHole && sys.blackHoleTarget!=null) {
								Region bhtRegion = data.universe.findRegion(sys.blackHoleTarget);
								blackHoleDist2C = r.distToCenter_RU-bhtRegion.distToCenter_RU;
								if (numberOfBlackHoles==0) avgBlackHoleDist2C = blackHoleDist2C;
								else avgBlackHoleDist2C = avgBlackHoleDist2C*numberOfBlackHoles + blackHoleDist2C;
								avgBlackHoleDist2C /= (++numberOfBlackHoles);
							}
						}
						if (!r.isReachableByTeleport()) continue;
						
						if (min>r.distToCenter_RU) {
							minRegions.clear();
							minRegions.add(r);
							min = r.distToCenter_RU;
						} else if (min==r.distToCenter_RU)
							minRegions.add(r);
						
						if (max<r.distToCenter_RU) {
							maxRegions.clear();
							maxRegions.add(r);
							max = r.distToCenter_RU;
						} else if (max==r.distToCenter_RU)
							maxRegions.add(r);
					}
					
					if (!textAreaIsEmpty()) appendEmptyLine();
					appendLine(Locale.ENGLISH, "Average Black Hole Jump Distance:  (%d black hole jumps)", numberOfBlackHoles);
					appendLine(Locale.ENGLISH, "   %1.1f ly", avgBlackHoleDist2C*400);
					
					if (!maxRegions.isEmpty() && !minRegions.isEmpty()) {
						if (!textAreaIsEmpty()) appendEmptyLine();
						appendLine("Way to Galactic Center:    ( = Teleport Range )");
						appendLine(Locale.ENGLISH, "   max: %1.1f ly  %s", max*400, maxRegions.toString());
						appendLine(Locale.ENGLISH, "   min: %1.1f ly  %s", min*400, minRegions.toString());
						appendLine("   passed distance:");
						appendLine(Locale.ENGLISH, "      %1.1f ly  (%1.1f%%)", (max-min)*400, (1-min/max)*100);
						appendLine("   remaining distance:");
						appendLine(Locale.ENGLISH, "      %1.1f ly  (%1.1f%%)", min*400, min/max*100);
						appendLine(Locale.ENGLISH, "      %1.1f black hole jumps", min/avgBlackHoleDist2C);
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
		
		private void appendLine(Locale locale, String format, Object... values) {
			appendLine(String.format(locale, format, values));
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

		private void appendValue (String label, int  value) { appendStatement(label, ""+value); }
		private void appendValueO(String label, Long value) { appendStatement(label, value==null?"":String.format(Locale.ENGLISH, "%,d%s", value, value>=0?"":String.format(Locale.ENGLISH, " (%,d)", (1L<<32)+value))); }
		private void appendValue (String label, int length, Long   value) { appendStatement(label, value==null?"":String.format(Locale.ENGLISH, "%,"+length+"d", value)); }
		private void appendValue (String label, int length, String value) { appendStatement(label, value==null?"":String.format(Locale.ENGLISH, "%" +length+"s", value)); }

		private void appendStatement(String label, String statement) {
			appendLine(label+": "+statement);
		}
	}