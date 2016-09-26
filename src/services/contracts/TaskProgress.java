package services.contracts;

public class TaskProgress {
    private final int total;
    private int progress;

    public TaskProgress(int total) {
        this.total = total;
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
}
