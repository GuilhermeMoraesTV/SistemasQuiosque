USE [master]
GO
/****** Object:  Database [quiosque_db]    Script Date: 28/07/2025 08:37:10 ******/
CREATE DATABASE [quiosque_db]
 CONTAINMENT = NONE
 ON  PRIMARY 
( NAME = N'quiosque_db', FILENAME = N'C:\Program Files\Microsoft SQL Server\MSSQL16.SQLEXPRESS\MSSQL\DATA\quiosque_db.mdf' , SIZE = 8192KB , MAXSIZE = UNLIMITED, FILEGROWTH = 65536KB )
 LOG ON 
( NAME = N'quiosque_db_log', FILENAME = N'C:\Program Files\Microsoft SQL Server\MSSQL16.SQLEXPRESS\MSSQL\DATA\quiosque_db_log.ldf' , SIZE = 8192KB , MAXSIZE = 2048GB , FILEGROWTH = 65536KB )
 WITH CATALOG_COLLATION = DATABASE_DEFAULT, LEDGER = OFF
GO
ALTER DATABASE [quiosque_db] SET COMPATIBILITY_LEVEL = 160
GO
IF (1 = FULLTEXTSERVICEPROPERTY('IsFullTextInstalled'))
begin
EXEC [quiosque_db].[dbo].[sp_fulltext_database] @action = 'enable'
end
GO
ALTER DATABASE [quiosque_db] SET ANSI_NULL_DEFAULT OFF 
GO
ALTER DATABASE [quiosque_db] SET ANSI_NULLS OFF 
GO
ALTER DATABASE [quiosque_db] SET ANSI_PADDING OFF 
GO
ALTER DATABASE [quiosque_db] SET ANSI_WARNINGS OFF 
GO
ALTER DATABASE [quiosque_db] SET ARITHABORT OFF 
GO
ALTER DATABASE [quiosque_db] SET AUTO_CLOSE ON 
GO
ALTER DATABASE [quiosque_db] SET AUTO_SHRINK OFF 
GO
ALTER DATABASE [quiosque_db] SET AUTO_UPDATE_STATISTICS ON 
GO
ALTER DATABASE [quiosque_db] SET CURSOR_CLOSE_ON_COMMIT OFF 
GO
ALTER DATABASE [quiosque_db] SET CURSOR_DEFAULT  GLOBAL 
GO
ALTER DATABASE [quiosque_db] SET CONCAT_NULL_YIELDS_NULL OFF 
GO
ALTER DATABASE [quiosque_db] SET NUMERIC_ROUNDABORT OFF 
GO
ALTER DATABASE [quiosque_db] SET QUOTED_IDENTIFIER OFF 
GO
ALTER DATABASE [quiosque_db] SET RECURSIVE_TRIGGERS OFF 
GO
ALTER DATABASE [quiosque_db] SET  ENABLE_BROKER 
GO
ALTER DATABASE [quiosque_db] SET AUTO_UPDATE_STATISTICS_ASYNC OFF 
GO
ALTER DATABASE [quiosque_db] SET DATE_CORRELATION_OPTIMIZATION OFF 
GO
ALTER DATABASE [quiosque_db] SET TRUSTWORTHY OFF 
GO
ALTER DATABASE [quiosque_db] SET ALLOW_SNAPSHOT_ISOLATION OFF 
GO
ALTER DATABASE [quiosque_db] SET PARAMETERIZATION SIMPLE 
GO
ALTER DATABASE [quiosque_db] SET READ_COMMITTED_SNAPSHOT OFF 
GO
ALTER DATABASE [quiosque_db] SET HONOR_BROKER_PRIORITY OFF 
GO
ALTER DATABASE [quiosque_db] SET RECOVERY SIMPLE 
GO
ALTER DATABASE [quiosque_db] SET  MULTI_USER 
GO
ALTER DATABASE [quiosque_db] SET PAGE_VERIFY CHECKSUM  
GO
ALTER DATABASE [quiosque_db] SET DB_CHAINING OFF 
GO
ALTER DATABASE [quiosque_db] SET FILESTREAM( NON_TRANSACTED_ACCESS = OFF ) 
GO
ALTER DATABASE [quiosque_db] SET TARGET_RECOVERY_TIME = 60 SECONDS 
GO
ALTER DATABASE [quiosque_db] SET DELAYED_DURABILITY = DISABLED 
GO
ALTER DATABASE [quiosque_db] SET ACCELERATED_DATABASE_RECOVERY = OFF  
GO
ALTER DATABASE [quiosque_db] SET QUERY_STORE = ON
GO
ALTER DATABASE [quiosque_db] SET QUERY_STORE (OPERATION_MODE = READ_WRITE, CLEANUP_POLICY = (STALE_QUERY_THRESHOLD_DAYS = 30), DATA_FLUSH_INTERVAL_SECONDS = 900, INTERVAL_LENGTH_MINUTES = 60, MAX_STORAGE_SIZE_MB = 1000, QUERY_CAPTURE_MODE = AUTO, SIZE_BASED_CLEANUP_MODE = AUTO, MAX_PLANS_PER_QUERY = 200, WAIT_STATS_CAPTURE_MODE = ON)
GO
USE [quiosque_db]
GO
/****** Object:  User [quiosque_user]    Script Date: 28/07/2025 08:37:10 ******/
CREATE USER [quiosque_user] FOR LOGIN [quiosque_user] WITH DEFAULT_SCHEMA=[dbo]
GO
ALTER ROLE [db_owner] ADD MEMBER [quiosque_user]
GO
/****** Object:  Schema [Relatorios]    Script Date: 28/07/2025 08:37:10 ******/
CREATE SCHEMA [Relatorios]
GO
/****** Object:  Table [dbo].[produtos]    Script Date: 28/07/2025 08:37:10 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[produtos](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[nome] [varchar](255) NOT NULL,
	[preco] [decimal](10, 2) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[nome] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[pedidos]    Script Date: 28/07/2025 08:37:10 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[pedidos](
	[id] [int] NOT NULL,
	[id_quiosque] [int] NOT NULL,
	[valor_total] [decimal](10, 2) NOT NULL,
	[data_hora] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[itens_pedido]    Script Date: 28/07/2025 08:37:10 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[itens_pedido](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[id_pedido] [int] NOT NULL,
	[id_produto] [int] NOT NULL,
	[quantidade] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  View [Relatorios].[v_PedidosDetalhados]    Script Date: 28/07/2025 08:37:10 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

-- Cria a view novamente, mas agora dentro do schema 'Relatorios'

CREATE VIEW [Relatorios].[v_PedidosDetalhados] AS
SELECT
    p.id AS ID_Pedido,
    p.id_quiosque,
    p.valor_total,
    p.data_hora,
    prod.nome AS Nome_Produto,
    ip.quantidade,
    prod.preco AS Preco_Unitario,
    (ip.quantidade * prod.preco) AS Subtotal_Item
FROM
    pedidos p
JOIN
    itens_pedido ip ON p.id = ip.id_pedido
JOIN
    produtos prod ON ip.id_produto = prod.id;
GO
ALTER TABLE [dbo].[pedidos] ADD  DEFAULT (getdate()) FOR [data_hora]
GO
ALTER TABLE [dbo].[itens_pedido]  WITH CHECK ADD FOREIGN KEY([id_pedido])
REFERENCES [dbo].[pedidos] ([id])
GO
ALTER TABLE [dbo].[itens_pedido]  WITH CHECK ADD FOREIGN KEY([id_produto])
REFERENCES [dbo].[produtos] ([id])
GO
USE [master]
GO
ALTER DATABASE [quiosque_db] SET  READ_WRITE 
GO
