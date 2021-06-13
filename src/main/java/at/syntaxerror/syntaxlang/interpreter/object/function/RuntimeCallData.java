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
package at.syntaxerror.syntaxlang.interpreter.object.function;

import at.syntaxerror.syntaxlang.interpreter.RuntimeEnvironment;
import at.syntaxerror.syntaxlang.interpreter.object.classlike.RuntimeClassLike;
import at.syntaxerror.syntaxlang.interpreter.object.classlike.RuntimeClassLikeInstance;
import at.syntaxerror.syntaxlang.interpreter.value.RuntimeValue;
import at.syntaxerror.syntaxlang.trace.Position;
import lombok.NonNull;

/**
 * @author SyntaxError
 * 
 */
public record RuntimeCallData(@NonNull RuntimeEnvironment env, @NonNull Position called, RuntimeClassLike type, 
		RuntimeClassLikeInstance inst, @NonNull RuntimeValue[] args) {
	
}
