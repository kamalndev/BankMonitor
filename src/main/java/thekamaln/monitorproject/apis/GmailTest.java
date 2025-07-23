package thekamaln.monitorproject.apis;

public class GmailTest {
    public static void main(String[] args) throws Exception {
        GmailAuthentication auth = new GmailAuthentication();
        String code = auth.fetchLatestCode();

        System.out.println("The code is " + code);
    }

}
