package ca.celticminstrel.cookbook;

import org.bukkit.configuration.Configuration;

public abstract class Option<T> {
	protected String node;
	protected T def;
	private static Configuration config;
	
	@SuppressWarnings("hiding")
	protected Option(String node, T def) {
		this.node = node;
		this.def = def;
	}
	
	public abstract T get();
	
	public void set(T value) {
		config.set(node, value);
	}
	
	public void remove() {
		config.set(node, null);
	}
	
	public void reset() {
		set(def);
	}
	
	public static void setConfiguration(Configuration c) {
		config = c;
	}
	
	protected Configuration setDefault() {
		if(def != null && config.get(node) == null) config.set(node, def);
		return config;
	}
}

class OptionBoolean extends Option<Boolean> {
	@SuppressWarnings("hiding") OptionBoolean(String node, boolean def) {
		super(node, def);
	}

	@Override
	public Boolean get() {
		return setDefault().getBoolean(node, def);
	}
}

class OptionString extends Option<String> {
	@SuppressWarnings("hiding") OptionString(String node, String def) {
		super(node, def);
	}

	@Override
	public String get() {
		return setDefault().getString(node, def);
	}
}

class OptionInteger extends Option<Integer> {
	@SuppressWarnings("hiding") OptionInteger(String node, int def) {
		super(node, def);
	}

	@Override
	public Integer get() {
		return setDefault().getInt(node, def);
	}
}