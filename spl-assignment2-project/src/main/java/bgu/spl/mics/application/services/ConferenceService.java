package bgu.spl.mics.application.services;

import bag.spl.mics.objects.Cluster;
import bag.spl.mics.objects.ConfrenceInformation;
import bag.spl.mics.objects.Model;
import bgu.spl.mics.*;

import java.util.List;

/**
 * Conference service is in charge of
 * aggregating good results and publishing them via the ,
 * after publishing results the conference will unregister from the system.
 * This class may not hold references for objects which it is not responsible for.
 * <p>
 * You can add private fields and public methods to this class.
 * You MAY change constructor signatures and even add new public constructors.
 */
public class ConferenceService extends MicroService {
    private MessageBus messageBus;
    private ConfrenceInformation conference;
    private Cluster cluster = Cluster.getInstance();



    public ConferenceService(String name,ConfrenceInformation conference) {
        super(name);
        messageBus = MessageBusImpl.getInstance();
        this.conference = conference;
    }

    @Override
    protected void initialize() {
        messageBus.register(this);
        subscribeBroadcast(TerminateBroadcast.class, lastCall -> this.terminate());

        subscribeBroadcast(TickBroadcast.class, tickIncoming -> {
            if (conference.checkPublish()) {
                sendBroadcast(new PublishConferenceBroadcast(conference.getSuccessfulModel()));
                messageBus.unregister(this);

            }
        });




        subscribeEvent(PublishResultsEvent.class, event -> {
             Model successfulModel = event.getModel();
            conference.addModel(successfulModel);

        });


    }
}
