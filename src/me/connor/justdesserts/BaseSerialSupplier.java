package me.connor.justdesserts;

import java.util.function.*;

import me.connor.util.*;

public class BaseSerialSupplier<S> implements SerialSupplier<S> {
	
	final Qualifier<Class<?>> qualifier;
	final Function<Class<S>, S> creator;
	
	BaseSerialSupplier(@Nonnull Qualifier<Class<?>> qualifier, @Nonnull Function<Class<S>, S> creator) {
		Assert.allNotNull(qualifier, creator);
		this.qualifier = qualifier;
		this.creator = creator;
	}

	@Override
	public boolean accepts(@Nonnull Class<?> type) {
		return qualifier.accepts(type);
	}

	@Override
	public S create(@Nonnull Class<S> serialType) throws IllegalArgumentException {
		if (!accepts(serialType)) throw new IllegalArgumentException("This SerialSupplier does not accept Class " + serialType.getCanonicalName());
		return creator.apply(serialType);
	}

}
