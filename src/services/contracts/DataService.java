package services.contracts;

import org.jdeferred.Promise;
import org.jdeferred.multiple.MasterProgress;
import org.jdeferred.multiple.MultipleResults;
import org.jdeferred.multiple.OneReject;

public interface DataService {
    Promise<Void, Throwable, MasterProgress> acquire();
    Promise<Void, Throwable, MasterProgress> saveData();
}
