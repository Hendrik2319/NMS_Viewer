package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Locale;

import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.UniverseAddress;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveViewer;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.Planet;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.Universe.SolarSystem;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.SaveGameView.SaveGameViewTabPanel;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.BoolValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.FloatValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.IntegerValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.PathIsNotSolvableException;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.StringValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value.Type;

class GeneralDataPanel extends SaveGameViewTabPanel {
		private static final long serialVersionUID = -3866983525686776846L;
		
		private JTextArea textArea;
	
		public GeneralDataPanel(SaveGameData data) {
			super(data);
			
			textArea = new JTextArea();
			textArea.setEditable(false);
			
			JScrollPane treeScrollPane = new JScrollPane(textArea);
			treeScrollPane.setPreferredSize(new Dimension(600, 500));
			
			JPanel buttonPanel = new JPanel();
			buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
			buttonPanel.add(SaveViewer.createButton("Set name for current position",e -> setNameForUniverseAddress(data.general.getCurrentUniverseAddress())));
			
			add(treeScrollPane,BorderLayout.CENTER);
			add(buttonPanel,BorderLayout.SOUTH);
			
			updateContent();
		}
		
		private void setNameForUniverseAddress(UniverseAddress ua) {
			if (ua==null) {
				JOptionPane.showMessageDialog(this, "Current location couldn't be identified.");
				return;
			}
			
			if (ua.isPlanet     ()) setNameForUniverseAddress(ua, data.universe.getOrCreatePlanet     (ua), "planet"      );
			if (ua.isSolarSystem()) setNameForUniverseAddress(ua, data.universe.getOrCreateSolarSystem(ua), "solar system");
			
			updateContent();
		}

