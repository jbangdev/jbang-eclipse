package dev.jbang.eclipse.core.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;

@SuppressWarnings("restriction")
public class JBangFileUtils {

	public static final Pattern GROOVY_GRAPES = Pattern.compile("^\\s*(@Grab|@Grapes).*", Pattern.DOTALL);

	public static final Pattern JBANG_INSTRUCTIONS = Pattern.compile("^//([A-Z_:]+)(\\s+.*)?$");

	private static final Pattern JBANG_HEADER = Pattern.compile("//.*jbang.*");

	public static final Pattern JAVA_INSTRUCTION = Pattern.compile("//JAVA (\\S*).*");

	public static final Pattern MODULE_INSTRUCTION = Pattern.compile("//MODULE (\\S*).*");

	public static final Pattern SOURCES_INSTRUCTION = Pattern.compile("//SOURCES (\\S*).*");

	public static final Pattern FILES_INSTRUCTION = Pattern.compile("//FILES (\\S*).*");

	private static final int LINE_LIMIT = 300;

	private static final Set<String> EXTENSIONS = new LinkedHashSet<>();

	static {
		EXTENSIONS.add("java");
		EXTENSIONS.add("jsh");
		EXTENSIONS.add("kt");
		EXTENSIONS.add("groovy");
	}

	private JBangFileUtils() {
	}

	public static boolean isJBangFile(IResource resource) {
		if (!(resource instanceof IFile file)
				|| EXTENSIONS.stream().filter(ext -> ext.equals(resource.getFileExtension())).findAny().isEmpty()) {
			return false;
		}
		try {
			return hasJBangInstructions(new InputStreamReader(file.getContents(true)));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean isJBangBuildFile(IResource resource) {
		if (!(resource instanceof IFile file)) {
			return false;
		}
		return JBangConstants.JBANG_BUILD.equals(file.getName());
	}

	public static boolean isJBangFile(Path file) {
		if (!Files.isRegularFile(file)) {
			return false;
		}
		String fileName = file.getFileName().toString().toLowerCase();

		if (EXTENSIONS.stream().filter(ext -> fileName.endsWith("." + ext)).findAny().isEmpty()) {
			return false;
		}
		try {
			return hasJBangInstructions(Files.newBufferedReader(file));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean isJBangBuildFile(Path file) {
		if (!Files.isRegularFile(file)) {
			return false;
		}
		return JBangConstants.JBANG_BUILD.equals(file.getFileName().toString());
	}

	public static boolean isMainFile(Path file) {
		if (!Files.isRegularFile(file)) {
			return false;
		}
		return JBangConstants.JBANG_MAIN.equals(file.getFileName().toString());
	}

	public static String getJavaVersion(String line) {
		return getMatch(JAVA_INSTRUCTION, line);
	}

	public static String getSource(String line) {
		return getMatch(SOURCES_INSTRUCTION, line);
	}

	public static String[] getFile(String line) {
		String file = getMatch(FILES_INSTRUCTION, line);
		String[] tuple = null;
		if (file != null) {
			tuple = file.split("=");
			if (tuple.length == 1) {
				return new String[] { file, file };
			}
		}
		return tuple;
	}

	private static String getMatch(Pattern pattern, String line) {
		var matcher = pattern.matcher(line);
		if (matcher.matches()) {
			return matcher.group(1);
		}
		return null;
	}

	public static String getPackageName(IFile file) {
		// TODO probably not the most efficient way to get the package name as this
		// reads the whole file;
		String content = null;
		try {
			content = ResourceUtil.getContent(file);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return getPackageName(content);
	}

	public static String getPackageName(Path file) {
		// TODO probably not the most efficient way to get the package name as this
		// reads the whole file;
		String content = null;
		try {
			content = ResourceUtil.getContent(file);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return getPackageName(content);
	}

	private static String getPackageName(String content) {
		var ast = createCompilationUnit(content);
		if (ast != null) {
			PackageDeclaration pkg = ast.getPackage();
			if (pkg != null && pkg.getName() != null) {
				return pkg.getName().getFullyQualifiedName();
			}
		}
		return null;
	}

	public static CompilationUnit createCompilationUnit(IFile file) {
		String content = null;
		try {
			content = ResourceUtil.getContent(file);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return createCompilationUnit(content);
	}

	public static CompilationUnit createCompilationUnit(String content) {
		if (content == null) {
			return null;
		}
		char[] source = content.toCharArray();

		ASTParser parser = ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setIgnoreMethodBodies(true);
		parser.setSource(source);
		CompilationUnit ast = (CompilationUnit) parser.createAST(null);
		return ast;
	}

	private static boolean hasJBangInstructions(Reader reader) throws IOException {
		try (BufferedReader br = new BufferedReader(reader)) {
			String line = null;
			for (int i = 0; (line = br.readLine()) != null && i < LINE_LIMIT; i++) {
				if (line.isBlank()) {
					continue;
				}
				if (isJBangInstruction(line)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean isConfigElement(String line) {
		return JBANG_INSTRUCTIONS.matcher(line).matches() || GROOVY_GRAPES.matcher(line).matches();
	}

	public static boolean isJBangInstruction(String line) {
		return JBANG_HEADER.matcher(line).matches() || isConfigElement(line);
	}

}
