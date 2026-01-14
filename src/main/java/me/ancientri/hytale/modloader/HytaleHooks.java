package me.ancientri.hytale.modloader;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.impl.FabricLoaderImpl;

import java.io.File;


public class HytaleHooks {
	public static final String INTERNAL_NAME = HytaleHooks.class.getName().replace('.', '/');

	@SuppressWarnings("unused") // The call to this method is injected into the game itself
	public static void preInit() {
		var runDir = new File(".");
		FabricLoaderImpl loader = FabricLoaderImpl.INSTANCE;
		loader.prepareModInit(runDir.toPath(), loader.getGameInstance());
		loader.invokeEntrypoints("preInit", ModInitializer.class, ModInitializer::onInitialize);
	}

//	public static void init() {
//		FabricLoaderImpl loader = FabricLoaderImpl.INSTANCE;
//		loader.invokeEntrypoints("init", ModInitializer.class, ModInitializer::onInitialize);
//	}
//
//	public static void initResources() {
//		FabricLoaderImpl loader = FabricLoaderImpl.INSTANCE;
//		loader.invokeEntrypoints("initResources", ModInitializer.class, ModInitializer::onInitialize);
//	}
//
//	public static void postInit() {
//		FabricLoaderImpl loader = FabricLoaderImpl.INSTANCE;
//		loader.invokeEntrypoints("postInit", ModInitializer.class, ModInitializer::onInitialize);
//	}
}
