package bgu.spl.mics;

import bag.spl.mics.objects.Model;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The {@link MessageBusImpl class is the implementation of the MessageBus interface.
 * Write your implementation here!
 * Only private fields and methods can be added to this class.
 */
public class MessageBusImpl implements MessageBus {

    private AtomicInteger eventsIndex = new AtomicInteger(0);

    private static class SingletonHolder {
        private static MessageBus messageBus = new MessageBusImpl();
    }


    private ConcurrentHashMap<MicroService, BlockingQueue<Message>> existingMicroServices;
    private ConcurrentHashMap<Class<? extends Event<?>>, List<MicroService>> mapEventsToMicroServiceType;
    private ConcurrentHashMap<Class<? extends Broadcast>, List<MicroService>> mapBroadToMicroServiceType;
    private ConcurrentHashMap<MicroService, List<Class<? extends Event<?>>>> microServiceEventsList;
    private ConcurrentHashMap<MicroService, List<Class<? extends Broadcast>>> microServiceBroadcastList;
    private ConcurrentHashMap<Event<?>, Future<?>> mapEventToFuture;
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();


    public static synchronized MessageBus getInstance() {
        return SingletonHolder.messageBus;
    }

    private MessageBusImpl() {
        existingMicroServices = new ConcurrentHashMap<>();
        mapEventsToMicroServiceType = new ConcurrentHashMap<>();
        microServiceEventsList = new ConcurrentHashMap<>();
        mapEventToFuture = new ConcurrentHashMap<>();
        microServiceBroadcastList = new ConcurrentHashMap<>();
        mapBroadToMicroServiceType = new ConcurrentHashMap<>();

    }

    private synchronized <T> void roundRobbin(Event<T> e, ConcurrentHashMap<Class<? extends Event<?>>, List<MicroService>> mapEventsToMicroServiceType) throws InterruptedException {

        int number_of_microServices = mapEventsToMicroServiceType.get(e.getClass()).size();
        int index = eventsIndex.intValue() % number_of_microServices;
        eventsIndex.incrementAndGet();
        MicroService m = mapEventsToMicroServiceType.get(e.getClass()).get(index);
        existingMicroServices.get(m).put(e);


    }

    @Override
    public <T> void subscribeEvent(Class<? extends Event<T>> type, MicroService m) {
        synchronized (lock1) {
            if (!microServiceEventsList.containsKey(m))
                microServiceEventsList.put(m, new ArrayList<>());
            microServiceEventsList.get(m).add(type);

            if (!mapEventsToMicroServiceType.containsKey(type))
                mapEventsToMicroServiceType.put(type, new ArrayList<>());
            mapEventsToMicroServiceType.get(type).add(m);
        }
    }


    @Override
    public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m) {
        synchronized (lock1) {
            if (!microServiceBroadcastList.containsKey(m))
                microServiceBroadcastList.put(m, new ArrayList<>());
            microServiceBroadcastList.get(m).add(type);
        }
        synchronized (lock2) {
            if (!mapBroadToMicroServiceType.containsKey(type))
                mapBroadToMicroServiceType.put(type, new ArrayList<>());
            mapBroadToMicroServiceType.get(type).add(m);
        }

    }

    /**
     * A Micro-Service calls this method in
     * order to notify the Message-Bus that the event was handled, and provides the
     * result of handling the request. The Future object associated with event e
     * should be resolved to the result given as a parameter.
     */
    @Override
    public <T> void complete(Event<T> e, T result) {
        Future<T> future = (Future<T>) this.mapEventToFuture.get(e);
        future.resolve(result);
    }


    @Override

    public synchronized void sendBroadcast(Broadcast b) {
        try {
            for (MicroService microService : mapBroadToMicroServiceType.get(b.getClass())) {
                try {
                    existingMicroServices.get(microService).put(b);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        } catch (ConcurrentModificationException ignore) {

        }

    }



    @Override
    public synchronized <T> Future sendEvent(Event<T> e) {
        if (mapEventsToMicroServiceType.containsKey(e.getClass())) {
            try {
                Future<Model> future = new Future<>();
                roundRobbin(e, mapEventsToMicroServiceType);

                mapEventToFuture.put(e, future);

            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            } catch (NullPointerException exception) {
                exception.printStackTrace();
            }
            return mapEventToFuture.get(e);
        }


        return null;
    }


    @Override
    public void register(MicroService m) {
        if (!existingMicroServices.containsKey(m)) {
            existingMicroServices.put(m, new LinkedBlockingQueue<>());
        }
    }

    @Override
    public void unregister(MicroService m) {
        if (microServiceBroadcastList.containsKey(m)) {
            synchronized (microServiceBroadcastList.get(m)) {
                for (Class<? extends Broadcast> b : microServiceBroadcastList.get(m))
                    mapBroadToMicroServiceType.get(b).remove(m);
            }
        }
        if (microServiceEventsList.containsKey(m)) {
            synchronized (microServiceEventsList.get(m)) {
                for (Class<? extends Event<?>> e : microServiceEventsList.get(m))
                    mapEventsToMicroServiceType.get(e).remove(m);
            }
        }
        microServiceEventsList.remove(m);
        microServiceBroadcastList.remove(m);
    }


    @Override
    public Message awaitMessage(MicroService m) throws InterruptedException {
        if (existingMicroServices.containsKey(m))
            return existingMicroServices.get(m).take();
        throw new IllegalArgumentException();
    }


}
/**
 * @mapEventToFuture will used for the complete method.
 * @mapEventToMicroServiceBlockingQueues will used for mapping trainModelEvent
 * to the gpu in a round robbin manner.
 * @mapMicroServiceToMessage for each micro service the list of events he handles.
 * @mapEventToMicroServiceBlockingQueues will used for mapping trainModelEvent
 * to the gpu in a round robbin manner.
 * @mapMicroServiceToMessage for each micro service the list of events he handles.
 * @mapEventToMicroServiceBlockingQueues will used for mapping trainModelEvent
 * to the gpu in a round robbin manner.
 * @mapMicroServiceToMessage for each micro service the list of events he handles.
 * @mapEventToMicroServiceBlockingQueues will used for mapping trainModelEvent
 * to the gpu in a round robbin manner.
 * @mapMicroServiceToMessage for each micro service the list of events he handles.
 * @mapEventToMicroServiceBlockingQueues will used for mapping trainModelEvent
 * to the gpu in a round robbin manner.
 * @mapMicroServiceToMessage for each micro service the list of events he handles.
 * @mapEventToMicroServiceBlockingQueues will used for mapping trainModelEvent
 * to the gpu in a round robbin manner.
 * @mapMicroServiceToMessage for each micro service the list of events he handles.
 * @mapEventToMicroServiceBlockingQueues will used for mapping trainModelEvent
 * to the gpu in a round robbin manner.
 * @mapMicroServiceToMessage for each micro service the list of events he handles.
 * @mapEventToMicroServiceBlockingQueues will used for mapping trainModelEvent
 * to the gpu in a round robbin manner.
 * @mapMicroServiceToMessage for each micro service the list of events he handles.
 * @mapEventToMicroServiceBlockingQueues will used for mapping trainModelEvent
 * to the gpu in a round robbin manner.
 * @mapMicroServiceToMessage for each micro service the list of events he handles.
 *//*
    private ConcurrentHashMap<Event, Future> mapEventToFuture = new ConcurrentHashMap<>();
    *//**
 * @mapEventToMicroServiceBlockingQueues will used for mapping trainModelEvent
 * to the gpu in a round robbin manner.
 *//*
    private ConcurrentHashMap<Class<? extends Event>, BlockingQueue<MicroService>>
            mapEventToMicroServiceBlockingQueues = new ConcurrentHashMap<>();

    private ConcurrentHashMap<Class<? extends Broadcast>, List<MicroService>>
            mapBroadCastToMicroServiceBlockingQueues = new ConcurrentHashMap<>();
    */
/**
 * @mapMicroServiceToMessage for each micro service the list of events he handles.
 *//*
    private ConcurrentHashMap<MicroService,
            List<Class<? extends Message>>>
            mapMicroServiceToMessage = new ConcurrentHashMap<>();*/