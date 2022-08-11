package org.tsdl.client.impl.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.tsdl.client.api.builder.ChoiceSpecification;
import org.tsdl.client.api.builder.EventSpecification;
import org.tsdl.client.api.builder.FilterConnectiveSpecification;
import org.tsdl.client.api.builder.FilterSpecification;
import org.tsdl.client.api.builder.Range;
import org.tsdl.client.api.builder.TemporalSampleSpecification;
import org.tsdl.client.api.builder.TsdlQueryBuilder;
import org.tsdl.client.api.builder.ValueSampleSpecification;
import org.tsdl.client.api.builder.YieldSpecification;
import org.tsdl.client.util.TsdlQueryBuildException;
import org.tsdl.infrastructure.common.TsdlTimeUnit;

/**
 * Default implementation of {@link TsdlQueryBuilder}.
 */
public class TsdlQueryBuilderImpl implements TsdlQueryBuilder {
  private static final String INDENT = "  ";
  private static final String SECTION_SEPARATOR = "\n";

  private final List<String> samples;
  private String filter;
  private final List<String> events;
  private String choice;
  private String yield;

  public TsdlQueryBuilderImpl() {
    this.samples = new ArrayList<>();
    this.events = new ArrayList<>();
  }

  @Override
  public TsdlQueryBuilder valueSample(ValueSampleSpecification sampleSpec) {
    var lowerBound = sampleSpec.lowerBound().isPresent() ? "\"%s\"".formatted(sampleSpec.lowerBound().get()) : "\"\"";
    var upperBound = sampleSpec.upperBound().isPresent() ? "\"%s\"".formatted(sampleSpec.upperBound().get()) : "\"\"";
    var sampleArgument = !"\"\"".equals(lowerBound) || !"\"\"".equals(upperBound) ? "%s, %s".formatted(lowerBound, upperBound) : "";
    var aggregatorFunction = switch (sampleSpec.type()) {
      case AVERAGE -> "avg";
      case MAXIMUM -> "max";
      case MINIMUM -> "min";
      case SUM -> "sum";
      case COUNT -> "count";
      case INTEGRAL -> "integral";
      case STANDARD_DEVIATION -> "stddev";
    };

    samples.add("%s(%s) AS %s".formatted(aggregatorFunction, sampleArgument, sampleSpec.identifier()));
    return this;
  }

  @Override
  public TsdlQueryBuilder temporalSample(TemporalSampleSpecification sampleSpec) {
    var unitArgument = sampleSpec.type() != TemporalSampleSpecification.TemporalSampleType.COUNT
        ? "%s, ".formatted(unitString(sampleSpec.unit()))
        : "";
    var periodsArgument = String.join(", ", sampleSpec.periods().stream().map(p -> "\"%s/%s\"".formatted(p.start(), p.end())).toList());
    var sampleArgument = "%s%s".formatted(unitArgument, periodsArgument);
    var aggregatorFunction = switch (sampleSpec.type()) {
      case AVERAGE -> "avg_t";
      case MAXIMUM -> "max_t";
      case MINIMUM -> "min_t";
      case SUM -> "sum_t";
      case COUNT -> "count_t";
      case STANDARD_DEVIATION -> "stddev_t";
    };

    samples.add("%s(%s) AS %s".formatted(aggregatorFunction, sampleArgument, sampleSpec.identifier()));
    return this;
  }

  @Override
  public TsdlQueryBuilder filter(FilterConnectiveSpecification filterConnectiveSpec) {
    filter = filterConnectiveString(filterConnectiveSpec);
    return this;
  }

  @Override
  public TsdlQueryBuilder event(EventSpecification eventSpec) {
    var eventDefinition = filterConnectiveString(eventSpec.definition());
    var durationConstraint = eventSpec.duration().isPresent() ? intervalString(eventSpec.duration().get(), "FOR") : "";
    events.add("%s%s AS %s".formatted(eventDefinition, durationConstraint, eventSpec.identifier()));
    return this;
  }

  @Override
  public TsdlQueryBuilder choice(ChoiceSpecification choiceSpec) {
    var durationConstraint = choiceSpec.tolerance().isPresent() ? intervalString(choiceSpec.tolerance().get(), "WITHIN") : "";
    var operator = switch (choiceSpec.type()) {
      case PRECEDES -> "precedes";
      case FOLLOWS -> "follows";
    };

    choice = "%s %s %s%s".formatted(choiceSpec.operand1(), operator, choiceSpec.operand2(), durationConstraint);
    return this;
  }

