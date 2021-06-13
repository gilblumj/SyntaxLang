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
package at.syntaxerror.syntaxlang.interpreter.object;

import at.syntaxerror.syntaxlang.interpreter.object.function.RuntimeFunction;
import at.syntaxerror.syntaxlang.interpreter.object.variable.RuntimeVariable;
import lombok.Getter;
import lombok.NonNull;

/**
 * @author SyntaxError
 * 
 */
@Getter
public class Member {

	public static final Member EMPTY = new Member();
	
	private final RuntimeFunction function;
	private final RuntimeVariable variable;
	
	public Member(@NonNull RuntimeFunction function) {
		this.function = function;
		variable = null;
	}
	public Member(@NonNull RuntimeVariable variable) {
		function = null;
		this.variable = variable;
	}
	
	private Member() {
		function = null;
		variable = null;
	}
	
	public boolean isFunction() {
		return function != null;
	}
	public boolean isVariable() {
		return variable != null;
	}
	
	public boolean isResolved() {
		return isFunction() || isVariable();
	}

}
