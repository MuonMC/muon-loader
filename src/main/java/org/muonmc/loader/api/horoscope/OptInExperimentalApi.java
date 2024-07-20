package org.muonmc.loader.api.horoscope;

import org.muonmc.loader.api.horoscope.muon.MuonFeatures;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <h1>Horoscope Explicit Opt-In</h1>
 * <p>
 * This annotation explicitly marks a class as opt-in for an experimental API feature. Note that experimental features are subject to change during non-breaking
 * API versions.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
@ExperimentalApi(MuonFeatures.HOROSCOPE)
public @interface OptInExperimentalApi {
	/**
	 * The IDs of the features to opt into. These can be found in {@link MuonFeatures}.
	 */
	String[] value() default {};
}
