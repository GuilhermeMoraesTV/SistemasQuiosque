package quiosque.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    // URL de conexão para o  SQL Server.
    private static final String URL = "jdbc:sqlserver://localhost:1433;databaseName=quiosque_db;encrypt=false;trustServerCertificate=true;";
    private static final String USER = "quiosque_user";
    private static final String PASSWORD = "SenhaForte#2025";

    /**
     * Obtém uma nova conexão com o banco de dados.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Cria as tabelas necessárias no banco de dados se elas ainda não existirem.
     * Este método torna a aplicação auto-configurável.
     */
    public static void initializeDatabase() {
        String sqlProdutos = "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='produtos' and xtype='U') " +
                "CREATE TABLE produtos (id INT IDENTITY(1,1) PRIMARY KEY, nome VARCHAR(255) NOT NULL UNIQUE, preco DECIMAL(10, 2) NOT NULL)";

        String sqlPedidos = "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='pedidos' and xtype='U') " +
                "CREATE TABLE pedidos (id INT PRIMARY KEY, id_quiosque INT NOT NULL, valor_total DECIMAL(10, 2) NOT NULL, data_hora DATETIME DEFAULT GETDATE())";

        String sqlItensPedido = "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='itens_pedido' and xtype='U') " +
                "CREATE TABLE itens_pedido (id INT IDENTITY(1,1) PRIMARY KEY, id_pedido INT NOT NULL, id_produto INT NOT NULL, quantidade INT NOT NULL, " +
                "FOREIGN KEY (id_pedido) REFERENCES pedidos(id), FOREIGN KEY (id_produto) REFERENCES produtos(id))";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            System.out.println("Inicializando banco de dados...");
            stmt.execute(sqlProdutos);
            stmt.execute(sqlPedidos);
            stmt.execute(sqlItensPedido);
            System.out.println("Tabelas verificadas/criadas com sucesso.");

            // Insere os produtos iniciais se a tabela estiver vazia
            stmt.execute("MERGE INTO produtos AS target " +
                    "USING (VALUES ('Hambúrguer Clássico', 25.50), ('X-Bacon Supremo', 29.00), ('Batata Frita (M)', 12.00), ('Anéis de Cebola', 15.00), ('Refrigerante Lata', 7.00), ('Milkshake de Morango', 18.50)) " +
                    "AS source (nome, preco) ON target.nome = source.nome " +
                    "WHEN NOT MATCHED THEN INSERT (nome, preco) VALUES (source.nome, source.preco);");
            System.out.println("Cardápio inicial verificado/inserido.");

        } catch (SQLException e) {
            System.err.println("Erro fatal ao inicializar o banco de dados. Verifique a conexão e as credenciais.");
            e.printStackTrace();
            System.exit(1); // Encerra a aplicação se não conseguir conectar ao DB
        }
    }

    public static int getLastOrderId() {
        String sql = "SELECT MAX(id) FROM pedidos";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                // Retorna o maior ID encontrado, ou 999 se a tabela estiver vazia
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // Valor inicial padrão se a tabela estiver vazia ou ocorrer um erro
        return 999;
    }
}