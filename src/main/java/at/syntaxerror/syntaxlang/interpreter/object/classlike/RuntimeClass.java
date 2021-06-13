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
import java.util.Optional;

import at.syntaxerror.syntaxlang.interpreter.Resolved;
import at.syntaxerror.syntaxlang.interpreter.RuntimeEnvironment;
import at.syntaxerror.syntaxlang.interpreter.object.AccessModifier;
import at.syntaxerror.syntaxlang.interpreter.object.AccessOptions;
import at.syntaxerror.syntaxlang.interpreter.object.Member;
import at.syntaxerror.syntaxlang.interpreter.object.function.RuntimeFunction;
import at.syntaxerror.syntaxlang.interpreter.object.variable.RuntimeVariable;
import at.syntaxerror.syntaxlang.lexer.Keyword;
import at.syntaxerror.syntaxlang.lexer.Token;
import at.syntaxerror.syntaxlang.parser.node.ClassNode;
import at.syntaxerror.syntaxlang.parser.node.EmptyNode;
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
public class RuntimeClass implements RuntimeClassLike {

	private final AccessModifier accessModifier;
	
	private final boolean cloneableModifier;
	private final boolean abstractModifier;
	private final boolean finalModifier;
	
	private final String name;

	private Optional<RuntimeClass> extended;
	private final List<RuntimeInterface> implemented;
	
	private final Map<String, RuntimeFunction> functions;
	private final Map<String, RuntimeVariable> variables;
	
	private final RuntimeEnvironment environment;
	
	private final Position position;
	private final ClassNode node;
	
	public RuntimeClass(@NonNull RuntimeEnvironment env, @NonNull ClassNode node) {
		environment = env = env.buildChild();
		
		position = node.getPosition();
		this.node = node;
		
		accessModifier = AccessModifier.valueOf(node.getModAccess());
		cloneableModifier = node.getModCloneable() != null;
		
		Token modAbstractFinal = node.getModAbstractFinal();
		
		if(modAbstractFinal != null) {
			abstractModifier = modAbstractFinal.is(Keyword.ABSTRACT);
			finalModifier = modAbstractFinal.is(Keyword.FINAL);
		} else abstractModifier = finalModifier = false;
		
		name = node.getName().identifierValue();
		
		if(env.resolveLocalClass(name).isResolved())
			env.getInputEnvironment().terminate("Duplicate class declaration", node.getName().getPosition());
		
		extended = Optional.empty();
		implemented = new ArrayList<>();
		
		functions = new HashMap<>();
		variables = new HashMap<>();
		
		for(FuncDefNode fun : node.getBody().getFunctions()) {
			String funName = fun.getName().identifierValue();
			
			if(variables.containsKey(funName) || functions.containsKey(funName))
				env.getInputEnvironment().terminate("Duplicate function declaration", node.getName().getPosition());
			
			functions.put(funName, new RuntimeFunction(env, fun));
		}
		
		for(VarDeclNode var : node.getBody().getVariables()) {
			String varName = var.getName().identifierValue();
			
			if(variables.containsKey(varName) || functions.containsKey(varName))
				env.getInputEnvironment().terminate("Duplicate variable declaration", node.getName().getPosition());
			
			variables.put(varName, new RuntimeVariable(env, var));
		}
	}
	
	@Override
	public void init() {
		// make sure all class-likes are registered before resolving extended class and implemented interfaces
		
		Node extended = node.getExtendedClass();
		
		if(!(extended instanceof EmptyNode)) {
			Resolved<RuntimeClassLike> resolved = environment.resolveClass(extended);
			
			if(!resolved.isResolved())
				environment.getInputEnvironment().terminate("Cannot extended unknown class", extended.getPosition());
			
			if(!resolved.isInstance(RuntimeClass.class))
				environment.getInputEnvironment().terminate("Can only extend classes", extended.getPosition());
			
			this.extended = Optional.of(resolved.getAs(RuntimeClass.class));
		} else this.extended = Optional.empty();
		
		for(Node implemented : node.getImplementedClasses()) {
			Resolved<RuntimeClassLike> resolved = environment.resolveClass(implemented);
			
			if(!resolved.isResolved())
				environment.getInputEnvironment().terminate("Cannot implement unknown interface", implemented.getPosition());
			
			if(!resolved.isInstance(RuntimeInterface.class))
				environment.getInputEnvironment().terminate("Can only implement interfaces", implemented.getPosition());
			
			this.implemented.add(resolved.getAs(RuntimeInterface.class));
		}
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
		
		if(extended.isPresent()) {
			Member resolved = extended.get().resolveMember(options, name);
			
			if(resolved.isResolved())
				return resolved;
		}
		
		for(RuntimeInterface implemented : this.implemented) {
			Member resolved = implemented.resolveMember(options, name);
			
			if(resolved.isResolved())
				return resolved;
		}
		
		return Member.EMPTY;
	}
	
	@Override
	public boolean isInstanceof(RuntimeClassLike type) { // TODO circular reference?
		if(type == this)
			return true;
		
		if(extended.isPresent() && extended.get().isInstanceof(type))
			return true;
		
		for(RuntimeInterface implemented : this.implemented)
			if(implemented.isInstanceof(type))
				return true;
		
		return false;
	}
	
}
