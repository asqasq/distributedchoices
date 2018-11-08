package asq.choices.client.gui;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

public class MainWindow {
	private Container contentPane;
	private JPanel p1;
	private JLabel l1;
	private JTextField t1;
	private JLabel l2;
	private JPasswordField t2;
	private JButton b1;

	private JPanel p2;
	private JLabel l3;
	private JTextField t3;
	private JLabel l4;
	private JTextField t4;
	private JButton b2;

	private JList list;
	private DefaultListModel listModel;
	private JPanel p4;
	private JTextArea pubKeyText;

	private JPanel p3;

	private JPanel p5;
	private JButton b3;

	private JTextField serverName;


	public void startGui() {
		JFrame frame = new JFrame("Wichtelprogramm");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800,500);


		contentPane = frame.getContentPane();

		JPanel basePanel = new JPanel();
		basePanel.setLayout(new BoxLayout(basePanel, BoxLayout.Y_AXIS));

		contentPane.add(basePanel);

		p1 = new JPanel();
		l1 = new JLabel("Name:");
		p1.add(l1);
		t1 = new JTextField(10);
		p1.add(t1);
		l2 = new JLabel("Passwort:");
		p1.add(l2);
		t2 = new JPasswordField(10);
		p1.add(t2);
		p1.add(new JLabel("Server:"));
		serverName = new JTextField(20);
		serverName.setText("localhost");
		p1.add(serverName);
		b1 = new JButton("Einloggen");
		p1.add(b1);
		basePanel.add(p1);

		p4 = new JPanel();
        listModel = new DefaultListModel();

        //Create the list and put it in a scroll pane.
        list = new JList(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//        list.setSelectedIndex(0);
        list.setVisibleRowCount(20);
        list.setFixedCellWidth(100);
        JScrollPane listScrollPane = new JScrollPane(list);
        p4.add(listScrollPane);

        pubKeyText = new JTextArea();
        pubKeyText.setEditable(false);
        pubKeyText.setColumns(40);
        pubKeyText.setRows(20);
        pubKeyText.setText("                                                                ");
        p4.add(pubKeyText);

        basePanel.add(p4);


		p2 = new JPanel();
		b2 = new JButton("Start");
		b2.setEnabled(false);
		p2.add(b2);
		l3 = new JLabel("Runde:");
		p2.add(l3);
		t3 = new JTextField(10);
		t3.setHorizontalAlignment(SwingConstants.RIGHT);
		t3.setEditable(false);
		p2.add(t3);
		basePanel.add(p2);

		p5 = new JPanel();
		p5.add(new JLabel("Entscheidung aus Datei laden: "));
		b3 = new JButton("Laden");
		p5.add(b3);
		basePanel.add(p5);

		p3 = new JPanel();
		l4 = new JLabel("Du wichtelst fuer:");
		p3.add(l4);
		t4 = new JTextField(20);
		t4.setEditable(false);
		p3.add(t4);
		basePanel.add(p3);



		ButtonListener buttonListener = new ButtonListener(t1, t2, t3, t4, b1, b2, listModel, list, pubKeyText, serverName);
		b1.addActionListener(buttonListener);
		b2.addActionListener(buttonListener);
		b3.addActionListener(buttonListener);

        list.addListSelectionListener(buttonListener);

		frame.setVisible(true);
	}
}
