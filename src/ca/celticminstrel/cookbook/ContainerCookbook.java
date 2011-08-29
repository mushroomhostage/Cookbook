package ca.celticminstrel.cookbook;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Material;
import static org.bukkit.Material.*;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.material.MaterialData;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.inventory.InventoryBuilder;

import net.minecraft.server.Block;
import net.minecraft.server.ContainerWorkbench;
import net.minecraft.server.EntityHuman;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.InventoryCraftResult;
import net.minecraft.server.InventoryCrafting;
import net.minecraft.server.Slot;
import net.minecraft.server.SlotResult;

public class ContainerCookbook extends ContainerWorkbench {
	public enum ItemGroup implements Iterable<Material> {
		BLOCK(STONE,COBBLESTONE,DIRT,GRAVEL,SAND,SANDSTONE,CLAY,GRASS,SOIL,
			COAL_ORE,IRON_ORE,GOLD_ORE,LAPIS_ORE,DIAMOND_ORE,REDSTONE_ORE,GLOWSTONE,NETHERRACK,SOUL_SAND,
			ICE,SNOW_BLOCK,WOOL,GLASS,BRICK,WOOD,MOSSY_COBBLESTONE,OBSIDIAN,BEDROCK),
		CONTAINER(CHEST,FURNACE,DISPENSER,WORKBENCH,null,null,null,LOCKED_CHEST,TNT,
			JUKEBOX,NOTE_BLOCK,BOOKSHELF,MOB_SPAWNER,null,null,null,WOOD_STAIRS,COBBLESTONE_STAIRS,
			IRON_BLOCK,GOLD_BLOCK,LAPIS_BLOCK,DIAMOND_BLOCK,null,null,null,STEP,DOUBLE_STEP),
		PLANT(SAPLING,LEAVES,LOG,CACTUS,PUMPKIN,SPONGE,DEAD_BUSH,LONG_GRASS,WATER,
			YELLOW_FLOWER,RED_ROSE,BROWN_MUSHROOM,RED_MUSHROOM,SEEDS,SUGAR_CANE,null,null,null,
			WEB,SNOW,null,null,null,null,PORTAL,FIRE,LAVA),
		MECHANISM(RAILS,POWERED_RAIL,DETECTOR_RAIL,MINECART,STORAGE_MINECART,POWERED_MINECART,BOAT,COMPASS,WATCH,
			REDSTONE,STONE_BUTTON,LEVER,REDSTONE_TORCH_ON,DIODE,WOOD_PLATE,STONE_PLATE,PISTON_BASE,PISTON_STICKY_BASE,
			WOOD_DOOR,IRON_DOOR,TRAP_DOOR,FENCE,SIGN,TORCH,JACK_O_LANTERN,REDSTONE_TORCH_OFF,LADDER),
		TOOL(WOOD_PICKAXE,WOOD_AXE,WOOD_SPADE,WOOD_HOE,DIAMOND_PICKAXE,DIAMOND_AXE,DIAMOND_SPADE,DIAMOND_HOE,
				FLINT_AND_STEEL,
			STONE_PICKAXE,STONE_AXE,STONE_SPADE,STONE_HOE,GOLD_PICKAXE,GOLD_AXE,GOLD_SPADE,GOLD_HOE,SHEARS,
			IRON_PICKAXE,IRON_AXE,IRON_SPADE,IRON_HOE,BUCKET,WATER_BUCKET,LAVA_BUCKET,MILK_BUCKET,FISHING_ROD),
		ARMOUR(LEATHER_HELMET,LEATHER_CHESTPLATE,LEATHER_LEGGINGS,LEATHER_BOOTS,DIAMOND_HELMET,DIAMOND_CHESTPLATE,
				DIAMOND_LEGGINGS,DIAMOND_BOOTS,BOW,
			CHAINMAIL_HELMET,CHAINMAIL_CHESTPLATE,CHAINMAIL_LEGGINGS,CHAINMAIL_BOOTS,GOLD_HELMET,GOLD_CHESTPLATE,
				GOLD_LEGGINGS,GOLD_BOOTS,ARROW,
			IRON_HELMET,IRON_LEGGINGS,IRON_CHESTPLATE,IRON_BOOTS,WOOD_SWORD,STONE_SWORD,IRON_SWORD,DIAMOND_SWORD,
				GOLD_SWORD),
		FOOD(APPLE,GOLDEN_APPLE,null,null,null,MUSHROOM_SOUP,null,null,SUGAR,
			BREAD,CAKE,COOKIE,null,null,null,null,null,EGG,
			PORK,GRILLED_PORK,RAW_FISH,COOKED_FISH,null,null,null,null,WHEAT),
		ITEM(GREEN_RECORD,GOLD_RECORD,PAINTING,BOOK,PAPER,MAP,null,STICK,CLAY_BRICK,
			COAL,IRON_INGOT,GOLD_INGOT,DIAMOND,FLINT,SULPHUR,GLOWSTONE_DUST,CLAY_BALL,SNOW_BALL,
			FEATHER,LEATHER,SLIME_BALL,INK_SACK,STRING,BONE,BOWL,SADDLE,BED),
		;
		private Material icon;
		private List<Material> inven;
		
