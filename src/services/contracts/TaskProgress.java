package services.contracts;

import java.util.concurrent.atomic.AtomicInteger;

public class TaskProgress {
    private AtomicInteger total;
    private AtomicInteger progress;
    private Step step;
    private String message;

    public enum Step {
        KingdomsCreation, DirectoriesCreationFinished, OrganismTreatment, OrganismProcessing, KingdomGathering
    }

    public TaskProgress() {
        this.total = new AtomicInteger();
        this.progress = new AtomicInteger();
        this.step = null;
        this.message = null;
    }

    public Step getStep() {
        return step;
    }

    public void setStep(Step step) {
        this.step = step;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public AtomicInteger getTotal() {
        return total;
    }

    public AtomicInteger getProgress() {
        return progress;
    }

    public String getMessage() {
        return message;
    }
}
