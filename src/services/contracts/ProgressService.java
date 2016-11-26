package services.contracts;

import java.util.Observer;

public interface ProgressService {
    void addObserver(Observer o);
    TaskProgress getCurrentProgress();
    TaskProgress getCurrentDownloadProgress();
    void invalidateProgress();
    void invalidateDownloadProgress();
}
