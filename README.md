# Sistema de Chamados — IFRN

## 1. Apresentação

API REST para registrar e acompanhar solicitações de suporte do IFRN. Projeto acadêmico individual com Java 21 e Spring Boot 4.1.2.

## 2. Objetivo

Encaminhar problemas ao setor responsável pela categoria, permitindo que um técnico assuma, converse com o solicitante e registre a solução. Não há autenticação: os IDs de quem executa cada ação são enviados no corpo da requisição. Os dados iniciais são fictícios.

## 3. Tecnologias

Java 21, Maven, Spring Boot 4.1.2, Spring MVC, Spring Data JPA, Bean Validation, PostgreSQL, Flyway, Lombok, JUnit 5, Mockito e Docker Compose. A imagem de execução usa JRE 21 e usuário sem privilégios de root.

## 4. Arquitetura em camadas

Código em `src/main/java/br/edu/ifrn/chamados`:

| Pacote | Responsabilidade |
| --- | --- |
| `controller` | Rotas HTTP, validação de entrada e códigos de resposta |
| `dto` | Contratos de entrada, resposta e paginação |
| `service` | Regras de negócio e transações |
| `repository` | Persistência com Spring Data JPA |
| `model` / `enums` | Entidades, relacionamentos e valores permitidos |
| `specification` | Filtros opcionais e combináveis |
| `exception` | Exceções próprias e tratamento uniforme de erros |
| `config` | Validação dos parâmetros de paginação |

Entidades não são serializadas nas respostas. Operações de escrita são transacionais. Alterações de estado do chamado e novas interações usam bloqueio pessimista na linha do chamado, evitando que duas ações simultâneas executem transições incompatíveis.

## 5. Entidades e relacionamentos

- **Setor**: nome, sigla única; possui categorias e técnicos.
- **Categoria**: nome, descrição e setor obrigatório; possui chamados.
- **Usuario**: nome, email único, matrícula, tipo e setor. Técnicos exigem setor válido; solicitantes podem não possuir setor.
- **Chamado**: título, descrição, local, prioridade, status, datas e solução; possui solicitante, categoria, técnico opcional e interações.
- **Interacao**: texto, data/hora, chamado e autor obrigatórios.

Enums são persistidos como texto. Chaves estrangeiras não têm exclusão em cascata. Emails são normalizados para minúsculas e siglas para maiúsculas; índices únicos também protegem contra duplicidade sem diferenciar caixa.

Dados iniciais de um banco novo:

| Recurso | IDs |
| --- | --- |
| Setores | 1: COTIC; 2: Manutenção (`MANUT`) |
| Categorias | 1: Redes e Internet; 2: Equipamentos de TI (COTIC); 3: Infraestrutura; 4: Instalações Elétricas (Manutenção) |
| Solicitantes | 1: Ana Silva; 2: Bruno Souza |
| Técnicos | 3: Carlos Lima (COTIC); 4: Daniela Santos (Manutenção) |

## 6. Regras de negócio

1. Somente solicitantes abrem chamados. O status inicial é `ABERTO`, a abertura é automática, a prioridade padrão é `MEDIA` e o técnico começa sem atribuição.
2. Somente técnico do setor da categoria assume chamado `ABERTO`, passando a `EM_ATENDIMENTO`.
3. Somente o técnico atribuído resolve chamado `EM_ATENDIMENTO`, com solução preenchida. A resolução registra `dataFechamento`.
4. Somente o próprio solicitante cancela chamado `ABERTO`. O cancelamento também registra `dataFechamento`.
5. Chamados `RESOLVIDO` e `CANCELADO` não aceitam alterações, novas transições nem interações.
6. Somente o solicitante e o técnico atribuído podem interagir. O solicitante pode interagir antes da atribuição.
7. Categorias e setores com chamados não podem ser excluídos. Um setor também não pode ser excluído enquanto possuir categorias ou usuários.
8. Para preservar os vínculos, categorias com chamados não podem mudar de setor; usuários com chamados não podem mudar de tipo ou setor. Nome, descrição e demais dados cadastrais continuam editáveis.

Transições exclusivas: `ABERTO → EM_ATENDIMENTO → RESOLVIDO` e `ABERTO → CANCELADO`. Violações de negócio retornam **409**, inclusive tentativa de resolver sem solução; formatos e tamanhos inválidos retornam **400**.

## 7. Endpoints

