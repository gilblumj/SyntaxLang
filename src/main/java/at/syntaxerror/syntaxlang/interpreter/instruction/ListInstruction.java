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
package at.syntaxerror.syntaxlang.interpreter.instruction;

import java.util.ArrayList;
import java.util.List;

import at.syntaxerror.syntaxlang.interpreter.Interpreter;
import at.syntaxerror.syntaxlang.interpreter.result.RuntimeResult;
import at.syntaxerror.syntaxlang.interpreter.value.RuntimeValue;
import at.syntaxerror.syntaxlang.parser.node.ListNode;
import at.syntaxerror.syntaxlang.parser.node.Node;
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
public class ListInstruction implements Instruction {
	
	public static Instruction of(ListNode node) {
		List<Instruction> values = new ArrayList<>();
		
		for(Node n : node.getValues())
			values.add(Interpreter.makeInstruction(n));
		
		return new ListInstruction(node.getPosition(), values);
	}
	
	private final Position position;
	private final List<Instruction> values;
	
	@Override
	public RuntimeResult process(InstructionData env) {
		List<RuntimeValue> runtimeValues = new ArrayList<>();
		
		for(Instruction instruction : values) {
			RuntimeResult value = instruction.process(env.at(instruction.getPosition()));
			
			if(value.isThrow())
				return value;
			
			if(value.isNothing() && value.hasValue())
				runtimeValues.add(value.getValue());
			else env.env().getInputEnvironment().terminate(
				"Invalid expression in list",
				instruction.getPosition()
			);
		}
		
		return new RuntimeResult().value(RuntimeValue.of(getPosition(), runtimeValues));
	}
	
}
