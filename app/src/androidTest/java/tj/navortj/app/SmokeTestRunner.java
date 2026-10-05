package tj.navortj.app;
import android.app.Instrumentation;
import android.app.Activity;
import android.os.Bundle;
import org.json.JSONObject;
/** Repository integration tests run on an emulator/device without third-party test libraries. */
public final class SmokeTestRunner extends Instrumentation {
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
    private void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    @Override public void onStart(){
        Bundle result=new Bundle();String prefs="navortj_integration_test";
        try {
            getTargetContext().getSharedPreferences(prefs,0).edit().clear().commit();
            ShopRepository r=new ShopRepository(getTargetContext(),prefs);
            check(!r.products.isEmpty(),"Catalog loads");String id=r.products.get(0).id;
            r.quantity(id,2);check(r.count()==2,"Quantity count");
            ShopRepository restored=new ShopRepository(getTargetContext(),prefs);
            check(restored.cart.get(id)==2,"Cart survives reload");
            r.toggle(id);check(new ShopRepository(getTargetContext(),prefs).favorites.contains(id),"Favorites survive reload");
            r.quantity(id,150);check(r.cart.get(id)==99,"Quantity limit");r.quantity(id,0);check(r.cart.isEmpty(),"Removal");
            r.quantity("missing-id",2);check(r.cart.isEmpty(),"Unknown ids rejected");
            Product priced=new Product(new JSONObject("{\"id\":\"test-priced\",\"price\":950}"));r.products.add(priced);r.quantity(priced.id,3);
            check(r.total()==2850,"Priced order total");check(!r.hasUnpriced(),"Priced order flag");r.quantity(id,1);check(r.hasUnpriced(),"Unpriced item is not treated as free");
            r.toggle(id);check(!r.favorites.contains(id),"Favorite toggle removes");
            org.json.JSONArray online=new org.json.JSONArray("[{\"id\":\"remote-1\",\"name_tj\":\"Онлайн\",\"price\":345}]");
            r.replaceCatalog(online);check(r.products.size()==1&&r.find("remote-1").price==345,"Online catalog replaces demos");
            check(r.cart.isEmpty()&&r.favorites.isEmpty(),"Deleted products pruned from saved lists");
            check(new ShopRepository(getTargetContext(),prefs).find("remote-1")!=null,"Online catalog available offline");
            boolean rejected=false;try{r.replaceCatalog(new org.json.JSONArray("[{\"id\":\"same\",\"name_tj\":\"A\"},{\"id\":\"same\",\"name_tj\":\"B\"}]"));}catch(Exception expected){rejected=true;}
            check(rejected&&r.find("remote-1")!=null,"Invalid catalog cannot replace saved catalog");
            r.replaceCatalog(new org.json.JSONArray());check(new ShopRepository(getTargetContext(),prefs).products.isEmpty(),"Empty online catalog stays empty offline");
            result.putString("stream","NAVORTJ: repository integration checks passed\n");finish(Activity.RESULT_OK,result);
        } catch(Throwable e){result.putString("stream","NAVORTJ FAILED: "+e+"\n");finish(Activity.RESULT_CANCELED,result);}
        finally {getTargetContext().getSharedPreferences(prefs,0).edit().clear().commit();}
    }
}

