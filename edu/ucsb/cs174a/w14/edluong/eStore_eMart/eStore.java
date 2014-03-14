package edu.ucsb.cs174a.w14.edluong.eStore_eMart;

import java.io.*;
import java.nio.charset.Charset;
import java.sql.*;
import java.util.*;

public class eStore implements Runnable{
	
	private static eStore ref = null;
	
	// Members that can be operated on by other threads
	private volatile Queue<StoreCmd> cmdQueue;

	/**
	 * Singleton class reference accessor/constructor
	 */
	public static eStore Ref() {
		if(ref==null) ref = new eStore();
		return ref;
	}
	private eStore() {
		cmdQueue = new LinkedList<StoreCmd>();
	}
	
	/**
	 * Sends the appropriate callback request to this thread. Will fail if there is already a command waiting to be executed.
	 * @param c: the command to be executed.
	 */
	public void inputCommand(StoreCmd c) {
		cmdQueue.add(c);
	}

	/**
	 * Begins the thread and waits for input. When an input exists, it executes the input's command.
	 */
	@Override
	public void run() {
		System.out.println("eStore Controller - Hello World!");
		try {
			System.out.println("eStore Controller - Listening for Input.");
			while (!Thread.currentThread().isInterrupted()) {
	            try {
	            	Thread.sleep(50);
	            	while(!cmdQueue.isEmpty()) {
	            		System.out.println("eStore Controller - Input Event!");
	            		cmdQueue.remove().execute();
	            	}
	            }catch(InterruptedException ex) {break;}
			}
			System.out.println("eStore Controller - Listener Closed");
		} catch (SQLException e) {e.printStackTrace();}	
	}
	
