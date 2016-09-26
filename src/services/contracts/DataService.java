package services.contracts;

import com.google.api.client.http.HttpResponse;
import models.Kingdom;
import org.jdeferred.Promise;

import java.util.List;

public interface DataService {
    Promise<Void, Throwable, Object> acquire(Kingdom[] kingdoms);
    Promise<Void, Throwable, Object> saveData(List<HttpResponse> responses);
}
