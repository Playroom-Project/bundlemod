package ppbundle.features.protections.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Simple config file used by the protections feature.

 * Format is intentionally minimal: a few named lists with optional comments.
 */
public final class InteractionBlacklistConfig {

	public static final String RELATIVE_PATH = "ppbundle/protection-blacklist.cfg";

	private final List<IdPattern> blockPatterns = new ArrayList<>();
	private final List<IdPattern> itemPatterns = new ArrayList<>();
	private final List<IdPattern> entityPatterns = new ArrayList<>();

	public static InteractionBlacklistConfig loadOrCreate() {
		Path configDir = FabricLoader.getInstance().getConfigDir();
		Path file = configDir.resolve(RELATIVE_PATH);

		try {
			Files.createDirectories(file.getParent());
		} catch (IOException ignored) {}

		if (!Files.exists(file)) {
			try {
				Files.writeString(file, defaultConfigText(), StandardCharsets.UTF_8);
			} catch (IOException ignored) {}
		}

		InteractionBlacklistConfig cfg = new InteractionBlacklistConfig();

		List<String> lines;
		try {
			lines = Files.readAllLines(file, StandardCharsets.UTF_8);
		} catch (IOException e) {
			lines = Collections.emptyList();
		}

		parse(lines, cfg);
		return cfg;
	}

	/* ------------------------------------------------------------ */
	/* BLOCK MATCHING                                               */
	/* ------------------------------------------------------------ */

	public boolean isBlockedBlock(String namespace, String path) {
		// 1. Direct + wildcard match
		if (matches(blockPatterns, namespace, path)) return true;

		// 2. Generic family fallback:
		// If config contains minecraft:anvil
		// also match chipped_anvil, damaged_anvil
		for (IdPattern pattern : blockPatterns) {
			if (!pattern.namespaceEquals(namespace)) continue;

			String base = pattern.getExactPathOrNull();
			if (base == null) continue; // wildcard already handled

			// direct equality already checked
			if (path.equals(base)) continue;

			// suffix match: *_base
			if (path.endsWith("_" + base)) return true;
		}

		return false;
	}

	public boolean isBlockedItem(String namespace, String path) {
		return matches(itemPatterns, namespace, path);
	}

	public boolean isBlockedEntity(String namespace, String path) {
		return matches(entityPatterns, namespace, path);
	}

	private static boolean matches(List<IdPattern> patterns, String namespace, String path) {
		for (IdPattern p : patterns) {
			if (p.matches(namespace, path)) return true;
		}
		return false;
	}

	/* ------------------------------------------------------------ */
	/* PARSER                                                       */
	/* ------------------------------------------------------------ */

	private static void parse(List<String> lines, InteractionBlacklistConfig cfg) {
		String section = "";
		boolean inList = false;

		for (String rawLine : lines) {
			String line = stripComments(rawLine).trim();
			if (line.isEmpty()) continue;

			if (line.startsWith("blocks:")) {
				section = "blocks";
				inList = line.contains("[");
			} else if (line.startsWith("items:")) {
				section = "items";
				inList = line.contains("[");
			} else if (line.startsWith("entities:")) {
				section = "entities";
				inList = line.contains("[");
			}

			if (line.contains("[")) inList = true;
			if (!inList) continue;

			String content = line.replace("[", " ").replace("]", " ").trim();
			if (content.isEmpty()) {
				if (line.contains("]")) inList = false;
				continue;
			}

			String[] parts = content.split(",");
			for (String part : parts) {
				String v = part.trim();
				if (v.isEmpty()) continue;

				if ((v.startsWith("\"") && v.endsWith("\"")) ||
						(v.startsWith("'") && v.endsWith("'"))) {
					v = v.substring(1, v.length() - 1).trim();
				}

				if (v.isEmpty()) continue;

				try {
					IdPattern p = new IdPattern(v);
					switch (section) {
						case "blocks" -> cfg.blockPatterns.add(p);
						case "items" -> cfg.itemPatterns.add(p);
						case "entities" -> cfg.entityPatterns.add(p);
					}
				} catch (IllegalArgumentException ignored) {}
			}

			if (line.contains("]")) inList = false;
		}
	}

	private static String stripComments(String line) {
		int idx = line.indexOf('#');
		return idx >= 0 ? line.substring(0, idx) : line;
	}

	private static String defaultConfigText() {
		return """
				# This config blacklists entities, items and blocks that will be prohibited from being used/interacted
				# inside other players' claimed areas (players without edit permissions).
				# Allies (players with edit permissions) will still be allowed.
				#
				# Wildcards are supported in the path using '*'. Example:
				#   betterend:*anvil
				#
				blocks: [
				  minecraft:respawn_anchor,
				  minecraft:cauldron,
				  minecraft:composter,
				  minecraft:anvil,
				  betterend:*anvil,
				  betternether:*anvil,
				]

				items: [
				  minecraft:flint_and_steel,
				  minecraft:fire_charge,
				]

				entities: [
				]
				""";
	}

	private InteractionBlacklistConfig() {
	}
}