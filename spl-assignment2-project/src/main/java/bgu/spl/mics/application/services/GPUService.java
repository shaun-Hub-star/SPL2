package bgu.spl.mics.application.services;

import bag.spl.mics.objects.Cluster;
import bag.spl.mics.objects.GPU;
import bag.spl.mics.objects.Model;
import bag.spl.mics.objects.Student;
import bgu.spl.mics.*;
import bgu.spl.mics.application.OutputFile;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * GPU service is responsible for handling the
 * {@link TrainModelEvent} and {@link TestModelEvent},
 * in addition to sending the .
 * This class may not hold references for objects which it is not responsible for.
 * <p>
 * You can add private fields and public methods to this class.
 * You MAY change constructor signatures and even add new public constructors.
 */
public class GPUService extends MicroService {
    private MessageBus messageBus;
    private GPU gpu;
    private Model currentModel;
    private BlockingQueue<Model> modelQueue;
    private BlockingQueue<TrainModelEvent> trainModelEventBlockingQueue;

    private Cluster cluster = Cluster.getInstance();

    public GPUService(String name) {
        super("GPUService");
        messageBus = MessageBusImpl.getInstance();
    }

    public GPUService(String name, GPU gpu) {
        super(name);
        this.modelQueue = new LinkedBlockingQueue<>();
        this.gpu = gpu;
        messageBus = MessageBusImpl.getInstance();
        trainModelEventBlockingQueue = new LinkedBlockingQueue<>();
    }

    @Override
    protected void initialize() {
        messageBus.register(this);
        subscribeBroadcast(TerminateBroadcast.class, lastCall -> this.terminate());

        subscribeBroadcast(TickBroadcast.class, tickIncoming -> {
            if (gpu.isFinished()) {
                complete(trainModelEventBlockingQueue.take(), null);
                gpu.setFirst_run(true);
                if (!modelQueue.isEmpty()) {
                    modelQueue.remove();
                }
                sendEvent(new TestModelEvent(gpu.getModel(), this));
                if (!modelQueue.isEmpty()) {
                    currentModel = modelQueue.peek();
                    gpu.setCurrentUnProcessedBatchesIndex(0);
                    gpu.setModel(currentModel);
                    gpu.sendUnprocessedBatch();
                }


            } else {
                cluster.getStatistics().increaseNumberOfGPUTimeUnitUsed();
                gpu.trainDataBatches();
            }

        });

        subscribeEvent(TrainModelEvent.class, event -> {
            if (modelQueue.isEmpty()) {
                if (currentModel == null)
                    currentModel = event.getModel();
                gpu.setModel(currentModel);
                gpu.sendUnprocessedBatch();

            }

            this.modelQueue.add(event.getModel());
            this.trainModelEventBlockingQueue.add((event));

        });

        subscribeEvent(TestModelEvent.class, event -> {
            currentModel = event.getModel();
            Model.Results valueOfTest;
            //masters
            if (event.getSenderStatus() == Student.Degree.MSc) {
                if (Math.random() <= 0.6) {
                    valueOfTest = Model.Results.Good;
                } else
                    valueOfTest = Model.Results.Bad;
            }
            //PhD
            else {
                if (Math.random() <= 0.8) {
                    valueOfTest = Model.Results.Good;
                } else
                    valueOfTest = Model.Results.Bad;
            }

            currentModel.setResults(valueOfTest);
            currentModel.setStatus(Model.Status.Tested);
            cluster.addFinishedModelName(currentModel.getName());
            OutputFile outputFile = OutputFile.getInstance();
            if (currentModel.getStatus() == Model.Status.Tested) {
                outputFile.addToStudentModel((currentModel));
            }
            complete(event, currentModel);


        });

    }

}
