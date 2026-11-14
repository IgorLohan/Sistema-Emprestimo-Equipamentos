# Sistema de Empréstimo de Equipamentos

Microsserviços síncronos para empréstimo de notebooks, projetores e tablets. Cada serviço de domínio tem banco PostgreSQL próprio. A comunicação entre eles é HTTP com OpenFeign e descoberta pelo Eureka. O cliente entra somente pela API Gateway.

Autenticação JWT não faz parte desta entrega.

## Arquitetura

```
Cliente
  |
  v
api-gateway :8080  ---->  discovery-server (Eureka) :8761
  |                         ^
  | lb://                   | registro
  +-- msusuario      :8081 --+   banco db_msusuario
  +-- msequipamento  :8082 --+   banco db_msequipamento
  +-- msemprestimo   :8083 --+   banco db_msemprestimo
         |
         | OpenFeign (sem URL fixa)
         +-- GET  msusuario      /api/v1/usuarios/{id}
         +-- GET  msequipamento  /api/v1/equipamentos/{id}
         +-- PUT  msequipamento  /api/v1/equipamentos/{id}/reservas/{emprestimoId}
         +-- DELETE msequipamento /api/v1/equipamentos/{id}/reservas/{emprestimoId}
```

O `msemprestimo` guarda apenas `usuarioId` e `equipamentoId`. Não há chave estrangeira nem `@ManyToOne` entre os bancos.

## Stack

- Java 25
- Spring Boot 4.1.2
- Spring Cloud 2025.1.4
- Spring Data JPA, Validation, Flyway
- Eureka Server, Spring Cloud Gateway (`lb://`) e OpenFeign com LoadBalancer
- PostgreSQL 18, um banco por microsserviço

## Portas

| Aplicação | Porta | Banco |
| --- | --- | --- |
| discovery-server | 8761 | — |
| api-gateway | 8080 | — |
| msusuario | 8081 | `db_msusuario` |
| msequipamento | 8082 | `db_msequipamento` |
| msemprestimo | 8083 | `db_msemprestimo` |

## Preparar o PostgreSQL

O PostgreSQL local precisa estar em execução na porta 5432. O usuário padrão é `postgres`, no host `localhost`. A senha fica apenas no `application.yaml` de cada microsserviço e pode ser substituída pela variável `POSTGRES_PASSWORD`, sem registrá-la neste arquivo.

Crie os três bancos:

```powershell
.\scripts\criar-bancos.ps1
```

O Flyway cria as tabelas `tb_usuarios`, `tb_equipamentos` e `tb_emprestimos` na primeira subida de cada serviço.

## Executar

Em terminais separados, nesta ordem:

```powershell
cd discovery-server
.\mvnw.cmd spring-boot:run
```

```powershell
cd msusuario
.\mvnw.cmd spring-boot:run
```

```powershell
cd msequipamento
.\mvnw.cmd spring-boot:run
```

```powershell
cd msemprestimo
.\mvnw.cmd spring-boot:run
```

```powershell
cd api-gateway
.\mvnw.cmd spring-boot:run
```

O script `.\scripts\subir.ps1` abre as cinco janelas. Se `POSTGRES_PASSWORD` estiver definida na sessão, ela prevalece sobre a senha do `application.yaml`.