| Método | Caminho | Ação |
| --- | --- | --- |
| POST | `/setores` | Criar setor |
| GET | `/setores` | Listar; filtro `nome` |
| GET | `/setores/{id}` | Consultar setor |
| PUT | `/setores/{id}` | Atualizar setor |
| DELETE | `/setores/{id}` | Excluir setor sem vínculos |
| GET | `/setores/{id}/categorias` | Categorias paginadas do setor |
| POST | `/categorias` | Criar categoria |
| GET | `/categorias` | Listar; filtros `nome`, `setorId` |
| GET | `/categorias/{id}` | Consultar categoria |
| PUT | `/categorias/{id}` | Atualizar categoria |
| DELETE | `/categorias/{id}` | Excluir categoria sem chamados |
| POST | `/usuarios` | Criar usuário |
| GET | `/usuarios` | Listar; filtros `nome`, `tipo`, `setorId` |
| GET | `/usuarios/{id}` | Consultar usuário |
| PUT | `/usuarios/{id}` | Atualizar usuário |
| GET | `/usuarios/{id}/chamados` | Chamados do solicitante ou técnico; filtro `status` |
| POST | `/chamados` | Abrir chamado |
| GET | `/chamados` | Listar com filtros combinados |
| GET | `/chamados/{id}` | Detalhar chamado e histórico |
| PATCH | `/chamados/{id}/assumir` | Atribuir técnico |
| PATCH | `/chamados/{id}/resolver` | Resolver com solução |
| PATCH | `/chamados/{id}/cancelar` | Cancelar pelo solicitante |
| POST | `/chamados/{id}/interacoes` | Registrar interação |

O detalhe retorna `{ "chamado": {...}, "interacoes": [...] }`. Interações são ordenadas por data/hora e, em caso de empate, ID. As listagens e respostas de ações retornam os dados do chamado sem carregar seu histórico completo.

## 8. Como executar com Docker

Pré-requisitos: Docker Engine e Docker Compose. Não é preciso Java, Maven ou PostgreSQL instalados na máquina.

```bash
git clone https://github.com/Nathan1227n/ifrn-sistema-chamados-api.git
cd ifrn-sistema-chamados-api
docker compose up
```

A primeira execução compila a aplicação, executa os testes unitários e de controller e baixa as imagens. O banco precisa ficar saudável antes de a API iniciar. Após o log `Started SistemaChamadosApplication`, consulte:

```bash
curl http://localhost:8080/setores
```

Base da API: **http://localhost:8080**. Não há página web na raiz `/`; use os endpoints da tabela. Para reconstruir após mudar código: `docker compose up --build`. Para executar em segundo plano: `docker compose up -d --build`.

O Compose utiliza PostgreSQL 17 e volume persistente `dados_postgres`. O banco não publica uma porta no host. `docker compose down` encerra os serviços e mantém os dados. `docker compose down -v` apaga o banco local: use somente se quiser reiniciá-lo.

As credenciais padrão do Compose são exclusivamente locais. Para sobrescrever, copie `.env.example` para `.env` ou defina `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` e `API_PORT` no ambiente. `.env` é ignorado pelo Git e pelo Docker build. Alterar essas variáveis não muda automaticamente as credenciais de um volume já inicializado.

Execução opcional com Java 21, Maven e PostgreSQL locais:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/chamados
export DB_USERNAME=seu_usuario
export DB_PASSWORD=sua_senha
./mvnw spring-boot:run
```

O banco deve existir; o Flyway cria as tabelas. `application.properties` não contém usuário ou senha fixos.

## 9. Exemplos de requisições

O arquivo [requisicoes.http](requisicoes.http) contém todos os endpoints e cenários válidos e inválidos. Abra-o no cliente HTTP do IntelliJ IDEA ou em extensão compatível. Execute as criações antes das ações que usam seus IDs.

```http
POST /chamados
Content-Type: application/json

{"titulo":"Projetor não funciona","descricao":"O projetor não liga.","local":"Sala 12","prioridade":"ALTA","categoriaId":2,"solicitanteId":1}
```

Com o ID retornado (exemplo: 1):

```http
PATCH /chamados/1/assumir
Content-Type: application/json

{"tecnicoId":3}
```

```http
POST /chamados/1/interacoes
Content-Type: application/json

{"autorId":3,"texto":"Vou verificar a fonte."}
```

```http
PATCH /chamados/1/resolver
Content-Type: application/json

