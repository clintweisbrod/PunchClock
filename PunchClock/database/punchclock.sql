# EMS MySQL Manager 1.9.6.2
# ---------------------------------------
# Host     : localhost
# Port     : 3306
# Database : Consulting


CREATE DATABASE Consulting;

USE Consulting;

#
# Structure for table companies : 
#

CREATE TABLE `companies` (
  `id` int(4) unsigned NOT NULL auto_increment,
  `name` varchar(64) default NULL,
  `address` varchar(64) default NULL,
  `city` varchar(32) default NULL,
  `state` char(2) default NULL,
  `zip` varchar(16) default NULL,
  `billingContact` varchar(32) default NULL,
  `timesheetPath` varchar(255) default NULL,
  `invoicePath` varchar(255) default NULL,
  PRIMARY KEY  (`id`)
) TYPE=MyISAM ROW_FORMAT=DYNAMIC;

#
# Structure for table expenses : 
#

CREATE TABLE `expenses` (
  `id` int(11) NOT NULL auto_increment,
  `companyID` int(11) default NULL,
  `date` datetime default NULL,
  `amount` int(11) unsigned default NULL,
  `gstApplicable` int(11) default '0',
  `description` varchar(64) default NULL,
  PRIMARY KEY  (`id`)
) TYPE=MyISAM ROW_FORMAT=DYNAMIC;

#
# Structure for table invoices : 
#

CREATE TABLE `invoices` (
  `id` int(11) NOT NULL auto_increment,
  `companyID` int(11) default NULL,
  `periodStart` datetime default NULL,
  `periodEnd` datetime default NULL,
  `date` datetime default NULL,
  `total` int(11) unsigned default NULL,
  `gst` int(11) unsigned default NULL,
  PRIMARY KEY  (`id`)
) TYPE=MyISAM ROW_FORMAT=DYNAMIC;

#
# Structure for table tasks : 
#

CREATE TABLE `tasks` (
  `id` int(11) NOT NULL default '0',
  `companyID` int(11) default NULL,
  `name` varchar(64) default NULL,
  `code` varchar(16) default NULL,
  `rate` int(11) default NULL,
  PRIMARY KEY  (`id`)
) TYPE=MyISAM ROW_FORMAT=DYNAMIC;

#
# Structure for table timelog : 
#

CREATE TABLE `timelog` (
  `id` int(11) NOT NULL auto_increment,
  `companyID` int(11) unsigned default NULL,
  `taskID` int(11) default NULL,
  `start` datetime default NULL,
  `end` datetime default NULL,
  `comment` varchar(64) default NULL,
  PRIMARY KEY  (`id`)
) TYPE=MyISAM ROW_FORMAT=DYNAMIC;

