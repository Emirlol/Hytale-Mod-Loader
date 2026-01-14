package me.ancientri.hytale.modloader;

import me.ancientri.hytale.modloader.patches.EntrypointPatch;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.game.LibClassifier;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.metadata.BuiltinModMetadata;
import net.fabricmc.loader.impl.metadata.ContactInformationImpl;
import net.fabricmc.loader.impl.util.Arguments;
import net.fabricmc.loader.impl.util.ExceptionUtil;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.fabricmc.loader.impl.util.log.LogHandler;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class HytaleGameProvider implements GameProvider {

	static {
		// This comes before `initialize`, so we can make sure all logs are formatted correctly by doing this early.
		// Putting this in `initialize` makes the first line use fabric's default logger format.
		System.setProperty("java.util.logging.manager", "com.hypixel.hytale.logger.backend.HytaleLogManager");
	}

	private Arguments arguments;
	private String main;
	private String lateMain;
	private HytaleMetadataLookup metadataLookup;
	private Path gameJar;
	private Collection<Path> validParentClassPath; // computed parent class path restriction (loader+deps)
	private GameTransformer transformer;

	@Override
	public String getGameId() {
		return "hytale";
	}

	@Override
	public String getGameName() {
		return metadataLookup.getName();
	}

	@Override
	public String getRawGameVersion() {
		return metadataLookup.getVersion();
	}

	@Override
	public String getNormalizedGameVersion() {
		return metadataLookup.getVersion();
	}

	@Override
	public Collection<BuiltinMod> getBuiltinMods() {
		Map<String, String> contactMap = Map.of(
				"discord", "https://discord.gg/hytale",
				"facebook", "https://www.facebook.com/HytaleGame",
				"bluesky", "https://bsky.app/profile/hytale.bsky.social",
				"instagram", "https://www.instagram.com/HytaleGame/",
				"twitter", "https://x.com/Hytale",
				"x", "https://x.com/Hytale",
				"tiktok", "https://www.tiktok.com/@hytale",
				"youtube", "https://www.youtube.com/Hytale",
				"homepage", "https://hytale.com/"
		);

		BuiltinModMetadata.Builder modMetadata = new BuiltinModMetadata.Builder(getGameId(), getNormalizedGameVersion())
														 .setName(getGameName())
														 .addAuthor("Hypixel", contactMap)
														 .setContact(new ContactInformationImpl(contactMap))
														 .setDescription("Hytale Server");

		HashMap<String, String> contactMapProvider = new HashMap<>();
		BuiltinModMetadata.Builder providerMetadata = new BuiltinModMetadata.Builder("hytale-provider", "1.0.0")
				.setName("Hytale Game Provider")
				.addAuthor("Rime", contactMapProvider)
				.setContact(new ContactInformationImpl(contactMapProvider))
				.setDescription("The game provider implementation for Hytale for the Fabric Loader.");

		return List.of(
				new BuiltinMod(Collections.singletonList(gameJar), modMetadata.build()),
				new BuiltinMod(Collections.emptyList(), providerMetadata.build())
		);
	}

	@Override
	public String getEntrypoint() {
		return main;
	}

	@Override
	public Path getLaunchDirectory() {
		if (arguments == null) {
			return Paths.get(".");
		}

		return getLaunchDirectory(arguments);
	}

	private static Path getLaunchDirectory(Arguments arguments) {
		return Paths.get(arguments.getOrDefault("gameDir", "."));
	}

	@Override
	public boolean requiresUrlClassLoader() {
		return true;
	}

	@Override
	public Set<BuiltinTransform> getBuiltinTransforms(String className) {
		return Set.of();
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public boolean locateGame(FabricLauncher launcher, String[] args) {
		this.arguments = new Arguments();
		arguments.parse(args);

		try {
			LibClassifier<HytaleLibrary> classifier = new LibClassifier<>(HytaleLibrary.class, EnvType.SERVER, this);
			HytaleLibrary gameLib = HytaleLibrary.MAIN;
			if (gameJar != null) {
				classifier.process(gameJar);
			} else {
				List<String> gameLocations = new ArrayList<>();

				String fabricGameJarPath = System.getProperty(SystemProperties.GAME_JAR_PATH);
				if (fabricGameJarPath != null) gameLocations.add(fabricGameJarPath);

				gameLocations.add("./HytaleServer.jar");
				gameLocations.add("../HytaleServer.jar");

				var gameLocation = gameLocations.stream()
												.map(Paths::get)
												.map(Path::toAbsolutePath)
												.map(Path::normalize)
												.filter(Files::exists)
												.findFirst();

				if (gameLocation.isPresent()) classifier.process(gameLocation.get());
			}

			classifier.process(launcher.getClassPath());
			gameJar = classifier.getOrigin(gameLib);
			main = classifier.getClassName(gameLib);
			lateMain = classifier.getClassName(HytaleLibrary.LATE_MAIN);
			validParentClassPath = classifier.getSystemLibraries();
		} catch (IOException e) {
			throw ExceptionUtil.wrap(e);
		}

		if (gameJar == null) throw new RuntimeException("Unable to locate game jar!");
		else {
			metadataLookup = new HytaleMetadataLookup(gameJar);
			transformer = new GameTransformer(new EntrypointPatch(lateMain));
		}

		return true;
	}

	@Override
	public void initialize(FabricLauncher launcher) {
		launcher.setValidParentClassPath(validParentClassPath);
		unlockClassPath(launcher); // The logger is acquired via reflection, so we need the game jar to be on the classpath to be able to retrieve its method handles.
		setupLogHandler(launcher, true);
		Log.debug(LogCategory.GAME_PROVIDER, "Valid classpath: " + validParentClassPath);
		transformer.locateEntrypoints(launcher, List.of(gameJar));
	}

	private void setupLogHandler(FabricLauncher launcher, boolean useTargetCl) {
		try {
			final String logHandlerClsName = HytaleLogHandler.class.getName();

			ClassLoader prevCl = Thread.currentThread().getContextClassLoader();
			Class<?> logHandlerCls;

			if (useTargetCl) {
				Thread.currentThread().setContextClassLoader(launcher.getTargetClassLoader());
				logHandlerCls = launcher.loadIntoTarget(logHandlerClsName);
			} else {
				logHandlerCls = Class.forName(logHandlerClsName);
			}

			Log.init((LogHandler) logHandlerCls.getConstructor().newInstance());
			Thread.currentThread().setContextClassLoader(prevCl);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public GameTransformer getEntrypointTransformer() {
		return transformer;
	}

	@Override
	public void unlockClassPath(FabricLauncher launcher) {
		launcher.addToClassPath(gameJar);
	}

	@Override
	public void launch(ClassLoader loader) {
		Class<?> targetClass;

		try {
			targetClass = loader.loadClass(main);
			MethodHandle invoker = MethodHandles.lookup().findStatic(targetClass, "main", MethodType.methodType(void.class, String[].class));
			var launchArgs = getLaunchArguments(false);
			String[] args = new String[launchArgs.length + 1];
			System.arraycopy(launchArgs, 0, args, 1, launchArgs.length);
			args[0] = "--assets=./Assets.zip";
			invoker.invoke((Object[]) args);
		} catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
			throw new RuntimeException("Failed to find the main class invoker!", e);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Arguments getArguments() {
		return arguments;
	}

	@Override
	public String[] getLaunchArguments(boolean sanitize) {
		if (arguments == null) return new String[0];

		String[] ret = arguments.toArray();
		if (!sanitize) return ret;

		int writeIdx = 0;

		for (int i = 0; i < ret.length; i++) {
			String arg = ret[i];

			if (i + 1 < ret.length && arg.startsWith("--")) i++; // skip value
			else ret[writeIdx++] = arg;
		}

		if (writeIdx < ret.length) ret = Arrays.copyOf(ret, writeIdx);

		return ret;
	}
}