{"tecnicoId":3,"solucao":"Projetor reparado."}
```

Para cancelar, abra outro chamado e envie `{"solicitanteId":1}` a `PATCH /chamados/{id}/cancelar` enquanto estiver `ABERTO`.

## 10. Paginação e filtros

Toda consulta de coleção aceita `page` (padrão **0**), `size` (padrão **10**, máximo **50**) e `sort=campo,asc|desc`. Tamanhos acima de 50 são limitados a 50; página negativa, tamanho menor que 1 ou valor não inteiro retorna 400. Campos desconhecidos de ordenação retornam 400. A ordenação recebe `id` como desempate estável; sem `sort`, usa `id,asc`.

Exemplo de estrutura:

```json
{"itens":[],"numeroPagina":0,"tamanhoPagina":10,"totalElementos":0,"totalPaginas":0}
```

Filtros são opcionais e combináveis. `nome` busca trecho sem distinguir maiúsculas e minúsculas. `tipo`, `status` e `prioridade` usam os valores exatos dos enums em maiúsculas.

`/chamados` aceita `status`, `prioridade`, `setorId`, `categoriaId`, `solicitanteId`, `tecnicoId`, `dataInicio` e `dataFim`.

```http
GET /chamados?status=ABERTO&setorId=1&prioridade=ALTA&page=0&size=10&sort=dataAbertura,desc
GET /chamados?dataInicio=2026-10-01&dataFim=2026-10-30
GET /usuarios/3/chamados?status=EM_ATENDIMENTO&page=0&size=10
```

Datas usam **yyyy-MM-dd**, sobre `dataAbertura`, do início de `dataInicio` até o início do dia seguinte a `dataFim` (exclusivo). Assim, todo o dia final está incluído, mesmo com frações de segundo. Datas/horas retornadas são locais, sem offset; A imagem Docker usa `America/Fortaleza`; na execução local, o horário segue o fuso da JVM. Intervalo invertido retorna 400.

Campos de ordenação: setores (`id`, `nome`, `sigla`); categorias (`id`, `nome`, `descricao`, `setor.id`); usuários (`id`, `nome`, `email`, `matricula`, `tipo`, `setor.id`); chamados (`id`, `titulo`, `descricao`, `local`, `prioridade`, `status`, `dataAbertura`, `dataFechamento`, `categoria.id`, `solicitante.id`, `tecnico.id`). Prioridades/status ordenam pelo texto persistido.

## 11. Tratamento de erros

- **200**: consultas, atualizações e transições.
- **201**: criação, abertura e interação; inclui `Location`.
- **204**: exclusão sem corpo.
- **400**: Bean Validation, JSON malformado, enum/parâmetro inválido.
- **404**: recurso inexistente.
- **409**: regras de negócio, email/sigla duplicados e conflitos de integridade.
- **405/415**: método ou tipo de conteúdo não suportado; **500**: falha inesperada com mensagem genérica.

Todos seguem o contrato abaixo, sem detalhes internos ou stack trace:

```json
{
  "timestamp":"2026-10-30T10:00:00",
  "status":409,
  "mensagem":"Técnico não pertence ao setor responsável",
  "caminho":"/chamados/1/assumir"
}
```

## 12. Flyway e migrations

`V1__criar_tabelas.sql` cria as cinco tabelas, constraints, índices e chaves estrangeiras. `V2__inserir_dados_iniciais.sql` cadastra 2 setores, 4 categorias e 4 usuários. Os arquivos iniciais estavam vazios e sem implementação de esquema no repositório inspecionado.

O Flyway executa migrations pendentes na inicialização. A tabela **flyway_schema_history** registra versão, descrição, data de instalação, execução bem-sucedida e **checksum** (uma assinatura do conteúdo da migration). Se o conteúdo de uma migration já aplicada mudar, seu checksum diverge e a validação do Flyway impede a inicialização.

Depois de aplicada em qualquer banco, uma migration deve permanecer imutável: faça correções em `V3__...sql`, `V4__...sql` etc. Não apague o histórico nem use `repair` para disfarçar alterações de esquema. Se já existirem migrations aplicadas fora deste repositório, confira seu histórico antes de usar estes arquivos.

`spring.jpa.hibernate.ddl-auto=validate` verifica o mapeamento das entidades sem criar ou alterar o esquema. `ddl-auto=update` não é recomendado para produção porque não oferece migrations revisáveis, histórico confiável ou controle explícito de transformações e perda de dados.

## 13. Como executar os testes

Testes de service (Mockito) e controller (`@WebMvcTest`), sem banco e sem Docker:

```bash
./mvnw test
./mvnw clean package
```

Também é possível usar `mvn` em vez de `./mvnw`. O build Docker executa esses mesmos testes.

Os testes de integração `*IT` preservam a verificação de carregamento do contexto e acrescentam persistência PostgreSQL real, validação Flyway/Hibernate, CRUD, histórico, transições, filtros combinados, datas e paginação. Usam um **banco exclusivo de testes**. Exemplo de banco descartável:

```bash
docker run -d --name chamados-testes \
  -e POSTGRES_DB=chamados_teste -e POSTGRES_PASSWORD=testes_local \
  -p 127.0.0.1:55432:5432 postgres:17-alpine
# Aguarde: docker exec chamados-testes pg_isready -U postgres -d chamados_teste
TEST_DB_PASSWORD=testes_local ./mvnw -Pintegracao verify
# Ao terminar:
docker rm -f chamados-testes
```

A configuração de teste aceita `TEST_DB_URL` (padrão `jdbc:postgresql://localhost:55432/chamados_teste`), `TEST_DB_USERNAME` (padrão `postgres`) e `TEST_DB_PASSWORD`. Use somente banco dedicado, nunca produção. Os casos de persistência são transacionais e revertidos ao final. Relatórios ficam em `target/surefire-reports` e `target/failsafe-reports`.