		private ItemGroup(Material... contents) {
			icon = contents[0];
			inven = Arrays.asList(contents);
			if(contents.length != 27)
				Cookbook.warning("ItemGroup " + name() + " has the wrong number of items! (" + contents.length + ")");
		}
		
		public Material getIcon() {
			return icon;
		}

		@Override
		public Iterator<Material> iterator() {
			return inven.iterator();
		}
	}
	
	public final static boolean DIRECTION_UP = true;
	public final static boolean DIRECTION_DOWN = false;
	
	private EntityPlayer who;
	private int x, y, z;
	private InventoryCrafting matrix = new InventoryCrafting(this, 3, 3);
	private InventoryCraftResult result = new InventoryCraftResult();
	private CraftInventory toolbar;
	private ItemGroup group;
	private int currentRecipe = -1;
	private Cookbook cookbook;

	public ContainerCookbook(EntityPlayer player, int benchX, int benchY, int benchZ, Cookbook plugin) {
		super(player.inventory,player.world,benchX,benchY,benchZ);
		super.d.clear();
		super.e.clear();
		cookbook = plugin;
		who = player;
		x = benchX;
		y = benchY;
		z = benchZ;
		initializeToolbar();
		SlotResult resultSlot = new SlotResult(who, matrix, result, 0, 124, 35);
		this.a(resultSlot);
		
		int h;
		int v;
		
		for (h = 0; h < 3; ++h) {
			for (v = 0; v < 3; ++v) {
				super.a(new Slot(matrix, v + h * 3, 30 + v * 18, 17 + h * 18));
			}
		}
		
		for (h = 0; h < 3; ++h) {
			for (v = 0; v < 9; ++v) {
				super.a(new Slot(toolbar.getInventory(), v + h * 9 + 9, 8 + v * 18, 84 + h * 18));
			}
		}
		
		for (h = 0; h < 9; ++h) {
			super.a(new Slot(toolbar.getInventory(), h, 8 + h * 18, 142));
		}
		
		cycleRecipe(DIRECTION_UP);
	}
	
	private void initializeToolbar() {
		InventoryBuilder build = SpoutManager.getInventoryBuilder();
		toolbar = (CraftInventory)build.construct(36, "Cookbook");
		// Navigation; right-click will be "next" and left-click will be "previous"
		toolbar.setItem(0, new ItemStack(Material.COMPASS,0,(short)127));
		// Group selections;
		ItemGroup[] groups = ItemGroup.values();
		for(int i = 0; i < groups.length; i++)
			toolbar.setItem(i + 1, new ItemStack(groups[i].getIcon(),0,(short)127));
		// The group itself
		setGroup(ItemGroup.BLOCK);
	}
	
	public void setGroup(ItemGroup grp) {
		group = grp;
		int i = 9;
		for(Material item : group) {
			if(item != null) toolbar.setItem(i, new ItemStack(item));
			else toolbar.setItem(i, null);
			i++;
		}
	}
	
