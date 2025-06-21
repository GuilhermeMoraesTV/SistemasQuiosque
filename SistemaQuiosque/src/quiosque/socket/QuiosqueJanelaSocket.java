package quiosque.socket;

import quiosque.model.ItemPedido;
import quiosque.model.Produto;
import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class QuiosqueJanelaSocket extends JFrame {
    private final int kioskId;
    private final List<ItemPedido> carrinho = new ArrayList<>();
    private JTextArea carrinhoArea;
    private JComboBox<Produto> cardapioComboBox; // ComboBox para selecionar produtos.
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
    public QuiosqueJanelaSocket(int kioskNumber) {
        this.kioskId = kioskNumber;
        setTitle("Quiosque " + kioskNumber + " (Socket)");
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


        adicionarButton.addActionListener(e -> {
            Produto produtoSelecionado = (Produto) cardapioComboBox.getSelectedItem();
            int quantidade = (int) quantidadeSpinner.getValue();
            if (produtoSelecionado != null) {
                carrinho.add(new ItemPedido(produtoSelecionado, quantidade));
                atualizarCarrinho();
                finalizarButton.setEnabled(true);
            }
        });

        finalizarButton.addActionListener(e -> {
            if (carrinho.isEmpty()) return;

            finalizarButton.setEnabled(false);
            adicionarButton.setEnabled(false);


            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws Exception {
                    // Formata o pedido em uma única string
                    String pedidoFormatado = carrinho.stream()
                            .map(item -> item.produto().nome() + "," + item.quantidade())
                            .collect(Collectors.joining("|"));

                    String dadosParaEnviar = String.format("ID:%d;PEDIDO:%s", kioskId, pedidoFormatado);

                    // Conecta ao servidor, envia os dados e aguarda a resposta
                    try (Socket socket = new Socket("localhost", 12345);
                         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                        out.println(dadosParaEnviar);
                        return in.readLine();
                    }
                }

                @Override
                protected void done() {

                    try {
                        String resposta = get();
                        Icon icon = UIManager.getIcon("OptionPane.informationIcon");
                        JOptionPane.showMessageDialog(QuiosqueJanelaSocket.this, resposta, "Pedido Confirmado", JOptionPane.INFORMATION_MESSAGE, icon);
                        limparPedido();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(QuiosqueJanelaSocket.this, "Erro de comunicação com o servidor.", "Erro no Pedido", JOptionPane.ERROR_MESSAGE);
                    }

                    finalizarButton.setEnabled(true);
                    adicionarButton.setEnabled(true);
                }
            }.execute();
        });

        carregarCardapioFixo();
        setVisible(true);
        setLocationByPlatform(true);
    }
    private void carregarCardapioFixo() {
        cardapioComboBox.addItem(new Produto("Hambúrguer Clássico", 25.50));
        cardapioComboBox.addItem(new Produto("X-Bacon Supremo", 29.00));
        cardapioComboBox.addItem(new Produto("Batata Frita (M)", 12.00));
        cardapioComboBox.addItem(new Produto("Anéis de Cebola", 15.00));
        cardapioComboBox.addItem(new Produto("Refrigerante Lata", 7.00));
        cardapioComboBox.addItem(new Produto("Milkshake de Morango", 18.50));
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
}