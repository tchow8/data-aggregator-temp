package uk.gov.hmcts.reform.mi;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.MSICredentials;
import com.microsoft.azure.keyvault.KeyVaultClient;
import org.springframework.stereotype.Component;

@Component
public class KeyVaultAccessor {

    private static final String VAULT_URL = "https://mikeyvault-p1.vault.azure.net";
    private static final String SECRET_NAME = "mi-staging-storage-token-key";
    private static final String OUTPUT_SECRET_NAME = "mi-timdaexedatastore-token";

    public static String getConnectionStringForInput() {
        MSICredentials credentials = new MSICredentials(AzureEnvironment.AZURE);
        KeyVaultClient kvc = new KeyVaultClient(credentials);

        String secretUrl = VAULT_URL + "/secrets/" + SECRET_NAME;

        System.out.println("Getting Secret");

        return kvc.getSecret(secretUrl).value();
    }

    public static String getConnectionStringForOutput() {
        MSICredentials credentials = new MSICredentials(AzureEnvironment.AZURE);
        KeyVaultClient kvc = new KeyVaultClient(credentials);

        String secretUrl = VAULT_URL + "/secrets/" + OUTPUT_SECRET_NAME;

        System.out.println("Getting Output Secret");

        return kvc.getSecret(secretUrl).value();
    }
}