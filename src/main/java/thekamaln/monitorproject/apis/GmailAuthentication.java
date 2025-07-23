package thekamaln.monitorproject.apis;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.ModifyMessageRequest;
import org.springframework.stereotype.Component;


import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
public class GmailAuthentication {
    private static final String APPLICATION_NAME = "Bank Monitor";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    // .modify allows read and write access
    private static final List<String> SCOPES = Collections.singletonList(
            "https://www.googleapis.com/auth/gmail.modify"
    );

    private static final String CREDENTIALS_FILE_PATH = "/credentials/googleCredentials.json";

    private final Gmail gmailService;

    public GmailAuthentication() throws Exception {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        // loads from credentials json
        InputStream in = GmailAuthentication.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                JSON_FACTORY, new InputStreamReader(in)
        );


        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES
        )
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File("tokens")))
                .setAccessType("offline")
                .build();

        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()
        ).authorize("user");

        // 3) Construct the Gmail client
        gmailService = new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Polls Gmail for the newest unread message from Nusenda and extracts
     * the first 6-digit code it finds, then marks that message as read.
     *
     * @return a 6-digit code string, or null if none found
     */
    public String fetchLatestCode() throws Exception {
        // Search for unread messages from the bank in the last 2 minutes
        ListMessagesResponse listResponse = gmailService.users().messages()
                .list("me")
                .setQ("from:noreply@nusenda.org newer_than:10m is:unread")
                .setMaxResults(1L)
                .execute();

        List<Message> messages = listResponse.getMessages();
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        // Fetch the full message payload
        Message msg = gmailService.users().messages()
                .get("me", messages.get(0).getId())
                .setFormat("full")
                .execute();

        // Gmail payloads can nest parts; handle both inline and multipart
        String body = null;
        if (msg.getPayload().getParts() != null && !msg.getPayload().getParts().isEmpty()) {
            body = new String(msg.getPayload().getParts()
                    .get(0).getBody().decodeData());
        } else if (msg.getPayload().getBody() != null) {
            body = new String(msg.getPayload().getBody().decodeData());
        }

        if (body == null) {
            return null;
        }

        // Extract the first 6-digit sequence
        Matcher m = Pattern.compile("\\b(\\d{6})\\b").matcher(body);
        if (m.find()) {
            // Mark as read so we don’t pick it again
            gmailService.users().messages().modify(
                    "me",
                    msg.getId(),
                    new ModifyMessageRequest()
                            .setRemoveLabelIds(Collections.singletonList("UNREAD"))
            ).execute();

            return m.group(1);
        }
        return null;
    }
}
