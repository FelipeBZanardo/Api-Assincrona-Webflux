package tech.ada.pedidoapi.pubsub;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import tech.ada.pedidoapi.client.CatalogoClient;
import tech.ada.pedidoapi.model.Status;
import tech.ada.pedidoapi.repository.PedidoRepository;
import tech.ada.pedidoapi.service.PedidoService;

import java.time.Instant;


@Slf4j
@Component
@RequiredArgsConstructor
public class PubSubListener implements InitializingBean {

    private final Sinks.Many<PubSubMessage> sink;
    private final PedidoRepository pedidoRepository;
    private final PedidoService pedidoService;
    private final CatalogoClient client;

    @Override
    public void afterPropertiesSet() throws Exception {
        sink.asFlux()
                //.delayElements(Duration.ofSeconds(5))
                .subscribe(
                        next -> {
                            log.info("Iniciando listener onNext -{}", next);
                            pedidoRepository.findById(next.pedido().getId())
                                    .publishOn(Schedulers.boundedElastic())
                                    .flatMap(pedidoService::atualizarStatus)
                                    .flatMap(pedidoService::atualizarValor)
                                    .flatMap(pedido -> {
                                        log.info("Verificando Status do Pedido - {}", pedido);
                                        if(pedido.getStatus().equals(Status.REALIZADO)){
                                            log.info("Pedido confirmado. Está pronto para ser enviado - {}",pedido );
                                            return pedidoRepository.save(
                                                    pedido.withStatus(Status.CONFIRMADO).withData(Instant.now()));
                                        }
                                        log.error("Pedido não confirmado. Erro ao efetuar compra");
                                        return Mono.defer(() -> pedidoRepository.save(pedido));
                                    })
                                    .flatMap(pedido -> {
                                        if(pedido.getStatus().equals(Status.CONFIRMADO)){
                                            log.info("Enviando pedido - {}", pedido);
                                            return pedidoRepository.save(
                                                    pedido.withStatus(Status.ENVIADO).withData(Instant.now()));
                                        }
                                        log.error("Pedido não enviado. Erro ao efetuar compra");
                                        return Mono.defer(() -> pedidoRepository.save(pedido));
                                    })
                                    .flatMap(pedido -> {
                                        if(pedido.getStatus().equals(Status.ENVIADO)){
                                            log.info("Atualizando Estoque na Api de Catálogo - {}", pedido);
                                            pedido.getItens().forEach(item ->
                                                    client.atualizarQuantidade(item.idProduto(), item.quantidade())
                                                            .subscribe());
                                        }
                                        return Mono.defer(() -> pedidoRepository.findById(pedido.getId()));
                                    })
                                    .subscribe();
                        },
                        err -> log.error("Error: {}", err.getMessage()),
                        () -> log.info("Completed")
                );
    }
}
