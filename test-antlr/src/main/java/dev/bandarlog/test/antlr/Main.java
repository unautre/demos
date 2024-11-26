package dev.bandarlog.test.antlr;

import java.io.IOException;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import dev.bandarlog.test.antlr.postgres.PostgreSQLLexer;
import dev.bandarlog.test.antlr.postgres.PostgreSQLParser;

public class Main {

	public static void main(String[] args) throws IOException {
		// create a CharStream that reads from standard input
		final CharStream input = CharStreams.fromStream(System.in);
		
		// create a lexer that feeds off of input CharStream
		final PostgreSQLLexer lexer = new PostgreSQLLexer(input);
		
		// create a buffer of tokens pulled from the lexer
		final CommonTokenStream tokens = new CommonTokenStream(lexer);
		
		// create a parser that feeds off the tokens buffer
		final PostgreSQLParser parser = new PostgreSQLParser(tokens);
		
		ParseTree tree = parser.root();
		
		System.out.println(tree.toStringTree(parser)); // print LISP-style tree
	}
}