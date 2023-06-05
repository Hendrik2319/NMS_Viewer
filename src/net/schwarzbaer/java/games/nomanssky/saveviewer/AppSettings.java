package net.schwarzbaer.java.games.nomanssky.saveviewer;

import net.schwarzbaer.java.lib.system.Settings;

public final class AppSettings extends Settings.DefaultAppSettings<AppSettings.ValueGroup, AppSettings.ValueKey> {
	
	private final static AppSettings instance = new AppSettings();
	
	public enum ValueKey {
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
