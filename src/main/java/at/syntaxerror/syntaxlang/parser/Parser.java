/*
 * SyntaxLang - A simple programming language written in Java
 * Copyright (C) 2021  SyntaxError
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package at.syntaxerror.syntaxlang.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.function.Supplier;

import at.syntaxerror.syntaxlang.input.InputEnvironment;
import at.syntaxerror.syntaxlang.lexer.Keyword;
import at.syntaxerror.syntaxlang.lexer.Token;
import at.syntaxerror.syntaxlang.lexer.TokenType;
import at.syntaxerror.syntaxlang.lexer.Tokens;
import at.syntaxerror.syntaxlang.parser.node.AnonymousFuncNode;
import at.syntaxerror.syntaxlang.parser.node.ArrayAccessNode;
import at.syntaxerror.syntaxlang.parser.node.BinaryOpNode;
import at.syntaxerror.syntaxlang.parser.node.BreakNode;
import at.syntaxerror.syntaxlang.parser.node.ClassBodyNode;
import at.syntaxerror.syntaxlang.parser.node.ClassNode;
import at.syntaxerror.syntaxlang.parser.node.CloneNode;
import at.syntaxerror.syntaxlang.parser.node.ContinueNode;
import at.syntaxerror.syntaxlang.parser.node.DoWhileNode;
import at.syntaxerror.syntaxlang.parser.node.EmptyNode;
import at.syntaxerror.syntaxlang.parser.node.EnumConstantNode;
import at.syntaxerror.syntaxlang.parser.node.EnumNode;
import at.syntaxerror.syntaxlang.parser.node.ForNode;
import at.syntaxerror.syntaxlang.parser.node.ForeachNode;
import at.syntaxerror.syntaxlang.parser.node.FuncArgsNode;
import at.syntaxerror.syntaxlang.parser.node.FuncArgsPartNode;
import at.syntaxerror.syntaxlang.parser.node.FuncCallArgsNode;
import at.syntaxerror.syntaxlang.parser.node.FuncCallNode;
import at.syntaxerror.syntaxlang.parser.node.FuncDefNode;
import at.syntaxerror.syntaxlang.parser.node.IfBodyNode;
import at.syntaxerror.syntaxlang.parser.node.IfNode;
import at.syntaxerror.syntaxlang.parser.node.ImportNode;
import at.syntaxerror.syntaxlang.parser.node.InstanceofNode;
import at.syntaxerror.syntaxlang.parser.node.InterfaceNode;
import at.syntaxerror.syntaxlang.parser.node.ListNode;
import at.syntaxerror.syntaxlang.parser.node.LiteralNode;
import at.syntaxerror.syntaxlang.parser.node.MapNode;
import at.syntaxerror.syntaxlang.parser.node.MapPartNode;
import at.syntaxerror.syntaxlang.parser.node.MemberAccessNode;
import at.syntaxerror.syntaxlang.parser.node.NamespaceNode;
import at.syntaxerror.syntaxlang.parser.node.NewNode;
import at.syntaxerror.syntaxlang.parser.node.Node;
import at.syntaxerror.syntaxlang.parser.node.ParenthesizedNode;
import at.syntaxerror.syntaxlang.parser.node.ReturnNode;
import at.syntaxerror.syntaxlang.parser.node.ScopedNode;
import at.syntaxerror.syntaxlang.parser.node.StatementsNode;
import at.syntaxerror.syntaxlang.parser.node.SwitchCaseNode;
import at.syntaxerror.syntaxlang.parser.node.SwitchNode;
import at.syntaxerror.syntaxlang.parser.node.TernaryOpNode;
import at.syntaxerror.syntaxlang.parser.node.ThrowNode;
import at.syntaxerror.syntaxlang.parser.node.TryCatchNode;
import at.syntaxerror.syntaxlang.parser.node.UnaryOpNode;
import at.syntaxerror.syntaxlang.parser.node.UseAsNode;
import at.syntaxerror.syntaxlang.parser.node.VarDeclNode;
import at.syntaxerror.syntaxlang.parser.node.VarModNode;
import at.syntaxerror.syntaxlang.parser.node.WhileNode;
import at.syntaxerror.syntaxlang.parser.node.YieldNode;
import at.syntaxerror.syntaxlang.trace.Position;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * @author SyntaxError
 * 
 */
@RequiredArgsConstructor
public class Parser {

	@NonNull
	private final InputEnvironment input;
	@NonNull
	private final Tokens tokens;

	private Stack<Position> startPositions = new Stack<>();
	private Stack<Throwable> startPositionsTrace = new Stack<>();
	
	private ParseResult expectedCurrent(String what, boolean severe) {
		Token tok = tokens.current();
		return ParseResult.error("Expected " + what + ", got " + tok.toSimpleString() + " instead", tok, severe);
	}
	
	private ParseResult expected(String what, boolean severe) {
		Token tok = tokens.previous();
		return ParseResult.error("Expected " + what + ", got " + tok.toSimpleString() + " instead", tok, severe);
	}
	private ParseResult expected(String what) {
		return expected(what, false);
	}
	
	private void startPosition() {
		startPositions.add(tokens.current().getPosition());
		startPositionsTrace.add(new Throwable());
	}
	private void clearPosition() {
		startPositions.pop();
		startPositionsTrace.pop();
	}
	private Position getPosition() {
		Position old = startPositions.pop();
		Position cur = tokens.current().getPosition();
		
		return new Position(old.abs(), old.rel(), old.line(), cur.abs() - old.abs(), old.prevLine());
	}
	private Position getAndKeepPosition() {
		Position old = startPositions.peek();
		Position cur = tokens.current().getPosition();
		
		return new Position(old.abs(), old.rel(), old.line(), cur.abs() - old.abs(), old.prevLine());
	}
	
	private Node emptyNode() {
		EmptyNode empty = new EmptyNode();
		empty.setPosition(tokens.previous().getPosition());		
		return empty;
	}
	
