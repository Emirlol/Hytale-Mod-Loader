package me.ancientri.hytale.modloader;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.game.LibClassifier.LibraryType;

public enum HytaleLibrary implements LibraryType {
	MAIN("com/hypixel/hytale/Main.class"),
	LATE_MAIN("com/hypixel/hytale/LateMain.class"),
	SERVER("com.hypixel.hytale.server.core.HytaleServer.class");

	private final String[] paths;

	HytaleLibrary(String... paths) {
		this.paths = paths;
	}

	@Override
	public boolean isApplicable(EnvType env) {
		return env == EnvType.SERVER;
	}

	@Override
	public String[] getPaths() {
		return paths;
	}
}
