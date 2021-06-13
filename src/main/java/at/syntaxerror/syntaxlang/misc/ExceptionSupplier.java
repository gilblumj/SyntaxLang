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
package at.syntaxerror.syntaxlang.misc;

import java.util.function.Supplier;

/**
 * @author SyntaxError
 * 
 */
public interface ExceptionSupplier<R, T extends Throwable> {

	R get() throws T;
	
	default Supplier<R> asSupplier() {
		return () -> {
			try {
				return get();
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		};
	}
	
}
