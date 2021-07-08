package me.connor.util;

import java.lang.reflect.*;
import java.util.*;

import me.connor.justdesserts.*;
import me.connor.justdesserts.annotations.DeserializeMethod;
import me.connor.justdesserts.annotations.SerializeMethod;
import me.connor.justdesserts.exceptions.NoSerialHandlerException;
import me.connor.justdesserts.exceptions.NoSerialMethodException;
import me.connor.justdesserts.exceptions.SerialException;

public class SerialUtils {
	
	public static Set<Field> getFields(@Nonnull Class<?> serialClass) {
		Assert.notNull(serialClass);
		Set<Field> fields = new HashSet<>();
		Class<?> currentClass = serialClass;
		do {
			for (Field field : currentClass.getDeclaredFields()) {
				int modifiers = field.getModifiers();
				if (!Modifier.isTransient(modifiers) && !Modifier.isStatic(modifiers)) fields.add(field);
			}
		} while ((currentClass = currentClass.getSuperclass()) != null && currentClass != Object.class);
		return Set.copyOf(fields);
	}
	
	public static Method getSerializeMethod(@Nonnull Class<? extends Serial> HandlerClass) throws SerialException, NoSerialMethodException {
		Assert.notNull(HandlerClass);
		for (Method method : HandlerClass.getDeclaredMethods()) {
			if (method.isAnnotationPresent(SerializeMethod.class)) {
				checkMethodModifiers(method);
				if (method.getParameterCount() != 1)
					throw new SerialException("Serialize Method in Class " + HandlerClass.getCanonicalName() + " must have one parameter");
				if (method.getReturnType() == null)
					throw new SerialException("Serialize Method in Class " + HandlerClass.getCanonicalName() + " must return an Object");
				return method;
			}
		}
		throw new NoSerialMethodException();
	}
	
	public static Method getDeserializeMethod(@Nonnull Class<? extends Serial> HandlerClass) throws SerialException, NoSerialMethodException {
		Assert.notNull(HandlerClass);
		for (Method method : HandlerClass.getDeclaredMethods()) {
			if (method.isAnnotationPresent(DeserializeMethod.class)) {
				checkMethodModifiers(method);
				if (method.getParameterCount() != 1)
					throw new SerialException("Deserialize Method in Class " + HandlerClass.getCanonicalName() + " must have one parameter");
				if (method.getReturnType() == null || !method.getReturnType().equals(HandlerClass))
					throw new SerialException("Deserialize Method in Class " + HandlerClass.getCanonicalName() + " must return an instance of itself");
				return method;
			}
		}
		throw new NoSerialMethodException();
	}
	
	private static void checkMethodModifiers(@Nonnull Method method) throws SerialException {
		Assert.notNull(method);
		int modifiers = method.getModifiers();
		if (Modifier.isAbstract(modifiers))
			throw new SerialException("Method " + method.getName() + " in Class " + method.getDeclaringClass().getCanonicalName() + " must not be abstract");
		if (!Modifier.isStatic(modifiers))
			throw new SerialException("Method " + method.getName() + " in Class " + method.getDeclaringClass().getCanonicalName() + " should be static");
		boolean exceptionFound = false;
		for (Class<?> exception : method.getExceptionTypes()) {
			if (exception.isAssignableFrom(SerialException.class)) {
				exceptionFound = true;
				break;
			}
		}
		if (!exceptionFound) throw new SerialException("Method " + method.getName() + " in Class " + method.getDeclaringClass().getCanonicalName() + " should throw a SerializationException in its Method signature");
	}
	
	public static <S extends Serial> SerialHandler<S, ?> getDeclaredHandler(@Nonnull Class<S> serialClass) throws SerialException, NoSerialHandlerException {
		Assert.notNull(serialClass);
		try {
			Field handlerField = serialClass.getDeclaredField(SerialHandler.FIELD_NAME);
			if (!handlerField.getType().equals(SerialHandler.class)) throw new SerialException("Field " + SerialHandler.FIELD_NAME + " in Class " + serialClass.getCanonicalName() + " must hold a SerialHandler");
			int modifiers = handlerField.getModifiers();
			if (!Modifier.isStatic(modifiers)) throw new SerialException("Field " + SerialHandler.FIELD_NAME + " in Class " + serialClass.getCanonicalName() + " must be static");
			if (!Modifier.isFinal(modifiers)) throw new SerialException("Field " + SerialHandler.FIELD_NAME + " in Class " + serialClass.getCanonicalName() + " must be final");
			SerialHandler<S, ?> handler = (SerialHandler<S, ?>) handlerField.get(null);
			if (handler == null) throw new SerialException("Field " + SerialHandler.FIELD_NAME + " in Class " + serialClass.getCanonicalName() + " must not be null");
			if (!handler.serialType().equals(serialClass)) throw new SerialException("Declared SerialHandler in Class " + serialClass.getCanonicalName() + " does not contain said Class");
			return handler;
		} catch (NoSuchFieldException e) {
			throw new NoSerialHandlerException();
		} catch (SecurityException | IllegalArgumentException | IllegalAccessException | ClassCastException e) {
			throw new SerialException("Exception when trying to find declared SerialHandler for Class " + serialClass.getCanonicalName(), e);
		}
	}
	
	public static <S> Constructor<S> getConstructor(@Nonnull Class<S> serialClass) throws SerialException {
		try {
			return serialClass.getConstructor();
		} catch (NoSuchMethodException e1) {//TODO: Get rid of the nested try block
			try {
				return serialClass.getDeclaredConstructor();
			} catch (NoSuchMethodException e2) {
				throw new SerialException("A valid no-args Constructor does not exist to derive a Supplier for Class " + serialClass.getCanonicalName());
			}
		}
	}
	
	public static final String ARTIFACT_PREFIX = "$!";
	
	public static final String CLASS_ARTIFACT_KEY = ARTIFACT_PREFIX + "CLASS";
	
	public static final String KEY_CLASS_ARTIFACT_KEY = ARTIFACT_PREFIX + "KEY_CLASS";
	
	public static final String VALUE_CLASS_ARTIFACT_KEY = ARTIFACT_PREFIX + "VALUE_CLASS";
	
	public static Class<?> parseClassArtifact(@Nonnull Map<String, Object> data) throws SerialException, ClassNotFoundException {
		Assert.notNull(data);
		try {
			String className = (String) data.get(CLASS_ARTIFACT_KEY);
			if (className == null) throw new SerialException("Class Artifact missing");
			return Class.forName(className);
		} catch (ClassCastException e) {
			throw new SerialException("Class Artifact is not a String");
		}
	}
}
