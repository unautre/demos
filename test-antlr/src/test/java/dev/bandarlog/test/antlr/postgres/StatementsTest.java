package dev.bandarlog.test.antlr.postgres;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.TestRig;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.Test;

import dev.bandarlog.test.antlr.IndentListener;

public class StatementsTest {

	private void parse(String s) {
		// create a CharStream that reads from standard input
		final CharStream input = CharStreams.fromString(s);
		
		// create a lexer that feeds off of input CharStream
		final PostgreSQLLexer lexer = new PostgreSQLLexer(input);
		
		// create a buffer of tokens pulled from the lexer
		final CommonTokenStream tokens = new CommonTokenStream(lexer);
		
		// create a parser that feeds off the tokens buffer
		final PostgreSQLParser parser = new PostgreSQLParser(tokens);
		final ParseTree tree = parser.root();
		
		final ParseTreeWalker walker = new ParseTreeWalker();
		walker.walk(new IndentListener(parser), tree);
		
//		System.out.println(tree.toStringTree(parser)); // print LISP-style tree
	}
	
	@Test
	public void test1() throws Exception {
		parse("SELECT * FROM mytable ;");
	}
	
	@Test
	public void test2() throws Exception {
		parse("SELECT * FROM (SELECT * FROM mytable WHERE classification = 'CD') ;");
	}
}