package btdex.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import btdex.core.Account;
import btdex.core.Globals;
import btdex.core.Market;
import layout.SpringUtilities;

public class AccountsPanel extends JPanel implements ActionListener, ListSelectionListener {

	private static final long serialVersionUID = 1L;

	JTable table;
	DefaultTableModel model;

	private JButton addButton;

	private JButton removeButton;

	private JComboBox<Market> marketComboBox;

	private JTextField nameField;
	private JPanel formPanel;
	private ArrayList<JTextField> formFields = new ArrayList<>();

	private JButton cancelButton;

	private JButton okButton;

	private JPanel right;

	private JPanel left;

	private Main main;

	private JPanel rightButtonPane;

	public static final int COL_MARKET = 0;
	public static final int COL_NAME = 1;

	static final int PAD = 6;

	static final String[] COLUMN_NAMES = {
			"MARKET",
			"NAME",
	};

	public AccountsPanel(Main main) {
		super(new BorderLayout());

		this.main = main;

		table = new JTable(model = new DefaultTableModel(COLUMN_NAMES, 0));
		table.setRowHeight(table.getRowHeight()+7);
		table.setPreferredScrollableViewportSize(new Dimension(400, 200));
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(this);

		left = new JPanel(new BorderLayout());
		right = new JPanel();
		right.setVisible(false);
		right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

		left.setBorder(BorderFactory.createTitledBorder("Your accounts"));
		right.setBorder(BorderFactory.createTitledBorder("Account details"));

		JScrollPane scrollPane = new JScrollPane(table);
		table.setFillsViewportHeight(true);

		// Center header and all columns
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );
		for (int i = 0; i < table.getColumnCount(); i++) {
			table.getColumnModel().getColumn(i).setCellRenderer( centerRenderer );			
		}
		JTableHeader jtableHeader = table.getTableHeader();
		DefaultTableCellRenderer rend = (DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer();
		rend.setHorizontalAlignment(JLabel.CENTER);
		jtableHeader.setDefaultRenderer(rend);

		table.setAutoCreateColumnsFromModel(false);
		table.getColumnModel().getColumn(COL_NAME).setPreferredWidth(200);
		table.getColumnModel().getColumn(COL_MARKET).setPreferredWidth(20);

		JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		left.add(buttonPane, BorderLayout.PAGE_END);

		addButton = new JButton("ADD");
		removeButton = new JButton("REMOVE");
		removeButton.setEnabled(false);

		addButton.addActionListener(this);
		removeButton.addActionListener(this);

		buttonPane.add(addButton);
		buttonPane.add(removeButton);

		marketComboBox = new JComboBox<Market>();
		for(Market m : Globals.getInstance().getMarkets()) {
			if(m.getTokenID()!=null)
				continue;
			marketComboBox.addItem(m);
		}
		marketComboBox.addActionListener(this);

		JPanel topPanel = new JPanel(new SpringLayout());
		topPanel.add(new Desc("Market", marketComboBox), BorderLayout.LINE_START);
		topPanel.add(new Desc("Account name", nameField = new JTextField()), BorderLayout.CENTER);
		SpringUtilities.makeCompactGrid(topPanel, 1, 2, 0, 0, PAD, PAD);
		right.add(topPanel);

		formPanel = new JPanel(new SpringLayout());
		//		JScrollPane formScroll = new JScrollPane(formPanel);
		//		right.add(formScroll);
		right.add(formPanel);

		cancelButton = new JButton("Cancel");
		okButton = new JButton("OK");

		cancelButton.addActionListener(this);
		okButton.addActionListener(this);
		rightButtonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		rightButtonPane.add(cancelButton);
		rightButtonPane.add(okButton);
		right.add(rightButtonPane);

		add(left, BorderLayout.LINE_START);
		JPanel rightContainer = new JPanel(new BorderLayout());
		rightContainer.add(right, BorderLayout.PAGE_START);
		add(rightContainer, BorderLayout.CENTER);

		left.add(scrollPane, BorderLayout.CENTER);	

		loadAccounts();
	}

