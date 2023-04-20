package tech.ada.pedidoapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import tech.ada.pedidoapi.client.CatalogoClient;
import tech.ada.pedidoapi.model.dto.ItemDTO;
import tech.ada.pedidoapi.model.Pedido;
import tech.ada.pedidoapi.model.Status;
import tech.ada.pedidoapi.repository.PedidoRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final CatalogoClient client;

    public Mono<Pedido> efetuarPedido(List<ItemDTO> itens) {
        log.info("Iniciando Pedido - {}", itens);
        return Mono.defer(() -> {
            log.info("Verificando estoque e calculando total do pedido");
            Pedido pedido = Pedido.builder()
                    .id(UUID.randomUUID().toString())
                    .itens(itens)
                    .status(Status.REALIZADO)
                    .total(BigDecimal.ZERO)
                    .data(Instant.now())
                    .build();
            return calcularTotal(itens)
                    .map(bigDecimal -> {
                        pedido.setTotal(bigDecimal);
                        return pedido;
                    })
                    .onErrorContinue((throwable, o) -> {
                        pedido.setStatus(Status.ERRO_NO_PEDIDO);
                        log.error(throwable.getMessage());
                    })
                    .flatMap(__ -> {
                        log.info("Salvando pedido no banco de dados - {}", pedido);
                        return pedidoRepository.save(pedido);
                    });
        });
    }

   private Mono<BigDecimal> calcularTotal(List<ItemDTO> itens){
       return Flux.fromIterable(itens).
               subscribeOn(Schedulers.boundedElastic())
               .flatMap(itemDTO ->
                       client.getProdutoById(itemDTO.idProduto())
                               .map(produto -> {
                                   if (produto.quantidade() < itemDTO.quantidade())
                                       throw new RuntimeException("Quantidade insuficiente do produto " + itemDTO.idProduto());
                                   return produto;
                               })
                               .map(produto -> {
                                   BigDecimal quantidade = BigDecimal.valueOf(itemDTO.quantidade());
                                   return produto.preco().multiply(quantidade);}))
               .reduce(BigDecimal::add);
   }

    public Flux<Pedido> getAll() {
        return Flux.defer(() ->{
            log.info("Buscando todos os pedidos no banco de dados");
            return pedidoRepository.findAll();
        });
    }

    public Mono<Pedido> getPedidoById(String id) {
        return Mono.defer(() -> {
            log.info("Buscando Pedido no banco de dados - {}", id);
            return pedidoRepository.findById(id)
                    .switchIfEmpty(Mono.error(new RuntimeException("Pedido não existe na base de dados")));
        });
    }
}
