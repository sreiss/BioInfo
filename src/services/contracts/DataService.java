package services.contracts;

import models.Kingdom;
import org.jdeferred.Promise;
import org.jdeferred.multiple.MasterProgress;
import org.jdeferred.multiple.MultipleResults;
import org.jdeferred.multiple.OneReject;

public interface DataService {
    Promise<Void, Throwable, Void> acquire(Kingdom[] kingdoms);
    Promise<Void, Throwable, Void> saveData();
}
