package services.contracts;

import java.util.Observer;

public interface ProgressService {
    void invalidateApiStatus();

    void addObserver(Observer o);
    TaskProgress getCurrentProgress();
    DownloadTaskPogress getCurrentDownloadProgress();
    ApiStatus getCurrentApiStatus();
    void invalidateProgress();
    void invalidateDownloadProgress();
}
