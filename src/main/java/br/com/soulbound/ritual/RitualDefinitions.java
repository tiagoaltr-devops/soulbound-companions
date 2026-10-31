package br.com.soulbound.ritual;

import com.google.gson.*;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Resource-manager lookups respect datapack overrides and /reload; only performed at confirmation. */
public final class RitualDefinitions {
    public record Cost(String item,int count){
        public boolean matches(ItemStack stack){return count>0 && stack.getCount()>=count && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(item);}
    }
    public record Definition(String kind,List<Cost> offerings){}
    private RitualDefinitions() {}
    private static List<Cost> load(ServerLevel level,String name){
        var id=Identifier.fromNamespaceAndPath("soulbound","soul_ritual/"+name+".json");
        try(var reader=level.getServer().getResourceManager().getResourceOrThrow(id).openAsReader()){
            var json=JsonParser.parseReader(reader).getAsJsonObject();var costs=new ArrayList<Cost>();
            for(var element:json.getAsJsonArray("offerings")){var c=element.getAsJsonObject();var cost=new Cost(c.get("item").getAsString(),c.get("count").getAsInt());
                if(cost.count<1 || cost.count>64 || !BuiltInRegistries.ITEM.containsKey(Identifier.parse(cost.item)))throw new IllegalArgumentException("Invalid ritual ingredient");costs.add(cost);}
            if(costs.size()!=3)throw new IllegalArgumentException("Rituals require exactly three offerings");return costs;
        }catch(Exception e){throw new IllegalStateException("Cannot read ritual "+id,e);}
    }
    public static List<Cost> activation(ServerLevel level){return load(level,"activation");}
    public static List<Cost> offerings(ServerLevel level,String kind){return load(level,kind);}
}
