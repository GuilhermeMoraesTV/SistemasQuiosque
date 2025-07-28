package quiosque.socket;

import quiosque.db.DatabaseManager;
import quiosque.model.Produto;
import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class PainelControleSocket {
    private JTextArea logArea;
    private int kioskCounter = 0;
    private final List<Produto> cardapio;
    private final AtomicInteger orderIdGenerator;

    // Construtor
    public PainelControleSocket() {

        this.cardapio = carregarCardapioDoBanco();

        int lastOrderId = DatabaseManager.getLastOrderId();
        this.orderIdGenerator = new AtomicInteger(lastOrderId + 1);
        System.out.println(">>> [Servidor Socket] Próximo ID de pedido será: " + orderIdGenerator.get());


        // Janela principal do servidor (sem alterações aqui)
        JFrame frame = new JFrame("Painel de Controle - Servidor Socket");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);
        frame.setLayout(new BorderLayout());

        // Area de Log (sem alterações aqui)
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        // Botão para iniciar novos clientes (sem alterações aqui)
        JButton launchKioskButton = new JButton("Abrir Novo Quiosque");
        frame.add(launchKioskButton, BorderLayout.SOUTH);

        launchKioskButton.addActionListener(e -> {
            kioskCounter++;
            new QuiosqueJanelaSocket(kioskCounter);
        });

        frame.setVisible(true);
        startServer();
    }

    /**
     * NOVO MÉTODO: Carrega a lista de produtos (cardápio) do banco de dados.
     */
    private List<Produto> carregarCardapioDoBanco() {
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
            JOptionPane.showMessageDialog(null, "Erro fatal ao carregar o cardápio do banco de dados.", "Erro Crítico de DB", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        return menu;
    }

    // Adiciona uma mensagem ao log da UI de forma segura para threads (sem alterações aqui)
    private void addLog(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    // Inicia uma nova Thread para o servidor não bloquear a UI (sem alterações aqui)
    private void startServer() {
        new Thread(() -> {

            try (ServerSocket serverSocket = new ServerSocket(12345)) {
                addLog(" Servidor Socket iniciado na porta 12345");
                addLog("-------------------------------------------");
                // Loop infinito para aceitar novas conexões de clientes
                while (true) {
                    Socket clientSocket = serverSocket.accept(); // Aguarda uma conexão.
                    // Para cada cliente, cria uma nova Thread para tratá-lo
                    new ClientHandler(clientSocket, this::addLog, this.cardapio, this.orderIdGenerator).start();
                }
            } catch (IOException e) {
                addLog(" Erro fatal no servidor.");
            }
        }).start();
    }

    /**
     * CLASSE INTERNA MODIFICADA: ClientHandler agora salva os pedidos no banco de dados.
     */
    private static class ClientHandler extends Thread {
        private final Socket clientSocket;
        private final Consumer<String> logger;
        private final List<Produto> cardapio;
        private final AtomicInteger orderIdGenerator;

        public ClientHandler(Socket socket, Consumer<String> logger, List<Produto> cardapio, AtomicInteger orderIdGenerator) {
            this.clientSocket = socket;
            this.logger = logger;
            this.cardapio = cardapio;
            this.orderIdGenerator = orderIdGenerator;
        }

        public void run() {

            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                String dadosRecebidos = in.readLine();
                if (dadosRecebidos == null) return;

                // 1. Processa a string do pedido
                String[] partes = dadosRecebidos.split(";");
                String kioskIdStr = partes[0].replace("ID:", "");
                int kioskId = Integer.parseInt(kioskIdStr);

                int pedidoId = orderIdGenerator.getAndIncrement();
                double total = 0.0;
                List<String[]> itensPedido = new ArrayList<>();

                // 2. Calcula o total e prepara os itens para salvar
                if (partes.length > 1 && !partes[1].replace("PEDIDO:", "").isEmpty()) {
                    String pedidoStr = partes[1].replace("PEDIDO:", "");
                    String[] itens = pedidoStr.split("\\|");
                    for (String item : itens) {
                        String[] itemDetalhe = item.split(","); // [0] = nome, [1] = quantidade
                        itensPedido.add(itemDetalhe);

                        String nomeProduto = itemDetalhe[0];
                        int quantidade = Integer.parseInt(itemDetalhe[1]);

                        double preco = cardapio.stream()
                                .filter(p -> p.nome().equals(nomeProduto))
                                .findFirst()
                                .map(Produto::preco)
                                .orElse(0.0);

                        total += preco * quantidade;
                    }
                }

                String totalFormatado = String.format("R$ %.2f", total);

                // 3. Salva no Banco de Dados
                String sqlPedido = "INSERT INTO pedidos (id, id_quiosque, valor_total) VALUES (?, ?, ?)";
                String sqlItem = "INSERT INTO itens_pedido (id_pedido, quantidade, id_produto) VALUES (?, ?, (SELECT id FROM produtos WHERE nome = ?))";

                try (Connection conn = DatabaseManager.getConnection()) {
                    conn.setAutoCommit(false); // Inicia transação

                    try (PreparedStatement pstmtPedido = conn.prepareStatement(sqlPedido)) {
                        pstmtPedido.setInt(1, pedidoId);
                        pstmtPedido.setInt(2, kioskId);
                        pstmtPedido.setDouble(3, total);
                        pstmtPedido.executeUpdate();
                    }

                    try (PreparedStatement pstmtItem = conn.prepareStatement(sqlItem)) {
                        for (String[] itemDetalhe : itensPedido) {
                            pstmtItem.setInt(1, pedidoId);
                            pstmtItem.setInt(2, Integer.parseInt(itemDetalhe[1])); // quantidade
                            pstmtItem.setString(3, itemDetalhe[0]); // nome do produto
                            pstmtItem.addBatch();
                        }
                        pstmtItem.executeBatch();
                    }
                    conn.commit(); // Confirma transação
                } catch (SQLException dbException) {
                    dbException.printStackTrace();
                    logger.accept("⚠️ Erro ao salvar pedido #" + pedidoId + " no banco de dados.");
                    // Informa o cliente sobre o erro no DB
                    out.println("Erro interno no servidor ao processar o pedido. Tente novamente.");
                    return; // Encerra o processamento para este cliente
                }

                // 4. Log e resposta para o cliente (após sucesso no DB)
                logger.accept(String.format("===== NOVO PEDIDO #%d (do Quiosque %s) =====", pedidoId, kioskIdStr));
                for (String[] itemDetalhe : itensPedido) {
                    logger.accept(String.format("  - %sx %s", itemDetalhe[1], itemDetalhe[0]));
                }
                logger.accept("VALOR TOTAL: " + totalFormatado);
                logger.accept("=========================================");

                out.println(String.format("Pedido #%d confirmado! Valor total: %s", pedidoId, totalFormatado));

            } catch (IOException | ArrayIndexOutOfBoundsException | NumberFormatException e) {
                logger.accept("⚠️ Erro na comunicação ou formato do pedido.");
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    // Ignora
                }
            }
        }
    }

    // Ponto de entrada da aplicação do servidor.
    public static void main(String[] args) {
        // MODIFICAÇÃO: Chama a inicialização do banco de dados antes de iniciar a UI
        DatabaseManager.initializeDatabase();

        SwingUtilities.invokeLater(PainelControleSocket::new);
    }
}