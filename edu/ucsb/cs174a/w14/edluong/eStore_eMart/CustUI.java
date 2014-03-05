package edu.ucsb.cs174a.w14.edluong.eStore_eMart;

import java.sql.*;

public class CustUI implements Runnable{
	
	private ResultSet input;
	
	public eMart mart;

	/**
	 * Constructor
	 */
	public CustUI() {
		
	}
	
	public boolean inputResult(ResultSet r) {
		if(input!=null) return false;
		input = r;
		return true;
	}

	/**
	 * Begins the thread. Thread will create input events, listen for data to display
	 */
	@Override
	public void run() {
		System.out.println("Customer Interface - Hello World!");
		System.out.println("Customer Interface - Sending Input to Mart.");
		mart.inputCommand(new eMart.QueryCatalog());
		try {
			System.out.println("Customer Interface - Listening for Input.");
			while (!Thread.currentThread().isInterrupted()) {
	            try {
	            	Thread.sleep(50);
	            	if(input != null) {
	            		System.out.println("Customer Interface - Input Event!");
	            		if(input.next()) {
		            		ResultSetMetaData rsmd = input.getMetaData();
		            		assert(rsmd.getTableName(1)=="Catalog");
		            		int numCol = rsmd.getColumnCount();
		            		int i;
		            		for(i=0; i<=numCol; i++)
		            			System.out.print(rsmd.getColumnName(i) + "\t\t");
		            		do{
		            			for(i=0; i<numCol; i++)
		            				System.out.print(input.getString(i) + "\t\t");
		            		} while(input.next());
		            		rsmd=null;
	            		}
	            		input = null;
	            		break;
	            	}
	            }catch(InterruptedException ex) {break;}
			}
			System.out.println("Customer Interface - Listener Closed");
			Database.stmt.close();
		} catch (SQLException e) {e.printStackTrace();}	
	}
}
