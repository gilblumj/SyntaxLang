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
package at.syntaxerror.syntaxlang.interpreter.object.function;

import at.syntaxerror.syntaxlang.interpreter.RuntimeEnvironment;
import at.syntaxerror.syntaxlang.interpreter.object.AccessModifier;
import at.syntaxerror.syntaxlang.interpreter.object.Accessible;
import at.syntaxerror.syntaxlang.interpreter.object.Callable;
import at.syntaxerror.syntaxlang.interpreter.object.classlike.RuntimeClassLikeInstance;
import at.syntaxerror.syntaxlang.interpreter.result.RuntimeResult;
import at.syntaxerror.syntaxlang.interpreter.value.RuntimeValue;
import at.syntaxerror.syntaxlang.lexer.Keyword;
import at.syntaxerror.syntaxlang.lexer.Token;
import at.syntaxerror.syntaxlang.parser.node.FuncDefNode;
import at.syntaxerror.syntaxlang.trace.Position;
import lombok.Getter;
import lombok.NonNull;

/**
 * @author SyntaxError
 * 
 */
@Getter
public class RuntimeFunction implements Accessible, Callable {

	private final AccessModifier accessModifier;

	private final boolean staticModifier;
	private final boolean abstractModifier;
	private final boolean finalModifier;
	private final boolean defaultModifier;
	
	private final boolean hasBody;
	
	private final String name;
	
	private final RuntimeEnvironment env;
	
	private final Position position;
	
	public RuntimeFunction(@NonNull RuntimeEnvironment env, @NonNull FuncDefNode node) {
		position = node.getPosition();
		this.env = env;
		
		accessModifier = AccessModifier.valueOf(node.getModAccess());
		staticModifier = node.getModStatic() != null;
		
		Token modAbstractFinalDefault = node.getModAbstractFinalDefault();
		
		if(modAbstractFinalDefault != null) {
			abstractModifier = modAbstractFinalDefault.is(Keyword.ABSTRACT);
			finalModifier = modAbstractFinalDefault.is(Keyword.FINAL);
			defaultModifier = modAbstractFinalDefault.is(Keyword.DEFAULT);
		} else abstractModifier = finalModifier = defaultModifier = false;
		
		hasBody = node.isHasBody();
		
		this.name = node.getName().identifierValue();
	}
	
	public void init() {
		
	}
	
	@Override
	public RuntimeResult call(RuntimeClassLikeInstance instance, RuntimeValue[] args) {
		
		
		return null;
	}

}