	public StatementsNode makeNodes() {
		if(tokens.size() == 0) {
			StatementsNode statements = new StatementsNode(new ArrayList<>());
			statements.setPosition(new Position(0, 0, 0, 0, 0));
			return statements;
		}
		
		ParseResult res = makeMarked(this::makeMainStatements);
		
		if(tokens.isMarked()) {
			System.err.println("WARN: parser is still marked");
			tokens.purgeMarks();
		}
		
		if(!startPositions.isEmpty()) {
			System.err.println("WARN: position is still started");

			System.err.printf("purging %d positions:\n", startPositions.size());
			
			for(int i = 0; i < startPositions.size(); ++i) {
				System.err.printf("- #%s:\n", startPositions.pop());
				startPositionsTrace.pop().printStackTrace();
			}
		}
		
		if(res.isError()) {
			Position pos = res.getPosition();
			
			if(pos == null)
				input.terminate(
					"null",
					new Position(0, 0, 0, Integer.MAX_VALUE, 0)
				);
			else input.terminate(
				res.getMessage(),
				res.getPosition()
			);
			
			return null;
		}
		
		Token eof = tokens.current();
		
		if(!eof.is(TokenType.EOF)) {
			Position pos = eof.getPosition();
			
			input.terminate(
				"Expected EOF, got " + eof.toSimpleString() + " instead",
				pos
			);
		}
		
		StatementsNode nodes = (StatementsNode) res.getResult();
		
		try {
			nodes.errorCheck();
		} catch (Exception e) {
			input.terminate(
				e.getMessage(),
				new Position(0, 0, 0, Integer.MAX_VALUE, 0)
			);
		}
		
		return nodes;
	}
	
	private ParseResult makeMarked(Supplier<ParseResult> fn) {
		tokens.mark();
		startPosition();
		ParseResult res = fn.get();
		Position pos = getPosition();
		
		if(res.isError()) {
			tokens.reset();
			return res;
		} else {
			tokens.unmark();
			
			Node node = res.getResult();
			node.setPosition(pos);
			
			return ParseResult.success(node);
		}
	}
	
	// Statements
	
	private ParseResult makeMainStatements() {
		List<Node> statements = new ArrayList<>();
		
		while(true) {
			if(tokens.current().is(TokenType.EOF))
				break;
			
			ParseResult res = makeMarked(this::makeStatements);
			
			if(res.isError())
				return res;
			
			statements.add(res.getResult());
		}
		
		return ParseResult.success(new StatementsNode(statements));
	}

	private ParseResult makeStatements() {
		if(tokens.current().is(TokenType.LBRACE))
			return makeMarked(this::makeStatementsBlock);
		
		return makeMarked(this::makeStatement);
	}

	private ParseResult makeStatementsBlock() {
		if(!tokens.next().is(TokenType.LBRACE))
			return expected("'{'");

		List<Node> statements = new ArrayList<>();
		
		while(true) {
			if(tokens.current().is(TokenType.RBRACE))
				break;
			
			ParseResult res = makeMarked(this::makeStatements);
			
			if(res.isSevere())
				return res;
			
			if(res.isError())
				break;
			
			statements.add(res.getResult());
		}
		
		if(!tokens.next().is(TokenType.RBRACE))
			return expected("'}'", true);
		
		return ParseResult.success(new StatementsNode(statements));
	}
	
	private ParseResult makeStatement() {
		if(tokens.current().is(TokenType.SEMICOLON)) {
			tokens.next();
			return ParseResult.success(new EmptyNode());
		}
		
		ParseResult res = makeMarked(this::makeStatementPart);
		
		if(res.noError())
			return res;
		
		res = makeMarked(this::makeStatementPartSemi);
		
		if(!res.isError()) {
			if(!tokens.next().is(TokenType.SEMICOLON))
				return expectedCurrent("';'", true);
			
			return res;
		}
		
		if(res.isSevere())
			return res;
		
		return makeMarked(this::makeStatementPart);
	}
	
	private ParseResult makeStatementPartSemi() {
		Token tok = tokens.current();
		
		if(tok.getType() == TokenType.KEYWORD)
			switch(tok.getKeyword()) {
			case YIELD:
				tokens.next();
				ParseResult yieldval = makeMarked(this::makeExpr);
				
				if(yieldval.isError())
					return yieldval;
				
				return ParseResult.success(new YieldNode(yieldval.getResult()));
			case RETURN:
				tokens.next();
				ParseResult retval = makeMarked(this::makeExpr);
				
				if(retval.isSevere())
					return retval;
				
				if(retval.isError())
					return ParseResult.success(new ReturnNode(emptyNode()));
				
				return ParseResult.success(new ReturnNode(retval.getResult()));
			case BREAK:
				tokens.next();
				
				if(tokens.current().is(TokenType.IDENTIFIER))
					return ParseResult.success(new BreakNode(tokens.next()));
				
				return ParseResult.success(new BreakNode(null));
			case CONTINUE:
				tokens.next();
				
				if(tokens.current().is(TokenType.IDENTIFIER))
					return ParseResult.success(new ContinueNode(tokens.next()));
				
				return ParseResult.success(new ContinueNode(null));
			case NAMESPACE:
				tokens.next();
				
				if(!tokens.current().is(TokenType.IDENTIFIER))
					return expected("identifier", true);
				
				return ParseResult.success(new NamespaceNode(tokens.next()));
			case IMPORT:
				tokens.next();
				
				if(!tokens.current().is(TokenType.STRING))
					return expected("string", true);
				
				return ParseResult.success(new ImportNode(tokens.next()));
			case USE:
				tokens.next();
				
				Token namespace = tokens.next();
				
				if(!namespace.is(TokenType.IDENTIFIER))
					return expected("identifier", true);

				if(!tokens.next().is(Keyword.AS))
					return expected("keyword as", true);
				
				if(!tokens.current().is(TokenType.IDENTIFIER))
					return expected("identifier", true);
				
				return ParseResult.success(new UseAsNode(namespace, tokens.next()));
			default: break;
			}
		
		return makeMarked(this::makeExpr);
	}

	private final List<Supplier<ParseResult>> statementParts = Arrays.asList(
		this::makeTryCatchBlock,
		this::makeIfBlock,
		this::makeForBlock,
		this::makeForeachBlock,
		this::makeWhileBlock,
		this::makeDoWhileBlock,
		this::makeSwitchBlock,
		this::makeClass,
		this::makeEnum,
		this::makeInterface,
		this::makeFuncDef
	);
	
