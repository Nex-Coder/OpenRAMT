package Controller.Library.Services;

import Model.Task.TaskRequest;
import Model.Task.TaskResponse;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javax.net.ssl.*;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Objects;


/**
 * The task service for task work for the service, has many response codes defined by the server, here they are:
 * A (error) code referring to the success of the operation. The codes are as follows:
 * - 0 - Success without issue.
 * - 1 - Generic error where the request failed normally i.e. no results found or something *without* exception/fault.
 * - 2 - Parameters do not meet the require constraints of the database column, field or value.
 * - 3 - Parameter contained semantically immutable field such as root/default user in a delete query.
 * - 4 - Parameters required are invalid. This could be because of a null//incorrect value in request parameters.
 * - 10 - Username not found.
 * - 11 - Username found but incorrect password.
 * - 12 - User details verified but account is suspended
 * - 19 - User details verified but permissions are not satisfied (Unauthorised).
 * - 20 - An SQL exception was thrown that wasn't handled (correctly).
 * - 21 - Duplicate SQL error. When a value given would of violated a unique column for example.
 * - 31 - Process task related error, process restart attempted, killed but couldn't start again.
 * - 44 - Data given couldn't be found within the request i.e. row not found when updating a line in the database.
 * - 97 - Server wasn't launched with permissions (i.e. sudo, as admin...).
 * - 98 - Server doesn't support this task.
 * - 99 - Catastrophic generic error. If this has returned, something has gone seriously wrong (i.e. unforeseen bugs).
 */
public class TaskProgressiveService extends Service<TaskResponse<?>> {
    private TaskRequest<?> request;
    private TaskResponse<?> lastResponse;

    private final char[] ksPwd = "jknm43c23C1EW342we".toCharArray();

    /**
     * Constructs the service needed to request the tasks. users the information in the request to contact the server
     * and ask if we have permission, if every goes as expected then the task will be executed and the server will send
     * a response reflecting the state of the task. Read this classes response codes for more information on them.
     * @param request
     */
    public TaskProgressiveService(TaskRequest<?> request) {
        this.request = request;

        // For security with SSLSockets.
        System.setProperty("javax.net.ssl.trustStore","data/keystore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", String.valueOf(ksPwd));
    }

    public TaskRequest<?> getRequest() {
        return this.request;
    }
    public void setRequest(TaskRequest<?> request) {
        this.request = request;
    }

    /**
     * Updates the task and calls restart() in the service.
     *
     * @param request The new task to complete.
     * @return The request ID for convenience. Can be safely ignored.
     */
    public String updateAndRestart(TaskRequest<?> request) {
        this.request = request;
        this.restart();
        return request.getRequestID();
    }

    @Override
    protected Task<TaskResponse<?>> createTask() {
        return new Task<>() {

            @Override
            protected TaskResponse<?> call() throws IOException, UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException,  ClassNotFoundException {
                System.out.println("Starting client socket");
                progressUpdate(0.1f, "Starting");

                // Communications with server
                Socket socket = request.getUser().isSecure() ?  // Secure or plain socket.
                        sslGeneration() :
                        new Socket(request.getUser().getHost(), request.getUser().getPort());

                // Create Data Streams.
                progressUpdate(0.33f, "Connected");
                ObjectOutputStream socketOutput = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream socketInput = new ObjectInputStream(socket.getInputStream());

                // Send request to the server.
                progressUpdate(0.45f, "Sending Request");
                socketOutput.writeObject(request);

                // Get the response
                progressUpdate(0.66f, "Server Processing");
                TaskResponse<?> response = (TaskResponse<?>) socketInput.readObject();

                System.out.println("Response received: " + response.getRequestID() + " | "+
                        response.getRequest().getTask() + " - " +response.getResponse());

                //Stop Communications
                progressUpdate(0.85f, "Finishing");
                try { socket.close(); }catch(IOException ignored){}

                progressUpdate(1f, response.getResponse().toString());

                lastResponse = response;
                return response;
            }


            /**
             * Generates a certificate at runtime if (parts of) one are missing. Then returns a SSLSocket for use.
             * @return A Secure socket based upon either a generated keystore in the data folder or one imported there.
             */
            private Socket sslGeneration() throws IOException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException, KeyManagementException {
                File ksFile = new File("data/keystore.jks");

                if(!ksFile.isFile()) {
                    progressUpdate(0.125f, "Creating Cert");
                    byte[] in = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("Cert/keystore.jks")).readAllBytes();

                    ksFile.getParentFile().mkdirs();
                    ksFile.createNewFile();

                    progressUpdate(0.15f, "Saving Cert");
                    FileOutputStream out = new FileOutputStream("data/keystore.jks");
                    out.write(in);
                    out.close();
                }

                progressUpdate(0.2f, "Loading Cert");

                KeyStore ks = KeyStore.getInstance("JKS");
                ks.load(new FileInputStream(ksFile), ksPwd);


                KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                kmf.init(ks, ksPwd);

                TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
                tmf.init(ks);

                progressUpdate(0.3f, "Connecting");

                SSLContext sc = SSLContext.getInstance("TLS");

                TrustManager[] trustManagers = tmf.getTrustManagers();
                sc.init(kmf.getKeyManagers(), trustManagers, null);

                SSLSocketFactory ssf = sc.getSocketFactory();

                return ssf.createSocket(request.getUser().getHost(), request.getUser().getPort());

            }

            private void progressUpdate(float progress, String message) {
                updateProgress(progress, 1f);
                updateMessage("State: " + message);
            }
        };
    }

    /**
     * Getting the value of a socket causes it to be null, so as a fallback for any data this method can be used as a
     * last resort.
     * @return The last response received from the server.
     */
    public TaskResponse<?> getLastResponse() {
        return lastResponse;
    }
}