		@Override
		public void updateContent() {
			textArea.setText("");
			appendValue("Current Units    ", data.general.getUnits() );
			appendValue("Player Health    ", data.general.getPlayerHealth() );
			appendValue("Player Shield    ", data.general.getPlayerShield() );
			appendValue("Ship Health      ", data.general.getShipHealth() );
			appendValue("Ship Shield      ", data.general.getShipShield() );
			appendValue("Time Alive       ", data.general.getTimeAlive() );
			appendValue("Total PlayTime   ", data.general.getTotalPlayTime() );
			appendValue("Hazard Time Alive", data.general.getHazardTimeAlive() );
			
			appendEmptyLine();
			UniverseAddress currentUA = data.general.getCurrentUniverseAddress();
			if (currentUA!=null) {
				appendLine("Current Location in Universe:");
				if (currentUA.isPlanet     ()) {
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
				appendLine(String.format(Locale.ENGLISH, "    Distance to Center of Galaxy: %1.1f regions", currentUA.getDistToCenter_inRegionUnits()));
			}
			
			Long knownGlyphs = data.general.getKnownGlyphsMaks();
			if (knownGlyphs!=null) {
				appendEmptyLine();
				appendLine("Known portal glyphs:");
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
			
			appendEmptyLine();
			appendLine("Discovered Items:");
			appendValue("   available"          , (long)data.discoveryData.availableData.size() );
			appendValue("      on planets"      , (long)data.discoveryData.availDiscoveredItemOnPlanets );
			appendValue("      in solar systems", (long)data.discoveryData.availDiscoveredItemInSolarSystms );
			appendValue("   stored   "          , (long)data.discoveryData.storeData.size() );
			appendValue("      on planets"      , (long)data.discoveryData.storedDiscoveredItemOnPlanets );
			appendValue("      in solar systems", (long)data.discoveryData.storedDiscoveredItemInSolarSystms );
			
			
			
//			if (SaveViewer.DEBUG) {
//				appendEmptyLine();
//				appendValue("Test value 1 (Bool)"   , data.general.getTestBool   ("PlayerStateData","Stats",7,"Stats",4,"Value","Denominator") );
//				appendValue("Test value 2 (Integer)", data.general.getTestInteger("PlayerStateData","Stats",7,"Stats",4,"Value","Denominator") );
//				appendValue("Test value 3 (Float)"  , data.general.getTestFloat  ("PlayerStateData","Stats",7,"Stats",4,"Value","Denominator") );
//				appendValue("Test value 4 (String)" , data.general.getTestString ("PlayerStateData","Stats",7,"Stats",4,"Value","Denominator") );
//				appendValue("Test value 5 (Float)"  , Type.Float  , "PlayerStateData","Stats",7,"Stats",4,"Value","Denominator");
//				appendValue("Test value 6 (Integer)", Type.Integer, "PlayerStateData","Stats",7,"Stats",4,"Value","Denominator");
//			}
		}
		
		private void appendEmptyLine() {
			textArea.append("\r\n");
		}

		private void appendStatement(String label, String statement) {
			String line = label+": "+statement;
			appendLine(line);
		}

		private void appendLine(String line) {
			if (!textArea.getText().isEmpty())
				textArea.append("\r\n");
			textArea.append(line);
		}

		private void showError(String label) {
			switch (data.error) {
			case NoError:
				appendStatement(label,"???");
				System.out.println("Value \""+label+"\" is <null>, but error is unknown: \""+data.errorMessage+"\"");
				break;
			case PathIsNotSolvable:
				appendStatement(label,"Value not found");
				System.out.println("Value \""+label+"\" not found: "+data.errorMessage);
				break;
			case UnexpectedType:
				appendStatement(label,"Value has unexpected type");
				System.out.println("Value \""+label+"\" has unexpected type: "+data.errorMessage);
				break;
			case ValueIsNull:
				appendStatement(label,"<null>");
				System.out.println("Value \""+label+"\" is <null>: "+data.errorMessage);
				break;
			}
		}

		@SuppressWarnings("unused")
		private void appendValue(String label, Boolean value) { if (value==null) showError(label); else appendStatement(label, ""+value); }
		private void appendValue(String label, Long    value) { if (value==null) showError(label); else appendStatement(label, ""+value); }
		@SuppressWarnings("unused")
		private void appendValue(String label, Double  value) { if (value==null) showError(label); else appendStatement(label, ""+value); }
		@SuppressWarnings("unused")
		private void appendValue(String label, String  value) { if (value==null) showError(label); else appendStatement(label,    value); }
		
		@SuppressWarnings("unused")
		private void appendValue(String label, Type expectedType, Object... path) {
			Value value = null;
			try {
				value = JSON_Data.getSubNode(data.json_data,path);
				switch(expectedType) {
				case Bool   : if (appendBoolValue   (label,value)) return; break;
				case Float  : if (appendFloatValue  (label,value)) return; break;
				case Integer: if (appendIntegerValue(label,value)) return; break;
				case String : if (appendStringValue (label,value)) return; break;
				default: break;
				}
			} catch (PathIsNotSolvableException e) {
				appendStatement(label,"Value not found");
				System.out.println("Value \""+label+"\" not found: "+e.getMessage());
				return;
			}
			appendStatement(label,"Value has unexpected type");
			System.out.println("Value \""+label+"\" has unexpected type: "+(value==null?"<null>":value.getClass()));
		}

		private boolean appendBoolValue(String label, Value value) {
			if (value instanceof BoolValue) {
				BoolValue valueB = (BoolValue)value;
				appendStatement(label, ""+valueB.value);
				return true;
			}
			return false;
		}

		private boolean appendIntegerValue(String label, Value value) {
			if (value instanceof IntegerValue) {
				IntegerValue valueI = (IntegerValue)value;
				appendStatement(label, ""+valueI.value);
				return true;
			}
			return false;
		}

		private boolean appendFloatValue(String label, Value value) {
			if (value instanceof FloatValue) {
				FloatValue valueF = (FloatValue)value;
				appendStatement(label, ""+valueF.value);
				return true;
			}
			return false;
		}

		private boolean appendStringValue(String label, Value value) {
			if (value instanceof StringValue) {
				StringValue valueS = (StringValue)value;
				appendStatement(label, valueS.value);
				return true;
			}
			return false;
		}
	}