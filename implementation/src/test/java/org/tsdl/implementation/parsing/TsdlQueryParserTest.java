package org.tsdl.implementation.parsing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.tsdl.implementation.evaluation.impl.common.formatting.TsdlSampleOutputFormatter;
import org.tsdl.implementation.factory.ObjectFactory;
import org.tsdl.implementation.factory.TsdlElementFactory;
import org.tsdl.implementation.model.choice.relation.FollowsOperator;
import org.tsdl.implementation.model.choice.relation.PrecedesOperator;
import org.tsdl.implementation.model.common.TsdlIdentifier;
import org.tsdl.implementation.model.common.TsdlOutputFormatter;
import org.tsdl.implementation.model.connective.AndConnective;
import org.tsdl.implementation.model.connective.OrConnective;
import org.tsdl.implementation.model.event.TsdlEvent;
import org.tsdl.implementation.model.filter.GreaterThanFilter;
import org.tsdl.implementation.model.filter.LowerThanFilter;
import org.tsdl.implementation.model.filter.NegatedSinglePointFilter;
import org.tsdl.implementation.model.filter.SinglePointFilter;
import org.tsdl.implementation.model.filter.ThresholdFilter;
import org.tsdl.implementation.model.filter.argument.TsdlSampleFilterArgument;
import org.tsdl.implementation.model.result.YieldFormat;
import org.tsdl.implementation.model.result.YieldStatement;
import org.tsdl.implementation.model.sample.TsdlSample;
import org.tsdl.implementation.model.sample.aggregation.TsdlAggregator;
import org.tsdl.implementation.model.sample.aggregation.TsdlLocalAggregator;
import org.tsdl.implementation.model.sample.aggregation.global.GlobalAverageAggregator;
import org.tsdl.implementation.model.sample.aggregation.global.GlobalCountAggregator;
import org.tsdl.implementation.model.sample.aggregation.global.GlobalMaximumAggregator;
import org.tsdl.implementation.model.sample.aggregation.global.GlobalMinimumAggregator;
import org.tsdl.implementation.model.sample.aggregation.global.GlobalSumAggregator;
import org.tsdl.implementation.model.sample.aggregation.local.LocalAverageAggregator;
import org.tsdl.implementation.model.sample.aggregation.local.LocalCountAggregator;
import org.tsdl.implementation.model.sample.aggregation.local.LocalMaximumAggregator;
import org.tsdl.implementation.model.sample.aggregation.local.LocalMinimumAggregator;
import org.tsdl.implementation.model.sample.aggregation.local.LocalSumAggregator;
import org.tsdl.implementation.parsing.enums.ConnectiveIdentifier;
import org.tsdl.implementation.parsing.enums.FilterType;
import org.tsdl.implementation.parsing.exception.DuplicateIdentifierException;
import org.tsdl.implementation.parsing.exception.InvalidReferenceException;
import org.tsdl.implementation.parsing.exception.TsdlParserException;
import org.tsdl.implementation.parsing.exception.UnknownIdentifierException;

class TsdlQueryParserTest {
  private static final TsdlQueryParser PARSER = ObjectFactory.INSTANCE.queryParser();
  private static final TsdlElementFactory ELEMENTS = ObjectFactory.INSTANCE.elementFactory();
  private static final Function<? super ThresholdFilter, Double> VALUE_EXTRACTOR = filter -> filter.threshold().value();

  @Nested
  @DisplayName("sample declaration tests")
  class SampleDeclaration {
    // @MethodSource does not work very well in @Nested class => @ArgumentsSource wih ArgumentsProvider as alternative
    @ParameterizedTest
    @ArgumentsSource(SampleDeclarationSamplesArgumentsProvider.class)
    void sampleDeclaration_knownAggregatorFunctionsWithoutEcho(String aggregator, Class<? extends TsdlSample> clazz) {
      var queryString = "WITH SAMPLES: %s(_input) AS s1\n          YIELD: all periods".formatted(aggregator);

      var query = PARSER.parseQuery(queryString);

      assertThat(query.samples())
          .hasSize(1)
          .element(0, InstanceOfAssertFactories.type(TsdlSample.class))
          .satisfies(sample -> {
            assertThat(sample.identifier()).isEqualTo(ELEMENTS.getIdentifier("s1"));
            assertThat(sample.aggregator()).isInstanceOf(clazz);
            assertThat(sample.formatter()).isNotPresent();
          });
    }

