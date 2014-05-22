ucsb-databases-eMart
====================

Term project for cs174a (databases). Basic implementation of a database-driven online store.

Authors:
Peter Bennion
Edmund Luong

Original repository located at: 
http://github.com/edluong/UCSB-Databases-Project

Program is multithreaded, uses a jFrame interface, and is intended for use with an Oracle DBMS. The username and password of a valid oracle DBMS (containing an appropriate database) must be passed as command-line arguments. The database this program was designed for belongs to UCSB, so login info is not supplied. Due to the limited timeframe of the project, features and bugfixes that were not important for the final grade were not fully implemented.

It creates three interfaces: a customer interface; a store manager interface; an a warehouse interface. 
- The customer interface supports searching for items, adding items to cart, checking out, and viewing personal order history. 
- The store manager interface supports searching for items, viewing total order history, and filtering orders by certain attributes.
- The warehouse manager interface supports searching for stock by ID, viewing items that need to be replentished, and registering shipment invoices.

A rough outline of the database relations is located in Projdetails.txt. 
