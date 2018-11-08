package asq.choices.client.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchProviderException;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.bouncycastle.openpgp.PGPException;

import asq.choices.KeyMgmt;
import asq.choices.client.Client;

public class ButtonListener implements ActionListener, ListSelectionListener, ZKprotoUpdateListener {
	private JTextField t1;
	private JPasswordField t2;
	private JTextField t3;
	private JTextField t4;
	private JTextField serverName;
	private Client client;
	private KeyMgmt k;
	private JButton loginButton;
	private JButton startButton;
	private DefaultListModel listModel;
	private JList list;
	private JTextArea pubKeyField;

	public ButtonListener(JTextField t1, JPasswordField t2, JTextField t3, JTextField t4,
			JButton loginButton, JButton startButton, DefaultListModel listModel,
			JList list, JTextArea pubKeyField, JTextField serverName) {
		this.t1 = t1;
		this.t2 = t2;
		this.t3 = t3;
		this.t4 = t4;
		this.loginButton = loginButton;
		this.startButton = startButton;
		this.listModel = listModel;
		this.list = list;
		this.pubKeyField = pubKeyField;
		this.serverName = serverName;
	}

	class RunProtocol extends Thread {
		public void run() {
			try {
				client.runZKprotocolOnce();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			startButton.setEnabled(true);
		}
	}
	@Override
	public void actionPerformed(ActionEvent event) {
		try {
			if (event.getActionCommand().equals("Einloggen")) {
				k = new KeyMgmt(t1.getText(), t2.getPassword());
				client = new Client(serverName.getText(), 63425, t1.getText(), k.getPubKeyFile(), k, this);
				t2.setText("");
				loginButton.setEnabled(false);
				startButton.setEnabled(true);
			} else if (event.getActionCommand().equals("Start")) {
				startButton.setEnabled(false);
				new RunProtocol().start();
			} else if (event.getActionCommand().equals("Laden")) {
				if (t1.getText().equals("")) {
					t4.setText("Gib deinen Namen ein.");
					t4.setForeground(Color.RED);
				} else {
					try {
						String s = loadChoiceFromFile();
						t4.setText(s);
						t4.setForeground(Color.BLUE);
					} catch (IOException e) {
						t4.setText("Konnte Datei nicht laden.");
						t4.setForeground(Color.RED);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("bloed");
		}
	}

	public String loadChoiceFromFile() throws IOException {
		byte[] buffer = new byte[1024];
		FileInputStream fin = new FileInputStream(Client.getResultFileName(t1.getText()));
		int anzahl = fin.read(buffer);
		fin.close();
		return new String(buffer, 0, anzahl);
	}

	@Override
	public void committedChoice(String name) {
		t4.setText(name);
		t4.setForeground(Color.BLUE);
	}
	@Override
	public void currentRound(int round) {
		t3.setText(round + "");
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
	    if (e.getValueIsAdjusting() == false) {

            if (list.getSelectedIndex() == -1) {
            	//No selection, disable fire button.
            	pubKeyField.setText("");
            } else {
            	//Selection, enable the fire button.
                pubKeyField.setText(client.getPubKey(list.getSelectedIndex()));
            }
        }
	}

	@Override
	public void listChanged(String[] users) {
		listModel.clear();

		for (String user: users) {
			listModel.addElement(user);
		}
	}

	@Override
	public void reportError(String s) {
		loginButton.setEnabled(true);
		startButton.setEnabled(false);
    	pubKeyField.setText(s);
	}

	@Override
	public void pgpError(String s) {
		loginButton.setEnabled(true);
		startButton.setEnabled(false);
		t4.setText("Falsches Passwort?");
		t4.setForeground(Color.RED);
    	pubKeyField.setText(s);
	}

	@Override
	public void loggedOut() {
		loginButton.setEnabled(true);
		startButton.setEnabled(false);
	}


}
