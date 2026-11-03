package br.com.soulbound.companion.progression;

import java.util.*;
import br.com.soulbound.companion.identity.SoulboundRecord;

/** Stable gameplay IDs, independent of models and movement implementation. */
public final class TalentDefinitions {
    public enum Branch { MAIN,BABY,JUNIOR,PLENO,GOD }
    public record Talent(String id,String archetype,Branch branch,int maximum,int minimumLevel,int branchPoints,String previous) {
        public String key(){return id.substring(id.indexOf(':')+1);}
        public String nameKey(){return "talent.soulbound."+key();}
    }
    private static Talent t(String key,Branch branch,int max,int level,int points,String previous){return new Talent("soulbound:"+key,"soulbound:lupus",branch,max,level,points,previous==null?null:"soulbound:"+previous);}
    public static final List<Talent> ALL=List.of(
        t("vitalidade_ancestral",Branch.MAIN,5,2,0,null),t("forca_espiritual",Branch.MAIN,5,2,0,null),
        t("sintonia_da_alma",Branch.MAIN,3,3,2,null),t("pele_runica",Branch.MAIN,3,4,3,"vitalidade_ancestral"),t("regeneracao_espiritual",Branch.MAIN,3,6,5,"pele_runica"),
        t("olhar_ancestral",Branch.BABY,3,2,0,null),t("ressonancia_aprimorada",Branch.BABY,3,3,1,"olhar_ancestral"),
        t("aura_protetora",Branch.BABY,3,5,3,"ressonancia_aprimorada"),t("vinculo_de_exploracao",Branch.BABY,3,4,2,"olhar_ancestral"),
        t("golpe_aprimorado",Branch.JUNIOR,5,2,0,null),t("instinto_predador",Branch.JUNIOR,3,4,2,"golpe_aprimorado"),
        t("ruptura_potencializada",Branch.JUNIOR,3,6,4,"golpe_aprimorado"),t("sintonia_de_combate",Branch.JUNIOR,3,8,5,"ruptura_potencializada"),t("furia_ancestral",Branch.JUNIOR,1,12,8,"instinto_predador"),
        t("dominio_ancestral",Branch.PLENO,3,15,0,null),t("investida_implacavel",Branch.PLENO,3,16,1,"dominio_ancestral"),
        t("impacto_runico",Branch.PLENO,3,17,2,"dominio_ancestral"),t("cataclismo_aprimorado",Branch.PLENO,3,18,3,"impacto_runico"),t("guardiao_inabalavel",Branch.PLENO,3,15,0,null),
        t("poder_primordial",Branch.GOD,3,25,0,null),t("investida_suprema",Branch.GOD,3,25,1,"poder_primordial"),
        t("ruptura_absoluta",Branch.GOD,3,26,2,"poder_primordial"),t("apocalipse_potencializado",Branch.GOD,3,27,3,"ruptura_absoluta"),t("essencia_eterna",Branch.GOD,3,25,0,null)
    );
    private TalentDefinitions() {}
    public static Talent find(String id){return ALL.stream().filter(t->t.id.equals(id)).findFirst().orElse(null);}
    public static int branchPoints(ProgressionState state,Branch branch){return ALL.stream().filter(t->t.branch==branch).mapToInt(t->state.rank(t.id)).sum();}
    /** Reason IDs are also synchronized to the client; the client never authorizes an investment. */
    public static int rejection(SoulboundRecord record,Talent talent){
        if(talent==null)return 8;if(!talent.archetype.equals(record.archetypeId()))return 7;
        var p=record.progression();if(p.rank(talent.id)>=talent.maximum)return 2;if(p.available()<1)return 1;
        if(p.level()<talent.minimumLevel)return 3;
        if(talent.branch!=Branch.MAIN && !record.unlockedForms().contains(talent.branch.name()))return 4;
        if(branchPoints(p,talent.branch)<talent.branchPoints)return 5;
        if(talent.previous!=null && p.rank(talent.previous)<1)return 6;return 0;
    }
}