	private ParseResult makeStatementPart() {
		for(Supplier<ParseResult> part : statementParts) {
			ParseResult res = makeMarked(part);
			
			if(res.noError())
				return res;
		}
		
		return ParseResult.error("Invalid statement", tokens.current(), false);
	}
	
	private ParseResult makeStatementsBlockClass() {
		if(!tokens.next().is(TokenType.LBRACE))
			return expected("'{'");
		
		ParseResult res = makeMarked(this::makeStatementsBlockClassBody);
		
		if(res.isError())
			return res;
		
		if(!tokens.next().is(TokenType.RBRACE))
			return expected("'}'");
		
		return res;
	}

	private ParseResult makeStatementsBlockClassBody() {
		List<VarDeclNode> vars = new ArrayList<>();
		List<FuncDefNode> funs = new ArrayList<>();
		
		while(true) {
			ParseResult res = makeMarked(this::makeVarDecl);
			
			if(res.isSevere())
				return res;
			
			if(!res.isError()) {
				if(!tokens.next().is(TokenType.SEMICOLON))
					return expected("';'", true);
				
				vars.add((VarDeclNode) res.getResult());
				continue;
			}
			
			res = makeMarked(this::makeFuncDef);
			
			if(res.isSevere())
				return res;
			
			if(!res.isError()) {
				funs.add((FuncDefNode) res.getResult());
				continue;
			}
			
			break;
		}
		
		return ParseResult.success(new ClassBodyNode(vars, funs));
	}

	private ParseResult makeStatementsBlockFunc() {
		if(!tokens.next().is(TokenType.LBRACE))
			return expected("'{'");
		
		List<FuncDefNode> funs = new ArrayList<>();
		
		while(true) {
			ParseResult res = makeMarked(this::makeFuncDef);
			
			if(res.isSevere())
				return res;
			
			if(!res.isError()) {
				funs.add((FuncDefNode) res.getResult());
				continue;
			}
			
			break;
		}
		
		if(!tokens.next().is(TokenType.RBRACE))
			return expected("'}'");
		
		return ParseResult.success(new ClassBodyNode(new ArrayList<>(), funs));
	}
	
	// Blocks
	
	private ParseResult makeTryCatchBlock() {
		if(!tokens.next().is(Keyword.TRY))
			return expected("keyword try");
		
		ParseResult tryBody = makeMarked(this::makeStatementsBlock);
		
		if(tryBody.isError())
			return tryBody;
		
		if(!tokens.next().is(Keyword.CATCH))
			return expected("keyword catch", true);
		
		if(!tokens.next().is(TokenType.LPAREN))
			return expected("'('", true);
		
		Token var = tokens.next();
		
		if(!var.is(TokenType.IDENTIFIER))
			return expected("identifier", true);
		
		if(!tokens.next().is(TokenType.RPAREN))
			return expected("')'", true);
		
		ParseResult catchBody = makeMarked(this::makeStatementsBlock);
		
		if(catchBody.isError())
			return catchBody;
		
		return ParseResult.success(new TryCatchNode(
			(StatementsNode) tryBody.getResult(),
			var,
			(StatementsNode) catchBody.getResult()
		));
	}
	
	private ParseResult makeIfBlock() {
		if(!tokens.next().is(Keyword.IF))
			return expected("keyword if");

		List<IfBodyNode> ifBodies = new ArrayList<>();
		
		ParseResult body = makeIfBody();
		
		if(body.isError())
			return body;
		
		ifBodies.add((IfBodyNode) body.getResult());
		
		while(true) {
			if(!tokens.current().is(Keyword.ELIF, Keyword.ELSEIF))
				break;
			tokens.next();
			
			body = makeIfBody();
			
			if(body.isError())
				return body.severe();
			
			ifBodies.add((IfBodyNode) body.getResult());
		}
		
		Node elseBody;
		
		if(tokens.current().is(Keyword.ELSE)) {
			tokens.next();
			body = makeMarked(this::makeStatements);
			
			if(body.isError())
				return body.severe();
			
			elseBody = body.getResult();
		} else elseBody = emptyNode();
		
		return ParseResult.success(new IfNode(ifBodies, elseBody));
	}
	
	private ParseResult makeIfBody() {
		if(!tokens.next().is(TokenType.LPAREN))
			return expected("'('", true);

		ParseResult condition = makeMarked(this::makeExpr);
		
		if(condition.isError())
			return condition.severe();
		
		if(!tokens.next().is(TokenType.RPAREN))
			return expected("')'", true);
		
		ParseResult body = makeMarked(this::makeStatements);
		
		if(body.isError())
			return body.severe();
		
		return ParseResult.success(new IfBodyNode(condition.getResult(), body.getResult()));
	}
	
	private Token getLabel() {
		tokens.mark();
		Token label = tokens.next();
		
		if(label.is(TokenType.IDENTIFIER) && tokens.next().is(TokenType.COLON)) {
			tokens.unmark();
			return label;
		}
		
		tokens.reset();
		return null;
	}
	
	private ParseResult makeForBlock() {
		Token label = getLabel();
		
		if(!tokens.next().is(Keyword.FOR))
			return expected("keyword for");

		if(!tokens.next().is(TokenType.LPAREN))
			return expected("'('", true);
		
		List<VarDeclNode> init = new ArrayList<>();

		ParseResult res = makeMarked(this::makeVarDecl);
		
		if(!res.isError()) {
			init.add((VarDeclNode) res.getResult());
		
			while(true) {
				if(!tokens.current().is(TokenType.COMMA))
					break;
				tokens.next();
				
				res = makeMarked(this::makeVarDecl);
				
				if(res.isError())
					return res.severe();
				
				init.add((VarDeclNode) res.getResult());
			}
		
		}

		if(!tokens.next().is(TokenType.SEMICOLON))
			return expected("';'", true);

		res = makeMarked(this::makeExpr);
		
		if(res.isError())
			return res.severe();
		
		Node condition = res.getResult();

		if(!tokens.next().is(TokenType.SEMICOLON))
			return expected("';'", true);
		
		List<Node> update = new ArrayList<>();
		
		res = makeMarked(this::makeExpr);
		
		if(!res.isError()) {
			update.add(res.getResult());
		
			while(true) {
				if(!tokens.current().is(TokenType.COMMA))
					break;
				tokens.next();
				
				res = makeMarked(this::makeExpr);
				
				if(res.isError())
					return res.severe();
				
				update.add(res.getResult());
			}
		
		}
		
		if(!tokens.next().is(TokenType.RPAREN))
			return expected("')'", true);
		
		ParseResult body = makeMarked(this::makeStatements);
		
		if(body.isError())
			return body.severe();
		
		return ParseResult.success(new ForNode(label, init, condition, update, body.getResult()));
	}
	
