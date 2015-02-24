grammar AisFilter;

filter: filterExpression EOF;

filterExpression:
    |   MSGID compareTo INT
    //|   MSGID (in|notin) (intRange|intList)
      |   MMSI compareTo INT
    //|   MMSI (in|notin) (intRange|intList)
    //|   filterExpression (op=(AND|OR) filterExpression)+
    //|   '(' filterExpression ')'
    ;

compareTo : '!='|'='|'>'|'>='|'<='|'<' ;
//in : '@'|'in'|'IN'|'=' ;
//notin : '!@'|'not in'|'NOT IN'|'!=';

//intList  : '('? INT (',' INT)* ')'? ;
//stringList : '('? string (',' string)* ')'? ;

//intRange : '('? INT RANGE INT ')'? ;
//numberRange : '('? number RANGE number ')'? ;

//number : INT|FLOAT;
//string : number|STRING;

AND     : '&' ;
//OR      : '|' ;
//RANGE   : '..';
INT     : '-'? [0-9]+;
//FLOAT   : '-'? [0-9]* '.' [0-9]+ ;
//STRING  : [a-zA-Z0-9_?\*]+ | '\'' .*? '\'' ;
//WS      : [ \n\r\t]+ -> skip ; // toss out whitespace

//MSGID : 'id';
MMSI: 'mmsi';
