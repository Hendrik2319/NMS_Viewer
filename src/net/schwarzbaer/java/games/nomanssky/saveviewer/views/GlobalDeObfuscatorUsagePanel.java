package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveViewer;
import net.schwarzbaer.java.games.nomanssky.saveviewer.views.TableView.SimplifiedTable;
import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.lib.gui.Tables.SimplifiedColumnConfig;

public class GlobalDeObfuscatorUsagePanel extends JPanel
{
	private static final long serialVersionUID = -5638035761158059068L;
	private final ReplacementsTableModel replacementsTableModel;
	private final UsageTableModel        usageTableModel;
	private final SimplifiedTable<ReplacementsTableModel.ColumnID> replacementsTable;
	private final SimplifiedTable<UsageTableModel       .ColumnID> usageTable;

	public GlobalDeObfuscatorUsagePanel(Vector<SaveGameView> loadedSaveGames)
	{
		super(new BorderLayout(3, 3));
		setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		
		replacementsTableModel = new ReplacementsTableModel(loadedSaveGames);
		replacementsTable = new SimplifiedTable<>("ReplacementsTable",replacementsTableModel,true,SaveViewer.DEBUG,true);
		replacementsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION, true);
		
		usageTableModel = new UsageTableModel(loadedSaveGames);
		usageTable = new SimplifiedTable<>("UsageTable",usageTableModel,true,SaveViewer.DEBUG,true);
		
		replacementsTable.getSelectionModel().addListSelectionListener(e->{
			int selectedRowV = replacementsTable.getSelectedRow();
			String originalName = selectedRowV<0 ? null : replacementsTableModel.getRow(replacementsTable.convertRowIndexToModel(selectedRowV));
			usageTableModel.setOriginalName(originalName);
		});
		
		JScrollPane replacementsTableScrollPane = new JScrollPane(replacementsTable);
		replacementsTableScrollPane.setPreferredSize(new Dimension(270, 100));
		
		add(replacementsTableScrollPane, BorderLayout.WEST);
		add(new JScrollPane(usageTable), BorderLayout.CENTER);
	}

	public void updateSaveGameList()
	{
		replacementsTableModel.updateData();
		usageTableModel.updateColumns();
	}
	
	private static class ColumnID<TableModelType> implements Tables.SimpleGetValueTableModel2.ColumnIDTypeInt2<TableModelType, String>
	{
		private final SimplifiedColumnConfig cfg;
		private final Function<String, ?> getValueFcn;
		private final BiFunction<TableModelType, String, ?> getValueMFcn;
		
		<V> ColumnID(String name, Class<V> columnClass, int width, Function<String, V> getValueFcn, BiFunction<TableModelType, String, V> getValueMFcn)
		{
			this.cfg = new SimplifiedColumnConfig(name, columnClass, 20, -1, width, width);
			this.getValueFcn = getValueFcn;
			this.getValueMFcn = getValueMFcn;
		}
	
		@Override public SimplifiedColumnConfig getColumnConfig() { return cfg; }
		@Override public Function<String, ?> getGetValue() { return getValueFcn; }
		@Override public BiFunction<TableModelType, String, ?> getGetValueM() { return getValueMFcn; }
	}
	
	private class ReplacementsTableModel extends Tables.SimpleGetValueTableModel2<ReplacementsTableModel, String, ReplacementsTableModel.ColumnID>
	{
		private static class ColumnID extends GlobalDeObfuscatorUsagePanel.ColumnID<ReplacementsTableModel>
		{
			<V> ColumnID(String name, Class<V> columnClass, int width, Function<String, V> getValueFcn, BiFunction<ReplacementsTableModel, String, V> getValueMFcn)
			{
				super(name, columnClass, width, getValueFcn, getValueMFcn);
			}
		}
		
		private final Vector<SaveGameView> loadedSaveGames;

		ReplacementsTableModel(Vector<SaveGameView> loadedSaveGames)
		{
			super(new ColumnID[] {
					new ColumnID("Original"   , String.class,  60, originalName -> originalName, null),
					new ColumnID("Replacement", String.class, 180, SaveViewer.deObfuscator::getReplacement, null)
			});
			this.loadedSaveGames = loadedSaveGames;
			updateData();
		}

		void updateData()
		{
			Set<String> originalNames = new HashSet<>();
			for (SaveGameView view : loadedSaveGames)
				originalNames.addAll(view.data.deObfuscatorUsage.keySet());
			
			List<String> sorted = new ArrayList<>(originalNames);
			sorted.sort(null);
			setData(sorted);
		}

		@Override protected ReplacementsTableModel getThis() { return this; }
	}
	
	private class UsageTableModel extends Tables.SimpleGetValueTableModel2<UsageTableModel, String, UsageTableModel.ColumnID>
	{
		private static class ColumnID extends GlobalDeObfuscatorUsagePanel.ColumnID<UsageTableModel>
		{
			ColumnID()
			{
				super("Path", String.class, 500, path -> path, null);
			}
			ColumnID(SaveGameView saveGameView)
			{
				super(saveGameView.file.getName(), Boolean.class, 62, null, (model,path) -> model.isPathUsedBy(saveGameView,path));
			}
		}
		
		private static final ColumnID DEFAULT_COLUMN__PATH = new ColumnID();
		private final Vector<SaveGameView> loadedSaveGames;
		private String originalName;

		UsageTableModel(Vector<SaveGameView> loadedSaveGames)
		{
			super(new ColumnID[] { DEFAULT_COLUMN__PATH });
			this.loadedSaveGames = loadedSaveGames;
			setOriginalName(null);
		}

		@Override protected UsageTableModel getThis() { return this; }

		void setOriginalName(String originalName)
		{
			this.originalName = originalName;
			Set<String> allPaths = new HashSet<>();
			if (this.originalName!=null)
				for (SaveGameView view : loadedSaveGames)
				{
					HashSet<String> paths = view.data.deObfuscatorUsage.get(this.originalName);
					if (paths!=null)
						allPaths.addAll(paths);
				}
			
			List<String> sorted = new ArrayList<>(allPaths);
			sorted.sort(null);
			setData(sorted);
		}

		void updateColumns()
		{
			List<ColumnID> columns = new ArrayList<>();
			columns.add(DEFAULT_COLUMN__PATH);
			loadedSaveGames.forEach(view -> columns.add(new ColumnID(view)));
			this.columns = columns.toArray(ColumnID[]::new);
			fireTableStructureUpdate();
			setColumnWidths(table);
		}

		private boolean isPathUsedBy(SaveGameView view, String path)
		{
			HashSet<String> paths = originalName==null ? null : view.data.deObfuscatorUsage.get(originalName);
			return paths!=null && paths.contains(path);
		}
	}
}
