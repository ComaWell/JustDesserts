package me.connor.justdesserts;

import me.connor.justdesserts.exceptions.SerialException;
import me.connor.util.*;

public class DefaultTranslator extends DefaultSerialDeriver implements Translator<Object, Object> {
	
	DefaultTranslator(@Nonnull SerialHandler<?, ?>...handlers) {
		Assert.notNull(handlers);
		for (SerialHandler<?, ?> h : handlers) addHandler(h);
	}

	@Override
	public <U, K> K serialize(@Nullable U obj) throws IllegalArgumentException, SerialException {
		return obj == null ? null : (K) derive((Class<U>) obj.getClass()).serialize(obj);
	}

	@Override
	public <U, K> U deserialize(@Nonnull Class<U> serialType, @Nullable K data) throws IllegalArgumentException, SerialException {
		Assert.notNull(serialType);
		return data == null ? null : data.getClass().equals(serialType) ? (U) data : derive(serialType).deserialize(data);
																	//  ^ TODO: theoretically prevents double-feeding an Object from causing an issue, but needs testing
	}

}
