# MLB Strikeout Predictor API

Micronaut API em `Java 21 + Maven`, preparada para rodar localmente ou em uma Oracle Compute VM com `Docker Compose + Nginx`.

## Requisitos

- Java 21
- Maven 3.9+
- Docker e Docker Compose para containerização/deploy

## Rodando localmente

1. Copie `.env.example` para `.env`.
2. Ajuste `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` e `API_KEY`.
3. Rode os testes:

```bash
mvn -Dmaven.test.skip=false test
```

4. Gere o jar:

```bash
mvn -Dmaven.test.skip=true package
java -jar target/mlb-strikeout-predictor-api-0.1.0.jar
```

Por padrão, a aplicação sobe em `http://localhost:8080`.

## Build do jar

```bash
mvn -Dmaven.test.skip=true package
```

O artefato gerado fica em `target/mlb-strikeout-predictor-api-0.1.0.jar`.

## Subindo com Docker Compose

1. Copie `.env.example` para `.env`.
2. Ajuste as variáveis.
3. Suba os containers:

```bash
docker compose --env-file .env up -d --build
```

Serviços:

- `api`: aplicação Micronaut na porta interna `8080`
- `nginx`: proxy reverso exposto em `NGINX_PORT` (default `80`)

## Validando localmente com Postgres real em container

Para testar a stack completa sem depender do banco externo:

1. Copie `.env.local.example` para `.env.local`.
2. Suba com o override local:

```bash
docker compose -f docker-compose.yml -f docker-compose.local.yml --env-file .env.local up -d --build
```

3. Verifique a saúde:

```bash
curl http://localhost:8081/nginx-health
curl http://localhost:8081/health
```

No override local, a API fala com `postgres:5432` dentro da rede Docker e o Postgres também fica exposto no host em `LOCAL_DB_PORT` (default `5433`) para inspeção manual.

4. Derrube a stack quando terminar:

```bash
docker compose -f docker-compose.yml -f docker-compose.local.yml --env-file .env.local down -v
```

## Deploy na Oracle VM

Passo mínimo para uma VM Ubuntu:

1. Instale `docker` e `docker compose`.
2. Copie este diretório para a VM.
3. Crie `.env` com credenciais reais.
4. Execute:

```bash
chmod +x deploy.sh
./deploy.sh
```

## Variáveis principais

- `API_KEY`: chave exigida pelo filtro `X-API-Key`
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`: conexão com Postgres
- `FRONTEND_ORIGIN`: origem liberada no CORS
- `NGINX_PORT`: porta pública do Nginx
- `SERVER_PORT`: porta interna da API

## Observações de produção

- O `nginx.conf` está pronto para proxy reverso HTTP. Se quiser TLS na VM, o próximo passo é plugar certificado e bloco `server` para `443`.
- A VM precisa conseguir acessar o Postgres externo na porta `5432`.
- Os endpoints `/api/**` continuam exigindo header `X-API-Key`.
