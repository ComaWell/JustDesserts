package me.connor.justdesserts;

import me.connor.justdesserts.exceptions.*;
import me.connor.util.*;

public interface Translator<S, D> extends SerialDeriver<S, D> {

	<U extends S, K extends D> K serialize(@Nullable U obj) throws IllegalArgumentException, SerialException;
	
	<U extends S, K extends D> U deserialize(@Nonnull Class<U> serialType, @Nullable K data) throws IllegalArgumentException, SerialException;
	
	public static <S, D> Translator<S, D> from(@Nonnull SerialDeriver<S, D> deriver, @Nonnull SerialHandler<? extends S, ? extends D>...handlers) {
		return new BaseTranslator<>(deriver, handlers);
	}
	
	public static <S, D> Translator<S, D> from(@Nonnull SerialDeriver<S, D> deriver) {
		return new BaseTranslator<>(deriver);
	}
	
	public static <S, D> Translator<S, D> from(@Nonnull SerialHandler<? extends S, ? extends D>...handlers) {
		return from(SerialDeriver.from(handlers));
	}
	
	public static <S, D> Translator<S, D> merge(@Nonnull Translator<? extends S, ? extends D>...translators) {
		Assert.notNull(translators);
		return from(SerialDeriver.merge(translators));
	}
	
	public static DefaultTranslator newDefaultWith(@Nonnull SerialHandler<?, ?>...handlers) {
		return new DefaultTranslator(handlers);
	}
	
	public static DefaultTranslator newDefault() {
		return new DefaultTranslator();
	}
	
	public static final DefaultTranslator GLOBAL_DEFAULT = newDefault();
	
}
