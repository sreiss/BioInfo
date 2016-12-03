package services.impls;

import services.contracts.ApiStatus;
import services.contracts.DownloadTaskPogress;
import services.contracts.ProgressService;
import services.contracts.TaskProgress;
import java.util.Observable;
import java.util.Observer;

public class DefaultProgressService extends Observable implements ProgressService {
    private TaskProgress currentProgress = new TaskProgress();
    private DownloadTaskPogress currentDownloadProgress = new DownloadTaskPogress();
    private ApiStatus currentApiStatus = new ApiStatus();

    @Override
    public TaskProgress getCurrentProgress() {
        return currentProgress;
    }

    @Override
    public DownloadTaskPogress getCurrentDownloadProgress() { return currentDownloadProgress; }

    @Override
    public ApiStatus getCurrentApiStatus() { return currentApiStatus; };

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
    public void invalidateApiStatus() {
        setChanged();
        notifyObservers(currentApiStatus);
    }

    @Override
    public synchronized void addObserver(Observer o) {
        super.addObserver(o);
    }
}
