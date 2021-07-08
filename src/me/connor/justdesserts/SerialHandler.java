package me.connor.justdesserts;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.*;

import me.connor.justdesserts.exceptions.NoSerialHandlerException;
import me.connor.justdesserts.exceptions.SerialException;
import me.connor.util.*;

public interface SerialHandler<S, D> {
	
	public static final String FIELD_NAME = "SERIAL_HANDLER";

	Class<S> serialType();
	
	D serialize(@Nonnull S obj);
	
	S deserialize(@Nonnull D data);
	
	public default <U> SerialHandler<U, D> prepend(@Nonnull SerialHandler<U, S> prefix) {
		Assert.allNotNull(prefix);
		return from(
				prefix.serialType(),
				(s) -> serialize(prefix.serialize(s)),
				(d) -> prefix.deserialize(deserialize(d))
				);
	}
	
	public default SerialHandler<S, D> unaryPrepend(@Nonnull SerialHandler<S, S> prefix) {
		return prepend(prefix);
	}
	
	public default <U> SerialHandler<S, U> append(@Nonnull SerialHandler<D, U> suffix) {
		Assert.allNotNull(suffix);
		return from(
				serialType(),
				(s) -> suffix.serialize(serialize(s)),
				(d) -> deserialize(suffix.deserialize(d))
				);
	}
	
	public default SerialHandler<S, D> unaryAppend(@Nonnull SerialHandler<D, D> suffix) {
		return append(suffix);
	}
	
	public default SerialDeriver<S, D> asDeriver() {
		return SerialDeriver.from(this);
	}
	
	public static <S, D> SerialHandler<S, D> from(@Nonnull Class<S> serialType, 
			@Nonnull Function<S, D> serializer, @Nonnull Function<D, S> deserializer) {
		return new BaseSerialHandler<>(serialType, serializer, deserializer);
	}
	
	public static <S, D, U> Function<SerialDeriver<S, U>, SerialHandler<S, D>> partialHandler(@Nonnull Class<S> serialType, @Nonnull Function<SerialHandler<S, U>, SerialHandler<S, D>> suffix) {
		Assert.allNotNull(serialType, suffix);
		return (d) -> suffix.apply(d.derive(serialType));
	}
	
	public static <S, D> Function<SerialDeriver<S, D>, SerialHandler<S, D>> unaryPartialHandler(@Nonnull Class<S> serialType, @Nonnull Function<SerialHandler<S, D>, SerialHandler<S, D>> suffix) {
		return partialHandler(serialType, suffix);
	}
	
	public static <S> SerialHandler<S, ?> forClass(@Nonnull Class<S> serialType) throws NoSerialHandlerException {
		Assert.notNull(serialType);
		try {
			Field handlerField = serialType.getDeclaredField(FIELD_NAME);
			if (!handlerField.getType().equals(SerialHandler.class)) throw new SerialException("Field " + FIELD_NAME + " in Class " + serialType.getCanonicalName() + " must hold a SerialHandler");
			int modifiers = handlerField.getModifiers();
			if (!Modifier.isStatic(modifiers)) throw new SerialException("Field " + FIELD_NAME + " in Class " + serialType.getCanonicalName() + " must be static");
			//if (!Modifier.isFinal(modifiers)) throw new SerialException("Field " + FIELD_NAME + " in Class " + serialType.getCanonicalName() + " must be final");//TODO: Should I enforce this?
			SerialHandler<S, ?> handler = (SerialHandler<S, ?>) handlerField.get(null);
			if (handler == null) throw new SerialException("Field " + FIELD_NAME + " in Class " + serialType.getCanonicalName() + " must not be null");
			if (!handler.serialType().equals(serialType)) throw new SerialException("Declared SerialHandler in Class " + serialType.getCanonicalName() + " does not contain said Class");
			return handler;
		} catch (NoSuchFieldException e) {
			throw new NoSerialHandlerException();
		} catch (SecurityException | IllegalArgumentException | IllegalAccessException | ClassCastException e) {
			throw new SerialException("Exception when trying to find declared SerialHandler for Class " + serialType.getCanonicalName(), e);
		}
	}
	
}
