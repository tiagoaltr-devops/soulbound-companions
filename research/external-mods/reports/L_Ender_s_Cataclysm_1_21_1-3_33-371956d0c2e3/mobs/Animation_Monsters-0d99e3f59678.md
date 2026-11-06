# Animation_Monsters

Classe: `com/github/L_Ender/cataclysm/entity/etc/Animation_Monsters` (com/github/L_Ender/cataclysm/entity/etc/Animation_Monsters.class). Mob concreto por herança; registro/invocação não comprovados.

Inventário técnico: [JSON](Animation_Monsters-0d99e3f59678.json). Plano: [proposta](../proposals/Animation_Monsters-0d99e3f59678.json).

Viabilidade: BAIXA, conservadora; port, permissões e fluxo de comportamento pendentes.

Análise estática parcial: referências não provam execução. Não interpreta fluxo de controle, reflection, mixins ou configuração em runtime. Valores, prioridades, triggers, fases e cooldowns sem vínculo comprovado permanecem desconhecidos. Classes abstratas e projéteis não são mobs catalogados. Relações por nome são inferências; recursos compartilhados ficam no índice global. Compatibilidade binária e funcionamento no servidor não foram testados.
