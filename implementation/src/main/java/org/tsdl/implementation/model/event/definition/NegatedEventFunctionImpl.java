package org.tsdl.implementation.model.event.definition;

import org.tsdl.infrastructure.common.Condition;
import org.tsdl.infrastructure.common.Conditions;

public record NegatedEventFunctionImpl(EventFunction eventFunction) implements NegatedEventFunction {
  public NegatedEventFunctionImpl {
    Conditions.checkNotNull(Condition.ARGUMENT, eventFunction, "Event function must not be null.");
  }
}
