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

import java.math.BigDecimal;

import at.syntaxerror.syntaxlang.SyntaxLangException;
import at.syntaxerror.syntaxlang.string.WideString;
import at.syntaxerror.syntaxlang.trace.Position;
import ch.obermuhlner.math.big.BigComplex;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * @author SyntaxError
 * 
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class Token {

	@NonNull
	@Setter
	private Position position = null;
	
	@NonNull
	private final TokenType type;
	
	private final Object value;
	private final Keyword keyword;

	public Token(TokenType type) {
		this(type, null, null);
	}
	public Token(TokenType type, Object value) {
		this(type, value, null);
	}

	public Token(Keyword keyword) {
		this(TokenType.KEYWORD, null, keyword);
	}
	
	public boolean isLiteral() {
		return is(TokenType.IDENTIFIER, TokenType.STRING, TokenType.NUMBER, TokenType.COMPLEX) || 
				is(Keyword.TRUE, Keyword.FALSE, Keyword.NULL);
	}
	
	public boolean is(TokenType... types) {
		for(TokenType type : types)
			if(this.type == type)
				return true;
		return false;
	}
	public boolean is(Keyword... keywords) {
		for(Keyword keyword : keywords)
			if(this.keyword == keyword)
				return true;
		return false;
	}

	private void ensureType(TokenType type) {
		if(this.type != type)
			throw new SyntaxLangException("Expected Token to be of type %s, got %s instead at position %s"
					.formatted(type, this.type, position));
	}
	
	public String identifierValue() {
		ensureType(TokenType.IDENTIFIER);
		return ((WideString) value).toString();
	}
	public WideString stringValue() {
		ensureType(TokenType.STRING);
		return (WideString) value;
	}
	public BigDecimal numberValue() {
		ensureType(TokenType.NUMBER);
		return (BigDecimal) value;
	}
	public BigComplex complexValue() {
		ensureType(TokenType.COMPLEX);
		return (BigComplex) value;
	}
	
	public boolean booleanValue() {
		if(!is(Keyword.TRUE, Keyword.FALSE))
			throw new SyntaxLangException("Expected Token to be of type boolean, got %s instead at position %s"
					.formatted(type, position));
		
		return is(Keyword.TRUE);
	}
	
	@Override
	public String toString() {
		if(keyword != null)
			return "Token[keyword = %s]".formatted(keyword, position);
		
		if(value != null)
			return "Token[%s, value = %s]".formatted(type, value, position);
		
		return "Token[%s]".formatted(type, position);
	}
	
	public String toSimpleString() {
		if(keyword != null)
			return "keyword " + keyword.toString();
		
		if(value != null)
			return type + "[" + value + "]";
		
		return type.toString();
	}
	
}
