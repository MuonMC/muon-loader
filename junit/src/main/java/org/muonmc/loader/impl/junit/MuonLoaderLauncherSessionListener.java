package org.muonmc.loader.impl.junit;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;
import org.muonmc.loader.api.game.minecraft.Environment;
import org.muonmc.loader.impl.launch.knot.Knot;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;
import org.muonmc.loader.impl.util.SystemProperties;

import java.util.Locale;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public class MuonLoaderLauncherSessionListener implements LauncherSessionListener {
	static {
		System.setProperty(SystemProperties.DEVELOPMENT, "true");
		System.setProperty(SystemProperties.UNIT_TEST, "true");
	}

	private final ClassLoader classLoader;

	private ClassLoader launcherSessionClassLoader;

	public MuonLoaderLauncherSessionListener() {
		final Thread currentThread = Thread.currentThread();
		final ClassLoader originalClassLoader = currentThread.getContextClassLoader();

		try {
			Knot knot = new Knot(Environment.valueOf(System.getProperty(SystemProperties.SIDE, Environment.CLIENT.name().toUpperCase(Locale.ROOT))));
			classLoader = knot.init(new String[]{});
		} finally {
			// Knot.init sets the context class loader, revert it back for now.
			currentThread.setContextClassLoader(originalClassLoader);
		}
	}

	@Override
	public void launcherSessionOpened(LauncherSession session) {
		final Thread currentThread = Thread.currentThread();
		launcherSessionClassLoader = currentThread.getContextClassLoader();
		currentThread.setContextClassLoader(classLoader);
	}

	@Override
	public void launcherSessionClosed(LauncherSession session) {
		final Thread currentThread = Thread.currentThread();
		currentThread.setContextClassLoader(launcherSessionClassLoader);
	}
}
