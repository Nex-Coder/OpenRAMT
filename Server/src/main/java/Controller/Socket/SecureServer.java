package Controller.Socket;

import Controller.RAMTAlert;
import javafx.scene.control.Alert;
import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.BindException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Objects;

public class SecureServer implements Runnable {
    protected int port = 3069; // Default
    protected boolean isStopped = false;

    protected Thread runningThread = null;

    protected static File ksFile = new File("data/keystore.jks");

    protected SSLServerSocket serverSocket;

    /**
     * Creates a server to accept tasks from clients with the default port. Doesn't extend PLainServer but does mostly
     * the same thing.
     */
    public SecureServer() { // default (use default port)
        try {
            serverSocket = initialisation(port);
        } catch (BindException e) {
            new RAMTAlert(Alert.AlertType.ERROR,
                    "OpenRAMT Startup Error.",
                    "The port assigned to the server is already taken.",
                    "This can be because two servers are running at once (on the same port) so please " +
                            "try closing the other program or wiping this application to choose a different port." +
                            "\n\n This program will now be exiting.").showAndWait();
            System.exit(-1);
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | KeyManagementException e) {
            e.printStackTrace();
        }
    }

    /**
     * Accepts a custom port to start a server to accept tasks from clients. Doesn't extend PLainServer but does mostly
     * the same thing.
     * @param port the port to run the server on.
     */
    public SecureServer(int port) {
        this.port = port;

        try {
            serverSocket = initialisation(port);
        } catch (BindException e) {
            new RAMTAlert(Alert.AlertType.ERROR,
                    "OpenRAMT Startup Error.",
                    "The port assigned to the server is already taken.",
                    "This can be because two servers are running at once (on the same port) so please " +
                            "try closing the other program or wiping this application to choose a different port." +
                            "\n\n This program will now be exiting.").showAndWait();
            System.exit(-1);
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | KeyManagementException e) {
            e.printStackTrace();
        }
    }

    /**
     * Runs a server that will start a new thread per incoming client request.
     */
    @Override
    public void run() {
        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }

        while (!isStopped()) {
            SSLSocket clientSocket;

            try {
                clientSocket = (SSLSocket) this.serverSocket.accept();
            } catch (IOException e) {
                if (isStopped()) {
                    System.out.println("Server Stopped.");
                    return;
                }
                throw new RuntimeException("Error accepting client connection", e);
            }

            new Thread(new ServerWorker(clientSocket), "Client Socket Thread "+clientSocket.toString()).start();
        }

        System.out.println("Server Stopped.");
    }

    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop() {
        this.isStopped = true;
        try {
            if (this.serverSocket != null) {
                this.serverSocket.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);

        }
    }

    /**
     * Creates our keystore file which is used to create a certificate for our secure server connection.
     *
     * @throws IOException               If the file doesn't exist then this exception will be thrown or
     * @throws BindException             If the port or server couldn't be opened when creating server.
     * @throws KeyStoreException         If the KeyStore file most likely doesn't contain correct keys. Can also be thrown for generic errors.
     * @throws NoSuchAlgorithmException  If the configured algorithm cannot be found then this exception is thrown.
     * @throws UnrecoverableKeyException Thrown when the key cannot be extracted from the keystore.
     * @throws CertificateException      This will only be thrown if the certificate is not valid.
     * @throws KeyManagementException    If keystore fails generally in the processes here then this exception is thrown.
     */
    public static SSLServerSocket initialisation(int port) throws IOException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException, KeyManagementException {
        char[] ksPwd = "jknm43c23C1EW342we".toCharArray();

        if (!ksFile.isFile()) {
            Files.copy(Objects.requireNonNull(SecureServer.class.getClassLoader().getResourceAsStream("Cert/keystore.jks")), Paths.get("data/keystore.jks"), StandardCopyOption.REPLACE_EXISTING);
        }

        System.out.println(ksFile.exists() + " | " + ksFile.getAbsolutePath() + " | " + ksFile.canRead() + " | " + ksFile.length());

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(ksFile), ksPwd);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, ksPwd);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext sc = SSLContext.getInstance("TLS");
        TrustManager[] trustManagers = tmf.getTrustManagers();
        sc.init(kmf.getKeyManagers(), trustManagers, null);

        SSLServerSocketFactory ssf = sc.getServerSocketFactory();
        return (SSLServerSocket) ssf.createServerSocket(port);
    }
}