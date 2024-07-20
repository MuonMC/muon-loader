package org.muonmc.loader.api.horoscope;

import org.muonmc.loader.api.horoscope.muon.MuonFeatures;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an API as experimental.
 *
 * @see OptInExperimentalApi
 * @see org.muonmc.loader.api.horoscope
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
@ExperimentalApi(MuonFeatures.HOROSCOPE)
public @interface ExperimentalApi {
	/**
	 * Represents the feature IDs that this element belongs to.
	 */
	String[] value();
}
