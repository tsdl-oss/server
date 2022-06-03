package org.tsdl.testutil.creation.provider;

import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ArgumentsSource(TsdlTestProvider.class)
public @interface TsdlTestSources {
    TsdlTestSource[] value();
}
