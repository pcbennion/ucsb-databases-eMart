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
	 * Get full catalog. Constructor takes destination only.
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
	 * Search items for search terms specified. Constructor takes destination and search term string.
	 */
	public static class QueryCatalogSearch implements MartCmd {
		private String search;
		private int dest;
		public QueryCatalogSearch(int d, String search) {this.dest = d; this.search=search;}
		@Override
		public void execute() throws SQLException {
			// Assemble command string
			String cmd = 	"SELECT * FROM Catalog WHERE iid = ";
			cmd +=			"(SELECT UNIQUE c.iid FROM Catalog c ";
			boolean desc = search.contains("D.");
			boolean acc  = search.contains("A.");
			if(desc) {
				cmd += ", Descriptions d ";
				if(acc) cmd+=", Accessories a WHERE c.iid=d.iid AND c.iid=a.iid AND ";
				else cmd+="WHERE c.iid=d.iid AND ";
			} else if(acc) cmd+=", Accessories a WHERE c.iid=a.iid AND ";
			else cmd+="WHERE ";
			cmd +=			search +")";
			System.out.println("\tCatalog Query - Command = " + cmd);
			// Execute and push result
			switch(dest){
				case Database.DEST_CSTMR:
					CustGUI.Ref().SetCatalogData(Database.stmt.executeQuery(cmd));
				default:
			}
		}
	}
	/**
	 * Fetch the cart of a customer. Constructor takes destination and customer id.
	 */
	public static class QueryCartItems implements MartCmd {
		private String cid;
		private int dest;
		public QueryCartItems(int d, String cid) {this.dest = d; this.cid=cid;}
		@Override
		public void execute() throws SQLException {
			// Assemble command string
			String cmd =  	"SELECT c.iid, c.category, c.warranty, c.manufacturer, c.model, c.price, o.quantity ";
			cmd +=			"FROM OrderItems o, Catalog c ";
			cmd +=			"WHERE o.iid = c.iid AND o.oid = (SELECT oid FROM Customers WHERE cid = '" + cid + "')";
			System.out.println("\tCart Query - Command = " + cmd);
			// Execute and push result
			switch(dest){
				case Database.DEST_CSTMR:
					CustGUI.Ref().SetCartData(Database.stmt.executeQuery(cmd));
				default:
			}
		}
	}
	/**
	 * Get all customer orders. STUB
	 */
	public static class QueryCustOrders implements MartCmd {
		private int dest;
		public QueryCustOrders(int d) {this.dest=d;}
		@Override
		public void execute() throws SQLException {
			// TODO Stub
			String cmd1;
			cmd1 ="SELECT * ";
			cmd1 +="FROM Orders";
			
			switch(dest){
			case Database.DEST_CSTMR:
				CustGUI.Ref().SetOrdersData(Database.stmt.executeQuery(cmd1));
			}
		}
	}
	/**
	 * Search customer orders by oid. STUB
	 */
	public static class QueryOrdersOid implements MartCmd {
		private int oid;
		private int dest;
		public QueryOrdersOid(int oid,int d) {this.dest=d;this.oid=oid;}
		@Override
		public void execute() throws SQLException {
			// TODO Stub
			String cmd1;
			cmd1 ="SELECT * ";
			cmd1 +="FROM Orders";
			cmd1 +="WHERE oid="+oid;
			
			switch(dest){
			case Database.DEST_CSTMR:
				CustGUI.Ref().SetOrdersData(Database.stmt.executeQuery(cmd1));
			}
		}
	}
	/**
	 * Adds/removes cart item specified by iid. Constructor takes destination, customer id, item id, and quantity (negative for removal).
	 */
	public static class UpdItemCart implements MartCmd {
		private String cid, iid;
		private int dest, quantity;
		public UpdItemCart(int d, String cid, String iid, int quantity) {this.dest=d; this.cid=cid; this.iid=iid; this.quantity=quantity;}
		@Override
		public void execute() throws SQLException {
			// Assemble command strings. First, get item entry, if any, from order items
			String cmd1;
			cmd1  =  		"SELECT quantity  ";
			cmd1 +=			"FROM OrderItems ";
			cmd1 +=			"WHERE iid = " + iid + " AND oid = (SELECT oid FROM Customers WHERE cid = '" + cid + "')";
			ResultSet rs = Database.stmt.executeQuery(cmd1);
			if(rs.next()) {
				System.out.println("\tCart Item Update - Modifying existing entry");
				int i = rs.getInt(1) + quantity;
				if(i<=0){cmd1 = "DELETE FROM OrderItems WHERE iid = " + iid + " AND oid = (SELECT oid FROM Customers WHERE cid = '" + cid + "')";}
				else {
					cmd1 = 		"UPDATE OrderItems ";
					cmd1+= 		"SET quantity = " + i + " ";
					cmd1+= 		"WHERE iid = " + iid + " AND oid = (SELECT oid FROM Customers WHERE cid = '" + cid + "')";
				}
			} else {
				System.out.println("\tCart Item Update - No entry found, inserting");
				cmd1 = 		"INSERT INTO OrderItems ";
				cmd1+= 		"VALUES ((SELECT oid FROM Customers WHERE cid = '" + cid + "'), " + iid + ", " + quantity + ")";
			}
			String cmd2 =  	"SELECT c.iid, c.category, c.warranty, c.manufacturer, c.model, c.price, o.quantity ";
			cmd2 +=			"FROM OrderItems o, Catalog c ";
			cmd2 +=			"WHERE o.iid = c.iid AND o.oid = (SELECT oid FROM Customers WHERE cid = '" + cid + "')";
			System.out.println("\tCart Item Update - Command = " + cmd1);
			switch(dest){
				case Database.DEST_CSTMR:
					rs = Database.stmt.executeQuery(cmd1);
					CustGUI.Ref().SetCartData(Database.stmt.executeQuery(cmd2));
				default:
			}
		}
	}
	/**
	 * Removes cart item specified by iid. Constructor takes destination, customer id, and item id.
	 */
	public static class RmItemCart implements MartCmd {
		private String cid, iid;
		private int dest;
		public RmItemCart(int d, String cid, String iid) {this.dest=d; this.cid=cid; this.iid=iid;}
		@Override
		public void execute() throws SQLException {
			// Assemble command strings - one for deleting and one for updating the cart
			String cmd1 =  	"DELETE FROM OrderItems  ";
			cmd1 +=			"WHERE iid = " + iid + " AND oid = (SELECT oid FROM Customers WHERE cid = '" + cid + "')";
			String cmd2 =  	"SELECT c.iid, c.category, c.warranty, c.manufacturer, c.model, c.price, o.quantity ";
			cmd2 +=			"FROM OrderItems o, Catalog c ";
			cmd2 +=			"WHERE o.iid = c.iid AND o.oid = (SELECT oid FROM Customers WHERE cid = '" + cid + "')";
			System.out.println("\tCart Item Removal - Command = " + cmd1);
			switch(dest){
				case Database.DEST_CSTMR:
					Database.stmt.executeQuery(cmd1);
					CustGUI.Ref().SetCartData(Database.stmt.executeQuery(cmd2));
				default:
			}
		}
	}
	/**
	 * Removes all items in cid's cart. Constructor takes destination and customer id.
	 */
	public static class RmAllItemCart implements MartCmd {
		private String cid;
		private int dest;
		public RmAllItemCart(int d, String cid) {this.dest=d; this.cid=cid;}
		@Override
		public void execute() throws SQLException {
			// Assemble command strings - one for deleting and one for updating the cart
			String cmd1 =  	"DELETE FROM OrderItems  ";
			cmd1 +=			"WHERE oid = (SELECT oid FROM Customers WHERE cid = '" + cid + "')";
			String cmd2 =  	"SELECT c.iid, c.category, c.warranty, c.manufacturer, c.model, c.price, o.quantity ";
			cmd2 +=			"FROM OrderItems o, Catalog c ";
			cmd2 +=			"WHERE o.iid = c.iid AND o.oid = (SELECT oid FROM Customers WHERE cid = '" + cid + "')";
			System.out.println("\tCart Item Removal - Command = " + cmd1);
			switch(dest){
				case Database.DEST_CSTMR:
					Database.stmt.executeQuery(cmd1);
					CustGUI.Ref().SetCartData(Database.stmt.executeQuery(cmd2));
				default:
			}
		}
	}
	/**
	 * Creates and finalizes a new order from the customer's cart. STUB
	 */
	public static class AddOrder implements MartCmd {
		public AddOrder() {}
		@Override
		public void execute() {
			// TODO Stub
		}
	}
	/**
	 * Creates and finalizes a copy of an order. STUB
	 */
	public static class AddOrderCopy implements MartCmd {
		public AddOrderCopy() {}
		@Override
		public void execute() {
			// TODO Stub
		}
	}
	/**
	 * Searches for username and password for login.
	 */
	public static class QueryLogin implements MartCmd {
		private String user, pass;
		private int dest;
		public QueryLogin(int d, String user, String pass) {this.dest = d; this.user=user; this.pass=pass;}
		@Override
		public void execute() throws SQLException {
			// Assemble command string
			String cmd =  	"SELECT cid ";
			cmd +=			"FROM Customers c ";
			cmd +=			"WHERE c.cid = '" + user + "' AND c.password = '" + pass +"'" ;
			System.out.println("\tLogin Query - Command = " + cmd);
			// Pass to appropriate function
			switch(dest) {
				case Database.DEST_CSTMR:
					CustGUI.Ref().SetLoginResult(Database.stmt.executeQuery(cmd));
			}
		}
	}
	/**
	 * Retrieves customer status, discount, shipping. STUB
	 */
	public static class QueryCustStats implements MartCmd {
		public QueryCustStats() {}
		@Override
		public void execute() {
			// TODO Stub
		}
	}
	// ====================================================================================================
	// End command implementations.
	// ====================================================================================================
}
