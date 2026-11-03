# Ruínas — 50% por vila

A seleção de novas Ruínas Ancestrais usa um sorteio determinístico de 50% para
cada origem de vila vanilla. O resultado depende da seed e coordenadas, sem
consultar vizinhos, sem distância mínima padrão entre ruínas e sem exclusão
padrão perto do spawn. Não muda ao recarregar ou inverter a ordem de geração.
50% é probabilidade por vila, não garantia de metade exata numa amostra pequena.

A seleção e os comandos de candidatos usam a mesma política. Geração ainda
valida a vila real, seu bioma e um espaço decorativo seguro; uma selecionada sem
local seguro pode não receber a construção. Não se destrói uma casa para forçar
o resultado. A construção continua sendo a ruína com pedestal ancestral, não
uma Forja ativada gratuita; provas, crafting e propriedade não mudam.

Vilas/chunks existentes não recebem reconstrução retroativa. Ruínas e peças
antigas preservam identidade e dados. Para testar, use um mundo Normal novo ou
áreas novas. Datapacks personalizados ainda podem alterar os filtros restantes.

Arquivos: `RuinPlacement`, `RuinSelection`, `RuinStructure`, dois JSONs de worldgen,
`AncestralResonanceChecks` e manual. Campos antigos de codec foram mantidos para
leitura; o espaçamento legado não controla mais a seleção nova. Os algoritmos
históricos continuam apenas em testes de regressão, identificados como legados.

Validação: teste determinístico em 10.000 coordenadas e comparação entre seeds,
além da suíte existente. Build: **BUILD SUCCESSFUL**. A amostra sintética selecionou **5.001/10.000** origens, e as 54 verificações de ressonância/seleção passaram. Não foi
executada geração real de chunks nem homologação visual nesta alteração.
