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

import java.util.HashMap;
import java.util.Map;

import at.syntaxerror.syntaxlang.SyntaxLangException;
import at.syntaxerror.syntaxlang.interpreter.Interpreter;
import at.syntaxerror.syntaxlang.interpreter.instruction.literal.IdentifierInstruction;
import at.syntaxerror.syntaxlang.interpreter.instruction.literal.LiteralInstruction;
import at.syntaxerror.syntaxlang.interpreter.instruction.literal.NullInstruction;
import at.syntaxerror.syntaxlang.interpreter.result.RuntimeResult;
import at.syntaxerror.syntaxlang.interpreter.value.RuntimeValue;
import at.syntaxerror.syntaxlang.parser.node.MapNode;
import at.syntaxerror.syntaxlang.parser.node.MapPartNode;
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
public class MapInstruction implements Instruction {
	
	public static Instruction of(MapNode node) {
		Map<RuntimeValue, Instruction> values = new HashMap<>();
		
		for(MapPartNode n : node.getValues()) {
			Instruction keyInstruction = LiteralInstruction.of(n.getName());
			Instruction valueInstruction = Interpreter.makeInstruction(n.getValue());
			
			if(keyInstruction instanceof LiteralInstruction lit) {
				if(lit instanceof NullInstruction)
					throw new SyntaxLangException("Map key is null");
				
				values.put(lit.getValue(), valueInstruction);
			}
			else if(keyInstruction instanceof IdentifierInstruction id)
				values.put(RuntimeValue.of(id.getPosition(), id.getValue()), valueInstruction);
			else throw new SyntaxLangException("Invalid map key: " + valueInstruction);
		}
		
		return new MapInstruction(node.getPosition(), values);
	}
	
	private final Position position;
	private final Map<RuntimeValue, Instruction> values;
	
	@Override
	public RuntimeResult process(InstructionData env) {
		Map<RuntimeValue, RuntimeValue> runtimeValues = new HashMap<>();
		
		for(RuntimeValue key : runtimeValues.keySet()) {
			Instruction instruction = values.get(key);
			
			RuntimeResult value = instruction.process(env.at(instruction.getPosition()));
			
			if(value.isThrow())
				return value;
			
			if(value.isNothing() && value.hasValue())
				runtimeValues.put(key, value.getValue());
			else env.env().getInputEnvironment().terminate(
				"Invalid expression in list",
				instruction.getPosition()
			);
		}
		
		return new RuntimeResult().value(RuntimeValue.of(getPosition(), runtimeValues));
	}
	
}
