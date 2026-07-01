# SpringExtensao

Sistema de gerenciamento de extensão universitária desenvolvido para a disciplina de Engenharia de Software / Programação Orientada a Objetos da **UFMA (Universidade Federal do Maranhão)**. A aplicação evoluiu de um projeto Java OOP puro para uma API REST completa construída com **Spring Boot**, permitindo o gerenciamento de cursos, discentes, docentes, grupos de extensão, oportunidades, inscrições e solicitações de aproveitamento de carga horária.

## Sobre o projeto

O SpringExtensao modela o fluxo de atividades de extensão de uma universidade, cobrindo desde o cadastro de cursos e usuários até a criação de grupos de extensão, publicação de oportunidades, inscrição de discentes e submissão/análise de solicitações de horas de extensão.

### Principais funcionalidades

- **Usuários**: cadastro e login de discentes e docentes, promoção de discente a docente, desativação e anonimização de contas (LGPD).
- **Cursos**: cadastro de cursos, controle de versões de currículo (PPC) e histórico de UCEs (Unidades Curriculares de Extensão).
- **Grupos de extensão**: criação, aprovação/rejeição, gestão de membros e atribuição de cargos (diretor, tesoureiro etc.).
- **Oportunidades**: criação, publicação, aprovação, início, encerramento e cancelamento de oportunidades de extensão.
- **Inscrições**: inscrição de discentes em oportunidades, aprovação, rejeição, desistência e fila de espera.
- **Solicitações**: submissão de solicitações de carga horária, aprovação, indeferimento e reenvio dentro do prazo.

## Tecnologias utilizadas

- **Java 17**
- **Spring Boot 4.1.0** (Web MVC, Web Services, Jersey, Data JPA)
- **PostgreSQL** (banco de dados principal)
- **H2 Database** (banco em memória para testes)
- **Spring Security Crypto** (hash de senhas)
- **Lombok** (redução de boilerplate)
- **JUnit 5 + Mockito + AssertJ** (testes unitários)
- **Maven** (gerenciamento de dependências e build)

## Estrutura do projeto

```
src/main/java/br/ufma/springextensao/
├── controller/         # Controllers REST (endpoints da API)
│   └── dtos/            # Data Transfer Objects
├── enums/               # Enums de domínio (Status, StatusOp)
├── model/               # Entidades JPA (Usuario, Curso, Grupo, Oportunidade, ...)
├── repository/          # Interfaces Spring Data JPA
├── service/              # Regras de negócio
├── util/                # Classes utilitárias (Sessao, Validacao)
└── SpringExtensaoApplication.java

src/main/resources/
├── application-example.properties  # Modelo de configuração do banco
└── data.sql                        # Dados iniciais (seed)

src/test/java/br/ufma/springextensao/
├── controller/          # Testes unitários dos controllers
└── service/              # Testes unitários dos services
```

## Modelo de domínio

O sistema é organizado em torno das seguintes entidades principais:

- `Usuario` (classe base, herança `JOINED`) → `Discente` e `Docente`
- `Curso` → possui `UCE`s, `Discente`s, `Grupo`s e `Oportunidade`s
- `Grupo` → possui um `Docente` responsável e uma lista de `Discente`s membros, com histórico de cargos (`GrupoMembro` + `Papel`)
- `Oportunidade` → vinculada a um `Curso`, um `Tipo` e um `Docente` coordenador; recebe `Inscricao`es de discentes
- `Solicitacao` → pedido de um `Discente` para aproveitamento de carga horária, com prazo de análise e reenvio

Os status dos fluxos de negócio são controlados pelos enums `Status` (Aprovado, Pendente, Indeferido, Cancelado, Rejeitado) e `StatusOp` (Aberta, Rascunho, Encerrada, Cancelada, Aguarda aprovação, Em execução).

## Pré-requisitos

- JDK 17+
- Maven (ou use o wrapper `mvnw` incluído no projeto)
- PostgreSQL em execução localmente (ou acessível via rede)

## Configuração

1. Clone o repositório e acesse a branch `trabalho-entrega-03`:

   ```bash
   git clone https://github.com/leticiassaif/SpringExtensao.git
   cd SpringExtensao
   git checkout trabalho-entrega-03
   ```

2. Copie o arquivo de exemplo de configuração e ajuste com as suas credenciais do PostgreSQL:

   ```bash
   cp src/main/resources/application-example.properties src/main/resources/application.properties
   ```

   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/myapp
   spring.datasource.username=seu_usuario
   spring.datasource.password=sua_senha
   ```

3. Crie o banco de dados correspondente no PostgreSQL (ex.: `myapp`) antes de subir a aplicação.

## Executando a aplicação

Usando o Maven Wrapper:

```bash
./mvnw spring-boot:run
```

No Windows:

```bash
mvnw.cmd spring-boot:run
```

A aplicação sobe por padrão em `http://localhost:8080`.

