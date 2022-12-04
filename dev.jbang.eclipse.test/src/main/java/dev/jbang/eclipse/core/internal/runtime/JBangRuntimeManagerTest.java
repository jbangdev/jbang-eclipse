package dev.jbang.eclipse.core.internal.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.jbang.eclipse.core.internal.AbstractJBangTest;

public class JBangRuntimeManagerTest {

	private JBangRuntimeManager jBangRuntimeManager;

	@BeforeEach
	private void setUp() throws IOException {
		jBangRuntimeManager = AbstractJBangTest.setupJBang();
	}

	@Test
	public void testGetDefaultRuntime() throws Exception {
		var defaultRuntime = jBangRuntimeManager.getDefaultRuntime();
		assertNotNull(defaultRuntime, "defaultRuntime is null");
		assertEquals("tests", defaultRuntime.getName());
	}

	@Test
	public void testReset() throws Exception {
		jBangRuntimeManager.reset();
		var systemRuntime = jBangRuntimeManager.getDefaultRuntime();
		assertNotNull(systemRuntime, "System runtime is null");
		assertEquals(JBangRuntime.SYSTEM, systemRuntime.getName());
		var runtimes = jBangRuntimeManager.getRuntimes();
		assertEquals(1, runtimes.size(), "runtimes were not reset");
		assertEquals(systemRuntime, runtimes.values().iterator().next());
	}

	@Test
	public void testGetJBangRuntimes() throws Exception {
		var defaultRuntime = jBangRuntimeManager.getDefaultRuntime();
		var badRuntime = new JBangRuntime("bad", "/foo/bar", "6.6.6");
		jBangRuntimeManager.setRuntimes(List.of(defaultRuntime, badRuntime));
		var runtimes = jBangRuntimeManager.getJBangRuntimes(false);
		assertEquals(3, runtimes.size(), "Found "+runtimes);

		runtimes = jBangRuntimeManager.getJBangRuntimes(true);
		assertEquals(1, runtimes.size());
		assertEquals("tests", runtimes.get(0).getName());
	}

	@Test
	public void testGetRuntime() throws Exception {
		var runtime1 = new JBangRuntime("jbang1", "/foo/bar", "1.1.1");
		var runtime2 = new JBangRuntime("jbang2", "/foo/bar", "1.1.1");
		jBangRuntimeManager.setRuntimes(List.of(runtime1, runtime2));

		assertEquals(runtime1, jBangRuntimeManager.getRuntime(runtime1.getName()));
		assertEquals(runtime2, jBangRuntimeManager.getRuntime(runtime2.getName()));
		assertEquals(JBangRuntime.SYSTEM, jBangRuntimeManager.getRuntime("nope").getName());
	}

}