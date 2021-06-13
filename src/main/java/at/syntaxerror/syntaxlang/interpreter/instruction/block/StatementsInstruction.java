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

import java.util.List;

import at.syntaxerror.syntaxlang.interpreter.Interpreter;
import at.syntaxerror.syntaxlang.interpreter.RuntimeEnvironment;
import at.syntaxerror.syntaxlang.interpreter.instruction.EmptyInstruction;
import at.syntaxerror.syntaxlang.interpreter.instruction.Instruction;
import at.syntaxerror.syntaxlang.interpreter.instruction.InstructionData;
import at.syntaxerror.syntaxlang.interpreter.result.RuntimeResult;
import at.syntaxerror.syntaxlang.parser.node.StatementsNode;
import at.syntaxerror.syntaxlang.trace.Position;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author SyntaxError
 * 
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class StatementsInstruction implements Instruction {

	public static Instruction of(StatementsNode node) {
		if(node.getStatements().isEmpty())
			return EmptyInstruction.of(node.getPosition());
		
		return new StatementsInstruction(node.getPosition(), Interpreter.makeInstructions(node));
	}
	
	@Getter
	private final Position position;
	private final List<Instruction> instructions;
	
	@Override
	public RuntimeResult process(InstructionData data) {
		RuntimeEnvironment env = data.env().buildChild();
		
		for(Instruction instr : instructions) {
			RuntimeResult res = instr.process(data, env);
			
			if(!res.isNothing())
				return res;
		}
		
		return new RuntimeResult();
	}

}
