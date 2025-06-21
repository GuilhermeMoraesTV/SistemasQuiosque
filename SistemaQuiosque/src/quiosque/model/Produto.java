package quiosque.model;

import java.io.Serializable;

public record Produto(String nome, double preco) implements Serializable {}