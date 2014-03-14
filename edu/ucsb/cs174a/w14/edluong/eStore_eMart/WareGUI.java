package edu.ucsb.cs174a.w14.edluong.eStore_eMart;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;

import net.miginfocom.swing.*;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WareGUI extends JFrame implements Runnable {
	
	private static final long serialVersionUID = 1L;
	
	private static WareGUI ref = null;
	
	private eStore controller;
	
	// Frame elements
	private JFrame frame;
	private JTable tableStock;
	private JTable tableRestock;
	private JTable tableShip;
	
	// Members that can be operated on by other threads
	private volatile StockTable tableStockData;
	private volatile RestockTable tableRestockData;
	private volatile ShipTable tableShipData;
	private volatile DefaultListModel<String> shipmentOverview;
	
	/**
	 * Singleton class reference accessor/constructor
	 */
	public static WareGUI Ref() {
		if(ref==null) ref = new WareGUI();
		return ref;
	}
	private WareGUI() {
		initialize();
	}
	
	/**
	 * Push a result set to the stock table
	 */
	public void SetStockData(ResultSet rs) throws SQLException {
		tableStockData.setContents(rs);
		tableStock.repaint();
	}
	/**
	 * Push a result set to the requests table
	 */
	public void SetRestockData(ResultSet rs) throws SQLException {
		tableRestockData.setContents(rs);
		tableRestock.repaint();
	}
	/**
	 * Push a result set to the shipments table
	 */
	public void SetShipData(ResultSet rs) throws SQLException {
		tableShipData.setContents(rs);
		tableShip.repaint();
	}
	/**
	 * Push shipment overview to ship summary list
	 */
	public void SetShipOverview(ResultSet rs) throws SQLException {
		shipmentOverview.clear();
		int i = tableShip.getSelectedRow();
		if(i!=-1) {
			int j=0;
			if(rs.next()) {
				do{
					System.out.println(j++);
					System.out.println(rs.getString(2)+"x "+rs.getString(1)+": $"+rs.getString(3));
					shipmentOverview.addElement(rs.getString(2)+"x "+rs.getString(1)+": $"+rs.getString(3));
				} while(rs.next());
			}
			shipmentOverview.addElement(" ");
			shipmentOverview.addElement("---");
			String s=(String)tableShipData.getValueAt(i, 2);
			shipmentOverview.addElement("TOTAL: $"+s);
		}
	}
	
	/**
	 * Initialize the contents of the frame. Created using WindowBuilder.
	 */
	private void initialize() {
		// Setup for core frame
		frame = new JFrame();
		frame.setResizable(false);
		frame.setBounds(100, 100, 1000, 700);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.X_AXIS));
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		frame.getContentPane().add(tabbedPane);
		
		// ====================================================================================================
		// Catalog Tab
		// ====================================================================================================
		// Core tab panel
		JPanel stock_tab = new JPanel();
		tabbedPane.addTab("Stock", null, stock_tab, null);
		stock_tab.setLayout(new BorderLayout(0, 0));
		
		// Table for displaying items
		JPanel stock_table = new JPanel();
		String[] columnNamesStock = {"IID", "LOCATION", "QUANTITY", "MIN", "MAX", "REPLENTISH"};
		tableStockData = new StockTable(columnNamesStock, 0);
		tableStock = new JTable(tableStockData);
		tableStock.setRowSelectionAllowed(true);
		tableStock.setFillsViewportHeight(true); 
		JScrollPane spStock = new JScrollPane(tableStock);
		tableStock.setPreferredScrollableViewportSize(new Dimension(400, 300));
		stock_table.add(tableStock.getTableHeader(), BorderLayout.PAGE_START);
		stock_table.setLayout(new BorderLayout(0, 0));
		stock_table.add(spStock);
		stock_tab.add(stock_table, BorderLayout.CENTER);
		
		// Buttons along bottom: find item; order restock; refresh table
		JPanel stock_controls = new JPanel();
		stock_controls.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		stock_tab.add(stock_controls, BorderLayout.SOUTH);
		stock_controls.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
		JLabel lblID = new JLabel("IID:");
		stock_controls.add(lblID);
		final JTextField txtID = new JTextField();
		txtID.setText("");
		stock_controls.add(txtID);
		txtID.setColumns(10);
		JButton btnFindItem = new JButton("Find Item");									//<--FIND ITEM
		btnFindItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Get ID from text box
				String iid = txtID.getText(); txtID.setText("");
				System.out.println("Warehouse GUI - Find item clicked: iid = " + iid);
				// Push command to locate item
				controller.inputCommand(new eStore.QueryStockID(Database.DEST_WAREH, iid));
			}
		});
		JButton btnOrderItem = new JButton("Order Restock");						//<--ORDER RESTOCK
		btnOrderItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Grab iid from currently selected row in catalog table
				int selected = tableStock.getSelectedRow();
				if(selected == -1) { // If no selection, make an error dialog
					JOptionPane.showMessageDialog(frame,
						    "No item selected.",
						    "",
						    JOptionPane.WARNING_MESSAGE);
					return;
				}
				String iid = (String)tableStockData.getValueAt(selected, 0);
				System.out.println("Warehouse GUI - Order Shipment clicked: iid = "+iid);
				// Push command to create restock order
				controller.inputCommand(new eStore.AddRestockReq(Database.DEST_WAREH, iid));
				controller.inputCommand(new eStore.QueryRestock(Database.DEST_WAREH));
			}
		});
		stock_controls.add(btnFindItem);
		JButton btnRefresh = new JButton("Refresh");									//<--REFRESH PAGE CONTENTS
		btnRefresh.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("Warehouse GUI - Refresh Stock clicked");
				controller.inputCommand(new eStore.QueryStock(Database.DEST_WAREH));
			}
		});
		stock_controls.add(btnRefresh);
		
		// ====================================================================================================
		// Requested Shipments Tab
		// ====================================================================================================
		// Core tab panel
		JPanel request_tab = new JPanel();
		tabbedPane.addTab("Shipment Requests", null, request_tab, null);
		request_tab.setLayout(new BorderLayout(0, 0));
		
		// Table for displaying requests
		JPanel req_table = new JPanel();
		String[] columnNamesReq= {"IID", "LOCATION", "QUANTITY"};
		tableRestockData = new RestockTable(columnNamesReq, 0);
		tableRestock = new JTable(tableRestockData);
		tableRestock.setRowSelectionAllowed(true);
		tableRestock.setFillsViewportHeight(true); 
		JScrollPane spReq = new JScrollPane(tableRestock);
		tableRestock.setPreferredScrollableViewportSize(new Dimension(400, 300));
		req_table.add(tableRestock.getTableHeader(), BorderLayout.PAGE_START);
		req_table.setLayout(new BorderLayout(0, 0));
		req_table.add(spReq);
		request_tab.add(req_table, BorderLayout.CENTER);
		
		// Buttons along bottom: generate order
		JPanel request_controls = new JPanel();
		request_tab.add(request_controls, BorderLayout.SOUTH);
		request_controls.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
		JButton btnOrder = new JButton("Generate Order");								//<--GENERATE ORDER (DELETE REQUEST)
		btnOrder.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Grab iid, location from currently selected row in catalog table
				int selected = tableRestock.getSelectedRow();
				if(selected == -1) { // If no selection, make an error dialog
					JOptionPane.showMessageDialog(frame,
						    "No item selected.",
						    "",
						    JOptionPane.WARNING_MESSAGE);
					return;
				}
				String iid = (String)tableRestockData.getValueAt(selected, 0);
				String loc = (String)tableRestockData.getValueAt(selected, 1);
				System.out.println("Warehouse GUI - Delete Request clicked: iid = "+iid+", loc = "+loc);
				controller.inputCommand(new eStore.RmRestockReq(Database.DEST_WAREH, iid, loc));
				controller.inputCommand(new eStore.QueryRestock(Database.DEST_WAREH));
			}
		});
		request_controls.add(btnOrder);
		JButton btnRefreshReq = new JButton("Refresh");									//<--REFRESH PAGE CONTENTS
		btnRefreshReq.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("Warehouse GUI - Refresh Requests clicked");
				controller.inputCommand(new eStore.QueryStock(Database.DEST_WAREH));
			}
		});
		request_controls.add(btnRefreshReq);
		
		// ====================================================================================================
		// Shipments Tab
		// ====================================================================================================
		// Core tab panel
		JPanel ship_tab = new JPanel();
		tabbedPane.addTab("Shipments", null, ship_tab, null);
		ship_tab.setLayout(new BorderLayout(0, 0));
		
		//Table for displaying shipments
		JPanel ship_table = new JPanel();
		String[] columnNamesShip= {"SID", "COMPANY", "LOCATION"};
		tableShipData = new ShipTable(columnNamesShip, 0);
		tableShip = new JTable(tableShipData);
		tableShip.setRowSelectionAllowed(true);
		tableShip.setFillsViewportHeight(true); 
		JScrollPane spship = new JScrollPane(tableShip);
		tableShip.setPreferredScrollableViewportSize(new Dimension(400, 300));
		ship_table.add(tableShip.getTableHeader(), BorderLayout.PAGE_START);
		ship_table.setLayout(new BorderLayout(0, 0));
		ship_table.add(spship);
		ship_tab.add(ship_table, BorderLayout.CENTER);
		
		// Buttons along bottom: add/receive shipment; refresh
		JPanel ship_controls = new JPanel();
		ship_tab.add(ship_controls, BorderLayout.SOUTH);				
		ship_controls.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
		JLabel lblPrice = new JLabel("File:");
		ship_controls.add(lblPrice);
		final JTextField txtPrice = new JTextField();
		txtPrice.setText("");
		ship_controls.add(txtPrice);
		txtPrice.setColumns(10);
		JButton btnAddShip = new JButton("Add Shipment");									//<--ADD SHIPMENT
		btnAddShip.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String text = txtPrice.getText();
				txtPrice.setText("");
				System.out.println("Warehouse GUI - Add shipment clicked: file = " + text);
				// Push command to change price
				controller.inputCommand(new eStore.AddShipment(Database.DEST_WAREH, text));
				controller.inputCommand(new eStore.QueryShipments(Database.DEST_WAREH));
			}
		});
		ship_controls.add(btnAddShip);							
		JButton btnRecvShip = new JButton("Recieve Shipment");								//<--RECIEVE (DELETE) SHIPMENT
		btnRecvShip.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Grab sid, from currently selected row in table
				int selected = tableShip.getSelectedRow();
				if(selected == -1) { // If no selection, make an error dialog
					JOptionPane.showMessageDialog(frame,
						    "No item selected.",
						    "",
						    JOptionPane.WARNING_MESSAGE);
					return;
				}
				String sid = (String)tableShipData.getValueAt(selected, 0);
				System.out.println("Warehouse GUI - Delete Shipment clicked: sid = "+sid);
				controller.inputCommand(new eStore.RmShipment(Database.DEST_WAREH, sid));
				controller.inputCommand(new eStore.QueryShipments(Database.DEST_WAREH));
			}
		});
		ship_controls.add(btnRecvShip);
		JButton btnRefreshShip = new JButton("Refresh");									//<--REFRESH PAGE CONTENTS
		btnRefreshShip.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("Warehouse GUI - Refresh Shipments clicked");
				controller.inputCommand(new eStore.QueryStock(Database.DEST_WAREH));
			}
		});
		ship_controls.add(btnRefreshShip);
		
		// Shipment sidebar: shipment details
		JPanel ship_review = new JPanel();
		ship_review.setPreferredSize(new Dimension(300, 300));
		ship_tab.add(ship_review, BorderLayout.EAST);
		ship_review.setLayout(new MigLayout("", "[73.00px,grow]", "[15px][399.00,grow][][]"));
		JLabel lblShipReview = new JLabel("Shipment Details:");
		ship_review.add(lblShipReview, "cell 0 0,alignx left,aligny top");
		JPanel shipReview = new JPanel();
		ship_review.add(shipReview, "cell 0 1,grow");
		shipReview.setLayout(new BorderLayout(0, 0));
		shipmentOverview = new DefaultListModel<String>();
		JList<String> ordReviewList = new JList<String>(shipmentOverview);
		shipReview.add(new JScrollPane(ordReviewList));
		tableShip.getSelectionModel().addListSelectionListener(new ListSelectionListener() { //<--ON TABLE SELECTION CHANGED
			public void valueChanged(ListSelectionEvent e) {
				// Get new list selection. If not nothing, ask controller for order details
				if(e.getValueIsAdjusting()) return;
				int selected = tableShip.getSelectedRow();
				String sid = "";
				if(selected != -1 && selected<tableShipData.getDataVector().size()) sid = (String) tableShipData.getValueAt(selected, 0);
				System.out.println("Warehouse GUI - Shipment selection changed: sid = "+sid);
				if(sid != "") controller.inputCommand(new eStore.QueryShipmentItems(Database.DEST_CSTMR,sid));
			}
		});
	}

	@Override
	public void run() {
		controller = eStore.Ref();
		
		// Set frame to be visible and open login dialog
		this.frame.setVisible(true);	
		controller.inputCommand(new eStore.QueryShipments(Database.DEST_WAREH));
		controller.inputCommand(new eStore.QueryRestock(Database.DEST_WAREH));
		controller.inputCommand(new eStore.QueryStock(Database.DEST_WAREH));
	}
	
	// ====================================================================================================
	// Implementations of tables.
	// ====================================================================================================
	/**
	 * Table for displaying catalog items
	 */
	@SuppressWarnings("serial")
	private class StockTable extends DefaultTableModel {
		public StockTable(Object[] obj, int i){super(obj, i);}
		public void setContents(ResultSet rs) throws SQLException{
			this.getDataVector().clear();
			if(rs.next()) {
	    		int col;
	    		do{
	    			Object[] obj = new Object[6];
	    			for(col=0; col<6; col++) 
	    				obj[col] =rs.getString(col+1);
	    			this.addRow(obj);
	    			
	    		} while(rs.next());
			}
		}
	}
	/**
	 * Table for displaying restock requests
	 */
	@SuppressWarnings("serial")
	private class RestockTable extends DefaultTableModel {
		public RestockTable(Object[] obj, int i){super(obj, i);}
		public void setContents(ResultSet rs) throws SQLException{
			this.getDataVector().clear();
			if(rs.next()) {
	    		int col;
	    		do{
	    			Object[] obj = new Object[3];
	    			for(col=0; col<3; col++) 
	    				obj[col] =rs.getString(col+1);
	    			this.addRow(obj);
	    			
	    		} while(rs.next());
			}
		}
	}
	/**
	 * Table for displaying shipments
	 */
	@SuppressWarnings("serial")
	private class ShipTable extends DefaultTableModel {
		public ShipTable(Object[] obj, int i){super(obj, i);}
		public void setContents(ResultSet rs) throws SQLException{
			this.getDataVector().clear();
			if(rs.next()) {
	    		int col;
	    		do{
	    			Object[] obj = new Object[3];
	    			for(col=0; col<3; col++) 
	    				obj[col] =rs.getString(col+1);
	    			this.addRow(obj);
	    			
	    		} while(rs.next());
			}
		}
	}
}
