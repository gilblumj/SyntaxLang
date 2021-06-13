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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import at.syntaxerror.syntaxlang.input.InputEnvironment;
import at.syntaxerror.syntaxlang.interpreter.object.AccessOptions;
import at.syntaxerror.syntaxlang.interpreter.object.Member;
import at.syntaxerror.syntaxlang.interpreter.object.function.RuntimeCallData;
import at.syntaxerror.syntaxlang.interpreter.object.function.RuntimeFunction;
import at.syntaxerror.syntaxlang.interpreter.result.RuntimeResult;
import at.syntaxerror.syntaxlang.interpreter.value.RuntimeValue;
import at.syntaxerror.syntaxlang.interpreter.value.ValueType;
import at.syntaxerror.syntaxlang.trace.Position;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * @author SyntaxError
 * 
 */
@UtilityClass
public class SpecialFunctions {
	
	private static final List<String> FUNCTION_ONE = Arrays.asList(
		"__equ",
		"__ieq",
		
		"__and",
		"__or",
		
		"__lss",
		"__leq",
		"__gtr",
		"__geq",
		
		"__idx",
		
		"__bitand",
		"__xor",
		"__bitor",
		
		"__lsh",
		"__rsh",
		
		"__add",
		"__sub",
		"__mul",
		"__div",
		"__mod",
		"__pow"
	);
	private static final List<String> FUNCTION_ZERO = Arrays.asList(
		"__inc",
		"__dec",
		
		"__plus",
		"__minus",
		"__not",
		"__compl",
		
		"__clone",
		
		"__hashcode",
		
		"__string",
		"__number",
		"__complex",
		"__boolean"
	);
	private static final List<String> FUNCTION_ENUM = Arrays.asList(
		"name",
		"ordinal",
		"values",
		"valueOf"
	);
	
	public static void checkFunction(RuntimeClassLike classLike, RuntimeFunction function) {
		InputEnvironment input = classLike.getEnvironment().getInputEnvironment();
		String name = function.getName();
		String type = classLike.getFullName();
		int args = 0; // TODO
		
		if(classLike instanceof RuntimeEnum && FUNCTION_ENUM.contains(name))
			input.terminate(
				"Cannot override enum function %s in enum %s"
					.formatted(name, type),
				function.getPosition()
			);
		
		else if(name.equals("__clone")) {
			if(classLike instanceof RuntimeClass cls) {
				if(!cls.isCloneableModifier())
					input.terminate(
						"Class %s is not cloneable".formatted(type),
						function.getPosition()
					);
			} else input.terminate(
				"Cannot override function __clone in %s (not a class)"
					.formatted(type),
				function.getPosition()
			);
		}
		
		else if(FUNCTION_ONE.contains(name)) {
			if(args != 1)
				input.terminate(
					"Invalid number of arguments for function %s in enum %s (expected 1, got %d instead)"
						.formatted(name, type, args),
					function.getPosition()
				);
		}
		
		else if(FUNCTION_ZERO.contains(name)) {
			if(args != 0)
				input.terminate(
					"Invalid number of arguments for function %s in enum %s (expected 0, got %d instead)"
						.formatted(name, type, args),
					function.getPosition()
				);
		}
	}
	
	public static boolean isReservedName(RuntimeClassLike classLike, String name) {
		return (classLike instanceof RuntimeEnum && FUNCTION_ENUM.contains(name)) ||
				(classLike instanceof RuntimeInterface && classLike.getName().equals(name)) ||
				FUNCTION_ONE.contains(name) || FUNCTION_ZERO.contains(name);
	}
	
	public static RuntimeResult instantiate(RuntimeCallData data) {
		RuntimeClassLike target = data.type();
		String typeName = target.getFullName();
		Member member = target.resolveMember(AccessOptions.FULL, typeName);
		
		if(member.isFunction())
			return member.getFunction().call(data.inst(), data.args());
		
		return error(data, typeName);
	}
	
	private static boolean compare(RuntimeValue[] a, RuntimeCallData b) {
		return a[0].isObject() && b.inst() == a[0].objectValue(); 
	}
	
