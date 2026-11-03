# Soulbound Companions

Mod de Minecraft Java com companheiros vinculados ao jogador por UUID, evolução,
exploração, combate e rituais. Em desenvolvimento e preparado para colaboração.

| Componente | Versão do projeto |
| --- | --- |
| Minecraft Java | 1.21.11 |
| NeoForge | 21.11.48 |
| Java | 21 |
| Gradle Wrapper | 9.2.1 |
| Mod ID / package | `soulbound` / `br.com.soulbound` |

As versões são definidas pelo projeto. Use o wrapper incluído e um JDK 21;
não é necessário instalar Gradle globalmente nem adicionar JARs de outros mods.
A primeira compilação baixa as dependências e precisa de acesso à internet.

## Começar após clonar

No PowerShell, dentro da raiz do clone:

```powershell
java -version
.\gradlew.bat build --console=plain
.\gradlew.bat runClient
```

No Linux/macOS:

```bash
bash ./gradlew build --console=plain
bash ./gradlew runClient
```

Abra a raiz na IDE e importe o projeto Gradle. Configurações `.vscode`/`.idea`,
caches, mundos e configurações de servidor são locais e não acompanham o clone.
Veja [CONTRIBUTING.md](CONTRIBUTING.md) para setup, branches e validação.

O artefato é `build/libs/soulbound-1.0.0.jar`. O build padrão executa a suíte
Java existente e confere recursos e exclusões de desenvolvimento no JAR.
O workflow em `.github/workflows/build.yml` executa o build com Java 21 no GitHub.

## Funcionalidades atuais

- Identidade canônica persistente, propriedade por UUID e reserva de entidade única.
- Ovo/Bastão da Alma, comandos, recuperação do item e Soul Recall.
- Baby flutuante, alertas de ameaças, fuga, detecção de minérios e ressonância ancestral.
- Formas Junior e Pleno, progressão de nível, talentos e combate.
- God temporário, escudo espiritual e habilidades próprias.
- Equipamentos, alimentação manual/automática, regeneração e comportamento defensivo.
- Ruínas nas vilas, provações, Forja da Alma, rituais e ressurreição.
- HUD de vida/XP e interfaces de comandos e inventário.
- Visuais Junior Irushia e Pleno Brambleback; God Prime próprio.

Implementado não significa homologado em todos os cenários. Os relatórios registram
os testes realmente executados e as pendências. As integrações recentes dos modelos
externos ainda exigem inspeção visual no Minecraft e testes com clientes reais.

## Documentação para continuar o desenvolvimento

| Documento | Conteúdo |
| --- | --- |
| [Manual do jogador](docs/MANUAL_DO_JOGADOR.md) | Mecânicas e receitas |
| [Contribuição](CONTRIBUTING.md) | Setup, fluxo de Git e validação |
| [AGENTS.md](AGENTS.md) | Convenções de desenvolvimento e arquitetura |
| [Soulbound Identity](docs/SOULBOUND_IDENTITY.md) | Autoridade, UUIDs, persistência e limites |
| [Fase 6 e checklist](docs/FASE_6_IMPLEMENTACAO.md) / [checklist](docs/FASE_6_CHECKLIST.md) | Altares, rituais e testes |
| [Sobrevivência](docs/FASE_6_9_SOBREVIVENCIA.md) | Equipamentos, HP, alimentação e IA |
| [Pré-release](docs/FASE_6_10_PRE_RELEASE.md) | Revisão, empacotamento e homologação |
| [Junior Irushia](docs/JUNIOR_IRUSHIA.md) / [Pleno Brambleback](docs/PLENO_BRAMBLEBACK.md) | Modelos atuais, créditos e testes visuais |
| [Frequência das ruínas](docs/RUINAS_CHANCE_50.md) | Política atual de geração |

Os demais relatórios em `docs/` descrevem fases históricas. Consulte os documentos
mais recentes para mudanças posteriores. O [README anterior](docs/README_HISTORICO.md)
foi preservado como referência histórica, incluindo as antigas limitações do MVP.

## Estrutura

```text
src/main/java/br/com/soulbound/   Código do mod separado por responsabilidade
src/main/resources/             Assets, traduções e datapacks
src/main/templates/             Metadados expandidos pelo Gradle
src/test/                       Verificações e fixtures de desenvolvimento
docs/                           Manual, arquitetura e relatórios
tools/                          Importadores de modelos e ferramentas locais
gradle/wrapper/                 Wrapper necessário ao clone
*.bbmodel                       Fontes dos modelos usados pelos importadores
```

Geometria Java e texturas já estão prontas para o build. Os scripts PowerShell de
importação são opcionais, usados ao editar os modelos-fonte. Mantenha os `.bbmodel`
na raiz enquanto os scripts usarem esses caminhos.

## Compartilhamento e licenças

Inclua fontes, recursos, testes, documentos, ferramentas e wrapper no commit.
Não copie `build/`, `.gradle/`, `run/`, `runs/`, configurações de IDE ou JARs externos.
O `.gitignore` cobre esses arquivos e preserva a exceção do `gradle-wrapper.jar`.

O mod mantém a licença definida nos metadados (`All Rights Reserved`), com exceções
específicas para assets de terceiros. Consulte `TEMPLATE_LICENSE.txt` e os créditos
em `src/main/resources/assets/soulbound/credits/`. Não remova esses avisos ao compartilhar.