	public ItemStack cycleData(int slot, boolean upwards) {
		if(slot < 9 || slot >= 27) throw new IllegalArgumentException("can't cycle data in slot " + slot);
		ItemStack stack = toolbar.getItem(slot);
		short dmg = stack.getDurability();
		short max = stack.getType().getMaxDurability();
		if(max < 0) max = stack.getType() == Material.MAP ? Short.MAX_VALUE : 15;
		if(upwards) dmg++; else dmg--;
		if(dmg <= 0) dmg = max;
		if(dmg > max) dmg = 1;
		stack.setDurability(dmg);
		toolbar.setItem(slot, stack); // probably not needed
		super.a();
		return stack;
	}
	
	public void cycleRecipe(boolean upwards) {
		if(upwards) currentRecipe++; else currentRecipe--;
		Recipe recipe = cookbook.getRecipe(currentRecipe);
		if(recipe == null) {
			currentRecipe = upwards ? 0 : cookbook.numRecipes() - 1;
			recipe = cookbook.getRecipe(currentRecipe);
		}
		for(int i = 0; i < 9; i++) matrix.setItem(i, null);
		if(recipe instanceof ShapedRecipe) {
			ShapedRecipe shaped = (ShapedRecipe)recipe;
			String[] shape = shaped.getShape();
			for(int i = 0; i < shape.length; i++) {
				for(int j = 0; j < shape[i].length(); j++) {
					char c = shape[i].charAt(j);
					int slot = j + (i * 3);
					matrix.setItem(slot, mcStackFromMD(shaped.getIngredientMap().get(c)));
				}
			}
			super.a(matrix);
		} else if(recipe instanceof ShapelessRecipe) {
			ShapelessRecipe shapeless = (ShapelessRecipe)recipe;
			ArrayList<MaterialData> ingred = shapeless.getIngredientList();
			int slot = 0;
			for(MaterialData data : ingred)
				matrix.setItem(slot++, mcStackFromMD(data));
			super.a(matrix);
		} else if(recipe instanceof FurnaceRecipe) {
			FurnaceRecipe smelt = (FurnaceRecipe)recipe;
			matrix.setItem(1, mcStackFromMD(smelt.getInput()));
			matrix.setItem(4, new net.minecraft.server.ItemStack(Material.FIRE.getId(), 0, 0));
			matrix.setItem(8, new net.minecraft.server.ItemStack(Material.BURNING_FURNACE.getId(), 0, 0));
		}
		result.setItem(0, mcStackFromIS(recipe.getResult()));
		super.a();
	}

	public net.minecraft.server.ItemStack mcStackFromMD(MaterialData data) {
		if(data == null) return null;
		ItemStack stack = data.toItemStack(1);
		return mcStackFromIS(stack);
	}
	
	public net.minecraft.server.ItemStack mcStackFromIS(ItemStack stack) {
		if(stack == null) return null;
		net.minecraft.server.ItemStack mcstack =
			new net.minecraft.server.ItemStack(stack.getTypeId(), stack.getAmount(), stack.getDurability());
		return mcstack;
	}
	
	public ItemStack getTool(int slot) {
		if(slot < 9 || slot >= 27) throw new IllegalArgumentException("can't get tool from slot " + slot);
		ItemStack stack = toolbar.getItem(slot);
		return new ItemStack(stack.getType(), 1, stack.getDurability());
	}

	@Override // Check whether the player is still allowed to view the cookbook
	public boolean b(EntityHuman eh) {
		//Cookbook.debug("Checking if player can view ContainerCookbook...");
		int typeId = who.world.getTypeId(this.x, this.y, this.z);
		//Cookbook.debug("Block at (" + x + "," + y + "," + z + ") is " + Material.getMaterial(typeId));
		if(typeId != Block.WORKBENCH.id) return false;
		double distanceSq = eh.e(this.x + 0.5d, this.y + 0.5d, this.z + 0.5d);
		//Cookbook.debug("Distance from ContainerCookbook: " + Math.sqrt(distanceSq));
		return distanceSq <= 64.0d;
	}
}
