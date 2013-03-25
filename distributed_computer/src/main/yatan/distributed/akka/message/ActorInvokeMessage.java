package yatan.distributed.akka.message;

import java.util.Arrays;
import java.util.Iterator;

import com.google.common.base.Splitter;

public class ActorInvokeMessage extends BaseMessage {
    private String command;
    private Object[] arguments;

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public void setArguments(Object... arguments) {
        this.arguments = arguments;
    }

    @Override
    public String toString() {
        Iterator<String> argStringIterator =
                Splitter.fixedLength(1000).omitEmptyStrings().split(Arrays.toString(this.arguments)).iterator();
        return getClass().getSimpleName() + " [command=" + command + ", arguments=" + argStringIterator.next()
                + (argStringIterator.hasNext() ? "..." : "") + "]";
    }
}
