package dev.jbang.eclipse.ui.internal.preferences;

import java.util.regex.Pattern;

public class Version {


	public static void main(String[] args) {
		var nameVersionPattern = Pattern.compile("JBang (\\d+\\.\\d+\\.\\d+)");
		System.err.println(nameVersionPattern.matcher("JBang 6.6.6").matches());
		System.err.println(nameVersionPattern.matcher("JBang 0.83.0").matches());
	}
}
