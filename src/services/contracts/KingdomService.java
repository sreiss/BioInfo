package services.contracts;

import com.google.api.client.http.HttpResponse;
import models.Kingdom;
import org.jdeferred.Promise;
import org.jdeferred.multiple.MasterProgress;
import org.jdeferred.multiple.MultipleResults;
import org.jdeferred.multiple.OneReject;

public interface KingdomService {
    Promise<Void, Throwable, Void> createKingdomTree(Kingdom kingdom, HttpResponse response);
}
