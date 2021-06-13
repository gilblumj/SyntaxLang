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
package at.syntaxerror.syntaxlang.interpreter.object.classlike;

import at.syntaxerror.syntaxlang.input.InputEnvironment;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Delegate;

/**
 * @author SyntaxError
 * 
 */
@Getter
public class RuntimeClassLikeInstance implements RuntimeClassLike {

	@Delegate
	private final RuntimeClassLike type;
	
	public RuntimeClassLikeInstance(@NonNull RuntimeClassLike type) {
		this.type = type;
		
		InputEnvironment input = type.getEnvironment().getInputEnvironment();
		String name = type.getFullName();
		
		if(type instanceof RuntimeInterface)
			input.terminate(
				"Cannot instantiate interface %s".formatted(name),
				getPosition()
			);
		
		if(type instanceof RuntimeEnum enm && !enm.isMayCallConstructor())
			input.terminate(
				"Cannot instantiate enum %s".formatted(name),
				getPosition()
			);
		
		if(type instanceof RuntimeClass cls && cls.isAbstractModifier())
			input.terminate(
				"Cannot instantiate abstract class %s".formatted(name),
				getPosition()
			);
	}

}
