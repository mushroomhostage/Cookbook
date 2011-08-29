package ca.celticminstrel.cookbook;

import ca.celticminstrel.cookbook.ContainerCookbook.ItemGroup;

import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.getspout.spoutapi.event.inventory.*;

import net.minecraft.server.Container;

public class WindowListener extends InventoryListener {
	private Cookbook cookbook;

	public WindowListener(Cookbook plugin) {
		cookbook = plugin;
	}
	
	
	@Override@SuppressWarnings("incomplete-switch")
	public void onInventoryClick(InventoryClickEvent event) {
		Container container = ((CraftPlayer)event.getPlayer()).getHandle().activeContainer;
		if(!(container instanceof ContainerCookbook)) return;
		ContainerCookbook book = (ContainerCookbook)container;
		InventorySlotType slotType = event.getSlotType();
		boolean dir;
		if(event.isLeftClick()) dir = ContainerCookbook.DIRECTION_UP;
		else dir = ContainerCookbook.DIRECTION_DOWN;
		int slot = event.getSlot();
		switch(slotType) {
		case CRAFTING:
			break;
		case FUEL:
			break;
		case OUTSIDE:
			event.setCancelled(true);
			break;
		case PACK:
			if(event.isShiftClick()) book.cycleData(slot, dir);
			else event.setItem(book.getTool(slot));
			event.setCancelled(true);
			break;
		case QUICKBAR:
			if(slot == 0) {
				if(event.isShiftClick()) { // left-click -> save, right-click -> new
					if(event.isLeftClick()) { // save changes
					} else { // create new recipe
					}
				} else { // Navigate
					book.cycleRecipe(dir);
				}
			} else { // Switch group
				ItemGroup group = ItemGroup.values()[slot - 1];
				book.setGroup(group);
			}
			event.setCancelled(true);
			break;
		case RESULT:
			break;
		case SMELTING:
			break;
		}
	}
	
	@Override
	public void onInventoryClose(InventoryCloseEvent event) {}
	
	@Override
	public void onInventoryCraft(InventoryCraftEvent event) {}
	
	@Override
	public void onInventoryOpen(InventoryOpenEvent event) {}
}
