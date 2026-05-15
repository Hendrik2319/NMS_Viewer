package net.schwarzbaer.java.games.nomanssky.saveviewer.views;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData;
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
	private SaveGameData[] saveGames;

	public GlobalDeObfuscatorUsagePanel()
	{
		super(new BorderLayout(3, 3));
		setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		saveGames = null;
		
		replacementsTableModel = new ReplacementsTableModel();
		replacementsTable = new SimplifiedTable<>("ReplacementsTable",replacementsTableModel,true,SaveViewer.DEBUG,true);
		replacementsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION, true);
		
		usageTableModel = new UsageTableModel();
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

	public synchronized void updateData(SaveGameData[] saveGames)
	{
		this.saveGames = saveGames;
		replacementsTableModel.updateData();
		usageTableModel.fireTableUpdate();
	}
	
	private synchronized SaveGameData getSaveGame(int saveGameIndex)
	{
		if (saveGames==null || saveGameIndex<0 || saveGameIndex>=saveGames.length)
			return null;
		return saveGames[saveGameIndex];
	}

	private synchronized void forEachSaveGame(Consumer<SaveGameData> action)
	{
		if (saveGames!=null)
			for (SaveGameData data : saveGames)
				if (data!=null)
					action.accept(data);
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
		
		Vector<String> rows;
		
		ReplacementsTableModel()
		{
			super(new ColumnID[] {
					new ColumnID("Original"   , String.class,  60, originalName -> originalName, null),
					new ColumnID("Replacement", String.class, 180, SaveViewer.deObfuscator::getReplacement, null)
			});
			rows = new Vector<>();
			setData(rows);
		}

		void updateData()
		{
			Set<String> originalNames = new HashSet<>();
			forEachSaveGame(data -> originalNames.addAll(data.deObfuscatorUsage.keySet()));
			
			List<String> sorted = new ArrayList<>(originalNames);
			sorted.sort(null);
			
			insertNewValues(sorted);
		}

		private void insertNewValues(List<String> newValues)
		{
			int newIndex = 0;
			boolean getNextNewValue = true;
			String newValue = null;
			
			for (int oldIndex=0; oldIndex<rows.size() && newIndex<newValues.size(); oldIndex++)
			{
				if (getNextNewValue)
					newValue = newValues.get(newIndex);
				getNextNewValue = false;
				
				String oldValue = rows.get(oldIndex);
				int cmp = oldValue.compareTo(newValue);
				
				if (cmp > 0)
				{
					rows.insertElementAt(newValue, oldIndex);
					fireTableRowAdded(oldIndex);
				}
				
				if (cmp >= 0)
				{
					newIndex++;
					getNextNewValue = true;
				}
			}
			
			if (newIndex<newValues.size())
			{
				int firstRowIndex = rows.size();
				for (int i=newIndex; i<newValues.size(); i++) rows.add(newValues.get(i));
				int lastRowIndex  = rows.size()-1;
				
				fireTableRowsAdded(firstRowIndex,lastRowIndex);
			}
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
			ColumnID(int index)
			{
				super(SaveViewer.getFilename(index), Boolean.class, 62, null, (model,path) -> model.isPathUsedBy(index,path));
			}
		}
		
		private String originalName;

		UsageTableModel()
		{
			super(createColumns());
			setOriginalName(null);
		}

		private static ColumnID[] createColumns()
		{
			ColumnID[] columnIDs = new ColumnID[SaveViewer.SaveGameListPanel.SAVE_GAME_COUNT*2+1];
			columnIDs[0] = new ColumnID();
			for (int index=0; index<SaveViewer.SaveGameListPanel.SAVE_GAME_COUNT*2; index++)
				columnIDs[index+1] = new ColumnID(index);
			return columnIDs;
		}

		@Override protected UsageTableModel getThis() { return this; }

		void setOriginalName(String originalName)
		{
			this.originalName = originalName;
			Set<String> allPaths = new HashSet<>();
			if (this.originalName!=null)
				forEachSaveGame(data -> {
					HashSet<String> paths = data.deObfuscatorUsage.get(this.originalName);
					if (paths!=null)
						allPaths.addAll(paths);
				});
			
			List<String> sorted = new ArrayList<>(allPaths);
			sorted.sort(null);
			setData(sorted);
		}

		private boolean isPathUsedBy(int saveGameIndex, String path)
		{
			SaveGameData data = getSaveGame(saveGameIndex);
			HashSet<String> paths = originalName==null || data==null ? null : data.deObfuscatorUsage.get(originalName);
			return paths!=null && paths.contains(path);
		}
	}
}