	public static RuntimeResult equ(RuntimeCallData data) {
		return callBoolean("__equ", data, other -> compare(other, data), 1);
	}
	public static RuntimeResult ieq(RuntimeCallData data) {
		return callBoolean("__ieq", data, other -> compare(other, data), 1);
	}
	
	public static RuntimeResult and(RuntimeCallData data) {
		return callBoolean("__and", data, "&&", 1);
	}
	public static RuntimeResult or(RuntimeCallData data) {
		return callBoolean("__or", data, "||", 1);
	}

	public static RuntimeResult lss(RuntimeCallData data) {
		return callBoolean("__lss", data, "<", 1);
	}
	public static RuntimeResult leq(RuntimeCallData data) {
		return callBoolean("__leq", data, "<=", 1);
	}
	public static RuntimeResult gtr(RuntimeCallData data) {
		return callBoolean("__gtr", data, ">", 1);
	}
	public static RuntimeResult geq(RuntimeCallData data) {
		return callBoolean("__geq", data, ">=", 1);
	}
	
	public static RuntimeResult idx(RuntimeCallData data) {
		return callArgs("__idx", data, "[x]", 1);
	}
	
	public static RuntimeResult bitand(RuntimeCallData data) {
		return callArgs("__bitand", data, "&", 1);
	}
	public static RuntimeResult xor(RuntimeCallData data) {
		return callArgs("__xor", data, "^", 1);
	}
	public static RuntimeResult bitor(RuntimeCallData data) {
		return callArgs("__bitor", data, "|", 1);
	}

	public static RuntimeResult lsh(RuntimeCallData data) {
		return callArgs("__lsh", data, "<<", 1);
	}
	public static RuntimeResult rsh(RuntimeCallData data) {
		return callArgs("__rsh", data, ">>", 1);
	}

	public static RuntimeResult add(RuntimeCallData data) {
		return callArgs("__add", data, "+", 1);
	}
	public static RuntimeResult sub(RuntimeCallData data) {
		return callArgs("__sub", data, "-", 1);
	}
	public static RuntimeResult mul(RuntimeCallData data) {
		return callArgs("__mul", data, "*", 1);
	}
	public static RuntimeResult div(RuntimeCallData data) {
		return callArgs("__div", data, "/", 1);
	}
	public static RuntimeResult mod(RuntimeCallData data) {
		return callArgs("__mod", data, "%", 1);
	}
	public static RuntimeResult pow(RuntimeCallData data) {
		return callArgs("__pow", data, "**", 1);
	}

	public static RuntimeResult inc(RuntimeCallData data) {
		return callArgs("__inc", data, "++", 1);
	}
	public static RuntimeResult dec(RuntimeCallData data) {
		return callArgs("__dec", data, "--", 1);
	}
	
	public static RuntimeResult plus(RuntimeCallData data) {
		return callArgs("__plus", data, "+", 0);
	}
	public static RuntimeResult minus(RuntimeCallData data) {
		return callArgs("__minus", data, "-", 0);
	}
	public static RuntimeResult not(RuntimeCallData data) {
		return callArgs("__not", data, "!", 0);
	}
	public static RuntimeResult complement(RuntimeCallData data) {
		return callArgs("__compl", data, "~", 0);
	}

	public static RuntimeResult clone(RuntimeCallData data) {
		InputEnvironment input = data.env().getInputEnvironment();
		
		if(data.inst().getType() instanceof RuntimeClass cls) {
			if(cls.isCloneableModifier())
				return callArgs("__clone", data, "clone", 0);
			
			input.terminate("Class %s is not cloneable".formatted(cls.getFullName()), data.called());
		}
		
		input.terminate("Cannot clone %s (not a class)".formatted(data.inst().getFullName()), data.called());
		return error(data, "clone");
	}
	
	public static RuntimeResult hashcode(RuntimeCallData data) {
		return expect("__hashcode", data, ValueType.NUMBER, callArgs("__hashcode", data, other ->
			new RuntimeResult().value(
				RuntimeValue.of(data.called(), data.inst().hashCode())
			), 0));
	}

