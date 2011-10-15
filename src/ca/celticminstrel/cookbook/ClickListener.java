package ca.celticminstrel.cookbook;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.player.SpoutPlayer;

import net.minecraft.server.ContainerWorkbench;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.ICrafting;

public class ClickListener extends PlayerListener {
	private Cookbook cookbook;

	public ClickListener(Cookbook plugin) {
		cookbook = plugin;
	}

	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		if(event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		Block block = event.getClickedBlock();
		if(block.getType() != Material.WORKBENCH && block.getType() != Material.FURNACE
			&& block.getType() != Material.BURNING_FURNACE) return;
		Player player = event.getPlayer();
		if(player.getItemInHand().getType() == Material.getMaterial(Option.VIEW_WAND.get()))
			viewRecipes(event);
		else if(player.getItemInHand().getType() == Material.getMaterial(Option.SHAPED_WAND.get()))
			newShaped(event);
		else if(player.getItemInHand().getType() == Material.getMaterial(Option.SHAPELESS_WAND.get()))
			newShapeless(event);
		else if(player.getItemInHand().getType() == Material.getMaterial(Option.SMELT_WAND.get()))
			newFurnace(event);
	}

	public void viewRecipes(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		Cookbook.debug("Correct wand is being held...");
		if(!player.hasPermission("cookbook.view")) {
			player.sendMessage("Sorry, you do not have permission to view available recipes.");
			return;
		}
		Cookbook.debug("Player has required permissions...");
		SpoutPlayer sPlayer = SpoutManager.getPlayer(player);
		final CraftPlayer cPlayer = (CraftPlayer) player;
		final EntityPlayer ePlayer = cPlayer.getHandle();
		if(sPlayer.isSpoutCraftEnabled()) {
			sPlayer.sendNotification("Viewing recipes!", "x recipes installed.", Material.WORKBENCH);
		} else {
			sPlayer.sendMessage("Viewing recipes! x recipes installed.");
		}
		final Location loc = event.getClickedBlock().getLocation();
		Cookbook.debug("Clicked location: " + loc);
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(cookbook, new Runnable() {
			@Override
			public void run() {
				// openWorkbenchWindow calls EntityPlayer.a(int,int,int) instead of EntityPlayer.b(int,int,int)
				// thus we must make that call directly instead.
				//sPlayer.openWorkbenchWindow(loc);
				cPlayer.getHandle().b(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
				// Replace the workbench container with our custom cookbook container
				ContainerWorkbench benchWindow = (ContainerWorkbench)ePlayer.activeContainer;
				ContainerCookbook bookWindow =
					new ContainerCookbook(ePlayer, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), cookbook);
				bookWindow.windowId = benchWindow.windowId;
				ePlayer.activeContainer = bookWindow;
				ICrafting listener = ePlayer;
				bookWindow.a(listener);
			}
		});
		event.setCancelled(true);
	}
	
	public void newShapeless(PlayerInteractEvent event) {}
	
	public void newShaped(PlayerInteractEvent event) {}
	
	public void newFurnace(PlayerInteractEvent event) {}
}
