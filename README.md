# AuroraOrg - Sistema de Abertura de Chamados

Sistema backend para gerenciamento de chamados e suporte técnico, desenvolvido com Spring Boot e Java 25.

## 📋 Descrição

O AuroraOrg é uma API REST para gerenciamento de chamados de suporte, permitindo que usuários abram, acompanhem e gerenciem solicitações de suporte técnico. O sistema utiliza autenticação JWT para segurança e PostgreSQL como banco de dados.

## 🛠 Stack Tecnológica

- **Java 25** - Linguagem de programação
- **Spring Boot 4.0.6** - Framework principal
- **Spring Data JPA** - Acesso ao banco de dados
- **Spring Security** - Segurança e autenticação
- **PostgreSQL 18** - Banco de dados relacional
- **Flyway** - Gerenciamento de migrations
- **JWT (jjwt)** - Autenticação baseada em tokens
- **SpringDoc OpenAPI** - Documentação da API (Swagger)
- **Lombok** - Redução de boilerplate
- **Testcontainers** - Testes de integração com containers
- **Gradle** - Gerenciador de dependências e build

## 🚀 Pré-requisitos

- **Java 25** ou superior
- **PostgreSQL 18** ou superior
- **Gradle 8.x** (ou use o wrapper `./gradlew`)
- **Docker** (para testes com Testcontainers)

## 🔧 Configuração

### 1. Clone o repositório

```bash
git clone <url-do-repositorio>
cd auroraorg
```

### 2. Configure as variáveis de ambiente

Crie um arquivo `.env` na raiz do projeto baseado no exemplo abaixo:

```bash
# Server
SERVER_HOST=localhost
APP_PORT=8080

# Database PostgreSQL
DB_HOST=localhost
DB_PORT=5432
DB_NAME=auroraorg
DB_USER=postgres
DB_PASSWORD=sua_senha_aqui

# JWT
JWT_SECRET=sua_chave_secreta_aqui
JWT_EXPIRATION=3600

# Admin User
ADMIN_EMAIL=admin@exemplo.com
ADMIN_PASSWORD=sua_senha_aqui
ADMIN_NAME=Admin AuroraOrg
ADMIN_ID=uuid-gerado-aqui

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:4200
```

### 3. Configure o banco de dados

Crie o banco de dados PostgreSQL:

```sql
CREATE DATABASE auroraorg;
```

As migrations do Flyway serão executadas automaticamente na inicialização da aplicação.

## ▶️ Executando a Aplicação

### Usando Gradle Wrapper

```bash
# Compilar o projeto
./gradlew build

# Executar a aplicação
./gradlew bootRun
```

### Usando Docker (opcional)

```bash
# Build da imagem
docker build -t auroraorg .

# Executar o container
docker run -p 8080:8080 --env-file .env auroraorg
```

A aplicação estará disponível em `http://localhost:8080`

## 📚 Documentação da API

A documentação interativa da API está disponível através do Swagger UI:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

## 🏗 Estrutura do Projeto

```
auroraorg/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── br/com/api/auroraorg/
│   │   │       ├── AuroraorgApplication.java  # Classe principal
│   │   │       ├── auth/                      # Módulo de autenticação
│   │   │       │   ├── controller/
│   │   │       │   ├── dto/
│   │   │       │   ├── security/
│   │   │       │   └── service/
│   │   │       ├── user/                      # Módulo de usuários
│   │   │       │   ├── controller/
│   │   │       │   ├── dto/
│   │   │       │   ├── entity/
│   │   │       │   ├── enums/
│   │   │       │   ├── mapper/
│   │   │       │   ├── repository/
│   │   │       │   └── service/
│   │   │       └── shared/                    # Componentes compartilhados
│   │   │           ├── config/
│   │   │           ├── exception/
│   │   │       └── resources/
│   │   │           ├── application.yml        # Configurações
│   │   │           └── db/migration/         # Migrations Flyway
│   └── test/
│       └── java/                              # Testes
├── build.gradle                                # Configuração Gradle
├── settings.gradle
├── .env                                        # Variáveis de ambiente
└── README.md
```

## 🔒 Autenticação

A API utiliza autenticação baseada em JWT (JSON Web Tokens). Para acessar os endpoints protegidos, você precisa:

1. Fazer login para obter o token JWT
2. Incluir o token no header `Authorization` das requisições

```bash
# Exemplo de login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@exemplo.com","password":"senha"}'

# Exemplo de requisição com token
curl -X GET http://localhost:8080/api/chamados \
  -H "Authorization: Bearer <seu-token-jwt>"
```

## 🧪 Testes

### Executar todos os testes

```bash
./gradlew test
```

### Executar testes específicos

```bash
# Testes unitários
./gradlew test --tests "*Test"

# Testes de integração
./gradlew test --tests "*IntegrationTest"
```

### Relatório de cobertura

```bash
./gradlew test jacocoTestReport
```

O relatório será gerado em `build/reports/jacoco/test/html/index.html`

## 📝 Endpoints Principais

### Autenticação
- `POST /api/auth/login` - Login e geração de token JWT
- `POST /api/auth/register` - Registro de novo usuário

### Usuários
- `GET /api/users` - Listar usuários (autenticado)
- `GET /api/users/{id}` - Buscar usuário por ID
- `PUT /api/users/{id}` - Atualizar usuário
- `DELETE /api/users/{id}` - Deletar usuário

### Chamados
- `POST /api/chamados` - Criar novo chamado
- `GET /api/chamados` - Listar chamados
- `GET /api/chamados/{id}` - Buscar chamado por ID
- `PUT /api/chamados/{id}` - Atualizar chamado
- `DELETE /api/chamados/{id}` - Deletar chamado

## 🛡️ Segurança

- Autenticação JWT com tokens expiráveis
- Validação de inputs com Bean Validation
- Proteção contra SQL Injection (JPA com queries parametrizadas)
- CORS configurado para origens permitidas
- Senhas hashadas com BCrypt
- Rate limiting recomendado para produção

## 📊 Monitoramento

A aplicação expõe métricas através do Spring Boot Actuator:

- **Health Check**: http://localhost:8080/actuator/health
- **Métricas**: http://localhost:8080/actuator/metrics
- **Info**: http://localhost:8080/actuator/info

## 🔄 Migrations

O sistema usa Flyway para gerenciamento de versões do banco de dados. As migrations são:

- Localizadas em `src/main/resources/db/migration/`
- Executadas automaticamente na inicialização
- Versionadas com prefixo `V{versão}__{descricao}.sql`

Para adicionar uma nova migration:

1. Crie um arquivo SQL na pasta `db/migration`
2. Siga o padrão de nome: `V{versão}__{descricao}.sql`
3. A migration será executada na próxima inicialização

## 🤝 Contribuindo

1. Fork o repositório
2. Crie uma branch para sua feature (`git checkout -b feature/nova-feature`)
3. Faça commit das suas mudanças (`git commit -m 'feat: adiciona nova feature'`)
4. Push para a branch (`git push origin feature/nova-feature`)
5. Abra um Pull Request

### Padrões de Commit

Siga o padrão Conventional Commits:

- `feat:` - Nova funcionalidade
- `fix:` - Correção de bug
- `docs:` - Mudanças na documentação
- `style:` - Mudanças de formatação
- `refactor:` - Refatoração de código
- `test:` - Adição ou modificação de testes
- `chore:` - Mudanças no build process

## 📄 Licença

Este projeto está sob a licença MIT.

## 👥 Autores

- AuroraOrg Team

## 🆘 Suporte

Para dúvidas ou problemas, abra uma issue no repositório ou entre em contato através da equipe de desenvolvimento.
