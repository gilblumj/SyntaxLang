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

import lombok.NonNull;

/**
 * @author SyntaxError
 * 
 */
public record RuntimeStackTraceElement(@NonNull String file, String namespace, String className, String function, int line) {
	
	@Override
	public String toString() {
		if(namespace == null) {
			if(function == null) // file:line
				return "%s:%d".formatted(file, line);
			
			if(className == null) // file#function:line
				return "%s#%s:%d".formatted(file, function, line);
			
			// file@class#function:line
			return "%s@%s#%s:%d".formatted(file, className, function, line);
		}
		
		if(function == null) // file@namespace:line
			return "%s@%s:%d".formatted(file, namespace, line);
		
		if(className == null) // file#namespace\function:line
			return "%s#%s\\%s:%d".formatted(file, namespace, function, line);
		
		// file@namespace\class#function:line
		return "%s@%s\\%s#%s:%d".formatted(file, namespace, className, function, line);
	}
	
}