	private ParseResult makeForeachBlock() {
		Token label = getLabel();
		
		if(!tokens.next().is(Keyword.FOREACH))
			return expected("keyword foreach");

		if(!tokens.next().is(TokenType.LPAREN))
			return expected("'('", true);

		Token var = tokens.next();

		if(!var.is(TokenType.IDENTIFIER))
			return expected("identifier", true);
		
		if(!tokens.next().is(Keyword.IN))
			return expected("keyword in", true);
		
		ParseResult value = makeMarked(this::makeExpr);
		
		if(value.isError())
			return value.severe();
		
		if(!tokens.next().is(TokenType.RPAREN))
			return expected("')'", true);
		
		ParseResult body = makeMarked(this::makeStatements);
		
		if(body.isError())
			return body.severe();
		
		return ParseResult.success(new ForeachNode(label, var, value.getResult(), body.getResult()));
	}
	
	private ParseResult makeWhileBlock() {
		Token label = getLabel();
		
		if(!tokens.next().is(Keyword.WHILE))
			return expected("keyword while");

		if(!tokens.next().is(TokenType.LPAREN))
			return expected("'('", true);

		ParseResult condition = makeMarked(this::makeExpr);
		
		if(condition.isError())
			return condition.severe();
		
		if(!tokens.next().is(TokenType.RPAREN))
			return expected("')'", true);
		
		ParseResult body = makeMarked(this::makeStatements);
		
		if(body.isError())
			return body.severe();
		
		return ParseResult.success(new WhileNode(label, condition.getResult(), body.getResult()));
	}
	
	private ParseResult makeDoWhileBlock() {
		Token label = getLabel();
		
		if(!tokens.next().is(Keyword.DO))
			return expected("keyword do");
		
		ParseResult body = makeMarked(this::makeStatementsBlock);
		
		if(body.isError())
			return body.severe();
		
		if(!tokens.next().is(Keyword.WHILE))
			return expected("keyword while");

		if(!tokens.next().is(TokenType.LPAREN))
			return expected("'('", true);

		ParseResult condition = makeMarked(this::makeExpr);
		
		if(condition.isError())
			return condition.severe();
		
		if(!tokens.next().is(TokenType.RPAREN))
			return expected("')'", true);
		
		return ParseResult.success(new DoWhileNode(label, condition.getResult(), body.getResult()));
	}
	
	private ParseResult getSwitch(Supplier<ParseResult> switchCaseSupplier) {
		if(!tokens.next().is(Keyword.SWITCH))
			return expected("keyword switch");

		if(!tokens.next().is(TokenType.LPAREN))
			return expected("'('", true);

		ParseResult value = makeMarked(this::makeExpr);
		
		if(value.isError())
			return value.severe();
		
		if(!tokens.next().is(TokenType.RPAREN))
			return expected("')'", true);
		
		if(!tokens.next().is(TokenType.LBRACE))
			return expected("'{'", true);
		
		List<SwitchCaseNode> body = new ArrayList<>();
		
		while(true) {
			if(tokens.current().is(TokenType.RBRACE))
				break;
			
			ParseResult switchCase = makeMarked(switchCaseSupplier);
			
			if(switchCase.isError())
				return switchCase;
			
			body.add((SwitchCaseNode) switchCase.getResult());
		}
		
		if(!tokens.next().is(TokenType.RBRACE))
			return expected("'}'", true);
		
		return ParseResult.success(new SwitchNode(value.getResult(), body));
	}
	
	private ParseResult makeSwitchBlock() {
		return getSwitch(this::makeSwitchCase);
	}
	private ParseResult makeSwitchValueBlock() {
		return getSwitch(this::makeSwitchValueCase);
	}
	
	private ParseResult getSwitchCaseHead() {
		if(!tokens.next().is(Keyword.CASE, Keyword.DEFAULT))
			return expected("keyword case or keyword default");

		if(tokens.previous().is(Keyword.CASE))
			return makeMarked(this::makeExpr);
		
		return ParseResult.success(emptyNode());
	}
	
	private ParseResult makeSwitchCase() {
		ParseResult value = getSwitchCaseHead();
		
		if(value.isError())
			return value.severe();
		
		if(!tokens.next().is(TokenType.COLON))
			return expected("':'", true);

		List<Node> statements = new ArrayList<>();
		
		while(true) {
			if(tokens.current().is(TokenType.RBRACE) || tokens.current().is(Keyword.CASE, Keyword.DEFAULT))
				break;
			
			ParseResult stmt = makeMarked(this::makeStatement);
			
			if(stmt.isError())
				return stmt.severe();
			
			statements.add(stmt.getResult());
		}
		
		return ParseResult.success(new SwitchCaseNode(value.getResult(), new StatementsNode(statements), false));
	}
	private ParseResult makeSwitchValueCase() {
		ParseResult value = getSwitchCaseHead();
		
		if(value.isError())
			return value.severe();
		
		if(!tokens.next().is(TokenType.SINGLE_ARROW))
			return expected("'->'");

		ParseResult res = makeMarked(this::makeExpr);
		
		if(res.isError())
			return res.severe();
		
		if(!tokens.next().is(TokenType.SEMICOLON))
			return expected("';'", true);
		
		return ParseResult.success(new SwitchCaseNode(value.getResult(), res.getResult(), true));
	}
	
	// Variables
	
