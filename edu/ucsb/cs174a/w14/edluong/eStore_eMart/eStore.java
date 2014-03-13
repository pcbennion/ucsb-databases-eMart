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
			String cmd =  	"SELECT s.location, s.quantity, s.max, s.replentishment ";
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
				cmd = "SELECT * FROM Requests WHERE iid="+iid+" AND location="+location;
				rs2=Database.stmt.executeQuery(cmd);
				if(qty<max && !rs2.next()) {
					cmd =			"INSERT INTO Requests ";
					cmd+=			"VALUES("+iid+", "+location+", "+(max-(qty+rep))+")";
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
			cmd +=			"WHERE iid = "+iid+" AND location = "+loc;
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
				String cmd, sid, com, loc, iid, qty;
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
					    qty=tok[0]; iid=tok[1];
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
}