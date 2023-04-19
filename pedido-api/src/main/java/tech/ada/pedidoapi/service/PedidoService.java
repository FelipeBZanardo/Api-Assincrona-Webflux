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
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final CatalogoClient client;
    private static Pedido pedido;


    public Mono<Pedido> efetuarPedido(List<ItemDTO> itens) {
        log.info("Iniciando Pedido - {}", itens);
        pedido = Pedido.builder()
                .id(UUID.randomUUID().toString())
                .itens(itens)
                .status(Status.REALIZADO)
                .total(BigDecimal.ZERO)
                .build();



        itens.forEach(itemDTO -> verificarEstoque(itemDTO).delayElement(Duration.ofSeconds(5)).subscribe());
        return Mono.defer(() -> {
                    log.info("Salvando pedido");
                    return pedidoRepository.save(pedido.withData(Instant.now()));
                })

                .subscribeOn(Schedulers.boundedElastic());

    }

    public Mono<Pedido> verificarEstoque(ItemDTO itemDTO) {
        return Mono.defer(() -> {
            log.info("Verificando estoque do item - {}", itemDTO);
            return client.getProdutoById(itemDTO.idProduto());
        })
                .doOnError(produto -> log.error("Produto inesxistente no catálogo - {}", itemDTO))
                .flatMap(produto -> {
                    BigDecimal valorASomar = produto.preco().multiply(BigDecimal.valueOf(itemDTO.quantidade()));
                    if(produto.quantidade() < itemDTO.quantidade()){
                        pedido = pedido.withStatus(Status.ERRO_NO_PEDIDO);
                        log.error("Quantidade do produto insuficiente - {}", itemDTO);
                    }
                    pedido = pedido.withTotal(pedido.getTotal().add(valorASomar));
                    return Mono.fromCallable(() -> pedido);

                });
    }

    public Mono<Pedido> atualizarValor(Pedido pedidoAAtualizar){
        return Mono.fromCallable(() -> pedidoAAtualizar.withTotal(pedido.getTotal()));
    }

    public Mono<Pedido> atualizarStatus(Pedido pedidoAAtualizar){
        return Mono.fromCallable(() -> pedidoAAtualizar.withStatus(pedido.getStatus()));
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
