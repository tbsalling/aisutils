grammar AisFilter;

filter: filterExpression EOF;

filterExpression:           #root
    |   MSGID compareToInt INT #msgid
    //|   MSGID (in|notin) (intRange|intList)
    |   MMSI compareToInt INT  #mmsi
    //|   MMSI (in|notin) (intRange|intList)
    //|   '(' filterExpression ')'

    |   left=filterExpression (op=(AND|OR) right=filterExpression)+ # and
   // |   '(' filterExpression ')'                    # parens
    ;

compareToInt : neq|eq|gt|gte|lte|lt ;

neq : '!=';
eq : '=';
gt : '>';
gte : '>=';
lte : '<=';
lt : '<';

//in : '@'|'in'|'IN'|'=' ;
//notin : '!@'|'not in'|'NOT IN'|'!=';

//intList  : '('? INT (',' INT)* ')'? ;
//stringList : '('? string (',' string)* ')'? ;

//intRange : '('? INT RANGE INT ')'? ;
//numberRange : '('? number RANGE number ')'? ;

//number : INT|FLOAT;
//string : number|STRING;

AND     : '&' | 'and' ;
OR      : '|' | 'or';
//RANGE   : '..';
INT     : '-'? [0-9]+;
//FLOAT   : '-'? [0-9]* '.' [0-9]+ ;
//STRING  : [a-zA-Z0-9_?\*]+ | '\'' .*? '\'' ;
WS      : [ \n\r\t]+ -> skip ; // toss out whitespace

MSGID : 'msgid';
MMSI: 'mmsi';
