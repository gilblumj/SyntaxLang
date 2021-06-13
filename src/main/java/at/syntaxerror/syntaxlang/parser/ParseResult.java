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

import at.syntaxerror.syntaxlang.lexer.Token;
import at.syntaxerror.syntaxlang.parser.node.Node;
import at.syntaxerror.syntaxlang.trace.Position;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * @author SyntaxError
 * 
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public class ParseResult {

	private Throwable trace;
	
	private Node result;
	
	private String message;
	private Position position;
	
	private boolean severe;
	
	public boolean isError() {
		return result == null;
	}
	public boolean isSevere0() {
		return severe;
	}
	public boolean noError() {
		return !isError() || isSevere0();
	}
	
	public ParseResult severe() {
		severe = true;
		return this;
	}
	
	public static ParseResult error(String message, Position position, boolean severe) {
		return new ParseResult(new Throwable(), null, message, position, severe);
	}
	public static ParseResult error(String message, Token tok, boolean severe) {
		return new ParseResult(new Throwable(), null, message, tok.getPosition(), severe);
	}
	
	public static ParseResult success(Node result) {
		return new ParseResult(new Throwable(), result, null, result.getPosition(), false);
	}

}