	private ParseResult makeVarDecl() {
		Token modAccess = tokens.current();
		
		if(!modAccess.is(Keyword.PUBLIC, Keyword.PRIVATE, Keyword.PROTECTED))
			modAccess = null;
		else tokens.next();
		
		Token modStatic = tokens.current();
		
		if(!modStatic.is(Keyword.STATIC))
			modStatic = null;
		else tokens.next();
		
		Token modFinal = tokens.current();
		
		if(!modFinal.is(Keyword.FINAL))
			modFinal = null;
		else tokens.next();
		
		Token name = tokens.next();
		
		if(!name.is(TokenType.IDENTIFIER))
			return expected("identifier");
		
		if(!tokens.next().is(TokenType.ASSIGN))
			return expected("'='");
		
		ParseResult res = makeMarked(this::makeExpr);
		
		if(res.isError())
			return res.severe();
		
		return ParseResult.success(new VarDeclNode(modAccess, modStatic, modFinal, name, res.getResult()));
	}

	private ParseResult makeVarMod() {
		Token name = tokens.next();
		
		if(!name.is(TokenType.IDENTIFIER))
			return expected("identifier");
		
		Token op = tokens.next();
		
		if(!op.is(TokenType.ASSIGN_ADD, TokenType.ASSIGN_SUB, TokenType.ASSIGN_MUL, TokenType.ASSIGN_DIV, 
							TokenType.ASSIGN_MOD, TokenType.ASSIGN_POW, TokenType.ASSIGN_LSH, TokenType.ASSIGN_RSH, 
							TokenType.ASSIGN_AND, TokenType.ASSIGN_XOR, TokenType.ASSIGN_OR))
			return expected("assignment operator");
		
		ParseResult res = makeMarked(this::makeExpr);
		
		if(res.isError())
			return res;
		
		return ParseResult.success(new VarModNode(name, op, res.getResult()));
	}
	
	// Functions
	
	private ParseResult makeFuncDef() {
		Token modAccess = tokens.current();
		
		if(!modAccess.is(Keyword.PUBLIC, Keyword.PRIVATE, Keyword.PROTECTED))
			modAccess = null;
		else tokens.next();
		
		Token modStatic = tokens.current();
		
		if(!modStatic.is(Keyword.STATIC))
			modStatic = null;
		else tokens.next();
		
		Token modAbstractFinalDefault = tokens.current();
		
		if(!modAbstractFinalDefault.is(Keyword.FINAL, Keyword.ABSTRACT, Keyword.DEFAULT))
			modAbstractFinalDefault = null;
		else tokens.next();
		
		if(!tokens.next().is(Keyword.FUN))
			return expected("keyword fun");
		
		Token name = tokens.next();
		
		if(!name.is(TokenType.IDENTIFIER))
			return expected("identifier", true);
		
		if(!tokens.next().is(TokenType.LPAREN))
			return expected("'('", true);

		ParseResult args = makeMarked(this::makeFuncArgs);
		
		if(args.isError())
			return args.severe();
		
		if(!tokens.next().is(TokenType.RPAREN))
			return expected("')'", true);
		
		boolean hasBody;
		StatementsNode bodyNode;
		
		if(tokens.current().is(TokenType.SEMICOLON)) {
			hasBody = false;
			bodyNode = new StatementsNode(new ArrayList<>());
			bodyNode.setPosition(tokens.current().getPosition());
			
			tokens.next();
		} else {
			ParseResult body = makeMarked(this::makeStatementsBlock);
			
			if(body.isError())
				return body.severe();
			
			hasBody = true;
			bodyNode = (StatementsNode) body.getResult();
		}
		
		return ParseResult.success(new FuncDefNode(
			modAccess,
			modStatic,
			modAbstractFinalDefault,
			name,
			(FuncArgsNode) args.getResult(),
			bodyNode,
			hasBody
		));
	}
	
	private ParseResult makeFuncArgs() {
		List<FuncArgsPartNode> args = new ArrayList<>();
		
		ParseResult res = makeMarked(this::makeFuncArgsPart);
		
		if(!res.isError()) {
			args.add((FuncArgsPartNode) res.getResult());
			
			while(true) {
				if(!tokens.current().is(TokenType.COMMA))
					break;
				
				tokens.next();
				
				if(tokens.current().is(TokenType.VARARGS))
					break;
				
				res = makeMarked(this::makeFuncArgsPart);
				
				if(res.isError())
					return res;
				
				args.add((FuncArgsPartNode) res.getResult());
			}
		}
		
		Token varargs;
		
		if(tokens.current().is(TokenType.VARARGS)) {
			tokens.next();
			
			varargs = tokens.next();
			
			if(!varargs.is(TokenType.IDENTIFIER))
				return expected("identifier", true);
		} else varargs = null;
		
		return ParseResult.success(new FuncArgsNode(args, varargs));
	}

	private ParseResult makeFuncArgsPart() {
		Token name = tokens.next();
		
		if(!name.is(TokenType.IDENTIFIER))
			return expected("identifier");
		
		tokens.mark();
		
		Node value;
		
		if(tokens.next().is(TokenType.ASSIGN)) {
			tokens.unmark();
			
			ParseResult res = makeMarked(this::makeExpr);
			
			if(res.noError())
				return res;
			
			value = res.getResult();
		} else {
			tokens.reset();
			value = emptyNode();
		}
		
		return ParseResult.success(new FuncArgsPartNode(name, value));
	}
	
	private ParseResult makeFuncCallArgs() {
		List<Node> args = new ArrayList<>();
		
		ParseResult res = makeMarked(this::makeExpr);
		
		if(!res.isError()) {
			args.add(res.getResult());
			
			while(true) {
				if(!tokens.current().is(TokenType.COMMA))
					break;
				
				tokens.next();
				
				res = makeMarked(this::makeExpr);
				
				if(res.isError())
					return res;
				
				args.add(res.getResult());
			}
		}
		
		return ParseResult.success(new FuncCallArgsNode(args));
	}
	
	private ParseResult makeFuncAnonymous() {
		ParseResult res = makeMarked(this::makeFuncAnonymous1);
		
		if(res.noError())
			return res;
		
		return makeMarked(this::makeFuncAnonymous2);
	}

