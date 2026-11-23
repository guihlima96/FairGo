# Contexto e Regras do Projeto

## Stack Tecnológica
- **Linguagem:** Java 17 (use Records para DTOs, Pattern Matching e recursos modernos do Java 17 quando adequado)
- **Framework:** Spring Boot 3.x (Web, Data JPA, Validation)
- **Build & Dependências:** Maven (`pom.xml`)
- **Banco de Dados:** PostgreSQL

## Arquitetura e Padrões de Código
- Organize o código em camadas claras: `controller`, `service`, `repository`, `domain` (ou `model`), `dto` e `exception`.
- **Nunca exponha entidades JPA diretamente** nos Controllers; sempre utilize DTOs de entrada (Request) e saída (Response).
- Utilize injeção de dependência via construtor (evite `@Autowired` direto em atributos).
- Valide entradas usando Jakarta Validation (`@Valid`, `@NotNull`, `@NotBlank`, etc.) e trate erros de forma centralizada com `@RestControllerAdvice`.

## Banco de Dados (PostgreSQL)
- Respeite nomenclatura `snake_case` para tabelas e colunas no banco de dados.
- Caso o projeto utilize Flyway ou Liquibase, sempre crie scripts de migration versionados para alterações de schema em vez de depender de `ddl-auto=update`.
- **Segurança:** Nunca altere nem commite senhas reais em `application.properties` ou `application.yml`. Use variáveis de ambiente.

## Comandos Úteis (Maven)
- **Compilar:** `./mvnw clean compile` (ou `mvn clean compile`)
- **Rodar testes:** `./mvnw test` (ou `mvn test`)
- **Empacotar:** `./mvnw clean package -DskipTests`

## Fluxo de Trabalho (Modo Work)
1. Antes de escrever código complexo, apresente um breve plano dos arquivos que serão criados ou alterados.
2. Após implementar uma funcionalidade ou correção, sempre execute `mvn test` (ou compile o projeto) para garantir que não há erros de compilação ou testes quebrados.
3. Mantenha commits atômicos e mensagens claras seguindo o padrão Conventional Commits (`feat:`, `fix:`, `refactor:`).
