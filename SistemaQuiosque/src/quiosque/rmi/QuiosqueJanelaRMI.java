package quiosque.rmi;

import quiosque.model.ItemPedido;
import quiosque.model.Produto;
import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;


public class QuiosqueJanelaRMI extends JFrame {
    private final int kioskId;
    private final String authToken;
    private final List<ItemPedido> carrinho = new ArrayList<>();
    private JTextArea carrinhoArea;
    private JComboBox<Produto> cardapioComboBox;
    private JButton finalizarButton;
    private JButton adicionarButton;


    private static class ProdutoRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Produto produto) {
                setText(String.format("%s - R$ %.2f", produto.nome(), produto.preco()));
            }
            return this;
        }
    }

    // Construtor
    public QuiosqueJanelaRMI(int kioskNumber) {
        this.kioskId = kioskNumber;
        // Gera um token de autenticação simples baseado no número do quiosque
        this.authToken = "QUIOSQUE_0" + kioskNumber + "_TOKEN_SECRETO";
        setTitle("Quiosque " + kioskNumber + " (RMI Seguro)");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(500, 450);
        setLayout(new BorderLayout(10, 10));

        JPanel painelSuperior = new JPanel();
        painelSuperior.setLayout(new BoxLayout(painelSuperior, BoxLayout.Y_AXIS));

        JPanel painelProduto = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cardapioComboBox = new JComboBox<>();
        cardapioComboBox.setRenderer(new ProdutoRenderer());
        painelProduto.add(new JLabel("Produto:"));
        painelProduto.add(cardapioComboBox);

        JPanel painelQtd = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JSpinner quantidadeSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        painelQtd.add(new JLabel("Quantidade:"));
        painelQtd.add(quantidadeSpinner);

        JPanel painelBotao = new JPanel(new FlowLayout(FlowLayout.CENTER));
        adicionarButton = new JButton("Adicionar ao Pedido");
        painelBotao.add(adicionarButton);

        painelSuperior.add(painelProduto);
        painelSuperior.add(painelQtd);
        painelSuperior.add(painelBotao);

        carrinhoArea = new JTextArea();
        carrinhoArea.setEditable(false);
        carrinhoArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollCarrinho = new JScrollPane(carrinhoArea);
        carrinhoArea.setText("--- Seu Pedido ---");
        carrinhoArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        finalizarButton = new JButton("Finalizar Pedido e Pagar");
        finalizarButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        finalizarButton.setEnabled(false);

        add(painelSuperior, BorderLayout.NORTH);
        add(scrollCarrinho, BorderLayout.CENTER);
        add(finalizarButton, BorderLayout.SOUTH);
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Adicionar um item ao carrinho
        adicionarButton.addActionListener(e -> {
            Produto produtoSelecionado = (Produto) cardapioComboBox.getSelectedItem();
            int quantidade = (int) quantidadeSpinner.getValue();
            if (produtoSelecionado != null) {
                carrinho.add(new ItemPedido(produtoSelecionado, quantidade));
                atualizarCarrinho();
                finalizarButton.setEnabled(true);
            }
        });

        // Finalizar o pedido e fazer a chamada RMI
        finalizarButton.addActionListener(e -> {
            if (carrinho.isEmpty()) return;

            finalizarButton.setEnabled(false);
            adicionarButton.setEnabled(false);

            // Usa SwingWorker para a chamada remota não travar a UI
            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws Exception {
                    Registry registry = LocateRegistry.getRegistry("localhost", 1099, new SslRmiClientFactory());
                    InterfacePedidos stub = (InterfacePedidos) registry.lookup("ServicoPedidos");
                    return stub.fazerPedido(authToken, kioskId, new ArrayList<>(carrinho));
                }

                @Override
                protected void done() {
                    try {
                        String resposta = get();
                        Icon icon = UIManager.getIcon("OptionPane.informationIcon");
                        JOptionPane.showMessageDialog(QuiosqueJanelaRMI.this, resposta, "Pedido Confirmado", JOptionPane.INFORMATION_MESSAGE, icon);
                        limparPedido();
                    } catch (Exception ex) {
                        // Trata exceções
                        if (ex.getCause() instanceof SecurityException) {
                            JOptionPane.showMessageDialog(QuiosqueJanelaRMI.this, "Falha na autenticação: " + ex.getCause().getMessage(), "Acesso Negado", JOptionPane.ERROR_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(QuiosqueJanelaRMI.this, "Erro de comunicação com o servidor.", "Erro no Pedido", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    finalizarButton.setEnabled(true);
                    adicionarButton.setEnabled(true);
                }
            }.execute();
        });

        carregarCardapio();
        setLocationByPlatform(true);
        setVisible(true);
    }

    private void limparPedido() {
        carrinho.clear();
        atualizarCarrinho();
        finalizarButton.setEnabled(false);
    }

    private void atualizarCarrinho() {
        StringBuilder sb = new StringBuilder("--- Seu Pedido ---\n");
        double total = 0;
        for (ItemPedido item : carrinho) {
            sb.append(String.format("%dx %-20s R$ %.2f\n", item.quantidade(), item.produto().nome(), item.produto().preco() * item.quantidade()));
            total += item.produto().preco() * item.quantidade();
        }
        sb.append("---------------------------------\n");
        sb.append(String.format("TOTAL: R$ %.2f", total));
        carrinhoArea.setText(sb.toString());
    }

    // Carrega o cardápio do servidor
    private void carregarCardapio() {
        new SwingWorker<List<Produto>, Void>() {
            @Override
            protected List<Produto> doInBackground() throws Exception {
                Registry registry = LocateRegistry.getRegistry("localhost", 1099, new SslRmiClientFactory());
                InterfacePedidos stub = (InterfacePedidos) registry.lookup("ServicoPedidos");
                return stub.getCardapio();
            }

            @Override
            protected void done() {
                try {
                    List<Produto> cardapio = get();
                    for (Produto p : cardapio) {
                        cardapioComboBox.addItem(p);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(QuiosqueJanelaRMI.this, "Nao foi possivel carregar o cardapio do servidor seguro.", "Erro de Conexao", JOptionPane.ERROR_MESSAGE);
                    dispose();
                }
            }
        }.execute();
    }

    // Configura o cliente para confiar no certificado do servidor
    private static void setupClientSsl() {
        try {
            char[] password = "123456".toCharArray();
            KeyStore ts = KeyStore.getInstance("JKS");
            // O cliente carrega o keystore do servidor
            var keystoreStream = QuiosqueJanelaRMI.class.getResourceAsStream("/server.keystore");
            ts.load(keystoreStream, password);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ts);

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, tmf.getTrustManagers(), null);

            SSLContext.setDefault(sc);
        } catch (Exception e) {
            // Se a segurança não puder ser configurada, o cliente não pode continuar
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        // Configura a segurança SSL do cliente
        setupClientSsl();

        // Inicia a UI do quiosque
        SwingUtilities.invokeLater(() -> {
            int kioskNumber = 1;
            if (args.length > 0) {
                try {
                    kioskNumber = Integer.parseInt(args[0]);
                } catch (NumberFormatException ex) {
                }
            }
            new QuiosqueJanelaRMI(kioskNumber);
        });
    }
}