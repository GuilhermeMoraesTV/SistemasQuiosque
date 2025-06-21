package quiosque.rmi;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.rmi.server.RMIServerSocketFactory;
import java.security.KeyStore;

public class SslRmiServerFactory implements RMIServerSocketFactory, Serializable {
    @Override
    public ServerSocket createServerSocket(int port) throws IOException {
        try {
            char[] password = "123456".toCharArray();
            KeyStore ks = KeyStore.getInstance("JKS");
            var keystoreStream = SslRmiServerFactory.class.getResourceAsStream("/server.keystore");
            ks.load(keystoreStream, password);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, password);

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(kmf.getKeyManagers(), null, null);

            SSLServerSocketFactory factory = context.getServerSocketFactory();
            SSLServerSocket socket = (SSLServerSocket) factory.createServerSocket(port);
            return socket;
        } catch (Exception e) {
            throw new IOException("Erro ao criar SSL server socket", e);
        }
    }
}