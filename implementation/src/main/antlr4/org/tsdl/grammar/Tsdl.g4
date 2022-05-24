grammar Tsdl;

tsdl
  :  filtersDeclaration mandatoryWhitespace yieldDeclaration
  ;

yieldDeclaration
  : YIELD
  ;

filtersDeclaration
  :  FILTER_CLAUSE COLON mandatoryWhitespace filterConnective
  ;

filterConnective
  : connectiveIdentifier PARENTHESIS_OPEN whitespace singlePointFilterList whitespace PARENTHESIS_CLOSE
  ;

connectiveIdentifier
  : CONNECTIVE_AND
  | CONNECTIVE_OR
  ;

singlePointFilterList
  :  singlePointFilters filterSeparator singlePointFilterDeclaration     // either two or more parameters
  |  singlePointFilterDeclaration                                        // or exactly one
  ;

singlePointFilters
  :  singlePointFilterDeclaration (filterSeparator singlePointFilterDeclaration)* // one parameter plus [0..n] additional parameters
  ;

singlePointFilterDeclaration
  :  singlePointFilter
  |  negatedSinglePointFilter
  ;

singlePointFilter
  :  filterType PARENTHESIS_OPEN whitespace NUMBER whitespace PARENTHESIS_CLOSE
  ;

negatedSinglePointFilter
  : CONNECTIVE_NOT PARENTHESIS_OPEN singlePointFilter PARENTHESIS_CLOSE
  | singlePointFilter
  ;

filterType
  : OPERATOR_GT
  | OPERATOR_LT
  ;

whitespace
  : WHITESPACE*
  ;

mandatoryWhitespace
  : WHITESPACE+
  ;

WHITESPACE
  :  ' '
  |  '\r'
  |  '\n'
  |  '\r\n'
  ;

filterSeparator
  : ',' whitespace
  ;

FILTER_CLAUSE
  : 'FILTER'
  ;

FILTER_ALL
  : 'ALL'
  ;

FILTER_ANY
  : 'ANY'
  ;

YIELD
  :  'YIELD *'
  ;

CONNECTIVE_NOT
  :  'NOT'
  ;

CONNECTIVE_AND
  :  'AND'
  ;

CONNECTIVE_OR
  :  'OR'
  ;

OPERATOR_GT
  :  'gt'
  ;

OPERATOR_LT
  :  'lt'
  ;


PARENTHESIS_OPEN
  :  '('
  ;

PARENTHESIS_CLOSE
  :  ')'
  ;

COLON
  : ':'
  ;

NUMBER : INT | FLOAT;

fragment DIGIT
  :  [0-9]
  ;

fragment SIGN
  :  '-'?
  ;

INT
  :  SIGN? DIGIT+
  ;

FLOAT
  :  SIGN? DIGIT+ '.' DIGIT+
  ;

TERMINATOR : [\r\n]+ -> channel(HIDDEN);