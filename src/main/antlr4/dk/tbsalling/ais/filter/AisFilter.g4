grammar AisFilter;

filter: filterExpression EOF;

filterExpression:                     #root
    | MSGID compareTo INT             #msgid
    | MMSI compareTo INT              #mmsi
    | MSGID (in|notin) intList        #msgidInList
    | MMSI (in|notin) intList         #mmsiInList
    | (SOG|COG) compareTo (INT|FLOAT) #sogCog
    | (LAT|LNG) compareTo FLOAT       #latLng
    |  left=filterExpression (op=(AND|OR) right=filterExpression)+ # andOr
    ;

compareTo : neq|eq|gt|gte|lte|lt ;

neq : '!=';
eq : '=';
gt : '>';
gte : '>=';
lte : '<=';
lt : '<';

in : 'in'|'IN' ;
notin : 'not in'|'NOT IN';

intList  : '('? INT (',' INT)* ')'? ;

//number : INT|FLOAT;

AND     : '&' | 'and' ;
OR      : '|' | 'or';
INT     : '-'? [0-9]+;
FLOAT   : '-'? [0-9]* '.' [0-9]+ ;
//STRING  : [a-zA-Z0-9_?\*]+ | '\'' .*? '\'' ;
WS      : [ \n\r\t]+ -> skip ; // toss out whitespace

MSGID : 'msgid';
MMSI: 'mmsi';
SOG: 'sog';
COG: 'cog';
LAT: 'lat';
LNG: 'lng';