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
package at.syntaxerror.syntaxlang.lexer;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * @author SyntaxError
 * 
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum TokenType {
	
	IDENTIFIER		("Identifier"),
	STRING			("String"),
	NUMBER			("Number"),
	COMPLEX			("Complex"),
	
	KEYWORD			("Keyword"),
	EOF				("EOF"),
	
	LBRACE			("{"),
	RBRACE			("}"),
	LBRACKET		("["),
	RBRACKET		("]"),
	LPAREN			("("),
	RPAREN			(")"),
	
	SEMICOLON		(";"),
	COMMA			(","),
	COLON			(":"),
	PERIOD			("."),
	
	SINGLE_ARROW	("->"),
	DOUBLE_ARROW	("=>"),
	
	ASSIGN			("="),
	ASSIGN_ADD		("+="),
	ASSIGN_SUB		("-="),
	ASSIGN_MUL		("*="),
	ASSIGN_DIV		("/="),
	ASSIGN_MOD		("%="),
	ASSIGN_POW		("**="),
	ASSIGN_LSH		("<<="),
	ASSIGN_RSH		(">>="),
	ASSIGN_AND		("&="),
	ASSIGN_XOR		("^="),
	ASSIGN_OR		("|="),
	
	VARARGS			("..."),
	
	QUESTION		("?"),
	
	OR				("||"),
	AND				("&&"),
	
	BITOR			("|"),
	XOR				("^"),
	BITAND			("&"),
	
	EQUAL			("=="),
	IDENTICAL		("==="),
	NOT_EQUAL		("!="),
	NOT_IDENTICAL	("!=="),
	
	LESS			("<"),
	GREATER			(">"),
	LESS_EQUAL		("<="),
	GREATER_EQUAL	(">="),
	
	LSHIFT			("<<"),
	RSHIFT			(">>"),
	
	PLUS			("+"),
	MINUS			("-"),
	MULTIPLY		("*"),
	DIVIDE			("/"),
	MODULO			("%"),
	POWER			("**"),
	INCREMENT		("++"),
	DECREMENT		("--"),
	
	NOT				("!"),
	COMPLEMENT		("~"),
	
	SCOPE			("\\");
	
	private final String strrep;
	
	@Override
	public String toString() {
		return strrep;
	}
	
}