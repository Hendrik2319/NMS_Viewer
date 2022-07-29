package net.schwarzbaer.java.games.nomanssky.recipeanalyser;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

class RecipeChainFinder<IDType extends Comparable<IDType>> {
	
	private final DataModel<IDType> dataModel;
	private DataModel<IDType>.Ingredient finalOutput;
	private HashMap<IDType, HashSet<DataModel<IDType>.InputValueCombination>> allProdRecipe;
	private RecipeOutput baseRecipeOutput;

	public RecipeChainFinder(DataModel<IDType> dataModel, DataModel<IDType>.Ingredient finalOutput, HashMap<IDType, HashSet<DataModel<IDType>.InputValueCombination>> allProdRecipe) {
		this.dataModel = dataModel;
		this.finalOutput = finalOutput;
		this.allProdRecipe = allProdRecipe;
	}

	public void search() {
		baseRecipeOutput = new RecipeOutput(null,this.finalOutput);
	}
	
	public void printTree(PrintStream out) {
		out.printf("Possible Recipe Chains for \"%s\"%n", dataModel.getIngredientName(finalOutput.getID()));
		baseRecipeOutput.printTree(out,"      ","      ");
		out.printf("<end>%n");
	}

	private class RecipeOutput {

		private final AllowedRecipe parentRecipe;
		private final DataModel<IDType>.Ingredient output;
		private final IDType outputID;
		private final Vector<AllowedRecipe> allowedRecipes;
		private final boolean isBaseInput;

		public RecipeOutput(AllowedRecipe parentRecipe, DataModel<IDType>.Ingredient output) {
			this.parentRecipe = parentRecipe;
			this.output = output;
			this.outputID = output.getID();
			this.allowedRecipes = new Vector<>();
			HashSet<DataModel<IDType>.InputValueCombination> allowed = allProdRecipe.get( outputID );
			this.isBaseInput = dataModel.isInStock(outputID);
			if (!isBaseInput && !allowed.isEmpty()) {
				for (DataModel<IDType>.InputValueCombination recipe:allowed) {
					if (!recipeContainsParent(recipe)) {
						AllowedRecipe allowedRecipe = new AllowedRecipe(this,recipe);
						if (allowedRecipe.isExecutable)
							allowedRecipes.add(allowedRecipe);
					}
				}
			}
		}

		public RecipeOutput(AllowedRecipe parentRecipe, IDType unknownID) {
			this.parentRecipe = parentRecipe;
			this.output = null;
			this.outputID = unknownID;
			this.allowedRecipes = null;
			this.isBaseInput = false;
		}

		public void printTree(PrintStream out, String firstIndent, String nextIndent) {
			if (output == null) {
				out.printf("%s%s%s is unknown", firstIndent, dataModel.getIngredientName(outputID));
				return;
			}
			out.printf("%s%s%s", firstIndent, dataModel.getIngredientName(outputID), isBaseInput?"  [BaseInput]":"");
			if (allowedRecipes.size() == 1) {
				allowedRecipes.get(0).printTree(out, " ", nextIndent);
			} else {
				out.println();
				for (AllowedRecipe recipe:allowedRecipes )
					recipe.printTree(out, nextIndent+"      ", nextIndent+"      ");
			}
		}

		private boolean recipeContainsParent(DataModel<IDType>.InputValueCombination recipe) {
			if (recipe.contains(output.getID())) return true;
			if (parentRecipe==null) return false;
			return parentRecipe.parentRecipeOutput.recipeContainsParent(recipe);
		}
		
	}
	
	private class AllowedRecipe {

		private final RecipeOutput parentRecipeOutput;
		private final DataModel<IDType>.InputValueCombination recipe;
		private final Vector<RecipeOutput> inputs;
		private boolean isExecutable;

		public AllowedRecipe(RecipeOutput parentRecipeOutput, DataModel<IDType>.InputValueCombination recipe) {
			this.parentRecipeOutput = parentRecipeOutput;
			this.recipe = recipe;
			this.isExecutable = true;
			this.inputs = new Vector<>();// new RecipeOutput[this.recipe.values.size()];
			this.recipe.forEach(val->{
				RecipeOutput recipeOutput;
				DataModel<IDType>.Ingredient input = dataModel.getIngredient(val);
				if (input==null) {
					inputs.add(new RecipeOutput(this,val));
				} else {
					inputs.add(recipeOutput = new RecipeOutput(this,input));
					if (recipeOutput.allowedRecipes.isEmpty() && !recipeOutput.isBaseInput)
						isExecutable = false;
				}
			});
		}

		public void printTree(PrintStream out, String firstIndent, String nextIndent) {
			if (inputs.size() == 1) {
				out.printf("%s<--", firstIndent);
				inputs.firstElement().printTree(out, " ", nextIndent);
			} else {
				out.printf("%s<-- %s%n", firstIndent, recipe.toString());
				for (RecipeOutput input:inputs)
					input.printTree(out, nextIndent+"      ", nextIndent+"      ");
			}
		}
	}

}