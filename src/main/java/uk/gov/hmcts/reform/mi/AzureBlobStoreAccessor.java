package uk.gov.hmcts.reform.mi;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.security.InvalidKeyException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class AzureBlobStoreAccessor {

    private static final String LOCAL_PATH = FileSystems.getDefault().getPath(".").toAbsolutePath().toString();
    private static final String TEMP_PATH = LOCAL_PATH + "/notifytemp/";
    private static final String DATA_PATH = LOCAL_PATH + "/notifydata/";

    private static final String EXTENSION = ".csv";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter OUTPUT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSZZZZZ");

    private static final String EXTRACTION_DATE_COLUMN_NAME = "extraction_date";

    Logger logger = LoggerFactory.getLogger(AzureBlobStoreAccessor.class);

    List<String> activeNotifyServices = Stream.of(
        "notifystagingcmc",
        "notifystagingdivorce",
        "notifystagingprobate",
        "notifystagingsscs",
        "notifystagingtidam"
    ).collect(Collectors.toList());

    String eightByEightService = "exestaging";

    private void cleanLocalStorage() {
        logger.info("Preparing Local Storage.");

        File tempDir = new File(TEMP_PATH);
        File dataDir = new File(DATA_PATH);

        tempDir.mkdirs();
        dataDir.mkdirs();

        for (File file : tempDir.listFiles()) {
            file.delete();
        }

        for (File file : dataDir.listFiles()) {
            file.delete();
        }

        logger.info("Local Storage Preparation Complete.");
    }

    private void generateBlob(LocalDate fromDate, LocalDate toDate, List<String> containerNames, String outputName, boolean shouldSort) {
        cleanLocalStorage();

        try {
            CloudBlobClient cloudBlobClient = CloudStorageAccount.parse(KeyVaultAccessor.getConnectionStringForInput()).createCloudBlobClient();

            String resultPath = outputName + fromDate.minusDays(1l).format(DATE_FORMAT) + "-" + toDate.minusDays(1l).format(DATE_FORMAT) + EXTENSION;

            logger.info("Writing to file: " + resultPath);

            FileWriter fileWriter = new FileWriter(DATA_PATH + resultPath);
            CSVWriter writer = new CSVWriter(fileWriter,
                ICSVWriter.DEFAULT_SEPARATOR, ICSVWriter.DEFAULT_QUOTE_CHARACTER, ICSVWriter.DEFAULT_ESCAPE_CHARACTER, ICSVWriter.DEFAULT_LINE_END);

            boolean hasHeaderBeenWritten = false;

            List<String[]> body = new ArrayList<>();

            for (String containerName : containerNames) {
                logger.info("Getting Blob Container " + containerName);
                CloudBlobContainer cloudBlobContainer = cloudBlobClient.getContainerReference(containerName);
                logger.info("Blob Container Retrieved");

                for (ListBlobItem blobItem : cloudBlobContainer.listBlobs()) {
                    CloudBlockBlob blob = (CloudBlockBlob) blobItem;

                    LocalDate blobDate = LocalDate.parse(blob.getName(), DATE_FORMAT);

                    if (blobDate.isAfter(fromDate.minusDays(1)) && blobDate.isBefore(toDate.plusDays(1))) {
                        String tempOutputName = TEMP_PATH + containerName + blob.getName() + EXTENSION;
                        String createdDate = blob.getProperties().getCreatedTime().toInstant().atZone(ZoneOffset.UTC)
                            .toLocalDateTime().format(OUTPUT_DATE_FORMAT);

                        blob.downloadToFile(tempOutputName);

                        FileReader fileReader = new FileReader(tempOutputName);
                        CSVReader reader = new CSVReader(fileReader);

                        String header[] = reader.readNext();
                        if (!hasHeaderBeenWritten) {
                            writer.writeNext(ArrayUtils.insert(0, header, EXTRACTION_DATE_COLUMN_NAME));
                            hasHeaderBeenWritten = true;
                        }

                        String[] nextLine;
                        while ((nextLine = reader.readNext()) != null) {
                            if (shouldSort) {
                                body.add(ArrayUtils.insert(0, nextLine, createdDate));
                            } else {
                                writer.writeNext(ArrayUtils.insert(0, nextLine, createdDate));
                                writer.flush();
                            }
                        }

                        reader.close();
                    }
                }
            }

            if (shouldSort) {
                try {
                    // Hardcoded column to sort by for the time being
                    body.sort(Comparator.comparing(strings -> LocalDateTime.parse(strings[9], TIMESTAMP_FORMAT)));
                    writer.writeAll(body);
                    writer.flush();
                } catch (Exception e) {
                   logger.error("Exception has occurred when sorting data: ", e);
                   throw new RuntimeException(e);
                }
            }

            writer.close();

            String zipPath = resultPath.replaceFirst(".csv", ".zip");
            ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(DATA_PATH + zipPath));

            FileInputStream fileInputStream = new FileInputStream(DATA_PATH + resultPath);
            zipOutputStream.putNextEntry(new ZipEntry(resultPath));

            byte[] buffer = new byte[1024];
            int length;

            while((length = fileInputStream.read(buffer)) > 0) {
                zipOutputStream.write(buffer, 0, length);
            }

            fileInputStream.close();

            zipOutputStream.closeEntry();
            zipOutputStream.close();

            CloudBlobClient outputCloudBlobClient = CloudStorageAccount.parse(KeyVaultAccessor.getConnectionStringForOutput()).createCloudBlobClient();
            CloudBlobContainer containerClient = outputCloudBlobClient.getContainerReference(LocalDate.now().getMonth().name().toLowerCase());

            containerClient.createIfNotExists();

            containerClient.getBlockBlobReference(zipPath).uploadFromFile(DATA_PATH + zipPath);

            cleanLocalStorage();

        } catch (URISyntaxException | InvalidKeyException | IOException | StorageException | CsvValidationException e) {
            logger.error("Exception has occurred.", e);
            throw new RuntimeException("Something went wrong.");
        }
    }

    public void getNotifyBlob(LocalDate fromDate, LocalDate toDate) {
        generateBlob(fromDate, toDate, activeNotifyServices, "NotifyAggregate", true);
    }

    public void getEightByEightBlob(LocalDate fromDate, LocalDate toDate) {
        generateBlob(fromDate, toDate, Collections.singletonList(eightByEightService), "EightByEightAggregate", false);
    }

}
