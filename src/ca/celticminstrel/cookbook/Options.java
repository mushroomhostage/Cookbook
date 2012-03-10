package ca.celticminstrel.cookbook;

import org.bukkit.Material;

public final class Options {
	public static Option<Boolean> PERMISSIONS_DEFAULT = new OptionBoolean("permissions.default", true);
	public static Option<Boolean> PERMISSIONS_BY_RESULT = new OptionBoolean("permissions.by-result", false);
	public static Option<Boolean> PERMISSIONS_BY_NAME = new OptionBoolean("permissions.by-name", false);
	public static Option<Integer> VIEW_WAND = new OptionInteger("wands.view", Material.BOOK.getId());
	public static Option<Integer> SHAPED_WAND = new OptionInteger("wands.shaped", Material.IRON_SWORD.getId());
	public static Option<Integer> SHAPELESS_WAND = new OptionInteger("wands.shapeless", Material.BUCKET.getId());
	public static Option<Integer> SMELT_WAND = new OptionInteger("wands.smelt", Material.COAL.getId());
	public static Option<String> STARTUP = new OptionString("startup", "reset");
	public static Option<Boolean> FIX_LAVA_BUCKET = new OptionBoolean("fix.lava-bucket", true);
	public static Option<Boolean> FIX_SOUP_BOWL = new OptionBoolean("fix.soup-bowl", false);
	public static Option<Boolean> FIX_GLASS_BOTTLE = new OptionBoolean("fix.glass-bottle", false);
	public static Option<Boolean> DEBUG = new OptionBoolean("debug", false);
	private Options() {}
}
