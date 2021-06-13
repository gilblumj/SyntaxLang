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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * @author SyntaxError
 * 
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Resolved<T> {

	private static final Resolved<?> EMPTY = new Resolved<>();
	
	@SuppressWarnings("unchecked")
	public static <T> Resolved<T> empty() {
		return (Resolved<T>) EMPTY;
	}

	public static <T> Resolved<T> of(T value) {
		return new Resolved<T>(value);
	}

	@NonNull
	private final T value;
	
	private Resolved() {
		value = null;
	}
	
	public boolean isResolved() {
		return value != null;
	}
	
	public boolean isInstance(Class<?> cls) {
		return isResolved() && cls.isAssignableFrom(value.getClass());
	}

	public <R> R getAs(Class<R> type) {
		return type.cast(value);
	}

}
