package services.contracts;

import java.util.Observer;

public interface ProgressService {
    void addObserver(Observer o);
    TaskProgress getCurrentProgress();
    void invalidateProgress();
}
