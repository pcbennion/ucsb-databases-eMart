package edu.ucsb.cs174a.w14.edluong.eStore_eMart;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;

import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;

import net.miginfocom.swing.*;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CustGUI extends JFrame implements Runnable {
	
	private static final long serialVersionUID = 1L;
	
	private static CustGUI ref = null;
	
	private eMart controller;
	
	// Frame elements
	private JFrame frame;
	private JDialog login;
	private JDialog search;
	private JTable tableCata;
	private JTable tableCart;
	private JTable tableOrder;
	
	// Members that can be operated on by other threads
	private volatile String CID = null;
	private volatile String CStatus = null;
	private volatile float CDisc = 0;
	private volatile float SChrg = 0;
	private volatile CatalogTable tableCataData;
	private volatile CartTable tableCartData;
	private volatile OrderTable tableOrderData;
	private volatile DefaultListModel<String> cartOverview;
	private volatile DefaultListModel<String> orderOverview;
	
	// Non-volatile data containers
	private DefaultListModel<String> searchTermList;
	
	/**
	 * Singleton class reference accessor/constructor
	 */
	public static CustGUI Ref() {
		if(ref==null) ref = new CustGUI();
		return ref;
	}
	private CustGUI() {
		initialize();
	}
	
	/**
	 * Push a result set to the catalog table
	 */
	public void SetCatalogData(ResultSet rs) throws SQLException {
		tableCataData.setContents(rs);
		tableCata.repaint();
	}
	/**
	 * Push a result set to the cart table
	 */
	public void SetCartData(ResultSet rs) throws SQLException {
		tableCartData.setContents(rs);
		tableCart.repaint();
	}
	/**
	 * Push a result set to the orders table
	 */
	public void SetOrdersData(ResultSet rs) throws SQLException {
		tableOrderData.setContents(rs);
		tableOrder.repaint();
	}
	/**
	 * Interpret login return
	 */
	public void SetLoginResult(ResultSet rs) throws SQLException {
		// If the login returns a valid result, close login pane.
		if(rs.next()) {
    		CID = rs.getString(1);
    		System.out.println("CustGUI Login - CID recieved: "+CID);
    		login.setVisible(false);
    		// Initialize info on all panels
    		controller.inputCommand(new eMart.QueryCatalog(Database.DEST_CSTMR));
    		controller.inputCommand(new eMart.QueryCustStats(Database.DEST_CSTMR, CID));
    		controller.inputCommand(new eMart.QueryCartItems(Database.DEST_CSTMR, CID));
    		controller.inputCommand(new eMart.QueryCustOrders(Database.DEST_CSTMR, CID));
		} else login.setEnabled(true);
	}
	/**
	 * Push order overview to order summary list
	 */
	public void SetOrdersOverview(ResultSet rs) throws SQLException {
		orderOverview.clear();
		int i = tableOrder.getSelectedRow();
		if(i!=-1) {
			int j=0;
			if(rs.next()) {
				do{
					System.out.println(j++);
					System.out.println(rs.getString(2)+"x "+rs.getString(1)+": $"+rs.getString(3));
					orderOverview.addElement(rs.getString(2)+"x "+rs.getString(1)+": $"+rs.getString(3));
				} while(rs.next());
			}
			orderOverview.addElement(" ");
			orderOverview.addElement("---");
			String s=(String)tableOrderData.getValueAt(i, 2);
			orderOverview.addElement("TOTAL: $"+s);
		}
	}
	/**
	 * Updates stored customer and shipping information
	 */
	public void SetCustInfo(ResultSet rs) throws SQLException {
		if(rs.next()) {
			this.CStatus=rs.getString(1);
			this.CDisc	=rs.getFloat(2);
			this.SChrg	=rs.getFloat(3);
			System.out.println(CStatus + " " + CDisc+ " "+SChrg);
		}
	}
	
	/**
	 * Assembles a string of SQL search parameters from the list of search terms and pushes it to the controller 
	 */
	private void pushSearch() {
		String s = "";
		for (int i=0; i<searchTermList.getSize(); i++) {
			if(i!=0) s += " AND ";
			s += searchTermList.get(i);
		}
		if(!s.isEmpty()) controller.inputCommand(new eMart.QueryCatalogSearch(Database.DEST_CSTMR, s));
		else controller.inputCommand(new eMart.QueryCatalog(Database.DEST_CSTMR));
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
		JPanel catalog_tab = new JPanel();
		tabbedPane.addTab("Catalog", null, catalog_tab, null);
		catalog_tab.setLayout(new BorderLayout(0, 0));
		
		// Table for displaying items
		JPanel catalog_table = new JPanel();
		String[] columnNamesCata = {"IID", "CATEGORY", "MANUFACTURER", "MODEL #", "WARRANTY", "PRICE"};
		tableCataData = new CatalogTable(columnNamesCata, 0);
		catalog_table.setLayout(new BorderLayout(0, 0));
		tableCata = new JTable(tableCataData);
		tableCata.setRowSelectionAllowed(true);
		tableCata.setFillsViewportHeight(true); 
		JScrollPane spCata = new JScrollPane(tableCata);
		tableCata.setPreferredScrollableViewportSize(new Dimension(400, 300));
		catalog_table.add(tableCata.getTableHeader(), BorderLayout.PAGE_START);
		catalog_table.add(spCata, BorderLayout.CENTER);
		catalog_tab.add(catalog_table, BorderLayout.CENTER);
		
		// Buttons along bottom: add to cart; remove from cart; refresh table
		JPanel catalog_controls = new JPanel();
		catalog_controls.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		catalog_tab.add(catalog_controls, BorderLayout.SOUTH);
		catalog_controls.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
		JLabel lblQty = new JLabel("Qty:");
		catalog_controls.add(lblQty);
		final JTextField txtQty = new JTextField();
		txtQty.setText("");
		catalog_controls.add(txtQty);
		txtQty.setColumns(10);
		JButton btnAddToCart = new JButton("Add to Cart");							//<--ADD TO CART
		btnAddToCart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Get quantity from text box
				String text = txtQty.getText();
				txtQty.setText("");
				int qty;
				try {
					qty = Integer.parseInt(text);
				} catch(NumberFormatException ex) { // If invalid quantity, make an error dialog
					JOptionPane.showMessageDialog(frame,
						    "Please enter a valid quantity.",
						    "",
						    JOptionPane.WARNING_MESSAGE);
					return;
				}
				// Grab iid from currently selected row in catalog table
				int selected = tableCata.getSelectedRow();
				if(selected == -1) { // If no selection, make an error dialog
					JOptionPane.showMessageDialog(frame,
						    "No item selected.",
						    "",
						    JOptionPane.WARNING_MESSAGE);
					return;
				}
				String iid = (String)tableCataData.getValueAt(selected, 0);
				System.out.println("Customer GUI - Add to Cart clicked: iid = " + iid + ", qty = " + qty);
				// Push command to change cart contents
				controller.inputCommand(new eMart.UpdItemCart(Database.DEST_CSTMR, CID, iid, qty));
			}
		});
		catalog_controls.add(btnAddToCart);
		JButton btnRemoveFromCart = new JButton("Remove from Cart");				//<--REMOVE FROM CART
		btnRemoveFromCart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Grab iid from currently selected row in catalog table
				int selected = tableCata.getSelectedRow();
				if(selected == -1) { // If no selection, make an error dialog
					JOptionPane.showMessageDialog(frame,
						    "No item selected.",
						    "",
						    JOptionPane.WARNING_MESSAGE);
					return;
				}
				String iid = (String)tableCataData.getValueAt(selected, 0);
				System.out.println("Customer GUI - Remove From Cart clicked: iid = " + iid);
				// Push command to change cart contents
				controller.inputCommand(new eMart.RmItemCart(Database.DEST_CSTMR, CID, iid));
			}
		});
		catalog_controls.add(btnRemoveFromCart);
		JButton btnRefresh = new JButton("Refresh");								//<--REFRESH PAGE CONTENTS
		btnRefresh.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("Customer GUI - Refresh Cart clicked");
				pushSearch();
			}
		});
		catalog_controls.add(btnRefresh);
		
		// Search sidebar: display of search terms; add a new term; remove all terms
		JPanel catalog_search = new JPanel();
		catalog_tab.add(catalog_search, BorderLayout.EAST);
		catalog_search.setLayout(new MigLayout("", "[73.00px,grow]", "[15px][399.00,grow][][]"));
		JLabel lblSearchTerms = new JLabel("Search Terms:");
		catalog_search.add(lblSearchTerms, "cell 0 0,alignx left,aligny top");
		JPanel searchTerms = new JPanel();
		catalog_search.add(searchTerms, "cell 0 1,grow");
		searchTerms.setLayout(new BorderLayout(0, 0));
		searchTermList = new DefaultListModel<String>();
		final JList<String> searchTermJList = new JList<String>(searchTermList);
		searchTerms.add(new JScrollPane(searchTermJList));
		JButton btnAddTerm = new JButton(" Add Term...");							//<--ADD NEW SEARCH TERM
		btnAddTerm.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("Customer GUI - Add Term clicked");
				search.setVisible(true);
			}
		});
		catalog_search.add(btnAddTerm, "cell 0 2");
		JButton btnClearTerms = new JButton("Clear Terms");							//<--CLEAR ALL SEARCH TERMS
		btnClearTerms.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("Customer GUI - Clear Terms clicked");
				searchTermList.clear();
				//searchTermJList.repaint();
				controller.inputCommand(new eMart.QueryCatalog(Database.DEST_CSTMR));
			}
		});
		catalog_search.add(btnClearTerms, "cell 0 3");
		
		// ====================================================================================================
		// Cart Tab
		// ====================================================================================================
		// Core tab panel
		JPanel cart_tab = new JPanel();
		tabbedPane.addTab("Cart", null, cart_tab, null);
		cart_tab.setLayout(new BorderLayout(0, 0));
		
		// Table for displaying items
		JPanel cart_table = new JPanel();
		String[] columnNamesCart = {"IID", "CATEGORY", "WARRANTY", "MANUFACTURER", "MODEL #", "PRICE", "QUANTITY"};
		tableCartData = new CartTable(columnNamesCart, 0);
		cart_table.setLayout(new BorderLayout(0, 0));
		tableCart = new JTable(tableCartData);
		tableCart.setRowSelectionAllowed(true);
		tableCart.setFillsViewportHeight(true); 
		JScrollPane spCart = new JScrollPane(tableCart);
		tableCart.setPreferredScrollableViewportSize(new Dimension(300, 300));
		cart_table.add(tableCart.getTableHeader(), BorderLayout.PAGE_START);
		cart_table.add(spCart, BorderLayout.CENTER);
		cart_tab.add(cart_table, BorderLayout.CENTER);
		
		// Buttons along bottom: add to cart; remove from cart; refresh table
		JPanel cart_controls = new JPanel();
		cart_controls.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		cart_tab.add(cart_controls, BorderLayout.SOUTH);
		cart_controls.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
		JLabel lblQtycart = new JLabel("Qty:");
		cart_controls.add(lblQtycart);
		final JTextField txtQtyCart = new JTextField();
		txtQtyCart.setText("");
		cart_controls.add(txtQtyCart);
		txtQtyCart.setColumns(10);
		JButton btnUpdCart = new JButton("Update Quantity");						//<--UPDATE CART QUANTITY
		btnUpdCart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Get quantity from text box
				String text = txtQtyCart.getText();
				txtQtyCart.setText("");
				int qty;
				try {
					qty = Integer.parseInt(text);
				} catch(NumberFormatException ex) { // If invalid quantity, make an error dialog
					JOptionPane.showMessageDialog(frame,
						    "Please enter a valid quantity.",
						    "",
						    JOptionPane.WARNING_MESSAGE);
					return;
				}
				// Grab iid from currently selected row in catalog table
				int selected = tableCart.getSelectedRow();
				if(selected == -1) { // If no selection, make an error dialog
					JOptionPane.showMessageDialog(frame,
						    "No item selected.",
						    "",
						    JOptionPane.WARNING_MESSAGE);
					return;
				}
				String iid = (String)tableCartData.getValueAt(selected, 0);
				String current = (String)tableCartData.getValueAt(selected, 6);
				System.out.println("Customer GUI - Update Cart clicked: iid = " + iid + ", qty = " + qty);
				// Push command to change cart contents
				controller.inputCommand(new eMart.UpdItemCart(Database.DEST_CSTMR, CID, iid, qty-Integer.parseInt(current)));
			}
		});
		cart_controls.add(btnUpdCart);
		JButton btnCartRemove = new JButton("Remove from Cart");					//<--REMOVE FROM CART
		btnCartRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Grab iid from currently selected row in catalog table
				int selected = tableCart.getSelectedRow();
				if(selected == -1) { // If no selection, make an error dialog
					JOptionPane.showMessageDialog(frame,
						    "No item selected.",
						    "",
						    JOptionPane.WARNING_MESSAGE);
					return;
				}
				String iid = (String)tableCartData.getValueAt(selected, 0);
				System.out.println("Customer GUI - Remove From Cart clicked: iid = " + iid);
				// Push command to change cart contents
				controller.inputCommand(new eMart.RmItemCart(Database.DEST_CSTMR, CID, iid));
			}
		});
		cart_controls.add(btnCartRemove);
		JButton btnRefreshCart = new JButton("Refresh");							//<--REFRESH PAGE CONTENTS
		btnRefreshCart.addActionListener(new ActionListener() {	
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("Customer GUI - Refresh Catalog clicked");
				controller.inputCommand(new eMart.QueryCustStats(Database.DEST_CSTMR, CID));
				controller.inputCommand(new eMart.QueryCartItems(Database.DEST_CSTMR, CID));
			}
		});
		cart_controls.add(btnRefreshCart);
		
		// Cart sidebar: subtotal, customer discount, shipping, checkout button
		JPanel cart_ckout = new JPanel();
		cart_ckout.setPreferredSize(new Dimension(200, 300));
		cart_tab.add(cart_ckout, BorderLayout.EAST);
		cart_ckout.setLayout(new MigLayout("", "[73.00px,grow]", "[15px][399.00,grow][][]"));
		JLabel lblCartTotal = new JLabel("Cart Overview:");
		cart_ckout.add(lblCartTotal, "cell 0 0,alignx left,aligny top");
		JPanel cartTotal = new JPanel();
		cart_ckout.add(cartTotal, "cell 0 1,grow");
		cartTotal.setLayout(new BorderLayout(0, 0));
		cartOverview = new DefaultListModel<String>();
		JList<String> cartTotalList = new JList<String>(cartOverview);
		cartTotal.add(new JScrollPane(cartTotalList));
		JButton btnCkout = new JButton("Checkout ");								//<--CHECKOUT
		btnCkout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("Customer GUI - Checkout clicked");
				controller.inputCommand(new eMart.AddOrderCart(Database.DEST_EMART, CID));
			}
		});
		cart_ckout.add(btnCkout, "cell 0 2");
		JButton btnClearCart = new JButton("Clear Cart");							//<--CLEAR CART
		btnClearCart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("Customer GUI - Clear cart clicked");
				controller.inputCommand(new eMart.RmAllItemCart(Database.DEST_CSTMR, CID));
			}
		});
		cart_ckout.add(btnClearCart, "cell 0 3");
		
		// ====================================================================================================
		// Orders Tab
		// ====================================================================================================
		// Core tab panel
		JPanel order_tab = new JPanel();
		tabbedPane.addTab("Orders", null, order_tab, null);
		order_tab.setLayout(new BorderLayout(0, 0));
		
		// Table for displaying items
		JPanel order_table = new JPanel();
		String[] columnNamesOrd = {"OID", "CID", "TOTAL"};
		tableOrderData = new OrderTable(columnNamesOrd, 0);
		order_table.setLayout(new BorderLayout(0, 0));
		tableOrder = new JTable(tableOrderData);
		tableOrder.setRowSelectionAllowed(true);
		tableOrder.setFillsViewportHeight(true); 
		JScrollPane spOrder = new JScrollPane(tableOrder);
		tableOrder.setPreferredScrollableViewportSize(new Dimension(200, 300));
		order_table.add(tableOrder.getTableHeader(), BorderLayout.PAGE_START);
		order_table.add(spOrder, BorderLayout.CENTER);
		order_tab.add(order_table, BorderLayout.CENTER);
		
		// Buttons along bottom: add to cart; remove from cart; refresh table
		JPanel order_controls = new JPanel();
		order_controls.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		order_tab.add(order_controls, BorderLayout.SOUTH);
		order_controls.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
		JLabel lblOidcart = new JLabel("Order ID:");
		order_controls.add(lblOidcart);
		final JTextField txtOidCart = new JTextField();
		txtOidCart.setText("");
		order_controls.add(txtOidCart);
		txtOidCart.setColumns(10);
		JButton btnSearchOrd = new JButton("Search Orders");						//<--SEARCH ORDERS
		btnSearchOrd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Get quantity from text box
				String text = txtOidCart.getText();
				txtOidCart.setText("");
				int oid;
				try {
					oid = Integer.parseInt(text);
				} catch(NumberFormatException ex) { // If invalid quantity, make an error dialog
					JOptionPane.showMessageDialog(frame,
						    "Please enter a valid OID.",
						    "",
						    JOptionPane.WARNING_MESSAGE);
					return;
				}
				System.out.println("Customer GUI - Search Orders clicked: oid = " + oid);
				// Push command search orders
				controller.inputCommand(new eMart.QueryCustOrdersOid(Database.DEST_CSTMR, oid, CID) );
			}
		});
		order_controls.add(btnSearchOrd);
		JButton btnRefreshOrd = new JButton("Refresh");								//<--REFRESH PAGE CONTENTS
		btnRefreshOrd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("Customer GUI - Refresh Orders clicked");
				
				controller.inputCommand(new eMart.QueryCustOrders(Database.DEST_CSTMR, CID));
				
				
				
			}
		});
		order_controls.add(btnRefreshOrd);
		
		// Cart sidebar: subtotal, customer discount, shipping, checkout button
		JPanel ord_review = new JPanel();
		ord_review.setPreferredSize(new Dimension(300, 300));
		order_tab.add(ord_review, BorderLayout.EAST);
		ord_review.setLayout(new MigLayout("", "[73.00px,grow]", "[15px][399.00,grow][][]"));
		JLabel lblOrdReview = new JLabel("Order Overview:");
		ord_review.add(lblOrdReview, "cell 0 0,alignx left,aligny top");
		JPanel ordReview = new JPanel();
		ord_review.add(ordReview, "cell 0 1,grow");
		ordReview.setLayout(new BorderLayout(0, 0));
		orderOverview = new DefaultListModel<String>();
		JList<String> ordReviewList = new JList<String>(orderOverview);
		ordReview.add(new JScrollPane(ordReviewList));
		JButton btnReRun = new JButton("Re-Run Order");								//<--RE-RUN ORDER
		btnReRun.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Grab iid from currently selected row in catalog table
				int selected = tableOrder.getSelectedRow();
				if(selected == -1) { // If no selection, make an error dialog
					JOptionPane.showMessageDialog(frame,
						    "No item selected.",
						    "",
						    JOptionPane.WARNING_MESSAGE);
					return;
				}
				String oid = (String)tableOrderData.getValueAt(selected, 0);
				System.out.println("Customer GUI - Re-Run Order clicked: oid = "+oid);
				// Push command to change cart contents
				controller.inputCommand(new eMart.RmItemCart(Database.DEST_CSTMR, CID, oid));
			}
		});
		tableOrder.getSelectionModel().addListSelectionListener(new ListSelectionListener() { //<--ON TABLE SELECTION CHANGED
			public void valueChanged(ListSelectionEvent e) {
				// Get new list selection. If not nothing, ask controller for order details
				if(e.getValueIsAdjusting()) return;
				int selected = tableOrder.getSelectedRow();
				String oid = "";
				if(selected != -1 && selected<tableOrderData.getDataVector().size()) oid = (String) tableOrderData.getValueAt(selected, 0);
				System.out.println("Manager GUI - Order selection changed: oid = "+oid);
				if(oid != "") controller.inputCommand(new eMart.QueryOrderItems(Database.DEST_CSTMR,oid));
			}
		});
		ord_review.add(btnReRun, "cell 0 2");
		
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
				controller.inputCommand(new eMart.QueryLogin(Database.DEST_CSTMR, user, pass));
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
			String attrSelectItems[] = {"Item ID", "Category", "Manufacturer", "Model", "Warranty", "Price"};
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
		search.setContentPane(popupPane);
		// ====================================================================================================
	}

	@Override
	public void run() {
		controller = eMart.Ref();
		
		// Set frame to be visible and open login dialog
		this.frame.setVisible(true);	
		login.setVisible(true);
		//controller.inputCommand(new eMart.QueryLogin(Database.DEST_CSTMR, "Pquirrell", "Pquirrell"));
	}
	
	// ====================================================================================================
	// Implementations of tables.
	// ====================================================================================================
	/**
	 * Table for displaying catalog items
	 */
	@SuppressWarnings("serial")
	private class CatalogTable extends DefaultTableModel {
		public CatalogTable(Object[] obj, int i){super(obj, i);}
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
	 * Table for displaying a customer's cart. Keeps a running tally of item quantity and subtotal
	 */
	@SuppressWarnings("serial")
	private class CartTable extends DefaultTableModel {
		public CartTable(Object[] obj, int i){super(obj, i);}
		public void setContents(ResultSet rs) throws SQLException{
			this.getDataVector().clear();
			int quantity = 0;
			float subtotal = 0;
			if(rs.next()) {
	    		int col;
	    		do{
	    			int i;
	    			Object[] obj = new Object[7];
	    			for(col=0; col<7; col++) 
	    				obj[col] =rs.getString(col+1);
	    			this.addRow(obj);
	    			i = rs.getInt(7);
	    			quantity += i;
	    			subtotal += rs.getFloat(6)*i;
	    		} while(rs.next());
			}
			cartOverview.clear();
			cartOverview.addElement(" # of Items: "+quantity);
			cartOverview.addElement(" ");
			cartOverview.addElement("Cust ID:");
			cartOverview.addElement("\t"+CID);
			cartOverview.addElement(" ");
			cartOverview.addElement("Cust Status:");
			cartOverview.addElement("\t"+CStatus);
			cartOverview.addElement(" ");
			cartOverview.addElement("Subtotal:$"+subtotal);
			cartOverview.addElement("Shipping:  "+SChrg+"%");
			cartOverview.addElement("Discount:  "+CDisc+"%");
			cartOverview.addElement(" ");
			cartOverview.addElement("TOTAL:$"+(subtotal+subtotal*SChrg-subtotal*CDisc));
		}
	}
	/**
	 * Table for displaying order entries
	 */
	@SuppressWarnings("serial")
	private class OrderTable extends DefaultTableModel {
		public OrderTable(Object[] obj, int i){super(obj, i);}
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
// ====================================================================================================