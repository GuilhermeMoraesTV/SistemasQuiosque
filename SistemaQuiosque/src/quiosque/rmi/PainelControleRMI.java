package quiosque.rmi;

import quiosque.model.ItemPedido;
import quiosque.model.Produto;
import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class PainelControleRMI extends UnicastRemoteObject implements InterfacePedidos {
    private JTextArea logArea;
    private int kioskCounter = 0;
    private final List<Produto> cardapio;
    private final AtomicInteger orderIdGenerator = new AtomicInteger(1000);
    // Lista de tokens válidos para autenticação dos quiosques.
    private final List<String> validTokens = List.of("QUIOSQUE_01_TOKEN_SECRETO", "QUIOSQUE_02_TOKEN_SECRETO", "QUIOSQUE_03_TOKEN_SECRETO", "QUIOSQUE_04_TOKEN_SECRETO", "QUIOSQUE_05_TOKEN_SECRETO");

    // Construtor
    public PainelControleRMI() throws RemoteException {
        // Chama o construtor de UnicastRemoteObject, passando as factories para sockets SSL
        // Isso garante que a comunicação RMI será criptografada
        super(0, new SslRmiClientFactory(), new SslRmiServerFactory());
        this.cardapio = criarCardapio();


        JFrame frame = new JFrame("Painel de Controle - Servidor RMI SEGURO");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);
        frame.setLayout(new BorderLayout());
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        JButton launchKioskButton = new JButton("Abrir Novo Quiosque");
        frame.add(launchKioskButton, BorderLayout.SOUTH);

        launchKioskButton.addActionListener(e -> {
            kioskCounter++;
            new QuiosqueJanelaRMI(kioskCounter);
        });

        frame.setVisible(true);
        startServer();
    }

    public static void setupSslContext() {
        try {
            char[] password = "123456".toCharArray();
            KeyStore ts = KeyStore.getInstance("JKS");
            // Carrega o keystore do servidor, que contém o certificado
            var keystoreStream = PainelControleRMI.class.getResourceAsStream("/server.keystore");
            ts.load(keystoreStream, password);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ts);

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, tmf.getTrustManagers(), null);

            // Define este como o contexto SSL padrão para toda a aplicação
            SSLContext.setDefault(sc);
        } catch (Exception e) {
            System.exit(1);
        }
    }

    //Lista de produtos
    private List<Produto> criarCardapio() {
        List<Produto> menu = new ArrayList<>();
        menu.add(new Produto("Hambúrguer Clássico", 25.50));
        menu.add(new Produto("X-Bacon Supremo", 29.00));
        menu.add(new Produto("Batata Frita (M)", 12.00));
        menu.add(new Produto("Anéis de Cebola", 15.00));
        menu.add(new Produto("Refrigerante Lata", 7.00));
        menu.add(new Produto("Milkshake de Morango", 18.50));
        return menu;
    }

    private void addLog(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    @Override
    public List<Produto> getCardapio() throws RemoteException {
        return this.cardapio;
    }

    // Implementação do método remoto que processa um pedido
    @Override
    public String fazerPedido(String authToken, int kioskId, List<ItemPedido> itens) throws RemoteException, SecurityException {
        if (authToken == null || !validTokens.contains(authToken)) {
            addLog(String.format(" TENTATIVA DE ACESSO NEGADO do Quiosque %d (Token inválido)", kioskId));
            throw new SecurityException("Token de autenticação inválido ou ausente!");
        }
        int pedidoId = orderIdGenerator.getAndIncrement();
        addLog(String.format("===== NOVO PEDIDO #%d (do Quiosque %d) =====", pedidoId, kioskId));
        double total = 0.0;
        for (ItemPedido item : itens) {
            addLog(String.format("  - %dx %s", item.quantidade(), item.produto().nome()));
            total += item.produto().preco() * item.quantidade();
        }
        String totalFormatado = String.format("R$ %.2f", total);
        addLog("VALOR TOTAL: " + totalFormatado);
        addLog("=========================================");
        return String.format("Pedido #%d confirmado! Valor total: %s", pedidoId, totalFormatado);
    }

    private void startServer() {
        try {
            // Cria um registro RMI na porta 1099, protegido com SSL
            Registry registry = LocateRegistry.createRegistry(1099, new SslRmiClientFactory(), new SslRmiServerFactory());
            registry.bind("ServicoPedidos", this);
            addLog(" Servidor RMI SEGURO (TLS) pronto e registrado.");
            addLog("-------------------------------------------");
        } catch (Exception e) {
            String errorMessage = " Erro fatal no servidor RMI.";
            addLog(errorMessage);
            JOptionPane.showMessageDialog(null, errorMessage, "Erro Crítico", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        // Configura a segurança SSL antes de qualquer coisa.
        setupSslContext();

        SwingUtilities.invokeLater(() -> {
            try {
                new PainelControleRMI();
            } catch (RemoteException e) {
                JOptionPane.showMessageDialog(null, "Nao foi possivel criar o objeto remoto RMI.", "Erro RMI", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}