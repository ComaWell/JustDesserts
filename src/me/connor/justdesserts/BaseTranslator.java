package me.connor.justdesserts;

import java.util.*;
import java.util.stream.*;

import me.connor.justdesserts.exceptions.SerialException;
import me.connor.util.*;

public class BaseTranslator<S, D> implements Translator<S, D> {
	
	@SuppressWarnings("rawtypes")
	final HashMap<Class, SerialHandler> handlers = new HashMap<>();
	
	final SerialDeriver<S, D> deriver;
	
	BaseTranslator(@Nonnull SerialDeriver<S, D> deriver, @Nonnull SerialHandler<? extends S, ? extends D>...handlers) {
		this(deriver);
		Assert.notNull(handlers);
		this.handlers.putAll(
				List.of(handlers)
				.stream()
				.collect(Collectors.toMap(
						SerialHandler::serialType,
						(h) -> h
						))
				);
	}
	
	BaseTranslator(@Nonnull SerialDeriver<S, D> deriver) {
		Assert.notNull(deriver);
		this.deriver = deriver;
	}

	@Override
	public <U extends S> SerialHandler<U, D> derive(@Nonnull Class<U> serialType) throws IllegalArgumentException {
		Assert.notNull(serialType);
		if (handlers.containsKey(serialType)) return handlers.get(serialType);
		SerialHandler<U, D> handler = deriver.derive(serialType);
		handlers.put(serialType, handler);
		return handler;
	}

	@Override
	public boolean accepts(@Nonnull Class<?> type) {
		try {
			return derive((Class<? extends S>) type) != null;//TODO: Should the cast be to Class<? extends S> or Class<S>? Does it make a difference?
		} catch (ClassCastException | IllegalArgumentException | SerialException e) {
			return false;
		}
	}

	@Override
	public <U extends S, K extends D> K serialize(@Nullable U obj) throws IllegalArgumentException, SerialException {
		return obj == null ? null : (K) derive((Class<U>) obj.getClass()).serialize(obj);
	}

	@Override
	public <U extends S, K extends D> U deserialize(@Nonnull Class<U> serialType, @Nullable K data) throws IllegalArgumentException, SerialException {
		Assert.notNull(serialType);
		return data == null ? null : derive(serialType).deserialize(data);
	}

}
