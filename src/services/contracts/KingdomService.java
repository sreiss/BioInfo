package services.contracts;

import com.google.api.client.http.HttpResponse;
import models.Kingdom;
import org.jdeferred.Promise;

import java.io.InputStream;

public interface KingdomService {
    Promise<Void, Throwable, Object> createKingdomTree(Kingdom kingdom, InputStream inputStream);
}
