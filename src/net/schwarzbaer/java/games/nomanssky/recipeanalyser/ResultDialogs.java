package net.schwarzbaer.java.games.nomanssky.recipeanalyser;

import java.awt.Window;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import net.schwarzbaer.java.games.nomanssky.saveviewer.Gui;
import net.schwarzbaer.java.games.nomanssky.saveviewer.Gui.TextAreaDialog;

class ResultDialogs {
	
	private final Window mainwindow;
	private final Vector<TextAreaDialog> openDialogs;
	private JMenu menu;
	private boolean ignoreCloseEvents;
	
	ResultDialogs(Window mainwindow) {
		this.mainwindow = mainwindow;
		menu = null;
		openDialogs = new Vector<>();
		ignoreCloseEvents = false;
	}

	JMenu createMenu(String title) {
		menu = new JMenu(title);
		fillMenu();
		return menu;
	}

	private void fillMenu() {
		if (menu==null) return;
		menu.removeAll();
		menu.add(Gui.createMenuItem("Close All", e->closeAllDialogs(), !openDialogs.isEmpty()));
		if (!openDialogs.isEmpty()) {
			menu.addSeparator();
			for (TextAreaDialog dlg : openDialogs)
				menu.add(Gui.createMenuItem(dlg.getTitle(), e->dlg.requestFocus()));
		}
	}

	void closeAllDialogs() {
		ignoreCloseEvents = true;
		for (TextAreaDialog dlg : openDialogs)
			dlg.closeDialog();
		ignoreCloseEvents = false;
		openDialogs.clear();
		fillMenu();
	}

	private void dialogClosed(TextAreaDialog dlg) {
		if (ignoreCloseEvents) return;
		openDialogs.remove(dlg);
		fillMenu();
	}

	void showStream(String title, Consumer<PrintStream> print) {
		TextAreaDialog resultDialog = new TextAreaDialog(mainwindow, title, this::dialogClosed);
		resultDialog.setText_Stream(print);
		showDialog(resultDialog);
	}

	void showWriter(String title, Consumer<PrintWriter> print) {
		TextAreaDialog resultDialog = new TextAreaDialog(mainwindow, title, this::dialogClosed);
		resultDialog.setText_Writer(print);
		showDialog(resultDialog);
	}

	void showStreams(String title, Vector<Consumer<PrintStream>> prints) {
		TextAreaDialog resultDialog = TextAreaDialog.createForStreamVariants(mainwindow, title, this::dialogClosed, prints);
		showDialog(resultDialog);
	}

	void showWriters(String title, Vector<Consumer<PrintWriter>> prints) {
		TextAreaDialog resultDialog = TextAreaDialog.createForWriterVariants(mainwindow, title, this::dialogClosed, prints);
		showDialog(resultDialog);
	}

	<IDType extends Comparable<IDType>> void showStream(DataModel<IDType> dataModel, String title, Consumer<PrintStream> print) {
		TextAreaDialog resultDialog = new ResultDialogs.ResultDialog<>(dataModel, mainwindow, title, this::dialogClosed);
		resultDialog.setText_Stream(print);
		showDialog(resultDialog);
	}

	<IDType extends Comparable<IDType>> void showWriter(DataModel<IDType> dataModel, String title, Consumer<PrintWriter> print) {
		TextAreaDialog resultDialog = new ResultDialogs.ResultDialog<>(dataModel, mainwindow, title, this::dialogClosed);
		resultDialog.setText_Writer(print);
		showDialog(resultDialog);
	}

	<IDType extends Comparable<IDType>> void showStreams(DataModel<IDType> dataModel, String title, Vector<Consumer<PrintStream>> prints) {
		TextAreaDialog resultDialog = ResultDialog.createResultDialogForStreamVariants(dataModel, mainwindow, title, this::dialogClosed, prints);
		showDialog(resultDialog);
	}

	<IDType extends Comparable<IDType>> void showWriters(DataModel<IDType> dataModel, String title, Vector<Consumer<PrintWriter>> prints) {
		TextAreaDialog resultDialog = ResultDialog.createResultDialogForWriterVariants(dataModel, mainwindow, title, this::dialogClosed, prints);
		showDialog(resultDialog);
	}

