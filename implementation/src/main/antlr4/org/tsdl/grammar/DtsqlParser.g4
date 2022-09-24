parser grammar DtsqlParser;

options
{
  tokenVocab = DtsqlLexer;
}

dtsqlQuery
  :  WHITESPACE?
       (samplesDeclaration WHITESPACE)?
       (filtersDeclaration WHITESPACE)?
       (eventsDeclaration WHITESPACE)?
       (selectDeclaration WHITESPACE)?
       yieldDeclaration WHITESPACE?
       EOF // include EOF (generated by lexer at end of source input) to ensure there is no unparsed content left
  ;

samplesDeclaration
  :  SAMPLES_CLAUSE COLON WHITESPACE aggregatorsDeclarationStatement
  ;

eventsDeclaration
  :  EVENTS_CLAUSE COLON WHITESPACE eventsDeclarationStatement
  ;

eventsDeclarationStatement
  :  eventList
  ;

eventList
  :  events LIST_SEPARATOR eventDeclaration     // either two or more events
  |  eventDeclaration                           // or exactly one
  ;

events
  :  eventDeclaration (LIST_SEPARATOR eventDeclaration)*   // one event plus [0..n] additional events
  ;

eventDeclaration
  :  eventConnective WHITESPACE? (durationSpecification WHITESPACE?)? identifierDeclaration
  ;

eventConnective
  :  CONNECTIVE_IDENTIFIER PARENTHESIS_OPEN WHITESPACE? eventFunctionList WHITESPACE? PARENTHESIS_CLOSE
  ;

eventFunctionList
  :  eventFunctions LIST_SEPARATOR eventFunctionDeclaration     // either two or more parameters
  |  eventFunctionDeclaration                                        // or exactly one
  ;

eventFunctions
  :  eventFunctionDeclaration (LIST_SEPARATOR eventFunctionDeclaration)* // one parameter plus [0..n] additional parameters
  ;

eventFunctionDeclaration
  :  singlePointFilterDeclaration
  |  complexEventDeclaration
  ;

complexEventDeclaration
  :  complexEvent
  |  negatedComplexEvent
  ;

complexEvent
  :  constantEvent
  |  increaseEvent
  |  decreaseEvent
  ;

negatedComplexEvent
  :  CONNECTIVE_NOT PARENTHESIS_OPEN WHITESPACE? complexEvent WHITESPACE? PARENTHESIS_CLOSE
  ;

// first scalar: non-negative real number, second scalar: non-negative real number
constantEvent
  :  EVENT_CONSTANT PARENTHESIS_OPEN WHITESPACE? slope=scalarArgument LIST_SEPARATOR deviation=scalarArgument WHITESPACE? PARENTHESIS_CLOSE
  ;

// first scalar: non-negative real number, second scalar: non-negative real number, third scalar: real number
// additionally: first scalar <= second scalar
increaseEvent
  :  EVENT_INCREASE PARENTHESIS_OPEN WHITESPACE? minChange=scalarArgument LIST_SEPARATOR monotonicUpperBound LIST_SEPARATOR tolerance=scalarArgument WHITESPACE? PARENTHESIS_CLOSE
  ;

// first scalar: non-negative real number, second scalar: non-negative real number, third scalar: real number
// additionally: first scalar <= second scalar
decreaseEvent
  :  EVENT_DECREASE PARENTHESIS_OPEN WHITESPACE? minChange=scalarArgument LIST_SEPARATOR monotonicUpperBound LIST_SEPARATOR tolerance=scalarArgument WHITESPACE? PARENTHESIS_CLOSE
  ;

monotonicUpperBound
  :  scalarArgument
  |  HYPHEN
  ;

durationSpecification
  : EVENT_DURATION WHITESPACE TIME_UNIT
  ;

selectDeclaration
  :  SELECT_CLAUSE COLON WHITESPACE temporalRelation
  ;

temporalRelation
  :  PARENTHESIS_OPEN op1=IDENTIFIER WHITESPACE TEMPORAL_RELATION WHITESPACE op2=IDENTIFIER WHITESPACE? timeToleranceSpecification? PARENTHESIS_CLOSE  #EventEvent
  |  PARENTHESIS_OPEN op1=IDENTIFIER WHITESPACE TEMPORAL_RELATION WHITESPACE op2=temporalRelation WHITESPACE? timeToleranceSpecification? PARENTHESIS_CLOSE  #EventRecursive
  |  PARENTHESIS_OPEN op1=temporalRelation WHITESPACE TEMPORAL_RELATION WHITESPACE op2=IDENTIFIER WHITESPACE? timeToleranceSpecification? PARENTHESIS_CLOSE  #RecursiveEvent
  |  PARENTHESIS_OPEN op1=temporalRelation WHITESPACE TEMPORAL_RELATION WHITESPACE op2=temporalRelation WHITESPACE? timeToleranceSpecification? PARENTHESIS_CLOSE  #RecursiveRecursive
  ;

timeToleranceSpecification
  : TIME_TOLERANCE WHITESPACE TIME_UNIT
  ;

yieldDeclaration
  :  YIELD COLON WHITESPACE yieldType
  ;

yieldType
  :  YIELD_ALL_PERIODS
  |  YIELD_LONGEST_PERIOD
  |  YIELD_SHORTEST_PERIOD
  |  YIELD_DATA_POINTS
  |  YIELD_SAMPLE WHITESPACE IDENTIFIER
  |  YIELD_SAMPLE_SET WHITESPACE identifierList
  ;

filtersDeclaration
  :  FILTER_CLAUSE COLON WHITESPACE filterConnective
  ;

