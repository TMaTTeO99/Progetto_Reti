import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.rmi.server.UnicastRemoteObject;

public class StartGui {

    private JPanel panelLogin;
    private JPanel panelRegistrazione;
    private JPanel mainPanel;
    private JFrame frame;
    private Socket socket;


    public StartGui() {

        frame = new JFrame("Wordle");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocation(new Point(200, 200));
        socket = new Socket();

        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.LINE_AXIS));
        panelLogin = getPanelLogin(frame);
        panelRegistrazione = getPanelRegistrazione();
        mainPanel.add(panelLogin);
        mainPanel.add(panelRegistrazione);
        frame.getContentPane().add(mainPanel);

        frame.setSize(new Dimension(1000, 500));
        frame.setVisible(true);

        try {
            socket.connect(new InetSocketAddress("localhost", 6501));
        }
        catch (Exception ee) {
            ee.printStackTrace();
        }


    }




    //metodi utilizzati per creare l'interfaccia grafica
    private JPanel getPanelLogin(JFrame frame) {

        JPanel panel = new JPanel();
        JButton login = new JButton("Accedi");
        JTextField user = new JTextField(10);
        JTextField pass = new JPasswordField(10);
        NotificaClient skeleton = null;

        panel.setLayout(new FlowLayout());
        panel.add(new JLabel("Login:"));
        panel.add(new JLabel("Username"));
        panel.add(user);
        panel.add(new JLabel("Password"));
        panel.add(pass);

        try {login.addActionListener(new LoginGui(user, pass, socket, frame));}
        catch (Exception e) {e.printStackTrace();}
        panel.add(login);

        return panel;

    }
    private JPanel getPanelRegistrazione() {

        JPanel panel = new JPanel();
        JButton registrati = new JButton("Registrati");
        JTextField user = new JTextField(10);
        JTextField pass = new JPasswordField(10);

        panel.setLayout(new FlowLayout());
        panel.add(new JLabel("Registrazione:"));
        panel.add(new JLabel("Username"));
        panel.add(user);
        panel.add(new JLabel("Password"));
        panel.add(pass);
        registrati.addActionListener(new RegistrGui(user, pass));
        panel.add(registrati);

        return panel;
    }
}
