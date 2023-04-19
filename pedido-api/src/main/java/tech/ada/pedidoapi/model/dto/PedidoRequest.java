package tech.ada.pedidoapi.model.dto;

import java.util.List;

public record PedidoRequest(List<ItemDTO> itens) {
}
