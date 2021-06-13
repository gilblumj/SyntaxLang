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
package at.syntaxerror.syntaxlang.interpreter.trace;

import java.util.List;

import at.syntaxerror.syntaxlang.interpreter.value.RuntimeValue;
import at.syntaxerror.syntaxlang.misc.AnsiUtils;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * @author SyntaxError
 * 
 */
@Getter
@RequiredArgsConstructor
@NonNull
public class RuntimeError {

	@NonNull
	private final RuntimeValue value;
	
	@NonNull
	private final String name;
	private final String message;
	@NonNull
	private final List<RuntimeStackTraceElement> trace;

	public final String printStackTrace() {
		StringBuilder sb = new StringBuilder();
		sb.append(AnsiUtils.Foreground.BRIGHT_RED);
		
		sb.append("Uncaught exception %s\n".formatted(this));
		
		for(RuntimeStackTraceElement element : trace)
			sb.append("\tat %s\n".formatted(element));
		
		String str = sb.toString();
		return AnsiUtils.stripAnsi(str.substring(0, str.length() - 1));
	}
	
	@Override
	public String toString() {
		return message == null ? name : name + ": " + message;
	}
	
}
