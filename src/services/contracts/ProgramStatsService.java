package services.contracts;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Observer;

public interface ProgramStatsService {
    void resetAcquisitionTime();
    void addDate(ZonedDateTime date);
    void beginAcquisitionTimeEstimation();
    void endAcquisitionTimeEstimation();
    int getRemainingRequests();
    void setRemainingRequests(int number);
    void addObserver(Observer o);
}
