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
package at.syntaxerror.syntaxlang.trace;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

/**
 * @author SyntaxError
 * 
 */
@Getter
@ToString
public class ErrorOptional<T> {

	public static <T> ErrorOptional<T> of(T value) {
		return new ErrorOptional<T>(value);
	}
	
	private final T value;
	
	private final Position position;
	private final String message;
	
	private final boolean error;
	
	private final Throwable trace;

	public ErrorOptional(T value) {
		this.value = value;
		
		position = null;
		message = null;
		
		error = false;
		
		trace = null;
	}
	
	public ErrorOptional(@NonNull Position position, @NonNull String message) {
		value = null;
		
		this.position = position;
		this.message = message;
		
		error = true;
		
		trace = new Throwable();
	}
	
	public boolean isEmpty() {
		return value == null;
	}
	public boolean isPresent() {
		return value != null;
	}
	
}
