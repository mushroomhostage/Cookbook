package ca.celticminstrel.cookbook;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.Recipe;

public class RecipeListener implements Listener {
	private Cookbook plugin;
	public RecipeListener(Cookbook cookbook) {
		plugin = cookbook;
	}
	
	@EventHandler
	public void onPreCraft(PrepareItemCraftEvent evt) {
		Recipe recipe = evt.getRecipe();
		if(!hasPermission(evt.getInventory().getHolder(), recipe)) {
			evt.getInventory().setResult(null);
		}
	}

	private boolean hasPermission(InventoryHolder holder, Recipe recipe) {
		if(!(holder instanceof Player)) {
			Cookbook.debug("Error: Crafting inventory holder was not a player!");
			return true;
		}
		Player who = (Player)holder;
		boolean can = Options.PERMISSIONS_DEFAULT.get();
		if(Options.PERMISSIONS_BY_NAME.get()) {
			String name = plugin.getRecipeName(recipe);
			if(name != null) {
				Cookbook.debug("Checking if " + who.getName() + " is allowed to craft recipe: " + name);
				String permission = "cookbook.craft.name." + name;
				if(who.isPermissionSet(permission)) can = who.hasPermission(permission);
			} else Cookbook.debug("Note: " + who.getName() + " crafted a recipe whose name is unknown.");
		}
		if(Options.PERMISSIONS_BY_RESULT.get()) {
			Cookbook.debug("Checking if " + who.getName() + " is allowed to craft item: " + recipe.getResult().getType());
			String permission = "cookbook.craft.result." + getFormattedResult(recipe);
			if(who.isPermissionSet(permission)) can = who.hasPermission(permission);
		}
		return can;
	}

	private String getFormattedResult(Recipe recipe) {
		return recipe.getResult().getType().toString().toLowerCase().replace('_', '-');
	}
}
