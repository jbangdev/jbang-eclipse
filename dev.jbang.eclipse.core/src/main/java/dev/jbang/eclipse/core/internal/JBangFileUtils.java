package dev.jbang.eclipse.core.internal;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;

@SuppressWarnings("restriction")
public class JBangFileUtils {

	private static Pattern JBANG_HEADERS = Pattern.compile("(//.*jbang.*)|(//[A-Z:]+ ).*");

	private static Pattern JAVA_INSTRUCTION = Pattern.compile("//JAVA (\\S*).*");

	private static Pattern SOURCES_INSTRUCTION = Pattern.compile("//SOURCES (\\S*).*");
	
	private static Pattern FILES_INSTRUCTION = Pattern.compile("//FILES (\\S*).*");
	
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
		if (!(resource instanceof IFile)) {
			return false;
		}
		if (EXTENSIONS.stream().filter(ext -> ext.equals(resource.getFileExtension())).findAny().isEmpty()) {
			return false;
		}
		IFile file = (IFile) resource;
		try {
			return isJBangContent(new InputStreamReader(file.getContents()));			
		} catch (Exception e) {
			//ignore
		}
		return false;
	}
	
	public static boolean isJBangFile(Path file) {
		if (!(Files.isRegularFile(file))) {
			return false;
		}
		String fileName = file.getFileName().toString().toLowerCase();
		
		if (EXTENSIONS.stream().filter(ext -> fileName.endsWith("."+ext)).findAny().isEmpty()) {
			return false;
		}
		try {
			return isJBangContent(Files.newBufferedReader(file));			
		} catch (Exception e) {
			//ignore
		}
		return false;
	}
	
	private static boolean isJBangContent(Reader r) throws IOException {
		try (BufferedReader reader = new BufferedReader(r)) {
			String line;
			while((line = reader.readLine()) != null) {
				//XXX, only check the first n lines then bail?
				if (!line.isBlank()) {					
					return isJBangInstruction(line);
				}
			}
		}
		return false;
	}

	public static boolean isJBangInstruction(String line) {
		return JBANG_HEADERS.matcher(line).matches();
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
				return new String[] {file, file};
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

	public static String getPackageName(IJavaProject javaProject, IFile file) {
		//TODO probably not the most efficient way to get the package name as this reads the whole file;
		char[] source = null;
		try (InputStream is = new BufferedInputStream(file.getContents(true));
			 ByteArrayOutputStream result = new ByteArrayOutputStream()) {
			 byte[] buffer = new byte[1024];
			 for (int length; (length = is.read(buffer)) != -1; ) {
			     result.write(buffer, 0, length);
			 }
			 source = result.toString(file.getCharset()).toCharArray();
		} catch (IOException | CoreException e) {
			e.printStackTrace();
		}
		if (source == null) {
			return null;
		}
		
		ASTParser parser = ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setProject(javaProject);
		parser.setIgnoreMethodBodies(true);
		parser.setSource(source);
		CompilationUnit ast = (CompilationUnit) parser.createAST(null);
		PackageDeclaration pkg = ast.getPackage();
		return (pkg == null || pkg.getName() == null)? null :pkg.getName().getFullyQualifiedName();
	}
}
