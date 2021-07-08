package me.connor.justdesserts;

import java.lang.reflect.*;

import me.connor.justdesserts.exceptions.NoSerialHandlerException;
import me.connor.justdesserts.exceptions.SerialException;
import me.connor.util.*;

@FunctionalInterface
public interface PartialSerialHandler<S, D> {
	
	public static final String FIELD_NAME = "PARTIAL_HANDLER";

	public SerialHandler<S, D> create(@Nonnull SerialDeriver<? super S, ? super D> deriver);
	
	public static <S> PartialSerialHandler<S, ?> forClass(@Nonnull Class<S> serialType) throws NoSerialHandlerException {
		Assert.notNull(serialType);
		try {
			Field handlerField = serialType.getDeclaredField(FIELD_NAME);
			if (!handlerField.getType().equals(PartialSerialHandler.class)) throw new SerialException("Field " + FIELD_NAME + " in Class " + serialType.getCanonicalName() + " must hold a SerialHandler");
			int modifiers = handlerField.getModifiers();
			if (!Modifier.isStatic(modifiers)) throw new SerialException("Field " + FIELD_NAME + " in Class " + serialType.getCanonicalName() + " must be static");
			//if (!Modifier.isFinal(modifiers)) throw new SerialException("Field " + FIELD_NAME + " in Class " + serialType.getCanonicalName() + " must be final");//TODO: Should I enforce this?
			PartialSerialHandler<S, ?> handler = (PartialSerialHandler<S, ?>) handlerField.get(null);
			if (handler == null) throw new SerialException("Field " + FIELD_NAME + " in Class " + serialType.getCanonicalName() + " must not be null");
			return handler;
		} catch (NoSuchFieldException e) {
			throw new NoSerialHandlerException();
		} catch (Throwable t) {
			throw new SerialException("Exception when trying to find declared PartialSerialHandler for Class " + serialType.getCanonicalName(), t);
		}
	}
	
}
