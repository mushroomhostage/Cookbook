package ca.celticminstrel.cookbook;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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
		Cookbook.debug("Received PLAYER_INTERACT...");
		if(event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		Cookbook.debug("Event was RIGHT_CLICK_BLOCK...");
		int viewItem = cookbook.getConfiguration().getInt("view-wand", Material.BOOK.getId());
		final Player player = event.getPlayer();
		if(player.getItemInHand().getType() != Material.getMaterial(viewItem)) return;
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
			sPlayer.sendNotification("Viewing recipes!", "x recipes installed", Material.WORKBENCH);
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
}
