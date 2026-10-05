package tj.navortj.app;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
final class ShopRepository {
    final ArrayList<Product> products=new ArrayList<>();
    final LinkedHashMap<String,Integer> cart=new LinkedHashMap<>();
    final HashSet<String> favorites=new HashSet<>();
    final SharedPreferences prefs;
    JSONObject config=new JSONObject();
    ShopRepository(Context context) throws Exception { this(context,"navortj"); }
    ShopRepository(Context context,String preferencesName) throws Exception {
        prefs=context.getSharedPreferences(preferencesName,Context.MODE_PRIVATE);
        JSONArray data=new JSONArray(readAsset(context,"products.json"));
        HashSet<String> ids=new HashSet<>();
        for(int i=0;i<data.length();i++) { Product p=new Product(data.getJSONObject(i));
            if(p.id.isEmpty() || !ids.add(p.id)) throw new IOException("Invalid or duplicate product id");
            products.add(p);
        }
        config=new JSONObject(readAsset(context,"shop.json"));
        try { JSONObject saved=new JSONObject(prefs.getString("cart","{}"));
            for(Product p:products) { int q=saved.optInt(p.id); if(q>0)cart.put(p.id,Math.min(q,99)); }
        } catch(JSONException ignored) { }
        for(String id:prefs.getStringSet("favorites",new HashSet<>()))if(find(id)!=null)favorites.add(id);
    }
    static String readAsset(Context c,String name) throws IOException {
        try(InputStream in=c.getAssets().open(name); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[4096]; int n; while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
    Product find(String id) { for(Product p:products)if(p.id.equals(id))return p; return null; }
    int quantityOf(String id) { Integer value=cart.get(id);return value==null?0:value; }
    int count() { int n=0; for(int q:cart.values())n+=q; return n; }
    double total() { double v=0; for(Map.Entry<String,Integer> e:cart.entrySet()){Product p=find(e.getKey());if(p!=null)v+=p.price*e.getValue();}return v; }
    boolean hasUnpriced(){for(String id:cart.keySet()){Product p=find(id);if(p!=null&&p.price<=0)return true;}return false;}
    void quantity(String id,int q) { if(find(id)==null)return; if(q<=0)cart.remove(id);else cart.put(id,Math.min(99,q));save(); }
    void toggle(String id){if(favorites.contains(id))favorites.remove(id);else favorites.add(id);save();}
    void save(){prefs.edit().putString("cart",new JSONObject(cart).toString()).putStringSet("favorites",new HashSet<>(favorites)).apply();}
    String config(String key){return config.optString(key);}
    String phone(){return prefs.getString("whatsapp",config("whatsapp"));}
    boolean tajik(){return prefs.getBoolean("tajik",true);}
}
