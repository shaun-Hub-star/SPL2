package bag.spl.mics.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Passive object representing a single CPU.
 * Add all the fields described in the assignment as private fields.
 * Add fields and methods to this class as you see fit (including public methods and constructors).
 */
public class CPU {
    private int cores;
    private BlockingDeque<DataBatch> processingData;
    private Cluster cluster;
    private HashMap<DataBatch, AtomicInteger> timeToTrainABatch;//from the GPU
    private int i = 0;

    public CPU(int cores, List<DataBatch> processingData) {
        this.cores = cores;
        this.processingData = pushDataBatchToProcessingData(processingData);
        this.cluster = Cluster.getInstance();

        setTimeToTrainABatch();
    }

    public CPU(int cores) {
        this.cores = cores;
        this.processingData = new LinkedBlockingDeque<>();
        this.timeToTrainABatch = new HashMap<>();
        this.cluster = Cluster.getInstance();

    }


    /**
     * @pre b = this.getCluster().getStatistics().getNumber_of_processed_batches()
     * @inv b>=0
     * @post (b. @ pre = = b. @ post - 1)
     */

    ///DELETE?
    private List<Integer> computeTime() {
        int size = 32 / cores;
        List<Integer> compute_times = new ArrayList<>();
        for (DataBatch dataBatch : processingData) {
            Data.Type type = dataBatch.getData().getType();
            if (type == Data.Type.Images) {
                compute_times.add(size * 4);
            } else if (type == Data.Type.Text) {
                compute_times.add(size * 2);
            } else {
                compute_times.add(size * 1);
            }
        }
        return compute_times;
    }


    public void setTimeToTrainABatch() {//receives a processed batch from the cluster and adds the batch to the toTrainMap
        for (DataBatch dataBatch : processingData) {
            this.timeToTrainABatch.put(dataBatch, new AtomicInteger(getTimeToProcess(dataBatch)));
        }
    }

    public int getTimeToProcess(DataBatch dataBatch) {
        int size = 32 / cores;
        Data.Type type = dataBatch.getData().getType();
        if (type == Data.Type.Images)
            return size * 4;
        else if (type == Data.Type.Text) {
            return size * 2;
        } else
            return size * 1;
    }


    public void processDataBatches() {

        DataBatch dataBatch = this.processingData.peek();
        if (dataBatch != null&&timeToTrainABatch.get(dataBatch)!=null) {
            Integer temp = this.timeToTrainABatch.get(dataBatch).decrementAndGet();
            if (finishedTraining(temp)) {
                sendProcessedBatch(dataBatch);
                if (!processingData.remove(dataBatch)) {
                } else {
                    this.timeToTrainABatch.remove(dataBatch);
                }
            }
        }
    }

    // }

    private void sendProcessedBatch(DataBatch dataBatch) {//(DataBatch dataBatch)
        if (dataBatch != null) {
            this.cluster.sendProcessedBatch(dataBatch);

        }
    }

    //TODO look/if where we push 1 dataBanch to CPU
    public void getUnProcessedBatchFromCluster(DataBatch dataBatch) {//receives a processed batch from the cluster and adds the batch to the toTrainMap
        this.processingData.add(dataBatch);
        this.timeToTrainABatch.put(dataBatch, new AtomicInteger(getTimeToProcess(dataBatch)));

    }


    public BlockingDeque<DataBatch> pushDataBatchToProcessingData(List<DataBatch> listToProcessingData) {
        BlockingDeque<DataBatch> queueProcessingData = new LinkedBlockingDeque<>();
        for (DataBatch dataBatch : listToProcessingData) {
            queueProcessingData.add(dataBatch);
            getUnProcessedBatchFromCluster(dataBatch);

        }
        return queueProcessingData;

    }



    public boolean finishedTraining(Integer temp) {
       return temp == 0;
    }

    public int getCores() {
        return cores;
    }




    public void receiveBatchToProcess(DataBatch dataBatch) {
        getUnProcessedBatchFromCluster(dataBatch);
    }
}