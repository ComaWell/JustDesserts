package me.connor.justdesserts;

import java.util.function.*;

import me.connor.util.*;

public class BaseSerialDeriver<S, D> implements SerialDeriver<S, D> {
	
	final Qualifier<Class<?>> qualifier;
	final Function<Class<? extends S>, SerialHandler<? extends S, D>> deriver;
	
	BaseSerialDeriver(@Nonnull Qualifier<Class<?>> qualifier, @Nonnull Function<Class<? extends S>, SerialHandler<? extends S, D>> deriver) {
		Assert.allNotNull(qualifier, deriver);
		this.qualifier = qualifier;
		this.deriver = deriver;
	}

	@Override
	public boolean accepts(@Nonnull Class<?> type) {
		return qualifier.accepts(type);
	}

	@Override
	public <U extends S> SerialHandler<U, D> derive(@Nonnull Class<U> serialType) throws IllegalArgumentException {
		if (!accepts(serialType)) throw new IllegalArgumentException("This SerialDeriver does not accept Class " + serialType.getCanonicalName());
		return (SerialHandler<U, D>) deriver.apply(serialType);
	}

}