	private ParseResult makeFuncAnonymous1() {
		if(!tokens.next().is(Keyword.FUN))
			return expected("keyword fun");
		
		if(!tokens.next().is(TokenType.LPAREN))
			return expected("'('");

		ParseResult args = makeMarked(this::makeFuncArgs);
		
		if(args.isError())
			return args;
		
		if(!tokens.next().is(TokenType.RPAREN))
			return expected("')'", true);
		
		ParseResult body = makeMarked(this::makeStatementsBlock);
		
		if(body.isError())
			return body;
		
		return ParseResult.success(new AnonymousFuncNode(
			(FuncArgsNode) args.getResult(),
			(StatementsNode) body.getResult()
		));
	}
	private ParseResult makeFuncAnonymous2() {
		FuncArgsNode args;
		
		if(tokens.current().is(TokenType.IDENTIFIER)) {
			Token tok = tokens.next();
			Position pos = tok.getPosition();
			
			Node empty = emptyNode();
			
			FuncArgsPartNode part = new FuncArgsPartNode(tok, empty);
			part.setPosition(pos);
			
			args = new FuncArgsNode(Arrays.asList(
				part
			), null);
			args.setPosition(pos);
		} else {
			if(!tokens.next().is(TokenType.LPAREN))
				return expected("'('");

			ParseResult res = makeMarked(this::makeFuncArgs);
			
			if(res.isError())
				return res;
			
			if(!tokens.next().is(TokenType.RPAREN))
				return expected("')'");
			
			args = (FuncArgsNode) res.getResult();
		}
		
		if(!tokens.next().is(TokenType.DOUBLE_ARROW))
			return expected("'=>'", true);
		
		StatementsNode body;
		
		startPosition();
		ParseResult res = makeMarked(this::makeExpr);
		
		if(res.isError()) {
			res = makeMarked(this::makeStatementsBlock);
			
			if(res.isError()) {
				clearPosition();
				return res;
			}
			
			body = (StatementsNode) res.getResult();
		} else {
			ReturnNode ret = new ReturnNode(res.getResult());
			ret.setPosition(getAndKeepPosition());
			
			body = new StatementsNode(Arrays.asList(ret));
		}
		
		body.setPosition(getPosition());
		
		return ParseResult.success(new AnonymousFuncNode(args, body));
	}
	
	// Class
	
	private ParseResult getScopedName() {
		Token name = tokens.next();
		
		if(!name.is(TokenType.IDENTIFIER))
			return expected("identifier");
		
		if(tokens.current().is(TokenType.SCOPE)) {
			tokens.next();
			
			Token className = tokens.next();

			if(!className.is(TokenType.IDENTIFIER))
				return expected("identifier");
			
			return ParseResult.success(new ScopedNode(name, new LiteralNode(className)));
		}
		
		return ParseResult.success(new LiteralNode(name));
	}
	
	private ParseResult makeClass() {
		Token modAccess = tokens.current();
		
		if(!modAccess.is(Keyword.PUBLIC, Keyword.PRIVATE, Keyword.PROTECTED))
			modAccess = null;
		else tokens.next();
		
		Token modCloneable = tokens.current();
		
		if(!modCloneable.is(Keyword.CLONEABLE))
			modCloneable = null;
		else tokens.next();
		
		Token modAbstractFinal = tokens.current();
		
		if(!modAbstractFinal.is(Keyword.FINAL, Keyword.ABSTRACT))
			modAbstractFinal = null;
		else tokens.next();
		
		if(!tokens.next().is(Keyword.CLASS))
			return expected("keyword class");
		
		Token name = tokens.next();
		
		if(!name.is(TokenType.IDENTIFIER))
			return expected("identifier", true);
		
		Node extendedClass;
		
		if(tokens.current().is(Keyword.EXTENDS)) {
			tokens.next();
			
			ParseResult extended = getScopedName();
			
			if(extended.isError())
				return extended.severe();
			
			extendedClass = extended.getResult();
		} else extendedClass = emptyNode();
		
		List<Node> implementedClasses = new ArrayList<>();
		
		if(tokens.current().is(Keyword.IMPLEMENTS)) {
			tokens.next();
			
			while(true) {
				ParseResult implemented = getScopedName();

				if(implemented.isError())
					return implemented.severe();
				
				implementedClasses.add(implemented.getResult());
				
				if(!tokens.current().is(TokenType.COMMA))
					break;

				tokens.next();
			}
		}
		
		ParseResult body = makeMarked(this::makeStatementsBlockClass);
		
		if(body.isError())
			return body.severe();
		
		return ParseResult.success(new ClassNode(
			modAccess,
			modCloneable,
			modAbstractFinal,
			name,
			extendedClass,
			implementedClasses,
			(ClassBodyNode) body.getResult()
		));
	}
	
	// Enum

	private ParseResult makeEnum() {
		Token modAccess = tokens.current();
		
		if(!modAccess.is(Keyword.PUBLIC, Keyword.PRIVATE, Keyword.PROTECTED))
			modAccess = null;
		else tokens.next();
		
		if(!tokens.next().is(Keyword.ENUM))
			return expected("keyword enum");
		
		Token name = tokens.next();
		
		if(!name.is(TokenType.IDENTIFIER))
			return expected("identifier", true);
		
		List<Node> implementedClasses = new ArrayList<>();
		
		if(tokens.current().is(Keyword.IMPLEMENTS)) {
			tokens.next();
			
			while(true) {
				ParseResult implemented = getScopedName();

				if(implemented.isError())
					return implemented.severe();
				
				implementedClasses.add(implemented.getResult());
				
				if(!tokens.current().is(TokenType.COMMA))
					break;
				
				tokens.next();
			}
		}
		
		if(!tokens.next().is(TokenType.LBRACE))
			return expected("'{'", true);
		
		List<EnumConstantNode> constants = new ArrayList<>();

		if(!tokens.current().is(TokenType.SEMICOLON))
			while(true) {
				startPosition();
				Token constantName = tokens.next();
	
				if(!constantName.is(TokenType.IDENTIFIER))
					return expected("identifier", true);
				
				FuncCallArgsNode constantArgs;
				
				startPosition();
				if(tokens.current().is(TokenType.LPAREN)) {
					tokens.next();
					
					ParseResult res = makeMarked(this::makeFuncCallArgs);
					
					if(res.isError()) {
						getPosition();
						return res.severe();
					}
					
					if(!tokens.next().is(TokenType.RPAREN)) {
						getPosition();
						return expected("')'", true);
					}
	
					constantArgs = (FuncCallArgsNode) res.getResult();
				} else constantArgs = new FuncCallArgsNode(new ArrayList<>());
				
				constantArgs.setPosition(getPosition());
				
				EnumConstantNode node = new EnumConstantNode(constantName, constantArgs);
				node.setPosition(getPosition());
				constants.add(node);
				
				if(!tokens.current().is(TokenType.COMMA))
					break;
				
				tokens.next();
			}
		
		if(!tokens.next().is(TokenType.SEMICOLON))
			return expected("';'", true);
		
		ParseResult body = makeMarked(this::makeStatementsBlockClassBody);
		
		if(body.isError())
			return body.severe();

		if(!tokens.next().is(TokenType.RBRACE))
			return expected("'}'", true);
		
		return ParseResult.success(new EnumNode(
			modAccess,
			name,
			implementedClasses,
			constants,
			(ClassBodyNode) body.getResult()
		));
	}

