package services.contracts;

public class TaskProgress {
    private final int total;
    private int progress;
    private String text;
    private Type type;
    private Step step;

    public enum Step {
        DirectoriesCreationFinished
    }

    public enum Type {
        Text,
        Number
    }

    public TaskProgress(int total) {
        this.total = total;
        this.type = type;
    }

    public TaskProgress(int total, Type type, Step step) {
        this.total = total;
        this.type = type;
        this.step = step;
    }

    public int getTotal() {
        return total;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public Type getType() {
        return type;
    }

    public Step getStep() {
        return step;
    }

    public void setStep(Step step) {
        this.step = step;
    }
}
