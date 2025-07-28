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
import quiosque.db.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PainelControleRMI extends UnicastRemoteObject implements InterfacePedidos {
    private JTextArea logArea;
    private int kioskCounter = 0;
    private final List<Produto> cardapio;
    private final AtomicInteger orderIdGenerator;
    // Lista de tokens válidos para autenticação dos quiosques.
    private final List<String> validTokens = List.of("QUIOSQUE_01_TOKEN_SECRETO", "QUIOSQUE_02_TOKEN_SECRETO", "QUIOSQUE_03_TOKEN_SECRETO", "QUIOSQUE_04_TOKEN_SECRETO", "QUIOSQUE_05_TOKEN_SECRETO");

    // Construtor
    public PainelControleRMI() throws RemoteException {
        // Chama o construtor de UnicastRemoteObject, passando as factories para sockets SSL
        // Isso garante que a comunicação RMI será criptografada
        super(0, new SslRmiClientFactory(), new SslRmiServerFactory());
        this.cardapio = criarCardapio();

        int lastOrderId = DatabaseManager.getLastOrderId();
        this.orderIdGenerator = new AtomicInteger(lastOrderId + 1);
        System.out.println(">>> [Servidor RMI] Próximo ID de pedido será: " + orderIdGenerator.get());

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
        String sql = "SELECT nome, preco FROM produtos ORDER BY id";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String nome = rs.getString("nome");
                double preco = rs.getDouble("preco");
                menu.add(new Produto(nome, preco));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Em caso de falha, exibe um erro e fecha, pois o sistema não pode operar sem o cardápio.
            JOptionPane.showMessageDialog(null, "Erro fatal ao carregar o cardápio do banco de dados.", "Erro Crítico de DB", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }


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
        double total = itens.stream().mapToDouble(item -> item.produto().preco() * item.quantidade()).sum();
        String totalFormatado = String.format("R$ %.2f", total);

        String sqlPedido = "INSERT INTO pedidos (id, id_quiosque, valor_total) VALUES (?, ?, ?)";
        String sqlItem = "INSERT INTO itens_pedido (id_pedido, quantidade, id_produto) VALUES (?, ?, (SELECT id FROM produtos WHERE nome = ?))";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // Inicia uma transação

            // Insere o registro do pedido principal
            try (PreparedStatement pstmtPedido = conn.prepareStatement(sqlPedido)) {
                pstmtPedido.setInt(1, pedidoId);
                pstmtPedido.setInt(2, kioskId);
                pstmtPedido.setDouble(3, total);
                pstmtPedido.executeUpdate();
            }

            // Insere cada item do carrinho
            try (PreparedStatement pstmtItem = conn.prepareStatement(sqlItem)) {
                for (ItemPedido item : itens) {
                    pstmtItem.setInt(1, pedidoId);
                    pstmtItem.setInt(2, item.quantidade());
                    pstmtItem.setString(3, item.produto().nome());
                    pstmtItem.addBatch();
                }
                pstmtItem.executeBatch();
            }

            conn.commit(); // Confirma a transação se tudo deu certo

            // Log no painel do servidor
            addLog(String.format("===== NOVO PEDIDO #%d (do Quiosque %d) =====", pedidoId, kioskId));
            for (ItemPedido item : itens) {
                addLog(String.format("  - %dx %s", item.quantidade(), item.produto().nome()));
            }
            addLog("VALOR TOTAL: " + totalFormatado);
            addLog("=========================================");

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Erro interno ao salvar pedido no banco de dados.", e);
        }

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
        DatabaseManager.initializeDatabase();

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