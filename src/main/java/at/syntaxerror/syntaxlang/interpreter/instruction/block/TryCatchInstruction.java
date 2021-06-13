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

import at.syntaxerror.syntaxlang.interpreter.Interpreter;
import at.syntaxerror.syntaxlang.interpreter.RuntimeEnvironment;
import at.syntaxerror.syntaxlang.interpreter.instruction.EmptyInstruction;
import at.syntaxerror.syntaxlang.interpreter.instruction.Instruction;
import at.syntaxerror.syntaxlang.interpreter.instruction.InstructionData;
import at.syntaxerror.syntaxlang.interpreter.object.variable.RuntimeVariable;
import at.syntaxerror.syntaxlang.interpreter.result.RuntimeResult;
import at.syntaxerror.syntaxlang.lexer.Token;
import at.syntaxerror.syntaxlang.parser.node.TryCatchNode;
import at.syntaxerror.syntaxlang.trace.Position;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * @author SyntaxError
 * 
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TryCatchInstruction implements Instruction {

	public static Instruction of(TryCatchNode node) {
		Instruction tryBody = Interpreter.makeInstruction(node.getTryBody());
		Instruction catchBody = Interpreter.makeInstruction(node.getCatchBody());
		
		if(tryBody instanceof EmptyInstruction)
			return EmptyInstruction.of(node.getPosition());
		
		return new TryCatchInstruction(
			node,
			tryBody,
			catchBody,
			node.getVar()
		);
	}
	
	private final TryCatchNode node;
	private final Instruction tryBody;
	private final Instruction catchBody;
	private final Token varName;
	
	@Override
	public Position getPosition() {
		return node.getPosition();
	}
	
	@Override
	public RuntimeResult process(InstructionData data) {
		RuntimeEnvironment env = data.env();
		
		RuntimeResult res = tryBody.process(data, env.buildChild());
		
		if(res.isThrow()) {
			RuntimeEnvironment catchEnv = env.buildChild();
			
			new RuntimeVariable(
				varName.getPosition(),
				catchEnv,
				varName.identifierValue(),
				res.getException().getValue()
			);
			
			return catchBody.process(data, catchEnv);
		}
		
		return res;
	}

}