  @Override
  public TsdlQueryBuilder yield(YieldSpecification yieldSpec) {
    yield = switch (yieldSpec.type()) {
      case DATA_POINTS -> "data points";
      case ALL_PERIODS -> "all periods";
      case LONGEST_PERIOD -> "longest period";
      case SHORTEST_PERIOD -> "shortest period";
      case SAMPLE -> "sample %s".formatted(yieldSpec.sample());
      case SAMPLES -> "samples %s".formatted(String.join(", ", yieldSpec.samples()));
    };
    return this;
  }

  @Override
  public String build() {
    if (yield == null) {
      throw new TsdlQueryBuildException("'YIELD' statement is mandatory, but has not been set.");
    }

    var finalQuery = new StringBuilder();
    appendSection(finalQuery, "WITH SAMPLES", samples);
    appendSection(finalQuery, "APPLY FILTER", filter != null ? List.of(filter) : List.of());
    appendSection(finalQuery, "USING EVENTS", events);
    appendSection(finalQuery, "CHOOSE", choice != null ? List.of(choice) : List.of());
    appendSection(finalQuery, "YIELD", List.of(yield));

    // keep one consecutive section separator, but not more
    return finalQuery.toString().replaceAll("%s+".formatted(SECTION_SEPARATOR), SECTION_SEPARATOR).trim();
  }

  private void appendSection(StringBuilder builder, String sectionTitle, Collection<String> sectionContents) {
    if (sectionContents.isEmpty()) {
      return;
    }

    builder
        .append(sectionTitle)
        .append(":\n").append(INDENT)
        .append(String.join(",%n%s".formatted(INDENT), sectionContents))
        .append(SECTION_SEPARATOR);
  }

  private static String intervalString(Range range, String prefix) {
    if (range == null) {
      return "";
    }
    var parens = switch (range.type()) {
      case OPEN_START -> new String[] {"(", "]"};
      case OPEN_END -> new String[] {"[", ")"};
      case CLOSED -> new String[] {"[", "]"};
      case OPEN -> new String[] {"(", ")"};
    };
    var lower = range.lowerBound().isPresent() ? "%s".formatted(range.lowerBound().get()) : "";
    var upper = range.upperBound().isPresent() ? "%s".formatted(range.upperBound().get()) : "";

    return range.lowerBound().isPresent() || range.upperBound().isPresent()
        ? " %s %s%s,%s%s %s".formatted(prefix, parens[0], lower, upper, parens[1], unitString(range.unit()))
        : "";
  }

  private static String filterConnectiveString(FilterConnectiveSpecification filterConnective) {
    var filters = filterConnective.filters().stream().map(TsdlQueryBuilderImpl::filterString).collect(Collectors.joining(", "));
    var connective = switch (filterConnective.type()) {
      case AND -> "AND";
      case OR -> "OR";
    };

    return "%s(%s)".formatted(connective, filters);
  }

  private static String filterString(FilterSpecification filter) {
    var filterString = switch (filter) {
      case FilterSpecification.ThresholdFilterSpecification threshold -> thresholdFilterString(threshold);
      case FilterSpecification.TemporalFilterSpecification temporal -> temporalFilterString(temporal);
      case FilterSpecification.DeviationFilterSpecification deviation -> deviationFilterString(deviation);
      default -> throw new TsdlQueryBuildException("Unknown filter specification '%s' known".formatted(filter.getClass().getSimpleName()));
    };
    return filter.isNegated() ? "NOT(%s)".formatted(filterString) : filterString;
  }

  private static String thresholdFilterString(FilterSpecification.ThresholdFilterSpecification filter) {
    var filterFunction = switch (filter.type()) {
      case LESS_THAN -> "lt";
      case GREATER_THAN -> "gt";
    };
    return "%s(%s)".formatted(filterFunction, filter.threshold());
  }

  private static String temporalFilterString(FilterSpecification.TemporalFilterSpecification filter) {
    if (filter.argument() == null) {
      throw new TsdlQueryBuildException("Temporal filter argument must not be null.");
    }

    var filterFunction = switch (filter.type()) {
      case BEFORE -> "before";
      case AFTER -> "after";
    };
    return "%s(\"%s\")".formatted(filterFunction, filter.argument().toString());
  }

  private static String deviationFilterString(FilterSpecification.DeviationFilterSpecification filter) {
    var firstArgument = switch (filter.type()) {
      case RELATIVE -> "rel";
      case ABSOLUTE -> "abs";
    };

    return "around(%s, %s, %s)".formatted(firstArgument, filter.reference(), filter.maximumDeviation());
  }

  private static String unitString(TsdlTimeUnit unit) {
    return switch (unit) {
      case MILLISECONDS -> "millis";
      case SECONDS -> "seconds";
      case MINUTES -> "minutes";
      case HOURS -> "hours";
      case DAYS -> "days";
      case WEEKS -> "weeks";
    };
  }
}