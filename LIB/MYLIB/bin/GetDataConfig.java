import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

//classe usata per recuperare i dati di configurazione sia per il client che per il server
public class GetDataConfig {

    private String URLtranslate;//Stringa usata poer contenere l url del servizio di traduzione
    private long LastTimeWord = 0;//var utilizzata per poter recuperare il tempo in millisecondi dell ultima volta in cui una parola è stata estratta
    private final long DayInMS = 86400000;//conversione di un giorno in millisecondi
    private final long OraInMS = 3600000; //conversione di 24 ore in millisecondi
    private final long MinInMS = 60000; //conversione di 1 minuto in millisecondi
    private final long SecInMS = 1000;

    private int MaxThread;//di default assegno un numero di thread cosi in caso di errore nel file di config
   							  //iol server comunque puo lavorare
    private long TimeStempWord = 86400000;//24 ore in millisecondi
    private String ConfigureFileName; //nome del file contenente le info di configurazione
    private String PathSerialization;
    private String PathVocabolario;
    private String PathStart; //Path della dir da cui cominciare la ricerca del file
    private int PortExport; // il valore che  assumerà questa var deve essere lo sesso lato client nel suo file dio config
    private String IP_Multicast;//stringa che conterra l ip del gruppo multicast su cui condividere i risultati del gioco
    private int Port_Multicast;//porta usata per la condivisione sul gruppo multicast
    private int Port_ListeningSocket;
    private int P;
    private int G;
    private String IP_server;
    private File ConfigureFile = null;


    public GetDataConfig(String ConfgName, String ConfPath) {
        ConfigureFileName = ConfgName;
        PathStart = ConfPath;
    }

    private long ToMLS(String arg) throws Exception{

        //il formato della stringa arg è: giorni ore minuti secondi
        long tm = 0;

        StringTokenizer tok = new StringTokenizer(arg, " ");
        tm += DayInMS * Integer.parseInt(tok.nextToken());
        tm += OraInMS * Integer.parseInt(tok.nextToken());
        tm += MinInMS * Integer.parseInt(tok.nextToken());
        tm += SecInMS * Integer.parseInt(tok.nextToken());

        return tm;
    }
    //metodo privato per effettuare il pars di ogni linea letta dal file di configurazione
    private void ParsCLine(String ln) throws Exception{

        StringTokenizer tok = new StringTokenizer(ln, "=");
        while(tok.hasMoreTokens()) {
                switch(tok.nextToken()) {
                    case "thread" :
                        MaxThread = Integer.parseInt(tok.nextToken());
                        break;
                    case "timeword" :
                        TimeStempWord = ToMLS(tok.nextToken());//metodo privato per la conversione in long
                        break;
                    case "pathjson" :
                        PathSerialization = tok.nextToken();
                        break;
                    case "PortExport":
                        PortExport = Integer.parseInt(tok.nextToken());
                        break;
                    case "PathVocabolario":
                        PathVocabolario = tok.nextToken();
                        break;
                    case "lastWord":
                        LastTimeWord = Long.parseLong(tok.nextToken());
                        break;
                    case "URL":
                        URLtranslate = tok.nextToken();
                        break;
                    case "IP_multicast":
                        IP_Multicast = tok.nextToken();
                        break;
                    case "Port_multicast":
                        Port_Multicast = Integer.parseInt(tok.nextToken());
                        break;
                    case "Port_ListenigSck":
                        Port_ListeningSocket = Integer.parseInt(tok.nextToken());
                        break;
                    case "IP_server":
                        IP_server = tok.nextToken();
                        break;
                    case "P_number" :
                        P = Integer.parseInt(tok.nextToken());
                        break;
                    case "G_generator" :
                        G = Integer.parseInt(tok.nextToken());
                        break;
                }
            }
    }
    public void ReadConfig() throws Exception{

        String line = null;
        BufferedReader inConfig = new BufferedReader(new FileReader(ConfigureFile));
        while((line = inConfig.readLine()) != null) {ParsCLine(line);}

        inConfig.close();
    }
    //metodo per la ricerca del path del file di configurazione
    public void SearchFile(File curfile) {

        String [] lst = curfile.list();
        File next = null;
        for(int i = 0; i<lst.length; i++) {
            next = new File(curfile+"/"+lst[i]);
            try {
                if(next.isFile()) {
                    if(next.getName().equals(ConfigureFileName))ConfigureFile = new File(next.getPath());
                }
                else if(next.isDirectory()) SearchFile(next);
            }
            catch (Exception e) {e.printStackTrace();}
        }
    }
    public ArrayList<String > getVocabolario() {

        ArrayList<String > tmp = new ArrayList<>();
        File pathWord = new File(PathVocabolario);

        try (BufferedReader in = new BufferedReader(new FileReader(pathWord))){
            String word = null;
            while((word = in.readLine()) != null) {
                tmp.add(word);
            }
        }
        catch (Exception e) {e.printStackTrace();}
        return tmp;
    }

    //metodi per recuperare le info dall oggetto
    public File getConfigureFile() {return ConfigureFile;}
    public String getURLtranslate() {return URLtranslate;}
    public long getLastTimeWord() {return LastTimeWord;}
    public int getMaxThread() {return MaxThread;}
    public long getTimeStempWord() {return TimeStempWord;}
    public String getPathSerialization() {return PathSerialization;}
    public int getPortExport() {return PortExport;}
    public String getIP_Multicast() {return IP_Multicast;}
    public int getPort_Multicast() {return Port_Multicast;}
    public int getPort_ListeningSocket() {return Port_ListeningSocket;}
    public String getPathStart() {return PathStart;}
    public String getIP_server() {return IP_server;}

    public int getG() {return G;}

    public int getP() {return P;}

    //metodi per settare i dati all interno dell oggetto nel caso il file di configurazione fosse corrotto

    public void setP(int p) {P = p;}
    public void setG(int g) {G = g;}
    public void setURLtranslate(String url) {URLtranslate = url;}
    public void setLastTimeWord(long ltw) {LastTimeWord = ltw;}
    public void setMaxThread(int maxT) {MaxThread = maxT;}
    public void setTimeStempWord(long tsw) {TimeStempWord = tsw;}
    public void setPathSerialization(String path) {PathSerialization = path;}
    public void setPortExport(int port) {PortExport = port;}
    public void setIP_Multicast(String IP) {IP_Multicast = IP;}
    public void setPort_Multicast(int port) {Port_Multicast = port;}
    public void setPort_ListeningSocket(int port) {Port_ListeningSocket = port;}
    public void setIP_server(String IP) {IP_server = IP;}
    public void setPathVocabolario(String pathVocabolario) {PathVocabolario = pathVocabolario;}
}
