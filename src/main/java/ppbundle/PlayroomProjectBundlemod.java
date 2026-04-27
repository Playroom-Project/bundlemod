package ppbundle;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ppbundle.core.bootstrap.Bootstrap;

public final class PlayroomProjectBundlemod implements ModInitializer {

	public static final String MOD_ID = "playroom-project-bundlemod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		Bootstrap.initCommon();
	}
}