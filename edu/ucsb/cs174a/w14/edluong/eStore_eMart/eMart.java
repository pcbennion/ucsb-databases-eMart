package edu.ucsb.cs174a.w14.edluong.eStore_eMart;

import java.sql.*;
import java.util.*;

public class eMart implements Runnable{
	
	private static eMart ref = null;
	
	// Members that can be operated on by other threads
	private volatile Queue<MartCmd> cmdQueue;

	/**
	 * Singleton class reference accessor/constructor
	 */
	public static eMart Ref() {
		if(ref==null) ref = new eMart();
		return ref;
	}
	private eMart() {
		cmdQueue = new LinkedList<MartCmd>();
	}
	
	/**
	 * Sends the appropriate callback request to this thread. Will fail if there is already a command waiting to be executed.
	 * @param c: the command to be executed.
	 */
	public void inputCommand(MartCmd c) {
		cmdQueue.add(c);
	}

	/**
	 * Begins the thread and waits for input. When an input exists, it executes the input's command.
	 */
	@Override
	public void run() {
		System.out.println("eMart Controller - Hello World!");
		try {
			System.out.println("eMart Controller - Listening for Input.");
			while (!Thread.currentThread().isInterrupted()) {
	            try {
	            	Thread.sleep(50);
	            	while(!cmdQueue.isEmpty()) {
	            		System.out.println("eMart Controller - Input Event!");
	            		cmdQueue.remove().execute();
	            	}
	            }catch(InterruptedException ex) {break;}
			}
			System.out.println("eMart Controller - Listener Closed");
		} catch (SQLException e) {e.printStackTrace();}	
	}
	
	// ====================================================================================================
	// The Java version of thread callbacks : Class interfaces!
	//		These represent database commands for eMart.
	//		Instantiate one of these bad boys and pass to this thread; execute will be called as specified.
	//
	// Here is the base interface:
	// ====================================================================================================
	private interface MartCmd {
		public void execute() throws SQLException;
	}
	// ====================================================================================================
	// Valid eMart command implementations follow :
	// ====================================================================================================
	/**
	 * Get full catalog. Constructor takes no arguments.
	 */
	public static class QueryCatalog implements MartCmd {
		private int dest;
		public QueryCatalog(int d) {dest = d;}
		@Override
		public void execute() throws SQLException {
			// Assemble command string
			String cmd =  	"SELECT * ";
			cmd +=			"FROM Catalog c";
			System.out.println("\tCatalog Query - Hello World! Command = " + cmd);
			// Execute and push result
			switch(dest){
				case Database.DEST_CSTMR:
					CustGUI.Ref().SetCatalogData(Database.stmt.executeQuery(cmd));
				default:
			}
		}
	}
	/**
	 * Search items by attribute. Constructor takes attribute name and value.
	 */
	public static class QueryCatalogAttr implements MartCmd {
		private String attrname, value;
		private char op;
		public QueryCatalogAttr(String attrname, char op, String value) {
			// Asserts to make sure command fields are kosher
			assert(Database.CatalogCol.contains(attrname));
			assert(op == '=' || op == '<' || op == '>');
			this.attrname=attrname; this.op = op; this.value=value;
		}
		@Override
		public void execute() throws SQLException {
			// Assemble command string
			String cmd =  	"SELECT * ";
			cmd +=			"FROM Catalog c ";
			cmd +=			"WHERE c." + attrname + " " + op + " " +  value;
			System.out.println("\tCatalog Query - Hello World! Command = " + cmd);
			// Execute and return result
			//return Database.stmt.executeQuery(cmd);
		}
	}
	/**
	 * Search items by description. Constructor takes attribute name and value.
	 */
	public static class QueryCatalogDesc implements MartCmd {
		private String attrname, value;
		public QueryCatalogDesc(String attrname, String value) {this.attrname=attrname; this.value=value;}
		@Override
		public void execute() throws SQLException {
			// Assemble command string
			String cmd =  	"SELECT * ";
			cmd +=			"FROM Catalog c ";
			cmd +=			"WHERE c.iid = (SELECT d.iid FROM Descriptions WHERE d.attribute = " + attrname + " AND d.value = " + value + ")";
			System.out.println("\tCatalog Query - Hello World! Command = " + cmd);
			// Execute and return result
			//return Database.stmt.executeQuery(cmd);
		}
	}
	/**
	 * Search items by accessory. Constructor takes item id.
	 */
	public static class QueryCatalogAcc implements MartCmd {
		private String iid;
		public QueryCatalogAcc(String iid) {this.iid=iid;}
		@Override
		public void execute() throws SQLException {
			// Assemble command string
			String cmd =  	"SELECT * ";
			cmd +=			"FROM Catalog c ";
			cmd +=			"WHERE c.iid = (SELECT a.iid2 FROM Accessories WHERE a.iid1 = " + iid + ")";
			System.out.println("\tCatalog Query - Hello World! Command = " + cmd);
			// Execute and return result
			//return Database.stmt.executeQuery(cmd);
		}
	}
	/**
	 * Fetch the cart of a customer. Constructor takes customer id.
	 */
	public static class QueryCartItems implements MartCmd {
		private String cid;
		public QueryCartItems(String cid) {this.cid=cid;}
		@Override
		public void execute() throws SQLException {
			// Assemble command string
			String cmd =  	"SELECT * ";
			cmd +=			"FROM Customers c ";
			cmd +=			"WHERE c.cid = " + cid;
			System.out.println("\tCatalog Query - Hello World! Command = " + cmd);
			// Execute and return result
			//return Database.stmt.executeQuery(cmd);
		}
	}
	/**
	 * Search customer orders by oid. Constructor takes customer id and order id.
	 */
	public static class QueryCustOrders implements MartCmd {
		private String cid, oid;
		public QueryCustOrders(String cid, String oid) {this.cid=cid; this.oid=oid;}
		@Override
		public void execute() throws SQLException {
			// Assemble command string
			String cmd =  	"SELECT * ";
			cmd +=			"FROM Orders o ";
			cmd +=			"WHERE o.cid = " + cid + " AND o.oid = " + oid;
			System.out.println("\tCatalog Query - Hello World! Command = " + cmd);
			// Execute and return result
			//return Database.stmt.executeQuery(cmd);
		}
	}
	/**
	 * Adds/removes item specified by iid. Constructor takes item id and quantity (negative for removal).
	 */
	public static class UpdItemCart implements MartCmd {
		private String iid;
		private int quantity;
		public UpdItemCart(String iid, int quantity) {this.iid=iid; this.quantity=quantity;}
		@Override
		public void execute() {
			// TODO Stub
		}
	}
	/**
	 * Creates and finalizes a new order from the customer's cart. Empties cart. Constructor takes customer id.
	 */
	public static class AddOrder implements MartCmd {
		private String cid;
		public AddOrder(String cid) {this.cid=cid;}
		@Override
		public void execute() {
			// TODO Stub
		}
	}
	/**
	 * Creates and finalizes a copy of an order. Constructor takes customer id and order id.
	 */
	public static class AddOrderCopy implements MartCmd {
		private String cid, oid;
		public AddOrderCopy(String cid, String oid) {this.cid=cid; this.oid=oid;}
		@Override
		public void execute() {
			// TODO Stub
		}
	}
	// ====================================================================================================
	// End command implementations.
	// ====================================================================================================
}
