package bgu.spl.mics.application.services;

import bag.spl.mics.objects.Model;
import bag.spl.mics.objects.Pair;
import bag.spl.mics.objects.Student;
import bgu.spl.mics.*;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Student is responsible for sending the {@link TrainModelEvent},
 * {@link TestModelEvent} and {@link PublishResultsEvent}.
 * In addition, it must sign up for the conference publication broadcasts.
 * This class may not hold references for objects which it is not responsible for.
 * <p>
 * You can add private fields and public methods to this class.
 * You MAY change constructor signatures and even add new public constructors.
 */
public class StudentService extends MicroService {
    MessageBus messageBus = MessageBusImpl.getInstance();
    private Student student;
    private Vector<Pair<Model, Boolean>> modelList;// TODO :the boolean is in order to not send the same event 10000 times

    private Future<Model> futureTrain;
    private Future<Model> futureTest;

    public StudentService(String name, List<Model> modelList, Student student) {
        super(name);
        this.modelList = new Vector<>();
        for (Model model : modelList) {
            this.modelList.add(new Pair<>(model, false));
        }
        this.student = student;
        this.futureTest = null;
        this.futureTrain = null;
    }

    public Student getStudent() {
        return this.student;
    }

    @Override
    protected void initialize() {
        messageBus.register(this);

        subscribeBroadcast(TerminateBroadcast.class, lastCall ->
        {
            terminate();

        });
        subscribeBroadcast(TickBroadcast.class, tickIncoming -> onTick());

        subscribeBroadcast(PublishConferenceBroadcast.class, broadcast -> {
              List<Model> modelList = broadcast.getModels();
              if(modelList!=null){
          for (Model model : modelList) {
                if (model.getStudent().equals(student))
                    this.student.increasePublications();
               else
                    this.student.increaseNumberOfReadPaper();
           }}
              else {
              }
        });


    }

    public void onTick() {

        if (student.getI() < this.modelList.size() && this.modelList.get(student.getI()).getFirst().getStatus() == Model.Status.PreTrained) {
            this.modelList.get(student.getI()).getFirst().setStatus(Model.Status.Training);
            Model model = this.modelList.get(student.getI()).getFirst();
            TrainModelEvent trainModelEvent = new TrainModelEvent(model, this);//send if not visited
            futureTrain = super.sendEvent(trainModelEvent);
            futureTrain.get();
            this.modelList.get(student.getI()).getFirst().setStatus(Model.Status.Trained);
            futureTest = sendEvent(new TestModelEvent(model, this));
            model = futureTest.get();
            modelStatusCases(model, futureTest.get().getStatus());
        }
    }


    private void modelStatusCases(Model currentModel, Model.Status status) {
        if (status == Model.Status.Tested) {
            this.student.increaseI();
            if (currentModel.getResults() == Model.Results.Good) {
                PublishResultsEvent publishEvent = new PublishResultsEvent(currentModel);//TODO
                sendEvent(publishEvent);
            }
        }


    }


}
