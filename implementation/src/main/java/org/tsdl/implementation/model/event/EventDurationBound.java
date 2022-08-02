package org.tsdl.implementation.model.event;

import org.tsdl.implementation.evaluation.impl.event.EventDurationBoundImpl;

/**
 * Represents a (lower or upper) bound of a duration specification.
 */
public interface EventDurationBound {
  long value();

  boolean inclusive();

  static EventDurationBound of(long value, boolean inclusive) {
    return new EventDurationBoundImpl(value, inclusive);
  }
}