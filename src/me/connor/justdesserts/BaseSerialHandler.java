package me.connor.justdesserts;

import java.util.function.Function;

import me.connor.util.*;

public class BaseSerialHandler<S, D> implements SerialHandler<S, D> {
	
	final Class<S> serialType;
	final Function<S, D> serializer;
	final Function<D, S> deserializer;
	
	public BaseSerialHandler(@Nonnull Class<S> serialType, @Nonnull Function<S, D> serializer, @Nonnull Function<D, S> deserializer) {
		Assert.allNotNull(serialType, serializer, deserializer);
		this.serialType = serialType;
		this.serializer = serializer;
		this.deserializer = deserializer;
	}

	@Override
	public Class<S> serialType() {
		return serialType;
	}

	@Override
	public D serialize(@Nonnull S obj) {
		return serializer.apply(obj);
	}

	@Override
	public S deserialize(@Nonnull D data) {
		return deserializer.apply(data);
	}

}
