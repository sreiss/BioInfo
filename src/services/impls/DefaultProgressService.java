package services.impls;

import services.contracts.ProgressService;
import services.contracts.TaskProgress;
import java.util.Observable;
import java.util.Observer;

public class DefaultProgressService extends Observable implements ProgressService {
    private TaskProgress currentProgress = new TaskProgress();

    @Override
    public TaskProgress getCurrentProgress() {
        return currentProgress;
    }

    @Override
    public void invalidateProgress() {
        setChanged();
        notifyObservers(currentProgress);
    }

    @Override
    public synchronized void addObserver(Observer o) {
        super.addObserver(o);
    }
}
