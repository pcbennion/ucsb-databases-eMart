ucsb-databases-eMart
====================

Term project for cs174a (databases). Basic implementation of a database-driven online store.

Authors:
Peter Bennion
Edmund Luong

Original repository located at: 
http://github.com/edluong/UCSB-Databases-Project

Program is multithreaded, uses a jFrame interface, and is intended for use with an Oracle DBMS. The username and password of a valid oracle DBMS (containing an appropriate database) must be passed as command-line parameters. The database this program was designed with belongs to UCSB, so login info is not supplied.

It creates three interfaces: a customer interface; a store manager interface; an a warehouse interface. 
- The customer interface supports searching for items, adding items to cart, checking out, and viewing personal order history. 
- The store manager interface supports searching for items, viewing total order history, and filtering orders by certain attributes.
- The warehouse manager interface supports searching for stock by ID, viewing items that need to be replentished, and registering shipment invoices.

Due to the limited development timeframe, this program is far from perfect. There are threading issues that can cause crashes, and not all database functions are stable. Features that were not important for the final grade of the project were not fully implemented.
