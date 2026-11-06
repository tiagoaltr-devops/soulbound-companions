import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import javax.tools.ToolProvider;

/** Synthetic fixtures only. The generated mod classes are never loaded. */
public class ModAnalyzerTest {
    static int checks;
    static Path temp;
    static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    interface Action {void run() throws Exception;}
    static void rejects(Action a,String label) throws Exception {try{a.run();throw new AssertionError("Accepted: "+label);}catch(IOException|IllegalArgumentException e){checks++;}}
    static Path jar(String name,Map<String,byte[]> files) throws Exception {Path p=temp.resolve(name);try(var z=new ZipOutputStream(Files.newOutputStream(p))){for(var e:files.entrySet()){z.putNextEntry(new ZipEntry(e.getKey()));z.write(e.getValue());z.closeEntry();}}return p;}
    static byte[] b(String s){return s.getBytes(StandardCharsets.UTF_8);}
    static Map<String,byte[]> fixture() throws Exception {
        Path source=temp.resolve("source"),compiled=temp.resolve("classes");Files.createDirectories(source);Files.createDirectories(compiled);
        var sources=Map.ofEntries(Map.entry("net/minecraft/world/entity/PathfinderMob.java","package net.minecraft.world.entity; public class PathfinderMob {}"),
                Map.entry("fixture/Creature.java","package fixture; public class Creature extends net.minecraft.world.entity.PathfinderMob {public static final Animation IDLE=Animation.create(20); static {if(true) throw new RuntimeException(\"MUST NEVER EXECUTE\");} public String attack(){return \"textures/entity/absent.png\";} public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attrs(){return new net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder().add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH,42.0);} public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder configured(){return new net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder().add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH,Config.VALUE);} public net.minecraft.world.entity.ai.goal.GoalSelector goalSelector=new net.minecraft.world.entity.ai.goal.GoalSelector(); public void registerGoals(){goalSelector.addGoal(3,new ChaseGoal());}}"),
                Map.entry("fixture/Animation.java","package fixture; public class Animation { public static Animation create(int duration){return new Animation();}}"),
                Map.entry("fixture/Config.java","package fixture; public class Config { public static double VALUE=123;}"),
                Map.entry("net/minecraft/world/entity/ai/goal/Goal.java","package net.minecraft.world.entity.ai.goal; public class Goal {}"),
                Map.entry("net/minecraft/world/entity/ai/goal/GoalSelector.java","package net.minecraft.world.entity.ai.goal; public class GoalSelector {public void addGoal(int priority,Goal goal){}}"),
                Map.entry("fixture/ChaseGoal.java","package fixture; public class ChaseGoal extends net.minecraft.world.entity.ai.goal.Goal {}"),
                Map.entry("net/minecraft/world/entity/ai/attributes/Attributes.java","package net.minecraft.world.entity.ai.attributes; public class Attributes {public static fixture.Holder MAX_HEALTH=new fixture.Holder();}"),
                Map.entry("net/minecraft/world/entity/ai/attributes/AttributeSupplier.java","package net.minecraft.world.entity.ai.attributes; public class AttributeSupplier {public static class Builder { public Builder add(fixture.Holder h,double value){return this;} }}"),
                Map.entry("fixture/Holder.java","package fixture; public class Holder {}"),
                Map.entry("fixture/EntityType.java","package fixture; public class EntityType<T> {}"),
                Map.entry("fixture/Registry.java","package fixture; public class Registry {static class Deferred<T> {} static <T> Deferred<EntityType<T>> register(String name,java.util.function.Supplier<EntityType<T>> factory){return new Deferred<>();} static Deferred<EntityType<Creature>> CREATURE=register(\"creature\",()->new EntityType<Creature>());}"),
                Map.entry("fixture/AbstractCreature.java","package fixture; public abstract class AbstractCreature extends Creature {}"),
                Map.entry("fixture/CreatureRenderer.java","package fixture; public class CreatureRenderer { Creature c; public String texture(){return \"textures/entity/absent.png\";} }"),
                Map.entry("fixture/Unmatched.java","package fixture; public class Unmatched {}"),
                Map.entry("fixture/Projectile.java","package fixture; public class Projectile {} "));
        var args=new ArrayList<String>(List.of("-d",compiled.toString()));for(var e:sources.entrySet()){Path p=source.resolve(e.getKey());Files.createDirectories(p.getParent());Files.writeString(p,e.getValue());args.add(p.toString());}
        check(ToolProvider.getSystemJavaCompiler().run(null,null,null,args.toArray(String[]::new))==0,"compile trusted fixtures");var files=new TreeMap<String,byte[]>();try(var paths=Files.walk(compiled)){for(Path p:paths.filter(Files::isRegularFile).toList())files.put(compiled.relativize(p).toString().replace('\\','/'),Files.readAllBytes(p));}
        // Stub dependency is intentionally not bundled, matching an external Minecraft dependency.
        files.remove("net/minecraft/world/entity/PathfinderMob.class");
        files.put("META-INF/neoforge.mods.toml",b("modLoader=\"javafml\"\nlicense=\"Test-only\"\n[[mods]]\nmodId=\"fixture\"\nversion=\"1.0\"\ndisplayName=\"Fixture\"\n[[dependencies.fixture]]\nmodId=\"minecraft\"\nversionRange=\"1.21.1\""));
        files.put("assets/fixture/geo/creature.geo.json",b("{\"minecraft:geometry\":[{\"bones\":[{\"name\":\"root\"}]}]}"));
        files.put("assets/fixture/animations/orphan.animation.json",b("{\"animations\":{\"animation.orphan.idle\":{\"animation_length\":1.5}}}"));
        files.put("assets/second/models/item/shared.json",b("{\"parent\":\"missing:item/absent\"}"));
        files.put("assets/fixture/models/item/a.json",b("{\"parent\":\"second:item/shared\"}"));
        files.put("assets/fixture/models/item/b.json",b("{\"parent\":\"second:item/shared\"}"));
        return files;
    }
    public static void main(String[] args) throws Exception {
        temp=Files.createTempDirectory("soulbound-mod-analyzer-test-");Path out=temp.resolve("reports");
        Path valid=jar("valid.jar",fixture());String before=ModAnalyzer.hash(valid);var r=ModAnalyzer.analyze(valid,out,null);
        check(r.get("cataloged_mob_count").equals(1),"only concrete living mob, exclude abstract/projectile/unmatched");
        check(r.get("namespaces").toString().contains("second"),"multiple namespaces");
        check(((List<?>)r.get("metadata")).size()==1,"NeoForge metadata");
        check(ModAnalyzer.hash(valid).equals(before),"original bytes unchanged");
        check(Files.exists(out.resolve(r.get("id").toString()).resolve("inventory.json")),"inventory report");
        var mob=(Map<?,?>)new ModAnalyzer.Json(Files.readString(out.resolve(r.get("id").toString()).resolve("mobs/"+ModAnalyzer.mobFile("fixture/Creature")+".json"))).parse();
        check(((List<?>)((Map<?,?>)mob.get("attributes")).get("confirmed_base_attribute_calls")).size()==1,"attribute literal linked to add call");
        check(((List<?>)((Map<?,?>)mob.get("attributes")).get("dynamic_attribute_expressions")).size()==1,"dynamic config attribute recorded without inventing effective value");
        check(((List<?>)((Map<?,?>)mob.get("identity")).get("registrations")).size()==1,"registry path linked to typed entity field");
        check(((List<?>)((Map<?,?>)mob.get("ai")).get("confirmed_goal_registrations")).size()==1,"Goal priority linked to registration");
        check(((List<?>)((Map<?,?>)mob.get("animations")).get("java_animation_definitions")).size()==1,"Java animation duration argument linked to field");
        check(!((List<?>)r.get("broken_reference_candidates")).isEmpty(),"broken model reference");
        check(((Map<?,?>)r.get("license")).get("status").equals("NÃO VERIFICADAS"),"declaration does not authorize reuse");
        check(((List<?>)((Map<?,?>)r.get("license")).get("license_files")).isEmpty(),"no license file");
        check(((List<?>)r.get("resources")).size()==6,"shared resources counted once; orphan models/animations preserved");
        String id=r.get("id").toString();Path inventory=out.resolve(id).resolve("inventory.json");String first=Files.readString(inventory);
        ModAnalyzer.analyze(valid,out,null);check(first.equals(Files.readString(inventory)),"deterministic rerun");
        ModAnalyzer.consolidate(out,List.of(r),List.of());check(Files.readString(out.resolve("index.json")).contains("fixture/Creature"),"searchable index");
        check(Files.readString(out.resolve("analysis-summary.md")).contains("fixture/Creature"),"summary generated");
        Path extracted=temp.resolve("extract");ModAnalyzer.analyze(valid,out,extracted);
        check(Files.exists(extracted.resolve(id).resolve("assets/fixture/geo/creature.geo.json")),"selective extraction");
        check(!Files.exists(extracted.resolve(id).resolve("fixture/Creature.class")),"classes not extracted");
        try(var p=Files.list(out.resolve(id).resolve("proposals"))){check(p.count()==1,"one proposal per concrete mob");}
        Object proposal=new ModAnalyzer.Json(Files.readString(out.resolve(id).resolve("proposals/"+ModAnalyzer.mobFile("fixture/Creature")+".json"))).parse();check(Boolean.FALSE.equals(((Map<?,?>)proposal).get("executable")),"proposal nonexecutable");
        Path noMeta=jar("no-metadata.jar",Map.of("assets/a/models/only.json",b("{}")));var nom=ModAnalyzer.analyze(noMeta,out,null);
        check(((List<?>)nom.get("metadata")).isEmpty(),"missing metadata supported");check(nom.get("cataloged_mob_count").equals(0),"resource-only archive");
        Path fabric=jar("fabric.jar",Map.of("fabric.mod.json",b("{\"id\":\"f\",\"version\":\"2\",\"depends\":{\"minecraft\":\"1.21.1\"}}")));check(ModAnalyzer.metadata(ModAnalyzer.readJar(fabric)).toString().contains("Fabric"),"Fabric metadata");
        Path forge=jar("forge.jar",Map.of("META-INF/mods.toml",b("[[mods]]\nmodId=\"f\"")));check(ModAnalyzer.metadata(ModAnalyzer.readJar(forge)).toString().contains("Forge"),"Forge metadata");
        Path invalid=temp.resolve("invalid.jar");Files.writeString(invalid,"not a zip");rejects(()->ModAnalyzer.readJar(invalid),"invalid zip");rejects(()->ModAnalyzer.readJar(temp.resolve("missing.jar")),"missing file");
        Path truncated=temp.resolve("truncated.jar");byte[] validBytes=Files.readAllBytes(valid);Files.write(truncated,Arrays.copyOf(validBytes,validBytes.length-16));rejects(()->ModAnalyzer.readJar(truncated),"truncated ZIP directory");
        for(String name:List.of("../escape.json","/absolute.json","C:/escape.json","assets/../../escape.json","assets\\escape.json","assets/NUL.json","assets/file. ")){Path bad=jar("bad-"+checks+".jar",Map.of(name,b("{}")));rejects(()->ModAnalyzer.readJar(bad),"ZIP Slip/Windows path: "+name);}
        Path dup=jar("case.jar",Map.of("assets/A.json",b("{}"),"assets/a.json",b("{}")));rejects(()->ModAnalyzer.readJar(dup),"case collision");
        Path collision=jar("collision.jar",Map.of("assets/a",b("{}"),"assets/a/x.json",b("{}")));rejects(()->ModAnalyzer.readJar(collision),"file directory collision");
        Path bomb=jar("bomb.jar",Map.of("assets/bomb.json",new byte[(int)ModAnalyzer.MAX_ENTRY+1]));rejects(()->ModAnalyzer.readJar(bomb),"oversized member");
        Path ratio=jar("ratio.jar",Map.of("assets/ratio.json",new byte[2*1024*1024]));rejects(()->ModAnalyzer.readJar(ratio),"compression bomb");
        Path malformed=jar("class.jar",Map.of("Bad.class",new byte[]{0,1,2}));check(!((List<?>)ModAnalyzer.analyze(malformed,out,null).get("warnings")).isEmpty(),"malformed class becomes warning");
        rejects(()->new ModAnalyzer.Json("[".repeat(130)+"]".repeat(130)).parse(),"JSON recursion limit");
        rejects(()->ModAnalyzer.safePath(temp.resolve("safe"),"../escape"),"extraction path");
        Path original=temp.resolve("original.json"),linked=temp.resolve("linked.json");Files.writeString(original,"original");Files.createLink(linked,original);ModAnalyzer.write(linked,"replacement");
        check(Files.readString(original).equals("original"),"atomic output preserves hard-linked original");
        check(Files.readString(linked).equals("replacement"),"atomic output replaces only output entry");
        // Provenance/evidence states remain explicit, including runtime expressions.
        check(ModAnalyzer.evidence(null,"f","m","config value","DINÂMICO").get("status").equals("DINÂMICO"),"dynamic evidence state");
        check(new ModAnalyzer.Json(ModAnalyzer.json(Map.of("value",Double.NaN))).parse() instanceof Map,"nonfinite constant still emits valid JSON");
        System.out.println("PASS: "+checks+" checks; fixtures retained in "+temp);
    }
}