    // @MethodSource does not work very well in @Nested class => @ArgumentsSource wih ArgumentsProvider as alternative
    @ParameterizedTest
    @ArgumentsSource(SampleDeclarationSamplesArgumentsProvider.class)
    void sampleDeclaration_knownAggregatorFunctionsWithEcho(String aggregator, Class<? extends TsdlSample> clazz) {
      var queryString = "WITH SAMPLES: %s(_input) AS s1->echo(9)\n          YIELD: all periods".formatted(aggregator);

      var query = PARSER.parseQuery(queryString);

      assertThat(query.samples())
          .hasSize(1)
          .element(0, InstanceOfAssertFactories.type(TsdlSample.class))
          .satisfies(sample -> {
            assertThat(sample.identifier()).isEqualTo(ELEMENTS.getIdentifier("s1"));
            assertThat(sample.aggregator()).isInstanceOf(clazz);
            assertThat(sample.formatter())
                .asInstanceOf(InstanceOfAssertFactories.optional(TsdlSampleOutputFormatter.class)).get()
                .extracting(TsdlOutputFormatter::args)
                .isEqualTo(new String[] {"9"});
          });
    }

    // @MethodSource does not work very well in @Nested class => @ArgumentsSource wih ArgumentsProvider as alternative
    @ParameterizedTest
    @ArgumentsSource(SampleDeclarationLocalSamplesArgumentsProvider.class)
    void sampleDeclaration_knownAggregatorFunctionsWithTimeRange(String aggregator, Instant lower, Instant upper, Class<? extends TsdlSample> clazz) {
      var queryString = """
            WITH SAMPLES: %s AS s1
            YIELD: all periods
          """.formatted(aggregator);

      var query = PARSER.parseQuery(queryString);

      assertThat(query.samples())
          .hasSize(1)
          .element(0, InstanceOfAssertFactories.type(TsdlSample.class))
          .satisfies(sample -> {
            assertThat(sample.identifier()).isEqualTo(ELEMENTS.getIdentifier("s1"));
            assertThat(sample.aggregator()).isInstanceOf(clazz);
            assertThat(sample.formatter()).isNotPresent();
            assertThat(sample.aggregator())
                .asInstanceOf(InstanceOfAssertFactories.type(TsdlLocalAggregator.class))
                .extracting(TsdlLocalAggregator::lowerBound, TsdlLocalAggregator::upperBound)
                .containsExactly(lower, upper);
          });
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "avg()", // no arguments
        "min(\"2022-08-08T13:04:23.000Z\")", // only one argument
        "max(\"2022-08-08T13:04:23.000Z\", \"2022-08-19T23:55:55.000Z\"\", \"2022-08-24T23:59:55.000Z\")", // three arguments
        "sum(\"2022-08-08T13:04:23.000Z\",\"2022-08-19 23:55:55.000Z\")", // second argument has space " " between date and time instead of "T"
        "count(\"2022-08-19T23:55:55.000Z\", \"2022-08-08T13:04:23.000Z\")", // first argument is before second argument
        "count(\"2022-08-19T23:55:55.000Z\", \"2022-08-19T23:55:55.000Z\")", // first and second argument are equal
        "avg(\"2022-08-08T13:04:23.000Z\" \"2022-08-19T23:55:55.000Z\")", // no comma "," between first and second argument
        "min(\"2022-08-08T13:04:23.000Z\",,\"2022-08-19T23:55:55.000Z\")", // two commas "," between first and second argument
        "max(\"2022-08-08T13:04:23.000Z\"\",\"2022-08-19T23:55:55.000Z\")", // superfluous quote (") at end of first argument
        "sum(\"test\",\"2022-08-19T23:55:55.000Z\")", // invalid first argument
    })
    void sampleDeclaration_knownAggregatorFunctionsWithInvalidTimeRange_throws(String aggregator) {
      Assertions.setMaxStackTraceElementsDisplayed(100);
      var queryString = """
            WITH SAMPLES: %s AS s1
            YIELD: all periods
          """.formatted(aggregator);

      assertThatThrownBy(() -> PARSER.parseQuery(queryString)).isInstanceOf(TsdlParserException.class);
    }

