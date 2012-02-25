package ca.celticminstrel.cookbook;

import org.bukkit.Material;

public final class Options {
	public static OptionBoolean PERMISSIONS_BY_RESULT = new OptionBoolean("permissions-by-result", false);
	public static OptionInteger VIEW_WAND = new OptionInteger("wands.view", Material.BOOK.getId());
	public static OptionInteger SHAPED_WAND = new OptionInteger("wands.shaped", Material.IRON_SWORD.getId());
	public static OptionInteger SHAPELESS_WAND = new OptionInteger("wands.shapeless", Material.BUCKET.getId());
	public static OptionInteger SMELT_WAND = new OptionInteger("wands.smelt", Material.COAL.getId());
	public static OptionString STARTUP = new OptionString("startup", "reset");
	public static OptionBoolean FIX_LAVA_BUCKET = new OptionBoolean("fix.lava-bucket", true);
	public static OptionBoolean FIX_SOUP_BOWL = new OptionBoolean("fix.soup-bowl", false);
	public static OptionBoolean FIX_GLASS_BOTTLE = new OptionBoolean("fix.glass-bottle", false);
	private Options() {}
}
