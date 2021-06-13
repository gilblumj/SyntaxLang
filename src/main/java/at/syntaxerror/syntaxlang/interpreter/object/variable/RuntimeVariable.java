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
package at.syntaxerror.syntaxlang.interpreter.object.variable;

import at.syntaxerror.syntaxlang.interpreter.Interpreter;
import at.syntaxerror.syntaxlang.interpreter.RuntimeEnvironment;
import at.syntaxerror.syntaxlang.interpreter.instruction.Instruction;
import at.syntaxerror.syntaxlang.interpreter.object.AccessModifier;
import at.syntaxerror.syntaxlang.interpreter.object.Accessible;
import at.syntaxerror.syntaxlang.interpreter.value.RuntimeValue;
import at.syntaxerror.syntaxlang.parser.node.VarDeclNode;
import at.syntaxerror.syntaxlang.trace.Position;
import lombok.Getter;
import lombok.NonNull;

/**
 * @author SyntaxError
 * 
 */
@Getter
public class RuntimeVariable implements Accessible {

	private final AccessModifier accessModifier;

	private final boolean staticModifier;
	private final boolean finalModifier;
	
	private final String name;
	
	private final Instruction variableValue;
	private final RuntimeValue constantValue;
	private final boolean isValueConstant;
	
	private final RuntimeEnvironment env;
	
	private final Position position;
	
	public RuntimeVariable(@NonNull RuntimeEnvironment env, @NonNull VarDeclNode node) {
		position = node.getPosition();
		this.env = env;
		
		accessModifier = AccessModifier.valueOf(node.getModAccess());
		staticModifier = node.getModStatic() != null;
		finalModifier = node.getModFinal() != null;
		
		this.name = node.getName().identifierValue();
		
		variableValue = Interpreter.makeInstruction(node.getValue());
		constantValue = null;
		isValueConstant = false;

		register();
	}
	
	public RuntimeVariable(@NonNull Position position, @NonNull RuntimeEnvironment env, 
			@NonNull String name, @NonNull RuntimeValue value) {
		this.position = position;
		this.env = env;
		
		accessModifier = AccessModifier.PUBLIC;
		staticModifier = false;
		finalModifier = true;
		
		this.name = name;
		
		variableValue = null;
		constantValue = value;
		isValueConstant = true;
		
		register();
	}
	
	private void register() {
		
	}

}
