package services.impls;

import com.google.inject.Inject;
import models.Organism;
import org.jdeferred.DeferredCallable;
import org.jdeferred.DeferredManager;
import org.jdeferred.Promise;
import services.contracts.OrganismService;

import java.util.Date;
import java.util.concurrent.Callable;

public class DefaultOrganismService implements OrganismService {
    private final DeferredManager deferredManager;

    @Inject
    public DefaultOrganismService(DeferredManager deferredManager) {
        this.deferredManager = deferredManager;
    }

    @Override
    public Promise<Organism, Throwable, Object> createOrganism(final String name, final String group, final String subGroup, final Date updateDate, final String[] geneIds, final String kingdomId) {
        return deferredManager.when(new DeferredCallable<Organism, Object>() {
            @Override
            public Organism call() throws Exception {
                return new Organism(name, group, subGroup, updateDate, geneIds, kingdomId);
            }
        });
    }
}
