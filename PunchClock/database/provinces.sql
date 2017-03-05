drop table if exists provinces;

CREATE TABLE provinces (
id smallint unsigned not null auto_increment comment 'PK: Unique province ID',
name VARCHAR( 32 ) NOT NULL ,
abbr VARCHAR( 8 ) NOT NULL ,
PRIMARY KEY ( id )
)TYPE=MyISAM ROW_FORMAT=DYNAMIC;

INSERT INTO provinces
VALUES
(NULL, 'Alberta', 'AB'),
(NULL, 'British Columbia', 'BC'),
(NULL, 'Manitoba', 'MB'),
(NULL, 'New Brunswick', 'NB'),
(NULL, 'Newfoundland and Labrador', 'NL'),
(NULL, 'Northwest Territories', 'NT'),
(NULL, 'Nova Scotia', 'NS'),
(NULL, 'Nunavut', 'NU'),
(NULL, 'Ontario', 'ON'),
(NULL, 'Prince Edward Island', 'PE'),
(NULL, 'Quebec', 'QC'),
(NULL, 'Saskatchewan', 'SK'),
(NULL, 'Yukon', 'YT')
;
