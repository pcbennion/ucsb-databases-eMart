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
import java.util.Vector;

public class MngrGUI extends JFrame implements Runnable {
	
	private static final long serialVersionUID = 1L;
	
	private static MngrGUI ref = null;
	
	private eMart controller;
	
	// Frame elements
	private JFrame frame;
	private JDialog login;
	private JDialog searchCata;
	private JDialog searchOrders;
	private JTable tableCata;
	private JTable tableOrder;
	
	// Members that can be operated on by other threads
	private volatile String CID = null;
	private volatile CatalogTable tableCataData;
	private volatile OrderTable tableOrderData;
	private volatile DefaultListModel<String> orderOverview;
	private volatile DefaultListModel<String> orderResults;
	
	// Non-volatile data containers
	private DefaultListModel<String> searchTermListCata;
	private String ordSearchTerm = "";
	
	int d = Database.DEST_CSTMR;
	
	/**
	 * Singleton class reference accessor/constructor
	 */
	public static MngrGUI Ref() {
		if(ref==null) ref = new MngrGUI();
		return ref;
	}
	private MngrGUI() {
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
	 * Push quantity data to the catalog table.
	 */
	public void SetCatalogQuantityData(ResultSet rs) throws SQLException {
		// Loop through entire result set, putting quantity into slots matched by iid
		while(rs.next()) {
			int i=0;
			@SuppressWarnings("unchecked")
			Vector<Vector<Object>> v = tableOrderData.getDataVector();
			if(v.isEmpty()) return;
			String iid = rs.getString(1);
			String qty = rs.getString(2);
			while(v.get(i).get(0)!=iid) {if(i>=v.size()) break; i++;}
			if(i<v.size()) tableOrderData.setValueAt(qty, i, 5);
		}
		tableCata.repaint();
	}
	
	/**
	 * Push a result set to the orders table
	 */
	public void SetOrdersData(ResultSet rs) throws SQLException {
		tableOrderData.setContents(rs);
		tableOrder.repaint();
	}
	/**
	 * Push order results to set info list
	 */
	public void SetOrdersResults(ResultSet rs) throws SQLException {
		orderResults.clear();
		if(rs.next()) {
			int count = rs.getInt(1);
			int total = rs.getInt(2);
			// modify search info table
			if(ordSearchTerm!="") {
				orderResults.addElement("Filtered by:");
				orderResults.addElement("\t"+ordSearchTerm);
				orderResults.addElement(" ");
			}
			if(ordSearchTerm.contains("O.")||ordSearchTerm=="") orderResults.addElement("Number of Orders:");
			else orderResults.addElement("Number of Units:");
			orderResults.addElement("\t"+Integer.toString(count));
			orderResults.addElement("TOTAL COST:");
			orderResults.addElement("\t"+Integer.toString(total));
		}
	}
	/**
	 * Push order overview to order summary list
	 */
	public void SetOrdersOverview(ResultSet rs) throws SQLException {
		orderOverview.clear();
		int i = tableOrder.getSelectedRow();
		if(i!=-1) {
			while(rs.next()) orderOverview.addElement(rs.getString(1)+"x "+rs.getString(2)+": $"+rs.getString(3));
			orderOverview.addElement(" ");
			orderOverview.addElement("---");
			String s=(String)tableOrderData.getValueAt(i, 2);
			orderOverview.addElement("TOTAL: $"+s);
		}
	}
	/**
	 * Interpret login return
	 */
	public void SetLoginResult(ResultSet rs) throws SQLException {
		// If the login returns a valid result, close login pane.
		if(rs.next()) {
    		CID = rs.getString(1);
    		System.out.println("MngrGUI Login - CID recieved: "+CID+", isAdmin="+rs.getBoolean(2));
    		if(rs.getBoolean(2)){
	    		login.setVisible(false);
	    		// Initialize info on all panels
	    		controller.inputCommand(new eMart.QueryCatalog(Database.DEST_MANAG));
	    		controller.inputCommand(new eMart.QueryOrders(Database.DEST_MANAG));
    		} else login.setEnabled(true);
		} else login.setEnabled(true);
	}
	
	/**
	 * Assembles a string of SQL search parameters from the list of item search terms and pushes it to the controller 
	 */
	private void pushCataSearch() {
		String s = "";
		for (int i=0; i<searchTermListCata.getSize(); i++) {
			if(i!=0) s += " AND ";
			s += searchTermListCata.get(i);
		}
		if(!s.isEmpty()) controller.inputCommand(new eMart.QueryCatalogSearch(Database.DEST_MANAG, s));
		else controller.inputCommand(new eMart.QueryCatalog(Database.DEST_MANAG));
	}
	/**
	 * Sends order search parameter to controller
	 */
	private void pushOrderSearch() {
		if(ordSearchTerm!="") controller.inputCommand(new eMart.QueryOrdersSearch(Database.DEST_MANAG, ordSearchTerm));
		else controller.inputCommand(new eMart.QueryOrders(Database.DEST_MANAG));
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
		String[] columnNamesCata = {"IID", "CATEGORY", "MANUFACTURER", "MODEL #", "PRICE", "STOCK"};
		tableCataData = new CatalogTable(columnNamesCata, 0);
		tableCata = new JTable(tableCataData);
		tableCata.setRowSelectionAllowed(true);
		tableCata.setFillsViewportHeight(true); 
		JScrollPane spCata = new JScrollPane(tableCata);
		tableCata.setPreferredScrollableViewportSize(new Dimension(400, 300));
		catalog_table.add(tableCata.getTableHeader(), BorderLayout.PAGE_START);
		catalog_table.setLayout(new BorderLayout(0, 0));
		catalog_table.add(spCata);
		catalog_tab.add(catalog_table, BorderLayout.CENTER);
		
		// Search sidebar: display of search terms; add a new term; remove all terms
		JPanel catalog_search = new JPanel();
		catalog_tab.add(catalog_search, BorderLayout.EAST);
		catalog_search.setLayout(new MigLayout("", "[73.00px,grow]", "[15px][399.00,grow][][]"));
		JLabel lblSearchTerms = new JLabel("Search Terms:");
		catalog_search.add(lblSearchTerms, "cell 0 0,alignx left,aligny top");
		JPanel searchTerms = new JPanel();
		catalog_search.add(searchTerms, "cell 0 1,grow");
		searchTerms.setLayout(new BorderLayout(0, 0));
		searchTermListCata = new DefaultListModel<String>();
		final JList<String> searchTermJList = new JList<String>(searchTermListCata);
		searchTerms.add(new JScrollPane(searchTermJList));
		JButton btnAddTerm = new JButton(" Add Term...");							//<--ADD NEW SEARCH TERM
		btnAddTerm.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("Manager GUI - Add Term clicked");
				searchCata.setVisible(true);
			}
		});
		catalog_search.add(btnAddTerm, "cell 0 2");
		JButton btnClearTerms = new JButton("Clear Terms");							//<--CLEAR ALL SEARCH TERMS
		btnClearTerms.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("Manager GUI - Clear Terms clicked");
				searchTermListCata.clear();
				controller.inputCommand(new eMart.QueryCatalog(Database.DEST_MANAG));
			}
		});
		catalog_search.add(btnClearTerms, "cell 0 3");
		
		// Buttons along bottom: update price; order restock; refresh table
		JPanel catalog_controls = new JPanel();
		catalog_controls.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		catalog_tab.add(catalog_controls, BorderLayout.SOUTH);
		catalog_controls.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
		JLabel lblPrice = new JLabel("Price:");
		catalog_controls.add(lblPrice);
		final JTextField txtPrice = new JTextField();
		txtPrice.setText("");
		catalog_controls.add(txtPrice);
		txtPrice.setColumns(10);
		JButton btnUpdPrice = new JButton("Set Price");								//<--CHANGE PRICE
		btnUpdPrice.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Get quantity from text box
				String text = txtPrice.getText();
				txtPrice.setText("");
				int price;
				try {
					price = Integer.parseInt(text);
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
				System.out.println("Manager GUI - Set Price clicked: iid = " + iid + ", price = " + price);
				// Push command to change price
				controller.inputCommand(new eMart.UpdCatalogPrice(Database.DEST_EMART, iid, price));
				// Refresh pane
				pushCataSearch();
			}
		});
		catalog_controls.add(btnUpdPrice);
		JButton btnOrderItem = new JButton("Order Restock");						//<--ORDER RESTOCK
		btnOrderItem.addActionListener(new ActionListener() {
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
				System.out.println("Manager GUI - Order Shipment clicked: iid = "+iid);
				// Push command to create restock order
				controller.inputCommand(new eMart.AddRestockOrder(Database.DEST_ESTOR, iid));
			}
		});
		catalog_controls.add(btnOrderItem);
		JButton btnRefresh = new JButton("Refresh");								//<--REFRESH PAGE CONTENTS
		btnRefresh.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("Manager GUI - Refresh Catalog clicked");
				pushCataSearch();
			}
		});
		catalog_controls.add(btnRefresh);
		
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
		tableOrder.setPreferredScrollableViewportSize(new Dimension(400, 300));
		order_table.add(tableOrder.getTableHeader(), BorderLayout.PAGE_START);
		order_table.add(spOrder, BorderLayout.CENTER);
		order_tab.add(order_table, BorderLayout.CENTER);
		
		// Buttons along bottom: delete order; delete all unnecessary orders; refresh table
		JPanel order_controls = new JPanel();
		order_tab.add(order_controls, BorderLayout.SOUTH);
		order_controls.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		order_controls.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		JButton btnDelete = new JButton("Delete Order");								//<--DELETE ORDER
		btnDelete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Grab oid from currently selected row in catalog table
				int selected = tableOrder.getSelectedRow();
				if(selected == -1) { // If no selection, make an error dialog
					JOptionPane.showMessageDialog(frame,
						    "No item selected.",
						    "",
						    JOptionPane.WARNING_MESSAGE);
					return;
				}
				String oid = (String)tableCataData.getValueAt(selected, 0);
				System.out.println("Manager GUI - Delete Order clicked: oid = "+oid);
				// Push command to remove order
				controller.inputCommand(new eMart.RmOrderOid(Database.DEST_EMART, oid));
				// Refresh pane
				pushOrderSearch();
			}
		});
		order_controls.add(btnDelete);
		JButton btnDeleteAll = new JButton("Delete All Unnecessary");					//<--DELETE ALL UNNECESSARY
		btnDeleteAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("Manager GUI - Delete All clicked");
				controller.inputCommand(new eMart.RmOrders(Database.DEST_EMART));
				// Refresh pane
				pushOrderSearch();
			}
		});
		order_controls.add(btnDeleteAll);
		JButton btnRefreshOrders = new JButton("Refresh");								//<--REFRESH TABLE
		btnRefreshOrders.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("Manager GUI - Refresh Orders clicked");
				pushOrderSearch();
			}
		});
		order_controls.add(btnRefreshOrders);
		
		// Orders sidebar 1: overview of selected order
		JPanel ord_review = new JPanel();
		ord_review.setPreferredSize(new Dimension(250, 300));
		order_tab.add(ord_review, BorderLayout.WEST);
		ord_review.setLayout(new MigLayout("", "[73.00px,grow]", "[15px][399.00,grow]"));
		JLabel lblOrdReview = new JLabel("Order Overview:");
		ord_review.add(lblOrdReview, "cell 0 0,alignx left,aligny top");
		JPanel ordReview = new JPanel();
		ord_review.add(ordReview, "cell 0 1,grow");
		ordReview.setLayout(new BorderLayout(0, 0));
		orderOverview = new DefaultListModel<String>();
		JList<String> ordReviewList = new JList<String>(orderOverview);
		ordReview.add(new JScrollPane(ordReviewList));
		tableOrder.getSelectionModel().addListSelectionListener(new ListSelectionListener() { //<--ON TABLE SELECTION CHANGED
			public void valueChanged(ListSelectionEvent e) {
				// Get new list selection. If not nothing, ask controller for order details
				int selected = e.getFirstIndex();
				String oid = "";
				if(selected != -1) oid = (String) tableOrderData.getValueAt(selected, 0);
				System.out.println("Manager GUI - Order selection changed: oid = "+oid);
				if(oid != null) controller.inputCommand(new eMart.QueryOrderItems(Database.DEST_MANAG,oid));
			}
		});
		
		// Orders sidebar 2: search fucntions and summary
		JPanel order_search = new JPanel();
		order_search.setPreferredSize(new Dimension(250, 300));
		order_tab.add(order_search, BorderLayout.EAST);
		order_search.setLayout(new MigLayout("", "[117px,grow]", "[][25px][grow][][][][][][][][][][][][][][][][][][][]"));
		JLabel lblSearchResults = new JLabel("Selection Summary:");
		order_search.add(lblSearchResults, "cell 0 0");
		JPanel ordResults = new JPanel();
		order_search.add(ordResults, "cell 0 1 1 19,grow");
		ordResults.setLayout(new BorderLayout(0, 0));
		orderResults = new DefaultListModel<String>();
		JList<String> ordResultsList = new JList<String>(orderResults);
		ordResults.add(ordResultsList);
		JButton btnOrdNewTerm = new JButton(" New Filter");								//<--NEW SEARCH FILTER
		btnOrdNewTerm.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("Manager GUI - New Order Filter clicked");
				searchOrders.setVisible(true);
			}
		});
		order_search.add(btnOrdNewTerm, "cell 0 20");
		JButton btnOrdClear = new JButton("Clear Filter");								//<--CLEAR FILTER
		btnOrdClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("Manager GUI - Clear Order Filters clicked");
				ordSearchTerm = "";
				pushOrderSearch();
			}
		});
		order_search.add(btnOrdClear, "cell 0 21");
		
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
				System.out.println("MngrGUI Login - Login clicked: user = "+user+", pass = "+pass);
				login.setEnabled(false);
				// Pass to controller
				controller.inputCommand(new eMart.QueryLogin(Database.DEST_MANAG, user, pass));
			}
		});
		loginPanelBtn.add(loginBtn);
		login = new JDialog(frame, "Login ADMIN:", true);
		login.setContentPane(loginPane);
		login.setResizable(false);
		login.setBounds(300, 300, 450, 150);
		login.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		login.pack();
			
		// ====================================================================================================
		// Catalog Search term Popup.
		// ====================================================================================================
		searchCata = new JDialog(frame, "Create Search Term:", true);
		searchCata.setResizable(false);
		searchCata.setBounds(300, 300, 450, 150);
		searchCata.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
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
				System.out.println("MngrGUI Search Popup - OK Clicked: card = "+s);
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
								searchTermListCata.addElement(cmd);
								pushCataSearch();
							}
							searchCata.setVisible(false);
							searchSelect.setSelectedIndex(0);
							break;
						case "Search by Attribute":
							// Get attribute and value from appropriate text boxes
							s1 = (String)attrSelect.getSelectedItem();
							s2 = attrText.getText(); attrText.setText("");
							if(!s2.isEmpty()) {
								if(s1 == "Item ID")  s1 = "iid";
								cmd = "C."+s1+" = '"+s2+"'";
								searchTermListCata.addElement(cmd);
								pushCataSearch();
							}
							searchCata.setVisible(false);
							break;
						case "Search by Description":
							s1 = descText1.getText(); descText1.setText("");
							s2 = descText2.getText(); descText2.setText("");
							if(!s1.isEmpty()&&!s2.isEmpty()) {
								cmd = "D.Attribute = '"+s1+"'";
								searchTermListCata.addElement(cmd);
								cmd = "D.Value = '"+s2+"'";
								searchTermListCata.addElement(cmd);
								pushCataSearch();
							}
							searchCata.setVisible(false);
							searchSelect.setSelectedIndex(0);
							break;
					default:
						return;
				}
			}
		});
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				searchCata.setVisible(false);
				searchSelect.setSelectedIndex(0);
				attrText.setText(""); descText1.setText(""); descText2.setText("");
			}
		});
		searchCata.setContentPane(popupPane);
		
		// ====================================================================================================
		// Order Search term Popup.
		// ====================================================================================================
		searchOrders = new JDialog(frame, "Create Search Term:", true);
		searchOrders.setResizable(false);
		searchOrders.setBounds(300, 300, 450, 150);
		searchOrders.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		//search.pack();
		JPanel popupPane2 = new JPanel();
		popupPane2.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(popupPane2);
		popupPane2.setLayout(new BorderLayout(0, 0));
		// OK and cancel buttons
		JPanel popupControls2 = new JPanel();
		popupPane2.add(popupControls2, BorderLayout.SOUTH);
		popupControls2.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		final JButton btnOk2 = new JButton("OK");
		popupControls2.add(btnOk2);
		JButton btnCancel2 = new JButton("Cancel");
		popupControls2.add(btnCancel2);
		// Central panel
		final JPanel popupBody = new JPanel();
		popupPane2.add(popupBody, BorderLayout.CENTER);
		popupBody.setLayout(null);
		final JTextField attrText2 = new JTextField();
		attrText2.setBounds(244, 12, 182, 19);
		popupBody.add(attrText2);
		attrText2.setColumns(10);
		String attrSelectItems2[] = {"Order ID", "Item ID", "Category", "Customer ID", "Manufacturer"};
		final JComboBox<String> attrSelect2 = new JComboBox<String>(attrSelectItems2);
		attrSelect2.setBounds(12, 12, 173, 19);
		popupBody.add(attrSelect2);
		// Action listener for OK and cancel controls
		btnOk2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("MngrGUI Search Popup - OK Clicked");
				String s1, s2;
				String cmd;
				// Get attribute and value from appropriate text boxes
				s1 = (String)attrSelect2.getSelectedItem();
				s2 = attrText2.getText(); attrText2.setText("");
				if(!s2.isEmpty()) {
					switch(s1) {
						case "Order ID": s1="O.oid"; break;
						case "Item ID": s1="I.iid"; break;
						case "Customer ID": s1="O.cid"; break;
						default: s1 = "I."+s1;
					}
					cmd = s1+" = '"+s2+"'";
					ordSearchTerm = cmd;
					pushOrderSearch();
				}
				searchOrders.setVisible(false);
			}
		});
		btnCancel2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				searchOrders.setVisible(false);
				attrText2.setText("");
			}
		});
		searchOrders.setContentPane(popupPane2);
		// ====================================================================================================
	}

	@Override
	public void run() {
		controller = eMart.Ref();
		
		// Set frame to be visible and open login dialog
		this.frame.setVisible(true);	
		login.setVisible(true);
	}
	
	
	// ====================================================================================================
	// Implementations of tables.
	// ====================================================================================================
	/**
	 * Table for displaying catalog entries
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
	    			for(col=0; col<4; col++) 
	    				obj[col] =rs.getString(col+1);
	    			obj[col]=rs.getString(col+2); // skip over warranty - not important to manager
	    			this.addRow(obj);
	    		} while(rs.next());
			}
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
