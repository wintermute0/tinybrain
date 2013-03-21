package yatan.distributed.akka;

import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import yatan.distributed.DistributedComputerException;
import yatan.distributed.akka.message.ActorInvokeMessage;
import yatan.distributed.akka.message.BaseMessage;

import akka.actor.UntypedActor;

public class BaseActor<T extends ActorContract> extends UntypedActor {
    private final Logger logger = Logger.getLogger(getClass());
    private final T actorImplmementation;
    private static final Map<Class<?>, Class<?>> PRIMITIVES_TO_WRAPPERS = new HashMap<Class<?>, Class<?>>();
    private BaseMessage message;

    static {
        PRIMITIVES_TO_WRAPPERS.put(boolean.class, Boolean.class);
        PRIMITIVES_TO_WRAPPERS.put(byte.class, Byte.class);
        PRIMITIVES_TO_WRAPPERS.put(char.class, Character.class);
        PRIMITIVES_TO_WRAPPERS.put(double.class, Double.class);
        PRIMITIVES_TO_WRAPPERS.put(float.class, Float.class);
        PRIMITIVES_TO_WRAPPERS.put(int.class, Integer.class);
        PRIMITIVES_TO_WRAPPERS.put(long.class, Long.class);
        PRIMITIVES_TO_WRAPPERS.put(short.class, Short.class);
        PRIMITIVES_TO_WRAPPERS.put(void.class, Void.class);
    }

    public BaseActor(T actorImpl) {
        this.actorImplmementation = actorImpl;
        this.actorImplmementation.setActor(this);
    }

    @Override
    public void preStart() {
        super.preStart();

        this.actorImplmementation.preStart();
    }

    @Override
    public void onReceive(Object message) throws DistributedComputerException {
        this.logger.trace("Receive message: " + message);
        if (!(message instanceof BaseMessage)) {
            throw new UnknownMessageException("Unkonwn message: " + message);
        }

        this.message = (BaseMessage) message;

        if (message instanceof ActorInvokeMessage) {
            ActorInvokeMessage invokeMessage = (ActorInvokeMessage) message;
            try {
                Method targetMethod = null;
                for (Method method : this.actorImplmementation.getClass().getMethods()) {
                    if (method.getName().equals(invokeMessage.getCommand())
                            && method.getParameterTypes().length == (invokeMessage.getArguments() == null ? 0
                                    : invokeMessage.getArguments().length)) {
                        // check whether parameter types match
                        boolean parameterMatch = true;
                        for (int i = 0; i < method.getParameterTypes().length; i++) {
                            if (invokeMessage.getArguments()[i] != null
                                    && !wrap(method.getParameterTypes()[i]).isAssignableFrom(
                                            wrap(invokeMessage.getArguments()[i].getClass()))) {
                                parameterMatch = false;
                                break;
                            }
                        }

                        // check if we found the method to invoke
                        if (parameterMatch) {
                            targetMethod = method;
                            break;
                        }
                    }
                }

                // if we didn't found any matching method
                if (targetMethod == null) {
                    throw new InvalidMessageException("Actor implementation " + this.actorImplmementation
                            + " cannot handle message " + invokeMessage);
                }

                targetMethod.invoke(this.actorImplmementation, invokeMessage.getArguments());
            } catch (IllegalArgumentException e) {
                throw new ActorInvocationException("Error occurred while executing message " + invokeMessage.toString()
                        + " on actor implementation " + this.actorImplmementation + ". IllegalArgumentException: "
                        + e.getMessage(), e);
            } catch (IllegalAccessException e) {
                throw new ActorInvocationException("Error occurred while executing message " + invokeMessage.toString()
                        + " on actor implementation " + this.actorImplmementation + ". IllegalAccessException: "
                        + e.getMessage(), e);
            } catch (InvocationTargetException e) {
                throw new ActorInvocationException("Error occurred while executing message " + invokeMessage.toString()
                        + " on actor implementation " + this.actorImplmementation + ". InvocationTargetException: "
                        + e.getMessage(), e);
            }
        } else {
            // FIXME: throw unkonwn type exception
        }

        this.message = null;
    }

    public BaseMessage getMessage() {
        return this.message;
    }

    // safe because both Long.class and long.class are of type Class<Long>
    @SuppressWarnings("unchecked")
    private static <T> Class<T> wrap(Class<T> c) {
        return c.isPrimitive() ? (Class<T>) PRIMITIVES_TO_WRAPPERS.get(c) : c;
    }
}
