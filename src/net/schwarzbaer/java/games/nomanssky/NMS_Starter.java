package net.schwarzbaer.java.games.nomanssky;

import net.schwarzbaer.java.games.nomanssky.recipeanalyser.RecipeAnalyser;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Gui;
import net.schwarzbaer.java.games.nomanssky.saveviewer.ProductionOptimiser;
import net.schwarzbaer.java.games.nomanssky.saveviewer.ResourceHotSpots;
import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveViewer;
import net.schwarzbaer.java.games.nomanssky.saveviewer.UpgradeModuleInstallHelper;

public class NMS_Starter {

	public static void main(String[] args) {
		if (args.length>0)
			switch (args[0].toLowerCase()) {
			case "upgrademoduleinstallhelper": UpgradeModuleInstallHelper.main(new String[]{}); return;
			case "productionoptimiser"       : ProductionOptimiser       .main(new String[]{}); return;
			case "recipeanalyser"            : RecipeAnalyser            .main(new String[]{}); return;
			case "resourcehotspots"          : ResourceHotSpots          .main(new String[]{}); return;
			case "coordinates"               : Gui.CoordinatesDialog.showStandAlone(); return;
			}
		else {
			Gui.log_ln("No Mans Sky - Tools");
			Gui.log_ln("   by Hendrik Scholtz");
			Gui.log_ln("");
			Gui.log_ln("usage:");
			Gui.log_ln("   java -jar <jar-file> [Tool/Function]");
			Gui.log_ln("");
			Gui.log_ln("[Tool]");
			Gui.log_ln("      UpgradeModuleInstallHelper   starts UpgradeModule Install Helper");
			Gui.log_ln("      ProductionOptimiser          starts Production Optimiser");
			Gui.log_ln("      RecipeAnalyser               starts Recipe Analyser");
			Gui.log_ln("      ResourceHotSpots             starts Resource HotSpots tool");
			Gui.log_ln("      Coordinates                  starts Coordinates Dialog");
			Gui.log_ln("");
			Gui.log_ln("or without parameter starts SaveViewer");
			SaveViewer.writeSaveViewerFunctionsInUsage();
			Gui.log_ln("");
			Gui.log_ln("");
		}
		SaveViewer.main(args);
	}

}
