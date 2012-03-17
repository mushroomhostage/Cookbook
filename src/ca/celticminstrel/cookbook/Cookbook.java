package ca.celticminstrel.cookbook;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static java.lang.Math.min;
import static java.lang.Math.max;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Furnace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import net.minecraft.server.Item;

public class Cookbook extends JavaPlugin implements Listener {
	enum InitMethod {COMPOUND, RESET, CLEAN};
	private static Logger log;
	private static FileConfiguration config;
	private LinkedHashMap<String,Recipe> newRecipes = new LinkedHashMap<String,Recipe>();
	private Pattern stripComments = Pattern.compile("([^#]*)#.*");
	private Pattern furnacePat = Pattern.compile("\\s*([a-zA-Z0-9_-]+)\\s+->\\s+([0-9]+)[x\\s]\\s*([a-zA-Z0-9_/-]+)\\s*(.*)");
	private Pattern resultPat = Pattern.compile("\\s*->\\s*([0-9]+)[x\\s]\\s*([a-zA-Z0-9_/-]+)\\s*(.*)");
	private List<String> files = new ArrayList<String>();
	private static Cookbook plugin;
	
	@Override
	public void onDisable() {
		info(getDescription().getFullName() + " disabled.");
	}
	
	@Override
	public void onEnable() {
		log = getLogger();
		info(getDescription().getFullName() + " enabled.");
		config = getConfig();
		File yml = new File(getDataFolder(), "config.yml");
		if(!yml.exists()) {
			// TODO: Generate defaults
			try {
				yml.createNewFile();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		Option.setConfiguration(config);
		resetOrClear();
		File recipes = new File(getDataFolder(),config.getString("recipefile", "recipes.cb"));
		loadRecipes(recipes);
		showRecipes();
		PluginManager pm = getServer().getPluginManager();
		// TODO: Uncomment this line
		//pm.registerEvents(new WindowListener(this), this);
		pm.registerEvents(new RecipeListener(this), this);
		pm.registerEvents(this, this);
		if(Options.FIX_LAVA_BUCKET.get()) {
			info("Lava bucket fix enabled!");
		}
		if(Options.FIX_SOUP_BOWL.get()) {
			Item.MUSHROOM_SOUP.a(Item.BOWL);
			info("Soup bowl fix enabled!");
		}
		if(Options.FIX_GLASS_BOTTLE.get()) {
			Item.POTION.a(Item.GLASS_BOTTLE);
			Item.EXP_BOTTLE.a(Item.GLASS_BOTTLE);
			info("Glass bottle fix enabled! Note that this affects all potions, " +
				"including splash potions and (starting in 1.2) experience bottles.");
		}
		plugin = this;
		debug("Finished loading!");
	}
	
	@EventHandler
	public void onFurnaceBurn(FurnaceBurnEvent evt) {
		if(!Options.FIX_LAVA_BUCKET.get()) {
			evt.getHandlers().unregister((Listener)this);
			return;
		}
		debug("Furnace burning with fuel " + evt.getFuel());
		final Furnace furnace = (Furnace)evt.getBlock().getState();
		if(evt.getFuel().getType() == Material.LAVA_BUCKET)
			Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable(){
				@Override public void run() {
					furnace.getInventory().setItem(1, new ItemStack(Material.BUCKET, 1));
				}
			});
	}
	
	@EventHandler
	public void onCraft(final InventoryClickEvent evt) { // TODO: Change to CraftItemEvent
		if(!(evt.getInventory() instanceof CraftingInventory)) return; // TODO: This line won't be needed
		if(evt.getRawSlot() != 0) return; // TODO: This line won't be needed
		boolean scheduleUpdate = false, scheduleFix = false;
		final CraftingInventory inven = (CraftingInventory)evt.getInventory();
		debug("Contains potion? " + (inven.contains(Material.POTION) || inven.contains(Material.EXP_BOTTLE)));
		debug("Contains soup? " + inven.contains(Material.MUSHROOM_SOUP));
		debug("Contains bucket? " + containsBucket(inven));
		debug("Shift click? " + evt.isShiftClick());
		for(ItemStack item : inven.getMatrix()) {
			debug(String.valueOf(item));
		}
		if(Options.FIX_GLASS_BOTTLE.get() && (inven.contains(Material.POTION) || inven.contains(Material.EXP_BOTTLE)))
			scheduleFix = scheduleUpdate = true;
		else if(Options.FIX_SOUP_BOWL.get() && inven.contains(Material.MUSHROOM_SOUP))
			scheduleFix = scheduleUpdate = true;
		else if(Options.FIX_BUCKETS.get() && containsBucket(inven))
			scheduleFix = scheduleUpdate = true;
		else if(evt.isShiftClick()) scheduleUpdate = true;
		scheduleFix = scheduleFix && Options.FIX_BUCKETS.get();
		scheduleUpdate = scheduleUpdate && evt.getWhoClicked() instanceof Player;
		if(scheduleFix) {
			final int slot = evt.getWhoClicked().getInventory().firstEmpty();
			final ItemStack[] contents = inven.getMatrix();
			Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable(){
				@Override public void run() {
					PlayerInventory pack = evt.getWhoClicked().getInventory();
					for(int i = slot; i >= 0 && i < pack.getSize() && pack.getItem(i) != null; i++) {
						ItemStack inPack = pack.getItem(i);
						int id = inPack.getTypeId();
							debug("Comparing " + inPack + " in the pack to...");
						for(int j = 0; j < contents.length; j++) {
							if(contents[j] == null) continue;
							debug("..." + contents[j] + " in the matrix...");
							Item item = Item.byId[contents[j].getTypeId()];
							if(item != null && item.k() && item.j().id == id && contents[j].getAmount() == 1) {
								debug("Moving a " + Material.getMaterial(id) + "!");
								ItemStack replace = new ItemStack(item.j().id, 1);
								contents[j] = null; // TODO: Should assign replace rather than null...
								inven.setItem(j + 1, replace);
								inPack.setAmount(inPack.getAmount() - 1);
								if(inPack.getAmount() == 0) pack.setItem(i, null);
								else i--;
								break;
							}
						}
					}
				}
			});
		}
		if(scheduleUpdate) {
			debug("Scheduling an inventory update for " + evt.getWhoClicked().getName() + "!");
			Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
				@Override@SuppressWarnings("deprecation") public void run() {
					((Player)evt.getWhoClicked()).updateInventory();
				}
			}, 1);
		}
	}

	private boolean containsBucket(CraftingInventory i) {
		return i.contains(Material.WATER_BUCKET) || i.contains(Material.LAVA_BUCKET) || i.contains(Material.MILK_BUCKET);
	}

	public static void info(String string) {
		log.info(string);
	}

	public static void warning(String string) {
		log.warning(string);
	}

	public static void debug(String string) {
		if(Options.DEBUG.get()) log.info("[DEBUG] " + string);
	}
	
	private void loadRecipes(File recipes) {
		String prefix = "Loading " + recipes.getName() + ": ";
		try {
			Scanner in = new Scanner(recipes);
			List<String> file = new ArrayList<String>();
			while(in.hasNextLine()) {
				String line = in.nextLine();
				Matcher m = stripComments.matcher(line);
				if(m.matches()) line = m.group(1).trim();
				else line = line.trim();
				file.add(line);
			}
			ListIterator<String> iter = file.listIterator();
			while(iter.hasNext()) {
				String directive = iter.next();
				if(directive.isEmpty()) continue;
				if(!directive.startsWith("@")) {
					warning(prefix + "Unexpected data on line " + iter.nextIndex() + "; skipping.");
					continue;
				}
				String name = "";
				if(directive.contains(" ")) {
					int i = directive.indexOf(' ');
					name = directive.substring(i + 1);
					directive = directive.substring(0, i);
				}
				if(directive.equalsIgnoreCase("@Smelt")) loadSmelting(iter, prefix, name);
				else if(directive.equalsIgnoreCase("@Shaped")) loadShaped(iter, prefix, name);
				else if(directive.equalsIgnoreCase("@Shapeless")) loadShapeless(iter, prefix, name);
				else if(directive.equals("@Include")) loadInclude(iter, prefix, name);
				else warning(prefix + "Invalid directive " + directive + " on line " + iter.nextIndex() + ".");
			}
		} catch(FileNotFoundException e) {}
	}
	
	private void showRecipes() {
		info("Loaded " + newRecipes.size() + " custom recipes.");
		for(Entry<String,Recipe> entry : newRecipes.entrySet()) {
			Recipe recipe = entry.getValue();
			StringBuilder show = new StringBuilder();
			show.append(recipe.getClass().getSimpleName()).append("(").append(entry.getKey());
			if(Options.DEBUG.get()) {
				show.append('=');
				if(recipe instanceof FurnaceRecipe) {
					formatItem(show,((FurnaceRecipe)recipe).getInput());
				} else if(recipe instanceof ShapelessRecipe) {
					List<ItemStack> ingred = ((ShapelessRecipe)recipe).getIngredientList();
					boolean first = true;
					for(ItemStack stack : ingred) {
						if(first) {
							show.append('[');
							first = false;
						} else show.append(", ");
						formatItem(show,stack);
					}
					show.append(']');
				} else if(recipe instanceof ShapedRecipe) {
					ShapedRecipe shaped = (ShapedRecipe)recipe;
					show.append(Arrays.asList(shaped.getShape())).append(" -- ");
					Map<Character,ItemStack> ingred = shaped.getIngredientMap();
					boolean first = true;
					for(char c : ingred.keySet()) {
						if(first) {
							show.append('{');
							first = false;
						} else show.append(", ");
						show.append(c).append('=');
						formatItem(show,ingred.get(c));
					}
					show.append('}');
				}
				ItemStack result = recipe.getResult();
				show.append(" -> ");
				formatItem(show,result);
				if(!result.getEnchantments().isEmpty()) {
					show.append(" -- ");
					for(Enchantment ench : result.getEnchantments().keySet()) {
						formatEnchant(show,ench).append('=').append(result.getEnchantmentLevel(ench));
					}
				}
				show.append(')');
				debug("Loaded " + show);
			} else {
				show.append(')');
				getLogger().info(show.toString());
			}
		}
	}
	
	private void loadInclude(ListIterator<String> iter, String prefix, String name) {
		if(name.isEmpty()) {
			warning(prefix + "Invalid include directive on line " + iter.previousIndex() + "; no file specified.");
			return;
		}
		if(files.contains(name)) {
			warning(prefix + "Include loop detected on line " + iter.previousIndex() + "; skipping!");
			return;
		}
		files.add(name);
		File file = new File(getDataFolder(), name);
		if(!file.exists()) {
			warning(prefix + "Couldn't find include file on line " + iter.previousIndex() + "!");
			return;
		}
		loadRecipes(file);
	}

	private StringBuilder formatItem(StringBuilder show, ItemStack stack) {
		if(stack == null) return show.append("null");
		return show.append("ItemStack{").append(stack.getType().toString()).append('/')
			.append(stack.getDurability()).append('}');
	}
	
	private StringBuilder formatEnchant(StringBuilder show, Enchantment enchant) {
		return show.append("Enchantment{").append(enchant.toString()).append('}');
	}
	
	private void loadShaped(ListIterator<String> iter, String prefix, String name) {
		if(!iter.hasNext()) {
			warning(prefix + "Expected shapeless recipe on line " + iter.nextIndex() + " but found end-of-file.");
			return;
		}
		int width = 0;
		String ingred;
		ItemStack[] line1, line2, line3;
		Matcher m;
		// First line
		ingred = iter.next();
		m = resultPat.matcher(ingred);
		if(m.matches()) {
			warning(prefix + "Shaped recipe on line " + iter.nextIndex() + " is missing shape.");
			return;
		}
		line1 = parseShapedLine(ingred, iter.nextIndex(), prefix);
		if(line1 == null) return;
		width = max(width, line1.length);
		// Second line
		if(!iter.hasNext()) {
			warning(prefix + "Expected shaped recipe (shape or result) on line " + iter.nextIndex() +
				" but found end-of-file.");
			return;
		}
		ingred = iter.next();
		m = resultPat.matcher(ingred);
		if(m.matches()) {
			addShapedRecipe(width, m, iter.nextIndex(), prefix, name, line1);
			return;
		}
		line2 = parseShapedLine(ingred, iter.nextIndex(), prefix);
		if(line2 == null) return;
		width = max(width, line2.length);
		// Third line
		if(!iter.hasNext()) {
			warning(prefix + "Expected shaped recipe (shape or result) on line " + iter.nextIndex() +
				" but found end-of-file.");
			return;
		}
		ingred = iter.next();
		m = resultPat.matcher(ingred);
		if(m.matches()) {
			addShapedRecipe(width, m, iter.nextIndex(), prefix, name, line1, line2);
			return;
		}
		line3 = parseShapedLine(ingred, iter.nextIndex(), prefix);
		if(line3 == null) return;
		width = max(width, line3.length);
		// Result
		if(!iter.hasNext()) {
			warning(prefix + "Expected recipe result on line " + iter.nextIndex() + " but found end-of-file.");
			return;
		}
		ingred = iter.next();
		m = resultPat.matcher(ingred);
		if(m.matches()) {
			addShapedRecipe(width, m, iter.nextIndex(), prefix, name, line1, line2, line3);
			return;
		}
		warning(prefix + "Missing recipe result on line " + iter.nextIndex() + ".");
		iter.previous(); // Back up in case the "unknown data" is a directive starting the next recipe
	}

	private void addShapedRecipe(int w, Matcher m, int lineno, String prefix, String name, ItemStack[]... lines) {
		debug("Result pat subpattern matches: " + m.group(1) + ", " + m.group(2) + ", " + m.group(3));
		ItemStack stack = parseResult(m.group(1), m.group(2), m.group(3), lineno, prefix);
		debug("Have enchanted result? " + stack.getEnchantments());
		ShapedRecipe recipe = new ShapedRecipe(stack);
		int h = lines.length;
		switch(h) {
		case 1:
			switch(w) {
			case 1: recipe.shape("a"); break;
			case 2: recipe.shape("ab"); break;
			case 3: recipe.shape("abc"); break;
			}
			break;
		case 2:
			switch(w) {
			case 1: recipe.shape("a","b"); break;
			case 2: recipe.shape("ab","cd"); break;
			case 3: recipe.shape("abc","def"); break;
			}
			break;
		case 3:
			switch(w) {
			case 1: recipe.shape("a","b","c"); break;
			case 2: recipe.shape("ab","cd","ef"); break;
			case 3: recipe.shape("abc","def","ghi"); break;
			}
			break;
		}
		char c = 'a';
		for(int i = 0; i < h; i++) {
			ItemStack[] line = lines[i];
			for(int j = 0; j < w; j++) {
				if(j < line.length && line[j] != null && line[j].getType() != Material.AIR)
					recipe.setIngredient(c, line[j].getType(), line[j].getDurability());
				c++;
			}
		}
		addRecipe(recipe, name);
	}

	private ItemStack[] parseShapedLine(String line, int lineno, String prefix) {
		String[] split = line.split("\\s+");
		if(split.length > 3) {
			warning(prefix + "Shape cannot be " + split.length + " wide on line " + lineno + ".");
			return null;
		}
		ItemStack[] parsed = new ItemStack[split.length];
		for(int i = 0; i < split.length; i++) {
			String[] item = split[i].split("/");
			parsed[i] = parseMaterial(item[0], item.length > 1 ? item[1] : "", lineno, prefix);
		}
		return parsed;
	}

	private void loadShapeless(ListIterator<String> iter, String prefix, String name) {
		if(!iter.hasNext()) {
			warning(prefix + "Expected shapeless recipe on line " + iter.nextIndex() + " but found end-of-file.");
			return;
		}
		String recipe = iter.next();
		String[] split = recipe.split("\\s*,\\s*");
		if(split.length > 9) {
			warning(prefix + "Too many ingredients for shapeless recipe on line " + iter.nextIndex() +
				"; skipping excess.");
		}
		if(!iter.hasNext()) {
			warning(prefix + "Expected recipe result on line " + iter.nextIndex() + " but found end-of-file.");
			return;
		}
		Matcher m = resultPat.matcher(iter.next());
		if(!m.matches()) {
			warning(prefix + "Missing recipe result on line " + iter.nextIndex() + ".");
			iter.previous(); // Back up in case the "unknown data" is a directive starting the next recipe
			return;
		}
		debug("Result pat subpattern matches: " + m.group(1) + ", " + m.group(2) + ", " + m.group(3));
		ItemStack result = parseResult(m.group(1), m.group(2), m.group(3), iter.nextIndex(), prefix);
		debug("Have enchanted result? " + result.getEnchantments());
		ShapelessRecipe shapeless = new ShapelessRecipe(result);
		for(int i = 0; i < min(split.length, 9); i++) {
			String[] mat = split[i].split("/");
			String data = mat.length > 1 ? mat[1] : "";
			ItemStack ingred = parseMaterial(mat[0], data, iter.nextIndex(), prefix);
			if(ingred == null) return;
			shapeless.addIngredient(ingred.getType(), ingred.getDurability());
		}
		addRecipe(shapeless, name);
	}

	private void loadSmelting(ListIterator<String> iter, String prefix, String name) {
		if(!iter.hasNext()) {
			warning(prefix + "Expected furnace recipe on line " + iter.nextIndex() + " but found end-of-file.");
			return;
		}
		String recipe = iter.next();
		Matcher m = furnacePat.matcher(recipe);
		if(!m.matches()) {
			warning(prefix + "Missing furnace recipe on line " + iter.nextIndex() + ".");
			iter.previous(); // Back up in case the "unknown data" is a directive starting the next recipe
			return;
		}
		Material smelt = parseMaterial(m.group(1), iter.nextIndex(), prefix);
		if(smelt == null) return;
		debug("Furnace pat subpattern matches: " + m.group(1) + ", " + m.group(2) + ", " + m.group(3) + ", " + m.group(4));
		ItemStack result = parseResult(m.group(2), m.group(3), m.group(4), iter.nextIndex(), prefix);
		if(result == null) return;
		debug("Have enchanted result? " + result.getEnchantments());
		FurnaceRecipe furnace = new FurnaceRecipe(result, smelt);
		addRecipe(furnace, name);
	}

	private ItemStack parseResult(String amount, String item, String ench, int lineno, String prefix) {
		if(!amount.matches("[0-9]+x?")) {
			warning(prefix + "Invalid amount " + amount + " on line " + lineno + "; defaulting to 1.");
			amount = "1";
		}
		amount = amount.replace("x", "");
		String[] split = item.split("/");
		Material material = parseMaterial(split[0], lineno, prefix);
		if(material == null) return null;
		short data = 0;
		if(split.length > 1) {
			if(!split[1].matches("[0-9]+")) {
				warning(prefix + "Invalid data " + split[1] + " on line " + lineno + "; defaulting to 0.");
				split[1] = "0";
			}
			data = Short.parseShort(split[1]);
			if(material.getMaxDurability() > 0 && data > material.getMaxDurability()) {
				warning(prefix + "Invalid data " + data + " for material " + material + " on line " + lineno +
					"; continuing anyway.");
			} else if(material == Material.MAP && getServer().getMap(data) == null) {
				warning(prefix + "Invalid data for material MAP on line " + lineno + "; map ID " + data +
					" does not exist. Continuing anyway.");
			} else if(material.getMaxDurability() == -1 && data >= 16) {
				warning(prefix + "Invalid data " + data + " for material " + material + " on line " + lineno +
					"; continuing anyway.");
			}
		}
		ItemStack result = new ItemStack(material, Integer.parseInt(amount), data);
		ench = ench.trim();
		debug("Parsing enchantments: " + ench);
		if(ench.isEmpty()) return result;
		for(String magic : ench.split("\\s")) {
			split = magic.split("=");
			Enchantment enchantment = magic.matches("[0-9]+") ? Enchantment.getById(Integer.parseInt(split[0]))
				: Enchantment.getByName(split[0]);
			if(enchantment == null) {
				warning(prefix + "Unknown enchantment " + split[0] + " on line " + lineno + "; skipping.");
				continue;
			}
			if(!enchantment.canEnchantItem(result)) {
				warning(prefix + "Invalid or conflicting enchantment " + split[0] + " for material " + material +
					" on line " + lineno + "; skipping.");
				continue;
			}
			int level = 1;
			if(split.length > 1) {
				if(split[1].matches("[0-9]+"))
					level = Integer.parseInt(split[1]);
				else warning(prefix + "Invalid enchantment level " + split[1] + " on line " + lineno +
					"; defaulting to 1.");
			}
			if(level > enchantment.getMaxLevel())
				warning(prefix + "Enchantment level " + level + " too high for enchantment " + split[0] +
					" on line " + lineno + "; defaulting to maximum (" + (level = enchantment.getMaxLevel()) + ").");
			result.addEnchantment(enchantment, level);
		}
		debug("Have enchanted result? " + result.getEnchantments());
		return result;
	}
	
	private Material parseMaterial(String name, int lineno, String prefix) {
		name = name.replace('-', '_').toUpperCase();
		Material mat = Material.getMaterial(name);
		if(mat == null && name.matches("\\d+")) mat = Material.getMaterial(Integer.parseInt(name));
		if(mat == null) warning(prefix + "Invalid material " + name + " on line " + lineno + ".");
		return mat;
	}
	
	private ItemStack parseMaterial(String name, String data, int lineno, String prefix) {
		Material mat = parseMaterial(name, lineno, prefix);
		if(mat == null) return null;
		if(data.isEmpty()) return new ItemStack(mat, 0, (short)0);
		if(!data.matches("-?[0-9]+")) {
			if(data.equals("*")) {
				data = "-1";
			} else {
				warning(prefix + "Invalid data " + data + " on line " + lineno + "; defaulting to 0.");
				data = "0";
			}
		}
		return new ItemStack(mat, 1, Short.parseShort(data));
	}

	private Random nameGen = new Random();
	private void addRecipe(Recipe recipe, String name) {
		getServer().addRecipe(recipe);
		if(newRecipes.containsKey(name)) {
			warning("Duplicate recipe name " + name + "; appending _ to make it unique.");
			do {
				name += '_';
			} while(newRecipes.containsKey(name));
		} else if(name.isEmpty()) {
			do {
				name = Integer.toHexString(nameGen.nextInt());
			} while(newRecipes.containsKey(name));
		}
		newRecipes.put(name.replace(' ', '_'), recipe);
	}

	private void resetOrClear() {
		files.clear();
		newRecipes.clear();
		InitMethod init = InitMethod.RESET;
		try {
			init = InitMethod.valueOf(Options.STARTUP.get().toUpperCase());
		} catch(Exception x) {
			warning("An exception occurred which is probably innocuous.");
			x.printStackTrace();
		}
		if(init == InitMethod.CLEAN) {
			Bukkit.getServer().clearRecipes();
		} else if(init == InitMethod.RESET) {
			Bukkit.getServer().resetRecipes();
		}
	}
	
	public String getRecipeName(Recipe recipe) {
		for(Entry<String,Recipe> entry : newRecipes.entrySet()) {
			Recipe toMatch = entry.getValue();
			if(!toMatch.getResult().equals(recipe.getResult())) continue;
			if(!toMatch.getClass().isAssignableFrom(recipe.getClass())) continue;
			if(recipe instanceof ShapedRecipe) {
				ItemStack[][] have = new ItemStack[][] {new ItemStack[3], new ItemStack[3], new ItemStack[3]};
				ItemStack[][] need = new ItemStack[][] {new ItemStack[3], new ItemStack[3], new ItemStack[3]};
				String[] haveShape = ((ShapedRecipe)toMatch).getShape();
				String[] needShape = ((ShapedRecipe)recipe).getShape();
				Map<Character,ItemStack> haveIngred = ((ShapedRecipe)toMatch).getIngredientMap();
				Map<Character,ItemStack> needIngred = ((ShapedRecipe)recipe).getIngredientMap();
				// TODO: This almost certainly won't work reliably
				for(int i = 0; i < 3; i++) {
					for(int j = 0; j < 3; j++) {
						have[i][j] = haveIngred.get(haveShape[i].charAt(j));
						need[i][j] = needIngred.get(needShape[i].charAt(j));
					}
				}
				if(!Arrays.deepEquals(have, need)) continue;
				for(int i = 0; i < 3; i++) {
					for(int j = 0; j < 3; j++) {
						have[i][j] = haveIngred.get(haveShape[i].charAt(j));
						need[i][j] = needIngred.get(needShape[i].charAt(j));
					}
				}
				if(!Arrays.deepEquals(have, need)) continue;
				return entry.getKey();
			} else if(recipe instanceof ShapelessRecipe) {
				List<ItemStack> need = ((ShapelessRecipe)recipe).getIngredientList();
				List<ItemStack> have = ((ShapelessRecipe)toMatch).getIngredientList();
				if(have.size() != need.size()) continue;
				need.removeAll(have);
				if(need.size() != 0) continue;
				return entry.getKey();
			} else if(recipe instanceof FurnaceRecipe) {
				if(((FurnaceRecipe)toMatch).getInput().equals(((FurnaceRecipe)recipe).getInput()))
					return entry.getKey();
			}
		}
		return null;
	}
}