	// ====================================================================================================
	// The Java version of thread callbacks : Class interfaces!
	//		These represent database commands for eMart.
	//		Instantiate one of these bad boys and pass to this thread; execute will be called as specified.
	//
	// Here is the base interface:
	// ====================================================================================================
	private interface StoreCmd {
		public void execute() throws SQLException;
	}
	// ====================================================================================================
	// Valid eStore command implementations follow :
	// ====================================================================================================
	/**
	 * Gets the quantities of all items. Takes destination
	 */
	public static class QueryItemQuantity implements StoreCmd {
		private int dest;
		public QueryItemQuantity(int d){this.dest=d;}
		@Override
		public void execute() throws SQLException {
			//Assemble command string
			String cmd =  	"SELECT s.iid, SUM(s.quantity) ";
			cmd +=			"FROM Stock s ";
			cmd +=			"GROUP BY s.iid";
			System.out.println("\tStock Quantity Query - Command = " + cmd);
			ResultSet rs = Database.stmt.executeQuery(cmd);
			if(!rs.next()) System.out.println("oops");
			// Push to appropriate destination
			switch(dest) {
				case Database.DEST_MANAG:
					eMart.Ref().inputCommand( new eMart.PushItemQuantity(dest, Database.stmt.executeQuery(cmd)));
				default:
			}
		}
	}
	/**
	 * Gets all items in stock. Takes destination
	 */
	public static class QueryStock implements StoreCmd {
		private int dest;
		public QueryStock(int d){this.dest=d;}
		@Override
		public void execute() throws SQLException {
			//Assemble command string
			String cmd =  	"SELECT * ";
			cmd +=			"FROM Stock s ";
			System.out.println("\tStock Query - Command = " + cmd);
			// Push to appropriate destination
			switch(dest) {
				case Database.DEST_WAREH:
					WareGUI.Ref().SetStockData(Database.stmt.executeQuery(cmd));
				default:
			}
		}
	}
	/**
	 * Gets an item from the stock. Takes destination, iid
	 */
	public static class QueryStockID implements StoreCmd {
		private String iid;
		private int dest;
		public QueryStockID(int d, String iid){this.dest=d; this.iid=iid;}
		@Override
		public void execute() throws SQLException {
			//Assemble command string
			String cmd =  	"SELECT * ";
			cmd +=			"FROM Stock s ";
			cmd +=			"WHERE s.iid = "+iid;
			System.out.println("\tStock Query - Command = " + cmd);
			// Push to appropriate destination
			switch(dest) {
				case Database.DEST_WAREH:
					WareGUI.Ref().SetStockData(Database.stmt.executeQuery(cmd));
				default:
			}
		}
	}
	/**
	 * Gets all restock requests. Takes destination
	 */
	public static class QueryRestock implements StoreCmd {
		private int dest;
		public QueryRestock(int d){this.dest=d;}
		@Override
		public void execute() throws SQLException {
			//Assemble command string
			String cmd =  	"SELECT * ";
			cmd +=			"FROM Requests r ";
			System.out.println("\tRestock Query - Command = " + cmd);
			// Push to appropriate destination
			switch(dest) {
				case Database.DEST_WAREH:
					WareGUI.Ref().SetRestockData(Database.stmt.executeQuery(cmd));
				default:
			}
		}
	}
	/**
	 * Creates a restock request for an item, provided the stock lies below max. Takes destination, iid
	 */
	public static class AddRestockReq implements StoreCmd {
		private String iid;
		@SuppressWarnings("unused")
		private int dest;
		public AddRestockReq(int d, String iid){this.dest=d; this.iid=iid;}
		@Override
		public void execute() throws SQLException {
			//Assemble command string
			String cmd =  	"SELECT s.location, s.quantity, s.max, s.replenishment ";
			cmd +=			"FROM Stock s ";
			cmd +=			"WHERE s.iid = "+iid;
			System.out.println("\tAdd Restock Request - Command = " + cmd);
			ResultSet rs = Database.stmt.executeQuery(cmd);
			int qty = 0, max = 0, rep = 0;
			String location;
			ResultSet rs2;
			while(rs.next()) {
				location = rs.getString(1);
				qty = rs.getInt(2); max=rs.getInt(3); rep = rs.getInt(4);
				cmd = "SELECT * FROM Requests WHERE iid="+iid+" AND location='"+location+"'";
				rs2=Database.stmt.executeQuery(cmd);
				if(qty<max && !rs2.next()) {
					cmd =			"INSERT INTO Requests ";
					cmd+=			"VALUES((SELECT iid FROM Stock WHERE iid="+iid+"), "
							+ "(SELECT location FROM Stock WHERE location='"+location+"'), "+(max-(qty+rep))+")";
					Database.stmt.executeQuery(cmd);
				}
			}
		}
	}
	/**
	 * Removes the a restock entry. Takes destination, iid, location
	 */
	public static class RmRestockReq implements StoreCmd {
		private String iid, loc;
		@SuppressWarnings("unused")
		private int dest;
		public RmRestockReq(int d, String iid, String loc){this.dest=d; this.iid=iid; this.loc=loc;}
		@Override
		public void execute() throws SQLException {
			//Assemble and execute command string
			String cmd =  	"DELETE FROM Requests ";
			cmd +=			"WHERE iid = "+iid+" AND location = '"+loc+"'";
			System.out.println("\tRemove Restock Request - Command = " + cmd);
			Database.stmt.executeQuery(cmd);
		}
	}
	/**
	 * Gets all shipments. Takes destination
	 */
	public static class QueryShipments implements StoreCmd {
		private int dest;
		public QueryShipments(int d){this.dest=d;}
		@Override
		public void execute() throws SQLException {
			//Assemble command string
			String cmd =  	"SELECT * ";
			cmd +=			"FROM Shipments s ";
			System.out.println("\tShipment Query - Command = " + cmd);
			// Push to appropriate destination
			switch(dest) {
				case Database.DEST_WAREH:
					WareGUI.Ref().SetShipData(Database.stmt.executeQuery(cmd));
				default:
			}
		}
	}
	/**
	 * Get items in a shipment. Takes Destination, sid
	 */
	public static class QueryShipmentItems implements StoreCmd {
		private int dest;
		private String sid;
		public QueryShipmentItems(int d, String sid){this.dest=d; this.sid=sid;}
		@Override
		public void execute() throws SQLException {
			//Assemble command string
			String cmd =  	"SELECT * ";
			cmd +=			"FROM ShipmentItems i ";
			cmd +=			"WHERE i.sid = "+sid;
			System.out.println("\tShipment Items Query - Command = " + cmd);
			// Push to appropriate destination
			switch(dest) {
				case Database.DEST_WAREH:
					WareGUI.Ref().SetShipOverview(Database.stmt.executeQuery(cmd));
				default:
			}
		}
	}
	/**
	 * Creates a shipment from a file. Takes destination, filename
	 */
	public static class AddShipment implements StoreCmd {
		private String s;
		@SuppressWarnings("unused")
		private int dest;
		public AddShipment(int d, String s){this.dest=d; this.s=s;}
		@Override
		public void execute() throws SQLException {
			//Parse file, assemble commands, and insert
			try {
				FileInputStream in = new FileInputStream("./"+s);
				BufferedReader br = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
				String line;
				String cmd, sid, com, mod, iid, loc, qty;
				if ((line = br.readLine()) != null) {
				    String[] tok =  line.split(" ");
				    if(tok.length!=3) {br.close();return;}
				    sid = tok[0]; com = tok[1]; loc = tok[2];
				    cmd =  			"SELECT * ";
					cmd +=			"FROM Shipment s ";
					cmd +=			"WHERE s.sid = "+sid;
					ResultSet rs = Database.stmt.executeQuery(cmd);
					if(rs.next()) return;
					cmd = "INSERT INTO Shipments ";
					cmd+= "VALUES("+sid+", "+com+", "+loc+")";
					Database.stmt.executeQuery(cmd);
					while((line = br.readLine()) != null) {
						tok =  line.split(" ");
					    if(tok.length!=2) {br.close();return;}
					    qty=tok[0]; mod=tok[1];
					    cmd = "SELECT iid FROM Catalog WHERE Manufacturer="+com+" AND Model="+mod;
					    rs = Database.stmt.executeQuery(cmd);
					    if(rs.next()) {iid = rs.getString(1);} 
					    else {
					    	cmd = "INSERT INTO Catalog VALUES(null, 'Unassigned', 0, 1000, "+com+", "+mod+") OUTPUT inserted.iid";
					    	rs = Database.stmt.executeQuery(cmd);
					    	rs.next(); iid=rs.getString(1);
					    	cmd = "INSERT INTO Stock VALUES("+iid+", "+loc+", "+qty+", "+(Integer.parseInt(qty)/2)+", "+qty+", 0)";
					    	Database.stmt.executeQuery(cmd);
					    }
					    cmd = "INSERT INTO ShipmentItems VALUES("+sid+", "+iid+", "+qty+")";
					    Database.stmt.executeQuery(cmd);
					}
				}
				br.close();
			} catch (IOException e) {return;}
		}
	}
	/**
	 * Removes the shipment. Takes destination, sid
	 */
	public static class RmShipment implements StoreCmd {
		private String sid;
		@SuppressWarnings("unused")
		private int dest;
		public RmShipment(int d, String sid){this.dest=d; this.sid=sid;}
		@Override
		public void execute() throws SQLException {
			//Assemble and execute command string
			String cmd =  	"DELETE FROM Shipments ";
			cmd +=			"WHERE sid = "+sid;
			System.out.println("\tRemove Shipment - Command = " + cmd);
			Database.stmt.executeQuery(cmd);
		}
	}
	/**
	 * Checks stock quantities to see if order can be fulfilled, then fulfills them. Takes dest, oid, cid
	 */
	public static class FullfillOrder implements StoreCmd {
		private String cid, oid;
		private int dest;
		public FullfillOrder(int d, String cid, String oid){this.dest=d; this.cid=cid; this.oid=oid;}
		@Override
		public void execute() throws SQLException {
			// Check to see if there is enough in stock for the order
			String cmd =  	"select i.quantity, sumlist.sum "
					+ "from orderitems i, (	 SELECT s.iid as id, SUM(s.quantity) as sum "
					+ 						"from orderitems i, stock s "
					+ 						"where i.iid=s.iid and i.oid=4 "
					+ 						"group by s.iid ) sumlist "
					+ "where i.oid=4 and i.iid=sumlist.id";
			ResultSet rs = Database.stmt.executeQuery(cmd);
			int oqty, sqty;
			while(rs.next()) {
				oqty=0; sqty=0;
				oqty=rs.getInt(1);
				sqty=rs.getInt(2);
				if(oqty>sqty) return; // if we're being fancy, tell eMart to push an error message here
			}
			// If we get here, order can be fulfilled. Remove needed quantities of items from the stock
			// Try to remove as much as possible from single locations
			cmd =	"SELECT quantity FROM OrderItems WHERE oid = "+oid;
			rs = Database.stmt.executeQuery(cmd);
			String iid, location;
			while(rs.next()) {
				oqty=0; oqty=rs.getInt(1);
				cmd = "SELECT s.iid, s.location, s.quantity FROM OrderItems i, Stock s WHERE i.iid=s.iid AND i.oid = "+oid+" ";
				ResultSet rs2 = Database.stmt.executeQuery(cmd);
				while(oqty>0 && rs2.next()) { 
					iid=""; iid=rs2.getString(1);
					location=""; location=rs2.getString(2);
					sqty=0; sqty=rs2.getInt(3);
					if(oqty>sqty) {
						cmd = "UPDATE Stock SET quantity = 0 WHERE iid="+iid+" AND location='"+location+"'";
						oqty-=sqty;
					} else {
						cmd = "UPDATE Stock SET quantity = "+(sqty-oqty)+" WHERE iid="+iid+" AND location='"+location+"'";
						oqty=0;
					}
					Database.stmt.executeQuery(cmd);
				}
			}
			// We're done with eStore's part. Pass back to eMart to finalize order
			eMart.Ref().inputCommand(new eMart.AddOrder(dest, cid, oid));
			// Now get current and min stock levels. Add restock requests as needed
			cmd = 	"SELECT s.iid, s.quantity, s.min ";
			cmd+=	"FROM Stock s, OrderItems i ";
			cmd+=	"WHERE i.iid=s.iid AND i.oid = "+oid+"";
			rs = Database.stmt.executeQuery(cmd);
			int min;
			while(rs.next()) {
				iid=""; sqty=0; min=0;
				iid=rs.getString(1); 
				sqty=rs.getInt(2); 
				min=rs.getInt(3);
				if(sqty<min) {
					ref.inputCommand(new eStore.AddRestockReq(Database.DEST_ESTOR, iid));
				}
			}
		}
	}
}