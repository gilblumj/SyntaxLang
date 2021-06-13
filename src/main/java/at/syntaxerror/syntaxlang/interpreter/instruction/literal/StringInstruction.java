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

import at.syntaxerror.syntaxlang.interpreter.instruction.Instruction;
import at.syntaxerror.syntaxlang.interpreter.value.RuntimeValue;
import at.syntaxerror.syntaxlang.parser.node.LiteralNode;
import at.syntaxerror.syntaxlang.string.WideString;
import at.syntaxerror.syntaxlang.trace.Position;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author SyntaxError
 * 
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class StringInstruction implements LiteralInstruction {
	
	public static Instruction of(LiteralNode node) {
		return new StringInstruction(node.getPosition(), node.getValue().stringValue());
	}
	
	private final Position position;
	private final WideString value;
	
	@Override
	public RuntimeValue getValue() {
		return RuntimeValue.of(getPosition(), value);
	}
	
}
