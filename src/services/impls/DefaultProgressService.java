package services.impls;

import services.contracts.DownloadTaskPogress;
import services.contracts.ProgressService;
import services.contracts.TaskProgress;
import java.util.Observable;
import java.util.Observer;

public class DefaultProgressService extends Observable implements ProgressService {
    private TaskProgress currentProgress = new TaskProgress();
    private DownloadTaskPogress currentDownloadProgress = new DownloadTaskPogress();

    @Override
    public TaskProgress getCurrentProgress() {
        return currentProgress;
    }

    @Override
    public DownloadTaskPogress getCurrentDownloadProgress() { return currentDownloadProgress; }

    @Override
    public void invalidateProgress() {
        setChanged();
        notifyObservers(currentProgress);
    }

    @Override
    public void invalidateDownloadProgress() {
        setChanged();
        notifyObservers(currentDownloadProgress);
    }

    @Override
    public synchronized void addObserver(Observer o) {
        super.addObserver(o);
    }
}
