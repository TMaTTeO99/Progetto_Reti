import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class ProxySerializer extends JsonSerializer<Serializable> {

    @Override
    public void serialize(Serializable value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new ByteArrayOutputStream());
            oos.writeObject(value);
            oos.flush();
            gen.writeObject(oos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}