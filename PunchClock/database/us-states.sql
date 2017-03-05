#
# http://kimbriggs.com/ 2007-05
# Creates a table of state IDs, names, and abbreviations.  Over 65,000 records allowed.
# Additional insert statement fills table for US states and DC.
#


drop table if exists states;


CREATE TABLE states
(
id smallint unsigned not null auto_increment comment 'PK: Unique state ID',
name varchar(32) not null comment 'State name with first letter capital',
abbr varchar(8) comment 'Optional state abbreviation (US is 2 capital letters)',

primary key (id)
)TYPE=MyISAM ROW_FORMAT=DYNAMIC;


insert into states
values
(NULL, 'Alabama', 'AL'),
(NULL, 'Alaska', 'AK'),
(NULL, 'Arizona', 'AZ'),
(NULL, 'Arkansas', 'AR'),
(NULL, 'California', 'CA'),
(NULL, 'Colorado', 'CO'),
(NULL, 'Connecticut', 'CT'),
(NULL, 'Delaware', 'DE'),
(NULL, 'District of Columbia', 'DC'),
(NULL, 'Florida', 'FL'),
(NULL, 'Georgia', 'GA'),
(NULL, 'Hawaii', 'HI'),
(NULL, 'Idaho', 'ID'),
(NULL, 'Illinois', 'IL'),
(NULL, 'Indiana', 'IN'),
(NULL, 'Iowa', 'IA'),
(NULL, 'Kansas', 'KS'),
(NULL, 'Kentucky', 'KY'),
(NULL, 'Louisiana', 'LA'),
(NULL, 'Maine', 'ME'),
(NULL, 'Maryland', 'MD'),
(NULL, 'Massachusetts', 'MA'),
(NULL, 'Michigan', 'MI'),
(NULL, 'Minnesota', 'MN'),
(NULL, 'Mississippi', 'MS'),
(NULL, 'Missouri', 'MO'),
(NULL, 'Montana', 'MT'),
(NULL, 'Nebraska', 'NE'),
(NULL, 'Nevada', 'NV'),
(NULL, 'New Hampshire', 'NH'),
(NULL, 'New Jersey', 'NJ'),
(NULL, 'New Mexico', 'NM'),
(NULL, 'New York', 'NY'),
(NULL, 'North Carolina', 'NC'),
(NULL, 'North Dakota', 'ND'),
(NULL, 'Ohio', 'OH'),
(NULL, 'Oklahoma', 'OK'),
(NULL, 'Oregon', 'OR'),
(NULL, 'Pennsylvania', 'PA'),
(NULL, 'Rhode Island', 'RI'),
(NULL, 'South Carolina', 'SC'),
(NULL, 'South Dakota', 'SD'),
(NULL, 'Tennessee', 'TN'),
(NULL, 'Texas', 'TX'),
(NULL, 'Utah', 'UT'),
(NULL, 'Vermont', 'VT'),
(NULL, 'Virginia', 'VA'),
(NULL, 'Washington', 'WA'),
(NULL, 'West Virginia', 'WV'),
(NULL, 'Wisconsin', 'WI'),
(NULL, 'Wyoming', 'WY')
;


 