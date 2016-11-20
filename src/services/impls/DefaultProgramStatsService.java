package services.impls;

import com.google.inject.Inject;
import services.contracts.ProgramStat;
import services.contracts.ProgramStatsService;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultProgramStatsService extends Observable implements ProgramStatsService {
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture currentFuture;
    private int remainingRequests;

    private List<ZonedDateTime> requestDates = new ArrayList<>();

    @Inject
    public DefaultProgramStatsService(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void resetAcquisitionTime() {
        requestDates = new ArrayList<>();
    }

    @Override
    public void addDate(ZonedDateTime date) {
        requestDates.add(date);
        if (requestDates.size() > 100) {
            requestDates.remove(0);
        }
    }

    @Override
    public void beginAcquisitionTimeEstimation() {
        currentFuture = scheduler.scheduleAtFixedRate(() -> {
            int j;
            ZonedDateTime zonedDateTime1;
            ZonedDateTime zonedDateTime2;
            Duration duration;
            long totalMilliSeconds = 0;
            for (int i = 0; i < requestDates.size(); i++) {
                if ((j = i+1) <= requestDates.size() - 1) {
                    zonedDateTime1 = requestDates.get(i);
                    zonedDateTime2 = requestDates.get(j);
                    duration = Duration.between(zonedDateTime1, zonedDateTime2);
                    totalMilliSeconds += duration.toMillis();
                }
            }
            long averageMilliSecondes = totalMilliSeconds / requestDates.size();
            long totalRemaining = averageMilliSecondes * remainingRequests;
            System.out.println(totalRemaining);
            ProgramStat programStat = new ProgramStat();
            programStat.setTimeRemaining(totalRemaining);
            setChanged();
            notifyObservers(programStat);
            requestDates.clear();
        }, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public void endAcquisitionTimeEstimation() {
        if (currentFuture != null) {
            currentFuture.cancel(true);
        }
    }

    public int getRemainingRequests() {
        return remainingRequests;
    }

    public void setRemainingRequests(int remainingRequests) {
        this.remainingRequests = remainingRequests;
    }
}