	private void loadAccounts() {
		model.setNumRows(0);
		ArrayList<Account> accs = Globals.getInstance().getAccounts();
		for (int i = 0; i < accs.size(); i++) {
			Object []row = new Object[2];
			row[0] = accs.get(i).getMarket();
			row[1] = accs.get(i).getName();
			model.addRow(row);
		}
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		Market market = (Market) marketComboBox.getSelectedItem();
		ArrayList<String> fieldNames = market.getFieldNames();

		if(e.getSource() == addButton) {
			right.setVisible(true);
			marketComboBox.setEnabled(true);
			nameField.setEditable(true);
			rightButtonPane.setVisible(true);
			marketComboBox.setSelectedIndex(0);
			addButton.setEnabled(false);
			table.setEnabled(false);
			table.clearSelection();
		}
		if(e.getSource() == cancelButton) {
			right.setVisible(false);
			addButton.setEnabled(true);
			table.setEnabled(true);
		}
		if(e.getSource() == okButton) {
			HashMap<String, String> fields = new HashMap<>();

			for (int i = 0; i < formFields.size(); i++) {
				fields.put(fieldNames.get(i), formFields.get(i).getText());
			}
			try {
				market.validate(fields);
			}
			catch (Exception ex) {
				Toast.makeText(main, ex.getMessage(), Toast.Style.ERROR).display(okButton);
				return;
			}

			String name = nameField.getText();
			if(name.trim().length()==0)
				name = market.simpleFormat(fields);

			Account ac = new Account(market.toString(), name, fields);
			Globals.getInstance().addAccount(ac);

			loadAccounts();
			right.setVisible(false);
			addButton.setEnabled(true);
			table.setEnabled(true);
		}
		if(e.getSource() == marketComboBox) {
			createFields(fieldNames, true);
			formFields.get(0).requestFocusInWindow();
		}

		if(e.getSource() == removeButton) {
			int row = table.getSelectedRow();
			if(row >= 0) {
				int ret = JOptionPane.showConfirmDialog(main,
						"Remove the selected account?\n" + 
								"This cannot be undone.", "Remove account",
								JOptionPane.YES_NO_OPTION);
				if(ret == JOptionPane.YES_OPTION) {
					Globals.getInstance().removeAccount(row);
					loadAccounts();
					right.setVisible(false);
				}				
			}
		}
	}
	
	private void createFields(ArrayList<String> fieldNames, boolean editable) {
		formPanel.removeAll();
		formFields.clear();

		for (int i = 0; i < fieldNames.size(); i++) {
			JLabel l = new JLabel(fieldNames.get(i), JLabel.TRAILING);
			formPanel.add(l);
			JTextField textField = new JTextField(10);
			textField.setEditable(editable);
			formFields.add(textField);
			l.setLabelFor(textField);
			formPanel.add(textField);
		}			
		SpringUtilities.makeCompactGrid(formPanel, fieldNames.size(), 2, 0, 0, PAD, PAD);
		validate();
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting())
			return;
		int row = table.getSelectedRow();
		removeButton.setEnabled(row >= 0);
		
		if(row >= 0) {
			rightButtonPane.setVisible(false);
			right.setVisible(true);
			marketComboBox.setEnabled(false);
			nameField.setEditable(false);
			
			// show this account properties
			Account ac = Globals.getInstance().getAccounts().get(row);
			
			for (int i = 0; i < marketComboBox.getItemCount(); i++) {
				if(ac.getMarket().equals(marketComboBox.getItemAt(i).toString())) {
					marketComboBox.setSelectedIndex(i);
					break;
				}
			}
			nameField.setText(ac.getName());
			
			ArrayList<String> fieldNames = new ArrayList<>();
			fieldNames.addAll(ac.getFields().keySet());
			createFields(fieldNames, false);
			
			for (int i = 0; i < fieldNames.size(); i++) {
				formFields.get(i).setText(ac.getFields().get(fieldNames.get(i)));
			}
		}
	}
}
