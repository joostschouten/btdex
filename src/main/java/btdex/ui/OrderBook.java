package btdex.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.AbstractCellEditor;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;

import bt.Contract;
import btdex.core.ContractState;
import btdex.core.Globals;
import btdex.core.Market;
import btdex.sm.SellContract;
import burst.kit.entity.BurstAddress;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

public class OrderBook extends JPanel {

	private static final long serialVersionUID = 1L;

	JTable table;
	DefaultTableModel model;

	Lock lock = new ReentrantLock();

	HashMap<BurstAddress, ContractState> map = new HashMap<>();
	ArrayList<ContractState> marketContracts = new ArrayList<>();

	public static final int COL_CONTRACT = 3;
	public static final int COL_SECURITY = 4;
	public static final int COL_ACTION = 5;

	String[] columnNames = {"ASK",
			"SIZE (BURST)",
			"TOTAL",
			"CONTRACT",
			"SECURITY (BURST)",
			// "TIMEOUT (MINS)",
	"ACTION"};

	Icon TAKE_ICON = IconFontSwing.buildIcon(FontAwesome.HANDSHAKE_O, 12);

	Market selectedMarket = null;

	class MyTableModel extends DefaultTableModel {
		private static final long serialVersionUID = 1L;

		public int getColumnCount() {
			return columnNames.length;
		}

		public String getColumnName(int col) {
			String colName = columnNames[col];
			if(col == 0 || col == 2)
				colName += " (" + selectedMarket.toString() + ")";
			return colName;
		}

		public boolean isCellEditable(int row, int col) {
			return col == COL_ACTION || col == COL_CONTRACT;
		}
	}

	public static class ButtonCellRenderer extends DefaultTableCellRenderer	{
		private static final long serialVersionUID = 1L;

		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			return (JButton)value;
		}
	}

	class ButtonCellEditor extends AbstractCellEditor implements TableCellEditor {
		private static final long serialVersionUID = 1L;

		JButton but;

		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int row, int column) {

			return but = (JButton) value;
		}

		public Object getCellEditorValue() {
			return but;
		}

		// validate the input
		public boolean stopCellEditing() {
			return super.stopCellEditing();
		}
	};

	public void setMarket(Market m) {
		this.selectedMarket = m;

		model.fireTableStructureChanged();

		update();
	}

	public OrderBook(Market m) {
		super(new BorderLayout());

		selectedMarket = m;

		table = new JTable(model = new MyTableModel());

		table.setRowHeight(table.getRowHeight()+7);

		JScrollPane scrollPane = new JScrollPane(table);
		table.setFillsViewportHeight(true);

		// Center header and all columns
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );
		for (int i = 0; i < table.getColumnCount()-1; i++) {
			table.getColumnModel().getColumn(i).setCellRenderer( centerRenderer );			
		}
		JTableHeader jtableHeader = table.getTableHeader();
		DefaultTableCellRenderer rend = (DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer();
		rend.setHorizontalAlignment(JLabel.CENTER);
		jtableHeader.setDefaultRenderer(rend);

		table.getColumnModel().getColumn(COL_ACTION).setCellRenderer(new ButtonCellRenderer());
		table.getColumnModel().getColumn(COL_ACTION).setCellEditor(new ButtonCellEditor());
		table.getColumnModel().getColumn(COL_CONTRACT).setCellRenderer(new ButtonCellRenderer());
		table.getColumnModel().getColumn(COL_CONTRACT).setCellEditor(new ButtonCellEditor());

		table.getColumnModel().getColumn(COL_CONTRACT).setPreferredWidth(120);

		add(scrollPane, BorderLayout.CENTER);
	}

	synchronized public void update() {
		ContractState.addContracts(map);

		marketContracts.clear();
		for(ContractState s : map.values()) {
			if(s.getMarket() == selectedMarket.getID() && s.getAmountNQT() > 0
					&& s.getState() == SellContract.STATE_OPEN
					&& Globals.isArbitratorAccepted(s.getArbitrator1())
					&& Globals.isArbitratorAccepted(s.getArbitrator2()) )
				marketContracts.add(s);
		}

		// sort by rate
		marketContracts.sort(new Comparator<ContractState>() {
			@Override
			public int compare(ContractState o1, ContractState o2) {
				return (int)(o1.getRate() - o2.getRate());
			}
		});

		for (ContractState s : marketContracts) {
			s.update();
		}

		model.setRowCount(marketContracts.size());

		// Update the contents
		for (int row = 0; row < marketContracts.size(); row++) {
			for (int col = 0; col < model.getColumnCount(); col++) {
				ContractState s = marketContracts.get(row);
				switch (col) {
				case 0:
					model.setValueAt(selectedMarket.numberFormat(s.getRate()), row, col);
					break;
				case 1:
					model.setValueAt(s.getAmount(), row, col);
					break;
				case 2:
					model.setValueAt(selectedMarket.numberFormat((s.getRate()*s.getAmountNQT()) / Contract.ONE_BURST),
							row, col);
					break;
				case COL_CONTRACT:
					model.setValueAt(new JButton(s.getAddress().getRawAddress()), row, col);
					break;
				case COL_SECURITY:
					model.setValueAt(s.getSecurity(), row, col);
					break;
				case COL_ACTION:
					model.setValueAt(new JButton("Take"), row, col);
					break;
					//				return new JButton("Take", TAKE_ICON);
				default:
					break;
				}				
			}
		}
	}
}