filterConnective
  :  CONNECTIVE_IDENTIFIER PARENTHESIS_OPEN WHITESPACE? singlePointFilterList WHITESPACE? PARENTHESIS_CLOSE
  ;

aggregatorsDeclarationStatement
  :  aggregatorList
  ;

aggregatorList
  :  aggregators LIST_SEPARATOR aggregatorDeclaration      // either two or more aggregators
  |  aggregatorDeclaration                                      // or exactly one
  ;

aggregators
  :  aggregatorDeclaration (LIST_SEPARATOR aggregatorDeclaration)*    // one aggregator plus [0..n] additional aggregators
  ;

aggregatorDeclaration
  :  aggregatorFunctionDeclaration WHITESPACE identifierDeclaration (WHITESPACE? echoStatement)?
  ;

echoStatement
  :  ECHO_ARROW WHITESPACE? ECHO_LABEL WHITESPACE? PARENTHESIS_OPEN WHITESPACE? echoArgumentList? WHITESPACE? PARENTHESIS_CLOSE
  ;

echoArgumentList
  :  echoArguments LIST_SEPARATOR echoArgument      // either two or more echo arguments
  |  echoArgument                                          // or exactly one
  ;

echoArguments
  :  echoArgument (LIST_SEPARATOR echoArgument)*    // one echo argument plus [0..n] additional arguments
  ;

identifierList
  :  identifiers LIST_SEPARATOR IDENTIFIER      // either two or more identifiers
  |  IDENTIFIER                                       // or exactly one
  ;

identifiers
  :  IDENTIFIER (LIST_SEPARATOR IDENTIFIER)*    // one aggregator plus [0..n] additional aggregators
  ;

aggregatorFunctionDeclaration
  :  valueAggregatorDeclaration
  |  temporalAggregatorDeclaration
  ;

valueAggregatorDeclaration
  :  VALUE_AGGREGATOR_FUNCTION PARENTHESIS_OPEN WHITESPACE? timeRange? WHITESPACE? PARENTHESIS_CLOSE
  ;

timeRange
  :  STRING_LITERAL LIST_SEPARATOR STRING_LITERAL  // expected: ISO-8601 UTC timestamp, e.g. '2011-12-03T10:15:30.123+04:00' (to be validated by application)
  ;

// expected string literal: T1/T2, both ISO-8601 UTC timestamps, e.g., '2011-12-03T10:15:30.123+04:00/2011-12-04T14:45:30.123Z' (app validation)
// 'count_t' is a special case because it does not take a unit argument
temporalAggregatorDeclaration
  :  TEMPORAL_AGGREGATOR_FUNCTION PARENTHESIS_OPEN WHITESPACE? TIME_UNIT LIST_SEPARATOR intervalList WHITESPACE? PARENTHESIS_CLOSE
  |  UNITLESS_TEMPORAL_AGGREGATOR_FUNCTION PARENTHESIS_OPEN WHITESPACE? intervalList WHITESPACE? PARENTHESIS_CLOSE
  ;

// filter argument (STRING_LITERAL): ISO-8601 UTC timestamp, e.g. '2011-12-03T10:15:30+04:00' (to be validated by application)
intervalList
  :  intervals LIST_SEPARATOR STRING_LITERAL     // either two or more events
  |  STRING_LITERAL                              // or exactly one
  ;

intervals
  :  STRING_LITERAL (LIST_SEPARATOR STRING_LITERAL)*   // one interval plus [0..n] additional intervals
  ;

identifierDeclaration
  :  AS WHITESPACE IDENTIFIER
  ;

echoArgument
  :  ECHO_ARGUMENT
  |  IDENTIFIER
  |  NUMBER
  ;

singlePointFilterList
  :  singlePointFilters LIST_SEPARATOR singlePointFilterDeclaration     // either two or more parameters
  |  singlePointFilterDeclaration                                        // or exactly one
  ;

singlePointFilters
  :  singlePointFilterDeclaration (LIST_SEPARATOR singlePointFilterDeclaration)* // one parameter plus [0..n] additional parameters
  ;

singlePointFilterDeclaration
  :  singlePointFilter
  |  negatedSinglePointFilter
  ;

singlePointFilter
  :  thresholdFilter
  |  temporalFilter
  |  deviationFilter
  ;

negatedSinglePointFilter
  :  CONNECTIVE_NOT PARENTHESIS_OPEN WHITESPACE? singlePointFilter WHITESPACE? PARENTHESIS_CLOSE
  ;

// filter argument: STRING_LITERAL; ISO-8601 UTC timestamp, e.g. '2011-12-03T10:15:30+04:00' (to be validated by application)
temporalFilter
  :  TEMPORAL_FILTER_TYPE PARENTHESIS_OPEN WHITESPACE? STRING_LITERAL WHITESPACE? PARENTHESIS_CLOSE
  ;

thresholdFilter
  :  THRESHOLD_FILTER_TYPE PARENTHESIS_OPEN WHITESPACE? scalarArgument WHITESPACE? PARENTHESIS_CLOSE
  ;

scalarArgument
  :  NUMBER
  |  IDENTIFIER
  ;

deviationFilter
  :  DEVIATION_FILTER_TYPE PARENTHESIS_OPEN WHITESPACE? deviationFilterArguments WHITESPACE? PARENTHESIS_CLOSE
  ;

deviationFilterArguments
  :  AROUND_FILTER_TYPE LIST_SEPARATOR reference=scalarArgument LIST_SEPARATOR deviation=scalarArgument
  ;