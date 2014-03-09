package edu.ucsb.cs174a.w14.edluong.eStore_eMart;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.table.*;

import net.miginfocom.swing.*;

import java.sql.*;
import java.util.*;

public class CustUI extends JFrame implements Runnable{
	
	private static final long serialVersionUID = 4831439921059983751L;

	public eMart mart;
	
	private ResultSet input;
	
	private JFrame frame;
	private JTextField txtQty;
	private JTable tableCata;
	CatalogTable tableCataData;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					CustUI frame = new CustUI();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public CustUI() {
		initialize();
	}
	
	/**
	 * Initialize the contents of the frame. Created using WindowBuilder.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setResizable(false);
		frame.setBounds(100, 100, 700, 700);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.X_AXIS));
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		frame.getContentPane().add(tabbedPane);
		
		JPanel catalog_tab = new JPanel();
		tabbedPane.addTab("Catalog", null, catalog_tab, null);
		catalog_tab.setLayout(new BorderLayout(0, 0));
		
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
		
		JButton btnAddToCart = new JButton("Add to Cart");
		catalog_controls.add(btnAddToCart);
		
		JButton btnRemoveFromCart = new JButton("Remove from Cart");
		catalog_controls.add(btnRemoveFromCart);
		
		JButton btnRefresh = new JButton("Refresh");
		btnRefresh.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				
			}
		});
		catalog_controls.add(btnRefresh);
		
		JPanel catalog_search = new JPanel();
		catalog_tab.add(catalog_search, BorderLayout.EAST);
		catalog_search.setLayout(new MigLayout("", "[73.00px,grow]", "[15px][399.00,grow][][]"));
		
		JLabel lblSearchTerms = new JLabel("Search Terms:");
		catalog_search.add(lblSearchTerms, "cell 0 0,alignx left,aligny top");
		
		JButton btnAddTerm = new JButton(" Add Term...");
		btnAddTerm.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			}
		});
		
		JPanel searchTerms = new JPanel();
		catalog_search.add(searchTerms, "cell 0 1,grow");
		searchTerms.setLayout(new BorderLayout(0, 0));
		
		JList<String> searchTermList = new JList<String>();
		searchTerms.add(searchTermList);
		catalog_search.add(btnAddTerm, "cell 0 2");
		
		JButton btnClearTerms = new JButton("Clear Terms");
		catalog_search.add(btnClearTerms, "cell 0 3");
		
		JPanel catalog_table = new JPanel();
		catalog_tab.add(catalog_table, BorderLayout.CENTER);
		
		String[] columnNames = {"IID", "CATEGORY", "WARRANTY", "PRICE", "MANUFACTURER", "MODEL #"};
		Object[][] data = {
			{"Kathy", "Smith",
					"Snowboarding", new Integer(5), new Boolean(false)},
			{"John", "Doe",
					"Rowing", new Integer(3), new Boolean(true)},
			{"Sue", "Black",
					"Knitting", new Integer(2), new Boolean(false)},
			{"Jane", "White",
					"Speed reading", new Integer(20), new Boolean(true)},
			{"Joe", "Brown",
					"Pool", new Integer(10), new Boolean(false)}
		};
		tableCataData = new CatalogTable(columnNames, 36);
		catalog_table.setLayout(new BorderLayout(0, 0));
		tableCata = new JTable(tableCataData);
		//tableCata.setPreferredScrollableViewportSize(new Dimension(370, 400));
		//tableCata.setPreferredSize(new Dimension(370, 400));
		tableCata.setRowSelectionAllowed(true);
		tableCata.setFillsViewportHeight(true); 
		JScrollPane spCata = new JScrollPane(tableCata);
		tableCata.setPreferredScrollableViewportSize(new Dimension(400, 300));
		catalog_table.add(tableCata.getTableHeader(), BorderLayout.PAGE_START);
		catalog_table.add(spCata, BorderLayout.CENTER);
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
		
		try {
			this.frame.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Customer Interface - Sending Input to Mart.");
		mart.inputCommand(new eMart.QueryCatalogAttr("iid", '>', "150"));
		try {
			System.out.println("Customer Interface - Listening for Input.");
			while (!Thread.currentThread().isInterrupted()) {
	            try {
	            	Thread.sleep(50);
	            	if(input != null) {
	            		System.out.println("Customer Interface - Input Event!");
	            		tableCataData.setContents(input);
	            		/*
	            		if(input.next()) {
		            		ResultSetMetaData rsmd = input.getMetaData();
		            		assert(rsmd.getTableName(1)=="Catalog");
		            		int numCol = rsmd.getColumnCount();
		            		int i;
		            		for(i=1; i<=numCol; i++)
		            			System.out.print(rsmd.getColumnName(i) + " ");
		            		do{
		            			System.out.print("\n");
		            			for(i=1; i<numCol; i++)
		            				System.out.print(input.getString(i) + " ");
		            		} while(input.next());
		            		System.out.print("\n");
		            		rsmd=null;
		            	}*/
	            		//input = null;
	            		break;
	            	}
	            }catch(InterruptedException ex) {break;}
			}
			System.out.println("Customer Interface - Listener Closed");
			Database.stmt.close();
		} catch (SQLException e) {e.printStackTrace();}	
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

}
