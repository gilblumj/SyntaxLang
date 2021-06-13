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
package at.syntaxerror.syntaxlang.interpreter;

import java.util.List;
import java.util.stream.Collectors;

import at.syntaxerror.syntaxlang.SyntaxLangException;
import at.syntaxerror.syntaxlang.interpreter.instruction.EmptyInstruction;
import at.syntaxerror.syntaxlang.interpreter.instruction.Instruction;
import at.syntaxerror.syntaxlang.interpreter.instruction.ListInstruction;
import at.syntaxerror.syntaxlang.interpreter.instruction.MapInstruction;
import at.syntaxerror.syntaxlang.interpreter.instruction.block.DoWhileInstruction;
import at.syntaxerror.syntaxlang.interpreter.instruction.block.ForInstruction;
import at.syntaxerror.syntaxlang.interpreter.instruction.block.ForeachInstruction;
import at.syntaxerror.syntaxlang.interpreter.instruction.block.IfInstruction;
import at.syntaxerror.syntaxlang.interpreter.instruction.block.StatementsInstruction;
import at.syntaxerror.syntaxlang.interpreter.instruction.block.SwitchInstruction;
import at.syntaxerror.syntaxlang.interpreter.instruction.block.TryCatchInstruction;
import at.syntaxerror.syntaxlang.interpreter.instruction.block.WhileInstruction;
import at.syntaxerror.syntaxlang.interpreter.instruction.literal.LiteralInstruction;
import at.syntaxerror.syntaxlang.interpreter.instruction.math.BinaryOpInstruction;
import at.syntaxerror.syntaxlang.parser.node.BinaryOpNode;
import at.syntaxerror.syntaxlang.parser.node.DoWhileNode;
import at.syntaxerror.syntaxlang.parser.node.EmptyNode;
import at.syntaxerror.syntaxlang.parser.node.ForNode;
import at.syntaxerror.syntaxlang.parser.node.ForeachNode;
import at.syntaxerror.syntaxlang.parser.node.IfNode;
import at.syntaxerror.syntaxlang.parser.node.ListNode;
import at.syntaxerror.syntaxlang.parser.node.LiteralNode;
import at.syntaxerror.syntaxlang.parser.node.MapNode;
import at.syntaxerror.syntaxlang.parser.node.Node;
import at.syntaxerror.syntaxlang.parser.node.StatementsNode;
import at.syntaxerror.syntaxlang.parser.node.SwitchNode;
import at.syntaxerror.syntaxlang.parser.node.TryCatchNode;
import at.syntaxerror.syntaxlang.parser.node.WhileNode;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * @author SyntaxError
 * 
 */
@UtilityClass
public class Interpreter {

	public static List<Instruction> makeInstructions(@NonNull StatementsNode statements) {
		return statements.getStatements()
				.stream()
				.map(Interpreter::makeInstruction)
				.collect(Collectors.toList());
	}
	
	public static Instruction makeInstruction(@NonNull Node node) {
		if(node instanceof EmptyNode)
			return EmptyInstruction.of(node.getPosition());
		
		if(node instanceof StatementsNode n)
			return StatementsInstruction.of(n);
		if(node instanceof TryCatchNode n)
			return TryCatchInstruction.of(n);
		if(node instanceof IfNode n)
			return IfInstruction.of(n);
		if(node instanceof ForNode n)
			return ForInstruction.of(n);
		if(node instanceof ForeachNode n)
			return ForeachInstruction.of(n);
		if(node instanceof WhileNode n)
			return WhileInstruction.of(n);
		if(node instanceof DoWhileNode n)
			return DoWhileInstruction.of(n);
		if(node instanceof SwitchNode n)
			return SwitchInstruction.of(n);
		
		if(node instanceof BinaryOpNode n)
			return BinaryOpInstruction.of(n);
		if(node instanceof LiteralNode n)
			return LiteralInstruction.of(n);
		if(node instanceof ListNode n)
			return ListInstruction.of(n);
		if(node instanceof MapNode n)
			return MapInstruction.of(n);
		
		throw new SyntaxLangException("Illegal node: " + node);
	}

}
