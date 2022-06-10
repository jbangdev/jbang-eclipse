package dev.jbang.eclipse.core.internal.process;

import org.junit.jupiter.api.Test;
import static dev.jbang.eclipse.core.internal.process.JBangExecution.sanitizeError;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;

public class JBangExecutionTest {

	@Test
	public void resolutionErrors() {
		String line = "Unable to collect/resolve dependency tree for a resolution due to: The following artifacts could not be resolved: bar.foo:jbang:jar:1.2.3, foo.bar:toto:jar:6.6.6: Could not find artifact foo.bar:toto:jar:6.6.6 in mavencentral (https://repo1.maven.org/maven2/), caused by: Could not find artifact foo.bar:toto:jar:6.6.6 in mavencentral (https://repo1.maven.org/maven2/)";
		var errors = new ArrayList<>(sanitizeError(line));
		assertEquals(2, errors.size());
		
		assertEquals(JBangDependencyError.class, errors.get(0).getClass());
		assertEquals("bar.foo:jbang:1.2.3", ((JBangDependencyError)errors.get(0)).getDependency());

		assertEquals(JBangDependencyError.class, errors.get(1).getClass());
		assertEquals("foo.bar:toto:6.6.6", ((JBangDependencyError)errors.get(1)).getDependency());
		
	}
	
}
