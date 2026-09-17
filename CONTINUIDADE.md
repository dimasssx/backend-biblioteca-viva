# Continuidade do trabalho

Data: 16 de setembro de 2026

## Correção realizada

Foi corrigida a exclusão de obras que possuem curtidas ou são utilizadas como
ilustração de um cordel.

O método `WorkService.delete()` agora executa, dentro da mesma transação, as
seguintes operações:

1. Confirma que a obra existe.
2. Remove os registros da tabela `likes` associados à obra.
3. Remove a referência da obra nos cordéis que a utilizam como ilustração.
4. Exclui a obra.

A política definida para ilustrações é preservar o cordel. Quando uma arte usada
como ilustração é excluída, o campo de ilustração do cordel passa a ser nulo.

O método que desvincula as ilustrações usa `flushAutomatically` e
`clearAutomatically` para evitar que o contexto JPA mantenha um cordel apontando
para uma arte já excluída.

## Arquivos alterados nesta correção

- `src/main/java/org/bibliotecaviva/backend/application/services/WorkService.java`
- `src/main/java/org/bibliotecaviva/backend/persistence/repository/WorkRepository.java`
- `src/test/java/org/bibliotecaviva/backend/application/services/WorkServiceTest.java`
- `src/test/java/org/bibliotecaviva/backend/integration/WorkControllerIntegrationTest.java`
- `README.md`

Nenhuma migration foi criada ou alterada. A solução remove os vínculos
explicitamente na camada de serviço e mantém `V1__init.sql` intacta.

## Testes adicionados

- Exclusão de uma obra curtida, confirmando a remoção da obra e dos vínculos de
  curtidas.
- Exclusão de uma arte usada por um cordel, confirmando que a arte é removida e
  que o cordel permanece com ilustração nula.
- Testes unitários verificando a ordem lógica de limpeza antes de `deleteById` e
  a ausência dessas chamadas quando a obra não existe.

## Validação

Os testes direcionados passaram:

```bash
./mvnw -B -Dtest=WorkServiceTest,WorkControllerIntegrationTest test
```

Resultado: 51 testes, sem falhas, erros ou testes ignorados.

A suíte completa também passou:

```bash
./mvnw -B test
```

Resultado: 305 testes, sem falhas, erros ou testes ignorados.

## Estado da worktree

As alterações desta correção ainda não foram commitadas. A worktree também
contém mudanças em `bruno-collection/work/bY ID.yml` e
`bruno-collection/work/deleteWork.yml` que já estavam presentes e não fizeram
parte desta correção.
