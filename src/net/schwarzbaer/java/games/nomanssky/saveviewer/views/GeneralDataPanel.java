package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Locale;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Duration;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Position;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Planet;
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
			appendValueT("Current Units    ", data.general.units );
			appendValueT("Current Nanites  ", data.general.nanites );
			appendValue ("Player Health    ", data.general.playerHealth );
			appendValue ("Player Shield    ", data.general.playerShield );
			appendValue ("Energy           ", data.general.energy );
			appendValue ("Ship Health      ", data.general.shipHealth );
			appendValue ("Ship Shield      ", data.general.shipShield );
			appendValue ("Time Alive       ", Duration.toString(data.general.timeAlive) );
			appendValue ("Total PlayTime   ", Duration.toString(data.general.totalPlayTime) );
			appendValue ("Hazard Time Alive", Duration.toString(data.general.hazardTimeAlive) );
			
			currentTextArea = 1;
			UniverseAddress currentUA = data.general.currentUniverseAddress;
			if (currentUA!=null) {
				appendEmptyLine();
				appendLine("Current Location in Universe:");
				if (currentUA.isPlanet()) {
					Planet planet = data.universe.findPlanet(currentUA);
					if (planet!=null)
						appendLine("    on planet \""+planet+"\"");
					else
						appendLine("    on a planet");
				}
				if (currentUA.isSolarSystem()) {
					SolarSystem system = data.universe.findSolarSystem(currentUA);
					if (system!=null)
						appendLine("    in solar system \""+system+"\"");
					else
						appendLine("    in a solar system");
				}
				appendLine("    "+currentUA.getCoordinates());
				appendLine("    "+currentUA.getExtendedSigBoostCode());
				appendLine(String.format(Locale.ENGLISH, "    distance to center of galaxy: %1.1f regions", currentUA.getDistToCenter_inRegionUnits()));
			}
			
			showUAddressAndPosition(data.general.freighterUA, data.general.freighterPos,"Position of Freighter");
			showUAddressAndPosition(data.general.graveUA, data.general.gravePos,"Position of Grave");
			
			currentTextArea = 0;
			Long knownGlyphs = data.general.knownGlyphsMask;
			if (knownGlyphs!=null) {
				appendEmptyLine();
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
				appendEmptyLine();
				appendLine("Discovered Items:");
				appendValue("   available"          , data.discoveryData.availableData.size() );
				appendValue("      on planets"      , data.discoveryData.availDiscoveredItemOnPlanets );
				appendValue("      in solar systems", data.discoveryData.availDiscoveredItemInSolarSystms );
				appendValue("   stored   "          , data.discoveryData.storeData.size() );
				appendValue("      on planets"      , data.discoveryData.storedDiscoveredItemOnPlanets );
				appendValue("      in solar systems", data.discoveryData.storedDiscoveredItemInSolarSystms );
			}
		}

		private void showUAddressAndPosition(UniverseAddress address, Position position, String title) {
			if (address!=null || position!=null) {
				appendEmptyLine();
				appendLine(title+":");
				if (address!=null) {
					appendLine("    location in universe:");
					for (String str:address.getVerboseName(data.universe)) appendLine("        "+str);
					appendLine("        "+address.getCoordinates());
					appendLine("        "+address.getExtendedSigBoostCode());
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
		
		private void appendLine(String line) {
			if (!textAreas[currentTextArea].getText().isEmpty())
				textAreas[currentTextArea].append("\r\n");
			textAreas[currentTextArea].append(line);
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