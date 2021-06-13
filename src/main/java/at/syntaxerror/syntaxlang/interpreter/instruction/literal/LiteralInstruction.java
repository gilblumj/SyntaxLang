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
package at.syntaxerror.syntaxlang.interpreter.instruction.literal;

import at.syntaxerror.syntaxlang.SyntaxLangException;
import at.syntaxerror.syntaxlang.interpreter.instruction.Instruction;
import at.syntaxerror.syntaxlang.interpreter.instruction.InstructionData;
import at.syntaxerror.syntaxlang.interpreter.result.RuntimeResult;
import at.syntaxerror.syntaxlang.interpreter.value.RuntimeValue;
import at.syntaxerror.syntaxlang.lexer.Keyword;
import at.syntaxerror.syntaxlang.lexer.Token;
import at.syntaxerror.syntaxlang.parser.node.LiteralNode;

/**
 * @author SyntaxError
 * 
 */
public interface LiteralInstruction extends Instruction {
	
	RuntimeValue getValue();
	
	@Override
	default RuntimeResult process(InstructionData env) {
		return new RuntimeResult().value(getValue());
	}
	
	static Instruction of(LiteralNode node) {
		Token value = node.getValue();
		
		return switch(value.getType()) {
		case IDENTIFIER: yield IdentifierInstruction.of(node); // TODO
		case STRING: yield StringInstruction.of(node);
		case NUMBER: yield NumberInstruction.of(node);
		case COMPLEX: yield ComplexInstruction.of(node);
		case KEYWORD:
			if(value.is(Keyword.TRUE, Keyword.FALSE))
				yield BooleanInstruction.of(node);
			if(value.is(Keyword.NULL))
				yield NullInstruction.of(node);
		default: throw new SyntaxLangException("Invalid literal value: " + value);
		};
	}

	static Instruction of(Token tok) {
		return of(new LiteralNode(tok));
	}
	
}
