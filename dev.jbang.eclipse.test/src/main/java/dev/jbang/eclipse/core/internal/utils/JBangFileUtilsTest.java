package dev.jbang.eclipse.core.internal.utils;

import static dev.jbang.eclipse.core.internal.JBangFileUtils.isJBangInstruction;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class JBangFileUtilsTest {

	@Test
	public void testIsJBangInstruction() {
		assertTrue(isJBangInstruction("//PREVIEW"));
		assertTrue(isJBangInstruction("//PREVIEW\n"));
		assertTrue(isJBangInstruction("//JAVA 18"));
		assertTrue(isJBangInstruction("//DEPS foo:bar  "));
		assertTrue(isJBangInstruction("//Q:CONFIG foo=bar"));
        assertTrue(isJBangInstruction("//COMPILE_OPTIONS -Dfoo=bar"));
		
        assertFalse(isJBangInstruction("//PREview"));
        assertFalse(isJBangInstruction("//JAVA20"));
        assertFalse(isJBangInstruction(" //JAVA 20"));
	}
	
}
