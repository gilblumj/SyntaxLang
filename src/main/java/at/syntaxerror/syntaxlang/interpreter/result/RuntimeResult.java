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
package at.syntaxerror.syntaxlang.interpreter.result;

import at.syntaxerror.syntaxlang.interpreter.RuntimeEnvironment;
import at.syntaxerror.syntaxlang.interpreter.instruction.InstructionData;
import at.syntaxerror.syntaxlang.interpreter.trace.RuntimeError;
import at.syntaxerror.syntaxlang.interpreter.value.RuntimeValue;
import at.syntaxerror.syntaxlang.trace.Position;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * @author SyntaxError
 * 
 */
@Getter
@ToString
public class RuntimeResult {
	
	public static RuntimeResult unexpected(RuntimeEnvironment env, Position pos, String className, String function) {
		return new RuntimeResult().throwException(
			env.buildError(
				RuntimeValue.of(pos, "An unexpected error occured"),
				className,
				function,
				pos
			)
		);
	}
	public static RuntimeResult unexpected(InstructionData data, Position pos) {
		return unexpected(data.env(), pos, data.caller().className(), data.caller().function());
	}
	
	private RuntimeResultAction action;
	
	private RuntimeError exception;
	private String label;
	private RuntimeValue value;
	
	public RuntimeResult() {
		nothing();
	}
	
	private void reset() {
		action = null;
		exception = null;
		label = null;
		value = null;
	}
	
	public RuntimeResult nothing() {
		reset();
		action = RuntimeResultAction.NOTHING;
		return this;
	}
	public RuntimeResult value(@NonNull RuntimeValue value) {
		reset();
		action = RuntimeResultAction.VALUE;
		this.value = value;
		return this;
	}

	public RuntimeResult returnPlain() {
		reset();
		action = RuntimeResultAction.RETURN;
		return this;
	}
	public RuntimeResult returnValue0(@NonNull RuntimeValue value) {
		reset();
		action = RuntimeResultAction.RETURN_VALUE;
		this.value = value;
		return this;
	}

	public RuntimeResult yieldValue(@NonNull RuntimeValue value) {
		reset();
		action = RuntimeResultAction.YIELD_VALUE;
		this.value = value;
		return this;
	}

	public RuntimeResult breakPlain() {
		reset();
		action = RuntimeResultAction.BREAK;
		return this;
	}
	public RuntimeResult breakLabel(@NonNull String label) {
		reset();
		action = RuntimeResultAction.BREAK_LABEL;
		this.label = label;
		return this;
	}

	public RuntimeResult continuePlain() {
		reset();
		action = RuntimeResultAction.CONTINUE;
		return this;
	}
	public RuntimeResult continueLabel(@NonNull String label) {
		reset();
		action = RuntimeResultAction.CONTINUE_LABEL;
		this.label = label;
		return this;
	}
	
	public RuntimeResult throwException(@NonNull RuntimeError exception) {
		action = RuntimeResultAction.THROW_EXCEPTION;
		reset();
		this.exception = exception;
		return this;
	}
	
	public boolean hasValue() {
		return action == RuntimeResultAction.RETURN_VALUE || 
				action == RuntimeResultAction.YIELD_VALUE || 
				action == RuntimeResultAction.VALUE;
	}
	public boolean hasLabel() {
		return action == RuntimeResultAction.BREAK_LABEL || 
				action == RuntimeResultAction.CONTINUE_LABEL;
	}
	public boolean hasException() {
		return action == RuntimeResultAction.THROW_EXCEPTION;
	}

	public boolean isNothing() {
		return action == RuntimeResultAction.NOTHING || 
				action == RuntimeResultAction.VALUE;
	}
	public boolean isReturn() {
		return action == RuntimeResultAction.RETURN || 
				action == RuntimeResultAction.RETURN_VALUE;
	}
	public boolean isYield() {
		return action == RuntimeResultAction.YIELD_VALUE;
	}
	public boolean isBreak() {
		return action == RuntimeResultAction.BREAK || 
				action == RuntimeResultAction.BREAK_LABEL;
	}
	public boolean isContinue() {
		return action == RuntimeResultAction.CONTINUE || 
				action == RuntimeResultAction.CONTINUE_LABEL;
	}
	public boolean isThrow() {
		return action == RuntimeResultAction.THROW_EXCEPTION;
	}

	public void terminateIfThrown() {
		if(!isThrow())
			return;
		
		System.err.println(getException().printStackTrace());
		System.exit(1);
	}

}
