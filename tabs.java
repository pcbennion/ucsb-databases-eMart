
import javax.swing.*;
import javax.swing.BoxLayout.*;
import java.awt.event.*;
import java.awt.*;

public class tabs extends JPanel{

    protected JButton addToCart;
    public tabs()
    {
        super(new GridLayout(1,1));

	JTabbedPane tabbedPane = new JTabbedPane();

	JComponent catalog = makeCatalogTable();
	tabbedPane.addTab("Catalog",catalog); 
	tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);
	
	JComponent orders = makeTextPanel("Orders");
	tabbedPane.addTab("Orders",orders); 
	tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);
	
	
	JComponent customerinfo = makeTextPanel("Customer Information");
	tabbedPane.addTab("Customer Information",customerinfo); 
	tabbedPane.setMnemonicAt(2, KeyEvent.VK_3);
	
	JComponent cart = makeTextPanel("Cart");
	tabbedPane.addTab("Cart",cart); 
	tabbedPane.setMnemonicAt(3, KeyEvent.VK_4);


	//Add the tabbed pane to this panel.
	add(tabbedPane);
        
        //The following line enables to use scrolling tabs.
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

    }

   protected JComponent makeTextPanel(String text) {
        JPanel panel = new JPanel();
        JLabel filler = new JLabel(text);
        filler.setHorizontalAlignment(JLabel.CENTER);
        panel.setLayout(new GridLayout(1, 1));
        panel.add(filler);
        panel.setPreferredSize(new Dimension(700,700));
        //panel.setBounds(700,250,200,200);
        return panel;
    }
    protected JComponent makeCatalogTable() {
    
   	
        
        JPanel panel = new JPanel(new FlowLayout());
        //JLabel filler = new JLabel(text);
        //filler.setHorizontalAlignment(JLabel.CENTER);
        //panel.setLayout(new GridLayout(1, 1));
        //panel.add(filler);
        panel.setPreferredSize(new Dimension(700,700));
        JComponent addtoCart = makeButton("Add to Cart");
       
        //addtoCart.setAlignmentX(Component.LEFT_ALIGNMENT);
        //addtoCart.setAlignmentY(Component.BOTTOM_ALIGNMENT);
        
        
        //panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
        //panel.setBounds(700,250,200,200);
               String[] columnNames = {"First Name",
                                "Last Name",
                                "Sport",
                                "# of Years",
                                "Vegetarian"};
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
        JTable table = new JTable(data,columnNames);
        //creates the scrollable plane
        JScrollPane spTable = new JScrollPane(table);
        //dimension(width,height)
        table.setPreferredScrollableViewportSize(new Dimension(400, 300));
        
        
        

        
        //spTable.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        
         panel.add(addtoCart,BorderLayout.SOUTH);
         panel.add(spTable,BorderLayout.WEST); 
        return panel;
    }
    protected JComponent makeButton(String s)
    {
      JButton b = new JButton(s);
      return b;
    }     

   private static void createGUI()
   {
   JFrame frame = new JFrame();
   frame.addWindowListener(new WindowAdapter() 
   {

      public void windowClosing(WindowEvent e) 
      {
	System.exit(0);
      }
   });
   frame.add(new tabs(), BorderLayout.CENTER);
   //sets the frame location near the middle
   frame.setLocationRelativeTo(null);
   
   frame.pack();
   frame.setVisible(true);				
  }





public static void main (String args[])
{
    SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                //Turn off metal's use of bold fonts
		//UIManager.put("swing.boldMetal", Boolean.FALSE);
		createGUI();
            }
        });
     }
}