	// Interface
	
	private ParseResult makeInterface() {
		Token modAccess = tokens.current();
		
		if(!modAccess.is(Keyword.PUBLIC, Keyword.PRIVATE, Keyword.PROTECTED))
			modAccess = null;
		else tokens.next();
		
		if(!tokens.next().is(Keyword.INTERFACE))
			return expected("keyword interface");
		
		Token name = tokens.next();
		
		if(!name.is(TokenType.IDENTIFIER))
			return expected("identifier", true);
		
		List<Node> extendedClasses = new ArrayList<>();
		
		if(tokens.current().is(Keyword.EXTENDS)) {
			tokens.next();
			
			while(true) {
				ParseResult extended = getScopedName();

				if(extended.isError())
					return extended.severe();
				
				extendedClasses.add(extended.getResult());
				
				if(!tokens.current().is(TokenType.COMMA))
					break;
				
				tokens.next();
			}
		}
		
		ParseResult body = makeMarked(this::makeStatementsBlockFunc);
		
		if(body.isError())
			return body.severe();
		
		return ParseResult.success(new InterfaceNode(
			modAccess,
			name,
			extendedClasses,
			(ClassBodyNode) body.getResult()
		));
	}
	
	// Math

	private ParseResult makeExpr() {
		Token tok = tokens.current();
		
		if(tok.getKeyword() == Keyword.THROW) {
			tokens.next();
			ParseResult res = makeMarked(this::makeExpr);
			
			if(res.isError())
				return res;
			
			return ParseResult.success(new ThrowNode(res.getResult()));
		}
		
		ParseResult math = makeMarked(this::makeVarDecl);
		
		if(math.noError())
			return math;
		
		math = makeMarked(this::makeVarMod);
		
		if(math.noError())
			return math;
				
		math = makeMarked(this::makeMath1);
		
		if(math.isError())
			return math;
		
		tokens.mark();
		if(!tokens.next().is(TokenType.QUESTION)) {
			tokens.reset();
			return math;
		}
		
		tokens.unmark();
		
		ParseResult ternaryThen = makeMarked(this::makeExpr);
		
		if(ternaryThen.isError())
			return ternaryThen;
		
		if(!tokens.next().is(TokenType.COLON))
			return expected("':'", true);
		
		ParseResult ternaryElse = makeMarked(this::makeMath1);
		
		if(ternaryElse.isError())
			return ternaryThen;
		
		return ParseResult.success(new TernaryOpNode(
			math.getResult(),
			ternaryThen.getResult(),
			ternaryElse.getResult()
		));
	}

