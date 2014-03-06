package edu.ucsb.cs174a.w14.edluong.eStore_eMart;

import java.awt.EventQueue;
import java.sql.*;
import java.util.*;

public class Database {
	
	public static Connection conn;
	public static Statement stmt;
	private static String username, password;
	private static String strConn = "jdbc:oracle:thin:@uml.cs.ucsb.edu:1521:xe";
	
	public static final List<String> CatalogCol 
		= Arrays.asList("iid", "category", "warranty", "price", "manufacturer", "model");
	
	/**
	 * Main function
	 */
	public static void main(String[] args) {
		
		// Handle args
		if(args.length!=2) {
			System.out.println("Invalid number of arguments.");
		}
		username = args[0];
		password = args[1];
		
		// Initialize interfaces and controllers
		CustUI ui = new CustUI();
		eMart mart = new eMart();
		ui.mart = mart;
		mart.ui = ui;
		
		// Initialize the connection
		try {
		openConnection();
		} catch (SQLException e) {e.printStackTrace();}
		
		// Run threads
		Thread t1 = new Thread(mart);
		t1.start(); 
		
		EventQueue.invokeLater(ui);
	}
	
	/**
	 * Opens a connection to the sql database
	 */
	public static void openConnection() throws SQLException {
		try {
			DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
		}catch ( Exception e){e.printStackTrace(); }
		conn = DriverManager.getConnection(strConn,username,password);
		stmt = conn.createStatement();
	}

}
