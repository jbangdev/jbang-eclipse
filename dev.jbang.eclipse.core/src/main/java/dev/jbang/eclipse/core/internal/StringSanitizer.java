package dev.jbang.eclipse.core.internal;

import java.util.regex.Pattern;

public class StringSanitizer {

	private StringSanitizer() {}

	private static final Pattern WHITESPACES_AND_LINE_TERMINATORS = Pattern.compile("[\\s|\\r|\\n]");
	
	private static final Pattern WHITESPACES = Pattern.compile("\\s+");
	
	public static String removeAllSpaces(String content) {
		return WHITESPACES_AND_LINE_TERMINATORS.matcher(content).replaceAll("");
	}
	
	public static String normalizeSpaces(String content) {
		return WHITESPACES.matcher(content).replaceAll(" ");
	}
	
}
