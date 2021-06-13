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
package at.syntaxerror.syntaxlang.parser.node;

import lombok.NonNull;
import lombok.ToString;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author SyntaxError
 * 
 */
@AllArgsConstructor
@Getter
@ToString
@NonNull
public class FuncCallNode extends Node {

	private final Node target;
	private final FuncCallArgsNode args;

}
