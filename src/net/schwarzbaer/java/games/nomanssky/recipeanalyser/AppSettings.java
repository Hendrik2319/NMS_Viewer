package net.schwarzbaer.java.games.nomanssky.recipeanalyser;

import net.schwarzbaer.java.lib.system.Settings;

class AppSettings extends Settings.DefaultAppSettings<AppSettings.ValueGroup, AppSettings.ValueKey> {
	
	private final static AppSettings instance = new AppSettings();
	
	public enum ValueKey {
		Language,
		OpenDataFile,
		IngredientsInStock_NutrientProcessor,
		IngredientsInStock_Refiner
	}

	enum ValueGroup implements Settings.GroupKeys<ValueKey> {
		;
		ValueKey[] keys;
		ValueGroup(ValueKey...keys) { this.keys = keys;}
		@Override public ValueKey[] getKeys() { return keys; }
	}
	
	public AppSettings() { super(AppSettings.class, ValueKey.values()); }

	public static AppSettings getInstance() {
		return instance;
	}
}