## Executando os testes

O projeto conta com testes unitários de controllers e services usando JUnit 5, Mockito e AssertJ, com banco H2 em memória para os testes de integração:

```bash
./mvnw test
```

## Principais endpoints da API

Todos os endpoints têm como prefixo `/api`.

### Usuário (`/api/usuario`)
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/login` | Autentica um usuário |
| POST | `/logout` | Encerra a sessão |
| POST | `/cadastrar/discente` | Cadastra um novo discente |
| POST | `/cadastrar/docente` | Cadastra um novo docente |
| PATCH | `/promover/docente/{id}` | Promove um discente a docente |
| PATCH | `/desativar/{id}` | Desativa um usuário |
| PATCH | `/anonimizar/{id}` | Anonimiza os dados de um usuário |
| GET | `/email/{email}` | Busca usuário por e-mail |
| GET | `/id/{id}` | Busca usuário por ID |
| GET | `/painel/{id}` | Painel de horas do discente |

### Grupo (`/api/usuario`)
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/criar` | Cria um grupo de extensão |
| PATCH | `/aprovar/{idGrupo}` | Aprova um grupo |
| PATCH | `/rejeitar/{id}` | Rejeita um grupo |
| PATCH | `/addmembro/{idGrupo}/{idDiscente}` | Adiciona membro ao grupo |
| PATCH | `/removemembro/{idGrupo}/{idDiscente}` | Remove membro do grupo |
| PATCH | `/atribuircargo/{idGrupo}/{idDiscente}` | Atribui cargo a um membro |
| PATCH | `/removercargo/{idGrupo}/{idDiscente}` | Remove cargo de um membro |
| GET | `/grupo/{id}` | Busca grupo por ID |
| GET | `/lista` | Lista grupos |
| GET | `/lista/membros/{id}` | Lista membros de um grupo |

### Curso (`/api/curso`)
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/cadastrar` | Cadastra um curso |
| GET | `/busca/{id}` | Busca curso por ID |
| GET | `/busca/versao` | Busca versão de currículo |
| GET | `/busca/vigente` | Busca currículo vigente |
| GET | `/historico` | Histórico de currículos |
| POST | `/uce/cadastrar` | Cadastra uma UCE |
| GET | `/uce/busca/{id}` | Busca UCE por ID |

### Oportunidade (`/api/oportunidade`)
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/criar` | Cria uma oportunidade |
| PATCH | `/publicar/{id}` | Publica uma oportunidade |
| PATCH | `/aprovar/{id}` | Aprova uma oportunidade |
| PATCH | `/iniciar/{id}` | Inicia a execução |
| PATCH | `/encerrar/{id}` | Encerra a oportunidade |
| PATCH | `/cancelar/{id}` | Cancela a oportunidade |
| GET | `/oportunidade` | Lista oportunidades |

### Inscrição (`/api/inscricao`)
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/inscrever` | Inscreve um discente em uma oportunidade |
| PATCH | `/aprovar/{id}` | Aprova uma inscrição |
| PATCH | `/rejeitar/{id}` | Rejeita uma inscrição |
| PATCH | `/remover/{id}` | Remove uma inscrição |
| PATCH | `/desistir/{id}` | Discente desiste da inscrição |
| GET | `/lista/oportunidade/{id}` | Lista inscrições de uma oportunidade |
| GET | `/lista/fila-espera/{id}` | Lista fila de espera de uma oportunidade |
| GET | `/lista/discente/{id}` | Lista inscrições de um discente |

### Solicitação (`/api/solicitacao`)
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/submeter` | Submete uma solicitação de carga horária |
| PATCH | `/aprovar/{id}` | Aprova uma solicitação |
| PATCH | `/indeferir/{id}` | Indefere uma solicitação |
| PATCH | `/reenviar/{id}` | Reenvia uma solicitação corrigida |
| GET | `/{id}` | Busca solicitação por ID |
| GET | `/discente/{id}` | Lista solicitações de um discente |
| GET | `/indeferidos/{id}` | Lista solicitações indeferidas de um discente |
| GET | `/pendentes` | Lista solicitações pendentes |

## Tratamento de erros

A API utiliza um `GlobalExceptionHandler` centralizado que converte exceções de negócio em respostas HTTP padronizadas:

- `IllegalArgumentException` → `400 Bad Request`
- `IllegalStateException` → `409 Conflict`
- `SecurityException` → `403 Forbidden`

## Autenticação e sessão

A autorização de operações sensíveis é feita através da classe utilitária `Sessao`, que valida se há um usuário logado na sessão HTTP (`IdUsuarioLogado`) antes de permitir a execução de determinadas ações.

## Autoria

Projeto desenvolvido em grupo por estudantes de Ciência da Computação da UFMA como parte da disciplina de Engenharia de Software / POO.
