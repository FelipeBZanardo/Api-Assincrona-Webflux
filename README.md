# Api-Assincrona-Webflux
Exercício Web III - API's Assíncronas com WebFlux

## ✒️ Autor

[Felipe Zanardo](https://github.com/FelipeBZanardo)

## 📋 Enunciado:
Crie duas API's e um API gateway, uma de Catalogo/Produtos e uma de Pedido.

#### API de catalogo:

- Deve expor chamadas REST para listar todos os Produtos que é composto por id, nome, preco e quantidade no estoque.

#### API de pedidos:

- Deve expor chamadas REST para efetuar um pedido e consultar um pedido;
- O Pedido deve ser composto por id, uma lista de itens, data hora, status e total do pedido;
- Um item deve ter id do produto e a quantidade;
- A api de pedidos deve confirmar se cada produto existe e se tem estoque suficiente antes de confirmar o pedido.
- Os status do pedido devem ser REALIZADO, CONFIRMADO, ERRO_NO_PEDIDO e ENVIADO PARA ENTREGA

## ⚙️ Configurações Iniciais

* ### catalogo-api:
  - Abrir o projeto catalogo-api
  - No terminal: 
  `docker compose up` (as vezes é necessário rodar duas vezes)
  - Play no microsserviço
  
 * ### pedido-api:
    - Abrir o projeto pedido-api
    - No terminal: 
    `docker compose up` (as vezes é necessário rodar duas vezes)
    - Play no microsserviço
  
 * ### gateway:
    - Abrir o projeto gateway
    - Play na aplicação
  
 * ### Abrir o Arquivo Insomnia e testar as Requisições
   

## 🛠️ Tecnologias Utilizadas

* [IntelliJ IDEA](https://www.jetbrains.com/pt-br/idea/) - IDE
* [Spring Initializer](https://start.spring.io/)
* [Maven](https://maven.apache.org/) - Gerenciador de Dependência
* [Docker](https://www.mongodb.com/) - Banco de Dados
* [MongoDB](https://www.docker.com/)






