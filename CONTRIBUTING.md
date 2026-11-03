# Desenvolvimento em colaboração

Leia o [README](README.md), o [AGENTS.md](AGENTS.md) e o documento da fase afetada
antes de alterar mecânicas. O projeto já avançou além do MVP: instruções históricas
de fases antigas não descrevem todas as funcionalidades atuais.

## Primeiro setup

1. Clone o repositório em uma pasta nova e abra a raiz que contém `settings.gradle`.
2. Instale um JDK 21 e configure o Gradle da IDE para usá-lo. Confira `java -version`.
3. Execute `./gradlew.bat build --console=plain` no PowerShell.
4. Execute `./gradlew.bat runClient` para testar em um mundo descartável.

No Linux/macOS, use `bash ./gradlew build --console=plain` e
`bash ./gradlew runClient`. O CI já configura o launcher como executável.
Não é necessário instalar Gradle globalmente nem copiar caches do outro desenvolvedor.
O wrapper fixa Gradle 9.2.1 e o toolchain solicita Java 21.

VS Code: instale suporte a Java e Gradle e importe a raiz. As configurações locais
em `.vscode/` contêm caminhos absolutos e não são compartilhadas. Gere/reconfigure os
launchers na sua máquina ou use as tarefas Gradle pelo terminal.

Se precisar definir `org.gradle.java.home`, use seu arquivo pessoal
`~/.gradle/gradle.properties`; não coloque um caminho da sua máquina no arquivo
`gradle.properties` do projeto. `gradle-local.properties` é ignorado, mas não é
carregado automaticamente por este build.

## Fluxo de contribuição

```text
git switch -c feature/descricao-curta
# Implementar e revisar alterações.
# Executar o build e os testes manuais pertinentes.
git status --short
git diff --check
git diff
git add <arquivos-da-sua-alteracao>
git diff --cached --stat
git diff --cached
git commit -m "Descreve a alteração"
```

Envie sua branch ao remoto e abra um pull request conforme combinado com o dono
do repositório. Prefira alterações pequenas e descreva problema, solução e testes.
O workflow existente executa o build em pushes e pull requests; configurações do
GitHub e proteção de branches são administradas no próprio repositório.

## Validação

- `build` compila, executa as verificações Java existentes e confere o JAR.
- `runClient` permite inspecionar renderização, interfaces e jogabilidade.
- `runServer` inicia um servidor de desenvolvimento; na primeira execução,
  leia o EULA gerado em `run/` e configure a aceitação localmente se concordar.
- Fixtures avançadas usam diretórios em `runs/`, flags explícitas e configurações
  locais que não acompanham o clone. Consulte os relatórios antes de executá-las.
  Não são necessárias para o build padrão e não devem usar mundos de jogadores.

Não trate build aprovado como homologação visual ou multiplayer. Informe o que
foi testado, o que não foi e quais versões foram usadas. Testes com FakePlayers
não substituem dois clientes reais.

## O que versionar

Versione fontes, recursos, testes, documentação, ferramentas, configurações Gradle
e todo o wrapper. Mantenha os dois `.bbmodel` da raiz: os importadores os leem
diretamente. As classes Java e PNGs resultantes também são versionadas, de modo
que ninguém precisa rodar PowerShell ou Blockbench para compilar.

Não versione builds, JARs de terceiros, caches, logs, mundos, configurações de
servidor, senhas de RCON, arquivos `.env` reais ou configurações da IDE.
O `.gitignore` não remove arquivos que já estejam rastreados; examine o diff staged.
O ZIP de arte original `soulbound_altar_assets.zip` já faz parte do histórico e foi
mantido como fonte de assets; não é uma dependência do build.

## Arquitetura e compatibilidade

Use `br.com.soulbound` e as responsabilidades descritas no AGENTS.md. O servidor
controla identidade, combate e progressão; classes de renderização ficam no cliente.
Preserve UUIDs, codecs legados e reservas contra duplicação. Alterações de versão,
dependências ou dados persistentes precisam de alinhamento com o responsável.
Não há novas dependências necessárias para os modelos atuais.

## Licenças e autoria

O mod mantém `All Rights Reserved` em seus metadados; compartilhar o repositório
com um parceiro não altera essa definição. O template tem seu próprio
`TEMPLATE_LICENSE.txt`. Modelos externos têm condições específicas:

- Irushia: créditos em `src/main/resources/assets/soulbound/credits/irushia.txt`.
- Brambleback: créditos e termos CC BY-SA em
  `src/main/resources/assets/soulbound/credits/brambleback.txt`.

Preserve autoria, links e avisos de adaptação também ao modificar esses assets.
O JAR de Cataclysm na máquina de origem não faz parte do projeto nem de seu build.

## Verificação da preparação para compartilhamento

Em 03/11/2026, uma cópia dos 299 arquivos candidatos ao commit foi compilada em
outra pasta, sem `.git`, configurações da IDE, instâncias Minecraft ou JARs locais.
Resultado: `BUILD SUCCESSFUL`. A execução usou o JDK e o cache global de dependências
da máquina; não constitui teste de download com cache vazio nem homologação em
outro sistema operacional. Evidência local ignorada: `build/share-validation-build.log`.

O wrapper e os dois `.bbmodel` foram incluídos na cópia; `run/`, `runs/`, o arquivo
acidental `=0` e o JAR de Cataclysm ficaram de fora. A busca direcionada por padrões
de chaves privadas, tokens GitHub/AWS e senha RCON não encontrou ocorrências nos
arquivos de texto candidatos. Revise sempre o conteúdo staged antes de compartilhar.
