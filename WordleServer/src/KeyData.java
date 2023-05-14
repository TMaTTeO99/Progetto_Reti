//classe usata per mantenere l associazione fra una key e un oggetto future, tale oggetto conterra i dati che in modo asincrono i thread producono

import java.nio.channels.SelectionKey;
import java.security.Key;
import java.util.concurrent.Future;

public class KeyData {

    private SelectionKey key;
    private Future<PkjData> dati;

    public KeyData(SelectionKey k, Future<PkjData> d) {
        key = k;
        dati = d;
    }
    public Future<PkjData> getDati() {return dati;}
    public void setDati(Future<PkjData> d) {dati = d;}

    public SelectionKey getKey() {return key;}

    public void setKey(SelectionKey k) {key = key;}
}