	public static RuntimeResult string(RuntimeCallData data) {
		return expect("__string", data, ValueType.STRING, callArgs("__string", data, other -> {
			RuntimeClassLikeInstance inst = data.inst();
			
			RuntimeResult hashcode = hashcode(data);
			
			if(!hashcode.hasValue())
				return hashcode;
			
			BigInteger hc = hashcode.getValue().numberValue().toBigInteger();
			
			return new RuntimeResult().value(
				RuntimeValue.of(data.called(), "%s@%s".formatted(inst.getFullName(), hc.toString(16)))
			);
		}, 0));
	}
	public static RuntimeResult number(RuntimeCallData data) {
		return callNumber("__string", data, "__string", 0);
	}
	public static RuntimeResult complex(RuntimeCallData data) {
		return callComplex("__string", data, "__string", 0);
	}
	public static RuntimeResult boolean0(RuntimeCallData data) {
		return callBoolean("__boolean", data, "__boolean", 0);
	}

	public static RuntimeResult enumName(RuntimeCallData data) {
		String typeName = data.type().getFullName();
		InputEnvironment input = data.env().getInputEnvironment();
		
		if(data.inst() == null)
			input.terminate("Function 'name' in class %s is not static".formatted(typeName), data.called());
		
		if(data.inst().getType() instanceof RuntimeEnum enm) {
			expectArgs("name", data, 0);
			
			Map<String, RuntimeClassLikeInstance> constants = enm.getConstants();
			
			for(String constantName : constants.keySet())
				if(constants.get(constantName) == data.inst())
					return new RuntimeResult().value(
						RuntimeValue.of(data.called(), constantName)
					);
			
			input.terminate("Unknown enum constant for enum %s".formatted(typeName), data.called());
		}

		input.terminate("Cannot get name of %s (not an enum)".formatted(typeName), data.called());
		return error(data, "name");
	}
	public static RuntimeResult enumOrdinal(RuntimeCallData data) {
		String typeName = data.type().getFullName();
		InputEnvironment input = data.env().getInputEnvironment();
		
		if(data.inst() == null)
			input.terminate("Function 'name' in class %s is not static".formatted(typeName), data.called());
		
		if(data.inst().getType() instanceof RuntimeEnum enm) {
			expectArgs("ordinal", data, 0);
			
			Map<String, RuntimeClassLikeInstance> constants = enm.getConstants();
			List<String> constantNames = enm.getConstantNames();
			
			for(String constantName : constantNames)
				if(constants.get(constantName) == data.inst())
					return new RuntimeResult().value(
						RuntimeValue.of(data.called(), constantNames.indexOf(constantName))
					);
			
			input.terminate("Unknown enum constant for enum %s".formatted(typeName), data.called());
		}

		input.terminate("Cannot get ordinal of %s (not an enum)".formatted(typeName), data.called());
		return error(data, "ordinal");
	}
	
	public static RuntimeResult enumValues(RuntimeCallData data) {
		String typeName = data.type().getFullName();
		InputEnvironment input = data.env().getInputEnvironment();
		
		if(data.inst().getType() instanceof RuntimeEnum enm) {
			expectArgs("values", data, 0);
			
			Map<String, RuntimeClassLikeInstance> constants = enm.getConstants();
			List<String> constantNames = enm.getConstantNames();
			
			List<RuntimeValue> orderedConstants = new ArrayList<>();
			
			for(String constantName : constantNames)
				orderedConstants.add(
					RuntimeValue.of(data.called(), constants.get(constantName))
				);
			
			return new RuntimeResult().value(
				RuntimeValue.of(data.called(), orderedConstants)
			);
		}

		input.terminate("Cannot get values of %s (not an enum)".formatted(typeName), data.called());
		return error(data, "values");
	}
	
