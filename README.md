ZucchiAdmin
===========

Admin aggregator module for Zucchi ZF2 Modules.

This is a jump off module for other ZF2 Modules to hook into to provide a web interface for administration

Installation
------------

From the root of your ZF2 Skeleton Application run

    ./composer.phar require zucchi/admin 
    
This module will require your vhost to use an AliasMatch in order to load public assets

    AliasMatch /_([^/]+)/(.+)/([^/]+) /path/to/vendor/$2/public/$1/$3


Features
--------

*    Dashboard - Simple Dashboard (under construction)
*    Controller - Some simple Controller Abstracts for Admin
*    CRUD
     *    Event - an event triggered by crud actions
     *    Trait - A trait that can be use dto provide instant crud functionality for simple entities
     *    View Helpers - Some helpers that help build the CRUD interface
*    Event Listener - Registers Admin specific listeners
*    Navigation - An Admin specific Navigation Factory
*    Layout - A ready made admin layout
*    Routes - A ready made routing structure for you to extend