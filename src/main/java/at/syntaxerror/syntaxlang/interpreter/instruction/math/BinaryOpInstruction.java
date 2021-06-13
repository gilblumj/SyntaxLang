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
package at.syntaxerror.syntaxlang.interpreter.instruction.math;

import java.util.function.Function;

import at.syntaxerror.syntaxlang.input.InputEnvironment;
import at.syntaxerror.syntaxlang.interpreter.Interpreter;
import at.syntaxerror.syntaxlang.interpreter.RuntimeEnvironment;
import at.syntaxerror.syntaxlang.interpreter.instruction.Instruction;
import at.syntaxerror.syntaxlang.interpreter.instruction.InstructionData;
import at.syntaxerror.syntaxlang.interpreter.instruction.literal.LiteralInstruction;
import at.syntaxerror.syntaxlang.interpreter.object.classlike.RuntimeClassLikeInstance;
import at.syntaxerror.syntaxlang.interpreter.object.classlike.SpecialFunctions;
import at.syntaxerror.syntaxlang.interpreter.object.function.RuntimeCallData;
import at.syntaxerror.syntaxlang.interpreter.result.RuntimeResult;
import at.syntaxerror.syntaxlang.interpreter.value.RuntimeValue;
import at.syntaxerror.syntaxlang.lexer.Token;
import at.syntaxerror.syntaxlang.lexer.TokenType;
import at.syntaxerror.syntaxlang.parser.node.BinaryOpNode;
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
public class BinaryOpInstruction implements Instruction {

	public static Instruction of(BinaryOpNode node) {
		return new BinaryOpInstruction(
			node.getPosition(),
			Interpreter.makeInstruction(node.getLeft()),
			node.getOp(),
			Interpreter.makeInstruction(node.getRight())
		);
	}
	
	private RuntimeResult performOperation(InstructionData data, RuntimeValue left, Token op, RuntimeValue right) {
		String opName = op.toSimpleString();
		
		RuntimeEnvironment env = data.env();
		InputEnvironment input = env.getInputEnvironment();
		
		if(left.isFunction()) {
			input.terminate("Invalid binary operation for function: %s".formatted(opName), position);
			return RuntimeResult.unexpected(data, getPosition());
		}
		
		if(left.isNull()) {
			if(op.is(TokenType.EQUAL, TokenType.IDENTICAL))
				return new RuntimeResult().value(
					RuntimeValue.of(getPosition(), right.isNull())
				);
			
			input.terminate("Invalid binary operation for null: %s".formatted(opName), position);
			return RuntimeResult.unexpected(data, getPosition());
		}
		
		RuntimeClassLikeInstance instance = left.getAsObject();
		
		Function<RuntimeCallData, RuntimeResult> fn = switch(op.getType()) {
		case EQUAL -> SpecialFunctions::equ;
		case IDENTICAL -> SpecialFunctions::ieq;
		
		case AND -> SpecialFunctions::and;
		case OR -> SpecialFunctions::or;
		
		case LESS -> SpecialFunctions::lss;
		case LESS_EQUAL -> SpecialFunctions::leq;
		case GREATER -> SpecialFunctions::gtr;
		case GREATER_EQUAL -> SpecialFunctions::geq;
		
		case BITAND -> SpecialFunctions::bitand;
		case XOR -> SpecialFunctions::xor;
		case BITOR -> SpecialFunctions::bitor;
		
		case LSHIFT -> SpecialFunctions::lsh;
		case RSHIFT -> SpecialFunctions::rsh;
		
		case PLUS -> SpecialFunctions::add;
		case MINUS -> SpecialFunctions::sub;
		case MULTIPLY -> SpecialFunctions::mul;
		case DIVIDE -> SpecialFunctions::div;
		case MODULO -> SpecialFunctions::mod;
		case POWER -> SpecialFunctions::pow;
		
		default -> null;
		};
		
		if(fn == null) {
			input.terminate(
				"Invalid binary operation: %s %s %s"
					.formatted(left.getType(), opName, right.getType()),
				position
			);
			return RuntimeResult.unexpected(data, getPosition());
		}
		
		return fn.apply(new RuntimeCallData(env, position, instance.getType(), instance, new RuntimeValue[] {
			right
		}));
	}
	
	private final Position position;
	private final Instruction left;
	private final Token op;
	private final Instruction right;
	
	private RuntimeValue cached = null;
	
	@Override
	public RuntimeResult process(InstructionData data) {
		if(left instanceof LiteralInstruction llit && right instanceof LiteralInstruction rlit) {
			if(cached != null)
				return new RuntimeResult().value(cached);
			
			RuntimeResult result = performOperation(data, llit.getValue(), op, rlit.getValue());
			
			if(result.isThrow())
				return result;
			
			if(!result.isNothing() || !result.hasValue())
				data.env().getInputEnvironment().terminate(
					"Invalid binary operation: %s %s %s"
						.formatted(llit.getValue().getType(), op.toSimpleString(), rlit.getValue().getType()),
					left.getPosition()
				);
			
			return new RuntimeResult().value(cached = result.getValue());
		}
		
		RuntimeResult leftResult = left.process(data);
		
		if(leftResult.isThrow())
			return leftResult;
		
		if(!leftResult.isNothing() || !leftResult.hasValue())
			data.env().getInputEnvironment().terminate(
				"Invalid expression in binary operation",
				left.getPosition()
			);
		
		RuntimeValue leftValue = leftResult.getValue();
		
		/*
		 * don't evaluate right expression for the following conditions:
		 * 
		 * - false && ... (always false)
		 * - true || ... (always true)
		 */
		if(op.is(TokenType.AND, TokenType.OR) && leftValue.isBoolean()) {
			boolean boolVal = leftValue.booleanValue();
			
			if((op.is(TokenType.AND) && !boolVal) ||
				(op.is(TokenType.OR) && boolVal))
				return new RuntimeResult().value(RuntimeValue.of(getPosition(), boolVal));
		}
		
		RuntimeResult rightResult = right.process(data);
		
		if(rightResult.isThrow())
			return leftResult;
		
		if(!rightResult.isNothing() || !rightResult.hasValue())
			data.env().getInputEnvironment().terminate(
				"Invalid expression in binary operation",
				left.getPosition()
			);
		
		return performOperation(data, leftValue, op, rightResult.getValue());
	}

}
