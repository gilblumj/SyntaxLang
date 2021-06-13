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
package at.syntaxerror.syntaxlang.interpreter.instruction.block;

import at.syntaxerror.syntaxlang.interpreter.instruction.Instruction;
import at.syntaxerror.syntaxlang.interpreter.instruction.InstructionData;
import at.syntaxerror.syntaxlang.interpreter.result.RuntimeResult;
import at.syntaxerror.syntaxlang.parser.node.WhileNode;
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
public class WhileInstruction implements Instruction {

	public static Instruction of(WhileNode node) {
		
		return new WhileInstruction(node.getPosition());
	}
	
	private final Position position;
	
	@Override
	public RuntimeResult process(InstructionData env) {
		
		return new RuntimeResult();
	}

}
