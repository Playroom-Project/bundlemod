package ppbundle.features.protections.config;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Simple namespace:path matcher supporting '*' wildcards in the path.
 */
public final class IdPattern {

	private final String namespace;
	private final String rawPath;
	private final Pattern compiledWildcard; // null if exact

	public IdPattern(String input) {
		Objects.requireNonNull(input, "Pattern cannot be null");

		String trimmed = input.trim();
		int idx = trimmed.indexOf(':');
		if (idx <= 0 || idx == trimmed.length() - 1) {
			throw new IllegalArgumentException("Invalid id pattern: " + input);
		}

		this.namespace = trimmed.substring(0, idx);
		this.rawPath = trimmed.substring(idx + 1);

		if (rawPath.contains("*")) {
			String regex = rawPath.replace(".", "\\.")
					.replace("*", ".*");
			this.compiledWildcard = Pattern.compile("^" + regex + "$");
		} else {
			this.compiledWildcard = null;
		}
	}

	public boolean matches(String ns, String path) {
		if (!namespace.equals(ns)) return false;

		if (compiledWildcard != null) {
			return compiledWildcard.matcher(path).matches();
		}

		return rawPath.equals(path);
	}

	/* ------------------------------------------------------------ */
	/*  Family-matching support                                     */
	/* ------------------------------------------------------------ */

	public boolean namespaceEquals(String ns) {
		return this.namespace.equals(ns);
	}

	/**
	 * Returns exact path if pattern is NOT wildcard.
	 * Returns null if pattern contains wildcard.
	 */
	public String getExactPathOrNull() {
		return compiledWildcard == null ? rawPath : null;
	}

	@Override
	public String toString() {
		return namespace + ":" + rawPath;
	}
}