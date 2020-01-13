package uk.gov.hmcts.reform.mi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class TaskRunner {

    @Value("${fromDaysAgo}")
    private long fromDaysAgo;

    @Value("${toDaysAgo}")
    private long toDaysAgo;

    @Autowired
    private AzureBlobStoreAccessor azureBlobStoreAccessor;

    @Scheduled(cron = "0 0 5 * * MON")
    public void weekly() {
        System.out.println("Weekly Runner called");
        azureBlobStoreAccessor.getNotifyBlob(LocalDate.now().minusDays(fromDaysAgo), LocalDate.now().minusDays(toDaysAgo));
        azureBlobStoreAccessor.getEightByEightBlob(LocalDate.now().minusDays(fromDaysAgo), LocalDate.now().minusDays(toDaysAgo));
    }

    @Scheduled(cron = "0 0 */1 ? * *")
    public void healthCheck() {
        System.out.println("Scheduler is still alive");
    }
}
