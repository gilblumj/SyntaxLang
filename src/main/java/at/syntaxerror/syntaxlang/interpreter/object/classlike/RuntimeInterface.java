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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.syntaxerror.syntaxlang.interpreter.Resolved;
import at.syntaxerror.syntaxlang.interpreter.RuntimeEnvironment;
import at.syntaxerror.syntaxlang.interpreter.object.AccessModifier;
import at.syntaxerror.syntaxlang.interpreter.object.AccessOptions;
import at.syntaxerror.syntaxlang.interpreter.object.Member;
import at.syntaxerror.syntaxlang.interpreter.object.function.RuntimeFunction;
import at.syntaxerror.syntaxlang.parser.node.FuncDefNode;
import at.syntaxerror.syntaxlang.parser.node.InterfaceNode;
import at.syntaxerror.syntaxlang.parser.node.Node;
import at.syntaxerror.syntaxlang.trace.Position;
import lombok.Getter;
import lombok.NonNull;

/**
 * @author SyntaxError
 * 
 */
@Getter
public class RuntimeInterface implements RuntimeClassLike {

	private final AccessModifier accessModifier;
	
	private final String name;

	private final List<RuntimeInterface> extended;
	
	private final Map<String, RuntimeFunction> functions;
	
	private final RuntimeEnvironment environment;
	
	private final Position position;
	private final InterfaceNode node;
	
	public RuntimeInterface(@NonNull RuntimeEnvironment env, @NonNull InterfaceNode node) {
		environment = env = env.buildChild();
		
		position = node.getPosition();
		this.node = node;
		
		accessModifier = AccessModifier.valueOf(node.getModAccess());
		
		name = node.getName().identifierValue();
		
		if(env.resolveLocalClass(name).isResolved())
			env.getInputEnvironment().terminate("Duplicate interface declaration", node.getName().getPosition());

		extended = new ArrayList<>();
		functions = new HashMap<>();
		
		for(FuncDefNode fun : node.getBody().getFunctions()) {
			String funName = fun.getName().identifierValue();
			
			if(functions.containsKey(funName))
				env.getInputEnvironment().terminate("Duplicate function declaration", node.getName().getPosition());
			
			RuntimeFunction function = new RuntimeFunction(env, fun);
			
			if(function.isAbstractModifier() || function.isFinalModifier())
				env.getInputEnvironment().terminate("Interface function cannot be abstract or final", 
					fun.getModAbstractFinalDefault().getPosition());
			
			if(function.getAccessModifier() == AccessModifier.PRIVATE || function.getAccessModifier() == AccessModifier.PROTECTED)
				env.getInputEnvironment().terminate("Interface function cannot be private or protected", 
						fun.getModAccess().getPosition());
			
			functions.put(funName, function);
		}
	}
	
	@Override
	public void init() {
		// make sure all class-likes are registered before resolving extended interfaces
		
		for(Node extended : node.getExtendedClasses()) {
			Resolved<RuntimeClassLike> resolved = environment.resolveClass(extended);
			
			if(!resolved.isResolved())
				environment.getInputEnvironment().terminate("Cannot extend unknown interface", extended.getPosition());
			
			if(!resolved.isInstance(RuntimeInterface.class))
				environment.getInputEnvironment().terminate("Can only extend interfaces", extended.getPosition());
			
			this.extended.add(resolved.getAs(RuntimeInterface.class));
		}
	}
	
	@Override
	public Member resolveMember(@NonNull AccessOptions options, @NonNull String name) {
		if(!canAccess(options))
			return Member.EMPTY;
		
		if(functions.containsKey(name)) {
			RuntimeFunction fun = functions.get(name);
			
			if(!fun.canAccess(options))
				return Member.EMPTY;
			
			return new Member(functions.get(name));
		}
		
		return Member.EMPTY;
	}
	
	@Override
	public boolean isInstanceof(RuntimeClassLike type) { // TODO circular reference?
		if(type == this)
			return true;
		
		for(RuntimeInterface extended : this.extended)
			if(extended.isInstanceof(type))
				return true;
		
		return false;
	}
	
}
