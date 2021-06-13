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

import java.util.HashMap;
import java.util.Map;

/**
 * @author SyntaxError
 * 
 */
public enum Keyword {
	YIELD,
	RETURN,
	BREAK,
	CONTINUE,
	NAMESPACE,
	IMPORT,
	USE,
	AS,
	TRY,
	CATCH,
	IF,
	ELSEIF,
	ELIF,
	ELSE,
	FOR,
	FOREACH,
	IN,
	WHILE,
	DO,
	SWITCH,
	CASE,
	DEFAULT,
	PUBLIC,
	PRIVATE,
	PROTECTED,
	STATIC,
	FINAL,
	ABSTRACT,
	FUN,
	CLONEABLE,
	CLASS,
	EXTENDS,
	IMPLEMENTS,
	STRUCT,
	ENUM,
	INTERFACE,
	THROW,
	INSTANCEOF,
	NEW,
	CLONE,
	TRUE,
	FALSE,
	NULL
	;
	
	private static final Map<String, Keyword> MAP = new HashMap<>();
	
	static {
		for(Keyword keyword : values())
			MAP.put(keyword.toString(), keyword);
	}
	
	public static Keyword getKeyword(String name) {
		return MAP.get(name);
	}
	
	@Override
	public String toString() {
		return name().toLowerCase();
	}
	
}
