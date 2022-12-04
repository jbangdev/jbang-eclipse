package dev.jbang.eclipse.core.internal.expressions;


import java.util.Objects;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IResource;

import dev.jbang.eclipse.core.internal.JBangFileUtils;

/**
 * Determines whether a given {@link IResource} is a JBang file
 */
public class JBangResourceTester extends PropertyTester {

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    	if (receiver instanceof IResource resource) {
        	boolean isJBangFile = JBangFileUtils.isJBangFile(resource) || JBangFileUtils.isJBangBuildFile(resource);
        	return Objects.equals(isJBangFile, expectedValue);
        }
    	return false;
    }

}