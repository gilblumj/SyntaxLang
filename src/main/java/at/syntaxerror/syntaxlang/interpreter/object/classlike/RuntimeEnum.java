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
import at.syntaxerror.syntaxlang.interpreter.object.variable.RuntimeVariable;
import at.syntaxerror.syntaxlang.parser.node.EnumConstantNode;
import at.syntaxerror.syntaxlang.parser.node.EnumNode;
import at.syntaxerror.syntaxlang.parser.node.FuncDefNode;
import at.syntaxerror.syntaxlang.parser.node.Node;
import at.syntaxerror.syntaxlang.parser.node.VarDeclNode;
import at.syntaxerror.syntaxlang.trace.Position;
import lombok.Getter;
import lombok.NonNull;

/**
 * @author SyntaxError
 * 
 */
@Getter
public class RuntimeEnum implements RuntimeClassLike {

	private final AccessModifier accessModifier;
	
	private final String name;

	private final List<RuntimeInterface> implemented;

	private final Map<String, RuntimeClassLikeInstance> constants;
	private final Map<String, RuntimeFunction> functions;
	private final Map<String, RuntimeVariable> variables;
	
	private final List<String> constantNames;
	
	private final RuntimeEnvironment environment;
	
	private final Position position;
	private final EnumNode node;
	
	private boolean mayCallConstructor;
	
	public RuntimeEnum(@NonNull RuntimeEnvironment env, @NonNull EnumNode node) {
		environment = env = env.buildChild();

		position = node.getPosition();
		this.node = node;
		
		accessModifier = AccessModifier.valueOf(node.getModAccess());
		
		name = node.getName().identifierValue();
		
		if(env.resolveLocalClass(name).isResolved())
			env.getInputEnvironment().terminate("Duplicate class declaration", node.getName().getPosition());
		
		this.implemented = new ArrayList<>();
		
		for(Node implemented : node.getImplementedClasses()) {
			Resolved<RuntimeClassLike> resolved = env.resolveClass(implemented);
			
			if(!resolved.isResolved())
				env.getInputEnvironment().terminate("Cannot implement unknown interface", implemented.getPosition());
			
			if(!resolved.isInstance(RuntimeInterface.class))
				env.getInputEnvironment().terminate("Can only implement interfaces", implemented.getPosition());
			
			this.implemented.add(resolved.getAs(RuntimeInterface.class));
		}
		
		constants = new HashMap<>();
		functions = new HashMap<>();
		variables = new HashMap<>();
		
		constantNames = new ArrayList<>();
		
		for(EnumConstantNode constant : node.getConstants())
			constantNames.add(constant.getName().identifierValue());
		
		for(FuncDefNode fun : node.getBody().getFunctions()) {
			String funName = fun.getName().identifierValue();
			
			if(variables.containsKey(funName) || functions.containsKey(funName) || constantNames.contains(funName))
				env.getInputEnvironment().terminate("Duplicate function declaration", node.getName().getPosition());
			
			functions.put(funName, new RuntimeFunction(env, fun));
		}
		
		for(VarDeclNode var : node.getBody().getVariables()) {
			String varName = var.getName().identifierValue();
			
			if(variables.containsKey(varName) || functions.containsKey(varName) || constantNames.contains(varName))
				env.getInputEnvironment().terminate("Duplicate variable declaration", node.getName().getPosition());
			
			variables.put(varName, new RuntimeVariable(env, var));
		}
		
		mayCallConstructor = true;
	}
	
	@Override
	public void init() {
		// make sure all class-likes are registered before resolving implemented interfaces
		
		for(Node implemented : node.getImplementedClasses()) {
			Resolved<RuntimeClassLike> resolved = environment.resolveClass(implemented);
			
			if(!resolved.isResolved())
				environment.getInputEnvironment().terminate("Cannot implement unknown interface", implemented.getPosition());
			
			if(!resolved.isInstance(RuntimeInterface.class))
				environment.getInputEnvironment().terminate("Can only implement interfaces", implemented.getPosition());
			
			this.implemented.add(resolved.getAs(RuntimeInterface.class));
		}
		
		// TODO make enum constants
		
		
		
		mayCallConstructor = false;
	}
	
	@Override
	public boolean canAccess(@NonNull AccessOptions options) {
		return switch(getAccessModifier()) {
		case PRIVATE -> options.sameFile();
		case DEFAULT -> true;
		default -> false;
		};
	}
	
	@Override
	public Member resolveMember(@NonNull AccessOptions options, @NonNull String name) {
		if(name.equals(this.name) && !mayCallConstructor)
			return Member.EMPTY;
		
		if(!canAccess(options))
			return Member.EMPTY;
		
		if(functions.containsKey(name)) {
			RuntimeFunction fun = functions.get(name);
			
			if(!fun.canAccess(options))
				return Member.EMPTY;
			
			return new Member(functions.get(name));
		}
		
		if(variables.containsKey(name)) {
			RuntimeVariable var = variables.get(name);
			
			if(!var.canAccess(options))
				return Member.EMPTY;
			
			return new Member(variables.get(name));
		}
		
		return Member.EMPTY;
	}
	
	@Override
	public boolean isInstanceof(RuntimeClassLike type) { // TODO circular reference?
		if(type == this)
			return true;
		
		for(RuntimeInterface implemented : this.implemented)
			if(implemented.isInstanceof(type))
				return true;
		
		return false;
	}
	
}
