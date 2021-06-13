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

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import at.syntaxerror.syntaxlang.input.InputEnvironment;
import at.syntaxerror.syntaxlang.interpreter.object.AccessOptions;
import at.syntaxerror.syntaxlang.interpreter.object.Member;
import at.syntaxerror.syntaxlang.interpreter.object.classlike.RuntimeClassLike;
import at.syntaxerror.syntaxlang.interpreter.object.classlike.RuntimeClassLikeInstance;
import at.syntaxerror.syntaxlang.interpreter.object.classlike.SpecialFunctions;
import at.syntaxerror.syntaxlang.interpreter.object.function.RuntimeCallData;
import at.syntaxerror.syntaxlang.interpreter.object.function.RuntimeFunction;
import at.syntaxerror.syntaxlang.interpreter.object.variable.RuntimeVariable;
import at.syntaxerror.syntaxlang.interpreter.result.RuntimeResult;
import at.syntaxerror.syntaxlang.interpreter.trace.RuntimeError;
import at.syntaxerror.syntaxlang.interpreter.trace.RuntimeStackTraceElement;
import at.syntaxerror.syntaxlang.interpreter.value.RuntimeValue;
import at.syntaxerror.syntaxlang.parser.node.ImportNode;
import at.syntaxerror.syntaxlang.parser.node.LiteralNode;
import at.syntaxerror.syntaxlang.parser.node.NamespaceNode;
import at.syntaxerror.syntaxlang.parser.node.Node;
import at.syntaxerror.syntaxlang.parser.node.ScopedNode;
import at.syntaxerror.syntaxlang.parser.node.UseAsNode;
import at.syntaxerror.syntaxlang.trace.Position;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * @author SyntaxError
 * 
 */
@Getter
public class RuntimeEnvironment {

	private final InputEnvironment inputEnvironment;
	
	private List<RuntimeEnvironment> imported;
	private List<String> importedFiles;
	
	private Map<String, String> namespaceMappings;
	private Map<String, List<RuntimeEnvironment>> importedNamespaces;
	
	private List<RuntimeStackTraceElement> stackTrace;
	
	private Map<String, RuntimeFunction> functions;
	private Map<String, RuntimeVariable> variables;
	private Map<String, RuntimeClassLike> classLikes;
	
	private RuntimeEnvironment parent;

	@Setter
	private boolean mayManageImports;
	
	@Setter
	private boolean mayDeclareNamespace;
	private String namespace;
	
	public RuntimeEnvironment(InputEnvironment input) {
		this.inputEnvironment = input;
		
		imported = new ArrayList<>();
		importedFiles = new ArrayList<>();
		
		namespaceMappings = new HashMap<>();
		importedNamespaces = new HashMap<>();
		
		stackTrace = new ArrayList<>();
		
		functions = new HashMap<>();
		variables = new HashMap<>();
		classLikes = new HashMap<>();
		
		mayManageImports = mayDeclareNamespace = true;
		namespace = null;
	}
	
	public RuntimeEnvironment setNamespace(@NonNull NamespaceNode namespace) {
		if(!mayDeclareNamespace) {
			inputEnvironment.terminate("Invalid location for namespace statement", namespace.getPosition());
			return null;
		}
		
		this.namespace = namespace.getValue().identifierValue();
		mayDeclareNamespace = false;
		
		return this;
	}
	
