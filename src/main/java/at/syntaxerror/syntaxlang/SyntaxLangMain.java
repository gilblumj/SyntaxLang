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
package at.syntaxerror.syntaxlang;

import java.io.File;
import java.util.List;
import java.util.Locale;

import at.syntaxerror.syntaxlang.input.InputEnvironment;
import at.syntaxerror.syntaxlang.interpreter.Interpreter;
import at.syntaxerror.syntaxlang.interpreter.RuntimeEnvironment;
import at.syntaxerror.syntaxlang.interpreter.instruction.Instruction;
import at.syntaxerror.syntaxlang.interpreter.instruction.InstructionData;
import at.syntaxerror.syntaxlang.interpreter.result.RuntimeResult;
import at.syntaxerror.syntaxlang.interpreter.trace.RuntimeStackTraceElement;
import at.syntaxerror.syntaxlang.lexer.Lexer;
import at.syntaxerror.syntaxlang.lexer.Tokens;
import at.syntaxerror.syntaxlang.parser.Parser;
import at.syntaxerror.syntaxlang.parser.node.StatementsNode;

/**
 * 
 * @author SyntaxError
 *
 */
public class SyntaxLangMain {

	public static void main(String[] args) {
		// TODO proper main
		
		Locale.setDefault(Locale.ENGLISH);
		
		InputEnvironment in = InputEnvironment.fromFile(new File("/run/media/thomas/Shared/Git/SyntaxLang/src/main/resources/test/test2.sl"));
		
		Lexer lexer = new Lexer(in);
		
		Tokens tokens = lexer.makeTokens();
		System.out.println(tokens);
		
		Parser parser = new Parser(in, tokens);
		
		StatementsNode nodes = parser.makeNodes();
		
		System.out.println(nodes);
		
		System.out.println(nodes.toJSON().toString(2));
		
		RuntimeEnvironment runtime = new RuntimeEnvironment(in);
		
		List<Instruction> instructions = Interpreter.makeInstructions(nodes);
		
		System.out.println(instructions);
		
		for(Instruction instr : instructions) {
			RuntimeResult res = instr.process(new InstructionData(runtime, new RuntimeStackTraceElement(
				in.getSource(),
				runtime.getNamespace(),
				null,
				null,
				instr.getPosition().line()
			)));
			
			res.terminateIfThrown();
			
			if(!res.isNothing()) {
				System.err.println("ERROR: Invalid instruction => " + res);
				break;
			}
		}
	}
	
}
