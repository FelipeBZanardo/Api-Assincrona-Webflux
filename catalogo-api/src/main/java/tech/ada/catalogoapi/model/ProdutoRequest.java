package tech.ada.catalogoapi.model;

import java.math.BigDecimal;

public record ProdutoRequest(String nome, BigDecimal preco, Integer quantidade) {
}
