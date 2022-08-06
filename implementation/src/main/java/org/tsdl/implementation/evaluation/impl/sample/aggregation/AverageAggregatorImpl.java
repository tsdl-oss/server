package org.tsdl.implementation.evaluation.impl.sample.aggregation;

import java.time.Instant;
import java.util.List;
import org.tsdl.implementation.math.SummaryStatistics;
import org.tsdl.implementation.model.sample.aggregation.AverageAggregator;
import org.tsdl.infrastructure.model.DataPoint;

/**
 * Default implementation of {@link AverageAggregator}.
 */
public class AverageAggregatorImpl extends AbstractSummaryAggregator implements AverageAggregator {
  public AverageAggregatorImpl(Instant lowerBound, Instant upperBound, SummaryStatistics summaryStatistics) {
    super(lowerBound, upperBound, summaryStatistics);
  }

  @Override
  protected double onAggregate(List<DataPoint> input) {
    return summaryStatistics.average();
  }
}
