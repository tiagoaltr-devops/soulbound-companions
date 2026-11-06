# Validação da entrega

Execução local em 05/11/2026, JDK 21. Ferramentas e relatórios adicionados apenas em `tools/mod-analyzer/` e `research/external-mods/`. Alterações de produção que já estavam presentes na árvore foram preservadas. Não houve commit, push, migração ou execução do mod externo.

## Comandos e resultados

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/mod-analyzer/test.ps1
# PASS: 47 checks

java tools/mod-analyzer/ModAnalyzer.java
# 51 candidatos concretos catalogados; 109 classes de entidades identificadas por herança

java tools/mod-analyzer/ModAnalyzer.java --extract research/external-mods/work/extracted
# Extração local seletiva executada; 2687 arquivos, sem classes Java extraídas

.\gradlew.bat build
# BUILD SUCCESSFUL; verificações existentes do projeto passaram
```

Os testes usam JARs sintéticos próprios para metadados NeoForge/Forge/Fabric, classes conhecidas/desconhecidas/abstratas, atributos literais e dinâmicos, registro tipado, prioridades de Goals, Animation.create, recursos órfãos/compartilhados, ausência de licença/metadados, referências quebradas, propostas não executáveis, reexecução determinística, integridade do original, ZIP inválido/truncado, colisões de caixa/arquivo-diretório, ZIP Slip e caminhos especiais do Windows, limites de entrada/compressão, profundidade JSON e substituição segura de hard links. As fixtures incluem um inicializador que lançaria uma exceção se carregado: os testes não o executam. Junctions/symlinks de saída são recusados pelo código, mas não foram criados como fixtures neste ambiente.

## Entrada real e proveniência

`L_Ender's Cataclysm 1.21.1-3.33.jar`, existente na raiz do Soulbound, foi copiado para a pasta de input, ignorada pelo Git. Ambas as cópias continuam com SHA-256:

```text
371956d0c2e3b56729ee93b955e936875d210e372fe734cdb74326d89ef79875
```

Metadados: Cataclysm 3.33, Minecraft 1.21.1, NeoForge; dependências declaradas incluem NeoForge, Minecraft, Curios e Lionfish API. Evidência: `META-INF/neoforge.mods.toml`, preservada no inventário. As versões/faixas declaradas não foram tratadas como versões instaladas nem como prova de compatibilidade com 1.21.11.

Resultado: 1313 classes inspecionadas; 109 classes de entidades com herança resolvida; 51 candidatos concretos do tipo LivingEntity, dos quais 39 têm registro tipado identificado. Os 12 restantes incluem classes auxiliares/bases concretas e permanecem candidatos sem registro comprovado. Foram produzidos exatamente 51 inventários JSON, 51 fichas Markdown e 51 propostas JSON. O índice distingue os registros encontrados e aponta para cada arquivo.

Evidências diretas nos candidatos: 253 chamadas de atributos base, 506 cadastros de Goals e 122 definições Java de animação. São chamadas/definições estáticas, não contagens de habilidades distintas nem prova de comportamento efetivo. Recursos globais: 537 modelos, 789 texturas, 232 sons, 921 dados, 207 configurações e 15 outros arquivos. Recursos globais não são automaticamente recursos de um mob.

## Limites observados

Dois JSONs de tradução não passaram pelo parser estrito: `assets/cataclysm/lang/fr_fr.json` e `assets/cataclysm/lang/pl_pl.json`. Foram registrados warnings; os originais não foram corrigidos. Nenhuma classe falhou no parser de bytecode nesta entrada.

As propostas são pesquisa, com `executable: false`. Atributos efetivos, configurações, fases, muitos cooldowns/triggers, associação completa de modelos Java, hitboxes e compatibilidade binária continuam pendentes quando não há prova suficiente. Não foram feitos testes de gameplay ou de carregamento do Cataclysm no alvo 1.21.11.

A declaração do pacote distingue assets com direitos reservados e código sob CC BY-NC-ND 4.0. Isso foi registrado como declaração, sem verificar autorização para adaptação. As permissões de código e assets permanecem separadas e NÃO VERIFICADAS. A ordem de revisão no resumo usa uma heurística estrutural explícita; não representa autorização de uso ou estimativa de horas.
