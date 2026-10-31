# Fase 6.1.2 — Frequência e repetição das provas

- Spawn mínimo: **500 blocos** para novas peças, configurado no JSON de worldgen.
- Separação: **1.000 blocos**, com margem de 256 para deslocamento das salas.
- Seleção em duas rodadas locais determinísticas: mantém os vencedores anteriores
  e preenche espaços que não conflitam com eles. Não retorna à seleção por grandes células.
  O buffer conservador das possíveis ruínas antigas permanece para compatibilidade.
- Integração às vilas, UUIDs, índice, conclusões e recompensas foram preservados.
- Prova: `trialCooldownTicks = 0` por padrão; zero ignora também prazos antigos
  persistidos. Cancelamento continua limpando guardiões e liberando a tentativa.
  Valores positivos reativam a espera. Conclusão anterior ainda bloqueia nova recompensa.
- Configuração local `run/config/soulbound-server.toml` ajustada para zero.
  Em outras instalações já existentes, confira esse valor: configurações salvas
  não são substituídas apenas porque o padrão do mod mudou.
- Recall, God, Ressonância, receitas, atributos, XP e probabilidades da Forja não alterados.

Validação: `gradlew.bat build` **BUILD SUCCESSFUL**, **264 verificações aprovadas**.
Uma seed sintética (`20261101`), 3.721 candidatas: **164 → 266 selecionadas** (+62%).
Testes verificaram inclusão dos vencedores anteriores, ausência de duplicação,
separação com margem, limite de 500 blocos e cooldown zero/positivo/expirado.
Essa comparação mede seleção, não a porcentagem de vilas reais que receberão ruínas.
Nenhum novo ensaio de geração de mundo foi executado nesta alteração.

Homologação pendente: reiniciar o jogo com o JAR atualizado, explorar **chunks novos**
no Overworld normal, conferir ruínas a partir de 500 blocos do spawn e repetir uma
prova imediatamente após falhar. Confirmar que uma prova já vencida não concede
outra Centelha. Peças antigas preservam seus parâmetros salvos; não há retrogeração.

Arquivos: `Config.java`, `AltarPolicy.java`, `TrialEngine.java`, `RuinSelection.java`,
`RuinPlacement.java`, `RuinStructure.java`, `ancestral_ruin.json`,
`AncestralResonanceChecks.java` e documentação/checklist. Log: `build/phase612-build.log`.
