package net.schwarzbaer.java.games.nomanssky.saveviewer;

public class Debug {
	public static void Assert(boolean predicate                ) { if (!predicate) throw new IllegalStateException("Debug.Assertion failed"); }
	public static void Assert(boolean predicate, String message) { if (!predicate) throw new IllegalStateException("Debug.Assertion failed: "+message); }
}