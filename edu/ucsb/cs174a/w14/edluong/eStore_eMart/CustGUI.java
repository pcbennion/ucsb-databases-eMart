package edu.ucsb.cs174a.w14.edluong.eStore_eMart;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
	private String CID = null;
	
	private JFrame frame;
	private JDialog login;
	private JTable tableCata;
	
	// Members that can be operated on by other threads
	private volatile CatalogTable tableCataData;

	private JTextField txtQty;
	
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
	}
	/**
	 * Receive and interpret login return
	 */
	public void SetLoginResult(ResultSet rs) throws SQLException {
		// If the login returns a valid result, close login pane.
		if(rs.next()) {
    		CID = rs.getString(1);
    		System.out.println("CustGUI Login - CID recieved: "+CID);
    		login.setVisible(false);
    		// Initialize info on all panels
    		controller.inputCommand(new eMart.QueryCatalog(Database.DEST_CSTMR));
		} else { // Otherwise, re-enable login to try again.
			login.setEnabled(true);
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
		JPanel catalog_tab = new JPanel();
		tabbedPane.addTab("Catalog", null, catalog_tab, null);
		catalog_tab.setLayout(new BorderLayout(0, 0));
		
		// Table for displaying items
		JPanel catalog_table = new JPanel();
		String[] columnNames = {"IID", "CATEGORY", "WARRANTY", "PRICE", "MANUFACTURER", "MODEL #"};
		tableCataData = new CatalogTable(columnNames, 36);
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
		txtQty = new JTextField();
		txtQty.setText("");
		catalog_controls.add(txtQty);
		txtQty.setColumns(10);
		JButton btnAddToCart = new JButton("Add to Cart");			//<--ADD TO CART
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
			}
		});
		catalog_controls.add(btnAddToCart);
		JButton btnRemoveFromCart = new JButton("Remove from Cart");//<--REMOVE FROM CART
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
			}
		});
		catalog_controls.add(btnRemoveFromCart);
		JButton btnRefresh = new JButton("Refresh");				//<--REFRESH PAGE CONTENTS
		btnRefresh.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("Customer GUI - Refresh Catalog clicked");
				controller.inputCommand(new eMart.QueryCatalog(Database.DEST_CSTMR));
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
		JList<String> searchTermList = new JList<String>();
		searchTerms.add(searchTermList);
		JButton btnAddTerm = new JButton(" Add Term...");			//<--ADD NEW SEARCH TERM
		btnAddTerm.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("Add Term clicked");
			}
		});
		catalog_search.add(btnAddTerm, "cell 0 2");
		JButton btnClearTerms = new JButton("Clear Terms");			//<--CLEAR ALL SEARCH TERMS
		btnClearTerms.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("Remove Terms clicked");
			}
		});
		catalog_search.add(btnClearTerms, "cell 0 3");
		
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
	}

	@Override
	public void run() {
		controller = eMart.Ref();
		
		// Set frame to be visible and open login dialog
		this.frame.setVisible(true);	
		login.setVisible(true);
	}
}
	
// ====================================================================================================
// Implementations of tables.
// ====================================================================================================
/**
 * Table for displaying items
 */
@SuppressWarnings("serial")
class CatalogTable extends DefaultTableModel {
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
// ====================================================================================================