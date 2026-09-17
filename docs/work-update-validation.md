# Validação de autoria e subtipo

Criação e atualização de obras exigem exatamente um dos campos authorEmail e
authorName. Ambos ausentes, ambos informados ou autoria vazia resultam em 400.
Na atualização, omitir autoria não preserva o autor: a requisição é rejeitada.
A troca entre usuário cadastrado e nome livre continua permitida.

O subtipo da entidade deve corresponder ao DTO da rota. A verificação ocorre
antes do mapeamento e de uploads. Um ID de artigo enviado à rota de artes,
por exemplo, retorna 400 no contrato ApiErrorResponse, sem alterar a obra.

Ao adicionar subtipos, usar requireSubtype antes de chamar partialUpdate.
