# Analisador estático de mods

Java 21/JDK, somente bibliotecas padrão. Executar na raiz do Soulbound:

```powershell
java tools/mod-analyzer/ModAnalyzer.java
```

Descobre `research/external-mods/input/*.jar`. Também aceita um ou vários caminhos explícitos:

```powershell
java tools/mod-analyzer/ModAnalyzer.java 'mod-a.jar' 'mod-b.jar'
java tools/mod-analyzer/ModAnalyzer.java --extract research/external-mods/work/extracted
powershell -NoProfile -ExecutionPolicy Bypass -File tools/mod-analyzer/test.ps1
```

`--extract` é opcional e cria apenas cópias locais de recursos selecionados. Os JARs são abertos somente para leitura; classes nunca são carregadas, executadas ou extraídas. Não é necessário Gradle, Minecraft ou instalar bibliotecas. O comando de teste compila somente o analisador e fixtures próprias. O bypass vale somente para esse processo de testes e não modifica a política do sistema.

Saídas em `research/external-mods/reports/`: `analysis-summary.md`, `index.json` e, por mod/SHA-256, `inventory.json`, `mobs/*.json`, `mobs/*.md`, `proposals/*.json`. A inspeção do Soulbound está em `soulbound-architecture.md`. O resumo e índice descrevem a invocação atual; diretórios de execuções anteriores continuam preservados. Conteúdo idêntico fornecido duas vezes na mesma execução é deduplicado por hash; a reexecução sobrescreve os mesmos relatórios determinísticos, sem anexar registros. JARs de conteúdo diferente têm identidades diferentes.

O inventário contém metadados Forge/NeoForge (campos TOML literais com seção e linha), Fabric (JSON), manifest, namespaces, recursos, hierarquias, classes/métodos/referências e candidatos a registro. Não é um parser TOML completo: strings multilinha, interpolação de versões e semântica de loader requerem revisão do texto original, preservado no relatório. Restrições declaradas de dependências não são versões instaladas. JARs aninhados são apenas inventariados, não analisados recursivamente.

O parser classfile lê constant pool, herança, assinaturas genéricas e instruções com offsets, incluindo wide/switch. Não usa class loaders ou decompiladores. Classes concretas descendentes de bases LivingEntity conhecidas são catalogadas como candidatos; um registro só é indicado quando há chamada register com nome literal atribuída a campo EntityType genérico correspondente. `registry_path` não implica namespace nem possibilidade de invocação. Bases externas não presentes no JAR e nomes ofuscados podem impedir reconhecimento. Classes auxiliares concretas não são automaticamente espécies.

Atributos base são confirmados somente para `getstatic Attributes.*`, constante numérica e chamada direta a Builder com double. Isso não determina atributos efetivos depois de configuração, dificuldade, equipamentos ou fases. Goals, dano, habilidades e animações têm referências com método de origem; sem análise semântica/fluxo, prioridades, cooldowns e condições permanecem desconhecidos. Modelos Bedrock/Gecko JSON preservam bones/parents e definições de animações, com dimensões PNG por IHDR. Modelos/animações em Java permanecem como evidências de classes e métodos, sem reconstrução visual automática. Associações por nome são INFERIDO; assets órfãos/compartilhados ficam no índice global. Referências a dependências podem aparecer como candidatos a referência quebrada.

Cada achado possui valor, arquivo, membro, tipo, status e confiança. Estados: CONFIRMADO, INFERIDO, NÃO IDENTIFICADO e DINÂMICO. A ausência de resolução não autoriza inventar números. Valores base alternativos permanecem como candidatos com métodos de origem; expressões de configuração são DINÂMICO, sem avaliar o valor efetivo. Sequências diretas de GoalSelector e Animation.create registram prioridade e argumento de duração, sem presumir condições ou unidades da biblioteca. A classificação de viabilidade é conservadora e explicita os campos ainda sem evidência. Propostas usam `executable: false`; não há leitor dessas propostas no runtime do Soulbound. A ordem sugerida de revisão usa somente uma heurística estrutural declarada, sem estimar tempo de desenvolvimento.

Limites por JAR: 512 MiB comprimidos, 40.000 entradas, 32 MiB por entrada e 768 MiB descomprimidos acumulados; razão de compressão acima de 1000 para entradas maiores que 1 MiB é rejeitada. A ferramenta mantém o pacote validado em memória: processe arquivos grandes em máquina com memória suficiente. Todas as entradas são validadas antes de gerar relatórios; tamanhos reais e CRC são conferidos. Caminhos absolutos, travessia, barras Windows, ADS, dispositivos reservados e colisões sem distinção de caixa são rejeitados. Links ZIP não são recriados: entradas são tratadas como bytes, nunca como links. A saída recusa symlinks existentes e fica confinada à árvore de pesquisa fora de input; extração e relatórios não podem se sobrepor. Execute sem alterações concorrentes nos diretórios de saída para evitar corridas de filesystem.

Falhas de JAR são registradas no índice e produzem exit code 1; os outros arquivos continuam. Ausência de entradas é uma execução válida com resumo vazio. Classes/recursos malformados geram warnings, sem inventar resultados. A ferramenta não determina compatibilidade binária e não testa servidor/cliente. Não modifica fontes, recursos de produção, Gradle, registro canônico ou mundos.

Licenças são declarações do pacote, não permissões verificadas. Código e assets têm pendências separadas; ausência/ambiguidade fica NÃO VERIFICADAS. A extração local não deve ser publicada. `research/external-mods/.gitignore` exclui input JARs e work. Os relatórios incluem estruturas/constantes observadas e também devem ser revisados antes de publicação, especialmente definições de modelos/animações. Nenhum commit ou push é feito.

Os resumos de geometria/animação conservam nomes, relações de ossos, dimensões e durações declaradas, sem copiar cubos, vértices ou keyframes completos para relatórios. Arquivos de saída são substituídos por arquivo temporário e rename, evitando truncar outros alvos de hard links existentes. Uma migração interna de nomes dos relatórios desta entrega encurta caminhos para Windows; não toca registros ou dados do jogo. A execução real e os limites observados estão em `research/external-mods/reports/validation.md`.
