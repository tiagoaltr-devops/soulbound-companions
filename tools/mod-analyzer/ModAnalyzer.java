import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

/** Offline research tool. Never loads classes from input archives. Java 21, no dependencies. */
public class ModAnalyzer {
    static final long MAX_ENTRY=32L*1024*1024, MAX_TOTAL=768L*1024*1024, MAX_ARCHIVE=512L*1024*1024;
    static final int MAX_ENTRIES=40000;
    static final String UNKNOWN="NÃO IDENTIFICADO";
    static final String LIMITS="Análise estática parcial: referências não provam execução. Não interpreta fluxo de controle, reflection, mixins ou configuração em runtime. Valores, prioridades, triggers, fases e cooldowns sem vínculo comprovado permanecem desconhecidos. Classes abstratas e projéteis não são mobs catalogados. Relações por nome são inferências; recursos compartilhados ficam no índice global. Compatibilidade binária e funcionamento no servidor não foram testados.";
    static Map<String,Object> map(Object... values) { var m=new LinkedHashMap<String,Object>(); for(int i=0;i<values.length;i+=2)m.put(values[i].toString(),values[i+1]);return m; }
    static Map<String,Object> evidence(Object value,String file,String member,String type,String status) {return map("value",value,"source_file",file,"source_member",member,"evidence_type",type,"status",status,"confidence",status.equals("CONFIRMADO")?"alta":status.equals("INFERIDO")?"média":"insuficiente");}
    static Object unknown(String reason) {return evidence(null,"","",reason,UNKNOWN);}
    static String text(byte[] b) {return new String(b,StandardCharsets.UTF_8);}
    static String hash(Path p) throws Exception {var digest=MessageDigest.getInstance("SHA-256");try(var in=Files.newInputStream(p)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)digest.update(b,0,n);}return HexFormat.of().formatHex(digest.digest());}
    static String slug(String s) {return s.replaceAll("[^A-Za-z0-9_-]","_");}
    static String json(Object o) {
        if(o==null)return "null";
        if(o instanceof Map<?,?> m){var j=new StringJoiner(",","{","}");m.forEach((k,v)->j.add(json(k.toString())+":"+json(v)));return j.toString();}
        if(o instanceof Collection<?> a){var j=new StringJoiner(",","[","]");a.forEach(v->j.add(json(v)));return j.toString();}
        if(o instanceof Double d && !Double.isFinite(d) || o instanceof Float f && !Float.isFinite(f))return json(o.toString());
        if(o instanceof Number || o instanceof Boolean)return o.toString();
        StringBuilder b=new StringBuilder("\"");for(char c:o.toString().toCharArray()){switch(c){case '"'->b.append("\\\"");case '\\'->b.append("\\\\");case '\n'->b.append("\\n");case '\r'->b.append("\\r");case '\t'->b.append("\\t");default->{if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}}return b.append('"').toString();
    }
    static void write(Path p,Object o) throws IOException {writeBytes(p,b(o instanceof String s?s:json(o)+"\n"));}
    static void writeBytes(Path p,byte[] bytes) throws IOException {
        safeParents(p);Path staging=Files.createTempFile(p.toAbsolutePath().getParent(),".analyzer-",".tmp");Files.write(staging,bytes);
        // Replacing the directory entry also avoids truncating a pre-existing hard link's other target.
        try{Files.move(staging,p,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(staging,p,StandardCopyOption.REPLACE_EXISTING);}
    }
    static void safeParents(Path p) throws IOException {Path a=p.toAbsolutePath().normalize();for(Path q=a;q!=null;q=q.getParent())if(Files.isSymbolicLink(q)||(Files.exists(q)&&!q.toRealPath().equals(q)))throw new IOException("Link/junction de saída recusado: "+q);Files.createDirectories(a.getParent());}
    static Path safePath(Path root,String name) throws IOException {
        if(name.isBlank() || name.contains("\\") || name.contains(":") || name.startsWith("/") || name.chars().anyMatch(c->c<32))throw new IOException("Caminho ZIP inseguro: "+name);
        for(String part:name.split("/"))if(part.equals("..") || part.equals(".") || part.endsWith(".") || part.endsWith(" ") || part.matches("(?i)(con|prn|aux|nul|com[1-9]|lpt[1-9])(\\..*)?"))throw new IOException("Caminho ZIP inseguro: "+name);
        Path r=root.toAbsolutePath().normalize(), p=r.resolve(name).normalize();if(!p.startsWith(r) || p.equals(r))throw new IOException("ZIP Slip: "+name);safeParents(p);return p;
    }
    record Archive(SortedMap<String,byte[]> files,String sha) {}
    static Archive readJar(Path jar) throws Exception {
        if(!Files.isRegularFile(jar))throw new IOException("Arquivo ausente: "+jar);
        if(Files.size(jar)>MAX_ARCHIVE)throw new IOException("JAR excede limite comprimido");
        var result=new TreeMap<String,byte[]>();var names=new HashSet<String>();long total=0;int count=0;
        // Validate every member before publishing any extraction or report.
        try(var zip=new ZipFile(jar.toFile())) {
            var entries=zip.entries();while(entries.hasMoreElements()) {
                ZipEntry e=entries.nextElement();if(++count>MAX_ENTRIES)throw new IOException("Excesso de entradas ZIP");
                String name=e.getName();validateName(name);if(!names.add(name.toLowerCase(Locale.ROOT)))throw new IOException("Entrada duplicada/colisão: "+name);
                if(e.isDirectory())continue;
                if(e.getSize()<0 || e.getSize()>MAX_ENTRY || (total+=e.getSize())>MAX_TOTAL)throw new IOException("Limite descomprimido excedido: "+name);
                if(e.getSize()>1024*1024 && e.getCompressedSize()>0 && e.getSize()/e.getCompressedSize()>1000)throw new IOException("Taxa de compressão excessiva: "+name);
                byte[] b;try(var in=zip.getInputStream(e)){b=in.readNBytes((int)MAX_ENTRY+1);}
                if(b.length>MAX_ENTRY || b.length!=e.getSize())throw new IOException("Tamanho ZIP inválido: "+name);
                CRC32 crc=new CRC32();crc.update(b);if(crc.getValue()!=e.getCrc())throw new IOException("CRC inválido: "+name);
                result.put(name,b);
            }
        }
        for(String name:result.keySet()){int split=name.lastIndexOf('/');while(split>0){if(result.containsKey(name.substring(0,split)))throw new IOException("Colisão arquivo/diretório: "+name);split=name.lastIndexOf('/',split-1);}}
        return new Archive(result,hash(jar));
    }
    static void validateName(String name) throws IOException {
        // Same policy as extraction, without creating directories.
        if(name.isBlank()||name.startsWith("/")||name.contains("\\")||name.contains(":")||name.chars().anyMatch(c->c<32))throw new IOException("Caminho ZIP inseguro: "+name);
        for(String p:name.split("/"))if(p.equals("..")||p.equals(".")||p.endsWith(".")||p.endsWith(" ")||p.matches("(?i)(con|prn|aux|nul|com[1-9]|lpt[1-9])(\\..*)?"))throw new IOException("Caminho ZIP inseguro: "+name);
    }
    record Instruction(int offset,int opcode,String reference,Object literal) {}
    record Field(String name,String descriptor,String signature) {}
    record Member(String name,String descriptor,List<String> refs,List<Object> literals,List<Instruction> instructions) {}
    static class Clazz {
        String name,parent,path;int flags,major;List<String> interfaces=new ArrayList<>(),refs=new ArrayList<>(),strings=new ArrayList<>();List<Member> methods=new ArrayList<>();List<Field> fields=new ArrayList<>();
        Object report(){return map("class",name,"source_file",path,"superclass",parent,"interfaces",interfaces,"abstract",(flags&0x400)!=0,"bytecode_major",major,"references",refs,"strings",strings,"methods",methods.stream().map(m->map("method",m.name,"descriptor",m.descriptor,"bytecode_references",m.refs,"literal_candidates",m.literals)).toList());}
    }
    static Clazz parseClass(String path,byte[] bytes) throws IOException {
        var in=new DataInputStream(new ByteArrayInputStream(bytes));if(in.readInt()!=0xcafebabe)throw new IOException("Class magic inválido");in.readUnsignedShort();var c=new Clazz();c.path=path;c.major=in.readUnsignedShort();
        int n=in.readUnsignedShort();Object[] cp=new Object[n];int[] tags=new int[n];
        for(int i=1;i<n;i++){int t=tags[i]=in.readUnsignedByte();cp[i]=switch(t){case 1->in.readUTF();case 3->in.readInt();case 4->in.readFloat();case 5->in.readLong();case 6->in.readDouble();case 7,8,16,19,20->in.readUnsignedShort();case 9,10,11,12,17,18->new int[]{in.readUnsignedShort(),in.readUnsignedShort()};case 15->new int[]{in.readUnsignedByte(),in.readUnsignedShort()};default->throw new IOException("Constant pool tag "+t);};if(t==5||t==6)i++;}
        c.flags=in.readUnsignedShort();c.name=resolve(cp,tags,in.readUnsignedShort());c.parent=resolve(cp,tags,in.readUnsignedShort());
        for(int count=in.readUnsignedShort();count>0;count--)c.interfaces.add(resolve(cp,tags,in.readUnsignedShort()));
        for(int count=in.readUnsignedShort();count>0;count--){in.readUnsignedShort();String fn=resolve(cp,tags,in.readUnsignedShort()),fd=resolve(cp,tags,in.readUnsignedShort()),sig="";for(int ac=in.readUnsignedShort();ac>0;ac--){String an=resolve(cp,tags,in.readUnsignedShort());int len=in.readInt();if(len<0||len>bytes.length)throw new IOException("Field inválido");if(an.equals("Signature")&&len==2)sig=resolve(cp,tags,in.readUnsignedShort());else in.skipNBytes(len);}c.fields.add(new Field(fn,fd,sig));}
        for(int count=in.readUnsignedShort();count>0;count--){in.readUnsignedShort();String name=resolve(cp,tags,in.readUnsignedShort()),desc=resolve(cp,tags,in.readUnsignedShort());var refs=new ArrayList<String>();var literals=new ArrayList<Object>();var instructions=new ArrayList<Instruction>();
            for(int ac=in.readUnsignedShort();ac>0;ac--){String an=resolve(cp,tags,in.readUnsignedShort());int len=in.readInt();if(len<0||len>bytes.length)throw new IOException("Atributo class inválido");byte[] a=in.readNBytes(len);if(a.length!=len)throw new EOFException();if(an.equals("Code")){var code=new DataInputStream(new ByteArrayInputStream(a));code.skipNBytes(4);int size=code.readInt();if(size<0||size>65535)throw new IOException("Code inválido");byte[] b=code.readNBytes(size);if(b.length!=size)throw new EOFException();decode(b,cp,tags,refs,literals,instructions);}}
            c.methods.add(new Member(name,desc,refs,literals,instructions));
        }
        skipAttributes(in);
        for(int i=1;i<n;i++){if(tags[i]==7)c.refs.add(resolve(cp,tags,i));if(tags[i]==8)c.strings.add(resolve(cp,tags,i));}
        c.refs=c.refs.stream().distinct().sorted().toList();c.strings=c.strings.stream().distinct().sorted().toList();return c;
    }
    static void skipAttributes(DataInputStream in) throws IOException {for(int a=in.readUnsignedShort();a>0;a--){in.readUnsignedShort();long len=Integer.toUnsignedLong(in.readInt());in.skipNBytes(len);}}
    static String resolve(Object[] cp,int[] tags,int i) throws IOException {
        return resolve(cp,tags,i,0);
    }
    static String resolve(Object[] cp,int[] tags,int i,int depth) throws IOException {
        if(depth>12||i<0||i>=cp.length)throw new IOException("Referência constant pool inválida");if(i==0)return "";
        Object o=cp[i];if(o instanceof String)return (String)o;
        if(tags[i]==7||tags[i]==8||tags[i]==16||tags[i]==19||tags[i]==20)return resolve(cp,tags,(int)o,depth+1);
        if(o instanceof int[] a){if(tags[i]==15)return resolve(cp,tags,a[1],depth+1);if(tags[i]==17||tags[i]==18)return "bootstrap#"+a[0]+" "+resolve(cp,tags,a[1],depth+1);return resolve(cp,tags,a[0],depth+1)+"."+resolve(cp,tags,a[1],depth+1);}return String.valueOf(o);
    }
    static int u2(byte[] b,int p){return (b[p]&255)*256+(b[p+1]&255);}
    static void decode(byte[] b,Object[] cp,int[] tags,List<String> refs,List<Object> literals,List<Instruction> instructions) throws IOException {
        try {for(int p=0;p<b.length;){int op=b[p]&255, next=p+1;if(op>201)throw new IOException("Opcode reservado/inválido: "+op);
            if(op==18){int i=b[p+1]&255;literals.add(resolve(cp,tags,i));next=p+2;}
            else if(op==19||op==20){literals.add(resolve(cp,tags,u2(b,p+1)));next=p+3;}
            else if(op>=178&&op<=187 || op==189 || op==192 || op==193 || op==197){refs.add(p+": "+resolve(cp,tags,u2(b,p+1)));next=p+(op==185||op==186?5:op==197?4:3);}
            else if(op==16){literals.add((int)b[p+1]);next=p+2;}
            else if(op==17){literals.add((int)(short)u2(b,p+1));next=p+3;}
            else if(op>=2&&op<=8)literals.add(op-3);
            else if(op>=9&&op<=15)literals.add(op<=10?op-9:op<=13?op-11:op-14);
            else if(op==170||op==171){int q=(p+4)&~3;ByteBuffer v=ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN);if(op==170){long count=(long)v.getInt(q+8)-v.getInt(q+4)+1;if(count<0||count>65536)throw new IOException("tableswitch inválido");next=Math.toIntExact(q+12+count*4);}else {int count=v.getInt(q+4);if(count<0||count>65536)throw new IOException("lookupswitch inválido");next=q+8+count*8;}}
            else if(op==196)next=p+((b[p+1]&255)==132?6:4);
            else if(op==132 || op>=153&&op<=168 || op==198||op==199)next=p+3;
            else if(op>=21&&op<=25 || op>=54&&op<=58 || op==169||op==188)next=p+2;
            else if(op==200||op==201)next=p+5;
            if(next<=p||next>b.length)throw new IOException("Instrução truncada");
            String reference=(op>=178&&op<=187||op==189||op==192||op==193||op==197)?resolve(cp,tags,u2(b,p+1)):"";Object literal=null;
            if(op==18||op==19||op==20){int idx=op==18?b[p+1]&255:u2(b,p+1);literal=tags[idx]==8?resolve(cp,tags,idx):cp[idx];}
            else if(op==16)literal=(int)b[p+1];else if(op==17)literal=(int)(short)u2(b,p+1);else if(op>=2&&op<=8)literal=op-3;else if(op>=9&&op<=15)literal=op<=10?op-9:op<=13?op-11:op-14;
            instructions.add(new Instruction(p,op,reference,literal));p=next;
        }}catch(IndexOutOfBoundsException|ArithmeticException e){throw new IOException("Bytecode malformado",e);}
    }
    static boolean ancestry(Clazz c,Map<String,Clazz> classes,Set<String> bases) {
        Set<String> seen=new HashSet<>();String name=c.parent;while(name!=null&&seen.add(name)){if(bases.contains(name))return true;Clazz p=classes.get(name);name=p==null?null:p.parent;}return false;
    }
    static final Set<String> LIVING=Set.of("net/minecraft/world/entity/LivingEntity","net/minecraft/world/entity/Mob","net/minecraft/world/entity/PathfinderMob","net/minecraft/world/entity/monster/Monster","net/minecraft/world/entity/monster/AbstractGolem","net/minecraft/world/entity/animal/Animal","net/minecraft/world/entity/animal/TamableAnimal","net/minecraft/world/entity/TamableAnimal","net/minecraft/world/entity/animal/AbstractFish","net/minecraft/world/entity/monster/Slime","net/minecraft/world/entity/monster/Zombie","net/minecraft/world/entity/monster/Skeleton","net/minecraft/world/entity/monster/Ravager","net/minecraft/world/entity/animal/Squid","net/minecraft/world/entity/animal/WaterAnimal","net/minecraft/world/entity/animal/AbstractGolem");
    static List<Object> metadata(Archive ar) {
        var out=new ArrayList<Object>();for(String path:List.of("META-INF/neoforge.mods.toml","META-INF/mods.toml","fabric.mod.json","mcmod.info","META-INF/MANIFEST.MF")) {
            if(!ar.files.containsKey(path))continue;String raw=text(ar.files.get(path));var fields=new ArrayList<Object>();String section="root";int line=0;
            for(String s:raw.split("\\R")){line++;String t=s.strip();if(t.startsWith("["))section=t;Matcher m=Pattern.compile("^([A-Za-z0-9_.-]+)\\s*=\\s*(.*)$").matcher(t);if(m.find())fields.add(evidence(m.group(2),path,"line "+line+" "+section+" "+m.group(1),"metadata literal","CONFIRMADO"));}
            Object parsed=null;if(path.endsWith(".json")||path.equals("mcmod.info"))try{parsed=new Json(raw).parse();}catch(Exception e){parsed=map("parse_error",e.getMessage());}
            out.add(map("file",path,"loader",path.contains("neoforge")?"NeoForge":path.contains("mods.toml")?"Forge":path.contains("fabric")?"Fabric":"não determinado","fields",fields,"parsed_json",parsed,"raw",raw));
        }return out;
    }
    static Map<String,Object> metadataFacts(List<Object> meta) {
        var mods=new ArrayList<Object>();var dependencies=new ArrayList<Object>();var versions=new ArrayList<Object>();var loaders=new TreeSet<String>();
        for(Object o:meta){var doc=(Map<?,?>)o;String path=doc.get("file").toString();if(!doc.get("loader").equals("não determinado"))loaders.add(doc.get("loader").toString());
            for(Object entry:(List<?>)doc.get("fields")){var e=(Map<?,?>)entry;String member=e.get("source_member").toString();if(member.contains("[[mods]]"))mods.add(entry);if(member.contains("dependencies.")){dependencies.add(entry);if(member.endsWith("versionRange"))versions.add(entry);}}
            if(doc.get("parsed_json") instanceof Map<?,?> m){mods.add(evidence(m,path,"root","JSON metadata literal","CONFIRMADO"));if(m.containsKey("depends"))dependencies.add(evidence(m.get("depends"),path,"depends","JSON metadata literal","CONFIRMADO"));}
        }return map("loaders",loaders,"mod_declarations",mods,"dependencies",dependencies,"dependency_version_ranges",versions,"note","Versões de dependência são restrições declaradas, não versões instaladas. Placeholders não resolvidos não são valores efetivos.");
    }
    static String category(String p) {String l=p.toLowerCase(Locale.ROOT);if(l.endsWith(".class"))return "classes";if(l.endsWith(".geo.json"))return "models";if(l.endsWith(".animation.json")||l.contains("/animations/"))return "animations";if(l.endsWith(".png"))return "textures";if(l.endsWith(".ogg"))return "sounds";if(l.contains("/models/"))return "models";if(l.startsWith("data/"))return "data";if(l.endsWith(".json")||l.endsWith(".toml")||l.endsWith(".cfg")||l.endsWith(".properties"))return "configuration";return "other";}
    static Object animationSummary(Object value) {
        if(!(value instanceof Map<?,?> m))return null;var out=new LinkedHashMap<String,Object>();for(var e:m.entrySet())if(e.getValue() instanceof Map<?,?> a)out.put(e.getKey().toString(),map("animation_length",a.get("animation_length"),"loop",a.get("loop"),"bones",a.get("bones") instanceof Map<?,?> bones?bones.keySet():List.of(),"timeline_keys",a.get("timeline") instanceof Map<?,?> timeline?timeline.keySet():List.of(),"triggers",unknown("Não determinados pelo nome da animação")));return out;
    }
    static Object geometrySummary(Object value) {
        if(!(value instanceof List<?> a))return null;var out=new ArrayList<Object>();for(Object o:a)if(o instanceof Map<?,?> geometry){var bones=new ArrayList<Object>();if(geometry.get("bones") instanceof List<?> list)for(Object bone:list)if(bone instanceof Map<?,?> b)bones.add(map("name",b.get("name"),"parent",b.get("parent")));Object desc=geometry.get("description");var info=map();if(desc instanceof Map<?,?> d)for(String key:List.of("identifier","texture_width","texture_height"))if(d.containsKey(key))info.put(key,d.get(key));out.add(map("description",info,"bones",bones));}return out;
    }
    static Object resource(String p,byte[] b,List<String> warnings) {
        var r=map("path",p,"category",category(p),"bytes",b.length);
        if(p.endsWith(".png")) {if(b.length>=24&&ByteBuffer.wrap(b).getLong()==0x89504e470d0a1a0aL)r.put("dimensions",evidence(List.of(ByteBuffer.wrap(b).getInt(16),ByteBuffer.wrap(b).getInt(20)),p,"IHDR","PNG header","CONFIRMADO"));else warnings.add("PNG inválido: "+p);}
        if(p.endsWith(".json"))try{Object obj=new Json(text(b)).parse();if(obj instanceof Map<?,?> m){if(m.containsKey("animations"))r.put("animation_definitions",evidence(animationSummary(m.get("animations")),p,"animations","JSON literal (durations/bones; triggers não comprovados)","CONFIRMADO"));if(m.containsKey("minecraft:geometry"))r.put("geometry",evidence(geometrySummary(m.get("minecraft:geometry")),p,"minecraft:geometry","JSON literal (bones/parents/texture dimensions)","CONFIRMADO"));}}catch(Exception e){warnings.add("JSON inválido: "+p+": "+e.getMessage());}
        return r;
    }
    static Map<String,Object> licensing(Archive ar,List<Object> meta) {
        var files=new ArrayList<String>();for(String p:ar.files.keySet())if(p.toLowerCase(Locale.ROOT).matches(".*(license|licence|notice|copyright|authors|credits).*"))files.add(p);
        var declarations=new ArrayList<Object>();for(Object o:meta){var m=(Map<?,?>)o;String raw=m.get("raw").toString();for(String line:raw.split("\\R"))if(line.strip().matches("(?i)^.*(license|copyright|authors|displayURL|source|homepage).*"))declarations.add(evidence(line.strip(),m.get("file").toString(),"metadata","declaração do pacote; não valida permissão","CONFIRMADO"));}
        return map("status","NÃO VERIFICADAS","declarations",declarations,"license_files",files,"code_permission",unknown("Revisar licença específica do código, versão e condições antes de reutilizar"),"asset_permission",unknown("Revisar licença específica de modelos/texturas/animações/sons separadamente"),"strategy","Reimplementação independente","restriction","Nenhum recurso é redistribuído ou importado na produção. Declarações não equivalem a autorização de adaptação.");
    }
    static String simple(String n){return n.substring(n.lastIndexOf('/')+1);}
    static String mobFile(String n){try{return slug(simple(n))+"-"+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b(n))).substring(0,12);}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
    static byte[] b(String s){return s.getBytes(StandardCharsets.UTF_8);}
    static void migrateReportNames(Path dir,String className) throws IOException {
        String old=slug(className.replace('/','.')),id=mobFile(className);if(old.equals(id))return;
        for(String suffix:List.of("mobs/"+old+".json","mobs/"+old+".md","proposals/"+old+".json")){
            Path previous=dir.resolve(suffix),dest=dir.resolve(suffix.replace(old,id));
            if(Files.exists(previous)){safeParents(dest);if(Files.isSymbolicLink(previous))throw new IOException("Symlink recusado");Files.move(previous,dest,StandardCopyOption.REPLACE_EXISTING);}
        }
    }
    static String normalize(String n){return n.toLowerCase(Locale.ROOT).replaceAll("entity|renderer|model|animation|[^a-z0-9]","");}
    static List<Object> methodEvidence(Clazz c,String pattern) {var list=new ArrayList<Object>();for(Member m:c.methods){List<String> refs=m.refs.stream().filter(s->s.toLowerCase(Locale.ROOT).matches(".*("+pattern+").*")).toList();if(!refs.isEmpty())list.add(evidence(refs,c.path,m.name+m.descriptor,"referências bytecode com offsets; sem prova de comportamento","CONFIRMADO"));}return list;}
    static List<Object> registrations(Clazz entity,Map<String,Clazz> classes) {
        var out=new ArrayList<Object>();for(Clazz c:classes.values())for(Field f:c.fields){
            if(!f.signature.contains("EntityType<L"+entity.name+";>"))continue;
            for(Member m:c.methods)if(m.name.equals("<clinit>")){var ins=m.instructions;for(int i=0;i<ins.size();i++)if(ins.get(i).opcode==179 && ins.get(i).reference.startsWith(c.name+"."+f.name+".")){
                // A short straight-line window: constant name -> supplier -> register -> typed field.
                for(int j=i-1;j>=Math.max(0,i-7);j--)if(ins.get(j).reference.contains(".register.")||ins.get(j).reference.contains(".registerEntityType.")){
                    for(int k=j-1;k>=Math.max(0,j-5);k--)if(ins.get(k).literal instanceof String s && s.matches("[a-z0-9_./-]+")){
                        out.add(evidence(map("registry_path",s,"typed_field",f.name,"generic_signature",f.signature),c.path,m.name+" @"+ins.get(k).offset+".."+ins.get(i).offset,"literal register call assigned to generic EntityType field; namespace not assumed","CONFIRMADO"));break;
                    }break;
                }
            }}
        }return out;
    }
    static List<Object> attributeCalls(Clazz c) {
        var out=new ArrayList<Object>();for(Member m:c.methods){var ins=m.instructions;for(int i=0;i<ins.size();i++){
            Instruction a=ins.get(i);if(a.opcode!=178||!a.reference.contains("/attributes/Attributes."))continue;
            // Only resolve an immediate numeric constant followed by the add(Holder,double) call.
            if(i+2<ins.size() && ins.get(i+1).literal instanceof Number n && ins.get(i+2).reference.contains("AttributeSupplier$Builder.") && ins.get(i+2).reference.contains(";D)"))out.add(evidence(map("attribute_field",a.reference,"base_value",n,"runtime_modifiers","não resolvidos"),c.path,m.name+m.descriptor+" @"+a.offset,"sequência direta getstatic/const/add; valor base, não efetivo","CONFIRMADO"));
        }}return out;
    }
    static List<Object> goalCalls(Clazz c) {
        var out=new ArrayList<Object>();for(Member m:c.methods){var a=m.instructions;for(int i=0;i+2<a.size();i++){
            if(a.get(i).opcode!=180||!a.get(i).reference.contains("GoalSelector;")||!(a.get(i+1).literal instanceof Number)||a.get(i+2).opcode!=187)continue;
            for(int j=i+3;j<Math.min(a.size(),i+20);j++){Instruction x=a.get(j);if(x.opcode>=153&&x.opcode<=171)break;if(x.reference.contains("GoalSelector.")&&x.reference.contains("(ILnet/minecraft/world/entity/ai/goal/Goal;)V")){
                out.add(evidence(map("selector",a.get(i).reference,"priority",a.get(i+1).literal,"goal_class",a.get(i+2).reference),c.path,m.name+m.descriptor+" @"+a.get(i).offset+".."+x.offset,"sequência direta selector/priority/new Goal/addGoal; condições do Goal não interpretadas","CONFIRMADO"));break;}
            }
        }}return out;
    }
    static List<Object> animationDefinitions(Clazz c) {
        var out=new ArrayList<Object>();for(Member m:c.methods){var a=m.instructions;for(int i=0;i+2<a.size();i++){
            if(!(a.get(i).literal instanceof Number)||!a.get(i+1).reference.matches("(?i).*animation.*\\.create\\.\\(I\\).*" )||a.get(i+2).opcode!=179)continue;
            out.add(evidence(map("field",a.get(i+2).reference,"duration_argument",a.get(i).literal,"unit","não comprovada; consultar API","triggers",null),c.path,m.name+" @"+a.get(i).offset,"constante passada a Animation.create(int) e atribuída a campo","CONFIRMADO"));
        }}return out;
    }
    static List<Object> dynamicAttributes(Clazz c) {
        var out=new ArrayList<Object>();for(Member m:c.methods){var a=m.instructions;for(int i=0;i<a.size();i++){
            if(a.get(i).opcode!=178||!a.get(i).reference.contains("/attributes/Attributes."))continue;var expression=new ArrayList<String>();
            for(int j=i+1;j<Math.min(a.size(),i+20);j++){Instruction x=a.get(j);if(x.reference.contains("/attributes/Attributes."))break;if(x.reference.contains("AttributeSupplier$Builder.")&&x.reference.contains(";D)")){if(expression.stream().anyMatch(s->s.toLowerCase(Locale.ROOT).matches(".*(config|difficulty|phase).*")))out.add(evidence(map("attribute",a.get(i).reference,"expression_references",expression,"effective_value",null),c.path,m.name+" @"+a.get(i).offset,"expressão dependente de configuração/dificuldade/fase não avaliada","DINÂMICO"));break;}if(!x.reference.isBlank())expression.add(x.reference);}
        }}return out;
    }
    static Map<String,Object> mob(Clazz c,Map<String,Clazz> classes,Archive ar,String key,List<Object> meta,List<String> warnings) {
        String name=simple(c.name), stem=normalize(name);var associated=new ArrayList<Object>();var visualClasses=new ArrayList<Object>();
        // Restrict heuristic assets to normalized entity stem; direct strings in related renderer/model classes also retained.
        Set<String> direct=new TreeSet<>(c.strings);for(Clazz candidate:classes.values())if(candidate.refs.contains(c.name) && candidate.name.toLowerCase(Locale.ROOT).matches(".*(render|model|anim).*")){visualClasses.add(evidence(candidate.name,candidate.path,"constant pool","entity class reference (association candidate)","INFERIDO"));direct.addAll(candidate.strings);}
        for(String p:ar.files.keySet())if(!p.endsWith(".class") && p.startsWith("assets/") && !stem.isEmpty() && normalize(p.substring(p.lastIndexOf('/')+1)).contains(stem))associated.add(evidence(p,p,"","correspondência de nome; não prova uso","INFERIDO"));
        var broken=new ArrayList<Object>();for(String s:direct)if(s.matches("(?:[a-z0-9_.-]+:)?(?:textures|geo|animations)/.*\\.(?:png|json)")){String namespace=s.contains(":")?s.substring(0,s.indexOf(':')):"";String path=s.contains(":")?"assets/"+namespace+"/"+s.substring(s.indexOf(':')+1):s;boolean found=ar.files.containsKey(path)||ar.files.keySet().stream().anyMatch(p->p.endsWith("/"+s));if(!found)broken.add(evidence(s,c.path,"associated constant pools","referência literal não resolvida no pacote; pode ser dependência","INFERIDO"));}
        List<Object> attrs=methodEvidence(c,"attributes|attribute|createattributes");
        var stats=map();for(String field:List.of("max_health","attack_damage","movement_speed","armor","armor_toughness","knockback_resistance","attack_knockback","attack_range","other","conditional_modifiers","difficulty"))stats.put(field,unknown("Valor efetivo não resolvido por análise de fluxo; consultar evidências de métodos"));stats.put("bytecode_evidence",attrs);
        stats.put("confirmed_base_attribute_calls",attributeCalls(c));
        stats.put("dynamic_attribute_expressions",dynamicAttributes(c));
        for(String field:List.of("MAX_HEALTH","ATTACK_DAMAGE","MOVEMENT_SPEED","ARMOR_TOUGHNESS","ARMOR","KNOCKBACK_RESISTANCE","ATTACK_KNOCKBACK","ATTACK_RANGE")){
            var candidates=attributeCalls(c).stream().filter(call->((Map<?,?>)((Map<?,?>)call).get("value")).get("attribute_field").toString().contains("."+field+".")).toList();
            if(!candidates.isEmpty())stats.put(field.toLowerCase(Locale.ROOT),map("base_value_candidates",candidates,"effective_value",unknown("Não seleciona um valor entre métodos/condições nem avalia modificadores")));
        }
        var combat=map();for(String field:List.of("basic_attack","damage","range","special_attacks","cooldowns","effects","projectiles","area_effect","combos","activation_conditions","phase_transitions"))combat.put(field,unknown("Sem resolução semântica/fluxo de controle"));combat.put("bytecode_evidence",methodEvidence(c,"attack|hurt|damage|shoot|projectile|effect|spell|ability|cooldown|phase"));
        var ai=map();for(String field:List.of("navigation","target_selection","goal_priorities","offense","defense","pursuit_distance","escape_reposition","without_target","phase_transitions"))ai.put(field,unknown("Comportamento e condições não determinados"));ai.put("bytecode_evidence",methodEvidence(c,"goal|navigation|path|target|movecontrol|brain"));
        ai.put("confirmed_goal_registrations",goalCalls(c));
        var animations=map();for(String state:List.of("idle","walk","run","attack","skill","hurt","death","transform","other"))animations.put(state,unknown("Nomes semânticos e triggers exigem revisão; ver recursos globais e classes Java"));animations.put("bytecode_evidence",methodEvidence(c,"animation|animate|trigger"));
        animations.put("java_animation_definitions",animationDefinitions(c));
        var identity=map("origin",key,"metadata",metadataFacts(meta),"java_class",evidence(c.name,c.path,"class header","classfile","CONFIRMADO"),"entity_type",evidence(c.parent,c.path,"super_class","herança de LivingEntity resolvida","CONFIRMADO"),"registry_id",unknown("Registro/lambda não vinculado semanticamente à entidade"),"category",unknown("MobCategory não associado estaticamente"),"dimensions_hitbox",unknown("Valores registrados/overrides não resolvidos"),"summonable",unknown("Uma classe concreta não prova registro ou possibilidade de invocação"));
        List<Object> registrations=registrations(c,classes);identity.put("registrations",registrations);identity.put("catalog_status",registrations.isEmpty()?"CANDIDATO: classe concreta, registro não resolvido":"MOB: registro tipado identificado; summonable não comprovado");
        var model=map("resources",associated,"renderers_models",visualClasses,"format",unknown("Ver classes e recursos associados; JSON não implica GeckoLib"),"animation_library_candidates",evidence(c.refs.stream().filter(s->s.toLowerCase(Locale.ROOT).matches(".*(geckolib|citadel|lionfish|animation).*" )).toList(),c.path,"constant pool","referências; uso efetivo não comprovado","INFERIDO"),"broken_reference_candidates",broken);
        var deps=c.refs.stream().filter(s->!s.startsWith("java/")&&!s.startsWith("net/minecraft/")).toList();
        return map("schema_version",1,"identity",identity,"attributes",stats,"model",model,"animations",animations,"combat",combat,"ai",ai,"dependencies",evidence(deps,c.path,"constant pool","classes referenciadas; não todas obrigatórias","CONFIRMADO"),"limitations",LIMITS);
    }
    static Map<String,Object> proposal(Clazz c,String key,Map<String,Object> mob) {
        var adaptations=List.of("Vínculo permanente: adaptar SoulboundIdentity sem duplicar estado","Formas BABY/JUNIOR/PLENO e GOD temporária: projeto específico pendente","Inventário/equipamento: integrar CompanionEquipment e CompanionInventoryMenu","Talentos: integrar ProgressionService/TalentDefinitions","Comandos do Grimório: autorização no servidor e snapshots","Ataque direcionado: TargetCommandService e proteção de aliados/PvP","Multiplayer: validar owner/bond/entity e sincronização","Persistência: preservar SoulboundRecord/SoulboundSavedData; migração fora deste escopo","Servidor dedicado: isolar modelos/renderers do código comum");
        return map("schema_version",1,"executable",false,"species_id",evidence("proposal:"+key.toLowerCase(Locale.ROOT)+"_"+slug(simple(c.name)).toLowerCase(Locale.ROOT),c.path,"","identificador de proposta, não registro real","INFERIDO"),"archetype",unknown("Escolha de design pendente"),"forms",List.of(),"attributes",mob.get("attributes"),"abilities",unknown("Reimplementar após revisão de combate, sem copiar automaticamente"),"model_references",mob.get("model"),"animation_references",mob.get("animations"),"ai_behaviors",mob.get("ai"),"dependencies",mob.get("dependencies"),"reusable_soulbound_systems",List.of("CompanionArchetype (ainda acoplado às formas Lupus)","CompanionEquipment","ProgressionService/TalentEffects","SoulboundRecord (contrato canônico protegido)","Goals de seguir/ficar (recebem LupusCompanionEntity; requer adaptação)"),"adaptations",adaptations,"strategy","Reimplementação independente","technical_pending",List.of("Comprovar registry ID, dimensões e categoria","Resolver atributos e condições por método","Revisar permissões separadas de código/assets","Portar APIs de origem para NeoForge 21.11.48 / Minecraft 1.21.11","Não há registro genérico de espécies funcional"),"feasibility",map("rating","BAIXA","visual_complexity",unknown("Revisão dos modelos necessária"),"animation_complexity",unknown("Revisão dos controladores necessária"),"combat_complexity",unknown("Revisão de fluxo necessária"),"ai_complexity",unknown("Revisão de Goals necessária"),"external_dependencies","Ver metadados e referências no inventário","compatibility","NÃO VERIFICADA: requer port e testes separados na versão alvo","maintenance_risk","alto: acoplamento a APIs e sistemas específicos do mod","licensing_risk","alto até revisão das permissões separadas","estimated_effort","não estimável em horas sem revisão; exige port e adaptação estrutural","reason","Classificação conservadora: evidências parciais, permissões e compatibilidade não verificadas; não é prova de inviabilidade de design"),"integration_tracks",map("code","API pública/dependência opcional somente após comprovação; preferir implementação independente","design","Estudar comportamento e projetar espécie própria","assets","Nenhuma autorização presumida; não importar"));
    }
    static Object reviewRecord(Clazz c,Map<String,Object> mob) {
        String id=mobFile(c.name);var identity=(Map<?,?>)mob.get("identity");return map("class",c.name,"inventory","mobs/"+id+".json","proposal","proposals/"+id+".json","catalog_status",identity.get("catalog_status"),"registrations",identity.get("registrations"),"confirmed_base_attributes",attributeCalls(c),"goal_registrations",goalCalls(c),"java_animation_definitions",animationDefinitions(c),"review_score",c.methods.size()+goalCalls(c).size()*3+animationDefinitions(c).size()*2,"review_score_note","Heurística para ordenar revisão: métodos + 3*Goals + 2*animações. Não estima esforço de implementação nem qualidade da espécie.");
    }
    @SuppressWarnings("unchecked")
    static Map<String,Object> analyze(Path jar,Path reports,Path extract) throws Exception {
        Archive ar=readJar(jar);String key=slug(jar.getFileName().toString().replaceFirst("(?i)\\.jar$",""))+"-"+ar.sha.substring(0,12);var warnings=new ArrayList<String>();var classes=new TreeMap<String,Clazz>();var resources=new ArrayList<Object>();var namespaces=new TreeSet<String>();
        for(var e:ar.files.entrySet()){String p=e.getKey();if(p.matches("(?:assets|data)/[^/]+/.*"))namespaces.add(p.split("/")[1]);if(p.endsWith(".class"))try{Clazz c=parseClass(p,e.getValue());classes.put(c.name,c);}catch(Exception err){warnings.add("Classe não analisada "+p+": "+err.getMessage());}else resources.add(resource(p,e.getValue(),warnings));}
        List<Object> meta=metadata(ar);var license=licensing(ar,meta);var mobs=new ArrayList<Object>();var entities=new ArrayList<Object>();var proposals=new ArrayList<Object>();var reviews=new ArrayList<Object>();
        for(String scope:List.of("code","asset"))license.put(scope+"_declarations",((List<?>)license.get("declarations")).stream().filter(e->((Map<?,?>)e).get("value").toString().toLowerCase(Locale.ROOT).contains(scope.equals("code")?"code":"assets")).toList());
        for(Clazz c:classes.values()){
            boolean living=ancestry(c,classes,LIVING);boolean entity=living||ancestry(c,classes,Set.of("net/minecraft/world/entity/Entity","net/minecraft/world/entity/projectile/Projectile","net/minecraft/world/entity/projectile/ThrowableProjectile","net/minecraft/world/entity/projectile/AbstractArrow"));
            if(entity)entities.add(evidence(c.name,c.path,"super_class chain","class hierarchy","CONFIRMADO"));
            if(living && (c.flags&(0x400|0x200))==0){var m=mob(c,classes,ar,key,meta,warnings);mobs.add(m);var prop=proposal(c,key,m);proposals.add(prop);String id=mobFile(c.name);Path dir=reports.resolve(key);migrateReportNames(dir,c.name);write(dir.resolve("mobs/"+id+".json"),m);write(dir.resolve("proposals/"+id+".json"),prop);write(dir.resolve("mobs/"+id+".md"),"# "+simple(c.name)+"\n\nClasse: `"+c.name+"` ("+c.path+"). Mob concreto por herança; registro/invocação não comprovados.\n\nInventário técnico: [JSON]("+id+".json). Plano: [proposta](../proposals/"+id+".json).\n\nViabilidade: BAIXA, conservadora; port, permissões e fluxo de comportamento pendentes.\n\n"+LIMITS+"\n");}
        }
        var registries=new ArrayList<Object>();var relevant=new ArrayList<Object>();for(Clazz c:classes.values()){if(c.refs.stream().anyMatch(s->s.contains("EntityType")||s.contains("DeferredRegister")))registries.add(map("class",c.name,"source_file",c.path,"string_candidates",c.strings,"methods",c.methods.stream().filter(m->m.refs.stream().anyMatch(s->s.matches(".*(register|EntityType|Registry).*"))).map(m->map("member",m.name+m.descriptor,"references",m.refs,"literal_candidates",m.literals)).toList(),"status","INFERIDO","limitation","Strings/lambdas não equivalem a registry IDs associados a mobs"));if(c.name.matches("(?i).*(entity|goal|target|render|model|animat|attribute|damage|ability|spell|projectile).*"))relevant.add(c.report());}
        var broken=new ArrayList<Object>();for(var e:ar.files.entrySet())if(e.getKey().endsWith(".json"))try{Object obj=new Json(text(e.getValue())).parse();collectReferences(obj,e.getKey(),ar,broken);}catch(Exception ignored){}
        var report=map("schema_version",1,"id",key,"input",jar.toAbsolutePath().toString(),"sha256",ar.sha,"metadata",meta,"namespaces",namespaces,"entity_count",entities.size(),"cataloged_mob_count",mobs.size(),"entities",entities,"class_count",classes.size(),"classes",classes.values().stream().map(c->map("class",c.name,"source_file",c.path,"superclass",c.parent,"bytecode_major",c.major)).toList(),"resources",resources,"registry_candidates",registries,"relevant_classes",relevant,"license",license,"broken_reference_candidates",broken,"warnings",warnings,"limitations",LIMITS,"mob_files",mobs.stream().map(m->((Map<?,?>)((Map<?,?>)((Map<?,?>)m).get("identity")).get("java_class")).get("value")).toList());
        report.put("registered_mob_count",mobs.stream().filter(m->!((List<?>)((Map<?,?>)((Map<?,?>)m).get("identity")).get("registrations")).isEmpty()).count());
        for(Object o:mobs){var m=(Map<String,Object>)o;var identity=(Map<?,?>)m.get("identity");String name=((Map<?,?>)identity.get("java_class")).get("value").toString();reviews.add(reviewRecord(classes.get(name),m));}
        report.put("mob_records",reviews);
        report.put("metadata_facts",metadataFacts(meta));
        write(reports.resolve(key).resolve("inventory.json"),report);
        if(extract!=null){Path root=extract.resolve(key);for(var e:ar.files.entrySet()){String p=e.getKey();if(!p.endsWith(".class")&&(!category(p).equals("other")||p.startsWith("META-INF/")||p.toLowerCase(Locale.ROOT).matches(".*(license|notice|copyright).*"))){Path dest=safePath(root,p);writeBytes(dest,e.getValue());}}}
        return report;
    }
    static void collectReferences(Object obj,String source,Archive ar,List<Object> out) {
        if(obj instanceof Map<?,?> m){for(var e:m.entrySet()){String key=e.getKey().toString();Object v=e.getValue();if(v instanceof String s && Set.of("parent","model").contains(key) && s.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")){String[] a=s.split(":",2);String target="assets/"+a[0]+"/models/"+a[1]+".json";if(!a[0].equals("minecraft")&&!ar.files.containsKey(target))out.add(evidence(target,source,key,"referência JSON ausente no pacote; possível dependência","INFERIDO"));}collectReferences(v,source,ar,out);}}else if(obj instanceof List<?> a)for(Object v:a)collectReferences(v,source,ar,out);
    }
    @SuppressWarnings("unchecked")
    static void consolidate(Path reports,List<Map<String,Object>> mods,List<Object> errors) throws IOException {
        var index=map("schema_version",1,"target",map("minecraft","1.21.11","neoforge","21.11.48","java",21),"mods",mods.stream().map(m->map("id",m.get("id"),"sha256",m.get("sha256"),"metadata",m.get("metadata"),"entities",m.get("entity_count"),"mobs",m.get("cataloged_mob_count"),"registered_mobs",m.get("registered_mob_count"),"mob_records",m.get("mob_records"),"inventory",m.get("id")+"/inventory.json","mob_classes",m.get("mob_files"))).toList(),"errors",errors);
        write(reports.resolve("index.json"),index);StringBuilder md=new StringBuilder("# Análise de mods externos\n\nAlvo: Minecraft 1.21.11, NeoForge 21.11.48, Java 21.\n\n");
        for(var m:mods){String id=m.get("id").toString();md.append("## ").append(id).append("\n\n[Inventário completo](").append(id).append("/inventory.json) — ").append(m.get("entity_count")).append(" entidades por herança; ").append(m.get("cataloged_mob_count")).append(" mobs concretos catalogados. Registro e invocação não comprovados.\n\nMetadados, versões, dependências e licenças: inventário, com arquivos de origem. Namespaces: ").append(m.get("namespaces")).append(". Recursos: ").append(((List<?>)m.get("resources")).size()).append(".\n\n");for(Object mob:(List<?>)m.get("mob_files")){String f=mobFile(mob.toString());md.append("- ").append(mob).append(" — [inventário](").append(id).append("/mobs/").append(f).append(".json), [plano](").append(id).append("/proposals/").append(f).append(".json). Atributos/habilidades: somente referências estáticas; valores sem prova ficam desconhecidos. Prioridade: revisão manual, sem importação.\n");}md.append("\nLicenciamento NÃO VERIFICADO: revisar declarações e licenças de código/assets separadamente. Estratégia sugerida: reimplementação independente. Viabilidade BAIXA conservadora até revisão técnica.\n\n");}
        for(var mod:mods){md.append("## Evidências e ordem de revisão: ").append(mod.get("id")).append("\n\nRegistros tipados ligados a mobs: **").append(mod.get("registered_mob_count")).append("**. O total catalogado inclui classes concretas auxiliares/candidatas cujo registro não foi resolvido.\n\n");
            for(Object o:(List<?>)mod.get("metadata")){var doc=(Map<?,?>)o;for(Object f:(List<?>)doc.get("fields")){var field=(Map<?,?>)f;String member=field.get("source_member").toString();if(member.contains("[[mods]]")&&(member.endsWith("modId")||member.endsWith("version")||member.endsWith("displayName")))md.append("- ").append(member).append(": ").append(field.get("value")).append(" (`").append(doc.get("file")).append("`).\n");}}
            md.append("\n| Mob | Atributos base confirmados | Goals diretos | Animações Java diretas |\n|---|---|---|---|\n");
            List<Map<String,Object>> records=((List<?>)mod.get("mob_records")).stream().map(o->(Map<String,Object>)o).toList();
            for(var r:records)md.append("| ").append(simple(r.get("class").toString())).append(" | ").append(((List<?>)r.get("confirmed_base_attributes")).size()).append(" | ").append(((List<?>)r.get("goal_registrations")).size()).append(" | ").append(((List<?>)r.get("java_animation_definitions")).size()).append(" |\n");
            md.append("\nPrioridade de **revisão**, entre registros encontrados: menor pontuação estrutural primeiro (métodos + 3×Goals + 2×animações). Não representa estimativa de adaptação nem aprovação de reutilização.\n\n");
            for(var r:records.stream().filter(r->!((List<?>)r.get("registrations")).isEmpty()).sorted(Comparator.comparingInt(r->((Number)r.get("review_score")).intValue())).limit(10).toList())md.append("- ").append(simple(r.get("class").toString())).append(": ").append(r.get("review_score")).append(" pontos; [plano](").append(mod.get("id")).append('/').append(r.get("proposal")).append(").\n");
            md.append("\nAvisos da análise: ").append(json(mod.get("warnings"))).append(".\n\n");
        }
        md.append("## Limitações e recomendações\n\n").append(LIMITS).append("\n\nPriorizar candidatos com herança e registro comprovados, depois confirmar atributos efetivos, configurações e direitos. Código, inspiração de design e assets são trilhas separadas. A prioridade de revisão usa somente evidência estrutural; priorização de implementação depende da avaliação manual. Nenhuma entidade de produção, migração ou configuração executável foi criada.\n\nErros: ").append(json(errors)).append("\n");write(reports.resolve("analysis-summary.md"),md.toString());
    }
    public static void main(String[] args) throws Exception {
        Path root=Path.of("").toAbsolutePath();Path reports=root.resolve("research/external-mods/reports"),extract=null;var inputs=new ArrayList<Path>();
        for(int i=0;i<args.length;i++){switch(args[i]){case "--reports"->reports=Path.of(args[++i]).toAbsolutePath();case "--extract"->extract=Path.of(args[++i]).toAbsolutePath();case "--help"->{System.out.println("java tools/mod-analyzer/ModAnalyzer.java [--reports DIR] [--extract DIR] [mod.jar ...]\nSem JARs explícitos: research/external-mods/input/*.jar. --extract: cópia local opcional de recursos, nunca classes.");return;}default->inputs.add(Path.of(args[i]));}}
        if(inputs.isEmpty()){Path input=root.resolve("research/external-mods/input");if(Files.isDirectory(input))try(var paths=Files.list(input)){inputs.addAll(paths.filter(p->p.toString().toLowerCase(Locale.ROOT).endsWith(".jar")).sorted().toList());}}
        // Output is confined to research, avoiding accidental production imports and source overwrites.
        Path research=root.resolve("research/external-mods").normalize();for(Path p:extract==null?List.of(reports):List.of(reports,extract)){p=p.toAbsolutePath().normalize();if(!p.startsWith(research)||p.startsWith(research.resolve("input")))throw new IOException("Saída deve permanecer em research/external-mods, fora de input");safeParents(p.resolve("check"));}
        if(extract!=null && (extract.normalize().startsWith(reports.normalize()) || reports.normalize().startsWith(extract.normalize())))throw new IOException("Extração e relatórios devem ocupar árvores separadas");
        var mods=new ArrayList<Map<String,Object>>();var errors=new ArrayList<Object>();Set<String> hashes=new HashSet<>();
        for(Path jar:inputs)try{String sha=hash(jar);if(!hashes.add(sha))continue;var r=analyze(jar,reports,extract);mods.add(r);System.out.println(r.get("id")+": "+r.get("cataloged_mob_count")+" mobs; "+r.get("entity_count")+" entidades");}catch(Exception e){errors.add(map("input",jar.toString(),"error",e.toString()));System.err.println(jar+": "+e);}
        consolidate(reports,mods,errors);if(inputs.isEmpty())System.out.println("Nenhum JAR disponível em research/external-mods/input.");if(!errors.isEmpty())System.exit(1);
    }
    /** Small bounded JSON reader for resource metadata; no script engines. */
    static class Json {
        final String s;int p,depth;Json(String s){this.s=s;}void ws(){while(p<s.length()&&Character.isWhitespace(s.charAt(p)))p++;}
        Object parse(){Object v=value();ws();if(p!=s.length())throw new IllegalArgumentException("JSON trailing data");return v;}
        Object value(){if(++depth>128)throw new IllegalArgumentException("JSON depth");try{ws();if(p>=s.length())throw new IllegalArgumentException("JSON EOF");char c=s.charAt(p);if(c=='"')return string();if(c=='{'||c=='['){p++;boolean object=c=='{';var m=new LinkedHashMap<String,Object>();var a=new ArrayList<Object>();ws();if(p<s.length()&&s.charAt(p)==(object?'}':']')){p++;return object?m:a;}while(true){if(object){ws();String key=string();ws();expect(':');m.put(key,value());}else a.add(value());ws();if(p<s.length()&&s.charAt(p)==(object?'}':']')){p++;break;}expect(',');}return object?m:a;}for(String literal:List.of("true","false","null"))if(s.startsWith(literal,p)){p+=literal.length();return literal.equals("null")?null:literal.equals("true");}Matcher n=Pattern.compile("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?").matcher(s);n.region(p,s.length());if(n.lookingAt()){p=n.end();return new java.math.BigDecimal(n.group());}throw new IllegalArgumentException("JSON value at "+p);}finally{depth--;}}
        void expect(char c){if(p>=s.length()||s.charAt(p++)!=c)throw new IllegalArgumentException("JSON expected "+c);}
        String string(){expect('"');StringBuilder b=new StringBuilder();while(p<s.length()){char c=s.charAt(p++);if(c=='"')return b.toString();if(c<32)throw new IllegalArgumentException("JSON control");if(c=='\\'){if(p>=s.length())break;char e=s.charAt(p++);c=switch(e){case '"','\\','/'->e;case 'b'->'\b';case 'f'->'\f';case 'n'->'\n';case 'r'->'\r';case 't'->'\t';case 'u'->{if(p+4>s.length())throw new IllegalArgumentException("JSON unicode");char u=(char)Integer.parseInt(s.substring(p,p+4),16);p+=4;yield u;}default->throw new IllegalArgumentException("JSON escape");};}b.append(c);}throw new IllegalArgumentException("JSON unterminated string");}
    }
}
