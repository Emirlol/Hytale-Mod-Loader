package me.ancientri.hytale.modloader;

import net.fabricmc.loader.impl.util.log.LogCategory;
import net.fabricmc.loader.impl.util.log.LogHandler;
import net.fabricmc.loader.impl.util.log.LogLevel;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.logging.Level;

public class HytaleLogHandler implements LogHandler {
	// The JIT should constant-fold these, so it's fine
	private static final MethodHandle GET_LOGGER;
	private static final MethodHandle AT_LEVEL;
	private static final MethodHandle WITH_CAUSE;
	private static final MethodHandle LOG_MSG;
	private static final MethodHandle IS_ENABLED;

	static {
		try {
			// Reflectively resolve HytaleLogger logic to avoid compile-time dependency on the game jar
			Class<?> loggerClass = Class.forName("com.hypixel.hytale.logger.HytaleLogger");
			MethodHandles.Lookup lookup = MethodHandles.publicLookup();

			// public static HytaleLogger get(String name)
			Method getMethod = loggerClass.getMethod("get", String.class);
			GET_LOGGER = lookup.unreflect(getMethod);

			Class<?> loggerInstanceClass = getMethod.getReturnType();

			// public Api at(Level level)
			Method atMethod = loggerInstanceClass.getMethod("at", Level.class);
			AT_LEVEL = lookup.unreflect(atMethod);

			Class<?> apiClass = atMethod.getReturnType();

			// Api methods
			WITH_CAUSE = lookup.unreflect(apiClass.getMethod("withCause", Throwable.class));
			LOG_MSG = lookup.unreflect(apiClass.getMethod("log", String.class));
			IS_ENABLED = lookup.unreflect(apiClass.getMethod("isEnabled"));
		} catch (Exception e) {
			throw new RuntimeException("Failed to link HytaleLogger via reflection", e);
		}
	}

	@Override
	public void log(long time, LogLevel level, LogCategory category, String msg, Throwable exc, boolean fromReplay, boolean wasSuppressed) {
		try {
			// HytaleLogger logger = HytaleLogger.get(categoryName);
			Object logger = GET_LOGGER.invoke(getCategoryName(category));

			// Api api = logger.at(julLevel);
			Object api = AT_LEVEL.invoke(logger, convertLevel(level));

			if (exc != null) {
				// api = api.withCause(exc);
				api = WITH_CAUSE.invoke(api, exc);
			}

			// api.log(msg);
			LOG_MSG.invoke(api, msg);
		} catch (Throwable t) {
			// Fallback for critical failure in logging bridge
			t.printStackTrace();
		}
	}

	private String getCategoryName(LogCategory category) {
		// Normalize the category name to ensure a clean logger name
		if (category == null || category == LogCategory.LOG || category.name.isEmpty()) {
			return "FabricLoader";
		}
		return category.name;
	}

	@Override
	public boolean shouldLog(LogLevel level, LogCategory category) {
		try {
			Object logger = GET_LOGGER.invoke(getCategoryName(category));
			Object api = AT_LEVEL.invoke(logger, convertLevel(level));
			return (boolean) IS_ENABLED.invoke(api);
		} catch (Throwable t) {
			return false;
		}
	}

	@Override
	public void close() {
		// JUL doesn't require closing for loggers
	}

	private Level convertLevel(LogLevel level) {
		return switch (level) {
			case ERROR -> Level.SEVERE;
			case WARN -> Level.WARNING;
			case INFO -> Level.INFO;
			case DEBUG -> Level.FINE;
			case TRACE -> Level.FINEST;
		};
	}
}
