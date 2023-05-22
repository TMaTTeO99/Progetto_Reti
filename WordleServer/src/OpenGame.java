public class OpenGame implements Runnable{

    private long time;//tempo che intercorre fra la publicazione di una parola e la successiva
    private SessioneWordle game;
    private long lasttime;//tempo in millisecondi della precedente creazione di un gioco, tale info
                          //viene estrapolata dal file di config
    private long currenttime;//variabile di istanza per salvare il tempo corrente
    private String word;//parola del gioco
    public OpenGame(long t, long lt, String w) {
        lasttime = lt;
        time = t;
        word = w;
    }
    public void run() {


        while(!Thread.interrupted()){
            //qui devo controllare che sia passato il giusto intervallo di tempo fra la precedente
            //creazione di un gioco e il momento corrente
            try {
                currenttime = System.currentTimeMillis();
                if((currenttime - lasttime) < time) {
                    Thread.sleep(currenttime - lasttime);
                }
                game = new SessioneWordle(word);
            }
            catch (Exception e) {e.printStackTrace();}
        }
        //qui quando tale thread verrà interrotto dovro aggiornare il file di configurazione con
        //lasttime attuale, quello iniziale sarà 0

    }
    public SessioneWordle getGame() {return game;}
}
