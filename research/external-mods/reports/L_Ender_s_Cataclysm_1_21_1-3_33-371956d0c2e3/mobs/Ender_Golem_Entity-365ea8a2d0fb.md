# Ender_Golem_Entity

Classe: `com/github/L_Ender/cataclysm/entity/AnimationMonster/BossMonsters/Ender_Golem_Entity` (com/github/L_Ender/cataclysm/entity/AnimationMonster/BossMonsters/Ender_Golem_Entity.class). Mob concreto por herança; registro/invocação não comprovados.

Inventário técnico: [JSON](Ender_Golem_Entity-365ea8a2d0fb.json). Plano: [proposta](../proposals/Ender_Golem_Entity-365ea8a2d0fb.json).

Viabilidade: BAIXA, conservadora; port, permissões e fluxo de comportamento pendentes.

Análise estática parcial: referências não provam execução. Não interpreta fluxo de controle, reflection, mixins ou configuração em runtime. Valores, prioridades, triggers, fases e cooldowns sem vínculo comprovado permanecem desconhecidos. Classes abstratas e projéteis não são mobs catalogados. Relações por nome são inferências; recursos compartilhados ficam no índice global. Compatibilidade binária e funcionamento no servidor não foram testados.
