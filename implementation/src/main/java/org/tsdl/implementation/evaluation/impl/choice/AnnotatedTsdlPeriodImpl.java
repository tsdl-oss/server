package org.tsdl.implementation.evaluation.impl.choice;

import org.tsdl.implementation.model.choice.AnnotatedTsdlPeriod;
import org.tsdl.implementation.model.common.TsdlIdentifier;
import org.tsdl.infrastructure.model.TsdlPeriod;

/**
 * Default implementation of {@link AnnotatedTsdlPeriod}.
 */
public record AnnotatedTsdlPeriodImpl(TsdlPeriod period, TsdlIdentifier event) implements AnnotatedTsdlPeriod {
}