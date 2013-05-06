package yatan.distributedcomputer.serialize;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import akka.serialization.JSerializer;

public class BaseMessageSerializer extends JSerializer {
    private final Kryo kryo = new Kryo();

    public BaseMessageSerializer() {
    }

    @Override
    public int identifier() {
        return 100;
    }

    @Override
    public boolean includeManifest() {
        return true;
    }

    @Override
    public byte[] toBinary(Object object) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Output output = new Output(os);
        try {
            this.kryo.writeObject(output, object);
        } finally {
            output.close();
            try {
                os.close();
            } catch (IOException e) {
                // ignore
            }
        }
System.out.println("aaaaa");
        return os.toByteArray();
    }

    @Override
    public Object fromBinaryJava(byte[] bytes, Class<?> type) {
        Input input = new Input(bytes);
        try {
            return this.kryo.readObject(input, type);
        } finally {
            input.close();
        }
    }
}
