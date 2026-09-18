# Biblioteca Viva — Backend

API RESTful em Spring Boot para a plataforma **Biblioteca Viva**, dedicada à publicação, preservação e difusão de produções literárias, artísticas e audiovisuais de estudantes de escolas públicas, além de gestão de clubes de leitura, notícias e comunidade escolar.

---

## Sumário

- [Tecnologias](#tecnologias)
- [Arquitetura e Módulos](#arquitetura-e-módulos)
- [Perfis de Acesso (RBAC)](#perfis-de-acesso-rbac)
- [Catálogo e Subtipos de Obras](#catálogo-e-subtipos-de-obras)
- [Fluxo de Autenticação e Segurança](#fluxo-de-autenticação-e-segurança)
- [Como Executar](#como-executar)
  - [Pré-requisitos e Variáveis de Ambiente](#pré-requisitos-e-variáveis-de-ambiente)
  - [Executando com Docker Compose](#executando-com-docker-compose)
  - [Executando Localmente com Maven](#executando-localmente-com-maven)
- [Banco de Dados, Migrations e Seed](#banco-de-dados-migrations-e-seed)
- [Testes Automatizados](#testes-automatizados)
- [Documentação da API e Coleção Bruno](#documentação-da-api-e-coleção-bruno)

---

## Tecnologias

- **Java 21** & **Spring Boot 4.0.3**
- **Spring Security** (Autenticação JWT Stateless, Rate Limiting, RBAC)
- **Spring Data JPA / Hibernate 7** (Herança Polimórfica `JOINED`, Projeções SQL)
- **PostgreSQL 16+** (Produção/Docker) & **H2 Database** (Testes)
- **Flyway** (Versionamento e Migrações do Esquema do Banco)
- **Cloudinary** (Armazenamento em nuvem para uploads de imagens e artes)
- **Resend API** (Envio transacional de e-mails para recuperação de senha)
- **Apache POI** (Processamento e importação em massa de planilhas `.xlsx`)
- **MapStruct & Lombok** (Mapeamento de DTOs e boilerplate)
- **SpringDoc OpenAPI & Scalar** (Documentação interativa de API)
- **Coleção Bruno** (Suíte completa de requisições HTTP para testes locais)

---

## Arquitetura e Módulos

O backend adota uma arquitetura em camadas bem definida:

```
src/main/java/org/bibliotecaviva/backend/
├── api/
│   ├── config/          # Segurança (SecurityConfig, JwtAuthFilter, RateLimitingFilter, CORS)
│   ├── controller/      # Endpoints REST (Auth, Admin, User, Work, Comment, BookClub, News)
│   └── handler/         # GlobalExceptionHandler com respostas padronizadas RFC 7807
├── application/
│   ├── dtos/            # DTOs estruturados em Java Records (Request e Response)
│   ├── mappers/         # Mapeadores MapStruct para conversão de Entidade <-> DTO
│   └── services/        # Regras de negócio, transações (@Transactional) e integrações
└── domain/
    ├── entities/        # Entidades JPA (Work com herança JOINED, User, Comment, BookClub, etc.)
    ├── enums/           # Enums de domínio (Role, Status, WorkTypes)
    └── exceptions/      # Exceções de domínio personalizadas
```

---

## Perfis de Acesso (RBAC)

A plataforma possui três papéis de acesso:

| Perfil | Permissões Principais | Status Inicial no Cadastro |
|---|---|---|
| `ALUNO` | Curtir obras, criar comentários, responder em suas próprias interações e inscrever-se em Clubes do Livro. | `PENDING` (requer aprovação de um Administrador) |
| `CURADOR` | Todas as permissões de Aluno + publicar e editar Obras de qualquer categoria, gerenciar Clubes do Livro e Notícias. | `ACTIVE` (cadastrado exclusivamente por Administrador) |
| `ADMIN` | Acesso irrestrito: aprovação/bloqueio de usuários, exclusão de contas, importação em lote via planilha `.xlsx` e dashboard de métricas. | `ACTIVE` |

---

## Catálogo e Subtipos de Obras

As obras são persistidas na tabela base `obras` com estratégia de herança polimórfica **JPA `JOINED`**. Cada obra pertence a uma categoria específica, aceitando autoria por usuário cadastrado (`authorEmail`) **ou** autor externo (`authorName`), nunca ambos:

| Categoria | Subtipo | Rota de Criação | Rota de Edição | Formato | Campos Principais |
|---|---|---|---|---|---|
| **Textual** | Artigo | `POST /work/articles` | `PUT /work/articles/{id}` | JSON | `content` |
| **Textual** | Poema | `POST /work/poems` | `PUT /work/poems/{id}` | JSON | `content`, `rhymeScheme`, `poemType` |
| **Textual** | Cordel | `POST /work/cordels` | `PUT /work/cordels/{id}` | JSON | `content`, `rhymeScheme`, `artName` (ilustração opcional) |
| **Textual** | Redação | `POST /work/essays` | `PUT /work/essays/{id}` | JSON | `content`, `rate` (0-1000), `theme`, `feedback` |
| **Textual** | Conto Curto | `POST /work/short-stories` | `PUT /work/short-stories/{id}` | JSON | `content` |
| **Textual** | Narrativa | `POST /work/tales` | `PUT /work/tales/{id}` | JSON | `content`, `genre` |
| **Textual** | Outro | `POST /work/others` | `PUT /work/others/{id}` | Multipart | `data` (JSON), `image` (arquivo opcional) |
| **Visual** | Arte | `POST /work/arts` | `PUT /work/arts/{id}` | Multipart | `data` (JSON), `image` (obrigatória na criação) |
| **Visual** | Infográfico | `POST /work/infographics` | `PUT /work/infographics/{id}` | Multipart | `data` (JSON), `image` (obrigatória na criação) |
| **Audiovisual** | Multimídia | `POST /work/multimedias` | `PUT /work/multimedias/{id}` | JSON | `url`, `duration` (ISO 8601, ex: `PT3M30S`) |
| **Audiovisual** | Literatura em Libras | `POST /work/libra-literatures` | `PUT /work/libra-literatures/{id}` | JSON | `url`, `duration` (ISO 8601) |

### Endpoints Globais de Obras
- `GET /work`: Consulta paginada com filtro opcional por subtipo (`?type=POEM`, `?type=ART`, etc.).
- `GET /work/{id}`: Detalhes da obra com incremento atômico do contador de visualizações (`viewCount`).
- `GET /work/home`: Dashboard público da página inicial com contadores e destaques por categoria.
- `GET /work/liked`: Lista os IDs das obras curtidas pelo usuário autenticado.
- `PUT /work/{id}/like` e `DELETE /work/{id}/like`: Curtir ou descurtir uma obra.
- `DELETE /work/{id}`: Exclusão lógica/física com limpeza automática de curtidas vinculadas e desassociação segura de ilustrações em cordéis.

---

## Fluxo de Autenticação e Segurança

- **Token Duplo (Access + Refresh Token):**
  - **`accessToken`**: JWT com validade de 15 minutos enviado no corpo da resposta e repassado nas requisições no cabeçalho `Authorization: Bearer <token>`.
  - **`refreshToken`**: Token opaco armazenado no banco com hash SHA-256 e transmitido ao cliente em cookie seguro `HttpOnly` (`refresh_token`).
  - **Rotação de Refresh Token:** a rota `POST /auth/refresh` valida o cookie, aplica bloqueio pessimista no banco, revoga o token anterior e emite um novo par de tokens.
  - **Logout (`POST /auth/logout`):** revoga o token ativo no banco e limpa o cookie HttpOnly.
- **Recuperação de Senha Segura:**
  - `POST /auth/password-reset/request`: gera um desafio com código de 6 dígitos enviado por e-mail com limite de tentativas e tempo de expiração.
  - `POST /auth/password-reset/verify`: valida o código e emite um token temporário.
  - `POST /auth/password-reset/confirm`: atualiza a senha com hash BCrypt + Pepper e incrementa a `sessionVersion` do usuário, invalidando instantaneamente todos os refresh tokens anteriores.
- **Proteção contra Abuso (Rate Limiting):**
  - Filtro em memória (`RateLimitingFilter`) que protege rotas sensíveis (`/login`, `/refresh`, `/register`, `/password-reset/*`) contra ataques de força bruta.

---

## Como Executar

### Pré-requisitos e Variáveis de Ambiente

Crie um arquivo `.env` na raiz do projeto (ou configure em seu ambiente) com base nas variáveis esperadas:

```env
# Banco de Dados
DB_USERNAME=postgres
DB_PASSWORD=postgres
DB_NAME=bibliotecaviva
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bibliotecaviva

# Segurança & JWT
JWT_SECRET=coloque_aqui_uma_chave_secreta_jwt_de_pelo_menos_256_bits_bem_longa
JWT_EXPIRATION=900000
REFRESH_TOKEN_EXPIRATION=604800000
PASSWORD_RESET_PEPPER=pimenta_secreta_para_recuperacao_de_senha
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

# Integrações Externas
CLOUDINARY_URL=cloudinary://api_key:api_secret@cloud_name
RESEND_API_KEY=re_sua_chave_resend
RESEND_FROM_EMAIL=contato@seudominio.org
```

### Executando com Docker Compose

Para subir a aplicação e o banco de dados PostgreSQL simultaneamente:

```bash
docker compose up --build
```

- A API estará disponível em: `http://localhost:8080`
- O banco PostgreSQL estará mapeado na porta `5432`

### Executando Localmente com Maven

Caso já possua um PostgreSQL em execução local:

```bash
# Baixar dependências e compilar
./mvnw clean compile

# Iniciar aplicação com perfil dev
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## Banco de Dados, Migrations e Seed

O controle do esquema do banco de dados é **estritamente gerenciado pelo Flyway**:
- O Hibernate está configurado com `ddl-auto: validate` em todos os perfis.
- As migrações ficam em `src/main/resources/db/migration/`:
  - `V1__init.sql`: Esquema base (tabelas de usuários, obras polimórficas, clubes, comentários, curtidas).
  - `V2__create_news_table.sql`: Tabela para gestão de notícias.
  - `V3__add_cloudinary_public_id.sql`: Suporte a identificadores públicos do Cloudinary para limpeza de imagens órfãs.
- No perfil de desenvolvimento (`dev`), o Flyway também carrega o seed em `src/main/resources/db/seed/V100__dev_seed_data.sql`.

### Usuários de Teste Pré-cadastrados no Seed

A senha de todas as contas do seed de desenvolvimento é: `123456`

| Usuário | E-mail | Perfil | Status |
|---|---|---|---|
| Administrador | `admin@teste.com` | `ADMIN` | `ACTIVE` |
| Professor/Curador | `professor@teste.com` | `CURADOR` | `ACTIVE` |
| Aluno 1 | `aluno1@teste.com` | `ALUNO` | `ACTIVE` |
| Aluno 2 | `aluno2@teste.com` | `ALUNO` | `ACTIVE` |
| Aluno 3 | `aluno3@teste.com` | `ALUNO` | `ACTIVE` |

---

## Testes Automatizados

A suíte de testes utiliza JUnit 5, Mockito e banco H2 em memória com isolamento transacional:

```bash
# Executar todos os testes
./mvnw test

# Executar uma classe de teste específica
./mvnw test -Dtest=WorkServiceLikeTest
```

---

## Documentação da API e Coleção Bruno

Com a aplicação rodando, a documentação interativa pode ser acessada em:

- **Scalar API UI:** [http://localhost:8080/scalar](http://localhost:8080/scalar)
- **Swagger UI:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI JSON Spec:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### Coleção Bruno ([bruno-collection/](bruno-collection/))

A pasta `bruno-collection/` contém a coleção completa com **100% dos 78 endpoints representados**:
- **Autenticação:** Login, Logout, Refresh Token com cookie HttpOnly, Me e Recuperação de Senha.
- **Painel Admin:** Aprovação, bloqueio e rejeição de contas, listagem de usuários e importação XLSX via multipart.
- **Clubes do Livro:** Ciclo completo de criação, atualização, participantes, inscrições e avaliações.
- **Obras:** Criação e atualização para todos os 11 subtipos de obras (textuais, visuais e audiovisuais).
- **Comentários e Notícias:** Fluxo completo com suporte a respostas de curadoria e curtidas.
- Todos os arquivos e pastas da coleção possuem documentação interna (`docs`) descrevendo parâmetros, cabeçalhos e respostas esperadas.
