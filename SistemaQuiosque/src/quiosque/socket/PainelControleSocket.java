package quiosque.socket;

import quiosque.model.ItemPedido;
import quiosque.model.Produto;
import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


public class PainelControleSocket {
    private JTextArea logArea;
    private int kioskCounter = 0;
    private final List<Produto> cardapio;
    private final AtomicInteger orderIdGenerator = new AtomicInteger(1000);

    // Construtor
    public PainelControleSocket() {
        this.cardapio = criarCardapio();

        // Janela principal do servidor
        JFrame frame = new JFrame("Painel de Controle - Servidor Socket");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);
        frame.setLayout(new BorderLayout());

        // Area de Log
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        // Botão para iniciar novos clientes (quiosques)
        JButton launchKioskButton = new JButton("Abrir Novo Quiosque");
        frame.add(launchKioskButton, BorderLayout.SOUTH);

        // Ação do botão: incrementa o contador e cria uma nova janela de quiosque
        launchKioskButton.addActionListener(e -> {
            kioskCounter++;
            new QuiosqueJanelaSocket(kioskCounter);
        });

        frame.setVisible(true);
        startServer();
    }

    // Cria a lista de produtos (cardápio)
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

    // Adiciona uma mensagem ao log da UI de forma segura para threads
    private void addLog(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    // Inicia uma nova Thread para o servidor não bloquear a UI
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

                // Processa a string do pedido usando o protocolo definido (ID;PEDIDO)
                String[] partes = dadosRecebidos.split(";");
                String kioskIdStr = partes[0].replace("ID:", "");

                int pedidoId = orderIdGenerator.getAndIncrement();
                logger.accept(String.format("===== NOVO PEDIDO #%d (do Quiosque %s) =====", pedidoId, kioskIdStr));

                double total = 0.0;
                // Verifica se há itens no pedido para processar
                if (partes.length > 1 && !partes[1].replace("PEDIDO:", "").isEmpty()) {
                    String pedidoStr = partes[1].replace("PEDIDO:", "");
                    String[] itens = pedidoStr.split("\\|");
                    for (String item : itens) {
                        String[] itemDetalhe = item.split(",");
                        String nomeProduto = itemDetalhe[0];
                        int quantidade = Integer.parseInt(itemDetalhe[1]);

                        // Procura o preço do produto no cardápio.
                        double preco = cardapio.stream()
                                .filter(p -> p.nome().equals(nomeProduto))
                                .findFirst()
                                .map(Produto::preco)
                                .orElse(0.0);

                        logger.accept(String.format("  - %dx %s", quantidade, nomeProduto));
                        total += preco * quantidade;
                    }
                }

                String totalFormatado = String.format("R$ %.2f", total);
                logger.accept("VALOR TOTAL: " + totalFormatado);
                logger.accept("=========================================");

                out.println(String.format("Pedido #%d confirmado! Valor total: %s", pedidoId, totalFormatado));

            } catch (IOException | ArrayIndexOutOfBoundsException | NumberFormatException e) {
                logger.accept("⚠️ Erro na comunicação ou formato do pedido.");
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                }
            }
        }
    }

    // Ponto de entrada da aplicação do servidor.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(PainelControleSocket::new);
    }
}