Confira o registro em [http://localhost:8761](http://localhost:8761). As instâncias `MSUSUARIO`, `MSEQUIPAMENTO`, `MSEMPRESTIMO` e `API-GATEWAY` devem aparecer antes dos testes pela Gateway.

## Endpoints do cliente

Todos passam pela Gateway em `http://localhost:8080`.

| Método | Rota | Função |
| --- | --- | --- |
| POST | `/api/v1/usuarios` | Cadastrar usuário |
| GET | `/api/v1/usuarios/{id}` | Consultar usuário |
| POST | `/api/v1/equipamentos` | Cadastrar equipamento |
| GET | `/api/v1/equipamentos/{id}` | Consultar equipamento |
| GET | `/api/v1/equipamentos/disponiveis` | Listar disponíveis |
| PUT | `/api/v1/equipamentos/{id}/reservas/{emprestimoId}` | Reservar, de forma idempotente |
| DELETE | `/api/v1/equipamentos/{id}/reservas/{emprestimoId}` | Liberar reserva, de forma idempotente |
| POST | `/api/v1/emprestimos` | Solicitar empréstimo |
| GET | `/api/v1/emprestimos/{id}` | Consultar empréstimo |
| GET | `/api/v1/emprestimos/usuarios/{usuarioId}` | Empréstimos do usuário |
| PATCH | `/api/v1/emprestimos/{id}/devolucao` | Registrar devolução |

E-mail e matrícula são únicos em `msusuario`. O e-mail é gravado em minúsculas. Patrimônio é único em `msequipamento`.

## Fluxo de empréstimo

1. O cliente envia `POST /api/v1/emprestimos` para a Gateway, com `usuarioId` e `equipamentoId`.
2. A Gateway encaminha a chamada para `lb://msemprestimo`.
3. O `msemprestimo` consulta o usuário no `msusuario`. Se não existir, responde `404`.
4. Consulta o equipamento e pede a reserva atômica no `msequipamento`, usando o id do empréstimo que será gravado. Equipamento inexistente responde `404`. Equipamento ocupado por outra reserva responde `409`.
5. Só depois da reserva o empréstimo é gravado com status `ATIVO` e a resposta é `201`.
6. Na devolução, o `msemprestimo` pede a liberação e marca o empréstimo como `DEVOLVIDO`. Repetir a devolução não libera de novo.

A reserva usa bloqueio pessimista no PostgreSQL (`SELECT ... FOR UPDATE`). Repetir o `PUT` com o mesmo `emprestimoId` devolve o equipamento já reservado, sem reservar outra vez. O campo `reservaEmprestimoId` fica só no banco de equipamentos e não é chave estrangeira para o outro serviço.

### Exemplos

```http
POST http://localhost:8080/api/v1/usuarios
Content-Type: application/json

{
  "nome": "Ana Souza",
  "email": "ana.souza@escola.edu",
  "matricula": "MAT1001"
}
```

```http
POST http://localhost:8080/api/v1/equipamentos
Content-Type: application/json

{
  "nome": "Notebook Lab 01",
  "tipo": "Notebook",
  "patrimonio": "PAT-1001"
}
```

```http
POST http://localhost:8080/api/v1/emprestimos
Content-Type: application/json

{
  "usuarioId": "11111111-1111-1111-1111-111111111111",
  "equipamentoId": "22222222-2222-2222-2222-222222222222"
}
```

Resposta de sucesso:

```http
HTTP/1.1 201 Created

{
  "id": "33333333-3333-3333-3333-333333333333",
  "usuarioId": "11111111-1111-1111-1111-111111111111",
  "equipamentoId": "22222222-2222-2222-2222-222222222222",
  "dataEmprestimo": "2026-11-14T09:30:00",
  "dataDevolucao": null,
  "status": "ATIVO"
}
```

```http
PATCH http://localhost:8080/api/v1/emprestimos/{id}/devolucao
```

## Erros tratados

| Situação | HTTP |
| --- | --- |
| Corpo inválido ou identificador malformado | 400 |
| Usuário, equipamento ou empréstimo inexistente | 404 |
| E-mail, matrícula ou patrimônio duplicado | 409 |
| Equipamento indisponível ou reserva de outro empréstimo | 409 |
| Serviço dependente fora do ar | 503 |
| Resposta inesperada do serviço dependente | 502 |

## Collection

A variável `baseUrl` aponta para `http://localhost:8080`. Execute a pasta **Fluxo feliz** de cima para baixo: as respostas gravam os ids para as próximas chamadas.

- Postman: importe `collections/postman/Sistema-Emprestimo-Equipamentos.postman_collection.json`.
- Insomnia: importe `collections/insomnia/Sistema-Emprestimo-Equipamentos.insomnia.json`.
- Bruno: abra a pasta `collections/bruno` como collection e selecione o ambiente `local`.

## Limites de consistência

Os bancos são independentes. Não existe transação distribuída.

- Se o `msemprestimo` reservar o equipamento e falhar ao gravar o empréstimo, ele tenta liberar a reserva antes de responder. Se o processo cair entre a reserva e a gravação, o equipamento permanece reservado sem empréstimo correspondente. Nesse caso, libere com `DELETE /api/v1/equipamentos/{id}/reservas/{emprestimoId}`.
- Se a liberação da devolução for concluída e a atualização do empréstimo falhar, uma nova chamada de devolução funciona: a liberação é idempotente quando o equipamento já está livre, e o status só muda de `ATIVO` para `DEVOLVIDO` uma vez.
- Apagar um usuário ou um equipamento no serviço de origem não apaga empréstimos já registrados. O empréstimo guarda somente os identificadores.
