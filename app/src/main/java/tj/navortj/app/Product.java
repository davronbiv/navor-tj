package tj.navortj.app;
import org.json.JSONObject;
public final class Product {
    public final String id,kind,image,nameTj,nameRu,categoryTj,categoryRu,subtitleTj,subtitleRu,descriptionTj,descriptionRu;
    public final double price;
    Product(JSONObject j) {
        id=j.optString("id"); kind=j.optString("kind","power"); image=j.optString("image");
        nameTj=j.optString("name_tj"); nameRu=j.optString("name_ru",nameTj);
        categoryTj=j.optString("category_tj"); categoryRu=j.optString("category_ru",categoryTj);
        subtitleTj=j.optString("subtitle_tj"); subtitleRu=j.optString("subtitle_ru",subtitleTj);
        descriptionTj=j.optString("description_tj"); descriptionRu=j.optString("description_ru",descriptionTj);
        double raw=j.optDouble("price",0); price=Double.isNaN(raw)||Double.isInfinite(raw)?0:Math.max(0,raw);
    }
    String name(boolean tj){return tj?nameTj:nameRu;}
    String category(boolean tj){return tj?categoryTj:categoryRu;}
    String subtitle(boolean tj){return tj?subtitleTj:subtitleRu;}
    String description(boolean tj){return tj?descriptionTj:descriptionRu;}
}
