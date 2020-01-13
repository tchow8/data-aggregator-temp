package uk.gov.hmcts.reform.mi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.Objects;

@Component
public class TaskRunner {

    @Value("${fromDaysAgo}")
    private String fromDaysAgo;

    @Value("${toDaysAgo}")
    private String toDaysAgo;

    @Autowired
    private AzureBlobStoreAccessor azureBlobStoreAccessor;

    @PostConstruct
    public void runTask() {
        if (Objects.nonNull(fromDaysAgo)) {
            long checkedToDaysAgo = Objects.nonNull(toDaysAgo) ? Long.parseLong(toDaysAgo) : 0l;
            azureBlobStoreAccessor.getNotifyBlob(LocalDate.now().minusDays(Long.parseLong(fromDaysAgo)), LocalDate.now().minusDays(checkedToDaysAgo));
            azureBlobStoreAccessor.getEightByEightBlob(LocalDate.now().minusDays(Long.parseLong(fromDaysAgo)), LocalDate.now().minusDays(checkedToDaysAgo));
        }
    }

    @Scheduled(cron = "0 0 5 * * MON")
    public void weekly() {
        System.out.println("Weekly Runner called");
        azureBlobStoreAccessor.getNotifyBlob(LocalDate.now().minusDays(7l), LocalDate.now());
        azureBlobStoreAccessor.getEightByEightBlob(LocalDate.now().minusDays(7l), LocalDate.now());
    }

    @Scheduled(cron = "0 0 */1 ? * *")
    public void healthCheck() {
        System.out.println("Scheduler is still alive");
    }
}
