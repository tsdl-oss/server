parser grammar TsdlParser;

options
{
  tokenVocab = TsdlLexer;
}

tsdlQuery
  :  WHITESPACE?
       (samplesDeclaration WHITESPACE)?
       (filtersDeclaration WHITESPACE)?
       (eventsDeclaration WHITESPACE)?
       (chooseDeclaration WHITESPACE)?
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
  :  filterConnective WHITESPACE? (durationSpecification WHITESPACE?)? identifierDeclaration
  ;

durationSpecification
  : EVENT_DURATION WHITESPACE TIME_UNIT
  ;

chooseDeclaration
  :  CHOOSE_CLAUSE COLON WHITESPACE choiceStatement
  ;

choiceStatement
  :  IDENTIFIER WHITESPACE TEMPORAL_RELATION WHITESPACE IDENTIFIER WHITESPACE? timeToleranceSpecification?
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
  :  AGGREGATOR_FUNCTION PARENTHESIS_OPEN WHITESPACE? timeRange? WHITESPACE? PARENTHESIS_CLOSE
  ;

timeRange
  :  STRING_LITERAL LIST_SEPARATOR STRING_LITERAL  // expected: ISO-8601 UTC timestamp, e.g. '2011-12-03T10:15:30.123+04:00' (to be validated by application)
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
  :  THRESHOLD_FILTER_TYPE PARENTHESIS_OPEN WHITESPACE? thresholdFilterArgument WHITESPACE? PARENTHESIS_CLOSE
  ;

thresholdFilterArgument
  :  NUMBER
  |  IDENTIFIER
  ;

deviationFilter
  :  DEVIATION_FILTER_TYPE PARENTHESIS_OPEN WHITESPACE? deviationFilterArguments WHITESPACE? PARENTHESIS_CLOSE
  ;

// NUMBER argument is in [0, 100] for type 'rel', otherwise unconstrained
deviationFilterArguments
  :  AROUND_FILTER_TYPE LIST_SEPARATOR thresholdFilterArgument LIST_SEPARATOR NUMBER
  ;
