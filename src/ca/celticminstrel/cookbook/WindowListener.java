package ca.celticminstrel.cookbook;

import ca.celticminstrel.cookbook.ContainerCookbook.ItemGroup;

import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.Event.Result;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.ItemStack;
import org.getspout.spoutapi.event.inventory.*;

import net.minecraft.server.Container;

public class WindowListener extends InventoryListener {
	private Cookbook cookbook;

	public WindowListener(Cookbook plugin) {
		cookbook = plugin;
	}
	
	private int getSlot(int slot, InventorySlotType type) {
		// This assumes the slot is for a ContainerCookbook, which is the same as a ContainerWorkbench
		Cookbook.debug("Raw slot: " + slot);
		switch(type) {
		case RESULT: return 0; // Result slot
		case CRAFTING: return slot - 1; // Crafting slot
		case CONTAINER:
			if(slot == 36) return 0; // Quickbar slot
			return slot - 9; // Pack slot
		case OUTSIDE: return slot; // Outside slot
		case QUICKBAR: return slot > 10 ? 44 - slot : slot; // Quickbar slot
		}
		return slot;
	}
	
	@Override@SuppressWarnings("incomplete-switch")
	public void onInventoryClick(InventoryClickEvent event) {
		//Cookbook.debug("[" + event.getRawSlot() + "] Slot " + event.getSlot() + " of type " + event.getSlotType() + " clicked (raw slot " + event.getRawSlot() + ")");
		Container container = ((CraftPlayer)event.getPlayer()).getHandle().activeContainer;
		if(!(container instanceof ContainerCookbook)) return;
		ContainerCookbook book = (ContainerCookbook)container;
		InventorySlotType slotType = event.getSlotType();
		boolean dir, isPlayerSlot = event instanceof InventoryPlayerClickEvent;
		if(event.isLeftClick()) dir = ContainerCookbook.DIRECTION_UP;
		else dir = ContainerCookbook.DIRECTION_DOWN;
		//int rawSlot = event.getRawSlot();
		//int slot = getSlot(rawSlot, slotType);
		//if(rawSlot == 36) slotType = InventorySlotType.QUICKBAR;
		int slot = event.getSlot();
		Cookbook.debug("Click on slot " + slot + " of type " + slotType);
		switch(slotType) {
		case CRAFTING:
			break;
		case FUEL:
			break;
		case OUTSIDE:
			event.setCancelled(true);
			break;
		case CONTAINER:
			if(!isPlayerSlot) break;
			Cookbook.debug("Click in pack; is it shift-click?");
			if(event.isShiftClick()) {
				Cookbook.debug("Yes it is; cycling.");
				book.cycleData(slot, dir);
				event.setResult(Result.DENY);
			} else {
				Cookbook.debug("Nope; copying to cursor.");
				event.setResult(Result.ALLOW);
				if(event.getCursor() == null) event.setItem(book.getTool(slot));
				else event.setCursor(null);
			}
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
			event.setResult(Result.DENY);
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
	public void onInventoryCraft(InventoryCraftEvent event) {
		ItemStack result = event.getResult();
		String item = result.getType().toString().toLowerCase().replace('_', '-');
		if(Option.PERMISSIONS_BY_RESULT.get() && !event.getPlayer().hasPermission("cookbook.craft.item." + item)) {
			event.setCancelled(true);
			event.getPlayer().sendMessage("You can't craft that item!");
			return;
		}
	}
	
	@Override
	public void onInventoryOpen(InventoryOpenEvent event) {}
	
	//@Override
	public void onFurnaceSmelt(FurnaceSmeltEvent event) {}
	
	//@Override
	public void onFurnaceBurn(FurnaceBurnEvent event) {}
}
