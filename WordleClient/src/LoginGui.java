import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class LoginGui implements ActionListener {

    private JTextField uname;
    private JTextField passwd;
    private Socket socket;
    private NotificaClient skeleton;
    private ImplementazioneNotificaClient notifica;
    private JFrame CurrentFrame;

    public LoginGui(JTextField u, JTextField p, Socket sck, JFrame frame)throws Exception {
        uname = u;
        passwd = p;
        socket = sck;
        CurrentFrame = frame;
        notifica = new ImplementazioneNotificaClient();
        skeleton = (NotificaClient) UnicastRemoteObject.exportObject(notifica, 0);
    }

    public void actionPerformed(ActionEvent e) {

        //in questo metodo dovro aggiungere l implementazione gia fatta nel main per effettuare il login

        String user = uname.getText();
        String pass = new String(passwd.getText());
        byte [] tmp = new byte[100];
        ByteArrayInputStream BuffIn = new ByteArrayInputStream(tmp);
        try {
            DataOutputStream ou = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            DataInputStream inn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            ou.writeInt((("login:"+ user + " " + pass).length())*2);
            ou.writeChars("login:" + user + " " + pass);
            ou.flush();
            System.out.print(inn.readInt());
            System.out.print(inn.readChar());
            System.out.print(inn.readChar());
            System.out.print(inn.readChar());
            System.out.print(inn.readChar());
            System.out.print(inn.readChar());
            System.out.println(inn.readChar());

            switch(inn.readInt()) {
                case 0 :
                    //definisco un interfaccia anonima con un metodo actionPerformed in modo da poter chiudere il frame
                    //quando si effettua il login del client
                    JOptionPane.showMessageDialog(null, "Login completato");
                    Timer timer = new Timer(0, new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            CurrentFrame.dispose(); // Chiude il frame
                        }
                    });
                    GameGui gg = new GameGui(socket, notifica, user);//avvio il frame di gioco
                    timer.start();//chiudo il frame iniziale
                    /**
                     * A questo punto il client non termina perhce rimane esportato l'ggetto
                     * per effettuare la notifica, naturalmente dovra essere esportato quando
                     * il client fara la login e eliminata l'esportazione in seguito a una logout
                     */
                    //ora qua provo a inviare lo stub al server dopo che mi sono registrato ecc
                    Registrazione servizio = (Registrazione) LocateRegistry.getRegistry(6500).lookup("Registrazione");;
                    servizio.sendstub(user, skeleton);
                    break;
                case -1:
                    JOptionPane.showMessageDialog(null, "Errore. Per partecipare al gioco bisogna prima essere iscritti");
                    break;
                case -2:
                    JOptionPane.showMessageDialog(null, "Errore. Password inserita non corretta");
                    break;
                case -3:
                    JOptionPane.showMessageDialog(null, "Login gia effettuato");
                    break;
            }
        }
        catch (Exception ee) {ee.printStackTrace();}
    }

}
