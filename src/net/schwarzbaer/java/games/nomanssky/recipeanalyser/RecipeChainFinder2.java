package net.schwarzbaer.java.games.nomanssky.recipeanalyser;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

class RecipeChainFinder2<IDType extends Comparable<IDType>> {
	
	private final DataModel<IDType> dataModel;
	private final DataModel<IDType>.Ingredient finalOutput;
	private final HashMap<IDType, HashSet<DataModel<IDType>.InputValueCombination>> allProdRecipe;
	private final HashMap<IDType, HashSet<DataModel<IDType>.InputValueCombination>> neededRecipes;
	private final HashMap<IDType, Integer> ingredientOrder;

	public RecipeChainFinder2(DataModel<IDType> dataModel, DataModel<IDType>.Ingredient finalOutput, HashMap<IDType, HashSet<DataModel<IDType>.InputValueCombination>> allProdRecipe) {
		this.dataModel = dataModel;
		this.finalOutput = finalOutput;
		this.allProdRecipe = allProdRecipe;
		this.neededRecipes = new HashMap<>();
		this.ingredientOrder = new HashMap<>();
	}
	
	public RecipeChainFinder2<IDType> search() {
		neededRecipes.clear();
		ingredientOrder.clear();
		addAllRecipesFor(finalOutput);
		setOrder(finalOutput,0);
		return this;
	}
	
	private void setOrder(DataModel<IDType>.Ingredient ingredient, int newOrderIndex) {
		if (ingredient==null) return;
		IDType id = ingredient.getID();
		Integer oldOrderIndex = ingredientOrder.get(id);
		if (oldOrderIndex==null || oldOrderIndex>newOrderIndex) {
			ingredientOrder.put(id,newOrderIndex);
			HashSet<DataModel<IDType>.InputValueCombination> recipes = neededRecipes.get(id);
			if (recipes!=null)
				for (DataModel<IDType>.InputValueCombination r:recipes)
					r.forEach(ingID->setOrder(dataModel.getIngredient(ingID),newOrderIndex+1));
		}
	}

	private void addAllRecipesFor(DataModel<IDType>.Ingredient ingredient) {
		if (ingredient==null) return;
		IDType id = ingredient.getID();
		if (neededRecipes.containsKey(id)) return;
		if (dataModel.isInStock(id)) return;
		
		HashSet<DataModel<IDType>.InputValueCombination> recipes = allProdRecipe.get(id);
		if (recipes!=null) {
			neededRecipes.put(id, recipes);
			for (DataModel<IDType>.InputValueCombination r:recipes)
				r.forEach(ingID->addAllRecipesFor(dataModel.getIngredient(ingID)));
		}
	}

	public void printTree(PrintStream out) {
		out.printf("Possible Recipe Chains for \"%s\"%n", dataModel.getIngredientName(finalOutput.getID()));
		
		Vector<IDType> sortedIDs = new Vector<>(neededRecipes.keySet());
		sortedIDs.sort(Comparator.<IDType,Integer>comparing(ingredientOrder::get,Comparator.nullsLast(Comparator.naturalOrder())));
		
		String indent;
		HashSet<DataModel<IDType>.InputValueCombination> recipes;
		for (IDType id:sortedIDs) {
			
			Integer orderIndex = ingredientOrder.get(id);
			recipes = neededRecipes.get(id);
			
			out.printf("    [%s] %s", orderIndex, dataModel.getIngredientName(id));
			if (recipes.size()==1) { indent = " "; }
			else { out.println(); indent = "          "; }
			
			Vector<DataModel<IDType>.InputValueCombination> sortedRecipes = new Vector<>(recipes);
			sortedRecipes.sort(null);
			for (DataModel<IDType>.InputValueCombination r:sortedRecipes)
				out.printf("%s<-- %s%n", indent, r.toString());
		}
		out.printf("<end>%n");
	}
}