	private void showDialog(TextAreaDialog resultDialog) {
		resultDialog.showDialog();
		openDialogs.add(resultDialog);
		openDialogs.sort(Comparator.nullsLast(Comparator.comparing(TextAreaDialog::getTitle)));
		fillMenu();
	}
	
	private static class ResultDialog<IDType extends Comparable<IDType>> extends Gui.TextAreaDialog {
		private static final long serialVersionUID = 5466523258727339825L;
		public static <IDType extends Comparable<IDType>> ResultDialogs.ResultDialog<IDType> createResultDialogForWriterVariants(
				DataModel<IDType> dataModel,
				Window parent, String title,
				Consumer<TextAreaDialog> closeListener,
				Vector<Consumer<PrintWriter>> variants
		) {
			Constructor<PrintWriter, ResultDialogs.ResultDialog<IDType>> constructor =
				(parent1, title1, closeListener1, variants1, setText) ->
					new ResultDialogs.ResultDialog<IDType>(dataModel, parent1, title1, closeListener1, variants1, setText);
			return createForWriterVariants(constructor, parent, title, closeListener, variants);
		}

		public static <IDType extends Comparable<IDType>> ResultDialogs.ResultDialog<IDType> createResultDialogForStreamVariants(
				DataModel<IDType> dataModel,
				Window parent, String title,
				Consumer<TextAreaDialog> closeListener,
				Vector<Consumer<PrintStream>> variants
		) {
			Constructor<PrintStream, ResultDialogs.ResultDialog<IDType>> constructor =
				(parent1, title1, closeListener1, variants1, setText) -> 
					new ResultDialogs.ResultDialog<IDType>(dataModel, parent1, title1, closeListener1, variants1, setText);
			return createForStreamVariants(constructor, parent, title, closeListener, variants);
		}

		private DataModel<IDType>.Ingredient selectedIngredient;

		private ResultDialog(DataModel<IDType> dataModel, Window parent, String title, Consumer<TextAreaDialog> closeListener) {
			this(dataModel, parent, title, closeListener, null, null);
		}

		private <Output> ResultDialog(
				DataModel<IDType> dataModel,
				Window parent, String title,
				Consumer<TextAreaDialog> closeListener,
				Vector<Consumer<Output>> variants,
				BiConsumer<TextAreaDialog, Consumer<Output>> setText
		) {
			super(parent, title, closeListener, variants, setText);
			if (dataModel==null) throw new IllegalArgumentException();
			
			selectedIngredient = null;
			
			JMenuItem miFindRecipeChain, miFindRecipesWith;
			JPopupMenu contextMenu = new JPopupMenu();
			contextMenu.add(miFindRecipeChain = Gui.createMenuItem("Find recipe chain for ####"     , e->{
				SwingUtilities.invokeLater(()->{
					if (selectedIngredient!=null)
						dataModel.serviceFunctions.findRecipeChains(selectedIngredient);
				});
			}));
			contextMenu.add(miFindRecipesWith = Gui.createMenuItem("Find recipes with #### as input", e->{
				SwingUtilities.invokeLater(()->{
					if (selectedIngredient!=null)
						dataModel.serviceFunctions.findRecipesWith(selectedIngredient);
				});
			}));
			
			Gui.ContextMenuInvoker menuInvoker = new Gui.ContextMenuInvoker(outputTextArea, contextMenu);
			menuInvoker.addContextMenuInvokeListener((x, y) -> {
				String text = outputTextArea.getSelectedText();
				selectedIngredient = dataModel.findIngredientByName(text);
				
				miFindRecipeChain.setEnabled(selectedIngredient!=null);
				miFindRecipesWith.setEnabled(selectedIngredient!=null);
				miFindRecipeChain.setText(selectedIngredient==null
						? "Find recipe chain"
						: String.format("Find recipe chain for \"%s\"", selectedIngredient.getName()));
				miFindRecipesWith.setText(selectedIngredient==null
						? "Find recipes with specified input"
						: String.format("Find recipes with \"%s\" as input", selectedIngredient.getName()));
			});
		}
	}
}