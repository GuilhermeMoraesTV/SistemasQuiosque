package quiosque.rmi;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;

public class SslRmiClientFactory implements RMIClientSocketFactory, Serializable {
    @Override
    public Socket createSocket(String host, int port) throws IOException {
        try {
            SSLContext context = SSLContext.getDefault();
            SSLSocketFactory factory = context.getSocketFactory();
            SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
            return socket;
        } catch (Exception e) {
            throw new IOException("Erro ao criar SSL client socket", e);
        }
    }
}