	private ParseResult makeMath1() {
		return binOpHelper(this::makeMath2, TokenType.OR);
	}
	private ParseResult makeMath2() {
		return binOpHelper(this::makeMath3, TokenType.AND);
	}
	private ParseResult makeMath3() {
		return binOpHelper(this::makeMath4, TokenType.BITOR);
	}
	private ParseResult makeMath4() {
		return binOpHelper(this::makeMath5, TokenType.XOR);
	}
	private ParseResult makeMath5() {
		return binOpHelper(this::makeMath6, TokenType.BITAND);
	}
	private ParseResult makeMath6() {
		return binOpHelper(this::makeMath7, TokenType.EQUAL, TokenType.NOT_EQUAL, TokenType.IDENTICAL, TokenType.NOT_IDENTICAL);
	}
	private ParseResult makeMath7() {
		return binOpHelper(this::makeMath8, TokenType.LESS, TokenType.GREATER, TokenType.LESS_EQUAL, TokenType.GREATER_EQUAL);
	}
	private ParseResult makeMath8() {
		ParseResult math = makeMarked(this::makeMath9);
		
		if(math.isError())
			return math;
		
		tokens.mark();
		if(!tokens.next().is(Keyword.INSTANCEOF)) {
			tokens.reset();
			return math;
		}
		
		tokens.unmark();

		Token type = tokens.next();
		
		if(!type.is(TokenType.IDENTIFIER))
			return expected("identifier", true);
		
		return ParseResult.success(new InstanceofNode(math.getResult(), type));
	}
	private ParseResult makeMath9() {
		return binOpHelper(this::makeMath10, TokenType.LSHIFT, TokenType.RSHIFT);
	}
	private ParseResult makeMath10() {
		return binOpHelper(this::makeMath11, TokenType.PLUS, TokenType.MINUS);
	}
	private ParseResult makeMath11() {
		return binOpHelper(this::makeMath12, TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.MODULO, TokenType.POWER);
	}
	private ParseResult makeMath12() {
		tokens.mark();
		Token op = tokens.next();
		
		if(op.is(Keyword.NEW, Keyword.CLONE)) {
			tokens.unmark();
			ParseResult math = makeMarked(this::makeMath13);
			
			if(math.isError())
				return math;
			
			Node target = math.getResult();
			
			if(op.is(Keyword.NEW))
				return ParseResult.success(new NewNode(target));
			
			return ParseResult.success(new CloneNode(target));
		}
		
		if(!op.is(TokenType.INCREMENT, TokenType.DECREMENT, TokenType.PLUS, TokenType.MINUS, TokenType.NOT, TokenType.COMPLEMENT)) {
			tokens.reset();
			op = null;
		} else tokens.unmark();
		
		ParseResult math = makeMarked(this::makeMath13);
		
		if(math.isError())
			return math;
		
		Node target = math.getResult();

		return ParseResult.success(op == null ? target : new UnaryOpNode(true, op, target));
	}
	private ParseResult makeMath13() {
		startPosition();
		ParseResult math = makeMarked(this::makeMath14);
		
		if(math.isError()) {
			clearPosition();
			return math;
		}
		
		Node node = math.getResult();

		tokens.mark();
		
		while(true) {
			tokens.unmark();
			tokens.mark();
			Token op = tokens.next();
			
			if(op.is(TokenType.LPAREN)) {
				ParseResult args = makeMarked(this::makeFuncCallArgs);
				
				if(!args.isError() && tokens.next().is(TokenType.RPAREN)) {
					node = new FuncCallNode(node, (FuncCallArgsNode) args.getResult());
					node.setPosition(getAndKeepPosition());
					continue;
				}
				
				if(args.isSevere()) {
					clearPosition();
					return args;
				}
			} else if(op.is(TokenType.LBRACKET)) {
				ParseResult args = makeMarked(this::makeExpr);
				
				if(!args.isError() && tokens.next().is(TokenType.RBRACKET)) {
					node = new ArrayAccessNode(node, args.getResult());
					node.setPosition(getAndKeepPosition());
					continue;
				}
				
				if(args.isSevere()) {
					clearPosition();
					return args;
				}
			} else if(op.is(TokenType.PERIOD)) {
				Token member = tokens.next();
				
				if(member.is(TokenType.IDENTIFIER)) {
					node = new MemberAccessNode(node, member);
					node.setPosition(getAndKeepPosition());
					continue;
				}
			} else if(op.is(TokenType.INCREMENT, TokenType.DECREMENT)) {
				node = new UnaryOpNode(false, op, node);
				node.setPosition(getAndKeepPosition());
				continue;
			}

			tokens.reset();
			break;
		}
		clearPosition();
		
		return ParseResult.success(node);
	}
	private ParseResult makeMath14() {
		tokens.mark();

		Token scope = tokens.next();
		
		if(scope.is(TokenType.IDENTIFIER) && tokens.next().is(TokenType.SCOPE))
			tokens.unmark();
		else {
			tokens.reset();
			scope = null;
		}
		
		ParseResult math = makeMarked(this::makeMath15);
		
		if(math.isError())
			return math;
		
		Node target = math.getResult();

		return ParseResult.success(scope == null ? target : new ScopedNode(scope, target));
	}
	private ParseResult makeMath15() {
		Token tok = tokens.current();
		
		if(tok.isLiteral()) {
			tokens.next();
			return ParseResult.success(new LiteralNode(tok));
		}
		
		ParseResult res = makeMarked(this::makeList);
		
		if(res.noError())
			return res;
		
		res = makeMarked(this::makeMap);
		
		if(res.noError())
			return res;
		
		res = makeMarked(this::makeFuncAnonymous);
		
		if(res.noError())
			return res;
		
		res = makeMarked(this::makeSwitchValueBlock);
		
		if(res.noError())
			return res;
		
		res = makeMarked(this::makeSwitchBlock);
		
		if(res.noError())
			return res;
		
		if(tok.is(TokenType.LPAREN)) {
			tokens.next();
			res = makeMarked(this::makeExpr);
			
			if(res.isError())
				return res;
			
			if(!tokens.next().is(TokenType.RPAREN))
				return expected("')'", true);
			
			return ParseResult.success(new ParenthesizedNode(res.getResult()));
		}
		
		return ParseResult.error("invalid expression", tok, false);
	}
	
	private ParseResult binOpHelper(Supplier<ParseResult> next, TokenType... types) {
		startPosition();
		ParseResult res = makeMarked(next);
		
		if(res.isError()) {
			clearPosition();
			return res;
		}
		
		Node node = res.getResult();
		List<TokenType> typeArray = Arrays.asList(types);
		
		while(true) {
			tokens.mark();
			Token op = tokens.next();
			
			if(!typeArray.contains(op.getType())) {
				tokens.reset();
				break;
			}
			
			tokens.unmark();
			res = makeMarked(next);
			
			if(res.isError()) {
				clearPosition();
				return res;
			}
			
			node = new BinaryOpNode(node, op, res.getResult());
			node.setPosition(getAndKeepPosition());
		}

		node.setPosition(getAndKeepPosition());
		
		clearPosition();
		return ParseResult.success(node);
	}
	
	// Lists & maps

	private ParseResult makeList() {
		if(!tokens.next().is(TokenType.LBRACKET))
			return expected("'[");
		
		ParseResult res = makeMarked(this::makeFuncCallArgs);
		
		if(res.isError())
			return res;

		if(!tokens.next().is(TokenType.RBRACKET))
			return expected("']", true);
		
		return ParseResult.success(new ListNode(((FuncCallArgsNode) res.getResult()).getArgs()));
	}

	private ParseResult makeMap() {
		if(!tokens.next().is(TokenType.LBRACE))
			return expected("'{");
		
		List<MapPartNode> map = new ArrayList<>();

		ParseResult res = makeMarked(this::makeMapPart);
		
		if(!res.isError()) {
			map.add((MapPartNode) res.getResult());
			
			while(true) {
				if(!tokens.current().is(TokenType.COMMA))
					break;
				
				tokens.next();
				
				res = makeMarked(this::makeMapPart);
				
				if(res.isError())
					return res.severe();
				
				map.add((MapPartNode) res.getResult());
			}
		}
		
		if(!tokens.next().is(TokenType.RBRACE))
			return expected("'}", true);
		
		return ParseResult.success(new MapNode(map));
	}
	
	private ParseResult makeMapPart() {
		Token name = tokens.next();
		
		if(!name.isLiteral())
			return expected("literal");
		
		if(name.is(Keyword.NULL))
			return ParseResult.error("Map key is null", name, true);
		
		if(!tokens.next().is(TokenType.COLON))
			return expected("':'", true);
		
		ParseResult res = makeMarked(this::makeExpr);
		
		if(res.isError())
			return res;
		
		return ParseResult.success(new MapPartNode(name, res.getResult()));
	}
	
}