    // @MethodSource does not work very well in @Nested class => @ArgumentsSource wih ArgumentsProvider as alternative
    @ParameterizedTest
    @ArgumentsSource(SampleDeclarationSamplesArgumentsProvider.class)
    void sampleDeclaration_knownAggregatorFunctionsWithInvalidEchoArgument_throws(String aggregator) {
      var queryString = "WITH SAMPLES: %s(_input) AS s1 -> echo(NaN)\n          YIELD: all periods".formatted(aggregator);

      assertThatThrownBy(() -> PARSER.parseQuery(queryString)).isInstanceOf(TsdlParserException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"_", "_s1", "sö", "", "123", "1s"})
    void sampleDeclaration_invalidIdentifier_throws(String identifier) {
      var queryString = "WITH SAMPLES: avg(_input) AS %s\n          YIELD: all periods".formatted(identifier);

      assertThatThrownBy(() -> PARSER.parseQuery(queryString)).isInstanceOf(TsdlParserException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "avg2(\"2022-08-08T13:04:23.000Z\",\"\"2022-08-19T23:55:55.000Z\"\")", // valid time range, but unknown function
        "counts(_input)" // valid input specification, but unknown function
    })
    void sampleDeclaration_unknownAggregatorFunction_throws(String aggregator) {
      var queryString = "WITH SAMPLES: %s AS s1 YIELD: data points".formatted(aggregator);

      assertThatThrownBy(() -> PARSER.parseQuery(queryString)).isInstanceOf(TsdlParserException.class);
    }

