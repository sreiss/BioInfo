package services.contracts;

import com.google.api.client.http.HttpResponse;
import models.Kingdom;
import org.jdeferred.Promise;

public interface KingdomService {
    Promise<Void, Throwable, Void> createKingdomTree(Kingdom kingdom, HttpResponse response);
}
