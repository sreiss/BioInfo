package services.contracts;

public class TaskProgress {
    private final int total;
    private int progress;
    private String text;
    private Type type;
    private enum Type {
        Text,
        Number
    }

    public TaskProgress(int total) {
        this.total = total;
        this.type = type;
    }

    public TaskProgress(int total, Type type) {
        this.total = total;
        this.type = type;
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

}