	public RuntimeEnvironment importFile(@NonNull ImportNode importNode) {
		if(!mayManageImports) {
			inputEnvironment.terminate("Invalid location for import statement", importNode.getPosition());
			return null;
		}
		
		File file;
		String pathname;
		
		try {
			file = Paths.get(importNode.getValue().stringValue().toString()).toAbsolutePath().toFile();
			
			pathname = file.getAbsolutePath();
		} catch (Exception e) {
			inputEnvironment.terminate("Invalid file path: " + e.getMessage(), importNode.getPosition());
			return null;
		}
		
		if(importedFiles.contains(pathname)) {
			inputEnvironment.terminate("File already imported", importNode.getPosition());
			return null;
		}
		
		if(file.isDirectory()) {
			inputEnvironment.terminate("Cannot import directory", importNode.getPosition());
			return null;
		}
		
		if(!file.getName().endsWith(".sl")) {
			inputEnvironment.terminate("Cannot import file with unsupported extension", importNode.getPosition());
			return null;
		}
		
		InputEnvironment inputEnv;
		
		try {
			inputEnv = InputEnvironment.fromFile(file);
		} catch (Exception e) {
			inputEnvironment.terminate("Cannot import file: " + e.getMessage(), importNode.getPosition());
			return null;
		}
		
		RuntimeEnvironment child = buildSibling(inputEnv, importNode.getPosition().line());
		
		// TODO execute imported data
		
		imported.add(child);
		importedFiles.add(pathname);
		
		String namespace = Objects.requireNonNullElse(child.getNamespace(), "");
		
		List<RuntimeEnvironment> envs = importedNamespaces.get(namespace);
		
		if(envs == null)
			envs = new ArrayList<>();
		
		envs.add(child);
		importedNamespaces.put(namespace, envs);
		
		return this;
	}
	
	public RuntimeEnvironment useNamespaceAs(@NonNull UseAsNode useAs) {
		if(!mayManageImports) {
			inputEnvironment.terminate("Invalid location for use-as statement", useAs.getPosition());
			return null;
		}
		
		String use = useAs.getUse().identifierValue();
		String as = useAs.getAs().identifierValue();
		
		if(namespaceMappings.containsKey(as)) {
			inputEnvironment.terminate("Alias is already defined", useAs.getPosition());
			return null;
		}
		
		namespaceMappings.put(as, use);
		
		return this;
	}

	private RuntimeEnvironment buildSibling(@NonNull InputEnvironment inputEnv, int line) {
		RuntimeEnvironment env = new RuntimeEnvironment(inputEnv);
		env.stackTrace = getStackTrace(null, null, line);
		env.parent = parent;
		env.mayDeclareNamespace = mayDeclareNamespace;
		env.mayManageImports = mayManageImports;
		return env;
	}/*
	public RuntimeEnvironment buildSibling() {
		RuntimeEnvironment env = new RuntimeEnvironment(inputEnvironment);
		env.stackTrace = getStackTrace();
		env.parent = parent;
		env.mayDeclareNamespace = mayDeclareNamespace;
		env.mayManageImports = mayManageImports;
		return env;
	}*/

	public RuntimeEnvironment buildChild(String className, String function, int line) {
		RuntimeEnvironment env = new RuntimeEnvironment(inputEnvironment);
		env.stackTrace = getStackTrace(className, function, line);
		env.parent = this;
		return env;
	}
	public RuntimeEnvironment buildChild() {
		RuntimeEnvironment env = new RuntimeEnvironment(inputEnvironment);
		env.stackTrace = getStackTrace();
		env.parent = this;
		env.mayDeclareNamespace = false;
		env.mayManageImports = false;
		return env;
	}
	
	public List<RuntimeStackTraceElement> getStackTrace() {
		return new ArrayList<>(stackTrace);
	}
	public List<RuntimeStackTraceElement> getStackTrace(String className, String function, int line) {
		List<RuntimeStackTraceElement> trace = getStackTrace();
		trace.add(new RuntimeStackTraceElement(inputEnvironment.getSource(), namespace, className, function, line));
		return trace;
	}
	
	//

	public String unmapNamespace(String namespace) {
		if(namespace == null || namespace.equals(this.namespace))
			return null;
		
		return namespaceMappings.getOrDefault(namespace, namespace);
	}
	
	public Resolved<RuntimeClassLike> resolveLocalClass(@NonNull String name) {
		if(classLikes.containsKey(name))
			return Resolved.of(classLikes.get(name));
		
		return Resolved.empty();
	}

