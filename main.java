import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ProxyBridge {

    public static void main(String[] args) {
        // Configuration
        String host = "127.0.0.1";
        int port = 22;
        int listenPort = 8880;

        try {
            // Listen new bridged port
            ServerSocket serverSocket = new ServerSocket(listenPort);
            System.out.println("Server started on port: " + listenPort);
            System.out.println("Redirecting requests to: " + host + " at port " + port);

            // Looping
            while (true) {
                // Accept client connection
                Socket clientSocket = serverSocket.accept();
                System.out.println("Connection received from " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());

                // Create a new thread to handle the connection
                Thread thread = new Thread(() -> handleConnection(clientSocket, host, port));
                thread.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleConnection(Socket clientSocket, String host, int port) {
        try (
            // Connect to the target server
            Socket targetSocket = new Socket(host, port);
            // Get input and output streams for both client and target
            InputStream clientInput = clientSocket.getInputStream();
            OutputStream clientOutput = clientSocket.getOutputStream();
            InputStream targetInput = targetSocket.getInputStream();
            OutputStream targetOutput = targetSocket.getOutputStream()
        ) {
            // Return HTTP Response Switching Protocols to client
            String response = "HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n\r\n";
            clientOutput.write(response.getBytes());

            // Create threads to forward data in both directions
            Thread forwardClientToTarget = new Thread(() -> forwardData(clientInput, targetOutput));
            Thread forwardTargetToClient = new Thread(() -> forwardData(targetInput, clientOutput));

            // Start the threads
            forwardClientToTarget.start();
            forwardTargetToClient.start();

            // Wait for the threads to finish
            forwardClientToTarget.join();
            forwardTargetToClient.join();

            // Log output
            System.out.println("Connection terminated for " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void forwardData(InputStream input, OutputStream output) {
        byte[] buffer = new byte[1024];
        int bytesRead;
        try {
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
