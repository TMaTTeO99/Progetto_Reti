import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

//classe che conterra un metodo run per poter periodicamente creare un nuova parola
public class OpenGame implements Runnable{

    private long time;//tempo che intercorre fra la publicazione di una parola e la successiva
    private SessioneWordle game;
    private long lasttime;//tempo in millisecondi della precedente creazione di un gioco, tale info
                          //viene estrapolata dal file di config
    private long currenttime;//variabile di istanza per salvare il tempo corrente
    private ArrayList<String> Vocabolario;//parola del gioco
    private File ConfigureFile;//file di configurazione che deve essere aggiornato alla chiusura del server per inserire il timestamp
                               //Dell ultima volta in cui è stata estratta una parola
    private SessioneWordle Game;//variabile che rappresenta il gioco
    private Lock lock;

    //private Condition cond; //Da eliminare
    public OpenGame(long t, long lt, ArrayList<String> Vcb, File ConfFile, SessioneWordle gm, Lock lck/*, Condition cnd*/) {
        lasttime = lt;
        time = t;
        Vocabolario = Vcb;
        ConfigureFile = ConfFile;
        Game = gm;
        lock = lck;
        //cond = cnd;
    }
    public void run() {

        Random randword = new Random();
        while(!Thread.interrupted()){
            //qui devo controllare che sia passato il giusto intervallo di tempo fra la precedente
            //creazione di un gioco e il momento corrente
            try {
                currenttime = System.currentTimeMillis();
                if((currenttime - lasttime) < time) {
                    Thread.sleep(time - (currenttime - lasttime));
                }
                lock.lock();
                Game.setWord(Vocabolario.get(randword.nextInt(Vocabolario.size())));
                Game.setLastTime(lasttime);
                Game.setNextTime(time);
                lasttime = System.currentTimeMillis();
                currenttime = System.currentTimeMillis();
                Game.setCurrentTime(currenttime);
                Game.setTentativi();
                lock.unlock();
                System.out.println("Game creato");

            }
            catch (Exception e) {e.printStackTrace();}
        }
        //qui quando tale thread verrà interrotto dovro aggiornare il file di configurazione con
        //lasttime attuale, la prima volta che il server sara lanciato sara 0,
        //quindi devo fare un metodo per effettuare questo aggiornamento
        System.out.println("esco");
    }
}
