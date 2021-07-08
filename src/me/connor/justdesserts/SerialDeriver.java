package me.connor.justdesserts;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import me.connor.util.*;

public interface SerialDeriver<S, D> extends Qualifier<Class<?>> {
	
	<U extends S> SerialHandler<U, D> derive(@Nonnull Class<U> serialType) throws IllegalArgumentException;
	
	public static <S, D> SerialDeriver<S, D> from(@Nonnull Qualifier<Class<?>> qualifier, @Nonnull Function<Class<? extends S>, SerialHandler<? extends S, D>> deriver) {
		return new BaseSerialDeriver<>(qualifier, deriver);
	}
	
	public static <S, D> SerialDeriver<S, D> from(@Nonnull Function<Class<? extends S>, SerialHandler<? extends S, D>> deriver) {
		return new BaseSerialDeriver<>((c) -> {
			try {
				return deriver.apply((Class<? extends S>) c) != null;
			} catch (Throwable t) {
				return false;
			}
		}, deriver);
	}
	
	public static <S, D> SerialDeriver<S, D> from(@Nonnull SerialHandler<S, D> handler) throws IllegalArgumentException {
		Assert.notNull(handler);
		return from(
				Qualifier.exactClass(handler.serialType()),
				(c) -> handler
				);
	}
	
	@SuppressWarnings("rawtypes")
	public static <S, D> SerialDeriver<S, D> from(@Nonnull SerialHandler<? extends S, ? extends D>...handlers) throws IllegalArgumentException {
		Assert.notNull(handlers);
		if (handlers.length == 0) throw new IllegalArgumentException("SerialHandler array cannot be empty");
		if (handlers.length == 1) return from((SerialHandler<S, D>) handlers[0]);
		Map<Class, SerialHandler> handlerMap = List.of(handlers)
				.stream()
				.collect(Collectors.toMap(
				SerialHandler::serialType,
				(h) -> h
				));
		return from(
				(c) -> handlerMap.containsKey(c),
				(c) ->  handlerMap.get(c)
				);
	}
	
	@SuppressWarnings("rawtypes")
	public static <S, D> SerialDeriver<S, D> merge(@Nonnull SerialDeriver<? extends S, ? extends D>...derivers) {
		Assert.notNull(derivers);
		List<SerialDeriver> list = List.of(derivers);
		return from(
				Qualifier.merge(Qualifier.ReturnCondition.ANY_ACCEPTS, derivers),
				(c) -> {
					for (SerialDeriver deriver : list) {
						if (deriver.accepts(c)) return deriver.derive(c);
					}
					throw new IllegalArgumentException("This SerialDeriver does not accept Class " + c.getCanonicalName());
				}
				);
	}
	
}