	public static RuntimeResult enumValueOf(RuntimeCallData data) {
		String typeName = data.type().getFullName();
		InputEnvironment input = data.env().getInputEnvironment();
		
		if(data.inst().getType() instanceof RuntimeEnum enm) {
			expectArgs("valueOf", data, 1);
			
			Map<String, RuntimeClassLikeInstance> constants = enm.getConstants();
			List<String> constantNames = enm.getConstantNames();
			
			String name = data.args()[0].stringValue().toString();
			
			if(constantNames.contains(name))
				return new RuntimeResult().value(
					RuntimeValue.of(data.called(), constants.get(name))
				);
			
			return new RuntimeResult().throwException(
				data.env().buildError(
					RuntimeValue.of(data.called(), "Unknown enum constant " + name),
					typeName,
					"values",
					data.called()
				)
			);
		}

		input.terminate("Cannot get valueOf of %s (not an enum)".formatted(typeName), data.called());
		return error(data, "valueOf");
	}
	
	//
	
	private static RuntimeResult callArgs(@NonNull String name, @NonNull RuntimeCallData data,
			@NonNull Function<RuntimeValue[], RuntimeResult> elseSupplier, int args) {
		
		RuntimeClassLike target = data.type();
		
		String typeName = target.getFullName();
		
		if(data.inst() == null)
			data.env().getInputEnvironment().terminate(
				"Function '%s' in class %s is not static".formatted(name, typeName),
				data.called()
			);

		expectArgs(name, data, args);
		
		Member member = target.resolveMember(AccessOptions.FULL, name);
		
		if(member.isVariable())
			return error(data, name);
		
		if(member.isFunction())
			return member.getFunction().call(data.inst(), data.args());
		
		return elseSupplier.apply(data.args());
	}
	
	private static RuntimeResult callArgs(String name, RuntimeCallData data, String humanReadableName, int args) {
		return callArgs(name, data, other -> {
			data.env().getInputEnvironment().terminate(
				"Cannot use '%s' on object of type %s".formatted(
					humanReadableName,
					data.type().getFullName()
				),
				data.called()
			);
			return error(data, name);
		}, args);
	}
	
	private static RuntimeResult callNumber(String name, RuntimeCallData data, String humanReadableName, int args) {
		return expect(name, data, ValueType.NUMBER, callArgs(name, data, humanReadableName, args));
	}
	
	private static RuntimeResult callComplex(String name, RuntimeCallData data, String humanReadableName, int args) {
		return expect(name, data, ValueType.COMPLEX, callArgs(name, data, humanReadableName, args));
	}
	
	private static RuntimeResult callBoolean(String name, RuntimeCallData data, String humanReadableName, int args) {
		return expect(name, data, ValueType.BOOLEAN, callArgs(name, data, humanReadableName, args));
	}
	
	private static RuntimeResult callBoolean(@NonNull String name, @NonNull RuntimeCallData data, 
			@NonNull Function<RuntimeValue[], Boolean> elseSupplier, int args) {
		return expect(name, data, ValueType.BOOLEAN, callArgs(name, data, other ->
			new RuntimeResult().value(
				RuntimeValue.of(data.called(), elseSupplier.apply(other))
			),
			args
		));
	}
	
	private static RuntimeResult error(RuntimeCallData data, String name) {
		return RuntimeResult.unexpected(data.env(), data.called(), data.type().getName(), name);
	}
	
	private static void expectArgs(String name, RuntimeCallData data, int args) {
		data.env().getInputEnvironment().terminate(
			"Invalid number of arguments for function %s in class %s"
				.formatted(name, data.type().getFullName()),
			data.called()
		);
	}
	
	private static RuntimeResult expect(String name, RuntimeCallData data, ValueType type, RuntimeResult result) {
		if(result.isThrow())
			return result;

		Position pos = data.called();
		
		if(result.isNothing() && result.hasValue()) {
			ValueType resultType = result.getValue().getType();
			
			if(resultType != type) {
				data.env().getInputEnvironment().terminate(
					"Expected return value of %s in class %s to be of type %s, got %s instead at position %s"
						.formatted(name, data.type().getFullName(), type, resultType, pos),
					pos
				);
				return error(data, name);
			}
		} else {
			data.env().getInputEnvironment().terminate(
				"Function %s in class %s did not properly return at position %s"
					.formatted(name, data.type().getFullName(), pos),
				pos
			);
			return error(data, name);
		}
		
		return result;
	}

}
