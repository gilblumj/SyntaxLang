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
package at.syntaxerror.syntaxlang.interpreter.object;

import at.syntaxerror.syntaxlang.lexer.Keyword;
import at.syntaxerror.syntaxlang.lexer.Token;

/**
 * @author SyntaxError
 * 
 */
public enum AccessModifier {
	
	DEFAULT,
	PUBLIC,
	PRIVATE,
	PROTECTED;
	
	public static AccessModifier valueOf(Token tok) {
		if(tok.is(Keyword.PUBLIC))
			return PUBLIC;

		if(tok.is(Keyword.PRIVATE))
			return PRIVATE;

		if(tok.is(Keyword.PROTECTED))
			return PROTECTED;
		
		return DEFAULT;
	}
	
}
