package services.contracts;

import com.google.api.client.http.HttpResponse;
import models.Kingdom;
import models.Organism;
import org.jdeferred.Promise;

import java.io.InputStream;
import java.util.List;

public interface KingdomService {
    Promise<List<Organism>, Throwable, Object> createKingdomTree(Kingdom kingdom, InputStream inputStream);
}