    static class SampleDeclarationSamplesArgumentsProvider implements ArgumentsProvider {
      @Override
      public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return Stream.of(
            Arguments.of("avg", GlobalAverageAggregator.class),
            Arguments.of("max", GlobalMaximumAggregator.class),
            Arguments.of("min", GlobalMinimumAggregator.class),
            Arguments.of("sum", GlobalSumAggregator.class),
            Arguments.of("count", GlobalCountAggregator.class)
        );
      }
    }

    static class SampleDeclarationLocalSamplesArgumentsProvider implements ArgumentsProvider {
      @Override
      public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return Stream.of(
            Arguments.of(
                "avg(\"2022-08-08T13:04:23.000Z\",\"2022-08-19T23:55:55.000Z\")",
                Instant.parse("2022-08-08T13:04:23.000Z"), Instant.parse("2022-08-19T23:55:55.000Z"), LocalAverageAggregator.class
            ),
            Arguments.of(
                "max(\"2022-08-08T13:04:23.000Z\",        \"2022-08-19T23:55:55.000Z\")",
                Instant.parse("2022-08-08T13:04:23.000Z"), Instant.parse("2022-08-19T23:55:55.000Z"), LocalMaximumAggregator.class
            ),
            Arguments.of(
                "min(\"2022-08-08T13:04:23.000Z\",\r\"2022-08-19T23:55:55.000Z\")",
                Instant.parse("2022-08-08T13:04:23.000Z"), Instant.parse("2022-08-19T23:55:55.000Z"), LocalMinimumAggregator.class
            ),
            Arguments.of(
                "sum(\"2022-08-08T13:04:23.000Z\" ,\r\n\"2022-08-19T23:55:55.000Z\")",
                Instant.parse("2022-08-08T13:04:23.000Z"), Instant.parse("2022-08-19T23:55:55.000Z"), LocalSumAggregator.class
            ),
            Arguments.of(
                "count(    \"2022-08-08T13:04:23.000Z\"    , \n \"2022-08-19T23:55:55.000Z\"     )",
                Instant.parse("2022-08-08T13:04:23.000Z"), Instant.parse("2022-08-19T23:55:55.000Z"), LocalCountAggregator.class
            ),
            Arguments.of(
                "count(    \"2022-08-08T13:04:23.000Z\"\r, \n \"2022-08-19T23:55:55.000Z\"     )",
                Instant.parse("2022-08-08T13:04:23.000Z"), Instant.parse("2022-08-19T23:55:55.000Z"), LocalCountAggregator.class
            )
        );
      }
    }
  }

  @Nested
  @DisplayName("filter declaration tests")
  class FilterDeclaration {

    @Test
    void filterDeclaration_conjunctiveFilterWithOneArgument() {
      var queryString = """
          FILTER:
                      AND(gt(23.4))
                    YIELD: data points""";

      var query = PARSER.parseQuery(queryString);

      assertThat(query.filter())
          .asInstanceOf(InstanceOfAssertFactories.optional(AndConnective.class))
          .isPresent().get()
          .extracting(AndConnective::filters, InstanceOfAssertFactories.list(SinglePointFilter.class))
          .hasSize(1);

      assertThat(query.filter())
          .isPresent().get()
          .extracting(connective -> connective.filters().get(0), InstanceOfAssertFactories.type(GreaterThanFilter.class))
          .extracting(VALUE_EXTRACTOR)
          .isEqualTo(23.4);
    }

    @Test
    void filterDeclaration_disjunctiveFilterWithOneArgument() {
      var queryString = """
          FILTER:
                      OR(lt(-2.3))
                    YIELD: data points""";

      var query = PARSER.parseQuery(queryString);

      assertThat(query.filter())
          .asInstanceOf(InstanceOfAssertFactories.optional(OrConnective.class))
          .isPresent().get()
          .extracting(OrConnective::filters, InstanceOfAssertFactories.list(SinglePointFilter.class))
          .hasSize(1)
          .element(0, InstanceOfAssertFactories.type(LowerThanFilter.class))
          .extracting(VALUE_EXTRACTOR)
          .isEqualTo(-2.3);
    }

    @Test
    void filterDeclaration_negatedFilter() {
      var queryString = """
          FILTER:
                      OR(NOT(lt(25)))
                    YIELD: data points""";

      var query = PARSER.parseQuery(queryString);

      assertThat(query.filter())
          .asInstanceOf(InstanceOfAssertFactories.optional(OrConnective.class))
          .isPresent().get()
          .extracting(OrConnective::filters, InstanceOfAssertFactories.list(SinglePointFilter.class))
          .hasSize(1)
          .element(0, InstanceOfAssertFactories.type(NegatedSinglePointFilter.class))
          .extracting(NegatedSinglePointFilter::filter, InstanceOfAssertFactories.type(LowerThanFilter.class))
          .extracting(VALUE_EXTRACTOR)
          .isEqualTo(25d);
    }

    @Test
    void filterDeclaration_multipleArguments() {
      var queryString = """
          FILTER:
                      OR(
                          NOT(lt(25.1)),       gt(3.4),
                          NOT(gt(1000)),
                          lt(-3.4447)
                        )
                    YIELD: data points""";

      var query = PARSER.parseQuery(queryString);

      assertThat(query.filter())
          .asInstanceOf(InstanceOfAssertFactories.optional(OrConnective.class))
          .isPresent().get()
          .extracting(OrConnective::filters, InstanceOfAssertFactories.list(SinglePointFilter.class))
          .hasSize(4);

      assertThat(query.filter())
          .isPresent().get()
          .extracting(connective -> connective.filters().get(0), InstanceOfAssertFactories.type(NegatedSinglePointFilter.class))
          .extracting(NegatedSinglePointFilter::filter, InstanceOfAssertFactories.type(LowerThanFilter.class))
          .extracting(VALUE_EXTRACTOR)
          .isEqualTo(25.1);

      assertThat(query.filter())
          .isPresent().get()
          .extracting(connective -> connective.filters().get(1), InstanceOfAssertFactories.type(GreaterThanFilter.class))
          .extracting(VALUE_EXTRACTOR)
          .isEqualTo(3.4);

      assertThat(query.filter())
          .isPresent().get()
          .extracting(connective -> connective.filters().get(2), InstanceOfAssertFactories.type(NegatedSinglePointFilter.class))
          .extracting(NegatedSinglePointFilter::filter, InstanceOfAssertFactories.type(GreaterThanFilter.class))
          .extracting(VALUE_EXTRACTOR)
          .isEqualTo(1000d);

      assertThat(query.filter())
          .isPresent().get()
          .extracting(connective -> connective.filters().get(3), InstanceOfAssertFactories.type(LowerThanFilter.class))
          .extracting(VALUE_EXTRACTOR)
          .isEqualTo(-3.4447);
    }

    @Test
    void filterDeclaration_sampleAsArgument() {
      var queryString = """
          WITH SAMPLES:
                      avg(_input) AS average
                    FILTER:
                      AND(gt(average))
                    YIELD: data points""";

      var query = PARSER.parseQuery(queryString);

      assertThat(query.filter())
          .asInstanceOf(InstanceOfAssertFactories.optional(AndConnective.class))
          .isPresent().get()
          .extracting(AndConnective::filters, InstanceOfAssertFactories.list(SinglePointFilter.class))
          .hasSize(1)
          .element(0, InstanceOfAssertFactories.type(GreaterThanFilter.class))
          .extracting(GreaterThanFilter::threshold, InstanceOfAssertFactories.type(TsdlSampleFilterArgument.class))
          .extracting(arg -> arg.sample().identifier().name(), InstanceOfAssertFactories.STRING)
          .isEqualTo("average");
    }
  }

  @Nested
  @DisplayName("event declaration tests")
  class EventDeclaration {
    @Test
    void eventDeclaration_valid() {
      var queryString = "USING EVENTS: AND(lt(2)) AS high, OR(gt(-3.2)) AS low\n"
          + "          YIELD: all periods";

      var query = PARSER.parseQuery(queryString);

      assertThat(query.events())
          .hasSize(2)
          .element(0, InstanceOfAssertFactories.type(TsdlEvent.class))
          .extracting(TsdlEvent::identifier, TsdlEvent::definition)
          .containsExactly(
              ELEMENTS.getIdentifier("high"),
              ELEMENTS.getConnective(ConnectiveIdentifier.AND, List.of(ELEMENTS.getFilter(FilterType.LT, ELEMENTS.getFilterArgument(2d))))
          );
    }

    @Test
    void eventDeclaration_validWithSample() {
      var queryString = """
          WITH SAMPLES: avg(_input) AS s3
                    USING EVENTS: OR(gt(-3.2)) AS low, AND(lt(s3)) AS sampledHigh
                    YIELD: all periods""";

      var query = PARSER.parseQuery(queryString);

      assertThat(query.events())
          .hasSize(2)
          .element(1, InstanceOfAssertFactories.type(TsdlEvent.class))
          .satisfies(event -> {
            assertThat(event.identifier()).isEqualTo(ELEMENTS.getIdentifier("sampledHigh"));

            assertThat(event.definition().filters().get(0))
                .asInstanceOf(InstanceOfAssertFactories.type(ThresholdFilter.class))
                .extracting(ThresholdFilter::threshold, InstanceOfAssertFactories.type(TsdlSampleFilterArgument.class))
                .extracting(sample -> sample.sample().identifier().name())
                .isEqualTo("s3");
          });
    }

    @Test
    void eventDeclaration_invalidIdentifier_throws() {
      var queryString = "USING EVENTS: AND(lt(2)) AS 1high,\n"
          + "          YIELD: all periods";

      assertThatThrownBy(() -> PARSER.parseQuery(queryString)).isInstanceOf(TsdlParserException.class);
    }

    @Test
    void eventDeclaration_unknownSample_throws() {
      var queryString = "USING EVENTS: AND(lt(s3)) AS high\n"
          + "          YIELD: all periods";

      assertThatThrownBy(() -> PARSER.parseQuery(queryString))
          .isInstanceOf(TsdlParserException.class)
          .hasCauseInstanceOf(UnknownIdentifierException.class)
          .extracting(Throwable::getCause, THROWABLE)
          .hasMessageContaining("s3");
    }

    @Test
    void eventDeclaration_invalidSampleReference_throws() {
      Assertions.setMaxStackTraceElementsDisplayed(10);
      var queryString = """
          USING EVENTS: AND(lt(3.5)) AS low, OR(gt(low)) AS high
                    CHOOSE: low precedes high
                    YIELD: all periods""";

      // depending on whether identifier 'low' is parsed before filter argument 'lt(3.5)' or after,
      // an InvalidReferenceException or UnknownIdentifierException is thrown
      assertThatThrownBy(() -> PARSER.parseQuery(queryString))
          .isInstanceOf(TsdlParserException.class)
          .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
          .isInstanceOfAny(UnknownIdentifierException.class, InvalidReferenceException.class)
          .hasMessageContaining("low");
    }
  }

  @Nested
  @DisplayName("choose declaration tests")
  class ChooseDeclaration {
    @Test
    void chooseDeclaration_precedes() {
      var queryString = """
          USING EVENTS: AND(lt(3)) AS e1,
                                  OR(gt(5)) AS e2
                    CHOOSE: e1 precedes e2
                    YIELD: data points""";

      var query = PARSER.parseQuery(queryString);

      assertThat(query.choice())
          .asInstanceOf(InstanceOfAssertFactories.optional(PrecedesOperator.class))
          .isPresent().get()
          .satisfies(op -> {
            assertThat(op.cardinality()).isEqualTo(2);
            assertThat(op.operand1().identifier().name()).isEqualTo("e1");
            assertThat(op.operand2().identifier().name()).isEqualTo("e2");
          });
    }

    @Test
    void chooseDeclaration_follows() {
      var queryString = """
          USING EVENTS: AND(lt(3)) AS e1,
                                  OR(gt(5)) AS e2
                    CHOOSE: e2 follows e1
                    YIELD: data points""";

      var query = PARSER.parseQuery(queryString);

      assertThat(query.choice())
          .asInstanceOf(InstanceOfAssertFactories.optional(FollowsOperator.class))
          .isPresent().get()
          .satisfies(op -> {
            assertThat(op.cardinality()).isEqualTo(2);
            assertThat(op.operand1().identifier().name()).isEqualTo("e2");
            assertThat(op.operand2().identifier().name()).isEqualTo("e1");
          });
    }

    @Test
    void chooseDeclaration_unknownEvent_throws() {
      var queryString = """
          USING EVENTS: AND(lt(3)) AS e1
                    CHOOSE: e1 follows e2
                    YIELD: data points""";

      assertThatThrownBy(() -> PARSER.parseQuery(queryString))
          .isInstanceOf(TsdlParserException.class)
          .hasCauseInstanceOf(UnknownIdentifierException.class)
          .extracting(Throwable::getCause, THROWABLE)
          .hasMessageContaining("e2");
    }

    @Test
    void chooseDeclaration_invalidEventReference_throws() {
      var queryString = """
          WITH SAMPLES: min(_input) AS low, max(_input) AS high
                    CHOOSE: low precedes high
                    YIELD: all periods""";

      assertThatThrownBy(() -> PARSER.parseQuery(queryString))
          .isInstanceOf(TsdlParserException.class)
          .hasCauseInstanceOf(InvalidReferenceException.class)
          .extracting(Throwable::getCause, InstanceOfAssertFactories.THROWABLE)
          .hasMessageContainingAll("low", "event");
    }

    @Test
    void chooseDeclaration_multipleStatements_throws() {
      var queryString = """
          USING EVENTS: AND(lt(3)) AS e1,
                                  OR(gt(5)) AS e2
                    CHOOSE: e1 precedes e2, e2 follows e1
                    YIELD: data points""";

      assertThatThrownBy(() -> PARSER.parseQuery(queryString)).isInstanceOf(TsdlParserException.class);
    }
  }

  @Nested
  @DisplayName("yield declaration tests")
  class YieldDeclaration {
    // @MethodSource does not work very well in @Nested class => @ArgumentsSource wih ArgumentsProvider as alternative
    @ArgumentsSource(YieldDeclarationValidInputProvider.class)
    @ParameterizedTest
    void yieldDeclaration_validRepresentations_parsed(String queryString, YieldStatement result) {
      var query = PARSER.parseQuery(queryString);

      assertThat(query.result()).isEqualTo(result);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "ALL periods", "longestperiod", "shortest perioD", "", "      ", "0", "1",
        "sample ", "sample", "sample   ", "sample 123", "sample 1up"
    })
    void yieldDeclaration_invalidRepresentations_throws(String representation) {
      var queryString = "YIELD: %s".formatted(representation);
      assertThatThrownBy(() -> PARSER.parseQuery(queryString)).isInstanceOf(TsdlParserException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "YIELD: sample hello1",
        "YIELD: samples hello1",
        "WITH SAMPLES: sum(_input) AS mySum, max(_input) AS myMax YIELD: samples mySum, maxi",
    })
    void yieldDeclaration_unknownSamples_throws(String queryString) {
      assertThatThrownBy(() -> PARSER.parseQuery(queryString))
          .isInstanceOf(TsdlParserException.class)
          .hasCauseInstanceOf(UnknownIdentifierException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "WITH SAMPLES: sum(_input) AS mySum  USING EVENTS: AND(lt(mySum)) AS LOW  YIELD: sample LOW",
        "WITH SAMPLES: sum(_input) AS mySum USING EVENTS: AND(lt(mySum)) AS LOW  YIELD: samples mySum, LOW"
    })
    void yieldDeclaration_invalidSampleTypes_throws(String queryString) {
      assertThatThrownBy(() -> PARSER.parseQuery(queryString))
          .isInstanceOf(TsdlParserException.class)
          .hasCauseInstanceOf(InvalidReferenceException.class);
    }

    static class YieldDeclarationValidInputProvider implements ArgumentsProvider {
      @Override
      public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return Stream.of(
            Arguments.of("YIELD: all periods", ELEMENTS.getResult(YieldFormat.ALL_PERIODS, null)),
            Arguments.of("YIELD: longest period", ELEMENTS.getResult(YieldFormat.LONGEST_PERIOD, null)),
            Arguments.of("YIELD: shortest period", ELEMENTS.getResult(YieldFormat.SHORTEST_PERIOD, null)),
            Arguments.of("YIELD: data points", ELEMENTS.getResult(YieldFormat.DATA_POINTS, null)),
            Arguments.of("WITH SAMPLES: min(_input) AS identifier1 YIELD: sample identifier1",
                ELEMENTS.getResult(YieldFormat.SAMPLE, List.of(ELEMENTS.getIdentifier("identifier1")))),
            Arguments.of("WITH SAMPLES: min(_input) AS identifier1 YIELD: samples identifier1",
                ELEMENTS.getResult(YieldFormat.SAMPLE_SET, List.of(ELEMENTS.getIdentifier("identifier1")))),
            Arguments.of("WITH SAMPLES: min(_input) AS identifier1, max(_input) AS identifier2 YIELD: samples identifier1, identifier2",
                ELEMENTS.getResult(YieldFormat.SAMPLE_SET, List.of(ELEMENTS.getIdentifier("identifier1"), ELEMENTS.getIdentifier("identifier2")))),
            Arguments.of("WITH SAMPLES: min(_input) AS i1, max(_input) AS i2, count(_input) AS i3 YIELD: samples i1, i2, i3",
                ELEMENTS.getResult(YieldFormat.SAMPLE_SET,
                    List.of(ELEMENTS.getIdentifier("i1"), ELEMENTS.getIdentifier("i2"), ELEMENTS.getIdentifier("i3"))))
        );
      }
    }
  }

  @Nested
  @DisplayName("integration tests")
  class Integration {
    private static final String FULL_FEATURE_QUERY = """
        WITH SAMPLES:
                  avg(_input) AS s1,
                  max(_input) AS s2,
                  min(_input) AS s3,
                  sum(_input) AS s4,
                  count("2022-04-03T12:45:03.123Z", "2022-07-03T12:45:03.123Z") AS s5

                FILTER:
                  AND(gt(s2), NOT(lt(3.5)))

                USING EVENTS:
                  AND(lt(3.5)) AS low,
                  OR(NOT(gt(7))) AS high,
                  AND(gt(s2)) AS mid

                CHOOSE:
                  low precedes high

                YIELD:
                  all periods""";

    @ValueSource(strings = FULL_FEATURE_QUERY)
    @ParameterizedTest
    void integration_detectsIdentifiers(String queryString) {
      var query = PARSER.parseQuery(queryString);

      assertThat(query.identifiers())
          .hasSize(7)
          .extracting(TsdlIdentifier::name)
          .containsExactlyInAnyOrder("s1", "s2", "s3", "high", "low", "s4", "mid", "s5");
    }

    @ValueSource(strings = FULL_FEATURE_QUERY)
    @ParameterizedTest
    void integration_detectsFilters(String queryString) {
      var query = PARSER.parseQuery(queryString);

      assertThat(query.filter())
          .asInstanceOf(InstanceOfAssertFactories.optional(AndConnective.class))
          .isPresent().get()
          .extracting(AndConnective::filters, InstanceOfAssertFactories.list(SinglePointFilter.class))
          .hasSize(2)
          .satisfies(filterArguments -> {
            assertThat(filterArguments.get(0))
                .asInstanceOf(InstanceOfAssertFactories.type(GreaterThanFilter.class))
                .extracting(GreaterThanFilter::threshold, InstanceOfAssertFactories.type(TsdlSampleFilterArgument.class))
                .extracting(arg -> arg.sample().identifier().name(), InstanceOfAssertFactories.STRING)
                .isEqualTo("s2");

            assertThat(filterArguments.get(1))
                .isEqualTo(ELEMENTS.getNegatedFilter(ELEMENTS.getFilter(FilterType.LT, ELEMENTS.getFilterArgument(3.5))));
          });
    }

    @ValueSource(strings = FULL_FEATURE_QUERY)
    @ParameterizedTest
    void integration_detectsSamples(String queryString) {
      var query = PARSER.parseQuery(queryString);

      assertThat(query.samples())
          .asInstanceOf(InstanceOfAssertFactories.list(TsdlSample.class))
          .hasSize(5)
          .satisfies(samples -> {
            assertAggregator(samples.get(0), GlobalAverageAggregator.class, "s1", this::accept);
            assertAggregator(samples.get(1), GlobalMaximumAggregator.class, "s2", this::accept);
            assertAggregator(samples.get(2), GlobalMinimumAggregator.class, "s3", this::accept);
            assertAggregator(samples.get(3), GlobalSumAggregator.class, "s4", this::accept);
            assertAggregator(samples.get(4), LocalCountAggregator.class, "s5", aggregator -> assertThat(aggregator)
                .asInstanceOf(InstanceOfAssertFactories.type(LocalCountAggregator.class))
                .extracting(LocalCountAggregator::lowerBound, LocalCountAggregator::upperBound)
                .containsExactly(Instant.parse("2022-04-03T12:45:03.123Z"), Instant.parse("2022-07-03T12:45:03.123Z")));
          });
    }

    private void assertAggregator(TsdlSample sample, Class<? extends TsdlAggregator> clazz, String identifier, Consumer<TsdlAggregator> moreChecks) {
      assertThat(sample)
          .asInstanceOf(InstanceOfAssertFactories.type(TsdlSample.class))
          .satisfies(s -> {
            assertThat(s.aggregator()).isInstanceOf(clazz);
            assertThat(s.identifier().name()).isEqualTo(identifier);
            moreChecks.accept(s.aggregator());
          });
    }

    @ValueSource(strings = FULL_FEATURE_QUERY)
    @ParameterizedTest
    void integration_detectsEvents(String queryString) {
      var query = PARSER.parseQuery(queryString);

      assertThat(query.events())
          .asInstanceOf(InstanceOfAssertFactories.list(TsdlEvent.class))
          .hasSize(3)
          .satisfies(events -> {
            assertThat(events.get(0))
                .asInstanceOf(InstanceOfAssertFactories.type(TsdlEvent.class))
                .extracting(TsdlEvent::definition, TsdlEvent::identifier)
                .containsExactly(
                    ELEMENTS.getConnective(ConnectiveIdentifier.AND,
                        List.of(ELEMENTS.getFilter(FilterType.LT, ELEMENTS.getFilterArgument(3.5)))
                    ),
                    ELEMENTS.getIdentifier("low")
                );

            assertThat(events.get(1))
                .asInstanceOf(InstanceOfAssertFactories.type(TsdlEvent.class))
                .extracting(TsdlEvent::definition, TsdlEvent::identifier)
                .containsExactly(
                    ELEMENTS.getConnective(ConnectiveIdentifier.OR,
                        List.of(ELEMENTS.getNegatedFilter(ELEMENTS.getFilter(FilterType.GT, ELEMENTS.getFilterArgument(7.0))))
                    ),
                    ELEMENTS.getIdentifier("high")
                );

            assertThat(events.get(2))
                .asInstanceOf(InstanceOfAssertFactories.type(TsdlEvent.class))
                .satisfies(event -> {
                  assertThat(event.definition().filters())
                      .hasSize(1)
                      .element(0, InstanceOfAssertFactories.type(GreaterThanFilter.class))
                      .extracting(GreaterThanFilter::threshold, InstanceOfAssertFactories.type(TsdlSampleFilterArgument.class))
                      .extracting(arg -> arg.sample().identifier().name(), InstanceOfAssertFactories.STRING)
                      .isEqualTo("s2");

                  assertThat(event.identifier()).isEqualTo(ELEMENTS.getIdentifier("mid"));
                });
          });
    }

    @ValueSource(strings = FULL_FEATURE_QUERY)
    @ParameterizedTest
    void integration_detectsChoice(String queryString) {
      var query = PARSER.parseQuery(queryString);

      var low = query.events().stream().filter(event -> event.identifier().name().equals("low")).findFirst().orElseThrow();
      var high = query.events().stream().filter(event -> event.identifier().name().equals("high")).findFirst().orElseThrow();
      assertThat(query.choice())
          .asInstanceOf(InstanceOfAssertFactories.optional(PrecedesOperator.class))
          .isPresent().get()
          .extracting(PrecedesOperator::cardinality, PrecedesOperator::operand1, PrecedesOperator::operand2)
          .containsExactly(2, low, high);
    }

    @Test
    void integration_duplicateIdentifierDeclarationInSameGroup_throws() {
      var queryString = "WITH SAMPLES: avg(_input) AS s1, max(_input) AS s1\n"
          + "          YIELD: all periods";

      assertThatThrownBy(() -> PARSER.parseQuery(queryString))
          .isInstanceOf(TsdlParserException.class)
          .hasCauseInstanceOf(DuplicateIdentifierException.class);
    }

    @Test
    void integration_duplicateIdentifierDeclarationInSeparateGroup_throws() {
      var queryString = """
          WITH SAMPLES: avg(_input) AS s1
                    USING EVENTS: AND(lt(3.5)) AS s1
                    YIELD: all periods""";

      assertThatThrownBy(() -> PARSER.parseQuery(queryString))
          .isInstanceOf(TsdlParserException.class)
          .hasCauseInstanceOf(DuplicateIdentifierException.class);
    }

    @Test
    void integration_optionalDirectives_parsedAsEmpty() {
      var queryString = "YIELD: data points";
      var query = PARSER.parseQuery(queryString);

      assertThat(query.filter()).isNotPresent();
      assertThat(query.samples()).isEmpty();
      assertThat(query.events()).isEmpty();
      assertThat(query.choice()).isNotPresent();
      assertThat(query.identifiers()).isEmpty();
      assertThat(query.result()).isNotNull();
    }

    private void accept(TsdlAggregator a) {
    }
  }
}
