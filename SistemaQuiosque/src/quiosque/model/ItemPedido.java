package quiosque.model;

import java.io.Serializable;

public record ItemPedido(Produto produto, int quantidade) implements Serializable {}