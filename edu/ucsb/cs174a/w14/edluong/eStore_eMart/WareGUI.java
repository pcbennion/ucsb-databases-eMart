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
			while(rs.next()) shipmentOverview.addElement(rs.getString(1)+"x "+rs.getString(2)+": $"+rs.getString(3));
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
		// Requested Shipments Tab
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
		request_controls.add(btnRefreshShip);
		
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
				int selected = e.getFirstIndex();
				String sid = "";
				if(selected != -1) sid = (String) tableShipData.getValueAt(selected, 0);
				System.out.println("Warehouse GUI - Shipment selection changed: sid = "+sid);
				if(sid != null) controller.inputCommand(new eStore.QueryShipmentItems(Database.DEST_CSTMR,sid));
			}
		});
		
		
		/*
		// ====================================================================================================
		// Login Dialogue Box.
		// ====================================================================================================
		JPanel loginPane = new JPanel();
		loginPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		loginPane.setLayout(new BorderLayout(0, 0));
		// Username and Password fields
		JPanel loginPanelMain = new JPanel();
		loginPane.add(loginPanelMain, BorderLayout.CENTER);
		loginPanelMain.setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("max(44dlu;default)"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("max(124dlu;default):grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,}));
		JLabel loginUserLbl = new JLabel("Username:");
		loginPanelMain.add(loginUserLbl, "2, 2, right, default");
		final JTextField loginUserTxt = new JTextField();
		loginPanelMain.add(loginUserTxt, "4, 2, fill, default");
		loginUserTxt.setColumns(10);
		JLabel loginPassLbl = new JLabel("Password:");
		loginPanelMain.add(loginPassLbl, "2, 6, right, default");
		final JTextField loginPassTxt = new JTextField();
		loginPanelMain.add(loginPassTxt, "4, 6, fill, default");
		// Login button
		JPanel loginPanelBtn = new JPanel();
		loginPane.add(loginPanelBtn, BorderLayout.SOUTH);
		JButton loginBtn = new JButton("Login");
		loginBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {			//<-- LOGIN BUTTON
				// Grab username and password
				String user = loginUserTxt.getText();
				String pass = loginPassTxt.getText();
				loginUserTxt.setText("");
				loginPassTxt.setText("");
				System.out.println("CustGUI Login - Login clicked: user = "+user+", pass = "+pass);
				login.setEnabled(false);
				// Pass to controller
			}
		});
		loginPanelBtn.add(loginBtn);
		login = new JDialog(frame, "Login:", true);
		login.setContentPane(loginPane);
		login.setResizable(false);
		login.setBounds(300, 300, 450, 150);
		login.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		login.pack();
			
		// ====================================================================================================
		// Search term Popup.
		// ====================================================================================================
		search = new JDialog(frame, "Create Search Term:", true);
		search.setResizable(false);
		search.setBounds(300, 300, 450, 150);
		search.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		//search.pack();
		JPanel popupPane = new JPanel();
		popupPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(popupPane);
		popupPane.setLayout(new BorderLayout(0, 0));
		// OK and cancel buttons
		JPanel popupControls = new JPanel();
		popupPane.add(popupControls, BorderLayout.SOUTH);
		popupControls.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		final JButton btnOk = new JButton("OK");
		popupControls.add(btnOk);
		JButton btnCancel = new JButton("Cancel");
		popupControls.add(btnCancel);
		// Central panel
		final JPanel popupCenter = new JPanel();
		popupPane.add(popupCenter, BorderLayout.CENTER);
		popupCenter.setLayout(new CardLayout(0, 0));
			// Attribute search card
			JPanel searchByAttr = new JPanel();
			popupCenter.add(searchByAttr, "Search by Attribute");
			searchByAttr.setLayout(null);
			final JTextField attrText = new JTextField();
			attrText.setBounds(244, 12, 182, 19);
			searchByAttr.add(attrText);
			attrText.setColumns(10);
			String attrSelectItems[] = {"Item ID", "Category", "Warranty", "Manufacturer", "Model", "Price"};
			final JComboBox<String> attrSelect = new JComboBox<String>(attrSelectItems);
			attrSelect.setBounds(12, 12, 173, 19);
			searchByAttr.add(attrSelect);
			// Description search card
			JPanel searchByDesc = new JPanel();
			popupCenter.add(searchByDesc, "Search by Description");
			searchByDesc.setLayout(null);
			final JTextField descText1 = new JTextField();
			descText1.setBounds(12, 12, 173, 19);
			searchByDesc.add(descText1);
			descText1.setColumns(10);
			final JTextField descText2 = new JTextField();
			descText2.setBounds(244, 12, 182, 19);
			searchByDesc.add(descText2);
			descText2.setColumns(10);
			JLabel label = new JLabel("=");
			label.setBounds(203, 14, 23, 17);
			searchByDesc.add(label);
			// Accessory search cards
			JPanel searchByAccGood = new JPanel();
			popupCenter.add(searchByAccGood, "AccCard1");
			JLabel lblSearchForAccessories = new JLabel("Search for accessories of selected item.");
			searchByAccGood.add(lblSearchForAccessories);
			JPanel searchByAccBad = new JPanel();
			popupCenter.add(searchByAccBad, "AccCard2");
			searchByAccBad.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
			JLabel lblNoItemSelected = new JLabel("No item selected. Please select an item on the catalog table.");
			searchByAccBad.add(lblNoItemSelected);
		// Selection combo box
		JPanel popupSelect = new JPanel();
		popupPane.add(popupSelect, BorderLayout.NORTH);
		popupSelect.setLayout(new BorderLayout(0, 0));
		String searchSelectItems[] = {"Search by Attribute", "Search by Description", "Search for Accessories"};
		final JComboBox<String> searchSelect = new JComboBox<String>(searchSelectItems);
		searchSelect.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				CardLayout c = (CardLayout)popupCenter.getLayout();
				String s = (String)e.getItem();
				btnOk.setEnabled(true);
				if(s == "Search for Accessories") {
					if(tableCata.getSelectedRow()!=-1) c.show(popupCenter, "AccCard1");
					else  {c.show(popupCenter, "AccCard2"); btnOk.setEnabled(false);}
				} else c.show(popupCenter, s);
			}
		});
		popupSelect.add(searchSelect, BorderLayout.EAST);
		// Action listener for OK and cancel controls
		btnOk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String s = (String)searchSelect.getSelectedItem();
				System.out.println("CustGUI Search Popup - OK Clicked: card = "+s);
				String s1, s2;
				String cmd;
				// Switch on the card that is currently visible.
				switch (s) {
						case "Search for Accessories":
							// get IID from catalog table's current selection, assemble command string
							int selected = tableCata.getSelectedRow();
							if(selected!=-1) {
								String iid = (String)tableCataData.getValueAt(selected, 0);
								cmd = "A.iid1 = '"+iid+"'";
								searchTermList.addElement(cmd);
								pushSearch();
							}
							search.setVisible(false);
							searchSelect.setSelectedIndex(0);
							break;
						case "Search by Attribute":
							// Get attribute and value from appropriate text boxes
							s1 = (String)attrSelect.getSelectedItem();
							s2 = attrText.getText(); attrText.setText("");
							if(!s2.isEmpty()) {
								if(s1 == "Item ID")  s1 = "iid";
								cmd = "C."+s1+" = '"+s2+"'";
								searchTermList.addElement(cmd);
								pushSearch();
							}
							search.setVisible(false);
							break;
						case "Search by Description":
							s1 = descText1.getText(); descText1.setText("");
							s2 = descText2.getText(); descText2.setText("");
							if(!s1.isEmpty()&&!s2.isEmpty()) {
								cmd = "D.Attribute = '"+s1+"'";
								searchTermList.addElement(cmd);
								cmd = "D.Value = '"+s2+"'";
								searchTermList.addElement(cmd);
								pushSearch();
							}
							search.setVisible(false);
							searchSelect.setSelectedIndex(0);
							break;
					default:
						return;
				}
			}
		});
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				search.setVisible(false);
				searchSelect.setSelectedIndex(0);
				attrText.setText(""); descText1.setText(""); descText2.setText("");
			}
		});
		search.setContentPane(popupPane);*/
		// ====================================================================================================
	}

	@Override
	public void run() {
		//controller = eMart.Ref();
		
		// Set frame to be visible and open login dialog
		this.frame.setVisible(true);	
		//login.setVisible(true);
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
