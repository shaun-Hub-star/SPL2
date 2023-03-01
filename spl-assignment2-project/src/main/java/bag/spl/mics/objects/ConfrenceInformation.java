package bag.spl.mics.objects;

import bgu.spl.mics.PublishResultsEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Passive object representing information on a conference.
 * Add fields and methods to this class as you see fit (including public methods and constructors).
 */
public class ConfrenceInformation {

    private String name;
    private int date;
    private int counterTick = 0;
    private List<String> successfulModelName;//the name of successful models
    private List<Model> successfulModel;
    private List<String> notSuccessfulModelName = new ArrayList<>();
    private int i = 0;

    public ConfrenceInformation(String name, int date) {
        this.name = name;
        this.date = date;
        this.successfulModelName = null;
        this.successfulModel = new ArrayList<>();
        this.notSuccessfulModelName.add("all the models failed");
    }

    public ConfrenceInformation(String name, int date, List<Model> successfulModel) {
        this.name = name;
        this.date = date;
        this.successfulModel = successfulModel;
        successfulModelName();
    }

    private void successfulModelName() {
        for (Model model : successfulModel) {
            addModel(model);
        }
    }

    public void addModel(Model model) {
            successfulModel.add(model);

        }


    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }



    public boolean checkPublish() {
        this.counterTick++;
        if (this.counterTick == date) {

            return true;
        }
        return false;

    }

    public List<Model> getSuccessfulModel() {
        return successfulModel;
    }
}

