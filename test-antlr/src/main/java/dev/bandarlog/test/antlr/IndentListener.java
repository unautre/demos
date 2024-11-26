package dev.bandarlog.test.antlr;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.TerminalNode;

public class IndentListener implements ParseTreeListener {

	private final Parser parser;

	public IndentListener(Parser parser) {
		this.parser = parser;
	}

	@Override
	public void visitTerminal(TerminalNode node) {
		final int depth = ((RuleContext)node.getParent()).depth() + 1;
		
		for (int i = 0; i < depth; i++)
			System.out.print("  ");
		System.out.println("Terminal: " + node.getText());
	}

	@Override
	public void visitErrorNode(ErrorNode node) {
		// TODO Auto-generated method stub
	}

	@Override
	public void enterEveryRule(ParserRuleContext ctx) {
		if (ctx.getText().length() == 0)
			return;
		
		final String ruleName = parser.getRuleNames()[ctx.getRuleIndex()];

		for (int i = 0; i < ctx.depth(); i++)
			System.out.print("  ");
		System.out.printf("Rule: %s:%d depth: %d length: %d%n", ruleName, ctx.getRuleIndex(), ctx.depth(), ctx.getText().length());
	}

	@Override
	public void exitEveryRule(ParserRuleContext ctx) {

	}
}