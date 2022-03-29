package dev.jbang.eclipse.core.internal.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.LineComment;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;

class JBangConfigVisitor extends ASTVisitor {

		private static final Pattern GROOVY_GRAPES = Pattern.compile("^(@Grab|@Grapes).*$");

		private static final Pattern JBANG_INSTRUCTIONS = Pattern.compile("^(//[A-Z_]+ ).*$");


		private List<String> configElements = new ArrayList<>();

		private String source;

		public JBangConfigVisitor(String source) {
			this.source = source;
		}

		public List<String> getConfigElements() {
			return configElements;
		}

		@Override
		public boolean visit(SingleMemberAnnotation node) {
			String annotation = getContent(node);
			if (GROOVY_GRAPES.matcher(annotation).matches()) {
				configElements.add(annotation);
			}
			return super.visit(node);
		}

		@Override
		public boolean visit(MarkerAnnotation node) {
			String annotation = getContent(node);
			if (GROOVY_GRAPES.matcher(annotation).matches()) {
				configElements.add(annotation);
			}
			return super.visit(node);
		}

		@Override
		public boolean visit(LineComment node) {
			if (node.isLineComment()) {
				String comment = getContent(node);
				if (JBANG_INSTRUCTIONS.matcher(comment).matches()) {
					configElements.add(comment);
				}
			}
			return super.visit(node);
		}

		private String getContent(ASTNode node) {
			int start = node.getStartPosition();
			int end = start + node.getLength();
			return source.substring(start, end).trim();
		}

	}