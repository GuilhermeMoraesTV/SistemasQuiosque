package quiosque.rmi;

import quiosque.model.ItemPedido;
import quiosque.model.Produto;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface InterfacePedidos extends Remote {
    List<Produto> getCardapio() throws RemoteException;
    String fazerPedido(String authToken, int kioskId, List<ItemPedido> itens) throws RemoteException;
}