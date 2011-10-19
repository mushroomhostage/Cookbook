package ca.celticminstrel.cookbook;

import org.getspout.spoutapi.material.Material;
import org.getspout.spoutapi.material.MaterialData;

public final class SpoutProxy {
	public static Material getMaterial(org.bukkit.Material mat) {
		return MaterialData.getOrCreateMaterial(mat.getId(), (short)0);
	}
	
	public static Material getMaterial(org.bukkit.Material mat, int data) {
		return MaterialData.getOrCreateMaterial(mat.getId(), (short)data);
	}
}
