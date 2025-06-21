

# Sistema de Pedidos para Quiosques Autônomos

Este projeto foi desenvolvido para a disciplina de Sistemas Distribuídos do Instituto Federal da Bahia (IFBA) - Campus Santo Antônio de Jesus. A aplicação simula um sistema de pedidos para quiosques de uma praça de alimentação, implementado com duas abordagens distintas de comunicação remota: **Sockets TCP** e **Java RMI (Remote Method Invocation)** com segurança SSL/TLS.

## Estrutura do Projeto

O código-fonte foi organizado para separar as fontes (`src`), as classes compiladas (`bin`) e os recursos (`resources`), seguindo as boas práticas de desenvolvimento Java.

```
SistemaQuiosque/
├── src/
│   ├── quiosque/
│   │   ├── model/
│   │   ├── rmi/
│   │   └── socket/
├── bin/
├── resources/
│   └── server.keystore
├── _COMPILAR.bat
├── _EXECUTAR_SISTEMA_RMI.bat
├── _EXECUTAR_SISTEMA_SOCKET.bat
└── _PARAR_TUDO.bat
```


## Como Executar os Sistemas

Para garantir o funcionamento, siga os passos na ordem correta. Foram criados scripts de automação (`.bat`) para facilitar a compilação e execução no Windows.

### Passo 1: Gerar o Certificado SSL (Necessário para a versão RMI)

Este passo só precisa ser executado uma vez.

1.  Abra um terminal (prompt de comando) na pasta raiz do projeto.
2.  Execute o comando `keytool` abaixo para criar o arquivo de certificado `server.keystore`. A senha utilizada é `123456`.
    ```sh
    keytool -genkey -alias server -keyalg RSA -keystore server.keystore -storepass 123456 -keypass 123456 -dname "CN=localhost, OU=Dev, O=MyOrg, L=Santo Antonio de Jesus, S=Bahia, C=BR"
    ```
3.  Após a criação, **mova o arquivo `server.keystore` para dentro da pasta `resources/`**.

### Passo 2: Compilar o Projeto

Execute o script `COMPILAR.bat` para compilar todo o código-fonte da pasta `src` e salvar os arquivos `.class` na pasta `bin`.

> Dê um duplo clique no arquivo: **`_COMPILAR.bat`**

### Passo 3: Executar o Sistema Desejado

Após compilar, você pode iniciar qualquer uma das versões.

1.  **Para rodar a versão com Sockets:**
    > Dê um duplo clique no arquivo: **`EXECUTAR_SISTEMA_SOCKET.bat`**
2.  **Para rodar a versão com RMI Seguro:**
    > Dê um duplo clique no arquivo: **`EXECUTAR_SISTEMA_RMI.bat`**

Após a execução, uma janela de "Painel de Controle" (servidor) aparecerá. Nela, você verá os logs do servidor e um botão **"Abrir Novo Quiosque"**. Clique neste botão para lançar quantas janelas de cliente (quiosques) desejar.

### Passo 4: Finalizando a Aplicação

Para fechar completamente o sistema (incluindo o `RMI Registry` que pode ficar rodando em segundo plano), execute o script `_PARAR_TUDO.bat`.

> Dê um duplo clique no arquivo: **`_PARAR_TUDO.bat`**