	public Resolved<RuntimeClassLike> resolveClass(String namespace, @NonNull String name) {
		namespace = unmapNamespace(namespace);
		
		if(namespace == null) {
			Resolved<RuntimeClassLike> local = resolveLocalClass(name);
			
			if(local.isResolved())
				return local;
			
			namespace = "";
		}
		
		List<RuntimeEnvironment> envs = importedNamespaces.get(namespace);
		
		if(envs == null)
			return Resolved.empty();
		
		for(RuntimeEnvironment env : envs) {
			Resolved<RuntimeClassLike> remote = env.resolveLocalClass(name);
			
			if(remote.isResolved())
				return remote;
		}
		
		return Resolved.empty();
	}
	
	public Resolved<RuntimeClassLike> resolveClass(@NonNull Node node) {
		String namespace = null;
		String name = null;
		
		if(node instanceof LiteralNode lit)
			name = lit.getValue().identifierValue();
		else if(node instanceof ScopedNode scp) {
			if(scp.getTarget() instanceof LiteralNode lit)
				name = lit.getValue().identifierValue();
			else return Resolved.empty();
			
			namespace = scp.getScope().identifierValue();
			
			if(namespace.equals(this.namespace))
				namespace = null;
			
		} else return Resolved.empty();
		
		return resolveClass(namespace, name);
	}
	
	//
	
	public void register(RuntimeClassLike classLike) {
		String name = classLike.getName();
		
		if(classLikes.containsKey(name))
			inputEnvironment.terminate("Class is already declared", classLike.getPosition());
		
		classLikes.put(name, classLike);
	}
	
	public void register(RuntimeFunction function) {
		String name = function.getName();
		
		if(functions.containsKey(name) || variables.containsKey(name))
			inputEnvironment.terminate("Function is already declared", function.getPosition());
		
		functions.put(name, function);
	}
	public void register(RuntimeVariable variable) {
		String name = variable.getName();
		
		if(functions.containsKey(name) || variables.containsKey(name))
			inputEnvironment.terminate("Variable is already defined", variable.getPosition());
		
		variables.put(name, variable);
	}
	
	//
	
	public RuntimeError buildError(@NonNull RuntimeValue value, String className, String function, Position position) {
		RuntimeValue exception = null;
		String type = "GenericException";
		Object message = null;
		
		if(value.isObject()) {
			exception = value;
			
			RuntimeClassLikeInstance inst = value.objectValue();
			
			if(!inst.isInstanceof(null)) // TODO proper class
				inputEnvironment.terminate("Cannot throw object of type %s"
					.formatted(inst.getName()), position);
			
			Member getMessage = inst.resolveMember(AccessOptions.FULL, "getMessage");
			
			if(!getMessage.isFunction())
				inputEnvironment.terminate("Invalid exception: missing function 'getMessage'"
						.formatted(inst.getName()), position);
			
			RuntimeResult result = getMessage.getFunction().call(inst, new RuntimeValue[0]);
			
			result.terminateIfThrown();
			
			value = result.getValue();
		}
		
		if(value.isObject()) {
			RuntimeClassLikeInstance inst = value.objectValue();
			
			RuntimeResult result = SpecialFunctions.string(new RuntimeCallData(
				this,
				position,
				inst.getType(),
				inst,
				new RuntimeValue[0]
			));
			
			result.terminateIfThrown();
			
			message = result.getValue().stringValue();
		} else if(value.isString() || value.isNumber() || value.isComplex() || value.isBoolean() || value.isNull())
			message = value.getRawValue();
		else inputEnvironment.terminate("Cannot throw anonymous function", position);
		
		String strMessage = String.valueOf(message);
		
		if(exception == null) {
			RuntimeResult result = SpecialFunctions.instantiate(new RuntimeCallData(
				this,
				position,
				null, // TODO proper class
				null,
				new RuntimeValue[] {
					RuntimeValue.of(position, strMessage)
				}
			));
			
			result.terminateIfThrown();
			
			exception = result.getValue();
		}
		
		return new RuntimeError(
			exception,
			type,
			strMessage,
			getStackTrace(className, function, position.line())
		);
	}
	
}
